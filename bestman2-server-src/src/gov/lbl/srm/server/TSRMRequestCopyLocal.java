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

//
// create the file in the token
public class TSRMRequestCopyLocal extends TSRMRequestCopy {
    ISRMLocalPath _localSrc = null;
    ISRMLocalPath _localTgt = null;

    int _recursiveLevel = 0;

    public TSRMRequestCopyLocal(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSRMSource tgt) {
	super(r, src, tgt, userReq);
    }

    public TSRMRequestCopyLocal(TSRMFileInputCopy r,  TSupportedURL srcSURL, TSupportedURL tgtSURL, TUserRequest userReq) 
    {
	super(r, srcSURL, tgtSURL, userReq);
	schedule();
    }
    
    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) {
	//if (_srcSiteURL.getSourceURL().isDeviceSpecific()) {
	if (_srcSiteURL.getLocalPath() != null) {
	    _localSrc = _srcSiteURL.getLocalPath();
	} else { 
	    _localSrc = _srcSiteURL.getSourceURL().getLocalPath();
	}

	if (_localSrc == null) {
	    setReturnStatusFailed("No such source.");
	    return;
	}

	_localTgt = tgtSURL.getLocalPath();
	
	if (_localTgt == null) {
	    if (srcIsDir) {
		_localTgt = this.getLocalPath(tgtSURL, TFileType.DIRECTORY, this.getFileStorageType(), getOverwriteMode());
	    } else {
		_localTgt = this.getLocalPath(tgtSURL, TFileType.FILE, this.getFileStorageType(), getOverwriteMode());
	    }

	    if (_localTgt != null) {
		setResponsibleToCleanupLocalFile(true);
	    }
	} else if (srcIsDir) {
	    if (!_localTgt.isDir()) {
		throw new TSRMException("Cannt copy dir to file.", false);
	    }
	}  else { // !srcIsDir
	    if (_localTgt.isDir()) {
		TSRMLocalDir tgtDir = (TSRMLocalDir)(_localTgt);
		_localTgt = tgtDir.createSubFile(TSupportedURL.getEndFileName(_srcSiteURL.getSourceURL().getSURLInfo()), 
						 this.getFileStorageType(), true);
		setResponsibleToCleanupLocalFile(true);
	    }

	    if (getOverwriteMode() == TOverwriteMode.NEVER) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
	    }	    

	    TSRMLocalFile tgtFile = (TSRMLocalFile)_localTgt;
	    if (!tgtFile.isReady()) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, "tgt file is busy"));
	    }
	    tgtFile.unsetToken(true);
	}
	    
	if (srcIsDir) {
	    if (!_localSrc.isDir()) {
		throw new TSRMException("Contradictory. actual src is not a dir.", false);
	    }	
	    // local tgt?
	    throw new TSRMException("Not supporting cp(dir/dir) yet", false);
	} else {
	    TSRMLocalFile srcFile = (TSRMLocalFile)_localSrc;
	    
	    if (!srcFile.isReady()) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, "source is busy. Cannt read."));
	    }
	}

    }

    public void cleanUp() {
	if (_localTgt == null) {
	    return;
	}

	if (!_localTgt.isDir()) {
	    if (!isResponsibleToCleanupLocalFile()) {
		return;
	    }
	    TSRMLocalFile f = (TSRMLocalFile)(_localTgt);
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().removeFile(f);
		}
	    f.detachFromParent();
	    f.unsetToken(false);
	}
    }

    public ISRMLocalPath getPinnedPath() {
	return _localTgt; 
    }
	
    TSRMRequestCopy createChildRequest(TSRMSource curr) {	
	return new TSRMRequestCopyLocal(getRequester(), this.getInput(), curr, null);
    }

    public void handleDirSrc(TSupportedURL srcSURL, TSupportedURL tgtSURL, TDirOption dirOp) {
	_srcSiteURL = TSRMSourceDir.create(srcSURL, getRequester().getOwner());
	_srcSiteURL.setToken(getSpaceToken());

	initTgt(true, tgtSURL);
	 
	((TSRMSourceDir)_srcSiteURL).setRecursiveLevel(dirOp);
	_recursiveLevel = ((TSRMSourceDir)_srcSiteURL).getRecursiveLevel();

	if (srcSURL.isDeviceSpecific()) {
	    //	    ((TSupportedURLDeviceSpecific)srcSURL).populateTo(_recursiveLevel, (TSRMLocalDirUserOwned)_localSrc);
	    srcSURL.populateTo(_recursiveLevel,  (TSRMLocalDirUserOwned)_localSrc);
	} else if (srcSURL.isProtocolFILE()) {
	    throw new TSRMException("NO support yet for file:/// dir.", false);
	}
    }

    // do local copy
    // no ftp tokens needed
    void work() {
	boolean srcIsReadable = _localSrc.getOwner().checkPermission(getRequester().getOwner(), TSRMPermission.Readable);
	boolean tgtIsWritable = _localTgt.getOwner().checkPermission(getRequester().getOwner(), TSRMPermission.Writable);
	if (!srcIsReadable && !tgtIsWritable) {
	    setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized("srcReadable="+srcIsReadable+" tgtWritable="+tgtIsWritable));
	    return;
	 }

	 try {
	    // start ftp from cache to source
	    if (!pinMe()) {
		cleanUp();
		return;
	    }
	    if (_localSrc.isDir()) {
		TSRMLocalDir srcDir = (TSRMLocalDir)_localSrc;
		TDeviceAccessInfo accessInfo = getSpaceToken().getHostDevice().getAccessInfo(getSpaceToken(), getRequester());
		srcDir.copyTo((TSRMLocalDir)_localTgt, _recursiveLevel, getSpaceToken(), accessInfo, this);
	    } else {
		TSRMLocalFile srcFile = (TSRMLocalFile)_localSrc;
		TSRMLocalFile tgtFile = (TSRMLocalFile)_localTgt;
		
		tgtFile.setToken(getSpaceToken());
		TSRMNameSpace.addEntry(tgtFile.getSiteURL().getSURL().toString(), tgtFile, this);
		
		TDeviceAccessInfo accessInfo = getSpaceToken().getHostDevice().getAccessInfo(getSpaceToken(), getRequester());
		/*
		if (tgtFile.isReady() && getOverwriteMode() == TOverwriteMode.NEVER) {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
		    cleanUp();
		    return;
		} else {
		*/
		    tgtFile.copyFrom(srcFile, _srcSiteURL.getSourceURL(), accessInfo);
		    //tgtFile.pin(this, TStatusCode.SRM_FILE_PINNED);
		    tgtFile.pin(this, TStatusCode.SRM_SUCCESS);
		    tgtFile.updateToken();
		    //}	       		
	    }
	    
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)); // will make it pinned?	  
	} catch (TSRMException e) {
	    handleException(e);
	} catch (RuntimeException e) {
	    handleException(e);
	} finally {
	    unpinMe();
	    if (isFailed()) {
		cleanUp();
	    }
	}
    }

    private boolean pinMe() {
	if (_localSrc == null) {
	    throw new TSRMException("Src is gone!", false);
	}

	if (_localSrc.isDir()) {
	    TSRMLocalDir srcDir = (TSRMLocalDir)_localSrc;
	    srcDir.pin(this, _recursiveLevel, TStatusCode.SRM_FILE_IN_CACHE);
	} else {
	    TSRMLocalFile srcFile = (TSRMLocalFile)_localSrc;
	    if (!srcFile.isValid()) {
		setReturnStatusSurlExpired("Surl has no lifetime available.");
		return false;
	    } else {
		srcFile.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
	    }
	}
	return true;
    }

    private void unpinMe() {
	if (_localSrc.isDir()) {
	    TSRMLocalDir srcDir = (TSRMLocalDir)_localSrc;
	    srcDir.unpin(this, _recursiveLevel);
	} else {
	    TSRMLocalFile srcFile = (TSRMLocalFile)_localSrc;
	    srcFile.unpin(this);
	}
    }

    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	
    }
	
    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }
	
    public void resume() {
	
    }
}
