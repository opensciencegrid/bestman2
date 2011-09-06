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

import java.util.Vector;
import java.util.Hashtable;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_STATUS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_STATUS {

private String requestToken="";
private String explanation;
private MSS_MESSAGE status;
private String sPath="";
private String tPath="";
private int numTimesStatusCalled;
private boolean alreadyReported=false;
private ExecScript currentProcess;
private FileObj fileObj;
private long startTimeStamp = System.currentTimeMillis();

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_STATUS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_STATUS () { 
   status = MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED;
   explanation="Request queued";
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStartTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public long getStartTimeStamp () {
   return startTimeStamp;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setStartTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setStartTimeStamp(long startTimeStamp) {
  this.startTimeStamp = startTimeStamp;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setRequestToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRequestToken(String rid) {
  requestToken = rid;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getRequestToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestToken() {
  return requestToken;
}

public ExecScript getCurrentProcess() {
  return currentProcess;
}
 
public void setCurrentProcess(ExecScript process) {
  currentProcess = process;
}

public void setFileObj (FileObj fileObj) {
   this.fileObj = fileObj;
}
 
public FileObj getFileObj () {
   return fileObj;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setExplanation
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setExplanation (String exp) {
  explanation = exp;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setStatus (MSS_MESSAGE status) {
   this.status = status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getExplanation
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getExplanation () {
  return explanation;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public MSS_MESSAGE getStatus () {
  return status;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTargetPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTargetPath(String tpath) {
  this.tPath = tpath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getTargetPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getTargetPath() {
  return tPath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSourcePath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSourcePath(String spath) {
  this.sPath = spath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSourcePath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getSourcePath() {
  return sPath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setAlreadyReported
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setAlreadyReported(boolean b) {
  alreadyReported = b;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getAlreadyReported
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean getAlreadyReported() {
  return alreadyReported; 
}

public int getAlreadyReportedCount() {
  return numTimesStatusCalled;
}

public void increaseAlreadyReportedCount() {
  numTimesStatusCalled ++;
}


}
