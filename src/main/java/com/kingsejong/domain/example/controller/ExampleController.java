package com.kingsejong.domain.example.controller;

import com.kingsejong.domain.example.dto.request.ExampleCreateRequest;
import com.kingsejong.domain.example.dto.response.ExampleResponse;
import com.kingsejong.domain.example.service.ExampleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 구조 예시용 컨트롤러 (실제 도메인 개발을 시작하면 example 패키지를 통째로 지운다).
 *
 * 규칙 (API 명세 "공통 규칙"):
 *  - 주소는 /api/v1/{복수형 리소스}
 *  - 응답은 감싸지 않고 데이터 그대로 (조회 200, 만들기 201)
 *  - 예외는 try-catch 하지 않고 던진다 (GlobalExceptionHandler 가 처리)
 *  - 요청 검증은 @Valid + jakarta validation (실패하면 422)
 */
@Tag(name = "Example", description = "구조 예시 API")
@RestController
@RequestMapping("/api/v1/examples")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @Operation(summary = "예시 만들기")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExampleResponse create(@Valid @RequestBody ExampleCreateRequest request) {
        return exampleService.create(request);
    }

    @Operation(summary = "예시 하나 조회")
    @GetMapping("/{exampleId}")
    public ExampleResponse findById(@PathVariable Long exampleId) {
        return exampleService.findById(exampleId);
    }

    @Operation(summary = "예시 목록 조회")
    @GetMapping
    public List<ExampleResponse> findAll() {
        return exampleService.findAll();
    }
}
