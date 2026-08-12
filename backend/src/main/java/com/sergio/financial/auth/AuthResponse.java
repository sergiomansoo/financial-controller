package com.sergio.financial.auth;

public record AuthResponse(String accessToken, String tokenType, AuthUser user) {
    public record AuthUser(Long id, String name, String email) {
    }
}
