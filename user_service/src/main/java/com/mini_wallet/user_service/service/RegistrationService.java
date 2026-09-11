package com.mini_wallet.user_service.service;

import com.mini_wallet.user_service.entity.UserAccount;
import com.mini_wallet.user_service.repository.UserAccountRepository;
import com.mini_wallet.user_service.web.error.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class RegistrationService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserAccount registerUser(String email, String rawPassword) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);

        if (userAccountRepository.existsByEmail(normalizedEmail)) {
            throw new EmailAlreadyExistsException("user with this email already exists");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        UserAccount user = UserAccount.register(normalizedEmail, passwordHash);
        UserAccount saved = userAccountRepository.save(user);

        log.info("user successfully registered with id = {}", saved.getId());
        return saved;
    }
}
