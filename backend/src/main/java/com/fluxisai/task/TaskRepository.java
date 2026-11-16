package com.fluxisai.task;
import java.util.*; import java.util.UUID; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface TaskRepository extends JpaRepository<Task,UUID>{Optional<Task> findByIdAndUserId(UUID id,UUID userId);Page<Task> findAllByUserId(UUID userId,Pageable pageable);}
