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

import java.io.File;
 
import java.util.*;
import gov.lbl.srm.util.*;
 
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import gov.lbl.srm.storage.*;
import org.apache.axis.types.URI;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMLocalDir implements ISRMLocalPath {
    ///private String _name 				= null; // dont want name to be empty
    private TSRMLocalDir _parent 		= null;
    ///private TSURLInfo _siteURL = null;
    private TSRMMutex _accessDirMutex = new TSRMMutex();
    private TSRMMutex _accessFileMutex = new TSRMMutex();
    ///public boolean                           _isObsolete                = false;

    //
    // we use 2 hashmaps here for ease of doing ls() width
    //
    private HashMap _contentDirs 			= new HashMap();
    private HashMap _contentFiles 			= new HashMap();
    
    private TMetaDataPathDetail _detail 	= new TMetaDataPathDetail();
    private TSRMLocalPathAttributes _attributes = new TSRMLocalPathAttributes();

    private TAccount _owner = null;
    
    public TSRMLocalDir(TSRMLocalDir parent, String name, boolean isSurlPath) {
	if (name == null) {
	    throw new RuntimeException("Directory name can not be null");
	}
	//_name = name;
	_attributes.setName(name);
	_parent = parent;
	
	// other members of detail will be filled in later
	_detail.setPath(getSiteURL(false).getSURL().toString());
	_detail.setType(TFileType.DIRECTORY);

	if (isSurlPath) {
	    _attributes.setSurlPath();
	    TSRMLog.debug(this.getClass(), null, "event=createdDir", "path="+getCanonicalPath()+" type=surl");	    
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=createdDir", "path="+getCanonicalPath()+" type=turl");
	}
	_attributes.setTimeStamp();
    }

    public void setOwner(TAccount user) {
	_owner = user;
    }

    public TAccount getOwner() {
	if (getParent() != null) {
	    return getParent().getOwner();
	} else {
	    return _owner;
	}
    }

    public boolean isDir() {
	return true;
    }
   
    public TSRMLocalDir getParent() {
	return _parent;
    }
    
    // immediate subdir
    public TSRMLocalDir getSubDir(String name) {
	TSRMLocalDir result = null;
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    result = (TSRMLocalDir)(_contentDirs.get(name));
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
	return result;
    }
    
    public TSRMLocalFile getFile(String name) {
	TSRMLocalFile result = null;
	if (name == null) {
	    return null;
	}

	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    result = (TSRMLocalFile)(_contentFiles.get(name));
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
	return result;
    }
    
    public ISRMLocalPath getPath(String name) {
	TSRMLocalDir d = getSubDir(name);
	if (d == null) {
	    return getFile(name);
	} else {
	    return d;
	}
    }
    
    public boolean isSurl() {
	return _attributes.isSurlPath();
    }

    public String getName() {
	return _attributes.getName();
    }
    
    public boolean isNameUsedInContents(String n) {
	/*
	if (_contentDirs.get(n) != null) {
	    return true;
	}
	if (_contentFiles.get(n) != null) {
	    return true;
	}
	*/

	if (getSubDir(n) != null) {
	    return true;
	}

	if (getFile(n) != null) {
	    return true;
	}
	return false;
    }
    
    public String getAlternateContentName(String n) {
	int counter=0;
		 
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    while (true) {
		if (_contentFiles.get(n+"_"+counter) == null) {
		    break;
		}
		counter++;
	    }
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
	return n+"_"+counter;
    }
    
    public String getCanonicalPath() {
	if (_parent == null) {
	    return File.separatorChar+getName()+File.separatorChar;
	} else {			 
	    return _parent.getCanonicalPath()+getName()+File.separatorChar;
	}
    }     

    public TSRMLocalFile createSubFile(TFileStorageType fileStorageType, TSupportedURL url, boolean isSurl) {
	String name = url.getEndFileName();
	TSRMLocalFile localDestination = createSubFile(name, fileStorageType, isSurl);
	localDestination.setSourceURI(url.getURI());
	subFileCreated(localDestination);
	return localDestination;
    }

    public TSRMLocalFile createSubFile(String name, TFileStorageType fileType, boolean isSurl) {
	TSRMLocalFile existingFile = getFile(name);

	if (existingFile != null) {
	    if ((fileType != null) && (existingFile.getFileStorageType() != null) && (existingFile.getFileStorageType() != fileType)) {
		TSRMException ex = new TSRMException("A file exists but with a different storage type", false);
		TSRMLog.exception(this.getClass(), "details", ex);
		throw ex;
	    }
	    return existingFile;
	}
	
	if (name == null) {
	    name = System.currentTimeMillis()+Thread.currentThread().getName();
	}

	TSRMLocalFile f = new TSRMLocalFile(this, name, fileType, isSurl);
	subFileCreated(f);
	return f;
    }
    
    protected void subFileCreated(TSRMLocalFile f) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {	    
	    _contentFiles.put(f.getName(), f);
	    TSRMLog.debug(this.getClass(), null, "event=createdSubfile path="+f.getCanonicalPath(), null);
	    TSRMUtil.releaseSync(_accessFileMutex);
	} else {
	    throw new TSRMException("Cannt read file:"+f.getName()+" from:"+getCanonicalPath(), false);
	}
    }

    public TSRMLocalDir createDir(String name, boolean isSurl) {
	if (name == null) {
	    throw new TSRMException("Null name for dir", false);
	}
	
	TSRMLocalDir  sub = getSubDir(name);
	if (sub != null) {
	    return sub;
	}
	
	TSRMLocalDir d = new TSRMLocalDir(this, name, isSurl);
	subDirCreated(d);
	return d;
    }   

    protected void subDirCreated(TSRMLocalDir d) {
	if (TSRMUtil.releaseSync(_accessDirMutex)) {
	    _contentDirs.put(d.getName(), d);
	    TSRMUtil.releaseSync(_accessDirMutex);
	} else {
	    throw new TSRMException("Cannt create subdir: "+d.getName()+" from:"+getCanonicalPath(), false);
	}
    }

    public boolean isEmpty() {
	return (_contentFiles.size() == 0) && (_contentDirs.size() == 0);
    }
    
    public TReturnStatus delete(boolean recursiveOn, boolean doSetStatusRelease) {
	if (getParent() == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "wont remove top dir");
	} 

	boolean isEmpty = isEmpty();
	if (isEmpty) {	    	 
	    cleanContent(doSetStatusRelease);
	    getParent().rmDirName(getName());
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);	    
	} else {
	    if (!recursiveOn) {
		if (isNotEffectivelyEmpty()) {
		    return TSRMUtil.createReturnStatus(TStatusCode.SRM_NON_EMPTY_DIRECTORY, "dir is not empty, cannt remove.");
		}		
	    } 
	    cleanContent(doSetStatusRelease);
	    if (getParent() == null) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "wont remove top dir");
	    } else {
		getParent().rmDirName(getName());
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	    }
	}
    }

    private boolean isNotEffectivelyEmpty() {
	if (_contentDirs.size() > 0) {
	    return true;
	}
	if (_contentFiles.size() > 0) {
	    java.util.Set keyset = _contentFiles.keySet();
	    java.util.Iterator iter = keyset.iterator();
	    
	    while (iter.hasNext()){
		TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		if (curr.isReady()) {
		    if (curr.isValid()) {
			return true;
		    }
		} else {
		    return true;
		}
	    }
	    return false;
	}
	return false;
    }


    public void cleanContent(boolean doSetStatusRelease) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    if (_contentFiles.size() > 0) {
		java.util.Set keyset = _contentFiles.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		    //curr.deleteMe();
		    curr.unsetToken(doSetStatusRelease);
		}
		
		_contentFiles.clear();
	    }
	    TSRMUtil.releaseSync(_accessFileMutex);
	}

	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    if (getNumSubDirs() > 0) {
		java.util.Set keyset = _contentDirs.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		    curr.cleanContent(doSetStatusRelease);
		}
	    
		_contentDirs.clear();
	    }
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
	TSRMLog.debug(this.getClass(), null, "event=cleanContent path="+getCanonicalPath(), null);

	// need to consider removing physical dir when HPSS is in concern..
	// will think about it later
    }
    
    public void rmDirName(String name) {
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    TSRMLog.debug(this.getClass(), null, "event=rmDir path="+getCanonicalPath(), "name="+name);	    
	    TSRMLocalDir subdir = (TSRMLocalDir)(_contentDirs.remove(name));
	    if (subdir == null) {
	         TSRMLog.debug(this.getClass(), null, "event=rmDir path="+getCanonicalPath(), "name="+name+" result=failed");
	    }
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }
    
    public void rmFileName(String name) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    Object obj = _contentFiles.remove(name);
	    if (obj != null) {
		TSRMLog.debug(this.getClass(), null, "event=rmFileName name="+name, "result=succ");	
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=rmFileName name="+name, "result=failed");	
	    }
	    obj = null;
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
    }
	
    public TMetaDataPathDetail getMetaData(boolean withFullDetail) {
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	
	//result.setPath(_detail.getPath());
	result.setPath(TSRMUtil.getAbsPath(getSiteURL().getSURL()));
	result.setType(_detail.getType());
	result.setSize(TSRMUtil.createTSizeInBytes(0));
	
	if (withFullDetail) {
	    result.setOtherPermission(TSRMUtil.createDefaultClientPermission());
	    result.setGroupPermission(TSRMUtil.createDefaultGroupPermission());
	    result.setOwnerPermission(TSRMUtil.createDefaultOwnerPermission(getTopDir().getName()));
	    result.setLastModificationTime(TSRMUtil.createGMTTime(0)); // donnt track it
	}
	result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	if  (_attributes.getCreatedAt() > 0) {
	    result.setCreatedAtTime(TSRMUtil.createGMTTime(_attributes.getCreatedAt()));
	}
	return result;
    }
    
    public Vector ls(TFileStorageType fileStorageType,// considerred		 
		     TSRMFileListingOption lsOption)
    {
	boolean doDetails = lsOption.isDetailNeeded();

	int nLevels = lsOption.getRecursiveLevel();

	int nCount = lsOption.getOutputCount();

	int nOffset = lsOption.getOutputOffset();
	TSRMLog.debug(this.getClass(), null, "event=ls path="+getCanonicalPath(), "fileStorage="+fileStorageType+" doDetails="+doDetails+" nLevels="+nLevels+" nCount="+nCount);
	
	Vector resultList = new Vector();

	/*if (nCount == 0) {
	    return resultList;
	}*/

	if ((nLevels == 0) || (nCount == 1) || (nCount == 0)) {
	    TMetaDataPathDetail result = getMetaData(doDetails);	    
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "level="+nLevels+" count="+nCount));
	    resultList.add(result);		    
	    return resultList;
	}
	if (isEmpty()) {
	    TMetaDataPathDetail result = getMetaData(doDetails);	    
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "empty dir."));
	    resultList.add(result);		    
	    return resultList;
	}

	TSRMBrowseDirBreathFirst browser = list(nLevels, fileStorageType, doDetails, nCount, nOffset);
	if (browser == null) {
	    TMetaDataPathDetail result = getMetaData(doDetails);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_TOO_MANY_RESULTS, null));
	    resultList.add(result);		    
	} else {
	    //resultList.addAll(browser.getResultCollection());

	    int resultSize=browser.getResultCollection().size();
	    //System.out.println("  TSRMLocalDir: resultSize="+resultSize+"  count ="+browser._iCount+" offset="+browser._iOffset);
	    TSRMLog.debug(this.getClass(), null, "event=ls", "rs="+resultSize+" count="+browser._iCount+" offset="+browser._iOffset);
	    if (browser.hasReachedOffsetLimit()) {
		lsOption.resetOffset();
	    }
	    if (browser.hasReachedCountLimit()) {
		//TMetaDataPathDetail result = getMetaData(doDetails);
		//result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_TOO_MANY_RESULTS, null));
		//result.setArrayOfSubPaths(browser.getResultAsArray());
		//resultList.add(result);
		resultList.addAll(browser.getResultCollection());
		lsOption.resetCount(0);
	    } else {
		resultList.addAll(browser.getResultCollection());
		lsOption.resetCount(browser._iCount);
	    }
	}
	return resultList;
	
    }
   

    private TSRMBrowseDirBreathFirst list(int recursiveLevels, // -1 for allLevelRecursive
					  TFileStorageType fileStorageType, // null means list all types
					  boolean doDetails,
					  int iCount,
					  int iOffset)
    {
	TSRMBrowseDirBreathFirst browser = new TSRMBrowseDirBreathFirst(doDetails, recursiveLevels, iCount, iOffset, fileStorageType);
	browser.addDirToBrowse(this);
	browser.browse();
	//result.setSubPaths(browser.getResultAsArray());
	
	//what about size limit?
	if (browser.reachedLimit()) {
	    return null;
	} else {
	    return browser;
	}
    }	 

	
    public void printContent() {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    if (_contentFiles.size() > 0) {
		java.util.Set keyset = _contentFiles.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		TSRMUtil.startUpInfo("==> file contents of: "+this+" "+getCanonicalPath());
		while (iter.hasNext()){
		    TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		TSRMUtil.startUpInfo("\t"+curr.getName());
		}
		TSRMUtil.startUpInfo("<==");
	    } else {
		TSRMUtil.startUpInfo("==> no file is in:"+this+"  "+getCanonicalPath());
	    }
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
	

	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    if (getNumSubDirs() > 0) {
		java.util.Set keyset = _contentDirs.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		TSRMUtil.startUpInfo("==> dir contents of:"+this+" "+getCanonicalPath());
		while (iter.hasNext()){
		    TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		    TSRMUtil.startUpInfo("\t"+curr.getName());
		}
		TSRMUtil.startUpInfo("<==");		   
	    } else {
		TSRMUtil.startUpInfo("==> no sub dir is in:"+this+"  "+getCanonicalPath());
	    }
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }
    
    public Vector listFilesOfType(TFileStorageType fileStorageType, boolean doDetails) {
	Vector v = new Vector();
	
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    try {
		java.util.Set keyset = _contentFiles.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		    if (curr.isSurl()) {						
			if ((fileStorageType == null) || (curr.getFileStorageType() == fileStorageType)) {			
			    v.addAll(curr.ls(fileStorageType, 
					     new TSRMFileListingOption(null, null, Boolean.valueOf(doDetails), null, null)));
			}
		    }
		}
	    } catch (TSRMException e) {
		throw e;
	    } finally {
		TSRMUtil.releaseSync(_accessFileMutex);		
	    }
	}
	return v;
    }
	 
    public URI getTxfURI(TSRMTxfProtocol protocol) {
	return null; 
    }
    
    public TSRMPin getPin(TSRMRequest r) {
	return null;
    }

    private void copyDirs(TSRMLocalDir tgtDir, int recursiveLevel, 
			  TSRMStorage.iSRMSpaceToken spaceToken, 
			  TDeviceAccessInfo accessInfo,
			  TSRMRequestCopyLocal req) 
    {
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    try {
		if (getNumSubDirs() > 0) {
		    java.util.Set keyset = _contentDirs.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
			TSRMLocalDir tgt = tgtDir.createDir(curr.getName(), curr.isSurl());

			curr.copyTo(tgt, recursiveLevel, spaceToken, accessInfo, req);
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessDirMutex);
	    }
	}	
    }

     private void copyFiles(TSRMLocalDir tgtDir, 
			    TSRMStorage.iSRMSpaceToken spaceToken, 
			    TDeviceAccessInfo accessInfo,
			    TSRMRequestCopyLocal req) 
    {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    try {
		if (_contentFiles.size() > 0) {
		    java.util.Set keyset = _contentFiles.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
			TFileStorageType fileType = null;

			TSRMLocalFile tgt = tgtDir.createSubFile(curr.getName(), fileType, curr.isSurl());
			tgt.setToken(spaceToken);

			TSRMNameSpace.addEntry(tgt.getSiteURL().getSURL().toString(), tgt, req);

			TSupportedURL url = TSupportedURL.create(curr.getSiteURL(false));
			if (url.isProtocolSRM()) {
			    TSupportedURLWithSRM surl = (TSupportedURLWithSRM)url;
			    if (surl.isLocalToThisSRM()) {
				TSupportedURLWithSRMRefToLocal local = (TSupportedURLWithSRMRefToLocal)surl;
				local.setCorrespondingPath(curr);
			    }
			}
			tgt.copyFrom(curr, url, accessInfo);
			tgt.updateToken();
			//this mutex becomes deadlocked with reftolocal.getcorrespondingpath()
			
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessFileMutex);
	    }
	}
    }

    private long getFileSizes() {
	long result = 0;
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    try {
		if (_contentFiles.size() > 0) {
		    java.util.Set keyset = _contentFiles.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
			long currSize = curr.getCurrentSize();

			if (currSize >= 0) {
			    result += currSize;
			}
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessFileMutex);
	    }
	}

	return result;
    }

    private long getDirSizes(int useRecursiveLevel) {
	long result = 0;

	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    try {
		if (getNumSubDirs() > 0) {
		    java.util.Set keyset = _contentDirs.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
			result += curr.getCurrentSize(useRecursiveLevel);
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessDirMutex);
	    }
	}	

	return result;
    }

    public long getCurrentSize(int recursiveLevel) {
	long result = 0;
	result += getFileSizes();

	if (recursiveLevel == 0) {
	    return result;
	} else if (recursiveLevel > 0) {
	    result += getDirSizes(recursiveLevel -1);
	} else {
	    result += getDirSizes(recursiveLevel);
	}
	
	return result;
    }

    private void manageAllDirs(TStatusCode pinCode, TSRMRequest r, int recursiveLevel) {
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    try {
		if (getNumSubDirs() > 0) {
		    java.util.Set keyset = _contentDirs.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
			if (pinCode != null) {
			    curr.pin(r, recursiveLevel, pinCode);
			} else {
			    curr.unpin(r, recursiveLevel);
			}
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessDirMutex);
	    }
	}	
    }

    private void manageAllFiles(TStatusCode pinCode, TSRMRequest r) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    try {
		if (_contentFiles.size() > 0) {
		    java.util.Set keyset = _contentFiles.keySet();
		    java.util.Iterator iter = keyset.iterator();
		    
		    while (iter.hasNext()){
			TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
			if (pinCode != null) {
			    curr.pin(r, pinCode);
			} else {
			    curr.unpin(r);
			}
		    }
		}
	    } catch (Exception e) {	        
		TSRMLog.exception(TSRMLocalDir.class, "details", e);
	    } finally {
		TSRMUtil.releaseSync(_accessFileMutex);
	    }
	}
    }

    public void pin(TSRMRequest r, int recursiveLevel, TStatusCode code) {
	manageAllFiles(code, r);

	if (recursiveLevel == 0) {
	    return;
	} else if (recursiveLevel > 0) {
	    recursiveLevel = recursiveLevel -1;
	}

	manageAllDirs(code, r, recursiveLevel);
    }
    
    public void unpin(TSRMRequest r, int recursiveLevel) {
	manageAllFiles(null, r);

	if (recursiveLevel == 0) {
	    return;
	} else if (recursiveLevel > 0) {
	    recursiveLevel = recursiveLevel -1;
	}

	manageAllDirs(null, r, recursiveLevel);
    }
    
    public void copyTo(TSRMLocalDir tgtDir, 
		       int recursiveLevel, 
		       TSRMStorage.iSRMSpaceToken spaceToken, 
		       TDeviceAccessInfo accessInfo,
		       TSRMRequestCopyLocal req) 
    {
	copyFiles(tgtDir, spaceToken, accessInfo, req);

	if (recursiveLevel == 0) {
	    return;
	} else if (recursiveLevel > 0) {
	    recursiveLevel = recursiveLevel -1;	    
	}
	copyDirs(tgtDir, recursiveLevel, spaceToken, accessInfo, req);
    }   
    
    public int getNumSubDirs() {
	return _contentDirs.size();
    }
    
    public Collection getSubDirCollection() {
	return _contentDirs.values();
    }	       
    
    public TSRMLocalDir getTopDir() {
	if (getParent() != null) {
	    return getParent().getTopDir();
	} else {
	    return this;
	}
    }
    
    public void addFileCollection(Map cols) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    _contentFiles.putAll(cols);
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
    }
    
    public void addDirCollection(Map cols) {
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    _contentDirs.putAll(cols);
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }
    
    public void changeParentTo(TSRMLocalDir p) {
	_parent = p;
	_detail.setPath(getSiteURL(true).getSURL().toString());
	
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    if (_contentFiles.size() > 0) {
		java.util.Set keyset = _contentFiles.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		    curr.resetPath();
		}
	    }
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
	
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    if (getNumSubDirs() > 0) {
		java.util.Set keyset = _contentDirs.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		    curr.changeParentTo(this);
		}
	    }
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }
    
    private void addPath(ISRMLocalPath p) {
	if (p == null) {
	    return;
	}
	if (p.isDir()) {
	    addDir((TSRMLocalDir)p);
	} else {
	    addFile((TSRMLocalFile)p);
	}
    }
    
    public void rename(String newName) {	    
	if (newName != null) {
	    _attributes.setName(newName);
	}
	_detail.setPath(getSiteURL(true).getSURL().toString());    	
    }
    
    private void updateFootprint(boolean addMe) {
	 if (_contentFiles.size() > 0) {
	     java.util.Set keyset = _contentFiles.keySet();
	     java.util.Iterator iter = keyset.iterator();
	     
	     while (iter.hasNext()){
		 TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		 if (addMe) {
			 if (TSRMLog.getCacheLog() != null) {
		     TSRMLog.getCacheLog().addFile(curr);
			 }
		 } else {
		     //TSRMLog.getCacheLog().removeFile(curr);
			 if (TSRMLog.getCacheLog() != null) {
		     TSRMLog.getCacheLog().removeEntry(curr);
			 }
		 }
	     }
	 }

	 if (getNumSubDirs() > 0) {
	     java.util.Set keyset = _contentDirs.keySet();
	     java.util.Iterator iter = keyset.iterator();
	     
	     while (iter.hasNext()){
		 TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		 curr.updateFootprint(addMe);
	     }	   
	 }
    }

    public void move(ISRMLocalPath from, String tgtName) {
	if (!from.isDir()) {
		if (TSRMLog.getCacheLog() != null) {
	    TSRMLog.getCacheLog().removeEntry((TSRMLocalFile)from);
		}
	} else {
	    ((TSRMLocalDir)from).updateFootprint(false);
	}
		
	TSRMLocalDir sub=getSubDir(tgtName);
	from.detachFromParent();
	if (sub != null) {
	    sub.addPath(from);
	    from.rename(from.getName());	    
	} else {
	    from.rename(tgtName);
	    this.addPath(from);	    
	}

	if (!from.isDir()) {
		if (TSRMLog.getCacheLog() != null) {		
	    TSRMLog.getCacheLog().addFile((TSRMLocalFile)from);
		}
	} else {
	    ((TSRMLocalDir)from).updateFootprint(true);
	}
    }
  
    public void detachFromParent() {
	if (getParent() != null) {
	    getParent().rmDirName(this.getName());
	}
    }   
    
    public TSURLInfo getSiteURL() {
	return getSiteURL(false);
    }

    public TSURLInfo getSiteURL(boolean forceToRegenerate) {
	return _attributes.generateSiteURL(getCanonicalPath(), forceToRegenerate);
    }

    public Vector releaseSurl() {
	Vector result = new Vector();

	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    java.util.Set keyset = _contentFiles.keySet();
	    java.util.Iterator iter = keyset.iterator();
	    
	    while (iter.hasNext()){
		TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		if (curr.isSurl()) {
		    result.addAll(curr.releaseSurl());
		}
	    }
	    TSRMUtil.releaseSync(_accessFileMutex);
	}

	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    if (getNumSubDirs() > 0) {
		java.util.Set keyset = _contentDirs.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		    result.addAll(curr.releaseSurl());
		}
	    }
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
	return result;
    }

    public void addFile(TSRMLocalFile f) {
	if (TSRMUtil.acquireSync(_accessFileMutex)) {
	    _contentFiles.put(f.getName(), f);
	    f.changeParentTo(this);
	    TSRMUtil.releaseSync(_accessFileMutex);
	}
    }
    
    public void addDir(TSRMLocalDir d) {
	if (TSRMUtil.acquireSync(_accessDirMutex)) {
	    _contentDirs.put(d.getName(), d);
	    d.changeParentTo(this);
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }

    public void populateTo(TSRMSourceDir dir) {
	if (!TSRMUtil.acquireSync(_accessDirMutex)) {
	    return;
	}

	try {
	    if (_contentFiles.size() > 0) {
		java.util.Set keyset = _contentFiles.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalFile curr = (TSRMLocalFile)(_contentFiles.get(iter.next()));
		    dir.addFile(curr, true);
		}
	    }

	    if (getNumSubDirs() > 0) {
		java.util.Set keyset = _contentDirs.keySet();
		java.util.Iterator iter = keyset.iterator();
		
		while (iter.hasNext()){
		    TSRMLocalDir curr = (TSRMLocalDir)(_contentDirs.get(iter.next()));
		    dir.addDir(curr, true);
		}	   
	    }
	} catch (Exception e) {	    
	    TSRMLog.exception(TSRMLocalDir.class, "details", e);
	} finally {
	    TSRMUtil.releaseSync(_accessDirMutex);
	}
    }

public void useCredential(org.ietf.jgss.GSSCredential cred) {}
}


