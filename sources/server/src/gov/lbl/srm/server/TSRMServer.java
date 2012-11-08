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

package gov.lbl.srm.server;
 
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.impl.*;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

import java.rmi.RemoteException;
import java.util.Vector;

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.*;


public class TSRMServer /*extends GridServiceImpl implements ISRMPortType*/{
    //private static TSRMLog _log = new TSRMLog();

    public TSRMLsManager _lsManager = new TSRMLsManager();
	
    //public static final TProcessingUnit _gProcessingUnit = new TProcessingUnitPDSF();
    public static TProcessingUnit _gProcessingUnit = null;

    public static String _hashedServerTag = null;
	public static String _compileDate = null;

    public void initTag() {
	String hi = System.currentTimeMillis()+" "+Config._wsdlEndPoint;
	java.util.zip.Adler32 checksum = new java.util.zip.Adler32();
	checksum.reset();
	checksum.update(hi.getBytes(), 0, hi.getBytes().length);
	long result = checksum.getValue();
	//int iresult=(int)(result & 0xFFFFFFFF);
	_hashedServerTag = ""+result;
    }

    public TSRMServer() {
	initTag();
	TSRMLog.info(TSRMServer.class, null, "name=LBNLSRM  JVMmaxbytes="+Runtime.getRuntime().maxMemory(),"hashedServerTag="+_hashedServerTag);

	if (Config._uploadQueueParameter == null) {
	    _gProcessingUnit = new TProcessingUnit();
	} else {
	    _gProcessingUnit = new TProcessingUnitUploadingQueue(Config._uploadQueueParameter);
	}

	new Thread(TSRMThreadSitter.getInstance()).start();
	new Thread(TSRMHeartbeater.getInstance()).start();
	    /*
	    String jarFileName   = TSRMUtil.getValueOf(Config._useProcessingClass, "jarFile=", '&', false);
	    String userClassName = TSRMUtil.getValueOf(Config._useProcessingClass, "name=", '&', false);
	    String plugInPath = Config._pluginDir + "/"+ jarFileName;	
	    System.out.println("=> looking in plug in path="+plugInPath);
	    System.out.println("   for classname="+userClassName);

	    try {
		Class implClass = TSRMUtil.loadClass(plugInPath, userClassName);
		java.lang.reflect.Constructor constructor = implClass.getConstructor((java.lang.Class [])null);
		_gProcessingUnit = (TProcessingUnit)constructor.newInstance((java.lang.Object []) null);

	    } catch (Exception e) {
		e.printStackTrace();
		System.exit(1);
	    }
	    */	
    }

    public static void checkMemoryUsage(String currMethodName) {
	
	TSRMLog.debug(TSRMServer.class, null, "method="+currMethodName, "JVMfreebytes="+Runtime.getRuntime().freeMemory());
	TSRMLog.debug(TSRMServer.class, null, "method="+currMethodName, "JVMtotalbytes="+Runtime.getRuntime().totalMemory());
	
	//Runtime.getRuntime().gc();
    }
  
    public Object checkUserValidity(String authorizationID, String currMethodName,  boolean getAccountIfNotFound) {
	TSRMLog.info(TSRMServer.class, currMethodName, "event=incoming", null);
	String uid = TSRMService.gGetUID(currMethodName);
    	
    	if (uid == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefMsgInvalidCredential);
    	}       
    	
	TAccount user = null;
	user = TAccountManager.getAccount(TAccount.markUp(uid, authorizationID), getAccountIfNotFound);	
    	
    	if (user == null) {
	    if (getAccountIfNotFound) {
		TSRMLog.error(TSRMServer.class, null, "method="+currMethodName, "msg=CreateAccount()_failed!");
		return TSRMUtil.createReturnStatusNotAuthorized("Authorization failure:"+TSRMUtil._DefMsgCreateAccountFailed);
	    } else {
		return TSRMUtil.createReturnStatusNotAuthorized(TSRMUtil._DefNoSuchUser+" Reason: caller never used this SRM");
	    }
    	}   

