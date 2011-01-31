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

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.storage.*;
 
import gov.lbl.srm.util.*;
import org.ietf.jgss.GSSCredential;
//import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import EDU.oswego.cs.dl.util.concurrent.Mutex; 
import org.apache.axis.types.*;

//import gov.lbl.srm.util.TSRMUtil;

import java.util.*;

public abstract class TUserRequest implements Comparable {
    private String _strUserReqDescription;
    private TAccount _owner;
    Mutex 		  _statusMutex  = new Mutex();
    private String _strID = null;
    private GSSCredential _cred = null;
    
    protected HashMap _requestCollection = new HashMap(); 
    private TSRMTxfProtocol[] _transferProtocols = null;
    private ArrayOfTExtraInfo	_storageSystemInfo = null;
    private int _retryPeriod = 0; // no retry by default

    private java.util.Calendar _creationTime = null;
    private long _startTimeInMilliSeconds = 0;
    private TReturnStatus _returnStatus = TSRMUtil.createReturnStatusQueued(null);
    
    protected Mutex _requestCollectionMutex = new Mutex();
    
    public TUserRequest(String userRequestDescription, TAccount owner) {
	_strUserReqDescription = userRequestDescription;
	_owner = owner;
	_strID = owner.generateRequestToken();
	_creationTime = java.util.Calendar.getInstance();
	//_owner.addRequest(this);
	
	if (_startTimeInMilliSeconds == 0) {
	    _startTimeInMilliSeconds = System.currentTimeMillis();
	}
    }
    
    public int compareTo(Object obj) {
	if (obj != this) {
	    TUserRequest r = (TUserRequest)obj;
	    //return getID().compareTo(r.getID());
	    return getCreationTime().compareTo(r.getCreationTime());
	}
	return 0;
    }

    public void decorateID(String type) {
	if (TSRMServer._hashedServerTag != null) {
	    _strID +="_"+type+"_"+TSRMServer._hashedServerTag;
	} else {
	    _strID +="_"+type+"_";
	}
	_owner.addRequest(this);
	TSRMLog.info(this.getClass(), null, "event=start", "rid="+_strID);
    }

    public boolean matchesDesc(String d) {
	if (d == null) {
	    return true;
	}	
	if (_strUserReqDescription == null) {
	    return false;
	}	
	return _strUserReqDescription.equals(d);
    }
    
    public void setGSSCredential(GSSCredential cred) {
	TSRMLog.debug(this.getClass(), null, "event=gssCredential val="+cred, "rid="+_strID);
	_cred = cred;
    }
    
    public GSSCredential getGSSCredential() {
	return _cred;
    }
	
    public String getID() {
	return _strID;
    }
    
    public java.util.Calendar getCreationTime() {
	return _creationTime;
    }

    public TAccount getOwner() {
	return _owner;
    }
   
    public boolean checkNewKey(URI surl) {
	if (surl == null) {
	    TSRMUtil.startUpInfo("!! found null key in: "+getID());
	    return false;
	}
	if (get(surl) != null) {
	    TSRMUtil.startUpInfo("!! found duplicated key:"+surl+" in: "+getID());
	    return false;
	}
	return true;
    }

