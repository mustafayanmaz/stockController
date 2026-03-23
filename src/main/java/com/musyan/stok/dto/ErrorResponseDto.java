package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(
        name = "ErrorResponse",
        description = "Error response DTO"
)
public class ErrorResponseDto {

    @Schema(description = "API path", example = "/api/products")
    private String apiPath;

    @Schema(description = "HTTP status code", example = "400")
    private Integer errorCode;

    @Schema(description = "Error message", example = "Validation failed")
    private String errorMessage;

    @Schema(description = "Error time", example = "2026-03-15T14:30:00")
    private LocalDateTime errorTime;
}