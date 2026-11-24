package com.kingsejong.domain.content.youtube.dto;

import jakarta.validation.constraints.NotBlank;

public record YoutubePreviewRequest(@NotBlank(message = "유튜브 주소가 필요해요") String url) {
}
