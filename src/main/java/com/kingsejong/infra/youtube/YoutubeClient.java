package com.kingsejong.infra.youtube;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * 유튜브에서 영상 정보와 한국어 자막을 가져온다 (관리자 콘텐츠 만들기용).
 *
 * - 제목·채널·썸네일·퍼가기 가능: 공식 oEmbed (키 필요 없음)
 * - 자막·영상 길이: 유튜브 앱이 쓰는 내부 API(youtubei/v1/player, ANDROID 클라이언트). <b>비공식</b>이라
 *   유튜브가 바꾸면 막힐 수 있다. 운영 서비스가 아니라서 쓰기로 함 (11/24). 막히면 자막 없이 직접 넣는다.
 */
@Slf4j
@Component
public class YoutubeClient {

    private static final String ANDROID_UA = "com.google.android.youtube/20.10.38 (Linux; U; Android 14)";

    private final RestClient restClient;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public YoutubeClient(@Value("${app.youtube.base-url:https://www.youtube.com}") String baseUrl,
                         ObjectMapper objectMapper) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(factory).build();
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.objectMapper = objectMapper;
    }

    public YoutubeVideo fetch(String videoId) {
        JsonNode oembed = oembed(videoId);
        JsonNode player = player(videoId);
        String title = oembed != null ? oembed.path("title").asText(null) : player.path("videoDetails").path("title").asText(null);
        String channel = oembed != null ? oembed.path("author_name").asText(null) : player.path("videoDetails").path("author").asText(null);
        String thumbnail = oembed != null ? oembed.path("thumbnail_url").asText(null)
                : "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg";
        if (title == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "유튜브에 없는 영상이에요");
        }
        String length = player.path("videoDetails").path("lengthSeconds").asText(null);
        return new YoutubeVideo(videoId, title, channel, thumbnail, oembed != null,
                length != null && length.matches("\\d+") ? Integer.valueOf(length) : null,
                captions(player));
    }

    /** 퍼가기가 막힌 영상은 oEmbed가 401·403 → null. 없는 영상(404·400)도 null (player에서 다시 확인). */
    private JsonNode oembed(String videoId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/oembed?format=json&url={u}", "https://www.youtube.com/watch?v=" + videoId)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            log.info("YouTube oEmbed {} for {}", e.getStatusCode().value(), videoId);
            return null;
        } catch (RestClientException e) {
            throw unavailable("oEmbed", e);
        }
    }

    private JsonNode player(String videoId) {
        ObjectNode body = objectMapper.createObjectNode();
        body.putObject("context").putObject("client")
                .put("clientName", "ANDROID").put("clientVersion", "20.10.38")
                .put("androidSdkVersion", 34).put("hl", "ko");
        body.put("videoId", videoId);
        try {
            JsonNode player = restClient.post()
                    .uri(baseUrl + "/youtubei/v1/player?prettyPrint=false")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("User-Agent", ANDROID_UA)
                    .body(body)
                    .retrieve().body(JsonNode.class);
            return player != null ? player : objectMapper.createObjectNode();
        } catch (RestClientException e) {
            throw unavailable("player", e);
        }
    }

    /** 한국어 자막: 사람이 단 자막(kind 없음)을 먼저, 없으면 자동 자막(asr). 없으면 null. */
    private YoutubeVideo.Captions captions(JsonNode player) {
        JsonNode tracks = player.path("captions").path("playerCaptionsTracklistRenderer").path("captionTracks");
        JsonNode manual = null;
        JsonNode asr = null;
        for (JsonNode track : tracks) {
            if (!track.path("languageCode").asText("").startsWith("ko")) {
                continue;
            }
            if ("asr".equals(track.path("kind").asText(null))) {
                asr = asr == null ? track : asr;
            } else {
                manual = manual == null ? track : manual;
            }
        }
        JsonNode chosen = manual != null ? manual : asr;
        if (chosen == null || !chosen.path("baseUrl").isTextual()) {
            return null;
        }
        try {
            String xml = restClient.get().uri(URI.create(chosen.get("baseUrl").asText()))
                    .header("User-Agent", ANDROID_UA)
                    .retrieve().body(String.class);
            List<YoutubeVideo.Line> lines = CaptionParser.parse(xml);
            return new YoutubeVideo.Captions(chosen.path("languageCode").asText(),
                    chosen == manual ? "manual" : "asr", lines);
        } catch (RestClientException e) {
            throw unavailable("captions", e);
        }
    }

    private static BusinessException unavailable(String step, RestClientException e) {
        log.warn("YouTube {} failed: {}", step, e.getMessage());
        return new BusinessException(ErrorCode.YOUTUBE_ERROR);
    }
}
