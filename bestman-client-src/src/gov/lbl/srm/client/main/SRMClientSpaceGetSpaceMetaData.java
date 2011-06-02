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
// Class SRMClientSpaceGetSpaceMetaData
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientSpaceGetSpaceMetaData 
{

private GSSCredential mycred;
private XMLParseConfig pConfig = new XMLParseConfig();

private Properties properties = new Properties();
private String configFileLocation = "";
private String _password ="";
private String userKey="";
private String userCert="";
private String proxyFile="";
private boolean isRenew = false;
private boolean textReport=true;
private String serviceUrl="";
private boolean spaceTokenGiven=false;
private boolean serviceURLGiven=false;
private String serviceURL="";
private int servicePortNumber=0;
private String serviceHandle="";
private int proxyType;
private String uid="";
private String userDesc="";
private String fileToken;
private boolean overrideserviceurl=false;
private String log4jlocation="";
private String delegationNeeded="";
private String logPath="";
private String eventLogPath="";
private int statusMaxTimeAllowed=-1;
private int statusWaitTime=30;
private int connectionTimeOutAllowed=600;
private int setHTTPConnectionTimeOutAllowed=600;
private boolean gotConnectionTimeOut;
private boolean gotHTTPConnectionTimeOut;
private int numRetry=3; //default
private int retryTimeOut=60; //default 60 seconds


private static Log logger;
private static PrintIntf pIntf;

private SRMWSDLIntf srmCopyClient;

private boolean _debug=false;
private boolean silent = false;
private boolean useLog=false;

private java.util.logging.Logger _theLogger =
        java.util.logging.Logger.getLogger
            (gov.lbl.srm.client.main.SRMClientN.class.getName());
private java.util.logging.FileHandler _fh;

private Vector inputVec = new Vector ();


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientSpaceGetSpaceMetaData
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientSpaceGetSpaceMetaData(String[] args, PrintIntf pIntf) {

  this.pIntf = pIntf;

  /*
  if(uid.equals("")) {
    //extract username from env.
    Properties props = System.getProperties();
    String tuid =props.getProperty("user.name");
    if(tuid != null) {
      uid = tuid; 
    }
  }
  */

   if(args.length == 0) {
    showUsage(true);
  }


  for(int i = 0; i < args.length; i++) {
    boolean ok = false;
    if(i == 0 && !args[i].startsWith("-")) {
      serviceUrl=args[i];
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
    else if(args[i].equalsIgnoreCase("-spacetoken")) {
      fileToken = args[i+1];
      spaceTokenGiven=true;
      i++;
    }
    else if(args[i].equalsIgnoreCase("-serviceurl") && i+1 < args.length) {
      serviceUrl = args[i+1];
      serviceURLGiven=true;
      i++;
    }
    else if(args[i].equalsIgnoreCase("-lite")) {
       ;
    }
    else if(args[i].equalsIgnoreCase("-log") && i+1 < args.length) {
      useLog=true;
      eventLogPath = args[i+1];
      i++;
    }
    else if(args[i].equalsIgnoreCase("-authid") && i+1 < args.length) {
      uid = args[i+1];
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
    /*
    else if(args[i].equalsIgnoreCase("-textreport")) {
      textReport = true;
    }
    */
    else if(args[i].equalsIgnoreCase("-proxyfile") && i+1 < args.length) {
      proxyFile = args[i+1];
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
    else if(args[i].equalsIgnoreCase("-debug")) {
      _debug=true;
    }
    else if(args[i].equalsIgnoreCase("-quiet")) {
      silent=true;
    }
    else if(args[i].equalsIgnoreCase("-renewproxy")) {
      isRenew = true;
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
		configFileLocation,"SRM-CLIENT",silent, useLog,_theLogger);
    }
  }catch(Exception e) {
    util.printMessage("SRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);
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
	   	   configFileLocation,"SRM-CLIENT",silent,useLog, _theLogger);
       }
     } catch(Exception e) {
        util.printMessage("SRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);
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

  String detailedLogDate = util.getDetailedDate();


  /*
  if(logPath.equals("")) {
    String temp = (String) sys_config.get("logpath");
    if(temp != null) {
      logPath = temp;
    }
    else {
      logPath ="./srm-client-detailed-"+detailedLogDate+".log";
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
     util.printMessage("SRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);
           util.printEventLogException(_theLogger,"",e);
  }
  }

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
         System.out.println("Given logpath is a directory, please give " +
			"full path name with desired log file name " + logPath);
         showUsage(false);
      }
      }catch(Exception e) {
         util.printMessage("SRM-CLIENT: Exception from client=" + e.getMessage(),logger,silent);
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
         util.printMessage("SRM-CLIENT: Exception from client=" + ex.getMessage(),logger,silent);
           util.printEventLogException(_theLogger,"",ex);
         showUsage(false);
      }
      log4jlocation=logPathDir+"/log4j_srmclient.properties";
      PropertyConfigurator.configure(log4jlocation);
      ClassLoader cl = this.getClass().getClassLoader();
      try {
         Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientSpace");
         logger = LogFactory.getLog(c.getName());
      }catch(ClassNotFoundException cnfe) {
         System.out.println("ClassNotFoundException " + cnfe.getMessage());
      }
    }
  }
  else {
     logPath ="./srm-client-detailed.log";
  }
  */

  try {
  PropertyConfigurator.configure(log4jlocation);
  }catch(Exception ee){;}

  /*
  if(_debug) {
    util.printMessage("Log4jlocation " + log4jlocation, logger,silent);
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
           inputVec.clear();
           inputVec.addElement("UserCert and UserKey both should be provided");
           util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog); 
           System.out.println
		     ("\nSRM-CLIENT: UserCert and UserKey both should be provided");
           showUsage(false);
         }
       }
     }

     if(isRenew) {
      String v1 = (String) properties.get("user-cert");
      String v2 = (String) properties.get("user-key");
      if(v1.startsWith("Enter") || v2.startsWith("Enter")) {
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically, " +
         "you need to enter user-cert location and user-key location.");
        util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog); 
        System.out.println("\nSRM-CLIENT: If you want to renew proxy automatically,\n "+
         "you need to enter user-cert location and user-key location.");
        inputVec.clear();
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      String v3 = (String)properties.get("proxy-file");
      if(v3.startsWith("Enter")) {
        inputVec.clear();
        inputVec.addElement("If you want to renew proxy automatically, "+
          "please enter your proxy file location.");
        util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog); 
        System.out.println("\nSRM-CLIENT: If you want to renew proxy automatically,\n "+
          "please enter your proxy file location.");
        inputVec.clear();
        inputVec.addElement("StatusCode=93");
        util.printEventLog(_theLogger,"StatusExit",inputVec,silent,useLog);
        System.exit(93); 
      }
      else {
        inputVec.clear();
        inputVec.addElement("Enter GRID passphrase");
        util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog); 
        String line = PasswordField.readPassword("Enter GRID passphrase: ");
        _password = line;
      }
      //else there is no need to renew proxy.
     }

     String[] surl=null;

     Vector fileInfo = new Vector();
     String temp = "";

      if(serviceUrl.equals("")) {
          String tt = (String) sys_config.get("ServiceUrl");
          if(tt != null) {
             serviceUrl = tt;
          }
          else {
           inputVec.clear();
		   inputVec.addElement("Please provide the -serviceurl full SRM service url");
           inputVec.addElement (" example:srm://<hostname>:<port>//wsdlpath");
           util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog);
           util.printMessage 
		      ("\nSRM-CLIENT: Please provide the -serviceurl full SRM service url",logger,silent);
           util.printMessage 
		      ("  example:srm://<hostname>:<port>//wsdlpath",logger,silent);
           util.printMessageHException 
		      ("\nSRM-CLIENT: Please provide the -serviceurl full SRM service url",pIntf);
           util.printMessageHException 
		      ("  example:srm://<hostname>:<port>//wsdlpath",pIntf);
                showUsage(false);
          }
      }

       GSSCredential credential=null;
     try {
       credential = gov.lbl.srm.client.util.Util.checkTimeLeft
            (pConfig,properties,_password, _theLogger,silent,useLog,logger,pIntf,_debug);
       proxyType=gov.lbl.srm.client.util.Util.getProxyType(properties);
     }catch(Exception ee) {
        util.printMessage("SRM-CLIENT: Exception from client=" + ee.getMessage(),logger,silent);
           util.printEventLogException(_theLogger,"",ee);
        util.printMessageHException("Exception="+ee.getMessage(),pIntf);
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

      serviceUrl = gov.lbl.srm.client.util.Util.getServiceUrl(
			serviceUrl,serviceURL,serviceHandle,servicePortNumber,0,silent,useLog,_theLogger,logger);
      if(serviceUrl == null) showUsage(false);
      //util.printMessage("ServiceUrl " + serviceUrl,logger,silent);
      inputVec.clear();
      inputVec.addElement("ServiceUrl="+serviceUrl);
      util.printEventLog(_theLogger,"SRMClientSpace",inputVec,silent,useLog);

      if(_debug) {
      util.printMessage("\n===================================",logger,silent);
      util.printMessage("\n===================================",pIntf);
      util.printMessage("SRM-CLIENT configuration",logger,silent);
      util.printMessageHL("SRM-CLIENT configuration",pIntf);
      util.printMessage("\nserviceUrl     : " + serviceUrl,logger,silent);
      util.printMessage("\nserviceUrl     : " + serviceUrl,pIntf);
      inputVec.clear();
      inputVec.addElement("ServiceUrl="+serviceUrl);
      util.printMessage("userid         : " + uid,logger,silent);
      util.printMessage("userid         : " + uid,pIntf);
      util.printMessage("SpaceToken     : " + fileToken,logger,silent);
      util.printMessage("SpaceToken     : " + fileToken,pIntf);
      inputVec.addElement("userid="+uid);
      util.printMessage("Debug ON       : " + _debug,logger,silent);
      util.printMessage("Quiet ON       : " + silent,logger,silent);
      util.printMessage("Debug ON       : " + _debug,pIntf);
      util.printMessage("Quiet ON       : " + silent,pIntf);
      inputVec.addElement("Debug="+_debug);
      inputVec.addElement("Quiet="+silent);

      util.printEventLog(_theLogger,"Initialization",inputVec,silent,useLog);
      }

      if(fileToken == null) {
         inputVec.clear();
         inputVec.addElement("Please specify the space token");
         util.printEventLog(_theLogger,"Error",inputVec,silent,useLog);
         util.printMessage ("\nSRM-CLIENT: Please specify the space token ",
				logger,silent);
         util.printMessageHException
			("\nSRM-CLIENT: Please specify the space token ", pIntf);
         showUsage(false);
      }

        util.printMessage("SRM-CLIENT: " + "Connecting to serviceurl " +
            serviceUrl,logger,silent);
        util.printMessageHL("SRM-CLIENT: " + "Connecting to serviceurl " +
            serviceUrl,pIntf);

       
      SRMUtilClient utilClient = 
	   new SRMUtilClient(serviceUrl,uid, userDesc, credential,
               _theLogger, logger, pIntf, _debug,silent,useLog,false,false,
	       statusMaxTimeAllowed,statusWaitTime,"",proxyType,
	       connectionTimeOutAllowed,setHTTPConnectionTimeOutAllowed,
	       delegationNeeded,3,60);

      ISRM srm = utilClient.getISRM();

      TStatusCode sCode = 
          SRMUtilClient.getSpaceTokenMeta(srm,fileToken,logger,_debug,
             credential,proxyType,serviceUrl,_theLogger,pIntf,null,
             silent,useLog,connectionTimeOutAllowed,    
             setHTTPConnectionTimeOutAllowed,delegationNeeded,numRetry,
             retryTimeOut);

      if(pIntf == null) {
       int exitValue = util.mapStatusCode(sCode);
       inputVec.clear();
       inputVec.addElement("ExitValue="+exitValue);
       util.printEventLog(_theLogger,"StatusCode",inputVec,silent,useLog);
       System.exit(exitValue);
      }
   }catch(Exception e) {
      //util.printStackTrace(e,logger);
      //util.printMessage("Exception="+e.getMessage(),logger,silent);
       util.printMessage("SRM-CLIENT: Exception from server=" + e.getMessage(),logger,silent);
           util.printEventLogException(_theLogger,"",e);
      if(pIntf == null) {
       int exitValue = 92;
       inputVec.clear();
       inputVec.addElement("ExitValue="+exitValue);
       util.printEventLog(_theLogger,"StatusCode",inputVec,silent,useLog);
       System.exit(exitValue);
      }
      util.printHException(e,pIntf);
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void showUsage (boolean b) {
 if(b) { 
    System.out.println("Usage :\n"+
            "\tsrm-sp-info -spacetoken <space_token> [command line options] \n" +
            "\tor srm-sp-info <service_url> [command line options] \n" +
            "\tcommand line options will override the options from conf file\n"+
            "\n"+
            "\t-conf              <path>\n"+
		    "\t-serviceurl        <full wsdl service url> \n" +
            "\t                      example srm://host:port/wsdlpath \n"+
            "\t-authid            (user authorization id used in SRM)\n"+
            "\t-spacetoken        <space_token> (valid space token \n" +
            "\t                      returned by server)\n"+
			"\t-delegation        uses delegation (default: no delegation)\n"+ 
            "\t-proxyfile         <path to proxyfile> \n"+
            "\t                      (default:from user default proxy location\n"+
            "\t-usercert          <path to usercert> \n" +
            "\t-userkey           <path to userkey> \n" +
            //"\t-renewproxy             (renews proxy automatically) \n" +
            //"\t-connectiontimeout <integer in seconds> (enforce http connection timeout in the given seconds)default:600 \n"+
            "\t-sethttptimeout    <integer in seconds> (enforce SRM/httpg connection timeout and sets client-side http connection timeout in the given seconds)default:600 \n"+
            "\t-debug             default:false\n" +
	        "\t-quiet             default:false\n" +
            "\t                      suppress output in the console.\n" +
            "\t                      this option puts the output to the logfile default:./srmclient-event-date-random.log\n" +
            "\t-log              <path to logfile>\n" +
            "\t-help              show this message.");
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
   new SRMClientSpaceGetSpaceMetaData(args,null);
}

}

