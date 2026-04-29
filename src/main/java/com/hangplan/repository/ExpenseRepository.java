package com.hangplan.repository;

import com.hangplan.entity.Event;
import com.hangplan.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    List<Expense> findByEventOrderById(Event event);

    @Query("select e from Expense e join fetch e.paidBy pb join fetch pb.user where e.event.id = :eid order by e.id")
    List<Expense> findByEventIdWithPayer(@Param("eid") UUID eid);
}
