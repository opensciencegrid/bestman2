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

import java.io.*;
import java.util.*;
import java.text.*;

import gov.lbl.adapt.srm.client.util.ShowException;
import gov.lbl.adapt.srm.client.util.FileStatusGUI;
import gov.lbl.adapt.srm.client.util.MyGlobusURL;
import gov.lbl.adapt.srm.client.intf.*;

import gov.lbl.srm.StorageResourceManager.*;

public class TimeOutCallBack2 extends Thread {
   private Object obj;
   private long startTimeStamp;
   private String target;
   private FileTransferTest ft;
   private Vector activeFiles;
   private Vector activeSRMFileTransfers;
   private FileStatusGUI fgui;
   private MyISRMFileTransfer tu;
   private FileIntf fIntf;
   private ProgressBarTableModel pmodel;
   private String displayName;
   private long sSize;
   private int idx2;
   private int idx3;
   private DecimalFormat df;
   private double Mbps;
   private int connectionTimeOutAllowed;
   private java.util.logging.Logger _theLogger;
   private boolean debug;
   private boolean silent;
   private boolean useLog;
   private Vector inputVec = new Vector ();

 public TimeOutCallBack2 (Object obj, long startTimeStamp) {
    this.obj = obj;
    this.startTimeStamp = startTimeStamp;
 }

 public void setParams(String target, FileTransferTest ft, Vector activeFiles,
        Vector activeSRMFileTransfers,
		FileStatusGUI fgui, MyISRMFileTransfer tu,
		FileIntf fIntf, ProgressBarTableModel pmodel, String displayName,
        long sSize, int idx2, int idx3, DecimalFormat df, double Mbps,
        int connectionTimeOutAllowed, java.util.logging.Logger logger,
	 boolean debug, boolean silent, boolean useLog) {

       this.target = target;
       this.ft = ft;
       this.activeFiles = activeFiles;
       this.activeSRMFileTransfers = activeSRMFileTransfers;
       this.fgui = fgui;
       this.tu = tu;
       this.fIntf = fIntf;
       this.pmodel = pmodel;
       this.displayName = displayName;
       this.sSize = sSize;
       this.idx2 = idx2;
       this.idx3 = idx3;
       this.df = df;
       this.Mbps = Mbps;
       this.connectionTimeOutAllowed = connectionTimeOutAllowed;
       this._theLogger = logger;
       this.debug = debug;
       this.silent = silent;
       this.useLog = useLog;
 }

 public void setObject(Object obj) {
    this.obj = obj;
 }

 public void run () {  
   try {
     long prevBytes = 0;
     long newBytes = 0;
     int count=0;
     while(true) {
       sleep(1000); 
       WrappedFT wft = (WrappedFT)obj; 
       if(wft.transferDone() || (wft.getStatus() !=null) || 
		wft.getErrorOccured()) { 
         interrupt();
         return;
       }//end if
       else {
        //boolean b = 
          //util.isConnectionTimeOut(startTimeStamp,connectionTimeOutAllowed);
        boolean b = false;
        String tPath = util.parseLocalSourceFileForPath(target);        
        newBytes = new File(tPath).length(); 
        //System.out.println(">>>target="+target + " " + 
		//connectionTimeOutAllowed + " " + newBytes + " " + prevBytes); 
        if(prevBytes == newBytes) {
          count++; 
          if(count == connectionTimeOutAllowed) {
             b = true;
          }
        }
        else {
          count = 0;
          prevBytes = newBytes;
        }
        if(b) {
          tu.stop(); //stops the file transfer thread.
          //retry now
          //System.out.println("this="+this);
          //System.out.println("startTimeStamp="+startTimeStamp);
          System.out.println("\nSRM-CLIENT: " + new Date() + 
                " GsiFTP Time out occured, "+ 
                " Retrying the gsiftp transfer now...");
          inputVec.clear();
          inputVec = new Vector();
          inputVec.addElement("GsiFTP Time out occured, " +
		"Retrying the gsiftp transfer now... " + new Date());
          inputVec.addElement("newbytes="+newBytes + 
	                      " prevBytes="+prevBytes+ " count="+count);
          util.printEventLog(_theLogger,"TimeOutCallBack2.run",
				inputVec,silent,useLog);
          try {
              MyGlobusURL toUrl = new MyGlobusURL(target,0);
              WrappedFT nWFT = new WrappedFT(tu,fgui,fIntf,
                         pmodel,displayName,sSize,idx2,idx3,df,Mbps);
              nWFT.setErrorOccured(true,"GsiFTP Time out");
              fIntf.setRetryTime(new Date());
              //fIntf.retry();
              ft.updateListeners(4,idx3,""+0);
              ft.updateListeners(11,idx3,"GsiFTP Time out");
              activeFiles.add(new File(toUrl.getPath()));
              activeSRMFileTransfers.add(nWFT);
          }catch(Exception e) {
              System.out.println("\nSRM-CLIENT: Exception="+e.getMessage());
              inputVec.clear();
              inputVec = new Vector();
              inputVec.addElement("Exception="+e.getMessage());
              util.printEventLog(_theLogger,"TimeOutCallBack2.run",
				inputVec,silent,useLog);
              util.printEventLogException(_theLogger, "TimeOutCallBack2.run",e);
          }
          interrupt(); //interrupts this thread 
          return;
        }//end if(timeout)
      }//end else
     }//end while
   }catch(Exception e) { 
     System.out.println("TimeOutCallBack2.Exception="+e.getMessage());
     util.printEventLogException(_theLogger, "TimeOutCallBack2.run",e);
   }
 }
}
