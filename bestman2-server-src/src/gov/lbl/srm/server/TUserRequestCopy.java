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

import gov.lbl.srm.util.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import java.util.*;
import org.apache.axis.types.URI;

public class TUserRequestCopy extends TUserRequest {
    HashMap _collection = new HashMap();
    int _size = 0;
    TOverwriteMode   	_overwriteMode = null;
    TFileStorageType    _storType = null;
   
    public TUserRequestCopy(String userRequestDescription, TAccount owner) {
	super(userRequestDescription, owner);
	decorateID("COPY");
    }
    
    public void setTargetFileStorageType(TFileStorageType f) {
	_storType = f;
    }

    public TFileStorageType getTargetFileStorageType() {
	return _storType;
    }

    public void setOverwriteMode(TOverwriteMode m) {
	if (m != null) {
	    _overwriteMode = m;
	} else {
	    _overwriteMode = TOverwriteMode.NEVER;
	}
    }
    
    public TOverwriteMode getOverwriteMode() {
	return _overwriteMode;
    }	

    public Vector getStatusOfRequest(URI[] fromSurlArray, URI[] toSurlArray) {
	if ((fromSurlArray == null) || (toSurlArray == null)) {
	    return getAllStatuses();
	}

	if ((fromSurlArray.length == 0) || (toSurlArray.length==0)) {
	    return null;
	}
	
	if (fromSurlArray.length != toSurlArray.length) {
	    throw new TSRMException("from/to surl arrays are not matching", false);
	}       
	
	Vector result = new Vector(fromSurlArray.length);
	
	for (int i=0; i<fromSurlArray.length; i++) {
	    URI from = fromSurlArray[i];
	    URI to   = toSurlArray[i];
	    
	    TCopyRequestFileStatus s = getStatus(from, to);
	    if (s != null) {
		result.add(s);
	    }
	}
	return result;
    }
    
