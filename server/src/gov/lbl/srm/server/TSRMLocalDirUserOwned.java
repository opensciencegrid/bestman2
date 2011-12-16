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

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import org.apache.axis.types.URI;

import java.util.Vector;


public class TSRMLocalDirUserOwned extends TSRMLocalDir {
    //long _knownBytes = -1;
    TSRMPhysicalLocation _physicalLocation = null;
    TSURLInfo _originalSURLInfo = null;

	org.ietf.jgss.GSSCredential _credential = null;
	TAccount _user = null;

	public void useCredential(org.ietf.jgss.GSSCredential cred) {
		_credential = cred;
	}

    public TSRMLocalDirUserOwned(String name, TAccount user) {
	super(null, name, true);
	_user = user;
	TSRMLog.debug(TSRMLocalDirUserOwned.class, null, "event=userDirCreated name="+name, "user="+_user.getID());
    }

    public void setPhysicalLocation(TSRMPhysicalLocation f) {
	_physicalLocation = f;
    }

    public URI getOriginalSURL() {
	if (_originalSURLInfo != null) {
	    return _originalSURLInfo.getSURL();
	} 
	return null;
    }

    public void setSource(TSURLInfo urlInfo) {
	_originalSURLInfo = urlInfo;
    }

    public void addFile(TMetaDataPathDetail detail) {
	String endName = TSRMUtil.getEndFileName(detail.getPath());
	/*TSRMLocalFile subFile = */ createSubFile(endName, null, false);
	//subFile.setKnownBytes(detail.getSize().getValue());
    }

    private void addDir(int recursiveLevel, TMetaDataPathDetail detail) {	
	if (recursiveLevel == 0) {
	    return;
	}

	if (detail.getArrayOfSubPaths() == null) {
	    return;
	}

	String endName = TSRMUtil.getEndFileName(detail.getPath());
	TSRMLocalDirUserOwned subDir = (TSRMLocalDirUserOwned)(createDir(endName));
        subDirCreated(subDir);

	if (recursiveLevel > 0 ) {
	    recursiveLevel -= 1;
	}

	TMetaDataPathDetail[] subpaths = detail.getArrayOfSubPaths().getPathDetailArray();
	for (int i=0; i<subpaths.length; i++) {
	    TMetaDataPathDetail curr = subpaths[i];
	    subDir.populate(recursiveLevel, curr);
	}
    }

    private void populate(int recursiveLevel, TMetaDataPathDetail curr) {
	if (curr.getType() == TFileType.DIRECTORY) {
	    addDir(recursiveLevel, curr);	    
	} else {
	    addFile(curr);
	}
    }

    public void populate(int recursiveLevel, TMetaDataPathDetail[] details) {
	cleanContent(false);

	TSRMUtil.startUpInfo("...populating details..to:"+this+" rec level:"+recursiveLevel);
	for (int i=0; i<details.length; i++) {
	    TMetaDataPathDetail curr = details[i];	   
	    populate(recursiveLevel, curr);
	}
    }

	public TSRMLocalFile createSubFile(TFileStorageType fileStorageType, TSupportedURL url, boolean isSurl) {
		TSRMLog.info(this.getClass(), null, "event=creatingSubFile url=\""+url.getURLString(), null);
		  return createSubFile(url.getEndFileName(), fileStorageType);
	}

    public TSRMLocalFile createSubFile(TFileStorageType fileStorageType, TSupportedURL url) {
	TSRMLocalFile result = super.createSubFile(fileStorageType, url, false);
	TSRMLog.info(this.getClass(), null, "event=creatingSubFile url=\""+url.getURLString()+"\"", " originalSurl=\""+getOriginalSURL().toString()+"\"");

	//TSURLInfo info = createTSURLInfoForSubPath(result.getName());	
	//result.setPhysicalLocation(TSupportedURL.create(info), null);

	//subFileCreated(result); // added from super.createSubFile
	return result;
    }

    public TSRMLocalFile createSubFile(String name, TFileStorageType fileType) {
	TSRMLocalFileUserOwned f = new TSRMLocalFileUserOwned(name, fileType);
	
	TSURLInfo info = createTSURLInfoForSubPath(f.getName());	
	f.setSourceURI(info.getSURL());
	f.setPhysicalLocation(TSupportedURL.create(info), null);

	subFileCreated(f);
	return f;
    }

