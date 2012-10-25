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

package gov.lbl.srm.client;
 
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.storage.Volatile.TDiskDevice;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.SimpleDateFormat;

//httpg
import org.globus.axis.gsi.GSIConstants;
import org.ietf.jgss.GSSCredential;
import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.globus.gsi.gssapi.auth.*;

import org.apache.axis.types.*;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.utils.Options;
import org.apache.axis.SimpleTargetedChain;
import org.globus.axis.transport.*;
import org.apache.axis.transport.http.HTTPSender;
import org.globus.axis.util.Util;
//import org.globus.util.Util;

import org.gridforum.jgss.*;

class TokenInfo {
    TRetentionPolicyInfo _type;
    Integer _lifetimeObj = null;
    UnsignedLong _sizeObj = null;

    public TokenInfo() {
	_type = new TRetentionPolicyInfo();
	_type.setRetentionPolicy(TRetentionPolicy.REPLICA);
	//_type.setAccessLatency(TAccessLatency.ONLINE);
	_lifetimeObj = new Integer(100);
    }

    public void takeValue(String[] details) {
	int len = details.length;

	if (len >= 1) {
	    setType(details[0]);
	}

	if (len >= 2) {
	    setLifeTime(details[1]);
	}

	if (len >=3) {
	    setSize(details[2]);
	}
    }
	   
    public void setType(String value) {
	if (value.equalsIgnoreCase("po")) {
	    _type.setRetentionPolicy(TRetentionPolicy.CUSTODIAL);	    
	    _type.setAccessLatency(TAccessLatency.ONLINE);
	} else if (value.equalsIgnoreCase("pn")) {
	    _type.setRetentionPolicy(TRetentionPolicy.CUSTODIAL);	    
	    _type.setAccessLatency(TAccessLatency.NEARLINE);
	} else if (value.equalsIgnoreCase("d")) {
	    _type.setRetentionPolicy(TRetentionPolicy.OUTPUT);
	}
    }

    public void setLifeTime(String value) {
	int v = Integer.parseInt(value);
	if (v > 0) {
	    _lifetimeObj = null;
	    _lifetimeObj = new Integer(v);	
	}
    }

    public void setSize(String valueInMB) {	
	_sizeObj = null;

	long bytes = Long.parseLong(valueInMB);
	bytes = bytes * ((long)(1048576));
	//_sizeObj.setValue(Integer.parseInt(valueInMB)*1048576);
	_sizeObj = new UnsignedLong(bytes);
    }

    public void print() {
	if (_sizeObj == null) {
	    System.out.println(_type+", "+_lifetimeObj.intValue()+" seconds");
	} else {
	    System.out.println(_type+", "+_lifetimeObj.intValue()+" seconds,"+_sizeObj.longValue()+" bytes");
	}
    }
}

class InputInfo {
    String _uid = null;
    String _filename = null;
    String _seqFileName = null;
    String _mssUIDSrc = null;
    String _mssPWDSrc = null;

    String _mssUIDTgt = null;
    String _mssPWDTgt = null;

    //TokenInfo _tokenInfo = new TokenInfo();   
    TokenInfo _tokenInfo = null;
    String _userToken = null;

    boolean _doRecursive = false;
    boolean _showDetail = false;

    int _numLevel = 0;
    int _nCount = -1;
    TOverwriteMode _overwriteMode = TOverwriteMode.NEVER;
    int _nTxfLT = 100;
    int _nSurlLT = 100;

    public InputInfo(String[] args) {
	_uid = getUID(args);
	_filename = getInputName(args, "-file=");
	
	String overwriteStr = getInputName(args, "-overwrite=");
	if ((overwriteStr != null) && (overwriteStr.startsWith("t"))) {
	    _overwriteMode = TOverwriteMode.ALWAYS;
	}

	String txfLTStr = getInputName(args, "-txfLT=");
	if (txfLTStr != null) {
	    _nTxfLT = Integer.parseInt(txfLTStr);
	}

	String surlLTStr = getInputName(args, "-surlLT=");
	if (surlLTStr != null) {
	    _nSurlLT = Integer.parseInt(surlLTStr);
	}

	String levelStr = getInputName(args, "-level=");
	String countStr = getInputName(args, "-count=");
	if (levelStr != null) {
	    _numLevel = Integer.parseInt(levelStr);
	}
	if (countStr != null) {
	    _nCount = Integer.parseInt(countStr);
	}

	String doRecursive = getInputName(args, "-recursive=");
	if ((doRecursive != null) && (doRecursive.startsWith("t"))) {
	    _doRecursive=true;
	}
	String showDetail = getInputName(args, "-showdetail=");
	if ((showDetail != null) && (showDetail.startsWith("t"))) {
	    _showDetail=true;
	}

	_seqFileName = getInputName(args, "-seqFile=");
	checkSpaceToken(args);

	_mssUIDSrc = getInputName(args, "-mssUIDSrc=");
	_mssPWDSrc = getInputName(args, "-mssPWDSrc=");

	_mssUIDTgt = getInputName(args, "-mssUIDTgt=");
	System.out.println("_mssUIDTgt="+_mssUIDTgt);
	System.out.println("_mssUIDSrc="+_mssUIDSrc);
	_mssPWDTgt = getInputName(args, "-mssPWDTgt=");		
    }
    
    public ArrayOfTExtraInfo getMSSStorageInfo(boolean isSource) {
	if (isSource) {
	    return getSourceStorageInfo();
	} else {
	    return getTargetStorageInfo();
	}
    }

    public ArrayOfTExtraInfo junk(String extra) {
	TExtraInfo[] ssinfo = new TExtraInfo[3];
	ssinfo[0] = new TExtraInfo();
	ssinfo[0].setKey("gsiftpStream");
	ssinfo[0].setValue("3");
	
	ssinfo[1] = new TExtraInfo();
	ssinfo[1].setKey("bufferSize");
	ssinfo[1].setValue("1048576");
	
	ssinfo[2] = new TExtraInfo();
	ssinfo[2].setKey(extra);
	ssinfo[2].setValue("true");
	/*	
	ssinfo[2] = new TExtraInfo();
	ssinfo[2].setKey("dcau");
	ssinfo[2].setValue("true");
	ssinfo[3] = new TExtraInfo();
	ssinfo[3].setKey("protection");
	ssinfo[3].setValue("true");
	*/
	return (TSRMUtil.convertToArray(ssinfo));	    
    }
    private ArrayOfTExtraInfo getSourceStorageInfo() {
	if ((_mssUIDSrc != null) && (_mssPWDSrc != null)) {
	    return TDeviceAccessInfo.gCreateStorageSystemInfo(_mssUIDSrc, _mssPWDSrc);
	} else {
	    System.out.println("Using proxy for src storage info....");
	    return TDeviceAccessInfo.gCreateStorageSystemInfo("useProxy", TSRMUtil.getDefaultProxyStr());	
	}
    }

    private ArrayOfTExtraInfo  getTargetStorageInfo() {
	if ((_mssUIDTgt != null) && (_mssPWDTgt != null)) {
	    return TDeviceAccessInfo.gCreateStorageSystemInfo(_mssUIDTgt, _mssPWDTgt);
	} else {
	    System.out.println("Using proxy for tgt storage info....");
	    return TDeviceAccessInfo.gCreateStorageSystemInfo("useProxy", TSRMUtil.getDefaultProxyStr());	
	}
    }
   

    public boolean shouldReserveToken() {
	if (_tokenInfo == null) {
	    return false;
	}
	return true;
    }

    public boolean doSequenceTest(ISRM srm) {
	if (_seqFileName == null) {
	    return false;
	}

	Vector content = readContents(_seqFileName);

	if (content == null) {
	    return false;
	}

	for (int i=0; i<content.size(); i++) {
	    String curr = (String)(content.get(i));
	    System.out.println(".... "+i+"th test file name="+curr);
	    runTest(srm, curr);		
	}
	return true;
    }

    public boolean runTest(ISRM srm, String filename) {
	 Vector srcList = readContents(filename);
	 if (srcList == null) {
	     System.out.println("        no entries from:"+filename);
	     return false;
	 }
	 System.out.println("        entry size of:"+filename+"  is  "+srcList.size());
	 if (filename.startsWith("get")) {	   
	     SRMClient.testGet(srm, this, srcList);
	 } else if (filename.startsWith("put")) {
	     SRMClient.testPut(srm, this, srcList);
	 } else if (filename.startsWith("copy")) {
	     SRMClient.testCopy(srm, this, srcList);
	 } else if (filename.startsWith("ls")) {
	     SRMClient.testls(srm, this, srcList);
	 } else if (filename.startsWith("mkdir")) {
	     SRMClient.testMkdir(srm, this, srcList);
	 } else if (filename.startsWith("rmdir")) {
	     SRMClient.testRmdir(srm, this, srcList);
	 } else if (filename.startsWith("rmFile")) {
	     SRMClient.testRmfile(srm, this, srcList);
	 } else if (filename.startsWith("mv")) {
	     SRMClient.testMv(srm, this, srcList);
	 } else if (filename.startsWith("abortFiles")) {
	     SRMClient.testAbortFiles(srm, this, srcList);
	 } else if (filename.startsWith("abortReq")) {
	     SRMClient.testAbortReq(srm, this, srcList);
	 } else if (filename.startsWith("extFileLT")) {
	     SRMClient.testExtFileLT(srm, this, srcList);
	 } else if (filename.startsWith("reqSummary")) {
	     SRMClient.testReqSummary(srm, this, srcList);
	 } else if (filename.startsWith("getrid")) {
	     SRMClient.testGetRID(srm, this, srcList);
	 } else if (filename.startsWith("changeSpace")) {
	     SRMClient.testChangeSpace(srm, this, srcList);
	 }
	 return true;
    }


    public void print() {
        System.out.println("user specified id   ="+_uid);
	System.out.println("filename            ="+_filename);
	System.out.println("seq file name       ="+_seqFileName);

	System.out.println("requestToken first? ="+shouldReserveToken());
	if (shouldReserveToken()) {
	    System.out.print("    desired:     =");
	    _tokenInfo.print();
	} else {
	    if (_userToken != null) {
		System.out.println("user provided token id="+_userToken);
	    } 		
	}

	if ((_mssUIDSrc != null) && (_mssPWDSrc != null)) {
	    System.out.println("using src mss info:["+_mssUIDSrc+"]["+_mssPWDSrc+"]");
	} else if ((_mssUIDTgt != null) && (_mssPWDTgt != null)) {
	    System.out.println("using src mss info:["+_mssUIDTgt+"]["+_mssPWDTgt+"]");
	} else {
	    System.out.println("using proxy as MSS info.");
	}
    }


    public Vector  fileForRequest(String prefix) {
	System.out.println("Testing for: "+prefix);

	String name = _filename;
	if (_filename == null) {
	    name = prefix+".txt";
	} else if (!_filename.startsWith(prefix+".")) {
	    System.out.println("Will not read: "+_filename+". Expecting filenames to be: "+prefix+".*");
	    return null;
	}
	
	return readContents(name);
    }

    public Vector readContents(String name) {
	try {
	    Vector srcList = new Vector();
	    java.io.BufferedReader input = new java.io.BufferedReader(new java.io.FileReader(name));
	    while (input.ready()) {
		String currLine = input.readLine();
		if (currLine.charAt(0) == '#') {
		    // skip
		    //System.out.println("## skipping:"+currLine);
		} else {
		    srcList.add(currLine.trim()); 
		    //System.out.println("## input: "+currLine);
		}
	    }
	    return srcList;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;	  
	} 
    }

	
    public static String getUID(String[] args) {
	for (int i=1; i<args.length; i++) {
	    String prefix = "-uid=";
	    if (args[i].startsWith(prefix)) {
		return args[i].substring(prefix.length());
	    }
	}
	
	return null;
    }
    
    //
    //e.g. -getToken=V:200:300 (type:lifetime:size)
    //
    public void checkSpaceToken(String[] args) {
	for (int i=1; i<args.length; i++) {
	    if (args[i].equals("-getToken=yes")) {
		_tokenInfo = new TokenInfo();
		return;
	    } else if (args[i].startsWith("-getToken")) {
		String spec = args[i].substring(10);

		String[] details = spec.split(":");
		if (details == null) {
		    return;
		}
		
		_tokenInfo = new TokenInfo();
		_tokenInfo.takeValue(details);
	    } else if (args[i].startsWith("-useToken")) {	
		String idStr = args[i].substring(10);
		if (idStr.startsWith("\"")) {
		    _userToken = (idStr.substring(1,idStr.length()-1));
		} else {
		    _userToken = (idStr);
		}
	    } 
	}	    	   
    }

