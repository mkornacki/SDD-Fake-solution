package com.acme.foundation.adapters.outbound.persistence;

import com.acme.foundation.adapters.outbound.persistence.AuditEventJpaAdapter.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for AuditEvent entities.
 */
interface AuditEventJpaRepository extends JpaRepository<AuditEventEntity, String> {
}
