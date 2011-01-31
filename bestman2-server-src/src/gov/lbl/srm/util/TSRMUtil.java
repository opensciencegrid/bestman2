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

package gov.lbl.srm.util;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import EDU.oswego.cs.dl.util.concurrent.*;

import gov.lbl.srm.storage.TSRMStorage;
import gov.lbl.srm.server.*;
import gov.lbl.srm.impl.*;
import gov.lbl.srm.storage.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.InetAddress;
import java.net.NetworkInterface;

import org.apache.axis.types.*;
import org.globus.util.ConfigUtil;

public class TSRMUtil {
   public static final String _DefMsgInvalidCredential 		= "Credential is empty or invalid.";
   public static final String _DefMsgCreateAccountFailed 	= "Account is not created in SRM.";
   public static final String _DefBadInput 		       	= "Bad input format received.";
   public static final String _DefInternalError 		= "Internal Error.";
   public static final String _DefSystemBusy			= "System is currently full.";
   public static final String _DefNoSuchUser			= "No such user.";
   public static final String _DefNoSuchRequestToken		= "No such request for the user.";
   public static final String _DefNoSuchFileRequestToken	= "No such file request.";
   public static final String _DefNoSuchFileRequest		= "No such file request with the given surl";
   public static final String _DefNoSuchSpaceToken		= "No such space token for this user.";
   public static final String _DefInProgress			= "File transfer is in progress.";
   public static final String _DefInvalidPath			= "The path is not recognized by this SRM.";
   public static final String _DefInvalidLocalPath		= "The path is not local to this SRM. Please verify the service address";
   public static final String _DefInvalidProtocol		= "Protocol used is not supported for the specific SRM function";
   public static final String _DefNoSuchPath			= "No such path exist in this SRM!";
   public static final String _DefCheckDetails			= "Details shall be in the related outputs";
   public static final String _DefEmptyInputs			= "Empty input values";
   public static final String _DefNullToken			= "NULL token";
   public static final String _DefFileTypeMismatch		= "FileType is not as requested.";
   public static final String _DefNoOverwrite                   = "file already exists, and no overwrite is allowed from the request.";
   public static final String _DefConflictStorageType           = "file already exists, and the provided fileStorageType does not match with the existing one. Cannt overwrite.";

   public static final Mutex _tokenCounterMutex                 = new Mutex();
   protected static int _numTokensIssued                           = 0; 
   public static final Random _randomGenerator                        = new Random(System.currentTimeMillis());	

   public static final long   MAX_MSG_SIZE                  = 104857600; // 100MB

   public static String generatePathStr(String tokenID, String filename) {
       return tokenID+"-"+TSRMUtil.getNextRandomUnsignedInt()+"/"+filename;
   }

   public static int getNextRandomUnsignedInt() {
       int out = _randomGenerator.nextInt();
       if (out < 0) {
	   out = 0 - out;
       } 
       return out;
   }

   public static void noNegative(boolean isNegative, String comment) {
	if (isNegative) {
	    TSRMUtil.startUpInfo("No negative is allowed for entry: "+comment);
	    System.exit(1);
	}
   }

   public static TSURLLifetimeReturnStatus createTSURLLifetimeReturnStatus(URI uri, TReturnStatus status)
    {
	TSURLLifetimeReturnStatus result = new TSURLLifetimeReturnStatus();
	result.setSurl(uri);
	result.setStatus(status);
	return result;
    }

    public static TSURLLifetimeReturnStatus createTSURLLifetimeReturnStatus(URI uri, TStatusCode statusCode, String exp)
    {
	TReturnStatus status = createReturnStatus(statusCode, exp);
	return createTSURLLifetimeReturnStatus(uri, status);
    }
					
    public static TReturnStatus createReturnStatus(TSRMException e) {
	return createReturnStatus(e.getStatusCode(), e.getMessage());
    }

    public static TReturnStatus createReturnStatus(RuntimeException e) {
	return createReturnStatus(TStatusCode.SRM_FAILURE, e.getMessage());
    }

    public static TReturnStatus createReturnStatus(TStatusCode code, String exp) {
	TReturnStatus status = new TReturnStatus();
	
	status.setStatusCode(code);
	status.setExplanation(exp);
	
	return status;
    }

    public static TReturnStatus createReturnStatusSuccess(String exp) {
	return createReturnStatus(TStatusCode.SRM_SUCCESS, exp);
    }

    public static TReturnStatus createReturnStatusNotAuthorized(String exp) {
	return createReturnStatus(TStatusCode.SRM_AUTHORIZATION_FAILURE, exp);
    }

    public static TReturnStatus createReturnStatusQueued(String exp) {
	return createReturnStatus(TStatusCode.SRM_REQUEST_QUEUED, exp);
    }

    public static TReturnStatus createReturnStatusTimedOut(String exp) {
	return createReturnStatus(TStatusCode.SRM_REQUEST_TIMED_OUT, exp);
    }

    public static TReturnStatus createReturnStatusSuspended(String exp) {
	return createReturnStatus(TStatusCode.SRM_REQUEST_SUSPENDED, exp);
    }

    public static TReturnStatus createReturnStatusAborted(String exp) {
	return createReturnStatus(TStatusCode.SRM_ABORTED, exp);
    }

    public static TReturnStatus createReturnStatusFailed(String exp) {
	return createReturnStatus(TStatusCode.SRM_FAILURE, exp);
    }

