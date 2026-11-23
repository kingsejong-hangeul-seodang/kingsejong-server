package com.kingsejong.domain.writing.service;

import com.kingsejong.domain.content.service.ContentService;
import com.kingsejong.domain.content.service.ContentSummary;
import com.kingsejong.domain.writing.dto.response.WritingDetailResponse;
import com.kingsejong.domain.writing.dto.response.WritingListResponse;
import com.kingsejong.domain.writing.entity.Writing;
import com.kingsejong.domain.writing.entity.WritingStatus;
import com.kingsejong.domain.writing.repository.RecentWritingRow;
import com.kingsejong.domain.writing.repository.WritingRepository;
import com.kingsejong.domain.writing.repository.WritingRow;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** 쓰기 기록 조회 (기록 화면, 지난 결과 다시 보기). 판정이 끝난(GRADED) 기록만 보인다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WritingQueryService {

    private final WritingRepository writingRepository;
    private final ContentService contentService;

    /** 최신순. cursor는 앞 응답의 nextCursor(그 페이지 마지막 writingId). 없으면 처음부터. */
    public WritingListResponse getWritings(Long userId, String cursor, int size) {
        long before = parseCursor(cursor);
        List<WritingRow> rows = writingRepository.findGradedRows(userId, before, PageRequest.of(0, size + 1));
        boolean hasMore = rows.size() > size;
        List<WritingRow> page = hasMore ? rows.subList(0, size) : rows;

        Map<String, ContentSummary> contents = contentService.getSummaries(
                page.stream().map(WritingRow::contentId).distinct().toList(),
                page.stream().map(WritingRow::stage).distinct().toList());
        List<WritingListResponse.Item> items = page.stream()
                .map(r -> WritingListResponse.Item.of(r, contents.get(r.contentId() + ":" + r.stage().getCode())))
                .toList();
        String nextCursor = hasMore ? String.valueOf(page.get(page.size() - 1).id()) : null;
        return new WritingListResponse(items, nextCursor);
    }

    /** 판정이 끝난 내 기록 최신순 limit개 (자모 상세의 최근 기록). */
    public List<RecentWritingRow> recentGraded(Long userId, int limit) {
        return writingRepository.findRecentGraded(userId, PageRequest.of(0, limit));
    }

    /** 내 기록이 아니면 403, 없거나 판정 전·실패한 기록이면 404. */
    public WritingDetailResponse getWriting(Long userId, Long writingId) {
        Writing writing = writingRepository.findById(writingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "쓰기 기록을 찾을 수 없어요"));
        if (!writing.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "내 기록만 볼 수 있어요");
        }
        if (writing.getStatus() != WritingStatus.GRADED) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "판정이 끝나지 않은 기록이에요");
        }
        return WritingDetailResponse.from(writing);
    }

    private static long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return Long.MAX_VALUE;
        }
        try {
            long value = Long.parseLong(cursor.trim());
            if (value <= 0) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST,
                    List.of(new ErrorResponse.Detail("cursor", "앞 응답의 nextCursor를 그대로 보내 주세요")));
        }
    }
}
