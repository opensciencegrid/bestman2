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
import gov.lbl.srm.policy.*;
import org.apache.axis.types.*;

//
// this class is a collection of descriptions
//
public class TTokenAttributes {
    TSRMSpaceType _spaceType = null;
    TAccessLatency _accessLatency = null;
    String     _userDescription		= null;
    long       _iTotalSpace 		= -1;
    long       _iGuaranteedSpace 	= -1;
    long       _iLifeTimeInSeconds 	= -1;
    
    //String     _uid = null;
    TDeviceAccessInfo     _accessInfo = null;
    
    String     _tokenID = null;
    
    public TTokenAttributes(TSRMSpaceType type, long size) {
	_iTotalSpace 		= size;
	_iGuaranteedSpace	= size;
	_spaceType			= type;
	_iLifeTimeInSeconds = 0; // no life time
    }
    
    public TTokenAttributes (SrmReserveSpaceRequest r) {
	_userDescription = r.getUserSpaceTokenDescription();
	if (r.getRetentionPolicyInfo() == null) {
	    _spaceType = TSRMSpaceType.Volatile;
	} else {
	    TSRMLog.debug(TTokenAttributes.class, null,
			 "event=reserveSpace()",
			 "rp="+r.getRetentionPolicyInfo().getRetentionPolicy()+ 
			 " latency="+r.getRetentionPolicyInfo().getAccessLatency());
	    _spaceType = TSRMSpaceType.createSRMSpaceType(r.getRetentionPolicyInfo());
	    _accessLatency = r.getRetentionPolicyInfo().getAccessLatency();
	}

	Integer lifetimeObj = r.getDesiredLifetimeOfReservedSpace();
	UnsignedLong totalBytesObj = r.getDesiredSizeOfTotalSpace();
	UnsignedLong guaranteedBytesObj = r.getDesiredSizeOfGuaranteedSpace();
	
	TSRMLog.debug(TTokenAttributes.class, null, "event=reservespace desc="+_userDescription, 
		     "lifetime="+lifetimeObj+" total="+totalBytesObj+" guaranteed:"+guaranteedBytesObj);
	if (_spaceType == TSRMSpaceType.Permanent) {
	   // leave lifetime as -1
	} else {
	   if (lifetimeObj != null) {
	      _iLifeTimeInSeconds = lifetimeObj.intValue();
	      if ((_iLifeTimeInSeconds < 0) && (!_spaceType.isInfiniteLifetimeAllowed())) {
		     throw new TSRMException("Cannt have negative value for lifetime.", false); 
	      }
	   } else {
	    _iLifeTimeInSeconds = TSRMStorage._DefDefaultUserSpaceLifeTime;
	   }
	}

	if (guaranteedBytesObj != null) {
	    noNegative(guaranteedBytesObj.longValue(), "GuaranteedSpace");
	} 
	if (totalBytesObj != null) {
	    noNegative(totalBytesObj.longValue(), "Total Space");
	}

	if ((guaranteedBytesObj == null) && (totalBytesObj == null)) {
	    // use default
	    //_iGuaranteedSpace = TSRMStoragePolicyDefault._DEFAULT_SIZE_PER_TOKEN_MB*1048576;
	    //_iTotalSpace = _iGuaranteedSpace;
	} else if ((guaranteedBytesObj != null) && (totalBytesObj != null)) {
	    if (guaranteedBytesObj.longValue() > totalBytesObj.longValue()) {
		throw new TSRMException("MaxSpace < MinSpace!", false);
	    } 
	    _iGuaranteedSpace = guaranteedBytesObj.longValue();
	    _iTotalSpace = totalBytesObj.longValue();
	} else if (guaranteedBytesObj == null) {
	    _iTotalSpace = totalBytesObj.longValue();
	    _iGuaranteedSpace = _iTotalSpace;
	} else {
	    _iGuaranteedSpace = guaranteedBytesObj.longValue();
	    _iTotalSpace = _iGuaranteedSpace;
	}
	
	setAccessInfo(r.getStorageSystemInfo());

    }

    public static void noNegative(long v, String comment) {
	if  (v < 0) {
	    throw new TSRMException("No negative values allowed. "+comment, false);
	}
    }

