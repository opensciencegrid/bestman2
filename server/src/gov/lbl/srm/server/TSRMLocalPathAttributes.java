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
import java.io.IOException;
import java.util.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMLocalPathAttributes {    
    private String 			     _name 			= null; // dont want name to be empty
    public boolean                           _isObsolete                = false;
    
    protected TSURLInfo		             _siteURL	 	        = null;
    private long                             _timeStamp                 = -1; // time when file is materialized
    private long                             _createdAt                 = -1; // time this file is created

    private boolean                          _isPartOfSurl              = false;
      
    public void setName(String name) {
	_name = name;
    }

    public String getName() {
	return _name;
    }

    public void setSurlPath() {
	_isPartOfSurl = true;
	_createdAt = System.currentTimeMillis(); // if it is read from the log, then shall be the creation time of this round
    }

    public long getCreatedAt() {
	return _createdAt;
    }

    public boolean isSurlPath() {
	return _isPartOfSurl;
    }

    public void setTimeStamp() {
	_timeStamp =  System.currentTimeMillis(); 
	TSRMLog.debug(this.getClass(), null, "name="+_name, "timeStamp="+_timeStamp);	
    }
    
    public long getTimeStamp() {
	return _timeStamp;
    }

    public TSURLInfo getSiteURL() {
	return _siteURL;
    }

    public TSURLInfo generateSiteURL(String canonicalPath, boolean forced) {
	if ((_siteURL != null) && !forced) {
	    return _siteURL;
	}
	
	try{
	    URI uri = new URI(Config._wsdlEndPoint);
	    uri.setScheme(TSupportedURLWithSRM._DefProtocolStr);
	    if (!Config._usingCacheKeyword) {
		uri.setPath(uri.getPath()+"?SFN="+canonicalPath);
	    } else if (canonicalPath.startsWith("/")) {
		uri.setPath(uri.getPath()+"?SFN="+Config._DefCacheKeyword+canonicalPath);
	    } else {
		uri.setPath(uri.getPath()+"?SFN="+Config._DefCacheKeyword+"/"+canonicalPath);
	    }
	    
	    _siteURL = TSRMUtil.createTSURLInfo(uri);
	    return _siteURL;
	} catch (URI.MalformedURIException e) {
	    // should not reach here	    
	    TSRMLog.exception(TSRMLocalPathAttributes.class, "details", e);	    
	    return null;
	}
    }
    //
    // extra functions for removal detection
    //                 and V22 requirement
    // added jun 14, 2006
    //

    public boolean isObsolete() {
	return _isObsolete;
    }

    public void setObsolete() {
	_isObsolete = true;
    }
}
