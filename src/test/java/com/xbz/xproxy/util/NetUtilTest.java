package com.xbz.xproxy.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class NetUtilTest {

    @Test
    public void testPortConnect() {
        String ip = "140.82.116.3";
        int successTimes = 0;
        for (int i = 0; i < 20; i++) {
            boolean port443 = NetUtil.isPortOpen(ip, 443, 500);
            boolean port80 = NetUtil.isPortOpen(ip, 80, 500);
            if (port80 && port443) {
                successTimes++;
            }
            System.out.println("443:" + port443);
            System.out.println("80:" + port80);
        }
        System.out.println("----------");
        System.out.println("successTimes=" + successTimes);
        System.out.println("----------");
    }

}