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

import  gov.lbl.srm.storage.*;
import  gov.lbl.srm.server.*;
//import  srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import  java.util.*;
import  java.io.*;
import  org.apache.axis.types.URI;

public class TSRMCacheLog implements ISRMLogCache {
    private TSRMFileWriter _writer = null;
    private static TSRMCacheLogReader _reader = null;

    public static final String _seperator = " ";

    public static final String _strDevice   = "D";
    public static final String _strTokenIn  = "+T";
    public static final String _strTokenOut = "-T";
    public static final String _strFileIn   = "+f";
    public static final String _strFileOut   = "-f";
    public static final String _strAccount  = "A";

    // wont have fileRemoved, because we need to check whether the physical file exists on
    // each line for file-in anyway
 
    protected static boolean _isAccessingLog = false;

    public TSRMCacheLog() {
	// creates a default event log filename
	Calendar curr = Calendar.getInstance();

	String filename="cachelog."+curr.getTimeInMillis();
	_writer = new TSRMFileWriter(filename);       
    }

    public TSRMCacheLog(String filename) {
	try {
	    readLogStarts(filename);
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	}
	_writer = new TSRMFileWriter(filename);
    }

    public void clean() 
    {}

    private boolean isEnabled() {	
	return (_writer.isEnabled());
    }

    public void accountCreated(TAccount acct) {
	if (!isEnabled()) {
	    return;
	}

	String[] msgItems = {TSRMCacheLog._strAccount, 
			     acct.getID()};

	_writer.writeMsg(generateMsg(msgItems), false);
    }

    public void deviceCreated(TBasicDevice device) {
	if (!isEnabled()) {
	    return;
	}

	String[] msgItems = {TSRMCacheLog._strDevice, 
			     device.getTag(),
			     String.valueOf(device.getTotalSizeInMB()),
			     device.getDescription()};

	_writer.writeMsg(generateMsg(msgItems), false);
    }

   
    public void removeEntry(TSRMLocalFile f) {	
	if (f == null) {
	    return;
	}

	if (isEnabled()) {	
	    actionOnFile(false, f, f.getCurrentSize(), f.getToken());
	    //RuntimeException e = new RuntimeException("removed this: "+f.getCanonicalPath());
	    //e.printStackTrace();
	}
    }

    public boolean removeFile(TSRMLocalFile f) {
	if (f == null) {
	    return false;
	}
	removeEntry(f);
	return TSRMNameSpace.removeFile(f);
    }

    public void addFile(TSRMLocalFile f) {
	if (!isEnabled()) {
	    return;
	}

	actionOnFile(true, f, f.getCurrentSize(), f.getToken());
    }

    public void fileIsAdded(TSRMLocalFile f, long size, TSRMStorage.iSRMSpaceToken token) {
	if (_isAccessingLog || !isEnabled()) {
	    return;
	}
	
	actionOnFile(true, f, size, token);
    }

     private void actionOnFile(boolean isAdded, TSRMLocalFile f, long size, TSRMStorage.iSRMSpaceToken token) {
	String fileTypeString = "";
	if (f.getFileStorageType() != null) {
	    fileTypeString = f.getFileStorageType().toString();
	}

	String isUserAppointedPath = "false";
	if (f.isSFNUserAppointed()) {
	    isUserAppointedPath = "true";
	}
	String surlTag = "T";
	if (f.isSurl()) {
	    surlTag = "S";
	}

	String action = TSRMCacheLog._strFileIn;
	if (!isAdded) {
	    action = TSRMCacheLog._strFileOut;
	}
	String locationStr = "";
	Object location = f.getPhysicalPath();
	if (location != null) {
	    locationStr = location.toString();
	}
	String lifetimeStr = "";
	if (f.isSurl()) {
	    if (locationStr != null) {
		long exptime = f.getExpirationTimeInMilliSeconds();
		if (exptime > 0) {
		    lifetimeStr = String.valueOf(exptime);
		}
	    }
	}
	if (token == null) {
	    RuntimeException ex = new RuntimeException("Null token");
	    TSRMLog.exception(this.getClass(), "actionOnFile "+action, ex);
	    return;
	}

	String surl = writeSourceURI(f);
	String[] msgItems = {action,
			     token.getID(), 
			     f.getCanonicalPath(),
			     surl,
			     String.valueOf(size),
			     isUserAppointedPath,
			     surlTag,
	                     fileTypeString, 
			     locationStr,
	                     lifetimeStr};
	_writer.writeMsg(generateMsg(msgItems), false);	
    }

