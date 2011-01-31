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

package gov.lbl.srm.policy;

import gov.lbl.srm.storage.TBasicDevice;
import gov.lbl.srm.util.*;

import java.io.IOException;
import java.util.Properties;
/**
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class TSRMGeneralEnforcement {
  //public static  int                  MAX_MSS_LOOKUP_QUEUE = 5;

    public static  int 			MAX_MSS_CONNECTION = 5;
    public static  int 		MAX_ACTIVE_ACCOUNTS = 10;
    public static  long 		MAX_FILE_REQUESTS = 10000; // per SRM. but does it include finished ones?
    public static  int 			MAX_CONCURRENT_TRANSFER_ALLOWED = 5; // ftps, including MSS
    public static  int  		THREAD_POOL_SIZE  = 3; // to make sense, this should be larger than MAX_CONCURRENT_TRANSFER_ALLOWED
    
    public static final String _DefConfigMaxActiveAccount = "MaxNumberOfUsers";
    public static final String _DefConfigVolatileFileLifeTime     = "DefaultVolatileFileLifeTimeInSeconds";
    public static final String _DefConfigMaxFileRequests  = "MaxNumberOfFileRequests";
    public static final String _DefConfigMaxConcurrentTxf = "MaxConcurrentFileTransfer";
    public static final String _DefConfigThreadPoolSize   = "Concurrency";
    public static final String _DefConfigInactiveTxfTimeOut   = "InactiveTxfTimeOutInSeconds";
    public static final String _DefConfigMaxMSSConnection = "MaxMSSConnections";
    
    public static int  _DEFAULT_FILE_LIFETIME_SECS = 1200; // for volatile files
    public final static long _MAX_FILE_LIFETIME_ALLOWED = 24*3600; // one day
    public static long _INACTIVE_TXF_TIME_IN_MILLISECONDS = 900000; // 15 minutes 
    
    public static long _ACTIVE_FILE_REQUEST_COUNTER = 0;

    public static void add() {
	_ACTIVE_FILE_REQUEST_COUNTER ++;
	if (_ACTIVE_FILE_REQUEST_COUNTER > MAX_FILE_REQUESTS) {
	    throw new TSRMException("SRM is too busy.", false);
	}
    }

    public static void minus() {
	_ACTIVE_FILE_REQUEST_COUNTER --;
    }

    public static void setMaxActiveAccounts(int n) {
	MAX_ACTIVE_ACCOUNTS = n;
    }
    
    public static void setDefaultFileLifeTimeSecs(int n) {
	_DEFAULT_FILE_LIFETIME_SECS = n;
    }

    public static void setMaxFileRequests(long n) {
	MAX_FILE_REQUESTS = n;
    }
    
    public static void setThreadPoolSize(int n) {
	THREAD_POOL_SIZE = n;
    }
    
    public static void setMaxConcurrentTransferAllowed(int n) {
	MAX_CONCURRENT_TRANSFER_ALLOWED = n;	
    }
    
    public static void setMaxMSSConnectionAllowed(int n) {
	MAX_MSS_CONNECTION = n;
    }

    /*
    public static void setMaxMSSLookupQueue(int n) {
	MAX_MSS_LOOKUP_QUEUE = n;
    }
    */

    public static void setInactiveTxfTimeOut(long n) {
	_INACTIVE_TXF_TIME_IN_MILLISECONDS = n*1000;
    }
    
    public static void loadFromFile(String configFileName) throws IOException {	
	Properties prop = new Properties();
	java.io.FileInputStream configFile = null;
	try {
	    configFile = new java.io.FileInputStream(configFileName);
	    prop.load(configFile);
	} catch (java.io.IOException e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfoSilent("## error with reading this config file: ["+configFileName+"], abort.");
	} finally {
	    if (configFile != null) {
		configFile.close();
	    }
	}
	
	
	String entryValue = loadEntry(prop, 
				      TSRMGeneralEnforcement._DefConfigMaxActiveAccount, true);				     
	if (entryValue != null) {
	    //TSRMGeneralEnforcement.MAX_ACTIVE_ACCOUNTS = Integer.parseInt(entryValue);
	    setMaxActiveAccounts(Integer.parseInt(entryValue));
	    TSRMUtil.noNegative((MAX_ACTIVE_ACCOUNTS < 0), TSRMGeneralEnforcement._DefConfigMaxActiveAccount);
	}
	TSRMUtil.startUpInfoSilent("########## Max active accounts = "+ TSRMGeneralEnforcement.MAX_ACTIVE_ACCOUNTS);
	

	entryValue = loadEntry(prop, TSRMGeneralEnforcement._DefConfigVolatileFileLifeTime, true);   			       
	if (entryValue != null) {
	    //TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS = Long.parseLong(entryValue);		    
	    setDefaultFileLifeTimeSecs(Integer.parseInt(entryValue));
	    TSRMUtil.noNegative((_DEFAULT_FILE_LIFETIME_SECS < 0), TSRMGeneralEnforcement._DefConfigVolatileFileLifeTime);
	}
	TSRMUtil.startUpInfoSilent("########## Default volatile File LifeTime = "+ TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS +" seconds");
	
	entryValue = loadEntry(prop,TSRMGeneralEnforcement._DefConfigThreadPoolSize, true);			       
	if (entryValue != null) {
	    //TSRMGeneralEnforcement.THREAD_POOL_SIZE = Integer.parseInt(entryValue);
	    setThreadPoolSize(Integer.parseInt(entryValue));
	    TSRMUtil.noNegative((THREAD_POOL_SIZE  < 0), TSRMGeneralEnforcement._DefConfigThreadPoolSize);
	}
	TSRMUtil.startUpInfoSilent("########## thread pool size = "+TSRMGeneralEnforcement.THREAD_POOL_SIZE);

	entryValue = loadEntry(prop, TSRMGeneralEnforcement._DefConfigMaxFileRequests, true);
	if (entryValue != null) {
	    setMaxFileRequests(Long.parseLong(entryValue));
	    TSRMUtil.noNegative(( MAX_FILE_REQUESTS < 0), TSRMGeneralEnforcement._DefConfigMaxFileRequests);
	} 
	TSRMUtil.startUpInfoSilent("########## max file requests allowed = "+TSRMGeneralEnforcement.MAX_FILE_REQUESTS);

	       
	entryValue = loadEntry(prop, TSRMGeneralEnforcement._DefConfigMaxConcurrentTxf, true);
	if (entryValue != null) {
	    setMaxConcurrentTransferAllowed(Integer.parseInt(entryValue));
	    TSRMUtil.noNegative((MAX_CONCURRENT_TRANSFER_ALLOWED < 0), TSRMGeneralEnforcement._DefConfigMaxConcurrentTxf);
	} 
	
	TSRMUtil.startUpInfoSilent("########## max concurrent transfer limit="+TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED);
	
	entryValue = loadEntry(prop, TSRMGeneralEnforcement._DefConfigInactiveTxfTimeOut, true);
	if (entryValue != null) {
	    setInactiveTxfTimeOut(Long.parseLong(entryValue));
	    TSRMUtil.noNegative((_INACTIVE_TXF_TIME_IN_MILLISECONDS  < 0), TSRMGeneralEnforcement._DefConfigInactiveTxfTimeOut);
	} 
	TSRMUtil.startUpInfoSilent("########## inactive transfer time out = "+TSRMGeneralEnforcement._INACTIVE_TXF_TIME_IN_MILLISECONDS/1000 + " seconds");

	// advisory attributes:
	if (!TBasicDevice._mssDeviceCreated) {
	    TSRMUtil.startUpInfoSilent("............NO MSS");
	    setMaxMSSConnectionAllowed(0);
	} else {
	    entryValue = loadEntry(prop, TSRMGeneralEnforcement._DefConfigMaxMSSConnection, true);
	    if (entryValue != null) {
		setMaxMSSConnectionAllowed(Integer.parseInt(entryValue));
		TSRMUtil.noNegative((MAX_MSS_CONNECTION < 0), TSRMGeneralEnforcement._DefConfigMaxMSSConnection);
	    } 
	}
	TSRMUtil.startUpInfoSilent("########## desired mss connection limit="+TSRMGeneralEnforcement.MAX_MSS_CONNECTION);

    }
    
    public static String loadEntry(Properties prop, String entryName, boolean hasDefaultValue) throws IOException {
	String entryValue = prop.getProperty(entryName);
	if (entryValue == null) {
	    if (hasDefaultValue == true) {
		TSRMUtil.startUpInfoSilent("## "+entryName+":using default value");
		return null;
	    } else {
		TSRMUtil.startUpInfoSilent("## error: need to define entry:"+entryName);
		throw new IOException ("Error! No value for entry:["+entryName+"] ");
	    }
	} else {
	    TSRMUtil.startUpInfoSilent( "######## => input: "+entryName+" => ["+entryValue+"]");
	    return entryValue;
	}
    }
}
