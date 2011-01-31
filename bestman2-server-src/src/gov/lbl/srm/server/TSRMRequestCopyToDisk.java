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
import gov.lbl.srm.util.TSRMLog;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*; 

import gov.lbl.srm.util.TSRMException;

public class TSRMRequestCopyToDisk extends TSRMRequestCopy {
    public TSRMRequestCopyToDisk(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSRMSource tgt) {
	super(r, src, tgt, userReq);
    }

    public TSRMRequestCopyToDisk(TSRMFileInputCopy r,  TSupportedURL srcSURL, TSupportedURL tgtSURL, TUserRequest userReq) {
	super(r, srcSURL, tgtSURL, userReq);

	schedule();
    }

    public void cleanUp() {
    }

	public boolean assignSpaceToken(String userProvidedToken, TFileStorageType type) {
		TSRMLog.info(this.getClass(), null, "event=assignNoToken", null);
		return true;
	}
    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) {
	if (srcIsDir) {
	    if (!tgtSURL.isDir()) {
		ISRMLocalPath localPath = tgtSURL.getLocalPath();
		
		if (localPath == null) {
		    throw new TSRMException("No such dir:"+tgtSURL.getURLString(), false);
		} 
		
		if (!localPath.isDir()) {
		    throw new TSRMException("Can not copy from dir to a file", false);
		}
	    }
	    
	    _tgtSiteURL = new TSRMSourceDirUserOwned(tgtSURL,getRequester().getOwner());
	    _srcSiteURL.setLocalDestination(_tgtSiteURL.getLocalPath());
	} else {
	    if (tgtSURL.isDir()) {
		//TSURLInfo sub = tgtSURL.createSubpath(_srcSiteURL.getSourceURL().getEndFileName());
		TSupportedURL sub = tgtSURL.createSubpath(_srcSiteURL.getSourceURL().getEndFileName());
		
		_tgtSiteURL = new TSRMSourceFileUserOwned(sub);
		
		TSRMLocalFile f = ((TSRMSourceFileUserOwned)_tgtSiteURL).getLocalDestination();
		
		_srcSiteURL.setLocalDestination(f);	
		//_tgtSiteURL = tgtFile;
	    } else {
		// assuming it is a file...maybe need to detect the
		// case when the path does not exist (e.g. missing subpath)
		//
		if ((tgtSURL.getAccessObj() != null) && tgtSURL.getAccessObj().isBrokenPath()) {
		    throw new TSRMException("The path is broken.", false);
		}
		
		_tgtSiteURL = new TSRMSourceFileUserOwned(tgtSURL);
		_srcSiteURL.setLocalDestination(_tgtSiteURL.getLocalPath());
	    }
	}
    }
    
    void work() {
	if (_srcSiteURL.isDir()) {
	    TSRMUtil.startUpInfo(".........dissipating..."+_srcSiteURL);
	    TSRMSourceDir srcDir = (TSRMSourceDir)(_srcSiteURL);
	    srcDir.dissipate(this);	   
	    return;
	} 

	if (!pickupIOToken()) {
	    return;
	}

	try {
	    // nothing to authorize for
	    // start ftp from source to cache
	    //if (!authorized(_srcSiteURL.getSourceURL(), TSRMPermission.Readable)) {
		//setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized(null));
	    //} else {
		_srcSiteURL.getSourceURL().useCredential(getRequester().getGSSCredential());

		TSRMLocalFile srcFile = _srcSiteURL.getSourceURL().getLocalFile();
		if (_tgtSiteURL.getSourceURL().isProtocolFILE() && (srcFile != null) ) {
		    ISRMTxfHandler txf = _tgtSiteURL.getSourceURL().uploadFrom(srcFile, getRequester().getGSSCredential());
		    if (txf != null) {
			txf.action();
		    }
		} else {
		    _srcSiteURL.transferMe(this);
		}
		TSRMLog.debug(this.getClass(), null, "state=file-in-usr-address", "src=\""+_srcSiteURL.getSourceURL().getURLString()+"\"");
			      
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)); // make it pinned?
	     // }
	} catch (TSRMException e) {	    
	    TSRMLog.exception(TSRMRequestCopyToDisk.class, "details", e);	    
	    _srcSiteURL.detachFromLocalCache();
	    
	    if (e.isRetriable()) {			 
		TSRMLog.debug(this.getClass(), null, "action=rescheduling src=\""+ _srcSiteURL.getSourceURL().getURLString()+"\"", "reason="+e.toString());
			      
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, 
							    e.toString()+" SRM will retry."));
		TSRMServer._gProcessingUnit.submitJob(this);
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=file-failed src=\""+ _srcSiteURL.getSourceURL().getURLString()+"\"", null);
		//setReturnStatusFailed(e.getMessage());
		setReturnStatus(e.getReturnStatus());		
		TSRMLog.exception(TSRMRequestCopyToDisk.class, "details", e);	    
	    }
	} catch (RuntimeException ex) {
	    TSRMLog.debug(this.getClass(), null, "event=file-failed src=\""+ _srcSiteURL.getSourceURL().getURLString()+"\"",null);
	    TSRMLog.exception(TSRMRequestCopyToDisk.class, "details", ex);	    
	    
	    _srcSiteURL.detachFromLocalCache();
	    setReturnStatusFailed(ex.getMessage());
	} finally {	    
	    dropOffIOToken();
	}
    }
    
    public void abortMe(TStatusCode statusCode, boolean implicitAbort) {
		
    }
    
    public TReturnStatus removeFileInCache() {
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Not supporting this yet.");
    }
    
    public void resume() {
	
    }
	
    public ISRMLocalPath getPinnedPath() {
	return null; // no need for pin here
    }
	
    TSRMRequestCopy createChildRequest(TSRMSource curr) {	
	return new TSRMRequestCopyToDisk(getRequester(), this.getInput(), curr, _tgtSiteURL);
	/*
	TSURLInfo tgtInfo = curr.getLocalPath().getSiteURL();
    TSupportedURL tgtUrl = TSupportedURL.create(tgtInfo);
    TSRMSource tgt = null;
    if (curr.isDir()) {
        tgt = new TSRMSourceDir(tgtUrl);
    } else {
        tgt = new TSRMSourceFile(tgtUrl);
    }
    return new TSRMRequestCopyFromRemote(getRequester(), this.getInput(), curr, tgt);
	*/
    }
}
