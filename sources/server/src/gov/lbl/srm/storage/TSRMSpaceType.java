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

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;

public class TSRMSpaceType {
    TRetentionPolicy _retentionPolicy = null;
    private long _userQuotaBytes = -1; // default: unlimited
    //TAccessLatency   _accessLatency = null;

    public static final TSRMSpaceType Volatile         = new TSRMSpaceType(TRetentionPolicy.REPLICA);//, TAccessLatency.ONLINE);
    public static final TSRMSpaceType Durable          = new TSRMSpaceType(TRetentionPolicy.OUTPUT); //, TAccessLatency.ONLINE);

    public static final TSRMSpaceType Permanent      = new TSRMSpaceType(TRetentionPolicy.CUSTODIAL);
    //public static final TSRMSpaceType PermanentMSS       = new TSRMSpaceType(TRetentionPolicy.CUSTODIAL, TAccessLatency.NEARLINE);
    
    public static final String _DefVolatileTokenPrefix 	= "V.";
    public static final String _DefDurableTokenPrefix 	= "D.";
    public static final String _DefPermanentTokenPrefix = "P.";

    private TSRMSpaceType(TRetentionPolicy policy) {
	_retentionPolicy = policy;
	//_accessLatency = latency;
    }
    
    public TRetentionPolicy getRetentionPolicy() {
	return _retentionPolicy;
    }

    public boolean isPermanent() {
	return (getRetentionPolicy() == TRetentionPolicy.CUSTODIAL);
    }

    public boolean matches(TRetentionPolicyInfo policyInfo) {
	return (policyInfo.getRetentionPolicy()  == _retentionPolicy);
    }
	    
    public static TSRMSpaceType createSRMSpaceType(TRetentionPolicyInfo info) {
	if (info == null) {
	    //return null;
	    return TSRMSpaceType.Volatile;
	}
	if (info.getRetentionPolicy() == TRetentionPolicy.REPLICA) {
	    if (info.getAccessLatency() != TAccessLatency.NEARLINE) {
		return TSRMSpaceType.Volatile;
	    }
	}
		 
	if (info.getRetentionPolicy() == TRetentionPolicy.CUSTODIAL) {	
	    return TSRMSpaceType.Permanent;
	}

	throw new TSRMException("Not supported space type:"+info.getRetentionPolicy()+" "+info.getAccessLatency(), false);
    }
    
    public static TSRMSpaceType getSpaceType(String tid) {
	if (tid.startsWith(TSRMSpaceType._DefVolatileTokenPrefix)) {
	    return TSRMSpaceType.Volatile;
	} else if (tid.startsWith(TSRMSpaceType._DefDurableTokenPrefix)) {
	    return TSRMSpaceType.Durable;
	} else if (tid.startsWith(TSRMSpaceType._DefPermanentTokenPrefix)) {
	    return TSRMSpaceType.Permanent;
	} 
	return null;
    }

    public boolean isInfiniteLifetimeAllowed() {
	if (this == TSRMSpaceType.Permanent) {
	    return true;
	}
	return false;
    }

    public long getUserQuota() {
	return _userQuotaBytes;
    }

    public void setUserQuota(long q) {
	_userQuotaBytes = q;
	TSRMLog.info(this.getClass(), "setUserQuota()", "bytes="+q, "retentionPolicy="+_retentionPolicy);	
    }
}
