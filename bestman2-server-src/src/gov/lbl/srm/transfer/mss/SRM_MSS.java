/**
 *
 * BeStMan Copyright (c) 2007-2008, 
 * The Regents of the University of California,
 * through Lawrence Berkeley National Laboratory (subject to receipt of any
 * required approvals from the U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this software,
 * please contact Berkeley Lab's Technology Transfer Department at TTD@lbl.gov.
 *
 * NOTICE.  This software was developed under partial funding from the
 * U.S. Department of Energy.  As such, the U.S. Government has been
 * granted for itself and others acting on its behalf a paid-up,
 * nonexclusive, irrevocable, worldwide license in the Software to
 * reproduce, prepare derivative works, and perform publicly and
 * display publicly.  Beginning five (5) years after the date permission
 * to assert copyright is obtained from the U.S. Department of Energy,
 * and subject to any subsequent five (5) year renewals, the
 * U.S. Government is granted for itself and others acting on its
 * behalf a paid-up, nonexclusive, irrevocable, worldwide license in
 * the Software to reproduce, prepare derivative works, distribute
 * copies to the public, perform publicly and display publicly, and
 * to permit others to do so.
 *
 * Email questions to SRM@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 *
*/

package gov.lbl.srm.transfer.mss;


import java.io.*;
import java.util.*;

import org.globus.util.ConfigUtil;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

