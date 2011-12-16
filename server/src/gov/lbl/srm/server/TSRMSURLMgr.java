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
import gov.lbl.srm.policy.TSRMGeneralEnforcement;

import java.util.*;
import org.apache.axis.types.URI;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;


public class TSRMSURLMgr {
    private Vector _copies = new Vector(); // sorted by token
    private TSRMMutex   _accessMutex = new TSRMMutex();
    private String  _surl = null;
    private Vector _requests=new Vector();


    public TSRMSURLMgr(String siteUrl) {
	_surl = siteUrl;
    }

    public void replaceSURL(URI uri) {
	_surl = uri.toString();

		
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return;
	}

	try {
	    for (int i=0; i<_copies.size(); i++) {
		TSRMLocalFile f = (TSRMLocalFile)(_copies.get(i));	
		if (!f.isSurl()) {
		    f.setSourceURI(uri);
		}
		if (TSRMLog.getCacheLog() != null) {
		TSRMLog.getCacheLog().addFile(f);
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}    	
	
    }

    public void removeRequest(TSRMRequest req) {
	if (req == null) {
	    return;
	}

	if (TSRMUtil.acquireSync(_accessMutex)){
	    try {
		if (_requests.contains(req)) {
		    _requests.remove(req);
		    TSRMLog.debug(this.getClass(), null, "event=RequestEntry- total="+_requests.size(), "surl=\""+_surl+"\"");
		}
	    } finally {
		TSRMUtil.releaseSync(_accessMutex);
	    }
	}
    }
	    
    public void addRequest(TSRMRequest req) {
	if (req == null) {
	    return;
	}

	if (TSRMUtil.acquireSync(_accessMutex)){
	    try {
		if (!_requests.contains(req)) {
		    _requests.add(req);
		    TSRMLog.debug(this.getClass(), null, "event=RequestEntry+ total="+_requests.size(), "surl=\""+_surl+"\"");
		}
	    } finally {
		TSRMUtil.releaseSync(_accessMutex);
	    }
	}
    }

    public void abortRelatedRequests() {
	if (TSRMUtil.acquireSync(_accessMutex)){
	    try {
		for (int i=0; i<_requests.size(); i++) {
		    TSRMRequest curr = (TSRMRequest)(_requests.get(i));
		    curr.abort("Abort implicitly by srmRm("+_surl+")", true);
		}
		_requests.removeAllElements();
		TSRMLog.debug(this.getClass(), null, "event=RequestEntriesAborted total="+_requests.size(), "surl=\""+_surl+"\"");
	    } finally {
		TSRMUtil.releaseSync(_accessMutex);
	    }
	}
    }

    public boolean removeInstance(TSRMLocalFile curr) {	
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return false;
	}
	try {
	    if (_copies.contains(curr)) {
		return removeFile(curr);
	    } 
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
	return false;
    }

    private boolean removeFile(TSRMLocalFile curr) {	
	TReturnStatus status = curr.remove();
	if (status.getStatusCode() != TStatusCode.SRM_SUCCESS) {
	    return false;
	}
	_copies.remove(curr);
	TSRMLog.debug(this.getClass(), null, "event=entry- total="+_copies.size(), "surl=\""+_surl+"\"");
	curr = null;
	return true;
    }

    public void addInstance(TSRMLocalFile curr) {
	if (TSRMUtil.acquireSync(_accessMutex)){
	    try {
		if (!_copies.contains(curr)) {
		    _copies.add(curr);
		    //TSRMLog.debug(this.getClass(), "entry +", _surl, "# of entries:"+_copies.size());
		    TSRMLog.debug(this.getClass(), null, "event=entry+ "+curr.stampPath()+" total="+_copies.size(), "surl=\""+_surl+"\"");
		} 
	    } finally {
		TSRMUtil.releaseSync(_accessMutex);
	    }
	}
    }

    public void disableReplicas(TSRMRequest caller) {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return;
	}
	try {
	    for (int i=0; i<_requests.size(); i++) {
		TSRMRequest curr = (TSRMRequest)(_requests.get(i));
		if (curr != caller) {
		    curr.abort("Abort implicitly by disableReplica()"+_surl+")", true);
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }
    
    public boolean isLastCopy() {	
	return isLastCopy(null);
    }

    public TSRMLocalFile getPrimaryCopy() {
	for (int i=0; i<_copies.size(); i++) {
	    TSRMLocalFile temp = (TSRMLocalFile)(_copies.get(i));		
	    if (temp.isSurl()) {
		return temp;		
	    }
	}
	TSRMLog.debug(this.getClass(), null, "event=getPrimaryCopy result=none",  "surl=\""+_surl+"\"");
	return null;
    }

    public TSURLLifetimeReturnStatus  pinSurl(Integer newLifeTime, TAccount user) {		
	TSURLLifetimeReturnStatus result = new TSURLLifetimeReturnStatus();
	//result.setSurl(_surl);

	if (_copies.size() == 0) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No files to pin."));
	    return result;
	}
	
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "locking mutex failed."));
	    return result;
	}

