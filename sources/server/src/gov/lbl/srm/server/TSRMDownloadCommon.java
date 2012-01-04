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
public  class TSRMDownloadCommon implements ISRMTxfHandler {
    private SRMFileTransfer _transfer = null;
    TSRMLocalFile _file = null;
    boolean _doGet = false;
    boolean _skipAPI = false;

    public TSRMDownloadCommon(SRMFileTransfer txf, TSRMLocalFile f) {
	_transfer = txf;
	_file = f;
	_doGet = !f.isThirdPartyTxfAssumed();
    }

    public void setSkipAPI() {
	_skipAPI = true;
    }

    public void abort() {
	_transfer.cancel();
    }

    public void action() {	
	String pathStr = "tgt="+_file.getCanonicalPath()+" src="+_transfer.getSourceStr();
	    TSRMLog.info(this.getClass(), null, "event=TxfStarts code=Pull",  pathStr);
	long start = System.currentTimeMillis();
	String sizeStr = "";
	  
	boolean succ = false;
	try {
	    if (_doGet) {
		get();
	    } else {
		thirdparty();
	    }
	    if ((_file != null) && (_file.getPhysicalLocation() != null)) {
		sizeStr = " size="+_file.getPhysicalLocation().getSize();		
	    }
	    succ = true;
	} finally {	    
	    long dur = System.currentTimeMillis() - start;
	    TSRMLog.info(this.getClass(), null, "event=TxfEnds code=Pull"+sizeStr, _transfer.getSetupStr()+" "+_transfer.getInputStr()+" durMillis="+dur+" succ="+succ);
	}
    }
    
    private void thirdparty() {
	_transfer.setTransferMode(SRMTransferMode.THIRDPARTY);
	
	 try {
	     //_transfer.setDestinationUrl(_file.getURIString(TSRMTxfProtocol.GSIFTP));
	     _transfer.setDestinationUrl(_file.getURIString(TSRMTxfProtocol.GSIFTP));
	 } catch (Exception e) {
	     throw new RuntimeException(e.toString());
	 }
	 
	 _transfer.transferSync();
	 if (_transfer.getStatus() != null) {
	     throw new RuntimeException("Third party transfer failed. Reason:"+_transfer.getStatus());
	 }
    }


    private void doTxf(long trustedSize) {
	//TSRMLog.info(this.getClass(), "TxfStartsPull",  _file.getCanonicalPath(), null);

	if (trustedSize > 0) {
	    TSRMLog.info(this.getClass(), null, "event=txfSetup "+_file.stampPath(),  "trustedSize="+trustedSize);
	    _transfer.transferSync();
	} else {
	    TSRMLog.info(this.getClass(), null, "event=txfSetup "+_file.stampPath(),  "result=no_trustedSize");
	    _transfer.transfer();

	    while (!_transfer.transferDone()) {
		//System.out.println("#### transfer to:"+_file.getName()+" is NOT done.");
		try {
		    TSRMLog.debug(this.getClass(), null, "event=FTPNotDone", _file.stampPath());
		    Thread.sleep(1000);
		} catch (InterruptedException ex) {
		    _file.getPhysicalLocation().changeTransferState(false);
		    TSRMLog.info(this.getClass(), null, "event=FTPendsPull "+_file.stampPath(), "exp="+ex.toString());
		    throw new RuntimeException(ex.toString());
		}
		
		if (_file.hasExceededReservedBytes()) {
		    //
		    // Q:do something and exit!!!!! later
		    // 
		}
	    }
	}
	
	//TSRMLog.info(this.getClass(), "TxfEndsPull",  _file.getName(), Thread.currentThread().getName());
    }

    public void get() {  // was doTransfer(tgt)
	//String tgt = _file.getURIString(TSRMTxfProtocol.FILE);	       
	String tgt = _file.getDefaultURIString();	       
	
	SRMTransferMode mode = SRMTransferMode.GET; // since tgt is file:/ protocol
	
	try {
	    _transfer.setDestinationUrl(tgt);
	    _transfer.setTransferMode(mode);
	} catch (IOException e) {
	    throw new RuntimeException(e.toString());
	}
	
	if (!_file.getPhysicalLocation().changeTransferState(true)) {
	    TSRMException ex = new TSRMException("Another request is transfering this file...", true);
	    ex.setWarning(true);
	    throw ex;
	}
	
	if (_file.isReady()) {
	    // file exists, dont transfer again
	    // if anything, check the permission on the source site
	    TSRMUtil.startUpInfo("##<<file:"+ _file.getName()+"  exists, skipping transfer>>");
	    _file.getPhysicalLocation().changeTransferState(false);
	    return;
	}

	if (_skipAPI) { 
	    boolean noError = _transfer.doubleCheck();
	    if (!noError) {
		throw new TSRMException("GUC transfer from remote site to "+tgt+" failed. Reason:"+_transfer.getStatus(), false);
	    }
	    return;
	} 

	long trustedSize = _transfer.getSourceFileSize();
	doTxf(trustedSize);
	
	_file.getPhysicalLocation().changeTransferState(false);
	
	if (_transfer.getStatus() != null) {
	    boolean noError = _transfer.doubleCheck();
	    if (!noError) {
		throw new TSRMException("Transfer from remote site to "+tgt+" failed. Reason:"+_transfer.getStatus(), false);
	    }
	}	    

	long sizeOnDisk = _file.getCurrentSize();

	if (trustedSize > 0) {
	    if (trustedSize > sizeOnDisk) {
	        // 
	        // Q:transfer again? or should this be dealt by the _transfer class?			
	        throw new TSRMException("Transfered bytes: "+sizeOnDisk+" < source size:"+trustedSize, true);
	    } else if (trustedSize < sizeOnDisk) {
		throw new TSRMException("Transfered bytes: "+sizeOnDisk+" > source size:"+trustedSize, false);
	    }
	}	
    }
}


