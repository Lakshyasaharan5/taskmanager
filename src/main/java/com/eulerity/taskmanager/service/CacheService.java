package com.eulerity.taskmanager.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.eulerity.taskmanager.entity.CacheEntity;
import com.eulerity.taskmanager.repository.CacheRepository;

@Service
public class CacheService {

	private final CacheRepository cacheRepository;

	public CacheService(CacheRepository cacheRepository) {
		this.cacheRepository = cacheRepository;
	}

	public Optional<String> get(String key) {
		Optional<CacheEntity> entry = cacheRepository.findById(key);
		if (entry.isEmpty()) {
			return Optional.empty();
		}
		if (entry.get().getExpiry().isBefore(Instant.now())) {
			cacheRepository.deleteById(key);
			return Optional.empty();
		}
		return Optional.of(entry.get().getResponse());
	}

	public void put(String key, String response, Duration ttl) {
		CacheEntity entry = new CacheEntity();
		entry.setKey(key);
		entry.setResponse(response);
		entry.setExpiry(Instant.now().plus(ttl));
		cacheRepository.save(entry);
	}

}
