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

package gov.lbl.srm.storage;

import gov.lbl.srm.server.*;
import gov.lbl.srm.util.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;
//import java.util.Vector;

public abstract class TSupportedURLDeviceSpecific extends TSupportedURL {
    private TBasicDevice _device               = null;
    //private TDeviceAccessInfo       _deviceAccessInfo     = null; // to access the device
    //private TSRMLocalFile           _correspondingFile = null;
    private ISRMLocalPath           _correspondingPath = null;
    //private TFileStorageType        _fileStorageType = null;
    
    public TSupportedURLDeviceSpecific(TSURLInfo info, TBasicDevice device) {
	super(info);
	_device = device;
	
	if (device == null) {
	    throw new TSRMException("No device found to support the source url.", false);
	}
	//_device.createPath(this, null, _deviceAccessInfo);
    }
    
    public abstract String getProtocol();  

    public TFileStorageType getFileStorageType() {
	return TFileStorageType.PERMANENT; //_fileStorageType;
    }

    /*
    public void setFileStorageType(TFileStorageType t) {
	_fileStorageType = t;
    }
    */
    
    public boolean isDeviceSpecific() {
	return true;
    }
    
    public void setLocalPath (ISRMLocalPath p) {
	_correspondingPath = p;
    }

    public java.util.Vector ls(URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	getLocalPath();
	if (_correspondingPath == null) {	    
	    return null;
	}
	return _correspondingPath.ls(ftype, listingOp);
    }

    public TSRMLocalFile getLocalFile() {
	if (isDir()) {
	    return null;
	}

	if (_correspondingPath == null) {
	    TSRMSourceFileUserOwned temp = new TSRMSourceFileUserOwned(this);
	    setLocalPath(temp.getLocalDestination());
	    if (getAccessObj().getTrustedSize() >= 0) {
		((TSRMLocalFile)(_correspondingPath))._isReady = true;
	    }
	}
	return (TSRMLocalFile)(_correspondingPath);
    }    

    
    public ISRMLocalPath getLocalPath() {
	if (isDir()) {
	    if (_correspondingPath == null) { 
			// use SUPER_USER as the owner. Not able to pass the actual user easily.
		TSRMSourceDirUserOwned temp = new TSRMSourceDirUserOwned(this, TAccountManager._SUPER_USER); 
		setLocalPath(temp.getLocalPath());
	    }
	} else {
	    getLocalFile();
	}
	return _correspondingPath;
    }

    /*
    // get the local path and do ls through the path
    public TMetaDataPathDetail[] ls() {
	return null;
    }
    */
    
    //public TReturnStatus moveTo(TSupportedURLDeviceSpecific tgt) {
    //return TSRMUtil.createReturnStatusFailed("Not supported: moving "+this.getURLString()+" to "+tgt.getURLString());
    //}

    public boolean isDir() {
	checkType();
	    
	if (getAccessObj() != null) {
	    return getAccessObj().isDir();
	} else {
	    return false;
	}
    }

    public void checkType() {
	if (getAccessObj().isTypeUnknown()) {
	    _device.setPathType(getAccessObj());
	}
    }

    public void populateTo(int recursiveLevel, TSRMLocalDirUserOwned dir) {
	Boolean allLevelRecursive = Boolean.valueOf(true);
	TSRMFileListingOption lsOption = new TSRMFileListingOption(allLevelRecursive, null, null, null, null);

	TMetaDataPathDetail result = _device.ls(getAccessObj(), lsOption);

	if (result == null) {
	    return;
	}

	if (result.getType() != TFileType.DIRECTORY) {
	    return;
	}

	TMetaDataPathDetail[] details = result.getArrayOfSubPaths().getPathDetailArray();
	if ((details == null) || (details.length == 0)) {
	    return;
	}

	dir.populate(recursiveLevel, details);
    }

    public void populateMe(TSRMSourceDir dir) {
	Boolean allLevelRecursive = Boolean.valueOf(true);
	TSRMFileListingOption lsOption = new TSRMFileListingOption(allLevelRecursive, null, null, null, null);

	TMetaDataPathDetail result = _device.ls(getAccessObj(), lsOption);

	if (result == null) {
	    return;
	}

	if (result.getType() != TFileType.DIRECTORY) {
	    return;
	}

	if (result.getArrayOfSubPaths() == null) {
	    return;
	}

	TMetaDataPathDetail[] details = result.getArrayOfSubPaths().getPathDetailArray();
	if ((details == null) || (details.length == 0)) {
	    return;
	}

	dir.populate(details);
    }

