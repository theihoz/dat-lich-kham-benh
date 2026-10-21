package com.datlichkhambenh.backend.pattern.notification;

public record CancellationContent(String subject, String body) implements NotificationContent { }
