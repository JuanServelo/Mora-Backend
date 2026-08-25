package com.mora.portaria.security;

public record JwtClaims(String authUserId, String email, String perfil) {}

