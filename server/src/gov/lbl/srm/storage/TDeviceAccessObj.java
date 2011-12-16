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

public class TDeviceAccessObj {
    Object _obj = null;
    TDeviceAccessInfo _accessInfo = null;
    
    //    boolean _isDir = false;
    static int _DefDIR = 0;
    static int _DefFILE = 1;
    static int _DefUnknown = 2;
    static int _DefBrokenPath = 3;

    long _trustedSize = -1;
    long _timeStamp = -1;

    private int _pathType = TDeviceAccessObj._DefUnknown;

    boolean _doRecursiveRemove = false;
    boolean _autoDeleteParentDir = false;
	boolean _noChmod = false;
    String _urlPrefix = null;

    public TDeviceAccessObj(Object deviceSpecificObj, TDeviceAccessInfo accessInfo, boolean autoDeleteParent) {
	_obj = deviceSpecificObj;
	_accessInfo = accessInfo;
	_autoDeleteParentDir = autoDeleteParent;
    }

    public TDeviceAccessObj(Object deviceSpecificObj, TDeviceAccessInfo accessInfo) {
	_obj = deviceSpecificObj;
	_accessInfo = accessInfo;
    }

    public TDeviceAccessObj(Object deviceSpecificObj, TDeviceAccessInfo accessInfo, String prefix) {
	_obj = deviceSpecificObj;
	_accessInfo = accessInfo;
	_urlPrefix = prefix;
    }


	public void doNotTouch() {
		_noChmod = true;
	}

	public boolean cannotTouch() {
		return _noChmod;
	}
    public boolean doAutoDeleteParent() {
	return _autoDeleteParentDir;
    }

    public long getTrustedSize() {
	return _trustedSize;
    }

    public void setTrustedSize(long s, long timeStamp) {
	_trustedSize = s;
	_timeStamp = timeStamp;
    }

    public long getTimeStamp() {
	return _timeStamp;
    }

    public Object getObj() {
	return _obj;
    }
    
    public TDeviceAccessInfo getAccessInfo() {
	return _accessInfo;
    }
    
    public void setBrokenPath() {
	_pathType = TDeviceAccessObj._DefBrokenPath;
    }

    public void setIsDir() {
	//_isDir = true;
	_pathType = TDeviceAccessObj._DefDIR;
    }

    public void setIsFile() {
	_pathType = TDeviceAccessObj._DefFILE;
    }

    public boolean isDir() {
	return (_pathType == TDeviceAccessObj._DefDIR);
    }

    public boolean isFile() {
	return (_pathType == TDeviceAccessObj._DefFILE);
    }    

    public boolean isBrokenPath() {
	return (_pathType == TDeviceAccessObj._DefBrokenPath);
    }

    public boolean isTypeUnknown() {
	return (_pathType == TDeviceAccessObj._DefUnknown);
    }

    public boolean isRemoveRecursive() {
	return _doRecursiveRemove;
    }

    public void setRecursiveRemove() {
	_doRecursiveRemove = true;
    }

    public String generateMetaDataPath(String fullPath) {
	if (_urlPrefix == null) {
	    return fullPath;
	} else {
	    return _urlPrefix + fullPath;
	}
    }
}
