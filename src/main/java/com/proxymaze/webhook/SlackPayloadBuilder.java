package com.proxymaze.webhook;

import com.proxymaze.dto.webhook.SlackPayload;
import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds Slack-formatted webhook payloads using strict DTOs.
 */
public class SlackPayloadBuilder {

    private SlackPayloadBuilder() {}

    public static SlackPayload build(String event, Alert alert, Integration integration) {
        boolean isFired = "alert.fired".equals(event);
        String color = isFired ? "#FF4444" : "#44BB44";
        String title = isFired ? "🚨 Proxy Alert Fired" : "✅ Proxy Alert Resolved";
        String footerText = "ProxyMaze Monitoring";

        SlackPayload.Attachment attachment = new SlackPayload.Attachment();
        attachment.setColor(color);
        attachment.setTitle(title);
        attachment.setText(alert.getMessage());

        List<SlackPayload.Field> fields = new ArrayList<>();
        fields.add(new SlackPayload.Field("Failure Rate", String.format("%.1f%%", alert.getFailureRate() * 100), true));
        fields.add(new SlackPayload.Field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies(), true));
        fields.add(new SlackPayload.Field("Threshold", String.format("%.0f%%", alert.getThreshold() * 100), true));
        fields.add(new SlackPayload.Field("Alert ID", alert.getAlertId(), false));
        
        if (alert.getFailedProxyIds() != null && !alert.getFailedProxyIds().isEmpty()) {
            fields.add(new SlackPayload.Field("Failed IDs", String.join(", ", alert.getFailedProxyIds()), false));
        }
        
        if (alert.getFiredAt() != null) {
            String firedAtStr = DateTimeFormatter.ISO_INSTANT.format(alert.getFiredAt());
            fields.add(new SlackPayload.Field("Fired At", firedAtStr, false));
        }
        
        attachment.setFields(fields);
        attachment.setFooter(footerText);
        attachment.setTs(alert.getFiredAt() != null ? alert.getFiredAt().getEpochSecond() : 0);
        attachment.setFooterIcon("https://example.com/proxymaze-icon.png");

        SlackPayload payload = new SlackPayload();
        payload.setUsername(integration.getUsername() != null ? integration.getUsername() : "ProxyMaze");
        payload.setText(title);
        payload.setAttachments(List.of(attachment));

        return payload;
    }
}
