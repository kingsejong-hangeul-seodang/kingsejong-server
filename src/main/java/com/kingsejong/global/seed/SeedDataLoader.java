package com.kingsejong.global.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kingsejong.domain.user.entity.Experience;
import com.kingsejong.domain.user.entity.Locale;
import com.kingsejong.domain.user.entity.User;
import com.kingsejong.domain.user.repository.UserRepository;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * 서버가 켜질 때 비어 있는 테이블에 시드 데이터를 넣는다 (src/main/resources/seed/*.json).
 * 이미 데이터가 있으면 건드리지 않는다. 시드를 바꿨으면 DB를 초기화(docker-compose down -v)하고 다시 켠다.
 * local·test에서만 돈다. 배포(prod) 계정은 무작위 로그인 코드로 따로 넣는다 (시드 코드는 맞히기 쉬움).
 */
@Slf4j
@Component
@Profile({"local", "test"})
@RequiredArgsConstructor
public class SeedDataLoader implements ApplicationRunner {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    record UserSeed(String name, String loginCode, Locale locale, Experience experience, Stage stage,
                    Mode lastMode, boolean consentAgreed, boolean onboardingCompleted) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (userRepository.count() == 0) {
            List<UserSeed> users = read("seed/users.json", new TypeReference<>() {
            });
            users.forEach(seed -> userRepository.save(User.builder()
                    .name(seed.name())
                    .loginCode(seed.loginCode())
                    .locale(seed.locale())
                    .experience(seed.experience())
                    .stage(seed.stage())
                    .lastMode(seed.lastMode())
                    .consentAgreed(seed.consentAgreed())
                    .onboardingCompleted(seed.onboardingCompleted())
                    .build()));
            log.info("Seeded {} users", users.size());
        }
    }

    private <T> T read(String path, TypeReference<T> type) throws IOException {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, type);
        }
    }
}
