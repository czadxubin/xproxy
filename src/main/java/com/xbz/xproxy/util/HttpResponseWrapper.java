package com.xbz.xproxy.util;

import java.util.List;
import java.util.Map;

/**
 * 响应封装类
 */
public class HttpResponseWrapper {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public HttpResponseWrapper(int statusCode, Map<String, List<String>> responseHeaders, String body) {
        this.statusCode = statusCode;
        this.headers = responseHeaders;
        this.body = body;
    }

    // Getters
    public int getStatusCode() {
        return statusCode;
    }

    public String getBody() {
        return body;
    }

    @Override
    public String toString() {
        return "HttpResponse{" +
                "statusCode=" + statusCode +
                ", headers =" + headers +
                ", body='" + body + '\'' +
                '}';
    }
}