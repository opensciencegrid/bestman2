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
//import gov.lbl.srm.policy.TSRMGeneralEnforcement;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;

import org.apache.axis.types.URI;
import java.io.IOException;
import java.util.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;


public class TSRMLocalFile implements ISRMLocalPath {
    ///private String 			     _name 			= null; // dont want name to be empty
    private TSRMLocalDir 		     _parent 		       	= null;
    
    private TSRMStorage.iSRMSpaceToken       _token 			= null;
    
    public TSRMPhysicalLocation              _physicalFileLocation	= null;
    public boolean                           _isSFNAppointedByUser      = false;
    public boolean			     _isReady	       	        = false;
    ///public boolean                           _isObsolete                = false;

    public TSRMFileTxfState                  _txfState                  = TSRMFileTxfState.NONE; 
    public ISRMTxfHandler                    _txfObj                    = null;
    ///private long                             _timeStamp                 = -1; // time when file is materialized
    
    protected TMetaDataPathDetail            _detail 			= new TMetaDataPathDetail();
    private URI			             _transferURI	        = null;
    ///protected TSURLInfo		             _siteURL	 	        = null;
    public long                              _actualSize                = -1;
    public long                              _inheritedExpirationTimeInMilliSeconds  = -1;
   
    private TSRMMutex _transferMutex = new TSRMMutex();
    private Vector _consumers = new Vector(); // requests that want to pin this file       
   
    private TSRMPinManager _pinManager = new TSRMPinManager(this);    
    protected TSRMLocalPathAttributes _attributes = new TSRMLocalPathAttributes();
    private URI _sourceURI = null; 
    
    public TSRMLocalFile(TSRMLocalDir parent, String name, TFileStorageType fileType, boolean isSurlPath) {
	if (name == null) {
	    throw new RuntimeException("File name can not be null");
	}
	
	if (parent!=null) {
	    if (parent.isNameUsedInContents(name)) {
	        _attributes.setName(parent.getAlternateContentName(name));
	    } else {
		_attributes.setName(name);
	    }
	} else {
	    _attributes.setName(name);
	}
	
	_parent = parent;
	
	// other members of detail will be filled in later
	_detail.setPath(getSiteURL(false).getSURL().toString());
	_detail.setType(TFileType.FILE);
	_detail.setFileStorageType(fileType);
	
	if (isSurlPath) {
	    _attributes.setSurlPath();
	    TSRMLog.debug(this.getClass(), null, "event=created_file_surl,", stampPath());	
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=created_file_turl,", stampPath());
	}		     
    }
    
