package com.mini_wallet.user_service.repository;

import com.mini_wallet.user_service.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmail(String email);

    Optional<UserAccount> findByEmail(String email);
}
