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

import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.exception.*;

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
    private TransferRateMonitorThread _transferRateMonitorThread = null;
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

public static gov.lbl.adapt.adt.TTxfHandler _adtTxf = new gov.lbl.adapt.adt.TTxfHandler();

    class TTxfForADT implements gov.lbl.adapt.adt.ITxf {
	String _source = null;
	String _target = null;
	MyISRMFileTransfer _tu = null;

	public TTxfForADT(String source, String target, MyISRMFileTransfer tu) {
	    _source = source;
	    _target = target;
	    _tu = tu;
	}
	
	public String getSrc() {return _source;}
	public String getTgt() {return _target;}
	public String getStatus() {return _tu.getStatus();}
	public void setParallel(int p) { _tu.setParallel(p);}
	public int getParallel() {
	    return gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism;
	}
	public void logPTMMsg(String ptmMsg, String comment) {
	    gov.lbl.adapt.srm.client.main.SRMClientN.logMsg(ptmMsg, comment, "ptmMsg");
	}

	public boolean transferDone() {return _tu.transferDone();}
	public void transferSync() { _tu.transferSync();}
    }
    public gov.lbl.adapt.adt.ITxf createAdtTxf(String source, String target, MyISRMFileTransfer tu) {
	return new TTxfForADT(source, target, tu);
    }

    /*
    public  void logMsg(String msg, String logHeader, String clientHeader) {
	if (clientHeader != null) {
	    util.printMessage(clientHeader + msg, null,silent); // note the original code didnt specify logger here
	    util.printMessage(clientHeader + msg, pIntf);
	}
	if (logHeader != null) {
	    inputVec.clear();
	    inputVec.addElement(msg);
	    util.printEventLog(_theLogger, logHeader, inputVec,silent,useLog);
	    inputVec.clear();
	}
    }

    public  void logMsg(String msg[], String logHeader, String clientHeader) {	
	inputVec.clear();
	for (int i=0; i<msg.length; i++) {
	    if (clientHeader != null) {
		util.printMessage(clientHeader+msg[i], null,silent); // note the original code didnt specify logger here
		util.printMessage(clientHeader+msg[i], pIntf);
	    }
	    inputVec.addElement(msg[i]);
	}
	if (logHeader != null) {
	    util.printEventLog(_theLogger, logHeader, inputVec,silent,useLog);
	}
	inputVec.clear();
    }
    */
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
    _srmClientThreadPool = new SRMClientThreadPool(1, this,silent);
    _srmClientThread = new SRMClientThreadMain(_srmClientThreadPool, this, silent);
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
    
    util.printEventLog(_theLogger,"FileTransferTest.constructor", inputVec,silent,useLog);		     
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

private String markSURL(SRMTransferMode localModeIndicator, String surl) throws SRMClientException    
{
    try {
	if(this.mode == localModeIndicator) {
	    return "file:///"+SRMClientN.parseLocalSourceFileForPath(surl);
	}
	return surl;
    } catch(Exception ee) {
	throw new SRMClientException(ee.getMessage());
    }
}

private MyISRMFileTransfer getTransferObj(String source, String target, boolean lahfs) 
{
    MyISRMFileTransfer tu = null;
    if(lahfs) {
	tu = new LahfsFileTransfer(source,target);
    } else if(guc) {
	tu = new GUCScriptFileTransfer (source,target,_debug,silent,gucScriptPath,gucEventLogPath);		
    } else {
	tu = new MySRMFileTransfer(source, target);
	//tu = _adtTxf.createJob(source, target);
    }
    tu.setLogger(_logger,_theLogger,silent,useLog,_debug);
    tu.setTransferMode(this.mode);
    
    if(dcau) {
	tu.setDCAU(dcau);
    }
    return tu;
}

private void setTxfObjProperty(String protocol, FileIntf fIntf, MyISRMFileTransfer tu, String tag) 
    throws ProxyNotFoundException
{
    if(protocol.equals("gsiftp")) {
	tu.setParallel(this.parallelism);
	try {
	    if(_debug) {
		SRMClientN.logMsg("Getting proxy", tag, null);
		_logger.debug("Getting proxy");
		    }
	    this.credential = _parent.getParentIntf().checkProxy();
	    if(_debug) {
		SRMClientN.logMsg("Setting credentials", tag, null);
		_logger.debug("Setting credentials");
	    }
	    tu.setCredentials(this.credential);
	}catch(ProxyNotFoundException pnfe) {
	    setSRMClientThreadDisable(false);
	    fIntf.used(false);    
	    JFrame frame = _parent.getParentIntf().getParentWindow();
	    ShowException.showMessageDialog(frame, "Exception : " + pnfe.getMessage());						    
	    throw pnfe;
	}
	tu.setTransferType(SRMTransferProtocol.GSIFTP);	    
    } else if(protocol.equals("ftp")) {
	tu.setTransferType(SRMTransferProtocol.FTP);      
    } else if(protocol.equals("http")) {
	tu.setTransferType(SRMTransferProtocol.HTTP);	    
    } else if(protocol.equals("https")) {
	tu.setTransferType(SRMTransferProtocol.HTTPS);
    }

    tu.setSessionType(1); 
    tu.setSessionMode(1); 
    tu.setDCAU(dcau);
}

