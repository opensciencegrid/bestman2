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

package gov.lbl.adapt.srm.client.util;

import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.main.*;
import gov.lbl.adapt.srm.client.exception.*;


import org.globus.util.GlobusURL;

import org.apache.commons.logging.Log;

public class FileStatusGUI {
  private FileIntf fIntf;
  private FileProgress pbar;
  private FileExpectedSize expectedSize;
  private FileHostInfo hostInfo;
  private FileCurrentSize currentSize;
  private FileTransferRate transferRate;
  private FileColorCode colorCode;
  private boolean showSize=false;

  public FileStatusGUI(FileIntf fIntf) throws Exception {
     this.fIntf = fIntf;
     this.pbar = new FileProgress();
     this.showSize = fIntf.getShowSize();
     this.expectedSize = new FileExpectedSize(fIntf.getExpectedSize());
     this.hostInfo = new FileHostInfo(fIntf);
     this.currentSize = new FileCurrentSize();
     this.transferRate = new FileTransferRate();
     this.colorCode = new FileColorCode();
  }


  public FileProgress getFileProgress() {
    return this.pbar;
  }

  public boolean getShowSize() {
    return showSize;
  }

  public FileCurrentSize getFileCurrentSize () {
    return currentSize;
  }

  public FileExpectedSize getFileExpectedSize () {
    return expectedSize;
  }

  public void setFileExpectedSize(String size) {
     this.expectedSize = new FileExpectedSize(size);
  }

  public FileHostInfo getFileHostInfo () {
    return hostInfo;
  }

  public FileTransferRate getFileTransferRate () {
    return transferRate;
  }

  public FileColorCode getFileColorCode () {
    return colorCode;
  }

  public FileIntf getFileIntf () {
    return fIntf;
  }
}