	return user;
    }

    //
    // requests that returns rightaway
    //

    public SrmPingResponse srmPing(SrmPingRequest req) throws java.rmi.RemoteException {       			
	//checkUserValidity(null, "srmPing", false); // this is commented out so can pass dn/mapped to the user
	//
	TSRMLog.info(TSRMServer.class, null, "event=incoming", "name=srmPing authorizationId="+req.getAuthorizationID());

	if (_compileDate == null) {
	    _compileDate = TSRMUtil.getCompiledDate(gov.lbl.srm.server.TSRMServer.class);
	    TSRMLog.info(this.getClass(), null, "compiledAt="+_compileDate, null);
	}

	int debugLevel = 0;
	if (req.getAuthorizationID() != null) {
	    if (req.getAuthorizationID().equalsIgnoreCase("admin")) {
		debugLevel = 1;
	    }
	}

	SrmPingResponse result = new SrmPingResponse();
	result.setVersionInfo("v2.3.0");

	Vector storageReport = new Vector();
	TSRMUtil.addVersionInfo(storageReport, _compileDate);	
	TSRMUtil.addCallerDN(storageReport);
	TSRMUtil.addStorageEntries(debugLevel, storageReport);
	
	String userDefinedTokenDisplay = TSRMUserSpaceTokenSupport.displayAll();
	if (!userDefinedTokenDisplay.equalsIgnoreCase("None.")) {
	    storageReport.add(TSRMUtil.createTExtraInfo("userDefinedTokens", userDefinedTokenDisplay));
	}

	if (Config._staticTokenList != null) {
	  for (int i=0; i<Config._staticTokenList.length; i++) {
	    StaticToken curr = ConfigGateway._staticTokenList[i];
	    storageReport.add(TSRMUtil.createTExtraInfo("staticToken("+i+")", curr.getID()+" desc="+curr.getDesc()+" size="+curr.getTotalBytes()));
	  }
	}

	TSRMUtil.addConfigEntries(debugLevel, storageReport);
	result.setOtherInfo(TSRMUtil.convertToArray(storageReport));
	    //int i=0; while (i < 1) {System.out.println("..waiting " + i +" minutes."+Thread.currentThread()); TSRMUtil.sleep(60000); i++;}
	return result;
    }

    public SrmGetTransferProtocolsResponse srmGetTransferProtocols(SrmGetTransferProtocolsRequest req)  
	throws java.rmi.RemoteException 
    {       	
	Object s = checkUserValidity(req.getAuthorizationID(), "srmGetTransferProtocols", true);	
	SrmGetTransferProtocolsResponse result = new SrmGetTransferProtocolsResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));

	/*TSupportedTransferProtocol temp = new TSupportedTransferProtocol();
        temp.setTransferProtocol("gsiftp");
	
	TSupportedTransferProtocol[] defaultArray = new TSupportedTransferProtocol[1];
	defaultArray[0] = temp;*/

	ArrayOfTSupportedTransferProtocol protocolInfo = new ArrayOfTSupportedTransferProtocol();
	protocolInfo.setProtocolArray(TSRMTxfProtocol.getSupportedProtocols());

	result.setProtocolInfo(protocolInfo);
	return result;
    }

    //
    // will not return null or throw exceptions to users
    // a status with msg is more friendly and meaningful
    //
    public SrmReserveSpaceResponse srmReserveSpace(SrmReserveSpaceRequest req) throws RemoteException { 
    	String currMethodName = "srmReserveSpace";		
	SrmReserveSpaceResponse result = new SrmReserveSpaceResponse();
		
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getRetentionPolicyInfo() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "Null retentionPolicyInfo"));
	    return result;
	} 

	/*
	if (req.getDesiredSizeOfGuaranteedSpace() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Null guaranteed space size"));
	    return result;
	}
    */

	TTokenAttributes input = null;
	try {
	    input = TSRMStorage.checkReservationRequest(req);
	} catch (TSRMException e) {
	    TSRMLog.error(TSRMServer.class, null, "method="+currMethodName, "msg=chcekReservationRequest()_failed!");
	    result.setReturnStatus(e.getReturnStatus(TSRMUtil._DefBadInput));
	    return result;
	}

	TReturnStatus status = user.requestSpace(input);
	input.print();	   

	String tokenAssigned = input.getTokenID();
	
	if (tokenAssigned != null) {
	    input.log();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	    result.setSpaceToken(tokenAssigned);
	    
	    result.setLifetimeOfReservedSpace(input.getLifeTimeObj());
	    result.setSizeOfTotalReservedSpace(input.getTotalSpaceByteObj());
	    result.setSizeOfGuaranteedReservedSpace(input.getGuaranteedSpaceByteObj());		
	} else {
	    // no token is assigned.
	    //result.setReturnStatus(TSRMUtil.createReturnStatus(code, TSRMUtil._DefInternalError));
	    result.setReturnStatus(status);
	}
	    
	return result;	
    }

    public SrmUpdateSpaceResponse srmUpdateSpace(SrmUpdateSpaceRequest req) {
    	String currMethodName = "srmUpdateSpace";
    	SrmUpdateSpaceResponse result = new SrmUpdateSpaceResponse();

	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	if (req.getSpaceToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));	
	    return result;
	}

    	TSRMStorage.iSRMSpaceToken token = user.getToken(req.getSpaceToken());
    	if (token == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
	    return result;
    	}
    	String errMsg = TSRMStorage.checkUpdateRequest(req);
    	if (errMsg != null) {
	    TSRMLog.error(TSRMServer.class, null, "method="+currMethodName, "msg=chcekUpdateRequest()_failed!");
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefBadInput+" "+errMsg));
    	} else {
	    //TStatusCode code = token.update(req);
	    //result.setReturnStatus(TSRMUtil.createReturnStatus(code, null));
	    result.setReturnStatus(token.update(req));
	    TStatusCode code = result.getReturnStatus().getStatusCode(); 
	    if ((code == TStatusCode.SRM_SUCCESS) ||(code == TStatusCode.SRM_LOWER_SPACE_GRANTED))
    	    {
		result.setLifetimeGranted(token.getTokenAttributes().getLifeTimeLeftObj(token.getCreationTimeInMilliSeconds()));
		result.setSizeOfGuaranteedSpace(token.getTokenAttributes().getGuaranteedSpaceByteObj());
		result.setSizeOfTotalSpace(token.getTokenAttributes().getTotalSpaceByteObj());
	    }
    	}
    	
    	return result;	
    }
    
    public SrmReleaseSpaceResponse srmReleaseSpace(SrmReleaseSpaceRequest req) {
    	String currMethodName = "srmReleaseSpace";
    	SrmReleaseSpaceResponse result = new SrmReleaseSpaceResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

    	if (req.getSpaceToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
    	}

    	TSRMStorage.iSRMSpaceToken token = user.getToken(req.getSpaceToken());
    	if (token == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchSpaceToken));
	    return result;
    	}
    	
	boolean tokenReleasedOK = false;

	Boolean forceFileRelease = req.getForceFileRelease();
	if (forceFileRelease == null) {
	    tokenReleasedOK = token.release(false);
	} else {
	    tokenReleasedOK = token.release(forceFileRelease.booleanValue());
	}
    	
	if (tokenReleasedOK) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	} else {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Has active files."));
	}
    	return result;
    }

    public SrmPrepareToGetResponse srmPrepareToGet(SrmPrepareToGetRequest req) { 
    	String currMethodName = "srmPrepareToGet";
    	SrmPrepareToGetResponse result = new SrmPrepareToGetResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getArrayOfFileRequests() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests received!"));	
	    return result;
    	}

    	TGetFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();    	
    	if ((listOfFileReqs == null) || (listOfFileReqs.length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       "No file requests received."));
	    return result;
    	}	
    
    	TUserRequestGet userRequest = new TUserRequestGet(req.getUserRequestDescription(), user);
	
	TReturnStatus storageConsistencyStatus = userRequest.checkStorageConsistency(req.getTargetSpaceToken(), 
										     req.getTargetFileRetentionPolicyInfo());
	if (storageConsistencyStatus != null) {
	    result.setReturnStatus(storageConsistencyStatus);
	    return result;
	}

	TReturnStatus typeConsistencyStatus = userRequest.checkFileTypeInconsistency(req.getTargetSpaceToken(),
										     req.getDesiredFileStorageType());
	if (typeConsistencyStatus != null) {
	    result.setReturnStatus(typeConsistencyStatus);
	    return result;
	}

	TSRMLog.info(TSRMServer.class, null, "event=list rid="+userRequest.getID()+" reqSize="+listOfFileReqs.length,
		      "fst="+req.getDesiredFileStorageType()+" tst="+req.getTargetSpaceToken()+"  rpi="+req.getTargetFileRetentionPolicyInfo()+
		      " lt="+req.getDesiredPinLifeTime());

	GSSCredential creds = TSRMService.gGetCredential(currMethodName); // temp
        userRequest.setGSSCredential(creds);
	userRequest.setRetryPeriod(req.getDesiredTotalRequestTime());
	if(req.getTransferParameters() != null) {
	    int numProtocolsTaken = userRequest.setTransferProtocols(req.getTransferParameters().getArrayOfTransferProtocols());
	    if (numProtocolsTaken == 0) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, 
								   "No transfer protocol specified by user is supported by SRM."));
		return result;
	    }
	}
	userRequest.setStorageSystemInfo(req.getStorageSystemInfo());
        
    	result.setRequestToken(userRequest.getID());
    	
    	TGetRequestFileStatus[] fileStatusList = new TGetRequestFileStatus[listOfFileReqs.length];
    		
	for (int i=0; i<listOfFileReqs.length; i++) {	    
	    TGetFileRequest curr = listOfFileReqs[i];
	    
	    try {
		TSRMFileInput input = new TSRMFileInput(req, curr, i);
		userRequest.verifyConsistency(input.getSpaceType(), input.getSpaceToken());
		TGetRequestFileStatus status = userRequest.addGetFileRequest(input);
		fileStatusList[i] = status;	    
	    } catch (TSRMException e) {
		TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+userRequest.getID(), "src="+curr.getSourceSURL());

		fileStatusList[i] =  new TGetRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    } catch (RuntimeException e) {
		TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+userRequest.getID(), "src="+curr.getSourceSURL());

		fileStatusList[i] =  new TGetRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    }
	}
	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<listOfFileReqs.length; i++) {
	     TGetRequestFileStatus status = fileStatusList[i];
	     TStatusCode code = status.getStatus().getStatusCode();
	     reqSummary.add(code);
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
    	
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
    	
	if ((reqSummary.getStatusCode() != TStatusCode.SRM_REQUEST_QUEUED) &&
	    (reqSummary.getStatusCode() != TStatusCode.SRM_REQUEST_INPROGRESS))
	{
	    userRequest.cleanMe();
	}
    	return result;
    }
    
    public SrmStatusOfGetRequestResponse srmStatusOfGetRequest(SrmStatusOfGetRequestRequest req) { 
	String currMethodName = "srmStatusOfGetRequest rid="+req.getRequestToken();    	
    	SrmStatusOfGetRequestResponse result = new SrmStatusOfGetRequestResponse();

    	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequestGet userRequest = (TUserRequestGet)(user.getRequest(req.getRequestToken(),
									TUserRequestGet.class));
    
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}

	if (userRequest.isTerminated()) {
	    result.setReturnStatus(userRequest.getReturnStatus());
	    userRequest.cleanMe();
	    return result;
	}
	
	URI[] SURLList = null;
	if (req.getArrayOfSourceSURLs() != null) {
	    SURLList = req.getArrayOfSourceSURLs().getUrlArray();
	}
    	
    	Vector status = userRequest.getStatusOfRequest(SURLList);
    	
    	if ((status == null) || (status.size() == 0)) {
	    // since user request has at least one file requests, so 
	    // if status == null or has 0 size, then there is no match for the SURLs provided
	    // hence the status is failure.
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "No matching file requests found!"));
	    return result;
    	} 
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	TGetRequestFileStatus[] fileStatusArray = new TGetRequestFileStatus[status.size()];
    	
    	for (int i=0; i<status.size(); i++) {
	    fileStatusArray[i] = (TGetRequestFileStatus)(status.get(i));
	    TStatusCode code   = fileStatusArray[i].getStatus().getStatusCode();
	    reqSummary.add(code);
    	}
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));
	//result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(userRequest.getWLCGStatus(), null));

	//
	// successful requests are immediately cleaned up when user calls releaseFile
	//
	if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) 	    
	{
	    userRequest.cleanMe();
	}

    	return result;
    }
    
 
    public SrmReleaseFilesResponse srmReleaseFiles(SrmReleaseFilesRequest req) { 
    	String currMethodName = "srmReleaseFiles";
    	SrmReleaseFilesResponse result = new SrmReleaseFilesResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	boolean willKeepSpace = true;
	
	if (req.getDoRemove() != null) {
	    willKeepSpace = !(req.getDoRemove().booleanValue());
	}	
	
	TSRMLog.debug(TSRMServer.class, null, "func=releaseFiles", "token="+req.getRequestToken()+" willKeepSpace="+willKeepSpace);

	if (req.getRequestToken() == null) {
	    if (req.getArrayOfSURLs().getUrlArray() == null) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "All inputs are null"));
		return result;
	    }
	    result.setArrayOfFileStatuses(TSRMUtil.convertToArray(user.releaseSuchSURLsInAllRequests(req.getArrayOfSURLs().getUrlArray(), 
												     willKeepSpace)));
	} else {
	    TUserRequest userReq = user.getRequest(req.getRequestToken());
	    if (userReq == null) {       
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "No such req."));
		return result;
	    } else if (!userReq.isReleasable()) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "Release is not applicable"));
		return result;
	    } else {		
		if ((req.getArrayOfSURLs() == null) || (req.getArrayOfSURLs().getUrlArray() == null) || 
		    (req.getArrayOfSURLs().getUrlArray().length == 0)) 
	        {				 
		    result.setArrayOfFileStatuses(TSRMUtil.convertToArray(userReq.release(null, willKeepSpace)));
		} else {
		    result.setArrayOfFileStatuses(TSRMUtil.convertToArray(userReq.release(req.getArrayOfSURLs().getUrlArray(),  
											  willKeepSpace)));
		}
		userReq.cleanMe(userReq.getWLCGStatus());		
	    }
	}

	if (result.getArrayOfFileStatuses().getStatusArray() == null) {
	    return result;
	}

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<result.getArrayOfFileStatuses().getStatusArray().length; i++) {
	    TStatusCode code = result.getArrayOfFileStatuses().getStatusArray(i).getStatus().getStatusCode();
	    reqSummary.add(code);
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));	
    	return result;
    }
    

    public SrmGetSpaceMetaDataResponse srmGetSpaceMetaData(SrmGetSpaceMetaDataRequest req) { 
    	String currMethodName = "srmGetSpaceMetaData";
    	SrmGetSpaceMetaDataResponse result = new SrmGetSpaceMetaDataResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    if (Config._staticTokenList == null) {
	        result.setReturnStatus(supportStatus);
		return result;
	    } else {
	      // go through static tokens
	    }
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;
    	 
	if (req.getArrayOfSpaceTokens() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No array of tokens provided"));
	    return result;
	}

    	String[] inputTokens = req.getArrayOfSpaceTokens().getStringArray();
	if (inputTokens == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}
				   
    	//Vector validInputTokens = new Vector();
	TMetaDataSpace[] metaDataArray = new TMetaDataSpace[inputTokens.length];
    	for (int i=0; i<inputTokens.length; i++) {
	    String curr = inputTokens[i];
	    metaDataArray[i] = new TMetaDataSpace();
	    metaDataArray[i].setSpaceToken(curr);

	    TSRMStorage.iSRMSpaceToken token = user.getToken(curr);
	    if (token == null) {
	        StaticToken t = StaticToken.find(Config._staticTokenList, curr);
		if (t == null) {
		  metaDataArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
		} else {
		  metaDataArray[i] = t.getMetadata();
		}
	    } else {
	        metaDataArray[i] = token.getMetaData();
	    }
    	}

    	result.setArrayOfSpaceDetails(TSRMUtil.convertToArray(metaDataArray));

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<metaDataArray.length; i++) {
	    //reqSummary.add(metaDataArray[i].getStatus().getStatusCode());
	    reqSummary.add(metaDataArray[i]);
	}
    	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
    	return result;
    }

    // sample inputs:
    // file://mylocalAccountInSRM/dir1
    // srm://srm.host.address[:port]/..&SFN=/mylocalAccountInSRM/dir1/..
    public SrmMkdirResponse srmMkdir(SrmMkdirRequest req) { 
    	String currMethodName = "srmMkdir";
    	SrmMkdirResponse result = new SrmMkdirResponse();
	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getSURL() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No path found in mkdir()"));
	    return result;
	}

	try {
	    result.setReturnStatus(user.makeDir(new TSURLInfo(req.getSURL(), req.getStorageSystemInfo())));
	} catch (TSRMException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	} catch (RuntimeException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	}
    	return result;
    }

    public SrmRmdirResponse srmRmdir(SrmRmdirRequest req) { 
    	String currMethodName = "srmRmdir";
    	SrmRmdirResponse result = new SrmRmdirResponse();

	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	try {
	    Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
	    if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
		result.setReturnStatus((TReturnStatus)s);
		return result;
	    }
	    TAccount user = (TAccount)s;

	    if (req.getSURL() == null) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No path found in rmdir()"));
		return result;
	    }
	    
	    boolean doRecursive = false;
	    if (req.getRecursive() != null) {
		doRecursive = req.getRecursive().booleanValue();
	    }
	    result.setReturnStatus(user.rmDir(new TSURLInfo(req.getSURL(), req.getStorageSystemInfo()), doRecursive));	    
	} catch (TSRMException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	} catch (RuntimeException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	}
	return result;
    }

    public SrmLsResponse srmLs(SrmLsRequest req) {
    	String currMethodName = "srmLs";
    	SrmLsResponse result = new SrmLsResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

    	if ((req.getArrayOfSURLs() == null) || (req.getArrayOfSURLs().getUrlArray() == null) || (req.getArrayOfSURLs().getUrlArray().length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefEmptyInputs));
	    return result;
    	}
	TSRMLog.debug(TSRMServer.class, null, "method="+currMethodName, "uid="+user.getID());
    	
    	//TSURLInfo[] inputPathArray = req.getArrayOfSURLs().getUrlArray()
	URI[] inputPathArray = req.getArrayOfSURLs().getUrlArray();
	
	TSRMLog.debug(TSRMServer.class, null, "event=bookkeeping_ls()"+" inputSize="+inputPathArray.length,
		     "storageType="+req.getFileStorageType());

	TSRMFileListingOption listingOp = new TSRMFileListingOption(req, user.getID());
	String err = listingOp.checkParameters();
	if (err != null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, err));
	    return result;
	}
	
	listingOp.printMe();

	TUserRequestLs lsReq = _lsManager.add(user, inputPathArray, listingOp, 
					      req.getFileStorageType(), req.getStorageSystemInfo());	
	GSSCredential creds = TSRMService.gGetCredential(currMethodName); // temp
    	lsReq.setGSSCredential(creds);

	//lsReq.setStorageSystemInfo(req.getStorageSystemInfo());			

	long napMilliSeconds = 5000;
	long totalNap = 0;
	long maxNap = 30000; // 0.5 minutes
	while (!lsReq.isDone()) {	
	    if (totalNap > maxNap) {
		result.setRequestToken(lsReq.getID());    	
	        TSRMLog.debug(TSRMServer.class, null, "event=CallLsStatusForResults  rid="+lsReq.getID(), "firstInput="+inputPathArray[0].toString());
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_INPROGRESS, 
								   "check later. rid="+lsReq.getID()));
		return result;
	    }
	    TSRMUtil.sleep(napMilliSeconds);
	    totalNap += napMilliSeconds;		
	}
	
	result = lsReq.getResult();		    
	_lsManager.removeWhenFinished(lsReq);
	return result;
    }

    public static String printRetentionPolicyInfo(TRetentionPolicyInfo info) {
	if (info == null) {
	    return "rpi=Null";
	   }
	String result = "";
	if (info.getAccessLatency() != null) {
	    result = "latency="+info.getAccessLatency().toString();
	} 
	if (info.getRetentionPolicy() != null) {
	    result += " retention="+info.getRetentionPolicy().toString();
	}
	return result;
    }

    public SrmMvResponse srmMv(SrmMvRequest req) { 
    	String currMethodName = "srmMv";
    	SrmMvResponse result = new SrmMvResponse();
    	    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getFromSURL() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No from path provided in mv()"));
	    return result;
	} else if (req.getToSURL() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No to path provided in mv()"));
	    return result;
	}

    	TSRMLog.debug(TSRMServer.class, null, "method="+currMethodName+" uid="+user.getID(), 
		     "from=\""+req.getFromSURL().toString()+"\" to=\""+req.getToSURL().toString()+"\"");
    	
	try {
	    TSURLInfo fromSurlInfo = new TSURLInfo(req.getFromSURL(), req.getStorageSystemInfo());
	    TSURLInfo toSurlInfo = new TSURLInfo(req.getToSURL(), req.getStorageSystemInfo());
	    
	    TSupportedURL fromurl = TSupportedURL.createAndCheckSiteFileName(fromSurlInfo, user, TSRMPermission.Writable);
	    
	    fromurl.useCredential(TSRMService.gGetCredential(currMethodName));
	    TReturnStatus state = fromurl.doMv(user, 
					       TSupportedURL.createAndCheckSiteFileName(toSurlInfo, user, TSRMPermission.Writable));
	    result.setReturnStatus(state);
	} catch (TSRMException e) {
	    TSRMLog.exception(this.getClass(), "srmMv()", e);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(e));
	} catch (RuntimeException e) {
	    TSRMLog.exception(this.getClass(), "srmMv()", e);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(e));
	}
	    
	return result;
    }    

    public SrmPrepareToPutResponse srmPrepareToPut(SrmPrepareToPutRequest req) { 
    	String currMethodName = "srmPrepareToPut";
    	SrmPrepareToPutResponse result = new SrmPrepareToPutResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;
    	
	if (req.getArrayOfFileRequests() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests received!"));	
	    return result;
    	}

    	TPutFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();
    	
    	if ((listOfFileReqs == null) || (listOfFileReqs.length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests received."));
	    return result;
    	}
	
	TUserRequestPut userRequest = new TUserRequestPut(req.getUserRequestDescription(), user);
	/*
	TReturnStatus storageConsistencyStatus = userRequest.checkStorageConsistency(req.getTargetSpaceToken(), 
										     req.getTargetFileRetentionPolicyInfo());

	if (storageConsistencyStatus != null) {
	    result.setReturnStatus(storageConsistencyStatus);
	    return result;
	}
	*/

	TReturnStatus typeConsistencyStatus = userRequest.checkFileTypeInconsistency(req.getTargetSpaceToken(),
										     req.getDesiredFileStorageType());
	if (typeConsistencyStatus != null) {
	    result.setReturnStatus(typeConsistencyStatus);
	    return result;
	}
	
	
	TSRMLog.info(TSRMServer.class, null, "event=list rid="+userRequest.getID()+" reqSize="+listOfFileReqs.length,
		     "overwrite="+req.getOverwriteOption()+" desiredFStype="+req.getDesiredFileStorageType()+" targetSpaceToken="+req.getTargetSpaceToken()+"  "+printRetentionPolicyInfo(req.getTargetFileRetentionPolicyInfo())+" txfLT="+req.getDesiredPinLifeTime()+" surlLT="+req.getDesiredFileLifeTime());
	GSSCredential creds = TSRMService.gGetCredential(currMethodName); // temp
    	userRequest.setGSSCredential(creds);

	if(req.getTransferParameters() != null) {
	     int numProtocolsTaken = userRequest.setTransferProtocols(req.getTransferParameters().getArrayOfTransferProtocols());
	     if (numProtocolsTaken == 0) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, 
								   "No transfer protocol specified by user is supported by SRM."));
		return result;
	    }
	}

	TPutRequestFileStatus[] fileStatusList = userRequest.add(req);
	if (fileStatusList == null) {	
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "file level requests are invalid."));
	    return result;
	}
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	reqSummary.collect(fileStatusList);
	
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));	    
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
	if (result.getReturnStatus().getStatusCode() != TStatusCode.SRM_FAILURE) {
	    result.setRequestToken(userRequest.getID());
	}
	
	if ((reqSummary.getStatusCode() != TStatusCode.SRM_REQUEST_QUEUED) &&
	    (reqSummary.getStatusCode() != TStatusCode.SRM_REQUEST_INPROGRESS) &&
	    (reqSummary.getStatusCode() != TStatusCode.SRM_SPACE_AVAILABLE)) 
	{
	    userRequest.cleanMe();
	}

    	return result;
    }
    
    public SrmPutDoneResponse srmPutDone(SrmPutDoneRequest req) { 
    	String currMethodName = "srmPutDone";
    	SrmPutDoneResponse result = new SrmPutDoneResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;
	
	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequest userRequest = user.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}

	if ((req.getArrayOfSURLs() == null) || 
	    (req.getArrayOfSURLs().getUrlArray() == null) || 
	    (req.getArrayOfSURLs().getUrlArray().length == 0)) 
	{
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests specified."));
	    return result;
	}

        URI[] SURLList = req.getArrayOfSURLs().getUrlArray();

    	TSURLReturnStatus[] urlStatusList = new TSURLReturnStatus[SURLList.length];    	
    	 
	TSRMLog.debug(this.getClass(), null, "rid="+req.getRequestToken(), "numFiles="+SURLList.length+" event=putDoneCalled");

    	for (int i=0; i<SURLList.length; i++) {
	    URI curr = SURLList[i];
	    urlStatusList[i] = new TSURLReturnStatus();
	    urlStatusList[i].setSurl(curr);
    		
	    TSRMRequestPut r = (TSRMRequestPut)(userRequest.get(curr));
	    if (r == null) {
		urlStatusList[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, null));
	    } else {
		if (r.isAborted()) {
		    urlStatusList[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_ABORTED, null));
		} else {
		    urlStatusList[i].setStatus(r.putDone());
		}
	    }
    	}
	
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(urlStatusList));

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<urlStatusList.length; i++) {
	    reqSummary.add(urlStatusList[i].getStatus().getStatusCode());
	}
    	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));

	// if file requests are all finished, clean up
	//but causes null error at TSRMRequestPut:591
	//TSRMUtil.sleep(3500); //  for requests to finish waiting on user input
	//userRequest.cleanMe(userRequest.getWLCGStatus());
	    
    	return result;
    }
     
    public SrmStatusOfPutRequestResponse srmStatusOfPutRequest(SrmStatusOfPutRequestRequest req) { 
    	String currMethodName = "srmStatusOfPutRequest rid="+req.getRequestToken();
    	SrmStatusOfPutRequestResponse result = new SrmStatusOfPutRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;
    	
	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}
	TUserRequestPut userRequest = (TUserRequestPut)(user.getRequest(req.getRequestToken(), 
									TUserRequestPut.class));
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
	if (userRequest.isTerminated()) {
	    result.setReturnStatus(userRequest.getReturnStatus());
	    userRequest.cleanMe();
	    return result;
	}
	
    	URI[] SURLList = null;
	if (req.getArrayOfTargetSURLs() != null) {
	    SURLList = req.getArrayOfTargetSURLs().getUrlArray();
	}
    	
    	Vector status = userRequest.getStatusOfRequest(SURLList);
    	
    	if ((status == null) || (status.size() == 0)) {
	    // since user request has at least one file requests, so 
	    // if status == null or has 0 size, then there is no match for the SURLs provided
	    // hence the status is failure.
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No matching file requests found!!"));
	    return result;
    	} 

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	TPutRequestFileStatus[] fileStatusArray = new TPutRequestFileStatus[status.size()];
    	
    	for (int i=0; i<status.size(); i++) {
	    fileStatusArray[i] = (TPutRequestFileStatus)(status.get(i));
	    TStatusCode code   = fileStatusArray[i].getStatus().getStatusCode();
	    reqSummary.add(code);
    	}
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));
    	//result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(userRequest.getWLCGStatus(), null));
	
	if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {	
	    userRequest.cleanMe(result.getReturnStatus().getStatusCode());
	}

    	return result;
    }

    public SrmCopyResponse srmCopy(SrmCopyRequest req) {  
    	String currMethodName = "srmCopy";
    	SrmCopyResponse result = new SrmCopyResponse();
    
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getArrayOfFileRequests() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests received!"));	
	    return result;
    	}

    	TCopyFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();
    	
    	if ((listOfFileReqs == null) || (listOfFileReqs.length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "No file requests received."));
	    return result;
    	}    	

    	TUserRequestCopy userRequest = new TUserRequestCopy(req.getUserRequestDescription(), user);
	/*
	TReturnStatus storageConsistencyStatus = userRequest.checkStorageConsistency(req.getTargetSpaceToken(), 
										     req.getTargetFileRetentionPolicyInfo());
	verify this only when copy is a local one.
	if (storageConsistencyStatus != null) {
	    result.setReturnStatus(storageConsistencyStatus);
	    return result;
	}
	*/

	TSRMLog.info(TSRMServer.class, null, "event=list rid="+userRequest.getID()+" reqSize="+listOfFileReqs.length,
		     "overwrite="+req.getOverwriteOption()+" fst="+req.getTargetFileStorageType()+" token="+req.getTargetSpaceToken()+"  rpi="+req.getTargetFileRetentionPolicyInfo()+" lt="+req.getDesiredTargetSURLLifeTime());

	GSSCredential creds = TSRMService.gGetCredential(currMethodName);//temp
	if (creds == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No credential. Did you do delegation?"));
	    return result;
	}

    	userRequest.setGSSCredential(creds);
    	userRequest.setRetryPeriod(req.getDesiredTotalRequestTime());
	//userRequest.setStorageSystemInfo(req.getStorageSystemInfo().getExtraInfoArray());
	userRequest.setOverwriteMode(req.getOverwriteOption());       	 	
	userRequest.setTargetFileStorageType(req.getTargetFileStorageType());

    	//result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, null));
    	result.setRequestToken(userRequest.getID());
    	
    	TCopyRequestFileStatus[] fileStatusList = new TCopyRequestFileStatus[listOfFileReqs.length];
    	
	for (int i=0; i<listOfFileReqs.length; i++) {
	    TCopyFileRequest curr = listOfFileReqs[i];
	    try {
		TSRMFileInputCopy input = new TSRMFileInputCopy(req, curr, i);
		userRequest.verifyConsistency(input.getSpaceType(), input.getSpaceToken());
		TCopyRequestFileStatus status = userRequest.addCopyFileRequest(input);
		fileStatusList[i] = status;	    
	    } catch (TSRMException e) {
		TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+ userRequest.getID(), "src="+curr.getSourceSURL());

		fileStatusList[i] =  new TCopyRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setTargetSURL(curr.getTargetSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    } catch (RuntimeException e) {
		TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+ userRequest.getID(), "src="+curr.getSourceSURL());

		fileStatusList[i] =  new TCopyRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setTargetSURL(curr.getTargetSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    }			
	}

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<listOfFileReqs.length; i++) {
	     TCopyRequestFileStatus status = fileStatusList[i];
	     TStatusCode code = status.getStatus().getStatusCode();
	     reqSummary.add(code);
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));

    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
    	
    	return result;
    }

    public SrmStatusOfCopyRequestResponse srmStatusOfCopyRequest(SrmStatusOfCopyRequestRequest  req) { 
	String currMethodName = "srmStatusOfCopyRequest rid="+req.getRequestToken();
    	SrmStatusOfCopyRequestResponse result = new SrmStatusOfCopyRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequestCopy userRequest = (TUserRequestCopy)(user.getRequest(req.getRequestToken(), 
									  TUserRequestCopy.class));
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}

	if (userRequest.isTerminated()) {
	    result.setReturnStatus(userRequest.getReturnStatus());
	    userRequest.cleanMe();
	    return result;
	}
	
    	URI[] fromSURLList = null;
	if (req.getArrayOfSourceSURLs() != null) {
	    fromSURLList = req.getArrayOfSourceSURLs().getUrlArray();
	}
    	URI[] toSURLList = null;
	if (req.getArrayOfTargetSURLs() != null) {
	    toSURLList = req.getArrayOfTargetSURLs().getUrlArray();
	}
    	
    	Vector status = userRequest.getStatusOfRequest(fromSURLList, toSURLList);
    	
    	if ((status == null) || (status.size() == 0)) {
	    // since user request has at least one file requests, so 
	    // if status == null or has 0 size, then there is no match for the SURLs provided
	    // hence the status is failure.
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No matching file requests found"));
	    return result;
    	} 
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	TCopyRequestFileStatus[] fileStatusArray = new TCopyRequestFileStatus[status.size()];
    	       
    	for (int i=0; i<status.size(); i++) {
	    fileStatusArray[i] = (TCopyRequestFileStatus)(status.get(i));
	    TStatusCode code   = fileStatusArray[i].getStatus().getStatusCode();
	    reqSummary.add(code);
    	}
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));
    	//result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(userRequest.getWLCGStatus(), null));
		
	userRequest.cleanMe(result.getReturnStatus().getStatusCode());
	
    	return result;
    }

    private SrmExtendFileLifeTimeResponse extendSURLLifeTime(SrmExtendFileLifeTimeRequest req, TAccount user) {	
    	SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();

	URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
	TSURLLifetimeReturnStatus[] fileStatusList = new TSURLLifetimeReturnStatus[surlArray.length];
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	
	for (int i=0; i<surlArray.length; i++) {
	    URI curr = surlArray[i];	   
	    TSURLLifetimeReturnStatus status = TSRMNameSpace.pinSURL(curr, req.getNewFileLifeTime(), user);
	    TSRMLog.info(this.getClass(), null, "extsurl=\""+curr+"\" code="+status.getStatus().getStatusCode(), "exp="+status.getStatus().getExplanation());
	    fileStatusList[i] = status;
	    if (status.getStatus().getStatusCode() == TStatusCode.SRM_RELEASED) {
		reqSummary.add(TStatusCode.SRM_FAILURE); // RELEASED is considered failure in extFileLifeTime
	    } else {
		reqSummary.add(status.getStatus().getStatusCode());
	    }
	}

	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));

	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));	

	return result;
    }
    
    public SrmExtendFileLifeTimeResponse srmExtendFileLifeTime(SrmExtendFileLifeTimeRequest req) { 
    	String currMethodName = "srmExtendFileLifeTime";
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	if (req.getArrayOfSURLs() == null) {
	    SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefEmptyInputs));
	    return result;
	}
	TAccount user = (TAccount)s;

	int urlArrayLength = req.getArrayOfSURLs().getUrlArray().length;
	if (req.getRequestToken() == null) {
	    TSRMLog.info(TSRMServer.class, null, "event=ext(SURL)", "size_of_input="+urlArrayLength);
	    return extendSURLLifeTime(req, user);
	}

	TSRMLog.info(TSRMServer.class, null, "event=ext(turl) rid="+req.getRequestToken(),  "size_of_input="+urlArrayLength);
	return extendTxfURLLifeTime(req, user);
    }
    
    private SrmExtendFileLifeTimeResponse extendTxfURLLifeTime(SrmExtendFileLifeTimeRequest req, TAccount user) {
    	SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();
	
    	TUserRequest  userRequest =  (user.getRequest(req.getRequestToken()));
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}    	

	//long newLifeTime = 600000; // 10 minutes, by default	

	URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
	TSURLLifetimeReturnStatus[] fileStatusList = new TSURLLifetimeReturnStatus[surlArray.length];
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();

	for (int i=0; i<surlArray.length; i++) {
	    TSRMRequest curr = userRequest.get(surlArray[i]);
	    if (curr == null) {		
		fileStatusList[i] = TSRMUtil.createTSURLLifetimeReturnStatus(surlArray[i], 
									     TStatusCode.SRM_INVALID_REQUEST, 
									     "No such turl known to the request. Ref:"+surlArray[i]);		
	    } else {		
		fileStatusList[i] = curr.extendTURLLifeTime(surlArray[i], req.getNewPinLifeTime());
		fileStatusList[i].setSurl(surlArray[i]);
	    }
	    if (fileStatusList[i].getStatus().getStatusCode() == TStatusCode.SRM_RELEASED) {
		reqSummary.add(TStatusCode.SRM_FAILURE); // RELEASED is considered failure in extFileLifeTime
	    } else {
		reqSummary.add(fileStatusList[i].getStatus().getStatusCode());
	    }
	}
	       
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));

    	return result;	
    }


 public SrmExtendFileLifeTimeInSpaceResponse srmExtendFileLifeTimeInSpace(SrmExtendFileLifeTimeInSpaceRequest req) { 
    	String currMethodName = "srmExtendFileLifeTimeInSpace";
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	if (req.getSpaceToken() == null) {
	    SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       "space token: "+TSRMUtil._DefEmptyInputs));
	    return result;
	}

	TSRMLog.debug(TSRMServer.class, null, "event=bookkeeping_"+currMethodName+" token="+req.getSpaceToken(), null);

	TAccount user = (TAccount)s;

	TSRMStorage.iSRMSpaceToken token = user.getToken(req.getSpaceToken());
	if (token == null) {
	    TSRMLog.debug(TSRMServer.class, null, "event=srmExtendFileLifetimeInSpace result=failed token="+ req.getSpaceToken(), "exp=\"No such token.\"");
	    SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
	    return result;
    	}

	if (token.isLifeTimeExpired()) {
	    TSRMLog.debug(TSRMServer.class, null, "event=srmExtendFileLifetimeInSpace result=failed token="+req.getSpaceToken(), "exp=\"Lifetime expired.\"");
	    SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED, null));
	    return result;
	}

	if (req.getArrayOfSURLs() != null) {	    
	    return token.extend(req.getArrayOfSURLs().getUrlArray(), req.getNewLifeTime());
	} else {
	    return token.extend(null, req.getNewLifeTime());
	}
    }
    
    
    public SrmGetRequestSummaryResponse srmGetRequestSummary(SrmGetRequestSummaryRequest req) {
    	String currMethodName = "srmGetRequestSummary";
    	SrmGetRequestSummaryResponse result = new SrmGetRequestSummaryResponse();
    	    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
    	
	TAccount user = (TAccount)s;

	if (req.getArrayOfRequestTokens() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	String[] tokenList = req.getArrayOfRequestTokens().getStringArray();
	if (tokenList.length == 0) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "No inputs"));
	    return result;
	}

    	Vector summaryList = new Vector();
	Vector invalidList = new Vector();
    	
    	for (int i=0; i<tokenList.length; i++) {
	    String curr = tokenList[i];
	    TUserRequest  userRequest =  user.getRequest(curr);
	    
	    if (userRequest == null) { 
		//result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
		//TSRMUtil._DefNoSuchRequestToken+":"+curr.getValue()));
		//return result;	
		TRequestSummary sum = new TRequestSummary();
		sum.setRequestToken(curr);
		sum.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "no such token."));
		invalidList.add(sum);
	    } else {
		TRequestSummary sum = userRequest.getSummary();
		sum.setRequestToken(curr);
		summaryList.add(sum);
	    }
    	}
	if (summaryList.size() == 0) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "None of the tokens are valid."));
	} else {
	    if (invalidList.size() > 0) {
		summaryList.addAll(invalidList);
	    }
	    TRequestSummary[] summaryArray = new TRequestSummary[summaryList.size()];  	 
	    for (int i=0; i<summaryList.size(); i++) {
		summaryArray[i] = (TRequestSummary)(summaryList.get(i)); 
	    }
		if (invalidList.size() > 0) {
			result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_PARTIAL_SUCCESS, null));
		} else {
	        result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
		}
	    result.setArrayOfRequestSummaries(TSRMUtil.convertToArray(summaryArray));
	}
	return result;
    }
    
    public SrmSuspendRequestResponse srmSuspendRequest(SrmSuspendRequestRequest req) { 
    	String currMethodName = "srmSuspendRequest";    	
    	SrmSuspendRequestResponse result = new SrmSuspendRequestResponse();    	
	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

    	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	    	
    	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));		
	    return result;
	}

    	TUserRequest userRequest = user.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST,
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
    	
	if (userRequest.isAborted()) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Aborted request can not be suspended."));
	    return result;
	}

    	userRequest.setSuspend();
    	
    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
    	return result;
    }

    public SrmResumeRequestResponse srmResumeRequest(SrmResumeRequestRequest req) {
    	String currMethodName = "srmResumeRequest";
    	SrmResumeRequestResponse result = new SrmResumeRequestResponse();
	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
    	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequest userRequest = user.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
    	
		if (userRequest.isSuspended()) {
    	   userRequest.resume();
    	
    		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
		} else {
			result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Was not suspended"));
		}
    	return result;
    }        
     
    public SrmAbortRequestResponse srmAbortRequest(SrmAbortRequestRequest req) { 
    	String currMethodName = "srmAbortRequest";
    	SrmAbortRequestResponse result = new SrmAbortRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}
    	
    	TUserRequest userRequest = user.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
    	
    	result.setReturnStatus(userRequest.abort());
    	
    	//result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
    	return result;    	
    }

    public SrmAbortFilesResponse srmAbortFiles(SrmAbortFilesRequest req) {
    	String currMethodName = "srmAbortFiles";
    	SrmAbortFilesResponse result = new SrmAbortFilesResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;
    	
	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequest userRequest = user.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
	if ((req.getArrayOfSURLs() == null) || (req.getArrayOfSURLs().getUrlArray().length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No surls is provided."));
	    return result;
	}
    	
    	URI[] surls = req.getArrayOfSURLs().getUrlArray();
    	TSURLReturnStatus[] statusArray = new TSURLReturnStatus[surls.length];
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	for (int i=0; i<surls.length; i++) {
	    TSRMRequest curr = userRequest.get(surls[i]);
	    statusArray[i] = new TSURLReturnStatus();
	    if (curr != null) {
		curr.abort(null, false);
		statusArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	    } else {
		statusArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No such file request"));
	    }
	    reqSummary.add(statusArray[i].getStatus().getStatusCode());
	    statusArray[i].setSurl(surls[i]);
    	}
	userRequest.fileAbortCalled();

    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(statusArray));
    	//result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
    	return result;
    	
    }
    public SrmRmResponse srmRm(SrmRmRequest req) { 
    	String currMethodName = "srmRm";
    	SrmRmResponse result = new SrmRmResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	try {
	    Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
	    if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
		result.setReturnStatus((TReturnStatus)s);
		return result;
	    }
	    
	    TAccount user = (TAccount)s;
	    
	    if ((req.getArrayOfSURLs() == null) || (req.getArrayOfSURLs().getUrlArray().length == 0)) {
		result.setReturnStatus(TSRMUtil.createReturnStatusFailed("No inputs"));
	    }
	    
	    TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	    URI[] urlArray = req.getArrayOfSURLs().getUrlArray();
	    TSURLReturnStatus[] statusArray = new TSURLReturnStatus[urlArray.length];
	    
	    TSRMLog.debug(TSRMServer.class, null, "event=bookkeeping_srmRm"+ " inputSize="+urlArray.length, "caller="+user.getID());
	    
	    for (int i=0; i<urlArray.length; i++) {
		statusArray[i] = new TSURLReturnStatus();
		statusArray[i].setSurl(urlArray[i]);
		
		TSURLInfo curr = new TSURLInfo(urlArray[i], req.getStorageSystemInfo());
		statusArray[i].setStatus(user.rmFile(curr));
		
		TSRMLog.debug(TSRMServer.class, null, "event=srmRmReturns surl=\""+urlArray[i].toString()+"\"", 
			     "code="+statusArray[i].getStatus().getStatusCode()+" exp="+statusArray[i].getStatus().getExplanation());			      
		reqSummary.add(statusArray[i].getStatus().getStatusCode());
	    }
	    
	    ArrayOfTSURLReturnStatus statusArrayObj = new ArrayOfTSURLReturnStatus();
	    statusArrayObj.setStatusArray(statusArray);
	    result.setArrayOfFileStatuses(statusArrayObj);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	} catch (TSRMException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	} catch (RuntimeException ex) {
	    TSRMLog.exception(TSRMServer.class, "details:", ex);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(ex));
	}

	return result;
    }
           
    public SrmGetSpaceTokensResponse srmGetSpaceToken(SrmGetSpaceTokensRequest req) 
    { 
	String currMethodName = "srmGetSpaceTokens";
	SrmGetSpaceTokensResponse result = new SrmGetSpaceTokensResponse();

	String[] m = TSRMUserSpaceTokenSupport.match(req.getUserSpaceTokenDescription());
	if (m != null) {
	  //result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	  //result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(m));
	  //return result;
	} 

	String[] staticTokens = StaticToken.findByDesc(Config._staticTokenList, req.getUserSpaceTokenDescription());
	if ((staticTokens == null) && (Config._staticTokenList != null)) {
	  staticTokens = StaticToken.getNames(Config._staticTokenList);
	}
	if ((m != null) || (staticTokens != null)) {
	  result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	  result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(m, staticTokens));
	  return result;
	}
              
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
    	
	TAccount user = (TAccount)s;

        String[] matchedTokens = user.getMatchingTokens(req.getUserSpaceTokenDescription());
	if (matchedTokens != null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	    result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(matchedTokens));
	} else {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "No matching found."));
	}
	return result;
    }

    public SrmGetRequestTokensResponse srmGetRequestTokens(SrmGetRequestTokensRequest req) { 
	String currMethodName = "srmGetRequestTokens";
	SrmGetRequestTokensResponse result = new SrmGetRequestTokensResponse();
	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	/*
	if (req.getUserRequestDescription() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No input description."));
	    return result;
	}
	*/

	TUserRequest[] matchedReqs = user.getMatchingReqs(req.getUserRequestDescription());
	if (matchedReqs != null) {	    
	    result.setArrayOfRequestTokens(TSRMUtil.convertToArray(matchedReqs));
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	} else {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "No matches."));
	}
	return result;
    }

    public SrmPurgeFromSpaceResponse srmPurgeFromSpace(SrmPurgeFromSpaceRequest req) {
	String currMethodName = "srmPurgeFromSpace";    	
    	SrmPurgeFromSpaceResponse result = new SrmPurgeFromSpaceResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	TAccount user = (TAccount)s;

	if (req.getSpaceToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatusFailed(TSRMUtil._DefNullToken));
	    return result;
	}

	TSRMStorage.iSRMSpaceToken token = user.getToken(req.getSpaceToken());
	if (token == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
	    return result;
    	}

	if (req.getArrayOfSURLs() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatusFailed("No input surl received!")); 
	    return result;
	}
	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
	TSURLReturnStatus[] fileStatusArray = new TSURLReturnStatus[surlArray.length];
	for (int i=0; i<surlArray.length; i++) {
	    URI surl = surlArray[i];
	    try {
		TSURLInfo currSURLInfo = new TSURLInfo(surl, req.getStorageSystemInfo());
		TSupportedURL curr = TSupportedURL.createAndCheckSiteFileName(currSURLInfo, user, TSRMPermission.Writable);
		TReturnStatus status = TSRMNameSpace.purge(curr.getURLString(), token, false);
		curr = null;
		fileStatusArray[i] = TSRMUtil.createTSURLReturnStatus(surl, status);
		reqSummary.add(status.getStatusCode());
	    } catch (TSRMException e) {
		TSRMLog.exception(TSRMServer.class, "details", e);
		
		TReturnStatus status = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.toString());
		fileStatusArray[i] = TSRMUtil.createTSURLReturnStatus(surl, status);
		reqSummary.add(status.getStatusCode());
	    }
	}
	//
	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));

	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
		
	return result;
	
    }

    public SrmStatusOfLsRequestResponse srmStatusOfLs(SrmStatusOfLsRequestRequest req) {
	String currMethodName = "srmStatusOfLsRequest rid="+req.getRequestToken();
    	SrmStatusOfLsRequestResponse result = new SrmStatusOfLsRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}
	//TAccount user = (TAccount)s;
    	
	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

	TUserRequestLs userRequest = _lsManager.getRequest(req.getRequestToken());
    	
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}

	result.setReturnStatus(userRequest.getReturnStatus());
	if (userRequest.isDone()) {
	    result.setReturnStatus(userRequest.getResult().getReturnStatus());
	    result.setDetails(userRequest.getResult().getDetails());
	}
	_lsManager.removeWhenFinished(userRequest);
    	return result;

    }

    public SrmBringOnlineResponse srmBringOnline(SrmBringOnlineRequest req) {
	String currMethodName = "srmBringOnline";
    	SrmBringOnlineResponse result = new SrmBringOnlineResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	if (req.getArrayOfFileRequests() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No file requests received!"));	
	    return result;
    	}

    	TGetFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();    	
    	if ((listOfFileReqs == null) || (listOfFileReqs.length == 0)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "No file requests received."));
	    return result;
    	}

	TUserRequestGet userRequest = new TUserRequestGet(user, req.getUserRequestDescription());
	TReturnStatus storageConsistencyStatus = userRequest.checkStorageConsistency(req.getTargetSpaceToken(), 
										     req.getTargetFileRetentionPolicyInfo());
	if (storageConsistencyStatus != null) {
	    result.setReturnStatus(storageConsistencyStatus);
	    return result;
	}

	TReturnStatus typeConsistencyStatus = userRequest.checkFileTypeInconsistency(req.getTargetSpaceToken(),
										     req.getDesiredFileStorageType());
	if (typeConsistencyStatus != null) {
	    result.setReturnStatus(typeConsistencyStatus);
	    return result;
	}

	TSRMLog.info(TSRMServer.class, null, "event=list rid="+userRequest.getID()+" reqSize="+listOfFileReqs.length,
		     "desiredFStype="+req.getDesiredFileStorageType()+" targetSpaceToken="+req.getTargetSpaceToken()+"  trpi="+req.getTargetFileRetentionPolicyInfo()+
		     " desiredLifetime="+req.getDesiredLifeTime());
	GSSCredential creds = TSRMService.gGetCredential(currMethodName); // temp
        userRequest.setGSSCredential(creds);
	userRequest.setRetryPeriod(req.getDesiredTotalRequestTime());
	if(req.getTransferParameters() != null) {
	    int numProtocolsTaken = userRequest.setTransferProtocols(req.getTransferParameters().getArrayOfTransferProtocols());
	    if (numProtocolsTaken == 0) {
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, 
								   "No transfer protocol specified by user is supported by SRM."));
		return result;
	    }
	}
	userRequest.setStorageSystemInfo(req.getStorageSystemInfo());
        
    	result.setRequestToken(userRequest.getID());
    	
	TBringOnlineRequestFileStatus[] fileStatusList = new TBringOnlineRequestFileStatus[listOfFileReqs.length];
    	
	for (int i=0; i<listOfFileReqs.length; i++) {
	    TGetFileRequest curr = listOfFileReqs[i];
	    try {
		TSRMFileInput input = new TSRMFileInput(req, curr, i);
		userRequest.verifyConsistency(input.getSpaceType(), input.getSpaceToken());
		TBringOnlineRequestFileStatus status = TSRMUtil.convert(userRequest.addGetFileRequest(input));
		fileStatusList[i] = status;	    
	    } catch (TSRMException e) {
	     	TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+userRequest.getID(), "src="+curr.getSourceSURL());
		
		fileStatusList[i] =  new TBringOnlineRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    } catch (RuntimeException e) {
		TSRMLog.exception(TSRMServer.class, "details:", e);
		TSRMLog.info(TSRMServer.class, null, "event=rejected rid="+userRequest.getID(), "src="+curr.getSourceSURL());
		
		fileStatusList[i] =  new TBringOnlineRequestFileStatus();
		fileStatusList[i].setSourceSURL(curr.getSourceSURL());
		fileStatusList[i].setStatus(TSRMUtil.createReturnStatus(e));
	    }
	}
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<listOfFileReqs.length; i++) {
	     TBringOnlineRequestFileStatus status = fileStatusList[i];
	     TStatusCode code = status.getStatus().getStatusCode();
	     reqSummary.add(code);
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));    	
    	
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
    	
	return result;
    }

    public SrmStatusOfBringOnlineRequestResponse srmStatusOfBringOnline(SrmStatusOfBringOnlineRequestRequest req) {
	String currMethodName = "srmStatusOfBringOnline rid="+req.getRequestToken();    	
    	SrmStatusOfBringOnlineRequestResponse result = new SrmStatusOfBringOnlineRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequestGet userRequest = (TUserRequestGet)(user.getRequest(req.getRequestToken(),
									TUserRequestGet.class));
    
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}

	if (userRequest.isTerminated()) {
	    result.setReturnStatus(userRequest.getReturnStatus());
	    userRequest.cleanMe();
	    return result;
	}
	
	URI[] SURLList = null;
	if (req.getArrayOfSourceSURLs() != null) {
	    SURLList = req.getArrayOfSourceSURLs().getUrlArray();
	}
    	
    	Vector status = userRequest.getStatusOfRequest(SURLList);
    	
    	if ((status == null) || (status.size() == 0)) {
	    // since user request has at least one file requests, so 
	    // if status == null or has 0 size, then there is no match for the SURLs provided
	    // hence the status is failure.
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "No matching file requests found!"));
	    return result;
    	} 
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	TBringOnlineRequestFileStatus[] fileStatusArray = new TBringOnlineRequestFileStatus[status.size()];
    	
    	for (int i=0; i<status.size(); i++) {
	    fileStatusArray[i] = TSRMUtil.convert((TGetRequestFileStatus)(status.get(i)));
	    TStatusCode code   = fileStatusArray[i].getStatus().getStatusCode();
	    reqSummary.add(code);
    	}
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));
	//result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(userRequest.getWLCGStatus(), null));
    	return result;
    }

    public SrmChangeSpaceForFilesResponse srmChangeSpaceForFiles(SrmChangeSpaceForFilesRequest req) { 
	String currMethodName = "srmChangeSpaceForFile";    	
    	SrmChangeSpaceForFilesResponse result = new SrmChangeSpaceForFilesResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, true);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	String spaceToken = req.getTargetSpaceToken();
	if (spaceToken == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No target space token given."));
	    return result;
	}
	TSRMStorage.iSRMSpaceToken token = user.getToken(req.getTargetSpaceToken());
	if (token == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
	    return result;
	}

	if (token.isLifeTimeExpired()) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED, null));
	    return result;
	}

	if ((req.getArrayOfSURLs() == null) || 
	    (req.getArrayOfSURLs().getUrlArray() == null) || 
	    (req.getArrayOfSURLs().getUrlArray().length == 0))
	{
	     result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No surls given."));
	     return result;	
	}

	TUserRequestFileToSpace userRequest = new TUserRequestFileToSpace(null, user);
	userRequest.setStorageSystemInfo(req.getStorageSystemInfo());

	result.setRequestToken(userRequest.getID());
    
	URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
	TSURLReturnStatus[] statusArray = new TSURLReturnStatus[surlArray.length];
	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();	
        for (int i=0; i<surlArray.length; i++) {
	    URI curr = surlArray[i];
	    statusArray[i] = userRequest.addFileRequest(curr, token, i);
	    reqSummary.add(statusArray[i].getStatus().getStatusCode());
	}
	
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(statusArray));
	return result;
    }

    public SrmStatusOfChangeSpaceForFilesRequestResponse srmStatusOfChangeSpaceForFilesRequest(SrmStatusOfChangeSpaceForFilesRequestRequest req) {
	String currMethodName = "srmStatusOfChangeSpaceForFilesRequest rid="+req.getRequestToken();    	
    	SrmStatusOfChangeSpaceForFilesRequestResponse result = new SrmStatusOfChangeSpaceForFilesRequestResponse();
    	
	TReturnStatus supportStatus = isSupported(currMethodName);
	if (supportStatus != null) {
	    result.setReturnStatus(supportStatus);
	    return result;
	}

	Object s = checkUserValidity(req.getAuthorizationID(), currMethodName, false);
    	if (s.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    result.setReturnStatus((TReturnStatus)s);
	    return result;
	}

	TAccount user = (TAccount)s;

	if (req.getRequestToken() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}

    	TUserRequestFileToSpace userRequest = (TUserRequestFileToSpace)(user.getRequest(req.getRequestToken()));
    
    	if (userRequest == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       TSRMUtil._DefNoSuchRequestToken));
	    return result;
    	}
	if (userRequest.isTerminated()) {
	    result.setReturnStatus(userRequest.getReturnStatus());
	    return result;
	}
	    	
    	Vector status = userRequest.getStatusOfRequest(null);
    	
    	if ((status == null) || (status.size() == 0)) {
	    // since user request has at least one file requests, so 
	    // if status == null or has 0 size, then there is no match for the SURLs provided
	    // hence the status is failure.
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
							       "No matching file requests found!"));
	    return result;
    	} 
    	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
    	TSURLReturnStatus[] fileStatusArray = new TSURLReturnStatus[status.size()];
    	
    	for (int i=0; i<status.size(); i++) {
	    fileStatusArray[i] = (TSURLReturnStatus)(status.get(i));
	    TStatusCode code   = fileStatusArray[i].getStatus().getStatusCode();
	    reqSummary.add(code);
    	}
    	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusArray));
	//result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setReturnStatus(TSRMUtil.createReturnStatus(userRequest.getWLCGStatus(), null));

    	return result;
	
    }

    // not planning to do immediately        

    public SrmSetPermissionResponse srmSetPermission(SrmSetPermissionRequest srmSetPermissionRequest) { 
	SrmSetPermissionResponse result = new SrmSetPermissionResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, null));
	return result;
    }

    public SrmCheckPermissionResponse srmCheckPermission(SrmCheckPermissionRequest srmCheckPermissionRequest) { 
	SrmCheckPermissionResponse result = new SrmCheckPermissionResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, null));
	return result;
    }
    
    public SrmGetPermissionResponse srmGetPermission(SrmGetPermissionRequest req) {
	SrmGetPermissionResponse result = new SrmGetPermissionResponse ();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, null));
	return result;
    }

    public SrmStatusOfUpdateSpaceRequestResponse srmStatusOfUpdateSpace(SrmStatusOfUpdateSpaceRequestRequest req) {
	SrmStatusOfUpdateSpaceRequestResponse result = new SrmStatusOfUpdateSpaceRequestResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, null));
	return result;
    }

    public SrmStatusOfReserveSpaceRequestResponse srmStatusOfReserveSpace(SrmStatusOfReserveSpaceRequestRequest req) {
	SrmStatusOfReserveSpaceRequestResponse result = new SrmStatusOfReserveSpaceRequestResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, null));
	return result;
    }

    private TReturnStatus isSupported(String currMethodName) {	
	if (isSpaceMgtFunction(currMethodName) && (Config.isSpaceMgtDisabled())) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "SpaceManagement is disabled");
	}

	if (isDirMgtFunction(currMethodName) && (Config.isDirMgtDisabled())) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "DirManagement is disabled");
	}

	if (isRemoteCopyFunction(currMethodName) && (Config.isRemoteCopyDisabled())) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Copy is disabled");
	}

	if (isLocalAuthorizationFunction(currMethodName) && (Config.isLocalAuthorizationDisabled())) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Local Authorization is disabled");
	}
	    
	return null;
    }

    private boolean isLocalAuthorizationFunction(String currMethodName) {
	int pos = currMethodName.indexOf("Permission");
	if (pos > 0) {
	    return true;
	}
	return false;
    }

    private boolean isRemoteCopyFunction(String currMethodName) {
	if (currMethodName.equalsIgnoreCase("srmCopy")) {
	    return true;
	}
	return false;
    }

    private boolean isSpaceMgtFunction(String currMethodName) {
	int pos = currMethodName.indexOf("Space");
	if (pos > 0) {
	    return true;
	}
	return false;
    }

    private boolean isDirMgtFunction(String currMethodName) {
	String[] functionList = {"srmMkdir", "srmRmdir", "srmRm", "srmLs", "srmStatusOfLsRequest", "srmMv"};
	return isInGroup(functionList, currMethodName);
    }

    private boolean isInGroup(String[] functionList, String currMethodName) {
	for (int i=0; i<functionList.length; i++) {
	    String curr = functionList[i];
	    if (currMethodName.equalsIgnoreCase(curr)) {
		return true;
	    }
	}
	return false;
    }   
}
