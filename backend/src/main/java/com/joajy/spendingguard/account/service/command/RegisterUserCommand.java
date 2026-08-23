package com.joajy.spendingguard.account.service.command;

/** 검증을 마친 회원 등록 입력을 애플리케이션 계층에 전달한다. */
public record RegisterUserCommand(String email, String password) {
}
