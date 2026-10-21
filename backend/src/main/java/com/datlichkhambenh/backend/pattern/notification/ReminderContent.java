package com.datlichkhambenh.backend.pattern.notification;

public record ReminderContent(String subject, String body) implements NotificationContent { }
