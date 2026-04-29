package com.hangplan.repository;

import com.hangplan.entity.Event;
import com.hangplan.entity.Participant;
import com.hangplan.entity.ParticipantStatus;
import com.hangplan.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantRepository extends JpaRepository<Participant, UUID> {

    Optional<Participant> findByEventIdAndUserId(UUID eventId, UUID userId);

    int countByEventAndStatus(Event event, ParticipantStatus status);

    @Query("select p from Participant p join fetch p.user where p.event.id = :eid order by p.user.name")
    List<Participant> findByEventIdWithUser(@Param("eid") UUID eid);
}