    private TSURLInfo createTSURLInfoForSubPath(String subpathName) {
	String urlStr = TSRMUtil.createPath(getOriginalSURL().toString(), subpathName);
	TSURLInfo info = TSRMUtil.createTSURLInfo(urlStr);
	info.setStorageSystemInfo(_originalSURLInfo.getStorageSystemInfo());
	TSRMLog.info(this.getClass(), null, "event=createTSRULInfoForSubPath", "url=\""+urlStr+"\" ssi="+info.getStorageSystemInfo());
	return info;
    }

    
	public TSRMLocalDir createDir(String name, boolean isSurl) {
		return createDir(name);
	}

    public TSRMLocalDir createDir(String name) {
	TSRMLocalDirUserOwned dir = new TSRMLocalDirUserOwned(name, _user);
	TSURLInfo pathInfo = createTSURLInfoForSubPath(name);
	TSupportedURL url = TSupportedURL.create(pathInfo);
	url.useCredential(_credential);
	if (url.isDeviceSpecific()) {
	    //TSupportedURLDeviceSpecific deviceUrl = (TSupportedURLDeviceSpecific)url;
	    dir.setPhysicalLocation(new TSRMPhysicalLocation(url));
	    _physicalLocation.getDevice().createPath(url, null, _physicalLocation.getDeviceAccessInfo());
	} else {
		if (url.isProtocolFILE()) {
			TReturnStatus status = url.mkdir(TAccountManager._SUPER_USER);
			if ((status.getStatusCode() != TStatusCode.SRM_SUCCESS) &&
				(status.getStatusCode() != TStatusCode.SRM_DUPLICATION_ERROR)) {
				throw new TSRMException("Failed to create dir: code="+status.getStatusCode()+" exp: "+status.getExplanation(),false);
			}
		}
	}
	dir.setSource(pathInfo);
	subDirCreated(dir);
	return dir;
    }
    
    public Vector ls(TFileStorageType fileStorageType,// considerred		 
		     TSRMFileListingOption lsOption)
    {
	int nLevels = lsOption.getRecursiveLevel();
	int nCount = lsOption.getOutputCount();
	int nOffset = lsOption.getOutputOffset();

	Vector resultList = new Vector(1);

	if ((nLevels == 0) || (nCount == 1)) {
	    TMetaDataPathDetail result =  new TMetaDataPathDetail();
	    //result.setPath(getOriginalSURL().toString());
	    result.setPath(TSRMUtil.getAbsPath(getOriginalSURL()));
	   
	    result.setType(TFileType.DIRECTORY);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "User file. level="+nLevels+" count="+nCount));
	    resultList.add(result);		    
	    return resultList;
	}

	if (nLevels > 1) {
	    throw new TSRMException("We do not support level argument in use files, either use allRecursive or use noRecursive.", false);
	}

	TMetaDataPathDetail result = null;

	if (_physicalLocation == null) {
	    result = new TMetaDataPathDetail();
	    //result.setType(TFileType.DIRECTORY);
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Not on device yet."));
	} else {
	    result = _physicalLocation.ls(lsOption); // size, type, lastModificationTime
	    
	    result = filter(result, nOffset, nCount);
	}

	//result.setPath(getOriginalSURL().toString());
	result.setPath(TSRMUtil.getAbsPath(getOriginalSURL()));

	resultList.add(result);
	return resultList;
    }

    private TMetaDataPathDetail filter(TMetaDataPathDetail result, int offset, int count) {	
	if (result.getArrayOfSubPaths() == null) {
	    return result;
	}
	
	TMetaDataPathDetail[] subPaths = result.getArrayOfSubPaths().getPathDetailArray();

	if (subPaths.length < offset) {
	    result.setArrayOfSubPaths(null);
	    return result;
	}

	int begin = offset;
	int end = count + offset;
	if (count == 0) {
	    end = subPaths.length;
	    if (begin == 0) {
		return result;
	    }
	} else {
	    if (subPaths.length  < end) {
		end = subPaths.length;
	    } 
	}
	
	TMetaDataPathDetail[] trimmed = new TMetaDataPathDetail[end-begin];
	for (int i=begin; i<end; i++) {
	    trimmed[i-begin] = subPaths[i];
	}

	result.setArrayOfSubPaths(TSRMUtil.convertToArray(trimmed));
	return result;
    }
}
