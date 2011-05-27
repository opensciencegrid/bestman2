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

package gov.lbl.srm.client.wsdl;
 
import gov.lbl.srm.StorageResourceManager.*;
//import gov.lbl.srm.util.*;
import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.main.*;
import gov.lbl.srm.client.util.MyGlobusURL;
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
    }
    else if(delegationNeeded.equals("true")) {
      setSecurityPropertiesForcefully(uu);
    }
    else if(delegationNeeded.equals("")) {
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
    if(skip) {
      inputVec.addElement("SetRequestDone is set with true and false");
      util.printEventLog(_theLogger,"performTransfer.put", inputVec,silent,useLog);
      _srmClientIntf.setRequestDone(true,false);
      inputVec.clear();
      return;
    }
    else {
     try {
      srmPut(uid, fileInfo);
     }catch(Exception e) {
       String msg = e.getMessage();
       util.printEventLogException(_theLogger,"PerformTranfer",e);
       int idx = msg.indexOf("Connection refused");
       int idx1 = msg.indexOf("Defective credential detected");
       int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
       int idx6 = msg.indexOf(
			"java.net.SocketTimeoutException: Read timed out");
       util.printHException(e,pIntf);
       if(pIntf != null) {
        pIntf.setCompleted(false);
       }
       if(msg.startsWith("CGSI-gSOAP: Could not find mapping") 
			||  idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
         inputVec.addElement("Exception="+msg);
         util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
         util.printMessage("\nSRM-CLIENT: Exception : "+msg,logger,silent);
         util.printMessageHException("\nSRM-CLIENT: Exception : "+msg,pIntf);
         if(pIntf == null) {
          if(idx != -1 || idx6 !=-1 ) {
            inputVec.clear();
            inputVec.add("ExitCode="+90);
            util.printEventLog(_theLogger,"ExitCodeStatus", inputVec,silent,useLog);
            util.printHException(e,pIntf);
            System.exit(90);
          }
          else if(idx1 != -1 || idx5 != -1) {
            util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy type ", logger,silent);
            inputVec.clear();
            inputVec.add("ExitCode="+96);
            util.printEventLog(_theLogger,"ExitCodeStatus", inputVec,silent,useLog);
            util.printHException(e,pIntf);
            System.exit(96);
          }
          else {
            inputVec.clear();
            inputVec.add("ExitCode="+91);
            util.printEventLog(_theLogger,"ExitCodeStatus", inputVec,silent,useLog);
            util.printHException(e,pIntf);
            System.exit(91);
          } 
         }
       }
       else {
         throw e;
       }
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
  util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmStatusOfPutRequest ...", logger,silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmStatusOfPutRequest ...", pIntf);
  //util.printMessage("TimeStamp="+new Date(), logger,silent);
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmStatusOfPutRequest",inputVec,silent,useLog);
  int exitCode = doStatus(uid, fileInfo, statusToken);
  if(pIntf == null) {
    inputVec.clear();
    inputVec.add("ExitCode="+exitCode);
    util.printEventLog(_theLogger,"ExitCodeStatus", inputVec,silent,useLog);
    System.exit(exitCode);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseSpace() throws Exception {
  if(gateWayModeEnabled) return;

  if(fileToken != null) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling releaseSpace for FileToken "+ fileToken, logger,silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling releaseSpace for FileToken "+ fileToken, pIntf);
    //util.printMessage("TimeStamp="+new Date(), logger,silent);
    //util.printMessage("\tFileToken="+fileToken,logger,silent);
    inputVec.clear();
    inputVec.addElement("TimeStamp="+new Date());
    inputVec.addElement("FileToken="+fileToken);
    util.printEventLog(_theLogger,"DoReleaseSpace",inputVec,silent,useLog);
    releaseSpace(fileToken,true);  
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles (String turl, String rid, int label) throws Exception {

  inputVec.clear();
  inputVec.addElement("SURL="+turl);
  inputVec.addElement("token="+rid);
  inputVec.addElement("label="+label);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmPutClient.abortFiles",inputVec,silent,useLog);

  if(gateWayModeEnabled) return; 

  if(rid == null || rid.equals("")) {
     util.printMessage("\nSRM-CLIENT: Cannot abort files with null rid " +rid, 
			logger,silent);
     util.printMessage("\nSRM-CLIENT: Cannot abort files with null rid " +rid, 
			pIntf);
     return;
  }

  if(noAbortFile) {
    util.printMessage("\nSRM-CLIENT: Skipping abort files as per user request", logger,silent);
    util.printMessage("\nSRM-CLIENT: Skipping abort files as per user request", pIntf);
    return;
  }

  util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling abortFiles", logger,silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling abortFiles", pIntf);
  //util.printMessage("TimeStamp="+new Date(), logger,silent);
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  inputVec.addElement("Token="+rid);
  inputVec.addElement("Uid="+uid);
  inputVec.addElement("SURL="+turl);
  if(_debug) {
    util.printMessage("SRM-CLIENT: ::::::::::::: Input parameters for SrmAbortFiles :::::", logger,silent);
    util.printMessage("SRM-CLIENT: surl="+turl,logger,silent);
    util.printMessage("SRM-CLIENT: token="+rid,logger,silent);
    util.printMessage("SRM-CLIENT: uid="+uid,logger,silent);
    util.printMessage("SRM-CLIENT: ...Input parameters for SrmAbortFiles...", 
		pIntf);
    util.printMessage("SRM-CLIENT: surl="+turl,pIntf);
    util.printMessage("SRM-CLIENT: token="+rid,pIntf);
    util.printMessage("SRM-CLIENT: uid="+uid,pIntf);
  }
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

    result = (SrmAbortFilesResponse) 
                callSoapThread(req,result,"srmabortfiles");

    util.printMessage("\nSRM-CLIENT: ### Output of SRM ###",logger,silent);
    util.printMessage("\nSRM-CLIENT: ### Output of SRM ###",pIntf);
    if(result == null) {
      inputVec.clear();
      inputVec.addElement("Null result from SrmAbortFiles");
      util.printEventLog(_theLogger,"Response from SrmAbortFiles",inputVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
			" Null result from SrmAbortFiles", logger,silent);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
			" Null result from SrmAbortFiles", pIntf);
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(label); 
      String temp = fIntf.getFileExplanation();
      if(temp.equals("")) {
        fIntf.setFileExplanation("SRM-CLIENT: Failed during srmAbortFiles");
      }
      else {
        fIntf.setFileExplanation(temp+ ", Failed during srmAbortFiles");
      }
      return;
    }

    if(result.getReturnStatus() != null) {
     if(result.getReturnStatus().getStatusCode() == 
                      TStatusCode.SRM_NOT_SUPPORTED) { 
        util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
            logger,silent);
        util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
            pIntf);
        util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), logger,silent);
        util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), pIntf);
        return;
     }
     else {
       if(_debug) {
        util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
            logger,silent);
        util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
            pIntf);
        util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), logger,silent);
        util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), pIntf);
       } 
     } 
    }
    else {
      util.printMessage("\nSRM-CLIENT: Null return status from abortFiles",
			logger,silent);
      util.printMessage("\nSRM-CLIENT: Null return status from abortFiles",
			pIntf);
      return;
    }

    if(result.getArrayOfFileStatuses() == null) {
      inputVec.clear();
      inputVec.addElement("Null ArrayOfFileStatuses from SrmAbortFiles");
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Null ArrayOfFileStatuses from SrmAbortFiles", logger,silent);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Null ArrayOfFileStatuses from SrmAbortFiles", pIntf);
      util.printEventLog(_theLogger,"Response from SrmAbortFiles",inputVec,silent,useLog);
      return;
    }

    TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);

    if(status == null) {
     inputVec.clear();
     inputVec.addElement("Null TSURLReturnStatus from SrmAbortFiles");
     util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Null TSURLReturnStatus from SrmAbortFiles", logger,silent);
     util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Null TSURLReturnStatus from SrmAbortFiles", pIntf);
     util.printEventLog(_theLogger,"Response from SrmAbortFiles",inputVec,silent,useLog);
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
         }
         else {
           fInfo.setFileExplanation("SRM-CLIENT: AbortFiles is called successfully");
         }
      }
    }

    if(_debug) {
    util.printMessage("\tsurl="+status.getSurl().toString(),logger,silent);
    util.printMessage("\tstatus="+status.getStatus().getStatusCode(),
            logger,silent);
    util.printMessage("\texplanation="+status.getStatus().getExplanation(),
            logger,silent);
    util.printMessage("\tsurl="+status.getSurl().toString(),pIntf);
    util.printMessage("\tstatus="+status.getStatus().getStatusCode(),
            pIntf);
    util.printMessage("\texplanation="+status.getStatus().getExplanation(),
            pIntf);
    }
    inputVec.clear();
    inputVec.addElement("surl="+status.getSurl().toString());
    inputVec.addElement("status="+status.getStatus().getStatusCode());
    inputVec.addElement("explanation="+status.getStatus().getExplanation());
    util.printEventLog(_theLogger,"Response from SrmAbortFiles",inputVec,silent,useLog);
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

  inputVec.clear();
  inputVec.addElement("SURL="+siteUrl);
  inputVec.addElement("token="+rid);
  inputVec.addElement("label="+label);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmPutClient.putDone", inputVec,silent,useLog);

  if(gateWayModeEnabled) return;

  if(_debug) {
    util.printMessage
	  ("SRM-CLIENT: ::::::::::::: Input parameters for srmPutDone :::::", 
		logger,silent);
    util.printMessage("SRM-CLIENT: ...Input parameters for srmPutDone...", 
		pIntf);
    util.printMessage("SRM-CLIENT: surl="+siteUrl,logger,silent);
    util.printMessage("SRM-CLIENT: token="+rid,logger,silent);
    util.printMessage("SRM-CLIENT: uid="+uid,logger,silent);
    util.printMessage("SRM-CLIENT: surl="+siteUrl,pIntf);
    util.printMessage("SRM-CLIENT: token="+rid,pIntf);
    util.printMessage("SRM-CLIENT: uid="+uid,pIntf);
  }

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);

  SrmPutDoneRequest req = new SrmPutDoneRequest();
  req.setArrayOfSURLs(SRMUtilClient.formArrayOfAnyURI(uri));
  req.setRequestToken(rid);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
  }

  inputVec.clear();
  try {

    SrmPutDoneResponse result = null;

    result = (SrmPutDoneResponse) 
                callSoapThread(req,result,"srmputdone");


    boolean resultNotNull = true;
    String checkUrl = siteUrl;

    TSURLReturnStatus status = null;
    TStatusCode temp = null;

    if(result == null) {
      inputVec.addElement("SRM returned null result for the srmPutDone request");
      util.printEventLog(_theLogger,"Response from srmPutDone", 
	inputVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
	" SRM returned null result for the srmPutDone request", logger,silent);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
	" SRM returned null result for the srmPutDone request", pIntf);
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(label); 
      fIntf.setFileExplanation("SRM-CLIENT: Failed during putDone");
      fIntf.setPutDoneFailed(true);
      return;
    }

    if(result.getReturnStatus() == null) {
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
	" SRM returned null returnstatus for the srmPutDone request", 
                logger,silent);
      util.printMessage("\nSRM-CLIENT: " + new Date() +
	" SRM returned null returnstatus for the srmPutDone request", pIntf);
      return;
    }

    util.printMessage("Result.status="+result.getReturnStatus().getStatusCode(),logger,silent);
    util.printMessage("Result.status="+result.getReturnStatus().getStatusCode(),pIntf);
    util.printMessage("Result.Explanation="+result.getReturnStatus().getExplanation(),logger,silent);
    util.printMessage("Result.Explanation="+result.getReturnStatus().getExplanation(),pIntf);

    if(result.getArrayOfFileStatuses() == null) {
       inputVec.clear();
       util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" SRM returned null getArrayOfFileStatus for srmPutDone request",
          logger,silent);
       util.printMessage("\nSRM-CLIENT: " + new Date() + 
	" SRM returned null getArrayOfFileStatus for srmPutDone request", pIntf);
       inputVec.addElement
        ("SRM returned null getArrayOfFileStatus for srmPutDone request");
       util.printEventLog
        (_theLogger,"Response from srmPutDone", inputVec,silent,useLog);
       return;
    }


    status = result.getArrayOfFileStatuses().getStatusArray(0);
    temp = status.getStatus().getStatusCode();
    checkUrl = status.getSurl().toString();
    if(_debug) {
      util.printMessage("SRM-CLIENT: " + "### Output of SRM ### ", logger,silent);
      util.printMessage("SRM-CLIENT: " + "### Output of SRM ### ", pIntf);
    }
    inputVec.clear();
    inputVec.addElement("SURL="+checkUrl);
    inputVec.addElement("status="+temp);
    inputVec.addElement("explanation="+status.getStatus().getExplanation());
    util.printEventLog(_theLogger,"SrmPutDoneResponse",inputVec,silent,useLog);

    Object[] objArray = fileInfo.toArray();
    for(int i = 0; i < objArray.length; i++) {
      FileInfo fInfo = (FileInfo) objArray[i];
      if(fInfo.getOrigTURL().equals(checkUrl)) {
         if(status != null && status.getStatus().getExplanation() != null) {
           fInfo.setFileExplanation(status.getStatus().getExplanation());
         }
         else {
           fInfo.setFileExplanation("SRM-CLIENT: PutDone is called successfully");
         }
      }
    }

    if(_debug) {
      util.printMessage("\tsurl="+status.getSurl().toString(),logger,silent);
      util.printMessage("\tstatus="+temp,logger,silent);
      util.printMessage("\texplanation="+status.getStatus().getExplanation(),
		logger,silent);
      util.printMessage("\tsurl="+status.getSurl().toString(),pIntf);
      util.printMessage("\tstatus="+temp,pIntf);
      util.printMessage("\texplanation="+status.getStatus().getExplanation(), 
		pIntf);
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
            util.printMessage("SRM-CLIENT: Doing next status call in " + 
				tStatusTime + " seconds", logger,silent); 
            util.printMessage("SRM-CLIENT: Doing next status call in " + 
				tStatusTime + " seconds", pIntf); 
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
               }
               else {
                   tempCode = f_status.getStatus().getStatusCode();
                   rCode.append(f_status.getStatus().getExplanation());
                   if(tempCode == TStatusCode.SRM_FILE_IN_CACHE ||
                      tempCode == TStatusCode.SRM_SPACE_AVAILABLE) { 
                        //System.out.println(">>>>TEMPCODE.PutDone="+tempCode);
                        ; //continue checking the status 
	                      //until SRM_SUCCESS or any error is returned
                   } 
                   else {
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
       util.printMessage("\tsurl="+status.getSurl().toString(),logger,silent);
       util.printMessage("\tstatus="+tempCode,logger,silent);
       util.printMessage("\texplanation="+rCode.toString(),logger,silent);
       util.printMessage("\tsurl="+status.getSurl().toString(),pIntf);
       util.printMessage("\tstatus="+tempCode,pIntf);
       util.printMessage("\texplanation="+rCode.toString(), pIntf);
     }
     else {
       util.printMessage("\tsurl="+status.getSurl().toString(),logger,silent);
       util.printMessage("\tstatus="+temp,logger,silent);
       util.printMessage("\texplanation="+status.getStatus().getExplanation(),
			logger,silent);
       util.printMessage("\tsurl="+status.getSurl().toString(),pIntf);
       util.printMessage("\tstatus="+temp,pIntf);
       util.printMessage("\texplanation="+status.getStatus().getExplanation(), pIntf);
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

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void srmPut(String uid, Vector fileInfo) throws Exception {

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
    }
    else if(totalSize > maxSize) {
      modSize = (totalSize % maxSize);
      int subSize = totalSize/maxSize;
      if(modSize > 0) {
        totalSubRequest = subSize + 1;
      }
      else {
        totalSubRequest = subSize;
      }
    }
    else if(totalSize <= maxSize) {
      totalSubRequest = 1;
      maxSize = totalSize;
    }

    if(!thirdparty) {
      _srmClientIntf.setTotalSubRequest(totalSubRequest);
      _srmClientIntf.setTotalFilesPerRequest(maxSize);
    }
    else {
      _srmClientIntf.setTotalSubRequest(tFiles);
      _srmClientIntf.setTotalFilesPerRequest(maxSize);
      //_srmClientIntf.setTotalFilesPerRequest(1);
    }

    int count = 0;
    int index = 0;

    inputVec.clear();

    if(totalSize > 1) {
      inputVec.addElement("Total number of subrequests="+totalSubRequest);
      util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
			" Total number of subrequests=" + totalSubRequest,logger,silent);
      util.printMessage("\nSRM-CLIENT: " + new Date() + 
			" Total number of subrequests=" + totalSubRequest,pIntf);
    }

    if(_debug) {
      util.printMessage("SRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::::", logger,silent);
      util.printMessage("SRM-CLIENT: :::: Input parameters for SrmPrepareToPutRequest :::::::", logger,silent);
      util.printMessage("SRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::::", pIntf);
      util.printMessage("SRM-CLIENT: ...Input parameters for SrmPrepareToPutRequest...", pIntf);
    }

    while (count < totalSubRequest) {

      if(count == (totalSubRequest -1)) {
        if(modSize > 0) {
          maxSize = modSize;    
        }
      }

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
           util.printMessage("SRM-CLIENT: From URL ("+sindex+")="+
		      fInfo.getOrigSURL(),logger,silent);
           util.printMessage("SRM-CLIENT: From URL ("+sindex+")="+
		      fInfo.getOrigSURL(),pIntf);
           inputVec.addElement("From URL("+sindex+")="+fInfo.getOrigSURL());
           util.printMessage("SRM-CLIENT: To URL ("+sindex+")=" + 
		      surl[sindex], logger,silent);
           util.printMessage("SRM-CLIENT: To URL ("+sindex+")=" + 
		      surl[sindex], pIntf);
           inputVec.addElement("To URL("+sindex+")="+surl[sindex]);
           util.printMessage("SRM-CLIENT: FileSize ("+sindex+")=" + 
		      fSize[sindex], logger,silent);
           util.printMessage("SRM-CLIENT: FileSize ("+sindex+")=" + 
		      fSize[sindex], pIntf);
           inputVec.addElement("FileSize("+sindex+")="+fSize[sindex]);
           util.printEventLog(_theLogger,"Input parameters for SrmPut",inputVec,silent,useLog);
         }
         sindex++;
      }

      if (doReserveSpace) {
        fileToken = reserveSpace(uid);
        if(fileToken == null) {
           inputVec.clear();
           inputVec.addElement("SRM returned null space token");
           util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
           util.printMessage
            ("\nSRM-CLIENT: " + new Date() + 
				" SRM returned null space token", logger,silent);
           util.printMessage
            ("\nSRM-CLIENT: " + new Date() + 
				" SRM returned null space token", pIntf);
         if(pIntf == null) {
           inputVec.clear();
           inputVec.add("ExitCode="+1000);
           util.printEventLog(_theLogger,"ExitCodeStatus", inputVec,silent,useLog);
           System.exit(100);//srm returned no status
         }
        }
      }

      SrmPrepareToPutResponse response =
         prepareToPutMultiFiles(surl, fSize, fileToken);

      StringBuffer responseBuffer = new StringBuffer();
      HashMap subStatusArray = printPutResult(response,responseBuffer,oResult);

      //added for timing out things for 3partycopy, single p2p at a time 
      //srm(full)->srm
      if(thirdparty && response == null) {
        for(int i =  index+0; i < index+maxSize; i++) {
           FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
           _srmClientIntf.srmFileFailure(fInfo.getLabel(),
		"Put Request Timed Out, server may be busy");
        }
      }

      if(response == null || 
	  subStatusArray == null || subStatusArray.size() == 0) {
        inputVec.clear();
        if(!responseBuffer.toString().equals("")) {
          util.printMessage("\tSRM-CLIENT: " + 
			"Explanation from responseBuffer="+responseBuffer.toString(), 
			logger,silent);
          util.printMessage("\tSRM-CLIENT: " + 
		"Explanation from responseBuffer="+responseBuffer.toString(),pIntf);
          inputVec.addElement("Explanation from responseBuffer="+
		responseBuffer.toString());
          util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
          responseBuffer = null;
        }
      }
      else {
        statusArray.put(response.getRequestToken(),subStatusArray);
      }
      if(response != null) {
       _srmClientIntf.addRequestToken(response.getRequestToken());
     }
     else {
       inputVec.clear();
       util.printMessage("\nSRM-CLIENT: Null response from SRM " +
	 "for this sub request", logger, silent);
       util.printMessage("\nSRM-CLIENT: Null response from SRM " +
	 "for this sub request", pIntf);
       inputVec.addElement("Null response from SRM for this sub request");
       util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
     }

     totalFiles = totalFiles + subStatusArray.size();

     if(thirdparty) {
        _srmClientIntf.setTotalFiles(tFiles);
     }
     else {
       _srmClientIntf.setTotalFiles(totalFiles);
     }

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
         TPutRequestFileStatus status =
            (TPutRequestFileStatus) (subStatusArray.get(oSubKey));
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
               _srmClientIntf.srmFileFailure(ffInfo.getLabel(),
                 "File Status TimedOut.");
                break;
              }
             }
             numFailedFiles++;
             subStatusArray.remove(oSubKey);
             /*
             util.printMessage("\nSRM-CLIENT: Calling SrmAbortRequest",
                logger,silent);
             SRMUtilClient utilClient = new SRMUtilClient
               (serverUrl,uid,userDesc, _credential, _theLogger, logger,
                pIntf, _debug,silent,useLog,false, false,
                statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
                connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		        delegationNeeded,numRetry,retryTimeOut);
             TStatusCode abortRequestCode =
                utilClient.doSrmAbortRequest(requestToken);
             util.printMessage("\nSRM-CLIENT: AbortStatusCode="+
                abortRequestCode.getValue(),logger,silent);
             */
         }
         else {
           if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
              (code == TStatusCode.SRM_REQUEST_QUEUED)) {

              //System.out.println(">>added here");
              keySURLS.addElement(new URI(subKey));
           }
           else if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
                    (code != TStatusCode.SRM_REQUEST_QUEUED)) { 

              //added file_in_cache for cern v2 server
              if(code == TStatusCode.SRM_SPACE_AVAILABLE ||
                 code == TStatusCode.SRM_SUCCESS ||
                 code == TStatusCode.SRM_DONE ||
                 code == TStatusCode.SRM_FILE_IN_CACHE) {
                //initiate pulling the file
                String gSURL =  subKey;
                Object[] objArray = fileInfo.toArray();
                FileInfo ffInfo = null;
                for(int kk = 0; kk < objArray.length; kk++) {
                  ffInfo = (FileInfo) objArray[kk];
                  //System.out.println(">>>>FFINFO="+ffInfo.getTURL());
                  //System.out.println(">>>>gSURL="+gSURL);
                  if(ffInfo.getTURL().equals(gSURL)) {
                   //System.out.println(">>>Status.getTransferURL()="+
                      //status.getTransferURL());
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
                   }
                   else {
                     keySURLS.addElement(new URI(subKey));
                   } 
                   break;
                 }
                }
              }
              else {  //for other types of failures
                if(code == null) {
                  code = SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);
                }
                TReturnStatus rs = new TReturnStatus();
                if(code != null) {
                  rs.setStatusCode(code);
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
                     _srmClientIntf.srmFileFailure(ffInfo.getLabel(),
                        "File Aborted.");
                    }
                    else {
                     if(status !=null && status.getStatus() != null) {
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

                numFailedFiles ++;
                subStatusArray.remove(oSubKey);
              }//end else
            }//end else if
          }//end else
       }//end for

       if(abortFilesVec.size() > 0) {
             util.printMessage("\nSRM-CLIENT: Calling SrmAbortFiles",
                logger,silent);
             SRMUtilClient utilClient = new SRMUtilClient
               (serverUrl,uid,userDesc, _credential, _theLogger, logger,
                pIntf, _debug,silent,useLog,false, false,
                statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
                connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		        delegationNeeded,numRetry,retryTimeOut);
             TStatusCode abortRequestCode =
                utilClient.doSrmAbortFiles(abortFilesVec,requestToken);
             util.printMessage("\nSRM-CLIENT: AbortStatusCode="+
                abortRequestCode.getValue(),logger,silent);
       }

       keySetArray = subStatusArray.keySet().toArray();

       if(keySetArray.length > 0 && 
		!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
           inputVec.clear();
           inputVec.addElement("Max retry check status exceeded for put status");
           util.printMessage("SRM-CLIENT: " + new Date() + 
		" Max retry check status exceeded for put status", logger,silent);
           util.printMessage("SRM-CLIENT: " + new Date() + 
		" Max retry check status exceeded for put status", pIntf);
           expBuffer.append("Max retry check status exceeded for put status");
           util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
           timedOutCase=true;
       }
       

       if(keySetArray.length > 0 && !timedOutCase) { 
         oSubKey = (String) keySetArray [0];
         TPutRequestFileStatus status =
            (TPutRequestFileStatus) (subStatusArray.get(oSubKey));
         TStatusCode code = status.getStatus().getStatusCode();
           if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
              (code == TStatusCode.SRM_REQUEST_QUEUED)) {
              int tStatusTime = 1;
              if(statusWaitTime >= 1000) {
               tStatusTime = statusWaitTime/1000;
              }

              util.printMessage("SRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",logger,silent);
              util.printMessage("SRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",pIntf);
              inputVec.clear();
              inputVec.addElement("NextStatusCallIn="+tStatusTime+" seconds.");
              util.printEventLog(_theLogger,"NextStatusCall",inputVec,silent,useLog);
            }
       }


        TPutRequestFileStatus[] statuses = null;
        IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
        if(subStatusArray.size() > 0 && !timedOutCase)  {
          Thread.sleep(statusWaitTime);
          if(keySURLS.size() > 0) {
            statuses  = checkPutStatus(keySURLS, subResponse, rCode,	
					wholeRequestFailed);
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
        }
        else {
           for(int kk = 0; kk < statuses.length; kk++) {
            TPutRequestFileStatus status =
              (TPutRequestFileStatus) statuses[kk];
            expBuffer.delete(0,expBuffer.length());
            MyGlobusURL gurl = 
		new MyGlobusURL(status.getSURL().toString(),1);
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
                 if(tempCode == TStatusCode.SRM_SPACE_AVAILABLE ||
                   tempCode == TStatusCode.SRM_SUCCESS ||
                   tempCode == TStatusCode.SRM_DONE ||
                   tempCode == TStatusCode.SRM_FILE_IN_CACHE) {
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
               }
             }
             else {
               tempCode =
                    SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);
             }
             
             if(tempCode != TStatusCode.SRM_REQUEST_QUEUED &&
                tempCode != TStatusCode.SRM_REQUEST_INPROGRESS) {
              inputVec.clear();
              inputVec.addElement("SURL="+status.getSURL());
              inputVec.addElement("FileStatus code from server="+tempCode);
              inputVec.addElement("Explanation from server="+expBuffer.toString());
              if(_debug) {
              util.printMessage("surl="+status.getSURL(),logger,silent);
              util.printMessage("surl="+status.getSURL(),pIntf);
              util.printMessage
                ("SRM-CLIENT: FileStatus code from server="+tempCode,logger,silent);
              util.printMessage
                ("SRM-CLIENT: FileStatus code from server="+tempCode,pIntf);
              util.printMessage
                ("SRM-CLIENT: Explanation from server=" + expBuffer.toString(),logger,silent); 
              util.printMessage
                ("SRM-CLIENT: Explanation from server=" + expBuffer.toString(),
					pIntf); 
              } 
              util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
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
   }
   else {
   if(!callSetDone) {
       if(!thirdparty) {
        inputVec.clear();
        inputVec.addElement
		("SetRequestDone is set with true and false in notCallSetDone");
        util.printEventLog(_theLogger,"srmPut", inputVec,silent,useLog);
        _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
        _srmClientIntf.setRequestDone(true,false);
       }
       else {
         if(_srmClientIntf.getRequestStatus().equals("")) {
           _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
         }
         if(partytype == 1) {
          //when surl is gsiftp, we have to set request done here
           _srmClientIntf.setRequestDone(true,false);
         }
       }
   }
   else {
     if(numFailedFiles == totalFiles) {
        if(!thirdparty) {
         inputVec.clear();
         inputVec.addElement
		("SetRequestDone is set with true and true in callSetDone");
         util.printEventLog(_theLogger,
		"doDirectGsiFTP.put", inputVec,silent,useLog);
         _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
         _srmClientIntf.setRequestDone(true,true);
        }
        else {
         if(_srmClientIntf.getRequestStatus().equals("")) {
           _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
         }
         if(partytype == 1) {
          //when surl is gsiftp, we have to set request done here
           _srmClientIntf.setRequestDone(true,false);
         }
        }
     }
     else  {
        if(!thirdparty) {
         inputVec.clear();
         inputVec.addElement("SetRequestDone is set with true and false " +
		"in callSetDone and numFailedFiles is not equal to totalFiles");
         util.printEventLog(_theLogger,
		"doDirectGsiFTP.put", inputVec,silent,useLog);
         _srmClientIntf.setRequestInformation(resultStatus,resultExplanation);
         _srmClientIntf.setRequestDone(true,false);
        }
        else {
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
              boolean b = utilClient.doSrmMkdir
					(fileInfo,sCode,false,false,true,false);
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

private Object callSoapThread(Object request, 
        Object result, String methodName) throws Exception {

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

       if(retrySoFar > 0) {
         Thread.sleep(retryTimeOut);
       }
        
       inputVec.clear();
       inputVec.addElement("Creating NewCall for " + methodName + 
                " for numRetry="+retrySoFar);
       util.printEventLog(_theLogger,"SRMPutClient.callSoapThread",
                inputVec,silent,useLog);
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
         if((timeOutHappened && responseObject == null) ||
           (!exceptionHappened.equals("") && responseObject == null)) {
           inputVec.clear();
           inputVec.addElement("Interrupting " + methodName);
           inputVec.addElement("timedout and responseobject is null");
           util.printEventLog(_theLogger,"SRMPutClient.callSoapThread",
                inputVec,silent,useLog);
           try {
            _srm=null;
            soapCallThread.setInterrupt(true);
            openSRMConnection();
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMPutClient.callSoapThread",ie);
             System.out.println
                ("SRM-CLIENT: Exception happended while interrupting "+ 
                  methodName);
           }
           stay = false;
         }
         else {
           responseObject = soapCallThread.getResponseObject();
           if(responseObject != null) {
             result = responseObject;
             timeOutCallBack.setObject(responseObject);
             stay = false;
             retrySoFar = numRetry; //breaking the upper loop
             inputVec.clear();
             inputVec.addElement("Got Response " + responseObject);
             util.printEventLog(_theLogger,"SRMPutClient.callSoapThread",
                inputVec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
     }//end while

     /*
     if(methodName.equals("srmpreparetoput") && pinTime == 500) { 
     //if(methodName.equals("srmpreparetoput") && firstTime < 1) 
      timeOutHappened = true;
      result = null;
      firstTime++; 
     }
     */

     if(timeOutHappened && result == null && !statusCall) {
         inputVec.clear();
         inputVec.addElement("setrequestdone to true and failed");
         inputVec.addElement("timedout and responseobject is null");
         inputVec.addElement("SRM server did not respond, server may be busy");
         util.printEventLog(_theLogger,"SRMPutClient.callSoapThread",
                inputVec,silent,useLog);
         util.printMessage("SRM server did not respond, server may be busy ",
               logger,silent);
         if(methodName.equals("srmpreparetoput")) {
            if(!thirdparty) {
              _srmClientIntf.setRequestTimedOut(true);
            }
            else {
              _srmClientIntf.setRequestInformation("SRM_RETURNED_NO_STATUS",
			"Request Timed Out. Reason, Server may be busy");
            }
         }
     }
     else if (!timeOutHappened && result != null && 
	      methodName.equals("srmpreparetoput") && thirdparty) {
          //this is needed for srm(full)->srm
          //for may P2P calls
          if(result instanceof SrmPrepareToPutResponse) {
             TReturnStatus rStatus = 
		((SrmPrepareToPutResponse)result).getReturnStatus();
             String temp="";
             if(rStatus != null) {
               temp = rStatus.getStatusCode().toString();
             }
             _srmClientIntf.setRequestInformation(temp,"");
          }
     }
     return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int doStatus(String uid, Vector fileInfo, String rToken)
      throws URI.MalformedURIException, java.rmi.RemoteException {

   if(rToken == null) {
     inputVec.clear();
     inputVec.addElement("\nCannot perform Status, request token is null");
     util.printEventLog(_theLogger,"DoStatus",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: " + 
		"Cannot perform Status, request token is null", logger,silent);
     util.printMessage("\nSRM-CLIENT: " + 
		"Cannot perform Status, request token is null", pIntf);
   }

   if(_debug) {
     util.printMessage("\nSRM-CLIENT:  ....Input parameters for SrmStatusOfPutRequest...", logger,silent);
     util.printMessage("\nSRM-CLIENT:  ....Input parameters for SrmStatusOfPutRequest...", pIntf);
     util.printMessage("SRM-CLIENT: UID="+uid, logger,silent);
     util.printMessage("SRM-CLIENT: UID="+uid, pIntf);
     util.printMessage("SRM-CLIENT: RequestToken="+rToken, logger,silent);
     util.printMessage("SRM-CLIENT: RequestToken="+rToken, pIntf);
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
     result = (SrmStatusOfPutRequestResponse) 
         callSoapThread(r,result,"srmstatusofput");
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
     util.printMessage("\nSRM-CLIENT: " + "Null result.", logger,silent);
     util.printMessage("\nSRM-CLIENT: " + "Null result.", pIntf);
     util.printMessage("SRM-CLIENT: " + "May be the given requestId is not valid",logger,silent);
     util.printMessage("SRM-CLIENT: " + "May be the given requestId is not valid",pIntf);
     util.printMessage("SRM-CLIENT: " + "RequestId : "+rToken,logger,silent);
     util.printMessage("SRM-CLIENT: " + "RequestId : "+rToken,pIntf);
     inputVec.clear();
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("Null result");
     inputVec.addElement("May be the given requestId is not valid");
     util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
     return 1000;
   }

   ArrayOfTPutRequestFileStatus arrayFStatus =
       result.getArrayOfFileStatuses();
  
   if(arrayFStatus == null) {
   TReturnStatus rStatus = result.getReturnStatus();
   if(rStatus != null) {
     sCode = rStatus.getStatusCode();
     util.printMessage("\nRequestStatus="+sCode.toString(),logger,silent);
     util.printMessage("\nRequestExplanation="+rStatus.getExplanation(),logger,silent);
     util.printMessage("\nRequestStatus="+sCode.toString(),pIntf);
     util.printMessage("\nRequestExplanation="+rStatus.getExplanation(),pIntf);
     inputVec.clear();
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("null arrayoffilestatust");
     inputVec.addElement("RequestStatus="+sCode.toString());
     inputVec.addElement("RequestExplanation="+rStatus.getExplanation());
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   else {
     util.printMessage("Null status code for this request", logger,silent);
     util.printMessage("Null status code for this request", pIntf);
     inputVec.clear();
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("null status code for this request");
     inputVec.addElement("null arrayoffilestatus");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   return util.mapStatusCode(sCode);
 }

   TPutRequestFileStatus[] fileStatus = arrayFStatus.getStatusArray();

   if(fileStatus.length == 0) {
      inputVec.clear();
      inputVec.addElement("No files in the request");
      util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
      util.printMessage("SRM-CLIENT: " + "No files in the request.", logger,silent);
      util.printMessage("SRM-CLIENT: " + "No files in the request.", pIntf);
      return util.mapStatusCode(sCode);
   }

   for(int i = 0; i < fileStatus.length; i++) {
      inputVec.clear();
      util.printMessage("SRM-CLIENT: ...........................................",logger,silent);
      util.printMessage("SRM-CLIENT: ...........................................",pIntf);
      util.printMessage("\tsurl="+
                fileStatus[i].getSURL().toString(),logger,silent);
      util.printMessage("\tsurl="+
                fileStatus[i].getSURL().toString(),pIntf);
      inputVec.addElement("SiteURL="+fileStatus[i].getSURL().toString());
      if(fileStatus[i].getTransferURL() != null) {
          util.printMessage("\tTransferURL="+
                fileStatus[i].getTransferURL().toString(),logger,silent);
          util.printMessage("\tTransferURL="+
                fileStatus[i].getTransferURL().toString(),pIntf);
          inputVec.addElement("TransferURL="+fileStatus[i].getTransferURL().toString());
      }
      if(fileStatus[i].getFileSize() != null) {
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),logger,silent);
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),pIntf);
          inputVec.addElement("FileSize="+fileStatus[i].getFileSize());
      }
      if(fileStatus[i].getRemainingPinLifetime() != null) {
          util.printMessage("\tRemainingPinTime="+
                fileStatus[i].getRemainingPinLifetime(),logger,silent);
          util.printMessage("\tRemainingPinTime="+
                fileStatus[i].getRemainingPinLifetime(),pIntf);
          inputVec.addElement("RemainingPinTime="+fileStatus[i].getRemainingPinLifetime());
      }
      if(fileStatus[i].getRemainingFileLifetime() != null) {
          util.printMessage("\tRemainingFileLifeTime="+
                fileStatus[i].getRemainingFileLifetime(),logger,silent);
          util.printMessage("\tRemainingFileLifeTime="+
                fileStatus[i].getRemainingFileLifetime(),pIntf);
          inputVec.addElement("RemainingFileLifeTime="+fileStatus[i].getRemainingFileLifetime());
      }
      if(fileStatus[i].getEstimatedWaitTime() != null) {
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),logger,silent);
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),pIntf);
          inputVec.addElement("\tEstimatedWaitTime="+ fileStatus[i].getEstimatedWaitTime());
      }
      if(fileStatus[i].getTransferProtocolInfo() != null) {
          ArrayOfTExtraInfo a_extraInfos = fileStatus[i].getTransferProtocolInfo();
          TExtraInfo[] eInfos = a_extraInfos.getExtraInfoArray();
          if(eInfos != null) {
            for(int j = 0; j < eInfos.length; j++) {
               TExtraInfo eInfo = eInfos[j];
               util.printMessage("\tKey="+eInfo.getKey(), logger, silent);
               util.printMessage("\tValue="+eInfo.getValue(), logger, silent);
               util.printMessage("\tKey="+eInfo.getKey(), pIntf);
               util.printMessage("\tValue="+eInfo.getValue(),pIntf);
               inputVec.addElement("Key="+eInfo.getKey());
               inputVec.addElement("Value="+eInfo.getValue());
            }
          }
      }
      util.printMessage("\tstatus="+
                fileStatus[i].getStatus().getStatusCode(),logger,silent);
      util.printMessage("\tstatus="+
                fileStatus[i].getStatus().getStatusCode(),pIntf);
      inputVec.addElement("\tStatus="+ fileStatus[i].getStatus().getStatusCode());
      util.printMessage("\texplanation="+
             fileStatus[i].getStatus().getExplanation(),logger,silent);
      util.printMessage("\texplanation="+
             fileStatus[i].getStatus().getExplanation(),pIntf);
      inputVec.addElement("\texplanation="+ fileStatus[i].getStatus().getExplanation());
      util.printEventLog(_theLogger,"SrmPrepareToPutResponse",inputVec,silent,useLog);
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
