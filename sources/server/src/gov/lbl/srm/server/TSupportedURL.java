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

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;
import java.util.Vector;
import java.io.*;

public abstract class TSupportedURL {
    private TSURLInfo _info;
    private TDeviceAccessObj _accessObj = null;
    private GSSCredential _cred = null;

    public TSupportedURL(TSURLInfo info) {
	_info = info;
    }
    
    public abstract String getProtocol();
    public abstract java.util.Vector ls(URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype);
    
    public abstract TReturnStatus authorize(TSRMPermission p, TAccount user);
    public abstract long getTrustedSize(int recursiveLevel);
    public abstract void setTrustedSize(long size);
    public TFileStorageType getFileStorageType() {return null;}

    public String getEffectivePath() {
	return getURI().getPath();
    }
    
    public abstract boolean checkExistence();
    public void validate() {}
    
    public void useCredential(GSSCredential creds) 
    {
	_cred = creds;
	validate();
    } 

    public GSSCredential getCredential() {
	return _cred;
    }
    
    public TDeviceAccessObj getAccessObj() {
	return _accessObj;
    }
    
    public void setAccessObj(TDeviceAccessObj obj) {
	_accessObj = obj;
    }
    
	public TSupportedURL createSubdir(String sub) {
		if (sub.startsWith("/")) {
			sub = sub.substring(1);
		}

		String p = getURLString();
		if (p.endsWith("/")) {
			p += sub;
		} else {
			p += "/";
			p += sub;
		}
		TSURLInfo info = TSRMUtil.createTSURLInfo(p);
    	info.setStorageSystemInfo(getSURLInfo().getStorageSystemInfo());
 
    	TSupportedURL result= TSupportedURL.create(info);
    	result.useCredential(getCredential());
   		return result;
	}

    public TSupportedURL createSubpath(String subpath) {
	String p = TSRMUtil.createPath(getURLString(), subpath);
	TSURLInfo info = TSRMUtil.createTSURLInfo(p);	
	info.setStorageSystemInfo(getSURLInfo().getStorageSystemInfo());

	TSupportedURL result= TSupportedURL.create(info);
	result.useCredential(getCredential());
	return result;
    }

    public TSURLInfo getSURLInfo() {
	return _info;
    }
    
    public URI getURI() {
	return _info.getSURL();
    }
    
    public String getHost() {
	return getURI().getHost();
    }
    
    public int getPort() {
	return getURI().getPort();
    }
    
    public String getHostNPort() {
	return gGetHostNPort(getSURLInfo());
    }
    
    public static String gGetHostNPort(TSURLInfo path) {
	int port = path.getSURL().getPort();
	String host = path.getSURL().getHost();
	
	if (port >= 0) {
	    return host+"-"+port;
	} else {
	    if (host != null) {
		return host;
	    } else {
		return Config._host;
	    }
	}
    }
    
    public String getURLString() { 
	return _info.getSURL().toString();
    } 
    
    private boolean hasSameURI(URI another) {
	return _info.getSURL().equals(another);
    }
    
    public boolean isCorrespondingTo(TSRMLocalFile localFile) {
	if (localFile.isSurl()) {
	    return hasSameURI(localFile.getSiteURL().getSURL());
	} else {
	    return hasSameURI(localFile.getSourceURI());
	}
    }
    //
    // note that for a directory, an empty string will be returned.
    //
    public String getEndFileName() {
	String urlStr = getURLString();
	
	return TSRMUtil.getEndFileName(urlStr);
    }
    
    public boolean isProtocolHTTP() {
	return false;
    }
    
    public boolean isProtocolSRM() {
	return false;
    }
    
    public boolean isProtocolFTP() {
	return false;
    }
	
    public boolean isProtocolGSIFTP() {
	return false;
    }
	
    public boolean isProtocolFILE() {
	return false;
    }
	
    public boolean isOfProtocol(String p) {
	return getProtocol().equals(p);
    }
	
    public boolean isDeviceSpecific() {
	return false;
    }

