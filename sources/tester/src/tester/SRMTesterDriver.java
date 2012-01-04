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
import java.lang.Thread;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMTesterDriver
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMTesterDriver implements threadIntf,testToolIntf
{
  private String[] _args;
  private String proxyFile="";
  private boolean defaultProxy=false;
  private boolean driverOn=false;
  private String sToken="";
  private boolean noPublish = true;
  private String authorizationID="";
  private Log logger = LogFactory.getLog(SRMTesterDriver.class.getName());
  private String log4jlocation="";
  private String logPath="";
  private String localTarget="";
  private String localPublishPath="";
  private boolean localPublishUrlGiven=false;
  private boolean publishUrlGiven=false;
  private String publishUrlStr=
	"http://sdm.lbl.gov/cgi-bin/srm-tester-v22/tester-report-gen.cgi";
  private String timeStamp="";
  private String displayType = "SRM-Tester-V22"; 
  private String _scriptPath="./gridcat-sites-ss.sh";
  private ExecScript eScript;
  private boolean verbose=false;
  private boolean cleanUpAllGiven=false;
  private Vector siteInfo=new Vector();
  private boolean gridCatEnquiryDone=false;
  private boolean dcau=true;
  private int bufferSize=1048576;
  private int parallelism=1;
  private String operations = "ping";
  private boolean useScript=false;
  //private String tableFile="conf/srm-tester-driver-input.txt";
  private int concurrency = 1;
  //private int tokenSize=0;
  private boolean advisoryDelete=true;
  private boolean copyAdvisoryDelete=true;
  private boolean releaseFile=true;
  private boolean detailed=false;
  private boolean dailyTest=false;
  private String gsiftpCopySourceFile="";
  private String pullCopySrms="";
  private String testsites="";
  private String putOverwrite="";
  private String copyOverwrite="";
  private String putSourceFile="";
  private String pushCopySources="";
  private int retryTimeAllowed=5; //5 minutes by default;
  private int statusWaitTime=30; //15 seconds by default;
  private String newSpaceSize="";
  private String newSpaceTime="";
  private String paramsFile = "srmtester";
  private boolean parseParams = false;
  private boolean abortOnFail=false;
  private String fileStorageType="";
  private int remoteFileLifeTime=0;
  private String gucScriptPath="";
  private String tempScriptPath="";

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMTesterDriver(String[] args) throws Exception
{ 
  _args = args;

  Calendar c = new GregorianCalendar();
  StringBuffer buf = new StringBuffer();
  int month = c.get(Calendar.MONTH)+1;
  if(month < 10) {
    buf.append("0"+month+"-");
  }
  else {
    buf.append(month+"-");
  }
  int day = c.get(Calendar.DATE);
  if(day < 10) {
    buf.append("0"+day+"-");
  }
  else {
    buf.append(day+"-");
  }
  buf.append(c.get(Calendar.YEAR)+":");
  int hour = c.get(Calendar.HOUR_OF_DAY);
  if(hour < 10) {
    buf.append("0"+hour+":");
  }
  else {
    buf.append(hour+":");
  }
  int minute = c.get(Calendar.MINUTE);
  if(minute < 10) {
    buf.append("0"+minute);
  }
  else {
    buf.append(minute);
  }

  timeStamp = buf.toString().trim();

  

  for(int i = 0; i < _args.length; i++) {
    if((_args[i].equalsIgnoreCase("-conf")) && i+1 <  _args.length) {
        paramsFile = _args[i+1];     
        parseParams=true;
        break;
    }
    else if(_args[i].equalsIgnoreCase("-version")) {
        SRMTester.printVersion();
    }
    else if(_args[i].equalsIgnoreCase("-help")) {
       showUsage();
    }
  }

  Properties values = new Properties ();

  /*
  if(!parseParams) {
     ResourceBundle rBundlevalues = ResourceBundle.getBundle(paramsFile);
     Enumeration e = rBundlevalues.getKeys();
     while(e.hasMoreElements()) {
       String key = (String) e.nextElement();
       try {
         String val = rBundlevalues.getString(key);
         if(val != null) { 
            values.put(key,val);          
         }
       }catch(MissingResourceException mre) {}
     }
  }
  else {
     values = parsefile(paramsFile);
  }
  */
  if(parseParams) {
    values = parsefile(paramsFile);
  }

  try {
     Object obj =  values.get("spacetoken");
     if(obj != null) {
       String tt = (String) obj; 
       sToken = tt;
     }

     obj =  values.get("localpublish");
     if(obj != null) {
       String tt = (String) obj; 
       localPublishPath = tt;
     } 

     obj =  values.get("retrytimeallowed");
     if(obj != null) {
       String tt = (String) obj; 
       try {
          retryTimeAllowed = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
     }

     obj =  values.get("statuswaittime");
     if(obj != null) {
       String tt = (String) obj; 
       try {
          statusWaitTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
     }


     obj =  values.get("cleanupall");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       advisoryDelete = b.booleanValue();
       copyAdvisoryDelete = b.booleanValue();
       cleanUpAllGiven = b.booleanValue();
     }

     obj =  values.get("abortonfail");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       abortOnFail = b.booleanValue();
     }

     obj =  values.get("releasefile");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       releaseFile = b.booleanValue();
     }

     obj =  values.get("detailed");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       detailed = b.booleanValue();
     }

     obj =  values.get("pullcopysrm");
     if(obj != null) {
       String tt = (String) obj;
       pullCopySrms = tt;
     }

     obj =  values.get("testsites");
     if(obj != null) {
       String tt = (String) obj;
       testsites = tt;
     }

     obj =  values.get("filestoragetype");
     if(obj != null) {
       String tt = (String) obj;
       fileStorageType = tt;
     }

     obj = values.get("putoverwrite");
     if(obj != null) {
        String tt = (String) obj;
        putOverwrite = tt;
     }

     obj = values.get("copyoverwrite");
     if(obj != null) {
        String tt = (String) obj;
        copyOverwrite = tt;
     }

     obj =  values.get("gsiftpcopysource");
     if(obj != null) {
       String tt = (String) obj;
       gsiftpCopySourceFile = tt;
     }

     obj =  values.get("publishurl");
     if(obj != null) {
       String tt  = (String) obj;
       publishUrlStr=tt;
       publishUrlGiven = true;
     }

     obj =  values.get("publish");
     if(obj != null) {
       String tt = (String) obj;
       noPublish = false;
     }

     obj =  values.get("localtargetdir");
     if(obj != null) {
       String tt = (String) obj;
       localTarget = tt;
     }

     obj =  values.get("newspacesize");
     if(obj != null) {
       String tt = (String) obj;
       newSpaceSize = tt;
     }

     obj =  values.get("newspacetime");
     if(obj != null) {
       String tt = (String) obj;
       newSpaceTime = tt; 
    }

     obj =  values.get("buffersize");
     if(obj != null) {
       String tt = (String) obj;
       try {
        int x = Integer.parseInt(tt);
        bufferSize = x;
       }catch(NumberFormatException nfe) {
          System.out.println("Given token size is not a valid integer " + tt);
	   }
     }

     obj =  values.get("parallelism");
     if(obj != null) {
       String tt = (String) obj;
       try {
        int x = Integer.parseInt(tt);
        parallelism = x;
       }catch(NumberFormatException nfe) {
          System.out.println("Given token size is not a valid integer " + tt);
	   }
     }

     obj =  values.get("nodcau");
     if(obj != null) {
       dcau=false;
     }  

     obj =  values.get("dailytest");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       dailyTest = b.booleanValue();
     }

     obj =  values.get("output");
     if(obj != null) {
       String tt = (String) obj;
       logPath = tt;
     }

     obj =  values.get("op");
     if(obj != null) {
       String tt = (String) obj;
       operations = tt;
     }

     obj =  values.get("concurrency");
     if(obj != null) {
       String tt = (String) obj;
       try {
          int ii = Integer.parseInt(tt);
          concurrency = ii;
       }catch(NumberFormatException nfe) { }
     }

     obj =  values.get("remotefilelifetime");
     if(obj != null) {
       String tt = (String) obj;
       try {
          int ii = Integer.parseInt(tt);
          remoteFileLifeTime = ii;
       }catch(NumberFormatException nfe) { }
     }

     obj =  values.get("proxyfile");
     if(obj != null) {
       String tt = (String) obj;
       proxyFile = tt;
       if(proxyFile != null && !proxyFile.equals("")) {
          defaultProxy = false;
       }
     }

     obj =  values.get("localsource");
     if(obj != null) {
       String temp = (String) obj;
       putSourceFile = temp;
     }

     obj =  values.get("pushcopysource");
     if(obj != null) {
       String temp = (String) obj;
       pushCopySources = temp;
     }

     obj =  values.get("gucscriptpath");
     if(obj != null) {
       String temp = (String) obj;
       gucScriptPath = temp;
     }

     obj =  values.get("tempscriptpath");
     if(obj != null) {
       String temp = (String) obj;
       tempScriptPath = temp;
     }

     /*
     obj =  values.get("drive");
     if(obj != null) {
       String temp = (String) obj;
       tableFile = temp;
     }
     */

  }catch(Exception e) {
     e.printStackTrace();
  }

  
  for(int i = 0; i < _args.length; i++) {
     //System.out.println("ARGS[i]"+_args[i]);
     if(_args[i].equals("-proxyfile") && i+1 < _args.length) {
        proxyFile = _args[i+1];
	    i++;
     }
     else if((_args[i].equalsIgnoreCase("-op")) && i+1 <  _args.length) {
        operations = _args[i+1];
     }
     else if(_args[i].equals("-drive")) {
        driverOn=true;
     }
     else if(_args[i].equals("-usedriver")) {
     }
     else if(_args[i].equals("-direct")) {
     }
     else if((_args[i].equals("-concurrency")) && i+1 < _args.length) {
        try { 
          int ii = Integer.parseInt(_args[i+1]);
          concurrency = ii;
        }catch(NumberFormatException nfe) {
          System.out.println("\nGiven concurrency is not a valid integer " +	
             "using default value "); 
		}
        i++; 
     }
     else if((_args[i].equals("-remotefilelifetime")) && i+1 < _args.length) {
        try { 
          int ii = Integer.parseInt(_args[i+1]);
          remoteFileLifeTime = ii;
        }catch(NumberFormatException nfe) { }
        i++; 
     }
     else if(_args[i].equals("-localtargetdir") && i+1 < _args.length) {
        localTarget = _args[i+1];
	    i++;
     }
     else if(_args[i].equals("-gucscriptpath") && i+1 < _args.length) {
        gucScriptPath = _args[i+1];
	    i++;
     }
     else if(_args[i].equals("-s") && i+1 < _args.length) {
	    i++;
     }
     else if(_args[i].equals("-publish"))  {
        noPublish = false;
     }
     else if(_args[i].equals("-abortonfail"))  {
        abortOnFail = true;
     }
     else if(_args[i].equals("-publishurl") && i+1 < _args.length) {
        publishUrlGiven=true;
        publishUrlStr=_args[i+1];
	    i++;
     }
     else if(_args[i].equals("-localpublish") && i+1 < _args.length) {
        localPublishUrlGiven=true;
        localPublishPath=_args[i+1];
	    i++;
     }
     else if(_args[i].equals("-newspacesize") && i+1 < _args.length) {
        newSpaceSize =_args[i+1];
	    i++;
     }
     else if(_args[i].equals("-newspacetime") && i+1 < _args.length) {
        newSpaceTime=_args[i+1];
	    i++;
     }
     else if(_args[i].equals("-dailytest")) {
        dailyTest = true;
     }
     /*
     else if(_args[i].equalsIgnoreCase("-output") && i+1 < _args.length) {
        logPath = _args[i+1];
	    i++;
     }
     */
     else if(_args[i].equalsIgnoreCase("-scriptpath") && i+1 < _args.length) {
        _scriptPath = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-gsiftpcopysource") && i+1 < _args.length) {
        gsiftpCopySourceFile = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-pullcopysrm") && i+1 < _args.length) {
        pullCopySrms = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-testsites") && i+1 < _args.length) {
        testsites = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-filestoragetype") && i+1 < _args.length) {
        fileStorageType = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-localsource") && i+1 < _args.length) {
        putSourceFile = _args[i+1];
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-pushcopysource") && i+1 < _args.length) {
        pushCopySources = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-cleanupall")) {
        if(i+1 < _args.length) {
          String tt = _args[i+1];
          Boolean b = new Boolean(tt);
          advisoryDelete=b.booleanValue();
          copyAdvisoryDelete=b.booleanValue();
          cleanUpAllGiven = b.booleanValue();
        }
        else {
          advisoryDelete = true;
          copyAdvisoryDelete = true;
          cleanUpAllGiven = true;
        }
     }
     else if((_args[i].equalsIgnoreCase("-releasefile"))
        && i+1 < _args.length) {
       String tt = _args[i+1];
       Boolean b = new Boolean(tt);
       releaseFile=b.booleanValue();
     }
     else if(_args[i].equalsIgnoreCase("-detailed")) {
        detailed = true;
     }
     else if(_args[i].equalsIgnoreCase("-verbose")) {
        verbose=true; 
     } 
     else if(_args[i].equalsIgnoreCase("-usescript")) {
        useScript=true; 
     } 
     else if((_args[i].equalsIgnoreCase("-conf")) && i+1 < _args.length) {
        i++;
     }
     else {
       showUsage();
     }
  }

  //System.out.println("Log4jlocation " + log4jlocation);

  String ttemp = System.getProperty("log4j.configuration");
  if(ttemp != null && !ttemp.equals("")) {
    log4jlocation = ttemp;
  }

  /*
  String ttemp = System.getProperty("log4j.configuration");
  if(ttemp != null && !ttemp.equals("")) {
    log4jlocation = ttemp;
  }
  else {
    log4jlocation = "log4j/log4j_srmtester.properties";
  }
  */

  /*
  if(!logPath.equals("")) {

    String detailedLogPath = logPath+"/"+"srm_tester_detailed.log";

    //rewrite the log4j conf file with the new log path

    try {

      String ref;
      FileInputStream file = new FileInputStream (log4jlocation);
      BufferedReader in =
          new BufferedReader ( new InputStreamReader (file));

      FileOutputStream outFile =
         new FileOutputStream(logPath+"/log4j_srmtester.properties");

      BufferedWriter out =
         new BufferedWriter(new OutputStreamWriter(outFile));

      while ((ref= in.readLine()) != null) {
        if(ref.startsWith("log4j.appender.GRIDCATTEST.File")) {
           out.write("log4j.appender.GRIDCATTEST.File="+detailedLogPath+"\n");
        }
        else {
           out.write(ref+"\n");
        }
      }

      in.close();
      if(file != null) file.close();

      out.close();
      if(outFile != null) outFile.close();

    }catch(IOException ex) {
      throw new IOException("cannot read from " + ex.getMessage());
    }
    log4jlocation=logPath+"/log4j_srmtester.properties";
   }
   */

   try {
   PropertyConfigurator.configure(log4jlocation);
   }catch(Exception ee){;}

   /*
   ClassLoader cl = this.getClass().getClassLoader();

   try {
     Class cc = cl.loadClass("SRMTesterDriver");
     logger = LogFactory.getLog(cc.getName());
   }catch(ClassNotFoundException cnfe) {
      System.out.println("ClassNotFoundException" + cnfe.getMessage());
   }
   */

  if(proxyFile.equals("")) {
    defaultProxy=true; 
  }

  if(useScript) {
    String[] cmd = new String[2];
    cmd[0]=_scriptPath;
    cmd[1]="--testvo=ivdgl";

    eScript = new ExecScript(_scriptPath,verbose,logger,this,this);
   
    ThreadCallBack thread_call_back = new ThreadCallBack(this,"execscript");
    thread_call_back.setParams_1(cmd);
    thread_call_back.start();
  }
  else {
    /*
    try {
      FileInputStream fis = new FileInputStream(tableFile);
      BufferedReader bis = new BufferedReader(new InputStreamReader(fis));
      String ref = null;
      while ((ref = bis.readLine()) != null) {
        if (ref.equals(""))
            continue;
        if (ref.startsWith("#"))
            continue;
       SiteInfo siteInfo = new SiteInfo(ref);
       addInfo(siteInfo);
      }
	  bis.close();
      fis.close();
      doSiteInfoTest();
    }catch(IOException ioe) {
       util.printMessage(ioe.getMessage(), logger,null);
       throw new Exception(ioe.getMessage()); 
    }
    */
    if(testsites.equals("")) {
      System.out.println("\nPlease provide the siteinformation in the testsites variable");
      System.exit(1);
    }
    if(pullCopySrms.equals("")) {
      System.out.println("\nPlease provide the siteinformation in the pullCopySrm variable");
      System.exit(1);
    }
    else {
       StringTokenizer testSitesInfo = new StringTokenizer(testsites,",");
       while(testSitesInfo.hasMoreTokens()) {
         String tSites = testSitesInfo.nextToken();
         StringTokenizer stk = new StringTokenizer(pullCopySrms,",");
         while(stk.hasMoreTokens()) {
           String temp = stk.nextToken();
           int idx = temp.indexOf("@");
           if(idx != -1) {
             if(tSites.equals(temp.substring(0,idx))) {
               SiteInfo siteInfo = new SiteInfo(temp);
               addInfo(siteInfo);
               break;
             }
           }
         }
       }
         
       if(!putOverwrite.equals("")) { 
         StringTokenizer stk = new StringTokenizer(putOverwrite,",");
         while(stk.hasMoreTokens()) { 
           String temp = stk.nextToken();
           int idx = temp.indexOf("@");
           if(idx != -1) {
             String siteLocation = temp.substring(0,idx);
             Vector siteVec = getSiteInfo();
             for(int i = 0; i < siteVec.size(); i++) {
                SiteInfo sInfo = (SiteInfo)siteVec.elementAt(i);
                if(sInfo.getLocation().equals(siteLocation)) {
                   sInfo.setPutOverwrite(temp.substring(idx+1).trim());
                }
             }
           }
         }
       }
       if(!copyOverwrite.equals("")) { 
         StringTokenizer stk = new StringTokenizer(copyOverwrite,",");
         while(stk.hasMoreTokens()) { 
           String temp = stk.nextToken();
           int idx = temp.indexOf("@");
           if(idx != -1) {
             String siteLocation = temp.substring(0,idx);
             Vector siteVec = getSiteInfo();
             for(int i = 0; i < siteVec.size(); i++) {
                SiteInfo sInfo = (SiteInfo)siteVec.elementAt(i);
                if(sInfo.getLocation().equals(siteLocation)) {
                   sInfo.setCopyOverwrite(temp.substring(idx+1).trim());
                }
             }
           }
         }
       }
       doSiteInfoTest();
    }
  }
}   

 public void addInfo(SiteInfo sInfo) {
   siteInfo.addElement(sInfo);
 }

 public Vector getSiteInfo() {
   return siteInfo;
 }

 public void processThreadRequest_1(String[] command) {
   eScript.execCommand(command);
 }

 public void processThreadRequest_3(String[] args,SharedObjectLock sLock) {
    SRMTester srmTester = new SRMTester(args,false,logger,null);
    srmTester.startTester();
    if(sLock != null) {
     sLock.setIncrementCount();
    }
 }

 public void processThreadRequest_2(String type, Hashtable cmd) {
 } 

 public void processGridFTPWaitTimeOut(Hashtable cmd) {
 }


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void showUsage () {
  System.out.println("++++++++++++++++++++++++++++++++++++++++++");
  System.out.println("Usage : \n" +
            "\t-proxyfile    \tproxyfile location \n" +
            //"\t-output       \tlocation to output log file \n" +
            //"                \t(default is /tmp/srm-xxx-userlogin.log)\n" + 
            "\t-t            \t<local targetUrl>\n" +
            "\t-publish  \tNot Publish to web site (default it publishes)\n"+
            //"\t-publishurl \t<web site url to publish report>\n" +
	        //"\t  (default: publishes to http://sdm.lbl.gov/cgi-bin/srm-tester/tester-report-gen.cgi)\n" +
            "\t-localpublish \t<url to publish report locally>\n" +
            "\t-usescript   (true | false) \n" +
	        "\t  (default: false (uses table instead) \n" +
            "\t-scriptpath   \t<path to gridcat script>\n" +
	        "\t  (default: ./gridcat-sites-ss.sh\n" +
            "\t-putsource  \t<PutSourceURL> (required for put operation)\n" +
            "\t-copysource  \t<CopySourceURL> (required for copy operation)\n" +
            "\t-cleanupall  \t<true|false> (default: false)\n" +
            "\t-abortonfail  \t(abort file when gsiftp is failed for put)\n" +
            "\t-releasefile  \t<true|false> (default: true)\n" +
            "\t-detailed  \t<true|false> (default: true)\n" +
            "\t-op  \t(list of operations) (defalt: ping)\n" +
		    "\t (ping,prepareToGet,releaseFile,prepareToPut,putDone,releaseFile,copy,reserveSpace,reserveSpacePut,releaseSpace| all )\n" +
            //"\t-tablepath   \t<path to table file name>\n" +
	        "\t  (default: ./site-table.txt\n" +
            //"\t-tokensize   \t<valid integer>\n" +
            //"\t-dailytest \t(default:false)\n" +
            "\t-conf   \t<name of different params file>\n" +
	        "\t  (default: properties/srmtester.properties\n" +
            "\t-verbose      \t<true | false> (default:false) show detailed output\n" +
            "\t-help         \t<true | false (show this message)>\n");
   System.exit(1);
}


 public void setDone() {
   gridCatEnquiryDone = true;
   doSiteInfoTest();
 }

