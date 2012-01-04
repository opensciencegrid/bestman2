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

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ExecScript
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

public class ExecScript {
  
public boolean _verbose;
private String  _path;
private Log logger;
private testToolIntf cIntf;
private SRMTesterDriver gcTest;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ExecScript (String path, boolean debug, Log logger, 
	testToolIntf cIntf, SRMTesterDriver gcTest) { 
  _path = path;
  _verbose = debug;
  this.logger = logger;
  this.cIntf = cIntf;
  this.gcTest = gcTest;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// exec
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean execCommand(String command[])
{
  return(exec(command, true, false));
}
  
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// exec
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
private synchronized boolean exec(String command[], 
	boolean printResults, boolean wait) 
{

  try {

    StringBuffer buf = new StringBuffer();
    util.printMessage("\n+++++++++++++++++++++++++++++++++++++++++",logger,null);
    util.printMessage("Executing command",logger,null);
    for(int i = 0; i < command.length; i++) {
      buf.append(command[i]+" ");
    }
    util.printMessage(buf.toString(),logger,null);
    util.printMessage("\n+++++++++++++++++++++++++++++++++++++++\n",logger,null);

    boolean ok = false;
    Thread t = new Thread();
    Process p = null; 

    while (!ok) {
      try {
          p  = Runtime.getRuntime().exec(command);
          ok = true;
      }catch(IOException ioe) {
          ok = false;
          if(_verbose) { 
            util.printMessageNL(".",logger,null);
          }
          t.sleep(1000); //wait for a while and then execute
      }catch(Exception e) {
          e.printStackTrace();
          ok = true;
      }
    }//end while


    if(printResults) {

      try {
        StreamGobbler errorGobbler = 
	 	   new StreamGobbler (p.getErrorStream(), "error",_verbose,gcTest);
        errorGobbler.setCallerIntf(cIntf);
        errorGobbler.setLogger(logger);
        StreamGobbler outputGobbler = 
	 	   new StreamGobbler (p.getInputStream(), "output",_verbose,gcTest);
        outputGobbler.setCallerIntf(cIntf);
        outputGobbler.setLogger(logger);

        errorGobbler.start();
        outputGobbler.start();

        int returnVal = p.waitFor();

        if (returnVal != 0) {

            if (_verbose) printError(command); 
	        return(false);
	     }
      } catch (Exception e) {
          if(_verbose) printError(command, e);
      }
    } else if (wait) {
      try {
          // Doesn't always wait. If the previous exec was a print-the-results
          // version, then this will NOT wait unless there is a
          // System.out.println call here! Odd...
          System.out.println(" ");
          int returnVal = p.waitFor();
          if (returnVal != 0) {
            if (_verbose) printError(command);
	        return(false);
          }
      } catch (Exception e) {
	    if (_verbose) printError(command, e);
	    return(false);
	  }
    }

   } catch (Exception e) {
     if (_verbose)
       printError(command, e);
      return(false);
    }
    return(true);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
  
private synchronized void printError(String command[], Exception e) 
{

  StringBuffer cmd = new StringBuffer(); 
  for(int i = 0; i < command.length; i++) {
     cmd.append(command[i] + " ");
  }
  util.printMessage("Error doing exec " + cmd.toString() + ".",logger,null); 
  util.printMessage("Did you specify the full pathname?",logger,null);
  util.printMessage("Exception occured message " + e.getMessage(),logger,null);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printError
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private synchronized void printError(String command[]) 
{
  StringBuffer cmd = new StringBuffer(); 
  for(int i = 0; i < command.length; i++) {
     cmd.append(command[i]+ " ");
  }
  util.printMessage("Error executing '" + cmd.toString() + "'.",logger,null);
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
  boolean verbose;
  testToolIntf cIntf;
  Log logger;
  SRMTesterDriver gcTest;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler (InputStream is, String type, 
		boolean verbose, SRMTesterDriver gcTest)
{
    this(is, type, null,verbose,gcTest);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// StreamGobbler
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

StreamGobbler (InputStream is, String type, 
	OutputStream redirect,boolean verbose, SRMTesterDriver gcTest )
{
    this.is = is;
    this.type = type;
    this.os = redirect;
    this.verbose = verbose;
    this.gcTest = gcTest;
}

public void setCallerIntf(testToolIntf cIntf) { 
  this.cIntf = cIntf;
}

public void setLogger(Log logger) { 
  this.logger = logger;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run()
{
  try {
      PrintWriter pw = null;
      if(os != null) 
        pw = new PrintWriter(os);
      InputStreamReader isr = new InputStreamReader (is);
      BufferedReader br = new BufferedReader(isr);
      String line = null;
      while((line = br.readLine()) != null) {
        parseStream(line,verbose);
        if(pw != null) pw.println(line);
      }
      if(!this.type.equals("error")) {
        if(cIntf != null) { 
          cIntf.setDone();
        }
      }
      if(pw != null) pw.flush();
    }catch(IOException ioe) {
      ioe.printStackTrace ();
    }
}

private void parseStream(String line, boolean verbose) {

   util.printMessage(line,logger,null);
   if(type.equals("output")) {
    if(!line.equals("")) {
    try {
     SiteInfo siteInfo = new SiteInfo(line);
     gcTest.addInfo(siteInfo);    
    }catch(Exception e) {
        System.out.println("Exception : " + e.getMessage());
    }
    }
   }
}
}
