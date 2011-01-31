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

package gov.lbl.srm.storage;

import java.util.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.util.*;
import gov.lbl.srm.policy.TSRMFileRemovalPolicy;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import gov.lbl.srm.server.*;

import org.apache.axis.types.*;

// actual work and knowledge of each token
public abstract class TSpaceTokenSkeleton implements TSRMStorage.iSRMSpaceToken {
    private TTokenAttributes 	        _description;
    private TBasicDevice 	 	_hostDevice;
    private Date 	 	       	_creationTime;	
    private HashMap  	       	        _contents 		= new HashMap(); // files that resides in this token
    private TAccount 		        _owner 			= null;
    private long 		       	_bytesReserved 	        = 0;
    private Mutex 		       	_byteMutex 		= new Mutex();
    private TSRMFileRemovalPolicy       _removalPolicy          = null; //new TSRMFileRemovalPolicyLRU(this);
    
    public final int		        _RELEASE_OK             = 0;
    public final int		        _RELEASE_FORCEFUL_OK    = 1;
    public final int		        _RELEASE_FORBIDDEN      = 2;

    public boolean                      _isReleased             = false;
    
    private boolean			_dynamicCompactIsOn     = false;
    
    private  TRetentionPolicyInfo       _stubObj = null;

    public abstract TSRMSpaceType getType();
    public abstract TFileStorageType getDefaultFileType();
    public abstract void actionOnReleasedFiles(Set files);
    
    public TSpaceTokenSkeleton(TTokenAttributes desc, TBasicDevice device) {
	_description = desc;
	_creationTime = new Date();
	_hostDevice = device;

	_removalPolicy = TSRMFileRemovalPolicy.getDefaultPolicy();
    }
    public void listContents() {
	TSRMUtil.startUpInfo("==> ......token contents: size="+_contents.size());
	Vector v = new Vector(_contents.keySet());
	for  (int i=0; i<v.size(); i++) {
	    TSRMLocalFile curr = (TSRMLocalFile)(v.get(i));
	    TSRMUtil.startUpInfo("\t"+i+"th: "+curr.getCanonicalPath());
	}
	TSRMUtil.startUpInfo("<==");
    }
    public TBasicDevice getHostDevice() {
	return _hostDevice;
    }
    
