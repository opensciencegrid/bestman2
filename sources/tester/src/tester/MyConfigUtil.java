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

import java.io.IOException;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.security.auth.*;
import javax.security.auth.login.*;
import java.util.Properties;

public class MyConfigUtil {

    public static final int UNDEFINED_OS = -1;
    public static final int WINDOWS_OS   = 0;
    public static final int UNIX_OS      = 1;
    public static final int MAC_OS       = 2;
    public static final int OTHER_OS     = 3;
  
    private static int osType = UNDEFINED_OS;

    private static final String PROXY_NAME = "x509up_u";

    public static String globus_dir = null;
    
    static {
	globus_dir = System.getProperty("user.home") + 
	    File.separator + 
	    ".globus" +
	    File.separator;
    }
    
    /** Returns default PKCS11 library name */
    public static String discoverPKCS11LibName() {
	return "dspkcs"; // use the ibutton library as the default for now
    }
    
    /** Returns default location of user cert file */
    public static String discoverUserCertLocation() {
	String location = null;
	location = globus_dir + "usercert.pem";
	return location;
    }
    
    /** Returns default location of user key file */
    public static String discoverUserKeyLocation() {
	String location = null;
	location = globus_dir + "userkey.pem";
	return location;
    }
    
    /**
     * Tries to discover user proxy location.
     * If a UID system property is set, and running on a Unix machine it
     * returns /tmp/x509up_u${UID}. If any other machine then Unix, it returns
     * ${tempdir}/x509up_u${UID}, where tempdir is a platform-specific
     * temporary directory as indicated by the java.io.tmpdir system property.
     * If a UID system property is not set, the username will be used instead
     * of the UID. That is, it returns ${tempdir}/x509up_u_${username}
     */
    public static String discoverProxyLocation() {

	String dir = null;

	if (getOS() == UNIX_OS) {
	    dir = "/tmp/";
	} else {
	    String tmpDir = System.getProperty("java.io.tmpdir");
	    dir = (tmpDir == null) ? globus_dir : tmpDir;
	}
	
	String uid = System.getProperty("UID");

	if (uid != null) {
	    return getLocation(dir, PROXY_NAME + uid);
	} else if (getOS() == UNIX_OS) {
	    try {
		return getLocation(dir, PROXY_NAME + getUID());
	    } catch (IOException e) {
	    }
	}
	
	/* If all else fails use username */
	String suffix = System.getProperty("user.name");
	if (suffix != null) {
	    suffix = suffix.toLowerCase();
	} else {
	    suffix = "nousername";
	}

	return getLocation(dir, PROXY_NAME + "_" + suffix);
    }

    private static String getLocation(String dir, String file) {
	File f = new File(dir, file);
	return f.getAbsolutePath();
    }

    public static String getUID() throws IOException {
	Runtime runTime = Runtime.getRuntime();
	Process process = null;
	BufferedReader buffInReader;
	String s = null;
	StringBuffer output = new StringBuffer();
	int exitValue = -1;
	try {
	    process = runTime.exec("id -a");
	    buffInReader = new BufferedReader
		( new InputStreamReader(process.getInputStream()) ); 
	    while ((s = buffInReader.readLine()) != null) {
        int idx = s.trim().indexOf("uid=");
        if(idx != -1) {
           int idx1 = s.trim().indexOf("(");
           if(idx1 != -1) {
             output.append(s.substring(idx+4,idx1));
           }
           else {
             output.append(s.substring(idx+4));
           }
        }
        else 
		  output.append(s);
	    }
	    buffInReader.close();
	    exitValue = process.waitFor();
	} catch (Exception e) {
        System.out.println("Exception " + e.getMessage());
	    throw new IOException("Unable to execute 'id -a'");
	} 
	if (exitValue != 0 && output.toString().equals("")) {
        System.out.println("\nexitValue returned by the process" + exitValue);
	    throw new IOException("Unable to perform 'id -a'");
	}
	return output.toString().trim();
    } 


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getUID2
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
 
public static String getUID2() throws Exception {
  String result = "";
  try {
    Properties props = System.getProperties();
    String osname = props.getProperty("os.name");
    System.out.println("osname="+osname);
    LoginContext loginContext = new LoginContext("Login");
    loginContext.login();
    String str = loginContext.getSubject().toString();
    if(osname.startsWith("Windows")) {
      int idx = str.indexOf("NTSidUserPrincipal");
      if(idx != -1) {
        String testStr = str.substring(idx+18);
        int idx1 = testStr.indexOf("Principal");
        if(idx1 != -1) {
          result = testStr.substring(0,idx1).trim();
        }
      }
      return result;
    }
     if(osname.equalsIgnoreCase("linux") ||
       osname.equalsIgnoreCase("sunos") || osname.startsWith("Mac")) {
      int idx = str.indexOf("UnixNumericUserPrincipal");
      if(idx != -1) {
        String testStr = str.substring(idx+25);
        int idx1 = testStr.indexOf("Principal");
        if(idx1 != -1) {
          result = testStr.substring(0,idx1).trim();
        }
      }
      return result;
    }
   }catch(Exception e) {
      throw e;
   }
   return result;
}



    /**
     * Discovers location of CA certificates directory.
     * First the ${user.home}/.globus/certificates directory is checked.
     * If the directory does not exist, and on a Unix machine, the
     * /etc/grid-security/certificates directory is checked next.
     * If that directory does not exist and GLOBUS_LOCATION 
     * system property is set then the ${GLOBUS_LOCATION}/share/certificates
     * directory is checked. Otherwise, null is returned. 
     * This indicates that the certificates directory could
     * not be found.
     */
    public static String discoverCertDirLocation() {
	String location = null;
    
	location = getDir(globus_dir + "certificates");
	if (location != null) return location;
	
	if (getOS() == UNIX_OS) {
	    location = getDir( "/etc/grid-security/certificates");
	    if (location != null) return location;
	    
	    String suffix = File.separator + "share" + 
		File.separator + "certificates";
	    
	    location = getDir(System.getProperty("GLOBUS_LOCATION") +
			      suffix);
	    if (location != null) return location;
	}
	
	return null;
    }

  
    public static int getOS() {
	if (osType != UNDEFINED_OS) {
	    return osType;
	}

	String osname = System.getProperty("os.name");
	if (osname != null) {
	    osname = osname.toLowerCase();
	    if (osname.indexOf("windows") != -1) {
		osType = WINDOWS_OS;
	    } else if ( (osname.indexOf("solaris") != -1) ||
			(osname.indexOf("sunos") != -1) ||
			(osname.indexOf("linux") != -1) ||
			(osname.indexOf("aix") != -1) ||
 			(osname.indexOf("hp-ux") != -1) ||
 			(osname.indexOf("compaq's digital unix") != -1) ||
 			(osname.indexOf("osf1") != -1) ||
			(osname.indexOf("mac os x") != -1) ||
			(osname.indexOf("irix") != -1) ) {
		osType = UNIX_OS;
	    } else if (osname.indexOf("mac") != -1) {
		osType = MAC_OS;
	    } else {
		osType = OTHER_OS;
	    }
	} else {
	    osType = OTHER_OS;
	}
    
	return osType;
    }

    private static String getDir(String directory) {
	if (directory == null) return null;
	File f = new File(directory);
	if (f.isDirectory() && f.canRead()) {
	    return f.getAbsolutePath();
	} else {
	    return null;
	}
    }
    
}
