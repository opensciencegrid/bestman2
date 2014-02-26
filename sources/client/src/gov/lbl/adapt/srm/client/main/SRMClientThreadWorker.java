/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2010-2014, The Regents of the University of California, 
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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientThreadWorker
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;
import java.util.Vector;
import java.util.HashSet;
import java.util.Date;

import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.exception.*;
import gov.lbl.adapt.srm.client.intf.*;

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
    //private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private boolean debug; 
private static boolean silent;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMClientThreadWorker (ObjectFIFO idleWorkers, String name, 
			      FileTransferTest parent,
			      boolean silent)
{
    this.idleWorkers = idleWorkers;
    _name = name;
    _parent = parent;
    debug = _parent.getDebug();
    this.silent = silent;
    
    try {
	ClassLoader cl = this.getClass().getClassLoader(); 
	Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.SRMClientThreadWorker");
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
		    SRMClientN.logEx("SRMClientThreadWorker::run", ex);
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
    /*
    Runnable r = new Runnable() {
	    public void run () {
		try {
		    runWork ();
		}catch(Exception ex) {
		    SRMClientN.logEx("SRMClientThreadWorker::run", ex);
		}
	    }
	};
    
    internalThread = new Thread (r);
    internalThread.start();
    */
}

private void checkWrappedFT(WrappedFT wtu, String tag, double currSize) {
    if (!wtu.isNew()) {  ; }
    else if(wtu.transferDone())  {
	if(wtu.getStatus() != null) {
	    SRMClientN.logMsg(" gsiftp transfer failed and error occured ",tag, "SRM-CLIENT: "+ new Date());
	    _parent.setErrorMessage(wtu.getStatus(), wtu.getTableIndex());
	    _parent.setPreviousStatusPanel(4,wtu,wtu.getTableIndex(),wtu.getStatus());
	} else {
	    SRMClientN.logMsg("gsiftp transfer done", tag, "SRM-CLIENT:"+new Date());		
	    _parent.setTimeTaken(wtu.getTransferTime(),wtu.getTableIndex());
	    _parent.setPreviousStatusPanel(2, wtu,wtu.getTableIndex(),""+(long)currSize);
	    wtu.setValue(currSize);
	}                 
    } else if (wtu.getErrorOccured()) {
	SRMClientN.logMsg("gsiftp transfer failed  and error occured (1)=" + wtu.getErrorOccured(),tag, "SRM-CLIENT: " + new Date());
	String errorMessage = wtu.getErrorMessage();
	_parent.setErrorMessage(errorMessage, wtu.getTableIndex());
	_parent.setPreviousStatusPanel(4,wtu,wtu.getTableIndex(),"");		      	
    } else if (wtu.getStatus() != null) {
	SRMClientN.logMsg(" gsiftp transfer failed and getStatus()="+wtu.getStatus(), tag, "SRM-CLIENT:"+new Date());
	_parent.setErrorMessage(wtu.getStatus(), wtu.getTableIndex());	
	_parent.setPreviousStatusPanel(4,wtu,wtu.getTableIndex(),wtu.getStatus());
    }    
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  runWork
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void runWork () {
    String tag = "SRMClientThreadWorker.runWork ";
    SRMClientN.logMsg(this+"start noStopRequested="+noStopRequested, tag, null);

    try {	
	while (noStopRequested) {
	    idleWorkers.add(this);	    

	    if ((_activeFiles == null) || (_activeSRMFileTransfers == null)) {
		continue;
		}
	    
	    Object[] fileObj = _activeFiles.toArray (); 
	    Object[] srmFTObj = _activeSRMFileTransfers.toArray ();
	    
	    //updating current size of active files
	    if(fileObj.length == 0) {
		SRMClientN.logMsg("No Active files to monitor", tag, "SRM-CLIENT:");
		continue;
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
		if ((wtu.transferDone()) || (wtu.getStatus() != null) ||(wtu.getErrorOccured())) {
		    _activeSRMFileTransfers.remove(wtu);
		    _activeFiles.remove(fileObj[i]);

		    double currSize = ((File)fileObj[i]).length();
		    checkWrappedFT(wtu, tag, currSize );
		    if(!_parent.getAllDone()) {
			try {
			    if (_parent.isNextTransfer()) {
				if (_parent.isCancelPressed ()) {
				    SRMClientN.logMsg("cancelPressed", tag, null);
				    _parent.setCancelRequest(5,wtu.getTableIndex());					  
				    _parent.setSRMClientThreadDisable(true);
				    break;
				}
				
				/*if(_parent.isNextTransferAvailable()) {
				  }*/

			    } else {
				SRMClientN.logMsg("No more txfs", tag, null);
				FileTransferTest._adtTxf.setFinished();
				clearList = true;
			    }
			}catch(ProxyNotFoundException pnfe) {
			    pnfe.printStackTrace();
			    _parent.setSRMClientThreadDisable(false);
			    Thread.currentThread().interrupt (); //re-assert
			}catch(DiskSpaceFullException dse) {
			    dse.printStackTrace();
			    _parent.setSRMClientThreadDisable(false);
			    Thread.currentThread().interrupt (); //re-assert
			}catch(GSSException gsse) {
			    gsse.printStackTrace();
			    _parent.setSRMClientThreadDisable(false);
			    Thread.currentThread().interrupt (); //re-assert
			}catch(Exception e) {
			    util.printMessage("Excepton="+e.getMessage(),logger,silent);
			    SRMClientN.logEx(tag, e);			    
			}		      
		    } else {
			clearList = true;
		    }
		}
	    }
	    if(clearList) {		       
		///_parent.setSRMClientThreadDisable(true);
		_parent.setTransferMonitorThreadDisable();
		SRMClientN.logMsg("Clearing client thread "+noStopRequested, tag,"SRM-CLIENT");
	    }
	    //} 
	}
    }catch(InterruptedException ex) {
	//ShowException.logDebugMessage(logger,ex);
	util.printMessage("Excepton(2)="+ex.getMessage(),logger,silent);
	SRMClientN.logEx(tag, ex);
	Thread.currentThread().interrupt (); //re-assert
    } finally {
	SRMClientN.logMsg(this+"end "+noStopRequested, tag, "SRM-CLIENT");
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
