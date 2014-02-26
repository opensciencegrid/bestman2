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

package gov.lbl.adapt.srm.client.wsdl;
 
import gov.lbl.srm.StorageResourceManager.*;
//import gov.lbl.adapt.srm.util.*;
import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.main.*;
import gov.lbl.adapt.srm.client.util.MyGlobusURL;
import org.globus.axis.util.Util;

import java.io.File;
import java.net.URL;
import java.util.Vector;
import java.util.HashMap;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.StringTokenizer;


import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
import org.apache.log4j.PropertyConfigurator;

import org.globus.axis.gsi.GSIConstants;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;

//import org.globus.ogsa.utils.GSIUtils;
import org.globus.util.ConfigUtil;
//import org.globus.ogsa.utils.GetOpts;
//import org.globus.ogsa.gui.ServiceContext;
import org.globus.gsi.GlobusCredential;

import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.auth.*;

import org.apache.axis.types.*;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.utils.Options;
import org.apache.axis.SimpleTargetedChain;
import org.globus.axis.transport.*;
import org.apache.axis.transport.http.HTTPSender;
import org.globus.axis.util.Util;

//import org.globus.wsrf.impl.security.authentication.Constants;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMPutClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMPutClient implements SRMWSDLIntf {
  private Stub _stub;
  private ISRM _srm;
  //private ServiceContext _context; 
  private SRMClientIntf _srmClientIntf; 
  private GSSCredential _credential;
  private boolean _debug;
  private Vector abortFilesVec = new Vector ();
  private Log logger;
  private String fileToken;
  private boolean doReserveSpace;
  private Vector fileInfo;
  private String uid="";
  private String userDesc="";
  private char tokenType;
  private int tokenLifetime;
  private long tokenSize;
  private long guarnSize;
  private boolean overwrite;
  private String statusToken;
  private boolean silent;
  private boolean useLog;
  private String storageInfo="";
  private String remoteTransferInfo="";
  private boolean callSetDone;
  private int tFiles;
  private boolean thirdparty;
  private int partytype;
  private TFileStorageType fileStorageType;
  private int totalRequestTime;
  private int pinTime; 
  private int fileLifeTime; 
  private TRetentionPolicyInfo retentionPolicyInfo;
  private int parallelism;
  private int bufferSize;
  private PrintIntf pIntf;
  private boolean dcau=true;
  private boolean directGsiFTP;
  private boolean submitOnly=false;
  private boolean remotePutCase=false;
  private static int statusMaxTimeAllowed=10800; //in seconds
  //private static int statusMaxTimeAllowed=-1; //in seconds
  private static int statusWaitTime=10; //changed for exponential polling
  private static int threshHoldValue = 600; 
  private boolean statusWaitTimeGiven=false;
  private String protocolsList="";
  private java.util.logging.Logger _theLogger;
  private Vector inputVec = new Vector();
  //private Date requestDate;
  private int proxyType;
  private TReturnStatus rStatus = null;
  private String resultStatus="";
  private String resultExplanation="";
  private TimeOutCallBack timeOutCallBack;
  private int connectionTimeOutAllowed=1800;
  private int setHTTPConnectionTimeOutAllowed=600;
  private String delegationNeeded="";
  private boolean targetHasGsiFTP=false;
  private String requestToken="";
  private String serverUrl="";
  private boolean noAbortFile;
  private boolean domkdir;
  private boolean recursive;
  private boolean noWaitOnRemotePut;
  private SoapCallThread soapCallThread;
  //private boolean firstTimeStatusCall=true;
  //private int spaceAvailableFiles;
  private int numRetry;
  private int retryTimeOut;
  private int firstTime=0;
  private boolean gateWayModeEnabled;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMPutClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMPutClient (SRMClientIntf srmClientIntf, 
		     String serverUrl, String uid, Vector fileInfo, 
		     String fileToken, String statusToken,
		     boolean doReserveSpace, 
		     boolean overwrite,
		     int tokenlifetime, long tokensize, long guarnSize,
		     GSSCredential credential, 
		     TFileStorageType fileStorageType, 
		     TRetentionPolicyInfo retentionPolicyInfo,
		     int totalRequestTime, int pinTime, int fileLifeTime,
		     boolean callSetDone, int tFiles, 
		     boolean thirdparty, 
		     int parallelism, int bufferSize, boolean dcau,
		     boolean directGsiFTP, 
		     int sMaxTimeAllowed, int statusWaitTime, 
		     String protocolsList,
		     boolean statusWaitTimeGiven, String storageInfo,
		     String remoteTransferInfo,
		     String userDesc, java.util.logging.Logger theLogger,
		     Log logger, PrintIntf pIntf, 
		     boolean debug, boolean silent, boolean useLog, boolean submitOnly,
		     int proxyType, boolean remotePutCase, 
		     int connectionTimeOutAllowed, int setHTTPConnectionTimeOutAllowed,
		     String delegationNeeded, boolean noAbortFile,boolean domkdir,
		     boolean recursive, int threshHold,boolean noWaitOnRemotePut,
		     int numRetry, int retryTimeOut, boolean checkPing) throws Exception 
    {
	//_context = new ServiceContext ();
    _srmClientIntf = srmClientIntf;
    _credential = credential;
    this.statusToken = statusToken;
    this.pIntf = pIntf;
    this.fileToken = fileToken;
    this.userDesc = userDesc;
    this._theLogger = theLogger;
    this.logger = logger;
    this.storageInfo = storageInfo;
    this.numRetry = numRetry;
    this.retryTimeOut = retryTimeOut;
    this.delegationNeeded = delegationNeeded;
    this.remoteTransferInfo = remoteTransferInfo;
    this.overwrite = overwrite;
    _debug = debug;
    this.silent = silent;
    this.threshHoldValue = threshHold;
    this.useLog = useLog;
    this.callSetDone = callSetDone;
    this.tFiles = tFiles;
    this.thirdparty = thirdparty;
    this.fileStorageType = fileStorageType;
    this.retentionPolicyInfo = retentionPolicyInfo;
    this.totalRequestTime = totalRequestTime;
    this.pinTime = pinTime;
    this.fileLifeTime = fileLifeTime;
    this.domkdir = domkdir;
    this.protocolsList = protocolsList;
    this.proxyType = proxyType;
    this.submitOnly = submitOnly;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.setHTTPConnectionTimeOutAllowed = setHTTPConnectionTimeOutAllowed;
    this.noAbortFile = noAbortFile;
    this.recursive = recursive;
    this.noWaitOnRemotePut = noWaitOnRemotePut;

    if(sMaxTimeAllowed != 10800) {
      this.statusMaxTimeAllowed = sMaxTimeAllowed;
    }
    if(statusWaitTime < 1000) {
      this.statusWaitTime = statusWaitTime*1000;
    }
    else {
      this.statusWaitTime = statusWaitTime;
    }
    this.parallelism = parallelism;
    this.bufferSize = bufferSize;
    this.statusWaitTimeGiven = statusWaitTimeGiven;
    this.dcau = dcau;
    this.directGsiFTP = directGsiFTP;
    this.remotePutCase = remotePutCase;

    inputVec.clear();
    inputVec.addElement("RemotePutCase="+remotePutCase);
    inputVec.addElement(" StatusWaitTime="+this.statusWaitTime);
    inputVec.addElement(" StatusMaxTime="+(this.statusMaxTimeAllowed*1000));
    inputVec.addElement(" threshold="+this.threshHoldValue);
    util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);

    String[] args = new String[2];

   this.uid = uid;
   this.doReserveSpace = doReserveSpace;
   //this.tokenType = type;
   this.tokenLifetime = tokenlifetime;
   this.tokenSize = tokensize;
   this.guarnSize = guarnSize;
   this.fileInfo = fileInfo;

   if(!directGsiFTP) {
    args[0] = serverUrl;
    args[1] = uid;
    this.serverUrl = serverUrl;

    util.printMessageHL("SRM-CLIENT: " + new Date()+ 
		" Connecting to " + serverUrl,pIntf);
    if(!uid.equals("")) {
      if(_debug) {
        util.printMessage("\nSRM-CLIENT: AuthId=" + uid,logger,silent);
        util.printMessage("\nSRM-CLIENT: AuthId=" + uid,pIntf);
      }
    }
    inputVec.addElement("ServerUrl="+serverUrl);
    inputVec.addElement("AuthId="+uid);
    util.printEventLog(_theLogger,"SrmPutClient.constructor",
	inputVec,silent,useLog);
    openSRMConnection();
   }

   if(checkPing) {
      gateWayModeEnabled =SRMUtilClient.isServerGateWayEnabled(_srm, uid,
                        logger,  _debug, credential,
                        proxyType, serverUrl,  _theLogger,
                        pIntf,  _srmClientIntf, silent,
                        useLog, connectionTimeOutAllowed,
                        setHTTPConnectionTimeOutAllowed,
                        delegationNeeded, numRetry, retryTimeOut);
     inputVec.clear();
     inputVec.addElement("GateWayModeEnabled="+gateWayModeEnabled);
     util.printEventLog(_theLogger,"SrmPutClient.constructor",
	inputVec,silent,useLog);
     System.out.println("SRM-CLIENT: GateWayModeEnabled="+gateWayModeEnabled);
      _srmClientIntf.setGateWayModeEnabled(gateWayModeEnabled);
   } 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// openSRMConnection
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void openSRMConnection () throws Exception {

    URL uu = null;
    MyGlobusURL gurl = new MyGlobusURL(serverUrl,0);
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
    SimpleProvider provider = new SimpleProvider();
    SimpleTargetedChain c = null;
    if(serverUrl.startsWith("httpg")) {
       String protocols0 = System.getProperty("java.protocol.handler.pkgs");
       org.globus.net.protocol.httpg.Handler handler =
           new org.globus.net.protocol.httpg.Handler();
       try {
         uu = new URL("httpg",host,port,path,handler);
       }catch(Exception h) {
          System.out.print("does not work");
       }
       c = new SimpleTargetedChain(new GSIHTTPSender());
       provider.deployTransport("httpg",c);
    }
    else if(serverUrl.startsWith("https")) {
       c = new SimpleTargetedChain(new HTTPSSender());
       provider.deployTransport("https",c);
       uu = new URL(serverUrl);
    }
    else {
       c = new SimpleTargetedChain(new HTTPSender());
       provider.deployTransport("http",c);
       uu = new URL(serverUrl);
    }
    Util.registerTransport();
    SRMServiceLocator service = new SRMServiceLocator(provider);

    ISRM srm = service.getsrm(uu);

    if(srm == null) {
      inputVec.addElement("remote srm object is null");
      util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
      throw new Exception("\nremote srm object is null");
    }
    else {
      inputVec.addElement("got remote srm object");
      util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
    }
 

    _srm = srm;

    org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) _srm;
    srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000);
    checkTargetContainsGsiFTP(fileInfo);
    if(delegationNeeded.equals("false")) {
      if(_debug) {
	  util.printMessage("SRM-CLIENT: no delegation by client",
			    logger,silent);
      }    
    } else if(delegationNeeded.equals("true")) {
	setSecurityPropertiesForcefully(uu);    
    } else if(delegationNeeded.equals("")) {
	setSecurityProperties(uu);
    }
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkSourceContainsFileInfo
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void checkTargetContainsGsiFTP(Vector fileInfo) {
   int size = fileInfo.size();
   for(int i = 0; i < size; i++) {
      FileInfo ffInfo = (FileInfo)fileInfo.elementAt(i);
      if (ffInfo.getTURL().startsWith("gsiftp")) {
          targetHasGsiFTP = true;
      }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setThirdPartyType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setThirdPartyType (int partytype) {
  this.partytype = partytype;
}

public int getThirdPartyType () {
  return partytype; 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// performTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void performTransfer(boolean skip) throws Exception {
    String tag = "performTransfer.put";
    SRMClientN.logMsg("start. skip="+skip, tag, null);
    if(skip) {	
	_srmClientIntf.setRequestDone(true,false);
	return;    
    } else {
	try {
	    srmPut(uid, fileInfo);
	}catch(Exception e) {
	    String msg = e.getMessage();
	    util.printEventLogException(_theLogger,"PerformTranfer",e);
	    int idx = msg.indexOf("Connection refused");
	    int idx1 = msg.indexOf("Defective credential detected");
	    int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
	    int idx6 = msg.indexOf("java.net.SocketTimeoutException: Read timed out");
				   
	    util.printHException(e,pIntf);
	    if(pIntf != null) {
		pIntf.setCompleted(false);
	    }
	    if( msg.startsWith("CGSI-gSOAP: Could not find mapping") ||  idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
		SRMClientN.logMsg("Exception="+msg, tag, "SRM-CLIENT:");
		util.printMessageHException("\nSRM-CLIENT: Exception : "+msg,pIntf);
		if(pIntf == null) {
		    if(idx != -1 || idx6 !=-1 ) {
			SRMClientN.logMsg("ExitCode="+90, tag, null);
			util.printHException(e,pIntf);
			System.exit(90);		    
		    } else if(idx1 != -1 || idx5 != -1) {
			SRMClientN.logMsg("Proxy type mismatch. ExitCode="+96, tag, null);
			util.printHException(e,pIntf);
			System.exit(96);
		    } else {
			SRMClientN.logMsg("ExitCode="+91, tag, null);
			util.printHException(e,pIntf);
			System.exit(91);
		    } 
		}
	    } else {
		throw e;
	    }
	} finally {
	    SRMClientN.logMsg("end", tag, null);
	}
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setGetTypeWSDLIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setGetTypeWSDLIntf (SRMWSDLIntf getTypeWSDLIntf) {
  ;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getGetTypeWSDLIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMWSDLIntf getGetTypeWSDLIntf () {
  return null;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatusEnquiry
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doStatusEnquiry() throws Exception {
    String tag="SrmStatusOfPutRequest";
    SRMClientN.logMsg("start", tag, "\nSRM-CLIENT:");
    
    int exitCode = doStatus(uid, fileInfo, statusToken);
    
    SRMClientN.logMsg("exitCode="+exitCode, tag, "\nSRM-CLIENT:");
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseSpace() throws Exception {
    if(gateWayModeEnabled) return;
    
    String tag = "SrmReleaseSpace";
    if(fileToken != null) {
	SRMClientN.logMsg("Token="+fileToken, tag, "\nSRM-CLIENT:");
	releaseSpace(fileToken,true);  
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles (String turl, String rid, int label) throws Exception 
{
    String tag = "abortFiles";

    String[] msg = {"SURL="+turl, "token="+rid, "label="+label, "gatewaymodeEnabled="+gateWayModeEnabled};
    
    SRMClientN.logMsg(msg, tag, null);
    
    if(gateWayModeEnabled) return; 

    if(rid == null || rid.equals("")) {
	SRMClientN.logMsg("Cannot abort with null rid", tag, "\nSRM-CLIENT:");
	return;
    }

    if(noAbortFile) {
	SRMClientN.logMsg("Skipping abort file as user req.", null, "SRM-CLIENT:");
	return;
    }

    SRMClientN.logMsg("Calling:", tag, "\nSRM-CLIENT:");

    util.printEventLog(_theLogger,"Input parameters for SrmAbortFiles",inputVec,silent,useLog);
    
    org.apache.axis.types.URI uri = new org.apache.axis.types.URI (turl);
    
    SrmAbortFilesRequest req = new SrmAbortFilesRequest();
    req.setArrayOfSURLs(SRMUtilClient.formArrayOfAnyURI(uri));
    req.setRequestToken(rid);
    if(!uid.equals("")) {
	req.setAuthorizationID(uid);
    }
    
    try {		
	SrmAbortFilesResponse result = null;
	
	result = (SrmAbortFilesResponse) callSoapThread(req,result,"srmabortfiles");	    

	SRMClientN.logMsg("### Output of SRM ###", null, "\nSRM-CLIENT:");

	if(result == null) {
	    SRMClientN.logMsg("Response=null.", tag, "\nSRM-CLIENT:");

	    FileIntf fIntf = (FileIntf) fileInfo.elementAt(label); 
	    String temp = fIntf.getFileExplanation();
	    if(temp.equals("")) {
		fIntf.setFileExplanation("SRM-CLIENT: Failed during srmAbortFiles");	    
	    } else {
		fIntf.setFileExplanation(temp+ ", Failed during srmAbortFiles");
	    }
	    return;
	}
	
	if(result.getReturnStatus() != null) {	    
	    SRMClientN.logMsg("\tstatus="+result.getReturnStatus().getStatusCode(), tag, "\nSRM-CLIENT:");
	    SRMClientN.logMsg("\tstatus="+result.getReturnStatus().getExplanation(), tag, "\nSRM-CLIENT:");	
	} else {
	    SRMClientN.logMsg("Null return status.", tag, "\nSRM-CLIENT:"); 
	    return;
	}
	
	if(result.getArrayOfFileStatuses() == null) {
	    SRMClientN.logMsg("Null ArrayOfFileStatues.", tag, "\nSRM-CLIENT:"); 
	    return;
	}

	TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
	
	if(status == null) {
	    SRMClientN.logMsg("Null TSURLReturnStatues.", tag, "\nSRM-CLIENT:"); 
	    return;
	}
	
	Object[] objArray = fileInfo.toArray();
	for(int i = 0; i < objArray.length; i++) {
	    FileInfo fInfo = (FileInfo) objArray[i];
	    if(fInfo.getOrigTURL().equals(status.getSurl().toString())) {
		if(status != null && status.getStatus() != null && 
		   status.getStatus().getStatusCode() != null) {
		    fInfo.setFileStatus(status.getStatus().getStatusCode().toString());
		}
		if(status.getStatus().getExplanation() != null) {
		    fInfo.setFileExplanation(status.getStatus().getExplanation());		
		} else {
		    fInfo.setFileExplanation("SRM-CLIENT: AbortFiles is called successfully");
		}
	    }
	}
	
	SRMClientN.logMsg("\tsurl="+status.getSurl().toString(), tag, "\nSRM-CLIENT:"); 
	SRMClientN.logMsg("\tstatus="+status.getStatus().getStatusCode(), tag, "\nSRM-CLIENT:"); 
	SRMClientN.logMsg("\texplanation="+status.getStatus().getExplanation(),tag, "\nSRM-CLIENT:"); 	
    }catch(Exception e) {
	util.printEventLogException(_theLogger,"srmAbortFiles.put",e);
	util.printMessage("SRM-CLIENT: " + e.getMessage(),logger,silent);
	util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
	//e.printStackTrace();
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// putDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) throws Exception {
    String tag = "SRMPutClientPutDone";
    String[] msg = {"SURL="+siteUrl, "token="+rid, "label="+label, "gatewaymodeenabled="+gateWayModeEnabled};
    /*_srmClientIntf*/SRMClientN.logMsg(msg, tag, "SRM-CLIENT:");

    if(gateWayModeEnabled) return;
    
    org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);
    
    SrmPutDoneRequest req = new SrmPutDoneRequest();
    req.setArrayOfSURLs(SRMUtilClient.formArrayOfAnyURI(uri));
    req.setRequestToken(rid);
    if(!uid.equals("")) {
	req.setAuthorizationID(uid);
    }
    
    try {
	SrmPutDoneResponse result = null;
	result = (SrmPutDoneResponse) callSoapThread(req,result,"srmputdone");   

	boolean resultNotNull = true;
	String checkUrl = siteUrl;
	
	TSURLReturnStatus status = null;
	TStatusCode temp = null;
	
	if(result == null) {
	    /*_srmClientIntf*/SRMClientN.logMsg("SRM returned null result for the srmPutDone request", tag, "\nSRM-CLIENT:"+new Date());
	    FileIntf fIntf = (FileIntf) fileInfo.elementAt(label); 
	    fIntf.setFileExplanation("SRM-CLIENT: Failed during putDone");
	    fIntf.setPutDoneFailed(true);
	    return;
	}

	if(result.getReturnStatus() == null) {
	    /*_srmClientIntf*/SRMClientN.logMsg("SRM returned null returnstatus for the srmPutDone request", tag, "\nSRM-CLIENT:"+new Date());
	    return;
	}
    
	/*_srmClientIntf*/SRMClientN.logMsg("Result.status="+result.getReturnStatus().getStatusCode(), tag, "\nSRM-CLIENT:"+new Date());
	/*_srmClientIntf*/SRMClientN.logMsg("Result.explanation="+result.getReturnStatus().getExplanation(), tag, "\nSRM-CLIENT:"+new Date());
    
	if(result.getArrayOfFileStatuses() == null) {
	    /*_srmClientIntf*/SRMClientN.logMsg("SRM returned null getArrayOfFileStatus from srmPutDone()", tag, "\nSRM-CLIENT:"+new Date());
	    return;
	}
	
	status = result.getArrayOfFileStatuses().getStatusArray(0);
	temp = status.getStatus().getStatusCode();
	checkUrl = status.getSurl().toString();
	if(_debug) {
	    /*_srmClientIntf*/SRMClientN.logMsg("### Output of SRM ### ", null, "SRM-CLIENT:");
	}
	
	/*_srmClientIntf*/SRMClientN.logMsg("SURL="+checkUrl+" status="+temp+" explanation="+status.getStatus().getExplanation(), tag, null);
	
	Object[] objArray = fileInfo.toArray();
	for(int i = 0; i < objArray.length; i++) {
	    FileInfo fInfo = (FileInfo) objArray[i];
	    if(fInfo.getOrigTURL().equals(checkUrl)) {
		if(status != null && status.getStatus().getExplanation() != null) {
		    fInfo.setFileExplanation(status.getStatus().getExplanation());		
		} else {
		    fInfo.setFileExplanation("SRM-CLIENT: PutDone is called successfully");			
		}
	    }
	}

	if(_debug) {
	    /*_srmClientIntf*/SRMClientN.logMsg("\tsurl="+status.getSurl().toString(), tag, "SRM-CLIENT:");
	    /*_srmClientIntf*/SRMClientN.logMsg("\tstatus="+temp, tag, "SRM-CLIENT:");
	    /*_srmClientIntf*/SRMClientN.logMsg("\texplanation="+status.getStatus().getExplanation(), tag, "SRM-CLIENT:");
	}
	
	TStatusCode tempCode = temp;
	TPutRequestFileStatus[] statuses= null;
	TPutRequestFileStatus f_status = null;
	StringBuffer rCode = new StringBuffer();
	
	if(remotePutCase && !noWaitOnRemotePut) {
	    URI uuri = new URI(siteUrl);
	    Vector siteUrls = new Vector();
	    siteUrls.addElement(uuri);
	    boolean checkUntilDone = true;
	    while(checkUntilDone) {
		int tStatusTime = 1;
		if(statusWaitTime >= 1000) {
		    tStatusTime = statusWaitTime/1000;
		}
		/*_srmClientIntf*/SRMClientN.logMsg("Doing next status call in " + tStatusTime + " seconds", tag, "SRM-CLIETN:");
		Thread.sleep(statusWaitTime);
		rCode.delete(0,rCode.length());
		IsWholeRequestFailed wholeRequestFailed = 
		    new IsWholeRequestFailed();
		statuses  = checkPutStatus(siteUrls, rid, rCode,	
					   wholeRequestFailed);
		if(statuses == null) { 
		    if(wholeRequestFailed.getWholeRequestFailed()) {
			if(wholeRequestFailed.getStatus() != null) {
			    if(wholeRequestFailed.getStatus() == 
			       TStatusCode.SRM_ABORTED) {
				checkUntilDone = false;
			    }
			}
		    } 
		}//end if(statuses == null) 
		else {
		    for(int kk = 0; kk < statuses.length; kk++) {
			f_status = (TPutRequestFileStatus) statuses[kk];
			if(f_status != null) {
			    if(wholeRequestFailed.getWholeRequestFailed()) {
				tempCode = TStatusCode.SRM_FAILURE;
				rCode.append(f_status.getStatus().getExplanation());			    
			    } else {
				tempCode = f_status.getStatus().getStatusCode();
				rCode.append(f_status.getStatus().getExplanation());
				if(tempCode == TStatusCode.SRM_FILE_IN_CACHE ||
				   tempCode == TStatusCode.SRM_SPACE_AVAILABLE) { 
				    //System.out.println(">>>>TEMPCODE.PutDone="+tempCode);
				    ; //continue checking the status 
				    //until SRM_SUCCESS or any error is returned				
				} else {
				    checkUntilDone = false;
				}
			    }//end else
			}//end if
		    }//end for
		}//end else
	    }//end while
	}//end (remotePutCase)
	
	if(_debug) {
	    if(remotePutCase) {
		/*_srmClientIntf*/SRMClientN.logMsg("\tsurl="+status.getSurl().toString(), tag, "SRM-CLIENT:");
		/*_srmClientIntf*/SRMClientN.logMsg("\tstatus="+tempCode, tag, "SRM-CLIENT");
		/*_srmClientIntf*/SRMClientN.logMsg("\texplanation="+rCode.toString(), tag, "SRM-CLIENT:");	    
	    } else {
		/*_srmClientIntf*/SRMClientN.logMsg("\tsurl="+status.getSurl().toString(), tag, "SRM-CLIENT:");
		/*_srmClientIntf*/SRMClientN.logMsg("\tstatus="+temp,tag, "SRM-CLIENT:");
		/*_srmClientIntf*/SRMClientN.logMsg("\texplanation="+status.getStatus().getExplanation(), tag, "SRM-CLIENT:");
	    }
	}
    }catch(Exception e) {
	util.printEventLogException(_theLogger,"SrmPut.put",e);
	util.printMessage("SRM-CLIENT: " + e.getMessage(),logger,silent);
	util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(String siteUrl, String rid,int label) 
        throws Exception {
   if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

   String code = null;
   if(rid != null && !rid.equals("")) {
       util.printMessage("\nSRM-CLIENT: " + new Date() + 
			 " Calling releaseFile ...", logger,silent);
       util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
			   " Calling releaseFile ...", pIntf);
       //util.printMessage("TimeStamp="+new Date(), logger,silent);
       inputVec.clear();
       inputVec.addElement("RID="+rid);
       inputVec.addElement("SURL="+siteUrl);
       inputVec.addElement("TimeStamp="+new Date());
       util.printEventLog(_theLogger,"ReleaseFile",inputVec,silent,useLog);
       if(_debug) {
	   getSpaceTokenMeta(fileToken);
       }
       boolean keepSpace = false; 
       code = releaseFile(keepSpace,siteUrl,rid,label);
       if(_debug) {
	   getSpaceTokenMeta(fileToken);
       }
   }
   return code;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doDirectGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDirectGsiFTP (Vector fileInfo) throws Exception {

  int size = fileInfo.size();
  for(int i = 0; i < size; i++) {
      FileInfo ffInfo = (FileInfo) fileInfo.elementAt(i);
      ffInfo.setIsDirectGsiFTP(true);
      File f = new File(util.parseLocalSourceFileForPath(ffInfo.getSURL())); 
      ffInfo.setActualSize(""+f.length());
      _srmClientIntf.initiatePullingFile(ffInfo);
  }
  int numFilesFailed = 0;
  if(size == numFilesFailed) {
     inputVec.clear();
     inputVec.addElement("SetRequestDone is set with true and true");
     util.printEventLog(_theLogger,"doDirectGsiFTP.put", inputVec,silent,useLog);
     _srmClientIntf.setRequestDone(true,true);
  }
  else {
     inputVec.clear();
     inputVec.addElement("SetRequestDone is set with true and false");
     util.printEventLog(_theLogger,"doDirectGsiFTP.put", inputVec,silent,useLog);
     _srmClientIntf.setRequestDone(true,false);
   }
}

    private void relayPutToSRM(int maxSize, int totalSize, int totalFiles, HashMap oResult, HashMap statusArray) 
	throws Exception 
    {
	int index = 0;
	int sindex = 0;
	String[] surl = new String[maxSize];
	long[] fSize = new long[maxSize]; 
	
	for(int i =  index+0; i < index+maxSize; i++) {
	    inputVec.clear();
	    FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
	    String temp = fInfo.getTURL();
	    fInfo.setOrigTURL(temp); 
	    surl[sindex] = temp;
	    fSize[sindex] = new Long(fInfo.getExpectedSize()).longValue();
	    if(_debug) {
		String[] msgs = {"From URL("+sindex+")="+fInfo.getOrigSURL(), 
				 "To URL("+sindex+")="+surl[sindex], 
				 "FileSize("+sindex+")="+fSize[sindex]};
		/*_srmClientIntf*/SRMClientN.logMsg(msgs, "Input parameters for SrmPut", "SRM-CLIENT");
	    }
	    sindex++;
	}
	
	if (doReserveSpace) {
	    fileToken = reserveSpace(uid);
	    if (fileToken == null) {
		String msg = "SRM returned null space token";
		/*_srmClientIntf*/SRMClientN.logMsg(msg, "SrmPut", "\nSRM-CLIENT: "+new Date());
		
		if (pIntf == null) {
		    /*_srmClientIntf*/SRMClientN.logMsg("ExitCode="+1000, "ExitCodeStatus", null);
		    System.exit(100);//srm returned no status
		}
	    }
	}
	
	SrmPrepareToPutResponse response = prepareToPutMultiFiles(surl, fSize, fileToken);	    
	
	StringBuffer responseBuffer = new StringBuffer();
	HashMap subStatusArray = printPutResult(response,responseBuffer,oResult);

	//added for timing out things for 3partycopy, single p2p at a time 
	//srm(full)->srm
	if (thirdparty && response == null) {
	    for(int i =  index+0; i < index+maxSize; i++) {
		FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
		_srmClientIntf.srmFileFailure(fInfo.getLabel(),"Put Request Timed Out, server may be busy");					      
	    }
	}
	
	if (response == null || subStatusArray == null || subStatusArray.size() == 0) 
	{	    
	    if (!responseBuffer.toString().equals("")) {
		String msg = "Explanation from responseBuffer="+responseBuffer.toString(); 		    
		/*_srmClientIntf*/SRMClientN.logMsg(msg, "SrmPut", "\tSRM-CLIENT:");
		responseBuffer = null;
	    }
	} else {	
	    statusArray.put(response.getRequestToken(),subStatusArray);
	}

	if (response != null) {
	    _srmClientIntf.addRequestToken(response.getRequestToken());
	} else {	
	    String msg = "Null response from SRM for this sub request";
	    /*_srmClientIntf*/SRMClientN.logMsg(msg, "SrmPut", "\nSRM-CLIENT:");
	}
	
	totalFiles = totalFiles + subStatusArray.size();
	
	if (thirdparty) {
	    _srmClientIntf.setTotalFiles(tFiles);
	} else {	
	    _srmClientIntf.setTotalFiles(totalFiles);
	}
    }

    private void handleFailedCode(TPutRequestFileStatus status, String subResponse, StringBuffer expBuffer, String subKey, StringBuffer rCode) {
	TStatusCode code = status.getStatus().getStatusCode();
	if(code == null) {
	    code = SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);
	}
	
	printErrorResponse(status);
	String gSURL =  subKey;
	Object[] objArray = fileInfo.toArray();
	FileInfo ffInfo = null;
	for(int kk = 0; kk < objArray.length; kk++) {
	    ffInfo = (FileInfo) objArray[kk];
	    if(ffInfo.getTURL().equals(gSURL)) {
		ffInfo.setRID(subResponse);
		if(code == TStatusCode.SRM_ABORTED) {
		    ffInfo.setFileStatus(code.toString());
		    ffInfo.setFileExplanation("SRM-CLIENT: File Aborted");
		    _srmClientIntf.srmFileFailure(ffInfo.getLabel(), "File Aborted.");								  
		} else {
		    if(status != null && status.getStatus() != null) {
			if(status.getStatus().getExplanation() != null) {
			    expBuffer.append(status.getStatus().getExplanation());
			}
		    }
		    ffInfo.setFileStatus(code.toString());
		    ffInfo.setFileExplanation(expBuffer.toString());
		    if(code.toString().equals("SRM_DUPLICATION_ERROR")) {
			ffInfo.setDuplicationError(true);
		    }
		    _srmClientIntf.srmFileFailure(ffInfo.getLabel(),"");
		}
		break;
	    }
	}
    }

    private void printRemainingPinTime(TPutRequestFileStatus status) {
	if(status.getRemainingPinLifetime () != null) {
	    util.printMessage("\nSRM-CLIENT: RemainingPinTime="+
			      status.getRemainingPinLifetime(),logger,silent);
	    if(status.getRemainingPinLifetime().intValue() <= 60) {
		util.printMessage("\nSRM-CLIENT: WARNING: File transfer may be " +
				  " interrupted, because TURL lifetime is too short", 
				  logger,silent);
	    }
	}   
	if(status.getRemainingFileLifetime () != null) {
	    util.printMessage("\nSRM-CLIENT: RemainingFileLifeTime="+
			      status.getRemainingFileLifetime(),logger,silent);
	}
    }

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void srmPut(String uid, Vector fileInfo) throws Exception 
{
    int maxSize = _srmClientIntf.getMaximumFilesPerRequest (); 
    int totalSubRequest = 1;
    int totalSize = fileInfo.size();
    int modSize = 0;
    int totalFiles = 0;
    int numFailedFiles = 0; 
    HashMap statusArray = new HashMap();
    HashMap oResult = new HashMap();

    if(!statusWaitTimeGiven) {
	if(totalSize > 20) {
	    statusWaitTime=60;
	}
    }
    if(directGsiFTP) {
	doDirectGsiFTP(fileInfo);
	return;
    }
    if(maxSize == 0) { //if user did not give any option
	maxSize = totalSize;
    }
    if(totalSize == 1) { 
	totalSubRequest = 1;
	maxSize = 1;
    } else if(totalSize > maxSize) {    
	modSize = (totalSize % maxSize);
	int subSize = totalSize/maxSize;
	if(modSize > 0) {
	    totalSubRequest = subSize + 1;
	} else {
	    totalSubRequest = subSize;
	}
    } else if(totalSize <= maxSize) {
	totalSubRequest = 1;
	maxSize = totalSize;
    }

    if(!thirdparty) {
	_srmClientIntf.setTotalSubRequest(totalSubRequest);
	_srmClientIntf.setTotalFilesPerRequest(maxSize);
    } else {  
	_srmClientIntf.setTotalSubRequest(tFiles);
	_srmClientIntf.setTotalFilesPerRequest(maxSize);
      //_srmClientIntf.setTotalFilesPerRequest(1);
    }

    int count = 0;
    int index = 0;

    if(totalSize > 1) {
	String msg = "Total number of subrequests="+totalSubRequest;
	/*_srmClientIntf*/SRMClientN.logMsg(msg, "SrmPut", "\nSRM-CLIENT: "+new Date());
    }

    if(_debug) {
	util.printMessage("SRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::::", logger,silent);
	util.printMessage("SRM-CLIENT: :::: Input parameters for SrmPrepareToPutRequest :::::::", logger,silent);
	util.printMessage("SRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::::", pIntf);
	util.printMessage("SRM-CLIENT: ...Input parameters for SrmPrepareToPutRequest...", pIntf);
    }
    
    while (count < totalSubRequest) {
	if(count == (totalSubRequest -1)) {
	    if (modSize > 0) {
		maxSize = modSize;    
	    }
	}
	
	relayPutToSRM(maxSize, totalSize, totalFiles, oResult, statusArray);
	count ++;
	index = index + maxSize;
    }//end while
    
    StringBuffer expBuffer = new StringBuffer();
    int si = 0;
    
    if(!submitOnly) {
	while (statusArray.size() > 0) {	    
	    if(si >= statusArray.size()) {
		si = 0;
	    }
	    
	    Object ids = (statusArray.keySet().toArray()) [si];
	    HashMap subStatusArray = (HashMap) (statusArray.get(ids));
	    boolean timedOutCase=false;
	    String subResponse = (String)ids;
	    StringBuffer rCode = new StringBuffer();
	    String subKey="";
	    String oSubKey="";
	    
	    long sTimeStamp = util.startTimeStamp();
	    while (subStatusArray.size() > 0) { 
		Vector keySURLS = new Vector(); 
		Object[] keySetArray = subStatusArray.keySet().toArray();
		//march 4, 10 
		//Object[] oldKeySetArray = oResult.keySet().toArray();
		rCode.delete(0,rCode.length());
		expBuffer.delete(0,expBuffer.length());
		
		for(int k = 0; k < keySetArray.length; k++) {
		    expBuffer.delete(0,expBuffer.length());
		    oSubKey = (String) keySetArray [k];
		    //subKey = (String) oldKeySetArray [k];
		    //march 4, 10
		    subKey = (String) oResult.get(oSubKey);
		    TPutRequestFileStatus status = (TPutRequestFileStatus) (subStatusArray.get(oSubKey));			
		    TStatusCode code = status.getStatus().getStatusCode();
		    if(timedOutCase) {
			Object[] objArray = fileInfo.toArray();
			for(int kk = 0; kk < objArray.length; kk++) {
			    FileInfo ffInfo = (FileInfo) objArray[kk];
			    String turl = subKey;
			    if(ffInfo.getTURL().equals(turl)) {
				if(code != null) {
				    ffInfo.setFileStatus(code.toString());
				}
				ffInfo.setSURL(ffInfo.getTURL());
				abortFilesVec.add(ffInfo);
				ffInfo.setFileExplanation("SRM-CLIENT: File Status TimedOut");
				_srmClientIntf.srmFileFailure(ffInfo.getLabel(), "File Status TimedOut.");							      
				break;
			    }
			}
			numFailedFiles++;
			subStatusArray.remove(oSubKey);
		    } else {		    
			if((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED)) {	
			    keySURLS.addElement(new URI(subKey));			
			} else {						    
			    //added file_in_cache for cern v2 server
			    if (code == TStatusCode.SRM_SPACE_AVAILABLE || code == TStatusCode.SRM_SUCCESS ||
				code == TStatusCode.SRM_DONE || code == TStatusCode.SRM_FILE_IN_CACHE) 				
			    {
				//initiate pulling the file
				String gSURL =  subKey;
				Object[] objArray = fileInfo.toArray();
				FileInfo ffInfo = null;
				for(int kk = 0; kk < objArray.length; kk++) {
				    ffInfo = (FileInfo) objArray[kk];
				    if(ffInfo.getTURL().equals(gSURL)) {
					if(status != null && status.getTransferURL() != null) {
					    String srmTurl = ""+status.getTransferURL().toString();
					    ffInfo.setTURL(srmTurl);
					    ffInfo.setRID(subResponse);
					    ffInfo.setTransferURL(srmTurl);
					    //added on March 18, 09 to fix the 3partycopy file status
					    ffInfo.setFileStatus(code.toString());
					    //expected size for put request is already set SRMClientNGUI

					    _srmClientIntf.initiatePullingFile(ffInfo);
					    subStatusArray.remove(oSubKey);					
					} else {
					    keySURLS.addElement(new URI(subKey));
					} 
					break;
				    }
				}
			    } else {  //for other types of failures
				handleFailedCode(status, subResponse, expBuffer, subKey, rCode);
				numFailedFiles ++;
				subStatusArray.remove(oSubKey);
			    }//end else
			}//end else if
		    }//end else
		}//end for
		
		if(abortFilesVec.size() > 0) {
		    util.printMessage("\nSRM-CLIENT: Calling SrmAbortFiles", logger,silent);
				      
		    SRMUtilClient utilClient = new SRMUtilClient
			(serverUrl,uid,userDesc, _credential, _theLogger, logger,
			 pIntf, _debug,silent,useLog,false, false,
			 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
			 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
			 delegationNeeded,numRetry,retryTimeOut);
		    TStatusCode abortRequestCode = utilClient.doSrmAbortFiles(abortFilesVec,requestToken);	
		
		    util.printMessage("\nSRM-CLIENT: AbortStatusCode="+ abortRequestCode.getValue(),logger,silent);				      
		}
		
		keySetArray = subStatusArray.keySet().toArray();
		
		if(keySetArray.length > 0 && !util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
		    String msg = "Max retry check status exceeded for put status";
		    /*_srmClientIntf*/SRMClientN.logMsg(msg, "SrmPut", "SRM-CLIENT: "+new Date());
		    timedOutCase=true;
		}
		
		
		if(keySetArray.length > 0 && !timedOutCase) { 
		    oSubKey = (String) keySetArray [0];
		    TPutRequestFileStatus status = (TPutRequestFileStatus) (subStatusArray.get(oSubKey));
			
		    TStatusCode code = status.getStatus().getStatusCode();
		    if((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED)) {		       
			int tStatusTime = 1;
			if(statusWaitTime >= 1000) {
			    tStatusTime = statusWaitTime/1000;
			}			
			String msg = "NextStatusCallIn="+tStatusTime+" seconds.";
			/*_srmClientIntf*/SRMClientN.logMsg(msg, "NextStatusCall", "SRM-CLIENT:");
		    }
		}		
		
		TPutRequestFileStatus[] statuses = null;
		IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
		if(subStatusArray.size() > 0 && !timedOutCase)  {
		    Thread.sleep(statusWaitTime);
		    if(keySURLS.size() > 0) {
			statuses  = checkPutStatus(keySURLS, subResponse, rCode, wholeRequestFailed);						   
		    }
		    
		    TStatusCode tempCode = null;
		    if(statuses == null) { 
			if(wholeRequestFailed.getWholeRequestFailed()) {
			    if(wholeRequestFailed.getStatus() != null) {
				if(wholeRequestFailed.getStatus() == TStatusCode.SRM_ABORTED) {
				    subStatusArray.clear();
				}
			    }
			} 		    
		    } else {
			for(int kk = 0; kk < statuses.length; kk++) {
			    TPutRequestFileStatus status = (TPutRequestFileStatus) statuses[kk];
				
			    expBuffer.delete(0,expBuffer.length());
			    MyGlobusURL gurl = new MyGlobusURL(status.getSURL().toString(),1);				
			    String protocol = gurl.getProtocol();
			    String host = gurl.getHost();
			    int port = gurl.getPort();
			    String path = gurl.getFilePath();
			    int idx = path.indexOf("?SFN=");
			    if(idx != -1) {
				path = path.substring(idx+5);
			    }
			    
			    subStatusArray.put(protocol+"://"+host+":"+port+"/"+path,status);
			    
			    if(status != null) {
				if(wholeRequestFailed.getWholeRequestFailed()) {
				    tempCode = TStatusCode.SRM_FAILURE;
				    expBuffer.append(status.getStatus().getExplanation());
				    TReturnStatus rs = new TReturnStatus();
				    rs.setExplanation(status.getStatus().getExplanation());
				    rs.setStatusCode(tempCode);
				    status.setStatus(rs);
				}
				else {
				    tempCode = status.getStatus().getStatusCode();
				    expBuffer.append(status.getStatus().getExplanation());
				    if(tempCode == TStatusCode.SRM_SPACE_AVAILABLE || tempCode == TStatusCode.SRM_SUCCESS ||				      
				       tempCode == TStatusCode.SRM_DONE || tempCode == TStatusCode.SRM_FILE_IN_CACHE) 
				    {
					printRemainingPinTime(status);
				    }
				}			    
			    } else {
				tempCode = SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);				    
			    }
			    
			    if(tempCode != TStatusCode.SRM_REQUEST_QUEUED && tempCode != TStatusCode.SRM_REQUEST_INPROGRESS) {				
				String[] msgs = {"SURL="+status.getSURL(), 
						 "FileStatus code from server="+tempCode,
						 "Explanation from server="+expBuffer.toString()};
				/*_srmClientIntf*/SRMClientN.logMsg(msgs, "SrmPut", "SRM-CLIENT");
			    }
			}//end for
		    }//end else
		}//end if (!timedOutCase)
		keySURLS.clear();
	    }//while subStatus

	    if(subStatusArray.size() == 0)  { 
		oResult.clear();
		statusArray.remove(ids);
	    }
	    si++;
	}//while status
    }//(!submitOnly)
    
    if(rStatus != null) {
	if(rStatus.getStatusCode() != null) {
	    resultStatus = rStatus.getStatusCode().toString();
	}
	if(rStatus.getExplanation() != null) {
	    resultExplanation = rStatus.getExplanation();
	}
    }
    
    if(submitOnly) {
        inputVec.clear();
        inputVec.addElement
	    ("SetRequestDone is set with true and true in submitonly");
        util.printEventLog(_theLogger,"srmPut", inputVec,silent,useLog);
        _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
        _srmClientIntf.setRequestDone(true,true);    
    } else {
	if(!callSetDone) {
	    if(!thirdparty) {
		inputVec.clear();
		inputVec.addElement
		    ("SetRequestDone is set with true and false in notCallSetDone");
		util.printEventLog(_theLogger,"srmPut", inputVec,silent,useLog);
		_srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		_srmClientIntf.setRequestDone(true,false);	    
	    } else {
		if(_srmClientIntf.getRequestStatus().equals("")) {
		    _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		}
		if(partytype == 1) {
		    //when surl is gsiftp, we have to set request done here
		    _srmClientIntf.setRequestDone(true,false);
		}
	    }	
	} else {
	    if(numFailedFiles == totalFiles) {
		if(!thirdparty) {
		    /*_srmClientIntf*/SRMClientN.logMsg("SetRequestDone is set with true and true in callSetDone", "SrmPut", null);
		    _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		    _srmClientIntf.setRequestDone(true,true);		
		} else {
		    if(_srmClientIntf.getRequestStatus().equals("")) {
			_srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		    }
		    if(partytype == 1) {
			//when surl is gsiftp, we have to set request done here
			_srmClientIntf.setRequestDone(true,false);
		    }
		}	    
	    } else  {
		if(!thirdparty) {
		    inputVec.clear();
		    inputVec.addElement("SetRequestDone is set with true and false " +
					"in callSetDone and numFailedFiles is not equal to totalFiles");
		    util.printEventLog(_theLogger, "doDirectGsiFTP.put", inputVec,silent,useLog);
				       
		    _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		    _srmClientIntf.setRequestDone(true,false);
		
		} else {
		    if(_srmClientIntf.getRequestStatus().equals("")) {
			_srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
		    }
		    if(partytype == 1) {
			//when surl is gsiftp, we have to set request done here
			_srmClientIntf.setRequestDone(true,false);
		    }
		}
	    }
	}
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printErrorResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printErrorResponse(TPutRequestFileStatus fileStatus) {
   //util.printMessage("SRM-CLIENT: ...........................................",logger,silent);

   if(fileStatus == null) {
     inputVec.clear();
     inputVec.addElement("printErrorResponse:Null fileStatus from SRM");
     util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: " + 
		"printErrorResponse:Null fileStatus from SRM", logger,silent);
     util.printMessage("\nSRM-CLIENT: " + 
		"printErrorResponse:Null fileStatus from SRM", pIntf);
     return;
   }
   if(_debug) {
     util.printMessage("\tsurl="+
            fileStatus.getSURL().toString(),logger,silent);
     util.printMessage("\tStatus="+
            fileStatus.getStatus().getStatusCode(),logger,silent);
     util.printMessage("\tExplanation="+
            fileStatus.getStatus().getExplanation(),logger,silent);
     util.printMessage("\tsurl="+
            fileStatus.getSURL().toString(),pIntf);
     util.printMessage("\tStatus="+
            fileStatus.getStatus().getStatusCode(),pIntf);
     util.printMessage("\tExplanation="+
            fileStatus.getStatus().getExplanation(),pIntf);
   }
   inputVec.clear();
   inputVec.addElement("SURL="+fileStatus.getSURL().toString());
   inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
   inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
   util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printPutResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public HashMap printPutResult (SrmPrepareToPutResponse response,
	StringBuffer responseStatus, HashMap oResult) throws Exception {

    HashMap result = new HashMap ();
    if(_debug) {
      util.printMessage("\nSRM-CLIENT: ####### Output from SRM #####",logger,silent);
      util.printMessageHL("\nSRM-CLIENT:###### Output from SRM #####",pIntf);
    }
    if (response == null) {
      inputVec.clear();
      inputVec.addElement("printPutResult:SRM returned null response");
      util.printMessage("SRM-CLIENT: " +
			"printPutResult:SRM returned null response", logger, silent);
      util.printMessage("SRM-CLIENT: " +
			"printPutResult:SRM returned null response", pIntf);
      util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
      return result;
    }

    if(response.getRequestToken() != null) {
      inputVec.clear();
      inputVec.addElement("request.token="+response.getRequestToken());
        util.printMessage("request.token= "+ 
			response.getRequestToken(),logger,silent);
        util.printMessage("request.token= "+ 
			response.getRequestToken(),pIntf);
        requestToken = response.getRequestToken();
    }
    else {
      inputVec.clear();
      inputVec.addElement("request.token=null");
      util.printMessage("request.token =null",logger,silent);
      util.printMessage("request.token =null",pIntf);
    }

    if(response.getReturnStatus() == null) {
       inputVec.clear();
       inputVec.addElement("printPutResult:SRM returned null return status for the response");
       util.printMessage("SRM-CLIENT: printPutResult:SRM returned null return status for the response", logger, silent);
       util.printMessage("SRM-CLIENT: printPutResult:SRM returned null return status for the response", pIntf);
       responseStatus.append("SRM-CLIENT: null return status"); 
       util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
       return result;
    }

    resultStatus = response.getReturnStatus().getStatusCode().toString();
    if(response.getReturnStatus().getExplanation() != null) {
      resultExplanation = response.getReturnStatus().getExplanation();
    }
    inputVec.addElement("response.status="+response.getReturnStatus().getStatusCode());
    inputVec.addElement("response.explanation="+response.getReturnStatus().getExplanation());
 
    if(submitOnly) {
      rStatus = response.getReturnStatus();
    }
    util.printMessage("Request.status="+
      response.getReturnStatus().getStatusCode(),logger,silent);
    util.printMessage("explanation="+
      response.getReturnStatus().getExplanation(),logger,silent);
    util.printMessage("Request.status="+
      response.getReturnStatus().getStatusCode(),pIntf);
    util.printMessage("explanation="+
      response.getReturnStatus().getExplanation(),pIntf);

    if(response.getReturnStatus().getExplanation() != null) {
      responseStatus.append(response.getReturnStatus().getExplanation());
    }

    responseStatus.append(
			response.getReturnStatus().getStatusCode().toString());

    if(response.getRemainingTotalRequestTime() != null) {
        inputVec.addElement("request.remainingTotalRequestTime="+
			response.getRemainingTotalRequestTime());
        if(_debug) {
          util.printMessage("request.remainingTotalRequestTime=" +
             response.getRemainingTotalRequestTime(),logger,silent);
          util.printMessage("request.remainingTotalRequestTime=" +
             response.getRemainingTotalRequestTime(),pIntf);
        }
    }
    else {
        inputVec.addElement("request.remainingTotalRequestTime=null");
        if(_debug) {
          util.printMessage("request.remainingTotalRequestTime=" +
             null,logger,silent);
          util.printMessage("request.remainingTotalRequestTime=" +
             null,pIntf);
        }  
    }

    if(response.getArrayOfFileStatuses() == null) {
       inputVec.addElement("printPutResult:SRM returns null getArrayOfFileStatuses"); 
       util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + "printPutResult:SRM returns null getArrayOfFileStatuses ", logger,silent);
       util.printMessage("SRM-CLIENT: " + "printPutResult:SRM returns null getArrayOfFileStatuses ", pIntf);
       return result; 
    }

    if(response.getArrayOfFileStatuses().getStatusArray() == null) {
       inputVec.addElement("printPutResult:SRM returns null getStatusArray");
       util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + 
			"printPutResult:SRM returns null getStatusArray", logger,silent);
       util.printMessage("SRM-CLIENT: " + 
			"printPutResult:SRM returns null getStatusArray", pIntf);
       return result; 
    }

    int size = response.getArrayOfFileStatuses().getStatusArray().length;

    util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);

    int localStatusWaitTime = 60;
    boolean estimatedTimeGiven = false;
    for (int i=0;  i < size; i++) {
       TPutRequestFileStatus fileStatus =
         response.getArrayOfFileStatuses().getStatusArray(i);
       if(_debug) {
         util.printMessage("\tsurl="+
            fileStatus.getSURL().toString(),logger,silent);
         if(fileStatus.getTransferURL() != null) {
           util.printMessage("\ttransferurl="+
              fileStatus.getTransferURL().toString(),logger,silent);
         }
         util.printMessage("\tstatus="+
            fileStatus.getStatus().getStatusCode(),logger,silent);
         util.printMessage("\texplanation="+
            fileStatus.getStatus().getExplanation(),logger,silent);
         util.printMessage("\tsurl="+ fileStatus.getSURL().toString(),pIntf);
         util.printMessage("\tstatus="+ fileStatus.getStatus().getStatusCode(),
				pIntf);
         util.printMessage("\texplanation="+
            fileStatus.getStatus().getExplanation(),pIntf);
       }

       if(fileStatus.getEstimatedWaitTime () != null) {
         Integer xx = fileStatus.getEstimatedWaitTime();
         int yy = xx.intValue();
         if(yy != -1) {
           estimatedTimeGiven = true;
           if(_debug) {
               System.out.println("\nSRM-CLIENT: EstimatedWait " +
                "given by server is " + yy + " seconds.");
           }
           if(localStatusWaitTime > yy) {
            localStatusWaitTime = yy;
          }
         }
       }
       MyGlobusURL gurl = new MyGlobusURL(fileStatus.getSURL().toString(),1);
       String protocol = gurl.getProtocol();
       String host = gurl.getHost();
       int port = gurl.getPort();
       String path = gurl.getFilePath();
       int idx = path.indexOf("?SFN=");
       if(idx != -1) {
          path = path.substring(idx+5);
       }
       result.put(protocol+"://"+host+":"+port+"/"+path, fileStatus);
       //oResult.put(fileStatus.getSURL().toString(), fileStatus);
       //march 4,10
       oResult.put(protocol+"://"+host+":"+port+"/"+path, fileStatus.getSURL().toString());
       inputVec.clear();
       inputVec.addElement("SURL="+fileStatus.getSURL().toString());
       inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
       inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
       util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);

       Object[] objArray = fileInfo.toArray();
       for(int j = 0; j < objArray.length; j++) {
          FileInfo fInfo = (FileInfo) objArray[j];
          if(fInfo.getOrigTURL().equals(fileStatus.getSURL().toString())) {
            if(fileStatus.getStatus() != null && 
			   fileStatus.getStatus().getStatusCode() != null) {
              fInfo.setFileStatus(fileStatus.getStatus().getStatusCode().toString());
            }
            if(fileStatus.getStatus() != null && 
			   fileStatus.getStatus().getExplanation() != null) {
              fInfo.setFileExplanation(fileStatus.getStatus().getExplanation());
            }
            else {
              fInfo.setFileExplanation("");
            }
            if(fileStatus.getTransferURL() != null) {
                fInfo.setTransferURL(fileStatus.getTransferURL().toString());
            }
          }
       }
   }

   if(_debug) {
     util.printMessage("...........................................",logger,silent);
     util.printMessage("...........................................",pIntf);
   }

   if(estimatedTimeGiven) {
      //System.out.println(">>EstimatedTime given");
      statusWaitTime = localStatusWaitTime*1000;
      if(statusWaitTime >= statusMaxTimeAllowed*1000) {
         if(_debug) {
            System.out.println("\nSRM-CLIENT: Next status call is " +
              "adjusted according to totalrequesttime given by the user");
         }
         statusWaitTime = statusWaitTime - 5000;
      }
   }
   return result;

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSpaceTokenMeta
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void getSpaceTokenMeta(String token) 
	throws Exception {

  if(doReserveSpace) {
    if(token != null) {
      SRMUtilClient.getSpaceTokenMeta
        (_srm,token,logger,_debug,_credential,proxyType,
        serverUrl,_theLogger, pIntf, _srmClientIntf, silent, useLog, 
        connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
        delegationNeeded, numRetry, retryTimeOut);
    }
    else {
      inputVec.clear();
      inputVec.addElement("Cannot do getSpaceTokenMeta for null token");
      util.printEventLog(_theLogger,"GetSpaceTokenMeta",inputVec,silent,useLog);
      util.printMessage("SRM-CLIENT: " + 
			"Cannot do getSpaceTokenMeta for null token", 
		logger,silent);
      util.printMessage("SRM-CLIENT: " + 
			"Cannot do getSpaceTokenMeta for null token", pIntf);
    }
  }
}
	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(boolean keepSpace, String surl, 
	String token, int label) throws Exception {

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  return SRMUtilClient.removeFile
        (_srm,keepSpace, surl,token,uid,logger,_credential,
         proxyType,serverUrl,_theLogger,
          pIntf, _srmClientIntf, silent, useLog, 
          connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
          delegationNeeded, numRetry, retryTimeOut);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public void releaseSpace(String token, 
   boolean forceFileRelease) throws Exception {
		 
  if(gateWayModeEnabled) return;

  SRMUtilClient.releaseSpace(_srm,token,uid,forceFileRelease,
        logger,_debug,_credential, proxyType,serverUrl, _theLogger,
        pIntf, _srmClientIntf, silent, useLog, 
        connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
        delegationNeeded, numRetry, retryTimeOut); 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareToPutMultiFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public SrmPrepareToPutResponse prepareToPutMultiFiles(String[] surls, 
	long[] fileSizes, String token)  throws Exception {


  if(token != null) {
      if(_debug)  {
        getSpaceTokenMeta(token);
      }
   }

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);


   if(domkdir && !recursive) {
     SRMUtilClient utilClient = new SRMUtilClient
        (serverUrl,uid,userDesc, _credential, _theLogger, logger,
        pIntf, _debug,silent,useLog,false, false,
	    statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
	    connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
	    delegationNeeded,numRetry,retryTimeOut);

     StringBuffer sCode = new StringBuffer();
     if(fileInfo.size() == 1) {
      FileInfo fInfo  = (FileInfo) fileInfo.elementAt(0);
      String turl = fInfo.getTURL().toString();
      int idx = turl.indexOf("?SFN=");
      if(idx != -1) {
        String firstPart = turl.substring(0,idx);
        String temp = turl.substring(idx+4);
        String temp1 = ""; 
        while (true) {
          idx = temp.indexOf("/");
          if(idx != -1) {
            int idx2 = temp.indexOf("/",idx+1);
            if(idx2 != -1) {
              //System.out.println(">>>TEMP1="+temp1);
              String temp2 = temp.substring(idx,idx2);
              //System.out.println(">>>TEMP2="+temp2);
              fInfo.setTURL(firstPart+"?SFN="+temp1+temp2);
              boolean b = utilClient.doSrmMkdir(fileInfo,sCode,false,false,true,false);
		  
              String temp3 = temp.substring(idx2);
              //System.out.println(">>>TEMP3="+temp3);
              temp1 = temp1+temp2;
              temp = temp3;
            }
            else break;
          }
          else break;
        }//end while
        fInfo.setTURL(turl); //set the turl back
      }
      /*
      if(!b) {
         util.printMessage(
		  "\nSRM-CLIENT: Cannot continue put since directory" +
          " creation is not successful", logger,silent);
         int value = util.mapStatusCode(sCode.toString());     
         System.exit(value);
      }
      */
     }
     else {
       util.printMessage(
         "\nSRM-CLIENT: Cannot continue, since -mkdir option is " +
         " only for single source and single target", logger,silent); 
         System.exit(1);
     } 
   }
          

   SrmPrepareToPutRequest r = new SrmPrepareToPutRequest();

   inputVec.clear();
   inputVec.addElement("UserDescription="+userDesc);
   if(!userDesc.equals("")) {
       if(_debug) {
        util.printMessage("SRM-CLIENT: UserDescription="+userDesc, logger, silent);
        util.printMessage("SRM-CLIENT: UserDescription="+userDesc, pIntf);
      }
      r.setUserRequestDescription(userDesc);
   }

   inputVec.addElement("\tAuthorizationID="+uid);
   if(_debug) {
       util.printMessage("SRM-CLIENT: AuthorizationID="+uid, logger, silent);
       util.printMessage("SRM-CLIENT: AuthorizationID="+uid, pIntf);
   }
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   if(overwrite) {
      r.setOverwriteOption(TOverwriteMode.ALWAYS);
   }
   else {
       r.setOverwriteOption(TOverwriteMode.NEVER);
   }

   inputVec.addElement("\tOverwrite="+overwrite);
   if(_debug) {
     util.printMessage("SRM-CLIENT: Overwrite="+overwrite, logger, silent);
     util.printMessage("SRM-CLIENT: Overwrite="+overwrite, pIntf);
   }

   TTransferParameters transferParameters = new TTransferParameters();
   ArrayOfString protocolsArray = new ArrayOfString();
   Vector protocolVec = new Vector ();

   if(protocolsList.equals("")) {
     protocolVec.addElement("gsiftp");
     protocolVec.addElement("http");
     protocolVec.addElement("https");
     protocolVec.addElement("ftp");
   }
   else {
     StringTokenizer st = new StringTokenizer(protocolsList,",");
     while (st.hasMoreTokens()) {
       String vv = st.nextToken(); 
       protocolVec.addElement(vv);
     }
   }


   String [] protocols = new String[protocolVec.size()];
   for(int i = 0; i < protocolVec.size(); i++) {
      protocols[i] = (String) protocolVec.elementAt(i);
   }
   protocolVec.clear();

   String tt = "";
   for(int i = 0; i < protocols.length;i++) {
     tt = tt+protocols[i]+",";
   }

   inputVec.addElement("Protocols="+tt);
   if(_debug) {
     util.printMessage("SRM-CLIENT: Protocols="+tt, logger,silent);
     util.printMessage("SRM-CLIENT: Protocols="+tt,pIntf);
   }
   protocolsArray.setStringArray(protocols);
   transferParameters.setArrayOfTransferProtocols(protocolsArray);
   //transferParameters.setAccessPattern(TAccessPattern.TRANSFER_MODE);
   //util.printMessage("\tAccessPattern=TRANSFERMODE", logger,silent);
   //transferParameters.setConnectionType(TConnectionType.WAN);
   //util.printMessage("\tConnectionType=WAN", logger,silent);
   r.setTransferParameters(transferParameters);

   if(retentionPolicyInfo != null) {
     if(_debug) {
       util.printMessage("SRM-CLIENT: RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy(), logger,silent);
       util.printMessage("SRM-CLIENT: RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy(), pIntf);
     } 
     inputVec.addElement("RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy());
     if(_debug) {
       util.printMessage("SRM-CLIENT: ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), logger,silent);
       util.printMessage("SRM-CLIENT: ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), pIntf);
     }
     inputVec.addElement("ACCESSINFO="+retentionPolicyInfo.getAccessLatency());
     r.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
   }

   if(token != null) {
     if(_debug) {
        util.printMessage("SRM-CLIENT: SpaceToken= "+token, logger, silent);
        util.printMessage("SRM-CLIENT: SpaceToken= "+token, pIntf);
     }
     inputVec.addElement("SpaceToken="+token);
     r.setTargetSpaceToken(token);
   }
   if(fileStorageType != null) {
     if(_debug) {
        util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType, logger, silent);
        util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType, 
			pIntf);
     }
     inputVec.addElement("FileStorageType="+fileStorageType);
     r.setDesiredFileStorageType(fileStorageType);
   }
   if(totalRequestTime != 0) {
    if(_debug) {
       util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime, logger, silent);
       util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime, 
			pIntf);
    }
    inputVec.addElement("TotalRequestTime="+totalRequestTime);
    r.setDesiredTotalRequestTime(new Integer(totalRequestTime));
   }
   if(pinTime != 0) {
    if(_debug) {
       util.printMessage("SRM-CLIENT: PinTime="+pinTime, logger, silent);
       util.printMessage("SRM-CLIENT: PinTime="+pinTime, pIntf);
    }
    inputVec.addElement("PinTime="+pinTime);
    r.setDesiredPinLifeTime(new Integer(pinTime));
   }
   if(fileLifeTime != 0) {
    if(_debug) {
       util.printMessage("SRM-CLIENT: FileLifeTime="+fileLifeTime, logger, silent);
       util.printMessage("SRM-CLIENT: FileLifeTime="+fileLifeTime, pIntf);
    }
    inputVec.addElement("FileLifeTime="+fileLifeTime);
    r.setDesiredFileLifeTime(new Integer(fileLifeTime));
   }

   int total = surls.length;

   TPutFileRequest[] fileReqList = new TPutFileRequest[total];

   for (int i=0; i<total; i++) {
     TPutFileRequest curr = new TPutFileRequest();
     if(surls[i] == null || surls[i].equals("")) {
       curr.setTargetSURL(null);
     }
     else {
       curr.setTargetSURL(new URI(surls[i]));

       if(fileSizes[i] > 0) {
         //UnsignedLong ulong = new UnsignedLong();
         //ulong.setValue(fileSizes[i]);
         UnsignedLong ulong = new UnsignedLong(fileSizes[i]);
         curr.setExpectedFileSize(ulong);
       }
       else {
         curr.setExpectedFileSize(null);
       }

     }

      fileReqList[i] = curr;
   }


   ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
   Vector vec = new Vector();

   TExtraInfo tExtra = new TExtraInfo();

   if(!remoteTransferInfo.equals("")) {
     HashMap transferInfoMap =
        util.parseRemoteTransferInfo(remoteTransferInfo);
     Object obj = transferInfoMap.get("buffersize");
     if(obj != null) {
       String value = (String) obj;
       tExtra = new TExtraInfo();
       tExtra.setKey("bufferSize");
       tExtra.setValue(value);
       vec.addElement(tExtra);
       if(_debug) {
         util.printMessage("SRM-CLIENT: BufferSize="+value,logger,silent);
         util.printMessage("SRM-CLIENT: BufferSize="+value,pIntf);
       }
       inputVec.addElement("buffersize="+value);
     }
     obj = transferInfoMap.get("parallelism");
     if(obj != null) {
       String value = (String) obj;
       tExtra = new TExtraInfo();
       tExtra.setKey("gsiftpStream");
       tExtra.setValue(value);
       vec.addElement(tExtra);
       if(_debug) {
         util.printMessage("SRM-CLIENT: gsiftpStream="+value,logger,silent);
         util.printMessage("SRM-CLIENT: gsiftpStream="+value,pIntf);
       }
       inputVec.addElement("gsiftpStream="+value);
     }
     obj = transferInfoMap.get("dcau");
     if(obj != null) {
       String value = (String) obj;
       tExtra = new TExtraInfo();
       tExtra.setKey("dcau");
       tExtra.setValue(value);
       vec.addElement(tExtra);
       if(_debug) {
         util.printMessage("SRM-CLIENT: dcau="+value,logger,silent);
         util.printMessage("SRM-CLIENT: dcau="+value,pIntf);
       }
       inputVec.addElement("DCAU="+value);
     }
     obj = transferInfoMap.get("protection");
     if(obj != null) {
       String value = (String) obj;
       tExtra = new TExtraInfo();
       tExtra.setKey("protection");
       tExtra.setValue(value);
       vec.addElement(tExtra);
       if(_debug) {
         util.printMessage("SRM-CLIENT: protection="+value,logger,silent);
         util.printMessage("SRM-CLIENT: protection="+value,pIntf);
       }
       inputVec.addElement("protection="+value);
     }
     obj = transferInfoMap.get("guc");
     if(obj != null) {
       String value = (String) obj;
       tExtra = new TExtraInfo();
       tExtra.setKey("noAPI");
       tExtra.setValue(value);
       vec.addElement(tExtra);
       if(_debug) {
         util.printMessage("SRM-CLIENT: guc="+value,logger,silent);
         util.printMessage("SRM-CLIENT: guc="+value,pIntf);
       }
       inputVec.addElement("guc="+value);
     }
   }

   if(!overwrite)  {
     tExtra = new TExtraInfo();
     tExtra.setKey("reportduplicate");
     tExtra.setValue("true");
     vec.addElement(tExtra);
     if(_debug) {
         util.printMessage("SRM-CLIENT: reportduplicate=true",logger,silent);
         util.printMessage("SRM-CLIENT: reportduplicate=true",pIntf);
     }
     inputVec.addElement("reportduplicate=true");
   }

   if(_debug) {
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo, logger, silent);
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo, pIntf);
   }
   inputVec.addElement("StorageInfo="+storageInfo);

   if(storageInfo.equals("true")) {

     tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue("put");
     vec.addElement(tExtra);

     TExtraInfo tExtra1 = new TExtraInfo();
     tExtra1.setKey("pwd");
     tExtra1.setValue(proxyString.trim());
     vec.addElement(tExtra1);
  }
  else if(storageInfo.startsWith("for")) {
     StringTokenizer stk = new StringTokenizer(storageInfo,",");
     while(stk.hasMoreTokens()) {
         String temp = stk.nextToken();
         int idx = temp.indexOf(":");
         if(idx != -1) {
           String key = temp.substring(0,idx);
           String value = temp.substring(idx+1);
           tExtra = new TExtraInfo();
           tExtra.setKey(key);
           tExtra.setValue(value);
           vec.addElement(tExtra);
         }
         else {
           String msg = "Given storage info is not in the correct format " +
            "Please use the format for:source,login:uid,passwd:pwd" +
            " Given storage info is " + storageInfo;
           throw new Exception(msg);
         }
      }
  }

  TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
  for(int i = 0; i < vec.size(); i++) {
    a_tExtra[i] = (TExtraInfo) vec.elementAt(i);
  }

  storageInfoArray.setExtraInfoArray(a_tExtra);
  r.setStorageSystemInfo(storageInfoArray);

  ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
  arrayOfFileRequest.setRequestArray(fileReqList);
  r.setArrayOfFileRequests(arrayOfFileRequest);

  util.printEventLog(_theLogger,"Input parameters for SrmPrepareToPutRequest",inputVec,silent,useLog);
  util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmPrepareToPutRequest now ...", logger,silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmPrepareToPutRequest now ...", pIntf);
  //util.printMessage("TimeStamp="+new Date(), logger,silent);

  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"Sending SrmPrepareToPutRequest now",inputVec,silent,useLog);

  try {
     SrmPrepareToPutResponse result = null;

     result = (SrmPrepareToPutResponse) 
                callSoapThread(r,result,"srmpreparetoput");

     return result;
  }catch(Exception e) {
      util.printEventLogException(_theLogger,"srmPut.put",e);
      util.printMessage("SRM-CLIENT: " + e.getMessage(),logger,silent);
      util.printMessage("SRM-CLIENT: " + e.getMessage(),pIntf);
      throw e;
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callSoapThread
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Object callSoapThread(Object request, Object result, String methodName) throws Exception {			      
    String tag = "CallSoapThread";

    try {
	SRMClientN.logMsg("start "+methodName+" "+Thread.currentThread(), tag, null);	
	int nRetry = numRetry;
	boolean statusCall = false;
	String exceptionHappened="";
	
	if(methodName.equals("srmstatusofput")) {
	    nRetry = 1;
	    statusCall = true;
	}
	
	int retrySoFar=0;
	boolean timeOutHappened=false;
	
	while(retrySoFar < nRetry) {	
	    if(retrySoFar > 1) {
		Thread.sleep(retryTimeOut*1000);
	    }
	    
	    SRMClientN.logMsg("Creating NewCall for "+methodName+" numRetry="+retrySoFar, tag, null);	
	    
	    long sTimeStamp = util.startTimeStamp();
	    
	    timeOutCallBack = new TimeOutCallBack(result,sTimeStamp,
						  setHTTPConnectionTimeOutAllowed,methodName);
	    timeOutCallBack.setLogger(_theLogger,silent,useLog);
	    timeOutCallBack.start();
	    
	    soapCallThread = new SoapCallThread(_srm, request,methodName);
	    soapCallThread.setLogger(_theLogger,silent,useLog);
	    soapCallThread.start();
	    boolean stay=true;
	    timeOutHappened = timeOutCallBack.isTimedOut();
	    
	    while(stay) {
		Object responseObject = soapCallThread.getResponseObject();
		timeOutCallBack.setObject(responseObject);
		Thread.sleep(1000);
		responseObject = soapCallThread.getResponseObject();
		timeOutCallBack.setObject(responseObject);
		timeOutHappened = timeOutCallBack.isTimedOut();
		exceptionHappened = soapCallThread.exceptionHappened();
		if((timeOutHappened && responseObject == null) || (!exceptionHappened.equals("") && responseObject == null)) 
		{
		    String[] msg = {"Interrupting " + methodName, "timedout and responseobject is null"};
		    SRMClientN.logMsg(msg, tag, null);
		    try {
			_srm=null;
			soapCallThread.setInterrupt(true);
			openSRMConnection();
		    }catch(Exception ie) {
			util.printEventLogException(_theLogger,"SRMPutClient.callSoapThread",ie);					     
			System.out.println ("SRM-CLIENT: Exception happended while interrupting "+ methodName);			
		    }
		    stay = false;         
		} else {
		    responseObject = soapCallThread.getResponseObject();
		    if(responseObject != null) {
			result = responseObject;
			timeOutCallBack.setObject(responseObject);
			stay = false;
			retrySoFar = numRetry; //breaking the upper loop
			SRMClientN.logMsg("Got Response " + responseObject, tag, null);
		    }
		}
	    }//end while
	    retrySoFar++;
	}//end while
	
	if(timeOutHappened && result == null && !statusCall) {
	    String[] msg = {"setrequestdone to true and failed", "timedout and responseobject is null",
			    "SRM server did not respond, server may be busy"};
	    SRMClientN.logMsg(msg, tag, "\nSRM-CLIENT:");
	    
	    if(methodName.equals("srmpreparetoput")) {
		if(!thirdparty) {
		    _srmClientIntf.setRequestTimedOut(true);	     
		} else {
		    _srmClientIntf.setRequestInformation("SRM_RETURNED_NO_STATUS", "Request Timed Out. Reason, Server may be busy");
		}
	    }     
	} else if (!timeOutHappened && result != null && 
		   methodName.equals("srmpreparetoput") && thirdparty) {
	    //this is needed for srm(full)->srm
	    //for may P2P calls
	    if(result instanceof SrmPrepareToPutResponse) {
		TReturnStatus rStatus = ((SrmPrepareToPutResponse)result).getReturnStatus();
		String temp="";
		if(rStatus != null) {
		    temp = rStatus.getStatusCode().toString();
		}
		_srmClientIntf.setRequestInformation(temp,"");
	    }
	}
	return result;
    } finally {
	SRMClientN.logMsg("end "+methodName+" "+Thread.currentThread(), tag, null);	
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int doStatus(String uid, Vector fileInfo, String rToken)
      throws URI.MalformedURIException, java.rmi.RemoteException {

    String tag = "doSrmStatus";
    SRMClientN.logMsg("RequestToken="+rToken+"uid="+uid, tag, "SRM-CLIENT:");

    if (rToken == null) {
	SRMClientN.logMsg("\nCannt perform status with null token", tag, "\nSRM-CLIENT:");
    }

    SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
    int size = fileInfo.size();
    String sampleSURL = "";

    URI[] tsurl = new URI[size];
    for(int i = 0; i < size; i++) {
	FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
	fIntf.setRID(rToken);
	URI temp = new URI(fIntf.getTURL());
	tsurl[i] = temp;
	sampleSURL=fIntf.getTURL();
	if(_debug) {
	    util.printMessage("SRM-CLIENT: surl="+sampleSURL, logger,silent);
	    util.printMessage("SRM-CLIENT: surl="+sampleSURL, pIntf);
	}
	fIntf = null;
    }
    r.setArrayOfTargetSURLs(SRMUtilClient.convertToArrayOfAnyURI(tsurl));
    
    r.setRequestToken(rToken);
    
    if(!uid.equals("")) {
	r.setAuthorizationID(uid);
    }
    
    SrmStatusOfPutRequestResponse result = null;
    
    
    try {
	result = (SrmStatusOfPutRequestResponse) callSoapThread(r,result,"srmstatusofput");	    
    }catch(Exception e) {
	System.out.println("Exception="+e.getMessage());
	util.printEventLogException(_theLogger,"srmStatusOfPut.put",e);
    }
    
    TStatusCode sCode = null;
    tsurl = null;
    r = null;
    
    if(_debug) {
	util.printMessage("\nSRM-CLIENT: #### Output from SRM ###",logger,silent);
	util.printMessage("\nSRM-CLIENT: #### Output from SRM ###",pIntf);
    }
    
    if(result == null) {
	SRMClientN.logMsg("Null result. Check Rid. Given="+rToken, tag, "SRM-CLIENT");
	return 1000;
    }

    ArrayOfTPutRequestFileStatus arrayFStatus = result.getArrayOfFileStatuses();	
  
    if(arrayFStatus == null) {
	TReturnStatus rStatus = result.getReturnStatus();
	if(rStatus != null) {
	    sCode = rStatus.getStatusCode();
	    SRMClientN.logMsg("Code="+sCode.toString()+" Exp:"+rStatus.getExplanation(), tag, "SRM-CLIENT:");	
	} else {
	    SRMClientN.logMsg("null status", tag, "SRM-CLIENT:");
	}
	return util.mapStatusCode(sCode);
    }

   TPutRequestFileStatus[] fileStatus = arrayFStatus.getStatusArray();

   if(fileStatus.length == 0) {
       SRMClientN.logMsg("No files in the request", tag, "SRM-CLIENT:");
      return util.mapStatusCode(sCode);
   }

   for(int i = 0; i < fileStatus.length; i++) {
       SRMClientN.logMsg("surl="+fileStatus[i].getSURL().toString(), tag, "SRM-CLIENT:");
       if(fileStatus[i].getTransferURL() != null) {
	   SRMClientN.logMsg("txfurl="+fileStatus[i].getTransferURL().toString(), tag, "SRM-CLIENT:");
      }
      if(fileStatus[i].getFileSize() != null) {
	  SRMClientN.logMsg("filesize="+fileStatus[i].getFileSize(), tag, "SRM-CLIENT:");
      }
      if(fileStatus[i].getRemainingPinLifetime() != null) {
	  SRMClientN.logMsg("RemainingPinTime="+fileStatus[i].getRemainingPinLifetime(), tag, "SRM-CLIENT:");
      }
      if(fileStatus[i].getRemainingFileLifetime() != null) {
	  SRMClientN.logMsg("RemainingFileLifeTime="+fileStatus[i].getRemainingFileLifetime(), tag, "SRM-CLIENT:");
      }
      if(fileStatus[i].getEstimatedWaitTime() != null) {
	  SRMClientN.logMsg("EstimatedWaitTime="+fileStatus[i].getEstimatedWaitTime(), tag, "SRM-CLIENT:");
      }
      if(fileStatus[i].getTransferProtocolInfo() != null) {
          ArrayOfTExtraInfo a_extraInfos = fileStatus[i].getTransferProtocolInfo();
          TExtraInfo[] eInfos = a_extraInfos.getExtraInfoArray();
          if(eInfos != null) {
	      for(int j = 0; j < eInfos.length; j++) {
		  TExtraInfo eInfo = eInfos[j];
		  SRMClientN.logMsg("\tKey="+eInfo.getKey(), tag, "SRM-CLIENT:");
		  SRMClientN.logMsg("\tValue="+eInfo.getValue(), tag, "SRM-CLIENT:");
	      }
          }
      }
      SRMClientN.logMsg("\tStatus="+ fileStatus[i].getStatus().getStatusCode(), tag, "SRM-CLIENT:");
      SRMClientN.logMsg("\tStatus="+ fileStatus[i].getStatus().getExplanation(), tag, "SRM-CLIENT:");
   }
   fileStatus = null;
   result = null;
   return util.mapStatusCode(sCode);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkPutStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TPutRequestFileStatus[] checkPutStatus
    (Vector keySURLS, String response, StringBuffer rCode,
		IsWholeRequestFailed wholeRequestFailed)
        throws URI.MalformedURIException, java.rmi.RemoteException {

   URI[] uris = new URI[keySURLS.size()];
   for(int i = 0; i < keySURLS.size();i++) {
     uris[i] = (URI) keySURLS.elementAt(i);
   }


   Date statusDate = new Date();
   util.printMessage("SRM-CLIENT: " + statusDate + 
		" Calling Status at " + response + " " + new Date(),logger,silent);
   util.printMessageHL("SRM-CLIENT: " + statusDate + 
		" Calling Status at " + response + " " + new Date(),pIntf);

     /*
   if(requestDate != null) {
     util.printMessage("SRM-CLIENT: Elapsed time from request is " +
		util.getElapsedTime(requestDate,statusDate) + " seconds.", 
			logger,silent);
     util.printMessage("SRM-CLIENT: Elapsed time from request is " +
		util.getElapsedTime(requestDate,statusDate) + " seconds.", pIntf);
   }
     */

   if(_debug) {
     inputVec.clear();
     inputVec.addElement("RID="+response);
     util.printMessage("SRM-CLIENT: ::::::::::::: Input parameters for SrmStatusOfPutRequest ::::", logger,silent);
     util.printMessage("SRM-CLIENT: ...Input parameters for SrmStatusOfPutRequest...", pIntf);
     util.printMessage("SRM-CLIENT: RID="+response,logger,silent);
     util.printMessage("SRM-CLIENT: RID="+response,pIntf);
     util.printMessage("SRM-CLIENT: uid="+uid,logger,silent);
     util.printMessage("SRM-CLIENT: uid="+uid,pIntf);
     for(int i = 0; i < keySURLS.size(); i++) {
      util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),logger,silent);
      util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),pIntf);
      inputVec.addElement("SourceSURL("+i+")="+(URI)keySURLS.elementAt(i));
     }
     util.printEventLog(_theLogger,"Input parameters for SrmStatusOfPutRequest",inputVec,silent,useLog);
   }

   if(response == null) {
     inputVec.clear();
     inputVec.addElement("SRM return null request token");
     util.printMessage("\nSRM-CLIENT: " + 
			"SRM return null request token", logger,silent);
     util.printMessage("\nSRM-CLIENT: " + "SRM return null request token", pIntf);
     util.printEventLog(_theLogger,"SrmStatusOfPutResponse",inputVec,silent,useLog);
     return null;
   }

   SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   r.setArrayOfTargetSURLs(SRMUtilClient.convertToArrayOfAnyURI(uris));
   r.setRequestToken(response);
   inputVec.clear();
   inputVec.addElement("Status Calling at " + new Date());
   util.printEventLog(_theLogger,"SrmStatusOfPutResquest",inputVec,silent,useLog);


   SrmStatusOfPutRequestResponse result = null;


   try {
    result = (SrmStatusOfPutRequestResponse) 
         callSoapThread(r,result,"srmstatusofput");
   }catch(Exception e) {
      System.out.println("Exception="+e.getMessage());
      util.printEventLogException(_theLogger,"srmStatusOfPut",e);
   }

   if(result == null) {
     inputVec.clear();
     inputVec.addElement("SRM returned null result");
     util.printEventLog(_theLogger,"SrmStatusOfPutResponse",
                inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: SRM returned null result" , 
                logger, silent);
     util.printMessage("SRM-CLIENT: SRM returned null result" , pIntf);
     return null;
   }

   rStatus = result.getReturnStatus();

   if(rStatus == null) {
     inputVec.clear();
     inputVec.addElement("SRM returned null return status");
     util.printEventLog(_theLogger,"SrmStatusOfPutResponse",
                inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: SRM returned null return status" , 
                logger, silent);
     util.printMessage("SRM-CLIENT: SRM returned null return status" , pIntf);
     return null;
   }

   String explanation = result.getReturnStatus().getExplanation();

   if(_debug) {
     util.printMessage("\nSRM-CLIENT: ### Output from SRM ###",logger,silent);
     util.printMessage("\nSRM-CLIENT: ### Output from SRM ###",pIntf);
   }
     inputVec.clear();
     inputVec.addElement("Result Status from SRM (srmStatusOfPutRequest)=" +
           result.getReturnStatus().getStatusCode());
     util.printMessage("SRM-CLIENT: " + "Result Status from SRM (srmStatusOfPutRequest)=" + result.getReturnStatus().getStatusCode(), logger, silent);
     util.printMessage("SRM-CLIENT: " + "Result Status from SRM (srmStatusOfPutRequest)=" + result.getReturnStatus().getStatusCode(), pIntf);
     if(explanation != null) {
       util.printMessage("\tSRM-CLIENT: " + 
			"Check Status Explanation=" + explanation, logger,silent);
       util.printMessage("\tSRM-CLIENT: " + 
			"Check Status Explanation=" + explanation, pIntf);
       inputVec.addElement("\tCheck Status Explanation=" + explanation);
     }
     util.printEventLog(_theLogger,"SrmStatusOfPutResponse",inputVec,silent,useLog);
   if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE ||
		result.getReturnStatus().getStatusCode() == TStatusCode.SRM_ABORTED) {
     wholeRequestFailed.setWholeRequestFailed(true);
     wholeRequestFailed.setStatus(result.getReturnStatus().getStatusCode());
     wholeRequestFailed.setExplanation(result.getReturnStatus().getExplanation());
   }

   rCode.append(result.getReturnStatus().getStatusCode().toString());

   TPutRequestFileStatus[] fileStatus = null;

   if(result.getArrayOfFileStatuses() != null) {
       fileStatus = result.getArrayOfFileStatuses().getStatusArray();
   }

   if(fileStatus != null) {
     for(int kk = 0; kk < fileStatus.length; kk++) {
       TPutRequestFileStatus fStatus = fileStatus[kk];
       //if(firstTimeStatusCall) {
         //if(fStatus.getStatus().getStatusCode() == 
			//TStatusCode.SRM_SPACE_AVAILABLE) {
              //spaceAvailableFiles++;
         //}
       //}
       inputVec.clear();
       inputVec.addElement("SURL="+fStatus.getSURL().toString());
       inputVec.addElement("Status="+fStatus.getStatus().getStatusCode());
       util.printEventLog(_theLogger,"SrmStatusOfPutResponse",
			inputVec,silent,useLog);
       //oResult.put(fStatus.getSURL().toString(), fStatus);
       if(fStatus.getEstimatedWaitTime() != null) {
          Integer xx = fStatus.getEstimatedWaitTime ();
          int yy = xx.intValue();
          if(yy != -1) {
            if(_debug) {
               System.out.println("\nSRM-CLIENT: EstimatedWaitTime " +
                  "given by server is " + yy + " seconds.");
            }
            if(statusWaitTime > yy*1000) {
              statusWaitTime = yy*1000;
            }
          }
       }
     }

     //if(firstTimeStatusCall) {
       //firstTimeStatusCall=false;
     //}
   }

   statusWaitTime = statusWaitTime*2;
   if(statusWaitTime >= threshHoldValue*1000) {
     //statusWaitTime = 30*1000; //resetting back to 30 seconds
     statusWaitTime = threshHoldValue*1000; 
        //resetting back to threshHoldValue seconds
   }
   if(statusWaitTime >= statusMaxTimeAllowed*1000) {
      if(_debug) {
       System.out.println("\nSRM-CLIENT: Next status call is " +
            "adjusted according to totalrequesttime given by the user");
     }
     statusWaitTime = statusWaitTime - 5000;
   }
   return fileStatus;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// reserveSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public String reserveSpace(String strUID) throws Exception {
 
  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  TStatusCode sCode = null;
  StringBuffer statusBuf = new StringBuffer();
  return SRMUtilClient.reserveSpace(_srm,strUID,userDesc,retentionPolicyInfo,
	  tokenSize, tokenLifetime,guarnSize,new StringBuffer(),
	  logger,_debug,false,sCode,statusBuf,_credential,proxyType,
          serverUrl,_theLogger, pIntf, _srmClientIntf, silent, useLog, 
          connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
          delegationNeeded, numRetry, retryTimeOut);
}
	
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

/*
private void updateToken(TSpaceToken token, 
  long gSpaceInMB, long tSpaceInMB, long lifetime)  throws Exception 
{
   if(_debug) {
     util.printMessage("....................................",logger,silent);
     util.printMessage("updating token for="+token,logger,silent);
   }
			
   SrmUpdateSpaceRequest r1 = new SrmUpdateSpaceRequest();
   r1.setSpaceToken(token);  
   TSizeInBytes newSizeG = new TSizeInBytes();
   long g = gSpaceInMB*(long)1048576;
   newSizeG.setValue(g);
   r1.setNewSizeOfGuaranteedSpaceDesired(newSizeG);
			
   TSizeInBytes newSizeT = new TSizeInBytes();
   long t = tSpaceInMB*(long)1048576;;
   newSizeT.setValue(t);
   r1.setNewSizeOfTotalSpaceDesired(newSizeT);
			
   TLifeTimeInSeconds spaceLifetime= new TLifeTimeInSeconds();
   spaceLifetime.setValue(lifetime);
   r1.setNewLifeTimeFromCallingTime(spaceLifetime);

   if(!uid.equals("")) {
     r1.setAuthorizationID(uid);
   }
			
   if(_debug) {
     util.printMessage("Total="+newSizeT.getValue(),logger,silent);
     util.printMessage("Min="+newSizeG.getValue(),logger,silent);
     util.printMessage("LifeTime="+spaceLifetime.getValue(),logger,silent);
   }

   SrmUpdateSpaceResponse result1 = null;

   result1 = (SrmUpdateSpaceResponse) 
                callSoapThread(r1,result1,"srmupdatespace");
			
   if (result1 != null) {
     if(_debug) {
       util.printMessage(".............................",logger,silent);
       util.printMessage("\tstatus="+ 
	     result1.getReturnStatus().getStatusCode(),logger,silent);

       if (result1.getLifetimeGranted() != null) {
	     util.printMessage("\tlifetime="+
  	        result1.getLifetimeGranted().getValue(),logger,silent);
       }

       if (result1.getSizeOfGuaranteedSpace() != null) {
          util.printMessage("\tMin="+
	        result1.getSizeOfGuaranteedSpace().getValue(),logger,silent);
       }

       if (result1.getSizeOfTotalSpace() != null) {
	      util.printMessage("\tMax="+
   	        result1.getSizeOfTotalSpace().getValue(),logger,silent);
       }

       util.printMessage("\texplanation="+
	        result1.getReturnStatus().getExplanation(),logger,silent);
      }
   }
}
*/

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSecurityPropertiesForcefully
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSecurityPropertiesForcefully(URL endpoint) {
  if (endpoint.getProtocol().equals("httpg") ||
        _srm instanceof org.apache.axis.client.Stub) {
        if(_debug) {
          util.printMessage("\nSRM-CLIENT: ProxyType found " + proxyType,
            logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("ProxyType="+proxyType);
        util.printEventLog(_theLogger,"SetSecurityPropertiesForceFully",
                inputVec,silent,useLog);
        //GSIUtils.setDelegationGSIProperties(_stub, endpoint);
        org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) _srm;
        //needed this line to pass the given proxy to srm_copy
        srm_stub._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,_credential);
        srm_stub._setProperty(
            org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
            org.globus.gsi.gssapi.auth.NoAuthorization.getInstance());
        //proxyType == 10 is full (old type)
        //proxyType == 14 is full
        //proxyType == 11 is limited
        //proxyType == 15 is pre-RFC
        if(proxyType == 14 || proxyType == 10) {
          if(_debug) {
            util.printMessage("\nSRM-CLIENT: full delegation by client",
                logger,silent);
          }
          srm_stub._setProperty
            (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
             org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
        }
        else {
          if(_debug) {
            util.printMessage("\nSRM-CLIENT: limited delegation by client",
                logger,silent);
          }
          srm_stub._setProperty
           (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
            org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_LIMITED_DELEG);
        }
        srm_stub._setProperty
        (org.globus.gsi.GSIConstants.AUTHZ_REQUIRED_WITH_DELEGATION,
           Boolean.FALSE);
    } else {
        //_context.setAuthentication(_stub);
    }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSecurityProperties
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSecurityProperties(URL endpoint) {
  if (endpoint.getProtocol().equals("httpg") ||
        _srm instanceof org.apache.axis.client.Stub) {
        if(_debug) {
          util.printMessage("\nSRM-CLIENT: ProxyType found " + proxyType,
            logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("ProxyType="+proxyType);
        util.printEventLog(_theLogger,"SetSecurityProperties",
                inputVec,silent,useLog);
        org.apache.axis.client.Stub srm_stub =
             (org.apache.axis.client.Stub)_srm;
        //GSIUtils.setDelegationGSIProperties(_stub, endpoint);
        srm_stub._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,_credential);
        srm_stub._setProperty(
            org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
            org.globus.gsi.gssapi.auth.NoAuthorization.getInstance());
        //proxyType == 10 is full (old type)
        //proxyType == 14 is full
        //proxyType == 11 is limited
        //proxyType == 15 is pre-RFC
        if(proxyType == 14 || proxyType == 10) {
          if(_debug) {
            util.printMessage("\nSRM-CLIENT: full delegation by system",
                logger,silent);
          }
          srm_stub._setProperty
           (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
            org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
        }
        else {
          if(_debug) {
            util.printMessage("\nSRM-CLIENT: limited delegation by system",
                logger,silent);
          }
          srm_stub._setProperty
           (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
            org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_LIMITED_DELEG);
        }
        srm_stub._setProperty
        (org.globus.gsi.GSIConstants.AUTHZ_REQUIRED_WITH_DELEGATION,
           Boolean.FALSE);
    } else {
        //_context.setAuthentication(_stub);
    }
}
	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// useCredential
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void useCredential(GSSCredential cred) {
  _stub._setProperty(GSIConstants.GSI_CREDENTIALS, cred);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// IsWholeRequestFailed
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class IsWholeRequestFailed {
  boolean wholeRequestFailed=false;
  TStatusCode status;
  String explanation;

  public void setStatus(TStatusCode status) {
    this.status = status;
  }

  public void setExplanation(String explanation) {
    this.explanation = explanation;
  }

  public void setWholeRequestFailed(boolean b) {
     wholeRequestFailed=b;
  }

  public boolean getWholeRequestFailed() {
     return wholeRequestFailed;
  }

  public TStatusCode getStatus() {
     return status;
  }

  public String getExplanation() {
     return explanation;
  }
}
	
}