    public void tokenCreated(TSRMStorage.iSRMSpaceToken token) {
	if (!isEnabled()) {
	    return;
	}

	String userDesc = token.getTokenAttributes().getUserDesc();
	if (userDesc != null) {
	    userDesc = " ["+userDesc+"]";
	} else {
	    userDesc = "";
	}

	String[] msgItems = {TSRMCacheLog._strTokenIn, 			     
			     token.getID(), 
			     token.getHostDevice().getTag(),
			     String.valueOf(token.getTokenAttributes().getGuaranteedSpaceBytes()),
	                     token.getOwner().getID(),
			     String.valueOf(token.getTokenAttributes().getLifeTimeInSeconds()),
	                     String.valueOf(token.getCreationTimeInMilliSeconds())+userDesc};

	_writer.writeMsg(generateMsg(msgItems), false);
    }

    public void tokenRemoved(TSRMStorage.iSRMSpaceToken token) {
	if (!isEnabled()) {
	    return;
	}

	String[] msgItems = {TSRMCacheLog._strTokenOut, 			     
			     token.getID(), 
	                     token.getOwner().getID()}; //+token.getOwner().getID()};

	_writer.writeMsg(generateMsg(msgItems), false);
    }

    public String generateMsg(String[] msgItems) {
	String msg = "";
	for (int i=0; i<msgItems.length-1; i++) {
	    msg += msgItems[i]+_seperator;
	}
	msg += msgItems[msgItems.length-1]+"\n";

	return msg;
    }

    //
    //
    //
    public static void readLogStarts(String cacheLogName) throws java.io.IOException {	
	TSRMLog.info(TSRMCacheLog.class, null, "event=READLOGstarts", "log="+cacheLogName);
	_isAccessingLog = true;
        _reader = new TSRMCacheLogReader(cacheLogName);
	_reader.printContents();
    }

    public  boolean readLogEnds() {
	try {
	    boolean result= _reader.reengineer();
	    _isAccessingLog = false;

	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    _isAccessingLog = false;
	    return false;
	}
    }  

    public static boolean isExpired(long creationTimeMilliSeconds, long durSeconds) {
	if (durSeconds < 0) {
	    return false;
	}
	long currTime = System.currentTimeMillis();

	if ((currTime - creationTimeMilliSeconds > durSeconds*1000) ) {
	    return true;
	}
	return false;
    }

    public static String writeSourceURI(TSRMLocalFile f) {
	String surl = f.getSourceURI().toString();
	if (surl.equalsIgnoreCase(f.getSiteURL().getSURL().toString())) {
	    surl = "self";
	} else if (f.getSourceURI().getHost().equalsIgnoreCase(Config._host) &&
		   f.getSourceURI().getPort() == Config._securePort) 
	{
	    String p = f.getSourceURI().getPath();
	    int addressEnds = surl.indexOf(f.getSourceURI().getPath())+p.length();
	    surl = surl.substring(addressEnds);
	}
	return surl;
    }

       public static boolean readSourceURI(TSRMLocalFile f, String storedURI) {
	if (storedURI.equalsIgnoreCase("self")) {
	    f.setSourceURI(f.getSiteURL().getSURL());
	    return true;
	} else if (storedURI.startsWith("?")) {
	    try {
		URI uri = new URI(Config._wsdlEndPoint);
		uri.setScheme(TSupportedURLWithSRM._DefProtocolStr);
		uri.setPath(uri.getPath()+storedURI);
		f.setSourceURI(uri);
		return true;
	    } catch (URI.MalformedURIException e) {
		e.printStackTrace();
		return false;
	    }
	}
	
	URI uri = TSRMUtil.createTSURL(storedURI);
	if (uri == null) {
	    return false;
	}
	f.setSourceURI(uri);
	return true;	

	/*
	  ArrayOfTExtraInfo ssinfo = TDeviceAccessInfo.gCreateStorageSystemInfo("useDefaultProxyAtStartUp", 
	  TSRMUtil.getDefaultProxyStr());	    
	  sinfo.setStorageSystemInfo(ssinfo);
	*/
    }
}

