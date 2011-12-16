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

package gov.lbl.srm.storage.Permanent;

import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.hpss.*;

import org.apache.axis.types.URI;
import java.io.*;
import java.util.*;

public class THPSSDevice extends TBasicDevice {
    static String _mssHost;
    String _address;	    

    IMSSTxf _hpssConnection = null;
    
    public THPSSDevice(String host, String address, int bytesFreeInMB, 
		       TSRMStorage.iStoragePolicy policy, IMSSTxf txf) 
    {
	super(bytesFreeInMB, policy);
	_mssHost = host;
	_address = address;
	if (_address.length() > 0) {
	    if (!_address.endsWith("/")) {
		_address += "/";
	    }
	}

	_hpssConnection = txf;

	// should initialize before calling THPSSDevice, from the rc file.1
    }

    public TSupportedURL create(TSURLInfo info) {
	return TSupportedURLOnLocalHPSS.createMe(info);
    }

    public Vector getStatusReport(String keyPrefix) {       
	return _hpssConnection.getStatusReport(keyPrefix);
    }
    public boolean verifyDeviceSize(long bytes) {
	TSRMUtil.startUpInfo("!!Cannt verify HPSS capacity...assuming true");
	return true;
    }

    public String getDeviceDisk() {
	return _mssHost;
    }
    
    public String getDescription() {
	return "HPSS@"+_mssHost;
    }

    public String getDescriptionForPublic() {
	if (Config._doMarkupPingMsg) {
	    return "HPSS";
	} else {
	    return getDescription();
	}		
    }
    
    public TAccessLatency getAccessLatency() {
	return TAccessLatency.NEARLINE;
    }

    public String getHPSSPath(URI fullPathURI) {
	return THPSSDeviceGsiftp.getHPSSPath(fullPathURI, _mssHost);
    }
    /*
    public String getHPSSPath(URI fullPathURI) {
	int sfnPos = fullPathURI.toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	
	if (sfnPos == -1) {
	    sfnPos = fullPathURI.toString().indexOf("?SFN=/"+_mssHost);
	    if (sfnPos == -1) {
		String currPath = fullPathURI.getPath();
		if (currPath.startsWith("/"+_mssHost)) {
		    return currPath.substring(_mssHost.length()+1);
		} else {
		    return currPath;
		}
	    } else {
		int pathStrStarts = sfnPos+6+_mssHost.length();
		String pathStr = fullPathURI.toString().substring(pathStrStarts);
		return pathStr;
	    }
	} else {
	    int pathStrStarts = sfnPos+5+TSupportedURLOnLocalHPSS._DefProtocolStr.length()+3;
	    String pathStr = fullPathURI.toString().substring(pathStrStarts);
	    int pos = pathStr.indexOf("/");
	    if (pos > 0) {
		pathStr = pathStr.substring(pos);
		return pathStr;
	    } else {
		throw new TSRMException("No hpss path detected."+pathStr, false);
	    }
	}
    }
    */
    public TDeviceAccessObj createPath(TSupportedURL fullPath,  
				       TSRMStorage.iSRMSpaceToken onBehalfOf,
				       TDeviceAccessInfo accessInfo)
    {	       	
	String pathStr = "";
	boolean pathIsAFile = false;

	TSRMLog.debug(this.getClass(), null, "event=createPath inputToken="+onBehalfOf, "inputPath="+fullPath.getURI().toString());
	String slash = "/";
	if (onBehalfOf != null) {
	    pathStr = fullPath.prepareDiskPathString(onBehalfOf); 
	    //pathStr += "-"+onBehalfOf.getID();
	    pathIsAFile = true;
	} else { // _address should be in here
	    pathStr = getHPSSPath(fullPath.getURI());

	    TSRMLog.debug(this.getClass(), null, "event=createPath, hpssPath="+pathStr, null);
	    /*
	    int sfnPos = fullPath.getURI().toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	    if (sfnPos == -1) {
		pathStr = fullPath.getURI().getPath();
	    } else {
		pathStr = fullPath.getURI().toString().substring(sfnPos+5+TSupportedURLOnLocalHPSS._DefProtocolStr.length()+3);
		int pos = pathStr.indexOf("/");
		if (pos > 0) {
		    pathStr = pathStr.substring(pos);
		} else {
		    throw new TSRMException("No hpss path detected."+pathStr, false);
		}
	    }
	    */

	    if (_address.length() > 0) {
		if (!pathStr.startsWith(_address)) {
		    throw new TSRMException("Illegal operation. MSS address needs to be under this dir:"+_address, false);
		}
		pathStr = pathStr.substring(_address.length());
	    }
	}
	
	String homeDir = _hpssConnection.getHomeDir(accessInfo);
	if (homeDir == null) {
	    throw new TSRMException("Cannt find home dir. Invalid accessInfo.", false);
	}
	if (!homeDir.endsWith("/")) {
	    homeDir += "/";
	}

	// now address is in the path
	TSRMUtil.startUpInfo("############### creating on HPSS: homedir="+homeDir+"["+pathStr+"] isFile?="+pathIsAFile+" "+accessInfo);
	TSRMLog.debug(this.getClass(), "createPath(HPSS)", "path="+pathStr, "isFile="+pathIsAFile);

	if (fullPath.isDeviceSpecific()) {
	    // we donnt allow recursive since we donnt know the home dir of each user	    
	    _hpssConnection.mkdir(pathStr, null, accessInfo, pathIsAFile); 
	} else {
	    int pos = pathStr.indexOf(slash,1);
	    if (pos == -1) {
		if (!pathIsAFile) {
		    _hpssConnection.mkdir(/*_address*+*/pathStr, null, accessInfo, pathIsAFile);	   
		}
	    } else {
		_hpssConnection.mkdir(/*_address+*/homeDir+pathStr.substring(0, pos), 
				      pathStr.substring(pos).split(slash), accessInfo, pathIsAFile);
	    }
	}
	
	//TDeviceAccessObj result =  new TDeviceAccessObj(/*_address+*/pathStr, accessInfo);	 
	TDeviceAccessObj result =  new TDeviceAccessObj(homeDir+pathStr, accessInfo);	 
	return result;
    }   

