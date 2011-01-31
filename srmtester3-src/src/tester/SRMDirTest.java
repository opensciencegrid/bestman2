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

package tester;

import java.io.*;
import java.util.*;
import java.net.*;
import java.lang.Thread;

import javax.xml.rpc.Stub;

import org.globus.io.urlcopy.UrlCopy;
import org.globus.util.GlobusURL;

//import org.globus.ogsa.utils.GSIUtils;
import org.globus.axis.gsi.GSIConstants;
//import org.globus.ogsa.impl.security.authentication.Constants;

import org.globus.util.ConfigUtil;
//import org.globus.ogsa.utils.GetOpts;
//import org.globus.ogsa.gui.ServiceContext;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.auth.*;

import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.client.main.MySRMFileTransfer;
import gov.lbl.srm.client.intf.PrintIntf;

//import gov.lbl.srm.server.TSRMUtil;

import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMDirTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMDirTest
{
   private String publishUrlStr;
   private String displayType;
   private String testType;
   private String timeStamp;
   private String fullTimeStamp;
   private String servicePath;
   private GSSCredential credential;
   private String proxyFile;
   private boolean noPublish;
   private boolean detailed;
   private boolean defaultProxy;
   private int currTestCondition;
   private String siteName;
   private int numOperations;
   private boolean dailyTest;
   private String startTimeStamp;
   private int numSites;
   private int retryTimeAllowed;
   private int statusWaitTime=15; //default 15 seconds
   private Log logger;
   private PrintIntf pIntf;
   private String typeString="srmLs";
   private boolean localPublish;  
   private BufferedWriter bs;
   private BufferedWriter bsa;
   private boolean changeToCopyServicePath;
   private String copyServicePath;
   private OperOk lsOk;
   private TimeOutCallBack timeOutCallBack;
   private boolean useDriverOn;
   private OperOk pingOverAllSuccess;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMDirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMDirTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp,
	   String servicePath, GSSCredential credential, String proxyFile, 
	   boolean noPublish, boolean detailed, boolean defaultProxy, 
       String siteName, int numOperations, 
       String typeString, String startTimeStamp, boolean dailyTest,
       int numSites, int retryTimeAllowed, 	int statusWaitTime,
	   boolean localPublish, BufferedWriter bs,
	   BufferedWriter bsa, Log logger, PrintIntf pIntf,
	   boolean useDriverOn, OperOk pingOverAllSuccess) {

   this.publishUrlStr = publishUrlStr;
   this.displayType = displayType;
   this.testType = testType;
   this.timeStamp = timeStamp;
   this.fullTimeStamp = fullTimeStamp;
   this.servicePath = servicePath;
   this.credential = credential;
   this.proxyFile = proxyFile;
   this.noPublish = noPublish;
   this.detailed = detailed;
   this.defaultProxy = defaultProxy;
   this.siteName = siteName;
   this.numOperations = numOperations;
   this.startTimeStamp = startTimeStamp;
   this.dailyTest = dailyTest;
   this.numSites = numSites;
   this.logger = logger;
   this.pIntf = pIntf;
   this.retryTimeAllowed = retryTimeAllowed;
   this.statusWaitTime = statusWaitTime*1000;
   this.localPublish = localPublish;
   this.bs = bs;
   this.bsa = bsa;
   if(!typeString.equals("")) {
     this.typeString = typeString;
   }
   this.useDriverOn = useDriverOn;
   this.pingOverAllSuccess = pingOverAllSuccess;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRedirectedSrmServicePath
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRedirectedSrmServicePath(boolean changeCopyServicePath,
                                          String copyServicePath) {
  this.changeToCopyServicePath = changeCopyServicePath;
  this.copyServicePath = copyServicePath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doRmdirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doRmdirTest(ISRM srm, StringBuffer proxyString, String uid,
    String source, SharedObjectLock sLock, OperOk rmdirOk, int testCondition) 
		throws  Exception {

  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.currTestCondition = testCondition;

  util.printMessage(" ++++ Sending \"Rmdir\" request +++++ ",logger,pIntf);

  SrmRmdirRequest r = new SrmRmdirRequest(); 
  ReportRequestStatus rrs = util.createReportRequestStatus(source,"","");

  try {
    if(detailed) {
      if(testCondition == 0) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doRmdirStorageTest(uid,surls[0],rrs);
      }
      else if(testCondition == 1) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doRmdirTestRecursive(uid,surls[0],true,rrs);
      }
      else { 
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doRmdirTest(uid,surls[0],rrs);
      }
    } 
    else {
     SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
     pTest.doRmdirTest(uid,surls[0],rrs);
    }
  } catch(Exception ee) {
     util.printMessage("Exception " + ee.getMessage(),logger,pIntf);
     rmdirOk.setExplanation("Exception");
     ee.printStackTrace();
  }


  Date d = new Date ();
  String operatorTimeStamp = d.toString();

  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "Rmdir","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp,
			servicePath,noPublish,
           siteName,numOperations,typeString,"srmRmdir",
	       "Sending",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
  }


  StringBuffer explanationBuffer = new StringBuffer();
  String status = "";
  rrs.setLinkName("rmdir_"+currTestCondition);
  String displayStatus = "Ok";
  try {
    SrmRmdirResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmRmdir(r); 
      timeOutCallBack.setObject(response); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }
    status = printRmDirResult(response,rrs,explanationBuffer);
    if(status.equals("SRM_SUCCESS") || status.equals("SRM_NOT_SUPPORTED") || 
       status.equals("SRM_DONE")) {
        if(status.equals("SRM_NOT_SUPPORTED")) {
          displayStatus = "N.S.";
        }
    }
    else {
        displayStatus = "Failed";
    }
    if(displayStatus.equals("Ok")) {
     rmdirOk.isOperOk(true);
    }
    else {
     rmdirOk.setExplanation(displayStatus);
    }
  }catch(Exception e) {
    rmdirOk.setExplanation("Exception");
    explanationBuffer.append(e.getMessage());
    status = "Exception";
    displayStatus = "Exception";
    rrs.setActualResult("Exception");
    rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
  }

  try {
       util.webScript(publishUrlStr,
	    displayType,"SendingRMDIR","Rmdir", rrs,
		"", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,displayType,
	    "Rmdir",status,rrs,explanationBuffer.toString(),
		timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmRmdir",
		displayStatus,dailyTest,numSites,localPublish,bs,bsa);
  }catch(Exception e) { e.printStackTrace(); }
  if(sLock != null)  {
    sLock.setIncrementCount();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRmTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doSrmRmTest(ISRM srm, StringBuffer proxyString, String uid,
    String source, SharedObjectLock sLock, OperOk isRmSuccess, 
	int testCondition) throws  Exception {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.currTestCondition = testCondition;

  util.printMessage(" ++++ Sending \"SrmRm\" request +++++ ",logger,pIntf);

  SrmRmRequest r = new SrmRmRequest(); 
  ReportRequestStatus rrs = util.createReportRequestStatus(source,"","");

  try {
    if(detailed) {
      if(testCondition == 0) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doSrmRmStorageTest(uid,surls[0],rrs);
      }
      else { 
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doSrmRmTest(uid,surls[0],rrs);
      }
    } 
    else {
     SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
     pTest.doSrmRmTest(uid,surls[0],rrs);
    }
  } catch(Exception ee) {
     util.printMessage("Exception " + ee.getMessage(),logger,pIntf);
     ee.printStackTrace();
  }


  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "SrmRm","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmRm","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
  }


  StringBuffer explanationBuffer = new StringBuffer();
  StringBuffer statusBuffer = new StringBuffer();
  String status = "";
  String displayStatus = "Ok";
  rrs.setLinkName("srmrm_"+currTestCondition);
  try {
    SrmRmResponse response = null;
    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmRm(r); 
      timeOutCallBack.setObject(response); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }
    status = printSrmRmResult(response,rrs,statusBuffer,explanationBuffer);
    if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
       status.equals("SRM_NOT_SUPPORTED")) {
       if(status.equals("SRM_NOT_SUPPORTED")) {
         displayStatus = "N.S.";
       }
    }
    else {
      displayStatus = "Failed";
    }
    if(displayStatus.equals("Ok")) {
       isRmSuccess.isOperOk(true);
    }
    else {
       isRmSuccess.setExplanation(displayStatus);
    }
  }catch(Exception e) {
    explanationBuffer.append(e.getMessage());
    status = "Exception";
    isRmSuccess.setExplanation("Exception");
    displayStatus = "Exception";
    rrs.setActualResult("Exception");
    rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
  }

  try {
       util.webScript(publishUrlStr,
	    displayType,"SendingSRMRM","SrmRm", rrs,
		"", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,displayType,
	    "SrmRm",statusBuffer.toString(),rrs,explanationBuffer.toString(),
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmRm",
	       displayStatus,dailyTest,numSites,localPublish,bs,bsa);
  }catch(Exception e) { e.printStackTrace(); }
  if(sLock != null)  {
    sLock.setIncrementCount();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLsTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doSrmLsTest(ISRM srm, StringBuffer proxyString, String uid,
    String source, SharedObjectLock sLock, OperOk lsOk, int testCondition) 
		throws  Exception {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.lsOk = lsOk;
  this.currTestCondition = testCondition;

  SrmLsRequest r = new SrmLsRequest(); 
  BrowseRequestStatus brs = util.createBrowseRequestStatus();
  brs.setBrowseUrl(surls[0]);

  if(changeToCopyServicePath) {
    brs.setSrmServicePath(copyServicePath);
  }

  util.printMessageHL("\nSRM-TESTER: " +
		new Date() + " Calling SrmLs ...\n", logger, pIntf);
  //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

  if(detailed) {
      if(testCondition == 0) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsFullDetailedListTest(uid,surls[0],brs,1);
      }
      else if(testCondition == 1) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsNumOfLevelsTest(uid,surls[0],brs,0);
      }
      else if(testCondition == 2) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsNumOfLevelsTest(uid,surls[0],brs,1);
      }
      else if(testCondition == 3) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsNumOfLevelsTest(uid,surls[0],brs,2);
      }
      else if(testCondition == 4) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsAllLevelRecursiveTest(uid,surls[0],brs);
      }
      else if(testCondition == 5) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsFileStorageTypeTest(uid,surls[0],brs,"volatile");
      }
      else if(testCondition == 6) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsFileStorageTypeTest(uid,surls[0],brs,"durable");
      }
      else if(testCondition == 7) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsFileStorageTypeTest(uid,surls[0],brs,"permanent");
      }
      else if(testCondition == 8) {
       util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsStorageSystemInfoTest(uid,surls[0],brs);
      }
      else {
       SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
       pTest.doSrmLsTest(uid,surls[0],brs);
      }
  } 
  else {
     SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
     pTest.doSrmLsTest(uid,surls[0],brs);
  }


  String ts = "srmLs";
  if(!typeString.equals("srmLs")) {
    ts = ts+"-"+typeString;
  }
  try {
     util.webScript(publishUrlStr,
	  displayType,"SendingBrowse","SrmLs Request now ...", brs,
	  "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, 
	  servicePath,noPublish, siteName,numOperations,typeString,
	  ts,"Sending",dailyTest,numSites,localPublish,bs,bsa);
  }catch(Exception ee) {
    util.printMessage("Exception " + ee.getMessage(), logger,pIntf);
	ee.printStackTrace(); 
  }

  String tempServicePath = servicePath;
  if(changeToCopyServicePath) {
     tempServicePath = copyServicePath;
  }

  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "SrmLs","FAILED",null,"Possibly SRM is down.",
	     timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, 
		 tempServicePath,noPublish, siteName,numOperations,
		 typeString,ts,"Failed",
		 dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp,startTimeStamp, 
	    operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
  }

  SrmLsResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  try {
    response = srm.srmLs(r);
    timeOutCallBack.setObject(response); 
  }catch(Exception e) {
    timeOutCallBack.interruptNow(true);
    throw e;
  }

  String turl = "";
  boolean reqOk = false;
  String notOkMsg = "";

  StringBuffer responseBuffer = new StringBuffer();
  StringBuffer expBuffer = new StringBuffer();
  HashMap statusArray = printLsResult(response,responseBuffer, expBuffer);
  if(response.getRequestToken() != null) {
    brs.setRequestId(response.getRequestToken().toString());
  }

  if(response == null || statusArray.size() == 0) {
     if(!responseBuffer.toString().equals("")) {
        notOkMsg=responseBuffer.toString();
     }
     reqOk = false;
  } 
  
  boolean doStatusTest = true;
  boolean testerTimedOut=false;

  TMetaDataPathDetail[] pathDetail = null;
  if(response.getDetails() != null) {
    pathDetail = response.getDetails().getPathDetailArray();
  }
  TStatusCode code = response.getReturnStatus().getStatusCode();

  int i = 0;
  int sTimeStamp = util.startTimeStamp();
  while (statusArray.size() > 0) {
    expBuffer = new StringBuffer();
    notOkMsg = "";
// AS 070809
// changed from 5000 to 30000
//viji, don't change this sleep time to statusWaitTime, which causes this
//test to timeout and halt after put and not process after ??????? 
    Thread.sleep(5000);
    if(!util.isRetryOk(sTimeStamp, retryTimeAllowed)) {
       util.printMessage("Status check exceeds maximum time", logger,pIntf);
       expBuffer.append("Notes from tester : Status check exceeds maximum time");
       notOkMsg="Notes from tester : Status check exceeds maximum time";
       testerTimedOut=true;
       break;
    }
    if(i >= statusArray.size()) {
       i = 0;
    }
    Object key = (statusArray.keySet().toArray()) [i];

    TReturnStatus status =
           (TReturnStatus)(statusArray.get(key));
    code = status.getStatusCode();

    //code = TStatusCode.SRM_REQUEST_QUEUED;
    StringBuffer rCode = new StringBuffer();
    if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
       (code == TStatusCode.SRM_REQUEST_QUEUED)) {
       expBuffer.delete(0,expBuffer.length());
       //if(doStatusTest && !detailed) {
         //checkLsStatusTest(srm,response.getRequestToken(),uid, 1);
       //}
       SrmStatusOfLsRequestResponse lsStatus = 
		   checkLsStatus (srm, response.getRequestToken(), 
				uid, rCode,expBuffer,doStatusTest,sLock);
       doStatusTest = false;
       if(lsStatus != null) {
        code = lsStatus.getReturnStatus().getStatusCode();
        pathDetail = lsStatus.getDetails().getPathDetailArray();
       }
       else {
          TReturnStatus returnStatus = new TReturnStatus();
          code = util.mapReturnStatusValueBackToCode(rCode);
       }
    }
    util.printMessage("\nFileStatus code=" + code,logger,pIntf);
    if(status != null) {
      util.printMessage("\nExplanation=" + status.getExplanation(),
			logger,pIntf);
    }
    String ts_1 = "srmStatusOfLsRequest";
    if(!typeString.equals("srmLs")) {
      ts_1 = ts_1+"-"+typeString;
    }
    if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
       (code != TStatusCode.SRM_REQUEST_QUEUED)) {
       if(code == TStatusCode.SRM_SUCCESS ||
        code == TStatusCode.SRM_DONE) {
        reqOk = true;
        statusArray.remove(key); 
        util.webScript(publishUrlStr,displayType,
         "Browse",code.getValue(),
          null,expBuffer.toString(), timeStamp,fullTimeStamp,startTimeStamp, 
		  operatorTimeStamp, tempServicePath,noPublish,
          siteName,numOperations,typeString,ts_1,
		  "Ok",dailyTest,numSites,localPublish,bs,bsa);
       }
      else {
        util.webScript(publishUrlStr,displayType,
         "Browse",code.getValue(),
          null,expBuffer.toString(), timeStamp,fullTimeStamp,startTimeStamp, 
		  operatorTimeStamp, tempServicePath,noPublish,
          siteName,numOperations,typeString,ts_1,
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
        if(code == null) {
          code = util.mapReturnStatusValueBackToCode(rCode);
        }
        if(!expBuffer.toString().equals("")) {
          brs.setExplanation(expBuffer.toString());
        }
        reqOk = false;
        statusArray.remove(key);
      }
    }
  }

  brs.setStatus(code.getValue());
  brs.setExplanation(expBuffer.toString());


  if(reqOk) {
    try {
     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setLinkName("browse_"+testCondition);
     rrs.setActualResult(code.getValue());
     rrs.setMatchingInfo("Yes");
     if(lsOk != null) {
       lsOk.isOperOk(true);
     }
     util.webScript(publishUrlStr,displayType,
     "Browse",code.getValue(),
      rrs,expBuffer.toString(), timeStamp,fullTimeStamp,
	  startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
      siteName,numOperations,typeString,ts+"-OverAll",
	  "Ok",dailyTest,numSites,localPublish,bs,bsa);
      if(lsOk != null) {
        lsOk.isOperOk(true);
      }
    }catch(Exception e) { e.printStackTrace(); }

    if(pathDetail != null) {
      for (int j = 0; j < pathDetail.length; j++) {
       try {
         DataPathStatus dps = 
			printMetaData("\t\t",pathDetail[j],brs,0,pIntf);
         brs.setDataPathStatus(dps);
       }catch(Exception e) {e.printStackTrace();} 
      }
    }
    try {
      if(lsOk != null) {
        lsOk.isOperOk(true);
      }
      util.webScript(publishUrlStr,
         displayType,"RequestSummary","",
         brs, expBuffer.toString(), timeStamp, fullTimeStamp,startTimeStamp, 
	     operatorTimeStamp, tempServicePath,noPublish,
         siteName,numOperations,typeString,ts+"-OverAll","Ok",dailyTest,
		 numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }
    if(sLock != null)  {
        sLock.setIncrementCount();
    }
  }
  else {
    if(lsOk != null) {
      lsOk.setExplanation("Failed");
    }
    try {
      ReportRequestStatus rrs = new ReportRequestStatus();
      if(changeToCopyServicePath) {
         rrs.setSrmServicePath(copyServicePath);
      }
      rrs.setLinkName("browse_"+testCondition);
      rrs.setActualResult(code.getValue());
      rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      util.webScript(publishUrlStr,displayType,
       "Browse",code.getValue(),
        rrs,expBuffer.toString(),
          timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, 
		  tempServicePath,noPublish,
          siteName,numOperations,typeString,ts+"-OverAll",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }

    pathDetail = response.getDetails().getPathDetailArray();
    if(pathDetail != null) {
      for (int j = 0; j < pathDetail.length; j++) {
       try {
         DataPathStatus dps = 
			printMetaData("\t\t",pathDetail[j],brs,0,pIntf);
         brs.setDataPathStatus(dps);
       }catch(Exception e) {e.printStackTrace();}
      }
    }
    try {
      if(testerTimedOut) {
        if(lsOk != null) {
          lsOk.setExplanation("TimedOut");
        }
        util.webScript(publishUrlStr,
        displayType,"SrmLs","TimedOut",
        null, notOkMsg, timeStamp, fullTimeStamp,
		startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,"srmStatusOfLsRequest",
		"TimedOut",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
        displayType,"RequestSummary","",
        brs, notOkMsg, timeStamp, fullTimeStamp,
		startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,ts+"-OverAll",
		"TimedOut",dailyTest,numSites,localPublish,bs,bsa);
      }
      else {
        if(lsOk !=null) {
          lsOk.setExplanation("Failed");
        } 
        util.webScript(publishUrlStr,
        displayType,"RequestSummary","Failed",
        brs, notOkMsg, timeStamp, fullTimeStamp,
	    startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,ts+"-OverAll","Failed",
		dailyTest,numSites,localPublish,bs,bsa);
      }
    }catch(Exception e) { e.printStackTrace(); }
    if(sLock != null)  {
        sLock.setIncrementCount();
    }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMetaData
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private DataPathStatus printMetaData(String prefix,
    TMetaDataPathDetail pDetail, BrowseRequestStatus brs, int count, 
		PrintIntf ppIntf) throws Exception {

  if(count > 10) return null;

  DataPathStatus dps = new DataPathStatus();


  if(pDetail.getPath() != null) {
    util.printMessage(prefix+"SURL        : " +
            pDetail.getPath().toString(),logger,ppIntf);
    dps.setSurl(pDetail.getPath().toString());
  }

  if(pDetail.getSize() != null) {
    util.printMessage(prefix+"Bytes       : " +
            pDetail.getSize(), logger,ppIntf);
    dps.setSize(""+pDetail.getSize());
  }

  if(pDetail.getType() != null) {
    util.printMessage(prefix+"FileType        : " +
            pDetail.getType().getValue(),logger,ppIntf);
    dps.setFileType(pDetail.getType().getValue());
  }

  if(pDetail.getFileStorageType() != null) {
     util.printMessage(prefix+"StorageType   : " +
          pDetail.getFileStorageType(), logger,ppIntf);
     dps.setStorageType(""+pDetail.getFileStorageType());
  }

  if(pDetail.getStatus() != null && pDetail.getStatus().getStatusCode() != null) {
    dps.setStatus(pDetail.getStatus().getStatusCode().getValue().toString());
    util.printMessage(prefix+"Status        : " +
            pDetail.getStatus().getStatusCode(), logger,ppIntf);
  }

  if(pDetail.getStatus().getExplanation() != null) {
    dps.setExplanation(pDetail.getStatus().getExplanation());
    util.printMessage(prefix+"Explanation   : " +
            pDetail.getStatus().getExplanation(), logger,ppIntf);
  }


  if(pDetail.getOwnerPermission() != null) {
    util.printMessage(prefix+"OwnerPermission        : " +
            pDetail.getOwnerPermission().getUserID(),logger,ppIntf);
    dps.setOwnerPermission(pDetail.getOwnerPermission().getUserID());
  }

  if(pDetail.getLifetimeLeft() != null) {
    util.printMessage(prefix+"LifetimeLeft        : " +
            pDetail.getLifetimeLeft().intValue(),logger,ppIntf);
    dps.setLifetimeLeft(""+pDetail.getLifetimeLeft().intValue());
  }

  if (pDetail.getLastModificationTime() != null) {
    Calendar gcal = pDetail.getLastModificationTime();
    Date dd = gcal.getTime();
    util.printMessage(prefix+"Last accessed: =" + dd.toString(), logger,ppIntf);
    int year = dd.getYear();
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
    util.printMessage(prefix+"     Year   : " + year, logger,ppIntf);
    util.printMessage(prefix+"     Month  : " + month, logger,ppIntf);
    util.printMessage(prefix+"     Day    : " + day, logger,ppIntf);
    util.printMessage(prefix+"     Hour   : " + hour, logger,ppIntf);
    util.printMessage(prefix+"     Minute : " + minute, logger,ppIntf);
    util.printMessage(prefix+"     Second : " + second, logger,ppIntf);
    */
    dps.setLastAccessed(year+":"+month+":"+day+":"+hour+":"+minute+":"+second);
  }

  if(pDetail.getArrayOfSubPaths() != null) {
    TMetaDataPathDetail[] subPDetail =
            pDetail.getArrayOfSubPaths().getPathDetailArray();
    int totalLength = count+subPDetail.length;
    if(brs != null) {
      brs.setTotalNumPathDetail(""+totalLength);
    }
    for(int i = 0; i<subPDetail.length;i++) {
       DataPathStatus dps1 = 
			printMetaData(prefix+"\t\t\t",subPDetail[i],brs,totalLength,ppIntf);
       if(dps1 != null) { 
         dps.addSubPath(dps1);
       }   
    }
  }
  return dps;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printLsResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private HashMap printLsResult (SrmLsResponse response,
        StringBuffer responseStatus, StringBuffer expBuffer) throws Exception {
   HashMap result = new HashMap();
   Date d = new Date ();
   String operatorTimeStamp = d.toString();
   
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..."+new Date(),logger,pIntf);
     util.printMessage("+++\tSrmLs Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }
   util.printMessage("SRM-TESTER:   ...Output from SRM..."+new Date(),logger,pIntf);
   if(response.getRequestToken() != null) {
     util.printMessage("request.token    : \t" +
        response.getRequestToken().toString(),logger,pIntf);
   }
   else {
     util.printMessage("request.token    : \t" + null,logger,pIntf);
   }
   String ts = "srmLs";
   if(!typeString.equals("srmLs")) {
     ts = ts+"-"+typeString;
   }

   String tempServicePath = servicePath;
   if(changeToCopyServicePath) {
     tempServicePath = copyServicePath;
   }
   if(response.getReturnStatus() == null) {
    try {
     util.webScript(publishUrlStr,displayType,
       "Browse","Failed",
        null,"SRM returned null return status", timeStamp,fullTimeStamp,
	    startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,ts,
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) {}
     util.printMessage("request.state : \t" + null,logger,pIntf);
   }
   else {
     util.printMessage("request.state : \t" +
        response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
     String displayStatusMsg = "Ok";
     if(response.getReturnStatus().getStatusCode() == 
          TStatusCode.SRM_SUCCESS ||
        response.getReturnStatus().getStatusCode() == 
          TStatusCode.SRM_DONE ||
        response.getReturnStatus().getStatusCode() == 
          TStatusCode.SRM_NOT_SUPPORTED ||
        response.getReturnStatus().getStatusCode() == 
          TStatusCode.SRM_REQUEST_QUEUED ||
        response.getReturnStatus().getStatusCode() == 
          TStatusCode.SRM_REQUEST_INPROGRESS)
     {
       if(response.getReturnStatus().getStatusCode() ==
			TStatusCode.SRM_NOT_SUPPORTED) {
         displayStatusMsg="N.S.";
       }
     }
     else {
        displayStatusMsg="Failed";
     }
     String exp = "";
     if(response.getReturnStatus().getExplanation() != null) {
       exp = response.getReturnStatus().getExplanation();
     }
     try {
      util.webScript(publishUrlStr,displayType,
       "Browse",response.getReturnStatus().getStatusCode().toString(),
        null,exp, timeStamp,fullTimeStamp,
	    startTimeStamp, operatorTimeStamp, tempServicePath,noPublish,
        siteName,numOperations,typeString,ts,
		displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) {}
     if(response.getReturnStatus().getExplanation() != null) {
       expBuffer.append(response.getReturnStatus().getExplanation());
     }
     if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_AUTHENTICATION_FAILURE) {
       responseStatus.append("SRM_AUTHENTICATION_FAILURE");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_AUTHORIZATION_FAILURE) {
       responseStatus.append("SRM_AUTHORIZATION_FAILURE");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_INVALID_REQUEST) {
       responseStatus.append("SRM_INVALID_REQUEST");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_INVALID_PATH) {
       responseStatus.append("SRM_INVALID_PATH");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_TOO_MANY_RESULTS) {
       responseStatus.append("SRM_TOO_MANY_RESULTS");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NOT_SUPPORTED) {
       responseStatus.append("SRM_NOT_SUPPORTED");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_INTERNAL_ERROR) {
       responseStatus.append("SRM_INTERNAL_ERROR");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE) {
       responseStatus.append("SRM_FAILURE");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_PARTIAL_SUCCESS) {
       responseStatus.append("SRM_PARTIAL_SUCCESS");
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS) {
       responseStatus.append("SRM_SUCCESS");
     }
   }


   if(response.getDetails() != null) {
     if(response.getDetails().getPathDetailArray() != null) {

        TMetaDataPathDetail[] pathDetail = response.getDetails().getPathDetailArray();
        int size = pathDetail.length;
        for (int i = 0; i < size; i++) {
           result.put(pathDetail[i].getPath(),pathDetail[i].getStatus());
          try {
           DataPathStatus dps = 
				printMetaData("\t\t",pathDetail[i],null,0,pIntf);
          }catch(Exception e) {e.printStackTrace();}
        }
     }
  }

  util.printMessage("===========================================",logger,pIntf);
  //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);

  return result;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMkdirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doMkdirTest(ISRM srm, StringBuffer proxyString, String uid,
    String source, SharedObjectLock sLock, OperOk mkdirOk, int testCondition) 
		throws  Exception {

  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.currTestCondition = testCondition;

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
  util.printMessage(" ++++ Sending \"Mkdir\" request +++++ ",logger,pIntf);

  SrmMkdirRequest r = new SrmMkdirRequest(); 
  ReportRequestStatus rrs = util.createReportRequestStatus(source,"","");

  try {
    if(detailed) {
      if(testCondition == 0) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doMkdirStorageTest(uid,surls[0],rrs);
      }
      else {
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doMkdirTest(uid,surls[0],rrs);
      }
    } 
    else {
     SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
     pTest.doMkdirTest(uid,surls[0],rrs);
    }
  }catch(Exception ee) {
    util.printMessage("Exception " + ee.getMessage(), logger,pIntf);
    mkdirOk.setExplanation("Exception");
    ee.printStackTrace();
  }


  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "Mkdir","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmMkdir","Sending",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
  }


  StringBuffer explanationBuffer = new StringBuffer();
  String status = "";
  rrs.setLinkName("mkdir_"+currTestCondition);
  String displayStatus = "Ok";
  try {
    SrmMkdirResponse response = null;
    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();
    try {
      response = srm.srmMkdir(r); 
      timeOutCallBack.setObject(response); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    } 
    status = printMkDirResult(response,rrs,explanationBuffer);
    if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
       status.equals("SRM_NOT_SUPPORTED")) {
      if(status.equals("SRM_NOT_SUPPORTED")) {
        displayStatus = "N.S.";
      }
    }
    else {
      displayStatus = "Failed";
    }
    if(displayStatus.equals("Ok")) {
      mkdirOk.isOperOk(true);
    }
    else {
      mkdirOk.setExplanation(displayStatus);
    }
  }catch(Exception e) {
    mkdirOk.setExplanation("Exception");
    explanationBuffer.append(e.getMessage());
    status = "Exception";
    displayStatus = "Exception";
    rrs.setActualResult("Exception");
    rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
  }

   try {
       util.webScript(publishUrlStr,
	    displayType,"SendingMKDIR","Mkdir", rrs,
		"", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,displayType,
	    "Mkdir",status,rrs,explanationBuffer.toString(),
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmMkdir",
	       displayStatus,dailyTest,numSites,localPublish,bs,bsa);
   }catch(Exception e) { e.printStackTrace(); }
   if(sLock != null)  {
     sLock.setIncrementCount();
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMkdirResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String printMkDirResult (
	SrmMkdirResponse response, ReportRequestStatus rrs, 
	  StringBuffer explanationBuffer) {

   if(response == null) {
     util.printMessage("==========================================",logger,pIntf);
     util.printMessage("+++\tMkdirRequest Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     explanationBuffer.append("Null response from SRM");
     return "";
   }

   util.printMessage("==========================================",logger,pIntf);
   if(response.getReturnStatus() != null) {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
         
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       util.printMessage("request.explanation \t" + explanation,logger,pIntf);
       explanationBuffer.append(explanation); 
     }
     String status = response.getReturnStatus().getStatusCode().getValue();
     rrs.setActualResult(status);
     if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE")) {
       rrs.setMatchingInfo("Yes");
     }
     else {
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     }
     return status;
   }
   else {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     rrs.setActualResult("Null state");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     explanationBuffer.append("Null state from SRM");
     return "";
   }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printRmdirResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String printRmDirResult (
	SrmRmdirResponse response, ReportRequestStatus rrs, 
	  StringBuffer explanationBuffer) {

   if(response == null) {
     util.printMessage("+++\tRmdirRequest Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     explanationBuffer.append("Null response from SRM");
     return "";
   }

   util.printMessage("==========================================",logger,pIntf);
   if(response.getReturnStatus() != null) {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
         
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       util.printMessage("request.explanation \t" + explanation,logger,pIntf);
       explanationBuffer.append(explanation); 
     }
     String status = response.getReturnStatus().getStatusCode().getValue();
     rrs.setActualResult(status);
     if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE")) {
       rrs.setMatchingInfo("Yes");
     }
     else {
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     }
     return status;
   }
   else {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     rrs.setActualResult("Null state");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     explanationBuffer.append("Null state from SRM");
     return "";
   }

}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMvTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doMvTest(ISRM srm, StringBuffer proxyString, String uid,
    String source, String target, SharedObjectLock sLock, 
		OperOk mvOk, int testCondition) throws  Exception {

  String[] surls = null;
  String[] turls = null;
  surls = new String[1];
  turls = new String[1];
  surls [0] = new String(source.trim());
  turls [0] = new String(target.trim());

  this.currTestCondition = testCondition;

  util.printMessage(" ++++ Sending \"Mv\" request +++++ ",logger,pIntf);

  SrmMvRequest r = new SrmMvRequest(); 
  ReportRequestStatus rrs = util.createReportRequestStatus(source,target,"");

  Date d = new Date ();
  String operatorTimeStamp = d.toString();

  try {
    if(detailed) {
      if(testCondition == 0) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doMvStorageTest(uid,surls[0],turls[0],rrs);
      }
      else {
        SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
        pTest.doMvTest(uid,surls[0],turls[0],rrs);
      }
    } 
    else {
     SRMDirParams pTest = new SRMDirParams(r,logger,proxyString,pIntf);
     pTest.doMvTest(uid,surls[0],turls[0],rrs);
    }
  }catch(Exception ee) {
    util.printMessage("Exception : " + ee.getMessage(), logger,pIntf);
    mvOk.setExplanation("Exception");
    ee.printStackTrace();
  }


  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "SrmMv","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmMv","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
  }


  StringBuffer explanationBuffer = new StringBuffer();
  String status = "";
  String displayStatus = "Ok";
  rrs.setLinkName("mv_"+currTestCondition);
  try {
    SrmMvResponse response = null;
    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmMv(r); 
      timeOutCallBack.setObject(response); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }
    status = printMvResult(response,rrs,explanationBuffer);
    if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
       status.equals("SRM_NOT_SUPPORTED")) {
      if(status.equals("SRM_NOT_SUPPORTED")) {
        displayStatus = "N.S.";
      }
    }
    else {
      displayStatus = "Failed";
    }
    if(displayStatus.equals("Ok")) {
      mvOk.isOperOk(true);
    }
    else {
      mvOk.setExplanation(displayStatus);
    }
  }catch(Exception e) {
    mvOk.setExplanation("Exception");
    explanationBuffer.append(e.getMessage());
    status = "Exception";
    displayStatus = "Exception";
    rrs.setActualResult("Exception");
    rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
  }

   try {
       util.webScript(publishUrlStr,
	    displayType,"SendingMV","SrmMv", rrs,
		"", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,displayType,
	    "SrmMv",status,rrs,explanationBuffer.toString(),
		   timeStamp,fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmMv",
	       displayStatus,dailyTest,numSites,localPublish,bs,bsa);
   }catch(Exception e) { e.printStackTrace(); }
   if(sLock != null)  {
     sLock.setIncrementCount();
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printSrmRmResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String printSrmRmResult (
	SrmRmResponse response, ReportRequestStatus rrs, 
	  StringBuffer statusBuffer, StringBuffer explanationBuffer) throws Exception {

   if(response == null) {
     util.printMessage("==========================================",logger,pIntf);
     util.printMessage("+++\tSrmRm Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     explanationBuffer.append("Null response from SRM");
     return "";
   }

   util.printMessage("==========================================",logger,pIntf);
   if(response.getReturnStatus() != null) {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
         
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       util.printMessage("request.explanation \t" + explanation,logger,pIntf);
       explanationBuffer.append(explanation); 
     }
     ArrayOfTSURLReturnStatus arrayOfRStatus = response.getArrayOfFileStatuses();
     if(arrayOfRStatus != null) {
        TSURLReturnStatus[] stauses = arrayOfRStatus.getStatusArray();
        for(int i = 0; i < stauses.length; i++) {
           TSURLReturnStatus rStatus = stauses[i];
           if(rStatus != null) {
             util.printMessage("Surl : \t" + rStatus.getSurl(), logger,pIntf);
             String temp = rStatus.getStatus().getStatusCode().getValue();
             util.printMessage("FileStatus : \t"+ temp, logger,pIntf);
             util.printMessage("FileStatus Explanation : \t"+ 
				rStatus.getStatus().getExplanation(), logger,pIntf);
             rrs.setActualResult(temp);
             statusBuffer.append(temp);
             if(temp.equals("SRM_SUCCESS") || temp.equals("SRM_DONE") ||
				temp.equals("SRM_NOT_SUPPORTED")) {
               rrs.setMatchingInfo("Yes");
             }
             else {
               rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
             }
             return temp; 
           }
           else {
             String status = response.getReturnStatus().getStatusCode().getValue();
             statusBuffer.append(status);
             rrs.setActualResult(status);
             if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
			    status.equals("SRM_NOT_SUPPORTED")) {
               rrs.setMatchingInfo("Yes");
             }
             else {
               rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
             }
             return status;
           }
        }
     }
     else {
       String status = response.getReturnStatus().getStatusCode().getValue();
       statusBuffer.append(status);
       rrs.setActualResult(status);
       if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
			status.equals("SRM_NOT_SUPPORTED")) {
         rrs.setMatchingInfo("Yes");
       }
       else {
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
       }
       return status;
     }
   }
   else {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     rrs.setActualResult("Null state");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
     explanationBuffer.append("Null state from SRM");
   }
   return "";
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMvResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String printMvResult (
	SrmMvResponse response, ReportRequestStatus rrs, 
	  StringBuffer explanationBuffer) {

   if(response == null) {
     util.printMessage("==========================================",logger,pIntf);
     util.printMessage("+++\tMvRequest Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
     explanationBuffer.append("Null response from SRM");
     return "";
   }

   util.printMessage("==========================================",logger,pIntf);
   if(response.getReturnStatus() != null) {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
         
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       util.printMessage("request.explanation \t" + explanation,logger,pIntf);
       explanationBuffer.append(explanation); 
     }
     String status = response.getReturnStatus().getStatusCode().getValue();
     rrs.setActualResult(status);
     if(status.equals("SRM_SUCCESS") || status.equals("SRM_DONE") ||
		status.equals("SRM_NOT_SUPPORTED")) {
       rrs.setMatchingInfo("Yes");
     }
     else {
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
     }
     return status;
   }
   else {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     rrs.setActualResult("Null state");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
     explanationBuffer.append("Null state from SRM");
     return "";
   }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkLsStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SrmStatusOfLsRequestResponse checkLsStatus
    (ISRM srm, String token, String uid, StringBuffer rCode, 
	 StringBuffer expBuffer, boolean statusTest, SharedObjectLock sLock) 
		throws URI.MalformedURIException, java.rmi.RemoteException,Exception {

    Date d = new Date ();
    String operatorTimeStamp = d.toString();

    if(token == null)  {
      throw new java.rmi.RemoteException("Request token from server cannot be null.");
    }

    if(statusTest) {
      util.printMessage("\nSending LsStatus for requestToken " + token, logger,pIntf);
      util.printMessage("\nAuthorizationID " + uid, logger,pIntf);
    }

    SrmStatusOfLsRequestRequest r = new SrmStatusOfLsRequestRequest();

    r.setRequestToken(token);
    if(!uid.equals("")) {
      r.setAuthorizationID(uid);
    }

    SrmStatusOfLsRequestResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setRequestToken(token,srm);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmStatusOfLsRequest(r);
      timeOutCallBack.setObject(result); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }

    String tempServicePath = servicePath;
    if(changeToCopyServicePath) {
      tempServicePath = copyServicePath;
    }
   
    String ts = "srmStatusOfLsRequest";
    if(!typeString.equals("srmLs")) {
      ts = ts+"-"+typeString;
    }

    if(result == null) {
      try {
       util.webScript(publishUrlStr,
        displayType,"LsStatusMethod","SRM returned null status", null,
        "SRM returned null status",
        timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, 
		tempServicePath,noPublish, siteName,numOperations,
		typeString,ts,
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      return result;
    }
    else {
       util.printMessage("Status from SRM (checkLsStatus) " +
          result.getReturnStatus().getStatusCode(), logger,pIntf);
       String explanation = result.getReturnStatus().getExplanation();
       if(explanation != null) {
         util.printMessage("Explanation=" + explanation, logger,pIntf);
         expBuffer.append(explanation);
       }

       if (result.getReturnStatus().getStatusCode() ==
           TStatusCode.SRM_TOO_MANY_RESULTS) {
         rCode.append("SRM_TOO_MANY_RESULTS");
       }
       else if (result.getReturnStatus().getStatusCode() ==
           TStatusCode.SRM_INTERNAL_ERROR) {
         rCode.append("SRM_INTERNAL_ERROR");
       }
       else if (result.getReturnStatus().getStatusCode() ==
           TStatusCode.SRM_PARTIAL_SUCCESS) {
         rCode.append("SRM_PARTIAL_SUCCESS");
       }
       else if (result.getReturnStatus().getStatusCode() ==
           TStatusCode.SRM_SUCCESS) {
         rCode.append("SRM_SUCCESS");
       }
       else {
         if (result.getReturnStatus().getStatusCode() ==
              TStatusCode.SRM_AUTHENTICATION_FAILURE) {
            rCode.append("SRM_AUTHENTICATION_FAILURE");
         }
         else if (result.getReturnStatus().getStatusCode() ==
             TStatusCode.SRM_AUTHORIZATION_FAILURE) {
            rCode.append("SRM_AUTHORIZATION_FAILURE");
         }
         else if (result.getReturnStatus().getStatusCode() ==
             TStatusCode.SRM_INVALID_REQUEST) {
            rCode.append("SRM_INVALID_REQUEST");
         }
         else if (result.getReturnStatus().getStatusCode() ==
             TStatusCode.SRM_NOT_SUPPORTED) {
            rCode.append("SRM_NOT_SUPPORTED");
         }
         else if (result.getReturnStatus().getStatusCode() ==
             TStatusCode.SRM_FAILURE) {
            rCode.append("SRM_FAILURE");
         }

         try {
           util.webScript(publishUrlStr,
             displayType,"LsStatusMethod","No", null, explanation,
             timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp,
			 tempServicePath,noPublish, siteName,numOperations,
		     typeString,ts,
	         "Failed",dailyTest,numSites,localPublish,bs,bsa);
         }catch(Exception e) { e.printStackTrace(); }
        return null; 
      }//end else

      //the status seems to be ok here with ok status codes (see above)
      if(result.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_REQUEST_QUEUED ||
         result.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_REQUEST_INPROGRESS) {
         ;
      }
      else {
        try {
         util.webScript(publishUrlStr,
            displayType,"LsStatusMethod","Yes", null, explanation,
            timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, 
		    tempServicePath,noPublish, siteName,numOperations,
			typeString,ts,
	        "Ok",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
      }
      return result;
    }
}

public void  checkLsStatusTest
    (ISRM srm, String token, String uid, int condition, SharedObjectLock sLock)
      throws URI.MalformedURIException, java.rmi.RemoteException,Exception {

   Date d = new Date ();
   String operatorTimeStamp = d.toString();
   if(token == null)  {
     throw new java.rmi.RemoteException("Request token from server cannot be null.");
   }

   if(condition == 1) {
     util.printMessage("\nSending LsStatus for Bad requestToken " +
            token+"_bad_token", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_INVALID_REQUEST", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
         rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token+"_bad_token");
     //rrs.setExpectedResult("SRM_INVALID_REQUEST");


     SrmStatusOfLsRequestRequest r = new SrmStatusOfLsRequestRequest();
     r.setRequestToken(token+"_bad_token");

     SrmStatusOfLsRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
      result = srm.srmStatusOfLsRequest(r);
      timeOutCallBack.setObject(result); 
     }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
     }

     if(result != null) {
       util.printMessage("\nStatus from SRM (checkLsStatusTest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
       String explanation = result.getReturnStatus().getExplanation();
       if(explanation == null) {
         explanation = "";
       }
       util.printMessage("Explanation="+explanation,logger,pIntf);

      if(result.getReturnStatus().getStatusCode() ==
        TStatusCode.SRM_INVALID_REQUEST ||
        result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE)
      {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadtoken_"+currTestCondition);

        try {
         util.webScript(publishUrlStr,
           displayType,"SendingLSSTATUS","LS Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Ok",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"LsStatus","Yes", rrs,
                    explanation,
             timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfLsRequest",
	       "Ok",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {

        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
        rrs.setLinkName("statusbadtoken_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingLSSTATUS","Ls Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"LsStatus",
            "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE", rrs,
            explanation,
            timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfLsRequest",
		    "Failed",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST");
      rrs.setLinkName("statusbadtoken_"+currTestCondition);
      try {
       util.webScript(publishUrlStr,
           displayType,"SendingLSSTATUS","Ls Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
        displayType,"LsStatus","SRM returned null status", rrs,
        "SRM returned null status",
        timeStamp, fullTimeStamp,startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmStatusOfLsRequest",
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
   }
}

}
