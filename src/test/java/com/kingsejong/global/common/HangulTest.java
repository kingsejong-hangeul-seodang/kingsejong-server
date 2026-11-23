package com.kingsejong.global.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HangulTest {

    @Test
    void 음절을_도감_자모로_쪼갠다() {
        assertThat(Hangul.toJamo("밥")).containsExactly("ㅂ", "ㅏ", "ㅂ");
        assertThat(Hangul.toJamo("와")).containsExactly("ㅇ", "ㅘ");
        assertThat(Hangul.toJamo("까")).containsExactly("ㄲ", "ㅏ");
    }

    @Test
    void 겹받침은_낱자모로_나눈다() {
        assertThat(Hangul.toJamo("닭")).containsExactly("ㄷ", "ㅏ", "ㄹ", "ㄱ");
        assertThat(Hangul.toJamo("값")).containsExactly("ㄱ", "ㅏ", "ㅂ", "ㅅ");
        assertThat(Hangul.normalize("ㄳ")).containsExactly("ㄱ", "ㅅ");
    }

    @Test
    void 자모_단독은_그대로_한글이_아니면_빈_목록() {
        assertThat(Hangul.toJamo("ㄷ")).containsExactly("ㄷ");
        assertThat(Hangul.toJamo("ㅢ")).containsExactly("ㅢ");
        assertThat(Hangul.toJamo("a")).isEmpty();
        assertThat(Hangul.toJamo("?")).isEmpty();
        assertThat(Hangul.CONSONANTS).hasSize(19);
        assertThat(Hangul.VOWELS).hasSize(21);
    }
}
