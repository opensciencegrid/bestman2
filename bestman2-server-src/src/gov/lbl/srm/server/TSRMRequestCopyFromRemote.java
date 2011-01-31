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
import gov.lbl.srm.storage.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import org.apache.axis.types.URI;

public class TSRMRequestCopyFromRemote extends TSRMRequestCopy {
    public TSRMRequestCopyFromRemote(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSRMSource tgt) {
	super(r, src, tgt, userReq);
    }

    public TSRMRequestCopyFromRemote(TSRMFileInputCopy r, TSupportedURL from, TSupportedURL to, TUserRequest userReq) {
	super(r, from, to, userReq);
	
	schedule();
    }
    
    public void setReturnStatusPinned(String notes) {
	setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "pinned:"+notes)); // 
    }

    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) {
	if (srcIsDir) {
	    ISRMLocalPath localPath = tgtSURL.getLocalPath();
	    if (localPath == null) {
		throw new TSRMException("No such diretory:"+tgtSURL.getURLString(), false);
	    } 
	    if (!localPath.isDir()) {
		throw new TSRMException("Cann't copy from dir to a file", false);
	    }
	    _srcSiteURL.setLocalDestination(localPath);
	    _tgtSiteURL = TSRMSourceDir.create(tgtSURL, getRequester().getOwner());
	} else {
	    ISRMLocalPath localPath = tgtSURL.getLocalPath();
	    if (localPath == null) {
		localPath = getLocalPath(tgtSURL, TFileType.FILE, this.getFileStorageType(), getOverwriteMode());	    
		if (localPath == null) {
		    throw new TSRMException("The path is invalid. Use mkdir to make sure all paths exist.", false);
		}
		setResponsibleToCleanupLocalFile(true);
	    } else {
		if (getOverwriteMode() == TOverwriteMode.NEVER) {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
		}
	    }
	    
	    if (localPath.isDir()) {
		TSRMLocalDir dir = (TSRMLocalDir)localPath;
		//TSRMLocalFile f = dir.createSubFile(TSupportedURL.getEndFileName(_srcSiteURL.getSourceURL().getSURLInfo()), getFileStorageType());
		TSRMLocalFile f = dir.createSubFile(_srcSiteURL.getSourceURL().getEndFileName(), getFileStorageType(), true);
		setResponsibleToCleanupLocalFile(true);

		_srcSiteURL.setLocalDestination(f);		 
		f.setSourceURI(_srcSiteURL.getSourceURL().getURI());
		
		try {
		    _tgtSiteURL = new TSRMSourceFile(f.getSiteURL());   
		} catch (Exception e) {		    
		    TSRMLog.exception(TSRMRequestCopyFromRemote.class, "details", e);	    
		    throw new TSRMException("Oh no!", false);
		}
	    } else {
		_tgtSiteURL = TSRMSourceFile.create(tgtSURL);
		_srcSiteURL.setLocalDestination(localPath);
		TSRMLocalFile f = (TSRMLocalFile)localPath;
		if (f.getTxfState().doBlocking()) {
		     throw new TSRMExceptionFileBusy("file is used by another process.", false);
		}
		f.unsetToken(true);
		f.setSourceURI(_srcSiteURL.getSourceURL().getURI());
	    }
	}
    }

    public void cleanUp() {
	/*
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_BUSY) {
	    return;
	}
	*/
	if ((_srcSiteURL != null) && (_srcSiteURL.getLocalPath()!= null)) {
	    if (!_srcSiteURL.getLocalPath().isDir()) {
		if (!isResponsibleToCleanupLocalFile()) {
		    return;
		}
		TSRMLocalFile f = (TSRMLocalFile)(_srcSiteURL.getLocalPath());
		if (TSRMLog.getCacheLog() != null) {
		 TSRMLog.getCacheLog().removeFile(f);
		}
		 f.detachFromParent();
		 f.unsetToken(false);
	    }
	}
    }

    public ISRMLocalPath getPinnedPath() {
	return _srcSiteURL.getLocalPath();
	// we do not release surl
	//return null;  
    }

    TSRMRequestCopy createChildRequest(TSRMSource curr) {
	TSURLInfo tgtInfo = curr.getLocalPath().getSiteURL();
	TSupportedURL tgtUrl = TSupportedURL.create(tgtInfo);
	TSRMSource tgt = null;
	if (curr.isDir()) {
		tgt = new TSRMSourceDir(tgtUrl);
	} else {
		tgt = new TSRMSourceFile(tgtUrl);
	}
	return new TSRMRequestCopyFromRemote(getRequester(), this.getInput(), curr, tgt);
    }
	
    // call get() to remote SRM, or gsiftp from remote location
    void work() {
	if (_srcSiteURL.isDir()) {
	    TSRMSourceDir srcDir = (TSRMSourceDir)(_srcSiteURL);
	    srcDir.dissipate(this);	   
	    return;
	}
 
	if (!pickupIOToken()) {
	    return;
	}

	try {
	    // start ftp from source to cache
	    if (!authorized(_tgtSiteURL, TSRMPermission.Writable)) {
		return;
	    } else {
		_srcSiteURL.getSourceURL().useCredential(getRequester().getGSSCredential());
		_srcSiteURL.attachToLocalCache(this.getRequester(), _actualSizeInBytes);
		TSRMLocalFile f = ((TSRMSourceFile)_srcSiteURL).getLocalDestination();
		if (f != null) {
		    TSRMNameSpace.addEntry(f.getSiteURL().getSURL().toString(), f, this);
		}
		/*
		if (f.isReady() && getOverwriteMode() == TOverwriteMode.NEVER) {
		     setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
		} else {
		*/
		_srcSiteURL.transferMe(this);		    
		TSRMLog.debug(this.getClass(), null, "event=file-pinned", "src=\""+_srcSiteURL.getSourceURL().getURLString()+"\"");		
		//}

		if (isFailed()) {
		    cleanUp();
		}
	    }
	} catch (TSRMException e) {
	    if (!e.isRetriable()) {		
		TSRMLog.exception(TSRMRequestCopyFromRemote.class, "details", e);	    
		//_srcSiteURL.detachFromLocalCache();
	    }
	    
	    handleException(e);
	} catch (RuntimeException ex) {
	    TSRMLog.debug(this.getClass(), null, "event=file-failed!", "src=\""+_srcSiteURL.getSourceURL().getURLString()+"\"");	    
			  
	    TSRMLog.exception(TSRMRequestCopyFromRemote.class, "details", ex);	    
	    
	    //_srcSiteURL.detachFromLocalCache();
	    handleException(ex);
	} finally {	
	    dropOffIOToken();
	    if (isFailed()) {
		cleanUp();
	    }
	}
    }
	
    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	
	if (statusCode == TStatusCode.SRM_REQUEST_SUSPENDED) {
	    // no need to do anything
	    return;
	} else if (statusCode == TStatusCode.SRM_REQUEST_QUEUED) {
	    return; // since returnStatus changed to abort_done, runMe() wont do anything
	} else if (statusCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
	    _srcSiteURL.abortTransfer();
	} else if (statusCode == TStatusCode.SRM_FILE_LIFETIME_EXPIRED) {
	    release(true);
	} else if (statusCode == TStatusCode.SRM_FILE_PINNED) {
	    release(true);
	} else if (statusCode == TStatusCode.SRM_SUCCESS) {
	    release(true);
	}
    }
    
    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }
    
    public void resume() {
	
    }
}
