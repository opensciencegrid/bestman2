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

import java.util.*;

import gov.lbl.srm.storage.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;

import EDU.oswego.cs.dl.util.concurrent.Mutex;


public class TSRMDeviceBalancePolicyColdPotato extends TSRMDeviceBalancePolicy  {
    //TSRMMultiDeviceHandler _deviceInfo = null;
    Mutex _accessMutex = new Mutex();

    public TSRMDeviceBalancePolicyColdPotato(TSRMMultiDeviceHandler h) {
	super(h);
	//_deviceInfo = h;
    }

    // fill up devices sequentially
    public TReturnStatus createToken(TAccount owner, TTokenAttributes requestDescription) {
	requestDescription.print();
	if (_deviceInfo.getSize() == 1) {	    
	    TBasicDevice curr = (TBasicDevice)(_deviceInfo.getNthDevice(0));
	    return curr.fitToken(owner, requestDescription);
	}

	TSRMUtil.acquireSync(_accessMutex);		
	try {	    
	    Object[] asArray = _deviceInfo.getList().toArray();

	    //Arrays.sort(asArray);
	    TReturnStatus status = null;
	    for (int i=0; i<asArray.length; i++) {
		TBasicDevice curr = (TBasicDevice)asArray[i];
		status = curr.fitToken(owner, requestDescription);
		if ((status.getStatusCode() == TStatusCode.SRM_LOWER_SPACE_GRANTED) || 
		    (status.getStatusCode() == TStatusCode.SRM_SUCCESS)) 
		{
		    return status;
		}								       
	    }
	    return status;
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }
}
