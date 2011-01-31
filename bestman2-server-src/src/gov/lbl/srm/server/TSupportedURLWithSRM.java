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
//import gov.lbl.srm.StorageResourceManager.service.*;
import gov.lbl.srm.util.*;
//import gov.lbl.srm.client.SRMClient;
import gov.lbl.srm.transfer.globus.*;

import org.ietf.jgss.GSSCredential;

import javax.xml.rpc.Stub;
import java.net.URL;

//import org.apache.axis.message.addressing.Address;
//import org.apache.axis.message.addressing.EndpointReferenceType;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

//import org.apache.axis.types.URI;

public abstract class TSupportedURLWithSRM extends TSupportedURL {
    public static final String _DefProtocolStr = "srm";
   
    private String _endPoint;
    protected String _siteFileName;
    //private GSSCredential _cred = null;
    
    
    public TSupportedURLWithSRM(TSURLInfo info, String endPoint, String siteFileName) {
	super(info);
	_endPoint = endPoint;
	_siteFileName = siteFileName;
    }
   
    
    // returns [0] as _endPoint, 
    //         [1] as _siteFileName
    public static String[] parseTSURL(TSURLInfo info) {
	int port = info.getSURL().getPort();
	
	if (port == -1) {
	    port = Config._securePort; // default;
	}
	String[] result = new String[2];
	
	//String scheme = info.getSURL().getScheme();
	String host = info.getSURL().getHost();
	
	String url = info.getSURL().toString();
	
	int pos = url.indexOf("?SFN=");
	
	if (pos > 0){ //srm://host:[port]/soap_end_point?SFN=site+file+name
	    result[0] = Config._DefContactProtocolStr+"://"+host+":"+port+info.getSURL().getPath();
	    result[1] = url.substring(pos+5); 
	} else {
	    //result[1] = info.getSURL().getPath().substring(1);
	    //result[0] = Config._DefContactProtocolStr+"://"+host+":"+port+"/srm/srm.endpoint"; 
	    result[0] = Config._DefContactProtocolStr+"://"+host+":"+port+info.getSURL().getPath();
	    result[1] = info.getSURL().getPath();
	}
	
	if (result[0].endsWith("/")) { 
	    // We do this because Globus Axis server cannt tell the difference btw "path" and "path/".
	    // will throw: "Axis enginerr couldnt find target service" 
	    result[0] = result[0].substring(0, result[0].length()-1);
	}
	return result;
    }
    
    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    /*
    public TMetaDataPathDetail[] ls() {
	// contact targeted srm and get details, will do it later
	return null;
    }
    */
    
    public boolean isProtocolSRM() {
	return true;
    }
    

    public String getEffectivePath() {
	return getSiteFileName();
    }
	    
    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	// need to call srm_ls(), will do it later
	return null;
    }

    /*    
    public void useCredential(GSSCredential cred) {
	_cred = cred;
    }
    
    public GSSCredential getCredential() {
	return _cred;
    }
    */

    public abstract void abortDownload();		
    
    public abstract ISRMTxfHandler downloadTo(TSRMLocalFile tgt);
    
    public String formURLToSourceSRM() {
	return "file:////"+_siteFileName;
    }
    
    public String getEndPoint() {
	return _endPoint;
    }
    
    public String getSiteFileName() {
	//RuntimeException hi = new RuntimeException(this+" sfn="+_siteFileName);
	//hi.printStackTrace();
	return _siteFileName;
    }          

    public abstract long getTrustedSize(int recursiveLevel);
    public abstract void setTrustedSize(long size);
    
    public abstract boolean isLocalToThisSRM();
    /*{
      return _endPoint.equals(Config._wsdlEndPoint);
      }*/
}

