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

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;

import gov.lbl.srm.policy.*;

import gov.lbl.srm.storage.Volatile.*;
import gov.lbl.srm.storage.Durable.*;
import gov.lbl.srm.storage.Permanent.*;
 
import java.util.*;
import org.apache.axis.types.*;

public class TSRMStorage {
    public static HashMap _colSpaceManagers = new HashMap();
    
    public static final String _DefVolatileSpaceLocations  = "ReplicaQualityStorageMB";	 
    public static final String _DefDurableSpaceLocations   = "OutputQualityStorageMB";
    public static final String _DefPermanentSpaceLocations = "CustodialQualityStorageMB";

    public static final String _DefVolatileSpaceUserQuotaMB  = "ReplicaQualityStorageUserQuotaMB";	 
    public static final String _DefLocationSeperator	   = ";";
    
    public final static TSRMSpaceType _DefDefaultUserSpaceType 	= TSRMSpaceType.Volatile;
    public final static TFileStorageType _DefDefaultUserFileStorageType    = TFileStorageType.VOLATILE;
    
    // if a user does not specify space size, use this value
    //public static int _DefDefaultUserSpaceSizeInMB 		= 500; // MB, or 524288000 bytes;
    // if a user does not specify the lifetime in reserve request, use this value
    public final static long _DefDefaultUserSpaceLifeTime 	= 100000;
    // if a user does not specify space size, use this value
    public final static int _DefDefaultDeviceSizeInMB 		= 1000; // MB,     
    
    public interface iStoragePolicy {
	public TStatusCode negociate(TTokenAttributes tokenReq);
	public long getMaxSpaceLifeTime();
	public long getDefaultFileSize();
	public long getDefaultUserSpaceSizeInMB();
	public long getPublicTokenSizeInMB();
	public long determineExtraTotalSpaceToGrantForAToken(long proposed, long was, long _bytesUnReserved);
	public long determineGuaranteedSpaceToGrantForAToken(long min, long max, long was, long currFreeSpace);
	
    }
    // 
    // handles file system operations.  
    //
    public interface iStorageSystemAccess {
		public void ls(int recursiveLevel);
	public void makeDir(String path);    
	public void rmdir(String path);
	public void rmFile(String filename);
	public void cp();
    }
    
    public interface iSRMSpace {
	public TReturnStatus createToken(TAccount owner, TTokenAttributes requestDescription);
	//public TStatusCode updateToken(TTokenAttributes requestDescription);
	//public void compactToken(TSpaceToken tid);
	public iSRMSpaceToken getToken(String tid);
	public iSRMSpaceToken locateDefaultToken(TFileStorageType fileType);
	public TBasicDevice getRefDevice(TSURLInfo surlInfo);
	public TBasicDevice getDevice(String desc);
	public Vector getDeviceList();
	public long getDefaultUserSpaceSizeInMB();
    }
    
    public interface iSRMSpaceToken {    
	public TSRMSpaceType getType();
	
	public String getID();
	public void setOwner(TAccount owner);
	public TAccount getOwner();
	
	public boolean release(boolean forceFileRelease);
	public boolean isReleased();
	public void removeContents();
	public TReturnStatus update(SrmUpdateSpaceRequest req);
	public void compact(boolean doCompactUsage);
	public long removeMe(TSRMLocalFile f, boolean doDetachFromParent);
	public TTokenAttributes getTokenAttributes();
	public TSRMLocalFile createTURL(TSRMLocalFile orig, TUserRequest req);
	public TSRMLocalFile createFile(TSupportedURL src, 
					long specifiedByteObj,
					TDeviceAccessInfo accessInfo);
	public void updateToken(TSRMLocalFile f, long extraBytes);
	
	public void findSpaceForFile(TSRMLocalFile f, long size);
	public long getUsableSize(TSupportedURL url);
	public long getReservedBytes(TSRMLocalFile f);
	public long getAvailableBytes();

	public void removeFile(TSRMLocalFile f, boolean doDetachFromParent);
	public void removeFileIndependant(TSRMLocalFile f, boolean doDetachFromParent);
	public boolean fileIsSettled(TSRMLocalFile f);
	public void setDoDynamicCompact(boolean flag);
	public boolean isDynamicCompactOn();
	public void fileIsReleased(TSRMLocalFile req);
	public TMetaDataSpace getMetaData();
	public TSRMFileRemovalPolicy getRemovalPolicy();
	