class TSRMCacheLogReader {
    HashMap _deviceLookup = new HashMap();
    HashMap _tokenLookup  = new HashMap();
    HashMap _fileLookup = new HashMap();
    HashMap _accountLookup = new HashMap();

    boolean _readSuccessfully = false;

    TSRMCacheLogReader(String cacheLogName) throws java.io.IOException {
	//java.io.File f = new File(cacheLogName);
	java.io.File f = TSRMUtil.initFile(cacheLogName);
	if (!f.exists()) {
	    TSRMUtil.startUpInfo(".... cache log: "+cacheLogName+" does not exist.");
	    _readSuccessfully = true;
	    return;
	}
	java.io.FileInputStream cacheLogFile = null;
	InputStreamReader inputReader = null;
	try {
	    cacheLogFile = new java.io.FileInputStream(cacheLogName);
	    inputReader = new InputStreamReader(cacheLogFile);	   
	    BufferedReader input = new BufferedReader(inputReader);

	    String curr;
	    while ((curr = input.readLine()) != null) {
		String[] currItems = curr.split(TSRMCacheLog._seperator);
		if (curr.startsWith(TSRMCacheLog._strDevice)) {
		    _deviceLookup.put(currItems[1], curr);
		} else if (curr.startsWith(TSRMCacheLog._strAccount)) {
		    _accountLookup.put(currItems[1], curr);
		} else if (curr.startsWith(TSRMCacheLog._strTokenIn)) {
		    _tokenLookup.put(currItems[1], curr);
		} else if (curr.startsWith(TSRMCacheLog._strTokenOut)) {
		    _tokenLookup.remove(currItems[1]);
		    _fileLookup.remove(currItems[1]);
		} else if (curr.startsWith(TSRMCacheLog._strFileIn)) {
		    HashMap v = (HashMap)(_fileLookup.get(currItems[1]));
		    if (v == null) {
			v = new HashMap();
			_fileLookup.put(currItems[1], v);
		    }		  

		    v.put(currItems[2], curr);
		} else if (curr.startsWith(TSRMCacheLog._strFileOut)) {
		    HashMap v = (HashMap)(_fileLookup.get(currItems[1]));
		    if (v != null) {
			v.remove(currItems[2]);			
		    } 
		}
	    }
	    _readSuccessfully = true;
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("## error with reading cache log:"+cacheLogName+" contents will be ignored.");
	} finally {
	    if (cacheLogFile != null) {
		cacheLogFile.close();
	    }
	    if (inputReader != null) {
		inputReader.close();
	    }
	}
    }

    boolean isSuccessfullyRead() {
	return _readSuccessfully;
    }

    boolean reengineer() {
	if (!isSuccessfullyRead()) {	    
	    return false;
	}

	reengineerAccounts();

	reengineerDevices();

	boolean tryAgain = reengineerTokens();
	if (tryAgain) {
	    reengineerTokens();
	}

	boolean result = reengineerFiles();
	TSRMUtil.startUpInfo("-------------------- starts ");
	TSRMUtil.startUpInfo("\t devices found="+_deviceLookup.size());
	TSRMUtil.startUpInfo("\t tokens  found="+_tokenLookup.size());
	TSRMUtil.startUpInfo("-------------------- ends ");

	return result;
    }

    void reengineerAccounts() {
	Set accts = _accountLookup.keySet();
	Iterator iter = accts.iterator();

	while (iter.hasNext()) {
	    String curr = (String)(iter.next());

	    if (!curr.equals(TAccountManager._SUPER_USER.getID())) {
		TAccountManager.createAccount(curr);
	    }
	}
    }

