package com.kingsejong.domain.content.seed;

import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.global.common.Stage;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 시드 정답 문장 검사(엔진 규칙)와 다시 켜도 중복되지 않는지. */
class ContentSeederTest extends IntegrationTest {

    @Autowired
    ContentSeeder contentSeeder;

    @Autowired
    ContentRepository contentRepository;

    @Test
    void 엔진_규칙에_맞는_정답은_통과한다() {
        assertThatCode(() -> ContentSeeder.checkTarget("t", Stage.BEGINNER, "바다")).doesNotThrowAnyException();
        assertThatCode(() -> ContentSeeder.checkTarget("t", Stage.BEGINNER, "ㄷ")).doesNotThrowAnyException();
        assertThatCode(() -> ContentSeeder.checkTarget("t", Stage.INTERMEDIATE, "화장실은 어디예요?")).doesNotThrowAnyException();
        assertThatCode(() -> ContentSeeder.checkTarget("t", Stage.ADVANCED, "가나다라마바사아자차 카타파하가나다라마")).doesNotThrowAnyException();
    }

    @Test
    void 엔진_규칙을_어기면_서버가_켜지지_않게_예외() {
        assertRejected(Stage.ADVANCED, "가나다라마바사아자차카타파하가나다라마바사"); // 21칸
        assertRejected(Stage.ADVANCED, "가나  다라");                          // 연속 공백
        assertRejected(Stage.ADVANCED, " 가나");                               // 앞 공백
        assertRejected(Stage.BEGINNER, "바다 위");                             // 초보 띄어쓰기
        assertRejected(Stage.INTERMEDIATE, "하나 둘 셋 넷");                   // 중수 4어절
        assertRejected(Stage.ADVANCED, "ㄷ");                                  // 고수 자모
        assertRejected(Stage.BEGINNER, "ㄷㄹ");                                // 자모 두 글자
        assertRejected(Stage.BEGINNER, "sea");                                 // 영문
        assertRejected(Stage.BEGINNER, "");
    }

    @Test
    void 다시_돌려도_콘텐츠가_늘지_않는다() throws Exception {
        long before = contentRepository.count();
        contentSeeder.run(null);
        assertThat(contentRepository.count()).isEqualTo(before);
        assertThat(contentRepository.findBySeedKey("kpop-starlight")).isPresent();
    }

    private static void assertRejected(Stage stage, String target) {
        assertThatThrownBy(() -> ContentSeeder.checkTarget("t", stage, target))
                .isInstanceOf(IllegalStateException.class);
    }
}