private long getExpectedSize(FileIntf fIntf) {
    long sSize = -1;
    try {
	Long lSize = new Long(fIntf.getExpectedSize());
	sSize = lSize.longValue();
    }catch(NumberFormatException nfe) {
	sSize = -1;
    }
    /*if(sSize == -1) {
	sSize = (new Long(eSize)).longValue();
    }
    */ // this makes no sense as eSize is defined as fIntf.getExpectedSize()
       // so I comment this out - Junmin
    return sSize;
}

private String getProtocol(MyGlobusURL fromUrl, MyGlobusURL toUrl) {
    if(this.mode == SRMTransferMode.GET) {
	return fromUrl.getProtocol();
    } else {
	return toUrl.getProtocol();
    }
}

private String getHost(MyGlobusURL fromUrl, MyGlobusURL toUrl) {
    if(this.mode == SRMTransferMode.GET) {
	return fromUrl.getHost();
    } else {
	return toUrl.getHost();
    }
}

private String getFileName(String source, String target) {
    String path = source;
    if(this.mode != SRMTransferMode.GET) {
	path = target;
    }
    int ii = path.lastIndexOf("/");
    if(ii != -1) {
	return  path.substring(ii+1);		
    } else {
	return  path;
    }
}

private void checkDiskSpace(FileIntf fIntf, int idx) throws DiskSpaceFullException 
{
    long sSize = getExpectedSize(fIntf);
    File targetDir = new File(fIntf.getTargetDir());

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
	ShowException.showMessageDialog(frame, "Exception : " + dse.getMessage());
	throw dse;
    }

}	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doFirstActiveTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public void adtTransfer()  throws SRMClientException, ProxyNotFoundException, DiskSpaceFullException {
    String tag = "FileTransferTest.adtTxf "+Thread.currentThread();
    SRMClientN.logMsg("start _fInfo.size="+_fInfo.size(), tag, null);
    int i=0;

    long totalSize = 0;
    for (i=0; i<_fInfo.size(); i++) {
	int idx = ((Integer)index.elementAt(i)).intValue();
	FileIntf fIntf = (FileIntf) _fInfo.elementAt(i);
	fIntf.used(true); 
	String source = markSURL(SRMTransferMode.PUT, fIntf.getSURL());
	String target = markSURL(SRMTransferMode.GET, fIntf.getTURL());
	//String eSize = fIntf.getExpectedSize();
	boolean lahfs = fIntf.getLahfs();

	SRMClientN.logMsg("src="+source+" tgt="+target, tag, "SRM-CLIENT:");
	util.printMessage("SRM-CLIENT:Source="+source,null,silent);
	util.printMessage("SRM-CLIENT:Target="+target,null,silent);

	if (i == _fInfo.size()-1) {
	    int initC = 0;
	    if (System.getProperty("adtInitConcurrency") != null) {
		initC = Integer.parseInt(System.getProperty("adtInitConcurrency")); // init _gStart;
	    }

	    int stepC = 0;
	    if (System.getProperty("adtIncrementConcurrency") != null) {
		stepC = Integer.parseInt(System.getProperty("adtIncrementConcurrency")); // init _gStep;
	    }

	    System.out.println("initC = "+initC+" step = "+stepC);
	    _adtTxf.setPTM(gov.lbl.adapt.srm.client.main.SRMClientN.getPTM());
	    _adtTxf.setATM(gov.lbl.adapt.srm.client.main.SRMClientN._staticClient._atm);
	    
	    _adtTxf.init(source, target, totalSize, SRMClientN._staticClient.concurrency, SRMClientN._staticClient.parallelism,  _fInfo.size(), initC, stepC);
	    /*
	    //_adtTxf.init(concurrency, concurrency, source, target);
	    _adtTxf.init(source, target);

	    int maxStreams = 5 * SRMClientN._staticClient.parallelism; // let default be 5 concurrent txfs
	    if (SRMClientN._staticClient._atm != null) {
		maxStreams = SRMClientN._staticClient._atm.getConcurrencySuggestion(source,target, totalSize, _fInfo.size());
	    }
	    int initStreams = gov.lbl.adapt.adt.TManagerADT._gStart* gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism; 

	    //while ((maxStreams <= 0) || (maxStreams < initStreams)) {
	    while (true) {
		if (SRMClientN._staticClient._atm != null) {
		    SRMClientN._staticClient._atm.getCurrSnapshot();
		}
		if (maxStreams >= initStreams) {
		    break;
		} else if (maxStreams <= 0) {
		    if (SRMClientN._staticClient._atm != null) {
			maxStreams = SRMClientN._staticClient._atm.getConcurrencySuggestion(source,target, totalSize, _fInfo.size());
		    }
		} else {
		    //maxStreams = SRMClientN._staticClient._atm.getConcurrencySuggestion(null, null, -1, -1);
		    int correspondingConcurrency = maxStreams/gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism;
		    if (correspondingConcurrency > 0) {
			gov.lbl.adapt.adt.TManagerADT._gStart = correspondingConcurrency;
			SRMClientN.logMsg("modified starting concurrency "+gov.lbl.adapt.adt.TManagerADT._gStart, tag, "SRM-CLIENT:");
			break;
		    }
		}
		SRMClientN.logMsg("hibernating as suggested "+maxStreams+" "+initStreams, tag, "SRM-CLIENT:");
		gov.lbl.adapt.srm.util.TSRMUtil.sleep(5000);
	    }
	    _adtTxf.setPTM(gov.lbl.adapt.srm.client.main.SRMClientN.getPTM());
	    _adtTxf.setATM(gov.lbl.adapt.srm.client.main.SRMClientN._staticClient._atm);
	    //_adtTxf.setAD(new gov.lbl.adapt.adt.TManagerADT(maxStreams, gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism));
	    _adtTxf.setAD(maxStreams, gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism);
	    //_adtTxf.setAD(new gov.lbl.adapt.adt.TManagerADT(concurrency*gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism));		
	    */
	    Thread txfThread = new Thread(_adtTxf);      
	    txfThread.start();	    
	}

	MyISRMFileTransfer tu = getTransferObj(source, target, lahfs);

	//else no need to set, in SRMFileTransfer dcau is false by default
	String displayName = "";

	try { 
	    MyGlobusURL fromUrl = new MyGlobusURL(source,0);
	    MyGlobusURL toUrl = new MyGlobusURL(target,0);

	    String[] msgs = {"FromURLPath="+fromUrl.getPath(), "ToURLPath="+toUrl.getPath()};
	    SRMClientN.logMsg(msgs, tag, null);

	    String protocol = getProtocol(fromUrl, toUrl);
	    String host = getProtocol(fromUrl, toUrl);
	    String filename= getFileName(source, target);

	    setTxfObjProperty(protocol, fIntf, tu, tag);
	    
	    long sSize = -1;
	    if(this.mode == SRMTransferMode.GET) {
		//sSize = tu.getSourceFileSize();
		sSize = getExpectedSize(fIntf);

		if(checkDiskSpace) {
		    checkDiskSpace(fIntf, idx);
		}	    
	    } else if (this.mode == SRMTransferMode.PUT) {
		File f = new File(fromUrl.getPath());
		SRMClientN.logMsg("FromURLPath="+fromUrl.getPath()+"LocalFile="+f.toString(), tag, null);

		sSize = f.length();
		if(sSize != -1) {
		    fIntf.setActualFileSizeKnown(true);
		    fIntf.setActualSize(""+sSize);
		}	    
		if(sSize == 0) 
		    //sSize = (new Long(eSize)).longValue();		
		    sSize = getExpectedSize(fIntf);
	    } else {
		sSize = tu.getSourceFileSize();
		if(sSize != -1) {
		    fIntf.setActualFileSizeKnown(true);
		    fIntf.setActualSize(""+sSize);
		}
	    }
	    	    
	    totalSize += sSize;
	    fIntf.setHostInfo(protocol+"://"+host);
	    
	    if(!lahfs) {
		displayName = " " + filename;
	    }
	    
	    updateListeners(1,idx,""+sSize);
	    FileStatusGUI fgui = _parent.getProgressBar(i);
	    WrappedFT wft = new WrappedFT(tu,fgui,fIntf,pmodel,displayName,sSize,i,idx,df,Mbps);
		
	    _activeFiles.add(new File(toUrl.getPath()));
	    _activeSRMFileTransfers.add(wft);
	    if(bufferSize != 0) {
		tu.setBufferSize(bufferSize);
	    }
	    if(blockSize != 0) {
		tu.setBlockSize(blockSize);
	    }
	    //tu.start();
	    //_adtTxf.add(source, target);	   
	    _adtTxf.add(new TTxfForADT(source, target,tu));
	    
	    //_adtTxf.runMe(tu);
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
	    SRMClientN.logMsg("Exception="+e.getMessage(), tag, "\nSRM-CLIENT");
	    util.printEventLogException(_theLogger, tag, e);					
		
	    if(!silent) {
		System.out.println("\nSRM-CLIENT: Exception " + e.getMessage());
		if(fIntf.getRetry() == 0) {
		    e.printStackTrace();
		} 
	    }

	    try { 
	        MyGlobusURL toUrl = new MyGlobusURL (target,0);
		FileStatusGUI fgui = _parent.getProgressBar (i);
	        WrappedFT wft = new WrappedFT (tu, fgui, fIntf, pmodel, displayName,0, i,idx,df,Mbps);
					       
	        wft.setErrorOccured(true,e.getMessage());
		fIntf.setRetryTime(new Date());
		updateListeners(4,idx,""+0);
		updateListeners(11,idx,e.getMessage());
	        _activeFiles.add(new File(toUrl.getPath()));
	        _activeSRMFileTransfers.add(wft);
	    }catch(MalformedURLException me) {
		SRMClientN.logMsg("Exception="+me.getMessage(), tag, null);
		
		if(!silent) {
		    System.out.println("\nSRM-CLIENT: Exception " + me.getMessage());
		    //me.printStackTrace();
		}  
		throw new SRMClientException(me.getMessage());
	    }catch(Exception ioe) {
		SRMClientN.logMsg("Exception="+ioe.getMessage(), tag, "\nSRM-CLIENT:");
		    
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
    SRMClientN.logMsg("Inside doFirstActiveTransfer(nextPosition)="+nextPosition, tag, null);
}

    /*

*/

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateListeners
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void updateListeners(int status, int tableIndex, String value) {
  for(int i = 0; i < _listener.size(); i++) {
      FileEventListener fEListener = (FileEventListener) _listener.elementAt(i);
	  
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
    String[] msg = {"Adding next file info:"+fIntf.getLabel(), " _fInfo.size()="+_fInfo.size()};		    

    _fInfo.addElement(fIntf);

    SRMClientN.logMsg(msg, "FileTransferTest.addNextFileInfo", null);
    index.addElement(new Integer(fIntf.getLabel()));
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//findAndSwitch
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/*
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
        } else {
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
      } else {
        if(fIntf.getCompleted()) return false;
        return true;
      }
   }

   return b;
}
*/
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isNextTransferAvailable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/*
public synchronized boolean isNextTransferAvailable() {
   boolean b = findAndSwitch(nextPosition);
   return b;
}
*/
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doNextActiveTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
/*
public synchronized WrappedFT doNextActiveTransfer() throws
    SRMClientException, ProxyNotFoundException, DiskSpaceFullException 
{    

}
*/
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
    if (_transferRateMonitorThread == null) {
	return;
    }
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
       SRMClientN.logMsg("FileTransferTest.setSRMClientThreadDisable", "input="+b, null);
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
    String tag = "FileTransferTest::getAllDone()";
    int size = _activeSRMFileTransfers.size();
    int count =0;
    //SRMClientN.logMsg("ActiveTxfSize="+size, tag, null);

    for(int i = 0; i < size; i++) {
	WrappedFT wft = (WrappedFT) _activeSRMFileTransfers.elementAt(i);
	
	boolean b = wft.transferDone();
	String message = wft.getStatus();
	boolean error = wft.getErrorOccured ();
	
	//String[] msg = {"TransferDone="+b, "Message="+message, "Error="+error};
	//SRMClientN.logMsg(msg, tag, null);

	if (!b) {
	    return false;
	}
	/*
	if (b || message != null || error) {
	    count ++;
	}
	*/
    }
    /*
    String[] msg = {"Count="+count, "Size="+size, "NextPosition="+nextPosition, "Total="+totalFiles};
    SRMClientN.logMsg(msg, tag, null);	   
    
    if(nextPosition == totalFiles && count == size) {
	return true;
    }
    return false;
    */
    return true;
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
