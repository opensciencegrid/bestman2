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
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.hpss.*;

import org.apache.axis.types.URI;
import org.ietf.jgss.GSSCredential;
import java.io.*;
import java.util.*;

public class THPSSDeviceGsiftp extends TBasicDevice {
    String _host = null;

    public THPSSDeviceGsiftp(String host,int bytesFreeInMB, TSRMStorage.iStoragePolicy policy) 			     
    {
	super(bytesFreeInMB, policy);
	_host = host;
	TSRMLog.info(this.getClass(), null, "event=GsiHPSSEnabled.", "host="+_host);
    }  
    
    public static String getHPSSPath(URI fullPathURI, String mssHost) {
	int sfnPos = fullPathURI.toString().indexOf("?SFN="+TSupportedURLOnLocalHPSS._DefProtocolStr);
	
	if (sfnPos == -1) {
	    sfnPos = fullPathURI.toString().indexOf("?SFN=/"+mssHost);
	    if (sfnPos == -1) {
		String currPath = fullPathURI.getPath();
		if (currPath.startsWith("/"+mssHost)) {
		    return currPath.substring(mssHost.length()+1);
		} else {
		    return currPath;
		}
	    } else {
		int pathStrStarts = sfnPos+6+mssHost.length();
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
    public TSupportedURL create(TSURLInfo info) {
	if (!(info.getSURL().getScheme().equalsIgnoreCase(TSupportedURLWithGSIFTP._DefProtocolStr))) {
	    String hpssPath = getHPSSPath(info.getSURL(), _host);

	    try {
		//info.setSURL(new URI(TSupportedURLWithGSIFTP._DefProtocolStr+"://"+_host+"/"+hpssPath));
		//System.out.println(">>>>>>>>>>>>>>>>>>>>>>>     See if this preserves the mss:// protocol.");
		//return new TSupportedURLOnLocalHPSSGsiftp(TSRMUtil.createTSURLInfo(new URI(TSupportedURLWithGSIFTP._DefProtocolStr+"://"+_host+"/"+hpssPath)));
		TSupportedURLOnLocalHPSSGsiftp result = new TSupportedURLOnLocalHPSSGsiftp(info);
		result.setURLString(TSupportedURLWithGSIFTP._DefProtocolStr+"://"+_host+"/"+hpssPath);
		return result;
	    } catch (Exception e) {
		e.printStackTrace();
		throw new TSRMException("Unable to generate proper uri."+ e.getMessage(), false);
	    }
	}
	return new TSupportedURLOnLocalHPSSGsiftp(info);
    }

    public String getDescription() {
	return "HPSS_host="+_host;
    }

    public String getDescriptionForPublic() {
	if (Config._doMarkupPingMsg) {
	    return "HPSS";
	} else {
	    return getDescription();
	}
    }
    //
    // returns true if the surl is on this device
    public boolean isDeviceSpecific(TSURLInfo surlInfo) {
	if (surlInfo.getSURL().getHost().equals(_host)) {
	    return true;
	} else {
	    String[] parsed = TSupportedURLWithSRM.parseTSURL(surlInfo);

	    if (parsed.length > 0) {
		String sfn = parsed[1];

		TSURLInfo curr = TSRMUtil.createTSURLInfo(sfn);
		if (curr != null) {
		    if (curr.getSURL().getScheme().equals(TSupportedURLOnLocalHPSS._DefProtocolStr) &&
			curr.getSURL().getHost().equals(_host)) {
			return true;
		    } 		       
		} else if (parsed[1].startsWith("/"+_host)) {
		    return true;
		}
	    }
	}
	return false;
    }
    
    public TDeviceAccessInfo getAccessInfo(TSRMStorage.iSRMSpaceToken token, TUserRequest requester)
    {
	// same as in THPSSDevice
	/*
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
	*/
	throw new TSRMException("Do I need this?", false);
    }
    
    
    public TDeviceAccessObj createPath(TSupportedURL fullPathStr, 
				       TSRMStorage.iSRMSpaceToken onBehalfOf,
				       TDeviceAccessInfo accessInfo)
    {
	throw new TSRMException("Not supported until I know how to get home dir with gsiftp://"+_host, false);
    }

    public TMetaDataPathDetail ls(TDeviceAccessObj pathRef, TSRMFileListingOption lsOption) 
    {
	TSupportedURLWithGSIFTP uri = (TSupportedURLWithGSIFTP)pathRef.getObj();
	return uri.ls(lsOption);
    }

    public void setPathType(TDeviceAccessObj pathRef) {
	TSRMLog.debug(this.getClass(), null, "event=ignore", "function=setPathType");
	return;
    }

    public long getSize(TDeviceAccessObj pathRef) {
	TSupportedURLWithGSIFTP uri = (TSupportedURLWithGSIFTP)pathRef.getObj();
	return uri.getTrustedSize(0);
    }

    public boolean chmod(TDeviceAccessObj pathRef, boolean writable) {
	throw new TSRMException("N/A",false);
    }

    public String removePath(TDeviceAccessObj pathRef, boolean deepClean) {
	TReturnStatus result = null;
	TSupportedURLWithGSIFTP uri = (TSupportedURLWithGSIFTP)pathRef.getObj();
	if (uri.isDir()) {
	    result = uri.rmdir(null, deepClean);
	} else {
	    result = uri.rmFile(null);
	}
	if (result == null) {
	    return "Failed to removePath()";
	}
	if (result.getStatusCode() != TStatusCode.SRM_SUCCESS)  {
	    return "Failed to removePath(). Explanation:"+result.getExplanation();
	}
	return null;
    }

    public URI getTransferURI(TDeviceAccessObj pathRef, TSRMTxfProtocol protocol) {
	TSupportedURLWithGSIFTP uri = (TSupportedURLWithGSIFTP)pathRef.getObj();
	return uri.getURI();
    }

    public void toDisk(TDeviceAccessObj from, TSRMLocalFile tgt) {
	TSupportedURLWithGSIFTP uri = (TSupportedURLWithGSIFTP)from.getObj();
	ISRMTxfHandler txf = uri.downloadTo(tgt);
    }

    public boolean localCopy(TSRMLocalFile from, TSRMLocalFile to) {
	return false; // no local copy is supported when gsiftp is used
    }
    
    public boolean verifyDeviceSize(long bytes) {
	return true; // no verification
    }

    public String getDeviceDisk() {
	return _host;
    }

    public TAccessLatency getAccessLatency() {
	return TAccessLatency.NEARLINE;
    }
    
    public void fromDisk(TSRMLocalFile f, Object tgt) {
	//TSRMLocalFile hpssFile = (TSRMLocalFile)(tgt);
	TSupportedURL uri = (TSupportedURL)tgt;
	
	org.ietf.jgss.GSSCredential cred = uri.getCredential();
	ISRMTxfHandler txf = uri.uploadFrom(f, cred);
	if (txf != null) {
	    txf.action();
	}	
    }

    private SRMFileTransfer initTxf(GSSCredential cred) {
	SRMFileTransfer transfer = new SRMFileTransfer();
	transfer.setCredentials(cred);
	return transfer;
    }

    private void readFromDisk(File f, TSRMLocalFile archiveTgt, SRMFileTransfer txf) {
	if (!TSRMServer._gProcessingUnit.acquireIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    throw new TSRMException("Cann't upload to MSS, ftp semaphore failed.", false);
	}
	try {
	    String src = "file:/"+f.getCanonicalPath();	       
	    
	    SRMTransferMode mode = SRMTransferMode.PUT; // expecting src to be file:/ protocol
	    	
	    txf.setDestinationUrl(archiveTgt.getURIString(TSRMTxfProtocol.GSIFTP));
	    txf.setSourceUrl(src);
	    txf.setTransferMode(mode);

	    txf.transferSync();
	    if (txf.getStatus() != null) {
		boolean noError = txf.doubleCheck();
		if (!noError) {
		    throw new RuntimeException("Transfer to MSS from:"+src+" failed. Reason:"+txf.getStatus());
		}
	    }
	} catch (Exception e) {
	    TSRMLog.exception(THPSSDeviceGsiftp.class, "readFromDisk() exception.details", e);
	    throw new TSRMException("Failed to readFromDisk()", false);
	} finally {
	    TSRMServer._gProcessingUnit.releaseIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr);
	}
    }

    public void bringFromSource(TSupportedURL src, TSRMLocalFile tgt) {
	if (src.isProtocolFILE()) {
	    src.downloadTo(tgt).action();
	    return;
	}

	TSRMLocalFile local = src.getLocalFile();

	if ((local != null) && (local.getToken() != null)) {			       
	    if (local.getToken().getHostDevice().isDisk()) {
		src.downloadTo(tgt).action();
		return;
	    }	  
	} 
	// stage first
	TSRMLocalFile stagingFile = TSRMStorage.stage(src, tgt.getReservedBytes(), null);
	TSRMLog.debug(this.getClass(), null, "stagingfile="+stagingFile, null);

	try {
	    stagingFile.download(src);
	    readFromDisk((File)(stagingFile.getPhysicalLocation().getRef().getObj()), tgt, initTxf(src.getCredential()));
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
   
    public TDeviceAccessObj construct(String accessPath, TDeviceAccessInfo accessInfo)
    {
	TSupportedURLWithGSIFTP uri = new TSupportedURLWithGSIFTP(TSRMUtil.createTSURLInfo("gsiftp://"+_host+"/"+accessPath));
	return new TDeviceAccessObj(uri, accessInfo);
    }
}