import gov.lbl.srm.transfer.mss.intf.*;
import gov.lbl.srm.transfer.mss.hpss.SRM_MSS_UTIL;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_MSS implements callerIntf {
 protected SRM_ACCESS_TYPE accessType; 
 protected String MSSHost="";
 //protected int    MSSPort=0;
 protected int    MSSPort=2811;
 protected String HSIHost=""; 
 protected int HSIPort=0; 
 protected int bufferSize = 1048576;
 protected int parallelism=1;
 protected boolean dcau=true;
 protected String logPath=""; 
 protected String recoveryPath="";
 protected String detailedLogPath="";
 protected String setscipath="";
 protected String sitespecific2="";
 protected String ncarmssuidpath=""; 
 //used in SRM_MSS_HPSS in mssFilePut,
 // BNL needs quote site setcos 18 
 //used in SRM_MSS_NCAR
 protected boolean enableLogging=false;
 //protected boolean enableDetailedLog=false;
 protected boolean enableRecovery=false;
 protected boolean enableHSI=false;
 protected boolean enableSearchTapeId=false;
 protected boolean passiveMSSListing=true;
 protected int sizeOfRetrieveQueue=10; //this is the tape id searching number
 protected int sizeOfArchiveQueue=10;
 protected int sizeOfOthersQueue=10;
 protected int maximumQueueSize=15;
 protected int MSSMaxAllowed=5; //concurrent MSS connections
 protected int MSSMaxRetrial=3; //how many times to re-try the connection
 protected int MSSWaitTime=120; //re-trial time in seconds
 protected int debugLevel=10;

 protected ObjectFIFO retrieveQueue = new ObjectFIFO(maximumQueueSize);
 protected ObjectFIFO archiveQueue  = new ObjectFIFO(maximumQueueSize);
 protected ObjectFIFO othersQueue   = new ObjectFIFO(maximumQueueSize);
 protected Hashtable statusMap = new Hashtable(); 
 protected int nextQueueTurn;
 protected Hashtable userInfo = new Hashtable();
 protected Hashtable queueStatusMap = new Hashtable();
 protected int mssPending;
 protected String pftpPath="";
 protected String hsiPath="";

 protected String previousTapeId="";
 protected String _errorMessage="";
 protected boolean _errorOccured;
 protected boolean _completed;
 protected StringBuffer outputBuffer = new StringBuffer();
 protected String javaLibraryPath="";

 protected static final int MEGABYTE=1048576;
 protected static final int waitTime=300;
 protected static final int longWaitTime=600;
 protected Log logger = null;
 protected String log4jlocation="";

 protected java.util.logging.Logger _theLogger = 
		java.util.logging.Logger.getLogger 
			(gov.lbl.srm.transfer.mss.SRM_MSS.class.getName());
 protected java.util.logging.FileHandler _fh;
 protected MonitorThreadMain monitorThread = null;
 protected MonitorThreadPool monitorThreadPool = null;
 private int rId=0;
 private int keepLogFiles=2;
 protected boolean threadDisable;
 protected SRM_PING_STATUS srmPingStatus = new SRM_PING_STATUS();
 protected int logFileSize = 50000000; //default 50 MB log size
 protected int maxWait = 3000000;
 protected int waitDelay = 20000;
 protected int processTimeOutAllowed = 7200; //default 7200 sec.

 public File scriptPathDir=null;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_MSS () throws Exception { 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initialize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void init(Properties sys_config) throws Exception {

    String accessType = (String) sys_config.get("accesstype");
    if(accessType == null) {
      throw new SRM_MSS_Exception("AccessType cannot be null");
    }
    else {
      if(accessType.equalsIgnoreCase("gsi"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_GSI;
      else if(accessType.equalsIgnoreCase("plain"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN;
      else if(accessType.equalsIgnoreCase("encrypt"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT;
      else if(accessType.equalsIgnoreCase("kerberos"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_KERBEROS;
      else if(accessType.equalsIgnoreCase("ssh"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_SSH;
      else if(accessType.equalsIgnoreCase("scp"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_SCP;
      else if(accessType.equalsIgnoreCase("ncarmss"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_NCARMSS;
      else if(accessType.equalsIgnoreCase("lstoremss"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_LSTOREMSS;
      else if(accessType.equalsIgnoreCase("none"))
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_NONE;
      else 
        this.accessType = SRM_ACCESS_TYPE.SRM_ACCESS_UNKNOWN;
    }
    
    MSSHost = (String) sys_config.get("msshost");
    if(MSSHost == null) {
      //throw new SRM_MSS_Exception("MSSHost cannot be null");
    }

    String temp = (String) sys_config.get("mssport");

    if(temp == null) {
      //throw new SRM_MSS_Exception("MSSPort cannot be null");
    }
    else {
      try {
        MSSPort = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {
        throw new SRM_MSS_Exception
			("Given MSSPost is not a valid integer. " + temp);
      }
    }

    temp = (String) sys_config.get("keeplogfiles");
    
    if(temp != null) {
      try {
        int x = Integer.parseInt(temp);
        keepLogFiles = x;
      }catch(NumberFormatException nfe) { }
    }


    temp = (String) sys_config.get("logpath");
    if(temp != null) {
      logPath = temp;
    }

    temp = (String) sys_config.get("recoverypath");
    if(temp != null) {
      recoveryPath = temp;
    }

    //temp = (String) sys_config.get("javalibrarypath");
    temp = (String) sys_config.get("ldlibpath");
    if(temp != null) {
      javaLibraryPath = temp;
    }

    temp = (String) sys_config.get("setscipath");
    if(temp != null) {
      setscipath = temp;
    }

    temp = (String) sys_config.get("sitespecific2");
    if(temp != null) {
      sitespecific2 = temp;
    }

    temp = (String) sys_config.get("ncarmssuidpath");
    if(temp != null) {
      ncarmssuidpath = temp;
    }

    temp = (String) sys_config.get("pftppath");
    if(temp != null) {
      pftpPath = temp;
      if(MSSHost == null || MSSHost.equals("")) {
        throw new SRM_MSS_Exception("MSSHost cannot be null");
      }
    }

    temp = (String) sys_config.get("hsipath");
    if(temp != null) {
      hsiPath = temp;
      enableHSI=true;
      int idx = hsiPath.indexOf("-h");
      if(idx == -1) { 
        if(MSSHost == null || MSSHost.equals("")) {
          throw new SRM_MSS_Exception("MSSHost cannot be null");
        }
      }
    }

    if(accessType.equalsIgnoreCase("none")) {
      if(hsiPath.equals("")) {
        throw new SRM_MSS_Exception("Please provide a valid hsipath");
      }
    }

    temp = (String) sys_config.get("enablelogging");
    if(temp != null) {
      Boolean b = new Boolean(temp);
      enableLogging = b.booleanValue();
    }

    /*
    temp = (String) sys_config.get("enabledetailedlog");
    if(temp != null) {
      Boolean b = new Boolean(temp);
      enableDetailedLog = b.booleanValue();
    }
    */

    temp = (String) sys_config.get("enablerecovery");
    if(temp != null) {
      Boolean b = new Boolean(temp);
      enableRecovery = b.booleanValue();
    }

    temp = (String) sys_config.get("enablesearchtapeid");
    if(temp != null) {
      Boolean b = new Boolean(temp);
      enableSearchTapeId = b.booleanValue();
    }
   
    temp = (String) sys_config.get("debuglevel");
    if(temp != null) {
      try { 
        debugLevel = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("sizeofretrievequeue");
    if(temp != null) {
      try { 
        sizeOfRetrieveQueue = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("sizeofarchivequeue");
    if(temp != null) {
      try { 
        sizeOfArchiveQueue = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("logfilesize");
    if(temp != null) {
      try { 
        logFileSize = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("maxwait");
    if(temp != null) {
      try {
       maxWait = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("waitdelay");
    if(temp != null) {
      try {
       waitDelay = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("processtimeoutallowed");
    if(temp != null) {
      try {
       processTimeOutAllowed = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("sizeofothersqueue");
    if(temp != null) {
      try { 
        sizeOfOthersQueue = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("passivemsslisting");
    if(temp != null) {
       Boolean b = new Boolean(temp);
       passiveMSSListing = b.booleanValue();
    }

    temp = (String) sys_config.get("mssmaximumallowed");
    if(temp != null) {
      try { 
        MSSMaxAllowed = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("mssmaxretrial");
    if(temp != null) {
      try { 
        MSSMaxRetrial = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("msswaittime");
    if(temp != null) {
      try { 
        MSSWaitTime = Integer.parseInt(temp); 
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("parallelism");
    if(temp != null) {
      try {
        parallelism = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("buffersize");
    if(temp != null) {
      try {
        bufferSize = Integer.parseInt(temp);
      }catch(NumberFormatException nfe) {}
    }

    temp = (String) sys_config.get("dcau");
    if(temp != null) {
      try {
        Boolean b = new Boolean(temp);
        dcau = b.booleanValue();
      }catch(NumberFormatException nfe) {}
    }

    if(MSSMaxAllowed > sizeOfOthersQueue ) {
      System.out.println
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfOthersQueue="+ sizeOfOthersQueue);
      throw new SRM_MSS_Exception
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfOthersQueue="+ sizeOfOthersQueue);
    }
    if(MSSMaxAllowed > sizeOfRetrieveQueue ) {
      System.out.println
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfRetrieveQueue="+ sizeOfRetrieveQueue);
      throw new SRM_MSS_Exception
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfRetrieveQueue="+ sizeOfRetrieveQueue);
    }
    if(MSSMaxAllowed > sizeOfArchiveQueue ) {
      System.out.println
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfArchiveQueue="+ sizeOfArchiveQueue);
      throw new SRM_MSS_Exception
		("MSSMaxAllowed="+MSSMaxAllowed + 
			" cannot be more than sizeOfArchiveQueue="+ sizeOfArchiveQueue);
    }

    if(!logPath.equals("")) {
      //check existence of logfile.
      if(logPath.endsWith("/")) {
        logPath = logPath.substring(0,logPath.length()-1);
      }

     try {
       File f = new File(logPath);
       if(!f.exists()) {
         throw new SRM_MSS_Exception 
           ("Given log path does not exists " + logPath);
       }
       scriptPathDir = new File(logPath+"/scripts");
       boolean b = scriptPathDir.mkdirs();
       if(!b) { 
         if(!scriptPathDir.exists()) {
           throw new SRM_MSS_Exception 
             ("script path does not exists " + logPath+"/scripts");
         }
       }
     }catch(Exception e) {
         throw new SRM_MSS_Exception(e.getMessage());
     }
   }

    String tempLogPath="";

    String currentDate = SRM_MSS.printDate();

    if(enableLogging) {
      if(!logPath.equals("")) {
        //tempLogPath = logPath+"/"+"srm_mss."+currentDate+".log";
        tempLogPath = logPath+"/"+"srm_mss_event";
       _fh = new java.util.logging.FileHandler(logPath+"/srm_mss_event.log",logFileSize,10,true);
      }
      else {
        //tempLogPath = "srm_mss."+currentDate+".log";
       _fh = new java.util.logging.FileHandler("srm_mss_event.log",logFileSize,10,true);
      }

     try {
      //_fh = new java.util.logging.FileHandler(tempLogPath);
      //_fh = new java.util.logging.FileHandler(tempLogPath"%g.log",1000,10,true);
      _fh.setFormatter(new NetLoggerFormatter());  
      _theLogger.addHandler(_fh); 
      _theLogger.setLevel(java.util.logging.Level.ALL);

      File f = new File(tempLogPath+".lck");
      if(f.exists()) {
        f.delete();
      }

      Object[] param = new Object[26];
      param[0] = "MSSHOST="+MSSHost;
      param[1] = "MSSPORT="+MSSPort;
      param[2] = "ACCESSTYPE="+accessType;
      param[3] = "ENABLELOG="+enableLogging+" LOGPATH="+tempLogPath;
      //param[4] = "ENABLEDETAILEDLOG="+
		//enableDetailedLog+" DETAILEDLOGPATH="+detailedLogPath;
      param[4] = "";
      param[5] = "ENABLERECOVERY="+enableRecovery+
			" RECOVERYPATH="+recoveryPath;
      param[6] = "ENABLESEARCHTAPEID="+enableSearchTapeId;
      param[7] = "JAVALIBPATH="+javaLibraryPath;
      param[8] = "SETSCIPATH="+setscipath;
      param[9] = "SITESPECIFIC2="+sitespecific2;
      param[10] = "NCARMSSUIDPATH="+ncarmssuidpath;
      param[11] = "KEEPLOGFILES="+keepLogFiles;
      param[12]="HSIPATH="+hsiPath;
      param[13]="DEBUGLEVEL="+debugLevel;
      param[14]="READQUEUESIZE="+sizeOfRetrieveQueue;
      param[15]="WRITEQUEUESIZE="+sizeOfArchiveQueue;
      param[16]="OTHERQUEUESIZE="+sizeOfOthersQueue;
      param[17]="PASSIVEMSSLISTING="+passiveMSSListing;
      param[18]="MSSMAXALLOWED="+MSSMaxAllowed;
      param[19]="MSSMAXRETRIAL="+MSSMaxRetrial;
      param[20]="MSSWAITTIME="+MSSWaitTime;
      param[21]="MSSWAITTIME="+MSSWaitTime;
      param[22]="LOGFILESIZE="+logFileSize;
      param[23]="MAXWAIT="+maxWait;
      param[24]="WAITDELAY="+waitDelay;
      param[25]="PROCESSTIMEOUT="+processTimeOutAllowed;

      _theLogger.log(java.util.logging.Level.FINE,
			"INITILIZATION", (Object[]) param);
      }catch(Exception e) {
          util.printMessage("Exception : " + e.getMessage());
      }
    }

    String ttemp = System.getProperty("log4j.configuration");
    if(ttemp != null && !ttemp.equals("")) {
       log4jlocation = ttemp;
    }

    /*
    if(enableDetailedLog) {

      String ttemp = System.getProperty("log4j.configuration");
      if(ttemp != null && !ttemp.equals("")) {
        log4jlocation = ttemp;  
      }
      else {
        temp = (String) sys_config.get("log4jlocation"); 
        if(temp != null) {
          log4jlocation = temp;
        }
        else {
          log4jlocation = "log4j_srmmss.properties";  
        }
      }

      if(!logPath.equals("")) { 
        detailedLogPath = logPath+"/"+"srm_mss_detailed.log";

        //rewrite the log4j conf file with the new log path

        try {

          String ref;
          FileInputStream file = new FileInputStream (log4jlocation);
          BufferedReader in = 
				new BufferedReader ( new InputStreamReader (file));

          FileOutputStream outFile = 
				new FileOutputStream(logPath+"/log4j_srmmss.properties");

          BufferedWriter out = 
		    	new BufferedWriter(new OutputStreamWriter(outFile));

          while ((ref= in.readLine()) != null) {
             if(ref.startsWith("log4j.appender.SRMMSS.File")) {
                out.write("log4j.appender.SRMMSS.File="+detailedLogPath+"\n"); 
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
            throw new IOException("cannot read from " + ex.getMessage());
        }
        log4jlocation=logPath+"/log4j_srmmss.properties";
      }
      else {
        detailedLogPath = "srm_mss_detailed.log";
      }

      PropertyConfigurator.configure(log4jlocation);

      ClassLoader cl = this.getClass().getClassLoader();

      try {
       Class c = cl.loadClass("gov.lbl.srm.transfer.mss.SRM_MSS");
       logger = LogFactory.getLog(c.getName()); 
      }catch(ClassNotFoundException cnfe) {
         System.out.println("ClassNotFoundException" + cnfe.getMessage());
      }


    }//if(enableDetailedLogging)
    */
   
    //try {
      //PropertyConfigurator.configure(log4jlocation);
    //}catch(Exception ee) {}

    //remove old outputfiles and proxyfiles

    SRM_MSS_UTIL.removeOldLogFiles(logPath,keepLogFiles);
    
}

void setOutputBuffer(StringBuffer output) {
  outputBuffer = output;
}

StringBuffer getOutputBuffer() {
  return outputBuffer;
}

void setCompleted(boolean b) {
  _completed = b;
}

protected boolean getCompleted() {
  return _completed; 
}

public void setErrorMessage(String message) {
   _errorMessage = message;
}

protected String getErrorMessage() {
  return _errorMessage;
}

private String getRequestType(String requestToken) {
  if(requestToken.startsWith("mssget")) {
    return "get";
  }
  else if(requestToken.startsWith("mssput")) {
    return "put";
  }
  else {
    return "others";
  }
}

private boolean checkLogFileForErrors(String ftptransferlog) {
   try {
     File f = new File(ftptransferlog);
     if(!f.exists()) {
        return true;
     }
     FileInputStream fis = new FileInputStream(ftptransferlog);
     BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
     String ref = "";
     boolean beginScript=false;
     while((ref = bis.readLine()) != null) {
        if(debugLevel >= 6000) {
          System.out.println("\nDEGUG:checkLogFileForErrors="+ref);
        }
        int idx = ref.indexOf("pwd"); 
        if(idx != -1) {
          beginScript=true;
        }
        idx = ref.indexOf("226 Transfer Complete");
        if(idx != -1)  { 
           return false;
		}

        idx = ref.indexOf("HSI_ERROR_CODE=0");
        if(idx != -1)  {
           return false;
		}
       
        idx = ref.indexOf("HSI_ERROR_CODE=");
        if(idx != -1) {
           int idx1 = ref.indexOf("=");
           if(idx1 != -1) {
             String temp = ref.substring(idx1+1);
             try {
              Integer ii = new Integer(temp);
              if(debugLevel >= 6000) {
                System.out.println(
					"DEBUG:checkLogFileForErrors:HSI_ERROR_CODE="+ii);
              }
              if(ii.intValue() != 0) { 
                return true; 
			  }
             }catch(NumberFormatException nfe) { }
           }
        }
     }
     //if(beginScript) {
       if(debugLevel >= 6000) {
         System.out.println("\nDEBUG:checkLogFileForErrors:Script is hanging");
       }
       //may be script is hanging
       return true;
     //}
   }catch(Exception e) { }
   return false;
}


private void retryRequests(FileObj fObj, Object status, 
		ExecScript p, String requestType) throws Exception {

  fObj.setExplanation("Will retry again"); 
  fObj.incrementNumRetry();
  fObj.setTimeStamp(new GregorianCalendar());
  fObj.setRetryTime(MSSWaitTime*2);
  if(fObj.getNumRetry() < getMSSMaxRetrial()) {
     //kills the previous process,
     //starts a new process, and adds in the appropriate queue
     fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED);
     p.getProcess().destroy();
     Object[] param = new Object[2]; 
     param[0] = "SOURCE="+fObj; 
     param[1] = "REQUEST-ID="+fObj.getRequestToken(); 
     _theLogger.log(java.util.logging.Level.FINE,
	   "PROCESS_KILLED",(Object[])param);

     Object _parent = fObj.getTaskThread().getParent();
     ThreadCallBack taskThread = 
       new ThreadCallBack((SRM_MSS)_parent);
     taskThread.setCommand(fObj,status);
     fObj.setTaskThread(taskThread);

     if(monitorThread == null) {
       threadDisable = false;
       monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
       monitorThread = new MonitorThreadMain(monitorThreadPool,this);
       monitorThread.start();
    }

    param = new Object[2]; 
    param[0] = "SOURCE="+fObj; 
    param[1] = "REQUEST-ID="+fObj.getRequestToken(); 
    _theLogger.log(java.util.logging.Level.FINE,
	   "GOING_TO_RETRY_"+requestType,(Object[])param);
                 
    if(requestType.equals("get")) { 
      retrieveQueue.add(fObj);
    }
    else if(requestType.equals("put")) { 
      archiveQueue.add(fObj);
    }
    else if(requestType.equals("others")) { 
      othersQueue.add(fObj);
    }
  }
  else {
     //kills the process, and set the status to process killed
     System.out.println(">>>process is killed for requestid="+
		fObj.getRequestToken());
     p.getProcess().destroy();
     Object[] param = new Object[2]; 
     param[0] = "SOURCE="+fObj; 
     param[1] = "REQUEST-ID="+fObj.getRequestToken(); 
     _theLogger.log(java.util.logging.Level.FINE,
	   "PROCESS_KILLED",(Object[])param);
     fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_PROCESS_KILLED);
  }
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object checkStatus (String requestToken) throws Exception {

   Object[] param = new Object[1];
   param[0] = "REQUEST-TOKEN="+requestToken;
   _theLogger.log(java.util.logging.Level.FINE,
	  "CHECK STATUS is called", (Object[]) param);

   Object statusObj = statusMap.get(requestToken);
   if(statusObj != null) {
       if(statusObj instanceof SRM_MSSFILE_STATUS) {
         SRM_MSSFILE_STATUS fileStatus = (SRM_MSSFILE_STATUS) statusObj; 
         FileObj fObj = fileStatus.getFileObj();
         MSS_MESSAGE pftpmssg = fObj.getPFTPStatus();  
         fileStatus.setStatus(pftpmssg);  
         System.out.println(">>>CheckStatus("+requestToken+")="+pftpmssg);
         if(accessType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI && 
			pftpmssg == MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
           ExecScript p = fileStatus.getCurrentProcess();
           //System.out.println(">>>CheckStatus("+requestToken+")="+p);
           if(p != null) {
           long startTimeStamp = p.getStartTimeStamp();
           long currentTimeStamp = System.currentTimeMillis();
           //System.out.println(">>>CheckStatus("+requestToken+")="+currentTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+startTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+processTimeOutAllowed);
           if(currentTimeStamp > startTimeStamp+processTimeOutAllowed) {
             System.out.println(">>>CheckStatus("+requestToken+")=processtimedout");
             String logFile = p.getLogFile();
             //if logfile does not exists, it is an error,
             //process might be hanging, if logfile exists and shows
             //HSI error, then the procees might be hanging too
             boolean b  = checkLogFileForErrors(logFile); 
             if(debugLevel >= 6000) {
               System.out.println(
		"\nDEBUG:checkStatus.checkLogFileForErrors.returnValue="+b);
             }
             if(b) {
               String requestType = getRequestType(requestToken);
               p.setStartTimeStamp(System.currentTimeMillis());
               //System.out.println(">>>BEFORE RETRY(1)");
               retryRequests(fObj,fileStatus,p,requestType);
               pftpmssg = fObj.getPFTPStatus();  
               //System.out.println(">>>AFTER RETRY(1)");
               fileStatus.setStatus(pftpmssg);  
               System.out.println(">>>CheckStatus("+requestToken+")="+
					pftpmssg);
               param = new Object[2];
               param[0] = "REQUEST-TOKEN="+requestToken;
               param[1] = "STATUS="+pftpmssg;
               _theLogger.log(java.util.logging.Level.FINE,
	            "Process is killed", (Object[]) param);
             }
           }
          }
         }
         if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {   
            boolean b = fileStatus.getAlreadyReported();
            if(!b) {
              int count = fileStatus.getAlreadyReportedCount();
              if(count < 20) {
                fileStatus.increaseAlreadyReportedCount();
              } 
              else {
               fileStatus.setAlreadyReported(true);
               statusMap.put(requestToken, 
				MSS_MESSAGE.SRM_MSS_REQUEST_DONE);
              }
            }
         }
       }
       else if(statusObj instanceof SRM_PATH) { 
         SRM_PATH fileStatus = (SRM_PATH) statusObj; 
         FileObj fObj = fileStatus.getFileObj();
         MSS_MESSAGE pftpmssg = null;
         if(fObj != null) {
           pftpmssg = fObj.getPFTPStatus();  
           fileStatus.setStatus(pftpmssg);  
         }
         else {
            pftpmssg = fileStatus.getStatus();
            System.out.println("Status is not take from fileobj, " +
				"since it is null");
         }
         System.out.println(">>>CheckStatus("+requestToken+")="+pftpmssg);
         if(accessType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI && 
			pftpmssg == MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
           ExecScript p = fileStatus.getCurrentProcess();
           if(p != null) {
           long startTimeStamp = p.getStartTimeStamp();
           long currentTimeStamp = System.currentTimeMillis();
           //System.out.println(">>>CheckStatus("+requestToken+")="+currentTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+startTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+processTimeOutAllowed);
           if(currentTimeStamp > startTimeStamp+processTimeOutAllowed) {
             System.out.println(">>>CheckStatus("+requestToken+")=processtimedout");
             String logFile = p.getLogFile();
             //if logfile does not exists, it is an error,
             //process might be hanging, if logfile exists and shows
             //HSI error, then the procees might be hanging too
             boolean b  = checkLogFileForErrors(logFile); 
             if(debugLevel >= 6000) {
               System.out.println(
		"\nDEBUG:checkStatus.checkLogFileForErrors.returnValue="+b);
             }
             if(b) {
               p.setStartTimeStamp(System.currentTimeMillis());
               String requestType = getRequestType(requestToken);
               //System.out.println(">>>BEFORE RETRY(2)");
               retryRequests(fObj,fileStatus,p,requestType);
               pftpmssg = fObj.getPFTPStatus();  
               //System.out.println(">>>AFTER RETRY(2)");
               fileStatus.setStatus(pftpmssg);  
               //System.out.println(
			//">>>CheckStatus("+requestToken+")="+pftpmssg);
               param = new Object[2];
               param[0] = "REQUEST-TOKEN="+requestToken;
               param[1] = "STATUS="+pftpmssg;
               _theLogger.log(java.util.logging.Level.FINE,
	            "Process is killed", (Object[]) param);
             }
           }
          }
         }
         if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {   
            util.printMessage("\n***************************",logger,debugLevel);
            util.printMessage("Path=" + fileStatus.getDir(),logger,debugLevel);
            util.printMessage("isDir=" + fileStatus.isDir(),logger,debugLevel);
            util.printMessage("Status=" + fileStatus.getStatus(),logger,debugLevel);
            util.printMessage("***************************\n",logger,debugLevel);
            boolean b = fileStatus.getAlreadyReported();
            if(!b) {
              int count = fileStatus.getAlreadyReportedCount();
              if(count < 20) {
                fileStatus.increaseAlreadyReportedCount();
              } 
              else {
               fileStatus.setAlreadyReported(true);
               statusMap.put(requestToken, 
				MSS_MESSAGE.SRM_MSS_REQUEST_DONE);
              }
            }
         }
       }
       else if(statusObj instanceof SRM_STATUS) { 
         SRM_STATUS mkDirStatus = (SRM_STATUS) statusObj;
         FileObj fObj = mkDirStatus.getFileObj();
         MSS_MESSAGE pftpmssg = fObj.getPFTPStatus();  
         mkDirStatus.setStatus(pftpmssg);  
         System.out.println(">>>CheckStatus("+requestToken+")="+pftpmssg);
         if(accessType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI && 
			pftpmssg == MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
           ExecScript p = mkDirStatus.getCurrentProcess();
           if(p != null) {
           long startTimeStamp = p.getStartTimeStamp();
           long currentTimeStamp = System.currentTimeMillis();
           //System.out.println(">>>CheckStatus("+requestToken+")="+currentTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+startTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+processTimeOutAllowed);
           if(currentTimeStamp > startTimeStamp+processTimeOutAllowed) {
             //if logfile does not exists, it is an error,
             //process might be hanging, if logfile exists and shows
             //HSI error, then the procees might be hanging too
             System.out.println(">>>CheckStatus("+requestToken+")=processtimedout");
             String logFile = p.getLogFile();
             boolean b  = checkLogFileForErrors(logFile); 
             if(debugLevel >= 6000) {
               System.out.println(
		  "\nDEBUG:checkStatus.checkLogFileForErrors.returnValue="+b);
             }
             if(b) {
               p.setStartTimeStamp(System.currentTimeMillis());
               String requestType = getRequestType(requestToken);
               //System.out.println(">>>BEFORE RETRY(3)");
               retryRequests(fObj,mkDirStatus,p,requestType);
               pftpmssg = fObj.getPFTPStatus();  
               //System.out.println(">>>AFTER RETRY(3)");
               mkDirStatus.setStatus(pftpmssg);  
               //System.out.println(
			//">>>CheckStatus("+requestToken+")="+pftpmssg);
               param = new Object[2];
               param[0] = "REQUEST-TOKEN="+requestToken;
               param[1] = "STATUS="+pftpmssg;
               _theLogger.log(java.util.logging.Level.FINE,
	            "Process is killed", (Object[]) param);
             }
           }
          }
         } 
         if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
            boolean b = mkDirStatus.getAlreadyReported();
            if(!b) {
              int count = mkDirStatus.getAlreadyReportedCount();
              if(count < 20) {
                mkDirStatus.increaseAlreadyReportedCount();
              } 
              else {
               mkDirStatus.setAlreadyReported(true);
               statusMap.put(requestToken, 
				MSS_MESSAGE.SRM_MSS_REQUEST_DONE);
              }
            }
         }
       }
       else if(statusObj instanceof SRM_FILE) { 
         SRM_FILE status = (SRM_FILE) statusObj;
         FileObj fObj = status.getFileObj();
         MSS_MESSAGE pftpmssg = fObj.getPFTPStatus();  
         status.setStatus(pftpmssg);  
         System.out.println(">>>CheckStatus("+requestToken+")="+pftpmssg);
         if(accessType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI && 
			pftpmssg == MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
           ExecScript p = status.getCurrentProcess();
           //System.out.println(">>>CheckStatus("+requestToken+")="+p);
           if(p != null) {
           long startTimeStamp = p.getStartTimeStamp();
           long currentTimeStamp = System.currentTimeMillis();
           //System.out.println(">>>CheckStatus("+requestToken+")="+
		//currentTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+
		//startTimeStamp);
           //System.out.println(">>>CheckStatus("+requestToken+")="+
		//processTimeOutAllowed);
           if(currentTimeStamp > startTimeStamp+processTimeOutAllowed) {
             System.out.println(">>>CheckStatus("+requestToken+")=processtimedout");
             String logFile = p.getLogFile();
             boolean b  = checkLogFileForErrors(logFile); 
             if(debugLevel >= 6000) {
	        System.out.println
		  ("\nDEBUG:checkStatus.checkLogFileForErrors.returnValue="+b);
             }
             if(b) {
               p.setStartTimeStamp(System.currentTimeMillis());
               String requestType = getRequestType(requestToken);
               //System.out.println(">>>BEFORE RETRY(4)");
               retryRequests(fObj,status,p,requestType);
               pftpmssg = fObj.getPFTPStatus();  
               //System.out.println(">>>AFTER RETRY(4)");
               status.setStatus(pftpmssg);  
               //System.out.println(">>>CheckStatus("+
				//requestToken+")="+pftpmssg);
               param = new Object[2];
               param[0] = "REQUEST-TOKEN="+requestToken;
               param[1] = "STATUS="+pftpmssg;
               _theLogger.log(java.util.logging.Level.FINE,
	            "Process is killed", (Object[]) param);
             }
           }
          }
         }
         if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
            boolean b = status.getAlreadyReported();
            if(!b) {
              int count = status.getAlreadyReportedCount();
              if(count < 20) {
                status.increaseAlreadyReportedCount();
              } 
              else {
               status.setAlreadyReported(true);
               statusMap.put(requestToken, 
				MSS_MESSAGE.SRM_MSS_REQUEST_DONE);
              }
            }
         }
       }
       else if(statusObj instanceof MSS_MESSAGE) { 
         System.out.println(">>CheckStatus reported already for this " +
			"RequestToken " + requestToken);
       }
       return statusObj;
   }
   else {
     throw new Exception("RequestToken not found " + requestToken);
   }
}

protected boolean abortRequest (String rid) throws Exception
{
   _completed = false;
   _errorMessage ="";

   Object[] param = new Object[1];
   param[0] = "RID="+rid;
   _theLogger.log(java.util.logging.Level.FINE,"ABORT_REQUEST_COMES", 
		(Object[]) param);

   Object statusObj = statusMap.get(rid);
   if(statusObj != null) {
    if(statusObj instanceof SRM_MSSFILE_STATUS) {
      SRM_MSSFILE_STATUS fileStatus = (SRM_MSSFILE_STATUS) statusObj; 
      MSS_MESSAGE pftpmssg = fileStatus.getStatus();  

      if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {   
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_FAILED", 
	    	(Object[]) param);
        throw new Exception("Cannot abort completed request " + rid);
      }
      else {
        ExecScript p = fileStatus.getCurrentProcess ();
        if(p != null) {
          //destroy the current process
          p.destroyCurrentProcess();
        }
        fileStatus.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fileStatus.setExplanation("Transfer aborted");
        FileObj fObj = fileStatus.getFileObj();
        if(fObj != null) {
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          fObj.setExplanation("Transfer aborted"); 
        }
        mssPending --;
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_DONE", 
	    	(Object[]) param);
        //remove from the queue also,
        //check the read queue and write queue.
        boolean ok = removeFromRelevantQueue(retrieveQueue,rid);
        if(!ok) {
          ok = removeFromRelevantQueue(archiveQueue,rid);
          if(ok) {
            param = new Object[1];
            param[0] = "RID="+rid;
            _theLogger.log(java.util.logging.Level.FINE,
				"REMOVED_FROM_WRITE_QUEUE", (Object[]) param);
          }
          else {
            param = new Object[1];
            param[0] = "RID="+rid;
            _theLogger.log(java.util.logging.Level.FINE,
				"COULD_NOT_REMOVE_FROM_READ_OR_WRITE_QUEUE", (Object[]) param);
          }
        }
        else {
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
				"REMOVED_FROM_READ_QUEUE", (Object[]) param);
        }
      }
    }
    else if(statusObj instanceof SRM_PATH) { 
      SRM_PATH fileStatus = (SRM_PATH) statusObj; 
      MSS_MESSAGE pftpmssg = fileStatus.getStatus();
      if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {   
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_FAILED", 
	    	(Object[]) param);
        throw new Exception("Cannot abort completed request " + rid);
      }
      else {
        ExecScript p = fileStatus.getCurrentProcess ();
        if(p != null) {
          //destroy the current process
          p.destroyCurrentProcess();
        }
        mssPending --;
        fileStatus.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fileStatus.setExplanation("Transfer aborted");
        FileObj fObj = fileStatus.getFileObj();
        if(fObj != null) {
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          fObj.setExplanation("Transfer aborted");
        }
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_DONE", 
	    	(Object[]) param);
        //remove from the queue also,
        //check the read queue and write queue.
        boolean ok = removeFromRelevantQueue(othersQueue,rid);
        if(ok) { 
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"REMOVED_FROM_OTHERS_QUEUE", (Object[]) param);
        }
        else {
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"COULD_NOT_REMOVE_FROM_OTHERS_QUEUE", (Object[]) param);
        }
      }
    }
    else if(statusObj instanceof SRM_STATUS) { 
      SRM_STATUS fileStatus = (SRM_STATUS) statusObj;
      MSS_MESSAGE pftpmssg = fileStatus.getStatus();
      if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_FAILED", 
	    	(Object[]) param);
        throw new Exception("Cannot abort completed request " + rid);
      }
      else {
        ExecScript p = fileStatus.getCurrentProcess ();
        if(p != null) { 
          //destroy the current process
          p.destroyCurrentProcess();
        }
        mssPending --;
        fileStatus.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fileStatus.setExplanation("Transfer aborted");
        FileObj fObj = fileStatus.getFileObj();
        if(fObj != null) {
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          fObj.setExplanation("Transfer aborted");
        }
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_DONE", 
	    	(Object[]) param);
        //remove from the queue also,
        //check the read queue and write queue.
        boolean ok = removeFromRelevantQueue(othersQueue,rid);
        if(ok) { 
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"REMOVED_FROM_OTHERS_QUEUE", (Object[]) param);
        }
        else {
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"COULD_NOT_REMOVE_FROM_OTHERS_QUEUE", (Object[]) param);
        }
      }
    }
    else if(statusObj instanceof SRM_FILE) { 
      SRM_FILE fileStatus = (SRM_FILE) statusObj;
      MSS_MESSAGE pftpmssg = fileStatus.getStatus();
      if(pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_FAILED", 
	    	(Object[]) param);
        throw new Exception("Cannot abort completed request " + rid);
      }
      else {
        ExecScript p = fileStatus.getCurrentProcess ();
        if(p != null) {
          //destroy the current process
          p.destroyCurrentProcess();
        }
        mssPending --;
        fileStatus.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fileStatus.setExplanation("Transfer aborted");
        FileObj fObj = fileStatus.getFileObj();
        if(fObj != null) {
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          fObj.setExplanation("Transfer aborted");
        }
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_DONE", 
	    	(Object[]) param);
        //remove from the queue also,
        //check the read queue and write queue.
        boolean ok = removeFromRelevantQueue(othersQueue,rid);
        if(ok) { 
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"REMOVED_FROM_OTHERS_QUEUE", (Object[]) param);
        }
        else {
          param = new Object[1];
          param[0] = "RID="+rid;
          _theLogger.log(java.util.logging.Level.FINE,
			"COULD_NOT_REMOVE_FROM_OTHERS_QUEUE", (Object[]) param);
        }
      }
    }
    else if(statusObj instanceof MSS_MESSAGE) { 
        param = new Object[1];
        param[0] = "RID="+rid;
        _theLogger.log(java.util.logging.Level.FINE,"ABORT_FAILED", 
	    	(Object[]) param);
        throw new Exception("Cannot abort completed request " + rid);
    }
   }
   else {
     throw new Exception("Given request id not found " + rid);
   }
   return true;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssGetHomeDir
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_STATUS mssGetHomeDir(String path, 
	SRM_ACCESS_INFO accessInfo, mssIntf mssintf)
  throws Exception {

   _completed = false;
   _errorMessage = "";

   Object[] param = new Object[6];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "MSSPATH="+pftpPath;
   param[3] = "SRMNOCIPHER="+false;
   param[4] = "SRMNONPASSIVELISTING="+false;
   param[5] = "PATH="+path;

   _theLogger.log(java.util.logging.Level.FINE,"GETHOMEDIR_REQUEST_COMES", 
		(Object[]) param);


   String source ="";
   SRM_STATUS status = new SRM_STATUS();

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[3];
     param[0] = 
		"EXPLANATION=Others queue is full, please try this request later";
     param[1] = "size = " + othersQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[2] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
			("Others queue is full, please try this request later");
   }
   
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("gethomedir");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     status.setRequestToken(requestId);
	 requestInfo.put("gethomedir", status);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,status);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    status.setRequestToken(requestId);
    requestInfo.put("gethomedir", status);
    statusMap.put(requestId,status);
   }


   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }
    
   FileObj fObj = new FileObj(accessType, accessInfo,  
							  mssintf, requestId, "gethomedir", 
							  status);

   fObj.setSRMNoCipher(false);
   fObj.setSRMNonPassiveListing(false);
   fObj.setFilePath(path);
   status.setFileObj(fObj);
   
  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }

  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,status); 
  fObj.setTaskThread(taskThread);
  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */

  param = new Object[1];
  param[0] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
		"GETHOMEDIR_REQUEST_QUEUED", (Object[]) param);

  return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssMakeDirectory
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_STATUS mssMakeDirectory
	     (SRM_PATH dirs,
          SRM_ACCESS_INFO accessInfo,
	      boolean srmnocipher, 
	      boolean srmnonpassivelisting, mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   Object[] param = new Object[5];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "MSSPATH="+pftpPath;
   param[2] = "DIRS="+dirs.getDir();
   param[3] = "SRMNOCIPHER="+srmnocipher;
   param[4] = "SRMNONPASSIVELISTING="+srmnonpassivelisting;

   _theLogger.log(java.util.logging.Level.FINE,"MKDIR_REQUEST_COMES", 
		(Object[]) param);


   String source ="";
   SRM_STATUS status = new SRM_STATUS();

   if(dirs.getDir() == null || dirs.getDir().equals("")) {
      param = new Object[2];
      param[0] = "DIRS="+dirs.getDir();
      param[1] = "EXPLANATION=Input dir path is null";
      _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
      throw new SRM_MSS_Exception("Input dir path is null");
   }
   else {
      source = dirs.getDir();
      status.setSourcePath(source);
   }

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[4];
     param[0] = "DIRS="+dirs.getDir();
     param[1] = 
		"EXPLANATION=Others queue is full, please try this request later";
     param[2] = "size = " + othersQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[3] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
			("Others queue is full, please try this request later");
   }
   
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("mkdir");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     status.setRequestToken(requestId);
	 requestInfo.put(source+"-mkdir", status);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,status);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    status.setRequestToken(requestId);
    requestInfo.put(source+"-mkdir", status);
    statusMap.put(requestId,status);
   }


   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }
    
   FileObj fObj = new FileObj(accessType, accessInfo,  
							  mssintf, requestId, "mkdir", 
							  status);

   fObj.setDirs(dirs);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   status.setFileObj(fObj);
   
  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }

  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,status); 
  fObj.setTaskThread(taskThread);
  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */

  param = new Object[2];
  param[0] = "DIRS="+dirs.getDir();
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
		"MKDIR_REQUEST_QUEUED", (Object[]) param);

  return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmPing
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 
protected SRM_PING_STATUS srmPing() throws SRM_MSS_Exception,Exception {
  return srmPingStatus; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseLocalSourceFileForPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected static String parseLocalSourceFileForPath (String str)
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
   return str;
  }
 else {
   throw new Exception(
		"SRM-CLIENT: parseLocalSourceFileForPath:SURL not in correct format "+ 
		str);
 }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//mssFilePut
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_MSSFILE_STATUS mssFilePut
	     ( String source, 
          String target, long fileSize,
          SRM_OVERWRITE_MODE overwritemode,
          SRM_ACCESS_INFO accessInfo,
	  boolean srmnocipher, boolean srmnonpassivelisting,
	  mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   source = source.trim();

   Object[] param = new Object[8];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "MSSPATH="+pftpPath;
   param[2] = "SOURCE="+source;
   param[3] = "TARGET="+target;
   param[4] = "SIZE="+fileSize;
   param[5] = "OVERWRITE="+overwritemode;
   param[6] = "SRMNOCIPHER="+srmnocipher;
   param[7] = "SRMNONPASSIVELISTING="+srmnonpassivelisting;

   _theLogger.log(java.util.logging.Level.FINE,"PUT_REQUEST_COMES", 
		(Object[]) param);

   if(archiveQueue.getSize() >= maximumQueueSize) {
      param = new Object[4];
      param[0] = "SOURCE="+source;
      param[1] = 
		"EXPLANATION=Archive queue is full, please try this request later."; 
      param[2] = "size = " + archiveQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
      param[3] = "contents = "+archiveQueue;
	  _theLogger.log(java.util.logging.Level.FINE,
			"TRANSFER_FAILED", (Object[]) param);
      throw new SRM_MSS_Exception
        ("Archive queue is full, please try this request later.");
   }

   SRM_MSSFILE_STATUS srmMSSFileStatus = new SRM_MSSFILE_STATUS();
   srmMSSFileStatus.setLocalFileName(source);
   srmMSSFileStatus.setRemoteFileName(target);
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("mssput");

   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     srmMSSFileStatus.setRequestToken(requestId);
	 requestInfo.put(source+"-put", srmMSSFileStatus);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,srmMSSFileStatus);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    Object statusObj = requestInfo.get(source+"-put");
    srmMSSFileStatus.setRequestToken(requestId);
    requestInfo.put(source+"-put", srmMSSFileStatus);
    statusMap.put(requestId,srmMSSFileStatus);
   }


   try {

      String temp = parseLocalSourceFileForPath(source);
      File f  = new File(temp);
      if(!f.exists()) {
        util.printMessage("\nLocal file did not exists.");
        param = new Object[3];
        param[0] = "SOURCE="+source;
        param[1] = "EXPLANATION=Local file did not exists";
        param[2] = "REQUEST-ID="+requestId;
        _theLogger.log(java.util.logging.Level.FINE,"TRANSFER_FAILED", 
				(Object[]) param);

        srmMSSFileStatus.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_LOCAL_PATH);
        srmMSSFileStatus.setExplanation("Local file did not exists.");
        return srmMSSFileStatus;
      }
      else {
        if(fileSize == 0) { 
          if(debugLevel >= 10) {
            util.printMessage("\nClient did not give file Size " + source);
          }
          fileSize = f.length();
        }
        else if(fileSize != f.length()) {
          util.printMessage("\nUnable to verify local file size");
          util.printMessage("File Size given : " + fileSize);
          util.printMessage("File Size actual : " + f.length());
          param = new Object[3];
          param[0] = "SOURCE="+source;
          param[1]="EXPLANATION=Unable to verify local file size";
          param[2] = "REQUEST-ID="+requestId;
          _theLogger.log(java.util.logging.Level.FINE,"TRANSFER_FAILED", 
				(Object[]) param);
          srmMSSFileStatus.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_LOCAL_PATH);
          srmMSSFileStatus.setExplanation("Unable to verify local file size");
          return srmMSSFileStatus;
        }
      }
   } catch(Exception e) {
      util.printMessage("\nException : "+ e.getMessage());
      srmMSSFileStatus.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_LOCAL_PATH);
      srmMSSFileStatus.setExplanation("Exception : " + e.getMessage());
      return srmMSSFileStatus;
   }

   
   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }
    
   FileObj fObj = new FileObj(accessType, accessInfo, 
									mssintf, requestId, 
						            "put", srmMSSFileStatus);
   fObj.setSource(source);
   fObj.setTarget(target);
   fObj.setFileSize(fileSize);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   fObj.setOverWriteMode(overwritemode);
   srmMSSFileStatus.setFileObj(fObj);


  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }

  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,srmMSSFileStatus); 
  fObj.setTaskThread(taskThread);
    //fObj.setStatus(srmMSSFileStatus.getStatus());
  archiveQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> ArchiveQueue items");
    printQueue(archiveQueue);
  }
  */

  param = new Object[2];
  param[0] = "SOURCE="+source;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log
	(java.util.logging.Level.FINE,"PUT_REQUEST_QUEUED", (Object[]) param);

  return srmMSSFileStatus;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//mssFileGet
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_MSSFILE_STATUS mssFileGet 
	     ( String source, 
          String target, long fileSize,
          SRM_OVERWRITE_MODE overwritemode,
          SRM_ACCESS_INFO accessInfo,
	      boolean srmnocipher, boolean srmnonpassivelisting,
	      mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";
   _errorOccured=false;

   source = source.trim();

   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }

   Object[] param = new Object[8];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "MSSPATH="+pftpPath;
   param[2] = "SOURCE="+source;
   param[3] = "TARGET="+target;
   param[4] = "SIZE="+fileSize;
   param[5] = "OVERWRITE="+overwritemode;
   param[6] = "SRMNOCIPHER="+srmnocipher;
   param[7] = "SRMNONPASSIVELISTING="+srmnonpassivelisting;

   _theLogger.log(java.util.logging.Level.FINE,"GET_REQUEST_COMES", 
		(Object[]) param);

   if(retrieveQueue.getSize() >= maximumQueueSize) {
     param = new Object[4];
     param[0] = "SOURCE="+source;
     param[1] = 
		"EXPLANATION=Retrieve queue is full, please try this request later";
     param[2] = "size = " + retrieveQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[3] = "contents = "+retrieveQueue;
     _theLogger.log(java.util.logging.Level.FINE,"TRANSFER_FAILED", 
	    (Object[]) param);
     throw new SRM_MSS_Exception
       ("Retrieve queue is full, please try this request later.");
   }
    
   SRM_MSSFILE_STATUS srmMSSFileStatus = new SRM_MSSFILE_STATUS();
   srmMSSFileStatus.setLocalFileName(target);
   srmMSSFileStatus.setRemoteFileName(source);
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("mssget");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     srmMSSFileStatus.setRequestToken(requestId);
	 requestInfo.put(source+"-get", srmMSSFileStatus);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,srmMSSFileStatus);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    srmMSSFileStatus.setRequestToken(requestId);
    requestInfo.put(source+"-get", srmMSSFileStatus);
	statusMap.put(requestId,srmMSSFileStatus);
    }


   if(overwritemode == SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_NO) {
     try {
       File f = new File(parseLocalSourceFileForPath(target));
       if(f.exists()) {
         param = new Object[3];
         param[0] = "SOURCE="+source;
         param[1] = "EXPLANATION=Local file already exists, cannot overwrite";
         param[2] = "REQUEST-ID="+requestId;
         _theLogger.log(java.util.logging.Level.FINE,"TRANSFER_FAILED", 
		    (Object[]) param);
         srmMSSFileStatus.setSize(fileSize);
         srmMSSFileStatus.setTransferRate(0.0);
         srmMSSFileStatus.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
         srmMSSFileStatus.setExplanation
		   ("Local file already exists, cannot overwrite.");
         return srmMSSFileStatus;
       }
     }catch(Exception e) {}
   }
   else { 
     //delete the local file if  it already exists.
     File f = new File(target);
     if(f.exists() && (f.length() > 0)) {
          f.delete();
     }
   }


   FileObj fObj = new FileObj(accessType,accessInfo,
								mssintf,requestId, 
								"get",srmMSSFileStatus);
   fObj.setSource(target);
   fObj.setTarget(source);
   fObj.setFileSize(fileSize);
   fObj.setOverWriteMode(overwritemode);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   srmMSSFileStatus.setFileObj(fObj);

   if(enableSearchTapeId) {

     fObj.setHSIPath(hsiPath);
     if(debugLevel >= 1000) { 
       util.printMessage("\n\n>>>>>",logger);
       util.printMessage("Before finding tapeId for file " + fObj, logger);
       util.printMessage(">>>>>\n\n",logger);
     }
     ThreadCallBack findTapeThread = new ThreadCallBack(this);
     findTapeThread.setCommand(fObj,srmMSSFileStatus);
     //findTapeThread.setPriority(1);
     findTapeThread.start();
   }


  /*
  if(debugLevel >= 4000) {
    System.out.println(">>>Is MonitorThread null="+monitorThread);
  }    
  */
  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    /*
    if(debugLevel >= 4000) {
      System.out.println(">>>MonitorThread started");
    }
    */
    monitorThread.start();
  }
  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,srmMSSFileStatus); 
  if(enableSearchTapeId) { 
    taskThread.setPriority(10);
  }
  fObj.setTaskThread(taskThread);
     //fObj.setStatus(srmMSSFileStatus.getStatus());
  retrieveQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> RetrieveQueue items");
    printQueue(retrieveQueue);
  }
  */

  param = new Object[2];
  param[0] = "SOURCE="+source;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,"GET_REQUEST_QUEUED", 
	(Object[]) param);

  return srmMSSFileStatus;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmCopy
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_STATUS srmCopy (String sourcePath, 
		String targetPath, SRM_OVERWRITE_MODE overwritemode,
        boolean recursive, SRM_ACCESS_INFO accessInfo, 
        boolean srmnocipher, boolean srmnonpassivelisting,
		mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   sourcePath = sourcePath.trim(); 
   targetPath = targetPath.trim();

   Object[] param = new Object[4];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "SRMPATH="+pftpPath;
   param[2] = "SOURCE="+sourcePath;
   param[3] = "TARGET="+targetPath;

   _theLogger.log
		(java.util.logging.Level.FINE, "COPY_REQUEST_COMES", 
		 (Object[]) param);

   if(!enableSearchTapeId) {
       param = new Object[2];
       param[0] = "SOURCE="+sourcePath;
       param[1] = 
		"EXPLANATION=Copy function is allowed only with enableSearchTapeId option";
       _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
       throw new SRM_MSS_Exception
			("Copy is allowed only with enableSearchTapeId option");
   }

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[4];
     param[0] = "SOURCE="+sourcePath;
     param[1] = "EXPLANATION=SRM MSS Queue is busy";
     param[2] = "size = " + othersQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[3] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
		("Others queue is busy, please try this request later");
   }

   SRM_STATUS status = new SRM_STATUS();
   status.setSourcePath(sourcePath);
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("msscopy");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     status.setRequestToken(requestId);
     requestInfo.put(sourcePath+"-copy", status);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId, status);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    status.setRequestToken(requestId);
    requestInfo.put(sourcePath+"-copy", status);
    statusMap.put(requestId, status);
   }


   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }

   FileObj fObj = new FileObj(accessType,accessInfo,mssintf,
								requestId, "copy",status);

   fObj.setSource(sourcePath);
   fObj.setTarget(targetPath);
   fObj.setRecursive(recursive);
   fObj.setOverWriteMode(overwritemode);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   status.setFileObj(fObj);

  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }

  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,status); 
  fObj.setTaskThread(taskThread);
   //fObj.setStatus(status.getStatus());
  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */

  param = new Object[2];
  param[0] = "SOURCE="+sourcePath;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
	"COPY_REQUEST_QUEUED", (Object[]) param);

  return status;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmDelete
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_STATUS srmDelete 
		(String dirPath,
         SRM_ACCESS_INFO accessInfo,
         boolean isDir, boolean recursive, 
		 boolean srmnocipher,
		 boolean srmnonpassivelisting,
		 mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   dirPath = dirPath.trim(); 

   Object[] param = new Object[7];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "SRMPATH="+pftpPath;
   param[2] = "PATH="+dirPath;
   param[3] = "RECURSIVE="+recursive;
   param[4] = "ISDIR="+isDir;
   param[5] = "SRMNOCIPHER="+srmnocipher;
   param[6] = "SRMNONPASSIVELISTING="+srmnonpassivelisting;

   _theLogger.log
		(java.util.logging.Level.FINE, "DELETE_REQUEST_COMES", 
		 (Object[]) param);

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[4];
     param[0] = "SOURCE="+dirPath;
     param[1] = "EXPLANATION=Others Queue is busy";
     param[2] = "size = " + othersQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[3] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
		("Others queue is busy, please try this request later");
   }

   if(recursive) {
       param = new Object[2];
       param[0] = "SOURCE="+dirPath;
       param[1] = 
		    "EXPLANATION=Recursive is not allowed for rmdir";
       _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
       throw new SRM_MSS_Exception
			("Recursive is not allowed for rmdir");
     /*
     if(!enableSearchTapeId) {
       param = new Object[2];
       param[0] = "SOURCE="+dirPath;
       param[1] = 
		    "EXPLANATION=Recursive is allowed only with enableSearchTapeId option";
       _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
       throw new SRM_MSS_Exception
			("Recursive is allowed only with enableSearchTapeId option");
     }
     */
   }

   SRM_STATUS status = new SRM_STATUS();
   status.setSourcePath(dirPath);
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("mssdelete");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     status.setRequestToken(requestId);
     requestInfo.put(dirPath+"-del", status);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,status);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    status.setRequestToken(requestId);
    requestInfo.put(dirPath+"-del", status);
    statusMap.put(requestId,status);
   }



   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }

   FileObj fObj = new FileObj(accessType, accessInfo, 
							   mssintf, requestId, 
							   "delete", status);
   fObj.setFilePath(dirPath);
   fObj.setIsDir(isDir);
   fObj.setRecursive(recursive);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   status.setFileObj(fObj);

  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }

  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,status); 
  fObj.setTaskThread(taskThread);
    //fObj.setStatus(status.getStatus());
  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */

  param = new Object[2];
  param[0] = "SOURCE="+dirPath;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
	"DELETE_REQUEST_QUEUED", (Object[]) param);

  return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//srmGetFileSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_FILE srmGetFileSize
	     ( String path, 
          SRM_ACCESS_INFO accessInfo,
		  boolean srmnocipher,
		  boolean srmnonpassivelisting,
		  mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   path = path.trim(); 

   Object[] param = new Object[3];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "SRMMSSPATH="+pftpPath;
   param[2] = "PATH="+path;

   _theLogger.log(java.util.logging.Level.FINE,
			"GET_FILE_SIZE_REQUEST_COMES", (Object[]) param);

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[3];
     param[0] = "SOURCE="+path;
     param[1] = "EXPLANATION=Others Queue is busy";
     param[2] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
		("Others queue is busy, please try this request later");
   }

   SRM_FILE srmFile = new SRM_FILE();
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("getsize");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     srmFile.setRequestToken(requestId);
     requestInfo.put(path+"-getsize", srmFile);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,srmFile);
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    srmFile.setRequestToken(requestId);
    requestInfo.put(path+"-getsize", srmFile);
    statusMap.put(requestId,srmFile);
   }


   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }

   FileObj fObj = new FileObj(accessType, accessInfo, 
			                   mssintf, requestId, 
						       "getfilesize", srmFile);
   fObj.setFilePath(path);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   srmFile.setFileObj(fObj);

  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    monitorThread.start();
  }
  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,srmFile); 
  fObj.setTaskThread(taskThread);
    //fObj.setStatus(srmFile.getStatus());

  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */


  param = new Object[2];
  param[0] = "SOURCE="+path;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
	"GET_FILE_SIZE_REQUEST_QUEUED", (Object[]) param);
  return srmFile;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//srmls
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected SRM_PATH srmls
	     ( String path, 
          SRM_ACCESS_INFO accessInfo,
          boolean recursive, boolean srmnocipher,
          boolean srmnonpassivelisting,  
		  mssIntf mssintf) throws Exception 
{
   _completed = false;
   _errorMessage = "";

   System.out.println(">>>Path(1)="+path);
   path = path.trim(); 

   Object[] param = new Object[6];
   param[0] = "UID="+accessInfo.getUserId();
   param[1] = "SRMLSPATH="+pftpPath;
   param[2] = "PATH="+path;
   param[3] = "RECURSIVE="+recursive;
   param[4] = "SRMNOCIPHER="+srmnocipher;
   param[5] = "SRMNONPASSIVELISTING="+srmnonpassivelisting;

   _theLogger.log(java.util.logging.Level.FINE,"LS_REQUEST_COMES", 
			(Object[]) param);

   if(othersQueue.getSize() >= maximumQueueSize) {
     param = new Object[4];
     param[0] = "SOURCE="+path;
     param[1] = "EXPLANATION=SRM MSS Queue is busy";
     param[2] = "size = " + othersQueue.getSize() + " and maximumQueueSize " 
			+ maximumQueueSize;
     param[3] = "contents " + othersQueue;
     _theLogger.log(java.util.logging.Level.FINE,
		"TRANSFER_FAILED", (Object[]) param);
     throw new SRM_MSS_Exception
		("Others queue is full, please try this request later");
   }

   SRM_PATH srmMSSPath = new SRM_PATH();
   Object obj = userInfo.get(accessInfo.getUserId());
   String requestId = generateRequestNumber("srmls");
   if(obj == null) {
     Hashtable requestInfo = new Hashtable();
     srmMSSPath.setRequestToken(requestId);
     requestInfo.put(path+"-ls", srmMSSPath);
     userInfo.put(accessInfo.getUserId(),requestInfo);
     statusMap.put(requestId,srmMSSPath); 
   }
   else {
    Hashtable requestInfo = (Hashtable) obj;
    srmMSSPath.setRequestToken(requestId);
    requestInfo.put(path+"-ls", srmMSSPath);
    statusMap.put(requestId,srmMSSPath); 
   }



   if(enableSearchTapeId) {
     if(!HSIHost.equals("")) {
       MSSHost = HSIHost;  
     }
     if(HSIPort  != 0) {
       MSSPort = HSIPort;
     }
   }

   FileObj fObj = new FileObj(accessType, accessInfo, 
			                   mssintf, requestId, "ls", 
							   srmMSSPath);
   fObj.setFilePath(path);
   fObj.setRecursive(recursive);
   fObj.setSRMNoCipher(srmnocipher);
   fObj.setSRMNonPassiveListing(srmnonpassivelisting);
   srmMSSPath.setFileObj(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>>Is MonitorThread null="+monitorThread);
  } 
  */
  if(monitorThread == null) {
    threadDisable = false;
    monitorThreadPool = new MonitorThreadPool(1,this);
    monitorThread = new MonitorThreadMain(monitorThreadPool,this);
    /*
    if(debugLevel >= 4000) {
      System.out.println(">>>MonitorThread started");
    }
    */
    monitorThread.start();
  }
  ThreadCallBack taskThread = new ThreadCallBack(this);
  taskThread.setCommand(fObj,srmMSSPath); 
  fObj.setTaskThread(taskThread);
    //fObj.setStatus(srmMSSPath.getStatus());

  othersQueue.add(fObj);

  /*
  if(debugLevel >= 4000) {
    System.out.println(">>> OthersQueue items");
    printQueue(othersQueue);
  }
  */

  param = new Object[2];
  param[0] = "SOURCE="+path;
  param[1] = "REQUEST-ID="+requestId;
  _theLogger.log(java.util.logging.Level.FINE,
	"LS_REQUEST_QUEUED", (Object[]) param);
  return srmMSSPath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processTransferAction
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

void processTransferAction(FileObj fObj, Object status) {
  try {

  Object[] param = new Object[1];
  param[0] = "REQUEST-ID="+fObj.getRequestToken();
  _theLogger.log(java.util.logging.Level.FINE,
    "PROCESS_TRANSFER_ACTION",(Object[])param);

  if(!fObj.getHSIPath().equals("")) {
         String temp = fObj.getHSIPath();
         fObj.setHSIPath("");
       MSS_MESSAGE mss_status =
         SRM_MSS_UTIL.findTapeId(temp, MSSHost, MSSPort, 
			  fObj.getTarget(), fObj.getSource(), fObj, 
	          fObj.getAccessType(), fObj.getAccessInfo(), 
		      this, debugLevel,logPath);
       if(debugLevel >= 1000 && enableSearchTapeId) {
         util.printMessage("\n\n>>>>>>>>>", logger);
         util.printMessage("Found Tape ID for File " + fObj,logger);
         util.printMessage("currentTapeId :" + fObj.getTapeId(), logger);
         util.printMessage(">>>>>>>>>\n\n", logger);
       }
       //re-set hsipath
       //don't set fObj.setPFTPStatus for transfer done.
       //transfer done is not set, but other states are set.
       //srmMSSFileStatus.setStatus(mss_status);
  }
  else if(fObj.getType().equalsIgnoreCase("get")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssGet(fObj,(SRM_MSSFILE_STATUS)status);
  }
  else if(fObj.getType().equalsIgnoreCase("put")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssPut(fObj,(SRM_MSSFILE_STATUS)status);
  }
  else if(fObj.getType().equalsIgnoreCase("mkdir")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssMkDir(fObj,(SRM_STATUS)status);
  }
  else if(fObj.getType().equalsIgnoreCase("ls")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssLs(fObj,(SRM_PATH)status);
  }
  else if(fObj.getType().equalsIgnoreCase("getfilesize")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssGetFileSize(fObj,(SRM_FILE)status);
  }
  else if(fObj.getType().equalsIgnoreCase("delete")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssDelete(fObj,(SRM_STATUS)status);
  }
  else if(fObj.getType().equalsIgnoreCase("copy")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssCopy(fObj,(SRM_STATUS)status);
  }
  else if(fObj.getType().equalsIgnoreCase("gethomedir")) {
    mssIntf mssintf = fObj.getMSSIntf(); 
    mssintf.mssGetHomeDir(fObj,(SRM_STATUS)status);
  }
  }catch(Exception e) {
    e.printStackTrace();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isNextTransferAvailable
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

synchronized boolean isNextTransferAvailable() {
  /*
  if(debugLevel >= 4000) {
   try {
    System.out.println("RetrieveQueue.length="+retrieveQueue.getSize2());
    Object[] list = retrieveQueue.getObject();
    System.out.println("RetrieveQueue.length(1)="+list.length);
    System.out.println("ArchiveQueue.length="+archiveQueue.getSize2());
    list = archiveQueue.getObject();
    System.out.println("ArchiveQueue.length(1)="+list.length);
    System.out.println("OthersQueue.length="+othersQueue.getSize2());
    list = othersQueue.getObject();
    System.out.println("OthersQueue.length(1)="+list.length);
   }catch(Exception e) {
     System.out.println("Exception="+e.getMessage());
   }
  }
  */
  if(retrieveQueue.getSize2() > 0 ||
     archiveQueue.getSize2() > 0 ||
     othersQueue.getSize2() > 0 ) {
     return true;
  }
  return false;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setMonitorThreadDisable
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

synchronized void setMonitorThreadDisable(boolean b) {
  if(b) {
   threadDisable = b;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMonitorThreadDisabled
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

synchronized boolean getMonitorThreadDisable() {
  return threadDisable; 
}

synchronized void setMonitorThreadToNull() {
  if(monitorThread != null) {
    monitorThread = null;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getFromQueue
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected synchronized FileObj getFromQueue() throws Exception {
   boolean ok = false;
   FileObj fobj = null;

   while (!ok) {
      Object obj = checkQueueTurn();
      /* 
      if(debugLevel >= 4000) {
        printQueues();
      }
      */
      if(obj == null) { 
		ok = true;
      }
      else {
        fobj = (FileObj) obj;
        if(fobj.getPFTPStatus() != MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
           if(!fobj.getTaskThread().isAlreadyStarted()) {
             ok = true;
           }
        }
      }
   }
   return fobj;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkQueueTurn
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

protected synchronized Object checkQueueTurn () throws Exception {
    int value = checkTurn();
    int qturn = -1;
    switch(value) {
      case 0 : qturn = checkTimeStamp(retrieveQueue, value);
               if(qturn != -1) {
	             nextQueueTurn=1;
                 if(enableSearchTapeId) {
                   int sameTapeIdIndex = 
					   searchTapeId(retrieveQueue, qturn, previousTapeId);
                   if(debugLevel >= 1000) {  
                     util.printMessage("\n\n>>>>>>>>", logger);
                     util.printMessage("(CheckTurn) PreviousTapeId :" + 
						previousTapeId,logger);
                     util.printMessage("TAPEID index " + 
						sameTapeIdIndex, logger);
                     util.printMessage("qturn value " + qturn, logger);
                     util.printMessage(">>>>>>>>>\n\n", logger);
                   }
                   if(sameTapeIdIndex != -1) {
                     return retrieveQueue.remove(sameTapeIdIndex); 
                   }
                   else {
                     return retrieveQueue.remove(qturn); 
                   }
                 }
                 else { 
                   return retrieveQueue.remove(qturn); 
                 }
               }
               else {
                 qturn = checkTimeStamp(archiveQueue, value);
                 if(qturn != -1) {
	               nextQueueTurn=2;
                   return archiveQueue.remove(qturn); 
                 }
                 else { 
                   qturn = checkTimeStamp(othersQueue, value);
                   if(qturn != -1) {
	                 nextQueueTurn=0;
                     return othersQueue.remove(qturn); 
                   }
                   else {
                     return null;
                   }
                 }
               }
      case 1 : qturn = checkTimeStamp(archiveQueue, value);
               if(qturn != -1) {
                 nextQueueTurn=2;
                 return archiveQueue.remove(qturn); 
               }
               else {
                 qturn = checkTimeStamp(othersQueue, value);
                 if(qturn != -1) {
                   nextQueueTurn=0;
                   return othersQueue.remove(qturn); 
                 }
                 else {
                   qturn = checkTimeStamp(retrieveQueue, value);
                   if(qturn != -1) {
                     nextQueueTurn=1;
                     if(enableSearchTapeId) {
                       int sameTapeIdIndex = 
					     searchTapeId(retrieveQueue, qturn, previousTapeId);
                       if(debugLevel >= 1000) {
                         util.printMessage("\n\n>>>>>>>>>", logger);
                         util.printMessage
							("(CheckTurn) PreviousTapeId :" + 
						         previousTapeId,logger);
                         util.printMessage("TAPEID index " + 
							sameTapeIdIndex, logger);
                         util.printMessage("qturn value " + qturn,logger);
                         util.printMessage(">>>>>>>>>\n\n", logger);
                       }
                       if(sameTapeIdIndex != -1) {
                         return retrieveQueue.remove(sameTapeIdIndex); 
                       }
                       else {
                         return retrieveQueue.remove(qturn); 
                       }
                     }
                     else { 
                       return retrieveQueue.remove(qturn); 
                     }
                   }
                   else {
                     return null;
                   }
                 }
               }
      case 2 : qturn = checkTimeStamp(othersQueue, value);
               if(qturn != -1) {
                 nextQueueTurn=0;
                 return othersQueue.remove(qturn); 
               }
               else {
                 qturn = checkTimeStamp(retrieveQueue, value);
                 if(qturn != -1) {
                   nextQueueTurn=1;
                   if(enableSearchTapeId) {
                     int sameTapeIdIndex = 
				        searchTapeId(retrieveQueue, qturn, previousTapeId);
                     if(debugLevel >= 1000)  { 
                       util.printMessage("\n\n>>>>>>>>>", logger);
                       util.printMessage
							("\n(CheckTurn) PreviousTapeId :" + 
						         previousTapeId,logger);
                       util.printMessage("TAPEID index " + 
							sameTapeIdIndex,logger);
                       util.printMessage("qturn value " + qturn,logger);
                       util.printMessage(">>>>>>>>>\n\n", logger);
                     }
                     if(sameTapeIdIndex != -1) {
                        return retrieveQueue.remove(sameTapeIdIndex); 
                     }
                     else {
                       return retrieveQueue.remove(qturn); 
                     }
                   }
                   else { 
                     return retrieveQueue.remove(qturn); 
                   }
                 }
                 else {
                   qturn = checkTimeStamp(archiveQueue, value);
                   if(qturn != -1) {
                     nextQueueTurn=2;
                     return archiveQueue.remove(qturn); 
                   }
                   else {
                     return null;
                   }
                 }
               }
      default : 
                util.printMessage("read queues size " + 
						retrieveQueue.getSize(),logger);
                util.printMessage("write queues size " + 
						archiveQueue.getSize(),logger);
                util.printMessage("other queues size " + 
						othersQueue.getSize(),logger);
                util.printMessage("Unknown QueueTurn " + 
					nextQueueTurn,logger);
               return null; 
    }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// searchTapeId
//    search for tapeId, start from index to queue.length
//    if found return, else search from 0 to < index
//    if found return else return index itself.
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized int searchTapeId(ObjectFIFO queue,
		int index, String previousTapeId) {
  
  if(previousTapeId.equals("")) {
    return -1;
  }

  try {
    Object[] list = queue.getObject();
    for(int i = 0; i < list.length; i++) {
      FileObj fObj = (FileObj) list[i];
      if(fObj != null) {
        String tapeId = fObj.getTapeId();
        if(tapeId.equals(previousTapeId)) { 
           return i;
        }
      }
    }
  }catch(Exception e) {
    e.printStackTrace();
  }
  return -1;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//removeFromReleventQueue
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized boolean removeFromRelevantQueue(ObjectFIFO queue, 
	String rid) throws Exception {
 boolean ok = false; 
 try {
   Object[] list = queue.getObject();
   
   int index = -1;
   for(int i = 0; i < list.length; i++) {
      FileObj fObj = (FileObj) list[i];
      if(fObj != null) {
        if(fObj.getRequestToken().equals(rid)) {
           index = i;
           break;
        }
      }
   }
   //added !queue.isEmpty, because it gets stuck when there is nothing
   if(index != -1 && !queue.isEmpty()) {
     queue.remove(index);
     ok = true;
   }
  }catch(Exception e) {
     e.printStackTrace();
  }
  return ok;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//printQueue
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void printQueue(ObjectFIFO queue) {
  try {
   Object[] list = queue.getObject();
   for(int i = 0; i < list.length; i++) {
      System.out.println(">>>list["+i+"]="+list[i]);
   }
  }catch(Exception e) {
     System.out.println("Exception="+e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//printQueues
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void printQueues() {
  try {
   System.out.println(">>>Printing Queues after removing object ");
   System.out.println(">>>retrievequeue");
   Object[] list = retrieveQueue.getObject();
   for(int i = 0; i < list.length; i++) {
      System.out.println(">>>list["+i+"]="+list[i]);
   }
   System.out.println(">>>archivequeue");
   list = archiveQueue.getObject();
   for(int i = 0; i < list.length; i++) {
      System.out.println(">>>list["+i+"]="+list[i]);
   }
   System.out.println(">>>othersqueue");
   list = othersQueue.getObject();
   for(int i = 0; i < list.length; i++) {
      System.out.println(">>>list["+i+"]="+list[i]);
   }
   System.out.println(">>>Printing Queues done");
  }catch(Exception e) {System.out.println("Exception="+e.getMessage());}
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized int checkTimeStamp(ObjectFIFO queue, int value) {
   try {
    Object[] list = queue.getObject();
    if(debugLevel >= 4000) {
      StringBuffer buf = new StringBuffer();
      for(int i = 0; i < list.length; i++) {
        buf.append(list[i] + " ");
      }
      Object[] param = new Object[1];
      param[0] = "CONTENTS="+buf.toString();
      _theLogger.log(java.util.logging.Level.FINE,
		"CheckTimeStamp("+ value+ " contents)", (Object[]) param);
    }
    for(int i = 0; i < list.length; i++) {
     FileObj fObj = (FileObj) list[i];
      if(fObj != null) {
        if(fObj.getNumRetry() > 0) {
          int timestamp = fObj.getTimeStamp();
          int ctimestamp = fObj.getCurrentTimeStamp();
          if(debugLevel >= 4000) {
            Object[] param = new Object[2];
            param[0] = "ctimestamp-timestamp="+(ctimestamp-timestamp);
            param[1] = "retrytime="+fObj.getRetryTime();
            _theLogger.log(java.util.logging.Level.FINE,
		        "CheckTimeStamp("+ value+")", (Object[]) param);
          }
          if(ctimestamp - timestamp >= fObj.getRetryTime()) {
             return i;
          }
        }else { 
          //because timestamp is only set when it fails during 
          //retry time setup.
		  return i; 
	    }
      }
    }
   }catch(Exception e) {
     e.printStackTrace();
   }
  return -1;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkTurn
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized int checkTurn () {
  if(nextQueueTurn == 0) { 
    if(retrieveQueue.getSize() > 0) {
      return nextQueueTurn;
    }
    else {
      if(archiveQueue.getSize() > 0) {
         return (nextQueueTurn+1);
      }
      else {
        if(othersQueue.getSize() > 0) {
          return (nextQueueTurn+2);
        }
        //atleast one should be greater than 0.
      }
    }
  }
  else if(nextQueueTurn == 1) {
    if(archiveQueue.getSize() > 0) {
     return nextQueueTurn;
    }
    else {
      if(othersQueue.getSize() > 0) {
        return (nextQueueTurn+1);
      }
      else {
        if(retrieveQueue.getSize() > 0) {
          return (nextQueueTurn-1);
        }
      }
    }
  }
  else if(nextQueueTurn == 2) {
    if(othersQueue.getSize() > 0) {
     return nextQueueTurn;
    }
    else {
      if(retrieveQueue.getSize() > 0) {
        return (nextQueueTurn-2);
      } 
      else {
        if(archiveQueue.getSize() > 0) {
          return (nextQueueTurn-1);
        }
      }
    }
  }
  return -1;
}


synchronized Hashtable getQueueStatusMap() {
  return queueStatusMap;
}

synchronized void addToStatusMap(FileObj fObj) {
  queueStatusMap.put(fObj.getRequestToken(), fObj);
}

synchronized void removeFromStatusMap(FileObj fObj) {
  queueStatusMap.remove(fObj.getRequestToken());
}

public Log getLogger() {
  return logger;
}

public java.util.logging.Logger getJLogger() {
  return _theLogger;
}

synchronized void decreaseMSSPending() {
  if(mssPending > 0)
    mssPending--;
}

public synchronized int getMSSPending() {
  return mssPending;
}

private synchronized String generateRequestNumber(String type) {
  String temp = type+"-"+rId;
  rId ++;
  return temp;
}

public static String printDate() {
  Calendar c = new GregorianCalendar();
  StringBuffer buf = new StringBuffer();
  buf.append((c.get(Calendar.MONTH)+1)+"-");
  buf.append(c.get(Calendar.DATE)+"-");
  buf.append(c.get(Calendar.YEAR)+"-");
  buf.append(c.get(Calendar.HOUR_OF_DAY)+"-");
  buf.append(c.get(Calendar.MINUTE)+"-");
  buf.append(c.get(Calendar.SECOND));
  return buf.toString();
}

synchronized void setPreviousTapeId(String ptapeid) {
  previousTapeId = ptapeid;
}

synchronized String getPreviousTapeId() {
  return previousTapeId;
}

public synchronized int getMSSMaxAllowed() {
  return MSSMaxAllowed;
}

public synchronized int getMSSMaxRetrial() {
  return MSSMaxRetrial;
}

public synchronized  int getDebugLevel() {
  return debugLevel;
}

public synchronized void setBufferSize(int bufferSize) {
  this.bufferSize = bufferSize;
}

public synchronized void setParallelism(int parallelism) {
  this.parallelism = parallelism;
}

public synchronized void setDCAU(boolean dcau) {
  this.dcau = dcau;
}

public synchronized int getBufferSize() {
  return bufferSize;
}

public synchronized int getParallelism() {
  return parallelism;
}

public synchronized boolean getDCAU() {
  return dcau;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printMessage -- method implemented for callerIntf
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printMessage(String message) {
  util.printMessage("SRM-MSS-error:\n"+message,logger);
}

public void setErrorOccured(boolean b) {
  _errorOccured = b;
}

public boolean getErrorOccured() {
  return _errorOccured;
}

}
