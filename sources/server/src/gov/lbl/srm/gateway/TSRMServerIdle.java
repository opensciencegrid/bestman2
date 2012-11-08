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
//import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
//import gov.lbl.srm.storage.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

import java.rmi.RemoteException;
import java.util.Vector;

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.*;

public class TSRMServerIdle extends TSRMServer {
    final String _notSupportExp = "Gateway mode does not support this function";

    TConnectionController _fileSystemAccessGuard =  null;

    public TSRMServerIdle() {
	int concurrency = ConfigGateway._fsConcurrency;
	if (concurrency > 0) {
	    _fileSystemAccessGuard = new TConnectionController(concurrency);
	    TSRMLog.info(this.getClass(), null, "event=setFSAC", "value="+concurrency);
	} else {
	    TSRMLog.info(this.getClass(), null, "event=setFSAC", "value=null");
	}
    }

    public boolean acquireGuard() {
	if (_fileSystemAccessGuard == null) { 
	    return true;
	}
	return _fileSystemAccessGuard.acquire();
    }

    public boolean releaseGuard() {
	if (_fileSystemAccessGuard == null) { 
	    return true;
	}
	return _fileSystemAccessGuard.release();
    }

    public static void checkMemoryUsage(String currMethodName) {
	//TSRMLog.info(TSRMServer.class, currMethodName, "JVM: freebytes ="+Runtime.getRuntime().freeMemory(), null);
	//TSRMLog.info(TSRMServer.class, currMethodName, "JVM: totalbytes="+Runtime.getRuntime().totalMemory(), null);
	//Runtime.getRuntime().gc();
    }
  
    //
    // requests that returns rightaway
    //

    public SrmPingResponse srmPing(SrmPingRequest req) throws java.rmi.RemoteException {       			
	//checkUserValidity(null, "srmPing");

	SrmPingResponse result = new SrmPingResponse();
	result.setVersionInfo("v2.3.0");

	return result;
    }

