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

package tester;

import java.util.Hashtable;

public class ThreadCallBack extends Thread {

 private threadIntf _parent;
 private String[] args;
 private Hashtable cmd = new Hashtable();
 private SharedObjectLock sLock; 
 private String type=""; 

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ThreadCallBack
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ThreadCallBack(threadIntf parent, String type)
{
  super();
  _parent = parent;
  this.type = type;
}

public void setParams_2(Hashtable command) {
  cmd = command;
}

public void setParams_3(SharedObjectLock sLock) {
 this.sLock = sLock; 
}

public void setParams_1(String[] args) {
  this.args = args;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run()
{
  if(type.equalsIgnoreCase("execscript")) {
   _parent.processThreadRequest_1(args);
  }
  else if(type.equalsIgnoreCase("srmtester")) {
    if(sLock != null) {
      _parent.processThreadRequest_3(args,sLock);
    }
  }
  else if(type.equalsIgnoreCase("gridftp")) {
   _parent.processGridFTPWaitTimeOut(cmd);
  }
  else {
   _parent.processThreadRequest_2(type,cmd);
  }
}
}
