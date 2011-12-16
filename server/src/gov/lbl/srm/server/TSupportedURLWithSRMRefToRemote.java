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
import gov.lbl.srm.client.SRMClient;
import gov.lbl.srm.impl.*;
import gov.lbl.srm.transfer.globus.*;

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;

import javax.xml.rpc.Stub;
import java.net.URL;

//import EDU.oswego.cs.dl.util.concurrent.Mutex;

//
// refers to remote SRM
//
public class TSupportedURLWithSRMRefToRemote extends TSupportedURLWithSRM {
    //private ISRMPortType _srmStub = null;
    private TSRMStubHolder _srmStubHolder = null;
    //private String _reqToken = null;
    private String _token = null;
    TMetaDataPathDetail[] _contents = null;
    protected long _trustedSize = -1; // save it so dont have to go get it all the time
    private TDirOption _dirOp = null;
    private Integer _lifetime = null;
    private TOverwriteMode _overwrite = TOverwriteMode.NEVER;
    private TFileStorageType _fileStorType = null;

    TSRMMutex _remoteContactMutex = new TSRMMutex();

    public TSupportedURLWithSRMRefToRemote(TSURLInfo info, String ep, String sfn) {
	super(info, ep, sfn);
	_srmStubHolder = new TSRMStubHolder(getEndPoint());
	TSRMLog.debug(this.getClass(), null, "event=created", "url=\""+info.getSURL().toString()+"\"");
    }
    
    public void setRelatedToken(String t) {
	_token = t;
    }
    
    public void setRelatedDirOp(TDirOption op) {
	_dirOp = op;
    }

    public void setFileStorageType(TFileStorageType f) {
	_fileStorType = f;
    }

    public boolean checkExistence() {
	return true;
    }
    
    public TSRMStubHolder getStubHolder() {
	return _srmStubHolder;
    }
    
    public void setTrustedSize(long size) {
	_trustedSize = size;
    }

