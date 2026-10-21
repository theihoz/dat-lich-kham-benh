package com.datlichkhambenh.backend.pattern.notification;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Registry chỉ chọn Creator; factory method nằm ở createMessage() của lớp con. */
@Component
public class NotificationCreatorRegistry {
    private final Map<String, NotificationCreator> creators;
    public NotificationCreatorRegistry(List<NotificationCreator> creators) {
        this.creators = creators.stream().collect(Collectors.toUnmodifiableMap(
                NotificationCreator::type, Function.identity()));
    }
    public NotificationCreator get(String type) {
        NotificationCreator creator = creators.get(type);
        if (creator == null) throw new IllegalArgumentException("Loại thông báo không hỗ trợ: " + type);
        return creator;
    }
}
