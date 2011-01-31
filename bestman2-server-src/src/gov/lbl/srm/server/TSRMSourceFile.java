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

//import org.apache.axis.types.URI;

public class TSRMSourceFile extends TSRMSource {
    TSRMLocalFile _localResidence = null;
    long _knownBytes = -1;
    
    public TSRMSourceFile(TSURLInfo info) {
	super(info);
    }
    
    public TSRMSourceFile(TSupportedURL info) {
	super(info);
    }
    
    public static TSRMSourceFile create(TSupportedURL info) {
	if (info.isDeviceSpecific()) {
	    return new TSRMSourceFileUserOwned(info);
	} else if (info.isProtocolFILE()) {
	    return new TSRMSourceFileUserOwned(info);
	} else {
	    return new TSRMSourceFile(info);
	}
    }

    public void setKnownBytes(long size) {
	_knownBytes = size;
	getSourceURL().setTrustedSize(size);
	TSRMLog.debug(this.getClass(), null, getSourceURL().getURLString(), "hasKnownBytes="+size);
    }

    public long getKnownBytes() {
	return _knownBytes;
    }

    public long getTrustedSize() {
	if (_knownBytes <= 0) {	
	    _knownBytes = getSourceURL().getTrustedSize(0);	
	}
	return _knownBytes;
    }

    public void setLocalDestination(ISRMLocalPath p) {
	if (p.isDir()) {
	    //return; // maybe can create filename in the dir
	    throw new TSRMException("Cannt accept dir as destination for file.", false);
	}
	_localResidence = (TSRMLocalFile)p;
    }
    
    public void attachToLocalCache(TUserRequest requester,  long bytes) {
	if (bytes <= 0) {
	    if (_knownBytes > 0) {
		bytes = _knownBytes;
	    }
	}

	if (getToken() == null) {
	    return;
	}

	TDeviceAccessInfo accessInfo = getToken().getHostDevice().getAccessInfo(getToken(), requester);

	if (_localResidence == null) {
	    _localResidence = getToken().createFile(getSourceURL(), bytes, accessInfo);
	} else {
	    if (_localResidence.getToken() != getToken()) {
		if (bytes <= 0) {
		    getToken().findSpaceForFile(_localResidence, getToken().getUsableSize(getSourceURL()));	 
		} else {
		    getToken().findSpaceForFile(_localResidence, bytes);
		}		
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=attachToLocalCache", "action=no_needed token="+getToken().getID());
	    }
	    // physical location may vary depends on the source url, (even in the same token)
	    _localResidence.setPhysicalLocation(getSourceURL(), accessInfo);
	}
    }
    
    public ISRMLocalPath getLocalPath() {
	return _localResidence;
    }
    
    public void detachFromLocalCache() {
	if (getLocalDestination() != null){
	    getLocalDestination().unsetToken(false);
	}
	//detachingStagingAddress();
    }

    public TSRMLocalFile getLocalDestination() {
	return _localResidence;
	//return _localResidence.getCorrespondingURI();
    }			
    
    public boolean broadcast(RuntimeException e) {
	if (getLocalDestination() == null) {
	    return false;
	}
	return getLocalDestination().broadcast(e);
	//return true;
    }

    public boolean broadcast(TSRMException e) {
	if (getLocalDestination() == null) {
	    return false;
	}

	return getLocalDestination().broadcast(e);
    }


    public void receive(TSRMRequestPut req) { // user pushes to here
	//this.attachToLocalCache();
	if (getLocalDestination() == null) {
	    throw new RuntimeException("Internal Error, local target can not be created.");
	}
	
	getLocalDestination().receive(this.getSourceURL(), req);		
    }
	
    public void abortTransfer() {
	//getSourceURL().abortDownload();
	//getLocalDestination().abort(
	
	if (getLocalDestination() != null) {
	    if (getLocalDestination().getPhysicalLocation() != null) {
		getLocalDestination().getTxfFile().getPhysicalLocation().changeTransferState(false);
	    }
	}	
    }
	
    //
    // transfer the file to local address in token 
    //
    // cases: Remote/Local<HPSS/Disk> to <HPSS/Disk>
    // only Remote to Local<HPSS> needs staging.
    //
    public void transferMe(TSRMRequest r) {
	if (getLocalDestination() == null) {
	    throw new RuntimeException("Internal Error, local target can not be created.");
	}
	
	getLocalDestination().findSpaceInToken(getToken(), getTrustedSize());
	
	//if files are pinned, donnt think should allow any transfer (public token or not)
	//if files are being transferred, then donnt allow any txf either
	// only allows transfer when no active pin/transfer
	
	/*
	//if (getLocalDestination().getPhysicalLocation().isTransfering()) {
	if (getLocalDestination().getTxfState().isDownloading()) {
	    TSRMLog.info(this.getClass(), "file in txf", getSourceURL().getURLString(), null);
	    getLocalDestination().addConsumer(r);
	    return;
	} else if (getLocalDestination().getTxfState().doBlocking()) {
	    throw new TSRMExceptionFileBusy("file is used by other process. Will try again", true);
	}
	*/
	if (getLocalDestination().getTxfState().doBlocking()) {
	    throw new TSRMExceptionFileBusy("file is used by other process.", false);
	}

	TSRMLog.info(this.getClass(), null, "event=download rid="+r.getIdentifier(), "from="+getLocalDestination().getCanonicalPath());
	getLocalDestination().addConsumer(r);

	try {
	    getSourceURL().useCredential(r.getRequester().getGSSCredential());

	    getLocalDestination().download(getSourceURL());
	    
	    // will do pinning here for now until Dir issue with requests are solved
	    //
	    // pinning on behalf of userRequest r
	    //
	    if (getLocalDestination().pin()) {
		// update token after pin is setup, to avoid file being deleted btw updateToken and pin.
		getLocalDestination().updateToken(); 
	    }	    
	} catch (TSRMException e) {
	    throw e;
	} catch (RuntimeException e) {
	    throw e;
	} finally {
	    getLocalDestination().removeConsumer(r);
	}
    }
}

  
