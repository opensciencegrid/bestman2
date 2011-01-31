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

package gov.lbl.srm.transfer.mss;

import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

public class util {

    public static void printMessage(String msg) { 
       System.out.println(msg);
    }

    public static void printMessage(String msg, Log logger, int debugLevel) { 
       if(debugLevel >= 6000) {
         System.out.println(msg);
       }
    }

    public static void printMessage(String msg, Log logger) { 
      try {
       //System.out.println(msg);
       if(logger != null) {
	     logger.debug(msg);
       }
      }catch(Exception e) {}
    }

    public static void printMessageOnlyLog(String msg, Log logger) { 
       if(logger != null) {
	     logger.debug(msg);
       }
    }

    public static void printMessageNL(String msg, Log logger) { 
       System.out.print(msg);
    }

    public static void printStackTrace(Exception e, Log logger) {
      StackTraceElement[] strace = e.getStackTrace ();
      int length = strace.length;
      if(strace.length > 5) length = 5;
      if(logger != null) {
        logger.debug("++++++++++++++++++++++++++++++++++++");
        for(int i = 0; i < length; i++) {
           StackTraceElement ss =  strace[i];
           logger.debug("ClassName "  + ss.getClassName());
           logger.debug("FileName "   + ss.getFileName());
           logger.debug("LineNumber " + ss.getLineNumber());
           logger.debug("MethodName " + ss.getMethodName());
           logger.debug("...................................");
        }
        logger.debug("++++++++++++++++++++++++++++++++++++");
      }
      else {  
       for(int i = 0; i < length ; i++) {
         StackTraceElement ss =  strace[i];
         System.out.println("ClassName "  + ss.getClassName());
         System.out.println("FileName "   + ss.getFileName());
         System.out.println("LineNumber " + ss.getLineNumber());
         System.out.println("MethodName " + ss.getMethodName());
         System.out.println("...................................");
       }
      }
    }
 
    public static String rot13(String in) {

       int abyte = 0;

        StringBuffer tempReturn = new StringBuffer();

        for (int i=0; i<in.length(); i++) {

            abyte = in.charAt(i);
            int cap = abyte & 32;
            abyte &= ~cap;
            abyte = ( (abyte >= 'A') && 
	     (abyte <= 'Z') ? ((abyte - 'A' + 13) % 26 + 'A') : abyte) | cap;
            tempReturn.append((char)abyte);
        }


        return tempReturn.toString();
        
    }
}

