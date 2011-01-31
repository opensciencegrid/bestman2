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

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;


public class TSRMEventLog implements TSRMLog.iSRMLog {    
    private TSRMFileWriter _writer = null;

    public static final String _gColumnDeliminator = " ";
    public static final String _gNewLine           = "\n";
    public static final String _gWarningCode       = "Warn ";
    public static final String _gDebugCode         = "Debug";
    public static final String _gErrorCode         = "Error";
    public static final String _gExceptionCode     = "Excep";
    public static final String _gInfoCode          = "Info ";
    public static final String _gOutputCode        = "Console";

    public static int _gLogLevelERR = 0;
    public static int _gLogLevelCONSOLE = 1;
    public static int _gLogLevelINFO = 2;
    public static int _gLogLevelDEBUG = 3;
  
    public static int _logLevel = _gLogLevelDEBUG;

    //private long _counter = 0;
    //private String _logFileName = null; // original log file name 
    //private int _numLogFiles = 0;

    //
    // constructors
    //
    public TSRMEventLog() {
	// creates a default event log filename
	Calendar curr = Calendar.getInstance();

	String filename="eventlog."+curr.getTimeInMillis();
	_writer = new TSRMFileWriter(filename);
    }

    public TSRMEventLog(String filename) {
	_writer = new TSRMFileWriter(filename);
    }

    public static void setLogLevel(String input) {
	if (input == null) {
	    TSRMUtil.startUpInfo("Log level by default:"+levelToStr(_logLevel));
	    return;
	}
	if (input.equalsIgnoreCase("debug")) {
	    _logLevel = _gLogLevelDEBUG;
	} else if (input.equalsIgnoreCase("info")) {
	    _logLevel = _gLogLevelINFO;
	} else if (input.equalsIgnoreCase("error")) {
	    _logLevel = _gLogLevelERR;
	} else if (input.equalsIgnoreCase("console")) {
	    _logLevel = _gLogLevelCONSOLE;
	} else {
	    TSRMUtil.startUpInfo("Ignoring invalid log level:"+input+"Setting log level by default:"+levelToStr(_logLevel));
	    return;
	}
	TSRMUtil.startUpInfo("Log level is setting to:"+levelToStr(_logLevel));
    }

    public static String levelToStr(int lev) {
	if (lev == _gLogLevelDEBUG) {
	    return "DEBUG";
	} else if (lev == _gLogLevelINFO) {
	    return "INFO";
	} else if (lev == _gLogLevelERR) {
	    return "ERROR";
	} else if (lev == _gLogLevelCONSOLE) {
	    return "CONSOLE";
	} else {
	    return "UNKNOWN";
	}
    }

    //
    // public functions
    //
    public boolean isEnabled() {
	return (_writer.isEnabled());
    }

    public void error(Class objClass, String methodName, String why, String extra) {	
	if (!isEnabled()) {
	    return;
	}
	if (_logLevel < _gLogLevelERR) {
	    return;
	}
	String msg = generateLogMsg(_gErrorCode, why, objClass, methodName, extra);
	_writer.writeMsg(msg, true);
    }

    public void exception(Class objClass, String methodName, Throwable e) {	
	if (!isEnabled()) {
	    return;
	}
	if (_logLevel < _gLogLevelERR) {
	    return;
	}
	StackTraceElement[] stacks = e.getStackTrace();
	String stackMsg = e.getMessage()+"\n";
	for (int i=0; i<stacks.length; i++) {
	    stackMsg += stacks[i].toString()+"\n";
	}
	String msg = generateLogMsg(_gExceptionCode, stackMsg, objClass, methodName, null);
	_writer.writeMsg(msg, true);
    }

    public void console(Class objClass, String out) {
	if (!isEnabled()) {
	    return;
	}
	if (_logLevel < _gLogLevelCONSOLE) {
	    return;
	}
        String msg = generateLogMsg(_gOutputCode, out, objClass, null, null);
	_writer.writeMsg(msg, true);
    }


    public void debug(Class objClass, String methodName, String why, String extra) {	
	if (!isEnabled()) {
	    return;
	}
	if (_logLevel < _gLogLevelDEBUG) {
	    return;
	}
	String msg = generateLogMsg(_gDebugCode, why, objClass, methodName, extra);
	_writer.writeMsg(msg, true);
    }

    public void info(Class objClass, String methodName, String why, String extra) {	
	if (!isEnabled()) {
	    return;
	}
	if (_logLevel < _gLogLevelINFO) {
	    return;
	}
	String msg = generateLogMsg(_gInfoCode, why, objClass, methodName, extra);
	_writer.writeMsg(msg, true);
    }

    public void warning(Class objClass, String methodName, String why, String extra) {	
	if (!isEnabled()) { 
	    return;
	}
	if (_logLevel < _gLogLevelDEBUG) {
	    return;
	}
	String msg = generateLogMsg(_gWarningCode, why, objClass, methodName, extra);
	_writer.writeMsg(msg, true);
    }
    
    /*
    public long getSize() {
	return _counter;
    }
    */
    //    
    // private functions
    //
    public static String  generateLogMsg(String MsgCode, 
				   String why, 
				   Class objClass, 
				   String methodName, 
				   String extra) 
    {
	StringBuffer msg = new StringBuffer();
		
	msg.append("level="+MsgCode);
	msg.append(_gColumnDeliminator);

	if (objClass != null) {
	    msg.append("class="+objClass.getName());	    
	}

	if (why != null) {
	    msg.append(_gColumnDeliminator);	    
	    msg.append(why);
	    if (methodName != null) {
		msg.append(".");
		msg.append(methodName);
	    } 
	} else {
	    if (methodName != null) {
		msg.append(_gColumnDeliminator);
		msg.append(methodName);
	    } 
	}

	if (extra != null) {
	    msg.append(_gColumnDeliminator);	    
	    msg.append(extra);
	}

	msg.append(" tid="+Thread.currentThread().getName());
	msg.append(_gNewLine);

	return msg.toString();
    }

}
 
