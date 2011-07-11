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
// Class SRMClientCopyStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientCopyStatus implements SRMClientIntf
{

private GSSCredential mycred;
private XMLParseConfig pConfig = new XMLParseConfig();

private Properties properties = new Properties();
private String configFileLocation = "";
private String inputFile="";
private String _password ="";
private String userKey="";
private String userDesc="";
private boolean doReleaseSpace=false;
private boolean nooverwrite;
private boolean doBringOnline;
private boolean doReleaseFile;
private boolean dcau;
private String protocolsList;
private String userCert="";
private String proxyFile="";
private int concurrency=1;
private int statusWaitTime=30;
//private int statusMaxTimeAllowed=600;
private int statusMaxTimeAllowed=-1;
private int threshHoldValue=600; //default: 600 seconds
private int connectionTimeOutAllowed=600;
private int setHTTPConnectionTimeOutAllowed=600;
private boolean gotHTTPConnectionTimeOut;
private boolean statusWaitTimeGiven=false;
private boolean isRenew = false;
private SRMClientFileTransferN fileTransferNGUI;
private Vector srmSurlVec=new Vector();
private Vector srmFileSizeVec=new Vector();
private String sourceUrl="";
private String targetUrl="";
private String serviceUrl="";
private String storageInfo="";
private int parallelism;
private int bufferSize;
private int blockSize;
private String uid="";
private Vector inputVec = new Vector();
private int startedProcess;
private int completedProcess;
private String targetDir="";
private boolean requestDone=false;
private Request request;
private String requestType="";
private int totalFiles;
private int numRetry=3; //default
private int retryTimeOut=10; //default
private char tokenType; 
//private TFileStorageType fileStorageType = TFileStorageType.VOLATILE;
private TFileStorageType fileStorageType = null;
private TRetentionPolicyInfo retentionPolicyInfo;
private String retentionPolicyStr = "";
private String accessLatency = "";
private int tokenLifetime = 0; 
private int servicePortNumber=0;
private String serviceHandle="";
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
private String outputFile="";
private boolean textReport=true;
private boolean silent=false;
private boolean useLog=false;
private boolean gotNumRetry=false;
private boolean gotParallelism=false;
private boolean gotRetryTimeOut=false;
private boolean gotConnectionTimeOut=false;
private boolean gotBufferSize=false;
private boolean gotDcau=false;
private boolean pushMode=false;
private boolean partycopy=false;
private boolean serviceURLGiven=false;
private String serviceURL="";
private PrintIntf pIntf;
private int proxyType;
private String delegationNeeded="";

private static Log logger;
private java.util.logging.Logger _theLogger =
        java.util.logging.Logger.getLogger
            (gov.lbl.srm.client.main.SRMClientCopyStatus.class.getName());
private java.util.logging.FileHandler _fh;
private boolean overrideserviceurl=false;
private String statusToken="";
private boolean doStatus=false;
private int maxFilesPerRequest=0;
private int totalFilesPerRequest;
private int totalSubRequest;
private Vector ridList = new Vector ();

private SRMWSDLIntf srmCopyClient;

private TransferThread tThread;
private boolean _debug=false;
private boolean noDownLoad=false;
private boolean simpleSRMCOPY=false;

private boolean directGsiFTP=false;
private boolean recursive=false;
private boolean noAbortFile;
private boolean timedOut;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientCopyStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientCopyStatus(String[] args, PrintIntf pIntf) {


  this.pIntf = pIntf;
  /*
  //find login to set the uid from the env.
 
  Properties props = System.getProperties();
  String userTemp = props.getProperty("user.name");
  if(userTemp != null) { 
    uid = userTemp;
  }
  */

  if(args.length == 0) {
    showUsage(true);
  }


  for(int i = 0; i < args.length; i++) {
    boolean ok = false;
    if(i == 0 && !args[i].startsWith("-")) {
      statusToken=args[i];
      ok = true;
    }
    else if(args[i].equalsIgnoreCase("-conf") && i+1 < args.length) {
      configFileLocation = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-lite")) {
       ;
    }
    else if(args[i].equalsIgnoreCase("-nogui")) {
      ;
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
         System.out.println("\nGiven value for -statuswaittime is not valid, using default value " + 
               statusWaitTime);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-statusmaxtime") && i+1 < args.length) {
      String temp = args[i+1];
      try {
        statusMaxTimeAllowed = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {
         System.out.println("\nGiven value for -statusmaxtime is not valid, using default value " + 
               statusMaxTimeAllowed);
	  }
      i++;
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
      serviceURLGiven=true;
      i++;
    }
    else if(args[i].equalsIgnoreCase("-log") && i+1 < args.length) {
      useLog=true;
      eventLogPath = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-report") && i+1 < args.length) {
      outputFile = args[i+1];
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
    else if(args[i].equalsIgnoreCase("-type") && i+1 < args.length) {
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
    else if(args[i].equalsIgnoreCase("-accesslatency") && i+1 < args.length) {
      accessLatency = args[i+1].toUpperCase();
      i++;
    }
    else if(args[i].equalsIgnoreCase("-totalrequesttime") && i+1 < args.length) {
       String temp = args[i+1];
       try {
         totalRequestTime = Integer.parseInt(temp);
       }catch(NumberFormatException nfe) {
         System.out.println
           ("\nGiven Total Request Time valid value :" + temp); 
         System.out.println("Using default value");
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
    else if(args[i].equalsIgnoreCase("-retrytimeout") && i+1 < args.length) {
     try { 
        retryTimeOut = Integer.parseInt(args[i+1]);
        i++;
        gotRetryTimeOut=true;
     }catch(NumberFormatException nfe) {
        retryTimeOut = 120; //using the default value
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
    else if(args[i].equalsIgnoreCase("-renewproxy")) {
      isRenew = true;
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
        System.out.println("\nConfig file does not exists " +
			configFileLocation);
        showUsage(false);
      }
      if(_debug) {
        System.out.println("\nParsing config file " + configFileLocation);
      }
      sys_config = gov.lbl.srm.client.util.Util.parsefile(
			configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
    }
  }catch(Exception e) {
    System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
    showUsage(false);
  }

    String stemp = System.getProperty("SRM.HOME");
  if(stemp != null && !stemp.equals("")) {
     configFileLocation = stemp+"/conf/srmclient.conf";
     try {
       File f = new File(configFileLocation);
       if(f.exists()) {
        if(_debug) {
           System.out.println("\nParsing config file " + configFileLocation);
        }
        sys_config = gov.lbl.srm.client.util.Util.parsefile(
			configFileLocation,"SRM-COPY",silent,useLog,_theLogger); 
       }
     } catch(Exception e) {
        System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
        showUsage(false);
     }
  }


  if(proxyFile.equals("")) {
    Properties props = System.getProperties();
    Enumeration ee = props.propertyNames();
    while (ee.hasMoreElements()) {
     String str = (String) ee.nextElement();
     if(str.trim().equals("X509_USER_PROXY")) {
       String ttemp = props.getProperty(str.trim());
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

   if(silent || useLog) {
   if(eventLogPath.equals("")) {
    String temp = (String) sys_config.get("eventlogpath");
    if(temp != null) {
      eventLogPath = temp;
    }
    else {
      eventLogPath = "./srm-client-event-"+detailedLogDate+".log";
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
     System.out.println("\n SRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
  }
  }

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

  String ttemp = System.getProperty("log4j.configuration");    
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
         System.out.println("\nSRM-CLIENT: Exception from client=" + e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
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
         System.out.println("\nSRM-CLIENT: Exception from client="+ex.getMessage());
                    util.printEventLogException(_theLogger,"",ex);
         showUsage(false); 
      }
      log4jlocation=logPathDir+"/log4j_srmclient.properties";
      PropertyConfigurator.configure(log4jlocation);
      ClassLoader cl = this.getClass().getClassLoader();
      try {
        Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientCopyStatus");
        logger = LogFactory.getLog(c.getName());
      }catch(ClassNotFoundException cnfe) {
         System.out.println("ClassNotFoundException " + cnfe.getMessage());
                    util.printEventLogException(_theLogger,"",cnfe);
      }
    }
  }
  else {
      logPath="./srm-client-detailed-"+detailedLogDate+".log";
  }
  */

  try {
  PropertyConfigurator.configure(log4jlocation);
  }catch(Exception ee){;}

  if(_debug) {
   /*
   if(!logPath.equals("")) {
     util.printMessage("Log4jlocation " + log4jlocation, logger,silent);
     inputVec = new Vector();
     inputVec.addElement(log4jlocation);
     util.printEventLog(_theLogger,"Log4jlocation",inputVec,silent);
   }
   */
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
        //inputVec = new Vector();
        //inputVec.addElement("Given outputFile path did not exists ");
        //util.printEventLog(_theLogger,"Error",inputVec,silent);
        util.printMessage("Given outputFile path did not exists " + 
		   outputFile.substring(0,idx), logger,silent);
        showUsage(false);
      }
    }
  }catch(Exception e) {
    System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
    showUsage(false);
  }
  */

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
      String temp = (String) sys_config.get("RetryTimeOut");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           retryTimeOut = x;
        }catch(NumberFormatException nfe) { }
      }
     }

    if(!gotHTTPConnectionTimeOut) {
      String temp = (String) sys_config.get("SetHTTPConnectionTimeOut");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           setHTTPConnectionTimeOutAllowed = x;
        }catch(NumberFormatException nfe) { }
      }
    }

    if(!gotConnectionTimeOut) {
      String temp = (String) sys_config.get("ConnectionTimeOut");
      if(temp != null) {
        int x = 0;
        try {
           x = Integer.parseInt(temp);
           connectionTimeOutAllowed = x;
        }catch(NumberFormatException nfe) { }
      }
     }

    
     if(!serviceURLGiven) {
        String temp = (String) sys_config.get("ServiceURL");
        if(temp != null) {
           serviceURL = temp;
        }
     }


     //if all three not provided, using default proxy.
     //if one of userCert or userKey is provided, the other is needed
     if(proxyFile.equals("")) {
       if(userCert.equals("") && userKey.equals("")) {
         try {
           //proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID();
           proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID2();
         }catch(Exception e) {
           //util.printStackTrace(e,logger);
           util.printMessage("\nSRM-CLIENT: Exception from client="+e.getMessage(),
		logger,silent);
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
           inputVec = new Vector();
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
        inputVec = new Vector();
        inputVec.addElement("If you want to renew proxy automatically," +
			"you need to enter user-cert location and user-key location");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear(); 
        inputVec.addElement("ExitCode=93");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
        System.exit(93); 
      }
      String v3 = (String)properties.get("proxy-file");
      if(v3.startsWith("Enter")) {
        System.out.println("\nSRM-CLIENT: If you want to renew proxy automatically,\n "+
          "please enter your proxy file location.");
        inputVec = new Vector();
        inputVec.addElement("If you want to renew proxy automatically," +
			"please enter your proxy file location");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear(); 
        inputVec.addElement("ExitCode=93");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
        System.exit(93); 
      }
      else {
        String line = PasswordField.readPassword("Enter GRID passphrase: ");
        _password = line;
      }
      //else there is no need to renew proxy.
     }

      String[] surl = null;
      String[] turl = null;
      long[] size = null;

      Vector fileInfo = new Vector();

      if(inputFile.equals("")) { 
          surl = new String[1];
          turl = new String[1];

               //System.out.println(">>sourceurl="+sourceUrl);
               //System.out.println(">>targeturl="+targetUrl);

               //if(requestType.equalsIgnoreCase("put")) {
                  //if(targetUrl.equals("")) {
                     //System.out.println("SRM-CLIENT: -t <targeturl> is "+
			//"required " + "for put requests");   
                     //System.exit(1);
                  //}
               //}
               //else if((requestType.equalsIgnoreCase("get")) ||
			//(requestType.equalsIgnoreCase("bringonline"))) {
                  //if(sourceUrl.equals("")) {
                     //System.out.println("SRM-CLIENT: -s <sourceurl> is "+
			//"required " + "for get/bringonline requests");   
                     //System.exit(1);
                  //}
               //}

               if(!sourceUrl.equals("") || !targetUrl.equals("")) {
                 surl[0] = sourceUrl;
                 //june 28, 11
                 //if(targetUrl.equals("")) targetUrl=sourceUrl;
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
        //by default request is get, so target dir check is done in parseXML
        request = parseXML(inputFile,useLog);
      }

       if(requestType.equals("")) {
           util.printMessage("\nPlease provide a -requesttype <request_type>" + 
			requestType, logger,silent);
           util.printMessage("\nPlease provide a -requesttype <request_type>" , 
			pIntf);
           showUsage(false);
       }

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

      fileInfo = validateURL(request);

      String temp = "";
      String tempType ="surlType";

      if(fileInfo.size() > 0) {
          FileInfo fInfo = (FileInfo) fileInfo.elementAt(0);
          if((requestType.equalsIgnoreCase("put"))) {
               temp = fInfo.getTURL(); 
               tempType="turlType";
               if(serviceURLGiven) {
                 int idx = temp.indexOf("?SFN");
                 if(idx != -1) {
                   String aaa = temp.substring(0,idx);
                   if(aaa.equals(serviceUrl)) {
                     overrideserviceurl=true;
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
              if(pushMode || partycopy || doBringOnline) {
                 temp = fInfo.getSURL(); 
              } 
              else {  
                 //june 28, 11
                 if(fInfo.getTURL().equals("")) {
                   temp = fInfo.getSURL(); 
                 }
                 else {
                   tempType="turlType";
                   temp = fInfo.getTURL(); 
                 }
              }
              if(!serviceURLGiven) {
                overrideserviceurl = true;
              }
          }
          else {
             temp = fInfo.getSURL();
             if(temp.startsWith("srm:")) {
               if(!serviceURLGiven) {
                 overrideserviceurl = true;
               }
             }
          }
      }


         if(statusToken != null && !statusToken.equals("")) {
               if(temp.equals("") && serviceUrl.equals("")) {
                 util.printMessage("\nPlease provide the -serviceurl",
		   	      logger,silent);
                 showUsage(false);
               }
         }
         else {
              util.printMessage("\nPlease provide the status token.",
		   	    logger,silent);
              showUsage(false);
         }

      //System.out.println(">>>fileInfo.size()="+fileInfo.size() +  " " +
		//overrideserviceurl);
     
      if(overrideserviceurl) {
          //serviceUrl = getServiceUrl(temp,1);
          //System.out.println(">>temp="+temp+ " " + serviceURL +  " " +
		//serviceUrl);
          serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(temp,serviceURL,
	     serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
          for(int i = 0; i < fileInfo.size(); i++) {
            FileIntf fIntf = (FileIntf)fileInfo.elementAt(i);
            if(tempType.equals("surlType")) {
              String temp1 = fIntf.getSURL();
              String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1);
              fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
            }
            else {
              String temp1 = fIntf.getTURL();
              String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1);
              fIntf.setTURL(serviceUrl.replace("httpg","srm")+sfn);
            }
          }
       }
       else {
          if(serviceUrl.equals("")) {
           String tt = (String) sys_config.get("ServiceUrl");
           if(tt != null) {
             serviceUrl = tt;
           }
           else {
            if(!directGsiFTP) {
              util.printMessage 
		        ("\nPlease provide the -serviceurl full SRM service url",
					logger,silent);
              util.printMessage 
		        ("  example:srm://<hostname>:<port>//wsdlpath",
				logger,silent);
                showUsage(false);
            }
           }
          }
          //serviceUrl = getServiceUrl(serviceUrl,0);
          serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
	     serviceUrl,serviceURL, serviceHandle,servicePortNumber,0,
	     silent,useLog,_theLogger,logger);
      } 

      if(!doReleaseFile) {
   	    //currently we are setting concurrency 1 for
        //srm client transfers     
        concurrency = 1; 
        tThread = new TransferThread(this,
          concurrency, parallelism, bufferSize, blockSize, dcau,
          !nooverwrite, fileInfo.size(), request, 
	      numRetry, retryTimeOut, connectionTimeOutAllowed,_debug,silent,useLog,false,"","",pIntf);

        fileTransferNGUI = 
	      new SRMClientFileTransferN
			  (this,tThread, targetDir, concurrency, request, fileInfo, numRetry,_debug,textReport,_theLogger,silent,useLog,pIntf); 
        fileTransferNGUI.processTransferAction();
      }

      //util.printMessage("ServiceUrl="+serviceUrl,logger,silent);
       GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
        inputVec.clear(); 
        inputVec.addElement("ExitCode=92");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
                    util.printEventLogException(_theLogger,"",ee);
        System.out.println("\nSRM-CLIENT: Exception from client="+ee.getMessage());
        System.exit(92);
     }

      if(!directGsiFTP) {
        serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(serviceUrl,serviceURL,
		     serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
     }

      //util.printMessage("\n:::::::::::::::::::::::::::::::::", logger,silent);
      //util.printMessage(":::::: Running Version V2 Client now :::::", logger,silent);
      //util.printMessage(":::::::::::::::::::::::::::::::::", logger,silent);

      if(_debug) {
      util.printMessage("===================================",logger,silent);
      util.printMessage("SRM Configuration",logger,silent);
      inputVec = new Vector();
      if(!configFileLocation.equals("")) {
        util.printMessage("\tConfFile=" + configFileLocation,logger,silent);
        inputVec.add("ConfFile="+configFileLocation);
      } 
      else {
        util.printMessage("\tConfFile=none",logger,silent);
        inputVec.add("ConfFile=none");
      }
      util.printMessage("\tInputFile=" + inputFile,logger,silent);
      inputVec.add("inputFile="+inputFile);
      //util.printMessage("\tLogPath="+logPath,logger,silent);
      //inputVec.add("LogPath="+logPath);
      //util.printMessage("\tLog4jLocation="+log4jlocation,logger,silent);
      //inputVec.add("Log4jLocation="+log4jlocation);
      //util.printMessage("\tOutputFile="+outputFile,logger,silent);
      //inputVec.add("OutputFile="+outputFile);
      util.printMessage("\tProxyFile="+proxyFile,logger,silent);
      inputVec.add("ProxyFile="+proxyFile);
      if(!userCert.equals("")) {
        util.printMessage("\tUserCert="+userCert,logger,silent);
        inputVec.add("UserCert="+userCert);
      }
      if(!userKey.equals("")) {
        util.printMessage("\tUserKey="+userKey,logger,silent);
        inputVec.add("UserKey="+userKey);
      }
      
      util.printMessage("\tserviceUrl=" + serviceUrl,logger,silent);
      inputVec.add("serviceUrl="+serviceUrl);
      util.printMessage("\tDebug ON=" + _debug,logger,silent);
      inputVec.add("DebugON="+_debug);
      util.printMessage("\tQuiet ON=" + silent,logger, silent);
      inputVec.add("QuietON="+silent);
      util.printMessage("\tRenew proxy=" +isRenew,logger,silent);
      inputVec.add("RenewProxy="+isRenew); 

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
      if(!accessLatency.equals("")) {
       if(accessLatency.equalsIgnoreCase("NEARLINE")) {
          retentionPolicyInfo.setAccessLatency(TAccessLatency.NEARLINE);
       }
       if(accessLatency.equalsIgnoreCase("ONLINE")) {
          retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);
       }
      } 
      if(retentionPolicyInfo != null) {
        util.printMessage("\tRetentionPolicy=" +
			retentionPolicyInfo.getRetentionPolicy(),logger,silent);
        inputVec.add("RetentionPolicy="+
			retentionPolicyInfo.getRetentionPolicy());
        util.printMessage("\tAccessLatency=" +
			retentionPolicyInfo.getAccessLatency(),logger,silent);
        inputVec.add("AccessLatency="+retentionPolicyInfo.getAccessLatency());
      }
      if(!doReleaseFile) {
        util.printMessage("\tAuthId=" + uid,logger,silent);
        inputVec.add("AuthId="+uid);
        if(statusMaxTimeAllowed != -1) {
          util.printMessage("\tStatus Maximum time allowed=" +
			statusMaxTimeAllowed + " seconds ", logger,silent);
          inputVec.add("StatusMaximumTimeAllowed="+statusMaxTimeAllowed + 
				" seconds");
        }
        else {
          util.printMessage("\tStatus Maximum time allowed=unlimited", logger,silent);
          inputVec.add("StatusMaximumTimeAllowed=unlimited");
        }
        util.printMessage("\tStatus Wait time =" +statusWaitTime + " seconds ",
				logger,silent);
        inputVec.add("StatusWaitTime="+statusWaitTime + " second");
      }
      util.printMessage("===================================",logger,silent);

      util.printEventLog(_theLogger,"INITIALIZATION",inputVec,silent,useLog);
      }

      if(!requestType.equalsIgnoreCase("copy") && !partycopy) {
        if(!directGsiFTP) {
          inputVec = new Vector();
          inputVec.addElement(serviceUrl);
          util.printMessage("SRM-CLIENT: " + new Date() + " Connecting to " + 
				serviceUrl,logger,silent);
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

        if(doBringOnline) {
          util.printMessage("\nSRM-CLIENT: Doing BringOnline status for " + 
					statusToken ,logger,silent); 
          util.printMessage("\nSRM-CLIENT: Doing BringOnline status for " + 
					statusToken ,pIntf); 
          try {
            srmCopyClient = new SRMBringOnlineClient
		  (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
		   false, false, !nooverwrite,
                   tokenLifetime, tokenSize, gSize,
		   credential, fileStorageType, retentionPolicyInfo, 
		   totalRequestTime,pinLifeTime, parallelism, bufferSize, dcau,
                   statusMaxTimeAllowed,statusWaitTime, 
		   protocolsList, statusWaitTimeGiven,
		   storageInfo, "", userDesc, _theLogger, logger,pIntf,_debug,
		   silent,useLog,true,proxyType,noDownLoad,
		   connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		   delegationNeeded, noAbortFile,
                   threshHoldValue,numRetry,retryTimeOut,false); 

            }catch(Exception e) { 
                    util.printEventLogException(_theLogger,"",e);
              inputVec = new Vector();
              inputVec.addElement(e.getMessage());
              util.printMessage("\nSRM-CLIENT: Exception " + e,logger,silent);
              util.printMessageHException("\nSRM-CLIENT: Exception " + e,pIntf);
              util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
              System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
              requestDone = true;
              enableTransferButton(true,false);
            }
        }
        else if(requestType.equalsIgnoreCase("Get")) {
           try {
             if(doStatus) {
               inputVec = new Vector();
               inputVec.addElement("for StatusToken=" + statusToken);
               util.printMessage("\nSRM-CLIENT: Doing Get status for " + 
					statusToken ,logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing Get status for " + 
					statusToken ,pIntf); 
               util.printEventLog(_theLogger,"GetStatus",inputVec,silent,useLog);
             }
             else {
               if(!directGsiFTP) {
                 inputVec = new Vector();
                 util.printMessage("\nSRM-CLIENT:" + new Date() + " Doing srmGet now.",logger,silent); 
                 util.printEventLog(_theLogger,"SrmGet",inputVec,silent,useLog);
               }
               else {
                 inputVec = new Vector();
                 util.printMessage("\nSRM-CLIENT: Doing Direct GsiFTP now.",logger,silent); 
                 util.printMessage("\nSRM-CLIENT: Doing Direct GsiFTP now.",pIntf); 
                 util.printEventLog(_theLogger,"Direct GsiFTP",inputVec,silent,useLog);
               }
             }

             srmCopyClient = new SRMGetClient
			   (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
			    false, false, !nooverwrite,
                tokenLifetime, tokenSize, gSize,
			    credential, 
			    fileStorageType, retentionPolicyInfo, totalRequestTime,
				pinLifeTime,
                0,false, parallelism, bufferSize, dcau, directGsiFTP,
                statusMaxTimeAllowed,statusWaitTime,
				protocolsList, statusWaitTimeGiven,
                storageInfo,"",userDesc,_theLogger,
				logger, pIntf, _debug,silent,useLog,true,proxyType,
				connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
			    delegationNeeded,noAbortFile,
			    false,false,threshHoldValue,
                            numRetry,retryTimeOut,false); 
           }catch(Exception e) { 
                    util.printEventLogException(_theLogger,"",e);
             inputVec = new Vector();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception " + e.getMessage(),logger,silent);
             util.printMessage("\nSRM-CLIENT: Exception " + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
             requestDone = true;
             enableTransferButton(true,false);
           }
        }
        else if(requestType.equalsIgnoreCase("Put")) {
           try {
             if(doStatus) {
               inputVec = new Vector();
               inputVec.addElement("status for statusToken=" + statusToken);
               if(_debug) {
                 util.printMessage("\nSRM-CLIENT : " + 
					"Doing srmPut status for " + statusToken , logger,silent); 
                 util.printMessage("\nSRM-CLIENT : " + 
					"Doing srmPut status for " + statusToken , pIntf); 
               }
               util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
             }
             else {
               if(!directGsiFTP) { 
                 inputVec = new Vector();
                 util.printMessage("\nSRM-CLIENT: " + "Doing srmPut now.",logger,silent); 
                 util.printEventLog(_theLogger,"SrmPut",inputVec,silent,useLog);
               }
               else {
                 inputVec = new Vector();
                 util.printMessage("\nSRM-CLIENT: " + "Doing Direct GsiFTP now.",logger,silent); 
                 util.printMessage("\nSRM-CLIENT: " + "Doing Direct GsiFTP now.",pIntf); 
                 util.printEventLog(_theLogger,"Doing direct GsiFTP now",inputVec,silent,useLog);
               }
             }

             srmCopyClient = new SRMPutClient
		       (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
			false, !nooverwrite, tokenLifetime, tokenSize, gSize,
			credential, fileStorageType, retentionPolicyInfo, 
                        totalRequestTime, pinLifeTime, fileLifeTime, 
                        true, 0, false,  parallelism, bufferSize, dcau,
			directGsiFTP, statusMaxTimeAllowed, statusWaitTime,
                        protocolsList, statusWaitTimeGiven, 
                        storageInfo, "", userDesc, _theLogger,
		        logger,pIntf,_debug,silent,useLog,true,proxyType,false,
			connectionTimeOutAllowed,
                        setHTTPConnectionTimeOutAllowed, 
                        delegationNeeded,noAbortFile,
		        false,false,threshHoldValue,false,
                        numRetry,retryTimeOut,false); 
           }catch(Exception e) { 
                    util.printEventLogException(_theLogger,"",e);
             inputVec = new Vector();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception " + e.getMessage(),logger,silent);
             util.printMessageHException("\nSRM-CLIENT: Exception " + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
             requestDone = true;
             enableTransferButton(true,false);
          }
        }
        else if(requestType.equalsIgnoreCase("Copy")) {
           try {
             if(doStatus) {
               inputVec = new Vector();
               inputVec.addElement("for statusToken=" + statusToken);
               util.printMessage("\nSRM-CLIENT: Doing Copy status for " + 
					statusToken ,logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing Copy status for " + 
					statusToken ,pIntf); 
               util.printEventLog(_theLogger,"Copy Status",inputVec,silent,useLog);
             }
             if(partycopy) {
               inputVec = new Vector();
               util.printMessage("\nSRM-CLIENT: Doing 3partycopy now.",logger,silent); 
               util.printMessage("\nSRM-CLIENT: Doing 3partycopy now.",pIntf); 
               util.printEventLog(_theLogger,"Doing 3partycopy",inputVec,silent,useLog);
             }

             srmCopyClient = new SRMCopyClient
		      (this,serviceUrl,uid,fileInfo,fileToken,statusToken,
               pushMode, partycopy, !nooverwrite,
			   false, doReleaseSpace,  
			   tokenLifetime, tokenSize, gSize,
               credential, request, 
               fileStorageType,retentionPolicyInfo,totalRequestTime,
			   pinLifeTime,fileLifeTime,  parallelism, bufferSize, dcau,
               statusMaxTimeAllowed,statusWaitTime,
			   protocolsList, statusWaitTimeGiven,
			   storageInfo, "", userDesc, _theLogger,logger,pIntf,_debug,
			   silent,useLog,true,proxyType,connectionTimeOutAllowed,
               setHTTPConnectionTimeOutAllowed,
			   recursive,0, delegationNeeded,noAbortFile,
		       false,threshHoldValue, numRetry, retryTimeOut,false); 
           }catch(Exception e) { 
                    util.printEventLogException(_theLogger,"",e);
             inputVec = new Vector();
             inputVec.addElement(e.getMessage());
             util.printMessage("\nSRM-CLIENT: Exception " + e.getMessage(),logger,silent);
             util.printMessageHException("\nSRM-CLIENT: Exception " + e.getMessage(),pIntf);
             util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
             System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
             requestDone = true;
             enableTransferButton(true,false);
           }
        }

      if(doStatus) {
        srmCopyClient.doStatusEnquiry();
      }
   }catch(Exception e) {
        System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
        inputVec.clear(); 
        inputVec.addElement("ExitCode=92");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
     System.exit(92);
   }
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
// abortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void abortFiles (String turl, String rid, int label) {
  try {
    if(srmCopyClient != null) {
       inputVec = new Vector();
       inputVec.addElement("for turl="+turl);
       srmCopyClient.abortFiles(turl,rid,label);
       util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);
    }
    else {
      inputVec = new Vector();
      inputVec.addElement("Cannot do abortfiles="+turl);
      util.printMessage("SRM-CLIENT: Cannot do abortFiles", logger,silent);
      util.printMessage("SRM-CLIENT: Cannot do abortFiles",pIntf);
      util.printEventLog(_theLogger,"AbortFiles",inputVec,silent,useLog);
    }
  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
     util.printMessageHException(e.getMessage(),pIntf);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// putDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void putDone(String siteUrl, String rid, int label) {
  try {
   inputVec = new Vector();
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
     inputVec = new Vector();
     inputVec.addElement("Cannot do putDone for SURL="+siteUrl);
     util.printMessage("SRM-CLIENT: " + "Cannot do putDone for SURL="+siteUrl,logger,silent);
     util.printMessage("SRM-CLIENT: " + "Cannot do putDone for SURL="+siteUrl,
		pIntf);
     util.printEventLog(_theLogger,"PutDone",inputVec,silent,useLog);
   }
  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
     util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
  }
}

public void setGateWayModeEnabled (boolean b) {}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// releaseFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void releaseFile(String siteUrl, String rid, int label) {
  try {
   if(srmCopyClient != null) {
     if(doReleaseSpace) {
       inputVec = new Vector();
       inputVec.addElement("for SURL="+siteUrl);
       util.printEventLog(_theLogger,"ReleaseFile",inputVec,silent,useLog);
       srmCopyClient.releaseFile(siteUrl,rid,label);
     }
   }
   else {
     inputVec = new Vector();
     inputVec.addElement("Cannot do releaseFile for SURL="+siteUrl);
     util.printEventLog(_theLogger,"",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Cannot do releaseFile",logger,silent);
     util.printMessage("SRM-CLIENT: Cannot do releaseFile",pIntf);
   }
  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
     util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
  }
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


  if((request.getModeType().equalsIgnoreCase("get")) ||
     (request.getModeType().equalsIgnoreCase("bringonline")) ||
		(request.getModeType().equalsIgnoreCase("copy") && pushMode)) {
    if(size > 0) {
      FileInfo files = (FileInfo) fInfo.elementAt(0);
      String surl = files.getSURL();
      sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
    }
    for(int i = 1; i < size; i++) {
      FileInfo files = (FileInfo) fInfo.elementAt(i);
      String surl = files.getSURL();
      String sSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
      if(!sourceSRM.equals(sSRM)) {
        inputVec = new Vector();
        inputVec.addElement("Method=CheckAllSourceFromSameSRM2");
        inputVec.addElement
			("Reason=sources from multiple SRMs are not allowed");
        util.printMessage
			("\nsources from multiple SRMs are not allowed ",logger,silent);
        util.printMessageHException
			("\nsources from multiple SRMs are not allowed ",pIntf);
        util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
        return false;
      }
    }
  }
  else if((request.getModeType().equalsIgnoreCase("put")) ||
		(request.getModeType().equalsIgnoreCase("copy") && !pushMode)) {
    if(size > 0) {
      FileInfo files = (FileInfo) fInfo.elementAt(0);
      String surl = files.getTURL();
      sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
    }
    for(int i = 1; i < size; i++) {
      FileInfo files = (FileInfo) fInfo.elementAt(i);
      String surl = files.getTURL();
      String sSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
      if(!sourceSRM.equals(sSRM)) {
        inputVec = new Vector();
        inputVec.addElement("Method=CheckAllSourceFromSameSRM2");
        inputVec.addElement
			("Reason=Targets to multiple SRMs are not allowed");
        util.printMessage
			("\nTargets to multiple SRMs are not allowed",logger,silent);
        util.printMessageHException
			("\nTargets to multiple SRMs are not allowed",pIntf);
        util.printEventLog(_theLogger,"CheckInput",inputVec,silent,useLog);
        return false;
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
    if(surl.startsWith("gsiftp://")) {
       mode = 0;
    }
    else if(surl.startsWith("srm://")) {
       mode = 1;
    }
  }

  for(int i = 1; i < size; i++) {
    FileInfo files = (FileInfo) fInfo.elementAt(i);
    String surl = files.getSURL();
    if(mode == 0) {
      if(surl.startsWith("srm://")) {
         inputVec = new Vector();
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
      if(surl.startsWith("gsiftp://")) {
         inputVec = new Vector();
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
    if(surl.startsWith("gsiftp://")) return true;
    sourceSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
  }
  for(int i = 1; i < size; i++) {
    FileInfo files = (FileInfo) fInfo.elementAt(i);
    String surl = files.getSURL();
    String sSRM = gov.lbl.srm.client.util.Util.findServiceSRMFromSURL(surl);
    if(!sourceSRM.equals(sSRM)) {
      inputVec = new Vector();
      inputVec.addElement("Method=CheckAllSourceFromSameSRM");
      inputVec.addElement("Reason=sources from multiple SRMs " +
			"are not allowed in 3partycopy");
      util.printMessage("\nSRM-CLIENT: sources from multiple SRMs are not allowed in "+
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

  if(request == null) return result;

  Vector fInfo = request.getFiles();
  int size = fInfo.size();
  if((request.getModeType().equalsIgnoreCase("copy")) && 
		partycopy) {
    //check if all source from same SRM
   try {
    if(!checkAllSourceFromSameSRM(fInfo)) {
       inputVec = new Vector();
       inputVec.addElement
		("Reason=sources from multiple SRMs are not allowed in 3partycopy");
       util.printMessage("SRM-CLIENT: sources from multiple SRMs are not allowed " +
			"in 3partycopy", logger,true);
       util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
       showUsage(false);
    }
   }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
     util.printMessageHException("SRM-CLIENT: " + e.getMessage(),pIntf);
     //util.printStackTrace(e,logger);
   }
  }

  try {
    if(!checkAllSourceFromSameSRM2(fInfo)) {
      showUsage(false);
    }
  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
    System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
    util.printMessageHException(e.getMessage(),pIntf);
    //util.printStackTrace(e,logger);
  }

  for(int i = 0; i < size; i++) {
    boolean skip = false;
    FileInfo f = (FileInfo) fInfo.elementAt(i);
    String surl = f.getSURL();
    String turl = f.getTURL();
    if(request.getModeType().equalsIgnoreCase("releaseFile")) {
       if(!surl.startsWith("srm:")) {
         inputVec = new Vector();
         inputVec.addElement("RequestMode=ReleaseFile");
         inputVec.addElement("Reason=source url is not valid");
         util.printMessage("\nSRM-CLIENT: source url is not valid " + surl, logger,silent);
         util.printMessageHException("\nSRM-CLIENT: source url is not valid " + surl, pIntf);
         util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
         skip = true;
       }
    }
    else if(request.getModeType().equalsIgnoreCase("dir")) {
       if(!surl.startsWith("srm://")) {
         inputVec = new Vector();
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
       if((!surl.startsWith("srm://"))|| (!turl.startsWith("srm://"))) { 
        inputVec = new Vector();
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
       if((surl.startsWith("srm://"))&& (turl.startsWith("srm://"))) { ; }
       else if((surl.startsWith("gsiftp://"))&& (turl.startsWith("srm://"))) { ; }
       else {
         inputVec = new Vector();
         inputVec.addElement("RequestMode=copy");
         inputVec.addElement("Reason=For the partycopy request surl must begin" +
			" with either srm:// or gsiftp:// " + surl + " and turl " + turl + 
			" must start with srm");
         util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
         util.printMessage("\nSRM-CLIENT: For the partycopy request ",logger,silent);
         util.printMessage("SRM-CLIENT: surl must begin with either srm:// or gsiftp://" 
			+ surl + " and turl " + turl + " must start with srm", logger, silent);
         skip=true; 
       }
    }
    else {
      if((surl.startsWith("gsiftp:")) || (surl.startsWith("srm:")) ||
         (surl.startsWith("ftp:")) || (surl.startsWith("http:"))) {
         if(doBringOnline) {; }
         else {
          if(turl.equals("") && noDownLoad) {; }
          else if((!turl.startsWith("file:") || !turl.startsWith(File.separator)) && (!turl.startsWith("srm:"))) {
           /*
           inputVec = new Vector();
           inputVec.addElement("Reason=turl is not valid " + turl + 
				" for the given surl " + surl + " skipping this file in the request");
           util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
           util.printMessage("\nSRM-CLIENT: turl is not valid " + turl,logger,silent);
           util.printMessage("SRM-CLIENT: for the given surl " + surl,logger,silent);
           util.printMessage("SRM-CLIENT: skipping this file in the request",logger,silent);
           skip = true;
           */
          }
         }
      }
      else if(surl.startsWith("file:") || surl.startsWith(File.separator)) {
        if(!turl.startsWith("srm:") && !turl.startsWith("gsiftp:")) {
          inputVec = new Vector();
          inputVec.addElement("Reason=turl is not valid " + turl + 
			"for the given surl " + surl + " skipping this file in the request");
          util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
          util.printMessage("\nSRM-CLIENT: turl is not valid " + turl,logger,silent);
          util.printMessage("SRM-CLIENT: for the given surl " + surl,logger,silent);
          util.printMessage("SRM-CLIENT: skipping this file in the request",logger,silent);
          skip = true;
        }
      }
      else {  
        if(requestType.equalsIgnoreCase("put"))  {
        }  
        else {
        inputVec = new Vector();
        inputVec.addElement("Reason=Given surl is not valid " + surl +
		    " surl should start with gsiftp, ftp, http, srm, file" +
		    " skipping this file in the request");
        util.printEventLog(_theLogger,"ValidateURL",inputVec,silent,useLog);
        util.printMessage("\nSRM-CLIENT: Given surl is not valid " + surl, 
		logger,silent);
        util.printMessage
	 	  ("\nSRM-CLIENT: surl should start with gsiftp,ftp,http,srm,file",
			logger,silent);
        util.printMessage("SRM-CLIENT: skipping this file in the request.",
			logger,silent);
        skip = true;
        } 
      }
    }
    if(!skip) {
        result.add(f);
    }//end if(!skip)
   }//end for
   size = result.size();  
   for(int i =0; i < result.size(); i++) { 
      FileInfo f = (FileInfo) result.elementAt(i);
      f.setLabel(i);
   }
   return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//findRequestType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 
private void findRequestType(String surl, String turl, String durl) {
 
  /*
   if(doCopy) {
     requestType = "copy";
     return;
   }
  */
   if(doReleaseFile) {
     requestType="releaseFile";
     return;
   }
   if(!durl.equals("")) {
     requestType="dir";
   }
   else if(surl.startsWith("file:") || surl.startsWith(File.separator)) {
     if(turl.startsWith("srm:")) {
       requestType = "put";
     }
     else if(turl.startsWith("file:") || turl.startsWith(File.separator)) {
       requestType = "put";
       //mayBeDirectGsiFTP=true;
     }
     else if(turl.startsWith("gsiftp:")) {
       requestType = "put";
       //mayBeDirectGsiFTP=true;
     }
   }
   else if(turl.startsWith("file:") || turl.startsWith(File.separator)) {
     if(surl.startsWith("srm:")) {
       requestType = "get";
     }
     else if(surl.startsWith("file:") || surl.startsWith(File.separator)) {
       requestType = "put";
       //mayBeDirectGsiFTP=true;
     }
     else if(surl.startsWith("gsiftp:")) {
       requestType = "get";
       //mayBeDirectGsiFTP=true;
     }
   }
   else if(turl.equals("") && noDownLoad) {
     requestType = "get";
   }
   else {
     requestType = "copy";
   }
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
       inputVec = new Vector();
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
                     inputVec = new Vector();
                     inputVec.addElement("Creating mkdir for surl " +
						turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                     util.printEventLog(_theLogger,"DoListing",inputVec,silent,useLog);
                     fInfo.setSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                     fInfo.setOrigSURL(turl.substring(0,idx)+"/"+topLevelDir+"/"+ff.getName());
                  }
                  else {
                    util.printMessage("\nSRM-CLIENT: Creating mkdir for surl " + 
						turl.substring(0,idx)+"/"+ff.getName(),logger,silent);
                    inputVec = new Vector();
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
     inputVec = new Vector();
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

   
   if(surl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(surl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         surl = "gsiftp://"+host+":"+port+"/"+path;
       }
   }
   if(turl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(turl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         surl = "gsiftp://"+host+":"+port+"/"+path;
       }
   }

   inputVec = new Vector();
   util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
   if(_debug) {
     util.printMessage("SRM-CLIENT: Creating request", logger,silent);
   }
   Properties properties = System.getProperties();
   String userName = properties.getProperty("user.name");
   Request req = new Request();
   if(surl.startsWith("srm") && recursive) {
       GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
                    util.printEventLogException(_theLogger,"",ee);
        System.out.println("\nSRM-CLIENT: Exception from client="+ee.getMessage());
        inputVec.clear(); 
        inputVec.addElement("ExitCode=92");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
        System.exit(92);
     }

      //String utilServiceUrl = getServiceUrl(surl,1);
      String utilServiceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(surl,serviceURL,
		     serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
      inputVec = new Vector();
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
		  delegationNeeded,numRetry, retryTimeOut);
      Vector vec = new Vector ();
      FileInfo fInfo = new FileInfo();
      fInfo.setSURL(surl); 
      vec.addElement(fInfo);
      boolean getRecursive =true;
      Vector resultVec = new Vector();
      utilClient.doSrmLs(vec, false, recursive,
          false, false, 0,0,0,fileStorageType,"", statusWaitTimeGiven,
		  getRecursive,textReport,resultVec,surl,turl,false,false,false,"","");
      for(int i = 0; i < resultVec.size(); i++) {
         fInfo = (FileInfo)resultVec.elementAt(i);
         fInfo.setOverwriteFiles(!nooverwrite);
         req.addFileInfo(fInfo); 
      }
   }
   else if((surl.startsWith("file") || surl.startsWith(File.separator)) 
			&& recursive) {
       GSSCredential credential=null;
     try {
       credential = checkTimeLeft();
     }catch(Exception ee) {
                    util.printEventLogException(_theLogger,"",ee);
        System.out.println("\nSRM-CLIENT: Exception from client="+ee.getMessage());
        inputVec.clear(); 
        inputVec.addElement("ExitCode=92");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
        System.exit(92);
     }

      //String utilServiceUrl = getServiceUrl(turl,1);
      String utilServiceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(turl,serviceURL,
		     serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
      inputVec = new Vector();
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
		  delegationNeeded,numRetry, retryTimeOut);
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
        inputVec = new Vector();
        inputVec.addElement("TopLevel directory creation for this put request failed"); 
        util.printEventLog(_theLogger,"CreatingRequest", inputVec,silent,useLog);
        util.printMessage("SRM-CLIENT: TopLevel directory creation for " +
				" this put request failed" +
			fInfo.getSURL(),logger,silent);
        inputVec.clear(); 
        inputVec.addElement("ExitCode=94");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
        System.exit(94);
      }
      doListing (surl,turl,topLevelDir,req,utilClient,true,false,false);
      Vector files = req.getFiles();
      boolean canContinue=true;
      for(int i = 0; i < files.size(); i++) {
         fInfo = (FileInfo) files.elementAt(i);
	     if(!fInfo.getMkdirCreationOk()) {
           inputVec = new Vector();
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
         inputVec = new Vector();
         inputVec.addElement("Please check mkdir creation errors, " +
			"before continue this put request");
         util.printEventLog(_theLogger,"CreateRequest",inputVec,silent,useLog);
         util.printMessage
			("\nSRM-CLIENT: Please check mkdir creation errors, before continue this put request",
				logger,silent);
        inputVec.clear(); 
        inputVec.addElement("ExitCode=94");
        util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
         System.exit(94);
      }
   }
   else {
     FileInfo fInfo = new FileInfo(); 
     fInfo.setSURL(surl);
     fInfo.setTURL(turl);
     fInfo.setOrigSURL(surl);
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


public synchronized void numberOfSpaceAvailableFiles (int num) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSrmFirstUrl
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Vector getSrmFirstUrl (boolean firstTime) {
  Vector vec = new Vector ();
  if(firstTime) {
    if(srmSurlVec.size() >= concurrency) {
      for(int i = 0; i < concurrency; i++) {
       FileInfo fInfo = (FileInfo) srmSurlVec.elementAt(i);
       srmSurlVec.remove(i);
       vec.addElement(fInfo);
      }
      return vec;
    }
    else {
     return null;
    }
  }
  else {
    if(srmSurlVec.size() > 0) {
       FileInfo fInfo = (FileInfo) srmSurlVec.elementAt(0);
       srmSurlVec.remove(0);
       vec.addElement(fInfo);
       return vec;
    }
    else {
      return null;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void srmFileFailure(int idx, String message) {
   fileTransferNGUI.srmFileFailure(idx,message);
   tThread.srmFileFailure();
}

public synchronized void setRequest(Request request) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiatePullingFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void initiatePullingFile (FileInfo fInfo) {

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

   inputVec = new Vector();
   inputVec.addElement("RequestFileStatus For SURL="+fInfo.getOrigSURL() + " is ready.");
   util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
   util.printMessage("\nSRM-CLIENT: RequestFileStatus for " +
      "SURL="+fInfo.getOrigSURL()+" is Ready.",logger,silent);
   if(requestType.equalsIgnoreCase("Put") || 
      requestType.equalsIgnoreCase("3partycopy")) {
     if(!directGsiFTP) {
       inputVec = new Vector();
       inputVec.addElement("received TURL="+fInfo.getTURL());
       util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getTURL(),logger,silent);
     }
   }
   else {
     if(!directGsiFTP) {
       inputVec = new Vector();
       inputVec.addElement("received TURL="+fInfo.getSURL());
       util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
       util.printMessage("SRM-CLIENT: " + "received TURL="+fInfo.getSURL(),logger,silent);
     }
   }

   String source = fInfo.getSURL();
   String target = fInfo.getTURL();
   source = doValidateTwoSlashes(source);
   target = doValidateTwoSlashes(target);
   fInfo.setSURL(source); 
   fInfo.setTURL(target); 
 
   util.printMessage
		("\nSRM-CLIENT: " + new Date() + " start file transfer.",logger,silent);
   inputVec = new Vector();
   inputVec.addElement("Added in the queue for file transfer");
   util.printEventLog(_theLogger,"InitiatePullingFile",inputVec,silent,useLog);
   srmSurlVec.addElement(fInfo);
   startedProcess ++;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseXML
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Request parseXML (String fileName,boolean useLog)
    throws NumberFormatException, SRMClientException, Exception {

   inputVec = new Vector();
   inputVec.addElement("Parsing request file");
   util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
   if(_debug) {
     util.printMessage("\nSRM-CLIENT: Parsing request file " + fileName,logger,silent);
   }
   XMLParseRequest xmlParse = new XMLParseRequest(fileName,_theLogger,silent,true);
   Request req = xmlParse.getRequest();
   Vector filesVec = req.getFiles();

   for(int i = 0; i < filesVec.size(); i++) {
     FileInfo fInfo = (FileInfo) filesVec.elementAt(i);
     String ssurl = fInfo.getSURL();
     String tturl = fInfo.getTURL();

     if(ssurl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(ssurl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         fInfo.setSURL("gsiftp://"+host+":"+port+"/"+path);
       }
     }
     if(tturl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(tturl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         fInfo.setTURL("gsiftp://"+host+":"+port+"/"+path);
       }
     }
   }


   String ssurl = "";
   String tturl = "";

   if(filesVec.size() > 0) {
     FileInfo fInfo = (FileInfo) filesVec.elementAt(0);
     ssurl = fInfo.getSURL();
     tturl = fInfo.getTURL();
   }
   findRequestType(ssurl,tturl,"");
   req.setModeType(requestType);
   //at this point, if surl starts with file: then request we know
   //it is put type request
   if(req.getModeType().equalsIgnoreCase("Get")) {
     if(!doStatus) {
      if(targetDir.equals("") && !noDownLoad) {
        inputVec = new Vector();
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
                    util.printEventLogException(_theLogger,"",ee);
        System.out.println("\nSRM-CLIENT: Exception from client="+ee.getMessage());
        util.printMessageHException("SRM-CLIENT:Exception="+ee.getMessage(),pIntf);
        if(pIntf == null) {
        inputVec.clear(); 
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
          System.exit(92);
        }
        util.printHException(ee,pIntf);
     }

        //String utilServiceUrl = getServiceUrl(fInfo.getTURL(),1);
        String utilServiceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
			 fInfo.getTURL(),serviceURL, 
	         serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
        inputVec = new Vector();
        inputVec.addElement("Connecting to serviceurl="+utilServiceUrl);
        util.printEventLog(_theLogger,"Parsing Request",inputVec,silent,useLog);
        if(_debug) {
          util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " + utilServiceUrl,logger,silent);
        }
        SRMUtilClient utilClient = new SRMUtilClient(utilServiceUrl,uid,userDesc,
          credential, _theLogger, logger, pIntf, _debug,silent,useLog,false,
		  false, statusMaxTimeAllowed,statusWaitTime, storageInfo,proxyType,
		  connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		  delegationNeeded,numRetry, retryTimeOut);
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
          inputVec = new Vector();
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
           inputVec = new Vector();
           inputVec.addElement("Mkdir creation is not ok on the remote SRM for this SURL="+ fInfo.getOrigTURL());
           util.printEventLog(_theLogger,"Parsing Request", inputVec,silent,useLog);
           util.printMessage("\nSRM-CLIENT: Mkdir creation is not ok on the remote SRM for this SURL="+
				fInfo.getOrigTURL(),logger,silent);
           canContinue=false;   
         }
      }
      if(!canContinue) {
         inputVec = new Vector();
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
   /*
   if(doCopy) {
     requestType = "copy";
   }
   */

   return req;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//@@@@@@@  start method implemented for interface SRMClientWindow
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestInformation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestInformation(String status, String explanation) {
}

public String getRequestStatus() { return ""; }

public String getRequestExplanation() { return ""; }


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestTimedOut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestTimedOut(boolean b) {
  timedOut = b;
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

  if(b) {
    //util.printMessage("\n>>>> requestDone " + requestDone + " " + 
		//startedProcess + " " + completedProcess,logger,silent);
    if(requestDone && startedProcess <= completedProcess) {
       if(srmCopyClient != null) {
        if(doReleaseSpace) {
          try {
             srmCopyClient.releaseSpace();
          }catch (Exception e) {
                    util.printEventLogException(_theLogger,"",e);
             System.out.println("\nSRM-CLIENT: Exception from server="+e.getMessage());
             inputVec = new Vector();
             inputVec.addElement("Reason=Could not release SRM space");
             util.printEventLog(_theLogger,"EnableTrasferButton", inputVec,silent,useLog);
             ShowException.showMessageDialog
			   (null, "SRM-CLIENT: Could not release SRM space,\n"+
               e.getMessage());
          }
        }
       }
       if(!fileTransferNGUI.isReportSaved()) {
          if(tThread != null) {
           tThread.setDone(true);
          }
          tThread = null;
          if(ok) {
            inputVec = new Vector();
            inputVec.addElement("Reason=Request completed, saving report now");
            util.printEventLog(_theLogger,"EnableTransferButton", inputVec,silent,useLog);
            //ShowException.showMessageDialog(null,"\nSRM-CLIENT: Request completed, " +
            //"saving report now.");
            //ShowException.showMessageDialog(null, 
					//"\nSRM-CLIENT: Request completed with success, ");
          }
          else {
             inputVec = new Vector();
             inputVec.addElement("Reason=Request failed, please see the messages, " + "saving report now");
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
               inputVec = new Vector();
               inputVec.addElement("Reason=Given output file dir " + temp + 
					"does not exist");
               util.printEventLog(_theLogger,"EnableTransferButton", inputVec,silent,useLog);
               util.printMessage("\nSRM-CLIENT: Given output file dir " + temp +
                " does not exist.",logger,silent);
             }
          }
         }
         fileTransferNGUI.processSaveAction(outputFile,"","");
         //exit is called after saving report
       } 
       else { 
          inputVec.clear(); 
          inputVec.addElement("ExitCode=0");
          util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
         System.exit(0);
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
                    util.printEventLogException(_theLogger,"",e);
   inputVec = new Vector();
   inputVec.addElement("Exception="+e.getMessage());
   util.printEventLog(_theLogger,"IsConfigExists",inputVec,silent,useLog);
   System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
   //ShowException.logDebugMessage(logger,e);
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
            "\tsrm-copy-status requesttoken -requesttype <requesttype> [command line options]\n"+
            "\tsrm-copy-status -requesttoken request_token -requesttype <requesttype> \n"+
            "\tsrm-copy-status -requesttoken request_token -requesttype <requesttype> -f <inputfile>\n"+
			"\t                       [command line options]\n"+
            "\n"+
            "\t-conf              <path>\n"+
	    "\t-serviceurl        <full wsdl service url> \n" +
            "\t                   example srm://host:port/wsdlpath \n"+
            "\t -s                <sourceurl> \n" +
            "\t -t                <targeturl> \n" +
            "\t -f                <inputfile> \n" +
            "\t-authid            (user authorization id used in SRM) \n"+
            "\t-requesttoken      <request token> (valid request token \n" +
            "\t                      returned by server.)\n"+
            //"\t-connectiontimeout <integer in seconds> (enforce http connection timeout in the given seconds)default:1800 \n"+
            "\t-sethttptimeout    <integer in seconds> (enforce SRM/httpg connection timeout and sets client-side http connection timeout in the given seconds)default:600 \n"+
            "\t-requesttype       <get|put|copy|bringonline>default:copy\n" +
            "\t-delegation         uses delegation (default:no delegation)\n"+
            "\t-renewproxy        default:false\n"+
            "\t-proxyfile         <path to proxyfile> \n"+
            "\t                      default:from default user proxy location\n"+
            "\t-usercert          <path to usercert> \n" +
            "\t-userkey           <path to userkey> \n" +
            "\t-debug             default:false\n" +
            "\t-quiet             default:false\n" +
            "\t                      suppress output in the console.\n" +
            "\t                      this option writes the output to logfile.  default:./srmclient-event-date-random.log)\n" + 
            "\t-log               <path to logfile> \n"+
            "\t-report             <path to reportfile>\n" +
            "\t-help              show this message.");
 } 
          inputVec.clear(); 
          inputVec.addElement("ExitCode=93");
          util.printEventLog(_theLogger,"ExitStatus",inputVec,silent,useLog);
 System.exit(93);
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
     if(!proxyPath.startsWith("Enter")) {
       inputVec = new Vector();
       inputVec.addElement("proxypath=" +proxyPath);
       util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
       if(_debug) {
         util.printMessage("\nSRM-CLIENT: Get Credential for proxyPath " + proxyPath,
				logger,silent);
       }
       mycred = 
		gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
     }
     else {
       ukey = properties.getProperty("user-key");
       ucert = properties.getProperty("user-cert");

       if(!ukey.startsWith("Enter") && (!ucert.startsWith("Enter"))) {
         useProxy=false;
         inputVec = new Vector();
         inputVec.addElement("Using usercert=" +ucert);
         inputVec.addElement("Using userkey=" +ukey);
         util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
         if(_debug) {
           util.printMessage("\nSRM-CLIENT: Using usercert :" + ucert,logger,silent);
           util.printMessage("\nSRM-CLIENT: Using userkey  :" + ukey,logger,silent);
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
         inputVec = new Vector();
         inputVec.addElement("Using default user proxy=" +proxyPath);
         util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
         if(_debug) {
           util.printMessage("\nSRM-CLIENT: Using default user proxy " + proxyPath,logger,
				silent);
         }
        try {
           proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) {
                    util.printEventLogException(_theLogger,"",ue);
           util.printMessage("\nSRM-CLIENT: Exception from client="+ue.getMessage(),logger,silent);
           proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
         mycred = 
		  gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
       }
     }
   }catch(Exception e) {
      util.printMessage(e.getMessage(),logger,silent);
                    util.printEventLogException(_theLogger,"",e);
      //util.printStackTrace(e,logger);
   }

   if(mycred == null) {
     if(useProxy) {
       inputVec = new Vector();
       inputVec.addElement("Could not get credential for proxy " + proxyPath
			+ " Please check your configuration settings");
       util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
       throw new SRMClientException
         ("SRM-CLIENT: Could not get credential for proxy " + proxyPath + "\n" + 
          "Please check your configuration settings");
     }
     else {
       inputVec = new Vector();
       inputVec.addElement("Could not get credential for user-key " + ukey
          + "and user-cert " + ucert + " Please check yoru configuration settings");
       util.printEventLog(_theLogger,"GetCredential", inputVec,silent,useLog);
       throw new SRMClientException
        ("SRM-CLIENT: Could not get credential for user-key " + ukey + "\n" + 
         "and user-cert " + ucert + "\n" + 
         "Please check your configuration " + 
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
       inputVec = new Vector();
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
         inputVec = new Vector();
         inputVec.addElement("Your proxy has only " + remainingLifeTime + 
			" second left. Please renew your proxy.");
         util.printEventLog(_theLogger,"CheckTimeLeft", inputVec,silent,useLog);
         ShowException.showMessageDialog(null, "SRM-CLIENT: Your proxy has only " +
		remainingLifeTime + " second left.\n" + "Please renew your proxy.");
      }
    }
   }catch(Exception e) {
      System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
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

 inputVec = new Vector();
 util.printEventLog(_theLogger,"CreateProxy", inputVec,silent,useLog);
 util.printMessage("Creating proxy now ...",logger,silent);
 try {
  String userCert = pConfig.getUserCert();
  String userKey = pConfig.getUserKey();
  String proxyFile =  pConfig.getProxyFile();
  gov.lbl.srm.client.util.Util.createProxy
		(userCert, userKey, proxyFile, passwd);
 }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     inputVec = new Vector();
     inputVec.addElement("Reason="+e.getMessage());
     util.printEventLog(_theLogger,"CreateProxy",inputVec,silent,useLog);
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
     //ShowException.logDebugMessage(logger,e);
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
     if(protocol.startsWith("file") || protocol.startsWith(File.separator)) {
       return result;
     }
     String host = test.getHost();
     int port = test.getPort();
     String path = test.getPath();
     while(true) {
       if(path.startsWith("/")) {
         path = path.substring(1);
       }
       else {
          break;
       }
     }
     result =  protocol+"://"+host+":"+port+"//"+path;
   }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     inputVec = new Vector(); 
     inputVec.addElement("Reason=Not a valid URL/URI format");
     util.printEventLog(_theLogger,"DoValidatTwoSlashes",inputVec,silent,useLog);
     util.printMessage
	 ("\nSRM-CLIENT: Exception from client in doValidateTwoSlashes: Not a valid URL/URI format " + 
		ss,logger,silent);
   }
   return result;
}

public static void main(String[] args) {
   new SRMClientCopyStatus(args,null);
}

}