    public static TReturnStatus createReturnStatusInvalidPath(String exp) {
	return createReturnStatus(TStatusCode.SRM_INVALID_PATH, exp);
    }

    public static TReturnStatus createReturnStatusNoPermission(String exp) {
	return createReturnStatus(TStatusCode.SRM_AUTHORIZATION_FAILURE, exp);
    }

    public static boolean acquireSync(Sync m) {
    	try {
	    m.acquire();
	    return true;
    	} catch (InterruptedException e) {
	    e.printStackTrace();
	    return false;
    	}
    }
    
    public static boolean releaseSync(Sync m) {
    	m.release();
    	return true;
    }

    public static long parseTimeStr(String timeStamp) {
	timeStamp = timeStamp.trim();
	int pos = timeStamp.indexOf(":");
	try {
	    if (pos > 0) {
		Calendar curr = Calendar.getInstance();
		int currYear = curr.get(Calendar.YEAR);
		timeStamp +=" "+currYear;

		SimpleDateFormat formatter = new SimpleDateFormat("MMM d HH:mm yyyy");
		Date d = (java.util.Date)formatter.parse(timeStamp);
		return d.getTime();
	    } else {
		SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
		Date d = (java.util.Date)formatter.parse(timeStamp);
		return d.getTime();
	    }	    
	} catch (Exception e) {
	    TSRMLog.exception(TSRMUtil.class, "parseTimeStr()"+timeStamp, e);
	    //e.printStackTrace();
	    
	    return -1;
	}
    }
	
    public static void cleanFilePath(String location) {
	//File f = new File(location);
	File f = initFile(location);
	f.delete();
	
	if (f.getParentFile() != null) {
	    f.getParentFile().delete();
	}
	TSRMUtil.startUpInfo(".... now cleaned: "+location);
    }

    public static void wipe(TSRMLocalFile f) {
	TSRMLocalDir p = f.getParent();

	if (p != null) {
	    p.rmFileName(f.getName());
	}

	while (p != null) {
	    if (p.isEmpty()) {		
		TSRMLocalDir p1 = p.getParent();
		if (p1 != null) {
		    p1.rmDirName(p.getName());
		}
		p = p1;
	    } else {
		break;
	    }
	}
    }

    public static boolean verifyDirectory(String path, long demandedFreeBytes) {
        TSRMUtil.startUpInfo("## verifying device for path:"+path+" contains claimed "+demandedFreeBytes+" bytes free space");
    	//java.io.File input = new File(path);
	java.io.File input = initFile(path);
    	if (input.exists()) {
	    if (input.isDirectory()) {
		//long size = input.length();
		//System.out.println("## length of ("+path+")" +size);
		try {
		    long actualFreebytes = TPlatformUtil.getFreeSpace(input);
		    TSRMUtil.startUpInfo("## ## actual free bytes = "+actualFreebytes);
		    //long demandedFreeBytes = sizeInMB*1024*1024;
		    if (demandedFreeBytes > actualFreebytes) {
			// not enough space
			TSRMUtil.startUpInfo("ERROR: Disk has < "+demandedFreeBytes+" bytes left. Exiting.");
			return false;
		    } 
		    return true;
		} catch (Exception e) {
		    e.printStackTrace();
		    return false;
		}
	    } else {
		TSRMUtil.startUpInfo("path("+path+") is NOT a directory!!");
		return false;
	    }
    	} else {
	    TSRMUtil.startUpInfo("path("+path+") does not exist!!!");
	    return false;
    	}
    }
    
    public static String generateRandomString() {
    	Random r = new Random();
    	/*
    	 byte[] bytes = new byte[100];
    	 r.nextBytes(bytes);
    	 return bytes.toString();
    	 */
    	return String.valueOf(r.nextLong());
    }

    public static String getTokenID() {	
	if (TSRMUtil.acquireSync(_tokenCounterMutex)) {
	    _numTokensIssued++;
	    TSRMUtil.releaseSync(_tokenCounterMutex);
	    return String.valueOf(_numTokensIssued-1);
	}
	return generateRandomString();
    }
    
	public static void sync(Object[] tidSet) {
		int max = 0;
        for (int i=0; i<tidSet.length; i++) {
        String curr = ((String)(tidSet[i])).substring(2);
        if (!curr.startsWith("0")) {
            int temp = Integer.parseInt(curr);
            if (temp+1 > max) {
            max = temp+1;
            }
        }
        }
		sync(max);
	}

    private static void sync(int expectedCounter) {
	TSRMLog.info(TSRMUtil.class, null, "event=sync", "_numTokens="+_numTokensIssued+" expected="+expectedCounter);
	if (_numTokensIssued < expectedCounter) {
	    _numTokensIssued = expectedCounter;
	}
    }

    public static TPermissionMode createDefaultClientPermission() {
	return TPermissionMode.RX;
    }   

    public static TUserPermission createDefaultOwnerPermission(String uid) {
	TUserPermission p = new TUserPermission();
	p.setUserID(uid);
	p.setMode(TPermissionMode.RWX);
	return p;
    }

    public static TGroupPermission createDefaultGroupPermission() {
	TGroupPermission p = new TGroupPermission();
	p.setGroupID("defaultGroup");
	p.setMode(TPermissionMode.RX);
	return p;
    }

