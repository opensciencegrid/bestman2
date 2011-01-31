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

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;

import java.util.HashMap;

public class TDeviceAccessInfo {
    //private String _uid; use hashmap value!
    //private String _pwd; use hashmap value!

    static String _DefUID = "uid";
    static String _DefPWD = "pwd";
    
    ArrayOfTExtraInfo _stubObj = null;
    HashMap _inputInfo = null;    

    private void parse(TExtraInfo[] input) {
	if (input == null) {
	    return;
	}
	int size = input.length;
	for (int i=0; i<size; i++) {
	    TExtraInfo curr = input[i];
	    /*
	    if (curr.getKey().equals(_DefUID)) {
		_uid = curr.getValue();
	    } else if (curr.getKey().equals(_DefPWD)) {
		_pwd = curr.getValue();
	    } else {
		record(curr);
	    }
	    */
	    record(curr);
	}	
    }

    private void record(TExtraInfo curr) {
	if (_inputInfo == null) {
	    _inputInfo = new HashMap();
	}
	_inputInfo.put(curr.getKey(), curr.getValue());
	TSRMLog.info(this.getClass(), null, "event=addAccessInfo"+" key="+curr.getKey(), "val="+curr.getValue());
    }

    public HashMap getRecords() {
	return _inputInfo;
    }

    public ArrayOfTExtraInfo getStub() {
	if (_stubObj == null) {
	    _stubObj = gCreateStorageSystemInfo(getUid(), getPwd());
	}
	return _stubObj;
    }

    public TDeviceAccessInfo(TExtraInfo[] input) {
	parse(input);
    }

    public TDeviceAccessInfo(ArrayOfTExtraInfo userInput) {
	parse(userInput.getExtraInfoArray());
    }

    public void printMe() {
	TSRMUtil.startUpInfo("DAInfo uid="+getUid()+" pwd="+getPwd());
    }

    public static TDeviceAccessInfo gCreateDefault() {
	String proxy = TSRMUtil.getDefaultProxyStr();
	if (proxy == null) {
	    throw new TSRMException("Cannt find a proxy", false);
	}
	ArrayOfTExtraInfo ssinfo = TDeviceAccessInfo.gCreateStorageSystemInfo("defaultToSRM", proxy);	
	TDeviceAccessInfo defaultAccessInfo = new TDeviceAccessInfo(ssinfo);

	return defaultAccessInfo;
    }

    public static ArrayOfTExtraInfo gCreateStorageSystemInfo(String uid, String pwd) {
	TExtraInfo[] ssinfo = new TExtraInfo[2];
	ssinfo[0] = new TExtraInfo();
	ssinfo[0].setKey(_DefUID);
	if (uid == null) {
	    ssinfo[0].setValue("null");
	} else {
	    ssinfo[0].setValue(uid);
	}

	ssinfo[1] = new TExtraInfo();
	ssinfo[1].setKey(_DefPWD);
	if (pwd == null) {
	    ssinfo[1].setValue("null");
	} else {
	    ssinfo[1].setValue(pwd);
	}

	return TSRMUtil.convertToArray(ssinfo);
    }

    public String getUid() {
	//return _uid;
	if (_inputInfo == null) {
	    return null;
	}
	return (String)(_inputInfo.get(_DefUID));
    }

    public String getPwd() {
	//return _pwd;
	if (_inputInfo == null) {
	    return null;
	}
	return (String)(_inputInfo.get(_DefPWD));
    }
    
}
