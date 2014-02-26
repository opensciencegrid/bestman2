/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
 * through Lawrence Berkeley National Laboratory (subject to receipt of any 
 * required approvals from the U.S. Dept. of Energy).  This software was 
 * developed under funding from the U.S. Department of Energy and is 
 * associated with the Berkeley Lab Scientific Data Management Group projects.
 * All rights reserved.
 * 
 * If you have questions about your rights to use or distribute this software, 
 * please contact Berkeley Lab's Technology Transfer Department at TTD@lbl.gov.
 * 
 * NOTICE.  This software was developed under funding from the 
 * U.S. Department of Energy.  As such, the U.S. Government has been granted 
 * for itself and others acting on its behalf a paid-up, nonexclusive, 
 * irrevocable, worldwide license in the Software to reproduce, prepare 
 * derivative works, and perform publicly and display publicly.  
 * Beginning five (5) years after the date permission to assert copyright is 
 * obtained from the U.S. Department of Energy, and subject to any subsequent 
 * five (5) year renewals, the U.S. Government is granted for itself and others
 * acting on its behalf a paid-up, nonexclusive, irrevocable, worldwide license
 * in the Software to reproduce, prepare derivative works, distribute copies to
 * the public, perform publicly and display publicly, and to permit others to
 * do so.
 *
*/
/**
 *
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

import gov.lbl.adapt.srm.client.exception.*;

import java.util.Vector;
import java.util.Properties;
import java.io.*;

import org.globus.ftp.*;
import org.ietf.jgss.GSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class LocalFileInfo {
  private String surl; 
  private String size;
  private String time;
  private String date;
  private String message;
  private boolean fileType;
  private boolean directoryType;
  private boolean linkType;

  public LocalFileInfo (String surl) { 
    this.surl = surl;
  }

  public String getSURL() {
    return surl;
  }

  public void setSize(String size) {
    this.size = size;
  }

  public void setTime(String time) { 
   this.time = time;
  }

  public void setDate(String date) { 
   this.date = date;
  }

  public String getSize() {
    return size;
  }
  
  public void setErrorMessage(String msg) {
    this.message = msg;
  }

  public String getErrorMessage() {
    return message;
  }

  public String getTime() {
    return time;
  }

  public String getDate() {
    return date;
  }

  public void setFile(boolean b) {
    fileType = b;
  }

  public void setDirectory(boolean b) {
    directoryType = b;
  }

  public void setSoftLink(boolean b) {
    linkType = b;
  }

  public boolean isFile() {
    return fileType;
  }

  public boolean isDirectory () {
   return directoryType;
  }

  public boolean isSoftLink () {
   return linkType;
  }
}
