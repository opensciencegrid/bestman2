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
import gov.lbl.srm.storage.TSRMStorage;

import gov.lbl.srm.util.*;

public abstract class TSRMSource {
    //private TSURLInfo _info;
    private TSupportedURL _info = null;
    private TSRMStorage.iSRMSpaceToken _tokenToStoreIn = null;
    
    public TSRMSource(TSupportedURL url) {
	_info = url;
    }
    
    public TSRMSource(TSURLInfo info) {
	_info = TSupportedURL.create(info);
    }
	
    public TSupportedURL getSourceURL() {
	return _info;
    }
	
    public void setToken(TSRMStorage.iSRMSpaceToken token) {
	_tokenToStoreIn = token;
    }
	
    public TSRMStorage.iSRMSpaceToken getToken() {
	return _tokenToStoreIn;
    }
	
    public boolean isDir() {
	return false;
    }

    public boolean isUserOwned() {
	return false;
    }
    
    public String asString() {
	return _info.getURLString();
    }    

    public abstract long getTrustedSize();

    public abstract void setLocalDestination(ISRMLocalPath p);
    
    public abstract void attachToLocalCache(TUserRequest requester, long bytes);
    public abstract void detachFromLocalCache();
    public abstract void transferMe(TSRMRequest r);
    public abstract void abortTransfer();
    public abstract ISRMLocalPath getLocalPath();
    public abstract boolean broadcast(TSRMException e);
    public abstract boolean broadcast(RuntimeException e);
}

 
