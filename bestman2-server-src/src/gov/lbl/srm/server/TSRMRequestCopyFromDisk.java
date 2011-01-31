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

import gov.lbl.srm.util.TSRMUtil;
import gov.lbl.srm.util.TSRMLog;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*; 
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.TSRMException;

public class TSRMRequestCopyFromDisk extends TSRMRequestCopy {
    ISRMLocalPath _localSrc = null;
    ISRMLocalPath _localTgt = null;

    public TSRMRequestCopyFromDisk(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSRMSource tgt) {
	super(r, src, tgt, userReq);
    }

    public TSRMRequestCopyFromDisk(TSRMFileInputCopy r,  TSupportedURL srcSURL, TSupportedURL tgtSURL, TUserRequest userReq) {
	super(r, srcSURL, tgtSURL, userReq);

	schedule();
    }

    TSRMRequestCopy createChildRequest(TSRMSource curr) {	
	return new TSRMRequestCopyFromDisk(getRequester(), this.getInput(), curr, null);
    }

    public ISRMLocalPath getPinnedPath() {
	return _localTgt; 
    }

    public void cleanUp() 
    {
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

    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) {
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
	    } else {
		if (getOverwriteMode() == TOverwriteMode.NEVER) {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, TSRMUtil._DefNoOverwrite));
		}	    	    

		TSRMLocalFile tgtFile = (TSRMLocalFile)_localTgt;
		if (!tgtFile.isReady()) {
		    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, "tgt file is busy"));
		}
	    }
	}
	    
	if (srcIsDir) {
	    if (!_localSrc.isDir()) {
		throw new TSRMException("Contradictory. actual src is not a dir.", false);
	    }	
	    // local tgt?
	    throw new TSRMException("Not supporting cp(dir/dir) yet", false);
	}

	if (!getRequester().getOwner().checkPermission(_localTgt.getOwner(), TSRMPermission.Writable)) {
	    setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized("No write permission"));
	}
    }

    void work() {
	//if (!authorized(_tgtSiteURL, TSRMPermission.Writable)) {
	//return;
	// }

	 try {
	     TSRMLocalFile srcFile = (TSRMLocalFile)_localSrc;
	     TSRMLocalFile tgtFile = (TSRMLocalFile)_localTgt;		
	     tgtFile.setToken(getSpaceToken());
	     TSRMNameSpace.addEntry(tgtFile.getSiteURL().getSURL().toString(), tgtFile, this);
	
	     TDeviceAccessInfo accessInfo = getSpaceToken().getHostDevice().getAccessInfo(getSpaceToken(), getRequester());

	     tgtFile.copyFrom(srcFile, _srcSiteURL.getSourceURL(), accessInfo);
	     //tgtFile.pin(this, TStatusCode.SRM_FILE_PINNED);
	     tgtFile.pin(this, TStatusCode.SRM_SUCCESS);
	     tgtFile.updateToken();
	 } catch (TSRMException e) {
	    handleException(e);
	 } catch (RuntimeException e) {
	     handleException(e);
	 } finally {}
    }

    public void abortMe(TStatusCode statusCode, boolean implictAbort) {
	
    }
	
    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }
	
    public void resume() {
	
    }
}
