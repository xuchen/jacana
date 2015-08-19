// Copyright 2010-2012 Benjamin Van Durme. All rights reserved.
// This software is released under the 2-clause BSD license.
// See jerboa/LICENSE, or http://cs.jhu.edu/~vandurme/jerboa/LICENSE

// Benjamin Van Durme, vandurme@cs.jhu.edu, 29 Oct 2010

package edu.jhu.jacana.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Vector;
import java.io.File;
import java.util.regex.Pattern;

/**
   @author Benjamin Van Durme

   Utility functions for dealing with files.
*/
public class FileManager {
  private static final Logger logger = Logger.getLogger(File.class.getName());
  private static File[] fileArr = new File[0];

  /**
     Returns an array of files that match the filenames.
			 
     Names of files (not directories) may include wildcards, which will be
     checked with Java regexps against a file listing in the specified
     directory.
  */
  // Property:
  // FileManager.randomizeFiles : (Boolean) defaults to false, when true will
  //                              shuffle the array of files, meant for load balancing
  public static File[] getFiles (String[] filenames) {
    Vector<File> fileVector = new Vector<File>();
    File dir;
    String filePatternString;

    for (String filename : filenames) {
	    if (!filename.matches(".*/.*"))
        filename = "./" + filename;
	    dir = new File(filename.substring(0,filename.lastIndexOf(File.separator) +1));
	    filePatternString = filename.substring(filename.lastIndexOf(File.separator) +1);
	    Pattern p = Pattern.compile(filePatternString);
	    if (! dir.exists())
        logger.severe("Directory does not exist [" + dir.getName() + "]");
	    else {
        for (String s : dir.list()) {
          if (p.matcher(s).matches()) {
            fileVector.addElement(new File(dir,s));
          }
        }
	    }
    }

    return (File[]) fileVector.toArray(fileArr);
  }

  /**
     Returns new File(filename)
   */
  public static File getFile (String filename) {
    return new File(filename);
  }

  /**
     Returned BufferedReader is set to UTF-8 encoding
   */
  public static BufferedReader getReader (String filename) throws IOException {
    return getReader(new File(filename), "UTF-8");
  }

  public static BufferedReader getReader (String filename, String encoding) throws IOException {
    return getReader(new File(filename), encoding);
  }

  /**
     Returned BufferedReader is set to UTF-8 encoding
   */
  public static BufferedReader getReader (File file) throws IOException {
    return getReader(file, "UTF-8");
  }

  public static FileInputStream getFileInputStream (File file) throws IOException {
    logger.info("Opening FileInputStream [" + file.getCanonicalPath() + "]");
    return new FileInputStream(file);
  }
  public static ObjectInputStream getFileObjectInputStream (File file) throws IOException {
    logger.info("Opening file-backed ObjectInputStream [" + file.getCanonicalPath() + "]");
    return new ObjectInputStream(new FileInputStream(file));
  }
  public static ObjectInputStream getFileObjectInputStream (String filename) throws IOException {
    return getFileObjectInputStream(new File(filename));
  }

  /**
     Returns a BufferedReader from the given file.

     If filename ends in .gz suffix, will wrap the FileReader appropriately.
  */
  public static BufferedReader getReader (File file, String encoding) throws IOException {
    InputStreamReader isr;
    GZIPInputStream gs;

    // logger.info("Opening [" + file.getCanonicalPath() + "]");
    FileInputStream fis = new FileInputStream(file);
    if (file.getName().endsWith(".gz")) {
	    gs = new GZIPInputStream(fis);
	    isr = new InputStreamReader(gs,encoding);
    } else
	    isr = new InputStreamReader(fis,encoding);

    return new BufferedReader(isr);
  }

  public static BufferedWriter getWriter(String filename, String encoding, boolean append) throws IOException {
    return getWriter(new File(filename), encoding, append);
  }
  
  public static BufferedWriter getWriter(String filename, String encoding) throws IOException {
    return getWriter(new File(filename), encoding, false);
  }
  public static BufferedWriter getWriter(String filename, boolean append) throws IOException {
    return getWriter(new File(filename), "UTF-8", append);
  }
  /**
     Returned BufferedWriter is set to UTF-8 encoding.
   */
  public static BufferedWriter getWriter(String filename) throws IOException {
    return getWriter(new File(filename), "UTF-8", false);
  }

  /**
     Returned BufferedWriter is set to UTF-8 encoding.
   */
  public static BufferedWriter getWriter (File file) throws IOException {
    return getWriter(file, "UTF-8", false);
  }

  /**
     Returns a BufferedWriter aimed at the given file.

     If filename ends in .gz suffix, will wrap the writer appropriately.
   */
  public static BufferedWriter getWriter (File file, String encoding, boolean append) throws IOException {
    OutputStreamWriter osw;
    GZIPOutputStream gs;

    logger.info("Opening [" + file.getCanonicalPath() + "]");
    FileOutputStream fos = new FileOutputStream(file, append);
    if (file.getName().endsWith(".gz")) {
	    gs = new GZIPOutputStream(fos);
	    osw = new OutputStreamWriter(gs,encoding);
    } else
	    osw = new OutputStreamWriter(fos,encoding);

    return new BufferedWriter(osw);
  }
  
  public static String getUserHome() {
		return System.getProperty( "user.home" ) + "/";
  }
  
  public static boolean fileExists(String fname) {
    if (fname == null || fname.equals("")) return false;
    File file = new File(fname);
    return file.exists();
  }
  
  public static String getResource(String fname) {
    if (fileExists(fname))
      return fname;
    else {
      String newfname = System.getProperty("JACANA_HOME")+"/"+fname;
      if (fileExists(newfname)) {
          return newfname;
      } else {
	      System.err.println("Fatal error: can't find file/folder " + newfname);
	      System.err.println("Set up your JACANA_HOME properly! Currently it is "+System.getProperty("JACANA_HOME"));
	      System.exit(0);
          return null;
      }
    }
  }
  
  public static String getFreebaseResource(String fname) {
	  return getFreebaseResource(fname, true);
  }
  
  public static String getFreebaseResource(String fname, boolean quit) {
    if (fileExists(fname))
      return fname;
    else {
      String newfname = System.getProperty("FREEBASE_DATA")+"/"+fname;
      if (fileExists(newfname)) {
          return newfname;
      } else {
    	  newfname = System.getProperty("user.home") + "/Halo2/freebase/" + fname;
    	  if (fileExists(newfname)) {
    		  return newfname;
    	  } else {
    		  if (quit) {
    			  System.err.println("Fatal error: can't find file/folder " + fname);
    			  System.err.println("Set up your FREEBASE_DATA properly! Currently it is "+System.getProperty("FREEBASE_DATA"));
    			  System.exit(0);
    		  } else {
    			  System.err.println("warning: can't find file/folder " + fname);
    			  System.err.println("It could be that you don't have that file, or that");
    			  System.err.println("your FREEBASE_DATA is not set properly! Currently it is "+System.getProperty("FREEBASE_DATA"));
    			  return null;
    		  }
   			  return null;
    	  }
      }
    }
  }
  
  public static String readFile(String fname) throws IOException {
	  BufferedReader reader = getReader(fname);
	  StringBuffer lines = new StringBuffer();
	  String ls = System.getProperty("line.separator");
	  String line;
	  while ((line = reader.readLine()) != null) {
		  lines.append(line);lines.append(ls);
	  }
	  reader.close();
	  return lines.toString();
  }
}