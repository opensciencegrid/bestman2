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

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.*;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Properties;
import java.io.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

import javax.swing.AbstractAction;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.*;
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import javax.swing.Action;
import javax.swing.tree.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import java.util.Vector;
import javax.swing.border.*;
import javax.swing.event.UndoableEditEvent;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.imageio.ImageIO;

import java.util.*;

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClient extends JFrame
	implements colorIntf, listIntf, mouseIntf, 
	tabChangeIntf, threadIntf, actionIntf, SRMClientIntf
{

private TabChangedCallBack tabChangedCallBack;
private ActionCallBack actionCallBack;
private MouseCallBack mouseCallBack;
private MyJTabbedPane reqTabbedPane = new MyJTabbedPane("request");
private MyJTabbedPane transferTabbedPane = new MyJTabbedPane("transfer");
private MyJTabbedPane operationTabbedPane = new MyJTabbedPane("operation");
private GSSCredential mycred;
private XMLParseConfig pConfig = new XMLParseConfig();
private Hashtable ht = new Hashtable();

private JMenuItem importItem = null;
private JMenuItem saveReportItem = null;
private JMenuItem saveAsReportItem = null;
private JMenuItem createItem = null;
private JMenuItem configItem = null;
private JMenuItem overwriteMenuItem = null;
private JSplitPane splitPane = null;

private JButton transferB = new JButton("Transfer");
private JButton cancelB = new JButton("Cancel");
private JButton closeB  = new JButton("Close");
private JButton saveReport  = new JButton("Save Report");
private JButton browseButton  = new JButton("Browse");
private JTextField targetDirTF = new JTextField();

private static Log logger;  
private String targetDir="";

private JPanel mPanel = new JPanel(new BorderLayout());
private boolean srmFileTransferWindow = false;
final private String[] columnNames = {"Property","Browse a file"};
private Properties properties = new Properties();
private String configFileLocation = "./config.xml";
private String _password ="";
private JSpinner concurrency = new JSpinner(); 
private JSpinner parallelism = new JSpinner(); 
private JSpinner bufferSize = new JSpinner(); 
private int blockSize;
private boolean _lock;
private static String passwd;
private boolean requestDone;

private TransferThread tThread;
private String logFileLocation="";
private int _retryAllowed=3;
private int _retryTimeOut=300; //in seconds
private boolean _debug;
private boolean silent=false;
private int maximumFilesPerRequest = 0;
private int totalFilesPerRequest;
private int totalSubRequest;
private Vector ridList = new Vector ();

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClient
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClient(String[] args) throws SRMClientException {

  ClassLoader cl = this.getClass().getClassLoader();

  /*
  PropertyConfigurator.configure(
	cl.getResource("logs/log4j_srmclient.properties"));
  */

  try {
  Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClient");
  logger = LogFactory.getLog(c.getName());
  }catch(ClassNotFoundException cnfe) {
    System.out.println("ClassNotFoundException ");
    throw new SRMClientException(cnfe.getMessage());
  }

  

  for(int i = 0; i < args.length; i++) {
    if(args[i].equals("-conf") && i+1 < args.length) {
      configFileLocation = args[i+1];
      i++;
    }
    else if(args[i].equals("-targetdir") && i+1 < args.length) {
      targetDir = args[i+1];
      i++;
    }
    else if(args[i].equals("-gui")) {
      ;
    }
    else if(args[i].equals("-logdir") && i+1 < args.length) {
      logFileLocation = args[i+1];
      try {
       File f = new File(logFileLocation);
       if(!f.exists() || !f.isDirectory()) {
        System.out.println("Given logdir location is not valid");
        System.exit(1);
       }
       }catch(Exception ioe) {
         //util.printEventLogException(_theLogger,"",ioe);
         System.out.println("IOException : " + ioe.getMessage());
       }
       i++;
    }
    else {
      showUsage (true);
    }
  }

  //Make sure we have nice window decorations.
  JFrame.setDefaultLookAndFeelDecorated(true);

  mPanel.setBackground(bgColor);
  mPanel.setBorder(new EtchedBorder(30, Color.white, bdColor));

  tabChangedCallBack = new TabChangedCallBack (this);

  reqTabbedPane.addChangeListener(tabChangedCallBack);
  transferTabbedPane.addChangeListener(tabChangedCallBack);
  operationTabbedPane.addChangeListener(tabChangedCallBack);

  actionCallBack = new ActionCallBack(this);

  mouseCallBack = new MouseCallBack(this);
  
  mPanel.add(createToolBar(), BorderLayout.NORTH);

  this.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
              System.exit(1);
      }
   });

  this.addWindowFocusListener(new WindowFocusListener() {
    public void windowGainedFocus(WindowEvent evt) {
      //SRMClient.this.toFront();
    }
    public void windowLostFocus(WindowEvent evt) {
    }
  });

  properties.put("user-cert", pConfig.getUserCert());
  properties.put("user-key", pConfig.getUserKey());
  properties.put("proxy-file", pConfig.getProxyFile());
  properties.put("last-target-location",pConfig.getLastTargetLocation());

  if(!isConfigExists()) {
    logger.debug(
    	"Given config file does not exists, " + configFileLocation + 
        " please use menu Tools->Config to" + 
	" create the config file first.");
    /*
    ShowException.showMessageDialog(this,
    	"Given config file does not exists, " + configFileLocation + 
        " please use menu Tools->Config to" + 
	" create the config file first.");
    */
  }
  else {
   try {
    pConfig = new XMLParseConfig(configFileLocation);
    properties.put("user-cert", pConfig.getUserCert());
    properties.put("user-key", pConfig.getUserKey());
    properties.put("proxy-file", pConfig.getProxyFile());
    properties.put("last-target-location",pConfig.getLastTargetLocation());
   }catch(Exception e) {
     //util.printEventLogException(_theLogger,"",e);
     ShowException.logDebugMessage(logger,e);
     //ShowException.showMessageDialog(this,e.getMessage());
   }
  }

  String lastTargetLoc = (String) properties.get("last-target-location");

  operationTabbedPane.setBackground(bgColor);
  mPanel.add(operationTabbedPane, BorderLayout.CENTER);

  this.getContentPane().add(mPanel);

  JMenuBar menuBar = createMenuBar ();
  setJMenuBar(menuBar);

  importItem.setEnabled(true);
  configItem.setEnabled(true);
  saveReportItem.setEnabled(true);
  saveAsReportItem.setEnabled(true);
  createItem.setEnabled(true);
  transferB.setEnabled(true);
  cancelB.setEnabled(true);
  closeB.setEnabled(true);
  //saveReport.setEnabled(true);
  targetDirTF.setEnabled(true);
  if(!targetDir.equals("")) {
    targetDirTF.setText(targetDir);
  }
  if(!lastTargetLoc.equals("")) {
    targetDirTF.setText(lastTargetLoc);
  }
  else {
    targetDirTF.setText("");
  }
  JPanel p = new JPanel(new BorderLayout());
  p.setBackground(bgColor);
  operationTabbedPane.add("SRMFileTransfer",p);
  srmFileTransferWindow = true;

  this.validate ();

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//@@@ start method implemented for interface SRMClientIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void srmFileFailure(int idx, String message) {
}

