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

package gov.lbl.srm.client.main;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ExecScript
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;

public class ExecScript extends Thread {
  
public boolean _verbose;
private String  _path;
private String _javaLibraryPath;
public String gucEventLogPath="";
private Process p = null;
public boolean debug;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ExecScript (String path, String gucLogPath, String javaLibraryPath, 
	boolean verbose, boolean debug) { 
  _path = path;
  gucEventLogPath = gucLogPath;
  _javaLibraryPath = javaLibraryPath;
  _verbose = verbose;
  this.debug = debug;
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
  
public synchronized boolean execCommand(String[] command, boolean useEnvp) 
		throws IOException
{
  return(exec(command, true, false, useEnvp));
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
	(String[] command, boolean printResults, boolean wait, boolean useEnvp) throws IOException 
{

  try {

    this.sleep(3000);
    System.out.println("SRM-CLIENT: ..............................");
    for(int i = 0; i < command.length; i++) {
      System.out.println("SRM-CLIENT: " + command[i]);
    }
    System.out.println("SRM-CLIENT: ..............................");
    if(useEnvp) {
      String[] envp = new String[1];
      envp[0] = new String("LD_LIBRARY_PATH="+_javaLibraryPath);
      //p  = Runtime.getRuntime().exec(_path,envp);
      p  = Runtime.getRuntime().exec(command,envp);
    }
    else {
      //p  = Runtime.getRuntime().exec(_path);
      p  = Runtime.getRuntime().exec(command);
    }

    if(printResults) {

      try {
        StreamGobbler2 errorGobbler = 
	 	   new StreamGobbler2 (p.getErrorStream(), "error", this);
        StreamGobbler2 outputGobbler = 
	 	   new StreamGobbler2 (p.getInputStream(), "output", this);

        errorGobbler.start();
        outputGobbler.start();


        int returnVal = p.waitFor();
	    p.destroy();
        if (returnVal != 0) {
            if (_verbose) {
              System.out.println("process returnValue is " + returnVal);
              printError(_path); 
			  System.out.println("SRM-CLIENT:GUCSciptError="+
						 	 errorGobbler.getOutputBuffer().toString());
            }
	     return(false);
	    }
      } catch (Exception e) {
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
	      p.destroy();
          if (returnVal != 0) {
            if (_verbose)
	      printError(_path);
	    return(false);
          }
      } catch (Exception e) {
	  if (_verbose)
        e.printStackTrace();
	    printError(e);
	  return(false);
	}
      }
   }catch(IOException ioe) { 
     throw ioe;
    }
    catch (Exception e) {
     if (_verbose)
       e.printStackTrace(); 
       printError(e);
      return(false);
    }
    return(true);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
private synchronized void printError(Exception e) 
{
  System.out.println("Error doing exec."); 
  System.out.println("Did you specify the full pathname?");
  System.out.println("Exception occured message " + e.getMessage());
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void printError(String path) 
{
  System.out.println("Error executing " + path);
}

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// class StreamGobbler2
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

class StreamGobbler2 extends Thread
{
  InputStream is;
  String type;
  OutputStream os;
  ExecScript _execScript;
  StringBuffer output = new StringBuffer(1000); 

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler2
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler2 (InputStream is, String type, 
	ExecScript execScript)
{
    this(is, type, null, execScript);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler2
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler2 (InputStream is, String type, 
	OutputStream redirect, ExecScript execScript)
{
    this.is = is;
    this.type = type;
    this.os = redirect;
    _execScript = execScript;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getOutputBuffer
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public StringBuffer getOutputBuffer() {
   return output;
}		

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run()
{
  boolean verbose = _execScript._verbose;
  String gucEventLogPath = _execScript.gucEventLogPath;
  boolean debug = _execScript.debug;

  try {
      FileOutputStream outFile=null;
      BufferedWriter out = null;
      if(debug && !gucEventLogPath.equals("")) {
        outFile = new FileOutputStream(gucEventLogPath);
        out = new BufferedWriter(new OutputStreamWriter(outFile));
      }
 
      PrintWriter pw = null;
      if(os != null) 
        pw = new PrintWriter(os);
      InputStreamReader isr = new InputStreamReader (is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
	  
      while((line = br.readLine()) != null) {
         //System.out.println("type="+type+" line="+line);
         if(verbose) System.out.println(line);
         if(out != null) out.write(line+"\n");
         output.append(line);
         if(pw != null)
	       pw.println(line);
      }
      if(pw != null) {
        pw.flush();
	pw.close();
      }
      if(out !=null) out.close();
      if(isr != null) 
        isr.close();
    }catch(IOException ioe) {
      ioe.printStackTrace ();
    }
}

}
