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

import com.kingsejong.global.error.ErrorResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
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

    /** 자막 XML 상한. 영상 한 편 자막은 보통 100KB 안쪽. */
    private static final int MAX_CAPTION_BYTES = 2 * 1024 * 1024;
    private static final String ANDROID_UA = "com.google.android.youtube/20.10.38 (Linux; U; Android 14)";

    private final RestClient restClient;
    private final String baseUrl;
    private final URI base;
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
        this.base = URI.create(this.baseUrl);
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
            throw notFound();
        }
        String length = player.path("videoDetails").path("lengthSeconds").asText(null);
        return new YoutubeVideo(videoId, title, channel, thumbnail, oembed != null,
                length != null && length.matches("\\d+") ? Integer.valueOf(length) : null,
                captions(player));
    }

    /**
     * 퍼가기가 막힌 영상은 oEmbed가 401·403 → null (embeddable = false, 정보는 player에서).
     * 없는 영상(400·404)은 422, 그 밖(429·5xx·연결 실패)은 502. 일시 오류를 "퍼가기 불가"로 저장하지 않는다.
     */
    private JsonNode oembed(String videoId) {
        try {
            return restClient.get()
                    .uri(baseUrl + "/oembed?format=json&url={u}", "https://www.youtube.com/watch?v=" + videoId)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException e) {
            int code = e.getStatusCode().value();
            log.info("YouTube oEmbed {} for {}", code, videoId);
            if (code == 401 || code == 403) {
                return null;
            }
            if (code == 400 || code == 404) {
                throw notFound();
            }
            throw unavailable("oEmbed", e);
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
        URI uri = URI.create(chosen.get("baseUrl").asText());
        if (!trustedCaptionHost(uri)) {
            log.warn("YouTube caption url on unexpected host: {}", uri.getHost());
            return null;
        }
        try {
            String xml = restClient.get().uri(uri)
                    .header("User-Agent", ANDROID_UA)
                    .exchange((req, res) -> {
                        if (!res.getStatusCode().is2xxSuccessful()) {
                            throw new BusinessException(ErrorCode.YOUTUBE_ERROR);
                        }
                        return readLimited(res.getBody());
                    });
            boolean auto = chosen != manual;
            List<YoutubeVideo.Line> lines = CaptionParser.parse(xml);
            return new YoutubeVideo.Captions(chosen.path("languageCode").asText(),
                    auto ? "asr" : "manual", auto ? CaptionParser.mergeRollUp(lines) : lines);
        } catch (RestClientException e) {
            throw unavailable("captions", e);
        }
    }

    /** 응답이 준 주소를 그대로 부르므로 유튜브(또는 설정한 base-url) 호스트만. */
    private boolean trustedCaptionHost(URI uri) {
        String host = uri.getHost();
        if (host == null) {
            return false;
        }
        if (host.equalsIgnoreCase(base.getHost()) && uri.getScheme().equalsIgnoreCase(base.getScheme())) {
            return true;
        }
        return "https".equalsIgnoreCase(uri.getScheme())
                && (host.equalsIgnoreCase("youtube.com") || host.toLowerCase().endsWith(".youtube.com"));
    }

    private static String readLimited(InputStream in) throws IOException {
        byte[] bytes = in.readNBytes(MAX_CAPTION_BYTES + 1);
        if (bytes.length > MAX_CAPTION_BYTES) {
            throw new BusinessException(ErrorCode.YOUTUBE_ERROR, "자막이 너무 커요");
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static BusinessException notFound() {
        return new BusinessException(ErrorCode.INVALID_REQUEST,
                List.of(new ErrorResponse.Detail("url", "유튜브에 없는 영상이에요")));
    }

    private static BusinessException unavailable(String step, RestClientException e) {
        log.warn("YouTube {} failed: {}", step, e.getMessage());
        return new BusinessException(ErrorCode.YOUTUBE_ERROR);
    }
}
