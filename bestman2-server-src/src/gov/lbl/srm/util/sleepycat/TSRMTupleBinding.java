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

import gov.lbl.srm.server.*;
import gov.lbl.srm.util.*;

import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

public abstract class TSRMTupleBinding extends TupleBinding {
    protected static DatabaseEntry _data = new DatabaseEntry();
    private static DatabaseEntry _key  = new DatabaseEntry();

    public static void setKey(String id) {
	try {
	    _key = new DatabaseEntry(id.getBytes("UTF-8"));
	} catch (java.io.IOException e) {}
    }

    public static void writeData(Database db) {
	//System.out.println("..............writting:["+_key+"] = "+_data);
	// using autocommit
	try {
	    db.put(null, _key, _data);
	} catch (DatabaseException e) {
	    TSRMLog.exception(TSRMTupleBinding.class, "details", e);
	}
    }

    public void read(Database db) throws DatabaseException {
	read(db, null);
    }

    public void read(Database db, java.util.HashMap result) throws DatabaseException {		
	Cursor cursor = db.openCursor(null, null);

        // DatabaseEntry objects used for reading records
        DatabaseEntry currKey = new DatabaseEntry();
        DatabaseEntry currData = new DatabaseEntry();

	//
	// note that LockMode.DEFAULT seems to make the lock in write mode.
	// so I specify it to be read_uncommitted here
	//
	try { // always want to make sure the cursor gets closed
            while (cursor.getNext(currKey, currData, LockMode.READ_UNCOMMITTED) == OperationStatus.SUCCESS) {	
                Object obj = entryToObject(currData);                    
		TSRMUtil.startUpInfo("... got an obj: "+obj);
		if ((result != null) && (obj != null)) {
		    String keyStr = new String(currKey.getData(), "UTF-8");
		    result.put(keyStr, obj);
		} else if (obj == null) {
		    db.delete(null, currKey);
		}
            }
        } catch (Exception e) {
            System.err.println("Error on cursor for:"+db.getDatabaseName());
            System.err.println(e.toString());            
	    TSRMLog.exception(TSRMTupleBinding.class, "details", e);
        } finally {
            cursor.close();
        }
	//Object obj = entryToObject(currData);                    
	//System.out.println("... got an obj: "+obj);
    }
}
