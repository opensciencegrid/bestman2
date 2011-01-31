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

import java.io.*;
import java.util.*;
import java.net.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.io.urlcopy.UrlCopy;
import org.globus.util.GlobusURL;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.client.intf.PrintIntf;
//import gov.lbl.srm.server.TSRMUtil;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;

import javax.xml.rpc.Stub;

import org.globus.io.urlcopy.UrlCopy;
import org.globus.util.GlobusURL;

//import org.globus.ogsa.utils.GSIUtils;
import org.globus.axis.gsi.GSIConstants;
//import org.globus.ogsa.impl.security.authentication.Constants;

import org.globus.util.ConfigUtil;
//import org.globus.ogsa.utils.GetOpts;
//import org.globus.ogsa.gui.ServiceContext;
import org.globus.gsi.gssapi.auth.*;

import org.apache.axis.types.*;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.*;
import org.apache.axis.utils.Options;
import org.apache.axis.SimpleTargetedChain;
import org.globus.axis.transport.*;
import org.apache.axis.transport.http.HTTPSender;
import org.globus.axis.util.Util;



//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// utility class
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class util {
  public static void showException(Exception e, Log logger) {

     StackTraceElement[] strace = e.getStackTrace();
     System.out.println("Exception: " + e.getMessage());
     //logger.debug("Exception: " + e.getMessage());

     if(logger != null) {
       for(int i = (strace.length-1); i >= 0; i--) {
          StackTraceElement ss = strace[i];
          //logger.debug("++++ Stack Element (" + i + ")+++++");
          //logger.debug("ClassName  " + ss.getClassName());
          //logger.debug("FileName   " + ss.getFileName());
          //logger.debug("LineNumber " + ss.getLineNumber());
          //logger.debug("MethodName " + ss.getMethodName());
          //logger.debug("++++++++++++++++++++++++++++++++++++");
        }
     }
     else {
       for(int i = (strace.length-1); i >= 0; i--) {
          StackTraceElement ss = strace[i];
          System.out.println("++++ Stack Element (" + i + ")+++++");
          System.out.println("ClassName  " + ss.getClassName());
          System.out.println("FileName   " + ss.getFileName());
          System.out.println("LineNumber " + ss.getLineNumber());
          System.out.println("MethodName " + ss.getMethodName());
          System.out.println("++++++++++++++++++++++++++++++++++++");
        }
     }
  }

  public static ReportRequestStatus
    gsiftpResultCreate(StringBuffer startTimeStamp, 
	   StringBuffer endTimeStamp,long size) {

    ReportRequestStatus rrs = new ReportRequestStatus();
    rrs.setRequestId("");
    rrs.setStartTimeStamp(startTimeStamp.toString());
    rrs.setEndTimeStamp(endTimeStamp.toString());
    if(size != 0) {
      rrs.setFileSize(""+size);
    }
    return rrs;
  }

  public static boolean isConnectionTimeOut(int startTimeStamp) {
    int retryAllowed = 1800;
    /*
    Date d = new Date();
    int minutes = d.getMinutes();
    int seconds = d.getSeconds();
    int currentTimeStamp = minutes*60+seconds;
    if(currentTimeStamp < startTimeStamp) { 
      currentTimeStamp = currentTimeStamp+startTimeStamp;
    }
    if(currentTimeStamp <= (startTimeStamp+retryAllowed)) {
      return false;
    }
    return true; 
    */
    //long currentTimeStamp = System.currentTimeMillis();
    int currentTimeStamp = util.startTimeStamp();
    //System.out.println("currenttimestamp = " + currentTimeStamp);
    //System.out.println("starttimestamp = " + startTimeStamp);
    //System.out.println("value = " + (currentTimeStamp <= (startTimeStamp+(retryAllowed*1000))));
    if(currentTimeStamp <= (startTimeStamp+(retryAllowed*1000))) {
       return false;
    }
    return true;
  }


  public static void printMessageHException 
		(String str, Log logger, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHException(str);
        //logger.debug(str);
     }
     else {
       System.out.println(str);
       //logger.debug(str);
     }
  }

  public static void printMessageHL 
		(String str, Log logger, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHL(str);
        //logger.debug(str);
     }
     else {
       System.out.println(str);
       //logger.debug(str);
     }
  }

  public static void printMessageHL2 
		(String str, Log logger, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHL2(str);
        //logger.debug(str);
     }
     else {
       System.out.println(str);
       //logger.debug(str);
     }
  }

  public static void printMessage 
		(String str, Log logger, PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessage(str);
        //logger.debug(str);
     }
     else {
       System.out.println(str);
       //logger.debug(str);
     }
  }

  public static void printMessageHNL (String str, Log logger, 
		PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageHNL(str);
        if(!str.trim().equals("."))
         ;
          //logger.debug(str);
     }
     else {
       System.out.print(str);
       if(!str.trim().equals("."))
         ;
         //logger.debug(str);
     }
  }


  public static void printMessageNL (String str, Log logger, 
		PrintIntf pIntf) {
     if(pIntf != null) {
        pIntf.printMessageNL(str);
        if(!str.trim().equals("."))
         ;
          //logger.debug(str);
     }
     else {
       System.out.print(str);
       if(!str.trim().equals(".")) 
         ;
         //logger.debug(str);
     }
  }

  public static void writeSummary(boolean localPublish,
       BufferedWriter bs, BufferedWriter bsa, String message) 
			throws Exception {
      if(localPublish) {
        bsa.write(message+"<br>\n");
        bsa.flush();
      }
  }

  public static void webScript(String scriptName,
     String testType, String oper, String status, 
  	    Object obj, String possibleExplanation,
		String timeStamp, String fullTimeStamp, 
        String startTimeStamp, String operatorTimeStamp,
	    String servicePath,boolean noPublish, 
		String siteName, int numOperations, 
	    String operType, String methodName,
	    String opStatus, boolean dailyTest, 
		int numSites, boolean localPublish, 
		BufferedWriter bs, BufferedWriter bsa)  
			throws Exception {

   if(localPublish) {

     //if(status.equalsIgnoreCase("Yes")) {
      //status = "Ok";
     //}

     if(!methodName.equals("")) {
       //String methodname = siteName+"#"+methodName+"#"+status;
       String methodname = methodName+"#"+opStatus;
       bs.write(methodname);
       bs.newLine();
       bs.flush();
     }

     if(oper.equals("START") || oper.equals("BLANK")) { ; }
     else {
        boolean startAnchor=false;
        if(!oper.startsWith("Sending") && !methodName.equals("")) {
          //bsa.write("\n<a name=\""+methodName+"\"></a>\n");
          bsa.write("\n<a name=\""+methodName+"\">\n");
          bsa.flush();
          startAnchor=true;
        }
        writeToArchiveFile(bsa,"\n<p><HR><br>","");
        if(!methodName.equals("")) {
          if(oper.equalsIgnoreCase("RequestSummary")) {
            writeToArchiveFile(bsa,"<B><I>Summary for "+methodName+ "</I></B>",
				"ignoreequals"); 
          }
          else {
            writeToArchiveFile(bsa,"<B><I>"+methodName + " at " +
		  	  operatorTimeStamp+"</I></B>","ignoreequals");
          }
          if(methodName.startsWith("srmCopy-pull") || methodName.startsWith("srmLs") ||
             methodName.startsWith("srmStatusOfLs") || methodName.startsWith("srmRm")) {
            if( obj != null && obj instanceof ReportRequestStatus)  {
              ReportRequestStatus rrs = (ReportRequestStatus) obj;
              writeToArchiveFile(bsa,"\nServicePath",rrs.getSrmServicePath());
            }
            else if( obj != null && obj instanceof BrowseRequestStatus)  {
              BrowseRequestStatus brs = (BrowseRequestStatus) obj;
              writeToArchiveFile(bsa,"\nServicePath",brs.getSrmServicePath());
            }
            else {
              writeToArchiveFile(bsa,"\nServicePath",servicePath);
            }
          }
          else {  
            writeToArchiveFile(bsa,"\nServicePath",servicePath);
          }
        }
        if(!oper.startsWith("Sending")) {
           if(!methodName.equals("")) {
             writeToArchiveFile(bsa,"Status",status);
           }
           if(!possibleExplanation.equals("")) {
             writeToArchiveFile(bsa,"Explanation",possibleExplanation);
           }
           writeDetails(bsa,null,obj,localPublish);
        }
        else {
          writeDetails(bsa,null,obj,localPublish);
        } 
        if(startAnchor) {
          bsa.write("\n</a><HR>\n");
        }
     }

     return;
   }


   if(!noPublish) {
    Calendar c = new GregorianCalendar();
    StringBuffer buf = new StringBuffer();
    int month = c.get(Calendar.MONTH)+1;
    buf.append(month+"-");
    buf.append(c.get(Calendar.DATE)+"-");
    buf.append(c.get(Calendar.YEAR)+":");
    buf.append(c.get(Calendar.HOUR_OF_DAY)+":");
    int minute = c.get(Calendar.MINUTE);
    buf.append(minute);

    String replacedPath = servicePath.replace('/','-');

    String tlogin = URLEncoder.encode(replacedPath);
    String dlogin = URLEncoder.encode(servicePath);
    String ttime = URLEncoder.encode(timeStamp);
    String tstime = URLEncoder.encode(fullTimeStamp);
    String dttype = URLEncoder.encode(testType);
    String fttype = URLEncoder.encode(testType.replace('/','-'));
    String toper = URLEncoder.encode(oper);
    String tstatus = URLEncoder.encode(status);
    String texplanation = URLEncoder.encode(possibleExplanation);
    String sName = URLEncoder.encode(siteName);
    String optype = URLEncoder.encode(operType);
    String methodname = URLEncoder.encode(methodName);
    String numOp = URLEncoder.encode(""+numOperations);
    String ostatus = URLEncoder.encode(opStatus);
    String starttime = URLEncoder.encode(startTimeStamp);

    URL url = new URL (scriptName);
    URLConnection connection = url.openConnection();
    connection.setDoOutput(true);

    PrintWriter out = new PrintWriter(connection.getOutputStream());
    out.print("login=" + tlogin);
    out.print("&dlogin=" + dlogin);
    out.print("&ttime="+ttime);
    out.print("&tstime="+tstime);
    out.print("&dttype="+dttype);
    out.print("&fttype="+fttype);
    out.print("&oper="+toper);
    out.print("&status="+status);
    if(dailyTest) {
      out.print("&sitename="+sName);
      out.print("&starttime="+starttime);
      out.print("&optype="+operType);
      out.print("&methodname="+methodname);
      out.print("&numsites="+numSites);
      out.print("&tstatus="+ostatus);
      out.print("&numoper="+numOp);
    }

    writeDetails(bsa,out,obj,localPublish);
    out.print("&EXPLANATION="+texplanation);

    out.close();

    BufferedReader in = new BufferedReader(
                  new InputStreamReader(connection.getInputStream()));

    String inputLine;

    while((inputLine = in.readLine()) != null) {
         System.out.println(inputLine);
    }

    in.close();
  }
}