    boolean reengineerDevices() {	
	Set tags = _deviceLookup.keySet();
	Iterator iter = tags.iterator();

	Vector invalidDevices = new Vector();
	while (iter.hasNext()) {
	    String curr = (String)(iter.next());
	    String desc = (String)(_deviceLookup.get(curr));
	    TBasicDevice d = getDevice(desc.split(TSRMCacheLog._seperator));
	    if (d != null) {
		_deviceLookup.put(curr, d);
	    } else {
		TSRMUtil.startUpInfo("!device: "+desc+" is not in current setup. Skipping the related file and token info");
		//_deviceLookup.remove(curr);
		invalidDevices.add(curr);
	    }
	}	

	for (int i=0; i<invalidDevices.size(); i++) {
	    String curr = (String)(invalidDevices.get(i));
	    _deviceLookup.remove(curr);
	}
	return (tags.size() == _deviceLookup.size());
    }   

    public boolean reengineerTokens() {
	boolean result = true;

	Set tids = _tokenLookup.keySet();
	Iterator iter = tids.iterator();

	Vector invalidTokens = new Vector();

	while (iter.hasNext()) {
	    String tid = (String)(iter.next());
	    String desc = (String)(_tokenLookup.get(tid));
	    String[] descTokens = desc.split(TSRMCacheLog._seperator);
	    
	    String deviceTag = descTokens[2];

	    TBasicDevice device = (TBasicDevice)(_deviceLookup.get(deviceTag));

	    if (device == null) {
		RuntimeException ex = new RuntimeException("No device found with tag="+deviceTag);
		ex.printStackTrace();
		TSRMUtil.startUpInfo(".....removing "+tid+" b/c of unknown device");
		//_tokenLookup.remove(tid);
		invalidTokens.add(tid);
		//return true;
	    } else {
		TSRMSpaceType spaceType = TSRMSpaceType.getSpaceType(tid);
		
		if (spaceType != null) {	       
		    String accountName = descTokens[4];
		    TAccount user = TAccountManager.getAccount(accountName, true);
		    
		    long size = Long.parseLong(descTokens[3]);
		    long durSeconds = Long.parseLong(descTokens[5]);
		    
		    long creationTimeMilliSeconds = Long.parseLong(descTokens[6]);
		    
		    if (TSRMCacheLog.isExpired(creationTimeMilliSeconds, durSeconds)) {
			TSRMUtil.startUpInfo("... detected expired token: "+tid);
			invalidTokens.add(tid);
			continue;
		    }
		    
		    TTokenAttributes tokenAttr = new TTokenAttributes(spaceType, size);
		    tokenAttr.setLifeTimeInSeconds(durSeconds);
		    tokenAttr.setTokenID(tid);		    
		    
		    if (descTokens.length > 6) {
			int where = desc.indexOf(descTokens[6]);
			where = desc.indexOf("[", where+1);
			if (where > 0) {
			    String userDesc = desc.substring(where+1, desc.length()-1);
			    tokenAttr.setUserDesc(userDesc);
			}
		    }
		    TSRMStorage.iSRMSpaceToken token = device.getToken(tid);
		    
		    if (token == null) {
			token = device.accomodate(tokenAttr);
			if (token != null) {
			    token.setOwner(user);	
			    if (durSeconds > 0) {
				token.setCreationTimeInMilliSeconds(creationTimeMilliSeconds);
			    }
			} else {
			    TSRMUtil.startUpInfo("!!!Cannt accomodate token: "+tid+" in: "+device.getTag());
			}
		    }
		    if (token != null) {
			_tokenLookup.put(tid, token);
		    } else {
			TSRMUtil.startUpInfo("!!! token with desc: "+desc+" cannt be recreated.");
			//_tokenLookup.remove(tid);
			invalidTokens.add(tid);
		    }
		} else {
		    // this space type is not supported anymore..
		    result = false;
		}
	    }
	}

	if (invalidTokens.size() > 0) {
	    for (int i=0; i<invalidTokens.size(); i++) {
		_tokenLookup.remove((String)(invalidTokens.get(i)));
	    }
	}

	//
	// double check the consistency of tids here and max in the system.
	// i.e. donnt want cache log has token (v0, v2), and max in the system is 2. which 
	// will generate next token as v2 and cause conflict.
	//
	if (_tokenLookup.size() > 0) {
	    Object[] tidSet = _tokenLookup.keySet().toArray();	
		/*
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
	    //TSRMUtil.sync(max);
	    */
	    TSRMUtil.sync(tidSet);
	}
	
	return false;
    }

