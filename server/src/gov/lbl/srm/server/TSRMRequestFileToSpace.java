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
 
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

import org.apache.axis.types.URI;

public class TSRMRequestFileToSpace extends TSRMRequest {
    URI _surl = null;
    TSRMStorage.iSRMSpaceToken _token = null;   

    public TSRMRequestFileToSpace(URI surl, TSRMStorage.iSRMSpaceToken token, TUserRequest userReq, int pos) {
	super(pos, userReq);
	//setUserRequest(userReq);
	_surl = surl;
	_token = token;		
	schedule();
    }

    public URI getURI() {
	return _surl;
    }
    
    public TSRMStorage.iSRMSpaceToken getToken() {
	return _token;
    }

    public void setTrustedSize(long s) {
	// no use of it for now
    } 

    public void schedule() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
	    TSRMServer._gProcessingUnit.submitLSJob(this);
	    TSRMLog.info(this.getClass(), null, "event=queued",  description());  
	}
    }

    public void cleanUp() {}

    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	TSRMLog.info(this.getClass(), null,"comment=no_action_on_abort", description());
    }

    public TReturnStatus removeFileInCache() {
	throw new TSRMException("Why here?", false);
    }

    public String description() {
	if (_token != null) {
	    return "rid="+getRequester().getID()+" url="+_surl+" token="+_token.getID();
	} else {
	    return "rid="+getRequester().getID()+" url="+_surl+" token= null";
	}
    }

    public void setReceivedBytes(long bytes) {
	
    }

    public long getEstProcessingTimeInSeconds() {
	return -1;
    }
    
    public long getEstWaitTimeInSeconds() {
	return -1;
    }
    
    public ISRMLocalPath getPinnedPath() {
	TSRMLog.info(this.getClass(), null,"comment=getPinnedPath_is_not_implemented", description());
	return null;
    }

    public void resume() {

    }
    public void runMe() {
	if (!isQueued()) {
	    return;
	}

	setStatusInProgress();

	TSURLInfo surlInfo = new TSURLInfo(_surl, getRequester().getStorageSystemInfo());
       
	ISRMLocalPath path = null;
	try {
	    path = TAccountManager.getLocalPath(surlInfo, getRequester().getOwner(), TSRMPermission.Writable, true);
	} catch (TSRMException e) {
	    setReturnStatus(TSRMUtil.createReturnStatus(e));
	}

	if (path == null) {
	    setReturnStatusFailed("URI is not in local cache");
	    return;
	} 
	  
	if (path.isDir()) {
	    setReturnStatusFailed("URI is not a file.");
	    return;
	}

	TSRMLocalFile srcFile = (TSRMLocalFile)path;
	if (srcFile.getParent() == null) {
	    setReturnStatusFailed("Not a normal file in cache."+srcFile);
	    return;
	} 

	try {
	    srcFile.changeToken(this);
	} catch (RuntimeException e) {
	    TSRMLog.exception(this.getClass(), description(), e);
	    setReturnStatusFailed(e.getMessage());
	}    
    }   
}
