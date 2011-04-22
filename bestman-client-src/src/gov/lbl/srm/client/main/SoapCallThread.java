/** 
 *
 * BeStMan Copyright (c) 2007-2008, The Regents of the University of California,
 * through Lawrence Berkeley National Laboratory (subject to receipt of any
 * required approvals from the U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this software
 * please contact Berkeley Lab's Technology Transfer Department at TTD@lbl.gov.
 *
 * NOTICE.  This software was developed under partial funding from the
 * U.S. Department of Energy.  As such, the U.S. Government has been
 * granted for itself and others acting on its behalf a paid-up,
 * nonexclusive, irrevocable, worldwide license in the Software to
 * reproduce, prepare derivative works, and perform publicly and
 * display publicly.  Beginning five (5) years after the date permission
 * to assert copyright is obtained from the U.S. Department of Energy,
 * and subject to any subsequent five (5) year renewals, the
 * U.S. Government is granted for itself and others acting on its
 * behalf a paid-up, nonexclusive, irrevocable, worldwide license in
 * the Software to reproduce, prepare derivative works, distribute
 * copies to the public, perform publicly and display publicly, and
 * to permit others to do so.
 *
 * Email questions to SRM@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 *
*/

package gov.lbl.srm.client.main;

import java.io.*;
import java.util.*;
import java.text.*;

import gov.lbl.srm.StorageResourceManager.*;

public class SoapCallThread extends Thread { 
  private ISRM _srm;
  private Object request;
  private Object response;
  private String callingMethodName;
  private boolean interrupt;
  private java.util.logging.Logger _theLogger;
  private boolean silent;
  private boolean useLog;
  private Vector inputVec = new Vector ();

 public SoapCallThread (ISRM srm, Object request, String callingMethodName) {
    this._srm = srm;
    this.request = request;
    this.callingMethodName = callingMethodName;
 }


 public Object getResponseObject() {
   return response;
 }

 public void setInterrupt(boolean b) {
   this.interrupt=b;
   this._srm = null;
 }

 public void setLogger(java.util.logging.Logger theLogger, 
                boolean silent, boolean useLog) {
   this._theLogger = theLogger;
   this.silent=silent;
   this.useLog = useLog;
 }

