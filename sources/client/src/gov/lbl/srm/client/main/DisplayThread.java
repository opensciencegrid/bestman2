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


import java.io.*;
import java.awt.*;
import javax.swing.*;
import java.util.*;
import java.net.*;

import org.globus.util.GlobusURL;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.srm.client.transfer.*;
import gov.lbl.srm.client.transfer.globus.*;
import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// DisplayThread
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class DisplayThread extends Thread {

private FileTransferPanel _parent;
private FTPIntf _window;
private boolean isDisable;

private static Log logger;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// DisplayThread --- Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public DisplayThread (FileTransferPanel parent, FTPIntf window)
{
   _parent = parent;
   _window = window;

   try {
     ClassLoader cl = this.getClass().getClassLoader();
     Class c = cl.loadClass("gov.lbl.srm.client.main.DisplayThread");
     logger = LogFactory.getLog(c.getName());
   }catch(ClassNotFoundException cnfe) {
     System.out.println("ClassNotFoundException ");
     //throw new SRMClientException(cnfe.getMessage());
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run () {
    
  while(!isDisable) {
   try {
    Vector vec = _parent.getFileStatusVec();
    int size = vec.size();
    Vector idx = new Vector ();
    /*
    for(int i = 0; i < size; i++) {
     FileStatusGUI fgi = (FileStatusGUI) vec.elementAt(i);
     FileIntf fIntf = fgi.getFileIntf();
     Date d1 = fIntf.getTimeStamp();
     if(fIntf.getCompleted() && d1 != null) {
      Date d2 = new Date ();
      d2.setMinutes(d2.getMinutes()-1);
      int value = d2.compareTo(d1);
      if(value >= 0) {
        idx.addElement(new Integer(i));
        _window.refreshView();
      }
      else {
         break; //out of the for loop
      }
     }
    }
    for(int i= 0; i < idx.size() ; i++) {
      Integer ii = (Integer) idx.elementAt(i);
      _parent.removeElementFromVector(ii.intValue());
    }
    */
    _window.scrollView();
    //sleep(50000);
    sleep(1000);
   }catch(InterruptedException e) {
     //ShowException.logDebugMessage(logger,e);
     interrupt();
   }
  }
  //System.out.println("++++ Display thread disabled ++++");
}

public void setDisable(boolean b) {
   isDisable = b;
}

}
