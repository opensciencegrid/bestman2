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

import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import org.apache.axis.types.URI;

public class TAccount {
    private String _uid;
    
    private HashMap _activeSpaceTokens = new HashMap();
    private Mutex   _spaceTokenAccessMutex = new Mutex();
    
    private Mutex _ridGenerateMutex = new Mutex();
    private int _ridCounter = 0;
    
    private HashMap _requestCollection = new HashMap();
    private Mutex _requestMutex = new Mutex();
    
    private TSRMLocalDir _top = null;	         

    public static final Comparator _tokenIDComparator = new Comparator() {
	    public int compare(Object o1, Object o2) {
		char c1 = ((String) o1).charAt(0);
		char c2 = ((String) o2).charAt(0);
		if (c1 != c2) {
		    return c1 - c2;
		}

		String t1 = ((String) o1).substring(2);
		String t2 = ((String) o2).substring(2);
		
	        int result = Integer.parseInt(t1) - Integer.parseInt(t2);
		
		return result;
	    }
	};
    
    public TAccount(String uid) {
	//_uid = markUp(uid);
	_uid = uid;
	_top = new TSRMLocalDir(null, _uid, true);
	_top.setOwner(this);

	if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().accountCreated(this);
	}

	/*
	try {
	    URI testMe = new URI("gsiftp://host.my.address/path/now");
	    TSRMContactRemoteSRM.makeGlobusCogCompatible(testMe);
	} catch (Exception e) {
	    TSRMLog.exception(TAccount.class, "details", e);
	}
	*/
    }
    
    public static String markUp(String uid, String authorizationID) {
	//return uid.replaceAll("/", "__").replaceAll(" ", "");
	// GSI already ensured that user is in the gridmap at this stage
	if (uid.equals("shared")) {
	    return uid;
	}

	return Config.getMappedID(uid, authorizationID);
    }
    
    public boolean isSuperUser() {
	return (this == TAccountManager._SUPER_USER);
    }
    
    public String getID() {
	return _uid;
    }
    
    public String getUserIDType() {
	return getID();
    }
    
    public TSRMLocalDir getTopDir() {
	return _top;
    }
    
    public String generateRequestToken() {
	if (TSRMUtil.acquireSync(_ridGenerateMutex)){
	    String nextRID = _uid +":"+_ridCounter;
	    _ridCounter ++;
	    TSRMUtil.releaseSync(_ridGenerateMutex);
	    return nextRID;
	}
	return null;
    }
    
    public TReturnStatus requestSpace(TTokenAttributes req) {
	TSRMSpaceType type = req.getTypeOfSpace();
	long reserveLimit = canReserveOn(type);
	if (reserveLimit > 0) {
	    if (reserveLimit < req.getGuaranteedSpaceBytes()) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_NO_USER_SPACE, "exceeded user quota.");
	    }
	}

	TSRMStorage.iSRMSpace spaceManager = TSRMStorage.getSpaceManager(type);
	
	TReturnStatus result=  spaceManager.createToken(this,req);
	return result;
    }
    
    public TSRMStorage.iSRMSpaceToken getToken(String tokenId) {
	if (TSRMUtil.acquireSync(_spaceTokenAccessMutex)) {
	    Object result = _activeSpaceTokens.get(tokenId);
	    TSRMUtil.releaseSync(_spaceTokenAccessMutex);
	    
	    if (result != null) {
		return (TSRMStorage.iSRMSpaceToken)result;
	    }
	} 
	return null;
    }
    
    public TUserRequest[] getMatchingReqs(String desc) {
	TUserRequest[] result = null;
	if (TSRMUtil.acquireSync(_requestMutex)) {
	    if (_requestCollection.size() > 0) {
		Vector v = new Vector();
		Object[] requests = _requestCollection.values().toArray();
		for (int i=0; i<requests.length; i++) {
		    TUserRequest r = (TUserRequest)(requests[i]);
		    if ((desc == null) || (r.matchesDesc(desc))) {
			v.add(r);
		    }
		}
		Collections.sort(v);
		if (v.size() > 0) {
		    result = new TUserRequest[v.size()];
		    v.copyInto(result);
		}
	    }
	    TSRMUtil.releaseSync(_requestMutex);
	}
	
	return result;
    }
    
    public String[] getMatchingTokens(String desc) {	
	String[] result = null;
	if (TSRMUtil.acquireSync(_spaceTokenAccessMutex)) {
	    if (_activeSpaceTokens.size() > 0) {
		Vector matchedTokens = new Vector();
		Iterator iter = _activeSpaceTokens.values().iterator();
		while (iter.hasNext()) {
		    TSRMStorage.iSRMSpaceToken curr = (TSRMStorage.iSRMSpaceToken)iter.next();
		    if ((desc == null) || (curr.getTokenAttributes().agreesWithDescription(desc))) {
			matchedTokens.add(curr.getTokenAttributes().getSpaceToken());
		    }
		}

		if (matchedTokens.size() > 0) {
		    result = new String[matchedTokens.size()];
		    matchedTokens.copyInto(result);
		    java.util.Arrays.sort(result, _tokenIDComparator);
		}
	    }
	    TSRMUtil.releaseSync(_spaceTokenAccessMutex);
	}
	return result;
    }
    
    // called from requestSpace()'s spaceManager.createToken()
    public void addToken(TSRMStorage.iSRMSpaceToken token) {
	if (TSRMUtil.acquireSync(_spaceTokenAccessMutex)) {
	    _activeSpaceTokens.put(token.getID(), token);
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().tokenCreated(token);
		}
	    TSRMUtil.releaseSync(_spaceTokenAccessMutex);
	}
    }
    
    private long canReserveOn(TSRMSpaceType spaceType) 
    {
	long quota =  spaceType.getUserQuota();
	if (quota <= 0) {
	    return -1;
	}

	int bytesCounter = 0;
	if (_activeSpaceTokens.size() > 0) {
	    Iterator iter = _activeSpaceTokens.values().iterator();
	    while (iter.hasNext()) {
		TSRMStorage.iSRMSpaceToken curr = (TSRMStorage.iSRMSpaceToken)iter.next();
		if ((curr.getType() == spaceType) && (!curr.isLifeTimeExpired())) {
		    bytesCounter += curr.getTokenAttributes().getGuaranteedSpaceBytes();		    
		}
	    }	
	}

	return quota-bytesCounter;
    }

    public void removeToken(TSRMStorage.iSRMSpaceToken token) {
	if (TSRMUtil.acquireSync(_spaceTokenAccessMutex)) {
	   	 
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().tokenRemoved(token);
		}
	    _activeSpaceTokens.remove(token.getID());
	    TSRMUtil.releaseSync(_spaceTokenAccessMutex);
	}
    }
    
    
    public void addRequest(TUserRequest r) {
	if (TSRMUtil.acquireSync(_requestMutex)) {
	    _requestCollection.put(r.getID(), r);
	    TSRMUtil.releaseSync(_requestMutex);
	}
    }
    
    public TUserRequest getRequest(String id) {
	if (TSRMUtil.acquireSync(_requestMutex)) {
	    TUserRequest result = (TUserRequest)(_requestCollection.get(id));
	    TSRMUtil.releaseSync(_requestMutex);
	    return result;
	}
	return null;
    }
    
    public TUserRequest getRequest(String id, Class objClass) {
	TUserRequest result = getRequest(id);
	if (result != null) {
	    if (objClass.isAssignableFrom(result.getClass())) {
		return result;
	    }
	}
	return null;
    }
    
    public TSURLReturnStatus[] releaseSuchSURLsInAllRequests(URI[] inputSurlArray, boolean willKeepSpace) {
	if (inputSurlArray == null) {
	    return null;
	}

	if (TSRMUtil.acquireSync(_requestMutex)) {
	    Vector result = new Vector();
	    
	    for (int i=0; i<inputSurlArray.length; i++) {
		URI curr = inputSurlArray[i];
		result.addAll(releaseSurl(curr));
	    }
	    TSRMUtil.releaseSync(_requestMutex);

	    TSURLReturnStatus[] resultArray = new TSURLReturnStatus[result.size()];
	    for (int i=0; i<result.size(); i++) {
		resultArray[i] = (TSURLReturnStatus)result.get(i);
	    }
	    return resultArray;
	}

	return null;
    }

    private Vector releaseSurl(URI curr) {
	if (TSupportedURLDeviceSpecific.isDeviceSpecific(curr)) {
	    Vector result = new Vector();
	    TSRMNameSpace.release(curr.toString());
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null)));
	    return result;
	}

	TSURLInfo n = TSRMUtil.createTSURLInfo(curr);
	
	ISRMLocalPath p = null;
	try {	    
	    p = TAccountManager.getLocalPath(n, this, TSRMPermission.Readable, true);
	} catch (TSRMException e) { 
	    Vector result = new Vector(1);
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(e)));
	    return result;
	} catch (RuntimeException e) {
	    Vector result = new Vector(1);
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(e)));
	    return result;
	}

	if (p == null) {
	    Vector result = new Vector(1);
	    result.add(TSRMUtil.createTSURLReturnStatus(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, null)));
	    return result;
	}   
	
	return p.releaseSurl();
    }

    
    public TReturnStatus makeDir(TSURLInfo fullDirURL) {
	String methodName = "createDir(TSURLInfo)";
	TSupportedURL url = TSupportedURL.createAndCheckSiteFileName(fullDirURL, this, TSRMPermission.Writable);
	
	url.useCredential(gov.lbl.srm.impl.TSRMService.gGetCredential("TAccount.makeDir()"));
	return url.mkdir(this);	 
    }

	
    public TReturnStatus rmDir(TSURLInfo fullDirURL, boolean recursive) {
	TSRMLog.debug(this.getClass(), null, "event=rmDir  surl=\""+fullDirURL.getSURL()+"\"", "recursive="+recursive);
	String methodName = "rmDir(TSURLInfo, boolean)";
	TSupportedURL url = TSupportedURL.createAndCheckSiteFileName(fullDirURL, this, TSRMPermission.Writable);
	
	url.useCredential(gov.lbl.srm.impl.TSRMService.gGetCredential("TAccount.rmDir()"));
	return url.rmdir(this, recursive);
    }
    
    public TReturnStatus rmFile(TSURLInfo curr) {
	try {
	    TSupportedURL url = TSupportedURL.createAndCheckSiteFileName(curr, this, TSRMPermission.Writable);
	    url.useCredential(gov.lbl.srm.impl.TSRMService.gGetCredential("TAccount.rmFile()"));
	    return url.rmFile(this);
	    /*
	    if (url.isProtocolSRM()) {
		return url.rmFile(this);
	    } else if (url.isDeviceSpecific()) {

	    } else {
		return TSRMUtil.createReturnStatusFailed("Not supporting removing this kind of url.");
	    }
	    */
	} catch (TSRMException e) {
	    TSRMLog.exception(this.getClass(), "details:", e);
	    return TSRMUtil.createReturnStatus(e);
	} catch (RuntimeException e) {
	    TSRMLog.exception(this.getClass(), "details:", e);
	    return TSRMUtil.createReturnStatus(e);
	}
    }      

    public static TSRMChangeDirStatus changeDir(TSRMLocalDir topDir, 
						String methodName, 
						String[] pathArray, 
						int backTrackTo) 
    {

	String currDirName = topDir.getName();
	int upTo = pathArray.length - backTrackTo;

	//if (!currDirName.equals(pathArray[0]) &&  !TAccountManager.refersToHomedir(pathArray[0])) {
	if (!currDirName.equals(pathArray[0]))  {
	    TSRMLog.error(TAccount.class, null, "method="+methodName+" action=changeDir result=failed", "reason=hasWrongTopDir ref="+pathArray[0]);
	    
	    return new TSRMChangeDirStatus(null, 	    
					   TSRMUtil.createReturnStatusNotAuthorized("No sniffing in other user's directory"));	    
										    
	}

	for (int i=1; i<upTo; i++) {
	    String curr = pathArray[i];
	    if (curr.length()== 0) {
		continue;
	    }
	    topDir = topDir.getSubDir(curr);

	    if (topDir == null) {
		TSRMLog.error(TAccount.class, null, "method="+methodName+" event=failed reason="+currDirName+"_has_no_subdir_named_"+curr, null);
			      
		String errMsg = TSRMUtil._DefNoSuchPath+":"+currDirName+"/"+curr;
		return new TSRMChangeDirStatus(null, TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, errMsg));   
	    }
	    currDirName = topDir.getCanonicalPath();		 
	}
	return new TSRMChangeDirStatus(topDir, TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
    }
   
    // returns error code
    public TReturnStatus checkPathIsLocalWithPermission(TSupportedURL url, TSRMPermission mod, boolean pathShallExist) {
	ISRMLocalPath p = url.getLocalPath();
	if (p == null) {
	    if (pathShallExist) {
		return TSRMUtil.createReturnStatusInvalidPath(TSRMUtil._DefNoSuchPath+" "+url.getURLString());	    
	    } else {
		return null;
	    }
	} 
	if ((p.getOwner() != null) && !p.getOwner().checkPermission(this, mod)) {
	    return TSRMUtil.createReturnStatusNoPermission("No permission to touch:"+url.getURLString());
	}
	return null;
    }


    public boolean checkPermission(TAccount user, TSRMPermission mod) {
	return TAccessControl.checkPermission(this, user, mod);
	/*
	if (user == null) { // ignore permission check
	    return true;
	}

	if (user == this) {
	    return true;
	}

	if (mod == TSRMPermission.Readable) && {
	    return true;
	}
	
	return false;
	*/
    }


    /*
    boolean allowsBrowsing(TAccount someUser) {
	if (this == TAccountManager._SUPER_USER) {
	    return true;
	}
	if (someUser == null) { // forced
	    return true;
	}
	if (someUser == this) {
	    return true;
	}       	
	return false;
    }
    */
	// 
	// just an initial try
	//
	public URI autoGenerateSURL() {
	       String filename = System.currentTimeMillis()+Thread.currentThread().getName();
	       
	       try {
		   URI uri = new URI(Config._wsdlEndPoint);
		   uri.setScheme(TSupportedURLWithSRM._DefProtocolStr);
		   uri.setPath(uri.getPath()+"?SFN=/"+_uid+"/"+filename);
		    
		   //surl.setValue(uri);
		   return uri;
	       } catch (URI.MalformedURIException e) {
		   // should not reach here		   
		   TSRMLog.exception(this.getClass(), "details", e);
		   return null;
	       }
	}
}
