package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ProxyLoadRequest {

    @JsonProperty("urls")
    private List<String> urls;

    @JsonProperty("replace")
    private Boolean replace;

    public List<String> getUrls() { return urls; }
    public void setUrls(List<String> urls) { this.urls = urls; }

    public Boolean getReplace() { return replace; }
    public void setReplace(Boolean replace) { this.replace = replace; }
}
