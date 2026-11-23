package com.kingsejong.domain.taste.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsejong.domain.taste.entity.Taste;
import com.kingsejong.domain.taste.repository.TasteRepository;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.seed.SeedReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 취향 카드 시드 (seed/tastes.json). 콘텐츠 시드가 취향 key를 가리키므로 먼저 돈다. 모든 프로필.
 * key로 찾아서 덮어쓴다(upsert). JSON에서 지운 취향은 사용자 선택이 가리킬 수 있어서 지우지 않는다.
 */
@Slf4j
@Order(1)
@Component
@RequiredArgsConstructor
public class TasteSeeder implements ApplicationRunner {

    private final SeedReader seedReader;
    private final TasteRepository tasteRepository;

    record TasteSeed(String key, Mode mode, String name, String imageUrl) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        List<TasteSeed> seeds = seedReader.read("seed/tastes.json", new TypeReference<>() {
        });
        Set<String> keys = new HashSet<>();
        Map<Mode, Integer> order = new EnumMap<>(Mode.class);
        for (TasteSeed seed : seeds) {
            if (seed.key() == null || seed.key().isBlank() || !keys.add(seed.key())) {
                throw new IllegalStateException("시드 취향 '" + seed.name() + "': key가 없거나 겹쳐요");
            }
            if (seed.mode() == null || seed.mode() == Mode.BASIC) {
                throw new IllegalStateException("시드 취향 '" + seed.name() + "': mode는 kpop 또는 kdrama");
            }
            int sortOrder = order.merge(seed.mode(), 1, Integer::sum);
            Taste taste = tasteRepository.findBySeedKey(seed.key())
                    .orElseGet(() -> Taste.builder().seedKey(seed.key()).mode(seed.mode()).name(seed.name()).build());
            taste.update(seed.mode(), seed.name(), seed.imageUrl(), sortOrder);
            tasteRepository.save(taste);
        }
        log.info("Seeded {} tastes", seeds.size());
    }
}
