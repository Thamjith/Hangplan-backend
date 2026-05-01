package com.hangplan.service;

import com.hangplan.entity.SubscriptionPlan;
import com.hangplan.entity.User;
import com.hangplan.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;

    public SubscriptionPlan requirePlanByName(String name) {
        return subscriptionPlanRepository.findByName(name)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Subscription plan not configured: " + name));
    }

    /** New signups: FREE plan, no active window. */
    public void assignFreePlan(User user) {
        SubscriptionPlan free = requirePlanByName("FREE");
        user.setSubscriptionPlan(free);
        user.setSubscriptionStart(null);
        user.setSubscriptionEnd(null);
    }

    /** Paid upgrades: starts now for plan {@code durationDays}. */
    public void activatePaidPlan(User user, String planName) {
        SubscriptionPlan plan = requirePlanByName(planName);
        if (plan.getDurationDays() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plan has no duration: " + planName);
        }
        LocalDateTime now = LocalDateTime.now();
        user.setSubscriptionPlan(plan);
        user.setSubscriptionStart(now);
        user.setSubscriptionEnd(now.plusDays(plan.getDurationDays()));
    }
}
