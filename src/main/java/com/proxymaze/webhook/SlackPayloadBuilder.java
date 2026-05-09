package com.proxymaze.webhook;

import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Builds Slack-formatted webhook payloads.
 */
public class SlackPayloadBuilder {

    private SlackPayloadBuilder() {}

    public static Map<String, Object> build(String event, Alert alert, Integration integration) {
        boolean isFired = "alert.fired".equals(event);
        String color = isFired ? "#FF4444" : "#44BB44";
        String title = isFired ? "🚨 Proxy Alert Fired" : "✅ Proxy Alert Resolved";
        String footerText = "ProxyMaze Monitoring";

        Map<String, Object> attachment = new LinkedHashMap<>();
        attachment.put("color", color);
        attachment.put("title", title);
        attachment.put("text", alert.getMessage());

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(field("Failure Rate", String.format("%.1f%%", alert.getFailureRate() * 100), true));
        fields.add(field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies(), true));
        fields.add(field("Threshold", String.format("%.0f%%", alert.getThreshold() * 100), true));
        fields.add(field("Alert ID", alert.getAlertId(), false));
        if (alert.getFailedProxyIds() != null && !alert.getFailedProxyIds().isEmpty()) {
            fields.add(field("Failed IDs", String.join(", ", alert.getFailedProxyIds()), false));
        }
        if (alert.getFiredAt() != null) {
            String firedAtStr = DateTimeFormatter.ISO_INSTANT.format(alert.getFiredAt());
            fields.add(field("Fired At", firedAtStr, false));
        }
        attachment.put("fields", fields);

        String ts = alert.getFiredAt() != null
                ? DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC).format(alert.getFiredAt())
                : "";
        attachment.put("footer", footerText);
        attachment.put("ts", alert.getFiredAt() != null ? alert.getFiredAt().getEpochSecond() : 0);
        attachment.put("footer_icon", "https://example.com/proxymaze-icon.png");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", integration.getUsername() != null ? integration.getUsername() : "ProxyMaze");
        payload.put("text", title);
        payload.put("attachments", List.of(attachment));

        return payload;
    }

    private static Map<String, Object> field(String title, String value, boolean shortField) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("title", title);
        f.put("value", value);
        f.put("short", shortField);
        return f;
    }
}
