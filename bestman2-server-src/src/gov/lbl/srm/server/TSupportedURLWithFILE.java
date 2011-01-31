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

import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.impl.*;

import java.io.File;
import java.util.*;
import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.*;
import org.globus.ftp.*;
import org.apache.axis.types.URI;

public class TSupportedURLWithFILE extends TSupportedURL {
    public static final String _DefProtocolStr = "file";
    public File _file = null;
    public long _size = -1;
    public TSRMLocalFileUserOwned _srmFile = null;
    public static ArrayList _blockedPath = new ArrayList();
    public static ArrayList _allowedPath = new ArrayList();
    //private GridFTPClient _gridClient = null;
    ISRMFileAccess _fileAccess =  null;
    ISRMFileAccess _lsAccess = null;

    static IClientAuthorization _authPolicy = null;
    
    public TSupportedURLWithFILE(TSURLInfo info, String sitePath) {	
	super(info);	
	String localPath = getURI().getPath(); // srm://.../local/path

	if (sitePath == null) {
	    localPath = getURI().toString().substring(5); // file://local/path	    
	} else {
	    localPath = sitePath;
	}

	//blockSensitiveLocalPathAccess(localPath);
	//agreeWithAllowedList(localPath);

	if (!localPath.startsWith("/")) {
	    localPath = "/"+localPath;
	}

	//_file = new File(localPath);
	_file = TSRMUtil.initFile(localPath);
	TSRMLog.debug(this.getClass(), null, "localFilePath="+localPath, "uri="+getURI().toString());

	if (Config._accessFileSysUsingGSIFTP) {
	    _fileAccess = new TSRMFileAccessGsiftp(this);
	} else if (Config._accessFileSysUsingSUDO) {
	    _fileAccess = new TSRMFileAccessSudo();
	    if (Config._noSudoOnLs) {
		_lsAccess = new TSRMFileAccessDefault();
	    }
	} else {
	    _fileAccess = new TSRMFileAccessDefault();
	}
	if (_lsAccess == null) {
	    _lsAccess = _fileAccess;
	}	
    }
    
	public int getPort() {
		return -1;
	}
    public void setFileAccessFS(){
	if (!_lsAccess.isUsingFileSystem()) {
	    _lsAccess = new TSRMFileAccessDefault();
	}
    }

    public void setFileAccessGsiftp() {
	if (!_lsAccess.isUsingGsiftp()) {
	    _lsAccess = new TSRMFileAccessGsiftp(this);
	}
    }

    private static void agreeWithAllowedList(File f) {
	if (_allowedPath.size() == 0) {
	    return;
	}

	//File f = new File(localPath);
	//File f = TSRMUtil.initFile(localPath);
	
	String path = null;
	try {
		path = f.getCanonicalPath(); // canonical path is used instead of getPath() to handle subdir that are links from other dir
	} catch (Exception e) {
		throw new TSRMException("["+f.getPath()+"] has trouble with canonical path.Can not access it.", false);
	}

	int pos = _allowedPath.indexOf(path);
	if (pos > -1) {
	    return;
	}

	for (int i=0; i<_allowedPath.size(); i++) {
	    String curr = (String)(_allowedPath.get(i));	    
	    if (path.startsWith(curr+File.separator)) {
		return;
	    }
	}
	throw new TSRMException("["+path+"] is not in allowed list. Will not access it", false);
    }

    public void blockSensitiveLocalPathAccess(File f) {
	//File f = new File(localPath);
	//File f = TSRMUtil.initFile(localPath);

	if (_blockedPath == null) {
		return;
	}
	String path = null;
	try {
		path = f.getCanonicalPath();
	} catch (Exception e) {
		throw new TSRMException("["+f.getPath()+"] has trouble with canonical path. cannt access it.", false);
	}
	int pos = _blockedPath.indexOf(path);	
	if (pos > -1) {
	    throw new TSRMException("Access to: ["+path+"] is forbidden!", false);
	}

	for (int i=0; i<_blockedPath.size(); i++) {
	    String curr = (String)(_blockedPath.get(i));
        
	    if (curr.length() > 1) {
			pos = path.indexOf(curr);
			if (pos == 0) {
		    	throw new TSRMException("Access to: ["+curr+"] is forbidden", false);
			}
	    } else {
			try {
		 		if ((f.getParentFile() != null) && f.getParentFile().getCanonicalPath().equals(curr) && !isDir()) {
         			//Changed by viji on Jan 28, 08 on Alex suggestion
					throw new TSRMException("Access to dir: ["+curr+"] is forbidden", false);
	    		}
			} catch (Exception e) {
				e.printStackTrace();
				throw new TSRMException("Access to: ["+curr+"] is forbidden. Canonical path failure for:"+f.getParentFile().getPath()+" exp:"+e.getMessage(), false);
			}
		}
	}
    }

    public static void parseBlocked(String blockpathInput) {	
	ISRMFileAccess access = createFileAccess();
	initBlockedLocalPath(Config._blockedLocalPath, access);
	if (blockpathInput != null) {
	    initBlockedLocalPath(blockpathInput, access);	
	}

	for (int i=0; i<_blockedPath.size(); i++) {
	     TSRMUtil.startUpInfo("..............blocking access to path: "+_blockedPath.get(i));
	}
    }

    public static void parseAllowed(String allowedInput) {
	if (allowedInput == null) {
	    System.out.println("No restriction on allowed path.");
	    return;
	}
	ISRMFileAccess access = createFileAccess();
	initAllowedLocalPath(allowedInput, access);

	for (int i=0; i<_allowedPath.size(); i++) {
	    TSRMUtil.startUpInfo("...............allowed access to path: "+_allowedPath.get(i));
	}
    }

    private static void initAllowedLocalPath(String input, ISRMFileAccess access) {
	String[] inputList = input.split(";");
	if (inputList == null) {
	    throw new RuntimeException("Cannt parse the _allowed_local_path input:"+input);
	}
	for (int i=0; i<inputList.length; i++) {
	    String curr = inputList[i];
	    addAllowedLocalPath(curr, access);
	}
    }

