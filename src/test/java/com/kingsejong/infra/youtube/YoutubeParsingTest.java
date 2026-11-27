package com.kingsejong.infra.youtube;

import com.kingsejong.global.error.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YoutubeParsingTest {

    @Test
    void 여러_모양의_주소에서_영상_ID를_꺼낸다() {
        for (String url : new String[]{"pSUydWEqKwE", "https://www.youtube.com/watch?v=pSUydWEqKwE&t=10s",
                "https://youtu.be/pSUydWEqKwE?si=abc", "https://www.youtube.com/shorts/pSUydWEqKwE",
                "https://www.youtube.com/embed/pSUydWEqKwE", " https://m.youtube.com/watch?feature=x&v=pSUydWEqKwE "}) {
            assertThat(YoutubeVideoId.parse(url)).isEqualTo("pSUydWEqKwE");
        }
        assertThatThrownBy(() -> YoutubeVideoId.parse("https://example.com/video")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> YoutubeVideoId.parse("")).isInstanceOf(BusinessException.class);
    }

    @Test
    void 자막_format3을_읽고_HTML_기호를_푼다() {
        List<YoutubeVideo.Line> lines = CaptionParser.parse("<?xml version=\"1.0\"?><timedtext format=\"3\"><body>"
                + "<p t=\"18918\" d=\"677\">희수야</p><p t=\"23090\" d=\"1184\">Okay, Let&#39;s Go</p>"
                + "<p t=\"30000\" d=\"1000\"><s>두 </s><s>줄</s>\n이야</p><p t=\"40000\" d=\"500\"> </p></body></timedtext>");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(0)).isEqualTo(new YoutubeVideo.Line(18.92, 19.6, "희수야"));
        assertThat(lines.get(1).text()).isEqualTo("Okay, Let's Go");
        assertThat(lines.get(2).text()).isEqualTo("두 줄 이야");
    }

    @Test
    void 자막_format1도_읽는다() {
        List<YoutubeVideo.Line> lines = CaptionParser.parse("<transcript><text start=\"1.5\" dur=\"2\">안녕 &amp;amp; 반가워</text></transcript>");
        assertThat(lines).containsExactly(new YoutubeVideo.Line(1.5, 3.5, "안녕 & 반가워"));
    }
}
