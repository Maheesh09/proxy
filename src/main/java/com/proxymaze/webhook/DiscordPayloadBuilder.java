package com.proxymaze.webhook;

import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Builds Discord embed payloads.
 * Strictly adheres to Bonus B2 name requirements.
 */
public class DiscordPayloadBuilder {

    private DiscordPayloadBuilder() {}

    public static Map<String, Object> build(String event, Alert alert, Integration integration) {
        boolean isFired = "alert.fired".equals(event);

        // Precise Discord Colors: Red = 16729156, Green = 4504388
        int color = isFired ? 16729156 : 4504388;

        String summary = isFired ? "🚨 ALERT FIRED: Proxy pool failure rate exceeded threshold"
                                 : "✅ ALERT RESOLVED: Proxy pool has recovered";

        // Deterministic rounding
        double roundedRate = Math.round(alert.getFailureRate() * 100.0) / 100.0;

        // Snapshot IDs (sorted)
        List<String> ids = new ArrayList<>(
                alert.getFailedProxyIds() != null ? alert.getFailedProxyIds() : List.of());
        Collections.sort(ids);
        String failedIdsStr = ids.isEmpty() ? "None" : String.join(", ", ids);

        Instant firedAt = alert.getFiredAt() != null ? alert.getFiredAt() : Instant.now();
        String firedAtStr = firedAt.truncatedTo(ChronoUnit.SECONDS).toString();

        // Required field names: Alert ID, Failure Rate, Failed Proxies, Threshold, Failed IDs
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(discordField("Alert ID",       alert.getAlertId(), false));
        fields.add(discordField("Failure Rate",   String.valueOf(roundedRate), false));
        fields.add(discordField("Failed Proxies", String.valueOf(alert.getFailedProxies()), false));
        fields.add(discordField("Threshold",      String.valueOf(alert.getThreshold()), false));
        fields.add(discordField("Failed IDs",     failedIdsStr, false));

        Map<String, Object> footer = new LinkedHashMap<>();
        footer.put("text", "ProxyMaze Monitoring");

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title",       summary);
        embed.put("description", alert.getMessage());
        embed.put("color",       color);
        embed.put("fields",      fields);
        embed.put("footer",      footer);
        embed.put("timestamp",   firedAtStr);

        Map<String, Object> payload = new LinkedHashMap<>();
        if (integration.getUsername() != null && !integration.getUsername().isBlank()) {
            payload.put("username", integration.getUsername());
        }
        payload.put("embeds", List.of(embed));

        return payload;
    }

    private static Map<String, Object> discordField(String name, String value, boolean inline) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name",   name);
        f.put("value",  value);
        f.put("inline", inline);
        return f;
    }
}
