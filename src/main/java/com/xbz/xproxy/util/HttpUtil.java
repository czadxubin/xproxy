package com.xbz.xproxy.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class HttpUtil {
    /**
     * 发送 GET 请求
     */
    public static HttpResponseWrapper get(String url, Map<String, String> headers) throws IOException {
        return get(url, headers, (conn) -> {
            // 解析响应
            return resolveHttpResponseWrapper(conn);
        });
    }

    /**
     * 发送 GET 请求
     */
    public static <R> R get(String url, Map<String, String> headers, Function<HttpURLConnection, R> consumer) throws IOException {
        HttpURLConnection conn = null;
        try {
            // 1. 创建连接
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            // 2. 添加请求头
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }
            conn.connect();
            // 3. 处理响应
            return consumer.apply(conn);
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * 发送 POST 请求
     */
    public static HttpResponseWrapper post(String url, Map<String, String> headers, String body) throws IOException {
        HttpURLConnection conn = null;
        try {
            // 1. 创建连接
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setDoOutput(true);  // 允许写入请求体

            // 2. 添加请求头
            if (headers != null) {
                headers.forEach(conn::setRequestProperty);
            }

            // 3. 写入请求体
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            // 解析响应
            return resolveHttpResponseWrapper(conn);

        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static HttpResponseWrapper resolveHttpResponseWrapper(HttpURLConnection conn) {
        String body = null;
        int statusCode = -1;
        Map<String, List<String>> responseHeaders = null;
        try {
            statusCode = conn.getResponseCode();
            // 读取响应头
            responseHeaders = handleHeadersKeyToLetter(conn.getHeaderFields());
            // 处理响应头key转小写
            String charset = "utf-8";
            List<String> contentTypes = responseHeaders.get("content-type");
            if (contentTypes != null && !contentTypes.isEmpty()) {
                String contentTypeValue = contentTypes.get(0);
                String[] split = contentTypeValue.split(";");
                if (split.length > 1) {
                    charset = split[1].replaceAll(" charset=", "");
                }
            }
            body = readResponse(conn, charset);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new HttpResponseWrapper(statusCode, responseHeaders, body);
    }

    private static Map<String, List<String>> handleHeadersKeyToLetter(Map<String, List<String>> responseHeaders) {
        // 转换 key 为小写
        return responseHeaders.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> {
                            String key = entry.getKey();
                            return key != null ? key.toLowerCase() : null;
                        }, // key 转小写
                        Map.Entry::getValue,                    // value 不变
                        (existing, replacement) -> existing     // 如果有重复 key，保留第一个
                ));

    }

    private static String readResponse(HttpURLConnection conn, String charset) throws IOException {
        String contentEncoding = conn.getContentEncoding();
        InputStream in = conn.getResponseCode() < 400 ? conn.getInputStream() : conn.getErrorStream();
        if ("gzip".equals(contentEncoding)) {
            in = new GZIPInputStream(in);
        }
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(in, Charset.forName(charset)))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        }
    }
}