    public SrmGetTransferProtocolsResponse srmGetTransferProtocols(SrmGetTransferProtocolsRequest req)  
	throws java.rmi.RemoteException 
    {       	
	//checkUserValidity(null, "srmGetTransferProtocols");
	SrmGetTransferProtocolsResponse result = new SrmGetTransferProtocolsResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));

	return result;
    }

    //
    // will not return null or throw exceptions to users
    // a status with msg is more friendly and meaningful
    //
    public SrmReserveSpaceResponse srmReserveSpace(SrmReserveSpaceRequest req) throws RemoteException { 
    	//String currMethodName = "srmReserveSpace";		
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

	SrmReserveSpaceResponse result = new SrmReserveSpaceResponse();		

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmUpdateSpaceResponse srmUpdateSpace(SrmUpdateSpaceRequest req) {
    	//String currMethodName = "srmUpdateSpace";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmUpdateSpaceResponse result = new SrmUpdateSpaceResponse();

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    
    public SrmReleaseSpaceResponse srmReleaseSpace(SrmReleaseSpaceRequest req) {
    	//String currMethodName = "srmReleaseSpace";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmReleaseSpaceResponse result = new SrmReleaseSpaceResponse();    		

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmPrepareToGetResponse srmPrepareToGet(SrmPrepareToGetRequest req) { 
    	//String currMethodName = "srmPrepareToGet";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmPrepareToGetResponse result = new SrmPrepareToGetResponse();    	        

    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }
    
    public SrmStatusOfGetRequestResponse srmStatusOfGetRequest(SrmStatusOfGetRequestRequest req) { 
	//String currMethodName = "srmStatusOfGetRequest "+req.getRequestToken();    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmStatusOfGetRequestResponse result = new SrmStatusOfGetRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    
 
    public SrmReleaseFilesResponse srmReleaseFiles(SrmReleaseFilesRequest req) { 
    	//String currMethodName = "srmReleaseFiles";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmReleaseFilesResponse result = new SrmReleaseFilesResponse();    	
	
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));	
    	return result;
    }
    

    public SrmGetSpaceMetaDataResponse srmGetSpaceMetaData(SrmGetSpaceMetaDataRequest req) { 
    	//String currMethodName = "srmGetSpaceMetaData";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmGetSpaceMetaDataResponse result = new SrmGetSpaceMetaDataResponse();    		

    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmMkdirResponse srmMkdir(SrmMkdirRequest req) { 
    	//String currMethodName = "srmMkdir";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmMkdirResponse result = new SrmMkdirResponse();	    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmRmdirResponse srmRmdir(SrmRmdirRequest req) { 
    	//String currMethodName = "srmRmdir";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmRmdirResponse result = new SrmRmdirResponse();	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmLsResponse srmLs(SrmLsRequest req) {
    	//String currMethodName = "srmLs()";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmLsResponse result = new SrmLsResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;		    
    }

    public SrmMvResponse srmMv(SrmMvRequest req) { 
    	//String currMethodName = "srmMv()";
    	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmMvResponse result = new SrmMvResponse();    	    	
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }    

    public SrmPrepareToPutResponse srmPrepareToPut(SrmPrepareToPutRequest req) { 
    	//String currMethodName = "srmPrepareToPut";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmPrepareToPutResponse result = new SrmPrepareToPutResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }
    
    public SrmPutDoneResponse srmPutDone(SrmPutDoneRequest req) { 
    	//String currMethodName = "srmPutDone";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmPutDoneResponse result = new SrmPutDoneResponse();
    	
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }
     
    public SrmStatusOfPutRequestResponse srmStatusOfPutRequest(SrmStatusOfPutRequestRequest req) { 
    	//String currMethodName = "srmStatusOfPutRequest "+req.getRequestToken();
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmStatusOfPutRequestResponse result = new SrmStatusOfPutRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmCopyResponse srmCopy(SrmCopyRequest req) {  
    	//String currMethodName = "srmCopy";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmCopyResponse result = new SrmCopyResponse();    

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmStatusOfCopyRequestResponse srmStatusOfCopyRequest(SrmStatusOfCopyRequestRequest  req) { 
	//String currMethodName = "srmStatusOfCopyRequest ";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmStatusOfCopyRequestResponse result = new SrmStatusOfCopyRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

 
    public SrmExtendFileLifeTimeResponse srmExtendFileLifeTime(SrmExtendFileLifeTimeRequest req) { 
    	//String currMethodName = "srmExtendFileLifeTime";
    	//checkUserValidity(req.getAuthorizationID(), currMethodName);

	SrmExtendFileLifeTimeResponse result = new SrmExtendFileLifeTimeResponse();

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    

    public SrmExtendFileLifeTimeInSpaceResponse srmExtendFileLifeTimeInSpace(SrmExtendFileLifeTimeInSpaceRequest req) { 
    	//String currMethodName = "srmExtendFileLifeTimeInSpace";
    	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

	SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    
    
    public SrmGetRequestSummaryResponse srmGetRequestSummary(SrmGetRequestSummaryRequest req) {
    	//String currMethodName = "srmGetRequestSummary";
    	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmGetRequestSummaryResponse result = new SrmGetRequestSummaryResponse();    	    	
    	
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    
    public SrmSuspendRequestResponse srmSuspendRequest(SrmSuspendRequestRequest req) { 
    	//String currMethodName = "srmSuspendRequest";    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmSuspendRequestResponse result = new SrmSuspendRequestResponse();    		
	    	    	
    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmResumeRequestResponse srmResumeRequest(SrmResumeRequestRequest req) {
    	//String currMethodName = "srmResumeRequest";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmResumeRequestResponse result = new SrmResumeRequestResponse();	
    	
    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }        
     
    public SrmAbortRequestResponse srmAbortRequest(SrmAbortRequestRequest req) { 
    	//String currMethodName = "srmAbortRequest";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmAbortRequestResponse result = new SrmAbortRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;    	
    }

    public SrmAbortFilesResponse srmAbortFiles(SrmAbortFilesRequest req) {
    	//String currMethodName = "srmAbortFiles";
	//checkUserValidity(req.getAuthorizationID(), currMethodName, false);

    	SrmAbortFilesResponse result = new SrmAbortFilesResponse();    	
	
    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    	
    }
    public SrmRmResponse srmRm(SrmRmRequest req) { 
    	//String currMethodName = "srmRm";
	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

    	SrmRmResponse result = new SrmRmResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
           
    public SrmGetSpaceTokensResponse srmGetSpaceToken(SrmGetSpaceTokensRequest req) 
    { 
	//String currMethodName = "srmGetSpaceToken";
	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

	SrmGetSpaceTokensResponse result = new SrmGetSpaceTokensResponse();

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmGetRequestTokensResponse srmGetRequestTokens(SrmGetRequestTokensRequest req) { 
	//String currMethodName = "srmGetRequestTokens";
	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

	SrmGetRequestTokensResponse result = new SrmGetRequestTokensResponse();	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmPurgeFromSpaceResponse srmPurgeFromSpace(SrmPurgeFromSpaceRequest req) {
	//String currMethodName = "srmPurgeFromSpace()";    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

    	SrmPurgeFromSpaceResponse result = new SrmPurgeFromSpaceResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;	
    }

    public SrmStatusOfLsRequestResponse srmStatusOfLs(SrmStatusOfLsRequestRequest req) {
	//String currMethodName = "srmStatusOfLsRequest token="+req.getRequestToken();
	//checkUserValidity(req.getAuthorizationID(), currMethodName, false);

    	SrmStatusOfLsRequestResponse result = new SrmStatusOfLsRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmBringOnlineResponse srmBringOnline(SrmBringOnlineRequest req) {
	//String currMethodName = "srmBringOnline";
	//checkUserValidity(req.getAuthorizationID(), currMethodName, true);

    	SrmBringOnlineResponse result = new SrmBringOnlineResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmStatusOfBringOnlineRequestResponse srmStatusOfBringOnline(SrmStatusOfBringOnlineRequestRequest req) {
	//String currMethodName = "srmStatusOfBringOnline "+req.getRequestToken();    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmStatusOfBringOnlineRequestResponse result = new SrmStatusOfBringOnlineRequestResponse();    	

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
    }

    public SrmChangeSpaceForFilesResponse srmChangeSpaceForFiles(SrmChangeSpaceForFilesRequest req) { 
	//String currMethodName = "srmChangeSpaceForFile";    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmChangeSpaceForFilesResponse result = new SrmChangeSpaceForFilesResponse();    		

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmStatusOfChangeSpaceForFilesRequestResponse srmStatusOfChangeSpaceForFilesRequest(SrmStatusOfChangeSpaceForFilesRequestRequest req) {
	//String currMethodName = "srmStatusOfChangeSpaceForFilesRequest ";    	
	//checkUserValidity(req.getAuthorizationID(), currMethodName);

    	SrmStatusOfChangeSpaceForFilesRequestResponse result = new SrmStatusOfChangeSpaceForFilesRequestResponse();

	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
    	return result;
	
    }

    public SrmSetPermissionResponse srmSetPermission(SrmSetPermissionRequest srmSetPermissionRequest) { 
	SrmSetPermissionResponse result = new SrmSetPermissionResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmCheckPermissionResponse srmCheckPermission(SrmCheckPermissionRequest srmCheckPermissionRequest) { 
	SrmCheckPermissionResponse result = new SrmCheckPermissionResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
    
    public SrmGetPermissionResponse srmGetPermission(SrmGetPermissionRequest req) {
	SrmGetPermissionResponse result = new SrmGetPermissionResponse ();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmStatusOfUpdateSpaceRequestResponse srmStatusOfUpdateSpace(SrmStatusOfUpdateSpaceRequestRequest req) {
	SrmStatusOfUpdateSpaceRequestResponse result = new SrmStatusOfUpdateSpaceRequestResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }

    public SrmStatusOfReserveSpaceRequestResponse srmStatusOfReserveSpace(SrmStatusOfReserveSpaceRequestRequest req) {
	SrmStatusOfReserveSpaceRequestResponse result = new SrmStatusOfReserveSpaceRequestResponse();
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, _notSupportExp));
	return result;
    }
}