	public TBasicDevice getHostDevice();
	public boolean isLifeTimeExpired();
	public long getExpirationTimeInMilliSeconds();
	public long getMaxFileLifetimeMillis();
	public long getCreationTimeInMilliSeconds();
	public void setCreationTimeInMilliSeconds(long ct);
	public boolean isIdle();

	public SrmExtendFileLifeTimeInSpaceResponse extend(URI[] surlArray, Integer newRelativeLifetime);
	public TFileStorageType getDefaultFileType();
    }
	
    public static TSRMLocalFile stage(TSupportedURL src, long bytes, 
				      TSRMStorage.iSRMSpaceToken stagingToken) 
    {
	if (src.isProtocolFILE()) {
	    return ((TSupportedURLWithFILE)src).getStageFile();
	} 
	
	if (stagingToken == null) {
	    TSRMStorage.iSRMSpace volatileSpaceManager = TSRMStorage.getSpaceManager(TSRMSpaceType.Volatile);
	    stagingToken = volatileSpaceManager.locateDefaultToken(TFileStorageType.VOLATILE);
	    
	    if (stagingToken == null) {
		throw new TSRMException("No default volatile token to do staging!!", false);
	    }
	}	
	
	TSRMLocalFile f = src.getLocalFile();	
	
	if (f == null) {
	    return stagingToken.createFile(src, bytes, null); // volatile space doesnt need access info
	}
	
	TSRMLocalFile stagedFile = f.getStageFile();
	
	TSupportedURL siteURL = TSupportedURL.create(stagedFile.getSiteURL());
	stagedFile.setSourceURI(siteURL.getURI());
	stagingToken.findSpaceForFile(stagedFile, bytes);
	stagedFile.setPhysicalLocation(siteURL, null);
	stagedFile.setSFNAppointedByUser();
	return stagedFile;
    }
    
    public static boolean verifySpaceVolumn() {
	Vector allDevices = new Vector();
	
	iSRMSpace v = getSpaceManager(TSRMSpaceType.Volatile);
	iSRMSpace d = getSpaceManager(TSRMSpaceType.Durable);
	iSRMSpace p = getSpaceManager(TSRMSpaceType.Permanent);
	
	if (v != null) {
	    allDevices.addAll(v.getDeviceList());
	} 
	if (d != null) {
	    allDevices.addAll(d.getDeviceList());
	}
	if (p != null) {
	    allDevices.addAll(p.getDeviceList());
	}

	//HashMap m = new HashMap();
	HashMap deviceSorter = new HashMap();
	
	for (int i=0; i<allDevices.size(); i++) {
	    TBasicDevice curr = (TBasicDevice)(allDevices.get(i));
	    String des = curr.getDeviceDisk();
	    Vector devices = (Vector)(deviceSorter.get(des));
	    if (devices == null) {
		Vector col = new Vector();
		col.add(curr);
		deviceSorter.put(des, col);
	    } else {
		devices.add(curr); 
	    }
	}
	
	if (deviceSorter.size() == 0) {
	    return true; // nothing to verify
	}
	
	Object[] entries = deviceSorter.keySet().toArray();
	for (int i=0; i<entries.length; i++) {
	    String key = (String)(entries[i]);
	    Vector devices = (Vector)(deviceSorter.get(key));
	    long bytesize = 0;
	    for (int k=0; k<devices.size(); k++) {
		//mbSize += ((TBasicDevice)(devices.get(k))).getTotalSizeInMB();
		bytesize += ((TBasicDevice)(devices.get(k))).getSpareBytes();
	    }

	    if (!((TBasicDevice)(devices.get(0))).verifyDeviceSize(bytesize)) {
		return false;
	    }
	}

	return true;
    }