private void doSiteInfoTest () {
 Vector siteVec = getSiteInfo();
  
 for(int k = 0; k < siteVec.size(); k=k+concurrency) {
   //System.out.println("k="+k + " " + siteVec.size());
   for(int i = k; i < k+concurrency; i++) {
   //System.out.println("i="+i) ;
     SiteInfo sInfo = (SiteInfo) siteVec.elementAt(i);
     String protocol = sInfo.getProtocol();
     String host = sInfo.getHost();
     int port = sInfo.getPort();
     String path = sInfo.getRootDir();
     String servicePath = sInfo.getServicePath();
     String pathInfo = sInfo.getPathInfo();
     if(pathInfo.equals("")) {
       Random generator = new Random();
       double r = generator.nextDouble();
       pathInfo = "srmtester-put-test.dat."+r;
     }
     String siteName = sInfo.getLocation();
     String dirInfo = sInfo.getDirInfo();
     if(dirInfo.equals("")) {
       Random generator = new Random();
       double r = generator.nextDouble();
       dirInfo = "testdir."+r;
     }
     boolean disableAll = sInfo.getDisableAll();
     boolean disableBringOnLine = sInfo.getDisableBringOnline();
     boolean disableCopyAll = sInfo.getDisableCopyAll();
     boolean disableCopyGsiFTP = sInfo.getDisableCopyGsiFTP();
     boolean disableCopyPull = sInfo.getDisableCopyPull();
     boolean putSiteOverwrite=sInfo.getPutOverwrite();
     boolean copySiteOverwrite=sInfo.getCopyOverwrite();

     /*
     System.out.println("SiteName="+siteName);
     System.out.println("Protocol="+protocol);
     System.out.println("Host="+host);
     System.out.println("Port="+port);
     System.out.println("ServicePath="+servicePath);
     System.out.println("DirInfo="+dirInfo);
     System.out.println("putOverwrite="+putSiteOverwrite);
     System.out.println("copyOverwrite="+copySiteOverwrite);
     */

     Vector vec = new Vector();
     
     if(protocol.equals("srm")) {
       if(!path.equals("") && !pathInfo.equals("")) {
         if(disableAll) {
           util.printMessage("Skipping this siteInfo. " +
		  	  "since all its operations are currently disabled " + siteName,logger,null);
         }
         else {
           if(disableBringOnLine) {
             vec.addElement("-disablebringonline"); 
           }
           if(disableCopyAll) {
             vec.addElement("-disablecopyall"); 
           }
           if(disableCopyGsiFTP) {
             vec.addElement("-disablecopygsiftp"); 
           }
           if(disableCopyPull) {
             vec.addElement("-disablecopypull"); 
           }
           vec.addElement("-serviceurl"); 
           vec.addElement("srm://"+host+":"+port+servicePath); 
           if(!defaultProxy) {
             vec.addElement("-proxyfile");
             vec.addElement(proxyFile);
           }
           //vec.addElement("-output");
           //vec.addElement(logPath);
           vec.addElement("-op");
           vec.addElement(operations);
           //vec.addElement("-tokensize");
           //vec.addElement(""+tokenSize);
           vec.addElement("-gsiftpcopysource");
           vec.addElement(gsiftpCopySourceFile);
           vec.addElement("-sequencecopy");
           vec.addElement("-localsource");
           vec.addElement(putSourceFile);
           if(remoteFileLifeTime != 0) {
             vec.addElement("-remotefilelifetime");
             vec.addElement(""+remoteFileLifeTime);
           }
           vec.addElement("-pullcopysrm");
           vec.addElement(pullCopySrms);
           vec.addElement("-testsites");
           vec.addElement(testsites);
           vec.addElement("-filestoragetype");
           vec.addElement(fileStorageType);
           vec.addElement("-pushcopysource");
           vec.addElement(pushCopySources);
           vec.addElement("-remotesfn");
           vec.addElement ("?SFN="+path+"/"+pathInfo);
           if(!putSiteOverwrite) {
             vec.addElement("-nooverwrite"); 
             vec.addElement("-nocopyoverwrite"); 
           }
           if(!copySiteOverwrite) {
           }
           if(abortOnFail) {
             vec.addElement("-abortonfail");
           }
           /*
           if(driverOn) {
             vec.addElement("-deleteafterput"); 
             vec.addElement("false"); 
             vec.addElement("-deleteaftercopy");
             vec.addElement("false");
           }
           */
           if(releaseFile) {
             vec.addElement("-releasefile");
             vec.addElement("true");
           }
           if(!sToken.equals("")) {
             vec.addElement("-spacetoken");
             vec.addElement(sToken);
           }
           if(!authorizationID.equals("")) {
             vec.addElement("-authid");
             vec.addElement(authorizationID);
           }
           vec.addElement("-buffersize");
           vec.addElement(""+bufferSize);
           vec.addElement("-parallelism");
           vec.addElement(""+parallelism);
           if(!dcau) {
             vec.addElement("-nodcau");
           }    
           if(detailed) {
             vec.addElement("-detailed");
           }
           vec.addElement("-localtargetdir");
           vec.addElement(localTarget);
           vec.addElement("-srmtester");
           vec.addElement("-sitename");
           vec.addElement(siteName);
           if(localPublishUrlGiven) {
             vec.addElement("-localpublish");
             vec.addElement(localPublishPath);
           }
           if(!noPublish) {
             vec.addElement("-publish");
           }
           if(publishUrlGiven) {
             vec.addElement("-publishurl");
             vec.addElement(publishUrlStr);
           }
           if(dailyTest) {
            vec.addElement("-dailytest");
            vec.addElement("-numsites");
            vec.addElement(""+siteVec.size());
           }
           vec.addElement("-totalsites");
           vec.addElement(""+siteVec.size());
           vec.addElement("-currsite");
           vec.addElement(""+i);
           vec.addElement("-statuswaittime");
           vec.addElement(""+statusWaitTime);
           vec.addElement("-retrytimeallowed");
           vec.addElement(""+retryTimeAllowed);
           vec.addElement("-starttimestamp");
           vec.addElement(timeStamp);
           if(!newSpaceSize.equals("")) {
             vec.addElement("-newspacesize");
             vec.addElement(newSpaceSize);
           }
           if(!newSpaceTime.equals("")) {
             vec.addElement("-newspacetime");
             vec.addElement(""+newSpaceTime);
           } 
           if(parseParams) {  
             vec.addElement("-conf");
             vec.addElement(paramsFile);
           }
           if(driverOn) {
             vec.addElement("-usedriver");
           }
           if(cleanUpAllGiven) {
             vec.addElement("-cleanupall");
           }
           vec.addElement("-gucscriptpath");
           vec.addElement(gucScriptPath);
           //vec.addElement("-tempscriptpath");
           //vec.addElement(tempScriptPath);
           Object[] oo = vec.toArray(); 
           String[] args = new String[oo.length];
           for(int j = 0; j < oo.length; j++) {
             args[j] = (String) oo[j];
           }
           ThreadCallBack thread_call_back = 
				new ThreadCallBack(this,"srmtester");
           thread_call_back.setParams_1(args);
           SharedObjectLock sLock = null; 
           //System.out.println("i == " +(i+concurrency-1));
           if(i == (i+concurrency-1)) {
              sLock = new SharedObjectLock(1,logger,null); 
           }  
           thread_call_back.setParams_3(sLock);
           //System.out.println("Before starting");
           thread_call_back.start();
           if(sLock != null) {
             sLock.get();
           }
         }
       }
       else {
         util.printMessage("Skipping this  siteInfo. " +
			"path or pathInfo is null",logger,null);
       }
     }

     vec.clear();

     if(!path.equals("") && !pathInfo.equals("") && 
		protocol.equals("gsiftp")) {
       vec.addElement("-serviceurl"); 
       vec.addElement("gsiftp://"+host+":"+port); 
       if(!defaultProxy) {
         vec.addElement("-proxyfile");
         vec.addElement(proxyFile);
       }
       //vec.addElement("-output");
       //vec.addElement(logPath);
       vec.addElement("-op");
       vec.addElement("get");
       vec.addElement("-onlygsiftpget");
       vec.addElement("-getsource");
       vec.addElement
		("gsiftp://"+host+":"+port+"/"+path+"/"+pathInfo);
       vec.addElement("-gettarget");
       vec.addElement(localTarget);
       if(!dcau) {
         vec.addElement("-nodcau");
       }    
       vec.addElement("-srmtester");
       Object[] oo = vec.toArray(); 
       String[] args = new String[oo.length];
       for(int j = 0; j < oo.length; j++) {
          args[j] = (String) oo[j];
       }

       ThreadCallBack thread_call_back = 
		 new ThreadCallBack(this,"srmtester");
       thread_call_back.setParams_1(args);
       thread_call_back.start();
     }

     /*
     System.out.println("\n================================");
     System.out.println("siteId " + sInfo.getSiteId());
     System.out.println("serviceId " + sInfo.getServiceId());
     System.out.println("command " + sInfo.getCommand());
     System.out.println("protocol " + protocol);
     System.out.println("host " + host);
     System.out.println("port " + port);
     System.out.println("rootdir " + path);
     System.out.println("================================");
     */
   }
  }
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parsefile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Properties parsefile(String path) throws IOException,
    java.util.zip.DataFormatException, Exception
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
         int eqpos = ref.indexOf("=");
         if (eqpos == -1)
             throw new java.util.zip.DataFormatException(ref);
         String var = ref.substring(0, eqpos);
         String val = ref.substring(eqpos + 1);

         sys_config.put(var, val);
      }

       if(file != null) file.close();
   } catch(IOException ex) {
       throw new IOException("SRMTesterDriver.parseFile: Can't read from `" +
          ex.getMessage() + "'");
   } catch (java.util.zip.DataFormatException de) {
       throw new java.util.zip.DataFormatException
            ("SRMTesterDriver.parseFile: Invalid format " +
            de.getMessage() + "'");
   }
   return sys_config;
}

 



 public static void main(String[] args) {
  try {
   SRMTesterDriver test = new SRMTesterDriver(args);
  }catch(Exception e) {
    e.printStackTrace();
  }
 }

}

