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

// -----------------------------------------------------------------------------
// util.java
// -----------------------------------------------------------------------------

package gov.lbl.srm.client.main;

import java.io.*;
import java.util.*;

import gov.lbl.srm.client.intf.PrintIntf;
import gov.lbl.srm.StorageResourceManager.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

public class util {

   public static HashMap parseRemoteTransferInfo(String remoteTransferInfo) {

     StringTokenizer stk = new StringTokenizer(remoteTransferInfo,",");
      HashMap map = new HashMap();
      while(stk.hasMoreTokens()) {
        String temp = stk.nextToken();
        int idx = temp.indexOf(":");
        if(idx != -1) {
           String key = temp.substring(0,idx);
           String value = temp.substring(idx+1);
           if(key.equalsIgnoreCase("buffersize") ||
              key.equalsIgnoreCase("parallelism") ||
              key.equalsIgnoreCase("dcau") ||
              key.equalsIgnoreCase("protection") ||
              key.equalsIgnoreCase("guc")) {
              map.put(key,value);
           }
           else {
              System.out.println("SRM-CLIENT: Please check remotetransferinfo"+
                " parameter, one ore more keywords are not correct ");
              System.exit(93);
           }
        }
      }
      return map;
    }


    public static int mapStatusCode(TStatusCode code) {
       //90 , connection refused
       //91 , GSI mapping not found
       //92 , general unpreditable exception
       //93 , input error
       //94 , other error, not reached SRM yet.
       //95, connection did not go through for long time
       //96, proxy type mismatch
       if(code == null) return 100; //SRM returned no status, but it is a failure
       else {
          String str = code.toString();
          return mapStatusCode(str);
       }
     }

     public static int mapStatusCode(String str) {

          str = str.trim();
          int value = 0;
          if(str.equals("SRM_SUCCESS")) {
            value = 0;
          }
          else if(str.equals("SRM_FAILURE")) {
            value = 51;
          }
          else if(str.equals("SRM_AUTHENTICATION_FAILURE")) {
            value = 52;
          }
          else if(str.equals("SRM_AUTHORIZATION_FAILURE")) {
            value =  53;
          }
          else if(str.equals("SRM_INVALID_REQUEST")) {
            value = 54;
          }
          else if(str.equals("SRM_INVALID_PATH")) {
            value = 55;
          }
          else if(str.equals("SRM_FILE_LIFETIME_EXPIRED")) {
            value = 56;
          }
          else if(str.equals("SRM_SPACE_LIFETIME_EXPIRED")) {
            value = 57;
          }
          else if(str.equals("SRM_EXCEED_ALLOCATION")) {
            value = 58;
          }
          else if(str.equals("SRM_NO_USER_SPACE")) {
            value = 59;
          }
          else if(str.equals("SRM_NO_FREE_SPACE")) {
            value = 60;
          }
          else if(str.equals("SRM_DUPLICATION_ERROR")) {
            value = 61;
          }
          else if(str.equals("SRM_NON_EMPTY_DIRECTORY")) {
            value = 62;
          }
          else if(str.equals("SRM_TOO_MANY_RESULTS")) {
            value = 63;
          }
          else if(str.equals("SRM_INTERNAL_ERROR")) {
            value = 64;
          }
          else if(str.equals("SRM_FATAL_INTERNAL_ERROR")) {
            value = 65;
          }
          else if(str.equals("SRM_NOT_SUPPORTED")) {
            value = 66;
          }
          else if(str.equals("SRM_REQUEST_QUEUED")) {
            value = 67;
          }
          else if(str.equals("SRM_REQUEST_INPROGRESS")) {
            value = 68;
          }
          else if(str.equals("SRM_REQUEST_SUSPENDED")) {
            value = 69;
          }
          else if(str.equals("SRM_ABORTED")) {
            value = 70;
          }
          else if(str.equals("SRM_RELEASED")) {
            value = 71;
          }
          else if(str.equals("SRM_FILE_PINNED")) {
            value = 72;
          }
          else if(str.equals("SRM_FILE_IN_CACHE")) {
            value = 73;
          }
          else if(str.equals("SRM_SPACE_AVAILABLE")) {
            value = 74;
          }
          else if(str.equals("SRM_LOWER_SPACE_GRANTED")) {
            value = 75;
          }
          else if(str.equals("SRM_DONE")) {
            value = 76;
          }
          else if(str.equals("SRM_PARTIAL_SUCCESS")) {
            value = 77;
          }
          else if(str.equals("SRM_REQUEST_TIMED_OUT")) {
            value = 78;
          }
          else if(str.equals("SRM_LAST_COPY")) {
            value = 79;
          }
          else if(str.equals("SRM_FILE_BUSY")) {
            value = 80;
          }
          else if(str.equals("SRM_FILE_LOST")) {
            value = 81;
          }
          else if(str.equals("SRM_FILE_UNAVAILABLE")) {
            value = 82;
          }
          else if(str.equals("SRM_UNKNOWN_ERROR")) {
            value = 83;
          }
          else if(str.equals("") || str.equals("SRM_RETURNED_NO_STATUS")) {
            value = 100; //fake one
          }
          else if(str.equals("INPUT_ERROR")) {
            value = 93; //fake one
          }
          else {
            value = 84;
          }
          //System.out.println("ExitCode="+value);
          return value;
       }

