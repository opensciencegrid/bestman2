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

import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;
import java.util.Vector;
import java.io.*;

public abstract class TSupportedURL {
    private URI _info;
    private GSSCredential _cred = null;

    public TSupportedURL(URI info) {
	_info = info;
    }
    
    public abstract String getProtocol();

    public String getEffectivePath() {
	return getURI().getPath();
    }
    
    //public abstract boolean checkExistence();
    public void validate() {}
    
    public void useCredential(GSSCredential creds) 
    {
	_cred = creds;
	validate();
    } 

    public GSSCredential getCredential() {
	return _cred;
    }
    
    
    public URI getURI() {
	return _info;
    }
    
    public String getHost() {
	return getURI().getHost();
    }
    
    public int getPort() {
	return getURI().getPort();
    }
    
    public String getURLString() { 
	return _info.toString();
    } 
    
    private boolean hasSameURI(URI another) {
	return _info.equals(another);
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

    public static URI supportDoubleSlash(URI info) {		    
	//
	// Viji asked to support multiple // here to be compatible with V1
	//
	boolean found = false;
	String path = info.getPath();		    
	while (path.startsWith("//")) { 
	    path = path.substring(1);
	    found = true;
	}
	
	if (!found) {
	    return info;
	}
	
	try {
	    //info.getSURL().setPath(path0);
	    URI uri = new URI(info.toString());
	    uri.setPath(path);

	    return uri;
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    throw new RuntimeException(e.getMessage());
	}
    }
    
   
    public static TSupportedURL create(URI info, boolean limitedToLocal) {	  
	String protocol = info.getScheme();		

	if (protocol.equals(TSupportedURLWithHTTP._DefProtocolStr)) {
	    return new TSupportedURLWithHTTP(info);
	} else if (protocol.equals(TSupportedURLWithGSIFTP._DefProtocolStr)) {
	    return new TSupportedURLWithGSIFTP(info);
	} else if (protocol.equals(TSupportedURLWithFTP._DefProtocolStr)) {
	    return new TSupportedURLWithFTP(info);
	} else if (protocol.equals(TSupportedURLWithFILE._DefProtocolStr)) {
	    return new TSupportedURLWithFILE(info, null);
	} 
	
	throw new RuntimeException("Not supported URL:"+info);
    }

    public static TSupportedURL create(URI info) {
	return create(info, false);
    }
    
	
    public static String getEndFileName(URI src) {
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
	
}

