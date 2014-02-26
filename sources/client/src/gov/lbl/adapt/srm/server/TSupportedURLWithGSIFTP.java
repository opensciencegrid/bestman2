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
import java.util.Vector;
import org.apache.axis.types.URI;
import java.io.IOException;

public class TSupportedURLWithGSIFTP extends TSupportedURL {
    public static final String _DefProtocolStr = "gsiftp";
    //protected SRMFileTransfer _transfer = new SRMFileTransfer();
    
    protected long _trustedSize = -1; // save it so dont have to go get it all the time

    Boolean _doDcau = null;
    Boolean _doProtection = null;
    Integer _nBufferBytes = null;
    Integer _nStream = null;
    
    boolean _skipAPI = false;
    
    public TSupportedURLWithGSIFTP(URI info) {
	super(info);	   
	//parseInfo(info.getStorageSystemInfo());
    }
    

    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    public boolean isProtocolGSIFTP() {
	return true;
    }
    
    public void setTrustedSize(long size) {
	_trustedSize = size;
    }
    
    public void abortDownload() {
	
    }

}
