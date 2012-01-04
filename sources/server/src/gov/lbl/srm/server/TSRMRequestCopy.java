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

import gov.lbl.srm.util.TSRMException;
 
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

//import org.apache.axis.types.URI;

public abstract class TSRMRequestCopy extends TSRMRequest {
    TSRMSource _tgtSiteURL 	= null;  // not always able to decide whether it is dir/file, so use _tgtSURLInfo
    TSRMSource _srcSiteURL  = null;

    TSRMFileInputCopy _input = null;

    long  _actualSizeInBytes = -1;

    boolean _skipPostInit = false;
    
    public TSRMRequestCopy(TSRMFileInputCopy r, TSRMSource src, TSRMSource tgt, TUserRequest userReq) {
	super(r, userReq);
	//super.setUserRequest(userReq);
	super.setProposedPinDuration(r.getFileLifetime());
	
	_skipPostInit = true;

	_srcSiteURL = src;
	_tgtSiteURL = tgt;
	
	try {
	    _actualSizeInBytes = _srcSiteURL.getTrustedSize();
	} catch (TSRMException e) {
	    //setReturnStatusFailed(e.getMessage());
	    setReturnStatus(e.getReturnStatus());
	}
    }

    public TSRMRequestCopy(TSRMFileInputCopy r, TSupportedURL srcSURL, TSupportedURL tgtSURL, TUserRequest userReq) {
	super(r,userReq);
	//super.setUserRequest(userReq);

	assignSpaceToken(r.getSpaceToken(), r.getFileStorageType());
	super.setProposedPinDuration(r.getFileLifetime());

	_input = r;

	_srcSiteURL = new TSRMSourceFile(srcSURL); // keep the srcSURL,
	_tgtSiteURL = new TSRMSourceFile(tgtSURL); // keep the tgtSURL, for later use to call postInit()	
    }


    public TSRMFileInputCopy getInput() {
	return _input;
    }

    public void postInit(TDirOption dirOp, TSupportedURL srcSURL, TSupportedURL tgtSURL) {
	if (dirOp == null) {
	    TSRMLog.info(this.getClass(), null, "dirOptionReceived=null",null);
	} else {
	    String content = "IsSourceADir="+dirOp.isIsSourceADirectory();
	    if (dirOp.getAllLevelRecursive() == null) {
		content +=" allLevelRecursive=null";
	    } else {
		content +=" allLevelRecursive="+dirOp.getAllLevelRecursive().booleanValue();
	    }
	    if (dirOp.getNumOfLevels() == null) {
		content += " numLevels=null";
	    } else {
		content += " numLevels="+dirOp.getNumOfLevels().intValue();
	    }
	    TSRMLog.info(this.getClass(), null, "event=dirOpDisplay", content);
	}

	_srcSiteURL = null;
	_tgtSiteURL = null;

	_skipPostInit = true;

	srcSURL.useCredential(getRequester().getGSSCredential());
	tgtSURL.useCredential(getRequester().getGSSCredential());
	//if ((dirOp != null)&& (dirOp.isIsSourceADirectory())) {
	//    handleDirSrc(srcSURL, tgtSURL, dirOp);
	//} else {
	    boolean srcIsDir = false;
	    try {		
		srcIsDir = srcSURL.isDir();
	    } catch (Exception e) {
		//
		TSRMLog.exception(TSRMRequestCopy.class, "details", e);
	    }

	    if (srcIsDir) {
		handleDirSrc(srcSURL, tgtSURL, dirOp);
	    } else {
		_srcSiteURL = TSRMSourceFile.create(srcSURL);
		_srcSiteURL.setToken(getSpaceToken());
		initTgt(false, tgtSURL);
	    }
	//}

	if (!isQueued()) {
	    return;
	}

	try {
	    _actualSizeInBytes = _srcSiteURL.getTrustedSize();
	} catch (TSRMException e) {
	    //setReturnStatusFailed(e.getMessage());
	    setReturnStatus(e.getReturnStatus());
	}    
	
	if (tgtSURL.isDeviceSpecific()) {
	    return;
	}

	if (getSpaceToken() != null) {
	    long currAvailSize = getSpaceToken().getAvailableBytes();
	    
	    long srcSize = _srcSiteURL.getTrustedSize();
	    if (currAvailSize < srcSize) {
		getSpaceToken().compact(false);	
		currAvailSize = getSpaceToken().getAvailableBytes();
	    }
	    srcSize = _srcSiteURL.getTrustedSize();
	    
	    if (currAvailSize < srcSize) {
		throw new TSRMException("Token:"+getSpaceToken().getID()+" cannt accomodate: "+srcSize+" bytes.", false);
	    }
	}
    }
    