private static void writeDetails(BufferedWriter bsa, 
	PrintWriter out, Object obj, boolean localPublish) throws Exception {

    //if(bsa == null || out == null) return;

    if(bsa == null) return;

       if(obj == null) {
         if(!localPublish) { 
           out.print("&rrsstatus="+"NOTOK");
         }
       }
       else {
         if( obj instanceof PingReportStatus)  {
           PingReportStatus ps = (PingReportStatus)obj;
           if(!localPublish && out != null) {  
             out.print("&rrsstatus="+"POK");
             out.print("&id="+""+ps.getAuthorizationId());
             out.print("&versioninfo="+""+ps.getVersionInfo());
             out.print("&methodinfo="+""+ps.getMethodInfo());
             out.print("&actualresult="+ps.getActualResult());
             out.print("&matchinginfo="+ps.getMatchingInfo());
             out.print("&isexpected="+ps.isExpectedValue());
             Vector keys = ps.getKeys();
             Vector values = ps.getValues();
             out.print("&keysize="+keys.size());
             for(int i = 0; i < keys.size(); i++) {
               out.print("&key_"+i+"="+(String)keys.elementAt(i));
               out.print("&value_"+i+"="+(String)values.elementAt(i));
             }
           }
           else {  
             writeToArchiveFile(bsa,"AuthorizationId",ps.getAuthorizationId());
             writeToArchiveFile(bsa,"versioninfo",ps.getVersionInfo());
             writeToArchiveFile(bsa,"methodinfo",ps.getMethodInfo());
             //writeToArchiveFile(bsa,"actualresult",ps.getActualResult());
             //writeToArchiveFile(bsa,"matchinginfo",ps.getMatchingInfo());
             //writeToArchiveFile(bsa,"isexpected",ps.isExpectedValue());
             Vector keys = ps.getKeys();
             Vector values = ps.getValues();
             //writeToArchiveFile(bsa,"keysize",""+keys.size());
             for(int i = 0; i < keys.size(); i++) {
               writeToArchiveFile(bsa,"key",(String)keys.elementAt(i));
               writeToArchiveFile(bsa,"value",(String)values.elementAt(i));
             }
           }
         }
         if( obj instanceof ReportRequestStatus)  {
           ReportRequestStatus rrs = (ReportRequestStatus)obj;  
           if(!localPublish && out != null) {
             out.print("&rrsstatus="+"OK");
             out.print("&id="+""+rrs.getRequestId());
             out.print("&state="+rrs.getRequestState());
             out.print("&fsize="+rrs.getFileSize());
             out.print("&file_size="+rrs.getFileSize());
             out.print("&stime="+rrs.getStartTimeStamp());
             out.print("&etime="+rrs.getEndTimeStamp());
             out.print("&spacetoken="+rrs.getSpaceToken());
             out.print("&userdesc="+rrs.getUserDescription());
             out.print("&userid="+rrs.getUserId());
             out.print("&storagetype="+rrs.getFileStorageType());
             out.print("&totaltime="+rrs.getDesiredTotalRequestTime());
             out.print("&pintime="+rrs.getDesiredPinLifeTime());
             out.print("&filelifetime="+rrs.getDesiredFileLifeTime());
             out.print("&targetsurltime="+rrs.getTargetSURLTime());
             out.print("&overwrite="+rrs.getOverwrite());
             out.print("&recursive="+rrs.getRecursive());
             out.print("&storageinfo="+rrs.getStorageSystemInfo());
             out.print("&protocol="+rrs.getTransferParametersProtocol());
             out.print("&connectiontype="+	
				rrs.getTransferParametersConnectionType());
             out.print("&accesspattern="+
				rrs.getTransferParametersAccessPattern());
             out.print("&retentionpolicy="+rrs.getRetentionPolicy());
             out.print("&latency="+rrs.getAccessLatency());
             out.print("&requestid="+rrs.getRequestId());
             out.print("&expectedresult="+rrs.getExpectedResult());
             out.print("&actualresult="+rrs.getActualResult());
             out.print("&matchinginfo="+rrs.getMatchingInfo());
             out.print("&linkname="+rrs.getLinkName());
             out.print("&isexpected="+rrs.isExpectedValue());
             out.print("&srmpath="+rrs.getSrmServicePath());
             out.print("&tspace="+rrs.getTotalSpace());
             out.print("&gspace="+rrs.getGuarnSpace());
             out.print("&ltime="+rrs.getLifeTime());
             out.print("&newspacesize="+rrs.getNewSpaceSize());
             out.print("&newspacetime="+rrs.getNewLifeTime());
           }
           else {
             //writeToArchiveFile(bsa,"<p><B>InputParameters List</B>","");
             //writeToArchiveFile(bsa,"Redirected SrmServicePath", rrs.getSrmServicePath());
             writeToArchiveFile(bsa,"RequestId",rrs.getRequestId());
             writeToArchiveFile(bsa,"RequestState",rrs.getRequestState());
             writeToArchiveFile(bsa,"FileSize",rrs.getFileSize());
             writeToArchiveFile(bsa,"StartTimeStamp",rrs.getStartTimeStamp());
             writeToArchiveFile(bsa,"EndTimeStamp",rrs.getEndTimeStamp());
             writeToArchiveFile(bsa,"SpaceToken",rrs.getSpaceToken());
             writeToArchiveFile(bsa,"UserDesc",rrs.getUserDescription());
             writeToArchiveFile(bsa,"UserId",rrs.getUserId());
             writeToArchiveFile(bsa,"StorageType",rrs.getFileStorageType());
             writeToArchiveFile(bsa,"Totaltime",rrs.getDesiredTotalRequestTime());
             writeToArchiveFile(bsa,"PinTime",rrs.getDesiredPinLifeTime());
             writeToArchiveFile(bsa,"FileLifeTime",rrs.getDesiredFileLifeTime());
             writeToArchiveFile(bsa,"TargetSURLTime",rrs.getTargetSURLTime());
             writeToArchiveFile(bsa,"OverWrite",rrs.getOverwrite());
             writeToArchiveFile(bsa,"Recursive",rrs.getRecursive());
             writeToArchiveFile(bsa,"StorageInfo",rrs.getStorageSystemInfo());
             writeToArchiveFile(bsa,"Protocol",rrs.getTransferParametersProtocol());
             writeToArchiveFile(bsa,"ConnectionType",
					rrs.getTransferParametersConnectionType());
             writeToArchiveFile(bsa,"AccessPattern",
					rrs.getTransferParametersAccessPattern());
             writeToArchiveFile(bsa,"RetentionPolicy", rrs.getRetentionPolicy());
             writeToArchiveFile(bsa,"Latency", rrs.getAccessLatency());
             writeToArchiveFile(bsa,"ExpectedResult", rrs.getExpectedResult());
             //writeToArchiveFile(bsa,"ActualResult", rrs.getActualResult());
             //writeToArchiveFile(bsa,"MatchingInfo", rrs.getMatchingInfo());
             //writeToArchiveFile(bsa,"IsExpected", rrs.isExpectedValue());
             writeToArchiveFile(bsa,"tspace", rrs.getTotalSpace());
             writeToArchiveFile(bsa,"gspace", rrs.getGuarnSpace());
             writeToArchiveFile(bsa,"ltime", rrs.getLifeTime());
             writeToArchiveFile(bsa,"newspacesize", rrs.getNewSpaceSize());
             writeToArchiveFile(bsa,"newspacetime", rrs.getNewLifeTime());
           }
          
          
           ReportRequestFileStatus [] reportRequestFileStatus = 
				rrs.getReportRequestFileStatus();
           if(reportRequestFileStatus != null) { 
            for (int i = 0 ; i < reportRequestFileStatus.length; i++) {
             if(reportRequestFileStatus[i] != null) {
              ReportRequestFileStatus rfs = reportRequestFileStatus[i];
              if(localPublish) {
                writeToArchiveFile(bsa,"Source SURL",rfs.getSourceUrl());
                writeToArchiveFile(bsa,"Transfer URL",rfs.getSourceTransferUrl());
                writeToArchiveFile(bsa,"Transfer URL",rfs.getTransferUrl());
                writeToArchiveFile(bsa,"Target SURL",rfs.getTargetUrl());
                writeToArchiveFile(bsa,"FileStatus",rfs.getState());
                writeToArchiveFile(bsa,"Explanation",rfs.getExplanation());
                writeToArchiveFile(bsa,"Size",rfs.getSize());
              }
              else {
                out.print("&fstatus="+rfs.getState());
                out.print("&fexplanation="+rfs.getExplanation());
                out.print("&fsize="+rfs.getSize());
                out.print("&SURL="+rfs.getSourceUrl());
                out.print("&STURL="+rfs.getSourceTransferUrl());
                out.print("&TTURL="+rfs.getTransferUrl());
                out.print("&TURL="+rfs.getTargetUrl());
              }
             }
            }
           }
         }
         else if(obj instanceof SpaceRequestStatus) {
           SpaceRequestStatus rrs = (SpaceRequestStatus)obj;  
           if(!localPublish && out != null) { 
             out.print("&rrsstatus="+"SOK");
             out.print("&tdspace="+rrs.getSizeOfTotalReservedSpaceDesired());
             out.print("&spacetype="+rrs.getSpaceType());
             if(!rrs.getStatus().equals("")) {  
               out.print("&status="+rrs.getStatus());
               out.print("&explanation="+rrs.getExplanation());
               out.print("&spacetoken="+rrs.getSpaceToken());
               out.print("&gspace="+rrs.getSizeOfGuaranteedReservedSpace());
               out.print("&tspace="+rrs.getSizeOfTotalReservedSpace());
               out.print("&uspace="+rrs.getSizeOfUnusedReservedSpace());
               out.print("&ltaspace="+rrs.getLifetimeAssignedOfReservedSpace());
               out.print("&ltlspace="+rrs.getLifetimeLeftOfReservedSpace());
               out.print("&valid="+rrs.getIsValid());
             }
           }
           else { 
             writeToArchiveFile(bsa,"TotalReservedSpace",
					rrs.getSizeOfTotalReservedSpaceDesired());
             writeToArchiveFile(bsa,"SpaceType=",rrs.getSpaceType());
             if(!rrs.getStatus().equals("")) {  
               //writeToArchiveFile(bsa,"<p><B>InputParameters List</B>","");
               writeToArchiveFile(bsa,"Status",rrs.getStatus());
               writeToArchiveFile(bsa,"Explanation",rrs.getExplanation());
               writeToArchiveFile(bsa,"SpaceToken",rrs.getSpaceToken());
               writeToArchiveFile(bsa,"GuaranSpace",
						rrs.getSizeOfGuaranteedReservedSpace());
               writeToArchiveFile(bsa,"TotalReservedSpace",
						rrs.getSizeOfTotalReservedSpace());
               writeToArchiveFile(bsa,"UnusedReservedSpace",
						rrs.getSizeOfUnusedReservedSpace());
               writeToArchiveFile(bsa,"LifeTimeAssignedSpace",
						rrs.getLifetimeAssignedOfReservedSpace());
               writeToArchiveFile(bsa,"LifetimeLeftOfReservedSpace",
						rrs.getLifetimeLeftOfReservedSpace());
               writeToArchiveFile(bsa,"Valid",rrs.getIsValid());
             }
           }
         }
         else if(obj instanceof BrowseRequestStatus) {
           BrowseRequestStatus brs = (BrowseRequestStatus)obj;  
           if(!localPublish && out != null) {
             out.print("&rrsstatus="+"BOK");
             out.print("&id="+brs.getUserId());
             out.print("&rid="+brs.getRequestId());
             out.print("&flist="+brs.getFullDetailedList());
             out.print("&nlevels="+brs.getNumOfLevels());
             out.print("&recur="+brs.getAllLevelRecursive());
             out.print("&stype="+brs.getFileStorageType());
             out.print("&sinfo="+brs.getStorageSystemInfo());
             out.print("&burl="+brs.getBrowseUrl());
             out.print("&status="+brs.getStatus());
             out.print("&totallength="+brs.getTotalNumPathDetail());
             out.print("&explanation="+brs.getExplanation());
           }
           else {
             writeToArchiveFile(bsa,"UserId",brs.getUserId());
             writeToArchiveFile(bsa,"RequestId",brs.getRequestId());
             writeToArchiveFile(bsa,"FullDetailedList",brs.getFullDetailedList());
             writeToArchiveFile(bsa,"NumLevels",brs.getNumOfLevels());
             writeToArchiveFile(bsa,"AllLevelRecur",brs.getAllLevelRecursive());
             writeToArchiveFile(bsa,"FileStorageType",brs.getFileStorageType());
             writeToArchiveFile(bsa,"StorageSystemInfo",brs.getStorageSystemInfo());
             writeToArchiveFile(bsa,"BrowseUrl",brs.getBrowseUrl());
             writeToArchiveFile(bsa,"Status",brs.getStatus());
             writeToArchiveFile(bsa,"TotalLength",brs.getTotalNumPathDetail());
             writeToArchiveFile(bsa,"Explanation",brs.getExplanation());
           }
           if(brs.getDataPathStatus() != null) {
           DataPathStatus dps = brs.getDataPathStatus();
           if(dps != null) {
             if(!localPublish && out != null) {
               out.print("&dpsosurl="+dps.getSurl());
               out.print("&dpssize="+dps.getSize());
               out.print("&dpsstoragetype="+dps.getStorageType());
               out.print("&dpsstatus="+dps.getStatus());
               out.print("&dpsftype="+dps.getFileType());
               out.print("&dpsoperm="+dps.getOwnerPermission());
               out.print("&dpslleft="+dps.getLifetimeLeft());
               out.print("&dpsexplanation="+dps.getExplanation());
               out.print("&dpslastaccessed="+dps.getLastAccessed());
             }
             else {
               writeToArchiveFile(bsa,"Source SURL",dps.getSurl());
               writeToArchiveFile(bsa,"Size",dps.getSize());
               writeToArchiveFile(bsa,"StorageType",dps.getStorageType());
               writeToArchiveFile(bsa,"Status",dps.getStatus());
               writeToArchiveFile(bsa,"FileType",dps.getFileType());
               writeToArchiveFile(bsa,"OwnerPermission",dps.getOwnerPermission());
               writeToArchiveFile(bsa,"LifeTimeLeft",dps.getLifetimeLeft());
               writeToArchiveFile(bsa,"Explanation",dps.getExplanation());
               writeToArchiveFile(bsa,"LastAccessed",dps.getLastAccessed());
             }
             if(dps.getSubPath() != null) {
             Vector vec = dps.getSubPath();
             int size = 0;
             if(vec != null) {
               size = vec.size();
             }
             if(out != null) 
               out.print("&dpssubsize="+size);
             for(int i = 0; i < size ; i++) { 
                DataPathStatus d1 = (DataPathStatus)vec.elementAt(i);
                if(d1 != null) {
                if(!localPublish && out != null) {
                  out.print("&dpsosurl_"+i+"="+d1.getSurl());
                  out.print("&dpssize_"+i+"="+d1.getSize());
                  out.print("&dpsstoragetype_"+i+"="+d1.getStorageType());
                  out.print("&dpsftype_"+i+"="+d1.getFileType());
                  out.print("&dpslleft_"+i+"="+d1.getLifetimeLeft());
                  out.print("&dpsoperm_"+i+"="+d1.getOwnerPermission());
                  out.print("&dpsstatus_"+i+"="+d1.getStatus());
                  out.print("&dpsexplanation_"+i+"="+d1.getExplanation());
                  out.print("&dpslastaccessed_"+i+"="+d1.getLastAccessed());
                }
                else {
                  writeToArchiveFile(bsa,"Source SURL_"+i,d1.getSurl());
                  writeToArchiveFile(bsa,"Size_"+i,d1.getSize());
                  writeToArchiveFile(bsa,"StorageType_"+i,d1.getStorageType());
                  writeToArchiveFile(bsa,"FileType_"+i,d1.getFileType());
                  writeToArchiveFile(bsa,"LifeTimeLeft_"+i,d1.getLifetimeLeft());
                  writeToArchiveFile(bsa,"OwnerPermission_"+i,d1.getOwnerPermission());
                  writeToArchiveFile(bsa,"Status_"+i,d1.getStatus());
                  writeToArchiveFile(bsa,"Explanation_"+i,d1.getExplanation());
                  writeToArchiveFile(bsa,"LastAccessed_"+i,d1.getLastAccessed());
                }
                if(d1.getSubPath() != null) {
                Vector vec1 = d1.getSubPath();
                if(out != null) 
                  out.print("&dps_"+i+"_subsize="+vec1.size());
                for(int j = 0; j < vec1.size(); j++) { 
                   DataPathStatus d11 = (DataPathStatus)vec1.elementAt(j);
                   if(!localPublish && out != null) { 
                     out.print("&dpsosurl_"+i+"_"+j+"="+d11.getSurl());
                     out.print("&dpssize_"+i+"_"+j+"="+d11.getSize());
                     out.print("&dpsstoragetype_"+i+"_"+j+"="+
						d11.getStorageType());
                     out.print("&dpsftype_"+i+"_"+j+"="+d11.getFileType());
                     out.print("&dpslleft_"+i+"_"+j+"="+d11.getLifetimeLeft());
                     out.print("&dpsoperm_"+i+"_"+j+"="+
						d11.getOwnerPermission());
                     out.print("&dpsstatus_"+i+"_"+j+"="+d11.getStatus());
                     out.print("&dpsexplanation_"+i+"_"+j+"="+
						d11.getExplanation());
                     out.print("&dpslastaccessed_"+i+"_"+j+"="+
						d11.getLastAccessed());
                   }
                   else { 
                     writeToArchiveFile(bsa,"Source SURL_"+i+"_"+j,d11.getSurl());
                     writeToArchiveFile(bsa,"Size_"+i+"_"+j,d11.getSize());
                     writeToArchiveFile(bsa,"StorageType_"+i+"_"+j,
						d11.getStorageType());
                     writeToArchiveFile(bsa,"FileType_"+i+"_"+j,d11.getFileType());
                     writeToArchiveFile(bsa,"LifeTimeLeft_"+i+"_"+j,
						d11.getLifetimeLeft());
                     writeToArchiveFile(bsa,"OwnerPermission_"+i+"_"+j,
						d11.getOwnerPermission());
                     writeToArchiveFile(bsa,"Status_"+i+"_"+j,d11.getStatus());
                     writeToArchiveFile(bsa,"Explanation_"+i+"_"+j,
						d11.getExplanation());
                     writeToArchiveFile(bsa,"LastAccessed_"+i+"_"+j,
						d11.getLastAccessed());
                   }
                }
                }
               }
             }
            }
           }
           }
         }
       }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// writeToArchiveFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static void writeToArchiveFile(BufferedWriter bsa,
		String key,String value) throws Exception {
  if(key.equalsIgnoreCase("isexpected")) {
   ; //just skip this
   return;
  }
  if(value == null) {
    return;
  }
  if(value.equals("ignoreequals")) {
    bsa.write(key+ "<br>");
    bsa.newLine();
    bsa.flush();
  }
  else {
    if (!value.equals("")) {
      bsa.write(key+"="+value+"<br>");
      bsa.newLine();
      bsa.flush();
   }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parsefile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Properties parsefile(String path) throws IOException, Exception
{
 Properties sys_config = new Properties();
 try {
   String ref;
   BufferedReader in = null;
   FileInputStream file = null;

   if(path.startsWith("http:")) {
      URL iorURL = new URL(path);
      URLConnection conn = iorURL.openConnection ();
      in = new BufferedReader(
           new InputStreamReader(conn.getInputStream()));
   }
   else {
      file = new FileInputStream(path);
      in = new BufferedReader(new InputStreamReader(file));
   }

   while ((ref = in.readLine()) != null) {
     if (ref.startsWith("#") || ref.equals(""))
          continue;
     int starpos = ref.indexOf("*");
     if (starpos == -1)
         throw new Exception(ref);
     int eqpos = ref.indexOf("=", starpos);
     if (eqpos == -1)
        throw new Exception(ref);
     String module = ref.substring(0, starpos);
     String var = ref.substring(starpos + 1, eqpos);
     String val = ref.substring(eqpos + 1);
     
     if ((module.equalsIgnoreCase("drm")) ||
         (module.equalsIgnoreCase("common")))
         sys_config.put(var, val);
   }
   return sys_config;
 }catch(IOException ioe) {
    throw new IOException("File not found " + ioe.getMessage() + " " + path);
 } catch(Exception e) { throw e; }
}

public static ReportRequestStatus createAndConvertRequest
    (String surl, String turl, int status, long size) {
   Request request = new Request();
   request.setRequestId("srm-tester-gsiftp-test");

   ReportRequestStatus rrs = new ReportRequestStatus();
   rrs.setRequestId(request.getRequestId());

   ReportRequestFileStatus[] reportFileStatus = new ReportRequestFileStatus[1];
   ReportRequestFileStatus rfs = new ReportRequestFileStatus();

   if(status == 0) {
     rrs.setRequestState("Active");
     rfs.setState("Pending");
   }
   else if(status == 1) {
     rrs.setRequestState("Done");
     rfs.setState("Done");
   }
   else if(status == 2) {
     rrs.setRequestState("Failed");
     rfs.setState("Failed");
   }
   rfs.setSourceUrl(surl);
   rfs.setTargetUrl(turl);
   rfs.setTransferUrl("");
   if(size != -1) {
     rfs.setSize(""+size);
   } 

   reportFileStatus[0] = rfs;
   rrs.setReportRequestFileStatus(reportFileStatus);
   return rrs;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doOnlyGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static StringBuffer doOnlyGsiFTP(String ss, String tt,
    StringBuffer gsiFTPSTimeStamp,
    String publishUrlStr, String displayType, 
	String timeStamp, String fullTimeStamp, String startTimeStamp, 
	String operatorTimeStamp, String servicePath,
	GSSCredential credential, String proxyFile, 
	boolean noPublish, boolean onlyGsiFTPGet, 
	boolean defaultProxy, boolean dcau, 
    int bufferSize, int parallelism, String siteName, int numOperations, String typeString,
    boolean dailyTest, int numSites, boolean localPublish, 
	BufferedWriter bs, BufferedWriter bsa, 
	Log logger, PrintIntf pIntf, boolean useGUC, 
    String gucScriptPath, String scriptPathDir,OperOk pOk) throws Exception {

  StringBuffer gsiFTPETimeStamp = new StringBuffer();

  try {
    GlobusCredential gCreds = null;

    if(!defaultProxy) {
       gCreds = new GlobusCredential(proxyFile);
    }
    else {
      gCreds = new GlobusCredential("/tmp/x509up_u"+MyConfigUtil.getUID());
    }

    credential = new GlobusGSSCredentialImpl
            (gCreds, GSSCredential.INITIATE_AND_ACCEPT);
    UrlCopy ucopy = new UrlCopy();
    ucopy.setSourceCredentials(credential);
    ucopy.setDestinationCredentials(credential);
    ucopy.setDCAU(dcau);
    ucopy.setBufferSize(bufferSize);
    String ss1 = util.doValidateCheckingForFormat(ss);
    String tt1 = util.doValidateCheckingForFormat(tt);
    //String ss1 = ss;
    //String tt1 = tt;
    if(ss1.startsWith("file")) {
	  ss1 = "file:///"+util.parseLocalSourceFileForPath(ss1);
    }
    if(tt1.startsWith("file")) {
	  tt1 = "file:///"+util.parseLocalSourceFileForPath(tt1);
    }
    ucopy.setSourceUrl(new GlobusURL(ss1));
    ucopy.setDestinationUrl(new GlobusURL(tt1));
    util.printMessageHL("SRM-TESTER: " + new Date() +
		" Starting GSIFTP \n",logger,pIntf);
    //util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);
    util.printMessage ("SRM-TESTER: From="+ss1,logger,pIntf);
    util.printMessage ("SRM-TESTER: To="+tt1,logger,pIntf);
    try {
      ucopy.copy();
    }catch(Exception ee) {
      ByteArrayOutputStream bss = new ByteArrayOutputStream(); 
      PrintStream ps = new PrintStream(bss);
      ee.printStackTrace(ps);
      String str = bss.toString();
      int idx = str.indexOf("org.globus.ftp.FTPClient.close");
      if(idx == -1) {
        System.out.println(
			"SRM-TESTER: WARNING Java CoG gridftp throwed an exception " +
                ee.getMessage());
        if(useGUC) {
           if(pOk != null) {
             pOk.setExplanation("Failed");
           }
           util.printMessage("SRM-TESTER: Trying globus-url-copy now",logger,pIntf);
           runGUC(gucScriptPath,scriptPathDir,ss1,tt1,logger,pIntf,pOk,
				dcau,bufferSize,parallelism);
        }
        else {
          throw ee;
        }
      }
      else {
         System.out.println(
			"SRM-TESTER: WARNING Java CoG gridftp throwed the \"close\"" +
            " exception and it is handled here");
          ; //it is fine, the exception happended because of the close()
      }
    }
    //util.printMessageHL2("\nEndTime="+new Date()+"\n",logger,pIntf);
    util.printMessageHL("\nSRM-TESTER: " + new Date() +
		" GSIFTP completed.\n",logger,pIntf);

    gsiFTPETimeStamp = util.getTimeStamp();

    if(onlyGsiFTPGet) {
      try {
        util.webScript(publishUrlStr,
          displayType,"GridFTP","",
          util.createAndConvertRequest(ss,tt,0,-1), "",
          timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp, servicePath,noPublish,
          siteName,numOperations, "GridFTP","GridFTP",
		  "Sending",dailyTest,numSites,localPublish,bs,bsa);
      }catch(Exception e) { e.printStackTrace(); }

      long size = 0;
      int idx = tt.indexOf("file:///");
      if(idx != -1) {
        String zz = tt.substring(8);
        File zf = new File(zz);
        if(zf.exists()) {
          size = zf.length();
        }
      }
      try {
        File f = new File(tt);
        util.webScript(publishUrlStr,
         displayType,"GsiFTP","SUCCESS",
         util.gsiftpResultCreate(gsiFTPSTimeStamp, gsiFTPETimeStamp,size),
         "",timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
         siteName,numOperations,typeString,"","Ok",dailyTest,
		 numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"RequestSummary","SUCCESS",
         util.createAndConvertRequest(ss,tt,1,f.length()),
         "",timeStamp,fullTimeStamp, startTimeStamp,operatorTimeStamp, servicePath,noPublish,
         siteName,numOperations,typeString,"","Ok",dailyTest,
		 numSites,localPublish,bs,bsa);
      }catch(Exception we) { we.printStackTrace(); }
    }
  }catch(Exception e) {throw e;}

  return gsiFTPETimeStamp;
  
}

private static void runGUC(String gucScriptPath, String scriptPathDir,
  String source, String target, Log logger, PrintIntf pIntf,
	OperOk putOk, boolean dcau, int bufferSize, int parallelism) 
		throws Exception {

   //File script = File.createTempFile("script","",new File(scriptPathDir));
   //PrintWriter writer = new PrintWriter (new FileWriter(script,false),true);

   /*
   FileInputStream file = new FileInputStream(gucScriptPath);
   BufferedReader in = new BufferedReader(new InputStreamReader(file));
   String ref = "";
   while ((ref = in.readLine()) != null) {
     int idx = ref.indexOf("globus-url-copy");
     if(idx == -1) {
       writer.println(ref);
     }
   }
   writer.println("$GLOBUS_LOCATION/bin/globus-url-copy " + source
        + " " + target);
   writer.close();
   util.printMessage("SRM-TESTER: Wrote script file " + script.getAbsolutePath(),logger,pIntf);
   */

   /*
   boolean ok = false;
   Thread t = new Thread ();
   while(!ok) {
     try {
      if(script.exists()) {
         Process p0 = Runtime.getRuntime().exec("chmod 700 " + 
           script.getAbsolutePath());
         if(p0.waitFor() == 0) { 
           p0.destroy();
           util.printMessage("SRM-TESTER: Going to execute the script " + 
				script.getAbsolutePath(),logger,pIntf); 
           ExecScript2 process = new ExecScript2(script.getAbsolutePath(),
                  "",true);
           boolean b = process.execCommand(false);
           if(!b) { 
            //if(putOk != null) {
            // we can set the explanation  here later with the appropriate explanation
			//putOk.setExplanation("");
            //}
			throw new Exception("Error executing script " + script.getAbsolutePath()+ "," +
				"Please see the GUC error list");
           }
         }
         ok = true;
      }
     }catch(IOException ioe) {
        ok = false;
        t.sleep(1000);
     }catch(Exception e) {
        ok = true;
        //e.printStackTrace();
        throw e;
     }
   }//end while
   */
   //script.deleteOnExit();



   Thread t = new Thread ();
   File ss = new File(gucScriptPath);
   if(ss.exists()) {
      try {
           Vector vec = new Vector ();
           vec.addElement("-bs");
           vec.addElement(""+bufferSize);
           vec.addElement("-p");
           vec.addElement(""+parallelism);
           if(!dcau) { 
             vec.addElement("-nodcau");
           }
           vec.addElement(source);
           vec.addElement(target);
           String[] command = new String[vec.size()+1];
           command[0] = ss.getAbsolutePath();
           for(int i = 0; i < vec.size(); i++) {
             command[i+1] = (String)vec.elementAt(i);
           }  
           ExecScript2 process = new ExecScript2(ss.getAbsolutePath(),
                  "",true);
           boolean b = process.execCommand(command,false);
           if(!b) { 
			 throw new Exception("Error executing script " + ss.getAbsolutePath() + "," +
				" Please see the GUC error list");
             /*
             int idx = ss.getAbsolutePath().lastIndexOf("/");
             if(idx != -1) {
              try {
               StringBuffer buf = new StringBuffer();
                b = util.parseGUCOutput(ss.getAbsolutePath().substring(0,idx)+"/"+
					"output.log",buf);
               if(!b) {
			     throw new Exception(buf.toString());
               }
              }catch(Exception e) {
                 throw new Exception(e.getMessage());
              }
             }//end if(idx != -1)
             else {
			   throw new Exception("Error executing script " + ss.getAbsolutePath());
             }
             */
            //if(putOk != null) {
            // we can set the explanation  here later with the appropriate explanation
			//putOk.setExplanation("");
            //}
           }
      }
      catch(Exception e) {
          throw e;
      }
   }
   else {
     util.printMessage("Script does not exists " + scriptPathDir, logger,pIntf);
   }
}

public static String doValidateCheckingForFormat(String ss) 
  throws Exception {

   try {
     MyGlobusURL test = new MyGlobusURL(ss);
     String protocol = test.getProtocol();
     if(protocol.startsWith("file")) {
       return ss;
     }
     String host = test.getHost();
     int port = test.getPort();
     String path = test.getPath();
     String tempPath = path;
     int count = 0;
     while(true) {
       if(tempPath.startsWith("/")) {
         tempPath = tempPath.substring(1);
         count++;
       }
       else {
          break;
       }
     }
     //return protocol+"://"+host+":"+port+"//"+path;
     if(count == 0) {
       return protocol+"://"+host+":"+port+"//"+path;
     }
     else if(count == 1) {
       return protocol+"://"+host+":"+port+"/"+path;
     }
     else {
       return protocol+"://"+host+":"+port+path;
     }
   }catch(Exception e) {
     e.printStackTrace();
     throw new Exception("Not a valid URL/URI format " + ss);
   }
}

public static String parseLocalSourceFileForPath (String str)
  throws Exception {

  if(str.startsWith("file:////")) {
    return str.substring(8);
  }
  else if(str.startsWith("file:///")) {
    return str.substring(7);
  }
  else if(str.startsWith("file://")) {
    return str.substring(6);
  }
  else if(str.startsWith("file:/")) {
    return str.substring(5);
  }
  else if(str.startsWith("/")) {
    return str;
  }
  else {
    throw new Exception(
	"SRM-TESTER: parseLocalSourceFileForPath:SURL not in correct format " + str);
  }
}


public static StringBuffer getTimeStamp() {
  Calendar c = new GregorianCalendar();
  StringBuffer buf = new StringBuffer();
  int month = c.get(Calendar.MONTH)+1;
  buf.append(month+"-");
  buf.append(c.get(Calendar.DATE)+"-");
  buf.append(c.get(Calendar.YEAR)+":");
  buf.append(c.get(Calendar.HOUR_OF_DAY)+":");
  int minute = c.get(Calendar.MINUTE);
  buf.append(minute+":");
  int second = c.get(Calendar.SECOND);
  buf.append(second);
  return buf;
}

public static TStatusCode mapReturnStatusValueBackToCode(StringBuffer rCode) {

    if (rCode.toString().equals("SRM_AUTHENTICATION_FAILURE")) {
       return TStatusCode.SRM_AUTHENTICATION_FAILURE;
    }
    else if (rCode.toString().equals("SRM_AUTHORIZATION_FAILURE")) {
       return TStatusCode.SRM_AUTHORIZATION_FAILURE;
    }
    else if (rCode.toString().equals("SRM_INVALID_REQUEST")) {
       return TStatusCode.SRM_INVALID_REQUEST;
    }
    else if (rCode.toString().equals("SRM_SPACE_LIFETIME_EXPIRED")) {
       return TStatusCode.SRM_SPACE_LIFETIME_EXPIRED;
    }
    else if (rCode.toString().equals("SRM_EXCEED_ALLOCATION")) {
       return TStatusCode.SRM_EXCEED_ALLOCATION;
    }
    else if (rCode.toString().equals("SRM_NO_USER_SPACE")) {
       return TStatusCode.SRM_NO_USER_SPACE;
    }
    else if (rCode.toString().equals("SRM_NO_FREE_SPACE")) {
       return TStatusCode.SRM_NO_FREE_SPACE;
    }
    else if (rCode.toString().equals("SRM_NOT_SUPPORTED")) {
       return TStatusCode.SRM_NOT_SUPPORTED;
    }
    else if (rCode.toString().equals("SRM_ABORTED")) {
       return TStatusCode.SRM_ABORTED;
    }
    else if (rCode.toString().equals("SRM_REQUEST_SUSPENEDED")) {
       return TStatusCode.SRM_REQUEST_SUSPENDED;
    }
    else if (rCode.toString().equals("SRM_REQUEST_TIMED_OUT")) {
       return TStatusCode.SRM_REQUEST_TIMED_OUT;
    }
    else if (rCode.toString().equals("SRM_FAILURE")) {
       return TStatusCode.SRM_FAILURE;
    }
    else if (rCode.toString().equals("SRM_SUCCESS")) {
       return TStatusCode.SRM_SUCCESS;
    }
    else if (rCode.toString().equals("SRM_PARTIAL_SUCCESS")) {
       return TStatusCode.SRM_PARTIAL_SUCCESS;
    }
    else if (rCode.toString().equals("SRM_INVALID_PATH")) {
       return TStatusCode.SRM_INVALID_PATH;
    }
    else if (rCode.toString().equals("SRM_INTERNAL_ERROR")) {
       return TStatusCode.SRM_INTERNAL_ERROR;
    }
    else if (rCode.toString().equals("SRM_TOO_MANY_RESULTS")) {
       return TStatusCode.SRM_TOO_MANY_RESULTS;
    }
    else if (rCode.toString().equals("SRM_FILE_IN_CACHE")) {
       return TStatusCode.SRM_FILE_IN_CACHE;
    }
    else if (rCode.toString().equals("SRM_REQUEST_QUEUED")) {
       return TStatusCode.SRM_REQUEST_QUEUED;
    }
    else if (rCode.toString().equals("SRM_DUPLICATION_ERROR")) {
       return TStatusCode.SRM_DUPLICATION_ERROR;
    }
    return null;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getTargetFileName
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getTargetFileName(String tf) {

   int index = tf.indexOf("file:////");
   String temp = tf;
   if(index != -1) {
     temp  = "file:////"+tf.substring(9);
   }
   else {
     index = tf.indexOf("file:///");
     if(index != -1) {
      temp  = "file:////"+tf.substring(8);
     }
     else {
      index = tf.indexOf("file://");
      if(index != -1) {
        temp  = "file:////"+tf.substring(7);
      }
      else {
        index = tf.indexOf("file:/");
        if(index != -1) {
         temp  = "file:////"+tf.substring(6);
        }
      }
     }
  }
  return temp;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// formArrayOfAnyURI
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static ArrayOfAnyURI formArrayOfAnyURI(URI surl) {
    URI[] urlArray = new URI[1];
    urlArray[0] = surl;

    ArrayOfAnyURI aURI = new ArrayOfAnyURI();
    aURI.setUrlArray(urlArray);
    return aURI;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createReportRequestStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static ReportRequestStatus createReportRequestStatus (String surl,
  String turl, String spaceToken)
{
   ReportRequestStatus rrs = new ReportRequestStatus();

   ReportRequestFileStatus [] reportFileStatus =
        new ReportRequestFileStatus[1];

   ReportRequestFileStatus rrfs = new ReportRequestFileStatus();
   rrfs.setSourceUrl(surl);
   rrfs.setTargetUrl(turl);

   reportFileStatus[0] = rrfs;

   rrs.setReportRequestFileStatus(reportFileStatus);
   rrs.setSpaceToken(spaceToken);
   return rrs;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createBrowseRequestStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static BrowseRequestStatus createBrowseRequestStatus ()
{
   BrowseRequestStatus rrs = new BrowseRequestStatus();
   return rrs;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// connectToSRM
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static ISRM connectToSRM (String servicePath, StringBuffer proxyString,
		Log logger, PrintIntf pIntf, boolean defaultProxy, 
			GSSCredential credential, String proxyFile) 
			throws IOException, Exception
{
   ISRM service = null;
   try {
      util.printMessageHL("\nConnecting to ServicePath ...\n",
        logger,pIntf);
      service = util.getRemoteWSDLObject (servicePath,proxyString,logger,pIntf, 
			defaultProxy, credential, proxyFile);
   }catch(java.net.ConnectException ce) {
      util.printMessage("+++ Possible reason(s) ",logger,pIntf);
      util.printMessage("++++++ 1) SRM server may not be up.",logger,pIntf);
      util.printMessage
        ("++++++ 2) Check the parameters WSG host port and path",logger,pIntf);
      util.printMessage(" in the properties/srm.properties file.",logger,pIntf);
      throw ce;
   }catch(IOException ioe) {
      throw ioe;
   }catch(Exception e) {
      throw e;
   }
   return service;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRemoteWSDLObject
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static ISRM getRemoteWSDLObject(String servicePath,
    StringBuffer proxyString,
	Log logger, PrintIntf pIntf, 
	boolean defaultProxy, GSSCredential credential, String proxyFile) 
	  throws IOException, Exception
{
   ISRM srm = null;
   String host = "";
   int port = 0;
   String pathName = "";
   try {
     //ServiceContext context = new ServiceContext();
     GlobusURL sUrl = new GlobusURL(servicePath);
     host = sUrl.getHost();
     port = sUrl.getPort();
     pathName = sUrl.getPath();
     if(!pathName.startsWith("/")) {
       pathName = "/" + pathName;
     }

     URL uu = null;
     SimpleProvider provider = new SimpleProvider ();
     SimpleTargetedChain c = null;
     if(servicePath.startsWith("httpg")) {
      String protocols0 = System.getProperty("java.protocol.handler.pkgs");
      //System.out.println("protocol pkg handler: " + protocols0);
      //System.out.println(".....supporting: httpg");
      org.globus.net.protocol.httpg.Handler handler =
            new org.globus.net.protocol.httpg.Handler ();
      try {
        uu = new URL("httpg", host, port, pathName, handler);
      }catch(Exception h) {System.out.println(" does not work");}
      //System.out.println("  contacting fixed host : " + uu.toString());
      c = new SimpleTargetedChain(new GSIHTTPSender());
      provider.deployTransport("httpg",c);
     }
     else if(servicePath.startsWith("https")) {
      //System.out.println(".....supporting: https");
      c = new SimpleTargetedChain(new HTTPSSender());
      provider.deployTransport("https",c);
      uu = new URL(servicePath);
     }else {
      //System.out.println(".....always supports: https");
      c = new SimpleTargetedChain(new HTTPSender());
      provider.deployTransport("http",c);
      uu = new URL(servicePath);
     }

     Util.registerTransport( );
     SRMServiceLocator service = new SRMServiceLocator (provider);
     util.printMessageHL("Connecting to url " +
                  "httpg://"+host.trim()+":"+port+ pathName.trim(),
                        logger,pIntf);
     URL url = new URL
            ("httpg://"+host.trim()+":"+port+ pathName.trim());
     srm = service.getsrm(url);
     if(srm == null) {
       throw new Exception ("Could not connect to wsdlHandle " +
          "httpg://"+host.trim()+":"+port+"/"+pathName.trim());
     }
     GlobusCredential gCreds = null;
 
     if(!defaultProxy) {
       gCreds = new GlobusCredential(proxyFile);
     }
     else {
       gCreds = new GlobusCredential("/tmp/x509up_u"+MyConfigUtil.getUID());
     }
 
     util.printMessage("\nIssues=" + gCreds.getIssuer(), logger,pIntf);
     util.printMessage("Subject=" + gCreds.getSubject(), logger,pIntf);
     util.printMessage("Identity=" + gCreds.getIdentity(), logger,pIntf);
     util.printMessage("timeleft=" + gCreds.getTimeLeft(), logger,pIntf);

     credential = new GlobusGSSCredentialImpl
                        (gCreds, GSSCredential.INITIATE_AND_ACCEPT);

     ((org.apache.axis.client.Stub)srm)._setProperty
            (org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
             org.globus.gsi.gssapi.auth.NoAuthorization.getInstance());
     util.printMessage("++++ Setting setDelegationGSIProperties +++",
            logger,pIntf);
 
     ExtendedGSSCredential cred = (ExtendedGSSCredential) credential;
     byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
     proxyString.append(new String(bb));
 
     if(url.getProtocol().equals("httpg")) {
       util.printMessage("+++ Setting Full delegation mode +++",logger,pIntf);
       ((org.apache.axis.client.Stub)srm)._setProperty
            (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
             org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
       ((org.apache.axis.client.Stub)srm)._setProperty
          (org.globus.gsi.GSIConstants.AUTHZ_REQUIRED_WITH_DELEGATION,
                Boolean.FALSE);
       ((org.apache.axis.client.Stub)srm)._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,credential);

     }else {
       //context.setAuthentication((Stub)srm);
     }
   } catch(Exception e) {
      throw new Exception ("Could not connect to wsdlHandle " +
          "httpg://"+host.trim()+":"+port+"/"+pathName.trim() + " "  +
        e.getMessage());
   }
   return srm;
}

public static int startTimeStamp() {
  Date d = new Date();
  int minutes = d.getMinutes();
  int seconds = d.getSeconds();
  int currentTimeStamp = minutes*60+seconds;
  return currentTimeStamp;
}


public static boolean parseGUCOutput(String outputLogFile,
  StringBuffer buffer) throws Exception {

  FileInputStream file = new FileInputStream(outputLogFile);
  BufferedReader  in = new BufferedReader(new InputStreamReader(file));
  String ref="";
  while ((ref = in.readLine()) != null) {
    int idx = ref.indexOf("226 Transfer complete");
    int idx1 = ref.indexOf("Transfer complete");
    if(idx != -1 || idx1 != -1) {
     return true;
    }
    idx = ref.indexOf("error: a system call failed");
    if(idx != -1) {
      idx1 = ref.indexOf("No such file or directory");
      int idx2 = ref.indexOf("Permission denied");
      if(idx1 != -1 || idx2 != -1) {
        buffer.append(ref);
        return false;
      }
    }
    idx = ref.indexOf("error: the server sent an error response: 553");
    if(idx != -1) {
      idx1 = ref.indexOf("No such file or directory");
      int idx2 = ref.indexOf("Permission denied");
      if(idx1 != -1 || idx2 != -1) {
        buffer.append(ref);
        return false;
      }
    }
  }//end while
  return false;
}

public static boolean isRetryOk(int startTimeStamp, int retryAllowed) {
  Date d = new Date();
  int minutes = d.getMinutes();
  int seconds = d.getSeconds();
  int currentTimeStamp = minutes*60+seconds;
  if(currentTimeStamp < startTimeStamp) {
    currentTimeStamp = currentTimeStamp+startTimeStamp;
  }
  if(currentTimeStamp <= (startTimeStamp+retryAllowed)) {
     return true;
  }
  return false;
}


}
