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
import org.globus.axis.util.Util;

import java.io.*;
import java.net.URL;
import java.util.Vector;
import java.util.HashMap;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
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

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xml.serialize.*;

import gov.lbl.srm.client.intf.PrintIntf;
import gov.lbl.srm.client.util.MyGlobusURL;

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
//SRMUtilClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMUtilClient { 
  private Stub _stub;
  //private ServiceContext _context;
  private ISRM _srm;
  private SRMClientIntf _srmClientIntf; 
  private GSSCredential _credential;
  private static boolean _debug;
  private static Log logger;
  private PrintIntf pIntf;
  private String fileToken=null;
  private String requestToken;
  private String uid="";
  private String userDesc="";
  private String spaceType="replica";
  private String accessLatencyType="online";
  private int tokenLifetime;
  private long tokenSize;
  private long gTokenSize;
  private boolean doReserveSpace;
  private boolean overwrite;
  private static boolean silent;
  private boolean useLog;
  private static int statusMaxTimeAllowed=10800; //in seconds
  //private static int statusMaxTimeAllowed=-1;
  //private static int statusWaitTime=30;
  private static int statusWaitTime=10; //changed for exponential polling
  private static int threshHold = 600;
  private boolean statusWaitTimeGiven=false;
  private static String storageInfo="";
  private java.util.logging.Logger _theLogger;
  private Vector inputVec = new Vector ();
  private int proxyType;
  private int connectionTimeOutAllowed=1800;
  private int setHTTPConnectionTimeOutAllowed=600;
  public TimeOutCallBack timeOutCallBack;
  public SoapCallThread soapCallThread;
  public String serverUrl;
  public String delegationNeeded="";
  private int numRetry=3;
  private int retryTimeOut=60;
  private static int firstTime;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMUtilClient --- constructor
// used for releasefile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMUtilClient (String serverUrl, String uid, String userDesc, 
      GSSCredential credential, java.util.logging.Logger theLogger,
	  Log logger, PrintIntf pIntf, boolean debug, boolean silent, 
      boolean useLog,
	  boolean doGsiFTPList, boolean doLocalLsList,
	  int sMaxTimeAllowed, 
	  int statusWaitTime, 
	  String storageInfo, int proxyType, 
	  int connectionTimeOutAllowed, 
      int setHTTPConnectionTimeOutAllowed, String delegationNeeded,
        int numRetry, int retryTimeOut) throws Exception 
{
    this.uid = uid;
    this.userDesc = userDesc;
    this._credential = credential;
    this._theLogger = theLogger;
    this.logger = logger;
    this.pIntf = pIntf;
    this._debug = debug;
    this.silent = silent;
    this.useLog = useLog;
    this.storageInfo = storageInfo;
    this.delegationNeeded=delegationNeeded;
    this.proxyType = proxyType;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.setHTTPConnectionTimeOutAllowed = setHTTPConnectionTimeOutAllowed;
    this.numRetry = numRetry;
    this.retryTimeOut = retryTimeOut;
    if(sMaxTimeAllowed != 10800 && sMaxTimeAllowed > 0) {
      this.statusMaxTimeAllowed = sMaxTimeAllowed;
    }
    if(statusWaitTime < 1000) {
      this.statusWaitTime = statusWaitTime*1000;
    }
    else {
      this.statusWaitTime = statusWaitTime;
    }
    //System.out.println(">>>STATUSWAITTIME="+this.statusWaitTime);
    //System.out.println(">>>STATUSMAXTIME="+this.statusMaxTimeAllowed);
    this.serverUrl = serverUrl;

    if(!doGsiFTPList && !doLocalLsList) {
     openSRMConnection ();
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// openSRMConnection
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void openSRMConnection () throws Exception {

    SimpleProvider provider = new SimpleProvider ();
    SimpleTargetedChain c = null;
    URL uu = null;
    MyGlobusURL gurl = new MyGlobusURL(serverUrl,0);
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
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
    }else if(serverUrl.startsWith("https")) {
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
    SRMServiceLocator locator = new SRMServiceLocator (provider);

    ISRM srm = locator.getsrm(uu);

    if(srm == null) {
      inputVec.clear();
      inputVec.addElement("remote srm object is null");
      util.printEventLog(_theLogger,"SrmUtil-Constructor",inputVec,silent,useLog);
      inputVec.addElement("ServiceUrl="+serverUrl);
      throw new Exception("\nremote srm object is null" + serverUrl);
    }
    else {
      inputVec.clear();
      inputVec.addElement("got remote srm object");
      util.printEventLog(_theLogger,"SrmUtil-Constructor",inputVec,silent,useLog);
      inputVec.addElement("ServiceUrl="+serverUrl);
    }

    _srm = srm;
  
    
    org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) _srm;
    srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000);

    if(delegationNeeded.equals("false")) {
       if(_debug) {
         util.printMessage("\nSRM-CLIENT: no delegation by client",
            logger,silent);
       }
    }
    else if(delegationNeeded.equals("true")) {
        setSecurityPropertiesForcefully(new URL(serverUrl),_srm);
    }
    else if(delegationNeeded.equals("")) {
        setSecurityProperties(new URL(serverUrl),_srm);
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//openStaticSRMConnection
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static ISRM openStaticSRMConnection (String serverUrl,
        GSSCredential credential, int proxyType,
        java.util.logging.Logger _theLogger, PrintIntf pIntf, boolean silent,
        boolean useLog, int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded) throws Exception {

    SimpleProvider provider = new SimpleProvider ();
    SimpleTargetedChain c = null;
    URL uu = null;
    MyGlobusURL gurl = new MyGlobusURL(serverUrl,0);
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
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
    }else if(serverUrl.startsWith("https")) {
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
    SRMServiceLocator locator = new SRMServiceLocator (provider);

    ISRM srm = locator.getsrm(uu);

    Vector inputVec = new Vector ();

    if(srm == null) {
      inputVec.clear();
      inputVec.addElement("remote srm object is null");
      util.printEventLog(_theLogger,"SrmUtil-Constructor",inputVec,silent,useLog);
      inputVec.addElement("ServiceUrl="+serverUrl);
      throw new Exception("\nremote srm object is null" + serverUrl);
    }
    else {
      inputVec.clear();
      inputVec.addElement("got remote srm object");
      util.printEventLog(_theLogger,"SrmUtil-Constructor",inputVec,silent,useLog);
      inputVec.addElement("ServiceUrl="+serverUrl);
    }

    org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) srm;
    srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000);
    if(delegationNeeded.equals("false")) {
       if(_debug) {
         util.printMessage("\nSRM-CLIENT: no delegation by client",
            logger,silent);
       }
    }
    else if(delegationNeeded.equals("true")) {
        setStaticSecurityPropertiesForcefully
        (new URL(serverUrl),srm,credential,proxyType,
         _theLogger, pIntf, silent, useLog, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded);
    }
    else if(delegationNeeded.equals("")) {
        setStaticSecurityProperties
        (new URL(serverUrl),srm,credential,proxyType,
         _theLogger, pIntf, silent, useLog, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded);
    }
    return srm;
}




//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFileToken
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setFileToken(String fileToken) {
  this.fileToken = fileToken;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestToken
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestToken(String requestToken) {
  this.requestToken = requestToken;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSpaceParams
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSpaceParams(String spaceType, String accessLatencyType,
	int tokenLifetime, long tokenSize, long gTokenSize) {
  this.spaceType = spaceType;
  this.accessLatencyType = accessLatencyType;
  this.tokenLifetime = tokenLifetime;
  this.tokenSize = tokenSize;
  this.gTokenSize = gTokenSize;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// reserveSpaceStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode reserveSpaceStatus(boolean textReport, String rToken) 
	throws Exception {

   StringBuffer reportBuf = new StringBuffer();
   TStatusCode sCode = SRMUtilClient.doReserveStatus
        (_srm,uid, rToken, logger,_debug,_credential,proxyType,serverUrl,
         _theLogger, pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded, numRetry, retryTimeOut);


   if(textReport) {
      inputVec.clear();
      if(reportBuf.toString() != null && !reportBuf.toString().equals("")) {
        inputVec.addElement("Printing text report now");
        inputVec.addElement("ReportBuf="+reportBuf.toString());
        util.printEventLog(_theLogger,"ReserveSpace",inputVec,silent,useLog);
        util.printMessage("\nSRM-CLIENT Printing text report now...\n",
			logger,silent);
        util.printMessage(reportBuf.toString(),logger,silent);;
     }
   }
   return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// reserveSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String reserveSpace(boolean textReport, boolean submitOnly) 
	throws Exception {
   TRetentionPolicyInfo retentionPInfo = null;
   
   TStatusCode sCode = null;
   inputVec.clear();
   inputVec.addElement("SpaceType="+spaceType);
   inputVec.addElement("AccessLatencyType="+accessLatencyType);
   inputVec.addElement("SpaceToken="+fileToken);
   util.printEventLog(_theLogger,"ReserveSpace",inputVec,silent,useLog);
   if(spaceType.equalsIgnoreCase("CUSTODIAL")) {
     retentionPInfo = new TRetentionPolicyInfo();
     retentionPInfo.setRetentionPolicy(TRetentionPolicy.CUSTODIAL);
   }
   else if(spaceType.equalsIgnoreCase("OUTPUT")) {
     //retentionPInfo = new TRetentionPolicyInfo();
     //retentionPInfo.setRetentionPolicy(TRetentionPolicy.OUTPUT);
     ;
   }
   else if(spaceType.equalsIgnoreCase("REPLICA")) {
     retentionPInfo = new TRetentionPolicyInfo();
     retentionPInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
   }
   if(accessLatencyType.equalsIgnoreCase("NEARLINE")) {
     retentionPInfo.setAccessLatency(TAccessLatency.NEARLINE);
   }
   else {
     retentionPInfo.setAccessLatency(TAccessLatency.ONLINE);
   }
   StringBuffer reportBuf = new StringBuffer();
   StringBuffer statusBuf = new StringBuffer();
   fileToken = SRMUtilClient.reserveSpace
        (_srm,uid,userDesc,retentionPInfo, tokenSize, tokenLifetime,
         gTokenSize,reportBuf,logger,_debug,submitOnly, sCode,statusBuf,
         _credential, proxyType, serverUrl,_theLogger,
         pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegationNeeded, numRetry, retryTimeOut);

   /*
   if(fileToken != null) {
    util.printMessage("SpaceToken=" + fileToken,logger,silent);
    util.printMessageHL("SpaceToken=" + fileToken,pIntf);
   }
   */

   if(textReport) {
      if(reportBuf.toString() != null && !reportBuf.toString().equals("")) {
      inputVec.clear();
      inputVec.addElement("Printing text report now");
      inputVec.addElement("ReportBuf="+reportBuf.toString());
      util.printEventLog(_theLogger,"ReserveSpace",inputVec,silent,useLog);
       util.printMessage("\nPrinting text report now...\n",logger,silent);
       util.printMessage(reportBuf.toString(),logger,silent);;
      }
   }
   return statusBuf.toString();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode releaseSpace(boolean forceFileRelease) throws Exception {
  if(fileToken != null) {
    return SRMUtilClient.releaseSpace(_srm,fileToken,uid,forceFileRelease,
		logger,_debug,_credential,proxyType,serverUrl,_theLogger,
                pIntf, _srmClientIntf, silent, useLog, 
                connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
                delegationNeeded, numRetry, retryTimeOut);
  }
  else {
    return null;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmCheckPermission
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmCheckPermission (Vector fileInfo) throws Exception {

 TStatusCode sCode = null;

 try {

  util.printMessage("\nSRM-UTIL: " + new Date() + 
		" Calling srmCheckPermission", logger,silent);
  SrmCheckPermissionRequest request = new SrmCheckPermissionRequest ();

  inputVec.clear();

  if(!uid.equals("")) {
   if(_debug) {
   util.printMessage("\nSRM-UTIL:  ...Input parameters...", logger,silent);
   util.printMessage("\tSRM-UTIL: UID="+uid,logger,silent);
   }
   inputVec.addElement("UID="+uid);
   request.setAuthorizationID(uid);
  }

  URI[] urlArray = new URI[fileInfo.size()];

  for(int i = 0; i < fileInfo.size(); i++) {
    FileInfo fInfo = (FileInfo)fileInfo.elementAt(i);
    urlArray[i] = new URI(fInfo.getSURL());
  }
  ArrayOfAnyURI arrayOfAnyURI = new ArrayOfAnyURI();
  arrayOfAnyURI.setUrlArray(urlArray);

  request.setArrayOfSURLs(arrayOfAnyURI);

  //setStorageSystemInfo
  
  SrmCheckPermissionResponse response = null;

  response = (SrmCheckPermissionResponse) 
        callSoapThread(request,response,"srmcheckpermission");

  if(_debug) {
  util.printMessage("\nSRM-MISC:   #### Output from SRM ####", logger,silent);
  }
  if(response == null) {
    inputVec.clear();
    inputVec.addElement("SRM returned null response for SrmCheckPermission");
    util.printEventLog(_theLogger,"SrmCheckPermissionResponse",inputVec,silent,useLog);
    util.printMessage("\nSRM-UTIL: SRM returned null response for SrmCheckPermission " +
		"request", logger,silent);
  }
  else {
    TReturnStatus returnStatus = response.getReturnStatus ();
    if(returnStatus == null) {
      inputVec.clear();
      inputVec.addElement("SRM returned null returnstatus");
      util.printEventLog(_theLogger,"SrmCheckPermissionResponse",inputVec,silent,useLog);
      util.printMessage("\nSRM-UTIL: SRM returned null returnstatus " , logger,silent);
    }
    else {
      TStatusCode statusCode = returnStatus.getStatusCode();
      sCode = statusCode;
      if(statusCode == null) {
        inputVec.clear();
        inputVec.addElement("SRM returned null status code");
        util.printEventLog(_theLogger,"SrmCheckPermissionResponse",inputVec,silent,useLog);
        util.printMessage("\nSRM-UTIL: SRM returned null status code" , logger,silent);
      }
      else {
        inputVec.clear();
        inputVec.addElement("StatusCode="+statusCode.toString());
        inputVec.addElement("Explanation="+returnStatus.getExplanation());
        util.printEventLog(_theLogger,"SrmCheckPermissionResponse",inputVec,silent,useLog);
        util.printMessage("SRM-UTIL: ............................" , logger,silent);
        util.printMessage("SRM-UTIL: StatusCode=" + statusCode.toString(),
			logger,silent);
        util.printMessage("SRM-UTIL: Explanation=" + returnStatus.getExplanation(),
			logger,silent);
        if(statusCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
          return statusCode;
        }
        ArrayOfTSURLPermissionReturn arrayOfSurlPermReturn = 
			response.getArrayOfPermissions(); 
        if(arrayOfSurlPermReturn == null) {
          inputVec.clear();
          inputVec.addElement("SRM returned null array of surl permission return");
          util.printEventLog(_theLogger,"SrmCheckPermissionResponse",inputVec,silent,useLog);
          //util.printMessage("\nSRM-UTIL: SRM returned null array of surl permission return" , logger,silent);
        }
        else {
          TSURLPermissionReturn[] permSurlReturns = 
				arrayOfSurlPermReturn.getSurlPermissionArray();
          for(int i = 0; i < permSurlReturns.length; i++) {
            TSURLPermissionReturn permSurlReturn = permSurlReturns[i];
            if(permSurlReturn != null) { 
               inputVec.clear();
               URI surl = permSurlReturn.getSurl(); 
               if(surl != null) {
                 util.printMessage("SRM-UTIL: SURL="+surl.toString(), logger,silent);
                 inputVec.addElement("SURL="+surl.toString());
               }
               TReturnStatus rStatus = permSurlReturn.getStatus(); 
               if(rStatus != null) {
                  sCode = rStatus.getStatusCode();
                  if(sCode != null) {
                    util.printMessage("SRM-UTIL: SURLPermission Status="+sCode.toString(), 
						logger,silent);
                    inputVec.addElement("SURLPermissionStatus="+sCode.toString());
                  }
                  String explanation = rStatus.getExplanation();
                  util.printMessage("SRM-UTIL: Explanation="+explanation, logger,silent);
                  inputVec.addElement("Explanation="+explanation);
               }
               TPermissionMode mode = permSurlReturn.getPermission();
               if(mode != null) {
                  util.printMessage("SRM-UTIL: Mode="+mode.getValue(), logger,silent);
                  inputVec.addElement("Mode="+mode.getValue());
               }
               util.printEventLog(_theLogger,"CheckPermissionResponse",inputVec,silent,useLog);
            }
          }
        }
      }
    }
  }
  }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
     String msg = e.getMessage();
     int idx = msg.indexOf("Connection refused");
     int idx1 = msg.indexOf("Defective credential detected");
     int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
     inputVec.clear();
     inputVec.addElement(e.getMessage());
     util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
     util.printMessage("SRM-UTIL: Exception " + e.getMessage(),logger,silent);
     if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
        idx != -1 || idx1 != -1 || idx5 != -1) {
       util.printMessage("\nException : "+msg,logger,silent);
       if(pIntf == null) {
         if(idx != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
             util.printHException(e,pIntf);
           System.exit(90);
         }
         else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch, " +
				" please check your proxy type",logger,silent); 
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
  return sCode;
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

public SRMWSDLIntf setGetTypeWSDLIntf () {
 return null;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmGetPermission
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmGetPermission(Vector fileInfo) throws Exception {

 TStatusCode sCode = null;
 util.printMessage("\nSRM-UTIL: " + new Date() + 
		" Calling srmGetPermission request ...", logger,silent);
 try {
  SrmGetPermissionRequest request = new SrmGetPermissionRequest ();
  inputVec.clear();

  if(!uid.equals("")) {
   if(_debug) {
   util.printMessage("\nSRM-UTIL: ...Input parameters ...", logger,silent);
   util.printMessage("SRM-UTIL: UID="+uid,logger,silent);
   }
   inputVec.addElement("UID="+uid);
   request.setAuthorizationID(uid);
  }

  URI[] urlArray = new URI[fileInfo.size()];

  for(int i = 0; i < fileInfo.size(); i++) {
    FileInfo fInfo = (FileInfo)fileInfo.elementAt(i);
    urlArray[i] = new URI(fInfo.getSURL());
    inputVec.addElement("SURL(+"+i+")="+fInfo.getSURL());
  }
  ArrayOfAnyURI arrayOfAnyURI = new ArrayOfAnyURI();
  arrayOfAnyURI.setUrlArray(urlArray);

  request.setArrayOfSURLs(arrayOfAnyURI);

  //setStorageSystemInfo

  //util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent);
  
  SrmGetPermissionResponse response = null;
  response = (SrmGetPermissionResponse) 
        callSoapThread(request,response,"srmgetpermission");

  if(response == null) {
    inputVec.clear();
    inputVec.addElement("SRM returned null response for SrmGetPermission request");
    util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
    util.printMessage
		("\nSRM-UTIL: SRM returned null response for SrmGetPermission request", 
			logger,silent);
  }
  else {
    TReturnStatus returnStatus = response.getReturnStatus ();
    if(returnStatus == null) {
      inputVec.clear();
      inputVec.addElement("SRM returned null returnstatus");
      util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
      util.printMessage("\nSRM-UTIL: SRM returned null returnstatus ", 
			logger,silent);
    }
    else {
      TStatusCode statusCode = returnStatus.getStatusCode();
      sCode = statusCode;
      if(statusCode == null) {
        inputVec.clear();
        inputVec.addElement("SRM returned null status code");
        util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
        util.printMessage("\nSRM-UTIL: SRM returned null status code", 
			logger,silent);
      }
      else {
        if(_debug) {
        util.printMessage("SRM-UTIL: ............................" , logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("StatusCode="+statusCode.toString());
        inputVec.addElement("Explanation="+returnStatus.getExplanation());
        util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
        util.printMessage("SRM-UTIL: StatusCode=" + statusCode.toString(),
			logger,silent);
        util.printMessage("SRM-UTIL: Explanation=" + returnStatus.getExplanation(),
			logger,silent);
        if(statusCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
          return statusCode;
        }
        ArrayOfTPermissionReturn arrayOfPermReturn = 
			response.getArrayOfPermissionReturns();
        if(arrayOfPermReturn == null) {
          inputVec.clear();
          inputVec.addElement("SRM returned null array of permission return");
          util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
          //util.printMessage("\nSRM-UTIL: SRM returned null array of permission return" , logger,silent);
        }
        else {
          TPermissionReturn[] permReturns = 
			arrayOfPermReturn.getPermissionArray();
          if(permReturns == null) {
            inputVec.clear();
            inputVec.addElement("SRM returned null permission return");
            util.printEventLog(_theLogger,"SrmGetPermission",inputVec,silent,useLog);
            //util.printMessage("\nSRM-UTIL: SRM returned null permission return" , 
				//logger,silent);
          }
          else {
          for(int kk = 0; kk < permReturns.length; kk++) {
            inputVec.clear();
            TPermissionReturn permReturn = permReturns[kk];  
            if(permReturn.getStatus() == null) {
              inputVec.addElement("SRM returned null permission return status");
              //util.printMessage("\nSRM-UTIL: SRM returned null permission return status", logger,silent);
            }
            else {
              TReturnStatus rStatus = permReturn.getStatus();
              sCode = rStatus.getStatusCode();
              if(sCode == null) {
               inputVec.addElement("SRM returned null permission status code");
               //util.printMessage("\nSRM-UTIL: SRM returned null permission status code", logger,silent);
              }
              else {
                inputVec.addElement("StatusCode="+sCode.toString());
                inputVec.addElement("Explanation="+rStatus.getExplanation());
                //if(_debug) {
                util.printMessage("\tStatusCode="+sCode.toString(),
					logger,silent);
                util.printMessage("\tExplanation="+rStatus.getExplanation(),
					logger,silent);
                //} 
              }
            }
            if(permReturn.getSurl() != null) {
             util.printMessage("\tSURL="+permReturn.getSurl(),logger,silent);
             inputVec.addElement("SURL="+permReturn.getSurl());
            }
            if(permReturn.getOwner() != null) {
             if(_debug) {
             util.printMessage("\tOwner="+permReturn.getOwner(),logger,silent);
             }
             inputVec.addElement("Owner="+permReturn.getOwner());
            }
            if(permReturn.getOwnerPermission() != null) {
             inputVec.addElement("OwnerPermission="+
				permReturn.getOwnerPermission().toString());
             if(_debug) {
             util.printMessage("\tOwnerPermission="+
				permReturn.getOwnerPermission().toString(),logger,silent);
             }
            }
            if(permReturn.getOtherPermission() != null) {
             inputVec.addElement("OtherPermission="+
				permReturn.getOtherPermission().toString());
             util.printMessage("\tOtherPermission="+
				permReturn.getOtherPermission().toString(),logger,silent);
            }
            ArrayOfTUserPermission aUserPerm = 
				permReturn.getArrayOfUserPermissions ();
            if(aUserPerm != null) { 
               TUserPermission[] ups = aUserPerm.getUserPermissionArray();
               for(int i = 0; i < ups.length; i++) {
                TUserPermission up = ups[i];
                util.printMessage("\tUserPermissions.UserID="+
			   	   up.getUserID(),logger,silent);
                util.printMessage("\tUserPermissions.Mode="+
			   	   up.getMode(),logger,silent);
                inputVec.addElement("UserPermissions.UserID="+up.getUserID());
                inputVec.addElement("UserPermissions.Mode="+up.getMode());
               }
            }
            ArrayOfTGroupPermission aGroupPerm = 
				permReturn.getArrayOfGroupPermissions();
            if(aGroupPerm != null) { 
               TGroupPermission[] gps = aGroupPerm.getGroupPermissionArray();
               for(int i = 0; i < gps.length; i++) {
                TGroupPermission gp = gps[i];
                util.printMessage("\tGroupPermissions.UserID="+
			   	   gp.getGroupID(),logger,silent);
                util.printMessage("\tGroupPermissions.Mode="+
			   	   gp.getMode(),logger,silent);
                inputVec.addElement("GroupPermissions.UserID="+gp.getGroupID());
                inputVec.addElement("GroupPermissions.Mode="+gp.getMode());
               }
            }
            util.printEventLog(_theLogger,"GroupPermissions",inputVec,silent,useLog);
           }
          }
        }
      }
    }
  }
 }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear();
   inputVec.addElement(e.getMessage());
   util.printEventLog(_theLogger,"GroupPermissions",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-UTIL: Exception " + e.getMessage(),logger,silent);
   util.printMessageHException("SRM-UTIL: Exception " + e.getMessage(),pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
             util.printHException(e,pIntf);
             System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy type", logger,silent);
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
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callStaticSoapThread
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static Object callStaticSoapThread(ISRM srm, 
        GSSCredential credential, int proxyType, String serverUrl, 
        Object request, Object result, String methodName,
        java.util.logging.Logger _theLogger,
        PrintIntf pIntf, SRMClientIntf _srmClientIntf,boolean silent,
        boolean useLog, int connectionTimeOutAllowed,
        int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded, int numRetry, int retryTimeOut) 
                throws Exception {

     int retrySoFar=0;
     boolean timeOutHappened=false;
     String exceptionHappened="";
     Vector vec = new Vector ();
     int nRetry = numRetry;
     boolean statusCall = false;
     
     if(methodName.equalsIgnoreCase("srmstatusofls") ||
          methodName.equalsIgnoreCase("srmstatusofupdatespace") ||
          methodName.equalsIgnoreCase("srmstatusofreservespace") ||
          methodName.equalsIgnoreCase("srmstatusofchangespace")) {
        nRetry = 1;
        statusCall = true;
     }


     while(retrySoFar < numRetry) {

       if(retrySoFar > 1) {
         Thread.sleep(retryTimeOut*1000);
       }

       vec.clear();
       vec.addElement("Creating NewCall for " +
                methodName + " for numRetry="+retrySoFar);
       util.printEventLog(_theLogger,"SRMUtilClient.callStaticSoapThread",
                vec,silent,useLog);
       long sTimeStamp = util.startTimeStamp();
       TimeOutCallBack timeOutCallBack = new TimeOutCallBack(result,sTimeStamp,
          setHTTPConnectionTimeOutAllowed,methodName);
       timeOutCallBack.setLogger(_theLogger,silent,useLog);
       timeOutCallBack.start();

       SoapCallThread soapCallThread = new SoapCallThread(srm, request,methodName);
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
           vec.clear();
           vec.addElement("Interrupting " + methodName);
           vec.addElement("timedout and responseobject is null");
           util.printEventLog(_theLogger,"SRMUtilClient.callStaticSoapThread",
                vec,silent,useLog);
           try {
            srm=null;
            soapCallThread.setInterrupt(true);
            srm = openStaticSRMConnection(serverUrl,credential,proxyType,
                    _theLogger, pIntf,silent,useLog,
                    setHTTPConnectionTimeOutAllowed, delegationNeeded);
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMUtilClient.callStaticSoapThread",ie);
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
             vec.clear();
             vec.addElement("Got Response " + responseObject);
             util.printEventLog(_theLogger,"SRMUtilClient.callStaticSoapThread",
                vec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
    }//end while

    /*
    if(methodName.equalsIgnoreCase("srmreleasefiles") && firstTime < 1) {
      timeOutHappened=true;
      result=null;
      firstTime++;
    }
    */
    

    if(timeOutHappened && result == null && !statusCall) {
           vec.clear();
           vec.addElement("timedout and responseobject is null");
           vec.addElement("SRM server did not respond, server may be busy");
           util.printEventLog(_theLogger,"SRMUtilClient.callStaticSoapThread",
                vec,silent,useLog);
        util.printMessage("SRM server did not respond, server may be busy",
                logger,silent);
       //_srmClientIntf.setRequestTimedOut(true);
       //_srmClientIntf.setRequestDone(true,false);
       //this is need or, the summary prints and code exits properly
       //else this callreturns null result and ends up in printPutResult
       //Thread.sleep(30000);
     }
     return result;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callSoapThread
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Object callSoapThread(Object request,
        Object result, String methodName) throws Exception {

     int retrySoFar=0;
     boolean timeOutHappened=false;
     String exceptionHappened="";
     int nRetry = numRetry;
     boolean statusCall = false;


     if(methodName.equalsIgnoreCase("srmstatusofls") ||
          methodName.equalsIgnoreCase("srmstatusofupdatespace") ||
          methodName.equalsIgnoreCase("srmstatusofreservespace") ||
          methodName.equalsIgnoreCase("srmstatusofchangespace")) {
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
       util.printEventLog(_theLogger,"SRMUtilClient.callSoapThread",
                inputVec,silent,useLog);
       long sTimeStamp = util.startTimeStamp();
       timeOutCallBack = new TimeOutCallBack(result,sTimeStamp,
          setHTTPConnectionTimeOutAllowed,methodName);
       timeOutCallBack.setLogger(_theLogger,silent,useLog);
       timeOutCallBack.start();

       soapCallThread = new SoapCallThread(_srm, request,methodName);
       soapCallThread.start();
       soapCallThread.setLogger(_theLogger,silent,useLog);
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
           util.printEventLog(_theLogger,"SRMUtilClient.callSoapThread",
                inputVec,silent,useLog);
           try {
            _srm=null;
            soapCallThread.setInterrupt(true);
            openSRMConnection();
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMUtilClient.callSoapThread",ie);
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
             util.printEventLog(_theLogger,"SRMUtilClient.callSoapThread",
                inputVec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
    }//end while

    //timeOutHappened=true;
    //result=null;

    if(timeOutHappened && result == null && !statusCall) {
           inputVec.clear();
           inputVec.addElement("timedout and responseobject is null");
           inputVec.addElement("SRM server did not respond, server may be busy");
           util.printEventLog(_theLogger,"SRMUtilClient.callSoapThread",
                inputVec,silent,useLog);
        util.printMessage("SRM server did not respond, server may be busy",
                logger,silent);
       //_srmClientIntf.setRequestTimedOut(true);
       //_srmClientIntf.setRequestDone(true,false);
       //this is need or, the summary prints and code exits properly
       //else this callreturns null result and ends up in printPutResult
       //Thread.sleep(30000);
     }
     return result;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmSetPermission
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmSetPermission(Vector fileInfo, 
	TPermissionType permType, TPermissionMode ownerPerm, 
	TPermissionMode otherPerm, TUserPermission[] userPerms, 
	TGroupPermission[] groupPerms) throws Exception {

 TStatusCode sCode = null;
 try {

    util.printMessage("\nSRM-UTIL: " + new Date() + 
		" Calling srmSetPermission request ...", logger,silent);

    SrmSetPermissionRequest request = new SrmSetPermissionRequest ();

    inputVec.clear(); 

    if(!uid.equals("")) {
      if(_debug) {
      util.printMessage("\nSRM-UTIL: ...Input parameters ...",logger,silent);
      util.printMessage("SRM-UTIL: UID="+uid,logger,silent);
      }
      inputVec.addElement("UID="+uid);
      request.setAuthorizationID(uid);
    }

    if(fileInfo.size() > 0) {
      FileInfo fInfo =  (FileInfo) fileInfo.elementAt(0);
      String sourceUrl =  fInfo.getSURL();
      org.apache.axis.types.URI surl =  
		new org.apache.axis.types.URI(sourceUrl);
      if(_debug) {
        util.printMessage("SRM-UTIL: SURL="+surl,logger,silent);
      }
      inputVec.addElement("SURL="+surl);
      request.setSURL(surl);
    }
    else {
      inputVec.addElement("Please provide the surlf or the srmSetPermissionRequest");
      util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
      util.printMessage ("\nSRM-UTIL: Please provide the surl for the " +
			"srmSetPermissionRequest ", logger,silent);
      util.printMessage ("\nSRM-UTIL: Please provide the surl for the " +
			"srmSetPermissionRequest ", pIntf);
      if(pIntf == null) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+93);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(93);
      }
    }

    if(permType != null) {
      inputVec.addElement("PermissionType="+permType.toString());
      if(_debug) {
        util.printMessage("SRM-UTIL: PermissionType="+permType.toString(),logger,silent); 
      }
      request.setPermissionType(permType);
    }

    if(ownerPerm != null) {
      inputVec.addElement("OwnerPermission="+ownerPerm.toString());
      if(_debug) {
        util.printMessage("SRM-UTIL:OwnerPermission="+ownerPerm.toString(),logger,silent); 
      }
      request.setOwnerPermission(ownerPerm);
    }

    if(otherPerm != null) {
      inputVec.addElement("OtherPermission="+otherPerm.toString());
      if(_debug) {
        util.printMessage("SRM-UTIL:OtherPermission="+otherPerm.toString(),logger,silent); 
      }
      request.setOtherPermission(otherPerm);
    }

    if(userPerms != null) {
      for(int i = 0; i < userPerms.length; i++) {
        TUserPermission up = userPerms[i];
        inputVec.addElement("UserPermission.UserID="+up.getUserID());
        inputVec.addElement("UserPermission.Mode="+up.getMode());
        if(_debug) {
          util.printMessage("SRM-UTIL:UserPermission.UserID="+up.getUserID(),
				logger,silent); 
          util.printMessage("SRM-UTIL:UserPermission.Mode="+up.getMode(),logger,silent); 
        }
      }
      ArrayOfTUserPermission arrayOfUserPermission = 
			new ArrayOfTUserPermission();
      arrayOfUserPermission.setUserPermissionArray(userPerms);
      request.setArrayOfUserPermissions(arrayOfUserPermission);
    }

    if(groupPerms != null) {
      for(int i = 0; i < groupPerms.length; i++) {
        TGroupPermission gp = groupPerms[i];
        inputVec.addElement("GroupPermission.UserID="+gp.getGroupID());
        inputVec.addElement("GroupPermission.Mode="+gp.getMode());
        if(_debug) {
          util.printMessage("SRM-UTIL:GroupPermission.UserID="+gp.getGroupID(),
			logger,silent); 
          util.printMessage("SRM-UTIL:GroupPermission.Mode="+gp.getMode(),logger,silent); 
        }
      }
      ArrayOfTGroupPermission arrayOfGroupPermission = 
			new ArrayOfTGroupPermission();
      arrayOfGroupPermission.setGroupPermissionArray(groupPerms);
      request.setArrayOfGroupPermissions(arrayOfGroupPermission);
    }

    //setStorageSystemInfo

    inputVec.addElement("TimeStamp="+new Date());
    util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);

    SrmSetPermissionResponse response = null;
    response = (SrmSetPermissionResponse) 
        callSoapThread(request,response,"srmsetpermission");


    if(_debug) {
    util.printMessage("\nSRM-UTIL: #### Output from SRM ####", logger,silent);
    }
 
    if(response == null) {
      inputVec.clear(); 
      inputVec.addElement("SRM returned null response for SrmSetPermission");
      util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
      util.printMessage
		("\nSRM-UTIL: SRM returned null response for srmSetPermission ",
			logger,silent);
    }
    else {
      TReturnStatus returnStatus = response.getReturnStatus();
      if(returnStatus == null) { 
        inputVec.clear(); 
        inputVec.addElement("SRM returned null return status for SrmSetPermission");
        util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
        util.printMessage
		("\nSRM-UTIL: SRM returned null return status for srmSetPermission ", 
			logger,silent);
      }
      else {
         TStatusCode statusCode = returnStatus.getStatusCode();
         sCode = statusCode;
         if(statusCode == null) {
          inputVec.clear(); 
          inputVec.addElement("SRM returned null status code for SrmSetPermission");
          util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
          util.printMessage 
			("\nSRM-UTIL: SRM returned null status code for srmSetPermission ", 
					logger,silent);
         }
         else {
           inputVec.clear(); 
           inputVec.addElement("StatusCode="+statusCode);
           inputVec.addElement("Explanation="+returnStatus.getExplanation());
           util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
           if(_debug) {
           util.printMessage("SRM-UTIL: .....................................",
				logger,silent);
           } 
           util.printMessage("\tStatusCode="+statusCode,logger,silent);
           util.printMessage("\tExplanation="+returnStatus.getExplanation(),
				logger,silent);
         }
      }
    }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   inputVec.clear(); 
   inputVec.addElement(e.getMessage());
   util.printEventLog(_theLogger,"SrmSetPermission",inputVec,silent,useLog);
   util.printMessage("SRM-UTIL: Exception " + e.getMessage(),logger,silent);
   util.printMessageHException("SRM-UTIL: Exception " + e.getMessage(),pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy type", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
   }
   else {
     throw e;
   }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doPing
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doPing() throws Exception {

 inputVec.clear(); 
 try {
  SrmPingRequest request = new SrmPingRequest();
  util.printMessage("\nSRM-PING: " + new Date() + "  Calling SrmPing Request..." , logger,silent);
  util.printMessageHL("\nSRM-PING: " + new Date() + "  Calling SrmPing Request...", pIntf);
  if(!uid.equals("")) {
    inputVec.addElement("UID="+uid);
    if(_debug) {
    util.printMessage("\nSRM-PING: .... Input parameters ....", logger,silent);
    util.printMessage("SRM-PING : UID="+uid,logger,silent);
    }
    request.setAuthorizationID(uid);
  }

  if(_srm == null) {
    inputVec.addElement("Cannot continue, connection failed with null srm");
    util.printEventLog(_theLogger,"doPing",inputVec,silent,useLog);
    util.printMessage("\nSRM-PING: Cannot continue, connection failed with null _srm", logger,silent);
    util.printMessageHException("\nSRM-PING: Cannot continue, connection failed with null _srm", pIntf);
    return ;
  }

  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SendingSrmPingRequest",inputVec,silent,useLog);

  SrmPingResponse response = null;

  response = (SrmPingResponse) callSoapThread(request,response,"srmping");

  if(_debug) {
    util.printMessage("\nSRM-PING   #### Output from SRM ####",logger,silent);
    util.printMessageHL("\nSRM-PING   #### Output from SRM ####",pIntf);
  }

  if(response != null) {
    inputVec.clear(); 
    
    String version = response.getVersionInfo();
    inputVec.addElement("PingVersionInfo="+version);
    util.printMessage("versionInfo=" + version+"\n",
            logger, silent);
    util.printMessage("versionInfo=" + version+"\n", pIntf);
    ArrayOfTExtraInfo aExtraInfo = response.getOtherInfo();
    TExtraInfo[] info = null;
    if(aExtraInfo != null) {
      info = aExtraInfo.getExtraInfoArray();
      if(info != null && info.length > 0)  {
        inputVec.addElement("ExtraInformation");
        util.printMessage("Extra information (Key=Value)", logger, silent);
        util.printMessage("Extra information (Key=Value)", pIntf);
       for(int i = 0; i < info.length; i++) {
         inputVec.addElement("Key="+info[i].getKey());
         inputVec.addElement("Value="+info[i].getValue());
         util.printMessage(info[i].getKey()+"="+info[i].getValue(),
				logger, silent);
         util.printMessage(info[i].getKey()+"="+info[i].getValue(),pIntf);
         /*
         util.printMessage("\tKey="+info[i].getKey(),logger, silent);
         util.printMessage("\tValue="+info[i].getValue(),logger, silent);
         util.printMessage("\tKey="+info[i].getKey(),pIntf);
         util.printMessage("\tValue="+info[i].getValue(),pIntf);
         */
        }
      }
      else {
        inputVec.addElement("Ping did not return any extra information");
        util.printMessage("\nPing did not return any extra information", 
			logger, silent);
        util.printMessage("\nPing did not return any extra information",pIntf);
      }
      util.printEventLog(_theLogger,"SrmPingResponse",inputVec,silent,useLog);
    }
   }
   if(pIntf != null) {
     pIntf.setCompleted(true);
   }
  } 
  catch(Exception e) {
    util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement(e.getMessage());
    util.printEventLog(_theLogger,"SrmPingResponse",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
    int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    int idx6 = msg.indexOf("java.net.SocketTimeoutException: Read timed out");
    util.printMessage("SRM-PING : Exception " + e.getMessage(),logger,silent);
    util.printMessageHException("SRM-PING : Exception " + e.getMessage(),pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
     pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
      if(pIntf == null) {
             util.printMessage("\nException : "+msg,logger,silent);
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      if(idx6 != -1) {
         inputVec.clear(); 
         inputVec.addElement("ExitStatus="+90);
         util.printEventLog(_theLogger,
			"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
            util.printMessage("\nException : proxy type mismatch, " +
				"please check you proxy type",logger,silent);
            inputVec.clear();
            inputVec.addElement("ExitStatus="+96);
            util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
            System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// extendFileLifeTime
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode extendFileLifeTime(Vector fileInfo) 
	throws Exception {

 util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling srmExtendFileLifeTimeInSpace request ...", logger,silent);
 util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling srmExtendFileLifeTimeInSpace request ...", pIntf);

 TStatusCode sCode = null;
 try {
   if(_debug) {
   util.printMessage("SRM-CLIENT: ::::::::: Input parameters :::::",
		logger,silent);
   util.printMessage("SRM-CLIENT: AuthId="+uid,logger,silent);
   util.printMessage("SRM-CLIENT: SpaceToken="+fileToken,logger,silent);
   util.printMessage("SRM-CLIENT: NewLifeTime="+tokenLifetime,logger,silent);
   util.printMessage("SRM-CLIENT: ::::::::: Input parameters :::::", pIntf);
   util.printMessage("SRM-CLIENT: AuthId="+uid,pIntf);
   util.printMessage("SRM-CLIENT: SpaceToken="+fileToken,pIntf);
   util.printMessage("SRM-CLIENT: NewLifeTime="+tokenLifetime,pIntf);
   }

   inputVec.clear(); 
   inputVec.addElement("AuthId="+uid);
   inputVec.addElement("SpaceToken="+fileToken);
   inputVec.addElement("NewLifeTime="+tokenLifetime);
   
   URI[] aURI = new URI[fileInfo.size()];

   if(fileInfo != null && fileInfo.size() > 0) {
     for(int i = 0; i < fileInfo.size(); i++) {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String siteUrl = fIntf.getOrigSURL();
       util.printMessage("SRM-CLIENT: SURL="+siteUrl,logger,silent);
       util.printMessage("SRM-CLIENT: SURL="+siteUrl,pIntf);
       inputVec.addElement("SURL("+i+")="+siteUrl);
       aURI[i] = new URI(siteUrl);
     }
   }

   SrmExtendFileLifeTimeInSpaceRequest req = 
		new SrmExtendFileLifeTimeInSpaceRequest();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   }
   req.setSpaceToken(fileToken);
   if(tokenLifetime != 0) {
     req.setNewLifeTime(new Integer(tokenLifetime));
   }
   if(fileInfo != null && fileInfo.size() > 0) {
     req.setArrayOfSURLs(SRMUtilClient.convertToArrayOfAnyURI(aURI));
   }
   else {
     inputVec.clear(); 
     inputVec.addElement
		("Extending srmExtendLifeTime for all files in this space token");
     util.printEventLog(_theLogger,"SrmExtendLifeTime",inputVec,silent,useLog);
     util.printMessage (
		"\tSRM-CLIENT: Extending srmExtendFileLifeTime for all " +
	 	   "files in this space token", logger,silent); 
     util.printMessage (
		"\tSRM-CLIENT: Extending srmExtendFileLifeTime for all " +
	 	   "files in this space token", pIntf); 
   }

   SrmExtendFileLifeTimeInSpaceResponse response = null;

   response = (SrmExtendFileLifeTimeInSpaceResponse) 
        callSoapThread(req,response,"srmextendfilelifetimeinspace");

   if(_debug) {
   util.printMessage("SRM-CLIENT: ..............................",
		logger,silent);
   util.printMessage("SRM-CLIENT: ..............................", pIntf);
   }

   if(response == null) {
     inputVec.clear(); 
     inputVec.addElement(
		"SRM returned null response for SrmExtendFileLifeTime request");
     util.printEventLog(_theLogger,"SendingExtendLifeTime",inputVec,silent,useLog);
     util.printMessage
		("\nSRM-CLIENT: SRM returned null response for SrmExtendFileLifeTime " +
			"request", logger,silent);
     util.printMessage
		("\nSRM-CLIENT: SRM returned null response for SrmExtendFileLifeTime " +
			"request", pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TReturnStatus rStatus = response.getReturnStatus();

   if(rStatus == null) {
     inputVec.clear(); 
     inputVec.addElement("Null return status from SRM");
     util.printEventLog(_theLogger,"SendingExtendLifeTime",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: Null return status from SRM",
			logger,silent);
     util.printMessage("\nSRM-CLIENT: Null return status from SRM", pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TStatusCode code = rStatus.getStatusCode();
   sCode = rStatus.getStatusCode();
   inputVec.clear(); 
   inputVec.addElement("Status="+code);
   inputVec.addElement("Explanation="+rStatus.getExplanation());
   util.printEventLog(_theLogger,"SendingExtendLifeTime",inputVec,silent,useLog);
   util.printMessage("\tstatus="+code,logger,silent);
   util.printMessage("\texplanation="+rStatus.getExplanation(),logger,silent);
   util.printMessage("\tstatus="+code,pIntf);
   util.printMessage("\texplanation="+rStatus.getExplanation(),pIntf);

   if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
       return sCode;
   }

   ArrayOfTSURLLifetimeReturnStatus aLifeTimeReturnStatus = 
		response.getArrayOfFileStatuses();
   
   if(aLifeTimeReturnStatus == null) {
     inputVec.clear(); 
     inputVec.addElement("SRM returned null getArrayOfFileStatuses for ");
     inputVec.addElement("SrmExtendFileTimeInSpace request");
     util.printEventLog(_theLogger,"SendingExtendLifeTime",inputVec,silent,useLog);
     //util.printMessage
	  //("\nSRM-CLIENT: SRM returned null getArrayOfFileStatuses for " +
		//"SrmExtendFileTimeInSpace request", logger,silent);
     //util.printMessage
	  //("\nSRM-CLIENT: SRM returned null getArrayOfFileStatuses for " +
		//"SrmExtendFileTimeInSpace request", pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TSURLLifetimeReturnStatus[] lifeTimeReturnStatus = 
		aLifeTimeReturnStatus.getStatusArray(); 
   
   if(lifeTimeReturnStatus == null) {
     inputVec.clear(); 
     inputVec.addElement("SRM returned null getStatusArray for ");
     inputVec.addElement("SrmExtendFileTimeInSpace request");
     util.printEventLog(_theLogger,"SendingExtendLifeTime",inputVec,silent,useLog);
     //util.printMessage
	  //("\nSRM-CLIENT: SRM returned null getStatusArray for " +
		//"SrmExtendFileTimeInSpace request", logger,silent);
     //util.printMessage
	  //("\nSRM-CLIENT: SRM returned null getStatusArray for " +
		//"SrmExtendFileTimeInSpace request", pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }
   
   for(int i = 0; i < lifeTimeReturnStatus.length; i++) {
       inputVec.clear(); 
       TSURLLifetimeReturnStatus lStatus = lifeTimeReturnStatus[i];
       util.printMessage("\tSURL="+lStatus.getSurl(),logger,silent);
       util.printMessage("\tSURL="+lStatus.getSurl(),pIntf);
       inputVec.addElement("SURL="+lStatus.getSurl());
       TReturnStatus lRStatus = lStatus.getStatus();
       if(lRStatus == null) {
         util.printMessage("\tSRM-CLIENT: Null return status",logger,silent);
         util.printMessage("\tSRM-CLIENT: Null return status",pIntf);
         inputVec.addElement("Null return status");
       }
       else {
         inputVec.addElement("status="+lRStatus.getStatusCode());
         inputVec.addElement("explanation="+lRStatus.getExplanation());
         util.printMessage("\tstatus="+lRStatus.getStatusCode(),logger,silent);
         util.printMessage("\tstatus="+lRStatus.getStatusCode(),pIntf);
         util.printMessage
			("\texplanation="+lRStatus.getExplanation(),logger,silent);
         util.printMessage
			("\texplanation="+lRStatus.getExplanation(),pIntf);
       }
       inputVec.addElement("FileLifeTime="+lStatus.getFileLifetime());
       inputVec.addElement("PinLifeTime="+lStatus.getPinLifetime());
       util.printEventLog(_theLogger,"SrmExtendFileTimeInSpace",
			inputVec,silent,useLog);
       util.printMessage("\tFileLifeTime="+lStatus.getFileLifetime(),
			logger,silent);
       util.printMessage("\tPinLifeTime="+lStatus.getPinLifetime(),
			logger,silent);
       util.printMessage("\tFileLifeTime="+lStatus.getFileLifetime(),
			pIntf);
       util.printMessage("\tPinLifeTime="+lStatus.getPinLifetime(),
			pIntf);
   }
   if(rStatus.getStatusCode().toString().equals("SRM_SUCCESS")) { 
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
   }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement(e.getMessage());
   util.printEventLog(_theLogger,"SrmExtendFileTimeInSpace",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-CLIENT: Exception " + e.getMessage(),logger,silent);
   util.printMessageHException("SRM-CLIENT: Exception " + e.getMessage(),pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch, " +
				"please check your proxy type", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
     else {
       pIntf.setCompleted(false);
     }
   }
   else {
     throw e;
   }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(boolean keepSpace, String requestType, 
	Vector fileInfo) throws Exception {

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   if(requestType.equalsIgnoreCase("releasefile") && fileInfo.size() == 0) {
       return SRMUtilClient.releaseFile(_srm,keepSpace,"",
	       requestToken,uid,proxyString, _debug, 
               logger,pIntf,_credential,proxyType,serverUrl,
               _theLogger, pIntf, _srmClientIntf, silent, useLog, 
               connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
               delegationNeeded, numRetry, retryTimeOut );
   }
   else {
     for(int i = 0; i < fileInfo.size(); i++) {
       String siteUrl = "";
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       if(requestType.equalsIgnoreCase("put")) {
         siteUrl = fIntf.getOrigTURL();
       }
       else {
         siteUrl = fIntf.getOrigSURL();
       }
       return SRMUtilClient.releaseFile(_srm,keepSpace,siteUrl,
	   requestToken,uid,proxyString, _debug,logger,pIntf,_credential,
           proxyType,serverUrl,_theLogger, 
           pIntf, _srmClientIntf, silent, useLog, 
           connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
           delegationNeeded, numRetry, retryTimeOut);
    }
  }
  return null;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSpaceToken
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode getSpaceToken(String spaceTokenDesc) throws Exception {

 TStatusCode sCode = null;
 try {
  util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling srmGetSpaceTokens ...", logger,silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling srmGetSpaceTokens ...", pIntf);
  if(_debug) {
  util.printMessage("SRM-CLIENT: :::: Input parameter values :::::",logger,silent);
  util.printMessageHL("SRM-CLIENT: :::: Input parameter values :::::",pIntf);
  util.printMessage("SRM-CLIENT: AuthorizationID=" + uid,logger,silent);
  util.printMessage("SRM-CLIENT: AuthorizationID=" + uid,pIntf);
  }

  inputVec.clear(); 
  inputVec.addElement("AuthorizationID="+uid);

  SrmGetSpaceTokensRequest request = new SrmGetSpaceTokensRequest();
  if(!uid.equals("")) {
    request.setAuthorizationID(uid); 
  }
  if(!spaceTokenDesc.equals("")) {
    request.setUserSpaceTokenDescription(spaceTokenDesc);
    inputVec.addElement("SpaceTokenDescription="+spaceTokenDesc);
    util.printMessage("SRM-CLIENT: SpaceToken Description=" + spaceTokenDesc, 
			logger,silent);
    util.printMessage("SRM-CLIENT: SpaceToken Description=" + spaceTokenDesc, 
			pIntf);
  }
  
  util.printEventLog(_theLogger,"SrmGetSpaceTokensRequest",inputVec,silent,useLog);

  SrmGetSpaceTokensResponse result = null;

  result = 
    (SrmGetSpaceTokensResponse) callSoapThread(request,result,"srmgetspacetokens");

  util.printMessage("SRM-CLIENT: ...................................",logger,silent);
  util.printMessage("SRM-CLIENT: ...................................",pIntf);

  if(result == null) {
     inputVec.clear(); 
     inputVec.addElement("SRM returned null result for SrmGetSpaceToken");
     util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: SRM returned null result for srmGetSpaceToken", logger,silent);
     util.printMessageHException("SRM-CLIENT: SRM returned null result for srmGetSpaceToken", pIntf);
     return sCode;
  }

  TReturnStatus returnStatus = result.getReturnStatus();
  if(returnStatus == null) {
     inputVec.clear(); 
     inputVec.addElement("Null return status from SRM for srmGetSpaceToken");
     util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: Null return status from SRM for srmGetSpaceToken", logger,silent); 
     util.printMessageHException("\nSRM-CLIENT: Null return status from SRM for srmGetSpaceToken", pIntf); 
     return sCode;
  }

  TStatusCode statusCode = returnStatus.getStatusCode();
  sCode = statusCode;
  if(statusCode == null) {
     inputVec.clear(); 
     inputVec.addElement("Null statusCode from srmGetSpaceToken");
     util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: Null statusCode from srmGetSpaceToken", 
			logger,silent);
     util.printMessageHException("\nSRM-CLIENT: Null statusCode from srmGetSpaceToken", 
			pIntf);
     return sCode;
  }

  util.printMessage("\tStatus="+ statusCode.getValue(),logger,silent);
  util.printMessage("\tStatus="+ statusCode.getValue(),pIntf);
  String explanation = returnStatus.getExplanation();
  util.printMessage("\tExplanation="+ explanation,logger,silent);
  util.printMessage("\tExplanation="+ explanation,pIntf);
  inputVec.clear(); 
  inputVec.addElement("Status="+statusCode.getValue());
  inputVec.addElement("Explanation="+explanation);

  if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
       return sCode;
  }

  ArrayOfString spaceTokens = result.getArrayOfSpaceTokens();
  if(spaceTokens == null) {
     //util.printMessage("SRM-CLIENT: SRM returned null ArrayOfSpaceTokens for " +
			//"srmGetSpaceToken", logger,silent);
     //util.printMessageHException
     //	("SRM-CLIENT: SRM returned null ArrayOfSpaceTokens for " + 
     //		"srmGetSpaceToken", pIntf);
     inputVec.addElement("SRM returned null ArrayOfSpaceTokens for srmGetSpaceToken");
     util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
      if(pIntf != null) {
        pIntf.setCompleted(true);
      }
     return sCode;
  }

  String[] tokens = spaceTokens.getStringArray();
  if(tokens == null) {
     inputVec.addElement("SRM returned null tokens for srmGetSpaceToken");
     //util.printMessage("SRM-CLIENT: SRM returned null tokens for srmGetSpaceToken", logger,silent);
     //util.printMessageHException("SRM-CLIENT: SRM returned null tokens for srmGetSpaceToken", pIntf);
     util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
      if(pIntf != null) {
        pIntf.setCompleted(true);
      }
     return sCode;
  }

  for(int i = 0; i < tokens.length; i++)  {
     String stoken = tokens[i];
     if(stoken != null) {
       util.printMessage("SRM-CLIENT ("+i+")SpaceToken="+stoken,logger,silent);
       util.printMessage("SRM-CLIENT ("+i+")SpaceToken="+stoken,pIntf);
       inputVec.addElement("("+i+")SpaceToken="+stoken);
     }
  }
  util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
  if(pIntf != null) {
    pIntf.setCompleted(true);
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear();
   inputVec.addElement(e.getMessage());
   util.printEventLog(_theLogger,"SrmGetSpaceTokensResponse",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-CLIENT: Exception " + e.getMessage(),logger,silent);
   util.printMessageHException("SRM-CLIENT: Exception " + e.getMessage(),pIntf);
   util.printHException(e,pIntf);
   if(pIntf != null) {
     pIntf.setCompleted(false);
   }
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch, " +
					" please check your proxy type", logger,silent);
             inputVec.clear();
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//isServerGateWayEnabled
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean isServerGateWayEnabled(ISRM srm, String uid,
     Log logger, boolean _debug, GSSCredential credential,
     int proxyType, String serverUrl, java.util.logging.Logger _theLogger,
     PrintIntf pIntf, SRMClientIntf _srmClientIntf, boolean silent,
     boolean useLog, int connectionTimeOutAllowed,
     int setHTTPConnectionTimeOutAllowed, 
     String delegationNeeded, int numRetry, int retryTimeOut) 
         throws Exception {

 Vector inputVec = new Vector ();
 boolean value=false;;

 try {
   util.printMessage("\nSRM-MISC  " + new Date() +
		" Calling srmPing request...", logger,silent);
   if(_debug) {
     util.printMessage("\nSRM-MISC: ... Input parameters ...", logger,silent);
     util.printMessage("SRM-MISC: AuthorizationID="+uid,logger,silent);
   }
   inputVec.addElement("AuthorizationID="+uid);
   SrmPingRequest req = new SrmPingRequest();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   } 

   util.printEventLog(_theLogger,"isServerGateWayEnabled", 
                inputVec,silent,useLog);

   SrmPingResponse response = null;

   response = (SrmPingResponse) 
        SRMUtilClient.callStaticSoapThread(srm,credential, proxyType, 
        serverUrl, req,response,"srmping",_theLogger,
        pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
        setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
        retryTimeOut);

   if(_debug) {
   util.printMessage("\nSRM-MISC  #### Output from SRM ####",logger,silent);
   }

   inputVec.clear(); 
   if(response == null) {
     inputVec.addElement("Srm returned null response for SrmPing"); 
     util.printEventLog(_theLogger,"isServerGateWayEnabled", 
                inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null response for SrmPing", 
		logger,silent);
     return value;
   }

   ArrayOfTExtraInfo aInfo = response.getOtherInfo();

   if(aInfo == null) {
     inputVec.addElement("Srm returned null otherInfo");
     util.printEventLog(_theLogger,"isServerGateWayEnabled", 
                inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null otherInfo ", 
                logger,silent);
     return value;
   }

   TExtraInfo[] eInfos =  aInfo.getExtraInfoArray();

   if(eInfos == null) {
     inputVec.addElement("Srm returned null extrainfo"); 
     util.printEventLog(_theLogger,"isServerGateWayEnabled", 
                inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null extrainfo ", 
                logger,silent);
     return value;
   }

   int count = 0;
   for(int i = 0; i < eInfos.length; i++) {
     TExtraInfo eInfo = eInfos[i];
     inputVec.addElement("Key="+eInfo.getKey());
     inputVec.addElement("Value="+eInfo.getValue());
     util.printMessage("\t"+eInfo.getKey()+"="+eInfo.getValue(),logger,silent);
     if(eInfo.getKey().equalsIgnoreCase("GatewayMode") &&
        eInfo.getValue().equalsIgnoreCase("Enabled")) {
       count ++;
     }
     if(eInfo.getKey().equalsIgnoreCase("backend_type") &&
        eInfo.getValue().equalsIgnoreCase("bestman")) {
       count ++;
     }
     if(count == 2) {
       return true;
     }
   }

   util.printEventLog(_theLogger,"isServerGateWayEnabled", 
        inputVec,silent,useLog);
   return value;
 } 
 catch(Exception e) {
   util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"isServerGateWayEnabled", 
        inputVec,silent,useLog);
   return value;
 }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getISRM
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ISRM getISRM() throws Exception {
  return _srm;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSpaceTokenMeta
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static TStatusCode getSpaceTokenMeta(ISRM srm, String fileToken, 
     Log logger, boolean _debug, GSSCredential credential,
     int proxyType, String serverUrl, java.util.logging.Logger _theLogger,
        PrintIntf pIntf, SRMClientIntf srmClientIntf, boolean silent,
        boolean useLog, int connectionTimeOutAllowed,
        int setHTTPConnectionTimeOutAllowed, 
        String delegationNeeded, int numRetry, int retryTimeOut) 
                        throws Exception {

 TStatusCode sCode = null;
 Vector iVec = new Vector ();
 try {
   util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmGetSpaceMetaDataRequest ...", logger, silent);
   util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmGetSpaceMetaDataRequest ...", pIntf);

   if(fileToken == null)  { 
     iVec.addElement("Cannot do getSpaceTokenMeta, since fileToken is null");
     util.printEventLog(_theLogger,"GetSpaceTokenMeta",iVec,silent,useLog);
     util.printMessage(
		"SRM-CLIENT: Cannot do getSpaceTokenMeta, since fileToken is null",
			logger,silent);
     util.printMessageHException(
		"SRM-CLIENT: Cannot do getSpaceTokenMeta, since fileToken is null",
			pIntf);
     return sCode;
   }

   SrmGetSpaceMetaDataRequest req = new SrmGetSpaceMetaDataRequest();

   String[] tokenArray = new String[1];
   tokenArray[0] = fileToken;
		
   ArrayOfString aTokenArray = new ArrayOfString();
   aTokenArray.setStringArray(tokenArray);
   req.setArrayOfSpaceTokens(aTokenArray);

   if(_debug) {
   util.printMessage("SRM-CLIENT: ......................",logger,silent);
   util.printMessage("SRM-CLIENT: ......................",pIntf);
   util.printMessage("SRM-CLIENT: ....Input parameters for srmGetSpaceMetaData .....", logger,silent);
   util.printMessageHL("SRM-CLIENT: ....Input parameters for srmGetSpaceMetaData .....", pIntf);
   util.printMessage("SRM-CLIENT: SpaceToken="+fileToken,logger,silent);
   util.printMessage("SRM-CLIENT: SpaceToken="+fileToken,pIntf);
   }
   iVec.addElement("SpaceToken="+fileToken);
	

   iVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"SrmGetSpaceMetaDataRequest",iVec,silent,useLog);
   SrmGetSpaceMetaDataResponse result = null;

   result = (SrmGetSpaceMetaDataResponse) 
        SRMUtilClient.callStaticSoapThread(srm,credential, proxyType, 
        serverUrl, req,result,"srmgetspacemetadata",_theLogger,
        pIntf, null, silent, useLog, connectionTimeOutAllowed,
        setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
        retryTimeOut);

   if(_debug) {
   util.printMessage("SRM-CLIENT: ............................",logger,silent);
   util.printMessage("SRM-CLIENT: ............................",pIntf);
   }

   if(result == null) {
      iVec.clear();
      iVec.addElement("Null result for the srmGetSpaceMetaDataRequest ");
      util.printEventLog(_theLogger,"SrmGetSpaceMetaDataResponse",iVec,silent,useLog);
      util.printMessage
		("\nSRM-CLIENT: Null result for the srmGetSpaceMetaDataRequest ",
		 logger,silent);
      util.printMessageHException
		("\nSRM-CLIENT: Null result for the srmGetSpaceMetaDataRequest",pIntf);
      return sCode;
   }

   if(result.getReturnStatus() == null) {
      iVec.clear();
      iVec.addElement("\nNull ReturnStatus for the srmGetSpaceMetaDataRequest");
      util.printEventLog(_theLogger,"SrmGetSpaceMetaDataResponse",iVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: Null ReturnStatus for the " +
		"srmGetSpaceMetaDataRequest ", logger,silent);
      util.printMessageHException("\nSRM-CLIENT: Null ReturnStatus for the " +
		"srmGetSpaceMetaDataRequest ", pIntf);
      return sCode;
   }

   if(result.getReturnStatus().getStatusCode() == null) {
      iVec.clear();
      iVec.addElement("Null StatusCode for the srmGetSpaceMetaDataRequest ");
      util.printEventLog(_theLogger,"SrmGetSpaceMetaDataResponse",iVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: Null StatusCode for the " +
		"srmGetSpaceMetaDataRequest ", logger,silent);
      util.printMessageHException("\nSRM-CLIENT: Null StatusCode for the " +
		"srmGetSpaceMetaDataRequest ", pIntf);
      return sCode;
   }

   sCode = result.getReturnStatus().getStatusCode();

   //if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {

       util.printMessage("\nSRM-CLIENT:  ....space token details ....",
			logger,silent);
       util.printMessage("\nSRM-CLIENT:  ....space token details ....",
			pIntf);

       util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(), 
				logger, silent);
       util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(), 
				pIntf);
       util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), logger, silent);
       util.printMessage("\texplanation="+
		  result.getReturnStatus().getExplanation(), pIntf);
       iVec.addElement("Status="+result.getReturnStatus().getStatusCode());
          
       if(result.getArrayOfSpaceDetails() == null) { 
          util.printMessage("\n\tSRM-CLIENT: Null array of space details", 
			logger,silent);
          util.printMessage("\n\tSRM-CLIENT: Null array of space details", 
			pIntf);
          iVec.addElement("Null arrary of space details");
          return sCode;
       }

       ArrayOfTMetaDataSpace aMetaSpace = result.getArrayOfSpaceDetails();
       if(aMetaSpace != null) {
       TMetaDataSpace[] metaSpace = aMetaSpace.getSpaceDataArray();
         
       for(int j = 0; j < metaSpace.length; j++) {
          if(metaSpace[j].getSpaceToken() != null) {
            util.printMessage("\tSpaceToken="+metaSpace[j].getSpaceToken(), 
				logger, silent);
            util.printMessage("\tSpaceToken="+metaSpace[j].getSpaceToken(), 
				pIntf);
            iVec.addElement("SpaceToken="+metaSpace[j].getSpaceToken());
          }

          if(metaSpace[j].getTotalSize() != null) {
            util.printMessage("\tTotalSize="+metaSpace[j].getTotalSize(), 
				logger, silent);
            util.printMessage("\tTotalSize="+metaSpace[j].getTotalSize(), 
				pIntf);
            iVec.addElement("TotalSize="+metaSpace[j].getTotalSize());
          }

          if(metaSpace[j].getOwner() != null) {
            util.printMessage("\tOwner="+metaSpace[j].getOwner(), 
				logger, silent);
            util.printMessage("\tOwner="+metaSpace[j].getOwner(), pIntf);
            iVec.addElement("Owner="+metaSpace[j].getOwner());
          }

          if(metaSpace[j].getLifetimeAssigned() != null) {
            util.printMessage("\tLifetimeAssigned="+
				metaSpace[j].getLifetimeAssigned(), logger, silent);
            util.printMessage("\tLifetimeAssigned="+
				metaSpace[j].getLifetimeAssigned(), pIntf);
            iVec.addElement("LifetimeAssigned="+metaSpace[j].getLifetimeAssigned());
          }

          if(metaSpace[j].getLifetimeLeft() != null) {
             util.printMessage("\tLifetimeLeft="+
				metaSpace[j].getLifetimeLeft(), logger, silent);
             util.printMessage("\tLifetimeLeft="+
				metaSpace[j].getLifetimeLeft(), pIntf);
             iVec.addElement("LifetimeLeft="+metaSpace[j].getLifetimeLeft()); 
          }

          if(metaSpace[j].getUnusedSize() != null) {
             util.printMessage("\tUnusedSize="+metaSpace[j].getUnusedSize(), 
				logger, silent);
             util.printMessage("\tUnusedSize="+metaSpace[j].getUnusedSize(), 
				pIntf);
             iVec.addElement("UnusedSize="+metaSpace[j].getUnusedSize());
          }

          if(metaSpace[j].getGuaranteedSize() != null) {
             util.printMessage("\tGuaranteedSize="+metaSpace[j].getGuaranteedSize(), logger, silent);
             util.printMessage("\tGuaranteedSize="+metaSpace[j].getGuaranteedSize(), pIntf);
             iVec.addElement("GuaranteedSize="+metaSpace[j].getGuaranteedSize());
          }

          if(metaSpace[j].getRetentionPolicyInfo() != null) {
            TRetentionPolicyInfo retentionPolicyInfo = 
				metaSpace[j].getRetentionPolicyInfo();
            if(retentionPolicyInfo != null) {
               TRetentionPolicy retentionPolicy = 
					retentionPolicyInfo.getRetentionPolicy();
               TAccessLatency accessLatency = retentionPolicyInfo.getAccessLatency();
               if(retentionPolicy != null) {
                  util.printMessage("\tRetentionPolicy="+retentionPolicy.getValue(), logger, silent);
                  util.printMessage("\tRetentionPolicy="+retentionPolicy.getValue(), pIntf);
                  iVec.addElement("RetentionPolicy="+retentionPolicy.getValue());
               }
               if(accessLatency != null) {
                  util.printMessage("\tAccessLatency="+accessLatency.getValue(), logger, silent);
                  util.printMessage("\tAccessLatency="+accessLatency.getValue(), pIntf);
                  iVec.addElement("AccessLatency="+accessLatency.getValue());
               }
            }
          }
          if(metaSpace[j].getStatus() != null) {
            util.printMessage("\tmetaspace.status="+metaSpace[j].getStatus().getStatusCode(), logger, silent);
            util.printMessage("\tmetaspace.status="+metaSpace[j].getStatus().getStatusCode(), pIntf);
            iVec.addElement("status="+metaSpace[j].getStatus().getStatusCode());
            util.printMessage("\tmetaspace.explanation="+
					metaSpace[j].getStatus().getExplanation(), logger, silent);
            util.printMessage("\tmetaspace.explanation="+
					metaSpace[j].getStatus().getExplanation(), pIntf);
            iVec.addElement("explanation="+
				metaSpace[j].getStatus().getExplanation());
          }
      }
      } 
      util.printEventLog(_theLogger,"GetSpaceMetaDataResponse",iVec,silent,useLog);
     //} 
     /*
     else {
        iVec.clear();
        iVec.addElement("Code="+result.getReturnStatus().getStatusCode());
        iVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
        util.printEventLog(_theLogger,"GetSpaceMetaDataResponse",iVec,silent,useLog);
        util.printMessage("\tstatus="+ 
	   	  result.getReturnStatus().getStatusCode(),logger,silent);
        util.printMessage("\tstatus="+ 
	   	  result.getReturnStatus().getStatusCode(),pIntf);
        util.printMessage("\texplanation="+
	  	  result.getReturnStatus().getExplanation(),logger,silent);
        util.printMessage("\texplanation="+
	  	  result.getReturnStatus().getExplanation(),pIntf);
     }
     */
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    iVec.clear();
    iVec.addElement(e.getMessage());
    util.printEventLog(_theLogger,"GetSpaceMetaDataResponse",iVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Exception : " + e.getMessage(),logger,silent);
    util.printMessageHException("SRM-CLIENT: Exception : " + e.getMessage(),pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
      if(idx != -1) {
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy type", logger,silent);
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(96);
      }
      else {
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// removeFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String  removeFile(ISRM srm, boolean keepSpace, 
	String surl, String token, String uid, Log logger, 
        GSSCredential credential, int proxyType, String serverUrl,
        java.util.logging.Logger _theLogger,
        PrintIntf pIntf, SRMClientIntf _srmClientIntf,boolean silent,
        boolean useLog, int connectionTimeOutAllowed,
        int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded, int numRetry, int retryTimeOut) 
           throws Exception {

 String code = null;
 Vector iVec = new Vector ();
 try {
  util.printMessage("......................................",logger,silent);

  if (token == null || token.equals("")) {
    iVec.addElement("Cannot do remove files with null token");
    util.printEventLog(_theLogger,"RemoteFile",iVec,silent,useLog);
    util.printMessage("Cannot do remove files with null token",logger,silent);
    code = "INPUT_ERROR";
    return code;
  } 
  else {
    util.printMessage("::: Input parameters for SrmRm :::: " , logger,silent);
    util.printMessage("SURL="+ surl,logger,silent);
    util.printMessage("keepSpace?="+keepSpace,logger,silent);
    util.printMessage("token="+token,logger,silent);
    util.printMessage("AuthId="+uid,logger,silent);
    iVec.clear(); 
    iVec.addElement("SURL="+ surl);
    iVec.addElement("keepSpace="+keepSpace);
    iVec.addElement("token="+token);
    iVec.addElement("AuthId="+uid);
  }
		
  iVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmRmRequest",iVec,silent,useLog);
  SrmRmRequest req = new SrmRmRequest();
  //req.setKeepSpace(new Boolean(keepSpace));
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
  }
  //req.setRequestToken(token);
  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (surl);
  req.setArrayOfSURLs(SRMUtilClient.formArrayOfAnyURI(uri));

  iVec.clear(); 
  SrmRmResponse result = null;
  result = (SrmRmResponse) SRMUtilClient.callStaticSoapThread
                (srm,credential, proxyType, serverUrl, req,result,"srmrm",
                 _theLogger, pIntf, _srmClientIntf, silent, useLog, 
                 connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
                 delegationNeeded, numRetry, retryTimeOut);

  if (result == null) {
    iVec.addElement("SRM returned null result for the srmRm request");
    util.printEventLog(_theLogger,"SrmRmResponse",iVec,silent,useLog);
    util.printMessage("\tSRM returned null result for the srmRm request",
              logger,silent);
    code = "SRM_RETURNED_NO_STATUS";
    return code;
  }

  if(result.getReturnStatus() == null) { 
    iVec.addElement("Null return status for srmRm request");
    util.printEventLog(_theLogger,"SrmRmResponse",iVec,silent,useLog);
    util.printMessage("\tNull return status for srmRm request", logger,silent);
    code = "SRM_RETURNED_NO_STATUS";
    return code;
  }

  if(result.getReturnStatus().getStatusCode() == null) { 
    iVec.addElement("Null return status code for srmRm request");
    util.printEventLog(_theLogger,"SrmRmResponse",iVec,silent,useLog);
    util.printMessage("\tNull return status code for srmRm request", logger,silent);
    code = "SRM_RETURNED_NO_STATUS";
    return code;
  }

  code = result.getReturnStatus().getStatusCode().toString();
  iVec.addElement("status="+result.getReturnStatus().getStatusCode());
  iVec.addElement("explanation="+result.getReturnStatus().getExplanation());
  util.printMessage("\tstatus="+
    result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\texplanation="+
    result.getReturnStatus().getExplanation(),logger,silent);

  if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
       return code;
  }

  if(result.getArrayOfFileStatuses() == null) {
     iVec.addElement("Null Array of FileStatuses in SrmRm");
     //util.printMessage("Null Array of FileStatuses in SrmRm ",logger,silent);
     return code;
  }

  TStatusCode temp = 
	result.getArrayOfFileStatuses().getStatusArray(0).
  	  getStatus().getStatusCode();
  util.printMessage("\tstatus="+ temp,logger,silent);
  iVec.addElement("details status="+ temp);
  
  String ttemp =
    result.getArrayOfFileStatuses().getStatusArray(0).
	  getStatus().getExplanation();
  util.printMessage("\texplanation="+ ttemp, logger,silent);
  iVec.addElement("details explanation="+ ttemp);
  util.printEventLog(_theLogger,"SrmRmResponse",iVec,silent,useLog);
  return code;
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   iVec.clear();
   iVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmRm",iVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
     idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy type", logger,silent);
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(96);
      }
      else {
             Vector iinputVec = new Vector();
             iinputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
  }
  return code;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String releaseFile(ISRM srm, boolean keepSpace, 
	String surl, String token, String uid, String proxyString,
	boolean debug, Log logger, PrintIntf pIntf, 
        GSSCredential credential, int proxyType, String serverUrl, 
        java.util.logging.Logger _theLogger,
        PrintIntf pIntfextra, SRMClientIntf _srmClientIntf,boolean silent,
        boolean useLog, int connectionTimeOutAllowed,
        int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded, int numRetry, int retryTimeOut) 
                throws Exception {

 String sCode = null;
 Vector iVec = new Vector ();
 try {
  util.printMessage("\nSRM-CLIENT:  ...Calling srmReleaseFiles...",logger,silent);
  util.printMessageHL("\nSRM-CLIENT:  ...Calling srmReleaseFiles...",pIntf);

  if ((token == null || token.equals("")) && surl.equals("")) {
    iVec.addElement("Cannot do release files with null token and empty surl");
    util.printEventLog(_theLogger,"ReleaseFile",iVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Cannot do release files with null token and empty surl", logger,silent); 
    util.printMessage("SRM-CLIENT: Cannot do release files with null token and empty surl", pIntf); 
    sCode = "INPUT_ERROR";
	return sCode;
  } 
  else {
    if(debug) {
    util.printMessage("\nSRM-CLIENT:  ...Input parameters for SrmReleaseFile...", logger,silent);
    util.printMessage("\nSRM-CLIENT:  ...Input parameters for SrmReleaseFile...", pIntf);
    iVec.addElement("SURL="+surl);
    iVec.addElement("Token="+token);
    iVec.addElement("AuthId="+uid);
    util.printMessage("SRM-CLIENT: SURL="+ surl,logger,silent);
    util.printMessage("SRM-CLIENT: SURL="+ surl,pIntf);
    //util.printMessage("\tkeepSpace?="+keepSpace,logger,silent);
    util.printMessage("SRM-CLIENT: token="+token,logger,silent);
    util.printMessage("SRM-CLIENT: token="+token,pIntf);
    util.printMessage("SRM-CLIENT: AuthId="+uid,logger,silent);
    util.printMessage("SRM-CLIENT: AuthId="+uid,pIntf);
    }
  }

  SrmReleaseFilesRequest req = new SrmReleaseFilesRequest();

  /*
  if(storageInfo) {

    ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
    TExtraInfo[] a_tExtra = new TExtraInfo[2];

    TExtraInfo tExtra = new TExtraInfo();
    tExtra.setKey("uid");
    tExtra.setValue("releasefiles");
    a_tExtra[0] = tExtra;
 
    tExtra = new TExtraInfo();
    tExtra.setKey("pwd");
    tExtra.setValue(proxyString);
    a_tExtra[1] = tExtra;
 
    storageInfoArray.setExtraInfoArray(a_tExtra);
    req.setStorageSystemInfo(storageInfoArray);
  }
  */
		
  req.setDoRemove(new Boolean(keepSpace));
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
  }
  req.setRequestToken(token);
  if(!surl.equals("")) {
    org.apache.axis.types.URI uri = new org.apache.axis.types.URI (surl);
    req.setArrayOfSURLs(SRMUtilClient.formArrayOfAnyURI(uri));
  }

  iVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmReleaseFilesRequest",iVec,silent,useLog);

  SrmReleaseFilesResponse result = null;
  result = (SrmReleaseFilesResponse) SRMUtilClient.callStaticSoapThread
        (srm,credential, proxyType, serverUrl, req,result,
        "srmreleasefiles",_theLogger,
        pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
        setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
        retryTimeOut);

  if(debug) {
  util.printMessage("\nSRM-CLIENT:  #### Output from SRM ####", logger,silent);
  util.printMessage("\nSRM-CLIENT:  #### Output from SRM ####", pIntf);
  }
  iVec.clear();
  if (result == null) {
    iVec.addElement("SRM returned null result for the srmReleaseFile request");
    util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
    util.printMessage("\nSRM-CLIENT: SRM returned null result for the srmReleaseFile request", logger,silent);
    util.printMessage("\nSRM-CLIENT: SRM returned null result for the srmReleaseFile request", pIntf);
    sCode = "SRM_RETURNED_NO_STATUS";
    return sCode;
  }

  if(result.getReturnStatus() == null) { 
    util.printMessage("\n\tSRM-CLIENT: Null return status for srmReleaseFileRequest", logger,silent);
    util.printMessage("\n\tSRM-CLIENT: Null return status for srmReleaseFileRequest", pIntf);
    iVec.addElement("Null return status for srmReleaseFileRequest");
    util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
    sCode = "SRM_RETURNED_NO_STATUS";
    return sCode;
  }

  if(result.getReturnStatus().getStatusCode() == null) { 
    util.printMessage("\nSRM-CLIENT: Null return status code for srmReleaseFileRequest", logger,silent);
    util.printMessage("\nSRM-CLIENT: Null return status code for srmReleaseFileRequest", pIntf);
    iVec.addElement("Null return status code for srmReleaseFileRequest");
    util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
    sCode = "SRM_RETURNED_NO_STATUS";
    return sCode;
  }

  sCode = result.getReturnStatus().getStatusCode().toString();
  iVec.addElement("status="+result.getReturnStatus().getStatusCode());
  iVec.addElement("explanation="+result.getReturnStatus().getExplanation());
  util.printMessage("\tstatus="+
    result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\texplanation="+
    result.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\tstatus="+ 
		result.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation="+
    result.getReturnStatus().getExplanation(),pIntf);

   if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
       return sCode;
   }

  if(result.getArrayOfFileStatuses() == null) {
     iVec.addElement("Null Array of FileStatuses in SrmReleaseFiles");
     util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
     //util.printMessage("\nSRM-CLIENT: Null Array of FileStatuses in SrmReleaseFiles",logger,silent);
     //util.printMessage("\nSRM-CLIENT: Null Array of FileStatuses in SrmReleaseFiles",pIntf);
     return sCode;
  }

  TSURLReturnStatus rStatus = 
		result.getArrayOfFileStatuses().getStatusArray(0);

  if(rStatus == null) {
     util.printMessage ("\nSRM-CLIENT: Null TSURLReturnStatus",logger,silent);
     util.printMessage ("\nSRM-CLIENT: Null TSURLReturnStatus",pIntf);
     iVec.addElement("Null TSURLReturnStatus");
     util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
     return sCode;
  }

  if(rStatus.getStatus() == null) {   
     util.printMessage ("\nSRM-CLIENT: Null TSURLReturnStatus.getStatus() ",logger,silent);
     util.printMessage ("\nSRM-CLIENT: Null TSURLReturnStatus.getStatus() ",
		pIntf);
     iVec.addElement("Null TSURLReturnStatus.getStatus()");
     util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
     return sCode;
  }


  TStatusCode temp = rStatus.getStatus().getStatusCode();

  util.printMessage("\tstatus="+ temp,logger,silent);
  util.printMessage("\tstatus="+ temp,pIntf);
  iVec.addElement("details.status="+temp);

  String ttemp = rStatus.getStatus().getExplanation();
  util.printMessage("\texplanation="+ ttemp, logger,silent);
  util.printMessage("\texplanation="+ ttemp, pIntf);
  iVec.addElement("details.explanation="+ttemp);
  util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
  return sCode;
 } 
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   iVec.clear();
   iVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmReleaseFilesResponse",iVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-CLIENT: " + msg, logger,silent);
   util.printMessage("SRM-CLIENT: " + msg, pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessage("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy", logger,silent);
             Vector iinputVec = new Vector ();
             iinputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(96);
      }
      else {
             Vector iinputVec = new Vector(); 
             iinputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
 }
 return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmGetProtocols
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmGetProtocols() throws Exception {
 inputVec.clear();
 TStatusCode sCode = null;
 try {
   util.printMessage("\nSRM-UTIL: " + new Date() + 
		" Calling SrmGetTransferProtocols", logger,silent);
   util.printMessageHL("\nSRM-UTIL: " + new Date() + 
		" Calling SrmGetTransferProtocols", pIntf);
   if(_debug) {
     util.printMessage("\nSRM-UTIL: ... Input parameters ...", logger,silent);
     util.printMessage("SRM-UTIL: AuthorizationID="+uid,logger,silent);
     util.printMessage("\nSRM-UTIL: ... Input parameters ...", pIntf);
     util.printMessage("SRM-UTIL: AuthorizationID="+uid,pIntf);
   }
   inputVec.addElement("AuthorizationID="+uid);
   SrmGetTransferProtocolsRequest req = new SrmGetTransferProtocolsRequest();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   }

   inputVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"SrmGetProtocolsRequest",inputVec,silent,useLog);
   
   SrmGetTransferProtocolsResponse response =  null;

   response = (SrmGetTransferProtocolsResponse) 
                callSoapThread(req,response,"srmgettransferprotocols");

   if(_debug) {
   util.printMessage("\nSRM-UTIL:  #### Output from SRM ####",logger,silent);
   util.printMessage("\nSRM-UTIL:  #### Output from SRM ####",pIntf);
   }

   inputVec.clear();

   if(response == null) {
     inputVec.addElement
		("Srm returned null response for SrmGetTransferProtocols");
     util.printEventLog(_theLogger,"SrmGetProtocolsResponse",inputVec,silent,useLog);
     util.printMessage
		("\nSRM-UTIL: Srm returned null response for SrmGetTransferProtocols ", 
		     logger,silent);
     util.printMessage
		("\nSRM-UTIL: Srm returned null response for SrmGetTransferProtocols ", 
		     pIntf);
     return sCode;
   }

   TReturnStatus rStatus = response.getReturnStatus();

   if(rStatus == null) {
     inputVec.addElement
		("Srm returned null return status for SrmGetTransferProtocols");
     util.printEventLog(_theLogger,"SrmGetProtocolsResponse",inputVec,silent,useLog);
     util.printMessage
		("\nSRM-UTIL: Srm returned null return status " +
			"for SrmGetTransferProtocols ", logger,silent);
     util.printMessage
		("\nSRM-UTIL: Srm returned null return status " +
			"for SrmGetTransferProtocols ", pIntf);
     return sCode;
   }

   sCode = rStatus.getStatusCode();
   inputVec.addElement("status="+rStatus.getStatusCode());
   inputVec.addElement("explanation="+rStatus.getExplanation());
   util.printMessage("\tstatus="+rStatus.getStatusCode(),logger,silent);
   util.printMessage("\texplanation="+rStatus.getExplanation(),logger,silent);
   util.printMessage("\tstatus="+rStatus.getStatusCode(),pIntf);
   util.printMessage("\texplanation="+rStatus.getExplanation(),pIntf);

   if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
      return sCode;
   }

   ArrayOfTSupportedTransferProtocol protocolInfo = response.getProtocolInfo();

   if(protocolInfo == null) {
     inputVec.addElement("Srm returned null protocolInfo");
     util.printEventLog(_theLogger,"SrmGetProtocolsResponse",inputVec,silent,useLog);
     //util.printMessage("\nSRM-UTIL: Srm returned null protocolInfo", 
		//logger,silent);
     //util.printMessage("\nSRM-UTIL: Srm returned null protocolInfo",pIntf);
     return sCode;
   }

   TSupportedTransferProtocol[] pInfos =  protocolInfo.getProtocolArray();

   if(pInfos == null) {
     inputVec.addElement("\nSrm returned null protocolarray");
     util.printEventLog(_theLogger,"SrmGetProtocolsResponse",inputVec,silent,useLog);
     //util.printMessage("\nSRM-UTIL: Srm returned null protocolarray ", 
		//logger,silent);
     //util.printMessage("\nSRM-UTIL: Srm returned null protocolarray ",pIntf);
     return sCode;
   }

   for(int i = 0; i < pInfos.length; i++) {
     TSupportedTransferProtocol pInfo = pInfos[i];
     inputVec.addElement("TransferProtocol="+pInfo.getTransferProtocol());
     util.printMessage("\tTransferProtocol="+pInfo.getTransferProtocol(),
		logger,silent);
     util.printMessage("\tTransferProtocol="+pInfo.getTransferProtocol(),
		pIntf);
     ArrayOfTExtraInfo attributes = pInfo.getAttributes();
     if(attributes != null) {
        TExtraInfo[] eInfos = attributes.getExtraInfoArray(); 
        if(eInfos != null) {
           for(int j = 0; j < eInfos.length; j++) {
             TExtraInfo eInfo = eInfos[j];
             inputVec.addElement("Key="+eInfo.getKey());
             inputVec.addElement("Value="+eInfo.getValue());
             util.printMessage("\t"+eInfo.getKey()+"="+eInfo.getValue(),
					logger,silent);
             util.printMessage("\t"+eInfo.getKey()+"="+eInfo.getValue(),pIntf);
           }
        }
     }
   }
   util.printEventLog(_theLogger,"SrmGetProtocolsResponse",inputVec,silent,useLog);
   if(rStatus.getStatusCode().toString().equals("SRM_SUCCESS")) {
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
   }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear();
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmGetProtocols",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("\nSRM-UTIL: Exception " + msg, logger,silent);
   util.printMessageHException("\nSRM-UTIL: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(pIntf != null) {
     pIntf.setCompleted(false);
   }
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
                " please check your proxy", logger,silent);
             inputVec.clear();
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmPing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmPing() throws Exception {
 inputVec.clear();
 try {
   util.printMessage("\nSRM-MISC  " + new Date() +
		" Calling srmPing request...", logger,silent);
   if(_debug) {
     util.printMessage("\nSRM-MISC: ... Input parameters ...", logger,silent);
     util.printMessage("SRM-MISC: AuthorizationID="+uid,logger,silent);
   }
   inputVec.addElement("AuthorizationID="+uid);
   SrmPingRequest req = new SrmPingRequest();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   } 

   SrmPingResponse response = null;

   response = (SrmPingResponse) callSoapThread(req,response,"srmping");

   if(_debug) {
   util.printMessage("\nSRM-MISC  #### Output from SRM ####",logger,silent);
   }

   inputVec.clear(); 
   if(response == null) {
     inputVec.addElement("Srm returned null response for SrmPing"); 
     util.printEventLog(_theLogger,"DoSrmPingResponse", inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null response for SrmPing", 
		logger,silent);
     return ;
   }

   String versionInfo =response.getVersionInfo(); 
   util.printMessage("\tVersionInfo="+versionInfo, logger,silent);
   inputVec.addElement("VersionInfo="+versionInfo);

   ArrayOfTExtraInfo aInfo = response.getOtherInfo();

   if(aInfo == null) {
     inputVec.addElement("Srm returned null otherInfo");
     util.printEventLog(_theLogger,"DoSrmPingResponse", inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null otherInfo ", logger,silent);
     return ;
   }

   TExtraInfo[] eInfos =  aInfo.getExtraInfoArray();

   if(eInfos == null) {
     inputVec.addElement("Srm returned null extrainfo");
     util.printEventLog(_theLogger,"DoSrmPingResponse", inputVec,silent,useLog);
     util.printMessage("\nSRM-MISC: Srm returned null extrainfo ", logger,silent);
     return ;
   }

   for(int i = 0; i < eInfos.length; i++) {
     TExtraInfo eInfo = eInfos[i];
     inputVec.addElement("Key="+eInfo.getKey());
     inputVec.addElement("Value="+eInfo.getValue());
     util.printMessage("\t"+eInfo.getKey()+"="+eInfo.getValue(),logger,silent);
   }
   util.printEventLog(_theLogger,"DoSrmPingResponse", inputVec,silent,useLog);
 } 
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"DoSrmPing", inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("\nSRM-MISC: Exception " + e.getMessage(), logger,silent);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
                " please check your proxy", logger,silent);	
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRequestSummary
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmRequestSummary(Vector fileInfo) 
	throws Exception {

  int size = fileInfo.size();

  inputVec.clear();
  inputVec.addElement("AuthId="+uid);
  util.printMessage("\nSRM-REQUEST: " + new Date() + 		
		" Calling SrmRequestSummary", logger,silent);
  util.printMessageHL("\nSRM-REQUEST: " + new Date() + 		
		" Calling SrmRequestSummary", pIntf);
  if(_debug) {
  util.printMessage("\nSRM-REQUEST: ....Input parameters....", logger, silent);
  util.printMessage("SRM-REQUEST: AuthId="+uid,logger,silent);
  util.printMessage("\nSRM-REQUEST: ....Input parameters....", pIntf);
  util.printMessage("SRM-REQUEST: AuthId="+uid,pIntf);
  }
  String[] tokens = new String[size];
  for(int i = 0; i < size; i++) {
    FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
    String rid = fIntf.getSURL();
    inputVec.addElement("Token("+i+")="+rid);
    if(_debug) {
    util.printMessage("SRM-REQUEST: Token("+i+")="+rid,logger,silent);
    util.printMessage("SRM-REQUEST: Token("+i+")="+rid,pIntf);
    }
    tokens[i] = rid;
  }
  ArrayOfString aString = new ArrayOfString();
  aString.setStringArray(tokens);

  return getRequestSummary(aString);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestSummary
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private TStatusCode getRequestSummary (ArrayOfString tokenArray) 
	throws Exception {

 TStatusCode sCode = null;
 try {

  SrmGetRequestSummaryRequest r = new SrmGetRequestSummaryRequest();
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  r.setArrayOfRequestTokens(tokenArray);

  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmGetRequestSummaryRequest",inputVec,silent,useLog);

  SrmGetRequestSummaryResponse result = null;

  result = (SrmGetRequestSummaryResponse) 
                callSoapThread(r,result,"srmgetrequestsummary");

  if(_debug) {
  util.printMessage("\nSRM-REQUEST:  #### Output from SRM ####",logger,silent);
  util.printMessage("\nSRM-REQUEST:  #### Output from SRM ####",pIntf);
  }
  inputVec.clear();

  if(result == null) {
    inputVec.addElement("Null result from srmGetRequestSummary");
    util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",
		inputVec,silent,useLog);
    util.printMessage("\nSRM-REQUEST: Null result from srmGetRequestSummary",
		logger,silent);
    util.printMessage("\nSRM-REQUEST: Null result from srmGetRequestSummary",
		pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }

  if(result.getReturnStatus() == null) {
    inputVec.addElement("Null ReturnStatus from srmGetRequestSummary");
    util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",
		inputVec,silent,useLog);
    util.printMessage(
	 "\nSRM-REQUEST: Null ReturnStatus from srmGetRequestSummary",
		logger,silent);
    util.printMessage(
	 "\nSRM-REQUEST: Null ReturnStatus from srmGetRequestSummary", pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }

  sCode = result.getReturnStatus().getStatusCode();
  inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
  inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
  util.printMessage("\tstatus="+
	result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\texplanation="+
	result.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\tstatus="+
	result.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation="+
	result.getReturnStatus().getExplanation(),pIntf);
  
  if(sCode == TStatusCode.SRM_NOT_SUPPORTED) { 
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
	return sCode;
  }

  ArrayOfTRequestSummary sumArray = result.getArrayOfRequestSummaries();

  if(sumArray == null) {
    //util.printMessage("\nSRM-REQUEST: Null sumArray from srmGetRequestSummary",
			//logger,silent);
    //util.printMessage("\nSRM-REQUEST: Null sumArray from srmGetRequestSummary",
			//pIntf);
    inputVec.addElement("Null sumArray from srmGetRequestSummary");
    util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",
		inputVec,silent,useLog);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    return sCode;
  }

  for(int i = 0; i < sumArray.getSummaryArray().length; i++) {
    TRequestSummary curr = (sumArray.getSummaryArray())[i];
    util.printMessage("\tRID="+ curr.getRequestToken(),logger,silent);
    util.printMessage("\tRID="+ curr.getRequestToken(),pIntf);
    inputVec.addElement("RID="+ curr.getRequestToken());
    TReturnStatus rStatus = curr.getStatus();
    if(rStatus == null) {
      //util.printMessage(
		//"\n\tSRM-REQUEST: Null return status for request summary", 
			//logger, silent);
      //util.printMessage(
		//"\n\tSRM-REQUEST: Null return status for request summary", pIntf);
      inputVec.addElement("Null return status for request summary");
    }
    else {
      inputVec.addElement("status="+rStatus.getStatusCode());
      inputVec.addElement("explanation="+rStatus.getExplanation());
      util.printMessage("\tstatus="+rStatus.getStatusCode(),logger,silent);
      util.printMessage("\texplanation="+rStatus.getExplanation(),
			logger,silent);
      util.printMessage("\tstatus="+rStatus.getStatusCode(),pIntf);
      util.printMessage("\texplanation="+rStatus.getExplanation(),pIntf);
    }
    if(curr.getRequestType() != null) {
      inputVec.addElement("Request type="+ curr.getRequestType().getValue());
      util.printMessage("\tRequest type="+ curr.getRequestType().getValue(),
			logger,silent);
      util.printMessage("\tRequest type="+ curr.getRequestType().getValue(),
			pIntf);
    }
    else {
      util.printMessage("\tRequest type=null",logger,silent);
      util.printMessage("\tRequest type=null",pIntf);
      inputVec.addElement("\tRequest type=null");
    }
    util.printMessage("\tTotal files in request="+
       curr.getTotalNumFilesInRequest(),logger,silent);
    util.printMessage("\tTotal files in request="+
       curr.getTotalNumFilesInRequest(),pIntf);
    inputVec.addElement("TotalFilesInRequest="+
			curr.getTotalNumFilesInRequest());
    util.printMessage("\tNum of waiting files="+
	   curr.getNumOfWaitingFiles(),logger,silent);
    util.printMessage("\tNum of waiting files="+
	   curr.getNumOfWaitingFiles(),pIntf);
    inputVec.addElement("NumOFWaitingFiles="+curr.getNumOfWaitingFiles());
    util.printMessage("\tNum of failed files="+
	   curr.getNumOfFailedFiles(),logger,silent);
    util.printMessage("\tNum of failed files="+
	   curr.getNumOfFailedFiles(),pIntf);
    inputVec.addElement("NumOFFailedFiles="+curr.getNumOfFailedFiles());
    inputVec.addElement("NumOFCompletedFiles="+curr.getNumOfCompletedFiles());
    util.printMessage("\tNum of completed files="+
	   curr.getNumOfCompletedFiles(),logger,silent);
    util.printMessage("\tNum of completed files="+
	   curr.getNumOfCompletedFiles(),pIntf);
  }
  util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
  if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS")) { 
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  else {
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear();
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmGetRequestSummary",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
   util.printMessageHException("SRM-REQUEST: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(pIntf != null) {
     pIntf.setCompleted(false);
   }
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy", logger,silent);
             inputVec.clear();
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
  }
  return sCode;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmGetRID
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmGetRID (String userDes) throws Exception {
    return getRID(userDes);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRID
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private TStatusCode getRID(String userDes) throws Exception {

 TStatusCode sCode = null;
 inputVec.clear();
 try {
   util.printMessage("\nSRM-REQUEST: " + new Date() +
		" Calling srmGetRequestTokens", logger,silent);
   util.printMessageHL("\nSRM-REQUEST: " + new Date() +
		" Calling srmGetRequestTokens", pIntf);
   if(_debug) {
   util.printMessage("SRM-REQUEST: ...InputParameters listings ...", 
			logger,silent);
   util.printMessage("SRM-REQUEST: ...InputParameters listings ...", 
			pIntf);
   }
   inputVec.addElement("AuthorizationID="+uid);
   inputVec.addElement("UserDescription="+userDes);
   if(_debug) {
   util.printMessage("\tAuthorizationID="+uid,logger,silent);
   util.printMessage("\tUserDescription="+userDes,logger,silent);
   util.printMessage("\tAuthorizationID="+uid,pIntf);
   util.printMessage("\tUserDescription="+userDes,pIntf);
   }

   SrmGetRequestTokensRequest r = new SrmGetRequestTokensRequest ();
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   if(userDes == null || userDes.equals("")) {
     r.setUserRequestDescription(null);
   }
   else {
     r.setUserRequestDescription(userDes);
   }

   inputVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"SrmGetRequestTokensRequest",
                inputVec,silent,useLog);

   SrmGetRequestTokensResponse result = null;

   result = (SrmGetRequestTokensResponse) 
        callSoapThread(r,result,"srmgetrequesttokens");

   if(_debug) {
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####", logger,silent);
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####", pIntf);
   }

   inputVec.clear();
   if(result == null) {
     inputVec.addElement("SRM returned null result for SrmGetRequestTokens");
     util.printEventLog(_theLogger,
		"SrmGetRequestTokensResponse",inputVec,silent,useLog);
     util.printMessage(
		"\nSRM-REQUEST: SRM returned null result for SrmGetRequestTokens ", 
				logger,silent);
     util.printMessage(
		"\nSRM-REQUEST: SRM returned null result for SrmGetRequestTokens ", 
				pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   if(result.getReturnStatus() == null) {
     inputVec.addElement("SRM returned null ReturnStatus for SrmGetRequestTokens");
     util.printEventLog(_theLogger,"SrmGetRequestTokensResponse",
		inputVec,silent,useLog);
     util.printMessage(
	  "\nSRM-REQUEST: SRM returned null ReturnStatus for SrmGetRequestTokens", 
			logger,silent);
     util.printMessage(
	  "\nSRM-REQUEST: SRM returned null ReturnStatus for SrmGetRequestTokens", 
			pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   sCode = result.getReturnStatus().getStatusCode();
   inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
   inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
   util.printMessage("\tstatus="+ 
		result.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("\texplanation="+
	 result.getReturnStatus().getExplanation(),logger,silent);
   util.printMessage("\tstatus="+ 
		result.getReturnStatus().getStatusCode(),pIntf);
   util.printMessage("\texplanation="+
	 result.getReturnStatus().getExplanation(),pIntf);

   
   if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
      return sCode;
   }

   ArrayOfTRequestTokenReturn tokens = result.getArrayOfRequestTokens();

   if(tokens == null) {
     inputVec.addElement("SRM returned null Array of RequestTokens");
     //util.printMessage(
		//"\nSRM-REQUEST: SRM returned null Array of RequestTokens", logger,silent);
     //util.printMessage(
		//"\nSRM-REQUEST: SRM returned null Array of RequestTokens", pIntf);
     util.printEventLog(_theLogger,"SrmGetRequestTokensResponse",
			inputVec,silent,useLog);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TRequestTokenReturn[] tokenArray = tokens.getTokenArray(); 

   if(tokenArray == null) {
     //util.printMessage("\nSRM-REQUEST: SRM returned null tokenArray", 
			//logger,silent);
     //util.printMessage("\nSRM-REQUEST: SRM returned null tokenArray", pIntf);
     inputVec.addElement("SRM returned null tokenArray");
     util.printEventLog(_theLogger,"SrmGetRequestTokensResponse",
		inputVec,silent,useLog);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   for(int i = 0; i < tokenArray.length; i++) {
      TRequestTokenReturn curr = tokenArray[i];
      util.printMessage("\ttoken=" + curr.getRequestToken(),logger,silent);
      util.printMessage("\ttoken=" + curr.getRequestToken(),pIntf);
      inputVec.addElement("token=" + curr.getRequestToken());

      Calendar gcal = curr.getCreatedAtTime();
      Date dd = gcal.getTime();
      util.printMessage("\tCreatedAtTime="+dd.toString(),logger,silent);
      util.printMessage("\tCreatedAtTime="+dd.toString(),pIntf);
      inputVec.addElement("CreatedAtTime="+dd.toString());
      /*
      int year = dd.getYear()+1900; 
      int month = dd.getMonth(); 
      int day = dd.getDate(); 
      int hour = dd.getHours(); 
      int minute = dd.getMinutes(); 
      int second = dd.getSeconds(); 
      int year = gcal.get(Calendar.YEAR);
      int month = gcal.get(Calendar.MONTH);
      int day = gcal.get(Calendar.DAY_OF_MONTH);
      int hour = gcal.get(Calendar.HOUR_OF_DAY);
      int minute = gcal.get(Calendar.MINUTE);
      int second = gcal.get(Calendar.SECOND);
      */
     /*
      util.printMessage("\tYear=" + year, logger,silent);
      util.printMessage("\tMonth=" + (month+1), logger,silent);
      util.printMessage("\tDay="+ day, logger,silent);
      util.printMessage("\tHour=" + hour, logger,silent);
      util.printMessage("\tMinute=" + minute, logger,silent);
      util.printMessage("\tSecond=" + second, logger,silent);
      util.printMessage("\tYear=" + year, pIntf);
      util.printMessage("\tMonth=" + (month+1), pIntf);
      util.printMessage("\tDay="+ day, pIntf);
      util.printMessage("\tHour=" + hour, pIntf);
      util.printMessage("\tMinute=" + minute, pIntf);
      util.printMessage("\tSecond=" + second, pIntf);
      inputVec.addElement("CreatedAtTime=Year:"+year+":Month:"+
		(month+1)+":Day:"+ day+":Hour:"+hour+":Minute:"+
		minute+":Second:"+second);
      */
      util.printEventLog(_theLogger,"SrmGetRequestTokenResponse",
			inputVec,silent,useLog);
   }
   if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS")) {
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
   }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmGetRequestToken",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
    util.printMessageHException("SRM-REQUEST: Exception " + msg, pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1){
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmExtFileLT
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmExtFileLT(Vector fileInfo, 
	String token, int lifeTime, int pinLifeTime) throws Exception {

  int size = fileInfo.size();

  util.printMessage("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmExtFileLT", logger,silent);
  util.printMessageHL("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmExtFileLT", pIntf);
  if(_debug) {
  util.printMessage(
		"\nSRM-REQUEST: :::::::::: Input parameters for SrmExtFileLT ::::: ", 
				logger,silent);
  util.printMessage(
		"\nSRM-REQUEST: :::::::::: Input parameters for SrmExtFileLT ::::: ", 
				pIntf);
  }

  inputVec.clear(); 

  inputVec.addElement("AuthorizationID="+uid);
  inputVec.addElement("RequestToken="+token);
  if(_debug) {
  util.printMessage("SRM-REQUEST: AuthorizationID="+uid,logger,silent);
  if(token != null) {
    util.printMessage("SRM-REQUEST: RequestToken="+token,logger,silent);
  }
  util.printMessage("SRM-REQUEST: AuthorizationID="+uid,pIntf);
  util.printMessage("SRM-REQUEST: RequestToken="+token,pIntf);
  }
  if(lifeTime != 0) { 
    if(_debug) {
    util.printMessage("SRM-REQUEST: NewFileLifeTime="+lifeTime,logger,silent);
    util.printMessage("SRM-REQUEST: NewFileLifeTime="+lifeTime,pIntf);
    }
    inputVec.addElement("NewFileLifeTime="+lifeTime);
  }
  if(pinLifeTime != 0) {
    if(_debug) {
    util.printMessage("SRM-REQUEST: NewPinLifeTime="+pinLifeTime,logger,silent);
    util.printMessage("SRM-REQUEST: NewPinLifeTime="+pinLifeTime,pIntf);
    }
    inputVec.addElement("\tNewPinLifeTime="+pinLifeTime);
  }
  URI[] uris = new URI[size];
  for(int i = 0; i < size; i++) {
    FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
    String surl = fIntf.getOrigSURL();
    if(_debug) {
    util.printMessage("SRM-REQUEST: SURL="+surl,logger,silent);
    util.printMessage("SRM-REQUEST: SURL="+surl,pIntf);
    }
    inputVec.addElement("SURL=("+i+")"+surl);
    URI uri = new URI(surl);
    uris[i] = uri;
  }
  ArrayOfAnyURI arrayURI = new ArrayOfAnyURI();
  arrayURI.setUrlArray(uris);
  return extFileLT(token, arrayURI, lifeTime,pinLifeTime); 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// extFileLT
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private TStatusCode extFileLT(String token, ArrayOfAnyURI aURI,
   int lifeTime, int pinLifeTime) throws Exception {

 TStatusCode sCode = null;
 try {
  SrmExtendFileLifeTimeRequest r = new SrmExtendFileLifeTimeRequest();
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  if(token != null) {
    r.setRequestToken(token);
  }
  r.setArrayOfSURLs(aURI);
  if(lifeTime != 0) {
    r.setNewFileLifeTime(new Integer(lifeTime));
  }
  if(pinLifeTime != 0) {
    r.setNewPinLifeTime(new Integer(pinLifeTime));
  }

  SrmExtendFileLifeTimeResponse response = null;

  response = (SrmExtendFileLifeTimeResponse)
        callSoapThread(r,response,"srmextendfilelifetime");

  if(_debug) {
  util.printMessage("\nSRM-REQUEST: #### Output from SRM ####", logger,silent);
  util.printMessage("\nSRM-REQUEST: #### Output from SRM ####", pIntf);
  }

  if(response == null) {
    util.printMessage(
	 "\nSRM-REQUEST: Null response from srmExtendFileLifeTime", logger,silent);
    util.printMessage(
	 "\nSRM-REQUEST: Null response from srmExtendFileLifeTime", pIntf);
    inputVec.addElement("Null response from srmExtendFileLifeTime");
    util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }

  if(response.getReturnStatus() == null) {
    util.printMessage("\nSRM-REQ-EXTENDFIELIFETIME: Null returnStatus from srmExtendFileLifeTime", logger,silent);
    util.printMessage("\nSRM-REQ-EXTENDFIELIFETIME: Null returnStatus from srmExtendFileLifeTime", pIntf);
    inputVec.addElement("Null returnStatus from srmExtendFileLifeTime");
    util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }

  sCode = response.getReturnStatus().getStatusCode();
  util.printMessage("\tstatus="+
      response.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\texplanation="+
	  response.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\tstatus="+
      response.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation="+
	  response.getReturnStatus().getExplanation(),pIntf);
  inputVec.addElement("Status="+response.getReturnStatus().getStatusCode());
  inputVec.addElement("Explanation="+response.getReturnStatus().getExplanation());

  if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
      return sCode;
  }

  ArrayOfTSURLLifetimeReturnStatus fileStatuses = 
		response.getArrayOfFileStatuses();
  if(fileStatuses == null) {
    inputVec.addElement("Null FileStatuses from SrmExtendFileLifeTimeRequest");
    util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
    //util.printMessage(
	 //"\nSRM-REQUEST: Null FileStatuses from SrmExtendFileLifeTimeRequest", 
         //logger,silent);
    //util.printMessage(
	 //"\nSRM-REQUEST: Null FileStatuses from SrmExtendFileLifeTimeRequest", pIntf);
	if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }
  TSURLLifetimeReturnStatus[] lReturnStatuses = fileStatuses.getStatusArray(); 
  if(lReturnStatuses == null) {
    //util.printMessage
 	 //("\nSRM-REQUEST: Null fileStatuses.getStatusArray from SrmExtendFileLifeTimeRequest", logger, silent);
    //util.printMessage
 	 //("\nSRM-REQUEST: Null fileStatuses.getStatusArray from SrmExtendFileLifeTimeRequest", pIntf);
 	inputVec.addElement("Null fileStatuses.getStatusArray " +
		"from SrmExtendFileLifeTimeRequest");
    util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
	if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode;
  }
  util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
  for(int i = 0; i < lReturnStatuses.length; i++) {
    inputVec.clear(); 
    TSURLLifetimeReturnStatus lReturnStatus = lReturnStatuses[i];
    URI surl = lReturnStatus.getSurl();
    util.printMessage("\tsurl="+surl,logger,silent);
    util.printMessage("\tsurl="+surl,pIntf);
    inputVec.addElement("SURL="+surl);
    TReturnStatus status = lReturnStatus.getStatus();  
    if(status == null) {
      util.printMessage("\n\tSRM-REQUEST: Null return status from LifeTimeReturnStatus", logger, silent);
      util.printMessage("\n\tSRM-REQUEST: Null return status from LifeTimeReturnStatus", pIntf);
      inputVec.addElement("\nNull return status from LifeTimeReturnStatus");
    }
    else {
      inputVec.addElement("status="+status.getStatusCode());
      inputVec.addElement("explanation="+status.getExplanation());
      util.printMessage("\tstatus="+status.getStatusCode(), logger, silent);
      util.printMessage("\texplanation="+status.getExplanation(), logger, 
			silent);
      util.printMessage("\tstatus="+status.getStatusCode(),pIntf);
      util.printMessage("\texplanation="+status.getExplanation(), pIntf);
    }
    util.printMessage("\tFileLifeTime="+lReturnStatus.getFileLifetime(), 
			logger, silent);
    util.printMessage("\tFileLifeTime="+lReturnStatus.getFileLifetime(), 
			pIntf);
    inputVec.addElement("FileLifeTime="+lReturnStatus.getFileLifetime());
    util.printMessage("\tPinLifeTime="+lReturnStatus.getPinLifetime(), 
			logger, silent);
    util.printMessage("\tPinLifeTime="+lReturnStatus.getPinLifetime(), pIntf);
    inputVec.addElement("PinLifeTime="+lReturnStatus.getPinLifetime());
    util.printEventLog(_theLogger,"SrmExtendLifeTimeResponse",inputVec,silent,useLog);
  }
  if(response.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS")) {
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  else {
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmExtendLifeTime",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
      else {
        pIntf.setCompleted(false);
      }
   }
   else {
     throw e;
   }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmAbortRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmAbortRequest(String token) throws Exception  {

 TStatusCode sCode = null;
 try {
   util.printMessage("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmAbortRequest", logger,silent);
   util.printMessageHL("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmAbortRequest", pIntf);
   if(_debug) {
   util.printMessage(
	"\nSRM-REQUEST: ...Input parameters for SrmAbortRequest ....", 
			logger,silent);
   util.printMessage(
	"\nSRM-REQUEST: ...Input parameters for SrmAbortRequest ....", pIntf);
   util.printMessage( "\nSRM-REQUEST: uid="+uid,logger,silent);
   }
   SrmAbortRequestRequest r = new SrmAbortRequestRequest();
   r.setRequestToken(token);
   inputVec.clear(); 
   inputVec.addElement("Token="+token);
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
     inputVec.addElement("AuthorizationID="+uid);
   }

   SrmAbortRequestResponse result = null;

   result = (SrmAbortRequestResponse) callSoapThread(r,result,"srmabortrequest");

   if(_debug) {
   util.printMessage("SRM-REQUEST: #### Output from SRM ####",logger,silent);
   util.printMessage("SRM-REQUEST: #### Output from SRM ####",pIntf);
   }

   inputVec.clear(); 

   if(result == null) {
     inputVec.addElement("Null response from SrmAbortRequest");
     util.printEventLog(_theLogger,"SrmAbortResponse", inputVec,silent,useLog);
     util.printMessage("SRM-REQUEST: Null response from SrmAbortRequest", 
		logger, silent);
     util.printMessage("SRM-REQUEST: Null response from SrmAbortRequest", 
		pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   if(result.getReturnStatus() == null) {
     inputVec.addElement("Null return status from SrmAbortRequest");
     util.printEventLog(_theLogger,"SrmAbortResponse", inputVec,silent,useLog);
     util.printMessage("SRM-REQUEST: Null return status from SrmAbortRequest", 
		logger, silent);
     util.printMessage("SRM-REQUEST: Null return status from SrmAbortRequest", 
		pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   sCode = result.getReturnStatus().getStatusCode();
   inputVec.addElement("status="+ result.getReturnStatus().getStatusCode());
   inputVec.addElement("explanation="+ result.getReturnStatus().getExplanation());
   util.printMessage("\tstatus="+
 	 result.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("\texplanation="+
	 result.getReturnStatus().getExplanation(),logger,silent);
   util.printEventLog(_theLogger,"SrmAbortResponse", inputVec,silent,useLog);
   util.printMessage("\tstatus="+
 	 result.getReturnStatus().getStatusCode(),pIntf);
   util.printMessage("\texplanation="+
	 result.getReturnStatus().getExplanation(),pIntf);
    if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS")) { 
      if(pIntf != null) {
         pIntf.setCompleted(true);
      }
    }
    else {
      if(pIntf != null) {
         pIntf.setCompleted(false);
      }
    }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmAbort", inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
    util.printMessageHException("SRM-REQUEST: Exception " + msg, pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy " , logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doSrmAbortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmAbortFiles(Vector fileInfo, String token) 
	throws Exception {

  inputVec.clear(); 
  inputVec.addElement("AuthorizationID="+uid);
  inputVec.addElement("RID="+token);

  util.printMessage("\nSRM-COPY: " + new Date() + 
		" Calling SrmAbortFiles", logger,silent);
  if(_debug) {
  util.printMessage("\nSRM-COPY: .... Input parameters for AbortFiles ...", 
		logger,silent);
  util.printMessage("SRM-COPY: AuthorizationID="+uid, logger,silent);
  util.printMessage("SRM-COPY: RID="+token, logger,silent);
  }

  URI[] pathArray = new URI[fileInfo.size()];

  int size = fileInfo.size();
  for(int i = 0; i < size; i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String surl =  fIntf.getSURL();
     if(_debug) {
     util.printMessage("SRM-COPY: SURL=" + surl, logger,silent);
     util.printMessage("SRM-COPY: TURL=" + fIntf.getTURL(), logger,silent);
     }
     inputVec.addElement("SURL("+i+")="+token);
     pathArray[i] = new URI(surl);  
  }
  return abortFileArray(token, SRMUtilClient.convertToArrayOfAnyURI(pathArray));
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFileArray 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private TStatusCode abortFileArray(String token, 
	ArrayOfAnyURI pathArray) throws Exception {

 TStatusCode sCode = null;
 try {
   if((pathArray == null) || (pathArray.getUrlArray() == null)) {
     inputVec.addElement("No input files to perform abort file");
     util.printMessage("\nSRM-COPY: No input files to perform abort file",
			logger,silent);
     util.printEventLog(_theLogger,"SrmAbortFileArray",inputVec,silent,useLog);
     return sCode;
   }

   util.printMessage("SRM-COPY: Aborting files. Total #=" + 
		pathArray.getUrlArray().length,logger,silent);


   SrmAbortFilesRequest r = new SrmAbortFilesRequest();
   r.setArrayOfSURLs(pathArray);
   r.setRequestToken(token);
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   SrmAbortFilesResponse result = null;

   result = (SrmAbortFilesResponse) callSoapThread(r,result,"srmabortfiles");

   if(_debug) {
   util.printMessage("\nSRM-COPY: #### Output from SRM ####",logger,silent);
   }

   inputVec.clear(); 

   if(result == null) {
      inputVec.addElement("SRM returned null response for srmAbortFiles request");
      util.printMessage("\nSRM-COPY: SRM returned null response for srmAbortFiles request", logger,silent);
      util.printEventLog(_theLogger,"SrmAbortFilesResponse",inputVec,silent,useLog);
      return sCode;
   }

   if(result.getReturnStatus() == null) {
     util.printMessage("\nSRM-COPY: SRM returned null returnStatus for srmAbortFiles request", 
       logger,silent);
     inputVec.addElement("\nSRM returned null returnStatus for srmAbortFiles request");
     util.printEventLog(_theLogger,"SrmAbortFilesResponse",inputVec,silent,useLog);
     return sCode;
   } 

   sCode = result.getReturnStatus().getStatusCode();
   util.printMessage("\tstatus="+
      result.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("\texplanation="+
	  result.getReturnStatus().getExplanation(),logger,silent);
   inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
   inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());

   if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
     return sCode;
   }

   ArrayOfTSURLReturnStatus arrayFileStatusObj = 
	  result.getArrayOfFileStatuses();

   if(arrayFileStatusObj == null) {
     //util.printMessage
		//("\nSRM-COPY: SRM returned null arrayFileStatusObj " +
	      //"for srmAbortFiles request", logger,silent);
     inputVec.addElement
			("SRM returned null arrayFileStatusObj for srmAbortFiles request");
     util.printEventLog(_theLogger,"SrmAbortFilesResponse",inputVec,silent,useLog);
     return sCode; 
   }

   util.printEventLog(_theLogger,"SrmAbortFilesResponse",inputVec,silent,useLog);

   TSURLReturnStatus[] arrayFileStatus = 
	  arrayFileStatusObj.getStatusArray();

   for(int i=0; i < arrayFileStatus.length; i++) {
     inputVec.clear(); 
     inputVec.addElement("surl="+arrayFileStatus[i].getSurl().toString());
     //util.printMessage("\tSRM-COPY: " +
     //	" surl="+arrayFileStatus[i].getSurl().toString(), logger,silent);
     if(arrayFileStatus[i].getStatus() == null) {
        //util.printMessage("\nSRM-COPY: Null return status", logger,silent);
        inputVec.addElement("Null return status");
     }
     else {
       util.printMessage("\tstatus="+
	  	  arrayFileStatus[i].getStatus().getStatusCode(),logger,silent);
       util.printMessage("\texplanation="+
	   	  arrayFileStatus[i].getStatus().getExplanation(),logger,silent);
	   inputVec.addElement("status="+
			arrayFileStatus[i].getStatus().getStatusCode());
	   inputVec.addElement("explanation="+
	   	  arrayFileStatus[i].getStatus().getExplanation());
     }
     util.printEventLog(_theLogger,"SrmAbortFilesResponse",inputVec,silent,useLog);
   }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmAbortFiles",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-COPY:  Exception " + msg, logger,silent);
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				 " please check your proxy " , logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doSrmMv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doSrmMv(Vector fileInfo, boolean doLocalLsList, boolean doGridFTPList) 
	throws Exception {

  if(doLocalLsList) {
    util.printMessage("\nSRM-DIR: Total files to mv " + fileInfo.size(), logger,silent);
    util.printMessage("\nSRM-DIR: Total files to mv " + fileInfo.size(), pIntf);

    int count = 0;
    for(int i = 0; i < fileInfo.size(); i++) {
     try {
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
      String surl =  fIntf.getSURL();
      String turl =  fIntf.getTURL();
      String tsurl = SRMUtilClient.parseLocalSourceFileForPath (surl);
      File f = new File(tsurl);
      if(f.exists()) {
        if(!f.canRead()) {
          util.printMessage("\nSRM-DIR: No permission to read the file " + surl, logger,silent);
          util.printMessage("\nSRM-DIR: No permission to read the file " + surl, pIntf);
        }
        else {
          String tturl = SRMUtilClient.parseLocalSourceFileForPath (turl);
          //System.out.println(">>surl="+surl);
          //System.out.println(">>turl="+turl);
          //System.out.println(">>TSURL="+tsurl);
          //System.out.println(">>TTURL="+tturl);
          File ttf = new File(tturl);
          boolean b = f.renameTo(ttf);
          if(b) count ++;
        }
      }
      else {
        util.printMessage("\nSRM-DIR: File does not exists " + surl, logger,silent);
        util.printMessage("\nSRM-DIR: File does not exists " + surl, pIntf);
      }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
        util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),logger,silent);
        util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),pIntf);
     }
    }
    if (count == (fileInfo.size())) {
       util.printMessage("\nSRM-DIR: Mv is successful", logger, silent);
       util.printMessage("\nSRM-DIR: Mv is successful", pIntf);
       return "SRM_SUCCESS";
    }
    else {
       util.printMessage("\nSRM-DIR: Mv is not successful", logger, silent);
       util.printMessage("\nSRM-DIR: Mv is not successful", pIntf);
       return "SRM_FAILURE";
    }
  }
  else if(doGridFTPList) {
    util.printMessage("\nSRM-DIR: Total files to mv " + fileInfo.size(), logger,silent);
    util.printMessage("\nSRM-DIR: Total files to mv " + fileInfo.size(), pIntf);

    int count = 0;
    for(int i = 0; i < fileInfo.size(); i++) {
     try {
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
      String surl =  fIntf.getSURL();
      String turl =  fIntf.getTURL();
      String spath = surl;
      String tpath = turl;
      String ss = "";
      int idx = surl.lastIndexOf("/");
      if(idx != -1) {
         MyGlobusURL gurl = new MyGlobusURL(surl,0);
         spath = surl.substring(idx+1);
         surl = surl.substring(0,idx);
         ss = gurl.getPath();
         if(ss.startsWith("//")) {
             ss = ss.substring(1);
         }
      }
      idx = turl.lastIndexOf("/");
      if(idx != -1) {
         tpath = turl.substring(idx+1);
         MyGlobusURL gurl = new MyGlobusURL(turl,0);
         turl = gurl.getPath();
         if(turl.startsWith("//")) {
             turl = turl.substring(1);
         }
      }
      MyGridFTPClient gc = new MyGridFTPClient(surl,_credential,
		   logger,_theLogger,false);
      //boolean b = gc.doMv(spath,tpath);
      boolean b = gc.doMv(ss,turl);
      if(b) {
        count++;
        util.printMessage("\nSRM-DIR: Mv is successful ", logger, silent);
        util.printMessage("\nSRM-DIR: Mv is successful", pIntf);
      }
      else {
       util.printMessage("\nSRM-DIR: Mv is not successful " +surl+"/"+spath, logger, silent);
       util.printMessage("\nSRM-DIR: Mv is not successful " +surl+"/"+spath, pIntf);
      }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
        util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),logger,silent);
        util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),pIntf);
     }
     if (count == (fileInfo.size())) {
      return "SRM_SUCCESS";
     }
     else {
       return "SRM_FAILURE";
     }
    }
  }

  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);
 
  int count = 0;
  TStatusCode sCode = null;
  Vector codeVec = new Vector ();
  for(int i = 0; i < fileInfo.size(); i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String surl =  fIntf.getSURL();
     String turl =  fIntf.getTURL();
     URI fromPath = new URI(surl);
     URI toPath   = new URI(turl);

     String str = mv (fromPath,toPath,proxyString,storageInfo);
     codeVec.addElement(str);
  }

  String temp = getStatusCodeValue(codeVec);
  return temp;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getStatusCodeValue
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String getStatusCodeValue(Vector vec) {
 
  int size = vec.size();
  int successCount = 0;
  int errorCount = 0;
  int authnCount = 0;
  int authzCount = 0;
  int invalidPathCount = 0;
  int internalErrorCount=0;
  int notSupportedCount=0;
  int noStatusCount=0;

  for(int i = 0; i < size; i++) {
    String str = (String) vec.elementAt(i);
 
    
    if(str.equals("SRM_SUCCESS") ||
       str.equals("SRM_DUPLICATION_ERROR")) {
         successCount++;
    }
    else if(str.equals("SRM_FAILURE")) {
         errorCount++;
    }
    else if(str.equals("SRM_AUTHENTICATION_FAILURE")) {
         authnCount++;
    }
    else if(str.equals("SRM_AUTHORIZATION_FAILURE")) {
         authzCount++;
    }
    else if(str.equals("SRM_INVALID_PATH")) {
         invalidPathCount++;
    }
    else if(str.equals("SRM_INTERNAL_ERROR")) {
         internalErrorCount++;
    }
    else if(str.equals("SRM_NOT_SUPPORTED")) {
         notSupportedCount++;
    }
    else if(str.equals("SRM_RETURNED_NO_STATUS")) {
         noStatusCount++;
    }
  }

  String str="";
  int majority = 0;

  if(successCount == size) str= "SRM_SUCCESS"; //all success
  else if(errorCount == size) str= "SRM_FAILURE"; //all failure
  else if(authnCount == size) str= "SRM_AUTHENTICATION_FAILURE";
                //all authentication error
  else if(authzCount == size) str = "SRM_AUTHORIZATION_FAILURE";
                //all authorization error
  else if(invalidPathCount == size) str = "SRM_INVALID_PATH";
        //all invalidpath error
  else if(internalErrorCount == size) str= "SRM_INTERNAL_ERROR";
        //all internalerror
  else if(notSupportedCount == size) str= "SRM_NOT_SUPPORTED";
        //all notsupportederror
  else if(noStatusCount == size) str= "SRM_RETURNED_NO_STATUS";
  else {
    str="SRM_PARTIAL_SUCCESS";
  } 
    //fine tune what error is more
    if(errorCount == size) {
    //System.out.println("majority(1)="+majority);
    if(majority < authnCount) {
       majority = authnCount;
       str="SRM_AUTHENTICATION_FAILURE";
    }
    //System.out.println("majority(2)="+majority);
    if(majority < authzCount) {
       majority = authzCount;
       str="SRM_AUTHORIZATION_FAILURE";
    }
    //System.out.println("majority(3)="+majority);
    if(majority < invalidPathCount) {
       majority = invalidPathCount;
       str="SRM_INVALID_PATH";
    }
    //System.out.println("majority(4)="+majority);
    if(majority < internalErrorCount) {
       majority = internalErrorCount;
       str="SRM_INTERNAL_ERROR";
    }
    //System.out.println("majority(5)="+majority);
    if(majority < notSupportedCount) {
       majority = notSupportedCount;
       str="SRM_NOT_SUPPORTED";
    }
    if(majority < noStatusCount) {
       majority = noStatusCount;
       str="SRM_RETURNED_NO_STATUS";
    }
    }//end if
   //System.out.println("majority(6)="+majority+ " " + str);
   inputVec.clear();
   inputVec.addElement("SuccessCount="+successCount);
   inputVec.addElement(" ErrorCount="+errorCount);
   inputVec.addElement(" AuthNCount="+authnCount);
   inputVec.addElement(" AuthZCount="+authzCount);
   inputVec.addElement(" InvalidPathCount="+invalidPathCount);
   inputVec.addElement(" InternalErrorCount="+internalErrorCount);
   inputVec.addElement(" NotSupportedCount="+notSupportedCount);
   inputVec.addElement(" NoStatusCount="+noStatusCount);
   inputVec.addElement(" str="+str);
   inputVec.addElement(" majority="+majority);
   util.printEventLog(_theLogger,"getStatusCodeValue",inputVec,silent,useLog);
   return str;
} 

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String mv(URI fromPath, URI toPath,String proxyString, 
		String storageInfo) throws Exception {

 TStatusCode sCode = null;
 inputVec.clear(); 
 try { 
  util.printMessage("SRM-DIR: " + new Date() +  
		" Calling SrmMV", logger,silent);
  util.printMessageHL("SRM-DIR: " + new Date() +  
		" Calling SrmMV", pIntf);
  if(_debug) {
  util.printMessage("SRM-DIR: .... Input parameters for SrmMv ....", 
		logger,silent);
  util.printMessage("SRM-DIR: .... Input parameters for SrmMv ....", 
		pIntf);
  util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
  util.printMessage("SRM-DIR: from="+ fromPath.toString(),logger,silent);
  util.printMessage("SRM-DIR: to="+ toPath.toString(),logger,silent);
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,logger,silent);
  util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
  util.printMessage("SRM-DIR: from="+ fromPath.toString(),pIntf);
  util.printMessage("SRM-DIR: to="+ toPath.toString(),pIntf);
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,pIntf);
  }
  inputVec.addElement("AuthorizationID="+uid);
  inputVec.addElement("from="+ fromPath.toString());
  inputVec.addElement("to="+ toPath.toString());
  inputVec.addElement("UseStorageSystemInfo="+storageInfo);

  SrmMvRequest r = new SrmMvRequest();
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  r.setFromSURL(fromPath);
  r.setToSURL(toPath);

  ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
  TExtraInfo tExtra = new TExtraInfo();
  Vector vec = new Vector ();
  if(storageInfo.equals("true")) {

    tExtra.setKey("uid");
    tExtra.setValue("mv");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
   }

   TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];

   for(int i = 0; i < vec.size(); i++) {
     a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
   }

  storageInfoArray.setExtraInfoArray(a_tExtra);
  r.setStorageSystemInfo(storageInfoArray);

  SrmMvResponse result = null;

  result = (SrmMvResponse) callSoapThread(r,result,"srmmv");

  util.printMessage("SRM-DIR: .....................................", 
		logger,silent);
  util.printMessage("SRM-DIR: .....................................", pIntf);
  if(_debug) {
  util.printMessage("SRM-DIR: #### Output from SRM ####", logger,silent);
  util.printMessage("SRM-DIR: #### Output from SRM ####", pIntf);
  }
  

  inputVec.clear(); 
  
  if(result == null) {
     inputVec.addElement("Null result from SrmMv");
     util.printMessage("SRM-DIR: Null result from SrmMv", logger,silent);
     util.printMessage("SRM-DIR: Null result from SrmMv", pIntf);
     util.printEventLog(_theLogger,"SrmMvResponse",inputVec,silent,useLog);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return "SRM_RETURNED_NO_STATUS";
  }

  if(result.getReturnStatus () == null) {
    inputVec.addElement("Null return status");
    util.printEventLog(_theLogger,"SrmMvResponse",inputVec,silent,useLog);
    util.printMessage("SRM-DIR: Null return status ", logger,silent);
    util.printMessage("SRM-DIR: Null return status ", pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    return "SRM_RETURNED_NO_STATUS";
  }

  sCode = result.getReturnStatus().getStatusCode();
  util.printMessage("\tstatus="+
   result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\tstatus="+
   result.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation="+
   result.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\texplanation="+
   result.getReturnStatus().getExplanation(),pIntf);
  inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
  inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
  util.printEventLog(_theLogger,"SrmMvResponse",inputVec,silent,useLog);
  if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS"))  {
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  else {
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmMv",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-DIR: Exception " + msg, logger,silent);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
     else {
       pIntf.setCompleted(false);
     }
   }
   else {
    throw e;
   }
  }
  return sCode.toString();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRmFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doSrmRmFile (Vector fileInfo, 
	boolean doLocalLsList,boolean doGridFTPList) throws Exception {

 if(doLocalLsList) {
   util.printMessage("\nSRM-DIR: Total files to remove " + fileInfo.size(), logger,silent);
   util.printMessage("\nSRM-DIR: Total files to remove " + fileInfo.size(), pIntf);

   int count = 0;
   for(int i = 0; i < fileInfo.size(); i++) {
    try {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String surl = fIntf.getSURL();
       String tsurl = SRMUtilClient.parseLocalSourceFileForPath (surl);
       
       File f = new File(tsurl);
       if(!f.exists()) {
         util.printMessage("SRM-DIR: File does not exists " + surl, logger,silent);
         util.printMessage("SRM-DIR: File does not exists " + surl, pIntf);
       }
       else {
         if(!f.canRead() || !f.canWrite()) {
           util.printMessage("SRM-DIR: No permission to delete " + surl, logger,silent);
           util.printMessage("SRM-DIR: No permission to delete " + surl, pIntf);
         }
         else {
            boolean b = f.delete();
            if(b) {
             util.printMessage("SRM-DIR: File deleted " + surl, logger,silent);
             util.printMessage("SRM-DIR: File deleted " + surl, pIntf);
             count++;
            }
            else {
             util.printMessage("SRM-DIR: File not deleted " + surl, logger,silent);
             util.printMessage("SRM-DIR: File not deleted " + surl, pIntf);
            }
         }
       }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
         System.out.println("SRM-DIR: Exception="+e.getMessage());
     }
   }
   if(count == fileInfo.size()) {
     util.printMessage("SRM-DIR: Delete file is successful", logger,silent);
     util.printMessage("SRM-DIR: Delete file is successful", pIntf);
     //return TStatusCode.SRM_SUCCESS;
     return "SRM_SUCCESS";
   }
   else {
     util.printMessage("SRM-DIR: Delete file is not successful", logger,silent);
     util.printMessage("SRM-DIR: Delete file is not successful", pIntf);
     //return TStatusCode.SRM_FAILURE;
     return "SRM_FAILURE";
   }
 }
 else if(doGridFTPList) {
   util.printMessage("\nSRM-DIR: Total files to remove " + fileInfo.size(), logger,silent);
   util.printMessage("\nSRM-DIR: Total files to remove " + fileInfo.size(), pIntf);

   int count = 0;
   for(int i = 0; i < fileInfo.size(); i++) {
    try {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String surl = fIntf.getSURL();
       int idx = surl.lastIndexOf("/");
       String path = surl;
       if(idx != -1) {
         path = surl.substring(idx+1);
         surl = surl.substring(0,idx);
       }
       MyGridFTPClient gc = new MyGridFTPClient(surl,_credential,
	     logger,_theLogger,false);
       boolean b = gc.doDeleteDir(path,false,false);
       if(b) {
         count++;
         util.printMessage("SRM-DIR: Delete file is successful ", logger,silent);
         util.printMessage("SRM-DIR: Delete file is successful ", pIntf);
       }
       else {
         util.printMessage("SRM-DIR: Delete file is not successful "+surl+"/"+path, 
			logger,silent);
         util.printMessage("SRM-DIR: Delete file is not successful "+surl+"/"+path, pIntf);
       }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
         util.printMessage("SRM-DIR: Exception="+e.getMessage(),logger,silent);
         util.printMessage("SRM-DIR: Exception="+e.getMessage(),pIntf);
     }
   }
   if(count == fileInfo.size()) {
     //return TStatusCode.SRM_SUCCESS;
     return "SRM_SUCCESS";
   }
   else {
     //return TStatusCode.SRM_FAILURE;
     return "SRM_FAILURE";
   }
 }


  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  inputVec.clear(); 
  
  util.printMessage("\nSRM-DIR: " + new Date() + 
		" Calling SrmRmFile" , logger,silent);
  util.printMessage("\nSRM-DIR: " + new Date()+" Calling SrmRmFile" , pIntf);
  if(_debug) {
  util.printMessage("SRM-DIR: .... Input Parameters for SrmRmRequest ....",
		logger,silent);
  util.printMessage("SRM-DIR: .... Input Parameters for SrmRmRequest ....",
		pIntf);
  }
  if(_debug) {
  util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
  util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
  }
  inputVec.addElement("AuthorizationID="+uid);

  int size = fileInfo.size();
  URI[] uris = new URI[size];
  
  for(int i = 0; i < size; i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String surl = fIntf.getSURL();
     if(_debug) {
     util.printMessage("SRM-DIR: SURL=" + surl, logger,silent);
     util.printMessage("SRM-DIR: SURL=" + surl, pIntf);
     }
     inputVec.addElement("SURL("+i+")=" + surl);
     URI uri = new URI(surl);
     uris[i] = uri;
  }

  if(_debug) {
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+
		storageInfo,logger,silent);
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+
		storageInfo,pIntf);
  }
  inputVec.addElement("UseStorageSystemInfo="+storageInfo);

  return rmFileArray(SRMUtilClient.convertToArrayOfAnyURI(uris), 
		proxyString, storageInfo);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// rmFileArray 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String rmFileArray(ArrayOfAnyURI uris, 
    String proxyString, String storageInfo) 
	throws Exception {

 TStatusCode sCode = null;
 try {
  if((uris.getUrlArray() == null) || (uris.getUrlArray().length == 0)) {
    util.printMessage("\nSRM-DIR: No files to remove",logger,silent);
    util.printMessage("\nSRM-DIR: No files to remove",pIntf);
    inputVec.addElement("No files to remove");
    util.printEventLog(_theLogger,"SrmRmFile",inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return null;
  }

  util.printMessage("SRM-DIR: Total files to remove: "+
		uris.getUrlArray().length,logger,silent);
  util.printMessage("SRM-DIR: Total files to remove: "+
		uris.getUrlArray().length,pIntf);
  inputVec.addElement("Total files to remove="+ uris.getUrlArray().length);


  SrmRmRequest r = new SrmRmRequest();
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  r.setArrayOfSURLs(uris);


  ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
  TExtraInfo tExtra = new TExtraInfo();
  Vector vec = new Vector ();
  if(storageInfo.equals("true")) {
       tExtra.setKey("uid");
       tExtra.setValue("rm");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
  }
 

  TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];

  for(int i = 0; i < vec.size(); i++) {
    a_tExtra[i] = (TExtraInfo) vec.elementAt(i);
  }

  storageInfoArray.setExtraInfoArray(a_tExtra);
  r.setStorageSystemInfo(storageInfoArray);

  SrmRmResponse result = null;

  result = (SrmRmResponse) callSoapThread(r,result,"srmrm");

  if(_debug) {
  util.printMessage("\nSRM-DIR: #### Output from SRM ####",logger,silent);
  util.printMessage("\nSRM-DIR: #### Output from SRM ####",pIntf);
  }
 
  
  inputVec.clear(); 

  if(result == null) {
    inputVec.addElement("Null result from SrmRm");
    util.printEventLog(_theLogger,"SrmRmFileResponse",inputVec,silent,useLog);
    util.printMessage("SRM-DIR: Null result from SrmRm", logger,silent);
    util.printMessage("SRM-DIR: Null result from SrmRm", pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return "SRM_RETURNED_NO_STATUS";
  }

  if(result.getReturnStatus() == null) {
    util.printMessage("\nSRM-DIR: Null return status form SrmRm", 
		logger,silent);
    util.printMessage("\nSRM-DIR: Null return status form SrmRm", pIntf);
    inputVec.addElement("Null return status form SrmRm");
    util.printEventLog(_theLogger,"SrmRmFileResponse",inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return "SRM_RETURNED_NO_STATUS";
  }

  sCode = result.getReturnStatus().getStatusCode();

  util.printMessage("\tstatus=" + 
	 result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\tstatus=" + 
	 result.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation=" + 
	 result.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\texplanation=" + 
	 result.getReturnStatus().getExplanation(),pIntf);

  inputVec.addElement("status="+result.getReturnStatus().getStatusCode());
  inputVec.addElement("explanation="+result.getReturnStatus().getExplanation());

  if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
       return sCode.toString();
  }

  ArrayOfTSURLReturnStatus arrayFileStatuses = 
	result.getArrayOfFileStatuses();

  if(arrayFileStatuses == null) {
    //util.printMessage("SRM-DIR: Null file statuses from SrmRm", 
		//logger,silent);
    //util.printMessage("SRM-DIR: Null file statuses from SrmRm", pIntf);
    inputVec.addElement("Null file statuses from SrmRm");
    util.printEventLog(_theLogger,"SrmRmFileResponse",inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return sCode.toString();
  }

  util.printEventLog(_theLogger,"SrmRmFileResponse",inputVec,silent,useLog);

  TSURLReturnStatus[] arrayFileStatus = arrayFileStatuses.getStatusArray();

  for(int i = 0; i < arrayFileStatus.length; i++) {
     inputVec.clear(); 
     inputVec.addElement("surl="+
		arrayFileStatus[i].getSurl().toString());
     util.printMessage("\tsurl="+
		arrayFileStatus[i].getSurl().toString(), logger,silent);
     if(arrayFileStatus[i].getStatus() == null) {
        //util.printMessage("SRM-DIR: Null return file status", logger,silent);
        //util.printMessage("SRM-DIR: Null return file status", pIntf);
        inputVec.addElement("Null return file status");
     }
     else {
       inputVec.addElement("status="+
	  	  arrayFileStatus[i].getStatus().getStatusCode());
       inputVec.addElement("explanation="+
		  arrayFileStatus[i].getStatus().getExplanation());
       util.printMessage("\tstatus="+
	  	  arrayFileStatus[i].getStatus().getStatusCode(), logger,silent);
       util.printMessage("\tstatus="+
	  	  arrayFileStatus[i].getStatus().getStatusCode(), pIntf);
       util.printMessage("\texplanation="+
		  arrayFileStatus[i].getStatus().getExplanation(), logger,silent);
       util.printMessage("\texplanation="+
		  arrayFileStatus[i].getStatus().getExplanation(), pIntf);
     }
     util.printEventLog(_theLogger,"SrmRmFileResponse",inputVec,silent,useLog);
  }
  if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS")) {
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  else {
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmRmFile",inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-DIR: Exception " + msg, logger,silent);
   util.printMessageHException("SRM-DIR: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
     else {
      pIntf.setCompleted(false);
     }
   }
   else {
     throw e;
   }
 }
 return sCode.toString();
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRmdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doSrmRmdir(Vector fileInfo, boolean recursive, 
	boolean doLocalLsList, boolean doGridFTPList) throws Exception {

 if(doGridFTPList) {
   util.printMessage("\nSRM-DIR: Total directories to delete " + fileInfo.size(),logger,silent);
   util.printMessage("\nSRM-DIR: Total directories to delete " + fileInfo.size(), pIntf);

   int count = 0;
   for(int i = 0; i < fileInfo.size(); i++) {
    try {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String surl = fIntf.getSURL();

        /*
       int idx = surl.lastIndexOf("/");
       String path = surl;
       if(idx != -1) {
         path = surl.substring(idx+1);
         surl = surl.substring(0,idx);
       }
        */
       MyGridFTPClient gc = new MyGridFTPClient(surl,_credential,
	     logger,_theLogger,false);
       boolean b = gc.doDeleteDir(surl,true,recursive);
       if(b) {
          util.printMessage("\nSRM-DIR: Directory deleted " , logger,silent);
          util.printMessage("\nSRM-DIR: Directory deleted " , pIntf);
          count++;
       }
       else {
          util.printMessage("\nSRM-DIR: Directory not deleted "+surl , logger,silent);
          util.printMessage("\nSRM-DIR: Directory not deleted "+surl , pIntf);
       }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
         System.out.println("Exception="+e.getMessage());
     }
   }
   if(count == fileInfo.size()) {
     //return TStatusCode.SRM_SUCCESS;
     return "SRM_SUCCESS";
   }
   else {
     //return TStatusCode.SRM_FAILURE;
     return "SRM_FAILURE";
   }
 }
 else if(doLocalLsList) {
   util.printMessage("\nSRM-DIR: Total directories to delete " + fileInfo.size(),logger,silent);
   util.printMessage("\nSRM-DIR: Total directories to delete " + fileInfo.size(), pIntf);

   int count = 0;
   for(int i = 0; i < fileInfo.size(); i++) {
    try {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String surl = fIntf.getSURL();
       String tsurl = SRMUtilClient.parseLocalSourceFileForPath (surl);

       File f = new File(tsurl);
       if(!f.exists()) {
         util.printMessage("\nSRM-DIR: Directory does not exists " + surl, logger,silent);
         util.printMessage("\nSRM-DIR: Directory does not exists " + surl, pIntf);
       }
       else {
         if(!f.canRead() || !f.canWrite()) {
           util.printMessage("\nSRM-DIR: No permission to delete " + surl, logger,silent);
           util.printMessage("\nSRM-DIR: No permission to delete " + surl, pIntf);
         }
         else {
            if(recursive) {
             boolean b = rmdirs(f);
             if(b) count++ ;
            }
            else {
              boolean b = f.delete();
              if(b) {
               util.printMessage("\nSRM-DIR: Directory deleted " + surl, logger,silent);
               util.printMessage("\nSRM-DIR: Directory deleted " + surl, pIntf);
               count++;
              }
              else {
               util.printMessage("\nSRM-DIR: Directory not deleted, " +
					"directory may not be empty, use -recursive option" + surl, logger,silent);
               util.printMessage("\nSRM-DIR: Directory not deleted, " +
					"directory may not be empty, use -recursive option" + surl, pIntf);
              }
            }
         }
       }
     }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
         System.out.println("Exception="+e.getMessage());
     }
   }

   if(count == fileInfo.size()) {
     //return TStatusCode.SRM_SUCCESS;
     return "SRM_SUCCESS";
   }
   else {
     //return TStatusCode.SRM_FAILURE;
     return "SRM_FAILURE";
   }
 }


  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  inputVec.clear(); 
  util.printMessage("\nSRM-DIR: " + new Date() +
     " Calling SrmRmdir", logger,silent);		
  util.printMessageHL("\nSRM-DIR: " + new Date() +
     " Calling SrmRmdir", pIntf);		

  if(_debug) {
  util.printMessage("SRM-DIR: .... Input Parameters for SrmRmdir ....",
		logger,silent);
  util.printMessageHL("SRM-DIR: .... Input Parameters for SrmRmdir ....",
		pIntf);
  util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
  //util.printMessage("SRM-DIR: Recursive="+recursive,logger,silent);
  util.printMessage("SRM-DIR: Recursive=false",logger,silent);
  util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
  util.printMessage("SRM-DIR: Recursive="+recursive,pIntf);
  }
  inputVec.addElement("AuthorizationID="+uid);
  //inputVec.addElement("Recursive="+recursive);
  inputVec.addElement("Recursive=false");

  TStatusCode sCode = null;
  int count = 0;
  Vector codeVec = new Vector ();
  for(int i = 0; i < fileInfo.size(); i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String surl =  fIntf.getSURL();
     if(_debug) {
     util.printMessage("SRM-DIR: DirectoryPath="+surl,logger,silent);
     util.printMessage("SRM-DIR: DirectoryPath="+surl,pIntf);
     }
     inputVec.addElement("DirectoryPath="+surl);
     Boolean beRecursive = fIntf.getIsRecursive();
     if(recursive) { 
        beRecursive = new Boolean (recursive);
     }
     URI path = new URI(surl);  

     /* 
     if(storageInfo.equals("")) {
       int idx = surl.indexOf("mss://");
       if(idx != -1) {
         storageInfo = ""+true;
       }
     }
     */

     String str =  rmdir (path,beRecursive,proxyString,storageInfo);
     codeVec.addElement(str);
  }

  //sCode = TStatusCode.fromValue(getStatusCodeValue(codeVec));
  //return sCode;
  return getStatusCodeValue(codeVec);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// rmdirs
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean rmdirs(File path) throws Exception {
    if( path.exists() ) {
      File[] files = path.listFiles();
      for(int i=0; i<files.length; i++) {
         if(files[i].isDirectory()) {
           rmdirs(files[i]);
         }
         else {
           files[i].delete();
         }
      }
    }
    boolean b = path.delete();
    if(b) {
      util.printMessage("SRM-DIR: Directory deleted " + path, logger,silent);
      util.printMessage("SRM-DIR: Directory deleted " + path, pIntf);
    }
    return b;
  }

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// rmdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String rmdir(URI path, Boolean beRecursive, 
	String proxyString, String storageInfo) 
		throws Exception {

 TStatusCode sCode = null;
 try {
  SrmRmdirRequest r = new SrmRmdirRequest(); 
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  r.setSURL(path);
  //setting this value on May 28, 10 
  //DPM did not support that before, but BestMan and dCache supports it
  r.setRecursive(beRecursive);

  if(_debug) {
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,
		logger,silent);
  util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,
		pIntf);
  } 
  inputVec.addElement("Recursive="+storageInfo);


  ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
  TExtraInfo tExtra = new TExtraInfo();
  Vector vec = new Vector ();
  if(storageInfo.equals("true")) {

    tExtra.setKey("uid");
    tExtra.setValue("rmdir");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
  }
 
  TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
  for(int i = 0; i < vec.size(); i++) {
    a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
  }
  storageInfoArray.setExtraInfoArray(a_tExtra);
  r.setStorageSystemInfo(storageInfoArray);


  inputVec.addElement("TimeStamp="+new Date());
  if(_debug) {
    util.printMessage("\nSending srmRmdir request ...", logger,silent);
    util.printMessage("\nSending srmRmdir request ...", pIntf);
  }
  util.printEventLog(_theLogger,"SrmRmdirRequest", inputVec,silent,useLog);

  SrmRmdirResponse result = null;

  result = (SrmRmdirResponse) callSoapThread(r,result,"srmrmdir");

  util.printMessage("\nSRM-DIR: ........................",logger,silent);
  util.printMessage("\nSRM-DIR: ........................",pIntf);

  inputVec.clear(); 

  if(_debug) {
  util.printMessage("SRM-DIR: #### Output from SRM ####", logger,silent);
  util.printMessage("SRM-DIR: #### Output from SRM ####", pIntf);
  }

  if(result == null) {
    inputVec.addElement("Null response from SrmRmdir");
    util.printEventLog(_theLogger,"SrmRmdirResponse", inputVec,silent,useLog);
    util.printMessage("\nSRM-DIR: Null response from SrmRmdir", 
		logger,silent);
    util.printMessage("\nSRM-DIR: Null response from SrmRmdir", pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return "SRM_RETURNED_NO_STATUS";
  }

  if(result.getReturnStatus() == null) {
    util.printMessage("\nSRM-DIR: Null return status from SrmRmdir", 
		logger,silent);
    util.printMessage("\nSRM-DIR: Null return status from SrmRmdir", 
		pIntf);
    inputVec.addElement("Null return status from SrmRmdir");
    util.printEventLog(_theLogger,"SrmRmdirResponse", inputVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    return "SRM_RETURNED_NO_STATUS";
  }

  sCode = result.getReturnStatus().getStatusCode();
  util.printMessage("\tstatus="+
		result.getReturnStatus().getStatusCode(),logger,silent);
  util.printMessage("\tstatus="+
		result.getReturnStatus().getStatusCode(),pIntf);
  util.printMessage("\texplanation="+
		result.getReturnStatus().getExplanation(),logger,silent);
  util.printMessage("\texplanation="+
		result.getReturnStatus().getExplanation(),pIntf);
  inputVec.addElement("Status="+ result.getReturnStatus().getStatusCode());
  inputVec.addElement("Explanation="+ result.getReturnStatus().getExplanation());
  util.printEventLog(_theLogger,"SrmRmdirResponse", inputVec,silent,useLog);
  if(result.getReturnStatus().getStatusCode().toString().
		equals("SRM_SUCCESS"))   {
    if(pIntf != null) {
        pIntf.setCompleted(true);
    }
  }
  else {
    if(pIntf != null) {
        pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmRmdirResponse", inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-DIR: Exception " + msg, logger,silent);
   util.printMessageHException("SRM-DIR: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
                " please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
      else {
        pIntf.setCompleted(false);
      }
   }
   else {
      throw e;
   }
 }
 return sCode.toString();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doSrmMkdir(Vector fileInfo,StringBuffer sCode, 
	boolean doLocalLsList, boolean doGsiFTPList,boolean mkdiroption,
	boolean skipTopDirectoryExistenceForRecursiveOption) 
		throws Exception {

  boolean ok=false;
  if(doLocalLsList) {
    util.printMessage("\nSRM-DIR: Calling mkdir " + new Date(), logger,silent);
    util.printMessage("\nSRM-DIR: Calling mkdir " + new Date(),pIntf); 

    util.printMessage("\nSRM-DIR: Total directories to create " + fileInfo.size(), 
			logger,silent);
    util.printMessage("\nSRM-DIR: Total directories to create " + fileInfo.size() ,pIntf); 
    
    int count = 0;
    for(int i = 0; i < fileInfo.size(); i++) {
       try {  
         FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
         String surl =  fIntf.getSURL();
         String temp = SRMUtilClient.parseLocalSourceFileForPath(surl);
         File f = new File(temp);
         if(f.exists()) {
          util.printMessage("\nSRM-DIR: file or directory already exists "+surl,logger,silent);
          util.printMessage("\nSRM-DIR: file or directory already exists "+surl,pIntf);
          count++;
          //sCode.append("SRM_FAILURE");  
         }
         else {
          boolean b = f.mkdirs();
          if(b) {
            util.printMessage("\nSRM-DIR: directory created "+surl, logger,silent);
            util.printMessage("\nSRM-DIR: directory created "+surl,pIntf);
            count ++;
            //sCode.append("SRM_SUCCESS");
          }
          else {
            util.printMessage("\nSRM-DIR: directory not created "+surl, logger,silent);
            util.printMessage("\nSRM-DIR: directory not created "+surl,pIntf);
            //sCode.append("SRM_FAILURE");
          }
         }
       } catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
          util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),logger,silent);
          util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),pIntf);
       }
    }
    if(count == fileInfo.size()) {
       sCode.append("SRM_SUCCESS");
    }
    else {
       sCode.append("SRM_FAILURE");
    }
    return (count == fileInfo.size());
  }
  else if(doGsiFTPList) {
    util.printMessage("\nSRM-DIR: Calling mkdir " + new Date(), logger,silent);
    util.printMessage("\nSRM-DIR: Calling mkdir " + new Date(),pIntf); 

    util.printMessage("\nSRM-DIR: Total directories to create " + fileInfo.size(), 
			logger,silent);
    util.printMessage("\nSRM-DIR: Total directories to create " + fileInfo.size() ,pIntf); 

    int count = 0;
    for(int i = 0; i < fileInfo.size(); i++) {
       try {  
         FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
         String surl =  fIntf.getSURL();
         int idx = surl.lastIndexOf("/");
         String path = surl;
         if(idx != -1) {
           path = surl.substring(idx+1);
           surl = surl.substring(0,idx);
         }
         MyGridFTPClient gc = new MyGridFTPClient(surl,_credential,
		   logger,_theLogger,false);
         boolean b = gc.doMkdir(path);
         if(b) {
           count++;
           util.printMessage("\nSRM-DIR: Directory created " + surl+"/"+path,
								logger,silent);
           util.printMessage("\nSRM-DIR: Directory created "+surl+"/"+path,
								pIntf);
         }
         else {
           if(skipTopDirectoryExistenceForRecursiveOption) {
             util.printMessage("\nSRM-DIR: Please ignore the warnings ", 
                     logger,silent);
             util.printMessage("\nSRM-DIR: Please ignore the warnings ", 
                     pIntf);
             count++;
           }
           else {
           util.printMessage("\nSRM-DIR: Directory not created "+
				surl+"/"+path,logger,silent);
           util.printMessage("\nSRM-DIR: Directory not created "+
				surl+"/"+path,pIntf);
           }
         }
       } catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
           util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),logger,silent);
           util.printMessage("\nSRM-DIR: Exception="+e.getMessage(),pIntf);
        }
    }
    if(count == fileInfo.size()) {
       sCode.append("SRM_SUCCESS");
    }
    else {
       sCode.append("SRM_FAILURE");
    }
    return (count == fileInfo.size());
  }
  else if(mkdiroption) {
    ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
    byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
    String proxyString = new String(bb);

    util.printMessage("\nSRM-DIR: " + new Date() + 
		" Calling SrmMkdir", logger,silent);
    util.printMessageHL("\nSRM-DIR: " + new Date() + 
		" Calling SrmMkdir", pIntf);

    if(_debug) {
      util.printMessage("SRM-DIR: ... Input Parameters for SrmMkdir ...",
		logger,silent);
      util.printMessage("SRM-DIR: ... Input Parameters for SrmMkdir ...",
		pIntf);
      util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
      util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
    }

    inputVec.clear(); 
    inputVec.addElement("AuthorizationID="+uid);

    int count = 0;
    Vector codeVec = new Vector ();
    for(int i = 0; i < fileInfo.size(); i++) {
     inputVec.clear();
     sCode = sCode.replace(0,sCode.length(),"");
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String turl =  fIntf.getTURL();
     /*
     int idx = turl.lastIndexOf("/");
     if(idx != -1) {
       turl = turl.substring(0,idx);
     }
     */
     URI path = new URI(turl);
     //if(_debug) {
     util.printMessage("SRM-DIR: DirectoryPath("+i+")="+turl,logger,silent);
     util.printMessage("SRM-DIR: DirectoryPath("+i+")="+turl,pIntf);
     //}
     inputVec.addElement("DirectoryPath("+i+")="+turl);

     /*
     if(storageInfo.equals("")) {
       int idx = surl.indexOf("mss://");
       if(idx != -1) {
         storageInfo = ""+true;
       }
     }
     */
     ok = mkdir (path, proxyString,storageInfo,sCode);
     codeVec.addElement(sCode.toString());
    } 
    String str = getStatusCodeValue(codeVec);
    if(str.equals("SRM_SUCCESS")) {
      return true;
    }
    return false;
  }
  else {  

  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  util.printMessage("\nSRM-DIR: " + new Date() + 
		" Calling SrmMkdir", logger,silent);
  util.printMessageHL("\nSRM-DIR: " + new Date() + " Calling SrmMkdir", pIntf);

  if(_debug) {
    util.printMessage("SRM-DIR: ... Input Parameters for SrmMkdir ...",
		logger,silent);
    util.printMessage("SRM-DIR: ... Input Parameters for SrmMkdir ...",
		pIntf);
    util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
    util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
  }

  inputVec.clear(); 
  inputVec.addElement("AuthorizationID="+uid);

  int count = 0;
  Vector codeVec = new Vector ();

  for(int i = 0; i < fileInfo.size(); i++) {
     inputVec.clear();
     sCode = sCode.replace(0,sCode.length(),"");
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String surl =  fIntf.getSURL();
     URI path = new URI(surl);
     //if(_debug) {
     util.printMessage("SRM-DIR: DirectoryPath("+i+")="+surl,logger,silent);
     util.printMessage("SRM-DIR: DirectoryPath("+i+")="+surl,pIntf);
     //}
     inputVec.addElement("DirectoryPath("+i+")="+surl);

     /*
     if(storageInfo.equals("")) {
       int idx = surl.indexOf("mss://");
       if(idx != -1) {
         storageInfo = ""+true;
       }
     }
     */
     ok = mkdir (path, proxyString,storageInfo,sCode);
     codeVec.addElement(sCode.toString());
   }
   
   String str = getStatusCodeValue(codeVec);
   sCode = sCode.replace(0,sCode.length(),"");
   sCode.append(str);
   if(str.equals("SRM_SUCCESS")) {
      return true;
    }
   return false;
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean mkdir(URI path, String proxyString, 
	String storageInfo,StringBuffer sCode) throws Exception {

 if(_debug) {
 util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,
		logger,silent);
 util.printMessage("SRM-DIR: UseStorageSystemInfo="+storageInfo,
		pIntf);
 } 
 inputVec.addElement("StorageInfo="+storageInfo);


 try {
  SrmMkdirRequest r = new SrmMkdirRequest();
  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }
  r.setSURL(path);


  ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
  TExtraInfo tExtra = new TExtraInfo();
  Vector vec = new Vector ();
  if(storageInfo.equals("true")) {
    tExtra.setKey("uid");
    tExtra.setValue("mkdir");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
  }
 

  TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
  for(int i = 0; i < vec.size(); i++) {
    a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
  }
  storageInfoArray.setExtraInfoArray(a_tExtra);
  r.setStorageSystemInfo(storageInfoArray);

  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmMkdirRequest", inputVec,silent,useLog);

  SrmMkdirResponse result = null;

  result = (SrmMkdirResponse) callSoapThread(r,result,"srmmkdir");
  
  inputVec.clear(); 
  
  if(_debug) {
  util.printMessage("\nSRM-DIR: .....................................",
		logger,silent);
  util.printMessage("\nSRM-DIR: .....................................",
		pIntf);
  util.printMessage("SRM-DIR: #### Output from SRM ####", logger,silent);
  util.printMessage("SRM-DIR: #### Output from SRM ####", pIntf);
  }

  if(result == null) {
    inputVec.addElement("Null result from SrmMkdir");
    util.printEventLog(_theLogger,"SrmMkdirResponse", inputVec,silent,useLog);
    util.printMessage("\nSRM-DIR: Null result from SrmMkdir", 
			logger,silent);
    util.printMessage("\nSRM-DIR: Null result from SrmMkdir", pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    sCode.append("SRM_RETURNED_NO_STATUS");
    return false;
  }

  if(result.getReturnStatus() == null) {
    inputVec.addElement("Null return status for SrmMkdir");
    util.printEventLog(_theLogger,"SrmMkdirResponse", inputVec,silent,useLog);
    util.printMessage("\nSRM-DIR: Null return status for SrmMkdir", 
		logger, silent);
    util.printMessage("\nSRM-DIR: Null return status for SrmMkdir", 
		pIntf);
    if(pIntf != null) {
       pIntf.setCompleted(false);
    }
    sCode.append("SRM_RETURNED_NO_STATUS");
    return false;
  }

  inputVec.addElement("Status="+result.getReturnStatus().getStatusCode());
  inputVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
  util.printEventLog(_theLogger,"SrmMkdirResponse", inputVec,silent,useLog);
  
  if(result.getReturnStatus().getStatusCode() != null) {
    sCode.append(result.getReturnStatus().getStatusCode().toString());
  }
  else {
    sCode.append("SRM_RETURNED_NO_STATUS");
  }

  util.printMessage("\tstatus="+
	result.getReturnStatus().getStatusCode(), logger,silent);
  util.printMessage("\tstatus="+
	result.getReturnStatus().getStatusCode(), pIntf);
  util.printMessage("\texplanation="+
	result.getReturnStatus().getExplanation(), logger,silent);
  util.printMessage("\texplanation="+
	result.getReturnStatus().getExplanation(), pIntf);
  if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||  
		result.getReturnStatus().getStatusCode() == TStatusCode.SRM_DUPLICATION_ERROR) {
    if(pIntf != null) {
       pIntf.setCompleted(true);
    }
    return true;
  }
  else {
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmMkdir", inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("\nSRM-DIR: Exception " + e.getMessage(), logger,silent);
   util.printMessageHException("\nSRM-DIR: Exception " + e.getMessage(),pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy", logger,silent);   
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
     else {
       pIntf.setCompleted(false);
     }
   }
   else {
     throw e;
   }
 }
 return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLs
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmLs(Vector fileInfo,boolean doGsiFTPList, 
    boolean doLocalLsList,
	boolean recursive, boolean fullDetailedList, int numLevels,
    int count, int offset, TFileStorageType fileType, 
    String outputFile, boolean statusWaitTimeGiven,
	boolean getRecursive, boolean textReport,
	Vector resultVec, String surl, String turl, 
	boolean submitOnly, boolean esgReportType, boolean copyCase,
	String targetDir,String sourceUrl) throws Exception {


  TStatusCode sCode = null;
  try {

   if(!statusWaitTimeGiven) {
     if(fileInfo.size() > 20) {
        statusWaitTime=60*1000;
     }
   }

   if(doGsiFTPList) {
     Vector result = new Vector ();
     int size = fileInfo.size();
     for(int i = 0; i < size; i++) {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       String tsurl = fIntf.getSURL(); 
       try {
        RemoteHostInfo remoteHostInfo = 
			new RemoteHostInfo(tsurl,_credential,logger,_theLogger,
			recursive,recursive, copyCase,false);
        remoteHostInfo.doList();
        Vector fids = remoteHostInfo.getRemoteListings();
        Object[] robj = fids.toArray();
        for(int j = 0; j < robj.length; j++) {
          result.addElement(robj[j]);
        }
       } catch(Exception e) { 
              util.printEventLogException(_theLogger,"",e);
          e.printStackTrace();
          int idx = e.getMessage().indexOf("Unexpected reply");
          if(idx != -1) {
            util.printMessage("\nServer refused performing the request. Browsing Gridftp server failed",logger,silent);
            if(_debug) {
               util.printMessage("\n"+e.getMessage()+"\n",logger,silent);
            }
            return sCode = TStatusCode.SRM_FAILURE; 
          }
          else {
            throw e;
          }
       }
     }
     printResults(result,outputFile,resultVec,copyCase,targetDir,sourceUrl);
     return sCode = TStatusCode.SRM_SUCCESS;
   }
   else if(doLocalLsList) {
     //for put gsiftp recursive and local cp recursive, first create a parent directory 
     if(copyCase) {
        if(targetDir.startsWith("file:") && sourceUrl.startsWith("file:")) { 
           int idx = targetDir.lastIndexOf("/");
           if(idx != -1) {
             String tempTargetDir = targetDir.substring(0,idx);
             idx = sourceUrl.lastIndexOf("/");
             if(idx != -1) {
               String ppath = sourceUrl.substring(idx+1);
               String gtemp = 
					SRMUtilClient.parseLocalSourceFileForPath (tempTargetDir);
               try {
                 File f = new File(gtemp+"/"+ppath);
                 if(!f.exists()) {
                   f.mkdir();
                   System.out.println(
					"SRM-CLIENT:Destination parent directory created " + 
										gtemp+"/"+ppath);
                 }
                 else {
                   if(f.isDirectory()) {
                    System.out.println(
					  "SRM-CLIENT:Destination parent directory already exists" 
							+ gtemp+"/"+ppath);
                   }
                   else {
                    System.out.println(
					  "SRM-CLIENT:Destination path exists and it is a file" + 
							gtemp+"/"+ppath);
                    System.exit(92);
                   }
                 }
               }catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
                 System.out.println(
					"SRM-CLIENT:Destination parent directory not created " + 
						gtemp+"/"+ppath);
               }
             }
           }
        }
        else {
          int idx = targetDir.lastIndexOf("/");
          if(idx != -1) {
            String tempTargetDir = targetDir.substring(0,idx);
            RemoteHostInfo rrInfo = new RemoteHostInfo (
					tempTargetDir, _credential, 
                    logger, _theLogger, false,true,copyCase,false); 
            idx = sourceUrl.lastIndexOf("/");
            if(idx != -1) {
              //to check whether remote surl is a directory or file
              rrInfo.doList();
              String ppath = sourceUrl.substring(idx+1);  
              int val = rrInfo.doMkdir(ppath);
              if(val == 0) {
                System.out.println("SRM-CLIENT: Destination parent directory created " + tempTargetDir+"/"+ppath);
              }
              else {
                if(val == 1) {   
                  System.out.println
					 ("SRM-CLIENT: Destination parent directory could not be created " + tempTargetDir+"/"+ppath);
                }
                else if (val == 2) {
                  System.out.println
					("SRM-CLIENT: Destination parent directory already exists " + tempTargetDir+"/"+ppath);
                }
              }
            }
          }//end if
        }//end else 
     }//end if(copyCase)
     Vector result = new Vector ();
     int size = fileInfo.size();
     for(int i = 0; i < size; i++) {
        FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
        String tsurl = fIntf.getSURL();
        try {
          LocalListInfo localListInfo =
             new LocalListInfo(tsurl,logger,_theLogger,recursive,false);
          localListInfo.doList();
          Vector fids = localListInfo.getLocalListings();
          Object[] robj = fids.toArray();
          for(int j = 0; j < robj.length; j++) {
            result.addElement(robj[j]);
          }
        }catch(Exception e) {throw e;}
     }
     printResults(result,outputFile,resultVec,copyCase,targetDir,sourceUrl);
     return sCode = TStatusCode.SRM_SUCCESS;
   }
   else {
     ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
     byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
     String proxyString = new String(bb);

     util.printMessage("\nSRM-DIR: " + new Date() +
		" Calling srmLsRequest", logger,silent);
     util.printMessageHL("\nSRM-DIR: " + new Date() +
		" Calling srmLsRequest", pIntf);
     if(_debug) {
       util.printMessage("\nSRM-DIR: ..........................",logger,silent);
       util.printMessage("\nSRM-DIR: ..........................",pIntf);
       util.printMessage("SRM-DIR: .....Input parameters for SrmLsRequest ....",
		    logger,silent);
       util.printMessage("SRM-DIR: .....Input parameters for SrmLsRequest ....",
		    pIntf);
     }
         
     inputVec.clear(); 
     inputVec.addElement("AuthorizationID="+uid);
     inputVec.addElement("FullDetailedList="+fullDetailedList);
     inputVec.addElement("Recursive="+recursive);
     inputVec.addElement("NumOfLevels="+numLevels);
     inputVec.addElement("Offset="+offset);
     inputVec.addElement("Count="+count);

     if(_debug) {
       util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
       util.printMessage("SRM-DIR: AuthorizationID="+uid,pIntf);
       util.printMessage("SRM-DIR: FullDetailedList="+fullDetailedList, 
			logger,silent);
       util.printMessage("SRM-DIR: FullDetailedList="+fullDetailedList, 
			pIntf);
       util.printMessage("SRM-DIR: Recursive="+recursive,logger,silent);
       util.printMessage("SRM-DIR: Recursive="+recursive,pIntf);
       if(numLevels != -1) {
         util.printMessage("SRM-DIR: NumOfLevels="+numLevels,logger,silent);
         util.printMessage("SRM-DIR: NumOfLevels="+numLevels,pIntf);
       }
       if(offset != 0) {
         util.printMessage("SRM-DIR: Offset="+offset,logger,silent);
         util.printMessage("SRM-DIR: Offset="+offset,pIntf);
       }
       if(count != 0) {
         util.printMessage("SRM-DIR: Count="+count,logger,silent);
         util.printMessage("SRM-DIR: Count="+count,pIntf);
       }
     }

     SrmLsRequest req = new SrmLsRequest();
     int size = fileInfo.size();
     URI[] path = new URI[size];

     for(int i = 0; i < size; i++) {
       FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
       URI uri = new URI(fIntf.getSURL());
       if(_debug) {
         util.printMessage("SRM-DIR: SURL="+ uri.toString(),logger,silent); 
         util.printMessage("SRM-DIR: SURL="+ uri.toString(),pIntf); 
       }
       inputVec.addElement("SURL="+uri.toString());

      /*
       if(storageInfo.equals("")) {
         int idx = fIntf.getSURL().indexOf("mss://");
         if(idx != -1) {
           storageInfo = ""+true;
         }
       }
      */

       path[i] = uri;
     }

     if(_debug) {
      util.printMessage("SRM-DIR: UseStorageSystemInfo="+
	 	storageInfo,logger,silent);
      util.printMessage("SRM-DIR: UseStorageSystemInfo="+ storageInfo,pIntf);
     }
     inputVec.addElement("UseStorageSystemInfo="+storageInfo);

     req.setArrayOfSURLs(SRMUtilClient.convertToArrayOfAnyURI(path));
     if(recursive) {
       req.setAllLevelRecursive(new Boolean(recursive));
     }
     if(fullDetailedList) {
       req.setFullDetailedList(new Boolean(fullDetailedList));
     }
     if(!uid.equals("")) {
       req.setAuthorizationID(uid);
     }
     if(numLevels != -1) {
       req.setNumOfLevels(new Integer(numLevels));
     }
     if(count != 0) {
       req.setCount(new Integer(count));
     }
     if(offset != 0) {
       req.setOffset(new Integer(offset));
     } 


     ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
     TExtraInfo tExtra = new TExtraInfo();
     Vector vec = new Vector ();
     if(storageInfo.equals("true")) {

       tExtra.setKey("uid");
       tExtra.setValue("ls");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
    }
 
    TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
    for(int i = 0; i < vec.size(); i++) {
       a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
    }
	storageInfoArray.setExtraInfoArray(a_tExtra);
	req.setStorageSystemInfo(storageInfoArray);

    if(fileType != null) {
      req.setFileStorageType(fileType);
      if(_debug) {
        util.printMessage("SRM-DIR: FileStorageType="+fileType,logger,silent);
        util.printMessage("SRM-DIR: FileStorageType="+fileType,pIntf);
      }
      inputVec.addElement("FileStorageType="+fileType);
    }

    util.printEventLog(_theLogger,"DoSrmLsRequest", inputVec,silent,useLog);

    SrmLsResponse result = null;


    result = (SrmLsResponse) callSoapThread(req,result,"srmls");

    inputVec.clear(); 

    util.printMessage("\nSRM-DIR: ..........................",logger,silent);
    util.printMessage("\nSRM-DIR: ..........................",pIntf);
    if(_debug) {
      util.printMessage("SRM-DIR: #### Output from SRM ####", logger,silent);
      util.printMessage("SRM-DIR: #### Output from SRM ####", pIntf);
    }

    if(result == null) {
      util.printMessage("\nNull result from srmLs", logger,silent);
      util.printMessage("\nNull result from srmLs", pIntf);
      inputVec.addElement("Null result from srmLs");
      util.printEventLog(_theLogger,"DoSrmLsResponse", inputVec,silent,useLog);
      if(pIntf != null) {
       pIntf.setCompleted(false);
      }
      return sCode;
    }

    if(result.getReturnStatus () == null) {
      util.printMessage("Null ReturnStatus from srmLs", logger,silent);
      util.printMessage("Null ReturnStatus from srmLs", pIntf);
      inputVec.addElement("Null ReturnStatus from srmLs");
      util.printEventLog(_theLogger,"DoSrmLsResponse", inputVec,silent,useLog);
      if(pIntf != null) {
       pIntf.setCompleted(false);
      }
      return sCode;
    }

    TReturnStatus rStatus = result.getReturnStatus();
    sCode = rStatus.getStatusCode();

    util.printMessage("\tStatus    : " + sCode,logger,silent);
    util.printMessage("\tStatus    : " + sCode,pIntf);
    util.printMessage("\tExplanation : " + 
	   result.getReturnStatus().getExplanation(), logger,silent);
    util.printMessage("\tExplanation : " + 
	   result.getReturnStatus().getExplanation(), pIntf);

    inputVec.addElement("Status=" + sCode);
    inputVec.addElement("Explanation=" + 
			result.getReturnStatus().getExplanation());

    HashMap resultMap = new HashMap();
    resultMap.put(uid, sCode);
    String rToken = result.getRequestToken();
    util.printMessage("\tRequest token=" + rToken, logger,silent);
    util.printMessage("\tRequest token=" + rToken, pIntf);
    inputVec.addElement("Request token=" + rToken);

    TMetaDataPathDetail[] pathDetail = null;
    
    //System.out.println(">>>STATUSWAITTIME="+statusWaitTime);
    if(!submitOnly) {
    if(sCode == TStatusCode.SRM_REQUEST_QUEUED ||
       sCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
        int i = 0;
        long sTimeStamp = util.startTimeStamp();
        while (resultMap.size() > 0) {
          boolean timedOutCase=false;

          int tStatusTime = 1;
          if(statusWaitTime >= 1000) {
             tStatusTime = statusWaitTime/1000;
          }

          util.printMessage("\nSRM-DIR: Next status call in " +
                  tStatusTime + " seconds.", logger,silent);
          util.printMessage("\nSRM-DIR: Next status call in " +
                  tStatusTime + " seconds.", pIntf);
          inputVec.clear();
          inputVec.addElement("NextStatus="+statusWaitTime+" seconds.");
          util.printEventLog(_theLogger,"NextStatusCall",inputVec,silent,useLog);
          Thread.sleep(statusWaitTime);
          if(i >= resultMap.size()) {
            i = 0;
          }
          if(rToken != null) {
            //if(statusMaxTimeAllowed != -1) {
             if(!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
               inputVec.addElement(
					"Max retry check status exceeded for ls status");
               util.printMessage
					("SRM-DIR: Max retry check status exceeded for ls status", 
				     logger,silent);
               util.printMessage
					("SRM-DIR: Max retry check status exceeded for ls status", 
				     pIntf);
               timedOutCase=true;
             }
            //}
 
            if(!timedOutCase) {
            SrmStatusOfLsRequestRequest lsStatusRequest =
             new SrmStatusOfLsRequestRequest();
            lsStatusRequest.setRequestToken(rToken);
            if(!uid.equals("")) {
              lsStatusRequest.setAuthorizationID(uid);
            }
            if(count != 0) {
              lsStatusRequest.setCount(new Integer(count));
            }
            if(offset != 0) {
              lsStatusRequest.setOffset(new Integer(offset));
            }
            util.printMessage("SRM-DIR: " + new Date() +
				" Calling SrmStatusOfLsRequest " , logger,silent);
            util.printMessage("SRM-DIR: " + new Date() +
				" Calling SrmStatusOfLsRequest " , pIntf);

            SrmStatusOfLsRequestResponse lsStatusResponse = null;

            lsStatusResponse = (SrmStatusOfLsRequestResponse) 
                callSoapThread(lsStatusRequest,lsStatusResponse,"srmstatusofls");

            inputVec.clear(); 
            util.printEventLog(_theLogger,"SrmStatusOfLsResponse",
				inputVec,silent,useLog);
            util.printMessage("SRM-DIR: .......................",logger,silent);
            util.printMessage("SRM-DIR: .......................",pIntf);
            if(lsStatusResponse != null) {  
              if(lsStatusResponse.getReturnStatus() != null) {
                rStatus = lsStatusResponse.getReturnStatus();
                sCode = rStatus.getStatusCode();
                inputVec.addElement("Status="+sCode.toString());
                inputVec.addElement("Explanation="+rStatus.getExplanation());
                util.printMessage(
					"SRM-DIR: Status Code for lsStatusRequest=" + 
						sCode.toString(), logger,silent);
                util.printMessage(
					"SRM-DIR: Status Code for lsStatusRequest=" + 
						sCode.toString(), pIntf);
                util.printMessage("SRM-DIR: explanation=" + 
					rStatus.getExplanation(), logger,silent); 
                util.printMessage("SRM-DIR: explanation=" + 
					rStatus.getExplanation(), pIntf); 
                util.printEventLog(_theLogger,"SrmStatusOfLsResponse",
					inputVec,silent,useLog);
                if(sCode != TStatusCode.SRM_REQUEST_INPROGRESS &&
                   sCode != TStatusCode.SRM_REQUEST_QUEUED) {
                   pathDetail = 
					   lsStatusResponse.getDetails().getPathDetailArray(); 
				   resultMap.remove(uid); }
              }
              else {
                inputVec.addElement(
					"Null return status from srmStatusOfLsRequest");
                util.printEventLog(_theLogger,"SrmStatusOfLsResponse",
					inputVec,silent,useLog);
                util.printMessage(
					"\nSRM-DIR: Null return status from srmStatusOfLsRequest",
					   logger,silent);
                util.printMessage(
					"\nSRM-DIR: Null return status from srmStatusOfLsRequest",
					   pIntf);
              }
            }
            else {
                inputVec.addElement("Null response from srmStatusOfLsRequest");
                util.printEventLog(_theLogger,"SrmStatusOfLsResponse",	
					inputVec,silent,useLog);
                util.printMessage(
				  "\nSRM-DIR: Null response from srmStatusOfLsRequest",
					   logger,silent);
                util.printMessage(
				  "\nSRM-DIR: Null response from srmStatusOfLsRequest",pIntf);
            }
            }
            else {
              resultMap.remove(uid);
            }
          }
          else {
            util.printMessage(
			  "\nSRM-DIR: Expecting requestToken for this status code " +
				"for lsRequest from SRM",	logger,silent);
            util.printMessage(
			  "\nSRM-DIR: Expecting requestToken for this status code " +
				"for lsRequest from SRM",	pIntf);
            inputVec.addElement("Expecting requestToken for this status code " +
				"for lsRequest from SRM");
            util.printEventLog(_theLogger,"SrmStatusOfLsResponse",
				inputVec,silent,useLog);
            resultMap.remove(uid);
          }
          statusWaitTime = statusWaitTime*2;
          if(statusWaitTime >= threshHold*1000) {
            statusWaitTime = 30;
          }
        }
    } 
    else {
       pathDetail = result.getDetails().getPathDetailArray();
    }
   }
   else {
      if(result.getDetails() != null) {
         pathDetail = result.getDetails().getPathDetailArray();
      }
      else {
         util.printMessage("\nSRM-DIR: SRM returned null getDetails for srmLs",
             logger,silent);
         util.printMessage("\nSRM-DIR: SRM returned null getDetails for srmLs",
             pIntf);
         if(pIntf != null) {
          pIntf.setCompleted(false);
         }
         return sCode;
      }
   }
   
    if(pathDetail == null) {
      util.printMessage("\nSRM-DIR: SRM returned Null pathDetail for srmLs", 
		logger,silent);
      util.printMessage("\nSRM-DIR: SRM returned Null pathDetail for srmLs", 
		pIntf);
      inputVec.addElement("SRM returned Null result for srmLs");
      util.printEventLog(_theLogger,"SrmStatusOfLsResponse",inputVec,silent,useLog);
         if(pIntf != null) {
          pIntf.setCompleted(false);
         }
      return sCode;
    }

    for(int i = 0; i < pathDetail.length; i++) {
      //printMetaData("\t\t",pathDetail[i],getRecursive,resultVec,surl,turl);
      printMetaData("\t",pathDetail[i],getRecursive,resultVec,surl,turl);
    }

    if(textReport) {
      inputVec.clear(); 
      //if(_debug) {
         util.printMessage("\nSRM-DIR: Printing text report now ...",
				logger,silent);
         util.printMessage("\nSRM-DIR: Printing text report now ...", pIntf);
      //}
      util.printEventLog(_theLogger,"PrintingTextReport",inputVec,silent,useLog);
      if(rStatus.getStatusCode().toString().equals("SRM_FAILURE")) {
        if(pIntf != null) {
           pIntf.setCompleted(false);
        }
      }
      else {
        if(pIntf != null) {
           pIntf.setCompleted(true);
        }
      }
      util.printMessage("SRM-CLIENT*REQUEST_STATUS="+
			rStatus.getStatusCode().toString(),logger,silent);
      util.printMessage("SRM-CLIENT*REQUEST_STATUS="+
			rStatus.getStatusCode().toString(),pIntf);
      if(rStatus.getExplanation() != null) {
        util.printMessage("SRM-CLIENT*REQUEST_EXPLANATION="+ 
			rStatus.getExplanation(),logger,silent);
        util.printMessage("SRM-CLIENT*REQUEST_EXPLANATION="+ 
			rStatus.getExplanation(),pIntf);
      }
      for(int i = 0; i < pathDetail.length; i++) {
        //printMetaDataForTextReport("\t\t",pathDetail[i],getRecursive, surl,turl);
        printMetaDataForTextReport("\t",pathDetail[i],getRecursive, surl,turl);
      }
      inputVec.clear(); 
      util.printEventLog(_theLogger,"PrintedTextReport",inputVec,silent,useLog);
      if(!outputFile.equals("")) {
        printXMLReport(rStatus,pathDetail,getRecursive,surl,turl,outputFile,esgReportType);
      }
    }
  }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"PrintingTextReport",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused"); 
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-DIR: Exception " + msg, logger,silent);
    util.printMessageHException("SRM-DIR: Exception " + msg, pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
				" please check your proxy", logger,silent);
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLsStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmLsStatus(String rToken, boolean doGsiFTPList,
    int count, int offset,  String outputFile,
	boolean getRecursive, Vector resultVec, String surl, String turl) 
		throws Exception {

   TStatusCode sCode = null;
 try {
   if(_debug) {
     util.printMessage("\nSRM-DIR: ..........................",logger,silent);
   }

   inputVec.clear(); 
   if(doGsiFTPList) {
     inputVec.addElement("SRMLsStatus for doGsiFTPList is not supported");
     util.printEventLog(_theLogger,"DoSrmLsStatus",inputVec,silent,useLog);
     util.printMessage
		("SRM-DIR: SRMLsStatus for doGsiFTPList is not supported",
			logger,silent);
     if(pIntf != null) {
      pIntf.setCompleted(false);
     }
     return sCode;
   }

   util.printMessage("\nSRM-DIR: " + new Date() + 
      " Calling srmLsStatus ", logger,silent); 	
   if(_debug) {
     util.printMessage(
		"SRM-DIR: ....Input parameters for SrmStatusOfLsRequest...",
	    logger,silent);
   }
         
   inputVec.addElement("AuthorizationID="+uid);
   inputVec.addElement("Offset="+offset);
   inputVec.addElement("count="+count);
   if(_debug) {
     util.printMessage("SRM-DIR: AuthorizationID="+uid,logger,silent);
   }

   if(_debug) {
    if(offset != 0) {
       util.printMessage("SRM-DIR: Offset="+offset,logger,silent);
    }
    if(count != 0) {
       util.printMessage("SRM-DIR: Count="+count,logger,silent);
    }
   }

   util.printEventLog(_theLogger,"SrmStatusOfLsRequestRequest", inputVec,silent,useLog);
   if(rToken != null) {
      SrmStatusOfLsRequestRequest lsStatusRequest =
        new SrmStatusOfLsRequestRequest();
      lsStatusRequest.setRequestToken(rToken);
      if(!uid.equals("")) {
         lsStatusRequest.setAuthorizationID(uid);
      }
      if(count != 0) {
         lsStatusRequest.setCount(new Integer(count));
      }
      if(offset != 0) {
         lsStatusRequest.setOffset(new Integer(offset));
      }

      SrmStatusOfLsRequestResponse lsStatusResponse = null;

      lsStatusResponse = (SrmStatusOfLsRequestResponse) 
           callSoapThread(lsStatusRequest,lsStatusResponse,"srmstatusofls");

      inputVec.clear(); 
      util.printMessage("SRM-DIR: .......................",logger,silent);
      if(lsStatusResponse != null) {  
         if(lsStatusResponse.getReturnStatus() != null) {
          TReturnStatus rStatus = lsStatusResponse.getReturnStatus();
          sCode = rStatus.getStatusCode();
          util.printMessage("SRM-DIR: Status Code for lsStatusRequest=" + 
				sCode.toString(), logger,silent);
          util.printMessage("SRM-DIR: explanation=" + 
				rStatus.getExplanation(), logger,silent); 
          inputVec.addElement("Status="+sCode.toString());
          inputVec.addElement("Explanation="+rStatus.getExplanation());
          if(lsStatusResponse.getDetails() != null) {
           TMetaDataPathDetail[] pathDetail = 
				lsStatusResponse.getDetails().getPathDetailArray(); 
           util.printEventLog(_theLogger,
			"SrmStatusOfLsRequestResponse", inputVec,silent,useLog);
           for(int i = 0; i < pathDetail.length; i++) {
             inputVec.clear(); 
             //printMetaData( "\t\t",pathDetail[i],getRecursive,resultVec,surl,turl);
             printMetaData("\t",pathDetail[i],getRecursive,resultVec,surl,turl);
           }
          }
        }
        else {
           inputVec.addElement(
				"Null return status from srmStatusOfLsRequest");
           util.printEventLog(_theLogger,
				"SrmStatusOfLsRequestResponse", inputVec,silent,useLog);
           util.printMessage(
			"\nSRM-DIR: Null return status from srmStatusOfLsRequest",							logger,silent);
        }
      }
      else {
         util.printMessage(
			"\nSRM-DIR: Null response from srmStatusOfLsRequest",logger,silent);
         inputVec.addElement("Null response from srmStatusOfLsRequest");
         util.printEventLog(_theLogger,"SrmStatusOfLsRequestResponse", inputVec,silent,useLog);
      }
    }
    else {
      util.printMessage
		("\nSRM-DIR: Expecting requestToken for this status code for lsRequest from SRM",	
   	      logger,silent);
      inputVec.addElement("Expecting requestToken for this status code " +
			"for lsRequest from SRM");
      util.printEventLog(_theLogger,"SrmStatusOfLsRequestResponse", inputVec,silent,useLog);
    }
  }   
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmStatusOfLsRequest", inputVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("\nSRM-DIR: Exception " + e.getMessage(),logger,silent);
   util.printHException(e,pIntf);
   if(pIntf != null) {
     pIntf.setCompleted(false);
    }
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     if(pIntf == null) {
      if(idx != -1) {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception :  proxy type mismatch " +
					" please check your proxy type ", logger,silent); 
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+96);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(96);
      }
      else {
             inputVec.clear(); 
             inputVec.addElement("ExitStatus="+91);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(91);
      }
     }
   }
   else {
     throw e;
   }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMetaDataForTextReport
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printMetaDataForTextReport(String prefix, TMetaDataPathDetail pDetail,
		boolean getRecursive, String surl, String turl) {

  if(getRecursive) {
    if(pDetail.getArrayOfSubPaths() != null) {
       TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
       for(int i = 0; i<subPDetail.length;i++) {
         //printMetaDataForTextReport(prefix+"\t\t\t",subPDetail[i],getRecursive, surl,turl);
         printMetaDataForTextReport(prefix+"\t",subPDetail[i],getRecursive, surl,turl);
       }
    }
  } 
  else {
  if(pDetail.getPath() != null) {
    //if(_debug) {
    util.printMessage("SRM-CLIENT*SURL="+ pDetail.getPath(),
			logger,silent);
    util.printMessage("SRM-CLIENT*SURL="+ pDetail.getPath(), pIntf);
    //}
  }

  if(pDetail.getSize() != null) {
    //if(_debug) {
    util.printMessage("SRM-CLIENT*BYTES=" + pDetail.getSize(), 
			logger,silent);
    util.printMessage("SRM-CLIENT*BYTES=" + pDetail.getSize(), pIntf);
    //}
  }

  if(pDetail.getType() != null) {
    //if(_debug) {
    util.printMessage("SRM-CLIENT*FILETYPE=" + pDetail.getType().getValue(),
			logger,silent);
    util.printMessage("SRM-CLIENT*FILETYPE=" + pDetail.getType().getValue(),
			pIntf);
    //}
  }

  if(pDetail.getFileStorageType() != null) {
    //if(_debug) {
     util.printMessage("SRM-CLIENT*STORAGETYPE=" + 
			pDetail.getFileStorageType(), logger,silent);
     util.printMessage("SRM-CLIENT*STORAGETYPE=" + 
			pDetail.getFileStorageType(), pIntf);
    //}
  }

  if(pDetail.getStatus() != null && pDetail.getStatus().getStatusCode() != null) {
   //if(_debug) {
      util.printMessage("SRM-CLIENT*FILE_STATUS=" + 
			pDetail.getStatus().getStatusCode(), logger,silent);
      util.printMessage("SRM-CLIENT*FILE_STATUS=" + 
			pDetail.getStatus().getStatusCode(), pIntf);
    //}
  }

  if(pDetail.getStatus().getExplanation() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*FILE_EXPLANATION=" + 
		pDetail.getStatus().getExplanation(), logger,silent);
    util.printMessage("SRM-CLIENT*FILE_EXPLANATION=" + 
		pDetail.getStatus().getExplanation(), pIntf);
   //}
  }


  if(pDetail.getOwnerPermission() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*OWNERPERMISSION=" +
            pDetail.getOwnerPermission().getUserID(),logger,silent);
    util.printMessage("SRM-CLIENT*OWNERPERMISSION=" +
            pDetail.getOwnerPermission().getUserID(),pIntf);
   //}
  }

  if(pDetail.getLifetimeLeft() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*LIFETIMELEFT=" +
            pDetail.getLifetimeLeft().intValue(),logger,silent);
    util.printMessage("SRM-CLIENT*LIFETIMELEFT=" +
            pDetail.getLifetimeLeft().intValue(),pIntf);
   //}
  }

  if(pDetail.getLifetimeAssigned() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*LIFETIMEASSIGNED=" +
            pDetail.getLifetimeAssigned().intValue(),logger,silent);
    util.printMessage("SRM-CLIENT*LIFETIMEASSIGNED=" +
            pDetail.getLifetimeAssigned().intValue(),pIntf);
   //}
  }

  if(pDetail.getCheckSumType() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*CHECKSUMTYPE=" +
            pDetail.getCheckSumType(),logger,silent);
    util.printMessage("SRM-CLIENT*CHECKSUMTYPE=" + 
			pDetail.getCheckSumType(),pIntf);
    //}
  }

  if(pDetail.getCheckSumValue() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*CHECKSUMVALUE=" +
            pDetail.getCheckSumValue(),logger,silent);
    util.printMessage("SRM-CLIENT*CHECKSUMVALUE=" +
            pDetail.getCheckSumValue(),pIntf);
   //}
  }

  if(pDetail.getFileLocality() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*FILELOCALITY=" +
            pDetail.getFileLocality().getValue(),logger,silent);
    util.printMessage("SRM-CLIENT*FILELOCALITY=" +
            pDetail.getFileLocality().getValue(),pIntf);
    //}
  }

  if(pDetail.getOwnerPermission() != null) {
    TUserPermission uPermission = pDetail.getOwnerPermission();
    //if(_debug) {
    util.printMessage("SRM-CLIENT*OWNERPERMISSION.USERID=" +
            uPermission.getUserID(),logger,silent);
    util.printMessage("SRM-CLIENT*OWNERPERMISSION.USERID=" +
            uPermission.getUserID(),pIntf);
    util.printMessage("SRM-CLIENT*OWNERPERMISSION.MODE=" +
            uPermission.getMode().getValue(),logger,silent);
    util.printMessage("SRM-CLIENT*OWNERPERMISSION.MODE=" +
            uPermission.getMode().getValue(),pIntf);
    //}
  }

  if(pDetail.getGroupPermission() != null) {
    TGroupPermission gPermission = pDetail.getGroupPermission();
    //if(_debug) {
    util.printMessage("SRM-CLIENT*GROUPPERMISSION.GROUPID=" +
            gPermission.getGroupID(),logger,silent);
    util.printMessage("SRM-CLIENT*GROUPPERMISSION.GROUPID=" +
            gPermission.getGroupID(),pIntf);
    util.printMessage("SRM-CLIENT*GROUPPERMISSION.MODE=" +
            gPermission.getMode().getValue(),logger,silent);
    util.printMessage("SRM-CLIENT*GROUPPERMISSION.MODE=" +
            gPermission.getMode().getValue(),pIntf);
    //}
  }

  if(pDetail.getOtherPermission() != null) {
   //if(_debug) {
    util.printMessage("SRM-CLIENT*OTHERPERMISSION=" +
            pDetail.getOtherPermission().getValue(),logger,silent);
    util.printMessage("SRM-CLIENT*OTHERPERMISSION=" +
            pDetail.getOtherPermission().getValue(),pIntf);
    //}
  }

  if(pDetail.getArrayOfSpaceTokens() != null) {
      ArrayOfString spaceTokens = pDetail.getArrayOfSpaceTokens();
      if(spaceTokens != null) {
        String[] tokens = spaceTokens.getStringArray(); 
        if(tokens != null) { 
         for(int k = 0; k < tokens.length; k++) {
          if(_debug) {
            util.printMessage("SRM-CLIENT*SPACETOKENS("+k+")="+tokens[k],
			logger,silent);
            util.printMessage("SRM-CLIENT*SPACETOKENS("+k+")="+tokens[k],
			pIntf);
          }
         }
        }
     }
  }

  if(pDetail.getRetentionPolicyInfo() != null) {
      TRetentionPolicyInfo retentionPolicyInfo =	
			pDetail.getRetentionPolicyInfo();
      if(retentionPolicyInfo != null) {
      TRetentionPolicy retentionPolicy = 			
			retentionPolicyInfo.getRetentionPolicy();
      TAccessLatency accessLatency = retentionPolicyInfo.getAccessLatency();
      if(retentionPolicy != null) {
        if(_debug) {
         util.printMessage("SRM-CLIENT*RETENTIONPOLICY="+
			retentionPolicy.getValue(), logger,silent);
         util.printMessage("SRM-CLIENT*RETENTIONPOLICY="+
			retentionPolicy.getValue(), pIntf);
        }
      }
      if(accessLatency != null) {
        if(_debug) {
         util.printMessage("SRM-CLIENT*ACCESSLATENCY="+
			accessLatency.getValue(), logger,silent);
         util.printMessage("SRM-CLIENT*ACCESSLATENCY="+
			accessLatency.getValue(), pIntf);
        }
      }
     }
  } 

  if (pDetail.getLastModificationTime() != null) {
    Calendar gcal = pDetail.getLastModificationTime();
    Date dd = gcal.getTime();
    int year = dd.getYear()+1900; 
    int month = dd.getMonth(); 
    int day = dd.getDate(); 
    int hour = dd.getHours(); 
    int minute = dd.getMinutes(); 
    int second = dd.getSeconds(); 
    //if(_debug) {
    util.printMessage("SRM-CLIENT*LASTACCESSED="+
             dd.toString(), logger,silent);
    /*
    util.printMessage("SRM-CLIENT*LASTACCESSED="+year+"-"+(month+1)+"-"+
			day+"-"+hour+"-"+minute+"-"+second , logger,silent);
    util.printMessage("SRM-CLIENT*LASTACCESSED="+year+"-"+(month+1)+"-"+
			day+"-"+hour+"-"+minute+"-"+second , pIntf);
    */
    //}
  }

  if (pDetail.getCreatedAtTime() != null) {
    Calendar gcal = pDetail.getCreatedAtTime();
    Date dd = gcal.getTime();
    int year = dd.getYear()+1900; 
    int month = dd.getMonth(); 
    int day = dd.getDate(); 
    int hour = dd.getHours(); 
    int minute = dd.getMinutes(); 
    int second = dd.getSeconds(); 
    //if(_debug) {
    util.printMessage("SRM-CLIENT*CREATEDATTIME="+
             dd.toString(), logger,silent);
    /*
    util.printMessage("SRM-CLIENT*CREATEDATTIME="+
		year+"-"+(month+1)+"-"+day+"-"+hour+"-"+minute+"-"+second, 
			logger,silent);
    util.printMessage("SRM-CLIENT*CREATEDATTIME="+
		year+"-"+(month+1)+"-"+day+"-"+hour+"-"+minute+"-"+second, pIntf);
    */ 
    //}
  }

  if(pDetail.getArrayOfSubPaths() != null) {
    TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
    if(subPDetail != null) {
    for(int i = 0; i<subPDetail.length;i++) {
       //printMetaDataForTextReport(prefix+"\t\t\t",subPDetail[i],getRecursive, surl,turl);
       printMetaDataForTextReport(prefix+"\t",subPDetail[i],getRecursive, surl,turl);
    }
    }
  }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMetaData
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printMetaData(String prefix, TMetaDataPathDetail pDetail,
		boolean getRecursive, Vector resultVec, String surl, String turl) 
			throws Exception {

  inputVec.clear(); 
  if(getRecursive) {
    if(pDetail.getPath() != null) {
       if(pDetail.getType().getValue().equals("FILE")) {
         MyGlobusURL gurl = new MyGlobusURL(surl,0);
         
         FileInfo fInfo = new FileInfo();
         int idx = gurl.getPath().indexOf("?SFN=");
         String tendpoint = gurl.getPath();
         if(idx != -1) {
           tendpoint = gurl.getPath().substring(0,idx);
         }
         fInfo.setSURL("srm://"+gurl.getHost()+":"+gurl.getPort()+tendpoint+"?SFN="+pDetail.getPath());
         fInfo.setOrigSURL("srm://"+gurl.getHost()+":"+gurl.getPort()+tendpoint+"?SFN="+pDetail.getPath());
         int iidx = pDetail.getPath().lastIndexOf("/");
         if(iidx != -1) {
           fInfo.setTURL(turl+"/"+pDetail.getPath().substring(iidx+1));
           fInfo.setOrigTURL(turl+"/"+pDetail.getPath().substring(idx+1));
         }
         fInfo.overWriteTURL(false);
         resultVec.addElement(fInfo);
         inputVec.addElement("PDetail.getType().getValue()="+
			pDetail.getType().getValue());
         //inputVec.addElement("Adding Remote SURL="+ pDetail.getPath());
         inputVec.addElement("Adding Remote SURL="+ fInfo.getSURL());
         util.printEventLog(_theLogger,"PrintMetaData",inputVec,silent,useLog);
         //util.printMessage("SRM-DIR:Adding Remote SURL="+ pDetail.getPath(), 
			//logger,silent);
         util.printMessage("SRM-DIR:Adding Remote SURL="+ fInfo.getSURL(), 
			logger,silent);
         util.printMessage("SRM-DIR:TURL="+fInfo.getTURL(),logger,silent);
         String gtemp = SRMUtilClient.parseLocalSourceFileForPath
					(fInfo.getTURL());
         int iiidx = gtemp.lastIndexOf(File.separator);
         String ttemp = gtemp;
         if(iiidx != -1) {
            ttemp = gtemp.substring(0,iiidx); 
            File f = new File(ttemp);
            f.mkdirs();
            System.out.println("SRM-DIR: created local directory " + ttemp);
         }
          
         //util.printMessage("SRM-DIR:Adding Remote SURL="+ pDetail.getPath(),pIntf);
         util.printMessage("SRM-DIR:Adding Remote SURL="+ fInfo.getSURL(),pIntf);
       }
       else if(pDetail.getType().getValue().equals("DIRECTORY")) {
         inputVec.addElement("PDetail.getType().getValue()="+
			pDetail.getType().getValue());
         inputVec.addElement("pDetail.getPath()="+ pDetail.getPath());
         String temp = pDetail.getPath();
         if(temp.endsWith("/")) {
            temp = temp.substring(0,(temp.length()-1));
         }
         String ttemp = temp;
         int idx = temp.lastIndexOf("/");
         if(idx != -1) {
           ttemp = temp.substring(idx+1);
         }
         try {
          String gtemp = SRMUtilClient.parseLocalSourceFileForPath
					(turl+"/"+ttemp);
          File f = new File(gtemp);
          turl = "file:///"+gtemp;
          if(!f.exists()) {
            f.mkdir();
          }
          else {
            if(!f.isDirectory()) {
             inputVec.addElement(
				"Could not create dir at the local path, file already"+
					" exist, please remove it first");
             util.printMessage("Could not create dir at the local path, " +
				"file already exist, please remove it first",logger,silent);
             util.printMessage("Could not create dir at the local path, " +
				"file already exist, please remove it first",pIntf);
             util.printEventLog(_theLogger,"PrintMetaData",inputVec,silent,useLog);
             if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
             }
            }
          }
         }catch(Exception ioe) {
            util.printEventLogException(_theLogger,"",ioe);
            util.printMessage("IOException " + ioe.getMessage(),logger,silent);
            util.printMessageHException("IOException "+ioe.getMessage(),pIntf);
            inputVec.clear(); 
            inputVec.addElement("Exception="+ioe.getMessage());
            util.printEventLog(_theLogger,"PrintMetaData",inputVec,silent,useLog);
		 }
       }  
    }
    if(pDetail.getArrayOfSubPaths() != null) {
       TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
       for(int i = 0; i<subPDetail.length;i++) {
         //printMetaData(prefix+"\t\t\t",subPDetail[i],getRecursive, resultVec,surl,turl);
         //printMetaData(prefix+"\t",subPDetail[i],getRecursive, resultVec,surl,turl);
         printMetaData(prefix+"   ",subPDetail[i],getRecursive, resultVec,surl,turl);
       }
    }
  }
  else {
  inputVec.clear();
  if(pDetail.getPath() != null) {
    inputVec.addElement("SURL="+pDetail.getPath());
    util.printMessage("\n"+prefix+"SURL="+ pDetail.getPath(),
			logger,silent);
    util.printMessage("\n"+prefix+"SURL="+ pDetail.getPath(), pIntf);
  }
  else {
    inputVec.addElement("SURL=null");
    util.printMessage(prefix+"SURL=null", logger,silent);
    util.printMessage(prefix+"SURL=null", pIntf);
  }

  if(pDetail.getSize() != null) {
    inputVec.addElement("Bytes="+pDetail.getSize());
    util.printMessage(prefix+"Bytes=" + pDetail.getSize(), 
			logger,silent);
    util.printMessage(prefix+"Bytes=" + pDetail.getSize(), pIntf);
  }
  else {
    inputVec.addElement("Bytes=null");
    util.printMessage(prefix+"Bytes=null", logger,silent);
    util.printMessage(prefix+"Bytes=null", pIntf);
  }

  if(pDetail.getType() != null) {
    inputVec.addElement("FileType="+pDetail.getType().getValue());
    util.printMessage(prefix+"FileType=" + pDetail.getType().getValue(),
			logger,silent);
    util.printMessage(prefix+"FileType=" + pDetail.getType().getValue(),
			pIntf);
  }
  else {
    inputVec.addElement("FileType=null");
    util.printMessage(prefix+"FileType=null", logger,silent);
    util.printMessage(prefix+"FileType=null", pIntf);
  }

  if(pDetail.getFileStorageType() != null) {
     inputVec.addElement("StorageType="+pDetail.getFileStorageType());
     util.printMessage(prefix+"StorageType=" + pDetail.getFileStorageType(), 
		logger,silent);
     util.printMessage(prefix+"StorageType=" + pDetail.getFileStorageType(), 
		pIntf);
  }
  else {
     inputVec.addElement("StorageType=null");
     util.printMessage(prefix+"StorageType=null", logger,silent);
     util.printMessage(prefix+"StorageType=null", pIntf);
  }


  if(pDetail.getStatus() != null && pDetail.getStatus().getStatusCode() != null) {
    inputVec.addElement("Status="+pDetail.getStatus().getStatusCode());
    util.printMessage(prefix+"Status=" + pDetail.getStatus().getStatusCode(), 
		logger,silent);
    util.printMessage(prefix+"Status=" + pDetail.getStatus().getStatusCode(), 
		pIntf);
  }
  else {
    inputVec.addElement("Status=null");
    util.printMessage(prefix+"Status=null", logger,silent);
    util.printMessage(prefix+"Status=null", pIntf);
  }

  if(pDetail.getStatus().getExplanation() != null) {
    inputVec.addElement("Explanation="+pDetail.getStatus().getExplanation());
    util.printMessage(prefix+"Explanation=" + 
		pDetail.getStatus().getExplanation(), logger,silent);
    util.printMessage(prefix+"Explanation=" + 
		pDetail.getStatus().getExplanation(), pIntf);
  }
  else {
    inputVec.addElement("Explanation=null");
    util.printMessage(prefix+"Explanation=null" , logger,silent); 
    util.printMessage(prefix+"Explanation=null", pIntf); 
  }

  if(pDetail.getOwnerPermission() != null) {
    inputVec.addElement("OwnerPermission="+
		pDetail.getOwnerPermission().getUserID());
    util.printMessage(prefix+"OwnerPermission=" +
            pDetail.getOwnerPermission().getUserID(),logger,silent);
    util.printMessage(prefix+"OwnerPermission=" +
            pDetail.getOwnerPermission().getUserID(),pIntf);
  }
  else {
    inputVec.addElement("OwnerPermission=null");
    //util.printMessage(prefix+"OwnerPermission=null",logger,silent); 
    //util.printMessage(prefix+"OwnerPermission=null" , pIntf);
  }

  if(pDetail.getLifetimeLeft() != null) {
    inputVec.addElement("LifetimeLeft="+pDetail.getLifetimeLeft().intValue());
    util.printMessage(prefix+"LifetimeLeft=" +
            pDetail.getLifetimeLeft().intValue(),logger,silent);
    util.printMessage(prefix+"LifetimeLeft=" +
            pDetail.getLifetimeLeft().intValue(),pIntf);
  }
  else {
    inputVec.addElement("LifetimeLeft=null");
    //util.printMessage(prefix+"LifetimeLeft=null" ,logger,silent);
    //util.printMessage(prefix+"LifetimeLeft=null",pIntf);
  }

  if(pDetail.getLifetimeAssigned() != null) {
    inputVec.addElement("LifetimeAssigned="+
		pDetail.getLifetimeAssigned().intValue());
    util.printMessage(prefix+"LifetimeAssigned=" +
            pDetail.getLifetimeAssigned().intValue(),logger,silent);
    util.printMessage(prefix+"LifetimeAssigned=" +
            pDetail.getLifetimeAssigned().intValue(),pIntf);
  }
  else {
    inputVec.addElement("LifetimeAssigned=null");
    //util.printMessage(prefix+"LifetimeAssigned=null" , logger,silent);
    //util.printMessage(prefix+"LifetimeAssigned=null",pIntf); 
  }

  if(pDetail.getCheckSumType() != null) {
    inputVec.addElement("CheckSumType="+pDetail.getCheckSumType());
    util.printMessage(prefix+"CheckSumType=" +
            pDetail.getCheckSumType(),logger,silent);
    util.printMessage(prefix+"CheckSumType=" +
            pDetail.getCheckSumType(),pIntf);
  }
  else {
    inputVec.addElement("CheckSumType=null");
    //util.printMessage(prefix+"CheckSumType=null",logger,silent); 
    //util.printMessage(prefix+"CheckSumType=null" , pIntf);
  }

  if(pDetail.getCheckSumValue() != null) {
    inputVec.addElement("CheckSumValue="+pDetail.getCheckSumValue());
    util.printMessage(prefix+"CheckSumValue=" +
            pDetail.getCheckSumValue(),logger,silent);
    util.printMessage(prefix+"CheckSumValue=" +
            pDetail.getCheckSumValue(),pIntf);
  }
  else {
    inputVec.addElement("CheckSumValue=null");
    //util.printMessage(prefix+"CheckSumValue=null" , logger,silent);
    //util.printMessage(prefix+"CheckSumValue=null", pIntf);
  }

  if(pDetail.getFileLocality() != null) {
    inputVec.addElement("FileLocality="+pDetail.getFileLocality().getValue());
    util.printMessage(prefix+"FileLocality=" +
            pDetail.getFileLocality().getValue(),logger,silent);
    util.printMessage(prefix+"FileLocality=" +
            pDetail.getFileLocality().getValue(),pIntf);
  }
  else {
    inputVec.addElement("FileLocality=null");
    //util.printMessage(prefix+"FileLocality=null" , logger,silent);
    //util.printMessage(prefix+"FileLocality=null" , pIntf);
  }

  if(pDetail.getOwnerPermission() != null) {
    TUserPermission uPermission = pDetail.getOwnerPermission();
    inputVec.addElement("OwnerPermission.userId="+uPermission.getUserID());
    util.printMessage(prefix+"OwnerPermission.userId=" +
            uPermission.getUserID(),logger,silent);
    util.printMessage(prefix+"OwnerPermission.userId=" +
            uPermission.getUserID(),pIntf);
    inputVec.addElement("OwnerPermission.mode="+uPermission.getMode());
    util.printMessage(prefix+"OwnerPermission.mode=" +
            uPermission.getMode().getValue(),logger,silent);
    util.printMessage(prefix+"OwnerPermission.mode=" +
            uPermission.getMode().getValue(),pIntf);
  }
  else {
    inputVec.addElement("OwnerPermission=null");
    //util.printMessage(prefix+"OwnerPermission=null" , logger,silent);
  }

  if(pDetail.getGroupPermission() != null) {
    TGroupPermission gPermission = pDetail.getGroupPermission();
    util.printMessage(prefix+"GroupPermission.groupId=" +
            gPermission.getGroupID(),logger,silent);
    util.printMessage(prefix+"GroupPermission.groupId=" +
            gPermission.getGroupID(),pIntf);
    inputVec.addElement("GroupPermission.groupId="+gPermission.getGroupID());
    util.printMessage(prefix+"GroupPermission.mode=" +
            gPermission.getMode().getValue(),logger,silent);
    util.printMessage(prefix+"GroupPermission.mode=" +
            gPermission.getMode().getValue(),pIntf);
    inputVec.addElement("GroupPermission.mode="+gPermission.getMode());
  }
  else {
    inputVec.addElement("GroupPermission=null");
    //util.printMessage(prefix+"GroupPermission=null" , logger,silent);
  }

  if(pDetail.getOtherPermission() != null) {
    util.printMessage(prefix+"OtherPermission=" +
            pDetail.getOtherPermission().getValue(),logger,silent);
    util.printMessage(prefix+"OtherPermission=" +
            pDetail.getOtherPermission().getValue(),pIntf);
    inputVec.addElement("OtherPermission="+
		pDetail.getOtherPermission().getValue());
  }
  else {
    inputVec.addElement("OtherPermission=null");
    //util.printMessage(prefix+"OtherPermission=null" , logger,silent);
  }

  if(pDetail.getArrayOfSpaceTokens() != null) {
      ArrayOfString spaceTokens = pDetail.getArrayOfSpaceTokens();
      if(spaceTokens != null) {
      String[] tokens = spaceTokens.getStringArray(); 
      if(tokens != null) {
        for(int k = 0; k < tokens.length; k++) {
           util.printMessage(prefix+"Tokens("+k+")="+tokens[k],
			logger,silent);
           util.printMessage(prefix+"Tokens("+k+")="+tokens[k],pIntf);
           inputVec.addElement("Tokens("+k+")="+tokens[k]);
        }
      }
     }
  }
  else {
    inputVec.addElement("ArrayOfSpaceTokens=null");
    //util.printMessage(prefix+"ArrayOfSpaceTokens=null" , logger,silent);
  }

  if(pDetail.getRetentionPolicyInfo() != null) {
      TRetentionPolicyInfo retentionPolicyInfo =	
			pDetail.getRetentionPolicyInfo();
      if(retentionPolicyInfo != null) {
      TRetentionPolicy retentionPolicy = retentionPolicyInfo.getRetentionPolicy();
      TAccessLatency accessLatency = retentionPolicyInfo.getAccessLatency();
      if(retentionPolicy != null) {
         util.printMessage(prefix+"RetentionPolicy="+
			retentionPolicy.getValue(), logger,silent);
         util.printMessage(prefix+"RetentionPolicy="+
			retentionPolicy.getValue(), pIntf);
         inputVec.addElement("RetentionPolicy="+retentionPolicy.getValue());
      }
      if(accessLatency != null) {
         util.printMessage(prefix+"AccessLatency="+accessLatency.getValue(), 
			logger,silent);
         util.printMessage(prefix+"AccessLatency="+accessLatency.getValue(), 
			pIntf);
         inputVec.addElement("AccessLatency="+accessLatency.getValue());
      }
     }
  } 
  else {
    inputVec.addElement("RetentionPolicyInfo=null");
    //util.printMessage(prefix+"RetentionPolicyInfo=null" , logger,silent);
  }

  if (pDetail.getLastModificationTime() != null) {
    Calendar gcal = pDetail.getLastModificationTime();
    Date dd = gcal.getTime();
    util.printMessage(prefix+"LastModificationTime="+dd.toString(),
		logger,silent);
    util.printMessage(prefix+"LastModificationTime="+
		dd.toString(),pIntf);
    inputVec.addElement("LastModificationTime="+dd.toString());
    /*
    int year = dd.getYear()+1900; 
    int month = dd.getMonth(); 
    int day = dd.getDate(); 
    int hour = dd.getHours(); 
    int minute = dd.getMinutes(); 
    int second = dd.getSeconds(); 
    */
    /*
    int year = gcal.get(Calendar.YEAR);
    int month = gcal.get(Calendar.MONTH);
    int day = gcal.get(Calendar.DAY_OF_MONTH);
    int hour = gcal.get(Calendar.HOUR_OF_DAY);
    int minute = gcal.get(Calendar.MINUTE);
    int second = gcal.get(Calendar.SECOND);
    */
    /*
    util.printMessage(prefix+"     Year   : " + year, logger,silent);
    util.printMessage(prefix+"     Year   : " + year, pIntf);
    util.printMessage(prefix+"     Month  : " + (month+1), logger,silent);
    util.printMessage(prefix+"     Month  : " + (month+1), pIntf);
    util.printMessage(prefix+"     Day    : " + day, logger,silent);
    util.printMessage(prefix+"     Day    : " + day, pIntf);
    util.printMessage(prefix+"     Hour   : " + hour, logger,silent);
    util.printMessage(prefix+"     Hour   : " + hour, pIntf);
    util.printMessage(prefix+"     Minute : " + minute, logger,silent);
    util.printMessage(prefix+"     Minute : " + minute, pIntf);
    util.printMessage(prefix+"     Second : " + second, logger,silent);
    util.printMessage(prefix+"     Second : " + second, pIntf);
    inputVec.addElement("LastModificationTime="+
	   "year:"+year+":month:"+(month+1)+
       ":day:"+day+":hour:"+hour+":minute:"+minute+":second:"+second);		
    */
  }
  else {
    inputVec.addElement("LastModificationTime=null");
    //util.printMessage(prefix+"LastModificationTime=null" , logger,silent);
  }  

  if (pDetail.getCreatedAtTime() != null) {
    Calendar gcal = pDetail.getCreatedAtTime();
    Date dd = gcal.getTime();
    util.printMessage(prefix+"CreatedAtTime="+dd.toString(),logger,silent);
    util.printMessage(prefix+"CreatedAtTime="+dd.toString(),pIntf);
    int year = dd.getYear()+1900; 
    int month = dd.getMonth(); 
    int day = dd.getDate(); 
    int hour = dd.getHours(); 
    int minute = dd.getMinutes(); 
    int second = dd.getSeconds(); 
     /*
    int year = gcal.get(Calendar.YEAR);
    int month = gcal.get(Calendar.MONTH);
    int day = gcal.get(Calendar.DAY_OF_MONTH);
    int hour = gcal.get(Calendar.HOUR_OF_DAY);
    int minute = gcal.get(Calendar.MINUTE);
    int second = gcal.get(Calendar.SECOND);
    */
    /*
    util.printMessage(prefix+"     Year   : " + year, logger,silent);
    util.printMessage(prefix+"     Year   : " + year, pIntf);
    util.printMessage(prefix+"     Month  : " + (month+1), logger,silent);
    util.printMessage(prefix+"     Month  : " + (month+1), pIntf);
    util.printMessage(prefix+"     Day    : " + day, logger,silent);
    util.printMessage(prefix+"     Day    : " + day, pIntf);
    util.printMessage(prefix+"     Hour   : " + hour, logger,silent);
    util.printMessage(prefix+"     Hour   : " + hour, pIntf);
    util.printMessage(prefix+"     Minute : " + minute, logger,silent);
    util.printMessage(prefix+"     Minute : " + minute, pIntf);
    util.printMessage(prefix+"     Second : " + second, logger,silent);
    util.printMessage(prefix+"     Second : " + second, pIntf);
    inputVec.addElement("CreatedAtTime="+"year:"+year+":month:"+(month+1)+
       ":day:"+day+":hour:"+hour+":minute:"+minute+":second:"+second);		
    */
  }
  else {
    inputVec.addElement("CreatedAtTime=null");
    //util.printMessage(prefix+"CreatedAtTime=null" , logger,silent);
  }

  util.printEventLog(_theLogger,"PrintMetaData",inputVec,silent,useLog);
  if(pDetail.getArrayOfSubPaths() != null) {
    inputVec.clear(); 
    TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
    if(subPDetail != null) {
    for(int i = 0; i<subPDetail.length;i++) {
       //printMetaData(prefix+"\t\t\t",subPDetail[i],getRecursive, resultVec,surl,turl);
       //printMetaData(prefix+"\t",subPDetail[i],getRecursive, resultVec,surl,turl);
       printMetaData(prefix+"   ",subPDetail[i],getRecursive, resultVec,surl,turl);
    }
    }
  }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public static TStatusCode releaseSpace(ISRM srm, String token, String uid,
    boolean forceFileRelease, Log logger, boolean _debug, 
    GSSCredential credential, int proxyType, 
    String serverUrl,java.util.logging.Logger _theLogger,
    PrintIntf pIntf, SRMClientIntf _srmClientIntf,boolean silent,
    boolean useLog, int connectionTimeOutAllowed,
    int setHTTPConnectionTimeOutAllowed,
    String delegationNeeded, int numRetry, int retryTimeOut) 
        throws Exception {
		 
 TStatusCode sCode = null;
 Vector iVec = new Vector ();
 try {
    util.printMessage("SRM-CLIENT: " + new Date() + " Calling ReleaseSpace request ....", logger, silent);
    util.printMessageHL("SRM-CLIENT: " + new Date() + " Calling ReleaseSpace request ....", pIntf);
    if (token != null) {
      util.printMessage("SRM-CLIENT: Releasing space for token="+token,logger,silent);
      util.printMessageHL("SRM-CLIENT: Releasing space for token="+token,pIntf);
      iVec.addElement("Token="+token);
    } else {
      util.printMessage("SRM-CLIENT: Cannot release space with null token",logger,silent);
      util.printMessageHException("SRM-CLIENT: Cannot release space with null token",pIntf);
      iVec.addElement("Token=null");
    } 

    SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
    req.setSpaceToken(token);
    if(_debug) {
      util.printMessage("SRM-CLIENT: ....Input parameters ...",logger,silent);
      util.printMessageHL("SRM-CLIENT: ....Input parameters ...",pIntf);
    }
    if(!uid.equals("")) {
      req.setAuthorizationID(uid);
      iVec.addElement("AuthorizationID="+uid);
      if(_debug) {
        util.printMessage("SRM-CLIENT:AuthorizationID="+uid,logger,silent);
        util.printMessage("SRM-CLIENT:AuthorizationID="+uid,pIntf);
      }
    }
    req.setForceFileRelease(new Boolean(forceFileRelease));

    iVec.addElement("ForceFileRelease="+forceFileRelease);
    if(_debug) {
        util.printMessage("SRM-CLIENT:ForceFileRelease="+forceFileRelease,logger,silent);
        util.printMessage("SRM-CLIENT:ForceFileRelease="+forceFileRelease,pIntf);
    }

    /*
    if(storageInfo) {
       ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
       TExtraInfo[] a_tExtra = new TExtraInfo[2];

       TExtraInfo tExtra = new TExtraInfo();
       tExtra.setKey("uid");
       tExtra.setValue("reservespace");
       a_tExtra[0] = tExtra;
 
       tExtra = new TExtraInfo();
       tExtra.setKey("pwd");
       tExtra.setValue(proxyString);
       a_tExtra[1] = tExtra;
 
       storageInfoArray.setExtraInfoArray(a_tExtra);
       r.setStorageSystemInfo(storageInfoArray);
    } 
    */

    iVec.addElement("TimeStamp="+new Date());
    util.printEventLog(_theLogger,"SrmReleaseSpaceRequest",iVec,silent,useLog);

   
    SrmReleaseSpaceResponse result = null;
    result = (SrmReleaseSpaceResponse) 
        SRMUtilClient.callStaticSoapThread(srm,credential, proxyType, 
                serverUrl, req,result,"srmreleasespace",_theLogger,
                pIntf, _srmClientIntf, silent, useLog, 
                connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
                delegationNeeded, numRetry, retryTimeOut);

    if(_debug) {
    util.printMessage("\nSRM-CLIENT: #### Output from SRM ####",logger,silent);
    util.printMessageHL("\nSRM-CLIENT: #### Output from SRM ####",pIntf);
    }
    iVec.clear();
    if (result != null) {
      sCode = result.getReturnStatus().getStatusCode();
      util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
			logger,silent);
      util.printMessage("\tstatus="+result.getReturnStatus().getStatusCode(),
			pIntf);
      if(result.getReturnStatus().getExplanation() != null) {
       util.printMessage("\texplanation="+
	   result.getReturnStatus().getExplanation(),logger,silent);
       util.printMessage("\texplanation="+
	   result.getReturnStatus().getExplanation(),pIntf);
      }
      iVec.addElement("Status="+result.getReturnStatus().getStatusCode());
      iVec.addElement("Explanation="+result.getReturnStatus().getExplanation());
    }
    else {
      util.printMessage("\tSRM-CLIENT: SRM returned empty result", 
			logger, silent);
      util.printMessage("\tSRM-CLIENT: SRM returned empty result", 
			pIntf);
      iVec.addElement("SRM returned empty result");
    }
    util.printEventLog(_theLogger,"SrmReleaseSpaceResponse",iVec,silent,useLog);
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   iVec.clear();
   iVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmReleaseSpace",iVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-CLIENT: Exception " + e.getMessage(),logger,silent);
   util.printMessageHException("SRM-CLIENT: Exception " + e.getMessage(),pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf != null) {
      pIntf.setCompleted(false);
     }
     if(pIntf == null) {
             if(idx != -1) {
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type ", logger,silent);
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
               System.exit(96);
             }
             else {
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
               System.exit(91);
             }
     }
   }
   else {
     if(pIntf != null) {
      pIntf.setCompleted(false);
     }
     throw e;
   }
 }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReserveStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static TStatusCode doReserveStatus(ISRM srm, String uid, 
	String rToken, Log logger, boolean _debug, GSSCredential credential,
          int proxyType, String serverUrl, java.util.logging.Logger _theLogger,
          PrintIntf pIntf, SRMClientIntf _srmClientIntf,boolean silent,
          boolean useLog, int connectionTimeOutAllowed,
          int setHTTPConnectionTimeOutAllowed,
          String delegationNeeded, int numRetry, int retryTimeOut) 
                throws Exception {

   TStatusCode sCode = null;
   Vector iVec = new Vector();
   util.printMessage("SRM-CLIENT: " + new Date() +
		" Calling Reserve space status request",logger,silent);
   SrmStatusOfReserveSpaceRequestRequest spaceStatusRequest =
      new SrmStatusOfReserveSpaceRequestRequest();
   spaceStatusRequest.setRequestToken(rToken);

   if(!uid.equals("")) {
      spaceStatusRequest.setAuthorizationID(uid);
   }

   SrmStatusOfReserveSpaceRequestResponse spaceStatusResponse = null;

   spaceStatusResponse = (SrmStatusOfReserveSpaceRequestResponse) 
      SRMUtilClient.callStaticSoapThread(srm,credential, 
              proxyType, serverUrl,spaceStatusRequest,spaceStatusResponse,
              "srmstatusofreservespace",_theLogger,
              pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
              setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
              retryTimeOut);

   if(spaceStatusResponse != null) {
     if(spaceStatusResponse.getReturnStatus() != null) {
       TReturnStatus rStatus = spaceStatusResponse.getReturnStatus();
       sCode = rStatus.getStatusCode();
       util.printMessage
		("SRM-CLIENT: Status Code for spaceStatusRequest " + 
	   	    sCode.toString(), logger,silent);
       iVec.addElement("StatusCode="+sCode.toString());
       util.printEventLog(_theLogger,
	     "SrmStatusOfReserveSpaceResponse",iVec,silent,useLog);
       util.printMessage("\tStatus " +
			sCode.toString(),logger,silent);
       util.printMessage("\tExplanation " +
			rStatus.getExplanation(),logger,silent);
       if(spaceStatusResponse.getSpaceToken() != null) {
         util.printMessage("\tSpaceToken " +
			spaceStatusResponse.getSpaceToken(),logger,silent);
       }
       else {
         util.printMessage("\tSpaceToken null", logger,silent);
       } 
       if(spaceStatusResponse.getEstimatedProcessingTime() != null) {
         util.printMessage("\tEstimatedProcessingTime " +
			spaceStatusResponse.getEstimatedProcessingTime(),logger,silent);
       }
       if(spaceStatusResponse.getSizeOfTotalReservedSpace() != null) {
         util.printMessage("\tSizeOfTotalReservedSpace " +
			spaceStatusResponse.getSizeOfTotalReservedSpace(),logger,silent);
       }
       if(spaceStatusResponse.getSizeOfGuaranteedReservedSpace() != null) {
         util.printMessage("\tSizeOfGuaranteedReservedSpace " +
			spaceStatusResponse.getSizeOfGuaranteedReservedSpace(),logger,silent);
       }
       if(spaceStatusResponse.getLifetimeOfReservedSpace() != null) {
         util.printMessage("\tLifetimeOfReservedSpace " +
		spaceStatusResponse.getLifetimeOfReservedSpace(),logger,silent);
       }
       if(spaceStatusResponse.getRetentionPolicyInfo() != null) {
         TRetentionPolicy retentionPolicy = 
		spaceStatusResponse.getRetentionPolicyInfo().getRetentionPolicy();
         TAccessLatency accessLatency = 
		spaceStatusResponse.getRetentionPolicyInfo().getAccessLatency();
         if(retentionPolicy != null) {
           util.printMessage("\tRetentionPolicy " +
		    	retentionPolicy.getValue(), logger,silent);
         }
         if(accessLatency != null) {
           util.printMessage("\tAccessLatency " +
		    	accessLatency.getValue(), logger,silent);
         }
       }
     }
     else {
        iVec.addElement
		("Null return status from srmStatusOfReserveSpaceRequest");
        util.printEventLog(_theLogger,
				"SrmStatusOfReserveSpaceResponse",iVec,silent,useLog);
        System.out.println
		     ("SRM-CLIENT: Null return status " +
				"from srmStatusOfReserveSpaceRequest");
     }
   }
   else {
        System.out.println
		     ("SRM-CLIENT: return null result " +
				"from srmStatusOfReserveSpaceRequest");
   }
   return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// reserveSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public static String reserveSpace(ISRM srm, String uid, String userDesc,
	TRetentionPolicyInfo retentionPInfo, 
	long tSize, int tLifeTime, long gSize, 
	StringBuffer reportBuf, Log logger,
        boolean _debug,boolean submitOnly,
        TStatusCode sCode, StringBuffer statusBuf, GSSCredential credential,
        int proxyType, String serverUrl,java.util.logging.Logger _theLogger,
        PrintIntf pIntf, SRMClientIntf _srmClientIntf,boolean silent,
        boolean useLog, int connectionTimeOutAllowed,
        int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded, int numRetry, int retryTimeOut) 
                throws Exception {

 Vector iVec = new Vector ();

 String fileToken = null;
 try {

  util.printMessage("SRM-CLIENT: " + new Date() + 
		" Calling SrmReserveSpace", logger, silent);
  util.printMessageHL("SRM-CLIENT: " + new Date() + 
		" Calling SrmReserveSpace", pIntf);

  SrmReserveSpaceRequest r = new SrmReserveSpaceRequest();

  if(_debug) {
    util.printMessage("SRM-CLIENT: ... Input parameters...", logger,silent);
    util.printMessageHL("SRM-CLIENT: ... Input parameters...", pIntf);
  }
  if(!userDesc.equals("")) {
     if(_debug) {
       util.printMessage("SRM-CLIENT: UserSpaceTokenDescription="+userDesc, logger, silent);
       util.printMessage("SRM-CLIENT: UserSpaceTokenDescription="+userDesc, pIntf);
     }
     iVec.addElement("UserSpaceTokenDescription="+userDesc);
     r.setUserSpaceTokenDescription(userDesc);
   }

  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
    iVec.addElement("AuthorizationID="+uid);
    if(_debug) {
     util.printMessage("SRM-CLIENT: AuthorizationID="+uid,logger,silent);
     util.printMessage("SRM-CLIENT: AuthorizationID="+uid,pIntf);
    }
  }

  if(retentionPInfo != null) {
    r.setRetentionPolicyInfo(retentionPInfo);
    iVec.addElement("RetentionPolicy="+retentionPInfo.getRetentionPolicy());
    iVec.addElement("AccessLatency="+retentionPInfo.getAccessLatency());
    if(_debug) {
     util.printMessage("SRM-CLIENT: RetentionPolicy="+
		retentionPInfo.getRetentionPolicy(),logger,silent);
     util.printMessage("SRM-CLIENT: RetentionPolicy="+ 	
		retentionPInfo.getRetentionPolicy(),pIntf);
    }
  }

  //access latency is set by the configuration of the SRM
  
  iVec.addElement("DesiredSizeOfTotalSpace="+tSize);
  iVec.addElement("DesiredLifetimeOfReservedSpace="+tLifeTime);
  iVec.addElement("DesiredSizeOfGuaranteedSpace="+gSize);
  iVec.addElement("UserSpaceTokenDescription="+userDesc);

  if(tSize != 0) {
    r.setDesiredSizeOfTotalSpace(new UnsignedLong(tSize));
    if(_debug) {
      util.printMessage("SRM-CLIENT: DesiredSizeOfTotalSpace="+tSize,
			logger,silent);
      util.printMessage("SRM-CLIENT: DesiredSizeOfTotalSpace="+tSize,pIntf);
    }
  }
  else {
    //using some default value of 100000000 bytes
    r.setDesiredSizeOfTotalSpace(new UnsignedLong(100000000));
    if(_debug) {
      util.printMessage("SRM-CLIENT: Default DesiredSizeOfTotalSpace="+100000000,
			logger,silent);
      util.printMessage("SRM-CLIENT: Default DesiredSizeOfTotalSpace="+100000000,pIntf);
    }
  }
  
  if(tLifeTime != 0) { 
    r.setDesiredLifetimeOfReservedSpace(new Integer(tLifeTime));
    if(_debug) {
      util.printMessage("SRM-CLIENT: DesiredLifeTimeOfReserveSpace="+tLifeTime,logger,silent);
      util.printMessage("SRM-CLIENT: DesiredLifeTimeOfReserveSpace="+tLifeTime,pIntf);
    }
  }
  else {
    r.setDesiredLifetimeOfReservedSpace(new Integer(10000));
    if(_debug) {
      util.printMessage("SRM-CLIENT: Default DesiredLifeTimeOfReserveSpace="+10000,logger,silent);
      util.printMessage("SRM-CLIENT: Default DesiredLifeTimeOfReserveSpace="+10000,pIntf);
    }
  }

  if(gSize != 0) {
    r.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(gSize));
    if(_debug) {
      util.printMessage("SRM-CLIENT: DesiredSizeOfGuaranteedSpace="+gSize,logger,silent);
      util.printMessage("SRM-CLIENT: DesiredSizeOfGuaranteedSpace="+gSize,pIntf);
    }
  }
  else {
    r.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(1000));
    if(_debug) {
      util.printMessage("SRM-CLIENT: Default DesiredSizeOfGuaranteedSpace="+1000,
			logger,silent);
      util.printMessage("SRM-CLIENT: Default DesiredSizeOfGuaranteedSpace="+gSize,pIntf);
    }
  }

  r.setUserSpaceTokenDescription(userDesc);
  if(_debug) {
   util.printMessage("SRM-CLIENT: UserSpaceTokenDescription="+userDesc,logger,silent);
   util.printMessage("SRM-CLIENT: UserSpaceTokenDescription="+userDesc,pIntf);
  }

  iVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmReserveSpaceRequest", iVec,silent,useLog);
  SrmReserveSpaceResponse result = null;
  result = (SrmReserveSpaceResponse) 
        SRMUtilClient.callStaticSoapThread(srm,credential, 
           proxyType, serverUrl,r,result,"srmreservespace",_theLogger,
           pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
           setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
           retryTimeOut);

  HashMap resultMap = new HashMap ();
  

  util.printMessage("\nSRM-CLIENT: .......................",logger,silent);
  util.printMessage("\nSRM-CLIENT: .......................",pIntf);
  if(_debug) {
  util.printMessage("SRM-CLIENT: #### Output from SRM ####",logger,silent);
  util.printMessage("SRM-CLIENT: #### Output from SRM ####",pIntf);
  }
  iVec.clear(); 

  if(result == null) {
    iVec.addElement("SRM returned null result for srmReserveSpace");
    util.printEventLog(_theLogger,"SrmReserveSpaceRequest", iVec,silent,useLog);
    util.printMessage
		("\nSRM-CLIENT: SRM returned null result for srmReserveSpace",
		   logger,silent);
    util.printMessageHException
		("\nSRM-CLIENT: SRM returned null result for srmReserveSpace",
		   pIntf);
    return null;
  }

  TReturnStatus rStatus = result.getReturnStatus();
  if(rStatus == null) {
    util.printMessage("\nSRM-CLIENT: SRM returns null ReturnStatus for srmReserveSpace", logger,silent);
    util.printMessageHException("\nSRM-CLIENT: SRM returns null ReturnStatus for srmReserveSpace", pIntf);
    iVec.addElement("SRM returns null ReturnStatus for srmReserveSpace");
    util.printEventLog(_theLogger,"SrmReserveSpaceRequest", iVec,silent,useLog);
    return null;
  }

  sCode = rStatus.getStatusCode();
  
  resultMap.put(uid, sCode);
  String rToken = result.getRequestToken();
  //reportBuf.append("SRM-CLIENT*REQUEST_STATUS="+sCode.toString()+"\n");
  statusBuf.append(sCode.toString());
  if(rStatus.getExplanation() != null) {
    reportBuf.append("SRM-CLIENT*REQUEST_EXPLANATION="+rStatus.getExplanation()+"\n");
  }
  reportBuf.append("SRM-CLIENT*REQUEST-TOKEN="+rToken+"\n");
  util.printMessage("\tStatus Code=" + sCode.toString(), logger,silent);
  util.printMessage("\tStatus Code=" + sCode.toString(), pIntf);
  if(rToken != null) {
    util.printMessage("\tRequest token=" + rToken, logger,silent);
    util.printMessage("\tRequest token=" + rToken, pIntf);
  }
  iVec.addElement("StatusCode=" + sCode.toString());
  iVec.addElement("RequestToken=" + rToken);

  if(!submitOnly) {
  if(sCode == TStatusCode.SRM_REQUEST_QUEUED ||
     sCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
     int i = 0;
     long sTimeStamp = util.startTimeStamp();
     while (resultMap.size() > 0) {
       boolean timedOutCase=false;
       iVec.clear(); 
       int tStatusTime = 1;
       if(statusWaitTime >= 1000) {
        tStatusTime = statusWaitTime /1000;
       }
       util.printMessage("\nSRM-CLIENT: Next status call in " +
              tStatusTime + " seconds.", logger,silent);
       util.printMessage("\nSRM-CLIENT: Next status call in " +
              tStatusTime + " seconds.", pIntf);
       Thread.sleep(statusWaitTime);
       if(i >= resultMap.size()) {
         i = 0;
       }
       if(rToken != null) {
          //if(statusMaxTimeAllowed != -1) {
           if(!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
             iVec.addElement("Max retry check status exceeded reservespace status");
             if(_debug) {
               util.printMessage( "SRM-CLIENT: Max retry check " +
			  	"status exceeded reservespace status", logger,silent);
               util.printMessageHException( "SRM-CLIENT: Max retry check " +
			  	"status exceeded reservespace status", pIntf);
             }
             timedOutCase=true;
            }
          //}
          if(!timedOutCase) {
          util.printMessage("SRM-CLIENT: " + new Date() +
			" Calling Reserve space request",logger,silent);
          util.printMessageHL("SRM-CLIENT: " + new Date() +
			" Calling Reserve space request",pIntf);
          SrmStatusOfReserveSpaceRequestRequest spaceStatusRequest =
             new SrmStatusOfReserveSpaceRequestRequest();
          spaceStatusRequest.setRequestToken(rToken);
          if(!uid.equals("")) {
            spaceStatusRequest.setAuthorizationID(uid);
          }
          SrmStatusOfReserveSpaceRequestResponse spaceStatusResponse = null;

          spaceStatusResponse = 
            (SrmStatusOfReserveSpaceRequestResponse) 
             SRMUtilClient.callStaticSoapThread(srm,credential, 
              proxyType, serverUrl,spaceStatusRequest,spaceStatusResponse,
              "srmstatusofreservespace",_theLogger,
              pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
              setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
              retryTimeOut);

          if(spaceStatusResponse.getReturnStatus() != null) {
            rStatus = spaceStatusResponse.getReturnStatus();
            sCode = rStatus.getStatusCode();
            util.printMessage
				("SRM-CLIENT: Status Code for spaceStatusRequest " + 
			   	    sCode.toString(), logger,silent);
            util.printMessage
				("SRM-CLIENT: Status Code for spaceStatusRequest " + 
			   	    sCode.toString(), pIntf);
            iVec.addElement("StatusCode="+sCode.toString());
            util.printEventLog(_theLogger,
				"SrmStatusOfReserveSpaceResponse",iVec,silent,useLog);
            if(sCode != TStatusCode.SRM_REQUEST_INPROGRESS &&
               sCode != TStatusCode.SRM_REQUEST_QUEUED) {
               resultMap.remove(uid);
               fileToken = spaceStatusResponse.getSpaceToken();
               iVec.clear(); 
               iVec.addElement("SpaceToken="+fileToken);
               iVec.addElement("StatusCode="+sCode.toString());
               reportBuf.append(
					"SRM-CLIENT*REQUEST_STATUS="+sCode.toString()+"\n");
               reportBuf.append("SRM-CLIENT*SPACE-TOKEN="+fileToken+"\n"); 
               util.printEventLog(_theLogger,
				"SrmStatusOfReserveSpaceResponse",iVec,silent,useLog);
            }
          }
          else {
            iVec.addElement
				("Null return status from srmStatusOfReserveSpaceRequest");
            util.printEventLog(_theLogger,
					"SrmStatusOfReserveSpaceResponse",iVec,silent,useLog);
            util.printMessage 
				("SRM-CLIENT: Null return status from srmStatusOfReserveSpaceRequest",logger,silent);
            util.printMessageHL 
				("SRM-CLIENT: Null return status from srmStatusOfReserveSpaceRequest",pIntf);
          }
          }
          else {
            util.printEventLog(_theLogger,"SrmReserveSpaceResponse",iVec,silent,useLog);
            resultMap.remove(uid);
          }
       }
       else {
         iVec.addElement("Expecting reqeustToken for this status code");
         util.printEventLog(_theLogger,"SrmReserveSpaceResponse",iVec,silent,useLog);
         util.printMessage(
			"SRM-CLIENT: Expecting requestToken for this status code ", 
				logger,silent);
         util.printMessageHL(
			"SRM-CLIENT: Expecting requestToken for this status code ", pIntf);
         resultMap.remove(uid);
       }
       if(result.getEstimatedProcessingTime() != null) {
         int eTime = result.getEstimatedProcessingTime();
         if(eTime != -1 && (eTime*1000) < statusWaitTime) {
            statusWaitTime = eTime*1000; 
         }
       }
       statusWaitTime = statusWaitTime*2;
       if(statusWaitTime >= threshHold) {
         statusWaitTime = 30;
       }
     }
   }
   else {
     reportBuf.append("SRM-CLIENT*REQUEST_STATUS="+sCode.toString()+"\n");
     iVec.addElement("FileToken="+fileToken);
     iVec.addElement("StatusCode="+sCode.toString());
     //util.printMessage(
		//"\tStatus code from SrmReserveSpaceResponse " + 
			//sCode.toString(), logger,silent);
     fileToken = result.getSpaceToken();
     reportBuf.append("SRM-CLIENT*SPACE-TOKEN="+fileToken+"\n"); 
   }
  }
  else {
     //util.printMessage(
		//"\tStatus code from SrmReserveSpaceResponse " + 
			//sCode.toString(), logger,silent);
     fileToken = result.getSpaceToken();
     reportBuf.append("SRM-CLIENT*SPACE-TOKEN="+fileToken+"\n"); 
  }

   if(rStatus.getExplanation() != null) {
     util.printMessage("\texplanation="+ 
		rStatus.getExplanation(),logger,silent);
     util.printMessage("\texplanation="+ 
		rStatus.getExplanation(),pIntf);
   }
   iVec.addElement("Explanation="+ rStatus.getExplanation());
   reportBuf.append("SRM-CLIENT*EXPLANATION="+rStatus.getExplanation()+"\n"); 

   util.printMessage("\tSpaceToken= "+fileToken,logger,silent);
   util.printMessage("\tSpaceToken= "+fileToken,pIntf);

   if(result.getEstimatedProcessingTime() != null) {
      iVec.addElement("ProceesingTime="+ result.getEstimatedProcessingTime());
      util.printMessage("\tProceesingTime="+
        result.getEstimatedProcessingTime(), logger, silent);
      util.printMessage("\tProceesingTime="+
        result.getEstimatedProcessingTime(), pIntf);
      reportBuf.append("SRM-CLIENT*PROCESSINGTIME="+
			result.getEstimatedProcessingTime()+"\n"); 
   }

   if(result.getSizeOfTotalReservedSpace() != null) {
      iVec.addElement("TotalReservedSpaceSize="+ 
			result.getSizeOfTotalReservedSpace());
      util.printMessage("\tTotalReservedSpaceSize="+
        result.getSizeOfTotalReservedSpace(), logger, silent);
      util.printMessage("\tTotalReservedSpaceSize="+
        result.getSizeOfTotalReservedSpace(), pIntf);
      reportBuf.append("SRM-CLIENT*TOTALRESERVEDSPACESIZE="+
			result.getSizeOfTotalReservedSpace()+"\n"); 
   }

   if(result.getSizeOfGuaranteedReservedSpace() != null) {
      iVec.addElement("Guaranteed Space Size="+
        result.getSizeOfGuaranteedReservedSpace());
      util.printMessage("\tGuaranteed Space Size="+
        result.getSizeOfGuaranteedReservedSpace(), logger, silent);
      util.printMessage("\tGuaranteed Space Size="+
        result.getSizeOfGuaranteedReservedSpace(), pIntf);
      reportBuf.append("SRM-CLIENT*GUARANTEEDSPACESIZE="+
			result.getSizeOfGuaranteedReservedSpace()+"\n"); 
   }

  if(result.getLifetimeOfReservedSpace() != null) {
      iVec.addElement("Lifetime="+
        result.getLifetimeOfReservedSpace());
      util.printMessage("\tLifetime="+
        result.getLifetimeOfReservedSpace(), logger, silent);
      util.printMessage("\tLifetime="+
        result.getLifetimeOfReservedSpace(), pIntf);
      reportBuf.append("SRM-CLIENT*LIFETIME="+
			result.getLifetimeOfReservedSpace()+"\n"); 
  }

  if(result.getRetentionPolicyInfo() != null) {
      TRetentionPolicyInfo retentionPolicyInfo = 
			result.getRetentionPolicyInfo();
      TRetentionPolicy retentionPolicy = 
			retentionPolicyInfo.getRetentionPolicy();
      if(retentionPolicy != null) {
        iVec.addElement("Retention Policy="+retentionPolicy.getValue());
        util.printMessage
	  	  ("\tRetention Policy="+retentionPolicy.getValue(), logger, silent);
        util.printMessage
	  	  ("\tRetention Policy="+retentionPolicy.getValue(), pIntf);
        reportBuf.append("SRM-CLIENT*RETENTIONPOLICY="+
			retentionPolicy.getValue()+"\n"); 
      }
      TAccessLatency accessLatency = retentionPolicyInfo.getAccessLatency();
      if(accessLatency != null) {
        iVec.addElement("Access Latency="+accessLatency.getValue());
        util.printMessage("\tAccess Latency="+
			accessLatency.getValue(), logger, silent);
        util.printMessage("\tAccess Latency="+
			accessLatency.getValue(), pIntf);
        reportBuf.append("SRM-CLIENT*ACCESSLATENCY="+
			accessLatency.getValue()+"\n"); 
      }
  }
  util.printEventLog(_theLogger,"SrmReserveSpaceResponse",iVec,silent,useLog);
  if(pIntf != null) {
    pIntf.setCompleted(true);
  }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   iVec.clear(); 
   iVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmReserveSpace",iVec,silent,useLog);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   util.printMessage("SRM-CLIENT: Exception " + msg, logger,silent);
   util.printMessageHException("SRM-CLIENT: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
      idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf != null) {
        pIntf.setCompleted(false);
      }
      if(pIntf == null) {
        if(idx != -1) {
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
          System.exit(90);
        }
        else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type",logger,silent);
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
          System.exit(96);
        }
        else {
               Vector iinputVec = new Vector ();
               iinputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",iinputVec,silent,useLog);
          System.exit(91);
        }
      }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     throw e;
   }
 }
 
 return fileToken;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmResumeRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmResumeRequest(String requestToken) throws Exception 
{
 TStatusCode sCode = null;
 try {
   inputVec.clear(); 

   //this.requestToken = requestToken;
   util.printMessage("\nSRM-REQUEST: " + new Date () +
		" Calling SrmResumeRequst", logger,silent);
   util.printMessageHL("\nSRM-REQUEST: " + new Date () + 
		" Calling SrmResumeRequst",pIntf);
   if(_debug) {
   util.printMessage("\nSRM-REQUEST: .....Input parametets for SrmResumeRequest...",logger,silent);
   util.printMessage
	("\nSRM-REQUEST: .....Input parametets for SrmResumeRequest...",pIntf);
   util.printMessage("SRM-REQUEST: Resume Request="+requestToken,logger,silent);
   util.printMessage("SRM-REQUEST AuthorizationID="+uid,logger,silent);
   util.printMessage("SRM-REQUEST: Resume Request="+requestToken,pIntf);
   util.printMessage("SRM-REQUEST AuthorizationID="+uid,pIntf);
   }
   inputVec.addElement("RequestToken="+requestToken);
   inputVec.addElement("AuthorizationID="+uid);

   SrmResumeRequestRequest request = new SrmResumeRequestRequest();
   request.setRequestToken(requestToken);
   
   if(!uid.equals("")) {
     request.setAuthorizationID(uid);
   }

   SrmResumeRequestResponse result  = null;

   result = (SrmResumeRequestResponse) 
                callSoapThread(request,result,"srmresumerequest");

   if(_debug) {
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####",logger,silent);
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####",pIntf);
   }

   if(result == null) {
     util.printMessage
		("\nSRM-REQUEST: SRM returned null response for srmResumeRequest", 
			logger,silent);
     util.printMessage
		("\nSRM-REQUEST: SRM returned null response for srmResumeRequest", 
		pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TReturnStatus returnStatus = result.getReturnStatus();
   if(returnStatus == null) {
     inputVec.addElement("SRM returned null returnstatus for srmResumeRequest");
     util.printEventLog(_theLogger,"SrmResumeRequestResponse",inputVec,silent,useLog);
     util.printMessage
	  ("\nSRM-REQUEST: SRM returned null returnstatus for srmResumeRequest", 
		logger,silent);
     util.printMessage
	  ("\nSRM-REQUEST: SRM returned null returnstatus for srmResumeRequest", 
		pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TStatusCode statusCode = returnStatus.getStatusCode();
   sCode = statusCode;
   util.printMessage("\tStatus="+ statusCode.getValue(),logger,silent);
   util.printMessage("\tExplanation="+ returnStatus.getExplanation(),
		logger,silent);
   util.printMessage("\tStatus="+ statusCode.getValue(),pIntf);
   util.printMessage("\tExplanation="+ returnStatus.getExplanation(), pIntf);
   inputVec.addElement("Status="+statusCode.getValue());
   inputVec.addElement("Explanation="+returnStatus.getExplanation());
   util.printEventLog(_theLogger,"SrmResumeRequestResponse",inputVec,silent,useLog);
   if(statusCode.getValue().toString().equals("SRM_SUCCESS")) {
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
   }
 }
 catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
   String msg = e.getMessage();
   int idx = msg.indexOf("Connection refused");
   int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
   inputVec.clear(); 
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"SrmResumeRequest",inputVec,silent,useLog);
   util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
   util.printMessageHException("SRM-REQUEST: Exception " + msg, pIntf);
   util.printHException(e,pIntf);
   if(pIntf != null) {
     pIntf.setCompleted(false);
   }
   if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
     idx != -1 || idx1 !=-1 || idx5 != -1) {
     util.printMessage("\nException : "+msg,logger,silent);
     util.printMessageHException("\nException : "+msg,pIntf);
     if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type", logger,silent);
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(96);
             }
             else {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
     } 
   }
   else {
     throw e;
   }
  }
  return sCode;
} 

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmSuspendRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode doSrmSuspendRequest(String requestToken) throws Exception 
{
 TStatusCode sCode = null;
 try {
   this.requestToken = requestToken;
   util.printMessage("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmSuspendRequest ", logger,silent);
   util.printMessageHL("\nSRM-REQUEST: " + new Date() + 
		" Calling SrmSuspendRequest ", pIntf);
   if(_debug) {
   util.printMessage("\nSRM-REQUEST: ....Input parameters for srmSuspendRequest...", logger,silent);
   util.printMessage("\nSRM-REQUEST: ....Input parameters for srmSuspendRequest...", pIntf);
   util.printMessage("SRM-REQUEST: Suspend Request="+requestToken,
			logger,silent);
   util.printMessage("SRM-REQUEST: AuthorizationID="+uid,
			logger,silent);
   util.printMessage("SRM-REQUEST: Suspend Request="+requestToken, pIntf);
   util.printMessage("SRM-REQUEST: AuthorizationID="+uid, pIntf);
   }
   inputVec.clear(); 
   inputVec.addElement("RequestToken="+requestToken);
   inputVec.addElement("AuthorizationID="+uid);

   SrmSuspendRequestRequest request = new SrmSuspendRequestRequest();
   request.setRequestToken(requestToken);
   
   if(!uid.equals("")) {
     request.setAuthorizationID(uid);
   }

   inputVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"SrmSuspendRequestRequest",inputVec,silent,useLog);

   SrmSuspendRequestResponse result  = null;

   result = (SrmSuspendRequestResponse) 
        callSoapThread(request,result,"srmsuspendrequest");

   if(_debug) {
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####",logger,silent);
   util.printMessage("\nSRM-REQUEST: #### Output from SRM ####",pIntf);
   }
   inputVec.clear(); 

   if(result == null) {
     inputVec.addElement("SRM returned null response for srmResumeRequest");
     util.printEventLog(_theLogger,"SrmSuspendRequestResponse",inputVec,silent,useLog);
     util.printMessage("\nSRM-REQUEST: SRM returned null response " +
			"for srmResumeRequest", logger,silent);
     util.printMessage("\nSRM-REQUEST: SRM returned null response " +
			"for srmResumeRequest", pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TReturnStatus returnStatus = result.getReturnStatus();
   if(returnStatus == null) {
     inputVec.addElement("SRM returned null returnstatus for srmResumeRequest");
     util.printEventLog(_theLogger,"SrmSuspendRequestResponse",inputVec,silent,useLog);
     util.printMessage
		("\nSRM-REQUEST: SRM returned null returnstatus for srmResumeRequest", 
		logger,silent);
     util.printMessage
		("\nSRM-REQUEST: SRM returned null returnstatus for srmResumeRequest", 
	     pIntf);
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
     return sCode;
   }

   TStatusCode statusCode = returnStatus.getStatusCode();
   sCode = statusCode;
   util.printMessage("\tStatus="+ statusCode.getValue(),logger,silent);
   util.printMessage("\tExplanation="+ returnStatus.getExplanation(),
		logger,silent);
   util.printMessage("\tStatus="+ statusCode.getValue(),pIntf);
   util.printMessage("\tExplanation="+ returnStatus.getExplanation(), pIntf);
   inputVec.addElement("Status="+ statusCode.getValue());
   inputVec.addElement("Explanation="+ returnStatus.getExplanation());
   util.printEventLog(_theLogger,"SrmSuspendRequestResponse",inputVec,silent,useLog);
   if(statusCode.getValue().toString().equals("SRM_SUCCESS")) {
     if(pIntf != null) {
       pIntf.setCompleted(true);
     }
   }
   else {
     if(pIntf != null) {
       pIntf.setCompleted(false);
     }
   }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear(); 
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmSuspendRequest",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-REQUEST: Exception " + msg, logger,silent);
    util.printMessageHException("SRM-REQUEST: Exception " + msg, pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type ",logger,silent);
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(96);
             }
             else {
               inputVec.clear(); 
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
      }
    }
    else {
     throw e;
    }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// changeSpace
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode changeSpace (Vector fileInfo) throws Exception {

 TStatusCode sCode = null;
 try {
   util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling changeSpace " , logger,silent);
   util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling changeSpace " , pIntf);
   util.printMessage
		("SRM-CLIENT: ......Input parameters for SrmChangeSpace......",
			logger,silent);
   util.printMessage
		("SRM-CLIENT: ......Input parameters for SrmChangeSpace......",
			pIntf);

   inputVec.clear(); 
   inputVec.addElement("AuthorizationID="+uid);
   inputVec.addElement("SpaceToken="+fileToken);
   
   if(_debug) {
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,logger,silent);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,logger,silent);
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,pIntf);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,pIntf);
   }

   URI[] aURI = new URI[fileInfo.size()];

   for(int i = 0; i < fileInfo.size(); i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String siteUrl = fIntf.getOrigSURL();
     if(_debug) {
     util.printMessage("SRM-CLIENT: SURL="+siteUrl,logger,silent);
     util.printMessage("SRM-CLIENT: SURL="+siteUrl,pIntf);
     }
     inputVec.addElement("SURL="+siteUrl);
     aURI[i] = new URI(siteUrl);

     /*
     if(storageInfo.equals("")) {
       int idx = siteUrl.indexOf("mss://");
       if(idx != -1) {
        storageInfo=""+true;
       }
     }
     */
   }

   if(_debug) {
   util.printMessage("SRM-CLIENT: UseStorageSystemInfo="+storageInfo,
			logger,silent);
   util.printMessage("SRM-CLIENT: UseStorageSystemInfo="+storageInfo,
			pIntf);
   }
   inputVec.addElement("UseStorageSystemInfo="+storageInfo);

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   SrmChangeSpaceForFilesRequest req = new SrmChangeSpaceForFilesRequest ();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   }
   req.setTargetSpaceToken(fileToken);

   req.setArrayOfSURLs(SRMUtilClient.convertToArrayOfAnyURI(aURI));

   ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
   TExtraInfo tExtra = new TExtraInfo();
   Vector vec = new Vector ();

   if(storageInfo.equals("true")) {

     tExtra.setKey("uid");
     tExtra.setValue("changespace");
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
           if(key.equals("source") ||
              key.equals("for") ||
              key.equals("login") ||
              key.equals("passwd") ||
              key.equals("projectid") ||
              key.equals("readpasswd") ||
              key.equals("writepasswd")) {
             tExtra.setKey(key);
             tExtra.setValue(value);
             vec.addElement(tExtra);
           }
           else {
             throw new Exception(
              "Given storage info is not in the correct format " + storageInfo);
           }
         }
         else {
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
   }
 
   TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
   for(int i = 0; i < vec.size(); i++) {
     a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
   }
   storageInfoArray.setExtraInfoArray(a_tExtra);
   req.setStorageSystemInfo(storageInfoArray);

   SrmChangeSpaceForFilesResponse response = null;


   response = (SrmChangeSpaceForFilesResponse) 
        callSoapThread(req,response,"srmchangespace");

   if(_debug) {
   util.printMessage("\nSRM-CLIENT: .............................\n",logger,silent);
   util.printMessage("\nSRM-CLIENT: .............................\n",pIntf);
   }

   inputVec.clear(); 

   if(response == null) {
     inputVec.addElement
		("SRM returned null response for SrmChangeSpaceForFiles request");
     util.printEventLog(_theLogger,"SrmChangeSpaceForFilesResponse",inputVec,silent,useLog);
     util.printMessage
		("\nSRM-CLIENT: SRM returned null response for " + 
			"SrmChangeSpaceForFiles request", logger,silent);
     util.printMessageHException
		("\nSRM-CLIENT: SRM returned null response for " + 
			"SrmChangeSpaceForFiles request", pIntf);
     return sCode;
   }

   TReturnStatus rStatus = response.getReturnStatus();
  
   if(rStatus == null) {
     util.printMessage
		("\nSRM-CLIENT: SRM returned null return status for " +
			"SrmChangeSpaceForFiles request", logger,silent);
     util.printMessageHException
		("\nSRM-CLIENT: SRM returned null return status for " +
			"SrmChangeSpaceForFiles request", pIntf);
	 inputVec.addElement
		("SRM returned null return status for SrmChangeSpaceForFiles request");
     util.printEventLog(_theLogger,"SrmChangeSpaceForFilesResponse",inputVec,silent,useLog);
     return sCode;
   }

   TStatusCode code = rStatus.getStatusCode();
   sCode = code;

   String rToken = response.getRequestToken();
   HashMap resultMap = new HashMap();

   resultMap.put(uid, code);

   inputVec.addElement("RequestToken="+rToken);
   inputVec.addElement("Status="+code);
   inputVec.addElement("Explanation="+rStatus.getExplanation());
   util.printMessage("\tstatus="+code,logger,silent); 
   util.printMessage("\tstatus="+code,pIntf); 
   if(rStatus.getExplanation() != null) {
     util.printMessage("\texplanation="+
		rStatus.getExplanation(),logger,silent); 
     util.printMessage("\texplanation="+
		rStatus.getExplanation(),pIntf); 
   }
   util.printMessage("\tRequest token=" + rToken, logger,silent);
   util.printMessage("\tRequest token=" + rToken, pIntf);
   util.printEventLog(_theLogger,"SrmChangeSpaceResponse",inputVec,silent,useLog);

   if(code == TStatusCode.SRM_REQUEST_QUEUED ||
      code == TStatusCode.SRM_REQUEST_INPROGRESS) {
      int i = 0;
      long sTimeStamp = util.startTimeStamp();
      while (resultMap.size() > 0) {
         inputVec.clear();
         boolean timedOutCase=false;
         int tStatusTime = 1;
         if(statusWaitTime >= 1000) {
           tStatusTime = statusWaitTime/1000;
         }
         util.printMessage("\nSRM-CLIENT: Next status call is in " +
            tStatusTime + " seconds.", logger,silent);
         util.printMessage("\nSRM-CLIENT: Next status call is in " +
            tStatusTime + " seconds.", pIntf);
         Thread.sleep(statusWaitTime);
         if(i >= resultMap.size()) {
           i = 0;
         }
         if(rToken != null) {
            if(statusMaxTimeAllowed != -1) {
             if(!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
			  inputVec.addElement
				("Max retry check status exceeded for change space status");
              util.printMessage
				("SRM-CLIENT: Max retry check status exceeded " + 
					"for change space status", logger,silent);
              util.printMessageHException
				("SRM-CLIENT: Max retry check status exceeded " + 
					"for change space status", pIntf);
              timedOutCase=true;
             }
            }
            if(!timedOutCase) {
            util.printEventLog(_theLogger,"SrmChangeSpaceResponse",inputVec,silent,useLog);
            inputVec.clear();
            util.printMessage("\nSRM-CLIENT: " + new Date() + 
				"Calling StatusOfChangeSpaceForFilesRequest ", logger,silent);
            util.printMessage("\nSRM-CLIENT: " + new Date() + 
				"Calling StatusOfChangeSpaceForFilesRequest ", pIntf);
            SrmStatusOfChangeSpaceForFilesRequestRequest 
					changeSpaceStatusRequest =
             new SrmStatusOfChangeSpaceForFilesRequestRequest();
            changeSpaceStatusRequest.setRequestToken(rToken);
            if(!uid.equals("")) {
              inputVec.addElement("AuthorizationID="+uid);
              changeSpaceStatusRequest.setAuthorizationID(uid);
            } 

            SrmStatusOfChangeSpaceForFilesRequestResponse 
				changeSpaceStatusResponse = null;

            changeSpaceStatusResponse = 
                (SrmStatusOfChangeSpaceForFilesRequestResponse) 
                callSoapThread(changeSpaceStatusRequest,
                        changeSpaceStatusResponse,"srmstatusofchangespace");

            if(_debug) {
            util.printMessage("\nSRM-CLIENT: .......................\n",logger,silent);
            util.printMessage("\nSRM-CLIENT: .......................\n",pIntf);
            }
            if(changeSpaceStatusResponse != null) {  
              if(changeSpaceStatusResponse.getReturnStatus() != null) {
                rStatus = changeSpaceStatusResponse.getReturnStatus();
                code = rStatus.getStatusCode();
                inputVec.addElement("Status="+code.toString());
                inputVec.addElement("Explanation="+rStatus.getExplanation());
                util.printMessage ("\n\tStatus Code for " +
					"StatusOfChangeSpaceForFilesRequest=" + code.toString(), 
						logger,silent);
                util.printMessage ("\n\tStatus Code for " +
					"StatusOfChangeSpaceForFilesRequest=" + code.toString(), 
						pIntf);
                util.printMessage("\texplanation=" + 
						rStatus.getExplanation(), logger,silent); 
                sCode = code;
                if(rStatus.getExplanation() != null) {
                  util.printMessage("\texplanation=" + 
						rStatus.getExplanation(), pIntf); 
                }
                if(code != TStatusCode.SRM_REQUEST_INPROGRESS &&
                   code != TStatusCode.SRM_REQUEST_QUEUED) {
                   resultMap.remove(uid);
                   if(changeSpaceStatusResponse.getEstimatedProcessingTime() != null) {
                     inputVec.addElement("EstimatedProcessingTime="+
						changeSpaceStatusResponse.getEstimatedProcessingTime());
                     util.printMessage("\tEstimatedProcessingTime="+
						changeSpaceStatusResponse.getEstimatedProcessingTime(),
							logger,silent);
                     util.printMessage("\tEstimatedProcessingTime="+
						changeSpaceStatusResponse.getEstimatedProcessingTime(),
							pIntf);
                   }
                   else {
                     util.printMessage("\tEstimatedProcessingTime=null",
						logger,silent);
                     util.printMessage("\tEstimatedProcessingTime=null",pIntf);
                     inputVec.addElement("EstimatedProcessingTime=null");
                   }

                   ArrayOfTSURLReturnStatus aSurlReturnStatus = 
						changeSpaceStatusResponse.getArrayOfFileStatuses();

                   if(aSurlReturnStatus == null) {
				     inputVec.addElement(
						"SRM returned null arrayofFileStatuses " +
							"for SrmStatusOfChangeSpaceFilesRequest");
                     util.printMessage
						("\tSRM-CLIENT: SRM returned null arrayofFileStatuses " +
							"for SrmStatusOfChangeSpaceFilesRequest",
			                  logger,silent);
                     util.printMessageHException
						("\tSRM-CLIENT: SRM returned null arrayofFileStatuses " +
							"for SrmStatusOfChangeSpaceFilesRequest",pIntf);
                     util.printEventLog(_theLogger,
						"SrmStatusOfChangeSpaceFilesRequest", inputVec,silent,useLog);
                     return code;
                   }

                   TSURLReturnStatus[] surlStatuses = 
						aSurlReturnStatus.getStatusArray(); 

                   if(surlStatuses == null) { 
						inputVec.addElement(
							"SRM returned null surlStatuses for " +
							"SrmStatusOfChangeSpaceForFilesRequest");
						util.printMessage
						("\tSRM returned null surlStatuses for " +
							"SrmStatusOfChangeSpaceForFilesRequest",
			                  logger,silent);
						util.printMessageHException
						("\tSRM returned null surlStatuses for " +
							"SrmStatusOfChangeSpaceForFilesRequest",pIntf);
                        util.printEventLog(_theLogger,
							"SrmStatusOfChangeSpaceFilesRequest", inputVec,silent,useLog);
                     return code;
                   }

                   util.printEventLog(_theLogger,
							"SrmStatusOfChangeSpaceFilesRequest", inputVec,silent,useLog);
                   for(int j = 0; j < surlStatuses.length; j++) {  
                     inputVec = new Vector ();
                     inputVec.clear();
                     TSURLReturnStatus surlStatus = surlStatuses[j];
                     util.printMessage("\n\tSURL="+
							surlStatus.getSurl(), logger,silent);
                     inputVec.addElement("SURL="+surlStatus.getSurl());
                     TReturnStatus rrs = surlStatus.getStatus();
                     if(rrs == null) {
                       util.printMessage("\tstatus=null", logger,silent);
                       util.printMessage("\tstatus=null", pIntf);
                       inputVec.addElement("status=null");
                     }
                     else { 
                       inputVec.addElement("status="+rrs.getStatusCode());
                       inputVec.addElement("explanation="+rrs.getExplanation());
                       util.printMessage("\tstatus="+rrs.getStatusCode(),
						  logger,silent);
                       util.printMessage("\tstatus="+rrs.getStatusCode(),pIntf);
                       if(rrs.getExplanation() != null) {
                       util.printMessage("\texplanation="+rrs.getExplanation(),
							logger,silent);
                       util.printMessage("\texplanation="+rrs.getExplanation(),
							pIntf);
                       }
                     }
                     util.printEventLog(_theLogger,
						"SrmStatusOfChangeSpaceResponse",inputVec,silent,useLog);
                   }
                }
              }
              else {
                inputVec.addElement("Null return status from " +
					"srmStatusOfChangeSpaceForFilesRequest");
                util.printMessage("\nSRM-CLIENT: Null return status from " +
					"srmStatusOfChangeSpaceForFilesRequest", logger,silent);
                util.printMessageHException
					("\nSRM-CLIENT: Null return status from " +
					"srmStatusOfChangeSpaceForFilesRequest", pIntf);
                util.printEventLog(_theLogger,"SrmStatusOfChangeSpaceResponse",inputVec,silent,useLog);
              }
            }
            else {
                inputVec.addElement
					("Null response from SrmStatusOfChangeSpaceForFilesRequest");
                util.printEventLog(_theLogger,"SrmStatusOfChangeSpaceResponse",inputVec,silent,useLog);
                util.printMessage
					("\nSRM-CLIENT: Null response from srmStatusOfChangeSpaceForFilesRequest", logger,silent);
                util.printMessageHException
					("\nSRM-CLIENT: Null response from srmStatusOfChangeSpaceForFilesRequest", pIntf);
            }
            }
            else {
              util.printEventLog(_theLogger,"SrmChangeSpace",inputVec,silent,useLog);
              resultMap.remove(uid);
            }
         }
         else {
           inputVec.addElement("Expecting requestToken for this " +
				"status code for ChangeSpaceForFiles from SRM");
           util.printEventLog(_theLogger,"SrmChangeSpace",inputVec,silent,useLog);
           util.printMessage ("\nSRM-CLIENT: Expecting requestToken for this " +
				"status code for ChangeSpaceForFiles from SRM",	logger,silent);
           util.printMessage("\nSRM-CLIENT: Expecting requestToken for this " +
				"status code for ChangeSpaceForFiles from SRM",	pIntf);
           resultMap.remove(uid);
         }
         if(response.getEstimatedProcessingTime() != null) {
            int eTime = response.getEstimatedProcessingTime();
            if(eTime != -1 && (eTime*1000) < statusWaitTime) {
               statusWaitTime = eTime*1000;
            }
         } 
         statusWaitTime = statusWaitTime*2;
         if(statusWaitTime >= threshHold) {
           statusWaitTime = 30;
         }
       }
    }
    else {
       inputVec.clear();
       if(response.getEstimatedProcessingTime() != null) {
          inputVec.addElement("EstimatedProcessingTime="+
			response.getEstimatedProcessingTime());
          util.printMessage("\tEstimatedProcessingTime="+
			response.getEstimatedProcessingTime(),logger,silent);
          util.printMessage("\tEstimatedProcessingTime="+
			response.getEstimatedProcessingTime(),pIntf);
       }
       else {
          util.printMessage("\tEstimatedProcessingTime=null",logger,silent);
          util.printMessage("\tEstimatedProcessingTime=null",pIntf);
          inputVec.addElement("EstimatedProcessingTime=null");
       }

       ArrayOfTSURLReturnStatus aSurlReturnStatus = response.getArrayOfFileStatuses();

       if(aSurlReturnStatus == null) {
          util.printMessage("\tSRM returned null arrayofFileStatuses " +
				"for SrmChangeOfSpaceFiles request", logger,silent);
          util.printMessageHException
			("\tSRM returned null arrayofFileStatuses " +
				"for SrmChangeOfSpaceFiles request", pIntf);
          inputVec.addElement("SRM returned null arrayofFileStatuses " +
				"for SrmChangeOfSpaceFiles request");
          util.printEventLog(_theLogger,"SrmChangeOfSpaceFiles",inputVec,silent,useLog);
          if(pIntf != null) {
            pIntf.setCompleted(true);
          }
          return sCode;
       }

       TSURLReturnStatus[] surlStatuses = aSurlReturnStatus.getStatusArray(); 

       if(surlStatuses == null) {
          util.printMessage("\tSRM returned null surlStatuses for " +
				"SrmChangeOfSpaceFiles request", logger,silent);
          util.printMessageHException("\tSRM returned null surlStatuses for " +
				"SrmChangeOfSpaceFiles request", pIntf);
          inputVec.addElement("SRM returned null surlStatuses for " +
				"SrmChangeOfSpaceFiles request");
          util.printEventLog(_theLogger,"SrmChangeOfSpaceFiles",inputVec,silent,useLog);
          if(pIntf != null) {
            pIntf.setCompleted(true);
          }
          return sCode;
       }

       util.printEventLog(_theLogger,"SrmChangeOfSpaceFiles",inputVec,silent,useLog);
       for(int i = 0; i < surlStatuses.length; i++) {  
          inputVec.clear();
          TSURLReturnStatus surlStatus = surlStatuses[i];
          util.printMessage("\n\tSURL="+surlStatus.getSurl(), 
					logger,silent);
          util.printMessage("\n\tSURL="+surlStatus.getSurl(), pIntf);
          inputVec.addElement("SURL="+surlStatus.getSurl());
          TReturnStatus rrs = surlStatus.getStatus();
          if(rrs == null) {
             util.printMessage("\tstatus=null",logger,silent);
             util.printMessage("\tstatus=null",pIntf);
             inputVec.addElement("status=null");
          }
          else { 
             inputVec.addElement("status="+rrs.getStatusCode());
             inputVec.addElement("explanation="+rrs.getExplanation());
             util.printMessage("\tstatus="+
					rrs.getStatusCode(),logger,silent);
             util.printMessage("\tstatus="+
					rrs.getStatusCode(),pIntf);
             if(rrs.getExplanation() != null) {
             util.printMessage("\texplanation="+
					rrs.getExplanation(),logger,silent);
             util.printMessage("\texplanation="+
					rrs.getExplanation(),pIntf);
             }
          }
          util.printEventLog(_theLogger,"SrmChangeOfSpaceFiles",inputVec,silent,useLog);
       }
    }
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    inputVec.clear();
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmChangeOfSpaceFiles",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Exception " + e.getMessage(),logger,silent);
    util.printMessageHException("SRM-CLIENT: Exception " + e.getMessage(),pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
                   " please check your proxy type ",logger,silent);
               inputVec.clear();
               inputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(96);
             }
             else {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// purgeFromSpace
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode purgeFromSpace (Vector fileInfo) throws Exception {

 TStatusCode sCode = null;
 try {
   util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmPurgeFromSpace ", logger,silent); 
   util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling SrmPurgeFromSpace ", pIntf); 
   if(_debug) {
      util.printMessage
		("SRM-CLIENT: .......Input parameters for SrmPurgeFromSpace ......",
			logger,silent);
      util.printMessage
		("SRM-CLIENT: .......Input parameters for SrmPurgeFromSpace ......",
			pIntf);
   }

   inputVec.clear();
   inputVec.addElement("SpaceToken="+fileToken);
   inputVec.addElement("AuthrizationID="+uid);
   if(_debug) {
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,logger,silent);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,logger,silent);
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,pIntf);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,pIntf);
   }

   URI[] aURI = new URI[fileInfo.size()];

   for(int i = 0; i < fileInfo.size(); i++) {
     FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
     String siteUrl = fIntf.getOrigSURL();
     if(_debug) {
     util.printMessage("SRM-CLIENT: SURL="+siteUrl,logger,silent);
     util.printMessage("SRM-CLIENT: SURL="+siteUrl,pIntf);
     }
     inputVec.addElement("SURL="+siteUrl);
     aURI[i] = new URI(siteUrl);

     /*
     if(storageInfo.equals("")) {
       int idx = siteUrl.indexOf("mss://");
       if(idx != -1) {
         storageInfo=""+true;
       }
     }
     */
   }

   if(_debug) {
   util.printMessage("SRM-CLIENT: UseStorageSystemInfo="+storageInfo,
			logger,silent);
   util.printMessage("SRM-CLIENT: UseStorageSystemInfo="+storageInfo,pIntf);
   }
   inputVec.addElement("UseStorageSystemInfo="+storageInfo);

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   SrmPurgeFromSpaceRequest req = new SrmPurgeFromSpaceRequest ();
   if(!uid.equals("")) {
     req.setAuthorizationID(uid);
   }
   req.setSpaceToken(fileToken);

   req.setArrayOfSURLs(SRMUtilClient.convertToArrayOfAnyURI(aURI));

   ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
   TExtraInfo tExtra = new TExtraInfo();
   Vector vec = new Vector ();
   if(storageInfo.equals("true")) {

     tExtra.setKey("uid");
     tExtra.setValue("purgespace");
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
           throw new Exception(
            "Given storage info is not in the correct format " + storageInfo);
         }
      }
   }
 
   TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
   for(int i = 0; i < vec.size(); i++) {
     a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
   }
   storageInfoArray.setExtraInfoArray(a_tExtra);
   req.setStorageSystemInfo(storageInfoArray);

   SrmPurgeFromSpaceResponse response = null;

   response = (SrmPurgeFromSpaceResponse) 
                callSoapThread(req,response,"srmpurgespace");
  
   if(_debug) {
   util.printMessage("\nSRM-CLIENT: .............................\n",logger,silent);
   util.printMessage("\nSRM-CLIENT: .............................\n",pIntf);
   }

   if(response == null) {
     inputVec.addElement("SRM returned null response for SrmPurgeFromSpace");
     util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent,useLog);
     util.printMessage
	   ("\nSRM-CLIENT: SRM returned null response for SrmPurgeFromSpace", 
				logger, silent);
     util.printMessageHException
	   ("\nSRM-CLIENT: SRM returned null response for SrmPurgeFromSpace", 
				pIntf);
     return sCode;
   }

   TReturnStatus rStatus = response.getReturnStatus();

   if(rStatus == null) {
      util.printMessage("\nSRM-CLIENT: SRM returned null return status", 
			logger,silent);
      util.printMessageHException
			("\nSRM-CLIENT: SRM returned null return status", pIntf);
      inputVec.addElement("SRM returned null return status");
      util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent,useLog);
      return sCode;
   }

   sCode = rStatus.getStatusCode();
   util.printMessage("\n\tstatus="+rStatus.getStatusCode(),logger,silent);
   util.printMessage("\texplanation="+rStatus.getExplanation(),logger,silent);
   util.printMessage("\n\tstatus="+rStatus.getStatusCode(),pIntf);
   util.printMessage("\texplanation="+rStatus.getExplanation(),pIntf);

   inputVec.addElement("status="+rStatus.getStatusCode());
   inputVec.addElement("explanation="+rStatus.getExplanation());

   if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
           if(pIntf != null) {
              pIntf.setCompleted(true);
           }
      return sCode;
   }

   ArrayOfTSURLReturnStatus aSurlStatus = response.getArrayOfFileStatuses();

   if(aSurlStatus ==  null) {
     //util.printMessage
		//("\nSRM-CLIENT: SRM returned null getArrayOfFileStatuses " +
			//"for SrmPurgeFromSpace request", logger,silent);
     //util.printMessageHException
		//("\nSRM-CLIENT: SRM returned null getArrayOfFileStatuses " +
			//"for SrmPurgeFromSpace request", pIntf);
     inputVec.addElement("SRM returned null getArrayOfFileStatuses " +
			"for SrmPurgeFromSpace request");
     util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent,useLog);
     return sCode;
   }

   TSURLReturnStatus[] surlStatuses =  aSurlStatus.getStatusArray();

   if(surlStatuses == null) {
     //util.printMessage("\nSRM-CLIENT: SRM returned null getStatusArray " +
		//"from SrmPurgeFromSpace request", logger,silent);
     //util.printMessage("\nSRM-CLIENT: SRM returned null getStatusArray " +
		//"from SrmPurgeFromSpace request", pIntf);
     inputVec.addElement("SRM returned null getStatusArray " +
		"from SrmPurgeFromSpace request");
     util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent,useLog);
     return sCode;
   }

   //util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent);

   for(int i = 0; i < surlStatuses.length; i++) {
       inputVec.clear();
       TSURLReturnStatus surlStatus = surlStatuses[i];
       util.printMessage("\n\tsurl="+surlStatus.getSurl(),logger,silent);
       util.printMessage("\n\tsurl="+surlStatus.getSurl(),pIntf);
       inputVec.addElement("SURL="+surlStatus.getSurl());
       TReturnStatus surlReturnStatus = surlStatus.getStatus();
       if(surlReturnStatus == null) {
         util.printMessage("\tstatus=null",logger,silent);
         util.printMessage("\tstatus=null",pIntf);
         inputVec.addElement("status=null");
       }
       else { 
         inputVec.addElement("status="+surlReturnStatus.getStatusCode());
         inputVec.addElement("explanation="+surlReturnStatus.getExplanation());
         util.printEventLog(_theLogger,"SrmPurgeFromSpaceResponse",inputVec,silent,useLog);
         util.printMessage("\tstatus="+surlReturnStatus.getStatusCode(),
			logger,silent);
         util.printMessage("\texplanation="+surlReturnStatus.getExplanation(),
				logger,silent);
         util.printMessage("\tstatus="+surlReturnStatus.getStatusCode(),pIntf);
         util.printMessage("\texplanation="+surlReturnStatus.getExplanation(),
				pIntf);
       }
   }
   if(pIntf != null) {
     pIntf.setCompleted(true);
   }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    inputVec.clear();
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmPurgeFromSpace",inputVec,silent,useLog);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    util.printMessage("SRM-CLIENT: Exception " + msg, logger,silent);
    util.printMessageHException("SRM-CLIENT: Exception " + msg, pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type ", logger,silent);
               inputVec.clear();
               inputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(96);
             }
             else {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode updateToken()  throws Exception 
{ 
 TStatusCode sCode = null;
 try {
   inputVec.clear();

   util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling updateSpace request", logger,silent); 
   util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling updateSpace request", pIntf); 
   if(_debug) {
     util.printMessage("\nSRM-CLIENT: Input parameters :::::::::::::",
		logger,silent);
     util.printMessageHL("\nSRM-CLIENT: Input parameters :::::::::::::", pIntf);
   }

   inputVec.addElement("AuthorizationID="+uid);
   inputVec.addElement("SpaceToken="+fileToken);
   inputVec.addElement("NewSizeOfTotalSpaceDesired="+tokenSize);
   inputVec.addElement("Guaranteed Size= "+gTokenSize);
   inputVec.addElement("LifeTime="+tokenLifetime);

   if(_debug) {
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,logger,silent);
   util.printMessage("\nSRM-CLIENT: AuthorizationID="+uid,pIntf);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,logger,silent);
   util.printMessage("SRM-CLIENT: Space Token="+fileToken,pIntf);
   util.printMessage("SRM-CLIENT: NewSizeOfTotalSpaceDesired="+tokenSize,logger,silent);
   util.printMessage("SRM-CLIENT: NewSizeOfTotalSpaceDesired="+tokenSize,pIntf);
   util.printMessage("SRM-CLIENT: Guaranteed Size= "+gTokenSize,logger,silent);
   util.printMessage("SRM-CLIENT: Guaranteed Size= "+gTokenSize,pIntf);
   util.printMessage("SRM-CLIENT: LifeTime="+tokenLifetime,logger,silent);
   util.printMessage("SRM-CLIENT: LifeTime="+tokenLifetime,pIntf);
   }

   if(tokenLifetime <= 0) {
      util.printMessage(
			"\nSRM-CLIENT: Lifetime cannt be <= 0",logger,silent);
      util.printMessageHException(
			"\nSRM-CLIENT: Lifetime cannt be <= 0",pIntf);
      inputVec.addElement("Lifetime cannt be <= 0");
      util.printEventLog(_theLogger,"UpdateToken",inputVec,silent,useLog);
      return sCode;
   }
			
   SrmUpdateSpaceRequest request = new SrmUpdateSpaceRequest();
   request.setSpaceToken(fileToken);  
   if(!uid.equals("")) {
     request.setAuthorizationID(uid);
   }

   if(tokenSize != 0) {
     long g = tokenSize;
     request.setNewSizeOfTotalSpaceDesired(new UnsignedLong(g));
   }
			
   if(gTokenSize != 0) {
     long g = gTokenSize;
     request.setNewSizeOfGuaranteedSpaceDesired(new UnsignedLong(g));
   }

   request.setNewLifeTime(new Integer(tokenLifetime));
   //setting storage system info (how to do that)

    inputVec.addElement("\nSending srmUpdateSpace request ...");
    inputVec.addElement("TimeStamp="+new Date());
    util.printEventLog(_theLogger,"SrmUpdateSpaceRequest", inputVec,silent,useLog);

    SrmUpdateSpaceResponse result = null;

    result = (SrmUpdateSpaceResponse) 
        callSoapThread(request,result,"srmupdatespace");

    inputVec.clear();

    if(_debug) {
      util.printMessage("\nSRM-CLIENT: #### Output from SRM ####\n",
			logger,silent);
      util.printMessageHL("\nSRM-CLIENT: #### Output from SRM ####\n",pIntf);
			
    }
			
    if(result == null) {
      inputVec.addElement("SRM returned null result");
      util.printMessage("\nSRM-CLIENT: SRM returned null result", logger,silent);
      util.printMessageHException
		("\nSRM-CLIENT: SRM returned null result", pIntf);
      util.printEventLog(_theLogger,"SrmUpdateSpaceResponse", inputVec,silent,useLog);
      return sCode;
    }

    TReturnStatus rStatus = result.getReturnStatus();
    if(rStatus == null) {
      util.printMessage("\nSRM-CLIENT: SRM returned null return status", 
			logger,silent);
      util.printMessageHException
		("\nSRM-CLIENT: SRM returned null return status", pIntf);
      inputVec.addElement("SRM returned null return status");
      util.printEventLog(_theLogger,"SrmUpdateSpaceResponse", inputVec,silent,useLog);
      return sCode;
    }

    TStatusCode code = rStatus.getStatusCode();

    HashMap resultMap = new HashMap();
    resultMap.put(uid, code);
    String rToken = result.getRequestToken();
    util.printMessage("\tstatus="+code,logger,silent); 
    util.printMessage("\tstatus="+code,pIntf); 
    if(rStatus.getExplanation() != null) {
      util.printMessage("\texplanation="+
		rStatus.getExplanation(),logger,silent); 
      util.printMessage("\texplanation="+ rStatus.getExplanation(),pIntf); 
    }
    util.printMessage("\tRequest token=" + rToken, logger,silent);
    util.printMessage("\tRequest token=" + rToken, pIntf);

    inputVec.addElement("status="+code);
    inputVec.addElement("explanation="+rStatus.getExplanation());
    inputVec.addElement("Request token=" + rToken);

    util.printEventLog(_theLogger,"SrmUpdateSpaceResponse", inputVec,silent,useLog);

    sCode = code;
    inputVec.clear();
    if(code == TStatusCode.SRM_REQUEST_QUEUED ||
       code == TStatusCode.SRM_REQUEST_INPROGRESS) {
        int i = 0;
        long sTimeStamp = util.startTimeStamp();
        while (resultMap.size() > 0) {
          inputVec.clear();
          boolean timedOutCase = false;
          int tStatusTime = 1;
          if(statusWaitTime >= 1000) {
             tStatusTime = statusWaitTime/1000;
          }
          util.printMessage("\nSRM-CLIENT: Next status call is in " +
              tStatusTime + " seconds.", logger,silent);
          util.printMessage("\nSRM-CLIENT: Next status call is in " +
              tStatusTime + " seconds.", pIntf);
          Thread.sleep(statusWaitTime);
          //if(statusMaxTimeAllowed != -1) {
            if(!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
              inputVec.addElement(
				"Max retry check status exceeded for update space");
              if(_debug) {
              util.printMessage(
				"Max retry check status exceeded for update space", 
					logger,silent);
              util.printMessageHException(
				"Max retry check status exceeded for update space", pIntf);
              }
              timedOutCase=true;
            }
          //}
          if(i >= resultMap.size()) {
            i = 0;
          }
          util.printEventLog(_theLogger,"SrmUpdateSpaceResponse", inputVec,silent,useLog);
          if(rToken != null) {
           if(!timedOutCase) {
            inputVec.clear();
            inputVec.addElement("RequestToken="+rToken);
            inputVec.addElement("AuthorizationID="+uid);

            SrmStatusOfUpdateSpaceRequestRequest updateStatusRequest =
             new SrmStatusOfUpdateSpaceRequestRequest();
            updateStatusRequest.setRequestToken(rToken);
            if(!uid.equals("")) {
               updateStatusRequest.setAuthorizationID(uid);
            }

            SrmStatusOfUpdateSpaceRequestResponse updateStatusResponse =null;
                   
            updateStatusResponse =
                (SrmStatusOfUpdateSpaceRequestResponse) 
                    callSoapThread(updateStatusRequest,
                        updateStatusResponse,"srmstatusofupdatespace");


            inputVec.addElement("TimeStamp="+new Date());
            util.printEventLog(_theLogger,
				"SrmStatusOfUpdateSpaceRequest", inputVec,silent,useLog);

            inputVec.clear();
            util.printMessage("\nSRM-CLIENT: .......................\n",
					logger,silent);
            util.printMessage("\nSRM-CLIENT: .......................\n",pIntf);
            if(updateStatusResponse != null) {  
              if(updateStatusResponse.getReturnStatus() != null) {
                rStatus = updateStatusResponse.getReturnStatus();
                code = rStatus.getStatusCode();
                inputVec.addElement("Status="+code.toString());
                inputVec.addElement("explanation=" + rStatus.getExplanation());
                util.printMessage("\nStatus Code for udpateStatusRequest=" + 
					code.toString(), logger,silent);
                util.printMessage("\nStatus Code for udpateStatusRequest=" + 
					code.toString(), pIntf);
                if(rStatus.getExplanation() != null) {
                  util.printMessage("\texplanation=" + rStatus.getExplanation(),
					logger,silent); 
                  util.printMessage("\texplanation=" + rStatus.getExplanation(),
					pIntf); 
                }
                if(code != TStatusCode.SRM_REQUEST_INPROGRESS &&
                   code != TStatusCode.SRM_REQUEST_QUEUED) {
                   resultMap.remove(uid);
                   if (updateStatusResponse.getLifetimeGranted() != null) {
                     inputVec.addElement("lifetime="+
	                   updateStatusResponse.getLifetimeGranted());
                     util.printMessage("\tlifetime="+
	                   updateStatusResponse.getLifetimeGranted(),logger,silent);
                     util.printMessage("\tlifetime="+
	                   updateStatusResponse.getLifetimeGranted(),pIntf);
	               }
                   if (updateStatusResponse.getSizeOfGuaranteedSpace() != null) {
	                 inputVec.addElement("Min="+
	                   updateStatusResponse.getSizeOfGuaranteedSpace().toString());
	                 util.printMessage("\tMin="+
	                   updateStatusResponse.getSizeOfGuaranteedSpace().toString(),logger,silent);
	                 util.printMessage("\tMin="+
	                   updateStatusResponse.getSizeOfGuaranteedSpace().toString(),pIntf);
	               }
	               if (updateStatusResponse.getSizeOfTotalSpace() != null) {
	                 inputVec.addElement("Max="+
	                   updateStatusResponse.getSizeOfTotalSpace().toString());
	                 util.printMessage("\tMax="+
	                   updateStatusResponse.getSizeOfTotalSpace().toString(),logger,silent);
	                 util.printMessage("\tMax="+
	                   updateStatusResponse.getSizeOfTotalSpace().toString(),
							pIntf);
	               }
                   util.printEventLog(_theLogger,"SrmStatusOfUpdateRequestResponse",
						inputVec,silent,useLog);
                }
              }
              else {
                inputVec.addElement("Null return status from srmStatusOfUpdateRequest");
                util.printEventLog(_theLogger,"SrmStatusOfUpdateRequestResponse",
						inputVec,silent,useLog);
                util.printMessage("\nSRM-CLIENT: Null return status from srmStatusOfUpdateRequest", logger,silent);
                util.printMessageHException("\nSRM-CLIENT: Null return status from srmStatusOfUpdateRequest", pIntf);
              }
            }
            else {
                inputVec.addElement("Null response from srmStatusOfUpdateRequest");
                util.printEventLog(_theLogger,"SrmStatusOfUpdateRequestResponse", inputVec,silent,useLog);
                util.printMessage("\nSRM-CLIENT: Null response from srmStatusOfUpdateRequest", logger,silent);
                util.printMessageHException("\nSRM-CLIENT: Null response from srmStatusOfUpdateRequest", pIntf);
            }
            }
            else {
              resultMap.remove(uid);
            }
          }
          else {
            inputVec.addElement("Expecting requestToken for this status code " +
				"for updateRequest from SRM");
            util.printMessage("\nSRM-CLIENT: Expecting requestToken for this status code " + "for updateRequest from SRM",	logger,silent);
            util.printMessageHException("\nSRM-CLIENT: Expecting requestToken for this status code " + "for updateRequest from SRM",	pIntf);
            util.printEventLog(_theLogger,"SrmStatusOfUpdateRequestResponse", inputVec,silent,useLog);
            resultMap.remove(uid);
          }
          statusWaitTime = statusWaitTime*2;
          if(statusWaitTime >= threshHold) {
           statusWaitTime = 30;
          }
        }
    }
    else {
      if (result.getLifetimeGranted() != null) {
         inputVec.addElement("lifetime="+result.getLifetimeGranted());
         util.printMessage("\tlifetime="+
	      result.getLifetimeGranted(),logger,silent);
         util.printMessage("\tlifetime="+
	      result.getLifetimeGranted(),pIntf);
	  }
      if (result.getSizeOfGuaranteedSpace() != null) {
	     inputVec.addElement("Min="+
	      result.getSizeOfGuaranteedSpace().toString());
	     util.printMessage("\tMin="+
	      result.getSizeOfGuaranteedSpace().toString(),logger,silent);
	     util.printMessage("\tMin="+
	      result.getSizeOfGuaranteedSpace().toString(),pIntf);
	  }
	  if (result.getSizeOfTotalSpace() != null) {
	     inputVec.addElement("Max="+
	      result.getSizeOfTotalSpace().toString());
	     util.printMessage("\tMax="+
	      result.getSizeOfTotalSpace().toString(),logger,silent);
	     util.printMessage("\tMax="+
	      result.getSizeOfTotalSpace().toString(),pIntf);
	  }
      util.printEventLog(_theLogger,"SrmStatusOfUpdateRequestResponse", inputVec,silent,useLog);
    }
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }
  catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
    String msg = e.getMessage();
    int idx = msg.indexOf("Connection refused");
    int idx1 = msg.indexOf("Defective credential detected");
   int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
    inputVec.clear();
    inputVec.addElement("Exception="+e.getMessage());
    util.printEventLog(_theLogger,"SrmUpdateRequest", inputVec,silent,useLog);
    util.printMessage("\nSRM-CLIENT: Exception : "+msg,logger,silent);
    util.printMessageHException("\nSRM-CLIENT: Exception : "+msg,pIntf);
    util.printHException(e,pIntf);
    if(pIntf != null) {
      pIntf.setCompleted(false);
    }
    if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1) {
      util.printMessage("\nException : "+msg,logger,silent);
      util.printMessageHException("\nException : "+msg,pIntf);
      if(pIntf == null) {
             if(idx != -1) {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+90);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(90);
             }
             else if(idx1 != -1 || idx5 != -1) {
               util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type", logger,silent);
               inputVec.clear();
               inputVec.addElement("ExitStatus="+96);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(96);
             }
             else {
               inputVec.clear();
               inputVec.addElement("ExitStatus="+91);
               util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
               System.exit(91);
             }
      }
    }
    else {
      throw e;
    }
  }
  return sCode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSecurityPropertiesForcefully
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSecurityPropertiesForcefully(URL endpoint, 
        ISRM _srm) throws Exception {
  if (endpoint.getProtocol().equals("httpg") ||
        _srm instanceof org.apache.axis.client.Stub) {
        if(_debug) {
          util.printMessage("\nSRM-CLIENT: ProxyType found " + proxyType,
            logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("ProxyType="+proxyType);
        util.printEventLog(_theLogger,"SetStaticSecurityPropertiesForceFully",
                inputVec,silent,useLog);
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

public void setSecurityProperties(URL endpoint, ISRM _srm) {
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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setStaticSecurityPropertiesForcefully
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void setStaticSecurityPropertiesForcefully(URL endpoint, 
        ISRM srm, GSSCredential credential, int proxyType,
        java.util.logging.Logger _theLogger, PrintIntf pIntf, 
        boolean silent, boolean useLog,
        int setHTTPConnectionTimeOutAllowed, String delegationNeeded) 
{

  Vector inputVec = new Vector ();
  if (endpoint.getProtocol().equals("httpg") ||
        srm instanceof org.apache.axis.client.Stub) {
        if(_debug) {
          util.printMessage("\nSRM-CLIENT: ProxyType found " + proxyType,
            logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("ProxyType="+proxyType);
        util.printEventLog(_theLogger,"SetStaticSecurityPropertiesForceFully",
                inputVec,silent,useLog);
        org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) srm;
        srm_stub._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,credential);
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
// seStatictSecurityProperties
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void setStaticSecurityProperties(URL endpoint, ISRM srm,
        GSSCredential credential, int proxyType,
        java.util.logging.Logger _theLogger,
        PrintIntf pIntf, boolean silent,
        boolean useLog, int setHTTPConnectionTimeOutAllowed,
        String delegationNeeded) {

  Vector inputVec = new Vector ();
  if (endpoint.getProtocol().equals("httpg") ||
        srm instanceof org.apache.axis.client.Stub) {
        if(_debug) {
          util.printMessage("\nSRM-CLIENT: ProxyType found " + proxyType,
            logger,silent);
        }
        inputVec.clear();
        inputVec.addElement("ProxyType="+proxyType);
        util.printEventLog(_theLogger,"SetStaticSecurityProperties",
                inputVec,silent,useLog);
        org.apache.axis.client.Stub srm_stub =
             (org.apache.axis.client.Stub)srm;
        //GSIUtils.setDelegationGSIProperties(_stub, endpoint);
        srm_stub._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,credential);
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
// printXMLReport
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printXMLReport(TReturnStatus rStatus, TMetaDataPathDetail[] pDetail,
	boolean getRecursive, String surl, String turl, String outputFile,
	boolean esgReportType) throws Exception {
  if(rStatus == null) return;

  DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
  DocumentBuilder parser = factory.newDocumentBuilder ();
  org.w3c.dom.Document doc = parser.newDocument();
  org.w3c.dom.Element reportElement = doc.createElement("report");
  Attr fileName = null;

  if(!outputFile.equals("")) {
     fileName = doc.createAttribute("filename");
     fileName.setValue(outputFile);
     reportElement.setAttributeNode(fileName);
     Attr serviceUrl = null;
     if(serverUrl != null) {
       serviceUrl = doc.createAttribute("service_endpoint");
       serviceUrl.setValue(serverUrl.replace("httpg","srm"));
       reportElement.setAttributeNode(serviceUrl);
     }
     Attr requestStatus = null;
     if(rStatus.getStatusCode() != null) {
       requestStatus = doc.createAttribute("request-status");
       requestStatus.setValue(rStatus.getStatusCode().toString());
       reportElement.setAttributeNode(requestStatus);
     }
     Attr requestExplanation = null;
     if(rStatus.getExplanation() != null) {
       requestExplanation = doc.createAttribute("request-explanation");
       requestExplanation.setValue(rStatus.getExplanation());
       reportElement.setAttributeNode(requestExplanation);
     }
  } 

  if(pDetail != null) {
    for(int i = 0; i < pDetail.length; i++) {
      printXMLReportForPathDetail(doc,reportElement,"\t",pDetail[i],getRecursive, 
			surl,turl,esgReportType);
    }
  }

  doc.appendChild(reportElement);
  //default encoding is UTF-8
  OutputFormat format = new OutputFormat (doc);
  format.setIndenting(true);
  XMLSerializer serializer = new XMLSerializer(format);
  if(!outputFile.equals("")) {
    PrintWriter pw = new PrintWriter (new FileOutputStream(outputFile));
    serializer.setOutputCharStream(pw);
    serializer.setOutputFormat(format);
    serializer.serialize(doc);
    pw.close();
    inputVec.addElement("SRM-DIR: XML report saved in ="+outputFile);
    util.printEventLog(_theLogger,"PrintResults",inputVec,silent,useLog);
    util.printMessage("SRM-DIR: XML report saved in " + 
			outputFile,logger,silent);
  }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printResults
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printResults (Vector result, String outputFile, Vector resultVec,
	boolean copyCase, String targetDir, String sourceUrl) throws Exception {

   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance ();
   DocumentBuilder parser = factory.newDocumentBuilder ();
   org.w3c.dom.Document doc = parser.newDocument();
   org.w3c.dom.Element root = doc.createElement("list");
   if(result.size() == 0) { 
     if(!copyCase) {
       //printGsiFTPListResultForEmptyDirectory(doc,root,targetDir,sourceUrl);
     }
     return;
   }
   else {
     printGsiFTPListResults(doc,root,
		result,resultVec,copyCase,targetDir,sourceUrl);
   }
   doc.appendChild(root);
   //default encoding is UTF-8
   OutputFormat format = new OutputFormat (doc);
   format.setIndenting(true);
   XMLSerializer serializer = new XMLSerializer(format);
   if(!copyCase) {
     if(!outputFile.equals("")) {
       PrintWriter pw = new PrintWriter (new FileOutputStream(outputFile));
       serializer.setOutputCharStream(pw);
       serializer.setOutputFormat(format);
       serializer.serialize(doc);
       pw.close();
       inputVec.addElement("SRM-DIR: XML report saved in ="+outputFile);
       util.printEventLog(_theLogger,"PrintResults",inputVec,silent,useLog);
       util.printMessage("SRM-DIR: XML report saved in" + 
			outputFile,logger,silent);
     } else { 
        StringWriter sw = new StringWriter();
        serializer.setOutputCharStream(sw);
        serializer.setOutputFormat(format);
        serializer.serialize(doc);
        util.printMessage(sw.toString(), logger,silent);
     }
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//printXMLReportForPathDetail
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printXMLReportForPathDetail(org.w3c.dom.Document doc,
	    org.w3c.dom.Element root, 
		String prefix, TMetaDataPathDetail pDetail,
        boolean getRecursive, String surl, String turl,boolean esgReportType) {

  if(getRecursive) {
    if(pDetail.getArrayOfSubPaths() != null) {
       TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
       for(int i = 0; i<subPDetail.length;i++) {
         printXMLReportForPathDetail(doc,root,prefix+"\t",
				subPDetail[i],getRecursive, surl,turl,esgReportType);
       }
    }
  } 
  else {
   org.w3c.dom.Element fileElement = doc.createElement("file");
   if(esgReportType) {

     if(pDetail.getType() != null) {
       Attr fileType = doc.createAttribute("filetype");
       if(pDetail.getType().toString().trim().equals("DIRECTORY")) { 
         fileElement = doc.createElement("directory");
       } 
       //fileType.setValue(pDetail.getType().toString());
       //fileElement.setAttributeNode(fileType);
     }
     if(pDetail.getPath() != null) {
       Attr ssurl = doc.createAttribute("path");
       int idx = pDetail.getPath().indexOf("?SFN=");
       if(idx != -1) {
         ssurl.setValue(pDetail.getPath().substring(idx+5));
       }
       else {
         ssurl.setValue(pDetail.getPath());
       } 
       fileElement.setAttributeNode(ssurl);
     }
     if(pDetail.getSize() != null) {
       Attr bytes = doc.createAttribute("size");
       bytes.setValue(""+pDetail.getSize());
       fileElement.setAttributeNode(bytes);
     }
   }
   else {

     if(pDetail.getType() != null) {
       Attr fileType = doc.createAttribute("filetype");
       if(pDetail.getType().toString().trim().equals("DIRECTORY")) { 
         fileElement = doc.createElement("directory");
       }
       //fileType.setValue(pDetail.getType().toString());
       //fileElement.setAttributeNode(fileType);
     }

     if(pDetail.getPath() != null) {
       Attr ssurl = doc.createAttribute("path");
       int idx = pDetail.getPath().indexOf("?SFN=");
       if(idx != -1) {
         ssurl.setValue(pDetail.getPath().substring(idx+5));
       }
       else {
         ssurl.setValue(pDetail.getPath());
       } 
       fileElement.setAttributeNode(ssurl);
     }

     if(pDetail.getSize() != null) {
       Attr bytes = doc.createAttribute("size");
       bytes.setValue(""+pDetail.getSize());
       fileElement.setAttributeNode(bytes);
     }


     if(pDetail.getStatus() != null && pDetail.getStatus().getStatusCode() != null) {
       Attr fileStatus = doc.createAttribute("status");
       fileStatus.setValue(pDetail.getStatus().getStatusCode().toString());
       fileElement.setAttributeNode(fileStatus);
     }

     if(pDetail.getStatus().getExplanation() != null) {
       Attr fileExplanation = doc.createAttribute("explanation");
       fileExplanation.setValue(pDetail.getStatus().getExplanation());
       fileElement.setAttributeNode(fileExplanation);
     }

     if(pDetail.getFileStorageType() != null) {
       Attr storageType = doc.createAttribute("storagetype");
       storageType.setValue(pDetail.getFileStorageType().toString());
       fileElement.setAttributeNode(storageType);
     }

     if(pDetail.getOwnerPermission() != null && 
		pDetail.getOwnerPermission().getUserID() != null) {
       Attr ownerPermission = doc.createAttribute("owner-permission");
       ownerPermission.setValue(pDetail.getOwnerPermission().getUserID());
       fileElement.setAttributeNode(ownerPermission);
     }

    if(pDetail.getLifetimeLeft() != null) {
       Attr lifeTimeLeft = doc.createAttribute("lifetimeleft");
       lifeTimeLeft.setValue(""+pDetail.getLifetimeLeft().intValue());
       fileElement.setAttributeNode(lifeTimeLeft);
    }

    if(pDetail.getLifetimeAssigned() != null) {
       Attr lifeTimeAssigned = doc.createAttribute("lifetimeassigned");
       lifeTimeAssigned.setValue(""+pDetail.getLifetimeAssigned().intValue());
       fileElement.setAttributeNode(lifeTimeAssigned);
    }

    if(pDetail.getCheckSumType() != null) {
       Attr checkSumType = doc.createAttribute("checksumtype");
       checkSumType.setValue(pDetail.getCheckSumType());
       fileElement.setAttributeNode(checkSumType);
    }

    if(pDetail.getCheckSumValue() != null) {
       Attr checkSumValue = doc.createAttribute("checksumvalue");
       checkSumValue.setValue(pDetail.getCheckSumValue());
       fileElement.setAttributeNode(checkSumValue);
    }

    if(pDetail.getFileLocality() != null) {
       Attr fileLocality = doc.createAttribute("filelocality");
       fileLocality.setValue(pDetail.getFileLocality().toString());
       fileElement.setAttributeNode(fileLocality);
    }

    if(pDetail.getOwnerPermission() != null) {
       TUserPermission uPermission = pDetail.getOwnerPermission();
       Attr ouserid = doc.createAttribute("owner-userid");
       ouserid.setValue(uPermission.getUserID());
       fileElement.setAttributeNode(ouserid);
       Attr omode = doc.createAttribute("owner-mode");
       omode.setValue(uPermission.getMode().getValue());
       fileElement.setAttributeNode(omode);
    }

    if(pDetail.getGroupPermission() != null) {
      TGroupPermission gPermission = pDetail.getGroupPermission();
      Attr guserid = doc.createAttribute("gowner-groupid");
      guserid.setValue(gPermission.getGroupID());
      fileElement.setAttributeNode(guserid);
      Attr gmode = doc.createAttribute("gowner-mode");
      gmode.setValue(gPermission.getMode().getValue());
      fileElement.setAttributeNode(gmode);
    }

    if(pDetail.getOtherPermission() != null) {
      Attr otherpermission = doc.createAttribute("other-permission");
      otherpermission.setValue(pDetail.getOtherPermission().getValue());
      fileElement.setAttributeNode(otherpermission);
    }

    if(pDetail.getArrayOfSpaceTokens() != null) {
      ArrayOfString spaceTokens = pDetail.getArrayOfSpaceTokens();
      if(spaceTokens != null) {
      String[] tokens = spaceTokens.getStringArray(); 
      if(tokens != null) {
      for(int k = 0; k < tokens.length; k++) {
         Attr spacetokens = doc.createAttribute("spacetokens-("+k+")");
         spacetokens.setValue(tokens[k]);
         fileElement.setAttributeNode(spacetokens);
      }
      }
      }
    }

    if(pDetail.getRetentionPolicyInfo() != null) {
      TRetentionPolicyInfo retentionPolicyInfo =	
			pDetail.getRetentionPolicyInfo();
      if(retentionPolicyInfo != null) {
      TRetentionPolicy retentionPolicy = 			
			retentionPolicyInfo.getRetentionPolicy();
      TAccessLatency accessLatency = retentionPolicyInfo.getAccessLatency();
      if(retentionPolicy != null) {
         Attr retentionpolicy = doc.createAttribute("retentionpolicy");
         retentionpolicy.setValue(retentionPolicy.toString());
         fileElement.setAttributeNode(retentionpolicy);
      }
      if(accessLatency != null) {
         Attr accesslatency = doc.createAttribute("accesslatency");
         accesslatency.setValue(accessLatency.toString());
         fileElement.setAttributeNode(accesslatency);
      }
      }
    } 

    if (pDetail.getLastModificationTime() != null) {
      Calendar gcal = pDetail.getLastModificationTime();
      Date dd = gcal.getTime();
      int year = dd.getYear()+1900; 
      int month = dd.getMonth(); 
      int day = dd.getDate(); 
      int hour = dd.getHours(); 
      int minute = dd.getMinutes(); 
      int second = dd.getSeconds(); 
      Attr lastaccessed = doc.createAttribute("lastaccessed");
      lastaccessed.setValue(dd.toString());
      fileElement.setAttributeNode(lastaccessed);
    }

    if (pDetail.getCreatedAtTime() != null) {
      Calendar gcal = pDetail.getCreatedAtTime();
      Date dd = gcal.getTime();
      int year = dd.getYear()+1900; 
      int month = dd.getMonth(); 
      int day = dd.getDate(); 
      int hour = dd.getHours(); 
      int minute = dd.getMinutes(); 
      int second = dd.getSeconds(); 
      Attr createdattime = doc.createAttribute("createdattime");
      createdattime.setValue(dd.toString());
      fileElement.setAttributeNode(createdattime);
    }
   }//end else 
   root.appendChild(fileElement);

   if(pDetail.getArrayOfSubPaths() != null) {
     TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
     if(subPDetail != null) {
     for(int i = 0; i<subPDetail.length;i++) {
        printXMLReportForPathDetail(doc,root,
	  prefix+"\t",subPDetail[i],getRecursive, surl,turl,esgReportType);
     }
     }
   }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printGsiFTPListResults
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printGsiFTPListResults(org.w3c.dom.Document doc, 
   org.w3c.dom.Element root, Vector result, Vector resultVec, 
   boolean copyCase, String targetDir,String sourceUrl) {

  Object[] obj = result.toArray();
  for(int i = 0; i < obj.length; i++) {
    if(obj[i] instanceof RemoteFileInfo) {
      RemoteFileInfo rInfo = (RemoteFileInfo) obj[i];
      org.w3c.dom.Element fileElement = doc.createElement("file");
      Attr path = doc.createAttribute("path");
      //path.setValue(rInfo.getSURL());
      path.setValue(rInfo.getSFN());
      FileInfo fffInfo = new FileInfo();
      fffInfo.setSURL(rInfo.getSURL());
      resultVec.addElement(fffInfo);
      fileElement.setAttributeNode(path);
      if(rInfo.getErrorMessage() != null && !rInfo.getErrorMessage().equals("")) {
        Attr errorMessage = doc.createAttribute("message");
        errorMessage.setValue(rInfo.getErrorMessage());
        fileElement.setAttributeNode(errorMessage);
      }
      else {
       Attr size = doc.createAttribute("size");
       size.setValue(rInfo.getSize());
       fileElement.setAttributeNode(size);
       Attr timestamp = doc.createAttribute("timestamp");
       String s2 = "";
       if(rInfo.getDate() != null) {
          s2 = s2+rInfo.getDate();
       }
       if(rInfo.getTime() != null) {
          s2 = s2+ " " + rInfo.getTime();
       }
       timestamp.setValue(s2);
       if(s2 != null && !s2.trim().equals("")) {
         fileElement.setAttributeNode(timestamp);
       }
      }
      root.appendChild(fileElement);
    }
    else if(obj[i] instanceof RemoteHostInfo || obj[i] instanceof LocalListInfo) {  
      Vector remoteListings = new Vector ();
      if(obj[i] instanceof RemoteHostInfo) {
        RemoteHostInfo rInfo = (RemoteHostInfo) obj[i];
        remoteListings = rInfo.getRemoteListings();       
        org.w3c.dom.Element fileElement = doc.createElement("dir");
        Attr path = doc.createAttribute("path");
        //path.setValue(rInfo.getSURL());
        path.setValue(rInfo.getSFN());
        //FileInfo fffInfo = new FileInfo();
        //fffInfo.setSURL(rInfo.getSURL());
        //resultVec.addElement(fffInfo);
        if(copyCase) {
          if(sourceUrl.startsWith("gsiftp:") && targetDir.startsWith("gsiftp:")) { 
				//copy case
            int idx = rInfo.getSURL().indexOf(sourceUrl);
            if(idx != -1) {
              String temp = rInfo.getSURL().substring(idx+sourceUrl.length());
              int idx2 = temp.indexOf("/",0);
              if(idx2 != -1) {
                try {
                  String ppath = temp.substring(idx2+1);
                  RemoteHostInfo rrInfo = new RemoteHostInfo (targetDir, _credential, 
		            logger, _theLogger, false,false,copyCase,false); 
                  int val = rrInfo.doMkdir(ppath);
                  if(val == 0) {
                     System.out.println("SRM-CLIENT: Destination directory created " + 
						targetDir+"/"+ppath);
                  }
                  else {
                     if(val == 1) { 
                       System.out.println
							("SRM-CLIENT: Destination directory could not be created " + 
						           targetDir+"/"+ppath);
                     }
                     else if(val == 2) { 
                       System.out.println
							("SRM-CLIENT: Destination directory already exists " + 
						           targetDir+"/"+ppath);
                     }
                  }
                 }catch(Exception ee) {
                   util.printEventLogException(_theLogger,"",ee);
                   System.out.println("\nSRM-CLIENT: Exception="+ee.getMessage());
                 }
              }//end if(idx2 != -1)
           }//end if(idx != -1)
          }
          else if(sourceUrl.startsWith("gsiftp:")) { //get case
            int idx = rInfo.getSURL().indexOf(sourceUrl);
            if(idx != -1) {
              String temp = rInfo.getSURL().substring(idx+sourceUrl.length());
              int idx2 = temp.indexOf("/",0);
              if(idx2 != -1) {
                  File f = new File(targetDir+"/"+temp.substring(idx2+1));
                  try {
                    if(!f.exists()) {
                     f.mkdir();
                     System.out.println("SRM-CLIENT: Destination directory created " + 
						f.getCanonicalPath());
                   }
                   else {
                      if(f.isDirectory()) {
                        //System.out.println("SRM-CLIENT: Directory already exists " + 
							//f.getName());
                      }
                      else {
                        System.out.println("SRM-CLIENT: Destination directory is a file" + 
							f.getCanonicalPath());
                      }
                   }
                  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
                    System.out.println("Exception="+e.getMessage());
                  }
              }
           }
         }//end get case
        }//end copycase
 
        fileElement.setAttributeNode(path);
        //Attr size = doc.createAttribute("size");
        //size.setValue(rInfo.getSize());
        //fileElement.setAttributeNode(size);
        Attr timestamp = doc.createAttribute("timestamp");
        String s2 = "";
        if(rInfo.getDate() != null) {
          s2 = s2+rInfo.getDate();
        }
        if(rInfo.getTime() != null) {
          s2 = s2+ " " + rInfo.getTime();
        }
        timestamp.setValue(s2);
        if(s2 != null && !s2.trim().equals("")) {
          fileElement.setAttributeNode(timestamp);
        }
        root.appendChild(fileElement);
        printGsiFTPListResults(doc,fileElement,remoteListings,resultVec,
			copyCase,targetDir,sourceUrl);
      }
      else if(obj[i] instanceof LocalListInfo) {
        LocalListInfo rInfo = (LocalListInfo) obj[i];
        remoteListings = rInfo.getLocalListings();       
        org.w3c.dom.Element fileElement = doc.createElement("dir");
        Attr path = doc.createAttribute("path");
        //path.setValue(rInfo.getSURL());
        path.setValue(rInfo.getSFN());
        if(sourceUrl.startsWith("file") || targetDir.startsWith("file:")) { 
				//local copy
          rInfo.setSURL("file:///"+rInfo.getSURL());
          int idx = rInfo.getSURL().indexOf(sourceUrl);
          if(idx != -1) {
            String temp = rInfo.getSURL().substring(idx+sourceUrl.length());
            int idx2 = temp.indexOf("/",0);
            if(idx2 != -1) {
              try {
                String ppath = temp.substring(idx2+1);
                String ttargetDir = 
					SRMUtilClient.parseLocalSourceFileForPath (targetDir);
                File f = new File(ttargetDir+"/"+ppath);
                if(f.exists()) {
                   System.out.println(
					  "SRM-CLIENT: Destination directory not created " + 
					     ttargetDir+"/"+ppath);
                }
                else {
                   f.mkdir();
                   System.out.println(
					  "SRM-CLIENT: Destination directory created " + 
					     ttargetDir+"/"+ppath);
                }
              }catch(Exception ee) {
                    util.printEventLogException(_theLogger,"",ee);
                 System.out.println("\nSRM-CLIENT: Exception="+ee.getMessage());
              }
            }
          }//end if
         }//end local copy case
         else if(sourceUrl.startsWith("/") || sourceUrl.startsWith("file:")) { //put case
            int idx = rInfo.getSURL().indexOf(sourceUrl);
            if(idx != -1) {
              String temp = rInfo.getSURL().substring(idx+sourceUrl.length());
              int idx2 = temp.indexOf("/",0);
              if(idx2 != -1) {
                try {
                  String ppath = temp.substring(idx2+1);
                  RemoteHostInfo rrInfo = new RemoteHostInfo (targetDir, _credential, 
		            logger, _theLogger, false,false,copyCase,false); 
                  int val = rrInfo.doMkdir(ppath);
                  if(val == 0) {
                     System.out.println("SRM-CLIENT: Destination directory created " + 
						targetDir+"/"+ppath);
                  }
                  else {
                     if(val == 1) {
                       System.out.println(
							"SRM-CLIENT: Destination directory could not be created " + 
						      targetDir+"/"+ppath);
                     }
                     else if (val == 2) {
                       System.out.println(
							"SRM-CLIENT: Destination directory already exists " + 
						       targetDir+"/"+ppath);
                     }
                  }
                }catch(Exception ee) {
                    util.printEventLogException(_theLogger,"",ee);
                  System.out.println("\nSRM-CLIENT: Exception="+ee.getMessage());
                }
              }
            }//end if
         }//end put case
        fileElement.setAttributeNode(path);
        //Attr size = doc.createAttribute("size");
        //size.setValue(rInfo.getSize());
        //fileElement.setAttributeNode(size);
        Attr timestamp = doc.createAttribute("timestamp");
        //timestamp.setValue(rInfo.getDate()+" " + rInfo.getTime());
        timestamp.setValue(rInfo.getDate());
        if(rInfo.getDate() != null && !rInfo.getDate().equals("")) {
          fileElement.setAttributeNode(timestamp);
        }
        root.appendChild(fileElement);
        printGsiFTPListResults(doc,fileElement,remoteListings,resultVec,
			copyCase,targetDir,sourceUrl);
      }
    }
  }
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmPutDone
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TStatusCode  doSrmPutDone(Vector fileInfo, String rid) throws Exception {

  TStatusCode sCode = null;
  if(_debug) {
    util.printMessage("SRM-COPY: ::::::::::::: Input parameters for srmPutDone :::::", logger,silent);
    util.printMessage("SRM-COPY: ...Input parameters for srmPutDone...",
        pIntf);
    util.printMessage("SRM-COPY: token="+rid,logger,silent);
    util.printMessage("SRM-COPY: uid="+uid,logger,silent);
    util.printMessage("SRM-COPY: token="+rid,pIntf);
    util.printMessage("SRM-COPY: uid="+uid,pIntf);
  }

  org.apache.axis.types.URI[] uriArray = 
		new org.apache.axis.types.URI [fileInfo.size()];

  for(int i = 0; i < fileInfo.size(); i++) {
    FileInfo fInfo = (FileInfo)fileInfo.elementAt(i);
    org.apache.axis.types.URI uri = new org.apache.axis.types.URI ();
    uriArray[i] = new URI(fInfo.getSURL());
    if(_debug) {
      util.printMessage("SRM-COPY: surl="+fInfo.getSURL(),logger,silent);
      util.printMessage("SRM-COPY: surl="+fInfo.getSURL(),pIntf);
    }
  }

  SrmPutDoneRequest req = new SrmPutDoneRequest();
  req.setArrayOfSURLs(SRMUtilClient.convertToArrayOfAnyURI(uriArray));
  req.setRequestToken(rid);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
  }

  inputVec.clear();
  try {

    SrmPutDoneResponse result = null;

    result = (SrmPutDoneResponse) callSoapThread(req,result,"srmputdone");

    if(result == null) {
      inputVec.addElement("SRM returned null result for the srmPutDone request");
      util.printEventLog(_theLogger,"Response from srmPutDone", inputVec,silent,useLog);
      util.printMessage("\nSRM-COPY: " + new Date() +
            " SRM returned null result for the srmPutDone request", logger,silent);
      util.printMessage("\nSRM-COPY: " + new Date() +
            " SRM returned null result for the srmPutDone request", pIntf);
    }

    if(result.getArrayOfFileStatuses() == null) {
       inputVec.clear();
       util.printMessage("\nSRM-COPY: " + new Date() +
            " SRM returned null getArrayOfFileStatus for srmPutDone request",
          logger,silent);
       util.printMessage("\nSRM-COPY: " + new Date() +
            " SRM returned null getArrayOfFileStatus for srmPutDone request",
          pIntf);
       inputVec.addElement("SRM returned null getArrayOfFileStatus for srmPutDone request");
       util.printEventLog(_theLogger,"Response from srmPutDone", inputVec,silent,useLog);
    }

    TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
    TStatusCode temp = status.getStatus().getStatusCode();
    sCode = temp;
    inputVec.clear();
    util.printMessage(".............................",logger,silent);
    if(_debug) {
      util.printMessage("SRM-COPY: " + "Result of Put Done ", logger,silent);
      util.printMessage("SRM-COPY: " + "Result of Put Done ", pIntf);
    }
    inputVec.addElement("SURL="+status.getSurl().toString());
    inputVec.addElement("status="+temp);
    inputVec.addElement("explanation="+status.getStatus().getExplanation());
    util.printEventLog(_theLogger,"SrmPutDoneResponse",inputVec,silent,useLog);
    util.printMessage("\tsurl="+status.getSurl().toString(),logger,silent);
    util.printMessage("\tstatus="+temp,logger,silent);
    util.printMessage("\texplanation="+status.getStatus().getExplanation(),logger,silent);
    util.printMessage("\tsurl="+status.getSurl().toString(),pIntf);
    util.printMessage("\tstatus="+temp,pIntf);
    util.printMessage("\texplanation="+status.getStatus().getExplanation(),
        pIntf);
    //***********************
       //this one is hanging for some reason, need to check thsi later
    //if(temp != TStatusCode.SRM_REQUEST_INPROGRESS || temp != TStatusCode.SRM_REQUEST_QUEUED
       //|| temp != TStatusCode.SRM_SUCCESS || temp != TStatusCode.SRM_DONE) {
      //_srmClientIntf.srmFileFailure(label, temp.toString()+", " +
            //status.getStatus().getExplanation());
    //}
    //***********************

     }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     util.printMessage("SRM-COPY: " + e.getMessage(),logger,silent);
     util.printMessageHException("SRM-COPY: " + e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//mapReturnStatusVaueBackToCode
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static TStatusCode mapReturnStatusValueBackToCode(boolean debug,
		StringBuffer rCode, Log logger, 
                java.util.logging.Logger _theLogger) {

    if(debug) {
      util.printMessage(".....mapReturnStatus " + rCode.toString(),
			logger,silent);
    }
    try {
      return TStatusCode.fromValue(rCode.toString());
    }catch(IllegalStateException ise) {
      util.printEventLogException(_theLogger,"",ise);
      return null;
    }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// converToArrayOfAnyURI
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static ArrayOfAnyURI convertToArrayOfAnyURI
		(org.apache.axis.types.URI[] uris) {
  ArrayOfAnyURI aURI = new ArrayOfAnyURI();
  aURI.setUrlArray(uris);
  return aURI;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// formArrayOfAnyURI
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static ArrayOfAnyURI formArrayOfAnyURI(URI surl) {
    org.apache.axis.types.URI[] urlArray = new org.apache.axis.types.URI[1];
    urlArray[0] = surl;
    return SRMUtilClient.convertToArrayOfAnyURI(urlArray);
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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseLocalSourceFileForPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String parseLocalSourceFileForPath (String str)
  throws Exception {

  if(str.startsWith("file:////")) {
    return str.substring(8);
  }
  else if(str.startsWith("file:///")) {
    return str.substring(7);
  }
  else if(str.startsWith("file://")) {
    return str.substring(6);
  }
  else if(str.startsWith("file:/")) {
    return str.substring(5);
  }
  else if(str.startsWith("/")) {
    return str;
  }
  else {
    throw new Exception("SRMUtilClient:parseLocalSourceFileForPath:SURL not in correct format " + str);
  }
}

}
