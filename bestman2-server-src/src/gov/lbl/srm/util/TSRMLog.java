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

package gov.lbl.srm.util;

import gov.lbl.srm.server.Config;

public class TSRMLog {
    private static iSRMLog _eventLog = null;
    private static ISRMLogCache _cacheLog = null;

    protected static long _gMAX_BYTES_PER_FILE = 50000000;
    protected static boolean _isConsoleEnabled = true;

    public interface iSRMLog {	
	void error(Class objClass, String methodName, String why, String extra);
	void exception(Class objClass, String methodName, Throwable e);
	void info(Class objClass, String methodName, String why, String extra);
	void debug(Class objClass, String methodName, String why, String extra);
	void console(Class objClass, String str);
	void warning(Class objClass, String methodName, String why, String extra);
	
	//long getSize();
    }

    public static void setEventLog(String name) {
	_eventLog = new TSRMEventLog(name);	

        java.io.File path = TSRMUtil.initFile(name);     
	java.io.File shadow = new java.io.File(path.getParentFile(), ".bestmanRotationTest");
	try {
	  new java.io.FileWriter(shadow);
	} catch (Exception e) {
	  //e.printStackTrace();
	  System.out.println("Error! BeStMan has no write permission to event log dir:"+path.getParentFile().toString()+". Exiting.");
	  System.exit(0);
	} finally {
	  shadow.delete();
	}

	String byteStr = System.getProperty("SRMEventLogByteMax");
	if (byteStr != null) {
	  try {
	    long bytemax = Long.parseLong(byteStr);
	    _gMAX_BYTES_PER_FILE = bytemax;
	  } catch (Exception e) {
	    e.printStackTrace();
	  }
	}
	TSRMUtil.startUpInfo("==> assigned event log to be: "+name+" max bytes to rotate:"+_gMAX_BYTES_PER_FILE);
    }

    public static void setCacheLog(String name) throws java.io.IOException {
	if (Config._useBerkeleyDB) {
	    _cacheLog = new TSRMCacheLogSleepyCat(name);
	    System.out.println("==> assigned berkeley db log to be at: "+name);
	} else {
	    //TSRMCacheLog.readLogStarts(name);
	    _cacheLog = new TSRMCacheLog(name);
	    System.out.println("==> assigned cache log to be at: "+name);
	}
    }

    public static ISRMLogCache getCacheLog() {
	return _cacheLog;
    }

    public static void setEnableConsole(boolean b) {
	_isConsoleEnabled = b;
    }

    synchronized
    public static void setMaxFileSize(long s) {
	_gMAX_BYTES_PER_FILE = s;
    }

    synchronized
    public static void error(Class objClass, String methodName, String why, String extra) {
	if (_eventLog == null) {
	    return;
	}
	_eventLog.error(objClass, methodName, why, extra);
    }	

    synchronized
    public static void exception(Class objClass, String methodName, Throwable e) {
	if (_eventLog == null) {
	    return;
	}
	_eventLog.exception(objClass, methodName, e);
    }	

    synchronized
    public static void info(Class objClass, String methodName, String why, String extra) {
	if (_eventLog == null) {
	    return;
	}

	_eventLog.info(objClass, methodName, why, extra);
    }	

    synchronized 
    public static void console(Class objClass, String msg) {
	if (_eventLog == null) {
	    return;
	}
	_eventLog.console(objClass, msg);
    }

    synchronized
    public static void debug(Class objClass, String methodName, String why, String extra) {
	if (_eventLog == null) {
	    return;
	}

	_eventLog.debug(objClass, methodName, why, extra);
    }	

    synchronized
    public static void warning(Class objClass, String methodName, String why, String extra) {
	if (_eventLog == null) {
	    return;
	}
	_eventLog.warning(objClass, methodName, why, extra);
    }	

    public static void toConsole(String msg) {	
	outputToConsole(null, msg);
    }

    public static void toConsoleAlert(String msg) {	
	outputToConsole("!Warning:", msg);
    }

    public static void toConsoleError(String msg) {	
	outputToConsole("!!Error:", msg);
    }

    private static void outputToConsole(String prefix, String msg) {
	if (!_isConsoleEnabled) {
	    return;
	}

	if (prefix == null) {
	    TSRMUtil.startUpInfo(msg);
	} else {
	    TSRMUtil.startUpInfo(prefix+msg);
	}
    }
    
}
