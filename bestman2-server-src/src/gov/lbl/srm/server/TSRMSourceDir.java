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
import java.util.Vector;
//
// recursiveLevel:o = all recursive
//
public class TSRMSourceDir extends TSRMSource {
    private int _levelsOfRecursiveness = 0;
    private Vector _contents = new Vector();
    protected TSRMLocalDir _localResidenceDir = null;
    private long _trustedSize = -1;
    private boolean _blur = false;  // only need generic file/dir classes
    
    public TSRMSourceDir(TSURLInfo info) {
	super(info);
    }
    
    public TSRMSourceDir(TSupportedURL info) {
	super(info);
    }   

    public void setBlur() {
	_blur = true;
    }

    public static TSRMSourceDir create(TSupportedURL info, TAccount caller) {
	if (info.isDeviceSpecific()) {
	    return new TSRMSourceDirUserOwned(info, caller);
	} else if (info.isProtocolFILE()) {
	    return new TSRMSourceDirUserOwned(info, caller);
	} else {
	    return new TSRMSourceDir(info);
	}
    }

    public void setTrustedSize(long s) {
	_trustedSize = s;
    }

    public long getTrustedSize() {
	if (_trustedSize >= 0) {
	    return _trustedSize;
	}
	if (_contents.size() == 0) {
	    _trustedSize = getSourceURL().getTrustedSize(getRecursiveLevel());
	} else if (_trustedSize == -1) {
	    for (int i=0; i<_contents.size(); i++) {
		TSRMSource curr = (TSRMSource)(_contents.get(i));
		_trustedSize +=curr.getTrustedSize();
	    }
	} 
	return _trustedSize;	
    }

    private TSURLInfo createSURLInfo(String subpath) {
	String p = TSRMUtil.createPath(getSourceURL().getURLString(), subpath);
	TSURLInfo info = TSRMUtil.createTSURLInfo(p);	
	info.setStorageSystemInfo(getSourceURL().getSURLInfo().getStorageSystemInfo());
	return info;
    }

    public void dissipate(TSRMRequestGet req) {
	if (_contents.size() == 0) {
	    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Nothing needs to be done!"));
	    return;
	}
	for (int i=0; i<_contents.size(); i++) {
	    TSRMSource curr = (TSRMSource)(_contents.get(i));
	    TSRMUtil.startUpInfo("............... dissipating"+curr);
	    if (curr.isDir()) {
		((TSRMSourceDir)curr).dissipate(req);
	    } else {
		TSRMRequestGet sub = req.createChildRequest(curr);
		sub.useToken(getToken());
		req.addToBulkSet(sub);
		sub.schedule();
	    }
	}  
    }

    public void dissipate(TSRMRequestCopy req) {
	if (_contents.size() == 0) {
	    req.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Nothing needs to be done!"));
	    return;
	}

	for (int i=0; i<_contents.size(); i++) {
	    TSRMSource curr = (TSRMSource)(_contents.get(i));
	    if (curr.isDir()) {
		((TSRMSourceDir)curr).dissipate(req);
		req.dirSync(curr);
	    } else {
		TSRMRequestCopy sub = req.createChildRequest(curr);
		sub.useToken(getToken());
		req.addToBulkSet(sub);
		sub.schedule();
	    }
	}  
    }

    public void addFile(TSRMLocalFile f, boolean isSurl) {
	TSRMSourceFile subFile = new TSRMSourceFile(f.getSiteURL());
	subFile.setKnownBytes(f.getCurrentSize());
	subFile.setToken(f.getToken());

	TFileStorageType undeterminedFileType = null;
	if (_localResidenceDir != null) {
	    TSRMLocalFile localDestination = _localResidenceDir.createSubFile(undeterminedFileType, subFile.getSourceURL(), isSurl);
	    subFile.setLocalDestination(localDestination);
	}

	_contents.add(subFile);
    }

    private void addFile(TMetaDataPathDetail detail, boolean isSurl) {
	TSRMSourceFile subFile = null;
	if (_blur) {
	    subFile = new TSRMSourceFile(TSupportedURL.create(createSURLInfo(detail.getPath())));
	} else {
	    subFile = TSRMSourceFile.create(TSupportedURL.create(createSURLInfo(detail.getPath())));
	}
	//TSRMSourceFile subFile = TSRMSourceFile.create(TSupportedURL.create(createSURLInfo(detail.getSurl().toString())));
	subFile.setKnownBytes(detail.getSize().longValue());
	subFile.setToken(getToken());

	TFileStorageType undeterminedFileType = null;

	if ((_localResidenceDir != null) && (!subFile.isUserOwned())) {
	    TSRMLocalFile localDestination = _localResidenceDir.createSubFile(undeterminedFileType, subFile.getSourceURL(), isSurl);
	    subFile.setLocalDestination(localDestination);
	}

	_contents.add(subFile);
    }

