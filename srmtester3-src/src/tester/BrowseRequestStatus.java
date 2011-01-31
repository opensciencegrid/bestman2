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

public class BrowseRequestStatus {

    private String uid ="";
    private String state="";
    private String explanation="";
    private String browseUrl = "";
    private String requestId = "";
    private String fullDetailedList="";
    private String numOfLevels="";
    private String allLevelRecursive="";
    private String fileStorageType="";
    private String storageSystemInfo="";
    private DataPathStatus dps;
    private String totalNumPathDetail="";
    private String srmServicePath="";

    public BrowseRequestStatus() {
    }

    public void setUserId(String uid) {
       this.uid = uid;
    }

    public void setFullDetailedList(String fullDetailedList) {
       this.fullDetailedList = fullDetailedList;
    }

    public String getFullDetailedList() {
       return fullDetailedList; 
    }

    public void setFileStorageType(String fileStorageType) {
       this.fileStorageType = fileStorageType;
    }

    public String getFileStorageType() {
       return fileStorageType; 
    }

    public void setStorageSystemInfo(String storageSystemInfo) {
       this.storageSystemInfo = storageSystemInfo;
    }

    public String getStorageSystemInfo() {
       return storageSystemInfo;
    }

    public void setTotalNumPathDetail(String totalLength) {
       totalNumPathDetail = totalLength;
    }

    public String getTotalNumPathDetail() { 
       return totalNumPathDetail;
    }

    public void setAllLevelRecursive(String allLevelRecursive) {
       this.allLevelRecursive = allLevelRecursive;
    }

    public String getAllLevelRecursive() {
       return allLevelRecursive; 
    }

    public void setNumOfLevels(String numOfLevels) {
       this.numOfLevels = numOfLevels;
    }

    public String getNumOfLevels() {
       return numOfLevels; 
    }

    public void setRequestId(String rid) {
       this.requestId = rid;
    }

    public String getRequestId() {
       return this.requestId; 
    }

    public String getUserId() {
       return uid; 
    }

    public void setBrowseUrl(String surl) {
       browseUrl = surl;
    }

    public String getBrowseUrl() {
      return browseUrl;
    }

    public void setStatus(String state) {
       this.state = state;
    }

    public String getStatus() {
       return this.state; 
    }

    public void setExplanation(String explanation) {
       this.explanation = explanation;
    }

    public String getExplanation() {
       return this.explanation; 
    }

    public void setDataPathStatus(DataPathStatus dps) {
       this.dps = dps;
    }

    public DataPathStatus getDataPathStatus() {
       return dps;
    }

    public String getSrmServicePath() {
      return srmServicePath;
    }

    public void setSrmServicePath(String srmServicePath) {
      this.srmServicePath = srmServicePath;
    }


}
