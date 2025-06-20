package com.xbz.xproxy.util;

import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class ConfigUtilTest {

    @Test
    public void testReadAppConfig(){
        Properties appProps = ConfigUtil.readApplicationConfig();
        System.out.println(appProps);
    }
    @Test
    public void testGetAppConfig(){
        System.out.println(ConfigUtil.getAppConfig());
    }

    @Test
    public void testReadProxyDomains(){
        System.out.println(ConfigUtil.readProxyDomains());
    }

}