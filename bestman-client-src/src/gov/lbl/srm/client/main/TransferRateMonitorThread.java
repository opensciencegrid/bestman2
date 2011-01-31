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
// TransferRateMonitorThread
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class TransferRateMonitorThread extends Thread {

private FileTransferTest _parent;
private boolean isDisable;

private static Log logger;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private boolean debug;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// TransferRateMonitorThread --- Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public TransferRateMonitorThread (FileTransferTest parent,
	java.util.logging.Logger theLogger)
{
   _parent = parent;
   _theLogger = theLogger;
   debug = _parent.getDebug();

   try {
      ClassLoader cl = this.getClass().getClassLoader(); 
      Class c = 
		cl.loadClass("gov.lbl.srm.client.main.TransferRateMonitorThread");
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
    Vector v1 = _parent.getActiveFiles();
    Vector v2 = _parent.getActiveMyISRMFileTransfers();
    int s1 = v1.size();
    int s2 = v2.size();
    if(s1 != 0 && s2 != 0) {
      for(int i = 0; i < s1; i++) {
        File f = (File) v1.elementAt(i);
        double size =  f.length();
        WrappedFT wft = (WrappedFT) v2.elementAt(i);
        wft.setTransferRate(size);
      }
    }
    //sleep(60000);
    sleep(1000);
   }catch(InterruptedException e) {
     interrupt();
   }
  }
  /*
  inputVec = new Vector ();
  inputVec.addElement("TransferMonitorThreadDisabled");
  util.printEventLog(_theLogger,"TransferRateMonitorThread.run",inputVec);
  */
  if(debug) {
   System.out.println("TransferMonitorThreadDisabled");
  }
}

public void setDisable(boolean b) {
   isDisable = b;
}

}
