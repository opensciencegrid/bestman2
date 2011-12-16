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
//import org.ietf.jgss.GSSCredential;
 
import org.apache.axis.types.URI;

//import java.util.HashMap;
import java.util.*;

public class TUserRequestGet extends TUserRequest {
    public TUserRequestGet(String userRequestDescription, TAccount owner) {
	super(userRequestDescription, owner);
	decorateID("GET");
    }

    public TUserRequestGet(TAccount owner, String userRequestDescription) {
	super(userRequestDescription, owner);
	decorateID("BringONLINE");
    }
    
    private TSRMRequestGet getRequest(URI surl) {
	return (TSRMRequestGet)(super.get(surl));
    }
    
    public TGetRequestFileStatus addGetFileRequest(TSRMFileInput r) {	
	//##TGetRequestFileStatus result = new TGetRequestFileStatus();
	//##result.setSourceSURL(r.getURI());

	// we save the string here and use it when calling add()
	// because otherwise, it might get changed when replacing ~ in forming TSRMRequestGet()
	//
	String old = r.getOriginalURI().toString(); 

	TSRMRequestGet srmGetReq = new TSRMRequestGet(r, this);
	
	TGetRequestFileStatus result = getFileStatus(srmGetReq, r.getOriginalURI());
	//##result.setStatus(srmGetReq.getReturnStatus());
	
	super.add(old, srmGetReq);
	
	return result;
    }   
    
    public Vector getStatusOfRequest(URI[] surlArray) {
	if (surlArray == null) {
	    Set keyset = _requestCollection.keySet();
	    java.util.Iterator iter = keyset.iterator();
	    Vector result = new Vector(keyset.size());
	   
	    while (iter.hasNext()) {
		String key = (String)(iter.next());
		TSRMRequestGet r = (TSRMRequestGet)(_requestCollection.get(key));

		try {		   
		    result.add(getFileStatus(r, new URI(key)));
		} catch (Exception e) { // cannnt do anything when URI is invalid
		                        // not even return error, sicne TGetRequestFileStatus requires URI as surl
		    TSRMLog.exception(TUserRequestGet.class, "details", e);
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
	    TSRMRequestGet r = getRequest(curr);
	    
	    TGetRequestFileStatus s = null;
	    if (r != null) {		
		s = getFileStatus(r, curr);
	    } else {
		s = getFileStatusNotFound(curr);
	    }

	    result.add(s);
	}
	return result;
    }
	

    private TGetRequestFileStatus getFileStatusNotFound(URI curr) {
	TGetRequestFileStatus s = new TGetRequestFileStatus();
	s.setStatus(TSRMUtil.createReturnStatusInvalidPath("given path="+curr));
	
	s.setSourceSURL(curr);
	s.setFileSize(TSRMUtil.createTSizeInBytes(0));
	TSRMLog.error(this.getClass(), null, "event=error reason=\"file status not found.\"", "url=\""+curr+"\" rid="+getID());
	printContent();

	return s;
    }

    private TGetRequestFileStatus getFileStatus(TSRMRequestGet r, URI curr) {
	TGetRequestFileStatus s = new TGetRequestFileStatus();
	s.setStatus(r.getReturnStatusBulk());

	if (curr != null) {
	    s.setSourceSURL(curr);
	} else {
	    s.setSourceSURL(r.getInput().getOriginalURI());
	}
	s.setTransferURL(r.getTransferURL());

	if ((curr != null) && (s.getTransferURL() != null)) {
	    TSRMLog.debug(this.getClass(), null, "surl=\""+curr.toString()+"\"",  "rid="+getID()+" txfurl="+s.getTransferURL().toString());
	}

	if (showSizeInStatus(r.getReturnStatus())) {
	    s.setFileSize(TSRMUtil.createTSizeInBytes(r.getFileSize()));
	} else {
	    s.setFileSize(TSRMUtil.createTSizeInBytes(0));
	}
	//s.setEstimatedProcessingTime(r.getEstProcessingTimeInSeconds());
	
	s.setEstimatedWaitTime(TSRMUtil.createTLifeTimeInSeconds(r.getEstWaitTimeInSeconds(), false));
	s.setRemainingPinTime(r.getRemainingPinTime());

	return s;
    }

    public TRequestType getType() {
	return TRequestType.PREPARE_TO_GET;
    }

    public TReturnStatus checkStorageConsistency(String suggestedToken, TRetentionPolicyInfo suggestedInfo) {
	if ((suggestedInfo != null) && (suggestedInfo.getAccessLatency() != TAccessLatency.ONLINE)) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "ONLINE is required. Input was:"+suggestedInfo.getAccessLatency());
	}
	return super.checkStorageConsistency(suggestedToken, suggestedInfo);
    }
}
