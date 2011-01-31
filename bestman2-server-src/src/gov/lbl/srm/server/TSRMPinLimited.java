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

//import org.apache.axis.types.URI;
import java.io.IOException;
//import java.util.HashMap;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

//
// with limited duration time
// 
public class TSRMPinLimited  extends TSRMPin  {
    long _durationInMilliSeconds;
    
    
    public TSRMPinLimited(TSRMRequest r, TSRMStorage.iSRMSpaceToken token) {
	super(r, token);
	setDuration();
    }

    boolean switchT(TSRMStorage.iSRMSpaceToken oldT, TSRMStorage.iSRMSpaceToken newT) {
	if (!super.switchT(oldT, newT)) {
	    return false;
	}
	
	//adjust lifetime according to the new space token
	adjustDuration();
	TSRMLog.debug(this.getClass(), null, "event=tokenSwitched Token="+_spaceToken.getID()+" durationSeconds="+_durationInMilliSeconds/1000,  _pinIssuer.description());
	return true;
    }

    void announce() {
	TSRMLog.debug(this.getClass(), null, "event=file-pinned Token="+_spaceToken.getID()+" durationSeconds="+_durationInMilliSeconds/1000,  _pinIssuer.description());	
    }
    
    boolean isExpired() {
	long now = System.currentTimeMillis();
	
	return (now > (_startTimeInMilliSeconds+_durationInMilliSeconds));
    }
    
    // can negociate with system about pin time here
    void setDuration() {
	if (TSRMUtil.acquireSync(_timeGuard)) {
	    _durationInMilliSeconds =  _pinIssuer.getProposedPinDuration();
	    adjustDuration();
	    TSRMUtil.releaseSync(_timeGuard);
	}
    }
    
    boolean adjustDuration() {
	//long spaceExpTime = _spaceToken.getExpirationTimeInMilliSeconds();
	long spaceExpTime = _spaceToken.getMaxFileLifetimeMillis();
	if (spaceExpTime < 0) {
	    return false;
	}

	if (spaceExpTime > 0) {
	    long now = System.currentTimeMillis();
	    if (now > spaceExpTime) {
		_durationInMilliSeconds = 0;
		TSRMLog.debug(this.getClass(), null, "event=adjustDuration comment=LT_of_space_was_reached", "token="+_spaceToken.getID()+" spaceExpiresAt="+spaceExpTime);	
		return true;
	    }
	} 
	
	long proposedExpTime = _startTimeInMilliSeconds+_durationInMilliSeconds;
	if (spaceExpTime > proposedExpTime) {
	    return false;
	}

	_durationInMilliSeconds = spaceExpTime - _startTimeInMilliSeconds;
	TSRMLog.debug(this.getClass(), null, "event=adjustDuration comment=spaceLT_will_reach_sonner", "token="+_spaceToken.getID()+" newDur(millis)="+_durationInMilliSeconds);
	return true;
    }	
    
    long getTimeRemainsInMilliSeconds() {
	long now = System.currentTimeMillis();
	if (!TSRMUtil.acquireSync(_timeGuard)) {
	    throw new TSRMException("Internal Error when accquiring Mutex", false);
	}
	long result = (_startTimeInMilliSeconds+_durationInMilliSeconds) - now;
	TSRMUtil.releaseSync(_timeGuard);
	
	if (result < 0) {
	    return 0;
	} 
	return result;
    }
    
    boolean extend(long newLifeTimeInMilliSecondsFromCallingTime) {
	TSRMLog.debug(this.getClass(), null, "event=extend askingLifetimeMillis="+newLifeTimeInMilliSecondsFromCallingTime, null);
	long before = _durationInMilliSeconds;
	
	long now = System.currentTimeMillis();
	long proposedExpirationTime = now + newLifeTimeInMilliSecondsFromCallingTime;
	
	if (!TSRMUtil.acquireSync(_timeGuard)) {
	    throw new TSRMException("Internal Error when accquiring Mutex", false);
	}

	_durationInMilliSeconds = proposedExpirationTime - this._startTimeInMilliSeconds;	 
	
	if (_durationInMilliSeconds < 0) {
	    _durationInMilliSeconds = 0;
	} else {
	    adjustDuration();
	}
	TSRMUtil.releaseSync(_timeGuard);
	
	_numOfExtentions ++;
	
	TSRMLog.debug(this.getClass(), null, "event=extend assignedMillis="+(_durationInMilliSeconds+_startTimeInMilliSeconds-now), null);
	return true; // no limit on extending lifetime.
    }
}
