package com.datlichkhambenh.backend.pattern.notification;

public record ConfirmationContent(String subject, String body) implements NotificationContent { }