    public void print() {
	TSRMUtil.startUpInfo("####### "+this );
	TSRMUtil.startUpInfo("# "+getTokenID());
	TSRMUtil.startUpInfo("# ["+_userDescription+"]");
	TSRMUtil.startUpInfo("# space reservation request entries:");
	TSRMUtil.startUpInfo("#    => "+_iTotalSpace+" total.");
	TSRMUtil.startUpInfo("#    => "+_iGuaranteedSpace+" guaranteed.");
	TSRMUtil.startUpInfo("#    => "+_iLifeTimeInSeconds+" second lifetime");
	TSRMUtil.startUpInfo("#    => "+_spaceType);
	TSRMUtil.startUpInfo("#    => "+_accessLatency);
	if (getAccessInfo() != null) {
	    TSRMUtil.startUpInfo("#    => access info:"); getAccessInfo().printMe();
	} else {
	    TSRMUtil.startUpInfo("#    => null access info");
	}
	TSRMUtil.startUpInfo("#######");
    }
    
    public void log() {
	TSRMLog.debug(this.getClass(), null, "event=createdToken id="+getTokenID(), " LT="+_iLifeTimeInSeconds+" Gbytes="+_iGuaranteedSpace);
    }
    
    public String getSpaceToken() {
	return getTokenID();
    }
    
    public String getTokenID() {
	return _tokenID;
    }

    public TAccessLatency getAccessLatency() {
	return _accessLatency;
    }

    public void setAccessLatency(TAccessLatency al) {
	_accessLatency = al;
    }

    public TDeviceAccessInfo getAccessInfo() {
	return _accessInfo;
    }
    
    public void setAccessInfo(ArrayOfTExtraInfo info) {
	if (info!= null) {
	    _accessInfo  = new TDeviceAccessInfo(info);
	}
    }
    
    public void setTokenID(String id) {
	_tokenID = id;
    }
    
    public void setUserDesc(String desc) {
	_userDescription = desc;
    }

    public String getUserDesc() {
	return _userDescription;
    }

    public boolean agreesWithDescription(String desc) {
	if ((_userDescription != null) && (desc != null)) {
	    return _userDescription.equals(desc);
	}
	return false;
    }
    
    public TSRMSpaceType getTypeOfSpace() {
	return _spaceType;
    }
    
    public void setLifeTimeInSeconds(long lifeTime) {
	_iLifeTimeInSeconds = lifeTime;
    }
    
    public long getLifeTimeInSeconds() {
	return _iLifeTimeInSeconds;
    }
    
    public Integer getLifeTimeObj() {
	return TSRMUtil.createTLifeTimeInSeconds(_iLifeTimeInSeconds, false);
    }

    public org.apache.axis.types.UnsignedLong getTotalSpaceByteObj() {
	return TSRMUtil.createTSizeInBytes(_iTotalSpace);
    }

    public org.apache.axis.types.UnsignedLong getGuaranteedSpaceByteObj() {
	return TSRMUtil.createTSizeInBytes(_iGuaranteedSpace);
    }
    
    public long getTotalSpaceBytes() {
	return _iTotalSpace;
    }
    
    public long getGuaranteedSpaceBytes() {
	return _iGuaranteedSpace;
    }
    
    public void setTotalSpaceBytes(long ts) {
	_iTotalSpace = ts;
    }
    
    public void setGuaranteedSpaceBytes(long gs) {
	_iGuaranteedSpace = gs;
    }
    
    public Integer getLifeTimeLeftObj(long startTimeInMillis) {
	long timeLeft = this._iLifeTimeInSeconds*1000 - (System.currentTimeMillis() - startTimeInMillis);
	if (timeLeft < 0) {
	    return TSRMUtil.createTLifeTimeInSeconds(0, false);
	} else {
	    return TSRMUtil.createTLifeTimeInSeconds(timeLeft/(long)1000, false);
	}
    }
    
    public org.apache.axis.types.UnsignedLong getUnusedBytesObj(long reservedBytes) {
	return TSRMUtil.createTSizeInBytes(getGuaranteedSpaceBytes()-reservedBytes);
    }   
}