 public void run () {  
   try {
     int value = getMethodType();
     switch(value) {
       case 1 : 
              response = _srm.srmPrepareToPut((SrmPrepareToPutRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 2 : 
              response = _srm.srmAbortFiles((SrmAbortFilesRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 3 : 
              response = _srm.srmPutDone((SrmPutDoneRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 4 : 
              response = _srm.srmStatusOfPutRequest
                        ((SrmStatusOfPutRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 5 : 
              response = _srm.srmUpdateSpace
                        ((SrmUpdateSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 6 : 
              response = _srm.srmPrepareToGet
                        ((SrmPrepareToGetRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 7 : 
              response = _srm.srmGetRequestSummary
                        ((SrmGetRequestSummaryRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 8 : 
              response = _srm.srmCopy ((SrmCopyRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 9 : 
              response = _srm.srmBringOnline((SrmBringOnlineRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 10 : 
              response = _srm.srmPing((SrmPingRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 11 : 
              response = _srm.srmAbortRequest((SrmAbortRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 12 : 
              response = _srm.srmLs((SrmLsRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 13 : 
              response = _srm.srmMkdir((SrmMkdirRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 14 : 
              response = _srm.srmRmdir((SrmRmdirRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 15 : 
              response = _srm.srmMv((SrmMvRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 16 : 
              response = _srm.srmRm((SrmRmRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 17 : 
              response = _srm.srmSuspendRequest((SrmSuspendRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 18 : 
              response = _srm.srmResumeRequest((SrmResumeRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 19 : 
              response = 
                  _srm.srmGetRequestTokens((SrmGetRequestTokensRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 20 : 
              response = 
                 _srm.srmExtendFileLifeTime((SrmExtendFileLifeTimeRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 21 : 
              response = _srm.srmGetTransferProtocols
                        ((SrmGetTransferProtocolsRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 22 : 
              response = _srm.srmReleaseFiles ((SrmReleaseFilesRequest)request);
              if(_srm == null) 
                response = null;
              break;
       //case 23 : 
              //response = _srm.srmGetRequestSummary 
                        //((SrmGetRequestSummaryRequest)request);
              //if(_srm == null) 
                //response = null;
              //break;
       case 24 : 
              response = _srm.srmReserveSpace ((SrmReserveSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 25 : 
              response = _srm.srmReleaseSpace
                        ((SrmReleaseSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 26 : 
              response = _srm.srmChangeSpaceForFiles
                        ((SrmChangeSpaceForFilesRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 27 : 
              response = _srm.srmPurgeFromSpace
                        ((SrmPurgeFromSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 28 : 
              response = _srm.srmUpdateSpace
                        ((SrmUpdateSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 29 : 
              response = _srm.srmGetSpaceMetaData
                        ((SrmGetSpaceMetaDataRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 30 : 
              response = _srm.srmGetSpaceTokens
                        ((SrmGetSpaceTokensRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 31 : 
              response = _srm.srmCheckPermission
                        ((SrmCheckPermissionRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 32 : 
              response = _srm.srmSetPermission
                        ((SrmSetPermissionRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 33 : 
              response = _srm.srmGetPermission
                        ((SrmGetPermissionRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 34 : 
              response = _srm.srmExtendFileLifeTimeInSpace
                        ((SrmExtendFileLifeTimeInSpaceRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 35 : 
              response = _srm.srmStatusOfGetRequest
                        ((SrmStatusOfGetRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 36 : 
              response = _srm.srmStatusOfCopyRequest
                        ((SrmStatusOfCopyRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 37 : 
              response = _srm.srmStatusOfBringOnlineRequest
                        ((SrmStatusOfBringOnlineRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 38 : 
              response = _srm.srmStatusOfLsRequest
                        ((SrmStatusOfLsRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 39 : 
              response = _srm.srmStatusOfReserveSpaceRequest
                        ((SrmStatusOfReserveSpaceRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 40 : 
              response = _srm.srmStatusOfUpdateSpaceRequest
                        ((SrmStatusOfUpdateSpaceRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       case 41 : 
              response = _srm.srmStatusOfChangeSpaceForFilesRequest
                        ((SrmStatusOfChangeSpaceForFilesRequestRequest)request);
              if(_srm == null) 
                response = null;
              break;
       default : 
              System.out.println("SRM-CLIENT: Method type not defined " +
                        callingMethodName);
              inputVec.clear();
              inputVec.addElement("Method type not defined " + 
                        callingMethodName);
              util.printEventLog(_theLogger,"SoapCallThread",
                inputVec,silent,useLog);
     }
   }catch(Exception e) { 
     System.out.println("SoapCallThread.Exception="+e.getMessage()); 
     util.printEventLogException(_theLogger,"SoapCallThread.Exception",e);
     String msg = e.getMessage();
     int idx = msg.indexOf("Connection refused");
     int idx1 = msg.indexOf("Defective credential detected");
     int idx5 = msg.indexOf("org.globus.common.ChainedIOException");
     int idx6 = msg.indexOf("java.net.SocketTimeoutException: Read timed out");
     int idx7 = msg.indexOf("Connection timed out");
     int idx8 = msg.indexOf("java.net.SocketException: Connection reset");
     int idx9 = msg.indexOf("EOFException");
     int idx10 = msg.indexOf("Unauthorized");
     if(msg.startsWith("CGSI-gSOAP: Could not find mapping") ||
       idx != -1 || idx1 != -1 || idx5 != -1 || 
       idx6 != -1 || idx7 != -1 || idx8 != -1 || idx9 !=-1 || idx10 != -1) {
      if(idx != -1 || idx6 != -1) {
        inputVec.clear(); 
        inputVec.addElement("ExitStatus="+90);
        util.printEventLog(_theLogger,"ExitCodeStatus",inputVec,silent,useLog);
        System.exit(90);
      }
      else if(idx1 != -1 || idx5 != -1) {
        util.printMessage("\nException : proxy type mismatch, " +
	  "please check your proxy type",null,silent);
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
 }

 public int getMethodType() {
    if(callingMethodName.toLowerCase().equals("srmpreparetoput")) {
      return 1; 
    }
    else if(callingMethodName.toLowerCase().equals("srmabortfiles")) {
      return 2; 
    }
    else if(callingMethodName.toLowerCase().equals("srmputdone")) {
      return 3; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofput")) {
      return 4; 
    }
    else if(callingMethodName.toLowerCase().equals("srmupdatespace")) {
      return 5; 
    }
    else if(callingMethodName.toLowerCase().equals("srmpreparetoget")) {
      return 6; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgetrequestsummary")) {
      return 7; 
    }
    else if(callingMethodName.toLowerCase().equals("srmcopy")) {
      return 8; 
    }
    else if(callingMethodName.toLowerCase().equals("srmbringonline")) {
      return 9; 
    }
    else if(callingMethodName.toLowerCase().equals("srmping")) {
      return 10; 
    }
    else if(callingMethodName.toLowerCase().equals("srmabortrequest")) {
      return 11; 
    }
    else if(callingMethodName.toLowerCase().equals("srmls")) {
      return 12; 
    }
    else if(callingMethodName.toLowerCase().equals("srmmkdir")) {
      return 13; 
    }
    else if(callingMethodName.toLowerCase().equals("srmrmdir")) {
      return 14; 
    }
    else if(callingMethodName.toLowerCase().equals("srmmv")) {
      return 15; 
    }
    else if(callingMethodName.toLowerCase().equals("srmrm")) {
      return 16; 
    }
    else if(callingMethodName.toLowerCase().equals("srmsuspendrequest")) {
      return 17; 
    }
    else if(callingMethodName.toLowerCase().equals("srmresumerequest")) {
      return 18; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgetrequesttokens")) {
      return 19; 
    }
    else if(callingMethodName.toLowerCase().equals("srmextendfilelifetime")) {
      return 20; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgettransferprotocols")) {
      return 21; 
    }
    else if(callingMethodName.toLowerCase().equals("srmreleasefiles")) {
      return 22; 
    }
    //else if(callingMethodName.toLowerCase().equals("srmgetrequestsummary")) {
      //return 23; 
    //}
    else if(callingMethodName.toLowerCase().equals("srmreservespace")) {
      return 24; 
    }
    else if(callingMethodName.toLowerCase().equals("srmreleasespace")) {
      return 25; 
    }
    else if(callingMethodName.toLowerCase().equals("srmchangespace")) {
      return 26; 
    }
    else if(callingMethodName.toLowerCase().equals("srmpurgespace")) {
      return 27; 
    }
    else if(callingMethodName.toLowerCase().equals("srmupdatespace")) {
      return 28; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgetspacemetadata")) {
      return 29; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgetspacetokens")) {
      return 30; 
    }
    else if(callingMethodName.toLowerCase().equals("srmcheckpermission")) {
      return 31; 
    }
    else if(callingMethodName.toLowerCase().equals("srmsetpermission")) {
      return 32; 
    }
    else if(callingMethodName.toLowerCase().equals("srmgetpermission")) {
      return 33; 
    }
    else if
        (callingMethodName.toLowerCase().equals("srmextendfilelifetimeinspace")) {
      return 34; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofget")) {
      return 35; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofcopy")) {
      return 36; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofbringonline")) {
      return 37; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofls")) {
      return 38; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofreservespace")) {
      return 39; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofupdatespace")) {
      return 40; 
    }
    else if(callingMethodName.toLowerCase().equals("srmstatusofchangespace")) {
      return 41; 
    }
    return -1;
 }
}
