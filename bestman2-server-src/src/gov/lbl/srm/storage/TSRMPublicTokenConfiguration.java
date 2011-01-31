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

package gov.lbl.srm.storage;

import gov.lbl.srm.util.*;

import java.util.*;

public class TSRMPublicTokenConfiguration {
    public static long DEFAULT_MAX_SIZE_MB       = 300;
    public static int  DEFAULT_MAX_NUM_FILES     = 100;
    public static long DEFAULT_MAX_FILE_LIFETIME_SECS = 100;

    public static String DefMAX_MB_PER_USER = "PublicTokenMaxMBPerUser";
    public static String DefMAX_NUM_FILES   = "PublicTokenMaxNumFilesPerUser";
    public static String DefMAX_FILE_LIFETIME_SECONDS = "PublicTokenMaxFileLifetimeInSeconds";

    long _max_bytes_per_user = DEFAULT_MAX_SIZE_MB*(long)1048576;
    int  _max_files_per_user = DEFAULT_MAX_NUM_FILES;
    long _max_lifetime_per_file_millis = DEFAULT_MAX_FILE_LIFETIME_SECS*1000;

    public TSRMPublicTokenConfiguration(Properties prop, long publicTokenBytes) 
    {
	String maxMB = prop.getProperty(TSRMPublicTokenConfiguration.DefMAX_MB_PER_USER);
	if (maxMB != null) {
	    _max_bytes_per_user = Long.parseLong(maxMB)*(long)1048576;
	    TSRMUtil.noNegative((_max_bytes_per_user < 0), TSRMPublicTokenConfiguration.DefMAX_MB_PER_USER);
	}

	if (_max_bytes_per_user > publicTokenBytes) {
	    _max_bytes_per_user = publicTokenBytes/2;
	}

	String maxNumFiles = prop.getProperty(TSRMPublicTokenConfiguration.DefMAX_NUM_FILES);
	if (maxNumFiles != null) {
	    _max_files_per_user = Integer.parseInt(maxNumFiles);
	    TSRMUtil.noNegative((_max_files_per_user < 0), TSRMPublicTokenConfiguration.DefMAX_NUM_FILES);
	}

	String maxLifetimeSecs = prop.getProperty(TSRMPublicTokenConfiguration.DefMAX_FILE_LIFETIME_SECONDS);
	if (maxLifetimeSecs != null) {
	    _max_lifetime_per_file_millis = Long.parseLong(maxLifetimeSecs)*1000;
	    TSRMUtil.noNegative((_max_lifetime_per_file_millis < 0), TSRMPublicTokenConfiguration.DefMAX_FILE_LIFETIME_SECONDS);
	}

	printOut();
    }

    void printOut() {
	TSRMUtil.startUpInfo("==== public token restriction ==> ");

	TSRMUtil.startUpInfo("   max file lifetime (seconds)= "+_max_lifetime_per_file_millis/1000);
	TSRMUtil.startUpInfo("   max numer files per user = "+  _max_files_per_user);
	TSRMUtil.startUpInfo("   max size per user (MB) = "+_max_bytes_per_user/1048576);

	TSRMUtil.startUpInfo("<== ");
    }

    public long getMaxFileLifeTimeMillis() {	
	return System.currentTimeMillis()+_max_lifetime_per_file_millis;
    }
}
