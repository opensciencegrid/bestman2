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

package gov.lbl.srm.storage.Permanent;

import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.intf.*;
//import gov.lbl.srm.transfer.mss.hpss.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import org.apache.axis.types.URI;
import java.io.File;
import java.util.*;
import gov.lbl.srm.storage.*;

public class TMSSTransfer implements IMSSTxf {
    public final static String _DefSlash = "/";

    //SRM_MSS _mssTxf = null; // caller sets SRM_MSS_HPSS or SRM_MSS_NCAR, etc
    SRM_MSSIntf _hpssTxf = null;

    public TMSSTransfer(SRM_MSSIntf txf) {
	_hpssTxf = txf;
    }

     private void pickupMSSToken() {	 
	if (!TSRMServer._gProcessingUnit.acquireIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    throw new TSRMException("Cannt acquire mss ftp semaphore.", false);
	}	
	TSRMLog.debug(this.getClass(), null, "event=MSSToken+", null);
    }
    
    private void dropOffMSSToken() {
	TSRMServer._gProcessingUnit.releaseIOToken(TSupportedURLOnLocalHPSS._DefProtocolStr);
	TSRMLog.debug(this.getClass(), null, "event=MSSToken-", null);
    }

    private boolean isDone(String currMethodName, MSS_MESSAGE status) {		
	if ((status != null) && (status != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED)) {
	    TSRMLog.debug(this.getClass(), null, "isDone", "method="+currMethodName+" status="+status.toString());
	    return true;
	}
	if (status == null) {
	    TSRMLog.debug(this.getClass(), null, "isDone", "method="+currMethodName+" status=null");
	} else {
	    TSRMLog.debug(this.getClass(), null, "isDone", "method="+currMethodName+" status="+status.toString());
	}
	return false;
    }

    private SRM_ACCESS_INFO createMSSAccessInfo(TDeviceAccessInfo info) {
	if (info == null) {
	    info = TDeviceAccessInfo.gCreateDefault();
	}

	//String uid = info.getUid();
	//String pwd = info.getPwd();
	
	SRM_ACCESS_INFO accessInfo = null;
	try {
	    //accessInfo = _hpssTxf.generateAccessInfo(uid, uid, pwd);
	    accessInfo = _hpssTxf.generateAccessInfo(info.getRecords());
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "createMSSAccessInfo()", e);
	    throw new TSRMExceptionNotAuthorized("!failed with generateAccessInfo(): "+e.getMessage());
	}

	if (accessInfo == null) {
	    throw new TSRMException("NULL accessInfo generated from: "+_hpssTxf, false);
	}

	
	if (accessInfo.getPasswd() != null) {
	    TSRMLog.debug(this.getClass(), null, "event=creatingMSSaccessInfo", "uid="+accessInfo.getUserId() +
			 " login="+accessInfo.getLogin()+" pwdSize="+accessInfo.getPasswd().length());
	} else {
	    TSRMLog.debug(this.getClass(), null, "event=creatingMSSaccessInfo", "uid="+accessInfo.getUserId() +
			 " login="+accessInfo.getLogin());
	}
	
