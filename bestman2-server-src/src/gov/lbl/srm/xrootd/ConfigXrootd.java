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

package gov.lbl.srm.server;

import gov.lbl.srm.util.*;
//import gov.lbl.srm.storage.TSRMStorage;
//import gov.lbl.srm.policy.*;
import gov.lbl.srm.impl.*;

import java.io.*;
//import java.io.StringWriter;
import java.net.URL;
import java.util.*;

import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.globus.security.gridmap.GridMap;

public class ConfigXrootd extends Config{
    public static final String _DefConfigCheckXrootdFS = "checkSizeWithFS";
    public static final String _DefConfigCheckSizeGsiftp   = "checkSizeWithGsiftp";
    public static final String _DefConfigEnableRecursionInLs = "allowRecursionInLs";
    //public static final String _DefConfigCheckFSGsiftp   = "checkFileSysWithGsiftp";
    public static final String _DefConfigCheckXrootdToken = "pathForToken";
    public static final String _DefConfigXrootdTokenCompName ="xrootdTokenCompName";
    //    public static final String _DefConfigUseSudo = "useSudo";
    public static final String _DefConfigGatewayFSAC="fsConcurrency";

    public static int _fsConcurrency=0;
    public static String _xrootdTokenCompName = null;

    public static boolean _enableRecursionInLs = true;
    public static boolean _doCheckSizeWithXrootdFS = true;
    public static boolean _doCheckSizeWithGsiftp   = false;
    public static boolean _doCheckToken = false;
    //public static boolean _doAccessFileSysViaGsiftp = false;
    //public static boolean _doAccessFileSysViaSudo = false;

  //public static StaticToken[] _staticTokenList = null;
   

    public static boolean load(String configFileName) throws java.io.IOException {
	TSRMUtil.startUpInfoSilent("## reading configuration file: "+configFileName);
	Properties prop = new Properties();
	java.io.FileInputStream configFile = null;
	try {
	    configFile = new java.io.FileInputStream(configFileName);
	    prop.load(configFile);
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("## error with reading config file, abort!");
	    System.exit(1);
	} finally {
	    if (configFile != null) {
		configFile.close();
	    }
	}

	if (prop.getProperty(ConfigXrootd._DefConfigXrootdTokenCompName) != null) {
	    _xrootdTokenCompName = prop.getProperty(ConfigXrootd._DefConfigXrootdTokenCompName);
	    prop.put(ConfigXrootd._DefConfigXrootdTokenCompName, _xrootdTokenCompName);
	} else {
	    //TSRMUtil.startUpInfo(".........assuming default xrootd token component name:"+_xrootdTokenCompName);
	}

	String enableRecursionInLs = prop.getProperty(ConfigXrootd._DefConfigEnableRecursionInLs);
	if ((enableRecursionInLs != null) && (enableRecursionInLs.equalsIgnoreCase("false"))) {
	    _enableRecursionInLs = false;
	}
	TSRMUtil.startUpInfoSilent("recursive srmLs() allowd?"+_enableRecursionInLs); 

	String checkXrootdFS = prop.getProperty(ConfigXrootd._DefConfigCheckXrootdFS);
	if ((checkXrootdFS != null) && (checkXrootdFS.equalsIgnoreCase("false"))) {
	    _doCheckSizeWithXrootdFS = false;
	}
	
	String checkGsiftp = prop.getProperty(ConfigXrootd._DefConfigCheckSizeGsiftp);
	if ((checkGsiftp != null) && (checkGsiftp.equalsIgnoreCase("true"))) {
	    _doCheckSizeWithGsiftp = true;
	}

	if (!_doCheckSizeWithXrootdFS && !_doCheckSizeWithGsiftp) {
	    TSRMUtil.startUpInfo("Configuration is invalid. Either verify file size through FS or Gsiftp or both");
	    System.exit(1);
	}
	TSRMUtil.startUpInfoSilent("Checking size through FS?"+_doCheckSizeWithXrootdFS+", gsiftp?"+_doCheckSizeWithGsiftp);

	//TSRMUtil.startUpInfo("Accessing file system via gsiftp?"+Config._accessFileSysUsingGSIFTP);	       
	//TSRMUtil.startUpInfo("Accessing file system via Sudo?"+Config._accessFileSysUsingSUDO);

	String checkXrootdToken = prop.getProperty(ConfigXrootd._DefConfigCheckXrootdToken);
	if ((checkXrootdToken != null) && (checkXrootdToken.equalsIgnoreCase("true"))) {
	    _doCheckToken = true;
	}
	TSRMUtil.startUpInfoSilent("Checking space token with FS?"+_doCheckToken);

	String fsc = prop.getProperty(ConfigXrootd._DefConfigGatewayFSAC);	
	if (fsc != null) {
	    try {
		_fsConcurrency = Integer.parseInt(fsc);
	    } catch (Exception e) {
		e.printStackTrace();
		TSRMUtil.startUpInfo("Error: failed to read integers from: "+ConfigXrootd._DefConfigGatewayFSAC+" value="+fsc);
		System.exit(1);
	    }
	}
	return true;
    }
}


