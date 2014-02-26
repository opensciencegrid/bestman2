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

import gov.lbl.adapt.srm.client.util.FileStatusGUI;
import gov.lbl.adapt.srm.client.util.FileProgress;
import gov.lbl.adapt.srm.client.util.FileExpectedSize;
import gov.lbl.adapt.srm.client.util.FileCurrentSize;
import gov.lbl.adapt.srm.client.util.FileTransferRate;
import gov.lbl.adapt.srm.client.util.FileColorCode;
import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.client.intf.*;

import java.util.Vector;
import java.io.*;
//import java.awt.Color;
import java.net.*;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import java.text.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;


public  class WrappedFT implements colorIntf {

  private MyISRMFileTransfer tu;
  private FileStatusGUI fgui;
  private ProgressBarTableModel pmodel;
  private FileProgress fileProgress; 
  private FileCurrentSize currentSize;
  private FileExpectedSize expectedSize;
  private FileTransferRate transferRate;
  private FileColorCode colorCode;
  private long size;
  private double prevSize;
  private int numSec;
  private int index;
  private int tableIndex;
  private boolean error;
  private String _errorMessage;
  private double Mbps;
  private DecimalFormat df;
  private String displayName = "";
  private FileIntf fIntf;

  public WrappedFT (MyISRMFileTransfer tu, FileStatusGUI fgui, 
		    FileIntf fIntf, ProgressBarTableModel pmodel, 
		    String displayName,
		    long size, int index, int tableIndex,  
		    DecimalFormat df, double Mbps) {
      
    this.tu = tu;
    this.fgui = fgui;
    this.fIntf = fIntf;
    //this.displayName = displayName.trim()+", "+size;
    this.displayName = displayName.trim();
    if(fgui != null) {
	fileProgress = fgui.getFileProgress();
	currentSize = fgui.getFileCurrentSize();
	expectedSize = fgui.getFileExpectedSize();
	transferRate = fgui.getFileTransferRate();
	colorCode = fgui.getFileColorCode();
	fileProgress.setDisplayName(this.displayName);
	fgui.setFileExpectedSize(""+size);
	expectedSize.setValue(""+size);
    }
    this.size = size;
    this.Mbps = Mbps;
    this.df = df;
    this.pmodel = pmodel;
    this.index = index;
    this.tableIndex = tableIndex;
  }

  public FileIntf getFileIntf () {
    return fIntf;
  }

  public boolean isRetry() {
    return fIntf.isRetry();
  }

  public boolean isNew() {
    return fIntf.isNew();
  }

  public boolean transferDone () {
      return this.tu.transferDone();
  }
    
    //public boolean transferOk () {
    //return this.tu.transferOk();
    //}
    
  public String getStatus () {
    return this.tu.getStatus();
  }

  public double getPrevSize() {
     return prevSize;
  }


  public synchronized void setValue (double value) {
    if(this.pmodel != null && fgui != null) {
      numSec++;
      if(prevSize != value) {
       while(pmodel.getLock()) {
         try {
           System.out.println("setValue waiting ...");
           wait();
         }catch(InterruptedException ie) {
            System.out.println("InterruptedException " + ie.getMessage());
         }
       }
       pmodel.setLock(true);
       int oldValue = fileProgress.getPercen();
       int newValue = (int)(((oldValue+value)*100)/size);
       if(newValue > 100) newValue = 100; 
          //fudging the percentage value here, because soemtimes the 
          //false value is given for the filesize, which is less than
          //actual file size, then the percentage looks bigger than 100.
       fileProgress.setPercen(newValue);
       fileProgress.setValue(""+newValue);
       currentSize.setValue(""+(long)value);
       double vv = ((value-prevSize)/(numSec))/Mbps*1000;
       transferRate.setValue(df.format(vv));
       long temp  = (((long)(value-prevSize)/numSec)/1000000);
       if(temp == 0) temp = 1;
       colorCode.setValue(""+(int)temp);
       prevSize = value;
       numSec = 0;
       pmodel.fireTableDataChanged();
       pmodel.setLock(false);
      }
    }
  }
  
  public void setTransferRate(double value) {
    if(pmodel != null && fgui != null) {
      FileIntf fIntf = fgui.getFileIntf();
      if(!fIntf.getCompleted() && value < size) {
        while(pmodel.getLock()) {
          try {
            System.out.println("setValue waiting ...");
            wait();
          }catch(InterruptedException ie) {
             System.out.println("InterruptedException " + ie.getMessage());
          }
         }
         pmodel.setLock(true);
         if(value > prevSize) {
           if(numSec > 0) {
             transferRate.setValue(""+(long)(value-prevSize)+"/"+numSec);
             long temp  = (((long)(value-prevSize)/numSec)/1000000);
             if(temp == 0) temp = 1;
             colorCode.setValue(""+(int)temp);
           }
        }
        else {
         transferRate.setValue(""+0+"/"+numSec);
         colorCode.setValue(""+0);
        }
     }
     pmodel.fireTableDataChanged();
     pmodel.setLock(false);
    }
  }

  public void setErrorOccured (boolean b, String errorMsg) {
     this.error = b;
     _errorMessage = errorMsg;  
  }

  public boolean getErrorOccured () {
     return this.error;
  }

  public String getErrorMessage () {
     return _errorMessage;
  }

  public int getIndex() {
    return this.index;
  }

  public int getTableIndex() {
    return this.tableIndex;
  }

  public String getTransferTime () {
    //System.out.println("\n\n**** getTransferTime.transferdone " + transferDone());
    //System.out.println("\n\n**** getTransferTime.tt " + tu.getTransferTimeInMilliSeconds());
    if(transferDone()) {
	return ""+this.tu.getTransferTimeInMilliSeconds();
    }
    return "Active";
  }

}
