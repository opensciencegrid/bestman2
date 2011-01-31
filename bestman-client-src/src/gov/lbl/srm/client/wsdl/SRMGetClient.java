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
import java.net.*;
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

import org.globus.gsi.gssapi.auth.*;

import org.apache.axis.types.*;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.utils.Options;
import org.apache.axis.SimpleTargetedChain;
import org.globus.axis.transport.*;
import org.apache.axis.transport.http.HTTPSender;
import org.globus.axis.util.Util;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMGetClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMGetClient implements SRMWSDLIntf {
  private Stub _stub;
  private ISRM _srm;
  //private ServiceContext _context; 
  private SRMClientIntf _srmClientIntf; 
  private GSSCredential _credential;
  private boolean _debug;
  private Log logger;
  private PrintIntf pIntf;
  private String fileToken;
  private String userDesc="";
  private String uid="";
  private static boolean firstTimeStatus=true;
  //private char tokenType;
  private int tokenLifetime;
  private long tokenSize;
  private long guarnSize;
  private boolean doReserveSpace;
  private Vector fileInfo=new Vector();
  private boolean overwrite;
  private boolean silent=false;
  private boolean useLog=false;
  private int partytype;
  private String statusToken="";
  private String protocolsList="";
  private boolean partycopy;
  private int type = 0;
  private int totalRequestTime;
  private int pinLifeTime;
  private int tFiles = 0;
  private boolean bringonline = false;
  private TFileStorageType fileStorageType;
  private TRetentionPolicyInfo retentionPolicyInfo;
  private Vector srmCopyClientRefArray = new Vector();
  private int parallelism;
  private int bufferSize;
  private boolean dcau=true;
  private boolean directGsiFTP;
  private boolean submitOnly=false;
  private static int statusMaxTimeAllowed=10800; //in seconds
  //private static int statusMaxTimeAllowed=-1; //in seconds
  private String storageInfo="";
  private String remoteTransferInfo="";
  private String protocolList="";
  //private static int statusWaitTime=30;
  private static int statusWaitTime=10; //changed for exponentional polling
  private static int threshHold = 600;
  private boolean statusWaitTimeGiven=false;
  private java.util.logging.Logger _theLogger;
  private Vector inputVec = new Vector();
  private Date requestDate;
  private int proxyType;
  private TReturnStatus rStatus = null;
  private  String resultStatus="";
  private  String resultExplanation="";
  private TimeOutCallBack timeOutCallBack = null;
  private SoapCallThread soapCallThread = null;
  private int connectionTimeOutAllowed=1800;
  private int setHTTPConnectionTimeOutAllowed=600;
  private boolean sourceHasGsiFTP=false;
  private String delegationNeeded="";
  private String requestToken="";
  private String serverUrl="";
  private boolean noAbortFile;
  private boolean domkdir;
  private boolean recursive;
  private Vector vecFiles = new Vector ();
  private boolean firstTime=true;
  private int pinnedFiles;
  private int failedFiles;
  private HashMap indexMap = new HashMap();
  private boolean waitToInitiateSRMPutCallUntilAllFilesDone=false;
  private Vector oneTimePutCallVec = new Vector();
  private boolean oneTimePutCall=false;
  private int numRetry;
  private int retryTimeOut;
  private boolean gateWayModeEnabled;
  private boolean checkPing;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMGetClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMGetClient (SRMClientIntf srmClientIntf, 
	String serverUrl, String uid, Vector fileInfo, String  fileToken,
    String statusToken, boolean doReserveSpace, 
      boolean partycopy, boolean overwrite,
      int tokenlifetime, long tokensize, long guarnSize,
      GSSCredential credential,
      TFileStorageType fileStorageType, 
	  TRetentionPolicyInfo retentionPolicyInfo,
	  int totalRequestTime, int pinLifeTime,
      int tFiles, boolean bringonline,
      int parallelism, int bufferSize, boolean dcau,
      boolean directGsiFTP, 
      int sMaxTimeAllowed, int statusWaitTime,
      String protocolsList,
      boolean statusWaitTimeGiven, String storageInfo,
      String remoteTransferInfo,
      String userDesc, java.util.logging.Logger theLogger, Log logger, 
 	  PrintIntf pIntf, boolean debug,
      boolean silent,boolean useLog, boolean submitOnly,int proxyType,
	  int connectionTimeOutAllowed, int setHTTPConnectionTimeOutAllowed,
	  String delegationNeeded, 
	  boolean noAbortFile, boolean domkdir, 
	  boolean recursive, int threshHold,int numRetry, 
          int retryTimeOut, boolean checkPing) 
                throws Exception 
{
	//_context = new ServiceContext ();
    _srmClientIntf = srmClientIntf;
    _credential = credential;
    this._theLogger = theLogger;
    this.logger = logger;
    this.pIntf = pIntf;
    this.partycopy = partycopy;
    this.overwrite = overwrite;
    this.proxyType = proxyType;
    this.numRetry = numRetry;
    this.retryTimeOut = retryTimeOut;
    this.delegationNeeded = delegationNeeded;
    this.checkPing = checkPing;
    _debug = debug;
    this.silent = silent;
    this.useLog = useLog;
    this.threshHold = threshHold;
    this.fileStorageType = fileStorageType;
    this.retentionPolicyInfo = retentionPolicyInfo;
    this.bringonline = bringonline;
    this.tFiles = tFiles;
    this.parallelism = parallelism;
    this.protocolsList = protocolsList;
    this.storageInfo = storageInfo;
    this.remoteTransferInfo = remoteTransferInfo;
    this.bufferSize = bufferSize;
    this.dcau = dcau; 
    this.directGsiFTP = directGsiFTP;
    this.submitOnly = submitOnly;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.setHTTPConnectionTimeOutAllowed = setHTTPConnectionTimeOutAllowed;
    this.noAbortFile = noAbortFile;
    this.domkdir = domkdir;
    this.recursive = recursive;

    if(sMaxTimeAllowed != 10800) {
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
    this.statusWaitTimeGiven = statusWaitTimeGiven;
    this.userDesc = userDesc;

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


    if(!directGsiFTP) {

      String[] args = new String[2];

      args[0] = serverUrl;
      args[1] = uid;
      this.serverUrl = serverUrl;

      if(!uid.equals("")) {
        util.printMessage("SRM-CLIENT: AuthId=" + uid,logger,silent);
      }
      inputVec.addElement("ServerUrl="+serverUrl);
      inputVec.addElement("AuthId="+uid);
      util.printEventLog(_theLogger,"SrmGet constructor", 
			inputVec,silent,useLog);
      openSRMConnection();
  }

  if(checkPing) {
    gateWayModeEnabled= SRMUtilClient.isServerGateWayEnabled(_srm, uid,
                        logger,  _debug, credential,
                        proxyType, serverUrl,  _theLogger,
                        pIntf,  _srmClientIntf, silent,
                        useLog, connectionTimeOutAllowed,
                        setHTTPConnectionTimeOutAllowed,
                        delegationNeeded, numRetry, retryTimeOut);

     inputVec.clear();
     inputVec.addElement("GateWayModeEnabled="+gateWayModeEnabled);
     util.printEventLog(_theLogger,"SrmGet.Constructor", 
			inputVec,silent,useLog);
     System.out.println("SRM-CLIENT: GateWayModeEnabled="+gateWayModeEnabled);
    _srmClientIntf.setGateWayModeEnabled(gateWayModeEnabled);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// openSRMConnection
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void openSRMConnection () throws Exception {

      SimpleProvider provider = new SimpleProvider();
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
          System.out.print("does not work,");
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
        inputVec.clear();
        inputVec.addElement("srm is null");
        inputVec.addElement("remote srm object is null");
        util.printEventLog(_theLogger,"SrmGet constructor", 
				inputVec,silent,useLog);
        throw new Exception("\nSRM-CLIENT: remote srm object is null");
     }
     else {
       inputVec.clear();
       inputVec.addElement("got remote srm object");
       util.printEventLog(_theLogger,"SrmGet constructor",
            inputVec,silent,useLog);
     }

     _srm = srm;
      org.apache.axis.client.Stub srm_stub = 
			(org.apache.axis.client.Stub) srm;
      srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000); 
      checkSourceContainsGsiFTP(fileInfo);
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

private void checkSourceContainsGsiFTP(Vector fileInfo) {
   int size = fileInfo.size();
   for(int i = 0; i < size; i++) {
      FileInfo ffInfo = (FileInfo)fileInfo.elementAt(i);
      if (ffInfo.getSURL().startsWith("gsiftp")) {
          sourceHasGsiFTP = true;
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
    inputVec.clear();
    inputVec.addElement
     ("SetRequestDone with true and false");
    util.printEventLog(_theLogger,"PerformTransfer.get",inputVec,silent,useLog);
    _srmClientIntf.setRequestInformation("SRM_FAILURE","");
    _srmClientIntf.setRequestDone(true,false);
    return;
  }
  else {
   try {
     srmGet(uid, fileInfo);
   } catch(Exception e) {
       String msg = e.getMessage();
       int idx = msg.indexOf("Connection refused");
       int idx1 = msg.indexOf("Defective credential detected");
       int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
       int idx6 = msg.indexOf(
			"java.net.SocketTimeoutException: Read timed out");
       inputVec.clear();
       inputVec.addElement("Exception="+e.getMessage());
       util.printHException(e,pIntf);
       util.printEventLog(_theLogger,"PerformTransfer",inputVec,silent,useLog);
       util.printEventLogException(_theLogger,"PerformTranfer",e);
       if(pIntf != null) {
        pIntf.setCompleted(false);
       }
       if(msg.startsWith("CGSI-gSOAP: Could not find mapping") 
			|| idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
         util.printMessage("\nSRM-CLIENT Exception : "+msg,logger,silent);
         util.printMessageHException("\nSRM-CLIENT Exception : "+msg,pIntf);
         if(pIntf == null) {
           if(idx != -1 || idx6 != -1) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+90);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
             util.printHException(e,pIntf);
             System.exit(90);    
           }
           else if(idx1 != -1 || idx5 != -1) {
             util.printMessage("Exception : proxy type mismatch " +
					" please check your proxy type ", logger,silent);
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
        "Calling SrmStatusOfGetRequest", logger, silent);
  util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
        "Calling SrmStatusOfGetRequest", pIntf);
  util.printMessage("TimeStamp="+new Date(), logger,silent);
  inputVec.clear();
  util.printEventLog(_theLogger,"SrmStatusOfGetRequest",inputVec,silent,useLog);
  int exitCode = doStatus(uid, fileInfo, statusToken);
  if(pIntf == null) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+exitCode);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
    System.exit(exitCode);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseSpace() throws Exception {
  if(gateWayModeEnabled) return ;

  if(fileToken != null) {
    inputVec.clear();
    util.printEventLog(_theLogger,"ReleaseSpace",inputVec,silent,useLog);
    util.printMessage("\nSRM-CLIENT: " + new Date() + " Calling releaseSpace " + fileToken, logger, silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + " Calling releaseSpace " + fileToken, pIntf);

    //util.printMessage("TimeStamp="+new Date(), logger,silent);
    releaseSpace(fileToken,true);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(String siteUrl, String rid,int label) 
        throws Exception {

  inputVec.clear();
  inputVec.addElement("SURL="+siteUrl);
  inputVec.addElement("token="+rid);
  inputVec.addElement("label="+label);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmGetClient.releaseFile",inputVec,silent,useLog);

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  String code = null;
  if(rid != null && !rid.equals("")) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + " Calling releaseFile", logger, silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + " Calling releaseFile", 
		pIntf);
    //util.printMessage("TimeStamp="+new Date(), logger,silent);
    inputVec.clear();
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
// putDone
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) throws Exception {

  inputVec.clear();
  inputVec.addElement("SURL="+siteUrl);
  inputVec.addElement("token="+rid);
  inputVec.addElement("label="+label);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmGetClient.putDone", inputVec,silent,useLog);

  //if(gateWayModeEnabled) return ;
  //it should call srmPut.putDone because it depends on srmPut's gatewaymode
  //calling the srmPut's srmCopyClient.putDone

  if(partycopy && siteUrl.startsWith("gsiftp://")) return;

  inputVec.clear();
  inputVec.addElement("siteUrl="+siteUrl);
  inputVec.addElement("rid="+rid);
  inputVec.addElement("label="+label);
  util.printEventLog(_theLogger,"PutDone(srmGetClient)",inputVec,silent,useLog);

  util.printMessage("SRM-CLIENT: " + new Date() + 
	" Calling PutDone(srmGetClient)",logger,silent);
  util.printMessageHL("SRM-CLIENT: " + new Date() + 
	" Calling PutDone(srmGetClient)",pIntf);

  if(_debug) {
    util.printMessage("SRM-CLIENT:siteUrl=" + siteUrl, logger, silent);
    util.printMessage("SRM-CLIENT:rid=" + rid, logger, silent);
    util.printMessage("SRM-CLIENT:label=" + label, logger, silent);
    util.printMessage("SRM-CLIENT:siteUrl=" + siteUrl, pIntf);
    util.printMessage("SRM-CLIENT:rid=" + rid, pIntf);
    util.printMessage("SRM-CLIENT:label=" + label, pIntf);
  }

  int size = srmCopyClientRefArray.size();
  for(int i = 0; i < size; i++) {
    SRMCopyClientRef sCopyRef = 
	(SRMCopyClientRef) srmCopyClientRefArray.elementAt(i);
    if(!sCopyRef.getDone() || size == 1) {
      sCopyRef.setDone();
      SRMWSDLIntf srmCopyClient = sCopyRef.getCopyClient();
      inputVec.clear();
      inputVec.addElement("found reference in the loop");
      util.printEventLog(_theLogger,"PutDone(srmGetClient)",inputVec,silent,useLog);
      if(_debug) { 
        util.printMessage("SRM-CLIENT: found reference in the loop " ,logger,silent);
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

public void abortFiles(String siteUrl, String rid, int label) throws Exception {

  inputVec.clear();
  inputVec.addElement("SURL="+siteUrl);
  inputVec.addElement("token="+rid);
  inputVec.addElement("label="+label);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmGetClient.abortFiles", inputVec,silent,useLog);

  //if(gateWayModeEnabled) return ;
  //it should call srmPut.abortFiles because it depends on srmPut's gatewaymode
  //calling the srmPut's srmCopyClient.putDone

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
  inputVec.addElement("siteUrl="+siteUrl);
  util.printEventLog(_theLogger,"AbortFiles(srmGetClient)",inputVec,silent,useLog);
  util.printMessage("SRM-CLIENT: " + new Date() + " Calling abortFiles(srmGetClient)",logger, silent);
  util.printMessageHL("SRM-CLIENT: " + new Date() + " Calling abortFiles(srmGetClient)",pIntf);

  if(_debug) {
    util.printMessage("SRM-CLIENT: siteUrl="+siteUrl,logger, silent);
    util.printMessage("SRM-CLIENT: rid="+rid,logger, silent);
    util.printMessage("SRM-CLIENT: siteUrl="+siteUrl,pIntf);
    util.printMessage("SRM-CLIENT: rid="+rid,pIntf);
  }

  int size = srmCopyClientRefArray.size();
  for(int i = 0; i < size; i++) {
    SRMCopyClientRef sCopyRef = (SRMCopyClientRef) srmCopyClientRefArray.elementAt(i);
    if(!sCopyRef.getDone() || size == 1) {
      sCopyRef.setDone();
      SRMWSDLIntf srmCopyClient = sCopyRef.getCopyClient();
      inputVec.clear();
      inputVec.addElement("found reference in the loop");
      util.printEventLog(_theLogger,"AbortFiles(srmGetClient)",inputVec,silent,useLog);
      if(_debug) { 
        util.printMessage("SRM-CLIENT: found reference in the loop " ,logger,silent);
      }
      srmCopyClient.abortFiles(siteUrl,rid,label);
      
      break;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDirectGsiFTPTransfer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDirectGsiFTPTransfer(Vector fileInfo) {

  int size = fileInfo.size();
  for(int i = 0; i < size; i++) {
    FileInfo ffInfo = (FileInfo)fileInfo.elementAt(i);
    ffInfo.setIsDirectGsiFTP(true);
    _srmClientIntf.initiatePullingFile(ffInfo);
  }
  int numFilesFailed = 0;
  if(size == numFilesFailed) {
     inputVec.clear();
     inputVec.addElement
	("SetRequestDone with true and true");
     util.printEventLog(_theLogger,"doDirectGsiFTPTransfer.get",
		inputVec,silent,useLog);
     _srmClientIntf.setRequestDone(true,true);
  }
   else {
     inputVec.clear();
     inputVec.addElement
	("SetRequestDone with true and false");
     util.printEventLog(_theLogger,"doDirectGsiFTPTransfer.get",
		inputVec,silent,useLog);
     _srmClientIntf.setRequestDone(true,false);
   }
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void srmGet(String uid, Vector fileInfo)  
		throws Exception {

  HashMap statusArray = new HashMap();
  HashMap oResult = new HashMap ();
  int maxSize = _srmClientIntf.getMaximumFilesPerRequest();
  int totalSubRequest = 1;
  int totalSize = fileInfo.size();
  int modSize = 0;
  int totalFiles = 0;
  int numFilesFailed = 0;

  if(!statusWaitTimeGiven) {
    if(totalSize > 20) {
      statusWaitTime=60*1000;
    }
  }

  if(maxSize == 0) { //if user did not give any option
   maxSize = totalSize;
  }

  if(directGsiFTP) {
    maxSize = totalSize;
    doDirectGsiFTPTransfer(fileInfo);
    return;
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

  if(!bringonline) {
    _srmClientIntf.setTotalSubRequest(totalSubRequest);
    _srmClientIntf.setTotalFilesPerRequest(maxSize);
  }
  else {
    _srmClientIntf.setTotalSubRequest(tFiles);
    _srmClientIntf.setTotalFilesPerRequest(1);
  }


  if(totalSize > 1) {
    if(_debug) {
       util.printMessage("\nSRM-CLIENT: Total number of subrequests=" 
			+ totalSubRequest ,logger,silent);
    }
    inputVec.clear();
    inputVec.addElement(""+totalSubRequest);
    util.printEventLog(_theLogger,"Total number of subrequests",inputVec,silent,useLog);
  }

  if(_debug) {
    util.printMessage("SRM-CLIENT: ::::::::::::::::::::::::::::::::::::::::::::::::::::: ", 
		logger, silent);
    util.printMessage("SRM-CLIENT: :::::: Input parameters for SrmPrepareToGetRequest :::: ", 
		logger, silent);
    util.printMessage("SRM-CLIENT: :::::::::::::::::::::::::::::::::::::::::::::::::::::", 
		logger, silent);
    util.printMessageHL("SRM-CLIENT ....Input parameters from srmPrepareToGet...",pIntf);
  }

  inputVec.clear();

  int count = 0;
  int index = 0;
  while (count < totalSubRequest) {

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
       surl[sindex] = (String) surlVec.elementAt(i);
       if(_debug) {
         util.printMessage("SourceSURL("+sindex+")=" + 
                   surl[sindex],logger,silent);
         util.printMessage("SourceSURL("+sindex+")=" + surl[sindex],pIntf);
       }
       inputVec.addElement("SourceSURL("+sindex+")="+surl[sindex]);
       sindex++;
    }
    util.printEventLog(_theLogger,"Input parameters",inputVec,silent,useLog);

    if (doReserveSpace) {
      fileToken = reserveSpace(uid);
      if(fileToken == null) {
        inputVec.clear();
        inputVec.addElement
          ("SRM reservespace returned null space token, please " +
			"check with SRM admin");
        util.printMessage
	 ("\nSRM-CLIENT: SRM reservespace " +
          "returned null space token, please check with SRM admin", 
                logger,silent);
        util.printMessage
		 ("\nSRM-CLIENT: SRM reservespace returned null space token, please check with SRM admin", pIntf);
        util.printEventLog(_theLogger,"Reserve space response",inputVec,silent,useLog);
        if(pIntf == null) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+1000);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
           System.exit(100); //srm returned no status  
        }
      }
    }

   
    SrmPrepareToGetResponse response = 
	   prepareToGetMultiFiles(surl, fileToken);
    StringBuffer responseBuffer = new StringBuffer();
    HashMap subStatusArray = printGetResult(response,responseBuffer,oResult);

    inputVec.clear();
    if(response == null || subStatusArray == null || subStatusArray.size() == 0) {
       inputVec.addElement("NullResponse");
       if(!responseBuffer.toString().equals("")) {
         if(_debug) {
           util.printMessage("\tExplanation="+responseBuffer.toString(), logger,silent);
           util.printMessage("\tExplanation="+responseBuffer.toString(), pIntf);
         }
         inputVec.addElement("Explanation="+responseBuffer.toString());
       }
       util.printEventLog(_theLogger,"SrmPrepareToGetResponse",inputVec,silent,useLog);
    }
    else {
      statusArray.put(response.getRequestToken(),subStatusArray);
    }

    if(response != null) {
      _srmClientIntf.addRequestToken(response.getRequestToken());
    }
    else {
      inputVec.clear();
      inputVec.addElement("Null response from SRM for this sub request");
      util.printMessage("\nSRM-CLIENT: Null response from SRM for this sub request", logger, silent);
      util.printMessage("\nSRM-CLIENT: Null response from SRM for this sub request", pIntf);
      util.printEventLog(_theLogger,"SrmPrepareToGetResponse",inputVec,silent,useLog);
    }

    totalFiles = totalFiles + subStatusArray.size();

    if(bringonline) {
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
    Vector keySURLS = new Vector();
    while (subStatusArray.size() > 0) {
        keySURLS.clear();
        Object[] keySetArray = subStatusArray.keySet().toArray();
        rCode.delete(0,rCode.length());
        expBuffer.delete(0,expBuffer.length());



       
        //System.out.println(">>>PinnedFiles"+pinnedFiles);
        //System.out.println(">>>totalFiles"+totalFiles);
        //System.out.println(">>>FailedFiles"+failedFiles);
        if((pinnedFiles+failedFiles) == totalFiles) {
              inputVec.clear();
              inputVec.addElement("oneTimePutCall=true");
              inputVec.addElement(" totalpinnedFilesFirstTime="+pinnedFiles);
              inputVec.addElement(" totalFilesFailedFirstTime="+failedFiles);
              inputVec.addElement(" totalFiles="+totalFiles);
              util.printEventLog(_theLogger,"oneTimePutCall",
                inputVec,silent,useLog);
              waitToInitiateSRMPutCallUntilAllFilesDone=true;
        }
        else {
              inputVec.clear();
              inputVec.addElement("oneTimePutCall=false");
              inputVec.addElement(" totalpinnedFilesFirstTime="+pinnedFiles);
              inputVec.addElement(" totalFilesFailedFirstTime="+failedFiles);
              inputVec.addElement(" totalFiles="+totalFiles);
              util.printEventLog(_theLogger,"oneTimePutCall",
                inputVec,silent,useLog);
        }

        for(int k = 0; k < keySetArray.length; k++) {
          expBuffer.delete(0,expBuffer.length());
          oSubKey = (String) keySetArray [k];
          //System.out.println(">>oSubKey="+oSubKey);
          //System.out.println(">>oResult="+oResult);
          subKey = (String) oResult.get(oSubKey);
          //System.out.println(">>subKey="+subKey);
          TGetRequestFileStatus status =
           (TGetRequestFileStatus)(subStatusArray.get(oSubKey));
          TStatusCode code = status.getStatus().getStatusCode();
          if(timedOutCase) {
            Object[] objArray = fileInfo.toArray();
            for(int kk = 0; kk < objArray.length; kk++) {
              FileInfo ffInfo = (FileInfo) objArray[kk];
              String turl = subKey;
              if(ffInfo.getSURL().equals(turl)) {
               _srmClientIntf.srmFileFailure(ffInfo.getLabel(),
                 "File Status TimedOut.");
               if(code != null) {
                 ffInfo.setFileStatus(code.toString());
               }
               if(status.getStatus().getExplanation() != null) {
                 ffInfo.setFileExplanation(status.getStatus().getExplanation());
               }
               else {
                 ffInfo.setFileExplanation("SRM-CLIENT: File Status TimedOut");
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
             String turl = subKey;
             //System.out.println(">>>TURL ="+subKey);
             //System.out.println(">>>SURL ="+ffInfo.getSURL());
             if(ffInfo.getSURL().equals(turl)) {
               //System.out.println(">>>TTURL="+status.getTransferURL());
               if(status != null && status.getTransferURL() != null) {
                 String srmTurl = ""+status.getTransferURL();
                 String srmFileSize = ""+status.getFileSize();
                 ffInfo.setSURL(srmTurl);
                 ffInfo.setExpectedSize(srmFileSize);
                 ffInfo.setRID(subResponse); 
                 ffInfo.setGetRID(subResponse); 
                 ffInfo.setTransferURL(srmTurl);
                 ffInfo.setFileStatus(code.toString());
                 if(status.getStatus().getExplanation() != null) {
                   ffInfo.setFileExplanation(status.getStatus().getExplanation());
                 }
                 else {
                   ffInfo.setFileExplanation("");
                 }
                 if(!partycopy) { 
                   _srmClientIntf.initiatePullingFile(ffInfo);
                 }
                 else { 
                   if(ffInfo.getOrigTURL().startsWith("gsiftp://")) {
                     _srmClientIntf.initiatePullingFile(ffInfo);
                   }
                   else {
                     if(!waitToInitiateSRMPutCallUntilAllFilesDone) {
                       initiateSRMPutCall(ffInfo,totalFiles);   
                     }
                     else {
                       oneTimePutCallVec.addElement(ffInfo);
                     }
                   }
                 }
               }
               else {
                 ffInfo.setFileStatus(code.toString());
                 if(status.getStatus().getExplanation() != null) {
                   ffInfo.setFileExplanation
			(status.getStatus().getExplanation());
                 }
                 else {
                   ffInfo.setFileExplanation("");
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

        //if all files are returned from SourceSRM (gateway) source
        //then ok to call SRMPrepareToPut with all files in it.
        if(waitToInitiateSRMPutCallUntilAllFilesDone) {
           inputVec.clear();
           inputVec.addElement(" totalpinnedFilesFirstTime="+pinnedFiles);
           inputVec.addElement(" totalFilesFailedFirstTime="+failedFiles);
           inputVec.addElement(" totalFiles="+totalFiles);
           util.printEventLog(_theLogger,
             "waitedToInitiateSRMPutCallUntilAllFilesDone",
           inputVec,silent,useLog);
           initiateSRMPutCall(oneTimePutCallVec,totalFiles);   
        }


        keySetArray = subStatusArray.keySet().toArray();

        if(keySetArray.length > 0 &&
		 !util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
            util.printMessage
	     ("SRM-CLIENT: Max retry check status exceeded for get status", 
			logger,silent);
            util.printMessage
	     ("SRM-CLIENT: Max retry check status exceeded for get status", pIntf);
            expBuffer.append
	      ("SRM-CLIENT: Max retry check exceeded for get status .");
            inputVec.clear();
            inputVec.addElement("Max retry check status exceded for get status");
            util.printEventLog(_theLogger,"Max retry exceeded",
		inputVec,silent,useLog);
            timedOutCase=true;
        }

        if(keySetArray.length > 0 && !timedOutCase) {
          oSubKey = (String) keySetArray [0];
          TGetRequestFileStatus status =
           (TGetRequestFileStatus)(subStatusArray.get(oSubKey));
          TStatusCode code = status.getStatus().getStatusCode();

            if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
               (code == TStatusCode.SRM_REQUEST_QUEUED)) {

                int tStatusTime = 1;
              
                if(statusWaitTime >= 1000) {
                   tStatusTime = statusWaitTime/1000;
                }

                util.printMessage("\nSRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",logger,silent);
                util.printMessage("SRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",pIntf);
                inputVec.clear();
                inputVec.addElement("NextStatus="+tStatusTime+" seconds.");
                util.printEventLog(_theLogger,"NextStatusCall",
					inputVec,silent,useLog);
            }
        }

        TGetRequestFileStatus[] statuses = null;
        IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed ();
        if (subStatusArray.size() > 0 && !timedOutCase) {
           Thread.sleep(statusWaitTime);
           if(keySURLS.size() > 0) {
             statuses  = checkGetStatus(keySURLS,subResponse,rCode,
							wholeRequestFailed);
           }
         if(statuses != null) {
           for(int kk = 0; kk < statuses.length; kk++) {
            TGetRequestFileStatus status =
              (TGetRequestFileStatus) statuses[kk];
            expBuffer.delete(0,expBuffer.length());
            MyGlobusURL gurl = 
				new MyGlobusURL(status.getSourceSURL().toString(),1);
            String protocol = gurl.getProtocol();
            String host = gurl.getHost();
            int port = gurl.getPort();
            String path = gurl.getFilePath();
            int idx = path.indexOf("?SFN=");
            if(idx != -1) {
              path = path.substring(idx+5);
            }
            //added on oct 05, 2009 for the transfering same SURL one
            //or more times
            //I dont' about this, we need to check this with Alex,
            
            //System.out.println(">>>>subStatusArray.size()="+subStatusArray.size());
            Object[] indexKeys = indexMap.keySet().toArray();
            for(int mm = 0; mm < indexKeys.length; mm++ ) {
              int mmIndex = ((String)indexKeys[mm]).lastIndexOf("#"); 
              if(mmIndex != -1) {
                String temp = ((String)indexKeys[mm]).substring(0,mmIndex);
                if(temp.equals(protocol+"://"+host+":"+port+"/"+path)) {
                  Object oo = indexMap.get((String)indexKeys[mm]);
                  Integer ii = (Integer) oo;
                  subStatusArray.put(protocol+"://"+host+":"+port+"/"+path+"#"+ii.intValue(),status);
                }
              }
            }
            //Object oo = indexMap.get(protocol+"://"+host+":"+port+"/"+path);
            //Integer ii = (Integer) oo;
            //subStatusArray.put(protocol+"://"+host+":"+port+"/"+path+"#"+ii.intValue(),status);
            TStatusCode tempCode = null;

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
          
                 if (tempCode == TStatusCode.SRM_FILE_PINNED ||
                     tempCode == TStatusCode.SRM_SUCCESS ||
                     tempCode == TStatusCode.SRM_DONE ||
                     tempCode == TStatusCode.SRM_FILE_IN_CACHE) {
                     if(status.getRemainingPinTime() != null) {
                       util.printMessage("\nSRM-CLIENT: RemainingPinTime="+
   					    status.getRemainingPinTime(), logger,silent);
                       if(status.getRemainingPinTime().intValue() <= 60) {
                        util.printMessage(
						  "\nSRM-CLIENT: WARNING: File transfer may be " +
					      " interrupted, because TURL lifetime is too short", 
						  logger,silent);
                       }
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
              inputVec.addElement("FileStatusCode="+tempCode);
              inputVec.addElement("Explanation from server="+expBuffer.toString());
              util.printMessage("SRM-CLIENT: SURL="+status.getSourceSURL(),
				logger,silent);
              util.printMessage
                ("SRM-CLIENT: FileStatus code from server="+tempCode,logger,silent);
              util.printMessage
                ("SRM-CLIENT: FileStatus code from server="+tempCode,pIntf);
              util.printMessage("SRM-CLIENT: SURL="+status.getSourceSURL(),
				pIntf);
              util.printMessage
                ("SRM-CLIENT: Explanation from server=" + expBuffer.toString(),
                   logger,silent);
              util.printMessage
                ("SRM-CLIENT: Explanation from server=" + expBuffer.toString(),pIntf);
			  util.printEventLog(_theLogger,"StatusResponse",inputVec,silent,useLog);
            }
          }//end for
         }//end else
       }//end if(!timedOutCase)
    }//while subStatus
    if(subStatusArray.size() == 0) statusArray.remove(ids);
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
     inputVec.addElement ("SetRequestDone with true and true");
     util.printEventLog(_theLogger,"srmGet",inputVec,silent,useLog);
     _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     _srmClientIntf.setRequestDone(true,true);
   }
   else {
    if(totalFiles == numFilesFailed) {
     inputVec.clear();
     inputVec.addElement ("SetRequestDone with true and true");
     util.printEventLog(_theLogger,"srmGet",inputVec,silent,useLog);
     if(!partycopy) {
      _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     }
     else {
      if(_srmClientIntf.getRequestStatus().equals("")) {
        _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
      }
     }
     _srmClientIntf.setRequestDone(true,true);
    }
    else {
     inputVec.clear();
     inputVec.addElement ("SetRequestDone with true and false");
     util.printEventLog(_theLogger,"srmGet",inputVec,silent,useLog);
     if(!partycopy) {
      _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
     }
     else {
      if(_srmClientIntf.getRequestStatus().equals("")) {
        _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
      }
     }
     _srmClientIntf.setRequestDone(true,false);
    }
   }//end else 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiateSRMPutCall 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void initiateSRMPutCall (Vector oneTimePutCallVec,
        int tFiles) throws Exception {

  int size = oneTimePutCallVec.size();
  for(int i = 0; i < size; i++) {
    FileInfo fInfo = (FileInfo) oneTimePutCallVec.elementAt(i);
    vecFiles.addElement(fInfo);
  } 
  if(size != 0) {
    callPrepareToPut(tFiles,true);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiateSRMPutCall 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void initiateSRMPutCall (FileInfo fInfo, int tFiles) throws Exception {

  vecFiles.addElement(fInfo);
  callPrepareToPut(tFiles,false);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callPrepareToPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void callPrepareToPut (int tFiles, boolean oneP2PCall) throws Exception {

  if(!firstTime) { 
    pinnedFiles = vecFiles.size();
  }
  else {
    inputVec.clear();
    inputVec.addElement("Total files in the vector="+vecFiles.size());
    inputVec.addElement("Total files pinned first time ="+pinnedFiles);
    inputVec.addElement("Total files failed first time ="+failedFiles);
    util.printEventLog(_theLogger,"InitiateSRMPutCall",inputVec,silent,useLog);
  }
  if(vecFiles.size() >= pinnedFiles) {
    if (vecFiles.size() >= 0) { 
      FileInfo fInfo = (FileInfo) vecFiles.elementAt(0);
      String surl = fInfo.getTURL();
      if(oneP2PCall) {
        util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling initiateSRMPutCall now for total files " + 
                        vecFiles.size(), logger,silent);
        util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling initiateSRMPutCall now for total files " + 
                        vecFiles.size(), pIntf);
        inputVec.clear();
        inputVec.addElement("Calling initiateSRMPutCall now for total files" + 
                        vecFiles.size());
        util.printEventLog(_theLogger,"InitiateSRMPutCall",inputVec,silent,useLog);
      }
      else {
        util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling initiateSRMPutCall now for " + surl, logger,silent);
        util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling initiateSRMPutCall now for " + surl, pIntf);
        inputVec.clear();
        inputVec.addElement("Calling initiateSRMPutCall now for " + surl);
        util.printEventLog(_theLogger,"InitiateSRMPutCall",inputVec,silent,useLog);
      } 
      String ssUrl = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);

      util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Connecting to serviceUrl : "+ ssUrl,logger,silent);
      util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Connecting to serviceUrl : "+ ssUrl,pIntf);

      int pTime = 0;
      //if(firstTime) pTime = 500;
      SRMWSDLIntf srmCopyClient = new SRMPutClient (_srmClientIntf,
             ssUrl, uid, vecFiles, fileToken, statusToken,
             doReserveSpace, overwrite,
             tokenLifetime, tokenSize, guarnSize,
             _credential, 
             fileStorageType,retentionPolicyInfo,0,pTime,0,
			 false, tFiles, true,
             parallelism, bufferSize, dcau, false,
             statusMaxTimeAllowed, statusWaitTime,
             protocolsList,
             //statusWaitTimeGiven, 
             true, storageInfo, remoteTransferInfo, 
	     userDesc, _theLogger, logger, pIntf, _debug, 
	     silent,useLog,false,proxyType,false,
	     connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
	     delegationNeeded, noAbortFile,domkdir,false,threshHold,false,
             numRetry, retryTimeOut,checkPing);

      this.srmCopyClientRefArray.addElement(new SRMCopyClientRef(srmCopyClient));
      srmCopyClient.performTransfer(false);
      vecFiles.clear(); //resetting here
      pinnedFiles=0; //resetting here
      failedFiles=0; //resetting here
      firstTime=false;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printGetErrorResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printErrorResponse(TGetRequestFileStatus fileStatus) {
   inputVec.clear();
   util.printMessage("SRM-CLIENT: ...........................................",logger,silent);
   if(fileStatus == null) {
     util.printMessage("\n\tSRM-CLIENT: Null TGetRequestFileStatus ",logger, silent);
     util.printMessage("\n\tSRM-CLIENT: Null TGetRequestFileStatus ",pIntf);
     inputVec.addElement("Null TGetRequestFileStatus");
     util.printEventLog(_theLogger,"PrintErrorResponse",inputVec,silent,useLog);
     return;
   }

   if(_debug) {
    util.printMessage("\n\tsurl="+
			fileStatus.getSourceSURL(),logger,silent);
    util.printMessage("\tstatus="+
			fileStatus.getStatus().getStatusCode(),logger,silent);
    util.printMessage("\texplanation="+
            fileStatus.getStatus().getExplanation(),logger,silent);
    util.printMessage("\tsurl="+
			fileStatus.getSourceSURL(),pIntf);
    util.printMessage("\tstatus="+
			fileStatus.getStatus().getStatusCode(),pIntf);
    util.printMessage("\texplanation="+
            fileStatus.getStatus().getExplanation(),pIntf);
   }

   inputVec.addElement("SURL="+fileStatus.getSourceSURL());
   inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
   inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
   util.printEventLog(_theLogger,"PrintErrorResponse",inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callSoapThread
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Object callSoapThread(Object request,
        Object result, String methodName) throws Exception {

     int retrySoFar=0;
     int nRetry = numRetry;

     boolean timeOutHappened=false;
     boolean statusCall=false;

     if(methodName.equals("srmstatusofget")) {
        nRetry = 1;
        statusCall = true;
     }

     while(retrySoFar < nRetry) {

       if(retrySoFar > 0) {
         Thread.sleep(retryTimeOut);
       }

       inputVec.clear();
       inputVec.addElement("Creating NewCall for " + methodName +
                " for numRetry="+retrySoFar);
       util.printEventLog(_theLogger,"SRMGetClient.callSoapThread",
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
         if(timeOutHappened && responseObject == null) {
           inputVec.clear();
           inputVec.addElement("Interrupting " + methodName);
           inputVec.addElement("timedout and responseobject is null");
           util.printEventLog(_theLogger,"SRMGetClient.callSoapThread",
                inputVec,silent,useLog);
           try {
            _srm=null;
            soapCallThread.setInterrupt(true);
            openSRMConnection();
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMGetClient.callSoapThread",ie);
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
             util.printEventLog(_theLogger,"SRMGetClient.callSoapThread",
                inputVec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
    }//end while

    /*
    if(methodName.equals("srmpreparetoget")) {
      result = null;
      timeOutHappened=true;
    }
    */

    if(timeOutHappened && result == null && !statusCall) {
           inputVec.clear();
           inputVec.addElement("setrequestdone to true and failed");
           inputVec.addElement("timedout and responseobject is null");
           inputVec.addElement("SRM server did not respond, server may be busy");
           util.printEventLog(_theLogger,"SRMGetClient.callSoapThread",
                inputVec,silent,useLog);
       if(methodName.equals("srmpreparetoget")) {
         _srmClientIntf.setRequestTimedOut(true);
       }
     }

     return result;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printGetResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public HashMap printGetResult (SrmPrepareToGetResponse response, 
		StringBuffer responseStatus, HashMap oResult) throws Exception {

   HashMap result = new HashMap ();
   inputVec.clear();
   if(_debug) {
   util.printMessage("\nSRM-CLIENT: ##### Output from SRM #####",logger,silent);
   util.printMessageHL("\nSRM-CLIENT: #### Output from SRM ####",pIntf);
   }

   if(response == null) {
     util.printMessage("SRM-CLIENT: GetRequest Response is null ",logger,silent);
     util.printMessage("SRM-CLIENT: GetRequest Response is null ",pIntf);
     inputVec.addElement("GetRequest response is null");
     util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
     return result;
   }

   if(response.getRequestToken() != null) {
     //if(_debug) {
     util.printMessage("request.token= " + response.getRequestToken(),logger,silent);
     util.printMessage("request.token= " + response.getRequestToken(),pIntf);
     requestToken = response.getRequestToken();
     //} 
     inputVec.addElement("request.token="+response.getRequestToken());
   }
   else {
     if(_debug) {
     util.printMessage("request.token= " + null,logger,silent);
     util.printMessage("request.token= " + null,pIntf);
     }
     inputVec.addElement("request.token="+null);
   }
   util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
   

   if(response.getReturnStatus() == null) {
     inputVec.clear();
     util.printMessage("SRM-CLIENT: Null return status ", logger,silent);
     util.printMessage("SRM-CLIENT: Null return status ", pIntf);
     responseStatus.append("null return status");
     inputVec.addElement("Null return status");
     util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
     return result;
   }

   if(submitOnly) {
     rStatus = response.getReturnStatus();
   }

   inputVec.clear();
   inputVec.addElement("Request.status="+response.getReturnStatus().getStatusCode());
   inputVec.addElement("Request.explanation="+response.getReturnStatus().getExplanation());
   resultStatus = response.getReturnStatus().getStatusCode().toString();
   if(response.getReturnStatus().getExplanation() != null) {
     resultExplanation = response.getReturnStatus().getExplanation();
   }
   util.printMessage ("\nRequest.status="+
        response.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("Request.explanation="+
        response.getReturnStatus().getExplanation(),logger,silent);
   util.printMessage ("Request.status="+
        response.getReturnStatus().getStatusCode(),pIntf);
   util.printMessage("Request.explanation="+
        response.getReturnStatus().getExplanation(),pIntf);

   util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);

   if(response.getReturnStatus().getExplanation() != null) {
     responseStatus.append(response.getReturnStatus().getExplanation());
   }

   responseStatus.append(response.getReturnStatus().getStatusCode().toString());

   inputVec.clear();

   if(response.getRemainingTotalRequestTime() != null) {
     if(_debug) {
     util.printMessage("request.remainingTotalRequestTime=" +
        response.getRemainingTotalRequestTime(),logger,silent);
     util.printMessage("request.remainingTotalRequestTime=" +
        response.getRemainingTotalRequestTime(),pIntf);
     }
     inputVec.addElement("request.remainingTotalRequestTime="+
        response.getRemainingTotalRequestTime());
   }
   else {
     if(_debug) {
     util.printMessage("request.remainingTotalRequestTime=" +
        null,logger,silent);
     util.printMessage("request.remainingTotalRequestTime=" +
        null,pIntf);
     }
     inputVec.addElement("request.remainingTotalRequestTime=null");
   }

   if(response.getArrayOfFileStatuses() == null) {
     inputVec.clear();
     inputVec.addElement("Null ArrayOfFileStatuses");
     util.printMessage("SRM-CLIENT: Null ArrayOfFileStatuses ", logger,silent);
     util.printMessage("SRM-CLIENT: Null ArrayOfFileStatuses ", pIntf);
     util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
     return result;
   }

   if(response.getArrayOfFileStatuses().getStatusArray() == null) {
     inputVec.clear();
     util.printMessage("SRM-CLIENT: Null getStatusArray()", logger,silent);
     util.printMessage("SRM-CLIENT: Null getStatusArray()", pIntf);
     inputVec.addElement("Null getStatusArray()");
     util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
     return result;
   }

   int size = response.getArrayOfFileStatuses().getStatusArray().length;

   int localStatusWaitTime = 60;
   boolean estimatedTimeGiven=false;
   for (int i=0;  i < size; i++) {
     inputVec.clear();
     TGetRequestFileStatus fileStatus =
       response.getArrayOfFileStatuses().getStatusArray(i);

     if(fileStatus.getStatus() == null) {
        inputVec.clear();
        inputVec.addElement("Null return status from SRM");
        util.printMessage("SRM-CLIENT: Null return status from SRM", logger,silent);
        util.printMessage("SRM-CLIENT: Null return status from SRM", pIntf);
        util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
        return result;
     }
     else {
        inputVec.clear();
        inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
        inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
        if(fileStatus.getStatus().getStatusCode() == 
			TStatusCode.SRM_FILE_PINNED) {
          pinnedFiles++;
        }
        else if(fileStatus.getStatus().getStatusCode() != 
			TStatusCode.SRM_REQUEST_QUEUED) {
          failedFiles++;
        }
        if(_debug) {
        util.printMessage("\n\tsurl="+
          fileStatus.getSourceSURL(),logger,silent);
        util.printMessage("\tstatus="+
          fileStatus.getStatus().getStatusCode(),logger,silent);
        util.printMessage("\texplanation="+
          fileStatus.getStatus().getExplanation(),logger,silent);
        util.printMessage("\n\tsurl="+
          fileStatus.getSourceSURL(),pIntf);
        util.printMessage("\tstatus="+
          fileStatus.getStatus().getStatusCode(),pIntf);
        util.printMessage("\texplanation="+
          fileStatus.getStatus().getExplanation(),pIntf);
        }
     }
     if(fileStatus.getSourceSURL() != null) {
        inputVec.clear();
        inputVec.addElement("\tSURL="+ fileStatus.getSourceSURL().toString());
     }
     if(fileStatus.getFileSize() != null) {
        util.printMessage("\tFileSize="+fileStatus.getFileSize(),pIntf);
     }
     if(fileStatus.getEstimatedWaitTime() != null) {
        Integer xx = fileStatus.getEstimatedWaitTime();
        int yy = xx.intValue();
        if(yy != -1) {
          estimatedTimeGiven=true;
          if(_debug) {
               System.out.println("\nSRM-CLIENT: EstimatedWait " +
				"given by server is " + yy + " seconds.");
          }
          if(localStatusWaitTime > yy) {
            localStatusWaitTime = yy;
          } 
        }
     }
     util.printEventLog(_theLogger,"PrintGetResult",inputVec,silent,useLog);
     MyGlobusURL gurl = new MyGlobusURL(fileStatus.getSourceSURL().toString(),1);
     String protocol = gurl.getProtocol();
     String host = gurl.getHost();
     int port = gurl.getPort();
     String path = gurl.getFilePath();
     int idx = path.indexOf("?SFN=");
     if(idx != -1) {
        path = path.substring(idx+5);
     }
            //added on oct 05, 2009 for the transfering same SURL one
            //or more times
     result.put(protocol+"://"+host+":"+port+"/"+path+"#"+i, fileStatus);
            //added on oct 05, 2009 for the transfering same SURL one
            //or more times
     oResult.put(protocol+"://"+host+":"+port+"/"+path+"#"+i,
       fileStatus.getSourceSURL().toString());
     indexMap.put(protocol+"://"+host+":"+port+"/"+path+"#"+i, new Integer(i));
     Object[] objArray = fileInfo.toArray();
     for(int j = 0; j < objArray.length; j++) {
          FileInfo fInfo = (FileInfo) objArray[j];
          if(fInfo.getOrigSURL().equals(fileStatus.getSourceSURL().toString()))           {
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
    SRMUtilClient.getSpaceTokenMeta(_srm,token,logger,_debug,_credential,
        proxyType,serverUrl,_theLogger,
        pIntf, _srmClientIntf, silent, useLog, connectionTimeOutAllowed,
        setHTTPConnectionTimeOutAllowed, delegationNeeded, numRetry,
        retryTimeOut);
  }
  else {
    inputVec.clear();
    inputVec.addElement("Cannot do getSpaceTokenMeta for null token");
    util.printEventLog(_theLogger,"GetSpaceTokenMeta",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Cannot do getSpaceTokenMeta for null token ",logger,silent);
    util.printMessage("SRM-CLIENT: Cannot do getSpaceTokenMeta for null token ",pIntf);
  }
 }
}
	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(boolean keepSpace, String surl, 
     String token,int label) throws Exception {


  inputVec.clear();
  inputVec.addElement("SURL="+surl);
  inputVec.addElement("token="+token);
  inputVec.addElement("label="+label);
  inputVec.addElement("keepSpace="+keepSpace);
  inputVec.addElement("gatewaymodeenabled="+gateWayModeEnabled);
  util.printEventLog(_theLogger,"srmGetClient.releaseFile",inputVec,silent,useLog);

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  String sCode = 
	SRMUtilClient.releaseFile
          (_srm,keepSpace,surl,token,uid,proxyString,_debug,
           logger,pIntf,_credential,proxyType,serverUrl,_theLogger,
            pIntf, _srmClientIntf, silent, useLog, 
            connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
            delegationNeeded, numRetry, retryTimeOut);

  if(sCode.equalsIgnoreCase("SRM_RETURNED_NO_STATUS")) {
    FileIntf fIntf = (FileIntf) fileInfo.elementAt(label); 
    String temp = fIntf.getFileExplanation();
    if(temp.equals("")) {
      fIntf.setFileExplanation("SRM-CLIENT: Failed during srmReleaseFile");
    }
    else {
      fIntf.setFileExplanation(temp+ ", Failed during srmReleaseFile");
    }
  }
  return sCode;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public void releaseSpace(String token, 
    boolean forceFileRelease) throws Exception {
		 
  if(gateWayModeEnabled) return ;

  SRMUtilClient.releaseSpace(_srm,token,uid,forceFileRelease,
       logger,_debug,_credential,proxyType,serverUrl,_theLogger,
       pIntf, _srmClientIntf, silent, useLog, 
       connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
       delegationNeeded, numRetry, retryTimeOut);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareToGetMultiFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public SrmPrepareToGetResponse prepareToGetMultiFiles(String[] surls, 
	String  token)  throws Exception {

   if(_debug) {
     getSpaceTokenMeta(token);
   }

   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   /*
   if(domkdir && !recursive) {
     SRMUtilClient utilClient = new SRMUtilClient
        (serverUrl,uid,userDesc, _credential, _theLogger, logger,
        pIntf, _debug,silent,false, false,
	    statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
	    connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		delegationNeeded,numRetry,retryTimeOut);

     StringBuffer sCode = new StringBuffer();
     if(fileInfo.size() == 1) {
      FileInfo fInfo  = (FileInfo) fileInfo.elementAt(0);
      String turl = fInfo.getTURL().toString();
      MyGlobusURL gurl = new MyGlobusURL(turl,1);
      String path = gurl.getPath();
      String firstPart = "file:////"+gurl.getHost()+":"+gurl.getPort(); 

      String temp1 = ""; 
      while (true) {
          int idx = path.indexOf("/");
          if(idx != -1) {
            int idx2 = path.indexOf("/",idx+1);
            if(idx2 != -1) {
              //System.out.println(">>>TEMP1="+temp1);
              String temp2 = path.substring(idx,idx2);
              //System.out.println(">>>TEMP2="+temp2);
              fInfo.setTURL(firstPart+temp1+temp2);
              boolean b = utilClient.doSrmMkdir
					(fileInfo,sCode,true,false,true,false);
              String temp3 = path.substring(idx2);
              //System.out.println(">>>TEMP3="+temp3);
              temp1 = temp1+temp2;
              path = temp3;

              if(!b) {
                   util.printMessage("\nSRM-CLIENT: Cannot continue put since directory" +
                      " creation is not successful", logger,silent);
                   int value = util.mapStatusCode(sCode.toString());     
                   System.exit(value);
              }
            }
            else break;
          }
          else break;
      }//end while
      fInfo.setTURL(turl); //set the turl back


   }
   else {
     util.printMessage(
         "\nSRM-CLIENT: Cannot continue, since -mkdir option is " +
         " only for single source and single target", logger,silent); 
         System.exit(1);
  } 

  }
  */

  inputVec.clear();

   SrmPrepareToGetRequest r = new SrmPrepareToGetRequest();

   if(_debug) {
     util.printMessage("SRM-CLIENT: AuthorizationID="+uid, logger, silent);
     util.printMessage("SRM-CLIENT: AuthorizationID="+uid, pIntf);
   }
   inputVec.addElement("AuthorizationID="+uid);
   
   //temporarily commenting because gsoap, infn site did not work if i set this one 
   //uncommenting on April 18, 07 after Alex's suggestion

   if(!userDesc.equals("")) {
     if(_debug) {
       util.printMessage("SRM-CLIENT: UserDescription="+userDesc, logger, silent);
       util.printMessage("SRM-CLIENT: UserDescription="+userDesc, pIntf);
     }
     inputVec.addElement("UserDescription="+userDesc);
     r.setUserRequestDescription(userDesc);
   }

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

   if(totalRequestTime != 0) {
     r.setDesiredTotalRequestTime(new Integer(totalRequestTime));
     if(_debug) {
       util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime,logger,silent);
       util.printMessage("SRM-CLIENT: TotalRequestTime="+totalRequestTime,pIntf);
     }
     inputVec.addElement("TotalRequestTime="+totalRequestTime);
   }
   if(pinLifeTime != 0) {
     r.setDesiredPinLifeTime(new Integer(pinLifeTime));
     if(_debug) {
       util.printMessage("PinLifeTime="+pinLifeTime,logger,silent);
       util.printMessage("PinLifeTime="+pinLifeTime,pIntf);
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

   inputVec.addElement("StorageInfo="+storageInfo);
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
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo,logger,silent);
     util.printMessage("SRM-CLIENT: StorageInfo="+storageInfo,pIntf);
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
		   String msg = 
			"Given storage info is not in the correct format " +
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
     if(i < (protocols.length-1)) 
       tt = tt+protocols[i]+",";
     else {
       tt = tt+protocols[i];
     }
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
     util.printMessage("SRM-CLIENT: RetentionPolicy="+retentionPolicyInfo.getRetentionPolicy(), pIntf);
     }
     inputVec.addElement("RetentionPolicy="+retentionPolicyInfo);
     if(_debug) {
     util.printMessage("ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), logger,silent);
     util.printMessage("ACCESSINFO="+retentionPolicyInfo.getAccessLatency(), pIntf);
     }
     inputVec.addElement("AccessInfo="+retentionPolicyInfo.getAccessLatency());
     r.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
   }

   r.setTransferParameters(transferParameters);

   ArrayOfTGetFileRequest arrayOfFileRequest = new ArrayOfTGetFileRequest();
   arrayOfFileRequest.setRequestArray(fileReqList);
   r.setArrayOfFileRequests(arrayOfFileRequest);

   requestDate = new Date ();
   util.printMessage("\nSRM-CLIENT: " + requestDate + 
		" Calling SrmPrepareToGet Request now ...",logger,silent);
   util.printMessageHL("\nSRM-CLIENT: " + requestDate + 
		" Calling SrmPrepareToGet Request now ...",pIntf);
   util.printEventLog(_theLogger,"Input Parameters for SrmPrepareToGet",inputVec,silent,useLog);
  

   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   SrmPrepareToGetResponse result = null;

   try {
      result = (SrmPrepareToGetResponse) 
                callSoapThread(r,result,"srmpreparetoget");
   }catch(Exception e) {
      util.printEventLogException(_theLogger,"srmGet.get",e);
      util.printMessage("SRM-CLIENT: " + e.getMessage(),logger,silent);
      util.printMessage("SRM-CLIENT: " + e.getMessage(),pIntf);
      throw e;
   }
   return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int doStatus(String uid, Vector fileInfo, String rToken)
      throws URI.MalformedURIException, java.rmi.RemoteException {
   
  if(_debug) {
    util.printMessage("\nSRM-CLIENT: ...Input parameters for SrmStatusOfGetRequestRequest...", logger,silent);
    util.printMessage("SRM-CLIENT: RequestToken="+rToken,logger,silent);
    util.printMessage("SRM-CLIENT: AuthorizationId="+uid,logger,silent);
  }
  SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
  int size = fileInfo.size();
  String sampleSURL = "";

  URI[] tsurl = new URI[size];
  for(int i = 0; i < size; i++) {
    FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
    fIntf.setRID(rToken);
    URI temp = new URI(fIntf.getSURL());
    tsurl[i] = temp;
    sampleSURL = fIntf.getSURL();
    if(_debug) {   
      util.printMessage("SRM-CLIENT: SURL="+sampleSURL,logger,silent);
    }
  }
  r.setArrayOfSourceSURLs(SRMUtilClient.convertToArrayOfAnyURI(tsurl));

  r.setRequestToken(rToken);

  if(!uid.equals("")) {
    r.setAuthorizationID(uid);
  }

  if(_debug) {
    util.printMessage("SRM-CLIENT: #### Output from SRM ####",logger,silent);
  }

  SrmStatusOfGetRequestResponse result = null;
  TStatusCode sCode = null;

  try {
  result = (SrmStatusOfGetRequestResponse) 
                callSoapThread(r,result,"srmstatusofget");


  if(result == null) {
    util.printMessage("\nSRM-CLIENT: Null result.", logger,silent);
    util.printMessage("SRM-CLIENT: May be the given requestId is not valid",logger,silent);
    util.printMessage("SRM-CLIENT: RequestId : "+rToken,logger,silent);
    util.printMessage("\nSRM-CLIENT: Null result.", pIntf);
    util.printMessage("SRM-CLIENT: May be the given requestId is not valid",
		pIntf);
    util.printMessage("SRM-CLIENT: RequestId : "+rToken,pIntf);
    inputVec.clear();
    inputVec.addElement("RequestId="+rToken);
    inputVec.addElement("NullResult");
    inputVec.addElement("Reason=May be the given requestId is not valid");
    util.printEventLog(_theLogger,"SrmStatusOfGetRequestResponse",inputVec,silent,useLog);
    return 1000;
  }

  ArrayOfTGetRequestFileStatus arrayFStatus =
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


  TGetRequestFileStatus[] fileStatus = arrayFStatus.getStatusArray();

  if(fileStatus.length == 0) {
    util.printMessage("No files in this request", logger,silent);
    inputVec.addElement("RequestId="+rToken);
    inputVec.addElement("No files in this request");
    util.printEventLog(_theLogger,"SrmStatusOfGetRequestResponse",inputVec,silent,useLog);
    return util.mapStatusCode(sCode);
  }

  for(int i = 0; i < fileStatus.length; i++) {

      inputVec.clear();
      if(fileStatus[i].getSourceSURL() != null) {
          util.printMessage("\tsurl="+
                fileStatus[i].getSourceSURL(),logger,silent);
          util.printMessage("\tsurl="+
                fileStatus[i].getSourceSURL(),pIntf);
          inputVec.addElement("SURL="+fileStatus[i].getSourceSURL());
      }
      if(fileStatus[i].getTransferURL() != null) {
          util.printMessage("\tTransferURL="+
                fileStatus[i].getTransferURL(),logger,silent);
          util.printMessage("\tTransferURL="+
                fileStatus[i].getTransferURL(),pIntf);
          inputVec.addElement("TransferURL="+fileStatus[i].getTransferURL());
      }
      if(fileStatus[i].getFileSize() != null) {
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),logger,silent);
          util.printMessage("\tFileSize="+
                fileStatus[i].getFileSize(),pIntf);
          inputVec.addElement("FileSize="+fileStatus[i].getFileSize());
      }
      if(fileStatus[i].getRemainingPinTime() != null) {
          util.printMessage("\tRemainingPinTime="+
                fileStatus[i].getRemainingPinTime(),logger,silent);
          util.printMessage("\tRemainingPinTime="+
               fileStatus[i].getRemainingPinTime(),pIntf);
          inputVec.addElement("RemainingPingTime="+fileStatus[i].getRemainingPinTime());
      }
      if(fileStatus[i].getEstimatedWaitTime() != null) {
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),logger,silent);
          util.printMessage("\tEstimatedWaitTime="+
                fileStatus[i].getEstimatedWaitTime(),pIntf);
          inputVec.addElement("EstimatedWaitTime="+
				fileStatus[i].getEstimatedWaitTime());
      }
      if(fileStatus[i].getStatus()  != null) {
          util.printMessage("\tstatus="+
                fileStatus[i].getStatus().getStatusCode(),logger,silent);
          util.printMessage("\texplanation="+
             fileStatus[i].getStatus().getExplanation(),logger,silent);
          util.printMessage("\tstatus="+
                fileStatus[i].getStatus().getStatusCode(),pIntf);
          util.printMessage("\texplanation="+
             fileStatus[i].getStatus().getExplanation(),pIntf);
          inputVec.addElement("Status="+fileStatus[i].getStatus().getStatusCode());
          inputVec.addElement("Explanation="+fileStatus[i].getStatus().getExplanation());
      }

      if(fileStatus[i].getTransferProtocolInfo() != null) {
          ArrayOfTExtraInfo a_extraInfos = fileStatus[i].getTransferProtocolInfo();
          TExtraInfo[] extraInfos = a_extraInfos.getExtraInfoArray();
          if(extraInfos != null) {
            for(int j = 0; j < extraInfos.length; j++) {
              TExtraInfo extraInfo = extraInfos[j];
              util.printMessage("\tKey="+extraInfo.getKey(), logger, silent);
              util.printMessage("\tValue="+extraInfo.getValue(), logger, silent);
              util.printMessage("\tKey="+extraInfo.getKey(), pIntf);
              util.printMessage("\tValue="+extraInfo.getValue(), pIntf);
              inputVec.addElement("Key="+extraInfo.getKey());
              inputVec.addElement("Value="+extraInfo.getValue());
            }
          }
      }
      util.printEventLog(_theLogger,"SrmStatusOfGetRequestResponse",inputVec,silent,useLog);
    }
   }catch(Exception e) {
      System.out.println("SRM-CLIENT: SRMGetClient.doStatus.Exception="+
                e.getMessage());
      util.printEventLogException(_theLogger, 
                "SRMGetClient.doStatus.Exception", e);
                
   }
    return util.mapStatusCode(sCode);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkGetStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public TGetRequestFileStatus[] checkGetStatus
	(Vector keySURLS, String response, 
	 StringBuffer rCode, IsWholeRequestFailed wholeRequestFailed) 
		throws URI.MalformedURIException, java.rmi.RemoteException {

  URI[] uris = new URI[keySURLS.size()];
  for(int i = 0; i < keySURLS.size();i++) {
    uris[i] = (URI) keySURLS.elementAt(i);
  }

  Date statusDate = new Date();
  util.printMessage("SRM-CLIENT: " + statusDate +
        " Calling Status for " + response + " at " + new Date(),logger,silent);
  util.printMessageHL("SRM-CLIENT: " + statusDate +
        " Calling Status for " + response + " at " + new Date(),pIntf);


  inputVec.clear();
  if(_debug) {
   util.printMessage("\nSRM-CLIENT: :::::::::: Input parameters from srmStatusOfGetRequest :::", logger,silent); 
   util.printMessage("\nSRM-CLIENT: ...Input parameters from srmStatusOfGetRequest...", pIntf); 
   util.printMessage("SRM-CLIENT: RID="+response,logger,silent);
   util.printMessage("SRM-CLIENT: RID="+response,pIntf);
   util.printMessage("SRM-CLIENT: uid="+uid,logger,silent);
   util.printMessage("SRM-CLIENT: uid="+uid,pIntf);
   inputVec.addElement("RID="+response);
   for(int i = 0; i < keySURLS.size(); i++) {
    util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),logger,silent);
    util.printMessage("SRM-CLIENT: SourceSURL("+i+")="+(URI)keySURLS.elementAt(i),pIntf);
    inputVec.addElement("SourceSURL("+i+")="+(URI)keySURLS.elementAt(i));
   }
  }

  inputVec.clear();
  SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();

  r.setArrayOfSourceSURLs(SRMUtilClient.convertToArrayOfAnyURI(uris));
  if(response != null) {
  r.setRequestToken(response);
  }
  else {
     inputVec.addElement("SRM returned null request token for preparetogetrequest");
     util.printEventLog(_theLogger,"CheckGetStatus",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: SRM returned null request token", logger,silent);
     return null;
  }

  TGetRequestFileStatus[] fileStatus = null;

  try {
  inputVec.addElement("Status calling at="+new Date());
  statusDate = new Date();
  
  if(requestDate != null) {
  /*
  util.printMessage("SRM-CLIENT: Elapsed time from request is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", logger,silent);
  util.printMessage("SRM-CLIENT: Elapsed time from request is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", pIntf);
  inputVec.addElement("ElapsedTimeFromRequest="+util.getElapsedTime(requestDate,statusDate));
  */
  } 
  util.printEventLog(_theLogger,"SrmStatusOfGetRequest",inputVec,silent,useLog);

  /*
  if(firstTimeStatus) {
    util.printMessage("SRM-CLIENT: Waiting ...",logger,silent);
    util.printMessage("SRM-CLIENT: Waiting ...",pIntf);
    firstTimeStatus=false;
  }
  else {
  util.printMessageNL(".",logger,silent);
  }
  */

  SrmStatusOfGetRequestResponse result = null;

  result = (SrmStatusOfGetRequestResponse) 
                callSoapThread(r,result,"srmstatusofget");

  if(_debug) {
  util.printMessage("\nSRM-CLIENT:" + "### Output from SRM ###", logger,silent);
  }
  inputVec.clear();
  if(result == null) {
     util.printMessage("\nSRM-CLIENT: SRM returned null result for srmStatusOfGetRequest", logger,silent);
     util.printMessage("\nSRM-CLIENT: SRM returned null result for srmStatusOfGetRequest", pIntf);
     inputVec.addElement("SRM returnted null result for SrmStatusOfGetRequest");
     util.printEventLog(_theLogger,"SrmStatusOfGetRequest",inputVec,silent,useLog);
     return null;
  }

  if(result.getReturnStatus() == null) {
     util.printMessage
	  ("\nSRM-CLIENT: SRM returned null getReturnStatus() for srmStatusOfGetRequest", logger,silent);
     util.printMessage
	  ("\nSRM-CLIENT: SRM returned null getReturnStatus() for srmStatusOfGetRequest", pIntf);
     inputVec.addElement("SRM returnted null getReturnStatus() for srmStatusOfGetRequest");
     util.printEventLog(_theLogger,"SrmStatusOfGetRequest",inputVec,silent,useLog);
     return null;
  }

  inputVec.clear();
    inputVec.addElement("Request.Status from SRM (srmStatusOfGetRequest)="+
	   result.getReturnStatus().getStatusCode());

   util.printMessage("\nRequest.status="+result.getReturnStatus().getStatusCode(), logger,silent);
   util.printMessage("Request.status="+result.getReturnStatus().getStatusCode(), pIntf);

   rStatus = result.getReturnStatus();

   if(result.getReturnStatus().getExplanation() != null) { 
     util.printMessage("explanation="+result.getReturnStatus().getExplanation(), logger,silent);
     util.printMessage("explanation="+result.getReturnStatus().getExplanation(), pIntf);
     inputVec.addElement("explanation="+ result.getReturnStatus().getExplanation());
   }
   util.printEventLog(_theLogger,"SrmStatusOfGetRequest",inputVec,silent,useLog);

  rCode.append(result.getReturnStatus().getStatusCode().toString());
  if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {
    wholeRequestFailed.setWholeRequestFailed(true);
  }

 
  if(result.getArrayOfFileStatuses() != null) {
    fileStatus = result.getArrayOfFileStatuses().getStatusArray();
  }

  if(fileStatus != null) {
    for(int kk = 0; kk <fileStatus.length; kk++) {
      TGetRequestFileStatus fStatus = fileStatus[kk];
      if(fStatus.getFileSize() != null) {
        util.printMessage("\tFileSize="+fStatus.getFileSize(),pIntf);
      }
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

  //System.out.println(">>>StatusWaitTime Before="+statusWaitTime);
  statusWaitTime = statusWaitTime*2;
  if(statusWaitTime >= threshHold*1000) {
    statusWaitTime = threshHold*1000; 
        //resetting back to threshHoldValue seconds.
  }
  if(statusWaitTime >= statusMaxTimeAllowed * 1000) {
     if(_debug) {
       System.out.println("\nSRM-CLIENT: Next status call is " +
			"adjusted according to totalrequesttime given by the user");
     }
     statusWaitTime = statusWaitTime - 5000;
  }
  //System.out.println(">>>StatusWaitTime After="+statusWaitTime);

  }catch(Exception me) {
       util.printEventLogException(_theLogger,"PerformTranfer",me);
    int idx = me.getMessage().indexOf("No scheme found in URI");
    int idx1 = me.getMessage().indexOf("Cannot initialize URI with empty parameters");
    if(idx != -1 || idx1 != -1) {
      System.out.println("\nignoring this exception ...");
      rCode.append("SRM_REQUEST_QUEUED");
    }
    else {
      inputVec.clear();
      inputVec.addElement("Exception="+me.getMessage());
      throw new java.rmi.RemoteException(me.getMessage());
    }
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
   return SRMUtilClient.reserveSpace
       (_srm,strUID,userDesc, retentionPolicyInfo,
        tokenSize, tokenLifetime, guarnSize,
        new StringBuffer(), logger,_debug,false,
        sCode,statusBuf,_credential,proxyType,serverUrl,
        _theLogger, pIntf, _srmClientIntf, silent, useLog, 
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
    util.printMessage(".................................",logger,silent);
    util.printMessage("Updating Token="+token,logger,silent);
			
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
			
    util.printMessage("Total= "+newSizeT.getValue(),logger,silent);
    util.printMessage("Min= "+newSizeG.getValue(),logger,silent);
    util.printMessage("LifeTime="+spaceLifetime.getValue(),logger,silent);


    SrmUpdateSpaceResponse result1 = null;

    result1 = (SrmUpdateSpaceResponse) 
                callSoapThread(r1,result1,"srmupdatespace");
			
    if (result1 != null) {

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
        //this is needed, otherwise, it will grab the default credential
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
