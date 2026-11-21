package com.kingsejong.domain.writing.controller;

import com.kingsejong.domain.writing.dto.request.WritingCreateRequest;
import com.kingsejong.domain.writing.dto.response.WritingResponse;
import com.kingsejong.domain.writing.service.WritingService;
import com.kingsejong.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Writing", description = "쓰기 제출과 판정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/writings")
public class WritingController {

    private final WritingService writingService;

    @Operation(summary = "쓰기 제출", description = "원고지 필기를 받아 교정 엔진 판정 결과를 돌려준다. 정답 문장은 서버가 contentId + stage로 찾는다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WritingResponse submit(@Parameter(hidden = true) @LoginUser Long userId,
                                  @Valid @RequestBody WritingCreateRequest request) {
        return writingService.submit(userId, request);
    }
}