    public TMetaDataSpace getMetaData() {
	TMetaDataSpace result = new TMetaDataSpace();
	result.setSpaceToken(_description.getSpaceToken());
	//result.setIsValid(true);
	result.setGuaranteedSize(_description.getGuaranteedSpaceByteObj());
	result.setLifetimeAssigned(_description.getLifeTimeObj());
	result.setLifetimeLeft(_description.getLifeTimeLeftObj(this._creationTime.getTime()));
	result.setOwner(_owner.getUserIDType());
	result.setTotalSize(_description.getTotalSpaceByteObj());
	result.setRetentionPolicyInfo(getRetentionPolicyInfo());
	result.setUnusedSize(_description.getUnusedBytesObj(this._bytesReserved));
	if (isLifeTimeExpired()) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED, "TokenDesc:"+_description.getUserDesc()));
	} else {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "TokenDesc:"+_description.getUserDesc()));
	}
	return result;
    }
        
    public TRetentionPolicyInfo getRetentionPolicyInfo() {
	if (_stubObj == null) {
	    TAccessLatency latency = TAccessLatency.NEARLINE;
	    if (_hostDevice.isDisk()) {
		latency = TAccessLatency.ONLINE;
	    }
	    _stubObj = new TRetentionPolicyInfo();
	    _stubObj.setAccessLatency(latency);
	    _stubObj.setRetentionPolicy(_description.getTypeOfSpace().getRetentionPolicy());
	}
	return _stubObj;
    }

    public TTokenAttributes getTokenAttributes() {
	return _description;
    }
    
    private void setID(String id) {
	getTokenAttributes().setTokenID(id);	    
    }
    
    public void setID(String prefix, String suggestedID) {
	// this is need to make correct accounting of tokens issued.
	String defaultID =  TSRMUtil.getTokenID(); 
	
	if ((suggestedID != null) && (suggestedID.length() > 0)) {
	    setID(suggestedID); // needed to read back cache log
	} else {
	    setID(prefix+defaultID);
	}
    }
    
    public String getID() {
	return _description.getTokenID();
    }
    
    public void setOwner(TAccount owner) {
	_owner = owner;
	owner.addToken(this);
    }
    
    public TAccount getOwner() {
	return _owner;
    }
    
    public boolean isIdle() {
	int state = getSpaceReleaseState();
	if (state == _RELEASE_FORBIDDEN) {
	    TSRMLog.debug(this.getClass(), null, "event=_RELEASE_FORBIDDEN_", "token="+getID());
	    return false;
	} else if (state == _RELEASE_FORCEFUL_OK) {
	    TSRMLog.debug(this.getClass(), null, "event=_RELEASE_FORCEFUL_OK", "token="+getID());
	    return false;
	}
	return true;
    }
    
    public boolean isLifeTimeExpired() {
	long expirationTime = getExpirationTimeInMilliSeconds();

	if (expirationTime < 0) {
	    return false;
	}
	Date now = new Date();
	return now.after(new Date(expirationTime));
    }
    
    public long getMaxFileLifetimeMillis() {
	return getExpirationTimeInMilliSeconds();
    }
	    
    public long getExpirationTimeInMilliSeconds() {
	if (_description.getLifeTimeInSeconds() < 0) {
	    return -1;
	}
	return _creationTime.getTime() +_description.getLifeTimeInSeconds()*1000;
    }
    
    public long getCreationTimeInMilliSeconds() {
	return _creationTime.getTime();
    }
    
    public void setCreationTimeInMilliSeconds(long ct) {
	_creationTime.setTime(ct);
    }
    
    public long negociateLifeTime (long proposedLifetime) {
	long systemAllowedMax = _hostDevice.getPolicy().getMaxSpaceLifeTime();
	if (proposedLifetime < systemAllowedMax) {
	    return proposedLifetime;
	}
	return systemAllowedMax;
    }
    
    private long getElapsedTimeInSeconds() {
	Date now = new Date();
	return (now.getTime() - _creationTime.getTime())/(long)1000;
    }
        
    //
    // note that SRM_SUCCESS does not imply success of accepting the life time 
    //
    public TReturnStatus update(SrmUpdateSpaceRequest req) {
	//
	// check lifetime arrangement
	//
	TSRMLog.debug(this.getClass(), null, "event=updating token="+getID(), "currLT(sec)="+_description.getLifeTimeInSeconds()+" createdAt="+_creationTime.toString());
	if (isLifeTimeExpired()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_LIFETIME_EXPIRED, null);
	}

	if (isReleased()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "token was released");
	}

	long lifetime = _description.getLifeTimeInSeconds();

	if (req.getNewLifeTime() != null) {
	    long elapsedTimeInSeconds = getElapsedTimeInSeconds();
	    long proposedLifeTime = req.getNewLifeTime().intValue() + elapsedTimeInSeconds;
	    
	    lifetime = negociateLifeTime(proposedLifeTime);
	}	    

	long totalSpace = _description.getTotalSpaceBytes();
	if (req.getNewSizeOfTotalSpaceDesired() != null) {
	    long proposedTotalSpaceSize = req.getNewSizeOfTotalSpaceDesired().longValue();
	    totalSpace = _hostDevice.negociateTotalSpaceUsage(proposedTotalSpaceSize, _description.getTotalSpaceBytes());
	}

	long guaranteedSpace = 	_description.getGuaranteedSpaceBytes();
	if (req.getNewSizeOfGuaranteedSpaceDesired() != null) {
	    long proposedGuaranteedSpaceSize = req.getNewSizeOfGuaranteedSpaceDesired().longValue();
	    guaranteedSpace =_hostDevice.negociateGuaranteedSpaceUsage(proposedGuaranteedSpaceSize, 
									totalSpace,
									getUsedBytes(),
								       _description.getGuaranteedSpaceBytes());
	} else if (guaranteedSpace > totalSpace) {
	    guaranteedSpace =_hostDevice.negociateGuaranteedSpaceUsage(guaranteedSpace, 
									totalSpace,
									getUsedBytes(),
								       _description.getGuaranteedSpaceBytes());
	}

	if (guaranteedSpace > 0) {
	    TSRMLog.debug(this.getClass(), null, "event=updateResult token="+getID()+" G="+guaranteedSpace+" T="+totalSpace," LTseconds="+lifetime);
	    _description.setGuaranteedSpaceBytes(guaranteedSpace);
	    _description.setLifeTimeInSeconds(lifetime);
	    _description.setTotalSpaceBytes(totalSpace);
	    if (totalSpace == guaranteedSpace) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	    } else {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_LOWER_SPACE_GRANTED, null);
	    }
	} else if (guaranteedSpace == 0){
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NO_FREE_SPACE, null);
	} else {
	    String errMsg = "It is probrabably because the space is occupied. Try to call release() or compact() first.";
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, errMsg);
	}	
    } 
    
    public boolean release(boolean forceFileRelease) {
	TSRMLog.debug(this.getClass(), "releaseToken", "forcefilerelease= "+forceFileRelease, "token="+getID());
	boolean result = true;
	
	if (isReleased()) {
	    return result;
	}
		
	int state = getSpaceReleaseState();
	
	if (state == _RELEASE_FORBIDDEN) {
	    result = false;
	} else if (state == _RELEASE_FORCEFUL_OK) {
	    if (!forceFileRelease) {
		result = false;
	    }
	} 
	
	if (!result) {	    
	    return false;
	}
	
	try {
	    doRelease();				
	    _hostDevice.removeToken(this);	    
	    //_owner.removeToken(this);
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "details:", e);
	    return false;
	}
		
	return result;
    }
    
    public void removeContents() {
	TSRMLog.debug(this.getClass(), "removeContents", "token="+getID(), "numFiles="+_contents.size());

	if (_contents.size() == 0) {
	    return;
	}
	
	Vector v = new Vector(_contents.keySet());
	for  (int i=0; i<v.size(); i++) {
	    TSRMLocalFile curr = (TSRMLocalFile)(v.get(i));
	    curr.deleteMe(true);
	    TSRMUtil.wipe(curr);
	}
    }

    public boolean isReleased() {
	return _isReleased;
    }

    private void doRelease() {
	TSRMLog.debug(this.getClass(), "doRelease", "token="+getID(), "numFiles="+_contents.size());
	_isReleased = true;

	Set files = _contents.keySet();
	Iterator iter = files.iterator();
	
	while (iter.hasNext()) {
	    TSRMLocalFile curr = (TSRMLocalFile)(iter.next());
	    curr.release();
	}
	
	actionOnReleasedFiles(files);
    }	
    
    private int getSpaceReleaseState() {
        if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return this._RELEASE_FORBIDDEN;
	}

        try {
	    Set files = _contents.keySet();
	    Iterator iter = files.iterator();
	    
	    boolean hasPinnedFiles = false;
	    while (iter.hasNext()) {
		TSRMLocalFile curr = (TSRMLocalFile)(iter.next());
		
		if ((curr.getPhysicalLocation() != null) && 
		    (curr.getPhysicalLocation().isTransfering()))
		    {
			return this._RELEASE_FORBIDDEN;
		    }
		
		if (curr.isPinned()) {
		    hasPinnedFiles = true;
		}
	    }
	    
	    if (hasPinnedFiles) {
		return this._RELEASE_FORCEFUL_OK;
	    }
	    return this._RELEASE_OK;
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);	
	}
    }
     
    private void updateSize(long sizeCompacted, boolean doCompactTokenUsage) {
	if (sizeCompacted == 0) {
	    return;
	}
	
	// update size
	if (doCompactTokenUsage) {
	    long sizeBeforeCompact = this._description.getGuaranteedSpaceBytes();
	    long sizeAfterCompact = sizeBeforeCompact-sizeCompacted;
	    this._description.setGuaranteedSpaceBytes(sizeAfterCompact);
	    this._hostDevice.reduced(this._description.getTokenID(), sizeBeforeCompact, sizeAfterCompact);
	}	
    }
    
    public void removeFile(TSRMLocalFile f, boolean doDetachFromParent) {
	//03-20-2007  TSRMUtil.acquireSync(_byteMutex);

	long fileSize = makeSpace(f, doDetachFromParent);
	
	updateSize(fileSize, false);	
	if (fileSize > 0) {
	    getRemovalPolicy().fileRemoved(f, fileSize);
	}
	//03-20-2007 TSRMUtil.releaseSync(_byteMutex);
    }

    // mutex is needed as this is not called from space compacting functions. only from request cleanup.
    // e.g. putToRemote fileDone()
    public void removeFileIndependant(TSRMLocalFile f, boolean doDetachFromParent) {
	TSRMLog.debug(this.getClass(), null, "event=removingFile", "file="+f.getCanonicalPath()+" token="+getID());
	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return;
	}
	try {
	    long fileSize = makeSpace(f, doDetachFromParent);
	    
	    updateSize(fileSize, false);	
	    if (fileSize > 0) {
		getRemovalPolicy().fileRemoved(f, fileSize);
	    }
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}
    }
    
    public void compact(boolean doCompactTokenUsage) {
	TSRMLog.debug(this.getClass(), null, "event=compact("+doCompactTokenUsage+")", "token="+getID());
	/*TSRMUtil.acquireSync(_byteMutex);
	long compactedSize = makeSpace();
	
	updateSize(compactedSize, doCompactTokenUsage);
	
	TSRMUtil.releaseSync(_byteMutex);
	*/ 
	makeSpace();
    }
    
   
    public TSRMFileRemovalPolicy getRemovalPolicy() {
	return _removalPolicy;
    }

    public void setRemovalPolicy(TSRMFileRemovalPolicy p) {
	if (p != null) {
	    _removalPolicy = p;
	}
    }
   
    private long makeSpace() {
	// mutex is locked from caller
	return getRemovalPolicy().cleanAll();
    }

    public long removeMe(TSRMLocalFile curr, boolean doDetachFromParent) {
	return makeSpace(curr, doDetachFromParent);
    }

    private long makeSpace(TSRMLocalFile curr, boolean doDetachFromParent) {
	// mutex is locked from caller
	long fileSize = curr.getCurrentSize();
	curr.deleteMe(doDetachFromParent);
	this.removeFileFromCollection(curr);
	if (fileSize > 0) {
	    return fileSize;
	} else {
	    return 0;
	}
    }

    private void removeFileFromCollection(TSRMLocalFile f) {
	// assumes byteMutex is locked
	if (f == null) {
	    return;
	}

	Long sizeObj = (Long)(_contents.get(f));
	if (sizeObj != null) { 
	    long bytes = sizeObj.longValue();
		    
	    _bytesReserved -= bytes;
	    _contents.remove(f);

	    TSRMLog.debug(this.getClass(), null, "event=removeFile token="+getID()+" filename="+f.getCanonicalPath(), 
			 "fileBytesRemoved="+String.valueOf(bytes)+" totalBytesReserved="+String.valueOf(_bytesReserved));
	}
    }

    private void makingLimitedSpace(long targetedSize) {
	TSRMLog.debug(this.getClass(), "MakingLimitedSpace", "targetedSize="+targetedSize, "token="+getID());
	getRemovalPolicy().cleanLimitedTo(targetedSize);
    }
    
    public long getUsedBytes() {
	TSRMUtil.acquireSync(_byteMutex);
	long result =_bytesReserved;
	TSRMUtil.releaseSync(_byteMutex);
	
	return result;
    }
    
    // no mutex is locked here.
    // expecting caller to lock mutex
    public long getAvailableBytes() {
	long avail = this._description.getGuaranteedSpaceBytes() - _bytesReserved;
	TSRMLog.debug(this.getClass(), null, "token="+getID(), "bytesAvailble="+avail);
	return avail;
    }

    public void assertAllocable(long size) {
	long avail = this._description.getGuaranteedSpaceBytes();
	if (avail < size) {
	    throw new TSRMExceptionNoSpace("Exceeded token:"+getID(), size, avail, false);
	}
    }

    // caller already had the lock
    public long getReservedBytes(TSRMLocalFile f) {
	//TSRMUtil.acquireSync(_byteMutex);
	Long sizeObj = (Long)(_contents.get(f));
	//TSRMUtil.releaseSync(_byteMutex);
	
	if (sizeObj == null) {
	    return -1;
	}
	return sizeObj.longValue();
    }
    
    public boolean fileIsSettled(TSRMLocalFile f) {	
	boolean succ = true;

	if (f == null) {
	    throw new TSRMException("Unexpected null file", false);	    
	}
	
	long actualFileSize = f.getCurrentSize();

	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return false;
	}

	try {
	    Long sizeObj = (Long)(_contents.get(f));       
	    
	    if (sizeObj.longValue() != actualFileSize) {
		TSRMLog.debug(this.getClass(), "fileIsSettled", "path="+f.getCanonicalPath(), "reservedBytes="+sizeObj.longValue()+" actual="+actualFileSize);
		this._bytesReserved -= sizeObj.longValue();
		//_bytesReserved += actualFileSize;
		//_contents.put(f, new Long(actualFileSize));
		
		if (grabSpace(actualFileSize)) {
		    addFile(f, actualFileSize);	    
		} else {
		    succ = false;
		}
	    }
	    
	    if (succ) {
		getRemovalPolicy().addCandidate(f);
		//TSRMLog.info(this.getClass(), null, "event=fileIsSettled path="+f.getCanonicalPath(), "size="+actualFileSize);
	    }
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}

	if (succ) {
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().fileIsAdded(f, actualFileSize, this);
		}
	}
	return succ;
    }
       
    
    // we want to make sure:
    // - user can put in the same src file in two different tokens, and they will have 2 different site surl!
    // - user can put in the same token 2 different files (however slightly the difference might be)
    // this function will create the following in ~/ if a src is like: protocol://host/path/name
    //  host/path/tokenID/name, which will be part of site surl
    //
    private TSRMLocalFile mirrorDirectoryStructure(TSupportedURL surl) {
	TSRMLocalDir topDir = _owner.getTopDir();
	
	TSRMLocalDir workingDir = null;

	workingDir = topDir.createDir(surl.getHostNPort(), false);
	
	String[] potentialSubPaths = TSupportedURL.getPathInArray(surl);
	
	for (int i=0; i<potentialSubPaths.length-1; i++) {
	    String curr = potentialSubPaths[i];
	    if (curr.trim().length()==0) {
		continue;
	    }
	    workingDir = workingDir.createDir(curr, false);
	}
	
	return workingDir.createDir(getID(), false).createSubFile(potentialSubPaths[potentialSubPaths.length-1], 
								  getDefaultFileType(), false);
    }
    
    // _byteMutext is locked
    private void addFile(TSRMLocalFile f, long size) {
	f.setToken(this);
	_bytesReserved += size;
	_contents.put(f, new Long(size));
	TSRMLog.debug(this.getClass(), null, "event=addFile token="+getID()+" file="+f.getCanonicalPath()+" size="+String.valueOf(size), "totalBytesReserved="+String.valueOf(_bytesReserved));
	if (TSRMLog.getCacheLog() != null) {
	TSRMLog.getCacheLog().fileIsAdded(f, size, this);
	}
    }

    public void updateToken(TSRMLocalFile f, long extraBytes) {

	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return;
	}
	try {
	    boolean hasSpace = grabSpace(extraBytes);
	    
	    if (hasSpace) {
		_bytesReserved += extraBytes;
		long newSize = ((Long)_contents.get(f)).longValue()+ extraBytes;
		_contents.put(f, new Long(newSize));
		TSRMLog.debug(this.getClass(), "updateToken", "token="+getID()+" file="+f.getCanonicalPath(), "newSize="+String.valueOf(newSize)+" totalBytesReserved="+String.valueOf(_bytesReserved));
		if (TSRMLog.getCacheLog() != null) {
		TSRMLog.getCacheLog().fileIsAdded(f, newSize, this);
		}
	    }

	    if (!hasSpace) {
		throw new TSRMException("No space.", false);
	    }
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}

    }

    public void findSpaceForFile(TSRMLocalFile f, long size) {
        assertAllocable(size);

	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return;
	}
	
	boolean hasSpace = true;
	try {
	    
	    if (_contents.get(f) == null) {	    
		if (size == -1) {
		    size = _hostDevice.getPolicy().getDefaultFileSize();
		}
		
		hasSpace = grabSpace(size);
		
		if (hasSpace) {
		    /*
		      _bytesReserved += size;
		      f.setToken(this);		 				    
		      _contents.put(f, new Long(size));
		    */
		    addFile(f, size);
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}
	
	if (!hasSpace) {
	    TSRMUtil.sleep(2000);
	    throw new TSRMExceptionNoSpace(getID(), size, getAvailableBytes(), true);
	} 		
    }
    
    // mutex already locked
    private boolean grabSpace(long size) {
	boolean hasSpace = (getAvailableBytes() >= size);
	
	if (!hasSpace) {
	    //this.makingLimitedSpace(size);
	    this.makingLimitedSpace(size - getAvailableBytes());
	    hasSpace = (getAvailableBytes() >= size);
	}
	
	return hasSpace;
    }
    
    private TSRMLocalFile grabFile(TSupportedURL src) {
	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return null;
	}

	try {
	    Set files = _contents.keySet();
	    Iterator iter = files.iterator();
	    
	    while (iter.hasNext()) {
		TSRMLocalFile curr = (TSRMLocalFile)(iter.next());
		
		//if (src.hasSameURI(curr.getSourceURI())) {
		if (src.isCorrespondingTo(curr)) {
		    // already exists in this token
		    //TSRMUtil.releaseSync(_byteMutex);
		    return curr;
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}
	
	return null;
    }
    
    public long getUsableSize(TSupportedURL src) {
	long size = src.getTrustedSize(0);

	if (size > _description.getTotalSpaceBytes()) {
	    throw new TSRMException("Token is too small for this file", false);
	}     
	
	if (size == -1) {
	    size =  _hostDevice.getPolicy().getDefaultFileSize();
	    if (size > _description.getTotalSpaceBytes()) {
		size = _description.getTotalSpaceBytes();
	    }
	    TSRMLog.debug(this.getClass(), null, "assignsUsableBytes="+size, src.getURLString());
	}				
	return size;
    }
    
    public TSRMLocalFile createTURL(TSRMLocalFile f, TUserRequest req) {
	if (isLifeTimeExpired()) {
	    throw new TSRMException("Space lifetime is expired, Cannt use space anymore.", false);
	}
	
	//TDeviceAccessInfo accessInfo = getHostDevice().getAccessInfo(this, req);
	
	long size = f.getCurrentSize();
	if (size <= 0) {
	    throw new TSRMException("the file is empty or not existing.. found size="+size, false);
	}

        assertAllocable(size);

	TSRMLocalFile result = null;
	
	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    return null;
	}
		
	try {
	boolean hasSpace = grabSpace(size);
	if (!hasSpace) {
	    TSRMUtil.releaseSync(_byteMutex);
	    noSpaceError(size);
	}
	
	    TSRMLog.debug(this.getClass(), "createTURL()", "file="+f.getCanonicalPath()+" bytes="+size, null);
	    
	    TSRMLocalDir topDir = _owner.getTopDir();
	    result = topDir.createSubFile(null, getDefaultFileType(), false);
	    result.setSourceURI(f.getSourceURI());
	} catch (Exception e) {
	    throw new TSRMException(e.getMessage(), false);
	} finally {
	    TSRMUtil.releaseSync(_byteMutex);
	}

	return result;
    }

    //
    // using endfilename for now and file will be put in the for simplicity,
    //
    // can apply other schemes later if desired
    //
    public TSRMLocalFile createFile(TSupportedURL src, 
				    long specifiedSize, 
				    TDeviceAccessInfo accessInfo)
    {
	if (isLifeTimeExpired()) {
	    throw new TSRMException("Space lifetime is expired, Cannt use space anymore.", false);
	}
	
	//if (!src.checkExistence()) {
	    // throw new TSRMException("Source file:"+src.getURLString()+" does not exist in this SRM", false);
	//}
	
	long size = specifiedSize;
	if (specifiedSize <= 0) {
	    size = getUsableSize(src);
	}

        assertAllocable(size);       
	
	TSRMLocalFile localFile = grabFile(src);
	if (localFile != null) {
	    TSRMLog.debug(this.getClass(), "createFile", "name="+src.getURLString(), "event=grabExisting file="+localFile.getCanonicalPath());
	    return localFile;
	}

	if (!TSRMUtil.acquireSync(_byteMutex)) {
        throw new TSRMException("Interrnal error. Cannt lock mutex.", false);
    }

	try {
	    boolean hasSpace = grabSpace(size);

	    if (!hasSpace) {
	        noSpaceError(size);
	    } 
	/*
	if (!TSRMUtil.acquireSync(_byteMutex)) {
	    throw new TSRMException("Interrnal error. Cannt lock mutex.", false);
	}
	*/
	TSRMLog.debug(this.getClass(), null, "event=createFile name="+src.getURLString()+" bytes="+size, null);
	
	    localFile = src.getLocalFile();
	    // checking localFile is to make sure if src is mss://, we will get a localFile (LocalFileUserOwned())
	    // but this is not what we want for this function.
	    if ((localFile == null) || (localFile.isUserOwned())) { 
		localFile = mirrorDirectoryStructure(src);
	    } else {//if (isForceful) {
		if (!localFile.getSourceURI().toString().equalsIgnoreCase(src.getURI().toString())) {
		    TSRMLog.debug(this.getClass(), null, "event=redirectingLocalFile",  null);
		    localFile = localFile.getParent().createDir(getID(), false).createSubFile(localFile.getName(), 
											      getDefaultFileType(), false);
		} else if ((localFile.getToken() != null) && (localFile.getToken() != this)) {
		    TSRMLog.debug(this.getClass(), null, "event=wasInDifferentToken token="+localFile.getToken().getID(), " file="+localFile);
		    /* //08-15-2006, donnot think creating another file structure is a good idea...
		    //when user does a ls(), the file wont show the updated msg
		    localFile = mirrorDirectoryStructure(src);
		    */
		    localFile.switchToken();			
		}
	    }
	    
	    localFile.setSourceURI(src.getURI());		
	    addFile(localFile, size);
	    
	    localFile.setPhysicalLocation(src, accessInfo);		
	} catch (TSRMException e) {
		if ((localFile != null) && (localFile.getToken() != null)) {
			removeFile(localFile, true);
		}
	    throw e;	    
	} catch (RuntimeException e) {
		if ((localFile != null) && (localFile.getToken() != null)) {
            removeFile(localFile, true);
        }
	    throw e;
	} finally {    
	    TSRMUtil.releaseSync(_byteMutex);
	}
		
	return localFile;
    }

    public void noSpaceError(long size) {
	TSRMUtil.sleep(2000);
	throw new TSRMExceptionNoSpace(getID()+"total="+_description.getGuaranteedSpaceBytes()+" reserved="+_bytesReserved,
				       size, getAvailableBytes(), true);
    }
    
    public void setDoDynamicCompact(boolean flag) {
	_dynamicCompactIsOn = flag;
    }

    public boolean isDynamicCompactOn() {
	return _dynamicCompactIsOn;
    }
    
    public void fileIsReleased(TSRMLocalFile r) {
	/*
	 * since this function is called with keepSpace=false, compact(r) should be called
	 * regardless of _dynamicCompactIsOn
	 */
	/*
	  if (!this._dynamicCompactIsOn) {
	  return;
	  }
	*/
	// compact this file;
	//compact(r);
	getRemovalPolicy().setCurrentlyBusy(false);
    }

    private SrmExtendFileLifeTimeInSpaceResponse extendAll(Integer newRelativeLifetime) {
	 TSURLLifetimeReturnStatus[] fileStatusList = new TSURLLifetimeReturnStatus[_contents.size()];
	 TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	 SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
		
	 if (!TSRMUtil.acquireSync(_byteMutex)) {
	     result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "cannt lock mutex."));
	     return result;
	 }

	 try {
	     Set files = _contents.keySet();
	     Iterator iter = files.iterator();
	     
	     int counter = 0;
	     while (iter.hasNext()) {
		 TSRMLocalFile curr = (TSRMLocalFile)(iter.next());
		 TSURLLifetimeReturnStatus status = 
		     TSRMNameSpace.pinSURL(curr.getSourceURI(), newRelativeLifetime, getOwner());
		 fileStatusList[counter] = status;
		 reqSummary.add(status.getStatus().getStatusCode());
		 counter ++;
	     }
	 } finally {
	     TSRMUtil.releaseSync(_byteMutex);
	 }

	 result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	 result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));	
	 
	 return result;
     }

     public SrmExtendFileLifeTimeInSpaceResponse extend(URI[] surlArray, Integer newRelativeLifetime) {
	 if (surlArray == null) {
	     TSRMLog.debug(this.getClass(), null, "token="+getID(), "event=extendAll");
	     return extendAll(newRelativeLifetime);
	 }

	 TSRMLog.debug(this.getClass(), null, "token="+getID(), "event=extendFileLT numInputs="+surlArray.length);
	 SrmExtendFileLifeTimeInSpaceResponse result = new SrmExtendFileLifeTimeInSpaceResponse();
	 TSURLLifetimeReturnStatus[] fileStatusList = new TSURLLifetimeReturnStatus[surlArray.length];
	 TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	
	 if (!TSRMUtil.acquireSync(_byteMutex)) {
	     result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "cannt lock mutex!"));
	     return result;
	 }
	 try {
	     for (int i=0; i<surlArray.length; i++) {
		 URI curr = surlArray[i];		 
		 TSupportedURL revised = TSupportedURL.createAndCheckSiteFileName(new TSURLInfo(curr, null),getOwner(),TSRMPermission.Readable);

		 ISRMLocalPath localFile = revised.getLocalPath();
		 if (localFile == null) {
		     fileStatusList[i] = TSRMUtil.createTSURLLifetimeReturnStatus(revised.getURI(), TStatusCode.SRM_INVALID_PATH, "no such surl.");
		 } else {
		     //TSURLLifetimeReturnStatus status = TSRMNameSpace.pinSURL(curr, newRelativeLifetime, getOwner());
		     if (_contents.get(localFile) == null) {
			 fileStatusList[i] = TSRMUtil.createTSURLLifetimeReturnStatus(revised.getURI(), TStatusCode.SRM_INVALID_PATH, "file does not reside in this token.");
		     } else {
			 TSURLLifetimeReturnStatus status = TSRMNameSpace.pinSURL(revised.getURI(), newRelativeLifetime, getOwner());
			 fileStatusList[i] = status;
		     }
		 }
		 reqSummary.add(fileStatusList[i].getStatus().getStatusCode());		 
	     }
	 } finally {
	     TSRMUtil.releaseSync(_byteMutex);
	 }

	 result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	 result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));	
	 
	 return result;
     }
}
