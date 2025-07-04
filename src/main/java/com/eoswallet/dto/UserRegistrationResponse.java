package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserRegistrationResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String accessToken;
    private String refreshToken;
    private String message;
}