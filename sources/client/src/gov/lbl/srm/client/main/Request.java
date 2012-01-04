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

import java.util.Vector;
import java.util.Properties;


public class Request {
  private Vector fInfo = new Vector();
  private String _modeType="get";
  private String targetDir="";
  private boolean _modeSet;

  public Request () { 
  }

  public boolean isModeSet() {
    return _modeSet;
  }

  public void setModeType(String type) {
     _modeSet = true;
     _modeType = type;
  }

  public void setTargetDir(String tDir) {
     targetDir = tDir;
  }

  public String getModeType() {
     return _modeType;
  }

  public void addFileInfo(FileInfo fInfo) {
     this.fInfo.add(fInfo);
  }

  public int getTotalFiles () {
    return this.fInfo.size();
  }

  public Vector getFiles () {
   return fInfo;
  }

  public Vector getFileInfo() throws SRMClientException {

   //if source url is same, then the fileinfo is ignored
    int size = this.fInfo.size();

    /*
    Vector tFInfo = new Vector();
    for(int i =0; i < size; i++) {
      FileInfo f1 = (FileInfo)fInfo.elementAt(i);
      String s1="";
      s1 = f1.getSURL();
      boolean same = false;
      for(int j =i+1; j < size; j++) {
        FileInfo f2 = (FileInfo)fInfo.elementAt(j);
        String s2="";
        s2 = f2.getSURL();
        if(s1.equals(s2)) {
          same = true; 
        }
      }
      if(!same) {
       tFInfo.addElement(f1);
      }
    }

    fInfo = tFInfo;
    */

    if(_modeType.equalsIgnoreCase("copy")) {
      //little adjustment for 3partycopy release only the last file if
      //source is same 
      for(int i = 0; i < size; i++) {
        FileInfo f1 = (FileInfo)fInfo.elementAt(i);
        String s1 = f1.getSURL();
        for(int j = i+1; j < size; j++) {
          FileInfo f2 = (FileInfo)fInfo.elementAt(j);
          String s2 = f2.getSURL();
          if(s1.equals(s2)) { 
             f1.doNotReleaseFile(true);
          }
       }
      }
    }

    if(!_modeType.equalsIgnoreCase("get")) { 
      return fInfo;
    }

    //if request is get, then target url are generated from surl path info.

    size = fInfo.size();

    Properties properties = System.getProperties();

    String userName = properties.getProperty("user.name");

    for(int i =0; i < size; i++) {
       FileInfo file = (FileInfo)fInfo.elementAt(i);
       boolean lahfs = file.getLahfs();
       String ss = file.getSURL();
       int idx = ss.lastIndexOf("/");
       if(lahfs) {
         //file.setTURL("/file"+i+"-"+userName);
         file.setTURL("/file"+i);
       }
       else {
         if(idx != -1) {
          //for command line request, overTURL is set false.
          if(file.getOverWriteTURL()) {
            //file.setTURL("file:///"+targetDir+ss.substring(idx)+"-"+userName);
            if(targetDir.startsWith("file:")) {
              file.setTURL(targetDir+ss.substring(idx));
            }
            else {
              //if turl is already given by user in the input file,
              //don't set a new one.
              if(file.getOrigTURL().equals("")) {
                file.setTURL("file:///"+targetDir+ss.substring(idx));
              }
            }
          }
         }
       } 
    }

    for(int i = 0; i < size; i++) {
       FileInfo f1 = (FileInfo)fInfo.elementAt(i);
       String t1 = f1.getTURL(); 
       f1.setTURL(t1);
       String s1 = f1.getSURL();
       for(int j = i+1; j < size; j++) {
         FileInfo f2 = (FileInfo)fInfo.elementAt(j);
         String t2 = f2.getTURL();
         f2.setTURL(t2);
         String s2 = f2.getSURL();
         if(s1.equals(s2)) { 
            f1.doNotReleaseFile(true);
         }
         if(t1.equals(t2)) {
            f2.setTURL(t2+"-"+System.currentTimeMillis());
         }
       }
    }
    return fInfo;
  }
}
