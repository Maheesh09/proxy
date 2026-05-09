package com.proxymaze.dto.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Strict DTO for Slack webhook payloads.
 */
public class SlackPayload {
    @JsonProperty("username")
    private String username;

    @JsonProperty("text")
    private String text;

    @JsonProperty("attachments")
    private List<Attachment> attachments;

    public static class Attachment {
        @JsonProperty("color")
        private String color;

        @JsonProperty("title")
        private String title;

        @JsonProperty("text")
        private String text;

        @JsonProperty("fields")
        private List<Field> fields;

        @JsonProperty("footer")
        private String footer;

        @JsonProperty("ts")
        private long ts;

        @JsonProperty("footer_icon")
        private String footerIcon;

        // Getters and Setters
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<Field> getFields() { return fields; }
        public void setFields(List<Field> fields) { this.fields = fields; }
        public String getFooter() { return footer; }
        public void setFooter(String footer) { this.footer = footer; }
        public long getTs() { return ts; }
        public void setTs(long ts) { this.ts = ts; }
        public String getFooterIcon() { return footerIcon; }
        public void setFooterIcon(String footerIcon) { this.footerIcon = footerIcon; }
    }

    public static class Field {
        @JsonProperty("title")
        private String title;

        @JsonProperty("value")
        private String value;

        @JsonProperty("short")
        private boolean shortField;

        public Field() {}
        public Field(String title, String value, boolean shortField) {
            this.title = title;
            this.value = value;
            this.shortField = shortField;
        }

        // Getters and Setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public boolean isShortField() { return shortField; }
        public void setShortField(boolean shortField) { this.shortField = shortField; }
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }
}
