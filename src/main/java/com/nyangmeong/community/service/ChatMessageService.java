package com.nyangmeong.community.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.nyangmeong.community.domain.CareMatch;
import com.nyangmeong.community.domain.CareMatchStatus;
import com.nyangmeong.community.domain.ChatMessage;
import com.nyangmeong.community.domain.ChatMessagePhoto;
import com.nyangmeong.community.domain.ChatMessageType;
import com.nyangmeong.community.domain.ChatParticipantStatus;
import com.nyangmeong.community.dto.ChatMessageCreateRequest;
import com.nyangmeong.community.dto.ChatMessageResponse;
import com.nyangmeong.community.dto.ChatMessageSliceResponse;
import com.nyangmeong.community.dto.ChatMessageSummary;
import com.nyangmeong.community.dto.PostImageSummary;
import com.nyangmeong.community.mapper.CareMatchMapper;
import com.nyangmeong.community.mapper.ChatMessageMapper;
import com.nyangmeong.community.mapper.ChatRoomMapper;
import com.nyangmeong.global.error.CustomException;
import com.nyangmeong.global.error.ErrorCode;
import com.nyangmeong.global.file.FileCategory;
import com.nyangmeong.global.file.FileOwner;
import com.nyangmeong.global.file.FileStore;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

import lombok.RequiredArgsConstructor;

