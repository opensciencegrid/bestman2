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
// SRM_MSSFILE_STATUS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_MSSFILE_STATUS {

private String requestToken="";
private String explanation;
private MSS_MESSAGE status;
private long size;
private double transferRate;
private boolean alreadyReported=false;
//private boolean isDone=false;
private String remoteFileName="";
private String localFileName="";
private ExecScript currentProcess;
private FileObj fileObj;
private int numTimesStatusCalled;
private long startTimeStamp = System.currentTimeMillis();

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_MSSFILE_STATUS
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void clean() {
	currentProcess = null;
	fileObj.clean();
	fileObj = null;
}

public SRM_MSSFILE_STATUS () { 
  this.status = MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED;
  this.explanation="Request queued.";
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStartTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public long getStartTimeStamp() {
  return startTimeStamp;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setStartTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setStartTimeStamp(long startTimeStamp) {
  this.startTimeStamp = startTimeStamp;
}

public void setRequestToken(String rid) {
  requestToken = rid;
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
//getRequestToken
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRequestToken() {
  return requestToken;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSize (long size) {
 this.size = size;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public long getSize() {
 return this.size;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLocalFileName
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLocalFileName(String localFileName) {
  this.localFileName = localFileName;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLocalFileName
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getLocalFileName() {
  return this.localFileName;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setRemoteFileName
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setRemoteFileName (String remoteFileName) {
  this.remoteFileName = remoteFileName;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRemoteFileName
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getRemoteFileName () {
  return this.remoteFileName;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferRate
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferRate (double rate) {
  this.transferRate = rate;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getTransferRate
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public double getTransferRate () {
  return this.transferRate;
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

public boolean getAlreadyReported() {
 return alreadyReported;
}

public void setAlreadyReported(boolean b) {
 alreadyReported = b;
}

public int getAlreadyReportedCount() {
  return numTimesStatusCalled;
}

public void increaseAlreadyReportedCount() {
  numTimesStatusCalled++;
}

/*
public boolean getDone() {
  return isDone;
}
*/


}
