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

package gov.lbl.srm.transfer.mss;

public class SRM_OVERWRITE_MODE
{
   private static SRM_OVERWRITE_MODE [] values_ = new SRM_OVERWRITE_MODE [3];
   private int value_;

   public final static int  _SRM_MSS_OVERWRITE_YES = 0;
   public final static SRM_OVERWRITE_MODE SRM_MSS_OVERWRITE_YES = 
	new SRM_OVERWRITE_MODE(_SRM_MSS_OVERWRITE_YES);

   public final static int  _SRM_MSS_OVERWRITE_NO = 1;
   public final static SRM_OVERWRITE_MODE SRM_MSS_OVERWRITE_NO = 
	new SRM_OVERWRITE_MODE(_SRM_MSS_OVERWRITE_NO);

   public final static int  _SRM_MSS_OVERWRITE_SIZEDIFF = 2;
   public final static SRM_OVERWRITE_MODE SRM_MSS_OVERWRITE_SIZEDIFF = 
	new SRM_OVERWRITE_MODE(_SRM_MSS_OVERWRITE_SIZEDIFF);

   protected SRM_OVERWRITE_MODE(int value) 
  { 
      values_[value] = this;
      value_ = value;
  }

   public int value() {
     return value_;
   }

   public static SRM_OVERWRITE_MODE from_int(int value) 
        throws Exception {
        if(value < values_.length) {
           return values_[value];
        }
        else {  
           throw new Exception("BAD PARAM out of range exception");
        }
   }

    public String toString () {
      String result = "";
      switch(value_) {
          case 0 : result= "SRM_MSS_OVERWRITE_YES"; break;
          case 1 : result= "SRM_MSS_OVERWRITE_NO"; break;
          case 2 : result= "SRM_MSS_OVERWRITE_SIZEDIFF"; break;
      }
      return result;
   }


}
