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
// Class SRMCopyTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMCopyTest
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
   private boolean defaultProxy;
   private boolean advisoryDelete;
   private boolean dcau=true;
   private int bufferSize;
   private int parallelism;
   private String uid;
   private int currTestCondition;
   private String typeString="srmCopy";
   private String siteName; 
   private int numOperations;
   private boolean dailyTest;
   private String sTimeStamp;
   private int numSites;
   private int retryTimeAllowed;
   private int statusWaitTime=15; //default 15 seconds
   private boolean localPublish;
   private BufferedWriter bs;
   private BufferedWriter bsa;
   private Log logger;
   private PrintIntf pIntf;
   private String copyServicePath;
   private boolean changeToCopyServicePath;
   private OperOk copyOk;
   private TimeOutCallBack timeOutCallBack;
   private OperOk pingOverAllSuccess;
   private boolean useDriverOn;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMCopyTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMCopyTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp,
	   String servicePath, 
       GSSCredential credential, String proxyFile, 
	   boolean onlyGsiFTPGet, boolean noPublish, 
	   boolean detailed, boolean defaultProxy, 
	   boolean advisoryDelete, boolean dcau, int bufferSize,
       int parallelism, String siteName, int numOperations, String typeString, 
       String sTimeStamp, boolean dailyTest, 
	   int numSites, int retryTimeAllowed, int statusWaitTime,
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
   this.onlyGsiFTPGet = onlyGsiFTPGet;
   this.noPublish = noPublish;
   this.detailed = detailed;
   this.defaultProxy = defaultProxy;
   this.advisoryDelete = advisoryDelete;
   this.dcau = dcau;
   this.bufferSize = bufferSize;
   this.parallelism = parallelism;
   this.logger = logger;
   this.pIntf = pIntf;
   this.siteName = siteName;
   this.numOperations = numOperations;
   this.dailyTest = dailyTest;
   this.sTimeStamp = sTimeStamp;
   this.numSites = numSites;
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
// doCopyFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doCopyFile(ISRM srm, 
	StringBuffer proxyString, String uid, String source, String target, 
	String spaceToken, boolean browse, 
	boolean overwrite, SharedObjectLock sLock, 
    boolean pushMode, String copyServicePath,
    boolean changeToCopyServicePath, OperOk copyOk,
	int testCondition,TFileStorageType fileStorageType) throws Exception {

  File f = null;
  boolean isCopyOk = true; 
  boolean reqOk = false;
  StringBuffer notOkMsg = new StringBuffer();
  String path = "";
  this.uid = uid;
  long size = 0;
  this.copyServicePath = copyServicePath;
  this.changeToCopyServicePath = changeToCopyServicePath;
  this.copyOk = copyOk;
  HashMap map = copyOk.getSubOperations();
  OperOk subOk = (OperOk) map.get("srmCopy-"+typeString);
  if(subOk != null) {
    subOk.setTried(true);
  }

  this.currTestCondition = testCondition;


  Date d = new Date ();
  String operatorTimeStamp = d.toString();

  String tt = "";
  if(spaceToken != null) {
    tt = spaceToken;
  }


  util.printMessageHL("\nSRM-TESTER: " + new Date() +
		" Calling Copy request ...\n",logger,pIntf);
  if(pushMode) {
     util.printMessageHL("\tMode=Push",logger,pIntf);
  }
  if(changeToCopyServicePath) {
     if(source.startsWith("gsiftp")) {
       util.printMessageHL("\tSRM-TESTER: ServicePath=",logger,pIntf);
     }
     else {
       util.printMessageHL("\tSRM-TESTER: ServicePath="+copyServicePath,logger,pIntf);
     }
  }
  else {
     if(source.startsWith("gsiftp")) {
       util.printMessageHL("\tSRM-TESTER: ServicePath=",logger,pIntf);
       //Added by Viji
       servicePath=copyServicePath;
     }
     else {
       util.printMessageHL("\tSRM-TESTER: ServicePath="+servicePath,logger,pIntf);
     }
  }
  //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

  String[] surl = new String[1]; 
  String[] turl = new String[1];
  boolean[] overwritefiles = new boolean[1]; 

  if(source.equals("")) {
    notOkMsg.append("Please provide the -copysource <copysource>");
    util.printMessage("\n\n" + notOkMsg.toString() ,logger,pIntf);
    isCopyOk = false;
  }

  if(target.equals("")) {
    notOkMsg.append("Please provide the -copytarget <copytarget>");
    util.printMessage("\n\n" + notOkMsg.toString() ,logger,pIntf);
    isCopyOk = false;
  }

  surl[0] = source;
  turl[0] = target;
  overwritefiles[0] = overwrite;

  if(detailed) {
    turl[0]=target+"."+testCondition;
  }
  else {
    turl[0] = target;
  }

  if(isCopyOk) { 

    SrmCopyRequest request = new SrmCopyRequest();

    ReportRequestStatus rrs = 
		util.createReportRequestStatus(surl[0],turl[0],"");
    if(changeToCopyServicePath) {
      rrs.setSrmServicePath(copyServicePath); 
    }
    rrs.setOverwrite(""+overwritefiles[0]);


    if(!detailed) {
      SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
            bufferSize, proxyString,pIntf);
      pTest.doTestBasic(surl[0],turl[0],rrs,fileStorageType); 
    }
    else {
      util.printMessage("\n++++ Running Advanced Test ++++", logger,pIntf); 
      if(testCondition == 0) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize, proxyString,pIntf);
        pTest.doTestUserRequestDesc(surl[0],turl[0],rrs,"Test description"); 
      }
      /*
      else if(testCondition == 1) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize, proxyString,pIntf);
        pTest.doTestOverwrite(surl[0],turl[0],rrs,false); 
      }
      else if(testCondition == 2) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize, proxyString,pIntf);
        pTest.doTestOverwrite(surl[0],turl[0],rrs,true); 
      }
      */
      else if(testCondition == 1) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize, proxyString,pIntf);
        pTest.doTestFileStorageType
			(surl[0],turl[0],rrs,TFileStorageType.VOLATILE); 
      }
      else if(testCondition == 2) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        pTest.doTestFileStorageType
			(surl[0],turl[0],rrs,TFileStorageType.PERMANENT); 
      }
      else if(testCondition == 3) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        pTest.doTestFileStorageType
			(surl[0],turl[0],rrs,TFileStorageType.DURABLE); 
      }
      else if(testCondition == 4) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        pTest.doTestDesiredTotalRequestTime(surl[0],turl[0],rrs,10); 
      }
      else if(testCondition == 5) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        pTest.doTestDesiredTargetSURLLifeTime(surl[0],turl[0],rrs,10); 
      }
      else if(testCondition == 6) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
		    bufferSize,proxyString,pIntf);
        pTest.doTestStorageParam(surl[0],turl[0],rrs,uid); 
      }
      else if(testCondition == 7) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        pTest.doTestRetentionPolicy(surl[0],turl[0],rrs); 
      }
      else if(testCondition == 8) {
        SRMCopy pTest = new SRMCopy(request,logger,overwritefiles[0],dcau,
			bufferSize,proxyString,pIntf);
        surl[0] = target+"."+(testCondition-1);
        pTest.doLocalSRMCopy(surl[0],turl[0],rrs); 
      }
    }

    try {
     if(pushMode) {
       if(changeToCopyServicePath) {
         rrs.setSrmServicePath(copyServicePath);
       }
       util.webScript
        (publishUrlStr,displayType,"Sending",
			"Copy Request for Basic Test ... (Mode :Push)\n",
         rrs,"", timeStamp,fullTimeStamp, sTimeStamp, 
		 operatorTimeStamp, servicePath,noPublish,
         siteName,numOperations,typeString,"srmCopy-"+typeString,
	     "Sending", dailyTest,numSites,localPublish,bs,bsa);
     }
     else {
       if(changeToCopyServicePath) {
         rrs.setSrmServicePath(copyServicePath);
       }
       util.webScript
        (publishUrlStr,displayType,"Sending",
           "Copy Request for Basic Test ... (Mode: Pull)",
         rrs,"", timeStamp,fullTimeStamp, sTimeStamp, 
		 operatorTimeStamp, servicePath,noPublish,
         siteName,numOperations,typeString,"srmCopy-"+typeString,"Sending",
		 dailyTest,numSites,localPublish,bs,bsa);
     }
    }catch(Exception e) { e.printStackTrace(); }

    if(srm == null) {
      try {
       util.webScript(publishUrlStr,displayType,
        "Copy","FAILED",null,"Possibly SRM is down.",
           timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmCopy-"+typeString,
		   "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmCopy-"+typeString,"Sending",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      if(sLock != null) {
        sLock.setIncrementCount();
      }
      return;
    }

    if(srm == null) {
      util.printMessage("SRM is null", logger,pIntf);
      return;
    }

    SrmCopyResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmCopy(request);
      timeOutCallBack.setObject(response);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    }


    StringBuffer responseBuffer = new StringBuffer();
    HashMap statusArray = printCopyResult(response,responseBuffer);
    StringBuffer expBuffer = new StringBuffer();

    
    if(response == null || statusArray.size() == 0) {
     if(!responseBuffer.toString().equals("")) {
      notOkMsg.append(responseBuffer.toString());
     }
     reqOk = false;
    }

    if(response != null && response.getReturnStatus() != null 
		&&  response.getReturnStatus().getStatusCode() != null ) {
    if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
       response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ) {
         reqOk=true;
    }
   }

    boolean doStatusTest = true; 
    boolean testerTimedOut = false;
    URI keyURL = null;
    int i = 0;
    int startTimeStamp = util.startTimeStamp();
    while (statusArray.size() > 0) {
      expBuffer = new StringBuffer();
      notOkMsg = new StringBuffer();
// AS 070809
// changed from 5 sec to 60 sec
      Thread.sleep(statusWaitTime);
      if(!util.isRetryOk(startTimeStamp, retryTimeAllowed)) {
        util.printMessage("Check status exceeds max retry",logger,pIntf); 
        expBuffer.append("Notes from tester : status check exceeds time out");
        notOkMsg.append("Notes from tester : status check exceeds time out");
        testerTimedOut=true;
        HashMap mmap = copyOk.getSubOperations();
        subOk = (OperOk) mmap.get("srmCopy-"+typeString);
        if(subOk !=null) {
         subOk.setTried(true); 
         subOk.setExplanation("TimedOut");
        }
        break;
      }
      if(i >= statusArray.size()) {
        i = 0;
      }
      Object key = (statusArray.keySet().toArray()) [i];
      IsWholeRequestFailed wholeRequestFailed = 
			new IsWholeRequestFailed();
      TCopyRequestFileStatus status =
           (TCopyRequestFileStatus)(statusArray.get(key));
      TStatusCode code = status.getStatus().getStatusCode();
      StringBuffer rCode = new StringBuffer();
      expBuffer.delete(0,expBuffer.length());
      if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
         (code == TStatusCode.SRM_FILE_IN_CACHE) ||
       (code == TStatusCode.SRM_REQUEST_QUEUED)) {
        keyURL = (URI)key;
       /*
       if(doStatusTest && !detailed) {
         checkCopyStatusTest(srm,(URI)key,turl[0], response.getRequestToken(),1,sLock);
         checkCopyStatusTest(srm,(URI)key,turl[0], response.getRequestToken(),2,sLock);
       }
       */ 
       System.out.println("SRM-TESTER: Calling Copy Status " + new Date());
       status = checkCopyStatus(srm,
			(URI)key,turl[0], response.getRequestToken(), 
				rCode,doStatusTest,expBuffer,wholeRequestFailed,sLock);
       doStatusTest = false;
       TReturnStatus rStatus = new TReturnStatus();
       TStatusCode tempCode = util.mapReturnStatusValueBackToCode(rCode);
       if(tempCode != null) { 
          rStatus.setStatusCode(tempCode);
          //util.printMessage("Setting response status back " + 
			//tempCode,logger,pIntf); 
          response.setReturnStatus(rStatus);
       }
       if(status != null) {
        if(wholeRequestFailed.getWholeRequestFailed()) {
          rStatus.setStatusCode(TStatusCode.SRM_FAILURE);
          response.setReturnStatus(rStatus);
          code = TStatusCode.SRM_FAILURE;
        }
        else {
          code = status.getStatus().getStatusCode();
          notOkMsg.append(status.getStatus().getExplanation());
        }
       }
       else {
          TReturnStatus returnStatus = new TReturnStatus(); 
          code = util.mapReturnStatusValueBackToCode(rCode);
          if(code != null) {
            returnStatus.setStatusCode(code);
            if(status != null) {
             status.setStatus(returnStatus);
            }
            response.setReturnStatus(returnStatus);
          }
       }
      }
      if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
         (code != TStatusCode.SRM_FILE_IN_CACHE) &&
        (code != TStatusCode.SRM_REQUEST_QUEUED)) {
        util.printMessage("\nFileStatus code=" + code,logger,pIntf);
        if(status != null && status.getStatus() != null ) {
           util.printMessage("\nExplanation=" + 
				status.getStatus().getExplanation(), logger,pIntf);
        }
        //added file_in_cache for cern v2 server
        if(code == TStatusCode.SRM_FILE_PINNED || 
         code == TStatusCode.SRM_SUCCESS) {
        if(status != null && status.getTargetSURL() != null) {
           try {   
             util.webScript(publishUrlStr,displayType,
              "CopyStatusMethod",code.toString(),null,expBuffer.toString(),
              timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfCopyRequest-"+typeString,
		      "Ok",dailyTest,numSites,localPublish,bs,bsa);
              if(copyOk.getSubOperations().size() > 0 ) {
               HashMap mmap = copyOk.getSubOperations();
               OperOk gOk = (OperOk) mmap.get("srmStatusOfCopyRequest-"+typeString);
               if(gOk != null) {
                 gOk.setTried(true);
                 gOk.isOperOk(true);
               } 
              }
           }catch(Exception e) { e.printStackTrace(); }
           String tturl = status.getTargetSURL().toString();
           String sturl = status.getSourceSURL().toString();
           util.printMessage("\nSRM-TESTER: SourceSURL " + sturl,logger,pIntf); 
           util.printMessage("SRM-TESTER: TransferSURL " + tturl+"\n",logger,pIntf); 
           TReturnStatus rs1 = new TReturnStatus();
           rs1.setStatusCode(code);
           status.setStatus(rs1);
           TCopyRequestFileStatus[] statuses = new TCopyRequestFileStatus[1];
           statuses[0] = status;
           ArrayOfTCopyRequestFileStatus arrayofstatus =
              new ArrayOfTCopyRequestFileStatus();
           arrayofstatus.setStatusArray(statuses);
           response.setArrayOfFileStatuses(arrayofstatus);
           reqOk = true;
         }
         statusArray.remove(key);
       }
       /*
       else if(code == TStatusCode.SRM_FILE_IN_CACHE) {
          util.printMessage("SRM has locked the file and contacting remote SRM", 
			logger,pIntf);
       }
       */
       else {
        try {   
         util.webScript(publishUrlStr,displayType,
          "CopyStatusMethod",code.toString(),null,expBuffer.toString(),
           timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp, 
		   servicePath,noPublish, siteName,numOperations,
		   typeString,"srmStatusOfCopyRequest-"+typeString,
	       "Failed",dailyTest,numSites,localPublish,bs,bsa);
              if(copyOk.getSubOperations().size() > 0 ) {
               HashMap mmap = copyOk.getSubOperations();
               OperOk gOk = (OperOk) mmap.get("srmStatusOfCopyRequest-"+typeString);
               if(gOk != null ) {
                 gOk.setTried(true);
                 gOk.isOperOk(false);
                 if(code == TStatusCode.SRM_NOT_SUPPORTED) {
                   gOk.setExplanation("N.S.");
                 }
                 else {
                   gOk.setExplanation(expBuffer.toString());
                 }
               }
              }
        }catch(Exception e) { e.printStackTrace(); }
        if(code == null) {
          code = util.mapReturnStatusValueBackToCode(rCode);
        }
        TReturnStatus rs = new TReturnStatus();
        if(code != null) {
          rs.setStatusCode(code);
        }
        if(status != null && status.getStatus() != null &&
           status.getStatus().getExplanation() != null) {
            rs.setExplanation(status.getStatus().getExplanation());
            expBuffer.append(status.getStatus().getExplanation());
        }
        else {
          if(code != null) {
            rs.setExplanation(code.getValue());
            expBuffer.append(code.getValue());
          }
          else {  
            rs.setExplanation("");
          }
        }
        if(status != null) {
          status.setStatus(rs);
          TCopyRequestFileStatus[] statuses = new TCopyRequestFileStatus[1];
          statuses[0] = status;
          ArrayOfTCopyRequestFileStatus arrayofstatus =
            new ArrayOfTCopyRequestFileStatus();
          arrayofstatus.setStatusArray(statuses);
          response.setArrayOfFileStatuses(arrayofstatus);
        }
        statusArray.remove(key);
       }
     }
     else {
        //util.printMessage("FileStatus code=" + code,logger,pIntf);
        util.printMessageNL(".",logger,pIntf);
     }
    }

    //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);

    if(currTestCondition == 4 && keyURL != null) {
     util.printMessage("\nWaiting here to do the advanced test for srmStatusOfCopyRequest" +
        "\nfor condition wait for 120 seconds for the total request time ", logger,pIntf);
// AS 070809
// changed from 130000
     Thread.sleep(statusWaitTime);
     checkCopyStatusTest(srm,keyURL,turl[0], response.getRequestToken(),3,sLock);
    }

    if(currTestCondition == 5 && keyURL != null) {
     util.printMessage("\nWaiting here to do the advanced test for srmStatusOfCopyRequest" +
        "\nfor condition wait for 120 seconds for desired target surl life time ", logger,pIntf);
// AS 070809
// changed from 130000
     Thread.sleep(statusWaitTime);
     checkCopyStatusTest(srm,keyURL,turl[0], response.getRequestToken(),4,sLock);
    }

     if(reqOk) {
       String displayStatusMsg="Ok";
       try {
         rrs = new ReportRequestStatus();
         if(changeToCopyServicePath) {
           rrs.setSrmServicePath(copyServicePath);
         }
         rrs.setLinkName("copy_"+currTestCondition);
         rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
         if(currTestCondition == 4) {
          if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE ||
             response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_REQUEST_TIMED_OUT) {
             rrs.setMatchingInfo("Yes");
           }
           else {
             rrs.setMatchingInfo
				("No, expecting SRM_REQUEST_TIMED_OUT or SRM_FAILURE");
             displayStatusMsg="Failed";
           }
         }
         else {
          if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
             response.getReturnStatus().getStatusCode() == 
	         TStatusCode.SRM_NOT_SUPPORTED || 
		     response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
             rrs.setMatchingInfo("Yes");
             if(response.getReturnStatus().getStatusCode() == 	
				TStatusCode.SRM_NOT_SUPPORTED) {
               displayStatusMsg="N.S.";
             }
          }
          else {
             rrs.setMatchingInfo("No, expecting SRM_SUCCESS");
             displayStatusMsg="Failed";
          }
         } 

         if(displayStatusMsg.equals("Ok")) {
           HashMap mmap = copyOk.getSubOperations();
           OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
           if(ssubOk != null) {
              ssubOk.isOperOk(true);
           }
         }
         else {
           HashMap mmap = copyOk.getSubOperations();
           OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
           if(ssubOk != null) {
              ssubOk.setExplanation(displayStatusMsg);
           }
         }
         /* I don't think I need this also dec. 21, '06
       util.webScript(publishUrlStr,
        displayType,"Copy",response.getReturnStatus().getStatusCode().getValue(),
        rrs, "", timeStamp, fullTimeStamp, sTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"","Sending",dailyTest,numSites);
         */

       String exp = "";
       if(response.getReturnStatus().getExplanation() != null) {
         exp = response.getReturnStatus().getExplanation();
       }
       util.webScript(publishUrlStr,
        displayType,"Copy",response.getReturnStatus().getStatusCode().getValue(),
        rrs, exp, timeStamp, fullTimeStamp, sTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
		displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);

          if(browse) {
            OperOk copyLsOk = null;
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
              HashMap subMap =  ssubOk.getSubOperations();
              copyLsOk = (OperOk) subMap.get("srmls");
              if(copyLsOk != null) {
                copyLsOk.setTried(true);
              }
            }
            try {
               String tempServicePath = servicePath; 
               if(pushMode) {
                 proxyString = new StringBuffer();
                 int idx = turl[0].indexOf("?");
                 if(idx != -1) {
                   tempServicePath = turl[0].substring(0,idx);
                 }
                 srm = util.connectToSRM(tempServicePath,proxyString,
					logger,pIntf,defaultProxy, credential,proxyFile);
               } 
               SRMDirTest dirTest = 
			  	  new SRMDirTest(publishUrlStr, displayType,
               testType,timeStamp, fullTimeStamp, servicePath, credential, proxyFile,
                noPublish, detailed, defaultProxy,
			    siteName,numOperations,typeString, sTimeStamp, dailyTest, numSites,
				retryTimeAllowed, statusWaitTime,
				localPublish,bs,bsa,logger,pIntf,useDriverOn,pingOverAllSuccess);
               dirTest.setRedirectedSrmServicePath(changeToCopyServicePath,copyServicePath);
               if(copyLsOk != null) {
                 copyLsOk.setTried(true);
               }
               dirTest.doSrmLsTest
                  (srm,proxyString,uid, turl[0], null,copyLsOk,testCondition);
            }catch(Exception e) {

            try {
             rrs = new ReportRequestStatus();
             if(changeToCopyServicePath) {
               rrs.setSrmServicePath(copyServicePath);
             }
             rrs.setLinkName("browse_"+testCondition);
             rrs.setActualResult("Exception");
             rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
             util.webScript(publishUrlStr,displayType,
               "Browse","Exception", rrs,e.toString(),
               timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,"srmLs-"+typeString,"Exception",
			   dailyTest,numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,
               displayType,"BLANK","",
               null, "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, 
			   servicePath,noPublish, siteName,numOperations,
			   typeString,"srmLs-"+typeString,"Exception",
			   dailyTest,numSites,localPublish,bs,bsa);
             util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
            }catch(Exception ee) {ee.printStackTrace();}
              copyLsOk.setExplanation("Exception");
              e.printStackTrace();
              if(sLock != null) {
               sLock.setIncrementCount();
              }
            }
          }


          if(advisoryDelete) {
            OperOk copyRmOk = null;
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
              HashMap ssubMap = ssubOk.getSubOperations();
              copyRmOk = (OperOk) ssubMap.get("srmrm");
              if(copyRmOk != null) {
                copyRmOk.setTried(true);
              }
            }
             String tempServicePath = servicePath;
             if(pushMode) {
                 proxyString = new StringBuffer();
                 int idx = turl[0].indexOf("?");
                 if(idx != -1) {
                   tempServicePath = turl[0].substring(0,idx);
                 }
                 srm = util.connectToSRM
					(tempServicePath,proxyString,logger,pIntf,defaultProxy,
					    credential,proxyFile);
             } 
             StringBuffer explanationBuffer = new StringBuffer();
             StringBuffer statusBuffer = new StringBuffer();
             String statusMsg = doReleaseFile(srm, false,turl[0],
                response.getRequestToken(),statusBuffer,explanationBuffer,false,0);
             displayStatusMsg = "Ok";   
             if(statusBuffer.toString().equalsIgnoreCase("Yes") || 
			    statusBuffer.toString().equals("SRM_SUCCESS") || 
			    statusBuffer.toString().equals("SRM_NOT_SUPPORTED") || 
			    statusBuffer.toString().equals("SRM_DONE")) {
                 displayStatusMsg="Ok";
             }
             else {
                 displayStatusMsg="Failed";
             }
             if(displayStatusMsg.equals("Ok")) {
               copyRmOk.isOperOk(true);
             }
             else {
               copyRmOk.setExplanation(displayStatusMsg);
             }
             rrs = util.createReportRequestStatus(turl[0],"","");
             if(changeToCopyServicePath) {
                rrs.setSrmServicePath(copyServicePath); 
             }
             rrs.setUserId(uid);
             rrs.setMatchingInfo(statusMsg);
             rrs.setActualResult(statusBuffer.toString());
             rrs.setLinkName("srmrmbasic_"+currTestCondition);
             try {
                util.webScript(publishUrlStr,
                 displayType,"SendingPUTRelease","SrmRm", rrs,
                 "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,typeString,"srmRmAfterCopy-"+typeString,
				 "Sending",dailyTest,numSites,localPublish,bs,bsa);
                util.webScript(publishUrlStr,displayType,"PutRelease",
				  statusBuffer.toString(), rrs, explanationBuffer.toString(),
                  timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
                  siteName,numOperations,typeString,
		          "srmRmAfterCopy-"+typeString, 
				  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
             }catch(Exception e)  {e.printStackTrace();}

          }

          String expp = "";
		  if(response.getReturnStatus().getExplanation() != null) {
            expp = response.getReturnStatus().getExplanation();
          }
          displayStatusMsg = "Ok";
          if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
             response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
             response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED ||
             response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS ||
             response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
             if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
               displayStatusMsg="N.S.";
             }
          }
          else {
             displayStatusMsg="Failed";
          }
          if(displayStatusMsg.equals("Ok")) {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.isOperOk(true);
            }
          }
          else {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.setExplanation(displayStatusMsg);
            }
          }
          util.webScript(publishUrlStr,displayType,"RequestSummary",
             "SUCCESS",convertCopy (response,surl[0],turl[0]), 
			 expp, timeStamp,fullTimeStamp, sTimeStamp, 
	         operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
			 displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }

        if(sLock != null)  {
         sLock.setIncrementCount();
        }
     }
     else {
       String exp = getExplanationFromResponse(response);
       if(notOkMsg.toString().equals("") && !exp.equals("")) {
         notOkMsg.append(exp);
       }
       if(notOkMsg.toString().equals("")) {
        notOkMsg.append("Copy request failed, please check with SRM Admin.");
       }
       util.printMessage("SRM-TESTER: copy request failed \n", logger,pIntf);
       util.printMessage("SRM-TESTER: Testing " + testType + " failed ", logger,pIntf);
       String displayMsg = "Ok";
       try {
        rrs = new ReportRequestStatus();
        if(changeToCopyServicePath) {
          rrs.setSrmServicePath(copyServicePath);
        }
        rrs.setLinkName("copy_"+currTestCondition);
        rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
        if(currTestCondition == 4) {
        if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_TIMED_OUT) {
           rrs.setMatchingInfo("Yes");
        }
        else {
          rrs.setMatchingInfo("No, expecting SRM_REQUEST_TIMED_OUT or SRM_FAILURE");
         }
        }
        else {
        if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
           
          if(response.getReturnStatus().getStatusCode() == 
			 TStatusCode.SRM_NOT_SUPPORTED) {
            displayMsg = "N.S.";
          }
          else {
            displayMsg = "Ok";
          }
          rrs.setMatchingInfo("Yes");
        }
        else {
          rrs.setMatchingInfo("No, expecting SRM_SUCCESS");
          displayMsg = "Failed";
        }
          if(displayMsg.equals("Ok")) {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.isOperOk(true);
            }
          }
          else {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.setExplanation(displayMsg);
            }
          }
        }
        if(response.getReturnStatus().getExplanation() != null) {
          exp = response.getReturnStatus().getExplanation();
        }
        if(testerTimedOut) {
          util.webScript(publishUrlStr,displayType,"Copy",
           response.getReturnStatus().getStatusCode().getValue(),
           rrs, notOkMsg.toString(), timeStamp,fullTimeStamp, sTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,
		   "srmStatusOfCopyRequest-"+typeString,
		   "TimedOut",dailyTest,numSites,localPublish,bs,bsa);
              if(copyOk.getSubOperations().size() > 0 ) {
               HashMap mmap = copyOk.getSubOperations();
               OperOk gOk = (OperOk) mmap.get("srmStatusOfCopyRequest-"+typeString);
               if(gOk != null) {
                 gOk.setTried(true);
                 gOk.isOperOk(false);
                 gOk.setExplanation("TimedOut");
               }
              }
          util.webScript(publishUrlStr,displayType,"Copy",
           response.getReturnStatus().getStatusCode().getValue(),
           rrs, notOkMsg.toString(), timeStamp,fullTimeStamp, sTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
		   "TimedOut",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,displayType,"RequestSummary",
		    response.getReturnStatus().getStatusCode().getValue(),
            convertCopy(response,surl[0],turl[0]),
            notOkMsg.toString(), timeStamp,fullTimeStamp, 
			sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
		    "TimedOut",dailyTest,numSites,localPublish,bs,bsa);
        }
        else {
         util.webScript(publishUrlStr,displayType,"Copy",
           response.getReturnStatus().getStatusCode().getValue(),
           rrs, notOkMsg.toString(), timeStamp,fullTimeStamp, sTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
		   displayMsg,dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,displayType,"RequestSummary","Failed",
            convertCopy(response,surl[0],turl[0]),
            exp, timeStamp,fullTimeStamp, sTimeStamp, operatorTimeStamp,	
			servicePath,noPublish,
            siteName,numOperations,typeString,"srmCopy-"+typeString+"-OverAll",
			displayMsg,dailyTest,numSites,localPublish,bs,bsa);
        } 
       }catch(Exception e) {
         e.printStackTrace();
       }
       if(sLock != null) {
         sLock.setIncrementCount();
       }
    }  
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkCopyStatusTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  checkCopyStatusTest
    (ISRM srm, URI url, String turl, String token, int condition,SharedObjectLock sLock)
      throws URI.MalformedURIException, java.rmi.RemoteException {

  Date d = new Date ();
  String operatorTimeStamp = d.toString();
   if(token == null)  {
     throw new java.rmi.RemoteException("Request token from server cannot be null.");
   }

   if(condition == 1) {
     util.printMessage("\nSRM-TESTER: " + new Date()+
		" Calling CopyStatus for Bad requestToken " +
            token+"_bad_token", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_INVALID_REQUEST", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token+"_bad_token");


     SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
     r.setRequestToken(token+"_bad_token");
     try {

     SrmStatusOfCopyRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfCopyRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     if(!explanation.equals("")) {
       util.printMessage("Explanation="+explanation,logger,pIntf);
     }
     else {
       util.printMessageNL(".",logger,pIntf);
     }

    if(result.getReturnStatus().getStatusCode() == 
		TStatusCode.SRM_INVALID_REQUEST || 
	   result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE)
    {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadtoken_"+currTestCondition);

        try {
         util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"CopyStatus","Yes", rrs,
					explanation,
             timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest",
		     "Sending",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {

        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
        rrs.setLinkName("statusbadtoken_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"CopyStatus",
				"No. expecting SRM_INVALID_REQUEST || SRM_FAILURE", rrs,
				explanation, timeStamp, fullTimeStamp, sTimeStamp, 
				operatorTimeStamp, servicePath,noPublish,
                siteName,numOperations,typeString,
				"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST");
      rrs.setLinkName("statusbadtoken_"+currTestCondition);
      try {
       util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,
			"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
        displayType,"CopyStatus","SRM returned null status", rrs,
          "SRM returned null status",
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
		  "srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
    }catch(Exception me) {
		int idx = me.getMessage().indexOf("No scheme found in URI");
        int idx1 = me.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
        if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
        }
        else {
          throw new java.rmi.RemoteException(me.getMessage());
        }
    }
   }
   else if(condition == 2) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling CopyStatus for requestToken " + token, logger,pIntf);
     util.printMessage("\nBad SURL " + url+"_"+"bad_surl", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FAILURE", logger,pIntf);
     URI uuri = new URI(url.toString()+"_"+"bad_surl");

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_FAILURE");

     ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];

     ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
     rrfs.setSourceUrl(uuri.toString());

    reportFileStatus[0] = rrfs;

    rrs.setReportRequestFileStatus(reportFileStatus);


     SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
     r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(uuri));
     r.setArrayOfTargetSURLs(util.formArrayOfAnyURI(new URI(turl)));
     r.setRequestToken(token);
     try {

     SrmStatusOfCopyRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfCopyRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nSRM-TESTER: Status from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
        explanation = "";
     }
     util.printMessage("Explanation="+explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {

        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadsurl_"+currTestCondition);
        try {
         util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad SourceURL", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"CopyStatus","Yes", rrs, explanation,
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {

       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_FAILURE");
       rrs.setLinkName("statusbadsurl_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad SourceURL", rrs,
          "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"CopyStatus","No. expecting SRM_FAILURE", rrs,
  	      explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {

      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FAILURE");
      rrs.setLinkName("statusbadsurl_"+currTestCondition);
      try {
         util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for Bad SourceURL", rrs,
          "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"CopyStatus","SRM returned null status", rrs,
          "SRM returned null status",
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
    }catch(Exception me) {
		int idx = me.getMessage().indexOf("No scheme found in URI");
        int idx1 = me.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
        if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
        }
        else {
          throw new java.rmi.RemoteException(me.getMessage());
        }
    }
   } 
   else if(condition == 3) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling CopyStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_REQUEST_TIMED_OUT", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_REQUEST_TIMED_OUT");


     SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
     r.setRequestToken(token);
     try {
     SrmStatusOfCopyRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfCopyRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) { 
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null && result.getReturnStatus() != null) {

     util.printMessage("\nStatus from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
        explanation = "";
     }
     util.printMessage("Explanation="+explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_REQUEST_TIMED_OUT) {

       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("Yes");
       rrs.setLinkName("statusexpiredtoken_"+currTestCondition);
       try {
        util.webScript(publishUrlStr,
         displayType,"SendingCOPYSTATUS","Copy Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"CopyStatus","Yes", rrs,
           "status=" + result.getReturnStatus().getStatusCode().toString()+ 
		   explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken_"+currTestCondition);

       try {
        util.webScript(publishUrlStr,
         displayType,"SendingCOPYSTATUS","Copy Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"CopyStatus","No. expecting SRM_REQUEST_TIMED_OUT", rrs,
					explanation,
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken_"+currTestCondition);
       try {
        util.webScript(publishUrlStr,
         displayType,"SendingCOPYSTATUS","Copy Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"CopyStatus","SRM returned null status", null,
          "SRM returned null status", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
    }
    }catch(Exception me) {
		 int idx = me.getMessage().indexOf("No scheme found in URI");
         int idx1 = me.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
         if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
         }
         else {
           throw new java.rmi.RemoteException(me.getMessage());
         }
    }
   }
   else if(condition == 4) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling CopyStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token);
     rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
     //rrs.setExpectedResult("SRM_FILE_LIFETIME_EXPIRED");


     SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
     r.setRequestToken(token);
     try {

     SrmStatusOfCopyRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfCopyRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     String  explanation = result.getReturnStatus().getExplanation(); 
     if(explanation == null) {
        explanation=""; 
     }
     util.printMessage("Explanation="+explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS || 
		 result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {

       TCopyRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfCopyRequest) " +
            code, logger,pIntf);
         explanation = fileStatus.getStatus().getExplanation(); 
         if(explanation == null) {
            explanation=""; 
         }
         util.printMessage("\nExplanation : " + explanation, logger,pIntf);
         if(code == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
           rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
           rrs.setMatchingInfo("Yes");
           rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);

           try {
            util.webScript(publishUrlStr,
              displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
              "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"CopyStatus","Yes", rrs,
              "status=" + fileStatus.getStatus().getStatusCode().toString()+ 
			   explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
          rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
          rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
          try {
            util.webScript(publishUrlStr,
              displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
              "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"CopyStatus","No. expecting SRM_FILE_LIFETIME_EXPIRED", rrs,
		   	 explanation,
             timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
            "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"CopyStatus",result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest","Failed",
		  dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",
		   dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
         displayType,"CopyStatus",result.getReturnStatus().getStatusCode().toString(), rrs,
		   explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, 
		   servicePath,noPublish, siteName,numOperations,
		   typeString,"srmStatusOfCopyRequest","Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
       rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
      try {
       util.webScript(publishUrlStr,
          displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfCopyRequest",
		   "Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
         displayType,"CopyStatus","SRM returned null status", rrs,
         "SRM returned null status",
         timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
         siteName,numOperations,typeString,"srmStatusOfCopyRequest",
		 "Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
    }
     }catch(Exception re) {
         try {
          rrs.setActualResult("");
          rrs.setMatchingInfo("");
          rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
          util.webScript(publishUrlStr,
            displayType,"SendingCOPYSTATUS","Copy Status for expired pin lifetime ", rrs,
            "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
            rrs.setLinkName("exception_"+currTestCondition);
          util.webScript(publishUrlStr,displayType,"PException","",rrs,
            re.getMessage(),timeStamp,fullTimeStamp, sTimeStamp, 
		    operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfCopyRequest",
		    "Exception",dailyTest,numSites,localPublish,bs,bsa);
          rrs.setMatchingInfo("");
          rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
          util.webScript(publishUrlStr,
            displayType,"CopyStatus","FAILED", rrs,
		    "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,"srmStatusOfCopyRequest",
		    "Failed",dailyTest,numSites,localPublish,bs,bsa);
         }catch(Exception we) { we.printStackTrace(); }
		 int idx = re.getMessage().indexOf("No scheme found in URI");
         int idx1 = re.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
         if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
         }
         else {
           throw new java.rmi.RemoteException(re.getMessage());
         }
         //re.printStackTrace();
     }
   }
   else if(condition == 5) {
     util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling CopyStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     if(changeToCopyServicePath) {
       rrs.setSrmServicePath(copyServicePath);
     }
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_FILE_LIFETIME_EXPIRED");


     SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
     r.setRequestToken(token);
     try {

     SrmStatusOfCopyRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfCopyRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String  explanation = result.getReturnStatus().getExplanation(); 
     if(explanation == null) {
        explanation=""; 
     }
     util.printMessage("Explanation="+explanation, logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE || 
		 result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {

       TCopyRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfCopyRequest) " +
            code, logger,pIntf);
         explanation = fileStatus.getStatus().getExplanation(); 
         if(explanation == null) {
           explanation=""; 
         }
         util.printMessage("FileStatus Explanation="+explanation, logger,pIntf);
         if(code == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
           rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
           rrs.setMatchingInfo("Yes");
           rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
           try {
            util.webScript(publishUrlStr,
             displayType,"SendingCOPYSTATUS","Copy Status for expired file lifetime ", rrs,
             "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"CopyStatus","Yes", rrs,
		       explanation,
             timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest","Ok",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
          rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
          rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
          try {
            util.webScript(publishUrlStr,
             displayType,"SendingPUTSTATUS","Copy Status for expired file lifetime ", rrs,
             "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest","Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"CopyStatus","No. expecting SRM_FILE_LIFETIME_EXPIRED", rrs,
		       explanation,
             timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfCopyRequest","Failed",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        try {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Copy Status for expired file lifetime ", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Sending",
		   dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"CopyStatus",result.getReturnStatus().getStatusCode().toString(), rrs,
		   explanation, timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, 
		   servicePath,noPublish, siteName,numOperations,
		   typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
       rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Copy Status for expired file lifetime ", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
           displayType,"CopyStatus",result.getReturnStatus().getStatusCode().toString(), rrs,
		     explanation, timeStamp, fullTimeStamp, sTimeStamp, 
		 	 operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"","Failed",
			 dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    } 
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
      rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
      try {
        util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Copy Status for expired file lifetime ", rrs,
           "", timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"CopyStatus","SRM returned null status", rrs,
            "SRM returned null status",
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
    }catch(Exception me) {
		 int idx = me.getMessage().indexOf("No scheme found in URI");
         int idx1 = me.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
         if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
         }
         else {
           throw new java.rmi.RemoteException(me.getMessage());
         }
    }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkCopyStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TCopyRequestFileStatus checkCopyStatus
    (ISRM srm, URI url, String turl, String token, 
	  StringBuffer rCode, boolean statusTest, 
	  StringBuffer expBuffer, IsWholeRequestFailed wholeRequestFailed,
		SharedObjectLock sLock)
      throws URI.MalformedURIException, java.rmi.RemoteException {
        
  Date d = new Date ();
  String operatorTimeStamp = d.toString();
    if(token == null)  {
      throw new java.rmi.RemoteException("Request token from server cannot be null.");
    }

    if(statusTest) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling CopyStatus for requestToken=" + token+"\n", 
				logger,pIntf);
      //util.printMessage("\nSURL " + url, logger,pIntf);
      //util.printMessage("\nTURL " + turl,logger,pIntf);

      ReportRequestStatus rrs = new ReportRequestStatus();
      if(changeToCopyServicePath) {
        rrs.setSrmServicePath(copyServicePath);
      }
      rrs.setRequestId(token);

      ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];

      ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
      rrfs.setSourceUrl(url.toString());

      reportFileStatus[0] = rrfs;

      rrs.setReportRequestFileStatus(reportFileStatus);

    }

    SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
   
    r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(url));
    r.setArrayOfTargetSURLs(util.formArrayOfAnyURI(new URI(turl)));
    r.setRequestToken(token);
    TCopyRequestFileStatus fileStatus = null;

    try {
    SrmStatusOfCopyRequestResponse result = null;
    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setRequestToken(token,srm);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmStatusOfCopyRequest(r);
      timeOutCallBack.setObject(result);
    }catch(Exception e) { 
      timeOutCallBack.interruptNow(true);
       throw e;
    } 

    if(result == null) {
      util.printMessage("SRM returned null result ", logger,pIntf); 
      try {
        util.webScript(publishUrlStr,
          displayType,"CopyStatusMethod","Failed", null,
          "SRM returned null result",
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmCopy-"+typeString,
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e){e.printStackTrace();}
      return fileStatus;
    }

    util.printMessage("Status from SRM (srmStatusOfCopyRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
    String explanation = "";
    if (result.getReturnStatus().getExplanation() != null) {
      explanation = result.getReturnStatus().getExplanation();
      expBuffer.append(explanation);
	}
    if(!explanation.equals("")) {
      util.printMessage("Explanation=" + explanation, logger,pIntf);
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
            TStatusCode.SRM_FAILURE) {
       rCode.append("SRM_FAILURE");
       wholeRequestFailed.setWholeRequestFailed(true);
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_REQUEST_SUSPENDED) {
       rCode.append("SRM_REQUEST_SUSPENDED");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS) {
       rCode.append("SRM_SUCCESS");
    }
    else {
      rCode.append(result.getReturnStatus().getStatusCode().getValue());
    }

    if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
      result.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED ||
      result.getReturnStatus().getStatusCode() == 
		TStatusCode.SRM_REQUEST_INPROGRESS ||
      result.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
      result.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) 
    { ; }
    else {
      try {
        util.webScript(publishUrlStr,
          displayType,"CopyStatusMethod",result.getReturnStatus().getStatusCode().toString(), 
		  null, explanation,
          timeStamp, fullTimeStamp, sTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfCopyRequest-"+typeString,
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
              if(copyOk.getSubOperations().size() > 0 ) {
               HashMap mmap = copyOk.getSubOperations();
               OperOk gOk = (OperOk) mmap.get("srmStatusOfCopyRequest-"+typeString);
               if(gOk != null) {
                 gOk.setTried(true);
                 gOk.isOperOk(false);
                 gOk.setExplanation(explanation);
               }
              }
      }catch(Exception e){e.printStackTrace();}
    }


    if(result.getArrayOfFileStatuses() != null) {
        fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
    }

    util.printMessageNL(".",logger,pIntf);

    }catch(Exception me) {
		int idx = me.getMessage().indexOf("No scheme found in URI");
        int idx1 = me.getMessage().indexOf
		  ("Cannot initialize URI with empty parameters");
        if(idx != -1 || idx1 != -1) {
          util.printMessage("\nignoring this exception ...",logger,pIntf);
          rCode.append("SRM_REQUEST_QUEUED");
        }
        else {
          throw new java.rmi.RemoteException(me.getMessage());
        }
    }
    return fileStatus;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printCopyResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private HashMap printCopyResult (SrmCopyResponse response, 
		StringBuffer responseStatus) {

   HashMap result = new HashMap();
   Date d = new Date ();
   String operatorTimeStamp = d.toString();
   if(response == null) {
     util.printMessage("\nSRM-TESTER: ...Output from SRM..."+new Date(),logger,pIntf);
     util.printMessage("+++\tCopyRequest Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }
   util.printMessage("\nSRM-TESTER: ...Output from SRM..."+new Date(),logger,pIntf);
   if(response.getRequestToken() != null) { 
     util.printMessage("request.token    : \t" + 
		response.getRequestToken().toString(),logger,pIntf);
   }
   else {
     util.printMessage("request.token    : \t" + null,logger,pIntf);
   }
   if(response.getReturnStatus() == null) {
    try {
     util.webScript(publishUrlStr,
        displayType,"Copy","Failed",
        null, "Null return status", timeStamp, fullTimeStamp, sTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmCopy-"+typeString,
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
    }catch(Exception e) {}
    responseStatus.append("SRM returned null return status");
    return result;
   }
   else {
     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
     String displayStatusMsg = "Ok";
     if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED ||
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS ||
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
        if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
         displayStatusMsg="N.S.";
        }
     }
     else {
       displayStatusMsg="Failed";
     }
          if(displayStatusMsg.equals("Ok")) {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.isOperOk(true);
            }
          }
          else {
            HashMap mmap = copyOk.getSubOperations();
            OperOk ssubOk = (OperOk) mmap.get("srmCopy-"+typeString);
            if(ssubOk != null) {
               ssubOk.setExplanation(displayStatusMsg);
            }
            if(displayStatusMsg.equals("N.S.")) {
               OperOk gOk = (OperOk) mmap.get("srmStatusOfCopyRequest-"+typeString);
               if(gOk != null) {
                 gOk.setExplanation(displayStatusMsg);
               }
            }
          }
     String explanation = "";
     if(response.getReturnStatus().getExplanation() != null) {
       explanation = response.getReturnStatus().getExplanation();
     }
     try {
      util.webScript(publishUrlStr,
        displayType,"Copy",
		response.getReturnStatus().getStatusCode().getValue(),
        null, explanation, timeStamp, fullTimeStamp, sTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmCopy-"+typeString,
		displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
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

  if(response.getArrayOfFileStatuses() != null) {
     if(response.getArrayOfFileStatuses().getStatusArray() != null) {
        int size = response.getArrayOfFileStatuses().getStatusArray().length;

        for (int i=0;  i < size; i++) {
          TCopyRequestFileStatus fileStatus =
              response.getArrayOfFileStatuses().getStatusArray(i);
          util.printMessage("\n\tSURL="+
             fileStatus.getSourceSURL().toString(),logger,pIntf);
          util.printMessage("\n\tTURL="+
             fileStatus.getTargetSURL().toString(),logger,pIntf);
          util.printMessage("\tStatus="+
             fileStatus.getStatus().getStatusCode(),logger,pIntf);
          util.printMessage("\tExplanation="+
             fileStatus.getStatus().getExplanation(),logger,pIntf);
          result.put(fileStatus.getSourceSURL(), fileStatus);
          util.printMessage("............",logger,pIntf);
        }
     }
  }

  util.printMessage("===========================================",logger,pIntf);
  return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getExplanationFromResponse
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String getExplanationFromResponse (SrmCopyResponse response)
{
   String exp = "";
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..."+new Date(),logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   if(response.getReturnStatus() != null) {
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) 
       exp = explanation + " ";
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TCopyRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
         int size = fileStatuses.length;
         ReportRequestFileStatus [] reportFileStatus =
            new ReportRequestFileStatus[size];
         for (int i = 0 ; i < size; i++) {
           TCopyRequestFileStatus fileStatus =
              response.getArrayOfFileStatuses().getStatusArray(i);
           if(fileStatus != null) {
             String explanation = fileStatus.getStatus().getExplanation();
             if(explanation != null) {
               exp = exp + explanation;
             }
           }
         }
      }
   }
   return exp;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// convertCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ReportRequestStatus convertCopy (SrmCopyResponse response, 
		String surl, String turl)
{
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..."+new Date(),logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   ReportRequestStatus rrs = new ReportRequestStatus();
   if(changeToCopyServicePath) {
      rrs.setSrmServicePath(copyServicePath);
   }

   if(response.getRequestToken() != null) 
     rrs.setRequestId(""+response.getRequestToken());
   if(response.getReturnStatus() != null) {
     rrs.setRequestState(response.getReturnStatus().getStatusCode().getValue());
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) 
       rrs.setExplanation(explanation);
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TCopyRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
         int size = fileStatuses.length;
         ReportRequestFileStatus [] reportFileStatus =
            new ReportRequestFileStatus[size];
         for (int i = 0 ; i < size; i++) {
           TCopyRequestFileStatus fileStatus =
              response.getArrayOfFileStatuses().getStatusArray(i);
           ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
           rrfs.setSourceUrl(surl);
           rrfs.setTargetUrl(turl);
           if(fileStatus != null) {
           if(fileStatus.getStatus() != null) {
             if(fileStatus.getStatus().getStatusCode() != null) {
               rrfs.setState(fileStatus.getStatus().getStatusCode().getValue());
             }
           }
           String explanation = fileStatus.getStatus().getExplanation();
           if(explanation != null) {
             rrfs.setExplanation(explanation);
             util.printMessage("Explanation " + explanation, logger,pIntf);
           }
           }
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
    StringBuffer explanationBuffer, boolean detailedTest,
    int releaseTestCondition) throws Exception {

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);

  if(requestToken != null) {
    SrmRmRequest req = new SrmRmRequest();
    //req.setKeepSpace(new Boolean(keepSpace));
    //req.setKeepSpace(new Boolean(true));
    //req.setRequestToken(requestToken);
    if(!uid.equals("")) {
      req.setAuthorizationID(uid);
    }
    req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));

    if(!detailedTest) {
      util.printMessageHL("\nSRM-TESTER: " + new Date() +
		" Calling SrmRm ...\n", logger,pIntf);
      //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf); 
      util.printMessage("=============================",logger,pIntf);
      if(!uid.equals("")) {
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
      }
      util.printMessage("SRM-TESTER: SURL="+siteUrl,logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
    }
    else {
      if(currTestCondition == 1 && releaseTestCondition == 2) {
        uri = new org.apache.axis.types.URI (siteUrl+"_bad_surl");
        req = new SrmRmRequest();
        //req.setKeepSpace(new Boolean(keepSpace));
        //req.setKeepSpace(new Boolean(true));
        //req.setRequestToken(requestToken);
        if(!uid.equals("")) {
          req.setAuthorizationID(uid);
        }
        req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));

        util.printMessage("\nSRM-TESTER: " + 
			new Date() + 
				" Sending ReleaseFile Test for Bad SURL", logger,pIntf);
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
        util.printMessage("Bad SURL="+siteUrl+"_bad_surl",logger,pIntf);
        util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
      }
      else if(currTestCondition == 1 && releaseTestCondition == 1) {

        util.printMessage("\nSRM-TESTER: " +
			new Date() + " Sending srmRm Test for already released SURL",
                logger,pIntf);
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
        util.printMessage("SURL="+siteUrl,logger,pIntf);
        util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
      }
    }

    if(srm == null) {
      return "POSSIBLY_SRM_DOWN";
    }

    SrmRmResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmRm(req);
      timeOutCallBack.setObject(result);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
       throw e;
    }
    util.printMessage("SRM-TESTER: ...Output from SRM..."+new Date(),logger,pIntf);
    String explanation = "";
    if(result != null) {
      if(result.getReturnStatus() != null) {
        if(result.getReturnStatus().getStatusCode() != null) {
          util.printMessage("\tstatus="+
            result.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
        }
        else {
          util.printMessage("Null return status code", logger,pIntf);
        }
        explanation = result.getReturnStatus().getExplanation();
        util.printMessage("\texplanation="+ explanation, logger,pIntf);
      }
      else {
        util.printMessage("Null return status", logger,pIntf);
      }
      if(result.getArrayOfFileStatuses() != null) {
        ArrayOfTSURLReturnStatus arrayOfStatuses = result.getArrayOfFileStatuses();
        if(arrayOfStatuses != null) {
           TSURLReturnStatus[] rStatuses = arrayOfStatuses.getStatusArray();
           if(rStatuses != null) {
             if(rStatuses.length > 0) {
               TSURLReturnStatus rStatus = rStatuses[0]; 
               TReturnStatus rrStatus = rStatus.getStatus();
               if(rrStatus != null) {
                 TStatusCode temp = rrStatus.getStatusCode();
                 util.printMessage("\tdetails="+ temp,logger,pIntf);
                 util.printMessage("==============================\n",
						logger,pIntf);
                 util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf); 
                 String t_explanation = rrStatus.getExplanation();
                 statusBuffer.append(temp.getValue());
                 if(t_explanation != null) {
                    explanationBuffer.append(t_explanation);
                 }
                 else {
                  if(explanation !=null) {
                     explanationBuffer.append(explanation);
                  }
                 }
                 if(releaseTestCondition == 1 || releaseTestCondition == 2) {
                   if(temp == TStatusCode.SRM_INVALID_PATH ||
                      temp == TStatusCode.SRM_FAILURE) {
                      return "Yes";
                   }
                   else {
                      return "No. expecting SRM_INVALID_PATH";
                   }
                 }
                if(releaseTestCondition == 0) {
                   if(temp == TStatusCode.SRM_SUCCESS || 
						temp == TStatusCode.SRM_DONE ||
                        temp == TStatusCode.SRM_NOT_SUPPORTED  ||
                        temp == TStatusCode.SRM_RELEASED ) {
                      return "Yes";
                   }
                   else {
                      return "No. expecting SRM_SUCCESS " +
						"or SRM_RELEASED or SRM_NOT_SUPPORTED";
                   }
                }
                return temp.getValue();
               }
               else {
                 util.printMessage("Null ReturnStatus ", logger,pIntf);
               }
             }  
             else {
              util.printMessage("Null TSURLReturnStatus ", logger,pIntf);
             }
           }
           else {
             util.printMessage("Null ArrayOfFileStatuses.getStatusArray ",
				logger,pIntf);
           }
        }
        else { 
           util.printMessage("Null result.getArrayOfFileStatuses ",logger,pIntf);
        }
        return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
      }
      else {
        statusBuffer.append(result.getReturnStatus().getStatusCode().getValue());
        if(explanation != null) {
          explanationBuffer.append(explanation);
        }
        if(releaseTestCondition == 1 || releaseTestCondition == 2) {
            return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
        }
        return result.getReturnStatus().getStatusCode().getValue();
      }
    }
     else {
      explanationBuffer.append("null result from SRM");
      return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
    }
  }
  else {
     explanationBuffer.append("Cannot do release file for null request token");
     return "Cannot release for null request token";
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

