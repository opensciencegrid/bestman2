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

import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.TSRMStorage;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.impl.*;

import java.io.*;
//import java.io.StringWriter;
import java.net.*;
import java.util.*;

import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.globus.security.gridmap.GridMap;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class Config {    
    public static String _serviceHandle = ""; 
    public static String _id            = null; 
    public static String _DefContactProtocolStr = "httpg";
    public static String _defaultEventFileName = "/var/log/event.srm.log";

    public static String _host          = null;
    public static int _securePort       = 8443;
    public static int _publicPort       = 8080; // not used in GT4 version
    public static long _mssTimeOutMilliSeconds = 3600000;
    public static String _pluginDir     = null;
    public static String _gridMapFileName   = "/etc/grid-security/grid-mapfile";
    public static String _proxyFileName     = null;
    public static String _userKeyFileName     = null;
    public static String _userCertFileName     = null;
    public static boolean _useBerkeleyDB      = true;
    public static int _retryIntervalSeconds = 120;
    public static boolean _silent = false;
    public static boolean _doMarkupPingMsg = false;
    public static boolean _usingCacheKeyword = false;
    public static String _DefCacheKeyword = "/srmcache";
    public static String _uploadQueueParameter = null;
    public static String _blockedLocalPath = "/;/etc/;/var";
    public static String _sudoCommand = null;

    //public static 
    public static int _retryCountMax = 2;
    public static String _gucLocation = null;
    //public static TSRMLog _log = new TSRMLog();

    public static Integer _nStream = null;
    public static Integer _nBufferBytes = null;
    public static boolean _doDcau = true;

    public static String _configFileNameLoaded = null;

    public static final String _DefDefaultConfigFileName  = "./ws.rc";
    public static final String _DefConfigEntrySecurePort  = "securePort";
    public static final String _DefConfigEntryPublicPort  = "publicPort";

    public static final String _DefConfigEntryProtocol  = "protocol";
    public static final String _DefConfigEntryMarkupPingMsg  = "markupPingMsg";

    public static final String _DefConfigEntryMSSTimeOut  = "mssTimeOutSeconds";
    //public static final String _DefConfigEntryServiceName  = "ServiceName";
    public static final String _DefConfigEntryFactoryID   = "FactoryID";
    public static final String _DefConfigEntryPlugin  = "pluginLib";
    public static final String _DefConfigEntryGridMapFileName = "GridMapFileName";
    public static final String _DefConfigEntryProxyFileName   = "ProxyFileName";
    public static final String _DefConfigEntryKeyFileName   = "KeyFileName";
    public static final String _DefConfigEntryCertFileName   = "CertFileName";
    public static final String _DefConfigEntryEventLogFileName     ="EventLogLocation";
    public static final String _DefConfigEntryCacheLogFileName     ="CacheLogLocation";
    public static final String _DefConfigEntryGlobusParallelStream ="GridFTPNumStreams";
    public static final String _DefConfigEntryGlobusBufferSize     ="GridFTPBufferSizeBytes";
    public static final String _DefConfigEntryGlobusBufferSizeMB   ="GridFTPBufferSizeMB";
    public static final String _DefConfigEntryGlobusDoDcau   ="GridFTPDcauOn";
    public static final String _DefConfigEntryUseBerkeleyDB       ="useBerkeleyDB";
    public static final String _DefConfigEntryFailedFTPRetryPolicy = "retryGsiftp(seconds/maxRetry)";
    public static final String _DefConfigEntrySilent = "silent";
    public static final String _DefConfigEntryUploadQueueParameter = "uploadQueueParameter";
    public static final String _DefConfigEntrySupportedProtocols  ="supportedProtocolList"; //gsiftp://host:port;http://host:port;
	public static final String _DefConfigEntryUserDefinedProtocols="acceptProtocols";
    public static final String _DefConfigEntrySupportedProtocolPolicy  ="protocolSelectionPolicy"; //gsiftp://host:port;http://host:port;
    public static final String _DefConfigEntryUsingCacheKeyword = "srmcacheKeywordOn";
    public static final String _DefConfigEntryLocalPathListBlock = "localPathListToBlock";
    public static final String _DefConfigEntryLocalPathListAllow = "localPathListAllowed";
    public static final String _DefConfigEntryDisableSpaceMgt = "disableSpaceMgt";
    public static final String _DefConfigEntryDisableDirMgt   = "disableDirectoryMgt";
    public static final String _DefConfigEntryDisableLocalAuthorization = "disableLocalAuthorization";
    public static final String _DefConfigEntryDisableRemoteCopy = "disableSrmCopy";
    public static final String _DefConfigEntryUserTokens       ="userSpaceKeywords";
    public static final String _DefConfigEntryGUMSService          ="GUMSserviceURL";
    public static final String _DefConfigEntryGUMSCurrHostDN       ="GUMSCurrHostDN";
	public static final String _DefConfigEntryGUMSProtocol      ="GUMSProtocol";
    public static final String _DefConfigEntryAccessFileSysUsingGSIFTP="accessFileSysViaGsiftp";
    public static final String _DefConfigEntryAccessFileSysUsingSUDO="accessFileSysViaSudo";
    public static final String _DefConfigEntryNoSudoOnLs="noSudoOnLs";
    public static final String _DefConfigEntryNoEventLog="noEventLog";
    public static final String _DefConfigEntryNoCacheLog="noCacheLog";
    public static final String _DefConfigEntryGUCPATH="guc_path";
    public static final String _DefConfigEntryNoStopOnTimedOutThread="InterruptOnlyOnTimedOutThread";
    public static final String _DefConfigEntryMappedIDRefreshInterval="LifetimeSecondsMappedIDCached";
    public static final String _DefConfigEntryMappedIDStorageLimit="MaxMappedIDCached";
    public static final String _DefConfigEntryLocalFileSystemAccess="localFileSystemAccessClass";
    public static final String _DefConfigEntryComputeChecksumOnFileLs="showChecksumWhenListingFile";
	public static final String _DefConfigEntryChecksumCommand="hexChecksumCommand";
    public static final String _DefConfigEntryDefaultChecksumType="defaultChecksumType";
    public static final String _DefConfigEntryClientWaitParamsMaxWaitMillis="gsiftpClientWaitParamsMaxWaitMillis";
    public static final String _DefConfigEntryClientWaitParamsWaitDelayMillis="gsiftpClientWaitParamsWaitDelayMillis";
    public static final String _DefConfigEntryUserFileAuthorization="userFileAuthorizationClass";
    public static final String _DefConfigEntryEventLogLevel="eventLogLevel";
    public static final String _DefConfigEntryValidateVoms="validateVomsProxy";
    public static final String _DefConfigEntrySudoCommand="sudoCommand";
    public static final String _DefConfigStaticTokens="staticTokenList";

    public static StaticToken[] _staticTokenList = null;

    public static boolean _doValidateVoms=false;
    public static int _eventLogLevel = TSRMEventLog._gLogLevelDEBUG;
    
    public static int _gsiftpClientWaitParamsMaxWaitMillis = 3000000;
    public static int _gsiftpClientWaitParamsWaitDelayMillis = 20000;

    public static int _defaultChecksumType=1;
	public static String  _checksumCommand = null;
    public static boolean _doComputeFileChecksum = false;
    public static Class _localFileAccessClass = null;

    public static final int _defaultMaxMappedID=1000;
    public static final int _defaultMappedIDLifetime=-1;

    public static URI _wsdlEndPoint = null;
    public static GridMap _gridMap = null;

    public static boolean _stopOnTimedOutThread = false;

    public static boolean _accessFileSysUsingGSIFTP = false;
    public static boolean _accessFileSysUsingSUDO = false;
    public static boolean _noSudoOnLs=true;

    public static boolean _disableSpaceMgt = false;
    public static boolean _disableDirMgt = false;
    public static boolean _disableRemoteCopy = false;
    public static boolean _disableLocalAuthorization = true;

    public static IntGUMS _gumsClient = null;
    public static String _gumsHostDN = null;
    
    public static TSRMHistory _mappedIDHistory = null;

    public static boolean _isInitialized = false;
    public static java.text.SimpleDateFormat _dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS z ");

    public static Properties _prop = null; // for srmPing() display

    //
    //
    //
    public static boolean isSpaceMgtDisabled() {
	return _disableSpaceMgt;
    }

    public static boolean isDirMgtDisabled() {
	return _disableDirMgt;
    }

    public static boolean isRemoteCopyDisabled() {
	return _disableRemoteCopy;
    }

    public static boolean isLocalAuthorizationDisabled() {
	return _disableLocalAuthorization;
    }

    public static void setWSDLEndpoint(String addressStr) {
	String urlStr = _DefContactProtocolStr+"://"+addressStr;
	
	TSRMUtil.startUpInfo(".........local SRM is on: "+urlStr+"  current user:"+System.getProperty("user.name"));
	try {
	    _wsdlEndPoint = new URI(urlStr);
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Failed to initiate uri:"+urlStr+" "+e.toString());
	    //System.exit(1);
	    throw new RuntimeException("Failed to initiate uri:"+urlStr+" "+e.toString());
	}
    }

    public static boolean isADirectoryName(String userInput) {
	File f = TSRMUtil.initFile(userInput);
	if (f.isDirectory()) {
	    return true;
	}
	return false;
    }

    private static boolean isFalse(Properties prop, String entry) {
	String value = prop.getProperty(entry);
	if ((value != null) && (value.equalsIgnoreCase("false"))) {
	    return true;
	}
	return false;
    }

    public static boolean isTrue(Properties prop, String entry) {
	String value = prop.getProperty(entry);
	if ((value != null) && (value.equalsIgnoreCase("true"))) {
	    return true;
	}
	return false;
    }

    private static Properties getConfigProperties(String configFileName) throws java.io.IOException{
	Properties prop = new Properties();
	java.io.FileInputStream configFile = null;
	try {
	    configFile = new java.io.FileInputStream(configFileName);
	    prop.load(configFile);
	    return prop;
	} catch (java.io.IOException e) {
	    //e.printStackTrace();
	    System.err.println("## error with reading config file, abort!");
	    //System.exit(1);
	    throw new RuntimeException("## error with reading config file, abort!");
	} finally {
	    if (configFile != null) {
		configFile.close();
	    }
	}	
	//return null;
    }

    private static void assignLogs(Properties prop) {
	String dbOption = prop.getProperty(Config._DefConfigEntryUseBerkeleyDB);
	if ((dbOption != null) && (dbOption.equalsIgnoreCase("false"))) {
	    _useBerkeleyDB = false;
	}

	if (isTrue(prop, Config._DefConfigEntryNoEventLog)) {
	    TSRMUtil.startUpInfo("             no event log");
	} else {
	    String evtLogName = prop.getProperty(Config._DefConfigEntryEventLogFileName);
	    if (evtLogName != null) {
		if (isADirectoryName(evtLogName)) {
		    evtLogName += "/event.srm.log";
		    prop.put(Config._DefConfigEntryEventLogFileName, evtLogName);
		}		    
		TSRMLog.setEventLog(evtLogName);
	    } else {	   
		prop.put(Config._DefConfigEntryEventLogFileName, _defaultEventFileName);
		TSRMLog.setEventLog(_defaultEventFileName);
	    }
	}
	if (isTrue(prop, Config._DefConfigEntryNoCacheLog)) {
		TSRMUtil.startUpInfo("             no cache log");
		return;
	}
	
	String cacheLogName = prop.getProperty(Config._DefConfigEntryCacheLogFileName);
	if (cacheLogName  == null) {
	    cacheLogName = "/var/log/";
	    if (!_useBerkeleyDB) {
		cacheLogName = "/var/log/cache.srm.log";
	    } 
	    prop.put(Config._DefConfigEntryCacheLogFileName, cacheLogName);
	}
	try {
	    if (isADirectoryName(cacheLogName)) {
	    if (!_useBerkeleyDB) {
		cacheLogName += "/cache.log.srm";
		prop.put(Config._DefConfigEntryCacheLogFileName, cacheLogName);
	    }
	    }
	    
	    TSRMLog.setCacheLog(cacheLogName);
	} catch (java.io.IOException e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Will not continue without a cache log.");
	    //System.exit(1);
	    throw new RuntimeException("Will not continue without a cache log."+e.toString());
	}
    }


    private static void handleRetryOption(Properties prop) {
	String retryOption = prop.getProperty(Config._DefConfigEntryFailedFTPRetryPolicy);
	if (retryOption != null) {
	    String[] vals =  retryOption.split("/");
	    if (vals == null) {
		TSRMUtil.startUpInfo("!! error, your input value:"+retryOption+" is invalid. format is: num/num");
	    }
	    if (vals.length>=1) {
		int sec = Integer.parseInt(vals[0]);
		if (sec > 0) {
		    _retryIntervalSeconds = sec;
		} else {
		    TSRMUtil.startUpInfo("!! error, your input value:"+retryOption+" contains negative integer. Ignored.");
		}
	    }
	    if (vals.length >=2) {
		int max = Integer.parseInt(vals[1]);
		if (max >= 0) {
		    _retryCountMax = max;
		} else {
		    TSRMUtil.startUpInfo("!! error, your input value:"+retryOption+" contains negative integer. Ignored.");
		}
	    }
	}

	TSRMUtil.startUpInfo( "......[gsiftp retry options]: wait_interval(seconds)="+_retryIntervalSeconds+"  max="+ _retryCountMax );

    }

    private static void handleGlobusOptions(Properties prop) {
	String dodcau = prop.getProperty(Config._DefConfigEntryGlobusDoDcau);
	if ((dodcau != null) && (dodcau.equalsIgnoreCase("false"))) {
	    _doDcau = false;
	}
	TSRMLog.info(Config.class, null, "event=dcauInit", "value="+_doDcau);

	if (prop.getProperty(Config._DefConfigEntryGlobusParallelStream) != null) {
	    _nStream = new Integer (prop.getProperty(Config._DefConfigEntryGlobusParallelStream));
	    TSRMUtil.noNegative((_nStream.intValue() < 0), Config._DefConfigEntryGlobusParallelStream);
	}

	if (prop.getProperty(Config._DefConfigEntryGlobusBufferSizeMB) != null) {	    
	    _nBufferBytes = new Integer(1048576* Integer.parseInt(prop.getProperty(Config._DefConfigEntryGlobusBufferSizeMB)));
	    TSRMUtil.noNegative((_nBufferBytes.intValue() < 0), Config._DefConfigEntryGlobusBufferSizeMB);
	} else if (prop.getProperty(Config._DefConfigEntryGlobusBufferSize) != null) {
	    _nBufferBytes = new Integer (prop.getProperty(Config._DefConfigEntryGlobusBufferSize));
	    TSRMUtil.noNegative((_nBufferBytes.intValue() < 0), Config._DefConfigEntryGlobusBufferSize);
	}

	String maxWaitStr = prop.getProperty(Config._DefConfigEntryClientWaitParamsMaxWaitMillis);
	if (maxWaitStr != null) {
	    _gsiftpClientWaitParamsMaxWaitMillis = Integer.parseInt(maxWaitStr);
	    TSRMUtil.noNegative((_gsiftpClientWaitParamsMaxWaitMillis < 0), Config._DefConfigEntryClientWaitParamsMaxWaitMillis);
	}
	String waitDelayStr = prop.getProperty(Config._DefConfigEntryClientWaitParamsWaitDelayMillis);
	if (waitDelayStr != null) {
	    _gsiftpClientWaitParamsWaitDelayMillis = Integer.parseInt(waitDelayStr);
	    TSRMUtil.noNegative((_gsiftpClientWaitParamsWaitDelayMillis < 0), Config._DefConfigEntryClientWaitParamsWaitDelayMillis);
	}	
	
	_gucLocation = prop.getProperty(Config._DefConfigEntryGUCPATH);
    }

    private static void spaceMgtIsDisabled() {
	_disableSpaceMgt = true;
	TSRMUtil.startUpInfo("BeStMan: space mgt component is disabled.");

	if (!_usingCacheKeyword) {
	    TSRMUtil.startUpInfo("[Note:] "+Config._DefConfigEntryUsingCacheKeyword+" is set to true automatically when space mgt is disabled.");
	    _usingCacheKeyword = true;
	}
    }

    private static void handleComponentOptions(Properties prop) {
	if (isTrue(prop, Config._DefConfigEntryDisableSpaceMgt)) {
	    spaceMgtIsDisabled();
	}
	if (isTrue(prop, Config._DefConfigEntryDisableDirMgt)) {
	    _disableDirMgt = true;
	    TSRMUtil.startUpInfo("BeStMan: directory mgt component is disabled.");
	}

	if (isFalse(prop, Config._DefConfigEntryDisableLocalAuthorization)) {
	    _disableLocalAuthorization = false;
	    TSRMUtil.startUpInfo("BeStMan: local authorization component is enabled.");
	}

	if (isTrue(prop, Config._DefConfigEntryDisableRemoteCopy)) {	    
	    _disableRemoteCopy = true;
	    TSRMUtil.startUpInfo("BeStMan: calling srmCopy() is disabled.");
	}
    }

    private static void handleWsdlEndpoint(Properties prop) {	
	if (prop.getProperty(Config._DefConfigEntryFactoryID) != null) {
	    _id = prop.getProperty(Config._DefConfigEntryFactoryID).trim();
	}

	/*if (prop.getProperty(Config._DefConfigEntryPlugin) != null) {
	    _pluginDir = prop.getProperty(Config._DefConfigEntryPlugin).trim();
	    }*/
       
	try {
	    //_host = java.net.InetAddress.getLocalHost().getHostName();
	    _host = java.net.InetAddress.getLocalHost().getCanonicalHostName();
	    int ipAddresses = TSRMUtil.countIpAddresses();
	    //InetAddress[] addresses = InetAddress.getAllByName(InetAddress.getLocalHost().getHostName());
	    if (ipAddresses == 0) {
	      TSRMUtil.startUpInfo("## error, can not resolve addresses: "+_host+" "+InetAddress.getLocalHost().getHostName());
	      throw new RuntimeException("## error. failed to resolve address: "+_host);
	    }
	    TSRMUtil.startUpInfo("## found "+ipAddresses+" host addresses");
	    String assignedHostName = System.getProperty("GLOBUS_HOSTNAME");
	    TSRMUtil.startUpInfo("## found assigned hostname="+assignedHostName);
	    if (ipAddresses > 1) {
		if (assignedHostName== null) {
		    TSRMUtil.startUpInfo("## please supply preferred host name via -DGLOBUS_HOSTNAME");
		    throw new RuntimeException("##please supply preferred host name via -DGLOBUS_HOSTNAME");
		}
		_host = assignedHostName;
	    } else {// when the machine is configured to have multiple ips and/or multiple hostnames, 
	            // addresses only contains the ip(s) for the localhost name, not all the other hostnames
	            // I do not see another way to detect all the hostnames, so, have to use GLOBUS_HOSTNAME
	            // here so user has a way to assigne it. GLOBUS_HOSTNAME is chosen so globus container uses
	            // the same host name [for bestman/2.*]
	      if (assignedHostName != null) {
		_host = assignedHostName;
	      }
	    }
		
	    TSRMUtil.startUpInfo("## using local host: "+_host+" as web service host.");	    
	} catch (java.net.UnknownHostException e) {
	    //e.printStackTrace();
	    TSRMLog.exception(Config.class, "handleWsdlEndpoint()", e);
	    TSRMUtil.startUpInfo("## error, need a valid entry: Host for this web service. Exception"+e);
	    //return false;
	    //System.exit(1);
	    throw new RuntimeException("## error, need a valid entry: Host for this web service. Exception"+e);
	}

  

	if (prop.getProperty(Config._DefConfigEntrySecurePort) != null) {
	    _securePort = Integer.parseInt(prop.getProperty(Config._DefConfigEntrySecurePort));
	} else {
	    //prop.put(Config._DefConfigEntrySecurePort, String.valueOf(_securePort));
	}

	if (prop.getProperty(Config._DefConfigEntryProtocol) != null) {
	    String protocol = prop.getProperty(Config._DefConfigEntryProtocol);
	    if ((protocol.equals("httpg")) || (protocol.equals("https"))) {
		_DefContactProtocolStr = protocol;
	    } else {
		TSRMUtil.startUpInfo("Unsupported protocol. We only accept httpg/https so far.");
		//System.exit(1);
		throw new RuntimeException("Unsupported protocol. We only accept httpg/https so far.");
	    }
	} else {
	    TSRMUtil.startUpInfo(".........assuming "+_DefContactProtocolStr+": is the protocol to access SRM.");
	    //prop.put(Config._DefConfigEntryProtocol, _DefContactProtocolStr);
	}

	
	if (prop.getProperty(Config._DefConfigEntryPublicPort) != null) {
	    _publicPort = Integer.parseInt(prop.getProperty(Config._DefConfigEntryPublicPort));
	} else {
	    //prop.put(Config._DefConfigEntryPublicPort, String.valueOf(_publicPort));
	}      

	if (prop.getProperty(Config._DefConfigEntryGridMapFileName) != null) {	    
	    _gridMapFileName = prop.getProperty(Config._DefConfigEntryGridMapFileName).trim();
	} else {
	    TSRMUtil.startUpInfo("###### using default gridmap filename:"+_gridMapFileName);			       
	    //prop.put(Config._DefConfigEntryGridMapFileName, _gridMapFileName);	    
	}
	
	_proxyFileName   = prop.getProperty(Config._DefConfigEntryProxyFileName);
	_userKeyFileName = prop.getProperty(Config._DefConfigEntryKeyFileName);
	_userCertFileName = prop.getProperty(Config._DefConfigEntryCertFileName);
	
	String urlStr = _DefContactProtocolStr+"://"+_host+":"+_securePort+"/"+_serviceHandle;	    
	
	try {
	    _wsdlEndPoint = new URI(urlStr);
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Failed to init url:"+urlStr+" error:"+e.toString());
	    //System.exit(1);
	    throw new RuntimeException("Failed to init url:"+urlStr+" error:"+e.toString());
	}		

	String vomsValidation = prop.getProperty(Config._DefConfigEntryValidateVoms);
	if ((vomsValidation != null) && (vomsValidation.equalsIgnoreCase("true"))) {
		_doValidateVoms = true;
	}
	TSRMUtil.startUpInfo("validate voms proxy? ="+_doValidateVoms);
	_mappedIDHistory = new TSRMHistory(prop);
	if (prop.getProperty(Config._DefConfigEntryGUMSService) != null) {
	    loadGUMSLibraries(prop);
	}

	if (_gumsClient != null) {
	    return;
	}
	_gridMap = new GridMap();
	try {
	    _gridMap.load(_gridMapFileName);
	    TSRMUtil.startUpInfo("BeStMan is now using gridmap file: "+_gridMapFileName);
	    //TSRMUtil.startUpInfo("!!! Important: If this is your only gridmap file, make sure it is in server-config.wsdd as well.");
	} catch (Exception e) {
	    if (_gumsClient == null) {
		//e.printStackTrace();
		TSRMUtil.startUpInfo("Exiting. Failed to use gridmap file."+_gridMapFileName);
		//System.exit(1);
		throw  new RuntimeException("Failed to use gridmap file:"+_gridMapFileName+" "+e.toString());
	    }
	}
    }

    public static void handleLoadBalancingOption(Properties prop) {
	String protocolDef = prop.getProperty(Config._DefConfigEntryUserDefinedProtocols);
	TSRMTxfProtocol.setUserDefinedProtocols(protocolDef);
	
	String protocolOption = prop.getProperty(Config._DefConfigEntrySupportedProtocols);
	String policyOption = prop.getProperty(Config._DefConfigEntrySupportedProtocolPolicy);

	try {
	    TSRMTxfProtocol.parse(protocolOption);
	    TSRMTxfProtocol.applyUserPolicyList(policyOption);
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("Error: loading balancing option failed."+e.toString());
	    //System.exit(1);
	    throw new RuntimeException("loading balancing option failed."+e.toString());
	}     
    }

    private static void handleLocalAccessOption(Properties prop) {
	String blockedLocalPath = prop.getProperty(Config._DefConfigEntryLocalPathListBlock);
	try {
	    TSupportedURLWithFILE.parseBlocked(blockedLocalPath);
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Error: blocked local path failed."+e.toString());
	    //System.exit(1);
	    throw new RuntimeException("blocked local path failed."+e.toString());
	} 

	String allowedLocalPath = prop.getProperty(Config._DefConfigEntryLocalPathListAllow);
	try {
	    TSupportedURLWithFILE.parseAllowed(allowedLocalPath);	    
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Error: allowed local path failed."+e.toString());
	    //System.exit(1);
		throw new RuntimeException("allowed local path failed."+e.toString());
	}

	String authorizationClassOp = prop.getProperty(Config._DefConfigEntryUserFileAuthorization);
	try {
	    TSupportedURLWithFILE.setAuthorizationClass(authorizationClassOp);
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("Error: authorizationClass loading failure. detail="+authorizationClassOp);
	}
    }

    private static void handleStaticTokenOption(Properties prop) {
        String staticTokens = prop.getProperty(Config._DefConfigStaticTokens);
	if (staticTokens != null) {
	  _staticTokenList = StaticToken.fromInput(staticTokens.split(";"));	  
	} else {
	  TSRMUtil.startUpInfo("............ no static tokens defined for bestman");
	}
    }

    private static void handleUserDefinedTokenOptions(Properties prop) {
	String userTokens = prop.getProperty(Config._DefConfigEntryUserTokens);
	if (userTokens == null) {
	    return;
	}
	if (!TSRMUserSpaceTokenSupport.parse(userTokens)) {
	    TSRMUtil.startUpInfo("Error in parsing user defined tokens. Exiting.");
	    //System.exit(1);
	    throw new RuntimeException("Unable to parse user defined tokens!");
	}
	
	if (!isSpaceMgtDisabled()) {
	    TSRMUtil.startUpInfo("[Note:] space mgt is automatically disabled when user assigned space token is specified.");
	    spaceMgtIsDisabled();
	}
    }

    private static void handleMSSOption(Properties prop) {
	if (prop.getProperty(Config._DefConfigEntryMSSTimeOut) != null) {
	    _mssTimeOutMilliSeconds = Long.parseLong(prop.getProperty(Config._DefConfigEntryMSSTimeOut))*(long)1000;
	    TSRMUtil.noNegative((_mssTimeOutMilliSeconds < 0), Config._DefConfigEntryMSSTimeOut);
	} 
       
	//prop.put(Config._DefConfigEntryMSSTimeOut, String.valueOf(_mssTimeOutMilliSeconds/(long)1000));
    }

    private static void handleAccessControlOption(Properties prop) {
	if (prop.getProperty(TAccessControl._DefDefaultWorldPermission) != null) {
	    TAccessControl.setWorldPermission(TSRMPermission.fromString(prop.getProperty(TAccessControl._DefDefaultWorldPermission)));
	}
	//prop.put(TAccessControl._DefDefaultWorldPermission, TAccessControl._WorldPermission.toString());
    }

    private static void handleMiscOptions(Properties prop) {
	String usingCacheKeyword = prop.getProperty(Config._DefConfigEntryUsingCacheKeyword);
	if ((usingCacheKeyword != null) && (usingCacheKeyword.equalsIgnoreCase("true"))) {
	    _usingCacheKeyword = true;
	}
	//prop.put(Config._DefConfigEntryUsingCacheKeyword, ""+_usingCacheKeyword);

	String silentVal = prop.getProperty(Config._DefConfigEntrySilent);
	if ((silentVal != null) && (silentVal.equalsIgnoreCase("true"))) {
	    _silent = true;
	}

	String markupPingVal = prop.getProperty(Config._DefConfigEntryMarkupPingMsg);
	if ((markupPingVal != null) && (markupPingVal.equalsIgnoreCase("true"))) {
	    _doMarkupPingMsg = true;
	}
	//prop.put(Config._DefConfigEntryMarkupPingMsg, ""+_doMarkupPingMsg);

	if (prop.getProperty(Config._DefConfigEntryUploadQueueParameter) != null) {	    
	    _uploadQueueParameter = prop.getProperty(Config._DefConfigEntryUploadQueueParameter);	    
	}	

	if (isFalse(prop, Config._DefConfigEntryNoStopOnTimedOutThread)) {
	    _stopOnTimedOutThread = true;
	    TSRMUtil.startUpInfo("............. invoking: Thread.stop() on timed out threads");
	} else {
	    TSRMUtil.startUpInfo("............. invoking: Thread.interrupt() on timed out threads");
	}	

	if (prop.getProperty(Config._DefConfigEntryPlugin) != null) {
	    _pluginDir = prop.getProperty(Config._DefConfigEntryPlugin).trim();
	}
       
	String localFileAccessClassLoc = prop.getProperty(Config._DefConfigEntryLocalFileSystemAccess);
	if (localFileAccessClassLoc != null) {
	    String className = TSRMUtil.getValueOf(localFileAccessClassLoc, "class=", '&', false);
	    String jarName = TSRMUtil.getValueOf(localFileAccessClassLoc, "jarFile=", '&', false);

	    try {
		_localFileAccessClass = TSRMUtil.loadClass(Config._pluginDir+"/"+jarName,className);
	    } catch (Exception e) {
		//e.printStackTrace();
		TSRMUtil.startUpInfo("Error: failed to init local file access class"+className+" "+e.toString());
		//System.exit(1);
		throw new RuntimeException("failed to init local file access class"+className+" "+e.toString());
	    }	    
	}
	TSRMUtil.startUpInfo("....... file system will be accessed through class:"+_localFileAccessClass);

	_doComputeFileChecksum = isTrue(prop, Config._DefConfigEntryComputeChecksumOnFileLs);
	TSRMUtil.startUpInfo("....... ls(file) shows checksum?"+_doComputeFileChecksum);

	if (_doComputeFileChecksum) {
		_checksumCommand = prop.getProperty(Config._DefConfigEntryChecksumCommand);
		TSRMUtil.startUpInfo("....... checksum command: "+_checksumCommand);
	}
	
	String defaultChecksumVal = prop.getProperty(Config._DefConfigEntryDefaultChecksumType);
	if (defaultChecksumVal != null) {
	    _defaultChecksumType = Checksum.getType(defaultChecksumVal);
	    if (_defaultChecksumType< 0) {
		TSRMUtil.startUpInfo("Error: this checksum"+defaultChecksumVal+" is not supported.");
		//System.exit(1);
		throw new RuntimeException("This checksum type:"+defaultChecksumVal+" is not supported.");
	    }
    }
	
	if (_doComputeFileChecksum) {
		System.out.println("....... checksum type="+Checksum.display(_defaultChecksumType));
	}
	}


    private static void handleFileAccessOptions(Properties prop) {
	_accessFileSysUsingGSIFTP = isTrue(prop, Config._DefConfigEntryAccessFileSysUsingGSIFTP);
	_accessFileSysUsingSUDO = isTrue(prop, Config._DefConfigEntryAccessFileSysUsingSUDO);
	_noSudoOnLs = isTrue(prop, Config._DefConfigEntryNoSudoOnLs);

	_sudoCommand = prop.getProperty(Config._DefConfigEntrySudoCommand);
	if (_sudoCommand == null) {
	    _sudoCommand = "sudo";
	}
	System.out.println("sudo ="+_sudoCommand);

	if (_accessFileSysUsingGSIFTP) {
	    TSRMUtil.startUpInfo(".......local file system will be accessed through gsiftp");
	} else if (_accessFileSysUsingSUDO) {
	    TSRMUtil.startUpInfo(".......local file system will be accessed through sudo");
	    if (_noSudoOnLs){
		TSRMUtil.startUpInfo("       however, local _ls_ will not invoke sudo");
	    }
	    //String sudoTest = "sudo -v -S";
	    String sudoTest = _sudoCommand +" -v -S";
	    if (!TPlatformUtil.execCmdValidationWithReturn(sudoTest)) {
		TSRMUtil.startUpInfo("Failed to verify sudo command:"+sudoTest+" Exiting.");
		//System.exit(1);
	 throw new RuntimeException("Failed to verify sudo command:"+sudoTest);
	    } else {
		TSRMUtil.startUpInfo(sudoTest+" succeed.");
	    }
	} else {
	    TSRMUtil.startUpInfo(".......local file system will be accessed through srm server.");
	}
	TSRMFileListingOption.handleDirLsOff(prop);
    }
    public static void setLogLevel() {
	String logLevel = _prop.getProperty(Config._DefConfigEntryEventLogLevel);
	TSRMEventLog.setLogLevel(logLevel);
    }

    private static void loadGUMSLibraries(Properties prop) {
	String serviceUrl = prop.getProperty(Config._DefConfigEntryGUMSService);
	String hostDN = prop.getProperty(Config._DefConfigEntryGUMSCurrHostDN);	
	String gumsProtocol = prop.getProperty(Config._DefConfigEntryGUMSProtocol);

	boolean doXACML = false;
	if ((gumsProtocol == null) || (gumsProtocol.equalsIgnoreCase("AUTO"))) {
		int pos = serviceUrl.indexOf("XACML");
		if (pos > 0) {
			doXACML = true;
		}
	} else if (gumsProtocol.equalsIgnoreCase("XACML")) { 
		doXACML = true;
	} else if (!gumsProtocol.equalsIgnoreCase("SAML")) {
		TSRMUtil.startUpInfo("Unknown gums protocol:["+gumsProtocol+"]");
		//System.exit(1);
		throw new RuntimeException("Unknown gums protocol:["+gumsProtocol+"]");
	}	
	if (hostDN == null) {
	    String gumsCertFile = System.getProperties().getProperty("sslCertfile");
	    String gumsKeyFile = System.getProperties().getProperty("sslKey");
	    
	    if ((gumsCertFile == null) || (gumsKeyFile == null)) {
		TSRMUtil.startUpInfo(".. no gums  cert/key files are supplied to the server. using the server's:");
		gumsCertFile = Config._userCertFileName;
		gumsKeyFile = Config._userKeyFileName;
	    }
	    
	    org.globus.gsi.GlobusCredential cred = Server.getValidCred(gumsCertFile, gumsKeyFile);
	    if (cred != null) {		
		_gumsHostDN = cred.getIdentity();
	    } else {
		TSRMUtil.startUpInfo("Error: no host DN extracted. Can not start GUMS. Exiting.");
		//System.exit(1);
		throw new RuntimeException("No host DN extracted. Can not start GUMS.");
	    }
	} else {
	    _gumsHostDN = hostDN;
	}	
	TSRMUtil.startUpInfo(".. gums host dn"+_gumsHostDN);

	try {
	    if (doXACML) {
		    TSRMUtil.startUpInfo("initiating XACML to talk to GUMS");
		    _gumsClient = new GUMSXACMLClient(serviceUrl,_gumsHostDN);
	    } else {
		TSRMUtil.startUpInfo("initiating SAML to talk to GUMS");
		_gumsClient = new GUMSClientSAML(serviceUrl);
	    }
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Failed to contact GUMS service:"+serviceUrl+" Exiting.");
	    //System.exit(1);
	    throw new RuntimeException("Failed to contact GUMS service:"+serviceUrl+" Exiting.");
	}
    }

    public static boolean load(String configFileName) throws java.io.IOException {
	TSRMUtil.startUpInfoSilent("## reading configuration file: "+configFileName);

	Properties prop = getConfigProperties(configFileName);
	_prop = prop;

	handleMiscOptions(prop);

	assignLogs(prop);
	setLogLevel();

	handleFileAccessOptions(prop);

	TSRMHelp.listUnknownEntries(prop);
	
	handleWsdlEndpoint(prop);

	handleRetryOption(prop);

	handleLoadBalancingOption(prop);

	handleLocalAccessOption(prop);    
	
	TSRMLog.info(Config.class, null, "msg=\"LBL SRM Server starting \"", null);
	TSRMLog.info(Config.class, null, "\tjava.version  =",System.getProperty("java.version"));
	TSRMLog.info(Config.class, null, "\tjava.home     =",System.getProperty("java.home"));
	TSRMLog.info(Config.class, null, "\tworking dir   =",System.getProperty("user.dir"));
	TSRMLog.info(Config.class, null, "\tos name       =",System.getProperty("os.name"));
	TSRMLog.info(Config.class, null, "\tuser name       =",System.getProperty("user.name"));

/*
	if (prop.getProperty(Config._DefConfigEntryGUMSService) != null) {
	    loadGUMSLibraries(prop.getProperty(Config._DefConfigEntryGUMSService), prop.getProperty(Config._DefConfigEntryGUMSCurrHostDN));
	}
*/
	handleMSSOption(prop);

	handleAccessControlOption(prop);

	if (!Config._silent) {
	    prop.list(System.out);
	}
	TSRMUtil.startUpInfo("-- done with listing web service parameters --");	
	
	handleComponentOptions(prop);
	handleUserDefinedTokenOptions(prop);
	handleStaticTokenOption(prop);

	if (!TSRMStorage.init(prop)){
	    return false;
	}       

	handleGlobusOptions(prop);	

	//handleUserDefinedTokenOptions(prop);

	return true;
    }
    
    public static String getMappedID(String dn, String authorizationID) {
	if (_gumsClient == null) {
		TSRMLog.debug(Config.class, null, "event=checkDNHistory", null);
		String stored = getStored(dn, authorizationID);
		if (stored != null) {
	    	return stored;
		}
	}
	String result = null;
	if (Config._doValidateVoms) {
		java.security.cert.X509Certificate[] cred = null;
   		cred = TSRMService.gGetCertChain("tryValidation");
	    if (GUMSClientSAML.getVomsCert(cred) == null) {
			return null;
		}
	}

	if (_gumsClient!= null) {
	    result = getMappedIDUsingGUMS(dn);
	} else {
	    result = getMappedIDUsingGridmap(dn, authorizationID);
	}

	return result;
    }
    
    public static String getStored(String dn, String authorizationID) {
	return _mappedIDHistory.get(dn, authorizationID);
    }
	
    public static void storeID(String dn, String authorizationID, String result) {
	_mappedIDHistory.store(dn, authorizationID, result);
    }

    private static String getMappedIDUsingGUMS(String dn) {	       
	java.security.cert.X509Certificate[] cred = null;
	try {
	    cred = TSRMService.gGetCertChain("getGUMSMap");
	    return _gumsClient.getMappedID(dn, _gumsHostDN, cred);	  
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMLog.exception(Config.class, "gums mapping", e);	    
	    return null;	    
	}
    }

    private static String getMappedIDUsingGridmap(String dn, String authorizationID) {
	boolean loadOK = true;
	// always load gridmap file
	try {
	    _gridMap.load(_gridMapFileName);
	} catch (Exception e) {
	    e.printStackTrace();
	    //_gridMap = null;
		loadOK = false;
	}

	if (loadOK) {
	    String[] uids = _gridMap.getUserIDs(dn);

	    if ((uids == null) || (uids.length == 0)) {
		return null;
	    }
	    if (uids.length > 1) {
		TSRMLog.debug(Config.class, null, "event=getMappedID total="+uids.length, "first="+uids[0]+" last="+uids[uids.length-1]);
	    } else {
		TSRMLog.debug(Config.class, null, "event=getMappedID total="+uids.length, "uid="+uids[0]);
	    }
	    if (authorizationID != null) {
		for (int i=0; i<uids.length; i++) {
		    if (uids[i].equals(authorizationID)) {
			return authorizationID;
		    }
		}
		TSRMLog.error(Config.class, null, "event=getMappedID msg=\"No mapping found.\"", "authID="+authorizationID);
		return null;
	    } else {
		return uids[0];
	    }
	    //return _gridMap.getUserID(uid);
	} else {
	    return null;
	}
    }

    public static boolean loadSRM(String configFileName) {
	TSRMLog.info(Config.class, null, "event=READLOG_ends+", null);
	if (TSRMLog.getCacheLog() != null) {
	    if (!TSRMLog.getCacheLog().readLogEnds()) {
		return false;
	    }
	}
	TSRMLog.info(Config.class, null, "event=READLOG_ends-", null);

	try  {
	    TSRMGeneralEnforcement.loadFromFile(configFileName);	   
	    
	    TSRMUtil.startUpInfoSilent("== SRM is initialized == ");
	    return true;
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	    return false;
	}
    }
}