    public TBasicDevice getDevice() {return null;}
    public void populateTo(int recursiveLevel, TSRMLocalDirUserOwned dir) {throw new TSRMException("Not supported action.", false);};
	
    public static TSURLInfo supportDoubleSlash(TSURLInfo info) {		    
	//
	// Viji asked to support multiple // here to be compatible with V1
	//
	boolean found = false;
	String path = info.getSURL().getPath();		    
	while (path.startsWith("//")) { 
	    path = path.substring(1);
	    found = true;
	}
	
	if (!found) {
	    return info;
	}
	
	try {
	    //info.getSURL().setPath(path0);
	    TSURLInfo i = new TSURLInfo();
	    i.setStorageSystemInfo(info.getStorageSystemInfo());

	    URI uri = new URI(info.getSURL().toString());
	    uri.setPath(path);

	    i.setSURL(uri);
	    return i;
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    throw new RuntimeException(e.getMessage());
	}
    }
    
   
    private static TSupportedURL constructDeviceSpecificURL(String sfn, TSURLInfo info0) {
	if (TSRMStorage.getSpaceManager(TSRMSpaceType.Permanent) == null) {
	    return null;
	}
 
	TSupportedURL u = TSupportedURLDeviceSpecific.getDeviceSpecificURL(info0);	       
	if (u != null) {
	    return u;
	} 	   
	 
	return null;
    }

    private static TSupportedURL createSRMSURL(TSURLInfo info, boolean limitedToLocal) {
	 TSURLInfo info0 = supportDoubleSlash(info); 

	 String[] parsed = TSupportedURLWithSRM.parseTSURL(info0);
	 
	 if (TSRMUtil.isRefToSRM(parsed[0], Config._wsdlEndPoint, info0.getSURL())) {
	     if (parsed[1] == null) {
		 throw new RuntimeException("Not supported URL:"+info.getSURL()+" need SFN.");
	     }

	     TSupportedURL deviceUrl = constructDeviceSpecificURL(parsed[1], info0);
	     if (deviceUrl != null) {
		 return deviceUrl;
	     }
	     /*    	     
	     if (!parsed[1].startsWith("/")) {
		 TSURLInfo curr = TSRMUtil.createTSURLInfo(parsed[1]);
		 if (curr != null) {
		     ////curr.setStorageSystemInfo(info.getStorageSystemInfo());
		     ////TSupportedURLDeviceSpecific u = TSupportedURLDeviceSpecific.getDeviceSpecificURL(curr);	       
		     TSupportedURLDeviceSpecific u = TSupportedURLDeviceSpecific.getDeviceSpecificURL(info0);	       
		     if (u != null) {
			 return u;
		     } 
		 }
	     } 
	     */
	     
	     if (!Config._usingCacheKeyword) {
		 return new TSupportedURLWithSRMRefToLocal(info0, parsed[0], parsed[1]);
	     }

	     if (parsed[1].startsWith(Config._DefCacheKeyword)) {
		 parsed[1] = parsed[1].substring(10);
		 return new TSupportedURLWithSRMRefToLocal(info0, parsed[0], parsed[1]);
	     } else {
		 TSupportedURLWithFILE f = TSRMUserSpaceTokenSupport.expandURL(info0, parsed[1]);
		   
		 return f;
		 //return new TSupportedURLWithFILE(info0, parsed[1]);
	     }
	 } else {
	     if (!limitedToLocal) {
		 return new TSupportedURLWithSRMRefToRemote(info0, parsed[0], parsed[1]);
	     }
	 }
	 return null;
    }

