package com.kingsejong.domain.reference.controller;

import com.kingsejong.domain.reference.dto.StrokeReferenceResponse;
import com.kingsejong.domain.reference.service.ReferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@Tag(name = "Reference", description = "정답 획 데이터 (획순 애니메이션)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reference")
public class ReferenceController {

    private final ReferenceService referenceService;

    @Operation(summary = "정답 획", description = "chars = 공백 없는 한글 음절·자모 1~30자. 사전에 없는 글자는 items에서 빠진다. 하루 캐시")
    @GetMapping("/strokes")
    public ResponseEntity<StrokeReferenceResponse> getStrokes(@RequestParam String chars) {
        ReferenceService.Result result = referenceService.getStrokes(chars);
        // 빠진 글자가 있으면 브라우저가 그 빈 결과를 하루 동안 붙들지 않게 캐시하지 않는다
        CacheControl cache = result.complete()
                ? CacheControl.maxAge(Duration.ofDays(1)).cachePrivate()
                : CacheControl.noStore();
        return ResponseEntity.ok().cacheControl(cache).body(result.response());
    }
}
