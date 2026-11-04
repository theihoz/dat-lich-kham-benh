package com.datlichkhambenh.backend.messaging;

public record AccountNotificationMessage(String type, String recipientEmail, String link) { }
