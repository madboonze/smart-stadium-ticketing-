package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.MatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchRepository extends JpaRepository<MatchEntity, String> {

    List<MatchEntity> findByStadiumId(String stadiumId);
}