	return accessInfo;
    }


    public java.util.Vector getStatusReport(String keyPrefix) {
	try {
	    SRM_PING_STATUS pingStatus = ping();
	    if (pingStatus == null) {
		TSRMLog.debug(this.getClass(), "getStatusReport", "event=error reason=ping_returned_null", null);
		return null;
	    }
	    java.util.Vector result = new java.util.Vector();
	    result.add(TSRMUtil.createTExtraInfo(keyPrefix+" Status", pingStatus.getStatus().toString()));
	    result.add(TSRMUtil.createTExtraInfo(keyPrefix+" LastAccessTime", pingStatus.getLastAccessedTime()));
	    return result;
	} catch (Exception ex) {
	    TSRMLog.exception(this.getClass(), "ping() failed, details:", ex);	 
	    return null;
	}
    }

    public SRM_PING_STATUS ping() {
	try {
	    return _hpssTxf.srmPing();
	} catch  (Exception e) {
	    TSRMLog.exception(this.getClass(), "ping failed", e);
	    return null;
	}	
    }

    public String getHomeDir(TDeviceAccessInfo info) {
	String currMethodName = "mssGetHomeDir()";
	try {
	    SRM_STATUS status = _hpssTxf.mssGetHomeDir(createMSSAccessInfo(info));
	    String rid = null;

	    if (status == null) {
		return null;
	    }
	    if (!isDone(currMethodName, status.getStatus())) {
		rid = status.getRequestToken();
		currMethodName += " rid="+rid;

		TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, null);
		//long startTime = System.currentTimeMillis();
		
		while (true) {
		    TSRMUtil.sleep(1500);
		    Object currStatus = _hpssTxf.checkStatus(rid);	 
		    if (currStatus instanceof SRM_STATUS) {
			status = (SRM_STATUS)currStatus;			
			if (isDone(currMethodName, status.getStatus())) {
			    break;
			}
		    }
		} 		 
	    }
	    if ((status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE))	       
	    {
		return status.getSourcePath();
	    }
	} catch  (Exception e) {
	    TSRMLog.exception(this.getClass(), currMethodName+" failed", e);
	}	    
	return null;
    }

    public void abort(String rid) {
	try {
	    _hpssTxf.abortRequest(rid);
	    TSRMLog.debug(this.getClass(), "abortMSSOK", "rid="+rid, null);
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "abortMSSFailed"+rid, e);
	}
    }

    // may take some time

    private SRM_FILE callGetSize(String path, SRM_ACCESS_INFO accessInfo) {
	String currMethodName = "mssSize() ";
	
	SRM_FILE resultStatus = null;
	pickupMSSToken();

	try {
	    //String rid = _hpssTxf.srmGetFileSize(path, accessInfo).getRequestToken();
	    resultStatus = _hpssTxf.srmGetFileSize(path, accessInfo, false, false);

	    if (resultStatus != null) {		
		if (!isDone(currMethodName, resultStatus.getStatus())) {
		    String rid = resultStatus.getRequestToken();
		    currMethodName +=" rid="+rid+" ";
		    TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "path="+path);    
		    while (true) {
			TSRMUtil.sleep(1500);
			Object status = _hpssTxf.checkStatus(rid);		
			
			if (status instanceof SRM_FILE) {
			    resultStatus = (SRM_FILE)status;
			    
			    if (isDone(currMethodName, resultStatus.getStatus())) {
				break;
			    }
			}
		    }
		}
	    }
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), currMethodName, e);
	    //e.printStackTrace();

	    throw new TSRMException(currMethodName+e.getMessage(), false);	    
        } finally {
	    dropOffMSSToken();
	}

	if (resultStatus == null) {
	    throw new TSRMException(currMethodName+" null MSS return", false);
	}

	return resultStatus;
    }

    private long checkGetSizeResult(TDeviceAccessObj obj, SRM_FILE resultStatus, int numRetriesLeft) {
	String currMethodName = "mssSizestatus() "+resultStatus.getRequestToken();

	if (resultStatus.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
	    TSRMLog.debug(this.getClass(), "checkGetSize", "method="+currMethodName+" result=is_a_file"+ 
			 " timestamp="+resultStatus.getTimeStamp(), " bytesize="+resultStatus.getSize());		       
	    obj.setIsFile(); 
	    obj.setTrustedSize(resultStatus.getSize(), TSRMUtil.parseTimeStr(resultStatus.getTimeStamp()));
	    
	    return resultStatus.getSize();		    
	} else if (resultStatus.getStatus() == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {	    
	    obj.setIsDir();
	    return -1;
	} else if (isRetriableStatus(resultStatus.getStatus()) && (numRetriesLeft > 0)) {
	    TSRMLog.debug(this.getClass(), "checkGetSize", "method="+currMethodName, "event=retry");
	    return -2;
	} else {
	    //throw new TSRMException(currMethodName+resultStatus.getFile()+ " ==> "+resultStatus.getStatus().toString(), false);
	    throw new TSRMExceptionRelay(currMethodName+resultStatus.getFile(), resultStatus.getStatus());
	}
    }

    public long getSize(TDeviceAccessObj obj) {
	String path = (String)(obj.getObj());
	SRM_ACCESS_INFO accessInfo = createMSSAccessInfo(obj.getAccessInfo());

	int counter = 1;

	while (counter >= 0) {
	    SRM_FILE resultStatus  = callGetSize(path, accessInfo);
	    long result = checkGetSizeResult(obj, resultStatus, counter);
	    counter--;
	    if (result >= -1) {
		return result;
	    } 
	}	
	throw new TSRMException("Unexpected to be there!!", false);
    }


    private boolean isRetriableStatus(MSS_MESSAGE m) {
	return (m == MSS_MESSAGE.SRM_MSS_MSS_ERROR);
    }
    
    // NOTE: assuming the last entry is the file name....
    public void mkdir(String topDir, String[] subdirs, TDeviceAccessInfo accessInfo, boolean pathIsAFile) {
	pickupMSSToken();
	SRM_STATUS status = null;

	try {	    
	    SRM_PATH dir = new SRM_PATH();
	    dir.setDir(topDir);
	    
	    SRM_PATH curr = dir;
	    if (subdirs != null) {
		int num = subdirs.length;
		if (pathIsAFile) {
		    num = subdirs.length-1;
		}
		for (int i=0; i<num; i++) {
		    SRM_PATH subdirPath = new SRM_PATH();
		    subdirPath.setDir(curr.getDir()+_DefSlash+subdirs[i]);
		    curr.setSubPath(subdirPath);
		    curr = subdirPath;
		}
	    }

	    //test("\t", dir);
	    String currMethodName = "mssMkdir() ";
	    boolean srmnocipher = true;
	    boolean srmnonpassivelisting = false;

	    SRM_ACCESS_INFO mssAccessInfo = createMSSAccessInfo(accessInfo);

	    //String rid =_hpssTxf.mssMakeDirectory(dir, srmnocipher, srmnonpassivelisting, mssAccessInfo).getRequestToken();
	    status = _hpssTxf.mssMakeDirectory(dir, srmnocipher, srmnonpassivelisting, mssAccessInfo);

	    if (status == null) {
		throw new TSRMException("null MSS return", false);
	    }
	    if (!isDone(currMethodName, status.getStatus())) {		
		String rid = status.getRequestToken();
		currMethodName += " rid="+rid;
		
		TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "curr="+curr.getDir()+" topDir="+topDir);
		//SRM_STATUS status = null;
		
		while (true) {
		    TSRMUtil.sleep(1500);
		    Object currStatus = _hpssTxf.checkStatus(rid);	       
		    
		    if (currStatus instanceof SRM_STATUS) {
			status = (SRM_STATUS)currStatus;
			if (isDone(currMethodName, status.getStatus())) {
			    break;
			}
		    }
		}
	    }
	    if ((status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) || 
		(status.getStatus() == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) ||
		(status.getStatus() == MSS_MESSAGE.SRM_MSS_FILE_EXISTS) )
	    {
		return;
	    }
	} catch (Exception e) {
	    throw new TSRMException("mssmkdir() failed, reason:"+e.getMessage(), false);
	} finally {
	    dropOffMSSToken();
	}

	throw new TSRMExceptionRelay(status);
    }

    public String bringToDisk(TDeviceAccessObj hpssFile, TSRMLocalFile localTgt) {
	String src = (String)(hpssFile.getObj());
	String tgt = localTgt.getTxfURI(TSRMTxfProtocol.FILE).getPath();
	

	try {
	    File ff = (File)(localTgt.getPhysicalLocation().getRef().getObj());
	    ff.createNewFile();
	    TPlatformUtil.chmod(tgt, true);

	SRM_ACCESS_INFO accessInfo = createMSSAccessInfo(hpssFile.getAccessInfo());

	int counter = 1;

	String currMethod = "bringToDisk():";
	while (counter >= 0) {
	    SRM_MSSFILE_STATUS status = callmssGet(src, tgt, accessInfo);
	    counter --;

	    if (status == null) {
		throw new TSRMException(currMethod+" gets NULL status."+localTgt.getCanonicalPath(), false);
	    } else if (status.getStatus() == MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
		// timed out
		return status.getRequestToken();
	    } else if (status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
		return null;
	    } else if (!isRetriableStatus(status.getStatus()) || (counter < 0)) {
		throw new TSRMException(currMethod+status.getRequestToken()+ status.getExplanation(), false);
	    }
	    TSRMLog.debug(this.getClass(), null, "method="+currMethod+" token="+status.getRequestToken(), "event=will_be_retried.");
	}
	return null;
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	    //return null;
	    throw new TSRMException("hasIOException."+e.getMessage(), false);
	
	} finally {
	    TPlatformUtil.chmod(tgt, false);
	}
    }

    public SRM_MSSFILE_STATUS callmssGet(String src, String tgt, SRM_ACCESS_INFO accessInfo) {
	boolean timedOut = false; 

	pickupMSSToken();

	SRM_OVERWRITE_MODE mode = SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_YES;

	long fileSize = 0;

	String currMethodName = "mssget()";
	boolean srmnocipher = true;
	boolean srmnonpassivelisting = false;

	SRM_MSSFILE_STATUS status = null;
	try {	    
	    status = _hpssTxf.mssFileGet(src, tgt, fileSize, mode, srmnocipher, srmnonpassivelisting, accessInfo);

	    if (status == null) {
		throw new TSRMException("null MSS return", false);
	    }
	    if (!isDone(currMethodName, status.getStatus())) {
		String rid=status.getRequestToken();
		currMethodName +=" rid="+rid;
		
		TSRMLog.debug(this.getClass(), "mssGet", "event=started method="+currMethodName+" tgt="+tgt, "HPSSsrc="+src);
		long startTime = System.currentTimeMillis();
		long curr = 0;

	    TSRMUtil.sleep(35000); // alex claimed takes hpss 35+ seconds to stage
		while (!timedOut) {
		    TSRMUtil.sleep(1500);
		    Object currStatus = _hpssTxf.checkStatus(rid);	 
		    
		    if (currStatus instanceof SRM_MSSFILE_STATUS) {
			status = (SRM_MSSFILE_STATUS)currStatus;
			if (isDone(currMethodName, status.getStatus())) {
			    break;
			}
		    }

		    curr = System.currentTimeMillis();
		    timedOut = (curr - startTime > Config._mssTimeOutMilliSeconds);
		}
	    }
	} catch (Exception e) {
	    throw new TSRMException(currMethodName+" exception:"+e.getMessage(), false);
	} finally {
	    dropOffMSSToken();
	}
	/*
	if (!timedOut) {
	    if (status.getStatus() != MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
		
		throw new TSRMException(currMethodName+" transfer failed."+status.getExplanation(), false);
	    }
	    return null;
	} else {
	    return rid;
	}
	*/
	return status;
    }
  
    public String loadFromDisk(File fileOnDisk, TSRMLocalFile hpssFile) {             
	boolean timedOut = false;

	pickupMSSToken();

	SRM_OVERWRITE_MODE mode = SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_YES;

	String tgt = hpssFile.getTxfURI(TSRMTxfProtocol.GSIFTP).getPath();

	TSRMLog.debug(this.getClass(), null, "event=loadFromDisk", "path="+fileOnDisk.toString()+" tgt="+tgt);
			   
	long fileSize = fileOnDisk.length();

	String currMethodName = "mssput()";
	String rid = null;
	boolean srmnocipher = true;
	boolean srmnonpassivelisting = false;

	SRM_MSSFILE_STATUS status = null;
	try {
	    SRM_ACCESS_INFO accessInfo = createMSSAccessInfo(hpssFile.getPhysicalLocation().getDeviceAccessInfo());
	    status =  _hpssTxf.mssFilePut(fileOnDisk.toString(), tgt, fileSize, mode, srmnocipher, srmnonpassivelisting, accessInfo);

	    if (status != null) {
		if (!isDone(currMethodName, status.getStatus())) {
		    rid = status.getRequestToken();
		    currMethodName += " rid="+rid;
		    
		    TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "path="+fileOnDisk.toString()+" HPSS gt="+tgt);
		    long startTime = System.currentTimeMillis();
		    long curr = 0; 
			TSRMUtil.sleep(10000); // initial wait 
		    while (!timedOut) {
			TSRMUtil.sleep(1500);
			Object currStatus = _hpssTxf.checkStatus(rid);	 
			
			if (currStatus instanceof SRM_MSSFILE_STATUS) {
			    status = (SRM_MSSFILE_STATUS)currStatus;
			    if (isDone(currMethodName, status.getStatus())) {
				break;
			    }
			}
			curr = System.currentTimeMillis();
			timedOut = (curr - startTime > Config._mssTimeOutMilliSeconds);
		    }
		}
	    }
	} catch (Exception e) {
	    throw new TSRMException(currMethodName+" exception:"+e.getMessage(), false);
	} finally {
	    dropOffMSSToken();
	}

	if (status == null) {
	    throw new TSRMException(currMethodName+" no status from MSS call.", false);
	}

	if (!timedOut) {
	    if (status.getStatus() != MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
		//throw new TSRMException(currMethodName+" transfer failed."+status.getStatus()+" exp:="+status.getExplanation(), false);
		throw new TSRMExceptionRelay("mssput error:"+status.getExplanation(), status.getStatus());
	    }
	    /*
	    // check sum?
	    if (hpssFile.getCurrentSize() != fileSize) {
		throw new TSRMException("pftp from disk to hpss corrupted", false);
	    }
	    */
	    hpssFile.setActualBytes(fileSize);
	    return null;
	} else {
	    return rid;
	}
    }        
    
    public String remove(TDeviceAccessObj hpssPathObj, boolean isDir, boolean recursive) {
	pickupMSSToken();

	String path = (String)(hpssPathObj.getObj());

	SRM_STATUS resultStatus = null;
	String currMethodName = "mssRemove() ";

	try {
	if (!isDir && (hpssPathObj.getTrustedSize() < 0)) {
	    return null; // do not call remove if known size is -1
	}
	    SRM_ACCESS_INFO mssAccessInfo = createMSSAccessInfo(hpssPathObj.getAccessInfo());
	    resultStatus = _hpssTxf.srmDelete(path, mssAccessInfo, isDir, recursive, false, false); 
	    if (resultStatus == null) {
		return currMethodName+" null MSS return.";
	    }
	    if (!isDone(currMethodName, resultStatus.getStatus())) {
		String rid = resultStatus.getRequestToken(); 
		currMethodName +=" rid="+rid;
		
		TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "path="+path);
		while (true) {
		    TSRMUtil.sleep(1500);
		    
		    Object status = _hpssTxf.checkStatus(rid);
		    
		    if (status instanceof SRM_STATUS) {
			resultStatus = (SRM_STATUS)status;
			
			if (isDone(currMethodName, resultStatus.getStatus())) {
			    break;
			}
		    }
		}
	    }
	    if ((resultStatus.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE))	       
	    {
		return null;
	    } else {
		TSRMLog.debug(this.getClass(), null, "event=failed method="+currMethodName, "reason="+resultStatus.getExplanation());
		return currMethodName+" failed. Exp:"+resultStatus.getExplanation();
	    }
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), currMethodName, e);
	    return currMethodName+" exception. "+e.getMessage();
	} finally {
	    dropOffMSSToken();
	}
    }

    public void cp(TDeviceAccessObj src, TDeviceAccessObj tgt, 
		   SRM_OVERWRITE_MODE overwriteMode, boolean recursive) 
    {
	pickupMSSToken();

	String tgtPath = (String)(tgt.getObj());
	String srcPath = (String)(src.getObj());

	boolean timedOut = false;
	String currMethodName = "mssCp() ";
	SRM_STATUS status = null;
	SRM_STATUS resultStatus = null;
	try {
	    SRM_ACCESS_INFO mssAccessInfo = createMSSAccessInfo(tgt.getAccessInfo());
	    resultStatus = _hpssTxf.srmCopy(srcPath, tgtPath, overwriteMode, recursive, false, false, mssAccessInfo);
	    
	    if (resultStatus == null) {
		throw new TSRMException("null MSS return", false);
	    } 
	    if (!isDone(currMethodName, resultStatus.getStatus())) {
	    
		String rid = resultStatus.getRequestToken(); 
		currMethodName +=" rid="+rid;
		TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "srcPath="+srcPath+" tgt="+tgtPath);

		long startTime = System.currentTimeMillis();
		long curr = 0; 
		
		while (!timedOut) {
		    TSRMUtil.sleep(1500);
		    Object currStatus = _hpssTxf.checkStatus(rid);	 

		    if (currStatus instanceof SRM_STATUS) {
			status = (SRM_STATUS)currStatus;
			if (isDone(currMethodName, status.getStatus())) {
			    break;
			}
		    }
		    curr = System.currentTimeMillis();
		    timedOut = (curr - startTime > Config._mssTimeOutMilliSeconds);
		}
	    }
	}catch (Exception e) {
	    TSRMLog.exception(TMSSTransfer.class, "details", e);
	    throw new TSRMException(currMethodName+" exception:"+e.getMessage(), false);
	} finally {
	    dropOffMSSToken();
	}	

	if (!timedOut) {
	    if (status.getStatus() != MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
		//throw new TSRMException(currMethodName+" transfer failed."+status.getExplanation(), false);
		throw new TSRMExceptionRelay(currMethodName+" error:"+status.getExplanation(), status.getStatus());
	    }	    	    // check sum?		   
	} else {
	    throw new TSRMException(currMethodName+" timed out.", false);
	}
    }

    private void printMe(SRM_PATH resultStatus, String prefix) {
	TSRMUtil.startUpInfo("## Skipping printing the contents...");
	
	TSRMUtil.startUpInfo("=> path name="+resultStatus.getDir()+" is dir? ="+resultStatus.isDir());
	java.util.Vector files = resultStatus.getFids();
	TSRMUtil.startUpInfo(prefix+"=> # of files ="+files.size());

	java.util.Vector subdirs = resultStatus.getSubPath();
	TSRMUtil.startUpInfo("=> subpath size ="+subdirs.size());

	if ((files.size() > 1) || (subdirs.size() > 0)) {
	    if (!resultStatus.isDir()) {
		TSRMUtil.startUpInfo("## did correction.");
		resultStatus.isDir(true); // simple correction if MSS API misinforms
	    }
	}// else if ((files.size() == 0) && (subdirs.size() == 0)) {
	//}
	    
    }

    public long  getModificationTime(TDeviceAccessObj hpssPathObj, boolean recursive, boolean nocipher, boolean nonpassivelisting) {
	// ls doesnt report it.
	try {
	    getSize(hpssPathObj);
	} catch (Exception e) {	    
	    TSRMLog.exception(TMSSTransfer.class, "details", e);
	}

	try {
	    ls (hpssPathObj, recursive, nocipher, nonpassivelisting);
	} catch (Exception e) {	   
	    TSRMLog.exception(TMSSTransfer.class, "details", e);
	}
	return -1;
    }

    private TMetaDataPathDetail convert(TDeviceAccessObj obj, String dir, SRM_FILE f) {
	TMetaDataPathDetail result = new TMetaDataPathDetail();

	if (dir == null) {
	    //result.setPath(TSRMUtil.createTSURL("mss", f.getFile()).toString());
	    result.setPath(obj.generateMetaDataPath(f.getFile()));
	} else {
	    //result.setPath(TSRMUtil.createTSURL("mss", dir+"/"+f.getFile()).toString());
	    result.setPath(obj.generateMetaDataPath(dir+"/"+f.getFile()));
	}

	result.setType(TFileType.FILE);
	result.setSize(TSRMUtil.createTSizeInBytes(f.getSize()));
	
	long timeStamp = TSRMUtil.parseTimeStr(f.getTimeStamp());
	if (timeStamp > 0) {
	    result.setLastModificationTime(TSRMUtil.createGMTTime(timeStamp));
	}
	result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from HPSS."));

	return result;
    }

    public TMetaDataPathDetail convert(TDeviceAccessObj obj, SRM_PATH p, boolean recursively) {
	if (p.isDir()) {
	    TMetaDataPathDetail result = new TMetaDataPathDetail();
	    result.setType(TFileType.DIRECTORY);
	    result.setPath(TSRMUtil.createTSURL("mss", p.getDir()).toString());
	    /*
	    if (!recursively) {
		
		if (p.getFids().size() > 0) {
		    TMetaDataPathDetail[] files = new TMetaDataPathDetail[p.getFids().size()];
		    for (int i=0; i<files.length; i++) {
			files[i] = convert(obj, p.getDir(), p.getFids(i));
		    }
		    result.setSubPaths(TSRMUtil.convertToArray(files));
		}
	    } else {
		*/
		int total = p.getFids().size() + p.getSubPath().size();

		if (total > 0) {
		    TMetaDataPathDetail[] contents = new TMetaDataPathDetail[total];
		    if (p.getFids().size() > 0) {
			for (int i=0; i<p.getFids().size(); i++) {
			    contents[i] = convert(obj, p.getDir(), p.getFids(i));
			}
		    }
		    if (p.getSubPath().size() > 0) {
			for (int i=0; i<p.getSubPath().size(); i++) {
			    contents[i+p.getFids().size()] = convert(obj, p.getSubPath(i), recursively);
			}
		    }
		    result.setArrayOfSubPaths(TSRMUtil.convertToArray(contents));
		}
	    
	    result.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, "Read from HPSS."));		
	    return result;
	} else {
	    return convert(obj, p.getDir(), p.getFids(0));
	}
    }

    public TMetaDataPathDetail ls(TDeviceAccessObj hpssPathObj, boolean recursive, boolean nocipher, boolean nonpassivelisting) {
	pickupMSSToken();

	TMetaDataPathDetail result = null;

	String path = (String)(hpssPathObj.getObj());	

	while (true) {
	    if (path.endsWith("/")) {
		path = path.substring(0, path.length()-1); // NCAR cannt take "/" at the end. So remove it.
	    } else {
		break;
	    }
	}

	SRM_PATH resultStatus = null;
	String currMethodName = "mssLs() ";
	try {
	    SRM_ACCESS_INFO mssAccessInfo = createMSSAccessInfo(hpssPathObj.getAccessInfo());
	    resultStatus = _hpssTxf.srmLs(path, mssAccessInfo, recursive, nocipher, nonpassivelisting); 

	    if (resultStatus == null) {
		throw new TSRMException("null MSS return", false);
	    }
	    if (!isDone(currMethodName, resultStatus.getStatus())) {			
		String rid = resultStatus.getRequestToken(); 
		currMethodName +=" rid="+rid;
		
		TSRMLog.debug(this.getClass(), null, "event=started method="+currMethodName, "path="+path +
			     " recursive="+recursive);
		while (true) {
		    TSRMUtil.sleep(1500);
		     TSRMLog.debug(this.getClass(), null, "event=checkMssStatus", "rid="+rid);
		    Object status = _hpssTxf.checkStatus(rid);
			 TSRMLog.debug(this.getClass(), null, "event=MssStatusReturned", "result="+status);
		    
		    if (status == null) {
			throw new TSRMException("null MSS status return", false);
		    }

		    if (status instanceof SRM_PATH) {
			resultStatus = (SRM_PATH)status;	
			if (resultStatus == null) {
			    throw new TSRMException("null MSS return", false);
			}
			if (isDone(currMethodName, resultStatus.getStatus())) {			
			    break;
			}
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMLog.exception(this.getClass(), currMethodName, e);
	    throw new TSRMException("Failed to do mssls()."+e.getMessage(), false);
	}  finally {
	    dropOffMSSToken();
	}
	   
	if (resultStatus == null) {
	    throw new TSRMException("null MSS return", false);	
	}

	if ((resultStatus.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE)) {	    
	    printMe(resultStatus, "");
	    if (!resultStatus.isDir()) {
		if (resultStatus.getFids().size() == 0) { // e.g. file does not exist
		    throw new TSRMExceptionInvalidPath("No such file in MSS.");
		}
		resultStatus.getFids(0).setFile(path);
		result = convert(hpssPathObj, null, resultStatus.getFids(0));
	    } else {
		result = convert(hpssPathObj, resultStatus, recursive);			    
	    }
	} else {
		TSRMLog.debug(this.getClass(), null, "event=failed method="+currMethodName, "reason="+resultStatus.getExplanation());
		//throw new TSRMException("Failed to ls() from MSS: MSSAPI returns code="+resultStatus.getStatus().toString()+" exp="+resultStatus.getExplanation(), false);
		throw new TSRMExceptionRelay("Failed to ls() from MSS:"+resultStatus.getExplanation(), resultStatus.getStatus());
	}	    

	return result;
    }
    
}