public void setRequest(Request req) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initiatePullingFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void initiatePullingFile(FileInfo fInfo) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setRequestInformation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public void setRequestInformation(String status, String explanation) {
}

public String getRequestStatus() { return ""; }

public String getRequestExplanation() { return ""; }

public void setRequestTimedOut(boolean b) { }
public boolean getRequestTimedOut() { return false; }

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRequestDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public void setRequestDone(boolean b, boolean allFilesFailed) {
  requestDone = b;
  if(allFilesFailed) { 
    enableTransferButton(b,false); 
  }
}

public  void setGateWayModeEnabled(boolean b) {}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setTotalFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public void setTotalFiles (int tFiles) {}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void enableTransferButton (boolean b, boolean ok) {
  if(b) { 
   tThread = null;
  }
  transferB.setEnabled(b);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isConfigExists
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isConfigExists () {
 try {
  File f = new File(configFileLocation);
  if(!f.exists()) return false;
 }catch(Exception e) { 
   //util.printEventLogException(_theLogger,"",e);
   ShowException.logDebugMessage(logger,e);
   ShowException.showMessageDialog(this, "Exception : " + e.getMessage());
 }
 return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void showUsage (boolean b) {
 if(b) {
  System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
  System.out.println("java gov.lbl.srm.client.SRMClient <options>");
  System.out.println("\t\t -conf location to config.xml file");
  System.out.println("\t\t\t default ./config.xml");
  System.out.println("\t\t -logdir location to create status file");
  System.out.println("\t\t\t default in the same directory");
  System.out.println("\t\t -targetdir location ");
  System.out.println("\t\t\t not required for put requests");
  System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
 }
 System.exit(1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getConfigFileLocation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getConfigFileLocation () {
  return configFileLocation;
}

public Properties getProperties() {
  return properties;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getConfig
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

public int getMaximumFilesPerRequest() {
  return maximumFilesPerRequest;
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

   String proxyPath = properties.getProperty("proxy-file");

   logger.debug("Get Credential for proxyPath " + proxyPath);
   try {
     mycred = gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",true,"");
   }catch(Exception e) {
      logger.debug(e.getMessage());
      System.out.println(e.getMessage());
   }

   if(mycred == null) {
      logger.debug("Could not get credential for proxy " + proxyPath);
      throw new SRMClientException 
	("Could not get credential for proxy " + proxyPath + "\n" + 
         "Please check your configuration settings from menu Tools->Config.");
   }
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
        logger.debug
  	  ("User Credential expired, " +
	   "please use Menu Tools->InitProxy to renew" +
           " your credentials");
        throw new SRMClientException
        ("User Credential expired, please use Menu Tools->InitProxy to renew" +
                " your credentials");
    }

    if(remainingLifeTime <= 1800) { //if <= 30 minutes
      if(isRenewProxy()) {
        try { 
          mycred = createProxy(_password);
        }catch(SRMClientException srme) {
             ShowException.logDebugMessage(logger,srme);
             throw srme;
        }
      }  
      else {
         logger.debug("Your proxy has only " +
		remainingLifeTime + " second left.\n" +
		"Please use Menu->Tools  to renew your proxy.");
         ShowException.showMessageDialog(this, "Your proxy has only " +
		remainingLifeTime + " second left.\n" +
		"Please use Menu->Tools  to renew your proxy.");
      }
    }
   }catch(Exception e) {
      ShowException.logDebugMessage(logger,e);
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
// createProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized GSSCredential 
	createProxy (String passwd) throws SRMClientException {

 String proxyFile =  pConfig.getProxyFile();
 this.passwd = passwd;
 File f = new File(proxyFile); 
 if(f.exists()) { 
  System.out.println("file exists"); 
  SwingUtilities.invokeLater(new Runnable() {
     public void run() {
         String pFile =  pConfig.getProxyFile();
         int value = JOptionPane.showConfirmDialog(SRMClient.this,
         pFile + " already exists, do you want to overwrite", 
         pFile + " already exists, do you want to overwrite", 
         JOptionPane.YES_NO_CANCEL_OPTION,
         JOptionPane.QUESTION_MESSAGE);
         if(value == 0) {
           logger.debug("Creating proxy now ...");
           System.out.println("Creating proxy now ...");
           _lock = true; 
           try {
             String userCert = pConfig.getUserCert();
             String userKey = pConfig.getUserKey();
             gov.lbl.srm.client.util.Util.createProxy
		        (userCert, userKey, pFile, SRMClient.passwd);
             logger.debug("Created proxy.");
             System.out.println("Created proxy.");
             _lock = false;
             notifyAll(); //notify other threads waiting
           }catch(Exception e) {
             ShowException.logDebugMessage(logger,e);
             //throw new SRMClientException(e.getMessage());
           }  
         }
         else {
           logger.debug("Skipping Creating proxy.");
           System.out.println("Skipping Creating proxy.");
         }
       }
     });
   } 
   else {
     logger.debug("Creating proxy now ...");
     System.out.println("Creating proxy now ...");
     _lock = true; 
     try {
       String userCert = pConfig.getUserCert();
       String userKey = pConfig.getUserKey();
       proxyFile =  pConfig.getProxyFile();
       gov.lbl.srm.client.util.Util.createProxy
          (userCert, userKey, proxyFile, passwd);
       logger.debug("Created proxy.");
       System.out.println("Created proxy.");
       _lock = false;
       notifyAll(); //notify other threads waiting
     }catch(Exception e) {
       ShowException.logDebugMessage(logger,e);
       throw new SRMClientException(e.getMessage());
     }  
  }

  GSSCredential mycred = getCredential();
  if(tThread != null) {
   tThread.setProxy(mycred);
  }
 return mycred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLock
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean getLock ()
{
  return _lock;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// validateFrame
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void validateFrame () {
  this.validate();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getFrame
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JFrame getFrame() {
  return this;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// @@@@ end method implemented for interface SRMClientIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createMenuBar
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private JMenuBar createMenuBar () 
{
  JMenuBar mBar = new JMenuBar ();
  JMenu fileMenu = new JMenu ("File");
  importItem = new JMenuItem("Import");
  importItem.setActionCommand("import");
  importItem.addActionListener(actionCallBack);
  importItem.setEnabled(false);
  fileMenu.add(importItem);

  saveAsReportItem = new JMenuItem("saveAs");
  saveAsReportItem.setActionCommand("saveAs");
  saveAsReportItem.addActionListener(actionCallBack);
  saveAsReportItem.setEnabled(false);
  fileMenu.add(saveAsReportItem);

  saveReportItem = new JMenuItem("save");
  saveReportItem.setActionCommand("save");
  saveReportItem.addActionListener(actionCallBack);
  saveReportItem.setEnabled(false);
  fileMenu.add(saveReportItem);


  JMenuItem exitItem = new JMenuItem("Exit");
  exitItem.setActionCommand("exit");
  exitItem.addActionListener(actionCallBack);
  fileMenu.add(exitItem);

  JMenu toolsMenu = new JMenu("Tools");
  createItem = new JMenuItem("InitProxy");
  createItem.setActionCommand("init");
  createItem.addActionListener(actionCallBack);
  createItem.setEnabled(false);
  configItem = new JMenuItem("Config");
  configItem.setActionCommand("config");
  configItem.addActionListener(actionCallBack);
  configItem.setEnabled(false);
  toolsMenu.add(createItem);
  toolsMenu.add(configItem);

  JMenu operationsMenu = new JMenu("Operations");
  JMenuItem srmfileTransfer = new JMenuItem("SRMFileTransfer"); 
  srmfileTransfer.addActionListener(actionCallBack);
  srmfileTransfer.setActionCommand("srmfiletransfer");
  JMenuItem srmlsItem = new JMenuItem("SRMLs"); 
  srmlsItem.addActionListener(actionCallBack);
  srmlsItem.setActionCommand("srmls");
  JMenuItem srmprepareget = new JMenuItem("SRMPrepareToGet");
  srmprepareget.addActionListener(actionCallBack);
  srmprepareget.setActionCommand("srmprepareget");
  JMenuItem srmprepareput = new JMenuItem("SRMPrepareToPut");
  srmprepareput.addActionListener(actionCallBack);
  srmprepareput.setActionCommand("srmprepareput");

  operationsMenu.add(srmfileTransfer);
  operationsMenu.add(srmlsItem);
  operationsMenu.add(srmprepareget);
  operationsMenu.add(srmprepareput);

  JMenu optionsMenu = new JMenu("Options");
  //JMenu modeSubMenu = new JMenu("Select a transfer mode"); 

  //modeSubMenu.add(rbGetModeItem);
  //modeSubMenu.add(rbPutModeItem);
  //optionsMenu.add(modeSubMenu);

  //bgroup.add(rbGetModeItem);
  //bgroup.add(rbPutModeItem);

  //optionsMenu.add(new JSeparator());

  Integer min = new Integer(1);
  Integer max = new Integer(500);
  SpinnerNumberModel model = new SpinnerNumberModel(min,min,max,min);
  concurrency.setModel(model);
  concurrency.setBackground(bdColor);
  concurrency.setToolTipText("Select number of concurrent transfers");

  SpinnerNumberModel model_1 = new SpinnerNumberModel(min,min,max,min);
  parallelism.setModel(model_1);
  parallelism.setBackground(bdColor);
  parallelism.setToolTipText("Select number of parallel transfers");

  Integer vv = new Integer(1024);
  min = new Integer(128);
  max = new Integer(4096);
  SpinnerNumberModel model_2 = new SpinnerNumberModel(vv,min,max,min);
  bufferSize.setModel(model_2);
  bufferSize.setBackground(bdColor);
  bufferSize.setToolTipText("Set the buffer size for file transfer");

  JMenu concMenu = new JMenu("Concurrent transfers...");
  concMenu.add(concurrency);
  concMenu.setActionCommand("Concurrency");
  concMenu.addActionListener(actionCallBack);
  optionsMenu.add(concMenu);

  JMenu parMenu = new JMenu("Parallelism ...");

  parMenu.add(parallelism);
  parMenu.setActionCommand("Parallelism");
  parMenu.addActionListener(actionCallBack);
  optionsMenu.add(parMenu);

  JMenu buffMenu = new JMenu ("Buffer Size...");
  buffMenu.add(bufferSize);
  buffMenu.setActionCommand("buffersize");
  buffMenu.addActionListener(actionCallBack);
  optionsMenu.add(buffMenu);
  optionsMenu.add(new JSeparator());

  overwriteMenuItem = new JCheckBoxMenuItem("Overwrite");
  overwriteMenuItem.setSelected(true);
  overwriteMenuItem.setActionCommand("overwrite");
  overwriteMenuItem.addActionListener(actionCallBack);
  optionsMenu.add(overwriteMenuItem);

  mBar.add(fileMenu);
  mBar.add(toolsMenu);
  mBar.add(operationsMenu);
  mBar.add(optionsMenu);
  return mBar;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createToolBar
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JToolBar createToolBar () {
  JToolBar toolbar = new JToolBar ();
  toolbar.setFloatable(false);
  toolbar.setBackground(bdColor);

  JPanel p = new JPanel(new SpringLayout());
  p.setBackground(bdColor); 

  transferB.setBackground(bdColor);
  transferB.setEnabled(false);
  transferB.addActionListener(actionCallBack);
  transferB.setActionCommand("transfer");
  transferB.setToolTipText("Transfer files");

  p.add(transferB);

  cancelB.setBackground(bdColor);
  cancelB.setEnabled(false);
  cancelB.addActionListener(actionCallBack);
  cancelB.setActionCommand("cancel");
  cancelB.setToolTipText("Cancel Request");

  p.add(cancelB);

  closeB.setBackground(bdColor);
  closeB.setEnabled(false);
  closeB.addActionListener(actionCallBack);
  closeB.setActionCommand("close");
  closeB.setToolTipText("Close request tab");

  p.add(closeB);

  //saveReport.setBackground(bdColor);
  //saveReport.setEnabled(false);
  //saveReport.addActionListener(actionCallBack);
  //saveReport.setActionCommand("save");
  //saveReport.setToolTipText("Save Report");

  //p.add(saveReport);

  p.add(new JLabel("Target Dir :"));

  targetDirTF.setEnabled(false);
  p.add(targetDirTF);

  
  browseButton.setBackground(bdColor);
  browseButton.addActionListener(actionCallBack);
  browseButton.setActionCommand("browse");
  browseButton.setToolTipText("Browse Target Directory");
  p.add(browseButton);

  SpringUtilities.makeCompactGrid(p,1,6,1,1,1,1);

  toolbar.add(p);

  return toolbar;
}



//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processListEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processListEvent (ListSelectionEvent e) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processActionEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processActionEvent(ActionEvent evt) {
  String command = evt.getActionCommand();
  if(command.equals("exit")) {
    try { 
      if(isConfigExists()) {
        pConfig = new XMLParseConfig(configFileLocation);
        properties.put("user-cert", pConfig.getUserCert());
        properties.put("user-key", pConfig.getUserKey());
        properties.put("proxy-file", pConfig.getProxyFile());
      }
      String targetDir = targetDirTF.getText().trim();
      properties.put("last-target-location",targetDir);
      gov.lbl.srm.client.util.Util util = new gov.lbl.srm.client.util.Util();
      util.setThreadCallBack(configFileLocation,properties);
    }catch(Exception e) {
      ShowException.logDebugMessage(logger,e);
      ShowException.showMessageDialog(SRMClient.this, 
	 		"Exception : " + e.getMessage());
    }
      //System.exit(1);
  }
  else if(command.equals("init")) {
   try {
     pConfig = new XMLParseConfig(configFileLocation);
     SwingUtilities.invokeLater(new Runnable() {
       public void run() {
         PassPhraseWindow pw = 
		PassPhraseWindow.getPassPhraseWindow
		   (SRMClient.this,pConfig,0);
         pw.pack();
         Point p = operationTabbedPane.getLocationOnScreen();
         pw.setLocation((int)p.getX(), (int)p.getY());
         pw.setSize(500,85);
         pw.show();
       } 	 
     });
     }
     catch(Exception e) {
        ShowException.logDebugMessage(logger,e);
        ShowException.showMessageDialog(SRMClient.this, 
		 		"Exception : " + e.getMessage());
     }
  }
  else if(command.equals("config")) {
      try {
        if(isConfigExists()) {
          pConfig = new XMLParseConfig(configFileLocation);
          properties.put("user-cert", pConfig.getUserCert());
          properties.put("user-key", pConfig.getUserKey());
          properties.put("proxy-file", pConfig.getProxyFile());
          properties.put("last-target-location", pConfig.getLastTargetLocation());
        }
      }catch(Exception e) {
        ShowException.logDebugMessage(logger,e);
        ShowException.showMessageDialog(SRMClient.this,
			"Exception : " + e.getMessage());
      }
      SwingUtilities.invokeLater (new Runnable () {
         public void run() {
           ConfigTable ctable = 
		new ConfigTable (SRMClient.this, columnNames,
			properties, isRenewProxy());
           ctable.pack();
           Point p = operationTabbedPane.getLocationOnScreen();
           ctable.setLocation((int)p.getX(), (int)p.getY());
           ctable.setSize(500,300);
           ctable.show();
         }
      });
  }
  else if(command.equals("import")) {
      SwingUtilities.invokeLater (new Runnable () {
          public void run() {  
	    JFileChooser fchooser = 
	    	new JFileChooser(System.getProperty("user.dir"));    
            fchooser.setFileFilter 
	    	(new SimpleFilterSwing("xml", "List of request files"));
            fchooser.setDialogTitle("Open the request xml file");
	    int returnValue = fchooser.showOpenDialog(SRMClient.this);
	    if(returnValue == JFileChooser.APPROVE_OPTION) {
	       File f = fchooser.getSelectedFile();
	       if(f.getName().trim().equals("")) return;
               try { 
	       ThreadCallBack tCallBack = 
	       		new ThreadCallBack(SRMClient.this);
           String temp = f.getCanonicalPath();
	       tCallBack.setFileName(temp);
	       tCallBack.start();
               }catch(Exception e) {
                  System.out.println("Exception " + e.getMessage()); 
               }
	    }
	  }
      });
  }
  else if(command.equals("browse")) {
     SwingUtilities.invokeLater (new Runnable () {
          public void run() {  
	    JFileChooser fchooser = 
	    	new JFileChooser(System.getProperty("user.dir"));    
            fchooser.setFileFilter 
	    	(new SimpleFilterSwing("", "List of directories"));
            fchooser.setDialogTitle("Browse the destination directory");
            fchooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
	    int returnValue = fchooser.showOpenDialog(SRMClient.this);
	    if(returnValue == JFileChooser.APPROVE_OPTION) {
	       File f = fchooser.getSelectedFile();
	       if(f.getName().trim().equals("")) return;
               try {
                 String temp = f.getCanonicalPath();
                 targetDirTF.setText(temp);
               }catch(Exception e) { 
                  System.out.println("Exception " + e.getMessage());
               }
	    }
	  }
      });
  }
  else if ((command.equals("transfer")) || (command.equals("cancel")) ||
	   (command.equals("close")) || (command.equals("save")) ||
	   (command.equals("saveAs"))) {
      int idx = operationTabbedPane.getSelectedIndex();
      String title = operationTabbedPane.getTitleAt(idx);
      if(title.equals("SRMFileTransfer")) {
         int rIdx = reqTabbedPane.getSelectedIndex();
         if(rIdx == -1) return;
	 String rTitle = reqTabbedPane.getTitleAt(rIdx);
	 Object obj = ht.get(rTitle);
	 if(obj != null) {
	   final SRMClientFileTransfer  gui = (SRMClientFileTransfer)obj;
	   if(command.equals("transfer")) {
            try {
               int conc = 
		((Integer)concurrency.getModel().getValue()).intValue();
               int par = 
		((Integer)parallelism.getModel().getValue()).intValue();
               int bufS = 
		((Integer)bufferSize.getModel().getValue()).intValue();
               
               String targetDir = targetDirTF.getText().trim();
               properties.put("last-target-location",targetDir);
               String requestModeType = gui.getModeType();
               Request request = gui.getRequestMode(); 
               if(requestModeType.equalsIgnoreCase("Get")) {
                 if(targetDir == null || targetDir.trim().length() == 0) {
                   logger.debug("Target Dir is not given.");
                   ShowException.showMessageDialog (this,
		   	   "Target Dir is not given.");
                   return; 
                 }
                 int tIdx = targetDir.indexOf("file:///");
                 File f = null;
                 if(tIdx != -1) {
                   String temp = targetDir.substring(8);
                   f = new File(temp);
                 }
                 else {
                    f = new File(targetDir);
                    if(targetDir.startsWith("/")) {
                      targetDir = "file:///"+targetDir;
                    }
                    else {
                      targetDir = "file:////"+targetDir;
                    }
                 }
                 if(!f.exists()) {
                   ShowException.showMessageDialog (this,
		   	   "Target Dir " + targetDir + " does not exist.");
                   return; 
                 }
               }
               boolean dcau = false; //need to add that later in the gui
               tThread = new TransferThread( this, conc, par, bufS, blockSize,
					dcau, overwriteMenuItem.isSelected(), 10,request,
				     _retryAllowed, _retryTimeOut,1800,
				     _debug,silent,true,false,"","",null);
	       gui.processTransferAction(tThread,targetDir,conc);
            } catch(Exception e) {
                ShowException.logDebugMessage(logger,e);
                ShowException.showMessageDialog (this,
				"Exception : " + e.getMessage());
            }
           }
	   else if(command.equals("cancel")) {
	     gui.processCancelAction();
           }
	   else if(command.equals("save")) {
           String str = rTitle; 
           System.out.println("String " + str);
           int index = rTitle.lastIndexOf(".");
           if(index != -1) {
             str = rTitle.substring(0,index);
           }
           System.out.println("String (2)" + str+"-report");
	       gui.processSaveAction(str+"-report");
       } 
       else if (command.equals("saveAs")){
           SwingUtilities.invokeLater(new Runnable() {
              public void run() {  
                JFileChooser fchooser = 
				  new JFileChooser (System.getProperty("user.dir"));
                fchooser.setFileFilter
				  (new SimpleFilterSwing("xml", "List of requested files"));
                int returnValue = fchooser.showSaveDialog(SRMClient.this);
                if(returnValue == JFileChooser.APPROVE_OPTION) {
                  try {
                    File f = fchooser.getSelectedFile();
                    String str = f.getCanonicalPath();
                    int index = str.lastIndexOf("/");
                    if(index != -1) {
                      str = str.substring(index+1);
                    }
	                gui.processSaveAction(str);
                  }catch(Exception e) {
                    System.out.println("Exception : " + e.getMessage());
                    //e.printStackTrace();
                  }
                }
              }
           });
       }
	   else if(command.equals("close")) {
	     SwingUtilities.invokeLater(new Runnable() {
	       public void run() {
                 int rIdx = reqTabbedPane.getSelectedIndex();
	         String rTitle = reqTabbedPane.getTitleAt(rIdx);
	         Object obj = ht.get(rTitle);
	         if(obj != null) {
	           SRMClientFileTransfer  gui = 
			(SRMClientFileTransfer)obj;
                   if(gui.isTransferInProcess()) {  
                     ShowException.showMessageDialog(SRMClient.this,
                          "Cannot close this request, active transfer " +
                          "is going on now."); 
                   }
                   else {
	              int value = 
			JOptionPane.showConfirmDialog(SRMClient.this,
		         "Do you want to close this transfer "+rTitle,
		 	 "Do you want to close this transfer "+rTitle,
			  JOptionPane.YES_NO_CANCEL_OPTION, 
			  JOptionPane.QUESTION_MESSAGE);
                       if(value == 0) {
	                 ht.remove(rTitle);
	                 reqTabbedPane.remove(rIdx);
	                 transferTabbedPane.remove(rIdx);
		       }
                   }
                 }
	       }
	     });
	     this.validate();
           }
	 }
      }
  }
  else if(command.equals("srmfiletransfer")) {
     if(!srmFileTransferWindow) {
        importItem.setEnabled(true);
        saveAsReportItem.setEnabled(true);
        saveReportItem.setEnabled(true);
	configItem.setEnabled(true);
	createItem.setEnabled(true);
        transferB.setEnabled(true);
        cancelB.setEnabled(true);
	closeB.setEnabled(true);
	//saveReport.setEnabled(true);
        targetDirTF.setEnabled(true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bgColor);
        operationTabbedPane.add("SRMFileTransfer",p);
        srmFileTransferWindow = true;
     }
   }
   else if(command.equals("srmls")) {
       logger.debug("Sorry, SRMLs" + " is not implemented yet."); 
       ShowException.showMessageDialog(this, "Sorry, SRMLs" + 
 	 " is not implemented yet."); 
   }
   else if(command.equals("srmprepareget")) {
       logger.debug("Sorry, SRMPrepareToGet" + " is not implemented yet."); 
       ShowException.showMessageDialog(this, "Sorry, SRMPrepareToGet" + 
 	 " is not implemented yet."); 
   }
   else if(command.equals("srmprepareput")) {
       logger.debug("Sorry, SRMPrepareToPut" + " is not implemented yet."); 
       ShowException.showMessageDialog(this, "Sorry, SRMPrepareToPut" + 
 	 " is not implemented yet."); 
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processThreadRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processThreadRequest (String fileName) {
  try {
    String temp = fileName;
    int ii = temp.lastIndexOf("/");
    if(ii != -1) {
       temp = temp.substring(ii+1);
    }
    SRMClientFileTransfer fileTransferGUI = 
   		new SRMClientFileTransfer(this,temp,logFileLocation,_debug); 
    JPanel fPanel = fileTransferGUI.createFileTransferPanel(); 
    JPanel tpcPanel = fileTransferGUI.createTransferPanelContents(); 
    Request request = fileTransferGUI.parseXML(fileName);
    Object obj = ht.get(temp);
    if(obj != null) {
      logger.debug("Request for file already exists " + temp);
      ShowException.showMessageDialog
       (this,"Request for file already exists " + temp);
    }
    else {
      int rCount = reqTabbedPane.getTabCount();
      reqTabbedPane.add(temp, fPanel); 
      transferTabbedPane.add(temp, tpcPanel);
      ht.put(temp, fileTransferGUI);
      reqTabbedPane.setSelectedIndex(rCount);
    }
    int idx = operationTabbedPane.getSelectedIndex();
    Component comp = operationTabbedPane.getComponentAt(idx);
    if(comp instanceof JPanel) {
       JPanel leftPanel = (JPanel) comp;
       operationTabbedPane.remove(idx);
       leftPanel.add(reqTabbedPane, BorderLayout.CENTER);
       JPanel rightPanel = new JPanel(new BorderLayout());
       rightPanel.setBackground(bgColor);
       rightPanel.setBorder(new EtchedBorder(30, Color.white, bdColor));
       rightPanel.add(transferTabbedPane, BorderLayout.CENTER);
       splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
	   		leftPanel, rightPanel);
       operationTabbedPane.add("SRMFileTransfer", splitPane);
       addComponentListener(new ComponentAdapter() {
          public void componentShown(ComponentEvent event) {
                splitPane.setDividerLocation(0.9); 
                removeComponentListener(this);
          }
       });
    }
    else if(comp instanceof JSplitPane) {
      ;
    }
    this.validate();
  }catch(Exception e) {
     ShowException.logDebugMessage(logger,e);
     ShowException.showMessageDialog(this, "Exception : " + e.getMessage());
  } 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//isUrlExists
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isUrlExists() {
  return false;
}

public synchronized void numberOfSpaceAvailableFiles(int num) {
}

public synchronized Vector  getSrmFirstUrl(boolean b) {
  Vector vec = new Vector ();
  vec.addElement(new FileInfo());
  return vec;
}

public void incrementCompletedProcess() {
}

public void putDone(String surl, String rid, int label) {
}

public void abortFiles(String surl, String rid, int label) {
}

public void releaseFile(String surl, String rid, int label) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processMouseEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processMouseEvent (MouseEvent evt) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processMouseEnteredEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processMouseEnteredEvent (MouseEvent evt) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processMouseExitedEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processMouseExitedEvent (MouseEvent evt) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processChangeRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processChangeRequest (ChangeEvent evt) {
  MyJTabbedPane pane = (MyJTabbedPane)evt.getSource();
  if(pane.getTabCount() == 0) return;
  String label = pane.getLabel();
  if(label.equals("operation")) {
   ;
  }
  else if(label.equals("request")) {
    int idx = pane.getSelectedIndex();
    if(transferTabbedPane.getTabCount() > 0) {
      transferTabbedPane.setSelectedIndex(idx);
    }
  }
  else if(label.equals("transfer")) {
    int idx = pane.getSelectedIndex();
    if(reqTabbedPane.getTabCount() > 0) {
      reqTabbedPane.setSelectedIndex(idx);
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// inner class MyJTabbedPane
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class MyJTabbedPane extends JTabbedPane {
  private String _label="";
  public MyJTabbedPane (String label) {
    _label = label;
  }
  public String getLabel () {
    return _label;
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// main
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void main(String[] args) {
  boolean gui = true;
  boolean old = false;

  for(int i = 0; i < args.length; i++) {
    if(args[i].equals("-nogui")) {
      gui = false;
    }
    if(args[i].equals("-v1")) {
      old = true;
    }
    if(args[i].equals("-version")) {
      SRMClientN.printVersion();
    }
  }

  try {
    //System.out.println("\n+++++++ Running V2 client +++++ \n");
    if(gui) {
      try {
        JFrame frame = new SRMClient (args);
        frame.pack();  
        frame.setSize(900,600);
        frame.setLocation(100,100);
        frame.show();
      }catch(Exception e) {
        System.out.println("Exception " + e.getMessage());
        //e.printStackTrace(); 
      }
    }
    else {
        new SRMClientN(args,null);
    }
  }
  catch(Exception e) {
     System.out.println("Exception="+e.getMessage());
     System.exit(92);
  }
}

}
