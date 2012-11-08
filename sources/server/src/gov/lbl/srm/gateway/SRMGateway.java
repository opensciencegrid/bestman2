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
import gov.lbl.srm.transfer.*;
import gov.lbl.srm.impl.*;
import gov.lbl.srm.transfer.globus.*;
import org.apache.axis.types.URI;
import org.ietf.jgss.GSSCredential;
import org.globus.ftp.GridFTPClient;

public class SRMGateway extends TSRMServerIdle {    
    static final int DefDELETEFILE = 0;
    static final int DefDELETEDIR  = 1;
    static final int DefMKDIR = 2;
	static String _compileDate = null;

    public SrmPingResponse srmPing(SrmPingRequest req) throws java.rmi.RemoteException {       			
	//checkUserValidity(null, "srmPing", false); // this is commented out so can pass dn/mapped to the user
	//
	TSRMLog.info(TSRMServer.class, null,  "event=incoming", "name=srmPing authorizationId="+req.getAuthorizationID());

	if (_compileDate == null) {
	    _compileDate = TSRMUtil.getCompiledDate(gov.lbl.srm.server.SRMGateway.class);
	    TSRMLog.info(this.getClass(), null, "compiledAt="+_compileDate, null);
	}

	int debugLevel = 0;
	if (req.getAuthorizationID() != null) {
	    if (req.getAuthorizationID().equalsIgnoreCase("admin")) {
		debugLevel = 1;
	    }
	}

	SrmPingResponse result = new SrmPingResponse();
	result.setVersionInfo("v2.3.0");

	java.util.Vector storageReport = new java.util.Vector();

	TSRMUtil.addVersionInfo(storageReport, _compileDate);
	storageReport.add(TSRMUtil.createTExtraInfo("GatewayMode", "Enabled"));
	TSRMUtil.addCallerDN(storageReport);
	
	if (debugLevel == 1) {
	    if (Config._staticTokenList != null) {
		for (int i=0; i<Config._staticTokenList.length; i++) {
		    StaticToken curr = Config._staticTokenList[i];
		storageReport.add(TSRMUtil.createTExtraInfo("staticToken("+i+")", curr.getID()+" desc="+curr.getDesc()+" size="+curr.getTotalBytes()));
		}
	    }
	}

	TSRMUtil.addConfigEntries(debugLevel, storageReport);
	result.setOtherInfo(TSRMUtil.convertToArray(storageReport));
	return result;
    }
	    

    public SrmGetTransferProtocolsResponse srmGetTransferProtocols(SrmGetTransferProtocolsRequest req)  
    {   
	TReturnStatus s = validation("srmGetTransferProtocols()");	
	
        SrmGetTransferProtocolsResponse result = new SrmGetTransferProtocolsResponse();
	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}

        result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	TSupportedTransferProtocol temp = new TSupportedTransferProtocol();
        temp.setTransferProtocol("gsiftp");
        TSupportedTransferProtocol[] defaultArray = new TSupportedTransferProtocol[1];
        defaultArray[0] = temp;
	
	ArrayOfTSupportedTransferProtocol protocolInfo = new ArrayOfTSupportedTransferProtocol();
	protocolInfo.setProtocolArray(defaultArray);
	
	result.setProtocolInfo(protocolInfo);
	