    public void handleDirSrc(TSupportedURL srcSURL, TSupportedURL tgtSURL, TDirOption dirOp) {
	_srcSiteURL = TSRMSourceDir.create(srcSURL, getRequester().getOwner());
	_srcSiteURL.setToken(getSpaceToken());

	initTgt(true, tgtSURL);

	if (!isQueued()) {
		return;
	}
	 
	((TSRMSourceDir)_srcSiteURL).setRecursiveLevel(dirOp);
	//_srcSiteURL.setLocalDestination(localPath);
	srcSURL.populateMe((TSRMSourceDir)_srcSiteURL);
    }

    public void schedule() {
	if (getReturnStatus().getStatusCode() == TStatusCode.SRM_REQUEST_QUEUED) {
	    //_source.setToken(getSpaceToken());
	    
	    TSRMServer._gProcessingUnit.submitJob(this);
	    TSRMLog.info(this.getClass(), null,  description(),"event=queued");  
	}
    }
    
    public TOverwriteMode getOverwriteMode() {
	TUserRequestCopy req = (TUserRequestCopy)getRequester();	
	return req.getOverwriteMode();
    }
    
    public String description() {
	//String ridDesc = "rid ="+getRequester().getID();
	String ridDesc = "rid="+getIdentifier();
	String fromDesc = "";

	if (_srcSiteURL != null) {
	    fromDesc =" from=\"" +_srcSiteURL.asString()+"\" ";
	}
	
	fromDesc += " pinDur="+getProposedPinDuration();

	if (_tgtSiteURL != null) {
	    return ridDesc+fromDesc+" to=\""+_tgtSiteURL.asString()+"\"";
	} else {
	    return ridDesc+fromDesc;
	}
    }
    
    public long getFileSize() {	
	return _actualSizeInBytes;
    }	

    public void setTrustedSize(long s) {
	_actualSizeInBytes = s;	
    } 
    
    public long getEstProcessingTimeInSeconds() {
	return -1;
    }
    
    public long getEstWaitTimeInSeconds() {
	return -1;
    }
    
    public void runMe() {
	if (getReturnStatus().getStatusCode() != TStatusCode.SRM_REQUEST_QUEUED) {
	    return;
	}	   
	
	//TSRMLog.info(this.getClass(), "scheduling-copy", description(), "postInit?:"+!_skipPostInit+" "+"tid="+Thread.currentThread().getName());
	TSRMLog.info(this.getClass(), null, description(), "postInit="+!_skipPostInit+" "+" event=scheduled this="+this);
	
	try{
	    if (!_skipPostInit) {
		postInit(_input.getDirOption(), _srcSiteURL.getSourceURL(), _tgtSiteURL.getSourceURL());
	    }
	} catch (Exception e) {
	    setReturnStatusFailed(e.getMessage());
	    TSRMLog.exception(TSRMRequestCopy.class, "details", e);
	    cleanUp();
	    return;
	}

	if (!isQueued()) {
	    cleanUp();
	    return;
	}

	setStatusInProgress();

	try {
	    work();
	} finally {
	    TSRMLog.info(this.getClass(), null, description(), "event=end");
	}
    }
    
    //
    // abstract functions
    //
    abstract void work();
    
    public abstract ISRMLocalPath getPinnedPath();

    abstract TSRMRequestCopy createChildRequest(TSRMSource curr);
	public void dirSync(TSRMSource curr) {};
    
