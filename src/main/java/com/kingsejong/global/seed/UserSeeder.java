package com.kingsejong.global.seed;

import com.fasterxml.jackson.core.type.TypeReference;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

/**
 * 개발용 계정 시드 (seed/users.json). users가 비어 있을 때만 넣는다.
 * local·test에서만 돈다. 시드 로그인 코드는 맞히기 쉬워서, 배포(prod) 계정은 무작위 코드로 따로 넣는다.
 */
@Slf4j
@Component
@Profile({"local", "test"})
@RequiredArgsConstructor
public class UserSeeder implements ApplicationRunner {

    private final SeedReader seedReader;
    private final UserRepository userRepository;

    record UserSeed(String name, String loginCode, Locale locale, Experience experience, Stage stage,
                    Mode lastMode, boolean consentAgreed, boolean onboardingCompleted) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        if (userRepository.count() > 0) {
            return;
        }
        List<UserSeed> users = seedReader.read("seed/users.json", new TypeReference<>() {
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
