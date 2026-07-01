package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvBookingNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvBookingNoteRepository extends JpaRepository<TrvBookingNote, Long> {

    List<TrvBookingNote> findByBookingIdOrderByCreatedAtDesc(Long bookingId);
}
