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
// Class SRMClientRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientRequest
{

private GSSCredential mycred;
private XMLParseConfig pConfig = new XMLParseConfig();

private Properties properties = new Properties();
private String configFileLocation = "";
private String inputFile="";
private String _password ="";
private String userKey="";
private String userCert="";
private String proxyFile="";
private int proxyType;
private boolean isRenew = false;
private boolean serviceURLGiven=false;
private String serviceURL="";
private int servicePortNumber=0;
private String serviceHandle="";
private String storageInfo="";
private String sourceUrl="";
private String targetUrl="";
private String serviceUrl="";
private String delegationNeeded="";
private String uid="";
private Request request;
private String requestType="";
private String requestToken;
private String userDesc="";
private String log4jlocation="";
private String logPath="";
private String eventLogPath="";
private int lifeTime;
private int  pinLifeTime;

private static Log logger;
private boolean onlyAbFiles=false;
private boolean onlyAbRequest=false;
private boolean onlyExtFileLT=false;
private boolean onlyReqSummary=false;
private boolean onlyGetRID=false;
private boolean suspendRequest=false;
private boolean resumeRequest=false;

private SRMWSDLIntf srmCopyClient;

private java.util.logging.Logger _theLogger =
        java.util.logging.Logger.getLogger
            (gov.lbl.srm.client.main.SRMClientN.class.getName());
private java.util.logging.FileHandler _fh;
private Vector inputVec = new Vector ();

private PrintIntf pIntf;
private boolean _debug=false;
private boolean silent=false;
private boolean useLog=false;
private boolean overrideserviceurl=false;
private int statusMaxTimeAllowed=-1;
//private int statusMaxTimeAllowed=600;
private int statusWaitTime=30;
private int connectionTimeOutAllowed=600;
private int setHTTPConnectionTimeOutAllowed=600;
private boolean gotConnectionTimeOut;
private boolean gotHTTPConnectionTimeOut;



//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientRequest(String[] args, PrintIntf pIntf) {

  this.pIntf = pIntf;

  if(args.length == 0) {
    showUsage(true);
  }

  for(int i = 0; i < args.length; i++) {
    boolean ok = false;
    if(i == 0 && !args[i].startsWith("-")) {
      serviceUrl = args[i];
      serviceURLGiven=true;
      ok = true;
    }
    else if(args[i].equalsIgnoreCase("-conf") && i+1 < args.length) {
      configFileLocation = args[i+1];
      i++;
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
    /*
    else if(args[i].equalsIgnoreCase("-log4jlocation") && i+1 < args.length) {
      log4jlocation = args[i+1];
      i++;
    }
    */
    else if(args[i].equalsIgnoreCase("-nogui")) {
      ;
    }
    else if(args[i].equalsIgnoreCase("-abortrequest") && i+1 < args.length) { 
      onlyAbRequest=true;
      requestToken=args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-suspend") && i+1 < args.length) { 
      suspendRequest=true;
      requestToken=args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-resume") && i+1 < args.length) { 
      resumeRequest=true;
      requestToken=args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-abortfiles") && i+1 < args.length) { 
      onlyAbFiles=true;
      requestToken=args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-lite")) {
       ;
    }
    //else if(args[i].equalsIgnoreCase("-extlifetime") && i+1 < args.length) { 
    //else if(args[i].equalsIgnoreCase("-extlifetime")) { 
      //onlyExtFileLT=true;
      //requestToken=args[i+1];
      //i++;
    //}
    else if(args[i].equalsIgnoreCase("-requesttoken") && i+1 < args.length) { 
      requestToken=args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-lifetime") && i+1 < args.length) { 
      onlyExtFileLT=true;
      try {
        String temp = args[i+1];
        lifeTime = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) { 
        System.out.println("\nGiven LifeTime is not valid:"+ args[i+1]);
        System.out.println("Using default value : "+lifeTime);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-pinlifetime") && i+1 < args.length) { 
      onlyExtFileLT=true;
      try {
        String temp = args[i+1];
        pinLifeTime = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) { 
        System.out.println("\nGiven LifeTime is not valid:"+ args[i+1]);
        System.out.println("Using default value : "+pinLifeTime);
	  }
      i++;
    }
    else if(args[i].equalsIgnoreCase("-requestsummary") && i+1 < args.length) { 
      onlyReqSummary=true;
      requestToken=args[i+1];
      sourceUrl = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-getrequesttoken")) { 
      onlyGetRID=true;
    }
    else if(args[i].equalsIgnoreCase("-userdesc") && i+1 < args.length) { 
      userDesc = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-log") && i+1 < args.length) {
      useLog=true;
      eventLogPath = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-serviceurl") && i+1 < args.length) {
      serviceUrl = args[i+1];
      serviceURLGiven=true;
      i++;
    }
    else if(args[i].equalsIgnoreCase("-f") && i+1 < args.length) {
      inputFile = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-s") && i+1 < args.length) {
      sourceUrl = args[i+1];
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
    else if(args[i].equalsIgnoreCase("-delegation")) {
       if(i+1 < args.length) {
          delegationNeeded = ""+true;
          i++;
       }
       else {
          delegationNeeded = ""+true;
       }
    }
    else if(args[i].equalsIgnoreCase("-proxyfile") && i+1 < args.length) {
      proxyFile = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-debug")) {
      _debug=true;
    }
    else if(args[i].equalsIgnoreCase("-quiet")) {
      silent=true;
    }
    else if(args[i].equalsIgnoreCase("-renewproxy")) {
      isRenew = true;
    }
    else if(args[i].equalsIgnoreCase("-statusmaxtime") && i+1 < args.length) {
      String temp = args[i+1];
      try {
        statusMaxTimeAllowed = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {
          System.out.println("Given -statusmaxtime is not valid, using the default " +
            statusMaxTimeAllowed);
      }
      i++;
    }
    /*
    else if(args[i].equalsIgnoreCase("-statuswaittime") && i+1 < args.length) {
      String temp = args[i+1];
      try {
        statusWaitTime = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {
          System.out.println("Given -statuswaittime is not valid, using the default " +
            statusMaxTimeAllowed);
      }
      i++;
    }
    */
    else if(args[i].equalsIgnoreCase("-v2")) {
      ;
    }
    else if(args[i].equalsIgnoreCase("-version")) {
      SRMClientN.printVersion();
    }
    else if(args[i].equalsIgnoreCase("-help")) {
      showUsage (true);
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

  Properties sys_config = new Properties ();
  
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
			configFileLocation,"SRM-REQ",silent,useLog, _theLogger);
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
			configFileLocation,"SRM-REQ",silent, useLog,_theLogger);
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

  if(silent || useLog) {
  if(eventLogPath.equals("")) {
    String temp = (String) sys_config.get("eventlogpath");
    if(temp != null) {
      eventLogPath = temp;
    }
    else {
      eventLogPath = "./srmclient-event-"+detailedLogDate+".log";
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
     System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
  }
  }


  /*
  if(logPath.equals("")) {
    String temp = (String) sys_config.get("logpath");
    if(temp != null) {
      logPath = temp;
    }
    else {
      logPath = "./srm-client-detailed-"+detailedLogDate+".log";
    }
  }
  */
   
  /*
  if(silent) {
    if(logPath.equals("")) {
      System.out.println("\nFor the option quiet, -log is needed to " + 
        " forward to output to the logfile");
      System.out.println("Please provide -log <path to logfile> ");
      showUsage(false); 
    }
  }
  */

  
  String ttemp = System.getProperty("log4j.configuration");
  if(ttemp != null && !ttemp.equals("")) {
     log4jlocation = ttemp;
  }

  //setup log4j configuration
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
         log4jlocation = "logs/log4j_srmclient.properties";
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
         System.out.println("SRM-REQUEST: Given logfile location is a directory, please " +
            "give the full path name with desired log file name " + logPath);
         inputVec.clear();
	     inputVec.addElement("Given logfile location is a directory, please " +
            "give the full path name with desired log file name " + logPath);
         util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
         showUsage(false); 
      }
      }catch(Exception e) {
         System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
         inputVec.clear();
         inputVec.addElement(e.getMessage());
         util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
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

         while((ref= in.readLine()) != null) {
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
         Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientRequest");
         logger = LogFactory.getLog(c.getName());
      }catch(ClassNotFoundException cnfe) {
         System.out.println("ClassNotFoundException " + cnfe.getMessage());
                    util.printEventLogException(_theLogger,"",cnfe);
      }
    } 
  }
  else {
      logPath = "./srm-client-detailed.log";
  }
  */

  try {
  PropertyConfigurator.configure(log4jlocation);
  }catch(Exception ee){;}

  /*
  if(!logPath.equals("")) {
    util.printMessage("Log4jlocation " + log4jlocation, logger,silent);
    inputVec.clear();
    inputVec.addElement("Log4jlocation=" + log4jlocation);
    util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
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
         //System.out.println
		   //("\nProxyFile or UserCert and UserKey is not provided");
         try {
           //proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID();
           proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID2();
         }catch(Exception e) {
           System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
           proxyFile ="/tmp/x509up_u"+MyConfigUtil.getUID();
         }
         pConfig.setProxyFile(proxyFile);
         properties.put("proxy-file", proxyFile);
         //System.out.println("\nUsing default user proxy " + proxyFile);
       }
       else {
         if(userCert.equals("") || userKey.equals("")) {
           System.out.println
		     ("\nSRM-REQUEST: UserCert and UserKey both should be provided");
           inputVec.clear();
		   inputVec.addElement("UserCert and UserKey both should be provided");
           util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
           showUsage(false);
         }
       }
    }

    if(isRenew) {
      String v1 = (String) properties.get("user-cert");
      String v2 = (String) properties.get("user-key");
      if(v1.startsWith("Enter") || v2.startsWith("Enter")) {
        System.out.println("\nSRM-REQUEST: If you want to renew proxy automatically,\n "+
         "you need to enter user-cert location and user-key location.");
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically, ");
        inputVec.addElement("you need to enter user-cert location and user-key location.");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear();
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      String v3 = (String)properties.get("proxy-file");
      if(v3.startsWith("Enter")) {
        System.out.println("\nSRM-REQUEST: If you want to renew proxy automatically,\n "+
          "please enter your proxy file location.");
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically, ");
        inputVec.addElement("please enter your proxy file location.");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        inputVec.clear(); 
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      else {
        inputVec.clear(); 
        inputVec.addElement("Enter GRID passphrase");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        String line = PasswordField.readPassword("Enter GRID passphrase: ");
        _password = line;
      }
      //else there is no need to renew proxy.
    }

    String[] surl = null;
    surl = new String[1];
    String[] turl = null;
    turl = new String[1];

    Vector fileInfo = new Vector();

    if(inputFile.equals("")) { 
      if(onlyAbFiles || onlyExtFileLT || onlyReqSummary) {
      //if(onlyAbFiles || onlyExtFileLT) {
        if(sourceUrl.equals("")) {
           util.printMessage 
	   	     ("\nSRM-REQUEST: Please provide -s <sourceUrl>\n",logger,silent);
           util.printMessage 
		     ("or -f <inputfile>\n",logger,silent);
           inputVec.clear(); 
	   	   inputVec.addElement ("Please provide -s <sourceUrl> ");
		   inputVec.addElement("or -f <inputfile>");
           util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
           showUsage(false);
        }
        surl[0] = sourceUrl;
      }
      else {
        surl[0]="";
      }
      request = gov.lbl.srm.client.util.Util.createRequest(surl[0],"","",_debug,silent,useLog,
            "SRM-REQ",false,_theLogger,logger);
      request.setModeType("req");
    } 
    else {
      //by default request is get, so target dir check is done in parseXML
      request = gov.lbl.srm.client.util.Util.parseXML(inputFile,"SRM-REQ",silent,useLog,_theLogger);
      request.setModeType("req");
    }

    requestType = request.getModeType();

    String temp = "";

    if(onlyReqSummary) {
      fileInfo = request.getFiles();
    }

    if(onlyExtFileLT || onlyAbFiles) {
      fileInfo = validateURL(request,onlyExtFileLT);
      if(fileInfo.size() == 0) {
         inputVec.clear(); 
         inputVec.addElement("No files in the request for transfer");
         inputVec.addElement(" Cannot proceed further, please check input");
         util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
         util.printMessage("\nSRM-REQUEST: No files in the request for transfer",logger,silent);
         util.printMessage("Cannot proceed further, please check input",logger,silent);
         showUsage(false);
      }
      else {
        for(int i = 0; i < fileInfo.size(); i++) {
           FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
           temp = fIntf.getSURL();
           if(temp.startsWith("srm:/")) {       
              overrideserviceurl = true;
           }
        }
      }
    }

    if(overrideserviceurl) {
       serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
			temp,serviceURL,serviceHandle,servicePortNumber,1,silent,useLog,_theLogger,logger);
       if(serviceUrl == null) showUsage(false); 

       for(int i = 0; i < fileInfo.size(); i++) {
         FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
         String temp1 = fIntf.getSURL();
         String sfn = gov.lbl.srm.client.util.Util.getSFN(temp1);
         fIntf.setSURL(serviceUrl.replace("httpg","srm")+sfn);
       }
    }

    if(serviceUrl.equals("")) {
       String tt = (String) sys_config.get("ServiceUrl");
       if(tt != null) {
         serviceUrl = tt;
       }
       else {
         inputVec.clear(); 
         inputVec.addElement("Please provide the -serviceurl full SRM service url");
		 inputVec.addElement(" example:srm://<hostname>:<port>//wsdlpath");
         util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
         util.printMessage 
          ("\nSRM-REQUEST: Please provide the -serviceurl full SRM service url",logger,silent);
         util.printMessage 
		  ("  example:srm://<hostname>:<port>//wsdlpath",logger,silent);
              showUsage(false);
         }
      }

     GSSCredential credential=null;
     try {
       credential = gov.lbl.srm.client.util.Util.checkTimeLeft
            (pConfig,properties,_password, _theLogger,silent,useLog,logger,pIntf,_debug);
       proxyType=gov.lbl.srm.client.util.Util.getProxyType(properties);

     }catch(Exception ee) {
        System.out.println("\nSRM-CLIENT: Exception from client="+ee.getMessage());
                    util.printEventLogException(_theLogger,"",ee);
        inputVec.clear(); 
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(ee,pIntf);
        System.exit(92);
     }

      serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
		serviceUrl,serviceURL,serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
      if(serviceUrl == null) showUsage(false); 

      //util.printMessage("\nServiceUrl="+serviceUrl,logger,silent);

      if(_debug) {
      util.printMessage("\n===================================",logger,silent);
      util.printMessage("SRM Configuration",logger,silent);
      util.printMessage("\nserviceUrl   : " + serviceUrl,logger,silent);
      if(requestToken != null) { 
        util.printMessage("RequestToken : " + requestToken,logger,silent);
      }
      util.printMessage("Debug ON       : " + _debug,logger,silent);
      util.printMessage("Quiet ON       : " + silent,logger,silent);
      inputVec.clear(); 
      inputVec.addElement("ServiceUrl="+serviceUrl);
      inputVec.addElement("RequestToken="+requestToken);
      inputVec.addElement("Debug="+_debug);
      inputVec.addElement("Quiet="+silent);
      util.printEventLog(_theLogger,"Initialization",inputVec,silent,useLog);
      }

      if(suspendRequest || resumeRequest || onlyAbRequest
         || onlyAbFiles || onlyExtFileLT || onlyGetRID || 
            onlyReqSummary) {
        util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " +
            serviceUrl,logger,silent);
      }

       /*
      Properties props = System.getProperties();
      String uTemp = props.getProperty("user.name");
      if(uTemp != null) {
         uid = uTemp;
      }
       */

      SRMUtilClient utilClient = new SRMUtilClient(serviceUrl,uid,userDesc,
			   credential, _theLogger,logger, pIntf, _debug,silent,useLog,
			   false, false,
			   statusMaxTimeAllowed,statusWaitTime,storageInfo,proxyType,
			   connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
		       delegationNeeded,3,60);
      TStatusCode sCode = null;
      if(suspendRequest) {
        if(requestToken == null) { 
          inputVec.clear(); 
          inputVec.addElement("Please provide a valid RequestToken");
          util.printMessage("\nSRM-REQUEST: Please provide a valid RequestToken",
			logger,silent);
          util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
          showUsage(false);
        }
        inputVec.clear(); 
        if(_debug) {
          util.printEventLog(_theLogger,"Doing srm suspend request",inputVec,silent,useLog);
        }
        //util.printMessage("\nSRM-REQUEST: Doing srm Suspend Request ...",logger,silent);
        sCode = utilClient.doSrmSuspendRequest(requestToken);
      }
      else if (resumeRequest) {
        if(requestToken == null) { 
          inputVec.clear(); 
          inputVec.addElement("Please provide a valid RequestToken");
          util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
          util.printMessage("\nSRM-REQUEST: Please provide a valid RequestToken",
			logger,silent);
          showUsage(false);
        }
        inputVec.clear(); 
        inputVec.addElement("Doing srm Resume Request ...");
        if(_debug) {
          util.printMessage("\nSRM-REQUEST: Doing srm Resume Request ...",logger,silent);
        } 
        sCode = utilClient.doSrmResumeRequest(requestToken);
      }
      else if(onlyAbRequest) {
        if(requestToken == null) { 
          inputVec.clear(); 
          inputVec.addElement("Please provide a valid RequestToken");
          util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
          util.printMessage("\nSRM-REQUEST: Please provide a valid RequestToken",
			logger,silent);
          showUsage(false);
        }
        inputVec.clear(); 
        inputVec.addElement("Doing srm Abort Request ...");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        if(_debug) {
          util.printMessage("\nSRM-REQUEST: Doing srm Abort Request ...",logger,silent);
        }
        sCode = utilClient.doSrmAbortRequest(requestToken);
      }
      else if(onlyAbFiles) {
        if(requestToken == null) { 
          inputVec.clear(); 
          inputVec.addElement("Please provide a valid RequestToken");
          util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
          util.printMessage("\nSRM-REQUEST: Please provide a valid RequestToken",
			logger,silent);
          showUsage(false);
        }
        inputVec.clear(); 
        inputVec.addElement("Doing srm Abort Request ...");
        util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
        if(_debug) {
          util.printMessage("\nSRM-REQUEST: Doing srm Abort files ...",logger,silent);
        }
        sCode = utilClient.doSrmAbortFiles(fileInfo, requestToken);
      }
      else if(onlyExtFileLT) {
        //if -s <sourceUrl> -lifetime must be provided. (request token is not
        //necessary in this case.
        //or -s <surl> -requesttoken <requestToken> -pinlifetime <int>  
        if(inputFile.equals("")) {
          if(lifeTime == 0) 
		  {
            if(pinLifeTime == 0) {
              inputVec.clear(); 
              inputVec.addElement("Please provide -s <surl> -lifetime <lifetime>");
              inputVec.addElement(" or -s <surl> -pinlifetime <pintime> "); 
              util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
              util.printMessage("\nSRM-REQUEST: Please provide -s <surl> -lifetime <lifetime>"
               + " or -s <surl> -pinlifetime <pintime> and " +
					" -requesttoken <request_token>" +
                 " or -s <surl> -pinlifetime <pintime> -lifetime <lifetime> and " +
			 	 " -requesttoken <request_token>" , logger,silent);
              showUsage(false);
            }
          }
        }
        //if -f <fileName> is used, 
			//-lifeTime will override the individual options

        //when pinLifeTime is given, requesttoken is needed.
        if(pinLifeTime != 0) {
         if(requestToken == null) { 
            inputVec.clear(); 
            inputVec.addElement("Please provide -s <surl> -lifetime <lifetime> ");
            inputVec.addElement(" or -s <surl> -pinlifetime <pintime> "); 
			inputVec.addElement(" or -requesttoken <request_token> ");
            util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
            util.printMessage("\nSRM-REQUEST: Please provide -s <surl> -lifetime <lifetime>"
              + " or -s <surl> -pinlifetime <pintime> " +
				" and -requesttoken <request_token>" +
                " or -s <surl> -pinlifetime <pintime> -lifetime <lifetime> " +
				" and -requesttoken <request_token>" , logger,silent);
            showUsage(false);
         }
        }
        if(lifeTime != 0) {
          inputVec.clear(); 
          inputVec.addElement("LifeTime="+lifeTime);
          if(pinLifeTime == 0 && requestToken != null) {
			inputVec.addElement(" -requesttoken <request_token> is not needed for this option");
            inputVec.addElement("Please provide -s <surl> -lifetime <lifetime> ");
            inputVec.addElement(" or -s <surl> -pinlifetime <pintime> "); 
			inputVec.addElement(" and -requesttoken <request_token> ");
            util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
            util.printMessage
			  ("\nSRM-REQUEST: -requesttoken <request_token> is not needed for this option",
				logger, silent);
            util.printMessage("\nSRM-REQUEST: Please provide -s <surl> -lifetime <lifetime>"
              + " or -s <surl> -pinlifetime <pintime> -requestoken <request_token> " +
              " or -s <surl>  -pinlifetime <pintime> -lifetime <lifetime> " +
			  " and -requesttoken <request_token>", logger,silent);
            showUsage(false);
          }
        }
        else {
          inputVec.clear(); 
          inputVec.addElement("LifeTime=0");
        }
        if(pinLifeTime != 0) {
          inputVec.clear(); 
          inputVec.addElement("PinLifeTime="+pinLifeTime);
        }
        else {
          inputVec.clear(); 
          inputVec.addElement("PinLifeTime=0");
        }
        util.printEventLog(_theLogger,"Doing srm Extension file life time",inputVec,silent,useLog);
        if(_debug) {
        util.printMessage("\nSRM-REQUEST: Doing srm Extension file life time ...",logger,silent);
        }
        sCode = utilClient.doSrmExtFileLT(fileInfo, requestToken, lifeTime,pinLifeTime);
      }
      else if(onlyGetRID) {
        inputVec.clear(); 
        if(_debug) {
        if(userDesc.equals("")) {
          util.printMessage("SRM-REQUEST: UserDesc        : null",logger,silent);
          inputVec.addElement("UserDesc=null");
        }
        else {
          util.printMessage("SRM-REQUEST: UserDesc        : " + userDesc,logger,silent);
          inputVec.addElement("UserDesc="+userDesc);
        }
        } 
        util.printEventLog(_theLogger,"Doing srmGetRequestTokens",inputVec,silent,useLog);
        if(_debug) {
        util.printMessage("\nSRM-REQUEST: Doing SrmGetRequestTokens ...", logger,silent);
        }
        sCode = utilClient.doSrmGetRID(userDesc);
      }
      else if(onlyReqSummary) {
        inputVec.clear(); 
        util.printEventLog(_theLogger,"Doing srmRequestSummary",inputVec,silent,useLog);
        if(_debug) {
        util.printMessage("\nSRM-REQUEST: Doing srm Request Summary ...",logger,silent);
        } 
        sCode = utilClient.doSrmRequestSummary(fileInfo);
      }
      else {
        inputVec.clear(); 
        inputVec.addElement("-abortrequest <requesttoken>");
        inputVec.addElement("-suspend <requesttoken>");
        inputVec.addElement("-resume <requesttoken>");
        inputVec.addElement("-abortfiles <requesttoken>");
        inputVec.addElement("-getrequesttoken <requesttoken>");
        inputVec.addElement("-requestsummary <requesttoken>");
        util.printEventLog(_theLogger,"Please provide your options such as",inputVec,silent,useLog);
        util.printMessage("\nSRM-REQUEST: Please provide your options such as ",logger,silent);
        util.printMessage("\t -abortrequest    <RequestToken>",logger,silent);
        util.printMessage("\t -suspend    <RequestToken> ",logger,silent);
        util.printMessage("\t -resume    <RequestToken> ",logger,silent);
        util.printMessage("\t -abortfiles      <RequestToken> ",logger,silent);
        util.printMessage("\t -getrequesttoken           ",logger,silent);
        util.printMessage("\t -requestsummary  ",logger,silent);
      }
      int exitValue = util.mapStatusCode(sCode);
        inputVec.clear(); 
        inputVec.addElement("StatusCode="+exitValue);
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
      System.exit(exitValue);
    }catch(Exception e) {
        System.out.println("\nSRM-CLIENT: Exception from client="+e.getMessage());
                    util.printEventLogException(_theLogger,"",e);
        inputVec.clear(); 
        inputVec.addElement("StatusCode=92");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        util.printHException(e,pIntf);
      System.exit(92);
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRequestType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestType() {
  return requestType;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// validateURL
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Vector  validateURL(Request request,boolean checklifetime) {
  Vector result = new Vector ();
  Vector fInfo = request.getFiles();
  int size = fInfo.size();
  for(int i = 0; i < size; i++) {
    boolean skip = false;
    FileInfo f = (FileInfo) fInfo.elementAt(i);
    String surl = f.getOrigSURL();
    String turl = f.getOrigTURL();
    if(request.getModeType().equalsIgnoreCase("req")) {
       //if(lifeTime == 0) {
          //util.printMessage("\nSkipping this file, lifeTime must be specified",logger,silent);
          //util.printMessage("-lifeTime option",logger,silent);
          //skip=true;
       //}
       if((!surl.startsWith("srm://")) && (!surl.startsWith("http://")) &&
		  (!surl.startsWith("gsiftp://")) && (!surl.startsWith("ftp://"))) {
          inputVec.clear(); 
          inputVec.addElement("source url is not valid " + surl);
          inputVec.addElement("skipping this url in the request");
          util.printEventLog(_theLogger,"Please provide your options such as",inputVec,silent,useLog);
          util.printMessage("\nSRM-REQUEST: source url is not valid " + surl, logger,silent);
          util.printMessage("\nskipping this url in the request", logger,silent);
          skip = true;
       }
    }
    if(!skip) {
        result.add(f);
     }
   }
   size = result.size();  
   for(int i =0; i < result.size(); i++) { 
      FileInfo f = (FileInfo) result.elementAt(i);
      f.setLabel(i);
   }
   return result;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void showUsage (boolean b) {
 if(b) { 
    System.out.println("Usage :\n"+
            "\tsrm-request [command line options] -s <sourceurl> -abortfiles <RequestToken>\n"+
            "\tor srm-request [command line options] -s <sourceurl> -lifetime <lifetime> -pinlifetime <pinlifetime> \n"+
            "\tor srm-request [command line options] -s <sourceurl> -lifetime <lifetime> -pinlifetime <pinlifetime> -requesttoken <request_token> \n"+
            "\tor srm-request [command line options] -abortrequest <RequestToken>\n"+
            "\tor srm-request [command line options] -suspend <RequestToken>\n"+
            "\tor srm-request [command line options] -resume <RequestToken>\n"+
            "\tor srm-request [command line options] -requestsummary <RequestToken>\n"+
            "\tor srm-request [command line options] -getrequesttoken -userdesc <userdesc>\n"+
            "\tor srm-request [command line options] -f <file>\n" +
            "\t  where <file> is the path to the xml file containing\n"+
            "\t  the sourceurl information.\n"+
            "\tfor requests with more than one file -f option is used.\n" +
            "\tcommand line options will override the options from conf file\n"+
            "\n"+
            "\t-conf              <path>\n"+
            "\t-s                 <sourceUrl>  \n" +
            "\t-f                 <fileName> (input file in xml format)\n"+
            "\t                      either -s sourceurl\n"+
            "\t                      or -f <filename> is needed.\n"+
            "\t-authid            (SRM authorization id) \n"+
            "\t-serviceurl        <full wsdl service url> \n" +
            "\t                      example srm://host:port/wsdlpath \n"+
            "\t                      (required for requests when surl\n" +
            "\t                       does not have wsdl information)\n"+
            "\t-abortrequest      <request_token> (abort the request) \n" +
            "\t-suspend           <request_token> (suspend the request) \n" +
            "\t-resume            <request_token> (resume the request) \n" +
            "\t-lifetime          <file lifetime> \n"+
            "\t-pinlifetime       <file pin lifetime> \n"+
            "\t-requesttoken      <request_token> \n" +
            "\t-requestsummary    <request_token> (summary of the request)\n"+
            "\t-getrequesttoken   <request_token> \n"+
            "\t-userdesc          <user description> \n"+
            "\t-abortfiles        <request_token> \n" +
            "\t-delegation         uses delegation (default: no delegation) \n"+
	        "\t-proxyfile         <path to proxyfile> \n"+
            "\t                      default:from user default proxy lcoation\n"+
            "\t-usercert          <path to usercert> \n" +
            "\t-userkey           <path to userkey> \n" +
            //"\t-renewproxy        (renews proxy automatically) \n" +
            //"\t-statuswaittime    (wait time between status checking in seconds)default:15\n"+
            //"\t-statusmaxtime     (maximum time for status checking before\n"+
			//"\t                     timeout in seconds) default:600\n"+
	    //"\t-connectiontimeout <integer in seconds> (enforce http connection timeout in the given seconds)default:600 \n"+
            "\t-sethttptimeout    <integer in seconds> (enforce SRM/httpg connection timeout and sets client-side http connection timeout in the given seconds)default:600 \n"+

	        "\t-quiet             default:false\n" +
            "\t                      suppress output in the console.\n" +
            "\t                      this option prints the output to the logfile default:./srmclient-event-date-random.log\n" +
            "\t-log               <path to logfile>\n" +
            "\t-debug             default:false\n" +
            "\t-help              show this message.");
 } 
        inputVec.clear(); 
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
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
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getPassword
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getPassword() {
  return _password;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  main driver method
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void main(String[] args) {
   new SRMClientRequest(args, null);
}

}

