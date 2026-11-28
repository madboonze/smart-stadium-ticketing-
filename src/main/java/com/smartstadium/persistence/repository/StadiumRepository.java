package com.smartstadium.persistence.repository;

import com.smartstadium.persistence.entity.StadiumEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StadiumRepository extends JpaRepository<StadiumEntity, String> {
}
