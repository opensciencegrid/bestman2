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

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.transfer.*;
import org.ietf.jgss.GSSCredential;
import gov.lbl.srm.client.SRMClient;
import javax.xml.rpc.Stub;
import java.io.*;

//
// using SRMTransfer
// get from remote or put from remote, to local file "_file"
//
public  class TSRM3rdPartyTxfCommon implements ISRMTxfHandler {
    private SRMFileTransfer _transfer = null;
    boolean _skipAPI = false;

    public TSRM3rdPartyTxfCommon(SRMFileTransfer txf) {
	_transfer = txf;
    }

    public void setSkipAPI() {
	_skipAPI = true;
    }

    public void abort() {
	_transfer.cancel();
    }

    public void action() {	
	String pathStr = "tgt="+_transfer.getInputStr()+" src="+_transfer.getSourceStr();
	TSRMLog.info(this.getClass(), null, "event=TxfStarts code=Pull",  pathStr);

	long start = System.currentTimeMillis();
	  
	boolean succ = false;
	try {
	    work();
	    
	    succ = true;
	} finally {	    
	    long dur = System.currentTimeMillis() - start;
	    TSRMLog.info(this.getClass(), null, "event=TxfEnds code=Pull", _transfer.getSetupStr()+" "+_transfer.getInputStr()+" durMillis="+dur+" succ="+succ);
	}
    }
    
    private void work() {
	_transfer.setTransferMode(SRMTransferMode.THIRDPARTY);
		 
	 _transfer.transferSync();
	 if (_transfer.getStatus() != null) {
	     throw new RuntimeException("Third party transfer failed. Reason:"+_transfer.getStatus());
	 }
    }
}


