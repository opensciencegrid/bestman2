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

package gov.lbl.srm.util.sleepycat;

import java.util.*;
import com.sleepycat.je.*;
import gov.lbl.srm.util.*;

public class TSRMSleepyCatDbReader {
    static HashMap _devicesRead = new HashMap();
    static HashMap _tokensRead = new HashMap();
    //static HashMap _filesRead = null;

    public TSRMSleepyCatDbReader() {}

    public static boolean readAccounts(Database acctDB) {
	try {
	    TAccountBinding dummy = new TAccountBinding();
	    dummy.read(acctDB);
	    return true;
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMSleepyCatDbReader.class, "details", e);
	}
	return false;
    }

    public static boolean act(TSRMSleepyCatDbEnv env) {
	if (!readAccounts(env.getDBAccounts())) {
	    return false;
	}
	if (!readDevices(env.getDBDevices())) {
	    return false;
	}
	if (!readTokens(env.getDBTokens())) {
	    return false;
	}
	if (!readFiles(env.getDBFiles())) {
	    return false;
	}
	return true;
    }

    public static boolean readDevices(Database db) {
	try {
	    TDeviceBinding dummy = new TDeviceBinding();
	    dummy.read(db, _devicesRead);
	    /*
	    if ((_devicesRead == null) || (_devicesRead.size() == 0)) {
		return false;
	    }
	    */
	    TSRMUtil.startUpInfo(".........# of devices Read: "+_devicesRead.size());
	    return true;
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMSleepyCatDbReader.class, "details", e);
	}
	return false;
    }

    public static boolean readTokens(Database db) {
	try {
	    TSpaceTokenBinding dummy = new TSpaceTokenBinding();
	    dummy.read(db, _tokensRead);
	    /*
	    if ((_tokensRead == null) || (_tokensRead.size() == 0)) {
		return false;
	    }
	    */
	    TSRMUtil.startUpInfo(".........# of tokens Read: "+_tokensRead.size());
		TSRMUtil.sync(_tokensRead.keySet().toArray());
	    return true;
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMSleepyCatDbReader.class, "details", e);
	}
	return false;
    }

    public static boolean readFiles(Database db) {
	try {
	    TFileBinding dummy = new TFileBinding();
	    dummy.read(db);
	    return true;
	} catch (Exception e) {	   
	    TSRMLog.exception(TSRMSleepyCatDbReader.class, "details", e);
	}
	return false;
    }

}