    public static TSupportedURL create(TSURLInfo info, boolean limitedToLocal) {	  
	String protocol = info.getSURL().getScheme();		

	if (protocol.equals(TSupportedURLWithSRM._DefProtocolStr)) {
	    return createSRMSURL(info, limitedToLocal);
	    //return new TSupportedURLWithSRM(info);
	} else if (protocol.equals(TSupportedURLWithHTTP._DefProtocolStr)) {
	    return new TSupportedURLWithHTTP(info);
	} else if (protocol.equals(TSupportedURLWithGSIFTP._DefProtocolStr)) {
	    return new TSupportedURLWithGSIFTP(info);
	} else if (protocol.equals(TSupportedURLWithFTP._DefProtocolStr)) {
	    return new TSupportedURLWithFTP(info);
	} else if (protocol.equals(TSupportedURLWithFILE._DefProtocolStr)) {
	    return new TSupportedURLWithFILE(info, null);
	} else {  
	    TSupportedURL u = TSupportedURLDeviceSpecific.getDeviceSpecificURL(info);
	    
	    if (u != null) {
		return u;
	    }
	}
	throw new TSRMException("Not supported URL:"+info.getSURL(), false);
    }

    public static TSupportedURL create(TSURLInfo info) {
	return create(info, false);
    }
    
    public static TSupportedURL createAndCheckSiteFileName(TSURLInfo info, TAccount user, TSRMPermission p) {
	return createAndCheckSiteFileName(info, user, p, false);
    }

    public void checkSFN(TAccount user, TSRMPermission p) {
	if (isProtocolSRM()) {
	    TSupportedURLWithSRM surl = (TSupportedURLWithSRM)this;
	    if (surl.isLocalToThisSRM()) {
		TSupportedURLWithSRMRefToLocal local = (TSupportedURLWithSRMRefToLocal)surl;
		local.checkSiteFileName(user, p);
	    }
	}
    }
	
    public static TSupportedURL createAndCheckSiteFileName(TSURLInfo info, TAccount user, TSRMPermission p, boolean limitedToLocal) {
	if (info == null) {
	    throw new TSRMException("Null surl info.", false);
	}

	TSupportedURL url = TSupportedURL.create(info, limitedToLocal);
	if (url != null) {
	    TSRMLog.debug(TSupportedURL.class, null, "event=createdUrl address="+url, "url=\""+url.getURLString()+"\"");
	} else {
	    TSRMLog.debug(TSupportedURL.class, null, "event=createdNullUrl", null);
	}

	if (url != null) {
	    url.checkSFN(user, p);
	}
	
	return url;
    }
	
    public static String getEndFileName(TSURLInfo src) {
	if (src == null) {
	    return null;
	}
	
	TSupportedURL temp = TSupportedURL.create(src);
	String[] array = TSupportedURL.getPathInArray(temp);
	
	if (array != null) {
	    return array[array.length-1];
	}
	return null;	
    }
    
    public String prepareDiskPathString(TSRMStorage.iSRMSpaceToken onBehalfOf) {	
	/*
	String fullPathStr = "";
	if (owner != null) {
	    fullPathStr = owner.getID()+File.separatorChar;
	} 
	
	fullPathStr += getHostNPort();
	//fullPathStr += getURLString();

	String[] pathArray = TSupportedURL.getPathInArray(this);
	for (int i=0; i<pathArray.length; i++) {
	    fullPathStr += File.separatorChar+pathArray[i];
	}

	return fullPathStr;
	*/
	TAccount owner = onBehalfOf.getOwner();

	String[] pathArray = TSupportedURL.getPathInArray(this);

	//String result = onBehalfOf.getID()+"-"+TSRMUtil.getNextRandomUnsignedInt() +"-"+ pathArray[pathArray.length-1];
	String result = TSRMUtil.generatePathStr(onBehalfOf.getID(), pathArray[pathArray.length-1]);

	if (owner != null) {
	    return owner.getID()+"/"+result;
	} else {
	    return result;
	}
    }

    public static String[] getPathInArray(TSupportedURL src) {	
	String path = src.getEffectivePath();
	
	String[] result = path.split("/");
	
	// since splitting a string "/tmp//hi///" returns "" "tmp" "" "hi", 
	// we just clear all the empty strings in the result of split before returning from this function.
	Vector doubleCheck = new Vector();
	for (int i=0; i<result.length; i++) {
	    if (result[i].length() > 0) {
		if (!result[i].startsWith("?")) {
		    doubleCheck.add(result[i]);
		}
	    }
	}
	if (doubleCheck.size() == result.length) {
	    return result;
	} else {
	    String[] r = new String[doubleCheck.size()];
	    for (int i=0; i<doubleCheck.size(); i++) {
		r[i] = (String)(doubleCheck.get(i));
	    }
	    return r;
	}
    }
	
