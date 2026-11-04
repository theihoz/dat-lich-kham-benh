package com.datlichkhambenh.backend.messaging.outbox;

public interface OutboxTransport {
    /** Returns only after durable broker acceptance (or local demo delivery), otherwise throws. */
    void publish(String messageId, String kind, String payload);
}
