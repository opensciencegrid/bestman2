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

import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

import java.util.Vector;

public class SRMPrepareToBringOnLineTest {

  private SrmBringOnlineRequest request;
  private Log logger;
  private String proxyString="";
  private PrintIntf pIntf;
  private int bufferSize;
  private boolean dcau;

  public SRMPrepareToBringOnLineTest (SrmBringOnlineRequest request, 
		Log logger, StringBuffer proxyStrBuf, int bufferSize, boolean dcau,
		PrintIntf pIntf) {  
    this.request = request;
    this.logger = logger;
    this.proxyString = proxyStrBuf.toString();
    this.bufferSize = bufferSize;
    this.dcau =dcau;
    this.pIntf = pIntf;
  }

  public void doTestSimple(String surl, String userDesc,
      ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    rrs.setUserDescription(userDesc);
    includeTransferParameters(surl,request);
  }

  public void doTestStorageType
	(String surl, String userDesc, TFileStorageType fileStorageType, 
		ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    util.printMessage("StorageType="+fileStorageType.getValue(), logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    request.setDesiredFileStorageType(fileStorageType);
    rrs.setUserDescription(userDesc);
    if(fileStorageType == TFileStorageType.VOLATILE) {
      rrs.setFileStorageType("VOLATILE");
    }
    else if(fileStorageType == TFileStorageType.DURABLE) {
      rrs.setFileStorageType("DURABLE");
    }
    else if(fileStorageType == TFileStorageType.PERMANENT) {
      rrs.setFileStorageType("PERMANENT");
    }
    includeTransferParameters(surl,request);
  }

  public void doTestRequestTime (String surl, String userDesc, 
		int totalRequestTime, int pinTime, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }
    if(totalRequestTime != 0) {
      util.printMessage("TotalRequestTime="+totalRequestTime, logger,pIntf); 
    }
    if(pinTime != 0) {
      util.printMessage("PinTime="+pinTime, logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    rrs.setUserDescription(userDesc);
    if(totalRequestTime != 0) {
      request.setDesiredTotalRequestTime(new Integer(10));
      //rrs.setDesiredTotalRequestTime(""+new Integer(10));
    }
    /*
    if(pinTime != 0) {
      request.setDesiredPinLifeTime(new Integer(10));
      rrs.setDesiredPinLifeTime(""+new Integer(10));
    }
    */
    includeTransferParameters(surl,request);
  }

  public void doTestStorageParam(String surl, String userDesc,
    StringBuffer proxyString, String uid, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);
    if(!uid.equals("")) {
      request.setAuthorizationID(uid);
    }
    util.printMessage("UserId="+uid, logger,pIntf); 



    includeTransferParameters(surl,request);
    util.printMessage("============================================",logger,pIntf);
    rrs.setUserDescription(userDesc);
    rrs.setUserId(uid);
  }

  public void doTestTransferParam(String surl, String userDesc,
     ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);

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

    transferParameters.setAccessPattern(TAccessPattern.TRANSFER_MODE);
    util.printMessage("AccessPattern=TRANSFERMODE", logger,pIntf); 
    transferParameters.setConnectionType(TConnectionType.WAN);
    util.printMessage("ConnectionType=WAN", logger,pIntf); 
    util.printMessage("============================================",logger,pIntf);
    request.setTransferParameters(transferParameters);
    rrs.setUserDescription(userDesc);
    rrs.setTransferParametersProtocol(tt);
    rrs.setTransferParametersAccessPattern("TRANSFERMODE");
    rrs.setTransferParametersConnectionType("WAN");
  }

  public void doTestRetentionPolicy(String surl, 
		String userDesc, ReportRequestStatus rrs) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("UserRequestDescription="+userDesc, logger,pIntf); 
    if(rrs.getSpaceToken() != null && !rrs.getSpaceToken().equals("")) {
      util.printMessage("SpaceToken="+rrs.getSpaceToken(), logger,pIntf); 
    }

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setUserRequestDescription(userDesc);

    TRetentionPolicyInfo retentionPolicyInfo = new TRetentionPolicyInfo();
    retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
    util.printMessage("RetentionPolicy=REPLICA", logger,pIntf); 
    retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
    util.printMessage("AccessLatency=ONLINE", logger,pIntf); 
    includeTransferParameters(surl,request);
    util.printMessage("============================================",logger,pIntf);
    request.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
    rrs.setUserDescription(userDesc);
    rrs.setRetentionPolicy("REPLICA");
    rrs.setAccessLatency("ONLINE");
  }

  public void doReserveSpaceGet(String surl, String targetSpaceToken, ReportRequestStatus rrs) 
             throws Exception {
    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 
    util.printMessage("TargetSpaceToken="+targetSpaceToken,logger,pIntf);

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
    request.setTargetSpaceToken(targetSpaceToken);

    includeTransferParameters(surl,request);
    rrs.setSpaceToken(targetSpaceToken);
    util.printMessage("============================================",logger,pIntf);
  }

  public void doTestBasic(String surl, int pinTime) throws Exception {

    util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
    util.printMessage("SURL="+surl,logger,pIntf); 

    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
    TGetFileRequest curr = new TGetFileRequest();

    curr.setSourceSURL(new URI(surl));
    fileReqList[0] = curr;
    ArrayOfTGetFileRequest arrayOfFileRequest =
        new ArrayOfTGetFileRequest();
    arrayOfFileRequest.setRequestArray(fileReqList);
    request.setArrayOfFileRequests(arrayOfFileRequest);
    if(pinTime != 0) {
      request.setDesiredLifeTime(new Integer(pinTime));
      //rrs.setDesiredLifeTime(""+new Integer(pinTime));
    }
    //request.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));

    includeTransferParameters(surl,request);
    util.printMessage("============================================",logger,pIntf);
  }

  private void includeTransferParameters(String surl, SrmBringOnlineRequest request) {
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
    addStorageSystemParameter(surl,request);
  }

   private void addStorageSystemParameter(String surl,
        SrmBringOnlineRequest request) {

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
     tExtra.setValue(""+bufferSize);
     vec.addElement(tExtra);
     util.printMessage("BufferSize="+bufferSize,logger,pIntf);

     if(idx != -1) {
      util.printMessage("UseStorageSysemParameter=true",logger,pIntf);

      tExtra = new TExtraInfo();
      tExtra.setKey("uid");
      tExtra.setValue("get");
      vec.addElement(tExtra);

      tExtra = new TExtraInfo();
      tExtra.setKey("pwd");
      tExtra.setValue(this.proxyString);
      vec.addElement(tExtra);
     }

     TExtraInfo[] a_tExtra = new TExtraInfo[vec.size()];
     for(int i = 0; i < vec.size(); i++) {
       a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
     }
     storageInfoArray.setExtraInfoArray(a_tExtra);
     request.setStorageSystemInfo(storageInfoArray);
  }

  public SrmBringOnlineRequest getRequest() {
    return request;
  }
}
