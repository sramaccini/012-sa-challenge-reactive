package com.company.challenge.poc2.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import com.company.challenge.poc2.entity.EventRecord;

public interface EventRepository extends ReactiveCrudRepository<EventRecord, Long> {
}