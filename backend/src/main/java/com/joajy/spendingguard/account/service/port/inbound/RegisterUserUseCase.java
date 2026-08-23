package com.joajy.spendingguard.account.service.port.inbound;

import com.joajy.spendingguard.account.service.command.RegisterUserCommand;
import com.joajy.spendingguard.account.service.result.UserRegistration;

/** 새 계정을 등록하고 공개 가능한 계정 정보만 반환하는 입력 포트다. */
public interface RegisterUserUseCase {

    UserRegistration register(RegisterUserCommand command);
}
