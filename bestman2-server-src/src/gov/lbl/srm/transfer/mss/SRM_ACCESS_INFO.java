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

import java.util.HashMap;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_ACCESS_INFO
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRM_ACCESS_INFO  {

 private String uid="";
 private String login="";
 private String passwd=""; //GSI and other security info

 public static String _DefKeyUID = "uid"; // defined by junmin 2007-11-30
 public static String _DefKeyPWD = "pwd"; // defined by junmin 2007-11-30    
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_ACCESS_INFO
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_ACCESS_INFO () {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRM_ACCESS_INFO
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_ACCESS_INFO (String userId, String login, String passwd) {
   this.uid = userId;
   this.login = login;
   this.passwd = passwd;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setUserId
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setUserId (String userId) {
  this.uid = userId;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLogin
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLogin (String login) {
  this.login = login;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setPasswd
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setPasswd (String passwd) {
  this.passwd = passwd;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getUserId
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getUserId () {
  return this.uid;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLogin
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getLogin () {
  return this.login;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getPasswd
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getPasswd () {
  return this.passwd;
}

    // added by junmin 11/30/2007
    public static SRM_ACCESS_INFO generateDefault(HashMap input) throws Exception  {
	String uid = (String)(input.get(_DefKeyUID));
	String pwd = (String)(input.get(_DefKeyPWD));

	if (uid == null) {
	    throw new Exception("No login is found. did you use key:"+_DefKeyUID);
	}
	if (pwd == null) {
	    throw new Exception("No pwd is found. did you use key:"+_DefKeyPWD);
	}
	
	return new SRM_ACCESS_INFO(uid, uid, pwd);
    }
} 
