package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointServiceImplV1 implements PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    @Override
    public UserPoint getUserPoint(long id) {
        return userPointTable.selectById(id);
    }

    /**
     * 유저의 포인트를 충전하는 기능
     *
     * @param id     회원 아이디
     * @param amount 충전 금액
     * @return UserPoint
     */
    @Override
    public UserPoint chargeUserPoint(long id, long amount) {
        if (amount <= 0) {
            throw new RuntimeException("amount must be greater than 0");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new RuntimeException("not find user point");
        }

        userPoint = userPointTable.insertOrUpdate(id, userPoint.point() + amount);
        log.info("amount: {}, balance: {}, update: {}", amount, userPoint.point(), userPoint.updateMillis());

        pointHistoryTable.insert(id, amount, TransactionType.CHARGE, userPoint.updateMillis());
        return userPoint;
    }

    /**
     * 유저의 포인트를 사용하는 기능
     *
     * @param id     회원 아이디
     * @param amount 사용 금액
     * @return UserPoint
     */
    @Override
    public UserPoint useUserPoint(long id, long amount) {
        if (amount <= 0) {
            throw new RuntimeException("amount must be greater than 0");
        }

        UserPoint userPoint = userPointTable.selectById(id);

        if (userPoint == null) {
            throw new RuntimeException("not find user point");
        }

        if (userPoint.point() < amount) {
            throw new RuntimeException("not enough point");
        }

        userPoint = userPointTable.insertOrUpdate(id, userPoint.point() - amount);
        log.info("amount: {}, balance: {}, update: {}", amount, userPoint.point(), userPoint.updateMillis());

        pointHistoryTable.insert(id, amount, TransactionType.USE, userPoint.updateMillis());

        return userPoint;
    }

    /**
     * 유저의 포인트 충전/사용 내역 조회
     *
     * @param id 회원 아이디
     * @return List<PointHistory>
     */

    @Override
    public List<PointHistory> getPointHistories(long id) {
        return pointHistoryTable.selectAllByUserId(id);
    }
}