    public static void printMessage(String msg, Log logger, boolean silent) { 
       if(silent) {;}
       else if(!silent || logger == null) {
         System.out.println(msg);
       }
       /*
       if(logger != null) {
         if(silent) {
	       logger.debug(msg);
         } 
       }
       */
    }

    public static void printMessage
        (String str, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessage(str);
     }
    }

    public static void printMessageHL
        (String str, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHL(str);
     }
    }

    public static void printMessageHException
        (String str, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHException(str);
     }
    }

    public static void printHException
        (Exception e, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printHException(e);
     }
    }


    public static void printMessageNL(String msg, Log logger,boolean silent) { 
       if(!silent) {
         System.out.print(msg);
       }
    }

    public static void printEventLog(
            java.util.logging.Logger theLogger, 
			String eventName, Vector inputVec, boolean silent,
				boolean useLog) {
    try {
       if(silent || useLog) {
          if(theLogger != null) {
             Vector cvec = (Vector) inputVec.clone();
             int size = cvec.size();
             Object [] param = new Object[size];
 
             for(int i = 0; i < size; i++) {
               param[i] = (String) cvec.elementAt(i);
             }
             theLogger.log(java.util.logging.Level.FINE, eventName,
                (Object[]) param);
             cvec=null;
             param=null;
           }//end if
        }//end if
     }catch(Exception e) {
        System.out.println("Client Exception during logging, can be ignored "
           +e.getMessage());
     }
    }

    public static void printEventLogException(
            java.util.logging.Logger theLogger, 
			String eventName, Throwable e) {
        if(theLogger == null) return; 

        Vector stackMsg = new Vector();
        if(e != null) {
   	  StackTraceElement[] stacks = e.getStackTrace();
	  stackMsg.addElement(e.getMessage()+"\n");
	  for (int i=0; i<stacks.length; i++) {
	    stackMsg.addElement(stacks[i].toString()+"\n");
	  }
          Object[] param = new Object[stackMsg.size()];
          for(int i = 0; i < stackMsg.size(); i++) {
            param[i] = (String) stackMsg.elementAt(i);
          }
          theLogger.log(java.util.logging.Level.FINE, eventName, 
	    (Object[]) param);
        }   
        else {
          Object[] param = new Object[1];
          param[0] = "Null Exception";
          theLogger.log(java.util.logging.Level.FINE, eventName, 
	    (Object[]) param);
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
    return str;
  }
  else {
    throw new Exception("SRM-CLIENT: parseLocalSourceFileForPath:SURL not in correct format " + str);
  }
}


    public static void printStackTrace(Exception e, Log logger) {
      //StackTraceElement[] strace = e.getStackTrace ();
      System.out.println("++++++++++++++++++++++++++++++++++++");
      System.out.println("Exception : " + e.getMessage());
      e.printStackTrace();
      /*
      if(logger != null) {
        logger.debug("++++++++++++++++++++++++++++++++++++");
        logger.debug("Exception : " + e.getMessage());
        for(int i = 0; i < strace.length; i++) {
           StackTraceElement ss =  strace[i];
           logger.debug("ClassName "  + ss.getClassName());
           logger.debug("FileName "   + ss.getFileName());
           logger.debug("LineNumber " + ss.getLineNumber());
           logger.debug("MethodName " + ss.getMethodName());
           logger.debug("...................................");
        }
        logger.debug("++++++++++++++++++++++++++++++++++++");
      }
      int length = strace.length;
      if(strace.length > 5) length = 5;
      for(int i = 0; i < length ; i++) {
         StackTraceElement ss =  strace[i];
         System.out.println("ClassName "  + ss.getClassName());
         System.out.println("FileName "   + ss.getFileName());
         System.out.println("LineNumber " + ss.getLineNumber());
         System.out.println("MethodName " + ss.getMethodName());
         System.out.println("...................................");
      }
      */
    }