    public boolean reengineerFiles() {
	boolean result = true;

	Set tids = _fileLookup.keySet();
	Iterator iter = tids.iterator();

	while (iter.hasNext()) {
	    String tid = (String)(iter.next());
	    HashMap files = (HashMap)(_fileLookup.get(tid));

	    TSRMUtil.startUpInfo("token id="+tid+"  => "+_tokenLookup.get(tid));

	    TSRMStorage.iSRMSpaceToken token = (TSRMStorage.iSRMSpaceToken)(_tokenLookup.get(tid));
	    if (token == null) {
		// something is wrong, this token doesnt exist. cannt do anything;
		removeRelatedFiles(files);
		continue;
	    }

	    //
	    // we dont bother whether the token is expired or not..user can extend the lifetime
	    //
	    Object[] strArray = files.values().toArray();
	    for (int i=0; i<files.size(); i++) {
		String desc = (String)(strArray[i]);

		TSRMLocalFile file = recreateFile(token, desc.split(TSRMCacheLog._seperator));
		
		if (file == null) {
		    TSRMUtil.startUpInfo("==> "+desc+"==> null");
		} else {
		    TSRMUtil.startUpInfo("==> "+desc+"==> "+file.getName());
		    if (!file.isSurl()) {
			TSRMNameSpace.addEntry(file.getSourceURI().toString());
			TSRMNameSpace.addEntry(file.getSourceURI().toString(), file, null);
		    } else {
			TSRMNameSpace.addEntry(file.getSiteURL().getSURL().toString());
			TSRMNameSpace.addEntry(file.getSiteURL().getSURL().toString(), file, null);
		    }
		    TSRMLog.getCacheLog().addFile(file);
		}
	    }
	}

	return result;
    }

  

    public void removeRelatedFiles(HashMap files) {
	Object[] strArray = files.values().toArray();
	for (int i=0; i<files.size(); i++) {
	    String desc = (String)(strArray[i]);
	    String[] fileDesc = desc.split(TSRMCacheLog._seperator);
	    if (fileDesc.length >= 9) {
		String location = fileDesc[8];
		TSRMUtil.cleanFilePath(location);
	    }
	}
    }     

    public  TSRMLocalFile recreateFile(TSRMStorage.iSRMSpaceToken token,
				       String[] fileDesc) 
    {	
	long bytes = Long.parseLong(fileDesc[4]);
		
	TSRMLog.debug(this.getClass(), null, "event=recreateFile", "name="+fileDesc[2]+" bytes="+bytes);
		     	
	TFileStorageType fileType = null;
	if (fileDesc.length >= 8) {
	    fileType = TFileStorageType.fromValue(fileDesc[7]);
	}

	TSRMLocalFile result = recreateFilePath(fileDesc[2], fileType, fileDesc[6]);	  	

	try {
	    if ((result.getToken() != null) && (result.getToken() !=token)) {
		removeTrace(result);
		return null;
	    }		   	    	    	    
	    
	    if (!TSRMCacheLog.readSourceURI(result, fileDesc[3])) {
		removeTrace(result);
		return null;
	    }

	    //result.setSourceURI(src.getURI());	    
	    token.findSpaceForFile(result, bytes);
	    /*	    
	    boolean sfnAssigned = Boolean.valueOf(fileDesc[5]).booleanValue();
	    
	    if (!sfnAssigned) {
		result.setPhysicalLocation(src, null);
	    } else {
		TSupportedURL sfn = TSupportedURL.create(result.getSiteURL());
		result.setPhysicalLocation(sfn, null);
		result.setSFNAppointedByUser();
	    }
	    */
	    if (fileDesc.length >= 9) {
		String location = fileDesc[8];		
		result.setPhysicalLocation(location, null);
	    }
	    
	    if (fileDesc.length >= 10) {
		long expTime = Long.parseLong(fileDesc[9]);		
		if ((expTime > 0) && (System.currentTimeMillis() > expTime)) {
		    TSRMUtil.startUpInfo("File is expired"+result.getCanonicalPath());
		    removeTrace(result);
		} else {
		    result.setInheritedLifeTime(expTime);
		}
	    }

	    long actualBytes = result.getCurrentSize();

	    if (actualBytes <= 0) {
		removeTrace(result);
		return null;
	    }
	    
	    if (actualBytes == bytes) { // ok, everything matches
		//result.setSourceURI(src.getSURL());	    
		//token.findSpaceForFile(result, bytes);	    
	    } else {
		TSRMLog.info(getClass(), null, "event=conflictedSizeFound action=removingFileFromDisk", "path="+result.getCanonicalPath());
		removeTrace(result);
		return null;
	    }
	
	    result.updateToken();
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    removeTrace(result);
	    return null;
	}
    }

