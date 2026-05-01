package com.hangplan.repository;

import com.hangplan.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventRepository extends JpaRepository<Event, UUID> {

    /** Events where the user has any participant row (host or guest; accepted or declined). */
    @Query("select distinct e from Event e inner join Participant p on p.event.id = e.id "
            + "where p.user.id = :uid order by e.createdAt desc")
    List<Event> findDistinctEventsWhereUserParticipates(@Param("uid") UUID uid);
}