        return result;
    }
    
    public SrmPrepareToGetResponse srmPrepareToGet(SrmPrepareToGetRequest req) { 	    
	try {
	    if (!acquireGuard()) {
		SrmPrepareToGetResponse result = new SrmPrepareToGetResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doPrepareToGet(req);
	} finally {
	    releaseGuard();
	}
    } 
    private SrmPrepareToGetResponse doPrepareToGet(SrmPrepareToGetRequest req) {
	Object ss = validationWithUid("srmPrepareToGet()");	
    	SrmPrepareToGetResponse result = new SrmPrepareToGetResponse();    	        
	
	if (isReturnStatus(ss)) {
	    result.setReturnStatus((TReturnStatus)ss);
	    return result;
	}

	TGetFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();    	
	if (listOfFileReqs.length == 0) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       "No file requests received."));
	    return result;
    	}

	TGetRequestFileStatus[] fileStatusList = new TGetRequestFileStatus[listOfFileReqs.length];
    		
	for (int i=0; i<listOfFileReqs.length; i++) {	    
	    TGetFileRequest curr = listOfFileReqs[i];
	    TGetRequestFileStatus s = new TGetRequestFileStatus();
	    
	    URI src = curr.getSourceSURL();
	    s.setSourceSURL(src);
	    try {
		URI txfURL = getXrootdPath(src);	    
	
		if (txfURL == null) {
		    s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, src+" is not a supported url."));
		} else {
		    TReturnStatus authorizeStatus = TSupportedURLWithFILE.authorize(TSRMPermission.Readable, (String)ss, txfURL.getPath());
		    if (authorizeStatus != null) {
		        s.setStatus(authorizeStatus);
		    } else {
			TSupportedURLWithFILE currF = getFileURL(src, TSRMService.gGetCredential("p2g"));
			long size = currF.getTrustedSize(0); //getFileSize(txfURL);
			if (size >= 0) {
			    if (txfURL != null) {
				s.setTransferURL(txfURL);		
				s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FILE_PINNED, null));
				s.setFileSize(TSRMUtil.createTSizeInBytes(size));
			    } else {
				srmInfo(src+".......is invalid");
				s.setStatus(TSRMUtil.createReturnStatusInvalidPath("given path="+src));
			    }
			} else {
			    s.setStatus(TSRMUtil.createReturnStatusInvalidPath("Cannt find size info on the given path="+src+" via:"+txfURL.getPath()+" configuration option: "+ConfigGateway._DefConfigCheckXrootdFS+"="+ConfigGateway._doCheckSizeWithXrootdFS+" "+ConfigGateway._DefConfigCheckSizeGsiftp+"="+ConfigGateway._doCheckSizeWithGsiftp));
			}
		    }
		}
	    } catch (RuntimeException e) {
		s.setStatus(TSRMUtil.createReturnStatus(e));		 
	    }
	    fileStatusList[i] = s;	    
	}

	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
    	//result.setReturnStatus(TSRMUtil.createReturnStatusSuccess(null));

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<listOfFileReqs.length; i++) {
	     TGetRequestFileStatus status = fileStatusList[i];
	     TStatusCode code = status.getStatus().getStatusCode();
	     reqSummary.add(code);
	}
	result.setRequestToken("get:"+TSRMUtil.getTokenID());
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
    	return result;
    }

    private void announce(String methodName) {
	TSRMUtil.startUpInfo("=> incoming ["+methodName+"]");
	return;
    }

    private void srmInfo(String msg) {
	TSRMUtil.startUpInfo("[srminfo]"+msg);
    }

    private boolean checkPathStructureExistence(boolean dirOnly, URI uri, GSSCredential creds) {
	try {
	    java.io.File f = TSRMUtil.initFile(uri.getPath());

	    URI copy = new URI(uri);
	    copy.setPath(f.getParentFile().getCanonicalPath());
	    copy.setScheme("file");
	    copy.setHost("");

	    String path = null;
	    if (dirOnly) {
		path = f.getParentFile().getCanonicalPath();
	    } else {
		path = f.getCanonicalPath();
	    }
	    TSupportedURLWithFILE ff = new TSupportedURLWithFILE(new TSURLInfo(copy, null), path);
	    
	    if (ConfigGateway._doCheckSizeWithXrootdFS) {
		ff.setFileAccessFS();
	    } else if (ConfigGateway._doCheckSizeWithGsiftp) {
		ff.setFileAccessGsiftp();
	    } else {
		String dn = TSRMService.gGetUID("srmRmdir");
		String uid= ConfigGateway.getMappedID(dn,null);
		System.out.println("::::::::::::::::::::::: "+uid);
		ff.setFileAccessSudoCaller(uid);
	    }
	    ff.useCredential(creds);
	    return ff.checkExistence();
	} catch (RuntimeException e) {
	    e.printStackTrace();
	    //return false;
	    throw e;
	} catch (Exception e) {
		e.printStackTrace();
		throw new RuntimeException(e.toString());
    }
  	}

