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

//javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=3
class StreamGobbler extends Thread {
    InputStream is;
    String type;
    boolean isEmpty = true;
    String msg = null;

    StreamGobbler(InputStream is, String type) {
	this.is = is;
	this.type = type;
    }

    public String getMsg(){
	return msg;
    }

    public void run() {
	try {
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line = null;
	    while ((line = br.readLine()) != null) {
		TSRMUtil.startUpInfo(type +">"+line);
		isEmpty = false;
		if (msg != null) {
		    msg +=" "+line;
		} else {
		    msg = line;
		}
	    }
	}catch (IOException e) {
	    e.printStackTrace();
	}	
    }
}
//
// source is from http://forum.java.sun.com/thread.jsp?forum=31&thread=426291
//
public class TPlatformUtil {
    public static final boolean makeDirectoriesWritableByAll(File f) {
	if (f == null) {
	    return false;
	}

	String newPermission = "774";
	return execChmod(f.getParent(), newPermission);
    }

    public static final boolean chmod(String path,  boolean writable) {
	String newPermission = null;
	if (writable) {
	    newPermission = "666";
	} else {
	    newPermission = "644";
	} 

	return execChmod(path, newPermission);
    }

    private static final boolean execChmod(String path, String newPermission) {
	try {	
	    String cmd = "chmod "+newPermission+" "+path;
	    Runtime.getRuntime().exec(cmd);
	    TSRMLog.debug(TPlatformUtil.class, null, "event=chmod permission="+newPermission, "path="+path);
	    //System.out.println("Oh look!"+cmd);	    
	    //TSRMUtil.sleep(5000);
	    return true;
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}
    }

    public static final boolean execCmdValidationWithReturn(String cmd) {
	Process p = null;
	try {
	    p = Runtime.getRuntime().exec(cmd);	    	    
	    OutputStream os2 = p.getOutputStream();
	    if (os2 != null) {
		PrintWriter pw = new PrintWriter(os2);
		pw.println();
		pw.flush();
	    }

	    StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
	    StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");

	    errorGobbler.start();
	    outputGobbler.start();	    	    

	    outputGobbler.join();
	    errorGobbler.join();

	    p.waitFor();
	    if (errorGobbler.getMsg() == null) {
		return true;
	    } else {
		return false;
	    }
	} catch (Exception e) {
	    TSRMUtil.startUpInfo("Trouble with: "+cmd+" exception="+e);
	    e.printStackTrace();
	    return false;
	} finally {
	    if (p != null) p.destroy();
	}
    }

    public static final String execShellCmdWithOutput(String cmd, boolean showOutput) {
      Process p = null;
      try {
	String[] cmds = {"/bin/sh", "-c", cmd};
        p = Runtime.getRuntime().exec(cmds);
	return getProcessOutput(p, showOutput);
      } catch (Exception e) {
	TSRMUtil.startUpInfo("Trouble with commands: "+cmd+" exception:"+e); 
	e.printStackTrace();                                                                                                                         
	return e.getMessage();   		     
      } finally {
	  if (p != null) p.destroy();
      }
    }

    /*
    public static final String execShellCmdWithOutput0(String cmd, boolean showOutput) {
      try {
	String[] cmds = {"/bin/sh", "-c", cmd};
	Process p = Runtime.getRuntime().exec(cmds);
	return getProcessOutput(p, showOutput);
      } catch (Exception e) {
	TSRMUtil.startUpInfo("Trouble with commands: "+cmd+" exception:"+e); 
	e.printStackTrace();                                                                                                                         
	return e.getMessage();   		     
      }
    }
    */

    public static final String execCmdWithOutput(String cmd, boolean showOutput) {
	Process p = null;
	try {
	    p = Runtime.getRuntime().exec(cmd);
	    return getProcessOutput(p, showOutput);
	} catch (Exception e) {
	    TSRMUtil.startUpInfo("Trouble with: "+cmd+" exception:"+e);
	    e.printStackTrace();
	    return e.getMessage();
	} finally {
	    if (p != null) p.destroy();
	}
    }