    private static void addAllowedLocalPath(String curr, ISRMFileAccess access) {
	//File f = new File(curr);
	try {
	File f = TSRMUtil.initFile(curr);
	if (access != null) {
	    if (!access.exists(f)) {
		throw new RuntimeException("No such path:"+curr+". Invalid allowed_local_path input");
	    }
	} 
	    
	String pathName = f.getCanonicalPath();
	int pos = _allowedPath.indexOf(pathName);
	if (pos == -1) {
	    _allowedPath.add(pathName);
	}
	pos = _allowedPath.indexOf(f.getPath());
	if (pos == -1) {
		_allowedPath.add(f.getPath());
	}
	} catch (Exception e) {
		e.printStackTrace();
	}
    }
	
    private static void initBlockedLocalPath(String input, ISRMFileAccess access) {
	_blockedPath.clear();
	if (input.trim().length() == 0) {
	    return;
	}
	String[] inputList = input.split(";");
	if (inputList == null) {
	    throw new RuntimeException("Cann't parse this blocked local path input:"+input);
	}

	for (int i=0; i<inputList.length; i++) {
	    String curr = inputList[i];
	    addBlockedLocalPath(curr, access);
	}
    }
    
    private static ISRMFileAccess createFileAccess() {
	if (Config._accessFileSysUsingGSIFTP) {
	    TSRMUtil.startUpInfo("!!Can not use gsiftp to access files. No credential. Will take as is.");
	    return null;
	} else if (Config._accessFileSysUsingSUDO) {
	    return new TSRMFileAccessSudo(); // enough to test: exists()
	} else {
	    return new TSRMFileAccessDefault();
	}
    }

    public static void addBlockedLocalPath(String curr) {
	addBlockedLocalPath(curr, null);
    }

    public static void addBlockedLocalPath(String curr, ISRMFileAccess access) {
	//File f = new File(curr);
	try {
	File f = TSRMUtil.initFile(curr);
	if (access != null) {
	    if (!access.exists(f)) {
		throw new RuntimeException("No such path: ["+curr+"]. Invalid blocked local path input.");
	    }
	} 

	String pathName = f.getCanonicalPath();
	int pos = _blockedPath.indexOf(pathName);
	if (pos == -1) {
	    _blockedPath.add(pathName);
	}
	pos = _blockedPath.indexOf(f.getPath());
	if (pos == -1) {
		_blockedPath.add(f.getPath());
	}
	} catch (Exception e) {
		e.printStackTrace();
	}
    }


    public String getDiskPath() {
	return _file.getPath();
    }

    public TSRMLocalFile getStageFile() {
	if (_srmFile != null) {
	    return _srmFile;
	}
	_srmFile = new TSRMLocalFileUserOwned(_file.getName(), null);
	_srmFile.setPhysicalLocation(this, null);
	return _srmFile;
    }

    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    // get the local path and do ls through the path
    /*
    public TMetaDataPathDetail[] ls() {
	//return null;
	throw new TSRMException("Unexpected!", false);
    }
    */
    
    public boolean isProtocolFILE() {
	return true;
    }
    
    public TFileStorageType getFileStorageType() {return TFileStorageType.VOLATILE;}

