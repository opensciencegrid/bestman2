/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2010-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import java.util.List;
import java.util.Date;
import java.util.Set;
import java.util.Properties;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.io.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

import javax.swing.JFrame;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.globus.util.Util;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.bc.*;
import java.security.interfaces.*;
import java.security.PrivateKey;
import java.security.cert.*;

import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.exception.*;
import gov.lbl.adapt.srm.client.wsdl.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMClientN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientN implements SRMClientIntf
{
    //public static gov.lbl.adapt.adt.TTxfHandler _adtTxf = new gov.lbl.adapt.adt.TTxfHandler();
    public static SRMClientN _staticClient = null;

    //public static SRMPassiveTransferMonitor _gPTM = new SRMPassiveTransferMonitor();
    public static IPassiveTransferMonitor _gPTM = null;
    public static IPassiveTransferMonitor getPTM() { return _gPTM;}
    public static IPassiveTransferMonitor loadPTM() {
	try { 
	    if (_gPTM == null) {
		ClassLoader cl = gov.lbl.adapt.srm.client.main.SRMClientN.class.getClassLoader();
		Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.SRMPassiveTransferMonitor");
		java.lang.Class[] constructorInput = null;
		Object[] input = null;
		java.lang.reflect.Constructor cr = c.getConstructor(constructorInput); 	    
		_gPTM = (IPassiveTransferMonitor)(cr.newInstance(input));
	    }
	    return _gPTM;
	} catch (Exception e) {
	    //e.printStackTrace();
	    System.out.println("FYI: == no PTM moduleis detected, skipping PTM interaction== ");
	    return null;
	}
    }

    public static void doExit(String tag, int code) {
	try {
	    if (gov.lbl.adapt.srm.client.main.SRMClientN._staticClient._atm != null) {
		gov.lbl.adapt.srm.client.main.SRMClientN._staticClient._atm.completed(); 
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
	logMsg("StatusExit. StatusCode="+code, tag, null);
	System.exit(code);
    }

    public static void logEx(String tag, Exception ex) {	
	util.printEventLogException(_theLogger, tag, ex);
    }

    public static void logMsg(String msg, String logHeader, String clientHeader) {
	if (clientHeader != null) {
	    util.printMessage(clientHeader + msg, logger,silent);
	    util.printMessage(clientHeader + msg, pIntf);
	}
	if (logHeader != null) {
	    inputVec.clear();
	    inputVec.addElement(msg);
	    util.printEventLog(_theLogger, logHeader, inputVec,silent,useLog);
	    inputVec.clear();
	}
    }

    public  static void logMsg(String msg[], String logHeader, String clientHeader) {	
	inputVec.clear();
	for (int i=0; i<msg.length; i++) {
	    if (clientHeader != null) {
		util.printMessage(clientHeader+msg[i], logger,silent);
		util.printMessage(clientHeader+msg[i], pIntf);
	    }
	    inputVec.addElement(msg[i]);
	}
	if (logHeader != null) {
	    util.printEventLog(_theLogger, logHeader, inputVec,silent,useLog);
	}
	inputVec.clear();
    }

private GSSCredential mycred;
private XMLParseConfig pConfig = new XMLParseConfig();

private Properties properties = new Properties();
private String configFileLocation = "";
private String inputFile="";
private String targetDir="";
private String _password ="";
private String userKey="";
private String userDesc="";
private String userCert="";
private String proxyFile="";
private String protocolsList="";
public int concurrency = 1; 
    //private int parallelism = 1; 
public int parallelism = 1; 
//private int bufferSize = 1048576;
private int bufferSize = 0;
private int blockSize = 0;
//private int statusWaitTime=30;
private int statusWaitTime=10;  //changed for the exponential polling
private int threshHoldValue = 600; //default: 600 seconds
private String resultStatus="";
private String resultExplanation="";
private int statusMaxTimeAllowed=10800;
private int connectionTimeOutAllowed=600;
private int setHTTPConnectionTimeOutAllowed=600;
private boolean gotConnectionTimeOut;
private boolean gotHTTPConnectionTimeOut;
private boolean statusWaitTimeGiven=false;
private boolean statusMaxTimeGiven=false;
private boolean spaceTokenGiven=false;
private boolean serviceURLGiven=false;
private boolean noWaitOnRemotePut=false;
private boolean dcau=true;
private boolean checkDiskSpace=false;
private boolean nooverwrite = false;
private boolean isRenew = false;
private boolean esgReportType=false;
private boolean doBringOnline=false;
private boolean doCopy=false;
private SRMClientFileTransferN fileTransferNGUI;
private Vector srmSurlVec=new Vector();
private String sourceUrl="";
private String targetUrl="";
private String serviceUrl="";
private String serviceURL="";
private String storageInfo="";
private String remoteTransferInfo="";
private String uid="";
private boolean doReserveSpace=false;
private boolean doReleaseSpace=false;
private boolean alsoReleaseFile=true;
private boolean noAbortFile=false;
private static Vector inputVec = new Vector();
private boolean doReleaseFile=false;
private boolean keepSpace = true;
private int startedProcess;
private int completedProcess;
private boolean requestDone=false;
private Request request;
private String requestType="";
private int totalFiles;
private int proxyType;
private int numRetry=3; //default
private int retryTimeOut=60; //default 60 seconds
private char tokenType; 
private TFileStorageType fileStorageType = null;
private TRetentionPolicyInfo retentionPolicyInfo;
private String retentionPolicyStr = "";
private String accessLatency = "";
private int tokenLifetime = 0; 
private int totalRequestTime  = 0;
private int pinLifeTime  = 0;
private int fileLifeTime = 0;
private long tokenSize = 0;
private long gSize = 0;
private String fileToken;
private String requestToken;
private String log4jlocation="";
private String logPath="";
private String eventLogPath="";
private String gucEventLogPath="";
private String outputFile="";
private boolean gucAsked;
private boolean textReport=true;
private static boolean silent=false;
private static boolean useLog=false;
private boolean gotNumRetry=false;
private boolean gotParallelism=false;
private boolean gotRetryTimeOut=false;
private boolean gotBufferSize=false;
private boolean gotBlockSize = false;
private boolean gotDcau=false;
private boolean pushMode=false;
private boolean partycopy=false;
private static PrintIntf pIntf;

private static Log logger;
private static java.util.logging.Logger _theLogger =
    java.util.logging.Logger.getLogger(gov.lbl.adapt.srm.client.main.SRMClientN.class.getName());
    
private java.util.logging.FileHandler _fh;
private boolean overrideserviceurl=false;
private boolean remotePutCase=false;
private String statusToken="";
private boolean doStatus=false;
private int maxFilesPerRequest=0;
private int totalFilesPerRequest;
private int totalSubRequest;
private Vector ridList = new Vector ();
private boolean submitOnly=false;
private boolean adjustTURL=false;
private boolean gateWayModeEnabled=false;

private SRMWSDLIntf srmCopyClient;

private TransferThread tThread;
private boolean _debug=false;
private boolean noDownLoad=false;
private boolean simpleSRMCOPY=false;

private boolean directGsiFTP=false;
private boolean mayBeDirectGsiFTP=false;
private boolean recursive=false;
private boolean gotGUCScriptPath=false;
private int servicePortNumber = 0;
private String serviceHandle="";
private String gucScriptPath="";
private int numLevels=0;  
private String delegationNeeded="";
private boolean domkdir=false;
private boolean timedOut=false;
private boolean allLocalFileExists;
private boolean checkPing;

public gov.lbl.adapt.atm.TATMImpl _atm = null;
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public long parseNonNegLong(String input, long defaultV, String inputName) 
{
    long result = 0;
    try {
	result = Long.parseLong(input);
	if (result > 0) {
	    return result;
	}
    } catch (NumberFormatException nfe) {
	//nfe.printStackTrace();
	logMsg("Invalid int for: "+inputName, null, "\nSRM-CLIENT:");
	if (defaultV < 0) {
	    System.exit(1);	
	}
    }
    if (result < 0) {
	logMsg("Invalid to have negative input:"+result+" for: "+inputName, null, "\nSRM-CLIENT:");
	System.exit(1);
    }
    logMsg("Using default value "+defaultV+" for: "+inputName, null, "\nSRM-CLIENT");
    return defaultV;
}

public int checkIntSysConfig(Properties sys_config, String entryName, int defaultV) {
    String temp = (String) sys_config.get(entryName);
    return parseNonNegInt(temp, defaultV, "(sys_config)"+entryName);
} 

public int parseNonNegInt(String input, int defaultV, String inputName) 
{
    int result = 0;
    try {
	result = Integer.parseInt(input);
	if (result > 0) {
	    return result;
	}
    } catch (NumberFormatException nfe) {
	//nfe.printStackTrace();
	logMsg("Invalid int for: "+inputName, null, "\nSRM-CLIENT:");
	if (defaultV < 0) {
	    System.exit(1);	
	} 
    }
    if (result < 0) {
	logMsg("Invalid to have negative input:"+result+" for: "+inputName, null, "\nSRM-CLIENT:");
	System.exit(1);
    }
    logMsg("Using default value "+defaultV+" for: "+inputName, null, "\nSRM-CLIENT");
    return defaultV;
}
public SRMClientN(String[] args, PrintIntf pIntf) {
    String tag = "SRMClientN ";
    this.pIntf = pIntf;
    
    for(int i = 0; i < args.length; i++) {
	boolean ok = false;
	
	if (i == 0 && !args[i].startsWith("-")) {
	    sourceUrl=args[i];
	    ok = true;       
	} else if(i == 1 && !args[i].startsWith("-")) {
	    targetUrl=args[i];
	    ok = true;
	    simpleSRMCOPY=true;	
	} else if (args[i].equalsIgnoreCase("-pm")) {
	    _atm = new gov.lbl.adapt.atm.TATMImpl(args[i+1]);
	    _atm.getCurrSnapshot();
	    i++;
	} else if(args[i].equalsIgnoreCase("-recursive")) {
	    recursive = true;	
	} else if(args[i].equalsIgnoreCase("-nowaitonremoteput")) {
	    noWaitOnRemotePut = true;	
	} else if(args[i].equalsIgnoreCase("-mkdir")) {
	    domkdir = true;	
	} else if(args[i].equalsIgnoreCase("-lite")) {
	    ;	
	} else if(args[i].equalsIgnoreCase("-protocolslist") && i+1 < args.length) {
	    protocolsList = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-conf") && i+1 < args.length) {
	    configFileLocation = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-numlevels") && i+1 < args.length) {
	    numLevels = parseNonNegInt(args[i+1], 0, "-numlevels");
	    i++;
	} else if(args[i].equalsIgnoreCase("-nogui")) {
	    ;	
	} else if(args[i].equalsIgnoreCase("-reservespace") || args[i].startsWith("-reserveSpace")) {		  
	    doReserveSpace=true;	
	} else if(args[i].equalsIgnoreCase("-gatewayfriendly")) {
	    checkPing=true;	
	} else if(args[i].equalsIgnoreCase("-direct")) {
	    directGsiFTP=true;	
	} else if(args[i].equalsIgnoreCase("-remotetransferinfo") && i+1 < args.length) {		  
	    remoteTransferInfo = args[i+1];
	    i++; 	
	} else if(args[i].equalsIgnoreCase("-delegation")) {
	    if(i+1 < args.length) {
		delegationNeeded = ""+true;
		i++;	    
	    } else {
		delegationNeeded = ""+true;
	    }	
	} else if(args[i].equalsIgnoreCase("-storageinfo")) {
	    if(i+1 < args.length) {
		if(args[i+1].startsWith("for")) {
		    storageInfo = args[i+1];
		    i++;		
		} else {
		    storageInfo = ""+true;
		}	    
	    } else {
		storageInfo=""+true;
	    }	    
	} else if(args[i].equalsIgnoreCase("-threshold") && i+1 < args.length) {
	    String temp = args[i+1];
	    threshHoldValue = parseNonNegInt(args[i+1], threshHoldValue, "-threshold");
	    i++;    
	} else if(args[i].equalsIgnoreCase("-statuswaittime") && i+1 < args.length) {
	    statusWaitTimeGiven=true;
	    statusWaitTime = parseNonNegInt(args[i+1], statusWaitTime, "-statuswaittime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-statusmaxtime") && i+1 < args.length) {
	    String temp = args[i+1];
	    statusMaxTimeGiven=true;
	    statusMaxTimeAllowed = parseNonNegInt(args[i+1], statusMaxTimeAllowed, "-statusmaxtime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-copy") || args[i].startsWith("-copy")) {	    
	    doCopy=true;	
	} else if(args[i].equalsIgnoreCase("-nodownload")) {
	    noDownLoad=true;	
	} else if(args[i].equalsIgnoreCase("-bringonline") || args[i].startsWith("-bringOnline")) {		  
	    doBringOnline=true;	
	} else if(args[i].equalsIgnoreCase("-pushmode")) {
	    pushMode=true;	    
	} else if(args[i].equalsIgnoreCase("-3partycopy")) {
	    partycopy=true;	
	} else if(args[i].equalsIgnoreCase("-releasespace") || (args[i].equalsIgnoreCase("-releasespace"))) {		  
	    doReleaseSpace=true;	
	} else if(args[i].equalsIgnoreCase("-releasefile")) {
	    doReleaseFile=true;
	    alsoReleaseFile=true;	
	} else if(args[i].equalsIgnoreCase("-noreleasefile")) {
	    alsoReleaseFile=false;	
	} else if(args[i].equalsIgnoreCase("-noabortonfail")) {
	    noAbortFile=true;	
	} else if(args[i].equalsIgnoreCase("-requesttoken") && i+1 < args.length) {
	    requestToken=args[i+1];
	    i++;
	}
	/*
	  else if(args[i].equalsIgnoreCase("-keepspace")) {
	  keepSpace = true;
	  }
	*/
	else if(args[i].equalsIgnoreCase("-spacetoken") && i+1 < args.length) {
	    spaceTokenGiven=true;
	    fileToken=args[i+1];
	    i++;	
	} else if(args[i].startsWith("-space_token=")) {
	    spaceTokenGiven=true;
	    int idx = args[i].indexOf("=");
	    if(idx != -1) {
		fileToken=args[i].substring(idx+1);
	    }	
	} else if(args[i].equalsIgnoreCase("-requesttype") && i+1 < args.length) {
	    doStatus = true;
	    requestType = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-requesttoken") && i+1 < args.length) {
	    statusToken = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-serviceurl") && i+1 < args.length) {
	    serviceUrl = args[i+1];
	    serviceURL = args[i+1];
	    serviceURLGiven=true;
	    i++;	
	} else if(args[i].equalsIgnoreCase("-submitonly")) {
	    submitOnly=true;	
	} else if(args[i].equalsIgnoreCase("-log") && i+1 < args.length) {
	    useLog = true;
	    eventLogPath = args[i+1];
	    gucEventLogPath = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-report") && i+1 < args.length) {
	    outputFile = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-xmlreport") && i+1 < args.length) {
	    outputFile = args[i+1];
	    esgReportType=true;
	    i++;
	}
	/*
	  else if(args[i].equalsIgnoreCase("-textreport")) {
	  textReport=true;
	  }
	*/
	else if(args[i].equalsIgnoreCase("-authid") && i+1 < args.length) {
	    uid = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-checkdiskspace")) {
	    checkDiskSpace=true;	
	} else if(args[i].equalsIgnoreCase("-filestoragetype") && i+1 < args.length) {
	    String temp = args[i+1];
	    if(temp.toUpperCase().startsWith("P")) {
		tokenType = 'p';
	    }
	    else if(temp.toUpperCase().startsWith("D")) {
		tokenType = 'd';
	    }
	    else if(temp.toUpperCase().startsWith("V")) {
		tokenType = 'v';
	    }
	    i++;	
	} else if(args[i].equalsIgnoreCase("-retentionpolicy") && i+1 < args.length) {
	    retentionPolicyStr = args[i+1].toUpperCase();
	    i++;	
	} else if(args[i].startsWith("-retention_policy=")) {
	    int idx = args[i].indexOf("=");
	    if(idx != -1) {
		retentionPolicyStr = args[i].substring(idx+1);
	    }	
	} else if(args[i].equalsIgnoreCase("-accesslatency") && i+1 < args.length) {
	    accessLatency = args[i+1].toUpperCase();
	    i++;	
	} else if(args[i].startsWith("-access_latency=")) {
	    int idx = args[i].indexOf("=");
	    if(idx != -1) {
		accessLatency = args[i].substring(idx+1);
	    }	
	} else if(args[i].equalsIgnoreCase("-totalrequesttime") && i+1 < args.length) {
	    statusMaxTimeGiven=true;
	    statusMaxTimeAllowed = parseNonNegInt(args[i+1], statusMaxTimeAllowed, "-totalrequsttime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-pinlifetime") && i+1 < args.length) {
	    pinLifeTime = parseNonNegInt(args[i+1], pinLifeTime, "-pinlifetime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-filelifetime") && i+1 < args.length) {
	    fileLifeTime = parseNonNegInt(args[i+1], fileLifeTime, "-filelifetime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-spacelifetime") && i+1 < args.length) {
	    tokenLifetime = parseNonNegInt(args[i+1], tokenLifetime, "-spacelifetime");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-spacesize") && i+1 < args.length) {
	    tokenSize = parseNonNegLong(args[i+1], tokenSize, "-spacesize");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-spacegsize") && i+1 < args.length) {
	    gSize = parseNonNegLong(args[i+1], gSize, "-spacegsize");
	    i++;
	} else if(args[i].equalsIgnoreCase("-f") && i+1 < args.length) {
	    inputFile = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-maxfilesperrequest") && i+1 < args.length) { 		  
	    maxFilesPerRequest = parseNonNegInt(args[i+1], maxFilesPerRequest, "-maxfilesperrequest");
	    i++;	
	} else if(args[i].equalsIgnoreCase("-s") && i+1 < args.length) {
	    sourceUrl = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-t") && i+1 < args.length) {
	    targetUrl = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-td") && i+1 < args.length) {
	    targetDir = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-userdesc") && i+1 < args.length) {
	    userDesc = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-userkey") && i+1 < args.length) {
	    userKey = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-usercert") && i+1 < args.length) {
	    userCert = args[i+1];
	    i++;	
	} else if(args[i].equalsIgnoreCase("-proxyfile") && i+1 < args.length) {
	    proxyFile = args[i+1];
	    i++;
	} else if (args[i].equalsIgnoreCase("-adtStep") && i+1 < args.length) {
	    gov.lbl.adapt.adt.TManagerADT._gStep = parseNonNegInt(args[i+1], 1, "adtStep");
	    i++;
	} else if (args[i].equalsIgnoreCase("-adtStart") && i+1 < args.length) {
	    gov.lbl.adapt.adt.TManagerADT._gStart = parseNonNegInt(args[i+1], 1, "adtStart");
	    i++;
	} else if(args[i].equalsIgnoreCase("-concurrency") && i+1 < args.length) {
	    concurrency = parseNonNegInt(args[i+1], concurrency, "-concurrency");
	    i++;
	} else if(args[i].equalsIgnoreCase("-parallelism") && i+1 < args.length) {
	    parallelism = parseNonNegInt(args[i+1], parallelism, "-parallelism");
	    i++;
	} else if(args[i].equalsIgnoreCase("-debug")) {
	    _debug=true;	
	} else if(args[i].equalsIgnoreCase("-quiet")) {
	    silent=true;	
	} else if(args[i].equalsIgnoreCase("-numretry") && i+1 < args.length) {
	    numRetry = parseNonNegInt(args[i+1], numRetry, "-numretry");
	    gotNumRetry=true;	
	    i++;
	} else if(args[i].startsWith("-retry_num=")) {
	    int idx = args[i].indexOf("=");
	    if(idx != -1) {
		String temp = args[i].substring(idx+1);
		numRetry = parseNonNegInt(temp, numRetry, "-retry_num=");
	    }	
	} else if(args[i].equalsIgnoreCase("-retrydelay") && i+1 < args.length) {
	    retryTimeOut=parseNonNegInt(args[i+1], retryTimeOut, "-retrydelay");
	    gotRetryTimeOut=true;	
	    i++;
	} else if(args[i].equalsIgnoreCase("-sethttptimeout") && i+1 < args.length) {
	    setHTTPConnectionTimeOutAllowed = parseNonNegInt(args[i+1], setHTTPConnectionTimeOutAllowed, "-sethttptimeout");
	    i++;
	    gotHTTPConnectionTimeOut=true;	
	} else if(args[i].equalsIgnoreCase("-connectiontimeout") && i+1 < args.length) {
	    connectionTimeOutAllowed = parseNonNegInt(args[i+1], connectionTimeOutAllowed, "-connectiontimeout");
	    i++;
	    gotConnectionTimeOut=true;	
	} else if(args[i].equalsIgnoreCase("-gurlcopy") ||args[i].equalsIgnoreCase("-gucpath")) {
	    gucAsked=true;
	    if(i+1 < args.length) {
		if(!args[i+1].startsWith("-")) {
		    gucScriptPath = args[i+1];
		    gotGUCScriptPath=true;
		    i++;
		}
	    }	
	} else if(args[i].equalsIgnoreCase("-buffersize") && i+1 < args.length) {
	    bufferSize = parseNonNegInt(args[i+1], bufferSize, "-buffersize");
	    i++;
	    gotBufferSize=true;	
	} else if(args[i].equalsIgnoreCase("-blocksize") && i+1 < args.length) {	  
	    blockSize = parseNonNegInt(args[i+1], blockSize, "-blocksize");
	    i++;
	    gotBlockSize=true;	
	} else if(args[i].equalsIgnoreCase("-dcau") && i+1 < args.length) {
	    Boolean b = new Boolean(args[i+1]);
	    dcau = b.booleanValue();
	    gotDcau=true;
	    i++;	
	} else if(args[i].equalsIgnoreCase("-renewproxy")) {
	    isRenew = true;	
	} else if(args[i].equalsIgnoreCase("-adjustturl")) {
	    adjustTURL = true;	
	} else if(args[i].equalsIgnoreCase("-nooverwrite")) {
	    nooverwrite = true;	
	} else if(args[i].equalsIgnoreCase("-v2")) {
	    ;	
	} else if(args[i].equalsIgnoreCase("-version")) {
	    SRMClientN.printVersion();	
	} else if(args[i].equalsIgnoreCase("-help")) {
	    showUsage(true);	
	} else {
	    boolean b = gov.lbl.adapt.srm.client.util.Util.parseSrmCpCommands(args[i],0);
	    if(b) ;
	    else {
		if(!ok) {
		    showUsage (true);
		}
	    } 
	}
    }
}

    private void initDoRelease() throws Exception {
	String[] surl = new String[1];
	String[] turl = new String[1];

	surl[0] = sourceUrl;
	turl[0] = "";

	if(sourceUrl.equals("") && requestToken == null) {
	    logMsg("\nSRM-CLIENT: ..............................", null, "");
	    logMsg("SRM-CLIENT: Releasefile options are following", null, "");
	    logMsg("\n\tPlease provide -s <sourceUrl> \n", null, "");
	    logMsg("\tor -f <inputfile>\n", null, "");		    
	    logMsg("\n\tor Please provide -requesttoken <requesttoken> \n", null, "");
	    logMsg("\tand -serviceurl <serviceurl>\n", null, "");
	    logMsg("\n\tor Please provide -requesttoken <requesttoken> \n", null, "");
	    logMsg("\tand -s <sourceurl>\n", null, "");
	    showUsage(false);
	    if(pIntf != null) return;
	}
	if(sourceUrl.equals("") && serviceUrl.equals("")) {
	    logMsg("\n\tor Please provide -requesttoken <requesttoken> \n", null, "");
	    logMsg("\tand -serviceurl <serviceurl>\n", null, "");
	    showUsage(false);
	    if(pIntf != null) return;		
	} else if(!sourceUrl.equals("")) {
	    request = createRequest(surl[0],turl[0], "");            
	} else {
	    request = new Request();
	}
	findRequestType(surl[0],turl[0],"");
	request.setModeType(requestType);	    
}

private void initEnv(String tag, Properties sys_config) throws Exception {
    try {
	if(!configFileLocation.equals("")) {
	    File f = new File(configFileLocation);
	    if(!f.exists()) {
		System.out.println("\nUser Config file does not exists " +
				   configFileLocation);
		showUsage(false);
	    }
	    if(_debug) {
		System.out.println("\nParsing config file (1)" + configFileLocation);
	    } 
	    sys_config = gov.lbl.adapt.srm.client.util.Util.parsefile(configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
	}
    }catch(Exception e) {
	util.printEventLogException(_theLogger,"",e);
	System.out.println("SRM-CLIENT: Exception from client="+e.getMessage());
	//e.printStackTrace();
	util.printEventLogException(_theLogger,"parsefile",e);
	showUsage(false);
    }

    if(recursive) {
	if(targetDir.equals("")) {
	    System.out.println("Please use -td for recursive option");
	    doExit(tag, 1);
	}
    }
    
    String ttemp = System.getProperty("SRM.HOME");    
    if(ttemp != null && !ttemp.equals("")) {
	configFileLocation = ttemp+"/conf/srmclient.conf";
	if(gucAsked && !gotGUCScriptPath) {
	    gucScriptPath = ttemp+"/bin/g-urlcopy.sh";
	}
	try {
	    File f = new File(configFileLocation);
	    if(f.exists()) {
		if(_debug) {
		    System.out.println("\nParsing config file (2)" + configFileLocation);
		}
		sys_config = gov.lbl.adapt.srm.client.util.Util.parsefile(configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
	    }
	} catch(Exception e) {
	    util.printEventLogException(_theLogger,"",e);
	    System.out.println("SRM-CLIENT: Exception from client="+e.getMessage());
	    util.printEventLogException(_theLogger,"parsefile",e);
	    //e.printStackTrace();
	    showUsage(false);
	}
    }
    if(proxyFile.equals("")) {
	Properties props = System.getProperties();
	Enumeration ee = props.propertyNames();
	while (ee.hasMoreElements()) {
	    String str = (String) ee.nextElement();
	    if(str.trim().equals("X509_USER_PROXY")) {
		ttemp = props.getProperty(str.trim());
		if(ttemp != null) {
		    proxyFile=ttemp;
		}
	    }
	    //System.out.println(str);
	}
    }
    
    String detailedLogDate=util.getDetailedDate();
    
    if(gucAsked && gucEventLogPath.equals("")) {
	String temp = (String) sys_config.get("eventlogpath");
	if(temp != null) {
	    gucEventLogPath=temp+"-guc";	
	} else {
	    gucEventLogPath = "./srmclient-event-guc-"+detailedLogDate+".log";
	}
    }
    
    if(silent || useLog) {
	if(eventLogPath.equals("")) {
	    String temp = (String) sys_config.get("eventlogpath");
	    if(temp != null) {
		eventLogPath = temp;
		gucEventLogPath=temp+"-guc";	    
	    } else {
		eventLogPath = "./srmclient-event-"+detailedLogDate+".log";
		gucEventLogPath = "./srmclient-event-guc-"+detailedLogDate+".log";
	    }
	}
	
	try {
	    _fh = new java.util.logging.FileHandler(eventLogPath);
	    _fh.setFormatter(new NetLoggerFormatter());
	    _theLogger.addHandler(_fh);
	    _theLogger.setLevel(java.util.logging.Level.ALL);
	    
	    File f = new File(eventLogPath+".lck");
	    if(f.exists()) {
		f.delete();
	    }
	}catch(Exception e) {
	    util.printEventLogException(_theLogger,"",e);
	    System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
	    util.printEventLogException(_theLogger,"event log",e);
	}
    }//end(if silent)
    
    ttemp = System.getProperty("log4j.configuration");    
    if(ttemp != null && !ttemp.equals("")) {
	log4jlocation = ttemp;
    }
    
    if(_debug) {
	if(!logPath.equals("")) {
	    logMsg("Log4jlocation " + log4jlocation, tag, "");
	}
    }
    
    try {
	PropertyConfigurator.configure(log4jlocation);
    }catch(Exception ee){}
    
    
    //default values such as "Enter a Value"
    properties.put("user-cert", pConfig.getUserCert());
    properties.put("user-key", pConfig.getUserKey());
    properties.put("proxy-file", pConfig.getProxyFile());

	if(!userKey.equals("")) {
	    pConfig.setUserKey(userKey);
	    properties.put("user-key",userKey);	
	} else {
	    String temp =  (String) sys_config.get("UserKey");
	    if(temp != null) {
		userKey = temp;
		pConfig.setUserKey(userKey);
		properties.put("user-key",userKey);
	    }
	}

	if(!userCert.equals("")) {
	    pConfig.setUserCert(userCert);
	    properties.put("user-cert", userCert);	
	} else {
	    String temp =  (String) sys_config.get("UserCert");
	    if(temp != null) {
		userCert = temp;
		pConfig.setUserKey(userCert);
		properties.put("user-cert",userCert);
	    }
	}

	if(!proxyFile.equals("")) {
	    pConfig.setProxyFile(proxyFile);
	    properties.put("proxy-file", proxyFile);
	}
	else {
	    String temp =  (String) sys_config.get("ProxyFile");
	    if(temp != null) {
		proxyFile = temp;
		pConfig.setUserKey(proxyFile);
		properties.put("proxy-file",proxyFile);
	    }
	}
	
	if(!gotGUCScriptPath) {
	    String temp = (String) sys_config.get("GUCPath");
	    if(temp != null) {
		gucScriptPath = temp;		    
	    }
	}
	
	if(!gotGUCScriptPath) {
	    String temp = (String) sys_config.get("GURLCopy");
	    if(temp != null) {
		gucScriptPath = temp;		    
	    }
	}
	
	if(!gotBufferSize) {
	    bufferSize = checkIntSysConfig(sys_config, "BufferSize", bufferSize);
	}
	
	if(!gotBlockSize) {
	    blockSize = checkIntSysConfig(sys_config, "BlockSize", bufferSize);
	}
	
	if(!gotDcau) {
	    String temp = (String) sys_config.get("dcau");
	    if(temp != null) {
		Boolean b = new Boolean(temp);
		dcau = b.booleanValue();
	    } 
	}
	
	if(!gotParallelism) {
	    parallelism = checkIntSysConfig(sys_config, "Parallelism", parallelism);
	}
	
	if(!gotNumRetry) {
	    numRetry = checkIntSysConfig(sys_config, "NumRetry", numRetry);
	}
	
	servicePortNumber = checkIntSysConfig(sys_config, "ServicePortNumber", servicePortNumber);
	
	String xxtemp = (String) sys_config.get("ServiceHandle");
	if(xxtemp != null) {
	    serviceHandle = xxtemp;
	}
	
	if(!gotRetryTimeOut) {
	    retryTimeOut = checkIntSysConfig(sys_config, "RetryDelay", retryTimeOut);
	}           

	if(!gotHTTPConnectionTimeOut) {
	    setHTTPConnectionTimeOutAllowed = checkIntSysConfig(sys_config, "SetHTTPConnectionTimeOut", setHTTPConnectionTimeOutAllowed);
	}
	
	if(!gotConnectionTimeOut) {
	    connectionTimeOutAllowed = checkIntSysConfig(sys_config, "ConnectionTimeOut", connectionTimeOutAllowed);
	}
	
	if(!statusWaitTimeGiven) {
	    statusWaitTime = checkIntSysConfig(sys_config, "StatusWaitTime", statusWaitTime);
	}
	
	if(!statusMaxTimeGiven) {
	    statusMaxTimeAllowed = checkIntSysConfig(sys_config, "StatusMaxTime", statusMaxTimeAllowed);
	}
	
	if(!spaceTokenGiven) {
	    String temp = (String)sys_config.get("SpaceToken");
	    if(temp != null) {
		fileToken= temp;
	    }
	}
	
	if(!serviceURLGiven) {
	    String temp = (String) sys_config.get("ServiceURL");
	    if(temp != null) {
		serviceURL = temp;
	    }
	}
	
	if(fileToken != null && !fileToken.equals("")) {
	    retentionPolicyStr = "none";
	}       

	//if all three not provided, using default proxy.
	//if one of userCert or userKey is provided, the other is needed
	if(proxyFile.equals("")) {
	    if(userCert.equals("") && userKey.equals("")) {
		try {
		    //proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID();
		    proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID2();
		}catch(Exception e) {
		    util.printMessage("\nSRM-CLIENT: Exception from client="+e.getMessage(),logger,silent);
		    util.printEventLogException(_theLogger,"",e);
		    proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID();
		}
		pConfig.setProxyFile(proxyFile);
		properties.put("proxy-file", proxyFile);	    
	    } else {
		if(userCert.equals("") || userKey.equals("")) {
		    logMsg("UserCert and Userkey both should be provided", tag, "\nSRM-CLIENT:");
		    showUsage(false);
		}
	    }
	}
	
	if(isRenew) {
	    String v1 = (String) properties.get("user-cert");
	    String v2 = (String) properties.get("user-key");
	    if(v1.startsWith("Enter") || v2.startsWith("Enter")) {
		String err = "If you want to renew proxy automatically,\n "+"you need to enter user-cert location and user-key location.";
		System.out.println("\nSRM-CLIENT:"+err);		   
		logMsg(err, tag, null);
		doExit(tag, 93);
	    }
	    String v3 = (String)properties.get("proxy-file");
	    if(v3.startsWith("Enter")) {
		String err = "If you want to renew proxy automatically,\n "+"please enter your proxy file location.";
		System.out.println("\nSRM-CLIENT:"+err);
		logMsg(err, tag, null);
		doExit(tag, 93); 	    
	    } else {
		String line = PasswordField.readPassword("Enter GRID passphrase: ");
		_password = line;
	    }
	    //else there is no need to renew proxy.
	}
	

} 
	
    private void handleLocalTgtdir(String tag, String ostype) throws Exception {
	    if(targetDir.endsWith("/")) { 
		targetDir = targetDir.substring(0,targetDir.length()-1);
	    }
	    
	    if (ostype != null && (ostype.startsWith("Windows") || ostype.startsWith("windows"))) 
	    {       	
		String tempTargetDir =  parseLocalSourceFileForPath(targetDir);
		File f = new File(tempTargetDir.substring(1));
		if(!f.exists()) {
		    logMsg("Given targetDir does not exists "+ targetDir, tag, "SRM-CLIENT");
		    showUsage(false);
		}
		if(!f.isDirectory()) {
		    logMsg("Given targetDir is not a dir. "+ targetDir, tag, "SRM-CLIENT");
		    showUsage(false);
		}		
		if(!f.canWrite()) {
		    logMsg("Given targetDir has no write permission. "+ targetDir, tag, "SRM-CLIENT");   
		    showUsage(false);
		}	    
	    } else {
		String tempTargetDir =  parseLocalSourceFileForPath(targetDir);
		File f = new File(tempTargetDir);
		
		if(domkdir && !recursive) {
		    tempTargetDir = tempTargetDir+"/";
		    String temp3="";
		    while (true) {
			int idx = tempTargetDir.indexOf("/");
			if(idx != -1) {
			    int idx2 = tempTargetDir.indexOf("/",idx+1);
			    if(idx2 != -1) {
				String temp2 = tempTargetDir.substring(idx,idx2);
				f = new File(temp3+temp2);
				if(!f.exists()) {
				    boolean b = f.mkdir();
				    if(!b) {
					logMsg("SRM-CLIENT: Make directory creation failed " + temp3+temp2, null, "");
					System.exit(1);
				    }
				}
				temp3 = temp3+temp2;
				tempTargetDir = tempTargetDir.substring(idx2);
			    }
			    else break;
			} else break;
		    }//end while
		}
		tempTargetDir =  parseLocalSourceFileForPath(targetDir);
		f = new File(tempTargetDir);
		if(!f.exists()) {
		    logMsg("Given targetDir does not exists "+ tempTargetDir, tag, "SRM-CLIENT");
		    showUsage(false);
		}
		if(!f.isDirectory()) {
		    logMsg("Given target is not a dir:"+ tempTargetDir, tag, "SRM-CLIENT");
		    showUsage(false);
		}
		if(!f.canWrite()) {
		    logMsg("Given targetDir has no write permission " + targetDir, tag, "SRM-CLIENT");
		    showUsage(false);
		}
	    }

}

private void handleInputFile(Vector fileInfo, String tag) throws Exception {
	    //request type is found is parseXML with findRequestType method
	    //by default request is get, so target dir check is done in parseXML
	    request = parseXML(inputFile);
	    
	    requestType = request.getModeType();
	    
	    //added for Melina's case on Oct. 26, 09
	    if(requestType.equalsIgnoreCase("Get")) { 
		fileInfo = validateURL(request);
		
		if(domkdir && !recursive) {
		    if(fileInfo.size() > 1) {
			System.out.println("\nSRM-CLIENT: Mkdir is supported only for"+
					   " single source and single target");
			System.exit(1);
		    }
		    if(fileInfo.size() > 0) { 
			FileInfo fInfo = (FileInfo) fileInfo.elementAt(0);
			String temp = parseLocalSourceFileForPath(fInfo.getTURL());
			int idx = temp.lastIndexOf("/");
			if(idx != -1) {
			    temp = temp.substring(0,idx);
			}
			temp = temp+"/";
			String temp3="";
			while (true) {
			    idx = temp.indexOf("/");
			    if(idx != -1) {
				int idx2 = temp.indexOf("/",idx+1);
				if(idx2 != -1) {
				    String temp2 = temp.substring(idx,idx2);
				    //System.out.println("TEMP3="+temp3);
				    //System.out.println("TEMP2="+temp2);
				    File f = new File(temp3+temp2);
				    if(!f.exists()) {
					boolean b = f.mkdir();
					if(!b) {
					    logMsg("SRM-CLIENT: Make directory creation failed " + temp3+temp2, null, "");
					    System.exit(1);
					}
					logMsg("SRM-CLIENT: directory created" + temp3+temp2, null, "");
				    }
				    temp3 = temp3+temp2;
				    temp = temp.substring(idx2);
				}
				else break;
			    } else break;
			}//end while
		    }//end if
		}//domkdir
	    }
	    
}
	
public void init() {
    String tag = "SRMClientN::init ";
    Properties sys_config = new Properties(); 
        
    try {	
	initEnv(tag, sys_config);
	String ostype = System.getProperty("os.name");

	if(!targetDir.equals("") && (targetDir.startsWith("file:") || targetDir.startsWith(File.separator))) {
	    handleLocalTgtdir(tag, ostype);
	}

	String[] surl = null;
	String[] turl = null;
	long[] size = null;
	
	Vector fileInfo = new Vector();

	if(inputFile.equals("")) { 
	    surl = new String[1];
	    turl = new String[1];
	    
	    if(doReleaseFile) {
		initDoRelease();
	    } else {
		if(doStatus) {
		    if(!sourceUrl.equals("") || !targetUrl.equals("")) {
			surl[0] = sourceUrl;
			turl[0] = targetUrl;
			request = createRequest(surl[0],turl[0], "");
			//requestType is already given by user for doStatus.
			//findRequestType(surl[0],turl[0],"");
			if(requestType.equalsIgnoreCase("get")) {
			    request.setModeType("get");			
			} else if(requestType.equalsIgnoreCase("put")) {
			    request.setModeType("put");			
			} else if(requestType.equalsIgnoreCase("copy")) {
			    request.setModeType("copy");			
			} else if(requestType.equalsIgnoreCase("bringonline")) {
			    request.setModeType("bringonline");
			}
		    }		
		} else {
		    System.out.println("doBOL="+doBringOnline+" submitOnly="+submitOnly+" noDownload="+noDownLoad);
		    if(!doBringOnline) {
			if(!submitOnly && !noDownLoad) {
			    if(sourceUrl.equals("") || (targetUrl.equals("") && targetDir.equals(""))) {       
				logMsg("\nPlease provide me <sourceUrl> and <targeturl>\n", null, "");
				logMsg("or -f <inputfile>\n", null, "");
				showUsage(false);
				if(pIntf != null) return;
			    }
			    if(!targetDir.equals("") && targetUrl.equals("")) {
				if((sourceUrl.startsWith("gsiftp")) && (targetDir.startsWith("srm")) || 
				   (sourceUrl.startsWith("srm")) && (targetDir.startsWith("srm"))) 
				{
				    targetUrl = targetDir;				
				} else {
				    //June 4, 2010
				    if(sourceUrl.startsWith("srm") && targetDir.startsWith("file") && recursive) {						
					targetUrl = targetDir;
					int idx = sourceUrl.lastIndexOf("/");
					if(idx != -1) {
					    String temp = parseLocalSourceFileForPath(targetDir+"/"+sourceUrl.substring(idx+1));
					    File f = new File(temp);  
					    if(f.exists()) {
						targetUrl = targetDir+"/"+sourceUrl.substring(idx+1);
					    }
					}
				    } else {
					int idx = sourceUrl.lastIndexOf("/");
					if(idx != -1) {
					    targetUrl = targetDir+"/"+sourceUrl.substring(idx+1);
					}
				    }
				}
			    }
			} else {
			    if(!submitOnly && sourceUrl.equals("")) {
				logMsg("\nPlease provide <sourceUrl> \n", null, "");
				logMsg("or -f <inputfile>\n", null, "");
				showUsage(false);
				if(pIntf != null) return;
			    }
			}
		    }
		    surl[0] = sourceUrl;
		    turl[0] = targetUrl;
		    if(simpleSRMCOPY) {
			//either one of url should start with srm
			if(sourceUrl.startsWith("srm")) { ; }
			else if(targetUrl.startsWith("srm")) { ; }
		    }
		    if(!submitOnly && (targetUrl.startsWith("file:") || targetUrl.startsWith(File.separator))) { //get
			if(sourceUrl.startsWith("gsiftp:")) { //may be directGsiFTP
			    MyGlobusURL gurl = new MyGlobusURL(sourceUrl,0);
			    String host = gurl.getHost();
			    int port = gurl.getPort();
			    String path = gurl.getPath();
			    sourceUrl = "gsiftp://"+host+":"+port+path;
			    surl[0] = sourceUrl;
			    if(!path.startsWith("//")) {
				sourceUrl = "gsiftp://"+host+":"+port+"/"+path;
				surl[0] = sourceUrl;
			    }
			    if(serviceUrl.equals("")) {
				mayBeDirectGsiFTP=true;
				directGsiFTP=true;
			    }
			}
			turl[0] = targetUrl;
			try {
			    String temp = parseLocalSourceFileForPath(targetUrl);
			    if(ostype !=null && (ostype.startsWith("Windows") || ostype.startsWith("windows"))) {
				temp = temp.substring(1);
			    }
			    if(!recursive) {
				File f = new File(temp);
				if(f.isDirectory()) {
				    int idx = sourceUrl.lastIndexOf("/");
				    if(idx != -1) {
					targetUrl = targetUrl+"/"+sourceUrl.substring(idx+1);
					turl[0] = targetUrl;
				    }
				}
			    }
			    int idx = temp.lastIndexOf("/");
			    if(idx != -1) { 
				int idx4 = idx;
				String t = temp.substring(0,idx)+"/";
				if(domkdir && !recursive) {
				    String temp3="";
				    while (true) {
					idx = t.indexOf("/");
					if(idx != -1) {
					    int idx2 = t.indexOf("/",idx+1);
					    if(idx2 != -1) {
						String temp2 = t.substring(idx,idx2);
						//System.out.println("TEMP3="+temp3);
						//System.out.println("TEMP2="+temp2);
						File f = new File(temp3+temp2);
						if(!f.exists()) {
						    boolean b = f.mkdir();
						    if(!b) {
							logMsg("SRM-CLIENT: Make directory creation failed " + temp3+temp2, null, "");
							System.exit(1);
						    }
						}
						temp3 = temp3+temp2;
						t = t.substring(idx2);
					    }
					    else break;
					} else break;
				    }//end while
				}
				t = temp.substring(0,idx4);
				File f = new File(t);
				if(!f.exists()) {
				    logMsg("SRM-CLIENT: Given targetDir does not exists " +t, null, "");
				    showUsage(false);
				    if(pIntf != null) return;
				}
				if(!f.isDirectory()) {
				    logMsg("SRM-CLIENT: Given targetDir is not a directory " +t, null, "");
				    showUsage(false);
				    if(pIntf != null) return;
				}
				if(!temp.equals("/dev/null")) {
				    if(!f.canWrite()) {
					logMsg("SRM-CLIENT: Given targetDir has no write permission " +t, null, "");
					showUsage(false);
					if(pIntf != null) return;
				    }
				}
				targetDir = t;
			    }
			}catch(Exception e) { 
			    util.printMessage("SRM-CLIENT: Exception from client="+e.getMessage(),logger,silent);
			    util.printEventLogException(_theLogger,"",e);
			    //util.printStackTrace(e,logger);
			    showUsage(false);
			    if(pIntf != null) return;
			}
		    }
		    
		    if(!submitOnly && (sourceUrl.startsWith("file:") || sourceUrl.startsWith(File.separator))) { //put				       
			if(targetUrl.startsWith("gsiftp:")) { //may be directGsiFTP
			    MyGlobusURL gurl = new MyGlobusURL(targetUrl,0);
			    String host = gurl.getHost();
			    int port = gurl.getPort();
			    String path = gurl.getPath();
			    targetUrl = "gsiftp://"+host+":"+port+path;
			    turl[0] = targetUrl;
			    if(!path.startsWith("//")) {
				targetUrl = "gsiftp://"+host+":"+port+"/"+path;
				turl[0] = targetUrl;
			    }
			    if(serviceUrl.equals("")) {
				mayBeDirectGsiFTP=true;
				directGsiFTP=true;
			    }
			}
			String temp = parseLocalSourceFileForPath(sourceUrl);
			File f = new File(temp);
			if(f.exists()) {
			    if(f.isDirectory() && !recursive) {
				logMsg("\nSRM-CLIENT: SourceUrl is a directory.", null, "");
				logMsg("please use -recursive option to do the srmPrepareToPut request", null, "");
				showUsage(false);
				if(pIntf != null) return;
			    }
			    if(recursive && !f.isDirectory()) {
				logMsg("\nSRM-CLIENT: SourceUrl is not a directory. But recursive is set.", null, "");
				showUsage(false);
				if(pIntf != null) return;
			    }
			}		    
		    } else if(sourceUrl.startsWith("gsiftp") && targetUrl.startsWith("gsiftp")) { 
			if(serviceUrl.equals("")) {
			    directGsiFTP=true;
			}
			MyGlobusURL gurl = new MyGlobusURL(sourceUrl,0);
			String host = gurl.getHost();
			int port = gurl.getPort();
			String path = gurl.getPath();
			sourceUrl = "gsiftp://"+host+":"+port+path;
			surl[0] = sourceUrl;
			if(!path.startsWith("//")) {
			    sourceUrl = "gsiftp://"+host+":"+port+"/"+path;
			    surl[0] = sourceUrl;
			}
			gurl = new MyGlobusURL(targetUrl,0);
			host = gurl.getHost();
			port = gurl.getPort();
			path = gurl.getPath();
			targetUrl = "gsiftp://"+host+":"+port+path;
			turl[0] = targetUrl;
			if(!path.startsWith("//")) {
			    targetUrl = "gsiftp://"+host+":"+port+"/"+path;
			    turl[0] = targetUrl;
			}
		    }
		    
		    request = createRequest(surl[0],turl[0], "");
		    findRequestType(surl[0],turl[0],"");
		    request.setModeType(requestType);
		}
          }	
	} else {
	    handleInputFile(fileInfo, tag);
	}
	

	if(!doStatus) {
	    requestType = request.getModeType();	
	} else {
	    if(!requestType.equalsIgnoreCase("Get") && 	!requestType.equalsIgnoreCase("Put") && 
	       !requestType.equalsIgnoreCase("Copy") && !requestType.equalsIgnoreCase("BringOnline"))	       
	    {
		logMsg("Given requestType is not valid " +requestType, null, "\nSRM-CLIENT:");
		showUsage(false);
	    } 
	    if(requestType.equalsIgnoreCase("bringonline")) {
		doBringOnline=true;
	    }
	}
	
	//currently  in the srm-copy -reservespace is only allowed for put
	//operation.
	//To get or copy or bringonline from reservespace, use
	//srm-sp-reserve and then use that spacetoken to do the operation
	if(doReserveSpace && !requestType.equals("put")) {
	    logMsg("-reservespace option is currently only allowed for request type put", tag, "\nSRM-CLIENT:");
	    showUsage(false);
	}
	
	if(!doReleaseFile) {
	    //util.printMessage("\nRequestType " + requestType, logger,silent);
	    logMsg("RequestType="+requestType, tag, null);
	}
	
	if(mayBeDirectGsiFTP) {
	    //this is the case request type is copy and source and target are file://
	    //but -direct or to SRM server is not decided yet. 
	    if(serviceUrl.equals("")) {
		directGsiFTP=true;
		//requestType = "put"; //we have to fake this, otherwise it goes to              //SRMCopy method
		//request.setModeType(requestType);
	    }
	}
	
	fileInfo = validateURL(request);
	
	//if surl for file is same, it is ignored in the Request.getFileInfo
	if(!doStatus && !doReleaseFile) {
	    logMsg("SRM-CLIENT: Totalfiles in the request : " + fileInfo.size(), tag, "");	    
	}
	String temp = "";
	String tempType ="surlType";
	
	
	if(fileInfo.size() > 0) {
	    FileInfo fInfo = (FileInfo) fileInfo.elementAt(0);
	    if(doCopy) {
		temp = fInfo.getSURL();
              if(temp.startsWith("srm:")) {
		  overrideserviceurl = true;              
              } else {
		  overrideserviceurl = false;
              }	    
	    } else if(requestType.equalsIgnoreCase("releaseFile")) {
		temp = fInfo.getSURL();
		overrideserviceurl = true;	    
	    } else if((requestType.equalsIgnoreCase("put"))) {
		temp = fInfo.getTURL(); 
		tempType = "turlType";
		if(serviceURLGiven) {
		    int idx = temp.indexOf("?SFN");
		    if(idx != -1) {
			String aaa = temp.substring(0,idx);  
			if(aaa.equals(serviceUrl)) {
			    overrideserviceurl = true;			
			} else {
			    remotePutCase=true;
			}		    
		    } else {
			if((temp.startsWith("gsiftp")) || (temp.startsWith("srm"))) {
			    MyGlobusURL gurl = new MyGlobusURL(temp,0);
			    String host = gurl.getHost();
			    MyGlobusURL gurl2 = new MyGlobusURL(serviceUrl,0);
			    String host2 = gurl2.getHost();
			    if(!host.equals(host2)) {
				remotePutCase=true;
			    }
			}
		    }		
		} else {
		    if(temp.startsWith("srm:")) {
			overrideserviceurl = true;
		    }
		}	    
	    } else if((requestType.equalsIgnoreCase("copy"))) {
		if((fInfo.getSURL().startsWith("srm") && fInfo.getTURL().startsWith("gsiftp")) ||		    
		   (fInfo.getSURL().startsWith("srm") &&
		    ((fInfo.getTURL().startsWith(File.separator)) || fInfo.getTURL().startsWith("file")))) 
		{		     
		    pushMode = true;
		}
		if(fInfo.getSURL().startsWith("gsiftp") && fInfo.getTURL().startsWith("gsiftp")) {		   
		    partycopy=true;
		}
		if(pushMode || partycopy || doBringOnline) {
		    if(partycopy && fInfo.getSURL().startsWith("srm")) {
			temp = fInfo.getSURL(); 		    
		    } else if(partycopy && fInfo.getSURL().startsWith("gsiftp") &&
			    fInfo.getTURL().startsWith("srm")) {
			temp = fInfo.getTURL(); 
			tempType = "turlType";		    
		    } else if(partycopy && fInfo.getSURL().startsWith("gsiftp") &&
			    fInfo.getTURL().startsWith("gsiftp")) {
			temp = fInfo.getSURL(); 
			directGsiFTP=true;		    
		    } else {
			temp = fInfo.getSURL(); 
		    }		
		} else {  
		    temp = fInfo.getTURL(); 
		    tempType = "turlType";
		}
		if(!mayBeDirectGsiFTP) {
		    if(partycopy && temp.startsWith("srm")) {
			overrideserviceurl = true;
		    }
		    if(partycopy && temp.startsWith("gsiftp")) {
			overrideserviceurl = false;		    
		    } else {
			if(!serviceURLGiven) {
			    overrideserviceurl = true;
			}
		    }
		}	    
	    } else {
		//for get type.
		temp = fInfo.getSURL();
		if(temp.startsWith("srm:")) {
		    if(!serviceURLGiven) {
			overrideserviceurl = true;
		    }
		}
	    }	
	} else {  
	    if(doReleaseFile) {
		if(requestToken != null) {
		    if(!serviceUrl.equals("")) { ; }
		    else  {
			logMsg("\nNo files in the request for transfer", null, "");
			logMsg("Cannot proceed further, please check input", null, "");
			showUsage(false);
			if(pIntf != null) return;
		    }		
		} else {
		    logMsg("\nNo files in the request for transfer", null, "");
		    logMsg("Cannot proceed further, please check input", null, "");
		    showUsage(false);
		    if(pIntf != null) return;
		}	    
	    } else if(doStatus) { 
		if(statusToken != null && !statusToken.equals("")) {
		    if(serviceUrl.equals("")) {
			logMsg("\nPlease provide the -serviceurl", null, "");
			showUsage(false);
			if(pIntf != null) return;
		    }
		} else {
		    logMsg("\nPlease provide the status token.", null, "");
		    showUsage(false);
		    if(pIntf != null) return;
		}	    
	    } else {
		logMsg("\nNo files in the request for transfer", null, "");
		logMsg("Cannot proceed further, please check input", null, "");
		showUsage(false);
		if(pIntf != null) return;
		
	    }
	}
	
	if(overrideserviceurl) {
	    serviceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(temp,serviceURL,
								    serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
	    if(serviceUrl == null) showUsage(false);
	    for(int i = 0; i < fileInfo.size(); i++) {
		FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
		if(tempType.equals("surlType")) {
		    String temp1 = fIntf.getSURL();
		    if(partycopy) {
			if(temp1.startsWith("srm:")) {
			    String sfn = gov.lbl.adapt.srm.client.util.Util.getSFN(temp1,serviceUrl);
			    fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
			}		  
		  } else {
			String sfn = gov.lbl.adapt.srm.client.util.Util.getSFN(temp1,serviceUrl);
			fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
		    }	      
		} else {
		    String temp1 = fIntf.getTURL();
		    String sfn = gov.lbl.adapt.srm.client.util.Util.getSFN(temp1,serviceUrl);
		    fIntf.setTURL(serviceUrl.replace("httpg","srm")+sfn);
		}
	    }      
	} else {
	    if(serviceUrl.equals("")) {
		String tt = (String) sys_config.get("ServiceUrl");
		String tt2 = (String) sys_config.get("ServiceURL");
		
		if(tt != null || tt2 != null) {
		    if(tt != null) 
			serviceUrl = tt;
		    if(tt2 != null) 
			serviceURL = tt;	      
		} else {
		    if(!directGsiFTP) {
			logMsg("\nPlease provide the -serviceurl full SRM service url", null, "");
			logMsg("  example:srm://<hostname>:<port>//wsdlpath", null, "");
			showUsage(false);
			if(pIntf != null) return;
		    }
		}
	    }
	    if(!directGsiFTP) {
		serviceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(serviceUrl,serviceURL,
									serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
	    }
	} 
	
	if(!doReleaseFile) {
	    //currently we are setting concurrency 1 for
	  //srm client transfers     
	  //concurrency = 1; 

	    tThread = new TransferThread(this,
					 concurrency, parallelism, bufferSize, blockSize, dcau,
					 !nooverwrite, fileInfo.size(), request, 
					 numRetry, retryTimeOut,connectionTimeOutAllowed,
					 _debug,silent,useLog,checkDiskSpace,gucScriptPath,
					 gucEventLogPath,pIntf);
	    
	    fileTransferNGUI = new SRMClientFileTransferN(this,tThread, targetDir, concurrency,	    
							  request, fileInfo, numRetry,
							  _debug,textReport,_theLogger,silent,useLog,pIntf); 
	    fileTransferNGUI.processTransferAction();
	}
	
	GSSCredential credential = getValidCredential(tag);
	
	if(!directGsiFTP) {
	    serviceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(serviceUrl,serviceURL,
								    serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
	}
	
	if(_debug) {
	    logMsg("===================================", null, "");
	    logMsg("BeStMan Client Configuration", null, "");

	    if(!configFileLocation.equals("")) {
		logMsg("ConfFile="+configFileLocation, tag, "\t");	    
	    } else {
		logMsg("ConfFile=none", tag, "\t");	    
	    }
	    logMsg("inputFile="+inputFile, tag, "\t");
	    logMsg("Log4jLocation="+log4jlocation, tag, null);
	    logMsg("OutputFile="+outputFile, tag, null);
	    if(targetDir.equals("")) { 
		logMsg("TargetDir="+targetDir, tag, "\t");
	    }
	    logMsg("ProxyFile="+proxyFile, tag, "\t");
	    if(!userCert.equals("")) {
		logMsg("UserCert="+userCert, tag, "\t");
	    }
	    if(!userKey.equals("")) {
		logMsg("UserKey="+userKey, tag, "\t");
	    }
	    if(maxFilesPerRequest != 0) {
		logMsg("MaxFilesPerRequest="+maxFilesPerRequest, tag, "\t");
	    } else {
		logMsg("MaxFilesPerRequest=all the files in the input file", tag, "\t");
	    }
	    logMsg("serviceUrl="+serviceUrl, tag, "\t");
	    logMsg("DebugON="+_debug,  tag, "\t");
	    logMsg("QuietON="+silent,  tag, "\t");
	    logMsg("PushMode="+pushMode, tag, "\t");
	    logMsg("3PartyCopy="+partycopy, tag, "\t");
	    logMsg("RenewProxy="+isRenew,  tag, "\t");
	    logMsg("KeepSpace="+keepSpace,  tag, "\t");
	}
	if(tokenType == 'd') {
	    fileStorageType = TFileStorageType.DURABLE; 	
	} else if(tokenType == 'p') {
	    fileStorageType = TFileStorageType.PERMANENT; 	
	} else if(tokenType == 'v') {
	    fileStorageType = TFileStorageType.VOLATILE; 
	}
	
	if(_debug) {
	    if(fileStorageType != null) {
		logMsg("FileStorageType="+fileStorageType, tag, "");
	    } else {
		logMsg("FileStorageType=null", tag, "");
	    }
	}
	
	//default setup, added on March 28, 07.
	retentionPolicyInfo = new TRetentionPolicyInfo ();
	retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
	retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
	
	if(retentionPolicyStr.equalsIgnoreCase("REPLICA")) {  
	    retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
	}
	if(retentionPolicyStr.equalsIgnoreCase("OUTPUT")) {  
	    retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.OUTPUT);
	}
	if(retentionPolicyStr.equalsIgnoreCase("CUSTODIAL")) {  
	    retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.CUSTODIAL);
	}
	if(retentionPolicyStr.equalsIgnoreCase("NONE")) {
	    retentionPolicyInfo = null;
	}
	if(!accessLatency.equals("")) {
	    if(retentionPolicyInfo != null) {
		if(accessLatency.equalsIgnoreCase("NEARLINE")) {
		    retentionPolicyInfo.setAccessLatency(TAccessLatency.NEARLINE);
		}
		if(accessLatency.equalsIgnoreCase("ONLINE")) {
		    retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
		}
	    }
	} 
	if(_debug) {
	    if(retentionPolicyInfo != null) {
		logMsg("RetentionPolicy="+ retentionPolicyInfo.getRetentionPolicy(), tag, "\t");
		logMsg("AccessLatency="+retentionPolicyInfo.getAccessLatency(), tag, "\t");
	    }
	    if(!storageInfo.equals("")) {
		parseStorageInfo(storageInfo);
	    }
	}
	if(_debug) {
	    if(!doReleaseFile) {
		logMsg("AuthId="+uid, tag, "\t");
		logMsg("doReserveSpace="+doReserveSpace, tag, "\t");
		if(fileToken != null) {
		   logMsg("SpaceToken="+fileToken,  tag, "\t");
		}
		logMsg("BufferSize="+bufferSize,  tag, "\t");
		logMsg("BlockSize="+blockSize,  tag, "\t");
		logMsg("Parallelism="+parallelism,  tag, "\t");
		logMsg("NumRetry="+numRetry,  tag, "\t");
		logMsg("RetryTimeOut="+retryTimeOut,  tag, "\t");
		logMsg("Overwrite="+!nooverwrite,  tag, "\t");

		if(statusMaxTimeAllowed != -1) {
		    logMsg("StatusMaximumTimeAllowed="+statusMaxTimeAllowed + " seconds", tag, "\t");			   		
		} else {
		    logMsg("StatusMaximumTimeAllowed=unlimited", tag, "\t");
		}
		
		logMsg("StatusWaitTime="+statusWaitTime + " second", tag, "\t");
		if(totalRequestTime != 0) {
		    logMsg("TotalRequestTime="+totalRequestTime, tag, "\t");
		}
		if(fileLifeTime != 0) {
		    logMsg("FileLifeTime="+fileLifeTime, tag, "\t");
		}
		if(pinLifeTime != 0) {
		    logMsg("PinLifeTime="+pinLifeTime, tag, "\t");
		}
		if(tokenLifetime != 0) {
		    logMsg("TokenLifeTime="+tokenLifetime, tag, "\t");
		}
		else {
		    logMsg("TokenLifeTime=default", tag, "\t");
		}
		if(tokenSize != 0) {
		    logMsg("TokenSize="+tokenSize, tag, "\t");
		} 
		if(gSize != 0) {
		    logMsg("GuaranteedSize="+gSize, tag, "\t");
		}
	    }
	    logMsg("DirectGsiFTP="+directGsiFTP, tag, "\t");
	    logMsg("domkdir="+domkdir, tag, "\t");
	    logMsg("\tconnectiontimeoutAllowed="+connectionTimeOutAllowed, null, "");
	    logMsg("\tsethttptimeoutAllowed="+setHTTPConnectionTimeOutAllowed, null, "");
	    logMsg("===================================", null, "");
	}       
	
	if(!requestType.equalsIgnoreCase("copy") && !partycopy) {
	    if(!directGsiFTP) {
		logMsg("Connecting to service url:"+serviceUrl, tag, "SRM-CLIENT:"+new Date());
	    }
	}
	
	GSSCredential mycred = getCredential();
	if(_debug) {
	    logMsg("SRM-CLIENT: user credentials is " +mycred.getName().toString(), null, "");
	}
	
	if(doReleaseFile) {
	    if(requestToken != null || fileInfo.size() > 0) {
		logMsg("Releasing file now.", tag, "\nSRM-CLIENT");
		
		SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc, credential, _theLogger, logger,		    
							     pIntf, _debug,silent,useLog,false, false,
							     statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
							     connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
							     delegationNeeded,numRetry,retryTimeOut);
		utilClient.setRequestToken(requestToken);
		String str = utilClient.releaseFile(keepSpace, requestType,fileInfo);
		if(str != null) {
		    resultStatus = str;
		}
		if(pIntf == null) {
		    if(resultStatus == null || resultStatus.equals("")) {
			logMsg("StatusExit StatusCode=0", tag, null);
			System.exit(0);		    
		    } else {
			int exitCode = util.mapStatusCode(resultStatus);
			logMsg("StatusExit StatusCode="+exitCode, tag, null);
			System.exit(exitCode);
		    }
		}//end if pIntf == null	    
	    } else {
		logMsg("No request token or surl provided. Cannot release file", tag, "\nSRM-CLIENT");
		showUsage(false);
		if(pIntf != null) return;
	    }	
	} else {
	    if(doReserveSpace) {
		//if reservespace is used, ignore fileToken,
		//reserve new space
		fileToken = null; 
	    }
	    if(doBringOnline) {
		try {
		    if(doStatus) {
			logMsg("BringOnLine for statusToken=" +statusToken, tag, null);
		    } else {
			logMsg("BringOnLine", tag, null);
		    }
		    
		    srmCopyClient = new SRMBringOnlineClient(this,serviceUrl,uid,fileInfo,fileToken,statusToken,			
							     doReserveSpace, false, !nooverwrite,
							     tokenLifetime, tokenSize, gSize,
							     credential, fileStorageType, retentionPolicyInfo, 
							     totalRequestTime,pinLifeTime, parallelism, bufferSize, dcau,
							     statusMaxTimeAllowed,statusWaitTime, 
							     protocolsList, statusWaitTimeGiven,
							     storageInfo, remoteTransferInfo, 
							     userDesc, _theLogger, logger,pIntf,_debug,silent,useLog,
							     submitOnly,proxyType,noDownLoad,connectionTimeOutAllowed,
							     setHTTPConnectionTimeOutAllowed,
							     delegationNeeded,noAbortFile,threshHoldValue,
							     numRetry,retryTimeOut,checkPing); 
		    
		}catch(Exception e) { 
		    logMsg(e.getMessage(), tag, "\nSRM-CLIENT:");
		    util.printMessageHException("\nSRM-CLIENT: Exception " + e,pIntf);
		    //util.printStackTrace(e,logger);
		    requestDone = true;
		    enableTransferButton(true,false);
		    //e.printStackTrace();
		}	    
	    } else if(requestType.equalsIgnoreCase("Get")) {
		try {
		    if(doStatus) {
			logMsg("Check get status:"+statusToken, tag, "\nSRM-CLIENT:");		    
		    } else {
			if(!directGsiFTP) {
			    logMsg("P2Get", tag, null);			
			} else {
			    logMsg("DirectGsiFTP", tag, "\nSRM-CLIENT:");
			}
		    }
		    
		    srmCopyClient = new SRMGetClient
			(this,serviceUrl,uid,fileInfo,fileToken,statusToken,
			 doReserveSpace, false, !nooverwrite,
			 tokenLifetime, tokenSize, gSize,
			 credential, 
			 fileStorageType, retentionPolicyInfo, totalRequestTime,
			 pinLifeTime,
			 0,false, parallelism, bufferSize, dcau, directGsiFTP,
			 statusMaxTimeAllowed,statusWaitTime,
			 protocolsList, statusWaitTimeGiven,
			 storageInfo, remoteTransferInfo, userDesc,_theLogger,
			 logger, pIntf, _debug,silent,useLog,submitOnly,proxyType,
			 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
			 delegationNeeded,noAbortFile,false,
			 false,threshHoldValue,numRetry,retryTimeOut,checkPing); 
		}catch(Exception e) { 
		    logMsg("Exception from server:"+e.getMessage(), tag, "\nSRM-CLIENT:");
		    //util.printStackTrace(e,logger);
		    requestDone = true;
		    enableTransferButton(true,false);
		    //e.printStackTrace();
		}	    
	    } else if(requestType.equalsIgnoreCase("Put")) {
		try {
		    if(doStatus) {
			logMsg("Check put status for statusToken=" + statusToken, tag, "\nSRM-CLIENT:");		    
		    } else {
			if(!directGsiFTP) { 
			    logMsg("P2Put", tag, null);			
			} else {
			    logMsg("DirectGsiFTPPut", tag, "\nSRM-CLIENT:");
			}
		    }
		    
		    if(doReserveSpace) {
			for(int i = 0; i < fileInfo.size(); i++) {
			    FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
			    tokenSize = tokenSize+fIntf.getExpectedSize2();   
			}
			tokenSize = tokenSize*2; //just for safer
			gSize = tokenSize;
		    }
		    srmCopyClient = new SRMPutClient (this,serviceUrl,uid,fileInfo,fileToken,statusToken,			
						      doReserveSpace, !nooverwrite,
						      3600,   tokenSize ,gSize,
						      //tokenLifetime, tokenSize, gSize,
						      credential, fileStorageType, retentionPolicyInfo, 
						      totalRequestTime, pinLifeTime, fileLifeTime, 
						      true, 0, false,  parallelism, bufferSize, dcau,
						      directGsiFTP, statusMaxTimeAllowed, statusWaitTime,
						      protocolsList, statusWaitTimeGiven, storageInfo, 
						      remoteTransferInfo, userDesc, _theLogger,
						      logger,pIntf,_debug,silent,useLog,submitOnly,
						      proxyType,remotePutCase,
						      connectionTimeOutAllowed,
						      setHTTPConnectionTimeOutAllowed,
						      delegationNeeded,noAbortFile,domkdir,
						      recursive,threshHoldValue,noWaitOnRemotePut,
						      numRetry,retryTimeOut,checkPing); 
		}catch(Exception e) { 
		    logMsg("Exception from server:"+e.getMessage(), null, "\nSRM-CLIENT:");
		    util.printMessageHException("\nSRM-CLIENT: Exception from server=" + e.getMessage(),pIntf);
		    //util.printStackTrace(e,logger);
		    requestDone = true;
		    enableTransferButton(true,false);
		    //e.printStackTrace();
		}	    
	    } else if(requestType.equalsIgnoreCase("Copy")) {
		try {
		    if(doStatus) {
			logMsg("Checking copy status for statusToken=" + statusToken, tag, "\nSRM-CLIENT:");
		    }
		    if(partycopy) {
			logMsg("Doing 3partycopy now.", tag, "\nSRM-CLIENT:");		    
		    } else {
			logMsg("SrmCopy", tag, null);
		    } 
		    
		    
		    srmCopyClient = new SRMCopyClient (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
						       pushMode, partycopy, !nooverwrite,
						       doReserveSpace, doReleaseSpace,  
						       tokenLifetime, tokenSize, gSize,
						       credential, request, 
						       fileStorageType,retentionPolicyInfo,totalRequestTime,
						       pinLifeTime,fileLifeTime,  parallelism, bufferSize, dcau,
						       statusMaxTimeAllowed,statusWaitTime,
						       protocolsList, statusWaitTimeGiven,
						       storageInfo, remoteTransferInfo, userDesc, 
						       _theLogger,logger,pIntf,_debug,silent,useLog,
						       submitOnly,proxyType,connectionTimeOutAllowed,
						       setHTTPConnectionTimeOutAllowed,
						       recursive,numLevels,delegationNeeded,noAbortFile,domkdir,
						       threshHoldValue,numRetry,retryTimeOut,checkPing); 
		}catch(Exception e) { 
		    logMsg("Exception from server=" + e.getMessage(), tag, "SRM-CLIENT");
		    util.printMessageHException("\nSRM-CLIENT: Exception from server=" + e.getMessage(),pIntf);
		    //util.printStackTrace(e,logger);
		    requestDone = true;
		    enableTransferButton(true,false);
		    //e.printStackTrace();
		}
	    }
	}
	
	if(doStatus) {
	    srmCopyClient.doStatusEnquiry();	
	} else {
	    if(!doReleaseFile) {
		if(allLocalFileExists) {
		    srmCopyClient.performTransfer(true);		
		} else {
		    srmCopyClient.performTransfer(false);
		}
	    }
	}
    }catch(Exception e) {
	e.printStackTrace();
	//util.printStackTrace(e,logger);
	int idx = e.getMessage().indexOf("java.lang.reflect.InvocationTargetException");					 
	if(idx != -1) {
	    logMsg(e.getMessage(), null, "");
	    logMsg("SRM-CLIENT: Server exception, please contact server admin", null, "");	
	} else {
	    util.printMessage("SRM-CLIENT:Exception from client="+e.getMessage(), logger,silent);			      
	    util.printMessageHException("SRM-CLIENT:Exception from client="+ e.getMessage(),pIntf);					
	    util.printEventLogException(_theLogger,"",e);
	}
	if(pIntf == null) {
	    logMsg("StatusExit StatusCode=92", tag, null);
	    System.exit(92);
	}
	util.printHException(e,pIntf);
    }
}
    
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printVersion
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 
public static void printVersion () {
  String name = "SRM-Client " +
        SRMClientN.class.getPackage().getImplementationVersion();
    String lbnl = "SRM-Client and BeStMan Copyright(c) 2007-2008, The Regents of the University " +
      "Of California, through Lawrence Berkeley National Laboratory. All rights " +
      "reserved. ";
    String email = "Support at SRM@LBL.GOV";
 
    System.out.println(name);
    System.out.println(lbnl);
    System.out.println(email);
    System.exit(1);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// constructFileName
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String constructFileName(String type) {

  Properties properties = System.getProperties();
  String username = (String)properties.getProperty("user.name");
  String result="/tmp/srm-copy";
  if(username == null) {
    result = result+"-"+System.currentTimeMillis()+type;
  }
  else {
    result = result+"-"+username+type;
  }
  return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseStorageInfo
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void parseStorageInfo(String sInfo) {
   if(sInfo.equals("true") || sInfo.equals("false")) return;
   int checkIdx = sInfo.indexOf("#");
   String sInfo2 = "";
   if(checkIdx != -1) {
     sInfo2 = sInfo.substring(checkIdx+1); 
     sInfo = sInfo.substring(0,checkIdx);
   }
   Vector sourceStorage = new Vector();
   Vector targetStorage = new Vector();
   Vector sourcetargetStorage = new Vector();
   StringTokenizer stk = new StringTokenizer(sInfo,",");
   boolean sourceFound=false;
   boolean targetFound=false;
   while(stk.hasMoreTokens()) {
    String temp = stk.nextToken();
    int idx = temp.indexOf(":");
    if(idx != -1) {
      String key = temp.substring(0,idx);
      String value = temp.substring(idx+1);
      if(value.equals("source") || value.equals("sourcetarget")) {
        sourceFound=true;
      }
      if(value.equals("target")) {
        targetFound=true;
      }
      if(sourceFound) {
        sourceStorage.addElement(key+":"+value);
      }
      if(targetFound) {
        targetStorage.addElement(key+":"+value);
      }
    }
    else {
     String msg =
       "Given storage info is not in the correct format " +
       "Please use the format for:source,login:uid,passwd:pwd etc." +
       " Given storage info is " + storageInfo;
      System.out.println(msg);
      showUsage(false);
                     if(pIntf != null) return;
     }
  }
  if(checkIdx != -1) {
   stk = new StringTokenizer(sInfo2,",");
   while(stk.hasMoreTokens()) {
    String temp = stk.nextToken();
    int idx = temp.indexOf(":");
    if(idx != -1) {
      String key = temp.substring(0,idx);
      String value = temp.substring(idx+1);
      if(value.equals("source") || value.equals("sourcetarget")) {
        sourceFound=true;
      }
      if(value.equals("target")) {
        targetFound=true;
      }
      if(sourceFound) {
        sourceStorage.addElement(key+":"+value);
      }
      if(targetFound) {
        targetStorage.addElement(key+":"+value);
      }
    }
    else {
     String msg =
       "Given storage info is not in the correct format " +
       "Please use the format for:source,login:uid,passwd:pwd etc." +
       " Given storage info is " + storageInfo;
      System.out.println(msg);
      showUsage(false);
                     if(pIntf != null) return;
     }
   }
  }
  if(sourceStorage.size() > 0 ) {
    System.out.println("\tsource-storageinfo");
    for(int i = 0; i < sourceStorage.size(); i++) {
       if((!sourceStorage.elementAt(i).equals("for:source")) && 
          !sourceStorage.elementAt(i).equals("for:sourcetarget")) 
	   {
         System.out.println("\t\t"+sourceStorage.elementAt(i));
       }
    }
  }
  if(targetStorage.size() > 0 ) {
    System.out.println("\ttarget-storageinfo");
    for(int i = 0; i < targetStorage.size(); i++) {
       if(!sourceStorage.elementAt(i).equals("for:target")) {
         System.out.println("\t\t"+targetStorage.elementAt(i));
       }
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles (String turl, String rid, int label) {
   
  //if(!gateWayModeEnabled) {
  if(noAbortFile) {
    util.printMessage("\nSRM-CLIENT: Skipping abortFile as per user request", logger,silent);
    util.printMessage("\nSRM-CLIENT: Skipping abortFile as per user request", pIntf);
    return;
  }
  try {
    if(srmCopyClient != null) {
       inputVec.clear();
       inputVec.addElement("for turl="+turl);
       srmCopyClient.abortFiles(turl,rid,label);
       util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);
    }
    else {
      inputVec.clear();
      inputVec.addElement("Cannot do abortfiles="+turl);
      util.printMessage("SRM-CLIENT: Cannot do abortFiles", logger,silent);
      util.printMessage("SRM-CLIENT: Cannot do abortFiles",pIntf);
      util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);
    }
  }catch(Exception e) {
     util.printMessage("\nSRM-CLIENT: Exception from server="+e.getMessage(),logger,silent);
     util.printMessageHException(e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
  }
 //}
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setGateWayModeEnabled
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setGateWayModeEnabled (boolean b) {
  gateWayModeEnabled = b;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// putDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) {
  if(requestType.equals("3partycopy") && siteUrl.startsWith("gsiftp:"))
     return;

  //if(!gateWayModeEnabled) {
  try {
    inputVec.clear();
    inputVec.addElement("for SURL="+siteUrl);
    util.printEventLog(_theLogger,"PutDone",inputVec,silent,useLog);
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling putDone for " + siteUrl, logger,silent); 
    util.printMessage("\nSRM-CLIENT: " + new Date() + 
		" Calling putDone for " + siteUrl, pIntf); 
   if(srmCopyClient != null) {
     srmCopyClient.putDone(siteUrl,rid,label);
   }
   else {
     inputVec.clear();
     inputVec.addElement("Cannot do putDone for SURL="+siteUrl);
     util.printMessage("SRM-CLIENT: " + "Cannot do putDone for SURL="+siteUrl,logger,silent);
     util.printMessage("SRM-CLIENT: " + "Cannot do putDone for SURL="+siteUrl,
		pIntf);
     util.printEventLog(_theLogger,"PutDone",inputVec,silent,useLog);
   }
  }catch(Exception e) {
     util.printMessage("\nSRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
     util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
  }
 //}
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseFile(String siteUrl, String rid, int label) {
  //if(!gateWayModeEnabled) {
  try {
   if(srmCopyClient != null) {
     if(doReleaseSpace) {
       inputVec.clear();
       inputVec.addElement("for SURL="+siteUrl);
       util.printEventLog(_theLogger,"ReleaseFile.doReleaseSpace",inputVec,silent,useLog);
       String code = srmCopyClient.releaseFile(siteUrl,rid,label);
       if(code != null) {
         resultStatus = code;
       }
     }
     if(alsoReleaseFile) {
       inputVec.clear();
       inputVec.addElement("for SURL="+siteUrl);
       util.printEventLog(_theLogger,"ReleaseFile.alsoReleaseFile",inputVec,silent,useLog);
       //alex said, request type get can have releaseFile 
       if(requestType.equalsIgnoreCase("get")) {
         String code = srmCopyClient.releaseFile(siteUrl,rid,label);
         if(code != null) {
           resultStatus = code;
         }
       }
       else if((requestType.equalsIgnoreCase("copy") && partycopy) ||
			   (requestType.equalsIgnoreCase("3partycopy"))) {
         SRMWSDLIntf srmGetCopyClient = srmCopyClient.getGetTypeWSDLIntf();
         if(srmGetCopyClient != null) {
           String code = srmGetCopyClient.releaseFile(siteUrl,rid,label);
           if(code != null) {
             resultStatus = code;
           }
         }
       }
     }
   }
   else {
     inputVec.clear();
     inputVec.addElement("Cannot do releaseFile for SURL="+siteUrl);
     util.printEventLog(_theLogger,"",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Cannot do releaseFile",logger,silent);
     util.printMessage("SRM-CLIENT: Cannot do releaseFile",pIntf);
   }
  }catch(Exception e) {
     util.printMessage("SRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
     util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
  }
  //}
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTotalFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTotalFiles (int tFiles) {
  if(tThread != null) { 
    tThread.setTotalFiles(tFiles);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestType() {
  return requestType;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkAllSourceFromSameSRM2
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean checkAllSourceFromSameSRM2(Vector fInfo) throws Exception {
  int size = fInfo.size();

  String sourceSRM = "";


  if(doCopy) return true;

  if(directGsiFTP) return true;

  if((request.getModeType().equalsIgnoreCase("get")) ||
     (request.getModeType().equalsIgnoreCase("bringonline")) ||
		(request.getModeType().equalsIgnoreCase("copy") && pushMode)) {
    String ssurl = "";
    if(size > 0) {
      FileInfo files = (FileInfo) fInfo.elementAt(0);
      ssurl = files.getSURL();
      sourceSRM = gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(ssurl);
    }
    if(!ssurl.equals("") && !ssurl.startsWith("gsiftp")) {
    for(int i = 1; i < size; i++) {
      FileInfo files = (FileInfo) fInfo.elementAt(i);
      String surl = files.getSURL();
      String sSRM = gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(surl);
      if(!sourceSRM.equals(sSRM)) {
        inputVec.clear();
        inputVec.addElement("Method=CheckAllSourceFromSameSRM2");
        inputVec.addElement
			("Reason=sources from multiple SRMs are not allowed");
        inputVec.addElement(" SourceSRM="+sourceSRM);
        inputVec.addElement(" sSRM="+sSRM);
        util.printMessage
			("\nsources from multiple SRMs are not allowed ",logger,silent);
        util.printMessageHException
			("\nsources from multiple SRMs are not allowed ",pIntf);
        util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
        return false;
      }
    }
   }
  }
  else if((request.getModeType().equalsIgnoreCase("put")) ||
		(request.getModeType().equalsIgnoreCase("copy") && !pushMode)) {
    if(!partycopy) {
      if(size > 0) {
        FileInfo files = (FileInfo) fInfo.elementAt(0);
        String surl = files.getTURL();
        sourceSRM = gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(surl);
      }
      for(int i = 1; i < size; i++) {
        FileInfo files = (FileInfo) fInfo.elementAt(i);
        String surl = files.getTURL();
        String sSRM = 
			gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(surl);
        if(!sourceSRM.equals(sSRM)) {
          inputVec.clear();
          inputVec.addElement("Method=CheckAllSourceFromSameSRM2");
          inputVec.addElement
			("Reason=Targets to multiple SRMs are not allowed");
          inputVec.addElement(" SourceSRM="+sourceSRM);
          inputVec.addElement(" sSRM="+sSRM);
          util.printMessage
		("\nTargets to multiple SRMs are not allowed",logger,silent);
          util.printMessageHException
			("\nTargets to multiple SRMs are not allowed",pIntf);
          util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
          return false;
        }
      }
    }
  }
  return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkAllSourceFromSameSRM
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean checkAllSourceFromSameSRM(Vector fInfo) throws Exception {
  
  int size = fInfo.size();

  //check whether gsiftp and SRM sourcesurl are mixed.
  //mixed is not allowed, if found mixed, return false

  int mode = -1;  //0 --- gsiftp, 1 --- srm 
  if(size > 0) {
    FileInfo files = (FileInfo) fInfo.elementAt(0);
    String surl = files.getSURL();
    if(surl.startsWith("gsiftp:")) {
       mode = 0;
    }
    else if(surl.startsWith("srm:")) {
       mode = 1;
    }
  }

  for(int i = 1; i < size; i++) {
    FileInfo files = (FileInfo) fInfo.elementAt(i);
    String surl = files.getSURL();
    if(mode == 0) {
      if(surl.startsWith("srm:")) {
         inputVec.clear();
         inputVec.addElement("Method=CheckAllSourceFromSameSRM");
         inputVec.addElement("Reason=Mixed gsiftp and srm surls are not " +
			"allowed in 3partycopy");
         util.printMessage
			("\nSRM-CLIENT: Mixed gsiftp and srm surls are not allowed in 3partycopy",
				logger,silent);
         util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
         return false;
      }
    }
    else if(mode == 1) {
      if(surl.startsWith("gsiftp:")) {
         inputVec.clear();
         inputVec.addElement("Method=CheckAllSourceFromSameSRM");
         inputVec.addElement("Reason=Mixed gsiftp and srm surls are not " +
			"allowed in 3partycopy");
         util.printMessage
			("\nSRM-CLIENT: Mixed gsiftp and srm surls are not allowed in 3partycopy",
				logger,silent);
         util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
         return false;
      }
    }
  }

  String sourceSRM = "";

  if(size > 0) {
    FileInfo files = (FileInfo) fInfo.elementAt(0);
    String surl = files.getSURL();
    if(surl.startsWith("gsiftp:")) return true;
    sourceSRM = gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(surl);
  }
  for(int i = 1; i < size; i++) {
    FileInfo files = (FileInfo) fInfo.elementAt(i);
    String surl = files.getSURL();
    String sSRM = gov.lbl.adapt.srm.client.util.Util.findServiceSRMFromSURL(surl);
    if(!sourceSRM.equals(sSRM)) {
      inputVec.clear();
      inputVec.addElement("Method=CheckAllSourceFromSameSRM");
      inputVec.addElement("Reason=sources from multiple SRMs " +
			"are not allowed in 3partycopy");
      inputVec.addElement(" SourceSRM="+sourceSRM);
      inputVec.addElement(" sSRM="+sSRM);
      util.printMessage
        ("\nSRM-CLIENT: sources from multiple SRMs are not allowed in "+
		"3partycopy",logger,silent);
      util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
      return false;
    }
  }
  return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// validateURL
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Vector  validateURL(Request request) {
    String tag = "ValidateURL";
    Vector result = new Vector ();

    logMsg("request="+request, tag, null);
    if(request == null) return result;
    
    int numTgtFilesAlreadyLocal=0;
    Vector fInfo = request.getFiles();
    int size = fInfo.size();
    logMsg("request="+request.getModeType()+" size="+size, tag, null);
    if((request.getModeType().equalsIgnoreCase("copy")) && partycopy && !doCopy) 
    {
	//check if all source from same SRM
	try {
	    if(!checkAllSourceFromSameSRM(fInfo)) {
		logMsg("Reason=sources from multiple SRMs are not allowed in 3partycopy", tag, "SRM-CLIENT:");
		showUsage(false);
	    }
	}catch(Exception e) {
	    util.printMessage("\nSRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);
	    util.printEventLogException(_theLogger,"",e);
	    util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
	    //util.printStackTrace(e,logger);
	}
    }

    try {
	//if not recursive is added on June 2, 10 to make get recursive work
	if(!recursive) {
	    if(!checkAllSourceFromSameSRM2(fInfo)) {
		showUsage(false);
	    }
	}
    } catch(Exception e) {
	util.printMessage("\nSRM-CLIENT: Exception from client="+e.getMessage(),logger,silent);
	util.printEventLogException(_theLogger,"",e);
	util.printMessageHException(e.getMessage(),pIntf);
	//util.printStackTrace(e,logger);
    }
    
    for(int i = 0; i < size; i++) {
	boolean skip = false;
	FileInfo f = (FileInfo) fInfo.elementAt(i);
	String surl = f.getSURL();
	String turl = f.getTURL();
	if(request.getModeType().equalsIgnoreCase("releaseFile")) {
	    if(doCopy) {;}
	    if(!surl.startsWith("srm:")) {
		String[] msg = {"RequestMode=ReleaseFile", "Reason=source url is not valid"};
		logMsg(msg, tag, "SRM-CLIENT");
		skip = true;
	    }	
	} else if(request.getModeType().equalsIgnoreCase("dir")) {
	    if(doCopy) {;}
	    if(!surl.startsWith("srm:")) {
		String[] msg = {"RequestMode=dir", "Reason=source url is not valid, skipping this url in the request"};
		logMsg(msg, tag, "\nSRM-CLIENT:");
		skip = true;
	    }	
	} else if((request.getModeType().equalsIgnoreCase("copy")) && pushMode) {
	    if(doCopy) {;}
	    else if((!surl.startsWith("srm:"))|| (!turl.startsWith("srm:"))) { 
		String msg = "Reason=For the copy request with pushMode both surl " + surl +" and turl " + turl + " must start with srm";		    
		logMsg(msg, tag, "\nSRM-CLIENT:");		     
		skip=true; 
	    }	
	} else if((request.getModeType().equalsIgnoreCase("copy")) && partycopy) {
	    if(doCopy) {;}
	    else if((surl.startsWith("srm:"))&& (turl.startsWith("srm:"))) { ; }
	    else if((surl.startsWith("gsiftp:"))&& (turl.startsWith("srm:"))) { ; }		    
	    else if((surl.startsWith("srm:"))&& (turl.startsWith("gsiftp:"))) { ; }		    
	    else if((surl.startsWith("gsiftp:"))&& (turl.startsWith("gsiftp:"))) { ; }		    
	    else {
		String[] msg = {"RequestMode=copy",
				"Reason=For the partycopy request surl must begin" +
				" with either srm:// or gsiftp:// and turl " +  " must start with srm"};
		logMsg(msg, tag, "\nSRM-CLIENT:");
		skip=true; 
	    }	
	} else {
	    if(doCopy) {; }
	    else if(((request.getModeType().equalsIgnoreCase("get"))  || (request.getModeType().equalsIgnoreCase("bringonline"))) 
		    && nooverwrite) 
	    {
		try {
		    String temp = util.parseLocalSourceFileForPath(turl);
		    File localFile = new File(temp);
		    if(localFile.exists()) {
			numTgtFilesAlreadyLocal ++;
			f.setFileStatus("SRM_DUPLICATION_ERROR");
			f.setFileExplanation("path:"+temp);
			f.setDuplicationError(true);
			f.setCompleted(true);
			f.setErrorMessage("File already exists in target");
			//sending the completed files to SRMGetClient to handle
			//not to send these in SrmPrepareToGet or SrmBringOnline
			//and the output is produced at the end with all the files init.
		    }
		}catch(Exception e) {
		    System.out.println("SRM-CLIENT: Exception="+e.getMessage());
		}	    
	    } else if((surl.startsWith("gsiftp:")) || (surl.startsWith("srm:")) ||
		      (surl.startsWith("ftp:")) || (surl.startsWith("http:"))) {
		if(doBringOnline) {; }
		else {
		    if(turl.equals("") && noDownLoad) {; }
		    if(turl.equals("") && submitOnly) {; }
		    //if(turl.equals("") && !targetDir.equals("")) {;}
		    else if(((!turl.startsWith("file:")) && (!turl.startsWith(File.separator))) 			     
			    && (!turl.startsWith("srm:")) && (!turl.startsWith("gsiftp:"))) 
		    {
			String[] msg = {"Reason=invalid turl:" + turl + " for the given surl " + surl,					
					" skipping this file in the request"};
			logMsg(msg, tag, "\nSRM-CLIENT:");
			skip = true;
		    }
		}	    
	    } else if((surl.startsWith("file:") || surl.startsWith(File.separator))){
		if(!turl.startsWith("srm:") && !directGsiFTP) {
		    if(serviceURLGiven && turl.startsWith("gsiftp")) {; }
		    else {
			String[] msg = {"Reason=invalid turl=" + turl,
					"for the given surl " + surl + " skipping this file in the request",
					"Reason=turl must begin with srm:// ",
					"skipping this file in the request",
					"or If you intend to do a direct gsiftp",
					"with the given surl and turl, please use -direct"};
			logMsg(msg, tag, "\nSRM-CLIENT:");
			skip = true;
		    } 
		}
		if(directGsiFTP) {
		    if(!turl.startsWith("gsiftp") && (!turl.startsWith("file") && !turl.startsWith(File.separator))) 
		    {
			logMsg("Reason=invalid turl " + turl + "for the given surl " + surl + " skipping this file in the request",
			       tag, "SRM-CLIENT:");
			skip = true;
		    }
		}
	} else {  	  
		if(!submitOnly) {
		    logMsg("Reason=Given surl is not valid " + surl +" surl should start with gsiftp, ftp, http, srm, file" +
			   " skipping this file in the request", tag, "\nSRM-CLIENT:");
		    skip = true;
		}
	    }
	}
	if(!skip) {
	    if(!submitOnly && requestType.equalsIgnoreCase("Put")) {
		//check source file exists
		try {
		    String path = parseLocalSourceFileForPath(surl);
		    File ff = new File(path);  
		    if(ff.exists()) {
			f.setExpectedSize(""+ff.length());
			result.add(f);
		    }
		    else {
			logMsg("Source file does not exists, skipping this file "+path, tag, "\nSRM-CLIENT:");
		    }
		}catch(Exception e) {
		    util.printMessage("\nSRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);				      
		    util.printEventLogException(_theLogger,"",e);
		    //util.printStackTrace(e,logger);
		}	    
	    } else {
		result.add(f);
	    }
	}//end if(!skip)
    }//end for
    size = result.size();  
    for(int i =0; i < result.size(); i++) { 
	FileInfo f = (FileInfo) result.elementAt(i);
	f.setLabel(i);
    }

    if(result.size() == numTgtFilesAlreadyLocal) {
	allLocalFileExists=true;
    }
    
    logMsg("allLocalFileExists="+allLocalFileExists, tag, null);
    return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//findRequestType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void findRequestType(String surl, String turl, String durl) {
   
   inputVec.clear();
   inputVec.addElement("SURL="+surl);
   inputVec.addElement("TURL="+turl);
   inputVec.addElement("DURL="+durl);
   util.printEventLog(_theLogger,"findRequestType",inputVec,silent,useLog);
   if(doCopy) {
     requestType = "copy";
     if((surl.startsWith("file:") || surl.startsWith("srm") 
                || surl.startsWith(File.separator)) 
		&& turl.startsWith("gsiftp:")) { 
       pushMode = true;
     }
     return;
   }
   else if(doBringOnline && !noDownLoad) {
     requestType="bringonline";
     return;
   }
   else if (doBringOnline && noDownLoad) {
     requestType = "get";
   }
   else if(doReleaseFile) {
     requestType="releaseFile";
   }
   else if(!durl.equals("")) {
     requestType="dir";
   } 
   else if(submitOnly && turl.startsWith("srm")) {
      requestType = "put";
     return;
   }
   else if(submitOnly && surl.startsWith("srm")) {
      requestType = "get";
     return;
   }
   else if(submitOnly && surl.startsWith("gsiftp")) {
      requestType = "get";
     return;
   }
   else if(surl.startsWith("file:")||surl.startsWith(File.separator)) {
     if(turl.startsWith("srm:")) {
       requestType = "put";
     }
     else if(turl.startsWith("file:") || turl.startsWith(File.separator)){
       requestType = "put";
       mayBeDirectGsiFTP=true;
     }
     else if(turl.startsWith("gsiftp:")) {
       requestType = "put";
       mayBeDirectGsiFTP=true;
     }
   }
   else if(turl.startsWith("file:") || turl.startsWith(File.separator)){
     if(surl.startsWith("srm:")) {
       requestType = "get";
     }
     else if(surl.startsWith("file:") || surl.startsWith(File.separator)){
       requestType = "put";
       mayBeDirectGsiFTP=true;
     }
     else if(surl.startsWith("gsiftp:")) {
       requestType = "get";
       mayBeDirectGsiFTP=true;
     }
   }
   else if(turl.equals("") && noDownLoad) {
     requestType = "get";
   }
   else if(surl.startsWith("gsiftp:") && turl.startsWith("gsiftp:")) {
     requestType = "copy";
     directGsiFTP=true;
   }
   else { 
     requestType = "copy";
   }
   inputVec.clear();
   inputVec.addElement("RequestType="+requestType);
   util.printEventLog(_theLogger,"findRequestType",inputVec,silent,useLog);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseLocalSourceFileForPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String parseLocalSourceFileForPath (String str)
  throws Exception {

  if(str.startsWith("file:////")) {
    return str.substring(8);
  }
  else if(str.startsWith("file:///")) {
    return str.substring(7);
  }
  else if(str.startsWith("file://")) {
    return str.substring(6);
  }
  else if(str.startsWith("file:/")) {
    return str.substring(5);
  }
  else if(str.startsWith("/")) {
    String temp = str;
    int idx = 0;
    while(true) {
      if(str.charAt(idx) != File.separatorChar) {
        temp = "/"+str.substring(idx);
        break;
      }
      else {
        idx++;
      }
    }
    return temp;
  }
  else {
    throw new Exception("SRM-CLIENT: parseLocalSourceFileForPath:SURL not in correct format " + str);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doListing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doListing(String surl, String turl, String topLevelDir, 
		       Request req, SRMUtilClient utilClient,
		       boolean mkdirCreationResult,boolean useOverwriteInfo,
		       boolean overwriteFiles) 
throws Exception 
{    
    String tag = "DoListing";
    logMsg("[surl]"+surl+"[turl]"+turl+"[topDir]"+topLevelDir, tag, null);
 
    File f = new File(parseLocalSourceFileForPath(surl));
    if(f.exists()) {
	if(!f.isDirectory()) {
	    logMsg("Reason=Given source is not a directory", tag, null);
	    throw new Exception("\nSRM-CLIENT: Given source is not a directory ");	
	} else {
	    File[] lists = f.listFiles();
	    for(int i = 0;  i< lists.length; i++) {
		File ff = lists[i];
		if(ff.getName().equals(".") || ff.getName().equals("..")) {;}
		else {
		    if(!ff.isDirectory()) {
			FileInfo fInfo = new FileInfo(); 
			fInfo.setSURL(surl+"/"+ff.getName());
			int idx = turl.lastIndexOf("/");
			if(idx != -1) {
			    if(!topLevelDir.equals("")) {
				fInfo.setTURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
				fInfo.setOrigTURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());			    
			    } else {
				fInfo.setTURL(turl.substring(0,idx)+"/"+ff.getName());
				fInfo.setOrigTURL(turl.substring(0,idx)+"/"+ff.getName());
			    }
			}
			fInfo.setOrigSURL(surl+"/"+ff.getName());
			fInfo.overWriteTURL(false);
			fInfo.setOverwriteFiles(!nooverwrite);
			fInfo.setMkdirCreationOk(mkdirCreationResult);
			if(useOverwriteInfo) {
			    fInfo.setOverwriteFiles(overwriteFiles);
			}
			req.addFileInfo(fInfo);		    
		    } else {
			Vector vec = new Vector();
			FileInfo fInfo = new FileInfo();
			int idx = turl.lastIndexOf("/");
			if(idx != -1) {
			    if(!topLevelDir.equals("")) {
				logMsg("Creating mkdir for surl " +turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName(), tag, "\nSRM-CLIENT:");
				fInfo.setSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
				fInfo.setOrigSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());			    
			    } else {
				logMsg("Creating mkdir for surl " + turl.substring(0,idx)+"/"+ff.getName(), tag, "\nSRM-CLIENT:");
				fInfo.setSURL(turl.substring(0,idx)+"/"+ff.getName());
				fInfo.setOrigSURL(turl.substring(0,idx)+"/"+ff.getName());
			    }
			    fInfo.overWriteTURL(false);
			    vec.addElement(fInfo);
			    StringBuffer sCode = new StringBuffer();
			    boolean b = utilClient.doSrmMkdir(vec,sCode,false,false,false,false);
			    if(!topLevelDir.equals("")) {
				doListing(surl+"/"+ff.getName(), turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName()+"/","",req,utilClient,b,false,false);	  
			    } else {
				doListing(surl+"/"+ff.getName(), turl.substring(0,idx)+"/"+ff.getName()+"/","",	req,utilClient,b,false,false);
			    }
			}
		    }
		}
	    }
	}   
    } else {
	logMsg("Reason=Given source does not exists", tag, null);
	throw new Exception("SRM-CLIENT: Given source does not exists ");
    }
}

private Request getReqRecursiveSrcRemoteSRM(String tag, String surl, String turl) throws Exception
{
    Request req = new Request();
    //get request type
    GSSCredential credential = getValidCredential(tag);

    String utilServiceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(surl,serviceURL, serviceHandle,servicePortNumber,1,	    
								       silent,useLog,_theLogger,logger);
    logMsg("Connecting to serviceurl="+utilServiceUrl, tag, "SRM-CLIENT:");	
    
    SRMUtilClient utilClient = new SRMUtilClient(utilServiceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,false,false,
						 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    Vector vec = new Vector ();
    FileInfo fInfo = new FileInfo();
    fInfo.setSURL(surl); 
    vec.addElement(fInfo);
    
    boolean getRecursive =true;
    Vector resultVec = new Vector();
    utilClient.doSrmLs(vec, false, false, recursive, false, 0,0,0,fileStorageType,"",			   
		       statusWaitTimeGiven,getRecursive,textReport,resultVec,surl,turl,false,esgReportType,
		       false,"","");
    for(int i = 0; i < resultVec.size(); i++) {
	fInfo = (FileInfo)resultVec.elementAt(i);
	fInfo.setOverwriteFiles(!nooverwrite);
	req.addFileInfo(fInfo); 
    }    
    return req;
}
    
private Request getReqRecursiveThirdPartyCopy(String tag, String surl, String turl) throws Exception
{
    Request req = new Request();

    GSSCredential credential = getValidCredential(tag);

    SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,true,false,
						 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    Vector vec = new Vector ();
    FileInfo fInfo = new FileInfo();
    fInfo.setSURL(surl); 
    vec.addElement(fInfo);
    Vector resultVec = new Vector();
    MyGlobusURL gurl = new MyGlobusURL(sourceUrl,0);
    String protocol = gurl.getProtocol();
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
    utilClient.doSrmLs(vec, true, false, recursive, false, 0,0,0,fileStorageType,"",
		       statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
		       targetDir,protocol+"://"+host+":"+port+path);
    for(int i = 0; i < resultVec.size(); i++) {
	fInfo = (FileInfo)resultVec.elementAt(i);
	int idx = fInfo.getSURL().lastIndexOf("/");
	if(idx != -1) {
	    int idx2 = fInfo.getSURL().indexOf(protocol+"://"+host+":"+port+path);
	    if(idx2 != -1) {
		String temp = fInfo.getSURL().substring(sourceUrl.length()+2+4);
		String tempturl = parseLocalSourceFileForPath(targetDir);
		fInfo.setTURL(tempturl+"/"+temp);
	    }
	}
	fInfo.setOrigSURL(fInfo.getSURL());
	fInfo.setOrigTURL(fInfo.getTURL());
	fInfo.setOverwriteFiles(!nooverwrite);
	req.addFileInfo(fInfo); 
    }    
    return req;
}

private Request getReqLocalRecursiveCopy(String tag, String surl, String turl)  throws Exception 
{
    
    Request req = new Request();

    GSSCredential credential = getValidCredential(tag);

    //check if target directory already exists
    String ttemp = parseLocalSourceFileForPath(turl);
    File f = new File(ttemp);
    if(f.exists()) {
	int idx = surl.lastIndexOf("/");
	if(idx != -1) {
	    String tt = surl.substring(idx);
	    turl = turl+tt;
	    targetDir=targetDir+tt;
	} 
    }
    SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,false, true,
						 statusMaxTimeAllowed,statusWaitTime, 
						 storageInfo,proxyType,connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    Vector vec = new Vector ();
    FileInfo fInfo = new FileInfo();
    fInfo.setSURL(surl); 
    vec.addElement(fInfo);
    Vector resultVec = new Vector();
    
    utilClient.doSrmLs(vec, false, true, recursive, false, 0,0,0,fileStorageType,"",
		       statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
		       turl,surl);
    for(int i = 0; i < resultVec.size(); i++) {
	fInfo = (FileInfo)resultVec.elementAt(i);
	fInfo.setSURL("file:///"+fInfo.getSURL());
	int idx = fInfo.getSURL().lastIndexOf("/");
	if(idx != -1) {
	    idx = surl.lastIndexOf("/");
	    if(idx != -1) {
		String ttsurl = surl.substring(0,idx);
		int idx2 = fInfo.getSURL().indexOf(ttsurl);
		if(idx2 != -1) {
		    String temp = fInfo.getSURL().substring(ttsurl.length()+1);
		    String tempturl = parseLocalSourceFileForPath(targetDir);
		    //fInfo.setTURL("file:///"+tempturl+"/"+temp);
		    fInfo.setTURL(tempturl+"/"+temp);
		}
	    }
	}
	fInfo.setOrigSURL(fInfo.getSURL());
	fInfo.setOrigTURL(fInfo.getTURL());
	fInfo.setOverwriteFiles(!nooverwrite);
	req.addFileInfo(fInfo); 
    }   
    return req;
}

private Request getReqDirectRecursivePut(String tag, String surl, String turl) throws Exception {
    logMsg("start", tag, null);
    Request req = new Request();

    GSSCredential credential = getValidCredential(tag);

    SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,false, true,
						 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    Vector vec = new Vector ();
    FileInfo fInfo = new FileInfo();
    fInfo.setSURL(surl); 
    vec.addElement(fInfo);
    Vector resultVec = new Vector();
    MyGlobusURL gurl = new MyGlobusURL(turl,0);
    String protocol = gurl.getProtocol();
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
    String srcPath = parseLocalSourceFileForPath(surl);
    
    utilClient.doSrmLs(vec, false, true, recursive, false, 0,0,0,fileStorageType,"",
		       statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
		       protocol+"://"+host+":"+port+path, srcPath);

    for(int i = 0; i < resultVec.size(); i++) {
	fInfo = (FileInfo)resultVec.elementAt(i);
	
	if(fInfo.getSURL().startsWith("/")) {
	    fInfo.setSURL("file:///"+fInfo.getSURL());
	}
	
	int idx = fInfo.getSURL().lastIndexOf("/");
	if(idx != -1) {
	    //int idx2 = fInfo.getSURL().indexOf(surl);
	    int idx2 = fInfo.getSURL().indexOf(srcPath); //surl can finfo.getsurl() can have //// mismatch
	    System.out.println("src="+fInfo.getSURL()+" idx2="+idx2);
	    if(idx2 != -1) {
		//String temp = fInfo.getSURL().substring(surl.length()+1);
		String temp = fInfo.getSURL().substring(idx2+srcPath.length());
		fInfo.setTURL(protocol+"://"+host+":"+port+path+"/"+temp);
	    }
	}
	
	fInfo.setOrigSURL(fInfo.getSURL());
	fInfo.setOrigTURL(fInfo.getTURL());
	fInfo.setOverwriteFiles(!nooverwrite);
	req.addFileInfo(fInfo); 
    }    

    return req;
}

private Request getReqRemoteRecursivePut(String tag, String surl, String turl) throws Exception {
    Request req = new Request();
    
    GSSCredential credential = getValidCredential(tag);
    
    String utilServiceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(turl,serviceURL,	    
								       serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
    logMsg("Connecting to serviceurl="+utilServiceUrl, tag, "SRM-CLIENT:");
    
    SRMUtilClient utilClient = new SRMUtilClient(utilServiceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,false,false,
						 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    //creating the top level directory
    Vector vec = new Vector();
    FileInfo fInfo = new FileInfo();
    int idx = turl.lastIndexOf("/");
    String topLevelDir="";
    if(idx != -1) {
	if(surl.endsWith("/")) {
	    surl = surl.substring(0,surl.length()-1);
	}
	int idx1 = surl.lastIndexOf("/");
	if(idx1 != -1) {
	    fInfo.setSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
	    fInfo.setOrigSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
	    topLevelDir=surl.substring(idx1+1);
	}
    }
    fInfo.overWriteTURL(false);
    vec.addElement(fInfo);
    StringBuffer sCode = new StringBuffer();
    boolean b = utilClient.doSrmMkdir(vec,sCode,false,false,false,false);
    if(!b) {
	logMsg("TopLevel directory creation for this put request failed "+fInfo.getSURL(), tag, "SRM-CLIENT:");
	if(pIntf == null) {
	    logMsg("StatusExit StatusCode=94", tag, null);
	    System.exit(94);
	}
    }
    doListing (surl,turl,topLevelDir,req,utilClient,true,false,false);
    Vector files = req.getFiles();
    boolean canContinue=true;
    for(int i = 0; i < files.size(); i++) {
	fInfo = (FileInfo) files.elementAt(i);
	if(!fInfo.getMkdirCreationOk()) {
	    logMsg("Mkdir creation is not ok on the remote SRM for this SURL="+fInfo.getOrigTURL(), tag, "\nSRM-CLIENT:"); 
	    canContinue=false;   
	}
    }
    if(!canContinue) {
	logMsg("Please check mkdir creation errors, before continue this put request", tag, null);
	if(pIntf == null) {
	    logMsg("StatusExit StatusCode=94", tag, null);
	    System.exit(94);
	}
    }    
    
    return req;
}

private Request getReqDirectGetMkdir(String tag, String surl, String turl) throws Exception {
    Request req = new Request();

    //added to support 
    //srm-copy(gsiftp,gsiftp://dm.lbl.gov//tmp/rec1/rec2/hello.java) -domkdir
    GSSCredential credential = getValidCredential(tag);

    SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc, credential, _theLogger, logger, pIntf, _debug,silent, 
						 useLog,directGsiFTP,false, statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    
    Vector vec1 = new Vector();
    int iidx = targetUrl.lastIndexOf("/");
    String tempSurl = targetUrl;
    if(iidx != -1) {
	tempSurl = targetUrl.substring(0,iidx);
	}
    MyGlobusURL gurl = new MyGlobusURL(tempSurl,0);
    String protocol = gurl.getProtocol();
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();
    FileInfo fInfo2 = null;
    int firstIndex = 0;
    while(true) {
	iidx = path.indexOf("/",firstIndex);
	//System.out.println(">>>Path="+path + " " + iidx);
	if(iidx != -1) {
	    int iidx2 = path.indexOf("/",(iidx+1));
	    //System.out.println(">>>Path="+path + " " + iidx2);
	    fInfo2 = new FileInfo();
	    if(iidx2 != -1) {
		String temp = path.substring(0,iidx2);
		//System.out.println(">>>TEMP="+temp);
		if(!temp.equals("") && !temp.equals("/")) {
		    fInfo2.setSURL(protocol+"://"+host+":"+port+temp);
		    //System.out.println(">>>SURL="+fInfo2.getSURL());
		    vec1.addElement(fInfo2);
		}
		firstIndex = iidx2;		
	    } else {
		String temp = path;
		//System.out.println(">>>TEMP="+temp);
		if(!temp.equals("") && !temp.equals("/")) {
		    fInfo2.setSURL(protocol+"://"+host+":"+port+temp);
		    //System.out.println(">>>SURL="+fInfo2.getSURL());
			vec1.addElement(fInfo2);
		}
		break;
	    }	    
	    } else {
	    break;
	}
    }
    
    for(int i = 0; i < vec1.size(); i++) {
	FileInfo fInfo = (FileInfo) vec1.elementAt(i);
	System.out.println("Directory to create ..." + fInfo.getSURL());
    }
    
    StringBuffer sbuf2 = new StringBuffer();
    boolean b = utilClient.doSrmMkdir(vec1,sbuf2,false,true,false,true);
    if(b) {
	FileInfo fInfo = new FileInfo();
	fInfo.setSURL(surl);
	fInfo.setOrigSURL(fInfo.getSURL());
	fInfo.setTURL(targetUrl);
	fInfo.setOrigTURL(fInfo.getTURL());
	fInfo.setOverwriteFiles(!nooverwrite);
	req.addFileInfo(fInfo); 	
    } else {
	if(fInfo2 != null) { 
	    logMsg("Directory could not be created " + fInfo2.getSURL(), null, "\nSRM-CLIENT:");	    
	} else {
	    logMsg("Directory could not be created ", null, "\nSRM-CLIENT:");	    
	}
    }    
    return req;
}

private GSSCredential getValidCredential(String tag) {
    GSSCredential credential=null;
    try {
	credential = checkTimeLeft();
    }catch(Exception ee) {
	util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
	util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
	util.printEventLogException(_theLogger,"",ee);
	if(pIntf == null) {
	    logMsg("StatusExit. StatusCode=92", tag, null);
	    util.printHException(ee,pIntf);
	    System.exit(92);
	}
    }
    return credential;
}

private Request getReqDirectRecursiveGet(String tag, String surl, String turl) throws Exception 
{
    Request req = new Request();
    //added to support 
    //srm-copy(gsiftp,gsiftp://dm.lbl.gov//tmp/rec1/rec2) -recursive

    GSSCredential credential = getValidCredential(tag);
    
    SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
						 credential, _theLogger, logger, pIntf, _debug,silent, useLog,directGsiFTP,false,
						 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
						 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
						 delegationNeeded,numRetry,retryTimeOut);
    Vector vec = new Vector ();
    FileInfo fInfo = new FileInfo();
    fInfo.setSURL(surl); 
    vec.addElement(fInfo);

    Vector resultVec = new Vector();

    MyGlobusURL gurl = new MyGlobusURL(sourceUrl,0);
    String protocol = gurl.getProtocol();
    String host = gurl.getHost();
    int port = gurl.getPort();
    String path = gurl.getPath();

    //need to create the top directory in the recursive list if necessary
    Vector vec1 = new Vector();
    FileInfo fInfo2 = new FileInfo();
    int iidx = surl.lastIndexOf("/");
    String tempSurl = surl;
    if(iidx != -1) {
	tempSurl = surl.substring(iidx);
    }

    fInfo2.setSURL(targetDir+"/"+tempSurl);
    targetDir = targetDir+"/"+tempSurl;
    vec1.addElement(fInfo2);
    StringBuffer sbuf2 = new StringBuffer();

    // I am adding turl parsing to determine what mkdir to call
    //boolean b = utilClient.doSrmMkdir(vec1,sbuf2,false,true,false,true);
    boolean b = false;
    if (turl.startsWith("/") || turl.startsWith("file://")) {
	b = utilClient.localMkdir(vec1, sbuf2);
    } else if (turl.startsWith("gsiftp://")) {
	b = utilClient.gsiftpMkdir(vec1, sbuf2, true);
    } else {
	b = utilClient.doSrmMkdir(vec1,sbuf2,false, false, false,true);
    }
 
    //boolean b = utilClient.doSrmMkdir(vec1,sbuf2,tgtOnLocalDisk,true,false,true);
    
    if(b) {
	utilClient.doSrmLs(vec, directGsiFTP, false, recursive, false, 0,0,0,fileStorageType,"",
			   statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
			   targetDir,protocol+"://"+host+":"+port+path);
	for(int i = 0; i < resultVec.size(); i++) {
	    fInfo = (FileInfo)resultVec.elementAt(i);
	    int idx = fInfo.getSURL().lastIndexOf("/");
	    System.out.println("--------> "+fInfo.getSURL());
	    System.out.println("--------> "+sourceUrl);
	    System.out.println("--------> "+targetDir);
	    if(idx != -1) {
		
		int idx2 = fInfo.getSURL().indexOf(protocol+"://"+host+":"+port+path);
		if(idx2 != -1) {
		    // fixing this - Junmin, the substr may get outofbounds exception.
		    //String temp = fInfo.getSURL().substring(sourceUrl.length()+2+4);
		    String temp = fInfo.getSURL().substring(sourceUrl.length());
		    System.out.println("----->! "+targetDir+"/"+temp);
		    fInfo.setTURL(targetDir+"/"+temp);
		}
	    }
	    fInfo.setOrigSURL(fInfo.getSURL());
	    fInfo.setOrigTURL(fInfo.getTURL());
	    fInfo.setOverwriteFiles(!nooverwrite);
	    req.addFileInfo(fInfo); 
	}
    } else {
	util.printMessage("\nSRM-CLIENT: Top level directory could not be created " + fInfo2.getSURL(),logger,silent);
    }    
    return req;
}

private Request getReq(String tag, String surl, String turl) {
    Request req = new Request();

    FileInfo fInfo = new FileInfo(); 
    fInfo.setSURL(surl);
    //fInfo.setTURL(turl+"-"+userName);
    fInfo.setTURL(turl);
    fInfo.setOrigSURL(surl);
    //fInfo.setOrigTURL(turl+"-"+userName);
    fInfo.setOrigTURL(turl);
    fInfo.overWriteTURL(false);
    fInfo.setOverwriteFiles(!nooverwrite);
    //let us not set size.
    //fInfo.setExpectedSize(fileSize);
    req.addFileInfo(fInfo);

    return req;
}
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Request createRequest(String surl, String turl, String durl) throws Exception 			      
{
    String tag = "CreateRequest";

    Properties properties = System.getProperties();
    String userName = properties.getProperty("user.name");

    logMsg("start username="+userName, tag, null);

    System.out.println("[ surl]="+surl);
    System.out.println("[ turl]="+turl);

    Request req = null;
    if(surl.startsWith("srm") && recursive && !doCopy) {
	req = getReqRecursiveSrcRemoteSRM(tag+"1", surl, turl);
    } else if((surl.startsWith("gsiftp") && domkdir && directGsiFTP) || (surl.startsWith("file") && domkdir && directGsiFTP)) {	      
	req = getReqDirectGetMkdir(tag+"2", surl, turl);
    } else if(surl.startsWith("gsiftp") && recursive && directGsiFTP) {
	req = getReqDirectRecursiveGet(tag+"3", surl, turl);
    } else if(surl.startsWith("gsiftp") && turl.startsWith("gsiftp") && recursive && partycopy) {
	req = getReqRecursiveThirdPartyCopy(tag+"4", surl, turl);
    } else if((surl.startsWith("file") || surl.startsWith(File.separator)) && (turl.startsWith("file") || turl.startsWith(File.separator)) && recursive) {	
	req = getReqLocalRecursiveCopy(tag+"5", surl, turl);
    } else if((surl.startsWith("file") || surl.startsWith(File.separator)) && recursive && directGsiFTP) {	          
	req = getReqDirectRecursivePut(tag+"6", surl, turl);
    } else if((surl.startsWith("file") || surl.startsWith(File.separator)) && recursive && turl.startsWith("srm:")) {
	req = getReqRemoteRecursivePut(tag+"7", surl, turl);
    } else {
	req = getReq(tag+"8", surl, turl);
    }
    req.setTargetDir(targetDir);
    
    return req;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//isUrlExists
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isUrlExists () {
  if(srmSurlVec.size() > 0) return true;
  return false;
}

public synchronized void numberOfSpaceAvailableFiles(int num) {
  /*
  if(requestType.equalsIgnoreCase("3partycopy")) { 
     concurrency = num;
     if(tThread != null) {
       tThread.setNewConcurrency(concurrency);
     }
  }
  */
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSrmFirstUrl
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Vector getSrmFirstUrl(boolean irrelevant) 
{			   
    return srmSurlVec;
}

public synchronized Vector getSrmFirstUrl_nouse (boolean firstTime) {
  Vector vec = new Vector ();
  inputVec.clear();
  inputVec.addElement("FirstTimeCall="+firstTime+ 
		      " srmSurlVec.size()="+srmSurlVec.size()+" concurrency="+concurrency);
  util.printEventLog(_theLogger,"getSrmFirstUrl",inputVec,silent,useLog);
  if(firstTime) {
      int size = srmSurlVec.size();
      for(int i = 0; i < size; i++) {
       FileInfo fInfo = (FileInfo) srmSurlVec.elementAt(0); 
       srmSurlVec.remove(0);
       vec.addElement(fInfo);
      }
      return vec;
  } else {
    if(srmSurlVec.size() > 0) {
       FileInfo fInfo = (FileInfo) srmSurlVec.elementAt(0); 
       srmSurlVec.remove(0);
       vec.addElement(fInfo);
       return vec;    
    } else {
      return new Vector();
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmFileFailure
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void srmFileFailure(int idx, String message) {
   fileTransferNGUI.srmFileFailure(idx,message);
   tThread.srmFileFailure();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doLocalFileToFileCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void doLocalFileToFileCopy (FileInfo fInfo) 
	throws Exception {
  
  String surl = fInfo.getSURL();
  String turl = fInfo.getTURL();

  String spath = parseLocalSourceFileForPath(surl);
  String tpath = parseLocalSourceFileForPath(turl);

  try {
    File f = new File(spath);
    if(!f.exists()) { 
        new Exception("Source file does not exists " + spath);
    }
    if(!f.canRead()) { 
        new Exception("Source file has no read permission " + spath);
    }

    fInfo.setActualSize(""+f.length());
    FileInputStream infile = new FileInputStream(spath);
    BufferedReader in = new BufferedReader(new InputStreamReader(infile));

    FileOutputStream outfile = new FileOutputStream(tpath);
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outfile));

    f = new File(tpath);
    if(f.exists()) {
      //if(!fInfo.getOverwriteFiles()) {
      if(nooverwrite) {
        throw new Exception("Target file already exists, " +
			"please use overwrite to overwrite files");
      }
    }
    if(!f.canWrite()) {
        throw new Exception("No permission to create target file" + tpath);
    }

    String ref="";
    while ((ref = in.readLine()) != null) {
        out.write(ref+"\n");       
   }

   if(in != null) in.close();
   if(infile != null) infile.close();
   if(out != null) out.close();
   if(outfile != null) outfile.close();
  }catch(Exception e) {
      throw new Exception(e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void setRequest(Request req) {
  request = req;
  requestType = request.getModeType();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiatePullingFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void initiatePullingFile (FileInfo fInfo) 
{
    String tag = "InitiatePullingFile ";

    if((requestType.equalsIgnoreCase("put") || requestType.equalsIgnoreCase("copy")) && 	
       (fInfo.getSURL().startsWith("file") || fInfo.getSURL().startsWith(File.separator)) && 
       (fInfo.getTURL().startsWith("file") || fInfo.getTURL().startsWith(File.separator)) && recursive) 
    {
       startedProcess ++;
       try {
	   doLocalFileToFileCopy(fInfo);
	   fInfo.setCompleted(true);
	   fInfo.setStatusLabel("Done");
	   requestType="copy"; //faking back again just for the printout
	   request.setModeType("copy");
       }catch(Exception e) {
	   fInfo.setFailed(true);  
	   fInfo.setStatusLabel("Failed");
	   fInfo.setErrorMessage(e.getMessage());
       }
       
       completedProcess ++;
       fileTransferNGUI.incrementCompletedFiles();
       fInfo.setTimeStamp(new Date());
       fInfo.setEndTime(System.currentTimeMillis());
       return;
   }

    if(requestType.equalsIgnoreCase("put") && (fInfo.getTURL().startsWith("file") ||        
					       fInfo.getTURL().startsWith(File.separator))) {
	startedProcess ++;
	try {
	    doLocalFileToFileCopy(fInfo);
	    fInfo.setCompleted(true);
	    fInfo.setStatusLabel("Done");
	    requestType="copy"; //faking back again just for the printout
	    request.setModeType("copy");
	}catch(Exception e) {
	    fInfo.setFailed(true);  
	    fInfo.setStatusLabel("Failed");
	    fInfo.setErrorMessage(e.getMessage());
	}
	
	completedProcess ++;
	fileTransferNGUI.incrementCompletedFiles();
	fInfo.setTimeStamp(new Date());
	fInfo.setEndTime(System.currentTimeMillis());
	return;
    }

    if(requestType.equalsIgnoreCase("get") && noDownLoad)  {
	startedProcess ++;
	completedProcess ++;
	fInfo.setCompleted(true);
	fInfo.setStatusLabel("Done");
	fileTransferNGUI.incrementCompletedFiles();
	fInfo.setTimeStamp(new Date());
	fInfo.setEndTime(System.currentTimeMillis());
	return;
    }

    logMsg(" SURL="+fInfo.getOrigSURL() + " is to be processed.", tag, "\nSRM-CLIENT");

    if (requestType.equalsIgnoreCase("Put") || requestType.equalsIgnoreCase("3partycopy")) {
	if(!directGsiFTP) {
	    logMsg("received TURL="+fInfo.getTURL(), tag, "SRM-CLIENT:");
	}
    } else {    
	if(!directGsiFTP) {
	    logMsg("received TURL="+fInfo.getSURL(), tag, "SRM-CLIENT:");
	}
    }

    String source = fInfo.getSURL();
    String target = fInfo.getTURL();

    if(adjustTURL) {
	source = doValidateTwoSlashes(source);
	target = doValidateTwoSlashes(target);
    }
    fInfo.setSURL(source); 
    fInfo.setTURL(target); 
    
    srmSurlVec.addElement(fInfo);
    String[] msg = {"Added in the queue for file transfer " + srmSurlVec.size(), 
		    "StartProcess="+startedProcess+ " CompletedProcess="+completedProcess};
    logMsg(msg, tag, null);

    startedProcess ++;
    if(_debug) {
	System.out.println("Added in the queue for file transfer,queue size()= " + srmSurlVec.size());			   
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseXML
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Request parseXML (String fileName) throws NumberFormatException, SRMClientException, Exception {    
    String tag = "Parsing Request";
    logMsg("Parsing request file:"+fileName, tag, "\nSRM-CLINET");

    XMLParseRequest xmlParse = new XMLParseRequest(fileName,_theLogger,silent,useLog);
    Request req = xmlParse.getRequest();
    Vector filesVec = req.getFiles();
    if(concurrency > filesVec.size()) 
	concurrency = filesVec.size();
    String ssurl = "";
    String tturl = "";
    String tempTargetDir="";
    if(!targetDir.equals("")) {
	tempTargetDir =  parseLocalSourceFileForPath(targetDir);
    }
    
    for(int i = 0; i < filesVec.size(); i++) {
	FileInfo fInfo = (FileInfo) filesVec.elementAt(i);	
	if(ssurl.startsWith("gsiftp")) {
	    fInfo.use(ssurl);
	} else if(tturl.startsWith("gsiftp")) {	    
	    fInfo.use(tturl);
	}
    }
    
    if(filesVec.size() > 0) {
	FileInfo fInfo = (FileInfo) filesVec.elementAt(0);
	ssurl = fInfo.getSURL();
	tturl = fInfo.getTURL();
	if(tturl.equals("") && !targetDir.equals("")) {
	    int idx = ssurl.lastIndexOf("/");
	    if(idx != -1) {
		tturl="file:///"+tempTargetDir+"/"+ssurl.substring(idx+1);
	    }
	    fInfo.setTURL(tturl);
	}
	if(ssurl.startsWith("gsiftp")) {
	    MyGlobusURL gurl = new MyGlobusURL(ssurl,0);
	    String path = gurl.getPath();
	    String host = gurl.getHost();
	    int port = gurl.getPort();
	    ssurl = "gsiftp://"+host+":"+port+path;
	    fInfo.setSURL(ssurl);
	    if(!path.startsWith("//")) {
		ssurl = "gsiftp://"+host+":"+port+"/"+path;
		fInfo.setSURL(ssurl);
	    }
	}
	if(tturl.startsWith("gsiftp")) {
	    MyGlobusURL gurl = new MyGlobusURL(tturl,0);
	    String path = gurl.getPath();
	    String host = gurl.getHost();
	    int port = gurl.getPort();
	    tturl = "gsiftp://"+host+":"+port+path;
	    fInfo.setTURL(tturl);
	    if(!path.startsWith("//")) {
		tturl = "gsiftp://"+host+":"+port+"/"+path;
		fInfo.setTURL(tturl);
	    }
	}
    }
    findRequestType(ssurl,tturl,"");
    req.setModeType(requestType);
    //at this point, if surl starts with file: then request we know
    //it is put type request
    if(req.getModeType().equalsIgnoreCase("Get")) {
	if(!doStatus) {
	    FileInfo fInfo = (FileInfo) filesVec.elementAt(0);
	    tturl = fInfo.getTURL();
	    if(targetDir.equals("") && tturl.equals("") && !noDownLoad) {
		String[] msg = {"Reason=targetDir is required for get request",
				"Please provide the targetDir"};
		logMsg(msg, tag, "SRM-CLIENT");
		showUsage(false);
	    }
	    req.setTargetDir(targetDir);
	}
    }
    
    req.getFileInfo();
    
    if(req.getModeType().equalsIgnoreCase("put") && recursive) {
	Request modReq = new Request();
	Vector files = req.getFiles ();
	for(int i = 0; i < files.size(); i++) {
	    FileInfo fInfo = (FileInfo) files.elementAt(i);

	    GSSCredential credential = getValidCredential(tag);
	    
	    String utilServiceUrl = gov.lbl.adapt.srm.client.util.Util.getServiceUrl(fInfo.getTURL(),	 
									       serviceURL, serviceHandle,servicePortNumber,1,silent,
									       useLog,_theLogger,logger);
	    logMsg("Connecting to serviceurl="+utilServiceUrl, "Parsing Request", "SRM-CLIENT: ");
	    
	    SRMUtilClient utilClient = new SRMUtilClient(utilServiceUrl,uid,userDesc,
							 credential, _theLogger, logger, pIntf, _debug,silent, useLog,false,false,
							 statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
							 connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
							 delegationNeeded,numRetry,retryTimeOut);
	    //creating the top level directory
	    Vector vec = new Vector();
	    FileInfo objFileInfo = new FileInfo();
	    String topLevelDir="";
	    String surl = fInfo.getSURL();
	    String turl = fInfo.getTURL();
	    int idx = turl.lastIndexOf("/");
	    if(idx != -1) {
		if(surl.endsWith("/")) {
		    surl = surl.substring(0,surl.length()-1);
		}
		int idx1 = surl.lastIndexOf("/");
		if(idx1 != -1) {
		    objFileInfo.setSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
		    objFileInfo.setOrigSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
		    topLevelDir=surl.substring(idx1+1);
		}
	    }
	    objFileInfo.overWriteTURL(false);
	    vec.addElement(objFileInfo);
	    StringBuffer sCode = new StringBuffer();
	    boolean b = utilClient.doSrmMkdir(vec,sCode,false,false,false,false);
	    if(!b) {
		logMsg("TopLevel directory creation for this put request failed"+ " " + objFileInfo.getSURL(), "Parsing Request", "SRM-CLIENT:");
		if(pIntf == null) {
		    logMsg("StatusCode=94", "StatusExit", null);
		    System.exit(94);
	       }
	    }
	    doListing (fInfo.getSURL(),fInfo.getTURL(),topLevelDir, modReq,utilClient,true,true,fInfo.getOverwriteFiles());		   
       }
	modReq.setTargetDir(targetDir);
	req =modReq;
	
	req.setModeType("put");
	files = req.getFiles();
	boolean canContinue=true;
	for(int i = 0; i < files.size(); i++) {
	    FileInfo fInfo = (FileInfo) files.elementAt(i);
	    if(!fInfo.getMkdirCreationOk()) {
		logMsg("Mkdir creation is not ok on the remote SRM for this SURL="+ fInfo.getOrigTURL(), tag, "\nSRM-CLIENT");
		canContinue=false;   
	    }
	}
	if(!canContinue) {
	    logMsg("Please check mkdir creation errors, before continue this put request", tag, "\nSRM-CLIENT:");
	    if(pIntf == null) {
		logMsg("StatusCode=94", "StatusExit", null);
		System.exit(94);
	    }
	}
    }
    if(doCopy) {
	requestType = "copy";
    }
    
    return req;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//@@@@@@@  start method implemented for interface SRMClientWindow
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestInformation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestInformation(String resultStatus, String resultExplanation) 
{
   this.resultStatus = resultStatus;
   this.resultExplanation = resultExplanation;
   inputVec.clear(); 
   inputVec.addElement("resultStatus="+resultStatus);
   inputVec.addElement(" resultExplanation="+resultExplanation);
   util.printEventLog
	(_theLogger,"SetRequestInformation.SRMClientN", inputVec,silent,useLog);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestStatus()  {
   return resultStatus;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestExplanation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestExplanation()  {
   return resultExplanation;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestTimedOut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestTimedOut(boolean b) {
  this.timedOut = b;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestTimedOut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getRequestTimedOut() {
  return timedOut;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestDone(boolean b, boolean allFilesFailed) {
  
   String[] msg = {"done="+b, " allfilesfailed="+allFilesFailed};  
   logMsg(msg, "SetRequestDone", null);
   requestDone = b;
   //when allFilesFailed == true this is called with false flag
   //for get,put,copy requests
   //when someFiles are ok it is called with false, from SRMCopyClient,
   //and this is called with true 
   //only for copy requests

   enableTransferButton(b,!allFilesFailed);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// incrementCompletedProcess
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void incrementCompletedProcess() {
  completedProcess++;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void enableTransferButton (boolean b, boolean ok) {
    String tag = "EnableTransferButton";
    String[] msgs = {"b="+b, " ok="+ok, " startedProcess="+startedProcess,
		     " completedProcess="+completedProcess, " requestDone="+requestDone};
    logMsg(msgs, tag, null);

    if(!b || !requestDone || (startedProcess > completedProcess)) {
	return;
    }

    //if(requestDone && startedProcess <= completedProcess) {
    if(srmCopyClient != null) {
	if(doReleaseSpace) {
	    try {
		srmCopyClient.releaseSpace();
	    }catch (Exception e) {
		logMsg("Reason=Could not release SRM space", "tag", "SRM-CLIENT");
	    }
	}
    }
    
    if(!fileTransferNGUI.isReportSaved()) {
	if(tThread != null) {
	    tThread.setDone(true);
	}
	tThread = null;
	if(ok) {
	    logMsg("Reason=Request completed, saving report now", tag, null);
	} else {
	    logMsg("Reason=Request failed, please see the messages, " +"saving report now", tag, null);
	}
	if(outputFile.equals("")) {
	    //outputFile = constructFileName(".xml");		
	} else {
	    //check whether the path is correct
	    int idx = outputFile.lastIndexOf("/");
	    if(idx !=-1) {
		String temp = outputFile.substring(0,idx);
		File f = new File(temp);
		if(!f.isDirectory()) {
		    logMsg("Reason=Given output file dir " + temp + "does not exist", tag, "SRM-CLIENT");
		}
	    }
	}
	fileTransferNGUI.processSaveAction(outputFile,resultStatus,resultExplanation);
	//exit is called after saving report
    } else { 
	if(pIntf == null) {
	    if(resultStatus == null || resultStatus.equals("")) {
		logMsg("StatusCode=0", "StatusExit", null);
		System.exit(0);		    
	    } else {
		int exitCode = util.mapStatusCode(resultStatus);
		logMsg("StatusCode="+exitCode, "StatusExit", null);
		System.exit(exitCode);
	    }
	}
    }
	//}    
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isConfigExists
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isConfigExists () {
 try {
  File f = new File(configFileLocation);
  if(!f.exists()) return false;
 }catch(Exception e) { 
   inputVec.clear();
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"IsConfigExists",inputVec,silent,useLog);
   util.printMessage("\nSRM-CLIENT: Exception from client=" + e.getMessage(), logger,silent);
   //ShowException.logDebugMessage(logger,e);
                  util.printEventLogException(_theLogger,"",e);
   ShowException.showMessageDialog(null, "Exception : " + e.getMessage());
 }
 return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void showUsage (boolean b) {
 if(b) { 
    System.out.println("Usage :\n"+
            "\tsrm-copy <sourceUrl> <targetUrl> [command line options]\n"+
            "\tor srm-copy [command line options] -f <file>\n" +
            "\t  where <file> is the path to the xml file containing\n"+
            "\t  the sourceurl, targerurl information.\n"+
            "\tfor requests with more than one file -f option is used.\n" +
            "\tcommand line options will override the options from conf file\n"+
            "\n"+
            "\t-conf               <path> \n"+
            "\t-f                  <fileName> (input file in xml format)\n"+
            "\t                      either sourceurl and targeturl\n"+
            "\t                      or -f <filename> is needed.\n"+
            "\t-td                 <targetDir> (required only \n"+
            "\t                      for get requests)\n"+
            "\t-maxfilesperrequest <maximum file size per request>\n"+
            "\t                      (if this option is provided, \n"+
			"\t                       it divides the files in\n"+
            "\t                       request and sends as subrequests)\n"+
            "\t                       default: all files in the request\n"+
		    "\t-submitonly          default: false\n" +
		    "\t                      (If true, returns after the request submission.\n"+
			"\t					      Status must be checked seperately.\n" +
		    "\t-serviceurl          <full wsdl service url> \n" +
            "\t                      example srm://host:port/wsdlpath \n"+
            "\t                       (required for requests when source url or \n" +
            "\t                        or target url does not have wsdl information)\n"+
            "\t-numlevels           <valid integer> default:0\n" +
            "\t-checkdiskspace      default: false \n"+
            "\t-copy                (makes srmCopy request explicitly with\n"+
		    "\t                      source url and target url)\n"+
            "\t-bringonline         (does bringonline request)\n" +
            "\t-nowaitonremoteput   default: false (exits after putDone for remotePutCase)\n" +
            "\t-direct              (to perform direct gridftp for get and put operations)\n"+
            "\t-pushmode            (uses push mode while srmCopy request) default:pull\n" +
            "\t-3partycopy          (using 3rd party gridftp functionality to tranfer files for\n"+
			"\t				         copy operations between source url and target url)\n" +
            "\t-recursive           allows recursive get from source url and/or put\n"+
			"\t					    to target url\n"+
            "\t-nodownload          (not to download the file to the user machine)default:false\n" +
            "\t-nooverwrite         default:false\n" +
            "\t-mkdir             default:false (will create the parent directory before put request)\n" +
            //"\t-requesttoken       <request token> (valid request token \n" +
            //"\t                      returned by server.)\n"+
            //"\t-requesttype        <get|put|copy|bringonline>\n" +
            //"\t-releasefile         (releases file)\n" +
            "\t-noreleasefile         (won't releases the file)(default: release file)\n" +
            "\t-noabortonfail         (won't abort the file when there is a failure)(default: abort file)\n" +
            "\t-spacetoken          <space_token> (valid space token \n" +
            "\t                      returned by srm-space request, \n"+
			"\t                      used to put a file in the reserved space,\n"+
		    "\t                      or get a file from the reserved space.)\n"+
            "\t-space_token=        <space_token> (valid space token \n" +
            "\t                      returned by srm-space request, \n"+
			"\t                      used to put a file in the reserved space,\n"+
		    "\t                      or get a file from the reserved space.)\n"+
            //"\t-keepspace         <true | false> default: true\n" +
            //"\t                      (if true, it keep the space, else it releases the space as well.)\n"+
            "\t-reservespace        (reserves new space at the beginning of request) \n" +
            "\t-releasespace        (releases the space upon exit)\n"+
            "\t                      (if -releasespace used in pair with -reservespace\n"+
            "\t                      then it releases the newly created space\n"+
            "\t                      if used in pair with -spacetoken then it releases\n"+
            "\t                      the space associated with the spacetoken)\n"+
            "\t-renewproxy          (renews proxy automatically)default:false\n" +
            "\t                     (if provided, prompts for passwphrase)\n"+
            "\t-delegation         uses delegation (default:no delegation)\n"+
            "\t-proxyfile           <path to proxyfile> \n"+
            "\t                      default:from default user proxy location\n"+
            //"\t-log4jlocation     <path to log4jlocation> default:./logs/log4j_srmclient.properties\n"+
            "\t-usercert            <path to usercert> \n" +
            "\t-userkey             <path to userkey> \n" +
            "\t-userdesc            <user request description> \n" +
            "\t-report              <path to outputfile> (full report)\n" +
            "\t-xmlreport           <path to outputfile> (short report)\n" +
            //"\t-textreport         <true | false> (prints text report at the end)\n" +
            //"\t                      default:false\n"+
            "\t-authid              (user authorization id used in SRM)\n"+
            "\t-storageinfo         [string] this flag is needed just as -storageinfo \n"+  
            "\t                     for the gsi access to the storagesystem and below formatted string is needed for access other than  gsi \n"+ 
            "\t                    for:<source|target|sourcetarget>,login:<string>,\n"+ 
            "\t                    passwd:<string>,projectid:<string>,\n"+
            "\t                    readpasswd:<string>,writepasswd<string>\n"+
	        "\t                    (default:false)\n"+
            "\t-remotetransferinfo <string> needed for extra transfer parameters,\n"+
            "\t                    a formatted input seperated by command\n" + 
            "\t                    is used in the following keywords:\n" + 
            "\t                    buffersize:<number>,dcau:<true|false>,\n" + 
            "\t                    parallelism:<number>,guc:<true|false>\n" + 
            "\t                    protection:<true|false>\n" + 
            "\t-protocolslist       <list of supported protocols>\n"+
			"\t                     default:gsiftp,http,https,ftp\n"+
            "\t-filestoragetype     <'p'|'d'|'v'>default:none\n"+  
            "\t                      (p - Permmanent, d - Durable, v - Volatile)\n"+
            "\t-accesslatency       <online | nearline> default:online\n" +
            "\t-access_latency=     <online | nearline> default:online\n" +
            "\t-retentionpolicy     <replica | custodial | output | none> default:replica\n" + 
            "\t-retention_policy=   <replica | custodial | output | none> default:replica\n" + 
            "\t-spacelifetime       <integer> space life time\n"+ 
            "\t-filelifetime        <integer> file life time in seconds\n"+ 
            "\t-pinlifetime         <integer> pin life time in seconds\n"+ 
            "\t-totalrequesttime    <integer> total request time in seconds\n"+ 
            "\t-spacesize           <integer> to set total space size in bytes\n"+
            "\t-spacegsize          <integer> to set guaranteed size in bytes\n"+
            "\t-gurlcopy            <path to guc script> path to guc script\n"+
            "\t-gucpath             <path to guc script> path to guc script\n"+
            "\t-buffersize          <integer> to set the gridftp buffer size\n" +
            "\t-blocksize          <integer> to set the gridftp block size\n" +
            "\t-concurrency         <integer>(number of concurrent file transfers)default:1\n" +
            "\t-parallelism         <integer>(to set the parallelism for the gridftp transfers)default:1\n" +
            "\t-dcau                default:true\n" +
            "\t-numretry            <integer>(number of retries the client makes if there is any file transfer failure.)default:3\n" +
            "\t-retry_num=          <integer>(number of retries the client makes if there is any file transfer failure.)default:3\n" +
            "\t-retrydelay          <integer in seconds> number of seconds to \n"+
            "\t                        sleep after a file transfer failure before retrying default 60 seconds\n" +
            "\t-connectiontimeout <integer in seconds> (enforce gsiftp connection idle timeout in the given seconds)default:600 \n"+
            "\t-sethttptimeout    <integer in seconds> (enforce SRM/httpg connection timeout and sets client-side http connection timeout in the given seconds)default:600 \n"+

            "\t-statuswaittime      (wait time inbetween to check status in seconds)default:30\n"+
            "\t-threshold           (threshold value for the statustime)default:30\n"+
            "\t-statusmaxtime       (maximum time check status before timeout in seconds)default:600\n"+
            "\t-debug               default:false\n" +
            "\t-adjustturl          default:false\n" +
            "\t-quiet               default:false\n" +
            "\t                        suppress output in the console.\n" +
            "\t                        this option writes the output to logfile default: ./srmclient-event-date-random.log\n" +
            "\t-log                 <path to logfile>\n"+
            "\t-help                show this message.");
 } 
 if(pIntf == null) {
     inputVec.clear();
     inputVec.addElement("StatusCode=93");
     util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
     System.exit(93); 
 } else {
     pIntf.setCompleted(false);
 }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getConfigFileLocation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getConfigFileLocation () {
  return configFileLocation;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getProperties
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Properties getProperties() {
  return properties;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setConfig
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setConfig (XMLParseConfig config) {
  pConfig = config;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setPassword
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setPassword(String str) {
  _password = str;
  boolean b = false;
  if(_password != null && _password.trim().length() > 0) {
    b = true;
  }
  if(tThread != null) {
    tThread.setRenew(b);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getPassword
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getPassword() {
  return _password;
}


public int getMaximumFilesPerRequest () {
  return maxFilesPerRequest;
}

public void setTotalFilesPerRequest(int maxSize) {
  totalFilesPerRequest = maxSize;
}

public int getTotalFilesPerRequest() {
  return totalFilesPerRequest;
}

public void setTotalSubRequest(int maxSize) {
  totalSubRequest = maxSize;
}

public int getTotalSubRequest() {
  return totalSubRequest; 
}

public void addRequestToken(String rid) {
  ridList.addElement(rid);
}

public Vector getRequestToken() {
  return ridList;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getCredential
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential getCredential() throws SRMClientException{

   boolean useProxy=true;
   String ukey="";
   String ucert="";
   String proxyPath = properties.getProperty("proxy-file");

   String msg = "GetCredential";

   try {
       if(proxyPath != null && !proxyPath.startsWith("Enter")) {
	   //SRMClientN.logMsg("proxy="+proxyPath, msg, "SRM-CLIENT");
	   mycred = gov.lbl.adapt.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");	          
       } else {
	   ukey = properties.getProperty("user-key");
	   ucert = properties.getProperty("user-cert");
	   
	   if ((ukey != null && !ukey.startsWith("Enter")) && ((ucert != null && !ucert.startsWith("Enter")))) {	    
	       useProxy=false;
	       SRMClientN.logMsg("cert="+ucert+" key="+ukey, msg, "SRM-CLIENT");	   
	   
	       if(_password.equals("")) { 
		   String line = PasswordField.readPassword("Enter GRID passphrase: ");
		   _password = line;
	       }
	       mycred = gov.lbl.adapt.srm.client.util.Util.getCredential(ucert,ukey, useProxy,_password);       
	   } else {
	       SRMClientN.logMsg("useDefaultProxy", msg, "SRM-CLIENT");
	       //proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
	       try {
		   proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
	       }catch(Exception ue) {
		   util.printMessage("\nSRM-CLIENT: Exception from client="+
				     ue.getMessage(),logger,silent);
		   util.printEventLogException(_theLogger,"",ue);
		   proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
	       }
	       SRMClientN.logMsg("defaultProxy="+proxyPath, msg, "SRM-CLIENT");
	       
	       mycred = gov.lbl.adapt.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");	     
	   }//end else
       }//end else
   }catch(Exception e) {
       util.printMessage("\nSRM-CLIENT: Exception from client="+
			 e.getMessage(),logger,silent);
       util.printEventLogException(_theLogger,"",e);
   }
   
   if(mycred == null) {
       if(useProxy) {
	   String err = "Could not get credential for proxy " + proxyPath+ " Please check your configuration settings";
	   SRMClientN.logMsg(err, msg, "SRM-CLIENT");
	   
	   throw new SRMClientException("SRM-CLIENT: "+err);    
       } else {
	   String err = "Could not get credential for user-key " + ukey+ "and user-cert " + ucert + " Please check yoru configuration settings";
	   SRMClientN.logMsg(err, msg, "SRM-CLIENT");
	   throw new SRMClientException("SRM-CLIENT:"+err);	
       }
   }
   proxyType = gov.lbl.adapt.srm.client.util.Util.getProxyType(proxyPath,useProxy);
   return mycred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkTimeLeft
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential checkTimeLeft () throws SRMClientException {

  GSSCredential mycred = getCredential();
  try {
    int remainingLifeTime = mycred.getRemainingLifetime();
    if(remainingLifeTime == 0) {
       inputVec.clear();
       inputVec.addElement("User credential expired, please renew your credentials");
       util.printEventLog(_theLogger,"CheckTimeLeft", inputVec,silent,useLog);
        util.printMessage
  	     ("SRM-CLIENT: User Credential expired, " +
	      "please renew" +
           " your credentials",logger,silent);
        throw new SRMClientException
        ("SRM-CLIENT: User Credential expired, please renew your credentials");
    }

    if(remainingLifeTime <= 1800) { //if <= 30 minutes
      if(isRenewProxy()) {
        try { 
          mycred = createProxy(_password);
        }catch(SRMClientException srme) {
             //ShowException.logDebugMessage(logger,srme);
             throw srme;
        }
      }  
      else {
         inputVec.clear();
         inputVec.addElement("Your proxy has only " + remainingLifeTime + 
			" second left. Please renew your proxy.");
         util.printEventLog(_theLogger,"CheckTimeLeft", inputVec,silent,useLog);
         ShowException.showMessageDialog(null, "SRM-CLIENT: Your proxy has only " +
		remainingLifeTime + " second left.\n" + "Please renew your proxy.");
      }
    }
   }catch(Exception e) {
      util.printMessage("\nSRM-CLIENT: Exception from client=" + e.getMessage(), logger,silent);
                  util.printEventLogException(_theLogger,"",e);
      //ShowException.logDebugMessage(logger,e);
      throw new SRMClientException (e.getMessage());
   }

    return mycred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  isRenewProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isRenewProxy() {
   if(_password != null && _password.trim().length() > 0)
     return true;
   return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// validateFrame
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void validateFrame() {
 ;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getFrame
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JFrame getFrame() {
  return null;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLock
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getLock () {
  return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential createProxy (String passwd) throws SRMClientException {

 inputVec.clear();
 util.printEventLog(_theLogger,"CreateProxy", inputVec,silent,useLog);
 util.printMessage("Creating proxy now ...",logger,silent);
 try {
  String userCert = pConfig.getUserCert();
  String userKey = pConfig.getUserKey();
  String proxyFile =  pConfig.getProxyFile();
  gov.lbl.adapt.srm.client.util.Util.createProxy
		(userCert, userKey, proxyFile, passwd);
 }catch(Exception e) {
     inputVec.clear();
     inputVec.addElement("Reason="+e.getMessage());
     util.printEventLog(_theLogger,"CreateProxy",inputVec,silent,useLog);
     util.printMessage("\nSRM-CLIENT: Exception from client=" + e.getMessage(), logger,silent);
     //ShowException.logDebugMessage(logger,e);
                  util.printEventLogException(_theLogger,"",e);
     throw new SRMClientException(e.getMessage());
 }  

 GSSCredential mycred = getCredential();
 if(tThread != null) {
  tThread.setProxy(mycred);
 }
 return mycred;
}



public String doValidateTwoSlashes(String ss) {
    String result = ss;
    try {
	MyGlobusURL test = new MyGlobusURL(ss,0);
	String protocol = test.getProtocol();
	if(protocol.startsWith("file")|| protocol.startsWith(File.separator)){
	    return result;
	}
	String host = test.getHost();
	int port = test.getPort();
	String path = test.getPath();
	String tempPath = path;
	int count = 0;
	while(true) {
	    if(tempPath.startsWith("/")) {
		tempPath = tempPath.substring(1);
		count++;
	    }
	    else {
		break;
	    }
	}
	//result =  protocol+"://"+host+":"+port+"//"+tempPath;
	if(count == 0) {
	    result =  protocol+"://"+host+":"+port+"//"+path;	
	} else if(count == 1) {
	    result =  protocol+"://"+host+":"+port+"/"+path;	
	} else {
	    result =  protocol+"://"+host+":"+port+path;
	}
    }catch(Exception e) {
	logMsg("Not a valid URL/URI format", "DoValidTwoSlashes", "SRM-CLIENT:");
	//e.printStackTrace();
    }
    return result;
}

public static void main (String[] args) {
    _staticClient = new SRMClientN(args,null);
    _staticClient.logMsg("user input: _concurrency="+_staticClient.concurrency+" parallelism="+_staticClient.parallelism, "main", "SRM-CLIENT:");
    _staticClient.init();

    SRMClientN.loadPTM();
    
    Runtime.getRuntime().addShutdownHook(new Thread() 
	{
	    public void run() {
		if (_staticClient._atm != null) {
		    _staticClient._atm.completed();
		}
		System.out.println("========= Good bye! =========");
	    }
	});
    
}

}

