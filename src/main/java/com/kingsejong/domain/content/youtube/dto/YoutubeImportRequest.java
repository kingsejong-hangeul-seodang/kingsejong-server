package com.kingsejong.domain.content.youtube.dto;

import com.kingsejong.global.common.Mode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 유튜브 영상으로 콘텐츠 만들기. url·mode·taste만 넣으면 나머지는 자막에서 추천한다.
 * startSec·endSec는 둘 다 주거나 둘 다 빼고(자동), tasks는 난이도별로 직접 넣으면 추천값 대신 쓴다.
 * dryRun = true면 저장하지 않고 만들어질 모양만 돌려준다.
 */
public record YoutubeImportRequest(
        @NotBlank(message = "유튜브 주소가 필요해요") String url,
        @NotNull(message = "kpop 또는 kdrama") Mode mode,
        @NotBlank(message = "가수·작품 이름이 필요해요") @Size(max = 50) String taste,
        @Size(max = 100) String title,
        @Size(max = 200) String source,
        @Size(max = 300) String situation,
        @DecimalMin("0") Double startSec,
        @DecimalMin("0") Double endSec,
        @Valid Tasks tasks,
        boolean dryRun
) {

    public record Tasks(@Valid Task beginner, @Valid Task intermediate, @Valid Task advanced) {
    }

    /** target은 엔진 규칙대로 (초보 띄어쓰기 없는 단어, 중수 3어절 이하, 고수 20칸 이하). meaning은 초보 뜻. */
    public record Task(@NotBlank @Size(max = 20) String target, @Size(max = 100) String meaning) {
    }
}
