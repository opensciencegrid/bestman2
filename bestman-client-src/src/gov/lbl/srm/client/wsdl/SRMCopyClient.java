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
import gov.lbl.srm.client.transfer.globus.*;
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
//SRMCopyClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMCopyClient implements SRMWSDLIntf {
  private Stub _stub;
  private ISRM _srm;
  //private ServiceContext _context; 
  private SRMClientIntf _srmClientIntf; 
  private GSSCredential _credential;
  private boolean _debug;
  private Date requestDate;
  private Log logger;
  private String fileToken;
  private String uid="";
  private String userDesc="";
  private String storageInfo="";
  private String remoteTransferInfo="";
  private String protocolsList="";
  private TReturnStatus rStatus = null;
  //private char tokenType;
  private int tokenLifetime;
  private long tokenSize;
  private long guarnSize;
  private boolean overwrite;
  private boolean doReserveSpace;
  private boolean doReleaseSpace;
  private Vector fileInfo = new Vector ();
  private String statusToken;
  private String serverUrl;
  private boolean silent;
  private boolean useLog;
  private boolean pushMode;
  private boolean partycopy;
  private int partytype = 0;
  private SRMWSDLIntf srmCopyClient;
  private SRMWSDLIntf srmGetTypeClient;
  private TFileStorageType fileStorageType;
  private TRetentionPolicyInfo retentionPolicyInfo;
  private int totalRequestTime;
  private int pinLifeTime;
  private int fileLifeTime;
  private int parallelism;

  private PrintIntf pIntf;
  private int bufferSize;
  private boolean dcau=true;
  private boolean submitOnly=false;
  //private static int statusWaitTime=30;
  private static int statusWaitTime=10; //changed for exponential polling
  private static int threshHold = 600;
  //private static int statusMaxTimeAllowed=600; //in seconds
  //private static int statusMaxTimeAllowed=-1; //in seconds
  private static int statusMaxTimeAllowed=10800; //in seconds
  private boolean statusWaitTimeGiven=false;
  private java.util.logging.Logger _theLogger;
  private Vector inputVec = new Vector();
  private int proxyType;
  private String resultStatus="";
  private String resultExplanation="";
  private TimeOutCallBack timeOutCallBack;
  private SoapCallThread soapCallThread;
  private int connectionTimeOutAllowed=1800;
  private int setHTTPConnectionTimeOutAllowed=600;
  private boolean recursive=false;
  private int numLevels = 0; //default
  private String delegation="";
  private String requestToken="";
  private boolean noAbortFile;
  private boolean doMkdir=false;
  private int concurrency;
  private int numRetry;
  private int retryTimeOut;
  private boolean checkPing;
  private boolean gateWayModeEnabled;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMCopyClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMCopyClient (SRMClientIntf srmClientIntf, 
	String serverUrl, String uid, Vector fileInfo, 
    String fileToken, String statusToken, 
    boolean pushMode, boolean partycopy, boolean overwrite,
    boolean doReserveSpace, boolean doReleaseSpace,
        int tokenlifetime, long tokensize, long guarnSize,
		GSSCredential credential, 
		Request requestType, 
        TFileStorageType fileStorageType, TRetentionPolicyInfo retentionPolicyInfo,
	    int totalRequestTime, int pinLifeTime, int fileLifeTime,
        int parallelism, int bufferSize, boolean dcau,
        int sMaxTimeAllowed, int statusWaitTime,
        String protocolsList,
        boolean statusWaitTimeGiven, String storageInfo,
        String remoteTransferInfo,
	    String userDesc, java.util.logging.Logger theLogger, Log logger, 
        PrintIntf pIntf, boolean debug, 
		boolean silent, boolean useLog, boolean submitOnly, 
	    int proxyType, int connectionTimeOutAllowed, 
        int setHTTPConnectionTimeOutAllowed,
	    boolean recursive, int numLevels, 
	    String delegation, boolean noAbortFile, 
	    boolean doMkdir, int threshHold, int numRetry, int retryTimeOut,
            boolean checkPing) throws Exception 
{
	//_context = new ServiceContext ();
    _srmClientIntf = srmClientIntf;
    _credential = credential;
    this.fileToken = fileToken;
    this.statusToken = statusToken;
    this.userDesc = userDesc;
    this._theLogger = theLogger;
    this.logger = logger;
    this.pIntf = pIntf;
    this.numRetry = numRetry;
    this.retryTimeOut = retryTimeOut;
    this.overwrite = overwrite;
    this.threshHold = threshHold;
    _debug = debug;
    this.silent = silent;
    this.useLog=useLog;
    this.pushMode = pushMode;
    this.partycopy = partycopy;
    this.storageInfo = storageInfo;
    this.remoteTransferInfo = remoteTransferInfo;
    this.fileStorageType = fileStorageType;
    this.retentionPolicyInfo = retentionPolicyInfo;
    this.pinLifeTime = pinLifeTime;
    this.protocolsList = protocolsList;
    this.fileLifeTime = fileLifeTime;
    this.totalRequestTime = totalRequestTime;
    this.parallelism = parallelism;
    this.bufferSize = bufferSize;
    this.submitOnly = submitOnly;
    this.dcau = dcau;
    this.proxyType = proxyType;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.setHTTPConnectionTimeOutAllowed = setHTTPConnectionTimeOutAllowed;
    this.recursive = recursive;
    this.numLevels = numLevels;
    this.delegation = delegation;
    this.noAbortFile = noAbortFile;
    this.doMkdir = doMkdir;

    if(sMaxTimeAllowed != 10800) {
      this.statusMaxTimeAllowed = sMaxTimeAllowed;
    }

    this.statusWaitTimeGiven = statusWaitTimeGiven;

    if(statusWaitTime < 1000) {
      this.statusWaitTime = statusWaitTime*1000;
    }
    else {
      this.statusWaitTime = statusWaitTime;
    }

    this.uid = uid;
    this.serverUrl = serverUrl;
    this.fileInfo = fileInfo;
    this.doReserveSpace = doReserveSpace;
    this.doReleaseSpace = doReleaseSpace;
    this.tokenLifetime = tokenlifetime;
    this.tokenSize = tokensize;
    this.guarnSize = guarnSize;
    this.checkPing = checkPing;

    
    if(!partycopy) {

      String[] args = new String[2];

      args[0] = serverUrl;
      args[1] = uid;

      inputVec.clear();
      inputVec.addElement("ServerUrl="+serverUrl);
      inputVec.addElement("AuthId="+uid);

      util.printMessage("SRM-CLIENT: " + new Date() + 
		" Connecting to " + serverUrl,logger,silent);
      util.printMessageHL("SRM-CLIENT: " + new Date() + 
		" Connecting to " + serverUrl,pIntf);

      if(!uid.equals("")) {
       if(_debug) {
        util.printMessage("SRM-CLIENT: AuthId=" + uid,logger,silent);
        util.printMessage("SRM-CLIENT: AuthId=" + uid,pIntf);
       }
      }
      util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);

      // checking if httpg is handled.
      // if not java.net.URL returns exception on httpg.
      
      openSRMConnection();

      inputVec.clear();
      inputVec.addElement("SRM="+_srm);
      inputVec.addElement(" uid="+uid + " debug="+_debug + " proxyType="+proxyType);
      inputVec.addElement(" credendtial="+credential);
      inputVec.addElement(" logger="+logger + "pIntf="+pIntf);
      inputVec.addElement(" srmClientIntf="+_srmClientIntf);
      inputVec.addElement(" silent="+silent + " useLog="+useLog);
      inputVec.addElement(" connectionTimeOutAllowed="+connectionTimeOutAllowed);
      inputVec.addElement(" setHTTPConnectionTimeOutAllowed"+
		setHTTPConnectionTimeOutAllowed);
      inputVec.addElement(" delegation="+delegation);
      inputVec.addElement(" numRetry="+numRetry+ " retryTimeOut=" +retryTimeOut);
      inputVec.addElement(" checkPing="+checkPing);
      util.printEventLog(_theLogger,"SrmCopy.Initialization",
			inputVec,silent,useLog);

      if(checkPing) {
        util.printMessage("SRM-CLIENT: Calling isServerGateWayEnabled",
		logger,silent);
        gateWayModeEnabled=SRMUtilClient.isServerGateWayEnabled(_srm, uid,
                        logger,  _debug, credential,
                        proxyType, serverUrl,  _theLogger,
                        pIntf,  _srmClientIntf, silent,
                        useLog, connectionTimeOutAllowed,
                        setHTTPConnectionTimeOutAllowed,
                        delegation, numRetry, retryTimeOut);
        inputVec.clear();
        inputVec.addElement(" gateWayModeEnabled="+gateWayModeEnabled);
        util.printEventLog(_theLogger,"SrmCopy.Initialization",
		inputVec,silent,useLog);
        System.out.println("SRM-CLIENT: GateWayModeEnabled="+gateWayModeEnabled);
        _srmClientIntf.setGateWayModeEnabled(gateWayModeEnabled);
    }
    } 
    else {
      requestType.setModeType("3partycopy");
      _srmClientIntf.setRequest(requestType);
      inputVec.clear();
      inputVec.addElement("Changing modetype to " + requestType.getModeType());
      util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: Changing modetype to " + 
		requestType.getModeType(), logger,silent);
      util.printMessage("\nSRM-CLIENT: Changing modetype to " + 
		requestType.getModeType(), pIntf);
      int size = fileInfo.size();  
      FileInfo fInfo = (FileInfo) fileInfo.elementAt(0);
      String surl = fInfo.getSURL();
      String turl = fInfo.getTURL();
      //type=0, srmCopy(srm://,gsiftp://) and srmCopy(srm:://,srm://)
      if(surl.startsWith("gsiftp://") && turl.startsWith("srm://")) {
         this.partytype = 1;
      }
      else if(surl.startsWith("gsiftp://") && turl.startsWith("gsiftp://")) {
         this.partytype = 2;
      }
      inputVec.clear();
      inputVec.addElement("PartyType="+partytype);
      util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
      if(_debug) {
        util.printMessage("\nSRM-CLIENT: Thirdparty type " + 
                partytype, logger,silent);
      }
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// openSRMConnection
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void openSRMConnection () throws Exception {

      SimpleProvider provider = new SimpleProvider ();
      SimpleTargetedChain c = null;
      URL uu = null;
      MyGlobusURL gurl = new MyGlobusURL(serverUrl,0);
      String host = gurl.getHost();
      String path = gurl.getPath();
      int port = gurl.getPort();
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
        inputVec.addElement("remote srm object is null");
        util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
        throw new Exception("\nremote srm object is null");
      }
      else {
         inputVec.clear();
         inputVec.addElement("got remote srm object");
         util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
      }
      _srm = srm;

      org.apache.axis.client.Stub srm_stub =
               (org.apache.axis.client.Stub) _srm;
      srm_stub.setTimeout(setHTTPConnectionTimeOutAllowed*1000);
      if(delegation.equalsIgnoreCase("false")) {
          if(_debug) {
           util.printMessage("SRM-CLIENT: no delegation by client",
                    logger,silent);
          }
      }
      else if(delegation.equalsIgnoreCase("true")) {
         setSecurityPropertiesForcefully(uu);
      }
      else {
         setSecurityProperties(uu);
      }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setConcurrency
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//public void setConcurrency(int conc) {
  //this.concurrency = conc;