/*
    private long getFileSizeThroughFileSystem(String path) {
	//java.io.File f = new java.io.File(path);
	java.io.File f = TSRMUtil.initFile(path);
	if (!f.exists()) {
	    srmInfo(path+" No such file or directory on file system.");
	    return -1;
	}
	srmInfo(path+" is on file system.");
	return f.length();
    }

    private long getFileSize(URI xrootdPath) {
	if (xrootdPath == null) {
	    return -1;
	} else {
	    if (ConfigGateway._doCheckSizeWithXrootdFS) {
		long size = getFileSizeThroughFileSystem(xrootdPath.getPath());
		if (size >= 0) {
		    return size;
		}
	    } 
	    if (ConfigGateway._doCheckSizeWithGsiftp) {
		GSSCredential creds = TSRMService.gGetCredential("getFileSize"); // temp	     
		SRMFileTransfer txf = initTxf(xrootdPath, creds);
		long size = txf.getSourceFileSize();
		srmInfo(xrootdPath.getPath()+" reporting size "+size+" through gsiftp.");
		if (size >= 0) {
		    return size;
		} else {
		    return -1;
		}
	    }
	    return -1;
	}
    }
*/
    public SrmPrepareToPutResponse srmPrepareToPut(SrmPrepareToPutRequest req) { 
	try {
	    if (!acquireGuard()) {
		SrmPrepareToPutResponse result = new SrmPrepareToPutResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doPrepareToPut(req);
	} finally {
	    releaseGuard();
	}
    }
    
    private  SrmPrepareToPutResponse doPrepareToPut(SrmPrepareToPutRequest req) { 
	Object ss = validationWithUid("srmPrepareToPut()");

    	SrmPrepareToPutResponse result = new SrmPrepareToPutResponse();    	        
	
	if (isReturnStatus(ss)) {
	    result.setReturnStatus((TReturnStatus)ss);
	    return result;
	}   

	TPutFileRequest[] listOfFileReqs = req.getArrayOfFileRequests().getRequestArray();    	

	String spaceToken = null;
	if (ConfigGateway._xrootdTokenCompName != null) {
	    spaceToken = req.getTargetSpaceToken();
	    if (spaceToken != null) {
		if (Config._staticTokenList != null) {
		    if (StaticToken.find(Config._staticTokenList, spaceToken) == null) {
			result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken+" ref:"+spaceToken));
			return result;
		    }
		}
	    }
	}

	if (listOfFileReqs.length == 0) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, 
							       "No file requests received."));
	    return result;
    	}

	TPutRequestFileStatus[] fileStatusList = new TPutRequestFileStatus[listOfFileReqs.length];    		

	boolean fastTrack=false;
	GSSCredential creds = null;
	if (req.getAuthorizationID() != null) {
	    if (req.getAuthorizationID().equalsIgnoreCase("fasttrack")) {
		fastTrack=true;
	    }
	}
	if (fastTrack != true) {
	    creds = TSRMService.gGetCredential("srmPut");
	}
	    
	TSRMLog.debug(SRMGateway.class, null, "authid="+req.getAuthorizationID(), "fasttrack="+fastTrack);

	for (int i=0; i<listOfFileReqs.length; i++) {	    
	    TPutFileRequest curr = listOfFileReqs[i];
	    TPutRequestFileStatus s = new TPutRequestFileStatus();
	    
	    URI src = curr.getTargetSURL();
	    s.setSURL(src);

	    try {
		URI txfURL = getXrootdPath(src, spaceToken);
		if (txfURL != null) {
		    if (fastTrack) {
			s.setTransferURL(txfURL);		
			s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_AVAILABLE, null));			
		    } else {
			TReturnStatus authorizeStatus = TSupportedURLWithFILE.authorize(TSRMPermission.Writable, (String)ss, txfURL.getPath());
			
			if (authorizeStatus != null) {
			    s.setStatus(authorizeStatus);		    
			} else if (!checkSpaceAvailablity(spaceToken, txfURL, curr.getExpectedFileSize())) {
			    s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Not enough space."));
			} else if (checkPathStructureExistence(true, txfURL, creds)) {		      
			    boolean checkFileExistence = getBooleanFlag(req.getStorageSystemInfo(), "reportduplicate");
			    if (checkFileExistence && checkPathStructureExistence(false, txfURL, creds)) {
				s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_DUPLICATION_ERROR, null));
			    } else {
				s.setTransferURL(txfURL);		
				s.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SPACE_AVAILABLE, null));
			    }
			} else {
			    s.setStatus(TSRMUtil.createReturnStatusInvalidPath("Dir structure does not exist."+src));
			}
		    }
		} else {
		    srmInfo(src+".......invalid"+src.toString());
		    s.setStatus(TSRMUtil.createReturnStatusInvalidPath("given path="+src));
		}
	    } catch (RuntimeException e) {
		e.printStackTrace();
		s.setStatus(TSRMUtil.createReturnStatus(e));		 
	    }
	    fileStatusList[i] = s;	    
	}

	
	result.setArrayOfFileStatuses(TSRMUtil.convertToArray(fileStatusList));
    	//result.setReturnStatus(TSRMUtil.createReturnStatusSuccess(null));

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	for (int i=0; i<listOfFileReqs.length; i++) {
	     TPutRequestFileStatus status = fileStatusList[i];
	     TStatusCode code = status.getStatus().getStatusCode();
	     reqSummary.add(code);
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	result.setRequestToken("put:"+TSRMUtil.getTokenID());
    	return result;
    }

    private boolean checkSpaceAvailablity(String spaceToken, URI local, org.apache.axis.types.UnsignedLong expected) {
	if (!ConfigGateway._doCheckToken) {
	    return true;
	}
	if (spaceToken == null) {
	    return true;
	}
	if (expected == null) {
	    return true;
	}

	String path = local.getPath(); // will not contain ?... part

	int pos = local.toString().indexOf(path);
        String xrootdPath = local.toString().substring(pos);
	
	srmInfo("  checking space availablity:"+xrootdPath);       
	//java.io.File f = new java.io.File(xrootdPath);
	java.io.File f = TSRMUtil.initFile(xrootdPath);
	long freeBytes = 0;
	try {
	    freeBytes = f.getFreeSpace();
	} catch (Exception e) {
	    e.printStackTrace();
	    return true; // b/c unable to say false.
	}
	
	srmInfo("   freeBytes of "+xrootdPath+" = "+freeBytes);
	if ((freeBytes > 0) && (freeBytes < expected.longValue())) {
	    return false;
	}
	return true;
    }

    public SrmRmResponse srmRm(SrmRmRequest req) {
	try {
	    if (!acquireGuard()) {
		SrmRmResponse result = new SrmRmResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doRm(req);
	} finally {
	    releaseGuard();
	}
    } 

    private SrmRmResponse doRm(SrmRmRequest req) {
	TReturnStatus s = validation("srmRm()");
    	SrmRmResponse result = new SrmRmResponse();    	

	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	} 

	URI[] inputPathArray = req.getArrayOfSURLs().getUrlArray();
	
	if (inputPathArray == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No input for rm()"));
	    return result;
	}

	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	TSURLReturnStatus[] statusArray = new TSURLReturnStatus[inputPathArray.length];

	GSSCredential creds = TSRMService.gGetCredential("srmRm");

	for (int i=0; i<inputPathArray.length; i++) {
	    statusArray[i] = new TSURLReturnStatus();
	    statusArray[i].setSurl(inputPathArray[i]);

	    //URI xrootdPath = getXrootdPath(inputPathArray[i]);
	    URI xrootdPath = inputPathArray[i];
	    if (xrootdPath == null) {
	        statusArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "unable to work with this surl."));
	    } else {
		try {
		    TSupportedURLWithFILE f = getFileURL(xrootdPath, creds);
		    
		    if (f == null) {
			statusArray[i].setStatus(TSRMUtil.createReturnStatusInvalidPath(xrootdPath+" is not an existing filepath!"));
		    } else {
			String dn = TSRMService.gGetUID("srmRm");
			String uid= ConfigGateway.getMappedID(dn,null);
			statusArray[i].setStatus(f.rmFile(new TAccount(uid)));
		    }
		} catch (TSRMException e) {
		    statusArray[i].setStatus(TSRMUtil.createReturnStatus(e));		 
		} catch (Exception e) {
		    e.printStackTrace();
		    statusArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));		 
		}
	    }
	    reqSummary.add(statusArray[i].getStatus().getStatusCode());
	}

	ArrayOfTSURLReturnStatus statusArrayObj = new ArrayOfTSURLReturnStatus();
	statusArrayObj.setStatusArray(statusArray);
	result.setArrayOfFileStatuses(statusArrayObj);
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	return result;    
    }

    public SrmRmdirResponse srmRmdir(SrmRmdirRequest req) {
	try {
	    if (!acquireGuard()) {
		SrmRmdirResponse result = new SrmRmdirResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doRmdir(req);
	} finally {
	    releaseGuard();
	}
    } 
    private SrmRmdirResponse doRmdir(SrmRmdirRequest req) {
	TReturnStatus s = validation("srmRmdir()");
	SrmRmdirResponse result = new SrmRmdirResponse();
	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	} 

	URI inputPath = req.getSURL();
	
	if (inputPath == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No input for rmdir()"));
	    return result;
	}
	    	
	URI xrootdPath = null;
	try {
	    xrootdPath = getXrootdPath(inputPath);
	} catch (RuntimeException e) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(e));		 
	    return result;
	}
	if (xrootdPath == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_PATH, "unable to work with this surl."));
	    return result;
	}

	GSSCredential creds = TSRMService.gGetCredential("srmRmdir"); 
	try {
	    TSupportedURLWithFILE f = getFileURL(inputPath, creds);
	    
	    if (f == null) {
		result.setReturnStatus(TSRMUtil.createReturnStatusInvalidPath(inputPath+" is not an existing filepath!"));
	    } else {
		String dn = TSRMService.gGetUID("srmRmdir");
		String uid= ConfigGateway.getMappedID(dn,null);
		boolean recursiveFlag = false;
		if (req.getRecursive() != null) {
		    recursiveFlag = req.getRecursive().booleanValue();
		}
		result.setReturnStatus(f.rmdir(new TAccount(uid), recursiveFlag));
	    }
	} catch (TSRMException e) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(e));		 
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));
	}
	    
	return result;
    }   

    private boolean isReturnStatus(Object ss) {
	if (ss == null) {
	    return false;
	}
	if (ss.getClass().getName().equals("gov.lbl.srm.StorageResourceManager.TReturnStatus")) {
	    return true;
	}
	return false;
    }

    private TReturnStatus validation(String methodName) {
	Object result = validationWithUid(methodName);
	if (isReturnStatus(result)) {
	    return (TReturnStatus)result;
	}
	return null;
    }

    private Object validationWithUid(String methodName) {
	announce(methodName);
	String dn = TSRMService.gGetUID(methodName);

	if (dn == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "no credential found.");
	} 
	String uid = Config.getMappedID(dn, null);
	if (uid == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_AUTHORIZATION_FAILURE, "not mapped."+dn);
	}
	return uid;
    }

    public SrmMkdirResponse srmMkdir(SrmMkdirRequest req) {
	try {
	    if (!acquireGuard()) {
		SrmMkdirResponse result = new SrmMkdirResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doMkdir(req);
	} finally {
	    releaseGuard();
	}
    } 

    private SrmMkdirResponse doMkdir(SrmMkdirRequest req) {
	TReturnStatus s = validation("srmMkdir()");
	SrmMkdirResponse result = new SrmMkdirResponse();	    		
	
    	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}    	

	URI inputPath = req.getSURL();
	
	if (inputPath == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No input for mkdir()"));
	    return result;
	}
	
	GSSCredential creds = TSRMService.gGetCredential("srmMkdir"); 
	try {
	    TSupportedURLWithFILE f = getFileURL(inputPath, creds);
	    
	    if (f == null) {
		result.setReturnStatus(TSRMUtil.createReturnStatusInvalidPath(inputPath+" is not an existing filepath!"));
	    } else {
		String dn = TSRMService.gGetUID("srmMkdir");
		String uid= ConfigGateway.getMappedID(dn,null);
		TReturnStatus stat = f.mkdir(new TAccount(uid));
		announce(f.getURLString()+" mkdir("+uid+") =>"+stat.getStatusCode());
		TSRMLog.debug(this.getClass(), null, "event=mkdir url="+f.getURLString(), "code="+stat.getStatusCode());
		result.setReturnStatus(stat);
		//result.setReturnStatus(f.mkdir(new TAccount(uid)));
	    }
	} catch (TSRMException e) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(e));		 
	} catch (Exception e) {
	    e.printStackTrace();
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));		 
	}
	return result;
    }
 
    private TReturnStatus invokeSudo0(URI url, String uid, int op) {
	String sudoCommand = "sudo -u "+uid;
	if (op == SRMGateway.DefDELETEFILE) {
	    sudoCommand +=" /bin/rm ";
	} else if (op == SRMGateway.DefDELETEDIR) {
	    sudoCommand +=" /bin/rmdir  ";
	} else if (op == SRMGateway.DefMKDIR) {
	    sudoCommand +=" /bin/mkdir ";
	}
	sudoCommand += url.getPath();
	TSRMUtil.startUpInfo("sudo=["+sudoCommand+"]");

	String err = TPlatformUtil.execCmdWithOutput(sudoCommand, false);
	if (err == null) {
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	} else {
	    //return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Command:["+sudoCommand+"] failed. \nError:"+err);
	    return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Error:"+err+"\nRef"+sudoCommand.substring(5));
	}
    }


    private TReturnStatus srmLsNotSupportedAttrs(Integer attr, String desc, int min) {
	if (attr != null) {
	    int v = attr.intValue();
	    if (v > min) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "Do not support "+desc+" in gateway mode.");
	    }
	}
	return null;
    }

    public SrmLsResponse srmLs(SrmLsRequest req) {
	try {
	    if (!acquireGuard()) {
		SrmLsResponse result = new SrmLsResponse();
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "SRM Gateway concurrency failure."));
		return result;
	    }
	    return doLs(req);
	} finally {
	    releaseGuard();
	}
    } 

    private SrmLsResponse doLs(SrmLsRequest req) {
	TReturnStatus s = validation("srmLs()");
    	SrmLsResponse result = new SrmLsResponse();
    	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}    	
	
	Boolean isAllLevelRecursive = req.getAllLevelRecursive();
	if (isAllLevelRecursive != null) {
	    if ((isAllLevelRecursive.booleanValue() == true) && (!ConfigGateway._enableRecursionInLs)){
		result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_NOT_SUPPORTED, "all level recursion is diabled."));
		return result;
	    }
	}
	/* // ignoring output count and offset, since  srmcp client is setting them which causes errors to thrown
	TReturnStatus error = srmLsNotSupportedAttrs(req.getCount(), "output count", -1);

	if (error == null) {
	    error = srmLsNotSupportedAttrs(req.getOffset(), "offset", -1);
	}
	if (error != null) {
	    result.setReturnStatus(error);
	    return result;
	}
	*/

	String dn = TSRMService.gGetUID("srmLs");
	if (dn == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Null dn found."));
	    return result;
	}	

	TSRMFileListingOption listingOp = new TSRMFileListingOption(req, Config.getMappedID(dn, null)); // uid is not needed here
	listingOp.printMe();
	GSSCredential creds = TSRMService.gGetCredential("srmLs"); 
	if ((ConfigGateway._doCheckSizeWithGsiftp) && (creds == null)) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "Need credentials for gsiftp. Did you delegate?"));
	    return result;
	}
	URI[] inputPathArray = req.getArrayOfSURLs().getUrlArray();
	
	if (inputPathArray == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No input"));	
	} else {
	    TMetaDataPathDetail[] listingOutput = callXrootdLs(inputPathArray, listingOp, creds);
	    result.setDetails(TSRMUtil.convertToArray(listingOutput));

	    TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	    reqSummary.collect(listingOutput);
	    result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	}
	return result;		    
    }

   
    private TSupportedURLWithFILE getFileURL(URI uri, GSSCredential creds) {
	String path = findFilePath(uri);
	if (path == null) {
	    return null;
	}
	TSURLInfo info = new TSURLInfo(uri, null);
	TSupportedURLWithFILE result = new TSupportedURLWithFILE(info, path);	
	//TSupportedURLWithFILE result = gov.lbl.srm.server.TSRMUserSpaceTokenSupport.expandURL(info, path);
	
	if (ConfigGateway._doCheckSizeWithXrootdFS) {
	    result.setFileAccessFS();
	} else if (ConfigGateway._doCheckSizeWithGsiftp) {
	    result.setFileAccessGsiftp();
	}
	result.useCredential(creds);
	return result;
    }

    private TMetaDataPathDetail[] callXrootdLs(URI[] inputPathArray, TSRMFileListingOption listingOp, GSSCredential creds) {	
	TMetaDataPathDetail[] result = new TMetaDataPathDetail[inputPathArray.length];	

	for (int i=0; i<inputPathArray.length; i++) {
	     URI curr = inputPathArray[i];
	     try {
		 TSupportedURLWithFILE f = getFileURL(curr, creds);
		 if (f == null) {
		     result[i] = TSRMUtil.createTMetaDataPathDetail(curr, TSRMUtil.createReturnStatusInvalidPath(curr+" is not an existing filepath."));	
		 } else {
		     result[i] = (TMetaDataPathDetail)(f.ls(curr, listingOp, null).get(0));
		     
		     if (result[i].getStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {
			 if (listingOp.isDetailNeeded()) {
			     result[i].setOwnerPermission(TSRMUtil.createDefaultOwnerPermission("owner"));
			     result[i].setGroupPermission(TSRMUtil.createDefaultGroupPermission());
			     result[i].setOtherPermission(TSRMUtil.createDefaultClientPermission());			 
			     
			     result[i].setFileLocality(TFileLocality.ONLINE);
			     result[i].setLifetimeLeft(TSRMUtil.createTLifeTimeInSeconds(-1, false));
			 }
		     }
		 }	     
	     } catch (TSRMException e) {
		 result[i] = TSRMUtil.createTMetaDataPathDetail(curr, TSRMUtil.createReturnStatus(e));		 
	     } catch (Exception e) {
		 result[i] = TSRMUtil.createTMetaDataPathDetail(curr, TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage()));		 
	     }
	}	     
	return result;
    }   

    private SRMFileTransfer initTxf(URI uri, GSSCredential txfCred) {
	SRMFileTransfer transfer = new SRMFileTransfer();
	
	try {
	    transfer.setSourceUrl(uri.toString());
	} catch (java.io.IOException e) {
	    throw new RuntimeException(e.toString());
	}
	
	transfer.setTransferType(SRMTransferProtocol.GSIFTP);
	transfer.setCredentials(txfCred);

	return transfer;
    }

    private String findFilePath(URI input) {
	if (input == null) {
	    return null;
	}
	
	String protocol = input.getScheme();	     
	if (!protocol.equals("srm")) {
	    return null;
	}	

	if (!TSRMUtil.isRefToSRM(getHostPort(input), ConfigGateway._wsdlEndPoint, input)) {
	    srmInfo("Unrecognizable url:"+input+" for this server:"+ConfigGateway._wsdlEndPoint);
	    return null;
	}
	
	String path = null;
	int pos = input.toString().indexOf("?SFN=");
	if (pos > 0) {
	    path = input.toString().substring(pos+5);
	} else {
	    path = input.getPath();
	}
	
/*
	if ((path == null) || (path.length() == 0) || (path.equals("/"))) {
	    return null;
	}
	return path;
*/
	if ((path == null) || (path.length() == 0)) {
		return null;
	}
		
	TSupportedURLWithFILE f = TSRMUserSpaceTokenSupport.expandURL(TSRMUtil.createTSURLInfo(input), path);
	return f.getDiskPath();
	
    }

    private URI getXrootdPath(URI input, String token) {
	if (token == null) {
	    return getXrootdPath(input);
	}

	String path = findFilePath(input);
	if (path == null) {
	    return null;
	}      	

	try {
	    //URI result = new URI(TSRMTxfProtocol.GSIFTP.generateURI(path+"?"+ConfigGateway._xrootdTokenCompName+"="+token));
	    URI result = new URI(TSRMTxfProtocol.getDefaultTransferProtocol().generateURI(path+"?"+ConfigGateway._xrootdTokenCompName+"="+token));
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    private URI getXrootdPath(URI input) {
	String path = findFilePath(input);
	if (path == null) {
	    return null;
	}

	// validation	
	////TSupportedURLWithFILE f = new TSupportedURLWithFILE(TSRMUtil.createTSURLInfo(input), path);

	try {
	    //URI result = new URI(TSRMTxfProtocol.GSIFTP.generateURI(path));
	    URI result = new URI(TSRMTxfProtocol.getDefaultTransferProtocol().generateURI(path));
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }


    private static String getHostPort(URI info) {
	int port = info.getPort();
	
	if (port == -1) {
	    port = ConfigGateway._securePort; // default;
	}
	String result = null;
	
	String host = info.getHost();

	String path = info.getPath();
	while (path.startsWith("//")) {
	    path = path.substring(1);
	}

	result = ConfigGateway._DefContactProtocolStr+"://"+host+":"+port+path;
	
	if (result.endsWith("/")) { 
	    // We do this because Globus Axis server cannt tell the difference btw "path" and "path/".
	    // will throw: "Axis enginerr couldnt find target service" 
	    result = result.substring(0, result.length()-1);
	}
	return result;
    }    

            
    public SrmGetSpaceTokensResponse srmGetSpaceToken(SrmGetSpaceTokensRequest req) 
    { 
	TReturnStatus s= validation("srmGetSpaceTokens()");
	SrmGetSpaceTokensResponse result = new SrmGetSpaceTokensResponse();

	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}    	

	if (Config._staticTokenList == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "No token is defined to the server."));
	} else {
	    if (req.getUserSpaceTokenDescription() != null) {
		String[] tokens = StaticToken.findByDesc(Config._staticTokenList,req.getUserSpaceTokenDescription());
		if (tokens == null) {
		    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, null));
		} else {
		    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
		    result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(tokens));
		}
		//result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, "no desc for static tokens."));
	    } else {
	    	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
	        result.setArrayOfSpaceTokens(TSRMUtil.convertToArray(StaticToken.getNames(Config._staticTokenList)));
	    }
	}
	return result;
    }

    public SrmGetSpaceMetaDataResponse srmGetSpaceMetaData(SrmGetSpaceMetaDataRequest req) {
	TReturnStatus s= validation("srmGetSpaceMetaData()");
	SrmGetSpaceMetaDataResponse result = new SrmGetSpaceMetaDataResponse();
	
	if (s != null) {
	    result.setReturnStatus(s);
	    return result;	      	    
	}    	

	if (req.getArrayOfSpaceTokens() == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "No array of tokens provided"));
	    return result;
	}
	
	String[] inputTokens = req.getArrayOfSpaceTokens().getStringArray();
	if (inputTokens == null) {
	    result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, TSRMUtil._DefNullToken));
	    return result;
	}
	
	TSRMRequestStatusReturn reqSummary = new TSRMRequestStatusReturn();
	TMetaDataSpace[] metaDataArray = new TMetaDataSpace[inputTokens.length];
	
	for (int i=0; i<inputTokens.length; i++) {
	    String curr = inputTokens[i];
	    StaticToken token = StaticToken.find(Config._staticTokenList, curr);
	    metaDataArray[i] = new TMetaDataSpace();
		metaDataArray[i].setSpaceToken(curr);
	    if (token == null) {
		metaDataArray[i].setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_INVALID_REQUEST, TSRMUtil._DefNoSuchSpaceToken+" ref:"+null));
	    } else {
		metaDataArray[i] = token.getMetadata();		
	    }
	    reqSummary.add(metaDataArray[i]);
	}
	
	result.setArrayOfSpaceDetails(TSRMUtil.convertToArray(metaDataArray));
	result.setReturnStatus(TSRMUtil.createReturnStatus(reqSummary.getStatusCode(), null));
	return result;
    }

    public SrmPutDoneResponse srmPutDone(SrmPutDoneRequest req) { 
    	//String currMethodName = "srmPutDone";
	//checkUserValidity(req.getAuthorizationID(), currMethodName);
	TReturnStatus s = validation("srmPutDone()");
    	SrmPutDoneResponse result = new SrmPutDoneResponse();
	
	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}    	
 
	if ((req.getArrayOfSURLs() != null) && (req.getArrayOfSURLs().getUrlArray() != null)) {
	    URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
		if (surlArray != null) {
			for (int i=0; i<surlArray.length; i++) {
				TSRMLog.info(TSRMServer.class, null,  "event=putDone", "rid="+req.getRequestToken()+" tgt="+surlArray[i]);
			}
	    }
	    TSURLReturnStatus[] curr = assignAll(surlArray, TSRMUtil.createReturnStatusSuccess(null));
	    if (curr != null) {
		result.setArrayOfFileStatuses(TSRMUtil.convertToArray(curr));
	    }
	}
	result.setReturnStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, null));
    	return result;
    }

    public SrmReleaseFilesResponse srmReleaseFiles(SrmReleaseFilesRequest req) { 
	TReturnStatus s = validation("srmReleaseFiles()");
    	SrmReleaseFilesResponse result = new SrmReleaseFilesResponse();    	
	
	if (s != null) {
	    result.setReturnStatus(s);
	    return result;
	}    	

	if ((req.getArrayOfSURLs() != null) && (req.getArrayOfSURLs().getUrlArray() != null)) {
	    URI[] surlArray = req.getArrayOfSURLs().getUrlArray();
	    TSURLReturnStatus[] curr = assignAll(surlArray, TSRMUtil.createReturnStatusSuccess(null));
	    if (curr != null) {
		result.setArrayOfFileStatuses(TSRMUtil.convertToArray(curr));
	    }
	}
	result.setReturnStatus(TSRMUtil.createReturnStatusSuccess(null));	
    	return result;
    }
    
    private TSURLReturnStatus[] assignAll(URI[] surlArray, TReturnStatus defaultStatus) {
	int total = surlArray.length;
	if (total > 0) {
	    TSURLReturnStatus[] arrayAll = new TSURLReturnStatus[total];
	    for (int i=0; i<total; i++) {
		arrayAll[i] = new TSURLReturnStatus();
		arrayAll[i].setSurl(surlArray[i]);
		arrayAll[i].setStatus(defaultStatus);
	    }
	    return arrayAll;
	}
	return null;
    }
     
    public boolean getBooleanFlag(ArrayOfTExtraInfo info, String keyName) {
	if (info == null) {
	    return false;
	}
	TExtraInfo[] infoList = info.getExtraInfoArray();
	if (infoList == null) {
	    return false;
	}
	for (int i=0; i<infoList.length; i++) {
	    TExtraInfo curr = infoList[i];
	    String key = curr.getKey();
	    TSRMLog.debug(this.getClass(), null, "event=getBooleanFlag key="+key, " value="+curr.getValue());
	    if (key.equalsIgnoreCase(keyName)) {
		String value=curr.getValue();
		if (value != null) {
		    return Boolean.valueOf(value);
		}
	    }
	}
	return false;
    }
}
