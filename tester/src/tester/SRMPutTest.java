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
import gov.lbl.srm.client.intf.PrintIntf;

import gov.lbl.srm.client.main.MySRMFileTransfer;


import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMPutTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMPutTest implements threadIntf
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
   private boolean gridFTPDone;
   private boolean abortOnFail;
   private String spaceToken="";
   private boolean advisoryDelete;
   private String uid="";
   private boolean reserveSpace;
   private int currTestCondition;
   private Log logger;
   private boolean alreadyReleasedSpace;
   private boolean dcau=true;
   private int bufferSize;
   private int parallelism;
   private PrintIntf pIntf;
   private String siteName;
   private int numOperations;
   private boolean dailyTest;
   private String startTimeStamp;
   private int numSites;
   private int retryTimeAllowed;
   private int statusWaitTime=15; //default 
   private String typeString = "srmPrepareToPut";
   private boolean localPublish;
   private boolean plainPut;
   private BufferedWriter bs;
   private BufferedWriter bsa;
   private SharedObjectLock sLock;
   private String[] surl = new String[1]; 
   private long[] fSize = new long[1]; 
   private String notOkMsg = "";
   private String copySourceFile = "";
   private boolean browse;
   private int testCondition;
   private int remoteFileLifeTime=0;
   private StringBuffer proxyString = new StringBuffer();
   private OperOk canPutContinue; 
   private OperOk rmOk = null;
   private boolean useGUC=true;
   private String gucScriptPath="";
   private String tempScriptPath="";
   private TimeOutCallBack timeOutCallBack;
   private boolean useDriverOn;
   private OperOk pingOverAllSuccess;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMPutTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMPutTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp,
	   String servicePath, GSSCredential credential, String proxyFile, 
	   boolean onlyGsiFTPGet, boolean noPublish, 
	   boolean detailed, boolean threepartycopy, boolean defaultProxy, 
	   boolean advisoryDelete, String uid, boolean dcau,
	   int bufferSize, int parallelism, String siteName, int numOperations,
       String startTimeStamp, boolean dailyTest, 
	   int numSites, int retryTimeAllowed, 
       int statusWaitTime, boolean localPublish, BufferedWriter bs,
       BufferedWriter bsa, Log logger, int remoteFileLifeTime,
	   PrintIntf pIntf, boolean useGUC, String gucScriptPath,
	   String tempScriptPath, boolean useDriverOn, OperOk pingOverAllSuccess) {

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
   this.siteName = siteName;
   this.numOperations = numOperations;
   this.dailyTest = dailyTest;
   this.startTimeStamp = startTimeStamp;
   this.numSites = numSites;
   this.retryTimeAllowed = retryTimeAllowed;
   this.statusWaitTime = statusWaitTime * 1000;
   this.uid = uid;
   this.localPublish = localPublish;
   this.bs = bs;
   this.bsa = bsa;
   this.logger = logger;
   this.remoteFileLifeTime = remoteFileLifeTime;
   this.pIntf = pIntf;
   this.useGUC = useGUC;
   this.gucScriptPath = gucScriptPath;
   this.tempScriptPath = tempScriptPath;
   this.useDriverOn = useDriverOn;
   this.pingOverAllSuccess = pingOverAllSuccess;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doPutFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doPutFile(ISRM srm, 
	StringBuffer proxyString, String source, String target, 
    String copySourceFile, boolean plainPut, boolean reservedSpace, 
	boolean browse, boolean overwrite, String sToken, SharedObjectLock sLock, 
	int testCondition, OperOk canPutContinue, boolean abortOnFail,
	boolean directGsiFTP, TFileStorageType fileStorageType) throws Exception {

 this.canPutContinue = canPutContinue;
 this.proxyString = proxyString;
 this.testCondition = testCondition;
 this.browse = browse;
 this.copySourceFile = copySourceFile;
 this.plainPut = plainPut;
 this.sLock = sLock;
 File f = null;
 boolean writeFileOk = true;
 boolean isPutOk = true; 
 String path = "";
 long size = 0;
 this.reserveSpace = reservedSpace;
 this.alreadyReleasedSpace=false;
 this.spaceToken = sToken;
 this.abortOnFail = abortOnFail;

 this.currTestCondition = testCondition;
 String temp = "";

 Date d = new Date ();
 String operatorTimeStamp = d.toString();

 if(plainPut) {

   //create a temporary small file to put in the server
   try {
     /*
     f = new File("./srm-tester-put.txt");
     if(f.exists()) {
      f.delete();
      f = new File("./srm-tester-put.txt");
     }
     path = f.getAbsolutePath();
     FileOutputStream fos = new FileOutputStream(f);
     BufferedWriter out = new BufferedWriter (new OutputStreamWriter(fos));

     out.write("srm-tester test");
     out.close();
     fos.close();
     f = new File("./srm-tester-put.txt");
     size = f.length();
     temp = util.getTargetFileName("file:///"+path);
     */
     temp = source;
     String ttt = SRMPutTest.parseLocalSourceFileForPath (temp);
     f = new File(ttt);
     if(f.exists()) {
       size = f.length();
     }
   }catch(Exception e) {
     notOkMsg="Cannot create sample put file " + path;
     util.printMessage(notOkMsg,logger,pIntf);
     writeFileOk = false;
   }
  }
  else {;
    temp = source;
  }

  String tt = "";

  util.printMessageHL("\nSRM-TESTER: " +
	new Date() + " Calling Put request ...\n",logger,pIntf);
  //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);



  if(target.equals("")) {
    notOkMsg = "Please provide the -remotetarget <remotetarget>";
    util.printMessage("\n\n" + notOkMsg ,logger,pIntf);
    isPutOk = false;
  }

  if(isPutOk && writeFileOk) { 

    if(detailed) {
      if(testCondition == 5) {
        surl[0]=target+"."+(testCondition-1);
      }
      else if(testCondition == 6) {
        surl[0]=target+"."+(testCondition-2);
      }
      else {
        surl[0]=target+"."+testCondition;
      }
    }
    else {
      surl[0] = target;
    }

    if(plainPut) {
      fSize[0]=size;
    }
    else {
      fSize[0]=0;
    }
   
   ReportRequestStatus rrs = new ReportRequestStatus();

   if(!directGsiFTP) {
    if(reservedSpace) {
      SrmReserveSpaceRequest spaceRequest = new SrmReserveSpaceRequest();
      if(!uid.equals("")) {
        spaceRequest.setAuthorizationID(uid);
      }
      spaceRequest.setUserSpaceTokenDescription("test description");
      spaceRequest.setDesiredSizeOfTotalSpace(new UnsignedLong(100000000));
      spaceRequest.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(1000));
      spaceRequest.setDesiredLifetimeOfReservedSpace(new Integer(10000));

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

      rrs.setLinkName("reservespace_"+testCondition);
      util.printMessage("\nUID " + uid,logger,pIntf);
      rrs.setUserId(uid);
      rrs.setUserDescription("test description");
      rrs.setTotalSpace(""+1000);
      rrs.setGuarnSpace(""+1000);
      rrs.setLifeTime(""+10000);
      HashMap resultMap = new HashMap();

      util.webScript(publishUrlStr,
       displayType,"SendingRESERVESPACE","Sending ReserveSpace", rrs,
       "", timeStamp, fullTimeStamp, startTimeStamp, 
       operatorTimeStamp,
       servicePath,noPublish,
	   siteName,numOperations,typeString,"srmReserveSpace",
       "Sending",dailyTest,numSites,localPublish,bs,bsa);
      rrs.setLinkName("reservespace_"+testCondition);
      util.webScript(publishUrlStr,
       displayType,"ReserveSpace","Sending", rrs,
       "", timeStamp, fullTimeStamp, startTimeStamp, 
       operatorTimeStamp,
		servicePath,noPublish,
	   siteName,numOperations,typeString,"srmReserveSpace",
	   "Sending", dailyTest,numSites,localPublish,bs,bsa);
 
      if(spaceResponse != null) {
      if(spaceResponse.getReturnStatus() != null) {
        TReturnStatus spaceStatus = spaceResponse.getReturnStatus();
        TStatusCode sCode = spaceStatus.getStatusCode();
        resultMap.put(uid,sCode);
        String rToken = spaceResponse.getRequestToken();
        util.printMessage("Status Code for spaceStatus " + 
				sCode.toString(), logger,pIntf);
        util.printMessage("Request token " + rToken, logger,pIntf);
        if(sCode == TStatusCode.SRM_REQUEST_QUEUED ||
           sCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
           util.webScript(publishUrlStr,
            displayType,"ReserveSpace",sCode.toString(), null,
            "", timeStamp, fullTimeStamp, startTimeStamp, 
            operatorTimeStamp,
	        servicePath,noPublish, siteName,numOperations,
	        typeString,"srmReserveSpace",
	        "Ok", dailyTest,numSites,localPublish,bs,bsa);
           int i = 0;
           while (resultMap.size() > 0) {
// AS 070809
// changed from 5000 to 60000
             Thread.sleep(statusWaitTime);
             if(i >= resultMap.size()) {
               i = 0;
             }
             if(rToken != null) {
               SrmStatusOfReserveSpaceRequestRequest spaceStatusRequest =
                      new SrmStatusOfReserveSpaceRequestRequest();
               spaceStatusRequest.setRequestToken(rToken);
               if(!uid.equals("")) {
                  spaceStatusRequest.setAuthorizationID(uid);
               } 

               SrmStatusOfReserveSpaceRequestResponse spaceStatusResponse=null;

               ssTimeStamp = util.startTimeStamp();
               timeOutCallBack = new TimeOutCallBack
					(spaceStatusResponse,ssTimeStamp);
               timeOutCallBack.setRequestToken(rToken,srm);
               timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
               timeOutCallBack.setSharedObjectLock(sLock);
               timeOutCallBack.start();

               try {
                 spaceStatusResponse =
                     srm.srmStatusOfReserveSpaceRequest(spaceStatusRequest);
                 timeOutCallBack.setObject(spaceStatusResponse);
               }catch(Exception e) {
                 timeOutCallBack.interruptNow(true);
                 throw e;
               } 
               if(spaceStatusResponse.getReturnStatus() != null) {
                 spaceStatus = spaceStatusResponse.getReturnStatus();
                 sCode = spaceStatus.getStatusCode();
                 util.printMessage("Status Code for spaceStatusRequest " + 
					sCode.toString(), logger,pIntf);
                 if(sCode != TStatusCode.SRM_REQUEST_INPROGRESS &&
                    sCode != TStatusCode.SRM_REQUEST_QUEUED) {
                      resultMap.remove(uid);
                      rrs.setActualResult(sCode.getValue());
                      spaceToken = spaceStatusResponse.getSpaceToken();
                      String displayStatusMsg = "Ok";
                      if(sCode == TStatusCode.SRM_SUCCESS ||
                         sCode == TStatusCode.SRM_LOWER_SPACE_GRANTED ||
                         sCode == TStatusCode.SRM_NOT_SUPPORTED) {
                         if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
                           displayStatusMsg = "N.S.";
                         }
                      }
                      else {
                        displayStatusMsg = "Failed";
                      }
                      try {
                        util.webScript(publishUrlStr,
                          displayType,"ReserveSpaceStatus",displayStatusMsg, 	
					      null, "", timeStamp, fullTimeStamp, startTimeStamp, 
                          operatorTimeStamp,
				          servicePath,noPublish, siteName,numOperations,
			              typeString,"srmStatusOfReserveSpaceRequest",
				          displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
                     }catch(Exception e) {e.printStackTrace();}
                 }
               }
               else {
                 util.printMessage
			       ("Null return status from srmStatusOfReserveSpaceRequest",
					logger,pIntf);
                 try {
                   util.webScript(publishUrlStr,
                     displayType,"ReserveSpaceStatus","Failed", null,
                     "null return status from server", 
					 timeStamp, fullTimeStamp, startTimeStamp, 
                     operatorTimeStamp,
				     servicePath,noPublish, siteName,numOperations,
			         typeString,"srmStatusOfReserveSpaceRequest",
					 "Failed", dailyTest,numSites,localPublish,bs,bsa);
                 }catch(Exception e) {e.printStackTrace();}
               }
              }
              else {
                 util.printMessage("Expecting requestToken for this status code ", 
			   	  logger,pIntf);
                 resultMap.remove(uid);
                 try {
                   util.webScript(publishUrlStr,
                     displayType,"ReserveSpaceStatus","Failed", null,
                     "Expecting request token for this status code", 
					 timeStamp, fullTimeStamp, startTimeStamp, 
                            operatorTimeStamp,
				     servicePath,noPublish, siteName,numOperations,
			         typeString,"srmStatusOfReserveSpaceRequest",
					 "Failed", dailyTest,numSites,localPublish,bs,bsa);
                 }catch(Exception e) {e.printStackTrace();}
              }
            }
        }
        else {
           rrs.setActualResult(sCode.getValue());
           util.printMessage("Status Code for spaceRequest " + sCode.toString(), 
					logger,pIntf);
           spaceToken = spaceResponse.getSpaceToken();
           util.printMessage("SpaceToken " + spaceToken, logger,pIntf);
           String displayStatusMsg = "Ok";
           if(sCode != TStatusCode.SRM_SUCCESS && sCode != TStatusCode.SRM_DONE) {
             displayStatusMsg="Failed";
           }
           util.webScript(publishUrlStr,
            displayType,"ReserveSpace",displayStatusMsg, null,
            "", timeStamp, fullTimeStamp, startTimeStamp, 
            operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
			typeString,"srmReserveSpace",
			displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
        }

        if(sCode == TStatusCode.SRM_SUCCESS || sCode == TStatusCode.SRM_DONE) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        }

        util.printMessage("SpaceToken " + spaceToken, logger,pIntf);
        if(spaceToken != null && !spaceToken.equals("")) {
          util.printMessage("\n\nTargetSpaceToken " + spaceToken,logger,pIntf);
          rrs.setSpaceToken(spaceToken);
        }
      }
    }
    else {
       rrs.setActualResult("Null status");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
    }

    if(spaceToken.equals("")) {
       util.printMessage("ReserveSpaceRequest failed", logger,pIntf);
    }


     if(spaceToken == null || spaceToken.equals("")) {
        util.printMessage("Cannot continue this reserveput test, "+
				" since reserve space failed", logger,pIntf);
        if(sLock != null) {
          sLock.setIncrementCount();
        }
        return;
     }
    }
    }

    SrmPrepareToPutRequest request = new SrmPrepareToPutRequest();

    if(!directGsiFTP) {
      if(spaceToken != null && !spaceToken.equals("")) {
        request.setTargetSpaceToken(spaceToken);
      }
    }

    if(detailed) {
      if(testCondition == 0) {
        rrs = util.createReportRequestStatus("","",tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestSimple("user description test string",rrs);
      }
      if(testCondition == 1) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestSimple_1(surl[0],"user description test string",fSize[0],rrs);
      }
      else if (testCondition == 2) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestStorageType (surl[0],"user description test string", 
			TFileStorageType.VOLATILE,fSize[0],rrs);
      }
      else if (testCondition == 3) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestStorageType (surl[0],"user description test string", 
			TFileStorageType.PERMANENT,fSize[0],rrs);
      }
      else if (testCondition == 4) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestStorageType (surl[0],"user description test string", 
			TFileStorageType.DURABLE,fSize[0],rrs);
      }
      else if (testCondition == 5) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,false,dcau,bufferSize,pIntf);
        pTest.doTestOverwrite (surl[0],"user description test string", 
			fSize[0],rrs);
      }
      else if (testCondition == 6) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,true,dcau,bufferSize,pIntf);
        pTest.doTestOverwrite (surl[0],"user description test string", 
			fSize[0],rrs);
      }
      else if (testCondition == 7) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestRequestTime
			(surl[0],"user description test string", 60,0,0,fSize[0],rrs);
      }
      else if (testCondition == 8) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestRequestTime
			(surl[0],"user description test string", 0,60,0,fSize[0],rrs);
      }
      else if (testCondition == 9) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestRequestTime
			(surl[0],"user description test string", 0,0,60,fSize[0],rrs);
      }
      else if (testCondition == 10) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestStorageParam
			(surl[0],"user description test string", uid,fSize[0],rrs);
      }
      else if (testCondition == 11) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestTransferParam 
			(surl[0],"user description test string",fSize[0],rrs);
      }
      else if (testCondition == 12) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestRetentionPolicy 
			(surl[0],"user description test string",fSize[0],rrs);
      }
      else if (testCondition == 13) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doReserveSpacePut
			(surl[0],fSize[0],spaceToken,rrs);
      }
      else if(testCondition == -1) {
        rrs = util.createReportRequestStatus(temp,surl[0],tt);
        if(spaceToken != null) {
         rrs.setSpaceToken(spaceToken);
        }
        SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
			(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
        pTest.doTestBasic(surl[0],fSize[0],rrs,fileStorageType,remoteFileLifeTime); 
      }
    } 
    else {
        if(threepartycopy) {
          util.printMessage("\nSRM-TESTER: Running 3partycopy Test ", logger,pIntf); 
          rrs = util.createReportRequestStatus(temp,surl[0],tt);
          SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
				(request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
          if(spaceToken != null) {
           rrs.setSpaceToken(spaceToken);
          }
          pTest.doTest3partycopy(surl[0],fSize[0],rrs); 
        }
        else {
          rrs = util.createReportRequestStatus(temp,surl[0],tt);
          if(!directGsiFTP) {
            SRMPrepareToPutTest pTest = new SRMPrepareToPutTest
		  	  (request,logger,proxyString,overwrite,dcau,bufferSize,pIntf);
            if(spaceToken != null) {
              rrs.setSpaceToken(spaceToken);
            }
            pTest.doTestBasic(surl[0],fSize[0],rrs,fileStorageType,remoteFileLifeTime); 
          }
        }
    }

    if(plainPut) {
     if(detailed) {
       try {
         util.webScript
          (publishUrlStr,displayType,"Sending","Put Request for Advanced Test ...",
           rrs,"", timeStamp,fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
	       siteName,numOperations,typeString,
		   "srmPrepareToPut","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
     else {
       if(!directGsiFTP) {
       try {
         util.webScript
          (publishUrlStr,displayType,"Sending","Put Request for Basic Test ...",
           rrs,"", timeStamp,fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
	       siteName,numOperations,typeString,
		   "srmPrepareToPut","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
       }  
     }
   } 
   else {
     try {
       util.webScript
        (publishUrlStr,displayType,"Sending","Put Request now...",
         rrs, "", timeStamp,fullTimeStamp, startTimeStamp,
		 operatorTimeStamp, servicePath,noPublish,
	     siteName,numOperations,typeString,
		 "srmPrepareToPut","Sending",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
    }

    if(srm == null) {
      if(!directGsiFTP) {
      try {
       util.webScript(publishUrlStr,displayType,
        "Put","FAILED",null,"Possibly SRM is down.",
           timeStamp,fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
	       siteName,numOperations,typeString,"srmPrepareToPut",
		   "Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp,
	    operatorTimeStamp, servicePath,noPublish,
	    siteName,numOperations,typeString,
		"srmPrepareToPut","Failed",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      if(sLock != null) {
        sLock.setIncrementCount();
      }
      canPutContinue.isOperOk(false);
      return;
      } 
    }

    StringBuffer gsiFTPSTimeStamp = new StringBuffer();
    StringBuffer gsiFTPETimeStamp = new StringBuffer();

    if(directGsiFTP) {

     temp = util.doValidateCheckingForFormat(temp);
     String turl = util.doValidateCheckingForFormat(target);

     //String size = ""+0;

     util.printMessage ("SRM-TESTER: copying the file to " +
            temp + "\n",logger,pIntf);

     gsiFTPSTimeStamp = util.getTimeStamp();
     StringBuffer expBuffer = new StringBuffer();

     ThreadCallBack tcb = new ThreadCallBack(this,"gridftp");
     Hashtable paramTable = new Hashtable();
     paramTable.put("tsource",temp);
     paramTable.put("turl",turl);
     paramTable.put("source",source);
     paramTable.put("starttimestamp",gsiFTPSTimeStamp);
     paramTable.put("endtimestamp",gsiFTPETimeStamp);
     paramTable.put("optimestamp",operatorTimeStamp);
     if(srm != null) {
       paramTable.put("srm",srm);
     }
     paramTable.put("canPutContinue",canPutContinue);
     paramTable.put("expbuffer",expBuffer);
     paramTable.put("lsize",new Long(size));
     paramTable.put("turl",turl);
     paramTable.put("direct",new Boolean(true));
     tcb.setParams_2(paramTable);
     tcb.start();


     int gTimeStamp = util.startTimeStamp();

     while (!gridFTPDone) {
// AS 070809
// changed from 5000 to 30000
        Thread.sleep(statusWaitTime);
        if(!util.isRetryOk(gTimeStamp,retryTimeAllowed)) {
          gridFTPDone = true;
          tcb.stop();
          gsiFTPETimeStamp = util.getTimeStamp();
          util.printMessage("\n......................................",
            logger,pIntf);
          util.printMessage("GsiFTP failed with timeout (5): ",
            logger,pIntf);
          util.printMessage("......................................\n",
            logger,pIntf);
          try {
           rrs = util.createAndConvertRequest(temp,turl,0,-1);
           rrs.setActualResult("FAILED");
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
             displayType,"GridFTP","GsiFTP Failed with timeout",
             rrs, "GsiFTP Failed with timeout",
             timeStamp, fullTimeStamp, startTimeStamp,
             operatorTimeStamp, servicePath,noPublish,
             siteName, numOperations,typeString,"GridFTP-Put",
             "TimeOut",dailyTest, numSites,localPublish,bs,bsa);

             util.webScript(publishUrlStr,
               displayType,"RequestSummary","Failed during GsiFTP",
               convertPut(null,temp,"","",surl[0],""+size,
			   expBuffer.toString()),
               "",timeStamp,fullTimeStamp, startTimeStamp,
               operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,"GridFTP-Put",
               "Failed",dailyTest, numSites,localPublish,bs,bsa);
              canPutContinue.isOperOk(false);
            }catch(Exception  e) { e.printStackTrace(); }
            doReleaseSpace(srm,spaceToken);
            if(sLock != null) {
             sLock.setIncrementCount();
            }
          }
      }
      return; 
    }

    SrmPrepareToPutResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
               timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      response = srm.srmPrepareToPut(request);
      timeOutCallBack.setObject(response);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
    } 

    String turl = "";
    boolean reqOk = false;

    StringBuffer responseBuffer = new StringBuffer();
    HashMap statusArray = printPutResult(response,responseBuffer);
    StringBuffer expBuffer = new StringBuffer();

    if(response == null || statusArray.size() == 0) {
     if(!responseBuffer.toString().equals("")) {
      notOkMsg=responseBuffer.toString();
     }
     reqOk = false;
    }

    if(response != null && response.getReturnStatus() != null && 
		response.getReturnStatus().getStatusCode() != null ) {
    if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
       response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ) {
         reqOk=true;
    }
  }


    boolean doStatusTest = true; 
    boolean testerTimedOut=false;
    URI keyURL = null;
    StringBuffer rCode = new StringBuffer();
    int i = 0;
    int sTimeStamp = util.startTimeStamp();
    while (statusArray.size() > 0) {
      expBuffer = new StringBuffer();
      notOkMsg = "";
// AS 070809
// changed from 5000 to 60000
      Thread.sleep(statusWaitTime);
      if(!util.isRetryOk(sTimeStamp,retryTimeAllowed)) {
         util.printMessage("Max retry check status exceeded ", logger,pIntf);
         expBuffer.append("Notes from tester : Max retry check exceeded.");
         notOkMsg="Notes from tester : Max retry check exceeded.";
         testerTimedOut=true;
         break;
      }
      if(i >= statusArray.size()) {
        i = 0;
      }
      Object key = (statusArray.keySet().toArray()) [i];
      TPutRequestFileStatus status =
           (TPutRequestFileStatus)(statusArray.get(key));
      TStatusCode code = status.getStatus().getStatusCode();
      rCode = new StringBuffer();
      expBuffer.delete(0,expBuffer.length());
      IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
      if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
       (code == TStatusCode.SRM_REQUEST_QUEUED)) {
        keyURL = (URI)key;
       /*
       if(doStatusTest && !detailed && !threepartycopy) {
         checkPutStatusTest(srm,(URI)key,response,1);
         checkPutStatusTest(srm,(URI)key,response,2);
       }
       */
       System.out.println("SRM-TESTER: Calling PutStatus " + new Date());
       status = checkPutStatus(srm,
			(URI)key,response, rCode,doStatusTest, expBuffer,
				wholeRequestFailed);
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
          if(code != null) {
            returnStatus.setStatusCode(code);
            if(status != null) {
             status.setStatus(returnStatus);
            }
          }
          //especially for INFN cases gSoap throwing exception null pointer
          else {
            TPutRequestFileStatus status2 =
               (TPutRequestFileStatus)(statusArray.get(key));
            code = status2.getStatus().getStatusCode();
            returnStatus.setStatusCode(code);
            if(status != null) {
             status.setStatus(returnStatus);
            }
          }
       }
      }
      util.printMessage("\nFileStatus code=" + code,logger,pIntf);
      if(status !=null && status.getStatus() != null) {
        util.printMessage("\nExplanation=" + status.getStatus().
			getExplanation(),	logger,pIntf);
      }
      if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
        (code != TStatusCode.SRM_REQUEST_QUEUED)) {
        //added file_in_cache for cern v2 server
        if(code == TStatusCode.SRM_SPACE_AVAILABLE || 
         code == TStatusCode.SRM_SUCCESS || 
         code == TStatusCode.SRM_DONE || 
		 code == TStatusCode.SRM_FILE_IN_CACHE) {
        if(status != null && status.getTransferURL() != null) {
          try {
             util.webScript(publishUrlStr,displayType,"PutStatusMethod",
		     code.toString(),null,
             expBuffer.toString(), timeStamp,fullTimeStamp ,startTimeStamp,
		     operatorTimeStamp, servicePath,noPublish,
		     siteName,numOperations,typeString,"srmStatusOfPutRequest",	
             "Ok",dailyTest,numSites,localPublish,bs,bsa);
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               pOk.isOperOk(true);
             }
           }catch(Exception e) {e.printStackTrace();}
           turl = status.getTransferURL().toString();
           TReturnStatus rs1 = new TReturnStatus();
           rs1.setStatusCode(code);
           status.setStatus(rs1);
           TPutRequestFileStatus[] statuses = new TPutRequestFileStatus[1];
           statuses[0] = status;
           ArrayOfTPutRequestFileStatus arrayofstatus =
              new ArrayOfTPutRequestFileStatus();
           arrayofstatus.setStatusArray(statuses);
           response.setArrayOfFileStatuses(arrayofstatus);
           reqOk = true;
         }
         statusArray.remove(key);
       }
       else {
        util.webScript(publishUrlStr,displayType,"PutStatusMethod",
		     code.toString(),null,
             expBuffer.toString(), timeStamp,fullTimeStamp ,startTimeStamp,
		     operatorTimeStamp, servicePath,noPublish,
		     siteName,numOperations,typeString,"srmStatusOfPutRequest",	
             "Failed",dailyTest,numSites,localPublish,bs,bsa);
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               pOk.isOperOk(false);
               if(code == TStatusCode.SRM_NOT_SUPPORTED) {
                 pOk.setExplanation("N.S.");
               }
               else {
                 pOk.setExplanation(expBuffer.toString());
               }
             }
        code = util.mapReturnStatusValueBackToCode(rCode);
        TReturnStatus rs = new TReturnStatus();
        if(code != null) {
          rs.setStatusCode(code);
        }
        if(status != null && status.getStatus() != null &&
           status.getStatus().getExplanation() != null) {
            rs.setExplanation(status.getStatus().getExplanation());
            //expBuffer.append(status.getStatus().getExplanation());
        }
        else {
          if(code != null) {
            rs.setExplanation(code.getValue());
            //expBuffer.append(code.getValue());
          }
          else {  
            rs.setExplanation("");
          }
        }
        response.setReturnStatus(rs);
        if(status != null) {
          status.setStatus(rs);
          TPutRequestFileStatus[] statuses = new TPutRequestFileStatus[1];
          statuses[0] = status;
          ArrayOfTPutRequestFileStatus arrayofstatus =
            new ArrayOfTPutRequestFileStatus();
          arrayofstatus.setStatusArray(statuses);
          response.setArrayOfFileStatuses(arrayofstatus);
        }
        statusArray.remove(key);
       }
     }
    }
   
    if(currTestCondition == 7 && keyURL != null) {
     util.printMessage("\nWaiting here to do the advanced test for srmStatusOfPutRequest" +
        "\nfor condition wait for 120 seconds for the total request time ", logger,pIntf);
// AS 070809
// changed from 130000 to 300000
     Thread.sleep(statusWaitTime);
     checkPutStatusTest(srm,keyURL,response,3);
    }



    if(reqOk) {
     rrs = new ReportRequestStatus();
     rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
     String displayStatusMsg = "Ok"; 
     if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE || 
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED || 
        response.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
       rrs.setMatchingInfo("Yes");
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
         displayStatusMsg="N.S."; 
       }
     }
     else {
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
       displayStatusMsg="Failed"; 
     }
     try {
      rrs.setLinkName("put_"+currTestCondition);
      util.webScript(publishUrlStr,displayType,"Put",
		 response.getReturnStatus().getStatusCode().getValue(),rrs,
         "Notes from tester : TransferURL has been returned.",
          timeStamp,fullTimeStamp ,startTimeStamp,
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"srmStatusOfPutRequest",	
          displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg+" " + expBuffer.toString());
               }
             }
     }catch(Exception e) { e.printStackTrace(); }

     util.printMessage("",logger,pIntf);
     util.printMessageHNL
         ("\nSRM-TESTER: " + new Date() + " TURL returned=",logger,pIntf);
     util.printMessageNL (turl +"\n",logger,pIntf);
     //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);

     temp = util.doValidateCheckingForFormat(temp);
     //commented for Neetu's case on Oct 28, 09
     //turl = util.doValidateCheckingForFormat(turl);
     gsiFTPSTimeStamp = util.getTimeStamp();


     ThreadCallBack tcb = new ThreadCallBack(this,"gridftp");
     Hashtable paramTable = new Hashtable();
     paramTable.put("tsource",temp);
     paramTable.put("turl",turl);
     paramTable.put("source",source);
     paramTable.put("starttimestamp",gsiFTPSTimeStamp);
     paramTable.put("endtimestamp",gsiFTPETimeStamp);
     paramTable.put("optimestamp",operatorTimeStamp);
     paramTable.put("srm",srm);
     paramTable.put("canPutContinue",canPutContinue);
     paramTable.put("expbuffer",expBuffer);
     paramTable.put("lsize",new Long(size));
     paramTable.put("turl",turl);
     paramTable.put("direct",new Boolean(false));
     paramTable.put("response",response);
     tcb.setParams_2(paramTable);
     tcb.start();

     int gTimeStamp = util.startTimeStamp();

     while (!gridFTPDone) {
// AS 070809
// changed from 5000 to 30000
        Thread.sleep(5000);
        if(!util.isRetryOk(gTimeStamp,retryTimeAllowed)) {
          gridFTPDone = true;
          tcb.stop();
          gsiFTPETimeStamp = util.getTimeStamp();
          util.printMessage("\n......................................",
            logger,pIntf);
          util.printMessage("GsiFTP failed with timeout (4): ",
            logger,pIntf);
          util.printMessage("......................................\n",
            logger,pIntf);
          try {
           rrs = util.createAndConvertRequest(temp,turl,0,-1);
           rrs.setActualResult("FAILED");
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
             displayType,"GridFTP","GsiFTP Failed with timeout",
             rrs, "GsiFTP Failed with timeout",
             timeStamp, fullTimeStamp, startTimeStamp,
             operatorTimeStamp, servicePath,noPublish,
             siteName, numOperations,typeString,"GridFTP-Put",
             "TimeOut",dailyTest, numSites,localPublish,bs,bsa);

             util.webScript(publishUrlStr,
               displayType,"RequestSummary","Failed during GsiFTP",
               convertPut(null,temp,"","",surl[0],""+size,
					expBuffer.toString()),
               "",timeStamp,fullTimeStamp, startTimeStamp,
               operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,"GridFTP-Put",
               "Failed",dailyTest, numSites,localPublish,bs,bsa);
              canPutContinue.isOperOk(false);
            }catch(Exception  e) { e.printStackTrace(); }
            doReleaseSpace(srm,spaceToken);
            if(sLock != null) {
             sLock.setIncrementCount();
            }
          }
     }

     doReleaseSpace(srm,spaceToken);
     if(sLock != null) { 
       sLock.setIncrementCount();
     }
   }
   else {
     canPutContinue.isOperOk(false);
     String exp = getExplanationFromResponse(response);
     if(!exp.equals("")) {
         notOkMsg = exp;
     }
     if(notOkMsg.equals("")) {
        notOkMsg="Put request failed, please check with SRM Admin.";
     }
     notOkMsg = notOkMsg + " " + expBuffer.toString();
     util.printMessage("SRM-TESTER: put request failed \n", logger,pIntf);
     util.printMessage("SRM-TESTER: Explanation " + notOkMsg, logger,pIntf);
     util.printMessage("SRM-TESTER: Testing " + testType + " failed ", logger,pIntf);
     try {
        rrs = new ReportRequestStatus();
        rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
        if(currTestCondition == 2) { 
         if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {
            rrs.setMatchingInfo("Yes");
         }
         else {
          rrs.setMatchingInfo("No. expecting SRM_FAILURE");
         }
        }
        else if(currTestCondition == 7) { 
         if(response.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_REQUEST_TIMED_OUT) {
              rrs.setMatchingInfo("Yes");
         }
         else {
          rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
         }
        }
        else {
         if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
            response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_REQUEST_QUEUED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
            rrs.setMatchingInfo("Yes");
         }
         else {
          rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
         }
       }
       rrs.setLinkName("put_"+currTestCondition);
       String displayStatusMsg = "Ok";
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
            response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
            response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
         if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           displayStatusMsg="N.S.";
         }
       }
       else {
         displayStatusMsg="Failed";
       }
       
       util.webScript(publishUrlStr,displayType,"Put",
	      response.getReturnStatus().getStatusCode().getValue(),
          rrs, notOkMsg, timeStamp,fullTimeStamp, startTimeStamp,
		  operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,typeString,
		  "srmStatusOfPutRequest","Failed",dailyTest,numSites,localPublish,bs,bsa);
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg + " " + expBuffer.toString());
               }
             }
       if(plainPut) { 
         if(testerTimedOut) {
           util.webScript(publishUrlStr,displayType,"Put",
	        response.getReturnStatus().getStatusCode().getValue(),
            rrs, notOkMsg, timeStamp,fullTimeStamp, startTimeStamp,
		    operatorTimeStamp, servicePath,noPublish,
	        siteName,numOperations,typeString,
		    "srmStatusOfPutRequest","TimedOut",dailyTest,
			numSites,localPublish,bs,bsa);
           util.webScript(publishUrlStr,displayType,"RequestSummary",
		   response.getReturnStatus().getStatusCode().toString(),
           convertPut(response,temp,"","",surl[0],""+size,notOkMsg),
           notOkMsg, timeStamp,fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,
		   "srmPrepareToPut-OverAll","TimedOut",dailyTest,
			numSites,localPublish,bs,bsa);
           canPutContinue.isOperOk(false);
           canPutContinue.setExplanation("TimedOut");
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               pOk.isOperOk(false);
               pOk.setExplanation("TimedOut");
             }
         }
         else {
           util.webScript(publishUrlStr,displayType,"RequestSummary",
		   response.getReturnStatus().getStatusCode().toString(),
           convertPut(response,temp,"","",surl[0],""+size,notOkMsg),
           notOkMsg, timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		   servicePath,noPublish,
		   siteName,numOperations,typeString,
		   "srmPrepareToPut-OverAll",displayStatusMsg,dailyTest,
		   numSites,localPublish,bs,bsa);
           if(displayStatusMsg.equals("Ok")) {
              canPutContinue.isOperOk(true);
           }
           else {
             canPutContinue.isOperOk(false);
             canPutContinue.setExplanation(displayStatusMsg);
           }
         }
       }
       else {
         util.webScript(publishUrlStr,displayType,
	  	  "3rd Party Copy","FAILED",null,
          expBuffer.toString(), 
		  timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,
		  "srmPrepareToPut","Failed",dailyTest,numSites,localPublish,bs,bsa);

         util.webScript(publishUrlStr, displayType,"Sending3END",
            "", null, "", timeStamp, fullTimeStamp, startTimeStamp,
		    operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
		    typeString,"","Failed",dailyTest,numSites,localPublish,bs,bsa);

         util.webScript(publishUrlStr,displayType,"RequestSummary",
		    response.getReturnStatus().getStatusCode().toString(),
            convertPut(response,copySourceFile,temp,"",surl[0],"-1",notOkMsg),
            "", timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		    servicePath,noPublish, siteName,numOperations,typeString,
		    "srmPrepareToPut","Failed",dailyTest,numSites,localPublish,bs,bsa);
       } 
     }catch(Exception e) {
          e.printStackTrace();
     } 
     doReleaseSpace(srm,spaceToken);
     if(sLock != null) {
       sLock.setIncrementCount();
     }
   }
  }  
  else {
    canPutContinue.isOperOk(false);
    try {
      util.webScript
        (publishUrlStr,displayType,"Put","SrmPrepareToPut FAILED",null,
        notOkMsg, timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		servicePath,noPublish,
        siteName,numOperations,typeString,"srmPrepareToPut",
		"Failed",dailyTest,numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }
  }

  //delete the temporary file created.
  /*
  try {
    if(f != null) 
      f.delete();
  }catch(Exception e) {}
  */
}


