package com.proxymaze.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxymaze.dto.webhook.DiscordPayload;
import com.proxymaze.dto.webhook.SlackPayload;
import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WebhookPayloadTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testSlackPayloadStructure() throws Exception {
        Alert alert = new Alert();
        alert.setAlertId("alert-123");
        alert.setMessage("Test Message");
        alert.setFailureRate(0.6);
        alert.setTotalProxies(10);
        alert.setFailedProxies(6);
        alert.setThreshold(0.5);
        alert.setFiredAt(Instant.parse("2023-01-01T12:00:00Z"));

        Integration integration = new Integration();
        integration.setUsername("CustomUser");

        SlackPayload payload = SlackPayloadBuilder.build("alert.fired", alert, integration);
        String json = mapper.writeValueAsString(payload);

        assertTrue(json.contains("\"username\":\"CustomUser\""));
        assertTrue(json.contains("\"color\":\"#FF4444\""));
        assertTrue(json.contains("\"ts\":1672574400")); // Unix epoch for 2023-01-01T12:00:00Z
        assertTrue(json.contains("\"footer\":\"ProxyMaze Monitoring\""));
    }

    @Test
    public void testDiscordPayloadStructure() throws Exception {
        Alert alert = new Alert();
        alert.setAlertId("alert-123");
        alert.setMessage("Test Message");
        alert.setFailureRate(0.6);
        alert.setTotalProxies(10);
        alert.setFailedProxies(6);
        alert.setThreshold(0.5);
        alert.setFiredAt(Instant.parse("2023-01-01T12:00:00Z"));

        Integration integration = new Integration();
        integration.setUsername("DiscordBot");

        DiscordPayload payload = DiscordPayloadBuilder.build("alert.fired", alert, integration);
        String json = mapper.writeValueAsString(payload);

        assertTrue(json.contains("\"username\":\"DiscordBot\""));
        assertTrue(json.contains("\"color\":16729156")); // 0xFF4444 in decimal
        assertTrue(json.contains("\"timestamp\":\"2023-01-01T12:00:00Z\""));
        assertTrue(json.contains("\"text\":\"ProxyMaze Monitoring\""));
    }
}
