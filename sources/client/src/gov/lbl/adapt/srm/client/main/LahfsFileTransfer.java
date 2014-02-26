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

import org.ietf.jgss.GSSCredential;

import org.globus.util.GlobusURL;
import java.io.IOException;
import gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer;
import gov.lbl.adapt.srm.client.transfer.globus.*;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.util.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.StringTokenizer;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// LahfsFileTransfer
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class LahfsFileTransfer 
    extends MySRMFileTransfer implements MyISRMFileTransfer {

private GSSCredential credentials;
private SRMTransferProtocol ttype = SRMTransferProtocol.HTTPS;
private SRMTransferMode mode = SRMTransferMode.GET; 
private String source="";
private String target="";
private boolean done = false;
private long startTime = 0;
private long endTime = 0;
private long totalTime = -1;

public String status;

private static String OK = "200";
private static String REDIRECT = "302";
private static int LENGTH =  1024; 
private Log _logger;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector ();
private boolean _silent;
  //input/ouput buffer size

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// LahfsFileTransfer --- constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public LahfsFileTransfer (String source, String target) {
   this.source = source;
   this.target = target;

   //static {
     Properties props = new Properties (System.getProperties());
     try {
         //String parametersFile = System.getProperty("parameters");
         props.load(new BufferedInputStream(
				new FileInputStream("lahfs.properties"))); 
         System.setProperties(props);
     }catch(IOException ioe) { 
         //inputVec = new Vector();
         //inputVec.addElement("IOException="+ioe.getMessage());
         //inputVec.addElement("Parameter file not found");
         //util.printEventLog(_theLogger,"LahfsFileTransfer.constructor",inputVec);
         if(!_silent) {
           System.out.println("SRM-CLIENT: Parameter file not found " + ioe.getMessage());
         } 
         System.exit(1);
     }
   //}
   String authMode = System.getProperty("ncar.scd.lahfs.authenticationMode");
   if(authMode != null) {
        //inputVec = new Vector();
        //inputVec.addElement("AuthenticationMode="  + authMode);
        //util.printEventLog(_theLogger,"LahfsFileTransfer.constructor",inputVec);
        System.out.println("SRM-CLIENT: AuthenticationMode="+authMode);
        if(authMode.equalsIgnoreCase("plain")) {
           String userName = System.getProperty("ncar.scd.lahfs.username");
           String passWord = System.getProperty("ncar.scd.lahfs.password");
           if(userName != null && passWord != null) {
              this.source += "&username="+userName+"&password="+passWord;
           }else {
              //inputVec = new Vector();
              //inputVec.addElement("Either username or password is null");
              //util.printEventLog(_theLogger,"LahfsFileTransfer.constructor",inputVec);
              if(!_silent) {
                System.out.println("SRM-CLIENT: Either username or password is null");
              }
           }
        }
   }

}

public void setSessionType (int sessionType) {
}

