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

package gov.lbl.adapt.srm.client.intf;

import gov.lbl.adapt.srm.client.main.FileEventListener;

import java.util.Date;

public interface FileIntf {
  public void skipped(boolean b);
  public void used(boolean b);
  public void setFailed(boolean b);
  public boolean getFailed();
  public boolean  isSkipped();
  public boolean  isUsed();
  public void setLabel(int row);
  public int getLabel();
  public void retry();
  public void setRetry(int retry);
  public void setRID(String rid);
  public void setGetRID(String rid);
  public String getRID();
  public String getGetRID();
  public int getRetry();
  public void setRetryTime(Date d);
  public boolean isRetryOk(int retryTimeOut);
  public boolean isRetry();
  public void setNew(boolean b);
  public boolean isNew();
  public void setSURL(String turl);
  public void setTURL(String turl);
  public void setOrigSURL(String ourl);
  public void setOrigTURL(String ourl);
  public String getSURL(); 
  public void setTransferURL(String tturl);
  public String getTransferURL();
  public void setExtLifeTime(long ll);
  public long getExtLifeTime();
  public void setIsRecursive(Boolean b);
  public Boolean getIsRecursive(); 
  public String getOrigSURL(); 
  public String getOrigTURL(); 
  public String getTURL(); 
  public String getTargetDir(); 
  public void setExpectedSize(String s); 
  public void setIsDirectGsiFTP(boolean b);
  public boolean getIsDirectGsiFTP();
  public String getExpectedSize(); 
  public int getExpectedSize2(); 
  public void setActualSize (String size);
  public String getActualSize(); 
  public String getTimeTaken();
  public void setTimeTaken(String str);
  public void setErrorMessage(String str);
  public String getErrorMessage();
  public void setStatusLabel(String status);
  public void setFileStatus(String status);
  public void setFileExplanation(String status);
  public String getFileStatus();
  public String getFileExplanation();
  public String getStatusLabel();
  public void setTargetDir(String dir);
  public void setStartTime(long time);
  public long getStartTime();
  public void setEndTime(long time);
  public long getEndTime();
  public void setTimeStamp(Date d); 
  public Date getTimeStamp();
  public void setPutDoneFailed(boolean b);
  public boolean getPutDoneFailed();
  public void setDuplicationError(boolean b);
  public boolean getDuplicationError();
  public void setCompleted(boolean b);
  public boolean getCompleted();
  public void setActualFileSizeKnown(boolean b);
  public boolean getActualFileSizeKnown();
  public boolean getActualFileSizeSetAlready();
  public void addListeners(FileEventListener fEventListener);
  public void updateListeners();
  public String toString();
  public String writeFileInfo();
  public int getStatusCode();
  public void setLahfs(boolean b);
  public boolean getLahfs();
  public boolean getShowSize();
  public void setHostInfo(String str);
  public String getHostInfo();
  public void setMode(String mode);
  public void overWriteTURL(boolean b);
  public boolean getOverWriteTURL();
  public String getMode();
  public boolean isAbortFilesCalled();
  public void setAbortFilesCalled(boolean b);
  public void setFileFailedAfterAllowedRetries(boolean b);
  public boolean getFileFailedAfterAllowedRetries();
  public void doNotReleaseFile(boolean b);
  public boolean getDoNotReleaseFile();
}
