package com.kingsejong.domain.content.youtube;

import com.kingsejong.domain.content.youtube.dto.YoutubeImportRequest;
import com.kingsejong.domain.content.youtube.dto.YoutubeImportResponse;
import com.kingsejong.domain.content.youtube.dto.YoutubePreviewRequest;
import com.kingsejong.domain.content.youtube.dto.YoutubePreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자: 유튜브 영상으로 학습 콘텐츠 만들기. 헤더 X-Admin-Key 필요. */
@Tag(name = "Admin", description = "관리자 (X-Admin-Key)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminContentController {

    private final YoutubeContentService youtubeContentService;

    @Operation(summary = "유튜브 미리 보기", description = "영상 정보, 한국어 자막 전체, 추천 구간·쓸 글")
    @PostMapping("/youtube/preview")
    public YoutubePreviewResponse preview(@Valid @RequestBody YoutubePreviewRequest request) {
        return youtubeContentService.preview(request.url());
    }

    @Operation(summary = "유튜브로 콘텐츠 만들기",
            description = "url·mode·taste만 넣으면 구간과 쓸 글을 자막에서 추천해 만든다. 같은 영상·시작 초는 덮어쓴다. dryRun이면 저장 안 함")
    @PostMapping("/contents/youtube")
    @ResponseStatus(HttpStatus.CREATED)
    public YoutubeImportResponse importContent(@Valid @RequestBody YoutubeImportRequest request) {
        return youtubeContentService.importContent(request);
    }

    @Operation(summary = "콘텐츠 내리기", description = "목록·다음 글·제출에서 빠진다 (지난 기록은 그대로)")
    @DeleteMapping("/contents/{contentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable Long contentId) {
        youtubeContentService.deactivate(contentId);
    }
}
