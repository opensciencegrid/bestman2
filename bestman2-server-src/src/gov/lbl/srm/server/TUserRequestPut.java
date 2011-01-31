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
import org.apache.axis.types.URI;
import java.util.*;

public class TUserRequestPut extends TUserRequest {
    TOverwriteMode   	_overwriteMode = null;
    
    public TUserRequestPut(String userRequestDescription, TAccount owner) {
	super(userRequestDescription, owner);
	decorateID("PUT");
    }
    
    private TSRMRequestPut getRequest(URI turl) {
	return (TSRMRequestPut)(super.get(turl));
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
    
    public TPutRequestFileStatus[] add(SrmPrepareToPutRequest req) {
    	this.setRetryPeriod(req.getDesiredTotalRequestTime());	
	this.setStorageSystemInfo(req.getStorageSystemInfo());
    	this.setOverwriteMode(req.getOverwriteOption());

	TPutFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();    
	Vector fileStatusVector = new Vector(listOfFileReqs.length);

	for (int i=0; i<listOfFileReqs.length; i++) {	    
	    TPutFileRequest curr = listOfFileReqs[i];
	    try {
		if (!checkNewKey(curr.getTargetSURL())) {
		    continue;
		}
		TSRMFileInput input = new TSRMFileInput(req, curr, i);	    
		verifyConsistency(input.getSpaceType(), input.getSpaceToken());
		TPutRequestFileStatus status = this.addPutFileRequest(input);
		fileStatusVector.add(status);	     
	    } catch (TSRMException e) {
		TSRMLog.exception(this.getClass(), "details:", e);
		TSRMLog.debug(this.getClass(), null, "event=addPut action=rejected rid="+getID(), "tgt="+curr.getTargetSURL());
		TPutRequestFileStatus status = new TPutRequestFileStatus();
		status.setSURL(curr.getTargetSURL());
		status.setStatus(TSRMUtil.createReturnStatus(e));
		status.setFileSize(TSRMUtil.createTSizeInBytes(0));
		fileStatusVector.add(status);
	    } catch (RuntimeException e) {
		TSRMLog.exception(this.getClass(), "details:", e);
		TSRMLog.debug(this.getClass(), null, "event=addPut action=rejected rid="+getID(), "tgt="+curr.getTargetSURL());
		TPutRequestFileStatus status = new TPutRequestFileStatus();
		status.setSURL(curr.getTargetSURL());
		status.setStatus(TSRMUtil.createReturnStatus(e));
		status.setFileSize(TSRMUtil.createTSizeInBytes(0));
		fileStatusVector.add(status);
	    }
	}
    	
	if (fileStatusVector.size() == 0){
	    return null;
	}
	TPutRequestFileStatus[] fileStatusList = new TPutRequestFileStatus[fileStatusVector.size()];
	for (int i=0; i<fileStatusVector.size(); i++) {
	    fileStatusList[i] = (TPutRequestFileStatus)(fileStatusVector.get(i));
	}
	return fileStatusList;
    }

    private TSRMRequestPut createFileRequest(TSRMFileInput r) {
	if (r.getSURLInfo() == null) {
	    return new TSRMRequestPut(r, this);
	}
	
	TSupportedURL url = r.getSURLInfo();
	url.checkSFN(getOwner(), TSRMPermission.Readable);

	//url = TSupportedURL.createAndCheckSiteFileName(r.getSURLInfo(), getOwner(), TSRMPermission.Readable, false);

	if (url.isDeviceSpecific()  || url.isProtocolFILE() || url.isLocal()) {
	    return new TSRMRequestPut(r, this);
	}
	return new TSRMRequestPutToRemote(r, this);
    }

    private TPutRequestFileStatus addPutFileRequest(TSRMFileInput r) {	
	TPutRequestFileStatus result = new TPutRequestFileStatus();
	
	if (r.getSURLInfo() != null) {
	    //TSURL v = r.getToSURLInfo().getSURLOrStFN();
	    result.setSURL(r.getSURLInfo().getURI()); // a reference
	}
	result.setFileSize(TSRMUtil.createTSizeInBytes(0));
	try {
	    //TSRMRequestPut srmPutReq = new TSRMRequestPut(r, this);		
	    TSRMRequestPut srmPutReq = createFileRequest(r);
	    result.setStatus(srmPutReq.getReturnStatus());
	    
	    if (srmPutReq._tgtSite != null) { // _tgtSite may not be avail if error occurred
		if (result.getSURL() == null) {
		    result.setSURL(srmPutReq._tgtSite.getSourceURL().getURI());
		}
	    }
	    super.add(result.getSURL().toString(), srmPutReq);
	} catch (TSRMException e) {
	    TSRMLog.exception(TUserRequestPut.class, "details", e);
	    result.setStatus(TSRMUtil.createReturnStatus(e.getStatusCode(), e.getMessage()));
	} catch(RuntimeException e) {
	    TSRMLog.exception(TUserRequestPut.class, "details", e);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE,
							 e.getMessage()));
	}
	
