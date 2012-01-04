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

import java.util.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;
import gov.lbl.srm.util.*;
import org.apache.axis.types.*;

public class TUserRequestLs extends TUserRequest {
    URI[] _inputPathArray = null;
    TSRMFileListingOption _listingOp = null;    
    TFileStorageType _ftype = null;

    TSRMRequestLs _lsReq = null;
    SrmLsResponse _result = null;
    
    public TUserRequestLs(TAccount owner, URI[] input, TSRMFileListingOption op, 
			  TFileStorageType ftype, ArrayOfTExtraInfo ssinfo) 
    {
	super(null, owner);
	decorateID("LS");

	_listingOp      = op;
	_ftype          = ftype;

	if (input != null) {
	    _inputPathArray = new URI[input.length];
	  
	    for (int i=0; i<input.length; i++) {
		_inputPathArray[i] = input[i];
	    }
	}
	setStorageSystemInfo(ssinfo);		

	_lsReq = new TSRMRequestLs(this);
	TSRMLog.debug(TUserRequestLs.class, null, "rid="+getID(), "0thSurl=\""+input[0].toString()+"\" state=queued");			      		      
    }
       
    public TFileStorageType getFileStorageType() {
	return _ftype;
    }

    public URI getNthInputArray(int i) {
	if (_inputPathArray == null) {
	    return null;
	}

	if (i >=_inputPathArray.length) {
	    return null;
	}
	return _inputPathArray[i];
    }

    public int getInputArraySize() {
	if (_inputPathArray == null) {
	    return 0;
	}

	return _inputPathArray.length;
    }

    public TSRMFileListingOption getListingOp() {
	return _listingOp;
    }

    public TRequestType getType() {
	return TRequestType.LS;
    }

    public boolean isDone() {
	if (_lsReq.isQueued() || _lsReq.isInProgress()) {
	    return false;
	}
	TSRMUtil.startUpInfo("## "+getID()+"  "+_lsReq.getReturnStatus().getStatusCode());
	return true;
    }

    public SrmLsResponse getResult() {
	return _result;
    }

    public void setResult(Vector pathList) {
	if (_result != null) {
	    return;
	}
	_result = new SrmLsResponse();
	TMetaDataPathDetail[] outputArray = new TMetaDataPathDetail[pathList.size()];
	pathList.toArray(outputArray);

    	_result.setDetails(TSRMUtil.convertToArray(outputArray));

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	reqSummary.collect(outputArray);
	
	_result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), "Ref:"+getID()));	   
	//_result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	TSRMLog.debug(this.getClass(), null, "rid="+getID()+" resultSize="+pathList.size(), "statusCode="+_result.getReturnStatus().getStatusCode());
    }

    public void setFailed(String errMsg) {
	if (_result != null) {
	    return;
	}
	_result = new SrmLsResponse();
	_result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, errMsg));
    }

    public void cleanMe() {
	getOwner().removeRequest(this);
	_lsReq.cleanMe();
	_lsReq = null;
    }
}

