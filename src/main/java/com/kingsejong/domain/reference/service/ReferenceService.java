package com.kingsejong.domain.reference.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kingsejong.domain.reference.dto.StrokeReferenceResponse;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import com.kingsejong.infra.engine.EngineClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 정답 획 데이터. 사전(약 68MB)은 엔진에만 있고, 서버는 엔진에 물어서 받은 글자를 메모리에 기억해 둔다 (API 명세 3.11).
 * 같은 글자는 엔진에 다시 묻지 않는다. 사전에 없는 글자도 "없음"으로 기억한다.
 * 정답 획은 엔진을 다시 배포할 때만 바뀌므로, 그때 서버도 다시 시작해 기억을 비운다.
 */
@Service
@RequiredArgsConstructor
public class ReferenceService {

    static final int MAX_CHARS = 30;
    private static final Pattern HANGUL_CHAR = Pattern.compile("[가-힣ㄱ-ㅣ]");

    private final EngineClient engineClient;
    private final Map<String, Optional<JsonNode>> cache = new ConcurrentHashMap<>();

    @Value("${app.engine.reference-font:NotoSansKR-Light}")
    private String font;

    public StrokeReferenceResponse getStrokes(String chars) {
        List<String> unique = parse(chars);
        List<StrokeReferenceResponse.Item> items = new ArrayList<>();
        for (String ch : unique) {
            Optional<JsonNode> strokes = cache.get(ch);
            if (strokes == null) {
                // 엔진에 연결이 안 되면 여기서 503·504가 나간다 (기억에 없는 글자가 하나라도 있을 때만)
                strokes = engineClient.reference(ch);
                cache.put(ch, strokes);
            }
            strokes.ifPresent(s -> items.add(new StrokeReferenceResponse.Item(ch, s)));
        }
        return new StrokeReferenceResponse(font, items);
    }

    /** 공백 없는 한글 음절·자모 1~30자. 같은 글자는 한 번만. */
    private static List<String> parse(String chars) {
        if (chars == null || chars.isEmpty() || chars.codePointCount(0, chars.length()) > MAX_CHARS) {
            throw invalid("글자는 1~30자예요");
        }
        Set<String> unique = new LinkedHashSet<>();
        for (int i = 0; i < chars.length(); ) {
            int cp = chars.codePointAt(i);
            String ch = new String(Character.toChars(cp));
            if (!HANGUL_CHAR.matcher(ch).matches()) {
                throw invalid("한글 음절이나 자모만, 띄어쓰기 없이 보내 주세요");
            }
            unique.add(ch);
            i += Character.charCount(cp);
        }
        return new ArrayList<>(unique);
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail("chars", message)));
    }
}
