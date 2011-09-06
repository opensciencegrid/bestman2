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

public class SRM_ACCESS_TYPE 
{
   private static SRM_ACCESS_TYPE [] values_ = new SRM_ACCESS_TYPE[10];
   private int value_;

   public final static int  _SRM_ACCESS_UNKNOWN = 0;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_UNKNOWN = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_UNKNOWN);

   public final static int  _SRM_ACCESS_PLAIN = 1;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_PLAIN = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_PLAIN);

   public final static int  _SRM_ACCESS_GSI = 2;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_GSI = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_GSI);

   public final static int  _SRM_ACCESS_ENCRYPT = 3;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_ENCRYPT = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_ENCRYPT);

   public final static int  _SRM_ACCESS_KERBEROS = 4;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_KERBEROS = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_KERBEROS);

   public final static int  _SRM_ACCESS_SSH = 5;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_SSH = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_SSH);

   public final static int  _SRM_ACCESS_SCP = 6;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_SCP = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_SCP);

   public final static int  _SRM_ACCESS_NCARMSS = 7;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_NCARMSS = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_NCARMSS);

   public final static int  _SRM_ACCESS_LSTOREMSS = 8;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_LSTOREMSS = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_LSTOREMSS);

   public final static int  _SRM_ACCESS_NONE = 9;
   public final static SRM_ACCESS_TYPE  SRM_ACCESS_NONE = 
	new SRM_ACCESS_TYPE(_SRM_ACCESS_NONE);


   protected SRM_ACCESS_TYPE (int value)
   {
      values_[value] = this;
      value_ = value;
   }

   public int value() {
     return value_;
   }

   public static SRM_ACCESS_TYPE from_int(int value) 
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
          case 0 : result= "SRM_ACCESS_UNKNOWN"; break; 
          case 1 : result= "SRM_ACCESS_PLAIN"; break; 
          case 2 : result= "SRM_ACCESS_GSI"; break; 
          case 3 : result= "SRM_ACCESS_ENCRYPT"; break; 
          case 4 : result= "SRM_ACCESS_KERBEROS"; break; 
          case 5 : result= "SRM_ACCESS_SSH"; break; 
          case 6 : result= "SRM_ACCESS_SCP"; break; 
          case 7 : result= "SRM_ACCESS_NCARMSS"; break; 
          case 8 : result= "SRM_ACCESS_LSTOREMSS"; break; 
          case 9 : result= "SRM_ACCESS_NONE"; break; 
      }
      return result;
   }
}
