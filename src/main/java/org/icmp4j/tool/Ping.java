package org.icmp4j.tool;

import org.icmp4j.IcmpPingUtil;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.Icmp4jUtil;
import org.icmp4j.logger.PrintStreamLogger;
import org.icmp4j.logger.constants.LogLevelName;
import org.icmp4j.platform.NativeBridge;
import org.icmp4j.util.ArgUtil;

/**
 * Internet Control Message Protocol for Java (ICMP4J)
 * http://www.icmp4j.org
 * Copyright 2009 and beyond, icmp4j
 * <p/>
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation as long as:
 * 1. You credit the original author somewhere within your product or website
 * 2. The credit is easily reachable and not burried deep
 * 3. Your end-user can easily see it
 * 4. You register your name (optional) and company/group/org name (required)
 * at http://www.icmp4j.org
 * 5. You do all of the above within 4 weeks of integrating this software
 * 6. You contribute feedback, fixes, and requests for features
 * <p/>
 * If/when you derive a commercial gain from using this software
 * please donate at http://www.icmp4j.org
 * <p/>
 * If prefer or require, contact the author specified above to:
 * 1. Release you from the above requirements
 * 2. Acquire a commercial license
 * 3. Purchase a support contract
 * 4. Request a different license
 * 5. Anything else
 * <p/>
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, similarly
 * to how this is described in the GNU Lesser General Public License.
 * <p/>
 * User: Sal Ingrilli
 * Date: May 23, 2014
 * Time: 6:43:43 PM
 */
public class Ping {

  /**
   * The Java interface
   * 
   * To test:
   * cd c:\dev\icmp4j\trunk\icmp4j\output\tool
   * java -cp * -Djava.library.path=. org.icmp4j.tool.Ping www.google.com
   * 
   * @param args
   */
  public static void main (final String[] args){

    // handle exceptions
    try {

      // extract request parameters
      if (
        ArgUtil.findArgument (args, "-h") ||
        ArgUtil.findArgument (args, "-help") ||
        ArgUtil.findArgument (args, "-?") ||
        ArgUtil.findArgument (args, "/?")) {
        
        System.out.println (
          "Usage: java -cp * -Djava.library.path=. " +
          "org.icmp4j.tool.Ping [-debug] [-nativeBridge <NativeBridge class>] [-charsetName <charset>] [-count <number of pings>] [-timeout <msecs>] [-source <IPv4 of source interface>] <target host name or IPv4 address>");
        System.out.println ("");
        System.out.println ("Examples:");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping -h");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping www.google.com");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping -count 1 -timeout 5000 www.google.com");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping -count 1 -timeout 5000 -source 192.168.1.7 www.google.com");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping -nativeBridge org.icmp4j.platform.windows.WindowsProcessNativeBridge -count 1 -timeout 5000 -source 192.168.1.7 www.google.com");
        System.out.println ("  java -cp * -Djava.library.path=. org.icmp4j.tool.Ping -debug -nativeBridge org.icmp4j.platform.windows.WindowsProcessNativeBridge -charsetName CP866 -count 1 -timeout 5000 -source 192.168.1.7 www.google.com");
        System.out.println ("");
        System.out.println ("Known NativeBridge implementations:");
        System.out.println ("  1. Windows - org.icmp4j.platform.windows.WindowsNativeBridge: uses JNA and IcmpSendEcho");
        System.out.println ("  2. Windows - org.icmp4j.platform.windows.WindowsProcessNativeBridge: spawns ping.exe");
        System.out.println ("  3. Linux - org.icmp4j.platform.unix.LinuxProcessNativeBridge: spawns the ping executable");
        System.out.println ("  4. Mac - org.icmp4j.platform.unix.MacProcessNativeBridge: spawns the ping executable");
        System.out.println ("  5. Unix - org.icmp4j.platform.unix.jna.UnixJnaNativeBridge: uses JNA and send ()");
        System.out.println ("  6. Unix - org.icmp4j.platform.unix.jni.UnixJniNativeBridge: uses JNI and send ()");
        return;
      }

      // -t: windows ping.exe repeats until stopped, otherwise default to 4
      final boolean debugFlag = ArgUtil.findArgValue (args, "-debug") != null;
      final int count = ArgUtil.findArgValueAsInt (args, "-count", 4);
      final int timeout = ArgUtil.findArgValueAsInt (args, "-timeout", 0);
      final String charsetName = ArgUtil.findArgValue (args, "-charsetName");
      final String source = ArgUtil.findArgValue (args, "-source");
      final String host = args.length > 0 ?
        args [args.length - 1] :
        "google.com";

      // if you wanted a specific instance, initialize it and hand it over to Icmp4jUtil
      NativeBridge nativeBridge = null;
      {
        final String nativeBridgeClassName = ArgUtil.findArgValue (args, "-nativeBridge");
        if (nativeBridgeClassName != null) {
          final Class nativeBridgeClass = Class.forName (nativeBridgeClassName);
          nativeBridge = (NativeBridge) nativeBridgeClass.newInstance ();
          nativeBridge.initialize ();
        }
      }
      
      // initialize
      final String logLevel = debugFlag ?
        LogLevelName.DEBUG :
        LogLevelName.INFO;
      PrintStreamLogger.setLogLevel (logLevel);
      Icmp4jUtil.setNativeBridge (nativeBridge);
      Icmp4jUtil.initialize ();
      if (debugFlag) {
        final NativeBridge runtimeNativeBridge = Icmp4jUtil.getNativeBridge();
        System.out.println ("runtimeNativeBridge: " + runtimeNativeBridge.getClass().getName());
      }

      // request
      final IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest ();
      request.setCharsetName (charsetName);
      request.setHost (host);
      request.setSource (source);
      request.setTimeout (timeout);

      // repeat 4 times by default
      for (int number = 1; number <= count; number ++) {

        // delegate
        final IcmpPingResponse response = IcmpPingUtil.executePingRequest (request);

        // log
        if (debugFlag) {
          final String command = response.getCommand ();
          if (command != null) {
            System.out.println ("command: " + command);
          }
          final String output = response.getOutput ();
          if (output != null) {
            System.out.println ("<output>");
            System.out.println (output);
            System.out.println ("</output>");
          }
        }
        final String formattedResponse = IcmpPingUtil.formatResponse (response);
        System.out.println (formattedResponse);

        // rest
        Thread.sleep (1000);
      }
    }
    catch (final Throwable t){

      // log
      t.printStackTrace ();
    }
  }
}