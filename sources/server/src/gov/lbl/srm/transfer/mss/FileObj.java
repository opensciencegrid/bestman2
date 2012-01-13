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

import java.util.Vector;
import java.util.Hashtable;

import gov.lbl.srm.transfer.mss.intf.*;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// FileObj
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class FileObj {
 
private SRM_ACCESS_TYPE accessType;
private SRM_ACCESS_INFO accessInfo;
private mssIntf mssintf;
private String type="";
private String localFileName;
private String remoteFileName;
private String requestToken;
private long fileSize;
private String filePath;
private SRM_PATH dirs;
private SRM_OVERWRITE_MODE overwritemode = 
	SRM_OVERWRITE_MODE.SRM_MSS_OVERWRITE_YES;

private SRM_MSSFILE_STATUS _fileStatus;
private SRM_PATH _path;
private SRM_FILE _file;
private SRM_STATUS _mkDirStatus;

private MSS_MESSAGE pftpStatus;
private String explanation="";

private ThreadCallBack taskThread;

private boolean isDir;
private boolean recursive;
private boolean srmnocipher;
private boolean srmnonpassivelisting;
private boolean alreadyReported;

private int _retry;
private int _retryTime;
private int timeStamp;
private String _ftpTransferLog;
private String tapeId="";
private String hsiPath=""; //we need this don't remove

// ==>added by Junmin
private SRM_MSS _parent;
public void clean() {
	taskThread = null;
	_parent.finished(requestToken);
}
// <==

public FileObj( SRM_MSS mss, SRM_ACCESS_TYPE accessType, 
                SRM_ACCESS_INFO accessInfo,
                mssIntf mssintf, String rid,
				String type, Object obj) {

			_parent = mss;
            this.accessType = accessType;
            this.accessInfo = accessInfo;
            this.mssintf = mssintf;
            this.requestToken = rid;
            this.pftpStatus = MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED;
            this.type = type;
            if(obj instanceof SRM_STATUS) {
              _mkDirStatus = (SRM_STATUS)obj;
            }
            else if(obj instanceof SRM_FILE) {
              _file = (SRM_FILE)obj;
            }
            else if(obj instanceof SRM_PATH) {
              _path = (SRM_PATH)obj;
            }
            else if(obj instanceof SRM_MSSFILE_STATUS) {
              _fileStatus = (SRM_MSSFILE_STATUS)obj;
            }
}

public FileObj() { }

public void setIsDir(boolean b) {
  isDir = b;
}

public boolean getIsDir() {
  return isDir;
}


public String getUserId() {
  return accessInfo.getUserId();
}

public String getRequestToken () {
  return requestToken;
}

public void setDirs(SRM_PATH pp) {
  dirs = pp;
}

public SRM_PATH getDirs() {
  return dirs;
}

public mssIntf getMSSIntf() {
  return this.mssintf;
}

public void setPFTPStatus(MSS_MESSAGE pftpStatus) {
  this.pftpStatus = pftpStatus;
}

public MSS_MESSAGE getPFTPStatus() {
  return this.pftpStatus;
}

public void setExplanation(String explanation) {
  this.explanation = explanation;
}

public String getExplanation() {
  return explanation; 
}

public void setTaskThread(ThreadCallBack taskThread) {
  this.taskThread = taskThread;
}

public ThreadCallBack getTaskThread() {
  return taskThread;
}

public SRM_ACCESS_TYPE getAccessType() {
  return this.accessType;
}

public SRM_ACCESS_INFO getAccessInfo() {
  return this.accessInfo;
}

public void setSource(String source) {
  this.localFileName = source;
}

public String getSource() {
  return this.localFileName;
}

public void setTarget(String target) {
  this.remoteFileName = target;
}

public String getTarget() {
  return this.remoteFileName;
}

public String getSourcePath() {
  String tt = "";
  if(type.equalsIgnoreCase("get")) {
    tt = remoteFileName;
  }
  else if(type.equalsIgnoreCase("ls") || type.equalsIgnoreCase("delete")
		|| type.equalsIgnoreCase("getfilesize")) {
    tt = filePath;
  }
  else if(type.equalsIgnoreCase("put") || type.equalsIgnoreCase("copy")) {
    tt = localFileName;
  }
  else if(type.equalsIgnoreCase("mkdir")) {
    tt = dirs.getDir();
  }

  if(tt.endsWith("/")) {
    tt = tt.substring(0,tt.length()-1);
  }

  int idx = tt.lastIndexOf("/");
  if(idx != -1) {
     return tt.substring(idx+1);
  }

  return tt;
}

public void setFileSize(long fileSize) {
  this.fileSize = fileSize;
}

public long getFileSize() {
  return this.fileSize;
}

public void setAlreadyReported(boolean b) {
  alreadyReported = b;
}

public boolean getAlreadyReported() {
  return alreadyReported;
}

public String getType() {
  return this.type;
}

public void incrementNumRetry () {
  _retry ++;
}

public int getNumRetry () {
  return _retry;
}

public void setRetryTime(int retryTime) {
  _retryTime = retryTime;
}

public int getRetryTime() {
  return _retryTime;
}

public void setStatus(MSS_MESSAGE status) {
  Object obj = getSRMMSSStatus();
  if(obj != null) {
    if(obj instanceof SRM_MSSFILE_STATUS) {
      ((SRM_MSSFILE_STATUS)obj).setStatus(status);
    }
    if(obj instanceof SRM_PATH) {
      ((SRM_PATH)obj).setStatus(status);
    }
    if(obj instanceof SRM_STATUS) {
      ((SRM_STATUS)obj).setStatus(status);
    }
    if(obj instanceof SRM_FILE) {
      ((SRM_FILE)obj).setStatus(status);
    }
  }
}

public MSS_MESSAGE getStatus() {
  Object obj = getSRMMSSStatus();
  if(obj != null) {
    if(obj instanceof SRM_MSSFILE_STATUS) {
      return ((SRM_MSSFILE_STATUS)obj).getStatus();
    }
    if(obj instanceof SRM_PATH) {
      return ((SRM_PATH)obj).getStatus();
    }
    if(obj instanceof SRM_STATUS) {
      return ((SRM_STATUS)obj).getStatus();
    }
    if(obj instanceof SRM_FILE) {
      return ((SRM_FILE)obj).getStatus();
    }
  }
  return null;
}

public SRM_FILE getSRMFile() {
  return _file;
}

// this function is used in recursive calls for ls
public void setSRMPath(SRM_PATH path) {
  _path = path;
}

public SRM_PATH getSRMPath() {
  return _path;
}

public SRM_STATUS getSRMMkdirStatus() {
  return _mkDirStatus;
}

public SRM_MSSFILE_STATUS getSRMMSSFileStatus() {
  return _fileStatus;
}

public Object getSRMMSSStatus() {
  if(_fileStatus != null) {
    return _fileStatus;
  } 
  else if(_path != null) {
    return _path;
  }
  else if(_file != null) {
    return _file;
  }
  else if(_mkDirStatus != null) {
    return _mkDirStatus;
  }
  return null;
}

public String getFTPTransferLog() {
  return _ftpTransferLog;
}


public void setFTPTransferLog(String ftpTransferLog) {
  _ftpTransferLog = ftpTransferLog;
}

public void setRecursive(boolean b) {
  recursive = b;
}

public boolean getRecursive() {
  return recursive;
}

public void setSRMNoCipher(boolean b) {
  srmnocipher = b;
}

public boolean getSRMNoCipher() {
  return srmnocipher;
}

public void setSRMNonPassiveListing(boolean b) {
  srmnonpassivelisting = b;
}

public boolean getSRMNonPassiveListing() {
  return srmnonpassivelisting;
}

public void setHSIPath(String hsiPath) {
  this.hsiPath = hsiPath;
}

public String getHSIPath() {
  return hsiPath;
}

public void setFilePath(String s) {
  filePath = s;
}

public String getFilePath() {
  return filePath;
}

public void setTapeId(String tapeId) {
  this.tapeId = tapeId;
}

public String getTapeId() {
  return tapeId;
}

public SRM_OVERWRITE_MODE getOverWriteMode() {
  return overwritemode;
}

public void setOverWriteMode(SRM_OVERWRITE_MODE mode) {
  if(mode != null) { 
    this.overwritemode = mode;
  } 
}

public void setTimeStamp(Calendar c) {
  int minutes = c.get(Calendar.MINUTE);
  int seconds = c.get(Calendar.SECOND);
  timeStamp = minutes*60+seconds;
}

public int getTimeStamp() {
  return timeStamp;
}

public int getCurrentTimeStamp() {
  Calendar c = new GregorianCalendar();
  int minutes = c.get(Calendar.MINUTE);
  int seconds = c.get(Calendar.SECOND);
  return minutes*60+seconds;
}

public String toString() {
  if(type.equalsIgnoreCase("get")) {
    return remoteFileName;
  }
  else if(type.equalsIgnoreCase("put") || type.equalsIgnoreCase("copy")) {
    return localFileName;
  }
  else if(type.equalsIgnoreCase("ls")) {
    return filePath;
  }
  else if(type.equalsIgnoreCase("delete")) {
    return filePath;
  }
  else if(type.equalsIgnoreCase("getfilesize")) {
    return filePath;
  }
  else if(type.equalsIgnoreCase("mkdir")) {
    if(dirs.getDir() == null) return "";
    else return dirs.getDir();
  }
  return "";
}

}