    public static TReturnStatus authorize(TSRMPermission p, String uid, String path) {
	String err = null;

	if (_authPolicy == null) {
	    return null;
	}
	
	if (p == TSRMPermission.Readable) {
	    err = _authPolicy.isReadable(uid, path);
	} else if (p == TSRMPermission.Writable) {
	    err = _authPolicy.isWritable(uid, path);
	}

	if (err != null) {
	    return TSRMUtil.createReturnStatusNotAuthorized("User:"+uid+" has no ["+p.toString()+"] permission to path:"+path+".Detail:"+err);
	}	   

	return null;
    }

    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	return authorize(p, user.getID());
    }

    public static void setAuthorizationClass(String opStr) {
	if (opStr == null) {
	    return;
	}

	String classN = TSRMUtil.getValueOf(opStr, "class=", '&', false);
	String jarFileN = TSRMUtil.getValueOf(opStr, "jarFile=", '&', false);
	//String protocolN = TSRMUtil.getValueOf(opStr, "name=", '&', true);

	Object[] input = null;
	java.lang.Class[] constructorInput = null;

	if ((classN == null) || (jarFileN == null)) {
	    // cannt use this user input policy
	    throw new RuntimeException("Cann't apply this policy: "+opStr);
	}

	//ISRMClientAuthPolicy authPolicy = null;
	try {
	    Class userPolicyClass = TSRMUtil.loadClass(Config._pluginDir+"/"+jarFileN, classN);
	    java.lang.reflect.Constructor constructor = userPolicyClass.getConstructor(constructorInput);
	    _authPolicy = (IClientAuthorization) (constructor.newInstance(input));
	    TSRMLog.info(TSupportedURLWithFILE.class, null, "event=setUserAuthClass", "value="+_authPolicy);
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("Error loading user authorization policy."+e.toString());
	    System.exit(1);
	}
    }

    private TReturnStatus authorize(TSRMPermission p, String uid) {
	TReturnStatus result = authorize(p, uid, _file.getPath());

	if (result != null) {
	    return result;
	}

	// need to check ACL, will do it later
	// donnt use _file.canWrite/exists, use the access functions, b/c gsiftp access may have different storage disk than local disk.
	//if ((p == TSRMPermission.Writable) && (_file != null) && (_file.exists()) && (!_file.canWrite())) {
		//cannt verify whether a certain user can have write access to a file, so abandon it.
	    //return TSRMUtil.createReturnStatusNotAuthorized("No write permission:"+_file.getPath());
	//}
	return null;
    }

    private void invokeLocalCopy(TSRMLocalFile tgt) {
	if ((tgt == null) || (tgt.getPhysicalLocation() == null)) {
	    throw new TSRMException("Cannt generate URI.", false);
	}

	String localTgt = tgt.getPhysicalLocation().getTransferURI(TSRMTxfProtocol.FILE).getPath();
	String localSrc = getDiskPath(); //getURI().getPath();
	try {
	    //java.io.File srcFile = new java.io.File(localSrc);
	    //java.io.File tgtFile = new java.io.File(localTgt);
	    File srcFile = TSRMUtil.initFile(localSrc);
	    File tgtFile = TSRMUtil.initFile(localTgt);
	    gov.lbl.srm.storage.Volatile.TDiskDevice.copy(srcFile, tgtFile);
	} catch (java.io.IOException e) {	   
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details", e);	    
	    throw new TSRMException("Local copy failed. "+e.getMessage(), false);
	}
    }

    private SRMFileTransfer initTxf() {
	return initTxf(null);
    }
    
    private SRMFileTransfer initTxf(GSSCredential cred) {
	GSSCredential txfCred = cred;
	if (txfCred == null) {
	    if (getCredential() == null) {
		throw new TSRMException("No credential found. Cannt proceed. Did you delegate?", false);
	    }
	    txfCred = getCredential();
	}

	SRMFileTransfer transfer = new SRMFileTransfer();
	
	try {
	    transfer.setSourceUrl(getURLString());
	} catch (java.io.IOException e) {
	    throw new RuntimeException(e.toString());
	}
	
	if (getHost().equalsIgnoreCase("garchive.nersc.gov")) {
	    transfer.setNERSCHost();
	}
	transfer.setTransferType(SRMTransferProtocol.FILE);
	
	transfer.setCredentials(txfCred);

	return transfer;
    }
    
    public ISRMTxfHandler downloadTo(TSRMLocalFile tgt) {
	if ((tgt != null) && (tgt == _srmFile)) {
	    return null;
	}

	boolean tgtOwnedByCaller = tgt.isThirdPartyTxfAssumed();
	if (tgtOwnedByCaller) {
	    SRMFileTransfer transfer = initTxf();
	    
	    TSRMUploadCommon txf = new TSRMUploadCommon(transfer, tgt.getURIString(TSRMTxfProtocol.GSIFTP), getStageFile(), null);
	    tgt.setTxfObj(txf);
	    return txf;
	} else {
	    invokeLocalCopy(tgt);
	    return null;
	}
    }
	
    public ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred) {
	//RuntimeException ex = new RuntimeException("Not supported yet in TSupportedURLWith");	
	//TSRMLog.exception(TSupportedURLWithFILE.class, "details", ex);	    
	//throw ex;
	if (src.isDir()) {
	    RuntimeException ex = new RuntimeException("Donn't upload dirs");	
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details", ex);	    
	    throw ex;
	}

	TSRMLocalFile srcFile = (TSRMLocalFile)src;
	SRMFileTransfer transfer = initTxf(cred);
	//transfer.setSourceUrl(src);

	TSRMUploadCommon txf = new TSRMUploadCommon(transfer, getStageFile().getURIString(TSRMTxfProtocol.GSIFTP), srcFile, null);
	srcFile.setTxfObj(txf);
	return txf;
    }

    public ISRMTxfHandler uploadTo(URI tgt, GSSCredential cred) {
	TSRMLog.info(this.getClass(), null, "event=uploadFromRemote", "result=notSupported");
	return null;
    }
    
    public void abortDownload() {
	return; 
    }
    
    public void setTrustedSize(long size) {
	_size = size;
    }

    public long getTrustedSize(int ignoredRecurisveLevel) {	    
	if (_size < 0) {
	    _size = _fileAccess.getLength(_file);//_file.length();
	}
	
	return _size;
    }
    
    public void validate() {
	//if (_lsAccess.exists(_file)) {
	    blockSensitiveLocalPathAccess(_file);	
	    agreeWithAllowedList(_file);
	//}
    }

    public boolean checkExistence() {
	//return _file.exists();
	return _lsAccess.exists(_file);
    }

    public boolean isDir() {
	return _fileAccess.isDir(_file); //_file.isDirectory();
    }

    public boolean isFile() {
	return _fileAccess.isFile(_file);
    }

    public void populateMe(TSRMSourceDir dir) {
	//RuntimeException hi = new RuntimeException("Not supported;");	
	//TSRMLog.exception(TSupportedURLWithFILE.class, "details", hi);	    
	//throw hi;
		if (isFile()) {
			return;
		}
		TSRMFileListingOption op = null;
		int rlevel = dir.getRecursiveLevel();
		TSRMLog.info(this.getClass(), null, "event=populate", "recursiveLevel="+rlevel);
		if (rlevel == -1) {
			op = new TSRMFileListingOption(true, null, null, null, null);
		} else {
			op = new TSRMFileListingOption(null, new Integer(rlevel), null, null, null);
		}
		TMetaDataPathDetail result = listMe(_file, op, rlevel);
		if (result.getArrayOfSubPaths() == null) {
			return;
		}
		TMetaDataPathDetail[] subPath = result.getArrayOfSubPaths().getPathDetailArray();
		if ((subPath == null) || (subPath.length == 0)) {
			TSRMLog.debug(this.getClass(), null, "populated=false", "path="+_file.getPath());
			return;
		}
		TSRMLog.info(this.getClass(), null, "event=populate", "size="+subPath.length);
		dir.populate(subPath);
    }

    public TReturnStatus rmdir(TAccount caller, boolean recursive) {		
	TReturnStatus authStatus = authorize(TSRMPermission.Writable, caller);
	if (authStatus != null) {
	    return authStatus;
	}
	_fileAccess.setUid(caller.getID());
	if (checkExistence() && !isDir()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "input for rmdir() is a file.");
	}
	return _fileAccess.deleteDir(_file,recursive);	    
    }
  

    public TReturnStatus doMv(TAccount caller, TSupportedURL toUrl) {
	TReturnStatus authStatus = authorize(TSRMPermission.Writable, caller);
	if (authStatus != null) {
	    return authStatus;
	}

	if (!toUrl.isProtocolFILE()) {
	    return TSRMUtil.createReturnStatusInvalidPath(toUrl.getURLString()+" Error: No such file or directory: "+getURLString());
	}
       
	TSupportedURLWithFILE dest = (TSupportedURLWithFILE)toUrl;

	_fileAccess.setUid(caller.getID());
	return _fileAccess.rename(_file, dest._file);
    }

    public TReturnStatus mkdir(TAccount caller) {
	TReturnStatus authStatus = authorize(TSRMPermission.Writable, caller);
	if (authStatus != null) {
	    return authStatus;
	}

	_fileAccess.setUid(caller.getID());

	if (checkExistence()) {
	    if (isDir()) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, "already existss");
	    } else if (isFile()) {
		return TSRMUtil.createReturnStatusFailed("Is a file. Unable to mkdir().");
	    }
	} 

	TReturnStatus status= _fileAccess.makeDir(_file);
	if (status.getStatusCode() == TStatusCode.SRM_SUCCESS) {
	    return status;
	}
	if (checkExistence() && isDir()) {
	    status.setStatusCode(TStatusCode.SRM_DUPLICATION_ERROR);		    
	}
	int pos = status.getExplanation().indexOf("No such file");
	if (pos > 0) {
	    status.setStatusCode(TStatusCode.SRM_INVALID_PATH);
	}

	return status;
    }


    public TReturnStatus rmFile(TAccount caller) {
	TReturnStatus authStatus = authorize(TSRMPermission.Writable, caller);
	if (authStatus != null) {
	    return authStatus;
	}

	_fileAccess.setUid(caller.getID());

	if (isDir()) {
	    return TSRMUtil.createReturnStatusFailed("path is a dir. Call rmDir()");
	}  
	return _fileAccess.rmFile(_file);
    }

    public Vector ls(URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	Vector pathList = new Vector();
	
	TReturnStatus authStatus = authorize(TSRMPermission.Readable, listingOp.getUid());
	if (authStatus != null) {
	    TMetaDataPathDetail result = listMe(_file,listingOp, 0);
	    result.setPath(TSRMUtil.getAbsPath(uri));
	    result.setStatus(authStatus);
	    pathList.add(result);
	    return pathList;
	}	

	TMetaDataPathDetail result = listMe(_file, listingOp, listingOp.getRecursiveLevel());
	//result.setPath(uri.toString());
	result.setFileLocality(TFileLocality.ONLINE);
	result.setPath(TSRMUtil.getAbsPath(uri));
	pathList.add(result);
	
	return pathList;
    }


    private TMetaDataPathDetail listCurrent(File f, TSRMFileListingOption lsOption) {
	//return gov.lbl.srm.storage.Volatile.TDiskDevice.ls(f, lsOption);	
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	try {
	    /*
	    if (!exists(f)){
		return _lsAccess.listAttr(f, lsOption);
	    }

	    if (f.isDirectory()) {
		result.setType(TFileType.DIRECTORY);
	    } else {
		result.setType(TFileType.FILE);
		result.setSize(TSRMUtil.createTSizeInBytes(f.length()));
		Checksum.handle(result, f, lsOption);
	    }
	    
	    if (lsOption.isDetailNeeded() {
		result.setLastModificationTime(TSRMUtil.createGMTTime(f.lastModified()));		
	    }
	    
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk"));
	    */
	    return _lsAccess.listAttr(f, lsOption);
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason:"+e.getMessage()));
	}

	//result.setPath(TSRMTxfProtocol.FILE.generateURI(f).toString());
	result.setPath(TSRMUtil.getAbsPath(f));
	return result;
    }

    // we currently only handle recursivelevel 0, 1, allLevelRecursive
    private TMetaDataPathDetail listMe(File f, TSRMFileListingOption lsOption, int recursiveLevel) {
	TMetaDataPathDetail result = listCurrent(f, lsOption);
	if (result.getType() != TFileType.DIRECTORY) {
	    return result;
	}
	
	if (recursiveLevel == 0) {
	    return result;
	} 

	return _lsAccess.listDir(f,lsOption,recursiveLevel, result);	
    }

}



