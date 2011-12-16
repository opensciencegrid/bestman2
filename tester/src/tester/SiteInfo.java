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

import java.util.*;

public class SiteInfo {
  private int siteId;
  private int serviceId;
  private String command="";
  private String protocol="";
  private String hostName="";
  private int port;
  private String rootDir="";
  private String servicePath="/srm/managerv2";
  private String pathInfo="";
  private String removeInfo="";
  private String putOverwrite="true";
  private String copyOverwrite="true";
  private String dirInfo="";
  //private String copySources="";
  private String disableInfo="";
  private String location="";

  public SiteInfo (String line) throws Exception {
    int idx = line.indexOf("@");
    String temp = line;
    if(idx != -1) {
       location = line.substring(0,idx); 
    }
    else {
      temp = line.substring(idx+1);
    }
    idx = temp.indexOf("?SFN=");
    idx = temp.indexOf("srm://");
    if(idx != -1) {
      protocol = "srm";
      String tt = temp.substring(idx+5);
      idx = tt.indexOf(":");
      if(idx != -1) {
         hostName = tt.substring(1,idx);
         int idx2 = tt.indexOf("/",idx);
         if(idx2 != -1) {
           String aaa = tt.substring(idx+1,idx2);
           try {
             port = Integer.parseInt(aaa);
           }catch(NumberFormatException nfe) {}
           int idx3 = tt.indexOf("?SFN=");
           if(idx3 != -1) {
             servicePath = tt.substring(idx2,idx3);
             String bbb = tt.substring(idx3+5);
             int idx4 = bbb.lastIndexOf("/");
             if(idx4 != -1) {
               rootDir = bbb.substring(0,idx4);
               pathInfo = bbb.substring(idx4+1);
             }
             else {
               throw new Exception("Given siteInfo is not in the correct format");
             }
           }
           else {
             throw new Exception("Given siteInfo is not in the correct format");
           }
         }
         else {
           throw new Exception("Given siteInfo is not in the correct format");
         }
      }
      else {
        throw new Exception("Given siteInfo is not in the correct format");
      }
    }
    else {
      throw new Exception("Given siteInfo is not in the correct format");
    }
  }
  /*
  public SiteInfo(String line) {
    StringTokenizer st = new StringTokenizer(line);
    while (st.hasMoreTokens()) {
       String temp = st.nextToken();
       int idx = temp.indexOf("="); 
	   if(idx != -1) {
         String name = temp.substring(0,idx);
         String value = temp.substring(idx+1);
         if(name.equals("siteid")) {
           try {
            siteId = Integer.parseInt(value);
           }catch(NumberFormatException nfe) {}
         }
         else if(name.equals("serviceid")) {
           try{
            serviceId = Integer.parseInt(value);
           }catch(NumberFormatException nfe) {}
         }
         else if(name.equals("location")) {
           location = value;  
         }
         else if(name.equals("command")) {
            command = value;
         }
         else if(name.equals("protocol")) {
            protocol = value;
         }
         else if(name.equals("host")) {
            hostName = value;
         }
         else if(name.equals("port")) {
           try{
            port = Integer.parseInt(value);
           }catch(NumberFormatException nfe) {
              System.out.println("\nPort number is not valid : " + port);
		   }
         }
         else if(name.equals("rootdir")) {
            rootDir = value;
         }
         else if(name.equals("servicepath")) {
            servicePath = value;
         }
         else if(name.equals("pathinfo")) {
            pathInfo = value;
         }
         else if(name.equals("removeinfo")) {
            removeInfo = value;
         }
         else if(name.equals("dirinfo")) {
            dirInfo = value;
         }
         else if(name.equals("putoverwrite")) {
            putOverwrite = value;
         }
         else if(name.equals("copyoverwrite")) {
            copyOverwrite = value;
         }
         //else if(name.equals("copysource") || name.equals("copysources")) {
            //copySources = value;
         //}
         else if(name.equals("disable")) {
            disableInfo = value;
            disableInfo=disableInfo.toUpperCase(); 
         }
       }
    } 
  }
  */

  public int getSiteId() {
    return siteId;
  }
  public int getServiceId() {
    return serviceId;
  }
  public String getCommand() {
    return command;
  }
  public String getProtocol() {
    return protocol;
  }
  public String getLocation() {
    return location;
  }
  public String getHost() {
    return hostName;
  }
  public int getPort() {
    return port;
  }
  public String getRootDir() {
    return rootDir;
  }
  public String getServicePath() {
    return servicePath;
  }
  public String getPathInfo() {
    return pathInfo;
  }
  public String getRemoveInfo() {
    return removeInfo;
  }
  public String getDirInfo() {
    return dirInfo;
  }

  public void setPutOverwrite(String s) {
    putOverwrite = s;
  }

  public void setCopyOverwrite(String s) {
    copyOverwrite = s;
  }

  public boolean getPutOverwrite() {
    Boolean b = new Boolean(putOverwrite);
    return b.booleanValue();
  }

  public boolean getCopyOverwrite() {
    Boolean b = new Boolean(copyOverwrite);
    return b.booleanValue();
  }

  public boolean getDisableAll() {
    int idx = disableInfo.indexOf("ALLOP");
    if(idx != -1) {
      return true;
    }
    return false;
  }

  public boolean getDisableBringOnline() {
    int idx = disableInfo.indexOf("BRINGONLINE");
    if(idx != -1) {
      return true;
    }
    return false;
  }

  public boolean getDisableCopyAll() {
    int idx = disableInfo.indexOf("COPYALL");
    if(idx != -1) {
      return true;
    }
    return false;
  }

  public boolean getDisableCopyGsiFTP() {
    int idx = disableInfo.indexOf("COPYGSIFTP");
    if(idx != -1) {
      return true;
    }
    return false;
  }

  public boolean getDisableCopyPull() {
    int idx = disableInfo.indexOf("COPYPULL");
    if(idx != -1) {
      return true;
    }
    return false;
  }

  //public String getCopySources() {
    //return copySources;
  //}
}
