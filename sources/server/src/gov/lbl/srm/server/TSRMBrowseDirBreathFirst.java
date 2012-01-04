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

import java.io.File;
 
import java.util.*;
import gov.lbl.srm.util.*;
 
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import gov.lbl.srm.storage.*;
import org.apache.axis.types.URI;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMBrowseDirBreathFirst {
    int _iRecursiveLevel;
    int _iCount;
    int _iOffset;
    TFileStorageType _fileStorageType;
    Vector _collectionOfDirToBrowse = new Vector();
    Vector _collectionOfResult = new Vector();
    boolean _doDetails;

    HashMap _metaDataCollection = new HashMap();
    
    TSRMBrowseDirBreathFirst(boolean doDetails,
			     int recursiveLevel,
			     int count,
			     int offset,
			     TFileStorageType fileStorageType) 
    {
	_doDetails = doDetails;
	_iRecursiveLevel = recursiveLevel;
	_iCount = count;
	_iOffset = offset;
	_fileStorageType = fileStorageType;

	if (_iCount == TSRMFileListingOption._DefDEFAULTCountAll) {
	    _iCount = 0;
	}
	TSRMLog.debug(TSRMBrowseDirBreathFirst.class, null, "event=init", "count="+_iCount+" offset="+offset);
    }
    
    void addDirToBrowse(TSRMLocalDir dir) {
	_collectionOfDirToBrowse.add(dir);
    }
    
    boolean isDone() {
	if (_collectionOfDirToBrowse.size()==0) {
	    return true;
	}
	return false;
    }


    boolean hasReachedOffsetLimit() {
	return (_iOffset == 0);
    }

    boolean hasReachedCountLimit() {
	if (_iCount < 0) {
	    return true;
	}
	return false;
    }

    void browse() {
	while (!isDone()) {
	    browseCurrent();
	}
    }
    
    Vector getResultCollection() {
	return _collectionOfResult; 
    }
    
    ArrayOfTMetaDataPathDetail getResultAsArray() {
	TMetaDataPathDetail[] result = new TMetaDataPathDetail[_collectionOfResult.size()];
	for (int i=0; i<result.length; i++) {
	    result[i] = (TMetaDataPathDetail)(_collectionOfResult.get(i));
	}
	
	return TSRMUtil.convertToArray(result);
    }
    
    boolean reachedLimit() {
	long size = 0;
	for (int i=0; i<_collectionOfResult.size(); i++) {
	    Object curr = _collectionOfResult.get(i);
	    size += (long)(curr.toString().length());
	}
	
	if (size > TSRMUtil.MAX_MSG_SIZE) {
	    return true;
	}
	return false;
    }
    
    Vector adjustWithOffset(Vector v) {

	if (_iOffset <= 0) {
	    return v;
	}
	
	if (_iOffset >= v.size()) {
	    _iOffset = _iOffset - v.size();
	    v.removeAllElements();
	    _iOffset = _iOffset - v.size();  
	    return v;
	}
	
	for (int i= _iOffset; i >=0; i--) {
	    v.removeElementAt(i);
	}
	_iOffset = 0;
	return v;
    }
    
    //
    // add number of results limited to _iCount (which is >= 0)
    //
    private void saveResults(Vector additions, int limit, TMetaDataPathDetail parentDir) {	   

	//TSRMUtil.startUpInfo(".............. TSRMBrowserDirBreathFirst.class add results, size= "+additions.size()+" limit="+limit+"  "+parentDir.getArrayOfSubPaths()+"  count="+_iCount);
	TSRMLog.info(this.getClass(), null, "event=saveResults", "size="+additions.size()+" limit="+limit+" count="+_iCount);
	
	int s = additions.size();
	if (limit < s) {
	    for (int i=s-1; i>=limit; i--) {
		additions.removeElementAt(i);
	    }
	}
		
	ArrayOfTMetaDataPathDetail subPathArray = parentDir.getArrayOfSubPaths();
	if (additions.size() > 0) {
	    if (subPathArray == null) {
		TMetaDataPathDetail[] m = new TMetaDataPathDetail[additions.size()];
		additions.toArray(m);
		parentDir.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
	    } else {
		TMetaDataPathDetail[] existingArray = subPathArray.getPathDetailArray();
		TMetaDataPathDetail[] m = new TMetaDataPathDetail[additions.size()+existingArray.length];
		for (int i=0; i< existingArray.length; i++) {
		    additions.add(existingArray[i]);
		}
		additions.toArray(m);
		parentDir.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
	    }
	    
	}

	if (_collectionOfResult.size() == 0) {
	    _collectionOfResult.add(parentDir);
	    //_iCount = _iCount -1;
	} 	  
    }
    
    private boolean addResults(Vector additions, TMetaDataPathDetail parentDir) {
	if (additions.size() == 0) {
	    return false;
	}

	int currCount = additions.size();
	if (_iCount > 0) {
	    if (currCount >= _iCount) {
		overflew(parentDir, additions);
		return true;
	    }
	}
	saveResults(additions, additions.size(), parentDir);
	_iCount = _iCount - additions.size();
	
	return false;
    }
    
    private void done() {
	_collectionOfDirToBrowse.clear();
    }
    
    private void cascade(TSRMLocalDir dir) {
	if (_iCount < 0) {
	    TSRMLocalDir p = dir.getParent();
	    while (p != null) {
		TMetaDataPathDetail parentDir = getMetaData(p);
		if (parentDir == null) {
		    return;
		}
		parentDir.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_TOO_MANY_RESULTS, null));		    
		p = p.getParent();
	    }
	}
    }
    
    private void overflew(TMetaDataPathDetail parentDir, Vector matched) {
	int diff = _iCount - matched.size();
	saveResults(matched, _iCount, parentDir);
	_iCount = diff;  // just to make it negative to indicate limit reached

	if (_iCount < 0) {
	    parentDir.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_TOO_MANY_RESULTS, null));
	}
	 
	 done();
    }

    private boolean browseBreath(TSRMLocalDir dir) {
	Vector matched = adjustWithOffset(dir.listFilesOfType(_fileStorageType, _doDetails));
	//TMetaDataPathDetail parentDir = dir.getMetaData(_doDetails);
	TMetaDataPathDetail parentDir = getMetaData(dir);
	if (_iCount > 0) {
	    //_iCount = _iCount -1; // the start entry of this dir.
	}
	
	if (matched.size() > 0) {
	    if (_iCount > 0) {
		if (matched.size() >= _iCount) {
		    overflew(parentDir, matched);
		    return true;
		}
	    }
	}
	     
	if (_fileStorageType != null) { // no dirs are included in the result			
	    // not done, so shall browse the next level
	    if (matched.size() > 0) {
		saveResults(matched, matched.size(), parentDir);
		_iCount = _iCount - matched.size();
	    }
	} else {// include dirs in the results too			
	    matched.addAll(adjustWithOffset(getSubDirDetailCollection(dir)));
	    boolean gottenEnoughResults = addResults(matched, parentDir);
	    if (gottenEnoughResults) {
		done();
		return true;
	    }
	}
	return false;
    }

    private void browseCurrent() {
	Vector temp = new Vector();
	for (int i=0; i<_collectionOfDirToBrowse.size(); i++) {
	    TSRMLocalDir dir = (TSRMLocalDir)(_collectionOfDirToBrowse.get(i));
	    if (dir.isSurl()) {		
		if (!browseBreath(dir)) {
		    temp.addAll(dir.getSubDirCollection());
		} else {
		    cascade(dir);
		}
	    }
	}
	
	if (_iRecursiveLevel > 0) {
	    _iRecursiveLevel --;
	} 
	if (_iRecursiveLevel == 0) {
	    done();
	    return;
	} 
	
	_collectionOfDirToBrowse.clear();
	_collectionOfDirToBrowse = temp;
    }
    
    public Vector getSubDirDetailCollection(TSRMLocalDir dir) {	    	    	    
	Object[] dirArray = dir.getSubDirCollection().toArray();
	Vector v = new Vector(dirArray.length);
	for (int i=0; i<dirArray.length; i++) {
	    TSRMLocalDir curr = (TSRMLocalDir)(dirArray[i]);
	    if (curr.isSurl()) {
		v.add(getMetaData(curr));
	    }
	}
	
	return v;
    }

    private TMetaDataPathDetail getMetaData(TSRMLocalDir curr) {
	Object detail =_metaDataCollection.get(curr);
	if (detail != null) {	    
	    return (TMetaDataPathDetail)detail;
	}

	TMetaDataPathDetail d = curr.getMetaData(_doDetails);
	_metaDataCollection.put(curr, d);
	return d;
    }
}