abstract class  ISRMFileAccess {
    public abstract boolean exists(File f);
    public abstract boolean isDir(File f);
    public abstract boolean isFile(File f);
    public abstract long getLength(File f);
    public abstract TMetaDataPathDetail listAttr(File f, TSRMFileListingOption lsOption);
    public abstract TMetaDataPathDetail listDir(File f, TSRMFileListingOption lsOption, int recursiveLevel, TMetaDataPathDetail  result);

    public abstract TReturnStatus deleteDir(File dir, boolean recursive);
    public abstract TReturnStatus rename(File from, File to); 
    public abstract TReturnStatus makeDir(File f);
    public abstract TReturnStatus rmFile(File  f);

    public void setUid(String uid) {};

    public boolean isUsingFileSystem() {return false;}
    public boolean isUsingGsiftp() {return false;}
}

class TSRMFileAccessGsiftp extends ISRMFileAccess {
    GridFTPClient _gridClient = null;
    //TSupportedURLWithFILE _caller = null;
    TSupportedURL _caller = null;
    public TSRMFileAccessGsiftp(TSupportedURLWithFILE f){
	_caller = f;
    }
	public TSRMFileAccessGsiftp(TSupportedURLWithGSIFTP uri) {
		_caller = uri;
		
		GSSCredential cred = _caller.getCredential();
		if (cred == null) {
			throw new TSRMException("Need credentials for gsiftp access!"+uri.getURLString(), false);
		}
		int port = uri.getPort();
		try {
			if (port < 0) {
				port = 2811;
			}
			//_gridClient = new GridFTPClient(uri.getHost(), port);
			//_gridClient.authenticate(cred);
			_gridClient = gov.lbl.srm.transfer.globus.SRMGridFTP.initGridFTPClient(uri.getHost(), port, cred);
		} catch (Exception e) {
			e.printStackTrace();
			throw new TSRMException("No grid client constructed! host="+uri.getHost()+" port="+port+" err="+e.getMessage(), false);
		}
    }

