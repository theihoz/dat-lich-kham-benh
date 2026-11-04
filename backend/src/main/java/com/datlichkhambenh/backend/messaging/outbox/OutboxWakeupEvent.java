package com.datlichkhambenh.backend.messaging.outbox;

/** No personal data: only wakes an after-commit dispatcher. */
public record OutboxWakeupEvent() { }
