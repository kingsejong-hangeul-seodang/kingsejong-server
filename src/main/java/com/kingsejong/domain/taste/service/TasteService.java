package com.kingsejong.domain.taste.service;

import com.kingsejong.domain.taste.dto.request.TasteSaveRequest;
import com.kingsejong.domain.taste.dto.response.TasteListResponse;
import com.kingsejong.domain.taste.entity.Taste;
import com.kingsejong.domain.taste.entity.UserTaste;
import com.kingsejong.domain.taste.repository.TasteRepository;
import com.kingsejong.domain.taste.repository.UserTasteRepository;
import com.kingsejong.domain.user.service.UserService;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import com.kingsejong.global.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TasteService {

    private final TasteRepository tasteRepository;
    private final UserTasteRepository userTasteRepository;
    private final UserService userService;

    public TasteListResponse getTastes(Long userId, Mode mode) {
        checkMode(mode);
        Set<Long> selected = selectedTasteIds(userId);
        return new TasteListResponse(tasteRepository.findAllByModeAndActiveTrueOrderBySortOrderAscIdAsc(mode).stream()
                .map(taste -> TasteListResponse.Item.of(taste, selected.contains(taste.getId())))
                .toList());
    }

    /** 그 모드의 선택을 통째로 바꾼다. 다른 모드의 선택은 그대로 둔다. */
    @Transactional
    public void saveTastes(Long userId, TasteSaveRequest request) {
        checkMode(request.mode());
        // 같은 사용자의 저장을 한 번에 하나씩 (연타·재시도가 동시에 지우고 넣다가 겹치지 않게)
        userService.getUserForUpdate(userId);
        Set<Long> requested = new LinkedHashSet<>(request.tasteIds());
        Map<Long, Taste> found = tasteRepository.findAllById(requested).stream()
                .collect(Collectors.toMap(Taste::getId, Function.identity()));
        for (Long id : requested) {
            Taste taste = found.get(id);
            if (taste == null || !taste.isActive() || taste.getMode() != request.mode()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail(
                        "tasteIds", id + "은(는) 이 모드의 취향이 아니에요")));
            }
        }
        // 숨긴 취향의 옛 선택도 같이 지운다 (나중에 다시 켜졌을 때 안 고른 선택이 되살아나지 않게)
        List<Long> modeTasteIds = tasteRepository.findAllIdsByMode(request.mode());
        if (!modeTasteIds.isEmpty()) {
            userTasteRepository.deleteByUserIdAndTasteIdIn(userId, modeTasteIds);
        }
        userTasteRepository.saveAll(requested.stream().map(id -> new UserTaste(userId, id)).toList());
    }

    /** 내가 고른 취향 id 전체 (모든 모드). 콘텐츠 목록의 tasteMatched에 쓴다. */
    public Set<Long> selectedTasteIds(Long userId) {
        return Set.copyOf(userTasteRepository.findActiveTasteIds(userId));
    }

    /** 콘텐츠 시드가 취향 key로 id를 찾을 때. 숨긴 취향은 없는 것으로 봐서 콘텐츠 시드가 기동에서 걸리게 한다. */
    public java.util.Optional<Long> findIdBySeedKey(String seedKey) {
        return tasteRepository.findBySeedKeyAndActiveTrue(seedKey).map(Taste::getId);
    }

    private static void checkMode(Mode mode) {
        if (mode == Mode.BASIC) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, List.of(new ErrorResponse.Detail(
                    "mode", "취향은 kpop, kdrama만 있어요")));
        }
    }
}
