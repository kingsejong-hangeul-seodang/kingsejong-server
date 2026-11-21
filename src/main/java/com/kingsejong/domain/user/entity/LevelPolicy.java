package com.kingsejong.domain.user.entity;

import java.util.List;

/**
 * 레벨 5단계와 기준 경험치 (API 명세 3.2, 숫자는 임시). 레벨 계산은 서버만 한다.
 */
public final class LevelPolicy {

    public record LevelDef(int no, String name, int minExp) {
    }

    public static final List<LevelDef> LEVELS = List.of(
            new LevelDef(1, "서당 입문", 0),
            new LevelDef(2, "성균관 유생", 300),
            new LevelDef(3, "집현전 학사", 800),
            new LevelDef(4, "대제학", 1500),
            new LevelDef(5, "킹세종", 3000)
    );

    private LevelPolicy() {
    }

    public static LevelDef levelOf(int exp) {
        LevelDef current = LEVELS.get(0);
        for (LevelDef level : LEVELS) {
            if (exp >= level.minExp()) {
                current = level;
            }
        }
        return current;
    }

    /** 다음 레벨 기준 경험치. 마지막 레벨이면 null. */
    public static Integer nextLevelExp(LevelDef level) {
        return level.no() < LEVELS.size() ? LEVELS.get(level.no()).minExp() : null;
    }
}
