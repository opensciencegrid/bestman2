package gov.lbl.srm.transfer.mss;

//this class is for parsing -serviceurl type of handles
//for type == 0
//for type == 1 it is for parsing -s or -t type surl's mostly with ?SFN

public class MyGlobusURL {
  public String protocol="";
  public String host="";
  public int port;
  public String path = "";
  public String filePath="";
  public int type;

  public MyGlobusURL (String ss, int type) throws Exception { 
    this.type = type;
    if(type == 0) {
      parseServiceHandle(ss);
    }
    else if(type == 1) {
      parseSURLHandle(ss);
    }
  }

  public void parseServiceHandle (String ss) throws Exception {
    int idx = ss.indexOf("?SFN=");
    if(idx != -1) {
       ss = ss.substring(0,idx); 
    }

    idx = ss.indexOf("?S");
    if(idx != -1) {
       ss = ss.substring(0,idx); 
    }
    /*
    if(idx != -1) {
       throw new Exception("SURL format is not correct, " +
		"It must either contain srm://host:protocol/serviceHandle?SFN=/filepath  or " +
	    " srm://host:protocol/filePath please check it. "+ss);
    }
    */
    idx = ss.indexOf("://");
    if(idx != -1) {
       protocol = ss.substring(0,idx);
    }
    int idx1 = ss.indexOf(":",idx+3);
    if(idx1 != -1) {
       host = ss.substring(idx+3,idx1);
       //one more check for host sometimes has "/" in cern gsiftp 
       //for example gsiftp://host.cern.ch/host.cern.ch:/path
       int ii = ss.indexOf("?SFN=");
       if(ii != -1) {
         ss = ss.substring(0,ii);
       }
       int idx2 = ss.indexOf("/",idx1+1);
       if(idx2 != -1) {
           String tt = ss.substring(idx1+1,idx2).trim();
           if(!tt.equals("")) {
            try {
              port = Integer.parseInt(tt);
            }catch(NumberFormatException nfe) {
              throw new Exception("Given port is not valid number " + tt);
            }
           }
           path = ss.substring(idx2); 
      }
      else { //sometimes it is srm://host:port without  "/" after the port
        //sometime it like this srm://host:port?SFN
        ii = ss.indexOf("?SFN=");
        if(ii != -1) {
           ss = ss.substring(0,ii); 
        }
        String tt = ss.substring(idx1+1).trim();
        if(!tt.equals("")) {
         try {
          port = Integer.parseInt(tt);
         }catch(NumberFormatException nfe) {
           throw new Exception("Given port is not valid number " + tt);
         }
        }
      }
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
               //gsiftp://dmx.lbl.gov/tmp cases or  srm://dmx.lbl.gov/srmcache
               // protocol://host/path
               host=ss.substring((idx+3),idx2);
               path = ss.substring(idx2);
             }
           }
       }
       else {
          //cases where only srm://dmx.lbl.gov
          host = ss.substring(idx+3);
       }
    }
  }


  public void parseSURLHandle (String ss) throws Exception {
    int idx = ss.indexOf("?SFN=");
    if(idx != -1) {
       filePath = ss.substring(idx+5);
       if(filePath.equals("")) {
         throw new Exception("SURL format is not correct, " +
		  "It must either contain srm://host:protocol/serviceHandle?SFN=/filepath  or " +
	      " srm://host:protocol/filePath please check it. "+ss);
       }
       ss = ss.substring(0,idx); 
    }

    /*
    idx = ss.indexOf("?S");
    if(idx != -1) {
       throw new Exception("SURL format is not correct, " +
		"It must either contain srm://host:protocol/serviceHandle?SFN=/filepath  or " +
	    " srm://host:protocol/filePath please check it. "+ss);
    }
    */

    idx = ss.indexOf("://");
    if(idx != -1) {
       protocol = ss.substring(0,idx);
    }
    int idx1 = ss.indexOf(":",idx+3);
    if(idx1 != -1) {
       host = ss.substring(idx+3,idx1);

       int idx2 = ss.indexOf("/",idx1+1);
       if(idx2 != -1) {
         String tt = ss.substring(idx1+1,idx2).trim();
         if(!tt.equals("")) {
           try {
             port = Integer.parseInt(tt);
           }catch(NumberFormatException nfe) {
             throw new Exception("Given port is not valid number " + tt);
           }
         }
         if(filePath.equals("")) {
           filePath = ss.substring(idx2);
         }
         else {
           path = ss.substring(idx2);
         }
       }
       else { //sometimes it is srm://host:port without  "/" after the port
          String tt = ss.substring(idx1+1).trim();
          if(!tt.equals("")) {
           try {
            port = Integer.parseInt(tt);
           }catch(NumberFormatException nfe) {
            throw new Exception("Given port is not valid number " + tt);
           }
         }
      }
    }
    else {
      int idx2 = ss.indexOf("/",idx+3);
      if(idx2 != -1) {
        if(idx2 == idx+3) {
          //file:////path cases
          if(filePath.equals("")) {
            filePath = ss.substring(idx2);
          }
          else {
            path = ss.substring(idx2);
          }
        }
        else {
             if(idx2 > idx+3) {
               //gsiftp://dmx.lbl.gov/tmp cases or  srm://dmx.lbl.gov/srmcache
               // protocol://host/path
               host=ss.substring((idx+3),idx2);
               if(filePath.equals("")) { 
                 filePath = ss.substring(idx2);
               } 
               else {
                 path = ss.substring(idx2);
               }
             }
           }
        }
        else {
          //cases where only srm://dmx.lbl.gov
          host = ss.substring(idx+3);
        }
    }
  }

  public int getType() {
    return type;
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

  public String getFilePath() {
    return filePath;
  }
}