public void setSessionMode (int sessionMode) {
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setLogger
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setLogger(Log log, java.util.logging.Logger theLogger, boolean silent) {
   _logger = log;
   this._theLogger = theLogger;
   this._silent = silent;
}

public void setLogger(Log log) {
   _logger = log;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferType
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferType(SRMTransferProtocol ttypeu) {
   this.ttype = ttype;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setTransferMode
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setTransferMode(SRMTransferMode mode) {
   this.mode = mode;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setParallel
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setParallel(int parallelu) { }

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setCredential
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setCredentials(GSSCredential credentials) {
   this.credentials = credentials;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setBufferSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setBufferSize(int size) { }


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setDCAU
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setDCAU(boolean dcau) {}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// start
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//public void start() {
  //super.start();
//}

public void transfer () {
  this.start();
}

public void transfer(String source, String target) {
  this.source = source;
  this.target = target;
  this.start();
}

public void cancel () { }

public boolean transferDone () { 
  return done;
}

public void transferSync() {
  download(source);
}

public long getSourceFileSize() {
  return -1;
}

public long getTransferTimeInMilliSeconds () {
   if(done) {
     if(startTime > 0) {
       totalTime =  endTime - startTime;
     }
   }
   return totalTime;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void run() {
  transferSync();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// download
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void download(String url) {

 try {
   String path = url.substring(0,url.indexOf("?"));
   String query = url.substring(url.indexOf("?")+1);

   //send request;
   URLConnection conn = doPost(path,query);

   String response = getResponseStatus(conn);
   //_logger.debug("RESPONSE STATUS="+response);
   //inputVec = new Vector();
   //inputVec.addElement("ResponseStatus="+response);
   //util.printEventLog(_theLogger,"LahfsFileTransfer.download",inputVec);
   System.out.println("SRM-CLIENT: RESPONSE STATUS="+response);

   //redirect to new URL

   if(response.equals(REDIRECT)) {
     String location = conn.getHeaderField("Location");
     download(location);
     //read response -- write to file
   } else if(response.equals(OK)) {
       String type = conn.getContentType();
       startTime = System.currentTimeMillis();
       String tempFile = target;
       String tt = "file:///";
       int idx = target.indexOf(tt);
       if(idx != -1) {
          tempFile = target.substring(idx+tt.length()); 
       }
       if(type.toLowerCase().equals("text/html")) {
           writeText(conn,tempFile);
           done = true;
           endTime = System.currentTimeMillis();
       }else {
           writeBinary(conn,tempFile);
           done = true;
           endTime = System.currentTimeMillis();
       }
   }
  }catch(Exception e) {
     //inputVec = new Vector();
     //inputVec.addElement("Exception="+e.getMessage());
     //util.printEventLog(_theLogger,"LahfsFileTransfer.download",inputVec);
     System.out.println("SRM-CLIENT: EXCEPTION=" + e.getMessage());
     //_logger.debug("Exception " + e.getMessage());
     this.status = e.getMessage();
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doPost
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private URLConnection doPost(String path, String data) throws Exception 
{
   URL url = new URL(path);
   URLConnection conn = url.openConnection();
   conn.setDoOutput(true);
   OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
   wr.write(data);
   wr.flush();
   return conn;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGet
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private URLConnection doGet(String url) throws Exception {
  URL _url = new URL (url);
  URLConnection conn = _url.openConnection(); 
  return conn; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeText
//    Utility method to write text from an input stream to a file 
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void writeText(URLConnection conn, String outputFile) 
    throws Exception
{
    BufferedReader rd = new BufferedReader 
	(new InputStreamReader(conn.getInputStream()));
    BufferedWriter bw = new BufferedWriter (new FileWriter(outputFile));
    char[] chars = new char[LENGTH];
    int numRead = -1;
    while((numRead=rd.read(chars,0,chars.length)) >= 0) {
	bw.write(chars,0,numRead);
    }
    bw.flush();
    bw.close();
    rd.close();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeBinary
// Utility method to write binary data from an input stream to a file
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


private void writeBinary(URLConnection conn, String outputFile) 
   throws Exception 
{
   BufferedInputStream bis = new BufferedInputStream(conn.getInputStream());
   BufferedOutputStream bos = 
		new BufferedOutputStream(new FileOutputStream(outputFile));
   byte[] bytes = new byte[LENGTH];
   int numRead = -1;
   while ((numRead=bis.read(bytes,0,bytes.length))>=0) {
      bos.write(bytes,0,numRead);
   }
   bis.close();
   bos.flush();
   bos.close();

}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getResponseStatus
//  Utility method to retrieve the respopnse status 
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String getResponseStatus(URLConnection conn) {
   String firstHeader = conn.getHeaderField(0); 
   String[] parts = firstHeader.split("\\s+");
   return parts[1];
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// encode
//    Utility method to encode HTTP request parameters */
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String encode(String query) throws Exception {
   //_logger.debug("encoding query="+query);
   //inputVec = new Vector();
   //inputVec.addElement("EncodingQuery="+query);
   //util.printEventLog(_theLogger,"LahfsFileTransfer.encode",inputVec);
   System.out.println("SRM-CLIENT: EncodingQuery="+query);
   StringBuffer sb = new StringBuffer();
   StringTokenizer st = new StringTokenizer(query,"&");
   while (st.hasMoreTokens()) {
       String token = st.nextToken();
       String[] parts = token.split("=");
       if (sb.length()>0) sb.append("&");
       sb.append(URLEncoder.encode(parts[0], "UTF-8")).append("=").append(URLEncoder.encode(parts[1], "UTF-8"));
   }
   return sb.toString();
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getStatus
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getStatus () {
  return this.status;
}

}
