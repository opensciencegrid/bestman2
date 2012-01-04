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

import org.apache.axis.types.URI;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.policy.TSRMGeneralEnforcement;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import java.io.*;

//import EDU.oswego.cs.dl.util.concurrent.Mutex;
//
//
//
public class TSRMPhysicalLocation {
    private TBasicDevice 		_device       	= null;
    private TDeviceAccessObj 	_pathRef 	= null;
    private boolean 		_isTransfering	= false;
    private TSRMLocalFile	 	_stagingFile	= null;
    private TSRMMutex                   _txfMutex       = new TSRMMutex();
    private TDeviceAccessInfo       _deviceAccessInfo     = null; // to access the device
    private TSupportedURL           _ref = null;
    
    public TSRMPhysicalLocation(TSupportedURL path) 
    {
	if (path.getAccessObj() != null) {
	    _deviceAccessInfo = path.getAccessObj().getAccessInfo();
	}
	_device = path.getDevice();
	_pathRef = path.getAccessObj();

	_ref = path;
    }
    
    public TSRMPhysicalLocation(TSupportedURLWithFILE path) 
    {
	_deviceAccessInfo = null;
	TSRMStorage.iSRMSpace volatileSpaceManager = TSRMStorage.getSpaceManager(TSRMSpaceType.Volatile);

	String pathString = path.getDiskPath(); //path.getURI().toString().substring(5);
	
	// the org.apache.axis.types.URI returns hohoho if do getPath() with file://tmp/hohoho, so cannt use it. 
	//_pathRef = new TDeviceAccessObj(new File(path.getSURL().getValue().getPath()), null);
	_pathRef = new TDeviceAccessObj(TSRMUtil.initFile(pathString), null);

	_ref = path;

	if (volatileSpaceManager != null) {
	    _device = volatileSpaceManager.locateDefaultToken(TFileStorageType.VOLATILE).getHostDevice();
	} else {
	    _device = new gov.lbl.srm.storage.Volatile.TDiskDevice(".", 10, null); // 10 is a random choice. We do not care what it is.
	}

    }   

    public TSRMPhysicalLocation(TSupportedURL path, 				
				TSRMLocalFile onBehalfOf,
				TDeviceAccessInfo accessInfo) 
    {
	_deviceAccessInfo = accessInfo;
	_device           = onBehalfOf.getToken().getHostDevice();	    
	_pathRef          = _device.createPath(path, onBehalfOf.getToken(), _deviceAccessInfo);

	_ref = path;
    }

     // this constructor is used to read from cache
    public TSRMPhysicalLocation(String accessPath,
				TSRMLocalFile onBehalfOf,
				TDeviceAccessInfo accessInfo) 
    {
	_deviceAccessInfo = accessInfo;
	_device           = onBehalfOf.getToken().getHostDevice();
	
	_pathRef          = _device.construct(accessPath, accessInfo);
    }
    
    public TSupportedURL getSrcRef() {
	return _ref;
    }
    
    public TBasicDevice getDevice() {
	return _device;
    }

    
    public TDeviceAccessInfo getDeviceAccessInfo() {
	return _deviceAccessInfo;
    }
    
    public TDeviceAccessObj getRef() {
	return _pathRef;
    }
    
    /*
    public long getLastModificationTime() {
	return _device.getLastModificationTime(_pathRef);
    }
    */
    
    public TMetaDataPathDetail ls(TSRMFileListingOption op) {
	return _device.ls(_pathRef, op);
    }

    public long getSize() {
	try {
	    if (_pathRef == null) {
		return -1;
	    }
	    if (_pathRef.getTrustedSize() > 0) {
		return _pathRef.getTrustedSize();
	    }

	    long result = _device.getSize(_pathRef);
	    
	    return result;
	} catch (TSRMException e) {
	    TSRMLog.exception(this.getClass(), "phsy::getSize()", e);
	    return -1;
	}
    }
    
    public void remove(boolean deepClean) {
	String errMsg = _device.removePath(_pathRef, deepClean);
	if (errMsg != null) {
	    throw new TSRMException(errMsg, false);
	}
	//_size = 0; // update size
    }
    /*
    public void deepClean() {
	_device.deepClean(_pathRef);
    }
    */
    
    public URI getTransferURI(TSRMTxfProtocol protocol) {
	return _device.getTransferURI(_pathRef, protocol);
    }
    
    public boolean isTransfering() {
	return this._isTransfering;
    }
    
    public boolean changeTransferState(boolean state) {
	boolean result = true;
	if (TSRMUtil.acquireSync(_txfMutex)) {
	    if (_isTransfering && state) {
		result = false;
	    } 
	    this._isTransfering = state;
	    
	    TSRMUtil.releaseSync(_txfMutex);
	}

	TSRMLog.debug(this.getClass(), null, "event=changeTxfState", "state="+state);
	return result;
    }

    public boolean isFileTrivial() {	
	long currSize = _device.getSize(_pathRef);
	if (currSize >= 0) {
	    return false;
	}
	return true;
    }
    
