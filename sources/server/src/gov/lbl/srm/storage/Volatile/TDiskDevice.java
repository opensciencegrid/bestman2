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

package gov.lbl.srm.storage.Volatile;

//import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;
import java.io.*;

import gov.lbl.srm.storage.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
  
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
 
public class TDiskDevice extends TBasicDevice {
    //String _address;
    File _address;
    
    public TDiskDevice(String address, int bytesFreeInMB, TSRMStorage.iStoragePolicy policy) {
	super(bytesFreeInMB, policy);

	if (bytesFreeInMB == 0) {
	    throw new TSRMException("Cannt assume disk has infinite capability", false);
	}
	_address = TSRMUtil.initFile(address);
	if (!_address.exists()) {
	    TSRMUtil.startUpInfo("Error: no such path: ["+_address+"]");
	    System.exit(1);
	}
	TSupportedURLWithFILE.addBlockedLocalPath(address);
    }
    
    public boolean verifyDeviceSize(long bytes) {
	return TSRMUtil.verifyDirectory(_address.getAbsolutePath(), bytes);
    }
    
    public String getDeviceDisk() {
	try {
	    return TPlatformUtil.getDisk(_address.getAbsolutePath());
	} catch (Exception e) {
	    throw new RuntimeException("Error on device:"+getDescription()+", getDeviceDisk() gives:"+e.getMessage());
	}
    }
    
    public File getAddress() {
	return _address;
    }
    
    public String getDescription() {
	return "Disk@"+_address.toString();
    }

    public String getDescriptionForPublic() {
	if (Config._doMarkupPingMsg) {
	    return "Disk";
	} else {
	    return getDescription();
	}		
    }
        
    public TAccessLatency getAccessLatency() {
	return TAccessLatency.ONLINE;
    }

 
    public TDeviceAccessObj createPath(TSupportedURL fullPath, 
				       TSRMStorage.iSRMSpaceToken onBehalfOf,
				       TDeviceAccessInfo accessInfo) 
    {

	String fullPathStr = fullPath.prepareDiskPathString(onBehalfOf);
	//fullPathStr += "-"+onBehalfOf.getID();
	//fullPathStr = onBehalfOf.getID()+"-"+fullPathStr;

	TSRMLog.debug(this.getClass(), null, "event=createPath(disk)", "path="+fullPathStr);

	//File newPath = new File(_address, fullPathStr);
	File newPath = TSRMUtil.initFile(_address, fullPathStr);
	if (newPath.getParentFile().exists()) {
	    return new TDeviceAccessObj(newPath, accessInfo, true);
	} else {
	    boolean created = newPath.getParentFile().mkdirs();

	    if (created) {		
		return new TDeviceAccessObj(newPath, accessInfo, true);
	    } else {
		throw new TSRMException("Failed to create directory:"+newPath.getParentFile().getPath(), false);
	    }
	}	
    }

    public TDeviceAccessObj construct(String accessPath, TDeviceAccessInfo accessInfo) {
	File newPath = TSRMUtil.initFile(accessPath);
	return new TDeviceAccessObj(newPath, accessInfo);
    }

    public TMetaDataPathDetail ls(TDeviceAccessObj pathRef, TSRMFileListingOption lsOption) {
	File f = (File)(pathRef.getObj());

	return ls(f, lsOption);
    }

    public static TMetaDataPathDetail ls(File f, TSRMFileListingOption lsOption) {
	if (!f.exists()) {
	    throw new TSRMException(f.getPath()+" File is gone from Disk", false);
	}

	TMetaDataPathDetail result = new TMetaDataPathDetail();
	if (f.isDirectory()) {
	    result.setType(TFileType.DIRECTORY);
	} else {
	    result.setType(TFileType.FILE);
	    result.setSize(TSRMUtil.createTSizeInBytes(getFileSize(f)));

	    if (lsOption.getShowChecksum()) {
		Checksum.handle(result, f, lsOption);	   
	    }
	}

	if (lsOption.isDetailNeeded()) {
	    result.setLastModificationTime(TSRMUtil.createGMTTime(f.lastModified())); 
	}

	result.setPath(TSRMTxfProtocol.FILE.generateURI(f).toString());
	result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk"));
	return result;
    }
    
    /*
    public boolean isDir(TDeviceAccessObj pathRef) {
	File f = (File)(pathRef.getObj());
	if (f.isDirectory()) {
	    return true;
	}
	return false;
    }
    */

    public void setPathType(TDeviceAccessObj pathRef) {
	File f = (File)(pathRef.getObj());
	if (f.isDirectory()) {
	    pathRef.setIsDir();
	} else if (f.exists()) {
	    pathRef.setIsFile();
	} 
    }

    public boolean chmod(TDeviceAccessObj pathRef, boolean writable) {	
    File f = (File)(pathRef.getObj());

	try {
	    if (pathRef.cannotTouch()) {
		TSRMLog.debug(this.getClass(), null, "event=chmod_is_skipped", "src=\""+f.getCanonicalPath()+"\"");
		return true;
	    }
	    if (!f.exists()) {
		TSRMUtil.startUpInfo("Creating  file:"+f.getCanonicalPath());
		if (!f.createNewFile()) {		    
		    return false;
		}
	    }
	} catch (Exception e) {
	    TSRMLog.exception(TDiskDevice.class, "details:"+f.getPath(), e);
	    return false;
	}
	return TPlatformUtil.chmod(f.getPath(), writable);
    }
    
