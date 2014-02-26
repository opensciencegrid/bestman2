/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.server;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.adapt.srm.util.*;

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

    public static ArrayList _blockedPath = new ArrayList();
    public static ArrayList _allowedPath = new ArrayList();
    //private GridFTPClient _gridClient = null;
    //ISRMFileAccess _fileAccess =  null;
    //ISRMFileAccess _lsAccess = null;

    public TSupportedURLWithFILE(URI info, String sitePath) {	
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
	//_file = TSRMUtil.initFile(localPath);
	//TSRMLog.debug(this.getClass(), null, "localFilePath="+localPath, "uri="+getURI().toString());

	/*
	if (Config._accessFileSysUsingGSIFTP) {
	    //_fileAccess = new TSRMFileAccessGsiftp(this);
	} else if (Config._accessFileSysUsingSUDO) {
	    //_fileAccess = new TSRMFileAccessSudo();
	    if (Config._noSudoOnLs) {
		//_lsAccess = new TSRMFileAccessDefault();
	    }
	} else {
	    //_fileAccess = new TSRMFileAccessDefault();
	}
	if (_lsAccess == null) {
	    _lsAccess = _fileAccess;
	}
	*/
    }
    
    public int getPort() {
	return -1;
    }

    public String getDiskPath() {
	return _file.getPath();
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
    

    public void abortDownload() {
	return; 
    }
    
    public void setTrustedSize(long size) {
	_size = size;
    }

    public long getTrustedSize(int ignoredRecurisveLevel) {	    
	if (_size < 0) {
	    //_size = _fileAccess.getLength(_file);//_file.length();
	}
	
	return _size;
    }   
}