    public void addDir(TSRMLocalDir dir, boolean isSurl) {
	TSRMLog.debug(this.getClass(), null, "event=addDir0", "recursiveLevel="+getRecursiveLevel());
	if (getRecursiveLevel() == 0) {
	    return;
	}

	if (dir.isEmpty()) {
	    return;
	}

	TSRMSourceDir subDir = new TSRMSourceDir(dir.getSiteURL());
	subDir.setToken(getToken());
	
	if (getRecursiveLevel() < 0 ) {
	    subDir.setRecursiveLevel(-1);
	} else {
	    subDir.setRecursiveLevel(getRecursiveLevel() -1);
	}

	_contents.add(subDir);

	if (_localResidenceDir != null) {
	    String name = subDir.getSourceURL().getEndFileName();
	    subDir.setLocalDestination(_localResidenceDir.createDir(name, isSurl));
	}

	dir.populateTo(subDir);
    }    

    private void addDir(TMetaDataPathDetail detail, boolean isSurl) {	
	TSRMLog.info(this.getClass(), null, "event=addDir path="+detail.getPath(), "recursiveLevel="+getRecursiveLevel()+" subpathsize="+detail.getArrayOfSubPaths()+" localres="+_localResidenceDir);

	if (getRecursiveLevel() == 0) {
	    return;
	}

	if (detail.getArrayOfSubPaths() == null) {
	    return;
	}

	TSRMSourceDir subDir = new TSRMSourceDir(createSURLInfo(detail.getPath()));
	subDir.getSourceURL().useCredential(getSourceURL().getCredential());
	subDir.setToken(getToken());
        
	if (getRecursiveLevel() < 0 ) {
	    subDir.setRecursiveLevel(-1);
	} else {
	    subDir.setRecursiveLevel(getRecursiveLevel() -1);
	}

	_contents.add(subDir);

	if (_localResidenceDir != null) {
	    String name = subDir.getSourceURL().getEndFileName();
	    subDir.setLocalDestination(_localResidenceDir.createDir(name, isSurl));
	}

	TMetaDataPathDetail[] subpaths = detail.getArrayOfSubPaths().getPathDetailArray();
	for (int i=0; i<subpaths.length; i++) {
	    TMetaDataPathDetail curr = subpaths[i];
	    subDir.add(curr, isSurl);
	}
    }

    public void add(TMetaDataPathDetail curr, boolean isSurl) {
	if (curr.getType() == TFileType.DIRECTORY) {
	    addDir(curr, isSurl);	    
	} else {
	    addFile(curr, isSurl);
	}
    }
  
    public void populate(TMetaDataPathDetail[] details) {
	_contents.clear();

	TSRMUtil.startUpInfo("...populating details..");
	for (int i=0; i<details.length; i++) {
	    TMetaDataPathDetail curr = details[i];	   
	    add(curr, true);
	}
    }

    public void populate(TSRMLocalDir localDir) {
	_contents.clear();
	TSRMUtil.startUpInfo("...populating subdir.."+localDir);
	localDir.populateTo(this);
    }

    public void setRecursiveLevel(TDirOption dirOp) {
	if (dirOp == null) {
	    setRecursiveLevel(0);
	    return;
	}

	if (dirOp.getAllLevelRecursive() == null) {
	    setRecursiveLevel(dirOp.getNumOfLevels());	    
	} else {
	    if (dirOp.getAllLevelRecursive().booleanValue()) {
		setRecursiveLevel(-1);
	    } else {
		setRecursiveLevel(dirOp.getNumOfLevels());
	    }
	}
    }

    private void setRecursiveLevel(Integer i) {
	if (i != null) {
	    setRecursiveLevel(i.intValue());
	}
    }

    public void setRecursiveLevel(int i) {
	TSRMLog.debug(this.getClass(), null, "event=setRecursiveLvel value="+i, null);
	_levelsOfRecursiveness = i;
    }    
    
    public int getRecursiveLevel() {
	return _levelsOfRecursiveness;
    }
    //
    // populate on behalf of user 
    //
   
    public void attachToLocalCache(TUserRequest requester, long bytes) {
	//populate();
		
	for (int i=0; i<_contents.size(); i++) {
	    TSRMSource curr = (TSRMSource)(_contents.get(i));
	    curr.attachToLocalCache(requester, -1);
	}
    }
    
    public void detachFromLocalCache() {
	// will decide later
       RuntimeException hi = new RuntimeException("Will detach from local cache later.");
       hi.printStackTrace();
    }
    
    public ISRMLocalPath getLocalPath() {
	return _localResidenceDir;
    }
    
    public void transferMe(TSRMRequest r) {
	for (int i=0; i<_contents.size(); i++) {
	    TSRMSource curr = (TSRMSource)(_contents.get(i));
	    curr.transferMe(r);
	}      
    }
    
    public void abortTransfer() {
    }
    
    public boolean isDir() {
	return true;
    }
    
    public void setLocalDestination(ISRMLocalPath p) {
	if (!p.isDir()) {
	    throw new TSRMException("Cannt associate a file to dir."+p, false);
	}

	_localResidenceDir = (TSRMLocalDir)p;
	p.useCredential(getSourceURL().getCredential());
    }
    
    public boolean broadcast(RuntimeException e) {
	return false;
    }

    public boolean broadcast(TSRMException e) {
	return false;
    }
}
