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

import java.util.HashMap;
import java.util.*;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.*;

//import srm.common.StorageResourceManager.*; 
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.Volatile.*;
import gov.lbl.srm.storage.Permanent.*;
import gov.lbl.srm.policy.TSRMStoragePolicyDefault;
import gov.lbl.srm.server.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.intf.*;
//import gov.lbl.srm.transfer.mss.hpss.*;

import org.apache.axis.types.URI;
//import org.ietf.jgss.GSSCredential;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public abstract class TBasicDevice /*implements Comparable*/ {
    private int _totalSizeInMB;
    
    private long _bytesUnReserved   = -1;

    private TSRMMutex _bytesGuard 	    = new TSRMMutex();
    private TSRMMutex _tokenGuard       = new TSRMMutex();
    private HashMap _tokensIssued   = new HashMap();
    
    private TSRMStorage.iStoragePolicy _storageUsagePolicy = null;
    
    protected static int _gCounter = 0;
    public String _tag;

    public static boolean _mssDeviceCreated = false;
    
    //
    //
    //                   abstract functions
    public abstract TSupportedURL create(TSURLInfo info);
    public abstract String getDescription();
    public abstract String getDescriptionForPublic();
    //
    // returns true if the surl is on this device
    public abstract boolean isDeviceSpecific(TSURLInfo surlInfo);
    
    public abstract TDeviceAccessInfo getAccessInfo(TSRMStorage.iSRMSpaceToken token, TUserRequest requester);
    
    
    //
    // abstract functions for Path handling
    //
    //  
    public abstract TDeviceAccessObj createPath(TSupportedURL fullPathStr, 
						TSRMStorage.iSRMSpaceToken onBehalfOf,
						TDeviceAccessInfo accessInfo);
    //public abstract long getLastModificationTime(TDeviceAccessObj pathRef);
    public abstract TMetaDataPathDetail ls(TDeviceAccessObj pathRef, TSRMFileListingOption lsOption); 

    //public abstract boolean isDir(TDeviceAccessObj pathRef);
    public abstract void setPathType(TDeviceAccessObj pathRef);
    public abstract long getSize(TDeviceAccessObj pathRef);
    public abstract boolean chmod(TDeviceAccessObj pathRef, boolean writable);
    public abstract String removePath(TDeviceAccessObj pathRef, boolean deepClean);
    public abstract URI getTransferURI(TDeviceAccessObj pathRef, TSRMTxfProtocol protocol);
    public abstract void toDisk(TDeviceAccessObj from, TSRMLocalFile tgt);
    public abstract void fromDisk(TSRMLocalFile f, Object tgt);
    //public abstract boolean localCopy(TSURLInfo from, TSURLInfo to);
    public abstract boolean localCopy(TSRMLocalFile from, TSRMLocalFile to);
    
    public abstract boolean verifyDeviceSize(long bytes);
    public abstract String getDeviceDisk();
    
    public abstract void bringFromSource(TSupportedURL src, TSRMLocalFile tgt);
    public abstract TSRMLocalFile getStagingFile(TSupportedURL src, long bytes, TSRMRequest req);
    //public abstract boolean isDisk();
    public abstract TAccessLatency getAccessLatency();
    public abstract TDeviceAccessObj construct(String accessPath, TDeviceAccessInfo accessInfo); 
    
    public Vector getStatusReport(String key) {
	return null;
    }

    public String getUsageReport() {
	String result = "["+getDescriptionForPublic()+"] ";
	
	if (_totalSizeInMB > 0) {
	    long totalBytes = (long)_totalSizeInMB*(long)1048576;
	    result += "TotalBytes:["+totalBytes+"] ";   
	    result += "OccupiedBytes:["+(totalBytes-_bytesUnReserved)+"]";
	    //result +=" Available to reserve: ["+_bytesUnReserved+" bytes].";

	} else {
	    result += "Unlimited";
	}
	return result;
    }
    //
    // defined functions
    // 
    public TBasicDevice(int bytesFreeInMB, TSRMStorage.iStoragePolicy policy) {
	_totalSizeInMB = bytesFreeInMB;
	if (bytesFreeInMB > 0) {
	    _bytesUnReserved = ((long)bytesFreeInMB)*1024*1024;
	}
	
	_storageUsagePolicy = policy;		
    }

    public void deepClean(TDeviceAccessObj pathRef) {
	return;
    }

    public boolean isDisk() {
	return (getAccessLatency() == TAccessLatency.ONLINE);
    }

    public void setPolicy(TSRMStorage.iStoragePolicy p) {
	_storageUsagePolicy = p;
    }
    
    public TSRMStorage.iStoragePolicy getPolicy() {
	return _storageUsagePolicy;
    }
    
    private TSRMStorage.iSRMSpaceToken createToken(TTokenAttributes requestDescription, TSRMSpaceType spaceType) {
	if (spaceType == TSRMSpaceType.Volatile) {
	    if ((requestDescription.getTokenID() != null) &&
		(requestDescription.getTokenID().startsWith("V.0")))
	    {
		return new TVolatilePublicToken(requestDescription, this);
	    } else {
		return new TVolatileSpaceToken(requestDescription, this);
	    }
	} else if (spaceType == TSRMSpaceType.Permanent){
	    return new TPermanentSpaceToken(requestDescription, this);
	} else {
	    throw new gov.lbl.srm.util.TSRMException("Not yet supporting Durable Token.", false);
	}
    }

    public boolean hasUnlimitedQuota() {
	return (_bytesUnReserved < 0);
    }

    /*
    private void decreaseConservation(long bytes) {
	if (hasUnlimitedQuota()) {
	    return;
	}

	TSRMUtil.acquireSync(_bytesGuard);    
	this._bytesUnReserved -= bytes;
	TSRMLog.info(this.getClass(), "Device:"+getDescription(), "Bytes avail down to: "+_bytesUnReserved, null);
	TSRMUtil.releaseSync(_bytesGuard);	
    }
    */

    private void updateConservation(long bytes) {
	if (hasUnlimitedQuota()) {
	    return;
	}

	if (!TSRMUtil.acquireSync(_bytesGuard)) {
	    return;
	}
	try {
	    this._bytesUnReserved += bytes;
	    
	    TSRMLog.debug(this.getClass(), "updateConservation", "Device="+getTag()+" desc="+getDescription(), "UnreservedBytes="+_bytesUnReserved);
	} finally {
	    TSRMUtil.releaseSync(_bytesGuard);
	}
	
    }
    //
    //
    //
    public TReturnStatus fitToken(TAccount owner, TTokenAttributes requestDescription) {	
	TStatusCode code = getPolicy().negociate(requestDescription);

	if ((code == TStatusCode.SRM_LOWER_SPACE_GRANTED) ||
	    (code == TStatusCode.SRM_SUCCESS)) 
        {
	    TSRMStorage.iSRMSpaceToken token = accomodate(requestDescription);
	    
	    if (token == null) {		
		compactDevice();
		token = accomodate(requestDescription);
	    }
	    if (token == null) {
		code = TStatusCode.SRM_NO_FREE_SPACE;				 
	    } else {
		token.setOwner(owner);
		//owner.addToken(token);				
	    }
	    return TSRMUtil.createReturnStatus(code, null);
	} else {
	    return TSRMUtil.createReturnStatus(code, null);
	}			
    }
    //
    //
    //
    public TSRMStorage.iSRMSpaceToken accomodate(TTokenAttributes requestDescription) {
	TAccessLatency desiredLatency = requestDescription.getAccessLatency();
	if (desiredLatency != null) {
	    if (desiredLatency != getAccessLatency()) {
		TSRMLog.debug(this.getClass(), null, "event=accomodate Device="+getTag()+" desc="+getDescription()+" desiredLatency="+desiredLatency, "error=canntTakeThisLatencyValue");
		return null;
	    }
	}

	TSRMStorage.iSRMSpaceToken result = null;
	TSRMSpaceType spaceType = requestDescription.getTypeOfSpace();
	
	long askingBytes = requestDescription.getGuaranteedSpaceBytes();	 
	TSRMLog.debug(this.getClass(), null, "event=accomodate Device="+getTag()+" desc="+getDescription()+" unReserved="+_bytesUnReserved, "asking="+askingBytes);

	if (!TSRMUtil.acquireSync(_bytesGuard)) {
	    return result;
	}
	try {
	    boolean hasEnough = (hasUnlimitedQuota() || (_bytesUnReserved >= askingBytes));
	    
	    if (hasEnough) {
		result = createToken(requestDescription, spaceType);
		if (_bytesUnReserved > 0) {
		    _bytesUnReserved -= askingBytes;
		}
		TSRMLog.debug(this.getClass(), null, "event=accomodate Device="+getTag()+" desc="+getDescription(), "bytesUnreserved="+_bytesUnReserved);
		addToken(result);
		//_tokensIssued.put(token.getID(), token);
	    }  
	} finally {
	    TSRMUtil.releaseSync(_bytesGuard);
	}
	return result;
    }
    
    public int getTotalSizeInMB() {
	return _totalSizeInMB;
    }
    
    public long getSpareBytes() {
	if (hasUnlimitedQuota()) {
	    return -1;
	}

        long result =  _bytesUnReserved;

	Object[] ids = _tokensIssued.keySet().toArray();
	for (int i=0; i<ids.length; i++) {
	    Object key = ids[i];
	    TSRMStorage.iSRMSpaceToken token = (TSRMStorage.iSRMSpaceToken)(_tokensIssued.get(key));

	    result += token.getAvailableBytes();
	}

	return result;
    }

    // 
    // see if proposedSize can be accepted
    // if not, find what's reasonable to accept
    // there is no actual reservation
    //
    public long negociateTotalSpaceUsage(long proposedSize, long was) {
	TSRMLog.debug(this.getClass(), "negociateTotalSpace", "proposed="+proposedSize, "was="+was+" isUnlimited="+hasUnlimitedQuota());
	if (hasUnlimitedQuota()) {
	    return proposedSize;
	}

	if (!TSRMUtil.acquireSync(_bytesGuard)) {
	    throw new TSRMException("Cannt lock mutex.", false);
	}
	try {
	    long granted = _storageUsagePolicy.determineExtraTotalSpaceToGrantForAToken(proposedSize, was, _bytesUnReserved);
	    long result = granted + was;
	    TSRMLog.debug(this.getClass(), "negociateTotalSpace", "result="+result, null);
	    return result;
	} finally {	    
	    TSRMUtil.releaseSync(_bytesGuard);
	}	   	
    }
    
    public long negociateGuaranteedSpaceUsage(long min, long max, long usedBytes, long was) {	
	String methodName="negociateGuaranteedSpace";
	TSRMLog.debug(this.getClass(), methodName, "min="+min+" max="+max, "used="+usedBytes+" was="+was+" isUnlimited="+hasUnlimitedQuota());
	if (hasUnlimitedQuota()) {
	    return min;
	}

	if (!TSRMUtil.acquireSync(_bytesGuard)) {
	    throw new TSRMException("Cannt lock mutex!", false);
	}
     	
	try {
	    long granted = _storageUsagePolicy.determineGuaranteedSpaceToGrantForAToken(min, max, was, _bytesUnReserved);
	    TSRMLog.debug(this.getClass(), methodName, "wouldHaveGranted="+granted, null);
	    if (granted > 0) {
		if (usedBytes > granted) {
		    TSRMUtil.releaseSync(_bytesGuard);
		    return -1; // user should compact the space first
		}
		_bytesUnReserved -= (granted-was);
		TSRMLog.debug(this.getClass(), methodName, "Device="+getDescription(), "BytesAvailableUpdated="+_bytesUnReserved);
	    }
	    if (granted == 0) {
		return 0; // no free space
	    } else {
		return granted;
	    }
	} finally {
	    TSRMUtil.releaseSync(_bytesGuard);
	}       
    }    

    public TSRMStorage.iSRMSpaceToken getToken(String tid) {
	if (!TSRMUtil.acquireSync(_tokenGuard)) {
	    return null;
	}
	try {
	    Object entry = _tokensIssued.get(tid);
	    if (entry != null) {
		return (TSRMStorage.iSRMSpaceToken)entry;
	    }
	} finally {
	    TSRMUtil.releaseSync(_tokenGuard);
	}	
	return null;
    }
    
    public void addToken(TSRMStorage.iSRMSpaceToken token) {
	if (!TSRMUtil.acquireSync(_tokenGuard)) {
	    return;
	}

	try {
	    _tokensIssued.put(token.getID(), token);
	    
	    TSRMLog.debug(this.getClass(), null, "event=Addtoken", "token="+token.getID()+" desc="+ getDescription()+" totalTokens="+_tokensIssued.size());
	} finally {
	    TSRMUtil.releaseSync(_tokenGuard);
	}
    }
    
    public void removeToken(TSRMStorage.iSRMSpaceToken token) {
	if (!TSRMUtil.acquireSync(_tokenGuard)) {
	    return;
	}
	try {
	    removeTokenLocal(token);
	} finally {
	    TSRMUtil.releaseSync(_tokenGuard);
	}
    }
    
    public int getTokenCount() {
	int result = 0;
	
	if (!TSRMUtil.acquireSync(_tokenGuard)) {
	    return result;
	}
	try {
	    result = _tokensIssued.size();
	} finally {
	    TSRMUtil.releaseSync(_tokenGuard);
	}
	
	return result;
    }
        
    
    public void reduced(String tid, long sizebeforeCompact, long sizeAfterCompact) {	
	TSRMLog.debug(this.getClass(), null, "event=reducingToken token="+tid, "before="+sizebeforeCompact+" after="+sizeAfterCompact);

	updateConservation(sizebeforeCompact-sizeAfterCompact);

	/*
	TSRMUtil.acquireSync(_bytesGuard);
	if (!hasUnlimitedQuota()) {
	    this._bytesUnReserved += sizebeforeCompact;
	    this._bytesUnReserved -= sizeAfterCompact;
	}
	
	TSRMLog.debug(this.getClass(), "Device:"+getDescription(), "Bytes avail now: "+_bytesUnReserved, null);
	TSRMUtil.releaseSync(_bytesGuard);
	*/
    }
    
    private void removeTokenLocal(TSRMStorage.iSRMSpaceToken token) {
	TSRMLog.debug(this.getClass(), "removeTokenLocal()", "token="+token.getID(), null);	        
	_tokensIssued.remove(token.getID());	      
	TSRMLog.debug(this.getClass(), null, "event=removeToken", "token="+token.getID()+" device="+getTag()+" desc="+getDescription()+" totalTokenSize="+_tokensIssued.size());
	updateConservation(token.getTokenAttributes().getGuaranteedSpaceBytes());
	       
	token.removeContents();
	token.getOwner().removeToken(token);
    }
    
    public void compactDevice() {
	if (!TSRMUtil.acquireSync(_tokenGuard)) {
	    return;
	}
	try {
	    Object[] ids = _tokensIssued.keySet().toArray();
	    TSRMLog.debug(this.getClass(), "compactDevice", "event=compact","device="+getTag()+" desc="+getDescription() );
	    
	    for (int i=0; i<ids.length; i++) {
		Object key = ids[i];
		TSRMStorage.iSRMSpaceToken token = (TSRMStorage.iSRMSpaceToken)(_tokensIssued.get(key));
		if  (token.isLifeTimeExpired() && token.isIdle()) {
		    removeTokenLocal(token);
		} else {
		    // we don't want to do compact token usage, for, say V.0
		    // then compact a token here w/o compact usage does not make sense
		    // so it is commented out
		    //token.compact(false);
		}	    
	    }
	} finally {
	    TSRMUtil.releaseSync(_tokenGuard);
	}	
    }
    
    public static TBasicDevice createDevice(String deviceSpec, int defaultSizeInMB) {
	String sizeStr = TSRMUtil.getValueOf(deviceSpec, "[", ']', true);
	int size = defaultSizeInMB;
	if (sizeStr != null) {
	    size = Integer.parseInt(sizeStr);
	    if (size < 0) {
		throw new RuntimeException("Detected that assigned cache size is negative. detail:"+deviceSpec);
	    }
	}
	
	char seperatorChar = '&';
	String type = TSRMUtil.getValueOf(deviceSpec, "type=", seperatorChar, true);
	
	String path = TSRMUtil.getValueOf(deviceSpec, "path=", seperatorChar, false);
	
	if (type == null) {
	    return new TDiskDevice(path, size, null);
	} else if (type.equalsIgnoreCase("disk")) {
	    return new TDiskDevice(path, size, null);
	} else {
	    String tgtClassName = type;
	    String host         = TSRMUtil.getValueOf(deviceSpec, "host=", seperatorChar, false);
	    
	    if (type.equalsIgnoreCase("gsiftp")) {
		_mssDeviceCreated = true;
		return new THPSSDeviceGsiftp(host, size, null);
	    }

	    String configFileName = TSRMUtil.getValueOf(deviceSpec, "conf=", seperatorChar, false);
	    SRM_MSSIntf hpssAccessObj = null;
	    Class hpssAccessClass = null;
	    try {		       
	        hpssAccessClass = Class.forName(tgtClassName);	
	    } catch (ClassNotFoundException e) {
		TSRMUtil.startUpInfo("## No such class:"+tgtClassName+" in classpath. Trying to look for in the plugin dir.");
		String where = Config._pluginDir;
		if (where == null) {
		    throw new RuntimeException("No plugin library specified, please specify entry \"pluginLib=\" in ws.rc");
		}
		char sep = (java.io.File.separatorChar);
		where += sep;
		where += TSRMUtil.getValueOf(deviceSpec, "jarFile=", seperatorChar, false);
		TSRMUtil.startUpInfo("## Looking in: "+where);

		//final File jarFile = new File(where);
		final File jarFile = TSRMUtil.initFile(where);
		if (!jarFile.exists()) {
		    throw new RuntimeException("Cannt find jar file:"+where);
		}       
 
		
		URLClassLoader loader = 
		    (URLClassLoader) java.security.AccessController.doPrivileged (
								    new java.security.PrivilegedAction() {
				   public Object run() {
				       URL url = null;
				       try {
					   url = jarFile.toURI().toURL();
				       } catch (MalformedURLException ex) {
					   // do nothing					   
					   TSRMLog.exception(TBasicDevice.class, "details", ex);
					   throw new RuntimeException("Oh no!"+ex);
				       }
				       URL[] ok = new URL[1];
				       ok[0] = url;
				       return new URLClassLoader(ok);
				   }}
								    );	
		try {
		    hpssAccessClass = Class.forName(tgtClassName, true, loader);
		} catch (ClassNotFoundException ex) {
		    throw new RuntimeException("oh oh"+ex);
		}
	    }
	    try {
		Constructor constructor = hpssAccessClass.getConstructor((java.lang.Class [])null);
		hpssAccessObj = (SRM_MSSIntf)constructor.newInstance((java.lang.Object []) null);

		TSRMUtil.startUpInfo("## loaded class: "+tgtClassName);
		hpssAccessObj.init(configFileName);
	    
	    } catch (NoSuchMethodException e) {		
		TSRMLog.exception(TBasicDevice.class, "details", e);
		throw new RuntimeException("Oh!"+e);
	    } catch (InstantiationException e) {
		TSRMLog.exception(TBasicDevice.class, "details", e);		
		throw new RuntimeException("Oh!!"+e);
	    } catch (Exception e) {
		TSRMLog.exception(TBasicDevice.class, "details", e);
		throw new RuntimeException("Oh!!!"+e);
	    }
			   
	    TMSSTransfer txf = new TMSSTransfer(hpssAccessObj); 
	    _mssDeviceCreated = true;
	    return new THPSSDevice(host, path, size, null, txf);	    
	}
    }
    
    public static String getDeviceCounter() {
	_gCounter++;
	return String.valueOf(_gCounter);
    }
    
    public void setTag(String tag) {
	_tag = tag;
	if (TSRMLog.getCacheLog() != null) {
	TSRMLog.getCacheLog().deviceCreated(this);
	}
    }
    
    public String getTag() {
	return _tag;
    }           
    
    public void uploadTo(TSRMLocalFile src, TSRMLocalFile tgt) {
	if (tgt == null) {
	    TSRMLog.debug(this.getClass(), "uploadTo()", "tgt="+null, null);
	    return;
	}
	TSRMLog.debug(this.getClass(), "uploadTo()", "tgt="+tgt, "canonicalPath="+tgt.getCanonicalPath());
	
	TBasicDevice tgtDevice = null;
	if (tgt.getToken() != null) {
	    tgtDevice = tgt.getToken().getHostDevice();
	} else if (tgt.getPhysicalLocation() != null) {
	    tgtDevice = tgt.getPhysicalLocation().getDevice();
	} else {
	    throw new TSRMException("Tgt does not have enough info to be uploaded", false);
	}
	if (tgtDevice == this) {
	    localCopy(src, tgt);
	} else if (tgtDevice.isDisk()) {
	    toDisk(src.getPhysicalLocation().getRef(), tgt);
	} else {
	    throw new TSRMException("Donnt know how to upload to "+tgt.getSiteURL(), false);
	}
	/*
	if ((tgt.getToken()!= null) && (tgt.getToken().getHostDevice() == this)) {
	    localCopy(src, tgt);
	} else if ((tgt.getPhysicalLocation() != null) && (tgt.getPhysicalLocation().getDevice() == this)) {
	    localCopy(src, tgt);
	} else if (tgt.getToken().getHostDevice().isDisk()) {
	    //toDisk(src, (File)(tgt.getPhysicalLocation().getRef().getObj()));
	    toDisk(src.getPhysicalLocation().getRef(), tgt);
	} else {
	    throw new TSRMException("Donnt know how to upload to "+tgt.getSiteURL(), false);
	}
	*/
    }

    /*
    public int compareTo(Object o2) {
	TBasicDevice d2 = (TBasicDevice)o2;

        int tokenCount1 = this.getTokenCount();
	int tokenCount2 = d2.getTokenCount();

	if (tokenCount1 > tokenCount2) {
	    return 1;
	} else if (tokenCount1 < tokenCount2) {
	    return -1;
	} else {
	    return 0;
	}
    }
    */
}
