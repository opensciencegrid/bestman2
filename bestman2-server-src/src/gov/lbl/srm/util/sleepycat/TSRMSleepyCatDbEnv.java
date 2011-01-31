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

import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.*;


public class TSRMSleepyCatDbEnv {
    private Environment _env;

    private Database _dbAccounts;
    private Database _dbDevices;
    private Database _dbFiles;
    private Database _dbTokens;

    public static TSRMSleepyCatDbReader _reader = null;

    public TSRMSleepyCatDbEnv() 
    {}

    public void setup(java.io.File envHome, boolean readOnly) throws DatabaseException 
    {
	EnvironmentConfig envConfig = new EnvironmentConfig();
        DatabaseConfig dbConfig = new DatabaseConfig();

	envConfig.setReadOnly(readOnly);
	dbConfig.setReadOnly(readOnly);

	envConfig.setAllowCreate(!readOnly);
	dbConfig.setAllowCreate(!readOnly);

	envConfig.setTransactional(!readOnly);
	dbConfig.setTransactional(!readOnly);

	_env = new Environment(envHome, envConfig);
	
	_dbAccounts = _env.openDatabase(null, "srmAccountsDB", dbConfig);
	_dbTokens = _env.openDatabase(null,  "srmSpaceTokensDB", dbConfig);
	_dbDevices = _env.openDatabase(null,  "srmDevicesDB", dbConfig);
	_dbFiles = _env.openDatabase(null,  "srmFilesDB", dbConfig);	
    }

    public Environment getEnv() {
	return _env;
    }

    public Database getDBAccounts() {
	return _dbAccounts;
    }

    public Database getDBTokens() {
	return _dbTokens;
    }

    public Database getDBDevices() {
	return _dbDevices;
    }

    public Database getDBFiles() {
	return _dbFiles;
    }

    public static boolean readMe(TSRMSleepyCatDbEnv env) {
	_reader = new TSRMSleepyCatDbReader();
	return _reader.act(env);
    }

    public void close() {
	if (_env != null) {
	    try {
		_dbAccounts.close();
		_dbDevices.close();
		_dbFiles.close();
		_dbTokens.close();

		_env.close();
	    } catch (DatabaseException e) {
		System.err.println("Error closing database env: "+e.toString());
	    }
	}
    }
}

