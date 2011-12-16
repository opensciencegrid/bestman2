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

public class SRMCopy {

private SrmCopyRequest request;
private Log logger;
private String proxyString="";
private PrintIntf pIntf;
private boolean overwrite;
private boolean dcau;
private int bufferSize;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMCopy (SrmCopyRequest request, Log logger, 
	boolean overwrite, boolean dcau, int bufferSize,
	StringBuffer proxyStrBuf, PrintIntf pIntf) {  
  this.request = request;
  this.logger = logger;
  this.proxyString = proxyStrBuf.toString();
  this.pIntf = pIntf;
  this.overwrite = overwrite;
  this.dcau = dcau;
  this.bufferSize = bufferSize;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// testBasic
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void testBasic(String surl, String turl, ReportRequestStatus rrs,
		TFileStorageType fileStorageType) throws Exception {
  util.printMessage("SRM-TESTER:   ...Input parameters...",logger,pIntf);
    util.printMessage("============================================",logger,pIntf);
  util.printMessage("SURL="+surl,logger,pIntf); 
  util.printMessage("TURL="+turl,logger,pIntf); 
  util.printMessage("Overwrite="+this.overwrite, logger,pIntf);

  TCopyFileRequest[] fileReqList = new TCopyFileRequest [1];
  TCopyFileRequest curr = new TCopyFileRequest();

  if(fileStorageType != null) {
     util.printMessage("FileStorageType="+fileStorageType, logger, pIntf);
     request.setTargetFileStorageType(fileStorageType);
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


  curr.setSourceSURL(new URI(surl));
  curr.setTargetSURL(new URI(turl));

  fileReqList[0] = curr;
  ArrayOfTCopyFileRequest arrayOfCopy = new ArrayOfTCopyFileRequest();
  arrayOfCopy.setRequestArray(fileReqList);
  request.setArrayOfFileRequests(arrayOfCopy);
  addStorageSystemInfo(surl,turl,request);
  
  ReportRequestFileStatus[] fileStatuses = new ReportRequestFileStatus[1];
  ReportRequestFileStatus fileStatus = new ReportRequestFileStatus();
  fileStatus.setSourceUrl(surl);
  fileStatus.setTargetUrl(turl);
  fileStatuses[0] = fileStatus;

  if(this.overwrite) {
    request.setOverwriteOption(TOverwriteMode.ALWAYS);
  }
  else {
    request.setOverwriteOption(TOverwriteMode.NEVER);
  }

  rrs.setReportRequestFileStatus(fileStatuses);
  rrs.setOverwrite(""+this.overwrite);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestBasic
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestBasic(String surl, String turl, 
	ReportRequestStatus rrs,TFileStorageType fileStorageType) throws Exception {

   testBasic(surl,turl,rrs,fileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doLocalSRMCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doLocalSRMCopy(String surl, String turl, ReportRequestStatus rrs)
   throws Exception {

   testBasic(surl,turl,rrs,null);
   util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestUserRequestDesc
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestUserRequestDesc(String surl, String turl, 
	ReportRequestStatus rrs, String userDesc) throws Exception {
   
   testBasic(surl,turl,rrs,null);
   request.setUserRequestDescription(userDesc);
   rrs.setUserDescription(userDesc);
   util.printMessage("UserDesc : " + userDesc, logger,pIntf);
   util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestOverwrite
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestOverwrite(String surl, String turl,
  ReportRequestStatus rrs, boolean overwrite) throws Exception {
  testBasic(surl,turl,rrs,null);
    
  if(overwrite) {
    request.setOverwriteOption(TOverwriteMode.ALWAYS);
  }
  else {
    request.setOverwriteOption(TOverwriteMode.NEVER);
  }
  rrs.setOverwrite(""+overwrite);
  util.printMessage("Overwrite " + overwrite, logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestDesiredTotalRequestTime
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestDesiredTotalRequestTime(String surl, String turl,
  ReportRequestStatus rrs, int totalTime) throws Exception {
   testBasic(surl,turl,rrs,null);
   request.setDesiredTotalRequestTime(new Integer(totalTime)); 
   rrs.setDesiredTotalRequestTime(""+totalTime);
   util.printMessage("TotalTime " + totalTime, logger,pIntf);
   util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestDesiredTargetSURLLifeTime
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestDesiredTargetSURLLifeTime(String surl, String turl, 
  ReportRequestStatus rrs, int surlLifeTime) throws Exception {
  testBasic(surl,turl,rrs,null);
  request.setDesiredTargetSURLLifeTime(new Integer(surlLifeTime)); 
  rrs.setTargetSURLTime(""+surlLifeTime);
  util.printMessage("TargetSURLLifeTime " + surlLifeTime, logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestFileStorageType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestFileStorageType(String surl, String turl, 
	ReportRequestStatus rrs, TFileStorageType fileStorageType)  throws Exception {
 testBasic(surl,turl,rrs,null);
 if(fileStorageType == TFileStorageType.VOLATILE) {
   rrs.setFileStorageType("VOLATILE");
 }
 else if(fileStorageType == TFileStorageType.DURABLE) {
   rrs.setFileStorageType("DURABLE");
 }
 else if(fileStorageType == TFileStorageType.PERMANENT) {
   rrs.setFileStorageType("PERMANENT");
 }
 request.setTargetFileStorageType(fileStorageType);
 util.printMessage("FileStorageType " + fileStorageType, logger,pIntf);
 util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestRetentionPolicy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestRetentionPolicy(String surl, String turl, ReportRequestStatus rrs) 
	throws Exception {

  testBasic(surl,turl,rrs,null);
  TRetentionPolicyInfo retentionPolicyInfo = new TRetentionPolicyInfo();
  retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
  util.printMessage("RetentionPolicy=REPLICA", logger,pIntf);
  retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
  util.printMessage("AccessLatency=ONLINE", logger,pIntf);
  request.setTargetFileRetentionPolicyInfo(retentionPolicyInfo);
  rrs.setRetentionPolicy("REPLICA");
  rrs.setAccessLatency("ONLINE");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestStorageParam
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doTestStorageParam(String surl, String turl, 
	ReportRequestStatus rrs, String uid) throws Exception {
  testBasic(surl,turl,rrs,null);
  if(!uid.equals("")) {
    request.setAuthorizationID(uid);
  }
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  rrs.setTargetStorageSystemInfo("uid="+uid+",pwd=proxyString");
}

private void addStorageSystemInfo 
	(String surl, String turl, SrmCopyRequest request) {

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

   int idx = surl.indexOf("mss://");
   if(idx != -1) {
     util.printMessage("UseSourceStorageSystemInfo=true",logger,pIntf);
     tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue("copy");
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
  request.setSourceStorageSystemInfo(storageInfoArray);
  
  ArrayOfTExtraInfo targetStorageInfoArray = new ArrayOfTExtraInfo();
  vec = new Vector();
  tExtra = new TExtraInfo();
  tExtra.setKey("dcau");
  tExtra.setValue(""+dcau);
  vec.addElement(tExtra);

  tExtra = new TExtraInfo();
  tExtra.setKey("bufferSize");
  tExtra.setValue(""+bufferSize);
  vec.addElement(tExtra);

  idx = turl.indexOf("mss://");
  if(idx != -1) {
     util.printMessage("UseTargetStorageSystemInfo=true",logger,pIntf);
     tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue("copy");
     vec.addElement(tExtra);
 
     tExtra = new TExtraInfo();
     tExtra.setKey("pwd");
     tExtra.setValue(this.proxyString);
     vec.addElement(tExtra);
 
  }
  a_tExtra = new TExtraInfo[vec.size()];
  for(int i = 0; i < vec.size(); i++) { 
     a_tExtra[i] = (TExtraInfo)vec.elementAt(i);
  }
  targetStorageInfoArray.setExtraInfoArray(a_tExtra);
  request.setTargetStorageSystemInfo(targetStorageInfoArray);
  
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SrmCopyRequest getRequest() {
    return request;
}

}
