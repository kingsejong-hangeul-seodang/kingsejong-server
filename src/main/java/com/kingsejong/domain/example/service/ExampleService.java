package com.kingsejong.domain.example.service;

import com.kingsejong.domain.example.dto.request.ExampleCreateRequest;
import com.kingsejong.domain.example.dto.response.ExampleResponse;
import com.kingsejong.domain.example.entity.Example;
import com.kingsejong.domain.example.repository.ExampleRepository;
import com.kingsejong.global.error.BusinessException;
import com.kingsejong.global.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 구조 예시용 서비스 (실제 도메인 개발을 시작하면 example 패키지를 통째로 지운다).
 *
 * - 조회는 readOnly 트랜잭션, 쓰기는 @Transactional
 * - 다른 도메인의 데이터가 필요하면 그쪽 Repository 가 아니라 Service 를 주입한다
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional
    public ExampleResponse create(ExampleCreateRequest request) {
        Example saved = exampleRepository.save(Example.create(request.name()));
        return ExampleResponse.from(saved);
    }

    public ExampleResponse findById(Long exampleId) {
        Example example = exampleRepository.findById(exampleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "예시를 찾을 수 없어요"));
        return ExampleResponse.from(example);
    }

    public List<ExampleResponse> findAll() {
        return exampleRepository.findAll().stream()
                .map(ExampleResponse::from)
                .toList();
    }
}
