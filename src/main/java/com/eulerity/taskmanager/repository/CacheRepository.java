package com.eulerity.taskmanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.eulerity.taskmanager.entity.CacheEntity;

public interface CacheRepository extends JpaRepository<CacheEntity, String> {
}
