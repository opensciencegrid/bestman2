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

import gov.lbl.srm.util.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;

//import gov.lbl.srm.policy.TSRMGeneralEnforcement;
 
import EDU.oswego.cs.dl.util.concurrent.Mutex;

import org.apache.axis.types.URI;

public class TSRMRequestGet extends TSRMRequest {
    TSRMSource _source = null;
    long _actualSizeInBytes = -1;
    TSRMFileInput _input = null;

    boolean _skipPostInit = false;
	
    public TSRMRequestGet(TUserRequest userReq, TSRMFileInput r, TSRMSource src) {
	super(r, userReq);
	//setUserRequest(userReq);
	super.setProposedPinDuration(r.getTxfLifetime());

	_source = src;
	_skipPostInit = true;

	try {
	    _actualSizeInBytes = _source.getTrustedSize();
	} catch (TSRMException e) {
	    //setReturnStatusFailed(e.getMessage());
	    setReturnStatus(e.getReturnStatus());
	}
    }

    public TSRMRequestGet(TSRMFileInput r, TUserRequest userReq) {
	super(r, userReq);
	//setUserRequest(userReq);

	_input = r;

	schedule();
    }


    public TSRMFileInput getInput() {
	return _input;
    }

    private void postInit(TSRMFileInput r) {
	TSupportedURL fromUrl = r.getSURLInfo();
	fromUrl.checkSFN(getRequester().getOwner(), TSRMPermission.Readable);
    
	fromUrl.useCredential(getRequester().getGSSCredential());		 	
	
	if (!authorized(fromUrl, TSRMPermission.Readable)) {
	    return;
	}

	// assigns space token
	// 	
	if (!fromUrl.isProtocolFILE()) {
	if (!assignSpaceToken(r.getSpaceToken(), r.getFileStorageType())) {
	    return;
	}
    }
	_skipPostInit = true;
	try {
	    if (isSourceDir(r, fromUrl)) {
		handleDirSrc(fromUrl, r.getDirOption());
	    } else {
		_source = new TSRMSourceFile(fromUrl); // 
		_source.setToken(getSpaceToken());		
	    }
	} catch (RuntimeException e) {
	    TSRMLog.exception(TSRMRequestGet.class, "details", e);
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));
	    return;
	}			

	super.setProposedPinDuration(r.getTxfLifetime()); 	

	if (fromUrl.isProtocolFILE()) {
		return;
	}
	try {
	    long trustedSize = _source.getTrustedSize();
	    _actualSizeInBytes = trustedSize;

	    if (getSpaceToken().getTokenAttributes().getGuaranteedSpaceBytes() < trustedSize) {
		//throw new TSRMException("Token cannot accomodate: "+trustedSize+" bytes.", false);
		setReturnStatusFailed("Token cannot accomodate:"+trustedSize+" bytes.");
	    }
	} catch (TSRMException e) {
	    //setReturnStatusFailed(e.getMessage());
	    setReturnStatus(e.getReturnStatus());
	}       
    }

    TSRMRequestGet createChildRequest(TSRMSource curr) {
	return new TSRMRequestGet(getRequester(), this.getInput(), curr);
    }

    private boolean isSourceDir(TSRMFileInput r, TSupportedURL fromUrl) {
	if ((r.getDirOption() != null)&& (r.getDirOption().isIsSourceADirectory())) {
	    return true;
	} else if (fromUrl.isDir()) {
	    return true;	 
	}
	return false;
    }

    public void handleDirSrc(TSupportedURL surl, TDirOption dirOp) {
	//_source = TSRMSourceDir.create(surl);
	_source = new TSRMSourceDir(surl);
	_source.setToken(getSpaceToken());
	((TSRMSourceDir)_source).setRecursiveLevel(dirOp);
	((TSRMSourceDir)_source).setBlur();
	surl.populateMe((TSRMSourceDir)_source);
    }

    public void schedule() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
	    
	    TSRMServer._gProcessingUnit.submitJob(this);
	    TSRMLog.info(this.getClass(), null,  description(), "event=queued");  
	}
    }
    
    void setReturnStatusPinned() {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_PINNED, null)); // 
	//
	// we added the txf url in the hashmap, just to be nice.
	//
	/*
	URI transferURL = getTransferURL();	
	if (transferURL != null) {
	    getRequester().add(transferURL.toString(), this);	
	}
	*/
    }
    
    private boolean localSourceFileHandled() {
	if (_source.isDir()) {
	    TSRMLog.debug(this.getClass(), null, description(), "event=localSourceFileHandled msg=sourceIsAdir action=none");
	    return false;
	}
	
	TSRMSourceFile src = (TSRMSourceFile)(_source);
	if (src.getSourceURL().isProtocolFILE()) {
	    //fileIsOnLocalDisk();
	    TSupportedURLWithFILE f = (TSupportedURLWithFILE)(src.getSourceURL());
	    if (!f.checkExistence()) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "No such file on disk:"+f.getDiskPath()));
	    } else {
		src.setLocalDestination(f.getStageFile());
		f.getStageFile().pin(this, TStatusCode.SRM_FILE_PINNED);
	    }		  
	    return true;
	} else if (src.getSourceURL().isProtocolSRM()) {
	    TSupportedURLWithSRM surl = (TSupportedURLWithSRM)(src.getSourceURL());	    
	    if (surl.isLocalToThisSRM()) {
		TSupportedURLWithSRMRefToLocal local = (TSupportedURLWithSRMRefToLocal)surl;
		if (local.isDir()) {
		    setReturnStatusFailed("No support for src being directory");
		    return true;
		}

		TSRMLocalFile f = (TSRMLocalFile)(local.getCorrespondingLocalPath());

		if (f == null) {
		    //setReturnStatusFailed("No src found in cache.");		    
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "No src found in cache."));
		    return true;
		} else if (!f.isReady()) {
		    TSRMPhysicalLocation phyLoc = f.getPhysicalLocation();
		    if ((phyLoc != null) && (phyLoc.isTransfering())) {
			setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, null));
		    } else {
			setReturnStatusFailed("No src in cache.");
		    }
		    return true;
		} else if (!f.isValid()) {
		    //setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED, "Surl has no lifetime available"));
		    setReturnStatusSurlExpired("Surl has no lifetime available");
		    return true;
		} else {
		    TSRMNameSpace.addEntry(_source.getSourceURL().getURLString(), this);
		    TSRMLog.debug(this.getClass(), null, "event=localSourceChecking", description());

		    this.limitProposedPinDuration(f.getLifeTimeRemainingInSeconds()*1000);
		    if (getSpaceToken() == f.getToken()) {
			src.setLocalDestination(f);
			f.pin(this, TStatusCode.SRM_FILE_PINNED);
			return true;
		    } else {			
			try {
			    f.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
			    if (!TSRMNameSpace.pinTURLIfExists(_source, getSpaceToken(), this)) {
				TSRMLocalFile copy = getSpaceToken().createTURL(f, getRequester());
				_source.setLocalDestination(copy);
				work();
			    } 
			} catch (RuntimeException ex) {
			     TSRMLog.debug(this.getClass(), null, "event=RTEx", description());
			     TSRMLog.exception(TSRMRequestGet.class, "details", ex);
			     handleException(ex);
			}  finally {
			    f.unpin(this);
			}
			return true;
		    }
		}	
	    }
	}

	return false;
    }

    private void work() {
	if (!TSRMNameSpace.pinTURLIfExists(_source, getSpaceToken(), this)) {	    
	    _source.attachToLocalCache(this.getRequester(), _actualSizeInBytes);
	    TSRMNameSpace.addEntry(_source.getSourceURL().getURLString(), ((TSRMSourceFile)_source).getLocalDestination(), this);
	    _source.transferMe(this);
	}
    }
    
    public void runMe() {		 	    	    
	TSRMLog.info(this.getClass(), null, description(), "event=schedule");
	
	try{
	    if (!_skipPostInit) {
		postInit(_input);
	    }
	} catch (TSRMException e) {
	    setReturnStatus(TSRMUtil.createReturnStatus(e));	    
	    TSRMLog.exception(TSRMRequestGet.class, "details", e);
	    return;
	} catch (RuntimeException e) {
	    setReturnStatus(TSRMUtil.createReturnStatus(e));
	    TSRMLog.exception(TSRMRequestGet.class, "details", e);
	    return;
	}
	
	if (!isQueued()) {
	    return;
	}
	
	if (_source.isDir()) {
	    TSRMSourceDir srcDir = (TSRMSourceDir)(_source);
	    srcDir.dissipate(this);
	    return;
	} 
		
	setStatusInProgress();
 
	boolean isMSSProtocol = (_source.getSourceURL().isOfProtocol(TSupportedURLOnLocalHPSS._DefProtocolStr));

	/*if (!isMSSProtocol) {
	    //if (!pickupIOToken(_source.getSourceURL().getProtocol())) {
	    if (!pickupIOToken()) {
		return;
	    }
	}
	*/
	if (!_source.getSourceURL().pickupIOToken(this)) {
	    return;
	}
	
	//  ftp from source to cache
	try {      	 	   	  
	    if (localSourceFileHandled()) {
		return;
	    }
	    work();		    	
	} catch (TSRMException e) {
	    TSRMLog.exception(this.getClass(), "details", e);
	    if (!_source.broadcast(e)) {
		TSRMLog.debug(this.getClass(), null, "event=SRMEx", description());
		//_source.detachFromLocalCache();
		handleException(e);		
	    }
	} catch (RuntimeException ex) {
	    TSRMLog.debug(this.getClass(), null, "event=RTEx", description());	    
	    TSRMLog.exception(TSRMRequestGet.class, "details", ex);
	    
	    if (!_source.broadcast(ex)) {
		//_source.detachFromLocalCache();
		handleException(ex);
	    }
	} finally {
	    //if (!isMSSProtocol) {
	    _source.getSourceURL().dropOffIOToken(this);
	}
	
	//if (isAborted()) {
	    // wont do anything now (e.g. delete the files)
	    // because current avail functions in tokens will compactTokens automatically
	    // will leave the files here and let them be recycled when space is needed
	//}

	TSRMLog.info(this.getClass(), null, description(), "event=end");	    
    }
    
    public String description() {
	//String desc = "rid="+getRequester().getID();
	String desc = "rid="+getIdentifier();
	if (_source != null) {
	    desc += " src=\""+_source.asString()+"\"";
	} else {
	    desc += " src=unknown";
	}

	if (getSpaceToken() != null) {
	    desc += " token="+getSpaceToken().getID();
	}
	desc += " pinDur="+getProposedPinDuration();
	return desc;
    }
    
    public long getEstProcessingTimeInSeconds() {
	return -1;
    }
    
    public long getEstWaitTimeInSeconds() {
	return -1;
    }
    
    public ISRMLocalPath getPinnedPath() {
	if (_source != null) {
	    return _source.getLocalPath();
	} else {
	    return null;
	}
    }

    public void cleanUp() {
	if (getPinnedPath() == null) {
	    return;
	}

	if (getPinnedPath().isDir()) {
	    RuntimeException hi = new RuntimeException("Not supported yet with dir");
	    hi.printStackTrace();
	} else {
	    TSRMLocalFile f = (TSRMLocalFile)(getPinnedPath());
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().removeFile(f);
		}
	    f.detachFromParent();
	    f.unsetToken(false);	    
	}
    }
	
    public TReturnStatus removeFileInCache() {
	if (getPinnedPath().isDir()) {
	    RuntimeException hi = new RuntimeException("Not supported yet with dir");
	    hi.printStackTrace();
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Not supporting removing dir/");
	}

	TSRMLocalFile f = (TSRMLocalFile)(getPinnedPath());
	if (f == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "file not avail yet.");
	}
	
	if (f.getPin(this) != null) {
	    releaseFile(false);
	    //f.deleteMe();
	}	    

	if (f.isPinned()) { // is still pinned
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Still has active requests pinning this file.");
	} 

	
	f.unsetToken(false);
	f.getParent().rmFileName(f.getName());
	
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);	
    }
    
    public void setReceivedBytes(long bytes) {}

    public long getFileSize() {
	return _actualSizeInBytes;
    }

    public void setTrustedSize(long s) {
	_actualSizeInBytes = s;
    } 

    public URI getTransferURL() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_PINNED) {
	    return _source.getLocalPath().getTxfURI(getRequester().getTxfProtocol()); //TSRMTxfProtocol.getDefaultUploadProtocol());
	} else {
	    return null;
	}
    }

    public URI getSourceURI() {
	if (_source == null) {
	    TSRMLog.debug(this.getClass(), null, "getSourceURI=null", description());
	    return null;
	}
	return _source.getSourceURL().getURI();
    }

    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	if (statusCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
	    _source.abortTransfer();
	} else if (statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
	    releaseFile(true);
	} else if (statusCode == TStatusCode.SRM_FILE_PINNED) {
	    releaseFile(true);
	}
    }
    
    //
    // makes no difference to the reqs that are done with transfers.
    //
    public void resume() {
	
    }
}


 
 

 