	return result;
    }
    
    public Vector getStatusOfRequest(URI[] surlArray) {
	if (surlArray == null) {
	    /*
	    HashSet reqSet = new HashSet(_requestCollection.values());
	    Vector result = new Vector(reqSet.size());
	    
	    Iterator iter = reqSet.iterator();
	    while (iter.hasNext()) {
		TSRMRequestPut r = (TSRMRequestPut)(iter.next());
		result.add(getFileStatus(r, surl));
	    }
	    return result;
	    */
	    
	    Set keys = _requestCollection.keySet();	
	    Iterator iter = keys.iterator();
	    Vector result = new Vector(keys.size());

	    while (iter.hasNext()) {
		String curr = (String)(iter.next());		
		TSRMRequestPut r = (TSRMRequestPut)(_requestCollection.get(curr));		
		try {
		    result.add(getFileStatus(r, new URI(curr)));
		} catch (Exception e) {		    
		    TSRMLog.exception(TUserRequestPut.class, "details", e);
		}
	    }
	    return result;
	}
	
	if (surlArray.length == 0) {
	    return null;
	}
	
	Vector result = new Vector(surlArray.length);
	
	for (int i=0; i<surlArray.length; i++) {
	    URI curr = surlArray[i];
	    
	    TSRMRequestPut r = (TSRMRequestPut)(getRequest(curr));
	    
	    if (r != null) {		
		result.add(getFileStatus(r, curr));
	    } else {
		result.add(getFileStatusNotFound(curr));
	    }
	}
	return result;
    }
	
    private TPutRequestFileStatus getFileStatusNotFound(URI curr) {
	TPutRequestFileStatus s = new TPutRequestFileStatus();
	s.setStatus(TSRMUtil.createReturnStatusFailed("No such request."));
	
	s.setSURL(curr);
	s.setFileSize(TSRMUtil.createTSizeInBytes(0));
	return s;
    }

    public boolean showSizeInStatus(TReturnStatus status) {
	if ((status.getStatusCode() == TStatusCode.SRM_FILE_IN_CACHE) ||
	    (status.getStatusCode() == TStatusCode.SRM_SUCCESS) ||
	    (status.getStatusCode() == TStatusCode.SRM_FILE_PINNED))
	{
	    return true;
	}
	return false;
    }

    private TPutRequestFileStatus getFileStatus(TSRMRequestPut r, URI curr) {
	TPutRequestFileStatus s = new TPutRequestFileStatus();
	s.setStatus(r.getReturnStatusBulk());       
	s.setSURL(curr);	 
	s.setTransferURL(r.getTransferURL());
	if (showSizeInStatus(r.getReturnStatus())) {
	    s.setFileSize(TSRMUtil.createTSizeInBytes(r.getFileSize()));
	} else {
	    s.setFileSize(TSRMUtil.createTSizeInBytes(0));
	}
	//s.setEstimatedProcessingTime(TSRMUtil.createTLifeTimeInSeconds(r.getEstProcessingTimeInSeconds(), false));
	s.setEstimatedWaitTime(TSRMUtil.createTLifeTimeInSeconds(r.getEstWaitTimeInSeconds(), false));
	s.setRemainingPinLifetime(r.getRemainingTURLLifeTimeInSeconds());
	
	return s;
    }


    public TRequestType getType() {
	return TRequestType.PREPARE_TO_PUT;
    }
}
