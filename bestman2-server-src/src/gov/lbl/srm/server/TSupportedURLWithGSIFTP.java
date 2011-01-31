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
import org.ietf.jgss.GSSCredential;
import java.util.Vector;
import org.apache.axis.types.URI;
import java.io.IOException;

public class TSupportedURLWithGSIFTP extends TSupportedURL {
    public static final String _DefProtocolStr = "gsiftp";
    //protected SRMFileTransfer _transfer = new SRMFileTransfer();
    protected SRMTransferProtocol _protocol = SRMTransferProtocol.GSIFTP;
    
    protected long _trustedSize = -1; // save it so dont have to go get it all the time

    TSRMFileAccessGsiftp _access = null;

    Boolean _doDcau = null;
    Boolean _doProtection = null;
    Integer _nBufferBytes = null;
    Integer _nStream = null;
    
    boolean _skipAPI = false;
    
    public TSupportedURLWithGSIFTP(TSURLInfo info) {
	super(info);	   
	parseInfo(info.getStorageSystemInfo());
    }
    
    private TSRMFileAccessGsiftp getAccessFunction() {
	if (_access == null) {	
	    _access = new TSRMFileAccessGsiftp(this);
	}
	return _access;	
    }

    private void parseInfo(ArrayOfTExtraInfo arrayOfInfo) {
	if (arrayOfInfo == null) {
	    return;
	}

	TExtraInfo[] infoList = arrayOfInfo.getExtraInfoArray();
	if (infoList == null) {
	    return;
	}
	for (int i=0; i<infoList.length; i++) {
	     TExtraInfo curr = infoList[i];
	     String key = curr.getKey();
	     TSRMLog.debug(this.getClass(), null, "event=clientGsiftpInput key="+key+" value="+curr.getValue(), "str="+getURLString());
	     if (key.equalsIgnoreCase("gsiftpStream")) {
		 _nStream = new Integer(curr.getValue());	     
	     } else if (key.equalsIgnoreCase("bufferSize")) {
		 _nBufferBytes = new Integer(curr.getValue());		 
	     } else if (key.equalsIgnoreCase("dcau")) {
		 _doDcau = Boolean.valueOf(curr.getValue());
	     } else if (key.equalsIgnoreCase("protection")) {
		 _doProtection = Boolean.valueOf(curr.getValue());
	     } else if (key.equalsIgnoreCase("noAPI")) {
		 _skipAPI = Boolean.valueOf(curr.getValue());
		 TSRMLog.debug(this.getClass(), null, "event=setParameter url=\""+getURLString()+"\"", "skipAPI="+_skipAPI);
		 if (_skipAPI) {
		     if (gov.lbl.srm.server.Config._gucLocation == null) {
			 throw new TSRMException("Cannt do no api. guc location is not defined.",false);
		     }
		 }
	     }
	}

	if (_nBufferBytes != null) {
	    if (_nBufferBytes.intValue() <= 0) {
		TSRMLog.debug(this.getClass(), null, "event=ignoresBufferSize value="+_nBufferBytes.intValue(), null);
	    }
	    _nBufferBytes = null;
	}

	if (_nStream != null) {
	    if (_nStream.intValue() <= 0) {
		TSRMLog.debug(this.getClass(), null, "event=ignoreStreams value="+_nStream.intValue(), null);
	    }
	    _nStream = null;
	}

    }

    public String getProtocol() {
	return _DefProtocolStr;
    }
    
    public Vector ls(URI uri, TSRMFileListingOption listingOp, TFileStorageType ftype) {
	TMetaDataPathDetail result = ls(listingOp);
	if (result == null) {
	    return null;
	}
	Vector resultList = new Vector(1);
	resultList.add(result);
	return resultList;
    }