    public static String getInputName(String[] args, String prefix) {
	for (int i=1; i<args.length; i++) {
	    //String prefix = "-file=";
	    if (args[i].startsWith(prefix)) {
		return args[i].substring(prefix.length());
	    }
	}	   
	return null;
    }
    
}

public class SRMClient {

  public SRMClient() {System.out.println("init SRMClient");};

    public static boolean testBringOnline(ISRM srm, InputInfo info, Vector srcList) {
	// get a new token
	String fileToken = getToken(srm, info);
	
	String[] surlArray = new String[srcList.size()];
	for (int i=0; i<srcList.size(); i++) {
	    surlArray[i] = (String)(srcList.get(i));
	}
	try {
	checkBringOnlineStatus(srm, null, "asim:42(BringONLINE)");
	} catch (Exception e) {
	e.printStackTrace();
	}
	bringOnlineMultiFiles(srm, surlArray, fileToken, info);
	
	releaseSpace(srm, fileToken, true);
	return true;
    }

    public static boolean testGet(ISRM srm, InputInfo info, Vector srcList) {
	// get a new token
	String fileToken = getToken(srm, info);
	
	//TSRMUtil.sleep(10000);
	//updateToken(srm, fileToken, (long)2500, (long)2500, (long)3000);
	
	String[] surlArray = new String[srcList.size()];
	for (int i=0; i<srcList.size(); i++) {
	    surlArray[i] = (String)(srcList.get(i));
	    System.out.println(" surl="+surlArray[i]);
	}
	
	prepareToGetMultiFiles(srm, surlArray, fileToken, info);
	
	// next releasing token.

	if (fileToken != null) {
	    //releaseSpace(srm, fileToken, false);
	}
	return true;
    }
    
