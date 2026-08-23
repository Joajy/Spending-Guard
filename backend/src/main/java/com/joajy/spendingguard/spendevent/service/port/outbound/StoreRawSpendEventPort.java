package com.joajy.spendingguard.spendevent.service.port.outbound;

import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;

/**
 * 정제된 원천 소비 이벤트를 영속화하기 위한 출력 포트다.
 *
 * <p>같은 중복 키가 이미 존재하면 저장 기술의 예외를 노출하지 않고 애플리케이션의
 * 중복 접수 예외로 변환해야 한다.
 */
public interface StoreRawSpendEventPort {

    /**
     * 정제와 중복 키 계산이 끝난 원천 이벤트를 저장한다.
     *
     * @param spendEvent 저장할 원천 소비 이벤트
     * @throws com.joajy.spendingguard.spendevent.service.exception.DuplicateSpendEventException
     *     동일한 중복 키가 이미 저장된 경우
     */
    void store(RawSpendEvent spendEvent);
}