    public TMetaDataPathDetail ls(TSRMFileListingOption listingOp) {
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	result.setPath(getURLString());

	//TSRMFileAccessGsiftp access = new TSRMFileAccessGsiftp(this);
	if (getAccessFunction() == null) {
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Cannt access gsiftp."));
	    return result;
	}
	
	java.io.File path = null;
	try {
	    //path = new java.io.File(getEffectivePath());
	    path = TSRMUtil.initFile(getEffectivePath());
	
	    if (!getAccessFunction().getClient().exists(path.getPath())) {
		result.setStatus(TSRMUtil.createReturnStatusInvalidPath("Does not exist.Checked through gsiftp!"));
		return result;
	    }	    		
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setStatus(TSRMUtil.createReturnStatusFailed("Reason:"+e.getMessage()));
	    return result;
	}
	result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from disk"));

	try {
	    getAccessFunction().getClient().changeDir(path.getPath());
	    result.setType(TFileType.DIRECTORY);
	    int recursiveLevel = listingOp.getRecursiveLevel();
	    if (recursiveLevel != 0){
		result = getAccessFunction().listDir(path, listingOp,recursiveLevel, result);
	    }
	} catch (Exception e) {
	    result.setType(TFileType.FILE);
	    try {
		result.setSize(TSRMUtil.createTSizeInBytes(getAccessFunction().getClient().getSize(path.getCanonicalPath())));
		if (listingOp.isDetailNeeded()) {
		    result.setLastModificationTime(TSRMUtil.createGMTTime(getAccessFunction().getClient().getLastModified(path.getCanonicalPath()).getTime()));
		}
	    } catch (Exception ee) {
		result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, ee.getMessage()));
		return result;
	    }
		
	}

	return result;
    }    
    
    public TReturnStatus rmFile(TAccount caller) {
	if (getAccessFunction() == null) {
	    return TSRMUtil.createReturnStatusFailed("Unable to access gsiftp");
	}
	return getAccessFunction().rmFile(getEffectivePath());	
    }

    public TReturnStatus rmdir(TAccount caller, boolean recursive) {
	return TSRMUtil.createReturnStatusFailed("Not a local dir.");
    }

    public TReturnStatus mkdir(TAccount caller) {
	//return TSRMUtil.createReturnStatusFailed(TSRMUtil._DefInvalidPath);
	return getAccessFunction().makeDir(getEffectivePath());	
    }

    public boolean isProtocolGSIFTP() {
	return true;
    }
    
    public TReturnStatus authorize(TSRMPermission p, TAccount user) {
	return null; 
    }
    
    public void setTrustedSize(long size) {
	_trustedSize = size;
    }

    public long getTrustedSize(int ignoredRecursiveLevel) {	    
	try {
	    if (_trustedSize == -1) {
		SRMFileTransfer txf = initTxf();
		_trustedSize = txf.getSourceFileSize();
	    } 
	    return _trustedSize;
	} catch (Exception e) {
	    return -1;
	}
    }
    
    public void abortDownload() {
	
    }

    public SRMFileTransfer initTxf() {
	return initTxf(null);
    }
    
    private SRMFileTransfer initTxf(GSSCredential cred) {
	GSSCredential txfCred = cred;
	if (txfCred == null) {
	    if (getCredential() == null) {
		throw new TSRMException("No credential found. Cannt proceed.", false);
	    }
	    txfCred = getCredential();
	}

	SRMFileTransfer transfer = new SRMFileTransfer();
	
	try {
	    transfer.setSourceUrl(getURLString());
	} catch (IOException e) {
	    throw new RuntimeException(e.toString());
	}
	
	if (getHost().equalsIgnoreCase("garchive.nersc.gov")) {
	    transfer.setNERSCHost();
	}
	//transfer.setTransferType(SRMTransferProtocol.GSIFTP);
	transfer.setTransferType(_protocol);
	
	if (_doProtection != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"", "setProtection="+_doProtection.booleanValue());
	    transfer.setDoSessionProtection(_doProtection.booleanValue());
	}
	if (_doDcau != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"","setDcau="+_doDcau.booleanValue());
	    transfer.setDCAU(_doDcau.booleanValue());
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"","useDefaultDcau="+Config._doDcau);
	    transfer.setDCAU(Config._doDcau);
	}

	if (_nStream != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"", "setParallel="+_nStream.intValue());
	    transfer.setParallel(_nStream.intValue());
	} else if (Config._nStream != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"", "setParallelDefault="+Config._nStream.intValue());
	    transfer.setParallel(Config._nStream.intValue());
	}

	if (_nBufferBytes != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"", "setBufferSize="+_nBufferBytes.intValue());
	    transfer.setBufferSize(_nBufferBytes.intValue());
	} else if (Config._nBufferBytes != null) {
	    TSRMLog.debug(this.getClass(), null, "event=initTxf url=\""+getURLString()+"\"", "setBufferSizeDefault="+Config._nBufferBytes.intValue());
	    transfer.setBufferSize(Config._nBufferBytes.intValue());
	}

	transfer.setCredentials(txfCred);

	return transfer;
    }
    
    public ISRMTxfHandler downloadTo(TSRMLocalFile localTgt) {	
	SRMFileTransfer transfer = initTxf();

	//transfer.setCredentials(getCredential());

	TSRMDownloadCommon  txf = new TSRMDownloadCommon(transfer, localTgt);
	if (_skipAPI) {
	    txf.setSkipAPI();
	}

	localTgt.setTxfObj(txf);
		
	return txf;
    }	
    
    public ISRMTxfHandler uploadFrom(ISRMLocalPath srcLocal, GSSCredential cred) {
	if (srcLocal.isDir()) {
	    throw new TSRMException("No support for dir uploading in GSIFTP yet.", false);
	}
	
	SRMFileTransfer transfer = initTxf(cred);
	
	TSRMUploadCommon txf = new TSRMUploadCommon(transfer, getURLString(), (TSRMLocalFile)srcLocal, cred);
	
	return txf;
    }
    
    public ISRMTxfHandler uploadTo(URI tgt, GSSCredential cred) {
	SRMFileTransfer transfer = initTxf(cred);

	try {
	    transfer.setDestinationUrl(tgt.toString());
	 } catch (Exception e) {
	     throw new RuntimeException(e.toString());
	 }
	TSRM3rdPartyTxfCommon txf = new TSRM3rdPartyTxfCommon(transfer);
	return txf;
    }

    public boolean checkExistence() {
	java.io.File path = TSRMUtil.initFile(getEffectivePath());
	return getAccessFunction().exists(path);
    }

    public boolean isDir() {
	//TSRMLog.debug(this.getClass(), null, "event=isDir msg=\"Assuming_all_(gsi)ftp:// are file path.\"", null);
	//return false;
	java.io.File path = TSRMUtil.initFile(getEffectivePath());
	return getAccessFunction().isDir(path);
    }

    public void populateMe(TSRMSourceDir dir) {//client indicate this is a dir
	TSRMLog.debug(this.getClass(), null, "event=populateMe note=userIndicatedDir", null);
	TSRMFileAccessGsiftp access = new TSRMFileAccessGsiftp(this);
	TMetaDataPathDetail result = new TMetaDataPathDetail();
	TSRMFileListingOption listingOp = new TSRMFileListingOption(null, null, null, null, null);
	//java.io.File f = new java.io.File(getURI().getPath());
	java.io.File f = TSRMUtil.initFile(getURI().getPath());
	result = access.listDir(f, listingOp, dir.getRecursiveLevel(), result);
	if (result != null) {
		if (result.getArrayOfSubPaths() != null) {
		    dir.populate(result.getArrayOfSubPaths().getPathDetailArray());
		} else {
			TSRMLog.debug(this.getClass(), null, "event=populateMe result=noSubPath", null);		
		}
	}
	return;
    }  
}
