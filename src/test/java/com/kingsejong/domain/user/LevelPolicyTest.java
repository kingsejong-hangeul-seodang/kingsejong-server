package com.kingsejong.domain.user;

import com.kingsejong.domain.user.dto.response.LevelResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LevelPolicyTest {

    @Test
    void 기준_경험치_바로_아래는_이전_레벨이다() {
        LevelResponse level = LevelResponse.of(299);
        assertThat(level.no()).isEqualTo(1);
        assertThat(level.levelStartExp()).isEqualTo(0);
        assertThat(level.nextLevelExp()).isEqualTo(300);
    }

    @Test
    void 기준_경험치와_같으면_다음_레벨이다() {
        LevelResponse level = LevelResponse.of(300);
        assertThat(level.no()).isEqualTo(2);
        assertThat(level.name()).isEqualTo("성균관 유생");
        assertThat(level.levelStartExp()).isEqualTo(300);
        assertThat(level.nextLevelExp()).isEqualTo(800);
    }

    @Test
    void 마지막_레벨은_nextLevelExp가_null이다() {
        LevelResponse level = LevelResponse.of(5000);
        assertThat(level.no()).isEqualTo(5);
        assertThat(level.nextLevelExp()).isNull();
    }
}
