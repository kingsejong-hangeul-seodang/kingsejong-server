package com.kingsejong.domain.user.dto.response;

import com.kingsejong.domain.user.entity.LevelPolicy;

/** 지금 레벨 (API 명세 3.2 level, 3.8 reward.level). */
public record LevelResponse(
        int no,
        String name,
        int exp,
        int levelStartExp,
        Integer nextLevelExp
) {

    public static LevelResponse of(int exp) {
        LevelPolicy.LevelDef level = LevelPolicy.levelOf(exp);
        return new LevelResponse(level.no(), level.name(), exp, level.minExp(), LevelPolicy.nextLevelExp(level));
    }
}