    public long getTrustedSize(int ignoredRecursiveLevel) {
	try {
	    if (_trustedSize == -1) {
		if (_contents != null) {		    
		    if (!isDir()) {
			_trustedSize = _contents[0].getSize().longValue();
		    }
		    
		    //return -1;
		}		
	    }
	    return _trustedSize;
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "getTrustedSize() failed", e);
	    return -1;
	}
    }
    
    public boolean isLocalToThisSRM() {
	return false;
    }    
    
    public ISRMTxfHandler downloadTo(TSRMLocalFile tgt) {

	TSRMContactRemoteSRM srmContact = new TSRMContactRemoteSRM(getStubHolder(), getCredential());
	TSRMDownloadFromRemoteSRM txfObj = new TSRMDownloadFromRemoteSRM(srmContact, 
									 getSURLInfo(), null, tgt, getCredential());
	tgt.setTxfObj(txfObj);
	return txfObj;
    }
    
    public ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred) {
	TSRMContactRemoteSRM srmContact = new TSRMContactRemoteSRM(getStubHolder(), cred);
	TSRMUploadToRemoteSRM txfObj = new TSRMUploadToRemoteSRM(srmContact, ISRMTxfEndPoint.create(src), getSURLInfo(), _token, _lifetime, _dirOp, _overwrite, _fileStorType);
								 
	return txfObj;
    }

    public ISRMTxfHandler uploadFrom(TSupportedURL src, GSSCredential cred) {
	TSRMContactRemoteSRM srmContact = new TSRMContactRemoteSRM(getStubHolder(), cred);
	TSRMUploadToRemoteSRM txfObj = new TSRMUploadToRemoteSRM(srmContact, ISRMTxfEndPoint.create(src), getSURLInfo(), _token, _lifetime, _dirOp, _overwrite, _fileStorType);
								 
	return txfObj;
    }


    public ISRMTxfHandler uploadTo(URI uri, GSSCredential cred) {
	TSRMLog.info(this.getClass(), null, "event=uploadFromRemote", "result=notSupported");
	return null;
    }


    public void useLifetime(Integer dur) {
	_lifetime = dur;
    }
    
    public void setOverwriteMode(TOverwriteMode mode) {
	_overwrite = mode;
    }
    
    public void abortDownload() 
    {}

    public void populateMe(TSRMSourceDir dir) {
	if (_contents == null) {
	    isDir();
	}

	if (_contents == null) {
	    return;
	}

	if (isDir()) {
	    TMetaDataPathDetail first = _contents[0];
	    if (isSelfDescribing(first)) {
		if (first.getArrayOfSubPaths() != null) {
		    dir.populate(first.getArrayOfSubPaths().getPathDetailArray());
		}	    
	    } else {
		dir.populate(_contents);
	    }
	}
    }

    private boolean isSelfDescribing(TMetaDataPathDetail curr) {
	String homedirRef = TAccountManager.getHomedirRef();
	int hasTutar = getURLString().indexOf(homedirRef);

	if (hasTutar > 0) {
	    TSRMLog.debug(this.getClass(), null, "msg=assuming_urls_contains_~_are_selfdescribing.", null);
	    return true; 
	}
	int pos = getURLString().indexOf(curr.getPath());
	TSRMLog.debug(this.getClass(), null, "event=checkIsSelfDescribing pos="+pos+" url=\""+getURLString()+"\"", "refPath="+curr.getPath());
	if (pos >= 0) {
	    return true;
	}
	pos = curr.getPath().indexOf(getEffectivePath());
	TSRMLog.debug(this.getClass(), null, "event=checkAgain pos="+pos, "SFN="+getEffectivePath());

	if (pos >= 0) {
	    return true;
	}
	return false;
    }

    // we donn;t know if srmLs() returns contents of a dir
    // or dir first, i.e. if a dir "foo" contains "file0", "dir0"
    // donnt know if (file0, dir0) will be returned or
    // (foo, subpath=(file0, dir0)) will be returned
    // so guard against both possiblities.
    private boolean checkIsDir() {
	if ((_contents != null) && (_contents.length > 0)) {
	    TMetaDataPathDetail first = _contents[0];
	    TStatusCode code = first.getStatus().getStatusCode();
	    if (code != TStatusCode.SRM_SUCCESS) {
		throw new TSRMException("Error in path!"+code, false);
	    }
	    if (first.getType() == TFileType.DIRECTORY) {
		return true;
	    }

	    if (isSelfDescribing(first)) { // assuming this means first is this dir itself
		return (first.getType() == TFileType.DIRECTORY);
	    } else { // means the return is the contents of this directory.
		return true;
	    }
	} else {
	    throw new TSRMException("Cannt tell whether it is dir.", false);
	}
    }

    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	//return TSRMUtil.createReturnStatusInvalidPath("This path is not local."+getURLString());
	//System.out.println(">>>>>>>>>>>>>>>>>>>>>> always authorize");
	return null;
    }

    public boolean isDir() {		
	if (_contents != null) {
	    return checkIsDir();
	}	   
	
	String err = "BeStMan: Unable to list dir.";
	if (TSRMUtil.acquireSync(_remoteContactMutex)) {
	    try {
		TSRMContactRemoteSRM srmContact = new TSRMContactRemoteSRM(getStubHolder(), getCredential());
		
		SrmLsRequest r = new SrmLsRequest();

		r.setArrayOfSURLs(TSRMUtil.convertToArray(getURI()));
		//r.setAllLevelRecursive(new Boolean(true)); // CERN cannt handle this
		r.setStorageSystemInfo(getSURLInfo().getStorageSystemInfo());
		SrmLsResponse result = srmContact.ls(r);
	    
		TStatusCode status = result.getReturnStatus().getStatusCode();
		TSRMLog.debug(this.getClass(), null, "remoteCall=srmls status="+status.toString()+" exp="+result.getReturnStatus().getExplanation(), 
			      "url=\""+getURLString()+"\"");	
	  
		while (status == TStatusCode.SRM_REQUEST_INPROGRESS) {
		    TSRMUtil.sleep(60000);
		    result = srmContact.ls(r);
		    status = result.getReturnStatus().getStatusCode();
		    TSRMLog.debug(this.getClass(), null, "remoteCall=srmLs status="+status.toString(), "url=\""+getURLString()+"\"");	
		}

		if (status == TStatusCode.SRM_SUCCESS) {	    
		    TMetaDataPathDetail[] details = result.getDetails().getPathDetailArray();	    
		    _contents = details;
		    
		    return checkIsDir();
		} else {
		    //looks like a dir is invalid? we should return to user!!
		    
		    TSRMLog.debug(this.getClass(), null, "remoteCall=srmLs conclusion=\"got no info/or failed. Assuming false\"", null);
		    err += "remoteLs call status:"+status.toString();
		}
	    } catch (Exception e) {
		TSRMLog.exception(this.getClass(), "Error when calling srmLs()", e);
		err +=" Unable to call srmLs()";
	    } finally {
		TSRMUtil.releaseSync(_remoteContactMutex);
	    }	    
	    throw new TSRMException(err+getURLString(), false);	
	} else {
	    TSRMLog.debug(this.getClass(), null, "remoteCall=srmLs event=failed error=\"cannt lock mutex\"", null);
	    return false;
	}
    }

    public java.util.Vector ls(org.apache.axis.types.URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	TSRMLog.debug(this.getClass(), null, "event=lsFailed", "reason=surlIsNotLocal uri="+getURLString());
	return null;
    }
}

