//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.icmp4j;

import org.icmp4j.platform.NativeBridge;
import org.icmp4j.util.PlatformUtil;

public class Icmp4jUtil {
    private static NativeBridge nativeBridge;

    public Icmp4jUtil() {
    }

    public static void setNativeBridge(NativeBridge nativeBridge) {
        Icmp4jUtil.nativeBridge = nativeBridge;
    }

    public static NativeBridge getNativeBridge() {
        return nativeBridge;
    }

    public static void initialize() {
        if (Icmp4jUtil.nativeBridge == null) {
            Class var0 = Icmp4jUtil.class;
            synchronized(Icmp4jUtil.class) {
                if (Icmp4jUtil.nativeBridge == null) {
                    try {
                        PlatformUtil.initialize();
                        if (Icmp4jUtil.nativeBridge == null) {
                            int osFamilyCode = PlatformUtil.getOsFamilyCode();
                            String nativeBridgeClassName = osFamilyCode == 4 ? "org.icmp4j.platform.android.AndroidNativeBridge" : (osFamilyCode == 2 ? "org.icmp4j.platform.unix.UnixNativeBridge" : (osFamilyCode == 3 ? "org.icmp4j.platform.unix.UnixNativeBridge" : (osFamilyCode == 1 ? "org.icmp4j.platform.windows.WindowsNativeBridge" : "org.icmp4j.platform.java.JavaNativeBridge")));
                            Class<NativeBridge> nativeBridgeClass = (Class<NativeBridge>) Class.forName(nativeBridgeClassName);
                            NativeBridge nativeBridge = (NativeBridge)nativeBridgeClass.newInstance();
                            nativeBridge.initialize();
                            Icmp4jUtil.nativeBridge = nativeBridge;
                        }
                    } catch (Exception var6) {
                        Exception e = var6;
                        throw new RuntimeException(e);
                    }

                }
            }
        }
    }

    public static void destroy() {
        if (nativeBridge != null) {
            nativeBridge.destroy();
            nativeBridge = null;
        }

    }
}
