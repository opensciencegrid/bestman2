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

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Hashtable;

import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;

import org.apache.commons.logging.Log;

public class FileTransferPanel implements colorIntf {
  private Hashtable ht = new Hashtable();
  private Log logger;
  private java.util.logging.Logger _theLogger;
  private FTPIntf _ftpIntf;
  private boolean isRenew;
  private int concurrency;
  private Vector fInfo;
  private Vector vec = new Vector ();
  //private DisplayThread dThread; 
  private boolean gui;
  private String reqMode;
  private Vector inputVec = new Vector();
  private boolean silent;
  private boolean useLog;


  public FileTransferPanel (Vector fInfo, 
     int concurrency, Log logger, java.util.logging.Logger theLogger,
	FTPIntf ftpIntf, boolean isRenew,
	boolean gui, String reqMode, boolean silent,boolean useLog) {

     this.logger = logger;
     this._theLogger = theLogger;
     this._ftpIntf = ftpIntf;
     this.fInfo = fInfo;
     this.concurrency = concurrency;
     this.gui = gui;
     this.reqMode = reqMode;
     this.silent = silent;
     this.useLog = useLog;

     int ssize = concurrency*2;

     if(this.fInfo.size() < ssize) {
        ssize = this.fInfo.size();
     }

     //logger.debug("Creating progress monitors initial size " + ssize);
     inputVec = new Vector();
     inputVec.addElement("Creating progress monitors initial size " + ssize);
     util.printEventLog(_theLogger,"FileTransferPanel.constructor",
                inputVec,silent,useLog);
     _ftpIntf.prepareView(); //clear all the old monitors
     
     if(gui) {
       for (int i = 0; i < ssize; i++) {
          FileIntf fIntf = (FileIntf) fInfo.elementAt(i);
          try {
            FileStatusGUI fileStatusGUI = new FileStatusGUI(fIntf);
            ht.put(new Integer(i), fileStatusGUI);
            vec.addElement(fileStatusGUI);
            _ftpIntf.updateView(fileStatusGUI);
          }catch(Exception e) {
            inputVec = new Vector();
            inputVec.addElement("Exception="+e.getMessage());
            //ShowException.logDebugMessage(logger,e);
            util.printEventLog(_theLogger,"FileTransferPanel:constructor",inputVec,silent,useLog);
            ShowException.showMessageDialog(_ftpIntf.getParentWindow(),
			"Exception : " + e.getMessage());
          }
       }
       //logger.debug("Starting Display Thread");
       //dThread = new DisplayThread(this,_ftpIntf);
       //dThread.start();
     }
}

public synchronized void createNextProgressBar (int i) {
   inputVec = new Vector();
   inputVec.addElement("Creating next progress monitor for index=" + i);
   util.printEventLog(_theLogger,"FileTransferPanel:createNextProgressBar",inputVec,silent,useLog);
   if(gui) {
     FileIntf fIntf = (FileIntf) fInfo.elementAt(i);
     try {
     FileStatusGUI fileStatusGUI = new FileStatusGUI(fIntf);
     ht.put(new Integer(i), fileStatusGUI);
     vec.addElement(fileStatusGUI);
     _ftpIntf.updateView(fileStatusGUI);
     }catch(Exception e) {
       inputVec = new Vector();
       inputVec.addElement("Exception="+e.getMessage());
       util.printEventLog(_theLogger,"FileTransferPanel:createNextProgressBar",inputVec,silent,useLog);
       //ShowException.logDebugMessage(logger,e);
       ShowException.showMessageDialog(_ftpIntf.getParentWindow(),
		"Exception : " +  e.getMessage());
     } 
  }
}

public synchronized void removeElementFromVector(int i) {
  inputVec = new Vector();
  inputVec.addElement("Display thread removing element from vector=" + i);
  util.printEventLog(_theLogger,"FileTransferPanel:removeElementFromVector",inputVec,silent,useLog);
  if(!gui) return;

  if(i >= vec.size()) return;

  FileStatusGUI fileStatus = (FileStatusGUI) vec.elementAt(i);
  FileIntf fIntf = fileStatus.getFileIntf();
  vec.remove(i);
  ht.remove(new Integer(fIntf.getLabel()));
  if(ht.size() == 0 && vec.size() == 0) {
    //if(dThread != null) {
      //dThread.setDisable(true);
    //}
  }
}

/*
public void setDisplayThreadDisable (boolean b) {
  if(dThread != null) {
    dThread.setDisable(b);
  }
}
*/

public void setNewConcurrency(int conc) {
  concurrency = conc;
}

public FTPIntf getParentIntf () {
  return _ftpIntf;
}

public Log getLogger() {
   return this.logger;
}

public void setCurrentTargetIndex(int idx) {
  if(!gui) return;
  _ftpIntf.setCurrentTargetIndex(idx);
}

public Vector getFileStatusVec () {
   return vec;
}

public FileStatusGUI getProgressBar (int idx) {
   if(!gui) return null;

   FileStatusGUI fgui = (FileStatusGUI) ht.get(new Integer(idx));
   if(fgui != null) {
     return fgui;
   }
   return null;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// addNextFileInfo
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void addNextFileInfo(FileIntf fIntf) {
   this.fInfo.addElement(fIntf);
}
}
