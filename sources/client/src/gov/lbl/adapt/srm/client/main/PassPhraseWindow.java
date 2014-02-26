/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

import java.text.DecimalFormat;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.event.*;
import javax.swing.border.*;

import org.globus.gsi.bc.*;
import org.globus.gsi.*;
import org.globus.util.*;
import java.io.*;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.interfaces.*;

import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class PassPhraseWindow extends JFrame implements actionIntf, 
	colorIntf, threadIntf {

   private ActionCallBack actionCallBack; 
   private static JPasswordField passwordField = new JPasswordField();
   private XMLParseConfig pConfig;
   private static PassPhraseWindow _passPhraseWindow;
   private static SRMClientIntf _parent;
   private static JButton ok = new JButton("Ok");
   private static JButton cancel = new JButton("Cancel");
   private static Log logger;

   private PassPhraseWindow (SRMClientIntf parent, 
	XMLParseConfig pConfig) {
      JPanel panel = (JPanel) getContentPane ();
      panel.setLayout(new SpringLayout());
      panel.setBackground(bgColor);
      JPanel p1 = new JPanel(new SpringLayout());
      JPanel p2 = new JPanel(new SpringLayout());
      p1.setBackground(bgColor);
      p2.setBackground(bgColor);
      actionCallBack = new ActionCallBack(this);
      _parent = parent;
      JLabel label = new JLabel("Enter your passphrase : ");
      label.setBackground(bgColor);
      passwordField.setEchoChar('*');
      p1.add(label);
      p1.add(passwordField);
      SpringUtilities.makeCompactGrid(p1,1,2,1,1,1,1);
      ok.setActionCommand("ok");
      ok.addActionListener(actionCallBack);
      cancel.setActionCommand("cancel");
      cancel.addActionListener(actionCallBack);
      p2.add(ok);
      p2.add(cancel);
      this.pConfig = pConfig;
      SpringUtilities.makeCompactGrid(p2,1,2,1,1,1,1);
      panel.add(p1);
      panel.add(p2);
      SpringUtilities.makeCompactGrid(panel,2,1,1,1,1,1);
      try {
        ClassLoader cl = this.getClass().getClassLoader();
        Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.PassPhraseWindow");
        logger = LogFactory.getLog(c.getName());
      }catch(ClassNotFoundException cnfe) {
        System.out.println("ClassNotFoundException ");
        //throw new SRMClientException(cnfe.getMessage());
      }
      setContentPane(panel);
   }

   public char[] getPassword() {
     return passwordField.getPassword();
   }

   public void processActionEvent (ActionEvent evt) {
       String command = evt.getActionCommand();
       if(command.equals("ok")) {
           char[] ss = passwordField.getPassword();
           String str = new String(ss);
           if(str == null || str.length() == 0) {
              logger.debug("Password is not given");
              ShowException.showMessageDialog(this,"Password is not given");
           }
           else {
                ThreadCallBack tb = new ThreadCallBack(this);
                tb.setFileName(str);
                tb.start();
           }
       }
       this.hide();
   }

   public void processThreadRequest (String str) { 
     try { 
        _parent.createProxy(str);
     }catch(Exception e) {
       //ShowException.logDebugMessage(logger,e);
       ShowException.showMessageDialog(this,"Exception : " + e.getMessage());
     }
   }

   public static PassPhraseWindow getPassPhraseWindow(SRMClientIntf parent, 
	XMLParseConfig pConfig, int type) {
      if(_passPhraseWindow == null) {
         _passPhraseWindow = new PassPhraseWindow(parent, pConfig);
      }
      _passPhraseWindow.passwordField.setText("");
      _passPhraseWindow.pConfig = pConfig;
      if(type == 1) {
        ok.setEnabled(false);
        cancel.setEnabled(false);
      }
      else if(type == 0){
        ok.setEnabled(true);
        cancel.setEnabled(true);
      }
      return _passPhraseWindow;
   }
}