public static boolean isConnectionTimeOut(long startTimeStamp, 
	int retryAllowed) {
  long currentTimeStamp = System.currentTimeMillis();
  //System.out.println("currenttimestamp="+currentTimeStamp);
  //System.out.println("starttimestamp="+startTimeStamp);
  //System.out.println("timeout="+
  	//!(currentTimeStamp <= startTimeStamp+(retryAllowed*1000)));
  if(currentTimeStamp <= (startTimeStamp+(retryAllowed*1000))) {
    return false;
  }
  return true;
}


public static boolean isRetryOk(long startTimeStamp, int retryAllowed) {
  /*
  Date d = new Date();
  int minutes = d.getMinutes();
  int seconds = d.getSeconds();
  int currentTimeStamp = minutes*60+seconds;
  if(currentTimeStamp < startTimeStamp) {
    currentTimeStamp = currentTimeStamp+startTimeStamp;
  }
  if(currentTimeStamp <= (startTimeStamp+retryAllowed))) {
     return true;
  }
  return false;
  */
  long currentTimeStamp = System.currentTimeMillis();
  //System.out.println("CurrentTimeStamp="+currentTimeStamp);
  //System.out.println("StartTimeStamp="+(startTimeStamp+retryAllowed*1000));
  if(currentTimeStamp <= (startTimeStamp+(retryAllowed*1000))) {
    return true;
  }
  return false;
}

public static int getElapsedTime (Date requestDate, Date statusDate) {
   int rHours = requestDate.getHours();
   int rMinutes = requestDate.getMinutes();
   int rSeconds = requestDate.getSeconds();

   int rTime = rHours*60*60+rMinutes*60+rSeconds;

   int sHours = statusDate.getHours();
   int sMinutes = statusDate.getMinutes();
   int sSeconds = statusDate.getSeconds();

   int sTime = sHours*60*60+sMinutes*60+sSeconds;

   
   if(sTime < rTime) sTime = rTime+1;
   
   return sTime- rTime; 
}

public static long startTimeStamp() {
  /*
  Date d = new Date();
  int minutes = d.getMinutes();
  int seconds = d.getSeconds();
  int currentTimeStamp = minutes*60+seconds;
  return currentTimeStamp;
  */
  return System.currentTimeMillis();
}

public static String getDetailedDateWithNoHyphen () {
   Date tDate = new Date();
   Random random = new Random();
   String detailedLogDate = (tDate.getMonth()+1)+""+tDate.getDate()+
			""+tDate.getHours()+""+tDate.getMinutes()+""+tDate.getSeconds();
   return detailedLogDate+"-"+random.nextInt((int)4.294967296E9);
} 

public static String getDetailedDate () {
   Date tDate = new Date();
   Random random = new Random();
   String detailedLogDate = (tDate.getMonth()+1)+"-"+tDate.getDate()+
			"-"+tDate.getHours()+"-"+tDate.getMinutes()+
		    "-"+tDate.getSeconds();
   return detailedLogDate+"-"+random.nextInt((int)4.294967296E9);
} 


} 
