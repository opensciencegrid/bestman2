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
// Class SRMSpaceTest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMSpaceTest
{
   private String publishUrlStr;
   private String displayType;
   private String testType;
   private String timeStamp;
   private String fullTimeStamp;
   private String servicePath;
   private String spaceToken="";
   private GSSCredential credential;
   private String proxyFile;
   private boolean noPublish;
   private boolean detailed;
   private boolean defaultProxy;
   private int currTestCondition;
   private Log logger;
   private PrintIntf pIntf;
   private String typeString="SrmSpace";
   private boolean dailyTest;
   private String startTimeStamp;
   private int numSites;
   private int retryTimeAllowed;
   private String siteName="";
   private int numOperations;
   private boolean localPublish;
   private BufferedWriter bs;
   private BufferedWriter bsa;
   private TimeOutCallBack timeOutCallBack=null;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMSpaceTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMSpaceTest(String publishUrlStr, String displayType,
       String testType, String timeStamp, String fullTimeStamp, 
	   String servicePath, GSSCredential credential, String proxyFile, 
	   boolean noPublish, boolean detailed, boolean defaultProxy, 
       String siteName, int numOperations,
       String startTimeStamp, boolean dailyTest, int numSites,
	   int retryTimeAllowed, boolean localPublish,
	   BufferedWriter bs, BufferedWriter bsa,
	   Log logger, PrintIntf pIntf) {

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
   this.dailyTest = dailyTest;
   this.startTimeStamp = startTimeStamp;
   this.numSites = numSites;
   this.retryTimeAllowed = retryTimeAllowed;
   this.logger = logger;
   this.pIntf = pIntf;
   this.localPublish = localPublish;
   this.bs = bs;
   this.bsa = bsa;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReserveSpaceTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doReserveSpaceTest(ISRM srm, StringBuffer proxyString, String uid,
    SharedObjectLock sLock, int testCondition) throws  Exception {

   util.printMessage("\nSRM-TESTER: " + new Date() +
		" Calling ReserveSpace Test", logger,pIntf);
   currTestCondition = testCondition;
   ReportRequestStatus rrs = util.createReportRequestStatus("","","");
   SrmReserveSpaceRequest spaceRequest = new SrmReserveSpaceRequest();
   util.printMessage("\nSRM-TESTER: ...Input parameters...", logger,pIntf);
   if(!uid.equals("")) {
     spaceRequest.setAuthorizationID(uid);
   }
   spaceRequest.setUserSpaceTokenDescription("test description");
   spaceRequest.setDesiredSizeOfTotalSpace(new UnsignedLong(100000000));
   spaceRequest.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(1000));
   spaceRequest.setDesiredLifetimeOfReservedSpace(new Integer(10000));

   TRetentionPolicyInfo retentionPolicyInfo = new TRetentionPolicyInfo();
   retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
   //retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);

   spaceRequest.setRetentionPolicyInfo(retentionPolicyInfo);

   SrmReserveSpaceResponse spaceResponse = null;
   int ssTimeStamp = util.startTimeStamp();
   timeOutCallBack = new TimeOutCallBack(spaceResponse,ssTimeStamp);
               timeOutCallBack.setSharedObjectLock(sLock);
   timeOutCallBack.start();

   try {
      spaceResponse = srm.srmReserveSpace(spaceRequest);
      timeOutCallBack.setObject(spaceResponse);
   }catch(Exception e) {
      timeOutCallBack.interruptNow(true);
      throw e;
   }

   util.printMessage("\nSRM-TESTER: ...Ouput from SRM...", logger,pIntf);

   rrs.setLinkName("reservespace_"+testCondition);
   util.printMessage("\nSRM-TESTER: UID " + uid,logger,pIntf);
   rrs.setUserId(uid);
   rrs.setUserDescription("test description");
   rrs.setTotalSpace(""+1000);
   rrs.setGuarnSpace(""+1000);

   if(spaceResponse != null) {
      if(spaceResponse.getReturnStatus() != null) {
        rrs.setActualResult(
			spaceResponse.getReturnStatus().getStatusCode().getValue());
        util.printMessage ("StatusCode " + 
			spaceResponse.getReturnStatus().getStatusCode(),logger,pIntf);
        spaceToken = spaceResponse.getSpaceToken();

        if(spaceResponse.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_SUCCESS ||
           spaceResponse.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_DONE) {
           rrs.setMatchingInfo("Yes");
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        }

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
  
    Date d = new Date ();
    String operatorTimeStamp = d.toString();

    util.webScript(publishUrlStr,
      displayType,"SendingRESERVESPACE","Sending ReserveSpace", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
      siteName,numOperations,"SrmReserveSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);
     rrs.setLinkName("reservespace_"+testCondition);
    util.webScript(publishUrlStr,
      displayType,"ReserveSpace","ReserveSpace", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
      siteName,numOperations,"SrmReserveSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);

    if(sLock != null) {
      sLock.setIncrementCount();
    }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReserveSpaceTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doReleaseSpaceTest(ISRM srm, StringBuffer proxyString, String uid,
    String sToken, SharedObjectLock sLock, int testCondition) throws  Exception {

    Date d = new Date ();
    String operatorTimeStamp = d.toString();

    currTestCondition = testCondition;
    if(!sToken.equals("")) {

      if(sToken == null || sToken.equals("")) {
        util.printMessage("Cannot release space for null token", logger,pIntf);
        return;
      }
      try {
       util.printMessage("\n++++ Doing ReleaseSpace now ...++++", logger,pIntf);
       SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
       if(!uid.equals("")) {
         req.setAuthorizationID(uid);
       }
       req.setSpaceToken(sToken);

       SrmReleaseSpaceResponse response = null;
       int ssTimeStamp = util.startTimeStamp();
       timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
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
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
         }
       }
       else {
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
       }

       util.webScript(publishUrlStr,
        displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
        "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,"SrmReleaseSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);
        rrs.setLinkName("releasespace_"+currTestCondition);
       util.webScript(publishUrlStr,
        displayType,"ReleaseSpace","ReleaseSpace", rrs,
        "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,"SrmReleaseSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);
      }
      else {
       rrs.setActualResult("Null response from server");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
       util.webScript(publishUrlStr,
        displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
        "Null response from the server ", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,"SrmReleaseSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);
        rrs.setLinkName("releasespace_"+currTestCondition);
       util.webScript(publishUrlStr,
        displayType,"ReleaseSpace","ReleaseSpace", rrs,
        "Null response from the server", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
        siteName,numOperations,"SrmReleaseSpace","","Sending",dailyTest,numSites,localPublish,bs,bsa);
      }
     }catch(Exception e) {
        e.printStackTrace();
        throw e;
     }
   }
}

}
