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

import gov.lbl.srm.client.util.ShowException;
import gov.lbl.srm.client.util.FileStatusGUI;
import gov.lbl.srm.client.util.MyGlobusURL;
import gov.lbl.srm.client.intf.FileIntf;
import gov.lbl.srm.client.intf.MyISRMFileTransfer;
import gov.lbl.srm.client.intf.PrintIntf;
import gov.lbl.srm.client.transfer.*;
import gov.lbl.srm.client.transfer.globus.*;
import gov.lbl.srm.client.util.*;
import gov.lbl.srm.client.exception.*;

import java.util.Vector;
import java.util.Date;
import java.io.*;
import java.net.*;

import javax.swing.JProgressBar;
import javax.swing.JFrame;
import java.text.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// FileTransferTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public  class FileTransferTest {

private int parallelism;
private int concurrency;
private int bufferSize;
private int blockSize;
private String error;
private SRMTransferMode mode;
private GSSCredential credential;
private Vector pbars = new Vector ();
private Vector _activeFiles = new Vector ();
private Vector _activeSRMFileTransfers = new Vector ();
private Vector _fInfo;
private int nextPosition = 0;
private SRMClientThreadMain _srmClientThread;
private SRMClientThreadPool _srmClientThreadPool;
private FileTransferPanel _parent;
private Vector _listener = new Vector();
private Vector index = new Vector ();
private int totalFiles;
private boolean isRenew;
private boolean warning;
private DecimalFormat df = new DecimalFormat("#,##0.00");
private double Mbps = 1024*1024/8;
private TransferRateMonitorThread _transferRateMonitorThread;
private ProgressBarTableModel pmodel;
private boolean _disableCalledOnce = false;
private int _retryAllowed;
private int _retryTimeOut;
private boolean _debug;
private boolean silent;
private boolean useLog;
private boolean dcau;
private boolean checkDiskSpace;
private Log _logger;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private String gucScriptPath="";
private String gucEventLogPath="";
private boolean guc = false;
private int connectionTimeOutAllowed;
private PrintIntf pIntf;
private int numFilesStarted;
private int numFilesCompleted;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// FileTransferTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public FileTransferTest ( Vector fInfo, 
  	int conc, int bufferSize, int blockSize, boolean dcau, Vector index, 
	GSSCredential mycred, boolean isRenew, int totalFiles, 
	int retryAllowed, int retryTimeOut, int connectionTimeOutAllowed, 
	ProgressBarTableModel pmodel, 
	java.util.logging.Logger theLogger,
	boolean debug,boolean silent, boolean useLog, boolean checkDiskSpace,
	String gucScriptPath, String gucEventLogPath, PrintIntf pIntf) {

  this.pmodel = pmodel;
  this._theLogger = theLogger;
  _srmClientThreadPool = new SRMClientThreadPool(1, this,_theLogger,silent);
  _srmClientThread = new SRMClientThreadMain(_srmClientThreadPool, this, _theLogger,silent);
  _srmClientThread.start ();
  _fInfo = fInfo;
  this.concurrency = conc;
  this.bufferSize = bufferSize;
  this.blockSize = blockSize;
  this.index = index;
  this.credential = mycred;
  this.isRenew = isRenew;
  this._debug = debug;
  this.silent = silent;
  this.useLog = useLog;
  this.checkDiskSpace = checkDiskSpace;
  this._retryAllowed = retryAllowed;
  this._retryTimeOut = retryTimeOut;
  this.connectionTimeOutAllowed = connectionTimeOutAllowed;
  this.totalFiles = totalFiles+(totalFiles*_retryAllowed);
  this.dcau = dcau;
  this.gucScriptPath = gucScriptPath;
  this.gucEventLogPath = gucEventLogPath;
  this.pIntf = pIntf;
  if(!gucScriptPath.equals("")) {
    guc = true; 
  }
  inputVec = new Vector ();
  inputVec.addElement("concurrency="+conc);
  inputVec.addElement("bufferSize="+bufferSize);
  inputVec.addElement("blockSize="+blockSize);
  inputVec.addElement("index="+index);
  inputVec.addElement("isRenew="+isRenew);
  inputVec.addElement("debug="+debug);
  inputVec.addElement("silent="+silent);
  inputVec.addElement("useLog="+useLog);
  inputVec.addElement("checkDiskSpace="+checkDiskSpace);
  inputVec.addElement("retryAllowed="+retryAllowed);
  inputVec.addElement("retryTimeOut="+retryTimeOut);
  inputVec.addElement("connectionTimeOutAllowed="+connectionTimeOutAllowed);
  inputVec.addElement("totalFiles="+totalFiles);
  inputVec.addElement("dcau="+dcau);
  inputVec.addElement("gucScriptPath="+gucScriptPath);
  inputVec.addElement("gucEventLogPath="+gucEventLogPath);
  inputVec.addElement("guc="+guc);
  util.printEventLog(_theLogger,"FileTransferTest.constructor",
                inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// addFileEventListener
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void addFileEventListener(FileEventListener fEListener) {
  _listener.add(fEListener);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setParallelism
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setParallelism(int parallelism) {
     this.parallelism = parallelism;
}

public void setNewConcurrency(int conc) {
  concurrency = conc;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferMode
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferMode(SRMTransferMode mode) {
    this.mode = mode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCredential
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCredential(GSSCredential cred) {
    this.credential = cred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setParent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setParent(FileTransferPanel parent) {
  _parent = parent;
  _logger = _parent.getLogger();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFileInfo
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void setFileInfo(Vector vec) {
  int size = vec.size();
  for(int i = 0; i < size; i++) {
    FileIntf fIntf = (FileIntf) vec.elementAt(0);
    addNextFileInfo(fIntf);
    vec.remove(0);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doFirstActiveTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doFirstActiveTransfer() throws SRMClientException,
 ProxyNotFoundException, DiskSpaceFullException {

  inputVec = new Vector ();
  inputVec.addElement("Inside doFirstActiveTransfer");
  util.printEventLog(_theLogger,"FileTransferTest.doFirstActiveTransfer",
                inputVec,silent,useLog);

  if(_debug) {
    inputVec = new Vector();
    inputVec.addElement("Inside doFirstActiveTransfer ");
    inputVec.addElement("concurrecy="+this.concurrency);
    util.printEventLog(_theLogger,
        "FileTransferTest.doFirstActiveTransfer",inputVec,silent,useLog);
  }
  int i = 0;
  int fInfoSize = _fInfo.size();
  if(fInfoSize > concurrency)
    fInfoSize = concurrency;

  inputVec = new Vector();
  inputVec.addElement("Inside doFirstActiveTransfer ");
  inputVec.addElement("fInfoSize="+fInfoSize);
  util.printEventLog(_theLogger,"FileTransferTest.doFirstActiveTransfer",
		inputVec,silent,useLog);

  for(i = 0; i < fInfoSize; i++) {
    int idx = ((Integer)index.elementAt(i)).intValue();
    FileIntf fIntf = (FileIntf) _fInfo.elementAt(i);
    fIntf.used(true); 
    String source = fIntf.getSURL();
    String target = fIntf.getTURL();
    String eSize = fIntf.getExpectedSize();
    boolean lahfs = fIntf.getLahfs();
    MyISRMFileTransfer tu;
    try {
    if(this.mode == SRMTransferMode.PUT) {
      source = "file:///"+SRMClientN.parseLocalSourceFileForPath(source);
    }
    if(this.mode == SRMTransferMode.GET) {
      target = "file:///"+SRMClientN.parseLocalSourceFileForPath(target);
    }
    }catch(Exception ee) {
      throw new SRMClientException(ee.getMessage());
    }
    util.printMessage
		("\nSRM-CLIENT: " + new Date() + " start file transfer",null,silent);
    util.printMessage
		("\nSRM-CLIENT: " + new Date() + " start file transfer." ,pIntf);
    if(silent) {
      System.out.println(_fInfo);
    }
    util.printMessage("SRM-CLIENT:Source="+source,null,silent);
    util.printMessage("SRM-CLIENT:Target="+target,null,silent);
    if(lahfs) {
      tu = new LahfsFileTransfer(source,target);
    }
    else if(guc) {
      tu = new GUCScriptFileTransfer
	(source,target,_debug,silent,gucScriptPath,gucEventLogPath);
    }
    else {
      tu = new MySRMFileTransfer(source, target);
    }
    tu.setLogger(_logger,_theLogger,silent,useLog,_debug);
    tu.setTransferMode(this.mode);
    
    if(dcau) {
       tu.setDCAU(dcau);
    }
    //else no need to set, in SRMFileTransfer dcau is false by default
    String protocol = "";
    String host = "";
    String filename="";
    String displayName = "";
    try { 
       MyGlobusURL fromUrl = new MyGlobusURL(source,0);
       MyGlobusURL toUrl = new MyGlobusURL(target,0);
       inputVec = new Vector();
       inputVec.addElement("FromURLPath="+fromUrl.getPath());
       inputVec.addElement("ToURLPath="+toUrl.getPath());
       util.printEventLog(_theLogger,
		"doFirstActiveTransfer.Params",inputVec,silent,useLog);
       if(this.mode == SRMTransferMode.GET) {
         protocol = fromUrl.getProtocol();
         host = fromUrl.getHost();
         int ii = source.lastIndexOf("/");
         if(ii != -1) {
           filename = source.substring(ii+1);
         }
         else {
           filename = source;
         }
       }
       else {
         protocol = toUrl.getProtocol();
         host = toUrl.getHost();
         int ii = target.lastIndexOf("/");
         if(ii != -1) {
           filename = target.substring(ii+1);
         }
         else {
           filename = target;
         }
       }
       if(protocol.equals("gsiftp")) {
         tu.setParallel(this.parallelism);
         try {
           if(_debug) {
             inputVec = new Vector();
             _logger.debug("Getting proxy");
             inputVec.addElement("Getting proxy");
             util.printEventLog(_theLogger,
		"FileTransferTest.doFirstActiveTransfer", 
	        inputVec,silent,useLog);
           }
           this.credential = _parent.getParentIntf().checkProxy();
           if(_debug) {
             inputVec = new Vector();
             _logger.debug("Setting credentials");
             inputVec.addElement("Setting credentials");
             util.printEventLog(_theLogger,
		"FileTransferTest.doFirstActiveTransfer", 
	        inputVec,silent,useLog);
           }
           tu.setCredentials(this.credential);
         }catch(ProxyNotFoundException pnfe) {
           setSRMClientThreadDisable(false);
           fIntf.used(false);    
           JFrame frame = _parent.getParentIntf().getParentWindow();
           ShowException.showMessageDialog(frame, 
		     "Exception : " + pnfe.getMessage());
           throw pnfe;
         }
         tu.setTransferType(SRMTransferProtocol.GSIFTP);
       }
       else if(protocol.equals("ftp")) {
         tu.setTransferType(SRMTransferProtocol.FTP);
       }
       else if(protocol.equals("http")) {
         tu.setTransferType(SRMTransferProtocol.HTTP);
       }
       else if(protocol.equals("https")) {
         tu.setTransferType(SRMTransferProtocol.HTTPS);
       }
       tu.setSessionType(1); 
       tu.setSessionMode(1); 
       tu.setDCAU(dcau);
       long sSize = -1;
       if(this.mode == SRMTransferMode.GET) {
         //sSize = tu.getSourceFileSize();
         try {
           Long lSize = new Long(fIntf.getExpectedSize());
           sSize = lSize.longValue();
         }catch(NumberFormatException nfe) {
           sSize = -1;
         }
         if(sSize == -1) {
           sSize = (new Long(eSize)).longValue();
         }

         File targetDir = new File(fIntf.getTargetDir());
         if(checkDiskSpace) {
         try{
         SharedObjectLock sLock = new SharedObjectLock(1);
         _parent.getParentIntf().checkDiskSpaceFull(sLock,targetDir,sSize);
         sLock.get();
         boolean b = sLock.getIsCancel();
         if(b) {
           throw new DiskSpaceFullException("Disk space full");
         }
         }catch(DiskSpaceFullException dse) {
           setSRMClientThreadDisable(false);
           setCancelRequest(5,idx);
           JFrame frame = _parent.getParentIntf().getParentWindow();
           ShowException.showMessageDialog(frame, 
		"Exception : " + dse.getMessage());
           throw dse;
         }
         }
       }
       else if (this.mode == SRMTransferMode.PUT) {
         File f = new File(fromUrl.getPath());
         sSize = f.length();
         inputVec = new Vector();
         inputVec.addElement("FromURLPath="+fromUrl.getPath());
         inputVec.addElement("LocalFile="+f.toString());
         util.printEventLog(_theLogger,
		"doFirstActiveTransfer.ActualFileSize",inputVec,silent,useLog);
         if(sSize == 0) 
           sSize = (new Long(eSize)).longValue();
         if(sSize != -1) {
           fIntf.setActualFileSizeKnown(true);
           fIntf.setActualSize(""+sSize);
         }
       }
       else {
         sSize = tu.getSourceFileSize();
         if(sSize != -1) {
           fIntf.setActualFileSizeKnown(true);
           fIntf.setActualSize(""+sSize);
         }
       }


       /*
       if(lahfs) 
          displayName = " " + protocol + "://" + host;
       else 
          displayName = " " + protocol + "://" + host + ", " + filename;
       */

       fIntf.setHostInfo(protocol+"://"+host);

       if(!lahfs) {
          displayName = " " + filename;
       }

       updateListeners(1,idx,""+sSize);
       FileStatusGUI fgui = _parent.getProgressBar(i);
       WrappedFT wft = new WrappedFT
		(tu,fgui,fIntf,pmodel,displayName,sSize,i,idx,df,Mbps);
       _activeFiles.add(new File(toUrl.getPath()));
       _activeSRMFileTransfers.add(wft);
       if(bufferSize != 0) {
         tu.setBufferSize(bufferSize);
       }
       if(blockSize != 0) {
         tu.setBlockSize(blockSize);
       }
       tu.start();
       numFilesStarted++;

       if(target.startsWith("file:")) { //only monitors get requests
        long sTimeStamp = util.startTimeStamp();
        TimeOutCallBack2 timeOutCallBack2 = new TimeOutCallBack2(wft,sTimeStamp);
        timeOutCallBack2.setParams(target,this,_activeFiles, 
                _activeSRMFileTransfers, fgui, tu,fIntf,pmodel,displayName,
                sSize,i,idx,df,Mbps, connectionTimeOutAllowed,_theLogger,
		_debug,silent,useLog);
        timeOutCallBack2.start();
       }
      } catch(Exception e) {
         inputVec = new Vector();
         inputVec.addElement("Exception="+e.getMessage());
         util.printEventLog(_theLogger,
		"doFirstActiveTransfer.Exception",inputVec,silent,useLog);
         util.printEventLogException(_theLogger,
		"FileTransferTest.doFirstActiveTransfer",e);
         util.printMessage
		  ("\nSRM-CLIENT: Exception=" + e.getMessage() ,pIntf);
         if(!silent) {
           System.out.println("\nSRM-CLIENT: Exception " + e.getMessage());
           if(fIntf.getRetry() == 0) {
             e.printStackTrace();
           } 
         }
         //ShowException.logDebugMessage(_logger,e);
         try { 
	        MyGlobusURL toUrl = new MyGlobusURL (target,0);
            FileStatusGUI fgui = _parent.getProgressBar (i);
	        WrappedFT wft = new WrappedFT (tu, fgui, fIntf,
		      pmodel, displayName,0, i,idx,df,Mbps);
	        wft.setErrorOccured(true,e.getMessage());
            fIntf.setRetryTime(new Date());
            updateListeners(4,idx,""+0);
            updateListeners(11,idx,e.getMessage());
	        _activeFiles.add(new File(toUrl.getPath()));
	        _activeSRMFileTransfers.add(wft);
	      }catch(MalformedURLException me) {
             inputVec = new Vector();
             inputVec.addElement("Exception="+me.getMessage());
             util.printEventLog(_theLogger,
				"doFirstActiveTransfer.Exception",inputVec,silent,useLog);
             if(!silent) {
             System.out.println("\nSRM-CLIENT: Exception " + me.getMessage());
             //me.printStackTrace();
             }  
             //ShowException.logDebugMessage(_logger,me);
	            throw new SRMClientException(me.getMessage());
	     }catch(Exception ioe) {
             inputVec = new Vector();
             inputVec.addElement("Exception="+ioe.getMessage());
             util.printEventLog(_theLogger,
				"doFirstActiveTransfer.Exception",inputVec,silent,useLog);
             util.printMessage
		       ("\nSRM-CLIENT: Exception=" + ioe.getMessage() ,pIntf);
             if(!silent) {
             System.out.println("\nSRM-CLIENT: Exception " + ioe.getMessage());
             //ioe.printStackTrace();
             }
             //ShowException.logDebugMessage(_logger,ioe);
	     throw new SRMClientException(ioe.getMessage());
         }
      }
    }//end for
    nextPosition = i;
    inputVec = new Vector ();
    inputVec.addElement("Inside doFirstActiveTransfer(nextPosition)="+
                nextPosition);
    util.printEventLog(_theLogger,"FileTransferTest.doFirstActiveTransfer",
                inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateListeners
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void updateListeners(int status, int tableIndex, String value) {
  for(int i = 0; i < _listener.size(); i++) {
      FileEventListener fEListener = 
	(FileEventListener) _listener.elementAt(i);
      switch (status) {
        //case 0 : fEListener.filePending(tableIndex); break;
        //filePending is called directly from SRMFileTransfer
        case 1 :  fEListener.fileActive(tableIndex,value); break;
        case 2 :  fEListener.fileCompleted(tableIndex,value); break;
        case 3 :  fEListener.fileExists(tableIndex); break;
        case 4 :  fEListener.fileFailed(tableIndex); break;
	case 5 :  fEListener.requestCancelled(tableIndex); break;
	case 9 :  fEListener.setTimeTaken(tableIndex,value); break;
	case 11 : fEListener.setErrorMessage(tableIndex,value); break;
      }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRenew
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRenew (boolean b) {
  isRenew = b;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isNextTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isNextTransfer() throws Exception {
    return (nextPosition < totalFiles);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isNextTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void verifyProxy () throws SRMClientException, 
	ProxyNotFoundException, Exception {
   
  if(this.credential == null) {
     this.credential = _parent.getParentIntf().checkProxy();
  }

  int remainingTimeLeft = this.credential.getRemainingLifetime();
  if(remainingTimeLeft == 0) {
    throw new ProxyNotFoundException("Proxy expired, please renew");
  }
  if(remainingTimeLeft <= 1800) {
    inputVec = new Vector();
    inputVec.addElement("Proxy remaining time left " + remainingTimeLeft);
    util.printEventLog(_theLogger,"FileTransferTest.VerifyProxy",inputVec,silent,useLog);
    if(isRenew) {
       this.credential = _parent.getParentIntf().initProxy();
       inputVec = new Vector();
       inputVec.addElement("Proxy auto renewed");
       util.printEventLog(_theLogger,"FileTransferTest.VerifyProxy",inputVec,silent,useLog);
    }
    else {
      if(!warning) {
        JFrame frame = _parent.getParentIntf().getParentWindow();
        //_logger.debug(
  	     //"Your proxy has only " + remainingTimeLeft + " seconds " +
          //"please use Tools->init to renew your proxy.");
        inputVec = new Vector();
        inputVec.addElement("Your proxy has only " + 
				remainingTimeLeft + " seconds " +
                "please use Tools->init to renew your proxy.");
        util.printEventLog(_theLogger,"FileTransferTest.VerifyProxy",inputVec,silent,useLog);
        ShowException.showMessageDialog(frame, 
	  "Your proxy has only " + remainingTimeLeft + " seconds " +
                "please use Tools->init to renew your proxy.");
        warning = true;
      } 
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//srmFileFailure
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void srmFileFailure() {
  totalFiles = (totalFiles-(_retryAllowed))-1;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setPreviousStatusPanel
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void setPreviousStatusPanel(int status, WrappedFT wtu, 
		int tableIndex, String value) {
     if(status == 2) {
         FileIntf fIntf = wtu.getFileIntf();
         if(!fIntf.getCompleted()) {
          int numTimes = fIntf.getRetry(); 
          totalFiles = (totalFiles-(_retryAllowed-numTimes));
         }
     }
     if(status == 4) {
         updateListeners(status, tableIndex,value);
         FileIntf fIntf = wtu.getFileIntf();
         if(fIntf.getRetry() < _retryAllowed) {
            //mostly for permission denied case, we don't want to retry here
            //no such file or directory, we don't want to retry here
            int idx = value.indexOf("file_open failed");
            int idx2 = value.toLowerCase().indexOf("no such file");
            int idx3 = value.toLowerCase().indexOf("login incorrect");
            int idx4 = value.toLowerCase().indexOf("bad password");
            if(idx != -1 || idx2 != -1 || idx3 != -1 || idx4 != -1)  {
             //fIntf.setRetry(_retryAllowed);
             fIntf.setErrorMessage(value);
             inputVec = new Vector ();
             inputVec.addElement
               ("Retrying now again file " + 
		   fIntf.getLabel() + " in " + _retryTimeOut + 
	           " seconds for " + (fIntf.getRetry()+1) + " times.\n");
             util.printEventLog(_theLogger,
		"FileTransferTest.setPreviousStatusPanel", inputVec,silent,useLog);
             int numTimes = fIntf.getRetry(); 
             totalFiles = (totalFiles-(_retryAllowed-numTimes));
             fIntf.setFileFailedAfterAllowedRetries(true);
             updateListeners(status, tableIndex,"");
             return;
            }
            if(!silent) {
               System.out.println("\nSRM-CLIENT: Retrying now again file " + 
		   fIntf.getLabel() + " in " + _retryTimeOut + 
	           " seconds for " + (fIntf.getRetry()+1) + " times.\n");
             }
            inputVec = new Vector();
            inputVec.addElement("\nRetrying now again " + fIntf.getLabel() + 
		" in " + _retryTimeOut + " seconds for " + (fIntf.getRetry()+1)+
		" times.");
            util.printEventLog(_theLogger,
		"FileTransferTest.setPreviousStatusPanel", inputVec,silent,useLog);
            fIntf.retry();
            fIntf.setNew(false);
            fIntf.setRetryTime(new Date());
            fIntf.setErrorMessage("");
	        _parent.addNextFileInfo(wtu.getFileIntf());
            addNextFileInfo(wtu.getFileIntf());
            inputVec = new Vector(); 
            inputVec.addElement("FileInfo="+_fInfo);
            util.printEventLog(_theLogger,
		"FileTransferTest.setPreviousStatusPanel", inputVec,silent,useLog);
            if(!silent) {
             if(_debug) {
              System.out.println(_fInfo);
             } 
            }
         }
         else {
           inputVec = new Vector ();
           inputVec.addElement
		      ("Tried for allowed retry times : "+ _retryAllowed);
           util.printEventLog(_theLogger,
		"FileTransferTest.setPreviousStatusPanel", inputVec,silent,useLog);
           fIntf.setFileFailedAfterAllowedRetries(true);
           updateListeners(status, tableIndex,value);
         }
     }
     else {
        updateListeners(status, tableIndex,value);
     }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCancelRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCancelRequest(int status, int tableIndex) {
     updateListeners(status, tableIndex+1,""+0);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTimeTaken
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTimeTaken(String totalTime , int tableIndex) {
    updateListeners(9,tableIndex,totalTime);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setErrorMessage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setErrorMessage(String message, int tableIndex) {
    updateListeners(11,tableIndex,message);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// addNextFileInfo
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void addNextFileInfo(FileIntf fIntf) {
    inputVec = new Vector ();
    inputVec.addElement("Adding next file info="+fIntf.getLabel());
    inputVec.addElement(" _fInfo.size()="+_fInfo.size());
    _fInfo.addElement(fIntf);
    util.printEventLog(_theLogger,"FileTransferTest.addNextFileInfo",
			inputVec,silent,useLog);
    index.addElement(new Integer(fIntf.getLabel()));
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//findAndSwitch
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean findAndSwitch(int nextPos) {
   inputVec = new Vector ();
   inputVec.addElement("NextPosition="+nextPos);
   inputVec.addElement("FileInfo.size="+_fInfo.size());
   inputVec.addElement("index.size="+index.size());
   //System.out.println(">>>FindAndSwitch=");
   //System.out.println("NextPosition="+nextPos);
   //System.out.println("FInfo.size="+_fInfo.size());
   //System.out.println("index.size="+index.size());
   util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
   boolean b = false;
   if(_fInfo.size() > nextPos && index.size() > nextPos) {
      FileIntf fIntf = (FileIntf) _fInfo.elementAt(nextPos);
      inputVec = new Vector ();
      inputVec.addElement("IsRetry="+fIntf.isRetry());
      util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
      if(fIntf.isRetry()) { 
        if(fIntf.isRetryOk(_retryTimeOut)) {
           inputVec = new Vector ();
           inputVec.addElement("IsRetryOk");
           util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
           return true;
        }  
        else {
          inputVec = new Vector ();
          inputVec.addElement("IsRetryOk inside else");
          util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
	      b = findAndSwitch(nextPos+1);
          inputVec = new Vector ();
          inputVec.addElement("ReturnValue from findAndSwitch(nexPos+1)");
          util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
           
          if(b) {
            FileIntf ff = (FileIntf)_fInfo.elementAt(nextPos);
            inputVec = new Vector ();
            inputVec.addElement("NextPosition=" + nextPosition);
            //System.out.println(">>>>>>>>>NextPosition="+nextPosition);
            //System.out.println(">>>>>>>>>nextPos="+nextPos);
            util.printEventLog(_theLogger,"FileTransferTest.FindAndSwitch",inputVec,silent,useLog);
            if(nextPos > nextPosition)  {
              if(!ff.isRetry() || ff.getCompleted()) {
                FileIntf fff = (FileIntf)_fInfo.elementAt(nextPos-1);
                _fInfo.setElementAt(fff,nextPos);
                _fInfo.setElementAt(ff,nextPos-1);
                Object o1 = index.elementAt(nextPos-1);
                Object o2 = index.elementAt(nextPos);
                index.setElementAt(o1,nextPos);
                index.setElementAt(o2,nextPos-1);
              }
              if(ff.getCompleted()) nextPosition++;
            }
          }
        }
      }
      else {
        if(fIntf.getCompleted()) return false;

        /*
        FileIntf ff = (FileIntf)_fInfo.elementAt(nextPos-1);
        if(!ff.getCompleted()) {
          _fInfo.setElementAt(ff,nextPos);
          _fInfo.setElementAt(fIntf,nextPos-1);
          Object o1 = index.elementAt(nextPos-1);
          Object o2 = index.elementAt(nextPos);
          index.setElementAt(o1,nextPos);
          index.setElementAt(o2,nextPos-1);
        }
        */
        return true;
      }
   }
   //if(_debug) 
     //System.out.print(".");
   return b;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isNextTransferAvailable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isNextTransferAvailable() {
   boolean b = findAndSwitch(nextPosition);
   return b;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doNextActiveTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized WrappedFT doNextActiveTransfer() throws
 SRMClientException, ProxyNotFoundException, DiskSpaceFullException {

 inputVec = new Vector ();
 inputVec.addElement("Inside doNextActiveTransfer");
 inputVec.addElement("NextPosition=" + nextPosition);
 util.printEventLog(_theLogger,"FileTransferTest.doNextActiveTransfer",
                inputVec,silent,useLog);
 
 FileIntf fIntf = (FileIntf) _fInfo.elementAt(nextPosition);
 fIntf.setNew(true);
 Integer iii = (Integer) index.elementAt(nextPosition);
 int idx = ((Integer)index.elementAt(nextPosition)).intValue();
 String source = fIntf.getSURL();
 String target = fIntf.getTURL();
 String eSize  = fIntf.getExpectedSize();
 boolean lahfs = fIntf.getLahfs();
 try {
  if(this.mode == SRMTransferMode.PUT) {
    source = "file:///"+SRMClientN.parseLocalSourceFileForPath(source);
  }
  if(this.mode == SRMTransferMode.GET) {
    target = "file:///"+SRMClientN.parseLocalSourceFileForPath(target);
  }
 }catch(Exception ee) {
    throw new SRMClientException(ee.getMessage());
 }
 fIntf.used(true);
 MyISRMFileTransfer tu; 
 util.printMessage
		("\nSRM-CLIENT: " + new Date() + " start file transfer",null,silent);
 util.printMessage
		("\nSRM-CLIENT: " + new Date() + " start file transfer.",pIntf);
 if(silent) {
   System.out.println(_fInfo);
 }
 util.printMessage("SRM-CLIENT:Source="+source,null,silent);
 util.printMessage("SRM-CLIENT:Target="+target,null,silent);
 if(lahfs) {
   tu = new LahfsFileTransfer(source,target);
 }
 else if(guc) {
   tu = new GUCScriptFileTransfer(source,target,_debug,silent,gucScriptPath,gucEventLogPath);
 }
 else {
   tu = new MySRMFileTransfer(source, target);
 } 
 tu.setTransferMode(this.mode);
    //System.out.println("Setting logger="+ silent);
 tu.setLogger(_logger,_theLogger,silent,useLog,_debug);
 if(dcau) {
   tu.setDCAU(dcau);
 }
 //else no need to set, in SRMFileTransfer dcau is false by default
 WrappedFT wft = null;
 String protocol = "";
 String host = "";
 String filename = ""; 
 String displayName = ""; 

 try {
    MyGlobusURL fromUrl = new MyGlobusURL(source,0);
    MyGlobusURL toUrl = new MyGlobusURL(target,0);
    if(this.mode == SRMTransferMode.GET) {
      protocol = fromUrl.getProtocol();
      host = fromUrl.getHost();
      int ii = source.lastIndexOf("/");
      if(ii != -1) {
         filename = source.substring(ii+1);
      }
      else {
         filename = source;
      }
    }
    else  {
      protocol = toUrl.getProtocol();
      host = toUrl.getHost();
      int ii = target.lastIndexOf("/");
      if(ii != -1) {
         filename = target.substring(ii+1);
      }
      else {
         filename = target;
      }
    }
    if(protocol.equals("gsiftp")) {
       tu.setParallel(this.parallelism);
       try { 
         verifyProxy();    
         tu.setCredentials(this.credential);
       }catch(ProxyNotFoundException pnfe) {
         setSRMClientThreadDisable(false);
         fIntf.used(false);    
         JFrame frame = _parent.getParentIntf().getParentWindow();
         ShowException.showMessageDialog(frame, 
			"Exception : " +   pnfe.getMessage());
         throw pnfe;
       }
       tu.setTransferType(SRMTransferProtocol.GSIFTP);
    }
    else if(protocol.equals("ftp")) {
       tu.setTransferType(SRMTransferProtocol.FTP);
    }
    else if(protocol.equals("http")) {
       tu.setTransferType(SRMTransferProtocol.HTTP);
    }
    tu.setSessionType(1); 
    tu.setSessionMode(1); 
    tu.setDCAU(dcau);
    long sSize = -1;
    if(this.mode == SRMTransferMode.GET) {
      //sSize = tu.getSourceFileSize();
      try {
        Long lSize = new Long(fIntf.getExpectedSize());
        sSize = lSize.longValue();
      }catch(NumberFormatException nfe) {
        sSize = -1;
      }
      if(eSize == null) eSize = "100000000";
      if(sSize == -1) {
        sSize = (new Long(eSize)).longValue();
      }

      File targetDir = new File(fIntf.getTargetDir());
     if(checkDiskSpace) {
      try {
       SharedObjectLock sLock = new SharedObjectLock(1);
       _parent.getParentIntf().checkDiskSpaceFull(sLock,targetDir,sSize);
       sLock.get();
       boolean b = sLock.getIsCancel();
       if(b) {
           throw new DiskSpaceFullException("Disk space full");
       }
      }catch(DiskSpaceFullException dse) {
         setSRMClientThreadDisable(false);
         setCancelRequest(5,idx);
         JFrame frame = _parent.getParentIntf().getParentWindow();
         ShowException.showMessageDialog(frame, 
		"Exception : " + dse.getMessage());
         throw dse;
      }
      }
    }
    else if (this.mode == SRMTransferMode.PUT) {
      File f = new File(fromUrl.getPath());
      sSize = f.length();
      inputVec = new Vector();
      inputVec.addElement("FromURLPath="+fromUrl.getPath());
      inputVec.addElement("LocalFile="+f.toString());
      util.printEventLog(_theLogger,
	"doNextActiveTransfer.ActualFileSize",inputVec,silent,useLog);
      if(eSize == null) eSize = "100000000";
      if(sSize != -1) {
         fIntf.setActualFileSizeKnown(true);
         fIntf.setActualSize(""+sSize);
      }
    }
    else {
      sSize = tu.getSourceFileSize();
      if(sSize != -1) {
         fIntf.setActualFileSizeKnown(true);
         fIntf.setActualSize(""+sSize);
      }
    }

   /*
    if(lahfs) 
       displayName = " " + protocol + "://" + host;
    else 
       displayName = " " + protocol + "://" + host + ", " + filename;
   */

   fIntf.setHostInfo(protocol+"://"+host);

   if(!lahfs) {
       displayName = " " + filename;
   }
        
    FileStatusGUI fgui = _parent.getProgressBar(nextPosition);
    if((nextPosition+(concurrency)) < _fInfo.size()) {
      _parent.createNextProgressBar(nextPosition+(concurrency));
    } 
    updateListeners(1,idx,""+sSize);
    if(bufferSize != 0) {
      tu.setBufferSize(bufferSize);
    }
    if(blockSize != 0) {
      tu.setBlockSize(blockSize);
    }
    wft = new WrappedFT
	(tu, fgui,fIntf,pmodel,displayName,sSize,nextPosition,idx,df,Mbps);

     if(target.startsWith("file:")) { //only monitors get requests
       long sTimeStamp = util.startTimeStamp();
       TimeOutCallBack2 timeOutCallBack2 = new TimeOutCallBack2(wft,sTimeStamp);
       timeOutCallBack2.setParams(target,this,
		_activeFiles, _activeSRMFileTransfers,fgui,tu,fIntf,pmodel,
		displayName, sSize,nextPosition,idx,df,Mbps,
	        connectionTimeOutAllowed,_theLogger,_debug,silent,useLog);
       timeOutCallBack2.start();
     }
 }catch(Exception e) {
      inputVec = new Vector();
      inputVec.addElement("Exception="+e.getMessage());
      util.printEventLog(_theLogger,"FileTransferTest.doNextActiveTransfer",
				inputVec,silent,useLog);
      util.printEventLogException(_theLogger,
		"FileTransferTest.doNextActiveTransfer",e);
      util.printMessage
		  ("\nSRM-CLIENT: Exception=" + e.getMessage() ,pIntf);
      if(!silent) {
        System.out.println("\nSRM-CLIENT: Exception " + e.getMessage());
        if(fIntf.getRetry() == 0) {
          e.printStackTrace();
        }
      }
      FileStatusGUI fgui = _parent.getProgressBar (nextPosition);
      fIntf.setRetryTime(new Date());
      wft = new WrappedFT 
	  (tu, fgui, fIntf,pmodel, displayName,0, nextPosition,idx,df,Mbps);
      wft.setErrorOccured(true,e.getMessage());
      updateListeners(4,idx,""+0);
      updateListeners(11,idx,e.getMessage());
      if((nextPosition+(concurrency)) < _fInfo.size()) {
        _parent.createNextProgressBar(nextPosition+(concurrency));
      } 
 }
 nextPosition++;
 inputVec = new Vector ();
 inputVec.addElement("Inside doNextActiveTransfer");
 inputVec.addElement("NextPosition(doNext)=" + nextPosition);
 util.printEventLog(_theLogger,"FileTransferTest.doNextActiveTransfer",
                inputVec,silent,useLog);
 tu.start();
 numFilesStarted++;
 return wft;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getNumFilesStarted
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getNumFilesStarted() {
  return numFilesStarted;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getNumFilesCompleted
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getNumFilesCompleted() {
  return numFilesCompleted;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//incrementNumFilesCompleted
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void incrementNumFilesCompleted () {
  this.numFilesCompleted++;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getCurrentTarget
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public File getCurrentTarget() throws SRMClientException {
  int idx = ((Integer) index.elementAt(nextPosition - 1)).intValue();
  FileIntf fIntf = (FileIntf) _fInfo.elementAt(nextPosition-1);
  String target = fIntf.getTURL();
  try {
    MyGlobusURL toUrl = new MyGlobusURL(target,0);
    _parent.setCurrentTargetIndex(idx);
    return new File(toUrl.getPath());
  }catch(MalformedURLException me) {
    inputVec = new Vector();
    inputVec.addElement("Exception="+me.getMessage());
    util.printEventLog(_theLogger,"FileTransferTest.getCurrentTarget",inputVec,silent,useLog);
    util.printEventLogException(_theLogger,"FileTransferTest.getCurrentTarget",me);
    if(!silent) {
    System.out.println("\nSRM-CLIENT: Exception " + me.getMessage());
    me.printStackTrace();
    }
    throw new SRMClientException (me.getMessage());
  }catch(Exception e) {
    inputVec = new Vector();
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"FileTransferTest.getCurrentTarget",inputVec,silent,useLog);
    util.printEventLogException(_theLogger,"FileTransferTest.getCurrentTarget",e);
    if(!silent) {
    System.out.println("\nSRM-CLIENT: Exception " + e.getMessage());
    e.printStackTrace();
    }
    throw new SRMClientException (e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isProxyValid
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isProxyValid ()  throws GSSException {
   if(this.credential != null) {
     return (this.credential.getRemainingLifetime() != 0);
   }
   return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferMonitorThreadDisable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public void setTransferMonitorThreadDisable() {
  _transferRateMonitorThread.setDisable(true);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSRMClientThreadDisable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSRMClientThreadDisable(boolean b) {
   _srmClientThread = null;
   if(pmodel != null) {
     pmodel.setLock(false);
   }
   if(!_disableCalledOnce) {
     _disableCalledOnce = true;
     inputVec = new Vector ();
     _logger.debug("\n>>>> I am here setSRMClientThreadDisable");
     util.printEventLog(_theLogger,"FileTransferTest.setSRMClientThreadDisable", inputVec,silent,useLog);
     _parent.getParentIntf().enableTransferButton(true,b);
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isCancelPressed
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isCancelPressed() {
   return _parent.getParentIntf().isRequestCancel();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isClientThreadDisable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isClientThreadDisabled() {
  return (_srmClientThread == null);
}
  
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getAllDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getAllDone () {
  int size = _activeSRMFileTransfers.size();
  int count =0;
  inputVec = new Vector();
  inputVec.addElement("ActiveSRMFileTransfers.size="+size);
  util.printEventLog(_theLogger,"FileTransferTest.getAllDone",
	inputVec,silent,useLog);
  for(int i = 0; i < size; i++) {
    WrappedFT wft = 
	(WrappedFT) _activeSRMFileTransfers.elementAt(i);
    boolean b = wft.transferDone();
    String message = wft.getStatus();
    boolean error = wft.getErrorOccured ();
    inputVec = new Vector();
    inputVec.addElement("TransferDone="+b);
    inputVec.addElement("Message="+message);
    inputVec.addElement("Error="+error);
    util.printEventLog(_theLogger,"FileTransferTest.getAllDone",
		inputVec,silent,useLog);
    if(b || message != null || error) {
      count ++;
    }
    inputVec = new Vector();
    inputVec.addElement("Count="+count);
    inputVec.addElement("Size="+size);
    inputVec.addElement("NextPosition="+nextPosition);
    inputVec.addElement("TotalFiles="+totalFiles);
    util.printEventLog(_theLogger,"FileTransferTest.getAllDone",
	inputVec,silent,useLog);
  }
  if(silent) {
    inputVec = new Vector();
    inputVec.addElement("Count="+count);
    inputVec.addElement("Size="+size);
    inputVec.addElement("NextPosition="+nextPosition);
    inputVec.addElement("TotalFiles="+totalFiles);
    util.printEventLog(_theLogger,"FileTransferTest.getAllDone",
		inputVec,silent,useLog);
  }
  if(nextPosition == totalFiles && count == size) {
    return true;
  }
  return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getActiveFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Vector getActiveFiles () {
    return _activeFiles;
} 

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getDebug
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getDebug() {
   return _debug;
}
   
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getActiveMyISRMFileTransfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Vector getActiveMyISRMFileTransfers () {
    return _activeSRMFileTransfers;
}

}
