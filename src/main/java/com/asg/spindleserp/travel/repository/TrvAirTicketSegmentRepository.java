package com.asg.spindleserp.travel.repository;

import com.asg.spindleserp.travel.entity.TrvAirTicketSegment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrvAirTicketSegmentRepository extends JpaRepository<TrvAirTicketSegment, Long> {

    List<TrvAirTicketSegment> findByAirTicketIdOrderBySegmentOrderAsc(Long airTicketId);

    void deleteByAirTicketId(Long airTicketId);
}
