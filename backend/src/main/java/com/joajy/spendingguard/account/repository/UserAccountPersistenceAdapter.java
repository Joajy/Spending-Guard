package com.joajy.spendingguard.account.repository;

import com.joajy.spendingguard.account.service.exception.DuplicateEmailException;
import com.joajy.spendingguard.account.service.port.outbound.StoreUserAccountPort;
import com.joajy.spendingguard.account.domain.model.UserAccount;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** PostgreSQL 이메일 고유 제약을 계정 중복이라는 업무 의미로 변환하는 영속성 어댑터다. */
@Component
class UserAccountPersistenceAdapter implements StoreUserAccountPort {

    private final UserAccountJpaRepository repository;

    UserAccountPersistenceAdapter(UserAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void store(UserAccount account) {
        try {
            repository.saveAndFlush(UserAccountEntity.from(account));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateEmailException();
        }
    }
}
