package com.kingsejong.domain.content.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.seed.SeedReader;
import com.kingsejong.global.common.Stage;
import com.kingsejong.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 시드 정답 문장 검사(엔진 규칙)와 다시 켜도 중복되지 않는지. */
class ContentSeederTest extends IntegrationTest {

    @Autowired
    ContentSeeder contentSeeder;

    @Autowired
    ContentRepository contentRepository;

    @Autowired
    com.kingsejong.domain.taste.service.TasteService tasteService;

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
        flushAndClear();
        assertThat(contentRepository.count()).isEqualTo(before);
        assertThat(contentRepository.findBySeedKey("kpop-starlight")).isPresent();
    }

    @Test
    void DB에서_바꾼_값은_다시_돌리면_JSON_값으로_돌아오고_자막과_쓸_글이_쌓이지_않는다() throws Exception {
        Content content = contentRepository.findBySeedKey("kpop-starlight").orElseThrow();
        content.update(content.getMode(), content.getKind(), "바뀐 제목", content.getSource(), null,
                "CHANGED_ID", content.getStartSec(), content.getEndSec(), true, null, content.getSortOrder(), null);
        flushAndClear();

        contentSeeder.run(null);
        flushAndClear();

        Content restored = contentRepository.findBySeedKey("kpop-starlight").orElseThrow();
        assertThat(restored.getTitle()).isEqualTo("별빛 아래");
        assertThat(restored.getYoutubeVideoId()).isEqualTo("VIDEO_ID_01");
        assertThat(restored.getLines()).hasSize(2);
        assertThat(restored.getTasks()).hasSize(3);
    }

    @Test
    void 시드에서_빠진_콘텐츠와_난이도는_숨기거나_지운다() throws Exception {
        SeedReader onlyStarlight = new SeedReader(objectMapper) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T read(String path, TypeReference<T> type) {
                return (T) List.of(new ContentSeeder.ContentSeed("kpop-starlight", Mode.KPOP, ContentKind.CLIP,
                        "별빛 아래", null, null, "VIDEO_ID_01", 62.0, 88.0, true, null, List.of(),
                        List.of(new ContentSeeder.TaskSeed(Stage.BEGINNER, "바다", "sea", null)), null));
            }
        };
        new ContentSeeder(onlyStarlight, contentRepository, tasteService).run(null);
        flushAndClear();

        assertThat(contentRepository.findBySeedKey("kpop-starlight").orElseThrow().getTasks()).hasSize(1);
        assertThat(contentRepository.findBySeedKey("kpop-missed-you").orElseThrow().isActive()).isFalse();
        assertThat(contentRepository.findAllByModeAndStage(Mode.KPOP, Stage.BEGINNER)).hasSize(1);
        assertThat(contentRepository.findAllByModeAndStage(Mode.KPOP, Stage.INTERMEDIATE)).isEmpty();
    }

    @Test
    void key가_없거나_겹치면_예외() {
        ContentSeeder.TaskSeed task = new ContentSeeder.TaskSeed(Stage.BEGINNER, "물", null, null);
        ContentSeeder.ContentSeed a = new ContentSeeder.ContentSeed("same", Mode.BASIC, ContentKind.WORD,
                "a", null, null, null, null, null, false, null, null, List.of(task), null);
        ContentSeeder.ContentSeed noKey = new ContentSeeder.ContentSeed(null, Mode.BASIC, ContentKind.WORD,
                "b", null, null, null, null, null, false, null, null, List.of(task), null);
        assertThatThrownBy(() -> seederWith(List.of(a, a)).run(null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> seederWith(List.of(noKey)).run(null)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 클립_구간이_이상하면_예외() {
        List<ContentSeeder.LineSeed> noLines = List.of();
        assertThatThrownBy(() -> ContentSeeder.checkClip(clip("ID", 10.0, 50.0, noLines))) // 40초
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ContentSeeder.checkClip(clip("ID", 10.0, 5.0, noLines)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ContentSeeder.checkClip(clip(null, 10.0, 20.0, noLines)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ContentSeeder.checkClip(clip("ID", 10.0, 20.0,
                List.of(new ContentSeeder.LineSeed("자막", 18.0, 25.0, null))))) // 구간 밖
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ContentSeeder.checkClip(new ContentSeeder.ContentSeed("k", Mode.BASIC,
                ContentKind.WORD, "t", null, null, "ID", null, null, false, null, null, List.of(), null)))
                .isInstanceOf(IllegalStateException.class); // 영상 없는 콘텐츠에 영상 ID
        assertThatCode(() -> ContentSeeder.checkClip(clip("ID", 10.0, 40.0,
                List.of(new ContentSeeder.LineSeed("자막", 12.0, 15.0, null))))).doesNotThrowAnyException();
    }

    private ContentSeeder.ContentSeed clip(String videoId, Double start, Double end, List<ContentSeeder.LineSeed> lines) {
        return new ContentSeeder.ContentSeed("k", Mode.KPOP, ContentKind.CLIP, "t", null, null,
                videoId, start, end, true, null, lines, List.of(), null);
    }

    private ContentSeeder seederWith(List<ContentSeeder.ContentSeed> seeds) {
        SeedReader reader = new SeedReader(objectMapper) {
            @Override
            @SuppressWarnings("unchecked")
            public <T> T read(String path, TypeReference<T> type) {
                return (T) seeds;
            }
        };
        return new ContentSeeder(reader, contentRepository, tasteService);
    }

    private static void assertRejected(Stage stage, String target) {
        assertThatThrownBy(() -> ContentSeeder.checkTarget("t", stage, target))
                .isInstanceOf(IllegalStateException.class);
    }
}
