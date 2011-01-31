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

package gov.lbl.srm.policy;

import gov.lbl.srm.storage.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;

import java.util.*;

public class TSRMStoragePolicyDefault implements TSRMStorage.iStoragePolicy {
    public static long SYSTEM_DEFAULT_SIZE_PER_TOKEN_MB = 500;
    public static long SYSTEM_PUBLIC_TOKEN_PERCENTAGE = 50;
    public  long _DEFAULT_SPACE_LIFETIME_SECS = 999999; 
    public  long _MAX_SPACE_LIFETIME_ALLOWED  = 24*3600; // one day
    
    // may need to be adjusted according to the cache size
    public  long _DEFAULT_SIZE_PER_TOKEN_MB = SYSTEM_DEFAULT_SIZE_PER_TOKEN_MB; // default: granted = total 
    public  long _MAX_SIZE_PER_TOKEN_MB     = 2500000;

    public  long _PUBLIC_TOKEN_SIZE_MB = 0;
    public  long _PUBLIC_TOKEN_PERCENTAGE = SYSTEM_PUBLIC_TOKEN_PERCENTAGE;

    public  long _DEFAULT_FILE_SIZE_MB = 10000;
    public  long _MAX_SPACE_PER_USER   = 2500000;

    //public  long _DEFAULT_USER_SPACE_MB = 2500000;
    
    public final static String _DefDefaultSpaceLifeTime = "DefaultSpaceLifeTimeInSeconds";
    public final static String _DefMaxSpaceLifeTime     = "MaxSpaceLifeTimeInSeconds";   

    public final static String _DefDefaultMBPerToken    = "DefaultMBPerToken";
    public final static String _DefMaxMBPerToken        = "MaxMBPerToken";

    public final static String _DefMaxMBPerUser         = "MaxMBPerUser";
    public final static String _DefDefaultFileSizeMB    = "DefaultFileSizeMB";

