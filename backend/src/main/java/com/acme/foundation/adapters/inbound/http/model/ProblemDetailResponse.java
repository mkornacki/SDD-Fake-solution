package com.acme.foundation.adapters.inbound.http.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * RFC 9457 Problem Details response model.
 * Content-Type: application/problem+json
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class ProblemDetailResponse {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;
    private final Map<String, Object> extensions;

    public ProblemDetailResponse(
            String type,
            String title,
            int status,
            String detail,
            String instance,
            Map<String, Object> extensions) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
        this.extensions = extensions;
    }

    public String type() {
        return type;
    }

    public String getType() {
        return type;
    }

    public String title() {
        return title;
    }

    public String getTitle() {
        return title;
    }

    public int status() {
        return status;
    }

    public int getStatus() {
        return status;
    }

    public String detail() {
        return detail;
    }

    public String getDetail() {
        return detail;
    }

    public String instance() {
        return instance;
    }

    public String getInstance() {
        return instance;
    }

    public Map<String, Object> extensions() {
        return extensions;
    }

    public Map<String, Object> getExtensions() {
        return extensions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String type;
        private String title;
        private int status;
        private String detail;
        private String instance;
        private Map<String, Object> extensions;

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder instance(String instance) {
            this.instance = instance;
            return this;
        }

        public Builder extensions(Map<String, Object> extensions) {
            this.extensions = extensions;
            return this;
        }

        public ProblemDetailResponse build() {
            return new ProblemDetailResponse(type, title, status, detail, instance, extensions);
        }
    }
}
