package com.nyangmeong.global.mail;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.naming.NameNotFoundException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * [DNS 로 메일 받을 수 있는 도메인 확인]
 * - MX 레코드가 있으면 받을 수 있다(단 "0 ." 은 받지 않겠다는 선언이라 받을 수 없다)
 * - MX 가 없으면 A 레코드가 있을 때만 받을 수 있다(RFC 5321 의 암묵적 MX)
 * - 도메인이 없거나(NXDOMAIN) 둘 다 없으면 받을 수 없다
 * - 그 도메인의 DNS 가 서버 오류를 돌려줄 때도 항상 있는 도메인(example.com)이 정상 조회되면 받을 수 없다고 본다
 * - 시간 초과·네트워크 오류는 알 수 없는 것이므로 받을 수 있다고 본다
 * - JDK 내장 JNDI DNS 를 쓰므로 새 의존성이 없다
 */
@Slf4j
@Component
public class DnsMailDomainChecker implements MailDomainChecker {

    private static final String TIMEOUT_MS = "2000";
    private static final String RETRIES = "1";
    /** 항상 조회되는 도메인(IANA 예약) : 서버 오류가 그 도메인 탓인지 우리 DNS 탓인지 가린다 */
    private static final String CONTROL_DOMAIN = "example.com";

    @Override
    public boolean isReceivable(String domain) {
        if (domain == null || domain.isBlank()) {
            return false;
        }
        String name = domain.trim().toLowerCase(Locale.ROOT);
        try {
            List<String> mx = lookup(name, "MX");
            return judge(mx, () -> lookup(name, "A"));
        } catch (NameNotFoundException e) {
            return false;
        } catch (ServiceUnavailableException e) {
            /** 그 도메인의 DNS 가 오류를 돌려주는 경우(관리가 끊긴 도메인): 우리 DNS 조회가 정상일 때만 받을 수 없다고 본다 */
            return !isResolverWorking();
        } catch (NamingException | RuntimeException e) {
            log.warn("메일 도메인 조회에 실패해 통과시킵니다. type={}", e.getClass().getSimpleName());
            return true;
        }
    }

    /** 항상 있는 도메인을 조회해 이 서버의 DNS 조회 자체가 되는지 본다 */
    private boolean isResolverWorking() {
        try {
            lookup(CONTROL_DOMAIN, "A");
            return true;
        } catch (NamingException | RuntimeException e) {
            log.warn("메일 도메인 조회에 실패해 통과시킵니다. 대조 도메인도 조회되지 않습니다. type={}",
                    e.getClass().getSimpleName());
            return false;
        }
    }

    /** MX 목록과 A 목록(MX 가 없을 때만 조회)으로 판단한다 */
    static boolean judge(List<String> mxRecords, ARecordLookup aLookup) throws NamingException {
        if (!mxRecords.isEmpty()) {
            return mxRecords.stream().anyMatch(mx -> !isNullMx(mx));
        }
        return !aLookup.get().isEmpty();
    }

    /** "0 ." 형태의 null MX */
    static boolean isNullMx(String mx) {
        String[] parts = mx.trim().split("\\s+");
        return parts.length == 2 && ".".equals(parts[1]);
    }

    @FunctionalInterface
    interface ARecordLookup {
        List<String> get() throws NamingException;
    }

    /** 도메인이 없으면 NameNotFoundException, 레코드가 없으면 빈 목록 */
    protected List<String> lookup(String domain, String type) throws NamingException {
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", TIMEOUT_MS);
        env.put("com.sun.jndi.dns.timeout.retries", RETRIES);
        DirContext context = new InitialDirContext(env);
        try {
            Attributes attributes = context.getAttributes(domain, new String[] { type });
            Attribute attribute = attributes.get(type);
            List<String> values = new ArrayList<>();
            if (attribute != null) {
                NamingEnumeration<?> all = attribute.getAll();
                while (all.hasMore()) {
                    values.add(String.valueOf(all.next()));
                }
            }
            return values;
        } finally {
            context.close();
        }
    }
}
