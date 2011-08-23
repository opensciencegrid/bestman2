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
import gov.lbl.srm.util.*;
import gov.lbl.srm.impl.*;
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.transfer.*;
import org.ietf.jgss.GSSCredential;
import gov.lbl.srm.client.SRMClient;
import org.apache.axis.types.*;
//import javax.xml.rpc.Stub;
import java.io.*;


public  class TSRMContactRemoteSRM {
    //private ISRMPortType _srm = null;
    private TSRMStubHolder _srmStubHolder = null;
    private String  _reqToken = null;
    //private TRetentionPolicyInfo _token = null;
    private GSSCredential _cred = null;

    public TSRMContactRemoteSRM(TSRMStubHolder stubHolder) {
	_srmStubHolder = stubHolder;
    }

    public TSRMContactRemoteSRM(TSRMStubHolder stubHolder, GSSCredential cred) {
	_srmStubHolder = stubHolder;
	_srmStubHolder.useCredential(cred);
	_cred = cred;
	//SRMClient.useCredential((Stub)_srm, cred);	      
	//SRMClient.useCredential((Stub)(stubHolder._srmStubHolder.getStub()), cred);	      
    }
 
    public void abort() {
	if ((_reqToken == null) || (_srmStubHolder.getStub() == null)) {
	    return;
	}

	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	try {
	    SrmAbortRequestRequest r = new SrmAbortRequestRequest();
	    r.setRequestToken(_reqToken);
	    SrmAbortRequestResponse result =_srmStubHolder.getStub().srmAbortRequest(r);
	    String resultStr = null;
	    if (result.getReturnStatus() != null) {
	        resultStr = "result="+result.getReturnStatus().getStatusCode();
	    }
	    TSRMLog.info(this.getClass(), null, "event=srmAbortReq() reqToken="+_reqToken, resultStr);
	} catch (Exception e) {
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    throw new TSRMException(e.getMessage(), false);
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
    }

     public SrmLsResponse ls(SrmLsRequest r/*TSURLInfo url*/) {
	 TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	 try {	    
	     URI uri = r.getArrayOfSURLs().getUrlArray()[0];
	     TSRMLog.info(this.getClass(), null, "event=callSrmLs", "uri=\""+uri.toString()+"\"");
	     
	     SrmLsResponse result = _srmStubHolder.getStub().srmLs(r);
	     return result;
	 } catch (Exception e) {
	     TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	     throw new TSRMException(e.getMessage(), false);
	 } finally {
	     tt.setFinished();
	     tt.alarm();
	 }
    }
     
    public void releaseFile(URI url) {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	try {
	    TSRMLog.debug(this.getClass(), null, "event=callSrmReleaseFiles()", "reqToken="+_reqToken);
	    SrmReleaseFilesRequest r = new SrmReleaseFilesRequest();
	    //r.setKeepSpace(new Boolean(false)); // CERN cannt handle this
	    r.setRequestToken(_reqToken);
	    r.setArrayOfSURLs(TSRMUtil.convertToArray(url));
	    
	    SrmReleaseFilesResponse result = _srmStubHolder.getStub().srmReleaseFiles(r);
	    if (result.getReturnStatus() != null) {
	        TSRMLog.debug(this.getClass(), null, "event=callSrmReleaseFiles rid="+_reqToken, "status="+result.getReturnStatus().getStatusCode());
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=callSrmReleaseFiles rid="+_reqToken, "status=null");
	    }
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    //throw new TSRMException(e.getMessage(), false);
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
    }

    public void extendLifeTime(URI surl, int newLifeTimeSeconds) {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	try {
	    SrmExtendFileLifeTimeRequest r = new SrmExtendFileLifeTimeRequest();
	    r.setRequestToken(_reqToken);	    
	    r.setArrayOfSURLs(TSRMUtil.convertToArray(surl));
	    r.setNewFileLifeTime(TSRMUtil.createTLifeTimeInSeconds(newLifeTimeSeconds, false));

	    SrmExtendFileLifeTimeResponse result = _srmStubHolder.getStub().srmExtendFileLifeTime(r);

	    if (result == null) {
		TSRMLog.debug(this.getClass(), null, "event=callSrmExtendLifeTime", "result=null");		
	    } else if (result.getReturnStatus() == null) {
		TSRMLog.debug(this.getClass(), null, "event=callSrmExtendLifeTime", "resultStatus=null");
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=callSrmExtendLifeTime", "resultStatus="+result.getReturnStatus().getStatusCode());	    
		TReturnStatus status = result.getReturnStatus();
		
		if (status.getStatusCode() == TStatusCode.SRM_SUCCESS) {
		    if (result.getArrayOfFileStatuses() == null) {
			TSRMLog.debug(this.getClass(), null, "event=callSrmExtendLifeTime", "result=\"remote site returns null array\"");
		    } else {
			TSURLLifetimeReturnStatus temp = result.getArrayOfFileStatuses().getStatusArray(0);
			String v = "";
			if (temp.getFileLifetime() != null) {
			    v += "fileLT="+temp.getFileLifetime().intValue();
			} 
			if (temp.getPinLifetime() != null) {
			    v += " pinLT="+temp.getPinLifetime().intValue();
			}
			TSRMLog.debug(this.getClass(), null, "event=callSrmExtendlifetime rid="+_reqToken, v);
		    }
		}
	    }
	}  catch (Exception e) {	    
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "No action will be taken. Details", e);
	    // do nothing
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
    }

    private void checkGetResponse(SrmPrepareToGetResponse result, TSURLInfo urlInfo) {
	if (result == null) {
	    throw new TSRMException("No response from remote SRM.", false);
	}
	
	_reqToken = result.getRequestToken(); // needed to set value here so abort() can refer to it
	
	String tokenAssigned = "assigned reqToken="+_reqToken;
	if (_reqToken != null) {
	    tokenAssigned += " "+_reqToken;
	} 
	
	String srcStr = "src="+urlInfo;
	if (urlInfo != null) {
	    srcStr += urlInfo.getSURL().toString();
	} 
	
	if (result.getReturnStatus() != null) {
	    srcStr += " code="+result.getReturnStatus().getStatusCode() + " exp="+result.getReturnStatus().getExplanation();
	}
	TSRMLog.info(this.getClass(), null, "event=callSrmPrepareToGet()", "tokenAssigned="+tokenAssigned+" src=\""+srcStr+"\"");

	if (result.getReturnStatus() != null) {
	    TStatusCode resultCode = result.getReturnStatus().getStatusCode();
	    if ((resultCode == TStatusCode.SRM_FAILURE) || 
		(resultCode == TStatusCode.SRM_AUTHENTICATION_FAILURE) || 
		(resultCode == TStatusCode.SRM_AUTHORIZATION_FAILURE) || 
		(resultCode == TStatusCode.SRM_INVALID_REQUEST) || 
		(resultCode == TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) || 
		(resultCode == TStatusCode.SRM_EXCEED_ALLOCATION) || 
		(resultCode == TStatusCode.SRM_NO_USER_SPACE) || 
		(resultCode == TStatusCode.SRM_NO_FREE_SPACE) || 
		(resultCode == TStatusCode.SRM_NOT_SUPPORTED) || 
		(resultCode == TStatusCode.SRM_INTERNAL_ERROR))
	    {
		throw new TSRMExceptionRemoteStatus("remote prepareToGet("+_reqToken+") failed.", result.getReturnStatus(), false);
	    }
	}
    }

    public static URI makeGlobusCogCompatible(URI input) {
	if (input == null) {
	    return input;
	}

	if (!input.getPath().startsWith("//")) {
	    try {
		input.setPath("/"+input.getPath()); // no need "//" since URI contains one already
		TSRMLog.debug(TSRMContactRemoteSRM.class, null, "event=convertedUri", "uri=\""+input.toString()+"\"");
	    } catch (Exception e) {		
		TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    }
	}

	return input;
    }

    public URI getOneFile(TSURLInfo urlInfo, String token) {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	TSRMLog.info(this.getClass(), null, "event=callSrmPrepareToGet()", null);
	String error = null;
	try {
	    SrmPrepareToGetRequest r = new SrmPrepareToGetRequest();
	    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
		
	    fileReqList[0] = new TGetFileRequest();
	    
	    fileReqList[0].setSourceSURL(urlInfo.getSURL());
	    r.setStorageSystemInfo(urlInfo.getStorageSystemInfo());

	    //fileReqList[0].setSpaceToken(token);
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	   
	    r.setTransferParameters(TSRMUtil.createDefaultTxfParameters());
	                                     
	    SrmPrepareToGetResponse result = _srmStubHolder.getStub().srmPrepareToGet(r);

	    checkGetResponse(result, urlInfo);
	  
	    //
	    // try to call getStatus periodically
	    //
	    if (result.getArrayOfFileStatuses() == null) {
		error = "Error: Empty status array from srmPrepareToGet()";
	    } else {
		TGetRequestFileStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
		while (true) {
		    if (status == null) {
			error = "Error, failed to get status of the remote Get Request";
			throw new TSRMException(error, false);
		    }
		    if ((status.getStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS) ||
			(status.getStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED))
		    {
			Thread.sleep(10000);
			tt.extend(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
			status = SRMClient.checkGetStatus(_srmStubHolder.getStub(), urlInfo.getSURL(), result.getRequestToken());
		    } else {
			//if (status.getStatus().getStatusCode() == TStatusCode.SRM_FILE_PINNED) {
			if (status.getTransferURL() != null) {
			    return  makeGlobusCogCompatible(status.getTransferURL());
			} else if (status.getStatus().getStatusCode() == TStatusCode.SRM_ABORTED) {			
			    return null;
			}
			break;
		    }
		}
		error  = "code="+status.getStatus().getStatusCode().toString();
		error += " exp=\""+status.getStatus().getExplanation()+"\"";
	    }
	} catch (java.rmi.RemoteException e) {
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);	   
	    throw new TSRMException(e.getMessage(), false);
	} catch (java.lang.InterruptedException e) {
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    throw new TSRMException(e.getMessage(), false);
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    throw new TSRMException(e.getMessage(), false);
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
	
	//if (error != null) {
	throw new TSRMException(error, false);
	//}
	//return null;
    }

    public void srmPutDone(URI siteURL) {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	TSRMLog.info(this.getClass(), null, "event=callingSrmPutDone()", null);
	SrmPutDoneRequest req = new SrmPutDoneRequest();
	req.setArrayOfSURLs(TSRMUtil.convertToArray(siteURL));
	req.setRequestToken(_reqToken);
	try {
	    SrmPutDoneResponse result = _srmStubHolder.getStub().srmPutDone(req);
	    TSRMLog.debug(this.getClass(), null, "event=putDoneResult", ""+result); 

	    if ((result == null) || (result.getArrayOfFileStatuses() == null)) {
		TSRMLog.info(this.getClass(), null, "event=putDoneFailed", null);
	    } else {
		TSRMLog.info(this.getClass(), null, "event=putDoneResult", 
			     "statusCode="+result.getReturnStatus().getStatusCode()+" exp="+result.getReturnStatus().getExplanation()); 
		TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
		TSRMLog.info(this.getClass(), null, "event=putDone", "surl[0]=\""+ status.getSurl().toString()+"\""+" statusCode="+status.getStatus().getStatusCode());
	    }
	} catch (Exception e) {
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);	    
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
    }

    private SrmPrepareToPutResponse getPutFileResponse(/*TSRMLocalFile*/ISRMTxfEndPoint from, TSURLInfo tgt, String token, 
						       Integer fileLifetime, TOverwriteMode mode, TFileStorageType stor) 
    {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
	SrmPrepareToPutRequest r = new SrmPrepareToPutRequest();
	
	try {					
	    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
	    fileReqList[0] = new TPutFileRequest();
	    
	    fileReqList[0].setExpectedFileSize(TSRMUtil.createTSizeInBytes(from.getSize()));	    	    
	    fileReqList[0].setTargetSURL(tgt.getSURL());	    	    

	    //fileReqList[0].setSpaceToken(token);

	    r.setStorageSystemInfo(tgt.getStorageSystemInfo());
	    r.setTargetSpaceToken(token);	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    //r.setOverwriteOption(TOverwriteMode.ALWAYS);
	    r.setOverwriteOption(mode);

	    r.setTransferParameters(TSRMUtil.createDefaultTxfParameters());
	    r.setDesiredFileLifeTime(fileLifetime);
	    r.setDesiredFileStorageType(stor);
	    
	    SrmPrepareToPutResponse result = _srmStubHolder.getStub().srmPrepareToPut(r);
	    
	    return result;
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMContactRemoteSRM.class, "details", e);
	    throw new TSRMException(e.getMessage(), false);
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}	    
    }

    public void checkPutResponse(SrmPrepareToPutResponse result) {
	if (result == null) {
	    throw new TSRMException("Unexpected error, remote SRM returns NULL on put().", false);
	}

	TSRMLog.debug(this.getClass(), null, "event=prepareToPutRemotelyResponse"+" code="+result.getReturnStatus().getStatusCode(), 
		     "exp="+result.getReturnStatus().getExplanation()+"  token="+result.getRequestToken());

	TPutRequestFileStatus[] arrayOfResult = result.getArrayOfFileStatuses().getStatusArray();
	for (int i=0; i<arrayOfResult.length; i++) {
	    TPutRequestFileStatus fileStatus = arrayOfResult[i];
	    TSRMLog.debug(this.getClass(), null, " event=FileStatus_"+i+"th code="+fileStatus.getStatus().getStatusCode(), "exp="+fileStatus.getStatus().getExplanation());
	}

	_reqToken = result.getRequestToken();
	
	if (_reqToken == null) {
	    throw new TSRMExceptionRemoteStatus("remote prepareToPut() sends error", result.getReturnStatus(), false);
	}
	
	if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_FAILURE) {
	    throw new TSRMExceptionRemoteStatus("remote prepareToPut("+_reqToken+") failed", result.getReturnStatus(), false);
	}
	
	if (result.getArrayOfFileStatuses() == null) {
	    throw new TSRMException("Unexpected error, Bad response format from Remote Server.", false);
	}
    }
    /*
    private void doUpload(URI turl, TSRMLocalFile localFile) {
	TSupportedURL.upload(turl, localFile, _cred);
    }
    */
    

    public void putOnePath(/*TSRMLocalFile*/ISRMTxfEndPoint from, TSURLInfo tgt, String token, Integer lifetime, TOverwriteMode mode,TFileStorageType stor) {
	TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);

	try {
	    TSRMLog.info(this.getClass(), null, "event=prepareToPutFileRemotely", 
			 "tgt=\""+ tgt.getSURL().toString()+ "\" lifetime="+lifetime+"  overwriteMode="+mode+" token="+token+" stor="+stor); 

	    SrmPrepareToPutResponse result = getPutFileResponse(from, tgt, token, lifetime, mode, stor);
	    checkPutResponse(result);
	    
	    TPutRequestFileStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
	    
	    TSRMLog.info(this.getClass(), null, "event=prepareToPutRemotely", 
			 "assignedReqToken="+_reqToken+" from="+from.getPathStr()+", to="+tgt.getSURL());
	    
	    TStatusCode code = status.getStatus().getStatusCode();
	    URI siteUrl = status.getSURL();
	    while (true) {
		if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED)) {		
		    try {
			Thread.sleep(10000);
			tt.extend(TSRMTimedTask._REMOTE_CALL_TIME_OUT_SECONDS);
			status = SRMClient.checkPutStatus(_srmStubHolder.getStub(), siteUrl, _reqToken);
		    } catch (Exception e) {
			throw new TSRMException(e.getMessage(), false);
		    }
		    if (status == null) {
			throw new TSRMException("Unexpected error, no sensable put status.",false);
		    }
		    code = status.getStatus().getStatusCode();
		} else {
		    TSRMLog.info(this.getClass(), null, "event=finalStatusFromUploadingTo","tgt="+tgt.getSURL()+" statusCode="+code.toString()+ " txfUrl="+status.getTransferURL());
		    if (status.getTransferURL() != null) {
			//doUpload(makeGlobusCogCompatible(status.getTransferURL()), from);
			from.uploadTo(makeGlobusCogCompatible(status.getTransferURL()), _cred);
			srmPutDone(siteUrl);
			return;
		    } else {
			// failed
			break;
		    }
		}
	    }	    
	    
	    throw new TSRMExceptionRemoteStatus("remote prepareToPut("+_reqToken+") failed.", status.getStatus(), false);
	} finally {
	    tt.setFinished();
	    tt.alarm();
	}
    }

}

