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
import gov.lbl.srm.util.*;
import java.util.Properties;

public class TSRMFileListingOption {
    Boolean _isAllLevelRecursiveObj;
    Integer _numOfLevelsObj;
    Boolean _doFulleDetailedListObj;
    Integer _outputCountObj; // 0 = everything counts
    Integer _outputOffsetObj;
    String  _uid = null;
    static int _DefDEFAULTCountAll = 9999999;
    boolean _showChecksum = false;

	public static boolean _dirLsOff = false;
	public static String _DefConfigEntryDirLsOff = "NoDirBrowsing";

	public static void handleDirLsOff(Properties prop) {
		_dirLsOff = Config.isTrue(prop,_DefConfigEntryDirLsOff);
		if (_dirLsOff) {
		   TSRMUtil.startUpInfo("[warning] srmLs() turns dir browsing off.");		
		}
	}

    public String checkParameters() {	
	if (_numOfLevelsObj != null) {
	    if (_numOfLevelsObj.intValue() < 0) {
		return "numLevel < 0.";
	    }
	}
	if (_outputCountObj != null) {
	    if (_outputCountObj.intValue() < 0) {
		return "outputCount < 0";
	    }
	}
	if (_outputOffsetObj != null) {
	    if (_outputOffsetObj.intValue() < 0) {
		return "outputOffset < 0";
	    }
	}
	
	return null;
    }

    public void setShowChecksum(boolean s) {
	_showChecksum = s;
    }
    public boolean getShowChecksum() {
	return _showChecksum;
    }

    public String getUid(){
	return _uid;
    }
	    
    public TSRMFileListingOption(SrmLsRequest req, String uid) {
	if (!_dirLsOff) {
		_isAllLevelRecursiveObj = req.getAllLevelRecursive();
		_numOfLevelsObj	= req.getNumOfLevels();
	} else {
		_numOfLevelsObj = new Integer(0);
		TSRMUtil.startUpInfo("[browsingDir=off], numLevel is forced to be 0");
	}
	_doFulleDetailedListObj   = req.getFullDetailedList();
	_outputCountObj = req.getCount();
	_outputOffsetObj = req.getOffset();

	_uid = uid;

    }

    public TSRMFileListingOption(Boolean isAllLevelRecursive, 
				 Integer numOfLevels,	
				 Boolean doFullDetailedList,
				 Integer outputCount,
				 Integer offset) 
    {
	_isAllLevelRecursiveObj = isAllLevelRecursive;
	_numOfLevelsObj = numOfLevels;
	_doFulleDetailedListObj = doFullDetailedList;
	_outputCountObj = outputCount;
	_outputOffsetObj = offset;
    }

    public int getRecursiveLevel() {
	int nLevels = 1;
	if (isAllLevelRecursive()) {
	    nLevels = -1;
	} else {
	    if (_numOfLevelsObj != null) {
		nLevels = _numOfLevelsObj.intValue();
	    }
	}
	
	return nLevels;
    }

    public int getOutputCount() {
	int nCount = _DefDEFAULTCountAll; 
	if ((_outputCountObj != null) && (_outputCountObj.intValue() >= 0)) {
	    nCount = _outputCountObj.intValue();
	}
	return nCount;
    }

    public int getOutputOffset() {
	int nOffset = 0;
	if ((_outputOffsetObj != null) && (_outputOffsetObj.intValue() >= 0)) {
	    nOffset = _outputOffsetObj.intValue();
	}
	return nOffset;
    }

    public void resetOffset() {
	if ((_outputOffsetObj != null) && (_outputOffsetObj.intValue() >= 0)) {
	    _outputOffsetObj = null;
	}
    }

    public void resetCount(int v) {
	if ((_outputCountObj != null) && (v >= 0)) {
	    _outputCountObj = new Integer(v);	    
	}
    }

    public boolean isDetailNeeded() {
	if (_doFulleDetailedListObj == null) {
	    return false;
	}

	if (_doFulleDetailedListObj.booleanValue() == true) {
	    return true;
	}

	return false;
    }

    public boolean isAllLevelRecursive() {
	if (_isAllLevelRecursiveObj == null) {
	    return false;
	}

	if (_isAllLevelRecursiveObj.booleanValue() == true) {
	    return true;
	}

	return false;
    }

    public void printMe() {
	TSRMLog.debug(TSRMFileListingOption.class, null, "event=lsOptions "+ 
		      "  recursiveLevel="+getRecursiveLevel()+" outputCount="+getOutputCount(),
		      "offset="+getOutputOffset()+" detail="+isDetailNeeded());
    }

    public boolean isEquivalent(TSRMFileListingOption op) {
	if (op.getRecursiveLevel() != getRecursiveLevel()) {
	    return false;
	}

	if (op.getOutputCount() != getOutputCount()) {
	    return false;
	}

	if (op.getOutputOffset() != getOutputOffset()) {
	    return false;
	}

	if (op.isDetailNeeded() ^ isDetailNeeded()) {
	    return false;
	}
	return true;
    }
}