    public boolean isUsingGsiftp() {return true;}

    public GridFTPClient getClient() {
	return  getGridClient(_caller.getCredential());
    }

    public TReturnStatus rmFile(String p) {
	try {
	    getClient().deleteFile(p);
	    return TSRMUtil.createReturnStatusSuccess(null);
	} catch (Exception e) {
	    e.printStackTrace();
	    return TSRMUtil.createReturnStatusFailed("Cannt delete this file:"+p+" Reason:"+e.getMessage());
	}
    }

    public TReturnStatus rmFile(File f) {
	return rmFile(f.getPath());
    }

    public TReturnStatus makeDir(String p) {
	try {
	    getClient().makeDir(p);	    
	    return TSRMUtil.createReturnStatusSuccess(null);
	} catch (Exception e) {
	    e.printStackTrace();
	    return TSRMUtil.createReturnStatusFailed("Cann't mkdir: "+p+" Reason: "+e.getMessage());
	}
    }

    public TReturnStatus makeDir(File f) {
	TReturnStatus result = makeDir(f.getPath());
	if (result.getStatusCode() != TStatusCode.SRM_SUCCESS) {
	    return result;
	}

	//if (f.isDirectory()) {
	if (isDir(f)) {
	    return TSRMUtil.createReturnStatusSuccess(null);
	} else {
	    return TSRMUtil.createReturnStatusFailed("Unable to mkdir()");
	}

	//return makeDir(f.getPath());
    }

    public TReturnStatus rename(File from, File to) {
	try {	    
	    getClient().rename(from.getPath(), to.getPath());
	    return TSRMUtil.createReturnStatusSuccess("through gsiftp");
	} catch (Exception e) {
	    return TSRMUtil.createReturnStatusFailed("Failed to mv from:"+from.getPath()+" to: "+to.getPath()+" Reason:"+e.getMessage());
        }
    }

    private TReturnStatus deleteDir(File dir) {
	 try {
	     getClient().deleteDir(dir.getPath());
	     return TSRMUtil.createReturnStatusSuccess("deleted through gsiftp");
	 } catch (Exception e) {
	     return TSRMUtil.createReturnStatusFailed("Cannt delete this dir through gsiftp:"+dir.getPath()+" Reason:"+e.getMessage());
	 }
    }

    public TReturnStatus deleteDir(File dir,boolean recursive){
	if (!recursive)  {
	    return deleteDir(dir);
	} 
	String[] contents = dir.list();
	if ((contents == null) || (contents.length == 0)) {
	    return deleteDir(dir);
	}

	for (int i=0; i<contents.length; i++) {
	    //File curr = new File(dir.getPath(), contents[i]);
	    File curr = TSRMUtil.initFile(dir.getPath(), contents[i]);

	    if (curr.isDirectory()) {
		TReturnStatus s = deleteDir(curr, true);
		if (s.getStatusCode() != TStatusCode.SRM_SUCCESS) {
		    return s;
		}
	    } else {
		try {
		    getClient().deleteFile(curr.getPath());
		} catch (Exception e) {
		    return TSRMUtil.createReturnStatusFailed("Cannt delete file: "+curr.getPath()+" Reason:"+e.getMessage());
		}
	    }
	}
	    
	return deleteDir(dir);
	//return TSRMUtil.createReturnStatusSuccess("through gsiftp");
    }

	private String getHost() {
		if (_caller.isProtocolFILE()) {
			URI  uri = TSRMTxfProtocol.GSIFTP.generateURI("dummy");
			return uri.getHost();
		} else {
		 	return _caller.getHost();
		}
	}

	private int getPort() {
		int port = _caller.getPort();
        if (port < 0) {
            port = 2811;
        }
		return port;
	}

    private GridFTPClient getGridClient(GSSCredential cred) {
	if (_gridClient != null) {
	    return _gridClient;
	}
	
	if (cred == null) {
	    throw new TSRMException("Need credentials for gsiftp access."+_caller.getURLString(), false);
	}

	try {	    		
	    //URI uri = TSRMTxfProtocol.GSIFTP.generateURI("dummy");
	    //String host = uri.getHost();
	    //int port = uri.getPort();
	    String host = getHost();
		int port = getPort();

	    //_gridClient = new GridFTPClient(host, port);
	    //_gridClient.authenticate(cred);
	    _gridClient = gov.lbl.srm.transfer.globus.SRMGridFTP.initGridFTPClient(host, port, cred);
	    return _gridClient;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new TSRMException("No grid client constructed."+e.getMessage(), false);
	}
    }