    public final static String _DefPublicTokenMB        = "PublicSpaceInMB";
    public final static String _DefPublicTokenPercentage= "PublicSpaceProportion";
   
    
    // max continuous disk size to be used
    public TSRMStoragePolicyDefault(Properties prop, long limitInMB) {
	if (prop != null) {
	    getUserSpecs(prop);
	}
       
	if (limitInMB > 0) {
	    TSRMUtil.startUpInfo("==> DEVICE size limit: "+limitInMB+" MB ");
	    if (_PUBLIC_TOKEN_SIZE_MB >= limitInMB) {
		_PUBLIC_TOKEN_SIZE_MB = limitInMB/(long)2;
	    }

	    if (_PUBLIC_TOKEN_SIZE_MB == 0) {
		_PUBLIC_TOKEN_SIZE_MB = (limitInMB*_PUBLIC_TOKEN_PERCENTAGE)/(long)100;
	    }
	    
	    if (_DEFAULT_SIZE_PER_TOKEN_MB + _PUBLIC_TOKEN_SIZE_MB > limitInMB) {
		_DEFAULT_SIZE_PER_TOKEN_MB = limitInMB - _PUBLIC_TOKEN_SIZE_MB ;
	    }
	    
	    if (_MAX_SIZE_PER_TOKEN_MB > limitInMB) {
		_MAX_SIZE_PER_TOKEN_MB  = limitInMB;
	    }
	    
	    if (_DEFAULT_FILE_SIZE_MB >= limitInMB) {
		_DEFAULT_FILE_SIZE_MB = limitInMB/(long)10;
	    }	    
	} else {
	    TSRMUtil.startUpInfo("==> DEVICE size is [unlimited.]");
	}

	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefDefaultSpaceLifeTime+" => "+_DEFAULT_SPACE_LIFETIME_SECS);
        TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefMaxSpaceLifeTime +" => "+_MAX_SPACE_LIFETIME_ALLOWED);
	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefDefaultMBPerToken +" => "+_DEFAULT_SIZE_PER_TOKEN_MB);
	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefMaxMBPerToken  +" => "+ _MAX_SIZE_PER_TOKEN_MB);
	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefMaxMBPerUser + " => "+_MAX_SPACE_PER_USER);
	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefDefaultFileSizeMB + " => "+_DEFAULT_FILE_SIZE_MB);
	TSRMUtil.startUpInfo(TSRMStoragePolicyDefault._DefPublicTokenMB + " => "+_PUBLIC_TOKEN_SIZE_MB);
    }    

    

    public void getUserSpecs(Properties prop) {
	String publicTokenStr = prop.getProperty(TSRMStoragePolicyDefault._DefPublicTokenMB);
	if (publicTokenStr != null) {
	    _PUBLIC_TOKEN_SIZE_MB = Integer.parseInt(publicTokenStr);
	    TSRMUtil.noNegative((_PUBLIC_TOKEN_SIZE_MB < 0), TSRMStoragePolicyDefault._DefPublicTokenMB);
	}	
	String publicTokenPercentage = prop.getProperty(TSRMStoragePolicyDefault._DefPublicTokenPercentage);
	if (publicTokenPercentage != null) {
	    _PUBLIC_TOKEN_PERCENTAGE=Integer.parseInt(publicTokenPercentage);
	    TSRMUtil.noNegative((_PUBLIC_TOKEN_PERCENTAGE < 0), TSRMStoragePolicyDefault._DefPublicTokenPercentage);
	    if (_PUBLIC_TOKEN_PERCENTAGE > 100) {
		TSRMUtil.startUpInfo("Cannt exceed 100%");
		System.exit(1);
	    }
	}

	String defaultTokenStr = prop.getProperty(TSRMStoragePolicyDefault._DefDefaultMBPerToken);
	if (defaultTokenStr != null) {
	    _DEFAULT_SIZE_PER_TOKEN_MB = Long.parseLong(defaultTokenStr);
	    TSRMUtil.noNegative((_DEFAULT_SIZE_PER_TOKEN_MB < 0), TSRMStoragePolicyDefault._DefDefaultMBPerToken);
	}

	String defaultFileSizeStr = prop.getProperty(TSRMStoragePolicyDefault._DefDefaultFileSizeMB);
	if (defaultFileSizeStr != null) {
	    _DEFAULT_FILE_SIZE_MB = Long.parseLong(defaultFileSizeStr);
	    TSRMUtil.noNegative((_DEFAULT_FILE_SIZE_MB < 0), TSRMStoragePolicyDefault._DefDefaultFileSizeMB);
	}
    }

    public long getDefaultUserSpaceSizeInMB() {
	return _DEFAULT_SIZE_PER_TOKEN_MB;
    }
    // note this is the general policy of space negociation.
    // program will check the actual space available after obtaining the policy
    // then decide on how much space to grant to user
    public TStatusCode negociate(TTokenAttributes reqDesc) {
	long totalBytes = reqDesc.getTotalSpaceBytes();

	if (totalBytes < 0) {
	    totalBytes = getDefaultSpaceGranted(); // _DEFAULT_SIZE_PER_TOKEN_MB*1048576;
	    reqDesc.setTotalSpaceBytes(totalBytes);
	    reqDesc.setGuaranteedSpaceBytes(totalBytes);
	}

	if (reqDesc.getTypeOfSpace() == TSRMSpaceType.Permanent) {
	    return TStatusCode.SRM_SUCCESS;
	}
	//
	// assign a fixed lifetime, regardless of requested lifetime
	//
	if ((reqDesc.getLifeTimeInSeconds() > 0) && (reqDesc.getLifeTimeInSeconds() < _DEFAULT_SPACE_LIFETIME_SECS)) {
	    //
	} else {
	    reqDesc.setLifeTimeInSeconds(_DEFAULT_SPACE_LIFETIME_SECS);
	}		
	
	if (totalBytes < getDefaultSpaceGranted()) {
	    //reqDesc.setTotalSpace(getDefaultSpaceGranted());
	    reqDesc.setGuaranteedSpaceBytes(reqDesc.getTotalSpaceBytes());
	    return TStatusCode.SRM_SUCCESS;
	} else { 
	    //
	    // assign a fixed space
	    //
	    reqDesc.setTotalSpaceBytes(getDefaultSpaceGranted());
	    reqDesc.setGuaranteedSpaceBytes(getDefaultSpaceGranted());
	    if (getDefaultSpaceGranted() == 0) {
		return TStatusCode.SRM_NO_FREE_SPACE;
	    } else {
		return TStatusCode.SRM_LOWER_SPACE_GRANTED;
	    }
	}
    }
    
    public long getMaxSpaceLifeTime() {
	return _MAX_SPACE_LIFETIME_ALLOWED;
    }
    
    public long determineGuaranteedSpaceToGrantForAToken(long min, long max, long was, long currFreeSpace) {
	if (max < was) {
	    return max;
	}
	
	/*
	if (currFreeSpace == 0) {
	    return 0;
	}
	*/

	long grantedMax = getMaxGrantedSpacePerToken();
	if (min > grantedMax) {
	    return 0; // min is too large.
	}
	if (max > grantedMax) {
	    max = grantedMax;
	}
	
	if (currFreeSpace + was > max) {
	    return max;
	} 
	
	
	//
	// now max is not reachable, so just try to accomodate min requirement. wont set middle ground
	//
	if (min == 0) {
	    return was; // no need to do anything
	}
	
	if (min < was) {
	    return min; // stay put
	}
	if (min > currFreeSpace) { // cannt update space to meet the requirement
	    return 0;
	}
		
	if (currFreeSpace + was > min) {
	    return min;
	} else {
	    return was;
	}
    }
	 
    public long determineExtraTotalSpaceToGrantForAToken(long proposed, long was, long currFreeSpace) {
	if (proposed < was) {
	    return proposed - was;
	}
	
	long extra;
	long max = getMaxTotalSpace();
	
	if (proposed > max)  {
	    extra = max - was;
	} else {
	    extra = proposed - was;
	}
	if (extra > currFreeSpace) {
	    return currFreeSpace;
	}
	
	return extra;
    }
    
    public long getPublicTokenSizeInMB() {
	return _PUBLIC_TOKEN_SIZE_MB;
    }
    
    private long getDefaultSpaceGranted() {
	return _DEFAULT_SIZE_PER_TOKEN_MB*1024*1024;
    }
    
    private long getMaxGrantedSpacePerToken () {
	return _MAX_SIZE_PER_TOKEN_MB*1024*1024;
    }
    
    private long getMaxTotalSpace() {
	return _MAX_SIZE_PER_TOKEN_MB*1024*1024;
    }
    
    public long getDefaultFileSize() {
	return  _DEFAULT_FILE_SIZE_MB*1024*1024;
    }
}
