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

import org.ietf.jgss.GSSCredential;

import org.globus.util.GlobusURL;
import java.io.IOException;
import gov.lbl.srm.client.intf.MyISRMFileTransfer;
import gov.lbl.srm.client.transfer.globus.*;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.StringTokenizer;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// GUCScriptFileTransfer
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class GUCScriptFileTransfer 
	extends MySRMFileTransfer implements MyISRMFileTransfer {

private GSSCredential credentials;
private SRMTransferProtocol ttype = SRMTransferProtocol.GSIFTP;
private SRMTransferMode mode = SRMTransferMode.GET; 
private String source="";
private String target="";
private boolean done = false;
private long startTime = 0;
private long endTime = 0;
private long totalTime = -1;

public String status;

private Log _logger;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private boolean _silent;
private String scriptPath;
private String gucEventLogPath="";
private int bufferSize=1048576;
private int parallelism=1;
private boolean dcau = true;
private boolean debug=false;
  //input/ouput buffer size

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// GUCScriptFileTransfer --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GUCScriptFileTransfer(String source, String target, boolean debug,
	boolean silent, String scriptPath, String gucEventLogPath) {
   this.source = source;
   this.target = target;
   this.debug = debug;
   this._silent = silent;
   this.scriptPath = scriptPath;
   this.gucEventLogPath=gucEventLogPath;

}

public void setSessionType (int sessionType) {
}

public void setSessionMode (int sessionMode) {
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLogger
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLogger(Log log, java.util.logging.Logger theLogger, 
	boolean silent) {
   _logger = log;
   this._theLogger = theLogger;
   this._silent = silent;
}

public void setLogger(Log log) {
   _logger = log;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferType
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferType(SRMTransferProtocol ttypeu) {
   this.ttype = ttype;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferMode
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferMode(SRMTransferMode mode) {
   this.mode = mode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setParallel
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setParallel(int parallelu) { 
  this.parallelism = parallelu;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCredential
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCredentials(GSSCredential credentials) {
   this.credentials = credentials;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setBufferSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setBufferSize(int size) { 
  this.bufferSize = size;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setDCAU
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setDCAU(boolean dcau) {
  this.dcau = dcau;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// start
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void transfer () {
  this.start();
}

public void transfer(String source, String target) {
  this.source = source;
  this.target = target;
  this.start();
}

public void cancel () { }

public boolean transferDone () { 
  return done;
}

public void transferSync() {
  gucFileTransfer();
}

public long getSourceFileSize() {
  return -1;
}

public long getTransferTimeInMilliSeconds () {
   if(done) {
     if(startTime > 0) {
       totalTime =  endTime - startTime;
     }
   }
   return totalTime;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run() {
  transferSync();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// gucFileTransfer
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void gucFileTransfer() {
  
 startTime = System.currentTimeMillis();
 try {
    File f = new File(scriptPath); 
    if(f.exists()) {
      Vector vec = new Vector ();
      if(!_silent) {
        vec.addElement("-dbg");			   
      }
      //vec.addElement("-bs");
      vec.addElement("-tcp-bs");
      vec.addElement(""+bufferSize);
      vec.addElement("-p");
      vec.addElement(""+parallelism);
	  if(!dcau) {
        vec.addElement("-nodcau");			   
	  }
	  vec.addElement(source);
	  vec.addElement(target);
      //if(!gucEventLogPath.equals("")) {
        //vec.addElement(">&");
        //vec.addElement(">");
        //vec.addElement(gucEventLogPath);
	    //vec.addElement("2>&1");
      //}
	  String[] command = new String[vec.size()+1];
	  command[0] = scriptPath;
	  for(int i = 0; i < vec.size(); i++) {
	     command[i+1] = (String) vec.elementAt(i);
	  }
	  ExecScript process = new ExecScript(scriptPath,
					  	gucEventLogPath, "",true,debug);
	  boolean b = process.execCommand(command,false);
	  if(!b) {
        endTime = System.currentTimeMillis();
        this.status = "Error executing script " + scriptPath;
	  }
	  else {
       endTime = System.currentTimeMillis();
       done = true;
	  } 
    }
    else {
      String temp = "Script does not exists " + scriptPath;
      System.out.println(temp);
      this.status = temp;
    }
  }catch(Exception e) {
     //inputVec = new Vector();
     //inputVec.addElement("Exception="+e.getMessage());
     //util.printEventLog(_theLogger,"GUCScriptFileTransfer.gucFileTransfer",inputVec);
     System.out.println("SRM-CLIENT: EXCEPTION=" + e.getMessage());
     //_logger.debug("Exception " + e.getMessage());
     this.status = e.getMessage();
  }
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getStatus () {
  return this.status;
}

}
