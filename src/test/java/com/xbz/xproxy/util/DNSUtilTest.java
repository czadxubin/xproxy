package com.xbz.xproxy.util;

import org.junit.Test;

import java.util.ArrayList;

public class DNSUtilTest {

    @Test
    public void testDomain2IP(){
        String domain = "github.com";
        String dns = "1.1.1.1";
        System.out.println(DNSUtil.domain2IP(dns,domain));
    }
    @Test
    public void testDomain2IP2(){
        String domain = "github.com";
        String dns = "1.1.1.1";
        String dns2 = "8.8.8.8";
        ArrayList<String> dnsList = new ArrayList<>();
        dnsList.add(dns2);
        dnsList.add(dns);
    }
}