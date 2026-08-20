package com.joajy.spendingguard.account.application.port.outbound;

import com.joajy.spendingguard.account.domain.model.UserAccount;

/** 생성된 사용자 계정을 영속화하는 출력 포트다. */
public interface StoreUserAccountPort {

    void store(UserAccount account);
}
