package com.acme.reservation.adapters.outbound.persistence;

import com.acme.reservation.domain.audit.IntegrationTask;
import com.acme.reservation.application.ports.outbound.IntegrationTaskRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA adapter for integration task persistence.
 */
@Repository
public class IntegrationTaskJpaRepository implements IntegrationTaskRepository {

    private final IntegrationTaskSpringDataRepository springDataRepo;

    public IntegrationTaskJpaRepository(IntegrationTaskSpringDataRepository springDataRepo) {
        this.springDataRepo = springDataRepo;
    }

    @Override
    public IntegrationTask save(IntegrationTask task) {
        IntegrationTaskJpaEntity entity = IntegrationTaskJpaEntity.from(task);
        return springDataRepo.save(entity).toDomain();
    }

    @Override
    public Optional<IntegrationTask> findById(String taskId) {
        return springDataRepo.findById(taskId).map(IntegrationTaskJpaEntity::toDomain);
    }

    @Override
    public List<IntegrationTask> findPendingTasks() {
        return springDataRepo.findPendingTasks().stream()
                .map(IntegrationTaskJpaEntity::toDomain)
                .collect(Collectors.toList());
    }

}