    public static boolean init(Properties prop) {
	String vSpaceSpec = prop.getProperty(TSRMStorage._DefVolatileSpaceLocations);
	String dSpaceSpec = prop.getProperty(TSRMStorage._DefDurableSpaceLocations);
	String pSpaceSpec = prop.getProperty(TSRMStorage._DefPermanentSpaceLocations);

	if (!Config.isSpaceMgtDisabled()) {
	    if ((vSpaceSpec == null) && (dSpaceSpec == null) && (pSpaceSpec == null)) {
		TSRMUtil.startUpInfo("## error, need to specify cache locations, e.g."+TSRMStorage._DefVolatileSpaceLocations);
		return false;
	    } 
	} else {
	    if ((vSpaceSpec != null) || (dSpaceSpec != null) || (pSpaceSpec != null)) {
	        TSRMUtil.startUpInfo("## error, can not parse cache location specification. Space management is disabled.");
		TSRMUtil.startUpInfo("##   please remove or comment out all these entries:");
		TSRMUtil.startUpInfo("##\t"+TSRMStorage._DefVolatileSpaceLocations);
		TSRMUtil.startUpInfo("##\t"+TSRMStorage._DefDurableSpaceLocations);
		TSRMUtil.startUpInfo("##\t"+TSRMStorage._DefPermanentSpaceLocations);
		return false;
	    }
	}

	//System.out.println("vSpaceSpec="+vSpaceSpec);
	//System.out.println("pSpaceSpec="+pSpaceSpec);
	if ((vSpaceSpec != null) && !initVolatile(prop, vSpaceSpec.split(_DefLocationSeperator))) {
	    return false;
	}
	
	if ((dSpaceSpec != null) && !initDurable(prop, dSpaceSpec.split(_DefLocationSeperator))) {
	    return false;
	}
	if ((pSpaceSpec != null) && !initPermanent(prop, pSpaceSpec.split(_DefLocationSeperator))) {
	    return false;
	}
	
	//return verifySpaceVolumn();	
	return true;
    }
	
    //
    // we allow shared directories to be used in different types of spaces
    // e.g. c:/all can be used for volatile/durable/permanent. 
    // but we need ensure that in this case, we need to check the total free size specified 
    // in each space spec.
    //
    // we then also will make sure to verify the free space per disk!
    // 

	
    public static long getDefaultUserSpaceSize(iSRMSpace space) {
	//return TSRMStorage._DefDefaultUserSpaceSizeInMB*1024*1024;
	return space.getDefaultUserSpaceSizeInMB()*1048576;
    }
    
    public static long getDefaultDeviceSizeInMB() {
	//return TSRMStorage._DefDefaultUserSpaceSizeInMB*1024*1024;
	return _DefDefaultDeviceSizeInMB;
    }
    
    /*      
    */
    
    public static boolean initVolatile(Properties prop, String[] spaceSpecs) {		 		 
	TSRMSpaceVolatile v = new TSRMSpaceVolatile(prop, spaceSpecs);
	_colSpaceManagers.put(TSRMSpaceType.Volatile, v);
	
	String quota = prop.getProperty(TSRMStorage._DefVolatileSpaceUserQuotaMB);

	if (quota != null) {
	    int quotaInMB = Integer.parseInt(quota);
	    if (quotaInMB > 0) {
		TSRMUtil.startUpInfo("user quota on replica space: "+quotaInMB+"MB");
		TSRMSpaceType.Volatile.setUserQuota(quotaInMB*1048576);
	    }
	}
	return true;
    }
    
    public static boolean initDurable(Properties prop, String[] spaceSpecs) {		 
	TSRMSpaceDurable d = new TSRMSpaceDurable(prop, spaceSpecs);
	_colSpaceManagers.put(TSRMSpaceType.Durable, d);
	
	return true;
    }
    
    public static boolean initPermanent(Properties prop, String[] spaceSpecs) {		 
	TSRMSpacePermanent p = new TSRMSpacePermanent(prop, spaceSpecs);
	_colSpaceManagers.put(TSRMSpaceType.Permanent, p);
	return true;
    }
    
    public static Object[] getAllSpaceManagers() {
	return _colSpaceManagers.values().toArray();
    }

    public static iSRMSpace getSpaceManager(TFileStorageType fileType) {
	if (fileType == TFileStorageType.DURABLE) {
	    return getSpaceManager(TSRMSpaceType.Durable);
	} else if (fileType == TFileStorageType.PERMANENT) {
	    return getSpaceManager(TSRMSpaceType.Permanent);
	} else {
	    return getSpaceManager(TSRMSpaceType.Volatile);
	}
    }
    
    public static iSRMSpace getSpaceManager(TSRMSpaceType spaceType) {
	Object entry = _colSpaceManagers.get(spaceType);
	if (entry == null) {
	    return null;
	}
	return (iSRMSpace)entry;
    }
    
    public static iSRMSpace getSpaceManager(String tid) {
	TSRMSpaceType spaceType =TSRMSpaceType.getSpaceType(tid);
	if (spaceType == null)  {
	    return null;
	}
	return getSpaceManager(spaceType);
    }
    
    
    public static iSRMSpaceToken getToken(String tid) {
	iSRMSpace space = getSpaceManager(tid);
	if (space == null) {
	    return null;
	}
	return space.getToken(tid);
    }
    
