package com.hangplan.repository;

import com.hangplan.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Integer> {

    Optional<SubscriptionPlan> findByName(String name);
}
