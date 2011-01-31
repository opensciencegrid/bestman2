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

import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;

//
// source is from http://forum.java.sun.com/thread.jsp?forum=31&thread=426291
//
public class TPlatformUtil {
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
    
    private static final String getDiskDriveInLinux(String path) throws
	IllegalStateException, IOException
    {
	PrintWriter writer = null;
	BufferedReader reader = null;
	try {
	    // create a temp  file to run the dir command:
	    File script = File.createTempFile("script", "");
	    script.deleteOnExit();
	    writer = new PrintWriter( new FileWriter(script, false), true );
	    writer.println("df -B 1 " + path);
	    writer.close(); // MUST close it at this point, else the Process below will fail to run
	    
	    // get output from running the script:
	    Process p0 = Runtime.getRuntime().exec("chmod 777 "+ script.getAbsolutePath() );
	    Process p = Runtime.getRuntime().exec(script.getAbsolutePath());
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    String line = null;
	    while (true) {
		line = reader.readLine();
		if (line == null) 
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		else if (line.startsWith("/")) break;
	    }
	    // Here are some real examples seen of what line can look like:
	    // dmx.lbl.gov% df -B 1 /tmp/junk
	    // Filesystem            1-blocks      Used Available Use% Mounted on
	    // /dev/hda6            10700922880 6354206720 3803140096  63% /
	    
	    // parse the number (of bytes free) from line:
	    String[] tokens = line.split("\\s+", -1); 
	    
	    String drive = tokens[0]; 
	    return drive;
	}
	finally {
	    if (writer != null) writer.close();
	    if (reader != null) reader.close();
	}
    }
    
    public static final long getFreeSpace(File path) throws
	IllegalArgumentException, IllegalStateException, IOException
    {
	if (path == null) 
	    throw new IllegalArgumentException("path == null");
	
	String OSName = System.getProperty("os.name");
	System.out.println("["+OSName+"]");
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
	PrintWriter writer = null;
	BufferedReader reader = null;
	try {
	    String command = "df -k "+path;
	    Process p = Runtime.getRuntime().exec(command);
	    
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    String line = null;
	    while (true) {
		line = reader.readLine();
		if (line == null) 
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		else if (!line.startsWith("Filesystem")) {	
		    break;
		}
	    }
	    // Here are some real examples seen of what line can look like:
	    // srm.lbl.gov% df -k /tmp/junk
	    // Filesystem            kbytes    used     avail     capacity Mounted on
	    // swap                 11228024   1136   11226888    1%       /tmp
	    
	    // parse the number (of bytes free) from line:
	    String[] tokens = line.split("\\s+", -1); 
	    
	    return Long.parseLong(tokens[3])*1024;
	    //String drive = tokens[0]; 
	    //return drive;
	}
	finally {
	    if (writer != null) writer.close();
	    if (reader != null) reader.close();
	}
    }
    
    private static final long getFreeSpaceInLinux(String path) throws
	IllegalStateException, IOException
    {
	PrintWriter writer = null;
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
	    Process p = Runtime.getRuntime().exec(command);
	    
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    String line = null;
	    while (true) {
		line = reader.readLine();
		if (line == null) 
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		else if (!line.startsWith("Filesystem")) break;
	    }
	    // Here are some real examples seen of what line can look like:
	    // dmx.lbl.gov% df -B 1 /tmp/junk
	    // Filesystem            1-blocks      Used Available Use% Mounted on
	    // /dev/hda6            10700922880 6354206720 3803140096  63% /
	    
	    // parse the number (of bytes free) from line:
	    
	    String[] tokens = line.split("\\s+", -1); 
	    
	    return Long.parseLong(tokens[3]);
	    //String drive = tokens[0]; 
	    //return drive;
	}
	finally {
	    if (writer != null) writer.close();
	    if (reader != null) reader.close();
	}
    }
    
    private static final long getFreeSpaceInLinux2(File where) throws
	IllegalStateException, IOException 
    {
	PrintWriter writer = null;
	BufferedReader reader = null;
	try {
	    // create a temp file to run the dir command:
	    File script = File.createTempFile("script", "");
	    script.deleteOnExit();
	    writer = new PrintWriter( new FileWriter(script, false), true );
	    writer.println("df -B 1 " + where.getCanonicalPath());
	    writer.close(); // MUST close it at this point, else the Process below will fail to run
	    
	    System.out.println("df -B 1 "+where.getCanonicalPath());
	    // get output from running the script:
	    
	    System.out.println("hi, "+script.getAbsolutePath());
	    Process p0 = Runtime.getRuntime().exec("chmod 777 "+ script.getAbsolutePath() );
	    Process p = Runtime.getRuntime().exec(script.getAbsolutePath());
	    System.out.println("Hi");
	    reader = new BufferedReader( new InputStreamReader(p.getInputStream() ) );
	    String line = null;
	    while (true) {
		line = reader.readLine();
		System.out.println("line="+line);
		if (line == null) 
		    throw new IllegalStateException("Failed to encounter the expected output (a line ending with the text \"free\")while parsing the output of the dir command");
		else if (line.startsWith("/")) break;
	    }
	    // Here are some real examples seen of what line can look like:
	    // dmx.lbl.gov% df -B 1 /tmp/junk
	    // Filesystem            1-blocks      Used Available Use% Mounted on
	    // /dev/hda6            10700922880 6354206720 3803140096  63% /
	    
	    // parse the number (of bytes free) from line:
	    String[] tokens = line.split("\\s+", -1); 
	    
	    String numberText = tokens[3]; 
	    return Long.parseLong(numberText);
	}
	finally {
	    if (writer != null) writer.close();
	    if (reader != null) reader.close();
	}
    }
	
    private static final long getFreeSpaceInWindows(File where) throws
	IllegalStateException, IOException 
    {
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
	    Process p = Runtime.getRuntime().exec( script.getAbsolutePath() );
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
	}
    }
}
