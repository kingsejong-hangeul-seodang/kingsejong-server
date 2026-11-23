package com.kingsejong.domain.content.service;

import com.kingsejong.domain.content.dto.response.ContentDetailResponse;
import com.kingsejong.domain.content.dto.response.ContentListResponse;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.taste.service.TasteService;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.domain.writing.service.WritingStatsService;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContentService {

    private final ContentRepository contentRepository;
    private final UserService userService;
    private final WritingStatsService writingStatsService;
    private final TasteService tasteService;

    private static boolean matches(Content content, Set<Long> myTastes) {
        return content.getTasteId() != null && myTastes.contains(content.getTasteId());
    }

    public ContentListResponse getContents(Long userId, Mode mode, Stage stage) {
        Stage resolved = resolveStage(userId, stage);
        List<Content> contents = contentRepository.findAllByModeAndStage(mode, resolved);

        // contentId → perfect 여부. 키가 있으면 이 난이도로 한 번이라도 맞게 쓴 것
        Map<Long, Boolean> completion = writingStatsService.findCompletion(
                userId, resolved, contents.stream().map(Content::getId).toList());

        // 고른 가수·작품의 콘텐츠를 먼저 (같은 그룹 안에서는 원래 순서)
        Set<Long> myTastes = tasteService.selectedTasteIds(userId);
        List<ContentListResponse.Item> items = contents.stream()
                .sorted(Comparator.comparing((Content content) -> !matches(content, myTastes)))
                .map(content -> ContentListResponse.Item.of(
                        content,
                        content.taskOf(resolved).orElseThrow(),
                        matches(content, myTastes),
                        completion.containsKey(content.getId()),
                        completion.getOrDefault(content.getId(), false)))
                .toList();
        return new ContentListResponse(items);
    }

    public ContentDetailResponse getContent(Long userId, Long contentId, Stage stage) {
        Stage resolved = resolveStage(userId, stage);
        Content content = getContentEntity(contentId);
        ContentTask task = getTask(content, resolved);
        return ContentDetailResponse.of(content, task, findNextContentId(content, resolved));
    }

    /**
     * 쓰기 제출에서 정답 문장을 찾을 때. 콘텐츠가 없거나, 숨긴(active = false) 콘텐츠거나, 그 난이도로 쓸 글이 없으면 404.
     * 숨긴 콘텐츠도 상세는 열린다(지난 기록 화면용). 제출만 막는다.
     */
    public WritingTarget getWritingTarget(Long contentId, Stage stage) {
        Content content = getContentEntity(contentId);
        if (!content.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "지금은 쓸 수 없는 콘텐츠예요");
        }
        ContentTask task = getTask(content, stage);
        return new WritingTarget(content.getId(), task.getTarget(), findNextContentId(content, stage));
    }

    /** 같은 모드에서 이 난이도로 쓸 수 있는 다음 콘텐츠. 마지막이면 null. */
    public Long findNextContentId(Content content, Stage stage) {
        List<Content> sameMode = contentRepository.findAllByModeAndStage(content.getMode(), stage);
        for (int i = 0; i < sameMode.size() - 1; i++) {
            if (sameMode.get(i).getId().equals(content.getId())) {
                return sameMode.get(i + 1).getId();
            }
        }
        return null;
    }

    private Content getContentEntity(Long contentId) {
        return contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "콘텐츠를 찾을 수 없어요"));
    }

    private ContentTask getTask(Content content, Stage stage) {
        return content.taskOf(stage)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "이 난이도로 쓸 글이 없는 콘텐츠예요"));
    }

    /** stage를 빼면 내 지금 난이도. 온보딩 전이라 난이도가 없으면 초보. */
    private Stage resolveStage(Long userId, Stage stage) {
        if (stage != null) {
            return stage;
        }
        User user = userService.getUser(userId);
        return user.getStage() != null ? user.getStage() : Stage.BEGINNER;
    }
}
