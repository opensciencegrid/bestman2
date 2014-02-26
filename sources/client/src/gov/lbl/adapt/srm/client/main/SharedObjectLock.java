/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.adapt.srm.client.util.*;


public class SharedObjectLock {

private int _totalCount;
private int tempCount;
private boolean _available = false;
private boolean isCancel;

private static Log logger;


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SharedObjectLock (int totalCount)
{
  _totalCount = totalCount;
   try {
    ClassLoader cl = this.getClass().getClassLoader();
    Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.SharedObjectLock");
    logger = LogFactory.getLog(c.getName());
   }catch(ClassNotFoundException cnfe) {
    System.out.println("ClassNotFoundException ");
     //throw new SRMClientException(cnfe.getMessage());
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getTotalCount
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getTotalCount ()
{
  return _totalCount;
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

public synchronized void setIncrementCount (boolean b) {
  tempCount ++;
  if(tempCount == _totalCount) {
    isCancel = b;
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
         logger.debug("Waiting now for user inputs ...");
         System.out.println("Waiting now for user inputs ...");
         wait ();
      }catch(InterruptedException e) {
         //ShowException.logDebugMessage(logger,e);
      }
    }
    return; 
}

public boolean getIsCancel () {
  return isCancel;
}

}
