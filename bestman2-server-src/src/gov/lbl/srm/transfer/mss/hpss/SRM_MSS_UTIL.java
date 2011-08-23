/**
 *
 * BeStMan Copyright (c) 2007, The Regents of the University of California,
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

package gov.lbl.srm.transfer.mss.hpss;

import java.io.*;
import java.util.*;
import java.net.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.util.ConfigUtil;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.apache.log4j.*;

import gov.lbl.srm.transfer.mss.*;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSS_UTIL
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_MSS_UTIL {

private static boolean _debug;
private static Log logger = 
	LogFactory.getLog(gov.lbl.srm.transfer.mss.hpss.SRM_MSS_UTIL.class.getName());


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getConfig
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Properties getConfig(String confFile) throws Exception {
  return parsefile(confFile);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//filenameCompare
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean name_compare(String s1, String s2) {
  if(s1== null || s2 == null) return false;
  if(s1.equals(s2))  return true;
  return false;
}

public static String rot13 (String passwd) {

   StringBuffer tempReturn = new StringBuffer();
   int abyte = 0;

   for (int i=0; i < passwd.length(); i++) {
     abyte = passwd.charAt(i);
     int cap = abyte & 32;
     abyte &= ~cap;
     abyte = ((abyte >= 'A') && 
	      (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;
     tempReturn.append((char)abyte);
   }

    return tempReturn.toString();
        
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSAccess
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized static SRM_ACCESS_TYPE getMSSAccess (Properties sys_config)  
  throws Exception {

   SRM_ACCESS_TYPE mssType=null;
   String accessType = (String) sys_config.get("siteAccessType");
   if(_debug) {
      util.printMessage("siteAccessType : " + accessType, logger);
   }
   if(accessType == null) {
     mssType = SRM_ACCESS_TYPE.SRM_ACCESS_UNKNOWN;
   }
   else {
     if(name_compare(accessType,"GSI")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_GSI;
     }
     else if(name_compare(accessType,"PLAIN")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_PLAIN;
     }
     else if(name_compare(accessType,"ENCRYPT")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT;
     }
     else if(name_compare(accessType,"KERBEROS")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_KERBEROS;
     }
     else if(name_compare(accessType,"SSH")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_SSH;
     }
     else if(name_compare(accessType,"SCP")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_SCP;
     }
     else if(name_compare(accessType,"NCARMSS")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_NCARMSS;
     }
     else if(name_compare(accessType,"NONE")) {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_NONE;
     }
     else {
       mssType = SRM_ACCESS_TYPE.SRM_ACCESS_UNKNOWN;
     }
   }
   return mssType;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getValueParameter
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getValueParameter (Properties sys_config,String propName)  
  throws Exception {

   String mssls = (String) sys_config.get(propName);

   if(_debug) {
      util.printMessage("\nPropName : " + propName + " " + mssls,logger);
   }

   if(mssls != null) {
     return mssls;
   }
   return "";
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSPort
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static int getIntValueParameter (Properties sys_config,String propName)  
  throws Exception {

   String mssport = (String) sys_config.get(propName);

   if(_debug) {
      util.printMessage("\nPropName : " + propName + " " + mssport,logger);
   }

   if(mssport != null) {
      return Integer.parseInt(mssport);
   }
   return -1;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getNoCipher
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean getBoolValueParameter
	(Properties sys_config, String propName)  throws Exception {

   String value = (String) sys_config.get(propName);
   if(_debug) {
      util.printMessage("\nProperty : " + propName + " " + value, logger);
   }
   if(value != null) {
     if((name_compare(value,"true")) || 
        (name_compare(value,"yes")) || (name_compare(value,"1")) || 
        (name_compare(value,"defined"))) {
          return true;
     }
   }
   return false;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Properties parsefile(String confFile)  throws IOException,
  java.util.zip.DataFormatException {
   Properties sys_config = new Properties ();
   try {
     String ref;

     FileInputStream file = new FileInputStream (confFile);
     BufferedReader in = new BufferedReader ( new InputStreamReader (file));

     while ((ref= in.readLine()) != null) {
        if(ref.startsWith("#") || ref.equals(""))
            continue;
        int starpos = ref.indexOf("*");   
        if(starpos == -1) 
           throw new java.util.zip.DataFormatException(ref);
        int eqpos = ref.indexOf("=",starpos);
        if(eqpos == -1)  
           throw new java.util.zip.DataFormatException(ref);
        String module = ref.substring(0,starpos);
        String var = ref.substring(starpos+1, eqpos);
        String val = ref.substring(eqpos+1);

        if(module.equalsIgnoreCase("mss")) {
             sys_config.put(var,val); 
        }
     }
     if(file != null) file.close();
   }catch(IOException ex) {
        throw new IOException("SRMLSTest : cannot read from "
                + ex.getMessage());
   }catch(java.util.zip.DataFormatException de) {
        throw new java.util.zip.DataFormatException 
                 ("SRMLSTest : Invalid format " + de.getMessage());
   }
   return sys_config;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSTransferTime() is used if the file transfer was successful. It gets
// the transfer time from the pftp output.
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static double getMSSTransferTime(String ftptransferlog) 
	throws SRM_MSS_Exception { 

  double result = 0;

  try {
   FileInputStream fis = new FileInputStream(ftptransferlog);
   BufferedReader bis = new BufferedReader(new InputStreamReader(fis));

   String ref = "";
   while ((ref = bis.readLine()) != null) {
      int idx = ref.indexOf("bytes received");
      int idx1 = ref.indexOf("bytes sent");
      if(idx != -1 || idx1 != -1) {
        StringTokenizer stk = new StringTokenizer(ref);
        int i = 0;
        while(stk.hasMoreTokens()) {
          String temp = stk.nextToken();
          if(i == 4) {
            try {
               result = Double.parseDouble(temp);
            }catch(NumberFormatException nfe) {}
            break;
          }
          i++;
        }
      }
   }
  bis.close();
  fis.close();
  }catch(Exception e) {
     throw new SRM_MSS_Exception(e.getMessage());
  }
  return result;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// newToken
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String newToken(String token) {

 Random random = new Random ();
 Calendar c = new GregorianCalendar();
 int year =     c.get(Calendar.YEAR);
 int month =    c.get(Calendar.MONTH);
 int date =     c.get(Calendar.DATE);
 int hours =    c.get(Calendar.HOUR_OF_DAY);
 int minutes =  c.get(Calendar.MINUTE);
 int seconds =  c.get(Calendar.SECOND);

 StringBuffer buffer = new StringBuffer ();
 buffer.append(token+"-");
 buffer.append(year+"-"+month+"-"+date+":");
 buffer.append(hours+":"+minutes+":"+seconds+"-");
 buffer.append(random.nextInt((int)4.294967296E9));
 return buffer.toString();
}

//
// getPFTPError(), to be defined below, parses the output of pftp transfer
// to find possible reasons for the transfer failure. Examples of possible
// failures that this function can handle are show now. These are real
// examples.
/*
------------------------------------------------------------------------------
**** wrong username or password
------------------------------------------------------------------------------
Login incorrect. - Reenter username and password.
Login failed.

------------------------------------------------------------------------------
**** no such file
------------------------------------------------------------------------------
Connected to hpss.nersc.gov.
220-HELLO welcome to hpss
220-
220 hpss FTP server (Version PFTPD.70 Mon Mar 1 16:39:29 PST 1999) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
331 Password required for luis.
230 User /.../dce.nersc.gov/luis logged in.
200 Type set to I.
local: filelocal remote: fileremote
559 Could not stat file ("fileremote").
221 Goodbye.
------------------------------------------------------------------------------
**** reached limit of allowed ftps at BNL
------------------------------------------------------------------------------
Multinode is Disabled.
Connected to rmds01.rhic.bnl.gov.
220 rmds01 FTP server (Version PFTPD.4 Fri Feb 26 17:39:33 EST 1999) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
331 Password required for starreco.
530-**********************************************************************
530-*
530-*
530-*      Access denied due to active ftp login limit of 30 sessions
530-*
530-*
530-**********************************************************************
421 Service not available, remote server has closed connection
Login failed.
Not connected.
Not connected.
------------------------------------------------------------------------------
**** reached limit of allowed ftps at LBNL
------------------------------------------------------------------------------
Connected to sleepy.nersc.gov.
220 sleepy FTP server (Version PFTPD.69 Mon Jun 7 15:05:53 PDT 1999) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
421 Service not available - maximum number of sessions exceeded.
Login failed.
Not connected.
Not connected.
------------------------------------------------------------------------------
**** HPSS error
------------------------------------------------------------------------------
Multinode is Disabled.
Connected to rmds02a.rcf.bnl.local.
220 rmds02 FTP server (Version PFTPD.1 Thu Jun 4 18:21:37 CDT 1998) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
331 Password required for starreco.
230 User starreco logged in.
200 Type set to I.
559 Filename: /home/starreco/objy/COS7/fd30025/psc239_02_150evts_dst.xdf.STAR.DB
 cannot be opened - HPSS Error: -5
221 Goodbye.
------------------------------------------------------------------------------
**** system not available
------------------------------------------------------------------------------
Multinode is Disabled.
Connected to rmds01.rhic.bnl.gov.
220 rmds01 FTP server (Version PFTPD.4 Fri Feb 26 17:39:33 EST 1999) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
331 Password required for starreco.
230 User starreco logged in.
200 Type set to I.
421 Service not available, remote server has closed connection
------------------------------------------------------------------------------
*/
/*
220 heart-g0 FTP server (HPSS 4.3 PFTPD V1.1.2 Fri Dec 13 19:59:07 PST 2002) rea
dy.
Remote system type is UNIX.
Using binary mode to transfer files.
530 User /.../dce.nersc.gov/-----BEGIN (unknown) access denied.
Login failed.
?Invalid command
?Invalid command
421 Service not available, remote server has closed connection
Not connected.
*/
/*
% pftp_client_gsi garchive.nersc.gov
Warning Preceding without HPSS.conf (-2)
Connected to floyd-g0.nersc.gov.
421 Service not available, remote server has closed connection
421 Service not available, remote server has closed connection
421 Service not available, remote server has closed connection
Multinode is Disabled.
ftp> quit
*/
//
// getPFTPError() is used if a file transfer failed. It looks at the pftp
// output to try to find the reason for failure.
//

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSGetPutError
// used for mssGet, mssPut, calls etc
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static MSS_MESSAGE getMSSGetPutError
		(String ftptransferlog, int debugLevel, StringBuffer buf) {

   MSS_MESSAGE okdone = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

   try {
     FileInputStream fis = new FileInputStream(ftptransferlog);
     BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
     String ref = "";
     while((ref = bis.readLine()) != null) {
        //System.out.println(">>>REF="+ref);
        int idx = ref.indexOf("226 Transfer Complete");
        int idx1 = ref.indexOf("HSI_ERROR_CODE=0");
        if(idx != -1 || idx1 != -1) { 
          util.printMessage("\nLooks like everything is okay.",logger);
          okdone = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
          buf.append("Transfer is successful");
          return okdone;
        }
        idx = ref.indexOf("is a directory - ignored");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("is a directory - ignored");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=64");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Command line usage error.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=65");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Data format error.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=66");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Cannot open input, user's input file did not exist.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=67");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Addressee unknown, user specified does not exist.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=68");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Host name unknown, Host specified does not exist.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=70");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Internal software error inconsistency etc. in HSI code.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=71");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("OS error, such as cannot fork, pipe etc.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=72");
        if(idx != -1) {
	  okdone = MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
          buf.append("Critical OS file missing, cannot be opened.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=73");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append("Cannot create user output file.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=74");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append
            ("Input/output error, occurred while doing I/O on some file.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=75");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append
            ("Temporary failure. User is invited to retry.");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=76");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append 
           ("Remote error in protocol. " +
                "The remote system returned something that was not possible"+
                " during a protocol exchange");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=77");
        if(idx != -1) {
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          buf.append ("Permission denied");
          return okdone;
        }
        idx = ref.indexOf("HSI_ERROR_CODE=69");
        if(idx != -1) { 
          util.printMessage("\nMSS Service not available.",logger);
          okdone=MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE;
          break;
        }
        idx = ref.toLowerCase().indexOf("error -1 on transfer");
        if(idx != -1) { 
          util.printMessage("\nMSS Error",logger);
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          break;
        }
        idx = ref.toLowerCase().indexOf("i/o error");
        if(idx != -1) { 
          util.printMessage("\nMSS Error",logger);
          okdone = MSS_MESSAGE.SRM_MSS_MSS_ERROR;
          break;
        }
     }
     return okdone;
   }catch(Exception ioe) {
     util.printMessage("\nException : " + ioe.getMessage(), logger); 
     return MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSGSIError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void getMSSGSIError(String exception, MSS_MESSAGE_W_E okdone,
    int debugLevel) {

   boolean commandOk = true;
   StringBuffer buf = new StringBuffer();

   okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
   okdone.setExplanation(exception);

   if(exception.equals("")) {
      okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
      return;
   }

   System.out.println(">>> Exception from GSI=" + exception);
   try {
     int idx = exception.indexOf("cannot be opened - HPSS Error: -5");
     int idx1 = exception.indexOf("hpssex_OpenConnection: unable to obtain remote site info");
     int idx2 = exception.indexOf("Unable to setup communication to HPSS");
     if(idx != -1 || idx1 != -1 || idx2 != -1) {
       if(debugLevel > 1) {
         util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx = exception.indexOf("Permission denied");
     if(idx != -1) {
       if(debugLevel > 1) {
         util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx = exception.indexOf("HPSS Error: -13");
     if(idx != -1) {
       if(debugLevel > 1) {
         util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx1 = exception.indexOf("Input/output error");
     if(idx1 != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
       okdone.setExplanation
		("Given path is a directory not a plain file.");
       commandOk = false;
     }
     idx1 = exception.indexOf("Is a directory");
     if(idx1 != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
       okdone.setExplanation
		("Given path is a directory already existing in HPSS.");
       commandOk = false;
     }
     idx = exception.indexOf("HPSS Error: -2");
     idx1 = exception.indexOf("HPSS Error: -21");
     idx2 = exception.indexOf("No such file or directory");
     if(idx != -1 || idx1 != -1 || idx2 != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx = exception.indexOf("non-empty directory");
     if(idx != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_DIRECTORY_NOT_EMPTY);
       okdone.setExplanation(exception);
       commandOk = false;
     }

     idx = exception.indexOf("File exists");
     if(idx != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx = exception.indexOf("Too many users");
     if(idx != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED);
       okdone.setExplanation(exception);
       commandOk = false;
     }
     idx = exception.indexOf("Authentication Error");
     if(idx != -1) {
       if(debugLevel > 1) {
        util.printMessage("\nERROR: " + exception,logger);
       }
       okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
       okdone.setExplanation(exception);
       commandOk = false;
     }
   }catch(Exception ioe) {
     util.printMessage("\nException : " + ioe.getMessage(), logger);
     okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
     okdone.setExplanation(ioe.getMessage());
     //return okdone;
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getMSSError
// used for copy,delete, mkdir, msls, msslisting
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized static void getMSSError(String ftptransferlog, MSS_MESSAGE_W_E okdone, int debugLevel) {

   boolean commandOk = true;
   boolean hsiNoError=false;
   StringBuffer buf = new StringBuffer();

   okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
   
   try {
     FileInputStream fis = new FileInputStream(ftptransferlog);
     BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
     String ref = "";
     while((ref = bis.readLine()) != null) {
        //System.out.println(">>>REF="+ref);
        //if(ref.startsWith("pwd")) continue;
        int idx = ref.indexOf("No such file or directory");
        int idx1 = ref.indexOf("Could not stat");
        int idx3 = ref.indexOf("HSI_ERROR_CODE=72");
        if(idx != -1 || idx1 != -1 || idx3 != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nNo such file in HPSS.", logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);      
          okdone.setExplanation("No such file in HPSS");
          commandOk = false;
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=0");
        if(idx != -1) { 
          util.printMessage("\nLooks like everything is okay.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
          okdone.setExplanation("Transfer Done");
          hsiNoError=true;
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=69");
        if(idx != -1) { 
          util.printMessage("\nMSS Service not available.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
          okdone.setExplanation("Service unavailable");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=64");
        if(idx != -1) { 
          util.printMessage("\nCommand line usage error.",logger);
          //okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Command line usage error.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=65");
        if(idx != -1) { 
          util.printMessage("\nData format error.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Data Format ERROR.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=66");
        if(idx != -1) { 
          util.printMessage("\nCannot open input.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Cannot open input.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=67");
        if(idx != -1) { 
          util.printMessage("\nAddressee unknown.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Addresse unknown.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=68");
        if(idx != -1) { 
          util.printMessage("\nHost name unknown.",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Host name unknown.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=70");
        if(idx != -1) { 
          util.printMessage("\n Internal software error inconsistency",logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Internal software error inconsistency.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=71");
        if(idx != -1) { 
          util.printMessage("\n OS error, such as cannot fork, pipe etc.",
                logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("OS error, such as cannot fork, pipe etc.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=72");
        if(idx != -1) { 
          util.printMessage("\n Critical OS file missing, cannot be opened "+
                " or has some sort of error", logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Critical OS file missing, cannot be opened "+
                " or has some sort of error.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=73");
        if(idx != -1) { 
          util.printMessage("\n Cannot create user output file. ", logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Cannot create user output file.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=74");
        if(idx != -1) { 
          util.printMessage
           ("\n Input/output error, occured while doing I/O on some file", 
                        logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation
           ("Input/output error, occured while doing I/O on some file");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=75");
        if(idx != -1) { 
          util.printMessage
           ("\n Temporary failure, User is invited to retry", logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation
           ("Temporary failure, User is invited to retry");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=76");
        if(idx != -1) { 
          util.printMessage
           ("\nRemote error in protocol.", logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation ("Remote error in protocol.");
          break;
        }

        idx = ref.indexOf("HSI_ERROR_CODE=77");
        if(idx != -1) { 
          util.printMessage
           ("\nPermission denied.", logger);
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation ("Permission denied.");
          break;
        }

        idx = ref.indexOf("User not logged in");
        if(idx != -1) {
          if(debugLevel > 1) {
            util.printMessage
	      ("\nUser not logged in, loginname or password incorrect." 
	        + " a file.",logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
          okdone.setExplanation
           ("User not logged in, loginname or password incorrect");
          commandOk = false;
          break;
        }

        idx = ref.indexOf("is a directory - ignored");
        if(idx != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nGiven filePath is a directory and " 
		 + " not a file.",logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation("Given filepath is a directory and not a file");
          commandOk = false;
          break;
        }

        idx = ref.indexOf
           ("425 Can't build data connection: Connection refused");
        if(idx != -1) {
          if(debugLevel > 1) {
            util.printMessage(ref,logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation(ref);
          commandOk = false;
          break;
        }

        idx = ref.indexOf("Directory not empty");
        if(idx != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nGiven directory not empty ",logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_DIRECTORY_NOT_EMPTY);
          okdone.setExplanation("Given directory not empty");
          commandOk = false;
          break;
        }

        idx = ref.indexOf("cannot be opened - HPSS Error: -5");
        idx1 = ref.indexOf("cannot be opened - HPSS Error: -21");
        int idx2 = ref.indexOf("cannot be opened - HPSS Error");
        if(idx != -1 || idx1 != -1 || idx2 != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nERROR: " + ref,logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
          okdone.setExplanation(ref);
          commandOk = false;
          break;
        }

        //trying to copy a directory without -R option
        idx = ref.indexOf("is a directory, and -R (recursion) not specified");
        if(idx != -1) {
          if(debugLevel > 1) {
            util.printMessage
	     ("\nTrying to copy a directory without recursive option",logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
          okdone.setExplanation
           ("Trying to copy a directory without recursive option");
          commandOk = false;
          break;
        }

        int idx6 = ref.indexOf("is not a directory - skipped");
        idx = ref.indexOf("Not a directory");
        if(idx != -1 || idx6 != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nGiven file path is not a directory.", logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_IS_NOT_A_DIRECTORY);
          okdone.setExplanation("Given file path is not a directory");
          commandOk = false;
          break;
        }

        idx = ref.indexOf("Access denied");
        if(idx != -1) {
           if(debugLevel > 1) {
             util.printMessage("\nAccess denied.", logger);
           }
		   okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
           okdone.setExplanation("Access denied");
           commandOk = false;
           break;
        }

        idx = ref.indexOf("559 Could not stat file");
        idx1 = ref.indexOf("Could not stat file");
        if(idx != -1 || idx1 != -1) {
          if(debugLevel > 1) {
            util.printMessage("\nNo such file in HPSS.", logger);
          }
          okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
          okdone.setExplanation("No such file in HPSS");
          commandOk = false;
          break;
        }
        int vv = 9;
        idx = ref.indexOf("559 Filename");
        idx1 = ref.indexOf("hpss_Lstat: No such file or directory");
        idx2 = ref.indexOf("hpss_Opendir failed Code: -13");
        if(idx != -1 || idx1 != -1 || idx2 != -1) {
          if(idx1 != -1) vv = 2; 
          if(idx2 != -1) vv = 3; 
          int i = 0;
          int hpsserror = 0;
          StringTokenizer stk = new StringTokenizer(ref);
          while(stk.hasMoreTokens()) {
            String tt = stk.nextToken(); 
            if(i == vv) { 
               try {
                 hpsserror = -1*Integer.parseInt(tt.trim());
               }catch(NumberFormatException nfe) {}
            }
            i++;
          }
          if(debugLevel > 1) {
            util.printMessage("\nHPSS Error : " + hpsserror, logger);
          }
          if(hpsserror == 2) {
	    okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
            okdone.setExplanation("HPSS Error " + hpsserror);
          }
          //file does not exist
          else if(hpsserror == 21) { 
	    okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
            okdone.setExplanation("HPSS Error " + hpsserror);
          } 
          //not a file
          else if(hpsserror == 13) {
	    okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
            okdone.setExplanation("HPSS Error " + hpsserror);
          } 
          //cannot read file for GET, cannot write file for PUT
          else {
	    okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
            okdone.setExplanation("HPSS Error " + hpsserror);
          }
          commandOk = false;
          break;
        }

// ORNL
        /*
220 arm16 FTP server (HPSS 4.1 PFTPD V1.1.6 Tue Oct 23 13:07:33 EDT 2001) ready.
Remote system type is UNIX.
Using binary mode to transfer files.
**** NOTE: Server supports Parallel Features    ****
****       Auto-Parallel Substitution Enabled.  ****
331 User: /.../dce.ccs.ornl.gov/asim - Password Required.
530 Could not load thread state (pass).
Login failed.
200 Type set to I.
Please login with USER and PASS.
Please login with USER and PASS.
local: /d0/srm/data/1061984066_8486_1891 remote: //home/wgstrand/ACPI/B06.44/ins
tant_6/B06.44.atm.2047-08_hb.nc
200 PORT command successful.
530 Please login with USER and PASS.
221 Goodbye.
*/

          idx = ref.indexOf("530 Could not load thread state (pass)");
          idx1 = ref.indexOf("530 Please login with USER and PASS");
          if(idx != -1 || idx1 != -1) {
             util.printMessage("\nHPSS service not available",logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
             okdone.setExplanation(ref);
             commandOk = false;
             break;
          }



/*
432 Data channel authentication failed
Login failed.
421 Service not available, remote server has closed connection
*/

          idx = ref.indexOf("432 Data channel authentication failed");
          idx1 = ref.indexOf("Login failed");
          if(idx != -1 || idx1 != -1) {
             util.printMessage("\nLogin failed",logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
             okdone.setExplanation(ref);  
             commandOk = false;
             break;
          }


//421 Service not available, remote server has closed connection     
          idx = ref.indexOf("421 Service not available");
          if(idx != -1) {
            util.printMessage("\nHPSS service not available",logger);
            okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
            okdone.setExplanation(ref);
            commandOk = false;
            break;
          }

          idx = ref.indexOf("Unable to setup communication to HPSS");
          if(idx != -1) {
            util.printMessage("\nHPSS service not available",logger);
            okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
            okdone.setExplanation(ref);
            commandOk = false;
            break;
          }

/*
Parallel block size set to 1048576.
Multinode is Disabled.
Not connected.
Not connected.
Not connected.
*/
          idx = ref.indexOf("Not connected");
          if(idx != -1) {
            if(debugLevel > 1) {
              util.printMessage("\nHPSS DOWN.", logger);
            }
            okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
            okdone.setExplanation(ref);
            commandOk = false;
            break;
          }

          idx = ref.indexOf("Cannot get Shared Memory");
          idx1 = ref.indexOf("Please Notify the System");
            
          if(idx != -1 || idx1 != -1) {
            if(debugLevel > 1) {
              util.printMessage
                ("\nHPSS DOWN: Cannot get shared memory", logger);
            }
            okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_NOT_AVAILABLE);
            okdone.setExplanation(ref);
            commandOk = false;
            break;
          }

          //limit of pftps reached at BNL
          idx = ref.indexOf("Access denied due to active ftp login limit");
          if(idx != -1) {
             util.printMessage("\nLimit of pftps reached.", logger);     
             //do exec grep 530 ftptransferlog
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED);
             okdone.setExplanation(ref);
             commandOk = false;
             break;  
          }

          //limit of pftps reached at LBNL
          idx = ref.indexOf
	    ("421 Service not available - maximum number of sessions exceeded");
          if(idx != -1) {
             util.printMessage("\nLimit of pftps reached.", logger);     
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_LIMIT_REACHED);
             okdone.setExplanation(ref);
             commandOk = false;
             break;  
          }

/*
559 Filename: /nersc/mp9/tcraig/ccsm/b20.007/atm/hist/ cannot be opened - HPSS E
rror: -21
*/
/*
GSSAPI error: initializing context
GSSAPI authentication failed
GSSAPI error major: GSS Major Status: General failure
GSSAPI error minor: GSS Minor Status Error Chain:
*/

/*
GSSAPI error major: GSS Major Status: Authentication Failed

GSSAPI error minor: GSS Minor Status Error Chain:
*/


           idx = ref.indexOf("GSSAPI error: No local mapping for Globus ID");
           if(idx != -1) {
              util.printMessage ("\nGSSAPI error: " +
		"No Local mapping for DN. Check the GSS proxy.",logger);
              okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
              okdone.setExplanation(ref);
              commandOk = false;
              break;
           }

           idx = ref.indexOf("GSSAPI authentication failed"); 
           idx1 = ref.indexOf("GSSAPI error");

           if(idx != -1 || idx1 != -1) {
              util.printMessage("\nGSSAPI auth failed. " +
					"Check the GSS proxy.",logger);
              okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
              okdone.setExplanation(ref);
              commandOk = false;
              break;
           }

           idx = ref.indexOf
            ("GSSAPI error major: GSS Major Status: Authentication Failed");
           idx1 = ref.indexOf
            ("GSSAPI error minor: GSS Minor Status  Error Chain");
           idx2 = ref.indexOf("Invalid CRL: The available CRL has expired");

           if(idx != -1 || idx1 != -1 || idx2 != -1) {
              util.printMessage("\nGSSAPI error " +
		"Check the GSS proxy or may CRL has expired.",logger);
              okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
              okdone.setExplanation(ref);
              commandOk = false;
              break;
           }

           idx = ref.indexOf("ftp: SIZE command failed"); 
           if(idx != -1) {
              util.printMessage("\nHPSS could not find the file", logger);
              okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
              okdone.setExplanation(ref);
              commandOk = false;
           } 

           idx = ref.indexOf("529 Bad Data Transfer");
           if(idx != -1) {
              util.printMessage
		("\nHPSS ports are closed by firewall possibly.",logger);
              okdone.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
              okdone.setExplanation(ref);
              commandOk = false;
           }

           //ORNL: 530 Login incorrect. - Reenter username and password.
           idx = ref.indexOf("Login incorrect");
           idx1 = ref.indexOf("530 Login incorrect");
           idx2 = ref.indexOf("Login failed");
           idx3 = ref.indexOf("530 User");
           int idx4 = ref.indexOf("(unknown) access denied");

           if(idx != -1 || idx1 != -1 || idx2 != -1 || idx3 != -1 
				||idx4 != -1) {
             util.printMessage("\nLogin incorrect. Check username and" + 
                " password.", logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED); 
             okdone.setExplanation(ref);
             commandOk = false;
           }

//account does not exist  
//530 User /.../dce.ccs.ornl.gov/srm (unknown) access denied.
            
           idx = ref.indexOf("access denied");
           idx1 = ref.indexOf("hpss_Opendir failed Code: -13");
           idx2 = ref.indexOf("530 User");

           if(idx != -1 || idx1 != -1 || idx2 != -1) {
             util.printMessage("\nLogin incorrect. Check username and" + 
                " password.", logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED); 
             okdone.setExplanation(ref);
             commandOk = false;
           }

           idx = ref.indexOf("226 Transfer Complete");
           if(idx != -1) { 
             util.printMessage("\nLooks like everything is okay.",logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
             okdone.setExplanation(ref);
           }

           idx = ref.indexOf("257 MKD command successful");
           if(idx != -1) {
             util.printMessage("\nLooks like mkdir is okay.",logger);
             okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
             okdone.setExplanation(ref);
           }

           idx = ref.indexOf("550");
           idx1 = ref.indexOf("517");
           if(idx != -1 || idx1 != -1) {
             //util.printMessage ("\nLooks like mkdir or rmdir " +
                 //"failed due to existing target.",logger);
       
             idx = ref.indexOf("File exists");
             if(idx != -1) {
               //util.printMessage("\nLooks like mkdir/rmdir failed " +
                      //"for existing directory or file.", logger); 
               okdone.setStatus(MSS_MESSAGE.SRM_MSS_FILE_EXISTS);
               okdone.setExplanation(ref);
               commandOk = false;
             } 
             
             idx = ref.indexOf("Permission denied");
             if(idx != -1) {
               util.printMessage("\nLooks like mkdir/rmdir failed " +
                  "for access permission.", logger);
               okdone.setStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);  
               okdone.setExplanation(ref);
               commandOk = false;
             }

             idx = ref.indexOf("No such file or directory");
             if(idx != -1) {
               util.printMessage("\nLooks like ls/mkdir/rmdir failed " +
                  "for existing directory.", logger);
               okdone.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
               okdone.setExplanation(ref);
               commandOk = false;
             }

             idx = ref.indexOf("Arguments too long");
             if(idx != -1) {
               util.printMessage("\nresults are too much that pftp " +
                  "cannot handle.", logger);
               okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);  
               okdone.setExplanation(ref);
               commandOk = false;
             }

             idx = ref.indexOf("Invalid argument");
             if(idx != -1) {
               util.printMessage("\nsometimes when path is given as / to create mkdir " +
                  "this error is given.", logger);
               okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
               okdone.setExplanation(ref);
               commandOk = false;
             }

             idx = ref.indexOf("250 RMD command successful");
             if(idx != -1) { 
                util.printMessage("\nLooks like rmdir is okay.", logger);
                okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
                okdone.setExplanation(ref);
             }
          
           }//end of string matching 550 or 517

           idx = ref.indexOf("221 Goodbye");
           if(idx != -1) {
              //check the last message
           }

     }
     bis.close(); 
     fis.close(); 
     if(commandOk && hsiNoError) {
        util.printMessage("\nOKAY. Command Successful: ", logger);
        okdone.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
        okdone.setExplanation("Command successful");
     }
 	 //return okdone; 
   }catch(Exception ioe) {
     util.printMessage("\nException : " + ioe.getMessage(), logger); 
     okdone.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
     okdone.setExplanation(ioe.getMessage());
 	 //return okdone; 
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getPFTPTransferBytes() is used if the file transfer was successful. 
// It gets the transfered bytes from the pftp output.
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static double getMSSTransferBytes(String ftptransferlog) {

  double transferBytesSize = 0;


/*
ftp> pput /home/dm/users/u1/asim/src/ppdg/hrm-3.0/hrm/hsi.out /nersc/gc5/asim/x/
hsi.out
200 Command Complete (43, /nersc/gc5/asim/x/hsi.out, 0, 1, 1048576).
200 Command Complete.
150 Transfer starting.
226 Transfer Complete.(moved = 43).
43 bytes sent in 0.23 seconds (0.19 Kbytes/s)
200 Command Complete.
*/

  try {
    FileInputStream fis = new FileInputStream(ftptransferlog);
    BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
    String ref = ""; 
    while((ref=bis.readLine()) != null) {
      int idx = ref.indexOf("bytes sent in");
      if(idx != -1) {
        StringTokenizer tokens = new StringTokenizer(ref.trim());
        int i = 0;
        while (tokens.hasMoreTokens()) {
           String temp = tokens.nextToken();
           if(i == 0) {      
             try {
		        Double d = new Double(temp.trim());
                transferBytesSize = d.doubleValue();
                return transferBytesSize;
             }catch(NumberFormatException nfe) {
                util.printMessage("NumberFormatException " + nfe.getMessage(),
					logger);
	         }
           }
           i++;
        }
      }
      idx = ref.indexOf("(");
      if(idx != -1) {
        int idx2 = ref.indexOf("bytes");
        if(idx2 != -1) {
           String temp = ref.substring(idx+1,idx2);
           try {
             Double d = new Double(temp.trim());
             transferBytesSize = d.doubleValue();
             return transferBytesSize;
           }catch(NumberFormatException nfe) {
              util.printMessage("NumberFormatException " + nfe.getMessage(),
					logger);
	       }
        }
      }
    }
    bis.close();
    fis.close();
  }catch(Exception e) {
    util.printMessage("\nlog file " + ftptransferlog  + " could not be opened.", 
	logger);
  }
  return transferBytesSize;
    
}

public static void removeOldLogFiles(String logPath, 
		int keepLogFiles) throws Exception {

   File f = null;
   if(!logPath.equals("")) {
     f = new File(logPath);
   }
   else {
     f = new File(".");
   }
   if(!f.exists()) {
      throw new Exception("Given logPath does not exists " + logPath);
   }
   if(!f.isDirectory()) {
     throw new Exception("Given logPath is not a directory " + logPath);
   }

   File[] lists = f.listFiles(); 
   for(int i = 0; i < lists.length; i++) {
     File ff = lists[i];
     if(ff.getName().startsWith("srm_mss")) {
       long time = ff.lastModified();
       Calendar c = new GregorianCalendar();
       int today = c.get(Calendar.DAY_OF_MONTH);
       int thisMonth = c.get(Calendar.MONTH);
       c.setTime(new Date(time));
       int oldDay = c.get(Calendar.DAY_OF_MONTH);
       if(today >= oldDay) {
         if(today-keepLogFiles > oldDay) {
           ff.delete();
         }
         else {
          ; //you keep the files
         }
       }
       else {
         //oldday is greater than today
         int lastMonth = c.get(Calendar.MONTH);
         int lastDayOfMonth = 30;
         if((thisMonth-1) == lastMonth) {
           if(lastMonth == 1) { //febrauary
             lastDayOfMonth = 28;
           }
           else if(lastMonth %2 == 0) {   
             lastDayOfMonth = 31;  
           }
           if(lastDayOfMonth+today-keepLogFiles > oldDay) {
             ff.delete();
           }
         }
         else {
           //remove the file it may be very old
           ff.delete();
         }
       }
     }
   }
   
}

public static MSS_MESSAGE findTapeId(String srmmssPath, 
		String MSSHost, int MSSPort,
		String source, String target, FileObj fObj,
		SRM_ACCESS_TYPE mssType, SRM_ACCESS_INFO accessInfo,
                SRM_MSS srm_mss, int debugLevel, String logPath) {


  String outFile=""; 
  String proxyFile="";

  if(!logPath.equals("")) {
    outFile = logPath+"/"+"srm_mss.tapeid."+fObj.getSourcePath()+"."+
		System.currentTimeMillis();
    proxyFile = logPath+"/"+"srm_mss.tapeid."+fObj.getSourcePath()+"."+
		System.currentTimeMillis()+".proxy";
  }
  else {
    outFile = "srm_mss.tapeid."+fObj.getSourcePath()+"."+
		System.currentTimeMillis();
    proxyFile = "srm_mss.tapeid."+fObj.getSourcePath()+"."+
		System.currentTimeMillis()+".proxy";
  }

  MSS_MESSAGE result = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;

  if((mssType == SRM_ACCESS_TYPE.SRM_ACCESS_GSI) ||
    (mssType == SRM_ACCESS_TYPE.SRM_ACCESS_ENCRYPT)) {
      try {
        writeProxyFile(proxyFile,accessInfo.getPasswd());
        checkProxyIsValid(proxyFile,false);
      }catch(Exception ee) {
        util.printMessage("\nERROR : could not write proxy " + proxyFile,
            logger);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        result  = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
        return result;
      }

      try {
      File script = File.createTempFile("script","",srm_mss.scriptPathDir);
      //script.deleteOnExit();
      PrintWriter writer = new PrintWriter(new FileWriter(script,false),true);
      writer.println("#!/bin/sh\n\n");
      writer.println("export X509_USER_PROXY=" + proxyFile + "\n");
      writer.println(srmmssPath + " \"out " + outFile + "; ls -V " + 
			source + "\""); 
      writer.close();

      if(debugLevel >= 1000) {
        util.printMessage("\n\n#!/bin/sh\n\n", logger);
        util.printMessage("export X509_USER_PROXY=" + proxyFile, logger);
        util.printMessage(srmmssPath + " \"out " + outFile + "; ls -V " + 
			source + "\"", logger); 
      }

      boolean ok = false;
      Thread t = new Thread();

      while (!ok) {
        try {
          if(script.exists()) {
            Process p0 =
              Runtime.getRuntime().exec("chmod 700 "+ script.getAbsolutePath());
            //if(p0.exitValue() == 0) {
            if(p0.waitFor() == 0) {
               p0.destroy();
               boolean useEnvp = false;
               long startTime = System.currentTimeMillis();
               ExecScript process = 
                 new ExecScript(script.getAbsolutePath(), "", true,srm_mss,null);
               process.execCommand(useEnvp);
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

     try {
       if(script.exists()) { 
        script.delete();
       }
     }catch(Exception ioe) {
       util.printMessage("\nException " + ioe.getMessage(),logger);
     }


      long endTime = System.currentTimeMillis();

       File ftplog = new File(outFile);
       if(ftplog.length() == 0) {
        util.printMessage("\nProbably some problem in executing script.",
              logger);
        util.printMessage("\nlog file is empty " + outFile,logger);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        result = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
        return result;
       }
      }catch(Exception e) {
        util.printMessage("\nlog file is empty " + outFile,logger);
        fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
        result = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
        return result; 
      }

     try {
       FileInputStream fis = new FileInputStream(outFile);
       BufferedReader bis = new BufferedReader(new InputStreamReader(fis));

       String temp = source; 
       int idx = source.lastIndexOf("/");
       if(idx != -1) {
         temp = source.substring(idx+1);
       }
       String ref = "";
       while((ref = bis.readLine()) != null) {
          if(ref.trim().equals("\n") || ref.trim().equals("\r") ||
             ref.trim().equals("!") || ref.trim().equals("#")) {
             continue;
          }
          idx = ref.indexOf(temp);
          int i = 0;
          if(idx != -1) {
            StringTokenizer tok = new StringTokenizer(ref);  
            while(tok.hasMoreTokens()) {
              String tt = tok.nextToken();
              if(i == 7) {  
                try {
                  long fileSize =  new Long(tt).longValue();
                  fObj.setFileSize(fileSize);
                }catch(NumberFormatException nfe) {}   
              }
              i++;
            }
          }
          idx = ref.indexOf("PV List:"); 
          if(idx != -1) {
            String tt = ref.substring(idx+9);
            fObj.setTapeId(tt.trim());
            //fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
            result = MSS_MESSAGE.SRM_MSS_TRANSFER_DONE;
          }
          idx = ref.indexOf("HPSS_ENOENT");
          if (idx != -1) {
            fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
            result = MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH;
            return result;
          }
          idx = ref.indexOf("HPSS_EACCES");
          if(idx != -1) {
            fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED);
            result = MSS_MESSAGE.SRM_MSS_AUTHORIZATION_FAILED;
            return result;
          }
       }
       bis.close();
       fis.close();
       if(debugLevel <= 200) {
         File f = new File(outFile);
         if(f.exists()) { 
			f.delete(); 
		 }
         f = new File(proxyFile);
         if(f.exists()) { 
			f.delete(); 
		 }
       }
       return result; 
      }catch(Exception e) {
         fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
         result = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
         return result;
      }
     } else {
       util.printMessage("\nAccess type not supported ", logger);
       fObj.setPFTPStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
       result = MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR;
       return result;
     }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkProxyIsValid
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static void checkProxyIsValid(String proxyFile,  boolean showOutput) 
  throws SRM_MSS_Exception {

   try {
    GlobusCredential gCreds = new GlobusCredential(proxyFile);
    if(showOutput) {
       util.printMessage("\nSubject " + gCreds.getSubject(), logger);
       util.printMessage("timeleft " + gCreds.getTimeLeft(), logger);
    }
    GSSCredential credential = new GlobusGSSCredentialImpl
                          (gCreds, GSSCredential.INITIATE_AND_ACCEPT);
    if(credential.getRemainingLifetime() == 0) {
        throw new SRM_MSS_Exception 
            ("Credential expired, please renew your credentials");
    }
   }catch(Exception e) {
     throw new SRM_MSS_Exception (e.getMessage());
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeProxyFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static void writeProxyFile (String proxyFile, String passwd) 
   throws Exception {

    FileOutputStream fos = new FileOutputStream (proxyFile);
    BufferedWriter out = new BufferedWriter (new OutputStreamWriter (fos));
    out.write(passwd);
    out.flush();
    out.close();
    fos.close();

    boolean ok = false;
    Thread t = new Thread();
    
    while (!ok) {
      try {
        File f = new File(proxyFile);
        if(f.exists()) {
          //chmod proxy file to 600
          Process p0 = Runtime.getRuntime().exec("chmod 600 " + proxyFile);
          //if(p0.exitValue() == 0) {
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

}
