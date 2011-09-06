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

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class MonitorThreadPool
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.util.*;

public class MonitorThreadPool
{ 

private ObjectFIFO idleWorkers;
private MonitorThreadWorker[] workerList;
private int numberOfThreads;
private SRM_MSS _parent;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public MonitorThreadPool (int numberOfThreads, SRM_MSS parent)
{
  numberOfThreads = Math.max(1, numberOfThreads);
  idleWorkers = new ObjectFIFO (numberOfThreads);
  workerList = new MonitorThreadWorker [numberOfThreads];
  _parent = parent;

  for(int i = 0; i < workerList.length; i++) {
     workerList[i] = new MonitorThreadWorker
		(idleWorkers,i+" name", _parent);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// execute
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void execute (Hashtable statusMap) 
	throws InterruptedException {
   MonitorThreadWorker worker = 
		(MonitorThreadWorker) idleWorkers.remove ();
   worker.process(statusMap); 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// stopRequestIdleWorkers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void stopRequestIdleWorkers () {
  try { 
    Object[] idle = idleWorkers.removeAll ();
    for(int i = 0; i < idle.length; i++) {
       ((MonitorThreadWorker) idle[i]).stopRequest (); 
    }
  }catch(InterruptedException ex) {
    Thread.currentThread().interrupt(); //re-assert
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// stopRequestAllWorkers
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void stopRequestAllWorkers ()
{
  //Stop the idle one's first
  stopRequestIdleWorkers ();

  //give the idle workers a quick chance to die
  try {
   Thread.sleep(250);
  }catch(InterruptedException ex) { }

  //Step through the list of ALL workers.
  for(int i = 0; i < workerList.length; i++) {
    if(workerList[i].isAlive ()) {
      workerList[i].stopRequest ();
    }
  }
}

}
