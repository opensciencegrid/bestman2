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
import gov.lbl.srm.client.util.*;
import org.globus.axis.util.Util;

import java.io.File;
import java.net.URL;
import java.util.Vector;
import java.util.HashMap;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;
import java.util.Date;


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
//SRMBringOnlineClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMBringOnlineClient implements SRMWSDLIntf {
  private Stub _stub;
  private ISRM _srm;
  //private ServiceContext _context; 
  private SRMClientIntf _srmClientIntf; 
  private GSSCredential _credential;
  private boolean _debug;
  private boolean submitOnly=false;
  private Log logger;
  private String fileToken;
  private String uid="";
  private String userDesc="";
  //private char tokenType;
  private int tokenLifetime;
  private long tokenSize;
  private long guarnSize;
  private boolean doReserveSpace;
  private Vector fileInfo;
  private boolean overwrite;
  private boolean firstTimeStatus=true;
  private boolean silent=false;
  private boolean useLog=false;
  private int partytype;
  private String statusToken="";
  private TReturnStatus rStatus = null;
  private boolean partycopy;
  private String storageInfo="";
  private String remoteTransferInfo="";
  private int type = 0;
  private int totalRequestTime;
  private int pinLifeTime;
  private TFileStorageType fileStorageType;
  private TRetentionPolicyInfo retentionPolicyInfo;
  private Vector srmCopyClientRefArray = new Vector();
  private String serverUrl;
  private int parallelism;
  private String protocolsList="";
  private int bufferSize;
  private boolean dcau=true;
  private static int statusMaxTimeAllowed=10800;
  //private static int statusMaxTimeAllowed=-1;
  //private static int statusWaitTime=30;
  private static int threshHold = 600;
  private static int statusWaitTime=10; //for status polling
  private boolean statusWaitTimeGiven=false;
  private java.util.logging.Logger _theLogger;
  private Vector inputVec = new Vector ();
  private Date requestDate;
  private PrintIntf pIntf;
  private int proxyType;
  private boolean noDownLoad;
  private String resultStatus="";
  private String resultExplanation="";
  private TimeOutCallBack timeOutCallBack;
  private SoapCallThread soapCallThread;
  private int connectionTimeOutAllowed=1800;
  private int setHTTPConnectionTimeOutAllowed=600;
  private String delegationNeeded="";
  private String requestToken="";
  private boolean noAbortFile;
  private int numRetry;
  private int retryTimeOut;
  private boolean checkPing;
  private boolean gateWayModeEnabled;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMBringOnlineClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMBringOnlineClient (SRMClientIntf srmClientIntf, 
	String serverUrl, String uid, Vector fileInfo, String  fileToken,
    String statusToken, boolean doReserveSpace, 
      boolean partycopy, boolean overwrite,
      int tokenlifetime, long tokensize, long guarnSize,
      GSSCredential credential, 
      TFileStorageType fileStorageType, 
      TRetentionPolicyInfo retentionPolicyInfo,
      int totalRequestTime, int pinLifeTime,
      int parallelism, int bufferSize, boolean dcau,
      int sMaxTimeAllowed, int statusWaitTime,
      String protocolsList,
      boolean statusWaitTimeGiven, String storageInfo, 
      String remoteTransferInfo,
      String userDesc, java.util.logging.Logger theLogger,
      Log logger, PrintIntf pIntf,	
      boolean debug,boolean silent,boolean useLog, 
      boolean submitOnly, int proxyType,
      boolean noDownLoad, int connectionTimeOutAllowed, 
      int setHTTPConnectionTimeOutAllowed,
      String delegationNeeded, 
      boolean noAbortFile,int threshHold,
      int numRetry, int retryTimeOut, boolean  checkPing) throws Exception 
{
	//_context = new ServiceContext ();
    _srmClientIntf = srmClientIntf;
    _credential = credential;
    this._theLogger = theLogger;
    this.logger = logger;
    this.pIntf = pIntf;
    this.partycopy = partycopy;
    this.overwrite = overwrite;
    _debug = debug;
    this.silent = silent;
    this.useLog = useLog;
    this.threshHold = threshHold;
    this.numRetry = numRetry;
    this.retryTimeOut = retryTimeOut;
    this.storageInfo = storageInfo;
    this.remoteTransferInfo = remoteTransferInfo;
    this.fileStorageType = fileStorageType;
    this.retentionPolicyInfo = retentionPolicyInfo;
    this.noDownLoad = noDownLoad;
    this.proxyType = proxyType;
    this.parallelism = parallelism;
    this.delegationNeeded = delegationNeeded;
    this.bufferSize = bufferSize;
    this.protocolsList = protocolsList;
    this.userDesc = userDesc;
    this.dcau = dcau;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.setHTTPConnectionTimeOutAllowed = setHTTPConnectionTimeOutAllowed;
    this.noAbortFile = noAbortFile;

    if(sMaxTimeAllowed != 10800) {
      this.statusMaxTimeAllowed = sMaxTimeAllowed;
    }
    if(statusWaitTime < 1000) {
      this.statusWaitTime = statusWaitTime*1000;
    }
    else {
      this.statusWaitTime = statusWaitTime;
    }
    this.statusWaitTimeGiven = statusWaitTimeGiven;
    this.submitOnly = submitOnly;

    this.uid = uid;
    this.fileInfo = fileInfo;
    this.doReserveSpace = doReserveSpace;
    //this.tokenType = type;
    this.tokenLifetime = tokenlifetime;
    this.tokenSize = tokensize;
    this.guarnSize = guarnSize;
    this.statusToken = statusToken;
    this.fileToken = fileToken;
    this.totalRequestTime = totalRequestTime;
    this.pinLifeTime = pinLifeTime;
    this.checkPing = checkPing;

    //System.out.println(">>>STATUSWAITTIME="+this.statusWaitTime);
    //System.out.println(">>>STATUSMAXTIME="+this.statusMaxTimeAllowed);

    String[] args = new String[2];

    args[0] = serverUrl;
    args[1] = uid;

    this.serverUrl = serverUrl;
    inputVec.addElement("ServerUrl="+serverUrl);
    inputVec.addElement("AuthId="+uid);
    if(!uid.equals("")) {
      if(_debug) {
        util.printMessage("AuthId=" + uid,logger,silent);
      }
    }
    openSRMConnection();
    if(checkPing) {
     gateWayModeEnabled=SRMUtilClient.isServerGateWayEnabled(_srm, uid,
                        logger,  _debug, credential,
                        proxyType, serverUrl,  _theLogger,
                        pIntf,  _srmClientIntf, silent,
                        useLog, connectionTimeOutAllowed,
                        setHTTPConnectionTimeOutAllowed,
                        delegationNeeded, numRetry, retryTimeOut);
     _srmClientIntf.setGateWayModeEnabled(gateWayModeEnabled);
        inputVec.clear();
        inputVec.addElement(" gateWayModeEnabled="+gateWayModeEnabled);
        util.printEventLog(_theLogger,"SrmBringOnlineClient.Initialization",
		inputVec,silent,useLog);
        System.out.println("SRM-CLIENT: GateWayModeEnabled="+gateWayModeEnabled);
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//openSRMConnection
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void openSRMConnection () throws Exception {
    // checking if httpg is handled.
    // if not java.net.URL returns exception on httpg.

    util.printMessage("SRM-CLIENT: " + new Date() + "Connecting to " + 
                serverUrl,logger,silent);
    util.printMessageHL("SRM-CLIENT: " + new Date() + "Connecting to " + 
                serverUrl,pIntf);

    SimpleProvider provider = new SimpleProvider ();
    SimpleTargetedChain c = null;

    URL uu = null;
    MyGlobusURL gurl = new MyGlobusURL(serverUrl,0);
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
    if(serverUrl.startsWith("httpg")) {
      String protocols0 = System.getProperty("java.protocols.handler.pkgs");
      org.globus.net.protocol.httpg.Handler handler =
           new org.globus.net.protocol.httpg.Handler();
       try {
           uu = new URL("httpg",host,port,path,handler);
       }catch(Exception h) {
           System.out.println("does not work");
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
       c = new SimpleTargetedChain(new HTTPSSender());
       provider.deployTransport("http",c);
       uu = new URL(serverUrl);
    }
    Util.registerTransport();
    SRMServiceLocator service = new SRMServiceLocator(provider);

    ISRM srm = service.getsrm(uu);
    util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);

    if(srm == null) {
      inputVec.addElement("remote srm object is null");
      util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
      throw new Exception("\nSRM-CLIENT: remote srm object is null");
    }
    else {
       inputVec.addElement("got remote srm object");
       util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
    }

    _srm = srm;

    org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) _srm;
    srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000);
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
    _srmClientIntf.setRequestInformation("SRM_FAILURE","");
    _srmClientIntf.setRequestDone(true,false);
    return;
  }
  try {
    srmBringOnline(uid, fileInfo);
  }
  catch(Exception e) {
   util.printEventLogException(_theLogger,"performTransfer.bringonline",e);
   inputVec.clear();
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"PerformTransfer",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   int idx6 = msg.indexOf(
	  "java.net.SocketTimeoutException: Read timed out");
   util.printHException(e,pIntf);
   if(pIntf != null) {
      pIntf.setCompleted(false);
   }
   util.printMessageHException("\nSRM-CLIENT: Exception " + msg, pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") || 
      idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
     util.printMessage("\nSRM-CLIENT: Exception : "+msg,logger,silent);
     util.printMessageHException("\nSRM-CLIENT: Exception : "+msg,pIntf);
     if(pIntf == null) {
       if(idx != -1 || idx6 != -1) {
         inputVec.clear();
         inputVec.addElement("ExitStatus="+90);
         util.printEventLog(_theLogger,
				"ExitCodeStatus",inputVec,silent,useLog);
         util.printHException(e,pIntf);
         System.exit(90);
       }
       else if(idx != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type ",logger,silent);
             inputVec.clear();
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
         util.printHException(e,pIntf);
         System.exit(96);
       }
       else {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
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
		" Calling SrmStatusOfBringOnlineRequest", logger, silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmStatusOfBringOnlineRequest", pIntf);
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"Calling SrmStatusOfBringOnlineRequest", inputVec,silent,useLog);
  int exitCode = doStatus(uid, fileInfo, statusToken);
  if(pIntf == null) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+exitCode);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
    System.exit(exitCode);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callSoapThread
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Object callSoapThread(Object request,
        Object result, String methodName) throws Exception {

     int retrySoFar=0;
     boolean timeOutHappened=false;
     int nRetry = numRetry;
     boolean statusCall = false;
     String exceptionHappened="";

     if(methodName.equals("srmstatusofbringonline")) {
        nRetry = 1;
        statusCall = true;
     }

     while(retrySoFar < nRetry) {

       if(retrySoFar > 1) {
         Thread.sleep(retryTimeOut*1000);
       }

       inputVec.clear();
       inputVec.addElement("Creating NewCall for " +
         methodName + " for numRetry="+retrySoFar);
       util.printEventLog(_theLogger,"SRMBringOnlineClient.callSoapThread",
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
           util.printEventLog(_theLogger,"SRMBringOnlineClient.callSoapThread",
                inputVec,silent,useLog);
           try {
            _srm=null;
            soapCallThread.setInterrupt(true);
            openSRMConnection();
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMBringOnlineClient.callSoapThread",ie);
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
             util.printEventLog(_theLogger,
                "SRMBringOnlineClient.callSoapThread",
                inputVec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
    }//end while

    if(timeOutHappened && result == null && !statusCall) {
           inputVec.clear();
           inputVec.addElement("setrequestdone to true and failed");
           inputVec.addElement("timedout and responseobject is null");
           inputVec.addElement("SRM server did not respond, server may be busy");
           util.printEventLog(_theLogger,"SRMBrinOnlineClient.callSoapThread",
                inputVec,silent,useLog);
       util.printMessage("SRM server did not respond, server may be busy ",
               logger,silent);
       if(methodName.equals("srmbringonline")) { 
         _srmClientIntf.setRequestTimedOut(true);
       }
     }
     return result;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseSpace() throws Exception {
  if(gateWayModeEnabled) return;

  if(fileToken != null) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + " Calling releaseSpace " + fileToken, logger, silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + " Calling releaseSpace " + fileToken, pIntf);
    //util.printMessage("TimeStamp="+new Date(), logger,silent);
    inputVec.clear();
    inputVec.addElement("TimeStamp="+new Date());
    util.printEventLog(_theLogger,"Calling releaseSpace", inputVec,silent,useLog);
    releaseSpace(fileToken,true);
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
    util.printMessage("\nSRM-CLIENT: " + new Date() + " Calling releaseFile", logger, silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + " Calling releaseFile", 
		pIntf);
    //util.printMessage("TimeStamp="+new Date(), logger,silent);
    inputVec.clear();
    inputVec.addElement("TimeStamp="+new Date());
    util.printEventLog(_theLogger,"Calling releaseFile", inputVec,silent,useLog);
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
// putDone
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) throws Exception {
  //calling the srmPut's srmCopyClient.putDone

  if(gateWayModeEnabled) return;

  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  inputVec.addElement("SiteUrl="+siteUrl);
  inputVec.addElement("Rid="+rid);
  inputVec.addElement("Label="+label);
  util.printEventLog(_theLogger,"Calling PutDone", inputVec,silent,useLog);

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + " putDone (srmGetClient)", logger, silent);
    util.printMessage("siteUrl=" + siteUrl, logger, silent);
    util.printMessage("rid=" + rid, logger, silent);
    util.printMessage("label=" + label, logger, silent);
    util.printMessage("\nSRM-CLIENT: " + new Date() + " putDone (srmGetClient)", pIntf);
    util.printMessage("siteUrl=" + siteUrl, pIntf);
    util.printMessage("rid=" + rid, pIntf);
    util.printMessage("label=" + label, pIntf);
  }

  int size = srmCopyClientRefArray.size();
  for(int i = 0; i < size; i++) {
    inputVec.clear();
    SRMCopyClientRef sCopyRef = (SRMCopyClientRef) srmCopyClientRefArray.elementAt(i);
    if(!sCopyRef.getDone()) {
      sCopyRef.setDone();
      SRMWSDLIntf srmCopyClient = sCopyRef.getCopyClient();
      inputVec.addElement("FoundReferenceInTheLoop");
      util.printEventLog(_theLogger,"PutDone",inputVec,silent,useLog);
      if(_debug) { 
        util.printMessage("SRM-CLIENT: found reference in the loop " ,logger,silent);
        util.printMessage("SRM-CLIENT: found reference in the loop " ,pIntf);
      }
      srmCopyClient.putDone(siteUrl, rid, label);
      break;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFiles
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles(String siteUrl, String rid,int label) throws Exception {

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

  //calling the srmPut's srmCopyClient.abortFiles
 

  inputVec.clear(); 
  inputVec.addElement("SiteUrl="+siteUrl);
  util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" abortFiles (srmGetClient) " + siteUrl, logger, silent);
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" abortFiles (srmGetClient) " + siteUrl, pIntf);
  }

  int size = srmCopyClientRefArray.size();
  for(int i = 0; i < size; i++) {
    inputVec.clear(); 
    SRMCopyClientRef sCopyRef = (SRMCopyClientRef) srmCopyClientRefArray.elementAt(i);
    if(!sCopyRef.getDone()) {
      sCopyRef.setDone();
      SRMWSDLIntf srmCopyClient = sCopyRef.getCopyClient();
      inputVec.addElement("FoundReferenceInTheLoop"); 
      util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);
      if(_debug) { 
        util.printMessage("SRM-CLIENT: found reference in the loop " ,logger,silent);
        util.printMessage("SRM-CLIENT: found reference in the loop ",pIntf);
      }
      srmCopyClient.abortFiles(siteUrl,rid,label);
      break;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmBringOnline
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void srmBringOnline(String uid, Vector fileInfo)  
        throws Exception {

  HashMap statusArray = new HashMap();
  HashMap oResult = new HashMap();
  int maxSize = _srmClientIntf.getMaximumFilesPerRequest();
  int totalSubRequest = 1;
  int totalSize = fileInfo.size();
  int modSize = 0;
  int totalFiles = 0;
  int numFilesFailed = 0;


  if(!statusWaitTimeGiven) {
     if(totalSize > 20) {
        statusWaitTime=60;
     }
  }

  if(maxSize == 0) { //if user did not give any option
   maxSize = totalSize;
  }

  if(totalSize == 1) {
    totalSubRequest = 1;
    maxSize = 1;
  }
  else if (totalSize > maxSize) { 
    modSize = (totalSize % maxSize); 
    int subSize = totalSize/maxSize;
    if(modSize > 0) {
      totalSubRequest = subSize+1;
    }
    else {
     totalSubRequest = subSize;
    }
 }
 else if(totalSize <= maxSize) {
   totalSubRequest = 1;
   maxSize = totalSize;
 }
  
 _srmClientIntf.setTotalSubRequest(totalSubRequest);
 _srmClientIntf.setTotalFilesPerRequest(maxSize);

  int count = 0;
  int index = 0;

  if(totalSize > 1) {
    if(_debug) {
      System.out.println("\nSRM-CLIENT: Total number of subrequests=" + totalSubRequest);
    }
  }

  inputVec.clear(); 
  inputVec.addElement("TotalNumberOfSubRequests="+totalSubRequest);
  util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::: ", logger, silent);
    util.printMessage("SRM-CLIENT: :::::: Input parameters for SrmBringOnlineRequest :::: ", logger, silent);
    util.printMessage("SRM-CLIENT: :::::::::::::::::::::::::::::::::::::::::::::::::::::", logger, silent);
    util.printMessage("SRM-CLIENT: ...Input parameters for SrmBringOnlineRequest... ", pIntf);
  }

  while (count < totalSubRequest) {

    inputVec.clear(); 
    if(count == (totalSubRequest-1)) {
      if(modSize > 0) {
        maxSize = modSize;
      }
    }

    Vector surlVec = new Vector ();
    for(int i =  index+0; i < index+maxSize; i++) {
       FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
       if(!fInfo.getCompleted()) {
        surlVec.addElement(fInfo.getSURL());
        fInfo.setOrigTURL(fInfo.getTURL());
       }
    }

    util.printEventLog(_theLogger,"Input parameters",inputVec,silent,useLog);

    inputVec.clear();
    int ssize = surlVec.size();
    String[] surl = new String[ssize];
    int sindex = 0;

    for(int i = 0; i < ssize; i++) {
       FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
       surl[sindex] = fInfo.getSURL();
       if(_debug) {
         util.printMessage("SRM-CLIENT: SURL("+sindex+")=" + 
		surl[sindex],logger,silent);
         util.printMessage("SRM-CLIENT: SURL("+sindex+")=" + 
	        surl[sindex],pIntf);
       } 
       inputVec.addElement("SURL("+sindex+")="+surl[sindex]);
       sindex++;
    }

    util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);


    inputVec.clear(); 
    if (doReserveSpace) {
      fileToken = reserveSpace(uid);
      if(fileToken == null) {
		inputVec.addElement
			("SRM reservespace returned null space token, please check with SRM admin");
        util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
        util.printMessage
		 ("\nSRM-CLIENT: SRM reservespace returned null space token, please check with SRM admin", logger,silent);
        util.printMessage
		 ("\nSRM-CLIENT: SRM reservespace returned null space token, please check with SRM admin", pIntf);
        if(pIntf == null) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+1000);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(100);  
        }
      }
    }

    SrmBringOnlineResponse response = 
	   prepareToBringOnlineMultiFiles(surl, fileToken);

    StringBuffer responseBuffer = new StringBuffer();
    HashMap subStatusArray = 
		printBringOnlineResult(response,responseBuffer,oResult);

    inputVec.clear(); 
    if(response == null || subStatusArray == null || subStatusArray.size() == 0) {
       if(!responseBuffer.toString().equals("")) {
         inputVec.addElement("null response");
         inputVec.addElement("exp="+responseBuffer.toString());
         if(_debug) {
           util.printMessage("SRM-CLIENT: Explanation="+responseBuffer.toString(), logger,silent);
           util.printMessage("SRM-CLIENT: Explanation="+responseBuffer.toString(), pIntf);
         }
       }
    }
    else {
      statusArray.put(response.getRequestToken(),subStatusArray);
    }

    if(response != null) {
      _srmClientIntf.addRequestToken(response.getRequestToken());
    }
    else {
      inputVec.addElement("Null response from SRM for this sub request");
      if(_debug) {
        util.printMessage("\nSRM-CLIENT: Null response from SRM for this sub request", logger, silent);
        util.printMessage("\nSRM-CLIENT: Null response from SRM for this sub request", pIntf);
      }
    }

    util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);

    totalFiles = totalFiles + subStatusArray.size();

    _srmClientIntf.setTotalFiles(totalFiles);

	count ++;
	index = index + maxSize;
  }//end while

  StringBuffer expBuffer = new StringBuffer();
  int si = 0;
  if(!submitOnly) {
  while (statusArray.size() > 0) {
    inputVec.clear(); 
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
    Vector keySURLS = new Vector();
    while (subStatusArray.size() > 0) {
      inputVec.clear(); 
      keySURLS.clear();
      Object[] keySetArray = subStatusArray.keySet().toArray();
      Object[] oldKeySetArray = oResult.keySet().toArray();
      rCode.delete(0,rCode.length());
      expBuffer.delete(0,expBuffer.length());



        
      for(int k = 0; k < keySetArray.length; k++) {
        expBuffer.delete(0,expBuffer.length());
        oSubKey = (String) keySetArray [k];
        //subKey = (String) oldKeySetArray [k];
        subKey = (String) oResult.get(oSubKey);
        TBringOnlineRequestFileStatus status =
           (TBringOnlineRequestFileStatus)(subStatusArray.get(oSubKey));
        TStatusCode code = status.getStatus().getStatusCode();
        if(timedOutCase) {
          Object[] objArray = fileInfo.toArray();
          for(int kk = 0; kk < objArray.length; kk++) {
            FileInfo ffInfo = (FileInfo) objArray[kk];
            String turl = subKey;
            if(ffInfo.getSURL().equals(turl)) {
              _srmClientIntf.srmFileFailure(ffInfo.getLabel(),
               "File status timed out.");
              ffInfo.setFileStatus(code.toString());
              if(status.getStatus().getExplanation() != null) {
                ffInfo.setFileExplanation(status.getStatus().getExplanation());
              }
              else {
                ffInfo.setFileExplanation("SRM-CLIENT: File status timed out");
              }
              break;
            }
          }
          numFilesFailed++;
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
             keySURLS.addElement(new URI(subKey));
         }
        //added file_in_cache for cern v2 server
        else if(code != TStatusCode.SRM_REQUEST_INPROGRESS &&
           code != TStatusCode.SRM_REQUEST_QUEUED) {
           if (code == TStatusCode.SRM_FILE_PINNED ||
               code == TStatusCode.SRM_SUCCESS || 
		       code == TStatusCode.SRM_DONE ||
               code == TStatusCode.SRM_FILE_IN_CACHE) {
           //initiate pulling the file
           Object[] objArray = fileInfo.toArray();
           FileInfo ffInfo = null;
           for(int kk = 0; kk < objArray.length; kk++) {
             ffInfo = (FileInfo) objArray[kk];
             ffInfo.setFileStatus(code.toString());
             if(status.getStatus().getExplanation() != null) {
               ffInfo.setFileExplanation(status.getStatus().getExplanation());
             }
             else {
               ffInfo.setFileExplanation("");
             }
             String turl = subKey;
             //System.out.println(">>>ffInfo.getSURL()="+ffInfo.getSURL());
             //System.out.println(">>>ffInfo.getTURL()="+ffInfo.getTURL());
             //System.out.println(">>>TURL="+turl);
             if(ffInfo.getSURL().equals(turl)) {
               if(status != null) {
                 String srmFileSize = ""+status.getFileSize();
                 ffInfo.setExpectedSize(srmFileSize);
                 ffInfo.setRID(subResponse); 
                 if(ffInfo.getTURL().equals("")) {
                   ffInfo.setActualSize(srmFileSize); 
                   ffInfo.setStatusLabel("Done");
                 }
                 else {
                   if(!noDownLoad) {
                     initiateSRMGetCall(ffInfo,totalFiles);   
                   } 
                 }
               }
               subStatusArray.remove(oSubKey);
               break;
             }
            }
           }
           else {
             if(code == null) {
               code = SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);
             }
             if((code != null) && (code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
                (code != TStatusCode.SRM_REQUEST_QUEUED)) {
                 printErrorResponse(status);
                 Object[] objArray = fileInfo.toArray();
                 FileInfo ffInfo = null;
                 for(int kk = 0; kk < objArray.length; kk++) {
                   ffInfo = (FileInfo) objArray[kk];
                   String turl = subKey; 
                   if(ffInfo.getSURL().equals(turl)) {
                     ffInfo.setRID(subResponse); 
                     if(code == TStatusCode.SRM_ABORTED) {
                      _srmClientIntf.srmFileFailure(ffInfo.getLabel(),
                          "File Aborted");
                       ffInfo.setFileStatus(code.toString());
                       ffInfo.setFileExplanation("SRM-CLIENT: File Aborted");
                     }
                     else {
                      if(status != null && status.getStatus() != null) { 
                        if(status.getStatus().getExplanation() != null) {
                           expBuffer.append(status.getStatus().getExplanation());
                        }
                      } 
                      //_srmClientIntf.srmFileFailure(ffInfo.getLabel(),
                         //code.toString()+ " " + expBuffer.toString());
                      _srmClientIntf.srmFileFailure(ffInfo.getLabel(),"");
                       ffInfo.setFileStatus(code.toString());
                       ffInfo.setFileExplanation(expBuffer.toString());
                     }
                     break;
                   }
                 }
                 numFilesFailed++;
                 subStatusArray.remove(oSubKey);
              }
           }
         }
       } 
      }//end for

      keySetArray = subStatusArray.keySet().toArray();

      if(keySetArray.length > 0 && 
		!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
         util.printMessage
                ("SRM-CLIENT: Max retry check status " +
                  "exceeded for status bringonline", logger,silent);
         expBuffer.append("Max retry check exceeded for status bringonline");
         inputVec.addElement("Max retry check exceeded for status bringonline"); 
         util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
         timedOutCase=true;
       }

      if (keySetArray.length > 0 && !timedOutCase) {
        oSubKey = (String) keySetArray [0];
        TBringOnlineRequestFileStatus status =
           (TBringOnlineRequestFileStatus)(subStatusArray.get(oSubKey));
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
             util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
        }
      }

      TBringOnlineRequestFileStatus[] statuses = null;
      IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed ();
      if (subStatusArray.size() > 0 && !timedOutCase) {
         Thread.sleep(statusWaitTime);
         if(keySURLS.size() > 0) {
          statuses  = checkBringOnlineStatus(keySURLS, subResponse, rCode,
			wholeRequestFailed);
         }
      if(statuses != null) {
         for(int kk = 0; kk < statuses.length; kk++) {
            TBringOnlineRequestFileStatus status =
              (TBringOnlineRequestFileStatus) statuses[kk];
            expBuffer.delete(0,expBuffer.length());
            MyGlobusURL gurl = new MyGlobusURL
				(status.getSourceSURL().toString(),1);
            String protocol = gurl.getProtocol();
            String host = gurl.getHost();
            int port = gurl.getPort();
            String path = gurl.getFilePath();
            int idx = path.indexOf("?SFN=");
            if(idx != -1) {
               path = path.substring(idx+5);
            }
            subStatusArray.put(protocol+"://"+host+":"+port+"/"+path,status); 
            TStatusCode tempCode = null;

            if(status != null) {
               if(wholeRequestFailed.getWholeRequestFailed()) {
                 tempCode = TStatusCode.SRM_FAILURE;
                 expBuffer.append(status.getStatus().getExplanation());
                 TReturnStatus rs = new TReturnStatus();
                 rs.setStatusCode(tempCode);
                 rs.setExplanation(status.getStatus().getExplanation());
                 status.setStatus(rs);
               }
               else {
                 tempCode = status.getStatus().getStatusCode();
                 expBuffer.append(status.getStatus().getExplanation());
               }
             }
           else {
              tempCode =
                   SRMUtilClient.mapReturnStatusValueBackToCode(_debug,rCode,logger,_theLogger);
           }
           if(tempCode != TStatusCode.SRM_REQUEST_QUEUED &&
                tempCode != TStatusCode.SRM_REQUEST_INPROGRESS) {
              inputVec.clear(); 
              inputVec.addElement("Status="+tempCode);
              inputVec.addElement("Explanation="+expBuffer.toString());
              util.printEventLog(_theLogger,"SrmStatusOfBringOnline",inputVec,silent,useLog);
              if(_debug) {
              util.printMessage
                ("SRM-CLIENT: SURL="+status.getSourceSURL(),logger,silent);
              util.printMessage
                ("SRM-CLIENT: SURL="+status.getSourceSURL(),pIntf);
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
           }
         }//end for
      }//end else 
     }//end if(!timedOutCase)
    }//while subStatus
    if(subStatusArray.size() == 0) statusArray.remove(ids);
    si++;
   }//while status
   }

   if(rStatus != null) {
     if(rStatus.getStatusCode() != null) {
       resultStatus = rStatus.getStatusCode().toString();
     }
     if(rStatus.getExplanation() != null) {
       resultExplanation = rStatus.getExplanation();
     }
   }

   if(submitOnly) {
     _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     _srmClientIntf.setRequestDone(true,true);
   }
   else {
   if(totalFiles == numFilesFailed) {
     _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     _srmClientIntf.setRequestDone(true,true);
   }
   else {
     _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     _srmClientIntf.setRequestDone(true,false);
   }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiateSRMGetCall 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void initiateSRMGetCall (FileInfo fInfo, int tFiles) throws Exception {

  util.printMessage("\nSRM-CLIENT: " + new Date() + " Calling initiateSRMGetCall now ", logger,silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + " Calling initiateSRMGetCall now ", pIntf);

  inputVec.clear(); 
  inputVec.addElement("FileToken="+fileToken);
  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"InitiateSRMGetCall",inputVec,silent,useLog);

  Vector vecFiles = new Vector ();
  vecFiles.addElement(fInfo);

  if(_debug) {
    util.printMessage("SRM-CLIENT: Use the given fileToken  "+fileToken,logger,silent);
    util.printMessage("SRM-CLIENT: Use the given fileToken  "+fileToken,pIntf);
  }

  SRMWSDLIntf srmCopyClient = new SRMGetClient (_srmClientIntf,
                            serverUrl, uid, vecFiles,
                            fileToken, statusToken,
                            false,false, overwrite,
                            tokenLifetime, tokenSize, guarnSize,
                            _credential, fileStorageType, retentionPolicyInfo,
                            totalRequestTime,pinLifeTime,
                            tFiles,true,
                            parallelism, bufferSize, dcau, false,
                            statusMaxTimeAllowed, statusWaitTime/1000,
                            protocolsList, 
                            //statusWaitTimeGiven, 
                            true,
							storageInfo, remoteTransferInfo,userDesc,
							_theLogger, logger, pIntf, _debug, silent,useLog, 
							false,proxyType,connectionTimeOutAllowed,
                            setHTTPConnectionTimeOutAllowed,
						    delegationNeeded,noAbortFile,false,false,
				            threshHold,numRetry,retryTimeOut,checkPing);

  this.srmCopyClientRefArray.addElement(new SRMCopyClientRef(srmCopyClient));
  srmCopyClient.performTransfer(false);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printGetErrorResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printErrorResponse(TBringOnlineRequestFileStatus fileStatus) {
   if(_debug) {
     util.printMessage("SRM-CLIENT: ...........................................",logger,silent);
   }
   inputVec.clear(); 
   if(fileStatus == null) {
     inputVec.addElement("Null TBringOnlineRequestFileStatus ");
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Null TBringOnlineRequestFileStatus ",logger, silent);
     util.printMessage("SRM-CLIENT: Null TBringOnlineRequestFileStatus ",
			pIntf);
     return;
   }

   inputVec.addElement("SURL="+fileStatus.getSourceSURL());
   inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
   inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
   util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);

   if(_debug) {
     util.printMessage("SRM-CLIENT: SURL="+ fileStatus.getSourceSURL(),logger,silent);
     util.printMessage("SRM-CLIENT: Status="+ fileStatus.getStatus().getStatusCode(),logger,silent);
     util.printMessage("SRM-CLIENT: Explanation="+ fileStatus.getStatus().getExplanation(),logger,silent);
     util.printMessage("SRM-CLIENT: SURL="+ fileStatus.getSourceSURL(),pIntf);
     util.printMessage("SRM-CLIENT: Status="+ fileStatus.getStatus().getStatusCode(),pIntf);
     util.printMessage("SRM-CLIENT: Explanation="+ fileStatus.getStatus().getExplanation(),pIntf);
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printBringOnlineResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public HashMap printBringOnlineResult (SrmBringOnlineResponse response, 
	StringBuffer responseStatus, HashMap oResult) throws Exception {

   HashMap result = new HashMap ();
   if(_debug) {
     util.printMessage("\nSRM-CLIENT: #### Output from SRM ####",logger,silent);
     util.printMessageHL("\nSRM-CLIENT: ####Output from SRM ####",pIntf);
   }

   inputVec.clear(); 

   if(response == null) {
     inputVec.addElement("Null response");
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: \tBringOnlineRequest Response is null",logger,silent);
     util.printMessage("SRM-CLIENT: \tBringOnlineRequest Response is null",pIntf);
     return result;
   }

   if(response.getRequestToken() != null) {
     //if(_debug) { 
       util.printMessage("request.token= " + response.getRequestToken(),logger,silent);
       util.printMessage("request.token= " + response.getRequestToken(),pIntf);
     //}
     inputVec.addElement("RequestToken="+response.getRequestToken());
     requestToken = response.getRequestToken();
   }
   else {
     if(_debug) {
     util.printMessage("request.token= " + null,logger,silent);
     util.printMessage("request.token= " + null,pIntf);
     } 
     inputVec.addElement("RequestToken=null");
   }

   if(response.getReturnStatus() == null) {
     util.printMessage("SRM-CLIENT: Null return status ", logger,silent);
     util.printMessage("SRM-CLIENT: Null return status ", pIntf);
     responseStatus.append("null return status");
     inputVec.addElement("Null return status");
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     return result;
   }

   if(submitOnly) {
     rStatus = response.getReturnStatus ();
   }
   inputVec.addElement("Status="+response.getReturnStatus().getStatusCode());
   inputVec.addElement("Explanation="+response.getReturnStatus().getExplanation());
  
   resultStatus = response.getReturnStatus().getStatusCode().toString();
   if(response.getReturnStatus().getExplanation() != null) {
     resultExplanation  = response.getReturnStatus().getExplanation();
   }

   util.printMessage ("Request.status="+
        response.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("explanation="+
        response.getReturnStatus().getExplanation(),logger,silent);
   util.printMessage ("Request.status="+
        response.getReturnStatus().getStatusCode(),pIntf);
   util.printMessage("explanation="+
        response.getReturnStatus().getExplanation(),pIntf);

   if(response.getReturnStatus().getExplanation() != null) {
     responseStatus.append(response.getReturnStatus().getExplanation());
   }

   responseStatus.append(response.getReturnStatus().getStatusCode().toString());

   if(response.getRemainingTotalRequestTime() != null) {
     inputVec.addElement("request.remainingTotalRequestTime=" +
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
     if(_debug) {
     util.printMessage("Null ArrayOfFileStatuses ", logger,silent);
     util.printMessage("Null ArrayOfFileStatuses ", pIntf);
     }
     inputVec.addElement("Null ArrayOfFileStatuses ");
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     return result;
   }

   if(response.getArrayOfFileStatuses().getStatusArray() == null) {
     if(_debug) {
     util.printMessage("Null getStatusArray()", logger,silent);
     util.printMessage("Null getStatusArray()", pIntf);
     }
     inputVec.addElement("Null getStatusArray()");
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     return result;
   }

   util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);

   int size = response.getArrayOfFileStatuses().getStatusArray().length;

   int localStatusWaitTime = 60;
   boolean estimatedTimeGiven=false;
   for (int i=0;  i < size; i++) {
     inputVec.clear(); 
     TBringOnlineRequestFileStatus fileStatus =
       response.getArrayOfFileStatuses().getStatusArray(i);

     if(fileStatus.getStatus() == null) {
        if(_debug) {
          util.printMessage("SRM-CLIENT: Null return status from SRM", logger,silent);
          util.printMessage("SRM-CLIENT: Null return status from SRM", pIntf);
        }
        inputVec.addElement("Null return status from SRM");
        util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
        return result;
     }
     else {
        if(_debug) {
        inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
        inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
        util.printMessage("\tStatus="+
          fileStatus.getStatus().getStatusCode(),logger,silent);
        util.printMessage("\tExplanation="+
          fileStatus.getStatus().getExplanation(),logger,silent);
        util.printMessage("\tStatus="+
          fileStatus.getStatus().getStatusCode(),pIntf);
        util.printMessage("\tExplanation="+
          fileStatus.getStatus().getExplanation(),pIntf);
        }
     }
     if(fileStatus.getSourceSURL() != null) {
       if(_debug) {
         util.printMessage("\tSURL="+
           fileStatus.getSourceSURL().toString(),logger,silent);
         util.printMessage("\tSURL="+
           fileStatus.getSourceSURL().toString(),pIntf);
         inputVec.addElement("SURL="+fileStatus.getSourceSURL().toString());
       } 
     }
     if(fileStatus.getEstimatedWaitTime() != null) {
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
     MyGlobusURL gurl = new MyGlobusURL
		(fileStatus.getSourceSURL().toString(),1);
     String protocol = gurl.getProtocol();
     String host = gurl.getHost();
     int port = gurl.getPort();
     String path = gurl.getFilePath();
     int idx = path.indexOf("?SFN=");
     if(idx != -1) {
        path = path.substring(idx+5);
     }
     result.put(protocol+"://"+host+":"+port+"/"+path, fileStatus);
     //oResult.put(fileStatus.getSourceSURL().toString(), fileStatus);
     oResult.put(protocol+"://"+host+":"+port+"/"+path, 
		fileStatus.getSourceSURL().toString());
     util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
     Object[] objArray = fileInfo.toArray();
     for(int j = 0; j < objArray.length; j++) {
          FileInfo fInfo = (FileInfo) objArray[j];
          if(fInfo.getOrigSURL().equals(fileStatus.getSourceSURL().toString())) {
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
          }
     }
   }

   if(_debug) {
     util.printMessage("SRM-CLIENT: ...........................................",logger,silent);
   }

   if(estimatedTimeGiven) {
     statusWaitTime = localStatusWaitTime*1000;
     if(statusWaitTime >= statusMaxTimeAllowed * 1000) {
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

private void getSpaceTokenMeta(String token) throws Exception {

 if(doReserveSpace) {
  if(token != null) {
    SRMUtilClient.getSpaceTokenMeta(_srm,token,logger,_debug,
       _credential,proxyType,serverUrl,_theLogger,
      pIntf, _srmClientIntf, silent, useLog, 
       connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
       delegationNeeded, numRetry, retryTimeOut);
  }
  else {
    inputVec.clear(); 
    inputVec.addElement("Cannot do getSpaceTokenMeta for null token");
    util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Cannot do getSpaceTokenMeta for null token ",logger,silent);
    util.printMessage("SRM-CLIENT: Cannot do getSpaceTokenMeta for null token ",pIntf);
  }
 }
}
	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(boolean keepSpace, String surl, 
     String token, int label) throws Exception {
  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  return SRMUtilClient.releaseFile
        (_srm,keepSpace,surl,token,uid,proxyString,
         _debug, logger,pIntf,_credential,proxyType,serverUrl,
         _theLogger, pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded, numRetry, retryTimeOut);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public void releaseSpace(String token, 
    boolean forceFileRelease) throws Exception {
		 
  if(gateWayModeEnabled) return; 

  SRMUtilClient.releaseSpace
        (_srm,token,uid,forceFileRelease,logger,_debug,
         _credential,proxyType,serverUrl,_theLogger,
         pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded, numRetry, retryTimeOut);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareToBringOnlineMultiFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public SrmBringOnlineResponse 
	prepareToBringOnlineMultiFiles(String[] surls, String  token)  
		throws Exception {

   if(_debug) {
     getSpaceTokenMeta(token);
   }

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   inputVec.clear(); 
   SrmBringOnlineRequest r = new SrmBringOnlineRequest();

   if(!userDesc.equals("")) {
     if(_debug) { 
       util.printMessage("SRM-CLIENT: UserDescription="+userDesc, logger, silent);
       util.printMessage("SRM-CLIENT: UserDescription="+userDesc, pIntf);
     }
     inputVec.addElement("UserDescription="+userDesc);
     r.setUserRequestDescription(userDesc);
   }

   if(_debug) {
   util.printMessage("SRM-CLIENT: AuthorizationID="+uid, logger, silent);
   //util.printMessage("SRM-CLIENT: UserRequestDescription=Test description", logger, silent);
   util.printMessage("SRM-CLIENT: AuthorizationID="+uid, pIntf);
   //util.printMessage("SRM-CLIENT: UserRequestDescription=Test description", pIntf);
   } 
   inputVec.addElement("AuthorizationID="+uid);
   //inputVec.addElement("UserRequestDescription=Test description");

   if(fileToken != null) {
     if(_debug) {
     util.printMessage("SRM-CLIENT: SpaceToken= "+fileToken, logger, silent);
     util.printMessage("SRM-CLIENT: SpaceToken= "+fileToken, pIntf);
     }
     inputVec.addElement("SpaceToken="+fileToken);
     r.setTargetSpaceToken(fileToken);
   }

   if(fileStorageType != null) {
     if(_debug) {
     util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType, logger, silent);
     util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType, pIntf);
     }
     inputVec.addElement("FileStorageType="+fileStorageType);
     r.setDesiredFileStorageType(fileStorageType);
   }

   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }
   r.setUserRequestDescription(userDesc);
   //inputVec.addElement("Test description");

   if(totalRequestTime != 0) {
     r.setDesiredTotalRequestTime(new Integer(totalRequestTime));
     if(_debug) {
     util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime,logger,silent);
     util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime,
		pIntf);
     }
     inputVec.addElement("TotalRequestTime="+totalRequestTime);
   }
   if(pinLifeTime != 0) {
     r.setDesiredLifeTime(new Integer(pinLifeTime));
     if(_debug) {
     util.printMessage("SRM-CLIENT: PinLifeTime="+pinLifeTime,logger,silent);
     util.printMessage("SRM-CLIENT: PinLifeTime="+pinLifeTime,pIntf);
     }
     inputVec.addElement("PinLifeTime="+pinLifeTime);
   }

   int total = surls.length;

   TGetFileRequest[] fileReqList = new TGetFileRequest[total];

    for (int i=0; i<total; i++) {
      TGetFileRequest curr = new TGetFileRequest();

      curr.setSourceSURL(new URI(surls[i]));
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

   if(_debug) {
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo, logger,silent);
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo, pIntf);
   }
   inputVec.addElement("StorageInfo="+storageInfo);
   
   if(storageInfo.equals("true")) {

      tExtra = new TExtraInfo();
      tExtra.setKey("uid");
      tExtra.setValue("get");
      vec.addElement(tExtra);

      tExtra = new TExtraInfo();
      tExtra.setKey("pwd");
      tExtra.setValue(proxyString);
      vec.addElement(tExtra);
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
           String msg =  "Given storage info is not in the correct format " +
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

   TTransferParameters transferParameters = new TTransferParameters();
   ArrayOfString protocolsArray = new ArrayOfString();
   Vector protocolVec = new Vector();

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

   String tt = "";
   for(int i = 0; i < protocols.length;i++) {
     tt = tt+protocols[i]+",";
   }

   if(_debug) {
     util.printMessage("SRM-CLIENT: Protocols="+tt, logger, silent);
     util.printMessage("SRM-CLIENT: Protocols="+tt, pIntf);
   }
   inputVec.addElement("Protocols="+tt);
   protocolsArray.setStringArray(protocols);
   transferParameters.setArrayOfTransferProtocols(protocolsArray);
   //transferParameters.setAccessPattern(TAccessPattern.TRANSFER_MODE);
   //util.printMessage("\tAccessPattern=TRANSFERMODE", logger,silent);
   //transferParameters.setConnectionType(TConnectionType.WAN);
   //util.printMessage("\tConnectionType=WAN", logger,silent);

   if(retentionPolicyInfo != null) {
     if(_debug) {
     util.printMessage("SRM-CLIENT: RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy(), logger,silent); 
	 util.printMessage("SRM-CLIENT: ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), logger,silent);
     util.printMessage("SRM-CLIENT: RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy(), pIntf); 
	 util.printMessage("SRM-CLIENT: ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), pIntf);
     }
     inputVec.addElement("RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy());
     inputVec.addElement("AccessInfo="+retentionPolicyInfo.getAccessLatency());
     r.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
   }

   r.setTransferParameters(transferParameters);

   ArrayOfTGetFileRequest arrayOfFileRequest = 
		new ArrayOfTGetFileRequest();
   arrayOfFileRequest.setRequestArray(fileReqList);
   r.setArrayOfFileRequests(arrayOfFileRequest);

   requestDate = new Date();
   util.printMessage("\nSRM-CLIENT: " + requestDate + 
		" Calling SrmBringOnline Request now ...",logger,silent);
   util.printMessageHL("\nSRM-CLIENT: " + requestDate + 
		" Calling SrmBringOnline Request now ...",pIntf);
   //inputVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"SrmBringOnlineRequest",inputVec,silent,useLog);

   SrmBringOnlineResponse result = null;

   result = (SrmBringOnlineResponse) callSoapThread(r,result,"srmbringonline");

   return result;

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int doStatus(String uid, Vector fileInfo, String rToken)
      throws URI.MalformedURIException, java.rmi.RemoteException {
   
  inputVec.clear(); 
  inputVec.addElement("RequestToken="+rToken);

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: ...Input parameters for SrmStatusOfBringOnlineRequest...", logger,silent);
    util.printMessage("SRM-CLIENT: RequestToken="+rToken,logger,silent);
    util.printMessage("SRM-CLIENT: AuthId="+uid,logger,silent);
  }
  SrmStatusOfBringOnlineRequestRequest r = 
		new SrmStatusOfBringOnlineRequestRequest();
  int size = fileInfo.size();
  String sampleSURL = "";

  URI[] tsurl = new URI[size];
  for(int i = 0; i < size; i++) {
    FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
    fIntf.setRID(rToken);
    URI temp = new URI(fIntf.getSURL());
    tsurl[i] = temp;
    sampleSURL = fIntf.getSURL();
    inputVec.addElement("SURL="+fIntf.getSURL());
    if(_debug) {
     util.printMessage("SRM-CLIENT: SURL="+sampleSURL,logger,silent);
    }
  }
  r.setArrayOfSourceSURLs(SRMUtilClient.convertToArrayOfAnyURI(tsurl));

  r.setRequestToken(rToken);
   
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
    if(_debug) {
      inputVec.addElement("SRM-CLIENT: AuthorizationID="+uid);
    }
  }

  util.printEventLog(_theLogger,"SrmStatusOfBringOnlineRequest",inputVec,silent,useLog);

  TStatusCode sCode = null;
  SrmStatusOfBringOnlineRequestResponse result = null;

 try {

  result = (SrmStatusOfBringOnlineRequestResponse) 
        callSoapThread(r,result,"srmstatusofbringonline");

  inputVec.clear(); 

  if(_debug){
     util.printMessage("\nSRM-CLIENT: ####Output from SRM ####", logger,silent);
  }

  if(result == null) {
    inputVec.addElement("Null result.");
    inputVec.addElement("May be the given requestId is not valid");
    inputVec.addElement("RequestId="+rToken);
    util.printEventLog(_theLogger,"SrmStatusOfBringOnlineResponse",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Null result.", logger,silent);
    util.printMessage("SRM-CLIENT: May be the given requestId is not valid",logger,silent);
    util.printMessage("SRM-CLIENT: RequestId : "+rToken,logger,silent);
    util.printMessage("SRM-CLIENT: Null result.", pIntf);
    util.printMessage("SRM-CLIENT: May be the given requestId is not valid",
		pIntf);
    util.printMessage("SRM-CLIENT: RequestId : "+rToken,pIntf);
    return 1000;
  }

  ArrayOfTBringOnlineRequestFileStatus arrayFStatus =
       result.getArrayOfFileStatuses();


  if(arrayFStatus == null) {
   TReturnStatus rStatus = result.getReturnStatus();
   if(rStatus != null) {
     sCode = rStatus.getStatusCode();
     util.printMessage("\nRequestStatus="+sCode.toString(),logger,silent);
     util.printMessage("\nRequestExplanation="+rStatus.getExplanation(),logger,silent);
     inputVec.clear(); 
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("null arrayoffilestatust");
     inputVec.addElement("RequestStatus="+sCode.toString());
     inputVec.addElement("RequestExplanation="+rStatus.getExplanation());
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   else {
     util.printMessage("Null status code for this request", logger,silent);
     inputVec.clear(); 
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("null status code for this request");
     inputVec.addElement("null arrayoffilestatust");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   return util.mapStatusCode(sCode);
 }


  TBringOnlineRequestFileStatus[] fileStatus = arrayFStatus.getStatusArray();

  if(fileStatus.length == 0) {
    util.printMessage("SRM-CLIENT: No files in this request", logger,silent);
    inputVec.addElement("No files in this request");
    util.printEventLog(_theLogger,"SrmStatusOfBringOnlineResponse",inputVec,silent,useLog);
    return util.mapStatusCode(sCode);
  }

  util.printEventLog(_theLogger,"SrmStatusOfBringOnlineResponse",inputVec,silent,useLog);

  for(int i = 0; i < fileStatus.length; i++) {

      inputVec.clear(); 

      if(fileStatus[i].getSourceSURL() != null) {
          inputVec.addElement("SURL="+ fileStatus[i].getSourceSURL());
          util.printMessage("\tSURL="+
                fileStatus[i].getSourceSURL(),logger,silent);
          util.printMessage("\tSURL="+ fileStatus[i].getSourceSURL(),pIntf);
      }
      if(fileStatus[i].getFileSize() != null) {
          inputVec.addElement("FileSize="+ fileStatus[i].getFileSize());
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),logger,silent);
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),pIntf);
      }
      if(fileStatus[i].getRemainingPinTime() != null) {
          inputVec.addElement("RemainingPinTime="+
                fileStatus[i].getRemainingPinTime());
          util.printMessage("\tRemainingPinTime="+
                fileStatus[i].getRemainingPinTime(),logger,silent);
          util.printMessage("\tRemainingPinTime="+
                fileStatus[i].getRemainingPinTime(),pIntf);
      }
      if(fileStatus[i].getEstimatedWaitTime() != null) {
          inputVec.addElement("EstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime());
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),logger,silent);
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),pIntf);
      }
      if(fileStatus[i].getStatus()  != null) {
          inputVec.addElement("Status="+
                fileStatus[i].getStatus().getStatusCode());
          inputVec.addElement("Explanation="+
             fileStatus[i].getStatus().getExplanation());
          util.printMessage("\tStatus="+
                fileStatus[i].getStatus().getStatusCode(),logger,silent);
          util.printMessage("\tExplanation="+
             fileStatus[i].getStatus().getExplanation(),logger,silent);
          util.printMessage("\tStatus="+
                fileStatus[i].getStatus().getStatusCode(),pIntf);
          util.printMessage("\tExplanation="+
             fileStatus[i].getStatus().getExplanation(),pIntf);
      }
      util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
    }
  }catch(Exception e) {
     System.out.println("SRM-CLIENT: SrmBringOnline:Exception="+e.getMessage());
     util.printEventLogException(_theLogger,"SRMBringOnline.Exception",e);
  }
  return util.mapStatusCode(sCode);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkBringOnlineStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public TBringOnlineRequestFileStatus[] checkBringOnlineStatus
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


  inputVec.clear(); 

  if(_debug) {
  util.printMessage("\nSRM-CLIENT: ......Input parameters from srmStatusOfBringOnlineRequest... ", logger,silent); 
  util.printMessage("\nSRM-CLIENT: Input parameters from srmStatusOfBringOnlineRequest ", pIntf); 
  util.printMessage("SRM-CLIENT: RID="+response,logger,silent);
  util.printMessage("SRM-CLIENT: RID="+response,pIntf);
  util.printMessage("SRM-CLIENT: uid="+uid,logger,silent);
  util.printMessage("SRM-CLIENT: uid="+uid,pIntf);
  }

  inputVec.addElement("RID="+response);
  for(int i = 0; i < keySURLS.size(); i++) {
    if(_debug) {
    util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),logger,silent);
    util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),pIntf);
    }
    inputVec.addElement("SourceSURL("+i+")="+response);
  }

  util.printEventLog(_theLogger,"CheckBringOnlineStatus", inputVec,silent,useLog);

  if(response == null) {
    inputVec.clear(); 
    if(_debug) {
    util.printMessage("SRM-CLIENT: SRM return null response", logger,silent);
    util.printMessage("SRM-CLIENT: SRM return null response", pIntf);
    }
    inputVec.addElement("SRM return null response");
    util.printEventLog(_theLogger,"CheckBringOnlineStatus", inputVec,silent,useLog);
    return null;
  }

  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmStatusOfBringOnlineRequestRequest",inputVec,silent,useLog);

  if(requestDate != null) {
    /*
    util.printMessage("SRM-CLIENT: Elapsed time from request is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", logger,silent);
    util.printMessage("SRM-CLIENT: Elapsed time from request is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", pIntf);
    */
  }

  /*
  if(firstTimeStatus) {
     util.printMessage("SRM-CLIENT: Waiting ...",logger,silent);
     firstTimeStatus=false;
  }
  else {
  util.printMessageNL(".",logger,silent);
  } 
  */

  SrmStatusOfBringOnlineRequestRequest r = new SrmStatusOfBringOnlineRequestRequest();

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: ### Output from SRM ###",logger,silent);
  }
  r.setArrayOfSourceSURLs(SRMUtilClient.convertToArrayOfAnyURI(uris));
  r.setRequestToken(response);

  TBringOnlineRequestFileStatus[] fileStatus = null;
  SrmStatusOfBringOnlineRequestResponse result = null;
 try {

  result = (SrmStatusOfBringOnlineRequestResponse) 
        callSoapThread(r,result,"srmstatusofbringonline");

  inputVec.clear(); 

  if(result == null) {
    if(_debug) {
     util.printMessage("SRM-CLIENT: SRM returned null result for srmStatusOfBringOnlineRequest", logger,silent);
     util.printMessage("SRM-CLIENT: SRM returned null result for srmStatusOfBringOnlineRequest", pIntf);
    }
    inputVec.addElement("SRM returned null result for srmStatusOfBringOnlineRequest");
    util.printEventLog(_theLogger,"SrmStatusOfBringOnlineRequestResponse",inputVec,silent,useLog);
    return null;
  }

  if(result.getReturnStatus() == null) {
     if(_debug) {
     util.printMessage("\nSRM-CLIENT: SRM returned null getReturnStatus() for " + "srmStatusOfBringOnlineRequest",logger,silent);
     util.printMessage("\nSRM-CLIENT: SRM returned null getReturnStatus() for " + "srmStatusOfBringOnlineRequest",pIntf);
     } 
     inputVec.addElement("SRM returned null getReturnStatus() for " +
		"srmStatusOfBringOnlineRequest");
     util.printEventLog(_theLogger,"SrmStatusOfBringOnlineRequestResponse",inputVec,silent,useLog);
     return null;
  }

  rStatus = result.getReturnStatus();

  inputVec.addElement("Status Calling at " + new Date());

  util.printMessage("\nSRM-CLIENT: " + statusDate + " Request Status from SRM (srmStatusOfBringOnlineRequest) " + result.getReturnStatus().getStatusCode(), logger, silent);
  util.printMessage("\nSRM-CLIENT: " + statusDate + " Request Status from SRM (srmStatusOfBringOnlineRequest) " + result.getReturnStatus().getStatusCode(), pIntf);
  //if(_debug) {
  //inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
  //}
  if(result.getReturnStatus().getExplanation() != null) { 
    util.printMessage("\tStatus="+
	  result.getReturnStatus().getStatusCode(),logger,silent);
    util.printMessage("\tExplanation="+
	  result.getReturnStatus().getExplanation(),logger,silent);
    inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
  }


  rCode.append(result.getReturnStatus().getStatusCode().toString());

  if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {
     wholeRequestFailed.setWholeRequestFailed(true);
  }

 
  if(result.getArrayOfFileStatuses() != null) {
    fileStatus = result.getArrayOfFileStatuses().getStatusArray();
  }

  if(fileStatus != null) {
     for(int kk = 0; kk <fileStatus.length; kk++) {
       TBringOnlineRequestFileStatus fStatus = fileStatus[kk];
       if(fStatus.getEstimatedWaitTime() != null) {
          Integer xx = fStatus.getEstimatedWaitTime();
          int yy = xx.intValue();
          if(yy != -1) {
            if(_debug) {
               System.out.println("\nSRM-CLIENT: EstimatedWait " +
                "given by server is " + yy + " seconds.");
            }
            if(statusWaitTime > yy*1000) {
              statusWaitTime = yy*1000;
            }
          }
       }
     }
  }

  util.printEventLog(_theLogger,"SrmStatusOfBringOnlineRequestResponse",inputVec,silent,useLog);

  //System.out.println(">>>StatusWaitTime Before="+statusWaitTime);
  statusWaitTime = statusWaitTime*2;
  if(statusWaitTime >= threshHold*1000) {
     statusWaitTime = threshHold*1000;
        //resetting back to threshHold value
  }
  if(statusWaitTime >= statusMaxTimeAllowed * 1000) {
     if(_debug) {
       System.out.println("\nSRM-CLIENT: Next status call is " +
            "adjusted according to totalrequesttime given by the user");
     }
     statusWaitTime = statusWaitTime - 5000;
  }
  //System.out.println(">>>StatusWaitTime After="+statusWaitTime);

  }catch(Exception e) {
     System.out.println("SRM-CLIENT: SrmBringOnline:Exception="+e.getMessage());
     util.printEventLogException(_theLogger,"SRMBringOnline.Exception",e);
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
   return SRMUtilClient.reserveSpace(_srm,strUID,userDesc,
        retentionPolicyInfo, tokenSize, tokenLifetime, 
        guarnSize,new StringBuffer(), logger,_debug,false,
        sCode,statusBuf,_credential,proxyType,serverUrl,_theLogger,
        pIntf, _srmClientIntf, silent, useLog, 
        connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
        delegationNeeded, numRetry, retryTimeOut); 
}

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
        util.printEventLog(_theLogger,"SetSecurityPropertiesForceFully",
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

  public void setWholeRequestFailed(boolean b) {
     wholeRequestFailed=b;
  }

  public boolean getWholeRequestFailed() {
     return wholeRequestFailed;
  }
}
}
