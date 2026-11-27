package com.nyangmeong.seed.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.nyangmeong.global.security.crypto.CryptoProperties;
import com.nyangmeong.global.security.crypto.FieldCryptoService;

/**
 * [시드 공통 도구]
 * - 고정 난수, 실행일 기준 날짜, 공통코드, 암호화, 행 저장소, 남겨 둔 기준값(Masters)
 */
public final class Seed {

    private final Random random;
    private final LocalDate today;
    private final LocalDateTime now;
    private final Codes codes;
    private final FieldCryptoService crypto;
    private final String passwordHash;
    private final RowStore store;
    private final Masters masters;
    private final java.util.List<String> fixedAccounts;
    private final String fixedPasswordHash;

    private Seed(Random random, Codes codes, FieldCryptoService crypto, String password, RowStore store,
            Masters masters, java.util.List<String> fixedAccounts, String fixedPassword) {
        this.random = random;
        this.today = LocalDate.now();
        this.now = today.atTime(LocalTime.now().withNano(0));
        this.codes = codes;
        this.crypto = crypto;
        // 시드 계정 공통 비밀번호(application-local.properties의 app.seed.password)
        this.passwordHash = new BCryptPasswordEncoder().encode(password);
        this.store = store;
        this.masters = masters;
        this.fixedAccounts = fixedAccounts;
        this.fixedPasswordHash = new BCryptPasswordEncoder().encode(fixedPassword);
    }

    public static Seed create(Connection conn, Schema schema, long randomSeed, SeedSettings settings) throws SQLException {
        CryptoProperties props = new CryptoProperties();
        props.setAesKeyBase64(settings.aesKeyBase64());
        props.setHmacKeyBase64(settings.hmacKeyBase64());
        FieldCryptoService crypto = new FieldCryptoService(props);
        crypto.initializeKeys();
        return new Seed(new Random(randomSeed), Codes.load(conn), crypto, settings.requiredSeedPassword(),
                RowStore.load(conn, schema), Masters.load(conn), settings.requiredFixedAccounts(), settings.fixedPassword().trim());
    }

    public RowStore store() {
        return store;
    }

    public Codes codes() {
        return codes;
    }

    /** 꼭 있어야 하고 비밀번호를 app.seed.fixed-password로 맞출 계정 메일 */
    public java.util.List<String> fixedAccounts() {
        return fixedAccounts;
    }

    public String fixedPasswordHash() {
        return fixedPasswordHash;
    }

    public Masters masters() {
        return masters;
    }

    public FieldCryptoService crypto() {
        return crypto;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public LocalDate today() {
        return today;
    }

    public LocalDateTime now() {
        return now;
    }

    // ---- 난수

    /** a 이상 b 이하 */
    public int between(int a, int b) {
        return a + random.nextInt(b - a + 1);
    }

    public double between(double a, double b) {
        return a + random.nextDouble() * (b - a);
    }

    public boolean chance(double probability) {
        return random.nextDouble() < probability;
    }

    public <T> T pick(List<T> items) {
        return items.get(random.nextInt(items.size()));
    }

    @SafeVarargs
    public final <T> T pick(T... items) {
        return items[random.nextInt(items.length)];
    }

    /**
     * 겹치지 않게 n개
     */
    public <T> List<T> sample(List<T> items, int n) {
        List<T> copy = new ArrayList<>(items);
        Collections.shuffle(copy, random);
        return copy.subList(0, Math.min(n, copy.size()));
    }

    public <T> void shuffle(List<T> items) {
        Collections.shuffle(items, random);
    }

    /**
     * 가중치 선택. weights는 items와 같은 길이
     */
    public <T> T weighted(List<T> items, int... weights) {
        int total = 0;
        for (int w : weights) {
            total += w;
        }
        int r = random.nextInt(total);
        for (int i = 0; i < items.size(); i++) {
            r -= weights[i];
            if (r < 0) {
                return items.get(i);
            }
        }
        return items.get(items.size() - 1);
    }

    /** 고정 시드로 만든 UUID(멱등 요청 ID 등) */
    public String uuid() {
        return new UUID(random.nextLong(), random.nextLong()).toString();
    }

    /** 64자 16진수(토큰 해시 자리) */
    public String hex64() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP);
    }

    // ---- 날짜

    // 아래 날짜 함수는 분기와 상관없이 난수를 같은 개수만큼 뽑는다. 실행 시각에 따라 난수 순서가 밀리면 실행마다 데이터가 달라진다

    /** 오늘부터 minDays~maxDays일 전의 임의 시각 */
    public LocalDateTime daysAgo(int minDays, int maxDays) {
        LocalDateTime t = today.minusDays(between(minDays, maxDays)).atTime(between(7, 22), between(0, 59), between(0, 59));
        int backMinutes = between(1, 120);
        return t.isAfter(now) ? now.minusMinutes(backMinutes) : t;
    }

    /** from 이후 now 이전의 임의 시각 */
    public LocalDateTime after(LocalDateTime from) {
        double r = random.nextDouble();
        long span = java.time.Duration.between(from, now).toMinutes();
        if (span <= 1) {
            return now;
        }
        return from.plusMinutes(1 + (long) (r * (span - 1)));
    }

    /** from 이후 maxMinutes 분 안쪽, now를 넘지 않음 */
    public LocalDateTime soonAfter(LocalDateTime from, int maxMinutes) {
        LocalDateTime t = from.plusMinutes(between(1, maxMinutes));
        return t.isAfter(now) ? now : t;
    }
}