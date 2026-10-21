package com.datlichkhambenh.backend.pattern.notification;

/** Product chung của Factory Method; các record cụ thể là nội dung bất biến. */
public interface NotificationContent {
    String subject();
    String body();
}
