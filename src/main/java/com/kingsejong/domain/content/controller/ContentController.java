package com.kingsejong.domain.content.controller;

import com.kingsejong.domain.content.dto.response.ContentDetailResponse;
import com.kingsejong.domain.content.dto.response.ContentListResponse;
import com.kingsejong.domain.content.service.ContentService;
import com.kingsejong.global.auth.LoginUser;
import com.kingsejong.global.common.Mode;
import com.kingsejong.global.common.Stage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Content", description = "콘텐츠 (클립, 기본 연습)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/contents")
public class ContentController {

    private final ContentService contentService;

    @Operation(summary = "콘텐츠 목록", description = "mode = kpop | kdrama | basic. stage를 빼면 내 지금 난이도.")
    @GetMapping
    public ContentListResponse getContents(@Parameter(hidden = true) @LoginUser Long userId,
                                           @RequestParam Mode mode,
                                           @RequestParam(required = false) Stage stage) {
        return contentService.getContents(userId, mode, stage);
    }

    @Operation(summary = "콘텐츠 상세", description = "클립, 자막, 이 난이도에서 쓸 글")
    @GetMapping("/{contentId}")
    public ContentDetailResponse getContent(@Parameter(hidden = true) @LoginUser Long userId,
                                            @PathVariable Long contentId,
                                            @RequestParam(required = false) Stage stage) {
        return contentService.getContent(userId, contentId, stage);
    }
}
