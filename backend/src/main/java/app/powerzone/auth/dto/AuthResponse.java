package app.powerzone.auth.dto;

import app.powerzone.users.UserResponse;

/** Respuesta de sesion. El refresh token viaja en una cookie HttpOnly. */
public record AuthResponse(String accessToken, long expiresIn, UserResponse user) {
}