    public TBasicDevice getDevice() {
	return _device;
    }
    
    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	// need to check ACL, will do it later
	return null;
    }
    
    public ISRMTxfHandler downloadTo(TSRMLocalFile tgt) {
	if (tgt.getFileStorageType() == null) {
	    TSRMLog.debug(this.getClass(), "downloadTo", "event=failed reason=no_storage_type tgt="+tgt.getCanonicalPath(),null);
	    return null;
	}
	if (tgt.getFileStorageType() == TFileStorageType.VOLATILE) {
	    _device.toDisk(getAccessObj(), tgt);
	} else if ((tgt.getToken() != null) && tgt.getToken().getHostDevice() == _device) {	    
	    _device.localCopy(getLocalFile(), tgt); 
	} else {
	    throw new TSRMException("Not supporting transferring device specific surl to non volatile space yet.", false);
	}
	return null;
    }
    
    public ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred) {
	TSRMLog.debug(this.getClass(), "uploadFrom(local)", "event=failed reason=is_not_supported.", null);
	return null;
    }

    public ISRMTxfHandler uploadTo(URI tgt, GSSCredential cred) {
	TSRMLog.debug(this.getClass(), "uploadTo(tgt)", "event=failed reason=is_not_supported.", null);
	return null;
    }
    
    public void abortDownload() 
    {}
        
    public static boolean isDeviceSpecific(URI input) {
	if (input == null) {
	    return false;
	}

	String protocol = input.getScheme();
	if (protocol.equals(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    return true;
	}
	int sfnPos = input.toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	if (sfnPos > 0) {
	    return true;
	}
	       
	return false;
    }

    public static TSupportedURL getDeviceSpecificURL(TSURLInfo info) {
        TBasicDevice device = TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent).getRefDevice(info); 
	if (device == null) {
	    TSRMLog.debug(TSupportedURLDeviceSpecific.class, null, "function=getDeviceSpecificURL url="+info.getSURL().toString(), "deviceFound=null");
	    return null;
	}
	String protocol = info.getSURL().getScheme();
	if (protocol.equals(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    return device.create(info);
	} else if (protocol.equals(TSupportedURLWithSRM._DefProtocolStr)) {
	    int sfnPos = info.getSURL().toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	    if (sfnPos > 0) {
		return device.create(info);
	    } else {
		sfnPos = info.getSURL().toString().indexOf("/"+device.getDeviceDisk()+"/");
		if (sfnPos > 0) {
		    return device.create(info);
		}
	    }
	}
	return null;
    }		
    /*
    public static TSupportedURLDeviceSpecific getDeviceSpecificURL(TSURLInfo info) {
	String protocol = info.getSURL().getScheme();
	if (protocol.equals(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    return TSupportedURLOnLocalHPSS.createMe(info);
	} else {
	    int sfnPos = info.getSURL().toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	    if (sfnPos > 0) {
		return TSupportedURLOnLocalHPSS.createMe(info);
	    } else {
		TBasicDevice device = TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent).getRefDevice(info); 
		if (device == null) {
		    return null;
		}
		sfnPos = info.getSURL().toString().indexOf("/"+device.getDeviceDisk()+"/");
		if (sfnPos > 0) {
		    return TSupportedURLOnLocalHPSS.createMe(info);
		}
	    }
	}
	return null;
    }	
    */			

    public TReturnStatus mkdir(TAccount caller) {
	 checkType();
	 if (getAccessObj().isDir()) {
	     return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Dir already exists.");
	 } else if (getAccessObj().isFile()) {
	     return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Path is an existing file.");
	 } else {
	     getDevice().createPath(this, null, getAccessObj().getAccessInfo());
	     return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Created on device.");	    
	 }
    }

    public TReturnStatus rmdir(TAccount caller, boolean recursive) {
	if (isDir()) {
	    if (recursive) {
		getAccessObj().setRecursiveRemove();
	    }
	    String result = getDevice().removePath(getAccessObj(), false);
	    if (result == null) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Removed on device.");
	    } else {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, result);
	    }
	} else {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "no such dir on device.");
	}
    }
    
    public TReturnStatus rmFile(TAccount caller) {
	//TSupportedURLDeviceSpecific deviceUrl = (TSupportedURLDeviceSpecific)url;
	if (isDir()) {
	    return TSRMUtil.createReturnStatusFailed("path is a dir.");
	} else if (getAccessObj().isFile()) {
	    String result = getDevice().removePath(getAccessObj(), false);
	    if (result == null) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Removed file from device");
	    } else {
		return TSRMUtil.createReturnStatusFailed(result);
	    }
	} else {
	    return TSRMUtil.createReturnStatusFailed("No such file on device.");
	}	    		
    }

    public boolean pickupIOToken(TSRMRequest r) {
	return true; // tokens are handled in TMSSTxf
    }

    public void dropOffIOToken(TSRMRequest r) {
	 // tokens are handled in TMSSTxf
    }
}

 
