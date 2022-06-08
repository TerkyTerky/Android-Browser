package com.terky.g_browser.entity;

import java.io.Serializable;

public class Bookmark implements Serializable {

    private String webName;
    private String uri;

    public String getWebName() {
        return webName;
    }

    public void setWebName(String webName) {
        this.webName = webName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}

