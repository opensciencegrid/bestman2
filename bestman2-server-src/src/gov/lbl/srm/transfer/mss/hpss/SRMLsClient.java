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


package gov.lbl.srm.transfer.mss.hpss;

//import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.transfer.mss.*;

import java.util.Vector;
import java.util.Properties;
import java.util.Date;
import java.io.*;

import org.globus.ftp.*;
import org.ietf.jgss.GSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SRMLsClient {
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
  private Vector remoteListings = new Vector();
  private String size;
  private String time;
  private String date;
  private Vector inputVec = new Vector();
  private SRM_PATH srmPath = new SRM_PATH();
  private int debugLevel;
  private int maxWait = 3000000;
  private int waitDelay = 20000;
  
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//SRMLsClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMLsClient (String surl, GSSCredential credential, 
	Log logger, java.util.logging.Logger theLogger, 
	boolean recursive, int debugLevel)
	throws Exception {

  this.surl = surl;
  this.logger = logger;
  this._theLogger = theLogger;
  this.credential = credential;
  this.recursive = recursive;
  this.debugLevel = debugLevel;

  MyGlobusURL gurl = new MyGlobusURL(surl,0);
  hostName = gurl.getHost();
  hostPort = gurl.getPort();
  hostPath = gurl.getPath();
}

public void setMaxWait(int maxWait) {
  this.maxWait = maxWait;
}

