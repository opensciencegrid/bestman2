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
//import gov.lbl.srm.storage.TSRMStorage;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
 
import gov.lbl.srm.util.TSRMException;

//
// doesnt seem to need token
// if staging is needed, can use volatile space
// but if token is provided, should use token instead of volatile space?
//
public class TSRMRequestCopy3rdParty extends TSRMRequestCopy {
    TSupportedURL _tgtSURL = null;
   
    public TSRMRequestCopy3rdParty(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSupportedURL tgtSURL) {
	//	this(r, src.getSourceURL(), tgtSURL, userReq);
	super(r, src.getSourceURL(), tgtSURL, userReq);
	
	_tgtSURL = tgtSURL;
	if (_tgtSURL.isProtocolSRM()) {
	    TSupportedURLWithSRMRefToRemote temp = (TSupportedURLWithSRMRefToRemote)tgtSURL;
	    temp.setRelatedToken(r.getSpaceToken());
	    temp.setRelatedDirOp(r.getDirOption());
	    temp.useLifetime(r.getFileLifetime());
	    temp.setOverwriteMode(((TUserRequestCopy)userReq).getOverwriteMode());
	    temp.setFileStorageType(((TUserRequestCopy)userReq).getTargetFileStorageType());
	}
	_proposedPinDuration = -1;
	
	//   super(r, src, tgt, userReq);
    }

    public TSRMRequestCopy3rdParty(TSRMFileInputCopy r,  
				   TSupportedURL srcSURL, TSupportedURL tgtSURL, TUserRequest userReq) 	 
    {
	super(r, srcSURL, tgtSURL, userReq);

	_tgtSURL = tgtSURL;	
	if (_tgtSURL.isProtocolSRM()) {
	    TSupportedURLWithSRMRefToRemote temp = (TSupportedURLWithSRMRefToRemote)tgtSURL;
	    temp.setRelatedToken(r.getSpaceToken());
	    temp.setRelatedDirOp(r.getDirOption());
	    temp.useLifetime(r.getFileLifetime());
	    temp.setOverwriteMode(((TUserRequestCopy)userReq).getOverwriteMode());
	    temp.setFileStorageType(((TUserRequestCopy)userReq).getTargetFileStorageType());
	}
	_proposedPinDuration = -1;
	schedule();	
    }

    public String description() {
	return super.description()+" tgt=\""+_tgtSURL.getURI()+"\"";
    }
    
    public boolean willRelayToken() {
	return true;
    }
    
    public ISRMLocalPath getPinnedPath() {
	return null;  
    }
    
    public void dirSync(TSRMSource subPath) {
	// no dir sync is handled here
	// since putting to remote srm needs to be a file level function
    }
    
    TSRMRequestCopy createChildRequest(TSRMSource curr) { throw new TSRMException("Not supposed to be here!", false);}	    
    
    public void cleanUp() {};

    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) 
    {	   
	_tgtSURL.useCredential(getRequester().getGSSCredential());

	if (!_srcSiteURL.getSourceURL().checkExistence()) {
	    setReturnStatus(TSRMUtil.createReturnStatusInvalidPath("Source does not exist. "+_srcSiteURL.getSourceURL().getURLString()));
	}
    }

    //
    // status flow: QUEUED => IN_PROGRESS => PINNED => SUCCESS
    //              <<FAILURE>> can occur anywhere
    //
    void work() {
	try {
	    ISRMTxfHandler txfHandler = null;
	    txfHandler = _tgtSURL.uploadFrom(_srcSiteURL.getSourceURL(), getRequester().getGSSCredential());	   

	    if (txfHandler != null) {
		txfHandler.action();			   
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)); 
	    } else {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Cannt handle upload"));
	    }

	} catch (TSRMException e) {
	    setReturnStatus(e.getReturnStatus());		
	    TSRMLog.exception(TSRMRequestCopy3rdParty.class, "details", e); 
	} catch (RuntimeException ex) {
	    setReturnStatusFailed(ex.getMessage());
	    TSRMLog.exception(TSRMRequestCopy3rdParty.class, "details", ex);	    	    
	} finally {}	
    }
    
    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
	
    }
	
    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }
	
    public void resume() {
	
    }
	
}
