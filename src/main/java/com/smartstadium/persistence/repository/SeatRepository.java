package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.SeatEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<SeatEntity, String> {

    List<SeatEntity> findByRowIdOrderByOrdinalAsc(String rowId);

    List<SeatEntity> findByStandIdOrderByOrdinalAsc(String standId);
}
