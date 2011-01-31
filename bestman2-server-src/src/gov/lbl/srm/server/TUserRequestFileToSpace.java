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
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;
//import org.ietf.jgss.GSSCredential;
 
import org.apache.axis.types.URI;

//import java.util.HashMap;
import java.util.*;

public class TUserRequestFileToSpace extends TUserRequest {
    public TUserRequestFileToSpace(String userRequestDescription, TAccount owner) {
	super(userRequestDescription, owner);
	decorateID("CHANGESPACE");
    }

    
    private TSRMRequestFileToSpace getRequest(URI surl) {
	return (TSRMRequestFileToSpace)(super.get(surl));
    }
    
    public TSURLReturnStatus addFileRequest(URI uri, TSRMStorage.iSRMSpaceToken token, int pos) {	
	String old = uri.toString(); 

	TSRMRequestFileToSpace srmReq = new TSRMRequestFileToSpace(uri, token, this, pos);
	
	TSURLReturnStatus result = getFileStatus(srmReq, uri);
	
	super.add(old, srmReq);
	
	return result;
    }

    public Vector getStatusOfRequest(URI[] surlArray) {

	if (surlArray == null) {
	    HashSet reqSet = new HashSet(_requestCollection.values());
	    Vector result = new Vector(reqSet.size());
	    
	    Iterator iter = reqSet.iterator();
	    while (iter.hasNext()) {
		TSRMRequestFileToSpace r = (TSRMRequestFileToSpace)(iter.next());
		result.add(getFileStatus(r, r.getURI()));
	    }
	    return result;
	}
	
	if (surlArray.length == 0) {
	    return null;
	}
	
	Vector result = new Vector(surlArray.length);
	
	for (int i=0; i<surlArray.length; i++) {
	    URI curr = surlArray[i];
	    TSRMRequestFileToSpace r = getRequest(curr);
	    
	    TSURLReturnStatus s = null;
	    if (r != null) {		
		s = getFileStatus(r, curr);
	    } else {
		s = getFileStatusNotFound(curr);
	    }

	    result.add(s);
	}
	return result;
    }
	

    private TSURLReturnStatus getFileStatusNotFound(URI curr) {
	TSURLReturnStatus s = new TSURLReturnStatus();
	s.setStatus(TSRMUtil.createReturnStatusFailed("No such request."));
	
	s.setSurl(curr);
	return s;
    }
    private TSURLReturnStatus getFileStatus(TSRMRequestFileToSpace r, URI curr) {
	TSURLReturnStatus s = new TSURLReturnStatus();
	s.setStatus(r.getReturnStatusBulk());	
	s.setSurl(curr);

	return s;
    }

    public TRequestType getType() {
	return TRequestType.CHANGE_SPACE_FOR_FILES;
    }
}