    public static final String getProcessOutput(Process p, boolean showOutput) {
         try {	    
	    StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
	    StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");

	    errorGobbler.start();
	    outputGobbler.start();
	    
	    outputGobbler.join();
	    errorGobbler.join();

	    p.waitFor();

	    if (!showOutput) {
		return errorGobbler.getMsg();
	    } else if (errorGobbler.getMsg() != null) {
		return "ERROR:"+errorGobbler.getMsg() + "OUTPUT:"+outputGobbler.getMsg();
	    } else {
		return outputGobbler.getMsg();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return e.getMessage();
	}
    }

    public static final boolean execWithOutput(String cmd) {
	Process p = null;
	try {
	    p = Runtime.getRuntime().exec(cmd);
	    
	    StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR");
	    StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT");

	    errorGobbler.start();
	    outputGobbler.start();
	    
	    p.waitFor();

	    return errorGobbler.isEmpty;
	} catch (Exception e) {
	    TSRMUtil.startUpInfo("Trouble with: "+cmd+" exception:"+e);
	    e.printStackTrace();
	    return false;
	} finally {
	    if (p != null) p.destroy();
	}
    }

    public static final String getDisk(String path) throws
	IllegalArgumentException, IOException
    {
	if (path == null) 
	    throw new IllegalArgumentException("path == null");
	
	String OSName = System.getProperty("os.name");
	if (OSName.startsWith("Windows")) {
	    int pos = path.indexOf(':');
	    if (pos == -1) {
		throw new IllegalArgumentException("Bad format for specifing a path in Windows OS.");
	    }
	    return path.substring(0, pos+1);
	} else if (OSName.startsWith("Linux") || OSName.startsWith("SunOS")) {
	    return  path; //getDiskDriveInLinux(path);
	} else
	    throw new IllegalStateException("getDisk(path) currently does not support this operating system: " + 
					    System.getProperty("os.name"));
    }
        
    
    public static final long getFreeSpace(File path) throws SecurityException,
							    IllegalArgumentException, IllegalStateException, IOException
	
    {
	if (path == null) 
	    throw new IllegalArgumentException("path == null");
	
	if (!path.exists()) {
	    throw new IllegalArgumentException("No such path.");
	}

	long result = path.getFreeSpace();
	if (result == 0) {
	    System.out.println("Free space reported to be 0. Double checking with df");
	    result = getFreeSpaceObsolete(path);
	}

	return result;
    }

        
    public static final long getFreeSpaceObsolete(File path) throws
	IllegalArgumentException, IllegalStateException, IOException
    {
	if (path == null) 
	    throw new IllegalArgumentException("path == null");
	
	String OSName = System.getProperty("os.name");
	TSRMUtil.startUpInfo("["+OSName+"]");
	if (OSName.startsWith("Windows")) {
	    return getFreeSpaceInWindows(path);
	} else if (OSName.startsWith("Linux")) {
	    //System.out.println("### disk drive = "+getDiskDriveInLinux(path.getAbsolutePath()));
	    return getFreeSpaceInLinux(path.getAbsolutePath());
	} else if (OSName.startsWith("SunOS")) {
	    return getFreeSpaceInUnix(path.getAbsolutePath());
	}
	//else if (System.getProperty("os.name").startsWith("..."))
	// return ...
	else
	    throw new IllegalStateException("getFreeSpace(File) currently does not support this operating system: " + 
					    OSName);
    }
    
    

    private static final long getFreeSpaceInUnix(String path) throws
	IllegalStateException, IOException
    {
	Process p = null;
	//PrintWriter writer = null;
	BufferedReader reader = null;
	try {
	    String command = "df -k "+path;
	    p = Runtime.getRuntime().exec(command);
	    
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    
	    String result = getOutput(reader);

	    // Here are some real examples seen of what line can look like:
	    // srm.lbl.gov% df -k /tmp/junk
	    // Filesystem            kbytes    used     avail     capacity Mounted on
	    // swap                 11228024   1136   11226888    1%       /tmp
	    
	    // parse the number (of bytes free) from line:
	    TSRMUtil.startUpInfo("## unix command: "+command);
	    TSRMUtil.startUpInfo("## result: "+result);
	    String[] tokens = result.split("\\s+", -1); 
	    
	    return Long.parseLong(tokens[3])*1024;
	    //String drive = tokens[0]; 
	    //return drive;
	}
	finally {
	    //if (writer != null) writer.close();
	    if (reader != null) reader.close();
	    if (p != null) p.destroy();
	}
    }
    
    private  static final long getFreeSpaceInLinux(String path) throws
	IllegalStateException, IOException
    {
	//PrintWriter writer = null;
	Process p = null;
	BufferedReader reader = null;
	try {
	    // create a temp  file to run the dir command:
	    // this does not work for unix, since have to change the mod first then exec
	    // but the mod may not have changed before exec (unless you wait for awhile)
	    // the alternative is to have a file already having the 777 there and insert content
	    // when needed. But this is not a good solution either
	    // we will just exec the command directly.
	    /*
	      File script = File.createTempFile("script", "");
	      script.deleteOnExit();
	      writer = new PrintWriter( new FileWriter(script, false), true );
	      writer.println("df -B 1 " + path);
	      writer.close(); // MUST close it at this point, else the Process below will fail to run
	      
	      // get output from running the script:
	      Process p0 = Runtime.getRuntime().exec("chmod 777 "+ script.getAbsolutePath() );
	    */
	    //File script = new File("/tmp/hi");		    
	    //Process p = Runtime.getRuntime().exec(script.getAbsolutePath());
	    
	    String command = "df -B 1 "+path;
	    p = Runtime.getRuntime().exec(command);
	    
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	  
	    String result = getOutput(reader);

	    // Here are some real examples seen of what line can look like:
	    // dmx.lbl.gov% df -B 1 /tmp/junk
	    // Filesystem            1-blocks      Used Available Use% Mounted on
	    // /dev/hda6            10700922880 6354206720 3803140096  63% /
	    
	    // parse the number (of bytes free) from line:
	    
	    TSRMUtil.startUpInfo("## linux command: "+command);
	    TSRMUtil.startUpInfo("## result: "+result);
	    String[] tokens = result.split("\\s+", -1); 
	    TSRMUtil.startUpInfo("## tokens size: "+tokens.length);
	    for (int i=0; i<tokens.length; i++) {
		TSRMUtil.startUpInfo("["+tokens[i]+"]");
	    }
	    return Long.parseLong(tokens[3]);
	    //String drive = tokens[0]; 
	    //return drive;
	}
	finally {
	    //if (writer != null) writer.close();
	    if (reader != null) reader.close();
	    if (p != null) p.destroy();
	}
    }
    
  
    private static final String getOutput(BufferedReader reader) throws IllegalStateException, IOException {
	String line = null;
	String result = null;
	StringBuffer buf = new StringBuffer();
	while (true) {
	    line = reader.readLine();
	    if (line == null) {
		if (buf.length() == 0) {
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		}
		break;
	    } else if (!line.startsWith("Filesystem")) {  		
		if (buf.length() > 0) {
		    buf.append("\n");
		} 
		buf.append(line);
	    }
	}
	result = buf.toString();
	
	return result;
    }

    private static final long getFreeSpaceInWindows(File where) throws
	IllegalStateException, IOException 
    {
	Process p = null;
	PrintWriter writer = null;
	BufferedReader reader = null;
	try {
	    // create a temp .bat file to run the dir command:
	    File script = File.createTempFile("script", ".bat");
	    script.deleteOnExit();
	    writer = new PrintWriter( new FileWriter(script, false), true );
	    writer.println("dir \"" + where.getCanonicalPath() + "\"");
	    writer.close(); // MUST close it at this point, else the Process below will fail to run
	    
	    // get output from running the .bat file:
	    p = Runtime.getRuntime().exec( script.getAbsolutePath() );
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    String line = null;
	    while (true) {
		line = reader.readLine();
		if (line == null) 
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		else if (line.endsWith("free")) break;
	    }
	    // Here are some real examples seen of what line can look like:
	    //        12 dir(s)     631,889,920 bytes free
	    //        10 dir(s)     24,167.73 MB free
	    //                      788,021,248 bytes free
	    // (This last case happens if there are no contents inside volume)
	    // The parsing code below MUST handle all these cases
	    
	    // parse the number (of bytes free) from line:
	    String[] tokens = line.split("\\s+", -1); 
	    /* in ALL cases, should
	       have 		tokens[tokens.length - 3] = "788,021,248", 
	       tokens[tokens.length - 2] = "bytes" or "KB" or "MB" or "GB", 
	       tokens[tokens.length - 1] = "free"  
	       
	    */
	    String numberText = tokens[tokens.length - 3].replaceAll(",", ""); 
	    // replaceAll eliminates the commas but NOT any decimal point
	    
	    String unit = tokens[tokens.length - 2];
	    if (unit.equals("bytes"))
		return Long.parseLong(numberText);
	    else {
		System.err.println("--------------------------------------------------");
		
		System.err.println("WARNING: POSSIBLE LOSS OF PRECISION in determining the free space");
		System.err.println("The operating system reported the free space as" + numberText + " " + unit);
		System.err.println("This output may have been rounded, however,  which means that the exact number of bytes is impossible to determine");
		
		System.err.println("--------------------------------------------------");
		
		double number = Double.parseDouble(numberText);
		if (unit.equals("KB")) return (long) (number * 1024); 
		// see http://groups.google.com/groups?dq=&hl=en&lr=&ie=UTF-8&threadm=QNJ6c.54315%24H44.994342%40bgtnsc04-news.ops.worldnet.att.net&prev=/groups%3Fhl%3Den%26lr%3D%26ie%3DUTF-8%26group%3Dcomp.os.msdos.programmer
		
		else if (unit.equals("MB")) return (long) (number * 1024 * 1024);
		else if (unit.equals("GB")) return (long) (number * 1024 * 1024 * 1024);
		
		else throw new IllegalStateException("Program encountered unit = " +
						     unit + " which it is unable to handle");
	    }
	}
	finally {
	    if (writer != null) writer.close();
	    if (reader != null) reader.close();
	    if (p != null) p.destroy();
	}
    }
}
