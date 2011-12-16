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

import gov.lbl.srm.transfer.mss.intf.*;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ExecScript
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;

public class ExecScript extends Thread {
  
public boolean _verbose;
private String  _path;
private String _javaLibraryPath;
private String logFile;
private callerIntf callerIntf;
private Process p = null;
private java.util.logging.Logger _theLogger; 
private long startTimeStamp;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ExecScript (String path, String javaLibraryPath, 
	boolean debug, SRM_MSS srmmss, java.util.logging.Logger theLogger) { 
  _path = path;
  _javaLibraryPath = javaLibraryPath;
  callerIntf = srmmss;
  _verbose = debug;
  _theLogger = theLogger;
  this.startTimeStamp = System.currentTimeMillis();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLogFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getLogFile() {
  return logFile;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLogFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLogFile(String logFile) {
  this.logFile = logFile;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStartTimeStamp
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public long getStartTimeStamp() {
  return startTimeStamp;
}

public void setStartTimeStamp(long ll) {
 startTimeStamp = ll;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// execCommand
// Starts a process to execute the command. Prints all output the 
// command gives.
//
//     @param command The <B>full</B> pathname of the command to be
//     executed. No shell builtins or shell meta-chars allowed.
//     @return false if a problem is known to occur, either due to
//     an exception or from the subprocess returning a non-zero value.
//     Returns true otherwise.
//
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
public synchronized boolean execCommand(boolean useEnvp) 
		throws IOException
{
  return(exec(true, false, useEnvp));
}

public void destroyCurrentProcess () {
  if (p != null) {
     p.destroy ();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// exec
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
private synchronized boolean exec
	(boolean printResults, boolean wait, boolean useEnvp) throws IOException 
{

  try {

    Object[] param = new Object[5]; 
    if(_theLogger != null) {
    param[0] = "PrintResults="+printResults;
    param[1] = "wait="+wait;
    param[2] = "useEnvp="+useEnvp;
    param[3] = "PATH="+_path;
    param[4] = "LOGFILE="+logFile;

    _theLogger.log(java.util.logging.Level.FINE,
        "INSIDE_EXEC_METHOD",(Object[])param);
    }
    this.sleep(3000);
    if(useEnvp) {
      String[] envp = new String[1];
      envp[0] = new String("LD_LIBRARY_PATH="+_javaLibraryPath);
      //writeLogFile(logFile,"going to execute the script now");
      p  = Runtime.getRuntime().exec(_path,envp);
    }
    else {
      p  = Runtime.getRuntime().exec(_path);
    }

    if(printResults) {

      try {
        //StreamGobbler errorGobbler = 
	 	   //new StreamGobbler (p.getErrorStream(), "error", this);
        //errorGobbler.setCallerIntf(callerIntf);
        //StreamGobbler outputGobbler = 
	 	   //new StreamGobbler (p.getInputStream(), "output", this);
        //outputGobbler.setCallerIntf(callerIntf);

        //errorGobbler.start();
        //outputGobbler.start();
		//outputGobbler.join(); // added by junmin
        //Exec thread waits until outputGobbler finishes its work

        int returnVal = p.waitFor();
        //writeLogFile(logFile,"script executed return value is "+returnVal);
	    p.destroy();
        if (returnVal != 0) {
            //if (_verbose) {
              param = new Object[5]; 
              if(_theLogger != null) {
              param[0] = "ErrorMessage=Process return value is "+ returnVal;
              param[1] = "ErrorOccured=true";
              param[2] = "CallerIntf="+callerIntf;
              param[3] = "Path="+_path;
              param[4] = "LOGFILE="+logFile;
              _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_EXEC_METHOD_ERROR_OCCURED_TRUE",(Object[])param);
              }
              if(callerIntf != null) {
                 callerIntf.printMessage("SRM-MSS-error: process " +
					"returnValue is " + returnVal);
                 callerIntf.setErrorOccured(true);
                 callerIntf.setErrorMessage("SRM-MSS-error: process " +
					"returnValue is " + returnVal);
              }
              else {
                 System.out.println("SRM-MSS-error: process returnValue is " + 
						returnVal);
              }
              printError(_path); 
            //}
	     return(false);
	    }
      } catch (Exception e) {
         //writeLogFile(logFile,"script executed and exited with execption="+
				//e.getMessage());
         param = new Object[5]; 
         if(_theLogger != null) {
          param[0] = "ErrorMessage="+e.getMessage();
          param[1] = "ErrorOccured=true";
          param[2] = "CallerIntf="+callerIntf;
          param[3] = "Path="+_path;
          param[4] = "LOGFILE="+logFile;
          _theLogger.log(java.util.logging.Level.FINE,
              "INSIDE_EXEC_METHOD_ERROR_OCCURED_EXCEPTION",(Object[])param);
          }
          if(callerIntf != null) { 
            callerIntf.printMessage("SRM-MSS-Exception: " + e.getMessage());
			callerIntf.setErrorOccured(true);
            callerIntf.setErrorMessage("SRM-MSS-Exception: " + e.getMessage());
          }
          e.printStackTrace();
            if(_verbose)
	      printError(e);
      }
    } else if (wait) {
      try {
          // Doesn't always wait. If the previous exec was a print-the-results
          // version, then this will NOT wait unless there is a
          // System.out.println call here! Odd...
          System.out.println(" ");
          int returnVal = p.waitFor();
          //writeLogFile(logFile,"script executed return value is "+returnVal);
	      p.destroy();
          if (returnVal != 0) {
            param = new Object[5]; 
            if(_theLogger != null) {
            param[0] = "ErrorMessage=Process return value is "+ returnVal;
            param[1] = "ErrorOccured=true";
            param[2] = "CallerIntf="+callerIntf;
            param[3] = "Path="+_path;
            param[4] = "LOGFILE="+logFile;
            _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_EXEC_METHOD_ERROR_OCCURED_TRUE",(Object[])param);
            }
            if(callerIntf != null) { 
			   callerIntf.setErrorOccured(true);
               callerIntf.setErrorMessage("SRM-MSS-error:process " +
					"returnValue is " + returnVal);
               callerIntf.printMessage("process returnValue is " + returnVal);
            }
            if (_verbose)
	      printError(_path);
	    return(false);
          }
      } catch (Exception e) {
        //writeLogFile(logFile,"script executed and exited with execption="+
				//e.getMessage());
	  //if (_verbose)
         param = new Object[5]; 
	     if(_theLogger != null) {
         param[0] = "ErrorMessage="+e.getMessage();
         param[1] = "ErrorOccured=true";
         param[2] = "CallerIntf="+callerIntf;
         param[3] = "Path="+_path;
         param[4] = "LOGFILE="+logFile;
         _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_EXEC_METHOD_ERROR_OCCURED_TRUE",(Object[])param);
         }
        if(callerIntf != null) { 
		   callerIntf.setErrorOccured(true);
           callerIntf.setErrorMessage("SRM-MSS-Exception: " + e.getMessage()); 
           callerIntf.printMessage("Exception:"+e.getMessage());
        }
        e.printStackTrace();
	    printError(e);
	  return(false);
	}
      }
   }catch(IOException ioe) { 
     //System.out.println(">>>>>>IOEXCEPTION " + ioe.getMessage());
     throw ioe;
    }
    catch (Exception e) {
         Object[] param = new Object[5]; 
         if(_theLogger != null) {
         param[0] = "ErrorMessage="+e.getMessage();
         param[1] = "ErrorOccured=true";
         param[2] = "CallerIntf="+callerIntf;
         param[3] = "Path="+_path;
         param[4] = "LOGFILE="+logFile;
         _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_EXEC_METHOD_ERROR_OCCURED_TRUE",(Object[])param);
         }
     if(callerIntf != null) { 
		callerIntf.setErrorOccured(true);
		callerIntf.setErrorMessage("SRM-MSS-Exception " + e.getMessage());
		callerIntf.printMessage("SRM-MSS-Exception " + e.getMessage());
     }
     if (_verbose)
       e.printStackTrace(); 
       printError(e);
      return(false);
    }
    if(callerIntf != null) { 
      SRM_MSS srmmss = getSrmMSS();
	  srmmss.setCompleted(true);
    }
    return(true);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getProcess
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Process getProcess () {
  return p;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//writeLogFile
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void writeLogFile(String logFile, String msg)
    throws Exception {
  FileOutputStream fos = new FileOutputStream(logFile);
  BufferedWriter out = new BufferedWriter(new OutputStreamWriter(fos));
  out.write(msg);
  out.flush();
  out.close();
  fos.close();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
// changed to public by Junmin 1218-2008
public synchronized void printError(Exception e) 
{

	System.out.println("....................error occurred. executing this file: _path="+_path); // added by junmin
      Object[] param = new Object[6]; 
      if(_theLogger != null) {
         param[0] = "ErrorMessage="+e.getMessage();
         param[1] = "ErrorOccured=true";
         param[2] = "CallerIntf="+callerIntf;
         param[3] = "Path="+_path;
         param[4] = "Error doing exec, did you specify the full pathname";
         param[5] = "LOGFILE="+logFile;
         _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_PRINT_ERROR",(Object[])param);
       }
  if(callerIntf != null) { 
    callerIntf.printMessage("Error doing exec. " + _path); 
    callerIntf.printMessage("Did you specify the full pathname?");
    callerIntf.printMessage("Exception occured message " + e.getMessage());
    callerIntf.setErrorOccured(true);
    callerIntf.setErrorMessage("SRM-MSS-Exception" + e.getMessage());
    //added on Jan 20, 09
    SRM_MSS srmmss = getSrmMSS();
    srmmss.setCompleted(false);
    srmmss.setErrorOccured(true);
    srmmss.setErrorMessage(e.getMessage());
  }
  else {
    System.out.println("SRM-MSS-error: Error doing exec."); 
    System.out.println("SRM-MSS-error: Did you specify the full pathname?");
    System.out.println("SRM-MSS-error: Exception occured message " + 
			e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void printError(String path) 
{
   Object[] param = new Object[3]; 
   if(_theLogger != null) {
         param[0] = "CallerIntf="+callerIntf;
         param[1] = "Error executing "+_path;
         _theLogger.log(java.util.logging.Level.FINE,
                 "INSIDE_PRINT_ERROR",(Object[])param);
    }
  if(callerIntf != null) {
    callerIntf.printMessage("SRM-MSS-error: Error executing " + path);
    callerIntf.setErrorOccured(true);
    callerIntf.setErrorMessage("SRM-MSS-error: executing " + path);
  }
  else {
    System.out.println("SRM-MSS-error : Error executing " + path);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSRMMSS
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_MSS getSrmMSS () {
   return (SRM_MSS) callerIntf;
}

public java.util.logging.Logger  getLogger() {
  return _theLogger;
}
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// class StreamGobbler
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class StreamGobbler extends Thread
{
  InputStream is;
  String type;
  OutputStream os;
  callerIntf cIntf;
  ExecScript _execScript;
  StringBuffer output = new StringBuffer(500); 
  java.util.logging.Logger _logger;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler (InputStream is, String type, 
	ExecScript execScript)
{
    this(is, type, null, execScript);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler (InputStream is, String type, 
	OutputStream redirect, ExecScript execScript)
{
    this.is = is;
    this.type = type;
    this.os = redirect;
    _execScript = execScript;
    _logger = execScript.getLogger();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCallerIntf
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCallerIntf (callerIntf cIntf) 
{
  this.cIntf = cIntf;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run()
{
  SRM_MSS srmmss = _execScript.getSrmMSS(); 
  boolean verbose = _execScript._verbose;
  if(srmmss != null) {
    //srmmss.setOutputBuffer(output);
    output = srmmss.getOutputBuffer();
  }

  try {
      PrintWriter pw = null;
      if(os != null) 
        pw = new PrintWriter(os);
      InputStreamReader isr = new InputStreamReader (is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while((line = br.readLine()) != null) {
         System.out.println("INside while loop" + line);
         output.append(line);
         System.out.println("Appended in output " + output.toString());
         if(pw != null) {
	       pw.println(line);
           pw.flush();
         }
      }
      if(pw != null) {
        pw.flush();
	    pw.close();
      }
      if(isr != null) 
        isr.close();
      if(srmmss != null) srmmss.setCompleted(true);
      System.out.println("output contents " + output.toString());
      if(srmmss != null) {
        output = srmmss.getOutputBuffer();
        System.out.println("output contents again " + output.toString());
      }

      Object[] param = new Object[1]; 
      if(_logger != null) {
      param[0] = "output contents="+output.toString();
      _logger.log(java.util.logging.Level.FINE,
         "INSIDE_RUN_METHOD",(Object[])param);
      }
    }catch(IOException ioe) {
		if (srmmss != null) { 
	      srmmss.setErrorMessage("SRM-MSS-Exception: " + ioe.getMessage());
	      srmmss.setErrorOccured(true);
	      srmmss.printMessage("SRM-MSS-Exception: " + ioe.getMessage());
        }
		System.out.println("SRM-MSS-Exception: Detected exception " +
			"when running ExecScript ");
		_execScript.printError(ioe);
      Object[] param = new Object[2]; 
      if(_logger != null) {
      param[0] = "Detected exception when running ExecScript";
      param[1] = "Exception="+ioe.getMessage();
      _logger.log(java.util.logging.Level.FINE,
         "INSIDE_RUN_METHOD_EXCEPTION",(Object[])param);
      }
      ioe.printStackTrace ();
    }
}

}