    public void setLocalPath(ISRMLocalPath p) {// some device path needs it
	return;
    }

    public ISRMLocalPath getLocalPath() {
	if (isProtocolSRM()) {
	    TSupportedURLWithSRM surl = (TSupportedURLWithSRM)this;
	    if (surl.isLocalToThisSRM()) {
		TSupportedURLWithSRMRefToLocal local = (TSupportedURLWithSRMRefToLocal)surl;
		return local.getCorrespondingLocalPath();
	    }  
	}
	return null;
    }

    public TSRMLocalFile getLocalFile() {
	if (isProtocolSRM()) {
	    TSupportedURLWithSRM surl = (TSupportedURLWithSRM)this;
	    if (surl.isLocalToThisSRM()) {
		TSupportedURLWithSRMRefToLocal local = (TSupportedURLWithSRMRefToLocal)surl;

		if (local.isDir()) {
		    return null;
		}
		return (TSRMLocalFile)(local.getCorrespondingLocalPath());
	    }  
	}
	return null;
    }       
      
    public static void upload(URI remote, TSRMLocalFile local, GSSCredential cred) {
	// initiate gsiftp:
	TSURLInfo n = TSRMUtil.createTSURLInfo(remote);
	
	TSupportedURL temp = TSupportedURL.create(n);
	ISRMTxfHandler txf = temp.uploadFrom(local, cred);
	if (txf != null) {
	    txf.action();
	}
    }    

    public static void upload(TSupportedURL src, URI remote,  GSSCredential cred) {	
	ISRMTxfHandler txf = src.uploadTo(remote, cred);
	if (txf != null) {
	    txf.action();
	}
    }    


