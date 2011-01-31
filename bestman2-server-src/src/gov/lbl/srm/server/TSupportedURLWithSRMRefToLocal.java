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
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.storage.*;

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;

import javax.xml.rpc.Stub;
import java.net.URL;

//import org.apache.axis.message.addressing.Address;
//import org.apache.axis.message.addressing.EndpointReferenceType;

import EDU.oswego.cs.dl.util.concurrent.Mutex;


public class TSupportedURLWithSRMRefToLocal extends TSupportedURLWithSRM {
    //private String _putReqToken = null;
    
    ISRMLocalPath _localPath = null;

    public TSupportedURLWithSRMRefToLocal(TSURLInfo info, String ep, String sfn) {
	super(info, ep, sfn);	
	if (Config.isSpaceMgtDisabled()) {
	    throw new TSRMException("Cannt use local cache", false);
	}
    }
    
    public ISRMLocalPath getCorrespondingLocalPath() {
	if (_localPath == null) {
	    _localPath = TAccountManager.getLocalPath(this, null, TSRMPermission.Readable);
	}
	return _localPath;
    }

    public void setCorrespondingPath(ISRMLocalPath p) {
	_localPath = p;
    }

    public java.util.Vector ls(org.apache.axis.types.URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	if (_localPath == null) {
	    TSRMLog.debug(this.getClass(), null, "event=lsFailed", "reason=noLocalPathFound uri="+getURLString());
	    return null;
	}
	if (_localPath.isSurl()) {
	    return _localPath.ls(ftype, listingOp);
	}
	TSRMLog.debug(this.getClass(), null, "event=lsFailed", "reason=notAnSurl uri="+getURLString());
	return null;
    }

    public void checkSiteFileName(TAccount caller, TSRMPermission p) {
	String homedirRef = TAccountManager.getHomedirRef();
	int pos = _siteFileName.indexOf(homedirRef);

	String uidStr = caller.getID();
	if (pos > 1) {
	    return;
	}
	if (pos ==0) {
	    int frontSlashPos = getURLString().indexOf("/"+homedirRef);
	    if (frontSlashPos == -1) { 
		uidStr = "/"+caller.getID();
	    }
	} 
	_siteFileName = _siteFileName.replaceFirst(homedirRef, uidStr);	
	String urlStr = getURLString().replaceFirst(homedirRef+"/", uidStr+"/");

	// this replaces srm://**/?SFN to srm://**?SFN
	while (true) {
	    String tgt = "/?SFN";
	    pos = urlStr.indexOf(tgt);
	    if (pos == -1) {
		break;
	    } else {
		//urlStr = getURLString().replaceFirst(tgt, "?SFN");
		urlStr = urlStr.substring(0, pos)+urlStr.substring(pos+1);
	    }
	}

	try {	       	      
	    getSURLInfo().setSURL(new org.apache.axis.types.URI(urlStr));
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    throw new RuntimeException(e.getMessage()+" ref: "+urlStr);
	}

	_localPath = TAccountManager.getLocalPath(this, caller, p);
    }

    public boolean checkExistence() {
	if (getCorrespondingLocalPath() == null) {
	    return false;
	}	
	return true;
    }
    
    public boolean isCorrespondingTo(ISRMLocalPath localPath) {
	String lpath = localPath.getCanonicalPath();
	String sfn = getSiteFileName();
	
	//System.out.println("## lpath = ["+lpath+"]");
	//System.out.println("## sfn   = ["+sfn+"]");
	
	if (lpath.endsWith(sfn)) {
	    return true; 
	}
	
	return false;
    }

    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	ISRMLocalPath path = getCorrespondingLocalPath();
	if (path == null) {
	    return TSRMUtil.createReturnStatusInvalidPath("No such path.");
	}

