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

package gov.lbl.srm.client.util;


import javax.swing.*;
import gov.lbl.srm.client.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.main.*;


public class ShowException {
   private static String _msg;
   private static JFrame _frame;
   private static int userInput;   

   public static void logDebugMessage(Log logger, Exception e) {
      StackTraceElement[] strace = e.getStackTrace ();
      logger.debug("++++++++++++++++++++++++++++++++++++");
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

   public static void showMessageDialog(JFrame frame, String msg) {
      if(frame == null) {
          System.out.println(msg); return; //for non gui option
      }
      _frame = frame;
      _msg = msg;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
	   JOptionPane.showMessageDialog(ShowException._frame,
	     _msg, _msg, JOptionPane.ERROR_MESSAGE);
	   }
      });
   }

   public static void showConfirmDialog(JFrame frame, SharedObjectLock sLock, 
		String msg) {
      if(frame == null) {
          System.out.println(msg); return ; //for non gui option
      }
      _frame = frame;
      _msg = msg;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
	      int value = JOptionPane.showConfirmDialog(ShowException._frame,
	     _msg, _msg, 
         JOptionPane.YES_NO_CANCEL_OPTION,
         JOptionPane.QUESTION_MESSAGE);
         ShowException.userInput = value;
	   }
      });
      sLock.setIncrementCount();
   }

   public static int getUserInput() {
      return userInput;
   }
}
