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
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.transfer.*;
import org.ietf.jgss.GSSCredential;
import gov.lbl.srm.client.SRMClient;
import javax.xml.rpc.Stub;
import java.io.*;


// 
// using SRMTransfer
// upload from local file "_file" to remote location
// note: cannt upload a dir to a remote location using ftp
//
 public class TSRMUploadCommon implements ISRMTxfHandler {
     private SRMFileTransfer _transfer = null;
     TSRMLocalFile _file = null;
     String _remoteLoc = null;
     boolean _skipAPI = false;

    public TSRMUploadCommon(SRMFileTransfer txf, String remoteLoc, TSRMLocalFile f, GSSCredential cred) {
	_transfer = txf;
	_file = f;
	_remoteLoc = remoteLoc;

	if (cred != null) {
	    _transfer.setCredentials(cred);
	}
    }

     public void setSkipAPI() {
	_skipAPI = true;
    }

    public void abort() {
	_transfer.cancel();
    }

    public void action() {
	if (!TSRMServer._gProcessingUnit.acquireIOToken(true)) {
	    throw new TSRMException("Cann't upload, ftp semaphore failed.", false);
	}
	String sizeStr = " size="+_file.getCurrentSize();
	String pathStr = "src="+_file.getURIString(TSRMTxfProtocol.FILE)+" tgt="+_remoteLoc;

	TSRMLog.info(this.getClass(), null, "event=TxfStarts code=Push",  pathStr+sizeStr);
	//TSRMLog.info(this.getClass(), null, "event=txfSetup", "path="+_file.getCanonicalPath()+sizeStr);
	long start  = System.currentTimeMillis();

	boolean succ = false;
	try {
	    doWork();
	    succ = true;
	} finally {
	    TSRMServer._gProcessingUnit.releaseIOToken(true);
	    long dur = System.currentTimeMillis() - start;
		pathStr = "src==="+_transfer.getSourceStr()+" tgt="+_remoteLoc;
	    TSRMLog.info(this.getClass(), null, "event=TxfEnds code=Push",  _transfer.getSetupStr()+" "+_transfer.getInputStr()+" durMillis="+dur+sizeStr+" succ="+succ);
	}
    }

    private void doWork() {
	if (!_file.isValid()) {
	    throw new TSRMException("Cann't upload, the local file has expired lifetime.", false);
	}
	/*
	if (!_file.getPinManager().hasActivePin()) {
	    throw new TSRMException("Cann't upload, the local file has expired lifetime.", false);
	}	
	*/			

	String src = _file.getURIString(TSRMTxfProtocol.FILE);	       
	    
	SRMTransferMode mode = SRMTransferMode.PUT; // expecting src to be file:/ protocol
	    
	try {
	    _transfer.setDestinationUrl(_remoteLoc);
	    _transfer.setSourceUrl(src);
	    _transfer.setTransferMode(mode);
	} catch (IOException e) {
	    throw new RuntimeException(e.toString());
	}
	    
	//	long trustedSize = _transfer.getSourceFileSize();

	_transfer.transferSync();

	
	if (_transfer.getStatus() != null) {
	    boolean noError = _transfer.doubleCheck();
	    if (!noError) {
		throw new RuntimeException("Transfer to remote site: "+_remoteLoc+" Reason:"+_transfer.getStatus());
	    }
	}
    }
}
