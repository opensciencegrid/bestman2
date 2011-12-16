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
import gov.lbl.srm.policy.TDefaultSRMAccountPolicy;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import java.util.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TAccountManager {
	static private HashMap _allAccounts = new HashMap();
	static private TSRMMutex _accountAccessGuard = new TSRMMutex();        
        static String _DefHomeDirRefString = "~";

        public final static TAccount _SUPER_USER = createAccount("shared");

	public TAccountManager() 
	{}
	
        static public boolean refersToHomedir(String name) {
	    return name.equals(_DefHomeDirRefString);
	}

        static public String getHomedirRef() {
	    return _DefHomeDirRefString;
	}

        static public TAccount getAccount(String id) {
	    return getAccount(id, false);
	}

        static public void  cleanUpReqs() {
	    Iterator iter = _allAccounts.values().iterator();
	    while (iter.hasNext()) {
		TAccount curr = (TAccount)(iter.next());
		curr.cleanUp();
	    }
	}
	static public TAccount getAccount(String uid, boolean createIfNotIn) {
	    //String id = TAccount.markUp(uid);
	    if (uid == null) {
		return null;
	    }

	    Object found = null;		
	    
	    if (TSRMUtil.acquireSync(_accountAccessGuard)) {
		//found = _allAccounts.get(TAccount.markUp(id));
		found = _allAccounts.get(uid);
		
		if ((found == null) && createIfNotIn) {
		    found = tryCreatingAccount(uid);
		}
		
		TSRMUtil.releaseSync(_accountAccessGuard);		    
	    }
	    
	    if (found == null) {
		return null;
	    }
	    return (TAccount)found;
	}
	
    // assuming mutex is locked by caller
        static private TAccount tryCreatingAccount(String id) {
	    String currMethodName = "tryCreatingAccount";
	    
	    if (TDefaultSRMAccountPolicy.canAcceptNthNewUser(_allAccounts.size())) {
		TAccount account =  new TAccount(id);
		_allAccounts.put(account.getID(), account);		
		TSRMLog.debug(TAccountManager.class, null, "event=Account_created  id=["+id+"]", null);
		return account;
	    } else {
		TSRMLog.debug(TAccountManager.class, null, 
			     "event=Account_not_created client="+id, "reason=SRM_saturated");
		return null;
	    }
	}

        static public TAccount createAccount(String id) {		
	    TAccount result = null;
	    if (TSRMUtil.acquireSync(_accountAccessGuard)) {
		result = tryCreatingAccount(id);
		TSRMUtil.releaseSync(_accountAccessGuard);
	    } 
	    return result; 
	}
    
    public static ISRMLocalPath getLocalPath(TSURLInfo path, TAccount browsingUser, TSRMPermission p, boolean limitedToLocal) 
    {
	//TSupportedURL url = TSupportedURL.create(path);	
	TSupportedURL url = TSupportedURL.createAndCheckSiteFileName(path, browsingUser, p, limitedToLocal);

	if (limitedToLocal && !url.isLocal()) {
	    throw new TSRMException("Given URI:"+path.getSURL().toString()+" is not a user owned file.",false);
	}
	    
	String[] pathArray = TSupportedURL.getPathInArray(url);
	
	if (pathArray.length == 0) {
	    throw new TSRMException("No path is specified in:"+url.getURLString(), false);
	}
	return getLocalPath(pathArray, browsingUser, p);
    }
    
    public static ISRMLocalPath getLocalPath(TSupportedURL url, TAccount browsingUser, TSRMPermission p) 
    {
	String[] pathArray = TSupportedURL.getPathInArray(url);
	if (pathArray.length == 0) {
	    throw new TSRMException("No path is specified in:"+url.getURLString(), false);
	}
	return getLocalPath(pathArray, browsingUser, p);
    }
    
    private static ISRMLocalPath getLocalPath(String[] pathArray, TAccount browsingUser, TSRMPermission p) {		
	// assuming that pathArray[0] is the account info

	TAccount user = getAccount(pathArray[0]);
	if (user == null) {
	    //throw new TSRMExceptionNotAuthorized("No such user =>"+pathArray[0]);
	    throw new TSRMExceptionInvalidPath("No such user =>"+pathArray[0]);
	}
		
	if (!user.checkPermission(browsingUser, p)) {
	    throw new TSRMExceptionNotAuthorized("No permission.");
	}
	
	TSRMLocalDir tgtDir = TAccount.changeDir(user.getTopDir(), "getLocalPath", pathArray, 1).getDir();

	if (tgtDir != null) {
	    TSRMLog.debug(TAccountManager.class, null,"canonicalPath="+tgtDir.getCanonicalPath(), "lastEntry="+pathArray[pathArray.length-1] );			  

	    if (pathArray.length == 1) {
		return tgtDir;
	    } else {
		return tgtDir.getPath(pathArray[pathArray.length-1]);
	    }
	} else {
	    return null;
	}
	
    }
}

 