	try {
	    TSRMLocalFile f = getPrimaryCopy();

	    if (f == null) {
		result.setStatus(TSRMUtil.createReturnStatusFailed("Internal Error."));
	    } else if (!f.isValid()) {		
		result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_LIFETIME_EXPIRED,"No lifetime available!"+f.getCanonicalPath()));
	    } else {
	        if ((f.getOwner() == null) || (f.getPinManager() == null)) {
		    result.setStatus(TSRMUtil.createReturnStatusFailed("Not a srm managed file."));
		} else if (f.getOwner() != user) {		  
		    result.setStatus(TSRMUtil.createReturnStatusFailed("Owner is:"+f.getOwner().getID()+"  you are:"+user.getID()));
		} else if (f.getToken().getType() == TSRMSpaceType.Permanent) {
		    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "permanent file, no lifetime."));
		    result.setFileLifetime(new Integer(-1));
		} else if (f.getPinManager() != null) {			
		    TSRMPin pin = f.getPinManager().getPinOnSurl();
		    long newLifeTimeInSeconds = TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS;
		    if (newLifeTime!= null) {
			newLifeTimeInSeconds = newLifeTime.intValue();
		    }
		    if (pin != null) {			
			pin.extend(newLifeTimeInSeconds*1000);				   
			long remainningLT = pin.getTimeRemainsInMilliSeconds();
			if (remainningLT >= 0) {
			    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
			    int remainningLTInSeconds = (int)(remainningLT/1000);
			    result.setFileLifetime(new Integer(remainningLTInSeconds));
				if (TSRMLog.getCacheLog() != null) {
			    TSRMLog.getCacheLog().fileIsAdded(f, f.getCurrentSize(), f.getToken()); 
				}
			} else {
			    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "no limit"));
			    result.setFileLifetime(new Integer(-1));
			}
		    } else {
			if (!f.updateInheritedLifeTime(newLifeTimeInSeconds*1000)) {
			    result.setStatus(TSRMUtil.createReturnStatusFailed("No pin is found on this surl"));
			} else {
			    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "on inherited lifetime"));
			    result.setFileLifetime(new Integer((int)newLifeTimeInSeconds));
			}
		    }
		} else {
		    result.setStatus(TSRMUtil.createReturnStatusFailed("not pinned"));
		}
	    }	    
	} catch (Exception e) {
	  TSRMLog.exception(this.getClass(), "details", e);
	  result.setStatus(TSRMUtil.createReturnStatusFailed(e.getMessage()));
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}    	
	
	return result;
    }

    public TFileLocality getLocality() {
	boolean isOnline = false;
	boolean isNearline = false;
	for (int i=0; i<_copies.size(); i++) {
	    TSRMLocalFile f = (TSRMLocalFile)(_copies.get(i));	
	    if (f.getLocality() == TFileLocality.ONLINE) {
		isOnline = true;		
	    } else if (f.getLocality() == TFileLocality.NEARLINE) {
		isNearline = true;
	    }
	}

	if (isOnline && isNearline) {
	    return TFileLocality.ONLINE_AND_NEARLINE;
	} else if (isOnline) {
	    return TFileLocality.ONLINE;
	} else if (isNearline) {
	    return TFileLocality.NEARLINE;
	} else {
	    return TFileLocality.UNAVAILABLE;
	}
    }

    public TAccessLatency getLatency() {
	boolean isOnline = false;
	boolean isNearline = false;
	for (int i=0; i<_copies.size(); i++) {
	    TSRMLocalFile f = (TSRMLocalFile)(_copies.get(i));	
	    if (f.getLatency() == TAccessLatency.ONLINE) {
		isOnline = true;		
	    } else if (f.getLatency() == TAccessLatency.NEARLINE) {
		isNearline = true;
	    }
	}

	if (isOnline) {
	    return TAccessLatency.ONLINE;
	} else if (isNearline) {
	    return TAccessLatency.NEARLINE;
	} else {
	    return null;
	}
    }

    public void release() {
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return;
	}
	try {   
	    for (int i=0; i<_copies.size(); i++) {
		TSRMLocalFile f = (TSRMLocalFile)(_copies.get(i));
		if (f == null) {
		    continue;
		} else if (!f.isSurl()) {
		    TSRMLog.debug(this.getClass(), null, "event=release exp=release_surl_implies_turl_releas.", f.stampPath());
		    f.getPinManager().clearAllPins(true);
		} else {
		    TSRMLog.debug(this.getClass(), null, "event=release surl=\""+_surl+"\"", f.stampPath());
		    if (f.getPinManager() != null) {
			f.getPinManager().clearReadPins(true);
		    }
		}
	    }
	} catch (Exception e) {	       
	    TSRMLog.exception(TSRMSURLMgr.class, "details", e);	    
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public boolean pinIfExists(TSRMSource source,
			       TSRMStorage.iSRMSpaceToken token, TSRMRequest req) 
    {
	//System.out.println(".........................................pinIfExists? "+token+"  "+source);
	boolean result = false;
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return result;
	}
	try {
	    for (int i=0; i<_copies.size(); i++) {
		TSRMLocalFile f = (TSRMLocalFile)(_copies.get(i));

		if ((f != null) && (f.getToken() == token)) {
		    source.setLocalDestination(f);
		    f.pin(req, TStatusCode.SRM_FILE_PINNED);
		    result = true;
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}

	return result;
    }

    public boolean isLastCopy(TSRMStorage.iSRMSpaceToken token) {
	boolean result = false;
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return result;
	}
	try {
	    if (_copies.size() == 1) {
		if (token == null) {
		    result = true;
		}  else {
		    TSRMLocalFile f = (TSRMLocalFile)(_copies.get(0));
		    if (f.getToken() == token) {
			result = true;
		    }
		}
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
	return result;
    }

    public TReturnStatus purgeFromToken(TSRMStorage.iSRMSpaceToken token,
					boolean removeLastCopy) {
	if (token != null) {
	    TSRMLog.debug(this.getClass(), null, "event=purgeFromToken token="+token.getID(), "removeLastCopy="+removeLastCopy);
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=purgeFromToken token=null", "removeLastCopy="+removeLastCopy);
	}
	return clean(removeLastCopy, token);
    }

    public TReturnStatus removeAll() {
	return clean(true, null);
    }

    //
    // this function means, if there is one last copy, donnt do anything,
    // otherwise, removeAll
    //
    private TReturnStatus clean(boolean removeLastCopy, TSRMStorage.iSRMSpaceToken token) {
	if (!removeLastCopy && isLastCopy(token)) {	    
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_LAST_COPY, null);
	}

	int errorCounter = 0;
	if (!TSRMUtil.acquireSync(_accessMutex)){
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INTERNAL_ERROR, "failed to get mutex");
	}
	try {
	    int pos = _copies.size()-1;

	    while (pos >= 0) {
		TSRMLocalFile curr = (TSRMLocalFile)(_copies.get(pos));

		if ((token == null) || (curr.getToken() == token)) {
		    /*
		    if (curr.isReady()) {	       
			removeFile(curr);
		    } else {
			errorCounter ++;
		    }
		    */
		    removeFile(curr);
		}
		pos--;
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);	    
	}

	TSRMLog.debug(this.getClass(), null, "event=EntryRemoveAttemp total="+_copies.size(), "surl=\""+_surl+"\"");

	if (errorCounter > 0) {	    
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_BUSY, null);
	} else {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	}
	//setting the file to be unreachable? no more get requests shall succeed at this point ---> will see
    }
}


