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

import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.util.MyGlobusURL;

import java.util.Date;
import java.util.Vector;
import java.util.Properties;
import java.io.*;

import org.globus.ftp.*;
import org.ietf.jgss.GSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class RemoteHostInfo {
  private String hostName="";
  private int hostPort=2811;
  private String hostPath="";
  private Log logger;
  private java.util.logging.Logger _theLogger;
  private GSSCredential credential; 
  private String surl;
  private boolean recursive;
  private boolean copyCase;
  private boolean tempRecursive;
  private boolean dontList;
  private Vector remoteListings = new Vector();
  private String size;
  private String time;
  private String date;
  private Vector inputVec = new Vector();
  private int maxWait = 3000000;
  private int waitDelay = 20000;
  //default is 30000, and 200 as the wait delay
  

  public RemoteHostInfo (String surl, GSSCredential credential, 
		Log logger, java.util.logging.Logger theLogger, boolean recursive,
		boolean tempRecursive, boolean copyCase, boolean dontList) 
		throws Exception {

    this.surl = surl;
    this.logger = logger;
    this._theLogger = theLogger;
    this.credential = credential;
    this.recursive = recursive;
    this.tempRecursive = tempRecursive;
    this.copyCase = copyCase;
    this.dontList = dontList;

    MyGlobusURL gurl = new MyGlobusURL(surl,0);
    hostName = gurl.getHost();
    hostPort = gurl.getPort();
    hostPath = gurl.getPath();
    
  }

  private void setHost (String name) {
    hostName = name; 
  }

  public String getSURL() {
    return surl;
  }

  public String getSFN() {
    try {
       MyGlobusURL gurl = new MyGlobusURL(surl,0);
       String str = gurl.getPath();
       if(gurl.getProtocol() == null || gurl.getProtocol().equals("")) 
			return surl;
       if(str.startsWith("//")) {
         int i = 0;
         while(true) {
           if(str.charAt(i) != '/') break;
           else i++;
         }
		 return str.substring((i-1));
       }
       else {
		return str;
       }
    }catch(Exception e) {
       return surl;
    }
  }

  private void setPort(String port) {
    try {
      int x = Integer.parseInt(port);
      hostPort = x;
    }catch(NumberFormatException nfe) {
       System.out.println("SRM-CLIENT: Given port is not valid " + port + 
       "\nUsing default port value 2811");
       /*
       inputVec = new Vector(); 
       inputVec.addElement("port=" + port + " is not valid"); 
       inputVec.addElement("Using default port value 2811");
       util.printEventLog(_theLogger,"RemoteHostInfo.setPort",inputVec);
       */
    }
  }

  private void setDir (String path) { 
    hostPath = path;
  }
  
  public String getHost() {
    return hostName;
  }

  public int getPort() {
    return hostPort;
  }

  public String getDir() {
    return hostPath;
  }

  public int doMkdir(String path) throws Exception {

   try {
     GridFTPClient gc = new GridFTPClient(hostName,hostPort);
     gc.authenticate(credential);
     //null credential means, setting default user credential
     //gc.authenticate(null);
     gc.setType(Session.TYPE_ASCII);
     //gc.setClientWaitParams(maxWait,waitDelay);
     gc.changeDir(hostPath);
     System.out.println("ChangeDir ok");
     gc.makeDir(path);
     return 0;
    }catch(Exception e) {
      int idx = e.getMessage().indexOf("File exists");
      if(idx == -1) {
        System.out.println("Exception="+e.getMessage());
        return 1;
      }
      else {
        return 2;
      }
    }
  }
  
  public void doList () throws Exception {
    Vector result = new Vector ();
    String hostPathOrig = hostPath;
    boolean isOnlyFile = false;
    boolean isPermissionDenied=false;
    String onlyFileName="";
    try {
       GridFTPClient gc = new GridFTPClient(hostName,hostPort);
       gc.authenticate(credential);
       //null credential means, setting default user credential
       //gc.authenticate(null);
       gc.setType(Session.TYPE_ASCII);
       //gc.setClientWaitParams(maxWait,waitDelay);
       //setClientWaitParams(maxWait,waitDelay)
       try {
         gc.changeDir(hostPath);
       }catch(Exception e) {
         String msg = e.getMessage();
         int idx = msg.indexOf("Permission");
         if(idx != -1) {
           isPermissionDenied=true;
           System.out.println("SRM-CLIENT: Warning " + e.getMessage());
         }
         else {
           isOnlyFile = true;
           if(tempRecursive && copyCase) {
             //may be user thought the remote path is a directory, but it is a file instead
             System.out.println("SRM-CLIENT : Remote path is not a directory, it is a file");
             System.exit(1);
           }
           recursive = false;
           idx = msg.indexOf("Not a directory");
           if(idx != -1 ) {
             if(hostPath.endsWith("/")) {
                hostPath = hostPath.substring(0,hostPath.length()-1);
             }
             int idx2 = hostPath.lastIndexOf("/");
             if(idx2 != -1) {
               onlyFileName=hostPath.substring(idx2+1);
               hostPath = hostPath.substring(0,idx2);
               gc.changeDir(hostPath);
             }
           }
           else {
             idx = msg.indexOf("No such file or directory");
             if(idx != -1) {
                RemoteFileInfo rInfo = new RemoteFileInfo("gsiftp://"+
                  hostName+":"+hostPort+hostPath);
                rInfo.setErrorMessage("File does not exists");
                remoteListings.addElement(rInfo);
               System.out.println("SRM-CLIENT: Warning " + e.getMessage());
             }
             else {
               throw e;
             }
           }
         }
       }
       gc.setPassive(); 
       gc.setLocalActive();
       String filter = "*";
       if(isOnlyFile) {
         filter = onlyFileName; 
       }
       System.out.println("\nSRM-LS: Ready to list the files "+
		 hostPath+"/"+filter + " " + new Date());

       final ByteArrayOutputStream received = new ByteArrayOutputStream(1000);
       org.globus.ftp.FileInfo fileInfo = null;
       String line = null;
       BufferedReader reader =  null;

       //if(!isOnlyFile) {
         Vector vec = gc.list(filter); 
         String output = received.toString();
         reader = new BufferedReader(new StringReader(received.toString()));


         for(int i = 0; i < vec.size(); i++) {
           //org.globus.ftp.FileInfo ffInfo = (org.globus.ftp.FileInfo)vec.elementAt(i);
           //System.out.println("ffInfo="+ffInfo.toString());
           result.addElement((org.globus.ftp.FileInfo)vec.elementAt(i));
         }
       //}
       /*
       else {
         if(!isPermissionDenied) {
             gc.list(hostPath+"/"+filter,"-d", new DataSink() {
             public void write (Buffer buffer) throws IOException {
                received.write(buffer.getBuffer(), 0, buffer.getLength());
                return;
             }
             public void close () throws IOException {
                if(!dontList) {
                System.out.println("SRM-CLIENT: Listing is done for " + surl);
                System.out.println("SRM-CLIENT: "+new Date());
                } 
                return;
             }
           });
          }
 
          while((line = reader.readLine()) != null) {
           if(line.startsWith("total")) continue;
           try { 
            fileInfo = new org.globus.ftp.FileInfo(line);
           }catch(org.globus.ftp.exception.FTPException e) {
            String str = e.getMessage();
            int idx = str.indexOf("Permission denied");
            if(idx != -1) {
              System.out.println("SRM-CLIENT: WARNING " + e.getMessage()); 
            }
            else {
              throw new Exception(e.getMessage());
            }
           }
           result.addElement(fileInfo);
         }
      }
      */

     }catch(EOFException eof) {
         throw new Exception("EOFException thrown by the gsiftp server.");
     }catch(Exception e) { 
         throw e;
     }

     parseListings(result,isOnlyFile,hostPathOrig); 
     return;
  }

  private void  parseListings(Vector result,boolean isOnlyFile,
		String hostPathOrig) throws Exception {
     Object[] robj = result.toArray();

     for(int j = 0; j < robj.length; j++) {
       org.globus.ftp.FileInfo fInfo = (org.globus.ftp.FileInfo) robj[j];
       if(fInfo.getName().trim().equals(".") ||
         fInfo.getName().trim().equals("..")) {
         if(j == 0 && !dontList && !isOnlyFile) {
           dontList=true;
           //RemoteHostInfo dirInfo = new RemoteHostInfo
			//("gsiftp://"+hostName+":"+hostPort+hostPath,credential,
				//logger,_theLogger,recursive,tempRecursive,copyCase,true);
           //dirInfo.setSize(""+fInfo.getSize());
           //dirInfo.setTime(fInfo.getTime());
           //dirInfo.setDate(fInfo.getDate());
           //System.out.println(">>Calling doList (3)");
           //dirInfo.doList();
           //remoteListings.addElement(dirInfo);
         }
         //;
       }
       else { 
       if(fInfo.isFile() || fInfo.isSoftLink()) {
         RemoteFileInfo rInfo = new RemoteFileInfo("gsiftp://"+
            //hostName+":"+hostPort+"/"+hostPath+"/"+fInfo.getName());
            hostName+":"+hostPort+hostPath+"/"+fInfo.getName());
         rInfo.setSize(""+fInfo.getSize());
         rInfo.setTime(fInfo.getTime());
         rInfo.setDate(fInfo.getDate());
         if(fInfo.isFile()) {
           rInfo.setFile(true);
         }
         else {
           rInfo.setSoftLink(true);
         }
         if(isOnlyFile) {
           //System.out.println("hostPathOrig="+hostPathOrig);
           //System.out.println("hostPath="+hostPath+"/"+fInfo.getName());
           if(hostPathOrig.equals(hostPath+"/"+fInfo.getName())) {
             //if(dontList) {
               remoteListings.addElement(rInfo);
             //}
             break;
           }
         }
         else {
           if(dontList) {
             remoteListings.addElement(rInfo);
           } 
         }
       }
       else if(fInfo.isDirectory()) {

        if(recursive) {
           if(dontList) {
           RemoteHostInfo dirInfo = new RemoteHostInfo
			//("gsiftp://"+hostName+":"+hostPort+"/"+
			("gsiftp://"+hostName+":"+hostPort+hostPath
				+"/"+fInfo.getName(),credential,
					logger,_theLogger,recursive,tempRecursive,copyCase,true);
           dirInfo.setSize(""+fInfo.getSize());
           dirInfo.setTime(""+fInfo.getTime());
           dirInfo.setDate(""+fInfo.getDate());
             dirInfo.doList();
             remoteListings.addElement(dirInfo); 
           }
         }
         else {
           if(!isOnlyFile) {
           if(dontList) {
           RemoteHostInfo dirInfo = new RemoteHostInfo
			("gsiftp://"+hostName+":"+hostPort+"/"+
				hostPath+"/"+fInfo.getName(),credential,
					logger,_theLogger,recursive,recursive,copyCase,true);
           dirInfo.setSize(""+fInfo.getSize());
           dirInfo.setTime(""+fInfo.getTime());
           dirInfo.setDate(""+fInfo.getDate());
             remoteListings.addElement(dirInfo); 
           }
           }
         }
       }
      }
     }//end for
  } 

  public Vector getRemoteListings() {
    return remoteListings;
  }

  public void setSize(String size) {
   this.size = size;
  }

  public String getSize() {
   return size;
  }

  public void setDate(String date) {
   this.date = date;
  }

  public String getDate() {
   return date;
  }

  public void setTime(String time) {
   this.time = time;
  }

  public String getTime() {
   return time;
  }

}
