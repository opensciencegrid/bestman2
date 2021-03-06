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

import java.util.*;

public class SharedObjectLock {

private int _totalCount;
private int tempCount;
private boolean _available = false;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SharedObjectLock (int totalCount)
{
  _totalCount = totalCount;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTotalCount
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void setIncrementCount () {
   tempCount ++; 
   if(tempCount >= _totalCount) {
      setAvailable(true); 
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setAvailable
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void setAvailable (boolean b)
{
   _available = b;
   if(_available)
     notifyAll();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// get
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void get ()
{
    while(_available == false) { 
      try {
         //System.out.println("Waiting now for user inputs ...");
         wait ();
      }catch(InterruptedException e) {
         e.printStackTrace();
      }
    }
    return; 
}

}
