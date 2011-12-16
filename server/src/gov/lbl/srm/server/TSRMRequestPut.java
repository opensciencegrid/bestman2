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

import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;

import gov.lbl.srm.transfer.globus.SRMTransferProtocol;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

//import EDU.oswego.cs.dl.util.concurrent.Mutex;

import org.apache.axis.types.URI;

public class TSRMRequestPut extends TSRMRequest {
    TSRMMutex _pinMutex = new TSRMMutex();	
    
    TSRMSourceFile _tgtSite 	= null;
    long _givenSizeInBytes  	= -1;
    long _actualSizeInBytes     = -1;

    Integer _turlLifeTime = null;
    long _turlExpirationTimeMillis = -1;
    long _spaceAvailAtMillis = -1;
    
    TStatusCode _statusReturnedToUser = null;

    public TSRMRequestPut(TSRMFileInput r, TUserRequest userReq) {
	super(r, userReq);
	//setUserRequest(userReq);			

	if (!checkTgtPath(r)) {
	    cleanUp();
	    return;
	}

	if (_tgtSite.getLocalDestination() == null) {
	    TSRMNameSpace.addEntry(_tgtSite.getSourceURL().getURLString());
	} else {
	    TSRMNameSpace.addEntry(_tgtSite.getLocalDestination().getSiteURL().getSURL().toString());
	}

	// note this has to follow checkTgtPath, not the other way round
	if (!_tgtSite.getSourceURL().isDeviceSpecific() && !_tgtSite.getSourceURL().isProtocolFILE()) {
	    if (!assignSpaceToken(r.getSpaceToken(), r.getFileStorageType())) {
		cleanUp();
		return;
	    }
	}
       
	_turlLifeTime = r.getTxfLifetime();
	super.setProposedPinDuration(r.getFileLifetime()); 

	if ((getSpaceToken() != null) && (r.getKnownSizeOfThisFile() != null)) {
	    _givenSizeInBytes = r.getKnownSizeOfThisFile().longValue();
	    if (getSpaceToken().getMetaData().getGuaranteedSize().longValue() < _givenSizeInBytes) 		
	    {
		String detailMsg = " Asked: "+_givenSizeInBytes;
		detailMsg += " tokenSize="+getSpaceToken().getMetaData().getGuaranteedSize().longValue();
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NO_FREE_SPACE, 
							    "Not enough space in the token."+getSpaceToken().getID()+detailMsg));
	    }
	} 
	
	TSRMLog.debug(this.getClass(), null, "event=putAccepted rid="+getRequester().getID(), "surl=\""+_tgtSite.asString()+"\" localDest=\""+_tgtSite.getLocalDestination()+"\"");
	
	schedule();
    }

    public void setLastQueriedStatus(TStatusCode c) {
	_statusReturnedToUser = c;
	TSRMLog.info(this.getClass(), null, "event=setLastQueried", "id="+getIdentifier()+" value="+c);
    }

    public TStatusCode getLastQueriedStatusCode() {
	return _statusReturnedToUser;
    }

    public void cleanMe() {
	TSRMLog.debug(this.getClass(), null, "event=cleanPutReq", null);
	if (_tgtSite.getLocalDestination() == null) {
	    TSRMNameSpace.removeEntry(_tgtSite.getSourceURL().getURLString());
	} else {
	    TSRMNameSpace.removeEntry(_tgtSite.getLocalDestination().getSiteURL().getSURL().toString());
	}
	super.cleanMe();
    }

    public void schedule() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
	    if (_tgtSite.getLocalDestination() != null) {
		    //TSRMNameSpace.addEntry(_tgtSite.getSourceURL().getURLString(), _tgtSite.getLocalDestination(), this);
			TSRMNameSpace.addEntry(_tgtSite.getLocalDestination().getSiteURL().getSURL().toString(), _tgtSite.getLocalDestination(), this);
	    } 

	    //_source.setToken(getSpaceToken());
	    _tgtSite.setToken(getSpaceToken());
	    TSRMServer._gProcessingUnit.submitJob(this);
	    String sizeGiven = "GivenBytes=";
	    if (_givenSizeInBytes >= 0) {
		sizeGiven += _givenSizeInBytes;
	    } else {
		sizeGiven += "null";
	    }
	    if (getSpaceToken() != null) {
		TSRMLog.info(this.getClass(), null,  "tgt=\""+_tgtSite.toString()+"\"", 
			     "token="+getSpaceToken().getID()+ sizeGiven+" event=queued");  	    
	    } else {
		TSRMLog.info(this.getClass(), null,  "tgt=\""+_tgtSite.toString()+"\" comment=no_token_is_needed.", "sizeGiven="+sizeGiven+" event=queued");  	
	    }
	} else {
	    cleanUp();
	}
    }

    public Integer getRemainingTURLLifeTimeInSeconds() {
	if (getReturnStatus().getStatusCode() != TStatusCode.SRM_SPACE_AVAILABLE) {
	    return null;
    }	
	long now = System.currentTimeMillis();
	long resultMillis = getTxfURLExpirationTime() - now;
	if (resultMillis > 0) {
	    return TSRMUtil.createTLifeTimeInSeconds((int)(resultMillis/1000), false);
	} else {
	    return TSRMUtil.createTLifeTimeInSeconds(0, false);
	}
    }

    private TSupportedURL getValidURL(TSupportedURL info) { //TSURLInfo toPath) {
	TSupportedURL url = info;
	if (info == null) {
	    // assign default filename, for now. Deal with dir later;
	    TSURLInfo surlInfo = new TSURLInfo();
	    surlInfo.setSURL(getRequester().getOwner().autoGenerateSURL());
	    //_tgtSite = new TSRMSourceFile(surlInfo);	      
	    url = TSupportedURL.createAndCheckSiteFileName(surlInfo, getRequester().getOwner(), TSRMPermission.Readable, true);
	} else {
	    //TSupportedURL url = TSupportedURL.create(toPath);
	    //url = TSupportedURL.createAndCheckSiteFileName(toPath, getRequester().getOwner(), TSRMPermission.Readable, true);	    
	    url.checkSFN(getRequester().getOwner(), TSRMPermission.Readable);
	}	      

	if (url == null) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, TSRMUtil._DefInvalidLocalPath));	
	    return null;
	}	

	if (url.isProtocolSRM()) {	    
	    /*
	    TSupportedURLWithSRM srmUrl = (TSupportedURLWithSRM)url;
	    if (!srmUrl.isLocalToThisSRM()) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, TSRMUtil._DefInvalidLocalPath));
		return null;
	    }	
	    */
	} else if (url.isDeviceSpecific()) { 
	    return url;
	} else if (url.isProtocolFILE()) {
	    return url;
	} else {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, 
							TSRMUtil._DefInvalidProtocol));
	    return null;
	}
	
	return url;
    }
    
    private TAccount getValidUser(String name) {	   
	TAccount user = TAccountManager.getAccount(name);
	
	if (user == null) { // user provided an invalid path
	    setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized("No such user with name:"+name));
	    return null;
	}
	if (user != getRequester().getOwner()) {
	    setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized("Currently donnt allow put() to other user's account."));
	    return null;
	}
	return user;
    }
    
    private boolean whenTgtIsExistingFile(TSRMLocalFile currFile, TFileStorageType givenStorageType) {
	TUserRequestPut req = (TUserRequestPut)getRequester();
	if (req.getOverwriteMode() == TOverwriteMode.NEVER) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
	    return false;
	}
	// overwrite is possible
	if (givenStorageType == null) {
	    this._fileStorageType = currFile.getFileStorageType();
	} else if (givenStorageType != currFile.getFileStorageType()) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefConflictStorageType));   	
	    return false;
	}

	if (!currFile.isReady()) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, null));
	    return false;
	}
	
	currFile.unsetToken(true); // it took care of releasing all the pins on this file
	
	return true;
    }
    
    //
    // mainly, what we want to make sure is, the _tgtSite should be a file.
    // if a user provided a valid dir, then we create a file in that dir.
    // if the user provided file path already exists, we check the overwrite option
    // 
    public boolean checkTgtPath(TSRMFileInput r) {
	TFileStorageType givenStorageType = r.getFileStorageType();

	TSupportedURL toPath = r.getSURLInfo();
	TSupportedURL url = getValidURL(toPath);	

	if (url == null) {
	    setReturnStatus(TSRMUtil.createReturnStatusInvalidPath("Target path is not a valid url."));
	    return false;
	}
	
	url.useCredential(getRequester().getGSSCredential());

	if (url.isDeviceSpecific()) {
	    if ((r.getSpaceToken() != null)) {
		setReturnStatus(TSRMUtil.createReturnStatusFailed("Space token does not apply to:"+url.getURLString()));
		return false;
	    }
	    //TSupportedURL deviceUrl = (TSupportedURLDeviceSpecific)url;	    

	    if (url.isDir()) {
		String assignedName= System.currentTimeMillis()+Thread.currentThread().getName();
		TSURLInfo sub = TSRMUtil.createTSURLInfo(url.getURLString()+"/"+assignedName);
		if (sub != null) {
		    sub.setStorageSystemInfo(toPath.getSURLInfo().getStorageSystemInfo());
		    url = TSupportedURLDeviceSpecific.getDeviceSpecificURL(sub);
		} else {
		    throw new TSRMException("cannt create target path on device.", false);
		}
	    }
	   
	    _tgtSite = new TSRMSourceFileUserOwned(url);			

	    url.setLocalPath(_tgtSite.getLocalDestination());
	    
	    return true;
	}

	if (url.isProtocolFILE()) {
	    if (r.getSpaceToken() != null) {
		String[] result = TSRMUserSpaceTokenSupport.match(r.getSpaceToken());
		if ((result == null) || (result.length == 0)) {
			if (StaticToken.find(Config._staticTokenList, r.getSpaceToken())  == null) {
		        setReturnStatus(TSRMUtil.createReturnStatusFailed("Space token: ["+r.getSpaceToken()+"] is not recognized here."));		
		        return false;
			} else {
				TSRMLog.info(this.getClass(), null, "event=foundstaticToken", "value="+r.getSpaceToken());
			}
		} else {
		    // double check:
		    TSupportedURLWithFILE curr = (TSupportedURLWithFILE)url;
		    boolean matching = TSRMUserSpaceTokenSupport.matchWithURL(result[0], curr.getDiskPath());
		    if (!matching) {
			//setReturnStatus(TSRMUtil.createReturnStatusFailed("Space token: ["+r.getSpaceToken()+"] is not matching with url:"+url.getURLString()));		
			//return false;
			url = TSRMUserSpaceTokenSupport.addPrefix(result[0], curr.getDiskPath());
		    }
		}
	    }
	    _tgtSite = new TSRMSourceFileUserOwned(url);
	    return true;
	}

	//
	// assuming that now the url is srm:// and points to local site.
	// pathArray[0] should then contains the account info	 
	//
	String[] pathArray = TSupportedURL.getPathInArray(url);

	TAccount user = getValidUser(pathArray[0]);
	if (user == null) {
	    return false;
	}
	
	TSRMChangeDirStatus status = TAccount.changeDir(user.getTopDir(), "Put-checkDir", pathArray, 1);
	TSRMLocalDir tgtDir = status.getDir();	       
	
	if (tgtDir ==null) {
	    setReturnStatus(status.getStatus());
	    return false;
	}
	
	if (pathArray.length == 1) {
	    TSRMLocalFile autoFile = tgtDir.createSubFile(null, getFileStorageType(), false);
	    _tgtSite = new TSRMSourceFile(autoFile.getSiteURL());
	    _tgtSite.setLocalDestination(autoFile);
	    return true;
	}
	
	String tgtName = pathArray[pathArray.length-1];
	TSRMLocalFile currFile = tgtDir.getFile(tgtName);
	    
	if (currFile != null) {
	    if (!whenTgtIsExistingFile(currFile, givenStorageType)) {
		return false;
	    }
	    // matching of space type and filetype will be done in assignSpaceToken()
	    _tgtSite = new TSRMSourceFile(url.getSURLInfo());
	    _tgtSite.setLocalDestination(currFile);
	} else {
	    TSRMLocalDir curr = tgtDir.getSubDir(tgtName);
	    TSRMLocalFile f = null;
	    if (curr != null) {
		//_tgtSite = new TSRMSourceFile(curr.createSubFile(null, getFileStorageType(), true).getSiteURL());
		f = curr.createSubFile(null, getFileStorageType(), true);
		_tgtSite = new TSRMSourceFile(f.getSiteURL());
	    } else {
		// will assume the user is writting to this file. 
		f = tgtDir.createSubFile(tgtName, getFileStorageType(), true);
		f.setSourceURI(url.getURI());

		_tgtSite = new TSRMSourceFile(url.getSURLInfo());       	
	    }
	    _tgtSite.setLocalDestination(f);
	}	       	
	return true;
    }
    
    
    public String description() {
	//String desc = "rid="+getRequester().getID();
	String desc = "rid="+getIdentifier();
	if (_tgtSite != null) {
	    desc += " tgt=\""+_tgtSite.getSourceURL().getURLString()+"\"";
	} else {
	    desc += " tgt=NotAvail.";
	}
	desc += " pinDur="+getProposedPinDuration();

	if (_turlLifeTime != null) {
	    desc += " turlLT(sec)="+_turlLifeTime.intValue();
 	}
	return desc;
    }

    public void setTxfURLExpirationTime() {
	long dur = getPushWaitingTimeInMillis();
	_turlExpirationTimeMillis = System.currentTimeMillis() + dur;	
    }

    public void updateTxfURLExpirationTime(int durInSeconds) {
	_turlLifeTime = new Integer(durInSeconds);
	setTxfURLExpirationTime();
	TSRMLog.debug(this.getClass(), null, "event=txfLT_updated.", description());
    }

    public void adjustTxfURLExpirationTime() {
	if (getSpaceToken() != null) {
	    long spaceExpiresAt = getSpaceToken().getExpirationTimeInMilliSeconds();
	    if (spaceExpiresAt > 0) {
		long diff = getTxfURLExpirationTime() - spaceExpiresAt;
		if (diff > 0) {
		    long updatedDurMillis = spaceExpiresAt-System.currentTimeMillis();
		    if (updatedDurMillis < 0) {
			throw new TSRMExceptionSpaceTimedOut(getSpaceToken().getID());
		    }
		    updateTxfURLExpirationTime((int)(updatedDurMillis/1000));
		}
	    }
	}
    }

    public long getTxfURLExpirationTime() {
	return _turlExpirationTimeMillis;
    }

    private long getPushWaitingTimeInMillis() {
	long inactiveTimeLimit = TSRMGeneralEnforcement._INACTIVE_TXF_TIME_IN_MILLISECONDS;
	if ((_turlLifeTime != null) && (_turlLifeTime.intValue() > 0)) {
	    inactiveTimeLimit = _turlLifeTime.intValue() * 1000L;
	}
	return inactiveTimeLimit;
    }

    public URI getTargetURI() {
	if (_tgtSite != null) {
	    return _tgtSite.getSourceURL().getURI();
	}
	return null;
    }

    public URI getTransferURL() {

	if ((getReturnStatus().getStatusCode() == TStatusCode.SRM_SPACE_AVAILABLE) && 
	    (_tgtSite != null))
	{
	    return (_tgtSite.getLocalDestination().getTxfFile().getTxfURI(getRequester().getTxfProtocol())); //TSRMTxfProtocol.getDefaultUploadProtocol()));
	}
	
	return null;
    }
    
    public void setReceivedBytes(long bytes) {
	TSRMLog.debug(this.getClass(), null, description(), "setReceivedBytes="+bytes);
	_actualSizeInBytes = bytes;
    }

    public void setTrustedSize(long s) {
	// taken care by setReceivedBytes
    } 
    public long getFileSize() {
	if (_tgtSite == null) {
	    _actualSizeInBytes = -1;
	} else if (_tgtSite.getLocalDestination() == null) {
	    _actualSizeInBytes = -1;
	} else if ((getReturnStatus().getStatusCode() != TStatusCode.SRM_FILE_PINNED) && 
	           (getReturnStatus().getStatusCode() != TStatusCode.SRM_FAILURE) &&
		   (getReturnStatus().getStatusCode() != TStatusCode.SRM_SUCCESS)) 
	{	    
	    _actualSizeInBytes = _tgtSite.getLocalDestination().getCurrentSize();
	}
	return _actualSizeInBytes;
	/*
	TSizeInBytes s = new TSizeInBytes();
	
	if (_tgtSite == null) {
	    s.setValue(-1);
	} else if (_tgtSite.getLocalDestination() == null) {
	    s.setValue(-1);
	} else {
	    s.setValue(_tgtSite.getLocalDestination().getCurrentSize());
	}
	return s;
	*/
    }
    
    public ISRMLocalPath getPinnedPath() {
	return _tgtSite.getLocalPath();
    }

    public TSURLLifetimeReturnStatus extendTURLLifeTime(URI uri, Integer newLifeTimeObj) { //uri is ignored here
	int newLifeTimeInSeconds = TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS;
	if (newLifeTimeObj != null) {
	    newLifeTimeInSeconds = newLifeTimeObj.intValue();
	}
	
	TStatusCode code = getReturnStatus().getStatusCode();
	if ((code == TStatusCode.SRM_REQUEST_QUEUED) ||
	    (code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
	    (code == TStatusCode.SRM_SPACE_AVAILABLE)) 
	{
	    TSURLLifetimeReturnStatus result = 
		TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_SUCCESS, null);
	    updateTxfURLExpirationTime(newLifeTimeInSeconds);
	    adjustTxfURLExpirationTime();
	    result.setPinLifetime(_turlLifeTime);
	    return result;
	} else {							
	    return TSRMUtil.createTSURLLifetimeReturnStatus(null, TStatusCode.SRM_INVALID_REQUEST, "No turl.");
	} 
     }

    private void handleReceiveDone() {
	 if (!isSucc() && !isAborted() && !isReleased()) {		    		    
	     TSRMNameSpace.disableReplicas(_tgtSite.getSourceURL().getURLString(), this);
	     TSRMLocalFile localFile = _tgtSite.getLocalDestination();
	     localFile.pin(this, TStatusCode.SRM_FILE_PINNED);
		 if (TSRMLog.getCacheLog() != null) {
	     TSRMLog.getCacheLog().fileIsAdded(localFile, localFile.getCurrentSize(), localFile.getToken()); 
		 }
	 }
    }

    protected void handleFileDone(boolean checkTxfFile) {
	 _tgtSite.getLocalDestination().getTxfFile().updateToken();

	if (!TSRMUtil.acquireSync(_pinMutex)) {
	    return;
	}
	TSRMLocalFile localFile = _tgtSite.getLocalDestination();
	boolean reachedFinalDest = true;
	if (checkTxfFile) {
	    TSRMLocalFile txfFile = localFile.getTxfFile();
	    reachedFinalDest = (localFile == txfFile);
	}

	try {
	    if (!isSucc() && !isAborted() && !isReleased()) {		    		           		
		if (reachedFinalDest) {
		    TSRMNameSpace.disableReplicas(_tgtSite.getSourceURL().getURLString(), this);
		    localFile.pin(this, TStatusCode.SRM_SUCCESS);
		    if (TSRMLog.getCacheLog() != null) {
			TSRMLog.getCacheLog().fileIsAdded(localFile, localFile.getCurrentSize(), localFile.getToken()); 
		    }
		} else {
		    localFile.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
		    localFile.getTxfFile().pin(this, TStatusCode.SRM_FILE_IN_CACHE);
		}
		long dur = System.currentTimeMillis() - _spaceAvailAtMillis;
		TSRMLog.info(this.getClass(), null, "event=fileIsSettled size="+localFile.getCurrentSize(), description()+" durMillis="+dur);
	    }
	} finally {
	    TSRMUtil.releaseSync(_pinMutex);
	}	    
    }

    public boolean setReadyForClientToUpload() {
	_spaceAvailAtMillis = System.currentTimeMillis();
	return setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_AVAILABLE, null));
    }

    public TReturnStatus putDone() {
	TSRMLog.debug(this.getClass(), null, description(), "status="+getReturnStatus().getStatusCode().toString()+" event=putDoneCalled");

	if (getReturnStatus().getStatusCode() != TStatusCode.SRM_SPACE_AVAILABLE) {
	    //return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "putDone() is only for files currently being received.");
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, null);
	}

	if (_tgtSite.getLocalDestination().getTxfFile().getPhysicalLocation().isFileTrivial()) {	    	    
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "No bytes found while srmPutDone() was called.");
	}
	try {	    
	    _tgtSite.getLocalDestination().getTxfFile().getPhysicalLocation().changeTransferState(false);
	    _tgtSite.getLocalDestination().getTxfFile().updateToken();
	
	    handleFileDone(true);	    

	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	    
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), description()+" putDone() failed.",  e);
	    setReturnStatusFailed("putDone() failed");
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage());
	}
    }
    

    public void runMe() {
	if (!isQueued()) {
	    return;
	}
	
	if (_tgtSite != null) {
	    TSRMLog.debug(this.getClass(), null, "event=scheduled", "url=\""+_tgtSite.getSourceURL().getURLString()+"\" rid="+getRequester().getID());
	} else {
	    setReturnStatusFailed("No tgt found.");
	    return;
	}
	setStatusInProgress();
	if (!pickupIOToken()) {
	    return;
	}
	
	if (!isInProgress()) {
	   return;
    }
	try {
	    if (!authorized(_tgtSite.getSourceURL(), TSRMPermission.Writable)) {
		setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized(null));
	    } else {
		_tgtSite.attachToLocalCache(this.getRequester(), _givenSizeInBytes);
		_tgtSite.getLocalDestination().setSFNAppointedByUser();
		if (!isInProgress()){ // double check
		    if (isAborted()) {
			cleanUp();
		    }
		    return;
		}
		_tgtSite.receive(this);

		TSRMLog.debug(this.getClass(), null,  "event=receive-done", 
			      "rid="+getRequester().getID()+" tgt=\""+_tgtSite.getSourceURL().getURLString()+"\" status="+getReturnStatus().getStatusCode().toString());
		if (isFailed()) {
		    cleanUp();	    
		}
	    }
	} catch (TSRMException e) {
	    //e.printStackTrace();
	    handleException(e);
	} catch (RuntimeException ex) {
	    handleException(ex);
	} finally {	
	    dropOffIOToken();
	}

	try {
	    handleFileDone(false);	
	} catch (TSRMException e) {
	    //e.printStackTrace();
	    handleException(e);
	} catch (RuntimeException ex) {
	    handleException(ex);
	}
    }
	
    public void cleanUp() {
	/*
	 _tgtSite.getLocalDestination().unsetToken(false);
	 _tgtSite.getLocalDestination().detachFromParent();	
	*/
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_BUSY) {
	    return;
	}
	if (_tgtSite == null) {
	    return;
	}

	TSRMLocalFile f = _tgtSite.getLocalDestination();	
	if (f == null) {
	    TSRMLog.debug(this.getClass(), null, "event=cleanUp action=none", description());
	    return;
	}
	f.updateToken(); //this is needed so file is marked ready for being removed properly by file's remove() function
	if (TSRMLog.getCacheLog() != null) {
	TSRMLog.getCacheLog().removeFile(f);
	}
	f.detachFromParent();
	f.unsetToken(false);
    }    
    
    public long getEstProcessingTimeInSeconds() {
	return -1;
    }
    
    public long getEstWaitTimeInSeconds() {
	return -1;
    }
    
    /*
    public boolean isInProgress() {// for request summary
	TStatusCode statusCode = this.getReturnStatus().getStatusCode();
	
	return ((statusCode == TStatusCode.SRM_REQUEST_INPROGRESS) ||
		(statusCode == TStatusCode.SRM_FILE_PINNED) ||
		(statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) ||
		(statusCode == TStatusCode.SRM_SPACE_AVAILABLE));
    }
    */

    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }

    public boolean isCodeAllowed(TStatusCode code) {
	if (code == TStatusCode.SRM_FILE_IN_CACHE) {
	    return false;
	}
	return true;
    }

    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	 if ((statusCode == TStatusCode.SRM_REQUEST_INPROGRESS) ||
	     (statusCode == TStatusCode.SRM_SPACE_AVAILABLE))
	 {
	     _tgtSite.abortTransfer();
	     if (!implicitAbort) {
		 cleanUp();
	     }
	 } else if (statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
	     release(true);
	 } else if (statusCode == TStatusCode.SRM_FILE_PINNED) {
	     release(true);
	 }
	 // no need to do anything
    }

    public void resume() {
	
    }   
}
