package com.kingsejong.domain.taste.controller;

import com.kingsejong.domain.taste.dto.request.TasteSaveRequest;
import com.kingsejong.domain.taste.dto.response.TasteListResponse;
import com.kingsejong.domain.taste.service.TasteService;
import com.kingsejong.global.auth.LoginUser;
import com.kingsejong.global.common.Mode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Taste", description = "취향 카드 (가수·작품)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TasteController {

    private final TasteService tasteService;

    @Operation(summary = "취향 카드 목록", description = "mode = kpop(가수) | kdrama(작품). 내가 고른 것은 selected = true")
    @GetMapping("/tastes")
    public TasteListResponse getTastes(@Parameter(hidden = true) @LoginUser Long userId,
                                       @RequestParam Mode mode) {
        return tasteService.getTastes(userId, mode);
    }

    @Operation(summary = "취향 저장", description = "그 모드의 선택을 통째로 바꾼다. 응답 204")
    @PutMapping("/me/tastes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveTastes(@Parameter(hidden = true) @LoginUser Long userId,
                           @Valid @RequestBody TasteSaveRequest request) {
        tasteService.saveTastes(userId, request);
    }
}
