/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2010, The Regents of the University of California, 
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
 * Email questions to SRM@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.srm.client.main;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientThreadWorker
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;
import java.util.Vector;
import java.util.HashSet;
import java.util.Date;

import gov.lbl.srm.client.transfer.*;
import gov.lbl.srm.client.transfer.globus.*;
import gov.lbl.srm.client.util.*;
import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.intf.*;

import org.ietf.jgss.GSSException;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SRMClientThreadWorker 
{

private String _name;
private ObjectFIFO idleWorkers;

private Thread internalThread;
private volatile boolean noStopRequested;
private int _size;
private Vector _activeFiles;
private Vector _activeSRMFileTransfers;
private FileTransferTest _parent;
private static Log logger;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private boolean debug; 
private static boolean silent;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMClientThreadWorker (ObjectFIFO idleWorkers, String name, 
		FileTransferTest parent, java.util.logging.Logger theLogger,
		boolean silent)
{
   this.idleWorkers = idleWorkers;
   _name = name;
   _parent = parent;
   _theLogger = theLogger;
   debug = _parent.getDebug();
   this.silent = silent;

   try {
     ClassLoader cl = this.getClass().getClassLoader(); 
     Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientThreadWorker");
     logger = LogFactory.getLog(c.getName());
   }catch(ClassNotFoundException cnfe) {
      System.out.println("ClassNotFoundException ");
      //throw new SRMClientException(cnfe.getMessage());
   }

   //just before returning, the thread should be created.
   noStopRequested = true;

   Runnable r = new Runnable() {
     public void run () {
        try {
          runWork ();
         }catch(Exception ex) {
           if(ex == null) {
            util.printEventLogException(_theLogger,"SRMClientThreadWorker.run", ex);
           }
         }
     }
   };

   internalThread = new Thread (r);
   internalThread.start();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  process
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void process (Vector activeFiles, 
 Vector activeSRMFileTransfers) throws InterruptedException {
   _size = activeFiles.size();
   _activeFiles = activeFiles;
   _activeSRMFileTransfers = activeSRMFileTransfers;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  runWork
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void runWork () {
  while(noStopRequested) {
    try {
      idleWorkers.add(this);
        if(_activeFiles != null && _activeSRMFileTransfers != null) {
          Object[] fileObj = _activeFiles.toArray (); 
	      Object[] srmFTObj = _activeSRMFileTransfers.toArray ();
          //updating current size of active files
	      if(fileObj.length == 0) {
            inputVec = new Vector ();
	        inputVec.addElement("No Active files to monitor");
            util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",
				inputVec,silent,true);
            if(debug) {
              System.out.println("SRM-CLIENT: no active files to monitor");
            }
	      }
          for(int i = 0; i < fileObj.length; i++) {
              double currSize = ((File)fileObj[i]).length();
	      WrappedFT wft = (WrappedFT) srmFTObj[i];
              FileIntf fIntf = wft.getFileIntf();
              fIntf.setActualFileSizeKnown(true);
              wft.setValue(currSize);
          }
	      boolean clearList = false;
	      for(int i = 0; i < srmFTObj.length; i++) {
	         WrappedFT wtu = (WrappedFT) srmFTObj[i];
	         if((wtu.transferDone()) || (wtu.getStatus() != null) ||
	      		(wtu.getErrorOccured())) {
                 if (!wtu.isNew()) {  ; }
                 else if(wtu.transferDone())  {
                    if(wtu.getStatus() != null) {
                      util.printMessage("SRM-CLIENT: " + new Date() + 
							" gsiftp transfer failed and error occured ",logger,silent);
                       inputVec = new Vector ();
                       inputVec.addElement("gsiftp transfer failed and error occured");
                       util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",inputVec,silent,true);
		              _parent.setErrorMessage(wtu.getStatus(), 
							wtu.getTableIndex());
		              _parent.setPreviousStatusPanel
							(4,wtu,wtu.getTableIndex(),wtu.getStatus());
                    } 
                    else {
                      if(debug) {
                         util.printMessage("SRM-CLIENT: " + new Date() + 
										" gsiftp transfer done",logger,silent);
                      }
                      inputVec = new Vector ();
                      inputVec.addElement("gsiftp transfer done");
                      util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",inputVec,silent,true);
                      double currSize = ((File)fileObj[i]).length();
		              _parent.setTimeTaken(wtu.getTransferTime(),
		    		     wtu.getTableIndex());
                       //Thread tempThread = new Thread();
                       //tempThread.sleep(1000);
		               _parent.setPreviousStatusPanel(2, 
			             wtu,wtu.getTableIndex(),""+(long)currSize);
                       wtu.setValue(currSize);
                    }
                 }
                 else if (wtu.getErrorOccured()) {
                    util.printMessage("SRM-CLIENT: " + new Date() + 
					   " gsiftp transfer failed  and error occured (1)=" + 
				       wtu.getErrorOccured(),logger,silent);
                    inputVec = new Vector ();
                    inputVec.addElement("gsiftp transfer failed and error occured");
                    util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",inputVec,silent,true);
                    String errorMessage = wtu.getErrorMessage();
		            _parent.setErrorMessage(errorMessage, wtu.getTableIndex());
		            _parent.setPreviousStatusPanel
							(4,wtu,wtu.getTableIndex(),"");
                 }
                 else if (wtu.getStatus() != null) {
                    util.printMessage("SRM-CLIENT: " + new Date() + 
						" gsiftp transfer failed and getStatus()="+wtu.getStatus(),logger,silent);
                    inputVec = new Vector ();
                    inputVec.addElement("gsiftp transfer failed and error occured");
                    util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",inputVec,silent,true);
		            _parent.setErrorMessage(wtu.getStatus(), 
			           wtu.getTableIndex());
		            _parent.setPreviousStatusPanel
							(4,wtu,wtu.getTableIndex(),wtu.getStatus());
                 }
	             if(!_parent.getAllDone()) {
                   try {
                     if(_parent.isNextTransfer()) {
                        if(_parent.isCancelPressed ()) {
		                   _parent.setCancelRequest
				              (5,wtu.getTableIndex());
		                   _parent.setSRMClientThreadDisable(true);
		                   break;
		                 }
                         if(_parent.isNextTransferAvailable()) {
                               _parent.incrementNumFilesCompleted();
                               inputVec = new Vector ();
	                       inputVec.addElement("NumFilesStarted="+
                                        _parent.getNumFilesStarted());
	                       inputVec.addElement("NumFilesCompleted="+
                                        _parent.getNumFilesCompleted());
                               util.printEventLog(_theLogger,
                                 "SRMClientThreadWorker.runWork",
				 inputVec,silent,true);
	                       WrappedFT nwtu = _parent.doNextActiveTransfer();
	                       File target = _parent.getCurrentTarget();
		                   _activeSRMFileTransfers.set(i,nwtu);
		                   _activeFiles.set(i,target);
                         }
		             }
                     else {
                      clearList = true;
                     }
                   }catch(ProxyNotFoundException pnfe) {
                     _parent.setSRMClientThreadDisable(false);
                     Thread.currentThread().interrupt (); //re-assert
                   }catch(DiskSpaceFullException dse) {
                     _parent.setSRMClientThreadDisable(false);
                     Thread.currentThread().interrupt (); //re-assert
                   }catch(GSSException gsse) {
                     _parent.setSRMClientThreadDisable(false);
                     Thread.currentThread().interrupt (); //re-assert
                   }catch(Exception e) {
                       util.printMessage("Excepton="+e.getMessage(),logger,silent);
                       util.printEventLogException(_theLogger,"SRMClientThreadWorker.run", e);
                      //ShowException.logDebugMessage(logger,e);
                   }
		        }
		        else {
		          clearList = true;
		        }
	         }
	      }
	  if(clearList) {
            inputVec = new Vector ();
	    inputVec.addElement("Clearing client thread");
            util.printEventLog(_theLogger,"SRMClientThreadWorker.runWork",
				inputVec,silent,true);
            if(debug) { 
              util.printMessage("SRM-CLIENT: clearing client thread",logger,silent);
            }
	    _parent.setSRMClientThreadDisable(true);
            _parent.setTransferMonitorThreadDisable();
	  }
        } 
      }catch(InterruptedException ex) {
        //ShowException.logDebugMessage(logger,ex);
        util.printMessage("Excepton(2)="+ex.getMessage(),logger,silent);
        util.printEventLogException(_theLogger,"SRMClientThreadWorker.run", ex);
        Thread.currentThread().interrupt (); //re-assert
      }
    }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  stopRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void stopRequest () {
   noStopRequested = false;
   internalThread.interrupt ();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isAlive
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isAlive () {
   return internalThread.isAlive ();
}

}