    public static ArrayOfTUserPermission createDefaultUserPermission() {
    	TUserPermission[] result = new TUserPermission[2];
    	
	result[0] = new TUserPermission();
    	String world = "world";
    	result[0].setUserID(world);
    	result[0].setMode(TPermissionMode.R);
    	
	result[1] = new TUserPermission();
    	String self = "self";
    	result[1].setUserID(world);
    	result[1].setMode(TPermissionMode.RWX);
    	
    	ArrayOfTUserPermission col = new ArrayOfTUserPermission();
    	col.setUserPermissionArray(result);
    	return col;
    }
    
    public static ArrayOfTSURLLifetimeReturnStatus convertToArray(TSURLLifetimeReturnStatus[] statusArray) {
	ArrayOfTSURLLifetimeReturnStatus col = new ArrayOfTSURLLifetimeReturnStatus();
	col.setStatusArray(statusArray);
	return col;
    }

    public static ArrayOfTExtraInfo convertToArray(TExtraInfo[] statusArray) {
	ArrayOfTExtraInfo col = new ArrayOfTExtraInfo();
	col.setExtraInfoArray(statusArray);
	return col;
    }

    public static ArrayOfTExtraInfo convertToArray(Vector storageReport) {
	TExtraInfo[] info = new TExtraInfo[storageReport.size()];
	for (int i=0; i<storageReport.size(); i++) {
	    info[i] = (TExtraInfo)(storageReport.get(i));
	}
	return convertToArray(info);
    }

    public static TBringOnlineRequestFileStatus convert(TGetRequestFileStatus getReq) {
	TBringOnlineRequestFileStatus result = new TBringOnlineRequestFileStatus();
	result.setSourceSURL(getReq.getSourceSURL());
	result.setStatus(getReq.getStatus());
       
	result.setFileSize(getReq.getFileSize());
	result.setEstimatedWaitTime(getReq.getEstimatedWaitTime());
	result.setRemainingPinTime(getReq.getRemainingPinTime());

	if (result.getStatus().getStatusCode() == TStatusCode.SRM_FILE_PINNED) {
	    result.getStatus().setStatusCode(TStatusCode.SRM_SUCCESS);
	}
	return result;	
    }

    public static ArrayOfTBringOnlineRequestFileStatus convertToArray(TBringOnlineRequestFileStatus[] statusArray) {
    	ArrayOfTBringOnlineRequestFileStatus col = new ArrayOfTBringOnlineRequestFileStatus();
    	col.setStatusArray(statusArray);
    	return col;	
    }

    public static ArrayOfTGetRequestFileStatus convertToArray(TGetRequestFileStatus[] statusArray) {
    	ArrayOfTGetRequestFileStatus col = new ArrayOfTGetRequestFileStatus();
    	col.setStatusArray(statusArray);
    	return col;	
    }
    
    public static ArrayOfTPutRequestFileStatus convertToArray(TPutRequestFileStatus[] statusArray) {
    	ArrayOfTPutRequestFileStatus col = new ArrayOfTPutRequestFileStatus();
    	col.setStatusArray(statusArray);
    	return col;	
    }
    
    public static ArrayOfTCopyRequestFileStatus convertToArray(TCopyRequestFileStatus[] statusArray) {
    	ArrayOfTCopyRequestFileStatus col = new ArrayOfTCopyRequestFileStatus();
    	col.setStatusArray(statusArray);
    	return col;	
    }
    
    public static ArrayOfTSURLReturnStatus convertToArray(TSURLReturnStatus[] statusArray) {
    	ArrayOfTSURLReturnStatus col = new ArrayOfTSURLReturnStatus();
    	col.setStatusArray(statusArray);
    	return col;
    }

        
    public static ArrayOfAnyURI convertToArray(org.apache.axis.types.URI surl) {
	org.apache.axis.types.URI[] urlArray = new org.apache.axis.types.URI[1];
	urlArray[0] = surl;
	
	return TSRMUtil.convertToArray(urlArray);
    }

    /*
    public static ArrayOfTSURL  convertToArray(TSURL[] statusArray) {
    	ArrayOfTSURL col = new ArrayOfTSURL();
    	col.setSurlArray(statusArray);
    	return col;
    }
    */
    public static ArrayOfAnyURI  convertToArray(org.apache.axis.types.URI[] statusArray) {
    	ArrayOfAnyURI col = new ArrayOfAnyURI();
    	col.setUrlArray(statusArray);
    	return col;
    }

    /*
     public static ArrayOfTSURLInfo  convertToArray(TSURLInfo[] statusArray) {
    	ArrayOfTSURLInfo col = new ArrayOfTSURLInfo();
    	col.setSurlInfoArray(statusArray);
    	return col;
    }
    */
    
    public static ArrayOfTMetaDataPathDetail convertToArray(TMetaDataPathDetail[] detailArray) {
    	ArrayOfTMetaDataPathDetail col = new ArrayOfTMetaDataPathDetail();
    	col.setPathDetailArray(detailArray);
    	return col;
    }
    
    public static ArrayOfTMetaDataSpace convertToArray(TMetaDataSpace[] spaceArray) {
    	ArrayOfTMetaDataSpace col = new ArrayOfTMetaDataSpace();
    	col.setSpaceDataArray(spaceArray);
    	return col;
    }
    
