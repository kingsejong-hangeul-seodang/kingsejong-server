package com.kingsejong.domain.content.youtube;

import com.kingsejong.global.common.Stage;
import com.kingsejong.infra.youtube.YoutubeVideo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SubtitleStudyPickerTest {

    private static YoutubeVideo.Line line(double start, double end, String text) {
        return new YoutubeVideo.Line(start, end, text);
    }

    @Test
    void 자막을_엔진이_받는_글자로_정리한다() {
        assertThat(SubtitleStudyPicker.clean("[음악] 보고 싶었어 ♪")).isEqualTo("보고 싶었어");
        assertThat(SubtitleStudyPicker.clean("(웃음) 정말? 진짜!")).isEqualTo("정말? 진짜!");
        assertThat(SubtitleStudyPicker.usable("Okay, Let's Go")).isFalse();
        assertThat(SubtitleStudyPicker.usable("오 혜인 2칸")).isFalse();
        assertThat(SubtitleStudyPicker.usable("[음악]")).isFalse();
        assertThat(SubtitleStudyPicker.usable("희수야 그거 아니야")).isTrue();
    }

    @Test
    void 쓸_수_있는_줄이_가장_많은_30초_구간을_고른다() {
        List<YoutubeVideo.Line> lines = List.of(
                line(5, 7, "Intro"), line(40, 42, "하나 둘"), line(43, 45, "오늘 날씨 좋다"),
                line(46, 48, "우리 같이 걸어요"), line(80, 82, "혼자 남은 줄"));
        SubtitleStudyPicker.Window w = SubtitleStudyPicker.window(lines, null, null, 200).orElseThrow();
        assertThat(w.startSec()).isEqualTo(39);
        assertThat(w.endSec()).isLessThanOrEqualTo(69);
        assertThat(w.lines()).hasSize(3);
    }

    @Test
    void 구간을_주면_그_안의_줄만() {
        List<YoutubeVideo.Line> lines = List.of(line(10, 12, "가나"), line(12, 15, "다라"), line(29, 31, "마바"));
        SubtitleStudyPicker.Window w = SubtitleStudyPicker.window(lines, 10.0, 30.0, null).orElseThrow();
        assertThat(w.lines()).extracting(YoutubeVideo.Line::text).containsExactly("가나", "다라");
    }

    @Test
    void 자막이_없으면_구간도_없다() {
        assertThat(SubtitleStudyPicker.window(List.of(line(1, 2, "Hello")), null, null, null)).isEmpty();
    }

    @Test
    void 난이도별_쓸_글을_엔진_규칙에_맞게_추천한다() {
        Map<Stage, String> t = SubtitleStudyPicker.pickTargets(List.of(
                line(1, 2, "Yeah"), line(2, 4, "보고 싶었어"), line(4, 6, "오늘도 너를 기다렸어 정말로 아주 많이")));
        assertThat(t.get(Stage.ADVANCED)).isEqualTo("오늘도 너를 기다렸어 정말로 아주"); // 20칸 안에서 어절 단위 (많이까지 넣으면 21칸)
        assertThat(t.get(Stage.ADVANCED).length()).isLessThanOrEqualTo(20);
        assertThat(t.get(Stage.INTERMEDIATE)).isEqualTo("오늘도 너를");
        assertThat(t.get(Stage.BEGINNER)).isEqualTo("오늘"); // 조사 떼고 2글자 우선
    }

    @Test
    void 쓸_줄이_없으면_빈_추천() {
        assertThat(SubtitleStudyPicker.pickTargets(List.of(line(1, 2, "Hello world")))).isEmpty();
    }

    @Test
    void 영상_제목에서_따옴표_안을_꺼낸다() {
        assertThat(YoutubeContentService.shortTitle("NewJeans (뉴진스) 'Ditto' Official MV (side A)")).isEqualTo("Ditto");
        assertThat(YoutubeContentService.shortTitle("서당의 봄 1화 하이라이트")).isEqualTo("서당의 봄 1화 하이라이트");
    }
}
