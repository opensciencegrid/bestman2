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

package gov.lbl.srm.storage;

import java.util.*;

//import com.sun.rsasign.d;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.server.*;

import gov.lbl.srm.policy.TSRMStoragePolicyDefault;


public abstract class TSpaceSkeleton implements TSRMStorage.iSRMSpace {
    private TSRMMultiDeviceHandler _deviceHandler = null;	
    
    public abstract boolean authorize(TAccount owner, TTokenAttributes requestDescription);
    public abstract TBasicDevice getRefDevice(TSURLInfo surlInfo);
    public abstract TSRMStorage.iSRMSpaceToken locateDefaultToken(TFileStorageType fileType);
    
    //
    //////////////////////////////////////////////////////////////////////////
    //
    // constructor & supporting functions
    //
    // wont pass empty list here from TSRMStorage, so wont check
    //
    //////////////////////////////////////////////////////////////////////////
    //
    public TSpaceSkeleton(Properties prop, String[] spaceSpecs) {
	_deviceHandler = new TSRMMultiDeviceHandler(prop, spaceSpecs);
    }
    
    public Vector getDeviceList() {
	return _deviceHandler.getList();
    }              
    
    public int getNumDevices() {
	return _deviceHandler.getSize();
    }
    
    public TBasicDevice getNthDevice(int n) {
	return _deviceHandler.getNthDevice(n);
    }
    
    public TBasicDevice getDevice(String tag) {
	int size = getNumDevices();
	for (int i=0; i<size; i++) {
	    TBasicDevice curr = getNthDevice(i);
	    //if (curr.getDescription().equalsIgnoreCase(desc)) {
	    if (curr.getTag().equalsIgnoreCase(tag)) {
		return curr;
	    }
	}
	return null;
    }
    
    public long getDefaultUserSpaceSizeInMB() {
	return _deviceHandler.getNthDevice(0).getPolicy().getDefaultUserSpaceSizeInMB();
    }

    /*
    public String getCurrUsageReport() {
	String report = "";
	for (int i=0; i<_deviceHandler.getSize(); i++) {		    
	    TBasicDevice device = _deviceHandler.getNthDevice(i);
	    report += device.getUsageReport()+"\t";
	}
	return report;
    }
    */
    
    /*
    public TSRMStorage.iSRMSpaceToken preAllocateToken(TTokenAttributes requestDescription) {
	for (int i=0; i<_deviceHandler.getSize(); i++) {		    
	    TBasicDevice device = _deviceHandler.getNthDevice(i);
	    TSRMStorage.iSRMSpaceToken token = device.accomodate(requestDescription);
	    if (token != null) {
		return token;
	    }  
	}
	
	// nothing is available, try compact devices
	for (int i=0; i<_deviceHandler.getSize(); i++) {
	    TBasicDevice device = _deviceHandler.getNthDevice(i);
	    device.compactDevice();
	    TSRMStorage.iSRMSpaceToken token = device.accomodate(requestDescription);
	    if (token != null) {
		return token;
	    }
	}
	return null;
    }
    */
    
    public TReturnStatus createToken(TAccount owner, TTokenAttributes requestDescription) {
	if (!authorize(owner, requestDescription)) {
	    return TSRMUtil.createReturnStatusNotAuthorized(null);
	}
	//requestDescription.print();
	return _deviceHandler.getBalancePolicy().createToken(owner, requestDescription);

	/*
	//
	// assuming now user can access all devices supported here
	//
	int i = 0;
	TStatusCode code = TStatusCode.SRM_SUCCESS;
	while (i < _deviceHandler.getSize()) {
	    TBasicDevice curr = _deviceHandler.getNthDevice(i);
	    code = curr.getPolicy().negociate(requestDescription);
	    if ((code == TStatusCode.SRM_LOWER_SPACE_GRANTED) ||
		(code == TStatusCode.SRM_SUCCESS)) 
	    {
		TSRMStorage.iSRMSpaceToken token = curr.accomodate(requestDescription);
		
		if (token == null) {
		    code = TStatusCode.SRM_NO_FREE_SPACE;				 
		} else {
		    token.setOwner(owner);
		    //owner.addToken(token);				
		}
		return TSRMUtil.createReturnStatus(code, null);
	    } else {
		// return TSRMUtil.createReturnStatus(code, null);
	    }		 
	    i++;
	}
	return TSRMUtil.createReturnStatus(code, null);		
	*/
    }
    
    public TSRMStorage.iSRMSpaceToken getToken(String tid) {
	for (int i=0; i<_deviceHandler.getSize(); i++) {
	    TBasicDevice device = _deviceHandler.getNthDevice(i);
	    TSRMStorage.iSRMSpaceToken token = device.getToken(tid);
	    if (token != null) {
		return token;
	    }
	}
	return null;
    }	  	        
}
 