    private TMetaDataPathDetail listRecursively(String parentPath, FileInfo ff, TSRMFileListingOption lsOption, int recursiveLevel) {
	if (!ff.isDirectory()) {
	    return listFileInfo(parentPath, ff, lsOption);
	}
	
	if (recursiveLevel == 0) {
	    return listFileInfo(parentPath, ff, lsOption);
	} 

	if (ff.getName().equals(".")|| (ff.getName().equals(".."))) {
	    return listFileInfo(parentPath, ff,  lsOption);
	}

	TMetaDataPathDetail result = listFileInfo(parentPath, ff, lsOption);
	try {
	    /////GridFTPClient client = getClient();
	    // caant use the existing gridclient, got error "Unexpected Reply 426 Data conenction. Connection_write() failed:
	    // Call local_port or local_spor before calling connect_write.
	    // setting the MODE_EBLOCK did not help reuse the data connection. So have to have a new client here.
	    //
	    //GridFTPClient client= new GridFTPClient("dmx.lbl.gov", 2811);
	    //URI uri = TSRMTxfProtocol.GSIFTP.generateURI("dummy");
	    String host = getHost();
	    int port = getPort();
	    //GridFTPClient client = new GridFTPClient(uri.getHost(),port);
	    //client.authenticate(_caller.getCredential());
	    GridFTPClient client = gov.lbl.srm.transfer.globus.SRMGridFTP.initGridFTPClient(host, port, _caller.getCredential());

	    String currPath = parentPath+"/"+ff.getName();
	    client.changeDir(currPath);
	    //client.setMode(org.globus.ftp.GridFTPSession.MODE_EBLOCK);
	    Vector contents = client.list();
	    if ((contents != null) && (contents.size() > 0)) {
		TMetaDataPathDetail[] m = new TMetaDataPathDetail[contents.size()];
		for (int i=0; i<contents.size(); i++) {
		    org.globus.ftp.FileInfo curr = (org.globus.ftp.FileInfo)(contents.get(i));
		   
		    if (recursiveLevel == 1) {
			m[i] = listFileInfo(currPath, curr, lsOption);
		    } else {
			m[i] = listRecursively(currPath, curr, lsOption, recursiveLevel-1);
		    }
		}	       
		result.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason: "+e.getMessage()));			  
	}

	return result;
    }

    public TMetaDataPathDetail listDir(File f, TSRMFileListingOption lsOption, int recursiveLevel, TMetaDataPathDetail  result) {
	GridFTPClient client = getClient(); 
	try {
	    client.changeDir(f.getPath());
	    Vector contents = client.list();
	    if ((contents != null) && (contents.size() > 0)) {
		TMetaDataPathDetail[] m = new TMetaDataPathDetail[contents.size()];
		for (int i=0; i<contents.size(); i++) {
		    org.globus.ftp.FileInfo ff = (org.globus.ftp.FileInfo)(contents.get(i));
		    
		    if (recursiveLevel == 1) {
			m[i] = listFileInfo(f.getPath(), ff, lsOption);
		    } else {
			m[i] = listRecursively(f.getPath(), ff, lsOption, recursiveLevel-1);
		    }
		}	       
		result.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
	    }
	} catch (Exception e) {
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details:", e);
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason: "+e.getMessage()));	
	}
	return result;
    }

    private TMetaDataPathDetail listFileInfo(String parentPath, FileInfo ff, TSRMFileListingOption lsOption) {
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	try {
	    if (ff.isDirectory()) {
		result.setType(TFileType.DIRECTORY);
	    } else if (ff.isFile()){
		result.setType(TFileType.FILE);
		result.setSize(TSRMUtil.createTSizeInBytes(ff.getSize()));
	    } else {
		result.setType(TFileType.LINK);
	    }
	    
	    if (lsOption.isDetailNeeded() && ff.isFile()) {
		//result.setLastModificationTime(TSRMUtil.createGMTTime(ff.getTime())); // fileinfo only gives a string like "month day year"
	    }
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from gsiftp."));
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason:"+e.getMessage()));
	}

	//result.setPath(TSRMTxfProtocol.FILE.generateURI(parentPath+"/"+ff.getName()).toString());
	result.setPath(TSRMUtil.getAbsPath(parentPath+"/"+ff.getName()));
	return result;
    }

    
    public boolean exists(File f) {
	String path = f.getPath();
	GridFTPClient client = getClient();

	try {
	    boolean result= client.exists(path);
	    return result;
	} catch (Exception e) {
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details:", e);
	    return false;
	}
    }

    public long getLength(File f) {
	String path = f.getPath();
	GridFTPClient client = getClient();

	try {
	    return client.getSize(path);
	} catch (Exception e) {
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details:", e);
	    return -1;
	}
    }

    public boolean isDir(File f) {
	String path = f.getPath();
	GridFTPClient client = getClient();

	try {
	    client.changeDir(path);
	    return true;
	} catch (Exception e) {
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details:", e);
	    return false;
	}
    }

    public boolean isFile(File f) {
	String path = f.getPath();
	GridFTPClient client = getClient();

	if (!exists(f)) {
	    return false;
	}

	try {
	    client.changeDir(path);
	    return false;
	} catch (Exception e) {
	    TSRMLog.exception(TSupportedURLWithFILE.class, "details:", e);
	    return true;
	}
    }

    public TMetaDataPathDetail listAttr(File f, TSRMFileListingOption lsOption) {
	String path  = f.getPath();
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	//result.setPath(TSRMTxfProtocol.FILE.generateURI(f).toString());
	result.setPath(TSRMUtil.getAbsPath(f));
	GridFTPClient client =  getClient();
	if (client == null){
	    return result;
	}

	    if (!exists(f)) {
		result.setStatus(TSRMUtil.createReturnStatusInvalidPath("Does not exist.Checked through gsiftp."));
		return result;
	    } else {
		//TSRMLog.exception(TSupportedURLWithFILE.class,"details:",e);
		result.setStatus(TSRMUtil.createReturnStatusInvalidPath("Failed to list through gsiftp:"));
	    }	
	try {
	    client.changeDir(path);
	    result.setType(TFileType.DIRECTORY);
	} catch (Exception e){
	    result.setType(TFileType.FILE);
	}

	//if (result.getType() == TFileType.FILE) {
	    try {	   
		if (lsOption.isDetailNeeded()){	    
		    result.setLastModificationTime(TSRMUtil.createGMTTime(client.getLastModified(path).getTime()));
		}	    
		if (result.getType() == TFileType.FILE) {
		    result.setSize(TSRMUtil.createTSizeInBytes(client.getSize(path)));
		    Checksum.handle(result, f, lsOption);
		}
	    } catch (Exception e) {
		e.printStackTrace();
		TSRMLog.exception(TSupportedURLWithFILE.class, "failed to getsize/time. Detail:", e);
	    }
	    //	}

	result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk through gsiftp"));
	return result;
    }
}

class  TSRMFileAccessDefault extends ISRMFileAccess {
    String _uid = null;