    public TDeviceAccessObj construct(String accessPath, TDeviceAccessInfo accessInfo) {
	TDeviceAccessObj result = new TDeviceAccessObj(accessPath, accessInfo);
	return result;
    }

    public TMetaDataPathDetail ls(TDeviceAccessObj pathRef, TSRMFileListingOption lsOption) {
	if (pathRef.isFile() && (pathRef.getTrustedSize() >= 0)) {
	    TMetaDataPathDetail result = new TMetaDataPathDetail();
	    String fullPath = (String)(pathRef.getObj());	    

	    //result.setPath(TSRMUtil.createTSURL(fullPath));
	    //result.setPath(fullPath);
	    result.setPath(pathRef.generateMetaDataPath(fullPath));

	    result.setType(TFileType.FILE);
	    result.setSize(TSRMUtil.createTSizeInBytes(pathRef.getTrustedSize()));
	    result.setLastModificationTime(TSRMUtil.createGMTTime(pathRef.getTimeStamp()));
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "recycled"));

	    return result;
	} else {
	    return _hpssConnection.ls(pathRef, lsOption.isAllLevelRecursive(), true, false);
	}
    }

    public boolean chmod(TDeviceAccessObj pathRef, boolean writable) {	    
	throw new TSRMException("Why am I doing chmod?", false);
    }

    public void setPathType(TDeviceAccessObj pathRef) {
	String errMsg = null;
	
	try {
	    _hpssConnection.getSize(pathRef);
	    return;
	} catch (TSRMException ex) {
	    TSRMLog.exception(this.getClass(), "details:", ex);	   
	    errMsg = ex.getMessage();
	    if ((ex.getStatusCode() == TStatusCode.SRM_AUTHENTICATION_FAILURE) ||
		(ex.getStatusCode() == TStatusCode.SRM_AUTHORIZATION_FAILURE))
	    {
		throw ex;
	    }
	}

	if (errMsg == null) {
	    return;
	}	
	int pos = errMsg.indexOf("SRM_MSS_AUTHORIZATION_FAILED");
	if (pos >= 0) {
	    throw new TSRMException(errMsg, false);
	}

	pos = errMsg.indexOf("SRM_MSS_NO_SUCH");
	if (pos == -1) {
	    int pNo = errMsg.indexOf("No ");
	    if (pNo >= 0) {
		throw new TSRMException(errMsg, false);
	    }	    
	    return;
	}

	String path = (String) pathRef.getObj();
	if (path.endsWith("/")) {
	    return;
	}

	int lastSlashPos = path.lastIndexOf("/");
	if (lastSlashPos == -1) {
	    return;
	}
	TDeviceAccessObj upperPathRef = new TDeviceAccessObj(path.substring(0, lastSlashPos), pathRef.getAccessInfo());
	try {
	    _hpssConnection.getSize(upperPathRef);
	} catch (TSRMException ex) {
	    int p = ex.getMessage().indexOf("SRM_MSS_NO_SUCH");
	    if (p > 0) {
		pathRef.setBrokenPath();
		throw ex;
	    }
	}	    	
    }

    public long getSize(TDeviceAccessObj pathRef) {
	try {
	    return _hpssConnection.getSize(pathRef);
	} catch (Exception e) {
	    if (e.getMessage().toUpperCase().indexOf("NOT SUPPORTED") >= 0) {
		TMetaDataPathDetail detail = _hpssConnection.ls(pathRef, false, true, false);
		return detail.getSize().longValue();
	    } else {
		throw new TSRMException(e.getMessage(), false);
	    }
	}
    }
	
    public String removePath(TDeviceAccessObj pathRef, boolean deepClean) {
	return _hpssConnection.remove(pathRef, pathRef.isDir(), pathRef.isRemoveRecursive());
    }
    
    public URI getTransferURI(TDeviceAccessObj pathRef, TSRMTxfProtocol protocol) {
	//dont(and cannt) return turl for storage type like HPSS.
	try {		    

	    URI uri = new URI(protocol.toString(), _mssHost, pathRef.getObj().toString(), null, null);
	    uri.setPath("///"+uri.getPath()); // otherwise GlobusURL would complain
	    /*
	      if (protocol != SRMTransferProtocol.FILE) {
	      uri.setHost(_host); // use Server._host for both webservice host and gsiftp host
	      }
	    */
	    if (protocol != TSRMTxfProtocol.GSIFTP) {
		uri.setScheme(TSupportedURLOnLocalHPSS._DefProtocolStr); // user protocol is ignored here
	    } else {
		uri.setScheme(protocol.toString());
	    }
	    
	    return uri;
	} catch (URI.MalformedURIException e) {	   
	    TSRMLog.exception(THPSSDevice.class, "details", e);
	    return null;
	}  
    }
    
    //
    // if the surl contains the host as in this device, then yes
    // 
    public boolean isDeviceSpecific(TSURLInfo surlInfo) {
	if (surlInfo.getSURL().getHost().equals(_mssHost)) {
	    return true;
	} else {
	    String[] parsed = TSupportedURLWithSRM.parseTSURL(surlInfo);

	    if (parsed.length > 0) {
		String sfn = parsed[1];
		/*
		if (sfn.startsWith("/")) {
		    sfn = TSupportedURLOnLocalHPSS._DefProtocolStr+":/"+sfn;
		}
		*/
		TSURLInfo curr = TSRMUtil.createTSURLInfo(sfn);
		if (curr != null) {
		    if (curr.getSURL().getScheme().equals(TSupportedURLOnLocalHPSS._DefProtocolStr) &&
			curr.getSURL().getHost().equals(_mssHost)) {
			return true;
		    } 		       
		} else if (parsed[1].startsWith("/"+_mssHost)) {
		    return true;
		} else if (!sfn.startsWith("/")) {
		    return false;
		} else {
		    while (sfn.startsWith("/")) {
			sfn = sfn.substring(1);
		    }
		    if (sfn.startsWith(_mssHost)) {
			return true;
		    }
		}
	    }
	}
	return false;
    }
    
    public void toDisk(TDeviceAccessObj from, TSRMLocalFile tgt) {
	//_hpssConnection.bringToDisk(from, tgt);
	String timedOutRid = _hpssConnection.bringToDisk(from, tgt);
	int counter = 0;
	while (timedOutRid != null) {
	    if (counter > 2) {
		throw new TSRMException("Timed out with retries on mssget()", false);
	    } else {
		handleTimedOut(timedOutRid);
		timedOutRid = _hpssConnection.bringToDisk(from, tgt);
		counter ++;
	    }
	}
    }
    
    public void handleTimedOut(String rid) {       
	_hpssConnection.abort(rid);
	TSRMLog.debug(this.getClass(), null, "event=MSSCallsTimedOut", "rid="+rid);
    }

    public void fromDisk(TSRMLocalFile localFile, Object archiveTgt) {
	File f  = (java.io.File)(localFile.getPhysicalLocation().getRef().getObj());
	TSupportedURL url = (TSupportedURL)archiveTgt;
	readFromDisk(f, url.getLocalFile());
    }

    private void readFromDisk(File f, TSRMLocalFile archiveTgt) {
	String timedOutRid = _hpssConnection.loadFromDisk(f, (TSRMLocalFile)archiveTgt);
	int counter = 0;
	while (timedOutRid != null) {
	    if (counter > 2) {
		throw new TSRMException("Timed out with retries on mssput().", false);
	    } else {
		handleTimedOut(timedOutRid);
		timedOutRid = _hpssConnection.loadFromDisk(f, (TSRMLocalFile)archiveTgt);
		counter ++;	   
	    }	    
	}       
    }
    
    public boolean localCopy(TSRMLocalFile src, TSRMLocalFile tgt) {
	_hpssConnection.cp(src.getPhysicalLocation().getRef(), tgt.getPhysicalLocation().getRef(), 
			   SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_YES, false);
	return true;
    }        
    
    //
    //
    public void bringFromSource(TSupportedURL src, TSRMLocalFile tgt) {		
	TSRMLocalFile local = src.getLocalFile();

	if ((local != null) && (local.getToken() != null)) {
	    if (local.getToken().getHostDevice() == this) {
		localCopy(local, tgt);
		return;
	    } 
	    if (local.getToken().getHostDevice().isDisk()) {
		readFromDisk((File)(local.getPhysicalLocation().getRef().getObj()), tgt);
		return;
	    }
	} else if (src.isDeviceSpecific()) {
	    //TSupportedURLDeviceSpecific s = (TSupportedURLDeviceSpecific)src;
	    if (src.getDevice() == this) {
		TSRMSourceFileUserOwned temp = new TSRMSourceFileUserOwned(src);
		localCopy((TSRMLocalFile)(temp.getLocalDestination()), tgt);
		return;
	    }
	}
	
	// stage first:
	TSRMLocalFile stagingFile = TSRMStorage.stage(src, tgt.getReservedBytes(), null);
	TSRMLog.debug(this.getClass(), null, "stagingfile="+stagingFile, null);

	try {
	    //src.downloadTo(stagingFile);
	    stagingFile.download(src);

	    readFromDisk((File)(stagingFile.getPhysicalLocation().getRef().getObj()), tgt);
	} catch (TSRMException e) {
	    throw e;
	} catch (RuntimeException e) {
	    throw e;
	} finally {	
	    stagingFile.unsetToken(false);
	    TSRMUtil.wipe(stagingFile);
	}
    }
    
    public TSRMLocalFile getStagingFile(TSupportedURL src, long bytes, TSRMRequest req) {
	if ((req.getSpaceToken() != null) && (req.getSpaceToken().getType() == TSRMSpaceType.Volatile)) {
	    return TSRMStorage.stage(src, bytes, req.getSpaceToken());
	} else {
	    return TSRMStorage.stage(src, bytes, null);
	}
    }
    
    public TDeviceAccessInfo getAccessInfo(TSRMStorage.iSRMSpaceToken token,
					   TUserRequest requester) 
    {
	TDeviceAccessInfo accessInfo = token.getTokenAttributes().getAccessInfo();
	if (accessInfo == null) {
	    if (requester != null) {
		if (requester.getStorageSystemInfo() != null) {
		    return new TDeviceAccessInfo(requester.getStorageSystemInfo());
		}
	    }
	    return null;
	} else {
	    return accessInfo;
	}
    }
    
    public static TDeviceAccessInfo getAccessInfo(TSURLInfo surlInfo) {
	ArrayOfTExtraInfo info = surlInfo.getStorageSystemInfo();
	
	if (info != null) {
	    return new TDeviceAccessInfo(info);
	}
	
	return null;
    }
    
    public static TDeviceAccessObj getAccessObj(TSURLInfo surlInfo) {
	TDeviceAccessInfo accessInfo = getAccessInfo(surlInfo);
	
	String hpssPath = THPSSDeviceGsiftp.getHPSSPath(surlInfo.getSURL(), THPSSDevice._mssHost);
	/*
	String hpssPath = surlInfo.getSURL().getPath();
	int sfnPos = surlInfo.getSURL().toString().indexOf("?SFN=");
	if (sfnPos > 0) {
	    String[] parsed = TSupportedURLWithSRM.parseTSURL(surlInfo);
	    if (parsed.length > 0) {
		TSURLInfo curr = TSRMUtil.createTSURLInfo(parsed[1]);
		hpssPath = curr.getSURL().getPath();
		//return new TDeviceAccessObj(hpssPath, accessInfo, surlInfo.getSURL().toString());
	    }
	}
	*/
	String surlStr = surlInfo.getSURL().toString();
	String prefix = surlStr.substring(0, surlStr.length() - hpssPath.length());

	return new TDeviceAccessObj(hpssPath, accessInfo, prefix);
    }
}


