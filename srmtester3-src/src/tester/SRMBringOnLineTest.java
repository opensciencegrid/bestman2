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

import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMBringOnLineTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMBringOnLineTest
{
   private String publishUrlStr;
   private String displayType;
   private String testType;
   private String timeStamp;
   private String fullTimeStamp;
   private String servicePath;
   private GSSCredential credential;
   private String proxyFile;
   private boolean onlyGsiFTPGet;
   private boolean noPublish;
   private boolean detailed;
   private boolean threepartycopy;
   private boolean defaultProxy;
   private boolean advisoryDelete;
   private boolean dcau=true;
   private int bufferSize;
   private int parallelism; 
   private int currTestCondition;
   private Log logger;
   private String spaceToken="";
   private boolean reserveSpace;
   private boolean alreadyReleasedSpace;
   private String siteName;
   private int numOperations;
   private boolean dailyTest;
   private String startTimeStamp;
   private int numSites;
   private int retryTimeAllowed;
   private int statusWaitTime=15; //default 15 seconds
   private String typeString="srmBringOnline";
   private String uid="";
   private PrintIntf pIntf;
   private boolean localPublish;
   private BufferedWriter bs;
   private BufferedWriter bsa;
   private OperOk bringOnlineSuccess;
   private int desiredFileLifeTime;
   private TimeOutCallBack timeOutCallBack;
   private boolean useDriverOn;
   private OperOk pingOverAllSuccess;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMBringOnLineTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMBringOnLineTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp,
	   String servicePath, GSSCredential credential, String proxyFile, 
	   boolean onlyGsiFTPGet, boolean noPublish, 
	   boolean detailed, boolean threepartycopy, boolean defaultProxy, 
	   boolean advisoryDelete, boolean dcau, int bufferSize, 
       int parallelism, String siteName, int numOperations, String startTimeStamp, 
	   boolean dailyTest, int numSites,
	   int retryTimeAllowed, int statusWaitTime, boolean localPublish,
       int desiredFileLifeTime,
       BufferedWriter bs, BufferedWriter bsa, Log logger, 
	   PrintIntf pIntf, boolean useDriverOn, OperOk pingOverAllSuccess) {

   this.publishUrlStr = publishUrlStr;
   this.displayType = displayType;
   this.testType = testType;
   this.timeStamp = timeStamp;
   this.fullTimeStamp = fullTimeStamp;
   this.servicePath = servicePath;
   this.credential = credential;
   this.proxyFile = proxyFile;
   this.onlyGsiFTPGet = onlyGsiFTPGet;
   this.noPublish = noPublish;
   this.detailed = detailed;
   this.threepartycopy = threepartycopy;
   this.defaultProxy = defaultProxy;
   this.advisoryDelete = advisoryDelete;
   this.dcau = dcau;
   this.bufferSize = bufferSize;
   this.parallelism = parallelism;
   this.desiredFileLifeTime = desiredFileLifeTime;
   this.siteName = siteName;
   this.numOperations = numOperations;
   this.dailyTest = dailyTest;
   this.startTimeStamp = startTimeStamp;
   this.numSites = numSites;
   this.retryTimeAllowed = retryTimeAllowed;
   this.statusWaitTime = statusWaitTime * 1000;
   this.logger = logger;
   this.pIntf = pIntf;
   this.localPublish = localPublish;
   this.bs = bs;
   this.bsa = bsa;
   this.useDriverOn = useDriverOn;
   this.pingOverAllSuccess = pingOverAllSuccess;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBringOnLineFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doBringOnLineFile(ISRM srm, StringBuffer proxyString, String uid,
    String source, String localTarget, String remoteTarget, boolean plainGet, 
	SharedObjectLock sLock, String sToken, OperOk bringOnlineSuccess, int testCondition) 
		throws  Exception {

 
  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.bringOnlineSuccess = bringOnlineSuccess;
  this.currTestCondition = testCondition;
  this.uid = uid;

  Date d = new Date ();
  String operatorTimeStamp = d.toString();

  util.printMessageHL("\nSRM-TESTER: " + 
		new Date() + " Calling BringOnline request ...\n",logger,pIntf);
  //util.printMessageHL2("StartTime="+new Date()+"\n", logger,pIntf);

  SrmBringOnlineRequest r = new SrmBringOnlineRequest(); 
  ReportRequestStatus rrs = 
		util.createReportRequestStatus(source,localTarget,"");

  if(!sToken.equals("")) {
    this.spaceToken = sToken;
    r.setTargetSpaceToken(spaceToken);
    rrs.setSpaceToken(spaceToken);  
  }

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI(surls[0]);


  if(detailed) {
      if(testCondition == 0) {
        util.printMessage("\n Running Advanced Test ",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
		  	   bufferSize, dcau, pIntf);
        pTest.doTestSimple(surls[0],"user description test string",rrs);
      }
      else if (testCondition == 1) {
        util.printMessage("\nRunning Advanced Test ",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
		  	   bufferSize, dcau, pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.VOLATILE,rrs);
      }
      else if (testCondition == 2) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
		  	   bufferSize, dcau, pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.PERMANENT,rrs);
      }
      else if (testCondition == 3) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
		    new SRMPrepareToBringOnLineTest(r,logger,proxyString,
		  	   bufferSize, dcau, pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.DURABLE,rrs);
      }
      else if (testCondition == 4) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
			   bufferSize, dcau, pIntf);
        pTest.doTestRequestTime
			(surls[0],"user description test string", 60,0,rrs);
      }
      else if (testCondition == 5) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
		  	   bufferSize, dcau, pIntf);
        pTest.doTestRequestTime
			(surls[0],"user description test string",0,60,rrs);
      }
      else if (testCondition == 6) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
			   bufferSize, dcau, pIntf);
        pTest.doTestStorageParam
			(surls[0],"user description test string", proxyString,uid,rrs);
      }
      else if (testCondition == 7) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
			   bufferSize, dcau, pIntf);
        pTest.doTestTransferParam
			(surls[0],"user description test string",rrs);
      }
      else if (testCondition == 8) {
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
			   bufferSize, dcau, pIntf);
        pTest.doTestRetentionPolicy (surls[0],"user description test string",rrs);
      }
      else if (testCondition == 9) {
        reserveSpace=true;
        util.printMessage("\n+++ Running Advanced Test ++++",logger,pIntf);
        rrs = util.createReportRequestStatus("","","");
        rrs.setUserId(uid);
        rrs.setUserDescription("test description");
        rrs.setTotalSpace(""+1000);
        rrs.setGuarnSpace(""+1000);
        rrs.setLifeTime(""+10000);
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
			   bufferSize, dcau, pIntf);
        SrmReserveSpaceRequest spaceRequest = new SrmReserveSpaceRequest();
        if(!uid.equals("")) {
          spaceRequest.setAuthorizationID(uid);
        }
        spaceRequest.setUserSpaceTokenDescription("test description");
        spaceRequest.setDesiredSizeOfTotalSpace(new UnsignedLong(100000000));
        spaceRequest.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(1000));
        spaceRequest.setDesiredLifetimeOfReservedSpace(new Integer(10000));
        util.webScript(publishUrlStr,
	         displayType,"SendingRESERVESPACE","Sending ReserveSpace", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, 
		     operatorTimeStamp,servicePath,noPublish,
             siteName,numOperations,typeString,"srmReserveSpace",
		     "Sending",dailyTest,numSites,localPublish,bs,bsa);

        SrmReserveSpaceResponse spaceResponse = null;

        int ssTimeStamp = util.startTimeStamp();
        timeOutCallBack = new TimeOutCallBack(spaceResponse,ssTimeStamp);
        timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
        timeOutCallBack.setSharedObjectLock(sLock);
        timeOutCallBack.start();

        try {
          spaceResponse = srm.srmReserveSpace(spaceRequest);
          timeOutCallBack.setObject(spaceResponse); 
        }catch(Exception e) {
          timeOutCallBack.interruptNow(true);
          throw e;
        }
        if(spaceResponse != null) {
          rrs.setLinkName("reservespace_"+testCondition); 
          if(spaceResponse.getReturnStatus() != null) {
            util.printMessage("StatusCode " + 
				spaceResponse.getReturnStatus().getStatusCode(),logger,pIntf);
            rrs.setActualResult(
				spaceResponse.getReturnStatus().getStatusCode().getValue());
            String displayStatusMsg = "Ok";
            if(spaceResponse.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_SUCCESS ||
               spaceResponse.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_DONE ||
		       spaceResponse.getReturnStatus().getStatusCode() ==
		        TStatusCode.SRM_NOT_SUPPORTED) {
              rrs.setMatchingInfo("Yes");
              if(spaceResponse.getReturnStatus().getStatusCode() ==	
                TStatusCode.SRM_NOT_SUPPORTED) {
                  displayStatusMsg = "N.S.";
              }
             }
             else {
                displayStatusMsg = "Failed";
             }
             try {
              util.webScript(publishUrlStr,
	            displayType,"ReserveSpace","Sending ReserveSpace", null,
		        "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			    servicePath,
			    noPublish, siteName,numOperations,typeString,"srmReserveSpace",
			    displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
             }catch(Exception e) {e.printStackTrace();}
          }
          else {
            rrs.setActualResult("Null status");
            rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
          }
          spaceToken = spaceResponse.getSpaceToken();
          util.printMessage("\n\nSpaceToken " + spaceToken,logger,pIntf);
          if(spaceToken == null) spaceToken = "";
          if(spaceToken.equals("")) {
            rrs.setSpaceToken("empty");
          }
          else {
            rrs.setSpaceToken(spaceToken);
          }
          rrs.setLinkName("reservespace_"+testCondition); 
          if(spaceToken.equals("")) {
            util.webScript(publishUrlStr,
	           displayType,"ReserveSpace","Failed", rrs,
		       "SRM returned null space token", timeStamp, fullTimeStamp, 
               startTimeStamp,operatorTimeStamp,
			   servicePath,noPublish, siteName,numOperations,typeString,
			   "srmReserveSpace", "Sending",dailyTest,numSites,localPublish,bs,bsa);
          } 
          else {
            util.webScript(publishUrlStr,
	           displayType,"ReserveSpace","Sending", rrs,
		       "", timeStamp, fullTimeStamp, startTimeStamp, 
			   operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,"srmReserveSpace",
			   "Sending",dailyTest,numSites,localPublish,bs,bsa);
          }
          if(spaceToken != null) {
            rrs = util.createReportRequestStatus(source,localTarget,"");
            pTest.doReserveSpaceGet(surls[0],spaceToken,rrs);
          }
          else { 
            util.printMessage("Cannot do this test since spaceToken is null", 
				logger,pIntf);
          }
        }
        else {
          util.printMessage("Cannot do this test since spaceResponse is null", 
				logger,pIntf);
        }
      }
      else if(testCondition == -1) {
        SRMPrepareToBringOnLineTest pTest = 
			new SRMPrepareToBringOnLineTest(r,logger,proxyString,
				bufferSize, dcau, pIntf);
        pTest.doTestBasic (surls[0],desiredFileLifeTime);
      }
  } 
  else {
     SRMPrepareToBringOnLineTest pTest = 
		new SRMPrepareToBringOnLineTest(r,logger,proxyString, 
			bufferSize, dcau, pIntf);
     pTest.doTestBasic (surls[0],desiredFileLifeTime);
  }

  if(plainGet) {
   try {
     if(detailed) {
      util.webScript(publishUrlStr,
	   displayType,"Sending","BringOnline Request now for Advanced Test ...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline","Sending",dailyTest,numSites,localPublish,bs,bsa);
     } 
     else {
      util.webScript(publishUrlStr,
	   displayType,"Sending","BringOnline Request now for Basic Test ...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline","Sending",dailyTest,numSites,localPublish,bs,bsa);
     }
    }catch(Exception e) { e.printStackTrace(); }
  }
  else {
   try {
     util.webScript(publishUrlStr,
	   displayType,"Sending","BringOnline Request now...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline","Sending",dailyTest,numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }
  }

  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "BringOnline","FAILED",null,"Possibly SRM is down.",
		timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline",
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline","Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
     return "";
  }


  if(plainGet) { 
    //check local target dir exists
    int tindex = localTarget.lastIndexOf("/");
    if(tindex != -1) {
     int jj = localTarget.indexOf("file:");
     if(jj != -1) {
       String tt = localTarget.substring(0+5,tindex);
       File tf = new File (tt);
       if(!tf.exists()) {
          util.printMessage
		   ("Cannot do BringOnline request, given target directory " +
			tt + " did not exists.", logger,pIntf);

          try {
             rrs = new ReportRequestStatus();
             rrs.setLinkName("gettargetdirexp");
             util.webScript (publishUrlStr,displayType,
 		       "Exception","",rrs,
	           "Cannot do BringOnline request, given target directory " + tt +
			    " did not exists.",timeStamp,fullTimeStamp, 
			    startTimeStamp, operatorTimeStamp,servicePath,noPublish,
                siteName,numOperations,typeString,"srmBringOnline","Exception",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
        }
      }
    }
  }

  SrmBringOnlineResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
  timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  try {
    response = srm.srmBringOnline(r); 
    timeOutCallBack.setObject(response); 
  }catch(Exception e) { 
     timeOutCallBack.interruptNow(true);
          throw e;
  }

  String turl = "";
  String size = ""+0;
  boolean reqOk = false;
  String expMsg = "";


  StringBuffer responseBuffer = new StringBuffer();
  HashMap statusArray = printBringOnlineResult(response,responseBuffer);
  StringBuffer expBuffer = new StringBuffer();

  if(response == null || statusArray == null || statusArray.size() == 0) {
    reqOk = false;
    if(!responseBuffer.toString().equals("")) {
      expMsg = responseBuffer.toString();
    }
  }

  if(response != null && response.getReturnStatus() != null 
	&&  response.getReturnStatus().getStatusCode() != null ) {
    if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
       response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ) {
         reqOk=true;
    }
  }

  
  URI keyURL = null;
  boolean doStatusTest = true;
  boolean testerTimedOut=false;
  try {
  int i = 0;
  int sTimeStamp = util.startTimeStamp();
  while (statusArray.size() > 0) {
      expBuffer = new StringBuffer();
// AS 070809
// changed from 5000 to 60000
      Thread.sleep(statusWaitTime);
      if(!util.isRetryOk(sTimeStamp, retryTimeAllowed)) {
         util.printMessage("Status check exceeds max retry time",logger,pIntf);
         expBuffer.append("Notes from tester : status check exceeds time out");
         testerTimedOut=true;
         break;
      }
      if(i >= statusArray.size()) {
        i = 0;
      }
      Object key = (statusArray.keySet().toArray()) [i];
      IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
      TBringOnlineRequestFileStatus status =
           (TBringOnlineRequestFileStatus)(statusArray.get(key));
      TStatusCode code = status.getStatus().getStatusCode();
      StringBuffer rCode = new StringBuffer();
      expBuffer.delete(0,expBuffer.length());
      if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
       (code == TStatusCode.SRM_REQUEST_QUEUED)) {
       keyURL = (URI)key;
       /*
       if(doStatusTest && !detailed && !threepartycopy) {
         checkBringOnlineStatusTest(srm,(URI)key,response,1,sLock);
         checkBringOnlineStatusTest(srm,(URI)key,response,2,sLock);
       }
       */
       System.out.println("SRM-TESTER: Calling BringOnline Status " + new Date());
       status = checkBringOnlineStatus(srm,
			(URI)key,response, rCode,doStatusTest,expBuffer,
		     wholeRequestFailed,sLock);
       doStatusTest = false;
       if(status != null) {
        if(wholeRequestFailed.getWholeRequestFailed()) {
          TReturnStatus returnStatus = new TReturnStatus(); 
          returnStatus.setStatusCode(TStatusCode.SRM_FAILURE);
          code = TStatusCode.SRM_FAILURE;
        }
        else {
          code = status.getStatus().getStatusCode();
        }
       }
       else {
          TReturnStatus returnStatus = new TReturnStatus(); 
          code = util.mapReturnStatusValueBackToCode(rCode);
          returnStatus.setStatusCode(code);
          if(status != null) { 
            status.setStatus(returnStatus);
          }
       }
      }
      util.printMessage("\nFileStatus code=" + code,logger,pIntf);
      if(status != null && status.getStatus() != null) {
        util.printMessage("\nExplanation=" + 
			status.getStatus().getExplanation(), logger,pIntf);
      }
       //added file_in_cache for cern v2 server
      if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
         (code != TStatusCode.SRM_REQUEST_QUEUED)) {
       if(code == TStatusCode.SRM_FILE_PINNED || 
         code == TStatusCode.SRM_DONE || 
         code == TStatusCode.SRM_SUCCESS || 
		 code == TStatusCode.SRM_FILE_IN_CACHE) {
           try {
             util.webScript(publishUrlStr,
	          displayType,"BringOnlineStatusMethod",code.toString(), null,
	  	      expBuffer.toString(), timeStamp, fullTimeStamp, startTimeStamp, 
			  operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfBringOnlineRequest",
			  "Ok",dailyTest,numSites,localPublish,bs,bsa);
               
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.isOperOk(true);
             }
           }catch(Exception e){e.printStackTrace();}
        if(status != null) {
           //turl = status.getTransferURL().toString();
           size = ""+status.getFileSize();
           TReturnStatus rs1 = new TReturnStatus();
           rs1.setStatusCode(code);
           status.setStatus(rs1);
           TBringOnlineRequestFileStatus[] statuses = new TBringOnlineRequestFileStatus[1];
           statuses[0] = status;
           ArrayOfTBringOnlineRequestFileStatus arrayofstatus =
            new ArrayOfTBringOnlineRequestFileStatus();
           arrayofstatus.setStatusArray(statuses);
           response.setArrayOfFileStatuses(arrayofstatus);

           reqOk = true;
         }
         statusArray.remove(key);
       }
       else {
         util.webScript(publishUrlStr,
	          displayType,"BringOnlineStatusMethod",code.toString(), null,
	  	      expBuffer.toString(), timeStamp, fullTimeStamp, startTimeStamp, 
			  operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfBringOnlineRequest",
			  "Failed",dailyTest,numSites,localPublish,bs,bsa);

              if(code == TStatusCode.SRM_NOT_SUPPORTED) {
                 bringOnlineSuccess.setExplanation("N.S.");
              }
              else {
                 bringOnlineSuccess.setExplanation(expBuffer.toString());
              } 
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.isOperOk(false);
               if(code == TStatusCode.SRM_NOT_SUPPORTED) {
                 gOk.setExplanation("N.S.");
               }
               else {
                 gOk.setExplanation(expBuffer.toString());
               } 
             }
        if(code == null) {
          code = util.mapReturnStatusValueBackToCode(rCode);
        } 
        TReturnStatus rs = new TReturnStatus();
        rs.setStatusCode(code);
        if(status != null && status.getStatus() != null &&
           status.getStatus().getExplanation() != null) {
            rs.setExplanation(status.getStatus().getExplanation());
            expBuffer.append(status.getStatus().getExplanation());
        }
        else {
          if(code != null) {
            rs.setExplanation(code.getValue().toString());
            //expBuffer.append(code.getValue().toString());
            //expMsg = "SRM returned File status code " + code.toString();
          }
          else {  
            expMsg = "SRM returned File status code null";
            rs.setExplanation("");
          }
        }
        response.setReturnStatus(rs);
        if(status != null) {
          status.setStatus(rs);
          TBringOnlineRequestFileStatus[] statuses = new TBringOnlineRequestFileStatus[1];
          statuses[0] = status;
          ArrayOfTBringOnlineRequestFileStatus arrayofstatus =
            new ArrayOfTBringOnlineRequestFileStatus();
          arrayofstatus.setStatusArray(statuses);
          response.setArrayOfFileStatuses(arrayofstatus);
        }
        statusArray.remove(key);
       }
    }
   }
  }catch(java.rmi.RemoteException re) {
     expMsg = re.getMessage();
  }

  util.printMessage("",logger,pIntf);
  //util.printMessageHL2("EndTime="+new Date()+"\n", logger,pIntf);

  
  if(currTestCondition == 4 && keyURL != null) {
     util.printMessage
	  ("\nWaiting here to do the advanced test for srmStatusOfBringOnlineRequest" +
        "\nfor condition wait for 120 seconds for the total request time ", 
			logger,pIntf); 
// AS 070809
// changed from 120000 to 30000
     Thread.sleep(statusWaitTime);
     checkBringOnlineStatusTest(srm,keyURL,response,3,sLock);
  }

  if(reqOk) {
    
    String displayMsg="Ok";
    try {
      rrs = new ReportRequestStatus();
      rrs.setLinkName("BringOnline_"+testCondition);
      rrs.setActualResult
			(response.getReturnStatus().getStatusCode().getValue());
     
      String exp = "";
      if(response.getReturnStatus().getExplanation() != null) {
         exp = response.getReturnStatus().getExplanation();
      }
      if(response.getReturnStatus().getStatusCode() == 
	       TStatusCode.SRM_SUCCESS || 
			   response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_DONE ||
			   response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_NOT_SUPPORTED) {
           if(response.getReturnStatus().getStatusCode() ==
				TStatusCode.SRM_NOT_SUPPORTED) {
              displayMsg = "N.S.";
           }
           rrs.setMatchingInfo("Yes");
      }
      else {
           displayMsg = "Failed";
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      }
      if(displayMsg.equals("Ok")) {
          bringOnlineSuccess.isOperOk(true);
      }
      else {
          bringOnlineSuccess.isOperOk(false);
          System.out.println(">>>> DisplayStatusMsg="+displayMsg);
          bringOnlineSuccess.setExplanation(displayMsg);
      }
      util.webScript(publishUrlStr,
			displayType,"BringOnline",
	        response.getReturnStatus().getStatusCode().getValue(),rrs,
			exp, timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,
			noPublish, siteName,numOperations,typeString,"srmBringOnline-OverAll",
			displayMsg, dailyTest,numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }

     /*
     SRMGetTest getTest = new SRMGetTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath,
            credential, proxyFile, onlyGsiFTPGet,
            noPublish, detailed, false, defaultProxy,advisoryDelete, dcau,
            bufferSize, 
	        siteName,numOperations,"srmPrepareToGet",
		    startTimeStamp, 
		    dailyTest,numSites,
			retryTimeAllowed, true, localPublish,bs, bsa,logger,pIntf);
     getTest.doGetFile(srm,proxyString,uid, source, localTarget, remoteTarget,
            plainGet,sLock, spaceToken, testCondition, new Boolean(true),false);
     */

    if(currTestCondition == 5 && keyURL != null) {
        util.printMessage
	      ("\nWaiting here to do the advanced test for srmStatusOfBringOnlineRequest" +
           "\nfor condition wait for 120 seconds for the pin life time ", logger,pIntf); 
// AS 070809
// changed from 120 to 300 sec
        Thread.sleep(statusWaitTime);
        checkBringOnlineStatusTest(srm,keyURL,response,4,sLock);
    }

    StringBuffer explanationBuffer = new StringBuffer();
    StringBuffer statusBuffer = new StringBuffer();

    try {
       /*
       if(advisoryDelete) {
           if(!detailed) {
             String statusMsg = doReleaseFile(srm, false,source,
		  	    response.getRequestToken(),statusBuffer,explanationBuffer,false,0); 
             try {
               rrs = util.createReportRequestStatus (remoteTarget,"","");
               rrs.setMatchingInfo(statusMsg);
               rrs.setActualResult(statusBuffer.toString());
               rrs.setRequestId(response.getRequestToken());
               rrs.setLinkName("srmreleasebasic");

               util.webScript(publishUrlStr,
	             displayType,"SendingBRINGONLINERelease","ReleaseFile", rrs,
		         "", timeStamp, fullTimeStamp, startTimeStamp, 
		         operatorTimeStamp,servicePath,noPublish,
                 siteName,numOperations,typeString,
				 "srmBringOnline","Sending",dailyTest,numSites,localPublish,bs,bsa);
               util.webScript(publishUrlStr,displayType,"BringOnlineReleaseFile",
			    statusMsg,
                rrs, explanationBuffer.toString(), 
			    timeStamp,fullTimeStamp, startTimeStamp, 
				operatorTimeStamp,servicePath,noPublish,
                siteName,numOperations,typeString,"srmRelease",
			    "Sending",dailyTest,numSites,localPublish,bs,bsa);
             }catch(Exception e)  {e.printStackTrace();}
           }
           else {
             if(currTestCondition == 1) {
               String statusMsg = doReleaseFile(srm, false,source,
		  	      response.getRequestToken(),
				  statusBuffer,explanationBuffer,true,3); 
               rrs = util.createReportRequestStatus (remoteTarget,"","");
               rrs.setMatchingInfo(statusMsg);
               rrs.setActualResult(statusBuffer.toString());
               rrs.setRequestId(response.getRequestToken());
               rrs.setLinkName("srmreleasedoremote_"+currTestCondition);
               try {
                 util.webScript(publishUrlStr,
	              displayType,"SendingBRINGONLINERelease",
					"ReleaseFile with doRemote(true)", rrs,
		            "", timeStamp, fullTimeStamp, startTimeStamp, 
					operatorTimeStamp,servicePath,noPublish,
                    siteName,numOperations,typeString,"srmReleaseFiles","Sending",
					dailyTest,numSites,localPublish,bs,bsa);
                 util.webScript(publishUrlStr,displayType,
				   "BringOnlineReleaseFile",statusMsg,
                    rrs, explanationBuffer.toString(), 
			        timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
				    servicePath,noPublish,
                    siteName,numOperations,typeString,"srmReleaseFiles","Sending",
					dailyTest,numSites,localPublish,bs,bsa);
               }catch(Exception e)  {e.printStackTrace();}
             }
             else {
                String statusMsg = doReleaseFile(srm, false,source,
		  	      response.getRequestToken(),statusBuffer,explanationBuffer,true,0); 
                rrs = util.createReportRequestStatus (remoteTarget,"","");
                rrs.setMatchingInfo(statusMsg);
                rrs.setActualResult(statusBuffer.toString());
                rrs.setRequestId(response.getRequestToken());
                rrs.setLinkName("srmreleasebasic_"+currTestCondition);
                try {
                 util.webScript(publishUrlStr,
	               displayType,"SendingBRINGONLINERelease","ReleaseFile", rrs,
		           "", timeStamp, fullTimeStamp, startTimeStamp, 
				   operatorTimeStamp,servicePath, noPublish,
                   siteName,numOperations,typeString,"srmReleaseFiles","Sending",
				   dailyTest,numSites,localPublish,bs,bsa);
                 util.webScript(publishUrlStr,displayType,"BringOnlineReleaseFile",
				  statusMsg, rrs, explanationBuffer.toString(), 
			      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
				  servicePath,noPublish, siteName,numOperations,typeString,
				  "srmReleaseFiles","Sending",dailyTest,numSites,localPublish,bs,bsa);
                }catch(Exception e)  {e.printStackTrace();}

                if(currTestCondition == 0) {
                  explanationBuffer = new StringBuffer();
                  statusBuffer = new StringBuffer();

                  statusMsg = doReleaseFile(srm, false,source,
		  	        response.getRequestToken(),statusBuffer,explanationBuffer,true,1); 
                  rrs = util.createReportRequestStatus (remoteTarget,"","");
                  rrs.setMatchingInfo(statusMsg);
                  rrs.setActualResult(statusBuffer.toString());
                  rrs.setRequestId(response.getRequestToken());
                  rrs.setLinkName
					("srmreleasealreadyreleasedsurl_"+currTestCondition);
                  try {
                   util.webScript(publishUrlStr,
	                displayType,"SendingBRINGONLINERelease",
					    "ReleaseFile already released SURL", 
						rrs, "", timeStamp, fullTimeStamp, startTimeStamp, 
					    operatorTimeStamp,servicePath,noPublish,
                        siteName,numOperations,typeString,"srmReleaseFiles","Sending",
				        dailyTest,numSites,localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,
					    "BringOnlineReleaseFile",statusMsg,
                        rrs, explanationBuffer.toString(), 
			            timeStamp,fullTimeStamp, startTimeStamp, 
						operatorTimeStamp,servicePath,noPublish,
                        siteName,numOperations,typeString,"srmReleaseFiles",
						"Sending",dailyTest,numSites,localPublish,bs,bsa);
                  }catch(Exception e)  {e.printStackTrace();}

                  explanationBuffer = new StringBuffer();
                  statusBuffer = new StringBuffer();
                  statusMsg = doReleaseFile(srm, false,source,
		  	        response.getRequestToken(),statusBuffer,explanationBuffer,true,2); 
                  rrs = util.createReportRequestStatus (remoteTarget+"_bad_surl","","");
                  rrs.setMatchingInfo(statusMsg);
                  rrs.setActualResult(statusBuffer.toString());
                  rrs.setRequestId(response.getRequestToken());
                  rrs.setLinkName("srmreleasebadsurl_"+currTestCondition);
                  try {
                   util.webScript(publishUrlStr,
	                displayType,"SendingBRINGONLINERelease","ReleaseFile bad SURL", 
						rrs, "", timeStamp, fullTimeStamp, startTimeStamp, 
				        operatorTimeStamp,servicePath,noPublish,
                        siteName,numOperations,typeString,"srmReleaseFiles","Sending",
						dailyTest,numSites,localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,"BringOnlineReleaseFile",
				     statusMsg,
                     rrs, explanationBuffer.toString(), 
			         timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
				     servicePath,noPublish, siteName,numOperations,typeString,
					 "srmReleaseFiles","Sending",dailyTest,
					 numSites,localPublish,bs,bsa);
                  }catch(Exception e)  {e.printStackTrace();}
                }
            }
           }
       }
       */

       bringOnlineSuccess.isOperOk(true);
       try {
          util.webScript(publishUrlStr,
			displayType,"RequestSummary","SUCCESS",
			convertBringOnline(response,"",turl,""), "", 
			timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			servicePath,noPublish, siteName,numOperations,typeString,
			"srmBringOnline-OverAll","Ok",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e)  {e.printStackTrace();}
          doReleaseSpace(srm,spaceToken);
          if(sLock != null) {
             sLock.setIncrementCount();
          }
    }catch(Exception ge) {
          util.printMessage
	       (" +++++ Releasefile failed " + 
		      explanationBuffer.toString() + " " + ge.getMessage(), logger,pIntf);
          try {
            util.webScript(publishUrlStr,
              displayType,"ReleaseFile","FAILED",null,
              explanationBuffer.toString() + ge.getMessage(),
              timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString,"srmRelease","Failed",
			  dailyTest,numSites,localPublish,bs,bsa);
               bringOnlineSuccess.isOperOk(true);
            util.webScript(publishUrlStr,
		 	  displayType,"RequestSummary","SUCCESS",
			  convertBringOnline(response,"",turl,""), "", 
			  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString,"srmBringOnline-OverAll","Ok",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace();}
          doReleaseSpace(srm,spaceToken);
          if(sLock != null) {
           sLock.setIncrementCount();
          }
    }
  }
  else {
    util.printMessage
  	  ("BringOnline File request failed.\n",logger,pIntf);
    util.printMessage("##################\n", logger,pIntf);
    util.printMessage("SRM-TESTER: Testing " + testType + " failed ", logger,pIntf);
    util.printMessage("##################\n", logger,pIntf);

    try {
      String displayStatusMsg="Ok";
      rrs = new ReportRequestStatus();
      rrs.setLinkName("BringOnline_"+testCondition);
      rrs.setActualResult
		(response.getReturnStatus().getStatusCode().getValue());
      if(testCondition == 4) {
        if(response.getReturnStatus().getStatusCode() == 
	       TStatusCode.SRM_REQUEST_TIMED_OUT || 
			   response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_FAILURE) {
           rrs.setMatchingInfo("Yes");
           displayStatusMsg="Ok";
        }
        else {
           rrs.setMatchingInfo
		    ("No. expecting SRM_REQUEST_TIMED_OUT or SRM_FAILURE");
           displayStatusMsg="Failed";
        }
      }
      else {
        if(response.getReturnStatus().getStatusCode() == 
		   TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
          response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
          rrs.setMatchingInfo("Yes");
          if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           displayStatusMsg="N.S.";
          }
          else {
            displayStatusMsg="Ok";
          }
        }
        else {
          rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
          displayStatusMsg="Failed";
        }
      }
      String ee = getExplanationFromResponse(response);
      if(ee.equals("")) { 
        ee = expMsg+"." + expBuffer.toString();
      }
      else {
        ee = ee + ". " + expMsg + "." + expBuffer.toString();
      }
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.setExplanation(displayStatusMsg);
             }
      util.webScript(publishUrlStr,displayType,
		"BringOnline",response.getReturnStatus().getStatusCode().getValue(),
    	rrs, ee, timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,typeString,"srmBringOnline",
	    displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
      if(plainGet) {
        if(testerTimedOut) {
          util.webScript(publishUrlStr,displayType,
		   "BringOnline",response.getReturnStatus().getStatusCode().getValue(),
    	   rrs, ee, timeStamp,fullTimeStamp, startTimeStamp, 
		   operatorTimeStamp,servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfBringOnlineRequest",
	       displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.isOperOk(false);
               gOk.setExplanation("TimedOut");
             }
          util.webScript(publishUrlStr,displayType,
	  	  "RequestSummary","TimedOut",
		  convertBringOnline(response,"","",expBuffer.toString()),
		  expBuffer.toString(), timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, 
		  "srmBringOnline-OverAll","TimedOut",dailyTest,numSites,
		  localPublish,bs,bsa);
          bringOnlineSuccess.isOperOk(false);
          bringOnlineSuccess.setExplanation("TimedOut");
        }
        else {
          util.webScript(publishUrlStr,displayType,
	  	  "RequestSummary",
		  response.getReturnStatus().getStatusCode().toString(),
		  convertBringOnline(response,"","",expBuffer.toString()),
		  expBuffer.toString(), timeStamp,fullTimeStamp, 
		  startTimeStamp, operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, 
		  "srmBringOnline-OverAll",displayStatusMsg,dailyTest,numSites,
		  localPublish,bs,bsa);
          bringOnlineSuccess.isOperOk(false);
          if(bringOnlineSuccess.getExplanation().equals("")) {
            bringOnlineSuccess.setExplanation("Failed");
          }
        }
      }		 
      doReleaseSpace(srm,spaceToken);
      if(sLock != null) {
         sLock.setIncrementCount();
      }
    }catch(Exception  e) { e.printStackTrace(); }
  }
  return "";
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkBringOnlineStatusTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public void  checkBringOnlineStatusTest
    (ISRM srm, URI url, SrmBringOnlineResponse response, 
		int condition,SharedObjectLock sLock) 
      throws URI.MalformedURIException, java.rmi.RemoteException,Exception {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
   String token = response.getRequestToken();

   if(token == null)  {
     throw new java.rmi.RemoteException("server sent null request token.");
   }

   if(condition == 1) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling BringOnlineStatus for Bad requestToken " + 
			token+"_bad_token", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_INVALID_REQUEST", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token+"_bad_token");


     SrmStatusOfBringOnlineRequestRequest r = new SrmStatusOfBringOnlineRequestRequest();
     r.setRequestToken(token+"_bad_token");

     SrmStatusOfBringOnlineRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfBringOnlineRequest(r);
       timeOutCallBack.setObject(result); 
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
          throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfBringOnlineRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     util.printMessage("Explanation : "  + explanation,logger,pIntf);

     if(result.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_INVALID_REQUEST ||
        result.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_FAILURE) {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadtoken");
        
        try {
         util.webScript(publishUrlStr,
	       displayType,"SendingBRINGONLINESTATUS",
		   "BringOnline Status for Bad Token", rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
            siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	       displayType,"BringOnlineStatus","Yes", rrs,
	         explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
             siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			 "Ok",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
        rrs.setLinkName("statusbadtoken");
        try {
         util.webScript(publishUrlStr,
	       displayType,"SendingBRINGONLINESTATUS",
		   "BringOnline Status for Bad Token", rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
            siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
	       displayType,"BringOnlineStatus",
			  "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE", rrs,
			  explanation, 
		    timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
            siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
      rrs.setLinkName("statusbadtoken");
      try {
        util.webScript(publishUrlStr,
	       displayType,"SendingBRINGONLINESTATUS",
		   "BringOnline Status for Bad Token", rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
            siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	     displayType,"BringOnlineStatus","SRM returned null result", rrs,
	      "SRM returned null result", 
		   timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
           siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		   "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
   }
   else if(condition == 2) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling BringOnline Status for requestToken " + token, logger,pIntf);
     util.printMessage("\nBad SURL " + url+"_"+"bad_surl", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FAILURE", logger,pIntf);
     URI uuri = new URI(url.toString()+"_"+"bad_surl");

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);

     ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];
 
     ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
     rrfs.setSourceUrl(uuri.toString());
 
    reportFileStatus[0] = rrfs;
 
    rrs.setReportRequestFileStatus(reportFileStatus);


     SrmStatusOfBringOnlineRequestRequest r = new SrmStatusOfBringOnlineRequestRequest();
     r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(uuri));
     r.setRequestToken(token);

     SrmStatusOfBringOnlineRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfBringOnlineRequest(r);
       timeOutCallBack.setObject(result); 
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
          throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfBringOnlineRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     util.printMessage("Explanation=" + explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE 
			|| result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ) {

       TBringOnlineRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfBringOnlineRequest) " +
            code, logger,pIntf);
         explanation = fileStatus.getStatus().getExplanation();
         if(explanation == null) {
           explanation = "";
         }
         util.printMessage("FileStatus Explanation : " + explanation,logger,pIntf);
         if(code == TStatusCode.SRM_INVALID_PATH || 
		    code == TStatusCode.SRM_FAILURE) {
           rrs.setActualResult(code.getValue());
           rrs.setMatchingInfo("Yes");
           rrs.setLinkName("statusbadsurl");
           try {
            util.webScript(publishUrlStr,
	          displayType,"SendingBRINGONLINESTATUS",
	          "BringOnline Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"BringOnlineStatus","Yes", rrs,
				explanation, timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
           rrs.setActualResult(code.getValue());
           rrs.setMatchingInfo("No. expecting SRM_INVALID_PATH || SRM_FAILURE");
           rrs.setLinkName("statusbadsurl");
          try {
            util.webScript(publishUrlStr,
	          displayType,"SendingBRINGONLINESTATUS",
	          "BringOnline Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"BringOnlineStatus",
				"No. expecting SRM_INVALID_PATH || SRM_FAILURE", rrs, explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
             siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			 "Failed",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().getValue());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadsurl");
        try {
         util.webScript(publishUrlStr,
	          displayType,"SendingBRINGONLINESTATUS",
	          "BringOnline Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus","Yes", rrs,
				explanation, 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			  "Ok",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().getValue());
       rrs.setMatchingInfo("No. expecting SRM_FAILURE");
       rrs.setLinkName("statusbadsurl");
       try {
        util.webScript(publishUrlStr,
	          displayType,"SendingBRINGONLINESTATUS",
	          "BringOnline Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus",
			  result.getReturnStatus().getStatusCode().getValue(), rrs,
			  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
		      operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			 "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    } 
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FAILURE");
      rrs.setLinkName("statusbadsurl");
      try {
       util.webScript(publishUrlStr,
	          displayType,"SendingBRINGONLINESTATUS",
	          "BringOnline Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
	          displayType,"BringOnlineStatus","SRM returned null result", null,
	          "SRM returned null result ", 
		      timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			  "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
   }
   else if(condition == 3) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling BringOnlineStatus for expired requestToken " + 
			token, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_REQUEST_TIMED_OUT", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);


     SrmStatusOfBringOnlineRequestRequest r = new SrmStatusOfBringOnlineRequestRequest();
     r.setRequestToken(token);

     SrmStatusOfBringOnlineRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfBringOnlineRequest(r);
       timeOutCallBack.setObject(result); 
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
          throw e;
     }

     if(result != null && result.getReturnStatus() != null) {

     util.printMessage("\nStatus from SRM (srmStatusOfBringOnlineRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
        explanation = "";
     }
     util.printMessage("Explanation=" + explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_REQUEST_TIMED_OUT) {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("Yes");
       rrs.setLinkName("statusexpiredtoken");
       try {
        util.webScript(publishUrlStr,
	     displayType,"SendingBRINGONLINESTATUS",
	     "BringOnline Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus","Yes", rrs,
		  explanation, 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Ok",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken");
       try {
        util.webScript(publishUrlStr,
	     displayType,"SendingBRINGONLINESTATUS",
	     "BringOnline Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken");
       try {
        util.webScript(publishUrlStr,
	     displayType,"SendingBRINGONLINESTATUS",
	     "BringOnline Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus","SRM returned null status", rrs,
		  "SRM returned null status ", 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
    }
   }
   else if(condition == 4) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling BringOnline Status for requestToken=" + token, 
			logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", 
			logger,pIntf);
     URI uuri = new URI(url.toString());

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);

     ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];
 
     ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
     rrfs.setSourceUrl(uuri.toString());
 
    reportFileStatus[0] = rrfs;
 
    rrs.setReportRequestFileStatus(reportFileStatus);


     SrmStatusOfBringOnlineRequestRequest r = 
		new SrmStatusOfBringOnlineRequestRequest();
     r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(uuri));
     r.setRequestToken(token);

     SrmStatusOfBringOnlineRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfBringOnlineRequest(r);
       timeOutCallBack.setObject(result); 
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
          throw e;
     }   

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfBringOnlineRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     util.printMessage("Explanation=" + explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE || 
		 result.getReturnStatus().getStatusCode() ==TStatusCode.SRM_SUCCESS) {

       TBringOnlineRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         explanation = fileStatus.getStatus().getExplanation();
         if(explanation == null) {
           explanation = "";
         }
         util.printMessage("\nFileStatus from SRM (srmStatusOfBringOnlineRequest) " +
            code, logger,pIntf);
         util.printMessage("Explanation=" + explanation,logger,pIntf);
         if(code == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
           rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
           rrs.setMatchingInfo("Yes");
           rrs.setLinkName("statuslifetimeexpired");  
           try {
            util.webScript(publishUrlStr,
	         displayType,"SendingBRINGONLINESTATUS",
	         "BringOnline Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"BringOnlineStatus","Yes", rrs,
			  explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			  "Ok",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          try {
            rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
            rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
            rrs.setLinkName("statuslifetimeexpired");  
            util.webScript(publishUrlStr,
	         displayType,"SendingBRINGONLINESTATUS",
	         "BringOnline Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"BringOnlineStatus",
			  "No. expecting SRM_FILE_LILETIME_EXPIRED", rrs,
			  explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
			  "Failed",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        try {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired");  
         util.webScript(publishUrlStr,
	         displayType,"SendingBRINGONLINESTATUS",
	         "BringOnline Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       try {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
        rrs.setLinkName("statuslifetimeexpired");  
        util.webScript(publishUrlStr,
	         displayType,"SendingBRINGONLINESTATUS",
	         "BringOnline Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus",
			 result.getReturnStatus().getStatusCode().toString(), rrs,
		     explanation, timeStamp, fullTimeStamp, startTimeStamp, 
		     operatorTimeStamp,servicePath,noPublish,
             siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		     "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
      rrs.setLinkName("statuslifetimeexpired");  
      try {
        util.webScript(publishUrlStr,
	         displayType,"SendingBRINGONLINESTATUS",
	         "BringOnline Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatus","SRM returned null status", rrs,
	      "SRM returned null status", 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkBringOnlineStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TBringOnlineRequestFileStatus checkBringOnlineStatus
    (ISRM srm, URI url, SrmBringOnlineResponse response, 
     StringBuffer rCode, boolean statusTest, 
	 StringBuffer expBuffer, IsWholeRequestFailed wholeRequestFailed,
	  SharedObjectLock sLock) 
      throws URI.MalformedURIException, java.rmi.RemoteException, Exception {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
    String token = response.getRequestToken();
    if(token == null)  {
      throw new java.rmi.RemoteException("server sent null request token.");
    }

    if(statusTest) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling BringOnlineStatus for requestToken=" + token, 
			logger,pIntf);
      util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);

      ReportRequestStatus rrs = new ReportRequestStatus();
      rrs.setRequestId(token);

      ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];
 
      ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
      rrfs.setSourceUrl(url.toString());

      reportFileStatus[0] = rrfs;
 
     rrs.setReportRequestFileStatus(reportFileStatus);

     /*
     try {
       util.webScript(publishUrlStr,
	     displayType,"Sending","BringOnline Status Request now ...", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString, "Sending",dailyTest,numSites);
     }catch(Exception e) { e.printStackTrace(); }
     */
    }

    SrmStatusOfBringOnlineRequestRequest r = 
			new SrmStatusOfBringOnlineRequestRequest();
   
    r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(url));
    r.setRequestToken(token);

    SrmStatusOfBringOnlineRequestResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setRequestToken(token,srm);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmStatusOfBringOnlineRequest(r);
      timeOutCallBack.setObject(result); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }

    if(result != null) {
    util.printMessage("\nSRM-TESTER: Status from SRM (srmStatusOfBringOnlineRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
    response.setReturnStatus(result.getReturnStatus());
    String explanation = result.getReturnStatus().getExplanation();
    if(explanation != null) {
       util.printMessage("Explanation=" + explanation, logger,pIntf);
       expBuffer.append(explanation);
    }

    if(result.getReturnStatus().getStatusCode() == 
         TStatusCode.SRM_SUCCESS ||
       result.getReturnStatus().getStatusCode() == 
         TStatusCode.SRM_DONE ||
       result.getReturnStatus().getStatusCode() == 
         TStatusCode.SRM_REQUEST_INPROGRESS ||
       result.getReturnStatus().getStatusCode() == 
         TStatusCode.SRM_REQUEST_QUEUED ||
       result.getReturnStatus().getStatusCode() == 
         TStatusCode.SRM_NOT_SUPPORTED) {; }
    else {
        try {
           util.webScript(publishUrlStr,
	        displayType,"BringOnlineStatusMethod","Failed", null,
	        expBuffer.toString(), 
		    timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
            siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
	        "Failed",dailyTest,numSites,localPublish,bs,bsa);
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.isOperOk(false);
               gOk.setExplanation(expBuffer.toString());
             }
        }catch(Exception e) {e.printStackTrace();}
    }

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
            TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) {
       rCode.append("SRM_SPACE_LIFETIME_EXPIRED");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_EXCEED_ALLOCATION) {
       rCode.append("SRM_EXCEED_ALLOCATION");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NO_USER_SPACE) {
       rCode.append("SRM_NO_USER_SPACE");
    }
    else if (result.getReturnStatus().getStatusCode() ==
           TStatusCode.SRM_NO_FREE_SPACE) {
       rCode.append("SRM_NO_FREE_SPACE");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NOT_SUPPORTED) {
       rCode.append("SRM_NOT_SUPPORTED");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_ABORTED) {
       rCode.append("SRM_ABORTED");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_REQUEST_TIMED_OUT) {
       rCode.append("SRM_REQUEST_TIMED_OUT");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_INTERNAL_ERROR) {
       rCode.append("SRM_INTERNAL_ERROR");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE) {
       rCode.append("SRM_FAILURE");
       wholeRequestFailed.setWholeRequestFailed(true);
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS) {
       rCode.append("SRM_SUCCESS");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_DONE) {
       rCode.append("SRM_DONE");
    }

    TBringOnlineRequestFileStatus fileStatus = null;

    if(result.getArrayOfFileStatuses() != null) {
        fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
    }

    util.printMessageNL(".",logger,pIntf);

    return fileStatus;
   }
   else {
     util.printMessage("SRM returned null status for srmStatusOfBringOnlineRequest", 
		logger,pIntf);
     try {
        util.webScript(publishUrlStr,
	      displayType,"BringOnlineStatusMethod","SRM returned null status", 
		  null, "SRM returned null status", 
		  timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString, "srmStatusOfBringOnlineRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.isOperOk(false);
               gOk.setExplanation("SRM returned null status");
             }
     }catch(Exception e) {e.printStackTrace();}
     return null;
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printBringOnlineResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private HashMap printBringOnlineResult (SrmBringOnlineResponse response, 
	StringBuffer responseStatus) {

   HashMap result = new HashMap ();

   if(response == null) {
     util.printMessage("SRM-TESTER:  ...Output from SRM..."+ new Date(),logger,pIntf);
     util.printMessage("\tBringOnline Response is null ",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

   util.printMessage("SRM-TESTER:  ...Output from SRM..."+ new Date(),logger,pIntf);
   if(response.getRequestToken() != null) { 
     util.printMessage("request.token    : \t" + 
		response.getRequestToken(),logger,pIntf);
   }
   else {
     util.printMessage("request.token    : \t" + null,logger,pIntf);
   }
   if(response.getReturnStatus() == null) {
     try {
      util.webScript(publishUrlStr,
			displayType,"BringOnline",
	        "Failed",null,
			"SRM returned null return status", 
			timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,
			noPublish,
            siteName,numOperations,typeString,"srmBringOnline",
			"Failed", dailyTest,numSites,localPublish,bs,bsa);
       util.printMessage("request.state : \t" + null,logger,pIntf);
       responseStatus.append("Null request status");
     }catch(Exception e) {}
       return result;
   }
   else {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
     String displayStatusMsg="Ok";  
     if(response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS ||
	    response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_DONE ||
	    response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_NOT_SUPPORTED ||
	    response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_REQUEST_QUEUED ||
	    response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_REQUEST_INPROGRESS) {
        if(response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NOT_SUPPORTED) {
             displayStatusMsg="N.S.";
        }
      }
      else {
          displayStatusMsg="Failed";
      }

      bringOnlineSuccess.setExplanation(displayStatusMsg);
              if(bringOnlineSuccess.getSubOperations().size() > 0 ) {
               HashMap map = bringOnlineSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("bringonlinestatus");
               gOk.setTried(true);
               gOk.setExplanation(displayStatusMsg);
             }
      String exp = "";
      if(response.getReturnStatus().getExplanation () != null) {
        exp = response.getReturnStatus().getExplanation();
        util.printMessage("\nExplanation="+exp,logger,pIntf);
      }
      try {
      util.webScript(publishUrlStr,
			displayType,"BringOnline",
	        response.getReturnStatus().getStatusCode().toString(),null,
			exp, timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,
			noPublish, siteName,numOperations,typeString,"srmBringOnline",
			displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) {}
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
            TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) {
       responseStatus.append("SRM_SPACE_LIFETIME_EXPIRED");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_EXCEED_ALLOCATION) {
       responseStatus.append("SRM_EXCEED_ALLOCATION");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NO_USER_SPACE) {
       responseStatus.append("SRM_NO_USER_SPACE");
       return result;
     }
     else if (response.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_NO_FREE_SPACE) {
       responseStatus.append("SRM_NO_FREE_SPACE");
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
         
   }

   if(response.getRemainingTotalRequestTime() != null) {
     util.printMessage("request.remainingTotalRequestTime : \t" + 
		response.getRemainingTotalRequestTime(),logger,pIntf);
   }
   else {
     util.printMessage("request.remainingTotalRequestTime : \t" + 
		null,logger,pIntf);
   }

   if(response.getArrayOfFileStatuses() != null) { 
     int size = response.getArrayOfFileStatuses().getStatusArray().length;
     for (int i = 0 ; i < size; i++) {
      TBringOnlineRequestFileStatus fileStatus =
        response.getArrayOfFileStatuses().getStatusArray(i);
      if(fileStatus.getSourceSURL() != null) { 
        util.printMessage("\n\tSRM-TESTER: SURL="+
             fileStatus.getSourceSURL().toString(),logger,pIntf);
      }
      if(fileStatus.getFileSize() != null) { 
        util.printMessage("\n\tSize="+
             fileStatus.getFileSize(),logger,pIntf);
      }
      if(fileStatus.getStatus() != null) {
        util.printMessage("\tStatus="+
             fileStatus.getStatus().getStatusCode(),logger,pIntf);
        util.printMessage("\tExplanation="+
             fileStatus.getStatus().getExplanation(),logger,pIntf);
      }
      result.put(fileStatus.getSourceSURL(), fileStatus);
      util.printMessage("............",logger,pIntf);
    }
  }


  util.printMessage("===========================================",logger,pIntf);
  return result;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getExplanationFromResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String getExplanationFromResponse (SrmBringOnlineResponse response)
{
   String exp="";
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..."+ new Date(),logger,pIntf);
     util.printMessage("\tResponse is null ",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   if(response.getReturnStatus() != null) {
     if(response.getReturnStatus().getStatusCode() != null) { 
      String explanation = response.getReturnStatus().getExplanation();
      if(explanation != null) 
        exp = explanation;
     }
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TBringOnlineRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
        int size = fileStatuses.length;

        for (int i = 0 ; i < size; i++) {
          TBringOnlineRequestFileStatus fileStatus =
            response.getArrayOfFileStatuses().getStatusArray(i);
          String explanation = fileStatus.getStatus().getExplanation();
          if(explanation != null) {
             exp = exp + " " + explanation;
          }
        }
    }
  }
  return exp;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// convertBringOnline
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ReportRequestStatus convertBringOnline ( SrmBringOnlineResponse response, 
		String surl, String tturl, String exp)
{
   if(response == null) {
     util.printMessage("SRM-TESTER: ...Output from SRM..."+ new Date(),logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   ReportRequestStatus rrs = new ReportRequestStatus();

   if(response.getRequestToken() != null) {
     rrs.setRequestId(""+response.getRequestToken());
   }
   if(response.getReturnStatus() != null) {
     if(response.getReturnStatus().getStatusCode() != null) { 
       rrs.setRequestState(response.getReturnStatus().getStatusCode().getValue());
     }
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) 
       rrs.setExplanation(explanation);
   }
   if(response.getRemainingTotalRequestTime() != null) {
     rrs.setRemainingTotalRequestTime(""+ response.getRemainingTotalRequestTime());
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TBringOnlineRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
        int size = fileStatuses.length;
        ReportRequestFileStatus [] reportFileStatus =
            new ReportRequestFileStatus[size];

        for (int i = 0 ; i < size; i++) {
          TBringOnlineRequestFileStatus fileStatus =
            response.getArrayOfFileStatuses().getStatusArray(i);
          ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
          rrfs.setSourceUrl(fileStatus.getSourceSURL().toString());
          //rrfs.setTargetUrl(turl);
          rrfs.setState(fileStatus.getStatus().getStatusCode().getValue());
          String explanation = fileStatus.getStatus().getExplanation();
          if(explanation != null && !explanation.equals("")) {
           rrfs.setExplanation(explanation);
           util.printMessage("Notes from server" + 
				explanation, logger,pIntf);
          }
          else {
           rrfs.setExplanation(exp);
           if(!exp.equals("")) {
             util.printMessage("Notes from tester" + exp, logger,null);
           }
          }
          //rrfs.setSize(fSize);
          reportFileStatus[i] = rrfs;
        }
        rrs.setReportRequestFileStatus(reportFileStatus);
    }
  }
 
  return rrs;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReleaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String doReleaseFile (ISRM srm, boolean keepSpace, String siteUrl,
    String requestToken, StringBuffer statusBuffer, 
    StringBuffer explanationBuffer, 
	boolean detailedTest, int releaseTestCondition) throws Exception {

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);

  if(requestToken != null) {
    SrmReleaseFilesRequest req = new SrmReleaseFilesRequest();
    //req.setKeepSpace(new Boolean(keepSpace));
    //req.setKeepSpace(new Boolean(true));
    req.setRequestToken(requestToken);
    req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));

    if(!detailedTest) {
      util.printMessageHL("\nSRM-TESTER: " +
		new Date() + " Calling ReleaseFile(BringOnline) ...\n", 
			logger,pIntf);
      //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);
      util.printMessage("============================",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("============================\n",logger,pIntf);
    }
    else {
      if(currTestCondition == 0 && releaseTestCondition == 2) {
        uri = new org.apache.axis.types.URI (siteUrl+"_bad_surl");
        req = new SrmReleaseFilesRequest();
        //req.setKeepSpace(new Boolean(keepSpace));
        //req.setKeepSpace(new Boolean(true));
        req.setRequestToken(requestToken);
        req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));

        util.printMessage("\nSRM-TESTER: " + new Date() +
			" Calling ReleaseFile Test for Bad SURL", logger,pIntf);
        util.printMessage("RequestToken="+requestToken,logger,pIntf);
        util.printMessage("Bad SURL="+siteUrl+"_bad_surl",logger,pIntf);
        util.printMessage("ExpectedResult=SRM_INVALID_PATH || SRM_FAILURE",logger,pIntf);
      }
      else if(currTestCondition == 0 && releaseTestCondition == 1) {

        util.printMessage("\nSRM-TESTER: " + new Date() +
			" Calling ReleaseFile Test for already released SURL", 
				logger,pIntf);
        util.printMessage("RequestToken="+requestToken,logger,pIntf);
        util.printMessage("SURL="+siteUrl,logger,pIntf);
        util.printMessage("ExpectedResult=SRM_FAILURE",logger,pIntf);
      }
      else if(currTestCondition == 1 && releaseTestCondition == 3) {
        uri = new org.apache.axis.types.URI (siteUrl);
        req = new SrmReleaseFilesRequest();
        //req.setKeepSpace(new Boolean(keepSpace));
        //req.setKeepSpace(new Boolean(true));
        req.setRequestToken(requestToken);
        req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
        req.setDoRemove(new Boolean(true));

        util.printMessage("\nSRM-TESTER: " + new Date() +
			" Calling ReleaseFile Test with doRemote(true)", logger,pIntf);
        util.printMessage("RequestToken="+requestToken,logger,pIntf);
        util.printMessage("SURL="+siteUrl,logger,pIntf);
        util.printMessage("ExpectedResult=SRM_RELEASED",logger,pIntf);
      }
    }

    if(srm == null) {
      return "POSSIBLY_SRM_DOWN";
    }

    SrmReleaseFilesResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmReleaseFiles(req);
      timeOutCallBack.setObject(result); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
          throw e;
    }

    util.printMessage("SRM-TESTER: ...Output from SRM..."+ new Date(),logger,pIntf);
    if(result != null) {
      util.printMessage("\tstatus="+
            result.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
      util.printMessage("\texplanation="+
            result.getReturnStatus().getExplanation(),logger,pIntf);
      if(result.getArrayOfFileStatuses() != null) {
        TStatusCode temp = result.getArrayOfFileStatuses().getStatusArray(0).
			              getStatus().getStatusCode();
        statusBuffer.append(temp.getValue());
        util.printMessage("\tdetails="+ temp,logger,pIntf);
        String ttemp =
          result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getExplanation();
        util.printMessage("\tdetails explanation="+ ttemp, logger,pIntf);
        util.printMessage("================================\n",
			logger,pIntf);
        //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);
        if(ttemp != null) {
           explanationBuffer.append(ttemp);
        }
        //basic case 
        if(releaseTestCondition == 0) {
          if(temp == TStatusCode.SRM_RELEASED || 
             temp == TStatusCode.SRM_SUCCESS ||
             temp == TStatusCode.SRM_NOT_SUPPORTED ||
             temp == TStatusCode.SRM_DONE) {
            return "Yes";
          }
          return "No. expecting SRM_RELEASED or SRM_SUCCESS or SRM_NOT_SUPPORTED";
        }
        if(currTestCondition == 0) {
           if(releaseTestCondition == 2) {
              if(temp == TStatusCode.SRM_INVALID_PATH || 
                 temp == TStatusCode.SRM_FAILURE) { 
                return "Yes";
              }
              else {
                return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
              }
           }
           else if(releaseTestCondition == 1) {
              if(temp == TStatusCode.SRM_FAILURE) { 
                return "Yes";
              }
              else {
                return "No. expecting SRM_FAILURE";
              }
           }
        }
        else if(currTestCondition == 1) {
           if(releaseTestCondition == 3) {
              if(temp == TStatusCode.SRM_RELEASED || 
			     temp == TStatusCode.SRM_NOT_SUPPORTED) { 
                return "Yes";
              }
              else {
                return "No. expecting SRM_RELEASED or SRM_SUCCESS or SRM_NOT_SUPPORTED";
              }
           }
        }
        if(temp == TStatusCode.SRM_SUCCESS ||  temp == TStatusCode.SRM_DONE ||
                   temp == TStatusCode.SRM_NOT_SUPPORTED) {
           return "Yes";
        }
        return temp.getValue();
      }
      else  {
        statusBuffer.append(result.getReturnStatus().getStatusCode().getValue()); 
        if(result.getReturnStatus().getExplanation() != null) {
          explanationBuffer.append(result.getReturnStatus().getExplanation());
        }
        return result.getReturnStatus().getStatusCode().getValue();
      } 
    }
    else {
      statusBuffer.append("Null result from SRM"); 
      explanationBuffer.append("Please contact SRM admin.");
      return "Null result from SRM";
    }
  }
  else {
     explanationBuffer.append("Cannot do release file for null request token");
     return "Cannot do release file for null request token";
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doReleaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doReleaseSpace(ISRM srm, String sToken) throws Exception {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();

  if(!alreadyReleasedSpace && reserveSpace && !sToken.equals("")) {

   alreadyReleasedSpace = true;
   if(sToken == null || sToken.equals("")) {
     util.printMessage("SRM-TESTER: Cannot release space for null token", logger,pIntf);
     return;
   }

   try {
    util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling ReleaseSpace now ...", logger,pIntf);
    SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
    if(!uid.equals("")) {
      req.setAuthorizationID(uid);
    }
    req.setSpaceToken(sToken);

    SrmReleaseSpaceResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmReleaseSpace(req);
      timeOutCallBack.setObject(response); 
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
          throw e;
    }

    util.printMessage("SRM-TESTER: ...Output from SRM..."+new Date(), logger,pIntf);
    ReportRequestStatus rrs = util.createReportRequestStatus("","","");
    rrs.setSpaceToken(sToken);
    rrs.setLinkName("releasespace_"+currTestCondition);

    if(response != null) {
     if(response.getReturnStatus() != null) {
       util.printMessage("StatusCode " + response.getReturnStatus().getStatusCode(),logger,pIntf);
       rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo
				("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
        }
      }
     else {
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      }

     util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
	   servicePath,noPublish, siteName,numOperations,typeString, 
       "srmReleaseSpace","Sending",dailyTest,numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Sending", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish, siteName,numOperations,typeString, 
	  "srmReleaseSpace","Sending",dailyTest,numSites,localPublish,bs,bsa);
     }
     else {
       rrs.setActualResult("Null response from server");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "Null response from the server ", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		servicePath,noPublish, siteName,numOperations,typeString, "srmReleaseSpace",
		"Sending",dailyTest,numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Failed", rrs,
      "Null response from the server", timeStamp, fullTimeStamp, 
	  startTimeStamp, operatorTimeStamp,servicePath,noPublish, siteName,numOperations,typeString, 
	  "srmReleaseSpace","Sending",dailyTest,numSites,localPublish,bs,bsa);
     }
   }catch(Exception e) {
      e.printStackTrace();
      throw e;
    }
  }
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