/**
 * [펫돌봄 채팅 메시지 서비스]
 * - SCR-CMU-005 메시지 전송·조회(FN-CMU-005-04·05), 같은 idmptDmndId 재전송은 저장하지 않고 기존 메시지 반환
 * - 방 행 잠금으로 MSG_SEQ 를 방 안에서 겹치지 않게 매기고, 저장 뒤 커밋 시점에 참여자에게 실시간 전송
 * - 내용·공유 주소는 FieldCryptoService 로 암호화 저장
 */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int MAX_IMAGES = 10;
    private static final int DEFAULT_SIZE = 30;
    private static final int MAX_SIZE = 50;
    private static final String ADDRESS_DELIMITER = "\n";
    private static final String SYSTEM_ID_PREFIX = "SYS-";

    private final ChatRoomMapper chatRoomMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final CareMatchMapper careMatchMapper;
    private final ChatRoomGuard chatRoomGuard;
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final FileStore fileStore;
    private final FieldCryptoService fieldCryptoService;

    /**
     * 메시지 조회 (커서, 최신순)
     */
    @Transactional(readOnly = true)
    public ChatMessageSliceResponse search(Long mbrSn, Long otoChatRoomSn, Long beforeChatMsgSn, Integer size) {
        chatRoomGuard.getRoom(mbrSn, otoChatRoomSn);
        int limit = size == null ? DEFAULT_SIZE : Math.min(Math.max(size, 1), MAX_SIZE);

        List<ChatMessageSummary> rows = chatMessageMapper.selectList(otoChatRoomSn, beforeChatMsgSn, limit + 1);
        boolean hasNext = rows.size() > limit;
        List<ChatMessageSummary> content = hasNext ? rows.subList(0, limit) : rows;

        Map<Long, List<PostImageSummary>> photos = photosOf(content.stream().map(ChatMessageSummary::getChatMsgSn).toList());
        for (ChatMessageSummary row : content) {
            row.setMine(mbrSn.equals(row.getMbrSn()));
            row.setMsgCn(decrypt(row.getMsgCn()));
            applyAddress(row, decrypt(row.getShrnAddr()));
            row.setPhotos(photos.getOrDefault(row.getChatMsgSn(), List.of()));
        }
        return new ChatMessageSliceResponse(List.copyOf(content), hasNext);
    }

    /**
     * 메시지 전송 (텍스트·사진·주소 공유)
     */
    @Transactional
    public ChatMessageResponse send(Long mbrSn, Long otoChatRoomSn, ChatMessageCreateRequest req) {
        chatRoomGuard.getRoom(mbrSn, otoChatRoomSn);
        chatRoomGuard.checkJoined(mbrSn, otoChatRoomSn);
        List<MultipartFile> images = attached(req.images());
        checkRequest(req, images);

        /** 방 잠금 뒤에 중복 요청과 순서를 확인 : 같은 방의 전송이 한 줄로 처리됨 */
        chatRoomMapper.selectRoomForUpdate(otoChatRoomSn);
        ChatMessage replay = chatMessageMapper.selectByIdmptDmndIdForShare(req.idmptDmndId());
        if (replay != null) {
            if (!otoChatRoomSn.equals(replay.getOtoChatRoomSn()) || !mbrSn.equals(replay.getMbrSn())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            ChatMessageSummary existing = toSummary(replay, photosOf(List.of(replay.getChatMsgSn()))
                    .getOrDefault(replay.getChatMsgSn(), List.of()));
            return ChatMessageResponse.of(existing.forViewer(mbrSn), false);
        }
        if (ChatMessageType.ADDRESS.is(req.msgTypeCd())) {
            checkAddressAllowed(otoChatRoomSn);
        }

        ChatMessage message = new ChatMessage();
        message.setOtoChatRoomSn(otoChatRoomSn);
        message.setMbrSn(mbrSn);
        message.setMsgTypeCd(req.msgTypeCd());
        message.setMsgCnEnc(StringUtils.hasText(req.msgCn()) ? fieldCryptoService.encrypt(req.msgCn().strip()) : null);
        message.setShrnAddrEnc(ChatMessageType.ADDRESS.is(req.msgTypeCd())
                ? fieldCryptoService.encrypt(joinAddress(req.shrnAddr(), req.dtlAddr()))
                : null);
        message.setIdmptDmndId(req.idmptDmndId());
        message.setMsgSeq(chatMessageMapper.selectNextMsgSeqForUpdate(otoChatRoomSn));
        chatMessageMapper.insertMessage(message);

        int sortSeq = 0;
        for (MultipartFile image : images) {
            ChatMessagePhoto photo = new ChatMessagePhoto();
            photo.setChatMsgSn(message.getChatMsgSn());
            photo.setAtchFileSn(fileStore.store(image, FileCategory.COMMUNITY_CHAT, FileOwner.member(mbrSn), String.valueOf(mbrSn)));
            photo.setSortSeq(sortSeq++);
            chatMessageMapper.insertPhoto(photo);
        }

        ChatMessageSummary summary = toSummary(message, photosOf(List.of(message.getChatMsgSn()))
                .getOrDefault(message.getChatMsgSn(), List.of()));
        chatRealtimePublisher.publishMessage(otoChatRoomSn, joinedMbrSns(otoChatRoomSn), summary);
        return ChatMessageResponse.of(summary.forViewer(mbrSn), true);
    }

    /**
     * 시스템 안내 메시지 등록 (요청·수락·완료 등 거래 단계 변경 안내)
     */
    @Transactional
    public ChatMessageSummary createSystemMessage(Long otoChatRoomSn, String text) {
        chatRoomMapper.selectRoomForUpdate(otoChatRoomSn);

        ChatMessage message = new ChatMessage();
        message.setOtoChatRoomSn(otoChatRoomSn);
        message.setMsgTypeCd(ChatMessageType.SYSTEM.getCode());
        message.setMsgCnEnc(fieldCryptoService.encrypt(text));
        message.setIdmptDmndId(SYSTEM_ID_PREFIX + UUID.randomUUID());
        message.setMsgSeq(chatMessageMapper.selectNextMsgSeqForUpdate(otoChatRoomSn));
        chatMessageMapper.insertMessage(message);

        ChatMessageSummary summary = toSummary(message, List.of());
        chatRealtimePublisher.publishMessage(otoChatRoomSn, joinedMbrSns(otoChatRoomSn), summary);
        return summary;
    }

    private void checkRequest(ChatMessageCreateRequest req, List<MultipartFile> images) {
        if (!ChatMessageType.isSendable(req.msgTypeCd())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        boolean valid;
        if (ChatMessageType.TEXT.is(req.msgTypeCd())) {
            valid = StringUtils.hasText(req.msgCn()) && images.isEmpty();
        } else if (ChatMessageType.PHOTO.is(req.msgTypeCd())) {
            valid = !images.isEmpty() && images.size() <= MAX_IMAGES;
        } else {
            valid = StringUtils.hasText(req.shrnAddr()) && images.isEmpty();
        }
        if (!valid) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private void checkAddressAllowed(Long otoChatRoomSn) {
        CareMatch match = careMatchMapper.selectByRoomForUpdate(otoChatRoomSn);
        if (match == null || !CareMatchStatus.isConfirmed(match.getMtchDlngSttsCd())) {
            throw new CustomException(ErrorCode.CHAT_ADDRESS_NOT_ALLOWED);
        }
    }

    private List<Long> joinedMbrSns(Long otoChatRoomSn) {
        return chatRoomMapper.selectParticipantMbrSns(otoChatRoomSn, ChatParticipantStatus.JOINED.getCode());
    }

    private Map<Long, List<PostImageSummary>> photosOf(List<Long> chatMsgSns) {
        if (chatMsgSns.isEmpty()) {
            return Map.of();
        }
        return chatMessageMapper.selectPhotos(chatMsgSns).stream()
                .collect(Collectors.groupingBy(ChatMessagePhoto::getChatMsgSn,
                        Collectors.mapping(photo -> new PostImageSummary(photo.getAtchFileSn(), photo.getSortSeq()),
                                Collectors.toList())));
    }

    private ChatMessageSummary toSummary(ChatMessage message, List<PostImageSummary> photos) {
        ChatMessageSummary summary = new ChatMessageSummary();
        summary.setChatMsgSn(message.getChatMsgSn());
        summary.setMsgSeq(message.getMsgSeq());
        summary.setMbrSn(message.getMbrSn());
        summary.setMsgTypeCd(message.getMsgTypeCd());
        summary.setMsgCn(decrypt(message.getMsgCnEnc()));
        applyAddress(summary, decrypt(message.getShrnAddrEnc()));
        summary.setPhotos(photos);
        /** 방금 저장한 행은 REG_DT 를 다시 읽지 않아 같은 시각대로 채움 */
        summary.setRegDt(message.getRegDt() != null ? message.getRegDt() : LocalDateTime.now());
        return summary;
    }

    private String decrypt(String envelope) {
        return envelope == null ? null : fieldCryptoService.decrypt(envelope);
    }

    private String joinAddress(String shrnAddr, String dtlAddr) {
        String place = shrnAddr.replace(ADDRESS_DELIMITER, " ").strip();
        if (!StringUtils.hasText(dtlAddr)) {
            return place;
        }
        return place + ADDRESS_DELIMITER + dtlAddr.replace(ADDRESS_DELIMITER, " ").strip();
    }

    private void applyAddress(ChatMessageSummary summary, String address) {
        if (address == null) {
            summary.setShrnAddr(null);
            return;
        }
        String[] parts = address.split(ADDRESS_DELIMITER, 2);
        summary.setShrnAddr(parts[0]);
        summary.setDtlAddr(parts.length > 1 ? parts[1] : null);
    }

    private List<MultipartFile> attached(List<MultipartFile> images) {
        if (images == null) {
            return List.of();
        }
        return images.stream().filter(image -> image != null && !image.isEmpty()).toList();
    }
}
