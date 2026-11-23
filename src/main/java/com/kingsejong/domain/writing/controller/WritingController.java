package com.kingsejong.domain.writing.controller;

import com.kingsejong.domain.writing.dto.request.WritingCreateRequest;
import com.kingsejong.domain.writing.dto.response.WritingDetailResponse;
import com.kingsejong.domain.writing.dto.response.WritingListResponse;
import com.kingsejong.domain.writing.dto.response.WritingResponse;
import com.kingsejong.domain.writing.service.WritingQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final WritingQueryService writingQueryService;

    @Operation(summary = "쓰기 기록 목록", description = "최신순. cursor는 앞 응답의 nextCursor, size는 1~50 (기본 20)")
    @GetMapping
    public WritingListResponse getWritings(@Parameter(hidden = true) @LoginUser Long userId,
                                           @RequestParam(required = false) String cursor,
                                           @RequestParam(defaultValue = "20")
                                           @Min(value = 1, message = "1 이상이에요") @Max(value = 50, message = "50 이하예요")
                                           int size) {
        return writingQueryService.getWritings(userId, cursor, size);
    }

    @Operation(summary = "쓰기 기록 하나", description = "판정 결과와 필기 원본. 내 기록이 아니면 403")
    @GetMapping("/{writingId}")
    public WritingDetailResponse getWriting(@Parameter(hidden = true) @LoginUser Long userId,
                                            @PathVariable Long writingId) {
        return writingQueryService.getWriting(userId, writingId);
    }

    @Operation(summary = "쓰기 제출", description = "원고지 필기를 받아 교정 엔진 판정 결과를 돌려준다. 정답 문장은 서버가 contentId + stage로 찾는다.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WritingResponse submit(@Parameter(hidden = true) @LoginUser Long userId,
                                  @Valid @RequestBody WritingCreateRequest request) {
        return writingService.submit(userId, request);
    }
}
