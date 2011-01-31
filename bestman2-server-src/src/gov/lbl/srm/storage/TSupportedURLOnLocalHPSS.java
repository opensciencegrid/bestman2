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

import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.Permanent.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

public class TSupportedURLOnLocalHPSS extends TSupportedURLDeviceSpecific {
    public static final String _DefProtocolStr = "mss";
    private long _size = -2;
    
    public TSupportedURLOnLocalHPSS(TSURLInfo surlInfo) {
	super(surlInfo, TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent).getRefDevice(surlInfo));
	
	setAccessObj(THPSSDevice.getAccessObj(surlInfo));

	//setFileStorageType(TFileStorageType.PERMANENT);

	if (getAccessObj().getAccessInfo() == null) {
	    throw new TSRMException("No access info is provided for HPSS."+getURI(), false);
	}
    }       
    
    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    public static TSupportedURLOnLocalHPSS createMe(TSURLInfo info) {
	if (TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent) == null) {
	    throw new TSRMException("No support for HPSS.", false);
	}

	return new TSupportedURLOnLocalHPSS(info);  
    }
    
    public boolean checkExistence() {
	return true;
    }	
    
    public void setTrustedSize(long size) {
	_size = size;
    }

    public long getTrustedSize(int ignoredRecursiveLevel) {
	TSRMLog.debug(this.getClass(), "getTrustedSize", "ignoreRecursiveLevel="+ignoredRecursiveLevel, null);
	if (getAccessObj().getAccessInfo() == null) {
	    throw new TSRMException("No access info is provided for HPSS.", false);
	}
		
	if (_size < 0) {
	    _size = getAccessObj().getTrustedSize();
	    if (_size < 0) {
		_size = getDevice().getSize(getAccessObj());
	    }
	}
	
	return _size;
    }   

    public String getEffectivePath() {
	return (String)(getAccessObj().getObj());
    }
}
