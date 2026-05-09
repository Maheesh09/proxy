package com.proxymaze.webhook;

import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;

import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Builds Discord-formatted webhook payloads (embeds style).
 */
public class DiscordPayloadBuilder {

    private DiscordPayloadBuilder() {}

    public static Map<String, Object> build(String event, Alert alert, Integration integration) {
        boolean isFired = "alert.fired".equals(event);
        int color = isFired ? 0xFF4444 : 0x44BB44; // Discord uses decimal int
        String title = isFired ? "🚨 Proxy Alert Fired" : "✅ Proxy Alert Resolved";

        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(discordField("Failure Rate", String.format("%.1f%%", alert.getFailureRate() * 100), true));
        fields.add(discordField("Failed Proxies", alert.getFailedProxies() + " / " + alert.getTotalProxies(), true));
        fields.add(discordField("Threshold", String.format("%.0f%%", alert.getThreshold() * 100), true));
        fields.add(discordField("Alert ID", alert.getAlertId(), false));
        if (alert.getFailedProxyIds() != null && !alert.getFailedProxyIds().isEmpty()) {
            fields.add(discordField("Failed IDs", String.join(", ", alert.getFailedProxyIds()), false));
        }

        Map<String, Object> footer = new LinkedHashMap<>();
        footer.put("text", "ProxyMaze Monitoring");

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", title);
        embed.put("description", alert.getMessage());
        embed.put("color", color);
        embed.put("fields", fields);
        embed.put("footer", footer);

        if (alert.getFiredAt() != null) {
            embed.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(alert.getFiredAt()));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        if (integration.getUsername() != null) {
            payload.put("username", integration.getUsername());
        }
        payload.put("embeds", List.of(embed));

        return payload;
    }

    private static Map<String, Object> discordField(String name, String value, boolean inline) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("value", value);
        f.put("inline", inline);
        return f;
    }
}
