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

//import EDU.oswego.cs.dl.util.concurrent.Mutex;


public abstract class TSRMDeviceBalancePolicy {
    protected TSRMMultiDeviceHandler _deviceInfo = null;   

    public TSRMDeviceBalancePolicy(TSRMMultiDeviceHandler h) {
	_deviceInfo = h;
    }

    public abstract TReturnStatus createToken(TAccount owner, TTokenAttributes requestDescription);

    public static TSRMDeviceBalancePolicy getDefaultPolicy(TSRMMultiDeviceHandler h) {
	TSRMDeviceBalancePolicy defaultPolicy = null;
	String currPkg = TSRMDeviceBalancePolicy.class.getPackage().getName();
	
	String defaultName = currPkg+".TSRMDeviceBalancePolicyPreferred";

	try {
	    Class balanceClass = Class.forName(defaultName);
	    java.lang.Class[] inputSignature = new java.lang.Class[1];
	    inputSignature[0] = TSRMMultiDeviceHandler.class;
	    java.lang.reflect.Constructor constructor = balanceClass.getConstructor(inputSignature);
	    
	    Object[] inputObj = new Object[1];
	    inputObj[0] = h;
	    defaultPolicy = (TSRMDeviceBalancePolicy)(constructor.newInstance(inputObj));	   
	    TSRMUtil.startUpInfo("## loaded Balance policy: "+defaultPolicy);
	    return defaultPolicy;
	} catch (ClassNotFoundException e) {
	    TSRMUtil.startUpInfo(">>>>>>>.... no such class:"+defaultName);
	} catch (Exception e) {
	    TSRMUtil.startUpInfo(">>>>>>>.... cannt load class:"+defaultName);	    
	    TSRMLog.exception(TSRMDeviceBalancePolicy.class, "details", e);	    
	}

	defaultPolicy = new TSRMDeviceBalancePolicyColdPotato(h);	    	
	return defaultPolicy;
    }
}
