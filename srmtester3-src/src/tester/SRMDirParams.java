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

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMDirParams
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMDirParams {

  private Object request;
  private Log logger;
  private String proxyString="";
  private PrintIntf pIntf; 

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor --- SRMDirParams
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMDirParams (Object request, Log logger, 
		StringBuffer proxyStrBuf, PrintIntf pIntf) {  
  this.request = request;
  this.logger = logger;
  proxyString = proxyStrBuf.toString();
  this.pIntf = pIntf;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMkdirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doMkdirTest(String uid,String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmMkdirRequest mkdirRequest = (SrmMkdirRequest) request;
  if(!uid.equals("")) {
    mkdirRequest.setAuthorizationID(uid);
  } 
  mkdirRequest.setSURL(new URI(surl));
  addStorageSystemInfo(surl,mkdirRequest,"mkdir");
  rrs.setUserId(uid);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMkdirStorageTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doMkdirStorageTest(String uid,
  String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  } 
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmMkdirRequest mkdirRequest = (SrmMkdirRequest) request;
  if(!uid.equals("")) {
    mkdirRequest.setAuthorizationID(uid);
  }
  mkdirRequest.setSURL(new URI(surl));
  addStorageSystemInfo(surl,mkdirRequest,"mkdir");

  rrs.setUserId(uid);
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRmTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmRmTest(String uid,String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmRmRequest srmRmRequest = (SrmRmRequest) request;
  if(!uid.equals("")) {
    srmRmRequest.setAuthorizationID(uid);
  }
  ArrayOfAnyURI anyURIArray = new ArrayOfAnyURI();
  URI[] uArray = new URI[1]; 
  uArray[0] = new URI(surl);
  anyURIArray.setUrlArray(uArray);
  srmRmRequest.setArrayOfSURLs(anyURIArray);
  addStorageSystemInfo(surl,srmRmRequest,"rm");
  rrs.setUserId(uid);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmRmStorageTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmRmStorageTest(String uid,
  String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 


  SrmRmRequest srmRmRequest = (SrmRmRequest) request;
  if(!uid.equals("")) {
    srmRmRequest.setAuthorizationID(uid);
  }
  ArrayOfAnyURI anyURIArray = new ArrayOfAnyURI();
  URI[] uArray = new URI[1]; 
  uArray[0] = new URI(surl);
  anyURIArray.setUrlArray(uArray);
  srmRmRequest.setArrayOfSURLs(anyURIArray);
  addStorageSystemInfo(surl,srmRmRequest,"rm");
  rrs.setUserId(uid);
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doRmdirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doRmdirTest(String uid,String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmRmdirRequest rmdirRequest = (SrmRmdirRequest) request;
  if(!uid.equals("")) {
    rmdirRequest.setAuthorizationID(uid);
  }
  rmdirRequest.setSURL(new URI(surl));
  addStorageSystemInfo(surl,rmdirRequest,"rmdir");
  rrs.setUserId(uid);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doRmdirTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doRmdirTestRecursive(String uid,String surl, boolean recursive,
	ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 
  util.printMessage("Recursive="+recursive, logger,pIntf); 

  SrmRmdirRequest rmdirRequest = (SrmRmdirRequest) request;
  if(!uid.equals("")) {
    rmdirRequest.setAuthorizationID(uid);
  }
  rmdirRequest.setSURL(new URI(surl));
  if(recursive) {
    rmdirRequest.setRecursive(new Boolean(true));
  }
  else {
    rmdirRequest.setRecursive(new Boolean(false));
  }
  addStorageSystemInfo(surl,rmdirRequest,"rmdir");
  rrs.setUserId(uid);
  rrs.setRecursive(""+recursive);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doRmdirStorageTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doRmdirStorageTest(String uid,
  String surl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmRmdirRequest rmdirRequest = (SrmRmdirRequest) request;
  if(!uid.equals("")) {
    rmdirRequest.setAuthorizationID(uid);
  }
  rmdirRequest.setSURL(new URI(surl));
  addStorageSystemInfo(surl,rmdirRequest,"rmdir");

  rrs.setUserId(uid);
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMvTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doMvTest(String uid,String fromsurl, String tosurl, 
	ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("From SURL= "+fromsurl,logger,pIntf); 
  util.printMessage("To SURL= "+tosurl,logger,pIntf); 

  SrmMvRequest mvRequest = (SrmMvRequest) request;
  if(!uid.equals("")) {
    mvRequest.setAuthorizationID(uid);
  }
  mvRequest.setFromSURL(new URI(fromsurl));
  mvRequest.setToSURL(new URI(tosurl));
  addStorageSystemInfo(fromsurl,mvRequest,"mv");
  rrs.setUserId(uid);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doMvStorageTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doMvStorageTest(String uid,  
	String fromsurl, String tosurl, ReportRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("From SURL= "+fromsurl,logger,pIntf); 
  util.printMessage("To SURL= "+tosurl,logger,pIntf); 

  SrmMvRequest mvRequest = (SrmMvRequest) request;
  if(!uid.equals("")) {
    mvRequest.setAuthorizationID(uid);
  }
  mvRequest.setFromSURL(new URI(fromsurl));
  mvRequest.setToSURL(new URI(tosurl));
  addStorageSystemInfo(fromsurl,mvRequest,"mv");

  rrs.setUserId(uid);
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLsTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmLsTest(String uid,String surl, BrowseRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  rrs.setUserId(uid);
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLsFullDetailedListTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmLsFullDetailedListTest
	(String uid,String surl, BrowseRequestStatus rrs, int numLevels) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("FullDetailedList="+true, logger,pIntf);
  util.printMessage("NumOfLevels="+numLevels, logger,pIntf);
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  srmlsRequest.setFullDetailedList(new Boolean(true));
  srmlsRequest.setNumOfLevels(new Integer(numLevels));
  rrs.setUserId(uid);
  rrs.setFullDetailedList("true");
  rrs.setNumOfLevels(""+numLevels);
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  util.printMessage("============================================",logger,pIntf);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doSrmLsNumOfLevelsTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void doSrmLsNumOfLevelsTest
	(String uid,String surl, BrowseRequestStatus rrs, int numLevels) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("NumOfLevels="+numLevels, logger,pIntf);
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  srmlsRequest.setNumOfLevels(new Integer(numLevels));
  rrs.setUserId(uid);
  rrs.setNumOfLevels(""+numLevels);
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  util.printMessage("============================================",logger,pIntf);
}

public void doSrmLsAllLevelRecursiveTest
	(String uid,String surl, BrowseRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("AllLevelsRecursive=true", logger,pIntf);
  util.printMessage("SURL= "+surl,logger,pIntf); 

  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  srmlsRequest.setAllLevelRecursive(new Boolean(true));
  rrs.setUserId(uid);
  rrs.setAllLevelRecursive("true");
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  util.printMessage("============================================",logger,pIntf);
}

public void doSrmLsFileStorageTypeTest
	(String uid,String surl, BrowseRequestStatus rrs, 
		String fileStorageType) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 
  util.printMessage("FileStorageType="+fileStorageType,logger,pIntf); 


  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  if(fileStorageType.equalsIgnoreCase("volatile")) {
    srmlsRequest.setFileStorageType(TFileStorageType.VOLATILE);
  }
  else if(fileStorageType.equalsIgnoreCase("durable")) {
    srmlsRequest.setFileStorageType(TFileStorageType.DURABLE);
  }
  else if(fileStorageType.equalsIgnoreCase("permanent")) {
    srmlsRequest.setFileStorageType(TFileStorageType.PERMANENT);
  }
  rrs.setUserId(uid);
  rrs.setFileStorageType(fileStorageType);
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  util.printMessage("============================================",logger,pIntf);
}

private void addStorageSystemInfo (String surl, Object request, String key) {
   int idx = surl.indexOf("mss://");
   if(idx != -1) {
     util.printMessage("\n\nUseStorageSystemInfo=true",logger,pIntf);
     ArrayOfTExtraInfo storageInfoArray = new ArrayOfTExtraInfo();
     TExtraInfo[] a_tExtra = new TExtraInfo[2];
     TExtraInfo tExtra = new TExtraInfo();
     tExtra.setKey("uid");
     tExtra.setValue(key);
     a_tExtra[0] = tExtra;
 
     tExtra = new TExtraInfo();
     tExtra.setKey("pwd");
     tExtra.setValue(this.proxyString);
     a_tExtra[1] = tExtra;
 
     storageInfoArray.setExtraInfoArray(a_tExtra);
     if(request instanceof SrmLsRequest) {
        SrmLsRequest r = (SrmLsRequest)request;
        r.setStorageSystemInfo(storageInfoArray);
        util.printMessage("FileStorageType=PERMANENT",logger,pIntf);
        r.setFileStorageType(TFileStorageType.PERMANENT);
     }
     if(request instanceof SrmMvRequest) {
        SrmMvRequest r = (SrmMvRequest)request;
        r.setStorageSystemInfo(storageInfoArray);
        //util.printMessage("FileStorageType=PERMANENT",logger);
        //r.setFileStorageType(TFileStorageType.PERMANENT);
     }
     if(request instanceof SrmRmdirRequest) {
        SrmRmdirRequest r = (SrmRmdirRequest)request;
        r.setStorageSystemInfo(storageInfoArray);
        //util.printMessage("FileStorageType=PERMANENT",logger);
        //r.setFileStorageType(TFileStorageType.PERMANENT);
     }
     if(request instanceof SrmRmRequest) {
        SrmRmRequest r = (SrmRmRequest)request;
        r.setStorageSystemInfo(storageInfoArray);
        //util.printMessage("FileStorageType=PERMANENT",logger);
        //r.setFileStorageType(TFileStorageType.PERMANENT);
     }
     if(request instanceof SrmMkdirRequest) {
        SrmMkdirRequest r = (SrmMkdirRequest)request;
        r.setStorageSystemInfo(storageInfoArray);
        util.printMessage("FileStorageType=PERMANENT",logger,pIntf);
        //r.setFileStorageType(TFileStorageType.PERMANENT);
     }
   }
}

public void doSrmLsStorageSystemInfoTest
	(String uid,  String surl, BrowseRequestStatus rrs) throws Exception {

  util.printMessage("SRM-TESTER:   ....Input parameters ...",logger,pIntf);
  util.printMessage("============================================",logger,pIntf);
  if(!uid.equals("")) {
    util.printMessage("AuthorizationID="+uid, logger,pIntf); 
  }
  util.printMessage("SURL= "+surl,logger,pIntf); 


  SrmLsRequest srmlsRequest = (SrmLsRequest) request;
  if(!uid.equals("")) {
    srmlsRequest.setAuthorizationID(uid);
  }
  addStorageSystemInfo(surl,srmlsRequest,"ls");
  ArrayOfAnyURI curr = new ArrayOfAnyURI();
  URI[] urlArray = new URI[1];
  urlArray[0] = new URI(surl); 
  curr.setUrlArray(urlArray);
  srmlsRequest.setArrayOfSURLs(curr);
  util.printMessage("============================================",logger,pIntf);

  rrs.setUserId(uid);
  rrs.setStorageSystemInfo("uid="+uid+",pwd=proxyString");
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMkdirRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SrmMkdirRequest getMkdirRequest() {
  return (SrmMkdirRequest) request;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRmdirRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SrmRmdirRequest getRmdirRequest() {
  return (SrmRmdirRequest) request;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMvRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SrmMvRequest getMvRequest() {
  return (SrmMvRequest) request;
}

public SrmLsRequest getLsRequest() {
  return (SrmLsRequest) request;
}

}
