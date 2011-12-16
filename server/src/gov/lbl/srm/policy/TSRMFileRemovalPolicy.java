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
 
import gov.lbl.srm.server.TSRMLocalFile;
import gov.lbl.srm.util.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

public abstract class TSRMFileRemovalPolicy {
    long _inventoryBytes = 0;

    public TSRMFileRemovalPolicy() {
	
    }
	
    public static final Comparator _ComparatorLRU = new Comparator() {
	    public int compare(Object o1, Object o2) {
		TSRMLocalFile f1 = (TSRMLocalFile) o1;
		TSRMLocalFile f2 = (TSRMLocalFile) o2;
		
		int result = f1.isYoungerThan(f2);
		if (result > 0) {
		    
		}  else if (result < 0) {
		    
		} else {			
		    // making sure files that has same stamps will both be added to the set
		    result = f1.getName().compareTo(f2.getName()); 
		}
		return result;
	    }
	};
    
    //public static void sortFiles(ArrayList input) {
    //		Collections.sort(input, _ComparatorLRU);
    //	}

    public abstract void addCandidate(TSRMLocalFile f);
    public abstract void fileRemoved(TSRMLocalFile f, long size);
    public abstract long removeCandidate(TSRMLocalFile f);
    public abstract long cleanAll();
    public abstract void cleanLimitedTo(long targetedSize);
    public abstract void setCurrentlyBusy(boolean state);

    public void increaseInventory(long b) {	
	_inventoryBytes += b;
	TSRMLog.debug(this.getClass(), null, "event=inventory+ bytes="+_inventoryBytes, null);
    }

    public void decreaseInventory(long b) {
	_inventoryBytes -= b;
	TSRMLog.debug(this.getClass(), null, "event=inventory- bytes="+_inventoryBytes, null);
    }

    public boolean mayAccomodate(long b) {
	return (_inventoryBytes >= b);
    }

    public static TSRMFileRemovalPolicy getDefaultPolicy() {
	String currPkg = TSRMFileRemovalPolicy.class.getPackage().getName();

	String defaultName = currPkg+".TSRMFileRemovalPolicyPreferred";
	try {
	    Class removalClass = Class.forName(defaultName);
	    java.lang.reflect.Constructor constructor = removalClass.getConstructor((java.lang.Class [])null);
	    TSRMFileRemovalPolicy defaultPolicy = (TSRMFileRemovalPolicy)(constructor.newInstance((java.lang.Object []) null));
	    TSRMUtil.startUpInfo("## loaded file removal policy: "+defaultPolicy);
	    return defaultPolicy;
	} catch (ClassNotFoundException e) {
	    TSRMUtil.startUpInfo(">>>>>>>.... no such class:"+defaultName);
	    //e.printStackTrace();
	} catch (Exception e) {
	    TSRMUtil.startUpInfo(">>>>>>>.... cannt load class:"+defaultName);
	    e.printStackTrace();
	}
	return new TSRMFileRemovalPolicyLRU();	    
    }
}