    public void setUid(String uid) {
	if (TSupportedURLWithFILE._authPolicy != null) {
	    _uid = uid;
	}
    }

    public TSRMFileAccessDefault() {}

    public boolean isUsingFileSystem() {return true;}

    public TReturnStatus rmFile(File f) {
	if (!f.exists()) {
	    return TSRMUtil.createReturnStatusInvalidPath("No such file:"+f.getPath());
	}
	//f.delete();
	deletePath(f);
	if (f.exists()) {
	    return TSRMUtil.createReturnStatusFailed("Failed to delete file:"+f.getPath());
	}
	return TSRMUtil.createReturnStatusSuccess(null);
    }

    public TReturnStatus rename(File from, File to) {
	if (!from.exists()) {
	    return TSRMUtil.createReturnStatusInvalidPath("Source:"+from.getPath()+" No such file or directory");
	}

	if (to.exists()) {
	    return TSRMUtil.createReturnStatusFailed("Target:"+to.getPath()+" already exists. Cann't overwrite. Call rm() first.");
	}

	if (from.renameTo(to)) {
	    return TSRMUtil.createReturnStatusSuccess(null);
	} else {
	    return TSRMUtil.createReturnStatusFailed("Failed to mv from:"+from.getPath()+" to: "+to.getPath());
	}
    }    

    public TReturnStatus makeDir(File  f) {
	if (!f.getParentFile().exists()) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "parent dir does not exist.");
	}
	//if (f.mkdir()){
	if (makePath(f)) {
	    return TSRMUtil.createReturnStatusSuccess(null);
	} else {
	    return TSRMUtil.createReturnStatusFailed("Cann't mkdir: "+f.getPath());
	}
    }

    private TReturnStatus deleteDir(File dir) {
	if (!dir.exists()){
	    return TSRMUtil.createReturnStatusInvalidPath("No such dir:"+dir.getPath());
	}

	if (!dir.isDirectory()) {
	    return TSRMUtil.createReturnStatusFailed("path is a file. Call rm()");
	}
	
	//if (dir.delete()) { 
	if (deletePath(dir)) {
	    return TSRMUtil.createReturnStatusSuccess(null);
	} else {
	    return TSRMUtil.createReturnStatusFailed("delete("+dir.getPath()+") failed.");
	}
    }

    public TReturnStatus deleteDir(File dir, boolean recursive){
	if (!recursive) {
	    return deleteDir(dir);
	}

	if (!dir.exists()) {
	    return TSRMUtil.createReturnStatusInvalidPath("No such file or directory.");
	}
	/*
	if (!dir.canWrite()) {
	    return TSRMUtil.createReturnStatusFailed("No permission to delete: "+dir.getPath());
	}
	*/

	String[] contents = dir.list();
	if (contents == null) {
	    return deleteDir(dir);
	}

	for (int i=0; i<contents.length; i++) {
	    //File curr = new File(dir.getPath(), contents[i]);
	    File curr = TSRMUtil.initFile(dir.getPath(), contents[i]);
	    if (curr.isDirectory()) {
		TReturnStatus s = deleteDir(curr, true);
		if (s.getStatusCode() != TStatusCode.SRM_SUCCESS) {
		    return s;
		}
	    } else if (!deletePath(curr)) { //(!curr.delete()) {
		return TSRMUtil.createReturnStatusFailed("Cannt delete file: "+curr.getPath());
	    }
	}
	
	return deleteDir(dir);	
    }

    public boolean exists(File f) {
	return f.exists();
    }

    public long getLength(File f) {
      if (!f.exists()) {
	return -1;
      }
      return f.length();
    }
    public boolean isDir(File f) {
	return f.isDirectory();
    }
    
    public boolean isFile(File f) {
	return f.isFile();
    }

    public TMetaDataPathDetail listAttr(File f, TSRMFileListingOption lsOption) {
	TSRMLog.debug(this.getClass(), null, "event=listAttrStarts", f.getPath());
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	try {
	    if (!f.exists()) {
		TSRMLog.error(this.getClass(), null, "event=listAttr msg=\"Does not exist on disk\"", "path="+f.getPath());
		result.setStatus(TSRMUtil.createReturnStatusInvalidPath("No such file or directory."));
		return result;
	    }
	    if (f.isDirectory()) {
		result.setType(TFileType.DIRECTORY);
	    } else {
		result.setType(TFileType.FILE);
		result.setSize(TSRMUtil.createTSizeInBytes(f.length()));		
		Checksum.handle(result, f, lsOption);
	    }
	    
	    if (lsOption.isDetailNeeded() /*&& f.isFile()*/) {
		result.setLastModificationTime(TSRMUtil.createGMTTime(f.lastModified()));		
	    }

	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk.."));
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason:"+e.getMessage()));
	} finally {
	    TSRMLog.debug(this.getClass(), null, "event=listAttrEnds", f.getPath());
	    //result.setPath(TSRMTxfProtocol.FILE.generateURI(f).toString());
	    result.setPath(TSRMUtil.getAbsPath(f));
	    return result;
	}
    }

    public TMetaDataPathDetail listDir(File f, TSRMFileListingOption lsOption, int recursiveLevel, TMetaDataPathDetail result) {
	File[] contents = f.listFiles();

	if ((contents != null) && (contents.length > 0)) {
	    TMetaDataPathDetail[] m = new TMetaDataPathDetail[contents.length];
	    for (int i=0; i<contents.length; i++) {
		m[i] = listAttr(contents[i], lsOption);
		if (contents[i].isDirectory()) {
		    if (recursiveLevel != 1) {
			m[i] = listDir(contents[i], lsOption, recursiveLevel-1, m[i]);
		    } 
		}
	    }
	    result.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
	} else {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Empty dir read from disk"));		  	    
	}
	return result;
    }

    private boolean deletePath(File path) {
	if (TSupportedURLWithFILE._authPolicy == null) {
	    return path.delete();
	}
	return TSupportedURLWithFILE._authPolicy.delete(_uid, path.getPath());
    }

    private boolean makePath(File path) {	
	if (TSupportedURLWithFILE._authPolicy == null) {
	    return path.mkdir();
	}
	return TSupportedURLWithFILE._authPolicy.mkdir(_uid, path.getPath());
    }
}

