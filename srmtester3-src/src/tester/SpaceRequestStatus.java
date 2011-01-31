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

public class SpaceRequestStatus {

    private String spaceType="";
    private String state="";
    private String explanation="";
    private String spaceToken="";
    private String gSpace="";
    private String tSpace="";
    private String lifeTime="";
    private String tDesiredSpace="";
    private String lTimeAssigned="";
    private String lTimeLeft="";
    private String isValid="";
    private String uSpace="";

    public SpaceRequestStatus() {
    }

    public void setSpaceType(String spaceType) {
      this.spaceType = spaceType;
    }

    public String getSpaceType() {
       return this.spaceType;
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

    public void setSpaceToken(String spaceToken) {
      this.spaceToken = spaceToken;
    }

    public String getSpaceToken() {
       return this.spaceToken;
    }

    public void setSizeOfGuaranteedReservedSpace(String gSpace) {
      this.gSpace = gSpace;
    }

    public String getSizeOfGuaranteedReservedSpace() {
      return gSpace;
    }

    public void setSizeOfTotalReservedSpace(String tSpace) {
      this.tSpace = tSpace;
    }

    public String getSizeOfTotalReservedSpace() {
      return tSpace;
    }

    public void setSizeOfTotalReservedSpaceDesired(String tDesiredSpace) {
      this.tDesiredSpace = tDesiredSpace;
    }

    public String getSizeOfTotalReservedSpaceDesired() {
      return tDesiredSpace;
    }

    public void setSizeOfUnusedReservedSpace(String uSpace) {
      this.uSpace = uSpace;
    }

    public String getSizeOfUnusedReservedSpace() {
      return uSpace;
    }

    public void setLifetimeAssignedOfReservedSpace(String lSpace) {
      this.lTimeAssigned = lSpace;
    }

    public String getLifetimeAssignedOfReservedSpace() {
      return lTimeAssigned;
    }

    public void setLifetimeLeftOfReservedSpace(String lSpace) {
      this.lTimeLeft = lSpace;
    }

    public String getLifetimeLeftOfReservedSpace() {
      return lTimeLeft;
    }

    public void setIsValid(String valid) {
      this.isValid = valid;
    }

    public String getIsValid() {
      return this.isValid;
    }
}
