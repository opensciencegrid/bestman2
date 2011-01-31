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

import javax.swing.*;
import java.util.*;

import gov.lbl.srm.client.intf.*;

public class FileInfo implements FileIntf{
  private int _row;
  private String surl="";
  private String _rid="";
  private String _getRid="";
  private String osurl="";
  private String oturl="";
  private String turl="";
  private String tdir="";
  private String transferUrl="";
  private Boolean isRecursive = new Boolean(false);
  private boolean abortFilesCalled;
  private boolean isOverwriteFiles = false;
  private long _extLifeTime=0;
  private boolean _completed;
  private boolean _actualFileSizeKnown;
  private boolean _failed;
  private boolean mkdirCreation;
  private String expectedSize ="100000000";
  private String actualSize = "";
  private String timeTaken = "";
  private String errorMessage = "";
  private String fileStatus="";
  private String fileExplanation="";
  private int _statusCode;
  private boolean _used = false;
  private boolean _skipped = false;
  private boolean _existsInTarget=false;
  private String _statusLabel=""; 
  private Vector _listeners = new Vector ();
  private long _sTime;
  private long _eTime;
  private Date _date;
  private boolean _lahfs;
  private boolean showSize;
  private boolean isDirectGsiFTP;
  private boolean doNotReleaseFile;
  private boolean _putDoneFailed;
  private boolean _duplicationError;
  private String hostInfo;
  private String _mode;
  private boolean overWriteTURL = true;
  public int retry;
  public boolean _tried = true;
  public int timestamp;
  public boolean fileFailedAfterAllowedRetries;
  public boolean actualSizeSetAlready;

  public FileInfo () {
  }

  public void setMode(String mode) {
    _mode = mode;
  }

  public String getMode() {
    return _mode;
  }

  public void setFileExistsInTarget(boolean b) {
     _existsInTarget = b; 
  }

  public boolean getFileExistsInTarget() {
   return _existsInTarget; 
  }

  public void setMkdirCreationOk(boolean b) {
    mkdirCreation = b;
  }

  public boolean getMkdirCreationOk() {
    return mkdirCreation; 
  }

  public void setActualFileSizeKnown(boolean b) {
     _actualFileSizeKnown = b; 
  }

  public boolean getActualFileSizeKnown() {
     return _actualFileSizeKnown; 
  }

  public void setRID(String rid) {
    _rid = rid;
  }

  public String getRID() {
    return _rid; 
  }

  public void setGetRID(String rid) {
    _getRid = rid;
  }

  public String getGetRID() {
    return _getRid; 
  }

  public void addListeners(FileEventListener fEListener) {
    _listeners.add(fEListener);
  }

  public void updateListeners() {
     for(int i = 0; i < _listeners.size(); i++) {
         FileEventListener  fEListener = 
		(FileEventListener) _listeners.elementAt(i);
         fEListener.fileExists(_row);
     }
  }

  
  public void setFileStatus (String fileStatus) {
    this.fileStatus = fileStatus;
  }

  public void setFileExplanation (String fileExplanation) {
    this.fileExplanation = fileExplanation;
  }

  public String getFileStatus () {
    return this.fileStatus;
  }

  public String getFileExplanation () {
    return this.fileExplanation;
  }

  public void setStatusLabel(String statusLabel) {
    _statusLabel = statusLabel;
    if(_statusLabel.equalsIgnoreCase("active")) { 
      _statusCode = 1;
    }
    else if(_statusLabel.equalsIgnoreCase("failed")) { 
      _statusCode = 0;
    }
    else if(_statusLabel.equalsIgnoreCase("done")) { 
      _statusCode = 2;
    }
    else if(_statusLabel.equalsIgnoreCase("pending")) {
      _statusCode = 3;
    }
    else if(_statusLabel.equalsIgnoreCase("cancel")) {
      _statusCode = 4;
    }
    else if((_statusLabel.equalsIgnoreCase("skipped")) ||
            (_statusLabel.equalsIgnoreCase("exists"))) {
           
      _statusCode = 5;
    }
  }

  public void setExtLifeTime(long ll) {
    _extLifeTime=ll;
  }

  public long getExtLifeTime() {
    return _extLifeTime;
  }

  public void setIsRecursive(Boolean b) {
    isRecursive = b;
  }

  public Boolean getIsRecursive() {
    return isRecursive;
  }

  public void setOverwriteFiles(boolean b) {
    isOverwriteFiles = b;
  }

  public boolean getOverwriteFiles() {
    return isOverwriteFiles;
  }

  public void setCompleted(boolean b) {
    _completed = b;
  }

  public boolean getCompleted() {
    return _completed;
  }

  public void setPutDoneFailed(boolean b) {
    _putDoneFailed = b;
  }

  public boolean getPutDoneFailed() {
    return _putDoneFailed;
  }

  public void setDuplicationError(boolean b) {
    _duplicationError = b;
  }

  public boolean getDuplicationError() {
    return _duplicationError;
  }

  public void setFailed(boolean b) {
    _failed= b;
  }

  public boolean getFailed() {
    return _failed; 
  }

  public String getStatusLabel() {
    return _statusLabel.trim();
  }

  public void setSURL (String surl) {
    this.surl = surl;
  }

  public void setTransferURL(String tturl) {
    this.transferUrl = tturl;
  }

  public void setOrigSURL(String osurl) {
    this.osurl = osurl;
  }

  public void setOrigTURL(String oturl) {
    this.oturl = oturl;
  }