    public static void printAllTokens(ISRM srm) {
	SrmGetSpaceTokensRequest r = new SrmGetSpaceTokensRequest();
	
	String desc = null;
	r.setUserSpaceTokenDescription(desc);

	try {
	    SrmGetSpaceTokensResponse result = srm.srmGetSpaceTokens(r);
	    
	    ArrayOfString matched = result.getArrayOfSpaceTokens();
	    if (matched == null) {
		System.out.println("No tokens owned by this user with desc="+desc);
	    } else {
		String[] values = matched.getStringArray();
		System.out.println("Total owned: "+ values.length);
		for (int i=0; i<values.length; i++) {
		    System.out.println("\t"+values[i]);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static String getToken(ISRM srm, InputInfo info) {
	// get all tokens of this user:
	printAllTokens(srm);
	
	//
	String fileToken = null;
	
	if (info.shouldReserveToken()) {
	    SrmReserveSpaceResponse result = reserveSpace(srm, info);
	    fileToken = result.getSpaceToken();	    	    	       

	} else {
	    fileToken = info._userToken;
	}
	
	return fileToken;
    }

    public static boolean testPut(ISRM srm, InputInfo info, Vector srcList) {
	String fileToken = getToken(srm, info);
	
	/* 	  
	 // put files one at a time
	for (int i=0; i<srcList.size(); i++) {
	    String surl = (String)(srcList.get(i));
	    if (surl.equals("null")) {
		surl = null;
	    }
	    prepareToPutOneFile(srm, surl, fileToken);
	}
	*/
	
	// put files at bulk
	String[] surlArray = new String[srcList.size()];
	for (int i=0; i<srcList.size(); i++) {
	    surlArray[i] = (String)(srcList.get(i));
	}

	prepareToPutMultiFiles(srm, surlArray, fileToken, info);
	
	// release token
	if (fileToken != null) {
	    TSRMUtil.sleep(100000);
	    releaseSpace(srm, fileToken, false);
	}
	
	return true;
    }
    
    public static void printMetaData(String prefix, TMetaDataPathDetail detail) {
	System.out.println(prefix+" pathType ="+detail.getType());
	System.out.println(prefix+" path     ="+detail.getPath());
	System.out.println(prefix+" code     ="+detail.getStatus().getStatusCode());
	System.out.println(prefix+" exp      ="+detail.getStatus().getExplanation());
	System.out.println(prefix+" stor     ="+detail.getFileStorageType());
	System.out.println(prefix+" locality ="+detail.getFileLocality());

	if (detail.getOwnerPermission() != null) {
	    System.out.println(prefix+" owner   = "+detail.getOwnerPermission().getUserID());
	    System.out.println(prefix+" mode   = "+detail.getOwnerPermission().getMode());
	}
	if (detail.getGroupPermission()!= null) {
	    System.out.println(prefix+" group   = "+detail.getGroupPermission().getGroupID());
	    System.out.println(prefix+" mode   = "+detail.getGroupPermission().getMode());	
	}
	if (detail.getOtherPermission() != null) {
	    System.out.println(prefix+" otherMode   ="+detail.getOtherPermission());
	}

	if (detail.getLifetimeLeft() != null) {
	    System.out.println(prefix+" lifeTime left: "+detail.getLifetimeLeft().intValue());
	}

	if (detail.getSize() != null) {
	    System.out.println(prefix+" bytes="+detail.getSize().longValue());
	}
	if (detail.getPath() != null) {
	    System.out.println(prefix+" surl ="+detail.getPath().toString());
	}
	if (detail.getLastModificationTime() != null) {
	    System.out.println(prefix+" last accessed:"+detail.getLastModificationTime().toString());
	}	
	
	if (detail.getArrayOfSubPaths() != null) {
	    for (int i=0; i<detail.getArrayOfSubPaths().getPathDetailArray().length; i++) {
		printMetaData(prefix+"  "+i+"th ", detail.getArrayOfSubPaths().getPathDetailArray()[i]);
	    }
	}

	if (detail.getCreatedAtTime() != null) {
	    SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ssz ");		
	    System.out.println(prefix+" created at:"+formatter.format(detail.getCreatedAtTime().getTime()));
	}
	System.out.println();
    }
    
    public static String getTimeString() {
	Calendar curr = Calendar.getInstance();
	//DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ssz ");

	return formatter.format(curr.getTime());
    }

    public static void list(ISRM srm, URI[] path, InputInfo info) {
	System.out.println(getTimeString()+"listing files, size of input:"+path.length);
	int numLevel = info._numLevel; 
	boolean doFullDetail = info._showDetail;
	boolean doRecursive = info._doRecursive;
	int nCount = info._nCount;
	System.out.println("..... ls () numLevel="+numLevel+" count = "+nCount+" doFullDetail?"+doFullDetail+" recursive?"+doRecursive);
	try {
	    SrmLsRequest r = new SrmLsRequest();
	    r.setArrayOfSURLs(TSRMUtil.convertToArray(path));
	    r.setAllLevelRecursive(new Boolean(doRecursive));
	    if (numLevel >= 0) {
		r.setNumOfLevels(new Integer(numLevel));
	    }
	    if (nCount >= 0) {
		r.setCount(new Integer(nCount));
	    }
	    r.setFullDetailedList(new Boolean(doFullDetail));
	    //r.setFileStorageType(TFileStorageType.VOLATILE);
	    r.setStorageSystemInfo(info.getMSSStorageInfo(true));
	    
	    SrmLsResponse result = srm.srmLs(r);
	    if (result != null) {
		System.out.println("srmLs() status : "+result.getReturnStatus().getStatusCode());
		System.out.println("        exp    : "+result.getReturnStatus().getExplanation());
		if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {
		    System.out.println("        details: ");
		    showDetails(result.getDetails());
		} else {
		    showDetails(result.getDetails());
		    while (true) {
			System.out.println("...........rid="+result.getRequestToken());
			SrmStatusOfLsRequestRequest req = new SrmStatusOfLsRequestRequest();
			req.setRequestToken(result.getRequestToken());
			SrmStatusOfLsRequestResponse lsStatus = srm.srmStatusOfLsRequest(req);
			
			TStatusCode code = lsStatus.getReturnStatus().getStatusCode();
			System.out.println("......ls status code="+code);
			System.out.println("......          exp ="+lsStatus.getReturnStatus().getExplanation());
			if ((code != TStatusCode.SRM_REQUEST_QUEUED) && (code != TStatusCode.SRM_REQUEST_INPROGRESS)) {
			    //if (code == TStatusCode.SRM_SUCCESS) {
				showDetails(lsStatus.getDetails());
				//}
			    break;
			} else {
			    TSRMUtil.sleep(10000);
			}
		    }
		}
	    }		
	} catch (Exception e) {
	    System.out.println("--------Oh, maybe timed out. "+getTimeString());
	    e.printStackTrace();
	}
    }

    private static void showDetails(ArrayOfTMetaDataPathDetail detailArray) {
	if (detailArray == null) {
	    System.out.println("No detail array!");
	    return;
	}
	TMetaDataPathDetail[] details = detailArray.getPathDetailArray();
	if (details != null) {
	    for (int i=0; i<details.length; i++) {
		printMetaData("          "+i+" th: ", details[i]);
	    }
	} else {
	    System.out.println("Ls contains No details.");
	}
    }
    
    public static void mkdir(ISRM srm, TSURLInfo path) {
	System.out.println("\nmake dir, path="+path.getSURL());
	
	try {
	    SrmMkdirRequest r = new SrmMkdirRequest();
	    r.setSURL(path.getSURL());
	    r.setStorageSystemInfo(path.getStorageSystemInfo());
	    
	    SrmMkdirResponse result = srm.srmMkdir(r);
	    
	    if (result != null) {
		System.out.println("srmMkdir() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("           exp    :"+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static void printTSURLReturnStatusArray(ArrayOfTSURLReturnStatus arrayFileStatusObj) {
	if (arrayFileStatusObj != null) {
	    TSURLReturnStatus[] arrayFileStatus = arrayFileStatusObj.getStatusArray();
	    for (int i=0; i<arrayFileStatus.length; i++) {
		System.out.println("      => "+i+"th:");
		System.out.println("         surl  ="+arrayFileStatus[i].getSurl());
		System.out.println("         status="+arrayFileStatus[i].getStatus().getStatusCode());
		System.out.println("         exp   ="+arrayFileStatus[i].getStatus().getExplanation());
	    }
	}
    }

    public static void rmFileArray(ISRM srm, ArrayOfAnyURI pathArray) {
	if ((pathArray == null) || (pathArray.getUrlArray() == null)) {
	    System.out.println("\n removing files, no input. skipping.");
	    return;
	}
	System.out.println("\n removing files. Total #: "+pathArray.getUrlArray().length);
	try {
	    SrmRmRequest r = new SrmRmRequest();
	    
	    r.setArrayOfSURLs(pathArray);	    
	    r.setStorageSystemInfo(TDeviceAccessInfo.gCreateStorageSystemInfo("rm()", TSRMUtil.getDefaultProxyStr()));
	    SrmRmResponse result = srm.srmRm(r);
	    
	    if (result != null) {
		System.out.println("srmRm()    status :"+result.getReturnStatus().getStatusCode());
		System.out.println("           exp    :"+result.getReturnStatus().getExplanation());
		
		ArrayOfTSURLReturnStatus arrayFileStatusObj = result.getArrayOfFileStatuses();
		printTSURLReturnStatusArray(arrayFileStatusObj);	
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
     public static void rmdir(ISRM srm, TSURLInfo path, Boolean beRecursive) {
	System.out.println("\nremove dir, path="+path.getSURL().toString()+ " is recursive?"+beRecursive.booleanValue());
	
	try {
	    SrmRmdirRequest r = new SrmRmdirRequest();
	    r.setSURL(path.getSURL());
	    r.setRecursive(beRecursive);
	    r.setStorageSystemInfo(path.getStorageSystemInfo());
	   
	    SrmRmdirResponse result = srm.srmRmdir(r);
	    
	    if (result != null) {
		System.out.println("srmRmdir() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("           exp    :"+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    

    public static void mv(ISRM srm, TSURLInfo fromPath, TSURLInfo toPath) {
	System.out.println("\nMove, from:"+fromPath.getSURL().toString());
	System.out.println("        to:"+toPath.getSURL().toString());
	
	try {
	    SrmMvRequest r = new SrmMvRequest();
	    r.setFromSURL(fromPath.getSURL());
	    r.setToSURL(toPath.getSURL());
	    r.setStorageSystemInfo(fromPath.getStorageSystemInfo());

	    SrmMvResponse result = srm.srmMv(r);
	    
	    if (result != null) {
		System.out.println("srmMv() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("        exp    :"+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}	    
    }
    
    public static void abortReq(ISRM srm, String req) {
	System.out.println("\n aborting req..."+req);

	try {
	    SrmAbortRequestRequest r = new SrmAbortRequestRequest();
	    r.setRequestToken(req);
	    SrmAbortRequestResponse result = srm.srmAbortRequest(r);

	    if (result != null) {
		System.out.println("srmAbortReq()    status :"+result.getReturnStatus().getStatusCode());
		System.out.println("                 exp    :"+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void purge(ISRM srm, String spaceToken, ArrayOfAnyURI pathArray) {
	if ((pathArray == null) || (pathArray.getUrlArray() == null)) {
	    System.out.println("\n purging files, no input. skipping.");
	    return;
	}

	try {
	    SrmPurgeFromSpaceRequest r = new SrmPurgeFromSpaceRequest();
	    r.setArrayOfSURLs(pathArray);
	    r.setSpaceToken(spaceToken);

	    ArrayOfTExtraInfo ssifo = 
		TDeviceAccessInfo.gCreateStorageSystemInfo("put()", TSRMUtil.getDefaultProxyStr());
	    //r.setStorageSystemInfo(ssinfo);

	    SrmPurgeFromSpaceResponse result = srm.srmPurgeFromSpace(r);
	    if (result != null) {
		System.out.println("srmPurge() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("           exp    :"+result.getReturnStatus().getExplanation());
		
		ArrayOfTSURLReturnStatus arrayFileStatusObj = result.getArrayOfFileStatuses();
		printTSURLReturnStatusArray(arrayFileStatusObj);
	    }
	}  catch (Exception e) {
	    e.printStackTrace();
	}
	
    }

    public static void abortFileArray(ISRM srm, String req, ArrayOfAnyURI pathArray) {
	if ((pathArray == null) || (pathArray.getUrlArray() == null)) {
	    System.out.println("\n aborting files, no input. skipping.");
	    return;
	}
	System.out.println("\n aborting files. Total #: "+pathArray.getUrlArray().length);
	try {
	    SrmAbortFilesRequest r = new SrmAbortFilesRequest();
	    
	    r.setArrayOfSURLs(pathArray);
	    r.setRequestToken(req);
	    
	    SrmAbortFilesResponse result = srm.srmAbortFiles(r);
	    
	    if (result != null) {
		System.out.println("srmAbortFiles()    status :"+result.getReturnStatus().getStatusCode());
		System.out.println("                   exp    :"+result.getReturnStatus().getExplanation());
		
		ArrayOfTSURLReturnStatus arrayFileStatusObj = result.getArrayOfFileStatuses();
		printTSURLReturnStatusArray(arrayFileStatusObj);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static void getReqSummary(ISRM srm, ArrayOfString tokenArray) {
	System.out.println("\n getReqSummary..");
	
	try {
	    SrmGetRequestSummaryRequest r = new SrmGetRequestSummaryRequest();
	    r.setArrayOfRequestTokens(tokenArray);

	    SrmGetRequestSummaryResponse result = srm.srmGetRequestSummary(r);
	    if (result != null) {
		System.out.println("srmGetRequestSummary() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("                       exp    :"+result.getReturnStatus().getExplanation());

		ArrayOfTRequestSummary sumArray = result.getArrayOfRequestSummaries();

		if (sumArray != null) {
		    for (int i=0; i<sumArray.getSummaryArray().length; i++) {
			 TRequestSummary curr = (sumArray.getSummaryArray())[i];
			 System.out.println("\t\t"+i+"th, rid="+curr.getRequestToken()+" type="+curr.getRequestType());
			 System.out.println("\t\t\t"+"status:"+curr.getStatus().getStatusCode());
			 System.out.println("\t\t\t"+"num of finished:"+curr.getNumOfCompletedFiles());
			 System.out.println("\t\t\t"+"num of queued  :"+curr.getNumOfWaitingFiles());
			 System.out.println("\t\t\t"+"       total   :"+curr.getTotalNumFilesInRequest());
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public  static void extFileLTInSpace(ISRM srm,  String token, URI surl, Integer lt) {
	System.out.println("\n extendFileLifeTimeInSpace().."+token);
	if (surl != null) {
	    System.out.println("surl="+surl.toString());
	}
	
	try {
	    SrmExtendFileLifeTimeInSpaceRequest r = new SrmExtendFileLifeTimeInSpaceRequest();
	    r.setSpaceToken(token);
	    if (surl != null) {
		r.setArrayOfSURLs(TSRMUtil.convertToArray(surl));
	    }

	    r.setNewLifeTime(lt);

	    SrmExtendFileLifeTimeInSpaceResponse result = srm.srmExtendFileLifeTimeInSpace(r);

	    if (result != null) {
		System.out.println("srmExtendFileLifeTimeInSpace() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("                               exp    :"+result.getReturnStatus().getExplanation());

		System.out.println("  file surl: "+result.getArrayOfFileStatuses().getStatusArray(0).getSurl());
		System.out.println("  file status: "+result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getStatusCode());
		System.out.println("               "+result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getExplanation());
		Integer lifetime = result.getArrayOfFileStatuses().getStatusArray(0).getFileLifetime();
		if (lifetime != null) {
		    System.out.println("new file lifetime="+lifetime.intValue());
		}
		lifetime = result.getArrayOfFileStatuses().getStatusArray(0).getPinLifetime();
		if (lifetime != null) {
		    System.out.println("new turl lifetime="+lifetime.intValue());
		}
	     }
	}  catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void extFileLT(ISRM srm,  String rid, URI surl, Integer lt) {
	System.out.println("\n extendFileLifeTime().. rid="+rid+" surl="+surl);
	
	try {
	    SrmExtendFileLifeTimeRequest r = new SrmExtendFileLifeTimeRequest();
	    r.setRequestToken(rid);
	    r.setArrayOfSURLs(TSRMUtil.convertToArray(surl));

	    r.setNewFileLifeTime(lt);
	    r.setNewPinLifeTime(lt);

	    SrmExtendFileLifeTimeResponse result = srm.srmExtendFileLifeTime(r);

	    if (result != null) {
		System.out.println("srmExtendFileLifeTime() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("                        exp    :"+result.getReturnStatus().getExplanation());

		System.out.println("  file surl  : "+result.getArrayOfFileStatuses().getStatusArray(0).getSurl());
		System.out.println("  file status: "+result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getStatusCode());
		System.out.println("               "+result.getArrayOfFileStatuses().getStatusArray(0).getStatus().getExplanation());
		Integer lifetime = result.getArrayOfFileStatuses().getStatusArray(0).getFileLifetime();
		if (lifetime != null) {
		    System.out.println("new file lifetime="+lifetime.intValue());
		}
		lifetime = result.getArrayOfFileStatuses().getStatusArray(0).getPinLifetime();
		if (lifetime != null) {
		    System.out.println("new turl lifetime="+lifetime.intValue());
		}
	     }
	}  catch (Exception e) {
	    e.printStackTrace();
	}

    }

    public static void changeSpace(ISRM srm, String token, Vector src) {
	System.out.println("\n changeSpace("+token+") "+src.size());

	try {
	    SrmChangeSpaceForFilesRequest r = new SrmChangeSpaceForFilesRequest();
	    r.setTargetSpaceToken(token);
	    URI[] srcArray = new URI[src.size()];
	    for (int i=0; i<srcArray.length; i++) {
		srcArray[i] = new URI((String)src.get(i));
	    }
	    r.setArrayOfSURLs(TSRMUtil.convertToArray(srcArray));
	    ArrayOfTExtraInfo ssinfo = 
		TDeviceAccessInfo.gCreateStorageSystemInfo("put()", TSRMUtil.getDefaultProxyStr());
	    r.setStorageSystemInfo(ssinfo);

	    SrmChangeSpaceForFilesResponse result = srm.srmChangeSpaceForFiles(r);
	    
	    System.out.println("........... changeSpaceForFiles result="+result.getReturnStatus().getStatusCode());
	    System.out.println("...........                     exp   ="+result.getReturnStatus().getExplanation());
	    
	    if (result.getRequestToken() != null) {
		System.out.println(".... req token assigned: "+result.getRequestToken());
	    }
	    
	    if (result.getArrayOfFileStatuses() != null) {
		TSURLReturnStatus[] statusArray = result.getArrayOfFileStatuses().getStatusArray();
		for (int i=0; i<statusArray.length; i++) {
		    TSURLReturnStatus curr=  statusArray[i];
		    System.out.println("  ===> url   : "+curr.getSurl().toString());
		    System.out.println("  status code: "+curr.getStatus().getStatusCode());
		    System.out.println("  status exp : "+curr.getStatus().getExplanation());
		}

		checkChangeSpaceForFileStatus(srm, result.getRequestToken());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

 public static void checkChangeSpaceForFileStatus(ISRM srm, String token) 
	throws URI.MalformedURIException, java.rmi.RemoteException 
    {
	System.out.println("==> checking ChangeSpaceForFile status of request"+token);
	if (token == null) {
	    return;
	}
	
	SrmStatusOfChangeSpaceForFilesRequestRequest r = new SrmStatusOfChangeSpaceForFilesRequestRequest();	
	
	r.setRequestToken(token);
	
	SrmStatusOfChangeSpaceForFilesRequestResponse result = srm.srmStatusOfChangeSpaceForFilesRequest(r);       
	
	if (result.getReturnStatus().getStatusCode() != TStatusCode.SRM_SUCCESS) 
	{
	    System.out.println("req status is:"+result.getReturnStatus().getStatusCode());
	    System.out.println("          exp:"+result.getReturnStatus().getExplanation());
	}
	
	if (result.getArrayOfFileStatuses() == null) {
	    System.out.println("No contents in return status.");
	    return;
	} 

	TSURLReturnStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
	
	
	System.out.println("       url status="+fileStatus.getStatus().getStatusCode());
	System.out.println("              exp="+fileStatus.getStatus().getExplanation());
	
	return;
	
    }

    public static void getRID(ISRM srm, String userDesc) {
	System.out.println("\n getRID()..");
	
	try {
	    SrmGetRequestTokensRequest r = new SrmGetRequestTokensRequest();
	    r.setUserRequestDescription(userDesc);
	    
	    SrmGetRequestTokensResponse result = srm.srmGetRequestTokens(r);
	    if (result != null) {
		System.out.println("srmRID() status :"+result.getReturnStatus().getStatusCode());
		System.out.println("         exp    :"+result.getReturnStatus().getExplanation());

		ArrayOfTRequestTokenReturn tokens = result.getArrayOfRequestTokens();

		if (tokens != null) {
		    for (int i=0; i<tokens.getTokenArray().length; i++) {
			TRequestTokenReturn curr = (tokens.getTokenArray())[i];
			System.out.println("                "+i+"th: "+curr.getRequestToken());
		    }
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static boolean testMkdir(ISRM srm, InputInfo info, Vector srcList) {	      
	TSURLInfo[] path = new TSURLInfo[srcList.size()];
	
	try {
	    for (int i=0; i<srcList.size(); i++) {
		String surl = (String)(srcList.get(i));
		path[i] = createTSURLInfo(surl, info, true);
		if (surl.startsWith("mss")) {
		    ArrayOfTExtraInfo ssinfo = info.getMSSStorageInfo(true);	 
		    path[i].setStorageSystemInfo(ssinfo);
		}
		mkdir(srm, path[i]);
	    }		   
	    return true;	
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    return false;
	}	       	         
    }
    
    public static boolean testRmdir(ISRM srm, InputInfo info, Vector srcList) {	      
	
	TSURLInfo path = null;
	
	try {
	    int i=1;
	    while (i <= srcList.size()) {
		String surl = (String)(srcList.get(i-1));
		path  = createTSURLInfo(surl, info, true);
		path.setStorageSystemInfo(info.getMSSStorageInfo(true));
		Boolean beRecursive = Boolean.valueOf((String)(srcList.get(i)));
		rmdir(srm, path, beRecursive);
		i = i+2;
	    }		   
	    return true;	
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    return false;
	}	       	            
    }
    
    public static boolean testRmfile(ISRM srm, InputInfo info, Vector srcList) {	      
	
	URI[] pathArray = new URI[srcList.size()];
	

	    int i=0;
	    while (i < srcList.size()) {
		String surl = (String)(srcList.get(i));
		pathArray[i]  =  TSRMUtil.createTSURL(surl);
		i++;
	    }		   
	    rmFileArray(srm,TSRMUtil.convertToArray(pathArray));
	    return true;	
    }
    
    public static boolean testMv(ISRM srm, InputInfo info, Vector srcList) {	      
	
	TSURLInfo fromPath = null;
	TSURLInfo toPath = null;
	
	try {
	    int i=1;
	    while (i <= srcList.size()) {
		String surl = (String)(srcList.get(i-1));
		fromPath  = createTSURLInfo(surl, info, true);
		String turl = (String)(srcList.get(i));
		toPath  = createTSURLInfo(turl, info, false);
		mv(srm, fromPath, toPath);
		i = i+2;
	    }		   
	    return true;	
	} catch (org.apache.axis.types.URI.MalformedURIException e) {
	    return false;
	}	       	            
    }

    public static boolean testPurge(ISRM srm, InputInfo info, Vector srcList) {	      
	if (srcList.size() < 2) {
	    System.out.println("Need to provide token then file surls for purge()");
	    return false;
	}
	
	String token = (String)(srcList.get(0));
	URI[] pathArray = new URI[srcList.size()-1];

	int i=1; 
	while (i < srcList.size()) {
	    String surl = (String)(srcList.get(i));
	    pathArray[i-1]  = TSRMUtil.createTSURL(surl);
	    i++;
	}

	System.out.println("...purge() from space="+token+", # of files: "+pathArray.length);
	purge(srm, token, TSRMUtil.convertToArray(pathArray));

	return true;
    }
    
    public static boolean testAbortFiles(ISRM srm, InputInfo info, Vector srcList) {	      
	   
	URI[] pathArray = new URI[srcList.size()];
	
	    if (srcList.size() == 1) {
		System.out.println("Need to provide file surls for abortFiles()");
		return false;
	    }
	    String token = (String)(srcList.get(0));
	    
	    int i=1;
	    while (i < srcList.size()) {
		String surl = (String)(srcList.get(i));
		pathArray[i]  = TSRMUtil.createTSURL(surl);
		i++;
	    }		   
	    abortFileArray(srm,token, TSRMUtil.convertToArray(pathArray));
	    return true;			 	            
    }
    
    public static boolean testAbortReq(ISRM srm, InputInfo info, Vector srcList) {	      
	
	try {
	    int i=0;
	    while (i < srcList.size()) {
		String token = (String)(srcList.get(i));
		abortReq(srm, token);
		i++;
	    }		   
	    
	    return true;	
	} catch (Exception e) {
	    return false;
	}	       	            
    }
    
    public static boolean testReqSummary(ISRM srm, InputInfo info, Vector srcList) {	      
	String[] tokens = new String[srcList.size()];
	
	try {
	    int i=0;
	    while (i < srcList.size()) {
		String rid = (String)(srcList.get(i));
		tokens[i] = rid;
		i++;
	    }		   
	    getReqSummary(srm, TSRMUtil.convertToArray(tokens));
	    return true;	
	} catch (Exception e) {
	    return false;
	}	       	     	          
    }
    
    public static boolean testExtFileLT(ISRM srm, InputInfo info, Vector srcList) {	      
	Integer ltObj = null;
	URI surl = null;

	    int i=0;
	    while (i < srcList.size()) {
		String urlStr = (String)(srcList.get(i));
		surl = TSRMUtil.createTSURL(urlStr);
		String rid = (String)(srcList.get(i+1));
		if (rid.equalsIgnoreCase("null")) {
		    rid = null;
		}
		String token = rid;

		int lt = Integer.parseInt((String)(srcList.get(i+2)));
		ltObj = TSRMUtil.createTLifeTimeInSeconds(lt, false);
		extFileLT(srm, token, surl, ltObj);
		i = i+3;
	    }		   
		
	    return true;	
	
    }

    public static boolean testChangeSpace(ISRM srm, InputInfo info, Vector srcList) {	      
	try {
	    //String  token = (String)(srcList.get(0));
	    String token = reserveSpace(srm, info).getSpaceToken();
	    System.out.println("Will use token: "+token);

	    if (srcList.size() == 0) {
		System.out.println("No src provided, exit");
		return false;
	    }
	    changeSpace(srm, token, srcList);
	    return true;	
	} catch (Exception e) {
	    return false;
	}
    }	   		
    
    public static boolean testGetRID(ISRM srm, InputInfo info, Vector srcList) {	      
	try {
	    int i=0;
	    while (i < srcList.size()) {
		String userDes = (String)(srcList.get(i));
		if (userDes.equalsIgnoreCase("null")) {
		    getRID(srm, null);
		} else {
		    getRID(srm, userDes);
		}
		i++;
	    }		   
		
	    return true;	
	} catch (Exception e) {
	    return false;
	}
    }	   
	

    public static boolean testls(ISRM srm, InputInfo info, Vector srcList) {
	
	URI[] path = new URI[srcList.size()];
	
	    for (int i=0; i<srcList.size(); i++) {
		String surl = (String)(srcList.get(i));
		path[i] = TSRMUtil.createTSURL(surl);
	    }
	    
	    list(srm, path, info);
	    return true;	

    }
    
    public static boolean testCopy(ISRM srm, InputInfo info, Vector srcList) {
	String fileToken = getToken(srm, info);
	System.out.println("Size  ?? "+srcList.size());
	
	if (srcList.size() == 2) {
	    String src = (String)(srcList.get(0));
	    String tgt = (String)(srcList.get(1));
		
	    prepareToCopyOneFile(srm, src, tgt, fileToken, info);
	} else {	
	    prepareToCopyMultiFiles(srm, srcList, fileToken, info);
	}
	
	return true;
    }
  
  public static URL getURL(String url) {
    org.apache.axis.types.URI uri = null;
    try {
       uri = new org.apache.axis.types.URI(url);
    }  catch (Exception e) {
      TSRMLog.info(SRMClient.class, null, "event=invalidAxisURI", "input="+url);
      return null;
    }
    try {
       if (uri.getScheme().equalsIgnoreCase("httpg")) {
	   org.globus.net.protocol.httpg.Handler m = new org.globus.net.protocol.httpg.Handler();
	   return new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), m);
       } else if (uri.getScheme().equalsIgnoreCase("https")) {
	   org.globus.net.protocol.https.Handler m = new org.globus.net.protocol.https.Handler();
	   return new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), m);
       } else {
	   return new URL(url);
       }
    } catch (Exception e) {
       TSRMLog.info(SRMClient.class, null, "event=invalidURL", "input="+url);
       return null;
    }
  }
    
	public static ISRM getISRMHandle(String url) {
	  SimpleProvider provider = new SimpleProvider();
	  SimpleTargetedChain c = null;

	  if (url.startsWith("httpg")) {
	      String result =org.theshoemakers.which4j.Which4J.which(org.globus.axis.transport.GSIHTTPSender.class);            
	      System.out.println(".......result="+result);
	      c = new SimpleTargetedChain(new GSIHTTPSender());
	      provider.deployTransport("httpg",c);
	  } else if (url.startsWith("https")) {
	      TSRMLog.debug(SRMClient.class, null, "event=getHttpsHandle", "url="+url);
              c = new SimpleTargetedChain(new HTTPSSender());
              provider.deployTransport("https",c);
	  } else {
	      //System.out.println(".........always supports: http");
	      TSRMLog.debug(SRMClient.class, null, "event=getHttpHandle", "url="+url);
	      c = new SimpleTargetedChain(new HTTPSender());
	      provider.deployTransport("http",c);
	  }

	  Util.registerTransport();
	  TSRMLog.debug(SRMClient.class, null, "registered", null);

	  SRMServiceLocator service = new SRMServiceLocator(provider);
	  TSRMLog.debug(SRMClient.class, null, "located", null);
	  URL uu = getURL(url);
	  if (uu == null) {
	    return null;
	  }
	  try {
	    // Now use the service to get a stub to the service
	    TSRMLog.info(SRMClient.class, null, "event=contacting", "url="+url);
	    ISRM srm = service.getsrm(uu);
	    ((org.apache.axis.client.Stub)srm)._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
							    org.globus.gsi.gssapi.auth.NoAuthorization.getInstance());

	    ((org.apache.axis.client.Stub)srm)._setProperty(org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
							    org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);

	    ((org.apache.axis.client.Stub)srm)._setProperty(org.globus.gsi.GSIConstants.AUTHZ_REQUIRED_WITH_DELEGATION, Boolean.FALSE);
	    return srm;
	  } catch (Exception e) {
	    e.printStackTrace();
	    return null;
	  } finally {
	    TSRMLog.info(SRMClient.class, null, "event=contacted", "url="+url);
	  }
	}
  
  public static GSSCredential getCredential() {
    GSSCredential c = null;
    String proxyFileName = "/tmp/x509up_u18263";
    try {	
      File inFile = new File(proxyFileName);
			byte [] data = new byte[(int)inFile.length()];
			FileInputStream inStream = new FileInputStream(inFile);
			inStream.read(data);
			inStream.close();
			ExtendedGSSManager manager =
			  (ExtendedGSSManager)ExtendedGSSManager.getInstance();
			c = manager.createCredential(data,
						     ExtendedGSSCredential.IMPEXP_OPAQUE,
						     GSSCredential.DEFAULT_LIFETIME,
						     null, // use default mechanism - GSI
						     GSSCredential.INITIATE_AND_ACCEPT);
    } catch (Exception e) { e.printStackTrace(); }
    
    return c;

	}
    public static void test(String[] args) {
	//ServiceContext context = new ServiceContext ();
	try {
/*
	    GetOpts opts = 
		new GetOpts
		("Usage: SRMClient server_url [-uid=[uid]] [-getToken=yes] [-file=[filename]]\nnote:\n\tif you want to reserve space first, then specify -getToken=yes[V:seconds:MB]\n\tif -file is specified, filenames should be like: get.* put.* copy.*, e.g. get.txt", 1);
	    
	    String error = opts.parse(args);
	    if (error != null) {
		System.err.println(error);
		return;
	    }
*/	    
	    //
	    // checking if httpg is handled..if not java.net.URL returns exception on httpg.
	    //
	    //String protocols = System.getProperty("java.protocol.handler.pkgs");
	    //System.out.println(protocols);
	    //
	    
	    //
	    // Must initializing a SRMServiceGridlocator object before new URL("httpg://.."),
	    // so the correct handler pkg for httpg, which is 
	    // (org.globus.net.protocol), is given to System class.
	    //
	    // initialize ISRM here:
	    System.out.println("########## connecting to:"+args[0]);
		ISRM srm = getISRMHandle(args[0]);

		System.out.println("srm stub=" + srm); 

	if (srm == null) {
             System.exit(1);
	}

	if (args[0].startsWith("httpg")) {
     if(srm instanceof org.apache.axis.client.Stub) { 
		System.out.println("......Checking proxy");
		GSSCredential proxyCred = getCredential();
          org.apache.axis.client.Stub srm_stub = 
                                        (org.apache.axis.client.Stub)srm;
                srm_stub._setProperty(GSIConstants.GSI_CREDENTIALS,proxyCred);
                srm_stub._setProperty(GSIConstants.GSI_AUTHORIZATION,
                              HostAuthorization.getInstance());
                srm_stub._setProperty(GSIConstants.GSI_MODE,
                                 GSIConstants.GSI_MODE_FULL_DELEG);
                //
		}    
	}
	    /*
	 ((org.apache.axis.client.Stub)srm).setTimeout(10000);
		if (args.length > 1) {
	    boolean doDelegation = args[1].equals("delegate");
	    System.out.println("DELEGATION enabled? "+doDelegation);
	    setSecurityProperties((Stub)srm, url, doDelegation);
		} else {
			System.out.println("no delegation is required.");
		}
	      */ 
if (!testPing(srm)) {
    System.exit(1);
}

testTxfProtocols(srm);

	    InputInfo info = new InputInfo(args);
	    info.print();
	    
	    if (info.doSequenceTest(srm)) {
		return;
	    }
	    /*
	    try {
		URI src = new URI("srm://dmx09.lbl.gov:8442/srm/V2/Server?SFN=/asim/test.data");
		URI tgt = new URI("srm://dmx02.lbl.gov:8444/srm/V2/Server?=/asim/test.data");

		checkCopyStatus(srm, src, tgt, "asim:23(COPY)");
	    } catch (Exception e) {
		e.printStackTrace();
	    }		
	    */	    
	    Vector srcList = info.fileForRequest("get");
	    
	    if (srcList != null) {
		testGet(srm, info, srcList);
		return;
	    } 
	    srcList = info.fileForRequest("put");
	    
	    if (srcList != null) {
		testPut(srm, info, srcList);
		return;
	    } 
	    
	    srcList = info.fileForRequest("copy");
	    
	    if (srcList != null) {
		testCopy(srm, info, srcList);
		return;
	    } 
	    

	    srcList = info.fileForRequest("bringOnline");
	    
	    if (srcList != null) {
		testBringOnline(srm, info, srcList);
		return;
	    } 

	    srcList = info.fileForRequest("ls");
	    if (srcList != null) {
		testls(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("mkdir");
	    if (srcList != null) {
		testMkdir(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("rmdir");
	    if (srcList != null) {
		testRmdir(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("rmFile");
	    if (srcList != null) {
		testRmfile(srm, info, srcList);
		return;
	    }

	    srcList = info.fileForRequest("purge");
	    if (srcList != null) {
		testPurge(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("mv");
	    if (srcList != null) {
		testMv(srm, info, srcList);
		return;
	    } 
	    
	    srcList = info.fileForRequest("reqSummary");
	    if (srcList != null) {
		testReqSummary(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("getrid");
	    if (srcList != null) {
		testGetRID(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("extFileLT");
	    if (srcList != null) {
		testExtFileLT(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("abortFiles");
	    if (srcList != null) {
		testAbortFiles(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("abortReq");
	    if (srcList != null) {
		testAbortReq(srm, info, srcList);
		return;
	    }
	    
	    srcList = info.fileForRequest("changeSpace");

	    if (srcList != null) {
		testChangeSpace(srm, info, srcList);
		return;
	    } else {
		System.out.println("Oh no");
	    }
	    
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static void printSpaceDetail(TMetaDataSpace detail) {
	if (detail.getStatus() != null) {
	    System.out.println("      toke status: "+detail.getStatus().getStatusCode());
	    System.out.println("      toke exp: "+detail.getStatus().getExplanation());
	}
	if (detail.getRetentionPolicyInfo() != null) {
	    System.out.println("      type:"+detail.getRetentionPolicyInfo().getRetentionPolicy());
	    System.out.println("          :"+detail.getRetentionPolicyInfo().getAccessLatency());
	}
	
	if (detail.getTotalSize() != null) {
	    System.out.println("      total bytes     ="+detail.getTotalSize().longValue());
	}
	if (detail.getGuaranteedSize() != null){
	    System.out.println("      guaranteed bytes="+detail.getGuaranteedSize().longValue());
	}
	if (detail.getUnusedSize() != null) {
	    System.out.println("      unused bytes    ="+detail.getUnusedSize().longValue());
	}
	if (detail.getLifetimeAssigned() != null) {
	    System.out.println("      LifeTime assigned ="+detail.getLifetimeAssigned().intValue());
	}
	if (detail.getLifetimeLeft() != null) {
	    System.out.println("      LifeTime left ="+detail.getLifetimeLeft().intValue());
	}
    }

    public static void getSpaceTokenMeta(ISRM srm, String token) {
	SrmGetSpaceMetaDataRequest req = new SrmGetSpaceMetaDataRequest();
	
	String[] tokenArray = new String[1];
	tokenArray[0] = token;
	
	req.setArrayOfSpaceTokens(TSRMUtil.convertToArray(tokenArray));
	
	try {
	    SrmGetSpaceMetaDataResponse result = srm.srmGetSpaceMetaData(req);
	    if (result.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS) {
		System.out.println("##### detail of space token:"+token);
		TMetaDataSpace detail = result.getArrayOfSpaceDetails().getSpaceDataArray(0);
		//System.out.println("      is valid? = "+detail.isIsValid());
		printSpaceDetail(detail);		
	    } else {
		if (token != null) {
		    System.out.println("<<error>> with getSpaceMetaData("+token+"), code="+result.getReturnStatus().getStatusCode());
		    if (result.getArrayOfSpaceDetails() != null) {
			TMetaDataSpace detail = result.getArrayOfSpaceDetails().getSpaceDataArray(0);
			printSpaceDetail(detail);
		    }
		} else {
		    System.out.println("<<error>> with getSpaceMetaData(null), code="+result.getReturnStatus().getStatusCode());
		}
		System.out.println("         exp="+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
	
    public static void releaseFile(ISRM srm, boolean doRemove, URI surl, String rid) {
	System.out.println();
	System.out.println("--------------- Releasing files for request:"+rid+"  \nurl=  "+surl);
	System.out.println("---------------     doRemove?" +doRemove);
	
	SrmReleaseFilesRequest req = new SrmReleaseFilesRequest();
	req.setDoRemove(new Boolean(doRemove));
	//req.setRequestToken(rid);
	req.setArrayOfSURLs(TSRMUtil.convertToArray(surl));
	
	try { 
	    SrmReleaseFilesResponse result = srm.srmReleaseFiles(req);
	    if (result != null) {
		System.out.println("I got file release status :"+result.getReturnStatus().getStatusCode());
		System.out.println("         explanation :"+result.getReturnStatus().getExplanation());

		TSURLReturnStatus[] array = result.getArrayOfFileStatuses().getStatusArray();
		for (int i=0; i< array.length; i++) {
		    System.out.println("         detail: surl:"+array[i].getSurl());
		    System.out.println("         detail: code:"+array[i].getStatus().getStatusCode());
		    System.out.println("         detail: code:"+array[i].getStatus().getExplanation());
		}

		System.out.println();

	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static void releaseSpace(ISRM srm, String token, boolean forceFileRelease) {
	
	if (token != null) {
	    System.out.println("<<--------------- Releasing space:"+token);
	} else {
	    System.out.println("--------------- Cannt release space with null token");
	    //return;
	}
	SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
	req.setSpaceToken(token);
	req.setForceFileRelease(new Boolean(forceFileRelease));
	try {
	    SrmReleaseSpaceResponse result = srm.srmReleaseSpace(req);
	    
	    if (result != null) {
		System.out.println("I got space release status :"+result.getReturnStatus().getStatusCode());
		System.out.println("         explanation :"+result.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    static void printPutFileStatus(TPutRequestFileStatus fileStatus) {
	 System.out.println("  "+fileStatus);
	 System.out.println(" put file status="+fileStatus.getStatus().getStatusCode());
	 System.out.println("             exp="+fileStatus.getStatus().getExplanation());
	 System.out.println("            surl="+fileStatus.getSURL().toString());
	 if (fileStatus.getTransferURL() != null) {
	     System.out.println("            turl="+fileStatus.getTransferURL().toString());	 
	 }
	 if (fileStatus.getRemainingPinLifetime() != null) {
	     System.out.println("            turl lifetime="+fileStatus.getRemainingPinLifetime().intValue());
	 }
	 if (fileStatus.getRemainingFileLifetime() != null) {
	     System.out.println("            surl lifetime="+fileStatus.getRemainingFileLifetime().intValue());
	 }
	 
    }

    static boolean uploadFile(String src, TPutRequestFileStatus putFileStatus) {	
	try {
	    Thread.sleep(80000);
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return true;
	/*
	try {
	    System.out.println("## transfer a file from: "+src);
	    java.io.File from = new File(src);
	    if (from.exists()) {
		URI turi = putFileStatus.getTransferURL();
		System.out.println("coping to=>"+turi.getPath());
		File to = new File(turi.getPath());
		TDiskDevice.copy(from, to);
		if (to.exists()) {
		    return true;
		}
	    } else {
		System.out.println("!!!Sample file didnt exist! Cannt copy to the put location");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return false;	
	*/
    }
    
    public static void prepareToPutMultiFiles(ISRM srm, String[] array, String token, InputInfo info) {
	if (token != null) {
	    getSpaceTokenMeta(srm,token);
	}
	
	int total = array.length;
	System.out.println("prepareToPut multiple files, size="+total);
	
	SrmPrepareToPutRequest r = new SrmPrepareToPutRequest();
	
	try {					
	    TPutFileRequest[] fileReqList = new TPutFileRequest[total];
	    for (int i=0; i<total; i++) {
		fileReqList[i] = new TPutFileRequest();
		String toSurl = array[i];
		URI uri = TSRMUtil.createTSURL(toSurl);

		fileReqList[i].setTargetSURL(uri);
		fileReqList[i].setExpectedFileSize(new org.apache.axis.types.UnsignedLong(100));
		//fileReqList[i].setSpaceToken(token);
	    }

	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    //r.setOverwriteOption(TOverwriteMode.ALWAYS);
	    r.setOverwriteOption(info._overwriteMode);
	    System.out.println(".. overwrite mode="+info._overwriteMode);

	    if (info._tokenInfo != null) {
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }
	    r.setTargetSpaceToken(token);
	    //ArrayOfTExtraInfo ssinfo = TDeviceAccessInfo.gCreateStorageSystemInfo("put()", TSRMUtil.getDefaultProxyStr());
	    ArrayOfTExtraInfo ssinfo = info.getMSSStorageInfo(false);
	    r.setStorageSystemInfo(ssinfo);

	    System.out.println("... txf lifetime="+info._nTxfLT+"  surl lifetime="+info._nSurlLT);
	    r.setDesiredPinLifeTime(new Integer(info._nTxfLT));
	    r.setDesiredFileLifeTime(new Integer(info._nSurlLT));
	    //r.setDesiredFileStorageType(TFileStorageType.PERMANENT);
r.setTransferParameters(TSRMUtil.createDefaultTxfParameters());
	    SrmPrepareToPutResponse result = srm.srmPrepareToPut(r);
			
	    if (result != null) {
		System.out.println("I got put status :"+result.getReturnStatus().getStatusCode());
		System.out.println("     explanation :"+result.getReturnStatus().getExplanation());
		System.out.println("     request id  ="+result.getRequestToken());

		if (result.getArrayOfFileStatuses() == null) {
		    return;
		}
		HashMap statusArray = new HashMap();

		TPutRequestFileStatus[] arrayOfResult = result.getArrayOfFileStatuses().getStatusArray();
		for (int i=0; i<arrayOfResult.length; i++) {
		    TPutRequestFileStatus fileStatus = arrayOfResult[i];
		    System.out.println("=> "+i+"th:");
		    printPutFileStatus(fileStatus);
		    statusArray.put(fileStatus.getSURL(), fileStatus);
		}

		// try to call putStatus periodically
		
		int which = 0;
		TPutRequestFileStatus putFileStatus = null;
		while (statusArray.size() > 0) {
		    Thread.sleep(1000);
		    if (which >= statusArray.size()) {
			which = 0;
		    }
		    Object key = (statusArray.keySet().toArray()) [which];
		    TPutRequestFileStatus status = (TPutRequestFileStatus)(statusArray.get(key));
		    TStatusCode code = status.getStatus().getStatusCode();		    
		    /*
		    URI hoho = new URI("srm://dmx09.lbl.gov:8444//srm/V2/ServerOld/?SFN=/~");
		    releaseFile(srm, false, (URI)hoho, null);
		    */
		    if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED)) {
			putFileStatus =  checkPutStatus(srm, (URI)key, result.getRequestToken());
			if (putFileStatus == null) {
			    break;
			} else {
			    code = putFileStatus.getStatus().getStatusCode(); 		
			}
		    }
		    if (code == TStatusCode.SRM_SPACE_AVAILABLE) {			
			String from = "/tmp/medium.1";
			if (uploadFile(from, putFileStatus)) {
			    putDone(srm, result.getRequestToken(), status.getSURL());
			    statusArray.remove(key);
			}			
		    } else if ((code != TStatusCode.SRM_REQUEST_INPROGRESS) && (code != TStatusCode.SRM_REQUEST_QUEUED)) {
			//extFileLTInSpace(srm, token, (URI)key, null);
			TSRMUtil.sleep(3000);
			extFileLTInSpace(srm, token, null, new Integer(100));
			if (!(code == TStatusCode.SRM_FILE_IN_CACHE)) {
			    statusArray.remove(key);
			    releaseFile(srm, false, status.getSURL(), result.getRequestToken());
			}
		    }
		    which++;
		}	    
	    } else {
		System.out.println("Empty put result.");
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    public static void prepareToPutOneFile(ISRM srm, String toSurl, String token, InputInfo info) {
	if (token != null) {
	    getSpaceTokenMeta(srm,token);
	}
	
	System.out.println("prepareToPut to:"+toSurl);
	
	SrmPrepareToPutRequest r = new SrmPrepareToPutRequest();
	
	try {					
	    TPutFileRequest[] fileReqList = new TPutFileRequest[1];
	    fileReqList[0] = new TPutFileRequest();
	    /*    
		  TSizeInBytes sizeObj = new TSizeInBytes();
		  sizeObj.setValue(101019086);
		  fileReqList[0].setKnownSizeOfThisFile(sizeObj);
	    */
	    if (toSurl == null) {
		fileReqList[0].setTargetSURL(null);
	    } else {
		URI uri = TSRMUtil.createTSURL(toSurl);		
		fileReqList[0].setTargetSURL(uri);
	    }
	    
	    r.setTargetSpaceToken(token);
	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    r.setOverwriteOption(TOverwriteMode.ALWAYS);
	    if (info._tokenInfo != null) {
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }
	  
	    //srm.srmPrepareToPut(r);
	    
	    SrmPrepareToPutResponse result = srm.srmPrepareToPut(r);
			
	    if (result != null) {
		TPutRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
		System.out.println("I got put status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		System.out.println("  request id="+result.getRequestToken());
		
		System.out.println(" put file status="+fileStatus.getStatus().getStatusCode());
		System.out.println("             exp="+fileStatus.getStatus().getExplanation());
		System.out.println("            surl="+fileStatus.getSURL().toString());
		//System.out.println("            turl="+result.getArrayOfFileStatuses()[0].getTransferURL().toString());
		
		//}				
		// try to call getStatus periodically
		//
		TStatusCode code = fileStatus.getStatus().getStatusCode();
		TPutRequestFileStatus putFileStatus = null;
		URI assignedTURL = null;
		while (true) {
		    if ((code == TStatusCode.SRM_REQUEST_QUEUED) || (code == TStatusCode.SRM_REQUEST_INPROGRESS)) 			
		    {				    
			Thread.sleep(10000);
			putFileStatus = checkPutStatus(srm, fileStatus.getSURL(), result.getRequestToken());
			if (putFileStatus == null) {
			    break;
			}
			code = putFileStatus.getStatus().getStatusCode();
		    } else if ((code == TStatusCode.SRM_FAILURE) || 
			       (code == TStatusCode.SRM_INVALID_PATH))
		    {
			break;
		    } else if ((code == TStatusCode.SRM_FILE_PINNED) || (code == TStatusCode.SRM_SUCCESS)) {
			break;
		    } else {
			if (assignedTURL == null) {
			    assignedTURL = putFileStatus.getTransferURL();
			}
			if (code == TStatusCode.SRM_SPACE_AVAILABLE) {
			    String src = "/tmp/medium.1";
			    System.out.println("## transfer a file from: "+src);
			    java.io.File from = new File(src);
			    if (from.exists()) {
				URI turi = putFileStatus.getTransferURL();
				//turi.setScheme("file");
				System.out.println("coping to=>"+turi.getPath());
				File to = new File(turi.getPath());
				TDiskDevice.copy(from, to);
				if (to.exists()) {
				    putDone(srm, result.getRequestToken(), fileStatus.getSURL());	   
				}
			    } else {
				System.out.println("!!!Sample file didnt exist! Cannt copy to the put location");
			    }
			}
			code = TStatusCode.SRM_REQUEST_INPROGRESS;
			//break; 
		    }
		}
		
		if (assignedTURL != null) {
		    getSpaceTokenMeta(srm,token);
		    /*
		    boolean keepSpace = false;
		    
		    releaseFile(srm, keepSpace, fileStatus.getSURL(), result.getRequestToken());
		    
		    getSpaceTokenMeta(srm,token);
		    */
		}
		
	    } else {
		System.out.println("empty result");				 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}		
    }
    
    public static void putDone(ISRM srm, String token, URI  siteURL) {
	
	SrmPutDoneRequest req = new SrmPutDoneRequest();
	req.setArrayOfSURLs(TSRMUtil.convertToArray(siteURL));
	req.setRequestToken(token);

	System.out.println("#### calling putDone: "+token+" "+siteURL);
	try {
	    SrmPutDoneResponse result = srm.srmPutDone(req);
	    
	    System.out.println("#### result of putDdone():"+result.getReturnStatus().getStatusCode());

	    if (result.getArrayOfFileStatuses() != null) {
		TSURLReturnStatus status = result.getArrayOfFileStatuses().getStatusArray(0);
		System.out.println("##        surl="+status.getSurl().toString());
		System.out.println("##        code="+status.getStatus().getStatusCode());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}	


	//getRID(srm, null);
    }
    
    private static TSURLInfo createTSURLInfo(String surl, InputInfo info, boolean isSource) throws URI.MalformedURIException {
	TSURLInfo urlInfo = TSRMUtil.createTSURLInfo(surl);

	if (surl.startsWith("mss")) {
	    ArrayOfTExtraInfo ssinfo = info.getMSSStorageInfo(isSource);	    
	    urlInfo.setStorageSystemInfo(ssinfo);
	}

	if (surl.startsWith("srm")) {
	    int pos = surl.indexOf("mss");
	    if (pos > 0) {
		ArrayOfTExtraInfo ssinfo = info.getMSSStorageInfo(isSource);
		urlInfo.setStorageSystemInfo(ssinfo);
	    } 	  
	}

	return urlInfo;
    }
        
    
    public static void prepareToCopyMultiFiles(ISRM srm, Vector srcList, String token, InputInfo info) {
	System.out.println("prepareToCopy multiple files");
	try {
	    SrmCopyRequest r = new SrmCopyRequest();
	    int size = srcList.size()/2;
	    TCopyFileRequest[] fileReqList = new TCopyFileRequest[size];
	    
	    for (int i=0; i<size; i++) {
		String src = (String)(srcList.get(2*i));
		String tgt = (String)(srcList.get(2*i+1));

		fileReqList[i] = new TCopyFileRequest();
		fileReqList[i].setSourceSURL(TSRMUtil.createTSURL(src));
		fileReqList[i].setTargetSURL(TSRMUtil.createTSURL(tgt));
		
		if (src.endsWith("/")) {
		    TDirOption dirOp = new TDirOption();
		    dirOp.setIsSourceADirectory(true);
		    dirOp.setAllLevelRecursive(new Boolean(true));
		    fileReqList[i].setDirOption(dirOp);
		    System.out.println("....... source:"+src+" is a known to be a dir. All level recursive = true");
		}
	    }
	    
	    r.setTargetSpaceToken(token);
	    r.setOverwriteOption(TOverwriteMode.NEVER);
	    if (info._tokenInfo != null) {
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }

	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    //r.setTargetFileStorageType(TFileStorageType.PERMANENT);
	    SrmCopyResponse result = srm.srmCopy(r);

	    TStatusCode code = result.getReturnStatus().getStatusCode();
	    String exp = result.getReturnStatus().getExplanation();

	    System.out.println("return code="+code+", exp="+exp);

	    if (code == TStatusCode.SRM_REQUEST_QUEUED) {
		System.out.println("Not going to wait till all are finished!");		
	    }
	   
		    
	} catch (Exception e) {
	    e.printStackTrace();
	}
	    
    }

    public static void prepareToCopyOneFile(ISRM srm, String surl, String tgt, String token, InputInfo info) {
	System.out.println("prepareToCopy, from:"+surl);
	System.out.println("prepareToCopy, to:"+tgt);
	try {
	    SrmCopyRequest r = new SrmCopyRequest();
	    TCopyFileRequest[] fileReqList = new TCopyFileRequest[1];
	    
	    fileReqList[0] = new TCopyFileRequest();
	    fileReqList[0].setSourceSURL(TSRMUtil.createTSURL(surl));
	    fileReqList[0].setTargetSURL(TSRMUtil.createTSURL(tgt));

	    r.setTargetSpaceToken(token);
	    //fileReqList[0].setFileStorageType(TFileStorageType.Volatile);
	    r.setOverwriteOption(TOverwriteMode.ALWAYS);

	    if (surl.endsWith("/")) {
		TDirOption dirOp = new TDirOption();
		dirOp.setIsSourceADirectory(true);
		dirOp.setAllLevelRecursive(new Boolean(true));
		fileReqList[0].setDirOption(dirOp);
		System.out.println("....... source:"+surl+" is a known to be a dir. All level recursive = true");
	    }	    

	    int pos = surl.indexOf("garchive.nersc.gov");
	    if (pos >= 0) {
		r.setSourceStorageSystemInfo(info.getMSSStorageInfo(true));
	    } else {
		r.setSourceStorageSystemInfo(info.junk("noAPI"));
	    }

	    pos = tgt.indexOf("garchive.nersc.gov");
	    if (pos >= 0) {
		r.setTargetStorageSystemInfo(info.getMSSStorageInfo(false));
	    } else {
		r.setTargetStorageSystemInfo(info.junk("noAPI"));
	    }
	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    //r.setTargetFileStorageType(TFileStorageType.PERMANENT);	    
	    SrmCopyResponse result = srm.srmCopy(r);
	    
	    if (result != null) {
		System.out.println("I got status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		System.out.println("  request id="+result.getRequestToken());
		
		TCopyRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
		//for (int i=0; i<result.getArrayOfFileStatuses().length; i++) {
		System.out.println(" file status="+fileStatus.getStatus().getStatusCode());
		System.out.println("         exp="+fileStatus.getStatus().getExplanation());
		//}
		
		// try to call getStatus periodically
			TStatusCode code = fileStatus.getStatus().getStatusCode();
		while (true) {
		    if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
			(code == TStatusCode.SRM_REQUEST_QUEUED))
			{
			    Thread.sleep(10000);
			    TCopyRequestFileStatus status = checkCopyStatus(srm, fileReqList[0].getSourceSURL(), 
						   fileReqList[0].getTargetSURL(),
						   result.getRequestToken());
			    if (status != null) {
				code = status.getStatus().getStatusCode();
			    }
			} else {
			    break;
			}
		}
				
		//getSpaceTokenMeta(srm,token);
		
		boolean keepSpace = false;

		//System.out.println("Won't release files");
		/*
		URI[] pathArray = new URI[1];
	        pathArray[0] = fileReqList[0].getTargetSURL();
		rmFileArray(srm, TSRMUtil.convertToArray(pathArray));
		//releaseFile(srm, keepSpace, fileReqList[0].getSourceSURL(), result.getRequestToken());
		//releaseFile(srm, keepSpace, null, result.getRequestToken());
		*/
		
		//getSpaceTokenMeta(srm,token);
		compactToken(srm, token);
		getSpaceTokenMeta(srm,token);			
		
	    } else {
		System.out.println("empty result");				 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static void prepareToGetOneFile(ISRM srm, String surl, String token, InputInfo info) {
	getSpaceTokenMeta(srm,token);
	System.out.println("prepareToGet from:"+surl);
	try {
	    SrmPrepareToGetRequest r = new SrmPrepareToGetRequest();
	    TGetFileRequest[] fileReqList = new TGetFileRequest[1];
	    
	    fileReqList[0] = new TGetFileRequest();
	    
	    URI src = TSRMUtil.createTSURL(surl);
	    fileReqList[0].setSourceSURL(src);

	    r.setTargetSpaceToken(token);
	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));
	    
	    if (info._tokenInfo != null) {
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }

	    SrmPrepareToGetResponse result = srm.srmPrepareToGet(r);
	    
	    if (result != null) {
		System.out.println("I got status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		System.out.println("  request id="+result.getRequestToken());
		
		TGetRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0); 
		//for (int i=0; i<result.getArrayOfFileStatuses().length; i++) {
		System.out.println(" file status="+fileStatus.getStatus().getStatusCode());
		System.out.println("         exp="+fileStatus.getStatus().getExplanation());
		//}
		
		// try to call getStatus periodically
		
		TStatusCode code = fileStatus.getStatus().getStatusCode();
		while (true) {
		    if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) ||
			(code == TStatusCode.SRM_REQUEST_QUEUED))
			{
			    Thread.sleep(10000);
			    code = checkGetStatus(srm,  /*surl*/fileStatus.getSourceSURL(), 
						  result.getRequestToken()).getStatus().getStatusCode();
			} else {			     
			    break;
			}
		}
		
		getSpaceTokenMeta(srm,token);
		
		boolean doRemove = true;
		releaseFile(srm, doRemove, src, result.getRequestToken());
		
		getSpaceTokenMeta(srm,token);
	    } else {
		System.out.println("empty result");				 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public static void prepareToGetMultiFiles(ISRM srm, String[] surls, String token, InputInfo info) {
	System.out.println("prepareToGet from multiple files.... token="+token+"  "+surls.length);
	try {
	    SrmPrepareToGetRequest r = new SrmPrepareToGetRequest();
	    int total = surls.length;
	    
	    TGetFileRequest[] fileReqList = new TGetFileRequest[total];
	    
	    for (int i=0; i<total; i++) {
		TGetFileRequest curr = new TGetFileRequest();
		
		curr.setSourceSURL(TSRMUtil.createTSURL(surls[i]));
		//curr.setSpaceToken(token);

		if (surls[i].endsWith("/")) {
		    TDirOption dirOp = new TDirOption();
		    dirOp.setIsSourceADirectory(true);
		    dirOp.setAllLevelRecursive(new Boolean(true));
		    //dirOp.setNumOfLevels(1);
		    curr.setDirOption(dirOp);
		    System.out.println("....... source:"+surls[i]+" is a known to be a dir. Making it all level recursive");
		}
		
		fileReqList[i] = curr;				 
	    }
	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));

	//ArrayOfTExtraInfo ssinfoArray = TDeviceAccessInfo.gCreateStorageSystemInfo("get()", TSRMUtil.getDefaultProxyStr());
	    TExtraInfo[] ssinfo = new TExtraInfo[5];
	    ssinfo[0] = new TExtraInfo();
	    ssinfo[0].setKey("uid");
	    ssinfo[0].setValue("get()");
	    
	    ssinfo[1] = new TExtraInfo();
	    ssinfo[1].setKey("pwd");
	    ssinfo[1].setValue(TSRMUtil.getDefaultProxyStr());
	    
	    ssinfo[2] = new TExtraInfo();
	    ssinfo[2].setKey("gsiftpStream");
	    ssinfo[2].setValue("1");
	    
	    ssinfo[3] = new TExtraInfo();
	    ssinfo[3].setKey("login");
	    ssinfo[3].setValue("ncarlogin");

	    ssinfo[4] = new TExtraInfo();
	    ssinfo[4].setKey("passwd");
	    ssinfo[4].setValue("ncarpwd");
	    
	    /*
	    ssinfo[5] = new TExtraInfo();
	    ssinfo[5].setKey("projectid");
	    ssinfo[5].setValue("ncarpid");

	    ssinfo[6] = new TExtraInfo();
	    ssinfo[6].setKey("readpasswd");
	    ssinfo[6].setValue("ncar.readpwd");

	    ssinfo[7] = new TExtraInfo();
	    ssinfo[7].setKey("writepasswd");
	    ssinfo[7].setValue("writepasswd");
	    */
	ArrayOfTExtraInfo ssinfoArray = TSRMUtil.convertToArray(ssinfo);
	    r.setTransferParameters(TSRMUtil.createDefaultTxfParameters());
	   
	    r.setStorageSystemInfo(ssinfoArray);	
	    r.setDesiredPinLifeTime(new Integer(30000));
	    if (info._tokenInfo != null) {
		System.out.println(".... token type="+info._tokenInfo._type);
		info._tokenInfo._type.setAccessLatency(TAccessLatency.ONLINE);
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }
	    r.setTargetSpaceToken(token);
	    
	    //r.setDesiredFileStorageType(TFileStorageType.PERMANENT);

	    SrmPrepareToGetResponse result = srm.srmPrepareToGet(r);
	    
	    if (result != null) {
		System.out.println("I got status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		System.out.println("  request id="+result.getRequestToken());
		
		if (result.getArrayOfFileStatuses() == null) {
		    return;
		}
		HashMap statusArray = new HashMap();
		for (int i=0; i<result.getArrayOfFileStatuses().getStatusArray().length; i++) {
		    TGetRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(i);
		    System.out.println(i+"th file status="+fileStatus.getStatus().getStatusCode());
		    if (fileStatus.getStatus().getExplanation() != null) {
			System.out.println("            exp="+fileStatus.getStatus().getExplanation());
		    }
		    if (fileStatus.getTransferURL() != null) {
			System.out.println("        turl:"+fileStatus.getTransferURL());
		    }
		    if (fileStatus.getFileSize() != null) {
			System.out.println("        size:"+fileStatus.getFileSize().longValue());
		    }
		    URI surlInfo = r.getArrayOfFileRequests().getRequestArray()[i].getSourceSURL();
		    statusArray.put(surlInfo, fileStatus);
		    //statusArray.put(fileStatus.getFromSURLInfo(), fileStatus);
		}
		
		int which = 0;
		while (statusArray.size() > 0) {
		    Thread.sleep(1500);				    
		    if (which >= statusArray.size()) {
			which = 0;
		    }
		    Object key = (statusArray.keySet().toArray()) [which];
		    TGetRequestFileStatus status = (TGetRequestFileStatus)(statusArray.get(key));
		    TStatusCode code = status.getStatus().getStatusCode();
		    if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED) || 
			(code == TStatusCode.SRM_FILE_IN_CACHE)) 
		    {
			TGetRequestFileStatus s =  checkGetStatus(srm, (URI)key, result.getRequestToken());

			if (s == null) {
			    break;
			} else {
			    code = s.getStatus().getStatusCode(); 		
			}
		    }
		    if ((code != TStatusCode.SRM_REQUEST_INPROGRESS) 
			&& (code != TStatusCode.SRM_REQUEST_QUEUED) 
			&& (code != TStatusCode.SRM_FILE_IN_CACHE)) 
		    {
			statusArray.remove(key);
			//bringOnlineMultiFiles(srm, surls, token, info);
			//releaseFile(srm, true, (URI)key, null/*result.getRequestToken()*/);
			checkGetStatus(srm, (URI)key, result.getRequestToken());
			extFileLT(srm, result.getRequestToken(), (URI)key, new Integer(100));
			extFileLTInSpace(srm, "V.2", null, new Integer(100));
		    }
		    which++;
		}	
	    } else {
		System.out.println("empty result");				 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
	
    public static void bringOnlineMultiFiles(ISRM srm, String[] surls, String token, InputInfo info) {
	System.out.println("bringOnline from multiple files");
	try {
	    SrmBringOnlineRequest r = new SrmBringOnlineRequest();
	    int total = surls.length;
	    
	    TGetFileRequest[] fileReqList = new TGetFileRequest[total];
	    
	    for (int i=0; i<total; i++) {
		TGetFileRequest curr = new TGetFileRequest();
		
		curr.setSourceSURL(TSRMUtil.createTSURL(surls[i]));
		//curr.setSpaceToken(token);

		if (surls[i].endsWith("/")) {
		    TDirOption dirOp = new TDirOption();
		    dirOp.setIsSourceADirectory(true);
		    dirOp.setAllLevelRecursive(new Boolean(true));
		    //dirOp.setNumOfLevels(1);
		    curr.setDirOption(dirOp);
		    System.out.println("....... source:"+surls[i]+" is a known to be a dir. Making it all level recursive");
		}
		
		fileReqList[i] = curr;				 
	    }
	    
	    r.setArrayOfFileRequests(TSRMUtil.convertToArray(fileReqList));

	    //TStorageSystemInfo ssinfo = new TStorageSystemInfo();
	    //ssinfo.setValue("username=get()&pwd=hardcoded");
	    ArrayOfTExtraInfo ssinfo = TDeviceAccessInfo.gCreateStorageSystemInfo("get()", TSRMUtil.getDefaultProxyStr());
	    r.setStorageSystemInfo(ssinfo);	
	    if (info._tokenInfo != null) {
		r.setTargetFileRetentionPolicyInfo(info._tokenInfo._type);
	    }
	    r.setTargetSpaceToken(token);

	    SrmBringOnlineResponse result = srm.srmBringOnline(r);
	    
	    if (result != null) {
		System.out.println("I got status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		System.out.println("  request id="+result.getRequestToken());
		
		if (result.getArrayOfFileStatuses() == null) {
		    return;
		}
		HashMap statusArray = new HashMap();
		for (int i=0; i<result.getArrayOfFileStatuses().getStatusArray().length; i++) {
		    TBringOnlineRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(i);
		    System.out.println(i+"th file status="+fileStatus.getStatus().getStatusCode());
		    if (fileStatus.getStatus().getExplanation() != null) {
			System.out.println("            exp="+fileStatus.getStatus().getExplanation());
		    }
		    URI surlInfo = r.getArrayOfFileRequests().getRequestArray()[i].getSourceSURL();
		    statusArray.put(surlInfo, fileStatus);
		    //statusArray.put(fileStatus.getFromSURLInfo(), fileStatus);
		}
		
		// try to call getStatus periodically
		
		int which = 0;
		while (statusArray.size() > 0) {
		    Thread.sleep(1500);				    
		    if (which >= statusArray.size()) {
			which = 0;
		    }

		    Object key = (statusArray.keySet().toArray()) [which];
		    TBringOnlineRequestFileStatus status = (TBringOnlineRequestFileStatus)(statusArray.get(key));
		    TStatusCode code = status.getStatus().getStatusCode();
		    if ((code == TStatusCode.SRM_REQUEST_INPROGRESS) || (code == TStatusCode.SRM_REQUEST_QUEUED)) {
			TBringOnlineRequestFileStatus s =  checkBringOnlineStatus(srm, (URI)key, result.getRequestToken());
			if (s == null) {
			    break;
			} else {
			    code = s.getStatus().getStatusCode(); 		
			}
		    }
		    if ((code != TStatusCode.SRM_REQUEST_INPROGRESS) && (code != TStatusCode.SRM_REQUEST_QUEUED)) {
			statusArray.remove(key);

			releaseFile(srm, true, (URI)key, result.getRequestToken());
		    }
		    which++;
		}
	    } else {
		System.out.println("empty result");				 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static TCopyRequestFileStatus checkCopyStatus(ISRM srm, URI src, URI tgt, String token)
	throws URI.MalformedURIException, java.rmi.RemoteException 	
    {
	System.out.println("==> checking COPY status of surl:"+src+"  rid ="+token);
	
	SrmStatusOfCopyRequestRequest r = new SrmStatusOfCopyRequestRequest();
	
	r.setArrayOfSourceSURLs(TSRMUtil.convertToArray(src));
	
	r.setArrayOfTargetSURLs(TSRMUtil.convertToArray(tgt));
	r.setRequestToken(token);
	
	SrmStatusOfCopyRequestResponse result = srm.srmStatusOfCopyRequest(r);
	
	
	if (result.getReturnStatus() != null)      
	{
	    System.out.println("output status is:"+result.getReturnStatus().getStatusCode());
	    System.out.println("             exp:"+result.getReturnStatus().getExplanation());
	} else {
	    return null;
	}	

	if (result.getArrayOfFileStatuses() == null) {
	    System.out.println("!no array of file status out.");
	    return null;
	}

	System.out.println(result.getArrayOfFileStatuses());

	TCopyRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
	System.out.println("  0th= "+fileStatus.getStatus());

	if (fileStatus.getStatus() == null) {
	    return null;
	} else {
	    System.out.println("from url=:"+fileStatus.getSourceSURL());
	    System.out.println("  to url=:"+fileStatus.getTargetSURL());
	    System.out.println("   url status:"+fileStatus.getStatus().getStatusCode());
	    System.out.println("          exp:"+fileStatus.getStatus().getExplanation());	    
	}
	
	if (fileStatus.getFileSize() != null) {
	    System.out.println("    size:"+fileStatus.getFileSize().longValue());
	}
	
	if (fileStatus.getRemainingFileLifetime() != null) {
	    System.out.println("    remaining pin time:"+fileStatus.getRemainingFileLifetime().intValue());
	}
	
	return fileStatus;
    }
	

    public static TBringOnlineRequestFileStatus checkBringOnlineStatus(ISRM srm, URI url, String token) 
	throws URI.MalformedURIException,java.rmi.RemoteException 
    {
	System.out.println("==> checking prepareToGet status of surl:"+url);
	
	SrmStatusOfBringOnlineRequestRequest r = new SrmStatusOfBringOnlineRequestRequest();
	
	if (url != null) {
	r.setArrayOfSourceSURLs(TSRMUtil.convertToArray(url));
	}
	r.setRequestToken(token);
	
	SrmStatusOfBringOnlineRequestResponse result = srm.srmStatusOfBringOnlineRequest(r);
		
	if ((result.getReturnStatus().getStatusCode() != TStatusCode.SRM_SUCCESS) &&
	    (result.getReturnStatus().getStatusCode() != TStatusCode.SRM_DONE))
	{
	    System.out.println("req status is:"+result.getReturnStatus().getStatusCode());
	    System.out.println("          exp:"+result.getReturnStatus().getExplanation());
	    //return null;
	}
	
	if (result.getArrayOfFileStatuses() == null) {
	    System.out.println("No contents in return status.");
	    return null;
	} 
       
	TBringOnlineRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
	
	//System.out.println("from url   =:"+fileStatus.getFromSURLInfo().getValue());
	System.out.println("  url status:"+fileStatus.getStatus().getStatusCode());
	System.out.println("         exp:"+fileStatus.getStatus().getExplanation());
	if (fileStatus.getFileSize() != null) {
	    System.out.println("        size:"+fileStatus.getFileSize().longValue());
	}
	if (fileStatus.getRemainingPinTime() != null) {
	    System.out.println("        remaining pin time:"+fileStatus.getRemainingPinTime().intValue());
	}
	
	return fileStatus;
    }
    
   
    public static TGetRequestFileStatus checkGetStatus(ISRM srm, URI url, String token) 
	throws URI.MalformedURIException,java.rmi.RemoteException 
    {
	System.out.println("==> checking prepareToGet status of surl:"+url);
	
	SrmStatusOfGetRequestRequest r = new SrmStatusOfGetRequestRequest();
	
	//r.setArrayOfSourceSURLs(TSRMUtil.convertToArray(url));
		
	r.setRequestToken(token);
	
	SrmStatusOfGetRequestResponse result = srm.srmStatusOfGetRequest(r);
		
	if ((result.getReturnStatus().getStatusCode() != TStatusCode.SRM_SUCCESS) &&
	    (result.getReturnStatus().getStatusCode() != TStatusCode.SRM_DONE))
	{
	    System.out.println("req status is:"+result.getReturnStatus().getStatusCode());
	    System.out.println("          exp:"+result.getReturnStatus().getExplanation());
	    //return null;
	}
	
	if (result.getArrayOfFileStatuses() == null) {
	    System.out.println("No contents in return status.");
	    return null;
	} 


	TGetRequestFileStatus[] statusArray = result.getArrayOfFileStatuses().getStatusArray();
	for (int i=0; i<statusArray.length; i++) {
	    TGetRequestFileStatus fileStatus = statusArray[i];
	
	    System.out.println("from url   =:"+fileStatus.getSourceSURL());
	    System.out.println("  url status:"+fileStatus.getStatus().getStatusCode());
	    System.out.println("         exp:"+fileStatus.getStatus().getExplanation());
	    if (fileStatus.getFileSize() != null) {
		System.out.println("        size:"+fileStatus.getFileSize().longValue());
	    }
	    if (fileStatus.getTransferURL() != null) {
		System.out.println("        turl:"+fileStatus.getTransferURL());
	    }
	    if (fileStatus.getRemainingPinTime() != null) {
		System.out.println("        remaining pin time:"+fileStatus.getRemainingPinTime().intValue());
	    }
	}
	
	return statusArray[0];
    }
    
   
    
    public static TPutRequestFileStatus checkPutStatus(ISRM srm, URI surl, String token) 
	throws URI.MalformedURIException, java.rmi.RemoteException 
    {
	System.out.println("==> checking PUT status of surl:"+surl);
	
	SrmStatusOfPutRequestRequest r = new SrmStatusOfPutRequestRequest();
	
	r.setArrayOfTargetSURLs(TSRMUtil.convertToArray(surl));
	
	r.setRequestToken(token);
	
	SrmStatusOfPutRequestResponse result = srm.srmStatusOfPutRequest(r);       
	
	System.out.println("req status is:"+result.getReturnStatus().getStatusCode());
	System.out.println("          exp:"+result.getReturnStatus().getExplanation());	
	
	if (result.getArrayOfFileStatuses() == null) {
	    System.out.println("No contents in return status.");
	    return null;
	} 

	TPutRequestFileStatus fileStatus = result.getArrayOfFileStatuses().getStatusArray(0);
	
	if (fileStatus.getSURL() != null) {
	    System.out.println("details: Site url="+fileStatus.getSURL().toString());
	} 
	
	System.out.println("       url status="+fileStatus.getStatus().getStatusCode());
	System.out.println("              exp="+fileStatus.getStatus().getExplanation());
	if (fileStatus.getFileSize() != null) {
	    System.out.println("             size="+fileStatus.getFileSize().longValue());
	}
	if (fileStatus.getTransferURL() != null) {
	    System.out.println("             turl="+fileStatus.getTransferURL());
	}
	if (fileStatus.getRemainingFileLifetime() != null) {
	    System.out.println("    remaining surl lifetime="+ fileStatus.getRemainingFileLifetime().intValue());
	}
	if (fileStatus.getRemainingPinLifetime() != null) {
	     System.out.println("            turl lifetime="+fileStatus.getRemainingPinLifetime().intValue());
	}
	
	return fileStatus;
	
    }
    
    public static SrmReserveSpaceResponse reserveSpace(ISRM srm, InputInfo info) { //String strUID, TSpaceType spaceType) {
	System.out.println(">>--------------- Reserving a space ----------------");
	System.out.println(">>  "+info._tokenInfo._type);

	String id = info._uid;
	
	SrmReserveSpaceRequest r = new SrmReserveSpaceRequest();
	r.setAuthorizationID(id);
		
	r.setRetentionPolicyInfo(info._tokenInfo._type);
	r.setDesiredLifetimeOfReservedSpace(info._tokenInfo._lifetimeObj);
	if (info._tokenInfo._sizeObj != null) {		    
	    r.setDesiredSizeOfTotalSpace(info._tokenInfo._sizeObj);
	}
	
	
	//TStorageSystemInfo ssinfo = new TStorageSystemInfo();
	////ssinfo.setValue("username=reserveSpace&pwd=hardcoded");
	//ssinfo.setValue("username=reserveSpace&pwd="+TSRMUtil.getDefaultProxyStr());
	ArrayOfTExtraInfo ssinfo = TDeviceAccessInfo.gCreateStorageSystemInfo("reserveSpace", TSRMUtil.getDefaultProxyStr());
	r.setStorageSystemInfo(ssinfo);
	try {
	    SrmReserveSpaceResponse result = srm.srmReserveSpace(r);
	    if (result != null) {
		//srm.srmReleaseSpace(null);
		System.out.println("I got status :"+result.getReturnStatus().getStatusCode());
		System.out.println("  explanation:"+result.getReturnStatus().getExplanation());
		if (result.getSpaceToken() != null) {
		    System.out.println("I got space token: "+result.getSpaceToken());
		}
		if (result.getSizeOfGuaranteedReservedSpace() != null) {
		    System.out.println("  and guaranteed space size = "+result.getSizeOfGuaranteedReservedSpace().longValue());
		} 
		if (result.getSizeOfTotalReservedSpace() != null) {
		    System.out.println("  and total space size = "+result.getSizeOfTotalReservedSpace().longValue());
		}
		if (result.getLifetimeOfReservedSpace() != null) {
		    System.out.println("  and lifetime = "+result.getLifetimeOfReservedSpace().intValue());
		}
	    } else {
		System.out.println("empty result");
		return null;
	    }
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }
 
    public static void compactToken(ISRM srm, String token) {
    }
    public static void updateToken(ISRM srm, String token, long gSpaceInMB, long tSpaceInMB, long lifetime)   {
	try {
	    System.out.println("-------- updating token");
	    
	    SrmUpdateSpaceRequest r1 = new SrmUpdateSpaceRequest();
	    r1.setSpaceToken(token);  

	    long g = gSpaceInMB*(long)1048576;	    
	    UnsignedLong newSizeG = TSRMUtil.createTSizeInBytes(g);
	    
	    r1.setNewSizeOfGuaranteedSpaceDesired(newSizeG);

	    long t = tSpaceInMB*(long)1048576;;
	    UnsignedLong newSizeT = TSRMUtil.createTSizeInBytes(t);
	    r1.setNewSizeOfTotalSpaceDesired(newSizeT);
	    
	    Integer spaceLifetime= TSRMUtil.createTLifeTimeInSeconds(lifetime, false);

	    //r1.setNewLifeTimeFromCallingTime(spaceLifetime);
	    
	    System.out.println("  Total   = "+newSizeT.longValue());
	    System.out.println("  Min     = "+newSizeG.longValue());
	    System.out.println("  LifeTime="+spaceLifetime.intValue());
	    SrmUpdateSpaceResponse result1 = srm.srmUpdateSpace(r1);
	    
	    if (result1 != null) {
		System.out.println("I got status :"+result1.getReturnStatus().getStatusCode());
		if (result1.getLifetimeGranted() != null) {
		    System.out.println(" lifetime="+result1.getLifetimeGranted().intValue());
		}
		if (result1.getSizeOfGuaranteedSpace() != null){
		    System.out.println(" Min     ="+result1.getSizeOfGuaranteedSpace().longValue());
		}
		if (result1.getSizeOfTotalSpace() != null) {
		    System.out.println(" Max     ="+result1.getSizeOfTotalSpace().longValue());
		}
		System.out.println("  explanation:"+result1.getReturnStatus().getExplanation());
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void setSecurityProperties(Stub stub,
					     URL endpoint, 
					     boolean doDelegate)
	                                   //ServiceContext context) 
    {
	if (endpoint.getProtocol().equals("httpg")) {
	    /*
	    // sets authorization type
	    stub._setProperty(
	    Constants.AUTHORIZATION, NoAuthorization.getInstance()
	    );
	    
	    // sets gsi mode
	    stub._setProperty(GSIConstants.GSI_MODE, GSIConstants.GSI_MODE_LIMITED_DELEG);
	    */
	    // for httpg, same as above
	    
	    
	    System.out.println("... either would do with 3.2 to delegate ");
	    if (doDelegate) {
			System.out.println(".......not much to do about delegation.");
	    }
	    
	    
	    //stub._setProperty(GSIConstants.GSI_MODE,GSIConstants.GSI_MODE_FULL_DELEG); // no need after setting delegation

	    //stub._setProperty(GSIConstants.GSI_AUTHORIZATION, HostAuthorization.getInstance());
	    //stub._setProperty(GSIConstants.GSI_AUTHORIZATION, NoAuthorization.getInstance());
	    /*
	      System.out.println("........... using identity authorization ...........");
	    stub._setProperty(GSIConstants.GSI_AUTHORIZATION, new IdentityAuthorization("/DC=org/DC=doegrids/OU=Services/CN=srm/dmx09.lbl.gov"));
	    */
	    /*
	      // default works
	    GSIUtils.setDelegationGSIProperties(stub, endpoint);
	    stub._setProperty(GSIConstants.GSI_MODE,GSIConstants.GSI_MODE_FULL_DELEG);
	    */
	    
	} else {
	    //context.setAuthentication(stub);
	    throw new RuntimeException("Current only supporting httpg");
	}
    }              

    public static boolean testPing(ISRM srm) {
	try {	    
	    SrmPingResponse result = srm.srmPing(new SrmPingRequest());
	    System.out.println("srmPing result ="+result);
	    if (result == null) {
		return false;
	    }
	    System.out.println("srmPing returns:\n\tversion="+result.getVersionInfo());	    
	    if (result.getOtherInfo() != null) {
		TExtraInfo[] otherInfo = result.getOtherInfo().getExtraInfoArray();
		for (int i=0; i<otherInfo.length; i++) {
		    System.out.println("\t"+otherInfo[i].getKey()+" = "+otherInfo[i].getValue());
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }
	
    public static boolean testTxfProtocols(ISRM srm) {
	try {	    
	    SrmGetTransferProtocolsResponse result = srm.srmGetTransferProtocols(new SrmGetTransferProtocolsRequest());
	    System.out.println("srmGetTransferProtocol() returns:\n\tstatus="+result.getReturnStatus().getStatusCode());
	    if (result.getProtocolInfo() != null) {
		TSupportedTransferProtocol[] arrayProtocols = result.getProtocolInfo().getProtocolArray();
		for (int i=0; i<arrayProtocols.length; i++) {
		    System.out.println("\t"+arrayProtocols[i].getTransferProtocol());
		}
	    }
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public static void main (String[] args) {	

	test(args);
	if (args.length >= 0) {
	    return;
	}
	}
}
