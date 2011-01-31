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

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;

import java.util.*;
import org.apache.axis.types.URI;
import EDU.oswego.cs.dl.util.concurrent.Mutex;


public class TSRMNameSpace {
    static HashMap _allEntries = new HashMap();
    static Mutex _accessMutex = new Mutex();
    
    public TSRMNameSpace() {}

    public static void addEntry(String surl) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry result=failed reason=mutex_failure", null);
	    return;
	}

	try {	    
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) { 
		// need to be alerted?
	    } else {
		curr = new TSRMSURLMgr(surl);
		_allEntries.put(surl, curr);
		TSRMLog.debug(TSRMNameSpace.class, null, "event=surlEntry+", "surl=\""+surl+"\"");
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static boolean removeFile(TSRMLocalFile f) {
	if (f.getSourceURI() == null) {
	    return false;
	}
	String surl = f.getSourceURI().toString();
	if (f.isSurl()) {
	    surl = f.getSiteURL().getSURL().toString();
	}
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=removeFile result=failed reason=mutex_failure", null);
	    return false;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) { 
		return curr.removeInstance(f);
	    } else {
		TSRMLog.debug(TSRMNameSpace.class, null,"event=removeFile result=failed surl=\""+surl+"\"",null);
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
	return false;
    }

    public static void replaceEntry(String was, URI now) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=replaceEntry result=failed reason=mutex_failure", null);
	    return;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(was));
	    if (curr != null) { 		
		_allEntries.remove(was);
		curr.replaceSURL(now);
		_allEntries.put(now.toString(), curr);
		TSRMLog.debug(TSRMNameSpace.class, null, "event=replaceEntry result=replaced newSurl=\""+ now.toString()+"\"", "was=\""+was+"\"");
	    } else {
		TSRMLog.debug(TSRMNameSpace.class, null, "event=replaceEntry result=failed expectedSurl=\""+ now.toString()+"\"", "curr=\""+was+"\"");
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static void removeEntry(String surl) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=removeEntry result=failed reason=mutex_failure", null);
	    return;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) { 
		_allEntries.remove(surl);
		TSRMLog.debug(TSRMNameSpace.class, null, "event=surlEntry-", "surl=\""+surl+"\"");
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static void addEntry(String surl, TSRMLocalFile f, TSRMRequest req) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry2 result=failed reason=mutex_failure", null);
	    return;
	}

	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) { 
		curr.addInstance(f);
		curr.addRequest(req);
	    } else {
		TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry2 result=surl_not_found surl=\""+surl+"\"", "action=add_on_the_fly");
		curr = new TSRMSURLMgr(surl);
		_allEntries.put(surl, curr);
		TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry2_surlEntry+ surl=\""+surl+"\"", null);
		curr.addInstance(f);	
		curr.addRequest(req);	
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static void addEntry(String surl, TSRMRequest req) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry3 result=failed reason=mutex_failure", null);
	    return;
	}

	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) { 
		curr.addRequest(req);
	    } else {
		TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry3 result=surl_not_found surl=\""+surl+"\"", "action=add_on_the_fly");
		curr = new TSRMSURLMgr(surl);
		_allEntries.put(surl, curr);
		TSRMLog.debug(TSRMNameSpace.class, null, "event=addEntry2_surlEntry+ surl=\""+surl+"\"", null);
		curr.addRequest(req);		
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static TReturnStatus purge(String surl, boolean removeLastCopy) {
	TReturnStatus result = purge(surl, null, removeLastCopy);

	if (result.getStatusCode() == TStatusCode.SRM_SUCCESS) {
	    removeEntry(surl);
	}

	return result;
    }

    public static void disableReplicas(String surl, TSRMRequest caller) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=disableReplicas result=failed reason=mutex_failure", null);
	    return;
	}

	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr != null) {
		curr.disableReplicas(caller);
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static TReturnStatus purge(String surl, 
				      TSRMStorage.iSRMSpaceToken token, 
				      boolean removeLastCopy) 
    {
	TReturnStatus result = null;

	TSRMSURLMgr curr = null;
	if (!TSRMUtil.acquireSync(_accessMutex)){	
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=purge result=failed reason=mutex_failure", null);
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "internal error");
	}
	try {
	    curr = (TSRMSURLMgr)(_allEntries.get(surl));
	    if (curr == null) {
			result = TSRMUtil.createReturnStatusFailed("No such surl.["+surl+"]");
			return result;
	    } 
	   
	    if (removeLastCopy) {
		    curr.abortRelatedRequests();
		}
		result = curr.purgeFromToken(token, removeLastCopy);
	} finally {
		TSRMUtil.releaseSync(_accessMutex);
    }
	return result;
    }

    public static void release(String surl)
    {
	if (!TSRMUtil.acquireSync(_accessMutex)){	
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=release result=failed reason=mutex_failure", null);
	    return;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));

	    if (curr != null) { 
		curr.release();
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public static TFileLocality getLocality(String surl)
    {
	TFileLocality result = null;
	if (!TSRMUtil.acquireSync(_accessMutex)){	  
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=getLocality result=failed reason=mutex_failure", null);
	    return null;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));

	    if (curr != null) { 
		result = curr.getLocality();
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
	return result;
    }

    public static Object getPrimaryCopy(URI uri) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "mutex failure"); 
	}

	try {
	    TSRMSURLMgr mgr = (TSRMSURLMgr)(_allEntries.get(uri.toString()));
	    if (mgr != null) { 
		TSRMLocalFile f = mgr.getPrimaryCopy();
		if (f != null) {
		    return f;
		} else {
		    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "no record found.");
		}
	    } else {
		TSRMLog.debug(TSRMNameSpace.class, null, "event=getPrimaryCopy", "not found:"+uri.toString());
	    }

	} finally {
	    TSRMUtil.releaseSync(_accessMutex);	    
	}
	return TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "no record found!");
    }

    public static TAccessLatency getLatency(String surl)
    {
	TAccessLatency result = null;
	if (!TSRMUtil.acquireSync(_accessMutex)){	    
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=getLatency result=failed reason=mutex_failure", null);
	    return null;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(surl));

	    if (curr != null) { 
		result = curr.getLatency();
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
	return result;
    }

    public static boolean pinTURLIfExists(TSRMSource source,
					  TSRMStorage.iSRMSpaceToken token,
					  TSRMRequest req) 
    {
	boolean result = false;
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    TSRMLog.debug(TSRMNameSpace.class, null, "event=pinTURL result=failed reason=mutex_failure", null);
	    return false;
	}
	try {
	    TSRMSURLMgr curr = (TSRMSURLMgr)(_allEntries.get(source.getSourceURL().getURLString()));
	    if (curr != null) { 
		result = curr.pinIfExists(source, token, req);
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}

	TSRMLog.debug(TSRMNameSpace.class, null, "event=pinTURLIfExists result"+result, req.description());
	return result;
    }
    
    public static TSURLLifetimeReturnStatus pinSURL(URI curr, Integer newLifeTime, TAccount user)
    {
	TSURLLifetimeReturnStatus result = null;

	if (!TSRMUtil.acquireSync(_accessMutex)){
	    result = new TSURLLifetimeReturnStatus();
	    result.setSurl(curr);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "Internal Error!"));
	    return result;
	}

	try {
	    TSRMSURLMgr mgr = (TSRMSURLMgr)(_allEntries.get(curr.toString()));
	    if (mgr != null) { 
		result = (mgr.pinSurl(newLifeTime, user));
	    }  else {
		TSupportedURL marked = TSupportedURL.createAndCheckSiteFileName(TSRMUtil.createTSURLInfo(curr), user, TSRMPermission.Readable);
		mgr = (TSRMSURLMgr)(_allEntries.get(marked.getURLString()));		
		if (mgr == null) {
		    result = new TSURLLifetimeReturnStatus();
		    result.setSurl(curr);	
		    result.setStatus(TSRMUtil.createReturnStatusFailed("No such surl"+curr));
		} else {
		    result = mgr.pinSurl(newLifeTime, user);
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);	    
	}		
	
	result.setSurl(curr);
	return result;
    }
    
}
