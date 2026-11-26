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

    /**
     * 유튜브 콘텐츠를 만들 때 가수·작품 이름으로 취향을 찾고, 없으면 만든다 (key "yt:모드:이름", 시드가 지우지 않음).
     * dryRun이면 만들지 않고 null id를 돌려준다.
     */
    @Transactional
    /** imageUrl: 새로 만들 때(또는 유튜브로 만든 취향에 그림이 없을 때) 카드 그림. 영상 썸네일을 넣는다. */
    public TasteRef findOrCreate(Mode mode, String name, String imageUrl, boolean dryRun) {
        checkMode(mode);
        String normalized = normalizeName(name);
        java.util.Optional<Taste> found = tasteRepository.findFirstByModeAndNameAndActiveTrue(mode, normalized);
        if (found.isPresent()) {
            Taste taste = found.get();
            if (!dryRun && imageUrl != null && taste.getImageUrl() == null && taste.getSeedKey().startsWith("yt:")) {
                taste.update(mode, taste.getName(), imageUrl, taste.getSortOrder());
            }
            return new TasteRef(taste.getId(), taste.getSeedKey(), normalized, false);
        }
        // key 길이(50) 안에 들게 이름 대신 해시: "yt:kdrama:" + 12자
        String key = "yt:" + mode.getCode() + ":" + sha1(normalized).substring(0, 12);
        if (dryRun) {
            return new TasteRef(null, key, normalized, true);
        }
        Taste taste = tasteRepository.findBySeedKey(key)
                .orElseGet(() -> Taste.builder().seedKey(key).mode(mode).name(normalized).sortOrder(1000).build());
        taste.update(mode, normalized, imageUrl, 1000);
        tasteRepository.save(taste);
        return new TasteRef(taste.getId(), key, normalized, true);
    }

    /** 같은 이름이 공백·자모 조합(NFC/NFD) 차이로 다른 취향이 되지 않게. */
    public static String normalizeName(String name) {
        return java.text.Normalizer.normalize(name, java.text.Normalizer.Form.NFC).trim().replaceAll("\\s+", " ");
    }

    private static String sha1(String text) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1")
                    .digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** created = 이번에 새로 만들었는지 (또는 dryRun이면 새로 만들 예정인지). */
    public record TasteRef(Long id, String seedKey, String name, boolean created) {
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
