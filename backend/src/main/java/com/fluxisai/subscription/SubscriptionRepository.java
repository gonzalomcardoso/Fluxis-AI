package com.fluxisai.subscription;
import java.util.*; import java.util.UUID; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface SubscriptionRepository extends JpaRepository<Subscription,UUID>{Optional<Subscription> findByIdAndUserId(UUID id,UUID userId);Page<Subscription> findAllByUserId(UUID userId,Pageable pageable);List<Subscription> findAllByUserIdAndStatus(UUID userId,SubscriptionStatus status);}
