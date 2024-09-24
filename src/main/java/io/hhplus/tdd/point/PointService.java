package io.hhplus.tdd.point;

import java.util.List;

public interface PointService {
    public UserPoint getUserPoint(long id);

    public UserPoint chargeUserPoint(long id, long amount);

    public UserPoint useUserPoint(long id, long amount);

    public List<PointHistory> getPointHistories(long id);
}
