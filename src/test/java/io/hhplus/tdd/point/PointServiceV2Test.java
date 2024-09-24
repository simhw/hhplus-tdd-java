package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceV2Test {

    @Autowired
    private PointServiceImplV2 pointService;

    @Autowired
    private UserPointTable userPointTable;

    private long id = 1;
    private long point = 500;

    @BeforeEach
    void init() {
        userPointTable.insertOrUpdate(id, point);
    }

    @Test
    @DisplayName("10개 포인트 충전 동시 요청")
    void 포인트_충전() throws InterruptedException {
        // given
        long amount = 1000;

        int nThread = 10;
        ExecutorService es = Executors.newFixedThreadPool(nThread);

        // when
        for (int i = 0; i < nThread; i++) {
            es.submit(() -> {
                try {
                    pointService.chargeUserPoint(id, amount);
                } catch (Exception e) {
                    System.out.println(e);
                }
            });
        }

        es.shutdown();
        // 최대 5초 대기
        if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("didn't finish in time.");
        }

        UserPoint result = userPointTable.selectById(id);

        // then
        assertThat(result.point()).isEqualTo(point + (amount * nThread));
    }

    @Test
    @DisplayName("10개 포인트 사용 동시 요청")
    void 포인트_사용() throws InterruptedException {
        // given
        long amount = 50;

        int nThread = 10;
        ExecutorService es = Executors.newFixedThreadPool(nThread);

        // when
        for (int i = 0; i < nThread; i++) {
            es.submit(() -> {
                try {
                    pointService.useUserPoint(id, amount);
                } catch (Exception e) {
                    System.out.println(e);
                }
            });
        }

        es.shutdown();
        // 최대 5초 대기
        if (!es.awaitTermination(5, TimeUnit.SECONDS)) {
            System.out.println("didn't finish in time.");
        }

        UserPoint result = userPointTable.selectById(id);

        // then
        assertThat(result.point()).isEqualTo(point - (amount * nThread));
    }
}