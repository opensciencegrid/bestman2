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

package gov.lbl.srm.impl;

//import javax.xml.rpc.Stub;

import gov.lbl.srm.util.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.client.*;

//httpg
import org.globus.axis.gsi.GSIConstants;
import org.ietf.jgss.GSSCredential;
//import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import java.net.URL;

public class TSRMStubHolder {
    private ISRM _srmStub = null;

    public TSRMStubHolder (String endPoint) {
	initStub(endPoint);
    }

    public void initStub(String endPoint) {
      try {
        _srmStub = SRMClient.getISRMHandle(endPoint);
      } catch (Exception e) {
	TSRMLog.exception(TSRMStubHolder.class, "details:", e);
	e.printStackTrace();
	throw new TSRMException("Failed to init stub", false);
      }
    }    

    public ISRM getStub() {
	return _srmStub;
    }

    public void useCredential(GSSCredential cred) {
	TSRMStubHolder.useCredential((org.apache.axis.client.Stub)_srmStub, cred);
    }

    public static void useCredential(org.apache.axis.client.Stub stub, GSSCredential cred) {
	if (cred == null) {
	    throw new TSRMException ("There is no credential! Did you do delegation?", false);
	} 
	stub._setProperty(GSIConstants.GSI_CREDENTIALS, cred);	
	stub._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_CREDENTIALS,cred);
    }
}
