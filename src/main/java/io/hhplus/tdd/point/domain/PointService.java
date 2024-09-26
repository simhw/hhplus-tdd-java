package io.hhplus.tdd.point.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    private final Map<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    // private final Lock lock = new ReentrantLock();

    public UserPoint detail(long id) {
        return userPointRepository.selectById(id);
    }

    /**
     * 유저의 포인트를 충전하는 기능
     */
    public UserPoint charge(PointRequest request) {
        log.info("thread: {}, id: {}, amount: {}", Thread.currentThread().getName(), request.getId(), request.getAmount());
        // 1. 유효성 검사
        request.valid();

        // 2. 임계 영역 진입을 위한 락 조회
        ReentrantLock lock = locks.computeIfAbsent(request.getId(), k -> new ReentrantLock(false));
        lock.lock();

        try {
            UserPoint userPoint = userPointRepository.selectById(request.getId());

            if (userPoint == null) {
                throw new RuntimeException("not find user point");
            }

            // 3. 포인트 증감, 포인트 값 수정
            long point = userPoint.point() + request.getAmount();

            userPoint = userPointRepository.insertOrUpdate(
                    request.getId(),
                    point
            );

            // 4. 거래 내역 로그 저장
            pointHistoryRepository.insert(
                    request.getId(),
                    request.getAmount(),
                    TransactionType.CHARGE,
                    userPoint.updateMillis()
            );

            log.info("point: {}, updateMillis: {}", userPoint.point(), userPoint.updateMillis());
            return userPoint;

        } finally {
            lock.unlock();
        }

    }

    /**
     * 유저의 포인트를 사용하는 기능
     */
    public UserPoint use(PointRequest request) {
        log.info("thread: {}, id: {}, amount: {}", Thread.currentThread().getName(), request.getId(), request.getAmount());
        // 1. 유효성 검사
        request.valid();

        // 2. 임계 영역 진입을 위한 락 조회
        ReentrantLock lock = locks.computeIfAbsent(request.getId(), k -> new ReentrantLock(false));
        lock.lock();

        try {
            UserPoint userPoint = userPointRepository.selectById(request.getId());

            if (userPoint == null) {
                throw new RuntimeException("not find user point");
            }

            // 잔고보다 차감 포인트 금액이 더 큰 경우 취소
            if (userPoint.point() < request.getAmount()) {
                throw new RuntimeException("not enough point");
            }

            // 3. 포인트 차감, 포인트 값 수정
            long point = userPoint.point() - request.getAmount();

            userPoint = userPointRepository.insertOrUpdate(
                    request.getId(),
                    point
            );

            // 4. 거래 내역 로그 저장
            pointHistoryRepository.insert(
                    request.getId(),
                    request.getAmount(),
                    TransactionType.USE,
                    userPoint.updateMillis()
            );

            log.info("point: {}, updateMillis: {}", userPoint.point(), userPoint.updateMillis());
            return userPoint;

        } catch (RuntimeException e) {
            throw e;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 유저의 포인트 충전/사용 내역 조회
     */
    public List<PointHistory> histories(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }
}
