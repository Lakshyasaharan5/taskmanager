package com.eulerity.taskmanager.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cache_entry")
public class CacheEntity {

	@Id
	@Column(name = "cache_key")
	private String key;

	@Column(length = 2000, nullable = false)
	private String response;

	@Column(nullable = false)
	private Instant expiry;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public Instant getExpiry() {
		return expiry;
	}

	public void setExpiry(Instant expiry) {
		this.expiry = expiry;
	}

}
