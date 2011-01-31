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

package gov.lbl.srm.util;

import  java.util.*;
import  java.io.*;

import  gov.lbl.srm.storage.*;
import  gov.lbl.srm.server.*;
import  gov.lbl.srm.util.sleepycat.*;

import com.sleepycat.je.*;
import com.sleepycat.bind.tuple.TupleBinding;

public class TSRMCacheLogSleepyCat implements ISRMLogCache {
    private static TSRMSleepyCatDbEnv _dbEnv = new TSRMSleepyCatDbEnv();
    private static DatabaseEntry _data = new DatabaseEntry();
    private static DatabaseEntry _key  = new DatabaseEntry();

    private boolean _isReading = false;

    public TSRMCacheLogSleepyCat(String name) 
    {
	//File path = new File(name);
	File path = TSRMUtil.initFile(name);
	if (!path.exists()) {
	    throw new TSRMException("No such path: "+name, false);
	} 
	// check after seeing what bdb is doing.
	if (!path.isDirectory()) {
	    throw new TSRMException(path.toString()+" is not a directory.", false);
	}

	try {
	    _dbEnv.setup(path, false);
	} catch (DatabaseException e) {	    
	    TSRMLog.exception(TSRMCacheLogSleepyCat.class, "details", e);
	    throw new TSRMException("Cannt set up the cache database."+path.toString(), false);
	}
    }
    

    private boolean readBack() {
	_isReading = true;
	try {
	    if (!TSRMSleepyCatDbEnv.readMe(_dbEnv)) {
		return false;
	    }
	} finally {
	    _isReading = false;
	}

	return true;
    }

    public void clean() {
	TSRMLog.info(this.getClass(), null, "event=CleaningUpDB.",  null);
	_dbEnv.close();
    }

    private void setDBKey(String id) {
	try {
	    _key = new DatabaseEntry(id.getBytes("UTF-8"));
	} catch (IOException e) {}
    }

    public void accountCreated(TAccount acct) {
	if (_isReading) {
	    return;
	}
	TAccountBinding.writeMe(acct, _dbEnv.getDBAccounts());
    }
    
    public void deviceCreated(TBasicDevice device) {
	if (_isReading) {
	    return;
	}

	TDeviceBinding.writeMe(device, _dbEnv.getDBDevices());
    }

    public void tokenCreated(TSRMStorage.iSRMSpaceToken token) {
	if (_isReading) {
	    return;
	}
	TSpaceTokenBinding.writeMe(token, _dbEnv.getDBTokens());
    }

    public void fileIsAdded(TSRMLocalFile f, long size, TSRMStorage.iSRMSpaceToken token) 
    {
	if (f.getToken() != null) {
	    addFile(f);
	}
    }

    public void deleteMe(String id, Database db) {
	if (_isReading) {
	    return;
	}

	DatabaseEntry curr = null;
	try {
	    curr = new DatabaseEntry(id.getBytes("UTF-8"));
	    db.delete(null, curr);
	} catch (java.io.IOException e) {	    
	    TSRMLog.exception(TSRMCacheLogSleepyCat.class, "details", e);
	} catch (com.sleepycat.je.DatabaseException e) {
	    TSRMLog.exception(TSRMCacheLogSleepyCat.class, "details", e);
	} finally {	
	    curr = null;
	}
    }

    public void tokenRemoved(TSRMStorage.iSRMSpaceToken token){
	deleteMe(token.getID(), _dbEnv.getDBTokens());
    }

    public void tokenExpired(String tid) {
	deleteMe(tid, _dbEnv.getDBTokens());
    }

    public void removeEntry(TSRMLocalFile f) {
	deleteMe(f.getCanonicalPath(), _dbEnv.getDBFiles());
    }

    public boolean removeFile(TSRMLocalFile f) {
	removeEntry(f);
	TSRMNameSpace.removeFile(f);
	return true;
    }

    public void addFile(TSRMLocalFile f) {
	if (_isReading) {
	    return;
	}
	TFileBinding.writeMe(f, _dbEnv.getDBFiles());
    }

    public boolean readLogEnds() { 	
	if (!readBack()) {
	    return false;
	}
	return true;
    }
    
}
