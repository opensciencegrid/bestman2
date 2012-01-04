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
public class TSRMRequestCopyToRemote extends TSRMRequestCopy {
    ISRMLocalPath _localSrc = null;
    TSupportedURL _tgtSURL = null;
   
	 public TSRMRequestCopyToRemote(TUserRequest userReq, TSRMFileInputCopy r, TSRMSource src, TSupportedURL tgtSURL) {
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

    public TSRMRequestCopyToRemote(TSRMFileInputCopy r,  
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
		String currStr = _srcSiteURL.asString();
		String longerStr = subPath.asString();
		
		int idx = longerStr.indexOf(currStr);
		if (idx == -1) {
			TSRMLog.debug(this.getClass(), null, "event=nodirSyncDone", "base="+currStr+" tgt="+longerStr);
		} else {
			String addition = longerStr.substring(idx+currStr.length());
			if (addition.startsWith("/")) {
				addition = addition.substring(1);
			}
			TSupportedURL baseTgt = _tgtSURL;
			while (true) {
				idx = addition.indexOf("/");
				
				if ((idx == -1) || (idx == addition.length()-1)) {
					TSupportedURL tgt=baseTgt.createSubdir(addition);	
					tgt.mkdir(getRequester().getOwner());
					TSRMLog.debug(this.getClass(), null, "event=dirSync", "tgt="+tgt.getURLString()+" addition="+addition+" long="+longerStr);
					break;
				} else {
					String currDirName = addition.substring(0, idx);
					TSupportedURL tgt=baseTgt.createSubdir(currDirName);
                    tgt.mkdir(getRequester().getOwner());
                    TSRMLog.debug(this.getClass(), null, "event=dirSyncRec", "tgt="+tgt.getURLString()+" addition="+currDirName+" long="+longerStr);
					addition = addition.substring(idx+1);
					baseTgt = tgt;
				}
			}
		}
	}

    TSRMRequestCopy createChildRequest(TSRMSource curr) {
		String currStr = _srcSiteURL.asString();
        String longerStr = curr.asString();
		int idx = longerStr.indexOf(currStr);
		TSupportedURL tgt = null;
        if (idx == -1) {
            TSRMLog.debug(this.getClass(), null, "event=noMirrorForTgtInChildRequest", "base="+currStr+" tgt="+longerStr);
			tgt = _tgtSURL.createSubpath(curr.getSourceURL().getEndFileName());
		} else {
			idx = idx+ currStr.length();
			String subpath = longerStr.substring(idx);
			TSRMLog.debug(this.getClass(), null, "event=MirrorForTgtInChildRequest idx="+idx, "base="+currStr+" tgt="+longerStr+" sp="+subpath);
        	tgt=_tgtSURL.createSubdir(subpath);
			String test = TSRMUtil.createPath(_tgtSURL.getURLString(), subpath);
			TSRMLog.debug(this.getClass(), null, "event=Mirror tgtSURL="+_tgtSURL.getURLString(),"test="+test+" tgt="+tgt.getURLString());
		}
		//TSupportedURL tgt = _tgtSURL.createSubpath(curr.getSourceURL().getEndFileName());
		TSRMRequestCopyToRemote result =new TSRMRequestCopyToRemote(getRequester(), this.getInput(), curr, tgt);
		//RuntimeException hi = new RuntimeException("look:\n=>"+curr.asString()+"\ntgt="+tgt.getURLString()+"\n=>local="+_srcSiteURL.asString()+"\n=>curr="+curr+"\n=>pointer="+this+"\n=>result="+result);
        //hi.printStackTrace();
		//System.out.println("........"+result);
		return result;
    }	
    
    public void cleanUp() {};

    // wont differentiate dir or local
    // remote tgt should do ls() to this SRM and the create sub dir themselves
    public void initTgt(boolean srcIsDir, TSupportedURL tgtSURL) 
    {	   
	if (_srcSiteURL.getSourceURL().isDeviceSpecific()) {
	    _localSrc = (_srcSiteURL.getLocalPath());
	} else {
	    _localSrc = (_srcSiteURL.getSourceURL().getLocalPath());
	}

	if (_localSrc == null) {
	    _localSrc = _srcSiteURL.getLocalPath(); // this is added to accomodate the case when source is a local file:// 06-12-2007
	}

	_tgtSURL.useCredential(getRequester().getGSSCredential());

	if (_localSrc == null) {
	    //throw new TSRMException("No src found with:"+_srcSiteURL.getSourceURL().getURLString(), false);
	    setReturnStatusFailed("No src found with:"+_srcSiteURL.getSourceURL().getURLString());
	} else if (_localSrc.isDir()) {	    
	    //setReturnStatusFailed("We do not support uploading a dir to remote location");
	    TSRMLog.debug(this.getClass(), null, "expectingDir="+_srcSiteURL.getSourceURL().getURLString(), null);
		if (!_tgtSURL.checkExistence()) {
        	setReturnStatus(TSRMUtil.createReturnStatusInvalidPath("No such path:"+_tgtSURL.getURLString()));
		}
	} else if (!_srcSiteURL.getSourceURL().checkExistence()) {
		setReturnStatus(TSRMUtil.createReturnStatusInvalidPath("Source does not exist. "+_srcSiteURL.getSourceURL().getURLString()));
	}
    }


    //
    // status flow: QUEUED => IN_PROGRESS => PINNED => SUCCESS
    //              <<FAILURE>> can occur anywhere
    //
    void work() {
/*
	if (_srcSiteURL == null) {
		RuntimeException hii = new RuntimeException(this+"  "+super.description());
		hii.printStackTrace();
		setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized(null));
        return;
	} 
		
	if (!authorized(_srcSiteURL, TSRMPermission.Readable)) {
	    setReturnStatus(TSRMUtil.createReturnStatusNotAuthorized(null));
	    return;
	}
	*/
	if (_localSrc.isDir() && !_tgtSURL.checkExistence()) {
		setReturnStatus(TSRMUtil.createReturnStatusInvalidPath("No such path:"+_tgtSURL.getURLString()));
		return;
	}
	if (_localSrc.isDir()) {
        TSRMSourceDir srcDir = (TSRMSourceDir)(_srcSiteURL);
        srcDir.dissipate(this);
        return;
    }
	
	TSRMLocalFile f = ((TSRMLocalFile)_localSrc);  // dir should have gotten exception and cannt reach here
	if (!f.isValid()) {
	    setReturnStatusSurlExpired("Surl has no lifetime available");
	    return;
	}
	TSRMLocalFile curr = f.getPhysicalLocation().getDevice().getStagingFile(_srcSiteURL.getSourceURL(), f.getCurrentSize(), this);

	TSRMLog.info(this.getClass(), null, "event=upload rid="+getIdentifier(), "from=\""+f.getCanonicalPath()+"\"");
	try {
	    ISRMTxfHandler txfHandler = null;

	    // start ftp from cache to source	    	    
	    f.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
	    if ((curr != null) && (curr != f)) {
		curr.download(_srcSiteURL.getSourceURL());
		//curr.pin(this, TStatusCode.SRM_FILE_PINNED);
		curr.pin(this, TStatusCode.SRM_FILE_IN_CACHE);
		txfHandler = _tgtSURL.uploadFrom(curr,  getRequester().getGSSCredential());
	    } else {
		txfHandler = _tgtSURL.uploadFrom(_localSrc,  getRequester().getGSSCredential());
	    }

	    if (txfHandler != null) {
		txfHandler.action();			   
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)); 
	    } else {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Cannt handle upload"));
	    }

	} catch (TSRMException e) {
	    _srcSiteURL.detachFromLocalCache();
	    
	    if (e.isRetriable()) {			 
		TSRMLog.debug(this.getClass(), null, "event=rescheduling surl=\""+_srcSiteURL.getSourceURL().getURLString()+"\"", "exception="+e.toString());
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, e.toString()+" SRM will retry."));
		TSRMServer._gProcessingUnit.submitJob(this);
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=file-failed", "src=\""+_srcSiteURL.getSourceURL().getURLString()+"\"");
		//setReturnStatusFailed(e.getMessage());
		setReturnStatus(e.getReturnStatus());		
		TSRMLog.exception(TSRMRequestCopyToRemote.class, "details", e);	    
	    }
	} catch (RuntimeException ex) {
	    TSRMLog.debug(this.getClass(), null, "event=file-failed", "src=\""+_srcSiteURL.getSourceURL().getURLString()+"\"");
	    _srcSiteURL.detachFromLocalCache();
	    setReturnStatusFailed(ex.getMessage());	   
	    TSRMLog.exception(TSRMRequestCopyToRemote.class, "details", ex);	    
	} finally {
	    if ((curr != null) && (curr != f)) {
		curr.unpin(this);
	    }
	    f.unpin(this);	   
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
