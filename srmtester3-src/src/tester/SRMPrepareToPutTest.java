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

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.client.intf.PrintIntf;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import gov.lbl.srm.server.TSRMUtil;

import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;
import java.util.Vector;


public class SRMPrepareToPutTest {

  private SrmPrepareToPutRequest request;
  private Log logger;
  private String proxyString="";
  private PrintIntf pIntf;
  private boolean overwrite;
  private boolean dcau;
  private int bufferSize;

  public SRMPrepareToPutTest (SrmPrepareToPutRequest request, 
	Log logger, StringBuffer proxyStrBuf, boolean overwrite,
	boolean dcau, int bufferSize, PrintIntf pIntf) {  
    this.request = request;
    this.logger = logger;
    this.proxyString = proxyStrBuf.toString();
    this.pIntf = pIntf;
    this.overwrite = overwrite;
    this.dcau = dcau;
    this.bufferSize = bufferSize;
  }

  public void doTestSimple(String userDesc, ReportRequestStatus rrs) 
		throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    request.setUserRequestDescription(userDesc);
    rrs.setUserDescription(userDesc);
    includeTransferParameters("",request,rrs);
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestSimple_1(String surl, String userDesc, 
		long fSize, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    includeTransferParameters(surl,request,rrs);
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize); 
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestOverwrite
	(String surl, String userDesc, long fSize, 
		ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    util.printMessage("Overwrite="+overwrite, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);
    rrs.setOverwrite(""+this.overwrite); 
    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    if(this.overwrite) {
      request.setOverwriteOption(TOverwriteMode.ALWAYS);
    }
    else {
      request.setOverwriteOption(TOverwriteMode.NEVER);
    }
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    includeTransferParameters(surl,request,rrs);
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestStorageType
	(String surl, String userDesc, TFileStorageType fileStorageType, 
		long fSize, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    util.printMessage("StorageType="+fileStorageType.getValue(), logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);
    if(fileStorageType == TFileStorageType.VOLATILE) {
      rrs.setFileStorageType("VOLATILE");
    }
    else if(fileStorageType == TFileStorageType.DURABLE) {
      rrs.setFileStorageType("DURABLE");
    }
    else if(fileStorageType == TFileStorageType.PERMANENT) {
      rrs.setFileStorageType("PERMANENT");
    }
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    request.setDesiredFileStorageType(fileStorageType);
    includeTransferParameters(surl,request,rrs);
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestRequestTime
	(String surl, String userDesc, int totalRequestTime, int pinTime, 
			int fileLifeTime, long fSize, ReportRequestStatus rrs) 
		       throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }
    if(totalRequestTime != 0) {
      util.printMessage("TotalRequestTime="+totalRequestTime, logger,pIntf); 
    }
    if(pinTime != 0) {
      util.printMessage("PinTime="+pinTime, logger,pIntf); 
    }
    if(fileLifeTime != 0) {
      util.printMessage("FileLifeTime="+fileLifeTime, logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    if(totalRequestTime != 0) {
      request.setDesiredTotalRequestTime(new Integer(10));
      rrs.setDesiredTotalRequestTime(""+10);
    }
    if(pinTime != 0) {
      request.setDesiredPinLifeTime(new Integer(10));
      rrs.setDesiredPinLifeTime(""+10);
    }
    if(fileLifeTime != 0) {
      request.setDesiredFileLifeTime(new Integer(10));
      rrs.setDesiredFileLifeTime(""+10);
    }
    includeTransferParameters(surl,request,rrs);
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestStorageParam(String surl, String userDesc,
    String uid , long fSize, ReportRequestStatus rrs) 
			throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(!uid.equals("")) {
      util.printMessage("AuthorizationID="+uid, logger,pIntf); 
    }
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    if(!uid.equals("")) {
      request.setAuthorizationID(uid);
    }

    includeTransferParameters(surl,request,rrs);
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);
    rrs.setUserId(uid);
    rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  }

  public void doTestTransferParam(String surl, String userDesc,
	 long fSize, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);

    TTransferParameters transferParameters = new TTransferParameters();

    ArrayOfString protocolsArray = new ArrayOfString();
    String [] protocols = new String[4];
    protocols[0]="gsiftp";
    protocols[1]="http";
    protocols[2]="https";
    protocols[3]="ftp";


    String tt = "";
    for(int i = 0; i < protocols.length;i++) {
      if(i == (protocols.length-1)) {
        tt = tt+protocols[i]; 
      }
      else {
        tt = tt+protocols[i]+","; 
      }
    }

    util.printMessage("Protocols="+tt, logger,pIntf); 
    protocolsArray.setStringArray(protocols);
    transferParameters.setArrayOfTransferProtocols(protocolsArray);
    transferParameters.setAccessPattern(TAccessPattern.TRANSFER_MODE);
    util.printMessage("AccessPattern=TRANSFERMODE", logger,pIntf); 
    transferParameters.setConnectionType(TConnectionType.WAN);
    util.printMessage("ConnectionType=WAN", logger,pIntf); 
    request.setTransferParameters(transferParameters);
    rrs.setTransferParametersProtocol(tt);
    rrs.setTransferParametersAccessPattern("TRANSFERMODE");
    rrs.setTransferParametersConnectionType("WAN");
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestRetentionPolicy(String surl, String userDesc,
     long fSize, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));
    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    } 
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);

    TRetentionPolicyInfo retentionPolicyInfo = new TRetentionPolicyInfo();
    retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
    util.printMessage("RetentionPolicy=REPLICA", logger,pIntf); 
    retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
    util.printMessage("AccessLatency=ONLINE", logger,pIntf); 
    includeTransferParameters(surl,request,rrs);
    request.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
    rrs.setUserDescription(userDesc);
    rrs.setFileSize(""+fSize);
    rrs.setRetentionPolicy("REPLICA");
    rrs.setAccessLatency("ONLINE");
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTestBasic(String surl, long fSize, ReportRequestStatus rrs,
		TFileStorageType fileStorageType, int remoteFileLifeTime) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));

    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    }
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;
   

    if(remoteFileLifeTime != 0) {
      util.printMessage("RemoteFileLifeTime="+remoteFileLifeTime, logger, pIntf);
      request.setDesiredFileLifeTime(new Integer(remoteFileLifeTime));
      rrs.setDesiredFileLifeTime(""+remoteFileLifeTime);
    }

    if(fileStorageType != null) {
     util.printMessage("FileStorageType="+fileStorageType, logger, pIntf);
     request.setDesiredFileStorageType(fileStorageType);
     if(fileStorageType == TFileStorageType.VOLATILE) {
       rrs.setFileStorageType("VOLATILE");
     }
     else if(fileStorageType == TFileStorageType.DURABLE) {
       rrs.setFileStorageType("DURABLE");
     }
     else if(fileStorageType == TFileStorageType.PERMANENT) {
      rrs.setFileStorageType("PERMANENT");
     }
   }

    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    includeTransferParameters(surl,request,rrs);
    rrs.setFileSize(""+fSize);
    util.printMessage("============================================\n",logger,pIntf);
  }

   public void doReserveSpacePut(String surl, long fSize, String targetSpaceToken,
		ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("TargetSpaceToken="+targetSpaceToken,logger,pIntf);
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));

    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    }
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;

    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setTargetSpaceToken(targetSpaceToken);
    includeTransferParameters(surl,request,rrs);
    rrs.setFileSize(""+fSize);
    rrs.setSpaceToken(targetSpaceToken);
    util.printMessage("============================================\n",logger,pIntf);
  }

  public void doTest3partycopy(String surl, long fSize, ReportRequestStatus rrs) 
		throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters ...",logger,pIntf);
    util.printMessage("============================================\n",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    if(fSize > 0) {
      util.printMessage("FileSize="+fSize, logger,pIntf); 
    }
    else {
      util.printMessage("FileSize="+null, logger,pIntf); 
    }
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
    TPutFileRequest curr = new TPutFileRequest();

    curr.setTargetSURL(new URI(surl));

    if(fSize > 0) {
      //UnsignedLong ulong = new UnsignedLong();
      //ulong.setValue(fSize);
      UnsignedLong ulong = new UnsignedLong(fSize);
      curr.setExpectedFileSize(ulong);
    }
    else {
      curr.setExpectedFileSize(null);
    }
    fileReqList[0] = curr;

    ArrayOfTPutFileRequest arrayOfFileRequest = new ArrayOfTPutFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    includeTransferParameters(surl,request,rrs);
    rrs.setFileSize(""+fSize);
    //rrs.setOverwrite(""+true); 
    //request.setOverwriteOption(TOverwriteMode.ALWAYS);
    util.printMessage("============================================\n",logger,pIntf);
  }

   private void includeTransferParameters(String surl, SrmPrepareToPutRequest request,
		ReportRequestStatus rrs) {
    TTransferParameters transferParameters = new TTransferParameters();

    ArrayOfString protocolsArray = new ArrayOfString();
    String [] protocols = new String[4];
    protocols[0]="gsiftp";
    protocols[1]="http";
    protocols[2]="https";
    protocols[3]="ftp";



    String tt = "";
    for(int i = 0; i < protocols.length;i++) {
      tt = tt+protocols[i]+",";
    }

    util.printMessage("Protocols="+tt, logger,pIntf);
    protocolsArray.setStringArray(protocols);
    transferParameters.setArrayOfTransferProtocols(protocolsArray);
    request.setTransferParameters(transferParameters);
    rrs.setTransferParametersProtocol(tt);
    if(this.overwrite) {
      request.setOverwriteOption(TOverwriteMode.ALWAYS);
      rrs.setOverwrite(""+this.overwrite);
    }
    else {
      request.setOverwriteOption(TOverwriteMode.NEVER);
    }
    util.printMessage("Overwrite="+this.overwrite,logger,pIntf);
    addStorageSystemParameter(surl,request);
  }

  private void addStorageSystemParameter(String surl,
        SrmPrepareToPutRequest request) {
 
     int idx = surl.indexOf("mss://");
 
     ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
     Vector vec = new Vector();

     TExtraInfo tExtra = new TExtraInfo();
     tExtra.setKey("dcau");
     tExtra.setValue(""+dcau);
     vec.addElement(tExtra);
     util.printMessage("DCAU="+dcau,logger,pIntf);

     tExtra = new TExtraInfo();
     tExtra.setKey("bufferSize");
     tExtra.setValue(""+this.bufferSize);
     vec.addElement(tExtra);
     util.printMessage("BufferSize="+bufferSize,logger,pIntf);

     if(idx != -1) {
      util.printMessage("UseStorageSystemParameter=true",logger,pIntf);
 
      tExtra = new TExtraInfo();
      tExtra.setKey("uid");
      tExtra.setValue("put");
      vec.addElement(tExtra);
 
      tExtra = new TExtraInfo();
      tExtra.setKey("pwd");
      tExtra.setValue(this.proxyString);
      vec.addElement(tExtra);
      util.printMessage("FileStorageType=PERMANENT",logger,pIntf);
      request.setDesiredFileStorageType(TFileStorageType.PERMANENT);
     }
     TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];

     for(int i = 0; i < vec.size(); i++) {
       a_tExtra[i] = (TExtraInfo) vec.elementAt(i);
     }
     request.setStorageSystemInfo(storageInfoArray);
     storageInfoArray.setExtraInfoArray(a_tExtra);
  }

  public SrmPrepareToPutRequest getRequest() {
    return request;
  }
}