    public long getSize(TDeviceAccessObj pathRef) {	    
	File f = (File)(pathRef.getObj());
	return getFileSize(f);
    }

     public static long getFileSize(File f) {	    
	 //File f = (File)(pathRef.getObj());
	if (f.exists()) {
	    return f.length();
	} else {
	    return -1;
	}
     }
   
    public String removePath(TDeviceAccessObj pathRef, boolean deepClean) {
	File f = (File)(pathRef.getObj());
	f.delete();
	// this removed the parent dir when a rm is called before txf, causing txf to fail!
	if (deepClean && pathRef.doAutoDeleteParent()) {
	    if (f.getParentFile() != null) {
		f.getParentFile().delete();
	    }
	}
	
	return null;
    }
    
    public URI getTransferURI(TDeviceAccessObj pathRef, TSRMTxfProtocol protocol) {
	File f = (File)(pathRef.getObj());
	return protocol.generateURI(f);
	/*
	try {
	    //return new URI("gsiftp", null, "dmx.lbl.gov", 4000, getPhysicalLocation().toURI().getPath(), null, null);	  
	    URI uri = new URI(f.toURI().toString());
	    
	    uri.setPath("///"+uri.getPath()); // otherwise GlobusURL would complain
	    
	    if (protocol != TSRMTxfProtocol.FILE) {
		uri.setHost(Config._host); // use Server._host for both webservice host and gsiftp host
	    }
	    uri.setScheme(protocol.toString());
	    
	    return uri;
	    // works
	    //return new URI("file",  null, "////C:/temp/test-"+getPhysicalLocation().getName(), null, null);
	    //return new URI("file",  null, "////temp/test-"+getPhysicalLocation().getName(), null, null);
	    
	    // would contain: C:\projet\srm.lbl.v2 .. and "\"is not allowed.
	    //return new URI("gsiftp", null, "dummy.lbl.gov", 4000, getPhysicalLocation().getPath(), null, null);
	} catch (URI.MalformedURIException e) {	    
	    return null;
	} 
	*/
    }
    
    public boolean isDeviceSpecific(TSURLInfo surlInfo) {
	return false;
    }
    
    public void toDisk(TDeviceAccessObj from, TSRMLocalFile tgtFile) {
	File src = (File)(from.getObj());
	File tgt = (File)(tgtFile.getPhysicalPath());

	if ((tgt != null) && (src != null)) {
	    try {
		copy(src, tgt);
	    } catch (Exception e) {
		TSRMLog.exception(this.getClass(), "toDisk()", e);
		throw new TSRMException("Failed to copy from disk to disk."+e.getMessage(), false);
	    }
	} else {
	    throw new TSRMException("TDiskDevice.toDisk("+src+", "+tgt+") is not operatable", false);
	}
	//return false;
    }
    
    public void fromDisk(TSRMLocalFile f, Object tgt) {
	// return;
    }
    public TSRMLocalFile getStagingFile(TSupportedURL src, long bytes, TSRMRequest req) {
	return null;
    }
      
    public boolean localCopy(TSRMLocalFile src, TSRMLocalFile tgt) {    
	try {
	    File srcFile = (File)(src.getPhysicalLocation().getRef().getObj());
	    File tgtFile = (File)(tgt.getPhysicalLocation().getRef().getObj());

	    if ((srcFile == null) || (tgtFile == null)) {
		TSRMLog.error(this.getClass(), "localCopy()_fails", "src="+srcFile, "tgt="+tgtFile);
		return false;
	    }

	    if (srcFile.getPath().equalsIgnoreCase(tgtFile.getPath())) {
		TSRMLog.debug(this.getClass(), null, "event=localCopy", "reason=src_equals_tgt_on_disk");
	    } else {
		copy(srcFile, tgtFile);
	    }
	    return true;
	} catch (IOException e) { // from "new File"
	    return false;
	}
    }
    
    public static void copy(File src, File dst) throws IOException {
	if (src.getCanonicalPath().equals(dst.getCanonicalPath())) {
	    TSRMLog.debug(TDiskDevice.class, null, "event=copy_is_duplicated", "\""+src.getCanonicalPath()+"\"");
	    return;
	}
	TSRMLog.debug(TDiskDevice.class, null, "event=copy", "src=\""+src.getCanonicalPath()+"\" dst=\""+dst.getCanonicalPath()+"\"");
	InputStream in = null;  
	OutputStream out = null; 
	
	try {
	    in = new FileInputStream(src);
	    out = new FileOutputStream(dst);
	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
		out.write(buf, 0, len);
	    }
	} finally {
	    if (in != null) {
		in.close();
	    }
	    if (out != null) {
		out.close();
	    }
	}
    }
    
    public void bringFromSource(TSupportedURL src, TSRMLocalFile tgt) {
	src.downloadTo(tgt);
    }
    
    public TDeviceAccessInfo getAccessInfo(TSRMStorage.iSRMSpaceToken token,
					   TUserRequest requester) 
    {
	return null;
    }

    public TSupportedURL create(TSURLInfo info) {
	return TSupportedURL.create(info); // should not needed to be called here
    }
}
