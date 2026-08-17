package com.joajy.spendingguard.spendevent.application.port.outbound;

import com.joajy.spendingguard.spendevent.domain.model.RawSpendEvent;

/**
 * 정제된 원천 소비 이벤트를 영속화하기 위한 출력 포트다.
 * 저장 기술과 중복 제약 처리 방식은 어댑터에 맡기고 유스케이스에는 도메인 모델 기준의 계약만 제공한다.
 */
public interface StoreRawSpendEventPort {

    void store(RawSpendEvent spendEvent);
}