    abstract void initTgt(boolean srcIsDir, TSupportedURL tgtSURL);
    //
    // static functions
    //
    public static TSRMRequestCopy create(TSRMFileInputCopy r, TUserRequest userReq) {
	String fromStr = r.getFromSURLInfo().getURI().toString();
	String toStr = r.getToSURLInfo().getURI().toString();

	TSRMLog.debug(TSRMRequestCopy.class, null, "event=create from=\""+fromStr+"\"", "to=\""+toStr+"\"");
	/*
	TSupportedURL from = TSupportedURL.createAndCheckSiteFileName(r.getFromSURLInfo(), 
								      userReq.getOwner(), TSRMPermission.Readable);
	TSupportedURL to   = TSupportedURL.createAndCheckSiteFileName(r.getToSURLInfo(),
								      userReq.getOwner(), TSRMPermission.Writable);
	*/
	TSupportedURL from = r.getFromSURLInfo();
	TSupportedURL to = r.getToSURLInfo();

	from.checkSFN(userReq.getOwner(), TSRMPermission.Writable);
	to.checkSFN(userReq.getOwner(), TSRMPermission.Writable);

	//TSRMLog.info(TSRMRequestCopy.class, "[from]="+from, " [to]="+to, null);
	boolean copyFromLocal = false;
	if (from.isProtocolSRM()) {
	    copyFromLocal = ((TSupportedURLWithSRM)from).isLocalToThisSRM();
	}
	
	boolean copyToLocal = false;
	if (to.isProtocolSRM()) {
	    copyToLocal = ((TSupportedURLWithSRM)to).isLocalToThisSRM();
	}

	if (from.isProtocolFILE()) {
	    if (copyToLocal) {
		//TSRMRequestCopyLocal result = new TSRMRequestCopyLocal(r, from, to, userReq);
		TSRMRequestCopyFromDisk result = new TSRMRequestCopyFromDisk(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
	    } else if (to.isProtocolFILE()) {
		TSRMRequestCopyToDisk result = new TSRMRequestCopyToDisk(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
	    } else if (to.isDeviceSpecific()) {
		TSRMRequestCopyToDisk result = new TSRMRequestCopyToDisk(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
	    } else {
		TSRMRequestCopyToRemote result = new TSRMRequestCopyToRemote(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
		//throw new TSRMException("Copy(file:, other) is not allowed", false);
	    }
	} else if (to.isProtocolFILE() || to.isDeviceSpecific()) {
	     if (r.getSpaceToken() != null) {
		 throw new TSRMException("Space token does not apply here.", false);
	    }
	    if (copyFromLocal) {
		//return new TSRMRequestCopyToDiskFromLocal(r, from, to, userReq);
		TSRMRequestCopyToDisk result = new TSRMRequestCopyToDisk(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
	    } else {
		TSRMRequestCopyToDisk result = new TSRMRequestCopyToDisk(r, from, to, userReq);
		userReq.add(fromStr, result);
		return result;
	    }
	}

	if (copyFromLocal && copyToLocal) {
	    TSRMRequestCopyLocal result = new TSRMRequestCopyLocal(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}
	if (copyFromLocal && !copyToLocal) {
	    TSRMRequestCopyToRemote result = new TSRMRequestCopyToRemote(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}
	if (from.isDeviceSpecific() && !copyToLocal) {
	    TSRMRequestCopyToRemote result = new TSRMRequestCopyToRemote(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}

	if (from.isDeviceSpecific() && copyToLocal) {
	    //TSRMRequestCopyLocal result = new TSRMRequestCopyLocal(r, from, to, userReq);
	    TSRMRequestCopyFromDisk result = new TSRMRequestCopyFromDisk(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}

	if (!copyFromLocal && copyToLocal) {
	    TSRMRequestCopyFromRemote result = new TSRMRequestCopyFromRemote(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}
	
	if (from.isProtocolGSIFTP() && to.isProtocolSRM()) {
	    TSRMRequestCopy3rdParty result = new TSRMRequestCopy3rdParty(r, from, to, userReq);
	    userReq.add(fromStr, result);
	    return result;
	}
	throw new TSRMException("Copy("+from.getProtocol()+", "+to.getProtocol()+") is not allowed", false);
    }
    
    public void setReceivedBytes(long bytes) 
    {}
}