  public String getOrigTURL() {
    return this.oturl; 
  }

  public String getTransferURL() {
    return this.transferUrl;
  }

  public void setTURL (String turl) {
    this.turl = turl;
  }

  public void setTargetDir (String tdir) {
    this.tdir = tdir;
  }

  public void setExpectedSize(String size) {
    if(size != null && !size.equals("null")) {
      this.expectedSize = size;
    }
  }

  public void setIsDirectGsiFTP(boolean b) {
    isDirectGsiFTP = b;
  }

  public boolean getIsDirectGsiFTP() {
    return isDirectGsiFTP; 
  }

  public int getExpectedSize2() {
    if(this.expectedSize == null ||
       this.expectedSize.equals("null")) return 0;
    else {
      try {
        int x = Integer.parseInt(this.expectedSize);
        return x;
      }catch(NumberFormatException nfe) {}
    }
    return 0;
  }

  public String getExpectedSize() {
    if(this.expectedSize == null) return "100000000";
    if(this.expectedSize.equals("null")) return "100000000";
    return this.expectedSize.trim();
  }

  public void setHostInfo(String str) {
    this.hostInfo = str;
  }

  public String getHostInfo() {
    if(this.hostInfo == null) return "";
    return this.hostInfo.trim(); 
  }

  public String getSURL () {
    return this.surl.trim();
  }

  public String getOrigSURL () {
    return this.osurl.trim();
  }

  public void retry() {
    retry++; 
  }

  public void setRetry(int retry) {
    this.retry = retry;
  }

  public boolean isNew() {
    return _tried;
  }

  public void setNew(boolean b) {
    _tried = b;
  }

  public boolean isRetry() {
    return (retry > 0);
  }

  public void setRetryTime(Date d) {
    int minutes = d.getMinutes();
    int seconds = d.getSeconds();
    timestamp = minutes*60+seconds;
  }

  public boolean isRetryOk(int retryDuration) {
    Date d = new Date();
    int minutes = d.getMinutes();
    int seconds = d.getSeconds();
    int currentTimeStamp = minutes*60+seconds;
    if(currentTimeStamp-timestamp >= retryDuration) {
       return true;
    }
    return false;
  }

  public int getRetry() {
    return retry;
  }

  public String getTURL () {
    return this.turl.trim();
  }

  public String getTargetDir () {
    return this.tdir.trim(); 
  }

  public void setLabel(int row) {
    _row = row;
  }

  public int getLabel() {
     return _row;
  }

  public void used(boolean b) {
    _used = b;
  }

  public boolean isUsed() {
    return _used;
  }

  public void skipped(boolean b) {
    _skipped = b;
    updateListeners();
  }

  public boolean isSkipped() {
    return _skipped;
  }

  public void setTimeTaken (String timeTaken) {
    this.timeTaken  = timeTaken;
  }

  public void setErrorMessage (String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage () {
    if(this.errorMessage != null) {
      return this.errorMessage.trim(); 
    }
    else {
      return this.errorMessage; 
    }
  }

  public void setActualSize (String actualSize) {
    this.actualSize  = actualSize;
    actualSizeSetAlready=true;
  }

  public boolean getActualFileSizeSetAlready() {
    return actualSizeSetAlready;
  }

  public String getActualSize () {
    return actualSize.trim();
  }

  public String getTimeTaken() {
    return this.timeTaken.trim();
  }

  public void setStartTime(long time) { 
    _sTime = time;
  }

  public long getStartTime () {
    return _sTime;
  }

  public void setEndTime(long time) { 
    _eTime = time;
  }

  public long getEndTime () {
    return _eTime;
  }

  public void setTimeStamp(Date d) {
    _date = d;
  }

  public Date getTimeStamp() {
     return _date;
  }


  public String toString() {
    return _statusLabel + " " + _row;
  }

  public String writeFileInfo() {
    StringBuffer buf = new StringBuffer(250);
    buf.append("\tSURL="+surl.trim()+"\n");
    buf.append("\tTURL="+tdir.trim()+turl.trim()+"\n");
    buf.append("\tExpectedSize="+expectedSize.trim()+"\n");
    buf.append("\tActualSize="+actualSize.trim()+"\n");
    buf.append("\tStatus="+_statusLabel.trim()+"\n");
    buf.append("\tTimeTaken="+timeTaken.trim()+"\n");
    buf.append("\tMessage="+errorMessage.trim()+"\n");
    return buf.toString();
  }

  public int getStatusCode() {
    return _statusCode;
  }

  public void setLahfs (boolean b) {
    _lahfs = b;
  }

  public boolean getLahfs () {
    return _lahfs;
  }

  public void setShowSize(boolean b) {
    showSize = b;
  }

  public void overWriteTURL(boolean b) {
    overWriteTURL = b;
  }

  public boolean getOverWriteTURL() {
    return overWriteTURL;
  }

  public boolean getShowSize() {
    return showSize;
  }

  public boolean isAbortFilesCalled() {
    return abortFilesCalled;
  }

  public void setAbortFilesCalled(boolean b) {
    abortFilesCalled = b;
  }

  public void setFileFailedAfterAllowedRetries(boolean b) {
    fileFailedAfterAllowedRetries = b;
  }

  public boolean getFileFailedAfterAllowedRetries() {
    return fileFailedAfterAllowedRetries; 
  }

  public void doNotReleaseFile(boolean b) {
     doNotReleaseFile=b;
  }

  public boolean getDoNotReleaseFile() {
     return doNotReleaseFile;
  }
}
