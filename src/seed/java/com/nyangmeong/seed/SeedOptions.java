package com.nyangmeong.seed;

/**
 * [실행 옵션]
 * - 첫 인자 data·clean, 나머지 --이름=값 (build.gradle이 -P 값을 옮긴다)
 * - database : 로컬 대상일 때만 접속 DB 이름을 바꾼다(공용 DB 덤프를 넣은 확인용 DB 등)
 */
public record SeedOptions(Command command, Target target, String confirm, String database) {

    public enum Command {
        DATA, CLEAN
    }

    public enum Target {
        SHARED("공용 DB"), LOCAL("로컬 DB");

        private final String label;

        Target(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static SeedOptions parse(String[] args) {
        if (args.length == 0) {
            throw new SeedException("첫 인자는 data·clean 중 하나입니다");
        }
        Command command = switch (args[0]) {
        case "data" -> Command.DATA;
        case "clean" -> Command.CLEAN;
        default -> throw new SeedException("알 수 없는 명령: " + args[0]);
        };
        Target target = Target.SHARED;
        String confirm = null;
        String database = null;
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            int eq = arg.indexOf('=');
            String name = eq < 0 ? arg : arg.substring(0, eq);
            String value = eq < 0 ? "" : arg.substring(eq + 1);
            switch (name) {
            case "--target" -> target = switch (value) {
            case "shared" -> Target.SHARED;
            case "local" -> Target.LOCAL;
            default -> throw new SeedException("-Ptarget은 shared 또는 local입니다: " + value);
            };
            case "--confirm" -> confirm = value;
            case "--database" -> database = value.isBlank() ? null : value;
            default -> throw new SeedException("알 수 없는 옵션: " + arg);
            }
        }
        if (database != null && target != Target.LOCAL) {
            throw new SeedException("-Pdatabase는 -Ptarget=local일 때만 씁니다");
        }
        return new SeedOptions(command, target, confirm, database);
    }
}
