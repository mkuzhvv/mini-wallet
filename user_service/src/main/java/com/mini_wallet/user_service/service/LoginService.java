package com.mini_wallet.user_service.service;

import com.mini_wallet.user_service.security.IssuedAccessToken;
import com.mini_wallet.user_service.security.JwtTokenService;
import com.mini_wallet.user_service.security.UserPrincipal;
import com.mini_wallet.user_service.service.model.LoginResult;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;

    public LoginResult login(String email, String rawPassword) {
        Authentication loginAttempt = UsernamePasswordAuthenticationToken
                .unauthenticated(email, rawPassword);

        Authentication authentication = authenticationManager.authenticate(loginAttempt);
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserPrincipal userPrincipal)) {
            throw new IllegalStateException("Expected UserPrincipal after authentication");
        }
        IssuedAccessToken accessToken = jwtTokenService.issue(userPrincipal);

        return new LoginResult(accessToken.value(), "Bearer", accessToken.expiresIn());
    }
}
