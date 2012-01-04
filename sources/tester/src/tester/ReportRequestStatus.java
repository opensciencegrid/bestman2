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

public class ReportRequestStatus {

    private String requestId="";
    private String state="";
    private String explanation="";
    private String spaceToken="";
    private String startTimeStamp="";
    private String endTimeStamp="";
    private String remainingTotalRequestTime="";
    private String size="";
    private String userDesc="";
    private String uid="";
    private String overwrite="";
    private String recursive="";
    private String fileStorageType="";
    private String desiredRequestTime="";
    private String targetSURLTime="";
    private String desiredPinLifeTime="";
    private String desiredFileLifeTime="";
    private String protocol="";
    private String accessPattern="";
    private String connectionType="";
    private String accessLatency="";
    private String retentionPolicy="";
    private String storageSystemInfo="";
    private String targetStorageSystemInfo="";
    private String expectedResult="";
    private String matchingInfo="";
    private String actualResult="";
    private String linkName="";
    private String srmServicePath="";
    private ReportRequestFileStatus[] fileStatuses;
    private String totalSpace="";
    private String guarnSpace="";
    private String lifeTime="";
    private String newSpaceSize="";
    private String newSpaceTime="";

    public ReportRequestStatus() {
    }

    public void setUserDescription(String userDesc) {
       this.userDesc = userDesc;
    }
    
    public String getUserDescription() {
       return userDesc;
    }

    public String getSrmServicePath() {
      return srmServicePath;
    }

    public void setSrmServicePath(String srmServicePath) {
      this.srmServicePath = srmServicePath;
    }

    public void setTotalSpace(String tSpace) {
      totalSpace = tSpace;
    }

    public String getTotalSpace() {
      return totalSpace; 
    }

    public void setGuarnSpace(String gSpace) {
      guarnSpace = gSpace;
    }

    public String getGuarnSpace() {
      return guarnSpace; 
    }

    public void setLifeTime(String lTime) {
      lifeTime = lTime;
    }

    public String getLifeTime() {
      return lifeTime; 
    }

    public void setNewSpaceSize(String newSize) {
      this.newSpaceSize = newSpaceSize;
    }

    public String getNewSpaceSize() {
      return newSpaceSize;
    }

    public void setNewLifeTime(String newTime) {
      this.newSpaceTime = newSpaceTime;
    }

    public String getNewLifeTime() {
      return newSpaceTime;
    }

    public void setExpectedResult(String expectedResult) {
       this.expectedResult = expectedResult;
    }

    public String getExpectedResult() {
       return expectedResult;
    }
    
    public void setUserId(String uid) {
       this.uid = uid;
    }

    public String getUserId() {
       return uid;
    }

    public String isExpectedValue() {
       if(matchingInfo.equalsIgnoreCase("Yes")) {
         return "Yes";
       }
       else {
         return "No";
       }
    }

    public void setLinkName(String linkName) {
       this.linkName = linkName;
    }

    public String getLinkName() {
       return linkName;
    }

    public void setFileStorageType(String fileStorageType) {
       this.fileStorageType = fileStorageType;
    }

    public String getFileStorageType() {
       return fileStorageType;
    }

    public void setDesiredTotalRequestTime(String requestTime) {
       this.desiredRequestTime = requestTime; 
    }

    public String getDesiredTotalRequestTime() {
       return desiredRequestTime; 
    }

    public void setTargetSURLTime(String surlTime) {
       this.targetSURLTime = surlTime;
    }

    public String getTargetSURLTime() {
       return this.targetSURLTime;
    }

    public void setDesiredPinLifeTime(String pinLifeTime) {
       this.desiredPinLifeTime = pinLifeTime;
    }

    public String getDesiredPinLifeTime() {
       return this.desiredPinLifeTime;
    }

    public void setDesiredFileLifeTime(String fileLifeTime) {
       this.desiredFileLifeTime = fileLifeTime;
    }

    public String getDesiredFileLifeTime() {
       return this.desiredFileLifeTime;
    }

    public void setStorageSystemInfo(String storageInfo) {
       this.storageSystemInfo = storageInfo;
    }

    public String getStorageSystemInfo() {
       return this.storageSystemInfo; 
    }

    public void setTargetStorageSystemInfo(String storageInfo) {
       this.targetStorageSystemInfo = storageInfo;
    }

    public String getTargetStorageSystemInfo() {
       return this.targetStorageSystemInfo; 
    }

    public void setTransferParametersProtocol(String protocols) {
       this.protocol = protocols;
    }

    public String getTransferParametersProtocol() {
       return protocol;
    }

    public void setTransferParametersAccessPattern(String accessPattern) {
       this.accessPattern = accessPattern;
    }

    public String getTransferParametersAccessPattern() {
       return this.accessPattern; 
    }

    public void setRetentionPolicy(String retentionPolicy) {
       this.retentionPolicy = retentionPolicy;
    }

    public String getRetentionPolicy() {
       return this.retentionPolicy;
    }

    public void setAccessLatency(String accessLatency) {
       this.accessLatency = accessLatency;
    }

    public String getAccessLatency() {
       return this.accessLatency; 
    }

    public void setTransferParametersConnectionType(String connectionType) {
       this.connectionType = connectionType;
    }

    public String getTransferParametersConnectionType() {
       return this.connectionType;
    }

    public String getMatchingInfo() {
       return this.matchingInfo;
    }

    public void setMatchingInfo(String matchingInfo) {
       this.matchingInfo = matchingInfo;
    }

    public String getActualResult() {
       return this.actualResult;
    }

    public void setActualResult(String actualResult) {
       this.actualResult = actualResult;
    }

    public void setRequestId(String requestId) {
       this.requestId = requestId;
    }

    public void setRemainingTotalRequestTime(String remainingTotalRequestTime) {
       this.remainingTotalRequestTime = remainingTotalRequestTime;
    }

    public String getRemainingTotalRequestTime() {
       return this.remainingTotalRequestTime;
    }

    public String getRequestId() {
       return this.requestId;
    }

    public void setStartTimeStamp(String stime) {
      startTimeStamp = stime;
    }

    public void setEndTimeStamp(String etime) {
      endTimeStamp = etime;
    }

    public String getStartTimeStamp () {
      return startTimeStamp;
    }

    public String getEndTimeStamp () {
      return endTimeStamp;
    }

    public void setFileSize(String size) {
      this.size = size;
    }

    public String getFileSize() {
      return size;
    }

    public void setOverwrite(String overwrite) {
      this.overwrite = overwrite;
    }

    public String getOverwrite() {
      return this.overwrite;
    }

    public void setRecursive(String recursive) {
      this.recursive = recursive;
    }

    public String getRecursive() {
      return this.recursive;
    }

    public void setSpaceToken(String spaceToken) {
       this.spaceToken = spaceToken;
    }

    public String getSpaceToken() {
       return this.spaceToken; 
    }

    public void setRequestState(String state) {
       this.state = state;
    }

    public String getRequestState() {
       return this.state; 
    }

    public void setExplanation(String explanation) {
       this.explanation = explanation;
    }

    public String getExplanation() {
       if(this.explanation == null) return "";
       return this.explanation; 
    }

    public void setReportRequestFileStatus(ReportRequestFileStatus[] fs) {
      this.fileStatuses = fs;
    }

    public ReportRequestFileStatus[] getReportRequestFileStatus() {
      return this.fileStatuses;
    }
}
