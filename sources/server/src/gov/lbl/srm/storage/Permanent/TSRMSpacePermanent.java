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

package gov.lbl.srm.storage.Permanent;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
import java.util.*;
 

public class TSRMSpacePermanent extends TSpaceSkeleton  {
        TPermanentSpaceToken[] _publicTokenList = null;
        int _currTokenPos = -1; // current tokenUsed
    
    
	public TSRMSpacePermanent(Properties prop, String[] spaceSpecs) {
	    super(prop, spaceSpecs);
	    Vector v = assignPublicToken(prop);
	    if (v.size() > 0) {
		_publicTokenList = new TPermanentSpaceToken[v.size()];
		for (int i=0; i<v.size(); i++) {
		    _publicTokenList[i] = (TPermanentSpaceToken)(v.get(i));
		}
	    }
	    // no public token
	}	
    
        private Vector assignPublicToken(Properties prop) {
	    long publicTokenSizeMB = 0;
	    Vector v = new Vector();
	    for (int i=0; i<getNumDevices(); i++) {
		TBasicDevice curr = getNthDevice(i);
		if (curr.getAccessLatency() == TAccessLatency.ONLINE) {
		    TPermanentSpaceToken resultPublicToken = null;
		    publicTokenSizeMB = curr.getPolicy().getPublicTokenSizeInMB();
		    TSRMUtil.startUpInfo("####### public permanent space to reserve: "+ publicTokenSizeMB+"MB");
		    long size = publicTokenSizeMB*1024*1024;
		    
		    if (size > 0) {
			TTokenAttributes attr = new TTokenAttributes(TSRMSpaceType.Permanent, size);
			attr.setTokenID("P.0");
			attr.setLifeTimeInSeconds(-1);
			resultPublicToken = (TPermanentSpaceToken)(curr.accomodate(attr));  
			if (resultPublicToken == null) {
			    curr.compactDevice();
			    resultPublicToken = (TPermanentSpaceToken)(curr.accomodate(attr));
			}
			resultPublicToken.setOwner(TAccountManager._SUPER_USER);
			TSRMUtil.startUpInfo("####### created public token "+publicTokenSizeMB+" MB");
			resultPublicToken.getTokenAttributes().print();
			TSRMLog.info(this.getClass(), null, "event=public_token_created","id="+resultPublicToken.getID());
		    }
		    if (resultPublicToken != null) {
			v.add(resultPublicToken);
		    }
		}
	    }	    
	    return v;	    
	 }
    

	public boolean authorize(TAccount owner, TTokenAttributes requestDescription) {
	    return true;
	}
	
	public TSRMStorage.iSRMSpaceToken locateDefaultToken(TFileStorageType fileType) {
	    if (fileType != TFileStorageType.PERMANENT) {
		return null;
	    }	
	    //_currTokenPos  = (_currTokenPos + 1) % getNumDevices();
	    if (_publicTokenList == null) {
		return null;
	    }
	    if (_publicTokenList.length == 0) {
		return null;
	    }
	    _currTokenPos = (_currTokenPos +1) % (_publicTokenList.length);
	    return _publicTokenList[_currTokenPos];
	}
	
	public TBasicDevice getRefDevice(TSURLInfo surlInfo) {
	    //String host = surlInfo.getSURLOrStFN().getValue().getHost();
	    for (int i=0; i<getNumDevices(); i++) {
		TBasicDevice curr = getNthDevice(i);
		if (curr.isDeviceSpecific(surlInfo)) {
		    return curr;
		}
	    }
	    return null;
	}
} 