    public static ArrayOfTGetFileRequest convertToArray(TGetFileRequest[] reqArray) {
    	ArrayOfTGetFileRequest col = new ArrayOfTGetFileRequest();
    	col.setRequestArray(reqArray);
    	return col;
    }
    
    public static ArrayOfTPutFileRequest convertToArray(TPutFileRequest[] reqArray) {
    	ArrayOfTPutFileRequest col = new ArrayOfTPutFileRequest();
    	col.setRequestArray(reqArray);
    	return col;
    }
    
    public static ArrayOfTCopyFileRequest convertToArray(TCopyFileRequest[] reqArray) {
    	ArrayOfTCopyFileRequest col = new ArrayOfTCopyFileRequest();
    	col.setRequestArray(reqArray);
    	return col;
    }
    
    public static ArrayOfTRequestSummary convertToArray(TRequestSummary[] sumArray) {
    	ArrayOfTRequestSummary col = new ArrayOfTRequestSummary();
    	col.setSummaryArray(sumArray);
    	return col;
    }

    public static TReturnStatus generateStatus(int finished, int notFinished, int failed) {
	if (notFinished == 0) {
	    if (failed == 0) {
		return createReturnStatus(TStatusCode.SRM_SUCCESS, null);
	    } else if (failed == finished) {
		return createReturnStatus(TStatusCode.SRM_FAILURE, null);
	    } else {
		return createReturnStatus(TStatusCode.SRM_PARTIAL_SUCCESS, null);
	    }
	} else {
	    return createReturnStatus(TStatusCode.SRM_REQUEST_INPROGRESS, null);
	}
	
    }
    
    /*
    public static ArrayOfTSpaceToken convertToArray(TSpaceToken[] tokenArray) {
    	ArrayOfTSpaceToken col = new ArrayOfTSpaceToken();
    	col.setTokenArray(tokenArray);
    	return col;
    }
    */
    /*
    public static ArrayOfTRequestToken convertToArray(TRequestToken[] tokenArray) {
    	ArrayOfTRequestToken col = new ArrayOfTRequestToken();
    	col.setRequestTokenArray(tokenArray);
    	return col;
    }
    */

    public static ArrayOfTRequestTokenReturn convertToArray(TUserRequest[] reqs) {
	TRequestTokenReturn[] resultArray = new TRequestTokenReturn[reqs.length];
	for (int i=0; i<reqs.length; i++) {
	    resultArray[i] = new TRequestTokenReturn();
	    resultArray[i].setCreatedAtTime(reqs[i].getCreationTime());
	    resultArray[i].setRequestToken(reqs[i].getID());
	}
	
    	return convertToArray(resultArray);
    }

    public static ArrayOfTRequestTokenReturn convertToArray(TRequestTokenReturn[] tokenArray) {
    	ArrayOfTRequestTokenReturn col = new ArrayOfTRequestTokenReturn();
    	col.setTokenArray(tokenArray);
    	return col;
    }

    public static ArrayOfString convertToArray(String[] strArray) {
	ArrayOfString result = new ArrayOfString();
	result.setStringArray(strArray);
	return result;
    }

    public static ArrayOfString convertToArray(String[] strArray1, String[] strArray2) {
        ArrayOfString result = new ArrayOfString();
	if (strArray1 == null) {
	  result.setStringArray(strArray2);
	} else if (strArray2 == null) {
	  result.setStringArray(strArray1);
	} else {
	  String[] addup = new String[strArray1.length+strArray2.length];
	  for (int i=0; i<strArray1.length; i++) {
	    addup[i] = strArray1[i];
	  }
	  for (int i=0; i<strArray2.length; i++) {
	    addup[i+strArray1.length]=strArray2[i];
	  }
	  result.setStringArray(addup);
	}
	return result;
    }

    public static String getAbsPath(String str) {
	return str;
    }

    public static String getAbsPath(java.io.File f) {
	if (f == null) {
	    return null;
	}
	return f.getPath(); // full surl is: TSRMTxfProtocol.FILE.generateURI(f).toString()
    }

    public static String getAbsPath(URI uri) {
	if (uri == null) {
	    return null;
	}

	int pos = uri.toString().indexOf("?SFN=");
	if (pos == -1) {
	    if (uri.getScheme().equalsIgnoreCase("file")) {
		return uri.toString().substring(5);
	    }
	    return uri.getPath();
	}
	return uri.toString().substring(pos+5);
    }

    public static TMetaDataPathDetail createTMetaDataPathDetail(URI path, TReturnStatus status) {
	TMetaDataPathDetail curr = new TMetaDataPathDetail();
	if (path == null) {
	    curr.setPath(null);
	} else {
	    curr.setPath(getAbsPath(path));
	}
	curr.setStatus(status);

	return curr;
    }

     public static TMetaDataPathDetail createTMetaDataPathDetail(String path, TReturnStatus status) {
	TMetaDataPathDetail curr = new TMetaDataPathDetail();
	curr.setPath(path);

	curr.setStatus(status);

	return curr;
    }

    public static String createTSpaceToken(TSRMStorage.iSRMSpaceToken token) {
	if (token != null) {
	    return token.getID();
	} else {
	    return null;
	}
    }
    
    public static org.apache.axis.types.UnsignedLong createTSizeInBytes(long val) {
	if (val >= 0) {
	    UnsignedLong size = new UnsignedLong(val);
	    return size;
	} else {
	    return null;
	}
    }

