package org.icmp4j.util;

import java.util.List;

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
 * Time: 11:44:39 PM
 */
public class IpAddressUtil {

  /**
   * Returns true if the given address is an ipv4 address
   * @param address
   * @return boolean
   */
  public static boolean validateIpv4Address (final String address) {

    // delegate
    final int expectedOctetCount = 4;
    return validateIpAddressOrSegment (address, expectedOctetCount);
  }

  /**
   * Returns true if the given address is an ipv4 address or segment
   * 
   * Consider: http://www.regular-expressions.info/examples.html
   * 
   * @param address
   * @param expectedOctetCount
   * @return boolean
   */
  private static boolean validateIpAddressOrSegment (
    final String address,
    final int expectedOctetCount) {

    // convert the "1.2.3.4" into octents
    final List<String> octetList = getOctetList (address);

    // there must be 4 octets
    if (octetList.size () != expectedOctetCount) {
      return false;
    }

    // iterate all octets
    for (final String octetAsString : octetList) {

      // handle exceptions
      try {

        // parse 
        final int octet = Integer.parseInt (octetAsString);

        // the octet is a number: assert its range
        if (octet < 0 || octet > 255) {
          return false;
        }
      }
      catch (final NumberFormatException e) {

        // the octet is not a number: this is not an IP address.
        return false;
      }
    }

    // validation passed
    return true;
  }
  
  /**
   * Returns a List<String> representing the octets from the given address
   * @param address
   * @return List<String>
   */
  public static List<String> getOctetList (final String address) {

    // convert the "1.2.3.4" into octents
    return StringUtil.splitLoose (address, "\\.");
  }
}
