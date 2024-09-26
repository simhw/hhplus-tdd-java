package io.hhplus.tdd.point.domain;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class PointServiceTest {

    @Mock
    private UserPointRepository userPointRepository;

    @Mock
    private PointHistoryRepository pointHistoryRepository;

    @InjectMocks
    private PointService pointService;

    @Test
    @DisplayName("유효한 금액 전달 시 금액이 기존 잔고에 증액된다.")
    void 포인트_충전() {
        // given
        long id = 1;
        long point = 0;
        long now = System.currentTimeMillis();
        UserPoint userPoint = new UserPoint(id, point, now);

        long amount = 500;

        when(userPointRepository.selectById(id))
                .thenReturn(userPoint);

        when(userPointRepository.insertOrUpdate(id, amount))
                .thenReturn(new UserPoint(id, point + amount, now));

        when(pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, now))
                .thenReturn(null);

        // when
        UserPoint result = pointService.charge(new PointRequest(id, amount));

        // then
        assertThat(result.point()).isEqualTo(point + amount);
    }

    @Test
    @DisplayName("음수 금액 전달 시 RuntimeException 에러가 발생한다.")
    void 음수_포인트_충전() {
        // given
        long id = 1;
        long amount = -1;

        // when, then
        Assertions.assertThrows(RuntimeException.class,
                () -> pointService.charge(new PointRequest(id, amount)));
    }


    @Test
    @DisplayName("유효한 금액 전달 시 금액이 잔고에 차감된다.")
    void 포인트_사용() {
        // given
        long id = 1;
        long point = 100;
        long now = System.currentTimeMillis();
        UserPoint userPoint = new UserPoint(id, point, now);

        long amount = 100;

        when(userPointRepository.selectById(id))
                .thenReturn(userPoint);

        when(userPointRepository.insertOrUpdate(id, point - amount))
                .thenReturn(new UserPoint(id, point - amount, now));

        when(pointHistoryRepository.insert(id, amount, TransactionType.USE, now))
                .thenReturn(null);

        // when
        UserPoint result = pointService.use(new PointRequest(id, amount));

        // then
        assertThat(result.point()).isEqualTo(point - amount);
    }

    @Test
    @DisplayName("음수 금액 전달 시 RuntimeException 에러가 발생한다.")
    void 음수_포인트_사용() {
        // given
        long id = 1;
        long amount = -1;

        // when, then
        Assertions.assertThrows(RuntimeException.class,
                () -> pointService.use(new PointRequest(id, amount)));
    }

    @Test
    @DisplayName("잔고보다 큰 금액 전달 시 RuntimeException 에러가 발생한다.")
    void 잔고_부족_포인트_사용() {
        // given
        long id = 1;
        long point = 0;
        UserPoint userPoint = new UserPoint(id, point, System.currentTimeMillis());

        long amount = 100;

        when(userPointRepository.selectById(id))
                .thenReturn(userPoint);

        // when, then
        Assertions.assertThrows(RuntimeException.class,
                () -> pointService.use(new PointRequest(id, amount)));
    }
}