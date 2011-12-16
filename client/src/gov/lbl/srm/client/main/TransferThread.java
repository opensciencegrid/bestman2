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


import java.io.*;
//import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.net.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.srm.client.transfer.*;
import gov.lbl.srm.client.transfer.globus.*;
import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;
import gov.lbl.srm.client.exception.*;

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

public void run () {

  _window.enableTransferButton(false,false);
   if(_window.isRequestCancel()) {
     _window.enableTransferButton(true,true);
     return;
   }
   _window.resetValues(_targetDir,_overwrite);

   boolean gotAllConcFiles=false;
   while(true) {
    if(done) break;
    try {
     if(_firstTime || gotAllConcFiles) {
       sleep(5000);  
     } 
     else {
       sleep(1000);  
     }

       if(frame.isUrlExists()) {
         if(_firstTime) {
           Vector vec = frame.getSrmFirstUrl(_firstTime);
           inputVec.clear();
           inputVec.addElement("vec is (firstTime)="+vec.size());
           util.printEventLog(_theLogger,"TransferThread.run",
                inputVec,silent,useLog);
           if(vec != null && vec.size() != 0) {
             ft = transferFiles (vec,_window.isGui());
             ft.addFileEventListener(_listener);
             ft.doFirstActiveTransfer();
             _firstTime = false;
             if(vec.size() >= _concurrency) gotAllConcFiles=true;
           }
         }
         else if(!gotAllConcFiles) {
           Vector vec = frame.getSrmFirstUrl(true);
           inputVec.clear();
           inputVec.addElement("vec is (gotAllConcFiles)="+vec.size());
           util.printEventLog(_theLogger,"TransferThread.run",
                inputVec,silent,useLog);
       
           if(vec != null && vec.size() != 0) {
             int numFilesStarted = ft.getNumFilesStarted();
             int numFilesCompleted = ft.getNumFilesCompleted();
             int numFilesCurrentlyRunning = 
                (numFilesStarted-numFilesCompleted);
             int xx = (_concurrency-numFilesCurrentlyRunning);
             inputVec.clear();
             inputVec.addElement("numFilesStarted="+numFilesStarted);
             inputVec.addElement("numFilesCompleted="+numFilesCompleted);
             inputVec.addElement("numFilesCurrentlyRunning="+
                numFilesCurrentlyRunning);
             inputVec.addElement("numFilesNeedsToStartMore="+xx);
             util.printEventLog(_theLogger,"TransferThread.run",
                inputVec,silent,useLog);
             if(vec.size() >= xx) {
                gotAllConcFiles=true;
                Vector aVec = new Vector();
                for(int i = 0; i < xx; i++) {
                  aVec.addElement(vec.elementAt(i));
                }
                ft.setFileInfo(aVec);
                ft.doFirstActiveTransfer();
                for(int i = xx; i < vec.size(); i++) {
                  ftw.addNextFileInfo((FileIntf)vec.elementAt(i));
                  ft.addNextFileInfo((FileIntf)vec.elementAt(i));
                }
             }//end if
             else {
               ft.setFileInfo(vec);
               ft.doFirstActiveTransfer();
             }
           }
         }
         else {
           Vector vec = frame.getSrmFirstUrl(_firstTime);
           inputVec.clear();
           inputVec.addElement("vec is (else)="+vec.size());
           util.printEventLog(_theLogger,"TransferThread.run",
                inputVec,silent,useLog);
           if(vec != null) { 
            ftw.addNextFileInfo((FileIntf)vec.elementAt(0));
            ft.addNextFileInfo((FileIntf)vec.elementAt(0));
           }
         }
        }//end if(frame.isUrlExists())
       } catch(ProxyNotFoundException pnfe) {
         interrupt();
         util.printEventLogException(_theLogger,"TransferThread.run",pnfe);
         ShowException.showMessageDialog(frame.getFrame(), 
			"Exception : " + pnfe.getMessage());
         _window.enableTransferButton(true,false);
         if(!silent) {
         System.out.println("TransferThread calling enableTransferButton " +
			" ProxyNotFoundException");
         } 
         return;
       }catch(DiskSpaceFullException dse) {
         interrupt();
         util.printEventLogException(_theLogger,"TransferThread.run",dse);
         ShowException.showMessageDialog(frame.getFrame(), 
		   "Exception : " + dse.getMessage());
         _window.enableTransferButton(true,false);
         if(!silent) {
         System.out.println("TransferThread calling enableTransferButton " +
			" DiskFullException");
         }
         return;
       } catch(SRMClientException srme) {
          interrupt();
          util.printEventLogException(_theLogger,"TransferThread.run",srme);
          ShowException.showMessageDialog(frame.getFrame(), 
		    "Exception : " + srme.getMessage());
          _window.enableTransferButton(true,false);
          if(!silent) {
          System.out.println("TransferThread calling enableTransferButton " +
		    " SRMClientException");
          }
          return;
       } catch(Exception e) {
          //e.printStackTrace();
          util.printEventLogException(_theLogger,"TransferThread.run",e);
          ShowException.showMessageDialog(frame.getFrame(), 
		   "Exception : " + e.getMessage());
          _window.enableTransferButton(true,false); 
          if(!silent) {
          System.out.println("TransferThread calling enableTransferButton " +
			" Exception");
          }
       }
    }//end while
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
  }
  else if(_reqMode.getModeType().equalsIgnoreCase("Put")) {
    ft.setTransferMode(SRMTransferMode.PUT);
  }
  else if(_reqMode.getModeType().equalsIgnoreCase("3partycopy")) {
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
