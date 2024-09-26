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
     *
     * @param id     회원 아이디
     * @param amount 충전 금액
     * @return UserPoint
     */
    public UserPoint charge(long id, long amount) {
        // 유효성 검사
        if (amount <= 0) {
            throw new RuntimeException("amount must be greater than 0");
        }

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();

        try {
            UserPoint userPoint = userPointRepository.selectById(id);

            if (userPoint == null) {
                throw new RuntimeException("not find user point");
            }

            userPoint = userPointRepository.insertOrUpdate(id, userPoint.point() + amount);
            pointHistoryRepository.insert(id, amount, TransactionType.CHARGE, userPoint.updateMillis());

            log.info("amount: {}, balance: {}, update: {}", amount, userPoint.point(), userPoint.updateMillis());
            return userPoint;

        } finally {
            lock.unlock();
        }

    }
    /**
     * 유저의 포인트를 사용하는 기능
     *
     * @param id     회원 아이디
     * @param amount 사용 금액
     * @return UserPoint
     */
    public UserPoint use(long id, long amount) {
        // valid check
        if (amount <= 0) {
            throw new RuntimeException("amount must be greater than 0");
        }

        ReentrantLock lock = locks.computeIfAbsent(id, k -> new ReentrantLock(true));
        lock.lock();

        try {
            UserPoint userPoint = userPointRepository.selectById(id);

            if (userPoint == null) {
                throw new RuntimeException("not find user point");
            }

            // 잔고보다 차감 포인트 금액이 더 큰 경우 취소
            if (userPoint.point() < amount) {
                throw new RuntimeException("not enough point");
            }

            userPoint = userPointRepository.insertOrUpdate(id, userPoint.point() - amount);
            log.info("id: {}, amount: {}, balance: {}, update: {}", id, amount, userPoint.point(), userPoint.updateMillis());

            pointHistoryRepository.insert(id, amount, TransactionType.USE, userPoint.updateMillis());

            return userPoint;
        } catch (RuntimeException e) {
            throw e;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 유저의 포인트 충전/사용 내역 조회
     *
     * @param id 회원 아이디
     * @return List<PointHistory>
     */
    public List<PointHistory> histories(long id) {
        return pointHistoryRepository.selectAllByUserId(id);
    }
}
