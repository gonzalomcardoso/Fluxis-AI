package com.fluxisai.audit;
import java.util.UUID; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditEventRepository extends JpaRepository<AuditEvent,UUID>{Page<AuditEvent> findAllByUserId(UUID userId,Pageable pageable);}
