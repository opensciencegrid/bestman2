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

package gov.lbl.srm.server;

//import EDU.oswego.cs.dl.util.concurrent.*;

import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import java.util.*;
import java.util.concurrent.Semaphore;
 
public class TReschedulingThread extends Thread {
    TreeSet _delayedRequests = new TreeSet();
    
    TSRMMutex _collectionAccessMutex = new TSRMMutex();
    boolean _stopMe = false;
    Semaphore _semaphore = new Semaphore(1);

    public TReschedulingThread() {
	TSRMUtil.acquireSync(_semaphore);
    }

    public void add(TSRMRequest job) {
	if (TSRMUtil.acquireSync(_collectionAccessMutex)) {
	    int sizeWas = _delayedRequests.size();	    
	    /*boolean b = */_delayedRequests.add(job);	    
	    int sizeNow = _delayedRequests.size();

	    TSRMLog.debug(this.getClass(), null, "event=addJob desc="+job.description(), "sizeNow="+sizeNow);
	    if ((sizeWas == 0) && (sizeNow > 0)) {
		TSRMUtil.releaseSync(_semaphore);
	    }
		
	    TSRMUtil.releaseSync(_collectionAccessMutex);	    
	}        
    }

    public void run() {
	while (!_stopMe) {
	    if (_delayedRequests.size() == 0) {
		TSRMUtil.acquireSync(_semaphore);
	    }
	    checkCollection();
	}
    }    

    public void stopMe() {
	_stopMe = true;
    }

    public void checkCollection() {
	if (TSRMUtil.acquireSync(_collectionAccessMutex)) {
	    long delay = 0;
	    while (_delayedRequests.size() > 0 ) {	       
		TSRMRequest curr = (TSRMRequest)(_delayedRequests.first());
		delay =  curr.getDelay(System.currentTimeMillis());

		if (delay == 0) {
		    boolean b = _delayedRequests.remove(curr);
		    TSRMLog.info(this.getClass(), null, "event=remove "+curr.description()+" sizeNow="+_delayedRequests.size(), "isDone="+b);
		    if (b == false) {
			TSRMLog.debug(this.getClass(), null, "event=checkCollection", "item="+curr+" status=didnt_get_deleted.");
			TSRMUtil.sleep(1000);
			break;
		    } else {
			TSRMLog.debug(this.getClass(), null, "event=rescheduling", curr.description());
			TSRMServer._gProcessingUnit.submitJob(curr);
		    }
		} else {
		    break; // no need to check anymore, other requests are scheduled later than now.
		}
	    }
	    TSRMUtil.releaseSync(_collectionAccessMutex);

	    if (delay > 0) {
		TSRMLog.debug(this.getClass(), null, "event=reschedulerToWait", "delayMillis="+delay);
		TSRMUtil.sleep(delay);
	    }
	}	
    }
}