    private void removeTrace(TSRMLocalFile f) {
	//f.deleteMe();
	f.unsetToken(false);

	TSRMUtil.wipe(f);
	/*
	TSRMLocalDir p = f.getParent();

	p.rmFileName(f.getName());

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
	*/
    }

    private TSRMLocalFile recreateFilePath(String path, TFileStorageType fileType, String surlTag) {
	String slash = path.substring(0,1);
	String[] pathTokens = path.substring(1).split(slash);

	String uid = pathTokens[0];
	TAccount user = TAccountManager.getAccount(uid, true);

	if (user == null) {
	    TSRMLog.debug(this.getClass(), null, "error=recreatingFailed", "uid="+uid);
	    return null;
	}

	boolean isSurl = false;
	if (surlTag.equals("S")) {
	    isSurl = true;
	}

	TSRMLocalDir dir = user.getTopDir();

	for (int i=1; i<pathTokens.length-1; i++) {
	    String curr = pathTokens[i];
	    dir = dir.createDir(curr, isSurl);
	}

	return dir.createSubFile(pathTokens[pathTokens.length-1], fileType, isSurl);
    }

    private TBasicDevice getDevice(String[] deviceInfo, TSRMSpaceType type) {
	TSRMStorage.iSRMSpace v = TSRMStorage.getSpaceManager(type);
	TBasicDevice result = null;
	if (v != null) {
	    result = v.getDevice(deviceInfo[1]);
	}

	if (result == null) {
	    return null;	    
	}

	if (result.getTotalSizeInMB() <= Integer.parseInt(deviceInfo[2])) {
	    return result;
	}
	return null;   
    }


    public  TBasicDevice getDevice(String[] deviceInfo) {
	TSRMSpaceType type = TSRMSpaceType.Volatile;
	TBasicDevice result = getDevice(deviceInfo, type);
	if (result != null) {
	    return result;
	}

	type = TSRMSpaceType.Durable;
	result =getDevice(deviceInfo, type);
	if (result != null) {
	    return result;
	}

	type = TSRMSpaceType.Permanent;
	result = getDevice(deviceInfo, type);
	return result;
    }

    void printContents() {
	// now put the tokens to the devices
	TSRMUtil.startUpInfo("--------------------------- starts");
        TSRMUtil.startUpInfo("# of tokens: "+_tokenLookup.size());
	for (int i=0; i<_tokenLookup.size(); i++) {
	    TSRMUtil.startUpInfo((_tokenLookup.values().toArray())[i]);
	}
	// now put the files to the tokens
	TSRMUtil.startUpInfo("# of tokens that have files: "+_fileLookup.size());
	for (int i=0; i < _fileLookup.size(); i++) {
	    HashMap v = (HashMap)((_fileLookup.values().toArray())[i]);
	    TSRMUtil.startUpInfo("==========");
	    for (int j=0; j < v.size(); j++) {
		 TSRMUtil.startUpInfo("\t"+(v.values().toArray())[j]);
	    }
	}
	TSRMUtil.startUpInfo("--------------------------- ends");
    }
}