//}

public void setThirdPartyType (int thirdPartyType) { 
  partytype = thirdPartyType;
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
       inputVec.clear();
       inputVec.addElement
	 ("SetRequestDone is set with true and false");
       util.printEventLog(_theLogger,"srmCopy.peformTransfer", 
		inputVec,silent,useLog);
      return;
    }
    if(partycopy) {
       if(partytype == 0) {
         //this part will execute for srmCopy(srm://, srm://) and
         //srmCopy(srm://, gsiftp://) it differenciates in srmGetClient
         SRMWSDLIntf srmCopyClient = new SRMGetClient (_srmClientIntf, 
	                    serverUrl, uid, fileInfo, 
                            fileToken, statusToken, 
                            doReserveSpace,partycopy, overwrite,
                            tokenLifetime, tokenSize, guarnSize,
		             _credential, fileStorageType, retentionPolicyInfo,
			    totalRequestTime,pinLifeTime, 0,false, 
                            parallelism, bufferSize, dcau,  false,
			    statusMaxTimeAllowed, statusWaitTime,
                            protocolsList,
                            //statusWaitTimeGiven,
			    true, storageInfo, remoteTransferInfo,
                            userDesc,_theLogger, logger, pIntf, 
                            _debug, silent,useLog,false,proxyType,
			    connectionTimeOutAllowed,
                            setHTTPConnectionTimeOutAllowed,delegation,
			    noAbortFile,doMkdir,false,threshHold,
                            numRetry,retryTimeOut,checkPing); 

         this.srmCopyClient = srmCopyClient; 
         setGetTypeWSDLIntf(srmCopyClient);
         srmCopyClient.performTransfer(false);
       }
       else if (partytype == 1) {
         int size = fileInfo.size();
         boolean callSetDone = true;
         Vector vecFiles = new Vector();
         for(int i = 0; i < size; i++) {
           FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
           String surl = fInfo.getTURL();
           if(i == 0) {
              serverUrl = 
                   gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
               util.printMessage("\nSRM-CLIENT: Connecting to serviceUrl:"+
                  serverUrl, logger,silent);
               util.printMessage("\nSRM-CLIENT: Connecting to serviceUrl:"+
                  serverUrl, pIntf);
           }
           //no need to set the file sizes also
           //because for 3partycopy gsiftp->srm, we set filesizes in
           //doFirstTransfer and doNextTransfer, which is easy, because
           //we already opened connection and just grabbing filesize
           vecFiles.addElement(fInfo);
         }
         inputVec.clear();
         inputVec.addElement("TotalSize="+vecFiles.size());
         util.printEventLog(_theLogger,"partycopy.filesize",
                inputVec,silent,useLog);
         SRMWSDLIntf srmCopyClient = new SRMPutClient (_srmClientIntf, 
                        serverUrl, uid, vecFiles, 
                        fileToken, statusToken, 
                        doReserveSpace, overwrite,
                        tokenLifetime, tokenSize, guarnSize,
                        _credential, 
                        fileStorageType,retentionPolicyInfo,
			totalRequestTime,pinLifeTime,fileLifeTime, 
		        callSetDone, size, true, 
                        parallelism, bufferSize, dcau, false,
                        statusMaxTimeAllowed, statusWaitTime,
                        protocolsList,
                        //statusWaitTimeGiven, 
                        true, storageInfo, remoteTransferInfo,
		        userDesc, _theLogger, logger, pIntf,
		        _debug, silent,useLog,false,proxyType,false,
			connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
                        delegation, noAbortFile,doMkdir,false,
                        threshHold,false,numRetry,retryTimeOut,checkPing); 
           this.srmCopyClient = srmCopyClient;
           srmCopyClient.setThirdPartyType(partytype);
           srmCopyClient.performTransfer(false);
       }
       else if(partytype == 2) {
         int size = fileInfo.size();
         int failedFiles = 0;
         for(int i = 0; i < size; i++) {
           FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
           try {
             _srmClientIntf.initiatePullingFile(fInfo);
           }catch(Exception e) { 
              util.printEventLogException(_theLogger,"",e);
              fInfo.setFailed(true);
              fInfo.setStatusLabel("Failed");
              fInfo.setErrorMessage(e.getMessage());
              failedFiles++;
              inputVec.clear();
              inputVec.addElement("Exception occured during " +
			   "getSourceFileSize for file " + fInfo.getSURL()+ " " + 
		        e.getMessage());
              util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
              util.printMessage
			("\nSRM-CLIENT: Exception occured during " +
		     "getSourceFileSize for file " + fInfo.getSURL() + " " + 
					  e.getMessage(), logger, silent);
              util.printMessage
			("\nSRM-CLIENT: Exception occured during " +
		     "getSourceFileSize for file " + fInfo.getSURL() + " " + 
					  e.getMessage(), pIntf);
           }
           //util.printMessage("\nSRM-CLIENT: " + new Date() + 
				//" end file transfer", logger,silent);
         }//end for
         if(size == failedFiles) {
           _srmClientIntf.setRequestDone(true,true);
           inputVec.clear();
           inputVec.addElement
	     ("SetRequestDone is set with true and true");
           util.printEventLog(_theLogger,"srmCopy.thirdpartycopy==2", 
		inputVec,silent,useLog);
         }
         else {
           inputVec.clear();
           inputVec.addElement
	     ("SetRequestDone is set with true and false");
           util.printEventLog(_theLogger,"srmCopy.thirdpartycopy==2", 
		inputVec,silent,useLog);
           _srmClientIntf.setRequestDone(true,false);
         }
       }//else if (partycopy == 2)
    }
    else {
      try {
        srmCopy(uid, fileInfo);
      }
      catch(Exception e) {
              util.printEventLogException(_theLogger,"",e);
       String msg = e.getMessage();
       int idx = msg.indexOf("Connection refused");
       int idx1 = msg.indexOf("Defective credential detected");
       int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
       int idx6 = msg.indexOf(
			"java.net.SocketTimeoutException: Read timed out");
       util.printMessage("\nSRM-CLIENT: Exception " + msg, logger,silent);
       util.printMessage("\nSRM-CLIENT: Exception " + msg, pIntf);
       util.printHException(e,pIntf);
       if(pIntf != null) {
         pIntf.setCompleted(false);
       }
       if(msg.startsWith("CGSI-gSOAP: Could not find mapping") 
			|| idx != -1 || idx1 != -1 || idx5 != -1 || idx6 != -1) {
         util.printMessage("\nSRM-CLIENT: Exception : "+msg,logger,silent);
         util.printMessage("\nSRM-CLIENT: Exception : "+msg,pIntf);
         inputVec.clear();
         inputVec.addElement("Exception : "+msg);
         util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
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
  srmGetTypeClient = getTypeWSDLIntf;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getGetTypeWSDLIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//
public SRMWSDLIntf getGetTypeWSDLIntf () {
  return srmGetTypeClient;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatusEnquiry
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doStatusEnquiry() throws Exception {
  util.printMessage("SRM-CLIENT: " + new Date() + 
		" Calling SrmStatusOfCopyRequst ...", logger, silent);
  util.printMessageHL("SRM-CLIENT: " + new Date() + 
		" Calling SrmStatusOfCopyRequst ...", pIntf);
          
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  util.printEventLog(_theLogger,"SrmStatusOfCopyRequst",inputVec,silent,useLog);
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

  if(doReserveSpace) {
    if(fileToken != null) {
      inputVec.clear();
      util.printMessage("SRM-CLIENT: " + new Date() + 
		" Calling releaseSpace ...", logger, silent);
      util.printMessageHL("SRM-CLIENT: " + new Date() + 
		" Calling releaseSpace ...", pIntf);
      inputVec.addElement("TimeStamp="+new Date());
      util.printEventLog(_theLogger,"releaseSpace",inputVec,silent,useLog);
      releaseSpace(fileToken,true);  
    }
    else {
      inputVec.clear();
      inputVec.addElement("cannot release space file token is null");
      util.printEventLog(_theLogger,"releaseSpace",inputVec,silent,useLog);
      util.printMessage("SRM-CLIENT: cannot release space file token is null", logger,silent);
      util.printMessage("SRM-CLIENT: cannot release space file token is null", 
		pIntf);
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// putDone
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) throws Exception {

  if(gateWayModeEnabled) return ;

  //calling the srmPut's srmCopyClient.putDone
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());

  if(_debug) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling putDone(srmCopyClient)", logger,silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling putDone(srmCopyClient)", pIntf);
    util.printMessage("SRM-CLIENT: siteUrl=" + siteUrl, logger,silent);
    util.printMessage("SRM-CLIENT: rid=" + rid, logger,silent);
    util.printMessage("SRM-CLIENT: label=" + label, logger,silent);
    util.printMessage("SRM-CLIENT: siteUrl=" + siteUrl, pIntf);
    util.printMessage("SRM-CLIENT: rid=" + rid, pIntf);
    util.printMessage("SRM-CLIENT: label=" + label, pIntf);
    inputVec.addElement("siteUrl=" + siteUrl);
    inputVec.addElement("rid=" + rid);
    inputVec.addElement("label=" + label);
  }
  srmCopyClient.putDone(siteUrl, rid, label);
  util.printEventLog(_theLogger,"putDone",inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFiles
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles(String siteUrl, String rid,int label) throws Exception {

  if(gateWayModeEnabled) return ;

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
  inputVec.addElement("TimeStamp="+new Date());
  if(_debug) {
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling abortFiles (srmCopyClient).surl=" + siteUrl, logger,silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
		" Calling abortFiles (srmCopyClient).surl=" + siteUrl, pIntf);
    inputVec.addElement("SURL=" + siteUrl);
  }
  srmCopyClient.abortFiles(siteUrl, rid,label);
  util.printEventLog(_theLogger,"abortFiles",inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
// used for 3partycopy transfers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String releaseFile(String siteUrl,String rid,int label) 
        throws Exception {

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  //calling the srmPut's srmCopyClient.releaseFile
  //util.printMessage("TimeStamp="+new Date(), logger,silent);
  String code = null;
  inputVec.clear();
  inputVec.addElement("TimeStamp="+new Date());
  if(_debug) {
    util.printMessage ("\nSRM-CLIENT: " + new Date() + 
			" Calling releaseFile (srmCopyClient)", logger,silent);
    util.printMessage ("SRM-CLIENT: siteUrl="+siteUrl, logger,silent);
    util.printMessage ("SRM-CLIENT: rid="+rid, logger,silent);
    util.printMessageHL("\nSRM-CLIENT: " + new Date() + 
			" Calling releaseFile (srmCopyClient)", pIntf);
    util.printMessage ("SRM-CLIENT: siteUrl="+siteUrl, pIntf);
    util.printMessage ("SRM-CLIENT: rid="+rid, pIntf);
    util.printMessage ("SRM-CLIENT: label="+label, pIntf);
    inputVec.addElement("SRM-CLIENT: SURL=" + siteUrl);
  }
  code = srmCopyClient.releaseFile(siteUrl,rid,label);
  util.printEventLog(_theLogger,"releaseFile",inputVec,silent,useLog);
  return code;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void srmCopy(String uid, Vector fileInfo)  
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
   else if(totalSize > maxSize) {
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
   long startTime = System.currentTimeMillis();

   inputVec.clear();

   if(totalSize > 1) {
     inputVec.addElement("Total number of subrequests="+totalSubRequest);
     if(_debug) {
       util.printMessage("\nSRM-CLIENT: Total number of subrequests=" + 	
			totalSubRequest, logger,silent);
     }
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }

   Date d = new Date();
   requestDate = d;
   if(pushMode) {
   util.printMessage("\nSRM-CLIENT: " + requestDate + 
		" Calling srmCopy now in Push Mode", logger,silent);
   util.printMessageHL("\nSRM-CLIENT: " + requestDate + 
		" Calling srmCopy now in Push Mode", pIntf);
   }
   else {
   util.printMessage("\nSRM-CLIENT: " + requestDate + 
		" Calling srmCopy now in Pull Mode", logger,silent);
   util.printMessageHL("\nSRM-CLIENT: " + requestDate + 
		" Calling srmCopy now in Pull Mode", pIntf);
   }

   if(_debug) {
     util.printMessage("SRM-CLIENT: ::::::::::::::Input parameters ::::::", logger,silent);
     util.printMessage("SRM-CLIENT: ...Input parameters...", pIntf);
   }

   while (count < totalSubRequest) {
     if(count == (totalSubRequest-1)) {
        if(modSize > 0) {
          maxSize = modSize;
        }
     }

     String[] surl = new String[maxSize];
     String[] turl = new String[maxSize];
     boolean[] overwritefiles = new boolean[maxSize];
     int sindex = 0;

     for(int i =  index+0; i < index+maxSize; i++) {
        inputVec.clear();
        FileInfo fInfo = (FileInfo) fileInfo.elementAt(i);
        turl[sindex] = fInfo.getTURL();
        surl[sindex]=fInfo.getSURL();
        overwritefiles[sindex] = fInfo.getOverwriteFiles();
        fInfo.setOrigTURL(turl[sindex]);
        fInfo.setOrigSURL(surl[sindex]);
        if(_debug) {
         util.printMessage("SRM-CLIENT: SourceSURL("+sindex+")="+ 
			surl[sindex], logger,silent);
         util.printMessage("SRM-CLIENT: SourceSURL("+sindex+")="+ 
			surl[sindex], pIntf);
         util.printMessage("SRM-CLIENT: TargetSURL("+sindex+")="+ 
			turl[sindex], logger,silent);
         util.printMessage("SRM-CLIENT: TargetSURL("+sindex+")="+ turl[sindex], pIntf);
        }
        inputVec.addElement("SourceSURL("+sindex+")="+ surl[sindex]);
        inputVec.addElement("TargetSURL("+sindex+")="+ turl[sindex]);
        util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
        sindex++;
     }
   
     if (doReserveSpace) {
        fileToken  = reserveSpace(uid);
        if(fileToken == null) {
          inputVec.clear();
          inputVec.addElement ("\nSRM returns null space token");
          if(_debug) {
          util.printMessage
            ("\nSRM-CLIENT: SRM returns null space token", logger,silent);
          util.printMessage
            ("\nSRM-CLIENT: SRM returns null space token", pIntf);
          }
          util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
          if(pIntf == null) {
             inputVec.clear();
             inputVec.addElement("ExitStatus="+1000);
             util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
          System.exit(100);
          }
        }
     }


     SrmCopyResponse response = copyMultiFiles(surl, turl,
          overwritefiles, fileToken);

     StringBuffer responseBuffer = new StringBuffer();
     HashMap subStatusArray = printCopyResult(response,responseBuffer,oResult);
     StringBuffer expBuffer = new StringBuffer();

     if(response == null || subStatusArray == null || subStatusArray.size() == 0) {
       if(!responseBuffer.toString().equals("")) {
         inputVec.clear();
         inputVec.addElement("Explanation="+responseBuffer.toString());
         util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
         if(_debug) { 
           util.printMessage("\tExplanation="+responseBuffer.toString(), 
		logger,silent);
           util.printMessage("\tExplanation="+responseBuffer.toString(), 
		pIntf);
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
      inputVec.clear();
      inputVec.addElement("Null response from SRM for this sub request");
      if(_debug) {
      util.printMessage("\nSRM-CLIENT: Null response from SRM " +
	"for this sub request", logger, silent);
      util.printMessage("\nSRM-CLIENT: Null response from SRM " + 
	"for this sub request", pIntf);
      }
      util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
    }


    totalFiles = totalFiles + subStatusArray.size();
    _srmClientIntf.setTotalFiles(totalFiles);

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
      String subResponse = (String) ids;
      StringBuffer rCode = new StringBuffer();
      String subKey="";
      String oSubKey="";

        //trying to collect all the surls and turls for REQUEST_QUEUED files.
        Vector keySURLS = new Vector();
        Vector TURLS = new Vector();
        long sTimeStamp = util.startTimeStamp();
        while(subStatusArray.size() > 0) {
          keySURLS.clear();
          TURLS.clear();
          Object[] keySetArray = subStatusArray.keySet().toArray();
          Object[] oldKeySetArray = oResult.keySet().toArray();
          rCode.delete(0,rCode.length());
          expBuffer.delete(0,expBuffer.length());

          for(int k = 0; k < keySetArray.length; k++) {
          oSubKey = (String) keySetArray [k];
          expBuffer.delete(0,expBuffer.length());
          //subKey = (String) oldKeySetArray [k];
          subKey = (String) oResult.get(oSubKey); 
          //System.out.println(">>>oSubKey="+oSubKey);
          //System.out.println(">>>subKey="+subKey);
          TCopyRequestFileStatus status =
            (TCopyRequestFileStatus)(subStatusArray.get(oSubKey));
          TStatusCode code = status.getStatus().getStatusCode();
          if(timedOutCase) {
            subStatusArray.remove(oSubKey);
            /*
            util.printMessage("\nSRM-CLIENT: Calling SrmAbortRequest",
                logger,silent);
             SRMUtilClient utilClient = new SRMUtilClient
               (serverUrl,uid,userDesc, _credential, _theLogger, logger,
                pIntf, _debug,silent,useLog,false, false,
                statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
                connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,"",numRetry,retryTimeOut);
             TStatusCode abortRequestCode =
                utilClient.doSrmAbortRequest(requestToken);
             util.printMessage("\nSRM-CLIENT: AbortStatusCode="+
                abortRequestCode.getValue(),logger,silent);
             */
          }
          else {
           if(code == TStatusCode.SRM_REQUEST_QUEUED ||
              code == TStatusCode.SRM_FILE_IN_CACHE ||
              code == TStatusCode.SRM_REQUEST_INPROGRESS) {
             keySURLS.addElement(new URI(subKey));
             TURLS.addElement((URI)status.getTargetSURL());
           }
           else if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
                    (code != TStatusCode.SRM_REQUEST_QUEUED)) {
              Object[] objArray = fileInfo.toArray();
              FileInfo ffInfo = null;
              if(status != null && status.getSourceSURL() != null) {
                String gURL = status.getSourceSURL().toString();
                for(int kk = 0; kk < objArray.length; kk++) {
                  ffInfo = (FileInfo) objArray[kk];
                  if(ffInfo.getOrigSURL().equals(gURL)) {
                    ffInfo.setRID(subResponse);
                    if(!ffInfo.getCompleted() && !ffInfo.getFailed()) {
                       break;
                    }
                  }
                }
              }
              if(code == TStatusCode.SRM_FILE_PINNED ||
                 code == TStatusCode.SRM_SUCCESS) {
                if(status != null && status.getSourceSURL() != null) {
                  inputVec.clear();
                  inputVec.addElement("SourceSURL="+status.getSourceSURL().toString());
                  inputVec.addElement("TargetSURL="+status.getTargetSURL().toString());
                  util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
                  if(_debug) {
                  util.printMessage("SRM-CLIENT: SourceSURL=" +
					status.getSourceSURL().toString(),logger,silent);
                  util.printMessage("SRM-CLIENT: TargetSURL=" +
					status.getTargetSURL().toString(),logger,silent);
                  util.printMessage("SRM-CLIENT: SourceSURL=" +
					status.getSourceSURL().toString(),pIntf);
                  util.printMessage("SRM-CLIENT: TargetSURL=" +
					status.getTargetSURL().toString(),pIntf);
                  }
                }
                else {
                  util.printMessage("SRM-CLIENT: SourceSURL=" + null,logger,silent);
                  util.printMessage("SRM-CLIENT: TargetSURL=" + null,logger,silent);
                  util.printMessage("SRM-CLIENT: SourceSURL=" + null,pIntf);
                  util.printMessage("SRM-CLIENT: TargetSURL=" + null,pIntf);
                  inputVec.clear();
                  inputVec.addElement("SourceSURL=null");
                  inputVec.addElement("TargetSURL=null");
                  util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
                }
                TReturnStatus rs1 = new TReturnStatus();
                rs1.setStatusCode(code);
                if(status != null) {
                  status.setStatus(rs1);
                }
                if(ffInfo  != null) {
                  ffInfo.setRID(subResponse);
                  ffInfo.setFileStatus(code.toString());
                  if(status.getStatus().getExplanation() != null) {
                    ffInfo.setFileExplanation(status.getStatus().getExplanation());
                  }
                  else {
                    ffInfo.setFileExplanation("");
                  }
                  if(status != null && status.getFileSize() != null) {
                    String eSize = ""+status.getFileSize();
                    ffInfo.setExpectedSize(eSize);
                    ffInfo.setActualSize(eSize);
                   }
                   ffInfo.setCompleted(true);
                   long endTime = System.currentTimeMillis();
                   ffInfo.setTimeTaken(""+(endTime-startTime));
                   ffInfo.setStatusLabel("Done");
                }
                subStatusArray.remove(oSubKey);
                //System.out.println(">>>oSubKey="+oSubKey);
                //System.out.println(">>>subStatusArray="+subStatusArray);
              }
              /*
              else if(code == TStatusCode.SRM_FILE_IN_CACHE) {
                String explanation="code=SRM_FILE_IN_CACHE,";
                if(status.getStatus().getExplanation() != null) {
                  explanation = explanation+" " + 
						status.getStatus().getExplanation();
                }
                inputVec.clear();
                inputVec.addElement(explanation);
                util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
                util.printMessage
					("SRM-CLIENT: " + new Date() + " " + explanation, logger,silent);
                util.printMessage
					("SRM-CLIENT: " + new Date() + " " + explanation,pIntf);
                if(ffInfo != null) {
                  ffInfo.setFileStatus(code.toString());
                  if(status.getStatus().getExplanation() != null) {
                    ffInfo.setFileExplanation(explanation);
                  }
                  else {
                    ffInfo.setFileExplanation("");
                  }
                }
              }
              */
              else {
               /*
               if(_debug) {
                 System.out.println("\nSourceSURL="+subKey.toString());
                 System.out.println("FileStatusCode="+code);
               }  
               */
               if(code == null) {
                   code = SRMUtilClient.mapReturnStatusValueBackToCode
			    (_debug,rCode,logger,_theLogger);
                }
                TReturnStatus rs = new TReturnStatus();
                if(code != null) {
                  rs.setStatusCode(code);
                }

                if(ffInfo != null) {
                  long endTime = System.currentTimeMillis();
                  ffInfo.setTimeTaken(""+(endTime-startTime));
                  ffInfo.setFileStatus(code.toString());
                  if(status.getStatus().getExplanation() != null) {
                    ffInfo.setFileExplanation(status.getStatus().getExplanation());
                  }
                  else {
                    ffInfo.setFileExplanation("");
                  }
                }
                if(status != null) {
                  printErrorResponse(code,status,ffInfo);
                }
                subStatusArray.remove(oSubKey);
             }
             if(doReleaseSpace) {
               if(fileToken != null) {
                if(_debug) {
                  getSpaceTokenMeta(fileToken);
                }
                boolean keepSpace = false;
                if(ffInfo != null && status != null) {
                 releaseFile(keepSpace,
                   status.getSourceSURL().toString(), ffInfo.getRID(),-1);
                }
                if(_debug) {
                  getSpaceTokenMeta(fileToken);
                }
              }
             }
           }
        }
        }//end for

       /*
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
       */

        keySetArray = subStatusArray.keySet().toArray();

        //System.out.println(">>keySetArray.length="+keySetArray.length);
        if(keySetArray.length > 0 && 
		!util.isRetryOk(sTimeStamp,statusMaxTimeAllowed)) {
             inputVec.clear();
             inputVec.addElement("Max retry check status exceeded for copy status");
             util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
             util.printMessage(
		"SRM-CLIENT: Max retry check status exceeded for copy status", 
			logger,silent);
             expBuffer.append("Max retry check exceeded for copy status");
             timedOutCase=true;
        }

        if(keySetArray.length > 0 && !timedOutCase)  {
          oSubKey = (String) keySetArray [0];
          TCopyRequestFileStatus status =
            (TCopyRequestFileStatus)(subStatusArray.get(oSubKey));
          TStatusCode code = status.getStatus().getStatusCode();
          if(_debug) {
            util.printMessage("SRM-CLIENT: FileStatusCode=" + code,
			  logger,silent);
          }
          if(code == TStatusCode.SRM_REQUEST_QUEUED ||
             code == TStatusCode.SRM_FILE_IN_CACHE ||
              code == TStatusCode.SRM_REQUEST_INPROGRESS) {
             int tStatusTime = 1;
             if(statusWaitTime >= 1000) {
               tStatusTime = statusWaitTime/1000;
             }
             util.printMessage("SRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",logger,silent);
             util.printMessage("SRM-CLIENT: Next status call in " + 
					tStatusTime + " seconds.",pIntf);
             inputVec.clear();
             inputVec.addElement("NextStatusCall="+
					statusWaitTime+" seconds.");
             util.printEventLog(_theLogger,"NextStatusCall",inputVec,silent,useLog);
            }
        }

        TCopyRequestFileStatus[] statuses = null;
        IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
        if (subStatusArray.size() > 0 && !timedOutCase) {
          Thread.sleep(statusWaitTime);
          if(keySURLS.size() > 0 && TURLS.size() > 0) {
            statuses  = checkCopyStatus(keySURLS, TURLS,
                         subResponse, rCode, expBuffer,wholeRequestFailed);
          }
        if(statuses != null) {
          for(int kk = 0; kk < statuses.length; kk++) {
            TCopyRequestFileStatus status = 
		  	  (TCopyRequestFileStatus) statuses[kk];
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
            //if(!path.startsWith("/")) {
              //path = "/"; 
            //}
            subStatusArray.put(protocol+"://"+host+":"+port+"/"+path,status);
            TStatusCode tempCode = null; 
               
            if(status != null) {
               if(wholeRequestFailed.getWholeRequestFailed()) {
                 expBuffer.append(status.getStatus().getExplanation());
                 if(status.getStatus().getStatusCode() != null) {
                   tempCode = status.getStatus().getStatusCode();
                 }
                 else {
                   tempCode = TStatusCode.SRM_FAILURE;
                 }
                 TReturnStatus rs = new TReturnStatus(); 
                 rs.setExplanation(status.getStatus().getExplanation());
                 rs.setStatusCode(tempCode);
                 status.setStatus(rs);
               }
               else {
                 tempCode = status.getStatus().getStatusCode();
                 expBuffer.append(status.getStatus().getExplanation());
               }
             }
             else {
               tempCode =
                    SRMUtilClient.mapReturnStatusValueBackToCode
			(_debug,rCode,logger,_theLogger);
             } 
             if(tempCode != TStatusCode.SRM_REQUEST_QUEUED &&
              tempCode != TStatusCode.SRM_REQUEST_INPROGRESS) {
                inputVec.clear();
                inputVec.addElement("FileStatus code from server="+tempCode);
                inputVec.addElement
			    ("Explanation from server=" + expBuffer.toString());
                util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
                if(_debug) {
                util.printMessage
                    ("SRM-CLIENT: SURL="+status.getSourceSURL().toString(),
                            logger,silent);
                util.printMessage
		    ("SRM-CLIENT: FileStatus code from server="+tempCode,
				logger,silent);
                util.printMessage
			    ("SRM-CLIENT: Explanation from server=" + 
					expBuffer.toString(),logger,silent);
                util.printMessage
                    ("SRM-CLIENT: SURL="+status.getSourceSURL().toString(),
                            pIntf);
                util.printMessage
			    ("SRM-CLIENT: FileStatus code from server="+
				tempCode,pIntf);
                util.printMessage
			    ("SRM-CLIENT: Explanation from server=" + 
				expBuffer.toString(), pIntf);
                }
             }
          }//end for
        }//end else
        }//end if(!timedOutCase) 
       }//while substatus

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
       inputVec.addElement
	 ("SetRequestDone is set with true and true in submitonly");
       util.printEventLog(_theLogger,"srmCopy", inputVec,silent,useLog);
      _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
      _srmClientIntf.setRequestDone(true,true);
     }
    else {
    if(totalFiles == numFilesFailed) {
       inputVec.clear();
       inputVec.addElement
	 ("SetRequestDone is set with true and true in totalfiles=numfilesfailed");
       util.printEventLog(_theLogger,"srmCopy", inputVec,silent,useLog);
      _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
      _srmClientIntf.setRequestDone(true,true);
    }
    else {
       inputVec.clear();
       inputVec.addElement
	 ("SetRequestDone is set with true and false in totalfiles=numfilesfailed");
       util.printEventLog(_theLogger,"srmCopy", inputVec,silent,useLog);
      _srmClientIntf.setRequestInformation(resultStatus, resultExplanation);
      _srmClientIntf.setRequestDone(true,false);
    }
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printCopyErrorResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printErrorResponse(TStatusCode code, 
	TCopyRequestFileStatus fileStatus, FileInfo fInfo) {

   util.printMessage("\n...........................................",logger,silent);

   if(fileStatus == null) {
     inputVec.clear();
     inputVec.addElement("Null fileStatus in the srmCopy.printErrorResponse");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
     if(_debug) {
     util.printMessage("\nSRM-CLIENT: Null fileStatus in the srmCopy.printErrorResponse", logger,silent);
     util.printMessage("\nSRM-CLIENT: Null fileStatus in the srmCopy.printErrorResponse", pIntf);
     }
     return;
   }

   inputVec.clear();
   inputVec.addElement("SourceSURL="+fileStatus.getSourceSURL().toString());
   inputVec.addElement("TargetSURL="+fileStatus.getTargetSURL().toString());
   inputVec.addElement("Status="+fileStatus.getStatus().getStatusCode());
   inputVec.addElement("Explanation="+fileStatus.getStatus().getExplanation());
   util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   if(_debug) {
   util.printMessage("SRM-CLIENT: SourceSURL="+
      fileStatus.getSourceSURL().toString(), logger,silent);
   util.printMessage("SRM-CLIENT: TargetSURL="+
      fileStatus.getTargetSURL().toString(), logger,silent);
   util.printMessage("SRM-CLIENT: Status="+
      fileStatus.getStatus().getStatusCode(),logger,silent);
   util.printMessage("SRM-CLIENT: Explanation="+
      fileStatus.getStatus().getExplanation(),logger,silent);
   util.printMessage("SRM-CLIENT: SourceSURL="+
      fileStatus.getSourceSURL().toString(), pIntf);
   util.printMessage("SRM-CLIENT: TargetSURL="+
      fileStatus.getTargetSURL().toString(), pIntf);
   util.printMessage("SRM-CLIENT: Status="+
      fileStatus.getStatus().getStatusCode(),pIntf);
   util.printMessage("SRM-CLIENT: Explanation="+
      fileStatus.getStatus().getExplanation(),pIntf);
   }

   if(fInfo == null) {
     return;
   }

   fInfo.setFailed(true);
   if(code == TStatusCode.SRM_ABORTED) {
     fInfo.setErrorMessage(code.toString() + " File Aborted.");
   }
   else {
     //fInfo.setErrorMessage(code.toString() + " " + fileStatus.getStatus().getExplanation());
     fInfo.setErrorMessage("");
   }

   if(code == TStatusCode.SRM_SUCCESS) {
     fInfo.setStatusLabel("Done");
   }
   else {
     fInfo.setStatusLabel("Failed");
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printCopyResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public HashMap printCopyResult (SrmCopyResponse response, 
	StringBuffer responseStatus, HashMap oResult) throws Exception {

   HashMap result = new HashMap();

   if(_debug) {
      util.printMessage("\nSRM-CLIENT: #### Output from SRM ####", logger,silent);
      util.printMessageHL("\nSRM-CLIENT: #### Output from SRM ####", pIntf);
   }

   if(response == null) {
     if(_debug) {
     util.printMessage("SRM-CLIENT: CopyRequest Response is null",logger,silent);
     util.printMessage("SRM-CLIENT: CopyRequest Response is null",pIntf);
     }
     inputVec.clear();
     inputVec.addElement("CopyRequest response is null");
     util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);
     return result;
   }

   inputVec.clear();
   if(response.getRequestToken() != null) {
     inputVec.addElement("request.token="+response.getRequestToken().toString());
     //if(_debug) {
     util.printMessage("\trequest.token ="+
        response.getRequestToken().toString(),logger,silent);
     util.printMessage("\trequest.token ="+
        response.getRequestToken().toString(),pIntf);
     requestToken = response.getRequestToken();
     //}
   }
   else {
     if(_debug) {
     util.printMessage("\trequest.token =null",logger,silent);
     util.printMessage("\trequest.token =null",pIntf);
     }
     inputVec.addElement("request.token=null");
   }

   util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);

   if(response.getReturnStatus().getStatusCode() == null) {
      inputVec.clear();
      inputVec.addElement("Null ReturnStatus returned by SRM in SrmCopyRequest ");
      util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);
      if(_debug) {
      util.printMessage("SRM-CLIENT: Null ReturnStatus returned by SRM in SrmCopyRequest ", logger,silent);
      util.printMessage("SRM-CLIENT: Null ReturnStatus returned by SRM in SrmCopyRequest ", pIntf);
      }
      return result;
   }

   if(submitOnly) {
     rStatus = response.getReturnStatus();
   }

   inputVec.clear();
   inputVec.addElement("status=" + response.getReturnStatus().getStatusCode());

   resultStatus = response.getReturnStatus().getStatusCode().toString();
   if(response.getReturnStatus().getExplanation() != null) {
     resultExplanation  = response.getReturnStatus().getExplanation();
   }
   util.printMessage("\tRequest.status=" + 
		response.getReturnStatus().getStatusCode(),logger,silent);
   util.printMessage("\tRequest.status=" + 
		response.getReturnStatus().getStatusCode(),pIntf);

   responseStatus.append(response.getReturnStatus().getStatusCode().toString()); 

   String explanation = response.getReturnStatus().getExplanation();
   if(explanation != null) {
     util.printMessage("\tRequest.explanation=" + explanation,logger,silent);
     util.printMessage("\tRequest.explanation=" + explanation,pIntf);
     inputVec.addElement("request.explanation=" + explanation);
   }
   util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);

   if(response.getArrayOfFileStatuses() == null) {
      inputVec.clear();
      inputVec.addElement("SRM returns null ArrayOfFileStatus from SrmCopyRequest");
      util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);
      if(_debug) {
      util.printMessage("SRM-CLIENT: SRM returns null ArrayOfFileStatus from SrmCopyRequest", logger,silent);
      util.printMessage("SRM-CLIENT: SRM returns null ArrayOfFileStatus from SrmCopyRequest", pIntf);
      }
      return result;
   }

   if(response.getArrayOfFileStatuses().getStatusArray() == null) {
      inputVec.clear();
      inputVec.addElement("SRM returns null StatusArray from SrmCopyRequest");
      util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);
      if(_debug) {
      util.printMessage("SRM-CLIENT: SRM returns null StatusArray from SrmCopyRequest", logger,silent);
      util.printMessage("SRM-CLIENT: SRM returns null StatusArray from SrmCopyRequest", pIntf);
      }
      return result;
   }

   int size = response.getArrayOfFileStatuses().getStatusArray().length;

   int localStatusWaitTime = 60;
   boolean estimatedTimeGiven = false;

   for (int i=0;  i < size; i++) {
     TCopyRequestFileStatus fileStatus =
        response.getArrayOfFileStatuses().getStatusArray(i);
     inputVec.clear();
     inputVec.addElement("SourceSURL="+fileStatus.getSourceSURL().toString());
     inputVec.addElement("TargetSURL="+fileStatus.getTargetSURL().toString());
     inputVec.addElement("status="+fileStatus.getStatus().getStatusCode());
     inputVec.addElement("explanation="+fileStatus.getStatus().getExplanation());
     util.printEventLog(_theLogger,"SrmCopyResult",inputVec,silent,useLog);
     if(_debug) {
     util.printMessage("surl="+
        fileStatus.getSourceSURL().toString(),logger,silent);
     util.printMessage("targetSURL="+
        fileStatus.getTargetSURL().toString(),logger,silent);
     util.printMessage("status="+
        fileStatus.getStatus().getStatusCode(),logger,silent);
     util.printMessage("explanation="+
        fileStatus.getStatus().getExplanation(),logger,silent);
     util.printMessage("SRM-CLIENT: ............",logger,silent);
     util.printMessage("surl="+
        fileStatus.getSourceSURL().toString(),pIntf);
     util.printMessage("targetSURL="+
        fileStatus.getTargetSURL().toString(),pIntf);
     util.printMessage("status="+
        fileStatus.getStatus().getStatusCode(),pIntf);
     util.printMessage("explanation="+
        fileStatus.getStatus().getExplanation(),pIntf);
     util.printMessage("SRM-CLIENT: ............",logger,silent);
     util.printMessage("SRM-CLIENT: ............",pIntf);
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
    
     util.printEventLog(_theLogger,"PrintCopyResult",inputVec,silent,useLog);
     MyGlobusURL gurl = new MyGlobusURL(fileStatus.getSourceSURL().toString(),1);
     String protocol = gurl.getProtocol();
     String host = gurl.getHost();
     int port = gurl.getPort();
     String path = gurl.getFilePath();
     //System.out.println(">>>Path="+path);
     int idx = path.indexOf("?SFN=");
     if(idx != -1) {
        path = path.substring(idx+5);
     }
     //if(!path.startsWith("/")) {
        //path = "/"; 
     //}
     result.put(protocol+"://"+host+":"+port+"/"+path, fileStatus);
     //System.out.println(">>>"+protocol+"://"+host+":"+port+path + "  " +
	//fileStatus.getSourceSURL().toString() + " " +
	//fileStatus.getTargetSURL().toString());
     //oResult.put(fileStatus.getSourceSURL().toString(), fileStatus);
     oResult.put(protocol+"://"+host+":"+port+"/"+path, 
		fileStatus.getSourceSURL().toString());
     Object[] objArray = fileInfo.toArray();
     for(int j = 0; j < objArray.length; j++) {
          FileInfo fInfo = (FileInfo) objArray[j];
          if(fInfo.getOrigSURL().equals(fileStatus.getSourceSURL().toString())) {
            if(fileStatus.getStatus() != null &&
               fileStatus.getStatus().getStatusCode() != null) {
              fInfo.setFileStatus
		(fileStatus.getStatus().getStatusCode().toString());
            }
            if(fileStatus.getStatus() != null &&
               fileStatus.getStatus().getExplanation() != null) {
              fInfo.setFileExplanation(fileStatus.getStatus().getExplanation());
            }
            else {
              fInfo.setFileExplanation("");
            }
          }
     }//end for(j)
   }//end for(i)

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

  SRMUtilClient.getSpaceTokenMeta(_srm,token,logger,_debug,
        _credential,proxyType,serverUrl,_theLogger, pIntf, 
        _srmClientIntf, silent, useLog, 
        connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
        delegation, numRetry, retryTimeOut);
}
	
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String  releaseFile(boolean keepSpace, String surl, 
	String token, int label) throws Exception {

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
  byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
  String proxyString = new String(bb);

  //if(label == -1) ignore the setting values;
  return SRMUtilClient.releaseFile
        (_srm,keepSpace,surl,token,uid,proxyString,
         _debug,logger, pIntf,_credential,proxyType,
         serverUrl,_theLogger, pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegation, numRetry, retryTimeOut);
  
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public void releaseSpace(String token, 
		boolean forceFileRelease) throws Exception {
		 
  if(gateWayModeEnabled) return ;
  SRMUtilClient.releaseSpace
        (_srm,token,uid,forceFileRelease,logger,_debug,
         _credential,proxyType,serverUrl,_theLogger,
         pIntf, _srmClientIntf, silent, useLog, 
         connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
         delegation, numRetry, retryTimeOut);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareToCopyMultiFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public SrmCopyResponse copyMultiFiles(String[] surls, 
	String[] turls, boolean[] overwritefiles, 
	String token)  throws Exception {


   ExtendedGSSCredential cred = (ExtendedGSSCredential) _credential;
   byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   String proxyString = new String(bb);

   if(doMkdir) {
     if(turls.length > 0) {
        if(turls[0].startsWith("srm") && !recursive || turls[0].startsWith("gsiftp")) {
          SRMUtilClient utilClient = new SRMUtilClient
            (serverUrl,uid,userDesc, _credential, _theLogger, logger,
             pIntf, _debug,silent,useLog,false, false,
             statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
             connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,delegation,numRetry,retryTimeOut);

          StringBuffer sCode = new StringBuffer();
          if(fileInfo.size() == 1) {
            FileInfo fInfo  = (FileInfo) fileInfo.elementAt(0);
            String turl = fInfo.getTURL().toString();
            String surl = fInfo.getSURL();
            if(turl.startsWith("gsiftp")) {
               MyGlobusURL gurl = new MyGlobusURL(turl,1);
               String temp = gurl.getFilePath();
               String host = gurl.getHost();
               String temp1 = "";
               while(true) {
                 int idx = temp.indexOf("/");
                 if(idx == -1) break;
                 int idx2 = temp.indexOf("/",idx+1);
                 String temp2 = "";
                 if(idx2 != -1) {
                    temp2 = temp.substring(idx, idx2);
                 } 
                 else {
                    temp2 = temp;
                 }
                 //System.out.println(">>TEMP="+temp);
                 //System.out.println(">>TEMP2="+temp2);
                 fInfo.setSURL("gsiftp://"+gurl.getHost()+":"+gurl.getPort()+"/"+temp1+temp2);
                 boolean b =  utilClient.doSrmMkdir
                             (fileInfo,sCode,false,true,false,false);
                 //System.out.println(">>Return value ="+b);
                 if(idx2 != -1) {
                   String temp3 = temp.substring(idx2);
                   //System.out.println(">>>TEMP3="+temp3);
                   temp1 = temp1+temp2;
                   //System.out.println(">>>TEMP1="+temp1);
                   temp = temp3;
                 }
                 else { 
				   temp = "";
                 }
               }
               fInfo.setSURL(surl);
            }
            else if(turl.startsWith("srm")) {
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
                       boolean b =  utilClient.doSrmMkdir
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
               }
            }//end if srm 
            fInfo.setTURL(turl); //set the turl back
       }
       else {
        util.printMessage(
         "\nSRM-CLIENT: Cannot continue, since -mkdir option is " +
         " only for single source and single target", logger,silent);
         System.exit(1);
       }
     }//end if
    }//end if
   }//end if

   SrmCopyRequest r = new SrmCopyRequest();

   inputVec.clear();
   if(!userDesc.equals("")) {
     inputVec.addElement("UserDescription="+userDesc);
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
     if(_debug) {
     util.printMessage("SRM-CLIENT: UserDescription="+userDesc, 
			logger, silent);
     util.printMessage("SRM-CLIENT: UserDescription="+userDesc, pIntf);
     } 
     r.setUserRequestDescription(userDesc);
   }

   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
     if(_debug) {
       util.printMessage("SRM-CLIENT: uid="+uid, logger, silent);
       util.printMessage("SRM-CLIENT: uid="+uid, pIntf);
     }
   }

   inputVec.clear();
   inputVec.addElement("AuthorizationID="+uid);

   if(_debug) {
   util.printMessage("SRM-CLIENT: AuthorizationID="+uid,logger,silent);
   //util.printMessage("SRM-CLIENT: UserRequestDescription=test user description", logger,silent);
   util.printMessage("SRM-CLIENT: AuthorizationID="+uid,pIntf);
   //util.printMessage("SRM-CLIENT: UserRequestDescription=test user description", pIntf);
   }
   //inputVec.addElement("UserRequestDescription=test user description");
   r.setUserRequestDescription(userDesc);
   if(token != null) {
     r.setTargetSpaceToken(token);
     if(_debug) {
     util.printMessage("SRM-CLIENT: SpaceToken= "+token, logger, silent);
     util.printMessage("SRM-CLIENT: SpaceToken= "+token, pIntf);
     }
     inputVec.addElement("SpaceToken="+token);
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }

   if(overwrite) {
     r.setOverwriteOption(TOverwriteMode.ALWAYS);
   }
   else {
     r.setOverwriteOption(TOverwriteMode.NEVER);
   }

   inputVec.clear();
   inputVec.addElement("Overwrite="+overwrite);
   if(_debug) {
   util.printMessage("SRM-CLIENT: Overwrite="+overwrite, logger,silent);
   util.printMessage("SRM-CLIENT: Overwrite="+overwrite, pIntf);
   }

   if(totalRequestTime != 0) {
     r.setDesiredTotalRequestTime(new Integer(totalRequestTime));
     inputVec.addElement("TotalTime="+totalRequestTime);
     if(_debug) {
     util.printMessage("SRM-CLIENT: TotalTime="+totalRequestTime,
        logger,silent);
     util.printMessage("SRM-CLIENT: TotalTime="+totalRequestTime, pIntf);
     }
   }

   if(fileLifeTime != 0) {
     r.setDesiredTargetSURLLifeTime(new Integer(fileLifeTime));
     inputVec.addElement("SURLLifeTime="+fileLifeTime);
     if(_debug) {
     util.printMessage("SRM-CLIENT: SURLLifeTime="+fileLifeTime, logger, silent);
     util.printMessage("SRM-CLIENT: SURLLifeTime="+fileLifeTime, pIntf);
     }
   }

   if(fileStorageType != null) {
     r.setTargetFileStorageType(fileStorageType);
     inputVec.addElement("FileStorageType="+fileStorageType);
     if(_debug) {
     util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType,
        logger,silent);
     util.printMessage("SRM-CLIENT: FileStorageType="+fileStorageType, pIntf);
     }
   }

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

   int total = surls.length;

   TCopyFileRequest[] fileReqList = new TCopyFileRequest[total];

   /*
   cred = (ExtendedGSSCredential) _credential;
   bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
   proxyString = new String(bb);
   */

   boolean useSourceStorageSystemInfo = false;
   boolean useTargetStorageSystemInfo = false;

   for (int i=0; i<total; i++) {
     TCopyFileRequest curr = new TCopyFileRequest();
     curr.setSourceSURL(new URI(surls[i]));
     curr.setTargetSURL(new URI(turls[i]));

     if(recursive) {
        TDirOption dirOption = new TDirOption();
        dirOption.setIsSourceADirectory(recursive);
        if(numLevels == 0) {
          dirOption.setAllLevelRecursive(new Boolean(true));
          System.out.println("SRM-CLIENT: Recursive=true");
        }
        else {
          dirOption.setAllLevelRecursive(new Boolean(false));
          System.out.println("SRM-CLIENT: Recursive=false");
          dirOption.setNumOfLevels(new Integer(numLevels));
        }
        curr.setDirOption(dirOption);
     }

     int idx = surls[i].indexOf("mss://");

     if(storageInfo.equals("")) { 
       idx = turls[i].indexOf("mss://");
     }
     fileReqList[i] = curr;
   }

   inputVec.addElement("SourceStorageSystemInfo="+useSourceStorageSystemInfo);
   inputVec.addElement("TargetStorageSystemInfo="+useTargetStorageSystemInfo);
   ArrayOfTCopyFileRequest arrayOfCopy = new ArrayOfTCopyFileRequest();
   arrayOfCopy.setRequestArray(fileReqList);

   r.setArrayOfFileRequests(arrayOfCopy);

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

 
   int checkIdx = storageInfo.indexOf("#");
   String sInfo2 = "";

   if(useSourceStorageSystemInfo) {
     tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue("copy");
     vec.addElement(tExtra);
 
     tExtra = new TExtraInfo();
     tExtra.setKey("pwd");
     tExtra.setValue(proxyString);
     vec.addElement(tExtra);
   }
   else if(storageInfo.startsWith("for")) {
      if(checkIdx != -1) {
        sInfo2 = storageInfo.substring(checkIdx+1);
        storageInfo = storageInfo.substring(0,checkIdx);
      }
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

   if(checkIdx !=-1) {
     storageInfo = sInfo2;
   }
 

   TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
   for(int i = 0; i < vec.size(); i++) {
     a_tExtra[i] = (TExtraInfo) vec.elementAt(i);
   }
   storageInfoArray.setExtraInfoArray(a_tExtra);
   if(_debug) {
     System.out.println("\nSRM-CLIENT: setting source storage info");
   }
   r.setSourceStorageSystemInfo(storageInfoArray);

   ArrayOfTExtraInfo targetStorageInfoArray = new ArrayOfTExtraInfo();
   vec = new Vector();

   tExtra = new TExtraInfo();
   tExtra.setKey("gsiftpStream");
   tExtra.setValue(""+parallelism);
   vec.addElement(tExtra);
   if(_debug) {
   util.printMessage("SRM-CLIENT: gsiftpStream="+parallelism, logger, silent);
   util.printMessage("SRM-CLIENT: gsiftpStream="+parallelism, pIntf);
   }
   inputVec.addElement("gsiftpStream="+parallelism);

   tExtra = new TExtraInfo();
   tExtra.setKey("bufferSize");
   tExtra.setValue(""+bufferSize);
   vec.addElement(tExtra);
   if(_debug) {
   util.printMessage("SRM-CLIENT: bufferSize="+bufferSize, logger, silent);
   util.printMessage("SRM-CLIENT: bufferSize="+bufferSize, pIntf);
   }
   inputVec.addElement("bufferSize="+bufferSize);

   tExtra = new TExtraInfo();
   tExtra.setKey("dcau");
   tExtra.setValue(""+dcau);
   vec.addElement(tExtra);
   if(_debug) {
   util.printMessage("SRM-CLIENT: DCAU="+dcau, logger, silent);
   util.printMessage("SRM-CLIENT: DCAU="+dcau, pIntf);
   }
   inputVec.addElement("DCAU="+dcau);

   if(useTargetStorageSystemInfo) {
     tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue("copy");
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
 

   a_tExtra = new TExtraInfo[vec.size()];
   for(int i = 0; i < vec.size(); i++) {
     a_tExtra[i] = (TExtraInfo) vec.elementAt(i);
   }
   targetStorageInfoArray.setExtraInfoArray(a_tExtra);
   if(_debug) {
     System.out.println("\nSRM-CLIENT: setting target storage info");
   }
   r.setTargetStorageSystemInfo(targetStorageInfoArray);

   inputVec.addElement("TimeStamp="+new Date());
   util.printEventLog(_theLogger,"Sending SrmCopy request now",inputVec,silent,useLog);

   SrmCopyResponse result = null;


  try {
   result = (SrmCopyResponse) callSoapThread(r,result,"srmcopy");
  }catch (Exception e) {
    System.out.println("SrmCopy.Exception="+e.getMessage());
    util.printEventLogException(_theLogger,"srmCopy",e); 
  }
   return result;

}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int doStatus(String uid, Vector fileInfo, String rToken)
      throws URI.MalformedURIException, java.rmi.RemoteException {

 if(_debug) {
   util.printMessage("\nSRM-CLIENT: ...Input parameters for SrmStatusOfCopyRequst...",logger,silent);
   util.printMessage("SRM-CLIENT: RequestToken="+rToken,logger,silent);
   util.printMessage("SRM-CLIENT: AuthId="+uid,logger,silent);
 }

 SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
 int size = fileInfo.size();
 String sampleURL = "";

 URI[] fromsurl = new URI[size];
 URI[] tosurl  = new URI[size];

 for(int i = 0; i < size; i++) {
   FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
   fromsurl[i] =  new URI(fIntf.getSURL());
   tosurl[i] = new URI(fIntf.getTURL());
   sampleURL=fIntf.getSURL();
   if(_debug) {
     util.printMessage("SRM-CLIENT: SURL="+fromsurl[i],logger,silent);
     util.printMessage("SRM-CLIENT: TURL="+tosurl[i],logger,silent);
   }
 }

 ArrayOfAnyURI arrayOfFromURI = SRMUtilClient.convertToArrayOfAnyURI(fromsurl);
 ArrayOfAnyURI arrayOfToURI   = SRMUtilClient.convertToArrayOfAnyURI(tosurl);
 r.setArrayOfSourceSURLs(arrayOfFromURI);
 r.setArrayOfTargetSURLs(arrayOfToURI);

 r.setRequestToken(rToken);
 if(!uid.equals("")) {
   r.setAuthorizationID(uid);
 } 

 SrmStatusOfCopyRequestResponse result = null;
 TStatusCode sCode = null;

 try {
 result = (SrmStatusOfCopyRequestResponse) 
                callSoapThread(r,result,"srmstatusofcopy");


 if(_debug) {
   util.printMessage("\nSRM-CLIENT: #### Output from SRM ####",logger,silent);
 }

 if(result == null) {
   inputVec.clear();
   inputVec.addElement("RequestId="+rToken);
   inputVec.addElement("Null result");
   inputVec.addElement("May be the given requestId is not valid ");
   util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   util.printMessage("\nSRM-CLIENT: Null result",logger,silent);
   util.printMessage("SRM-CLIENT: May be the given requestId is not valid ",logger,silent);
   util.printMessage("SRM-CLIENT: RequestId="+rToken,logger,silent);
   util.printMessage("\nSRM-CLIENT: Null result",pIntf);
   util.printMessage("SRM-CLIENT: May be the given requestId is not valid ",pIntf);
   util.printMessage("SRM-CLIENT: RequestId="+rToken,pIntf);
   return util.mapStatusCode(sCode);
 }

 if(result.getReturnStatus() != null) {
   TReturnStatus rStatus = result.getReturnStatus();
   if(rStatus != null) {
     sCode = rStatus.getStatusCode();
     util.printMessage("\nRequestStatus="+sCode.toString(),logger,silent);
     util.printMessage("\nRequestExplanation="+rStatus.getExplanation(),logger,silent);
     inputVec.clear();
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("RequestStatus="+sCode.toString());
     inputVec.addElement("RequestExplanation="+rStatus.getExplanation());
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   else {
     util.printMessage("Null status code for this request", logger,silent);
     inputVec.clear();
     inputVec.addElement("RequestId="+rToken);
     inputVec.addElement("null status code for this request");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   }
   return util.mapStatusCode(sCode);
 }

 ArrayOfTCopyRequestFileStatus arrayFStatus = result.getArrayOfFileStatuses();

 if(arrayFStatus == null) {
   util.printMessage("Null getArrayOfFileStatuses", logger,silent);
   inputVec.clear();
   inputVec.addElement("RequestId="+rToken);
   inputVec.addElement("null getArrayOfFileStatuses");
   util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   return util.mapStatusCode(sCode);
 }

 TCopyRequestFileStatus[] fileStatus = arrayFStatus.getStatusArray();

 if(fileStatus.length == 0) {
   inputVec.clear();
   inputVec.addElement("No files in this request");
   util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
   util.printMessage("SRM-CLIENT: No files in this request", logger,silent);
   util.printMessage("SRM-CLIENT: No files in this request", pIntf);
   return util.mapStatusCode(sCode);
 }

 for(int i = 0; i < fileStatus.length; i++) {
   inputVec.clear();
   if(_debug) {
   util.printMessage("\nSRM-CLIENT: ...........................................",logger,silent);
   }
   if(fileStatus[i].getSourceSURL() != null) {
     util.printMessage("SRM-CLIENT: SourceSURL="+
       fileStatus[i].getSourceSURL().toString(),logger,silent);
     util.printMessage("SRM-CLIENT: SourceSURL="+
       fileStatus[i].getSourceSURL().toString(),pIntf);
     inputVec.addElement("SourceSURL="+fileStatus[i].getSourceSURL().toString());
   }
   if(fileStatus[i].getTargetSURL() != null) {
     util.printMessage("SRM-CLIENT: TargetSURL="+
       fileStatus[i].getTargetSURL().toString(),logger,silent);
     util.printMessage("SRM-CLIENT: TargetSURL="+
       fileStatus[i].getTargetSURL().toString(),pIntf);
     inputVec.addElement("TargetSURL="+fileStatus[i].getTargetSURL().toString());
   }
   if(fileStatus[i].getStatus() != null) {
     util.printMessage("SRM-CLIENT: Status="+
       fileStatus[i].getStatus().getStatusCode(),logger,silent);
     util.printMessage("SRM-CLIENT: Status="+
       fileStatus[i].getStatus().getStatusCode(),pIntf);
     util.printMessage("SRM-CLIENT: Explanation="+
       fileStatus[i].getStatus().getExplanation(),logger,silent);
     util.printMessage("SRM-CLIENT: Explanation="+
       fileStatus[i].getStatus().getExplanation(),pIntf);
     inputVec.addElement("Status="+fileStatus[i].getStatus().getStatusCode());
     inputVec.addElement("Explanation="+fileStatus[i].getStatus().getExplanation());
   }
   if(fileStatus[i].getFileSize() != null) {
     util.printMessage("SRM-CLIENT: FileSize="+
       fileStatus[i].getFileSize(),logger,silent);
     util.printMessage("SRM-CLIENT: FileSize="+
       fileStatus[i].getFileSize(),pIntf);
     inputVec.addElement("FileSize="+fileStatus[i].getFileSize());
   }
   if(fileStatus[i].getRemainingFileLifetime() != null) {
     util.printMessage("SRM-CLIENT: RemainingFileLifeTime="+
       fileStatus[i].getRemainingFileLifetime(),logger,silent);
     util.printMessage("SRM-CLIENT: RemainingFileLifeTime="+
       fileStatus[i].getRemainingFileLifetime(),pIntf);
     inputVec.addElement("RemainingFileLifeTime="+fileStatus[i].getRemainingFileLifetime());
   }
   if(fileStatus[i].getEstimatedWaitTime() != null) {
     util.printMessage("SRM-CLIENT: EstimatedWaitTime="+
       fileStatus[i].getEstimatedWaitTime(),logger,silent);
     util.printMessage("SRM-CLIENT: EstimatedWaitTime="+
       fileStatus[i].getEstimatedWaitTime(),pIntf);
     inputVec.addElement("EstiamtedWaitTime="+fileStatus[i].getEstimatedWaitTime());
   }
 }
 util.printEventLog(_theLogger,"SrmCopyStatus",inputVec,silent,useLog);
 }catch(Exception e) {
   System.out.println("SRM-CLIENT: SRMCopyClient.doStatus.Exception" +
                e.getMessage());
   util.printEventLogException(_theLogger,"bringonline.doStatus",e);
 }
 return util.mapStatusCode(sCode);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkCopyStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public TCopyRequestFileStatus[] checkCopyStatus
    (Vector keySURLS,
	 Vector TURLS, String response, StringBuffer rCode, 
	 StringBuffer expBuffer, IsWholeRequestFailed wholeStatusFailed)
    throws URI.MalformedURIException, java.rmi.RemoteException {

   URI[] uris = new URI[keySURLS.size()];
   for(int i = 0; i < keySURLS.size();i++) { 
     uris[i] = (URI) keySURLS.elementAt(i);   
   }
   URI[] turls = new URI[keySURLS.size()];
   for(int i = 0; i < TURLS.size();i++) { 
     turls[i] = (URI)TURLS.elementAt(i);   
   }

   Date statusDate = new Date();
   util.printMessage("SRM-CLIENT: " + statusDate +
        " Calling Status at " + response + " " + new Date(),logger,silent);
   util.printMessageHL("SRM-CLIENT: " + statusDate +
        " Calling Status at " + response + " " + new Date(),pIntf);

   if(_debug) {
     util.printMessage("\n::::::::: Input parameters for SrmStatusOfCopyRequest ::::",
            logger,silent);
     util.printMessage("\n\tRID="+response,logger,silent);
     util.printMessage("\tuid="+uid,logger,silent);
     for(int i = 0; i < keySURLS.size(); i++) {
      util.printMessage("\tSourceSURL("+i+")="+(URI)keySURLS.elementAt(i),logger,silent);
      util.printMessage("\tTargetSURL(="+i+")="+(URI)TURLS.elementAt(i),logger,silent);
     } 
   }

   if(response == null) {
     inputVec.clear();
     inputVec.addElement("SRM return null request token for SrmCopyResponse");
     util.printMessage("\nSRM return null reques token for SrmCopyResponse", logger,silent);
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
     return null;
   }

   SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();

   r.setArrayOfSourceSURLs(SRMUtilClient.convertToArrayOfAnyURI(uris));
   r.setArrayOfTargetSURLs(SRMUtilClient.convertToArrayOfAnyURI(turls));
   r.setRequestToken(response);
   if(!uid.equals("")) {
     r.setAuthorizationID(uid);
   }

   SrmStatusOfCopyRequestResponse result = null;
   TCopyRequestFileStatus[] fileStatus = null;

  try {
   result = (SrmStatusOfCopyRequestResponse) 
        callSoapThread(r,result,"srmstatusofcopy");

   if(_debug) {
     util.printMessage("\nSRM-CLIENT: ### Output from SRM ###",logger,silent);
   }

   if(result == null) {
     inputVec.clear();
     inputVec.addElement("\nSRM returns null SrmStatusOfCopyRequestResponse");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
     if(_debug) {
     util.printMessage("\nSRM-CLIENT: SRM returns null SrmStatusOfCopyRequestResponse", logger,silent);
     util.printMessage("\nSRM-CLIENT: SRM returns null SrmStatusOfCopyRequestResponse", pIntf);
     }
     return null;
   }

   if(result.getReturnStatus() == null) {
     inputVec.clear();
     inputVec.addElement("SRM returns null return status");
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: SRM returns null return status", logger, silent);
     util.printMessage("\nSRM-CLIENT: SRM returns null return status", pIntf);
     return null;
   }

   rStatus = result.getReturnStatus();

   String explanation = result.getReturnStatus().getExplanation();
     inputVec.clear();
     inputVec.addElement("Status Calling at " + new Date());
     inputVec.addElement("RequestStatus=" + result.getReturnStatus().getStatusCode());
     if(requestDate != null) {

     /*
     util.printMessage("SRM-CLIENT: Elapsed Time from requestTime is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", logger,silent);
     util.printMessage("SRM-CLIENT: Elapsed Time from requestTime is " + util.getElapsedTime(requestDate,statusDate) + " seconds.", pIntf);
     */
     }
     util.printMessage("SRM-CLIENT: (srmStatusOfCopyRequest) RequestStatus=" + result.getReturnStatus().getStatusCode(), logger,silent);
     util.printMessage("SRM-CLIENT: (srmStatusOfCopyRequest) RequestStatus=" + result.getReturnStatus().getStatusCode(), pIntf);
     if(explanation != null) {
       inputVec.addElement("RequestExplanation=" + explanation);
       util.printMessage("SRM-CLIENT: RequestExplanation=" + explanation, logger,silent);
       util.printMessage("SRM-CLIENT: RequestExplanation=" + explanation, 
			pIntf);
     }
     util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);

   rCode.append(result.getReturnStatus().getStatusCode().toString());
   if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {
     wholeStatusFailed.setWholeRequestFailed(true); 
   }


    /*
    if(pushMode) {

      if(_debug) {
        util.printMessage
		  ("SRM-CLIENT: ::::::::::::::::: Input parameters for SrmGetRequestSummary :::::", logger,silent);
        util.printMessage
		  ("SRM-CLIENT: ...Input parameters for SrmGetRequestSummary...",pIntf);
        inputVec.clear();
        inputVec.addElement("AuthId="+uid);
        inputVec.addElement("RID="+response);
        util.printMessage("SRM-CLIENT: AuthId="+uid,logger,silent);
        util.printMessage("SRM-CLIENT: RID="+response,logger,silent);
        util.printMessage("SRM-CLIENT: AuthId="+uid,pIntf);
        util.printMessage("SRM-CLIENT: RID="+response,pIntf);
        util.printEventLog(_theLogger,"Input parameters for SrmGetRequestSummary",inputVec,silent,useLog);
      }
      SrmGetRequestSummaryRequest sr = new SrmGetRequestSummaryRequest();
      if(!uid.equals("")) {
        sr.setAuthorizationID(uid);
      }
      String[] tokenArray = new String[1];
      tokenArray[0] = response;
      ArrayOfString arrayOfRequestTokens = new ArrayOfString();
      arrayOfRequestTokens.setStringArray(tokenArray);
      sr.setArrayOfRequestTokens(arrayOfRequestTokens);
      if(!uid.equals("")) {
        sr.setAuthorizationID(uid);
      }

      SrmGetRequestSummaryResponse sresult = null;

      try {
         sresult = callSoapThread(_srm,sr,sresult,"srmgetrequestsummary");
      }catch(Exception e) {
         System.out.println("SrmCopy.Exception="+e.getMessage());
         util.printEventLogException(_theLogger,"srmGetRequestSummary",e);
      }

      if(sresult == null) {
        inputVec.clear();
        inputVec.addElement("SRM returned null response for srmGetRequestSummary");
        if(_debug) {
        util.printMessage("\nSRM-CLIENT: SRM returned null response for srmGetRequestSummary", logger,silent);
        util.printMessage("\nSRM-CLIENT: SRM returned null response for srmGetRequestSummary", pIntf);
        } 
        util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
        return null;
      }

      if(sresult.getReturnStatus() == null) {
        inputVec.clear();
        inputVec.addElement(
			"\nSRM returned null returnStatus for srmGetRequestSummary");
        util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
        if(_debug) {
        util.printMessage("\nSRM-CLIENT: SRM returned null returnStatus for srmGetRequestSummary", logger,silent);
        util.printMessage("\nSRM-CLIENT: SRM returned null returnStatus for srmGetRequestSummary", pIntf);
        }
        return null;
      }

      inputVec.clear();
      inputVec.addElement("Status of GetRequestSummary is : " +
           sresult.getReturnStatus().getStatusCode());
      util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
      if(_debug) {
      util.printMessage("\nSRM-CLIENT: Status of GetRequestSummary is : " +
           sresult.getReturnStatus().getStatusCode(),logger,silent);
      util.printMessage("\nSRM-CLIENT: Status of GetRequestSummary is : " +
           sresult.getReturnStatus().getStatusCode(),pIntf);
      }

      if(sresult.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS) {
        if(sresult.getArrayOfRequestSummaries() != null) {
           ArrayOfTRequestSummary arrayOfSummaries =
                sresult.getArrayOfRequestSummaries();
           TRequestSummary[] reqSummary = arrayOfSummaries.getSummaryArray();
           if(reqSummary != null && reqSummary.length > 0)  {
             int totalFiles = reqSummary[0].getTotalNumFilesInRequest().intValue();
             int numFinished = reqSummary[0].getNumOfCompletedFiles().intValue();
             inputVec.clear();
             inputVec.addElement("TotalFiles=" + totalFiles);
             inputVec.addElement("NumFinished=" + numFinished);
             util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
             if(_debug) {
             util.printMessage("SRM-CLIENT: TotalFiles=" + totalFiles,logger,silent);
             util.printMessage("SRM-CLIENT: TotalFiles=" + totalFiles,pIntf);
             util.printMessage("SRM-CLIENT: NumFinished=" + numFinished,logger,silent);
             util.printMessage("SRM-CLIENT: NumFinished="+numFinished,pIntf);
             }
             if(totalFiles == numFinished) {
              if(result.getArrayOfFileStatuses() != null) {
                //fileStatus = 
					//result.getArrayOfFileStatuses().getStatusArray(0); 
                fileStatus = 
					result.getArrayOfFileStatuses().getStatusArray(); 
                //expBuffer.append(fileStatus.getStatus().getExplanation());
			  }
             }
           }
        }
      }
   }
   else {
    */
      if(result.getArrayOfFileStatuses() == null) {
         inputVec.clear();
         inputVec.addElement("SRM returned null ArrayOfFileStatus " +
			"in SrmStatusOfCopyRequest");
         util.printEventLog(_theLogger,"SrmGetRequestSummaryResponse",inputVec,silent,useLog);
         if(_debug) {
           util.printMessage("\nSRM-CLIENT: SRM returned null ArrayOfFileStatus " + 
				"in SrmStatusOfCopyRequest", logger,silent);
           util.printMessage("\nSRM-CLIENT: SRM returned null ArrayOfFileStatus " + 
				"in SrmStatusOfCopyRequest", pIntf);
         }
         return fileStatus;
      }
      fileStatus = result.getArrayOfFileStatuses().getStatusArray();

      if(fileStatus != null) {
        for(int kk = 0; kk < fileStatus.length; kk++) {
           TCopyRequestFileStatus fStatus = fileStatus[kk];
           if(fStatus.getEstimatedWaitTime() != null) {
              Integer xx = fStatus.getEstimatedWaitTime();
              int yy = xx.intValue();
              if(yy != -1) {
                 if(_debug) {
                    System.out.println("\nSRM-CLIENT: EstimatedWaitTime " + 
                      "given by server is " + yy  + " seconds.");
                 }
                 if(statusWaitTime > yy*1000) {
                   statusWaitTime = yy*1000;
                 }
              }
           }
        }
      }
   //}
   
  //System.out.println(">>>StatusWaitTime Before="+statusWaitTime);
  statusWaitTime = statusWaitTime*2;
  if(statusWaitTime >= threshHold*1000) {
    statusWaitTime = threshHold*1000; //resetting back to threshHold seconds
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
    System.out.println("SRM-CLIENT: SRMCopyClient.Exception " + 
                e.getMessage());
    util.printEventLogException(_theLogger,"SRMCopyClient.checkCopyStatus",e);
  }

  return fileStatus;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callSoapThread
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Object callSoapThread(Object request,
        Object result, String methodName) throws Exception {

     int retrySoFar=0;
     boolean timeOutHappened=false;
     int nRetry = numRetry;
     boolean statusCall = false;
     String exceptionHappened="";

     if(methodName.equals("srmstatusofcopy")) {
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
       util.printEventLog(_theLogger,"SRMCopyClient.callSoapThread",
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
           util.printEventLog(_theLogger,"SRMCopyClient.callSoapThread",
                inputVec,silent,useLog);
           try {
            _srm=null;
            soapCallThread.setInterrupt(true);
            openSRMConnection();
           }catch(Exception ie) {
             util.printEventLogException(_theLogger,
                "SRMCopyClient.callSoapThread",ie);
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
             util.printEventLog(_theLogger,"SRMCopyClient.callSoapThread",
                inputVec,silent,useLog);
           }
         }
       }//end while
       retrySoFar++;
    }//end while

    /*
    if(methodName.equals("srmcopy")) {
     timeOutHappened=true;
     result = null;
    }
    */

    if(timeOutHappened && result == null && !statusCall) {
           inputVec.clear();
           inputVec.addElement("setrequestdone to true and failed");
           inputVec.addElement("timedout and responseobject is null");
           inputVec.addElement("SRM server did not respond, server may be busy");
           util.printEventLog(_theLogger,"SRMCopyClient.callSoapThread",
                inputVec,silent,useLog);
       if(methodName.equals("srmcopy")) {
        _srmClientIntf.setRequestTimedOut(true);
       }
     }

     return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// reserveSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
	
public String reserveSpace(String strUID) throws Exception {

  if(gateWayModeEnabled) return "SRM_NOT_SUPPORTED";

  TStatusCode sCode = null;
  StringBuffer statusBuf = new StringBuffer();
  return SRMUtilClient.reserveSpace(_srm,strUID, userDesc, 
        retentionPolicyInfo, tokenSize, tokenLifetime,guarnSize,
        new StringBuffer(),logger,_debug,false,sCode,statusBuf,
        _credential,proxyType,serverUrl,_theLogger, pIntf, 
        _srmClientIntf, silent, useLog, 
        connectionTimeOutAllowed, setHTTPConnectionTimeOutAllowed, 
        delegation, numRetry, retryTimeOut);
}
	
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

/*
private void updateToken(TSpaceToken token, 
  long gSpaceInMB, long tSpaceInMB, long lifetime)  throws Exception 
{
    util.printMessage("....................................",logger,silent);
    util.printMessage("updating token for="+token,logger,silent);
			
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
			
   util.printMessage("Total="+newSizeT.getValue(),logger,silent);
   util.printMessage("Min="+newSizeG.getValue(),logger,silent);
   util.printMessage("LifeTime="+spaceLifetime.getValue(),logger,silent);

   SrmUpdateSpaceResponse result1 = null;


   result1 = callSoapThread(_srm,r1,result1,"srmupdatespace");
			
   if (result1 != null) {
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
