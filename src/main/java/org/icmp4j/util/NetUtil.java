package org.icmp4j.util;

import java.net.NetworkInterface;
import java.net.InetAddress;
import java.util.Enumeration;
import java.io.IOException;

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
 * Date: Mar 27, 2017
 * Time: 11:43:51 PM
 */
public class NetUtil {

  /**
   * Returns the first ip address representing localhost (like 192.168.1.1), but not loopback (127.0.0.1).
   * This code adapted from http://www.jguru.com/faq/view.jsp?EID=15835 
   * For a machine with multiple ip addresses, this should return the ip address
   * for the network interface with the highest metric in a "route print"
   * @return String
   */
  public static String findFirstLocalHostIpv4Address () {

    // handle exceptions
    try {

      // iterate all NetworkInterfaces
      final Enumeration<NetworkInterface> networkInterfacesEnumeration = NetworkInterface.getNetworkInterfaces ();
      while (networkInterfacesEnumeration.hasMoreElements ()) {

        // next
        final NetworkInterface networkInterface = networkInterfacesEnumeration.nextElement ();
        final Enumeration<InetAddress> inetAddressEnumeration = networkInterface.getInetAddresses ();
        while (inetAddressEnumeration.hasMoreElements ()) {

          final InetAddress inetAddress = inetAddressEnumeration.nextElement ();
          final String address = inetAddress.getHostAddress ();
          if (
            inetAddress.isSiteLocalAddress () &&
            !inetAddress.isLoopbackAddress () &&
            IpAddressUtil.validateIpv4Address (address)) {

            // done
            return inetAddress.getHostAddress ();
          }
        }
      }

      // not found
      return null;
    }
    catch (final IOException e) {

      // propagate
      throw new RuntimeException (e);
    }
  }
}