    public TCopyRequestFileStatus addCopyFileRequest(TSRMFileInputCopy r) {
	TCopyRequestFileStatus result = new TCopyRequestFileStatus();	    
	
	org.apache.axis.types.URI uri = new org.apache.axis.types.URI(r.getFromSURLInfo().getURI());	    
	result.setSourceSURL(uri);
	result.setFileSize(TSRMUtil.createTSizeInBytes(0));
	
	//result.setFromSURL(r.getFromSURLInfo().getSURLOrStFN());
	
	URI tsurl = new org.apache.axis.types.URI(r.getToSURLInfo().getURI());
	result.setTargetSURL(tsurl);
	//result.setToSURL(r.getToSURLInfo().getSURLOrStFN());
	
	try {
	    TSRMRequestCopy srmCopyReq = TSRMRequestCopy.create(r, this);
	    TSRMLog.debug(this.getClass(), null, "event=createdCopy ", srmCopyReq.description());	    
	    result.setStatus(srmCopyReq.getReturnStatus());	
	    if (!this.add(uri.toString(), tsurl.toString(), srmCopyReq)) {
		result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, "already had src="+uri.toString()+", tgt="+tsurl.toString()));
	    }
	} catch (TSRMException e) {
	    TSRMLog.exception(this.getClass(), "TUserRequestCopy::addCopyFileRequest()", e);
	    result.setStatus(TSRMUtil.createReturnStatus(e));
	} catch (RuntimeException e) {
	    TSRMLog.exception(this.getClass(), "TUserRequestCopy::addCopyFileRequest()", e);
	    result.setStatus(TSRMUtil.createReturnStatus(e));
	}
	
	//_getRequestCollection.put(r.getFromSURLInfo().getSURLOrStFN().getValue().toString(), srmGetReq);
	return result;
    }
    
    private boolean  add(String from, String to, TSRMRequestCopy r) {
	boolean added = true;
	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    HashMap existing = (HashMap)(_collection.get(from));

	    if (existing != null) {
		if (existing.get(to) != null) {
		    // already exists, wont add
		    added = false;
		} else {
		    existing.put(to, r);
		}
	    } else {
		HashMap map = new HashMap();
		map.put(to, r);
		_collection.put(from, map);
	    }
	    _size ++;
	    //r.setPosInReq(_size);
	    TSRMUtil.releaseSync(_requestCollectionMutex);
	}

	return added;
    }
    
    TSRMRequestCopy getRequest(URI from, URI to) {
	HashMap existing = (HashMap)(_collection.get(from.toString()));
	if (existing == null) {
	    return null;
	}
	return (TSRMRequestCopy)(existing.get(to.toString()));
    }
    
    public TRequestType getType() {
	return TRequestType.COPY;
    }
    
    public int getSize() {
	return _size;
    }

    public TRequestSummary getSummary() {
	TRequestSummary result = new TRequestSummary();
	result.setRequestType(this.getType());
	if (isSuspended()) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_REQUEST_SUSPENDED, null));
	    return result;
	}
	
	if (isSuspended()) {
	    return result;
	}
	
	int nQueued = 0;
	int nProgress = 0;
	int nFinished = 0;
	int nFailed = 0;
	
	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    HashSet reqSet = new HashSet(_collection.values());
	    result.setTotalNumFilesInRequest(new Integer(_size));

	    Iterator iter = reqSet.iterator();
	    while (iter.hasNext()) {
		HashMap subset = (HashMap)(iter.next());
		HashSet temp = new HashSet(subset.values());
		Iterator tempIter = temp.iterator();
		while (tempIter.hasNext()) {
		    TSRMRequestCopy r = (TSRMRequestCopy)(tempIter.next());
		    if (r.isQueued()) {
			nQueued++;
		    } else if (r.isFinished()) {
			nFinished++;
			if (r.isFailed()) {
			    nFailed ++;
			}
		    } else {
			nProgress++;
		    }
		}
	    }
	    TSRMUtil.releaseSync(_requestCollectionMutex);				  
	}
	
	result.setNumOfCompletedFiles(new Integer(nFinished));
	result.setNumOfWaitingFiles(new Integer(nProgress+nQueued));
	result.setNumOfFailedFiles(new Integer(nFailed));		

	result.setStatus(TSRMUtil.generateStatus(nFinished, nProgress+nQueued, nFailed));
	return result;
    }

    private Vector getAllStatuses() {
	Vector result = new Vector(_size);

	if (TSRMUtil.acquireSync(_requestCollectionMutex)) {
	    //HashSet reqSet = new HashSet(_collection.values());
	    Set keys = _collection.keySet();

	    Iterator iter = keys.iterator();
	    while (iter.hasNext()) {
		try {
		    URI from = new URI((String)(iter.next()));
		    
		    Set temp = ((HashMap)(_collection.get(from.toString()))).keySet();
		    Iterator tempIter = temp.iterator();
		    while (tempIter.hasNext()) {
			URI to = new URI((String)(tempIter.next()));
			TCopyRequestFileStatus s = getStatus(from, to);
			if (s != null) {
			    result.add(s);
			} else {
			    result.add(getStatusNotFound(from, to));
			}
		    }
		} catch (Exception e) {		    
		    TSRMLog.exception(TUserRequestCopy.class, "details", e);
		}
	    }
	    TSRMUtil.releaseSync(_requestCollectionMutex);				  
	}       
	
	return result;
    }

    private TCopyRequestFileStatus getStatusNotFound(URI from, URI to) {
	TCopyRequestFileStatus s = new TCopyRequestFileStatus();
	s.setSourceSURL(from);
	s.setTargetSURL(to);
	s.setStatus(TSRMUtil.createReturnStatusFailed("No such request."));
	s.setFileSize(TSRMUtil.createTSizeInBytes(0));

	return s;
    }

    private TCopyRequestFileStatus getStatus(URI from, URI to) {
	TSRMRequestCopy r = getRequest(from, to);
	    
	if (r != null) {
	    TCopyRequestFileStatus s = new TCopyRequestFileStatus();
	    s.setStatus(r.getReturnStatusBulk());
	    
	    s.setSourceSURL(from);
	    s.setTargetSURL(to);
	    //s.setTransferURL(r.getTransferURL());
	    if (showSizeInStatus(r.getReturnStatus())) {
		s.setFileSize(TSRMUtil.createTSizeInBytes(r.getFileSize()));
	    } else {
		s.setFileSize(TSRMUtil.createTSizeInBytes(0));
	    }
	    //s.setEstimatedProcessingTime(r.getEstProcessingTimeInSeconds());
	    s.setEstimatedWaitTime(TSRMUtil.createTLifeTimeInSeconds(r.getEstWaitTimeInSeconds(), false));
	    s.setRemainingFileLifetime(r.getRemainingPinTime());
	    return s;
	}  

	return null;
    }
}

