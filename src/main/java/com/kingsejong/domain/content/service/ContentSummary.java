package com.kingsejong.domain.content.service;

import com.kingsejong.global.common.Mode;

/** 기록 화면에 보여 줄 콘텐츠 정보. title은 그 난이도의 카드 제목(없으면 콘텐츠 제목). */
public record ContentSummary(Long contentId, String title, String source, Mode mode) {
}
