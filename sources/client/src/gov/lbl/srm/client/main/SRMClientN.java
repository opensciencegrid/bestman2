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

package gov.lbl.srm.client.main;

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

import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;
import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.wsdl.*;

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
private int concurrency = 1; 
private int parallelism = 1; 
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
private Vector inputVec = new Vector();
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
private boolean silent=false;
private boolean useLog=false;
private boolean gotNumRetry=false;
private boolean gotParallelism=false;
private boolean gotRetryTimeOut=false;
private boolean gotBufferSize=false;
private boolean gotBlockSize = false;
private boolean gotDcau=false;
private boolean pushMode=false;
private boolean partycopy=false;
private PrintIntf pIntf;

private static Log logger;
private java.util.logging.Logger _theLogger =
        java.util.logging.Logger.getLogger
            (gov.lbl.srm.client.main.SRMClientN.class.getName());
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

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientN(String[] args, PrintIntf pIntf) {


  this.pIntf = pIntf;
  /*
  //find login to set the uid from the env.
 
  Properties props = System.getProperties();
  String userTemp = props.getProperty("user.name");
  if(userTemp != null) { 
    uid = userTemp;
  }
  */

  //if(args.length == 1) {
    //showUsage(true);
  //}


  for(int i = 0; i < args.length; i++) {
    boolean ok = false;

    if(i == 0 && !args[i].startsWith("-")) {
      sourceUrl=args[i];
      ok = true;
    }
    else if(i == 1 && !args[i].startsWith("-")) {
      targetUrl=args[i];
      ok = true;
      simpleSRMCOPY=true;
    }
    else if(args[i].equalsIgnoreCase("-recursive")) {
      recursive = true;
    }
    else if(args[i].equalsIgnoreCase("-nowaitonremoteput")) {
      noWaitOnRemotePut = true;
    }
    else if(args[i].equalsIgnoreCase("-mkdir")) {
      domkdir = true;
    }
    else if(args[i].equalsIgnoreCase("-lite")) {
       ;
    }
    else if(args[i].equalsIgnoreCase("-protocolslist") && i+1 < args.length) {
      protocolsList = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-conf") && i+1 < args.length) {
      configFileLocation = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-numlevels") && i+1 < args.length) {
      try {
       numLevels = Integer.parseInt(args[i+1]);
      }catch (NumberFormatException nfe) {
        util.printMessage("\nSRM-CLIENT: Given numLevels is not valid" +
            numLevels, logger,silent);
        util.printMessage("\nSRM-CLIENT: Given numLevels is not valid" +
            numLevels, pIntf);
        util.printMessage("SRM-CLIENT: Using the default value",logger,silent);
        util.printMessage("SRM-CLIENT: Using the default value",pIntf);
                    util.printEventLogException(_theLogger,"",nfe);
      }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-nogui")) {
      ;
    }
    else if(args[i].equalsIgnoreCase("-reservespace") ||
		args[i].startsWith("-reserveSpace")) {
      doReserveSpace=true;
    }
    else if(args[i].equalsIgnoreCase("-gatewayfriendly")) {
      checkPing=true;
    }
    else if(args[i].equalsIgnoreCase("-direct")) {
      directGsiFTP=true;
    }
    else if(args[i].equalsIgnoreCase("-remotetransferinfo") && 
		i+1 < args.length) {
          remoteTransferInfo = args[i+1];
          i++; 
    }
    else if(args[i].equalsIgnoreCase("-delegation")) {
       if(i+1 < args.length) {
          delegationNeeded = ""+true;
          i++;
       }
       else {
          delegationNeeded = ""+true;
       }
    }
    else if(args[i].equalsIgnoreCase("-storageinfo")) {
      if(i+1 < args.length) {
         if(args[i+1].startsWith("for")) {
           storageInfo = args[i+1];
           i++;
         }
         else {
           storageInfo = ""+true;
         }
      }
      else {
        storageInfo=""+true;
      }
    }
    else if(args[i].equalsIgnoreCase("-threshold") && i+1 < args.length) {
      String temp = args[i+1];
      try {
        threshHoldValue = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {
         util.printEventLogException(_theLogger,"",nfe);
         System.out.println("\nGiven value for -threshold is not valid, " +
		   "using default value " + threshHoldValue);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-statuswaittime") && i+1 < args.length) {
      String temp = args[i+1];
      statusWaitTimeGiven=true;
      try {
        statusWaitTime = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println("\nGiven value for -statuswaittime is not valid, using default value " + 
               statusWaitTime);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-statusmaxtime") && i+1 < args.length) {
      String temp = args[i+1];
      statusMaxTimeGiven=true;
      try {
        statusMaxTimeAllowed = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println("\nGiven value for -statusmaxtime is not valid, using default value " + 
               statusMaxTimeAllowed);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-copy") || args[i].startsWith("-copy")) 
	{
      doCopy=true;
    }
    else if(args[i].equalsIgnoreCase("-nodownload")) {
      noDownLoad=true;
    }
    else if(args[i].equalsIgnoreCase("-bringonline") ||
	  args[i].startsWith("-bringOnline")) {
      doBringOnline=true;
    }
    else if(args[i].equalsIgnoreCase("-pushmode")) {
      pushMode=true;
    }
    else if(args[i].equalsIgnoreCase("-3partycopy")) {
      partycopy=true;
    }
    else if(args[i].equalsIgnoreCase("-releasespace") ||
		(args[i].equalsIgnoreCase("-releasespace"))) {
      doReleaseSpace=true;
    }
    else if(args[i].equalsIgnoreCase("-releasefile")) {
      doReleaseFile=true;
      alsoReleaseFile=true;
    }
    else if(args[i].equalsIgnoreCase("-noreleasefile")) {
      alsoReleaseFile=false;
    }
    else if(args[i].equalsIgnoreCase("-noabortonfail")) {
      noAbortFile=true;
    }
    else if(args[i].equalsIgnoreCase("-requesttoken") && i+1 < args.length) {
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
    }
    else if(args[i].startsWith("-space_token=")) {
      spaceTokenGiven=true;
      int idx = args[i].indexOf("=");
      if(idx != -1) {
        fileToken=args[i].substring(idx+1);
      }
    }
    else if(args[i].equalsIgnoreCase("-requesttype") && i+1 < args.length) {
      doStatus = true;
      requestType = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-requesttoken") && i+1 < args.length) {
      statusToken = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-serviceurl") && i+1 < args.length) {
      serviceUrl = args[i+1];
      serviceURL = args[i+1];
      serviceURLGiven=true;
      i++;
    }
    else if(args[i].equalsIgnoreCase("-submitonly")) {
      submitOnly=true;
    }
    else if(args[i].equalsIgnoreCase("-log") && i+1 < args.length) {
      useLog = true;
      eventLogPath = args[i+1];
      gucEventLogPath = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-report") && i+1 < args.length) {
      outputFile = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-xmlreport") && i+1 < args.length) {
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
    }
    else if(args[i].equalsIgnoreCase("-checkdiskspace")) {
      checkDiskSpace=true;
    }
    else if(args[i].equalsIgnoreCase("-filestoragetype") && i+1 < args.length) {
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
    }
    else if(args[i].equalsIgnoreCase("-retentionpolicy") && i+1 < args.length) {
      retentionPolicyStr = args[i+1].toUpperCase();
      i++;
    }
    else if(args[i].startsWith("-retention_policy=")) {
      int idx = args[i].indexOf("=");
      if(idx != -1) {
        retentionPolicyStr = args[i].substring(idx+1);
      }
    }
    else if(args[i].equalsIgnoreCase("-accesslatency") && i+1 < args.length) {
      accessLatency = args[i+1].toUpperCase();
      i++;
    }
    else if(args[i].startsWith("-access_latency=")) {
      int idx = args[i].indexOf("=");
      if(idx != -1) {
        accessLatency = args[i].substring(idx+1);
      }
    }
   else if(args[i].equalsIgnoreCase("-totalrequesttime") && i+1 < args.length) {
       String temp = args[i+1];
       statusMaxTimeGiven=true;
       try {
         totalRequestTime = Integer.parseInt(temp);
         statusMaxTimeAllowed = Integer.parseInt(temp); 
       }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven Total Request Time valid value :" + temp); 
         System.out.println("Using default value");
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-pinlifetime") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         pinLifeTime = Integer.parseInt(temp);
       }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven PinLifeTime is not valid value :" + temp); 
         System.out.println("Using default value");
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-filelifetime") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         fileLifeTime = Integer.parseInt(temp);
       }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven FileLifeTime is not valid value :" + temp); 
         System.out.println("Using default value");
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-spacelifetime") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         tokenLifetime = Integer.parseInt(temp);
       }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven Space LifeTime is not valid value :" + temp); 
         System.out.println("Using default value");
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-spacesize") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         tokenSize = Long.parseLong(temp);
       }catch(NumberFormatException nfe) {
         util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven tokensize is not valid value :|" + temp+"|"); 
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-spacegsize") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         gSize = Long.parseLong(temp);
       }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
         System.out.println
           ("\nGiven Space guarn size is not valid value :|" + temp+"|"); 
       }
       i++;
    }
    else if(args[i].equalsIgnoreCase("-f") && i+1 < args.length) {
      inputFile = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-maxfilesperrequest") 
			&& i+1 < args.length) { 
      String temp = args[i+1];
      try {
         maxFilesPerRequest = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {
         System.out.println
           ("\nGiven maximum files per request is not valid value :" + temp); 
         maxFilesPerRequest = 0;
         System.out.println("Assuming the default value " + 
				maxFilesPerRequest); 
                    util.printEventLogException(_theLogger,"",nfe);
      }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-s") && i+1 < args.length) {
      sourceUrl = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-t") && i+1 < args.length) {
      targetUrl = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-td") && i+1 < args.length) {
      targetDir = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-userdesc") && i+1 < args.length) {
      userDesc = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-userkey") && i+1 < args.length) {
      userKey = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-usercert") && i+1 < args.length) {
      userCert = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-proxyfile") && i+1 < args.length) {
      proxyFile = args[i+1];
      i++;
    }
    /*
    else if(args[i].equalsIgnoreCase("-log4jlocation") && i+1 < args.length) {
      log4jlocation = args[i+1];
      i++;
    }
    */
    else if(args[i].equalsIgnoreCase("-concurrency") && i+1 < args.length) {
     try { 
        concurrency = Integer.parseInt(args[i+1]);
        i++;
     }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
        System.out.println("Given concurrency value is not valid integer "+
			args[i+1]);
        showUsage(false);
     }
    }
    else if(args[i].equalsIgnoreCase("-parallelism") && i+1 < args.length) {
     try { 
        parallelism = Integer.parseInt(args[i+1]);
        i++;
        gotParallelism=true;
     }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
        System.out.println("Given parallelism value is not valid integer "+
			args[i+1]);
        showUsage(false);
     }
    }
    else if(args[i].equalsIgnoreCase("-debug")) {
      _debug=true;
    }
    else if(args[i].equalsIgnoreCase("-quiet")) {
      silent=true;
    }
    else if(args[i].equalsIgnoreCase("-numretry") && i+1 < args.length) {
     try { 
        numRetry = Integer.parseInt(args[i+1]);
        i++;
        gotNumRetry=true;
     }catch(NumberFormatException nfe) {
        numRetry = 3; //using the default value
     }
    }
    else if(args[i].startsWith("-retry_num=")) {
      int idx = args[i].indexOf("=");
      if(idx != -1) {
       try { 
         String temp = args[i].substring(idx+1);
         numRetry = Integer.parseInt(temp);
         gotNumRetry=true;
       }catch(NumberFormatException nfe) { 
         numRetry = 3; //using the default value
       }
      }
    }
    else if(args[i].equalsIgnoreCase("-retrydelay") && i+1 < args.length) {
     try { 
        retryTimeOut = Integer.parseInt(args[i+1]);
        i++;
        gotRetryTimeOut=true;
     }catch(NumberFormatException nfe) {
        retryTimeOut = 60; //using the default value
     }
    }
    else if(args[i].equalsIgnoreCase("-sethttptimeout") && i+1 < args.length) {
     try { 
        setHTTPConnectionTimeOutAllowed = Integer.parseInt(args[i+1]);
        i++;
        gotHTTPConnectionTimeOut=true;
     }catch(NumberFormatException nfe) {
        setHTTPConnectionTimeOutAllowed = 600; //using the default value
     }
    }
    else if(args[i].equalsIgnoreCase("-connectiontimeout") && i+1 < args.length) {
     try { 
        connectionTimeOutAllowed = Integer.parseInt(args[i+1]);
        i++;
        gotConnectionTimeOut=true;
     }catch(NumberFormatException nfe) {
        connectionTimeOutAllowed = 1800; //using the default value
     }
    }
    else if(args[i].equalsIgnoreCase("-gurlcopy") ||args[i].equalsIgnoreCase("-gucpath")) {
      gucAsked=true;
      if(i+1 < args.length) {
         if(!args[i+1].startsWith("-")) {
           gucScriptPath = args[i+1];
	       gotGUCScriptPath=true;
           i++;
         }
      }
    }
    else if(args[i].equalsIgnoreCase("-buffersize") && i+1 < args.length) {
     try { 
        bufferSize = Integer.parseInt(args[i+1]);
        i++;
        gotBufferSize=true;
     }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
        System.out.println("Given bufferSize value is not valid integer "+
			args[i+1]);
        showUsage(false);
     }
    }
    else if(args[i].equalsIgnoreCase("-blocksize") && i+1 < args.length) {
     try { 
        blockSize = Integer.parseInt(args[i+1]);
        i++;
        gotBlockSize=true;
     }catch(NumberFormatException nfe) {
                    util.printEventLogException(_theLogger,"",nfe);
        System.out.println("Given blockSize value is not valid integer "+
			args[i+1]);
        showUsage(false);
     }
    }
    else if(args[i].equalsIgnoreCase("-dcau") && i+1 < args.length) {
       Boolean b = new Boolean(args[i+1]);
       dcau = b.booleanValue();
       gotDcau=true;
       i++;
    }
    else if(args[i].equalsIgnoreCase("-renewproxy")) {
      isRenew = true;
    }
    else if(args[i].equalsIgnoreCase("-adjustturl")) {
      adjustTURL = true;
    }
    else if(args[i].equalsIgnoreCase("-nooverwrite")) {
      nooverwrite = true;
    }
    else if(args[i].equalsIgnoreCase("-v2")) {
      ;
    }
    else if(args[i].equalsIgnoreCase("-version")) {
       SRMClientN.printVersion();
    }
    else if(args[i].equalsIgnoreCase("-help")) {
      showUsage(true);
    }
    else {
      boolean b = gov.lbl.srm.client.util.Util.parseSrmCpCommands(args[i],0);
      if(b) ;
      else {
        if(!ok) {
         showUsage (true);
        }
      } 
    }
  }

  Properties sys_config = new Properties(); 

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
      sys_config = gov.lbl.srm.client.util.Util.parsefile(
			configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
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
      System.exit(1);
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
        sys_config = gov.lbl.srm.client.util.Util.parsefile(
			configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
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

  /*
  if(logPath.equals("")) {
    String temp = (String)sys_config.get("logpath");
    if(temp != null) {
      logPath = temp;
    }
    else {
      logPath="./srm-client-detailed-"+detailedLogDate+".log";
    }
  }
  */

  if(gucAsked && gucEventLogPath.equals("")) {
    String temp = (String) sys_config.get("eventlogpath");
    if(temp != null) {
      gucEventLogPath=temp+"-guc";
    }
    else {
      gucEventLogPath = "./srmclient-event-guc-"+detailedLogDate+".log";
    }
  }

  if(silent || useLog) {
   if(eventLogPath.equals("")) {
    String temp = (String) sys_config.get("eventlogpath");
    if(temp != null) {
      eventLogPath = temp;
      gucEventLogPath=temp+"-guc";
    }
    else {
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

  /*
  if(outputFile.equals("")) {
    String temp = (String)sys_config.get("output");
    if(temp != null) {
      outputFile = temp;
    }
    else {
      outputFile="./srm-client-output-"+detailedLogDate+".xml";
    }
  }
  */

  if(silent) {
    /*
    if(logPath.equals("")) {
      System.out.println("\nFor the option quiet, -log is needed to " + 
		" forward the debug output to the detailed logfile");
      System.out.println("Please provide -log <path to logfile> ");
      showUsage(false); 
    }
    */
  }

  ttemp = System.getProperty("log4j.configuration");    
  if(ttemp != null && !ttemp.equals("")) {
     log4jlocation = ttemp;
  }

  //setup log4j configuration.
  /*
  String ttemp = System.getProperty("log4j.configuration");    
  if(ttemp != null && !ttemp.equals("")) {
     log4jlocation = ttemp;
  }else {
     if(log4jlocation.equals("")) { 
       String temp = (String) sys_config.get("log4jlocation");
       if(temp != null) { 
         log4jlocation = temp;
       }
       else {
         log4jlocation = "./logs/log4j_srmclient.properties";
       }
     }
  }
  */

  /*
  if(!logPath.equals("")) {
    //check existence of logfile.
    if(logPath.endsWith("/")) {
       logPath = logPath.substring(0,logPath.length()-1);
    }

    int idx = logPath.lastIndexOf("/");
    if(idx != -1) {
      String logPathDir = logPath.substring(0,idx);
      if(idx == 0) {
        logPathDir = logPath;
      }
 
      try {
      File f = new File(logPath);
      if(f.isDirectory()) {
         System.out.println("Given logpath is a dir, please give " +
			"the full path name with desired log file name" + logPath);
         showUsage(false);
      }
      }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
         System.out.println("\nSRM-CLIENT: Exception from client=" + e.getMessage());
         util.printEventLogException(_theLogger,"logpath",e);
         //e.printStackTrace();
         showUsage(false); 
	  }

      //rewrite the log4j conf file with the new log path
      try {
         String ref;
         FileInputStream file = new FileInputStream(log4jlocation);
         BufferedReader in = new BufferedReader(new InputStreamReader(file));
         FileOutputStream outFile = 
			new FileOutputStream(logPathDir+"/log4j_srmclient.properties");
         BufferedWriter out =
            new BufferedWriter(new OutputStreamWriter(outFile));

         while ((ref= in.readLine()) != null) {
          if(ref.startsWith("log4j.appender.SRMCOPY.File")) {
             out.write("log4j.appender.SRMCOPY.File="+logPath+"\n");
          }
          else {
             out.write(ref+"\n");
          }
        }
        in.close();
        if(file != null) file.close(); 

        out.close();
        if(outFile != null) outFile.close();
      
      }catch(IOException ex) {
                    util.printEventLogException(_theLogger,"",ex);
         util.printMessage("\nSRM-CLIENT: Exception from client="+ex.getMessage());
         util.printEventLogException(_theLogger,"",ex);
         //ex.printStackTrace();
         showUsage(false); 
      }
      log4jlocation=logPathDir+"/log4j_srmclient.properties";
      PropertyConfigurator.configure(log4jlocation);
      ClassLoader cl = this.getClass().getClassLoader();
      try {
        Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientN");
        logger = LogFactory.getLog(c.getName());
      }catch(ClassNotFoundException cnfe) {
                    util.printEventLogException(_theLogger,"",cnfe);
         System.out.println("\nSRM-CLIENT: Exception from client ClassNotFoundException=" + cnfe.getMessage());
         util.printEventLogException(_theLogger,"",cnfe);
      }
    }
  }
  else {
      logPath="./srm-client-detailed-"+detailedLogDate+".log";
  }
  */

  if(_debug) {
   if(!logPath.equals("")) {
     util.printMessage("Log4jlocation " + log4jlocation, logger,silent);
     inputVec.clear();
     inputVec.addElement(log4jlocation);
     util.printEventLog(_theLogger,"Log4jlocation",inputVec,silent,useLog);
   }
  }

  /*
  try {
    if(outputFile.endsWith("/")) {
      outputFile = outputFile.substring(0,outputFile.length()-1);
    }
    int idx = outputFile.lastIndexOf("/");
    if(idx != -1) {
      File outf = new File(outputFile.substring(0,idx));
      if(!outf.exists()) {
        inputVec.clear();
        inputVec.addElement("Given outputFile path did not exists ");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        util.printMessage("Given outputFile path did not exists " + 
		   outputFile.substring(0,idx), logger,silent);
        showUsage(false);
      }
    }
  }catch(Exception e) {
    System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
         util.printEventLogException(_theLogger,"",e);
    //e.printStackTrace();
    showUsage(false);
  }
  */

  try {
  PropertyConfigurator.configure(log4jlocation);
  }catch(Exception ee){}


  //default values such as "Enter a Value"
  properties.put("user-cert", pConfig.getUserCert());
  properties.put("user-key", pConfig.getUserKey());
  properties.put("proxy-file", pConfig.getProxyFile());


  try {

    if(!userKey.equals("")) {
      pConfig.setUserKey(userKey);
      properties.put("user-key",userKey);
    }
    else {
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
    }
    else {
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
       String temp = (String) sys_config.get("BufferSize");
       if(temp != null) {
         int x = 0;
         try {
           x = Integer.parseInt(temp);
           bufferSize = x;
         }catch(NumberFormatException nfe) { }
       }
    }

    if(!gotBlockSize) {
       String temp = (String) sys_config.get("BlockSize");
       if(temp != null) {
         int x = 0;
         try {
           x = Integer.parseInt(temp);
           blockSize = x;
         }catch(NumberFormatException nfe) { }
       }
    }

    if(!gotDcau) {
      String temp = (String) sys_config.get("dcau");
      if(temp != null) {
        Boolean b = new Boolean(temp);
        dcau = b.booleanValue();
      } 
    }

    if(!gotParallelism) {
      String temp = (String) sys_config.get("Parallelism");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           parallelism = x;
        }catch(NumberFormatException nfe) { }
      }
    }

    if(!gotNumRetry) {
      String temp = (String) sys_config.get("NumRetry");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           numRetry = x;
        }catch(NumberFormatException nfe) { }
      }
    }

    String xtemp = (String) sys_config.get("ServicePortNumber");
    if(xtemp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(xtemp);
           servicePortNumber = x;
        }catch(NumberFormatException nfe) { }
    }

    String xxtemp = (String) sys_config.get("ServiceHandle");
    if(xxtemp != null) {
         serviceHandle = xxtemp;
    }

    if(!gotRetryTimeOut) {
      String temp = (String) sys_config.get("RetryDelay");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           retryTimeOut = x;
        }catch(NumberFormatException nfe) { 
           retryTimeOut=60;
		}
      }
     }

    if(!gotHTTPConnectionTimeOut) {
      String temp = (String) sys_config.get("SetHTTPConnectionTimeOut");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           setHTTPConnectionTimeOutAllowed = x;
        }catch(NumberFormatException nfe) { 
           setHTTPConnectionTimeOutAllowed=600;
		}
      }
     }

    if(!gotConnectionTimeOut) {
      String temp = (String) sys_config.get("ConnectionTimeOut");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           connectionTimeOutAllowed = x;
        }catch(NumberFormatException nfe) { 
           connectionTimeOutAllowed=1800;
		}
      }
     }

     if(!statusWaitTimeGiven) {
        String temp = (String) sys_config.get("StatusWaitTime");
        if(temp != null) {
          int x = 0;
          try {
            x = Integer.parseInt(temp); 
            statusWaitTime = x;
          }catch(NumberFormatException nfe) {
             System.out.println("\nSRM-CLIENT: Exception from client=" +
				"Warning StatusWaitTime is not a valid integer " + temp);
             util.printEventLogException(_theLogger,"",nfe);
		  }
        }
     }

     if(!statusMaxTimeGiven) {
        String temp = (String) sys_config.get("StatusMaxTime");
        if(temp != null) {
          int x = 0;
          try {
            x = Integer.parseInt(temp); 
            statusMaxTimeAllowed = x;
          }catch(NumberFormatException nfe) {
             System.out.println("\nSRM-CLIENT: Exception from client=" +
				"Warning StatusMaxTime is not a valid integer " + temp);
             util.printEventLogException(_theLogger,"",nfe);
		  }
        }
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
       }
       else {
         if(userCert.equals("") || userKey.equals("")) {
           System.out.println
		     ("\nSRM-CLIENT: UserCert and UserKey both should be provided");
           inputVec.clear();
           inputVec.addElement("UserCert and Userkey both should be provided");
           util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
           showUsage(false);
         }
       }
     }

     if(isRenew) {
      String v1 = (String) properties.get("user-cert");
      String v2 = (String) properties.get("user-key");
      if(v1.startsWith("Enter") || v2.startsWith("Enter")) {
        System.out.println("\nSRM-CLIENT: If you want to renew proxy automatically,\n "+
         "you need to enter user-cert location and user-key location.");
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically," +
			"you need to enter user-cert location and user-key location");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear();
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      String v3 = (String)properties.get("proxy-file");
      if(v3.startsWith("Enter")) {
        System.out.println("\nSRM-CLIENT: If you want to renew proxy automatically,\n "+
          "please enter your proxy file location.");
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically," +
			"please enter your proxy file location");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear();
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      else {
        String line = PasswordField.readPassword("Enter GRID passphrase: ");
        _password = line;
      }
      //else there is no need to renew proxy.
     }

      String ostype = System.getProperty("os.name");
      if(!targetDir.equals("") && (targetDir.startsWith("file:") || 
			targetDir.startsWith(File.separator))) {
       if(targetDir.endsWith("/")) { 
         targetDir = targetDir.substring(0,targetDir.length()-1);
       }

       if(ostype != null && 
	 (ostype.startsWith("Windows") || ostype.startsWith("windows"))) {
          String tempTargetDir =  parseLocalSourceFileForPath(targetDir);
          File f = new File(tempTargetDir.substring(1));
          if(!f.exists()) {
            util.printMessage
	         ("SRM-CLIENT: Given targetDir does not exists. " + 
					targetDir,logger,silent);
            util.printMessageHException
	         ("\nSRM-CLIENT: Exception from client="+
                   "Given targetDir does not exists. " + targetDir,pIntf);
            inputVec.clear();
            inputVec.addElement("Given targetDir does not exists "+ targetDir);
            util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
            showUsage(false);
          }
          if(!f.isDirectory()) {
            util.printMessage
	         ("SRM-CLIENT: Given targetDir is not a directory " + 
					targetDir,logger,silent);
            util.printMessageHException
	         ("SRM-CLIENT: Given targetDir is not a directory" + 
					targetDir,pIntf);
            inputVec.clear();
            inputVec.addElement("Given targetDir does not exists "+ targetDir);
            util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
            showUsage(false);
          }

          if(!f.canWrite()) {
            util.printMessage
	         ("SRM-CLIENT: Given targetDir has no write permission. " + 
		  	   targetDir,logger,silent);
            util.printMessageHException
	         ("SRM-CLIENT: Given targetDir has no write permission. " + 
			   targetDir,pIntf);
            inputVec.clear();
            inputVec.addElement("Given targetDir has no write permission " + 
				targetDir);
            util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
            showUsage(false);
         }
       }
       else {
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
                    util.printMessage(
			          "SRM-CLIENT: Make directory creation failed " + 
						temp3+temp2,logger,silent);
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
          util.printMessage
	   ("SRM-CLIENT: Given targetDir does not exist. " + 
                tempTargetDir,logger,silent);
          util.printMessageHException
	   ("SRM-CLIENT: Given targetDir does not exists. " + 
                tempTargetDir,pIntf);
        inputVec.clear();
        inputVec.addElement("Given targetDir does not exists "+ tempTargetDir);
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        showUsage(false);
       }
       if(!f.isDirectory()) {
          util.printMessage
	   ("SRM-CLIENT: Given targetDir is not a directory " + 
                tempTargetDir,logger,silent);
          util.printMessageHException
	   ("SRM-CLIENT: Given targetDir is not a directory " + 
                tempTargetDir,pIntf);
        inputVec.clear();
        inputVec.addElement("Given targetDir does not exists "+ tempTargetDir);
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        showUsage(false);
        }
       if(!f.canWrite()) {
          util.printMessage
	   ("SRM-CLIENT: Given targetDir has no write permission. " + 
			targetDir,logger,silent);
          util.printMessageHException
	   ("SRM-CLIENT: Given targetDir has no write permission. " + 
			targetDir,pIntf);
        inputVec.clear();
        inputVec.addElement("Given targetDir has no write permission " + 
				targetDir);
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        showUsage(false);
       }
       }
      }

      String[] surl = null;
      String[] turl = null;
      long[] size = null;

      Vector fileInfo = new Vector();


      if(inputFile.equals("")) { 
          surl = new String[1];
          turl = new String[1];

          if(doReleaseFile) {
            surl[0] = sourceUrl;
            turl[0] = "";
            if(sourceUrl.equals("") && requestToken == null) {
              util.printMessage("\nSRM-CLIENT: ..............................",logger,silent);
              util.printMessage("\nSRM-CLIENT: ..............................",pIntf);
              util.printMessage("SRM-CLIENT: Releasefile options are following",
					logger,silent);
              util.printMessage("SRM-CLIENT: Releasefile options are following",pIntf);
              util.printMessage 
		    	("\n\tPlease provide -s <sourceUrl> \n",logger,silent);
              util.printMessage 
		   	   ("\tor -f <inputfile>\n",logger,silent);
              util.printMessage ("\n\tPlease provide -s <sourceUrl> \n",pIntf);
              util.printMessage ("\tor -f <inputfile>\n",pIntf);
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n",
						logger,silent);
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n",pIntf);
              util.printMessage 
		   	   ("\tand -serviceurl <serviceurl>\n",logger,silent);
              util.printMessage ("\tand -serviceurl <serviceurl>\n",pIntf);
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n",
						logger,silent);
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n", pIntf);
              util.printMessage 
		   	   ("\tand -s <sourceurl>\n",logger,silent);
              util.printMessage ("\tand -s <sourceurl>\n",pIntf);
              showUsage(false);
              if(pIntf != null) return;
            }
            if(sourceUrl.equals("") && serviceUrl.equals("")) {
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n",
						logger,silent);
              util.printMessage 
		    	("\n\tor Please provide -requesttoken <requesttoken> \n", pIntf);
              util.printMessage 
		   	   ("\tand -serviceurl <serviceurl>\n",logger,silent);
              util.printMessage ("\tand -serviceurl <serviceurl>\n",pIntf);
              showUsage(false);
              if(pIntf != null) return;
            }
            else if(!sourceUrl.equals("")) {
              request = createRequest(surl[0],turl[0], "");
            }
            else {
              request = new Request();
            }
            findRequestType(surl[0],turl[0],"");
            request.setModeType(requestType);
          }
          else {
            if(doStatus) {
               if(!sourceUrl.equals("") || !targetUrl.equals("")) {
                 surl[0] = sourceUrl;
                 turl[0] = targetUrl;
                 request = createRequest(surl[0],turl[0], "");
                 //requestType is already given by user for doStatus.
                 //findRequestType(surl[0],turl[0],"");
                 if(requestType.equalsIgnoreCase("get")) {
                   request.setModeType("get");
                 }
                 else if(requestType.equalsIgnoreCase("put")) {
                   request.setModeType("put");
                 }
                 else if(requestType.equalsIgnoreCase("copy")) {
                   request.setModeType("copy");
                 }
                 else if(requestType.equalsIgnoreCase("bringonline")) {
                   request.setModeType("bringonline");
                 }
               }
            }
            else {
              if(!doBringOnline) {
                 if(!submitOnly && !noDownLoad) {
                   if(sourceUrl.equals("") || 
					  (targetUrl.equals("") && targetDir.equals(""))) {
                     util.printMessage 
		     	      ("\nPlease provide <sourceUrl> and <targeturl>\n",
					   logger,silent);
                     util.printMessage 
		     	      ("\nPlease provide <sourceUrl> and <targeturl>\n", pIntf);
                     util.printMessage 
		   	          ("or -f <inputfile>\n",logger,silent);
                     util.printMessage ("or -f <inputfile>\n",pIntf);
                     showUsage(false);
                     if(pIntf != null) return;
                   }
                   if(!targetDir.equals("") && targetUrl.equals("")) {
                      if((sourceUrl.startsWith("gsiftp")) && (targetDir.startsWith("srm")) ||
                        (sourceUrl.startsWith("srm")) && (targetDir.startsWith("srm"))) {
                           targetUrl = targetDir;
                      }
                      else {
                        //June 4, 2010
                        if(sourceUrl.startsWith("srm") &&
                         targetDir.startsWith("file") && recursive) {
                           targetUrl = targetDir;
                           int idx = sourceUrl.lastIndexOf("/");
                           if(idx != -1) {
                             String temp = 
							  parseLocalSourceFileForPath
							  (targetDir+"/"+sourceUrl.substring(idx+1));
                             File f = new File(temp);  
                             if(f.exists()) {
                               targetUrl = 
								targetDir+"/"+sourceUrl.substring(idx+1);
                             }
                           }
                        }
                        else {
                         int idx = sourceUrl.lastIndexOf("/");
                         if(idx != -1) {
                          targetUrl = targetDir+"/"+sourceUrl.substring(idx+1);
                         }
                        }
                      }
                   }
                 }
                 else {
                   if(!submitOnly && sourceUrl.equals("")) {
                     util.printMessage 
		     	      ("\nPlease provide <sourceUrl> \n", logger,silent);
                     util.printMessage 
		     	      ("\nPlease provide <sourceUrl> \n", pIntf);
                     util.printMessage 
		   	          ("or -f <inputfile>\n",logger,silent);
                     util.printMessage 
		   	          ("or -f <inputfile>\n",pIntf);
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
               if(!submitOnly && (targetUrl.startsWith("file:") ||
			         targetUrl.startsWith(File.separator))) { //get
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
                  if(ostype !=null && (ostype.startsWith("Windows") || 
					 ostype.startsWith("windows"))) {
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
                                   util.printMessage(
										"SRM-CLIENT: Make directory creation failed " + 
													temp3+temp2,logger,silent);
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
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir does not exists " +t,
							logger,silent);
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir does not exists " +t, pIntf);
		              showUsage(false);
                     if(pIntf != null) return;
                    }
                    if(!f.isDirectory()) {
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir is not a directory " +t,
							logger,silent);
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir does not exists " +t, pIntf);
		              showUsage(false);
                     if(pIntf != null) return;
                    }
                    if(!temp.equals("/dev/null")) {
                    if(!f.canWrite()) {
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir has no write permission " +
				                 t,logger,silent);
                      util.printMessage
		               ("SRM-CLIENT: Given targetDir has no write permission " +
				                 t,pIntf);
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
                   
               if(!submitOnly && (sourceUrl.startsWith("file:") ||
			         sourceUrl.startsWith(File.separator))) { //put
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
                       util.printMessage("\nSRM-CLIENT: SourceUrl is a directory, " +
						 "please use " +
						 "-recursive option to do the srmPrepareToPut request",
					 	 logger,silent);
                       util.printMessage("\nSRM-CLIENT: SourceUrl is a directory, " +
						 "please use " +
						 "-recursive option to do the srmPrepareToPut request", pIntf);
                       showUsage(false);
                     if(pIntf != null) return;
                     }
                     if(recursive && !f.isDirectory()) {
                       util.printMessage("\nSRM-CLIENT: SourceUrl is not a directory. ", 
					 	 logger,silent);
                       util.printMessage("\nSRM-CLIENT: SourceUrl is a directory. " , pIntf);
                       showUsage(false);
                     if(pIntf != null) return;
                     }
                  }
               }
               else if(sourceUrl.startsWith("gsiftp") &&
                 targetUrl.startsWith("gsiftp")) { 

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
      } 
      else {
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
                       util.printMessage(
			             "SRM-CLIENT: Make directory creation failed " + 
						    temp3+temp2,logger,silent);
                       System.exit(1);
                      }
                      util.printMessage("SRM-CLIENT: directory created " + 
							f.getName(),logger,silent);
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


      if(!doStatus) {
        requestType = request.getModeType();
      }
      else {
        if(!requestType.equalsIgnoreCase("Get") && 
          !requestType.equalsIgnoreCase("Put") && 
          !requestType.equalsIgnoreCase("Copy") && 
          !requestType.equalsIgnoreCase("BringOnline"))
	    {
           util.printMessage("\nSRM-CLIENT: Given requestType is not valid " +
				requestType, logger,silent);
           util.printMessage("\nSRM-CLIENT: Given requestType is not valid " ,
				pIntf);
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
        inputVec.clear();
        inputVec.addElement("-reservespace option is currently " +
			"only allowed for ");
        inputVec.addElement(" only request type put");
        util.printEventLog(_theLogger,"RequestType", inputVec,silent,useLog);
        util.printMessage("\nSRM-CLIENT: -reservespace option is " +
		 "currently only allowed for "+
         "only request type \"put\"", logger,silent);
        showUsage(false);
      }

      if(!doReleaseFile) {
        //util.printMessage("\nRequestType " + requestType, logger,silent);
        inputVec.clear();
        inputVec.addElement(requestType);
        util.printEventLog(_theLogger,"RequestType", inputVec,silent,useLog);
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
          inputVec.clear();
          inputVec.addElement(""+fileInfo.size());
          util.printEventLog(_theLogger,"Total files in the request", 
				inputVec,silent,useLog);
          if(_debug) {   
            util.printMessage
			("SRM-CLIENT: Totalfiles in the request : " +
				fileInfo.size(),logger,silent);
            util.printMessage
			("SRM-CLIENT: Totalfiles in the request : " + fileInfo.size(),pIntf);
          }
      }
      String temp = "";
      String tempType ="surlType";


      if(fileInfo.size() > 0) {
          FileInfo fInfo = (FileInfo) fileInfo.elementAt(0);
          if(doCopy) {
              temp = fInfo.getSURL();
              if(temp.startsWith("srm:")) {
                overrideserviceurl = true;
              }
              else {
                overrideserviceurl = false;
              }
          }
          else if(requestType.equalsIgnoreCase("releaseFile")) {
              temp = fInfo.getSURL();
              overrideserviceurl = true;
          }
          else if((requestType.equalsIgnoreCase("put"))) {
               temp = fInfo.getTURL(); 
               tempType = "turlType";
               if(serviceURLGiven) {
                  int idx = temp.indexOf("?SFN");
                  if(idx != -1) {
                     String aaa = temp.substring(0,idx);  
                     if(aaa.equals(serviceUrl)) {
                       overrideserviceurl = true;
                     }
                     else {
                       remotePutCase=true;
		     }
                  }
                  else {
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
               }
               else {
                 if(temp.startsWith("srm:")) {
                   overrideserviceurl = true;
                 }
               }
          }
          else if((requestType.equalsIgnoreCase("copy"))) {
              if((fInfo.getSURL().startsWith("srm") &&
                 fInfo.getTURL().startsWith("gsiftp")) ||
                 (fInfo.getSURL().startsWith("srm") &&
                   ((fInfo.getTURL().startsWith(File.separator)) ||
                   fInfo.getTURL().startsWith("file")))) {
                  pushMode = true;
              }
              if(fInfo.getSURL().startsWith("gsiftp") &&
                  fInfo.getTURL().startsWith("gsiftp")) {
                  partycopy=true;
              }
              if(pushMode || partycopy || doBringOnline) {
                 if(partycopy && fInfo.getSURL().startsWith("srm")) {
                   temp = fInfo.getSURL(); 
                 }
                 else if(partycopy && fInfo.getSURL().startsWith("gsiftp") &&
			fInfo.getTURL().startsWith("srm")) {
                   temp = fInfo.getTURL(); 
                   tempType = "turlType";
                 }
                 else if(partycopy && fInfo.getSURL().startsWith("gsiftp") &&
			fInfo.getTURL().startsWith("gsiftp")) {
                    temp = fInfo.getSURL(); 
                    directGsiFTP=true;
                 }
                 else {
                   temp = fInfo.getSURL(); 
                 }
              } 
              else {  
                 temp = fInfo.getTURL(); 
                 tempType = "turlType";
              }
              if(!mayBeDirectGsiFTP) {
                if(partycopy && temp.startsWith("srm")) {
                  overrideserviceurl = true;
                }
                if(partycopy && temp.startsWith("gsiftp")) {
                  overrideserviceurl = false;
                }
                else {
                  if(!serviceURLGiven) {
                    overrideserviceurl = true;
                  }
                }
              }
          }
          else {
             //for get type.
             temp = fInfo.getSURL();
             if(temp.startsWith("srm:")) {
               if(!serviceURLGiven) {
                  overrideserviceurl = true;
               }
             }
          }
      }
      else {  
         if(doReleaseFile) {
           if(requestToken != null) {
             if(!serviceUrl.equals("")) { ; }
             else  {
               util.printMessage("\nNo files in the request for transfer",
		   	     logger,silent);
               util.printMessage("Cannot proceed further, please check input",
			     logger,silent);
               util.printMessage("\nNo files in the request for transfer", pIntf);
               util.printMessage("Cannot proceed further, please check input", pIntf);
               showUsage(false);
                     if(pIntf != null) return;
             }
           } 
           else {
             util.printMessage("\nNo files in the request for transfer",
		   	   logger,silent);
             util.printMessage("Cannot proceed further, please check input",
			   logger,silent);
             util.printMessage("\nNo files in the request for transfer", pIntf);
             util.printMessage("Cannot proceed further, please check input",
			   pIntf);
             showUsage(false);
                     if(pIntf != null) return;
           }
         }
         else if(doStatus) { 
            if(statusToken != null && !statusToken.equals("")) {
               if(serviceUrl.equals("")) {
                 util.printMessage("\nPlease provide the -serviceurl",
		   	      logger,silent);
                 util.printMessage("\nPlease provide the -serviceurl", pIntf);
                 showUsage(false);
                     if(pIntf != null) return;
               }
            }
            else {
              util.printMessage("\nPlease provide the status token.",
		   	    logger,silent);
              util.printMessage("\nPlease provide the status token.", pIntf);
              showUsage(false);
                     if(pIntf != null) return;
            }
		 }
         else {
           util.printMessage("\nNo files in the request for transfer",
			logger,silent);
           util.printMessage("Cannot proceed further, please check input",
			logger,silent);
           util.printMessage("\nNo files in the request for transfer", pIntf);
           util.printMessage("Cannot proceed further, please check input", pIntf);
           showUsage(false);
                     if(pIntf != null) return;

         }
      }

      if(overrideserviceurl) {
          serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
			 temp,serviceURL,
		     serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
          if(serviceUrl == null) showUsage(false);
          for(int i = 0; i < fileInfo.size(); i++) {
            FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
            if(tempType.equals("surlType")) {
              String temp1 = fIntf.getSURL();
              if(partycopy) {
                if(temp1.startsWith("srm:")) {
                  String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1,serviceUrl);
                  fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
                }
              }
              else {
                String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1,serviceUrl);
                fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
              }
            }
            else {
              String temp1 = fIntf.getTURL();
              String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1,serviceUrl);
              fIntf.setTURL(serviceUrl.replace("httpg","srm")+sfn);
            }
          }
       }
       else {
          if(serviceUrl.equals("")) {
           String tt = (String) sys_config.get("ServiceUrl");
           String tt2 = (String) sys_config.get("ServiceURL");
           
           if(tt != null || tt2 != null) {
             if(tt != null) 
               serviceUrl = tt;
             if(tt2 != null) 
               serviceURL = tt;
           }
           else {
            if(!directGsiFTP) {
              util.printMessage 
		        ("\nPlease provide the -serviceurl full SRM service url",
					logger,silent);
              util.printMessage 
		        ("  example:srm://<hostname>:<port>//wsdlpath",logger,silent);
              util.printMessage 
		        ("\nPlease provide the -serviceurl full SRM service url", pIntf);
              util.printMessage 
		        ("  example:srm://<hostname>:<port>//wsdlpath",pIntf);
              showUsage(false);
                     if(pIntf != null) return;
            }
           }
          }
          if(!directGsiFTP) {
            serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(serviceUrl,serviceURL,
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

        fileTransferNGUI = 
	      new SRMClientFileTransferN 
			  (this,tThread, targetDir, concurrency,
			   request, fileInfo, numRetry,
			   _debug,textReport,_theLogger,silent,useLog,pIntf); 
        fileTransferNGUI.processTransferAction();
      }

       GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("SRM-CLIENT:Exception from client="+
                ee.getMessage(),logger,silent);
        util.printMessage("SRM-CLIENT:Exception from client="+
                ee.getMessage(),pIntf);
        util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
         System.exit(92);
        }
        else {
         util.printHException(ee,pIntf);
         pIntf.setCompleted(false);
         return;
        }
     }

      if(!directGsiFTP) {
        serviceUrl = 
            gov.lbl.srm.client.util.Util.getServiceUrl(serviceUrl,serviceURL,
	    serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
     }

      if(_debug) {
      util.printMessage("===================================",logger,silent);
      util.printMessage("===================================",pIntf);
      util.printMessage("BeStMan Client Configuration",logger,silent);
      util.printMessageHL("BeStMan Client Configuration",pIntf);
      inputVec.clear();
      if(!configFileLocation.equals("")) {
        util.printMessage("\tConfFile=" + configFileLocation,logger,silent);
        util.printMessage("\tConfFile=" + configFileLocation,pIntf);
        inputVec.add("ConfFile="+configFileLocation);
      } 
      else {
        util.printMessage("\tConfFile=none",logger,silent);
        util.printMessage("\tConfFile=none",pIntf);
        inputVec.add("ConfFile=none");
      }
      util.printMessage("\tInputFile=" + inputFile,logger,silent);
      util.printMessage("\tInputFile=" + inputFile,pIntf);
      inputVec.add("inputFile="+inputFile);
      //util.printMessage("\tLogPath="+logPath,logger,silent);
      //inputVec.add("LogPath="+logPath);
      //util.printMessage("\tLog4jLocation="+log4jlocation,logger,silent);
      inputVec.add("Log4jLocation="+log4jlocation);
      //util.printMessage("\tOutputFile="+outputFile,logger,silent);
      inputVec.add("OutputFile="+outputFile);
      if(targetDir.equals("")) { 
        util.printMessage("\tTargetDir="+targetDir,logger,silent);
        util.printMessage("\tTargetDir="+targetDir,pIntf);
        inputVec.add("TargetDir="+targetDir);
      }
      util.printMessage("\tProxyFile="+proxyFile,logger,silent);
      util.printMessage("\tProxyFile="+proxyFile,pIntf);
      inputVec.add("ProxyFile="+proxyFile);
      if(!userCert.equals("")) {
        util.printMessage("\tUserCert="+userCert,logger,silent);
        util.printMessage("\tUserCert="+userCert,pIntf);
        inputVec.add("UserCert="+userCert);
      }
      if(!userKey.equals("")) {
        util.printMessage("\tUserKey="+userKey,logger,silent);
        util.printMessage("\tUserKey="+userKey,pIntf);
        inputVec.add("UserKey="+userKey);
      }
      if(maxFilesPerRequest != 0) {
        util.printMessage("\tMaxFilesPerRequest="+maxFilesPerRequest,
			logger,silent);
        util.printMessage("\tMaxFilesPerRequest="+maxFilesPerRequest, pIntf);
        inputVec.add("MaxFilesPerRequest="+maxFilesPerRequest);
      }
      else {
        util.printMessage
   	  ("\tMaxFilesPerRequest=all the files in the input file",
				logger,silent);
        util.printMessage
	  ("\tMaxFilesPerRequest=all the files in the input file", pIntf);
        inputVec.add("\tMaxFilesPerRequest=all the files in the input file");
      }
      util.printMessage("\tserviceUrl=" + serviceUrl,logger,silent);
      util.printMessage("\tserviceUrl=" + serviceUrl,pIntf);
      inputVec.add("serviceUrl="+serviceUrl);
      util.printMessage("\tDebug ON=" + _debug,logger,silent);
      util.printMessage("\tDebug ON=" + _debug,pIntf);
      inputVec.add("DebugON="+_debug);
      util.printMessage("\tQuiet ON=" + silent,logger, silent);
      util.printMessage("\tQuiet ON=" + silent,pIntf);
      inputVec.add("QuietON="+silent);
      util.printMessage("\tPushMode=" + pushMode,  logger, silent);
      util.printMessage("\tPushMode=" + pushMode,  pIntf);
      inputVec.add("PushMode="+pushMode);
      util.printMessage("\t3PartyCopy=" + partycopy,  logger, silent);
      util.printMessage("\t3PartyCopy=" + partycopy,  pIntf);
      inputVec.add("3PartyCopy="+partycopy);
      util.printMessage("\tRenew proxy=" +isRenew,logger,silent);
      util.printMessage("\tRenew proxy=" +isRenew,pIntf);
      inputVec.add("RenewProxy="+isRenew); 
      util.printMessage("\tKeepSpace=" +keepSpace,logger,silent);
      util.printMessage("\tKeepSpace=" +keepSpace,pIntf);
      inputVec.add("KeepSpace="+keepSpace);
      }
      if(tokenType == 'd') {
        fileStorageType = TFileStorageType.DURABLE; 
      }
      else if(tokenType == 'p') {
        fileStorageType = TFileStorageType.PERMANENT; 
      }
      else if(tokenType == 'v') {
        fileStorageType = TFileStorageType.VOLATILE; 
      }
      
      if(_debug) {
        if(fileStorageType != null) {
         util.printMessage("\tFileStorageType=" +
			fileStorageType.toString(),logger,silent);
         util.printMessage("\tFileStorageType=" +
			fileStorageType.toString(),pIntf);
         inputVec.add("FileStorageType="+fileStorageType);
        }
        else {
         util.printMessage("\tFileStorageType=null",logger,silent);
         util.printMessage("\tFileStorageType=null",pIntf);
         inputVec.add("FileStorageType=null");
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
          util.printMessage("\tRetentionPolicy=" +
			retentionPolicyInfo.getRetentionPolicy(),logger,silent);
          util.printMessage("\tRetentionPolicy=" +
			retentionPolicyInfo.getRetentionPolicy(),pIntf);
          inputVec.add("RetentionPolicy="+
			retentionPolicyInfo.getRetentionPolicy());
          util.printMessage("\tAccessLatency=" +
			retentionPolicyInfo.getAccessLatency(),logger,silent);
          util.printMessage("\tAccessLatency=" +
			retentionPolicyInfo.getAccessLatency(),pIntf);
          inputVec.add("AccessLatency="+retentionPolicyInfo.getAccessLatency());
        }
        if(!storageInfo.equals("")) {
          parseStorageInfo(storageInfo);
        }
      }
      if(_debug) {
      if(!doReleaseFile) {
        util.printMessage("\tAuthId=" + uid,logger,silent);
        util.printMessage("\tAuthId=" + uid,pIntf);
        inputVec.add("AuthId="+uid);
        util.printMessage("\tdoReserveSpace=" + doReserveSpace,logger,silent);
        util.printMessage("\tdoReserveSpace=" + doReserveSpace,pIntf);
        inputVec.add("doReserveSpace="+doReserveSpace);
        util.printMessage("\tdoReleaseSpace=" + doReleaseSpace,logger,silent);
        util.printMessage("\tdoReleaseSpace=" + doReleaseSpace,pIntf);
        inputVec.add("doReleaseSpace="+doReleaseSpace);
        if(fileToken != null) {
          util.printMessage("\tSpaceToken=" + fileToken, logger,silent);
          util.printMessage("\tSpaceToken=" + fileToken, pIntf);
          inputVec.add("SpaceToken="+fileToken);
        }
        util.printMessage("\tBufferSize=" +bufferSize,logger,silent);
        util.printMessage("\tBufferSize=" +bufferSize,pIntf);
        util.printMessage("\tBlockSize=" +blockSize,logger,silent);
        util.printMessage("\tBlockSize=" +blockSize,pIntf);
        inputVec.add("BufferSize="+bufferSize);
        inputVec.add("BlockSize="+blockSize);
        util.printMessage("\tParallelism=" +parallelism,logger,silent);
        util.printMessage("\tParallelism=" +parallelism,pIntf);
        inputVec.add("Parallelism="+parallelism);
        util.printMessage("\tNumRetry=" +numRetry,logger,silent);
        util.printMessage("\tNumRetry=" +numRetry,pIntf);
        inputVec.add("NumRetry="+numRetry);
        util.printMessage("\tRetryTimeOut=" +retryTimeOut,logger,silent);
        util.printMessage("\tRetryTimeOut=" +retryTimeOut,pIntf);
        inputVec.add("RetryTimeOut="+retryTimeOut);
        util.printMessage("\tOverwrite=" +!nooverwrite,logger,silent);
        util.printMessage("\tOverwrite=" +!nooverwrite,pIntf);
        inputVec.add("Overwrite="+!nooverwrite);
        if(statusMaxTimeAllowed != -1) {
          util.printMessage("\tStatus Maximum time allowed=" +
			statusMaxTimeAllowed + " seconds ", logger,silent);
          util.printMessage("\tStatus Maximum time allowed=" +
			statusMaxTimeAllowed + " seconds ", pIntf);
          inputVec.add("StatusMaximumTimeAllowed="+statusMaxTimeAllowed + 
				" seconds");
        }
        else {
          util.printMessage("\tStatus Maximum time allowed=unlimited", logger,silent);
          util.printMessage("\tStatus Maximum time allowed=unlimited", pIntf);
          inputVec.add("StatusMaximumTimeAllowed=unlimited");
        }
        util.printMessage("\tStatus Wait time =" +statusWaitTime + " seconds ",
				logger,silent);
        util.printMessage("\tStatus Wait time =" +statusWaitTime + " seconds ",pIntf);
        inputVec.add("StatusWaitTime="+statusWaitTime + " second");
        if(totalRequestTime != 0) {
         util.printMessage("\tTotalRequestTime=" + totalRequestTime,
				logger,silent);
         util.printMessage("\tTotalRequestTime=" + totalRequestTime, pIntf);
         inputVec.add("TotalRequestTime="+totalRequestTime);
        }
        if(fileLifeTime != 0) {
          util.printMessage("\tFileLifeTime=" + fileLifeTime,logger,silent);
          util.printMessage("\tFileLifeTime=" + fileLifeTime,pIntf);
          inputVec.add("FileLifeTime="+fileLifeTime);
        }
        if(pinLifeTime != 0) {
          util.printMessage("\tPinLifeTime=" + pinLifeTime,logger,silent);
          util.printMessage("\tPinLifeTime=" + pinLifeTime,pIntf);
          inputVec.add("PinLifeTime="+pinLifeTime);
        }
        if(tokenLifetime != 0) {
          util.printMessage("\tTokenLifeTime=" + tokenLifetime,logger,silent);
          util.printMessage("\tTokenLifeTime=" + tokenLifetime,pIntf);
          inputVec.add("TokenLifeTime="+tokenLifetime);
        }
        else {
          util.printMessage("\tUsing default TokenLifeTime ",logger,silent);
          util.printMessage("\tUsing default TokenLifeTime ",pIntf);
          inputVec.add("TokenLifeTime=default");
        }
        if(tokenSize != 0) {
          util.printMessage("\tTokenSize=" + tokenSize,logger,silent);
          util.printMessage("\tTokenSize=" + tokenSize,pIntf);
          inputVec.add("TokenSize="+tokenSize);
        } 
        if(gSize != 0) {
          util.printMessage("\tGuaranteedSize=" + gSize,logger,silent);
          util.printMessage("\tGuaranteedSize=" + gSize,pIntf);
          inputVec.add("GuaranteedSize="+gSize);
        }
      }
      util.printMessage("\tDirectGsiFTP="+directGsiFTP,logger,silent);
      util.printMessage("\tDirectGsiFTP="+directGsiFTP,pIntf);
      inputVec.add("DirectGsiFTP="+directGsiFTP);
      util.printMessage("\tdomkdir="+domkdir,logger,silent);
      util.printMessage("\tdomkdir="+domkdir,pIntf);
      inputVec.add("domkdir="+domkdir);
      util.printMessage("\tconnectiontimeoutAllowed="+connectionTimeOutAllowed,logger,silent);
      util.printMessage("\tsethttptimeoutAllowed="+setHTTPConnectionTimeOutAllowed,logger,silent);
      util.printMessage("===================================",logger,silent);
      util.printMessage("===================================",pIntf);
      }

      util.printEventLog(_theLogger,"INITIALIZATION",inputVec,silent,useLog);

      if(!requestType.equalsIgnoreCase("copy") && !partycopy) {
        if(!directGsiFTP) {
          inputVec.clear();
          inputVec.addElement(serviceUrl);
          util.printMessage("SRM-CLIENT: " + new Date() + " Connecting to " + 
				serviceUrl,logger,silent);
          //util.printMessage("SRM-CLIENT: " + new Date() + " Connecting to " + 
				//serviceUrl,pIntf);
          util.printEventLog(_theLogger,"ServiceUrl Location",inputVec,silent,useLog);
        }
      }

      GSSCredential mycred = getCredential();
      if(_debug) {
      util.printMessage("SRM-CLIENT: " + "user credentials is " +
			mycred.getName().toString(),logger,silent);
      util.printMessage("SRM-CLIENT: " + "user credentials is " +
			mycred.getName().toString(),pIntf);
      }

      if(doReleaseFile) {
        if(requestToken != null || fileInfo.size() > 0) {
          inputVec.clear();
          if(_debug) {
            util.printMessage("\nSRM-CLIENT: Releasing file now.", logger,silent);
            util.printMessage("\nSRM-CLIENT: Releasing file now.", pIntf);
          } 
          util.printEventLog(_theLogger,"Releasing File",inputVec,silent,useLog);

          SRMUtilClient utilClient = new SRMUtilClient
               (serviceUrl,uid,userDesc, credential, _theLogger, logger,
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
             inputVec.clear();
             inputVec.addElement("StatusCode=0");
             util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
             System.exit(0);
           } 
           else {
             int exitCode = util.mapStatusCode(resultStatus);
             inputVec.clear();
             inputVec.addElement("StatusCode="+exitCode);
             util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
             System.exit(exitCode);
           }
          }//end if pIntf == null
        }
        else {
          inputVec.clear();
          inputVec.addElement("No request token or surl provided. " +
			"Cannot release file");
          util.printMessage
			("\nSRM-CLIENT: No request token or surl provided. Cannot release file", logger,silent);
          util.printMessage
			("\nSRM-CLIENT: No request token or surl provided. Cannot release file", pIntf);
          util.printEventLog(_theLogger,"Releasing File",inputVec,silent,useLog);
          showUsage(false);
                     if(pIntf != null) return;
        }
      }
      else {
        if(doReserveSpace) {
          //if reservespace is used, ignore fileToken,
          //reserve new space
          fileToken = null; 
        }
        if(doBringOnline) {
          try {
            if(doStatus) {
               inputVec.clear();
               inputVec.addElement("for statusToken=" +
                    statusToken);
               //util.printMessage("\nSRM-CLIENT: Doing srmBringOnline status for " + 
					//statusToken ,logger,silent); 
               util.printEventLog(_theLogger,"SrmBringOnline status",
					inputVec,silent,useLog);
            }
            else {
               inputVec.clear();
               //util.printMessage("\nSRM-CLIENT: Doing srmBringOnline now.",logger,silent); 
               util.printEventLog(_theLogger,"SrmBringOnline",inputVec,silent,useLog);
            }

            srmCopyClient = new SRMBringOnlineClient
			   (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
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
              inputVec.clear();
              inputVec.addElement(e.getMessage());
              util.printMessage("\nSRM-CLIENT: Exception from server=" + e,logger,silent);
              util.printMessageHException("\nSRM-CLIENT: Exception " + e,pIntf);
              util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
              //util.printStackTrace(e,logger);
              requestDone = true;
              enableTransferButton(true,false);
              //e.printStackTrace();
            }
        }
        else if(requestType.equalsIgnoreCase("Get")) {
           try {
             if(doStatus) {
               inputVec.clear();
               inputVec.addElement("for StatusToken=" + statusToken);
               util.printMessage("\nSRM-CLIENT: Doing Get status for " + 
					statusToken ,logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing Get status for " + 
					statusToken ,pIntf); 
               util.printEventLog(_theLogger,"GetStatus",inputVec,silent,useLog);
             }
             else {
               if(!directGsiFTP) {
                 inputVec.clear();
                 //util.printMessage("\nSRM-CLIENT:" + new Date() + " Doing srmGet now.",logger,silent); 
                 util.printEventLog(_theLogger,"SrmGet",inputVec,silent,useLog);
               }
               else {
                 inputVec.clear();
                 if(_debug) {
                   util.printMessage("\nSRM-CLIENT: Doing Direct GsiFTP now.",logger,silent); 
                   util.printMessage("\nSRM-CLIENT: Doing Direct GsiFTP now.",pIntf); 
                 }
                 util.printEventLog(_theLogger,"Direct GsiFTP",inputVec,silent,useLog);
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
             inputVec.clear();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
             util.printMessage("\nSRM-CLIENT: Exception from server=" + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             //util.printStackTrace(e,logger);
             requestDone = true;
             enableTransferButton(true,false);
             //e.printStackTrace();
           }
        }
        else if(requestType.equalsIgnoreCase("Put")) {
           try {
             if(doStatus) {
               inputVec.clear();
               inputVec.addElement("status for statusToken=" + statusToken);
               util.printMessage("\nSRM-CLIENT : " + "Doing srmPut status for " + statusToken , logger,silent); 
               util.printMessage("\nSRM-CLIENT : " + "Doing srmPut status for " + statusToken , pIntf); 
               util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
             }
             else {
               if(!directGsiFTP) { 
                 inputVec.clear();
                 //util.printMessage("\nSRM-CLIENT: " + "Doing srmPut now.",logger,silent); 
                 util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
               }
               else {
                 inputVec.clear();
                 //util.printMessage("\nSRM-CLIENT: " + "Doing Direct GsiFTP now.",logger,silent); 
                 util.printMessage("\nSRM-CLIENT: " + "Doing Direct GsiFTP now.",pIntf); 
                 util.printEventLog(_theLogger,"Doing direct GsiFTP now",
						inputVec,silent,useLog);
               }
             }
 
             /*
             System.out.println(">>>Total Memory ="+
				Runtime.getRuntime().totalMemory());
             System.out.println(">>>Free Memory ="+
				Runtime.getRuntime().freeMemory());
             System.out.println(">>>Memory in use="+
				(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
             */

             if(doReserveSpace) {
               for(int i = 0; i < fileInfo.size(); i++) {
                FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
                tokenSize = tokenSize+fIntf.getExpectedSize2();   
               }
               tokenSize = tokenSize*2; //just for safer
               gSize = tokenSize;
             }
             srmCopyClient = new SRMPutClient
		       (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
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
             inputVec.clear();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
             util.printMessageHException("\nSRM-CLIENT: Exception from server=" + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             //util.printStackTrace(e,logger);
             requestDone = true;
             enableTransferButton(true,false);
             //e.printStackTrace();
          }
        }
        else if(requestType.equalsIgnoreCase("Copy")) {
           try {
             if(doStatus) {
               inputVec.clear();
               inputVec.addElement("for statusToken=" + statusToken);
               util.printMessage("\nSRM-CLIENT: Doing Copy status for " + 
					statusToken ,logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing Copy status for " + 
					statusToken ,pIntf); 
               util.printEventLog(_theLogger,"Copy Status",inputVec,silent,useLog);
             }
             if(partycopy) {
               inputVec.clear();
               util.printMessage("\nSRM-CLIENT: Doing 3partycopy now.",
			logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing 3partycopy now.",pIntf); 
               util.printEventLog(_theLogger,"Doing 3partycopy",inputVec,silent,useLog);
             }
             else {
               inputVec.clear();
               util.printEventLog(_theLogger,"SrmCopy",inputVec,silent,useLog);
             } 


             srmCopyClient = new SRMCopyClient
		      (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
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
             inputVec.clear();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
             util.printMessageHException("\nSRM-CLIENT: Exception from server=" + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             //util.printStackTrace(e,logger);
             requestDone = true;
             enableTransferButton(true,false);
             //e.printStackTrace();
           }
        }
      }

      if(doStatus) {
        srmCopyClient.doStatusEnquiry();
      }
      else {
        if(!doReleaseFile) {
          if(allLocalFileExists) {
            srmCopyClient.performTransfer(true);
          }
          else {
            srmCopyClient.performTransfer(false);
          }
        }
      }
   }catch(Exception e) {
     //util.printStackTrace(e,logger);
     int idx = e.getMessage().indexOf(
		"java.lang.reflect.InvocationTargetException");
     if(idx != -1) {
        util.printMessage("SRM-CLIENT: " + e.getMessage(),logger,silent);
        util.printMessage(
            "SRM-CLIENT: Server exception, please contact server admin",
				logger,silent);
     }
     else {
         util.printMessage("SRM-CLIENT:Exception from client="+e.getMessage(),
				logger,silent);
         util.printMessageHException("SRM-CLIENT:Exception from client="+
				e.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",e);
     }
     if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
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
      sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(ssurl);
    }
    if(!ssurl.equals("") && !ssurl.startsWith("gsiftp")) {
    for(int i = 1; i < size; i++) {
      FileInfo files = (FileInfo) fInfo.elementAt(i);
      String surl = files.getSURL();
      String sSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
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
        sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
      }
      for(int i = 1; i < size; i++) {
        FileInfo files = (FileInfo) fInfo.elementAt(i);
        String surl = files.getTURL();
        String sSRM = 
			gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
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
    sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
  }
  for(int i = 1; i < size; i++) {
    FileInfo files = (FileInfo) fInfo.elementAt(i);
    String surl = files.getSURL();
    String sSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
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
  Vector result = new Vector ();

  inputVec.clear();
  inputVec.addElement("request="+request);
  util.printEventLog(_theLogger,"validateURL",inputVec,silent,useLog);

  if(request == null) return result;

  int localFileExists=0;
  Vector fInfo = request.getFiles();
  int size = fInfo.size();
  if((request.getModeType().equalsIgnoreCase("copy")) && 
		partycopy && !doCopy) {
    //check if all source from same SRM
   try {
    if(!checkAllSourceFromSameSRM(fInfo)) {
       inputVec.clear();
       inputVec.addElement
		("Reason=sources from multiple SRMs are not allowed in 3partycopy");
       util.printMessage("SRM-CLIENT: sources from multiple SRMs are not allowed " +
			"in 3partycopy", logger,true);
       util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
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
  }catch(Exception e) {
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
         inputVec.clear();
         inputVec.addElement("RequestMode=ReleaseFile");
         inputVec.addElement("Reason=source url is not valid");
         util.printMessage("\nSRM-CLIENT: source url is not valid " + surl, logger,silent);
         util.printMessageHException("\nSRM-CLIENT: source url is not valid " + surl, pIntf);
         util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
         skip = true;
       }
    }
    else if(request.getModeType().equalsIgnoreCase("dir")) {
       if(doCopy) {;}
       if(!surl.startsWith("srm:")) {
         inputVec.clear();
         inputVec.addElement("RequestMode=dir");
         inputVec.addElement
			("Reason=source url is not valid, skipping this url in the request");
         util.printMessage("\nSRM-CLIENT: source url is not valid " + surl, logger,silent);
         util.printMessage("\nSRM-CLIENT: skipping this url in the request", logger,silent); 
         util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
         skip = true;
       }
    }
    else if((request.getModeType().equalsIgnoreCase("copy")) && pushMode) {
       if(doCopy) {;}
       else if((!surl.startsWith("srm:"))|| (!turl.startsWith("srm:"))) { 
        inputVec.clear();
        inputVec.addElement("RequestMode=copy");
        inputVec.addElement
			("Reason=For the copy request with pushMode both surl " + surl +
		     " and turl " + turl + " must start with srm");
        util.printMessage("\nSRM-CLIENT: For the copy request with pushMode",logger,silent);
        util.printMessageHException("\nSRM-CLIENT: For the copy request with pushMode",pIntf);
        util.printMessage("SRM-CLIENT: both surl " + surl + 
			" and turl " + turl + " must start with srm", logger, silent);
        util.printMessageHException("SRM-CLIENT: both surl " + surl + 
			" and turl " + turl + " must start with srm", pIntf);
        util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
        skip=true; 
       }
    }
    else if((request.getModeType().equalsIgnoreCase("copy")) && partycopy) {
       if(doCopy) {;}
       else if((surl.startsWith("srm:"))&& (turl.startsWith("srm:"))) { ; }
       else if((surl.startsWith("gsiftp:"))&& 
			(turl.startsWith("srm:"))) { ; }
       else if((surl.startsWith("srm:"))&& 
			(turl.startsWith("gsiftp:"))) { ; }
       else if((surl.startsWith("gsiftp:"))&& 
			(turl.startsWith("gsiftp:"))) { ; }
       else {
         inputVec.clear();
         inputVec.addElement("RequestMode=copy");
         inputVec.addElement
           ("Reason=For the partycopy request surl must begin" +
	    " with either srm:// or gsiftp:// and turl " +  
	    " must start with srm");
         util.printEventLog
                (_theLogger,"ValidateURL",inputVec,silent,useLog);
         util.printMessage
           ("\nSRM-CLIENT: For the partycopy request ",logger,silent);
         util.printMessage
          ("SRM-CLIENT: surl must begin with either srm:// or gsiftp://" 
	    + surl + " and turl " + turl + 
            " must start with srm", logger, silent);
         skip=true; 
       }
    }
    else {
      if(doCopy) {; }
      else if(((request.getModeType().equalsIgnoreCase("get"))  ||
              (request.getModeType().equalsIgnoreCase("bringonline"))) 
                        && nooverwrite) {
         try {
          String temp = util.parseLocalSourceFileForPath(turl);
          File localFile = new File(temp);
          if(localFile.exists()) {
           localFileExists++;
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
      }
      else if((surl.startsWith("gsiftp:")) || (surl.startsWith("srm:")) ||
         (surl.startsWith("ftp:")) || (surl.startsWith("http:"))) {
         if(doBringOnline) {; }
         else {
          if(turl.equals("") && noDownLoad) {; }
          if(turl.equals("") && submitOnly) {; }
          //if(turl.equals("") && !targetDir.equals("")) {;}
          else if(((!turl.startsWith("file:")) 
			&& (!turl.startsWith(File.separator))) 
			&& (!turl.startsWith("srm:")) &&
				(!turl.startsWith("gsiftp:"))) {
           inputVec.clear();
           inputVec.addElement("Reason=turl is not valid " + turl + 
				" for the given surl " + surl + 
				" skipping this file in the request");
           util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
           util.printMessage("\nSRM-CLIENT: turl is not valid " + turl,logger,silent);
           util.printMessage("SRM-CLIENT: for the given surl " + surl,logger,silent);
           util.printMessage("SRM-CLIENT: skipping this file in the request",logger,silent);
           skip = true;
          }
         }
      }
      else if((surl.startsWith("file:") || surl.startsWith(File.separator))){
        if(!turl.startsWith("srm:") && !directGsiFTP) {
          if(serviceURLGiven && turl.startsWith("gsiftp")) {; }
          else {
            inputVec.clear();
            inputVec.addElement("Reason=turl is not valid " + turl + 
		   	   "for the given surl " + surl + " skipping this file in the request");
            inputVec.addElement("Reason=turl must begin with srm:// "); 
            inputVec.addElement("skipping this file in the request");
            inputVec.addElement("or If you intend to do a direct gsiftp");
            inputVec.addElement("with the given surl and turl, please use -direct");
            util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
            util.printMessage("\nSRM-CLIENT: turl is not valid " + turl,logger,silent);
            util.printMessage("SRM-CLIENT: for the given surl " + surl,logger,silent);
            util.printMessage("\nSRM-CLIENT: turl must begin with srm://",logger,silent);
            util.printMessage("SRM-CLIENT: skipping this file in the request",logger,silent);
            util.printMessage("SRM-CLIENT: or If you intend to do a direct gsiftp",
					logger,silent);
            util.printMessage("SRM-CLIENT: with the given surl and turl, please use -direct",
					logger,silent);
            skip = true;
          } 
        }
        if(directGsiFTP) {
          if(!turl.startsWith("gsiftp") && (!turl.startsWith("file")
				&& !turl.startsWith(File.separator))) {
          inputVec.clear();
          inputVec.addElement("Reason=turl is not valid " + turl + 
			"for the given surl " + surl + " skipping this file in the request");
          inputVec.addElement(
			"Reason=turl must begin with gsiftp://  or file://"); 
          util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
          util.printMessage("\nSRM-CLIENT: turl is not valid " + turl,logger,silent);
          util.printMessage("SRM-CLIENT: for the given surl " + surl,logger,silent);
          util.printMessage("\nSRM-CLIENT: turl must begin with gsiftp:// or file://",logger,silent);
          util.printMessage("SRM-CLIENT: skipping this file in the request",logger,silent);
          skip = true;
          }
        }
      }
      else {  

        if(!submitOnly) {
          inputVec.clear();
          inputVec.addElement("Reason=Given surl is not valid " + surl +
			" surl should start with gsiftp, ftp, http, srm, file" +
		    " skipping this file in the request");
          util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
          util.printMessage("\nSRM-CLIENT: Given surl is not valid " + surl, logger,silent);
          util.printMessage
	 	  ("\nSRM-CLIENT: surl should start with gsiftp,ftp,http,srm,file",logger,silent);
          util.printMessage("SRM-CLIENT: skipping this file in the request.",logger,silent);
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
               inputVec.clear();
               inputVec.addElement("Source file does not exists, skipping this file "+path);
               util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
               util.printMessage ("\nSRM-CLIENT: " +
                        "Source file does not exists,skipping this file " + 
                                path,logger,silent);
               util.printMessage ("\nSRM-CLIENT: Source file does not " +
                        "exists,skipping this file " + path,pIntf);
            }
          }catch(Exception e) {
            util.printMessage("\nSRM-CLIENT: Exception from client=" + 
                        e.getMessage(),logger,silent);
            util.printEventLogException(_theLogger,"",e);
            //util.printStackTrace(e,logger);
          }
        }
        else {
          result.add(f);
        }
    }//end if(!skip)
   }//end for
   size = result.size();  
   for(int i =0; i < result.size(); i++) { 
      FileInfo f = (FileInfo) result.elementAt(i);
      f.setLabel(i);
   }
   if(result.size() == localFileExists) {
     allLocalFileExists=true;
   }
   inputVec.clear();
   inputVec.addElement("allLocalFileExists="+allLocalFileExists);
   util.printEventLog(_theLogger,"validateURL",inputVec,silent,useLog);
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
    boolean overwriteFiles) throws Exception {

   File f = new File(parseLocalSourceFileForPath(surl));
   if(f.exists()) {
     if(!f.isDirectory()) {
       inputVec.clear();
       inputVec.addElement("Reason=Given source is not a directory");
       util.printEventLog(_theLogger,"DoListing",inputVec,silent,useLog);
       throw new Exception("\nSRM-CLIENT: Given source is not a directory ");
     }
     else {
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
                 }
                 else {
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
             }
             else {
               Vector vec = new Vector();
               FileInfo fInfo = new FileInfo();
               int idx = turl.lastIndexOf("/");
               if(idx != -1) {
                  if(!topLevelDir.equals("")) {
                     util.printMessage("\nSRM-CLIENT: Creating mkdir for surl " + 
						turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName(),logger,silent);
                     inputVec.clear();
                     inputVec.addElement("Creating mkdir for surl " +
						turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                     util.printEventLog(_theLogger,"DoListing",inputVec,silent,useLog);
                     fInfo.setSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                     fInfo.setOrigSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                  }
                  else {
                    util.printMessage("\nSRM-CLIENT: Creating mkdir for surl " + 
						turl.substring(0,idx)+"/"+ff.getName(),logger,silent);
                    inputVec.clear();
                    inputVec.addElement("Creating mkdir for surl " +
						turl.substring(0,idx)+"/"+ff.getName());
                    util.printEventLog(_theLogger,"DoListing",inputVec,silent,useLog);
                    fInfo.setSURL(turl.substring(0,idx)+"/"+ff.getName());
                    fInfo.setOrigSURL(turl.substring(0,idx)+"/"+ff.getName());
                  }
                 fInfo.overWriteTURL(false);
                 vec.addElement(fInfo);
                 StringBuffer sCode = new StringBuffer();
                 boolean b = utilClient.doSrmMkdir(vec,sCode,false,false,false,false);
                 if(!topLevelDir.equals("")) {
                   doListing(surl+"/"+ff.getName(),
				  	 turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName()+"/","",
						req,utilClient,b,false,false);
                 }
                 else {
                   doListing(surl+"/"+ff.getName(),
				  	 turl.substring(0,idx)+"/"+ff.getName()+"/","",
						req,utilClient,b,false,false);
                 }
               }
             }
           }
        }
     }
   }
   else {
     inputVec.clear();
     inputVec.addElement("Reason=Given source does not exists"); 
     util.printEventLog(_theLogger,"DoListing",inputVec,silent,useLog);
     throw new Exception("SRM-CLIENT: Given source does not exists ");
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Request createRequest(String surl,
   String turl, String durl) throws Exception {

   inputVec.clear();
   util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
   if(_debug) {
     util.printMessage("SRM-CLIENT: Creating request", logger,silent);
   }
   Properties properties = System.getProperties();
   String userName = properties.getProperty("user.name");
   Request req = new Request();
   if(surl.startsWith("srm") && recursive && !doCopy) {
     //get request type
     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }

      String utilServiceUrl = gov.lbl.srm.client.util.Util.getServiceUrl
        (surl,serviceURL, serviceHandle,servicePortNumber,1,
         silent,useLog,_theLogger,logger);
      inputVec.clear();
      inputVec.addElement("Connecting to serviceurl="+utilServiceUrl); 
      util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
      if(_debug) {
        util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " + 
			utilServiceUrl,logger,silent);
      }
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
      utilClient.doSrmLs(vec, false, false,
          recursive, false, 0,0,0,fileStorageType,"",
          statusWaitTimeGiven,getRecursive,textReport,resultVec,surl,turl,false,esgReportType,
			false,"","");
      for(int i = 0; i < resultVec.size(); i++) {
         fInfo = (FileInfo)resultVec.elementAt(i);
         fInfo.setOverwriteFiles(!nooverwrite);
         req.addFileInfo(fInfo); 
      }
   }
   else if((surl.startsWith("gsiftp") && domkdir && directGsiFTP) ||
           (surl.startsWith("file") && domkdir && directGsiFTP)) {
     //added to support 
     //srm-copy(gsiftp,gsiftp://dm.lbl.gov//tmp/rec1/rec2/hello.java) -domkdir
     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+
				ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),
				pIntf);
        util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }
     SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
          credential, _theLogger, logger, pIntf, _debug,silent, 
	  useLog,directGsiFTP,false,
          statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
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
          }
          else {
            String temp = path;
            //System.out.println(">>>TEMP="+temp);
            if(!temp.equals("") && !temp.equals("/")) {
             fInfo2.setSURL(protocol+"://"+host+":"+port+temp);
             //System.out.println(">>>SURL="+fInfo2.getSURL());
             vec1.addElement(fInfo2);
            }
            break;
          }
        }
        else {
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
      }
      else {
        if(fInfo2 != null) { 
        util.printMessage("\nSRM-CLIENT: Directory could not be created " + 
				fInfo2.getSURL(),logger,silent);
        util.printMessage("\nSRM-CLIENT: Directory could not be created " + 
				fInfo2.getSURL(),pIntf);
        }
        else {
        util.printMessage("\nSRM-CLIENT: Directory could not be created " 
				,logger,silent);
        util.printMessage("\nSRM-CLIENT: Directory could not be created " 
				,pIntf);
        }
      }
   }
   else if(surl.startsWith("gsiftp") && recursive && directGsiFTP) {
     //added to support 
     //srm-copy(gsiftp,gsiftp://dm.lbl.gov//tmp/rec1/rec2) -recursive

     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }

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
      boolean b = utilClient.doSrmMkdir(vec1,sbuf2,false,true,false,true);
      if(b) {
      utilClient.doSrmLs(vec, directGsiFTP, false, recursive, false, 0,0,0,fileStorageType,"",
          statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
		  targetDir,protocol+"://"+host+":"+port+path);
      for(int i = 0; i < resultVec.size(); i++) {
         fInfo = (FileInfo)resultVec.elementAt(i);
         int idx = fInfo.getSURL().lastIndexOf("/");
         if(idx != -1) {
            int idx2 = fInfo.getSURL().indexOf(protocol+"://"+host+":"+port+path);
            if(idx2 != -1) {
              String temp = fInfo.getSURL().substring(sourceUrl.length()+2+4);
              fInfo.setTURL(targetDir+"/"+temp);
            }
         }
         fInfo.setOrigSURL(fInfo.getSURL());
         fInfo.setOrigTURL(fInfo.getTURL());
         fInfo.setOverwriteFiles(!nooverwrite);
         req.addFileInfo(fInfo); 
      }
      }
      else {
        util.printMessage("\nSRM-CLIENT: Top level directory could not be created " + fInfo2.getSURL(),logger,silent);
      }
   }
   else if(surl.startsWith("gsiftp") && turl.startsWith("gsiftp") && recursive && partycopy) {

     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }

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
   }
   else if((surl.startsWith("file") || surl.startsWith(File.separator)) 
		&& (turl.startsWith("file") || turl.startsWith(File.separator)) 
		&& recursive) {

     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+
			ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }

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
   }
   else if((surl.startsWith("file") || surl.startsWith(File.separator)) 
			&& recursive && directGsiFTP) {

     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
          System.exit(92);
        }
     }

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
      String tempsurl = parseLocalSourceFileForPath(surl);

      utilClient.doSrmLs(vec, false, true, recursive, false, 0,0,0,fileStorageType,"",
          statusWaitTimeGiven,false,textReport,resultVec,"","",false,esgReportType,true,
		  protocol+"://"+host+":"+port+path,tempsurl);
      for(int i = 0; i < resultVec.size(); i++) {
         fInfo = (FileInfo)resultVec.elementAt(i);
         if(fInfo.getSURL().startsWith("/")) {
           fInfo.setSURL("file:///"+fInfo.getSURL());
         }
         int idx = fInfo.getSURL().lastIndexOf("/");
         if(idx != -1) {
            int idx2 = fInfo.getSURL().indexOf(surl);
            if(idx2 != -1) {
              String temp = fInfo.getSURL().substring(surl.length()+1);
              fInfo.setTURL(protocol+"://"+host+":"+port+path+"/"+temp);
            }
         }
         fInfo.setOrigSURL(fInfo.getSURL());
         fInfo.setOrigTURL(fInfo.getTURL());
         fInfo.setOverwriteFiles(!nooverwrite);
         req.addFileInfo(fInfo); 
       }
   }
   else if((surl.startsWith("file") || surl.startsWith(File.separator)) 
			&& recursive && turl.startsWith("srm:")) {
     GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
          System.exit(92);
        }
        util.printHException(ee,pIntf);
     }

      String utilServiceUrl = 
         gov.lbl.srm.client.util.Util.getServiceUrl(turl,serviceURL,
	 serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
      inputVec.clear();
      inputVec.addElement("Connecting to serviceurl="+utilServiceUrl); 
      util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
      if(_debug) {
        util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " + 
			utilServiceUrl,logger,silent);
      }
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
        inputVec.clear();
        inputVec.addElement("TopLevel directory creation for this put request failed"); 
        util.printEventLog(_theLogger,"CreatingRequest", inputVec,silent,useLog);
        util.printMessage("SRM-CLIENT: TopLevel directory creation for " +
				" this put request failed" + fInfo.getSURL(),logger,silent);
        util.printMessage("SRM-CLIENT: TopLevel directory creation for " +
				" this put request failed" + fInfo.getSURL(),pIntf);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=94");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
          System.exit(94);
        }
      }
      doListing (surl,turl,topLevelDir,req,utilClient,true,false,false);
      Vector files = req.getFiles();
      boolean canContinue=true;
      for(int i = 0; i < files.size(); i++) {
         fInfo = (FileInfo) files.elementAt(i);
	     if(!fInfo.getMkdirCreationOk()) {
           inputVec.clear();
           inputVec.addElement("Mkdir creation is not ok on the remote SRM for " +
			"this SURL="+fInfo.getOrigTURL()); 
           util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
           util.printMessage("\nSRM-CLIENT: Mkdir creation is not ok " +
				"on the remote SRM for this SURL="+
				fInfo.getOrigTURL(),logger,silent);
           canContinue=false;   
         }
      }
      if(!canContinue) {
         inputVec.clear();
         inputVec.addElement("Please check mkdir creation errors, " +
			"before continue this put request");
         util.printEventLog(_theLogger,"CreateRequest",inputVec,silent,useLog);
         util.printMessage
			("\nSRM-CLIENT: Please check mkdir creation errors, before continue this put request", logger,silent);
         util.printMessage
			("\nSRM-CLIENT: Please check mkdir creation errors, before continue this put request", pIntf);
         if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=94");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
           System.exit(94);
         }
      }
   }
   else {
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

public synchronized Vector getSrmFirstUrl (boolean firstTime) {
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
  }
  else {
    if(srmSurlVec.size() > 0) {
       FileInfo fInfo = (FileInfo) srmSurlVec.elementAt(0); 
       srmSurlVec.remove(0);
       vec.addElement(fInfo);
       return vec;
    }
    else {
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

public synchronized void initiatePullingFile (FileInfo fInfo) {

   if((requestType.equalsIgnoreCase("put") || 
		requestType.equalsIgnoreCase("copy")) && 
		(fInfo.getSURL().startsWith("file") || fInfo.getSURL().startsWith(File.separator)) && (fInfo.getTURL().startsWith("file") || fInfo.getTURL().startsWith(File.separator)) && recursive) {
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

   if(requestType.equalsIgnoreCase("put") && 
		(fInfo.getTURL().startsWith("file") || 
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

   inputVec.clear();
   inputVec.addElement("RequestFileStatus For SURL="+fInfo.getOrigSURL() + " is ready.");
   util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
   //if(_debug) {
     util.printMessage("\nSRM-CLIENT: RequestFileStatus for " +
       "SURL="+fInfo.getOrigSURL()+" is Ready.",logger,silent);
     util.printMessage("\nSRM-CLIENT: RequestFileStatus for " +
       "SURL="+fInfo.getOrigSURL()+" is Ready.",pIntf);
   //}
   if(requestType.equalsIgnoreCase("Put") || 
      requestType.equalsIgnoreCase("3partycopy")) {
     if(!directGsiFTP) {
       inputVec.clear();
       inputVec.addElement("received TURL="+fInfo.getTURL());
       util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getTURL(),logger,silent);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getTURL(),pIntf);
     }
   }
   else {
     if(!directGsiFTP) {
       inputVec.clear();
       inputVec.addElement("received TURL="+fInfo.getSURL());
       util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getSURL(),logger,silent);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getSURL(),pIntf);
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
   inputVec.clear();
   inputVec.addElement("Added in the queue for file transfer " + srmSurlVec.size());
   inputVec.addElement("StartProcess="+startedProcess+ " CompletedProcess="+
                completedProcess);
   util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
   startedProcess ++;
   if(_debug) {
     System.out.println("Added in the queue for file transfer,queue size()= " 
			+ srmSurlVec.size());
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseXML
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Request parseXML (String fileName)
    throws NumberFormatException, SRMClientException, Exception {

   inputVec.clear();
   inputVec.addElement("Parsing request file");
   util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
   if(_debug) {
     util.printMessage("\nSRM-CLIENT: Parsing request file " + fileName,logger,silent);
   }
   XMLParseRequest xmlParse = new XMLParseRequest(fileName,_theLogger,silent,useLog);
   Request req = xmlParse.getRequest();
   Vector filesVec = req.getFiles();
   if(concurrency > filesVec.size()) concurrency = filesVec.size();
   String ssurl = "";
   String tturl = "";
   String tempTargetDir="";
   if(!targetDir.equals("")) {
     tempTargetDir =  parseLocalSourceFileForPath(targetDir);
   }

   for(int i = 0; i < filesVec.size(); i++) {
     FileInfo fInfo = (FileInfo) filesVec.elementAt(i);

     if(ssurl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(ssurl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       fInfo.setSURL("gsiftp://"+host+":"+port+path);
       if(!path.startsWith("//")) {
         fInfo.setSURL("gsiftp://"+host+":"+port+"/"+path);
       }
     }
     if(tturl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(tturl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       fInfo.setTURL("gsiftp://"+host+":"+port+path);
       if(!path.startsWith("//")) {
         fInfo.setTURL("gsiftp://"+host+":"+port+"/"+path);
       }
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
        inputVec.clear();
        inputVec.addElement("Reason=targetDir is required for get request");
        inputVec.addElement("Please provide the targetDir");
        util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
        util.printMessage("SRM-CLIENT: targetDir is required for get request.",logger,silent);
        util.printMessage("SRM-CLIENT: Please provide the targetDir " + targetDir,logger,silent);
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
         GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        util.printMessage("\nSRM-CLIENT:Exception from client="+ee.getMessage(),logger,silent);
        util.printMessageHException("SRM-CLIENT:Exception from client="+ee.getMessage(),pIntf);
                  util.printEventLogException(_theLogger,"",ee);
        if(pIntf == null) {
        inputVec.clear();
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
          System.exit(92);
        }
        util.printHException(ee,pIntf);
     }

        String utilServiceUrl = 
             gov.lbl.srm.client.util.Util.getServiceUrl(fInfo.getTURL(),
	     serviceURL, serviceHandle,servicePortNumber,1,silent,
             useLog,_theLogger,logger);
        inputVec.clear(); 
        inputVec.addElement("Connecting to serviceurl="+utilServiceUrl);
        util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
        if(_debug) {
          util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " + utilServiceUrl,logger,silent);
        }
        SRMUtilClient utilClient = new SRMUtilClient(utilServiceUrl,uid,userDesc,
          credential, _theLogger, logger, pIntf, _debug,silent, useLog,false,false,
          statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
		  connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		  delegationNeeded,numRetry,retryTimeOut);
         //creating the top level directory
         Vector vec = new Vector();
         FileInfo fileInfo = new FileInfo();
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
           fileInfo.setSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
           fileInfo.setOrigSURL(turl.substring(0,idx)+"/"+surl.substring(idx1+1));
           topLevelDir=surl.substring(idx1+1);
          }
        }
        fileInfo.overWriteTURL(false);
        vec.addElement(fileInfo);
        StringBuffer sCode = new StringBuffer();
        boolean b = utilClient.doSrmMkdir(vec,sCode,false,false,false,false);
        if(!b) {
          inputVec.clear(); 
          inputVec.addElement("TopLevel directory creation for this put request failed"
			+ " " + fileInfo.getSURL());
          util.printEventLog(_theLogger,"Parsing Request", inputVec,silent,useLog);
          util.printMessage("SRM-CLIENT: TopLevel directory creation for this put request failed" + fileInfo.getSURL(),logger,silent);
          util.printMessage("SRM-CLIENT: TopLevel directory creation for this put request failed" + fileInfo.getSURL(),pIntf);
          if(pIntf == null) {
        inputVec.clear(); 
        inputVec.addElement("StatusCode=94");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
            System.exit(94);
          }
        }
        doListing (fInfo.getSURL(),fInfo.getTURL(),topLevelDir,
				modReq,utilClient,true,true,fInfo.getOverwriteFiles());
      }
      modReq.setTargetDir(targetDir);
      req =modReq;

      req.setModeType("put");
      files = req.getFiles();
      boolean canContinue=true;
      for(int i = 0; i < files.size(); i++) {
         FileInfo fInfo = (FileInfo) files.elementAt(i);
	     if(!fInfo.getMkdirCreationOk()) {
           inputVec.clear(); 
           inputVec.addElement("Mkdir creation is not ok on the remote SRM for this SURL="+ fInfo.getOrigTURL());
           util.printEventLog(_theLogger,"Parsing Request", inputVec,silent,useLog);
           util.printMessage("\nSRM-CLIENT: Mkdir creation is not ok on the remote SRM for this SURL="+
				fInfo.getOrigTURL(),logger,silent);
           canContinue=false;   
         }
      }
      if(!canContinue) {
         inputVec.clear(); 
         inputVec.addElement("Please check mkdir creation errors, " +
			"before continue this put request");
         util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
         util.printMessage("\nSRM-CLIENT: Please check mkdir creation errors, " +
			"before continue this put request", logger,silent);
         util.printMessage("\nSRM-CLIENT: Please check mkdir creation errors, " +
			"before continue this put request", pIntf);
         if(pIntf == null) {
        inputVec.clear(); 
        inputVec.addElement("StatusCode=94");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
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
  
   inputVec.clear(); 
   inputVec.addElement("done="+b);
   inputVec.addElement(" allfilesfailed="+allFilesFailed);
   util.printEventLog
	(_theLogger,"SetRequestDone.SRMClientN", inputVec,silent,useLog);
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

    //util.printMessage("\n>>>> requestDone " + requestDone + " " + b + " " +
		//startedProcess + " " + completedProcess,logger,silent);
    inputVec.clear(); 
    inputVec.addElement("b="+b);
    inputVec.addElement(" ok="+ok);
    inputVec.addElement(" startedProcess="+startedProcess);
    inputVec.addElement(" completedProcess="+completedProcess);
    inputVec.addElement(" requestDone="+requestDone);
    util.printEventLog(_theLogger,"enbaleTransferButton", 
                inputVec,silent,useLog);
  if(b) {
    //util.printMessage("\n>>>> requestDone " + requestDone + " " + 
		//startedProcess + " " + completedProcess,logger,silent);
    if(requestDone && startedProcess <= completedProcess) {
       if(srmCopyClient != null) {
        if(doReleaseSpace) {
          try {
             srmCopyClient.releaseSpace();
          }catch (Exception e) {
             inputVec.clear(); 
             inputVec.addElement("Reason=Could not release SRM space");
             util.printEventLog(_theLogger,"EnableTrasferButton", inputVec,silent,useLog);
			 util.printMessage("SRM-CLIENT: Could not release SRM space " + e.getMessage(),logger,silent);
             //ShowException.showMessageDialog
			   //(null, "SRM-CLIENT: Could not release SRM space,\n"+
               //e.getMessage());
          }
        }
       }
       if(!fileTransferNGUI.isReportSaved()) {
          if(tThread != null) {
           tThread.setDone(true);
          }
          tThread = null;
          if(ok) {
            inputVec.clear();
            inputVec.addElement("Reason=Request completed, saving report now");
            util.printEventLog(_theLogger,"EnableTransferButton", inputVec,silent,useLog);
            //ShowException.showMessageDialog(null,"\nSRM-CLIENT: Request completed, " +
            //"saving report now.");
            //ShowException.showMessageDialog(null, 
					//"\nSRM-CLIENT: Request completed with success, ");
          }
          else {
             inputVec.clear();
             inputVec.addElement("Reason=Request failed, please see the messages, " +
				"saving report now");
             util.printEventLog(_theLogger,"EnableTransferButton", inputVec,silent,useLog);
             //ShowException.showMessageDialog(null,
	          //"\n\nSRM-CLIENT: Request failed, please see the messages. " +
	          //"saving report now.");
             //ShowException.showMessageDialog(null,
	          //"\n\nSRM-CLIENT: Request failed, please see the messages. "); 
          }
          if(outputFile.equals("")) {
           //outputFile = constructFileName(".xml");
          }
          else {
            //check whether the path is correct
            int idx = outputFile.lastIndexOf("/");
            if(idx !=-1) {
              String temp = outputFile.substring(0,idx);
              File f = new File(temp);
              if(!f.isDirectory()) {
               //outputFile = constructFileName(".xml");
               inputVec.clear();
               inputVec.addElement("Reason=Given output file dir " + temp + 
					"does not exist");
               util.printEventLog(_theLogger,"EnableTransferButton", inputVec,silent,useLog);
               util.printMessage("\nSRM-CLIENT: Given output file dir " + temp +
                " does not exist.",logger,silent);
             }
          }
         }
         fileTransferNGUI.processSaveAction(outputFile,resultStatus,resultExplanation);
         //exit is called after saving report
       } 
       else { 
         if(pIntf == null) {
           if(resultStatus == null || resultStatus.equals("")) {
             inputVec.clear();
             inputVec.addElement("StatusCode=0");
             util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
             System.exit(0);
           } 
           else {
             int exitCode = util.mapStatusCode(resultStatus);
             inputVec.clear();
             inputVec.addElement("StatusCode="+exitCode);
             util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
             System.exit(exitCode);
           }
         }
       }
    }
  }
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
 }
  else {
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

   try {
     if(proxyPath != null && !proxyPath.startsWith("Enter")) {
       inputVec.clear();
       inputVec.addElement("proxypath=" +proxyPath);
       util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
       if(_debug) {
         util.printMessage("\nSRM-CLIENT: Get Credential for proxyPath " + 
		proxyPath, logger,silent);
       }
       mycred = 
	  gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
     }
     else {
       ukey = properties.getProperty("user-key");
       ucert = properties.getProperty("user-cert");

       if((ukey != null && !ukey.startsWith("Enter")) && 
	  ((ucert != null && !ucert.startsWith("Enter")))) {
         useProxy=false;
         inputVec.clear();
         inputVec.addElement("Using usercert=" +ucert);
         inputVec.addElement("Using userkey=" +ukey);
         util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
         if(_debug) {
           util.printMessage("\nSRM-CLIENT: Using usercert :" + 
		ucert,logger,silent);
           util.printMessage("\nSRM-CLIENT: Using userkey  :" + 
		ukey,logger,silent);
         }
         if(_password.equals("")) { 
          String line = PasswordField.readPassword("Enter GRID passphrase: ");
          _password = line;
         }
         mycred = 
	   gov.lbl.srm.client.util.Util.getCredential(ucert,ukey, 
			useProxy,_password);
       } 
       else {
         inputVec.clear();
         inputVec.addElement("Using default user proxy");
         util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
         if(_debug) {
           util.printMessage("\nSRM-CLIENT: Using default user proxy ",
		 logger, silent);
         }
         //proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
         try {
           proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
         }catch(Exception ue) {
            util.printMessage("\nSRM-CLIENT: Exception from client="+
		ue.getMessage(),logger,silent);
            util.printEventLogException(_theLogger,"",ue);
            proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
         }
         inputVec.clear();
         inputVec.addElement("Found default user proxy="+proxyPath);
         util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
         mycred = 
	  gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
       }//end else
     }//end else
   }catch(Exception e) {
      util.printMessage("\nSRM-CLIENT: Exception from client="+
		e.getMessage(),logger,silent);
      util.printEventLogException(_theLogger,"",e);
   }

   if(mycred == null) {
     if(useProxy) {
       inputVec.clear();
       inputVec.addElement("Could not get credential for proxy " + proxyPath
			+ " Please check your configuration settings");
       util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
       throw new SRMClientException
         ("SRM-CLIENT: Could not get credential for proxy " + proxyPath + "\n" + 
          "Please check your configuration settings");
     }
     else {
       inputVec.clear();
       inputVec.addElement("Could not get credential for user-key " + ukey
          + "and user-cert " + ucert + " Please check yoru configuration settings");
       util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
       throw new SRMClientException
        ("SRM-CLIENT: Could not get credential for user-key " + ukey + "\n" + 
         "and user-cert " + ucert + "\n" + "Please check your configuration " + 
	     "settings");
     }
   }
   proxyType = gov.lbl.srm.client.util.Util.getProxyType(proxyPath,useProxy);
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
  gov.lbl.srm.client.util.Util.createProxy
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
     }
     else if(count == 1) {
       result =  protocol+"://"+host+":"+port+"/"+path;
     }
     else {
       result =  protocol+"://"+host+":"+port+path;
     }
   }catch(Exception e) {
     inputVec.clear();
     inputVec.addElement("Reason=Not a valid URL/URI format");
     util.printEventLog(_theLogger,"DoValidatTwoSlashes",inputVec,silent,useLog);
     util.printMessage
	 ("\nSRM-CLIENT: Exception from client in doValidateTwoSlashes: Not a valid URL/URI format " + 
		ss,logger,silent);
                  util.printEventLogException(_theLogger,"",e);
     //e.printStackTrace();
   }
   return result;
}

public static void main (String[] args) {
  new SRMClientN(args,null);
}

}

