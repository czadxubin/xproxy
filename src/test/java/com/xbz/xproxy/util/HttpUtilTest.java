package com.xbz.xproxy.util;


import com.alibaba.fastjson2.JSON;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;

public class HttpUtilTest {

    @Test
    public void testGet() throws IOException {
        int successCount = 0;
        int totalCount = 10;
        for (int i = 0; i < totalCount; i++) {
            HashMap<String, String> reqHeaders = new HashMap<>();
            //                Accept: */*
            //                Accept-Encoding: gzip, deflate, br, zstd
            //                Accept-Language: zh-CN,zh;q=0.9
            //                Cache-Control: no-cache
            reqHeaders.put("Accept", "*/*");
            reqHeaders.put("Accept-Encoding", "gzip, deflate, br, zstd");
            reqHeaders.put("Cache-Control", "no-cache");
            reqHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/132.0.0.0 Safari/537.36");
            String url = "https://site.ip138.com/domain/read.do?domain=github.com&time=" + (new Date().getTime() - 10 * 1000);
            System.out.println("url:" + url);
            HttpResponseWrapper httpResponseWrapper = HttpUtil.get(url, reqHeaders);

            System.out.println("response:" + httpResponseWrapper);
            String body = httpResponseWrapper.getBody();
            System.out.println("body:" + body);
            System.out.println("-------------------------------------------------------");
            if (JSON.parseObject(body).getJSONArray("data") != null) {
                successCount++;
            }
        }
        System.out.println("总调用次数：" + totalCount + ",成功率：" + ((double) successCount) / totalCount);
    }
}