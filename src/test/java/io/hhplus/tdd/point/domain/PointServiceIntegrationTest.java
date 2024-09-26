package io.hhplus.tdd.point.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    PointService pointService;

    @Autowired
    UserPointRepository pointRepository;

    @Autowired
    PointHistoryRepository pointHistoryRepository;

    // 10명의 유저 생성 및 포인트 초기화
    @BeforeEach
    void init() {
        for (long i = 1; i <= 10; i++) {
            pointRepository.insertOrUpdate(i, 100);
        }
    }

    @Test
    @DisplayName("동시에 서로 다른 유저가 포인트를 충전, 사용 요청한 경우 순차적으로 처리한다.")
    void 포인트_충전_사용_동시성_테스트() throws InterruptedException {
        ExecutorService es = Executors.newFixedThreadPool(20);

        // 각 유저가 100, 200, 300 포인트를 차례로 사용
        List<Callable<Void>> tasks = new ArrayList<>();

        for (long i = 1; i <= 10; i++) {
            long id = i;
            tasks.add(() -> {
                pointService.charge(id, 100);
                pointService.use(id, 200);
                pointService.use(id, 100);
                return null;
            });
        }

        // 모든 작업을 동시에 실행
        es.invokeAll(tasks);

        // 각 유저의 포인트 사용 기록과 최종 잔액 검증
        for (long i = 1; i <= 10; i++) {
            UserPoint userPoint = pointRepository.selectById(i);
            List<PointHistory> histories = pointHistoryRepository.selectAllByUserId(i);

            // 최종 잔액 검증 (100 + 100 - 200 - 100(취소) = 0)
            assertEquals(0, userPoint.point(), "유저 " + i + "의 잔액이 맞지 않습니다.");

            System.out.println("유저 " + i + "의 포인트 사용 기록: " + histories);
        }

        es.shutdown();
    }
}
