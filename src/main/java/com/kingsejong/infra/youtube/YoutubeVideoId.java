package com.kingsejong.infra.youtube;

import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 유튜브 주소에서 영상 ID(11자)를 꺼낸다. watch?v=, youtu.be/, shorts/, embed/, ID 그대로 모두 받는다. */
public final class YoutubeVideoId {

    private static final Pattern ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern IN_URL = Pattern.compile(
            "(?:v=|youtu\\.be/|/shorts/|/embed/|/live/)([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])");

    private YoutubeVideoId() {
    }

    public static String parse(String urlOrId) {
        String value = urlOrId == null ? "" : urlOrId.trim();
        if (ID.matcher(value).matches()) {
            return value;
        }
        Matcher m = IN_URL.matcher(value);
        if (m.find()) {
            return m.group(1);
        }
        throw new BusinessException(ErrorCode.INVALID_REQUEST,
                List.of(new ErrorResponse.Detail("url", "유튜브 영상 주소나 영상 ID(11자)를 넣어 주세요")));
    }
}
