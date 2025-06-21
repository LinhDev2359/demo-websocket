package com.eoswallet.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserLoginResponse {
    private Long userId;
    private String email;
    private String fullName;
    private String role;
    private String accessToken;
    private String refreshToken;
    private String message;
}