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

package gov.lbl.srm.transfer.mss.hpss;

import java.io.*;
import java.util.*;
import java.net.*;

import org.globus.util.ConfigUtil;
import org.globus.util.GlobusURL;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;



import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

import gov.lbl.srm.transfer.mss.intf.*;

import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.hpss.gsiftp.MySRMFileTransfer;
import gov.lbl.srm.transfer.mss.hpss.gsiftp.intf.*;
import gov.lbl.srm.transfer.mss.hpss.gsiftp.transfer.globus.*;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSS_HPSS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_MSS_HPSS extends SRM_MSS implements SRM_MSSIntf, mssIntf {

private String pftpPath="";
private String hsiPath="";
private boolean _initialized;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSS_HPSS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_MSS_HPSS () throws Exception { 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// init
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void init(String path) throws SRM_MSS_Exception, Exception {

   util.printMessage("Initializing...", logger,debugLevel);

   Properties sys_config = new Properties();
   String ref;

   BufferedReader in = null;
   FileInputStream file = null;

   if(path.startsWith("http:")) {
      URL iorURL = new URL(path);
      URLConnection conn = iorURL.openConnection();
      in = new BufferedReader( new InputStreamReader(conn.getInputStream()));
    }
    else {
      file = new FileInputStream(path);
      in = new BufferedReader(new InputStreamReader(file));
    }

    while((ref = in.readLine()) != null) {
       if(ref.startsWith("#") || ref.equals(""))
            continue;
       int eqpos = ref.indexOf("=");
       if(eqpos == -1) {
          throw new SRM_MSS_Exception
            ("rc file contents should be in the name=value format");
       }
       else {
         sys_config.put(ref.substring(0,eqpos).toLowerCase(),
                ref.substring(eqpos+1));
       }
   }

   String temp = (String) sys_config.get("pftppath");
   if(temp == null || temp.equals("")) {
     String accessType = (String) sys_config.get("accesstype");
     if(accessType == null) {
        throw new SRM_MSS_Exception("AccessType cannot be null");
     }
     else {
        if(accessType.equalsIgnoreCase("plain")) {
          throw new SRM_MSS_Exception("Please provide PFTP path."); 
        }
     }
   }
   else {
     this.pftpPath = temp;
   }

   temp = (String) sys_config.get("enablesearchtapeid");
   if(temp != null) {
     Boolean b = new Boolean(temp);
     if(b.booleanValue()) {
        temp = (String) sys_config.get("hsipath");
        if(temp == null || temp.equals("")) {
          throw new SRM_MSS_Exception("EnableHSI option requires HSI path."); 
        }
        else {
          this.hsiPath = temp;
        }
     }
   }

   temp = (String) sys_config.get("hsipath");
   if(temp != null) {
     this.hsiPath = temp;
     enableHSI=true;
   }

   super.init(sys_config);

   _initialized = true;

   util.printMessage("Finished Initializing.", logger,debugLevel);

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//checkProxyIsValid 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized GSSCredential createAndCheckProxyIsValid 
	(String proxyFile, boolean showOutput) 
		throws SRM_MSS_Exception,Exception { 

  /*
  String temp = proxyFile;
  int idx = proxyFile.lastIndexOf("/");
  if(idx != -1) {
    temp = temp.substring(idx+1); 
  }
  */

  try {
   //File f = new File(scriptPathDir+"/"+temp);
   File f = new File(proxyFile);
   byte[] data =new byte[(int)f.length()];
   FileInputStream in = new FileInputStream(f);
   //read in the credential data
   in.read(data);
   in.close();
   ExtendedGSSManager manager =
     (ExtendedGSSManager) ExtendedGSSManager.getInstance(); 
   GSSCredential mycred = manager.createCredential(data,
              ExtendedGSSCredential.IMPEXP_OPAQUE,
              GSSCredential.DEFAULT_LIFETIME,
              null,
              GSSCredential.INITIATE_AND_ACCEPT);
   if(showOutput) {
     util.printMessage("\nSubject " + mycred.getName(),logger,debugLevel);
     util.printMessage("timeleft " + mycred.getRemainingLifetime(),
			logger,debugLevel);
   }
   if(mycred.getRemainingLifetime() == 0) {
       throw new SRM_MSS_Exception
                ("Credential expired, please renew your credentials");
   }
   return mycred;
  }catch(Exception e) {
    throw e;
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//checkProxyIsValid 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void checkProxyIsValid 
	(String proxyFile, boolean showOutput) 
		throws SRM_MSS_Exception,Exception { 

  try {
   X509Credential gCreds  = new X509Credential(proxyFile);
   if(showOutput) {
     util.printMessage("\nSubject " + gCreds.getSubject(),logger,debugLevel);
     util.printMessage("timeleft " + gCreds.getTimeLeft(),logger,debugLevel);
   }
   if(gCreds.getTimeLeft() == 0) {
       throw new SRM_MSS_Exception
                ("Credential expired, please renew your credentials");
   }
   GSSCredential credential = new GlobusGSSCredentialImpl
                (gCreds, GSSCredential.INITIATE_AND_ACCEPT);
   if(credential.getRemainingLifetime() == 0) {
       throw new SRM_MSS_Exception
                ("Credential expired, please renew your credentials");
   }
  }catch(Exception e) {
    throw e;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeLogFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void writeLogFile(String logFile, String msg) 
		throws Exception {
  FileOutputStream fos = new FileOutputStream(logFile);
  BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
  out.write(msg);
  out.flush();
  out.close();
  fos.close();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeProxyFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void writeProxyFile(String proxyFile, String passwd) 
  throws Exception {

  /*
  String temp = proxyFile;
  int idx = proxyFile.lastIndexOf("/");
  if(idx != -1) {
    temp = temp.substring(idx+1); 
  }
  FileOutputStream fos = new FileOutputStream(scriptPathDir+"/"+temp);
  */
  FileOutputStream fos = new FileOutputStream(proxyFile);
  BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
  out.write(passwd);
  out.flush();
  out.close();
  fos.close();

  boolean ok = false;
  Thread t = new Thread();
  
  while(!ok) {
    try { 
      //chmod proxy file to 600 
      File f = new File(proxyFile);
      if(f.exists()) {
        Process p0 = Runtime.getRuntime().exec("chmod 600 "+ proxyFile);  
        if(p0.waitFor() == 0) { 
          p0.destroy();
          ok = true;
        }
     }
    }catch(IOException ioe) {
      ok = false;   
      t.sleep(1000);
    }catch(Exception e) {
      ok = true;
      e.printStackTrace();
    }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmPing
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_PING_STATUS srmPing() throws SRM_MSS_Exception,Exception {
  return super.srmPing();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssFileGet
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_MSSFILE_STATUS mssFileGet
         (String source, 
          String target, long fileSize,
          SRM_OVERWRITE_MODE  overwritemode,
          boolean srmnocipher, boolean srmnonpassivelisting,
    	  SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception  {

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     source = "gsiftp://"+MSSHost+":"+MSSPort + source;
     target = "file://"+target;
   }

   SRM_MSSFILE_STATUS status = super.mssFileGet(source, target, fileSize,
                      overwritemode,
	                  accessInfo,srmnocipher,srmnonpassivelisting, this);

   mssPending++;

   util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

   if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue ();

      if(debugLevel >= 1000 && enableSearchTapeId) { 
         util.printMessage("\n\n>>>>>>",logger);
         util.printMessage("File returned from getFromQueue " + fObj, logger);
         util.printMessage(">>>>>>\n\n", logger);
      }

      if(fObj != null) {
      Object[] param = new Object [2];
      param[0] = fObj.toString();
      param[1] = "RID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE, "SERVING_GET_REQUEST",
			(Object[]) param);

      //each file transfer is handled by seperate thread.
      //at any given time only MaxAllowed of tasks can happen.
      queueStatusMap.put(fObj.getRequestToken(),fObj);
      //this variable is never getting set, because, this one always
      //executes first and then only finding tape id is happening.
      //may be I can put some conditions to make find tape id happen first
      // and then execute the file ????
      previousTapeId = fObj.getTapeId();
       
      if(debugLevel >= 1000 && enableSearchTapeId) {
        util.printMessage("\n\n>>>>>> ", logger);
        util.printMessage("Executing File now " + fObj, logger);
        util.printMessage("PreviousTapeId : " + previousTapeId, logger);
        util.printMessage(">>>>>>\n\n", logger);
      }

      ThreadCallBack taskThread = fObj.getTaskThread(); 
      taskThread.start();
     }
   }
   else  { // just added into queue and return
      ; 
   }

   return status; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssGet
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssGet(FileObj fObj, 
	SRM_MSSFILE_STATUS status) throws Exception {

  _completed = false;
  _errorMessage = "";
  String ftptransferlog="";
  String requestToken = status.getRequestToken();

  if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
    status.setExplanation("Transfer aborted");
    fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
    fObj.setExplanation("Transfer aborted");
    return;		
  } 

  if(!logPath.equals("")) {
    ftptransferlog = logPath+"/"+"srm_mss." + requestToken;
  }
  else {
    ftptransferlog = "srm_mss." + requestToken;
  }

  if(debugLevel >= 200) {
    util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
  } 

  MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

  long sizeOfFileInMB = 0;
  long counter = 0;
  double transferTime = 0;
  double totalTime = 0;

  SRM_ACCESS_TYPE mssType = fObj.getAccessType();
  SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
  String localFileName = fObj.getSource();
  String remoteFileName = fObj.getTarget();
  long fileSize = fObj.getFileSize();
  String srmmsspath = pftpPath; 
  String srmmsshost = MSSHost;
  int srmmssport = MSSPort;
  boolean srmnocipher = fObj.getSRMNoCipher();
  boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();

   
  try {
    if(debugLevel >= 10) {
      util.printMessage("\nLocal File : " + localFileName,logger,debugLevel);
      util.printMessage("Remote File: " + remoteFileName,logger,debugLevel); 
      util.printMessage("Remote File Size given : " + 
			fileSize,logger,debugLevel);
    }

    //check whether target dir exists.
    //check whether we have permission to create file in target dir. 

    String dirName = localFileName;
    int idx = localFileName.lastIndexOf("/");
    if(idx != -1) {
     dirName = localFileName.substring(0,idx);
    }

    /******************************************************
    String temp = parseLocalSourceFileForPath(dirName);
    File dir = new File(temp);
    if(!dir.exists()) {
      util.printMessage("\nTarget dir does not exist " + dirName);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_LOCAL_PATH);
      fObj.setExplanation("Target dir does not exists " + dirName);
      status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_LOCAL_PATH);
      status.setExplanation("Target dir does not exists " + dirName);
      return;
    }
  
    boolean b = dir.canWrite();

    if(!b) {
      util.printMessage("\nNo write permission in the Target dir. " + 
		dirName);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
      fObj.setExplanation("No write permission in the target dir " + dirName);
      status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
      status.setExplanation("No write permission in the target dir " + dirName);
      return;
    }
    ******************************************************/

    if(debugLevel >= 10) {
      util.printMessage("\nMSSTYPE : " + mssType,logger,debugLevel);
      util.printMessage("\nThread " + Thread.currentThread().getName() +
		" will retrieve file.", logger,debugLevel); 
    }


    String proxyFile = ftptransferlog+".proxy";
    File script=null;
    GSSCredential credential = null;
    boolean gsiTransferOk=false;
    String gsiException="";
    MyISRMFileTransfer tu = null;

    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
      }catch(SRM_MSS_Exception srme) {
        util.printMessage("\nERROR : expired proxy " + proxyFile,
			logger);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy."+srme.getMessage());
        status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        status.setExplanation("Expired proxy."+srme.getMessage());
        return; 
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system."+
			ee.getMessage());
        status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        status.setExplanation("FAILED_TO_WRITE_PROXY to local file system."+
			ee.getMessage());
        System.out.println("Exception="+ee.getMessage());
        return; 
      }

      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
	     "WROTE_PROXY_FILE",(Object[])param);

      try {
        tu = new MySRMFileTransfer(remoteFileName,localFileName);
        tu.setLogger(logger,_theLogger,false,true);
        tu.setTransferMode(SRMTransferMode.GET);
        tu.setParallel(parallelism);
        tu.setCredentials(credential);
        tu.setTransferType(SRMTransferProtocol.GSIFTP);
        tu.setSessionType(1);
        tu.setSessionMode(1);
        tu.setBufferSize(bufferSize);

        if(dcau) {
          tu.setDCAU(dcau);
        }
        util.printMessage("\nRETRIEVE_START", logger,debugLevel);
        tu.start();
        boolean ok = false;
        Thread t = new Thread();
        while(!ok) {
          t.sleep(5000);
          if(tu.transferDone()) {
             ok = true;
             gsiTransferOk=true;
          }
          if(tu.getStatus() != null) {
             ok=true;
             gsiException=tu.getStatus();
             gsiTransferOk=false;
          }
          if(!gsiTransferOk) {
           param = new Object[2]; 
           param[0] = "REQUEST-ID="+fObj.getRequestToken();
           param[1] = "EXCEPTION="+gsiException;
           _theLogger.log(java.util.logging.Level.FINE,
	       "EXCEPTION_GSI",(Object[])param);
          }
        }
      }catch(Exception ee) {
        util.printMessage("Exception ee="+ee.getMessage());
        param = new Object[2]; 
        param[0] = "REQUEST-ID="+fObj.getRequestToken();
        param[1] = "EXCEPTION="+ee.getMessage();
        _theLogger.log(java.util.logging.Level.FINE,
	     "EXCEPTION_GSI",(Object[])param);
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END " + " > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      writer.println("user " + accessInfo.getLogin() + " " + 
		accessInfo.getPasswd());
      writer.println("bin");
      writer.println("pget " + remoteFileName + " " + localFileName);
      writer.println("bye");
      writer.println("END");
      writer.close();

      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n" , logger,debugLevel);
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        util.printMessage("user " + accessInfo.getLogin() + "   " + 
                    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage("pget " + remoteFileName + " "+ 
			localFileName+"\n",logger,debugLevel);
        util.printMessage("bye\n", logger,debugLevel);
        util.printMessage("END\n", logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
	  mypasswd == null || mypasswd.length() == 0) {
        if(debugLevel >= 10) { 
          util.printMessage("\nWARNING: Either userid or password wrong.",
				logger,debugLevel);
        }
        status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setExplanation("Either userid or password wrong.");
        status.setExplanation("Either userid or password wrong.");
        return;
      }

      if(!srmnocipher) {
         //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
         //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
         mylogin = accessInfo.getLogin();
         mypasswd = accessInfo.getPasswd();
      }

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END "+ " > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      if(srmnonpassivelisting) {
         writer.println("passive");
      }
      writer.println("user " + mylogin + " " +  mypasswd);
      writer.println("bin");
      writer.println("pget " + remoteFileName + " " + localFileName);
      writer.println("bye");
      writer.println("END");
      writer.close();

      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
	     "WROTE_SCRIPT_FILE",(Object[])param);

      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n" , logger,debugLevel);
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog,  logger,debugLevel);
        util.printMessage("open " + srmmsshost + " " + srmmssport, logger,debugLevel);
        util.printMessage("passive\n", logger,debugLevel);
        util.printMessage("user " + mylogin + " " + 
                    mypasswd, logger,debugLevel);
        util.printMessage("bin",logger,debugLevel);
        util.printMessage("pget " + remoteFileName + " "+ 
			localFileName,logger,debugLevel);
        util.printMessage("bye",logger,debugLevel);
        util.printMessage("END",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) {
      //writeLogFile(ftptransferlog,"opening logfile");
      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println (hsiPath + " -q \"out " + ftptransferlog + ";" +
            //" pwd; " +
			" get " + localFileName + " : " + remoteFileName +" ; end\"");
      writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog);
      writer.println("echo \"DATE=`date`\" >> " + ftptransferlog);
      writer.println("ls -l " + localFileName + " >> " + ftptransferlog);
      writer.close();

      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage (hsiPath + " -q \"out " + ftptransferlog + ";" +
            //" pwd; " +
			" get " + localFileName + " : " + remoteFileName +" ; end\"",
					logger,debugLevel);
        util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog,
                    logger,debugLevel);
        util.printMessage("echo \"DATE=`date`\" >> " + ftptransferlog,
                    logger,debugLevel);
        util.printMessage("ls -l " + localFileName + " >> " + ftptransferlog, 
				logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
  }
  else {
      util.printMessage("\nHPSS TYPE not implemented : " + mssType, 
		logger,debugLevel);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("Unknown MSS Type");
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("Unknown MSS Type");
      return;
  }

   boolean useEnvp = false;
   long endTime=0;
   long startTime = System.currentTimeMillis(); 

   if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     util.printMessage("\nRETRIEVE_START", logger,debugLevel);
     boolean ok = false;
     Thread t = new Thread();
     while (!ok) { 
      try {
       if(script.exists()) {
         Process p0 = 
	   Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
         if(p0.waitFor() == 0) {
           p0.destroy(); 
           if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
             Object[] param = new Object[1]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             _theLogger.log(java.util.logging.Level.FINE,
	           "NO_NEED_TO_EXECUTE_SCRIPT_FILE_TRANSFER_ABORTED",
				  (Object[])param);
             status.setExplanation("Transfer aborted");
             fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
             fObj.setExplanation("Transfer aborted");
             return;
           }
           else { 
             Object[] param = new Object[1]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             _theLogger.log(java.util.logging.Level.FINE,
	           "GOING_TO_EXECUTE_SCRIPT_FILE",(Object[])param);
             ExecScript process = 
		        new ExecScript(script.getAbsolutePath(), 
	               this.javaLibraryPath, true,this, _theLogger);
             process.setLogFile(ftptransferlog);
             status.setCurrentProcess(process); 
             process.execCommand(useEnvp);
           }
           ok = true;	    
         }
       }
      }catch(IOException ioe) {
         ok = false;
         t.sleep(1000); //wait for a while and execute command again
      }catch(Exception e) {
       ok = true;
       e.printStackTrace();
      }
    }//end while

    endTime = System.currentTimeMillis(); 


     if(!_errorMessage.equals("") || _errorOccured) {
      File ftplog = new File(ftptransferlog);
       util.printMessage("\nProbably some problem in executing script.");
       util.printMessage("error message="+_errorMessage);
       util.printMessage("error occured="+_errorOccured);
       if(!ftplog.exists() || ftplog.length() == 0) {
         util.printMessage("log file is empty and " + ftptransferlog);
       }
       else {
         util.printMessage("log file is " + ftptransferlog);
       }
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       fObj.setExplanation("ERROR while executing script "+script + "  " +
                        _errorMessage + " and erroroccured");
       status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       status.setExplanation("ERROR while executing script "+script + " " +
                        _errorMessage + " and erroroccured");
    }//end if

    try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.");
          util.printMessage("log file is empty " + ftptransferlog);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation("ERROR while executing script.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation("ERROR while executing script.");
        }
    }catch(Exception e) {}
 
    
    if(debugLevel < 3000) { 
     try {
       if(script.exists()) { 
         if(_errorMessage.equals("")) {
           //script.delete();
         }
       }
      }catch(Exception ioe) {
        util.printMessage("Exception " + ioe.getMessage());
      }
    }

    //seems like the script runs successfully.
    //but there may be other problems such as login, file not found etc.
    //need to parse the transfer log to find out the details

    //do you think we need to check file exists or not etc.
    //I am not sure we will think about this later.

    //assume that file  really exists.

    //if file did not exists.

    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
          "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
   }//end if 

   endTime = System.currentTimeMillis(); 

   if(debugLevel >= 4000) {
    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     System.out.println(">>>GsiException="+gsiException);
    }
   }
   try {
      String ttemp = parseLocalSourceFileForPath(localFileName);
      File f = new File(ttemp); 
      MSS_MESSAGE_W_E pftpmssg_e = new MSS_MESSAGE_W_E ();
      if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
	  SRM_MSS_UTIL.getMSSGSIError(gsiException, pftpmssg_e, debugLevel);
          Object[] param = new Object[3]; 
          param[0] = "REQUEST-ID="+fObj.getRequestToken();
          param[1] = "STATUS="+pftpmssg_e.getStatus();
          param[2] = "EXPLANATION="+pftpmssg_e.getExplanation();
          _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
          pftpmssg = pftpmssg_e.getStatus();
      }
      else {
          StringBuffer buf = new StringBuffer();
	  pftpmssg = 
           SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog, debugLevel,buf);
          File ff = new File (ftptransferlog);
          Object[] param = new Object[5]; 
          param[0] = "REQUEST-ID="+fObj.getRequestToken();
          param[1] = "LOGFILE="+ftptransferlog;
          param[2] = "LOGFILE exists="+ff.exists();
          param[3] = "STATUS="+pftpmssg;
          param[4] = "EXPLANATION="+buf.toString();
          _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
      }



      if(debugLevel >= 4000) {
        System.out.println(">>>pftpmssg="+pftpmssg.toString());
      }
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        srmPingStatus.setLastAccessedTime(""+new Date());
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        fObj.setExplanation("MSS Down.");
      }
      else {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        srmPingStatus.setLastAccessedTime(""+new Date());
        //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        //fObj.setExplanation("MSS Up.");
      }
      if(!f.exists() && (pftpmssg != MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED &&
              pftpmssg != MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE)) {
          util.printMessage("\nLocal file did not exists " +
		         localFileName, logger,debugLevel);
          util.printMessage("RemoteFile="+remoteFileName,logger,debugLevel);
          if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
           util.printMessage("Remotefile did not exists ", 
				logger,debugLevel);
           fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
           fObj.setExplanation("HPSS file did not exists.");
           status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
           status.setExplanation("HPSS file did not exists.");
          }
          else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
           util.printMessage("Remotefile no permission to read ", 
				logger,debugLevel);
           fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
           fObj.setExplanation("Authentication failed.");
           status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
           status.setExplanation("Authentication failed.");
          }
          else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
           util.printMessage("Remotefile no permission to read ", 
				logger,debugLevel);
           fObj.setPFTPStatus(pftpmssg);
           fObj.setExplanation(pftpmssg_e.getExplanation());
           status.setStatus(pftpmssg);
           status.setExplanation(pftpmssg_e.getExplanation());
          }
          else {
           util.printMessage("POSTPONE due to unknown error", 
				logger,debugLevel);
           fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
           fObj.setExplanation("POSTPONE due to unknown error");
           status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
           status.setExplanation("POSTPONE due to unknown error");
          }
       }
       else {
         if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
           if(tu != null) {
             transferTime = tu.getTransferTimeInMilliSeconds();
           } 
         }
         else {
           transferTime = SRM_MSS_UTIL.getMSSTransferTime(ftptransferlog);
         }
         if((transferTime == 0) || (f.length() == 0)) {
             //file was not transferred correctly. 
             //should I care about that.
           if(fileSize != 0 && f.length() != 0) { //partial transfer
            
             if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
              StringBuffer buf = new StringBuffer();
              pftpmssg = 
		  SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog,debugLevel,buf);
             }
             else {
               pftpmssg_e = new MSS_MESSAGE_W_E ();
	           SRM_MSS_UTIL.getMSSGSIError(gsiException, 
					pftpmssg_e, debugLevel);
               pftpmssg = pftpmssg_e.getStatus();
             }
             if(debugLevel >= 10) {
                util.printMessage("\nFile " + localFileName + 
			      " was not transfered correctly. ",logger,debugLevel);
                util.printMessage("FID="+ remoteFileName, logger,debugLevel);
                util.printMessage("transferTime="+transferTime, logger,debugLevel);
                util.printMessage("size expected="+fileSize, logger,debugLevel);
                util.printMessage("size transfered="+f.length(),logger,debugLevel);
             }
           }
           else if(f.length() == 0) { 
               //file was not transfered at all
              if(debugLevel >= 10) {
                  util.printMessage("\nFile " + localFileName +  
			        " was not transfered.");
              }
              if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
                 //no retry for this error
                 status.setTransferRate(0.0);
                 if(debugLevel >= 10) {
                  util.printMessage
			        ("\nFile does not exist in HPSS"); 
                  util.printMessage("HPSS Path " +remoteFileName);
                  util.printMessage("Local Path " +localFileName);
                 }
              }
              else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
                //will give up for now
                status.setTransferRate(0.0);
                if(debugLevel >= 10) {
                  util.printMessage("\nFile has no read permission " +
                       "in HPSS");
                  util.printMessage("HPSS Path " + remoteFileName);
                  util.printMessage("Local Path " + localFileName);
                }
             }
             else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
                status.setTransferRate(0.0);
                if(debugLevel >= 10) { 
                   util.printMessage
	            ("\nWARNING : GSI proxy has a problem, or either"+
		        " userid or password are WRONG or No permission.");
                }
             }
            else if((pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) ||
                 (pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)) {
               status.setTransferRate(0.0);
               util.printMessage("\nRemoteFileName="+ 
					remoteFileName,logger,debugLevel);
               util.printMessage(pftpmssg.toString()+  
				   " will try again", logger,debugLevel); 
               fObj.setFTPTransferLog(ftptransferlog);
               fObj.setExplanation("Will retry again"); 
               fObj.incrementNumRetry();
               fObj.setTimeStamp(new GregorianCalendar());
               fObj.setRetryTime(MSSWaitTime*2);
               if(fObj.getNumRetry() < getMSSMaxRetrial()) {
                 Object _parent = fObj.getTaskThread().getParent();
                 //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
                 ThreadCallBack taskThread = 
			             new ThreadCallBack((SRM_MSS)_parent);
                 taskThread.setCommand(fObj,status);
                 fObj.setTaskThread(taskThread);

                 if(monitorThread == null) {
                    threadDisable = false;
                    monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
                    monitorThread = 
						new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
                    monitorThread.start();
                 }

                 Object[] param = new Object[2]; 
                 param[0] = "SOURCE="+fObj; 
                 param[1] = "REQUEST-ID="+status.getRequestToken(); 
                 _theLogger.log(java.util.logging.Level.FINE,
					"GET_RETRY",(Object[])param);
                 
                 retrieveQueue.add(fObj);
               }
               else {
                 util.printMessage("\nrid=" + fObj.getRequestToken() + 
					" failed.",logger,debugLevel);
                 status.setStatus(pftpmssg);
                 fObj.setPFTPStatus(pftpmssg);
                 fObj.setStatus(pftpmssg);
               }
           }
           else {
             status.setTransferRate(0.0);
             if(debugLevel >= 10) { 
                util.printMessage ("\n"+pftpmssg.toString(),logger,debugLevel);
             }
           }
         }
         else if(fileSize == 0 && f.length() != 0) {
           //pftpmssg = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
           //transfer time is zero, may be parsing is incomplete,
           //check for getMSSMSSGetPutError        
           //client did not give fileSize   

           if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
             StringBuffer buf = new StringBuffer();
             pftpmssg = 
		SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog, debugLevel,buf);
           }
           else {
             pftpmssg_e = new MSS_MESSAGE_W_E ();
	        SRM_MSS_UTIL.getMSSGSIError(gsiException, 
					pftpmssg_e, debugLevel);
             pftpmssg = pftpmssg_e.getStatus();
           }
           if(debugLevel >= 10) {
              util.printMessage("\nFile " + 
			    localFileName + " stagged with " +
			    f.length() + " bytes.",logger,debugLevel);
           }
           if(fileSize != f.length()) {
             //wrong file size from client
             status.setSize(f.length());
             fileSize = f.length();
           }
         }
    } 
    else { 
       //pftpmssg = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
       if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
          StringBuffer buf = new StringBuffer();
          pftpmssg = 
	      SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog, debugLevel,buf);
       }
       else {
          pftpmssg_e = new MSS_MESSAGE_W_E ();
	  SRM_MSS_UTIL.getMSSGSIError(gsiException, 
			pftpmssg_e, debugLevel);
          pftpmssg = pftpmssg_e.getStatus();
       }
       if(debugLevel >= 10) {
           util.printMessage("\nFile " + 
		      localFileName + " stagged with " +
			    f.length() + " bytes.",logger,debugLevel);
       }
       if(fileSize != f.length()) {
          //wrong file size from client
          status.setSize(f.length());
          fileSize = f.length();
       }
   }

   //util.printMessage("\nLocalFile="+localFileName,logger);

   fObj.setPFTPStatus(pftpmssg);

   if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
        util.printMessage("\nRETRIEVED.",logger,debugLevel);
           //successful transfer
        if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
            /*
            try {
              File ftplog = new File(ftptransferlog);
              if(ftplog.exists()) { ftplog.delete(); }
            }catch(Exception ioe) {}
            */
            if(debugLevel <= 6000) {
             try {
               File ftplog = new File(proxyFile);
               if(ftplog.exists()) { ftplog.delete(); }
             }catch(Exception ioe) {}
            }
        }

       //util.printMessage("\nTRANSFERTIME="+ transferTime,logger);
  
       sizeOfFileInMB=(long)fileSize/MEGABYTE;
       //convert bytes to MB

       totalTime=endTime-startTime;  
       // if this happens then most likely 
       // the system failed to open
       // msstransferlog. to make results reasonable 
       //assume the following...

       if(transferTime < 1) {
        transferTime = totalTime;
       }
       status.setTransferRate((double)sizeOfFileInMB/transferTime);
       status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
       status.setExplanation("RETRIEVED");
       fObj.setExplanation("RETRIEVED");
       status.setSize(f.length());
   } 
  else if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
       status.setExplanation("No such path on HPSS.");
       fObj.setExplanation("No such path on HPSS.");
       try {
        File tf = new File(parseLocalSourceFileForPath(localFileName));
        if(tf.exists()) {
           tf.delete();
        }
        util.printMessage("\nFILE does not exist on mss",logger,debugLevel);
       }catch(Exception ioe) {}
  }
  else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED); 
       status.setExplanation ("user not authorized and FILE_CANNOT_BE_READ");
       fObj.setExplanation ("user not authorized and FILE_CANNOT_BE_READ");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
           tf.delete();
         }
         util.printMessage("\nFILE_CANNOT_BE_READ",logger,debugLevel);
       }catch(Exception ioe) {}
  }
  else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY); 
       status.setExplanation ("Given hpss path is a directory.");
       fObj.setExplanation ("Given hpss path is a directory");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
           tf.delete();
         }
         util.printMessage("\nGiven hpss path is a directory",
			logger,debugLevel);
       }catch(Exception ioe) {}
  }
  else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
       status.setTransferRate(0.0);
       //status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED);
       status.setExplanation ("TOO_MANY_PFTPS");
       fObj.setExplanation ("TOO_MANY_PFTPS");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
           tf.delete();
         }
         util.printMessage("\ntoo many mss connections, try again.", 
			logger,debugLevel);
       }catch(Exception ioe) {}
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
       status.setExplanation ("SRM_MSS_MSS_ERROR");
       fObj.setExplanation ("SRM_MSS_MSS_ERROR");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
            tf.delete();
         }
         util.printMessage("\nSRM_MSS_MSS_ERROR, postpone", logger,debugLevel);
       }catch(Exception ioe) {}
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
       status.setTransferRate(0.0);
       //status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
       status.setExplanation ("MSS down. try again later.");
       fObj.setExplanation ("MSS down. try again later.");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
           tf.delete();
         }
         util.printMessage("\nMSS down. try again later.", logger,debugLevel);
       }catch(Exception ioe) {}
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
       status.setExplanation ("No Permission");
       fObj.setExplanation ("No Permission");
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
          tf.delete();
         }
         util.printMessage("\nclient authentication failed", logger,debugLevel);
        }catch(Exception ioe) {}
  }
  else {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
       status.setExplanation ("unknown errors " + gsiException);
       fObj.setExplanation ("unknown errors " + gsiException);
       try {
         File tf = new File(parseLocalSourceFileForPath(localFileName));
         if(tf.exists()) {
            tf.delete();
         }
         util.printMessage("\nrequest failed with other errors",
				logger,debugLevel);
        }catch(Exception ioe) {}
   }
   }//end if

   if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
    if(debugLevel <= 6000) {
      try {
        File ftplog = new File(proxyFile);
        if(ftplog.exists()) { ftplog.delete(); }
      }catch(Exception ioe) {}
    }
   }

  }catch(Exception e) {
       util.printMessage("\nLocal file did not exists", logger);
       status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       status.setExplanation("POSTPONE due to unknown error " + 
			e.getMessage());
       fObj.setExplanation("POSTPONE due to unknown error " + e.getMessage());
  }
 }catch(IOException ioe) {
   ioe.printStackTrace();
 } 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmGetFileSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_FILE srmGetFileSize (String fPath, SRM_ACCESS_INFO accessInfo,
	boolean srmnocipher, boolean srmnonpassivelisting) 
   throws SRM_MSS_Exception, Exception
{
   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     fPath = "gsiftp://"+MSSHost+":"+MSSPort+fPath;
   }
   SRM_FILE status  = super.srmGetFileSize( fPath, accessInfo, srmnocipher,
			srmnonpassivelisting,this);

   //added into appropriate queue
   mssPending++;

   util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);
   if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue();

      if(fObj != null) {
      Object[] param = new Object [2];
      param[0] = fObj.toString();
      param[1] = "RID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE, 
			"SERVING_GET_FILE_SIZE_REQUEST",
			  (Object[]) param);

      //each file transfer is handled by seperate thread.
      //at any given time only MaxAllowed of tasks can happen.
      queueStatusMap.put(fObj.getRequestToken(),fObj);
      ThreadCallBack taskThread = fObj.getTaskThread(); 
      taskThread.start();
      } 
    }
    else  { // just added into queue and return
      ; 
    }

    return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmls
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_PATH srmLs ( String path, 
		  SRM_ACCESS_INFO accessInfo, 
		  boolean recursive,  
          boolean srmnocipher, 
		  boolean srmnonpassivelisting) throws SRM_MSS_Exception, Exception  {

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     path = "gsiftp://"+MSSHost+":"+MSSPort + path;
   }

    SRM_PATH status = super.srmls( path, accessInfo, recursive,
			  srmnocipher,srmnonpassivelisting, this);

    //added into appropriate queue
    mssPending++;

    util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

    if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue();

      if(fObj != null) {
        Object[] param = new Object [2];
        param[0] = fObj.toString();
        param[1] = "RID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE, "SERVING_LS_REQUEST",
			(Object[]) param);

        //each file transfer is handled by seperate thread.
        //at any given time only MaxAllowed of tasks can happen.
        queueStatusMap.put(fObj.getRequestToken(),fObj);
        ThreadCallBack taskThread = fObj.getTaskThread(); 
        taskThread.start();
      }
    }
    else  { // just added into queue and return
      ; 
    }

    return status; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssCopy
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssCopy(FileObj fObj, SRM_STATUS status) 
		throws SRM_MSS_Exception, Exception {

    String ftptransferlog="";
    String requestToken = status.getRequestToken ();

    if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
        status.setExplanation("Transfer aborted");
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fObj.setExplanation("Transfer aborted");
        return;
    }

    if(!logPath.equals("")) {
        /*
       ftptransferlog = logPath+"/"+"srm_mss.copy.log." + 
						fObj.getSourcePath() + "." + requestToken + 
                       "."+System.currentTimeMillis();
        */
       ftptransferlog = logPath+"/"+"srm_mss." + requestToken;
    }
    else {
        /*
       ftptransferlog = "srm_mss.copy.log." + 
						fObj.getSourcePath() + "." + requestToken + 
                          "."+System.currentTimeMillis();
        */
       ftptransferlog = "srm_mss." + requestToken;
    }

    String proxyFile = ftptransferlog+".proxy";
    String source = fObj.getSource();
    String target = fObj.getTarget();
    boolean srmnocipher = fObj.getSRMNoCipher();
    boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
    boolean recursive = fObj.getRecursive();

    SRM_ACCESS_TYPE mssType = fObj.getAccessType();
    SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
    SRM_OVERWRITE_MODE overwritemode = fObj.getOverWriteMode();

    String copyCommand = "cp ";
    String srmmsspath = "";
    srmmsspath = hsiPath; //because we are using hsi to do the copy

    if(recursive) {
      copyCommand = copyCommand + "-R " + source + " " + target; 
    }
    else {
      copyCommand = copyCommand + " " + source + " " + target; 
    }

   try {

    if(debugLevel >= 200) {
      util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
    } 

    proxyFile = ftptransferlog+".proxy";

    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
       try {
         writeProxyFile(proxyFile, accessInfo.getPasswd());
         checkProxyIsValid(proxyFile,true);
       }catch(SRM_MSS_Exception srme) {
         util.printMessage("\nERROR : expired proxy " + proxyFile);
            
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        status.setExplanation("Expired proxy.");
        throw srme;
       }catch(Exception ee) {
         util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        status.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        System.out.println("Exception="+ee.getMessage());
        throw new SRM_MSS_Exception
			("Failed to write proxy to local file system.");
       }

       Object[] param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
		  "WROTE_PROXY_FILE",(Object[])param);
    }


    if(overwritemode == SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_NO) {
       MSS_MESSAGE_W_E lsresultpre = mssListing(target, accessInfo, 
                   ftptransferlog, debugLevel,
                   mssType, pftpPath,
                   MSSHost, MSSPort,
                   srmnocipher,srmnonpassivelisting, 
		   setscipath,true,
		   fObj, status);

      if(lsresultpre.getStatus() == 
			MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
          status.setExplanation("Transfer aborted");
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          fObj.setExplanation("Transfer aborted");
          return;
      }

      if(lsresultpre.getStatus() != MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         status.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
         status.setExplanation("Remote target already exists");
         fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
         fObj.setExplanation("Remote target already exists.");
         throw new SRM_MSS_Exception("Remote target already exists.");
      }
    }

    MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

    File script = null;
   
    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
       /*
       try {
         writeProxyFile(proxyFile, accessInfo.getPasswd());
         checkProxyIsValid(proxyFile,true);
       }catch(SRM_MSS_Exception srme) {
         util.printMessage("\nERROR : expired proxy " + proxyFile);
            
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        status.setExplanation("Expired proxy.");
        throw srme;
       }catch(Exception ee) {
         util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        status.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        System.out.println("Exception="+ee.getMessage());
        throw new SRM_MSS_Exception
			("Failed to write proxy to local file system.");
       }

       Object[] param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
		  "WROTE_PROXY_FILE",(Object[])param);
       */

       script = File.createTempFile("script","",scriptPathDir);
       //script.deleteOnExit();

       PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
       writer.println("#!/bin/sh\n\n");
       writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
       writer.println("export LD_LIBRARY_PATH\n");
       writer.println("X509_USER_PROXY=" + proxyFile + "\n");
       writer.println("export X509_USER_PROXY" + "\n");
       writer.println(srmmsspath + " \"out " + ftptransferlog + "; " +copyCommand + "\"");
       writer.close();

       if(debugLevel >= 1000) {
         util.printMessage("\n+++ begin script ++++\n", logger,debugLevel);
         util.printMessage("Date: " + SRM_MSS.printDate() + "\n", logger,
                debugLevel);  
         util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
         util.printMessage("X509_USER_PROXY="+proxyFile+"\n",logger,debugLevel);
         util.printMessage("export X509_USER_PROXY\n",logger,debugLevel);
         util.printMessage ("LD_LIBRARY_PATH=" +
            this.javaLibraryPath +"\n", logger,debugLevel);
         util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
         util.printMessage(srmmsspath + " \"out " + 
	    ftptransferlog + "; " +copyCommand + "\"", logger,debugLevel);
        util.printMessage("\n+++ end script ++++\n", logger,debugLevel);
       }
    }
    else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {
       script = File.createTempFile("script","",scriptPathDir);
       //script.deleteOnExit();
       PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
       writer.println("#!/bin/sh\n\n");
       writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
       writer.println("export LD_LIBRARY_PATH\n"); 
       writer.println(srmmsspath + " \"out " + 
			ftptransferlog + "; " +copyCommand + "\"");
       writer.close();

       if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel);
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,
                debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + this.javaLibraryPath +"\n", 
			logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH\n", logger,debugLevel);
        util.printMessage(srmmsspath + " \"out " + ftptransferlog + "; " +
				copyCommand + "\"", logger,debugLevel);
        util.printMessage("\n+++ end script ++++\n", logger,debugLevel);
       } 
      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
      "WROTE_SCRIPT_FILE",(Object[])param);
    }
    else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
		mypasswd == null || mypasswd.length() == 0) {
         if(debugLevel >= 10) {
            util.printMessage
	     ("WARNING: Either userid or password wrong.",logger,debugLevel);
         }
         status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED); 
         fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
         fObj.setExplanation("Either userid or password wrong.");
         status.setExplanation("Either userid or password wrong.");
         throw new SRM_MSS_Exception("Either userid or password wrong.");
      }

      if(!srmnocipher) {
        //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
        //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
        mylogin = accessInfo.getLogin();
        mypasswd = accessInfo.getPasswd();
      }

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println
        (srmmsspath + " \"out " + ftptransferlog + "; " +copyCommand + "\"");
      writer.close();

      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel);
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,
                debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + this.javaLibraryPath +"\n", 
			logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel);
        util.printMessage(srmmsspath + " \"out " + ftptransferlog + 
				"; " +copyCommand + "\"", logger,debugLevel);
        util.printMessage("\n+++ end script ++++\n", logger,debugLevel);
      }
      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
      "WROTE_SCRIPT_FILE",(Object[])param);
   }
   else {
      util.printMessage("HPSS TYPE not implemented : " + mssType, logger,debugLevel);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("HPSS Type not implemented " + mssType);
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("HPSS Type not implemented " + mssType);
      throw new SRM_MSS_Exception("HPSS type not implemented " + mssType);
   }


   boolean useEnvp = false;
   long startTime = System.currentTimeMillis();

   boolean ok = false;
   Thread t = new Thread();
   while (!ok) { 
    try {
      if(script.exists()) {
        Process p0 = 
	     	Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
        if(p0.waitFor() == 0) {
	       p0.destroy();
           if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
             Object[] param = new Object[1]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             _theLogger.log(java.util.logging.Level.FINE,
               "NO_NEED_EXECUTE_SCRIPT_TRANSFER_ABORTED",(Object[])param);
             status.setExplanation("Transfer aborted");
             fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
             fObj.setExplanation("Transfer aborted");
             return;
           }
           else {
             Object[] param = new Object[1]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             _theLogger.log(java.util.logging.Level.FINE,
               "GOING_TO_EXECUTE_SCRIPT_FILE",(Object[])param);
             ExecScript process = 
	   	       new ExecScript(script.getAbsolutePath(),
                 this.javaLibraryPath, true,this, _theLogger);
             process.setLogFile(ftptransferlog);
             status.setCurrentProcess(process); 
             process.execCommand(useEnvp);
           }
           ok = true;
        }
     } 
    }catch(IOException ioe) {
      ok = false;
      t.sleep(1000);
    }catch(Exception ie) {
      ok = true;
      ie.printStackTrace();
    }
   }

   long endTime = System.currentTimeMillis();

    if(!_errorMessage.equals("") || _errorOccured) {
      File ftplog = new File(ftptransferlog);
      util.printMessage("Probably some problem in executing script.",
            logger,debugLevel);
      util.printMessage("error message="+_errorMessage);
      util.printMessage("error occured="+_errorOccured);
      if(!ftplog.exists() || ftplog.length() == 0) {
        util.printMessage
          ("log file is empty and " + ftptransferlog,logger,debugLevel);
      }
      else {
        util.printMessage
          ("log file is " + ftptransferlog,logger,debugLevel);
      }
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation
                        ("Probably some problem in executing script "+script);
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation
                        ("Probably some problem in executing script "+script);
    }


   try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.",
                logger, debugLevel);
          util.printMessage("log file is empty " + ftptransferlog,
                logger, debugLevel);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation
            ("Probably some problem in executing script, log file is empty.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation
            ("Probably some problem in executing script, log file is empty.");
        }
    }catch(Exception e) {}



   if(debugLevel < 3000) {
    try {
     if(script.exists()) {
       if(_errorMessage.equals("")) {
         //script.delete();
       }
     }
    }catch(Exception ioe) {
      util.printMessage("Exception " + ioe.getMessage());
    }  
   }

   //seems like the script runs successfully.
   //but there may be other problems such as login, file not found etc.
   //need to parse the transfer log to find out the details

   //do you think we need to check file exists or not etc.
   //I am not sure we will think about this later.

   //assume that file  really exists.

   //if file did not exists.

   Object[] param = new Object[1]; 
   param[0] = "REQUEST-ID="+fObj.getRequestToken();
   _theLogger.log(java.util.logging.Level.FINE,
      "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

    try {

      MSS_MESSAGE_W_E pftpmssg_e = new MSS_MESSAGE_W_E ();
	  SRM_MSS_UTIL.getMSSError(ftptransferlog,pftpmssg_e,debugLevel);
      pftpmssg = pftpmssg_e.getStatus();

          File ff = new File (ftptransferlog);
          param = new Object[5]; 
          param[0] = "REQUEST-ID="+fObj.getRequestToken();
          param[1] = "LOGFILE="+ftptransferlog;
          param[2] = "LOGFILE exists="+ff.exists();
          param[3] = "STATUS="+pftpmssg_e.getStatus();
          param[4] = "EXPLANATION="+pftpmssg_e.getExplanation();
          _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

      fObj.setPFTPStatus(pftpmssg);
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {

         util.printMessage("\nCopy file/dir failed due to " +
            "MSS down and will try again", logger,debugLevel);

         fObj.setExplanation("Will retry again");
         fObj.setFTPTransferLog(ftptransferlog);
         fObj.incrementNumRetry();
         fObj.setTimeStamp(new GregorianCalendar());
         fObj.setRetryTime(MSSWaitTime*2);

         if(fObj.getNumRetry() < getMSSMaxRetrial()) {
            Object _parent = fObj.getTaskThread().getParent();
            //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
            ThreadCallBack taskThread =
                 new ThreadCallBack((SRM_MSS)_parent);
            taskThread.setCommand(fObj,status);
            fObj.setTaskThread(taskThread);

            if(monitorThread == null) {
               threadDisable = false;
               monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
               monitorThread = 
				new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
               monitorThread.start();
            }

            param = new Object[3];
            param[0] = "SOURCE="+source;
            param[1] = "PATH="+target;
            param[2] = "REQUEST-ID="+status.getRequestToken();
            _theLogger.log(java.util.logging.Level.FINE,
                  "COPY_RETRY",(Object[])param);
            othersQueue.add(fObj);
         }
         else {
            util.printMessage("\nrid=" + fObj.getRequestToken() + 
				" failed.",logger,debugLevel);
            status.setStatus(pftpmssg);
            fObj.setPFTPStatus(pftpmssg);
            fObj.setStatus(pftpmssg);
         }
         if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
           status.setExplanation("MSS access limit reached, Try again later.");
           fObj.setExplanation("MSS access limit reached, Try again later.");
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
           status.setExplanation("MSS not available.");
           fObj.setExplanation("MSS not available.");
         }
      }
      
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
         util.printMessage("\nMSSCOPY : STATUS=TRANSFER_DONE", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
         status.setExplanation("Copy done successfully.");
         fObj.setExplanation("Copy done successfully.");

         /*
         if(debugLevel <= 200) {
           File f = new File(ftptransferlog);
           if(f.exists()) { f.delete(); }
         }
         */
         if(debugLevel <= 6000) {
           File f = new File(proxyFile);
           if(f.exists()) { f.delete(); }
         }
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         util.printMessage("\nMSSCOPY : STATUS=REQUEST_FAILED", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
         status.setExplanation("No access permission");
         fObj.setExplanation("No access permission");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
         util.printMessage("\nMSS limit reached.", logger,debugLevel);
         status.setExplanation("mss limit reached");
         fObj.setExplanation("mss limit reached");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
         util.printMessage("\nMSS system not available.", logger,debugLevel);
         status.setExplanation("mss not available");
         fObj.setExplanation("mss not available");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
         util.printMessage("\nSRM MSS Error", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
         status.setExplanation("MSS error");
         fObj.setExplanation("MSS error");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {
         util.printMessage("\nSRM MSS Error", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
         status.setExplanation
			("Trying to copy a directory without recursive option");
         fObj.setExplanation
			("Trying to copy a directory without recursive option");
      }
      else {
         util.printMessage("\n"+pftpmssg.toString(), logger,debugLevel);
         status.setStatus(pftpmssg);
         status.setExplanation(pftpmssg.toString());
         fObj.setExplanation(pftpmssg.toString());
      }
   }catch(Exception e) {
     e.printStackTrace();
   }
 }catch(IOException ioe) {
    ioe.printStackTrace();
 }  
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssDelete
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssDelete(FileObj fObj, SRM_STATUS status) throws Exception {

    String ftptransferlog="";
    String requestToken = status.getRequestToken();

    if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
        status.setExplanation("Transfer aborted");
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
        fObj.setExplanation("Transfer aborted");
        return;
    }

    if(!logPath.equals("")) {
       /*
       ftptransferlog = 
			logPath+"/"+"srm_mss.delete.log." + 
				fObj.getSourcePath() + "." + requestToken + 
                  "."+System.currentTimeMillis();
        */
       ftptransferlog = 
			logPath+"/"+"srm_mss." + requestToken; 
    }
    else {
       /*
       ftptransferlog = "srm_mss.delete.log." + 
				fObj.getSourcePath() + "." + requestToken +
                  "."+System.currentTimeMillis();
       */
       ftptransferlog = "srm_mss." + requestToken; 
    }

    String path = fObj.getFilePath(); 

    SRM_ACCESS_TYPE mssType = fObj.getAccessType();
    SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();

    String srmmsspath = pftpPath; 
    int srmmssport = MSSPort;

    boolean srmnocipher = fObj.getSRMNoCipher();
    boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
    boolean recursive = fObj.getRecursive();
    boolean isDir = fObj.getIsDir();

    if(recursive) {
     isDir=true; //make isDir true when recursive is true
    }

   String deleteCommand="";

   if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      deleteCommand = "delete " + path + "\n";
      if(isDir) {
         if(recursive) {
           srmmsspath = hsiPath; 
           deleteCommand = "rm -R " + path;
           if(enableHSI) {
             deleteCommand = "rmdir " + path;
           }
         }
         else {
           deleteCommand = "rmdir " + path;
         }
      }
   }

   try {

    if(debugLevel >= 200) {
      util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
    } 

    MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

    String proxyFile = ftptransferlog+".proxy";
    File script = null;

    GSSCredential credential = null;
    String gsiException = "";
    MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E (); 

    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
       try {
         writeProxyFile(proxyFile,accessInfo.getPasswd());
         credential = createAndCheckProxyIsValid(proxyFile,true);
       }catch(SRM_MSS_Exception srme) {
         util.printMessage("\nERROR : Expired proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        status.setExplanation("Expired proxy.");
        return;
       }catch(Exception ee) {
         util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        status.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        System.out.println("Exception="+ee.getMessage());
        return;
       }

       Object[] param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
		  "WROTE_PROXY_FILE",(Object[])param);
       
       SRMLsClient srmLsClient = new SRMLsClient(path,credential,
                logger,_theLogger,false,debugLevel);
       srmLsClient.setMaxWait(maxWait);
       srmLsClient.setWaitDelay(waitDelay);
       boolean value = srmLsClient.doDeleteDir
			(path,isDir,recursive,mssresult); 
       if(!value) {
          gsiException = mssresult.getExplanation();
          pftpmssg = mssresult.getStatus();
          if(debugLevel >= 4000) {
             util.printMessage("GsiException="+gsiException);
          }
       }
    }
    else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {
       script = File.createTempFile("script","",scriptPathDir);
       //script.deleteOnExit();
       PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
       writer.println("#!/bin/sh\n\n");
       writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
       writer.println("export LD_LIBRARY_PATH\n");
       if(recursive) {
        writer.println(srmmsspath + " \"out " + 
		ftptransferlog + "; " +deleteCommand + "\"");
       }
       else {
        writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
        writer.println("open " + MSSHost + " " + srmmssport);
        writer.println("user " + accessInfo.getLogin() + " " +
         accessInfo.getPasswd());
        writer.println("bin");
        writer.println(deleteCommand);
        writer.println("bye");
        writer.println("END");
       }
       writer.close();


       if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel);
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + this.javaLibraryPath +"\n", 
			logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel);
        if(recursive) {
          util.printMessage(srmmsspath + " \"out " + ftptransferlog + "; " +
				deleteCommand + "\"", logger,debugLevel);
        }
        else {
          util.printMessage (srmmsspath +
            " -vn << END > "+ ftptransferlog, logger,debugLevel);
          util.printMessage("open " + MSSHost + "   " +
            srmmssport + "\n\n", logger,debugLevel);
          util.printMessage("user " + accessInfo.getLogin() + "   " +
                    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
          util.printMessage("bin\n",logger,debugLevel);
          util.printMessage(deleteCommand, logger,debugLevel);
          util.printMessage("bye\n",logger,debugLevel);
          util.printMessage("END\n",logger,debugLevel);
          util.printMessage("+++ end script ++++\n", logger,debugLevel);
        } 
       } 
    }
    else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
		mypasswd == null || mypasswd.length() == 0) {
         if(debugLevel >= 10) {
            util.printMessage
				("WARNING: Either userid or password wrong.",logger,debugLevel);
         }
         status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED); 
         fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
         fObj.setExplanation("Either userid or password wrong.");
         status.setExplanation("Either userid or password wrong.");
         return;
      }

      if(srmnocipher) {
        mylogin = accessInfo.getLogin();
        mypasswd = accessInfo.getPasswd();
      }
      else {
        //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
        //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
        mylogin = accessInfo.getLogin();
        mypasswd = accessInfo.getPasswd();
      }  

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      if(recursive) {
        writer.println(srmmsspath + " \"out " + 
		ftptransferlog + "; " +deleteCommand + "\"");
      }
      else {
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + MSSHost + " " + srmmssport);
      if(srmnonpassivelisting) {
         writer.println("passive");
      }
      writer.println("user " + mylogin + " " +  mypasswd);
      writer.println("bin");
      writer.println(deleteCommand);
      writer.println("bye");
      writer.println("END");
      }
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel);
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" +
            this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel);
        util.printMessage (pftpPath +
            " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + MSSHost + "   " +
            srmmssport + "\n\n", logger,debugLevel);
        util.printMessage("passive\n", logger,debugLevel);
        util.printMessage("user " + mylogin + "   " +
                    mypasswd + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage(deleteCommand,logger,debugLevel);
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel);
      }
   }
   else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) {
      //writeLogFile(ftptransferlog,"opening logfile");
      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      //writer.println (hsiPath + " -q " + "\"out " + ftptransferlog +";pwd;");
      writer.println (hsiPath + " -q " + "\"out " + ftptransferlog +";");
      writer.println(deleteCommand + "; end\"");  
      writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog);
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
			logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage (hsiPath + 
	        //" -q "+ "\" out " + ftptransferlog + ";pwd;", logger,debugLevel);
	        " -q "+ "\" out " + ftptransferlog + ";", logger,debugLevel);
        util.printMessage(deleteCommand + ";end\""); 
        util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog, 
			logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
   }
   else {
      util.printMessage("HPSS TYPE not implemented : " + mssType);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("Unknown MSS Type");
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("Unknown MSS Type");
      return;
   }

   boolean useEnvp = false;
   long startTime = System.currentTimeMillis();

   if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {

     Object[] param = new Object[1]; 
     param[0] = "REQUEST-ID="+fObj.getRequestToken();
     _theLogger.log(java.util.logging.Level.FINE,
      "WROTE_SCRIPT_FILE",(Object[])param);

     boolean ok = false;
     Thread t = new Thread ();
     while (!ok) {
      try {
       if(script.exists()) {
          Process p0 =
            Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
          if(p0.waitFor() == 0) {   
	        p0.destroy();
            if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
              param = new Object[1]; 
              param[0] = "REQUEST-ID="+fObj.getRequestToken();
              _theLogger.log(java.util.logging.Level.FINE,
                "NO_NEED_RUN_SCRIPT_TRANSFER_ABORTED",(Object[])param);
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
              status.setExplanation("Transfer aborted");
              fObj.setExplanation("Transfer aborted");
              return;
            }
            else {
              param = new Object[1]; 
              param[0] = "REQUEST-ID="+fObj.getRequestToken();
              _theLogger.log(java.util.logging.Level.FINE,
               "GOING_TO_EXECUTE_SCRIPT_FILE",(Object[])param);
              ExecScript process = 
	  	       new ExecScript(script.getAbsolutePath(),
               this.javaLibraryPath, true,this,_theLogger);
              process.setLogFile(ftptransferlog);
              status.setCurrentProcess(process); 
              process.execCommand(useEnvp);
            }
           ok = true;
         }
        }
       }catch(IOException ioe) {
         ok = false;
         t.sleep(1000); 
       }catch(Exception ie) {
         ok = true;
         ie.printStackTrace();
       }
     }

     long endTime = System.currentTimeMillis();


      if(!_errorMessage.equals("") || _errorOccured) {
      File ftplog = new File(ftptransferlog);
       util.printMessage("Probably some problem in executing script.");
       util.printMessage("error message="+_errorMessage);
       util.printMessage("error occured="+_errorOccured);
       if(!ftplog.exists() || ftplog.length() == 0) {
         util.printMessage("log file is empty and " + ftptransferlog);
       }
       else {
         util.printMessage("log file is " + ftptransferlog);
       }
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       fObj.setExplanation("ERROR while executing script "+script);
       status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       status.setExplanation("ERROR while executing script "+script);
      }//end if
     

     try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.",
                logger, debugLevel);
          util.printMessage("log file is empty " + ftptransferlog,
                logger, debugLevel);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation
            ("Probably some problem in executing script, log file is empty.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation
            ("Probably some problem in executing script, log file is empty.");
        }
    }catch(Exception e) {}


     if(debugLevel < 3000) {
      try {
        if(script.exists()) {
          if(_errorMessage.equals("")) {
            //script.delete();
          }
        }
       }catch(Exception ioe) {
         util.printMessage("Exception " + ioe.getMessage());
       }  
     }

     //seems like the script runs successfully.
     //but there may be other problems such as login, file not found etc.
     //need to parse the transfer log to find out the details

     //do you think we need to check file exists or not etc.
     //I am not sure we will think about this later.

     //assume that file  really exists.

     //if file did not exists.

     param = new Object[1]; 
     param[0] = "REQUEST-ID="+fObj.getRequestToken();
     _theLogger.log(java.util.logging.Level.FINE,
      "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

   }

   try {

      MSS_MESSAGE_W_E pftpmssg_e = new MSS_MESSAGE_W_E();
      if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
	     SRM_MSS_UTIL.getMSSError(ftptransferlog,pftpmssg_e,debugLevel);
         pftpmssg = pftpmssg_e.getStatus();

         File ff = new File (ftptransferlog);
         Object[] param = new Object[5]; 
         param[0] = "REQUEST-ID="+fObj.getRequestToken();
         param[1] = "LOGFILE="+ftptransferlog;
         param[2] = "LOGFILE exists="+ff.exists();
         param[3] = "STATUS="+pftpmssg_e.getStatus();
         param[4] = "EXPLANATION="+pftpmssg_e.getExplanation();
         _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
      }
      else {
         if(gsiException.equals("")) {
            pftpmssg = mssresult.getStatus();
         }
         else {
            SRM_MSS_UTIL.getMSSGSIError(gsiException, pftpmssg_e,debugLevel);
            pftpmssg = pftpmssg_e.getStatus();
         }
      }
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        srmPingStatus.setLastAccessedTime(""+new Date());
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        fObj.setExplanation("MSS Down.");
      }
      else {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        srmPingStatus.setLastAccessedTime(""+new Date());
        //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        //fObj.setExplanation("MSS Up.");
      }

      fObj.setPFTPStatus(pftpmssg);
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {

         util.printMessage("\nRemote file/dir failed due to " +
            "MSS down and will try again", logger,debugLevel);

         fObj.setExplanation("Will retry again");
         fObj.setFTPTransferLog(ftptransferlog);
         fObj.incrementNumRetry();
         fObj.setTimeStamp(new GregorianCalendar());
         fObj.setRetryTime(MSSWaitTime*2);

         if(fObj.getNumRetry() < getMSSMaxRetrial()) {
            Object _parent = fObj.getTaskThread().getParent();
            //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
            ThreadCallBack taskThread =
                 new ThreadCallBack((SRM_MSS)_parent);
            taskThread.setCommand(fObj,status);
            fObj.setTaskThread(taskThread);

           if(monitorThread == null) {
               threadDisable = false;
               monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
               monitorThread = 
					new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
               monitorThread.start();
            }

            Object[] param = new Object[2];
            param[0] = "SOURCE="+fObj;
            param[1] = "REQUEST-ID="+status.getRequestToken();
            _theLogger.log(java.util.logging.Level.FINE,
                  "DELETE_RETRY",(Object[])param);
            othersQueue.add(fObj);
         }
         else {
            util.printMessage("\nrid=" + fObj.getRequestToken() + 
				" failed.",logger,debugLevel);
            status.setStatus(pftpmssg);
            fObj.setPFTPStatus(pftpmssg);
            fObj.setStatus(pftpmssg);
         }
         if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
           status.setExplanation("MSS access limit reached, Try again later.");
           fObj.setExplanation("MSS access limit reached, Try again later.");
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
           status.setExplanation("MSS not available.");
           fObj.setExplanation("MSS not available.");
         }
      }
      
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
         util.printMessage("\nMSSDELETE : STATUS=TRANSFER_DONE", 
			logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
         status.setExplanation("Delete done successfully.");
         fObj.setExplanation("Delete done successfully.");

         /*
         if(debugLevel <= 200) {
           File f = new File(ftptransferlog);
           if(f.exists()) { f.delete(); }
         }
         */
         if(debugLevel <= 6000) {
         File f = new File(proxyFile);
           if(f.exists()) { f.delete(); }
         }
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_FILE_EXISTS) {
         util.printMessage("\nMSSDELETE : STATUS=FILE_EXISTS", 
			logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
         status.setExplanation("Directory not empty.");
         fObj.setExplanation("Directory not empty.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         util.printMessage("\nMSSDELETE : STATUS=REQUEST_FAILED", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
         status.setExplanation("No access permission");
         fObj.setExplanation("No access permission");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_DIRECTORY_NOT_EMPTY) {
         util.printMessage("\nMSSDELETE : STATUS=DIRECTORY_NOT_EMPTY", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_DIRECTORY_NOT_EMPTY);
         status.setExplanation("Directory not empty.");
         fObj.setExplanation("Directory not empty.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
         util.printMessage("\nMSS limit reached.", logger,debugLevel);
         status.setExplanation("mss limit reached");
         fObj.setExplanation("mss limit reached");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
         util.printMessage("\nMSS system not available.", logger,debugLevel);
         status.setExplanation("mss not available");
         fObj.setExplanation("mss not available");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_NOT_A_DIRECTORY) {
         util.printMessage("\nGiven file path is not a directory.", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_IS_NOT_A_DIRECTORY);
         status.setExplanation("Given file path is not a directory.");
         fObj.setExplanation("Given file path is not a directory.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {
         util.printMessage("\nGiven file path is a directory.", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
         status.setExplanation("Given file path is a directory.");
         fObj.setExplanation("Given file path is a directory.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
         util.printMessage("\nSRM MSS Error", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
         status.setExplanation("MSS error");
         fObj.setExplanation("MSS error");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
         util.printMessage("\nMSSMKDIR : STATUS=AUTHENTICATION_FAILED", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
         status.setExplanation("Permission denied.");
         fObj.setExplanation("Permission denied.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         util.printMessage("\nMSSMKDIR : STATUS=SRM_MSS_NO_SUCH_PATH", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
         status.setExplanation("No such path.");
         fObj.setExplanation("No such path.");
      }
      else {
         util.printMessage("\n"+pftpmssg.toString(), logger,debugLevel);
         status.setStatus(pftpmssg);
         status.setExplanation(pftpmssg.toString());
         fObj.setExplanation(gsiException);
      }
   }catch(Exception e) {
     e.printStackTrace();
   }
 }catch(IOException ioe) {
    ioe.printStackTrace();
 }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssGetFileSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssGetFileSize(FileObj fObj, SRM_FILE srmFile) 
	throws Exception {

    String logftpmssg="";
    String proxyFile="";
    String requestToken = srmFile.getRequestToken();

    if(srmFile.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) { 
       srmFile.setExplanation("Transfer aborted");
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
       fObj.setExplanation("Transfer aborted");
       return;
    }

    if(!logPath.equals("")) {
      logftpmssg = logPath+"/"+"srm_mss." + requestToken;
    }
    else {
      logftpmssg = "srm_mss." + requestToken;
    }

    proxyFile = logftpmssg+".proxy";
    
    boolean possibleFileNotExists = true;
    boolean possibleDirectoryPath = false;

    String path = fObj.getFilePath(); 
    srmFile.setFile(path);

    SRM_ACCESS_TYPE mssType = fObj.getAccessType();
    SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
    boolean srmnocipher = fObj.getSRMNoCipher();
    boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
    String userId = fObj.getUserId();

    if(debugLevel >= 200 && mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      util.printMessage("\nMY LOG path : " + logftpmssg, logger,debugLevel);
    }

    MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E(); 
    GSSCredential credential = null;

    long size=0;
    //without * in the filename
    if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      mssresult = mssListing(path, accessInfo, 
                              logftpmssg, debugLevel,
                              mssType, pftpPath,
                              MSSHost, MSSPort,
                              srmnocipher, srmnonpassivelisting, 
			      setscipath,false,fObj,srmFile);
    }
    else {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
        SRMLsClient srmLsClient = new SRMLsClient(path,credential,
                logger,_theLogger,false,debugLevel);
        srmLsClient.setMaxWait(maxWait);
        srmLsClient.setWaitDelay(waitDelay);
        mssresult = new MSS_MESSAGE_W_E (); 
        size = srmLsClient.getRemoteFileSize(mssresult);
      }catch(SRM_MSS_Exception srme) {
        util.printMessage("\nERROR : expired proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy. " + srme.getMessage());
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        mssresult.setExplanation("Expired proxy. " +srme.getMessage());
        return; 
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system. " + 
			ee.getMessage());
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        mssresult.setExplanation(
		"FAILED_TO_WRITE_PROXY to local file system. "+ee.getMessage());
        System.out.println("Exception="+ee.getMessage());
        return; 
      }
    }

    MSS_MESSAGE lsresult = mssresult.getStatus();

    Object[] param = new Object[4]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    param[1] = "LOGFILE="+logftpmssg;
    param[2] = "mssresult="+mssresult.getStatus();
    param[3] = "lsresult="+lsresult;
    _theLogger.log(java.util.logging.Level.FINE,
	   "GET_FILE_SIZE_STATUS_OF_MSSLISTING_RETURNED",(Object[])param);

    util.printMessage("MSSListing Result : " + lsresult.toString(), 
		logger,debugLevel);
    util.printMessage("MSSListing Explanation : " + mssresult.getExplanation(), 
		logger,debugLevel);

    if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
       lsresult == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) {
      srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
      srmPingStatus.setLastAccessedTime(""+new Date());
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
      fObj.setExplanation("MSS Down.");
    }
    else {
      srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
      srmPingStatus.setLastAccessedTime(""+new Date());
      //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
      //fObj.setExplanation("MSS Up.");
    }

    //Aug 25, 11
    if((lsresult != MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) &&
       (lsresult != MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) &&
       (lsresult != MSS_MESSAGE.SRM_MSS_TRANSFER_DONE)) {
       srmFile.setStatus(lsresult);
       fObj.setPFTPStatus(lsresult);
    }

    if(lsresult  == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
      if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
         int SIZE = 1024; //some buffer size;
         try {
           FileInputStream fis = new FileInputStream(logftpmssg); 
           BufferedReader bis = new BufferedReader( new InputStreamReader(fis));

           boolean parseStart = false; 
           boolean parseEnd = false; 
           Vector sizes = new Vector();
           Vector tstamp = new Vector();

           String ref = "";
           int moreFilesListed = 0;
           while((ref= bis.readLine())!= null)  {
            if((ref.trim().equals("\n")) ||
                (ref.trim().equals("\r"))) continue;
			    //(ref.trim().startsWith("pwd"))) continue;
              
            int idx = ref.indexOf("150 Opening ASCII mode data connection for");
            if(idx != -1) {
              if(debugLevel >= 300) {
                util.printMessage("\ndirectory listing starts", 
					logger,debugLevel);
              }
              parseStart=true;  
              continue;
            }
            if(enableHSI) {
               parseStart = true;
            }
            idx = ref.indexOf("HSI_ERROR_CODE=0");
            //empty dir
            //System.out.println(">>>MORE FILESLISTED="+moreFilesListed);
            //System.out.println(">>>REF="+ref);
            if(idx != -1) {
               parseEnd=true;
            }
            idx = ref.indexOf("hpss_Opendir failed Code:");
            if(idx != -1) {
              util.printMessage("\nACCESS denied to " + path, 
					logger,debugLevel);
              parseEnd=true;
              srmFile.setFile(path);
              srmFile.setSize(0);
              srmFile.setExplanation("ACCESS denied to " + path); 
              srmFile.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);  
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);  
              fObj.setExplanation("ACCESS denied to " + path); 
              lsresult=MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED;
            } 
            idx = ref.indexOf("hpss_Lstat: No such file or directory");
            if(idx != -1) {
              if(debugLevel >= 300) { 
                util.printMessage
                  ("\ndirectory listing ends", logger,debugLevel);
              }
              parseEnd = true;
              srmFile.setFile(path);
              srmFile.setSize(0);
              srmFile.setExplanation("No such file or directory"); 
              srmFile.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              fObj.setExplanation("No such file or directory"); 
              lsresult=MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
            }
            idx = ref.indexOf("226 Transfer complete");
            if (idx != -1) {
              if(debugLevel >= 300) {
                util.printMessage
                  ("\ndirectory listing ends", logger,debugLevel);
              }  
              parseEnd = true;
            }
            if(parseStart && !parseEnd) {
              int count = 0;
              moreFilesListed++;
              StringBuffer buf = new StringBuffer(); 
              StringTokenizer stk = new StringTokenizer(ref," ");
              while(stk.hasMoreTokens()) {
                String temp = stk.nextToken();
                //System.out.println(">>>TEMP="+temp + " " + count);
                //System.out.println(">>>path="+path);
                //System.out.println(">>>moreFilesListed="+moreFilesListed);
                if(count == 4) { 
                  Long ll = new Long(temp);
                  srmFile.setSize(ll.longValue());
                }
                if(count == 5 || count == 6 || count == 7) {
                  buf.append(temp+" ");
                }
                if(count == 8) {
                  if(enableHSI) {
                    if(moreFilesListed > 2) {
                      possibleDirectoryPath=true;
                    }
                  }
                  else {
                  if(!path.trim().equals(temp.trim())) {
                      possibleDirectoryPath=true;
                  }
                  }
                }
                count++;
              }//end while(stk has more tokens)
              if(count > 1) {
                possibleFileNotExists = false;
              } 
              srmFile.setTimeStamp(buf.toString().trim());
            }
           }//end while

             param = new Object[6]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             param[1] = "LOGFILE="+logftpmssg;
             param[2] = "PARSESTART="+parseStart;
             param[3] = "PARSEEND="+parseEnd;
             param[4] = "POSSIBLEDIRECTORYPATH="+possibleDirectoryPath;
             param[5] = "POSSIBLEFILENOTEXISTS="+possibleFileNotExists;
             _theLogger.log(java.util.logging.Level.FINE,
	        "STATUS_OF_PARSING_MSSLISTING",(Object[])param);

           bis.close();
           fis.close();
            /*
           if(debugLevel <= 200) {
            File f = new File(logftpmssg);
            if(f.exists()) { 
              f.delete();
            }
           }
           */
           if(possibleDirectoryPath) {
            util.printMessage("\nGiven path is a possible directory path",
				logger,debugLevel);
            util.printMessage("\nLast part (token 8) of dir output, is not" +
			  " matching to given path",logger,debugLevel);
            srmFile.setFile(path);
            srmFile.setSize(-1);
            String explanation="Given path is a directory";
            srmFile.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
            srmFile.setExplanation(explanation);
            fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
            fObj.setExplanation(explanation);
           }
           else { 
            if(!possibleFileNotExists) {
              srmFile.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
              fObj.setExplanation("success.");
              srmFile.setExplanation("success.");
            }
            else {
             //do one more check, to make sure whether it is a blank
             //dir or non existing path.

             param = new Object[2]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             param[1] = "LOGFILE="+logftpmssg;
             _theLogger.log(java.util.logging.Level.FINE,
	        "BEFORE_CALLING_MSSLISTING_AGAIN",(Object[])param);

             util.printMessage("\nPossibly file not exists",
					logger,debugLevel);
             util.printMessage("\nDoing one more check to make sure",
					logger,debugLevel);

             MSS_MESSAGE_W_E lsresultpre = mssListing(path, accessInfo, 
                                          logftpmssg, debugLevel,
                                          mssType, pftpPath,
                                          MSSHost, MSSPort,
                                          srmnocipher,srmnonpassivelisting, 
					  setscipath, true,fObj,srmFile);

             param = new Object[3]; 
             param[0] = "REQUEST-ID="+fObj.getRequestToken();
             param[1] = "LOGFILE="+logftpmssg;
             param[2] = "STATUS="+lsresultpre.getStatus(); 
             _theLogger.log(java.util.logging.Level.FINE,
	        "AFTER_CALLING_MSSLISTING_AGAIN",(Object[])param);

             util.printMessage("\nMSSListing Result " + 
	       lsresultpre.getStatus().toString(), logger,debugLevel);

             if(lsresultpre.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE){
                if(enableHSI) {
                  parseEnd = true;
                }
             }

             if(lsresultpre.getStatus() == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
              util.printMessage("\nNo such path exists", logger,debugLevel);
              srmFile.setFile(path);
              srmFile.setSize(-1);
              String explanation="No such file or directory";
              srmFile.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
              srmFile.setExplanation(explanation);
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
              fObj.setExplanation(explanation);
             } 
             else { 
              if(parseStart && parseEnd) {
                util.printMessage
				("\n parsing is fine, given path is a directory", 
					logger,debugLevel);
                srmFile.setFile(path);
                srmFile.setSize(-1);
                String explanation="Given path is a directory";
                srmFile.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
                srmFile.setExplanation(explanation);
                fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
                fObj.setExplanation(explanation);
              }
              else {
                util.printMessage("\n such path exists", logger,debugLevel);
                util.printMessage("\n may be parsing error, (parsing failure)" 
			+ "look in to pftp output file", logger,debugLevel);
                srmFile.setFile(path);
                srmFile.setSize(-1);
                srmFile.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
                srmFile.setExplanation(lsresultpre.getExplanation());
                fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
                fObj.setExplanation(lsresultpre.getExplanation());
              }
            }
          }
        }


      }catch(Exception e) {
        util.printMessage("\nCould not read " + logftpmssg, 
			logger,debugLevel);
        srmFile.setExplanation(
	    "could not read the output file produced by hpss command" + 
				logftpmssg);
        srmFile.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR); 
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR); 
        fObj.setExplanation(
	    "could not read the output file produced by hpss command" + 
				logftpmssg);
      }
      /*
      File f = new File(logftpmssg);
      if(f.exists()) {
        f.delete();
      }
      */

      param = new Object[7]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      param[1] = "LOGFILE="+logftpmssg;
      param[2] = "lsresult="+lsresult;
      param[3] = "PFTPSTATUS="+fObj.getPFTPStatus();
      param[4] = "PFTPEXPLANATION="+fObj.getExplanation();
      param[5] = "STATUS="+srmFile.getStatus();
      param[6] = "EXPLANATION="+srmFile.getExplanation();
      _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_PARSED_FROM_LOGFILE",(Object[])param);
     }
     else {
        SRMLsClient srmLsClient = new SRMLsClient(path,credential,
                logger,_theLogger,false,debugLevel);
        srmLsClient.setMaxWait(maxWait);
        srmLsClient.setWaitDelay(waitDelay);
        srmFile.setFile(path);
        srmFile.setSize(size);
        try {
          Date timeStamp = srmLsClient.lastModified();
          srmFile.setTimeStamp(timeStamp.toString());
          srmFile.setStatus(mssresult.getStatus());
          srmFile.setExplanation(mssresult.getExplanation());
          fObj.setPFTPStatus(mssresult.getStatus());
          fObj.setExplanation(mssresult.getExplanation());

          param = new Object[3]; 
          param[0] = "SOURCE="+fObj; 
          param[1] = "REQUEST-ID="+srmFile.getRequestToken();
          param[2] = "SIZE="+size;
          _theLogger.log(java.util.logging.Level.FINE,
				  "GET_FILE_SIZE_DONE",(Object[])param);
        }catch(Exception e) { 
          srmFile.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          srmFile.setExplanation(
				"Error while doing GridFTPClient.lastModified "+ 
						e.getMessage() );
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation(
				"Error while doing GridFTPClient.lastModified "+
					e.getMessage());
        }
     }
     if(debugLevel <= 6000) {
       File f = new File(proxyFile);
       if(f.exists()) {
         f.delete();
       }
     }
   }//end if
   else if((lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) ||
            (lsresult == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)) {
       util.printMessage("\nrid="+ fObj.getRequestToken(),logger,debugLevel);
       util.printMessage(lsresult.toString()+  " will try again", 
			logger,debugLevel); 
       fObj.setFTPTransferLog(logftpmssg);
       fObj.incrementNumRetry();
       fObj.setTimeStamp(new GregorianCalendar());
       fObj.setRetryTime(MSSWaitTime*2);
       if(fObj.getNumRetry() < getMSSMaxRetrial()) {
          fObj.setExplanation("Will retry again"); 
          //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
          Object _parent = fObj.getTaskThread().getParent();
          ThreadCallBack taskThread = 
	             new ThreadCallBack((SRM_MSS)_parent);
          taskThread.setCommand(fObj,srmFile);
          fObj.setTaskThread(taskThread);

          if(monitorThread == null) {
            threadDisable = false;
            monitorThreadPool = 
			new MonitorThreadPool(1,(SRM_MSS)this);
            monitorThread = 
			new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
            monitorThread.start();
          }

          param = new Object[2]; 
          param[0] = "SOURCE="+fObj; 
          param[1] = "REQUEST-ID="+srmFile.getRequestToken();
          _theLogger.log(java.util.logging.Level.FINE,
				  "GET_FILE_SIZE_RETRY",(Object[])param);
          othersQueue.add(fObj);
       }
       else {
          util.printMessage("\nrid=" + fObj.getRequestToken() + 
			" failed.",logger,debugLevel);
          srmFile.setStatus(lsresult);
          fObj.setStatus(lsresult);
          fObj.setPFTPStatus(lsresult);
       }
       if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
         srmFile.setExplanation("MSS access limit reached, Try again later.");
         fObj.setExplanation("MSS access limit reached, Try again later.");
       } 
       else if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) { 
         srmFile.setExplanation("MSS not available.");
         fObj.setExplanation("MSS not available.");
       }
    }
    else if(lsresult == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
       srmFile.setExplanation("Transfer aborted");
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
       fObj.setExplanation("Transfer aborted");
    }
    else {
       srmFile.setFile(path);
       srmFile.setSize(-1);
       srmFile.setStatus(lsresult);
       fObj.setPFTPStatus(lsresult);

       param = new Object[5]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       param[1] = "LOGFILE="+logftpmssg;
       param[2] = "srmFileStatus="+srmFile.getStatus();
       param[3]=  "fObjStatus="+fObj.getPFTPStatus();
       param[4] = "lsresult="+lsresult;
       _theLogger.log(java.util.logging.Level.FINE,
	   "SRM_FILE_STATUS_SET_NOW",(Object[])param);

       if(lsresult == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         srmFile.setExplanation("NO such file or directory");
         fObj.setExplanation("NO such file or directory");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
         srmFile.setExplanation("Authentication failed.");
         fObj.setExplanation("Authentication failed.");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         srmFile.setExplanation("Authorization failed.");
         fObj.setExplanation("Authorization failed.");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR) { 
         srmFile.setExplanation(mssresult.getExplanation());
         fObj.setExplanation(mssresult.getExplanation());
       } 
       else if(lsresult == MSS_MESSAGE.SRM_MSS_BAD_PROXY) { 
         srmFile.setExplanation("Expired proxy.");
         fObj.setExplanation("Expired proxy.");
       } 
       else {
         if(!mssresult.getExplanation().equals("")) {
           srmFile.setExplanation(mssresult.getExplanation());
           fObj.setExplanation(mssresult.getExplanation());
         }
         else {
           srmFile.setExplanation("mss listing failed.");
           fObj.setExplanation("mss listing failed.");
         }
       }

       if(debugLevel <= 6000) {
       File f = new File(proxyFile);
       if(f.exists()) {
         f.delete();
       }
       }
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssLs
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void mssLs(FileObj fObj, 
	SRM_PATH srmPath) throws Exception {

    String path = fObj.getFilePath(); 
    String requestToken = srmPath.getRequestToken();

    if(srmPath.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) { 
      srmPath.setExplanation("Transfer aborted");
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
      fObj.setExplanation("Transfer aborted");
      return;
    }

    SRM_ACCESS_TYPE mssType = fObj.getAccessType();
    SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
    String userId = fObj.getUserId();
    String srmlsPath = pftpPath; 

    String srmmsshost = MSSHost;
    int srmmssport = MSSPort;

    boolean srmnocipher = fObj.getSRMNoCipher();
    boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
    boolean recursive = fObj.getRecursive();
   
    boolean possibleFileNotExists=true;
    boolean plainFileListing=false;

    String tFileName =""; 
    String myPath = "";
    String patternPath = "";
    boolean lsPattern=false;

    int length = path.length();
    int idx = path.lastIndexOf("/");
    if(idx != -1) {
       String temp = path.substring(idx);
       if(temp.length() > 1) { 
         myPath = path;
         patternPath=path.substring(0,idx);
         tFileName = path.substring(idx+1);
       }
       else {
         myPath = path.substring(0,idx);
       }
       idx = tFileName.indexOf("*");
       if(idx != -1) {
          lsPattern = true; 
       }
    }

    if(debugLevel >= 10) {
       util.printMessage("\nMY SRMLS PATH : " + myPath, logger,debugLevel);
       util.printMessage("MY SRMLS FILENAME : " + tFileName, logger,debugLevel);
    }

    String logftpmssg="";

    if(!logPath.equals("")) {
       logftpmssg = logPath+"/"+"srm_mss." + requestToken;
    }
    else {
       logftpmssg = "srm_mss." + requestToken;
    }
    String proxyFile = logftpmssg+".proxy";

    if(debugLevel >= 200) {
      util.printMessage("\nMY LOG path : " + logftpmssg, logger,debugLevel);
    }

   MSS_MESSAGE lsresult = null; 
   MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E();
   GSSCredential credential = null;
   SRMLsClient srmLsClient = null;

   if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      //without * in the filename
      mssresult = mssListing(path, accessInfo, 
                              logftpmssg, debugLevel,
                              mssType, srmlsPath,
                              srmmsshost, srmmssport,
                              srmnocipher, srmnonpassivelisting,
                              setscipath,false,fObj,srmPath);

      lsresult = mssresult.getStatus(); 
    }
    else {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
      }catch(SRM_MSS_Exception srme) {
        util.printMessage("\nERROR : expired proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy. " + srme.getMessage());
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        mssresult.setExplanation("Expired proxy. " + srme.getMessage());
        return; 
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system. " +
				ee.getMessage());
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        mssresult.setExplanation(
			"FAILED_TO_WRITE_PROXY to local file system. " + ee.getMessage());
        System.out.println("Exception="+ee.getMessage());
        return; 
      }

      Object[] param = new Object[1]; 
      param[0] ="REQUEST-ID="+requestToken;
      _theLogger.log(java.util.logging.Level.FINE,
	     "WROTE_PROXY_FILE",(Object[])param);
      srmLsClient = new SRMLsClient(path,credential,
                       logger,_theLogger,false,debugLevel);        

      srmLsClient.setMaxWait(maxWait);
      srmLsClient.setWaitDelay(waitDelay);
 
       try {
        boolean value = srmLsClient.doList(path,requestToken,false,
			recursive,mssresult,false);
       }catch(Exception e) {
          System.out.println("Exception at doList=" + e.getMessage());
       }
       //lsresult = mssresult.getStatus(); 
       if(mssresult.getStatus() == MSS_MESSAGE.SRM_MSS_IS_NOT_A_DIRECTORY) {
         System.out.println(">>>I am here " );
         //could be a plain file
         int iidx = path.lastIndexOf("/");
         String tempPath = path;
         String fName = path;
         if(iidx != -1) {
           tempPath = path.substring(0,iidx);
           fName = path.substring(iidx+1);
         }
         srmLsClient = new SRMLsClient(tempPath,credential,
                       logger,_theLogger,false,debugLevel);        
         srmLsClient.setMaxWait(maxWait);
         srmLsClient.setWaitDelay(waitDelay);
         try {
           boolean value = srmLsClient.doList(fName,requestToken,false,
			recursive,mssresult,true);
         }catch(Exception e) {
          System.out.println("Exception at doList=" + e.getMessage());
         }
       }
       lsresult = mssresult.getStatus(); 
    }

    util.printMessage("MSSListing : " + lsresult.toString(), 
			logger,debugLevel);

    if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE || 
       lsresult == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) {
       srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
       srmPingStatus.setLastAccessedTime(""+new Date());
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);  
       fObj.setExplanation("MSS DOWN."); 
    }
    else {
       srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
       srmPingStatus.setLastAccessedTime(""+new Date());
       //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);  
       //fObj.setExplanation("MSS Up."); 
    }

    if(lsresult  == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
      if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
         SRM_PATH tempSrmPath = srmLsClient.getSrmPath();
         srmPath = tempSrmPath;
         //srmPath.setDir(path);
         //srmPath.isDir(true);
         srmPath.setStatus(lsresult);  
         srmPath.setExplanation(mssresult.getExplanation()); 
         statusMap.put(requestToken,srmPath);
         fObj.setPFTPStatus(lsresult);  
         fObj.setExplanation(mssresult.getExplanation()); 
         return;
      }
      else {
       int SIZE = 1024; //some buffer size;
       try {
         FileInputStream fis = new FileInputStream(logftpmssg); 
         BufferedReader bis = new BufferedReader( new InputStreamReader(fis));

         Vector breakFile = new Vector ();
         if(lsPattern) {
           String tfilename2 = tFileName;
           StringTokenizer stk = new StringTokenizer(tFileName,"*");
           while(stk.hasMoreTokens()) {
             breakFile.addElement(stk.nextToken());
           }
           if(debugLevel >= 300) {
             util.printMessage("\nParsing files for star : ", 
				  logger,debugLevel);
             for(int i = 0; i < breakFile.size(); i++) {
               util.printMessage(" " + breakFile.elementAt(i),
				  logger,debugLevel);
             }
           }
         }

          int totalCount = 0;
          if(debugLevel >= 1000) {
             util.printMessage("\n+++ FTP output ++++ ", logger,debugLevel);  
          }
          boolean parseStart = false; 
          boolean parseEnd = false; 
          Vector dirs = new Vector();
          Vector fids = new Vector();
          Vector sizes = new Vector();
          Vector tstamp = new Vector();

          String ref = "";
          while((ref= bis.readLine())!= null)  {
            if((ref.trim().equals("\n")) ||
                (ref.trim().equals("\r"))) continue;
            idx = ref.indexOf("150 Opening ASCII mode data connection for");
            if(idx != -1) {
              if(debugLevel >= 300) {
                util.printMessage("\ndirectory listing starts", 
					logger,debugLevel);
              }
              parseStart=true;  
              continue;
            }
            if(enableHSI) {
              parseStart=true;  
            }
            idx = ref.indexOf("hpss_Opendir failed Code:");
            if(idx != -1) {
              util.printMessage("\nACCESS denied to " + path, 
				    logger,debugLevel);
              parseEnd=true;
              //just set path here. we don't know more about this file.
              srmPath.setDir(path);
              srmPath.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);  
              srmPath.setExplanation("ACCESS denied to " + path); 
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);  
              fObj.setExplanation("ACCESS denied to " + path); 
              lsresult=MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED;
            } 
            idx = ref.indexOf("hpss_Lstat: No such file or directory");
            if(idx != -1) {
              if(debugLevel >= 300) { 
                util.printMessage("\ndirectory listing ends", logger,debugLevel);
              }
              parseEnd = true;
              //just set path here. we don't know more about this file.
              srmPath.setDir(path);
              srmPath.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              srmPath.setExplanation("No such file or directory"); 
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              fObj.setExplanation("No such file or directory"); 
              lsresult=MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
            }
            idx = ref.indexOf("Could not stat");
            if(idx != -1) {
              if(debugLevel >= 300) { 
                util.printMessage("\ndirectory listing ends", logger,debugLevel);
              }
              parseEnd = true;
              //just set path here. we don't know more about this file.
              srmPath.setDir(path);
              srmPath.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              srmPath.setExplanation("No such file or directory"); 
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
              fObj.setExplanation("No such file or directory"); 
              lsresult=MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
            }
            idx = ref.indexOf("226 Transfer complete");
            if (idx != -1) {
              if(debugLevel >= 300) {
                util.printMessage("\ndirectory listing ends", logger,debugLevel);
              }  
              parseEnd = true;
            }
            if(parseStart && !parseEnd) {
//actual parsing for the file sizes and the file names and timestamp
/*
PARSED: -rw-r-----
PARSED: 1
PARSED: asim
PARSED: gc5
PARSED: 22921216
PARSED: Jan
PARSED: 3
PARSED: 12:37
PARSED: set163_01_32evts_dst.xdf.STAR.DB
*/

              possibleFileNotExists = false;
              StringTokenizer stk = new StringTokenizer(ref," ");
              int count = 0;
              boolean isDir = false; 
              String ttsize = "";
              String ttdir="";
              StringBuffer buf = new StringBuffer();
              while(stk.hasMoreTokens()) {
                String temp = stk.nextToken();
                //System.out.println(">>>TEMP="+temp);
                if(count == 0)  {
                  if(temp.startsWith("d")) isDir=true;
                }
                if(count == 4) { 
                  ttsize = temp;
                }
                if(count == 5 || count == 6 || count == 7) {
                  buf.append(temp+" ");
                }
                if(count == 8) { 
                  ttdir = temp;
                  if(enableHSI) { plainFileListing=true;}
                  else {
                  if(path.equals(ttdir)) {
                     plainFileListing=true;
                  }
                  }
                }
                count++;
              }//end while(stk has more tokens)
              //dir with only one file and no dirs underneath
              if(!plainFileListing) {
                srmPath.setDir(path);
                srmPath.isDir(true);
                util.printMessage(" Set isDir is true", logger,debugLevel);
              }
              //just plain file listing
              else { 
                srmPath.setDir(path);
                //because of the last part of dir truncated e.g. hourly6
                //int ii = path.lastIndexOf("/");
                //if(ii != -1) {
                  //srmPath.setDir(path.substring(0,ii));
                //}
              }
              if(debugLevel >= 1000) {
                 if(isDir) {
                   util.printMessage("\nDIRECTORY : " + ttdir,logger,debugLevel);
                 }
                 else { 
                   util.printMessage("\nFILE NAME : " + ttdir,logger,debugLevel);
                 }
                 util.printMessage("FILE SIZE : " + ttsize,logger,debugLevel);
                 util.printMessage("TimeStamp : " + buf.toString(),logger,debugLevel);
              }
              if(isDir) {
                 dirs.addElement(ttdir);
              }
              else {
                totalCount++;
                if(!ttdir.trim().equals("")) { 
                  if(lsPattern){
                    idx = ttdir.indexOf(patternPath);
                    if(idx != -1) {
                      ttdir = ttdir.substring(patternPath.length());
                    }
                    if(ttdir.startsWith("/")) {
                      ttdir = patternPath+ttdir;
                    }
                    else {
                      ttdir = patternPath+"/"+ttdir;
                    }
                  }
                  else {
                    if(path.equals(ttdir)) {
                       ttdir=path;
                    }
                    else {
                      idx = ttdir.indexOf(path);
                      if(idx != -1) {
                        ttdir = ttdir.substring(path.length());
                      }
                      if(path.endsWith("/")) {
                        ttdir = path+ttdir;
                      }
                      else { 
                        ttdir = path+"/"+ttdir;
                      }
                    }
                  }
                }
                if(lsPattern) {
                   boolean lspresult = true;
                   int lspi = 0;
                   while(lspresult && lspi<breakFile.size()) {
                     boolean bb = 
			             ttdir.equals((String)breakFile.elementAt(lspi));
                     if(bb == false) {
                        break;
                     }
                     lspi++;
                   }
                   if(lspresult) {
                      if(!ttdir.trim().equals("")) {
                        fids.addElement(ttdir);
                        sizes.addElement(ttsize); 
                        tstamp.addElement(buf.toString());
                      }
                   }
                   else {
                     if(debugLevel >= 300) {
                        util.printMessage("\nSKIPPING " + ttdir, 
							logger,debugLevel);
                     }
                     totalCount --;  
                   }
                }
                else{
                   if(!ttdir.trim().equals("")) {
                      fids.addElement(ttdir);
                      sizes.addElement(ttsize);
                      tstamp.addElement(buf.toString());
                   }
                }
              }
            }
          }//end while
          if(enableHSI) {
             parseEnd=true;
          }
          bis.close();
          fis.close();  
          /*
          if(debugLevel <= 200) {
            File f = new File(logftpmssg);
            if(f.exists()) { 
              f.delete();
            }
          }
          */
          if(debugLevel >= 100) {
            util.printMessage("\nSRMLS: DIRS="+path, logger,debugLevel);
            util.printMessage("TOTALCOUNT="+totalCount, logger,debugLevel);
            util.printMessage("sizes.size()="+sizes.size(), logger,debugLevel);
            util.printMessage("fids.size()="+fids.size(), logger,debugLevel);
            util.printMessage("tstamp.size()="+tstamp.size(), logger,debugLevel);
          }
          for(int ii =0; ii < fids.size(); ii++) {
            String fidPath="";
            fidPath=(String)fids.elementAt(ii);
            SRM_FILE ftemp = new SRM_FILE();
            ftemp.setRequestToken(requestToken);
            int jj = fidPath.lastIndexOf("/");
            if(jj != -1) {
              if(fidPath.length() > jj+1) {
                ftemp.setFile(fidPath.substring(jj+1)); 
              }
              else {
                if(jj > 0) {
                  int kk = fidPath.lastIndexOf("/",jj-1);
                  if(kk != -1) { 
                    ftemp.setFile(fidPath.substring(kk+1,jj)); 
                  }
                  else {
                    ftemp.setFile(fidPath); 
                  }
                }
                else {
                    ftemp.setFile(fidPath); 
                }
              }
            }
            else {
              ftemp.setFile(fidPath); 
            }
            long lsize = 0;
            try {
              Long ll = new Long((String)sizes.elementAt(ii));
              lsize = ll.longValue();
            }catch(NumberFormatException nfe) {}
            ftemp.setSize(lsize);
            ftemp.setTimeStamp((String)tstamp.elementAt(ii));
            srmPath.setFids(ftemp);
            if(debugLevel >= 1000) {
               util.printMessage("\nSRMLS: FILE="+ftemp.getFile(),logger,debugLevel);
            }
          }
          if(possibleFileNotExists) {
            srmPath.setDir(path); 
            srmPath.isDir(true);
          }
          if(!recursive || dirs.size() == 0) {
              srmPath.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
          }
          srmPath.setExplanation("success.");
          fObj.setExplanation("success.");

          //recursive directory listing now
          for (int kk = 0; kk < dirs.size(); kk++) {
             String newDir="";
             if(path.endsWith("/")) {
               newDir=path+ dirs.elementAt(kk);
             }
             else {
               newDir=path+ "/" + dirs.elementAt(kk);
             }
             if(debugLevel >= 1000) {
               util.printMessage("\ndirs["+kk+"]="+newDir,logger,debugLevel);
             }
             if(recursive && !lsPattern) {
               SRM_PATH newPath = new SRM_PATH();
               newPath.setRequestToken(requestToken); 
               FileObj newFObj = new FileObj
                  (this, accessType,accessInfo,fObj.getMSSIntf(),requestToken,
					"ls",newPath);
               newFObj.setFilePath(newDir);
               newFObj.setRecursive(recursive);
               newFObj.setSRMNoCipher(srmnocipher);
               newFObj.setSRMNonPassiveListing(srmnonpassivelisting);
               newFObj.setPFTPStatus(newPath.getStatus());
               //sets for dir with dirs underneath and recursive
               newPath.setDir(newDir);
               newPath.setFileObj(newFObj);
               mssLs(newFObj,newPath);
               srmPath.setSubPath(newPath);
               //set status to parent to done, only at the last directory,  
               //otherwise it is ending prematurely.
               //remember this is not a pure recursive function with return
               //value to set the parent's state. 
               if(kk == dirs.size()-1)  {
                 srmPath.setDir(path); 
                 srmPath.isDir(true);
                 srmPath.setStatus(newPath.getStatus()); //we need this also
                 fObj.setSRMPath(newPath); //to make MontorThreaad go on.
                 fObj.setPFTPStatus(newPath.getStatus());
               } 
             }
             else {
               SRM_PATH rtemp =  new SRM_PATH();
               rtemp.setRequestToken(requestToken); 
               rtemp.setDir(newDir);
               rtemp.isDir(true);
               rtemp.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE); 
               fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE); 
               fObj.setExplanation("success.");
               rtemp.setExplanation("success.");
               srmPath.setSubPath(rtemp);
               //sets for dir with dirs underneath and not recursive
               srmPath.setDir(path); 
               srmPath.isDir(true);
             }
          }
          //remove dirs later
          if((fids.size() == 0) && (dirs.size() == 0)) {
            /*
            srmPath.setExplanation("invalid ENV setup " +
		"or NO such file or directory " +
		"or directory is empty."); 
            */
            if(!possibleFileNotExists)  {
             srmPath.setDir(path);
             srmPath.isDir(true); 
             srmPath.setExplanation("No files in this directory/blank dir.");
             lsresult = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
             srmPath.setStatus(lsresult);
             fObj.setPFTPStatus(lsresult);
             fObj.setExplanation("No files in this directory/blank dir.");
            }
           else {
               //make sure this a blank dir or non existing path before
               //we decide anything.

               MSS_MESSAGE_W_E mssresultpre = mssListing(path, accessInfo, 
                              logftpmssg, debugLevel,
                              mssType, srmlsPath,
                              srmmsshost, srmmssport,
                              srmnocipher, srmnonpassivelisting,
                              setscipath,true,fObj,srmPath);

               MSS_MESSAGE lsresultpre = mssresultpre.getStatus();

               if(lsresultpre == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
                 srmPath.setDir(path);
                 srmPath.setExplanation("No such file or directory"); 
                 srmPath.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
                 fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);  
                 fObj.setExplanation("No such file or directory"); 
                 lsresult=MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
               }
               else {
                if(parseStart && parseEnd) {
                  srmPath.setDir(path);
                  srmPath.isDir(true); 
                  srmPath.setExplanation("No files in this directory.");
                  lsresult = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
                  srmPath.setStatus(lsresult);
                  fObj.setPFTPStatus(lsresult);
                  fObj.setExplanation("No files in this directory.");
                }
                else {
                  //some times the parsing conditions fails (not included)
                  //and we thought it is successful,but it is not 
                  srmPath.isDir(true); 
                  srmPath.setExplanation(mssresultpre.getExplanation());
                  lsresult = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
                  srmPath.setStatus(lsresult);
                  fObj.setPFTPStatus(lsresult);
                  fObj.setExplanation(mssresultpre.getExplanation());
                }
               }
            }
          }
       }catch(Exception e) {
          util.printMessage("\nCould not read " + logftpmssg, logger,debugLevel);
          //just setDir here, because we don't anything about this filepath. 
          srmPath.setDir(path);
          srmPath.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR); 
          srmPath.setExplanation("Could not read file " + logftpmssg);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR); 
          fObj.setExplanation("Could not read file " + logftpmssg);
       }
      }
    }
    else if((lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) ||
            (lsresult == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)) {
       util.printMessage("\nrid="+ fObj.getRequestToken(),logger,debugLevel);
       util.printMessage(lsresult.toString()+  " will try again", 
			logger,debugLevel); 

       fObj.setExplanation("Will retry again");
       fObj.setFTPTransferLog(logftpmssg);
       fObj.incrementNumRetry();
       fObj.setTimeStamp(new GregorianCalendar());
       fObj.setRetryTime(MSSWaitTime*2);
       fObj.setPFTPStatus(lsresult);
       if(fObj.getNumRetry() < getMSSMaxRetrial()) {
          Object _parent = fObj.getTaskThread().getParent();
          //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
          ThreadCallBack taskThread = 
	             new ThreadCallBack((SRM_MSS)_parent);
          taskThread.setCommand(fObj,srmPath);
          fObj.setTaskThread(taskThread);

          if(monitorThread == null) {
            threadDisable = false;
            monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
            monitorThread = 
				new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
            monitorThread.start();
          }

          Object[] param = new Object[2]; 
          param[0] = "SOURCE="+fObj; 
          param[1] = "REQUEST-ID="+srmPath.getRequestToken(); 
          _theLogger.log(java.util.logging.Level.FINE,
				  "LS_RETRY",(Object[])param);
          othersQueue.add(fObj);
       }
       else {
          util.printMessage("\nrid=" + fObj.getRequestToken() + 
		    " failed.",logger,debugLevel);
          srmPath.setStatus(lsresult);
          fObj.setPFTPStatus(lsresult);
          fObj.setStatus(lsresult);
          //mss is down for more than num retries,
          //so set my setDir here,
          //still I don't know whether it is a dir or just plain file path.
          srmPath.setDir(path);
       }
       if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
         srmPath.setExplanation("MSS access limit reached, Try again later.");
         fObj.setExplanation("MSS access limit reached, Try again later.");
       } 
       else if(lsresult == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) { 
         srmPath.setExplanation("MSS not available.");
         fObj.setExplanation("MSS not available.");
       }
    }
    else {
       //since it is a error, I don't need to add any SRM_FILE into it ?

       //just add the path to setDir
       srmPath.setDir(path);

       srmPath.setStatus(lsresult);
       fObj.setPFTPStatus(lsresult);

       if(lsresult == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         srmPath.setExplanation("NO such file or directory");
         fObj.setExplanation("NO such file or directory");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
         srmPath.setExplanation("Authentication failed.");
         fObj.setExplanation("Authentication failed.");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         srmPath.setExplanation("Authorization failed.");
         fObj.setExplanation("Authorization failed.");
       }
       else if(lsresult == MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR) { 
         srmPath.setExplanation("mss listing failed.");
         fObj.setExplanation("mss listing failed.");
       } 
       else if(lsresult == MSS_MESSAGE.SRM_MSS_BAD_PROXY) { 
         srmPath.setExplanation("Expired proxy.");
         fObj.setExplanation("Expired proxy.");
       } 
       else if(lsresult == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) { 
         srmPath.setExplanation("Transfer aborted.");
         fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
         fObj.setExplanation("Transfer aborted.");
       } 
       else {
         if(!mssresult.getExplanation().equals("")) {
           srmPath.setExplanation(mssresult.getExplanation());
           fObj.setExplanation(mssresult.getExplanation());
         }
         else {
           srmPath.setExplanation("mss listing failed.");
           fObj.setExplanation("mss listing failed.");
         }
       }
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssListing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private MSS_MESSAGE_W_E mssListing (String rPath,
                        SRM_ACCESS_INFO accessInfo,
                        String logftpmssg, int debugLevel,
                        SRM_ACCESS_TYPE mssType, String srmlsPath, 
                        String srmmsshost, int srmmssport,
                        boolean nocipher, boolean srmnonpassivelisting,
                        String siteSpecific, 
			boolean preCheck,FileObj fObj, Object s) {

  /*
  _completed = false;
  _errorMessage = "";
  _errorOccured=false;
  */

 MSS_MESSAGE_W_E result = new MSS_MESSAGE_W_E();
 MSS_MESSAGE status = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
 result.setStatus(status); 
 result.setExplanation("Transfer done");

 String proxyFile="";
 String ftpTest="";

 util.printMessage("\nTYPE : " + mssType, logger,debugLevel);

 try {
   File script=null;

   GSSCredential credential = null;

   if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
       proxyFile=logftpmssg+".proxy";

       Object[] param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
		  "GOING_TO_WRITE_PROXY_FILE",(Object[])param);
       try { 
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
       }catch(SRM_MSS_Exception srme) {  
         util.printMessage("\nERROR : Expired Proxy " + proxyFile,
			logger,debugLevel);
         result.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
         result.setExplanation("Expired proxy");
         return result;
       }catch(Exception ee) {  
         util.printMessage("\nERROR : could not write proxy " + proxyFile);
         result.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
         result.setExplanation("Could not write proxy " + proxyFile);
         System.out.println("Exception="+ee.getMessage());
         return result;
       }

       param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
		  "WROTE_PROXY_FILE",(Object[])param);

       script = File.createTempFile("script","",scriptPathDir);
       //script.deleteOnExit();
       PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
       writer.println("#!/bin/sh\n\n");
       writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
       writer.println("export LD_LIBRARY_PATH\n");
       writer.println("X509_USER_PROXY=" + proxyFile + "\n");
       writer.println("export X509_USER_PROXY" + "\n");
       writer.println (srmlsPath + " -v << END " + " > " + logftpmssg);
       writer.println("open " + srmmsshost + " " + srmmssport);
       if(preCheck) {
         //writer.println("cd " + rPath +"\n\n");
         writer.println("ls " + rPath);
       }
       else {
         writer.println("dir " + rPath);
       }
       writer.println("bye");
       writer.println("END");
       writer.close();


       if(debugLevel >= 1000) {
          util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
          util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
                logger,debugLevel);
          util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
          util.printMessage("X509_USER_PROXY="+proxyFile+"\n",
                logger,debugLevel);
          util.printMessage("export X509_USER_PROXY\n",logger,debugLevel);
          util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
          util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
          util.printMessage (srmlsPath + 
	        " -v << END > "+ logftpmssg, logger,debugLevel);
          util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
          if(preCheck) { 
            //util.printMessage("cd " + rPath +"\n",logger,debugLevel);
            util.printMessage("ls " + rPath +"\n",logger,debugLevel);
          }
          else { 
            util.printMessage("dir " + rPath +"\n",logger,debugLevel);
          }
          util.printMessage("bye ", logger,debugLevel); 
          util.printMessage("END ", logger,debugLevel); 
          util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
      }//end ACCESS_TYPE_GSI
      else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) { 

        Object[] param = new Object[1]; 
        param[0] = "REQUEST-ID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE,
	    "GOING_TO_WRITE_SCRIPT_FILE",(Object[])param);

        script = File.createTempFile("script","",scriptPathDir);
        //script.deleteOnExit();
        PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
        writer.println("#!/bin/sh\n\n");
        writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
        writer.println("export LD_LIBRARY_PATH\n");
        writer.println (srmlsPath + " -vn << END > " + logftpmssg);
        writer.println("open " + srmmsshost + " " + srmmssport);
        writer.println("user " + accessInfo.getLogin() + " " + 
			accessInfo.getPasswd());
        if(preCheck) {
          //writer.println("cd " + rPath +"\n\n");
          writer.println("ls " + rPath);
        }
        else {
          writer.println("dir " + rPath);
        }
        writer.println("bye");
        writer.println("END");
        writer.close();


        if(debugLevel >= 1000) {
          util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
          util.printMessage("Date: " + SRM_MSS.printDate() + 
				"\n",logger,debugLevel);
          util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
          util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
          util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel);
          util.printMessage (srmlsPath + 
	        " -vn << END > "+ logftpmssg, logger,debugLevel);
          util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
          util.printMessage("user " + accessInfo.getLogin() + "   " + 
		    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
          if(preCheck) {
            //util.printMessage("cd " + rPath +"\n",logger,debugLevel);
            util.printMessage("ls " + rPath +"\n",logger,debugLevel);
          }
          else {
            util.printMessage("dir " + rPath +"\n",logger,debugLevel);
          }
          util.printMessage("bye ", logger,debugLevel); 
          util.printMessage("END ", logger,debugLevel); 
          util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }

       param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
	    "WROTE_SCRIPT_FILE",(Object[])param);
      }//end access type encrypt
      else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) { 
        Object[] param = new Object[1]; 
        param[0] = "REQUEST-ID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE,
	    "GOING_TO_WRITE_SCRIPT_FILE",(Object[])param);
          String myLogin="";
          String myPasswd="";
          if(accessInfo.getLogin().length() == 0 || 
				accessInfo.getPasswd().length() == 0) {
             if(debugLevel >= 10) {
              util.printMessage("\nMSSLS ERROR : WRONG LOGIN/PASSWD",logger,debugLevel);
             }
             result.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
             result.setExplanation("Wrong Login/Password");
             return result;
          }  

          if(nocipher) {
             myLogin = accessInfo.getLogin();
             myPasswd = accessInfo.getPasswd();
          }
          else {
            //myLogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
            //myPasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
            myLogin = accessInfo.getLogin();
            myPasswd = accessInfo.getPasswd();
          }  

          script = File.createTempFile("script","",scriptPathDir);
          //script.deleteOnExit();
          PrintWriter writer = 
		    new PrintWriter(new FileWriter(script,false),true);
          writer.println("#!/bin/sh\n\n");
          writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
          writer.println("export LD_LIBRARY_PATH\n");
          writer.println (srmlsPath + " -vn << END > " + logftpmssg);
          writer.println("open " + srmmsshost + " " + srmmssport);
          if(srmnonpassivelisting) {
            writer.println("passive");
          }
          writer.println("user " + myLogin + " " + myPasswd);
          if(preCheck) { 
            //writer.println("cd " + rPath);
            writer.println("ls " + rPath);
          }
          else { 
            writer.println("dir " + rPath);
          }
          writer.println("bye");
          writer.println("END");
          writer.close();


         if(debugLevel >= 1000) {
          util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
          util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,
                debugLevel);
          util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
          util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
          util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
          util.printMessage (srmlsPath + 
	        " -vn << END > "+ logftpmssg, logger,debugLevel);
          util.printMessage("open " + srmmsshost + " " + srmmssport, 
                logger,debugLevel);
          if(srmnonpassivelisting) {
            util.printMessage("passive\n",logger,debugLevel);
          }
          util.printMessage("user " + myLogin + " " + 
		    myPasswd, logger,debugLevel);
          if(preCheck) {
            //util.printMessage("cd " + rPath +"\n",logger,debugLevel);
            util.printMessage("ls " + rPath,logger,debugLevel);
          }
          else {
            util.printMessage("dir " + rPath,logger,debugLevel);
          }
          util.printMessage("bye ", logger,debugLevel); 
          util.printMessage("END ", logger,debugLevel); 
          util.printMessage("+++ end script ++++\n", logger,debugLevel); 
        }

       param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
	    "WROTE_SCRIPT_FILE",(Object[])param);
      }
      else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) { 
        Object[] param = new Object[1]; 
        param[0] = "REQUEST-ID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE,
	    "GOING_TO_WRITE_SCRIPT_FILE",(Object[])param);

        //writeLogFile(logftpmssg,"opening logfile");
        script = File.createTempFile("script","",scriptPathDir);
        //script.deleteOnExit();
        PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
        writer.println("#!/bin/sh\n\n");
        writer.println (hsiPath + " -q " + "\"out " + logftpmssg
			//+";pwd;");
			+";");
        writer.println("ls -l " + rPath + "; end\"");
        writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + logftpmssg);
        writer.close();


        if(debugLevel >= 1000) {
          util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
          util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
                logger,debugLevel);
          util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
          util.printMessage (hsiPath + 
	        //" -q \"out "+logftpmssg+";" + "pwd;" + 
	        " -q \"out "+logftpmssg+";" +  
					" ls -l " + rPath + "; end\"");
          util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> "+ logftpmssg,
			       logger,debugLevel);
          util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
       param = new Object[1]; 
       param[0] = "REQUEST-ID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE,
	    "WROTE_SCRIPT_FILE",(Object[])param);
      }//end access type encrypt
      else {
        util.printMessage("\nHPSS TYPE not implemented : " + mssType);
        result.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        result.setExplanation("HPSS type not implemented " + mssType);
        return result;
      }


      boolean useEnvp = false;
      long startTime = System.currentTimeMillis(); 
      util.printMessage("\nMSS_LISTING_START", logger,debugLevel);

      boolean ok = false;
      Thread t = new Thread();

      while(!ok) {
       try {
          if(script.exists()) {
            Process p0 = 
	    Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
            if(p0.waitFor() == 0) {
		      p0.destroy();
              MSS_MESSAGE currentStatus = fObj.getPFTPStatus();
              if(currentStatus == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
                   Object[] param = new Object[1]; 
                   param[0] = "REQUEST-ID="+fObj.getRequestToken();
                   _theLogger.log(java.util.logging.Level.FINE,
			       "NO_NEED_TO_EXECUTE_SCRIPT_TRANSFER_ABORTED",
							(Object[])param);
                   fObj.setExplanation("Transfer aborted");
                   fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
                   result.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED); 
                   result.setExplanation("Transfer aborted");
                   return result; 
              }
              else {
                   Object[] param = new Object[1]; 
                   param[0] = "REQUEST-ID="+fObj.getRequestToken();
                   _theLogger.log(java.util.logging.Level.FINE,
			       "GOING_TO_EXECUTE_SCRIPT",(Object[])param);
                   ExecScript process = 
	  	             new ExecScript(script.getAbsolutePath(), 
	                 this.javaLibraryPath, true,this,_theLogger);
                   process.setLogFile(logftpmssg);
                   if(s instanceof SRM_FILE) {
                     ((SRM_FILE) s).setCurrentProcess(process); 
                   }
                   else if(s instanceof SRM_PATH) {
                     ((SRM_PATH) s).setCurrentProcess(process); 
                   }
                   else if(s instanceof SRM_MSSFILE_STATUS) {
                     ((SRM_MSSFILE_STATUS) s).setCurrentProcess(process); 
                   }
                   else if(s instanceof SRM_STATUS) {
                     ((SRM_STATUS) s).setCurrentProcess(process); 
                   }
                   process.execCommand(useEnvp);
                }
               ok = true;
             }
          }
       }catch(IOException ioe) { 
          ok = false;
          t.sleep(1000);
       }catch(Exception ie) { 
          ok = true;
          ie.printStackTrace();
       }
      }

      long endTime = System.currentTimeMillis(); 

     
      if(!_errorMessage.equals("") || _errorOccured) {
      File ftplog = new File(logftpmssg);
        util.printMessage("\nProbably some problem in executing script.");
        util.printMessage("error message="+_errorMessage);
        util.printMessage("error occured="+_errorOccured);
        if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("log file is empty " + logftpmssg);
        }
        else {
          util.printMessage("log file is " + logftpmssg);
        }
        result.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        result.setExplanation
            ("probably some problem in executing script " + script);
        return result;
       }
     

      try {
       File ftplog = new File(logftpmssg);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(logftpmssg);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(logftpmssg);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.",
                logger, debugLevel);
          util.printMessage("log file is empty " + logftpmssg,
                logger, debugLevel);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation
            ("Probably some problem in executing script, log file is empty.");
          result.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          result.setExplanation
            ("Probably some problem in executing script, log file is empty.");
        }
    }catch(Exception e) {}


      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
	   "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
      
      MSS_MESSAGE_W_E estatus = new MSS_MESSAGE_W_E();
      SRM_MSS_UTIL.getMSSError(logftpmssg, estatus, debugLevel);
      result.setStatus(estatus.getStatus());
      result.setExplanation(estatus.getExplanation());

      File ff = new File (logftpmssg);

      param = new Object[7]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      param[1] = "LOGFILE="+logftpmssg;
      param[2] = "LOGFILE EXISTS="+ff.exists();
      param[3] = "STATUS="+estatus.getStatus();
      param[4] = "EXPLANATION="+estatus.getExplanation();
      param[5] = "RESULT_STATUS="+result.getStatus();
      param[6] = "RESULT_EXPLANATION="+result.getExplanation();
      _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

      util.printMessage("HPSS Path : "+rPath, logger,debugLevel); 

      if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         result.setStatus(estatus.getStatus());
         result.setExplanation("File directory does not exist in HPSS");
         if(debugLevel >= 10) {
            util.printMessage("\nFile directory does not exist in HPSS.", 
				logger,debugLevel); 
         }
      }
      else if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED){
         result.setStatus(estatus.getStatus());
         result.setExplanation("File has no read permission in HPSS");
         if(debugLevel >= 10) {
            util.printMessage("\nFile has no read permission in HPSS.", 
				logger,debugLevel); 
         }
      }
      else if(estatus.getStatus() == 
                MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
         result.setStatus(estatus.getStatus());
         result.setExplanation("GSI proxy has a problem/Authenticaion failed.");
         if(debugLevel >= 10) {
            util.printMessage("\nWARNING: GSI Proxy has a problem.", 
				logger,debugLevel); 
         }
      }
      else if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
         result.setStatus(estatus.getStatus());
         result.setExplanation("To many pftps, Try later.");
         if(debugLevel >= 10) {
            util.printMessage("\nTo many pftps. Try later.", 
				logger,debugLevel); 
         }
      }
      else if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
         result.setStatus(estatus.getStatus());
         result.setExplanation("HPSS system not available. Try later");
         if(debugLevel >= 10) {
            util.printMessage("\nHPSS system not available. Try later", 
				logger,debugLevel); 
         }
      }
      else if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_REQUEST_DONE) {
         result.setStatus(estatus.getStatus());
         if(result.getExplanation().equals("")) {
           result.setExplanation("MSS listing done.");
         }
         if(debugLevel >= 10) {
            util.printMessage("\nMSS Listing done, successful.", 
				logger,debugLevel); 
         }
      } 
      else if(estatus.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
         result.setStatus(estatus.getStatus());
         result.setExplanation("MSS listing done.");
         if(debugLevel >= 10) {
            util.printMessage("\nMSS Listing done, successful.", 
				logger,debugLevel); 
         }
      }
      else {
         StringBuffer buf = new StringBuffer();
         MSS_MESSAGE sss =      
                SRM_MSS_UTIL.getMSSGetPutError(logftpmssg, debugLevel,buf);
         result.setStatus(sss);
         result.setExplanation("MSS listing done.");
         if(debugLevel >= 10) {
            util.printMessage("\nMSS Listing done.", logger,debugLevel); 
         }
      }
      if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
         if(debugLevel <= 6000) {
           File f = new File(proxyFile); 
           if(f.exists()) {
             f.delete();
           }
         }
      }

      param = new Object[5]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      param[1] = "LOGFILE="+logftpmssg;
      param[2] = "LOGFILE EXISTS="+ff.exists();
      param[3] = "STATUS="+result.getStatus();
      param[4] = "EXPLANATION="+result.getExplanation();
      _theLogger.log(java.util.logging.Level.FINE,
	   "BEFORE_RETURNING_RESULT",(Object[])param);
      return result;
  }catch(Exception e) {
    e.printStackTrace();
    result.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
    result.setExplanation(e.getMessage());
    return result;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssFilePut
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_MSSFILE_STATUS mssFilePut
         (String source, 
          String target, long fileSize,
          SRM_OVERWRITE_MODE overwritemode,
          boolean srmnocipher, boolean srmnonpassivelisting,
    	  SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception  {

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   System.out.println(">>>>Source="+source);
   System.out.println(">>>>Target="+target);

   /*
   //remove extra slashes in the front
   String temp = target;
   int i = 0;
   while(true) {
     if(temp.length() > 0) {
       if(temp.charAt(i) == '/') {
         temp = temp.substring(i+1);
         System.out.println(">>>TEMP="+temp);  
         i++;
       }
       else {
         break;
       }
     }
   }//end while

   target = "//"+temp;

   System.out.println(">>>>Target="+target);
   */

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     target = "gsiftp://"+MSSHost+":"+MSSPort + target;
     source = "file:////"+source;
   }

    SRM_MSSFILE_STATUS status  = super.mssFilePut( source, target, fileSize,
                                  overwritemode, accessInfo,
								  srmnocipher,srmnonpassivelisting,this);

    //added into appropriate queue
    mssPending++;

    util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

    if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue 
      FileObj fObj = getFromQueue();

      if (fObj != null) {
      Object[] param = new Object [2];
      param[0] = fObj.toString();
      param[1] = "RID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE, "SERVING_PUT_REQUEST",
			(Object[]) param);

      //each file transfer is handled by seperate thread.
      //at any given time only MaxAllowed of tasks can happen.
      queueStatusMap.put(fObj.getRequestToken(),fObj);
      ThreadCallBack taskThread = fObj.getTaskThread(); 
      taskThread.start();
      }
    }
    else  { // just added into queue and return
      ; 
    }

    return status; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssPut
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssPut(FileObj fObj, 
	SRM_MSSFILE_STATUS status) throws Exception {

  String ftptransferlog="";
  String requestToken = status.getRequestToken();

  if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
    fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
    fObj.setExplanation("Transfer aborted");
    status.setExplanation("Transfer aborted");
    return;
  }

  if(!logPath.equals("")) {
      /*
     ftptransferlog = logPath+"/"+"srm_mss.put.log." + 
						fObj.getSourcePath() + "." + requestToken +
                       "."+System.currentTimeMillis();
        */
     ftptransferlog = logPath+"/"+"srm_mss." + requestToken;
  }
  else {
       /*
     ftptransferlog = "srm_mss.put.log." + 
						fObj.getSourcePath() + "." + requestToken +
                       "."+System.currentTimeMillis();
        */
     ftptransferlog = "srm_mss." + requestToken;
  }

  if(debugLevel >= 200) {
    util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
  } 

  MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

  long sizeOfFileInMB = 0;
  long counter = 0;
  double transferTime = 0;
  double totalTime = 0;

  SRM_ACCESS_TYPE mssType = fObj.getAccessType();
  SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
  String localFileName = fObj.getSource();
  String remoteFileName = fObj.getTarget();
  long fileSize = fObj.getFileSize();
  String srmmsspath = pftpPath; 
  boolean srmnocipher = fObj.getSRMNoCipher();
  boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();

  String srmmsshost = MSSHost;
  int srmmssport = MSSPort ;
   
  SRM_OVERWRITE_MODE overwritemode = fObj.getOverWriteMode();
  
  try {

    if(debugLevel >= 10) {
      util.printMessage("\nLocal File : " + localFileName,logger,debugLevel);
      util.printMessage("Remote File: " + remoteFileName,logger,debugLevel); 
      util.printMessage("Local File Size : " + fileSize,logger,debugLevel);
      util.printMessage("Thread " + Thread.currentThread().getName() +
		" will archive file.", logger,debugLevel); 
      util.printMessage("\nMSSTYPE : " + mssType,logger,debugLevel);
    }


    String proxyFile = ftptransferlog+".proxy";
    File script=null;
    GSSCredential credential = null;
    boolean gsiTransferOk=false;
    String gsiException="";

    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {

         try {
             writeProxyFile(proxyFile,accessInfo.getPasswd());
             credential = createAndCheckProxyIsValid(proxyFile,true);
         }catch(SRM_MSS_Exception srme) {
            util.printMessage("\nERROR : expired proxy " + proxyFile);
            fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
            fObj.setExplanation("Expired proxy.");
            status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
            status.setExplanation("Expired proxy.");
            return; 
         }catch(Exception ee) {
            util.printMessage("\nERROR : could not write proxy " + proxyFile);
            fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
            fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
            status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
            status.setExplanation(
				"FAILED_TO_WRITE_PROXY to local file system.");
            System.out.println("Exception="+ee.getMessage());
            return; 
         }
   }

    if(overwritemode == SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_NO) {
        //check the existence of remote file
        //
        //SRMLsClient.doList thing will come here.
        MSS_MESSAGE_W_E lsresultpre ;

        if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {

            lsresultpre = mssListing(remoteFileName, accessInfo, 
                                  ftptransferlog, debugLevel,
                                  mssType, srmmsspath,
                                  srmmsshost, srmmssport,
                                  srmnocipher, srmnonpassivelisting,
                                  setscipath,true,fObj,status);

        }
        else {


         Object[] param = new Object[1]; 
         param[0] ="REQUEST-ID="+status.getRequestToken();
         _theLogger.log(java.util.logging.Level.FINE,
	     "WROTE_PROXY_FILE",(Object[])param);

         SRMLsClient srmLsClient = new SRMLsClient(remoteFileName,credential,
                       logger,_theLogger,false,debugLevel);        
         srmLsClient.setMaxWait(maxWait);
         srmLsClient.setWaitDelay(waitDelay);
         lsresultpre = new MSS_MESSAGE_W_E (); 
         srmLsClient.doList
			(remoteFileName,requestToken,true,false,lsresultpre,false);
       }


       if(lsresultpre.getStatus() == 
			MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
          status.setExplanation("Transfer aborted");
          fObj.setExplanation("Transfer aborted");
          return; 
       }

       if(lsresultpre.getStatus() != MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
          util.printMessage
	       ("\nRemote file exists. " +
				"cannot overwrite with no overwrite option.",
			       logger,debugLevel);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
          fObj.setExplanation ("Remote file already exists. " + 
		     "Cannot overwrite with overwrite option no.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
          status.setExplanation ("Remote file already exists. " + 
			"Cannot overwrite with overwrite option no.");
          return; 
       }
    }

    MyISRMFileTransfer tu = null; 

    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {

      try {
        tu = new MySRMFileTransfer (localFileName,remoteFileName);
        tu.setLogger(logger,_theLogger, false, true);
        tu.setTransferMode(SRMTransferMode.PUT);
        tu.setParallel(parallelism);
        tu.setCredentials (credential);
        tu.setTransferType(SRMTransferProtocol.GSIFTP);
        tu.setSessionType(1);
        tu.setSessionMode(1);
        tu.setBufferSize(bufferSize);
        if(dcau) {
           tu.setDCAU(dcau);
        }
        tu.start();
        boolean ok = false;
        Thread t = new Thread();
        while(!ok) {
          t.sleep(5000);
          if(tu.transferDone()) {
             ok = true;
             gsiTransferOk=true;
          }
          if(tu.getStatus() != null) {
             ok=true;
             gsiException=tu.getStatus();
             gsiTransferOk=false;
          }
        }
      }catch(Exception ee) {
        util.printMessage("Exception="+ee.getMessage());
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > "+ ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      writer.println("user " + accessInfo.getLogin() + " " + 
		accessInfo.getPasswd());
      writer.println("bin");
      writer.println("pput " + localFileName + " " + remoteFileName);
      writer.println("bye");
      writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n" ,logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        util.printMessage("user " + accessInfo.getLogin() + "   " + 
                    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage("pput " + localFileName + " "+ 
			remoteFileName,logger,debugLevel);
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
	  mypasswd == null || mypasswd.length() == 0) {
        if(debugLevel >= 10) { 
          util.printMessage("\nWARNING: Either userid or password wrong.");
        }
        status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setExplanation("Either userid or password wrong.");
        status.setExplanation("Either userid or password wrong.");
        return;
      }

      if(!srmnocipher) {
         //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
         //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
         mylogin = accessInfo.getLogin();
         mypasswd = accessInfo.getPasswd();
      }

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      if(srmnonpassivelisting) {
         writer.println("passive");
      }
      writer.println("user " + mylogin + " " +  mypasswd);
	  writer.println("bin");
	  writer.println("pput " + localFileName + " " + remoteFileName);
	  writer.println("bye");
	  writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n" , logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        if(srmnonpassivelisting) {
           util.printMessage("passive\n",logger,debugLevel);
        }
        util.printMessage("user " + mylogin + "   " + 
                    mypasswd + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage("pput " + localFileName + " "+ 
			remoteFileName,logger,debugLevel);
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) {
      //writeLogFile(ftptransferlog,"opening logfile");
      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println (hsiPath + " -q \"out " + ftptransferlog + ";" +
            //"pwd;"+
			" put " + localFileName + " : " + remoteFileName +" ; end\"");
      writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog);
      writer.println("echo \"DATE=`date`\" >> " + ftptransferlog);
      writer.println("ls -l " + localFileName + " >> " + ftptransferlog);
      writer.close();

      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage (hsiPath + " -q \"out " + ftptransferlog + ";" +
            //"pwd;"+ 
			" put " + localFileName + " : " + remoteFileName +" ; end\"",
					logger,debugLevel);
        util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog,
                    logger,debugLevel);
        util.printMessage("echo \"DATE=`date`\" >> " + ftptransferlog,
                    logger,debugLevel);
        util.printMessage("ls -l " + localFileName + " >> " + ftptransferlog, 
				logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
  }
  else {
      util.printMessage("\nHPSS TYPE not implemented : " + mssType);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("Unknown MSS Type");
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("Unknown MSS Type");
      return;
  }

    boolean useEnvp = false;
    long startTime = System.currentTimeMillis(); 
    //util.printMessage("\nRemoteFileName : " + remoteFileName, logger);
    util.printMessage("\nARCHIVE_START", logger);
    long endTime=0;

    if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      boolean ok = false;
      Thread t = new Thread();
      while(!ok) {
       try {
        if(script.exists()) {
          Process p0 = 
		    Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
          if(p0.waitFor() == 0) {
	        p0.destroy();
            if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
              Object[] param = new Object[1]; 
              param[0] ="REQUEST-ID="+status.getRequestToken();
              _theLogger.log(java.util.logging.Level.FINE,
				  "NO_NEED_TO_EXECUTE_SCRIPT_TRANSFER_ABORTED",(Object[])param);
              fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
              status.setExplanation("Transfer aborted");
              fObj.setExplanation("Transfer aborted");
              return; 
            }
            else {
              Object[] param = new Object[1]; 
              param[0] ="REQUEST-ID"+status.getRequestToken();
              _theLogger.log(java.util.logging.Level.FINE,
				  "GOING_TO_EXECUTE_SCRIPT",(Object[])param);
              ExecScript process = 
	  	        new ExecScript(script.getAbsolutePath(), 
	               this.javaLibraryPath, true,this,_theLogger);
              process.setLogFile(ftptransferlog);
              status.setCurrentProcess(process); 
              process.execCommand(useEnvp);
            }
			ok = true;
          }
         }
       }catch(IOException ioe) {
         ok = false;
         t.sleep(1000);
       }catch(Exception ie) {
         ok = true;
         ie.printStackTrace();
       }
     }

     endTime = System.currentTimeMillis(); 


     try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.");
          util.printMessage("log file is empty " + ftptransferlog);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation("ERROR while executing script.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation("ERROR while executing script.");
        }
    }catch(Exception e) {}
  

     //seems like the script runs successfully.
     //but there may be other problems such as login, file not found etc.
     //need to parse the transfer log to find out the details

     //do you think we need to check file exists or not etc.
     //I am not sure we will think about this later.

     //assume that file  really exists.

     //if file did not exists.

     Object[] param = new Object[1]; 
     param[0] ="REQUEST-ID="+status.getRequestToken();
     _theLogger.log(java.util.logging.Level.FINE,
		  "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
   }//(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI)

    try {

     double remoteFileSize=0;

     if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
        if(gsiException.equals("")) {
         SRMLsClient srmLsClient = new SRMLsClient(remoteFileName,credential,
                       logger,_theLogger,false,debugLevel);        
         srmLsClient.setMaxWait(maxWait);
         srmLsClient.setWaitDelay(waitDelay);
         MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E (); 
         remoteFileSize = srmLsClient.getRemoteFileSize(mssresult);
         if(mssresult.getStatus() != MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
          gsiException = mssresult.getExplanation();
          if(debugLevel >= 4000) {
             util.printMessage("GsiException="+gsiException);
          }
         }
        }
     }
     else {
       remoteFileSize = 
	      SRM_MSS_UTIL.getMSSTransferBytes (ftptransferlog);
     }
     if(debugLevel >= 100) {
       util.printMessage("REMOTEFILE SIZE " + remoteFileSize, 
			logger,debugLevel);
     }
     if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
       if(gsiException.equals("") && tu != null) {
         transferTime = tu.getTransferTimeInMilliSeconds();
       }
     }
     else {
       transferTime = SRM_MSS_UTIL.getMSSTransferTime(ftptransferlog);
     }
     MSS_MESSAGE_W_E pftpmssg_e = new MSS_MESSAGE_W_E ();
     if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
	   SRM_MSS_UTIL.getMSSGSIError(gsiException, pftpmssg_e, debugLevel);
           pftpmssg = pftpmssg_e.getStatus();
     }
     else {
           StringBuffer buf = new StringBuffer();
	   pftpmssg = 
                SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog, debugLevel,buf);
           File ff = new File (ftptransferlog);
           Object[] param = new Object[5]; 
           param[0] = "REQUEST-ID="+fObj.getRequestToken();
           param[1] = "LOGFILE="+ftptransferlog;
           param[2] = "LOGFILE exists="+ff.exists();
           param[3] = "STATUS="+pftpmssg;
           param[4] = "EXPLANATION="+buf.toString();
           _theLogger.log(java.util.logging.Level.FINE,
	    "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
     }

     if(debugLevel >= 4000) {
        System.out.println(">>>>pftpmssg="+pftpmssg);
     }
     if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
        pftpmssg == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) { 
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        srmPingStatus.setLastAccessedTime(""+new Date());
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        fObj.setExplanation("MSS Down.");
     }
     else {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        srmPingStatus.setLastAccessedTime(""+new Date());
        //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        //fObj.setExplanation("MSS Up.");
     }
     //fileSize cannot be zero.
     if(fileSize != remoteFileSize) {
       //file was not transferred correctly. 
       //should I care about that.
       if(remoteFileSize != 0) {
         if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
           StringBuffer buf = new StringBuffer();
           pftpmssg = 
              SRM_MSS_UTIL.getMSSGetPutError(ftptransferlog, debugLevel,buf);
	        if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) { 
				//partial transfer
             if(debugLevel >= 10) {
               util.printMessage("\nFile " + localFileName + 
	            " was not transfered correctly",logger,debugLevel);
               util.printMessage("to remote file " + remoteFileName, 
					logger,debugLevel);
               util.printMessage("transferTime="+transferTime, 
					logger,debugLevel);
               util.printMessage("size expected="+fileSize, logger,debugLevel);
               util.printMessage("size transfered="+remoteFileSize,
					logger,debugLevel);
             }
         }
         else { 
           if(debugLevel >= 10) {
              util.printMessage("\nFile " + localFileName +  
	          " was not transfered.", logger,debugLevel);
           }
         }
        }//accesstype != GSI 
        else {
        
          if(gsiTransferOk) {
             pftpmssg_e = new MSS_MESSAGE_W_E ();
	         SRM_MSS_UTIL.getMSSGSIError(gsiException, pftpmssg_e, debugLevel);
             pftpmssg = pftpmssg_e.getStatus();
			 //partial transfer
             if(debugLevel >= 10) {
               util.printMessage("\nFile " + localFileName + 
	            " was not transfered correctly",logger,debugLevel);
               util.printMessage("to remote file " + remoteFileName, 
					logger,debugLevel);
               util.printMessage("transferTime="+transferTime, 
					logger,debugLevel);
               util.printMessage("size expected="+fileSize, logger,debugLevel);
               util.printMessage("size transfered="+remoteFileSize,
					logger,debugLevel);
             }
          }
          else {
            if(debugLevel >= 10) {
                util.printMessage("\nFile " + localFileName +  
	             " was not transfered.", logger,debugLevel);
            }
          }
        }//end else
       }//(remoteFileSize != 0)
       else if(remoteFileSize == 0 || remoteFileSize == -1) { 
         //remoteFileSize == -1 is added for the GSI access SRMLsClient
         //file was not transfered at all
         if(debugLevel >= 10) {
            util.printMessage("\nFile " + localFileName +  
	       " was not transfered.", logger,debugLevel);
         }

         if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
            status.setTransferRate(0.0);
            if(debugLevel >= 10) {
              util.printMessage ("\nFile path does not exist in HPSS",
					logger,debugLevel); 
              util.printMessage("HPSS Path " + remoteFileName,
					logger,debugLevel);
            }
         } 
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
            status.setTransferRate(0.0);
            if(debugLevel >= 10) {
              util.printMessage("\nFile has no write permission " +
                   "in HPSS", logger,debugLevel);
              util.printMessage("HPSS Path " + remoteFileName,
				logger,debugLevel);
            }
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
            status.setTransferRate(0.0);
            if(debugLevel >= 10) { 
              util.printMessage
                ("\nWARNING : GSI proxy has a problem, or either"+
	        " userid or password are WRONG or No permission.", logger,debugLevel);
            }
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {
            status.setTransferRate(0.0);
            if(debugLevel >= 10) { 
              util.printMessage
                ("\nWARNING : Given path is a directory existing in HPSS.",
	                    logger,debugLevel);
            }
         }
         else if((pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) ||
            (pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)) {
              status.setTransferRate(0.0);
              util.printMessage("\nHPSS Path "+ remoteFileName,
				logger,debugLevel);
              util.printMessage(pftpmssg.toString()+  
				   " will try again", logger,debugLevel); 
              fObj.setFTPTransferLog(ftptransferlog);
              fObj.setExplanation("Will retry again"); 
              fObj.incrementNumRetry();
              fObj.setTimeStamp(new GregorianCalendar());
              fObj.setRetryTime(MSSWaitTime*2);
              if(fObj.getNumRetry() < getMSSMaxRetrial()) {
                Object _parent = fObj.getTaskThread().getParent();
                //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY); 
                ThreadCallBack taskThread = 
	             new ThreadCallBack((SRM_MSS)_parent);
                taskThread.setCommand(fObj,status);
                fObj.setTaskThread(taskThread);

                if(monitorThread == null) {
                   threadDisable = false;
                   monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
                   monitorThread = 
					new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
                   monitorThread.start();
                }

                Object[] param = new Object[2]; 
                param[0] = "SOURCE="+fObj; 
                param[1] ="REQUEST-ID"+status.getRequestToken();
                _theLogger.log(java.util.logging.Level.FINE,
				  "PUT_RETRY",(Object[])param);

                archiveQueue.add(fObj);
              }
              else {
                util.printMessage("\nrid=" + fObj.getRequestToken() + 
		              " failed.",logger,debugLevel);
                status.setStatus(pftpmssg);
                fObj.setPFTPStatus(pftpmssg); 
                fObj.setStatus(pftpmssg); 
              }
         }
         else {
             status.setTransferRate(0.0);
             if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
               StringBuffer buf = new StringBuffer();
               pftpmssg = SRM_MSS_UTIL.getMSSGetPutError
                        (ftptransferlog, debugLevel,buf);
               //check one more time to make sure the tarsfer is complete
               if(debugLevel >= 10) { 
                  util.printMessage ("\n"+pftpmssg.toString(),
					logger,debugLevel);
               }
			 }
             else {
               pftpmssg_e = new MSS_MESSAGE_W_E ();
	           SRM_MSS_UTIL.getMSSGSIError(gsiException, 
					pftpmssg_e, debugLevel);
               pftpmssg = pftpmssg_e.getStatus();
               if(debugLevel >= 10) {
                 util.printMessage("\nGSITransferOk="+
					gsiTransferOk,logger,debugLevel);
               }
             }
         }
       }
    } 
    else { 
      //some times there may be a remote file existing with the same name
      // and that time, we cannot simply says transfer done.
      //we have to check pftpmssg also.
      if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
        StringBuffer buf = new StringBuffer();
        pftpmssg = SRM_MSS_UTIL.getMSSGetPutError
                (ftptransferlog, debugLevel,buf);
      } 
      else {
        pftpmssg_e = new MSS_MESSAGE_W_E ();
        SRM_MSS_UTIL.getMSSGSIError(gsiException, pftpmssg_e, debugLevel);
        pftpmssg = pftpmssg_e.getStatus();
      }
    }


   fObj.setPFTPStatus(pftpmssg);

   if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
      util.printMessage("\nARCHIVED.",logger);
      //successful transfer
      if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
        /*
        if(debugLevel <= 200) {
         try {
           File ftplog = new File(ftptransferlog);
           if(ftplog.exists()) {
              ftplog.delete();
           }
         }catch(Exception e) {}
        }
        */
        if(debugLevel <= 6000) {
         try {
           File ftplog = new File(proxyFile);
           if(ftplog.exists()) {
              ftplog.delete();
           }
          }catch(Exception e) {}
        }
      }

      //util.printMessage("\nTRANSFERTIME="+ transferTime,logger);
  
      sizeOfFileInMB=(long)fileSize/MEGABYTE;
      //convert bytes to MB

       totalTime=endTime-startTime;  
       // if this happens then most likely 
       // the system failed to open
       // msstransferlog. to make results reasonable 
       //assume the following...

       if(transferTime < 1) {
        transferTime = totalTime;
       }
       status.setTransferRate((double)sizeOfFileInMB/transferTime);
       status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
       status.setExplanation("ARCHIVED");
       fObj.setExplanation("File is written successfully.");
       status.setSize((long)remoteFileSize);
   } 
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
       status.setExplanation("No such path on HPSS.");
       fObj.setExplanation("No such path on HPSS.");
       util.printMessage("\nNo such path on HPSS",logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED); 
       status.setExplanation ("user not authorized and FILE_CANNOT_BE_READ");
       fObj.setExplanation ("user not authorized and FILE_CANNOT_BE_READ");
       util.printMessage("\nFILE_CANNOT_BE_READ",logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
       status.setTransferRate(0.0);
       //status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED);
       status.setExplanation ("TOO_MANY_PFTPS");
       fObj.setExplanation ("TOO_MANY_PFTPS");
       util.printMessage("\ntoo many mss connections, try again.", logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
       status.setTransferRate(0.0);
       status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
       status.setExplanation ("SRM_MSS_MSS_ERROR");
       fObj.setExplanation ("SRM_MSS_MSS_ERROR");
       util.printMessage("\nSRM_MSS_MSS_ERROR, postpone.", logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
      status.setTransferRate(0.0);
      //status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
      status.setExplanation ("MSS down. try again later.");
      fObj.setExplanation ("MSS down. try again later.");
      util.printMessage("\nMSS down. try again later.", logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
      status.setTransferRate(0.0);
      status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
      status.setExplanation ("No Permission.");
      fObj.setExplanation ("No Permission.");
      util.printMessage("\nclient authentication failed", logger,debugLevel);
   }
   else if(pftpmssg == MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY) {
      status.setTransferRate(0.0);
      status.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
      status.setExplanation ("Given path is a directory existing in HPSS.");
      fObj.setExplanation ("Given path is a directory existing in HPSS.");
   }
   else {
      status.setTransferRate(0.0);
      status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
      status.setExplanation ("unknown errors");
      fObj.setExplanation ("unknown errors");
      util.printMessage("\nrequest failed with other errors",logger,debugLevel);
   }
  }catch(Exception e) {
     status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
     status.setExplanation("POSTPONE due to unknown error");
     fObj.setExplanation("POSTPONE due to unknown error");
     util.printMessage("\nPOSTPONE due to unknown error",logger,debugLevel);
  }
 }catch(IOException ioe) {
   ioe.printStackTrace();
 } 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmCopy
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_STATUS srmCopy (
          String sourcePath, String targetPath, 
          SRM_OVERWRITE_MODE  overwritemode,
          boolean recursive, 
		  boolean srmnocipher, boolean srmnonpassivelisting,
		  SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception  {

   throw new Exception("Not supported for HPSS.");

   /*
   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   SRM_STATUS status  = super.srmCopy( sourcePath, targetPath,
				          overwritemode, recursive,accessInfo, 
						  srmnocipher, srmnonpassivelisting, this);

   //added into appropriate queue
   mssPending++;

   util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

   if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue();

       if(fObj != null) {
       Object[] param = new Object [2];
       param[0] = fObj.toString();
       param[1] = "RID="+fObj.getRequestToken();
       _theLogger.log(java.util.logging.Level.FINE, "SERVING_COPY_REQUEST",
         (Object[]) param);

       //each file transfer is handled by seperate thread.
       //at any given time only MaxAllowed of tasks can happen.
       queueStatusMap.put(fObj.getRequestToken(),fObj);
       ThreadCallBack taskThread = fObj.getTaskThread(); 
       taskThread.start();
       }
   }
   else  { // just added into queue and return
     ; 
   }

   return status;
   */
   
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// srmDelete
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_STATUS srmDelete (
          String path, 
		  SRM_ACCESS_INFO accessInfo, 
          boolean isDir,
		  boolean recursive,
	      boolean srmnoncipher, boolean srmnonpassivelisting) 
		    throws SRM_MSS_Exception, Exception  {

   /*
   if(recursive) {
      throw new Exception("Not supported for HPSS.");
   }
   */

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     path = "gsiftp://"+MSSHost+":"+MSSPort + path;
   }

    SRM_STATUS status  = super.srmDelete( path, accessInfo, 
				isDir, recursive, srmnoncipher, srmnonpassivelisting, this);

    //added into appropriate queue
    mssPending++;

    util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

    if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
     FileObj fObj = getFromQueue();

      if(fObj != null) {
      Object[] param = new Object [2];
      param[0] = fObj.toString();
      param[1] = "RID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE, "SERVING_DELETE_REQUEST",
			(Object[]) param);

      //each file transfer is handled by seperate thread.
      //at any given time only MaxAllowed of tasks can happen.
      queueStatusMap.put(fObj.getRequestToken(),fObj);
      ThreadCallBack taskThread = fObj.getTaskThread(); 
      taskThread.start();
      }
    }
    else  { // just added into queue and return
      ; 
    }

    return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// abortRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean abortRequest(String rid) throws Exception {
    return super.abortRequest(rid);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssGetHomeDir
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_STATUS  mssGetHomeDir (SRM_ACCESS_INFO accessInfo) 
	throws SRM_MSS_Exception, Exception  {

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   String path = "gsiftp://"+MSSHost+":"+MSSPort;
   SRM_STATUS status  = super.mssGetHomeDir(path,accessInfo,this);

   //added into appropriate queue
   mssPending++;

   util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

   if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue();

      if(fObj != null) {
        Object[] param = new Object [2];
        param[0] = fObj.toString();
        param[1] = "RID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE, "SERVING_GETHOME_REQUEST",
			(Object[]) param);

        //each file transfer is handled by seperate thread.
        //at any given time only MaxAllowed of tasks can happen.
        queueStatusMap.put(fObj.getRequestToken(),fObj);
        ThreadCallBack taskThread = fObj.getTaskThread(); 
        taskThread.start();
      }
   }
   else  { // just added into queue and return
     ; 
   }

   return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssGetHomeDir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssGetHomeDir(FileObj fObj, 
	SRM_STATUS status) throws Exception {

  String ftptransferlog="";
  String requestToken = status.getRequestToken();

  if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
    fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
    status.setExplanation("Transfer aborted");
    fObj.setExplanation("Transfer aborted");
    return;
  }

  if(!logPath.equals("")) {
    /*
    ftptransferlog = logPath+"/"+"srm_mss.gethomedir.log." + 
					requestToken + "."+System.currentTimeMillis();
    */
    ftptransferlog = logPath+"/"+"srm_mss.gethomedir.log." + requestToken; 
  } 
  else {
    /*
    ftptransferlog = "srm_mss.gethomedir.log." + 
					requestToken + "."+System.currentTimeMillis();
    */
    ftptransferlog = "srm_mss.gethomedir.log." + requestToken; 
  }

  SRM_ACCESS_TYPE mssType = fObj.getAccessType();
  SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
  String srmmsspath = pftpPath; 
  boolean srmnocipher = fObj.getSRMNoCipher();
  String path = fObj.getFilePath();
  boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
  String srmmsshost = MSSHost;
  int srmmssport = MSSPort;

  if(debugLevel >= 200 && mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
    util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
  } 

  MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

  long sizeOfFileInMB = 0;
  long counter = 0;
  double transferTime = 0;
  double totalTime = 0;
  MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E(); 

  try {

    if(debugLevel >= 10) {
     
      util.printMessage("\nThread " + Thread.currentThread().getName() +
		" will perform gethomedir.", logger,debugLevel); 
      util.printMessage("\nMSSTYPE : " + mssType,logger,debugLevel);
    }

    String proxyFile = ftptransferlog+".proxy";
    File script=null;

    GSSCredential credential = null;
    String homeDir="";

    long size=0;
    //without * in the filename
    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
        SRMLsClient srmLsClient = new SRMLsClient(path,credential, 
			logger,_theLogger,false,debugLevel);
        srmLsClient.setMaxWait(maxWait);
        srmLsClient.setWaitDelay(waitDelay);
        mssresult = new MSS_MESSAGE_W_E (); 
        homeDir = srmLsClient.getHomeDir(mssresult);
      }catch(SRM_MSS_Exception srme) {
        util.printMessage("\nERROR : expired proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy.");
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        mssresult.setExplanation("Expired proxy.");
        return; 
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        mssresult.setExplanation("Unknown Error.");
        return; 
     }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      writer.println("user " + accessInfo.getLogin() + " " + 
		accessInfo.getPasswd());
      writer.println("bin");
      writer.println("pwd");
      writer.println("bye");
      writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        util.printMessage("user " + accessInfo.getLogin() + "   " + 
                    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage("pwd" , logger,debugLevel);
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
	  mypasswd == null || mypasswd.length() == 0) {
        if(debugLevel >= 10) { 
          util.printMessage("WARNING: Either userid or password wrong.");
        }
        status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setExplanation("Either userid or password wrong.");
        status.setExplanation("Either userid or password wrong.");
        return;
      }

      if(!srmnocipher) {
         //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
         //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
         mylogin = accessInfo.getLogin();
         mypasswd = accessInfo.getPasswd();
      }

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      if(srmnonpassivelisting) {
         writer.println("passive");
      }
      writer.println("user " + mylogin + " " +  mypasswd);
      writer.println("bin");
      writer.println("pwd");
      writer.println("bye");
      writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        if(srmnonpassivelisting) {
           util.printMessage("passive\n",logger,debugLevel);
        }
        util.printMessage("user " + mylogin + "   " + 
                    mypasswd + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage("pwd",logger,debugLevel); 
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) {
      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println (hsiPath + " -q " + "\"out " + ftptransferlog +
				//";pwd;end\"");
				";end\"");
      writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog);
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
			logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage (hsiPath + 
	        //" -q "+ "\" out " + ftptransferlog + ";pwd;end\"", 
	        " -q "+ "\" out " + ftptransferlog + ";end\"", 
			logger,debugLevel);
        util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog, 
			logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
  }
  else {
      util.printMessage("HPSS TYPE not implemented : " + mssType);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("Unknown MSS Type");
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("Unknown MSS Type");
      return;
  }

  if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {

    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
     "WROTE_SCRIPT_FILE",(Object[])param);

    boolean useEnvp = false;
    long startTime = System.currentTimeMillis(); 

    boolean ok = false;
    Thread t = new Thread();

    while (!ok) {
      try {
         if(script.exists()) {
           Process p0 = 
		     Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
           if(p0.waitFor() == 0) {
	         p0.destroy();
             if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
                param = new Object[1]; 
                param[0] = "REQUEST-ID="+fObj.getRequestToken();
                _theLogger.log(java.util.logging.Level.FINE,
                "NO_NEED_TO_EXECUTE_SCRIPT_TRANSFER_ABORTED",(Object[])param);
                fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
                status.setExplanation("Transfer aborted");
                fObj.setExplanation("Transfer aborted");
                return; 
             }
             else {
                param = new Object[1]; 
                param[0] = "REQUEST-ID="+fObj.getRequestToken();
                _theLogger.log(java.util.logging.Level.FINE,
                "GOING_TO_EXECUTE_SCRIPT_FILE",(Object[])param);
                ExecScript process = 
		         new ExecScript(script.getAbsolutePath(), 
	               this.javaLibraryPath, true,this,_theLogger);
                process.setLogFile(ftptransferlog);
                status.setCurrentProcess(process); 
                process.execCommand(useEnvp);
             }
             ok = true;
           }
        }
      }catch(IOException ioe) {
         ok = false;
         t.sleep(1000);
      }catch(Exception ie) {
         ok = true;
         ie.printStackTrace(); 
      }
    }

    long endTime = System.currentTimeMillis(); 

    try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.");
          util.printMessage("log file is empty " + ftptransferlog);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation("ERROR while executing script.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation("ERROR while executing script.");
        }
    }catch(Exception e) {}


    /*
    if(debugLevel < 3000) {
      try {
        if(script.exists()) {
        script.delete();
        }
      }catch(Exception ioe) {
       util.printMessage("Exception " + ioe.getMessage());
      }
    }
    */

    //seems like the script runs successfully.
    //but there may be other problems such as login, file not found etc.
    //need to parse the transfer log to find out the details

    //do you think we need to check file exists or not etc.
    //I am not sure we will think about this later.

    //assume that file  really exists.

    //if file did not exists.

    param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
      "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

    try {

      MSS_MESSAGE_W_E pftpmssg_e =  new MSS_MESSAGE_W_E ();
	  SRM_MSS_UTIL.getMSSError(ftptransferlog, pftpmssg_e,debugLevel);
      pftpmssg = pftpmssg_e.getStatus();
     }catch(Exception e) {
        e.printStackTrace();
     }
   }
   else {
     pftpmssg = mssresult.getStatus();
   }

         File ff = new File (ftptransferlog);
         Object[] param = new Object[4]; 
         param[0] = "REQUEST-ID="+fObj.getRequestToken();
         param[1] = "LOGFILE="+ftptransferlog;
         param[2] = "LOGFILE exists="+ff.exists();
         param[3] = "STATUS="+pftpmssg;
         _theLogger.log(java.util.logging.Level.FINE,
	   "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);

   if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
     pftpmssg == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) { 
     srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
     srmPingStatus.setLastAccessedTime(""+new Date());
     fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
     fObj.setExplanation("MSS Down.");
   }
   else {
     srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
     srmPingStatus.setLastAccessedTime(""+new Date());
     //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
     //fObj.setExplanation("MSS Up.");
   }

   fObj.setPFTPStatus(pftpmssg);

   if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) { 
 
         util.printMessage("\nGet Homedir info failed due to " +
		    "MSS down and will try again", logger,debugLevel);

         fObj.setExplanation("Will retry again");
         fObj.setFTPTransferLog(ftptransferlog);
         fObj.incrementNumRetry();
         fObj.setTimeStamp(new GregorianCalendar());
         fObj.setRetryTime(MSSWaitTime*2);

         if(fObj.getNumRetry() < getMSSMaxRetrial()) {
            Object _parent = fObj.getTaskThread().getParent();
            //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
            ThreadCallBack taskThread = 
                 new ThreadCallBack((SRM_MSS)_parent);
            taskThread.setCommand(fObj,status);
            fObj.setTaskThread(taskThread);

            if(monitorThread == null) {
              threadDisable = false;
              monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
              monitorThread = 
				new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
              monitorThread.start();
            }

            param = new Object[2]; 
            param[0] = "SOURCE="+fObj; 
            param[1] = "REQUEST-ID="+status.getRequestToken(); 
            _theLogger.log(java.util.logging.Level.FINE,
				  "GETHOMEDIR_RETRY",(Object[])param);
            othersQueue.add(fObj);
         }
         else {
            util.printMessage("\nrid=" + fObj.getRequestToken() + 
		       " failed.",logger,debugLevel);
            fObj.setPFTPStatus(pftpmssg);
            status.setStatus(pftpmssg);
            fObj.setPFTPStatus(pftpmssg);
         }
         if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
           status.setExplanation("MSS access limit reached, Try again later.");
           fObj.setExplanation("MSS access limit reached, Try again later.");
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
           status.setExplanation("MSS not available.");
           fObj.setExplanation("MSS not available.");
         }
      }

      if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {

        if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
        
         //need to parse the output file
         FileInputStream fis = new FileInputStream(ftptransferlog); 
         BufferedReader bis = new BufferedReader( new InputStreamReader(fis));
         String ref="";
         while((ref= bis.readLine())!= null)  {
            if((ref.trim().equals("\n")) ||
                (ref.trim().equals("\r"))) continue;
            int idx = ref.indexOf("is current directory");
            if(idx != -1) {
              int idx1 = ref.indexOf("\""); 
              if(idx1 != -1) {
                int idx2 = ref.lastIndexOf("\"");
                if(idx2 != -1) { 
                  String currentDir = ref.substring(idx1+1,idx2);
                  if(debugLevel >= 300) {
                    util.printMessage("\nPWD="+currentDir, logger,debugLevel);
                  }
                  status.setSourcePath(currentDir);
                }
                else { 
                  System.out.println("\ncould not see a end \"" +
			  	    "is current directory"); 
                  util.printMessage("\ncould not see a end \"" +
			  	    "is current directory",logger,debugLevel); 
                }
              }
              else {
                System.out.println("\ncould not see a begin \"" +
			  	  "is current directory"); 
                util.printMessage("\ncould not see a begin \"" +
			  	  "is current directory",logger,debugLevel); 
              }
            }
         }
         util.printMessage("\nMSSGETHOMEDIR : STATUS=TRANSFER_DONE", 
			logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
         status.setExplanation("GetHome Dir done successfully.");
         fObj.setExplanation("Get Home Dir done successfully.");

         /*
         if(debugLevel <= 200) { 
           File f = new File(ftptransferlog);
           if(f.exists()) { f.delete(); }
         }
         */
         if(debugLevel <= 6000) { 
          File f = new File(proxyFile);
           if(f.exists()) { f.delete(); }
         }
        }
        else {
           status.setSourcePath(homeDir);
           util.printMessage("\nMSSGETHOMEDIR : STATUS=TRANSFER_DONE", 
			logger,debugLevel);
           status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
           status.setExplanation("GetHome Dir done successfully.");
           fObj.setExplanation("Get Home Dir done successfully.");
        }
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         util.printMessage("\nGETHOMEDIR : STATUS=REQUEST_FAILED", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
         status.setExplanation("No access permission");
         fObj.setExplanation("No access permission");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
         util.printMessage("\nMSS limit reached.", logger,debugLevel);
         status.setExplanation("mss limit reached");
         fObj.setExplanation("mss limit reached");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
         util.printMessage("\nMSS system not available.", logger,debugLevel);
         status.setExplanation("mss not available");
         fObj.setExplanation("mss not available");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
         util.printMessage("\nSRM MSS Error", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
         status.setExplanation("MSS error");
         fObj.setExplanation("MSS error");
      }
      else {
         util.printMessage("\n"+pftpmssg.toString(), logger,debugLevel);
         status.setStatus(pftpmssg);
         status.setExplanation(pftpmssg.toString());
         fObj.setExplanation(pftpmssg.toString());
      }
  }catch(IOException ioe) {
    ioe.printStackTrace();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mssMakeDirectory
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_STATUS  mssMakeDirectory
         (SRM_PATH dirs, 
          boolean srmnocipher,boolean srmnonpassivelisting,
    	  SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception  {

   if(!_initialized) { 
     throw new SRM_MSS_Exception
		("Please initialize first before calling this method.");
   }

   if(accessType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
     dirs.setDir("gsiftp://"+MSSHost+":"+MSSPort + dirs.getDir());
   }

   SRM_STATUS status  = super.mssMakeDirectory(dirs,
	                      accessInfo,srmnocipher,srmnonpassivelisting,this);

    //added into appropriate queue
    mssPending++;

    util.printMessage("\nMSSPending files : " + mssPending + 
			"   MSSMaxAllowed : " + MSSMaxAllowed, logger,debugLevel);

    if(mssPending <= MSSMaxAllowed) { 
      //check queue turn and get from queue
      FileObj fObj = getFromQueue();

      if(fObj != null) {
        Object[] param = new Object [2];
        param[0] = fObj.toString();
        param[1] = "RID="+fObj.getRequestToken();
        _theLogger.log(java.util.logging.Level.FINE, "SERVING_MKDIR_REQUEST",
			(Object[]) param);

        //each file transfer is handled by seperate thread.
        //at any given time only MaxAllowed of tasks can happen.
        queueStatusMap.put(fObj.getRequestToken(),fObj);
        ThreadCallBack taskThread = fObj.getTaskThread(); 
        taskThread.start();
      }
    }
    else  { // just added into queue and return
      ; 
    }

    return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// makeDirectoryRecursive
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void makeDirectoryRecursive
	(SRM_PATH subDir, StringBuffer dirNames,Vector dirs) {

  if(subDir == null || subDir.getDir() == null || subDir.getDir().equals("")) 
    return;

  dirNames.append("mkdir " + subDir.getDir() + "\n");
  dirs.addElement(subDir.getDir());
  Vector vec = subDir.getSubPath();
  for(int i = 0; i < vec.size(); i++) {
    SRM_PATH subSubDir = (SRM_PATH) vec.elementAt(i);
    makeDirectoryRecursive(subSubDir, dirNames,dirs);
  }
  return;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mkDir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void printSubDir(SRM_PATH subDir) {
  util.printMessage("SubDir : " + subDir.getDir(), logger,debugLevel);
  Vector vec = subDir.getSubPath();
  if(vec.size() > 0) {
    for(int i = 0; i < vec.size(); i++) {
      SRM_PATH subSubDir = (SRM_PATH) vec.elementAt(i);
      printSubDir(subSubDir);
    }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// mkDir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void mssMkDir(FileObj fObj, 
	SRM_STATUS status) throws Exception {

  String ftptransferlog="";
  String requestToken = status.getRequestToken();

  if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
    fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
    status.setExplanation("Transfer aborted");
    fObj.setExplanation("Transfer aborted");
    return;
  }

  if(!logPath.equals("")) {
    /*
    ftptransferlog = logPath+"/"+"srm_mss.mkdir.log." + 
					fObj.getSourcePath() + "." + requestToken + 
                       "."+System.currentTimeMillis();
    */
    ftptransferlog = logPath+"/"+"srm_mss." + requestToken; 
  } 
  else {
    /*
    ftptransferlog = "srm_mss.mkdir.log." + 
					fObj.getSourcePath() + "." + requestToken + 
                       "."+System.currentTimeMillis();
    */
    ftptransferlog = "srm_mss." + requestToken; 
  }

  if(debugLevel >= 200) {
    util.printMessage("\nMY LOG PATH : " + ftptransferlog,logger,debugLevel);
  } 

  MSS_MESSAGE pftpmssg = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

  long sizeOfFileInMB = 0;
  long counter = 0;
  double transferTime = 0;
  double totalTime = 0;

  SRM_ACCESS_TYPE mssType = fObj.getAccessType();
  SRM_ACCESS_INFO accessInfo =  fObj.getAccessInfo();
  String srmmsspath = pftpPath; 
  boolean srmnocipher = fObj.getSRMNoCipher();
  boolean srmnonpassivelisting = fObj.getSRMNonPassiveListing();
  String srmmsshost = MSSHost;
  int srmmssport = MSSPort;
  SRM_PATH dirs  = fObj.getDirs();

  
  try {

    Vector vec = dirs.getSubPath();

    if(debugLevel >= 10) {
     
      util.printMessage("\nInput Dir to be created : " + 
			dirs.getDir(), logger,debugLevel); 
      util.printMessage("SubDir size : " + vec.size(), logger,debugLevel);
      if(vec.size() > 0) {
        for(int i = 0; i < vec.size(); i++) {
           SRM_PATH subDir = (SRM_PATH) vec.elementAt(i);
           printSubDir(subDir);
        }
      }
      util.printMessage("\nThread " + Thread.currentThread().getName() +
		" will perform mkdir.", logger,debugLevel); 
      util.printMessage("\nMSSTYPE : " + mssType,logger,debugLevel);
    }

    if(dirs.getDir() == null || dirs.getDir().equals("")) {
      status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED); 
      status.setExplanation("input dir path is null");
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
      fObj.setExplanation("input dir path is null");
    }

    StringBuffer dirNames = new StringBuffer();
    dirNames.append("mkdir " + dirs.getDir() + "\n");
    Vector ddirs = new Vector();

    for(int i = 0; i < vec.size(); i++) {
       SRM_PATH subDir = (SRM_PATH) vec.elementAt(i);
       makeDirectoryRecursive(subDir,dirNames,ddirs);
    }

    if(!enableHSI) {
      dirNames.append("bye\n\n");
    }

    String proxyFile = ftptransferlog+".proxy";
    File script=null;
    GSSCredential credential = null;
    boolean gsiTransferOk=false;
    String gsiException="";


    if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        credential = createAndCheckProxyIsValid(proxyFile,true);
      }catch(SRM_MSS_Exception srme) {
        util.printMessage("\nERROR : Expired proxy " + proxyFile,
			logger,debugLevel);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        fObj.setExplanation("Expired proxy.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
        status.setExplanation("Expired proxy.");
        return; 
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        fObj.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        status.setExplanation("FAILED_TO_WRITE_PROXY to local file system.");
        System.out.println("Exception="+ee.getMessage());
        return; 
      }

      Object[] param = new Object[1]; 
      param[0] = "REQUEST-ID="+fObj.getRequestToken();
      _theLogger.log(java.util.logging.Level.FINE,
	     "WROTE_PROXY_FILE",(Object[])param);

      try {
        String remoteDirName = "";
        int idx = dirs.getDir().lastIndexOf("/");
        System.out.println(">>>dirs.getDir()="+dirs.getDir());
        if(idx == -8) {
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation("Given path is not in correct format.");
          fObj.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation("Given path is not in correct format.");
          return;
        }
        if(idx != -1 ) {
          remoteDirName = dirs.getDir().substring(0,idx);
        }
         SRMLsClient srmLsClient = new SRMLsClient(remoteDirName,credential,
                       logger,_theLogger,false,debugLevel);
         srmLsClient.setMaxWait(maxWait);
         srmLsClient.setWaitDelay(waitDelay);
         String path = dirs.getDir().substring(idx+1);
         System.out.println(">>>PATH="+path);
         gsiException = srmLsClient.doMkdir(path);
         idx = gsiException.toLowerCase().indexOf("file exists");
         if(gsiException.equals("") || idx != -1) { //if no error proceed
           for(int i = 0; i < ddirs.size(); i++) {
              String subPath = (String) ddirs.elementAt(i);
              GlobusURL gurl = new GlobusURL(dirs.getDir());
              String gurlPath = gurl.getPath();
              if(!gurlPath.startsWith("/")) {
                 gurlPath = "/"+gurlPath;
              }
              idx = subPath.indexOf(gurlPath); 
              if(idx != -1) {
                System.out.println("idx="+idx);
                System.out.println("subpath="+subPath);
                System.out.println("gurl.getPath="+gurlPath);
                String ttemp = subPath.substring(gurlPath.length());
                gsiException = srmLsClient.doMkdir(path+ttemp);
                if(debugLevel >= 4000) {
                  util.printMessage("GsiException="+gsiException);
                }
              }
           }
         }
      }catch(Exception e) {
         gsiException = e.getMessage();
         util.printMessage(gsiException,logger,debugLevel);
      }
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT) {

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      writer.println("user " + accessInfo.getLogin() + " " + 
		accessInfo.getPasswd());
      writer.println("bin");
      writer.println(dirNames.toString());
      writer.println("bye");
      writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
				logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        util.printMessage("user " + accessInfo.getLogin() + "   " + 
                    accessInfo.getPasswd() + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage(dirNames.toString(), logger,debugLevel);
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
       }
    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
     "WROTE_SCRIPT_FILE",(Object[])param);
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN) {
      String mylogin = accessInfo.getLogin();
      String mypasswd = accessInfo.getPasswd();

      if(mylogin == null || mylogin.length() == 0 ||
	  mypasswd == null || mypasswd.length() == 0) {
        if(debugLevel >= 10) { 
          util.printMessage("WARNING: Either userid or password wrong.",
				logger,debugLevel);
        }
        status.setStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_FAILED);
        fObj.setExplanation("Either userid or password wrong.");
        status.setExplanation("Either userid or password wrong.");
        return;
      }

      if(!srmnocipher) {
         //mylogin = SRM_MSS_UTIL.rot13(accessInfo.getLogin());
         //mypasswd = SRM_MSS_UTIL.rot13(accessInfo.getPasswd());
         mylogin = accessInfo.getLogin();
         mypasswd = accessInfo.getPasswd();
      }

      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("LD_LIBRARY_PATH=" + javaLibraryPath +"\n");
      writer.println("export LD_LIBRARY_PATH\n");
      writer.println (srmmsspath + " -vn << END > " + ftptransferlog);
      writer.println("open " + srmmsshost + " " + srmmssport);
      if(srmnonpassivelisting) {
         writer.println("passive");
      }
      writer.println("user " + mylogin + " " +  mypasswd);
      writer.println("bin");
      writer.println(dirNames.toString());
      writer.println("bye");
      writer.println("END");
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
			logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage ("LD_LIBRARY_PATH=" + 
		    this.javaLibraryPath +"\n", logger,debugLevel);
        util.printMessage ("export LD_LIBRARY_PATH\n", logger,debugLevel); 
        util.printMessage (srmmsspath + 
	        " -vn << END > "+ ftptransferlog, logger,debugLevel);
        util.printMessage("open " + srmmsshost + "   " + 
		    srmmssport + "\n\n", logger,debugLevel);
        if(srmnonpassivelisting) {
          util.printMessage("passive\n",logger,debugLevel);
        }
        util.printMessage("user " + mylogin + "   " + 
                    mypasswd + "\n\n", logger,debugLevel);
        util.printMessage("bin\n",logger,debugLevel);
        util.printMessage(dirNames.toString(),logger,debugLevel); 
        util.printMessage("bye\n",logger,debugLevel);
        util.printMessage("END\n",logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
     "WROTE_SCRIPT_FILE",(Object[])param);
  }
  else if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_NONE) {
      //writeLogFile(ftptransferlog,"opening logfile");
      script = File.createTempFile("script","",scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      //writer.println (hsiPath + " -q " + "\"out " + ftptransferlog +";pwd;");
      writer.println (hsiPath + " -q " + "\"out " + ftptransferlog +";");
      writer.println(dirNames.toString() + "; end\"");
      writer.println("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog);
      writer.close();


      if(debugLevel >= 1000) {
        util.printMessage("\n+++ begin script ++++\n", logger,debugLevel); 
        util.printMessage("Date: " + SRM_MSS.printDate() + "\n",
			logger,debugLevel);
        util.printMessage("#!/bin/sh\n\n",logger,debugLevel);
        util.printMessage (hsiPath + 
	        //" -q "+ "\" out " + ftptransferlog + ";pwd;", logger,debugLevel);
	        " -q "+ "\" out " + ftptransferlog + ";", logger,debugLevel);
        util.printMessage (dirNames.toString() + ";end\"");
        util.printMessage("echo \"HSI_ERROR_CODE=\"$? >> " + ftptransferlog, 
			logger,debugLevel);
        util.printMessage("+++ end script ++++\n", logger,debugLevel); 
      }
    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
     "WROTE_SCRIPT_FILE",(Object[])param);
  }
  else {
      util.printMessage("HPSS TYPE not implemented : " + mssType);
      fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      fObj.setExplanation("Unknown MSS Type");
      status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
      status.setExplanation("Unknown MSS Type");
      return;
  }


    if(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
      boolean useEnvp = false;
      long startTime = System.currentTimeMillis(); 

      boolean ok = false;
      Thread t = new Thread();

      while (!ok) {
        try {
           if(script.exists()) {
           Process p0 = 
		     Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
           if(p0.waitFor() == 0) {
	         p0.destroy();
             if(status.getStatus() == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
                Object[] param = new Object[1]; 
                param[0] = "REQUEST-ID="+fObj.getRequestToken();
                _theLogger.log(java.util.logging.Level.FINE,
                "NO_NEED_TO_EXECUTE_SCRIPT_TRANSFER_ABORTED",(Object[])param);
                fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED);
                status.setExplanation("Transfer aborted");
                fObj.setExplanation("Transfer aborted");
                return; 
             }
             else {
                Object[] param = new Object[1]; 
                param[0] = "REQUEST-ID="+fObj.getRequestToken();
                _theLogger.log(java.util.logging.Level.FINE,
                "GOING_TO_EXECUTE_SCRIPT_FILE",(Object[])param);
                ExecScript process = 
		         new ExecScript(script.getAbsolutePath(), 
	               this.javaLibraryPath, true,this,_theLogger);
                process.setLogFile(ftptransferlog);
                status.setCurrentProcess(process); 
                process.execCommand(useEnvp);
             }
             ok = true;
           }
          }
        }catch(IOException ioe) {
          ok = false;
          t.sleep(1000);
        }catch(Exception ie) {
          ok = true;
          ie.printStackTrace(); 
        }
      }

      long endTime = System.currentTimeMillis(); 


     //for the access type none, when the directory already exists,
     //it does not gives any error message.
     //generally for the new directory creation, it writes one line
     //in the logfile, for duplicate directory creation it leaves it
     //empty.


    try {
       File ftplog = new File(ftptransferlog);
       t = new Thread();
       int numTimes = 0;
       while(numTimes < 6) {
         ftplog = new File(ftptransferlog);
         if(!ftplog.exists() || ftplog.length() == 0) {
           t.sleep(10000);
           numTimes++;
         }
         else {
          break;
         }
       }//end while
       ftplog = new File(ftptransferlog);
       if(!ftplog.exists() || ftplog.length() == 0) {
          util.printMessage("\nProbably some problem in executing script.");
          util.printMessage("log file is empty " + ftptransferlog);
          fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          fObj.setExplanation("ERROR while executing script.");
          status.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
          status.setExplanation("ERROR while executing script.");
        }
    }catch(Exception e) {}


    //seems like the script runs successfully.
    //but there may be other problems such as login, file not found etc.
    //need to parse the transfer log to find out the details

    //do you think we need to check file exists or not etc.
    //I am not sure we will think about this later.

    //assume that file  really exists.

    //if file did not exists.

    Object[] param = new Object[1]; 
    param[0] = "REQUEST-ID="+fObj.getRequestToken();
    _theLogger.log(java.util.logging.Level.FINE,
      "SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
  }//(mssType != SRM_ACCESS_TYPE.SRM_ACCESS_GSI) 


  try {

      MSS_MESSAGE_W_E pftpmssg_e = new MSS_MESSAGE_W_E ();
      if(mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) {
	   SRM_MSS_UTIL.getMSSGSIError(gsiException,pftpmssg_e,debugLevel);
      }
      else {
	   SRM_MSS_UTIL.getMSSError(ftptransferlog,pftpmssg_e,debugLevel);
           File ff = new File (ftptransferlog);
           Object[] param = new Object[5]; 
           param[0] = "REQUEST-ID="+fObj.getRequestToken();
           param[1] = "LOGFILE="+ftptransferlog;
           param[2] = "LOGFILE exists="+ff.exists();
           param[3] = "STATUS="+pftpmssg_e.getStatus();
           param[4] = "EXPLANATION="+pftpmssg_e.getExplanation();
           _theLogger.log(java.util.logging.Level.FINE,
	    "STATUS_AFTER_SCRIPT_RAN_SUCCESSFULLY",(Object[])param);
      }
      pftpmssg = pftpmssg_e.getStatus();


      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE ||
        pftpmssg == MSS_MESSAGE.SRM_MSS_NOT_INITIALIZED) { 
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        srmPingStatus.setLastAccessedTime(""+new Date());
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_DOWN);
        fObj.setExplanation("MSS Down.");
      }
      else {
        srmPingStatus.setStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        srmPingStatus.setLastAccessedTime(""+new Date());
        //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_STATUS_UP);
        //fObj.setExplanation("MSS Up.");
      }

      fObj.setPFTPStatus(pftpmssg);
      if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED ||
         pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) { 
 
         util.printMessage("\nSome directory creation failed due to " +
		    "MSS down and will try again", logger,debugLevel);

         fObj.setExplanation("Will retry again");
         fObj.setFTPTransferLog(ftptransferlog);
         fObj.incrementNumRetry();
         fObj.setTimeStamp(new GregorianCalendar());
         fObj.setRetryTime(MSSWaitTime*2);

         if(fObj.getNumRetry() < getMSSMaxRetrial()) {
            Object _parent = fObj.getTaskThread().getParent();
            //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_RETRY);
            ThreadCallBack taskThread = 
                 new ThreadCallBack((SRM_MSS)_parent);
            taskThread.setCommand(fObj,status);
            fObj.setTaskThread(taskThread);

            if(monitorThread == null) {
              threadDisable = false;
              monitorThreadPool = new MonitorThreadPool(1,(SRM_MSS)this);
              monitorThread = 
				new MonitorThreadMain(monitorThreadPool,(SRM_MSS)this);
              monitorThread.start();
            }

            Object[] param = new Object[2]; 
            param[0] = "SOURCE="+fObj; 
            param[1] = "REQUEST-ID="+status.getRequestToken(); 
            _theLogger.log(java.util.logging.Level.FINE,
				  "MKDIR_RETRY",(Object[])param);
            othersQueue.add(fObj);
         }
         else {
            util.printMessage("\nrid=" + fObj.getRequestToken() + 
		       " failed.",logger,debugLevel);
            fObj.setPFTPStatus(pftpmssg);
            status.setStatus(pftpmssg);
            fObj.setPFTPStatus(pftpmssg);
         }
         if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED)  {
           status.setExplanation("MSS access limit reached, Try again later.");
           fObj.setExplanation("MSS access limit reached, Try again later.");
         }
         else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
           status.setExplanation("MSS not available.");
           fObj.setExplanation("MSS not available.");
         }
      }

      if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE) {
         util.printMessage("\nMSSMKDIR : STATUS=TRANSFER_DONE", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
         status.setExplanation("Created Directory successfully.");
         fObj.setExplanation("Created Directory successfully.");

         /*
         if(debugLevel <= 200) { 
           File f = new File(ftptransferlog);
           if(f.exists()) { f.delete(); }
         }
         */
         if(debugLevel <= 6000) { 
           File f = new File(proxyFile);
           if(f.exists()) { f.delete(); }
         }
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_FILE_EXISTS) {
         util.printMessage("\nMSSMKDIR : STATUS=FILE_EXISTS", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
         status.setExplanation("Directory already exists.");
         fObj.setExplanation("Directory already exists.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED) {
         util.printMessage("\nMSSMKDIR : STATUS=REQUEST_FAILED", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
         status.setExplanation("No access permission");
         fObj.setExplanation("No access permission");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED) {
         util.printMessage("\nMSSMKDIR : STATUS=AUTHENTICATION_FAILED", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
         status.setExplanation("No permission.");
         fObj.setExplanation("No permission.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH) {
         util.printMessage("\nMSSMKDIR : STATUS=SRM_MSS_NO_SUCH_PATH", 
				logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
         status.setExplanation("No such path.");
         fObj.setExplanation("No such path.");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED) {
         util.printMessage("\nMSS limit reached.", logger,debugLevel);
         status.setExplanation("mss limit reached");
         fObj.setExplanation("mss limit reached");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE) {
         util.printMessage("\nMSS system not available.", logger,debugLevel);
         status.setExplanation("mss not available");
         fObj.setExplanation("mss not available");
      }
      else if(pftpmssg == MSS_MESSAGE.SRM_MSS_MSS_ERROR) {
         util.printMessage("\nSRM MSS Error", logger,debugLevel);
         status.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
         status.setExplanation("MSS error");
         fObj.setExplanation("MSS error");
      }
      else {
         util.printMessage("\n"+pftpmssg.toString(), logger,debugLevel);
         status.setStatus(pftpmssg);
         status.setExplanation(pftpmssg.toString());
         fObj.setExplanation(gsiException);
      }
     }catch(Exception e) {
        e.printStackTrace();
     }
  }catch(IOException ioe) {
    ioe.printStackTrace();
  }
}

public Object checkStatus(String requestId) throws Exception {
  return super.checkStatus(requestId);
}

public SRM_ACCESS_INFO generateAccessInfo (HashMap input) throws Exception {
    return SRM_ACCESS_INFO.generateDefault(input);
}
}