    public void addConsumer(TSRMRequest r) {
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return;
	} 
	try {
	    if (!_consumers.contains(r)) {
		_consumers.add(r);
	    }
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);
	}
    }

    public void removeConsumer(TSRMRequest r) {
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return;
	} 
	try {
	    _consumers.remove(r);
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);
	}
    }
    
    public boolean isDir() {
	return false;
    }
    
    public TFileStorageType getFileStorageType() {
	return _detail.getFileStorageType();
    }
    
    public TSRMStorage.iSRMSpaceToken getToken() {
	return this._token;
    }
    
    public URI getSourceURI() {
	return _sourceURI; //_detail.getPath();
    }
	
    public void setSourceURI(URI surl) {
	_sourceURI = surl;
	//_detail.setPath(surl.toString());
    }
    
    public TSRMLocalDir getParent() {
	return _parent;
    }
    
    public boolean isSurl() {
	return _attributes.isSurlPath();
    }

    public String getName() {
	return _attributes.getName();
    }
    
    public String getCanonicalPath() {
	if (getParent() == null) {
	    return getName();
	} else {			 
	    return getParent().getCanonicalPath() + getName();
	}
    }
    
    public void rename(String newName) {
	//TSRMLog.getCacheLog().removeFile(this);
	if (newName != null) {
	    _attributes.setName(newName);
	}
	//TSRMLog.getCacheLog().addFile(this);

	resetPath();
    }
    
    public void resetPath() {
	//TSRMLog.getCacheLog().removeFile(this);
	_detail.setPath(getSiteURL(true).getSURL().toString());
    }
    
    public void setSFNAppointedByUser() {
	_isSFNAppointedByUser = true;
    }

    public boolean isSFNUserAppointed() {
	return _isSFNAppointedByUser;
    }
    
    public Object getPhysicalPath() {
	if (_physicalFileLocation == null) {
	    return null;
	}
	if (_physicalFileLocation.getRef() == null) {
	    return null;
	}
	return _physicalFileLocation.getRef().getObj();
    }
    
    public void setPhysicalLocation(String accessPath, TDeviceAccessInfo accessInfo) 
    {
	claimPhysicalFileLocation();
	_physicalFileLocation = new TSRMPhysicalLocation(accessPath, this, accessInfo);
    }

    private void claimPhysicalFileLocation() {
	if (_physicalFileLocation != null) {
	    //_physicalFileLocation.deepClean();
	    _physicalFileLocation.remove(true);
	}
	_physicalFileLocation = null;
    }

    public void setPhysicalLocation(TSupportedURL name, 		
				    TDeviceAccessInfo accessInfo) 
    {
	if (_physicalFileLocation != null) {
	    if (_physicalFileLocation.getSrcRef() != name) {
		if (!compactIsFeasible()) {
		    return;
		}
		activate(false);
		_physicalFileLocation.remove(true);
		claimPhysicalFileLocation();
	    } else {
		return;
	    }
	}

	_physicalFileLocation = new TSRMPhysicalLocation(name, this, accessInfo);

	//_physicalFileLocation.getParentFile().mkdirs();
    }		 
    
    public TSRMPhysicalLocation getPhysicalLocation() {
	return _physicalFileLocation;
    }
    
    public TAccount getOwner() {
	if (this.getParent() != null) {
	    return this.getParent().getOwner();
	} else {
	    return null;
	}
    }

    public TSRMLocalDir getTopDir() {
	if (this.getParent() != null) {
	    return this.getParent().getTopDir();
	} else {
	    return null;
	}
    }    

    public TMetaDataPathDetail lsCommonFileNotReady() {
	 TMetaDataPathDetail result = new TMetaDataPathDetail();
	 result.setType(TFileType.FILE);
	 result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, "Not on device yet."));
	 return result;
    }
  
    public TMetaDataPathDetail lsCommon(TSRMFileListingOption lsOption) {
	TMetaDataPathDetail result = null;

	if ((getPhysicalLocation() != null) && isReady()) {
	    result = getPhysicalLocation().ls(lsOption); // size, type, lastModificationTime
	} else {
	    //throw new TSRMException("No device found to handle ls()");
	    result = lsCommonFileNotReady();
	} 

	if (_attributes.getCreatedAt() > 0) {
	    result.setCreatedAtTime(TSRMUtil.createGMTTime(_attributes.getCreatedAt()));
	}
	result.setFileStorageType(_detail.getFileStorageType());

        //result.setPath(getSiteURL().getSURL().toString()); // making sure path is an srm:// surl
	result.setPath(TSRMUtil.getAbsPath(getSiteURL().getSURL()));

	if ((_token != null) && (_token.getOwner() != TAccountManager._SUPER_USER)) {
	    String[] tokenArray = new String[1];
	    tokenArray[0] = _token.getID();
	    result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(tokenArray));
	} 	

	/*
	if (!isReady()) {
	    result.getStatus().setStatusCode(TStatusCode.SRM_FILE_BUSY);
	}
	*/

	return result;
    }

    private TRetentionPolicyInfo getRetentionPolicy() {
	if (getToken() != null) {
	    TRetentionPolicyInfo info = new TRetentionPolicyInfo();	   
	    info.setRetentionPolicy(getToken().getType().getRetentionPolicy());
	    info.setAccessLatency(getSurlLatency());
	    return info;
	}
	return null;
    }

    private TUserPermission getOwnerPermission() {
	TSRMLocalDir topDir = getTopDir();
	if (topDir != null) {
	    return TSRMUtil.createDefaultOwnerPermission(topDir.getName());
	} else {
	    return TSRMUtil.createDefaultOwnerPermission(getName());
	}
    }

    public Vector ls(TFileStorageType fileStorageType,// considerred		 
		     TSRMFileListingOption lsOption)
    {
	Vector resultList = new Vector(1);

	if ((_detail.getFileStorageType() != null) && (fileStorageType != null)) {
	    if (_detail.getFileStorageType() != fileStorageType) {
		TMetaDataPathDetail result = TSRMUtil.createTMetaDataPathDetail(getCanonicalPath(), // _detail.getPath(),
										TSRMUtil.createReturnStatusFailed(TSRMUtil._DefFileTypeMismatch));
		result.setFileStorageType(_detail.getFileStorageType());
		resultList.add(result);
		return resultList;
	    }
	}	       
	/* have to ignore this argument for files
	if (lsOption.getOutputCount() == 0) {
	    return resultList;
	} */

	TMetaDataPathDetail result = lsCommon(lsOption);	

	if (lsOption.isDetailNeeded()) {
	    result.setOtherPermission(TSRMUtil.createDefaultClientPermission());
	    result.setGroupPermission(TSRMUtil.createDefaultGroupPermission());
	    result.setFileLocality(getSurlLocality());

	    result.setRetentionPolicyInfo(getRetentionPolicy());
	    result.setOwnerPermission(getOwnerPermission());
	}

	if ((getToken() != null) && (getToken().isLifeTimeExpired())) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED, null));
	    if (lsOption.isDetailNeeded()) {
		result.setLifetimeLeft(TSRMUtil.createTLifeTimeInSeconds(0, false));
	    }
	} else if (isReady()) {
	    long remainingLifeTime = getLifeTimeRemainingInSeconds();

	    if (remainingLifeTime <= 0) {
		if ((getToken() != null) && (getToken().getType() != TSRMSpaceType.Permanent)) {
		    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED, null));
		    if (lsOption.isDetailNeeded()) {
			result.setLifetimeLeft(TSRMUtil.createTLifeTimeInSeconds(0, false));
		    }
		} else {
		    if (lsOption.isDetailNeeded()) {
			result.setLifetimeLeft(TSRMUtil.createTLifeTimeInSeconds(-1, false));
		    }
		}
	    } else {
		if (lsOption.isDetailNeeded()) {
		    long lifeTimePassed = getLifeTimeUsedInSeconds();
		    result.setLifetimeLeft(TSRMUtil.createTLifeTimeInSeconds(remainingLifeTime, false));
		    result.setLifetimeAssigned(TSRMUtil.createTLifeTimeInSeconds(lifeTimePassed + remainingLifeTime, false));
		}
	    }
	} else {
	    // lifetime does not apply, we donnt return anything
	}    

	resultList.add(result);
	return resultList;
    }

    private TFileLocality getSurlLocality() {
	if (!isReady()) {
	    return null;
	}

	if (!isSurl()) {
	    return null;
	}

	if (getCurrentSize() == 0) {
	    return TFileLocality.NONE;
	}

	return TSRMNameSpace.getLocality(getSiteURL().getSURL().toString());
    }
    
    private TAccessLatency getSurlLatency() {
	if (!isReady()) {
	    return null;
	}

	if (!isSurl()) {
	    return null;
	}

	if (getCurrentSize() == 0) {
	    return null;
	}

	return TSRMNameSpace.getLatency(getSiteURL().getSURL().toString());
    }
    
    public TReturnStatus mv(TSupportedURL url) {
	//TSupportedURL url = TSupportedURL.create(toPath);
	String[] pathArray = TSupportedURL.getPathInArray(url);
	
	// assuming that pathArray[0] is the account info
	TAccount user = TAccountManager.getAccount(pathArray[0]);
	if (user == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
					       "Target path refers to non-exist user:"+pathArray[0]);
	}
	TSRMLocalDir dir = user.getTopDir();
	if (dir != this.getTopDir()) {
	    return TSRMUtil.createReturnStatusNotAuthorized("mv() across user accounts.");
	}
	
	TSRMChangeDirStatus status = TAccount.changeDir(dir, "getLocalPath-mvFile", pathArray, 1);
	TSRMLocalDir tgtDir = status.getDir();
	
	if (tgtDir == null) {	       
	    return status.getStatus();
	}
	String tgtName = pathArray[pathArray.length-1];
	if (tgtDir.getFile(tgtName) != null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, 
					       "tgt file already exists in mv(file, file), cannt overwrite");
	} else {
	    this.getParent().rmFileName(this.getName());
	    
	    TSRMLocalDir temp = tgtDir.getSubDir(tgtName);
	    if (temp != null) { // will move into this direcotry
		temp.addFile(this);
	    } else { 
		rename(tgtName);
		tgtDir.addFile(this);
	    }
	    
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	}
    }
    
    public long getLifeTimeUsedInSeconds() {	
	long start = this._pinManager._firstPinIssuedTimeInMilliSeconds;
	if (_inheritedExpirationTimeInMilliSeconds > System.currentTimeMillis()) {
	    if (start < _inheritedExpirationTimeInMilliSeconds) {
		return 0;
	    } 
	} 
	if (start < 0) {
	    return -1; // no pin yet.
	}
	return (System.currentTimeMillis()-start)/(long)1000;	
    }
    
    public long getLifeTimeRemainingInSeconds() {
	/*
	long resultInMilliSeconds = this._pinManager.getLifeTimeRemainingInMilliSeconds();
	if (resultInMilliSeconds < 0) {
	    return -1;
	}
	*/
	long exp = getExpirationTimeInMilliSeconds();
	if (exp <= 0) {
	    return exp;
	}

	long resultInMilliSeconds = exp - System.currentTimeMillis();
	return  resultInMilliSeconds/(long)1000;
    }
    
    public boolean isThirdPartyTxfAssumed() {
	return false;
    }

    public String getDefaultURIString() {
	return getURIString(TSRMTxfProtocol.FILE);
    }

    public String getURIString(TSRMTxfProtocol protocol) {
	URI uri = getPhysicalLocation().getTransferURI(protocol);
	if (uri == null) {
	    throw new TSRMException("Cannt generate URI", false);
	} 
	return uri.toString();
    }
    
    //
    // if file does not exist, File.length() returns 0
    //
    public long getCurrentSize() {	  
	if (_physicalFileLocation == null) {
	    return -1;
	}
	if (_detail.getSize() != null) {
	    return _detail.getSize().longValue();
	}

	if (_actualSize > 0) {
	    return _actualSize;
	}
	return _physicalFileLocation.getSize();
    }  
	
    public Vector releaseSurl() {
	URI curr = getSiteURL().getSURL();

	Vector result = new Vector();
	if (!isSurl()) {
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "not an surl.")));
	    return result;
	}
	
	if (!isReady()) {
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE,
											 "Cannt release this file. Not materialized yet.")));
	    return result;
	}

	/*
	_inheritedExpirationTimeInMilliSeconds = 0;
	getPinManager().clearAllPins(true);
	result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_RELEASED, null)));
	TSRMLog.getCacheLog().fileIsAdded(this, this.getCurrentSize(), this.getToken()); 
	return result;	
	*/
	TSRMNameSpace.release(curr.toString());
	result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_RELEASED, null)));
	return result;
    }

    public TAccessLatency getLatency() {
	if (getToken() == null) {
	    return null;
	} 
	if (getToken().getHostDevice().isDisk()) {
	    return TAccessLatency.ONLINE;
	} else {
	    return TAccessLatency.NEARLINE;
	}
    }

    public TFileLocality getLocality() {
	if (getToken() == null) {
	    return null;
	} 
	if (getToken().getHostDevice().isDisk()) {
	    return TFileLocality.ONLINE;
	} else {
	    return TFileLocality.NEARLINE;
	}
    }

    public void switchToken() {
	if (getToken().isLifeTimeExpired()) {
	    unsetToken(false);
	    _isReady = false;
	} else {
	    throw new TSRMException("This surl is used in another token. Call rm() first", false);
	}
    }

    public void unsetToken(boolean doSetStatusRelease) {

	if (_token != null) {
	    TSRMLog.debug(this.getClass(), null, "event=unsetToken token="+_token.getID(), stampPath());

	    if (!TSRMUtil.acquireSync(_transferMutex)){
		return;
	    }
	    try {
		cleanToken(doSetStatusRelease);
	    } finally {
		TSRMUtil.releaseSync(_transferMutex);
	    }
	    _token = null;
	    _transferURI=null;
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=unsetToken token=null", stampPath());
	}	
    }

    public void unsetTokenIndependant(boolean doSetStatusRelease) { // is not called from space compacting functions
	if (_token != null) {
	    TSRMLog.debug(this.getClass(), null, "event=unset token="+_token.getID(), stampPath());

	    if (!TSRMUtil.acquireSync(_transferMutex)){
		return;
	    }
	    try {
		getPinManager().clearAllPins(doSetStatusRelease);		
		_token.removeFileIndependant(this, false);
		claimPhysicalFileLocation();
		adjustFileStorageType();
	    } finally {
		TSRMUtil.releaseSync(_transferMutex);
	    }
	    _token = null;
	    _transferURI=null;
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=unset token=null", stampPath());
	}	
    }

    // caller locks mutex.
    private void cleanToken(boolean doSetStatusRelease) {	    
	if (_token != null) {
	    getPinManager().clearAllPins(doSetStatusRelease);

	    TSRMLog.debug(this.getClass(), null, "event=cleanToken token="+_token.getID(), stampPath());
	    _token.removeFile(this, false);

	    _token = null;
	    claimPhysicalFileLocation();

	    adjustFileStorageType();
	}
    }

    private void adjustFileStorageType() {
	if (getToken() != null) {
	    if (getToken().getType() == TSRMSpaceType.Volatile) {
		_detail.setFileStorageType(TFileStorageType.VOLATILE);	    
	    } else if (getToken().getType() == TSRMSpaceType.Durable) {
		_detail.setFileStorageType(TFileStorageType.DURABLE);	    
	    } else {
		_detail.setFileStorageType(TFileStorageType.PERMANENT);
	    }
	} else {
	    _detail.setFileStorageType(null);
	}
    }

    private boolean tryCleanToken() {
	if (_token != null) {
	    if (compactIsFeasible()) {
		_token.removeFile(this, false);
		this.activate(true);
		return true;
	    } else {
		return false;
	    }
	} else {
	    return true;
	}
    }
    
    public void findSpaceInToken(TSRMStorage.iSRMSpaceToken token, long size) {
	if (getToken() == null) {
	    token.findSpaceForFile(this, size);
	}
    }

    public void setActualBytes(long bytes) {
	_actualSize = bytes;
    }

    public void setToken(TSRMStorage.iSRMSpaceToken token) {
	if (token == null) {
	    TSRMException ex = new TSRMException("Trying to set a NULL token.", false);
	    TSRMLog.exception(this.getClass(), "details:", ex);
	    throw ex;
	}
	
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return;
	}
	try {
	    if (token != _token) {
		if (!tryCleanToken()) {		
		    TSRMUtil.releaseSync(_transferMutex);
		    TSRMException ex = new TSRMException("Already has token:"+_token.getID() +"Release before assign:"+token.getID(), false);
		    TSRMLog.exception(this.getClass(), "details:", ex);
		    throw ex;
		}		
		
		_token = token;
		TSRMLog.debug(this.getClass(), null, "event=setToken token="+ _token.getID(), stampPath());			     

		adjustFileStorageType();
	    }
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);
	}
    }
	
    public boolean hasExceededReservedBytes() {
	return (getCurrentSize() > _token.getReservedBytes(this));
    }
    
    public void activate(boolean contentIsDeleted) {
	getPinManager().open(contentIsDeleted);
    }
    
    private void setReady() {
	_isReady = true;
	setBlock(TSRMFileTxfState.NONE, false);
    }

    public void updateToken() {
	//TSizeInBytes size = new TSizeInBytes();
	//size.setValue(getCurrentSize());
	if (isReady()) {
	    return;
	}
	TSRMLog.info(this.getClass(), null, "event=updateToken", stampPath());
	_detail.setSize(TSRMUtil.createTSizeInBytes(getCurrentSize()));
	
	if ((_token != null) &&  !_token.fileIsSettled(this)) {
	    throw new TSRMException("Cannt update the file size growth.", false);
	}
	setReady();
	///_timeStamp =  System.currentTimeMillis(); 
	///TSRMLog.info(this.getClass(), "time stamp of ", getName(), "is "+_timeStamp);		
	_attributes.setTimeStamp();
    }
    
    public long getTimeStamp() {
	return _attributes.getTimeStamp();
    }
    
    //
    // 0 => same age
    // > 0 => is younger
    // < 0 => is older
    public int isYoungerThan(TSRMLocalFile guest) {
	if (getTimeStamp() == -1) {
	    if (guest.getTimeStamp() == -1) {
		return 0;
	    } else {
		return 1;
	    }
	} else {
	    if (guest.getTimeStamp() == -1) {
		return 1;
	    } else {
		return (int)(guest.getTimeStamp() - getTimeStamp());		
	    }
	}
    }
	
    public boolean broadcast(TSRMException e) {
	TSRMLog.debug(this.getClass(), null, "action=BroadcastingSRMEx "+stampPath(),  "consumerSize="+_consumers.size()+" isWarning="+e.isWarning());

	if (e.isWarning()) {
	    return true;
	}

	if (_consumers.size() == 0) {
	    return false;
	}
	
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return false;
	}
	try {
	    int total = _consumers.size();
	    for (int i=total-1; i>=0; i--) {
		TSRMRequest r = (TSRMRequest)(_consumers.get(i));
		 r.handleException(e);
		_consumers.remove(i);			
	    }

	    cleanToken(false);
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);
	}
	return true;
    }

    public boolean broadcast(RuntimeException e){
	TSRMLog.debug(this.getClass(), null, "event=BroadcastingRTEx", stampPath());

	if (_consumers.size() == 0) {
	    return false;
	}
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return false;
	}
	try {
	    TSRMUtil.startUpInfo("## ## ## # of consumers" + _consumers.size());
	    int total = _consumers.size();
	    for (int i=total-1; i>=0; i--) {
		TSRMRequest r = (TSRMRequest)(_consumers.get(i));
		 r.handleException(e);
		_consumers.remove(i);			
	    }

	    cleanToken(false);
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);	    
	}

	return true;
    }
    public boolean pin() {
	boolean result = true;
	
	if (!TSRMUtil.acquireSync(_transferMutex)){
	    return false;
	}
	try {
	    TSRMLog.debug(this.getClass(), null, "event=pin consumerSize="+_consumers.size(), stampPath());			  
	    long currSize = getCurrentSize();

	    if (currSize == -1) {
		throw new TSRMException("File does not seem to exist.", false);
	    }
	    int total = _consumers.size();
	    for (int i=total-1; i>=0; i--) {
		TSRMRequest r = (TSRMRequest)(_consumers.get(i));
		r.setTrustedSize(currSize);
		if (!pin(r, TStatusCode.SRM_FILE_PINNED)) {
		    result = false;
		    break;
		} else {
		    _consumers.remove(i);
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_transferMutex);
	} 

	return result;
    }
    
    public boolean pin(TSRMRequest r, TStatusCode code) {	 	
	if (r.isAborted()) {
	    return true;
	}
	
	boolean pinned = true;
	if (_pinManager.getPin(r) == null) {
	    TSRMPin p = null;
	    
	    if (r.getSpaceToken() == getToken()) {
		if (getToken().getType().isPermanent()) {
		    p = new TSRMPinUnlimited(r, getToken());
		} else if (r.getProposedPinDuration() > 0) {		    		    
		    p = new TSRMPinLimited(r, getToken());
		} else {
		    p = new TSRMPinUnlimited(r, getToken());
		}
	    } else {
		p = new TSRMPinUnlimited(r, getToken());
	    }
	    //boolean result = _pinManager.addPin(p);
	    pinned = _pinManager.addPin(p);
	    TSRMLog.debug(this.getClass(), null, "event=pin "+stampPath()+" result= "+pinned, r.description());
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=pin "+stampPath()+" result= alreadyPinned", r.description());
	}

	if (pinned) {
	    if (code == TStatusCode.SRM_FILE_PINNED) {
		r.setReturnStatusPinned(getCanonicalPath());	
	    } else {		
		//r.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_IN_CACHE, null));		
		r.setReturnStatus(TSRMUtil.createReturnStatus(code, null));		
	    }
	}
	
	return pinned;
    }
    
    public void unpin(TSRMRequest r) {	 
	TSRMLog.debug(this.getClass(), null, "event=unpin "+stampPath(), r.description());
	_pinManager.unpin(r);
    }
    
    public void release() {
	_pinManager.clearAllPins(true);
    }
    
    public TSRMPinManager getPinManager() {
	return _pinManager;
    }
    
    public TSRMPin getPin(TSRMRequest r) {
	return getPinManager().getPin(r);
    }
    
    public URI getTxfURI(TSRMTxfProtocol protocol) {
	if ((_transferURI != null) && (_transferURI.getScheme().equalsIgnoreCase(protocol.toString()))) {
	    return _transferURI;
	}
	
	_transferURI = getPhysicalLocation().getTransferURI(protocol);
	return _transferURI;		
    }
    
    public TSURLInfo getSiteURL() {
	return getSiteURL(false);
    }

    public TSURLInfo getSiteURL(boolean forceToRegenerate) {
	return _attributes.generateSiteURL(getCanonicalPath(), forceToRegenerate);
    }
	
    public TSRMFileTxfState getTxfState() {
	return _txfState;
    }

    public long compactMe() {	
	long result = 0;
	//if (TSRMUtil.acquireSync(_transferMutex)){
	if (compactIsFeasible()) {
	    result = getCurrentSize();
	    // 03-20-2007 getToken().removeMe(this, true);
	    if (TSRMLog.getCacheLog() != null) {
		if (!TSRMLog.getCacheLog().removeFile(this)) {
		    result = 0;
		}
	    }
	    
	    this.activate(true);
	    
	}
	//TSRMUtil.releaseSync(_transferMutex);
	//}
	return result;
    }

    public long getExpirationTimeInMilliSeconds() {
	long history = _inheritedExpirationTimeInMilliSeconds;

	long recent = getPinManager().getExpirationTimeInMilliSeconds();

	if (history == -1) {
	    return recent;
	}

	if (recent > history) {
	    return recent;
	} 
	
	return history;
    }

    private boolean isExpiredInheritly() {
	if (_inheritedExpirationTimeInMilliSeconds > 0) {
	    return  (System.currentTimeMillis() > _inheritedExpirationTimeInMilliSeconds);
	} else if ((getToken() != null) && (getToken().getType().isPermanent())) {
	    return false;
	} else {
	    return true;
	}
    }

    public boolean updateInheritedLifeTime(long newLifeTimeInMilliSeconds) {
	boolean expired = isExpiredInheritly();
	TSRMLog.debug(this.getClass(), null, "event=updateInheritedLT newLifeTimeInMillis="+newLifeTimeInMilliSeconds+" isInheritlyExpired="+expired, stampPath());
	if (!expired) {
	    _inheritedExpirationTimeInMilliSeconds =  System.currentTimeMillis()+newLifeTimeInMilliSeconds;	   
	    TSRMLog.debug(this.getClass(), null, "event=updateInheritedLT newInheritedLT="+_inheritedExpirationTimeInMilliSeconds, stampPath());
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().fileIsAdded(this, this.getCurrentSize(), this.getToken()); 
		}
	    return true;
	} else {
	    return false;
	}
    }

    public void setInheritedLifeTime(long ilt) {
	TSRMLog.debug(this.getClass(), null, "event=setInheritedLT newInheritedLT="+_inheritedExpirationTimeInMilliSeconds +" now="+System.currentTimeMillis(), stampPath());	
	if (ilt < System.currentTimeMillis()) {
	    TSRMLog.debug(this.getClass(), null, "event=setInheritedLT", "result=failed");
	}
	_inheritedExpirationTimeInMilliSeconds = ilt;	
    }

    //
    // return whether any change happened
    //
    public boolean compactIsFeasible() {	
	if (_txfState.doBlocking()) { 
	    return false;
	}

	if (!_isReady) { 
	    return false;
	}

	if (!isExpiredInheritly()) { 
	    return false;
	}

	if (!getPinManager().tryClose()) { 
	    return false;
	}	
	
	// now it is ready to take actions on deleting/archiving()	 
	return true;
    }
    
    private void deleteContent(boolean deepClean) {
	// remove the physical file
	if (getPhysicalLocation() == null) {
	    return;
	}
	if (!_isReady && getPhysicalLocation().isTransfering()) {
	    return;
	}

	getPhysicalLocation().changeTransferState(false); // not receiving files anymore.	
	
	this._physicalFileLocation.remove(deepClean);
	_actualSize = -1;
	_detail.setSize(null);
	TSRMLog.debug(this.getClass(), null, "event=file-deleted",  stampPath());	    
    }
    
    public void detachFromParent() {
	if (getParent() != null) {
	    getParent().rmFileName(getName());
	}
    }

    public TReturnStatus remove() {	
	TSRMLog.debug(this.getClass(), null, "action=rmMe "+stampPath(),"ready="+isReady());
	if (!isReady()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, "Abort related requests first.");
	} 
	  
	//TSRMLog.getCacheLog().removeFile(this); 

	detachFromParent();
	unsetToken(true);
		
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);	
    }    
   
    public void deleteMe(boolean doDetachFromParent) {
	deleteContent(true);
	if (doDetachFromParent) {
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().removeFile(this);
		}
	    detachFromParent();
	}
	_token = null;

	claimPhysicalFileLocation();
	adjustFileStorageType();
    }
    
    public boolean isValid() {
	if (isPinned()) {
	    return true;
	} else {
	    return !isExpiredInheritly();
	}
    }

    public boolean isPinned() {
	if (getPinManager().hasActivePin()) {
	    return true;
	} 
	return false;
    }
	
    public void changeParentTo(TSRMLocalDir p) {
	_parent = p;
	_detail.setPath(getSiteURL(true).getSURL().toString());       
    }
    
    public boolean isReady() {
	return _isReady;
    }
    
    public String stampPath() {
	return "path="+getCanonicalPath();
    }

    private boolean setTransition() {
	if (_txfState.doBlocking()) {
	    TSRMLog.debug(this.getClass(), null, "event=setBlockFailed. reason=alreadyBlocked", stampPath());
	    return false;
	}	    
	 _txfObj = null; // derefence

	 _txfState = TSRMFileTxfState.TRANSITION;

	 return true;
    }

    public boolean setBlock(TSRMFileTxfState state, boolean doDelete) {
	TSRMLog.debug(this.getClass(), null, "event=setBlock txfState="+_txfState+" newState="+state+" doDelete="+doDelete, stampPath());

	if (state.doBlocking()) {
	    if (_txfState.doBlocking()) {
		TSRMLog.debug(this.getClass(), null, "event=setBlockFailed. reason=alreadyBlocked", stampPath());
		return false;
	    }	    
	    
	    if (isPinned()) {
		TSRMLog.debug(this.getClass(), null, "event=forcedRelease commen=\"a forced release from overwritting option.\"",null);
		release();
	    }

	    if (doDelete) {
		deleteContent(false);
	    }

	    if (_isReady) {
		_isReady = false;
		if (getPinManager() != null) {
		    while (!(getPinManager().tryClose())) {
			TSRMUtil.sleep(2000);
		    }
		}
		activate(false);

		getToken().getRemovalPolicy().removeCandidate(this);
	    }
	}  else {
	    _txfObj = null; // derefence
	}

	_txfState = state;

	return true;
    }

    public long getReservedBytes() {
	return _token.getReservedBytes(this);
    }
  
    public boolean isUserOwned() {
	return (getParent() == null);
    }

    public void receive(TSupportedURL localTgt, TSRMRequestPut req) {
        TUserRequestPut userReq = (TUserRequestPut)(req.getRequester());
	boolean doDelete = (userReq.getOverwriteMode() == TOverwriteMode.ALWAYS);
	if (!setBlock(TSRMFileTxfState.CLIENTPUSH, doDelete)) {
	    throw new TSRMExceptionFileBusy("Already in use."+Thread.currentThread().getName(), false);
	}

	boolean usedToken = (_token != null);
	try {
	    getPhysicalLocation().receive(localTgt, getReservedBytes(), req);

	    if (!TSRMUtil.acquireSync(_transferMutex)){
		return;
	    }
	    try {
		if (usedToken && (_token == null)) {
		    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_ABORTED, "file was removed."));
		}
	    } finally {
		TSRMUtil.releaseSync(_transferMutex);
	    }
	} catch (RuntimeException e) {
	    throw e;
	} finally {
	    setBlock(TSRMFileTxfState.NONE, false);
	}
    }

    public void setTxfObj(ISRMTxfHandler txf) {
	_txfObj = txf;
    }

    public ISRMTxfHandler getTxfObj() {
	return _txfObj;
    }
	
    //
    // called from TSRMRequestCopyLocal
    // keeping srcURL is to recycle the srcURL from TSRMReqestCopyLocal
    //
    public void copyFrom(TSRMLocalFile srcFile, TSupportedURL srcURL, TDeviceAccessInfo accessInfo) {
	if (srcURL == null) {
	    throw new TSRMException("srcURL is null", false);
	}
	setSourceURI(srcFile.getSiteURL(false).getSURL());
	setSFNAppointedByUser();
	if (getToken() == null) {
	    TSRMLog.debug(this.getClass(), null, "event=copy from=\""+srcURL.getURLString()+"\"", stampPath());
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=copy from=\""+srcURL.getURLString()+"\"", stampPath()+" token="+getToken().getID());
	}
	
	if (getToken()!= null) {
	    getToken().findSpaceForFile(this, srcFile.getCurrentSize());
	}
	setPhysicalLocation(TSupportedURL.create(getSiteURL(false)), accessInfo);
	download(srcURL);	
    }

    public void download(TSupportedURL src) {
	if (!setBlock(TSRMFileTxfState.INITPULL, true)) {
	    throw new TSRMExceptionFileBusy("Already in use."+Thread.currentThread().getName(), false);
	}

	try {	   
	    //TSRMLog.debug(this.getClass(), "download()", "from="+src.getURI().toString(), "to:"+getCanonicalPath()+" tid="+Thread.currentThread().getName());
	    setTxfObj(null);
	    getToken().getHostDevice().bringFromSource(src, this);

	    //TSRMLog.debug(this.getClass(), "download()", "txfObj="+_txfObj, "tid="+Thread.currentThread().getName());
	    if (_txfObj != null) {
		_txfObj.action();
	    }
	} catch (RuntimeException e) {
	    throw e;
	} finally {
	    setBlock(TSRMFileTxfState.NONE, false);
	}
    }
    
    public TSRMLocalFile getTxfFile() {
	if (getPhysicalLocation() == null) {
	    return null;
	}
	TSRMLocalFile stagingFile = this.getPhysicalLocation().getStagingFile();
	
	if (stagingFile == null) {		
	    return this;
	} else {
	    return  stagingFile;
	}
    }       
    
    public TSRMLocalFile getStageFile() {
	return getParent().createSubFile(getName()+"-staged", TFileStorageType.VOLATILE, false);
    }
    
   

    public void changeToken(TSRMRequestFileToSpace req) {
	TSRMStorage.iSRMSpaceToken newToken = req.getToken();

	if (newToken == _token) {
	    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "no action. Same token."));
	    return;
	}
	
	if (newToken.getType() != _token.getType()) {
	    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "type mismatch."));
	    return;
	}

	if (!_isReady) {
	    req.setReturnStatusFailed("file is in use. Cannt change token.");
	    return;
	}

	//if (!setBlock(TSRMFileTxfState.TRANSITION, false)) {
	if (!setTransition()) {
	    throw new TSRMException("Already blocked.", true);
	}
	try {
	    // workingFile is not registered in NameSpace since it is meant to be deleted later

	    TSRMLocalFile workingFile = this.getParent().createSubFile(null, newToken.getDefaultFileType(), true);
	    workingFile.setToken(newToken);
	    TDeviceAccessInfo accessInfo = null;
	    if (req.getRequester().getStorageSystemInfo() != null){
		accessInfo = new TDeviceAccessInfo(req.getRequester().getStorageSystemInfo());
	    }
	    workingFile.copyFrom(this, TSupportedURL.create(getSiteURL(false)), accessInfo);
	    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	    
	    // cleaning up
	    _pinManager.switchToken(_token, newToken);
	    _token.removeFile(this, false);
	    claimPhysicalFileLocation();
	    adjustFileStorageType();
	    _token=null;
	    _transferURI=null;	   
	    //\\this.unsetToken(false);

	    this.setToken(newToken);
	    this._physicalFileLocation = workingFile.getPhysicalLocation();
	    this.getParent().rmFileName(workingFile.getName());
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().fileIsAdded(this, getCurrentSize(), newToken);
		}
	   
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), req.description()+"changeToken failed.", e);
	    req.setReturnStatusFailed(e.getMessage());
	} finally {
	    setReady(); 
	    setBlock(TSRMFileTxfState.NONE, false);
	}
    }
public void useCredential(org.ietf.jgss.GSSCredential cred) {}
}

