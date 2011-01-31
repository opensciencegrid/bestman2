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
 
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
//import gov.lbl.srm.policy.TSRMGeneralEnforcement;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;

import org.apache.axis.types.URI;
import java.io.IOException;
import java.util.*;
import EDU.oswego.cs.dl.util.concurrent.Mutex;


//
// 
public class TSRMPinManager  {
    TSRMLocalFile 	_pinTarget 		= null; 
    
    HashMap 		_pinCollection	= new HashMap();
    Mutex  			_pinMutex		= new Mutex();
    boolean			_isClosed		= false;
    long 			_firstPinIssuedTimeInMilliSeconds = -1;
	
    TSRMPinManager(TSRMLocalFile f) {
	_pinTarget = f;
    }
    
    boolean addPin(TSRMPin p) {
	boolean result = false;

	TSRMUtil.acquireSync(_pinMutex);

	try {
	    if (!_isClosed) {
		_pinCollection.put(p.getIssuer(), p);
		result = true;
		
		if (_firstPinIssuedTimeInMilliSeconds == -1) {
		    _firstPinIssuedTimeInMilliSeconds = p.getStartTimeInMilliSeconds();
		}
		p.announce();
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=addPin result=failed "+p.getIssuer().description(), _pinTarget.stampPath());
		p.getIssuer().retry("File is subject to delete before pin was issued.", TSRMException.DefDefaultRetryTime);
	    }
	} finally {	
	    TSRMUtil.releaseSync(_pinMutex);
	}
	
	return result;
    }

    public TSRMPin unpin(TSRMRequest r) {
	TSRMPin result = null;
	TSRMUtil.acquireSync(_pinMutex);
	
	_pinCollection.remove(r);
	TSRMUtil.releaseSync(_pinMutex);

	return result;
    }
    
    public TSRMPin getPin(TSRMRequest r) {
	TSRMPin result = null;
	TSRMUtil.acquireSync(_pinMutex);
	
	result = (TSRMPin)(_pinCollection.get(r));
	TSRMUtil.releaseSync(_pinMutex);

	return result;
    }
    
    public TSRMPin getPinOnSurl() {
	TSRMPin result = null;

	TSRMUtil.acquireSync(_pinMutex);

	try {
	    Iterator iter = _pinCollection.values().iterator();
	    while (iter.hasNext()) {
		TSRMPin p = (TSRMPin)(iter.next());
		if (!p.isReadPin()) {
		    if (!p.isExpired()) {
			result = p;
			break;
		    }
		}
	    }
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMPinManager.class, "details", e);	    
	} finally {
	    TSRMUtil.releaseSync(_pinMutex);
	}

