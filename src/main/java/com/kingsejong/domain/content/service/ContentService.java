package com.kingsejong.domain.content.service;

import com.kingsejong.domain.content.dto.response.ContentDetailResponse;
import com.kingsejong.domain.content.dto.response.ContentListResponse;
import com.kingsejong.domain.content.entity.Content;
import com.kingsejong.domain.content.entity.ContentKind;
import com.kingsejong.domain.content.entity.ContentTask;
import com.kingsejong.domain.content.repository.ContentRepository;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.taste.service.TasteService;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.domain.writing.service.WritingStatsService;
import com.kingsejong.global.common.Hangul;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.Optional;

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
        Set<Long> myTastes = tasteService.selectedTasteIds(userId);
        List<Content> contents = orderedContents(mode, resolved, myTastes);

        // contentId → perfect 여부. 키가 있으면 이 난이도로 한 번이라도 맞게 쓴 것
        Map<Long, Boolean> completion = writingStatsService.findCompletion(
                userId, resolved, contents.stream().map(Content::getId).toList());

        List<ContentListResponse.Item> items = contents.stream()
                .map(content -> ContentListResponse.Item.of(
                        content,
                        content.taskOf(resolved).orElseThrow(),
                        matches(content, myTastes),
                        completion.containsKey(content.getId()),
                        completion.getOrDefault(content.getId(), false)))
                .toList();
        return new ContentListResponse(items);
    }

    /** 목록 순서: 고른 가수·작품의 콘텐츠가 먼저, 같은 그룹 안에서는 시드 순서. "다음 글"도 이 순서를 따른다. */
    private List<Content> orderedContents(Mode mode, Stage stage, Set<Long> myTastes) {
        return contentRepository.findAllByModeAndStage(mode, stage).stream()
                .sorted(Comparator.comparing((Content content) -> !matches(content, myTastes)))
                .toList();
    }

    public ContentDetailResponse getContent(Long userId, Long contentId, Stage stage) {
        Stage resolved = resolveStage(userId, stage);
        Content content = getContentEntity(contentId);
        ContentTask task = getTask(content, resolved);
        return ContentDetailResponse.of(content, task, findNextContentId(userId, content, resolved));
    }

    /**
     * 쓰기 제출에서 정답 문장을 찾을 때. 콘텐츠가 없거나, 숨긴(active = false) 콘텐츠거나, 그 난이도로 쓸 글이 없으면 404.
     * 숨긴 콘텐츠도 상세는 열린다(지난 기록 화면용). 제출만 막는다.
     */
    public WritingTarget getWritingTarget(Long userId, Long contentId, Stage stage) {
        Content content = getContentEntity(contentId);
        if (!content.isActive()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "지금은 쓸 수 없는 콘텐츠예요");
        }
        ContentTask task = getTask(content, stage);
        return new WritingTarget(content.getId(), task.getTarget(), findNextContentId(userId, content, stage));
    }

    /**
     * 기록 화면용 콘텐츠 정보. (contentId, stage) 쌍마다 그 난이도의 카드 제목을 쓴다. 숨긴 콘텐츠도 준다(지난 기록).
     * 키는 contentId + ":" + stage.code
     */
    public Map<String, ContentSummary> getSummaries(Collection<Long> contentIds, Collection<Stage> stages) {
        Map<String, ContentSummary> result = new HashMap<>();
        for (Content content : contentRepository.findAllById(contentIds)) {
            for (Stage stage : stages) {
                String title = content.taskOf(stage).map(ContentTask::getTitle).filter(t -> t != null)
                        .orElse(content.getTitle());
                result.put(content.getId() + ":" + stage.getCode(),
                        new ContentSummary(content.getId(), title, content.getSource(), content.getMode()));
            }
        }
        return result;
    }

    /** 자모 상세의 [연습하기]: 그 자모를 쓰는 초보 자모 연습 콘텐츠 (보이는 것만). */
    public Optional<Long> findJamoPracticeContentId(String jamo) {
        return contentRepository.findAllByModeAndStage(Mode.BASIC, Stage.BEGINNER).stream()
                .filter(c -> c.getKind() == ContentKind.JAMO)
                .filter(c -> c.taskOf(Stage.BEGINNER).map(t -> t.getTarget().equals(jamo)).orElse(false))
                .map(Content::getId)
                .findFirst();
    }

    /** 자모 상세의 예시 글자: 보이는 콘텐츠의 정답 문장에서 이 자모가 든 음절을 limit개까지 (나온 순서). */
    public List<String> findExampleChars(String jamo, int limit) {
        java.util.LinkedHashSet<String> chars = new java.util.LinkedHashSet<>();
        for (Mode mode : Mode.values()) {
            for (Stage stage : Stage.values()) {
                for (Content content : contentRepository.findAllByModeAndStage(mode, stage)) {
                    String target = content.taskOf(stage).map(ContentTask::getTarget).orElse("");
                    for (int i = 0; i < target.length() && chars.size() < limit; i++) {
                        String ch = String.valueOf(target.charAt(i));
                        if (ch.charAt(0) >= '가' && ch.charAt(0) <= '힣' && Hangul.toJamo(ch).contains(jamo)) {
                            chars.add(ch);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(chars);
    }

    /** 같은 모드에서 이 난이도로 쓸 수 있는 다음 콘텐츠 (내 목록 순서 기준). 마지막이거나 목록에 없으면 null. */
    private Long findNextContentId(Long userId, Content content, Stage stage) {
        List<Content> sameMode = orderedContents(content.getMode(), stage, tasteService.selectedTasteIds(userId));
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
