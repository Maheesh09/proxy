package com.proxymaze.webhook;

import com.proxymaze.dto.webhook.DiscordPayload;
import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds Discord-formatted webhook payloads using strict DTOs.
 */
public class DiscordPayloadBuilder {

    private DiscordPayloadBuilder() {}

    public static DiscordPayload build(String event, Alert alert, Integration integration) {
        boolean isFired = "alert.fired".equals(event);
        int color = isFired ? 0xFF4444 : 0x44BB44;
        String title = isFired ? "🚨 Proxy Alert Fired" : "✅ Proxy Alert Resolved";

        List<DiscordPayload.Field> fields = new ArrayList<>();
        fields.add(new DiscordPayload.Field("Failure Rate", String.format("%.1f%%", alert.getFailureRate() * 100), true));
        fields.add(new DiscordPayload.Field("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies(), true));
        fields.add(new DiscordPayload.Field("Threshold", String.format("%.0f%%", alert.getThreshold() * 100), true));
        fields.add(new DiscordPayload.Field("Alert ID", alert.getAlertId(), false));
        
        if (alert.getFailedProxyIds() != null && !alert.getFailedProxyIds().isEmpty()) {
            fields.add(new DiscordPayload.Field("Failed IDs", String.join(", ", alert.getFailedProxyIds()), false));
        }

        DiscordPayload.Embed embed = new DiscordPayload.Embed();
        embed.setTitle(title);
        embed.setDescription(alert.getMessage());
        embed.setColor(color);
        embed.setFields(fields);
        embed.setFooter(new DiscordPayload.Footer("ProxyMaze Monitoring"));

        if (alert.getFiredAt() != null) {
            embed.setTimestamp(DateTimeFormatter.ISO_INSTANT.format(alert.getFiredAt()));
        }

        DiscordPayload payload = new DiscordPayload();
        if (integration.getUsername() != null) {
            payload.setUsername(integration.getUsername());
        } else {
            payload.setUsername("ProxyMaze");
        }
        payload.setEmbeds(List.of(embed));

        return payload;
    }
}
