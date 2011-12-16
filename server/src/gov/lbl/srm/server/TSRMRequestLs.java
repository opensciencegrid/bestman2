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

import java.util.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;
import gov.lbl.srm.util.*;
import org.apache.axis.types.*;

public class TSRMRequestLs extends TSRMRequest {
    private String _desc = null;

    public TSRMRequestLs(TUserRequest userReq) {
	super(-1, userReq);
	//setUserRequest(userReq);

	schedule();
    }

    public void schedule() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
	    TSRMServer._gProcessingUnit.submitLSJob(this);
	    TSRMLog.debug(this.getClass(), null, "event=schedule-ls",  description());  
	}
    }

    public void cleanMe() {
	TSRMLog.debug(this.getClass(), null, "event=cleanMe", null);
	_userRequest = null;
    }

    public void cleanUp() {}

    public void setTrustedSize(long s)
    {}

    public void abortMe(TStatusCode statusCode, boolean implicitAbort)
    {}

    public void resume() {}
   
    public TReturnStatus removeFileInCache() {
	throw new TSRMException("Not supporting this in ls request.", false);
    }

    public ISRMLocalPath getPinnedPath() {
	throw new TSRMException("Not supporting this in ls request.", false);    
    }

    public long getEstProcessingTimeInSeconds() {
	return -1;
    }

    public long getEstWaitTimeInSeconds() {
	return -1;      
    }

    public void setReceivedBytes(long bytes) 
    {}

    public String description() {
	if (_desc == null) {
	    _desc = "rid="+getRequester().getID();
	}
	return _desc;
    }

    public void runMe() {
	if (!isQueued()) {
	    return;
	}

	setStatusInProgress();

	try {
	    Vector result = work();

	    if (result == null) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "probably aborted."));
	    } else if (result.size() == 0) {
		setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "no input though."));
	    } else {
		TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	    
		for (int i=0; i<result.size(); i++) {
		    TMetaDataPathDetail curr = (TMetaDataPathDetail)(result.get(i));
		    reqSummary.add(curr);
		}
		setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	    } 	    

	    ((TUserRequestLs)getRequester()).setResult(result);
	} catch (TSRMException e) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));
	    ((TUserRequestLs)getRequester()).setFailed(e.getMessage());
	} catch (Exception e) {
	    setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));
	    ((TUserRequestLs)getRequester()).setFailed("ErrMsg="+e.getMessage());
	}
    }
    

    private Vector work() {
	Vector pathList = new Vector();
	//URI[] inputPathArray = ((TUserRequestLs)getRequester()).getInputArray();
	TUserRequestLs lsRequester = ((TUserRequestLs)getRequester());
	int inputSize = lsRequester.getInputArraySize();

	TSRMFileListingOption listingOp = ((TUserRequestLs)getRequester()).getListingOp();
	TFileStorageType ftype = ((TUserRequestLs)getRequester()).getFileStorageType();
	TAccount user = getRequester().getOwner();

	TSRMLog.debug(this.getClass(), null, "inputArraySize="+inputSize, null);

	for (int i=0; i<inputSize; i++) {	 
	    URI uri = lsRequester.getNthInputArray(i);
	    if (isAborted()) {
		pathList = null;
		return null;
	    }
	    try {
		TSURLInfo surlInfo = new TSURLInfo(uri, ((TUserRequestLs)getRequester()).getStorageSystemInfo());
		TSupportedURL url = TSupportedURL.createAndCheckSiteFileName(surlInfo, user, TSRMPermission.Readable);
		url.useCredential(getRequester().getGSSCredential());

		if (url.isDeviceSpecific()) {
		    /*
		    ISRMLocalPath f = url.getLocalPath();

		    if (f != null) {
			Vector result = f.ls(ftype, listingOp);
			pathList.addAll(result);
		    } else {			
			pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, TSRMUtil.createReturnStatusInvalidPath(null)));
		    }	
		    */
		    Vector result = url.ls(uri, listingOp, ftype);
		    if (result == null) {
			pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, TSRMUtil.createReturnStatusInvalidPath(null)));
		    } else {
			pathList.addAll(result);
		    }
		} else if (url.isProtocolFILE()) {
		    TSupportedURLWithFILE fileUrl = (TSupportedURLWithFILE)url;
		    fileUrl.useCredential(getRequester().getGSSCredential());
		    //return fileUrl.ls(uri, listingOp);
		    pathList.addAll(fileUrl.ls(uri, listingOp, ftype));
		} else if (!url.isProtocolSRM()) {
		    //if (url.isProtocolGSIFTP()) {
		    //pathList.add(((TSupportedURLWithGSIFTP)url).ls(listingOp));
		    //} else {
		    throw new TSRMException("We do not support browsing on non srm cache.", false);
		    //}
		} else {
		    TReturnStatus status = user.checkPathIsLocalWithPermission(url, TSRMPermission.Readable, true);
		    if (status != null) {
			pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, status));
		    } else {
			ISRMLocalPath path = url.getLocalPath();
		        if (path.isSurl()) {
			    listingOp.setShowChecksum(!(path.isDir()));
			    pathList.addAll(path.ls(ftype, listingOp));
			} else {
			    pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, TSRMUtil.createReturnStatusInvalidPath("not a surl")));
			}
		    }
		}
	    } catch (TSRMException e) {
		TSRMLog.exception(this.getClass(), "ls(),"+uri.toString(), e);
		pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, TSRMUtil.createReturnStatus(e)));
	    } catch (Exception e) {
		TSRMLog.exception(this.getClass(), "ls(),"+uri.toString(), e);
		pathList.add(TSRMUtil.createTMetaDataPathDetail(uri, TSRMUtil.createReturnStatusInvalidPath(e.getMessage())));
	    }
    	}

	return pathList;

    }
}
