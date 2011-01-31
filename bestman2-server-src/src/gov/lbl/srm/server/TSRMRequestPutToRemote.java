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

public class TSRMRequestPutToRemote extends TSRMRequestPut {
    TSRMRequestPutToRemote(TSRMFileInput r, TUserRequest userReq) {
	super(r, userReq);
	if (userReq.getGSSCredential() == null){
		setReturnStatusFailed("No credential! Cann't put to remote site.");
	    throw new TSRMException("No credential. Cannt proceed.", false);
	}
    }

   
    public boolean checkTgtPath(TSRMFileInput r) {
	TSRMLog.debug(this.getClass(), null, "event=checkTgtPath", "action=takeAsIs");
	_tgtSite=new TSRMSourceFile(r.getSURLInfo());

	TSupportedURL tgtSURL = _tgtSite.getSourceURL();
	if (tgtSURL.isProtocolSRM()) {
	    TSupportedURLWithSRMRefToRemote temp = (TSupportedURLWithSRMRefToRemote)tgtSURL;
	    //temp.setRelatedToken(r.getSpaceToken());
	    temp.setRelatedDirOp(r.getDirOption());
	    temp.useLifetime(r.getFileLifetime());
	    temp.setFileStorageType(r.getFileStorageType());

	    TUserRequestPut userReq = (TUserRequestPut)(getRequester());
	    temp.setOverwriteMode(userReq.getOverwriteMode());
	}

	return true;
    }

    public boolean authorized(TSupportedURL url, TSRMPermission p) {
	TSRMLog.debug(this.getClass(), null, "event=authorizationIgnored", null);
	return true;
    }

    public boolean authorized(TSRMSource s, TSRMPermission p) {
	return true;
    }

    public TReturnStatus putDone() {
	TSRMLog.info(this.getClass(), null, "event=putDone status="+getReturnStatus().getStatusCode().toString(), description());

	if (getReturnStatus().getStatusCode() != TStatusCode.SRM_SPACE_AVAILABLE) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "putDone() is only for files currently being received.");
	}

	if (_tgtSite.getLocalDestination().getTxfFile().getPhysicalLocation().isFileTrivial()) {	    	    
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "No bytes found while srmPutDone() was called.");
	}
	try {	    
	    _tgtSite.getLocalDestination().getTxfFile().getPhysicalLocation().changeTransferState(false);
	    _tgtSite.getLocalDestination().getTxfFile().updateToken();
	
	    doQuickPin();

	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	    
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), description()+" putDone() failed.",  e);
	    setReturnStatusFailed("putDone() failed");
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage());
	}
    }    

    protected void doQuickPin() {
	
	if (!TSRMUtil.acquireSync(_pinMutex)) {
	    return;
	}

	TSRMLocalFile localFile = _tgtSite.getLocalDestination();

	try {
	    if (isSucc() || isFailed()) {
		    return;
		}
		if (getReturnStatus().getStatusCode() == TStatusCode.SRM_FILE_IN_CACHE) {
			return;
		}
	    if (!isPinned() && !isAborted() && !isReleased()) {		    		           		
	
		TSRMNameSpace.disableReplicas(_tgtSite.getSourceURL().getURLString(), this);
		localFile.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
		if (TSRMLog.getCacheLog() != null) {
		TSRMLog.getCacheLog().fileIsAdded(localFile, localFile.getCurrentSize(), localFile.getToken()); 
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_pinMutex);
	}	    
    }

    public boolean isCodeAllowed(TStatusCode code) {
	return true;
    }

    protected void handleFileDone(boolean checkTxfFile) {
	doQuickPin();
	
	TSRMLog.info(this.getClass(), null, "action=handleFileDone", "code="+getReturnStatus().getStatusCode());
	if (getReturnStatus().getStatusCode() !=TStatusCode.SRM_FILE_IN_CACHE) {
	    return;
	}

	ISRMTxfHandler txfHandler = null;

	try {
	    txfHandler = _tgtSite.getSourceURL().uploadFrom(_tgtSite.getLocalDestination(), getRequester().getGSSCredential());
	    if (txfHandler == null) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Cannt handle upload"));
	    }

	     txfHandler.action();			   
	     setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)); 
	 } catch (Exception e) {
	     e.printStackTrace();
	     setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));	 
	 } finally {
	    TSRMLocalFile f = _tgtSite.getLocalDestination();
	    
	    TSRMLog.debug(this.getClass(), null, "event=removeIntermediateFile", "file="+f.getSiteURL().getSURL().toString());
          
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().removeFile(f);
		}
	    //TSRMNameSpace.purge(f.getSiteURL().getSURL().toString(), true);
	    TSRMNameSpace.purge(_tgtSite.getSourceURL().getURLString(), true);

	    f.detachFromParent();
	    f.unsetTokenIndependant(false);
	    
	     //_tgtSite.getLocalDestination().unpin(this);
	 }
    }
}
