package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceV1Test {

    @InjectMocks
    private PointServiceImplV1 pointService;

    @Mock
    private UserPointTable userPointTable;

    @Mock
    private PointHistoryTable pointHistoryTable;

    UserPoint userPoint;

    @BeforeEach
    void init() {
        long id = 1;
        long point = 500;
        long now = System.currentTimeMillis();

        userPoint = new UserPoint(id, point, now);
    }


    @Test
    void 포인트_조회() {
        when(userPointTable.selectById(userPoint.id()))
                .thenReturn(userPoint);

        UserPoint result = pointService.getUserPoint(userPoint.id());
        assertThat(result.id()).isSameAs(userPoint.id());
    }

    @Test
    @DisplayName("양수 금액 포인트 충전")
    void 포인트_충전() {
        // given
        long amount = 1000;

        when(userPointTable.selectById(userPoint.id()))
                .thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() + amount))
                .thenReturn(new UserPoint(userPoint.id(), userPoint.point() + amount, userPoint.updateMillis()));

        // when
        UserPoint result = pointService.chargeUserPoint(userPoint.id(), amount);

        // then
        assertThat(result.point()).isEqualTo(userPoint.point() + amount);
    }

    @Test
    @DisplayName("음수 금액 포인트 충전")
    void 음수_금액_포인트_충전() {
        // given
        long amount = -1;

        when(userPointTable.selectById(userPoint.id()))
                .thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() + amount))
                .thenReturn(new UserPoint(userPoint.id(), userPoint.point() + amount, System.currentTimeMillis()));

        // when, then
        Assertions.assertThrows(RuntimeException.class,
                () -> pointService.chargeUserPoint(userPoint.id(), amount));
    }

    @Test
    @DisplayName("충분한 잔액이 있는 경우 포인트 사용")
    void 포인트_사용() {
        // given
        long amount = 500;

        when(userPointTable.selectById(userPoint.id()))
                .thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userPoint.id(), userPoint.point() - amount))
                .thenReturn(new UserPoint(userPoint.id(), userPoint.point() - amount, System.currentTimeMillis()));

        // when
        UserPoint result = pointService.useUserPoint(userPoint.id(), amount);

        // then
        assertThat(result.point()).isEqualTo(userPoint.point() - amount);
    }

    @Test
    @DisplayName("충분한 잔액이 없는 경우 포인트 사용")
    void 잔액이_부족한_포인트_사용() {
        // given
        long amount = 1000;

        when(userPointTable.selectById(userPoint.id()))
                .thenReturn(userPoint);

        // when, then
        Assertions.assertThrows(RuntimeException.class,
                () -> pointService.useUserPoint(userPoint.id(), amount));
    }

    @Test
    void 포인트_거래_내역_조회() {
        // given
        when(pointHistoryTable.selectAllByUserId(userPoint.id()))
                .thenReturn(List.of(
                        new PointHistory(1, 1, 1000, TransactionType.CHARGE, System.currentTimeMillis()),
                        new PointHistory(1, 1, 500, TransactionType.USE, System.currentTimeMillis())
                ));

        // when
        List<PointHistory> result = pointService.getPointHistories(userPoint.id());

        // then
        assertThat(result).hasSize(2);
    }
}