	return result;
    }

    void clearReadPins(boolean doSetStatusRelease) {
	if (_isClosed) {
	    return;
	}
	
	if (!TSRMUtil.acquireSync(_pinMutex)) {
	    return;
	}

	try {
	    Iterator iter = _pinCollection.values().iterator();
	    Vector toRemove = new Vector();
	    while (iter.hasNext()) {
		TSRMPin p = (TSRMPin)(iter.next());
		
		if (p.isReadPin() && doSetStatusRelease) {		
		    if (p.getIssuer().getCurrentReturnStatus().getStatusCode() != TStatusCode.SRM_FILE_IN_CACHE) {
			p.getIssuer().setStatusReleased("system clears pin!");
		    }	       
		    p.getIssuer().getReturnStatus();
		    toRemove.add(p.getIssuer());
		}
	    }
	    for (int i=0; i<toRemove.size(); i++) {
		_pinCollection.remove(toRemove.get(i));
	    }

	} finally {
	    TSRMUtil.releaseSync(_pinMutex);
	}
    }

    void switchToken(TSRMStorage.iSRMSpaceToken oldT, TSRMStorage.iSRMSpaceToken newT) {
	if ((oldT == null) || (newT == null)) {
	    TSRMLog.debug(this.getClass(), null, "event=switchToken action=none oldT="+oldT, "newT="+newT);
	    return;
	}

	TSRMLog.debug(this.getClass(), null, "event=switchToken oldT="+oldT.getID(), "newT="+newT.getID());

	Iterator iter = _pinCollection.values().iterator();
	while (iter.hasNext()) {
	    TSRMPin p = (TSRMPin)(iter.next());
	    p.switchT(oldT, newT);
	}
    }

    void clearAllPins(boolean doSetStatusRelease) {
	if (!_isClosed) {
	    TSRMUtil.acquireSync(_pinMutex);
	}

	Iterator iter = _pinCollection.values().iterator();
	while (iter.hasNext()) {
	    TSRMPin p = (TSRMPin)(iter.next());

	    if (doSetStatusRelease) {		
		if (p.getIssuer().getCurrentReturnStatus().getStatusCode() != TStatusCode.SRM_FILE_IN_CACHE) {
		    p.getIssuer().setStatusReleased("system clears pin");
		}	       
		p.getIssuer().getReturnStatus();
	    }
	}
	_pinCollection.clear();

	if (!_isClosed) {
	    TSRMUtil.releaseSync(_pinMutex);
	}
    }
        
    void open(boolean contentIsDeleted) {
	//TSRMUtil.acquireSync(_pinMutex);	
	
	if (!contentIsDeleted) {
	    this._isClosed = false;
	}
	
	TSRMUtil.releaseSync(_pinMutex);
	TSRMLog.debug(this.getClass(), null, "event=open ",_pinTarget.stampPath());
    }
    
    boolean tryClose() {
	boolean result = false;
	
	TSRMUtil.acquireSync(_pinMutex);	
	if (doesActivePinExist()) {
	    TSRMLog.debug(this.getClass(), null, "event=closeFailed", _pinTarget.stampPath());
	    TSRMUtil.releaseSync(_pinMutex);
	    return false;
	} else {
	    this._isClosed = true;
	    TSRMLog.debug(this.getClass(), null,"event=closed", _pinTarget.stampPath());
	    result = true;
	    return true;
	}
	//TSRMUtil.releaseSync(_pinMutex);

	//return result;
    }

    public long getExpirationTimeInMilliSeconds() {
	long result = -1;
	 
	boolean needLock = !_isClosed;
	if (needLock) {
	    TSRMUtil.acquireSync(_pinMutex);
	}

	Iterator iter = _pinCollection.values().iterator();
	while (iter.hasNext()) {
	    TSRMPin p = (TSRMPin)(iter.next());
	    long curr = p.getTimeRemainsInMilliSeconds();
	    
	    if (result == -1) {
		result = curr;
	    } else if (result < curr) {
		result = curr;
	    }
	}

	if (needLock) {
	    TSRMUtil.releaseSync(_pinMutex);
	}

	if (result > -1) {	    
	    result += System.currentTimeMillis();
	}
	return result;
    }

    public long getLifeTimeRemainingInMilliSeconds() {
	long result = -1;
	
	boolean needLock = !_isClosed;
	if (needLock) {
	    TSRMUtil.acquireSync(_pinMutex);
	}

	Iterator iter = _pinCollection.values().iterator();
	while (iter.hasNext()) {
	    TSRMPin p = (TSRMPin)(iter.next());
	    long curr = p.getTimeRemainsInMilliSeconds();
	    
	    if (result == -1) {
		result = curr;
	    } else if (result < curr) {
		result = curr;
	    }
	}
	
	if (needLock) {
	    TSRMUtil.releaseSync(_pinMutex);
	}
	
	return result;
    }
	
    private boolean doesActivePinExist() {
	    // mutex locked by caller		
	Iterator iter = _pinCollection.values().iterator();
	while (iter.hasNext()) {
	    TSRMPin p = (TSRMPin)(iter.next());
	    if (!p.isExpired()) {
		return true;
	    }
	}
	return false;
    }
    
    boolean hasActivePin() {
	boolean result = false;
	
	TSRMUtil.acquireSync(_pinMutex);	
	result = doesActivePinExist();
	TSRMUtil.releaseSync(_pinMutex);
		
	return result;
    }

    void printMe() {
	TSRMUtil.startUpInfo("<<<<<<<PinManager, # of pins: "+_pinCollection.size());    
    }
}

 

