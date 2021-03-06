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

public class MyGlobusURL {
  public String protocol="";
  public String host="";
  public int port;
  public String path = "";

  public MyGlobusURL (String ss) throws Exception {
    //System.out.println("ss="+ss);
    int idx = ss.indexOf("://");
    if(idx != -1) {
       protocol = ss.substring(0,idx);
    }
    //System.out.println("protocol="+protocol+" " + idx);
    int idx1 = ss.indexOf(":",idx+3);
    if(idx1 != -1) {
       host = ss.substring(idx+3,idx1);
       //one more check for host sometimes has "/" in cern gsiftp 
       //for example gsiftp://host.cern.ch/host.cern.ch:/path
       int xidx = host.indexOf("/");
       if(xidx != -1) { 
         host = host.substring(0,xidx);
       }
       int idx2 = ss.indexOf("/",idx1+1);
       //System.out.println("host="+host+" " + idx1);
       if(idx2 != -1) {
         String tt = ss.substring(idx1+1,idx2).trim();
         if(!tt.equals("")) {
          try {
            port = Integer.parseInt(tt);
          }catch(NumberFormatException nfe) {
            throw new Exception("Given port is not valid number " + tt);
          }
         }
         //System.out.println("port="+port+" " + idx2);
         path = ss.substring(idx2); 
       }
       //System.out.println("path="+path+" " + idx2);
    }
    else {
      int idx2 = ss.indexOf("/",idx+3);
      if(idx2 != -1) {
          if(idx2 == idx+3) {
            //file:////path cases
            path = ss.substring(idx2);
          }
          else {
             if(idx2 > idx+3) {
               //gsiftp:////host/path cases
               host=ss.substring(idx+3,idx2);
               path = ss.substring(idx2);
             }
          }
      }
      //System.out.println("path="+path+" " + idx2);
    }
  }

  public String getProtocol() {
    return protocol;
  }

  public String getHost() {
    return host;
  }
  public int getPort() {
   if(port == 0) return 2811;
   return port;
  }
  public String getPath() {
    return path;
  }
}