class  TSRMFileAccessSudo extends ISRMFileAccess {
    String _uid = null;

    public void setUid(String uid) {
	_uid = uid;
    }

    public boolean exists(File f) {
	return f.exists();
    }

    public long getLength(File f) {
      if (!f.exists()) {
	return -1;
      }
      return f.length();
    }

    public boolean isDir(File f) {
	return f.isDirectory();
    }
    
    public boolean isFile(File f) {
	return f.isFile();
    }

    public TMetaDataPathDetail listAttr(File f, TSRMFileListingOption lsOption) {
	String path  = f.getPath();
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	//result.setPath(TSRMTxfProtocol.FILE.generateURI(f).toString());
	result.setPath(TSRMUtil.getAbsPath(f));

	String uid= lsOption.getUid();

	String op = "/bin/ls -l --time-style=long-iso ";
	String sudoCommand = "sudo -u "+uid+" "+op+path;

	String out = TPlatformUtil.execCmdWithOutput(sudoCommand, true);
	//System.out.println("####"+sudoCommand+"  =>"+out);
	if (out == null) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Read nothing from disk through sudo"));
	} else if (out.startsWith("ERROR")) {
		TSRMLog.info(this.getClass(), null, "event=sudoFailed", "Command:["+sudoCommand+"] failed. \nError:"+out);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, null));
	} else {
	     if (out.startsWith("total")) {
		result.setType(TFileType.DIRECTORY);
	    } else {
		String[] splitted = out.split(" +");
		result.setType(TFileType.FILE);
		result.setSize(TSRMUtil.createTSizeInBytes(Long.parseLong(splitted[4])));	    	
		Checksum.handle(result, f, lsOption);
	
		if (lsOption.isDetailNeeded()){	    
		  // long-iso style
		  if (splitted.length == 8) {
		      result.setLastModificationTime(TSRMUtil.createGMTTime(splitted[5],splitted[6]));
		  }
		}
	    }
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk through sudo."));
	}
	return result;
    }

    public TMetaDataPathDetail listDir(File f, TSRMFileListingOption lsOption, int recursiveLevel, TMetaDataPathDetail  result){
	String uid= lsOption.getUid();

	//String op = "/bin/ls -l ";
	String op = "/bin/ls -l --time-style=long-iso ";
	String sudoCommand = "sudo -u "+uid+" "+op+f.getPath();
	String out = TPlatformUtil.execCmdWithOutput(sudoCommand, true);
	if (out == null) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Read nothing from disk through sudo"));
	} else if (out.startsWith("ERROR")) {
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Command:["+sudoCommand+"] failed. \nError:"+out));
	} else {
	    if (!out.startsWith("total")) {
		result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Unexpected output from sudo..Expecting total  but got:"+out));
	    } else if (out.charAt(6) == '0') {
		result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "empty dir."));
	    } else {
		int numEntries = -1;
		int pos = out.indexOf(" ", 6);
		if (pos == -1) {
		    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "empty dir from ls."));
		} else {
		    String[] entries = out.substring(pos+1).split(" +");
		    numEntries = entries.length/8;
		    TMetaDataPathDetail[] m =new TMetaDataPathDetail[numEntries];
		    
		    for (int i=0; i<numEntries; i++) { 
			String name = entries[i*8+7];
			m[i] = new TMetaDataPathDetail();
			if (entries[8*i].startsWith("d")) {
			    m[i].setType(TFileType.DIRECTORY);
			} else {
			    m[i].setType(TFileType.FILE);
			    m[i].setSize(TSRMUtil.createTSizeInBytes(Long.parseLong(entries[i*8+4])));
			}
			m[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "read through sudo.."));
			//m[i].setPath(TSRMTxfProtocol.FILE.generateURI(f.getPath()+"/"+name).toString());
			m[i].setPath(TSRMUtil.getAbsPath(f.getPath()+"/"+name));
			if ((recursiveLevel != 1) && (m[i].getType() == TFileType.DIRECTORY)) {
			    //m[i] = listDir(new File(f, name), lsOption, recursiveLevel-1, m[i]);
			    m[i] = listDir(TSRMUtil.initFile(f, name), lsOption, recursiveLevel-1, m[i]);
			} 			    			
		    }
		    result.setArrayOfSubPaths(TSRMUtil.convertToArray(m));
		}
	    }
	}
	return result;
    }


    private TReturnStatus invokeSudo(String input, String uid, String command) {
	String sudoCommand = "sudo -u "+uid+" "+command;
	sudoCommand += input;
	TSRMUtil.startUpInfo("invokeSudo=["+sudoCommand+"]");

	String output = TPlatformUtil.execCmdWithOutput(sudoCommand, false);
	if (output == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	} else {
	    //return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Command:["+sudoCommand+"] failed. \nError:"+output);
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Error:"+output+"\nRef"+sudoCommand.substring(5));
	}
    }

    private TReturnStatus deleteDir(File dir) {
	return invokeSudo(dir.getPath(), _uid, " /bin/rmdir  ");
    }

    public TReturnStatus deleteDir(File dir, boolean recursive){
	if (!recursive)  {
	    return deleteDir(dir);
	}
	return invokeSudo(dir.getPath(), _uid, " /bin/rm -R ");
    }

    public TReturnStatus rename(File from, File to) {
       return invokeSudo(from.getPath()+" "+to.getPath(), _uid, " /bin/mv ");
    }

    public TReturnStatus makeDir(File f) {	
       return invokeSudo(f.getPath(), _uid, " /bin/mkdir ");
    }

    public TReturnStatus rmFile(File  f) {
	return invokeSudo(f.getPath(), _uid, " /bin/rm ");
    }
};
