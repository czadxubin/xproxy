package org.icmp4j.platform.unix;

import java.util.List;

import org.icmp4j.IcmpPingResponse;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingUtil;
import org.icmp4j.exception.ParseException;
import org.icmp4j.util.ProcessUtil;
import org.icmp4j.util.StringUtil;
import org.icmp4j.platform.NativeBridge;

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
 * Time: 9:38:18 PM
 */
public class LinuxProcessNativeBridge extends NativeBridge {

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
    
    // preconditions: in this Bridge, source is not supported
    final String source = request.getSource ();
    if (source != null) {
      throw new RuntimeException ("request attribute source is NOT supported by this NativeBridge implementation");
    }

    // handle exceptions
    String command = null;
    List<String> lineList = null;
    boolean parseExceptionFlag = false;
    try {

      // request
      // the minimum timeout for the ping command is 1 second, so override any request timeout to >= 1000 msecs
      final String host = request.getHost ();
      final int timeout = new Long (request.getTimeout ()).intValue ();
      final long timeout2 = timeout < 1000 ? 1000 : timeout;
      final long timeoutAsSeconds = timeout2 / 1000;
      final int packetSize = request.getPacketSize ();
      final String charsetName = request.getCharsetName ();
      
      // execute the ping command
      command = "ping -c 1 -s " + packetSize + " -w " + timeoutAsSeconds + " " + host;
      final long icmpSendEchoStartTime = System.currentTimeMillis ();
      lineList = executeProcessAndGetOutputAsStringList (command, charsetName);
      final long icmpSendEchoDuration = System.currentTimeMillis () - icmpSendEchoStartTime;
      
      // check for timeout
      final boolean timeoutFlag = icmpSendEchoDuration >= timeout2;
      if (timeoutFlag) {
        return IcmpPingUtil.createTimeoutIcmpPingResponse (icmpSendEchoDuration);
      }

      // delegate to a method that can be unit tested
      parseExceptionFlag = true;
      final IcmpPingResponse response = executePingRequest (lineList);
      parseExceptionFlag = false;
      response.setCommand (command);
      response.setOutput (StringUtil.joinByNewLine (lineList));

      // done
      return response;
    }
    catch (final Exception e) {
      
      // propagate
      final String output = StringUtil.joinByNewLine(lineList);
      if (parseExceptionFlag) {
        throw new ParseException("command: " + command + ", output: " + output, e);
      }
      throw new RuntimeException("command: " + command + ", output: " + output, e);
    }
  }

  /**
   * Executes the given request
   * @param lineList
   * @return IcmpEchoResponse
   */
  public IcmpPingResponse executePingRequest (final List<String> lineList) {

    // look for the first line with some output
    // sample output from DEBIAN 6
    //   ping existing host
    //   root@database:~# ping -c 1 -s 32 -w 5 www.google.com
    //   PING www.google.com (74.125.224.211) 32(60) bytes of data.
    //   40 bytes from lax02s02-in-f19.1e100.net (74.125.224.211): icmp_req=1 ttl=56 time=47.2 ms
    //
    //   ping non-existing host
    //   ping -c 1 -s 32 -w 5 www.googgle.com
    //   ping: unknown host www.googgle.com
    //
    // sat 2/13/2021, 4:25 pm pacific: Destination Host Unreachable
    // reported by Darlan Ullmann: https://sourceforge.net/p/icmp4j/discussion/general/thread/eb5bde09/
    // ping -c 1 -s 56 -w 1 192.168.32.9
    // PING 192.168.32.9 (192.168.32.9) 56(84) bytes of data.
    // From 177.84.139.27 icmp_seq=1 Destination Host Unreachable
    //
    // --- 192.168.32.9 ping statistics ---
    // 1 packets transmitted, 0 received, +1 errors, 100% packet loss, time 0ms
    for (final String line : lineList) {

      // discriminate against non-ping lines
      final int icmpReqIndex = line.indexOf ("icmp_req=");
      final int icmpSeqIndex = line.indexOf ("icmp_seq=");
      if (icmpReqIndex < 0 && icmpSeqIndex < 0) {
        continue;
      }

      // handle known failure cases
      if (line.endsWith("Destination Host Unreachable")) {
        final IcmpPingResponse response = new IcmpPingResponse ();
        response.setErrorMessage ("Destination Host Unreachable");
        response.setSuccessFlag (false);
        return response;
      }

      // parse response
      int size = 0;
      {
        final int bytesIndex = line.indexOf (" bytes");
        if (bytesIndex > 0) {
          final String sizeAsString = line.substring (0, bytesIndex);
          size = Integer.parseInt (sizeAsString);
        }
      }
      final String responseAddress = StringUtil.parseString (
        line,
        "from ",
        " ");
      final String ttlAsString = StringUtil.parseString (
        line,
        "ttl=",
        " ");
      final int ttl = Integer.parseInt (ttlAsString);
      final String rttAsString = StringUtil.parseString (
        line,
        "time=",
        "ms");
      final String rttAsString2 = rttAsString.trim ();
      final Float rttAsFloat = Float.parseFloat (rttAsString2);
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
    for (final String string : lineList) {

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

  /**
   * The LinuxProcessNativeBridge interface
   * Executes the given command
   * Returns the output of the process as a List of strings
   * This is abstracted so that it can be mocked
   * @param command
   * @param charsetName
   * @return List<String>
   */
  protected List<String> executeProcessAndGetOutputAsStringList (
    final String command,
    final String charsetName) {

    // delegate
    return ProcessUtil.executeProcessAndGetOutputAsStringList (command, charsetName);
  }
}