  // example 2006-06-27 11:33
  public static Calendar createGMTTime(String longisoDate, String longisoTime) {
        try {
	  Calendar result = Calendar.getInstance();
	  
	  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	  Date d = (java.util.Date)formatter.parse(longisoDate+" "+longisoTime);
	  return createGMTTime(d.getTime());
	} catch (Exception e) {
	  TSRMLog.exception(TSRMUtil.class, "parsingLongIso:"+longisoDate+" "+longisoTime, e);
	  return createGMTTime(-1);
	}
  }
    
    public static Calendar createGMTTime(long t) {
	Calendar c = Calendar.getInstance();
	c.setTimeInMillis(t);
	return c;
    }

   
    public static Integer createTLifeTimeInSeconds(long t, boolean refuseNegativeValue) {
	if (refuseNegativeValue && (t < 0)) {
	    return null;
	}

    	Integer result = new Integer((int)t);
    	return result;
    }
    

    public static TSURLInfo createTSURLInfo(URI from) {
	 TSURLInfo urlInfo = new TSURLInfo();
	 urlInfo.setSURL(from);
	 return urlInfo;
    }

    public static TSURLInfo createTSURLInfo(String surl) {
	try {
	    TSURLInfo urlInfo = new TSURLInfo();
	    urlInfo.setSURL(new URI(surl));
	    return urlInfo;
	} catch (URI.MalformedURIException e) {
	    //e.printStackTrace();
	    TSRMLog.exception(TSRMUtil.class, "failedWithSurl:"+surl, e);
	    return null;
	}
    }

    public static URI createTSURL(String defaultProtocol, String surl) {
	try {
	    return new URI(surl);
	} catch (URI.MalformedURIException e) {
	    return createTSURL(defaultProtocol+"://"+surl);
	}
    }

    public static URI createTSURL(String surl) {
	try {
	    return new URI(surl);
	} catch (URI.MalformedURIException e) {
	    e.printStackTrace();
	    TSRMLog.debug(TSRMUtil.class, null, "event=failedCreating surl=\""+surl+"\"", "reason="+e.getMessage());
	    return null;
	}
    }

    public static TSURLReturnStatus createTSURLReturnStatus(URI uri, TReturnStatus status) {
	TSURLReturnStatus result = new TSURLReturnStatus();
	result.setSurl(uri);
	result.setStatus(status);

	return result;
    }

    public static String createPath(String curr, String subpath) {
	// we assume subpath is one level below curr, so only need the endfilename

	int endPos = subpath.length();
	if (endPos >=2) {
	    endPos = endPos -2;
	}
	int lastSlashPos =subpath.lastIndexOf("/", endPos);
	
	if (lastSlashPos > 0) {
	    subpath = subpath.substring(lastSlashPos+1);
	} //else if (lastSlashPos == -1) {
	    //subpath = "/"+subpath;
	//}

	int pos = curr.indexOf("?");
	if (pos == -1) {
	    //subpath = curr+subpath;
	    return attachPath(curr, subpath);
	} else {
	    int sfnPos = curr.indexOf("SFN=");
	    if (sfnPos == -1) {
		//subpath = curr.substring(0, pos)+subpath+curr.substring(pos);
		return attachPath(curr.substring(0, pos), subpath+curr.substring(pos));
	    } else {
		if ((curr.charAt(sfnPos-1) != '?') && (curr.charAt(sfnPos-1) != '&')) {
		    //subpath=curr.substring(0, pos)+subpath+curr.substring(pos);
		    return attachPath(curr.substring(0, pos), subpath+curr.substring(pos));
		} else {
		    int sfnEnds = curr.indexOf("&", sfnPos+1);
		    if (sfnEnds == -1) {
			//subpath = curr+subpath;
			return attachPath(curr, subpath);
		    } else {
			//subpath = curr.substring(0, sfnEnds)+subpath+curr.substring(sfnEnds);
			return attachPath(curr.substring(0, sfnEnds), subpath+curr.substring(sfnEnds));
		    }
		}
	    }
	}

	//return subpath;
    }

    public static String attachPath(String orig, String subpath) {
	if (orig.endsWith("/")) {
	    return orig+subpath;
	} else {
	    return orig+"/"+subpath;
	}
    }

    public static String getEndFileName(String str) {	
	if (str.endsWith("/")) {
	    str = str.substring(0, str.length()-1);
	} 
	int idx = str.lastIndexOf("/");	
	
	// idx != -1, otherwise, malformed URL exception would have been thrown long ago from class URI
	return str.substring(idx+1);
    }

