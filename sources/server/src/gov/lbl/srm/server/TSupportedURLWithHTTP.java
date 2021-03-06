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
import gov.lbl.srm.transfer.globus.SRMFileTransfer;
import gov.lbl.srm.transfer.globus.SRMTransferMode;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import org.ietf.jgss.GSSCredential;
import org.apache.axis.types.URI;
import java.io.IOException;
import java.util.Vector;

public class TSupportedURLWithHTTP extends TSupportedURL {
    public static final String _DefProtocolStr = "http";
    //protected SRMFileTransfer _transfer = new SRMFileTransfer();
    protected long _trustedSize = -1; // save it so dont have to go get it all the time
    
    public TSupportedURLWithHTTP(TSURLInfo info) {
	super(info);		
    }
    
    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    /*
    public TMetaDataPathDetail[] ls() {
	return null;
    }
    */
    
    public boolean isProtocolHTTP() {
	return true;
    }
    
    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	return null;
    }

    public Vector ls(URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	return null;
    }

    public SRMFileTransfer initSRMFileTransfer(TSRMLocalFile localTgt) {
	SRMFileTransfer txf = new SRMFileTransfer();
	String tgt = localTgt.getURIString(TSRMTxfProtocol.FILE);
	SRMTransferMode mode = SRMTransferMode.GET; // since tgt is file:/ protocol
	
	try {
	    txf.setSourceUrl(getURLString());
	    txf.setDestinationUrl(tgt);
	    txf.setTransferMode(mode);
	} catch (IOException e) {
	    throw new RuntimeException(e.toString());
	}

	txf.setTransferType(SRMTransferProtocol.HTTP);

	return txf;
    }
    
    public ISRMTxfHandler downloadTo(TSRMLocalFile localTgt) {
	SRMFileTransfer transfer = initSRMFileTransfer(localTgt);
	transfer.setCredentials(getCredential());
	TSRMDownloadCommon txf = new TSRMDownloadCommon(transfer, localTgt);
	localTgt.setTxfObj(txf);

	return txf;
    }
    
	
    public ISRMTxfHandler uploadFrom(ISRMLocalPath src, GSSCredential cred) {
	throw new TSRMException("Cannt upload to a http source!", false);
    }
	

    public ISRMTxfHandler uploadTo(URI uri, GSSCredential cred) {
	TSRMLog.info(this.getClass(), null, "event=uploadFromRemote", "result=notSupported");
	return null;
    }

    public void abortDownload() {

    }
	
    public void setTrustedSize(long size) {
	_trustedSize = size;
    }

    // cannt query size with http
    public long getTrustedSize(int ignoredRecursiveLevel) {
	return _trustedSize;
    }
    
    public boolean checkExistence() {
	return true;
    }

    public boolean isDir() {
	TSRMLog.info(this.getClass(), "isDir", "msg=\"Assuming_all_http:// are file path.\"", null);
	return false;
    }

    public void populateMe(TSRMSourceDir dir) {
	return;
    }
}
