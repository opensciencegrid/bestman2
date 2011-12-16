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

import java.util.Vector;
import java.util.Properties;
import java.util.Date;
import java.text.*;
import java.io.*;

import org.globus.ftp.*;
import org.ietf.jgss.GSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class LocalListInfo {
  private String localPath="";
  private Log logger;
  private java.util.logging.Logger _theLogger;
  private String surl;
  private boolean recursive;
  private Vector remoteListings = new Vector();
  private String size;
  private String time;
  private String date;
  private Vector inputVec = new Vector();
  private boolean dontList;
  

  public LocalListInfo (String surl, Log logger, 
		java.util.logging.Logger theLogger, boolean recursive,boolean dontList) 
		throws Exception {

    this.surl = surl;
    this.logger = logger;
    this._theLogger = theLogger;
    this.recursive = recursive;
    this.dontList = dontList;
    localPath = surl;
  }

  public void setSURL(String surl) {
    this.surl = surl;
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


  private void setDir (String path) { 
    localPath = path;
  }
  
  public String getDir() {
    return localPath;
  }
  
  public void doList () throws Exception {
    Vector result = new Vector ();
    String hostPathOrig = localPath;
    boolean isOnlyFile = false;
    try {
     String temp = util.parseLocalSourceFileForPath (localPath);
     File f = new File(temp);
     if(!f.exists()) {
       LocalFileInfo fInfo = new LocalFileInfo(localPath); 
       if(!f.isDirectory()) {
         fInfo.setFile(true); 
       }
       else {
         fInfo.setDirectory(true); 
       }
       fInfo.setErrorMessage("File does not exists");
       result.addElement(fInfo);    
       System.out.println("\nSRM-CLIENT: Exception: File does not exists " + 
                  temp);
     }
     else {
     if(!f.canRead()) {
       LocalFileInfo fInfo = new LocalFileInfo(localPath); 
       if(!f.isDirectory()) {
         fInfo.setFile(true); 
       }
       else {
         fInfo.setDirectory(true); 
       }
       fInfo.setErrorMessage("Cannot read the given file, no permission.");
       result.addElement(fInfo);    
     }
     else {
       if(!f.isDirectory()) {
          isOnlyFile = true;
          LocalFileInfo fInfo = new LocalFileInfo(localPath);
          fInfo.setSize(""+f.length());
          fInfo.setFile(true); 
          DateFormat dataformat =  DateFormat.getDateInstance(DateFormat.LONG);
          long dd = f.lastModified();
          Date date = new Date(dd);
          fInfo.setDate(dataformat.format(date));
          result.addElement(fInfo);
          if(!f.exists()) {
            fInfo.setErrorMessage("File does not exists");
          }
       }
       else {
         File[] lists = f.listFiles();
         for(int k = 0; k < lists.length; k++) {
            File ff = lists[k]; 
            LocalFileInfo fInfo = new LocalFileInfo(ff.getAbsolutePath());
            fInfo.setSize(""+ff.length());
            if(!ff.isDirectory()) {
              fInfo.setFile(true); 
            }
            else {
              fInfo.setDirectory(true); 
            }
            DateFormat dataformat =  
				DateFormat.getDateInstance(DateFormat.LONG);
            long dd = ff.lastModified();
            Date date = new Date(dd);
            //String s4 = dataformat.format(date);
            //fInfo.setDate(s4);
            fInfo.setDate(date.toString());
            result.addElement(fInfo);
         }
       }
      }
     }
     }catch(Exception e) { 
        System.out.println("Exception="+e.getMessage());
        throw e;
     }

     parseListings(result,hostPathOrig); 
     return;
  }

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseListings
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

  private void  parseListings(Vector result, String hostPathOrig) throws Exception {
     Object[] robj = result.toArray();

     boolean plainFileIsGiven = false; 
     
     if(!dontList) {
        
       String temp = util.parseLocalSourceFileForPath (surl);
       File f = new File(temp);
       if(f.isDirectory()) {
           LocalListInfo ddirInfo = new LocalListInfo
			(surl,logger,_theLogger,recursive,true);
           ddirInfo.doList();
           ddirInfo.setSize(""+f.length());
           long dd = f.lastModified();
           Date date = new Date(dd);
           ddirInfo.setDate(""+date.toString());
           remoteListings.addElement(ddirInfo);
       }
       else {
          plainFileIsGiven=true;
       }
     }

     for(int j = 0; j < robj.length; j++) {
       LocalFileInfo lFInfo = (LocalFileInfo) robj[j];
       if(lFInfo.isFile() || lFInfo.isSoftLink()) {
         RemoteFileInfo rInfo = new RemoteFileInfo(lFInfo.getSURL());
         rInfo.setSize(""+lFInfo.getSize());
         rInfo.setTime(lFInfo.getTime());
         rInfo.setDate(lFInfo.getDate());
         rInfo.setErrorMessage(lFInfo.getErrorMessage());
         if(lFInfo.isFile()) {
           rInfo.setFile(true);
         }
         else {
           rInfo.setSoftLink(true);
         }
         if(dontList || plainFileIsGiven) {
          remoteListings.addElement(rInfo);
         } 
       }
       else if(lFInfo.isDirectory()) {
        
        /*
        RemoteFileInfo rInfo = new RemoteFileInfo(lFInfo.getSURL());
        rInfo.setSize(""+lFInfo.getSize());
        rInfo.setTime(lFInfo.getTime());
        rInfo.setDate(lFInfo.getDate());
        rInfo.setErrorMessage(lFInfo.getErrorMessage());
        remoteListings.addElement(rInfo);
        */
       
        if(recursive) {
         if(dontList) {
           LocalListInfo dirInfo = new LocalListInfo
			(lFInfo.getSURL(),logger,_theLogger,recursive,true);
           dirInfo.setSize(""+lFInfo.getSize());
           dirInfo.setTime(""+lFInfo.getTime());
           dirInfo.setDate(""+lFInfo.getDate());
           dirInfo.doList();
           remoteListings.addElement(dirInfo); 
          }
           //Vector rListings = dirInfo.getLocalListings();
           //for(int k = 0; k < rListings.size(); k++) {
             //remoteListings.addElement(rListings.elementAt(k)); 
           //}
        }
        else {
          if(dontList) {
           LocalListInfo dirInfo = new LocalListInfo
			(lFInfo.getSURL(),logger,_theLogger,recursive,true);
           dirInfo.setSize(""+lFInfo.getSize());
           dirInfo.setTime(""+lFInfo.getTime());
           dirInfo.setDate(""+lFInfo.getDate());
           remoteListings.addElement(dirInfo); 
          }
        }
       }
     }//end for
  } 

  public Vector getLocalListings() {
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
