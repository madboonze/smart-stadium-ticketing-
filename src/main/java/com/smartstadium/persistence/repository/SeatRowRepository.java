package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.SeatRowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRowRepository extends JpaRepository<SeatRowEntity, String> {

    List<SeatRowEntity> findBySectionIdOrderByOrdinalAsc(String sectionId);
}
