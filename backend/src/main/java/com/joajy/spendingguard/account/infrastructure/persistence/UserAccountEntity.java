package com.joajy.spendingguard.account.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import com.joajy.spendingguard.account.domain.model.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** 사용자 계정을 {@code user_account} 테이블에 매핑하는 JPA 엔티티다. */
@Entity
@Table(name = "user_account")
public class UserAccountEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 320, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserAccountEntity() {
    }

    private UserAccountEntity(UserAccount account) {
        this.id = account.id();
        this.email = account.email();
        this.passwordHash = account.passwordHash();
        this.createdAt = account.createdAt();
    }

    static UserAccountEntity from(UserAccount account) {
        return new UserAccountEntity(account);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
