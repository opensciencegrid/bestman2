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

import java.util.Vector;

public class DataPathStatus {

    private String state="";
    private String explanation="";
    private String origSURL="";
    private String path = "";
    private String fileType="";
    private String storageType="";
    private String size="";
    private String lastAccessed="";
    private String lifeTimeLeft="";
    private String ownerPermission="";
    private Vector subPaths = new Vector();

    public DataPathStatus() {
    }

    public void setStatus(String state) {
       this.state = state;
    }

    public String getStatus() {
       return this.state; 
    }

    public void setLifetimeLeft(String lifeTimeLeft) {
      this.lifeTimeLeft = lifeTimeLeft;
    }

    public String getLifetimeLeft() {
      return this.lifeTimeLeft;
    }

    public void setOwnerPermission(String ownerPermission) {
      this.ownerPermission = ownerPermission;
    }

    public String getOwnerPermission() {
      return this.ownerPermission;
    }

    public void setExplanation(String explanation) {
       this.explanation = explanation;
    }

    public String getExplanation() {
       return this.explanation; 
    }

    public void setSurl(String surl) {
       origSURL = surl;
    }

    public String getSurl() {
       return origSURL;
    }

    public void setPath(String path) {
      this.path = path;
    }

    public String getPath() {
      return this.path; 
    }

    public void setStorageType(String storageType) {
      this.storageType = storageType;
    }

    public String getStorageType() {
      return storageType;
    }

    public void setFileType(String fileType) {
      this.fileType = fileType;
    }

    public String getFileType() {
      return fileType;
    }

    public void setSize(String size) {
      this.size = size;
    }

    public String getSize() {
      return size;
    }

    public void setLastAccessed(String lastAccessed) {
      this.lastAccessed = lastAccessed;
    }

    public String getLastAccessed() {
      return this.lastAccessed;
    }

    public void addSubPath(DataPathStatus dps) {
      subPaths.addElement(dps); 
    }

    public void addSubPath(Vector vec) {
      subPaths = vec;
    }

    public Vector getSubPath() {
      return subPaths;
    }
}