    public TSRMLocalFile getStagingFile() {
	return _stagingFile;
    }
    
    private long receiveWhenStagingFileIsNull(long reservedBytes, TSRMRequestPut req) {
	if (!_device.chmod(_pathRef, true)) {
	    throw new TSRMException("Can not change mode to writable.", false);
	}
	try {
	    if (req.setReadyForClientToUpload()) {
		long receivedBytes = waitOnUserToPutFile(reservedBytes, req);
		//req.setReceivedBytes(receivedBytes);
		return receivedBytes - reservedBytes;			
	    } else {
		TSRMLog.debug(this.getClass(), null, "receive_maybe_aborted",  req.description());
		throw new TSRMException("Received maybe aborted.", false);
	    }
	} finally {
	    _device.chmod(_pathRef, false);
	}
    }

    private long receiveWithStagingFile(TSupportedURL localTgt, long reservedBytes, TSRMRequestPut req) {
	_stagingFile.setBlock(TSRMFileTxfState.CLIENTPUSH, true); // no need to check setBlock returnValue, 
	                                                          // this file is only using once.	

	if (!_stagingFile.getPhysicalLocation().getDevice().chmod(_stagingFile.getPhysicalLocation().getRef(), true)) {
	    throw new TSRMException("Cannt change mode to writable.", false);
	}
	try {
	    if (req.setReadyForClientToUpload()) {
		long actualBytes = _stagingFile.getPhysicalLocation().waitOnUserToPutFile(reservedBytes, req);	
		//req.setReceivedBytes(actualBytes);

		TSRMLocalFile tgtFile = localTgt.getLocalFile();
		if (tgtFile != null) {
		    long diff = actualBytes - tgtFile.getReservedBytes();
		    
		    if ((diff != 0) && (tgtFile.getToken() != null)) {			
			tgtFile.getToken().updateToken(tgtFile, diff);
		    }
		}

		if (!req.isAborted()) {
		    //_device.fromDisk((java.io.File)(_stagingFile.getPhysicalLocation().getRef().getObj()), tgtFile);
		    _device.fromDisk(_stagingFile, localTgt);
		    if (tgtFile != null) {
			tgtFile.updateToken();
		    }
		}
	    }
	    
	    return 0;
	} catch (RuntimeException e) {
	    throw e;
	} finally {
	    _stagingFile.getPhysicalLocation().getDevice().chmod(_stagingFile.getPhysicalLocation().getRef(), false);
	    _stagingFile.unsetToken(false);
	    _stagingFile.getParent().rmFileName(_stagingFile.getName());
	    _stagingFile.setBlock(TSRMFileTxfState.NONE, false);
	}
    }

    public long receive(TSupportedURL localTgt, long reservedBytes, TSRMRequestPut req) {
	if (localTgt.isProtocolFILE()) {
	    _pathRef.doNotTouch();
	}
	_stagingFile = _device.getStagingFile(localTgt, reservedBytes, req);
	if (_stagingFile == null) {
	    return receiveWhenStagingFileIsNull(reservedBytes, req);
	} else {
	    return receiveWithStagingFile(localTgt, reservedBytes, req);
	}
    }
    
    private long waitOnUserToPutFile(long reservedBytes, TSRMRequestPut req) { 
	if (!changeTransferState(true)) {
	    throw new TSRMException("Trying to put a file more than once.", false);
	}
	
	long inactiveTime = 0;
	long sizeWas = -1;
	
	long breakInMilliseconds = 3000;

	req.setTxfURLExpirationTime();
	req.adjustTxfURLExpirationTime();

	while (isTransfering()) {
	    if (System.currentTimeMillis() > req.getTxfURLExpirationTime()) {
		throw new TSRMException("timed out!", false);
	    }	    
	    TSRMUtil.sleepAlert(breakInMilliseconds);
	    sizeWas = getBytesActivelyWritten(sizeWas);
		req.setReceivedBytes(sizeWas);
	    if (req.getReturnStatus().getStatusCode() == TStatusCode.SRM_SPACE_AVAILABLE) {
		//sizeWas = getBytesActivelyWritten(sizeWas);
	    } else {
		//System.out.println("Whatever happens is not related to this request!!!");
		
		//req.setReceivedBytes(sizeWas);
		return 0;
	    }
	}

	req.setReceivedBytes(sizeWas);
	return sizeWas;
    }
    
    //
    // added parameter "was" avoids the case when the fileOfInterest exists and
    // if we were check "was" from disk, it would be the full size and awhile later, the
    // size is the transferred size which is a smaller number.
    //
    private long getBytesActivelyWritten(long was) {		
	long currSize = _device.getSize(_pathRef);
	TSRMLog.debug(this.getClass(), null, "event="+getRef().getObj()+".getBytesActivelyWritten", 
		     "currSize="+currSize+" was="+was);
	
	if (currSize >= was) {
	    return currSize;
	} else {		   
	    if (isTransfering()) {
		throw new gov.lbl.srm.util.TSRMException("File is unexpectedly deleted!", false);
	    } else {
		return -1;
	    }
	}
    }
}