   public TReturnStatus doMv(TAccount caller, TSupportedURL toUrl) {
        if (this.isDeviceSpecific() && toUrl.isDeviceSpecific()) {
	    TSRMUtil.createReturnStatusFailed("Not yet supported: moving "+this.getURLString()+" to "+toUrl.getURLString());
	    //return ((TSupportedURLDeviceSpecific)this).moveTo(((TSupportedURLDeviceSpecific)toUrl));
	} else if (this.isDeviceSpecific() || toUrl.isDeviceSpecific()) {
	    return TSRMUtil.createReturnStatusFailed("Move between user file and cache file is not allowed.");
	}

	TReturnStatus err = caller.checkPathIsLocalWithPermission(this, TSRMPermission.Writable, true);
	if (err != null) {
	    return err;
	}
	
	ISRMLocalPath from = this.getLocalPath();

	//if (!toUrl.isLocal()) {
	//   return TSRMUtil.createReturnStatusInvalidPath(toUrl.getURLString()+" is not local to this SRM.");
	//}
	err = caller.checkPathIsLocalWithPermission(toUrl, TSRMPermission.Writable, false);
	if (err != null) {
	    return err;
	}		

	String[] pathArray = TSupportedURL.getPathInArray(toUrl);	
	TAccount toUrlOwner = TAccountManager.getAccount(pathArray[0]);
	
	if (toUrlOwner == null) {
	    return TSRMUtil.createReturnStatusFailed("srmMv:Target path refers to non-exist user:"+pathArray[0]);
	}

	if (!toUrlOwner.checkPermission(caller, TSRMPermission.Writable)) {
	    return TSRMUtil.createReturnStatusNotAuthorized("srmMv() does not allow to move across user accounts!");
	}

	TSRMChangeDirStatus status = TAccount.changeDir(toUrlOwner.getTopDir(), "getLocalPath-mvFile", pathArray, 1);
	TSRMLocalDir tgtDir = status.getDir();
	
	if (tgtDir == null) {	       
	    return status.getStatus();
	}
	String tgtName = pathArray[pathArray.length-1];

	if (from.getParent() == tgtDir) {
	    if (tgtName.equals(from.getName())) {
		//return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "src and tgt has the same path");
		// changed to success b/c of Jean-Phillip's insistance
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "src and tgt has the same path");
	    }
	}
	if (tgtDir.getFile(tgtName) != null) {
	    String errorMsg;
	    if (from.isDir()) {
		errorMsg = "Cannt do mv(dir, file)";
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, errorMsg);
	    } else {
		errorMsg = "tgt file already exists in mv(file, file), cannt overwrite";
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, errorMsg);
	    }	    
	} else {
	    String fromSurl = from.getSiteURL().getSURL().toString();
	    tgtDir.move(from, tgtName);	
	    TSRMNameSpace.replaceEntry(fromSurl, from.getSiteURL().getSURL());
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	}
    }

    public TReturnStatus mvFrom(ISRMLocalPath from) {
	String fromSurl = from.getSiteURL().getSURL().toString();
	String[] pathArray = TSupportedURL.getPathInArray(this);
	
	// assuming that pathArray[0] is the account info
	TAccount user = TAccountManager.getAccount(pathArray[0]);
	if (user == null) {
	    return TSRMUtil.createReturnStatusFailed("Target path refers to non-exist user:"+pathArray[0]);
	}

	TSRMLocalDir dir = user.getTopDir();
	if (dir != from.getTopDir()) {
	    return TSRMUtil.createReturnStatusNotAuthorized("mv() across user accounts.");
	}
	TSRMChangeDirStatus status = TAccount.changeDir(dir, "getLocalPath-mvFile", pathArray, 1);
	TSRMLocalDir tgtDir = status.getDir();
	
	if (tgtDir == null) {	       
	    return status.getStatus();
	}
	String tgtName = pathArray[pathArray.length-1];

	if (from.getParent() == tgtDir) {
	    if (tgtName.equals(from.getName())) {
		//return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "src and tgt has the same path");
		// changed to success b/c of Jean-Phillip's insistance
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "src and tgt has the same path");
	    }
	}
	if (tgtDir.getFile(tgtName) != null) {
	    String errorMsg;
	    if (from.isDir()) {
		errorMsg = "Cannt do mv(dir, file)";
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, errorMsg);
	    } else {
		errorMsg = "tgt file already exists in mv(file, file), cannt overwrite";
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, errorMsg);
	    }	    
	} else {
	    tgtDir.move(from, tgtName);	
	    ///TSRMNameSpace.replaceEntry(fromSurl, getURI());
	    TSRMNameSpace.replaceEntry(fromSurl, from.getSiteURL().getSURL());
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	}
    }

    public TReturnStatus rmFile(TAccount caller) {
	return TSRMUtil.createReturnStatusFailed("Not a local file.");
    }

    public TReturnStatus rmdir(TAccount caller, boolean recursive) {
	return TSRMUtil.createReturnStatusFailed("Not a local dir.");
    }

    public TReturnStatus mkdir(TAccount caller) {
	return TSRMUtil.createReturnStatusFailed(TSRMUtil._DefInvalidPath);
    }

    //public abstract void transferTo(TSupportedURL tgt);
    public abstract ISRMTxfHandler downloadTo(TSRMLocalFile tgt);
    
    // upload from local "src" to this surl
    public abstract ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred);
    public          ISRMTxfHandler uploadFrom(TSupportedURL src, GSSCredential cred) {return null;}
    public abstract ISRMTxfHandler uploadTo(URI tgt, GSSCredential cred);

    public abstract void abortDownload();
    public abstract boolean isDir();

    public abstract void populateMe(TSRMSourceDir dir);
    public boolean isLocal() {return false;}

    public boolean pickupIOToken(TSRMRequest r) {
	return r.pickupIOToken();
    }

    public void dropOffIOToken(TSRMRequest r) {
	r.dropOffIOToken();
    }
}

