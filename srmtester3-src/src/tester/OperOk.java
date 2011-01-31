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

import gov.lbl.srm.StorageResourceManager.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//import gov.lbl.srm.server.TSRMUtil;

import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;
import java.util.Vector;
import java.util.HashMap;

public class OperOk {

  private boolean tried = false;
  private boolean isOperOk = false;
  private boolean isOperOkDummy = true;
  private HashMap map = new HashMap();
  private String reason = "";
  
  public boolean isOperOk () {
    return isOperOk;
  }
  public boolean isOperOkDummy() {
     return isOperOkDummy;
  }
  public HashMap getSubOperations() {
    return map;
  }
  public void addSubOperations(String key) {
    map.put(key,new OperOk());
  }
  public void isOperOk(boolean b) {
     isOperOk = b;
  }
  public void isOperOkDummy(boolean b) {
     isOperOkDummy = b;
  }
  public void setExplanation(String explanation) {
     reason = explanation;
  }
  public String getExplanation() {
     return reason;
  }
  public void setTried(boolean b) {
     tried = b;
  }
  public boolean isTried() {
     return tried;
  }
}