public void  checkPutStatusTest
    (ISRM srm, URI url, SrmPrepareToPutResponse response, 
	   int condition)
      throws URI.MalformedURIException, java.rmi.RemoteException {

   String token = response.getRequestToken();
   if(token == null)  {
     throw new java.rmi.RemoteException("Request token from server cannot be null.");
   }

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

   if(condition == 1) {
     util.printMessage("\nSRM-TESTER: " + new Date() + 
		" Sending PutStatus for Bad requestToken " +
            token+"_bad_token", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_INVALID_REQUEST", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token+"_bad_token");
     //rrs.setExpectedResult("SRM_INVALID_REQUEST");


     SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
     r.setRequestToken(token+"_bad_token");
     try {

     SrmStatusOfPutRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfPutRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nSRM-TESTER: Status from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     util.printMessage("\nSRM-TESTER: Explanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);
     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     System.out.println("Explanation="+explanation);
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
           displayType,"SendingPUTSTATUS","Put Status for Bad Token", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"PutStatus","Sending", rrs, explanation,
           timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {

        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
        rrs.setLinkName("statusbadtoken_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
           displayType,"PutStatus",
				"No. expecting SRM_INVALID_REQUEST || SRM_FAILURE", rrs,
					explanation,
            timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST");
      rrs.setLinkName("statusbadtoken_"+currTestCondition);
      try {
       util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for Bad Token", rrs,
            "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
        displayType,"PutStatus","SRM returned null status", rrs,
          "SRM returned null status",
        timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
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
		" Sending PutStatus for requestToken " + token, logger,pIntf);
     util.printMessage("\nBad SURL " + url+"_"+"bad_surl", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FAILURE", logger,pIntf);
     URI uuri = new URI(url.toString()+"_"+"bad_surl");

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_FAILURE");

     ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];

     ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
     rrfs.setSourceUrl(uuri.toString());

    reportFileStatus[0] = rrfs;

    rrs.setReportRequestFileStatus(reportFileStatus);


     SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
     r.setArrayOfTargetSURLs(util.formArrayOfAnyURI(uuri));
     r.setRequestToken(token);
     try {

     SrmStatusOfPutRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfPutRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) { 
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     util.printMessage("\nExplanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
        explanation = "";
     }

     if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {

        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadsurl_"+currTestCondition);
        try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for Bad SourceURL", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus","Sending", rrs, explanation,
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {

       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_FAILURE");
       rrs.setLinkName("statusbadsurl_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for Bad SourceURL", rrs,
          "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus","No. expecting SRM_FAILURE", rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {

      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FAILURE");
      rrs.setLinkName("statusbadsurl_"+currTestCondition);
      try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for Bad SourceURL", rrs,
          "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus","SRM returned null status", rrs,
          "SRM returned null status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
    } catch(Exception me) { 
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
		" Sending PutStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_REQUEST_TIMED_OUT", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_REQUEST_TIMED_OUT");


     SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
     r.setRequestToken(token);
     try {

     SrmStatusOfPutRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfPutRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null && result.getReturnStatus() != null) {

     util.printMessage("\nStatus from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     util.printMessage("\nExplanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);

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
         displayType,"SendingPUTSTATUS","Put Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"PutStatus",result.getReturnStatus().getStatusCode().toString(), rrs,
           "status=" + result.getReturnStatus().getStatusCode().toString()+ 
			explanation, timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,
		    noPublish, siteName,numOperations,
		    typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken_"+currTestCondition);

       try {
        util.webScript(publishUrlStr,
         displayType,"SendingPUTSTATUS","Put Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"PutStatus","No. expecting SRM_REQUEST_TIMED_OUT", rrs,
					explanation,
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken_"+currTestCondition);
       try {
        util.webScript(publishUrlStr,
         displayType,"SendingPUTSTATUS","Put Status for expired Token", rrs,
          "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"PutStatus","SRM returned null status", null,
          "SRM returned null status", timeStamp, fullTimeStamp, 
		  startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,typeString,"","Sending",dailyTest,numSites,localPublish,bs,bsa);
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
		" Sending PutStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", 
			logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);
     rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
     //rrs.setExpectedResult("SRM_FILE_LIFETIME_EXPIRED");


     SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
     r.setRequestToken(token);
     try {

     SrmStatusOfPutRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfPutRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     util.printMessage("\nExplanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);
     String  explanation = result.getReturnStatus().getExplanation(); 
     if(explanation == null) {
        explanation=""; 
     }
     util.printMessage("Explanation="+explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS || 
		 result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {

       TPutRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfPutRequest) " +
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
              displayType,"SendingPUTSTATUS","Put Status for expired pin lifetime ", 
			  rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		      siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"PutStatus",fileStatus.getStatus().getStatusCode().toString(), rrs,
              "status=" + fileStatus.getStatus().getStatusCode().toString()+ 
					explanation,
             timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		     siteName, numOperations,typeString, "srmStatusOfPutRequest",
		     "Ok", dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
          rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
          try {
            util.webScript(publishUrlStr,
              displayType,"SendingPUTSTATUS","Put Status for expired pin lifetime ", rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
              siteName, numOperations,typeString, "","Failed", dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"PutStatus","No. expecting SRM_FILE_LIFETIME_EXPIRED", rrs,
					explanation,
             timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
             siteName, numOperations,typeString, "srmStatusOfPutRequest",
		       "Failed", dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for expired pin lifetime ", rrs,
            "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		    siteName, numOperations,typeString, "","Sending", 
			dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus",
			 result.getReturnStatus().getStatusCode().toString(), rrs,
		     explanation, timeStamp, fullTimeStamp, 
			 startTimeStamp,operatorTimeStamp, servicePath,noPublish,
             siteName, numOperations,typeString, "srmStatusOfPutRequest",
			"Failed", dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for expired pin lifetime ", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus",
		  response.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, 
		  startTimeStamp,operatorTimeStamp, servicePath,noPublish,
	      siteName, numOperations,typeString, "srmStatusOfPutRequest",
		  "Failed", dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
       rrs.setLinkName("statuslifetimeexpired_"+currTestCondition);
      try {
       util.webScript(publishUrlStr,
          displayType,"SendingPUTSTATUS","Put Status for expired pin lifetime ", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
         displayType,"PutStatus","SRM returned null status", rrs,
          "SRM returned null status",
         timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		 servicePath,noPublish,
		 siteName, numOperations,typeString, "srmStatusOfPutRequest",
		 "Sending", dailyTest,numSites,localPublish,bs,bsa);
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
   else if(condition == 5) {
     util.printMessage("\nSRM-TESTER: " + new Date() + 
	    " Sending PutStatus for requestToken " +
            token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);
     //rrs.setExpectedResult("SRM_FILE_LIFETIME_EXPIRED");


     SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
     r.setRequestToken(token);
     try {

     SrmStatusOfPutRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfPutRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
     util.printMessage("\nExplanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);

     String  explanation = result.getReturnStatus().getExplanation(); 
     if(explanation == null) {
        explanation=""; 
     }
     util.printMessage("Explanation="+explanation, logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE || 
		 result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {

       TPutRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfPutRequest) " +
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
             displayType,"SendingPUTSTATUS",
			 "Put Status for expired file lifetime ", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		     siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"PutStatus","Sending", rrs,
		       explanation,
             timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		     siteName, numOperations,typeString, "srmStatusOfPutRequest",
			   "Ok", dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
          rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
          rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
          try {
            util.webScript(publishUrlStr,
             displayType,"SendingPUTSTATUS",
			 "Put Status for expired file lifetime ", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
			 siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
             displayType,"PutStatus","No. expecting SRM_FILE_LIFETIME_EXPIRED", rrs,
		       explanation,
             timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfPutRequest",
			   "Failed",dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        try {
         rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for expired file lifetime ", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName, numOperations,typeString, "","Sending", dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
          displayType,"PutStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp,
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfPutRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
       rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
       try {
         util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS",
			"Put Status for expired file lifetime ", rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
	       siteName, numOperations,typeString, "","Failed", dailyTest,numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
           displayType,"PutStatus",
		   result.getReturnStatus().getStatusCode().toString(), rrs,
		   explanation, timeStamp, fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfPutRequest",
		   "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    } 
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
      rrs.setLinkName("statusexpiredfilelifetime_"+currTestCondition);
      try {
        util.webScript(publishUrlStr,
           displayType,"SendingPUTSTATUS","Put Status for expired file lifetime ", 
		   rrs,
           "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
		   siteName, numOperations,typeString, "","Failed", dailyTest,numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
          displayType,"PutStatus","SRM returned null status", rrs,
            "SRM returned null status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfPutRequest",
		     "Failed",dailyTest,numSites,localPublish,bs,bsa);
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
// checkPutStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TPutRequestFileStatus checkPutStatus
    (ISRM srm, URI url, SrmPrepareToPutResponse response,
	  StringBuffer rCode, boolean statusTest, 
	  StringBuffer expBuffer, IsWholeRequestFailed wholeRequestFailed) 
         throws URI.MalformedURIException, java.rmi.RemoteException {
        
    String token = response.getRequestToken ();

    Date d = new Date ();
    String operatorTimeStamp = d.toString();

    if(token == null)  {
      throw new java.rmi.RemoteException("Request token from server cannot be null.");
    }

    if(statusTest) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
		" Sending PutStatus for requestToken=" + 
			token, logger,pIntf);
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
         displayType,"Sending","Put Status Request now ...", rrs,
          "", timeStamp, fullTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"","Failed",dailyTest,numSites);
     }catch(Exception e) { e.printStackTrace(); }
     */
    }

    SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
   
    r.setArrayOfTargetSURLs(util.formArrayOfAnyURI(url));
    r.setRequestToken(token);
    TPutRequestFileStatus fileStatus = null;
    try {

    SrmStatusOfPutRequestResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setRequestToken(token,srm);
               timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmStatusOfPutRequest(r);
      timeOutCallBack.setObject(result);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
       throw e;
    }

    if(result == null) {
      util.printMessage("SRM returned null result",logger,pIntf); 
      try {
        util.webScript(publishUrlStr,
          displayType,"PutStatusMethod","Failed", null,
          "SRM returned null status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		  servicePath,noPublish, siteName,numOperations,
		  typeString,"srmStatusOfPutRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) {e.printStackTrace();}
           if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               pOk.isOperOk(false);
               pOk.setExplanation("SRM returned null status");
           }
      return fileStatus;
    }

    String explanation = "";
	if(result.getReturnStatus().getExplanation() != null) {
      explanation = result.getReturnStatus().getExplanation();
      util.printMessage("Explanation=" + explanation, logger,pIntf);
    }
    expBuffer.append(explanation);

    if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
     result.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
     result.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED ||
	 result.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS){
      ;
    }
    else {
      try {
        util.webScript(publishUrlStr,
          displayType,"PutStatusMethod",
		  result.getReturnStatus().getStatusCode().toString(), null,
          explanation, timeStamp, fullTimeStamp, startTimeStamp,
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfPutRequest",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
           if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putstatus");
               pOk.setTried(true);
               pOk.isOperOk(false);
               pOk.setExplanation(expBuffer.toString());
           }
       }catch(Exception e) {e.printStackTrace();}
    }

    util.printMessage("Status from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
    util.printMessage("Explanation from SRM (srmStatusOfPutRequest) " +
            result.getReturnStatus().getExplanation(), logger,pIntf);
    response.setReturnStatus(result.getReturnStatus());

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
            TStatusCode.SRM_DUPLICATION_ERROR) {
       rCode.append("SRM_DUPLICATION_ERROR");
    }
    else if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_SUCCESS) {
       rCode.append("SRM_SUCCESS");
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
// printPutResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private HashMap printPutResult (SrmPrepareToPutResponse response, 
		StringBuffer responseStatus) {

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

   HashMap result = new HashMap();
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM ..." + new Date(), logger,pIntf);
     util.printMessage("+++\tPutRequest Response is null +++",
		logger,pIntf);
     util.printMessage("==========================================",
		logger,pIntf);
     return null;
   }
   util.printMessage("SRM-TESTER:   ...Output from SRM ..." + new Date(), logger,pIntf);
   util.printMessage("==========================================",logger,pIntf);
   if(response.getRequestToken() != null) { 
     util.printMessage("request.token    : \t" + 
		response.getRequestToken().toString(),logger,pIntf);
   }
   else {
     util.printMessage("request.token    : \t" + null,logger,pIntf);
   }


   if(response.getReturnStatus() == null) {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     try {
        util.webScript(publishUrlStr,
          displayType,"Put","SRM returned null return status", null,
          "SRM returned null return status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmPrepareToPut",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
     }catch(Exception e) {e.printStackTrace();}
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
            TStatusCode.SRM_REQUEST_QUEUED || 
       response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_REQUEST_INPROGRESS || 
       response.getReturnStatus().getStatusCode() == 
            TStatusCode.SRM_NOT_SUPPORTED ) {
       if(response.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_NOT_SUPPORTED) {    
          displayStatusMsg = "N.S.";
       }
       String exp = "";
       if(response.getReturnStatus().getExplanation() != null) {
         exp = response.getReturnStatus().getExplanation();
       }
       try {
          util.webScript(publishUrlStr,
          displayType,"Put",
		  response.getReturnStatus().getStatusCode().toString(), null,
          exp, timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmPrepareToPut",
		  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) {e.printStackTrace();}
     }
     /*
     else if(response.getReturnStatus().getStatusCode() == 
               TStatusCode.SRM_REQUEST_QUEUED || 
             response.getReturnStatus().getStatusCode() == 
               TStatusCode.SRM_REQUEST_INPROGRESS) 
	 {; }
     */
     else {
       try {
          util.webScript(publishUrlStr,
          displayType,"Put",response.getReturnStatus().getStatusCode().toString(), null,
          "SRM returned null return status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmPrepareToPut",
		  "Failed",dailyTest,numSites,localPublish,bs,bsa);
       }catch(Exception e) {e.printStackTrace();}
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
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       util.printMessage("request.explanation \t" + explanation,logger,pIntf);
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
     if(response.getArrayOfFileStatuses().getStatusArray() != null) {
        int size = response.getArrayOfFileStatuses().getStatusArray().length;

        for (int i=0;  i < size; i++) {
          TPutRequestFileStatus fileStatus =
              response.getArrayOfFileStatuses().getStatusArray(i);
          util.printMessage("\n\tSURL="+
             fileStatus.getSURL().toString(),logger,pIntf);
          util.printMessage("\tStatus="+
             fileStatus.getStatus().getStatusCode(),logger,pIntf);
          util.printMessage("\tExplanation="+
             fileStatus.getStatus().getExplanation(),logger,pIntf);
          result.put(fileStatus.getSURL(), fileStatus);
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

private String getExplanationFromResponse (SrmPrepareToPutResponse response)
{
   String exp = "";

   if(response == null) {
     util.printMessage("==========================================",logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   if(response.getReturnStatus() != null) {
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       exp = explanation;     
     }
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TPutRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      int size = fileStatuses.length;
      if(fileStatuses != null) { 
         for (int i = 0 ; i < size; i++) {
           TPutRequestFileStatus fileStatus =
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
// convertPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ReportRequestStatus convertPut (SrmPrepareToPutResponse response, 
		String surl, String sturl, String tturl, String turl, String fSize, 
		String notOkMsg)
{
   if(response == null) {
     util.printMessage("==========================================",logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   ReportRequestStatus rrs = new ReportRequestStatus();

   if(response.getRequestToken() != null) 
     rrs.setRequestId(""+response.getRequestToken());
   if(response.getReturnStatus() != null) {
     rrs.setRequestState(response.getReturnStatus().getStatusCode().getValue());
     String explanation = response.getReturnStatus().getExplanation();
     if(explanation != null) {
       rrs.setExplanation(explanation);
     }
   }
   if(response.getRemainingTotalRequestTime() != null) {
     rrs.setRemainingTotalRequestTime(""+ response.getRemainingTotalRequestTime());
   }

   if(response.getArrayOfFileStatuses() != null) { 
      TPutRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      int size = fileStatuses.length;
      if(fileStatuses != null) { 
         ReportRequestFileStatus [] reportFileStatus =
            new ReportRequestFileStatus[size];
         for (int i = 0 ; i < size; i++) {
           TPutRequestFileStatus fileStatus =
              response.getArrayOfFileStatuses().getStatusArray(i);
           ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
           rrfs.setSourceUrl(surl);
           rrfs.setSourceTransferUrl(sturl);
           if(fileStatus.getTransferURL() != null) {
             rrfs.setTransferUrl(fileStatus.getTransferURL().toString());
           }
           else {
             rrfs.setTransferUrl(tturl);
           }
           rrfs.setTargetUrl(turl);
           rrfs.setState(fileStatus.getStatus().getStatusCode().getValue());
           String explanation = fileStatus.getStatus().getExplanation();
           if(explanation != null && !explanation.equals("")) {
             rrfs.setExplanation(explanation);
             util.printMessage("Explanation from server=" + 
				explanation, logger,pIntf);
           }
           else {
             rrfs.setExplanation(notOkMsg);
             if(!notOkMsg.equals("")) {
               util.printMessage("Notes from tester" + notOkMsg, logger,pIntf);
             }
           }
           rrfs.setSize(fSize);
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
      util.printMessageHL("\nSRM-TESTER: " +
		new Date() + " Calling SrmRm ...\n", logger,pIntf);

      util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      if(!uid.equals("")) {
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
      }
      util.printMessage("SURL="+siteUrl,logger,pIntf);
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

        util.printMessage("\nSending ReleaseFile Test for Bad SURL", logger,pIntf);
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
        util.printMessage("Bad SURL="+siteUrl+"_bad_surl",logger,pIntf);
        util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
      }
      else if(currTestCondition == 1 && releaseTestCondition == 1) {

        util.printMessage("\nSending srmRm Test for already released SURL",
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
               timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmRm(req);
      timeOutCallBack.setObject(result);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
       throw e;
    }

    util.printMessage("===============================",logger,pIntf);
    if(result != null) {
      util.printMessage("\tstatus="+
            result.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
      String explanation = result.getReturnStatus().getExplanation();
      util.printMessage("\texplanation="+ explanation, logger,pIntf);
      if(result.getArrayOfFileStatuses() != null) {
        TStatusCode temp = result.getArrayOfFileStatuses().getStatusArray(0).
            getStatus().getStatusCode();
        util.printMessage("\tdetails="+ temp,logger,pIntf); 
        util.printMessage("===============================\n",
			logger,pIntf);
        util.printMessageHL2("EndTime="+new Date(), logger,pIntf);
        String t_explanation =  
		  result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getExplanation();
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
          if(temp == TStatusCode.SRM_SUCCESS || temp == TStatusCode.SRM_DONE ||
                   temp == TStatusCode.SRM_RELEASED ) {
           return "Yes";
          }
          else {
           return "No. expecting SRM_SUCCESS or SRM_RELEASED";
          }
        }
        return temp.getValue();
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
      util.printMessage("\n===============================\n",
			logger,pIntf);
      return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
    }
  }
  else {
     explanationBuffer.append("Cannot do release file for null request token");
     return "Cannot do release file for null request token";
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doAbortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doAbortFiles(ISRM srm, String siteUrl,String requestToken,
  StringBuffer explanationBuffer, StringBuffer statusBuffer,
	String uid, boolean detailed, int abortTestCondition) 
	throws Exception {

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);
  SrmAbortFilesRequest req = new SrmAbortFilesRequest();
  req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
  req.setRequestToken(requestToken);

  if(!detailed) {
    util.printMessage("\nSRM-TESTER: " +
		new Date() + " Calling AbortFiles Test ...", logger,pIntf);
    util.printMessage("SRM-TESTER: ...Input paramters...", logger,pIntf);
    util.printMessage("=============================\n",logger,pIntf);
    util.printMessage("RequestToken="+requestToken,logger,pIntf);
    util.printMessage("SURL="+siteUrl,logger,pIntf);
    util.printMessage("ExpectedResult=SRM_SUCCESS",logger,pIntf);
  }
  else {
    if(abortTestCondition == 0) {
      util.printMessage("\nSending AbortFiles Test ...", logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_SUCCESS",logger,pIntf);
    }
    else if(abortTestCondition == 1) {
      req = new SrmAbortFilesRequest();
      req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
      req.setRequestToken(requestToken+"_bad_token");

      util.printMessage("\nSending AbortFiles Test for bad token", logger,pIntf);
      util.printMessage("RequestToken="+requestToken+"_bad_token",logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_REQUEST",logger,pIntf);
    }
    else if(abortTestCondition == 2) {
      uri = new org.apache.axis.types.URI (siteUrl+"_bad_surl");
      req = new SrmAbortFilesRequest();
      req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
      req.setRequestToken(requestToken);

      util.printMessage("\nSending AbortFiles Test for bad surl", logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl+"_bad_surl",logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
    }
    else if(abortTestCondition == 3) {
      util.printMessage("\nSending AbortFiles Test ...", logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
    }
  }

  try {
     SrmAbortFilesResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmAbortFiles(req);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
       TStatusCode temp = result.getReturnStatus().getStatusCode();
       util.printMessage("Result status="+temp.getValue(), logger,pIntf);
       String explanation = result.getReturnStatus().getExplanation();
       util.printMessage("Result explanation="+explanation, logger,pIntf);

       TSURLReturnStatus status = result.getArrayOfFileStatuses().
                           getStatusArray(0);
       if(status != null) {
         util.printMessage("SRM-TESTER:  ...Output from SRM..." + new Date(),logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
         util.printMessage("SURL="+status.getSurl().toString(),logger,pIntf);
         util.printMessage("status="+status.getStatus().getStatusCode(),logger,pIntf);
         util.printMessage(".............................",logger,pIntf);
         temp = status.getStatus().getStatusCode();
         statusBuffer.append(temp.getValue());
         if(status.getStatus().getExplanation() != null) {
            explanationBuffer.append(status.getStatus().getExplanation());
         }
         if(abortTestCondition == 1) {
           if(temp == TStatusCode.SRM_INVALID_REQUEST || 
              temp == TStatusCode.SRM_FAILURE) {
              return "Yes";
           }
           return "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE";
         }
         else if(abortTestCondition == 2 || abortTestCondition == 3) { 
           if(temp == TStatusCode.SRM_INVALID_PATH || 
			  temp == TStatusCode.SRM_FAILURE) {
              return "Yes";
           }
           return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
         }
         if(!detailed) { 
            if(temp == TStatusCode.SRM_SUCCESS) {
              return "Yes";
            }
         }
         else {
            if(abortTestCondition == 0) {
              if(temp == TStatusCode.SRM_SUCCESS || temp == TStatusCode.SRM_ABORTED) {
                return "Yes";
              }
            }
         }
         return temp.getValue();
       }
       else {
         explanationBuffer.append("status="+temp.getValue()+" expalantion=");
         statusBuffer.append(temp.getValue());
         if(explanation != null) {
           explanationBuffer.append(explanation);
         }
         if(abortTestCondition == 1) { 
           if(temp == TStatusCode.SRM_INVALID_REQUEST ||
              temp == TStatusCode.SRM_FAILURE) {
              return "Yes";
           }
           return "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE";
         }
         else if(abortTestCondition == 2 || abortTestCondition == 3) { 
           if(temp == TStatusCode.SRM_INVALID_PATH ||
              temp == TStatusCode.SRM_FAILURE) {
              return "Yes";
           }
           return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
         }
         if(!detailed) { 
            if(temp == TStatusCode.SRM_SUCCESS || temp == TStatusCode.SRM_ABORTED) {
              return "Yes";
            }
         }
         else {
            if(abortTestCondition == 0) {
              if(temp == TStatusCode.SRM_SUCCESS || temp == TStatusCode.SRM_ABORTED) {
                return "Yes";
              }
            }
         }
         return temp.getValue();
       }
     }
     else {
       util.printMessage("Null result",logger,pIntf);
       explanationBuffer.append("Null result from SRM");
       statusBuffer.append("Null result from SRM");
       if(abortTestCondition == 1 || abortTestCondition == 2 
			|| abortTestCondition == 3) {
         return "No. got null result";
       }
       return "Null result";
     }
  }catch(Exception e) {
     util.showException(e,logger);
     //System.out.println("Before abortFiles");
     statusBuffer.append("SRM_FAILURE");
     return "SRM_FAILURE";
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doPutDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doPutDone(ISRM srm, String siteUrl,String requestToken,
  String uid, StringBuffer statusBuffer, 
  StringBuffer explanationBuffer, boolean detailedTest, int putDoneCondition) 
	throws Exception {

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);
  SrmPutDoneRequest req = new SrmPutDoneRequest();
  req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
  req.setRequestToken(requestToken);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
  }

  if(!detailedTest) {
    if(!threepartycopy) {
     if(putDoneCondition == 0) {
      req = new SrmPutDoneRequest();
      req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
      req.setRequestToken(requestToken+"_bad");
      if(!uid.equals("")) {
        req.setAuthorizationID(uid);
      }   

      util.printMessage("\nSRM-TESTER: " +
		new Date() + 
			" Calling PutDone Test for bad request token", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken+"_bad",logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_REQUEST",logger,pIntf);
    }
    else if(putDoneCondition == 3) {
      uri = new org.apache.axis.types.URI (siteUrl+"_bad_surl");
      req = new SrmPutDoneRequest();
      req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
      req.setRequestToken(requestToken);
      if(!uid.equals("")) {
        req.setAuthorizationID(uid);
      }

      util.printMessage("\nSRM-TESTER: " +
		new Date() + " Calling PutDone Test for bad SURL", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl+"_bad_surl",logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
    }
   }
   if(putDoneCondition == 1) {
      util.printMessageHL("\nSRM-TESTER: " + new Date() +
			" Calling PutDone ...\n", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

      util.printMessage("=========================",logger,pIntf); 
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      if(!uid.equals("")) {
        util.printMessage("AuthorizationID="+uid,logger,pIntf);
      }
      util.printMessage("=========================",logger,pIntf); 
    }
  }
  else {
     if(putDoneCondition == 0) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling PutDone Test ...", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_SUCCESS",logger,pIntf);
     }
     else if(putDoneCondition == 2) {
      util.printMessage("\nSRM-TESTER: " +
		new Date () + 
		" Calling PutDone Test for already putDone SURL", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_FAILURE",logger,pIntf);
     }
     else if(putDoneCondition == 3) {
      uri = new org.apache.axis.types.URI (siteUrl+"_bad_surl");
      util.printMessage("\nSRM-TESTER: " + new Date() + 
			" Calling PutDone Test for bad SURL", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("Bad SURL="+siteUrl+"_bad_surl",logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_INVALID_PATH",logger,pIntf);

      req = new SrmPutDoneRequest();
      req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));
      req.setRequestToken(requestToken);
      if(!uid.equals("")) {
        req.setAuthorizationID(uid);
      }
     }
     else if(putDoneCondition == 4) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
		  " Calling PutDone Test for already expired SURL", logger,pIntf);
      util.printMessage("\nSRM-TESTER: ...Input parameters ...",logger,pIntf);
      util.printMessage("=============================\n",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("AuthorizationID="+uid,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_FAILURE",logger,pIntf);
     }
  }

  String explanation="";
  try {
     SrmPutDoneResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
               timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmPutDone(req);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     util.printMessage("\nSRM-TESTER: ...Output from SRM..." + new Date(),logger,pIntf);
     if(result != null) {
       if(result.getArrayOfFileStatuses() != null) {
         TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode temp = status.getStatus().getStatusCode();
         util.printMessage("\n===========================",logger,pIntf);
         util.printMessage("\tSURL="+status.getSurl().toString(),logger,pIntf);
         util.printMessage("\tstatus="+temp,logger,pIntf);
         util.printMessage("\texplanation="+status.getStatus().getExplanation(),logger,pIntf);
         explanation=status.getStatus().getExplanation();
         if(explanation != null) {
           explanationBuffer.append(explanation);
         }
         util.printMessage("===========================\n",logger,pIntf);
         //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);
         statusBuffer.append(temp.getValue());
         if(status.getStatus().getExplanation() != null) {
           explanationBuffer.append(status.getStatus().getExplanation());
         }
         if(!detailedTest && putDoneCondition == 0) { 
           if(temp == TStatusCode.SRM_INVALID_REQUEST ||
              temp == TStatusCode.SRM_FAILURE) {
             return "Yes";
           }
           return "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE";
         }
         else if(putDoneCondition == 2 || putDoneCondition == 4) {
           if(temp == TStatusCode.SRM_FAILURE) {
             return "Yes";
           }
           return "No. expecting SRM_FAILURE";
         }
         else if(putDoneCondition == 3) {
           if(temp == TStatusCode.SRM_INVALID_PATH ||
              temp == TStatusCode.SRM_FAILURE) {
             return "Yes";
           }
           return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
         }
         else if(putDoneCondition == 1) {
           if(temp == TStatusCode.SRM_DONE || temp == TStatusCode.SRM_SUCCESS) {
             return "Yes";
           }
           return "No. expecting SRM_SUCCESS";
         }
         if(detailedTest && putDoneCondition == 0) { 
           if(temp == TStatusCode.SRM_DONE || temp == TStatusCode.SRM_SUCCESS) {
             return "Yes";
           }
           return "No. expecting SRM_SUCCESS";
         }
         return temp.getValue();
      }
      else {
        util.printMessage("Null arrayof file status",logger,pIntf);
        TStatusCode temp = result.getReturnStatus().getStatusCode();
        util.printMessage("result status code " + temp,logger,pIntf);
        statusBuffer.append(temp.getValue());
        explanation = result.getReturnStatus().getExplanation();
        if(explanation != null) {
          explanationBuffer.append(explanation);
        }
        
        if(!detailedTest && putDoneCondition == 0) { 
          if(temp == TStatusCode.SRM_INVALID_REQUEST ||
             temp == TStatusCode.SRM_FAILURE) {
            return "Yes";
          }
          return "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE";
        }
        else if(putDoneCondition ==2 || putDoneCondition == 4) {
          if(temp == TStatusCode.SRM_FAILURE) {
            return "Yes";
          }
          return "No. expecting SRM_FAILURE";
        }
        else if(putDoneCondition == 3) {
          if(temp == TStatusCode.SRM_INVALID_PATH ||
             temp == TStatusCode.SRM_FAILURE) {
            return "Yes";
          }
          return "No. expecting SRM_INVALID_PATH || SRM_FAILURE";
        }
        else if(detailedTest && putDoneCondition == 0) { 
           if(temp == TStatusCode.SRM_DONE || temp == TStatusCode.SRM_SUCCESS) {
             return "Yes";
           }
           return "No. expecting SRM_SUCCESS";
        }
        return "Null arrayof file status";
      }
    }
    else {
     util.printMessage("Null result",logger,pIntf);
     explanationBuffer.append("Null result");
     if(!detailedTest && putDoneCondition == 0) { 
       return "No. got null result";
     }
     else if(putDoneCondition == 2 || putDoneCondition == 3 || putDoneCondition ==4) {
       return "No. got null result";
     }
     return "Null result";
    }
  }catch(Exception e) {
     explanationBuffer.append(e.getMessage());
     //System.out.println("Before putDOne");
     util.showException(e,logger);
     return "SRM_FAILURE";
  }
}

private void doReleaseSpace(ISRM srm, String sToken) throws Exception {

  if(!alreadyReleasedSpace && reserveSpace && !sToken.equals("")) {

   alreadyReleasedSpace = true;
   if(sToken == null || sToken.equals("")) {
     util.printMessage("Cannot release space for null token", logger,pIntf);
     return;
   }

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

   try {
    util.printMessage("\nSRM-TESTER: Doing ReleaseSpace now ...", logger,pIntf);
    SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
    if(!uid.equals("")) {
      req.setAuthorizationID(uid);
    }
    req.setSpaceToken(sToken);

    SrmReleaseSpaceResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
               timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
       response = srm.srmReleaseSpace(req);
       timeOutCallBack.setObject(response);
    }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
    }

    ReportRequestStatus rrs = util.createReportRequestStatus("","","");
    rrs.setSpaceToken(sToken);
    rrs.setLinkName("releasespace_"+currTestCondition);

    if(response != null) {
     if(response.getReturnStatus() != null) {
       util.printMessage("StatusCode " + 
			response.getReturnStatus().getStatusCode(),logger,pIntf);
       rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
        }
      }
      else {
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      }

      util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "", timeStamp,fullTimeStamp , startTimeStamp,operatorTimeStamp, 
	   servicePath,noPublish,
	   siteName, numOperations,typeString, "","Sending", 
	   dailyTest,numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Sending", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, 
	  servicePath,noPublish,
	  siteName, numOperations,typeString, "srmReleaseSpace",
			"Sending", dailyTest,numSites,localPublish,bs,bsa);
     }
     else {
       rrs.setActualResult("Null response from server");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "Null response from the server ", timeStamp, fullTimeStamp, 
	   startTimeStamp, operatorTimeStamp, servicePath,noPublish,
       siteName, numOperations,typeString, "","Failed", dailyTest,
	   numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Failed", rrs,
      "Null response from the server", timeStamp, fullTimeStamp, 
	  startTimeStamp,operatorTimeStamp, servicePath,noPublish,
	  siteName, numOperations,typeString, "srmReleaseSpace",
 	  "Failed", dailyTest,numSites,localPublish,bs,bsa);
     }
   }catch(Exception e) {
      e.printStackTrace();
      throw e;
    }
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
    throw new Exception("SURL not in correct format " + str);
  }
}

public void processThreadRequest_3(String[] cmd, SharedObjectLock sLock) {
}

public void processThreadRequest_1(String[] cmd) {
}

public void processThreadRequest_2(String type, Hashtable cmd) {
}

public void processGridFTPWaitTimeOut(Hashtable ht) {

  String tsource = (String) ht.get("tsource");
  String turl = (String) ht.get("turl");
  String source = (String) ht.get("source");
  Long lsize = (Long) ht.get("lsize");
  long ll = lsize.longValue();
  StringBuffer gsiFTPSTimeStamp = (StringBuffer)ht.get("starttimestamp");
  StringBuffer gsiFTPETimeStamp = (StringBuffer)ht.get("endtimestamp");
  String operatorTimeStamp = (String)ht.get("optimestamp");
  OperOk canPutContinue = (OperOk)ht.get("canPutContinue");
  Object obj = ht.get("srm");
  ISRM srm = null;
  if(obj != null) {
    srm = (ISRM) obj;
  }
  StringBuffer expBuffer = (StringBuffer)ht.get("expbuffer");
  Boolean bb = (Boolean)ht.get("direct");
  boolean direct = bb.booleanValue();
  obj = ht.get("response");
  SrmPrepareToPutResponse response = null;
  if(obj != null) {
     response = (SrmPrepareToPutResponse)obj;
  }
  URI keyURL=null;
  obj = ht.get("keyurl");
  if(obj!= null) {
     keyURL = (URI) obj;
  }

  try {
   
    gsiFTPSTimeStamp = util.getTimeStamp();

             OperOk gsiftpOk = null;
             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               gsiftpOk = (OperOk) map.get("gsiftp-put");
               gsiftpOk.setTried(true);
             }

    gsiFTPETimeStamp = util.doOnlyGsiFTP(tsource,turl, 
			gsiFTPSTimeStamp,
			publishUrlStr, displayType, timeStamp, fullTimeStamp, 
			startTimeStamp,
		    operatorTimeStamp, servicePath,
	        credential, proxyFile, noPublish, onlyGsiFTPGet, defaultProxy, 
			dcau, bufferSize,parallelism,
            siteName,numOperations,typeString,dailyTest, numSites, 
			localPublish,bs,bsa,logger,pIntf,useGUC,gucScriptPath,tempScriptPath,
			gsiftpOk);

             if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("gsiftp-put");
               if(pOk.getExplanation().equals("")) {
                  pOk.isOperOk(true);
               }
             }


   if(!plainPut && !direct) {
         MySRMFileTransfer tu = new MySRMFileTransfer(tsource,turl);
         tu.setCredentials(credential);
         ll = tu.getSourceFileSize();
         util.printMessage("\nSize " + lsize,logger,pIntf);
   }
      
   try { 
       ReportRequestStatus rrs = 
			util.createAndConvertRequest(tsource,turl,0,-1);
       rrs.setActualResult("SUCCESS");
       rrs.setLinkName("gridftp_"+currTestCondition);
       util.webScript(publishUrlStr,
           displayType,"GridFTP","SUCCESS", rrs, "",
           timeStamp, fullTimeStamp, startTimeStamp,
		   operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"GridFTP-Put","Ok",dailyTest,
		   numSites,localPublish,bs,bsa);

        rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,ll);
        rrs.setLinkName("gridftp_"+currTestCondition);
        util.webScript(publishUrlStr,displayType,"GsiFTP","SUCCESS",
              rrs, "", timeStamp,fullTimeStamp, 
			  startTimeStamp,operatorTimeStamp,
			  servicePath,noPublish,
		      siteName,numOperations,typeString,"GridFTP-Put","Ok",
			  dailyTest,numSites,localPublish,bs,bsa);
   }catch(Exception e) { e.printStackTrace(); }
   if(!direct) {
     StringBuffer explanationBuffer = new StringBuffer();
     StringBuffer statusBuffer = new StringBuffer();

     if(currTestCondition == 8 && keyURL != null) {
        //don't do putDone, wait and do the statusCheck
        //after that, do putDone for the test already pin expired surl.
        util.printMessage("\nWaiting here to do the advanced test " +
			"for srmStatusOfPutRequest" + 
			"\nfor condition wait for 120 seconds for the pin life time ", 
			logger,pIntf);
// AS 070809
// changed from 120000 to 300000
        Thread.sleep(statusWaitTime);
        checkPutStatusTest(srm,keyURL,response,4);

        String statusMsg = doPutDone(srm, surl[0],
	  	  response.getRequestToken(),uid,statusBuffer, explanationBuffer,true,4); 
        String displayStatusMsg = "Ok"; 
        if((statusBuffer.toString().equals("SRM_SUCCESS")) ||
           (statusBuffer.toString().equals("SRM_DONE")) ||
           (statusBuffer.toString().equals("SRM_NOT_SUPPORTED"))) {
          if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
            displayStatusMsg = "N.S.";
          }
        }
        else {
          displayStatusMsg = "Failed";
        }
        ReportRequestStatus rrs = 
			util.createReportRequestStatus(surl[0],"","");
        rrs.setRequestId(response.getRequestToken());
        rrs.setActualResult(statusBuffer.toString());
        rrs.setUserId(uid);
        rrs.setMatchingInfo(statusMsg);
        rrs.setLinkName("putdoneexpiredsurl_"+currTestCondition);
        try {
          util.webScript(publishUrlStr,
           displayType,"SendingPUTDone",
			"PutDone Test for already expired SURL", rrs,
            "", timeStamp, fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmPutDone", 
			  displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,displayType,"PutDone",statusBuffer.toString(),
            rrs, explanationBuffer.toString(), 
	        timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmPutDone", 
			  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
           if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putdone");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg);
               }
           }
        }catch(Exception e) { 
           if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putdone");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg);
               }
           }
		   e.printStackTrace(); 
		}
     }
     else {
        if(!direct && !detailed) {
         String statusMsg = "";

         explanationBuffer = new StringBuffer();
         statusBuffer = new StringBuffer();
         statusMsg = doPutDone(srm, surl[0],
		  response.getRequestToken(),uid,statusBuffer, 
				explanationBuffer,false,1); 
         ReportRequestStatus rrs = 
			util.createReportRequestStatus(surl[0],"","");
         rrs.setRequestId(response.getRequestToken());
         rrs.setActualResult(statusBuffer.toString());
         rrs.setUserId(uid);
         rrs.setMatchingInfo(statusMsg);
         rrs.setLinkName("putdonebasic_"+currTestCondition);
         Date d = new Date ();
         operatorTimeStamp = d.toString();
         String displayStatusMsg = "Ok";
         if((statusBuffer.toString().equals("SRM_SUCCESS")) ||
            (statusBuffer.toString().equals("SRM_DONE")) ||
            (statusBuffer.toString().equals("SRM_NOT_SUPPORTED"))) {
            if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
              displayStatusMsg = "N.S.";
            }
         }
         else {
           displayStatusMsg = "Failed";
         }
         try {
          util.webScript(publishUrlStr,
            displayType,"SendingPUTDone","PutDone", rrs,
            "", timeStamp, fullTimeStamp, startTimeStamp,
			operatorTimeStamp, servicePath,noPublish,
		    siteName,numOperations,typeString,"srmPutDone",
			displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,displayType,"PutDone",statusBuffer.toString(),
            rrs, explanationBuffer.toString(), 
			timeStamp,fullTimeStamp, startTimeStamp,
		    operatorTimeStamp, servicePath,noPublish,
		    siteName,numOperations,typeString,"srmPutDone", displayStatusMsg,
			dailyTest,numSites,localPublish,bs,bsa);
            if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putdone");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg);
               }
            }
         }catch(Exception e) { 
            if(canPutContinue.getSubOperations().size() > 0 ) {
               HashMap map = canPutContinue.getSubOperations();
               OperOk pOk = (OperOk) map.get("putdone");
               pOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 pOk.isOperOk(true);
               }
               else {
                 pOk.isOperOk(false);
                 pOk.setExplanation(displayStatusMsg);
               }
            }
			e.printStackTrace(); 
		 }
        }
        else {
           explanationBuffer = new StringBuffer();
           statusBuffer = new StringBuffer();
           String statusMsg = doPutDone(srm, surl[0],
		  	  response.getRequestToken(),uid,statusBuffer, 
			  explanationBuffer,true,0); 
           String displayStatusMsg = "Ok";
           if((statusBuffer.toString().equals("SRM_SUCCESS")) ||
              (statusBuffer.toString().equals("SRM_DONE")) ||
              (statusBuffer.toString().equals("SRM_NOT_SUPPORTED"))) {
              if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                displayStatusMsg = "N.S.";
              }
           }
           else {
             displayStatusMsg = "Failed";
           }
           ReportRequestStatus rrs = 
				util.createReportRequestStatus(surl[0],"","");
           rrs.setRequestId(response.getRequestToken());
           rrs.setActualResult(statusBuffer.toString());
           rrs.setMatchingInfo(statusMsg);
           rrs.setUserId(uid);
           rrs.setLinkName("putdonebasic_"+currTestCondition);
           try {
             util.webScript(publishUrlStr,
              displayType,"SendingPUTDone","PutDone", rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,
		      operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"",displayStatusMsg,
			  dailyTest,numSites, localPublish,bs,bsa);
             util.webScript(publishUrlStr,displayType,"PutDone",statusBuffer.toString(),
              rrs, explanationBuffer.toString(), 
		 	  timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmPutDone",
			  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
              if(canPutContinue.getSubOperations().size() > 0 ) {
                HashMap map = canPutContinue.getSubOperations();
                OperOk pOk = (OperOk) map.get("putdone");
                pOk.setTried(true);
                if(displayStatusMsg.equals("Ok")) {
                  pOk.isOperOk(true);
                }
                else {
                  pOk.isOperOk(false);
                  pOk.setExplanation(displayStatusMsg);
                }
              }
           }catch(Exception e) { 
             if(canPutContinue.getSubOperations().size() > 0 ) {
                HashMap map = canPutContinue.getSubOperations();
                OperOk pOk = (OperOk) map.get("putdone");
                pOk.setTried(true);
                if(displayStatusMsg.equals("Ok")) {
                  pOk.isOperOk(true);
                }
                else {
                  pOk.isOperOk(false);
                  pOk.setExplanation(displayStatusMsg);
                }
             }
			 e.printStackTrace(); 
		   }

           if(currTestCondition == 1) {
             explanationBuffer = new StringBuffer();
             statusBuffer = new StringBuffer();
             statusMsg = doPutDone(srm, surl[0],
			  response.getRequestToken(),uid,statusBuffer,
				explanationBuffer,true,2); 
             displayStatusMsg = "Ok";
             if((statusBuffer.toString().equals("SRM_SUCCESS")) ||
                (statusBuffer.toString().equals("SRM_DONE")) ||
                (statusBuffer.toString().equals("SRM_NOT_SUPPORTED"))) {
                if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                  displayStatusMsg = "N.S.";
                }
             }
             else {
               displayStatusMsg = "Failed";
             }
             rrs = util.createReportRequestStatus(surl[0],"","");
             rrs.setRequestId(response.getRequestToken());
             rrs.setActualResult(statusBuffer.toString());
             rrs.setMatchingInfo(statusMsg);
             rrs.setUserId(uid);
             rrs.setLinkName("putdonealreadysurl_"+currTestCondition);
             util.printMessage("\nCalled PutDOne already SURL",logger,pIntf);  
             try {
              util.webScript(publishUrlStr,
               displayType,"SendingPUTDone","PutDone for already PutDone SURL", rrs,
               "", timeStamp, fullTimeStamp, startTimeStamp,
			   operatorTimeStamp, servicePath,noPublish,
		       siteName,numOperations,typeString,"",displayStatusMsg,dailyTest,
			   numSites,localPublish,bs,bsa);
              util.webScript(publishUrlStr,displayType,"PutDone",statusBuffer.toString(),
                rrs, explanationBuffer.toString(), 
		  	    timeStamp,fullTimeStamp, startTimeStamp,
			    operatorTimeStamp, servicePath,noPublish,
		        siteName,numOperations,typeString,"srmPutDone",
			    displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
               if(canPutContinue.getSubOperations().size() > 0 ) {
                  HashMap map = canPutContinue.getSubOperations();
                  OperOk pOk = (OperOk) map.get("putdone");
                  pOk.setTried(true);
                  if(displayStatusMsg.equals("Ok")) {
                    pOk.isOperOk(true);
                  }
                  else {
                    pOk.isOperOk(false);
                    pOk.setExplanation(displayStatusMsg);
                  }
                }
             }catch(Exception e) { 
               if(canPutContinue.getSubOperations().size() > 0 ) {
                  HashMap map = canPutContinue.getSubOperations();
                  OperOk pOk = (OperOk) map.get("putdone");
                  pOk.setTried(true);
                  if(displayStatusMsg.equals("Ok")) {
                    pOk.isOperOk(true);
                  }
                  else {
                    pOk.isOperOk(false);
                    pOk.setExplanation(displayStatusMsg);
                  }
                }
				e.printStackTrace(); 
			 }

           }
        }
      }
      if(currTestCondition == 9 && keyURL != null) {
        util.printMessage("\nWaiting here to do the advanced test " +
         "for srmStatusOfPutRequest" +
         "\nfor condition wait for 120 seconds for the file life time ",
         logger,pIntf);
// AS 070809
// changed from 120000 to 300000
        Thread.sleep(statusWaitTime);
        checkPutStatusTest(srm,keyURL,response,5);
       }
      
       /*
       if(doStatusEnquiry) {
          doStatusSpace(srm,spaceToken);
       }
       */

      if(browse) {
         try {
           SRMDirTest dirTest = new SRMDirTest
				(publishUrlStr, displayType,
                 testType,timeStamp, fullTimeStamp, 
				 servicePath, credential, proxyFile,
                 noPublish, detailed, defaultProxy,
		         siteName, numOperations,"srmPrepareToPut", startTimeStamp,
			     dailyTest,numSites, 
			     retryTimeAllowed, statusWaitTime,localPublish,bs,bsa,logger,pIntf,
				 useDriverOn,pingOverAllSuccess);
        
           OperOk lsOk = null;
           if(canPutContinue.getSubOperations().size() > 0) {
             HashMap map = canPutContinue.getSubOperations();
             lsOk = (OperOk) map.get("srmls");
             if(lsOk != null) {
               lsOk.setTried(true);
             }
           } 
           dirTest.doSrmLsTest
				(srm,proxyString,uid, surl[0], null,lsOk,testCondition);
         }catch(Exception e) {
          try {
           ReportRequestStatus rrs = new ReportRequestStatus();
           rrs.setLinkName("browse_"+testCondition);
           rrs.setActualResult("Exception");
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
           util.webScript(publishUrlStr,displayType,
             "Browse","Exception", rrs,e.toString(),
             timeStamp,fullTimeStamp, startTimeStamp,
			 operatorTimeStamp, servicePath,noPublish,
		         siteName,numOperations,typeString,"srmLs",
		        "Exception",dailyTest,numSites,localPublish,bs,bsa);
           util.webScript(publishUrlStr,
             displayType,"BLANK","",
             null, "", timeStamp, fullTimeStamp, startTimeStamp,
		     operatorTimeStamp, servicePath,noPublish,
		         siteName,numOperations,typeString,"srmLs",
				"Exception",dailyTest,numSites,localPublish,bs,bsa);
           util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
           if(canPutContinue.getSubOperations().size() > 0) {
             HashMap map = canPutContinue.getSubOperations();
             OperOk lsOk = (OperOk) map.get("srmls");
             if(lsOk != null) {
               lsOk.setTried(true);
               lsOk.setExplanation("Exception");
             }
           }
          }catch(Exception ee) {ee.printStackTrace();}
          e.printStackTrace();
          if(sLock != null) {
            sLock.setIncrementCount();
          }
         }
       }

       if(advisoryDelete) {
         statusBuffer = new StringBuffer();
          try {
            if(!detailed && !threepartycopy) {
              explanationBuffer = new StringBuffer();
              statusBuffer = new StringBuffer();
              String statusMsg = doReleaseFile(srm, false,surl[0],
			    response.getRequestToken(),statusBuffer,
				explanationBuffer,false,0); 
              String displayStatusMsg = "Ok";
              ReportRequestStatus rrs = 
				util.createReportRequestStatus(surl[0],"","");
              rrs.setUserId(uid);
              rrs.setMatchingInfo(statusMsg);
              rrs.setActualResult(statusBuffer.toString());
              rrs.setLinkName("srmrmbasic_"+currTestCondition);
              if(statusBuffer.toString().equals("SRM_DONE") || 
				 statusBuffer.toString().equals("SRM_SUCCESS") ||
                 statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                 if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                   displayStatusMsg = "N.S.";
                 }
              }
              else {
                 displayStatusMsg = "Failed";
              }
              
              HashMap map = canPutContinue.getSubOperations();
              rmOk = (OperOk) map.get("srmrm");
              if(rmOk != null) {
                rmOk.setTried(true);              
                if(displayStatusMsg.equals("Ok")) { 
                  rmOk.isOperOk(true);              
                }
                else {
                  rmOk.setExplanation(displayStatusMsg);
                }
              }
          
              try {
                util.webScript(publishUrlStr,
                 displayType,"SendingPUTRelease","SrmRm", rrs,
                 "", timeStamp, fullTimeStamp, startTimeStamp,
				 operatorTimeStamp, servicePath,noPublish,
		         siteName,numOperations,typeString,"srmRm",
			     "Sending",dailyTest,numSites,localPublish,bs,bsa);
                util.webScript(publishUrlStr,displayType,"PutRelease",statusMsg,
                  rrs, explanationBuffer.toString(), 
			      timeStamp,fullTimeStamp, startTimeStamp,
				  operatorTimeStamp, servicePath,noPublish,
		         siteName,numOperations,typeString,"srmRm",
				  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
              }catch(Exception e)  {e.printStackTrace();}
             }
             else if (detailed) {
               explanationBuffer = new StringBuffer();
               statusBuffer = new StringBuffer();
               String statusMsg = doReleaseFile(srm, false,surl[0],
                   response.getRequestToken(),statusBuffer,
				   explanationBuffer,true,0);
               String displayStatusMsg = "Ok";
               if(statusBuffer.toString().equals("SRM_DONE") || 
				  statusBuffer.toString().equals("SRM_NOT_SUPPORTED")
                    || statusBuffer.toString().equals("SRM_SUCCESS")) {
                  if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                    displayStatusMsg = "N.S.";
                  }
               }
               else {
                   displayStatusMsg = "Failed";
               }
               ReportRequestStatus rrs = 
					util.createReportRequestStatus(surl[0],"","");
               rrs.setUserId(uid);
               rrs.setMatchingInfo(statusMsg);
               rrs.setActualResult(statusBuffer.toString());
               rrs.setLinkName("srmrmbasic_"+currTestCondition);
               if(rmOk != null) {
                rmOk.setTried(true);              
                if(displayStatusMsg.equals("Ok")) { 
                  rmOk.isOperOk(true);              
                }
                else {
                  rmOk.setExplanation(displayStatusMsg);
                }
               }
               try {
                 util.webScript(publishUrlStr,
                  displayType,"SendingPUTRelease","SrmRm", rrs,
                  "", timeStamp, fullTimeStamp, startTimeStamp,
				  operatorTimeStamp, servicePath,noPublish,
		          siteName,numOperations,typeString,"srmRm",
				  "Sending",dailyTest,numSites,localPublish,bs,bsa);
                 util.webScript(publishUrlStr,displayType,"PutRelease",
				   statusMsg, rrs, explanationBuffer.toString(),
                   timeStamp,fullTimeStamp, startTimeStamp,
				   operatorTimeStamp, servicePath,noPublish,
		           siteName,numOperations,typeString,"srmRm",
			       displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
               }catch(Exception e)  {e.printStackTrace();}

               if(currTestCondition == 1) {
                 explanationBuffer = new StringBuffer();
                 statusBuffer = new StringBuffer();
                 statusMsg = doReleaseFile(srm, false,surl[0],
                   response.getRequestToken(),statusBuffer,explanationBuffer,true,1);
                 displayStatusMsg = "Ok";
                 if(statusBuffer.toString().equals("SRM_DONE") || 
					statusBuffer.toString().equals("SRM_NOT_SUPPORTED") ||
					statusBuffer.toString().equals("SRM_SUCCESS")) {
                    if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                      displayStatusMsg = "N.S.";
                    }
                 }
                 else {
                   displayStatusMsg = "Failed";
                 }
                 rrs = 
					util.createReportRequestStatus(surl[0],"","");
                 rrs.setMatchingInfo(statusMsg);
                 rrs.setActualResult(statusBuffer.toString());
                 rrs.setUserId(uid);

                 if(rmOk != null) {
                   rmOk.setTried(true);              
                   if(displayStatusMsg.equals("Ok")) { 
                     rmOk.isOperOk(true);              
                   }
                   else {
                     rmOk.setExplanation(displayStatusMsg);
                   }
                 }

                 try {
                   util.webScript(publishUrlStr,
                     displayType,"SendingPUTRelease",
				      "SrmRm for already released SURL", rrs,
                      "", timeStamp, fullTimeStamp, startTimeStamp,
				      operatorTimeStamp, servicePath,noPublish, siteName,
					  numOperations, typeString,"srmRm","Sending",dailyTest,
					  numSites,localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,"PutRelease",
					 statusMsg,
                     rrs, explanationBuffer.toString(),
                     timeStamp,fullTimeStamp, startTimeStamp,
					 operatorTimeStamp, servicePath,noPublish,
		             siteName,numOperations,typeString,"srmRm",
					 displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
                 }catch(Exception e)  {e.printStackTrace();}

                 explanationBuffer = new StringBuffer();
                 statusBuffer = new StringBuffer();
                 statusMsg = doReleaseFile(srm, false,surl[0],
                    response.getRequestToken(),statusBuffer,
					explanationBuffer,true,2);
                 displayStatusMsg = "Ok";
                 if(statusBuffer.toString().equals("SRM_DONE") || 
					statusBuffer.toString().equals("SRM_NOT_SUPPORTED") ||
					statusBuffer.toString().equals("SRM_SUCCESS")) {
                   if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
                     displayStatusMsg = "N.S.";
                   }
                 }
                 else {
                   displayStatusMsg = "Failed";
                 }

                 if(rmOk != null) {
                   rmOk.setTried(true);              
                   if(displayStatusMsg.equals("Ok")) { 
                     rmOk.isOperOk(true);              
                   }
                   else {
                     rmOk.setExplanation(displayStatusMsg);
                   }
                 }

                 rrs = util.createReportRequestStatus
					(surl[0]+"_bad_surl","","");
                 rrs.setMatchingInfo(statusMsg);
                 rrs.setActualResult(statusBuffer.toString());
                 rrs.setUserId(uid);
                 rrs.setLinkName("srmrmbadsurl_"+currTestCondition);
                 try {
                   util.webScript(publishUrlStr,
                     displayType,"SendingPUTRelease","SrmRm for Bad SURL", 
					 rrs, "", timeStamp, fullTimeStamp, startTimeStamp,
				     operatorTimeStamp, servicePath,
					 noPublish, siteName,numOperations,typeString,"srmRm",
					 "Sending",dailyTest,numSites,localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,"PutRelease",
					 statusMsg, rrs, explanationBuffer.toString(),
                     timeStamp,fullTimeStamp, startTimeStamp,
					 operatorTimeStamp, servicePath,noPublish,
		             siteName,numOperations,typeString,"srmRm",
					displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
                 }catch(Exception e)  {e.printStackTrace();}
             }
           }
         } catch(Exception ge) {
           util.printMessage
	        (" SRM-TESTER: Releasefile failed " + 
		      explanationBuffer.toString() + " " + ge.getMessage(), 
				logger,pIntf);
           ReportRequestStatus rrs = new ReportRequestStatus();
           rrs.setMatchingInfo("No."+ge.getMessage());
           rrs.setActualResult("");
           if(rmOk != null) {
             rmOk.setTried(false);              
           }
           try {
            util.webScript(publishUrlStr,
              displayType,"SendingPUTRelease","SrmRm", rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmRm",
			  "Sending",dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
              displayType,"PutRelease","FAILED",rrs,
              explanationBuffer.toString() + ge.getMessage(),
              timeStamp, fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,
		  	  "srmRm","Failed",dailyTest,numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace();}
          }
        }//end advisorydelete

       try {
         util.webScript(publishUrlStr, displayType,"Sending3END",
	      "", null, "", timeStamp, fullTimeStamp, startTimeStamp,
          operatorTimeStamp,
		  servicePath,noPublish, siteName,numOperations,typeString,"","Sending",
	  	  dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }

       /*
       if(doStatusEnquiry) {
          doStatusSpace(srm,spaceToken);
       }
       */

       if(plainPut) {
         try{ 
          util.webScript(publishUrlStr,displayType,"RequestSummary",
             "SUCCESS",convertPut
				(response,tsource,"","",surl[0],""+lsize,""), 
				"", timeStamp,fullTimeStamp, startTimeStamp,
		        operatorTimeStamp, servicePath,noPublish,
		        siteName,numOperations,typeString,
			    "srmPrepareToPut-OverAll","Ok",dailyTest,
			     numSites,localPublish,bs,bsa);
            canPutContinue.isOperOk(true);
         }catch(Exception e) { e.printStackTrace(); }
       }
       else {
         try{ 
          util.webScript(publishUrlStr,displayType,"RequestSummary",
             "SUCCESS",convertPut(response,copySourceFile,tsource,"",
					surl[0],"-1",""), "",
              timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,
			  "srmPrepareToPut-OverAll","Ok",dailyTest,
			  numSites,localPublish,bs,bsa);
            canPutContinue.isOperOk(true);
         }catch(Exception e) { e.printStackTrace(); }
       }
       doReleaseSpace(srm,spaceToken);
       if(sLock != null)  {
         sLock.setIncrementCount();
       }
    }
    else {
       if(sLock != null)  {
         sLock.setIncrementCount();
       }
    }
  }catch(Exception ee) {
     ee.printStackTrace();
     util.printMessage("\n.............................",logger,pIntf);
     util.printMessage("GsiFTP Failed with exception "+
		ee.getMessage(),logger,pIntf);
     util.printMessage(".............................\n",logger,pIntf);
     canPutContinue.isOperOk(false);
     try {
       ReportRequestStatus rrs = 
			util.createAndConvertRequest(tsource,turl,0,-1);
       rrs.setActualResult("FAILED");
       rrs.setLinkName("gridftp_"+currTestCondition);
       util.webScript(publishUrlStr,
           displayType,"GridFTP","Failed", rrs, "Notes from tester: " + ee.getMessage(),
           timeStamp, fullTimeStamp, startTimeStamp,
			operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,typeString,"GridFTP-Put",
		   "Failed",dailyTest,numSites,localPublish,bs,bsa);
       rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,ll);
       rrs.setLinkName("gridftp_"+currTestCondition);
       util.webScript(publishUrlStr,
            displayType,"GsiFTP","GSIFTP FAILED", rrs,
            "SourceURL did not get copied to TargetURL ",
            timeStamp, fullTimeStamp, startTimeStamp,
			operatorTimeStamp, servicePath,noPublish,
		    siteName,numOperations,typeString,"GridFTP-Put","Failed",
			dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }

      StringBuffer explanationBuffer = new StringBuffer();
      StringBuffer statusBuffer = new StringBuffer();

      if(!direct && !detailed) {
         if(abortOnFail) {
         try {
          String statusMsg = doAbortFiles(srm, surl[0],
			response.getRequestToken(),explanationBuffer,
			statusBuffer,uid,false,0); 

          String displayStatusMsg = "Ok"; 

          if(statusBuffer.toString().equals("SRM_SUCCESS") || 
				statusBuffer.toString().equals("SRM_NOT_SUPPORTED") ||
				statusBuffer.toString().equals("SRM_DONE")) {
             if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
               displayStatusMsg = "N.S.";
             }
          }
          else {  
            displayStatusMsg = "Failed";
          }
          try {
            ReportRequestStatus rrs = 
				util.createReportRequestStatus(surl[0],"","");
            rrs.setRequestId(response.getRequestToken());
            rrs.setActualResult(statusBuffer.toString());
            rrs.setMatchingInfo(statusMsg);
            rrs.setLinkName("abortfilesbasic_"+currTestCondition);

            util.webScript(publishUrlStr,
             displayType,"SendingAbortFiles","AbortFiles", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp,
			 operatorTimeStamp, servicePath,noPublish,
		     siteName,numOperations,typeString,"srmAbortFiles","Sending",
		     dailyTest,numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg,
              rrs, explanationBuffer.toString(), 
		  	  timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles", 
			  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) {e.printStackTrace();}
         }catch(Exception ae) {ae.printStackTrace();}
        }
       }
       else {
         if(abortOnFail) {
         try {
          String statusMsg = doAbortFiles(srm, surl[0],
			response.getRequestToken(),explanationBuffer,statusBuffer,uid,true,0); 
          String displayStatusMsg = "Ok"; 
          if(statusBuffer.toString().equals("SRM_SUCCESS") || 
				statusBuffer.toString().equals("SRM_NOT_SUPPORTED") ||
				statusBuffer.toString().equals("SRM_DONE")) {
             if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
               displayStatusMsg = "N.S.";
             }
          }
          else {
            displayStatusMsg = "Failed";
          }
          ReportRequestStatus rrs = 
				util.createReportRequestStatus(surl[0],"","");
          rrs.setRequestId(response.getRequestToken());
          rrs.setActualResult(statusBuffer.toString());
          rrs.setMatchingInfo(statusMsg);
          rrs.setLinkName("abortfilesbasic_"+currTestCondition);
          try {
            util.webScript(publishUrlStr,
             displayType,"SendingAbortFiles","AbortFiles", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp,
			 operatorTimeStamp, servicePath,noPublish,
		     siteName,numOperations,typeString,
			 "srmAbortFiles","Sending",dailyTest,numSites,
			 localPublish,bs,bsa);
            util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg, 
			   rrs, explanationBuffer.toString(), timeStamp,fullTimeStamp, 
			   startTimeStamp, operatorTimeStamp, servicePath,noPublish,
		       siteName,numOperations,typeString,"srmAbortFiles",
			   displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception e) {e.printStackTrace();}

         if(currTestCondition == 1) {
            statusMsg = doAbortFiles(srm, surl[0],
		   	  response.getRequestToken(),explanationBuffer,
			  statusBuffer,uid,true,1); 
            displayStatusMsg = "Ok"; 
            if(statusMsg.equals("SRM_SUCCESS") || 
			   statusMsg.equals("SRM_NOT_SUPPORTED") ||
			   statusMsg.equals("SRM_DONE")) {
               if(statusMsg.equals("SRM_NOT_SUPPORTED")) {
                 displayStatusMsg = "N.S.";
               }
            }
            else {
              displayStatusMsg = "Failed";
            }
            rrs = 
				util.createReportRequestStatus(surl[0],"","");
            rrs.setRequestId(response.getRequestToken());
            rrs.setActualResult(statusBuffer.toString());
            rrs.setMatchingInfo(statusMsg);
            rrs.setLinkName("abortfilesbadtoken_"+currTestCondition);

            try {
             util.webScript(publishUrlStr,
               displayType,"SendingAbortFiles","AbortFiles for bad token", rrs,
               "", timeStamp, fullTimeStamp, startTimeStamp,
			   operatorTimeStamp, servicePath,noPublish,
		       siteName,numOperations,typeString,"srmAbortFiles","Sending",
			   dailyTest,numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg,
              rrs, explanationBuffer.toString(), 
		  	  timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles",
			  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
            }catch(Exception e) {e.printStackTrace();}

            statusMsg = doAbortFiles(srm, surl[0],
		   	  response.getRequestToken(),explanationBuffer,
			  statusBuffer,uid,true,2); 
            displayStatusMsg = "Ok"; 
            if(statusMsg.equals("SRM_SUCCESS") || 
			   statusMsg.equals("SRM_NOT_SUPPORTED") ||
			   statusMsg.equals("SRM_DONE")) {
              if(statusMsg.equals("SRM_NOT_SUPPORTED")) {
                displayStatusMsg = "N.S.";
              }
            }
            else {
              displayStatusMsg = "Failed";
            }
            rrs = util.createReportRequestStatus(surl[0],"","");
            rrs.setRequestId(response.getRequestToken());
            rrs.setActualResult(statusBuffer.toString());
            rrs.setMatchingInfo(statusMsg);
            rrs.setLinkName("abortfilesbadsurl_"+currTestCondition);
            try {
             util.webScript(publishUrlStr,
              displayType,"SendingAbortFiles","AbortFiles for bad surl", rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp,
			  servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles",
			  "Sending",dailyTest,numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg,
              rrs, explanationBuffer.toString(), 
		  	  timeStamp,fullTimeStamp, startTimeStamp,
			  operatorTimeStamp, servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles",
			  displayStatusMsg, dailyTest,numSites,localPublish,bs,bsa);
            }catch(Exception e) {e.printStackTrace();}

            statusMsg = doAbortFiles(srm, surl[0],
		   	  response.getRequestToken(),explanationBuffer,
			  statusBuffer,uid,true,3); 
            displayStatusMsg = "Ok"; 
            if(statusMsg.equals("SRM_SUCCESS") || 
			   statusMsg.equals("SRM_NOT_SUPPORTED") ||
			   statusMsg.equals("SRM_DONE")) {
              if(statusMsg.equals("SRM_NOT_SUPPORTED")) {
                displayStatusMsg = "N.S.";
              }
            }
            else {
              displayStatusMsg = "Failed";
            }
            rrs = 
				util.createReportRequestStatus(surl[0],"","");
            rrs.setRequestId(response.getRequestToken());
            rrs.setActualResult(statusBuffer.toString());
            rrs.setMatchingInfo(statusMsg);
            rrs.setLinkName
				("abortfilesalreadyabortedsurl_"+currTestCondition);
            try {
             util.webScript(publishUrlStr,
              displayType,"SendingAbortFiles",
			  "AbortFiles for already aborted file", rrs,
              "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp,
			  servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles",
			  "Sending",dailyTest,numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg,
              rrs, explanationBuffer.toString(), 
		  	  timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp,
			  servicePath,noPublish,
		      siteName,numOperations,typeString,"srmAbortFiles",
			  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
            }catch(Exception e) {e.printStackTrace();}
         }
        }catch(Exception ae) {ae.printStackTrace();}
       }
      }

       try {
         if(plainPut) {
           util.webScript(publishUrlStr,
            displayType,"RequestSummary",
		    response.getReturnStatus().getStatusCode().toString(),
            convertPut(response,tsource,"","",surl[0],""+lsize,notOkMsg),
            "", timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp,
			servicePath,noPublish,
		    siteName,numOperations,typeString,"srmPrepareToPut-OverAll",
			"Failed", dailyTest,numSites,localPublish,bs,bsa);
            canPutContinue.isOperOk(false);
         }
         else {
              util.webScript(publishUrlStr, displayType,"Sending3END",
	           "", null, "", timeStamp, fullTimeStamp, startTimeStamp,
			   operatorTimeStamp, servicePath,noPublish,
		       siteName,numOperations,typeString,"","Failed",
			   dailyTest,numSites,localPublish,bs,bsa);

              util.webScript(publishUrlStr,displayType,"RequestSummary",
               response.getReturnStatus().getStatusCode().toString(), 
			   convertPut(response,copySourceFile,tsource,"",
			   surl[0],"-1",notOkMsg), "",
               timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp,
			   servicePath,noPublish,
		       siteName,numOperations,typeString,"","Failed",
			   dailyTest,numSites,localPublish,bs,bsa);
		 } 
      } catch(Exception e) { e.printStackTrace(); }

     try {
      doReleaseSpace(srm,spaceToken);
     }catch(Exception e) {e.printStackTrace();}
      if(sLock != null) {
         sLock.setIncrementCount();
      }
  }
  
  gridFTPDone=true;
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

