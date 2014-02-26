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


import java.io.*;
//import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.net.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.exception.*;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// TransferThread
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class TransferThread extends Thread {

private SRMClientIntf frame;
private Log logger;
private java.util.logging.Logger _theLogger;
//private Object[] _fileInfo;
private int _concurrency;
private int _parCount;
private int _bufferSize;
private int _blockSize;
private Vector _pList;
private Request _reqMode;
private boolean _overwrite;
private FTPIntf _window;
private FileEventListener _listener;
private GSSCredential mycred;
private boolean isRenew;
private FileTransferTest ft;
private FileTransferPanel ftw;
private String _targetDir="";
private boolean _firstTime = true;
private ProgressBarTableModel _pmodel;
private boolean done=false;
private int _totalFiles;
private int _numFilesFailed;
private int _retryAllowed;
private int _retryTimeOut;
private int connectionTimeOutAllowed;
private boolean _debug;
private boolean silent;
private boolean useLog;
private boolean checkDiskSpace;
private boolean dcau;
private String gucScriptPath="";
private String gucEventLogPath="";
private PrintIntf pIntf;
private Vector inputVec = new Vector ();

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// TransferThread --- Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TransferThread (SRMClientIntf frame, 
	int concurrency, int parCount, int bufferSize, int blockSize, 
	boolean dcau, boolean overwrite,
	int totalFiles, Request reqMode, int retryAllowed, 
	int retryTimeOut, int connectionTimeOutAllowed, boolean debug, 
	boolean silent,boolean useLog, boolean checkDiskSpace, 
	String gucScriptPath, String gucEventLogPath, PrintIntf pIntf)  
	throws SRMClientException
{
   this.frame = frame;
   _concurrency = concurrency;
   _parCount = parCount;
   _bufferSize = bufferSize;
   _blockSize = blockSize;
   _overwrite = overwrite;
   _reqMode = reqMode;
   _totalFiles = totalFiles;
   _retryAllowed = retryAllowed;
   _retryTimeOut = retryTimeOut;
   this.connectionTimeOutAllowed = connectionTimeOutAllowed;
   _debug = debug;
   this.silent = silent;
   this.useLog = useLog;
   this.checkDiskSpace = checkDiskSpace;
   this.dcau = dcau;
   this.gucScriptPath = gucScriptPath;
   this.gucEventLogPath = gucEventLogPath;
   this.pIntf = pIntf;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFileInfo
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

/*
public void setFileInfo (Object[] fileInfo) {
   _fileInfo = fileInfo;
}
*/

public void setTargetDir(String targetDir) {
  _targetDir = targetDir;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setPModel
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setPModel (ProgressBarTableModel pmodel) {
  _pmodel = pmodel;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFTPIntf
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setFTPIntf (FTPIntf window) {
  _window = window;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFileEventListener
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setFileEventListener (FileEventListener listener) {
  _listener = listener;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setProtocolList
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setProtocolList (Vector pList) {
  _pList = pList;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setProxy 
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setProxy(GSSCredential mycred) {
  this.mycred = mycred;
  if(ft != null) {
    ft.setCredential (this.mycred);
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLogger
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLogger (Log logger, java.util.logging.Logger theLogger) {
  this.logger = logger;
  this._theLogger = theLogger;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRenew
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRenew (boolean b) {
  isRenew = b;
  if(ft != null) {
    ft.setRenew(b);
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRenew
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getRenew () {
  return isRenew;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setDone
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setDone(boolean b) {
  done = b;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// validURLValues
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private  boolean validURLValues (String surl, String turl) {
   boolean sok = false;
   boolean tok = false;;
   for(int i = 0; i < _pList.size() ; i++) {
      String protocol = (String) _pList.elementAt(i);
      if(surl.startsWith(protocol)) {
         sok = true;
      }
      if(turl.startsWith(protocol)) {
         tok = true;
      }
   }
   if(!sok || !tok)
     return false;
   return true;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//srmFileFailure
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void srmFileFailure() {
  if(ft != null) {
    ft.srmFileFailure();
  }
  else {
    _numFilesFailed++;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run() {
    _window.enableTransferButton(false,false);
    if(_window.isRequestCancel()) {
	_window.enableTransferButton(true,true);
	return;
    }
    _window.resetValues(_targetDir,_overwrite);
    
    boolean gotAllConcFiles=false;

    String tag = "TransferThread.run";
    SRMClientN.logMsg("kickstart isUrlExists="+frame.isUrlExists(), tag, "totalFiles="+_totalFiles);

    try {	
	sleep(5000);
	while (!frame.isUrlExists()) {		
	    sleep(5000);
	}
	
	Vector vec = frame.getSrmFirstUrl(true);
	txf(vec);
	int processedCounter = vec.size();
	while (processedCounter < _totalFiles) {
	    sleep(5000);
	    vec = frame.getSrmFirstUrl(true);
	    if (vec.size() > processedCounter) {
		Vector newcomer = new Vector();
		for (int i=0; i<vec.size(); i++) {
		    FileIntf curr = (FileIntf)(vec.get(i));
		    //System.out.println("\t"+i+"th:"+curr.getFileStatus());
		    //System.out.println("\t"+i+"th:"+curr.getStatusLabel());

		    if (!curr.getStatusLabel().startsWith("Done") && !curr.getStatusLabel().startsWith("Fail")) {
			newcomer.add(curr);
		    }
		}
		txf(newcomer);
		processedCounter = vec.size();
	    }	    
	}       

	while (true) {	    
	    boolean allDone = true;
	    vec = frame.getSrmFirstUrl(true);
	    for (int i=0; i<vec.size(); i++) {	       
		FileIntf curr = (FileIntf)(vec.get(i));
		//System.out.println("\t\t"+i+"th:"+curr.getStatusLabel());
		if (!curr.getStatusLabel().startsWith("Done") && !curr.getStatusLabel().startsWith("Fail")) {
		    allDone = false;
		}
	    }
	    
	    if (allDone) {	    
		ft.setSRMClientThreadDisable(true);
		break;
	    } else {
		sleep(2000);
	    }
	}
	
    } catch (Exception e) {
	handleException(e);    
	System.exit(1);
    } finally {
	SRMClientN.logMsg("end", tag, null);
    }
}

    private void txf(Vector vec) throws Exception {
	String tag = "TransferThread.txf()";
	SRMClientN.logMsg("files to process="+vec.size(), tag, null);
	if(vec != null && vec.size() != 0) {
	    /*ft = */transferFiles (vec,_window.isGui());
	    ft.addFileEventListener(_listener);
	    ft.adtTransfer();
	}		

    }
private void handleException(Exception ex) {
    interrupt();
    util.printEventLogException(_theLogger,"TransferThread.run",ex);
    ShowException.showMessageDialog(frame.getFrame(), "Exception : " + ex.getMessage());
    
    _window.enableTransferButton(true,false);
    if(!silent) {
	System.out.println("TransferThread calling enableTransferButton:" + ex.getMessage());			   
	ex.printStackTrace();
    } 
    return;
}

public void setNewConcurrency(int conc) {
  ftw.setNewConcurrency(conc);
  ft.setNewConcurrency(conc);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setTotalFiles
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTotalFiles (int tFiles) {
   _totalFiles = tFiles;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// transferFiles
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    private FileTransferTest transferFiles(Vector vec, boolean isGui) {
	Vector index = new Vector();
	Vector vec1 = new Vector();
	Vector vec2 = new Vector();
	
	for(int i = 0; i < vec.size(); i++) {
	    FileIntf file = (FileIntf) vec.elementAt(i);
	    file.setMode(_reqMode.getModeType());
	    if(!file.isUsed() && !file.isSkipped()) {
		index.addElement(new Integer(file.getLabel()));
		vec1.addElement(file); 
		vec2.addElement(file); 
	    }
	}
	
	ftw = new FileTransferPanel (vec1, _concurrency, logger, _theLogger,
				     _window, isRenew,isGui,_reqMode.getModeType(),silent,useLog);
	
	ft = new FileTransferTest(vec2,_concurrency, _bufferSize, _blockSize, dcau,
				  index, this.mycred, isRenew, (_totalFiles-_numFilesFailed), 
				  _retryAllowed,_retryTimeOut,connectionTimeOutAllowed,_pmodel,
				  _theLogger,_debug,silent, useLog,checkDiskSpace,gucScriptPath,gucEventLogPath,pIntf);
	
	ft.setParallelism(_parCount);
	
	
	if(_reqMode.getModeType().equalsIgnoreCase("Get")) {
	    ft.setTransferMode(SRMTransferMode.GET);
	} else if(_reqMode.getModeType().equalsIgnoreCase("Put")) {
	    ft.setTransferMode(SRMTransferMode.PUT);	
	} else if(_reqMode.getModeType().equalsIgnoreCase("3partycopy")) {
	    ft.setTransferMode(SRMTransferMode.THIRDPARTY);
	}
	
	ft.setParent(ftw);
	return ft;
    }


private synchronized void addNextFileInfo(FileIntf fIntf) {
  //System.out.println(">>>Adding into next file info " + fIntf.getSURL());
  fIntf.setMode(_reqMode.getModeType());
  ftw.addNextFileInfo(fIntf);
  ft.addNextFileInfo(fIntf);
}

}
