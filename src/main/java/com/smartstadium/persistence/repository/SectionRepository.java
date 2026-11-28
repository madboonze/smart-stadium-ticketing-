package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.SectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<SectionEntity, String> {

    List<SectionEntity> findByStandIdOrderByOrdinalAsc(String standId);
}
