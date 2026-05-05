package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(name = "ImportResult", description = "Result of an Excel bulk-import operation")
public class ImportResultDto {

    @Schema(description = "Number of rows successfully imported", example = "5")
    private final int successCount;

    @Schema(description = "Number of rows that failed", example = "2")
    private final int failCount;

    @Schema(description = "Error details per failed row", example = "[\"Satır 3: Ürün kodu boş olamaz\"]")
    private final List<String> errors;

    public ImportResultDto(int successCount, List<String> errors) {
        this.successCount = successCount;
        this.errors = errors;
        this.failCount = errors.size();
    }
}