public void setWaitDelay(int waitDelay) {
  this.waitDelay = waitDelay;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getSURL
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getSURL() {
  return surl;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getHost
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getHost() {
  return hostName;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getPort
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public int getPort() {
  return hostPort;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getPath
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getPath() {
  return hostPath;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//printSrmPath
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printSrmPath(SRM_PATH srmPath) {
  System.out.println("Directory Name="+srmPath.getDir());
  Vector files = srmPath.getFids();
  for(int i = 0; i < files.size(); i++) {
     SRM_FILE ff = (SRM_FILE) files.elementAt(i);
     System.out.println("File Name="+ff.getFile());
  }
  Vector paths = srmPath.getSubPath();
  for(int j = 0; j < paths.size(); j++) {
      printSrmPath((SRM_PATH)paths.elementAt(j));
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//deleteSrmPath
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void deleteSrmPath(SRM_PATH srmPath, GridFTPClient gc) 
 throws Exception {

  Vector files = srmPath.getFids();
  for(int i = 0; i < files.size(); i++) {
     SRM_FILE ff = (SRM_FILE) files.elementAt(i);
     gc.deleteFile(srmPath.getDir()+"/"+ff.getFile());

  }
  Vector paths = srmPath.getSubPath();
  for(int j = 0; j < paths.size(); j++) {
      deleteSrmPath((SRM_PATH)paths.elementAt(j),gc);
  }
  gc.deleteDir(srmPath.getDir());
  System.out.println("Deleted directory " + srmPath.getDir());
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doDeleteDir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doDeleteDir(String path, 
	boolean directory, boolean recursive, MSS_MESSAGE_W_E mssresult) 
		throws Exception {

 GridFTPClient gc = null; 
 if(directory) {
    boolean value = doList(path,"",false,recursive,mssresult,false);
    if(!value) {
      //exception = lsresult.getExplanation();
      return false;
    }
    else {
     try {
      System.out.println("Before here");
      gc = new GridFTPClient(hostName,hostPort);
      System.out.println("after here");
      gc.authenticate(credential);
      gc.setClientWaitParams(maxWait,waitDelay); 
      System.out.println("after here again");
      //null credential means, setting default user credential
      //gc.authenticate(null);
      gc.setType(Session.TYPE_ASCII);
      System.out.println("after here again again");
      Vector paths = srmPath.getSubPath();
      Vector files = srmPath.getFids();
      if(!recursive ) {
		if((paths.size() > 0 || files.size() > 0)) {
          mssresult.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR); 
          mssresult.setExplanation(
			"Cannot delete an non-empty directory, use recursive option"); 
          if(gc != null) {
            gc.close(true);
          }
          return false;
         }
         else {
           try {
             deleteSrmPath(srmPath,gc); 
              mssresult.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE); 
              mssresult.setExplanation("Directory deleted from MSS");
              if(gc != null) {
                gc.close(true);
              }
              return true;
           }catch(Exception ee) {
              mssresult.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR); 
              mssresult.setExplanation(ee.getMessage());
              if(gc  != null) {
                gc.close(true);
              }
              return false;
           }
         }
      } 
      else {
        try {
          deleteSrmPath(srmPath,gc); 
          mssresult.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE); 
          mssresult.setExplanation("Directory deleted from MSS");
          if(gc != null) {
            gc.close(true);
          }
          return true;
        }catch(Exception ee) {
           mssresult.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR); 
           mssresult.setExplanation(ee.getMessage());
           if(gc != null) {
             gc.close(true);
           }
           return false;
        }
      }
     }catch(Exception se) {
        se.printStackTrace();
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR); 
        mssresult.setExplanation(se.getMessage());
        if(gc != null) {
          gc.close(true);
        }
        return false;
     }
    }
 } 
 else {
    try {
       gc = new GridFTPClient(hostName,hostPort);
       gc.authenticate(credential);
       gc.setClientWaitParams(maxWait,waitDelay); 
       //null credential means, setting default user credential
       //gc.authenticate(null);
       gc.setType(Session.TYPE_ASCII);
       int idx = path.lastIndexOf("/");
       if(idx != -1) {
         path = path.substring(idx+1);
       }
       idx = hostPath.lastIndexOf("/");
       if(idx != -1) {
         hostPath = hostPath.substring(0,idx);
       }
       gc.changeDir(hostPath);
       System.out.println("ChangeDir ok " + hostPath);
       gc.deleteFile(path);
       System.out.println("DeleteFile ok " + path);
       mssresult.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE); 
       mssresult.setExplanation("File deleted from MSS");
       if(gc != null) {
         gc.close(true);
       }
       return true;
     }catch(Exception e) {
        mssresult.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR); 
        mssresult.setExplanation(e.getMessage());
        if(gc != null) {
          gc.close(true);
        }
        return false;
     }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String doMkdir(String path) throws Exception {

 String exception = "";
 GridFTPClient gc = null;
 try {
   gc = new GridFTPClient(hostName,hostPort);
   gc.authenticate(credential);
   gc.setClientWaitParams(maxWait,waitDelay); 
   //null credential means, setting default user credential
   //gc.authenticate(null);
   gc.setType(Session.TYPE_ASCII);
   System.out.println("hostpath="+hostPath);
   gc.changeDir(hostPath);
   System.out.println("ChangeDir ok");
   System.out.println("Path="+path);
   gc.makeDir(path);
   if(gc != null) {
     gc.close(true);
   }
   return exception;
  }catch(Exception e) {
     if(gc != null) {
       gc.close(true);
     }
     return e.getMessage();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//lastModified
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Date lastModified() throws Exception {
   GridFTPClient gc = new GridFTPClient(hostName,hostPort);
   gc.authenticate(credential);
   gc.setClientWaitParams(maxWait,waitDelay); 
   //null credential means, setting default user credential
   //gc.authenticate(null);
   gc.setType(Session.TYPE_ASCII);
   Date date =  gc.lastModified(hostPath);
   if(gc != null) {
     gc.close(true);
   }
   return date;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getHomeDir
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getHomeDir(MSS_MESSAGE_W_E mssmessage) 
		throws Exception {

 GridFTPClient gc = null;
 try {
  
   gc = new GridFTPClient(hostName,hostPort);
   gc.authenticate(credential);
   gc.setClientWaitParams(maxWait,waitDelay); 
   //null credential means, setting default user credential
   //gc.authenticate(null);
   gc.setType(Session.TYPE_ASCII);
   String str = gc.getCurrentDir();
   mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
   mssmessage.setExplanation("Got HomeDir successfully.");
   if(gc != null) {
     gc.close(true);
   }
   return str;
 }catch(Exception e) {
   setErrorMessage(e.getMessage(),mssmessage);
   if(gc != null) {
     gc.close(true);
   }
   return "";
 }
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getRemoteFileSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public long getRemoteFileSize(MSS_MESSAGE_W_E mssmessage) 
		throws Exception {
 GridFTPClient gc = null;
 try {

   gc = new GridFTPClient(hostName,hostPort);
   gc.authenticate(credential);
   gc.setClientWaitParams(maxWait,waitDelay); 
   //null credential means, setting default user credential
   //gc.authenticate(null);
   gc.setType(Session.TYPE_ASCII);
   String str = gc.getCurrentDir();
   try {
     gc.changeDir(hostPath);
     mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
     mssmessage.setExplanation("Given path is a directory");
     if(gc != null) {
       gc.close(true);
     }
     return -1;
   }
   catch(Exception ee) {
     int idx = ee.getMessage().indexOf("Not a directory");
     int idx2 = ee.getMessage().indexOf("Could not change directory");
     if(idx != -1 || idx2 != -1) {
       long size =  gc.getSize(hostPath);
       mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
       mssmessage.setExplanation("Got file size successfully.");
       if(gc != null) {
          gc.close(true);
       }
       return size;
     }
     else {
       setErrorMessage(ee.getMessage(),mssmessage);
       if(gc != null) {
          gc.close(true);
       }
       return -1;
     }
  }
 }catch(Exception e) {
   setErrorMessage(e.getMessage(),mssmessage);
   if(gc != null) {
     gc.close(true);
   }
   return -1;
 }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doList
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doList(String fileName, String requestToken,boolean preCheck, 
		boolean recursive, MSS_MESSAGE_W_E mssmessage, boolean plainFileOnly) 
		throws Exception {

 Vector result = new Vector ();
 GridFTPClient gc = null;
 try {
   gc = new GridFTPClient(hostName, hostPort);
   gc.authenticate (credential);
   gc.setClientWaitParams(maxWait,waitDelay); 
   //null credential means, setting default user credential
   //gc.authenticate(null);
   gc.setType(Session.TYPE_ASCII);
   System.out.println(">>> HOSTPATH="+hostPath + " " + 
   		preCheck + " " + fileName);
   gc.changeDir(hostPath);

   if(preCheck) {
     mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_IS_A_DIRECTORY);
     mssmessage.setExplanation("Given path is a directory");
     if(gc != null) {
       gc.close(true);
     }
     return false;
   }
   else {
     gc.setPassive();
     gc.setLocalActive();
     final ByteArrayOutputStream received = new ByteArrayOutputStream(1000);
     String filter = "*";
     if(plainFileOnly) {
       filter=fileName;
     }
     gc.list(filter,"-d", new DataSink() {
        public void write(Buffer buffer) throws IOException {
           received.write(buffer.getBuffer(),0,buffer.getLength());
        }
        public void close() throws IOException {
            System.out.println("SRM-CLIENT: Listing is done.");
        }
     }); 
     String output = received.toString();
     BufferedReader reader = new BufferedReader(
			new StringReader(received.toString()));
     org.globus.ftp.FileInfo fileInfo = null;
     String line = null;
     while((line = reader.readLine()) != null) {
        if(line.startsWith("total")) continue;
        try {
          fileInfo = new org.globus.ftp.FileInfo (line);
        }catch(org.globus.ftp.exception.FTPException e) {
           System.out.println("\nSRM-CLIENT:Exception="+e.getMessage());
        }
        result.addElement(fileInfo);
     }
   }
   parseListings(result,requestToken,recursive,srmPath);
   mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
   mssmessage.setExplanation("MSS listing is done successfully.");
   if(gc != null) {
     gc.close(true);
   }
   return true;
  }catch(Exception e) {
     e.printStackTrace();
     String msg = e.getMessage();
     boolean returnValue = setErrorMessage(msg,mssmessage);
     if(gc != null) {
       gc.close(true);
     }
     return returnValue;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setErrorMessage
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean setErrorMessage(String msg, MSS_MESSAGE_W_E mssmessage) {

  int idx = msg.indexOf("Permission");
  int idx1 = msg.indexOf("Not a directory");
  int idx2 = msg.indexOf("No such file or directory");
  int idx3 = msg.indexOf("hpssex_OpenConnection: unable to obtain remote site info");
  int idx4 = msg.indexOf("Unable to setup communication to HPSS");
  int idx5 = msg.indexOf("Connection refused");
  int idx6 = msg.indexOf("Reply wait timeout");
  int idx7 = msg.indexOf("Bad password");
  int idx8 = msg.indexOf("Login incorrect");
  int idx9 = msg.indexOf("IPC connection failed");
  int idx10 = msg.indexOf("Couldn't do ssl handshake");
  int idx11 = msg.indexOf("unable to verify remote side's credentials");
  //System.out.println("idx="+idx7 + " " + idx8 + " " + idx9 + " " + idx10 + " " + idx11);
  if(debugLevel >= 4000) {
    System.out.println("Exception from MSS="+msg);
  }
  if(idx != -1) {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_AUTHENTICATION_FAILED);
    mssmessage.setExplanation("No Permission");
    return false;
  }
  else if(idx7 != -1 || idx8 != -1 || idx9 != -1 || idx10 != -1  
		|| idx11 !=-1) {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_BAD_PROXY);
    mssmessage.setExplanation("Proxy Error.");
    return false;
  } 
  else if(idx1 != -1) {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_IS_NOT_A_DIRECTORY);
    mssmessage.setExplanation("Given path is not a directory");
    return true;
  }
  else if(idx2 != -1) {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_NO_SUCH_PATH);
    mssmessage.setExplanation("No such file or directory");
    return false;
  }
  else if(idx3 != -1 || idx4 != -1 || idx5 != -1) {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_MSS_ERROR);
    mssmessage.setExplanation(msg);
    return false;
  }
  else {
    mssmessage.setStatus(MSS_MESSAGE.SRM_MSS_UNKNOWN_ERROR);
    mssmessage.setExplanation(msg);
    return false;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseListings
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void parseListings (Vector result, String requestToken,
	boolean recursive, SRM_PATH sPath) throws Exception {

  Object[] robj = result.toArray ();

  sPath.setDir(hostPath); 
  sPath.isDir(true);
  for( int j = 0; j < robj.length; j++) {
    org.globus.ftp.FileInfo fInfo = (org.globus.ftp.FileInfo) robj[j];
    if(fInfo.getName().trim().equals(".") ||
       fInfo.getName().trim().equals("..")) { ; }
    else {
       if(fInfo.isFile() || fInfo.isSoftLink()) {
         SRM_FILE srmFile = new SRM_FILE();
         srmFile.setFile(fInfo.getName().trim());
         srmFile.setRequestToken(requestToken);
         srmFile.setSize(fInfo.getSize());
         srmFile.setTimeStamp(fInfo.getDate()+" " + fInfo.getTime());
         sPath.setFids(srmFile);
       }
       else {
          MSS_MESSAGE_W_E mssresult = new MSS_MESSAGE_W_E();
          SRMLsClient srmLsClient = new SRMLsClient
				("gsiftp://"+hostName+":"+hostPort+hostPath+"/"+fInfo.getName(),
                 credential, logger, _theLogger,recursive,debugLevel);
          if(recursive) {
            boolean value = srmLsClient.doList(hostPath+"/"+fInfo.getName(),
			"",false,true,mssresult,false);
          }
          else {
             mssresult.setStatus(MSS_MESSAGE.SRM_MSS_TRANSFER_DONE);
             mssresult.setExplanation("MSS listing is done.");
          }
          SRM_PATH subPath = srmLsClient.getSrmPath();
          subPath.setDir(hostPath+"/"+fInfo.getName());
          subPath.isDir(true);
          subPath.setStatus(mssresult.getStatus());
          subPath.setExplanation(mssresult.getExplanation());
          sPath.setSubPath(subPath);
       }
    }
  }//end for
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getSrmPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_PATH getSrmPath() {
  return srmPath;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getRemoteListings
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Vector getRemoteListings() {
 return remoteListings;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setSize(String size) {
 this.size = size;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getSize() {
 return size;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setDate
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setDate(String date) {
 this.date = date;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getDate
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getDate() {
 return date;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setTime
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTime(String time) {
 this.time = time;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getTime
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getTime() {
 return time;
}

}