    public static boolean isRefToSRM(String surl, URI srmURI, URI incomingUrl) {
	boolean noServicePathCheck = true;
	int posSFNField = incomingUrl.toString().indexOf("?SFN=");	
	if (posSFNField > 0) {
	    noServicePathCheck = false;
	}
	//TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" srmURI="+srmURI.toString(), "incoming="+incomingUrl+" noServicePathCheckFlag="+noServicePathCheck);
	TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" srmURI="+srmURI.toString(), "incoming="+incomingUrl);
	try {
	    URI input = new URI(surl);
	    if (!input.getHost().equalsIgnoreCase(srmURI.getHost())) {
		String srmServerIP = java.net.InetAddress.getByName(srmURI.getHost()).getHostAddress();
		String inputIP = java.net.InetAddress.getByName(input.getHost()).getHostAddress();
		if (!srmServerIP.equalsIgnoreCase(inputIP)) {
		    TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" ip="+inputIP, "serverIP="+srmServerIP);
		    return false;
		}
	    }
	    if (input.getPort() != srmURI.getPort()) {
		TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" inputPort="+input.getPort(), "serverPort="+srmURI.getPort());
		return false;
	    }
	    if (noServicePathCheck) {
		return true;
	    }
	    if (input.getPath().equalsIgnoreCase(srmURI.getPath())) {
		return true;
	    }
	    TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" path="+input.getPath(), "serverPath="+srmURI.getPath());
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}

	return false;
    }


    //
    // the expected scheme to discover whether an surl refers to local is
    // through the discovery methods that's described in SRM.v2 spec
    // i.e. go through srm/srm.endpoint etc.
    //
    public static boolean isRefToSRM2(String surl, URI srmURI, String incomingUrl) {    	
	boolean noServicePathCheck = true;
	int posSFNField = incomingUrl.indexOf("?SFN=");	
	if (posSFNField > 0) {
	    noServicePathCheck = false;
	}
	
	try {
	    String hostPort = java.net.InetAddress.getByName(srmURI.getHost()).getHostAddress()+":"+srmURI.getPort();	    
	    int where = surl.indexOf(hostPort);
	    if (where > 0) {
		// returns true so lcg tests with srm://address/filepath can pass
		if (noServicePathCheck) {
		    return true;
		} else {
		
		    if (surl.equals(srmURI.toString())) {
			return true;
		    } else {
			return false; 
		    }
		}
	    } 
	    
	    hostPort = java.net.InetAddress.getByName(srmURI.getHost()).getHostName()+":"+srmURI.getPort();
	    where = surl.indexOf(hostPort);
	    if (where > 0) {
		// returns true so lcg tests with srm://address/filepath can pass
		if (noServicePathCheck) {
		    return true;
		} else {
		
		    String curr = srmURI.getScheme()+"://"+hostPort+srmURI.getPath();
		    if (surl.equals(curr)) {
			return true;
		    } else {
			return false;
		    }
		}
	    }
	} catch (java.net.UnknownHostException e) {
	    return false;
	}

	// check to see if hostname is a masked name
	//	String serverIp = java.net.InetAddress.getByName(srmURI.getHost());
	//String inputIp = java.net.InetAddress.getByName(surl);

    	return false; 
    }

    public static String getPassword(String prompt) {
        MaskingThread et = new MaskingThread(prompt);
        Thread mask = new Thread(et);
        mask.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String password = "";

        try {
           password = in.readLine();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }

        et.stopMasking();
        // return the password entered by the user
        return password;
    }

    public static void sleepAlert(long napInMilliseconds) {
	try {
	    Thread.sleep(napInMilliseconds);
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new TSRMException("Sleep was interrupted.", false);
	}
    }
    public static void sleep(long napInMilliseconds) {
	try {
	    Thread.sleep(napInMilliseconds);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static String getDefaultProxyStr() {
	try {
	    String proxyFile = "/tmp/x509up_u"+ConfigUtil.getUID();   

	    //File f = new File(proxyFile);
	    File f = initFile(proxyFile);
	    if (!f.exists()) {
		TSRMUtil.startUpInfo("............................ null proxy from:"+proxyFile);
		return null;
	    }
	    
	    FileInputStream fis = new FileInputStream(proxyFile);
	    BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
	    StringBuffer buf = new StringBuffer();
	    String ref = "";
	    while ((ref = bis.readLine()) != null) {
		buf.append(ref+"\n");
	    }
	    
	    bis.close();
	    fis.close();
	    
	    return buf.toString();
	} catch (IOException e) {
	    e.printStackTrace();
	    return null;
	} 
    }

     public static boolean getBooleanValueOf(String spec, String tokenName, char seperator) {
	String strValue = getValueOf(spec, tokenName, seperator, true);
	if (strValue == null) {
	    return false;
	}
	
	if (strValue.equalsIgnoreCase("true")) {
	    return true;
	}

	return false;
    }

    public static String getValueOf(String spec, String tokenName, char seperator, boolean allowsNullValue) {
	int posStarts = spec.indexOf(tokenName);
	if (posStarts >= 0) {
	    posStarts = posStarts + tokenName.length();
	    int posEnds = spec.indexOf(seperator, posStarts);
	    if (posEnds > 0) {
		return spec.substring(posStarts, posEnds).trim();
	    } else {
		return spec.substring(posStarts).trim();
	    }
	} else {
	    if (allowsNullValue) {
		return null;
	    }  
	    throw new RuntimeException("null value detected for "+tokenName+" in "+spec); 
	}
    }

    public static TTransferParameters createDefaultTxfParameters() {
	 String[] txfProtocols = new String[1];
	 txfProtocols[0] = "gsiftp";
	 ArrayOfString protocols = convertToArray(txfProtocols);
	 TTransferParameters result = new TTransferParameters();
	 result.setArrayOfTransferProtocols(protocols);
	 return result; 
     }

    public static TExtraInfo createTExtraInfo(String key, Object value) {
	if (value == null) {
	    return createTExtraInfo(key, "null");
	} else {
	    return createTExtraInfo(key, value.toString());
	}
    }

    public static TExtraInfo createTExtraInfo(String key, String value) {
	TExtraInfo result = new TExtraInfo();
	result.setKey(key);
	result.setValue(value);
	return result;
    }

	public static void startUpInfoSilent(Object info) {
		startUpInfoBasic(info, false);
	}
	public static void startUpInfo(Object info) {
		startUpInfoBasic(info, !Config._silent);
	}

    private static void startUpInfoBasic(Object info, boolean showOnConsole) {
	if (info == null) {
	    return;
	}
	if (showOnConsole) {
	    if (Config._isInitialized) {
		System.out.println(Config._dateFormatter.format(System.currentTimeMillis()).toString()+info);
	    } else {
		System.out.println(info);
	    }
	    TSRMLog.console(TSRMUtil.class, info.toString());
	} else {
	    TSRMLog.console(TSRMUtil.class, info.toString());
	}
    }

    public static void printout(Object info) {
	if (!Config._silent) {
	    System.out.println(info);
	} 
    }

    public static java.io.File initFile(String path) {
	if (Config._localFileAccessClass == null) {
	    return new java.io.File(path);
	}

	try {
	    java.lang.Class[] oneStrConstructor = new java.lang.Class[1];
	    oneStrConstructor[0] = String.class;
	    java.lang.reflect.Constructor constructor = Config._localFileAccessClass.getConstructor(oneStrConstructor);
	    Object[] oneStrInit = new Object[1];
	    oneStrInit[0] = path;
	    return (java.io.File) (constructor.newInstance(oneStrInit));
	} catch (Exception e) {
	    e.printStackTrace();
	    return new java.io.File(path);
	}
    }

    public static java.io.File initFile(File parent, String child) {
	if (Config._localFileAccessClass == null) {
	    return new java.io.File(parent, child);
	}

	try {
	    java.lang.Class[] fileConstructor = new java.lang.Class[2];
	    fileConstructor[0] = File.class;
	    fileConstructor[1] = String.class;
	    java.lang.reflect.Constructor constructor = Config._localFileAccessClass.getConstructor(fileConstructor);
	    Object[] twoInit = new Object[2];
	    twoInit[0] = parent;
	    twoInit[1] = child;
	    return (java.io.File) (constructor.newInstance(twoInit));
	} catch (Exception e) {
	    e.printStackTrace();
	    return new java.io.File(parent, child);
	}
    }

    public static java.io.File initFile(String parent, String child) {
	if (Config._localFileAccessClass == null) {
	    return new java.io.File(parent, child);
	}

	try {
	    java.lang.Class[] fileConstructor = new java.lang.Class[2];
	    fileConstructor[0] = String.class;
	    fileConstructor[1] = String.class;
	    java.lang.reflect.Constructor constructor = Config._localFileAccessClass.getConstructor(fileConstructor);
	    Object[] twoInit = new Object[2];
	    twoInit[0] = parent;
	    twoInit[1] = child;
	    return (java.io.File) (constructor.newInstance(twoInit));
	} catch (Exception e) {
	    e.printStackTrace();
	    return new java.io.File(parent, child);
	}
    }

    public static Class loadClass(String plugInPath, String userClassName) {
	Class implClass = null;
	final java.io.File jarFile = TSRMUtil.initFile(plugInPath); //new java.io.File(plugInPath);
	if (!jarFile.exists()) {
	    throw new RuntimeException("No such jar file: "+plugInPath+"/"+userClassName);
	}    
	java.net.URLClassLoader loader = 
	    (java.net.URLClassLoader) java.security.AccessController.doPrivileged (
		       new java.security.PrivilegedAction() {
			   public Object run() {
			       java.net.URL url = null;
			       try {
				   url = jarFile.toURI().toURL();
			       } catch (java.net.MalformedURLException ex) {
				   // do nothing					   
				   throw new RuntimeException("Oh no!"+ex);
			       }
			       java.net.URL[] ok = new java.net.URL[1];
			       ok[0] = url;
			       return new java.net.URLClassLoader(ok);
			   }}
		       );
	try {
	    implClass = Class.forName(userClassName, true, loader);
	} catch (ClassNotFoundException ex) {
	    throw new RuntimeException("No such class:"+userClassName);
	}   	    
	
	return implClass;
    }    

	public static String getCompiledDate(Class clazz) {
		try {
			 if (clazz == null) {
                 return null;
             }
             String result =org.theshoemakers.which4j.Which4J.which(clazz);
             if (result == null) {
                 return null;
             }
             int end = result.indexOf(".jar");
             if (end == -1) {
                 return null;
             }
             int begin = result.indexOf("/");
             if (begin > end) {
                 return null;
             }
             String jarFileName = result.substring(begin, end+4);
             //System.out.println(clazz.getName()+" detected jarFile=["+jarFileName+"]");
             TSRMLog.debug(TSRMUtil.class, null, "jarFileName="+jarFileName, null);
             java.util.jar.JarFile f = new java.util.jar.JarFile(jarFileName);
			
			 String className = clazz.getName().replace('.', '/')+".class";
			//System.out.println("classN="+className);
             java.util.zip.ZipEntry ze = f.getEntry(className);
             if (ze != null) {
                java.text.SimpleDateFormat dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z' ");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                return dateFormatter.format(ze.getTime());
			 }
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}

    public static void addVersionInfo(Vector storageReport, String compileDate) {
	storageReport.add(TSRMUtil.createTExtraInfo("backend_type", "BeStMan"));
	storageReport.add(TSRMUtil.createTExtraInfo("backend_version", TSRMServer.class.getPackage().getImplementationVersion()));
	storageReport.add(TSRMUtil.createTExtraInfo("backend_build_date", compileDate));	
	String[] gsiftpServers = TSRMTxfProtocol.GSIFTP.displayAllServers();
	if (gsiftpServers == null) {
	    return;
	}
	for (int i=0; i< gsiftpServers.length; i++) {
	    storageReport.add(TSRMUtil.createTExtraInfo("gsiftpTxfServers["+i+"]", gsiftpServers[i]));	  
	}
    }

    public static void addCallerDN(Vector storageReport) {
	String dn = TSRMService.gGetUID("srmPing");
	storageReport.add(TSRMUtil.createTExtraInfo("clientDN", dn));
	if (dn != null) {
	    String localIDStr = "localIDMapped";
	    if (Config._gumsClient != null) {
		localIDStr = "gumsIDMapped";
	    }
	    storageReport.add(TSRMUtil.createTExtraInfo(localIDStr, Config.getMappedID(dn, null)));
	} else {
	    storageReport.add(TSRMUtil.createTExtraInfo("localIDMapped", null));
	}

    }

    public static void addConfigEntries(int debugLevel, Vector storageReport) {
	if (debugLevel <= 0) {
	    return;
	}

	//storageReport.add(TSRMUtil.createTExtraInfo("====","==== below are the configuration entries used by the server ==="));
	storageReport.add(TSRMUtil.createTExtraInfo("configFileEntriesListBelow", "true"));
	Enumeration iter = Config._prop.propertyNames();
	while (iter.hasMoreElements()) {
	    String n = (String)(iter.nextElement());
	    addConfigProperty(storageReport, n);
	}
	storageReport.add(TSRMUtil.createTExtraInfo("Statics:",TSRMStatics.getReportStr()));       
    }
    private static void addConfigProperty(Vector storageReport, String propertyName) {
	storageReport.add(TSRMUtil.createTExtraInfo(propertyName, Config._prop.get(propertyName)));
    }

    public static void addStorageEntries(int debugLevel, Vector storageReport) {
	if (debugLevel <= 0) {
	    return;
	}

	Object[] spaceTypes = TSRMStorage._colSpaceManagers.keySet().toArray(); 
	for (int i=0; i<spaceTypes.length; i++) {
	    TSRMSpaceType currType = (TSRMSpaceType)spaceTypes[i];
	    TSpaceSkeleton currSpace = (TSpaceSkeleton)(TSRMStorage.getSpaceManager(currType));
	    //TSRMUtil.createTExtraInfo(currType.getRetentionPolicy().getValue(), currSpace.getCurrUsageReport());
	    String currRetentionPolicy = currType.getRetentionPolicy().getValue();
	    int currDeviceTotal = currSpace.getNumDevices();
	    for (int k=0; k<currDeviceTotal; k++) {
		TBasicDevice temp = currSpace.getNthDevice(k);
		String key = currRetentionPolicy+"|"+temp.getAccessLatency(); // "ALL*" is OSG requirement via asim 11/28/07
		storageReport.add(TSRMUtil.createTExtraInfo("ALL*"+key, temp.getUsageReport()));
		Vector statusReport = temp.getStatusReport(key); 
		if (statusReport != null) {
		    storageReport.addAll(statusReport);
		}
	    }
	}
    }

  public static int countIpAddresses() {
    int num =0;

    try {
      Enumeration<NetworkInterface> eni = NetworkInterface.getNetworkInterfaces();
      while (eni.hasMoreElements()) {
	NetworkInterface inet = eni.nextElement();
	if (!inet.isLoopback()) {
	  TSRMUtil.startUpInfo("## countIpAddress: loopback");
	  continue;
	}
	Enumeration<InetAddress> curr = inet.getInetAddresses();
	if (curr.hasMoreElements()) {
	  InetAddress i = curr.nextElement();
	  TSRMUtil.startUpInfo("## countIpAddress: found "+num+"th address="+i.getLocalHost().getCanonicalHostName());
	  num++;
	}
      }
    } catch (java.net.SocketException e) {
      TSRMUtil.startUpInfo("## countIpAddress, exception: "+e);
      e.printStackTrace();
    } finally {
      return num;
    }
  }
}

  /**
   * This class attempts to erase characters echoed to the console.
   */

class MaskingThread extends Thread {
     private volatile boolean stop;
     private char echochar = '*';

    /**
     *@param prompt The prompt displayed to the user
     */
     public MaskingThread(String prompt) {
        System.out.print(prompt);
     }

    /**
     * Begin masking until asked to stop.
     */
     public void run() {

        int priority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try {
           stop = true;
           while(stop) {
             System.out.print("\010" + echochar);
             try {
                // attempt masking at this rate
                Thread.currentThread().sleep(1);
             }catch (InterruptedException iex) {
                Thread.currentThread().interrupt();
                return;
             }
           }
        } finally { // restore the original priority
           Thread.currentThread().setPriority(priority);
        }
     }

    /**
     * Instruct the thread to stop masking.
     */
     public void stopMasking() {
        this.stop = false;
     }
    
     public static void assertNull(Object obj, String msg) {
	if (obj == null) {
	    throw new TSRMException("Unexpected error, "+msg, false);
	}
     }
    
  }
