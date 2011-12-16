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

package gov.lbl.srm.storage.Volatile;

//import java.util.Date;
import java.util.*;

//import com.sun.rsasign.d;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.server.*;

import gov.lbl.srm.policy.TSRMStoragePolicyDefault;

//import EDU.oswego.cs.dl.util.concurrent.Mutex;



import gov.lbl.srm.storage.*;

public class TSRMSpaceVolatile extends TSpaceSkeleton {
    //TVolatileSpaceToken _publicToken = null;
    //TVolatilePublicToken _publicToken = null;
    TVolatilePublicToken[] _publicTokenList = null;
    int _currTokenPos = -1; // current tokenUsed
    
    //
    //////////////////////////////////////////////////////////////////////////
    //
    // constructor & supporting functions
    //
    // wont pass empty list here from TSRMStorage, so wont check
    //
    //////////////////////////////////////////////////////////////////////////
    //
    public TSRMSpaceVolatile(Properties prop, String[] spaceSpecs) {
	super(prop, spaceSpecs);
	int totalNumDevices = getNumDevices();
	_publicTokenList = new TVolatilePublicToken[totalNumDevices];

	for (int i=0; i<totalNumDevices; i++) {
	    _publicTokenList[i] = assignPublicToken(prop, getNthDevice(i), "V.0."+i);
	}
    }
    
    
    private TVolatilePublicToken  assignPublicToken(Properties prop, TBasicDevice tgtDevice, String tid) {
	long publicTokenSizeMB = tgtDevice.getPolicy().getPublicTokenSizeInMB();
	TSRMUtil.startUpInfo("####### public volatile space to reserve: "+ publicTokenSizeMB+"MB");
	long size = publicTokenSizeMB*1024*1024;
	
	TVolatilePublicToken resultPublicToken = null;
	if (size > 0) {
	    TTokenAttributes attr = new TTokenAttributes(TSRMSpaceType.Volatile, size);
	    attr.setTokenID(tid);
	    attr.setLifeTimeInSeconds(-1);
	    //resultPublicToken = (TVolatilePublicToken)(preAllocateToken(attr));  
	    resultPublicToken = (TVolatilePublicToken)(tgtDevice.accomodate(attr));
	    if (resultPublicToken == null) {
		tgtDevice.compactDevice();
		resultPublicToken = (TVolatilePublicToken)(tgtDevice.accomodate(attr));
	    }
	    resultPublicToken.setOwner(TAccountManager._SUPER_USER);
	    TSRMUtil.startUpInfo("####### created public token "+publicTokenSizeMB+" MB");
	    resultPublicToken.getTokenAttributes().print();
	    TSRMLog.info(this.getClass(), null, "publicTokenAssigned_id="+resultPublicToken.getID(), null);
	    
	    TSRMPublicTokenConfiguration config = new TSRMPublicTokenConfiguration(prop, size);
	    resultPublicToken.setConfiguration(config);
	} 
	return resultPublicToken;
    }
    
    public boolean authorize(TAccount owner, TTokenAttributes requestDescription) {
	return true;
    }
    
    public TSRMStorage.iSRMSpaceToken locateDefaultToken(TFileStorageType fileType) {
	if (fileType != TFileStorageType.VOLATILE) {
	    return null;
	}
	_currTokenPos  = (_currTokenPos + 1) % getNumDevices();
	return _publicTokenList[_currTokenPos];
    }
    
    public TBasicDevice getRefDevice(TSURLInfo surlInfo) {
	return null;
    }
}
 
