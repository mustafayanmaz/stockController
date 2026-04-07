package com.musyan.stok.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(name = "AuthRequest", description = "DTO for authentication request")
public class AuthRequestDto {

    @NotBlank(message = "Username cannot be blank")
    @Schema(description = "Username", example = "admin")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    @Schema(description = "Password", example = "admin")
    private String password;
}
