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
import java.io.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;

import javax.swing.SpringLayout;
import javax.swing.SpinnerNumberModel;
import javax.swing.AbstractAction;
import javax.swing.text.*;
import javax.swing.event.*;
import java.awt.print.PrinterJob;
import java.awt.print.PageFormat;
import javax.swing.Action;
import javax.swing.tree.*;
import javax.swing.table.*;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.*;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.JTextPane;
import javax.swing.JSpinner;
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

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xml.serialize.*;


import java.net.MalformedURLException;

import gov.lbl.srm.client.transfer.*;
import gov.lbl.srm.client.transfer.globus.*;
import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.client.exception.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMClientFileTransfer
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientFileTransfer 
	implements colorIntf, mouseIntf, actionIntf, threadIntf,
	FileEventListener, FTPIntf
{

private JPanel bottomPanel = new JPanel (new SpringLayout());
private JPanel sidePanel = new JPanel (new SpringLayout());
private JTextPane statusArea = new JTextPane();
private JMenuItem editTargetURL = new JMenuItem("Edit targetURL");
private JPopupMenu popupMenu = new JPopupMenu();

private static SimpleAttributeSet styleH = new SimpleAttributeSet();
private static SimpleAttributeSet style  = new SimpleAttributeSet();

private ActionCallBack actionCallBack;
private MouseCallBack mouseCallBack;
private JSortTable transferTable;
private SRMClientIntf _parent;
private Vector pList = new Vector ();
private boolean isCancel;

private String requestMode="";
private Request request;
private SharedObjectLock sLock;
private JTextField requestedField = new JTextField();
private JTextField completedField = new JTextField();
private JTextField errorField = new JTextField();
private JTextField pendingField = new JTextField();
private JTextField existsField = new JTextField();

private ProgressBarTableModel pmodel;
private JTable progressMonitorTable;


private int requestedFiles;
private int completedFiles;
private int errorFiles;
private int existsFiles;
private int conc;
private int currentTargetIndex;

private boolean transferInProcess = false;
private FileOutputStream fos; 
private BufferedWriter out; 

private String requestFileName="";
private String logFileLocation="";
private boolean _debug;

private static Log logger;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientFileTransfer
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientFileTransfer(SRMClientIntf parent, 
	String fileName, String logFileLocation,boolean debug) {

  actionCallBack = new ActionCallBack(this);
  mouseCallBack = new MouseCallBack(this);
  _parent = parent;
  requestFileName = fileName;
  pList.add("gsiftp://");
  pList.add("ftp://");
  pList.add("srm://");
  pList.add("http://");
  pList.add("https://");
  pList.add("file:////");
  StyleConstants.setForeground(styleH, Color.blue);
  StyleConstants.setBackground(styleH, bdColor);
  StyleConstants.setFontFamily(styleH, "Default");
  StyleConstants.setItalic(styleH, true);
  StyleConstants.setFontSize(styleH, 12);

  StyleConstants.setForeground(style, bgColor);
  StyleConstants.setBackground(style, Color.blue);
  StyleConstants.setFontFamily(style, "Default");
  StyleConstants.setItalic(style, true);
  StyleConstants.setFontSize(style, 12);
  
  try {
    ClassLoader cl = this.getClass().getClassLoader();
    Class c = cl.loadClass("gov.lbl.srm.client.main.SRMClientFileTransfer");
    logger = LogFactory.getLog(c.getName());
  }catch(ClassNotFoundException cnfe) {
    System.out.println("ClassNotFoundException ");
    //throw new SRMClientException(cnfe.getMessage());
  }

  this.logFileLocation = logFileLocation;
  try {
    String OSName = System.getProperty("os.name");
    if (!OSName.startsWith("Windows")) {
      if(logFileLocation.equals("")) {
        this.logFileLocation = ".";
      }
    }
    int idx = fileName.indexOf(".xml");
    requestFileName = fileName;
    if(idx != -1) {
      requestFileName = fileName.substring(0,idx);
    }
   }catch(Exception e) {
       System.out.println("Exception " +  e.getMessage());
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createFileTransferPanel
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JPanel createFileTransferPanel () 
{
  JPanel bottomPanelContainer = new JPanel(new SpringLayout());
  bottomPanelContainer.setBackground(bgColor);
  bottomPanelContainer.setBorder(new EtchedBorder(30, Color.white, bdColor));

  bottomPanel.setBackground(bgColor);
  bottomPanel.setPreferredSize(new Dimension(500, 800));

  bottomPanelContainer.add(bottomPanel);

  SpringUtilities.makeCompactGrid(bottomPanelContainer,1,1,1,1,1,1);
  return bottomPanelContainer;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isTransferInProcess
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isTransferInProcess () {
  return transferInProcess;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  @@@@@   Start methods implemented for FTPIntf (window)
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkDiskSpaceFull
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean checkDiskSpaceFull (SharedObjectLock sLock,
	File targetDir, long sSize)  {

     this.sLock = sLock;
     final File tDir = targetDir;
     final long tsSize = sSize;
  try {
     long ll = TPlatformUtil.getFreeSpace(targetDir);
     logger.debug("Available Disk Size " + ll);
     logger.debug("Current File Size " + sSize);
     if((sSize+100000) >= ll) {
       SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            StringBuffer message = new StringBuffer(); 
	    message.append(
              "Disk space is full now. Please remove some files and "+
              "press Yes to continue further, else the request will " +
	      "be cancelled.");

  	    int value = JOptionPane.showConfirmDialog(_parent.getFrame(),
	    message.toString(), "Disk space full",
	      JOptionPane.YES_NO_CANCEL_OPTION,
	      JOptionPane.QUESTION_MESSAGE);
            if(value == 0) {
              if(checkDiskSpaceFull(SRMClientFileTransfer.this.sLock,
			tDir,tsSize)) {
                SRMClientFileTransfer.this.sLock.setIncrementCount(false);
              }
            }
            else {
              isCancel = true;
              SRMClientFileTransfer.this.sLock.setIncrementCount(true);
            }
         }
      });
      return false;  
    }
    else {
       SRMClientFileTransfer.this.sLock.setIncrementCount(false);
       return true;
    }
  }catch(IOException ioe) {
    ShowException.showMessageDialog(_parent.getFrame(),
		"Exception : " + ioe.getMessage());
  }
  return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//incrementCompletedProcess
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void incrementCompletedProcess() {  
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void enableTransferButton (boolean b, boolean ok) {
 if(!b) {
  transferInProcess = true;
 }
 else {
  transferInProcess = false;
  try {
    out.write("Total Active=0\n");
    out.write("Request Done\n");
    out.flush();
  }catch(IOException ioe) {
    //System.out.println("IOException : " + ioe.getMessage());
  }
  int cc =  (requestedFiles - (completedFiles+errorFiles+existsFiles));
  if(cc >= 0) {
     pendingField.setText(""+cc);
  }
 }
 _parent.enableTransferButton(b,ok);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// refreshView
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void refreshView() {
  pmodel.removeRowValues(0);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareView
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public synchronized void prepareView () { 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// updateView
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void updateView (FileStatusGUI fgui) {
  Vector row = new Vector();
  row.add(fgui.getFileProgress());
  row.add(fgui.getFileHostInfo());
  row.add(fgui.getFileExpectedSize());
  row.add(fgui.getFileCurrentSize());
  row.add(fgui.getFileTransferRate());
  row.add(fgui.getFileColorCode());
   
  pmodel.insertRowValues(pmodel.getRowCount(),row, new Integer(0),fgui);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getParentWindow
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isGui() {
  return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getParentWindow
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JFrame getParentWindow () {
  return (JFrame) _parent.getFrame();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential initProxy() throws Exception {
  return _parent.createProxy(_parent.getPassword());
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCurrentTargetIndex
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCurrentTargetIndex(int index) {
  currentTargetIndex = index;
  scrollView();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// scrollView
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void scrollView () {
  int selectedRow = currentTargetIndex;
  int selectedColumn = 0;
  int columnWidth = 25;
  progressMonitorTable.scrollRectToVisible(
       new Rectangle(columnWidth*selectedColumn,
                     transferTable.getRowHeight()*(selectedRow),
                     columnWidth,
                     transferTable.getRowHeight()));
  _parent.validateFrame();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isRequestCancel
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isRequestCancel() {
  return isCancel;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkProxy 
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized GSSCredential checkProxy() throws ProxyNotFoundException
{
  GSSCredential mycred = null;
  while(_parent.getLock()) {
   try {
     logger.debug("Waiting to get proxy.");
     System.out.println("Waiting to get proxy.");
     wait(); 
   }catch(InterruptedException ie) {
        System.out.println("InterruptedException " + ie.getMessage());
   }
  }
  try {
    mycred = _parent.getCredential();
    mycred = _parent.checkTimeLeft();
     
  }catch(Exception e) {
     ShowException.logDebugMessage(logger,e);
     throw new ProxyNotFoundException (e.getMessage()+"\n"+
	"Please Resume transfer after your renew your credentials.");
  }
  return mycred;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  @@@@@   end methods implemented for FTPIntf (window)
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLogger
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Log getLogger() {
  return logger;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createTransferPanelContents
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JPanel createTransferPanelContents() 
{
  sidePanel.setBackground(bgColor);
  sidePanel.setBorder(new EtchedBorder(30, Color.white, bdColor));

  JPanel statusPanel = new JPanel(new SpringLayout());
  statusPanel.setBackground(bgColor);
  statusPanel.setBorder(new EtchedBorder(30, Color.white, bdColor));

  JPanel legendPanel = new JPanel(new SpringLayout());
  legendPanel.setBackground(bgColor);
  legendPanel.setBorder(new EtchedBorder(30, Color.white, bdColor));
  JPanel p1 = new JPanel(new SpringLayout());
  p1.setBackground(bgColor);
  JPanel p2 = new JPanel(new SpringLayout());
  p2.setBackground(bgColor);
  p1.add(new JLabel("Network speed / sec. :  "));
  gov.lbl.srm.client.util.Shape f1 = 
	new gov.lbl.srm.client.util.Shape(0, 0, Color.red,"< 0 MB.");
  gov.lbl.srm.client.util.Shape f2 = 
	new gov.lbl.srm.client.util.Shape(0, 0, Color.orange,"< 1 MB.");
  gov.lbl.srm.client.util.Shape f3 = 
	new gov.lbl.srm.client.util.Shape(0,0, Color.yellow,"< 5 MB.");
  gov.lbl.srm.client.util.Shape f4 = 
	new gov.lbl.srm.client.util.Shape(0, 0, Color.green,"< 10 MB.");
  gov.lbl.srm.client.util.Shape f5 = 
	new gov.lbl.srm.client.util.Shape(0, 0, dGreenColor, "> 10 MB.");

  p2.add(f1);
  p2.add(f2);
  p2.add(f3);
  p2.add(f4);
  p2.add(f5);

  legendPanel.add(p1);
  legendPanel.add(p2);
  legendPanel.setPreferredSize(new Dimension(250,45));
  SpringUtilities.makeCompactGrid(p1,1,1,1,1,1,1);
  SpringUtilities.makeCompactGrid(p2,1,5,1,1,1,1);
  SpringUtilities.makeCompactGrid(legendPanel,2,1,1,1,1,1);

  JPanel pp = new JPanel (new BorderLayout());
  pp.setBackground(bgColor);
  pp.setBorder(new EtchedBorder(30, Color.white, bdColor));

  JPanel sidePanelContainer = new JPanel(new BorderLayout());
  sidePanelContainer.setBackground(bgColor);
  sidePanelContainer.setBorder(new EtchedBorder(30, Color.white, bdColor));
  sidePanelContainer.add(sidePanel, BorderLayout.CENTER);
  statusArea.setEditable(false);
  statusPanel.add(new JScrollPane(statusArea));
  JLabel infoLabel = new JLabel
	("Click on desired row to see detailed information");
  statusPanel.add(infoLabel);
  statusPanel.setPreferredSize(new Dimension(200,150));
  SpringUtilities.makeCompactGrid(statusPanel,2,1,1,1,1,1);
  sidePanelContainer.add(statusPanel,BorderLayout.SOUTH);
  pp.add(sidePanelContainer, BorderLayout.CENTER);
  pp.add(legendPanel, BorderLayout.SOUTH);

  return pp;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// populateFileTransferTable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void populateFileTransferTable (Vector fileInfo) {
  Vector columnNames = new Vector();
  columnNames.add("Source Url");
  columnNames.add("Target Url");
  columnNames.add("Expected Size");
  columnNames.add("Status");

  Vector data = new Vector ();
  Vector sizeVec = new Vector();
  int size = fileInfo.size();
  if(requestMode.equalsIgnoreCase("Get")) {
    for(int i = 0; i < size; i++) { 
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
      fIntf.setLabel(i);
      fIntf.setStatusLabel("");
      fIntf.addListeners(this);
      Vector row = new Vector();
      row.add(fIntf.getSURL());
      row.add(fIntf.getTURL());
      FSize fSize =  null;
      if(fIntf.getShowSize()) {
        fSize = new FSize(fIntf.getExpectedSize());
      } 
      else {
        fSize = new FSize("?");
      }
      row.add(fSize);
      row.add(fIntf);
      data.add(row);
      sizeVec.add(fSize);
    }
  }
  else if(requestMode.equalsIgnoreCase("Put")) {
    for(int i = 0; i < size; i++) { 
      FileIntf fIntf = (FileIntf) fileInfo.elementAt(i);
      fIntf.setLabel(i);
      fIntf.setStatusLabel("");
      fIntf.addListeners(this);
      String turl = fIntf.getTURL();
      Vector row = new Vector();
      row.add(fIntf.getSURL());
      row.add(turl);
      FSize fSize =  null;
      if(fIntf.getShowSize()) {
        fSize = new FSize(fIntf.getExpectedSize());
      } 
      else {
        fSize = new FSize("?");
      }
      row.add(fSize);
      row.add(fIntf);
      data.add(row);
      sizeVec.add(fSize);
    }
  }

  MyTableModel model = new MyTableModel(data,columnNames,fileInfo,sizeVec);
  transferTable = new JSortTable(model) {
    public JToolTip createToolTip() {
      return new JMultiLineToolTip(25,0);
    }
  };
  transferTable.setSpecialIndex(3);
  transferTable.setToolTipText("Click on the row, \n " +
  			   "to see information on the \n " +
			   "right side status area.");
  transferTable.setBackground(bgColor);

  editTargetURL.setActionCommand("edit");
  editTargetURL.addActionListener(actionCallBack);
  
  popupMenu.add(editTargetURL);

  TableColumn tm = transferTable.getColumnModel().getColumn(3);
  tm.setCellRenderer(new ColorColumnRenderer());

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//showDetailedStatusInformation
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void showDetailedStatusInformation (int rr) {
  MyTableModel model = (MyTableModel) transferTable.getModel();
  FileIntf fIntf =  (FileIntf) model.getValueAt(rr,3);
  int row = fIntf.getLabel();
  FileInfo fInfo = model.getValueAt(row);
  String sourceUrl = fInfo.getSURL();
  String targetUrl = fInfo.getTURL();
  String targetDir = fInfo.getTargetDir();
  String status = fInfo.getStatusLabel();
  insertText("SourceUrl       : ", styleH);
  insertText(sourceUrl+"\n", style);
  insertText("TargetUrl       : ", styleH);
  insertText(targetDir+targetUrl+"\n", style);
  insertText("Expected Size (in bytes) : ", styleH);
  if(fInfo.getShowSize())
    insertText(fInfo.getExpectedSize()+"\n", style);
  else 
    insertText(fInfo.getExpectedSize()+" (default size)\n", style);
  if(fInfo.getCompleted()) {
     insertText("ActualSize (in bytes)  : ", styleH);
     insertText(fInfo.getActualSize()+"\n", style);
  }
  insertText("TimeTaken (in milliseconds) : ", styleH);
  insertText(fInfo.getTimeTaken()+"\n", style);
  insertText("Status          : ", styleH);
  insertText(status+"\n", style);
  String message = fInfo.getErrorMessage();
  if(!message.equals("")) {
     insertText("Error Message          : ", styleH);
     insertText(message+"\n", style);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseXML
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Request parseXML (String fileName) 
	throws NumberFormatException, SRMClientException, Exception {

   logger.debug("Parsing request file " + fileName);
   XMLParseRequest xmlParse = new XMLParseRequest(fileName,null,false,true);
   request = xmlParse.getRequest(); 

   requestedFiles = request.getTotalFiles ();
   Vector fileInfo = request.getFileInfo();
   requestMode = request.getModeType ();
   populateFileTransferTable (fileInfo);
   transferTable.setPreferredScrollableViewportSize(new Dimension(500, 500));
   showDetailedStatusInformation(0);
   
   transferTable.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent me) {
           removeText(0);
           int rr = transferTable.rowAtPoint(me.getPoint());
           showDetailedStatusInformation(rr);
      }

      public void mouseClicked(MouseEvent me) {
           if (SwingUtilities.isRightMouseButton(me)) {
               popupMenu.show(transferTable, me.getX(), me.getY());
           }
      } 
   });
                  
   JScrollPane sPane = new JScrollPane(transferTable);
   sPane.getViewport().setBackground(bgColor);
   bottomPanel.add(sPane);
   JPanel p = new JPanel(new SpringLayout());
   p.setBackground(bgColor);
   requestedField.setEditable(false);
   completedField.setEditable(false);
   errorField.setEditable(false);
   pendingField.setEditable(false);
   existsField.setEditable(false);
   requestedField.setBackground(bgColor);
   completedField.setBackground(bgColor);
   errorField.setBackground(bgColor);
   pendingField.setBackground(bgColor);
   existsField.setBackground(bgColor);
   requestedField.setText(""+requestedFiles);
   JLabel l1 = new JLabel("Total Requested");
   p.add(l1);
   p.add(requestedField);
   p.add(new JLabel("Total Transfer"));
   p.add(completedField);
   p.add(new JLabel("Total Failed"));
   p.add(errorField);
   p.add(new JLabel("Total Pending"));
   p.add(pendingField);
   p.add(new JLabel("Total Already Exists"));
   p.add(existsField);
   SpringUtilities.makeCompactGrid(p,5,2,1,1,1,1);
   bottomPanel.add(p);
   SpringUtilities.makeCompactGrid(bottomPanel,2,1,1,1,1,1);
   _parent.validateFrame();
   logger.debug("Parsing done");
   return request;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getModeType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getModeType () {
  return requestMode;
}

public Request getRequestMode() {
  return request;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processSaveAction
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processSaveAction (String fileName) {
   ThreadCallBack tb = new ThreadCallBack (this);
   tb.setFileName(fileName);
   tb.start();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processThreadRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processThreadRequest(String fileName) {
 try {
    int idx = fileName.lastIndexOf(".");
    if(idx != -1) {
      fileName = fileName.substring(0,idx);
    }

    String ss = logFileLocation+"/"+fileName;

    ss = ss+".xml";
    System.out.println("Saving report " + ss);
    logger.debug("Saving report " + ss);

    MyTableModel model = (MyTableModel) transferTable.getModel();
    Vector fileInfo = model.getFileInfo();
    Object[] objArray = fileInfo.toArray();

    DocumentBuilderFactory factory =  
	DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = factory.newDocumentBuilder ();
    org.w3c.dom.Document doc = parser.newDocument();
    org.w3c.dom.Element root = doc.createElement("report");
    Attr reqName = doc.createAttribute("filename");
    reqName.setValue(fileName);
    root.setAttributeNode(reqName);
    Attr type = doc.createAttribute("requesttype");
    type.setValue(requestMode);
    root.setAttributeNode(type);
    Attr totalFiles = doc.createAttribute("total");
    totalFiles.setValue(""+objArray.length);
    root.setAttributeNode(totalFiles);
    Attr transferredFiles = doc.createAttribute("transferred");
    transferredFiles.setValue(""+completedFiles);
    root.setAttributeNode(transferredFiles);
    Attr pendingFiles = doc.createAttribute("pending");
    int cc =  (requestedFiles - (completedFiles+errorFiles+existsFiles));
    pendingFiles.setValue(""+cc);
    root.setAttributeNode(pendingFiles);
    Attr failedFiles = doc.createAttribute("failed");
    failedFiles.setValue(""+errorFiles);
    root.setAttributeNode(failedFiles);
    Attr alreadyExistsFiles = doc.createAttribute("exists");
    alreadyExistsFiles.setValue(""+existsFiles);
    root.setAttributeNode(alreadyExistsFiles);
    for(int i = 0; i < objArray.length; i++) {
       FileIntf fIntf = (FileIntf) objArray[i];
       org.w3c.dom.Element file = doc.createElement("file");
       org.w3c.dom.Element surl = doc.createElement("sourceurl");
       surl.appendChild(doc.createTextNode(fIntf.getSURL()));
       file.appendChild(surl);
       org.w3c.dom.Element turl = doc.createElement("targeturl");
       turl.appendChild(doc.createTextNode (fIntf.getTURL()));
       file.appendChild(turl);
       org.w3c.dom.Element esize = doc.createElement("expectedsize");
       esize.appendChild(doc.createTextNode (fIntf.getExpectedSize()));
       file.appendChild(esize);
       org.w3c.dom.Element asize = doc.createElement("actualsize");
       asize.appendChild(doc.createTextNode (fIntf.getActualSize()));
       file.appendChild(asize);
       org.w3c.dom.Element status = doc.createElement("status");
       status.appendChild(doc.createTextNode (fIntf.getStatusLabel()));
       file.appendChild(status);
       org.w3c.dom.Element timetaken = doc.createElement("timetaken");
       timetaken.appendChild(doc.createTextNode (fIntf.getTimeTaken()));
       file.appendChild(timetaken);
       org.w3c.dom.Element errormessage = doc.createElement("message");
       errormessage.appendChild(doc.createTextNode (fIntf.getErrorMessage()));
       file.appendChild(errormessage);
       root.appendChild(file);
    }
    doc.appendChild(root);
    //default encoding is UTF-8
    OutputFormat format = new OutputFormat(doc);
    format.setIndenting(true);
    XMLSerializer serializer = new XMLSerializer(format);
    PrintWriter pw = new PrintWriter(new FileOutputStream(ss));
    serializer.setOutputCharStream(pw);
    serializer.setOutputFormat(format);
    serializer.serialize(doc);
    ShowException.showMessageDialog(_parent.getFrame(),
		"Report saved in file " + ss);
    /*
    if(!transferInProcess) {
      out.close();
      fos.close(); 
    }
    */
 }catch(Exception e) {
    ShowException.logDebugMessage(logger,e);
    ShowException.showMessageDialog(_parent.getFrame(),
		"Exception : " + e.getMessage());
 }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// resetValues
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void resetValues (String targetDir, boolean isOverwrite) {
  //reset values here at the begining of transfer process
  completedFiles = 0;
  errorFiles = 0;
  completedField.setText(""+completedFiles);
  errorField.setText(""+errorFiles);
  pendingField.setText(""+requestedFiles);
  existsField.setText(""+existsFiles);

  MyTableModel model = (MyTableModel) transferTable.getModel();
  Vector fileInfo = model.getFileInfo();
  Object[] objArray = fileInfo.toArray();

  if(requestMode.equalsIgnoreCase("Get")) {
	for(int i = 0; i < objArray.length; i++) {
		model.setFileInfo(targetDir,i,9);
		filePending(i,isOverwrite);
	}
  }
  else {
	for(int i = 0; i < objArray.length; i++) {
		filePending(i,isOverwrite);
	}
  }
  _parent.validateFrame();

}

private void resetProgressTable () {
  sidePanel.removeAll();
  Vector colNames = new Vector ();
  colNames.add("%, FileName"); 
  colNames.add("Protocol://Host");
  colNames.add("Expected Size");
  colNames.add("Current Size");
  colNames.add("TransferRate Mbps.");
  colNames.add("speed / sec.");
  Vector data = new Vector ();
  pmodel = new ProgressBarTableModel(colNames,data); 
  progressMonitorTable = new JTable( pmodel);

  progressMonitorTable.addMouseListener(new MouseAdapter() {
      public void mouseReleased(MouseEvent me) {
           removeText(0);
           int rr = progressMonitorTable.rowAtPoint(me.getPoint());
           if(rr < 0) return;
           FileStatusGUI fgui =  (FileStatusGUI) pmodel.getFileStatusGUIVec(rr);
           FileIntf fIntf = fgui.getFileIntf();
           int row = fIntf.getLabel();
           String sourceUrl = fIntf.getSURL();
           String targetUrl = fIntf.getTURL();
           String status = fIntf.getStatusLabel();
	       insertText("SourceUrl       : ", styleH);
	       insertText(sourceUrl+"\n", style);
           insertText("TargetUrl       : ", styleH);
           insertText(targetUrl+"\n", style);
           insertText("Expected Size (in bytes) : ", styleH);
           if(fIntf.getShowSize()) 
             insertText(fIntf.getExpectedSize()+"\n", style);
           else 
             insertText(fIntf.getExpectedSize()+" (default size)\n", style);
           if(fIntf.getCompleted()) {
             insertText("ActualSize (in bytes) : ", styleH);
             insertText(fIntf.getActualSize()+"\n", style);
           }
           insertText("TimeTaken  (in milliseconds) : ", styleH);
           insertText(fIntf.getTimeTaken()+"\n", style);
           insertText("Status          : ", styleH);
           insertText(status+"\n", style);
      }

   });
                  
  progressMonitorTable.setPreferredScrollableViewportSize
	(new Dimension(500, 500));
  TableColumn col0 = progressMonitorTable.getColumnModel().getColumn(0);
  TableColumn col1 = progressMonitorTable.getColumnModel().getColumn(1);
  TableColumn col2 = progressMonitorTable.getColumnModel().getColumn(2);
  TableColumn col3 = progressMonitorTable.getColumnModel().getColumn(3);
  TableColumn col4 = progressMonitorTable.getColumnModel().getColumn(4);
  TableColumn col5 = progressMonitorTable.getColumnModel().getColumn(5);
  col0.setCellRenderer(new ProgressBarTableRenderer());
  col0.setMinWidth(0); 
  col0.setMaxWidth(500); 
  col0.setPreferredWidth(320); 
  //col1.setCellRenderer(new RightAlignRenderer());
  col2.setCellRenderer(new RightAlignRenderer());
  col3.setCellRenderer(new RightAlignRenderer());
  col4.setCellRenderer(new RightAlignRenderer());
  col5.setCellRenderer(new NetworkColumnRenderer());
  col5.setMinWidth(0); 
  col5.setMaxWidth(50); 
  col5.setPreferredWidth(25); 
  JScrollPane pane = new JScrollPane(progressMonitorTable);
  pane.getViewport().setBackground(bgColor);
  sidePanel.add(pane);
  SpringUtilities.makeCompactGrid(sidePanel,1,1,1,1,1,1);
  _parent.validateFrame();
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processTransferAction
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processTransferAction (TransferThread tThread, 
	String targetDir, int conc) { 

  if(isCancel) {
     logger.debug("Cannot perform transfer files, request is cancelled");
     ShowException.showMessageDialog(_parent.getFrame(),
      "Cannot perform transfer files, request is cancelled");
  }
  else {
   //targetDir == null is already checked in SRMClient before
   //calling this method
   logger.debug("Transfering files ... ");

   this.conc = conc;
   resetProgressTable();


   try { 
       fos = new FileOutputStream 
		(logFileLocation+"/"+requestFileName+"-status.txt");
       out = new BufferedWriter (new OutputStreamWriter(fos));

       MyTableModel model = (MyTableModel) transferTable.getModel();
       Vector fileInfo = model.getFileInfo();
       Object[] objArray = fileInfo.toArray();

       if(requestMode.equalsIgnoreCase("Get")) {
	     for(int i = 0; i < objArray.length; i++) {
		   model.setFileInfo(targetDir,i,9);
	     }
       }
  
       out.write("Total Files="+objArray.length+"\n");
       out.write("Total Exists="+existsFiles+"\n");
       out.flush();
       boolean isRenew = _parent.isRenewProxy();   
       tThread.setLogger(logger,null);
       tThread.setPModel(pmodel);
       //tThread.setFileInfo(objArray); 
       tThread.setFTPIntf(this);
       tThread.setFileEventListener(this);
       tThread.setProtocolList(pList);
       //tThread.setProxy(proxy);
       tThread.start();
       _parent.validateFrame();
    }catch(Exception e) {
        ShowException.logDebugMessage(logger,e);
        ShowException.showMessageDialog(_parent.getFrame(), 
		"Exception : " + e.getMessage());
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processCancelAction
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processCancelAction () { 
  if(!isCancel) {
     SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          StringBuffer message = new StringBuffer(); 
	  message.append(
	      "Cancel operation will not cancel the current transferring ");
	  message.append
	     ("file, it will only take effect for the next file in the queue."); 
	  message.append("\nDo you really want to cancel.");

	  int value = JOptionPane.showConfirmDialog(_parent.getFrame(),
	  message.toString(), "Cancel transfer",
	      JOptionPane.YES_NO_CANCEL_OPTION,
	      JOptionPane.QUESTION_MESSAGE);
          if(value == 0) {
	     isCancel = true;
          }
        }
     });
     isCancel = true;
     logger.debug("Process cancel request " + isCancel);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// FileEventListener method
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void fileActive (int idx, String size) {
  logger.debug("Event FileActive " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  model.setFileInfo("Active",idx,2);
  FileInfo fInfo = model.getValueAt(idx);
  FSize fSize = model.getFileSizeAt(idx);
  fInfo.setStartTime(System.currentTimeMillis());
  fSize.setSize(size);
  model.specialSortColumn(3, false);
  model.fireTableDataChanged();
  int cc =  (requestedFiles - (completedFiles+errorFiles+conc+existsFiles));
  if(cc >= 0) {
     pendingField.setText(""+cc);
  }
  try {
     out.write("Total Active="+conc+"\n");
     out.flush();
  }catch(IOException ioe) { 
     System.out.println("IOException " + ioe.getMessage());
  }
}

public void fileCompleted (int idx, String size) {
  logger.debug("Event fileCompleted " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  model.setFileInfo("Done", idx, 2);
  model.fireTableDataChanged();
  FileInfo fInfo = model.getValueAt(idx);
  if(!fInfo.getCompleted()) {
   fInfo.setCompleted(true);
   fInfo.setActualSize(size);
   fInfo.setTimeStamp(new Date());
   fInfo.setEndTime(System.currentTimeMillis());
   completedFiles++;
   completedField.setText(""+completedFiles);
   int cc =  (requestedFiles - (completedFiles+errorFiles+conc+existsFiles));
   try {
     out.write("Total Transfer="+completedFiles+"\n");
     if(cc >= 0) {
       pendingField.setText(""+cc);
       out.write("Total Pending="+cc+"\n");
       out.write("Total Active="+conc+"\n");
     }
     out.flush();
   }catch(IOException ioe) {
     System.out.println("IOException " + ioe.getMessage());
   }
  }
}

public void fileFailed (int idx) {
  logger.debug("Event fileFailed " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  model.setFileInfo("Failed", idx, 2);
  model.specialSortColumn(3, false);
  model.fireTableDataChanged();
  FileInfo fInfo = model.getValueAt(idx);
  if(!fInfo.getFailed()) {
    fInfo.setFailed(true);
    errorFiles++;
    errorField.setText(""+errorFiles);
    try {
     int cc =  (requestedFiles - (completedFiles+errorFiles+existsFiles));
     if(cc >= 0) {
       pendingField.setText(""+cc);
       out.write("Total Pending="+cc+"\n");
     }
     out.write("Total Failed="+errorFiles+"\n");
     out.write("MessageStart*\n");
     out.write(fInfo.writeFileInfo()+"\n");
     out.write("MessageEnd*\n");
     out.flush();
    }catch(IOException ioe) {
       System.out.println("IOException " + ioe.getMessage());
    }
  }
}

public void fileSkipped (int idx) {
  logger.debug("Event fileSkipped" + idx);
}

public void filePending(int idx, boolean isOverwrite) {
  MyTableModel model = (MyTableModel) transferTable.getModel();
  FileInfo fInfo = model.getValueAt(idx);
  if(!fInfo.isUsed())  {
   if(isOverwrite) {
     filePending(idx);
   }
   else {
     if(!fInfo.getStatusLabel().equalsIgnoreCase("exists")) {
       filePending(idx);
     }
   }
  }
}

public void filePending(int idx) {
  logger.debug("Event filePending " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  model.setFileInfo("Pending", idx, 2);
  model.fireTableDataChanged();
}

public void fileExists(int idx) {
  logger.debug("Event fileExists " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  model.setFileInfo("Exists", idx, 2);
  model.fireTableDataChanged();
  existsFiles++;
  existsField.setText(""+existsFiles);
  try {
    out.write("Total Exists="+existsFiles+"\n");
    out.flush();
  }catch(IOException ioe) {
    System.out.println("IOException " + ioe.getMessage());
  }
}

public void requestCancelled(int idx) {
  logger.debug("Event requestCancelled " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  int size = model.getRowCount();
  for(int i = idx; i < size; i++) {
    model.setFileInfo("Cancel", i, 2);
  }
  model.fireTableDataChanged();
}

public void  setTimeTaken(int idx, String value) {
  logger.debug("Event setTimeTaken " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  FileInfo fileInfo = model.getValueAt(idx);
  fileInfo.setTimeTaken(value);
}

public void setErrorMessage(int idx, String value) {
  logger.debug("Event setErrorMessage " + idx);
  MyTableModel model = (MyTableModel) transferTable.getModel();
  FileInfo fileInfo = model.getValueAt(idx);
  fileInfo.setErrorMessage(value);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processActionEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processActionEvent(ActionEvent evt) {
  if(evt.getSource() instanceof JMenuItem) {
      int row = transferTable.getSelectedRow();
      if(row != -1) {
        MyTableModel model = (MyTableModel) transferTable.getModel();
        TextFrame textFrame = new TextFrame (model,row);
        Point p = transferTable.getLocationOnScreen();
        textFrame.setLocation((int)p.getX(), (int)p.getY());
        textFrame.setVisible(true); 
      }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processMouseEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processMouseEvent (MouseEvent evt) {
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processItemEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processItemEvent (ItemEvent evt) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processMouseEnteredEvent
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processMouseEnteredEvent (MouseEvent evt) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// removeText
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void removeText(int idx) {
  try {
    javax.swing.text.Document doc = statusArea.getDocument();
    doc.remove(idx,doc.getLength());
  }catch(BadLocationException e) {
    logger.debug("Exception during remove text");
    ShowException.showMessageDialog(_parent.getFrame(),
		"Exception during remove text");
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// insertText
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void insertText (String text, AttributeSet set)
{
  try {
     javax.swing.text.Document doc = statusArea.getDocument();
     doc.insertString(doc.getLength(), text, set);
  }catch(BadLocationException e) {
    logger.debug("Exception during insert text");
    ShowException.showMessageDialog(_parent.getFrame(),
		"Exception during insert text");
  }
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
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// RightAlignRenderer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class RightAlignRenderer extends DefaultTableCellRenderer
{
   public RightAlignRenderer () {
     setHorizontalAlignment(SwingConstants.RIGHT);
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// NetworkColumnRenderer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class NetworkColumnRenderer extends DefaultTableCellRenderer 
{
     public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected,
             boolean hasFocus, int row, int column) {

         Component cell = super.getTableCellRendererComponent
           (table, value, isSelected, hasFocus, row, column);

         if(value instanceof FileColorCode) {
            FileColorCode colorCode = (FileColorCode)value; 
            int val = Integer.parseInt(colorCode.toString());
            if(val == -1) {
              cell.setBackground(Color.white);
              cell.setForeground(Color.white);
            }
            else if(val <= 0) {
              cell.setBackground(Color.red);
              cell.setForeground(Color.red);
            }
            else if(val > 0 && val <= 1) {
              cell.setBackground(Color.orange);
              cell.setForeground(Color.orange);
            }
            else if(val > 1 && val <= 5) {
              cell.setBackground(Color.yellow);
              cell.setForeground(Color.yellow);
            }
            else if(val > 5 && val <= 10) {
              cell.setBackground(Color.green);
              cell.setForeground(Color.green);
            }
            else if(val >= 10) {
              cell.setBackground(dGreenColor);
              cell.setForeground(dGreenColor);
            }
         }
         return cell; 
  
     }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ColorColumnRenderer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class ColorColumnRenderer extends DefaultTableCellRenderer 
{
     public ColorColumnRenderer() {
            super(); 
     }
                         
     public Component getTableCellRendererComponent
            (JTable table, Object value, boolean isSelected,
             boolean hasFocus, int row, int column) 
     {
         Component cell = super.getTableCellRendererComponent
           (table, value, isSelected, hasFocus, row, column);
         if(value instanceof FileIntf) {
          if(value.toString().equalsIgnoreCase("Active")) {
             cell.setBackground(bdColor);
           } 
          else if(value.toString().equalsIgnoreCase("Done")) {
             cell.setBackground(Color.green);
          } 
          else if(value.toString().equalsIgnoreCase("Failed")) {
             cell.setBackground(Color.red);
          } 
          else if(value.toString().equalsIgnoreCase("Skipped")) {
             cell.setBackground(Color.gray);
          } 
          else if(value.toString().equalsIgnoreCase("Exists")) {
             cell.setBackground(Color.pink);
          } 
          else if(value.toString().equalsIgnoreCase("Cancel")) {
             cell.setBackground(Color.magenta);
          } 
          else if(value.toString().equalsIgnoreCase("Pending")) {
             cell.setBackground(Color.orange);
          }
          else {
             cell.setBackground(bgColor);
          }
         }
        return cell;
    } 
}

class TextFrame extends JFrame
{
     private MyTableModel _model;
     private int _row;
     private JTextField jtf = new JTextField();

     public TextFrame(MyTableModel model, int row) {
         _model = model;
         _row = row;
         JPanel p = new JPanel(new SpringLayout());
         JPanel p1 = new JPanel(new SpringLayout());
         p.setBackground(bgColor); 
         jtf.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
              FileInfo fInfo = _model.getValueAt(_row);
              String value = jtf.getText().trim();
              int idx = value.lastIndexOf("/");
              if(idx != -1) {
                _model.setValueAt(value.substring(idx), _row, 1);
              }
              else {
                _model.setValueAt(value, _row, 1);
              }
            }
         });
         String target = (String) _model.getValueAt(_row,1);
         jtf.setText(target);
         p1.add(new JLabel("TargetURL : "));
         p1.add(jtf);
         SpringUtilities.makeCompactGrid(p1,1,2,1,1,1,1);
         JPanel p2 = new JPanel(new SpringLayout());
         JButton save = new JButton("save");
         JButton close = new JButton("close"); 
         save.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
              FileInfo fInfo = _model.getValueAt(_row);
              String value = jtf.getText().trim();
              int idx = value.lastIndexOf("/");
              if(idx != -1) {
                _model.setValueAt(value.substring(idx), _row, 1);
              }
              else {
                _model.setValueAt(value, _row, 1);
              }
              hide();
            }
         });
         close.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
               hide();
            }
         });
         p2.add(save);
         p2.add(close);
         SpringUtilities.makeCompactGrid(p2,1,2,1,1,1,1);
         p.add(p1);
         p.add(p2);
         SpringUtilities.makeCompactGrid(p,2,1,1,1,1,1);
         getContentPane().add(p);
         addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                dispose();
            }
         });
         setSize(400, 100);
     }

}
}
