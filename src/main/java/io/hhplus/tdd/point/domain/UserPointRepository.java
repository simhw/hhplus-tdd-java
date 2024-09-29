package io.hhplus.tdd.point.domain;

public interface UserPointRepository {
    UserPoint selectById(Long id);

    UserPoint insertOrUpdate(long id, long amount);
}
