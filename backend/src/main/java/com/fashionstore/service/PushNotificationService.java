package com.fashionstore.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class PushNotificationService {
    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    public void sendPushNotification(int userId, String title, String message, Map<String, String> metadata) {
        logger.info("Push notification to user {}: title={}, message={}", userId, title, message);
    }
}
