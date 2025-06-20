package org.icmp4j;

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
 * Time: 6:55:22 PM
 */
public class IcmpPingRequest {
  
  // my attributes
  // source: the ip of the source interface to use for sending the packet.
  // as of tue 2017-03-28, only WindowsProcessNativeBridge supports this
  // host: the host name or ipv4 address of the destination to ping
  // ttl: the TTL as defined by ICMP
  // packetsize: the packet size as defined by ICMP
  // timeout: max time to wait, milliseconds, for the response to return
  // charsetName: the charsetName to use for parsing output when native ping processes are spawned
  private String source;
  private String host;
  private int ttl;
  private int packetSize;
  private long timeout;
  private String charsetName;
  
  // my attributes
  public void setSource (final String source) { this.source = source; }
  public String getSource () { return source; }

  public void setHost (final String host) { this.host = host; }
  public String getHost () { return host; }

  public void setTtl (final int ttl) { this.ttl = ttl; }
  public int getTtl () { return ttl; }

  public void setPacketSize (final int packetSize) { this.packetSize = packetSize; }
  public int getPacketSize () { return packetSize; }
  
  public void setTimeout (final long timeout) { this.timeout = timeout; }
  public long getTimeout () { return timeout; }
  
  public void setCharsetName (final String charsetName) { this.charsetName = charsetName; }
  public String getCharsetName () { return charsetName; }
}