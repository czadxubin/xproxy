package org.icmp4j.exception;

/**
 * Copyright 2016 and beyond, IST, Inc.
 * http://www.ist97.com
 * <p/>
 * User: sal
 * Date: 2/13/2021
 * Time: 4:44 PM
 */
public class ParseException extends RuntimeException {

  /**
   * Def ctor
   * @param message
   * @param e
   */
  public ParseException(final String message, final Exception e) {

    // propagate
    super(message, e);
  }
}