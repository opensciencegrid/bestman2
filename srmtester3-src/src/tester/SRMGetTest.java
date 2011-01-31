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
// Class SRMGetTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMGetTest implements threadIntf
{
   private String publishUrlStr;
   private String displayType;
   private String testType;
   private String timeStamp;
   private String fullTimeStamp;
   private String servicePath;
   private GSSCredential credential;
   private int testCondition;
   private String proxyFile;
   private boolean onlyGsiFTPGet;
   private boolean noPublish;
   private boolean detailed;
   private boolean gridFTPDone=false;
   private SharedObjectLock sLock;
   private boolean threepartycopy;
   private boolean defaultProxy;
   private boolean advisoryDelete;
   private int currTestCondition;
   private Log logger;
   private String spaceToken="";
   private boolean reserveSpace;
   private boolean alreadyReleasedSpace;
   private boolean dcau=true;
   private boolean dailyTest=false;
   private String startTimeStamp="";
   private int numSites;
   private String siteName="";
   private int numOperations;
   private int bufferSize;
   private int parallelism;
   private String uid="";
   private PrintIntf pIntf;
   private int retryTimeAllowed;
   private int statusWaitTime=15; //default 15 seconds
   private String typeString = "srmPrepareToGet";
   private String typeStringForFunctionTest = "srmPrepareToGet";
   private boolean afterBringOnline=false;
   private boolean localPublish;
   BufferedWriter bs;
   BufferedWriter bsa;
   OperOk rmOk; 
   OperOk isGetSuccess;
   private boolean useGUC = true;
   private String gucScriptPath=""; 
   private String tempScriptPath=""; 
   private TimeOutCallBack timeOutCallBack;
   private boolean useDriverOn;
   private OperOk pingOverAllSuccess;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMGetTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMGetTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp,
	   String servicePath, 
       GSSCredential credential, String proxyFile, 
	   boolean onlyGsiFTPGet, boolean noPublish, 
	   boolean detailed, boolean threepartycopy, boolean defaultProxy, 
	   boolean advisoryDelete, boolean dcau, int bufferSize, 
	   int parallelism, String siteName, int numOperations, 
	   String typeString, String startTimeStamp, boolean dailyTest, 
	   int numSites, int retryTimeAllowed, 
	   int statusWaitTime, boolean afterBringOnline,
       boolean localPublish, BufferedWriter bs, BufferedWriter bsa,
	   Log logger, PrintIntf pIntf, boolean useGUC, 
	   String gucScriptPath, String tempScriptPath,
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
   this.threepartycopy = threepartycopy;
   this.defaultProxy = defaultProxy;
   this.advisoryDelete = advisoryDelete;
   this.dcau = dcau;
   this.bufferSize = bufferSize;
   this.parallelism = parallelism;
   this.siteName = siteName;
   this.numOperations = numOperations;
   this.startTimeStamp = startTimeStamp;
   this.dailyTest = dailyTest;
   this.numSites = numSites;
   this.retryTimeAllowed = retryTimeAllowed;
   this.statusWaitTime = statusWaitTime*1000;
   this.logger = logger;
   this.pIntf = pIntf;
   this.afterBringOnline = afterBringOnline;
   this.localPublish = localPublish;
   this.bs = bs;
   this.bsa = bsa;
   if(afterBringOnline) {
     typeStringForFunctionTest = "srmPrepareToGetAfterBringOnline";
   }
   if(!typeString.equals("")) {
    this.typeString = typeString;
   }
   this.useGUC = useGUC;
   this.gucScriptPath = gucScriptPath;
   this.tempScriptPath = tempScriptPath;
   this.useDriverOn = useDriverOn;
   this.pingOverAllSuccess = pingOverAllSuccess;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGetFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doGetFile(ISRM srm, StringBuffer proxyString, String uid,
    String source, String localTarget, String remoteTarget, boolean plainGet, 
	SharedObjectLock sLock, String sToken, int testCondition, 
	Boolean canGetContinue, OperOk isGetSuccess,
	boolean directGsiFTP, TFileStorageType fileStorageType) 
		throws  Exception {

  this.sLock = sLock;
  this.testCondition = testCondition;
  String[] surls = null;
  surls = new String[1];
  surls [0] = new String(source.trim());

  this.currTestCondition = testCondition;
  this.isGetSuccess = isGetSuccess;
  this.uid = uid;
  if(sToken != null && !sToken.equals("")) {
   spaceToken = sToken;
  }

  util.printMessageHL("\nSRM-TESTER: " + new  Date() +
		" Calling Get request ...\n",logger,pIntf);
  //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

  SrmPrepareToGetRequest r = new SrmPrepareToGetRequest(); 
  org.apache.axis.types.URI uri = new org.apache.axis.types.URI(surls[0]);

  ReportRequestStatus rrs = util.createReportRequestStatus(source,localTarget,"");

  if(!spaceToken.equals("")) {
   r.setTargetSpaceToken(spaceToken);
   rrs.setSpaceToken(spaceToken);
  }

  Date d = new Date();
  String operatorTimeStamp = d.toString();

  if(detailed) {
      if(testCondition == 0) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestSimple(surls[0],"user description test string",rrs);
      }
      else if (testCondition == 1) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.VOLATILE,rrs);
      }
      else if (testCondition == 2) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.PERMANENT,rrs);
      }
      else if (testCondition == 3) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestStorageType
			(surls[0],"user description test string", TFileStorageType.DURABLE,rrs);
      }
      else if (testCondition == 4) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestRequestTime
			(surls[0],"user description test string", 60,0,rrs);
      }
      else if (testCondition == 5) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestRequestTime
			(surls[0],"user description test string",0,60,rrs);
      }
      else if (testCondition == 6) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestStorageParam
			(surls[0],"user description test string", uid,rrs);
      }
      else if (testCondition == 7) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestTransferParam
			(surls[0],"user description test string",rrs);
      }
      else if (testCondition == 8) {
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
        pTest.doTestRetentionPolicy (surls[0],"user description test string",rrs);
      }
      else if (testCondition == 9) {
        reserveSpace=true;
        rrs = util.createReportRequestStatus("","","");
        rrs.setUserId(uid);
        rrs.setUserDescription("test description");
        rrs.setTotalSpace(""+1000);
        rrs.setGuarnSpace(""+1000);
        rrs.setLifeTime(""+10000);
        SRMPrepareToGetTest pTest = new SRMPrepareToGetTest(r,logger,
			proxyString,dcau,bufferSize,pIntf);
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
            operatorTimeStamp, 
			servicePath, noPublish,
	        siteName,numOperations,typeString,"srmReserveSpace","Sending",
			dailyTest, numSites,localPublish,bs,bsa);

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
        HashMap resultMap = new HashMap();
        String explanation="";
        if(spaceResponse != null) {
          rrs.setLinkName("reservespace_"+testCondition); 
          if(spaceResponse.getReturnStatus() != null) {
              TReturnStatus spaceStatus = spaceResponse.getReturnStatus();
              TStatusCode sCode = spaceStatus.getStatusCode();
              resultMap.put(uid,sCode);
              String rToken = spaceResponse.getRequestToken();
              util.printMessage("Status Code for spaceStatus " + 
					sCode.toString(), logger, pIntf);
              util.printMessage("Status Explanation" + 
					spaceStatus.getExplanation(), logger, pIntf);
              if(spaceStatus.getExplanation() != null) {
                 explanation= spaceStatus.getExplanation();
              }
              util.printMessage("Request token " + rToken, logger,pIntf);
               if(sCode == TStatusCode.SRM_REQUEST_QUEUED ||
                  sCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
                  util.webScript(publishUrlStr,
	                displayType,"ReserveSpace","ReserveSpace", rrs,
		            sCode.toString(), timeStamp,fullTimeStamp, 
                    startTimeStamp, operatorTimeStamp, 
			        servicePath,noPublish,
		            siteName,numOperations,typeString,"srmReserveSpace",
	                "Ok",dailyTest, numSites,localPublish,bs,bsa);
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

                     SrmStatusOfReserveSpaceRequestResponse 
						spaceStatusResponse = null;

                     ssTimeStamp = util.startTimeStamp();
                     timeOutCallBack = 
						new TimeOutCallBack(spaceStatusResponse,ssTimeStamp);
                     timeOutCallBack.setDriverInfo
						(useDriverOn,pingOverAllSuccess);
                     timeOutCallBack.setSharedObjectLock(sLock);
                     timeOutCallBack.setRequestToken(rToken,srm);
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
                       util.printMessage
							("Status Code for spaceStatusRequest " + 
								sCode.toString(), logger, pIntf);
                       if(sCode != TStatusCode.SRM_REQUEST_INPROGRESS &&
                          sCode != TStatusCode.SRM_REQUEST_QUEUED) {
                           resultMap.remove(uid);
                           rrs.setActualResult(sCode.getValue());
                           spaceToken = spaceStatusResponse.getSpaceToken();
                           String displayStatusMsg="Ok";
                           if(sCode == TStatusCode.SRM_SUCCESS || 
							  sCode == TStatusCode.SRM_DONE) {
                             rrs.setMatchingInfo("Yes");
                           }
                           else {
                             rrs.setMatchingInfo("No, expecting SRM_SUCCESS");
                             displayStatusMsg="Failed";
                           }
                           util.webScript(publishUrlStr,
	                        displayType,"ReserveSpace",displayStatusMsg, rrs,
		                    sCode.toString(), timeStamp,fullTimeStamp, 
                            startTimeStamp, operatorTimeStamp, 
			                servicePath,noPublish,
		                    siteName,numOperations,typeString,
							"srmStatusOfReserveSpaceRequest",
	                        displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
                       }
                     }
                    else {
                     util.printMessage ("Null return status from " + 
						"srmStatusOfReserveSpaceRequest",logger,pIntf);
                     util.webScript(publishUrlStr,
	                        displayType,"ReserveSpace","Failed", rrs,
		                    "Notes from tester : Null return status", 
							timeStamp,fullTimeStamp, 
                            startTimeStamp, operatorTimeStamp, 
			                servicePath,noPublish,
		                    siteName,numOperations,typeString,
							"srmStatusOfReserveSpaceRequest",
	                        "Failed",dailyTest, numSites,localPublish,bs,bsa);
                    }
                  }
                  else {
                    util.printMessage("Expecting requestToken for " + 
					    "this status code ", logger, pIntf);
                    resultMap.remove(uid);
                    util.webScript(publishUrlStr,
	                        displayType,"ReserveSpace","Failed", rrs,
		                    "Expecting requestTokenf or this status code ", 
							timeStamp,fullTimeStamp, 
                            startTimeStamp, operatorTimeStamp, 
			                servicePath,noPublish,
		                    siteName,numOperations,typeString,
							"srmStatusOfReserveSpaceRequest",
	                        "Failed",dailyTest, numSites,localPublish,bs,bsa);
                  }
                }
              }
              else {
               rrs.setActualResult(sCode.getValue());
               util.printMessage ("Status Code for spaceRequest " + 
					sCode.toString(), logger, pIntf);
               util.printMessage ("Status Code for Explanation " + 
					explanation, logger, pIntf);
               spaceToken = spaceResponse.getSpaceToken();
               util.printMessage("SpaceToken " + spaceToken, logger, pIntf);
               String displayStatusMsg="Ok";
               if(sCode == TStatusCode.SRM_SUCCESS || sCode == TStatusCode.SRM_DONE) {
                  rrs.setMatchingInfo("Yes");
               }
               else {
                  rrs.setMatchingInfo("No, expecting SRM_SUCCESS");
                  displayStatusMsg="Failed";
               }
               util.webScript(publishUrlStr,
	              displayType,"ReserveSpace",sCode.toString(), rrs,
		          explanation, timeStamp,fullTimeStamp, 
                  startTimeStamp, operatorTimeStamp, 
			      servicePath,noPublish,
		          siteName,numOperations,typeString,"srmReserveSpace",
	              displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
              }
              if(spaceToken != null && !spaceToken.equals("")) {
                util.printMessage("\n\nTargetSpaceToken " + 
					spaceToken,logger, pIntf);
                rrs.setSpaceToken(spaceToken);
              }
             }
             else {
               util.printMessage("SRM returns null status ", logger, pIntf);
             }
          }
          else {
             util.printMessage("Cannot do this test since " +
					"spaceResponse is null", logger, pIntf);
          }
          util.printMessage("\n\nSpaceToken " + spaceToken,logger, pIntf);

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
		       "SRM returned null space token", timeStamp,fullTimeStamp, 
               startTimeStamp, operatorTimeStamp, 
			   servicePath,noPublish,
		       siteName,numOperations,typeString,"srmReserveSpace-OverAll",
	           "Failed",dailyTest, numSites,localPublish,bs,bsa);
          } 
          else {
            util.webScript(publishUrlStr,
	           displayType,"ReserveSpace","SUCCESS", rrs,
		       "", timeStamp, fullTimeStamp, startTimeStamp, 
			   operatorTimeStamp, servicePath,noPublish,
	           siteName,numOperations,typeString,"srmReserveSpace-OverAll",
			   "Ok", dailyTest, numSites,localPublish,bs,bsa);
          }
          if(spaceToken != null) {
            rrs = util.createReportRequestStatus(source,localTarget,"");
            pTest.doReserveSpaceGet(surls[0],spaceToken,rrs);
          }
          else { 
            util.printMessage("Cannot do this test since spaceToken " +
				"is null", logger, pIntf);
          }
      }
      else if(testCondition == -1) {
        if(!directGsiFTP) {
          SRMPrepareToGetTest pTest = 
			new SRMPrepareToGetTest(r,logger,proxyString,dcau,bufferSize,pIntf);
          pTest.doTestBasic (surls[0],fileStorageType,rrs);
        }
      }
  } 
  else {
     if(!directGsiFTP) {
       SRMPrepareToGetTest pTest = 
			new SRMPrepareToGetTest(r,logger,proxyString,dcau,bufferSize,pIntf);
       pTest.doTestBasic (surls[0],fileStorageType,rrs);
     }
  }

  if(plainGet) {
   try {
     if(detailed) {
      util.webScript(publishUrlStr,
	   displayType,"Sending","Get Request now for Advanced Test ...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
		siteName,numOperations,typeString,typeStringForFunctionTest,
	    "Sending",dailyTest, numSites,localPublish,bs,bsa);
     } 
     else {
      if(!directGsiFTP) {
      util.webScript(publishUrlStr,
	   displayType,"Sending","Get Request now for Basic Test ...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
		siteName,numOperations,typeString,typeStringForFunctionTest,
		"Sending",dailyTest, numSites,localPublish,bs,bsa);
       }
     }
    }catch(Exception e) { e.printStackTrace(); }
  }
  else {
   try {
     util.webScript(publishUrlStr,
	   displayType,"Sending","Get Request now...", rrs,
		"", timeStamp, fullTimeStamp, startTimeStamp, 
	    operatorTimeStamp, servicePath,noPublish,	
        siteName,numOperations,"3PartyCopy",typeString,
	    "Sending",dailyTest, numSites,localPublish,bs,bsa);
    }catch(Exception e) { e.printStackTrace(); }
  }

  if(srm == null) {
   if(!directGsiFTP) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "Get","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp, startTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
	       siteName,numOperations,typeString,typeStringForFunctionTest,
		   "Failed",dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, 
	    operatorTimeStamp, servicePath,noPublish,
	    siteName,numOperations,typeString,typeStringForFunctionTest,
	    "Failed",dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     if(sLock != null)  {
        sLock.setIncrementCount();
     }
     canGetContinue = new Boolean(false);
     isGetSuccess.isOperOk(false);
     return "";
   }
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
		   ("Cannot do Get request, given target directory " +
			tt + " did not exists.", logger,pIntf);

          try {
             rrs = new ReportRequestStatus();
             rrs.setLinkName("gettargetdirexp");
             util.webScript (publishUrlStr,displayType,
 		       "Exception","",rrs,
	           "Cannot do Get request, given target directory " + tt +
			    " did not exists.",timeStamp,fullTimeStamp, startTimeStamp, 
                operatorTimeStamp, 
			    servicePath,noPublish,
                siteName,numOperations,typeString,typeStringForFunctionTest,
			    "Exception",dailyTest, numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
        }
      }
    }
  }


  if(directGsiFTP) {

      if(!source.startsWith("gsiftp:")) {
        util.printMessage("\nFor the -direct option, source must starts with "+
			"gsiftp.",logger,null);
        return "";
      }

      String temp = util.getTargetFileName(localTarget);
      String size = ""+0;
      StringBuffer gsiFTPSTimeStamp = new StringBuffer();
      StringBuffer gsiFTPETimeStamp = new StringBuffer();

      util.printMessage ("SRM-TESTER: " + new Date() +
		" copying the file to " + temp + "\n",logger,pIntf);

      String turl = util.doValidateCheckingForFormat(source);
      temp = util.doValidateCheckingForFormat(temp);
      StringBuffer expBuffer = new StringBuffer();
      gsiFTPSTimeStamp = util.getTimeStamp();

      ThreadCallBack tcb = new ThreadCallBack(this,"gridftp");
      Hashtable paramTable = new Hashtable();
      paramTable.put("tsource",source);
      paramTable.put("temp",temp);
      paramTable.put("source",source);
      paramTable.put("starttimestamp",gsiFTPSTimeStamp);
      paramTable.put("endtimestamp",gsiFTPETimeStamp);
      paramTable.put("optimestamp",operatorTimeStamp);
      if(srm != null) {
        paramTable.put("srm",srm);
      }
      paramTable.put("canGetContinue",canGetContinue);
      paramTable.put("isGetSuccess",isGetSuccess);
      paramTable.put("expbuffer",expBuffer);
      paramTable.put("localtarget",localTarget);
      paramTable.put("size",size);
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
          util.printMessage("GsiFTP failed with timeout (1): ", 
			logger,pIntf);
          util.printMessage("......................................\n",
			logger,pIntf);
          try {
           rrs = util.createAndConvertRequest(turl,temp,0,-1);
           rrs.setActualResult("FAILED");
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
             displayType,"GridFTP","GsiFTP Failed with timeout", 
			 rrs, "GsiFTP Failed with timeout",
             timeStamp, fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
			 siteName, numOperations,typeString,"GridFTP-Get",
			 "TimeOut",dailyTest, numSites,localPublish,bs,bsa);
             rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,0);
             rrs.setLinkName("gsiftp_"+testCondition);
             util.webScript(publishUrlStr,
		      displayType,"GsiFTP","Failed duirng GsiFTP",rrs,
		      "GsiFTP failed with timeout",
			  timeStamp,fullTimeStamp, startTimeStamp, 
              operatorTimeStamp,
			  servicePath,noPublish,
              siteName,numOperations,typeString,"GridFTP-Get",
			  "Failed",dailyTest, numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,
		       displayType,"RequestSummary","Failed during GsiFTP",
			   convertGet(null,"","",localTarget,size,expBuffer.toString()),
			   "",timeStamp,fullTimeStamp, startTimeStamp, 
			   operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,typeStringForFunctionTest,
		       "Failed",dailyTest, numSites,localPublish,bs,bsa);
              canGetContinue = new Boolean(false);
              isGetSuccess.isOperOk(false);
            }catch(Exception  e) { e.printStackTrace(); }
            doReleaseSpace(srm,spaceToken);
            if(sLock != null) { 
             sLock.setIncrementCount();
            }
          }
      }

    return "";
  }

  SrmPrepareToGetResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
  timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  try {
    response = srm.srmPrepareToGet(r); 
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
  HashMap statusArray = printGetResult(response,responseBuffer);
  StringBuffer expBuffer = new StringBuffer();

  if(response == null || statusArray == null || statusArray.size() == 0) {
    reqOk = false;
    if(!responseBuffer.toString().equals("")) {
      expMsg = responseBuffer.toString();
    }
  }

  if(response != null && response.getReturnStatus() != null 
		&& response.getReturnStatus().getStatusCode() != null ) {
    if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
       response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE ) {
         reqOk=true;
    }
  }

  int sTimeStamp = util.startTimeStamp();
  URI keyURL = null;
  boolean doStatusTest = true;
  boolean testerTimedOut=false;
  try {
  int i = 0;
  while (statusArray.size() > 0) {
      expBuffer = new StringBuffer(); 
// AS 070809
// changed from 5000 to 60000
      Thread.sleep(statusWaitTime);
      if(!util.isRetryOk(sTimeStamp,retryTimeAllowed)) {
         util.printMessage("Max retry timed exceeded", logger, pIntf);
         expBuffer.append("Notes from tester : Max retry timed out ...");
         testerTimedOut=true;
         break;
      }
      if(i >= statusArray.size()) {
        i = 0;
      }
      Object key = (statusArray.keySet().toArray()) [i];
      keyURL = (URI)key;
      TGetRequestFileStatus status =
           (TGetRequestFileStatus)(statusArray.get(key));
      TStatusCode code = status.getStatus().getStatusCode();
      StringBuffer rCode = new StringBuffer();
      expBuffer.delete(0,expBuffer.length());
      if((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
       (code == TStatusCode.SRM_REQUEST_QUEUED)) {
       keyURL = (URI)key;
        /*
       if(doStatusTest && !detailed && !threepartycopy) {
         checkGetStatusTest(srm,(URI)key,response,1);
         checkGetStatusTest(srm,(URI)key,response,2);
       }
        */
       IsWholeRequestFailed wholeRequestFailed = new IsWholeRequestFailed();
       System.out.println("SRM-TESTER: Calling Get Status " + new Date());
       status = checkGetStatus(srm,
			(URI)key,response, rCode,doStatusTest,expBuffer,
			wholeRequestFailed);
       doStatusTest = false;
       TReturnStatus rStatus = new TReturnStatus();
       TStatusCode tempCode = util.mapReturnStatusValueBackToCode(rCode);
       if(tempCode != null) {
         rStatus.setStatusCode(tempCode);
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
          util.printMessage("Status " + status.getStatus().getStatusCode(),
			logger,pIntf);
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
       //added file_in_cache for cern v2 server
      if((code != TStatusCode.SRM_REQUEST_INPROGRESS) &&
         (code != TStatusCode.SRM_REQUEST_QUEUED)) {
       util.printMessage("\nFileStatus code=" + code,logger,pIntf);
       if(status != null && status.getStatus() != null) {
          util.printMessage("\nExplanation=" + 
				status.getStatus().getExplanation(), logger,pIntf);
       }
       if(code == TStatusCode.SRM_FILE_PINNED || 
         code == TStatusCode.SRM_SUCCESS || code == TStatusCode.SRM_DONE || 
		 code == TStatusCode.SRM_FILE_IN_CACHE) {
        if(status != null && status.getTransferURL() != null) {
         try {
            util.webScript(publishUrlStr,
			displayType,"GetStatusMethod",
	        code.toString(),null,
			expBuffer.toString(), timeStamp, fullTimeStamp, startTimeStamp, 
			operatorTimeStamp,servicePath,noPublish,
	        siteName,numOperations,typeString,"srmStatusOfGetRequest",
		    "Ok",dailyTest, numSites,localPublish,bs,bsa);
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("getstatus");
               gOk.setTried(true);
               gOk.isOperOk(true);
             }

           }catch(Exception e) {e.printStackTrace();}
           turl = status.getTransferURL().toString();
           size = ""+status.getFileSize();
           TReturnStatus rs1 = new TReturnStatus();
           rs1.setStatusCode(code);
           status.setStatus(rs1);
           TGetRequestFileStatus[] statuses = new TGetRequestFileStatus[1];
           statuses[0] = status;
           ArrayOfTGetRequestFileStatus arrayofstatus =
            new ArrayOfTGetRequestFileStatus();
           arrayofstatus.setStatusArray(statuses);
           response.setArrayOfFileStatuses(arrayofstatus);

           reqOk = true;
         }
         statusArray.remove(key);
       }
       else {
        util.webScript(publishUrlStr,
			displayType,"GetStatusMethod",
	        code.toString(),null,
			expBuffer.toString(), timeStamp, fullTimeStamp, startTimeStamp, 
			operatorTimeStamp,servicePath,noPublish,
	        siteName,numOperations,typeString,"srmStatusOfGetRequest",
		    "Failed",dailyTest, numSites,localPublish,bs,bsa);
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("getstatus");
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
          }
          else {  
            expMsg = "SRM returned File status code null";
            rs.setExplanation("");
          }
        }
        response.setReturnStatus(rs);
        if(status != null) {
          status.setStatus(rs);
          TGetRequestFileStatus[] statuses = new TGetRequestFileStatus[1];
          statuses[0] = status;
          ArrayOfTGetRequestFileStatus arrayofstatus =
            new ArrayOfTGetRequestFileStatus();
          arrayofstatus.setStatusArray(statuses);
          response.setArrayOfFileStatuses(arrayofstatus);
        }
        statusArray.remove(key);
       }
    }
    else {
       util.printMessageNL(".",logger,pIntf);
    }
  }
  }catch(java.rmi.RemoteException re) {
     expMsg = re.getMessage();
  }

  util.printMessage("",logger,pIntf);

  
  if(currTestCondition == 4 && keyURL != null) {
     util.printMessage
	  ("\nWaiting here to do the advanced test for srmStatusOfGetRequest" +
        "\nfor condition wait for 120 seconds for the total request time ", 
			logger,pIntf); 
// AS 070809
// changed from 120000 to 300000
     Thread.sleep(statusWaitTime);
     checkGetStatusTest(srm,keyURL,response,3);
  }

  if(reqOk) {
    String displayStatusMsg = "Ok";
    try {
      rrs = new ReportRequestStatus();
      rrs.setLinkName("GET_"+testCondition);
      rrs.setActualResult
			(response.getReturnStatus().getStatusCode().getValue());
     
      if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS || 
	     response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
	     response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
          rrs.setMatchingInfo("Yes");
         if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED)
         {
           displayStatusMsg = "N.S.";
         }
      }
      else {
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
         displayStatusMsg = "Failed";
      }
      util.webScript(publishUrlStr,
			displayType,"Get",
	        response.getReturnStatus().getStatusCode().getValue(),rrs,
			"Notes from tester : TransferURL has been returned.", 
			timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		    servicePath,noPublish,
	        siteName,numOperations,typeString,"srmStatusOfGetRequest",
		    displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("getstatus");
               gOk.setTried(true);
               if(displayStatusMsg.equals("Ok")) {
                 gOk.isOperOk(true);
               }
               else {
                 gOk.isOperOk(false);
                 gOk.setExplanation(expBuffer.toString());
               } 
             }
    }catch(Exception e) { e.printStackTrace(); }

    util.printMessageHNL("\nSRM-TESTER: TURL returned=",logger,pIntf);
    util.printMessageNL(turl + "\n",logger,pIntf);
    //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);

    StringBuffer gsiFTPSTimeStamp = new StringBuffer();
    StringBuffer gsiFTPETimeStamp = new StringBuffer();
    if(plainGet) {
      String temp = util.getTargetFileName(localTarget);
      util.printMessage ("SRM-TESTER: copying the file to " + temp + "\n",
			logger,pIntf);

      turl = util.doValidateCheckingForFormat(turl);
      temp = util.doValidateCheckingForFormat(temp);

      gsiFTPSTimeStamp = util.getTimeStamp();
      expBuffer = new StringBuffer();

      ThreadCallBack tcb = new ThreadCallBack(this,"gridftp");
      Hashtable paramTable = new Hashtable();
      paramTable.put("tsource",turl);
      paramTable.put("keyurl",keyURL);
      paramTable.put("temp",temp);
      paramTable.put("source",source);
      paramTable.put("starttimestamp",gsiFTPSTimeStamp);
      paramTable.put("endtimestamp",gsiFTPETimeStamp);
      paramTable.put("optimestamp",operatorTimeStamp);
      paramTable.put("srm",srm);
      paramTable.put("canGetContinue",canGetContinue);
      paramTable.put("isGetSuccess",isGetSuccess);
      paramTable.put("expbuffer",expBuffer);
      paramTable.put("localtarget",localTarget);
      paramTable.put("size",size);
      paramTable.put("turl",turl);
      paramTable.put("direct",new Boolean(false)); 
      paramTable.put("response",response);
      tcb.setParams_2(paramTable);
      tcb.start();

      int gTimeStamp = util.startTimeStamp();
      while (!gridFTPDone) {
// AS 070809
// changed from 5 seconds to 30 seconds
        Thread.sleep(statusWaitTime);
        if(!util.isRetryOk(gTimeStamp,retryTimeAllowed)) {
          gridFTPDone = true;
          tcb.stop();
          gsiFTPETimeStamp = util.getTimeStamp();
          util.printMessage("\n......................................",
			logger,pIntf);
          util.printMessage("GsiFTP failed with timeout (2): ", 
			logger,pIntf);
          util.printMessage("......................................\n",
			logger,pIntf);
          try {
           rrs = util.createAndConvertRequest(turl,temp,0,-1);
           rrs.setActualResult("FAILED");
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
             displayType,"GridFTP","GsiFTP Failed with timeout", 
			 rrs, "GsiFTP Failed with timeout",
             timeStamp, fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
			 siteName, numOperations,typeString,"GridFTP-Get",
			 "TimeOut",dailyTest, numSites,localPublish,bs,bsa);
             rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,0);
             rrs.setLinkName("gsiftp_"+testCondition);
             util.webScript(publishUrlStr,
		      displayType,"GsiFTP","Failed duirng GsiFTP",rrs,
		      "GsiFTP failed with timeout",
			  timeStamp,fullTimeStamp, startTimeStamp, 
              operatorTimeStamp,
			  servicePath,noPublish,
              siteName,numOperations,typeString,"GridFTP-Get",
			  "Failed",dailyTest, numSites,localPublish,bs,bsa);
             util.webScript(publishUrlStr,
		       displayType,"RequestSummary","Failed during GsiFTP",
			   convertGet(null,"","",localTarget,size,expBuffer.toString()),
			   "",timeStamp,fullTimeStamp, startTimeStamp, 
			   operatorTimeStamp, servicePath,noPublish,
               siteName,numOperations,typeString,typeStringForFunctionTest,
		       "Failed",dailyTest, numSites,localPublish,bs,bsa);
              canGetContinue = new Boolean(false);
              isGetSuccess.isOperOk(false);
            }catch(Exception  e) { e.printStackTrace(); }
            doReleaseSpace(srm,spaceToken);
            if(sLock != null) { 
             sLock.setIncrementCount();
            }
          }
      }
     return "";
   }//end (plainGet)
   else { //threeparty transfers
     return turl;
   }
  }
  else {
    canGetContinue = new Boolean(false);
    isGetSuccess.isOperOk(false);
    util.printMessage
  	  ("SRM-TESTER: Get File request failed.\n",logger,pIntf);
    util.printMessage("#################\n", logger,pIntf);
    util.printMessage("SRM-TESTER: Testing " + testType + " failed ", logger,pIntf);
    util.printMessage("#################\n", logger,pIntf);

    try {
      rrs = new ReportRequestStatus();
      rrs.setLinkName("GET_"+testCondition);
      rrs.setActualResult
		(response.getReturnStatus().getStatusCode().getValue());
      if(testCondition == 4) {
        if(response.getReturnStatus().getStatusCode() == 
	       TStatusCode.SRM_REQUEST_TIMED_OUT || 
			   response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_FAILURE) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo
		    ("No. expecting SRM_REQUEST_TIMED_OUT or SRM_FAILURE");
        }
      }
      else {
        if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
          response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
          rrs.setMatchingInfo("Yes");
        }
        else {
          rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        }
      }
      String ee = getExplanationFromResponse(response);
      if(ee.equals("")) { 
        ee = expMsg+"." + expBuffer.toString();
      }
      else {
        ee = ee + ". " + expMsg + "." + expBuffer.toString();
      }
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
      util.webScript(publishUrlStr,displayType,
		"Get",response.getReturnStatus().getStatusCode().getValue(),
    	rrs, ee,
		timeStamp,fullTimeStamp, startTimeStamp, 
		    operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,typeStringForFunctionTest,
			displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
      canGetContinue = new Boolean(false);
      isGetSuccess.isOperOk(false);
      if(plainGet) {
        if(testerTimedOut) {
          isGetSuccess.setExplanation("TimedOut");
          util.webScript(publishUrlStr,displayType,
	  	  "Get",
		  response.getReturnStatus().getStatusCode().toString(),
          rrs,
		  expBuffer.toString(), timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
          "srmStatusOfGetRequest",
		  "TimedOut",dailyTest, numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,displayType,
	  	  "RequestSummary",
		  response.getReturnStatus().getStatusCode().toString(),
		  convertGet(response,"","",localTarget,size,expBuffer.toString()),
		  expBuffer.toString(), timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
		  typeStringForFunctionTest+"-OverAll",
		  "TimedOut",dailyTest, numSites,localPublish,bs,bsa);
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map =isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("getstatus");
               gOk.setTried(true);
               gOk.isOperOk(false);
               gOk.setExplanation("TimedOut");
             }
        }
        else {
          if(displayStatusMsg.equals("Ok")) { 
            isGetSuccess.isOperOk(true);
          }
          else {
            isGetSuccess.setExplanation(displayStatusMsg);
          }
          util.webScript(publishUrlStr,displayType,
	  	  "RequestSummary",
		  response.getReturnStatus().getStatusCode().toString(),
		  convertGet(response,"","",localTarget,size,expBuffer.toString()),
		  expBuffer.toString(), timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
		  typeStringForFunctionTest+"-OverAll",
		  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
        }
      }		 
      doReleaseSpace(srm,spaceToken);
      if(sLock != null) {
         sLock.setIncrementCount();
      }
    }catch(Exception  e) { e.printStackTrace(); }

    if(!plainGet) {
      try {
        util.printMessage
  	      ("Cannot continue 3party transfer, since source GET failed\n",logger,pIntf);
        util.printMessage("#####################\n", logger,pIntf);
        util.printMessage("SRM-TESTER: Testing " + testType + " failed ", logger,pIntf);
        util.printMessage("#####################\n", logger,pIntf);

        util.webScript(publishUrlStr,displayType,
	  	  "3rd Party Copy","FAILED",null,
	      "Cannot continue 3rd Party Copy, " +
		  "since GET request to source SRM failed",
		  timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,"3PartyCopy",
			 typeString,"Failed",dailyTest, numSites,localPublish,bs,bsa);

         util.webScript(publishUrlStr, displayType,"Sending3END",
	      "", null,
          "", timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,"3PartyCopy",typeString,"Failed",
			 dailyTest, numSites,localPublish,bs,bsa);

         util.webScript(publishUrlStr,displayType,
	  	  "RequestSummary",
		  response.getReturnStatus().getStatusCode().toString(),
		  convertGet(response,"","","",size,expBuffer.toString()),
		  "", timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,"3PartyCopy",typeString,"Failed",
			 dailyTest, numSites,localPublish,bs,bsa);

      }catch(Exception  e) { e.printStackTrace(); }
    }
    return "";
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkGetStatusTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public void  checkGetStatusTest
    (ISRM srm, URI url, SrmPrepareToGetResponse response, int condition) 
      throws URI.MalformedURIException, java.rmi.RemoteException {

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

   String token = response.getRequestToken();

   if(token == null)  {
     throw new java.rmi.RemoteException("server sent null request token.");
   }

   if(condition == 1) {
     util.printMessage("\nSRM-TESTER: " +
		new Date() + " Calling GetStatus for Bad requestToken " + 
			token+"_bad_token", logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_INVALID_REQUEST", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token+"_bad_token");


     try {
     SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
     r.setRequestToken(token+"_bad_token");

     SrmStatusOfGetRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfGetRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nSRM-TESTER: " +
		new Date() + " Status from SRM (srmStatusOfGetRequest) " +
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
	       displayType,"SendingGETSTATUS",
		   "Get Status for Bad Token", rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,
			"srmStatusOfGetRequest","Sending",dailyTest, numSites,
			 localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	       displayType,"GetStatus","SUCCESS", rrs,
	         explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfGetRequest",
		     "Ok",dailyTest,numSites,localPublish,bs,bsa);
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("getstatus");
               gOk.setTried(true);
               gOk.isOperOk(true);
             }
        }catch(Exception e) { e.printStackTrace(); }
     }
     else {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
        rrs.setLinkName("statusbadtoken");
        try {
         util.webScript(publishUrlStr,
	       displayType,"SendingGETSTATUS",
		   "Get Status for Bad Token", rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp,
			 servicePath,noPublish,
             siteName,numOperations,"SrmPrepareToGet",
			 "srmStatusOfGetRequest","Sending",
			 dailyTest, numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
	       displayType,"GetStatus",
			  "No. expecting SRM_INVALID_REQUEST || SRM_FAILURE", rrs,
			  explanation, 
		      timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfGetRequest",
			 "Failed",dailyTest, numSites,localPublish,bs,bsa);
         }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_INVALID_REQUEST || SRM_FAILURE");
      rrs.setLinkName("statusbadtoken");
      try {
        util.webScript(publishUrlStr,
	       displayType,"SendingGETSTATUS",
		   "Failed", rrs,
		    "Bad Token", timeStamp, 
			 fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
             siteName,numOperations,typeString,
			 "srmStatusOfGetRequest","Sending",dailyTest, numSites,
			 localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	     displayType,"GetStatus","Failed", rrs,
	      "SRM returned null result", 
		   timeStamp, fullTimeStamp, startTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,"srmStatusOfGetRequest",
		   "Failed",dailyTest, numSites,localPublish,bs,bsa);
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
     util.printMessage("\nSRM-TESTER: " + 
		new Date() + 
          " Calling GetStatus for requestToken " + token, logger,pIntf);
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

     try {
     SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
     r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(uuri));
     r.setRequestToken(token);

     SrmStatusOfGetRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfGetRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfGetRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     util.printMessage("Explanation=" + explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE 
			|| result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ) {

       TGetRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         util.printMessage("\nFileStatus from SRM (srmStatusOfGetRequest) " +
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
	          displayType,"SendingGETSTATUS",
	          "Failed", rrs,
		      "Bad SURL", timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp,
			 servicePath,noPublish,
              siteName,numOperations,typeString,
			  "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"GetStatus","Yes", rrs,
			 explanation, 
		     timeStamp, fullTimeStamp, 
		     startTimeStamp, operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfGetRequest",
	         "Ok",dailyTest, numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
           rrs.setActualResult(code.getValue());
           rrs.setMatchingInfo("No. expecting SRM_INVALID_PATH || SRM_FAILURE");
           rrs.setLinkName("statusbadsurl");
          try {
            util.webScript(publishUrlStr,
	          displayType,"SendingGETSTATUS",
	          "Get Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, 
			  operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,
			  "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"GetStatus",
			 "No. expecting SRM_INVALID_PATH || SRM_FAILURE", rrs, explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfGetRequest",
	         "Ok",dailyTest, numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().getValue());
        rrs.setMatchingInfo("Yes");
        rrs.setLinkName("statusbadsurl");
        try {
         util.webScript(publishUrlStr,
	          displayType,"SendingGETSTATUS",
	          "Get Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, 
              operatorTimeStamp,
		      servicePath,noPublish,
              siteName,numOperations,typeString,
			  "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	      displayType,"GetStatus","Yes", rrs, explanation, 
		  timeStamp, fullTimeStamp, startTimeStamp, 
	      operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
          "Ok",dailyTest, numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().getValue());
       rrs.setMatchingInfo("No. expecting SRM_FAILURE");
       rrs.setLinkName("statusbadsurl");
       try {
        util.webScript(publishUrlStr,
	          displayType,"SendingGETSTATUS",
	          "Get Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			  servicePath,noPublish,
              siteName,numOperations,typeString,
			  "srmStatusOfGetRequest","Ok",dailyTest, 
		      numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	      operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
		  typeString,"srmStatusOfGetRequest",
          "Ok",dailyTest, numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    } 
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FAILURE");
      rrs.setLinkName("statusbadsurl");
      try {
       util.webScript(publishUrlStr,
	          displayType,"SendingGETSTATUS",
	          "Get Status for Bad SourceURL", rrs,
		      "", timeStamp, fullTimeStamp, startTimeStamp, 
			  operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,
			  "srmStatusOfGetRequest","Ok",dailyTest, numSites,
		      localPublish,bs,bsa);
       util.webScript(publishUrlStr,
	    displayType,"GetStatus","Failed", null,
	    "SRM returned null result ", 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
	      "Ok",dailyTest, numSites,localPublish,bs,bsa);
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
     util.printMessage("\nSRM-TESTER: " +
		new Date() +  " Calling GetStatus for expired requestToken " + 
			token, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_REQUEST_TIMED_OUT", logger,pIntf);

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);

     try {
     SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
     r.setRequestToken(token);

     SrmStatusOfGetRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfGetRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }
     if(result != null && result.getReturnStatus() != null) {

     util.printMessage("\nSRM-TESTER: Status from SRM (srmStatusOfGetRequest)" +
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
	     displayType,"SendingGETSTATUS",
	     "Get Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,
		  "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus","Yes", rrs,
		  explanation, timeStamp, fullTimeStamp, 
	      startTimeStamp, operatorTimeStamp,servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
		  "Ok",dailyTest, numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
     else {
       rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken");
       try {
        util.webScript(publishUrlStr,
	     displayType,"SendingGETSTATUS",
	     "Get Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,"","Ok",
		  dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
          operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
	      "Ok",dailyTest, numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
       rrs.setActualResult("");
       rrs.setMatchingInfo("No. expecting SRM_REQUEST_TIMED_OUT");
       rrs.setLinkName("statusexpiredtoken");
       try {
        util.webScript(publishUrlStr,
	     displayType,"SendingGETSTATUS",
	     "Get Status for expired Token", rrs,
		  "", timeStamp, fullTimeStamp, startTimeStamp, 
	      operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
		  "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus","SRM returned null status", rrs,
		  "SRM returned null status ", 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
		  "Ok",dailyTest, numSites,localPublish,bs,bsa);
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
   else if(condition == 4) {
     util.printMessage("\nSRM-TESTER: " +
		new Date() +
			"  Calling GetStatus for requestToken " + token, logger,pIntf);
     util.printMessage("\nSRM-TESTER: SURL " + url, logger,pIntf);
     util.printMessage("ExpectedResult from SRM: SRM_FILE_LIFETIME_EXPIRED", logger,pIntf);
     URI uuri = new URI(url.toString());

     ReportRequestStatus rrs = new ReportRequestStatus();
     rrs.setRequestId(token);

     ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];
 
     ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
     rrfs.setSourceUrl(uuri.toString());
 
    reportFileStatus[0] = rrfs;
 
    rrs.setReportRequestFileStatus(reportFileStatus);

    try {
     SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
     r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(uuri));
     r.setRequestToken(token);

     SrmStatusOfGetRequestResponse result = null;

     int ssTimeStamp = util.startTimeStamp();
     timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
     timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
     timeOutCallBack.setRequestToken(token,srm);
     timeOutCallBack.setSharedObjectLock(sLock);
     timeOutCallBack.start();

     try {
       result = srm.srmStatusOfGetRequest(r);
       timeOutCallBack.setObject(result);
     }catch(Exception e) {
       timeOutCallBack.interruptNow(true);
       throw e;
     }

     if(result != null) {
     util.printMessage("\nStatus from SRM (srmStatusOfGetRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);

     String explanation = result.getReturnStatus().getExplanation();
     if(explanation == null) {
       explanation = "";
     }
     util.printMessage("Explanation=" + explanation,logger,pIntf);

     if (result.getReturnStatus().getStatusCode() ==
            TStatusCode.SRM_FAILURE || 
		 result.getReturnStatus().getStatusCode() ==TStatusCode.SRM_SUCCESS) {

       TGetRequestFileStatus fileStatus = null;
       if(result.getArrayOfFileStatuses() != null) {
         fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
         TStatusCode code = fileStatus.getStatus().getStatusCode();
         explanation = fileStatus.getStatus().getExplanation();
         if(explanation == null) {
           explanation = "";
         }
         util.printMessage("\nFileStatus from SRM (srmStatusOfGetRequest) " +
            code, logger,pIntf);
         util.printMessage("Explanation=" + explanation,logger,pIntf);
         if(code == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
           rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
           rrs.setMatchingInfo("Yes");
           rrs.setLinkName("statuslifetimeexpired");  
           try {
            util.webScript(publishUrlStr,
	         displayType,"SendingGETSTATUS",
	         "Get Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			  servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfGetRequest",
			  "Ok",dailyTest, numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"GetStatus","Yes", rrs,
			  explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
              siteName,numOperations,typeString,"srmStatusOfGetRequest",
		      "Ok",dailyTest, numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }
         }
         else {
          try {
            rrs.setActualResult(fileStatus.getStatus().getStatusCode().toString());
            rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
            rrs.setLinkName("statuslifetimeexpired");  
            util.webScript(publishUrlStr,
	         displayType,"SendingGETSTATUS",
	         "Get Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,
			 "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
	         displayType,"GetStatus","No. expecting SRM_FILE_LILETIME_EXPIRED", rrs,
			  explanation, 
		     timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			 servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfGetRequest",
		     "Ok",dailyTest, numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace(); }
         }
       }
       else {
        try {
         rrs.setActualResult
			(result.getReturnStatus().getStatusCode().toString());
         rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
         rrs.setLinkName("statuslifetimeexpired");  
         util.webScript(publishUrlStr,
	         displayType,"SendingGETSTATUS",
	         "Get Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
             siteName,numOperations,typeString,
			 "srmStatusOfGetRequest","Ok",dailyTest, numSites,localPublish,bs,bsa);
         util.webScript(publishUrlStr,
	      displayType,"GetStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, 
		  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
		  "Failed",dailyTest, numSites,localPublish,bs,bsa);
        }catch(Exception e) { e.printStackTrace(); }
       }
     }
     else {
       try {
        rrs.setActualResult(result.getReturnStatus().getStatusCode().toString());
        rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
        rrs.setLinkName("statuslifetimeexpired");  
        util.webScript(publishUrlStr,
	         displayType,"SendingGETSTATUS",
	         "Get Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
             siteName,numOperations,typeString,
		     "srmStatusOfGetRequest","Failed",dailyTest, 
			 numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus",
		  result.getReturnStatus().getStatusCode().toString(), rrs,
		  explanation, timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
		  "Failed",dailyTest, numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
     }
    }
    else {
      rrs.setActualResult("");
      rrs.setMatchingInfo("No. expecting SRM_FILE_LIFETIME_EXPIRED");
      rrs.setLinkName("statuslifetimeexpired");  
      try {
        util.webScript(publishUrlStr,
	         displayType,"SendingGETSTATUS",
	         "Get Status Request now for expired pin life time", rrs,
		     "", timeStamp, fullTimeStamp, startTimeStamp, 
		     operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"srmStatusOfGetRequest",
		     "Failed",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
	      displayType,"GetStatus","SRM returned null status", rrs,
	      "SRM returned null status", 
		  timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmStatusOfGetRequest",
		  "Failed",dailyTest, numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
    }
   }
   catch(Exception me) {
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
// checkGetStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TGetRequestFileStatus checkGetStatus
    (ISRM srm, URI url, SrmPrepareToGetResponse response, 
     StringBuffer rCode, boolean statusTest, StringBuffer expBuffer, 
     IsWholeRequestFailed wholeRequestFailed)
      throws URI.MalformedURIException, java.rmi.RemoteException {

    String token = response.getRequestToken();
    if(token == null)  {
      throw new java.rmi.RemoteException("server sent null request token.");
    }

    Date d = new Date ();
    String operatorTimeStamp = d.toString();

    if(statusTest) {
      util.printMessage("\nSRM-TESTER: " + new Date() +
			" Calling GetStatus for requestToken=" + 
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
	       displayType,"Sending","Get Status Request now ...", rrs,
	  	   "", timeStamp, fullTimeStamp, servicePath,noPublish);
       }catch(Exception e) { e.printStackTrace(); }
      */
    }

    TGetRequestFileStatus fileStatus = null;

    try {
    SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
   
    r.setArrayOfSourceSURLs(util.formArrayOfAnyURI(url));
    r.setRequestToken(token);

    SrmStatusOfGetRequestResponse result = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(result,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,pingOverAllSuccess);
    timeOutCallBack.setRequestToken(token,srm);
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmStatusOfGetRequest(r);
      timeOutCallBack.setObject(result);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
       throw e;
    }

    if(result == null) {
      util.printMessage("\nSRM returned null result",logger,pIntf); 
      try {
        util.webScript(publishUrlStr,
	      displayType,"GetStatusMethod","Failed", null,
	      "SRM returned null status", 
		  timeStamp, fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,
		  "srmStatusOfGetRequest","Failed",dailyTest, numSites,
		  localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }
      return fileStatus;
    }

    util.printMessage("\nStatus from SRM (srmStatusOfGetRequest) " +
            result.getReturnStatus().getStatusCode(), logger,pIntf);
    response.setReturnStatus(result.getReturnStatus());
    String explanation = "";
    if(result.getReturnStatus().getExplanation() != null) {
       util.printMessage("Explanation=" + explanation, logger,pIntf);
       explanation = result.getReturnStatus().getExplanation();
       expBuffer.append(explanation);
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

    if(result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS || 
     result.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
     result.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED ||
     result.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS){ 
       ;
    }
    else {
       try {
         util.webScript(publishUrlStr,
 	       displayType,"GetStatusMethod","Failed", null,
	       explanation,
		   timeStamp, fullTimeStamp, startTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
           siteName,numOperations,typeString,
		   "srmStatusOfGetRequest","Failed",dailyTest, numSites,localPublish,bs,bsa);
       }catch(Exception e) { e.printStackTrace(); }
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
// printGetResult
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private HashMap printGetResult (SrmPrepareToGetResponse response, 
	StringBuffer responseStatus) {

   HashMap result = new HashMap ();

   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..." + new Date(),logger,pIntf);
     util.printMessage("+++\tGetRequest Response is null +++",logger,pIntf);
     util.printMessage("==========================================",logger,pIntf);
     return null;
   }

   util.printMessage("SRM-TESTER:   ...Output from SRM..." + new Date(),logger,pIntf);
   util.printMessage("==========================================",logger,pIntf);
   if(response.getRequestToken() != null) { 
     util.printMessage("request.token    : \t" + 
		response.getRequestToken(),logger,pIntf);
   }
   else {
     util.printMessage("request.token    : \t" + null,logger,pIntf);
   }

   Date d = new Date ();
   String operatorTimeStamp = d.toString();
   if(response.getReturnStatus() == null) {
     util.printMessage("request.state : \t" + null,logger,pIntf);
     responseStatus.append("Null request status");
     try {
        util.webScript(publishUrlStr,
          displayType,"Get","Failed", null,
          "SRM returned null return status",
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, 
		  servicePath,noPublish,
          siteName,numOperations,typeString,"srmPrepareToGet",
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
       if(response.getReturnStatus().getExplanation () != null) { 
         exp = response.getReturnStatus().getExplanation();
       }
       try {
          util.webScript(publishUrlStr,
          displayType,"Get",response.getReturnStatus().getStatusCode().toString(), 	
		  null, exp,
          timeStamp, fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations,typeString,"srmPrepareToGet",
          displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) {e.printStackTrace();}
     }
     else {

     util.printMessage("request.state : \t" + 
		response.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
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
      TGetRequestFileStatus fileStatus =
        response.getArrayOfFileStatuses().getStatusArray(i);
      if(fileStatus.getSourceSURL() != null) { 
        util.printMessage("\n\tSRM-TESTER: SURL="+
             fileStatus.getSourceSURL().toString(),logger,pIntf);
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

private String getExplanationFromResponse (SrmPrepareToGetResponse response)
{
   String exp="";
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..." + new Date(),logger,pIntf);
     util.printMessage("+++\tResponse is null +++",logger,pIntf);
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
      TGetRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
        int size = fileStatuses.length;

        for (int i = 0 ; i < size; i++) {
          TGetRequestFileStatus fileStatus =
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
// convertGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ReportRequestStatus convertGet ( SrmPrepareToGetResponse response, 
		String surl, String tturl, String turl, String fSize, String exp)
{
   if(response == null) {
     util.printMessage("SRM-TESTER:   ...Output from SRM..." + new Date(),logger,pIntf);
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
      TGetRequestFileStatus[] fileStatuses =
              response.getArrayOfFileStatuses().getStatusArray();
      if(fileStatuses != null) { 
        int size = fileStatuses.length;
        ReportRequestFileStatus [] reportFileStatus =
            new ReportRequestFileStatus[size];

        for (int i = 0 ; i < size; i++) {
          TGetRequestFileStatus fileStatus =
            response.getArrayOfFileStatuses().getStatusArray(i);
          ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
          rrfs.setSourceUrl(fileStatus.getSourceSURL().toString());
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
           rrfs.setExplanation(exp);
           if(!exp.equals("")) { 
             util.printMessage("Notes from tester" + exp, logger,null);
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
    StringBuffer explanationBuffer, 
	boolean detailedTest, int releaseTestCondition) throws Exception {

  org.apache.axis.types.URI uri = new org.apache.axis.types.URI (siteUrl);

  util.printMessageHL("\nSRM-TESTER: " +
		new Date() + " Calling ReleaseFile(Get) ...\n", logger,pIntf);
  //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

  if(requestToken != null) {
    SrmReleaseFilesRequest req = new SrmReleaseFilesRequest();
    //req.setKeepSpace(new Boolean(keepSpace));
    //req.setKeepSpace(new Boolean(true));
    req.setRequestToken(requestToken);
    req.setArrayOfSURLs(util.formArrayOfAnyURI(uri));

    if(!detailedTest) {
      util.printMessage("SRM-TESTER: ...Input parameters...",logger,pIntf);
      util.printMessage("RequestToken="+requestToken,logger,pIntf);
      util.printMessage("SURL="+siteUrl,logger,pIntf);
      util.printMessage("ExpectedResult=SRM_RELEASED",logger,pIntf);
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
			" Sending ReleaseFile Test for Bad SURL", logger,pIntf);
        util.printMessage("RequestToken="+requestToken,logger,pIntf);
        util.printMessage("Bad SURL="+siteUrl+"_bad_surl",logger,pIntf);
        util.printMessage("ExpectedResult=SRM_INVALID_PATH || SRM_FAILURE",logger,pIntf);
      }
      else if(currTestCondition == 0 && releaseTestCondition == 1) {

        util.printMessage("\nSRM-TESTER: " + new Date() +
			" Sending ReleaseFile Test for already released SURL", 
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
			" Sending ReleaseFile Test with doRemote(true)", logger,pIntf);
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
    timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    try {
      result = srm.srmReleaseFiles(req);
      timeOutCallBack.setObject(result);
    }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
       throw e;
    }
    util.printMessage("SRM-TESTER: ...Output from SRM..." + 
		new Date(),logger,pIntf);
    if(result != null) {
      util.printMessage("\tstatus="+
            result.getReturnStatus().getStatusCode().getValue(),logger,pIntf);
      util.printMessage("\texplanation="+
            result.getReturnStatus().getExplanation(),logger,pIntf);
        explanationBuffer.append(result.getReturnStatus().getExplanation());
      if(result.getArrayOfFileStatuses() != null) {
        TStatusCode temp = result.getArrayOfFileStatuses().getStatusArray(0).
			              getStatus().getStatusCode();
        statusBuffer.append(temp.getValue());
        util.printMessage("\tdetails="+ temp,logger,pIntf);
        String ttemp =
          result.getArrayOfFileStatuses().
				getStatusArray(0).getStatus().getExplanation();
        explanationBuffer.append(", FileExplanation="+ttemp);
        util.printMessage("\tdetails explanation="+ ttemp, logger,pIntf);
        util.printMessage("=================================\n",
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
      explanationBuffer.append("Notes from tester: Please contact SRM admin.");
      return "SRM returned null result";
    }
  }
  else {
     explanationBuffer.append
		("Notes from tester : Cannot do release file for null request token");
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
     util.printMessage("Cannot release space for null token", logger,pIntf);
     return;
   }

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
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        }
      }
     else {
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      }

     util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		servicePath,noPublish,
        siteName,numOperations,typeString,"srmReleaseSpace",
		"Sending",dailyTest, numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Sending", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
        siteName,numOperations,typeString,"srmReleaseSpace",
	    "Sending",dailyTest, numSites,localPublish,bs,bsa);
     }
     else {
       rrs.setActualResult("Null response from server");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      util.webScript(publishUrlStr,
       displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
       "Null response from the server ", timeStamp, fullTimeStamp, startTimeStamp, 
       operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
	   typeString,"srmReleaseSpace","Sending",dailyTest, numSites,localPublish,bs,bsa);
      rrs.setLinkName("releasespace_"+currTestCondition);
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Failed", rrs,
      "Null response from the server", timeStamp, fullTimeStamp, 
	  startTimeStamp, operatorTimeStamp, servicePath,noPublish,
      siteName,numOperations,typeString,"srmReleaseSpace",
	  "Sending",dailyTest, numSites,localPublish,bs,bsa);
     }
   }catch(Exception e) {
      e.printStackTrace();
      throw e;
    }
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
      String source = (String) ht.get("source");
      String temp = (String) ht.get("temp");
      String size = (String) ht.get("size");
      String turl = (String) ht.get("turl");
      String localTarget = (String) ht.get("localtarget");
      StringBuffer gsiFTPSTimeStamp = (StringBuffer)ht.get("starttimestamp");
      StringBuffer gsiFTPETimeStamp = (StringBuffer)ht.get("endtimestamp");
      String operatorTimeStamp = (String)ht.get("optimestamp");
      Boolean canGetContinue = (Boolean)ht.get("canGetContinue");
      OperOk isGetSuccess = (OperOk)ht.get("isGetSuccess");
      ISRM srm =  null ;
      Object obj = ht.get("srm");
      if(obj != null) {
        srm = (ISRM) obj;
      }
      StringBuffer expBuffer = (StringBuffer)ht.get("expbuffer");
      Boolean bb = (Boolean)ht.get("direct");
      boolean direct = bb.booleanValue();
      SrmPrepareToGetResponse response = null;
      obj = ht.get("response");
      if(obj != null) {
		response = (SrmPrepareToGetResponse)obj;
      }
      URI keyURL=null;
      obj = ht.get("keyurl");
      if(obj!= null) {
          keyURL = (URI) obj;
      }

      try {

              OperOk gsiftpOk = null;
              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               gsiftpOk = (OperOk) map.get("gsiftp-get");
               gsiftpOk.setTried(true);
             }

        gsiFTPETimeStamp = util.doOnlyGsiFTP(tsource,temp,gsiFTPSTimeStamp,
            publishUrlStr, displayType, timeStamp, 
	        fullTimeStamp, startTimeStamp, operatorTimeStamp, 
		    servicePath, credential, 
	        proxyFile, noPublish, onlyGsiFTPGet,defaultProxy, dcau,bufferSize,
            parallelism, siteName,numOperations,typeString,dailyTest,numSites, 
		    localPublish,bs,bsa,logger,pIntf,useGUC,gucScriptPath,tempScriptPath,gsiftpOk);

              if(isGetSuccess.getSubOperations().size() > 0 ) {
               HashMap map = isGetSuccess.getSubOperations();
               OperOk gOk = (OperOk) map.get("gsiftp-get");
               if(gOk.getExplanation().equals("")) {
                 gOk.isOperOk(true);
               }
             }

        int targetindex = temp.indexOf("file:////");
        File ff = null; 
        if(targetindex != -1) {
         ff = new File(temp.substring(targetindex+8));
        }
        else {
         ff = new File(temp);
        }

        if(ff.exists()) {
         long ssize = ff.length();
         if(ssize > 0) { 
         util.printMessage
  	      ("SRM-TESTER: Get succeeded, file copied to the target location\n",
				logger,pIntf);

         try {
           ReportRequestStatus rrs = 
				util.createAndConvertRequest(turl,temp,0,-1);
           rrs.setActualResult("SUCCESS");
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
             displayType,"GridFTP","", rrs, "",
             timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			 servicePath,noPublish,
             siteName,numOperations,typeString,
			 "GridFTP-Get","Ok",dailyTest, numSites,localPublish,bs,bsa);

           rrs = 
			util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,ssize);
           rrs.setLinkName("gsiftp_"+testCondition);
           util.webScript(publishUrlStr,
	 	    displayType,"GsiFTP","SUCCESS",rrs,
		    "", timeStamp, fullTimeStamp, startTimeStamp, 
            operatorTimeStamp,
		    servicePath,noPublish, siteName,numOperations,
			typeString,"GridFTP-Get","Ok",dailyTest, 
			numSites,localPublish,bs,bsa);
           }catch(Exception e) { e.printStackTrace(); }

            if(!direct) {
            if(currTestCondition == 5 && keyURL != null) {
               util.printMessage
			   ("\nWaiting here to do the advanced test for srmStatusOfGetRequest" +
                "\nfor condition wait for 120 seconds for the pin life time", 
				logger,pIntf); 
// AS 070809
// changed from 120000 to 300000
               Thread.sleep(statusWaitTime);
               checkGetStatusTest(srm,keyURL,response,4);
            }
           
           StringBuffer explanationBuffer = new StringBuffer();
           StringBuffer statusBuffer = new StringBuffer();

           try {
            if(advisoryDelete) {
            if(!detailed) {
             String statusMsg = doReleaseFile(srm, false,source,
		  	    response.getRequestToken(),statusBuffer,explanationBuffer,false,0); 
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
             HashMap map = isGetSuccess.getSubOperations();
             rmOk = (OperOk) map.get("srmrelease");
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
               ReportRequestStatus rrs = 
				util.createReportRequestStatus (source,"","");
               rrs.setMatchingInfo(statusMsg);
               rrs.setActualResult(statusBuffer.toString());
               rrs.setRequestId(response.getRequestToken());
               rrs.setLinkName("srmreleasebasic");

               util.webScript(publishUrlStr,
	             displayType,"SendingGETRelease","ReleaseFile", rrs,
		         "", timeStamp, fullTimeStamp, startTimeStamp, 
				operatorTimeStamp, servicePath,noPublish, siteName, numOperations,
				typeString,"srmReleaseFiles","Sending",dailyTest, numSites,
			    localPublish,bs,bsa);
               util.webScript(publishUrlStr,displayType,"GetReleaseFile",
				 statusBuffer.toString(), rrs, explanationBuffer.toString(), 
			     timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
				 servicePath,noPublish,
	             siteName, numOperations,typeString,"srmReleaseFiles",
			     displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
             }catch(Exception e)  {e.printStackTrace();}
           }//(!detailed)
           else {
             if(currTestCondition == 1) {
               String statusMsg = doReleaseFile(srm, false,source,
		  	      response.getRequestToken(),
				  statusBuffer,explanationBuffer,true,3); 
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
               HashMap map = isGetSuccess.getSubOperations();
               rmOk = (OperOk) map.get("srmrelease");
               if(rmOk != null) {
                 rmOk.setTried(true);  
                 if(displayStatusMsg.equals("Ok")) {
                   rmOk.isOperOk(true);
                 }
                 else {
                   rmOk.setExplanation(displayStatusMsg);
                 }
               }
               ReportRequestStatus rrs = 
					util.createReportRequestStatus (source,"","");
               rrs.setMatchingInfo(statusMsg);
               rrs.setActualResult(statusBuffer.toString());
               rrs.setRequestId(response.getRequestToken());
               rrs.setLinkName("srmreleasedoremote_"+currTestCondition);
               try {
                 util.webScript(publishUrlStr,
	              displayType,"SendingGETRelease",
					"ReleaseFile with doRemote(true)", rrs,
		            "", timeStamp, fullTimeStamp, startTimeStamp, 
				    operatorTimeStamp, servicePath,noPublish,
				    siteName, numOperations,typeString,"srmReleaseFiles",
					"Sending",dailyTest, numSites,localPublish,bs,bsa);
                 util.webScript(publishUrlStr,displayType,"GetReleaseFile",
				  statusBuffer.toString(), rrs, explanationBuffer.toString(), 
			      timeStamp,fullTimeStamp, startTimeStamp,  operatorTimeStamp,
				  servicePath,noPublish,
				  siteName, numOperations,typeString ,"srmReleaseFiles",
				  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
               }catch(Exception e)  {e.printStackTrace();}
             }
             else {
                String statusMsg = doReleaseFile(srm, false,source,
		  	      response.getRequestToken(),statusBuffer,
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
					util.createReportRequestStatus (source,"","");
                rrs.setMatchingInfo(statusMsg);
                rrs.setActualResult(statusBuffer.toString());
                rrs.setRequestId(response.getRequestToken());
                rrs.setLinkName("srmreleasebasic_"+currTestCondition);
                HashMap map = isGetSuccess.getSubOperations();
                rmOk = (OperOk) map.get("srmrelease");
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
	               displayType,"SendingGETRelease","ReleaseFile", rrs,
		           "", timeStamp, fullTimeStamp, startTimeStamp, 
			       operatorTimeStamp, servicePath,noPublish,
                   siteName, numOperations,typeString ,"srmReleaseFiles",
				   "Sending",dailyTest, numSites,localPublish,bs,bsa);
                 util.webScript(publishUrlStr,displayType,"GetReleaseFile",
				  statusBuffer.toString(),
                  rrs, explanationBuffer.toString(), 
			      timeStamp,fullTimeStamp, startTimeStamp, 
				  operatorTimeStamp, servicePath,noPublish,
	              siteName, numOperations,typeString,"srmReleaseFiles",
				  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
                }catch(Exception e)  {e.printStackTrace();}

                if(currTestCondition == 0) {
                  explanationBuffer = new StringBuffer();
                  statusBuffer = new StringBuffer();

                  statusMsg = doReleaseFile(srm, false,source,
		  	        response.getRequestToken(),statusBuffer,explanationBuffer,true,1); 
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
                  rrs = 
					util.createReportRequestStatus (source,"","");
                  rrs.setMatchingInfo(statusMsg);
                  rrs.setActualResult(statusBuffer.toString());
                  rrs.setRequestId(response.getRequestToken());
                  rrs.setLinkName
					("srmreleasealreadyreleasedsurl_"+currTestCondition);
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
	                displayType,"SendingGETRelease",
				        "ReleaseFile already released SURL", 
						rrs, "", timeStamp, fullTimeStamp, startTimeStamp, 
					    operatorTimeStamp, servicePath,noPublish,
                        siteName,numOperations,typeString,
						"srmReleaseFiles","Sending",dailyTest, numSites,
					    localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,
					   "GetReleaseFile",statusMsg,
                       rrs, explanationBuffer.toString(), 
			           timeStamp,fullTimeStamp, startTimeStamp, 
					   operatorTimeStamp, servicePath,noPublish,
                       siteName, numOperations,typeString,"srmReleaseFiles",
				       displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
                  }catch(Exception e)  {e.printStackTrace();}

                  explanationBuffer = new StringBuffer();
                  statusBuffer = new StringBuffer();
                  statusMsg = doReleaseFile(srm, false,source,
		  	        response.getRequestToken(),statusBuffer,explanationBuffer,true,2); 
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
                  rrs = 
					util.createReportRequestStatus (source+"_bad_surl","","");
                  rrs.setMatchingInfo(statusMsg);
                  rrs.setActualResult(statusBuffer.toString());
                  rrs.setRequestId(response.getRequestToken());
                  rrs.setLinkName("srmreleasebadsurl_"+currTestCondition);
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
	                displayType,"SendingGETRelease","ReleaseFile bad SURL", 
						rrs, "", timeStamp, fullTimeStamp, startTimeStamp, 
					    operatorTimeStamp, 
					    servicePath,noPublish, siteName, numOperations,
						typeString,"srmReleaseFiles","Sending",dailyTest, 
						numSites,localPublish,bs,bsa);
                   util.webScript(publishUrlStr,displayType,"GetReleaseFile",
						statusBuffer.toString(), rrs, explanationBuffer.toString(), 
			            timeStamp,fullTimeStamp, startTimeStamp, 
					    operatorTimeStamp, servicePath,
					    noPublish, siteName,numOperations,typeString,
						"srmReleaseFiles", displayStatusMsg,dailyTest, 
						numSites,localPublish,bs,bsa);
                  }catch(Exception e)  {e.printStackTrace();}
              }
            }
           }
          }
          try {
           util.webScript(publishUrlStr,
			displayType,"RequestSummary","SUCCESS",
			convertGet(response,"",turl,temp,""+ssize,""), "", 
			timeStamp, fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,typeStringForFunctionTest+"-OverAll",
			 "Ok",dailyTest, numSites,localPublish,bs,bsa);
          }catch(Exception e)  {e.printStackTrace();}
          isGetSuccess.isOperOk(true);
          doReleaseSpace(srm,spaceToken);
          if(sLock != null) {
             sLock.setIncrementCount();
          }
        }catch(Exception ge) {
          util.printMessage
	       (" SRM-TESTER: Releasefile failed " + 
		      explanationBuffer.toString() + " " + ge.getMessage(), logger,pIntf);
          try {
            util.webScript(publishUrlStr,
              displayType,"ReleaseFile","Failed",null,
              explanationBuffer.toString() + ge.getMessage(),
              timeStamp, fullTimeStamp, startTimeStamp, 
				 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,typeString,"srmReleaseFiles",
		         "Failed",dailyTest, numSites,localPublish,bs,bsa);
            util.webScript(publishUrlStr,
		 	  displayType,"RequestSummary","Failed during Releasefile",
			  convertGet(response,"",turl,temp,""+ssize,""), "", 
			  timeStamp, fullTimeStamp, startTimeStamp, 
			  operatorTimeStamp, servicePath,noPublish,
              siteName,numOperations,typeString,typeStringForFunctionTest+"-OverAll",
			  "Failed",dailyTest, numSites,localPublish,bs,bsa);
          }catch(Exception e) { e.printStackTrace();}
          isGetSuccess.setExplanation("Failed");
          doReleaseSpace(srm,spaceToken);
          if(sLock != null) {
           sLock.setIncrementCount();
          }
        }
       }
       else {
          if(sLock != null) {
           sLock.setIncrementCount();
          }
       }
       }
      }
      else {
        util.printMessage
	     ("Get File did not copied to the target location\n",logger,pIntf);
        util.printMessage("SRM-TESTER: Testing " + testType + 
		   " failed ", logger,pIntf);
        try {
          ReportRequestStatus rrs = 
			util.createAndConvertRequest(turl,temp,0,-1);
          rrs.setActualResult("FAILED");
          rrs.setLinkName("gsiftp_"+testCondition);
          util.webScript(publishUrlStr,
             displayType,"GridFTP","", rrs, "",
             timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
             siteName,numOperations,typeString,"GridFTP-Get",
			 "Failed",dailyTest, numSites,localPublish,bs,bsa);
           isGetSuccess.setExplanation("Failed");

          rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,0);
          rrs.setLinkName("gsiftp_"+testCondition);
          util.webScript(publishUrlStr,
			displayType,"GsiFTP","GSIFTP FAILED", rrs,
			"File from the TransferURL" +
		    "did not get copied to the TargetURL", 
		    timeStamp,fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,"GridFTP-Get",
			 "Failed",dailyTest, numSites,localPublish,bs,bsa);
          util.webScript(publishUrlStr,
			displayType,"RequestSummary","Failed during GsiFTP",
			 convertGet(null,"","",temp,size,expBuffer.toString()),
			 "", timeStamp,fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
             siteName,numOperations,typeString,typeStringForFunctionTest,
		     "Failed",dailyTest, numSites,localPublish,bs,bsa);
           canGetContinue = new Boolean(false);
           isGetSuccess.setExplanation("Failed");
        }catch(Exception  e) { e.printStackTrace(); }
        doReleaseSpace(srm,spaceToken);
        if(sLock != null) {
          sLock.setIncrementCount();
        }
      } 
    }catch(Exception ee) {
       util.printMessage("\n......................................",
			logger,pIntf);
       util.printMessage("GsiFTP failed with exception (3): " +
			ee.getMessage(),logger,pIntf);
       util.printMessage("......................................\n",
			logger,pIntf);
       try {
        ReportRequestStatus rrs = util.createAndConvertRequest(turl,temp,0,-1);
        rrs.setActualResult("FAILED");
        rrs.setLinkName("gsiftp_"+testCondition);
        util.webScript(publishUrlStr,
             displayType,"GridFTP","GsiFTP Failed with exception", rrs, ee.getMessage(),
             timeStamp, fullTimeStamp, startTimeStamp, 
			 operatorTimeStamp, servicePath,noPublish,
			 siteName, numOperations,typeString,"GridFTP-Get",
			 "Failed",dailyTest, numSites,localPublish,bs,bsa);
        rrs = util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,0);
        rrs.setLinkName("gsiftp_"+testCondition);
        util.webScript(publishUrlStr,
		  displayType,"GsiFTP","Failed duirng GsiFTP",rrs,
		  ee.getMessage(),timeStamp,fullTimeStamp, startTimeStamp, 
            operatorTimeStamp,
			servicePath,noPublish,
            siteName,numOperations,typeString,"GridFTP-Get",
			"Failed",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
		  displayType,"RequestSummary","Failed during GsiFTP",
			convertGet(null,"","",localTarget,size,expBuffer.toString()),
			"",timeStamp,fullTimeStamp, startTimeStamp, 
			operatorTimeStamp, servicePath,noPublish,
            siteName,numOperations,typeString,typeStringForFunctionTest,
		    "Failed",dailyTest, numSites,localPublish,bs,bsa);
        canGetContinue = new Boolean(false);
        isGetSuccess.isOperOk(false);
       }catch(Exception  e) { e.printStackTrace(); }
       util.printMessage("Exception " + ee.getMessage(),logger,pIntf);
       try {
         doReleaseSpace(srm,spaceToken);
       }catch(Exception rexp) {
         util.printMessage("Exception " + rexp.getMessage(),logger,pIntf);
       }
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

