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
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;

import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

public class TSpaceTokenBinding extends TSRMTupleBinding {
    public Object entryToObject(TupleInput ti) {
	String tid = ti.readString();
	String deviceTag = ti.readString();
	long  gBytes = ti.readLong();
	String ownerID = ti.readString();
	long  lifetime = ti.readLong();
	long  creationTime = ti.readLong();
	String desc = ti.readString();
	
	TBasicDevice device = (TBasicDevice)(TSRMSleepyCatDbReader._devicesRead.get(deviceTag));
	if (device == null) {
	    RuntimeException ex = new RuntimeException("No device found with tag="+deviceTag);
	    ex.printStackTrace();
	    return null;
	}

	TSRMSpaceType spaceType = TSRMSpaceType.getSpaceType(tid);
		
	if (spaceType == null) {
	    inconsistencyFound();
	    return null; // no such space anymore!!
	}

	TAccount user = TAccountManager.getAccount(ownerID, true);
	if (user == null) {
	    inconsistencyFound();
	    return null;
	}

	TSRMUtil.startUpInfo("tid="+tid+" creationtime="+creationTime+", lifetime="+lifetime);
	if (TSRMCacheLog.isExpired(creationTime, lifetime)) {
	    TSRMUtil.startUpInfo(".... expired token detected: "+tid);
	    TSRMCacheLogSleepyCat log = (TSRMCacheLogSleepyCat)(TSRMLog.getCacheLog());
	    log.tokenExpired(tid);
	    return null;
	}

	TTokenAttributes tokenAttr = new TTokenAttributes(spaceType, gBytes);
	tokenAttr.setLifeTimeInSeconds(lifetime);
	tokenAttr.setTokenID(tid);

	if (desc.length() > 0) {
	    tokenAttr.setUserDesc(desc);
	}
	 
	TSRMStorage.iSRMSpaceToken token = device.getToken(tid);
		    
	if (token == null) {
	    token = device.accomodate(tokenAttr);
	    if (token != null) {
		token.setOwner(user);	
		if (lifetime > 0) {
		    token.setCreationTimeInMilliSeconds(creationTime);
		}
	    } else {
		TSRMUtil.startUpInfo("!!!Cannt accomodate token: "+tid);
		inconsistencyFound();
	    }
	}
	return token;
    }

    private void inconsistencyFound() {
	//System.out.println(".................... remove ME??");
    }

    public void objectToEntry(Object object, TupleOutput to) {
	TSRMStorage.iSRMSpaceToken token = (TSRMStorage.iSRMSpaceToken)object;

	String userDesc = token.getTokenAttributes().getUserDesc();
	if (userDesc != null) {
	    userDesc = " ["+userDesc+"]";
	} else {
	    userDesc = "";
	}

	to.writeString(token.getID());
	to.writeString(token.getHostDevice().getTag());
	to.writeLong(token.getTokenAttributes().getGuaranteedSpaceBytes());
	to.writeString(token.getOwner().getID());
	to.writeLong(token.getTokenAttributes().getLifeTimeInSeconds());
	to.writeLong(token.getCreationTimeInMilliSeconds());
	to.writeString(userDesc);
	TSRMLog.debug(TSpaceTokenBinding.class, null, "event=writesToken tid="+token.getID()+" lifetime="+token.getTokenAttributes().getLifeTimeInSeconds(), " createdTime="+token.getCreationTimeInMilliSeconds());
    }

    public static void writeMe(TSRMStorage.iSRMSpaceToken token, Database db) {
	TupleBinding tokenBinding = new TSpaceTokenBinding();
	
	setKey(token.getID());
	tokenBinding.objectToEntry(token, _data);

	writeData(db);	

    }   
}
