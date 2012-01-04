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

import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.policy.TSRMGeneralEnforcement;
 
//import EDU.oswego.cs.dl.util.concurrent.Mutex;
import java.util.Vector;
import org.apache.axis.types.URI;

public abstract class TSRMRequest implements Runnable, Comparable {
    TReturnStatus _returnStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, null); // default
    TSRMMutex 		  _statusMutex  = new TSRMMutex();
    TUserRequest  _userRequest  = null;
    Vector  _bulkSet = null;
    boolean _isResponsibleToCleanupLocalFile = false;
    

    TSRMStorage.iSRMSpaceToken _spaceToken = null;
    TFileStorageType _fileStorageType = null;
    long _proposedPinDuration = TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS*1000L;
    long _retryTime = 0;
    //int _posInReq = -1; // this is the nth file req in a user request.
    String _identifier = null;

    long _queuedAtMillis = System.currentTimeMillis();

    public TSRMRequest(int pos, TUserRequest userReq) {
	TSRMGeneralEnforcement.add();
	_userRequest = userReq;
	setFID(pos);
    }

    public TSRMRequest(TSRMFileInput r, TUserRequest userReq) {
	TSRMGeneralEnforcement.add();
	_userRequest = userReq;
	setFID(r.getPos());
    }

    public void cleanMe() {
	TSRMLog.debug(this.getClass(), null, "event=cleanMe", "ref="+_identifier);
	_userRequest = null;
    }

    //
    // methods
    //
    //public abstract boolean isValid();  
    
    //public abstract TReturnStatus getReturnStatus();
    
    public abstract void abortMe(TStatusCode statudCode, boolean implicitAbort);
    public abstract void resume();
    public abstract void runMe();     
    
    public abstract TReturnStatus removeFileInCache();
    
    public abstract String description();
    
    public abstract void setReceivedBytes(long bytes);

    //public abstract TUserRequest getRequester();
    
    public abstract long getEstProcessingTimeInSeconds();
    
    public abstract long getEstWaitTimeInSeconds();
    
    public abstract ISRMLocalPath getPinnedPath();
    
    public abstract void setTrustedSize(long s);

    public abstract void cleanUp();

    public void setFID(int n) {
	//_posInReq = n;
	if (n > -1) {
	    _identifier =getRequester().getID()+" pos="+n;
	}
    }

    /*public int getPosInReq() {
	return _posInReq;
    }*/

    public String getIdentifier() {
	if (_identifier != null) {
	    return _identifier;
	} else {
	    return getRequester().getID();
	}
	//return getRequester().getID()+getPosInReq();
    }

    public boolean authorized(TSRMSource s, TSRMPermission p) {
	return authorized(s.getSourceURL(), p);
    }

    public boolean authorized(TSupportedURL url, TSRMPermission p) {
	TReturnStatus authStatus = url.authorize(p, getRequester().getOwner());
	if (authStatus == null) {
	    return true;
	} else {
	    setReturnStatus(authStatus);
	}
	return false;
    }
    
    public void setResponsibleToCleanupLocalFile(boolean isRes) {
	_isResponsibleToCleanupLocalFile = isRes;
    }

    public boolean isResponsibleToCleanupLocalFile() {
	return _isResponsibleToCleanupLocalFile;
    }

    public int compareTo(Object obj) {
	int result = 0;

	if (obj != this) {
	    TSRMRequest r = (TSRMRequest)obj;
	    long retryMe = getRetryTime();
	    long retryOther = r.getRetryTime();
	    if (retryMe < retryOther ) {
		result = -1;
	    } else if (retryMe > retryOther) {
		result = 1;
	    } else {		
		result = description().compareTo(r.description());
	    }
	}

	//TSRMLog.info(this.getClass(), this+" compareTo(", obj+")="+result, null);

	return result;
    }

    public boolean equals(Object obj) {
	if (obj == null) {
	    return false;
	}
	if (obj.getClass() != this.getClass()) {
	    return false;
	}

	TSRMRequest r = (TSRMRequest)obj;
	if (r == null) {
	    return false;
	}

	long curr = System.currentTimeMillis();
	if (r.getDelay(curr) == getDelay(curr)) {
	    if (r.getIdentifier().equals(getIdentifier())) {
		return true;
	    } else {
		return false;
	    }
	}
	return false;
    }

    public void addToBulkSet(TSRMRequest r) {
	if (r == null) {
	    return;
	}

	if (_bulkSet == null) {
	    _bulkSet = new Vector();
	}

	_bulkSet.add(r);
    }

    public long getDelay(long curr) {
	if (getRetryTime() == 0) {
	    return 0;
	}
	long delay = getRetryTime() - curr;
	if (delay <= 0) {
	    return 0;
	}
        return delay;
    }

    public long getRetryTime() {
	return _retryTime;
    }

    public void setRetryTime(long milli) {
	_retryTime = milli;
    }

    public void retry(String reason, long delayInMillis) {
	TSRMLog.info(this.getClass(), null, "event=toRescheduleIn waitTimeMillis="+delayInMillis, description()+" reason=\""+reason+"\"");
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, reason+" SRM will retry in "+delayInMillis+" millis"));
	this.setRetryTime(System.currentTimeMillis() + delayInMillis);
	TSRMServer._gProcessingUnit.submitJob(this);
    }
    
    public boolean willRelayToken() {
	return false;
    }
    
    public void run() {
	if (getRequester().isSuspended()) {
	    return;
	}
	if (isAborted()) {
	    return;
	}
	
	if (getRequester().checkWithTotalRequestTime()) {	    
	    runMe();
	}
    }
    
    public void abort(String msg, boolean implicitAbort) {
	if (!isFinished() || (getReturnStatus().getStatusCode() == TStatusCode.SRM_SPACE_AVAILABLE)) {		
	    TStatusCode statusCode = getReturnStatus().getStatusCode();
	    TSRMLog.info(this.getClass(), null, "event=Aborting", getIdentifier()+" statusCode="+statusCode);
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_ABORTED, msg));
	    if (statusCode == TStatusCode.SRM_REQUEST_SUSPENDED) {
		// no need to do anything
		return;
	    } else if (statusCode == TStatusCode.SRM_REQUEST_QUEUED) {
		if (_bulkSet != null) {
		    for (int i=0; i<_bulkSet.size(); i++) {
			TSRMRequest curr = (TSRMRequest)(_bulkSet.get(i));
			curr.abort(msg, implicitAbort);
		    }
		}
		return; // since returnStatus changed to abort_done, runMe() wont do anything
	    }
	    abortMe(statusCode, implicitAbort);
	} else {
	    TSRMLog.info(this.getClass(), null, "event=AbortHasNoEffect", getIdentifier()+" statusCode="+getReturnStatus().getStatusCode().toString());
	}
    }
    
    public void releasePinnedFile(TSRMLocalFile pinnedFile) {
	if (getRequester().getOwner() == pinnedFile.getToken().getOwner()) {
	    //pinnedFile.unsetToken(false);
	    if (!pinnedFile.isSurl()) {
		if (TSRMLog.getCacheLog() != null) {
		    TSRMLog.getCacheLog().removeFile(pinnedFile);
		}
	    } else {
		pinnedFile.getToken().fileIsReleased(pinnedFile);
		TSRMLog.info(this.getClass(), null, "event=releaseSurl msg=pls_call_srmRm()_to_remove_an_surl.", description());
	    }
	} else {
	    pinnedFile.getToken().fileIsReleased(pinnedFile);
	    TSRMLog.info(this.getClass(), null, "event=release msg=Requester_does_not_own_the_token.", description());
	}
    }
    
    public TReturnStatus releaseFile(boolean willKeepSpace) {
	ISRMLocalPath p = getPinnedPath();

	if (p == null) {
	    setStatusReleased(null);
	    return getReturnStatus();
	}

	if (p.isDir()) {
	    return TSRMUtil.createReturnStatusFailed("Not supporting releasing a dir yet.");
	}

	TSRMLocalFile pinnedFile = (TSRMLocalFile)(p);

	/*
	if (pinnedFile == null) {
	    return TSRMUtil.createReturnStatusFailed("File is not there.");
	}
	*/
	
	pinnedFile.unpin(this);
	if (pinnedFile.isSurl()) {
	    setStatusReleased(null);
	    return TSRMUtil.createReturnStatusSuccess(null);
	    //return getReturnStatus();
	}

	//pinnedFile.unpin(this);
	if (!willKeepSpace || pinnedFile.getToken().isDynamicCompactOn()) {
	    releasePinnedFile(pinnedFile);
	}
	
	setStatusReleased(null);
	
	return TSRMUtil.createReturnStatusSuccess(null);
    }
    
    public TReturnStatus release(boolean willKeepSpace) {
	if ((getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS) || 
	    (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED))
	{
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefInProgress);
	}
	
	if ((getReturnStatus().getStatusCode() != TStatusCode.SRM_FILE_PINNED) &&  
	    (getReturnStatus().getStatusCode() != TStatusCode.SRM_FILE_LIFETIME_EXPIRED) && 
	    (getReturnStatus().getStatusCode() != TStatusCode.SRM_SUCCESS)) 
	{
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
					       "Cannt release file request. curr file status:"+getReturnStatus().getStatusCode());
	}
	
	return releaseFile(willKeepSpace);
    }
    
    public void setStatusReleased(String msg) {
	TReturnStatus releaseOK = TSRMUtil.createReturnStatus(TStatusCode.SRM_RELEASED, msg);
	setReturnStatus(releaseOK);
    }

    public void setStatusInProgress() {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_INPROGRESS, null));
    }
    
    public boolean isCodeAllowed(TStatusCode code) {
	return true;
    }

    public boolean isQueued() { // for request summary
	return (this.getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED);
    }

    public boolean isPinned() { // for request summary
	return (this.getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_PINNED);
    }
    
    public boolean isSucc() {
	return (this.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS);
    }
    
    public boolean isInProgress() { // for request summary
	TStatusCode statusCode = getReturnStatus().getStatusCode();
	return ((statusCode == TStatusCode.SRM_REQUEST_INPROGRESS) ||
		//(statusCode == TStatusCode.SRM_FILE_PINNED) || 
		(statusCode == TStatusCode.SRM_FILE_IN_CACHE) 
		//(statusCode == TStatusCode.SRM_SPACE_AVAILABLE) ||	
		//(statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED)
		);
    }

    public boolean isSubjectToRelease() {
	TStatusCode statusCode = getReturnStatus().getStatusCode();

	return ((statusCode == TStatusCode.SRM_FILE_PINNED) || 		
		(statusCode == TStatusCode.SRM_DONE) || 
		(statusCode == TStatusCode.SRM_FAILURE) || 
		(statusCode == TStatusCode.SRM_SUCCESS) || 
		(statusCode == TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) ||
		(statusCode == TStatusCode.SRM_INVALID_PATH) || 
		(statusCode == TStatusCode.SRM_INVALID_REQUEST) || 
		(statusCode == TStatusCode.SRM_INTERNAL_ERROR) || 
		(statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED));
    }
    
    public boolean isAborted() {
	TStatusCode statusCode = getReturnStatus().getStatusCode();
	return (statusCode == TStatusCode.SRM_ABORTED);
    }

    public boolean isReleased() {
	TStatusCode statusCode = getReturnStatus().getStatusCode();
	return (statusCode == TStatusCode.SRM_RELEASED);
    }
    
    public boolean isFinished() {
	return ((!isQueued()) && (!isInProgress()));
    }

    public boolean isFailed() {
	if (isQueued()) {
	    return false;
	}

	if (isInProgress()) {
	    return false;
	}

	TStatusCode statusCode = getReturnStatus().getStatusCode();

	if ((statusCode == TStatusCode.SRM_SUCCESS) ||
	    (statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) ||
	    (statusCode == TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) || 
	    (statusCode == TStatusCode.SRM_REQUEST_SUSPENDED) ||
	    (statusCode == TStatusCode.SRM_RELEASED) ||
	    (statusCode == TStatusCode.SRM_FILE_PINNED) ||
	    (statusCode == TStatusCode.SRM_FILE_IN_CACHE) || 
	    (statusCode == TStatusCode.SRM_SPACE_AVAILABLE) || 
	    (statusCode == TStatusCode.SRM_CUSTOM_STATUS)) 
        {
	    return false;
	}
	return true;	    
    }
    
    public TSURLLifetimeReturnStatus extendTURLLifeTime(URI uri, Integer newLifeTimeObj) {
	long newLifeTimeInMillis = TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS*1000;
	if (newLifeTimeObj != null) {
	    if (newLifeTimeObj.intValue() >= 0) {
		newLifeTimeInMillis = newLifeTimeObj.intValue()*1000;
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=extendTURLLifeTime action=usingDefaults", "invalid_input="+newLifeTimeObj.intValue());
	    }
	}
	
	if (getPinnedPath() == null) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "No turl!");
	} 

	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_RELEASED) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_RELEASED, "No extend life time for released requests");
	}
	if (getSpaceToken() == null) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "File is not in space yet, cannt extend lifetime.");
	}
	if (getSpaceToken().isLifeTimeExpired()) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_FILE_LIFETIME_EXPIRED, "Space lifetime expired. Cannt extend file lifetime.");
	}
	
	TSRMPin p = getPinnedPath().getPin(this);
	if (p == null) {
	    if (isFailed()) {
		return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "Cannot extendLifeTime for failed requests!");
	    }
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "Error in extendLifeTime, no valid pin!");
	}

	//long lifeTimeLimit = TSRMNameSpace.getLifetimeMillis(uri);
	Object obj = TSRMNameSpace.getPrimaryCopy(uri);
	if (obj.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
		TExtraInfo[] dummyInfo = new TExtraInfo[1];
		dummyInfo[0] = new TExtraInfo(); dummyInfo[0].setKey("none"); dummyInfo[0].setValue("none");
		ArrayOfTExtraInfo dummyInfoArray = TSRMUtil.convertToArray(dummyInfo);
	    TSupportedURL temp = TSupportedURL.createAndCheckSiteFileName(new TSURLInfo(uri, dummyInfoArray), getRequester().getOwner(), null, true);
	    if (temp == null) {
		TReturnStatus err = (TReturnStatus)obj;
		return TSRMUtil.createTSURLLifetimeReturnStatus(null, err.getStatusCode(), err.getExplanation());
	    } else {
		obj = TSRMNameSpace.getPrimaryCopy(temp.getURI());
		if (obj.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
		    TReturnStatus err = (TReturnStatus)obj;
		    return TSRMUtil.createTSURLLifetimeReturnStatus(null, err.getStatusCode(), err.getExplanation());
		}
	    }
	}
	long lifeTime = ((TSRMLocalFile)obj).getExpirationTimeInMilliSeconds();

	if (lifeTime > 0) {
	    long diff = lifeTime - System.currentTimeMillis();
	    if (diff < 0) {
		return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_FILE_LIFETIME_EXPIRED, "surl expired "+diff+"millis ago");
	    }
	    if (diff < newLifeTimeInMillis) {
		newLifeTimeInMillis = diff;
	    }	    
	}

	if (p.isExpired()) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "pin is expired. cannt extend");
	}
	
	boolean isExtended = p.extend(newLifeTimeInMillis);
	if (!isExtended) {
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_EXCEED_ALLOCATION, 
							    "no more extensions allowed");
	} else {
	    long remainningLT = p.getTimeRemainsInMilliSeconds();
	    TSURLLifetimeReturnStatus result = TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_SUCCESS, null);
	    if (remainningLT > 0) {
		int remainningLTInSeconds = (int)(remainningLT/1000);
		result.setPinLifetime(new Integer(remainningLTInSeconds));
	    } else {
		result.getStatus().setExplanation("no limit");
	    }
	    return result;
	}
    }

    public Integer getRemainingPinTime() {	
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_PINNED) {			
	    //TSRMPin p = _source.getLocalPath().getPin(this);
	    if (getPinnedPath() == null) {
		return null;
	    }
	    TSRMPin p = getPinnedPath().getPin(this);
	    if (p == null) {
		RuntimeException ex = new RuntimeException("UnexpectedError of request: "+description()+" in getRemainingPinTime, pin is gone!");
		TSRMLog.exception(this.getClass(), "details:", ex);
		//throw ex;
		return null;
	    }
	    return TSRMUtil.createTLifeTimeInSeconds(p.getTimeRemainsInMilliSeconds()/(long)1000, false);
	}
	return TSRMUtil.createTLifeTimeInSeconds(0, false);
    }
    
    public TUserRequest getRequester() {
	return _userRequest;
    }

    // assuming mutex locked
    private  void updateReturnStatus() {
	if (_returnStatus.getStatusCode() == TStatusCode.SRM_FILE_PINNED) {
	    ISRMLocalPath path = getPinnedPath();
	    if (path == null) {
		_returnStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "file is expired and removed.");
		return;
	    }
	    
	    if (path.isDir()) {
		return;
	    }

	    TSRMPin p = getPinnedPath().getPin(this);
	    if (p != null) {
		if (p.isExpired()) {
		    _returnStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED, null);
		}
	    } else {
		if (!getPinnedPath().isDir()) {
		    if ((((TSRMLocalFile)(getPinnedPath())).getPinManager()) != null) {
			_returnStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "pin is gone.");
		    }
		}
	    }
	} else if (_returnStatus.getStatusCode() == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
	    ISRMLocalPath path = getPinnedPath();
	    if (path == null) {
		_returnStatus = TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "file is gone!");
		return;
	    }	    
	}
    }

    public TReturnStatus getCurrentReturnStatus() {
	TReturnStatus result = null;
	if (TSRMUtil.acquireSync(_statusMutex)){
	    result = _returnStatus;
	    TSRMUtil.releaseSync(_statusMutex);
	}
	return result;
    }

    public TReturnStatus getReturnStatus() {

	TReturnStatus result = _returnStatus;
	if (!TSRMUtil.acquireSync(_statusMutex)){
	    return result;
	}
	try {
	    updateReturnStatus();
	    result = _returnStatus;
	} finally {
	    TSRMUtil.releaseSync(_statusMutex);
	} 
		
	return result;
    }

    public TReturnStatus getReturnStatusBulk() {
	if (_bulkSet == null) {
	    return getReturnStatus();
	}
	
	if ((_returnStatus.getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) || 
	    (_returnStatus.getStatusCode() == TStatusCode.SRM_REQUEST_INPROGRESS)) 
	{
	    boolean isDone = true;
		boolean foundFailure =false;
	    
	    for (int i=0; i<_bulkSet.size(); i++) {
		TSRMRequest curr = (TSRMRequest)(_bulkSet.get(i));

		if (!curr.isSubjectToRelease()) {
		    isDone = false;
		    break;
		} else {
			if (curr.isFailed()) {
				foundFailure = true;
			} 		    
	    }
		}
	    
	    if (isDone) {
		if (foundFailure) {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "not all files in dir are handled successfully."));
		} else {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "all files in dir are handled successfully."));
		}
	    }
	}    

	return getReturnStatus();
    }
    
    public boolean setReturnStatus(TReturnStatus s) {
	boolean result = false;

	if (!TSRMUtil.acquireSync(_statusMutex)){
	    TSRMLog.debug(this.getClass(), null, "setStatus_failed_getting_the_lock.", null);
	    return false;
	}

	try {
	    boolean wasAborted = (_returnStatus.getStatusCode() == TStatusCode.SRM_ABORTED);
	    if (!wasAborted) {
		if (_returnStatus.getStatusCode() != s.getStatusCode()) {
		    if (isCodeAllowed(s.getStatusCode())) {
			_returnStatus = s;		
		    } else {
			TSRMLog.debug(this.getClass(), null, "event=skipStatus "+description(), "status="+s.getStatusCode());			
			return true;
		    }		   
		}
		result = true;		
	    }
	} finally {
	    TSRMUtil.releaseSync(_statusMutex);
	}
	if (result) {
	    String statusStr = " statusCode="+s.getStatusCode().toString()+" exp=\""+s.getExplanation()+"\"";
	    if (isFinished() || isSubjectToRelease()) {
		statusStr += " durMillis="+(System.currentTimeMillis() - _queuedAtMillis);
	    }
	    TSRMLog.info(this.getClass(), null,  "event=reqStatus "+description(), statusStr);
	}	
	
	if (isFinished() || isSubjectToRelease()) {
	    TSRMGeneralEnforcement.minus();
	}
	
	return result;
    }
    
    public void setReturnStatusPinned(String notes) {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_PINNED, notes)); // 
    }

    public void setReturnStatusSurlExpired(String notes) {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED, notes));
    }

    public void setReturnStatusFailed(String reason) {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, reason));
    }
	
    protected void limitProposedPinDuration(long v) {
	if (_proposedPinDuration > v) {
	    _proposedPinDuration = v;
	    TSRMLog.debug(this.getClass(), null, "event=setProposedPinDuration value="+v, null);
	}
    }

    public void setProposedPinDuration(Integer v) {
	if (v != null) {
	    if (v.intValue() > 0) {
		_proposedPinDuration = v.intValue()*1000L;
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=setProposedPinDuration(0)_is_ignored", description());
	    }
	    return;
	} 
	
	if (getSpaceToken() != null) {
	    if (getSpaceToken().getType() != TSRMSpaceType.Volatile) {
		_proposedPinDuration = -1; // unlimited pin by default, to Durable and Permanent files
	    } else {
		long expirationTime = getSpaceToken().getExpirationTimeInMilliSeconds();
		java.util.Date now = new java.util.Date();

		if (expirationTime > now.getTime()) {
		    _proposedPinDuration = expirationTime - now.getTime();
		} else if (expirationTime < 0) {
		    _proposedPinDuration = TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS*1000L;
		} else {
		    throw new TSRMException(getSpaceToken().getID()+" is an expired token.", false);
		}
	    }
	}
    }
    
    public long getProposedPinDuration() {
	return this._proposedPinDuration;
    }
    
    public TSRMStorage.iSRMSpaceToken getSpaceToken() {
	return _spaceToken;
    }
        
    public TFileStorageType getFileStorageType() {
	return _fileStorageType;
    }

    public static boolean isBulkTransfer(TDirOption dirOp, TSupportedURL refurl) {
	if ((dirOp != null) && (dirOp.isIsSourceADirectory())) {
	    return true;
	} else if ((refurl != null) && (refurl.isDir())) {
	    return true;
	}
	return false;   
    }

    public void handleException(RuntimeException ex) {
	if (!isAborted()) {
	    TSRMLog.exception(this.getClass(), "rtEx, file-failed:"+description(), ex);// _source.getSourceURL().getURLString(), null);
	    //_source.detachFromLocalCache();
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, ex.getMessage()));
	    cleanUp();
	} else {
	    cleanUp();
	}
    }

    public void handleException (TSRMException e) {	
	if (isAborted()) {
	    cleanUp();
	    return;
	}

	if (e.isRetriable()) {			 
	    TSRMLog.debug(this.getClass(), null, description()+" event=WillRetry delayMillis="+e.getDelay(), "msg="+e.getMessage());
	    retry(e.toString(), e.getDelay());
	} else {
	    TSRMLog.exception(this.getClass(), "file-failed,"+description(), e);
	    setReturnStatus(e.getReturnStatus());
	    cleanUp();
	}		    
    }
    
    public ISRMLocalPath getLocalPath(TSupportedURL url, 
				      TFileType expectedType,
				      TFileStorageType givenStorageType, 
				      TOverwriteMode owMode) 
    {
	String[] pathArray = TSupportedURL.getPathInArray(url);
	
	// assuming that pathArray[0] is the account info
	TAccount user = TAccountManager.getAccount(pathArray[0]);
	if (user == null) { // user provided an invalid path
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, null));
	    return null;
	}
	if (user != getRequester().getOwner()) {
	    setReturnStatusFailed("Currently donnt allow access to other's account.");
	    return null;
	}
	TSRMLocalDir dir = user.getTopDir();
	
	TSRMLocalDir tgtDir = TAccount.changeDir(dir, "Req-getLocalPath", pathArray, 1).getDir();
	if (tgtDir == null) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, 
							"user provided target path does not exist"));
	    return null;
	}
	String tgtName = pathArray[pathArray.length-1];
	TSRMLocalFile currFile = tgtDir.getFile(tgtName);
	if (currFile != null) {
	    if (owMode == TOverwriteMode.NEVER) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
		return null;
	    }
	    // overwrite is possible
	    if (givenStorageType == null) {
		this._fileStorageType = currFile.getFileStorageType();
	    } else if ((currFile.getFileStorageType() != null) && 
		       (givenStorageType != currFile.getFileStorageType())) 
	    {
		setReturnStatusFailed("file already exists, and the provided fileStorageType != existing one. Cannt overwrite=>"+givenStorageType+"!="+currFile.getFileStorageType()); 
		return null;
	    }
	    // matching of space type and filetype will be done in assignSpaceToken()
	    return currFile;
	    //return new TSRMSourceFile(path);
	} else {
	    TSRMLocalDir curr = tgtDir.getSubDir(tgtName);
	    if (curr != null) {
		return curr;
		// will create the file here in this dir
		//return new TSRMSourceFile(curr.createFile(newFileName, getFileStorageType()).getSiteURL());
	    } else {
		// will assume the user is writting to this file. 
		//tgtDir.createFile(tgtName, getFileStorageType());
		if (expectedType != null) {
		    if (expectedType == TFileType.FILE) {
			return tgtDir.createSubFile(tgtName, getFileStorageType(), true);
		    } else {
			return tgtDir.createDir(tgtName, true);
		    }
		} else {
		    return null;
		}
		//return new TSRMSourceFile(tgtDir.createFile(tgtName, getFileStorageType()).getSiteURL());
	    }
	}    
    }   

    public boolean pickupIOToken() {
	if (!TSRMServer._gProcessingUnit.acquireIOToken()) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, 
							"Internal Error: ftp semaphore failed."));
	    return false;
	}
	return true;
    }
    
    public void dropOffIOToken() {
	TSRMServer._gProcessingUnit.releaseIOToken();
    }

    public boolean pickupIOToken(String protocol) {
	if (!TSRMServer._gProcessingUnit.acquireIOToken(protocol)) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, 
							"Internal Error: ftp semaphore failed."));
	    return false;
	}
	return true;
    }
    
    public void dropOffIOToken(String protocol) {
	TSRMServer._gProcessingUnit.releaseIOToken(protocol);
    }

    public void useToken(TSRMStorage.iSRMSpaceToken t) {
	_spaceToken = t;
    }

    public boolean assignSpaceToken(String userProvidedToken, TFileStorageType type) {
	if (Config.isSpaceMgtDisabled()) {
	    return true;
	}

	if (userProvidedToken==null) {			
	    TSRMStorage.iSRMSpace spaceManager = null;
	    if (type == null) {// assign to default
		spaceManager = TSRMStorage.getSpaceManager(TSRMStorage._DefDefaultUserSpaceType);		   
		_fileStorageType = TSRMStorage._DefDefaultUserFileStorageType;
	    } else {
		spaceManager = TSRMStorage.getSpaceManager(type);
		_fileStorageType = type;
	    }

	    if ((spaceManager == null) && !(Config.isSpaceMgtDisabled())) {
		setReturnStatusFailed("Not supported space: given storage type="+type+"  given token="+userProvidedToken);
		return false;
	    }
	    // find a token to use according to fileType
	    _spaceToken = spaceManager.locateDefaultToken(_fileStorageType); 
	    
	    if (_spaceToken == null) {
		setReturnStatusFailed("Not able to allocate default token of type:"+type); 				      
		return false;
	    } else {
		getReturnStatus().setExplanation("A default token is assigned");
	    }
	} else {
	    String tid = userProvidedToken;

	    _fileStorageType = type;
	    _spaceToken = getRequester().getOwner().getToken(tid);		
	    if (_spaceToken == null) {		    
		if (willRelayToken()) {
		    return true;
		} else {
		    _spaceToken = TAccountManager._SUPER_USER.getToken(tid);
		    if (_spaceToken == null) {
			setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken));
			return false;
		    }
		}
	    } 
	    
	    if (!TSRMStorage.isFileTypeConsistantWithSpaceType(type, _spaceToken.getType())) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, //TStatusCode.SRM_INVALID_REQUEST,
							    "space type and file type mismatch."));
		return false;
	    }
	    if (_spaceToken.isLifeTimeExpired()) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED, 
							    "Space token has expired life time."));
		return false;
	    }
	}
	return true;
    }
}
