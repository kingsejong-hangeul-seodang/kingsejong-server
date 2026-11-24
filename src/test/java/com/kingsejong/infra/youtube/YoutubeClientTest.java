package com.kingsejong.infra.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 가짜 유튜브(oEmbed·player·timedtext)로 클라이언트 동작 확인. */
class YoutubeClientTest {

    private HttpServer server;
    private volatile int oembedStatus = 200;
    private volatile String tracks;
    private String base;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        tracks = "[{\"languageCode\":\"ko\",\"kind\":\"asr\",\"baseUrl\":\"" + base + "/api/timedtext?asr\"},"
                + "{\"languageCode\":\"ko\",\"baseUrl\":\"" + base + "/api/timedtext?manual\"},"
                + "{\"languageCode\":\"en\",\"baseUrl\":\"" + base + "/api/timedtext?en\"}]";
        server.createContext("/oembed", ex -> respond(ex, oembedStatus,
                "{\"title\":\"NewJeans 'Ditto' MV\",\"author_name\":\"HYBE LABELS\",\"thumbnail_url\":\"https://i.ytimg.com/x.jpg\"}"));
        server.createContext("/youtubei/v1/player", ex -> respond(ex, 200,
                "{\"videoDetails\":{\"title\":\"Ditto\",\"author\":\"HYBE\",\"lengthSeconds\":\"333\"},"
                        + "\"captions\":{\"playerCaptionsTracklistRenderer\":{\"captionTracks\":" + tracks + "}}}"));
        server.createContext("/api/timedtext", ex -> respond(ex, 200, ex.getRequestURI().getQuery().equals("manual")
                ? "<timedtext format=\"3\"><body><p t=\"1000\" d=\"2000\">사람 자막</p></body></timedtext>"
                : "<timedtext format=\"3\"><body><p t=\"1000\" d=\"2000\">자동 자막</p></body></timedtext>"));
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    @Test
    void 영상_정보와_사람이_단_한국어_자막을_먼저_가져온다() {
        YoutubeVideo video = new YoutubeClient(base, new ObjectMapper()).fetch("pSUydWEqKwE");
        assertThat(video.title()).isEqualTo("NewJeans 'Ditto' MV");
        assertThat(video.channel()).isEqualTo("HYBE LABELS");
        assertThat(video.embeddable()).isTrue();
        assertThat(video.lengthSec()).isEqualTo(333);
        assertThat(video.captions().kind()).isEqualTo("manual");
        assertThat(video.captions().lines()).extracting(YoutubeVideo.Line::text).containsExactly("사람 자막");
    }

    @Test
    void 사람_자막이_없으면_자동_자막() {
        tracks = "[{\"languageCode\":\"ko\",\"kind\":\"asr\",\"baseUrl\":\"" + base + "/api/timedtext?asr\"}]";
        YoutubeVideo video = new YoutubeClient(base, new ObjectMapper()).fetch("pSUydWEqKwE");
        assertThat(video.captions().kind()).isEqualTo("asr");
        assertThat(video.captions().lines().get(0).text()).isEqualTo("자동 자막");
    }

    @Test
    void 한국어_자막이_없으면_captions는_null() {
        tracks = "[{\"languageCode\":\"en\",\"baseUrl\":\"" + base + "/api/timedtext?en\"}]";
        assertThat(new YoutubeClient(base, new ObjectMapper()).fetch("pSUydWEqKwE").captions()).isNull();
    }

    @Test
    void 퍼가기가_막히면_embeddable_false이고_제목은_player에서() {
        oembedStatus = 401;
        YoutubeVideo video = new YoutubeClient(base, new ObjectMapper()).fetch("pSUydWEqKwE");
        assertThat(video.embeddable()).isFalse();
        assertThat(video.title()).isEqualTo("Ditto");
    }

    @Test
    void 유튜브에_연결이_안_되면_YOUTUBE_ERROR() {
        YoutubeClient down = new YoutubeClient("http://127.0.0.1:1", new ObjectMapper());
        assertThatThrownBy(() -> down.fetch("pSUydWEqKwE"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.YOUTUBE_ERROR);
    }
}
