package org.icmp4j.util;

import java.io.File;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.icmp4j.logger.Logger;

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
 * Time: 4:08:21 PM
 * 
 * Copied from org.shortpasta.jle.lang.WindowsProcessUtil on wed 9/14/2016
 */
public class WindowsProcessUtil {

  // my attributes
  private static final Logger logger = Logger.getLogger (WindowsProcessUtil.class);
  private static final AtomicReference<File> windowsRootDirectory = new AtomicReference<File> ();

  /**
   * Returns a File representing the given Windows system file.
   * For example, if you lookup "arp.exe", this method returns "C:\WINDOWS\system32\arp.exe"
   * @param fileName
   * @return File
   */
  public static File findWindowsSystemFile (final String fileName) {

    // lookup "c:\windows\system32\fileName"
    final File systemRoot = getWindowsRootDirectory ();
    final File system32Directory = new File (systemRoot, "system32");
    final File file = new File (system32Directory, fileName);

    // return the File if it does exist
    return file.exists () ?
      file :
      null;
  }

  /**
   * Returns a File representing the given Windows system file.
   * For example, if you lookup "arp.exe", this method returns "C:\WINDOWS\system32\arp.exe"
   * @param fileName
   * @return File
   * @throws RuntimeException if the File is not found
   */
  public static File getWindowsSystemFile (final String fileName) {

    // delegate
    final File file = findWindowsSystemFile (fileName);
    if (file != null) {
      return file;
    }

    // not found
    throw new RuntimeException ("file not found: " + fileName);
  }

  /**
   * Look for mstsc.exe in the standard places
   * @return File
   */
  private static File findWindowsRootDirectory () {

    // already cached?
    {
      final File windowsRootDirectory = WindowsProcessUtil.windowsRootDirectory.get ();
      if (windowsRootDirectory != null) {
        return windowsRootDirectory;
      }
    }

    // load env vars
    // SystemRoot=C:\WINDOWS
    // windir=C:\WINDOWS
    final Map<String, String> systemVariablesMap = new ProcessBuilder ().environment ();

    // look for SystemRoot
    {
      final String path = systemVariablesMap.get ("SystemRoot");
      logger.debug ("findWindowsRootDirectory (): looking up env var SystemRoot: " + path);
      if (path != null) {
        final File directory = new File (path);
        if (directory.exists ()) {

          // cache and recurse to ensure proper caching
          windowsRootDirectory.set (directory);
          return findWindowsRootDirectory ();
        }
      }
    }

    // look for SystemRoot
    {
      final String path = systemVariablesMap.get ("windir");
      logger.debug ("findWindowsRootDirectory (): looking up env var windir: " + path);
      if (path != null) {
        final File directory = new File (path);
        if (directory.exists ()) {

          // cache and recurse to ensure proper caching
          windowsRootDirectory.set (directory);
          return findWindowsRootDirectory ();
        }
      }
    }

    // not found
    return null;
  }

  /**
   * Helper: returns the Windows directory
   * @return File
   * @throws RuntimeException If the directory is not found
   */
  private static File getWindowsRootDirectory () {

    // delegate
    final File directory = findWindowsRootDirectory ();
    if (directory == null || !directory.isDirectory ()) {
      throw new RuntimeException ("Windows root directory not found");
    }

    // done
    return directory;
  }
}