class TSRMLinkedHashMap extends LinkedHashMap {
    int _max=0;

    public TSRMLinkedHashMap(int limit) {
	if (limit > 0 ) {
	    _max = limit;
	}
    }
    protected boolean removeEldestEntry(Map.Entry eldest) {
	if (size() >= _max) {
	    return true;
	}
	return false;
    }
}

class TSRMHistory {
    int _entryLifetime = Config._defaultMappedIDLifetime; // -1 = never store any; 0 = always store, never expires
    TSRMLinkedHashMap _map = null;
    Mutex _mapGuard = null;
    int _limit = Config._defaultMaxMappedID;

    public TSRMHistory(Properties prop) {
        String lifetimeStr = prop.getProperty(Config._DefConfigEntryMappedIDRefreshInterval);
	if (lifetimeStr == null) {
	    return;
	}
	try {
	    _entryLifetime = Integer.parseInt(lifetimeStr);
	    if (_entryLifetime >= 0) {
		_mapGuard = new Mutex();
		String limitStr = prop.getProperty(Config._DefConfigEntryMappedIDStorageLimit);
		if (limitStr != null) {
		    int limitInput = Integer.parseInt(limitStr);
		    if (limitInput > 0) {
			_limit=limitInput;
		    }
		}
		TSRMLog.info(this.getClass(), null, "event=init", "limit="+_limit+" entrylifetime="+_entryLifetime);
		_map = new TSRMLinkedHashMap(_limit);
	    }
	} catch (Exception e) {
	    //e.printStackTrace();
	    TSRMUtil.startUpInfo("Error: failed to init the TSRMHistory class."+e.toString());
		throw new RuntimeException("failed to init the TSRMHistory class."+e.toString());
	    //System.exit(1);
	}
    } 

