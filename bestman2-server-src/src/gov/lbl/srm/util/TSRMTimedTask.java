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

import gov.lbl.srm.server.*;
//
// 
//
public class TSRMTimedTask {
    long _endTime;
    Thread _targetThread = null;
    String _desc = null;
    boolean _isStopped = false;

    public final static int _REMOTE_CALL_TIME_OUT_SECONDS = 2000;
    public final static int _GSIFTP_TIME_OUT_SECONDS = 1200;
    
    public TSRMTimedTask(long seconds) {
	createTimedTask(Thread.currentThread(), seconds);
    }

    private void createTimedTask(Thread t, long seconds) {
	long now = System.currentTimeMillis();
	_endTime = now + ((long)1000)*seconds;
	_targetThread = t;
	_desc = "targetThread="+t.getName()+" timeOutSeconds="+seconds;
	TSRMThreadSitter.getInstance().register(this);	
    }

	public void extend(long seconds) {
		long now = System.currentTimeMillis();
		_endTime = now + ((long)1000)*seconds;
		TSRMLog.info(this.getClass(), null, "event=resetStartTime", "dur="+seconds );
	}

    public String toString() {
	return _desc;
    }

    public synchronized void setFinished() {
	TSRMLog.info(this.getClass(), null, "event=setFinished", "tgtThread="+_targetThread);
	if (_targetThread == null) {
	    return;
	}
	_targetThread = null;
	TSRMThreadSitter.getInstance().unregister(this);
    }

	// cannt do synchronization here
	// dead loop to cause: srmgsiftp-thread is calling setFinished(), which waits a mutex for unregister in threadsitter,
	// while threadsitter work() holding the mutex and calling this instance's isExpired(), if synchronized, causes deadlock
    //public synchronized boolean isExpired() {	
	public boolean isExpired() {
	if (_targetThread == null) {
	    //return true;
	    return false; // 08/04/2010, this is needed for avoiding deadlock with the Threadsitter's run()
	}

	if (System.currentTimeMillis() > _endTime) {
	    return true;
	}
	return false;
    }

    public boolean isStopped() {
	return _isStopped;
    }

    public void alarm() {
	if (_isStopped) {
	    throw new TSRMException("Operation is timed out by SRM.", false);
	}
    }

    public synchronized void stopMe() {
	if (_isStopped) {
	    return;
	}

	TSRMLog.info(this.getClass(), null, "event=stopThread", null);
	_isStopped = true;
	if (_targetThread != null) {
	    if (Config._stopOnTimedOutThread) {
		_targetThread.stop();
	    } else {
		_targetThread.interrupt();
	    }
	}
	_targetThread = null;
    }
}
