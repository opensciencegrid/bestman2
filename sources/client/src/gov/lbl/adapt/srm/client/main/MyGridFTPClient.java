/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/


package gov.lbl.adapt.srm.client.main;

import java.util.Vector;
import java.util.Properties;
import java.util.Date;
import java.io.*;

import org.globus.ftp.*;
import org.ietf.jgss.GSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.adapt.srm.client.util.MyGlobusURL;


public class MyGridFTPClient {
  private String hostName="";
  private int hostPort=2811;
  private String hostPath="";
  private Log logger;
  private java.util.logging.Logger _theLogger;
  private GSSCredential credential; 
  private String surl;
  private boolean recursive;
  private Vector inputVec = new Vector();
  private SRM_PATH srmPath = new SRM_PATH();
  private int maxWait = 3000000;
  private int waitDelay = 20000;
  //default is 30000, and 200
  private GridFTPClient gc = null;
  
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//MyGridFTPClient --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public MyGridFTPClient (String surl, GSSCredential credential, 
	Log logger, java.util.logging.Logger theLogger, 
	boolean recursive)
	throws Exception {

  this.surl = surl;
  this.logger = logger;
  this._theLogger = theLogger;
  this.credential = credential;
  this.recursive = recursive;

  System.out.println(".....surl="+surl);
  MyGlobusURL gurl = new MyGlobusURL(surl,0);
  hostName = gurl.getHost();
  hostPort = gurl.getPort();
  hostPath = gurl.getPath();

  try {
     gc = new GridFTPClient(hostName, hostPort);
     gc.authenticate (credential);
     //null credential means, setting default user credential
     //gc.authenticate(null);
     //gc.setClientWaitParams(maxWait,waitDelay);
     gc.setType(Session.TYPE_ASCII);
     gc.changeDir(hostPath);

     gc.setPassive();
   }catch(Exception e) { throw e; }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doDeleteDir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doDeleteDir(String path, 
	boolean directory, boolean recursive) throws Exception {

 if(directory) {
    boolean value = doList(path,recursive);
    if(!value) {
      return false;
    }
    else {
      try {
         //if this is not initialized here again, socket closed error is coming.
         gc = new GridFTPClient(hostName,hostPort);
         gc.authenticate(credential);
         //null credential means, setting default user credential
         //gc.authenticate(null);
         gc.setType(Session.TYPE_ASCII);  
         //gc.setClientWaitParams(maxWait,waitDelay);
         Vector paths = srmPath.getSubPath();
         Vector files = srmPath.getFids();
         if(!recursive) {
           if(paths.size() > 0 || files.size() > 0) {
              System.out.println("\nSRM-DIR: Directory not empty, please use -recursive option");
              if(gc != null) {
                gc.close(true);
              }
              return false;
           }
           else {
              try {
                deleteSrmPath(srmPath,gc);
                System.out.println("\nSRM-DIR: Directory deleted.");
                if(gc != null) {
                    gc.close();
                }
                return true;
              }catch(Exception ee) { 
                 System.out.println("\nSRM-DIR: Exception="+ee.getMessage());
                 if(gc != null) {
                    gc.close();
                 }
                 return false;
              }
           }
         }//end (if not recursive)
         else {
            try {
               deleteSrmPath(srmPath,gc);
               if(gc != null) {
                  gc.close(true);
               }
               return true;
            }catch(Exception ee) {
              System.out.println("\nSRM-DIR: Exception="+ee.getMessage());
              if(gc != null) {
                 gc.close(true);
              }
              return false;
            }
         }
      }catch(Exception se) {
         System.out.println("\nSRM-DIR: Exception="+se.getMessage());
         if(gc != null) {  
           gc.close(true);
         }
         return false;
      }
    }
 }
 else {
    gc.changeDir(hostPath);
    System.out.println("ChangeDir ok " + hostPath);
    gc.deleteFile(path);
    try {
       System.out.println("DeleteFile ok " + path);
       if(gc != null) {
         gc.close(true);
       }
       return true;
     }catch(Exception e) {
        if(gc != null) {
          gc.close(true);
        }
        return false;
     }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//getSrmPath
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRM_PATH getSrmPath() {
  return srmPath;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doMv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doMv(String source, String target) throws Exception {

 try {
   //gc.changeDir(hostPath);
   //System.out.println("ChangeDir ok");
   System.out.println("Source="+source);
   System.out.println("target="+target);
   gc.rename(source,target);
   //gc.rename("/tmp/gridftpdir1/gridftpdir2/hello.java",
				//"/tmp/gridftpdir1/hello.java.2");
   if(gc != null) {
     gc.close(true);
   }
   return true;
  }catch(Exception e) {
     if(gc != null) {
       gc.close(true);
     }
     return setErrorMessage(e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doMkdir(String path) throws Exception {

 try {
   gc.changeDir(hostPath);
   System.out.println("ChangeDir ok");
   gc.makeDir(path);
   if(gc != null) {
     gc.close(true);
   }
   return true;
  }catch(Exception e) {
     if(gc != null) {
       gc.close(true);
     }
     return setErrorMessage(e.getMessage());
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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//doList
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean doList(String fileName, boolean recursive)
        throws Exception {
 
 if(recursive) {
   throw new Exception("Recursive not supported");
 }
 Vector result = new Vector ();
  System.out.println("HOSTPATH="+hostPath + " " + fileName);
 try {
     gc.changeDir(hostPath);

     gc.setPassive();
     gc.setLocalActive();
     String filter="*";
    /* 
    try {
       gc.changeDir(hostPath+"/"+fileName);
    }catch(Exception de) {
        gc.changeDir(hostPath);
        filter=fileName;
    }
    */
    //final ByteArrayOutputStream received = new ByteArrayOutputStream(1000);
    /*
    gc.list(filter,"-d", new DataSink() {
        public void write(Buffer buffer) throws IOException {
           received.write(buffer.getBuffer(),0,buffer.getLength());
        }
        public void close() throws IOException {
            System.out.println("SRM-CLIENT: Listing is done.");
        }
     });
    */

    //String output = received.toString();
    /*
    BufferedReader reader = new BufferedReader(
            new StringReader(received.toString()));
    org.globus.ftp.FileInfo fileInfo = null;
    String line = null;
    while((line = reader.readLine()) != null) {
        if(line.startsWith("total")) continue;
        try {
          fileInfo = new org.globus.ftp.FileInfo (line);
        }catch(org.globus.ftp.exception.FTPException e) {
           System.out.println("\nSRM-DIR:Exception="+e.getMessage());
        }
        result.addElement(fileInfo);
     }
    */

    Vector vec = gc.list(filter);
    for(int i = 0; i < vec.size(); i++) {
       result.addElement((org.globus.ftp.FileInfo)vec.elementAt(i));
    }
    parseListings(result,recursive,srmPath);
    if(gc != null) {
     gc.close(true);
   }
   return true;
  }catch(Exception e) {
     //e.printStackTrace();
     String msg = e.getMessage();
     if(gc != null) {
       gc.close(true);
     }
     return setErrorMessage(msg);
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseListings
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void parseListings (Vector result, boolean recursive, 
	SRM_PATH sPath) throws Exception {

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
         //srmFile.setSize(fInfo.getSize());
         //srmFile.setTimeStamp(fInfo.getDate()+" " + fInfo.getTime());
         sPath.setFids(srmFile);
       }
       else {
          //MyGridFTPClient srmLsClient = new MyGridFTPClient
                //("gsiftp://"+hostName+":"+hostPort+hostPath+"/"+fInfo.getName(),
                 //credential, logger, _theLogger,recursive);
          //hostPath = hostPath+"/"+fInfo.getName(); 
          if(recursive) {
            //boolean value = 
            	//srmLsClient.doList(hostPath+"/"+fInfo.getName(),true);
            hostPath=hostPath+"/"+fInfo.getName();
            boolean value = 
				doList("gsiftp://"+hostName+":"+hostPort+hostPath,true);
          }
          else {
              System.out.println("Listing done.");
          }
          //SRM_PATH subPath = srmLsClient.getSrmPath();
          SRM_PATH subPath = getSrmPath();
          subPath.setDir(hostPath+"/"+fInfo.getName());
          subPath.isDir(true);
          sPath.setSubPath(subPath);
       }
    }
  }//end for 
}



//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setErrorMessage
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private boolean setErrorMessage(String msg) {

  if(msg.equals("")) return true;
  int idx = msg.indexOf("Permission");
  int idx1 = msg.indexOf("Not a directory");
  int idx2 = msg.indexOf("No such file or directory");
  int idx3 = msg.indexOf("Connection refused");
  System.out.println("Exception from GsiFTPServer="+msg);
  if(idx != -1) {
    return false;
  }
  else if(idx1 != -1) {
    return true;
  }
  else if(idx2 != -1) {
    return false;
  }
  else if(idx3 != -1) {
    return false;
  }
  else {
    return false;
  }
}
}
