package org.icmp4j.platform.windows;

import java.util.List;
import java.io.File;

import org.icmp4j.platform.NativeBridge;
import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingUtil;
import org.icmp4j.util.ProcessUtil;
import org.icmp4j.util.StringUtil;
import org.icmp4j.util.WindowsProcessUtil;

/**
 * ShortPasta Foundation
 * http://www.shortpasta.org
 * Copyright 2009 and beyond, Sal Ingrilli at the ShortPasta Software Foundation
 * <p/>
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3
 * as published by the Free Software Foundation as long as:
 * 1. You credit the original author somewhere within your product or website
 * 2. The credit is easily reachable and not burried deep
 * 3. Your end-user can easily see it
 * 4. You register your name (optional) and company/group/org name (required)
 * at http://www.shortpasta.org
 * 5. You do all of the above within 4 weeks of integrating this software
 * 6. You contribute feedback, fixes, and requests for features
 * <p/>
 * If/when you derive a commercial gain from using this software
 * please donate at http://www.shortpasta.org
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
 * Date: Sep 14, 2016
 * Time: 4:05:05 PM
 */
public class WindowsProcessNativeBridge extends NativeBridge {

  /**
   * The NativeBridge interface
   * 
   * Executes the given icmp ECHO request
   * This call blocks until a response is received or a timeout is reached
   * 
   * The jna implementation adapted from:
   *   http://hp.vector.co.jp/authors/VA033015/jnasamples.html
   * 
   * @param request
   * @return IcmpEchoResponse
   */
  @Override
  public IcmpPingResponse executePingRequest (final IcmpPingRequest request) {

    // handle exceptions
    try {

      // request
      final String source = request.getSource ();
      final String host = request.getHost ();
      final long timeout = request.getTimeout ();
      final int packetSize = request.getPacketSize ();
      final String charsetName = request.getCharsetName ();

      // execute the ping command:
      //   ping -n 1 -l 32 -w 1000 www.google.com
      //   ping -n 1 -l 32 -w 1000 -S 192.168.1.7 www.google.com
      // note that the timeout is in milliseconds
      final File pingExeFile = WindowsProcessUtil.getWindowsSystemFile ("ping.exe");
      final String command =
        "\"" + pingExeFile.getAbsolutePath () + "\" " +
        "-n 1 " +
        (packetSize > 0 ? "-l " + packetSize + " " : "") +
        (timeout > 0 ? "-w " + timeout + " " : "") +
        (source != null ? "-S " + source + " " : "") +
        host;
      final long icmpSendEchoStartTime = System.currentTimeMillis ();
      final List<String> stringList = ProcessUtil.executeProcessAndGetOutputAsStringList (command, charsetName);
      final long icmpSendEchoDuration = System.currentTimeMillis () - icmpSendEchoStartTime;

      // check for timeout
      final boolean timeoutFlag = icmpSendEchoDuration >= timeout;
      if (timeoutFlag) {
        return IcmpPingUtil.createTimeoutIcmpPingResponse (icmpSendEchoDuration);
      }

      // delegate to a method that can be unit tested
      final IcmpPingResponse response = executePingRequest (stringList);
      response.setCommand (command);
      response.setOutput (StringUtil.joinByNewLine (stringList));
      
      // done
      return response;
    }
    catch (final Exception e) {

      // propagate
      throw new RuntimeException (e);
    }
  }

  /**
   * Executes the given request
   * @param stringList
   * @return IcmpEchoResponse
   */
  public IcmpPingResponse executePingRequest (final List<String> stringList) {

    // look for the first line with some output
    // sample output from win7 to a valid host
    //   c:\dev\icmp4j\trunk>ping -n 1 -l 32 -w 5 www.google.com
    //   
    //   Pinging www.google.com [216.58.217.196] with 32 bytes of data:
    //   Reply from 216.58.217.196: bytes=32 time=12ms TTL=56
    //   
    //   Ping statistics for 216.58.217.196:
    //       Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),
    //   Approximate round trip times in milli-seconds:
    //       Minimum = 12ms, Maximum = 12ms, Average = 12ms
    //
    // sample output from win7 to localohost where time< instead of time=
    //   c:\dev\icmp4j\trunk\icmp4j\test>ping -n 1 -l 32 -w 5 127.0.0.1
    //   
    //   Pinging 127.0.0.1 with 32 bytes of data:
    //   Reply from 127.0.0.1: bytes=32 time<1ms TTL=128
    //   
    //   Ping statistics for 127.0.0.1:
    //       Packets: Sent = 1, Received = 1, Lost = 0 (0% loss),
    //   Approximate round trip times in milli-seconds:
    //       Minimum = 0ms, Maximum = 0ms, Average = 0ms
    // 
    // sample output from win7 to non-existing host
    //   c:\dev\icmp4j\trunk>ping -n 1 -l 32 -w 5 www.googgle.com
    //   Ping request could not find host www.googgle.com. Please check the name and try again.
    for (final String string : stringList) {

      // this is what we are looking for:
      // Reply from 216.58.217.196: bytes=32 time=12ms TTL=56

      // discriminate against non-ping lines
      if (!string.startsWith ("Reply from ")) {
        continue;
      }

      // parse response
      final String sizeAsString = StringUtil.parseSequentialDigits (
        string,
        "bytes=");
      final int size = Integer.parseInt (sizeAsString);
      final String responseAddress = StringUtil.parseString (
        string,
        "from ",
        ":");
      final String ttlAsString = StringUtil.parseSequentialDigits (
        string,
        "TTL=");
      final int ttl = Integer.parseInt (ttlAsString);
      final String rttAsStringEquals = StringUtil.parseSequentialDigits (
        string,
        "time=");
      final String rttAsStringLessThan = StringUtil.parseSequentialDigits (
        string,
        "time<");
      final String rttAsString = rttAsStringEquals != null ?
        rttAsStringEquals :
        rttAsStringLessThan;
      final Float rttAsFloat = Float.parseFloat (rttAsString);
      final int rtt = rttAsFloat.intValue ();

      // objectify
      final IcmpPingResponse response = new IcmpPingResponse ();
      response.setHost (responseAddress);
      response.setErrorMessage (null);
      response.setRtt (rtt);
      response.setSize (size);
      response.setSuccessFlag (true);
      response.setTtl (ttl);

      // done
      return response;
    }

    // not found - if there is at least one line, use that as the error message
    // noinspection LoopStatementThatDoesntLoop
    for (final String string : stringList) {

      // objectify
      final IcmpPingResponse response = new IcmpPingResponse ();
      response.setErrorMessage (string);
      response.setSuccessFlag (false);

      // done
      return response;
    }

    // no results found
    {
      // objectify
      final IcmpPingResponse response = new IcmpPingResponse ();
      response.setErrorMessage ("No results could be parsed");
      response.setSuccessFlag (false);

      // done
      return response;
    }
  }
}