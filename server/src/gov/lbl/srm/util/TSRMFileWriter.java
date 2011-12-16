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

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.util.TimeZone;


public class TSRMFileWriter {
    private FileWriter _writer;

    private long _counter = 0;
    private String _logFileName = null; // original log file name 
    private int _numLogFiles = 0;
    private int _maxLogFilesToRetain = -1;

    private static final SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z' ");

    //
    // constructors
    //
    public TSRMFileWriter(String filename) {
	if (setWriter(filename)) {
	    _logFileName = filename;
	} else {
	    System.exit(1);
	}

	_dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

	String strMax = System.getProperty("SRMMaxNumEventLog");
	if (strMax != null) {
	   _maxLogFilesToRetain = Integer.parseInt(strMax);
	   if (_maxLogFilesToRetain <= 0) {
	     System.err.println("Error: "+strMax+" is an invalid number for SRMMaxNumEventLog.");
	     System.exit(1);
	   } else {
	     System.out.println("Will set max event logs in cache = "+_maxLogFilesToRetain);	   
	   }
	}
    }

    //
    // public functions
    //
    public boolean isEnabled() {
	return (_writer != null);
    }

    public long getSize() {
	return _counter;
    }

    private void logTimeStamps() {	
	try {
	    _writer.write("ts="+_dateFormatter.format(System.currentTimeMillis()));
	    _writer.flush();
	} catch (java.io.IOException e) {
	    TSRMLog.toConsoleAlert("failed to write dates to log file.");
	}
    }

    /*
    private void logTimeStampsGMT() {
	Calendar curr = Calendar.getInstance();
	//DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.FULL);
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd-HH:mm:ssz ");

	try {
	    _writer.write(formatter.format(curr.getTime()));	
	    formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
	    _writer.write(formatter.format(curr.getTime()));	
	    _writer.flush();
	} catch (java.io.IOException e) {
	    TSRMLog.toConsoleAlert("failed to write dates to log file.");
	}	
    }
    */

  private void rotate(boolean includeTimeStamp) {      
      _numLogFiles ++;
      
      //File curr = new File(_logFileName);	    
      File curr = TSRMUtil.initFile(_logFileName);
      //curr.renameTo(new File(_logFileName+"."+_numLogFiles));
      try {
	String msgStart = TSRMEventLog.generateLogMsg(TSRMEventLog._gInfoCode, null, TSRMFileWriter.class, 
						      "event=rotateStart", "destination="+_logFileName+"."+_numLogFiles);
	
	writeLine(msgStart, includeTimeStamp);
	
	boolean result;
	if (_maxLogFilesToRetain == 1) {
	  result = curr.delete();
	} else {
	  result = curr.renameTo(TSRMUtil.initFile(_logFileName+"."+_numLogFiles));	 	
	  if (_maxLogFilesToRetain > 0) {
	    int numToDelete = _numLogFiles - _maxLogFilesToRetain + 1;
	    File toDelete = TSRMUtil.initFile(_logFileName+"."+numToDelete);
	    toDelete.delete();
	  }
	}
	String msgEnd = TSRMEventLog.generateLogMsg(TSRMEventLog._gInfoCode, null, TSRMFileWriter.class,
						  "event=rotateEnd", "destination="+_logFileName+"."+_numLogFiles+" deleted="+result);
	writeLine(msgEnd, includeTimeStamp);
      } catch (Exception e) {
	e.printStackTrace();
	String msgAlert = TSRMEventLog.generateLogMsg(TSRMEventLog._gInfoCode, null, TSRMFileWriter.class,
						      "event=rotateFailed", "destination="+_logFileName+"."+_numLogFiles);
	writeLine(msgAlert, includeTimeStamp);
      }
      //if (setWriter(_logFileName+"."+_numLogFiles)) {
      if (setWriter(_logFileName)) {
	_counter = 0;
      } else {
	return;
      }
  }

    public void writeMsg(String msg, boolean includeTimeStamp) {
      if (_counter > TSRMLog._gMAX_BYTES_PER_FILE) {
	rotate(includeTimeStamp);
      }
      writeLine(msg, includeTimeStamp);
    }

    public void writeLine(String msg, boolean includeTimeStamp) {
	if (includeTimeStamp) {
	    logTimeStamps();
	    _counter += 46;
	}

	try {
	    _writer.write(msg);
	    _counter += msg.length();
	    _writer.flush();
	} catch (java.io.IOException e) {
	    TSRMLog.toConsoleAlert("failed to write msg to log.");
	}
    }

    private boolean setWriter(String filename) {
	try {
	    //TSRMLog.toConsole("Assigning log file to be: "+filename);
	    _writer = new FileWriter(filename);
	    return true;
	} catch (java.io.IOException e) {
	    //TSRMLog.toConsoleAlert("Failed to create log file: "+filename+" for events. Please make sure path exists and has write permission for SRM.");
	    e.printStackTrace(); // cannt log it when rotating log files, causes endless loop 
	    _writer = null;
	    return false;
	}
    }
}
 