    public static boolean isFileTypeConsistantWithSpaceType(TFileStorageType fileType, TSRMSpaceType spaceType) {
	if (fileType == null) {
	    return true;
	}
	
	boolean fileIsPermanent = (fileType == TFileStorageType.PERMANENT);
	boolean fileIsVolatile  = (fileType == TFileStorageType.VOLATILE);
	boolean fileIsDurable   = (fileType == TFileStorageType.DURABLE);
	
	boolean spaceIsPermanent = (spaceType == TSRMSpaceType.Permanent);
	boolean spaceIsVolatile  = (spaceType == TSRMSpaceType.Volatile);
	boolean spaceIsDurable   = (spaceType == TSRMSpaceType.Durable);
	
	if (fileIsPermanent && spaceIsPermanent) {
	    return true;
	}
	if (fileIsVolatile && spaceIsVolatile) {
	    return true;
	}
	if (fileIsDurable && spaceIsDurable) {
	    return true;
	}
	return false;
    }  

    public static TTokenAttributes checkReservationRequest(SrmReserveSpaceRequest r) {
	String currMethodName = "checkReservationRequest()";
	
	TSRMSpaceType assignedType = TSRMSpaceType.createSRMSpaceType(r.getRetentionPolicyInfo());
	if (getSpaceManager(assignedType) == null) {
	    TRetentionPolicy policy = r.getRetentionPolicyInfo().getRetentionPolicy();
	    TAccessLatency latency = r.getRetentionPolicyInfo().getAccessLatency();
	    throw new TSRMException("No support of : "+policy+" "+latency+" in this SRM.", false);
	}
	
	TTokenAttributes result = new TTokenAttributes(r);

	return result;	
    }

    public static String checkUpdateRequest(SrmUpdateSpaceRequest r) {
	String currMethodName = "checkUpdateRequest()";
	String errorMsg = null;

	if ((r.getNewSizeOfTotalSpaceDesired() == null) && 
	    (r.getNewSizeOfGuaranteedSpaceDesired() == null) &&
	    (r.getNewLifeTime() == null)) 
	{
	    errorMsg = "Nothing to be updated.";
	    TSRMLog.error(TSRMStorage.class, currMethodName, errorMsg, null);
	    return errorMsg;
	}

	String info = "";
	if (r.getNewLifeTime() != null) {
	    info += " NewLT="+r.getNewLifeTime().intValue();
	}

	if (r.getNewSizeOfGuaranteedSpaceDesired() != null) {
	    info += " NewbytesG="+ r.getNewSizeOfGuaranteedSpaceDesired().longValue();
	} 

	if (r.getNewSizeOfTotalSpaceDesired() != null) {
	    info += " NewbytesT="+ r.getNewSizeOfTotalSpaceDesired().longValue();
	} 

	TSRMLog.debug(TSRMStorage.class, currMethodName, info, null);
       
	if (r.getNewLifeTime() != null) {
	    if (r.getNewLifeTime().intValue() <=0) {
		errorMsg = "Lifetime_cannt_be_less_than_0";
		TSRMLog.error(TSRMStorage.class, currMethodName, errorMsg, null);
		return errorMsg;
	    }
	}
	if ((r.getNewSizeOfGuaranteedSpaceDesired() != null) &&
	    (r.getNewSizeOfTotalSpaceDesired() != null))
        {
	    if (r.getNewSizeOfGuaranteedSpaceDesired().longValue() > 
		r.getNewSizeOfTotalSpaceDesired().longValue()) 
	    {
		errorMsg = "MaxSpace_<_MinSpace!";
		TSRMLog.error(TSRMStorage.class, currMethodName, errorMsg, null);
		return errorMsg;
	    }
	}

	if (r.getNewSizeOfGuaranteedSpaceDesired() != null) {
	   if (r.getNewSizeOfGuaranteedSpaceDesired().longValue() <= 0) {
	       errorMsg = "\"Guaranteed space size cannt be <=0!\"";
	       TSRMLog.error(TSRMStorage.class, currMethodName, errorMsg, null);
	       return errorMsg;
	   }
	}
	
	if (r.getNewSizeOfTotalSpaceDesired() != null) {
	    if (r.getNewSizeOfTotalSpaceDesired().longValue() <= 0) {
		errorMsg = "\"total space size cannt be <=0!\"";
		TSRMLog.error(TSRMStorage.class, currMethodName, errorMsg, null);
		return errorMsg;
	    }
	}
	
	return null;
    }
}
