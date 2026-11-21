package com.kingsejong.domain.content.service;

/** 쓰기 제출에 필요한 콘텐츠 정보: 정답 문장과 "다음 글". */
public record WritingTarget(Long contentId, String target, Long nextContentId) {
}
