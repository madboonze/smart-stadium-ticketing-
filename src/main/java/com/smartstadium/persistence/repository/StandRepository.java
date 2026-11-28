package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.StandEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StandRepository extends JpaRepository<StandEntity, String> {

    List<StandEntity> findByStadiumIdOrderByOrdinalAsc(String stadiumId);
}
