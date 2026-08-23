package com.joajy.spendingguard.auth.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** JWT subject와 요청 대상 사용자가 같은지 판단한다. */
@Component("userScope")
public class UserScopeAuthorization {
    public boolean matches(Authentication authentication, UUID userId) {
        return authentication != null
                && authentication.isAuthenticated()
                && userId != null
                && userId.toString().equals(authentication.getName());
    }
}