    public void store(String dn, String authorizationId, String result) {
	if (_map == null) {
	    return;
	}
	if (TSRMUtil.acquireSync(_mapGuard)) {
	    try {
		String key = getKey(dn, authorizationId);
		TSRMHistoryEntry entry =  TSRMHistoryEntry.create(key, result);	
		_map.put(key, entry);		 		
	    } finally {
		TSRMUtil.releaseSync(_mapGuard);
	    }
	}
    }

    private String getKey(String dn, String authorizationId) {
	String key = dn;
	if (authorizationId != null) {	          
	    key = dn+"+"+authorizationId;
	}
	return key;
    }

    public String get(String dn, String authorizationId) {
	if (_map == null) {
	    return null;
	}
	if (TSRMUtil.acquireSync(_mapGuard)) {
	    try {
		String key = getKey(dn, authorizationId);
		TSRMHistoryEntry result = (TSRMHistoryEntry)(_map.get(key));
		if (result == null) {
		    return null;
		}
		
		if (result.isValid(_entryLifetime)) {		    
		    return result.getTarget();
		}
		_map.remove(key);
		return null;
	    } finally {
		TSRMUtil.releaseSync(_mapGuard);
	    }
	}
	return null;
    }

}

class TSRMHistoryEntry {
    long _timeStamp;
    String _id;
    String _tgt;

    private TSRMHistoryEntry(String id, String tgt) {
	_timeStamp = System.currentTimeMillis();
	_id = id;
	_tgt = tgt;
    }

    public static TSRMHistoryEntry create(String id, String tgt) {
	return new TSRMHistoryEntry(id,tgt);
    }

    public boolean isValid(int lifetimeSeconds) {
	if (lifetimeSeconds == 0) {
	    return true;
	}
	long now = System.currentTimeMillis();
	long dur = (now - _timeStamp)/1000;
	return (dur < lifetimeSeconds);
    }

    public String getTarget() {
	return _tgt;
    }

}
