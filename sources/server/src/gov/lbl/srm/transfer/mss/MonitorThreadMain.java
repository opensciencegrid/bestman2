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

package gov.lbl.srm.transfer.mss;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//Class MonitorThreadMain
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;
import java.util.Vector;
import java.util.HashSet;
import java.util.Hashtable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class MonitorThreadMain extends Thread {

private MonitorThreadPool _pool;
private SRM_MSS _parent;

private static Log _logger;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public MonitorThreadMain (MonitorThreadPool pool, SRM_MSS parent)
{
   _pool = pool;
   _parent = parent;
   _logger = _parent.getLogger();
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run 
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run() { 
  if(_parent.getDebugLevel() >= 300) {
    util.printMessage
    ("\n\n+++ Monitor Thread started to perform tasks. +++ ",_logger,_parent.getDebugLevel());
  }
  try {
     while(true) {
        if(_parent.getMonitorThreadDisable()) {
	      util.printMessage("+++ Monitor Thread Disabled +++",_logger,_parent.getDebugLevel());
          _pool.stopRequestAllWorkers();
          _parent.setMonitorThreadToNull(); 
	      interrupt();
	      return;
	    }
        sleep(5000);  
        Hashtable statusMap = _parent.getQueueStatusMap();
        _pool.execute(statusMap);
     }
  }catch(InterruptedException ix) {
      util.printStackTrace(ix,_logger);
  }
}

}