    public void add(String siteUrl, TSRMRequest req){
	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    this._requestCollection.put(siteUrl, req);
	    TSRMLog.debug(this.getClass(), null, "event=add url=\""+siteUrl+"\" size="+_requestCollection.size(), "rid="+getID());
	    ///req.setPosInReq(_requestCollection.size());
	    TSRMUtil.releaseSync(_requestCollectionMutex);
	}
    }
    
    public TSRMRequest get(URI surl) {
	Object obj = _requestCollection.get(surl.toString());

	if (obj == null) {// this is needed if surl uses double slashes to start the path
	    try {
	    TSupportedURL uri = TSupportedURL.create(TSRMUtil.createTSURLInfo(surl), false);
	    obj = _requestCollection.get(uri.getURI().toString());
		} catch (Exception e) {
			// ignore
		}
	}
	return (TSRMRequest)obj;
    }
    
    protected void printContent() {
	TSRMLog.debug(this.getClass(), null, "event=printContent rid="+getID(), "total="+_requestCollection.size());

	Set keys = _requestCollection.keySet();	
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {	    
	    String curr = (String)(iter.next());
	    TSRMLog.debug(this.getClass(), null, "event=printContent item=\""+curr+"\"", null);
	}
    }

    protected TSURLReturnStatus[] releaseAll(boolean willKeepSpace) {
	TSRMLog.debug(this.getClass(), null, "event=releaseAll willKeepSpace="+willKeepSpace, "size="+_requestCollection.size());
	if (_requestCollection.size() == 0) {
	    return null;
	}

	TSURLReturnStatus[] result = new TSURLReturnStatus[_requestCollection.size()];	
	int counter = 0;

	Set keys = _requestCollection.keySet();	
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    TSURLReturnStatus releaseStatusOfCurrSURL = new TSURLReturnStatus();
	    String curr = (String)(iter.next());
	    try {
		releaseStatusOfCurrSURL.setSurl(new URI(curr));
	    } catch (Exception e) {		
		TSRMLog.exception(TUserRequest.class, "details", e);
	    }

	    TSRMRequest r = (TSRMRequest)(_requestCollection.get(curr));
	    releaseStatusOfCurrSURL.setStatus(r.release(willKeepSpace));
	    result[counter] = releaseStatusOfCurrSURL;
	    counter ++;
	}

	return result;
    }

    public TSURLReturnStatus[] release(URI[] surlArray, boolean willKeepSpace) {
	if (surlArray == null) {
	    TSRMUtil.acquireSync(_requestCollectionMutex);
	    try {
		return releaseAll(willKeepSpace);
	    } finally {
		TSRMUtil.releaseSync(_requestCollectionMutex);
	    }
	}
	
	TSURLReturnStatus[] result = new TSURLReturnStatus[surlArray.length];
	
	for (int i=0; i<surlArray.length; i++) {
	    URI curr = surlArray[i];
	    TSRMLog.debug(this.getClass(), null, "event=releaseFile surl=\""+curr.toString()+ "\"", "willKeepSpace="+willKeepSpace);
	    TSRMRequest r = get(curr);
	    
	    TSURLReturnStatus releaseStatusOfCurrSURL = new TSURLReturnStatus();
	    releaseStatusOfCurrSURL.setSurl(curr);
	    if (r != null) {
		releaseStatusOfCurrSURL.setStatus(r.release(willKeepSpace));
	    } else {
		releaseStatusOfCurrSURL.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, 
									      TSRMUtil._DefNoSuchFileRequest));
	    }
	    result[i]=releaseStatusOfCurrSURL;
	}
	return result;
    }   
    
    public int setTransferProtocols(ArrayOfString protocolArray) {	 
	if (protocolArray== null) {
	    _transferProtocols = new TSRMTxfProtocol[1];
	    _transferProtocols[0] = TSRMTxfProtocol.getDefaultTransferProtocol();
	    return 1;
	}
	
	String[] protocols = protocolArray.getStringArray();
	Vector temp = new Vector(protocols.length);
	for (int i=0; i<protocols.length; i++) {
	    String curr = protocols[i];
		TSRMTxfProtocol p = TSRMTxfProtocol.getProtocol(curr);
		if (p != null) {
			if (p.isEnabled()) {
				temp.add(p);
			}
		}
	}
	
	if (temp.size() == 0) {
	    return 0;
	}

	_transferProtocols = new TSRMTxfProtocol[temp.size()];
	for (int i=0; i<temp.size(); i++) {
	    Object curr = temp.get(i);
	    _transferProtocols[i] = (TSRMTxfProtocol)curr;
	}
	return temp.size();
    }
    
    //public TSRMTxfProtocol[] getTransferProtocols() {
    //return _transferProtocols;
    //}

    public TSRMTxfProtocol getTxfProtocol() {
	if (_transferProtocols != null) {
	    return _transferProtocols[0];
	}
	//return TSRMTxfProtocol.getDefaultUploadProtocol();
	return TSRMTxfProtocol.getDefaultTransferProtocol();
    }
    
    public void setStorageSystemInfo(ArrayOfTExtraInfo input) {
	if (input != null) {
	    //_storageSystemInfo = input.getExtraInfoArray();
	    _storageSystemInfo = input;
	}
    }
    
    public ArrayOfTExtraInfo getStorageSystemInfo() {
	return _storageSystemInfo;
    }
    
    public void setRetryPeriod(Integer r) {
	if (r!= null) {
	    _retryPeriod = r.intValue()*1000;
	    TSRMLog.debug(this.getClass(), null, "event=setRetry rid="+getID(), "waitSeconds="+_retryPeriod);
	}
    }
    public long getRetryPeriod() {
	return _retryPeriod;
    }
    
    public TStatusCode getWLCGStatus() {
	TRequestSummary result = getSummary();
	int waiting = result.getNumOfWaitingFiles().intValue();
	int failed = result.getNumOfFailedFiles().intValue();
	int finished = result.getNumOfCompletedFiles().intValue();

	if (finished == 0) {
	    return TStatusCode.SRM_REQUEST_QUEUED;
	}
	if (failed == 0) {
	    if (waiting == 0) {
		return TStatusCode.SRM_SUCCESS;
	    } else {
		return TStatusCode.SRM_REQUEST_INPROGRESS;
	    }
	}
	
	if (waiting == 0) {
	    if (failed == finished) {
		return TStatusCode.SRM_FAILURE;
	    } else {
		return TStatusCode.SRM_PARTIAL_SUCCESS;
	    }
	} else {
	    return TStatusCode.SRM_REQUEST_INPROGRESS;
	}
	
    }
    public TRequestSummary getSummary() {
	TRequestSummary result = new TRequestSummary();
	result.setRequestType(this.getType());
	result.setTotalNumFilesInRequest(new Integer(_requestCollection.size()));
	if (isSuspended()) {
	    //result.setIsSuspended(isSuspended());
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_SUSPENDED, "The entire request is suspended"));
	    return result;
	} else if (isTimedOut()) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_TIMED_OUT, "The entire request is timed out"));
	    return result;
	} else if (isAborted()) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_ABORTED, "The entire request is aborted."));
	    return result;
	} 	    	
	
	int nQueued = 0;
	int nProgress = 0;
	int nFinished = 0;
	int nFailed = 0;
	
	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    HashSet reqSet = new HashSet(_requestCollection.values());
	    //result.setTotalNumFilesInRequest(new Integer(reqSet.size()));

	    Iterator iter = reqSet.iterator();
	    while (iter.hasNext()) {
		TSRMRequest r = (TSRMRequest)(iter.next());
		if (r.isQueued()) {
		    nQueued++;
		} else if (r.isFinished()) {
		    nFinished++;
		    if (r.isFailed()) { 
			nFailed++;
		    }
		} else {
		    nProgress++;
		}
	    }
	    TSRMUtil.releaseSync(_requestCollectionMutex);				  
	}
	

	result.setNumOfCompletedFiles(new Integer(nFinished));
	result.setNumOfWaitingFiles(new Integer(nProgress+nQueued));
	result.setNumOfFailedFiles(new Integer(nFailed));				 

	result.setStatus(TSRMUtil.generateStatus(nFinished, nProgress+nQueued, nFailed));

	return result;
    }
    
    public void setSuspend() {
	setReturnStatus(TSRMUtil.createReturnStatusSuspended(null));	
    }
    
    public void resume() {
	setReturnStatus(TSRMUtil.createReturnStatusQueued(null));
	
	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    HashSet reqSet = new HashSet(_requestCollection.values());
	    Iterator iter = reqSet.iterator();
	    while (iter.hasNext()) {
		TSRMRequest curr = (TSRMRequest)(iter.next());
		curr.resume();
	    }
	    TSRMUtil.releaseSync(_requestCollectionMutex);
	}
    }
    
    protected boolean isSuspended() {
	return (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_SUSPENDED);
    }
	
    private boolean isTimedOut() {
	return (_returnStatus.getStatusCode() == TStatusCode.SRM_REQUEST_TIMED_OUT);
    }

    public boolean isAborted() {
	return (_returnStatus.getStatusCode() == TStatusCode.SRM_ABORTED);
    }
    public boolean isTerminated() {
	checkWithTotalRequestTime();
	return (isSuspended() || isTimedOut() || isAborted());
    }

    public void fileAbortCalled() {
	HashSet reqSet = new HashSet(_requestCollection.values());
	Iterator iter = reqSet.iterator();

	while (iter.hasNext()) {
	    TSRMRequest curr = (TSRMRequest)(iter.next());
	    if (!curr.isAborted()) {
		return;
	    }
	}
	TSRMLog.debug(this.getClass(), null, "event=detectedAllFilesAborted", "action=requestIsSetToAbort");
	setReturnStatus(TSRMUtil.createReturnStatusAborted(null));      
    }

    public TReturnStatus abort() {
	//setReturnStatus(TSRMUtil.createReturnStatusAborted(null));
	if (isAborted()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "already aborted before.");
	}
	TReturnStatus abortStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "failed to lock the mutex to do abort.");

	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    HashSet reqSet = new HashSet(_requestCollection.values());
	    Iterator iter = reqSet.iterator();

	    int numFailedAbort = 0;
	    while (iter.hasNext()) {
		TSRMRequest curr = (TSRMRequest)(iter.next());
		curr.abort(null, false);
		if (curr.getReturnStatus().getStatusCode() != TStatusCode.SRM_ABORTED) {
		    numFailedAbort ++;
		}
	    }
	    if  (numFailedAbort == 0) {
		setReturnStatus(TSRMUtil.createReturnStatusAborted(null));      
		abortStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "All aborted.");
	    } else if (numFailedAbort== _requestCollection.size()) {
		abortStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "All files failed to abort");
	    } else {
		abortStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_PARTIAL_SUCCESS, "Some aborted, some failed.");
	    }
	    TSRMUtil.releaseSync(_requestCollectionMutex);
	}
	return abortStatus;
    }
	
    public boolean showSizeInStatus(TReturnStatus status) {
	if ((status.getStatusCode() != TStatusCode.SRM_FAILURE) && 
	    (status.getStatusCode() != TStatusCode.SRM_NOT_SUPPORTED) &&
	    (status.getStatusCode() != TStatusCode.SRM_REQUEST_QUEUED) && 
	    (status.getStatusCode() != TStatusCode.SRM_SPACE_AVAILABLE))
	{
	    return true;
	}
	return false;
    }

    public void verifyConsistency(TSRMSpaceType suggestedType, String suggestedToken) {
	if ((suggestedToken == null) || (suggestedType == null)) {
	    return;
	}
	
	TSRMStorage.iSRMSpaceToken token = getOwner().getToken(suggestedToken);
	if (token == null) {
	    throw new TSRMExceptionNoSuchSpace(suggestedToken);
	}
	if (token.getType() != suggestedType) {
	    throw new TSRMExceptionStorageTypeConflict(suggestedType, suggestedToken);
	}	    
    }

    public TReturnStatus checkStorageConsistency(String suggestedToken, TRetentionPolicyInfo suggestedInfo) {
	TSRMLog.debug(this.getClass(), null, "event=CheckStorageConsistency", "suggestedToken="+suggestedToken+" suggestedInfo="+suggestedInfo);
	if (suggestedToken == null) { 
	    return null;
	}
	if (TSRMUserSpaceTokenSupport.match(suggestedToken) != null) {
	    TSRMLog.debug(this.getClass(), null, "token="+suggestedToken, "isUserToken=true");
	    return null;
	}

	TSRMStorage.iSRMSpaceToken token = getOwner().getToken(suggestedToken);
	if ((token == null) || (token.isReleased())) {
	    return TSRMUtil.createReturnStatus(new TSRMExceptionNoSuchSpace(suggestedToken));
	}
	if (token.isLifeTimeExpired()) {
	    return TSRMUtil.createReturnStatus(new TSRMExceptionSpaceTimedOut(suggestedToken));
	}

	if ((suggestedInfo != null) && !token.getType().matches(suggestedInfo)) {
	    return TSRMUtil.createReturnStatus(new TSRMExceptionStorageTypeConflict(suggestedInfo, suggestedToken));
	}
	return null;
    }
   
    public TReturnStatus getReturnStatus() {
	TReturnStatus result = null;
	if (TSRMUtil.acquireSync(_statusMutex)){
	    result = _returnStatus;
	    TSRMUtil.releaseSync(_statusMutex);
	}
	return result;
    }

    public void setReturnStatus(TReturnStatus s) {
	if (TSRMUtil.acquireSync(_statusMutex)){
	    if (_returnStatus.getStatusCode() != s.getStatusCode()) {
		_returnStatus = s;
	    }	
	    TSRMUtil.releaseSync(_statusMutex);
	}
    }

    public boolean checkWithTotalRequestTime() {
	if (isTimedOut()) {
	    return false;
	}

	if (_retryPeriod <= 0) {
	    return true;
	}

	long timeNow = System.currentTimeMillis();
	long timeElapsed = timeNow - _startTimeInMilliSeconds;

	if (timeElapsed > _retryPeriod) {
	    TSRMLog.debug(this.getClass(), null, "event=checkTotalTime result=RequestTimedOut", "rid="+getID());
	    setReturnStatus(TSRMUtil.createReturnStatusTimedOut(null));
	    return false;
	}
	return true;
    }
    
    public boolean isReleasable() {
	if (getType() == TRequestType.PREPARE_TO_GET) {
	    return true;
	} else if (getType() == TRequestType.BRING_ONLINE) {
	    return true;
	} 
	return false;  
    }

    public TReturnStatus checkFileTypeInconsistency(String userProvidedToken, TFileStorageType type) {
	if (userProvidedToken == null) {
	    return null;
	}
	if (type == null) {
	    return null;
	}

	TSRMStorage.iSRMSpaceToken token = getOwner().getToken(userProvidedToken);
	if (token == null) {
	    return null;
	}

	if (!TSRMStorage.isFileTypeConsistantWithSpaceType(type, token.getType())) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "space type and file type mismatch.");    
	}
	return null;
    }

    public abstract TRequestType getType() ;

    //abstract Vector getStatusOfRequest(TSURL[] surls);
}

 
 
