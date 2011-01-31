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

import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import org.apache.axis.types.URI;

import java.util.Vector;


public class TSRMLocalFileUserOwned extends TSRMLocalFile {
     long _knownBytes = -1;
    TSURLInfo _siteURL = null; // different from the default provided by the super class

     public TSRMLocalFileUserOwned(String name, TFileStorageType fileType) {
	 super(null, name, fileType, true);
     }

     public long getReservedBytes() {	 
	 return _knownBytes;
     } 

     public void setKnownBytes(long v) {
	 _knownBytes = v;	
     }

     public TSURLInfo getSiteURL() {
	 if (_siteURL != null) {
	     return _siteURL;
	 }
	
	 _siteURL = new TSURLInfo();
	 try {
	     _siteURL.setSURL(new URI(_detail.getPath()));
	 } catch (Exception e) {
	     TSRMLog.exception(TSRMLocalFileUserOwned.class, "details", e);	    
	     TSRMLog.exception(this.getClass(), "getSiteURL()", e);
	 }

	 if ((getPhysicalLocation() != null) && (getPhysicalLocation().getDeviceAccessInfo() != null)) {
	     _siteURL.setStorageSystemInfo(getPhysicalLocation().getDeviceAccessInfo().getStub());
	 }

	 return _siteURL;
     }

     public void findSpaceInToken(TSRMStorage.iSRMSpaceToken token, long size) {
	 return;
     }

     public void setToken(TSRMStorage.iSRMSpaceToken token) 
	{}

     public boolean hasExceededReservedBytes() {
	 return false;
     }

     public void updateToken() 
	 {}

     public void changeParentTo(TSRMLocalDir p) 
	 {}
	 
     
     public void activate(boolean contentIsDeleted) 
	 {}     

     public boolean pin(TSRMRequest r, TStatusCode code) {
	r.setReturnStatus(TSRMUtil.createReturnStatus(code, "pin is not enforced for user owned files"));
	return true;
     }

     public void unpin(TSRMRequest r) 	    
	 {}
    	
     public void release() 
	 {}	
	
     public TSRMPinManager getPinManager() {
	 return null;
     }
     
     public long getExpirationTimeInMilliSeconds() {
	 return -1; // never expires
     }
     
     public TSRMPin getPin(TSRMRequest r) {
	 return null;
     }

     public boolean isThirdPartyTxfAssumed() {
	 return true;
     }

     public boolean compactIsFeasible() 
	 { return true;}

     public boolean isPinned() {
	 return false;
     }

     public void setBlock(boolean doBlock) 
	 {}

    public TMetaDataPathDetail lsCommonFileNotReady() {
	 TMetaDataPathDetail result = new TMetaDataPathDetail();
	 result.setType(TFileType.FILE);
	 result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "Not on device."));
	 return result;
    }
     public void setPhysicalLocation(TSupportedURL url, 		
				     TDeviceAccessInfo accessInfo) 
    {
	_detail.setPath(url.getURLString()); 
	if (url.isDeviceSpecific()) {
	    //TSupportedURLDeviceSpecific deviceUrl = (TSupportedURLDeviceSpecific)url;
	    _physicalFileLocation = new TSRMPhysicalLocation(url);
	    _detail.setFileStorageType(url.getFileStorageType());
	} else if (url.isProtocolFILE()) {
	    TSupportedURLWithFILE fileUrl = (TSupportedURLWithFILE)url;
	    _physicalFileLocation = new TSRMPhysicalLocation(fileUrl);
	} else {
	    super.setPhysicalLocation(url, accessInfo);
	}
    }

    public TSRMLocalFile getStageFile() {
	return TAccountManager._SUPER_USER.getTopDir().createSubFile(getName()+"-staged"+TSRMUtil.generateRandomString(), 
								     TFileStorageType.VOLATILE, false);
    }

    public void download(TSupportedURL src) {
	
	if (getToken() != null) {
	    super.download(src);
	} else {
	    setTxfObj(null);

	    getPhysicalLocation().getDevice().bringFromSource(src, this);
	    TSRMLog.debug(this.getClass(), null, "event=download", "txfObj="+_txfObj);

	    if (getTxfObj() != null) {
		getTxfObj().action();
	    }
	}
    }

    public String getDefaultURIString() {
	return getURIString(TSRMTxfProtocol.GSIFTP);
    }

    public boolean isValid() {
	return true;
    }
}



