package com.kingsejong.domain.jamo.controller;

import com.kingsejong.domain.jamo.dto.JamoDetailResponse;
import com.kingsejong.domain.jamo.dto.JamoListResponse;
import com.kingsejong.domain.jamo.service.JamoService;
import com.kingsejong.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Jamo", description = "자모 도감")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jamo")
public class JamoController {

    private final JamoService jamoService;

    @Operation(summary = "자모 도감", description = "자음 19 · 모음 21과 내 숙련도")
    @GetMapping
    public JamoListResponse getDogam(@Parameter(hidden = true) @LoginUser Long userId) {
        return jamoService.getDogam(userId);
    }

    @Operation(summary = "자모 상세", description = "jamo는 자모 한 글자를 URL 인코딩 (ㄷ → %E3%84%B7)")
    @GetMapping("/{jamo}")
    public JamoDetailResponse getDetail(@Parameter(hidden = true) @LoginUser Long userId,
                                        @PathVariable String jamo) {
        return jamoService.getDetail(userId, jamo);
    }
}
