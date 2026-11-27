package com.nyangmeong.seed;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.Map;

import com.nyangmeong.seed.core.RowStore;
import com.nyangmeong.seed.core.Schema;
import com.nyangmeong.seed.core.Seed;
import com.nyangmeong.seed.core.SeedSettings;
import com.nyangmeong.seed.core.SeedValidator;
import com.nyangmeong.seed.core.SeedWarnings;
import com.nyangmeong.seed.core.Seeder;
import com.nyangmeong.seed.core.World;
import com.nyangmeong.seed.domain.CareMissionSeeder;
import com.nyangmeong.seed.domain.CommunitySeeder;
import com.nyangmeong.seed.domain.ExpenseSeeder;
import com.nyangmeong.seed.domain.HealthSeeder;
import com.nyangmeong.seed.domain.MemberSeeder;
import com.nyangmeong.seed.domain.NotificationSeeder;
import com.nyangmeong.seed.domain.PetSeeder;
import com.nyangmeong.seed.domain.PlaceSeeder;
import com.nyangmeong.seed.domain.ReportSeeder;
import com.nyangmeong.seed.domain.SupportSeeder;

/**
 * [시연용 시드 생성기]
 * - clean : 업무 데이터만 골라 비운다(SeedTables·SeedCleaner). 기준값·장소·뉴스·관리자 기록·남길 회원 계정은 그대로. 한 트랜잭션
 * - data : 비운 DB의 남긴 행 위에 주인공·엑스트라 데이터를 넣는다. 한 트랜잭션, 커밋 전 검사 실패 시 롤백
 * - 실행은 build.gradle의 seedClean·seedData 태스크(README "시드 데이터")
 */
public final class SeedDataApplication {

    /** 난수 시드. 바꾸면 데이터가 달라진다 */
    private static final long RANDOM_SEED = 20261208L;

    private SeedDataApplication() {
    }

    public static void main(String[] args) {
        try {
            SeedOptions options = SeedOptions.parse(args);
            switch (options.command()) {
            case CLEAN -> clean(options);
            default -> seed(options);
            }
        } catch (SeedException e) {
            System.err.println();
            System.err.println("[시드] 중단: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println();
            System.err.println("[시드] 예상하지 못한 오류로 중단했습니다. DB는 바뀌지 않았습니다.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void seed(SeedOptions options) throws Exception {
        SeedSettings settings = SeedSettings.load(options.target(), options.database());
        System.out.println("[시드] 넣기 대상: " + options.target().label() + " " + settings.describeDatabase());
        // 비밀번호 확인은 DB에 붙기 전에 끝낸다
        settings.requiredSeedPassword();
        settings.requiredFixedAccounts();
        checkTarget(options, settings, "넣으려면");

        try (Connection conn = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            Schema schema = Schema.load(conn);
            requireCleaned(conn, schema);

            long started = System.currentTimeMillis();
            Seed seed = Seed.create(conn, schema, RANDOM_SEED, settings);
            World world = new World();
            List<Seeder> seeders = List.of(new MemberSeeder(), new PetSeeder(), new HealthSeeder(), new ExpenseSeeder(),
                    new PlaceSeeder(), new CommunitySeeder(), new ReportSeeder(), new SupportSeeder(), new NotificationSeeder(),
                    new CareMissionSeeder());
            for (Seeder seeder : seeders) {
                seeder.run(seed, world);
            }
            seed.store().checkColumns(schema);
            conn.setAutoCommit(false);
            try {
                seed.store().flush(conn, schema);
                new SeedValidator(conn, schema).validateData(seed.store().firstSns());
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            System.out.println();
            seed.store().counts().forEach((table, count) -> System.out.printf("  %-26s %,8d%n", table, count));
            System.out.printf("[시드] 완료: %,d행, 남긴 행 고치기 %d건, %.1f초%n", seed.store().totalRows(),
                    seed.store().updateCount(), (System.currentTimeMillis() - started) / 1000.0);
            SeedWarnings.print();
            System.out.println("[시드] 주인공 계정과 적용 순서는 README \"시드 데이터\"에 있습니다");
        }
    }

    private static void clean(SeedOptions options) throws Exception {
        SeedSettings settings = SeedSettings.load(options.target(), options.database());
        System.out.println("[시드] 비우기 대상: " + options.target().label() + " " + settings.describeDatabase());
        System.out.println("[시드] 업무 데이터(반려동물·기록·글·채팅·알림·문의·아이템 등)를 지웁니다. 기준값·장소·뉴스·관리자 기록·남길 회원 계정은 그대로입니다");
        checkTarget(options, settings, "지우려면");

        try (Connection conn = DriverManager.getConnection(settings.jdbcUrl(), settings.username(), settings.password())) {
            Schema schema = Schema.load(conn);
            conn.setAutoCommit(false);
            Map<String, Long> deleted;
            try {
                deleted = new SeedCleaner(conn, schema).clean();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
            deleted.forEach((table, count) -> System.out.printf("  %-26s %,8d%n", table, count));
            long total = deleted.values().stream().mapToLong(Long::longValue).sum();
            System.out.printf("[시드] 비우기 완료: %d개 테이블 %,d행. 지운 첨부파일의 서버 파일은 그대로 남습니다%n", deleted.size(), total);
            SeedWarnings.print();
        }
    }

    /**
     * 대상 DB 확인
     * - 공용 : DB 이름 입력 확인
     * - 로컬 : localhost·127.0.0.1만
     */
    private static void checkTarget(SeedOptions options, SeedSettings settings, String action) throws Exception {
        if (options.target() == SeedOptions.Target.LOCAL) {
            if (!settings.isLocalHost()) {
                throw new SeedException("-Ptarget=local인데 접속 주소가 " + settings.host()
                        + "입니다. application-local.properties의 spring.datasource.url을 확인해 주세요");
            }
            return;
        }
        String typed = options.confirm();
        if (typed == null) {
            System.out.print("[시드] " + action + " DB 이름(" + settings.database() + ")을 입력하세요: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            typed = in.readLine();
        }
        if (typed == null || !typed.trim().equals(settings.database())) {
            throw new SeedException("DB 이름이 맞지 않아 DB를 바꾸지 않았습니다");
        }
    }

    /**
     * 비운 DB인지 확인 : 지우는 테이블이 모두 비어 있어야 넣는다(섞어 넣지 않는다)
     */
    private static void requireCleaned(Connection conn, Schema schema) throws Exception {
        Map<String, Long> filled = RowStore.nonEmpty(conn, SeedTables.cleared(schema));
        if (!filled.isEmpty()) {
            StringBuilder message = new StringBuilder("비우지 않은 테이블 " + filled.size() + "개가 있습니다. 먼저 seedClean을 실행해 주세요:");
            filled.entrySet().stream().limit(5).forEach(e -> message.append(' ').append(e.getKey()).append('=').append(e.getValue()));
            throw new SeedException(message.toString());
        }
    }
}
