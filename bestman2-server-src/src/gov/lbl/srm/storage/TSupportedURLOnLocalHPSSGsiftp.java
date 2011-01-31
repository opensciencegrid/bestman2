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

public class TSupportedURLOnLocalHPSSGsiftp extends TSupportedURLWithGSIFTP {
    private long _size = -2;

    String _urlString = null;
    TBasicDevice _device = null;

    ISRMLocalPath _localPath = null;

    public TSupportedURLOnLocalHPSSGsiftp(TSURLInfo surlInfo) {
	super(surlInfo);	
	_device = TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent).getRefDevice(surlInfo);
    }      
 
    public void setURLString(String urlStr) {
	TSRMLog.info(this.getClass(), null, "event=setUrlString", "value="+urlStr);
	_urlString = urlStr;
    }
    
    public boolean pickupIOToken(TSRMRequest r) {
	return r.pickupIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr);
    }

    public void dropOffIOToken(TSRMRequest r) {
        r.dropOffIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr);
    }

    public String getURLString() {
	if (_urlString != null) {
	    return _urlString;
	}
	return super.getURLString();
    }

    public boolean isDeviceSpecific() {
	return true;
    }   

    public TBasicDevice getDevice() {
	return _device;
    }
 
    public TSRMLocalFile getLocalFile() {

	if (getLocalPath() == null) {
	    return null;
	}
	if (getLocalPath().isDir()) {
	    return null;
	}

	return (TSRMLocalFile)(getLocalPath());
    }

    public void setLocalPath(ISRMLocalPath p) {
	_localPath = p;
    }

    public ISRMLocalPath getLocalPath() {
	return _localPath;
    }
}