	if (!user.checkPermission(path.getOwner(), p)) {
	    return TSRMUtil.createReturnStatusNotAuthorized(null);
	}
	return null;
    }

    
    public void setTrustedSize(long size) 
    {
	TSRMLog.debug(this.getClass(), null, "event=setTrustedSize size="+size, "action=ignored");
    }

    public long getTrustedSize(int recursiveLevel) {
	ISRMLocalPath p =  getCorrespondingLocalPath();

	if (p == null) {
	    return -1;
	} else if (p.isDir()) {
	    TSRMLocalDir d = (TSRMLocalDir)p;
	    return d.getCurrentSize(recursiveLevel);
	} else {
	    TSRMLocalFile f = (TSRMLocalFile)p;
	    return f.getCurrentSize();
	}
    }

    public boolean isLocal() {return true;}
    
    public boolean isLocalToThisSRM() {
	return true;
    }
        
    public ISRMTxfHandler downloadTo(TSRMLocalFile tgt) {
	if (tgt == getCorrespondingLocalPath()) {
	    TSRMLog.info(this.getClass(), null, "event=downloadTo action=none reason=tgt_is_self", null);
	    return null;
	}

	/* not good if localFile is on HPSS 
	TSURLInfo src = TSRMUtil.createTSURLInfo(getCorrespondingLocalFile().getURIString(SRMTransferProtocol.GSIFTP));
	TSupportedURL srcUrl = TSupportedURL.create(src);
	return srcUrl.downloadTo(tgt);
	*/
	if (isDir()) {
	    throw new TSRMException("Cannt download from dir to file.", false);
	}
	TSRMLocalFile src = (TSRMLocalFile)getCorrespondingLocalPath();
	
	src.getPhysicalLocation().getDevice().uploadTo(src, tgt);
	return null;
    }
    
    public ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred) {
        RuntimeException hi = new RuntimeException("not implemented");
	hi.printStackTrace();
	throw hi;
    }


    public ISRMTxfHandler uploadTo(URI uri, GSSCredential cred) {
	TSRMLog.info(this.getClass(), null, "event=uploadFromRemote", "result=notSupported");
	return null;
    }

    
    public void abortDownload() {
	
    }
    
    public String prepareDiskPathString(TSRMStorage.iSRMSpaceToken onBehalfOf) {	
	TAccount owner = onBehalfOf.getOwner();
	// make sure owner is as in the path, or has permission to write in there
	String[] pathArray = TSupportedURL.getPathInArray(this);
	
	TAccount acct = TAccountManager.getAccount(pathArray[0]);
	
	if (acct == null) {
	    throw new TSRMException("No such account =>["+pathArray[0]+"]", false);
	}
	
	if ((acct != owner) && (owner != null) && !owner.isSuperUser() && !acct.isSuperUser()) {
	    throw new TSRMException(acct.getID()+" cannt write in: "+owner.getID()+" 's directory", false);
	}
	
	/*
	if (owner != null) {
	    //return getSiteFileName();	    
	    return acct.getID()+"/"+pathArray[pathArray.length-1]+TSRMUtil.getNextRandomUnsignedInt();
	} else {
	    String result = getSiteFileName().replaceFirst("/"+pathArray[0]+"/", "");
	    RuntimeException ex = new RuntimeException("Oh look:"+result);
	    ex.printStackTrace();
	    return result;
	}
	*/
	//return acct.getID()+"/"+onBehalfOf.getID()+"-"+TSRMUtil.getNextRandomUnsignedInt() +"-"+ pathArray[pathArray.length-1];
	//return acct.getID()+"/"+onBehalfOf.getID()+"-"+TSRMUtil.getNextRandomUnsignedInt() +"/"+ pathArray[pathArray.length-1];
	return acct.getID()+"/"+TSRMUtil.generatePathStr(onBehalfOf.getID(), pathArray[pathArray.length-1]);
    }

    public boolean isDir() {
	if (_localPath != null) {
	    return _localPath.isDir();
	} 
	return false;
    }

    public void populateMe(TSRMSourceDir dir) {
	if (!isDir()) {
	    return;
	}
	dir.populate((TSRMLocalDir)_localPath);
    }

    
    public TReturnStatus mkdir(TAccount caller) {
	 // assuming the paths are relative to the user top dir
	String[] pathArray = TSupportedURL.getPathInArray(this);
	
	TSRMChangeDirStatus status = TAccount.changeDir(caller.getTopDir(), "mkdir()", pathArray, 1);
	TSRMLocalDir tgtDir = status.getDir();	   
	if (tgtDir != null) {
	    String subdirName = pathArray[pathArray.length-1];		
	    if ((pathArray.length == 1) || (tgtDir.getSubDir(subdirName) != null) || (tgtDir.getFile(subdirName) != null)) 
		{
		    return TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, 
						       "Cannt create directory: "+subdirName+", already exists");
		} 
	    tgtDir.createDir(subdirName, true);						
	} 
	return status.getStatus();
    }

    public TReturnStatus rmdir(TAccount caller, boolean recursive) {
	// assuming the paths are relative to the user top dir
	String[] pathArray = TSupportedURL.getPathInArray(this);
	    
	TSRMChangeDirStatus status = TAccount.changeDir(caller.getTopDir(), "rmdir", pathArray, 0);
	TSRMLocalDir tgtDir = status.getDir();	
	if (tgtDir != null) {
	    return tgtDir.delete(recursive, true);
	} else {
	    return status.getStatus();		    
	}
    }

    public TReturnStatus rmFile(TAccount caller) {
	String[] pathArray = TSupportedURL.getPathInArray(this);
	TAccount owner = TAccountManager.getAccount(pathArray[0]);
	if (owner == null) {
	    return TSRMUtil.createReturnStatusFailed("No such user:"+pathArray[0]);
	} else if (owner != caller) {
	    return TSRMUtil.createReturnStatusFailed("Cannt rm other user's files.");
	} else {
	    ISRMLocalPath path = getLocalPath();
	    if (path == null) {
		return TSRMUtil.createReturnStatusInvalidPath("No such path");
	    } else if (path.isDir()) {
		return TSRMUtil.createReturnStatusFailed("Cannt remove a dir. Call rmDir()");
	    } else {
		TSRMLocalFile f = (TSRMLocalFile)path;
		if (TSRMLog.getCacheLog() != null) {
		TSRMLog.getCacheLog().removeFile(f); 		
		}
		
		//TReturnStatus status = TSRMNameSpace.purge(getURLString(), true);
		TReturnStatus status = TSRMNameSpace.purge(f.getSiteURL().getSURL().toString(), true);
		f.detachFromParent();
		f.unsetTokenIndependant(false);
		return status;
		//dereference?
	    }
	}
    }    
}
 
