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

public class TSRMLsManager {
    private HashMap _lsReqCollection = new HashMap();
    //private HashMap _lsReqHistory = new HashMap();

    private TSRMMutex   _requestMutex    = new TSRMMutex();
    
    public String getExistingRID(URI[] input) {
	/*
	TUserRequestLs found = (TUserRequestLs)(_lsReqHistory.get(input[0].toString()));
	if (found != null) {
	    return found.getID();
	} else {
	    return null;
	}
	*/
	return null;
    }

    public TUserRequestLs add(TAccount user, 
			      URI[] inputPathArray, 
			      TSRMFileListingOption listingOp, 
			      TFileStorageType fileType,
			      ArrayOfTExtraInfo ssinfo)
    {
	String rid = getExistingRID(inputPathArray);

	if (rid != null) {
	    TUserRequestLs r = getRequest(rid);

	    if (r.getInputArraySize() == inputPathArray.length) {		
		if (r.getListingOp().isEquivalent(listingOp)) {		
		    boolean matches = true;
		    for (int i=0; i<inputPathArray.length; i++) {
			if (!inputPathArray[i].equals(r.getNthInputArray(i))) {
			    matches = false;
			    break;
			}
		    }
		    if (matches) {
			return r;	 
		    }
		}
	    }
	} 

	TUserRequestLs lsReq = new TUserRequestLs(user, inputPathArray, listingOp, fileType, ssinfo);
	addRequest(lsReq);

	return lsReq;
    }

    public void removeWhenFinished(TUserRequestLs r) {
	if (!r.isDone()) {
	    return;
	}
	TSRMLog.info(this.getClass(), null, "removingReqRid="+r.getID(), "colsize="+_lsReqCollection.size());
	if (TSRMUtil.acquireSync(_requestMutex)) {
	    _lsReqCollection.remove(r.getID());
	    r.cleanMe();
	    r = null;
	    TSRMUtil.releaseSync(_requestMutex);
	}	
	TSRMLog.info(this.getClass(), null, "removed", "colsize="+_lsReqCollection.size());
    }

    private void addRequest(TUserRequestLs r) {
	if (TSRMUtil.acquireSync(_requestMutex)) {
	    _lsReqCollection.put(r.getID(), r);

	    // also, add to history before lsStatus is in the wsdl
	    //_lsReqHistory.put(r.getInputArray()[0].toString(), r);

	    TSRMUtil.releaseSync(_requestMutex);
	}
    }

    public TUserRequestLs getRequest(String rid) {
	return (TUserRequestLs)(_lsReqCollection.get(rid));
    }
} 


