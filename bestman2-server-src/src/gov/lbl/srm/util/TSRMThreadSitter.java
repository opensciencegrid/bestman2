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

import java.util.*;

//import gov.lbl.srm..*;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMThreadSitter implements Runnable{
    static TSRMThreadSitter _staticSitter = new TSRMThreadSitter();
    private ArrayList _contents = new ArrayList();

    Mutex _accessMutex = new Mutex();

    public static TSRMThreadSitter getInstance() {	
	return _staticSitter;
    }

    public void register(TSRMTimedTask candidate) {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return;
	}
	try {
	    _contents.add(candidate);
	    TSRMLog.info(this.getClass(), null, "event=register", candidate.toString());
	    if (_contents.size() == 1) {
		synchronized (this) {
		    notify();
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public void unregister(TSRMTimedTask candidate) {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    TSRMLog.debug(this.getClass(), null, "event=unregisterFailed", candidate.toString());
	    return;
	}

	try {
	    doUnregister(candidate);
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    private void doUnregister(TSRMTimedTask candidate) {
	_contents.remove(candidate);
	TSRMLog.info(this.getClass(), null, "event=unregister", candidate.toString());
    }

    private void work() {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return;
	}
	try {	
	    if (_contents.size() == 0) {
		return;
	    }
	    int i = 0; 
	    while (i >= 0) {
		TSRMLog.debug(this.getClass(), null, "event=checking", "size="+_contents.size());
		TSRMTimedTask curr = (TSRMTimedTask)(_contents.get(i));
		if (curr.isExpired()) {
		    doUnregister(curr);
		    curr.stopMe();
		} 

	        i = i-1;
	    }	
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public void run() {
	long napMillis = 5000; // 10 seconds;
	while (true) {
	    if (_contents.size() == 0) {
		synchronized (this) {
		    try {
			TSRMLog.info(this.getClass(), null, "event=wait", null);
			wait();
		    } catch (InterruptedException e) {
			e.printStackTrace();
			return;
		    }
		}
	    }
	    work();

	    TSRMUtil.sleep(napMillis);
	}
    }
}
