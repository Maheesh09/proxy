package com.proxymaze.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Strict DTO for Discord webhook payloads.
 */
public class DiscordPayload {
    @JsonProperty("username")
    private String username;

    @JsonProperty("embeds")
    private List<Embed> embeds;

    public static class Embed {
        @JsonProperty("title")
        private String title;

        @JsonProperty("description")
        private String description;

        @JsonProperty("color")
        private int color;

        @JsonProperty("fields")
        private List<Field> fields;

        @JsonProperty("footer")
        private Footer footer;

        @JsonProperty("timestamp")
        private String timestamp;

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public int getColor() { return color; }
        public void setColor(int color) { this.color = color; }
        public List<Field> getFields() { return fields; }
        public void setFields(List<Field> fields) { this.fields = fields; }
        public Footer getFooter() { return footer; }
        public void setFooter(Footer footer) { this.footer = footer; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    public static class Field {
        @JsonProperty("name")
        private String name;

        @JsonProperty("value")
        private String value;

        @JsonProperty("inline")
        private boolean inline;

        public Field() {}
        public Field(String name, String value, boolean inline) {
            this.name = name;
            this.value = value;
            this.inline = inline;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isInline() { return inline; }
        public void setInline(boolean inline) { this.inline = inline; }
    }

    public static class Footer {
        @JsonProperty("text")
        private String text;

        public Footer() {}
        public Footer(String text) { this.text = text; }

        // Getters and Setters
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public List<Embed> getEmbeds() { return embeds; }
    public void setEmbeds(List<Embed> embeds) { this.embeds = embeds; }
}
