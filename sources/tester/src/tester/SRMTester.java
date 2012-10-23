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

import javax.xml.rpc.Stub;

import org.globus.io.urlcopy.UrlCopy;
import org.globus.util.GlobusURL;

//import org.globus.ogsa.utils.GSIUtils;
import org.globus.axis.gsi.GSIConstants;
//import org.globus.ogsa.impl.security.authentication.Constants;

import org.globus.util.ConfigUtil;
//import org.globus.ogsa.utils.GetOpts;
//import org.globus.ogsa.gui.ServiceContext;
import org.globus.gsi.X509Credential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.gsi.gssapi.auth.*;

import org.gridforum.jgss.ExtendedGSSManager;
import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;
//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import gov.lbl.srm.client.main.MySRMFileTransfer;
import gov.lbl.srm.client.intf.PrintIntf;

//import gov.lbl.srm.server.TSRMUtil;

import javax.xml.rpc.Stub;
import org.apache.axis.types.URI;
import org.apache.axis.types.UnsignedLong;


import javax.xml.rpc.Stub;
import org.apache.axis.types.*;
import org.apache.axis.configuration.SimpleProvider;
import org.apache.axis.SimpleTargetedChain;
import org.globus.axis.transport.*;
import org.apache.axis.transport.http.HTTPSender;
import org.globus.axis.util.Util;


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMTester
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMTester implements threadIntf
{
  private String[] _args;
  private String servicePath ="";
  private String remoteSFN ="";
  private String sourceFile ="";
  private String getSourceFile="";
  private String copySourceFile ="";
  private String pullCopySrms ="";
  private String putSourceFile="";
  private String pushCopySourceFile="";
  private String pullCopySourceFile="";
  private String gsiftpCopySourceFile ="";
  private String localTarget="";
  private String remoteTarget="";
  private String copyTarget="";
  private String moveTarget="";
  private String browseUrl="";
  private String dirUrl="";
  private String removeUrl="";
  private String origRemoveUrl="";
  private String proxyFile="";
  private String startTimeStamp="";
  private String operationsGiven="";
  private boolean defaultProxy=true;
  private boolean overwrite=true;
  private boolean copyoverwrite=true;
  private boolean reserveSpace=false;
  private boolean browseAfterPut=false;
  private boolean dcau=true;
   boolean isPutGiven= false;
   boolean isGetGiven=false;
   boolean isPingGiven=false;
   boolean isBrowseGiven=false;
   boolean isCopyGiven=false;
   boolean isMvGiven=false;
   boolean isMkdirGiven=false;
   boolean isRmdirGiven=false;
   boolean isRmGiven=false;
   boolean isBringOnlineGiven=false;
   boolean isReserveSpaceGiven=false;
   boolean isReleaseSpaceGiven=false;
   boolean isReleaseGiven=false;
   boolean isGetSpaceMetaGiven=false;
   boolean isGetSpaceTokensGiven=false;
   boolean isGetTransferProtocolsGiven=false;
  private boolean disableBringOnLine=false;
  private boolean disableCopyPull=false;
  private boolean disableCopyAll=false;
  private boolean disableCopyGsiFTP=false;
  private int bufferSize=1048576;
  private int parallelism=1;
  private Log logger;
  private String log4jlocation="";
  private String logPath="";
  private String testType="-srm";
  private String spaceToken="";
  private long newSpaceSize; 
  private int newSpaceTime;
  private String requestToken="";
  private String typeString="all";
  private String displayType="";
  private boolean cleanUpAllGiven;
  private String publishUrlStr=
	"http://sdm.lbl.gov/cgi-bin/srm-tester-v22/tester-report-gen.cgi";
  private boolean dailyTest=false;
  private int numSites;
  private boolean isPublishUrlGiven=false;
  private StringBuffer proxyString= new StringBuffer();
  private String timeStamp="";
  private String fullTimeStamp="";
  private boolean noPublish = true;
   //by default it does not publish;
  private boolean onlyGsiFTPGet=false;
  private ISRM srm;
  private String uid="";
  private String spacetoken="";
  //private int tokenSize=0;
  private boolean advisoryDelete=false;
  private boolean copyAdvisoryDelete=false;
  private boolean releaseFile=true;
  private boolean pushMode=false;
  private boolean changeToCopyServicePath=false;
  private ThreadCallBack tcb;
  private PrintIntf pIntf = null;
  private boolean isGsiFTPCopyGiven=false;
  private boolean isPullCopyGiven=false;
  private boolean isPushCopyGiven=false;
  private boolean pullCopyStandAloneGiven=true;
  private StringBuffer gsiFTPSTimeStamp= new StringBuffer();
  private StringBuffer gsiFTPETimeStamp = new StringBuffer();
  private GSSCredential credential;
  private String copyServicePath="";
  private boolean detailed=false;
  private int givenTestCondition = -1;
  private int type= 0; // doing all tests, get, copy, put, reserve space, release space
  private int retryTimeAllowed=5*60; //5 minutes default
  private int statusWaitTime=30; //15 seconds default
  private int remoteFileLifeTime=0;
  private int desiredFileLifeTime=0;
  private Vector paramVec = new Vector();
  private String siteName="";
  private int numOperations=0;
  private String localPublishPath="";
  private boolean localPublish;
  private FileOutputStream ostream;
  private BufferedWriter bs;
  private FileOutputStream ostreamForArchive;
  private BufferedWriter bsa;
  private boolean driverOn=false;
  private boolean useDriverOn=false;
  private Boolean canGetContinue = new Boolean(true);
  private OperOk isBringOnlineSuccess = new OperOk();
  private OperOk isGetSuccess = new OperOk();
  private OperOk isPutSuccess = new OperOk();
  private OperOk isLsSuccess = new OperOk();
  private OperOk isCopySuccess = new OperOk();
  private OperOk isReserveSpaceSuccess = new OperOk();
  private OperOk isReleaseSpaceSuccess = new OperOk();
  private OperOk isGetSpaceTokensSuccess = new OperOk();
  private OperOk isGetRequestTokensSuccess = new OperOk();
  private OperOk isGetSpaceMetadataSuccess = new OperOk();
  private OperOk isGetTransferProtocolsSuccess = new OperOk();
  private OperOk isMvSuccess = new OperOk();
  private OperOk isMkdirSuccess = new OperOk();
  private OperOk isRmdirSuccess = new OperOk();
  private OperOk isPingSuccess = new OperOk();
  private OperOk isPingOverAllSuccess = new OperOk();
  private OperOk isRmSuccess = new OperOk();
  private boolean parseParams = false;
  private boolean directGsiFTP=false;
  private boolean abortOnFail=false;
  private boolean pushModeGiven=false;
  private String fileStorageType="";
  private Vector srmLsVec = new Vector ();
  private Vector srmReleaseVec = new Vector ();
  private Vector srmRmVec = new Vector ();
  private String testsites="";
  private boolean calledLocalCopyOnce=false;
  private String gucScriptPath="";
  private String tempScriptPath="";
  private boolean useGUC;
  private TimeOutCallBack timeOutCallBack;
  private int totalSites;
  private int currSite;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMTester () {
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public SRMTester(String[] args, boolean setLog, Log useThisLogger, 
	PrintIntf pIntf) 
{ 
  _args = args;
  this.pIntf = pIntf;

  String paramsFile = "srmtester";
  typeString = "all";


  String ltemp =  System.getProperty("log4j.configuration");
  if(ltemp != null && !ltemp.equals("")) {
    log4jlocation = ltemp;
  } 
  
  /*
  String temp =  System.getProperty("log4j.configuration");
  System.out.println("log4j.configuration property is " + temp);
  if(temp != null && !temp.equals("")) {
    log4jlocation = temp;
  } else {
    log4jlocation = "properties/log4j.properties";
  } 
  */
  
  for(int i = 0; i < _args.length; i++) {
     if(_args[i].equals("-conf") && i+1 < _args.length) {
       paramsFile=_args[i+1];
       parseParams = true;
       break;
     }
     if(_args[i].equals("-help")) {
      showUsage(false);
     }
  }

  Properties values = new Properties ();

  /*
  if(!parseParams) {
     ResourceBundle rBundleValues = ResourceBundle.getBundle(paramsFile);
     Enumeration e = rBundleValues.getKeys();
     while(e.hasMoreElements()) {
       String key = (String) e.nextElement();
       try {
         String val = rBundleValues.getString(key);
         if(val != null) {
            values.put(key,val);
         }
       }catch(MissingResourceException mre) {}
     }
  }
  */
  if(parseParams) {
    try {
     values = parsefile(paramsFile);
    }catch(Exception e) {
      Date d = new Date(); 
      String operatorTimeStamp = d.toString();
      util.printMessageHException("Exception " + e.getMessage(),logger,pIntf);
      util.printMessage("SRM-TESTER: Testing " + 
		testType + " failed +++", logger,pIntf);
      e.printStackTrace();
      if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
      try {
         ReportRequestStatus rrs = new ReportRequestStatus();
         rrs.setLinkName("generalexp");
         util.webScript(publishUrlStr,displayType,"Exception","Exception",rrs,
		   "Notes from tester: "+e.getMessage(),
		    timeStamp,fullTimeStamp,startTimeStamp,
           operatorTimeStamp, servicePath,noPublish,
	       siteName,1,typeString,"","Exception", dailyTest, 
	       numSites,localPublish,bs,bsa);
      }catch(Exception we) { 
        we.printStackTrace();
	  }
    }
   }
  }

  try {
     Object obj =  values.get("help");
     if(obj != null) {
        String help = (String) obj;
        Boolean b = new Boolean (help);
	    if(b.booleanValue())  {
	      showUsage (false);
	    }
     }

     obj =  values.get("spacetoken");
     if(obj != null) {
       String tt = (String) obj; 
       spacetoken = tt;
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
          retryTimeAllowed = Integer.parseInt(tt)*60;
        }catch(NumberFormatException nfe) {}
     }

     obj =  values.get("remotefilelifetime");
     if(obj != null) {
       String tt = (String) obj; 
       try {
          remoteFileLifeTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
     }

     obj =  values.get("desiredlifetime");
     if(obj != null) {
       String tt = (String) obj; 
       try {
          desiredFileLifeTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
     }


     obj =  values.get("statuswaittime");
     if(obj != null) {
       String tt = (String) obj; 
       try {
          statusWaitTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
     }


     obj =  values.get("serviceurl");
     if(obj != null) {
       String tt = (String) obj; 
       servicePath = tt;
     }

     obj =  values.get("filestoragetype");
     if(obj != null) {
       String tt = (String) obj; 
       fileStorageType = tt;
     }

     obj =  values.get("op");
     if(obj != null) { 
       String temp = (String) obj;
       operationsGiven = temp;
       try {
         typeString = getValueForOp(temp,paramVec);
         if(typeString.equalsIgnoreCase("all")) {
            type = 0;
         }
         else {
            type = -1;
         }
       }catch(Exception e) {
          System.out.println("Exception " + e.getMessage());
          if(pIntf == null) {
            System.exit(1);
          }
          else {
            util.printMessageHException("Exception " + e.getMessage(),logger,pIntf);
            pIntf.setCompleted(true);
          }
       }
     }

     obj =  values.get("cleanupall");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       advisoryDelete = b.booleanValue();
       copyAdvisoryDelete = b.booleanValue();
       cleanUpAllGiven=b.booleanValue();
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

     /*
     obj =  values.get("onlyGsiFTPGet");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       onlyGsiFTPGet = b.booleanValue();
     }
     */

     obj = values.get("remotesfn");
     if(obj != null) {
       String tt = (String) obj;
       remoteSFN=tt;
     }

     obj =  values.get("pushcopysource");
     if(obj != null) {
       String tt = (String) obj;
       pushCopySourceFile = tt;
     }

     obj =  values.get("pullcopysource");
     if(obj != null) {
       String tt = (String) obj;
       pullCopySourceFile = tt;
     }

     obj =  values.get("pullcopysrm");
     if(obj != null) {
       String tt = (String) obj;
       pullCopySrms = tt;
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
       isPublishUrlGiven = true;
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

     obj =  values.get("spacetoken");
     if(obj != null) {
       String tt = (String) obj;
       spaceToken = tt; 
     }

     obj =  values.get("newspacesize");
     if(obj != null) {
       String tt = (String) obj;
       try {
         Long ll = new Long(tt); 
         newSpaceSize = ll.longValue();
       }catch(NumberFormatException nfe) {
          System.out.println
			("GivenNewSpace size is not a valid long value (1)"+tt);
	   }
     }

     obj =  values.get("newspacetime");
     if(obj != null) {
       String tt = (String) obj;
       try {
         newSpaceTime = Integer.parseInt(tt); 
       }catch(NumberFormatException nfe) {
          System.out.println("GivenNewTime size is not a valid int value "+tt);
	   }
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

     obj =  values.get("requesttoken");
     if(obj != null) {
       String tt = (String) obj;
       requestToken = tt;
     }


     obj =  values.get("browseafterput");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       browseAfterPut = b.booleanValue();
     }

     obj =  values.get("gucscriptpath");
     if(obj != null) {
       String tt = (String) obj;
       gucScriptPath = tt;
     }

     obj =  values.get("tempscriptpath");
     if(obj != null) {
       String tt = (String) obj;
       tempScriptPath = tt;
     }

     obj =  values.get("dailytest");
     if(obj != null) {
       String tt = (String) obj;
       Boolean b = new Boolean(tt);
       dailyTest = b.booleanValue();
     }

     obj =  values.get("numsites");
     if(obj != null) {
       String temp = (String) obj;
       try {
         numSites = Integer.parseInt(temp);
       }catch(NumberFormatException nfe) {
         util.printMessageHL
			("Given number of sites is not a valid number " + temp,logger,pIntf);
         showUsage(false);
       }
     }


     obj =  values.get("drive");
     if(obj != null) {
       driverOn=true;
     }

     obj =  values.get("output");
     if(obj != null) {
       String tt = (String) obj;
       logPath = tt;
     }

     /*
     obj =  values.get("proxyfile");
     if(obj != null) {
       String tt = (String) obj;
       proxyFile = tt;
       if(proxyFile != null && !proxyFile.equals("")) {
          defaultProxy = false;
       }
     }
     */

     obj =  values.get("localsource");
     if(obj != null) {
       String temp = (String) obj;
       putSourceFile = temp;
     }

     obj =  values.get("pushmode");
     if(obj != null) {
       String temp = (String) obj;
       Boolean b = new Boolean(temp);
       pushModeGiven = b.booleanValue();
     }

  for(int i = 0; i < _args.length; i++) {
     if(_args[i].equalsIgnoreCase("-serviceurl") && i+1 < _args.length) {
        servicePath = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-remotesfn") && i+1 < _args.length) {
        remoteSFN = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-browseafterput")) {
        browseAfterPut=true;
     }
     else if(_args[i].equalsIgnoreCase("-proxyfile") && i+1 < _args.length) {
        proxyFile = _args[i+1];
	    i++;
        defaultProxy = false;
     }
     else if(_args[i].equalsIgnoreCase("-sitename") && i+1 < _args.length) {
        siteName = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-gucscriptpath") && i+1 < _args.length) {
        gucScriptPath = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-tempscriptpath") && i+1 < _args.length) {
        tempScriptPath = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-localpublish") 
			&& i+1 < _args.length) {
        localPublishPath = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-totalsites") 
           && i+1 < _args.length) {
         String tt = _args[i+1];
         try {
           totalSites = Integer.parseInt(tt);
         }catch(NumberFormatException nfe) {}
         i++;
     }
     else if(_args[i].equalsIgnoreCase("-currsite") 
           && i+1 < _args.length) {
         String tt = _args[i+1];
         try {
           currSite = Integer.parseInt(tt);
         }catch(NumberFormatException nfe) {}
         i++;
     }
     else if(_args[i].equalsIgnoreCase("-pushmode")) {
        pushModeGiven=true;
     }
     else if(_args[i].equalsIgnoreCase("-sequencecopy")) {
        pullCopyStandAloneGiven=false;
     }
     else if(_args[i].equalsIgnoreCase("-disablebringonline")) {
        disableBringOnLine = true;
     }
     else if(_args[i].equalsIgnoreCase("-retrytimeallowed") && 
			i+1 < _args.length) {
        String tt = _args[i+1];
        try {
           retryTimeAllowed = Integer.parseInt(tt)*60;
        }catch(NumberFormatException nfe) {}
        i++; 
     }
     else if(_args[i].equalsIgnoreCase("-remotefilelifetime") && 
			i+1 < _args.length) {
        String tt = _args[i+1];
        try {
           remoteFileLifeTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
        i++; 
     }
     else if(_args[i].equalsIgnoreCase("-desiredlifetime") && 
			i+1 < _args.length) {
        String tt = _args[i+1];
        try {
           desiredFileLifeTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
        i++; 
     }
     else if(_args[i].equalsIgnoreCase("-statuswaittime") && 
			i+1 < _args.length) {
        String tt = _args[i+1];
        try {
           statusWaitTime = Integer.parseInt(tt);
        }catch(NumberFormatException nfe) {}
        i++; 
     }
     else if(_args[i].equalsIgnoreCase("-drive")) {
        driverOn=true;
     }
     else if(_args[i].equalsIgnoreCase("-usedriver")) {
        useDriverOn=true;
     }
     else if(_args[i].equalsIgnoreCase("-direct")) {
        directGsiFTP=true;
        //onlyGsiFTPGet = true;
     }
     else if(_args[i].equalsIgnoreCase("-concurrency") && i+1 < _args.length) {
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-disablecopyall")) {
        disableCopyAll = true;
     }
     else if(_args[i].equalsIgnoreCase("-disablecopypull")) {
        disableCopyPull = true;
     }
     else if(_args[i].equalsIgnoreCase("-disablecopygsiftp")) {
        disableCopyGsiFTP = true;
     }
     else if(_args[i].equalsIgnoreCase("-spacetoken") && i+1 < _args.length) {
        spaceToken = _args[i+1];
        System.out.println("SpaceToken=" + spaceToken);
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-newspacesize") && i+1 < _args.length) {
        try {
          Long ll = new Long(_args[i+1]);
          newSpaceSize = ll.longValue();
          System.out.println("NewSpaceSize=" + newSpaceSize);
        }catch(NumberFormatException nfe) {
          System.out.println("Given NewSpaceSize is not a valid value (2)" +
				_args[i+1]);
        }
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-newspacetime") && i+1 < _args.length) {
        try {
          newSpaceTime = Integer.parseInt(_args[i+1]);
          System.out.println("NewSpaceTime=" + newSpaceTime);
        }catch(NumberFormatException nfe) {
          System.out.println("Given NewSpaceTime is not a valid value " +
				_args[i+1]);
        }
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-spacetoken") && i+1 < _args.length) {
        spaceToken = _args[i+1];
        System.out.println("SpaceToken=" + spaceToken);
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-requesttoken") && i+1 < _args.length) {
        requestToken = _args[i+1];
        System.out.println("RequestToken=" + requestToken);
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-authid") && i+1 < _args.length) {
        uid = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-publish")) {
        noPublish = false;
     }
     else if(_args[i].equalsIgnoreCase("-cleanupall")) { 
		if (i+1 < _args.length) {
          Boolean b = new Boolean(_args[i+1]);
          cleanUpAllGiven=b.booleanValue();
          advisoryDelete = b.booleanValue();
          copyAdvisoryDelete = b.booleanValue();
          i++;
        }
        else {
          cleanUpAllGiven=true;
          advisoryDelete = true;
          copyAdvisoryDelete = true;
        }
     }
     else if(_args[i].equalsIgnoreCase("-abortonfail")) { 
        abortOnFail=true;
     }
     else if(_args[i].equalsIgnoreCase("-starttimestamp") 
        && i+1 < _args.length) {
        startTimeStamp = _args[i+1];
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-releasefile") 
		&& i+1 < _args.length) {
        Boolean b = new Boolean(_args[i+1]);
        releaseFile = b.booleanValue();
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-nodcau")) {
        dcau = false;
     }
     else if(_args[i].equalsIgnoreCase("-buffersize") && i+1 < _args.length) {
        int x = Integer.parseInt(_args[i+1]);
        bufferSize=x;
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-parallelism") && i+1 < _args.length) {
        int x = Integer.parseInt(_args[i+1]);
        parallelism=x;
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-detailed")) {
        detailed = true;
     }
     else if(_args[i].equalsIgnoreCase("-testcondition") && i+1 < _args.length) {
        try {
         int x = Integer.parseInt(_args[i+1]);
         givenTestCondition = x;
        }catch(NumberFormatException nfe) {
          System.out.println("Given test condition is not a valid integer " + 
				_args[i+1]);
	    }
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-filestoragetype") && i+1 < _args.length) {
        fileStorageType = _args[i+1];
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
     else if(_args[i].equalsIgnoreCase("-localsource") && i+1 < _args.length) {
        putSourceFile = _args[i+1];
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-pushcopysource") 
			&& i+1 < _args.length) {
        pushCopySourceFile = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-pullcopysource") 
			&& i+1 < _args.length) {
        pullCopySourceFile = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-gsiftpcopysource") && i+1 < _args.length) {
        gsiftpCopySourceFile = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-localtargetdir") && i+1 < _args.length) {
        localTarget = _args[i+1];
	    i++;
     }
     /*
     else if(_args[i].equalsIgnoreCase("-onlygsiftpget")) {
        onlyGsiFTPGet = true;
     }
     */
     else if(_args[i].equalsIgnoreCase("-publishurl") && i+1 < _args.length) {
        isPublishUrlGiven = true;
        publishUrlStr=_args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-dailytest")) {
        dailyTest = true;
     }
     else if(_args[i].equalsIgnoreCase("-numsites") && i+1 < _args.length) {
        String temp = _args[i+1];
        try {
           numSites = Integer.parseInt(temp);
        }catch(NumberFormatException nfe) {
           util.printMessageHL("Given number of sites is not a valid number " + temp,
				logger,pIntf);
           showUsage(false);
        }
        i++;
     }
     else if(_args[i].equalsIgnoreCase("-nooverwrite")) {
        overwrite = false;
     }
     else if(_args[i].equalsIgnoreCase("-nocopyoverwrite")) {
        copyoverwrite = false;
     }
     else if(_args[i].equalsIgnoreCase("-conf") && i+1 < _args.length) {
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-spacetoken") && i+1 < _args.length) {
        spacetoken = _args[i+1];
	    i++;
     }
     else if(_args[i].equalsIgnoreCase("-authid") && i+1 < _args.length) {
        uid = _args[i+1];
	    i++;
     }
     /*
     else if(_args[i].equalsIgnoreCase("-tokensize") && i+1 < _args.length) {
       try {
        int x = Integer.parseInt(_args[i+1]);
        tokenSize = x;
       }catch(NumberFormatException nfe) {
          System.out.println("Given tokensize is not a valid integer " + 
				_args[i+1]);
	   }
	   i++;
     }
     */
      /*
     else if(_args[i].equalsIgnoreCase("-output") && i+1 < _args.length) {
        logPath = _args[i+1];
	    i++;
     }
      */
     else if((args[i].equalsIgnoreCase("-srm")) ||
             (args[i].equalsIgnoreCase("-srmtester"))) {
         testType = args[i];
     }
     else if(args[i].equalsIgnoreCase("-op") && i+1 < _args.length) {
        //we already know the testType here.
        paramVec = new Vector();
        operationsGiven = args[i+1];
        try {
          typeString = getValueForOp(args[i+1],paramVec);
          if(typeString.equalsIgnoreCase("all")) {  
            type = 0;
          }
          else { 
            type = -1;
          }
        }catch(Exception e) {
          System.out.println("Exception " + e.getMessage());
          if(pIntf == null) {
            System.exit(1);
          }
          else {
            util.printMessageHException("Exception " + e.getMessage(),logger,pIntf);
            pIntf.setCompleted(true);
          }
        }
        i++;
     }
     else if(args[i].equalsIgnoreCase("-version")) {
       printVersion();
     }
     else if(args[i].equalsIgnoreCase("-help")) {
      showUsage(false);
     }
     else {
      util.printMessageHL("args[i] " + args[i],logger,pIntf);
      showUsage(false);
     }
  }

   Vector vec = new Vector ();
   getValueForOp(operationsGiven, vec);
   for(int i = 0; i < vec.size(); i++) {
     if(vec.elementAt(i).equals("mv") || vec.elementAt(i).equals("move")) {
        isMvGiven=true; 
     }
     else if(vec.elementAt(i).equals("srmrm") || 
			vec.elementAt(i).equals("rm")) {
        isRmGiven=true; 
     }
     else if(vec.elementAt(i).equals("ping")) {
        isPingGiven=true; 
     }
     else if(vec.elementAt(i).equals("get")) {
        isGetGiven=true; 
     }
     else if(vec.elementAt(i).equals("put")) {
        isPutGiven=true; 
     }
     else if(vec.elementAt(i).equals("browse") || 
			vec.elementAt(i).equals("ls")) {
        isBrowseGiven=true; 
     }
     else if(vec.elementAt(i).equals("bringonline")) {
        isBringOnlineGiven=true; 
     }
     /*
     else if(vec.elementAt(i).equals("release")) {
        isReleaseGiven=true; 
     }
     */
     else if(vec.elementAt(i).equals("reservespace")||
		vec.elementAt(i).equals("reserve")) {
        isReserveSpaceGiven=true; 
     }
     else if(vec.elementAt(i).equals("releasespace")||
		vec.elementAt(i).equals("release")) {
        isReleaseSpaceGiven=true; 
     }
     else if(vec.elementAt(i).equals("gettransferprotocols")) {
        isGetTransferProtocolsGiven=true; 
     }
     else if(vec.elementAt(i).equals("getspacetokens")) {
        isGetSpaceTokensGiven=true; 
     }
     else if(vec.elementAt(i).equals("getspacemeta")) {
        isGetSpaceMetaGiven=true; 
     }
     else if(vec.elementAt(i).equals("mkdir")) {
        isMkdirGiven=true; 
     }
     else if(vec.elementAt(i).equals("rmdir")) {
        isRmdirGiven=true; 
     }
     else if(vec.elementAt(i).equals("copy") || 
			 vec.elementAt(i).equals("push") || 
		     vec.elementAt(i).equals("pull") || 
			 vec.elementAt(i).equals("gsiftp")) {
      isCopyGiven=true;
     }
   }
   if(isMvGiven && isRmGiven && cleanUpAllGiven) {
     advisoryDelete = false;
   }
   if(isGetGiven && cleanUpAllGiven && !isRmGiven) {
     advisoryDelete = false;
     paramVec.addElement("srmrm");
   }

   if(isCopyGiven && cleanUpAllGiven && !isRmGiven) {
     if(isPutGiven) {
       advisoryDelete = false;
       paramVec.addElement("srmrm");
     }
   }

  if(servicePath.equals("")) {
    if(onlyGsiFTPGet) {   
       if(gsiftpCopySourceFile.equals("")) {
         util.printMessageHL(
	      "Please provide -gsiftpcopysource in the options/properties file",
			logger,pIntf);
         showUsage(false);
       }
       else {
         int idx = gsiftpCopySourceFile.lastIndexOf("/");
         if (idx != -1) {
            localTarget = localTarget+"/"+gsiftpCopySourceFile.substring(idx+1);
         }
       }
    }
    else {
     if(!driverOn) {
       util.printMessageHL("Please provide -serviceurl in the " +
		"options/properties file",logger,pIntf);
       showUsage(false);
     }
    }
  }
  else {
     Random generator = new Random();
     double r = generator.nextDouble();
     if(isGetGiven || isCopyGiven || isPutGiven || 
	    isRmGiven || isMvGiven || isBringOnlineGiven ||
	    isBrowseGiven || isMkdirGiven || isRmdirGiven) { 
        if(!remoteSFN.equals("")) {
            if(!remoteSFN.startsWith("?SFN")) {
              remoteSFN="?SFN="+remoteSFN;
            }
       if(useDriverOn) {
        getSourceFile = servicePath+remoteSFN+"."+r;
        sourceFile = servicePath+remoteSFN+"."+r;
        copySourceFile = servicePath+remoteSFN+"."+r;
        copyTarget = servicePath+remoteSFN;
        remoteTarget = servicePath+remoteSFN+"."+r;
        browseUrl = servicePath+remoteSFN+"."+r;
       }
       else {
        getSourceFile = servicePath+remoteSFN;
        sourceFile = servicePath+remoteSFN;
        copySourceFile = servicePath+remoteSFN;
        copyTarget = servicePath+remoteSFN;
        remoteTarget = servicePath+remoteSFN;
        browseUrl = servicePath+remoteSFN;
       }
       if(useDriverOn) {
         dirUrl = servicePath+remoteSFN+".testdir"+r;
       }
       else {
         dirUrl = servicePath+remoteSFN;
       }

       if(isMvGiven) {
        moveTarget = servicePath+remoteSFN+".mv."+r;
        //if(useDriverOn) {
         removeUrl = servicePath+remoteSFN+".mv."+r;
         origRemoveUrl = servicePath+remoteSFN+"."+r;
        //}
       }
       else {
        if(useDriverOn) {
          moveTarget = servicePath+remoteSFN+"."+r;
          removeUrl = servicePath+remoteSFN+"."+r;
        } 
        else {
          moveTarget = servicePath+remoteSFN;
          removeUrl = servicePath+remoteSFN;
        }
       }
     } 
     else {
            util.printMessageHL(
			 "Please provide -remotesfn in the properties file" ,logger,pIntf);
            showUsage(false);
      }
     }
     if(isGetGiven) {
       if(localTarget.equals("")) {
         util.printMessageHL(
			"Please provide -localtargetdir in the options/properties file",
					logger,pIntf);
         showUsage(false);
       }
       int idx = remoteSFN.lastIndexOf("/");
       if(idx != -1) {
         localTarget = localTarget+"/"+remoteSFN.substring(idx+1);
       }
     }
  }

  if(isPutGiven) {
     if(putSourceFile.equals("")) {
         util.printMessageHL("Please provide -localsource in the " +
			"properties file", logger,pIntf);
         showUsage(false);
     }
     if(!putSourceFile.startsWith("file")) {
       putSourceFile = "file:///"+putSourceFile;
     }
  }
  
  if(isGetGiven) {
    if(!localTarget.startsWith("file")) {
       localTarget = "file:///"+localTarget;
    }
  }

  //if(proxyFile.equals("")) {
    Properties props = System.getProperties();
    Enumeration ee = props.propertyNames();
    while (ee.hasMoreElements()) {
     String str = (String) ee.nextElement();
     if(str.trim().equals("X509_USER_PROXY")) {
       String ttemp = props.getProperty(str.trim());
       if(ttemp != null && !ttemp.equals("")) {
         proxyFile=ttemp;
         defaultProxy = false;
       }
     }
     //System.out.println(str);
    }
  //}


   if(proxyFile.trim().equals("")) {
     obj =  values.get("proxyfile");
     if(obj != null) {
       String tt = (String) obj;
       proxyFile = tt;
       if(proxyFile != null && !proxyFile.trim().equals("")) {
          defaultProxy = false;
       }
     }
  }

  Calendar c = new GregorianCalendar();
  StringBuffer buf = new StringBuffer();
  StringBuffer buf2 = new StringBuffer();
  int month = c.get(Calendar.MONTH)+1;
  buf.append(month+"-");
  buf.append(c.get(Calendar.DATE)+"-");
  buf2.append(month+"-");
  buf2.append(c.get(Calendar.DATE)+"-");
  if(!dailyTest) {
    buf.append(c.get(Calendar.YEAR)+":");
    buf2.append(c.get(Calendar.YEAR)+":");
    buf.append(c.get(Calendar.HOUR_OF_DAY)+":");
    buf2.append(c.get(Calendar.HOUR_OF_DAY)+":");
    int minute = c.get(Calendar.MINUTE);
    buf.append(minute);
    buf2.append(minute);
  }
  else {
    buf.append(c.get(Calendar.YEAR));
    buf2.append(c.get(Calendar.YEAR)+":");
    buf2.append(c.get(Calendar.HOUR_OF_DAY)+":");
    int minute = c.get(Calendar.MINUTE);
    buf2.append(minute);
  }

  timeStamp = buf.toString();
  fullTimeStamp = buf2.toString();

  if(driverOn) { 
    try {
      SRMTesterDriver test = new SRMTesterDriver(args);
    }catch(Exception e) {
      //e.printStackTrace();
      throw e;
    }
  }
  else {

  if(dailyTest && !isPublishUrlGiven) {
    //using the default url for daily test
    publishUrlStr = 
	  "http://sdm.lbl.gov/cgi-bin/srm-tester-v22-daily/tester-report-gen.cgi";
  }
   

  if(!localPublishPath.equals("")) {
     localPublish=true;

     File f = new File(localPublishPath);
     if(!f.exists()) {
       util.printMessageHL("\nGiven localpublishpath does not exists " + 
			localPublishPath,logger,pIntf); 
       showUsage(true);
     }
     

     f = new File(localPublishPath+"/"+siteName);
     if(!f.exists()) {
        f.mkdir();
     }
     f = new File(localPublishPath+"/"+siteName+"/function-tester");
     if(!f.exists()) {
        f.mkdir();
     }
     f = new File(localPublishPath+"/"+siteName+"/results");
     if(!f.exists()) {
        f.mkdir();
     }

     //String ftester = 
		//servicePath.replace('/','_')+"_ftester_"+startTimeStamp.replace(':','_')+".txt";
     if(startTimeStamp.equals("")) {
      Calendar c1 = new GregorianCalendar();
      StringBuffer buf1 = new StringBuffer();
      int month1 = c1.get(Calendar.MONTH)+1;
      if(month1 < 10) {
        buf1.append("0"+month1+"-");
      }
      else {
        buf1.append(month1+"-");
      }
      int day = c1.get(Calendar.DATE);
      if(day < 10) {
        buf1.append("0"+day+"-");
      }
      else {
        buf1.append(day+"-");
      }
      buf1.append(c1.get(Calendar.YEAR)+":");
      int hour = c1.get(Calendar.HOUR_OF_DAY);
      if(hour < 10) {
        buf1.append("0"+hour+":");
      }
      else {
        buf1.append(hour+":");
      }
      int minute = c1.get(Calendar.MINUTE);
      if(minute < 10) {
        buf1.append("0"+minute);
      }
      else {
        buf1.append(minute);
      }
       startTimeStamp = buf1.toString().trim();
     }

     
     String ftester = "ftester_"+startTimeStamp.replace(':','_')+".txt";
     
     String archiveFile = "archive_"+startTimeStamp.replace(':','_')+".html";

     ostream =
        new FileOutputStream(localPublishPath+"/"+siteName+"/function-tester/"+
            ftester,true);
     bs = new BufferedWriter(new OutputStreamWriter(ostream));

     ostreamForArchive =
        new FileOutputStream(localPublishPath+"/"+siteName+"/results/"+
            archiveFile,true);
     bsa = new BufferedWriter(new OutputStreamWriter(ostreamForArchive));
     bsa.write("<html>"); 
     bsa.newLine(); 
     bsa.flush(); 
  }
  else {
    //System.out.println("\n\tPublishUrlStr="+publishUrlStr);
  }

  if(useThisLogger != null) {
     this.logger = useThisLogger;
  }
  else {
    logger = LogFactory.getLog(SRMTester.class.getName());
  }

  try {
  PropertyConfigurator.configure(log4jlocation);
  }catch(Exception eex){;}

  /*
  if(setLog) {
    //setting log file
    System.out.println("Log4jlocation " + log4jlocation);
    setLogFile(log4jlocation,logPath);
  }
  */

  }//end else if(driverOn)
  }catch(Exception e) {
    Date d = new Date(); 
    String operatorTimeStamp = d.toString();
    util.printMessageHException("Exception " + e.getMessage(),logger,pIntf);
    util.printMessage("SRM-TESTER: Testing (3)" + 
		testType + " failed ", logger,pIntf);
    e.printStackTrace();
    if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
    try {
       ReportRequestStatus rrs = new ReportRequestStatus();
       rrs.setLinkName("generalexp");
       util.webScript(publishUrlStr,displayType,"Exception","Exception",rrs,
		"Notes from tester" + e.getMessage(),
		timeStamp,fullTimeStamp,startTimeStamp,
        operatorTimeStamp, servicePath,noPublish,
	    siteName,1,typeString,"","Exception", dailyTest, 
	    numSites,localPublish,bs,bsa);
    }catch(Exception we) { 
        we.printStackTrace();
	}
   }
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// startTester
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void startTester () {

 try {
  //finding displayType
  displayType = findDisplayType(testType);

  //do some error checking here.

  numOperations = paramVec.size();


  if((testType.equals("-srm")) || (testType.equals("-srmtester"))) {
    if(type == 0) { 
      isGetGiven=true;
      isBringOnlineGiven=true;
      isPutGiven=true;
      isCopyGiven=true;  
      isGsiFTPCopyGiven=true;  
      isPullCopyGiven=true;  
      isPushCopyGiven=true;  
    }
    else {
      for(int i = 0; i < paramVec.size(); i++) {
        String tt = (String)paramVec.elementAt(i);
        if(tt.equalsIgnoreCase("get")) { 
           isGetGiven=true;
        }
        if(tt.equalsIgnoreCase("bringonline")) { 
           isBringOnlineGiven=true;
        }
        if(tt.equalsIgnoreCase("put")) {
           isPutGiven=true;
        }
        if(tt.equalsIgnoreCase("reserveput")) {
           reserveSpace=true;
           isPutGiven=true;
        }
        if(tt.equalsIgnoreCase("gsiftp")) {
           isGsiFTPCopyGiven=true;
           isCopyGiven=true;
        }
        if(tt.equalsIgnoreCase("pull")) {
           isPullCopyGiven=true;
           isCopyGiven=true;
        }
        if(tt.equalsIgnoreCase("push")) {
           isPushCopyGiven=true;
           isCopyGiven=true;
        }
        if(tt.equalsIgnoreCase("mv")||tt.equalsIgnoreCase("srmmv")) {
           isMvGiven=true;
        }
      }
    }
    if(isBringOnlineGiven) {
     if(localTarget.equals("")) {
       util.printMessageHL
        ("\n\nPlease provide the local target -t <target>\n\n option", logger,pIntf);
       showUsage(false);
     }
    }
    if(isGetGiven) {
     if(localTarget.equals("")) {
       util.printMessageHL
        ("\n\nPlease provide the local target -t <target>\n\n option", logger,pIntf);
       showUsage(false);
     }
    }
    if(!onlyGsiFTPGet) {
      if(isPutGiven) {
        if(putSourceFile.equals("")) {
          util.printMessageHL ("\n\nPlease provide the -localsource " +
           "which is required for put requests.\n\n",logger,pIntf);
          showUsage(false);
        }
        if(remoteTarget.equals("")) {
          util.printMessageHL ("\n\nPlease provide the -remotetarget " +
		   "which is required for put requests.\n\n",logger,pIntf);
          showUsage(false);
        }
      }
      if(isCopyGiven) {
        if(copyTarget.equals("")) {
          util.printMessageHL ("\n\nPlease provide the -copytarget " +
		   "which is required for copy requests.\n\n",logger,pIntf);
          showUsage(false);
        }
        if(isGsiFTPCopyGiven) {
         if(gsiftpCopySourceFile.equals("")) {
            util.printMessageHL ("\n\nPlease provide the -gsiftpcopysource " +
		     "which is required for gsiftp copy requests.\n\n",logger,pIntf);
            showUsage(false);
         }
        }
      }
      if(isMvGiven) {
        if(moveTarget.equals("")) {
          util.printMessageHL ("\n\nPlease provide the -mvtarget " +
		   "which is required for move requests.\n\n",logger,pIntf);
          showUsage(false);
        }
      }
    }
  }

  int cc = 0;
  if(isCopyGiven && !pushCopySourceFile.equals("")) {
    StringTokenizer stk = new StringTokenizer(pushCopySourceFile,",");
    while(stk.hasMoreTokens()) {
       stk.nextToken();
       cc++;
    }
    if(cc > 0) { 
      numOperations = numOperations+(cc-1);
    }
    else {
      numOperations = numOperations+cc;
    }
  }

  if(isPushCopyGiven && isPullCopyGiven) {
    if(cc > 0) { 
      numOperations = numOperations+(cc-1);
    }
    else {
      numOperations = numOperations+cc;
    }
    if(isPullCopyGiven) {
      if(pullCopySrms.equals("")) {
         util.printMessageHL("Please provide the valid pullcopysrm",logger,pIntf);
         showUsage(false);
      }
      if(pullCopySourceFile.equals("")) {
         util.printMessageHL("Please provide the valid pullcopysourcefile",
			logger,pIntf);
         showUsage(false);
      }
      if(copySourceFile.equals("")) {
         util.printMessageHL("Please provide the valid copysourcefile",logger,pIntf);
         showUsage(false);
      }
    }
    if(isPushCopyGiven) {
      if(pushCopySourceFile.equals("")) {
         util.printMessageHL("Please provide the valid pushcopysourcefile",	
			logger,pIntf);
         showUsage(false);
      }
    }
  }

  if(servicePath == null || servicePath.equals("")) {
    if(!directGsiFTP) {
      util.printMessageHL("\n\nPlease provide the -serviceurl\n\n",logger,pIntf);
      showUsage (false);
    }
  }


  if(!directGsiFTP) {
  MyGlobusURL serviceUrl = new MyGlobusURL(servicePath.trim());
  String sHost = serviceUrl.getHost();
  int sPort = serviceUrl.getPort();
  String sPath = serviceUrl.getPath();

  if(sHost == null || sHost.equals("")) {
    util.printMessageHL("\n\nHost name is required\n\n",logger,pIntf);
    showUsage(false);
  }
  if(sPort == -1) {
    util.printMessageHL("\n\nPort name is required\n\n",logger,pIntf);
    showUsage(false);
  }
  if(sPath == null || sPath.equals("")) {  
    if(!onlyGsiFTPGet) {
     util.printMessageHL("\n\nPath name is required\n\n",logger,pIntf);
     showUsage(false);
    }
  }

  if(sourceFile.startsWith("srm")) {

    MyGlobusURL surl = new MyGlobusURL (sourceFile);
    String sourceHost = surl.getHost();
    int sourcePort    = surl.getPort();
    String sourcePath = surl.getPath();

    int idx = sourcePath.indexOf("?SFN");
    String ttemp = sourcePath;
    if(idx != -1) {
      //srm://dmx.lbl.gov:6253///?SFN=/data/srm/...
      ttemp = sourcePath.substring(idx);
    }
    else {
        //srm://dmx.lbl.gov:6253//data/srm/...
        ttemp = "?SFN="+sourcePath;
    }

    if(sHost.equals(sourceHost)) {
     if(sPath != null && !sPath.startsWith("/")) {
       sPath = "/"+sPath;
     }
     sourceFile = "srm://"+sHost+":"+sPort+sPath+ttemp;
    }
  }
  if(getSourceFile.startsWith("srm")) {

    MyGlobusURL surl = new MyGlobusURL (getSourceFile);
    String sourceHost = surl.getHost();
    int sourcePort    = surl.getPort();
    String sourcePath = surl.getPath();

    int idx = sourcePath.indexOf("?SFN");
    String ttemp = sourcePath;
    if(idx != -1) {
      //srm://dmx.lbl.gov:6253///?SFN=/data/srm/...
      ttemp = sourcePath.substring(idx);
    }
    else {
        //srm://dmx.lbl.gov:6253//data/srm/...
        ttemp = "?SFN="+sourcePath;
    }

    if(sHost.equals(sourceHost)) {
     if(sPath != null && !sPath.startsWith("/")) {
       sPath = "/"+sPath;
     }
     getSourceFile = "srm://"+sHost+":"+sPort+sPath+ttemp;
    }
  }
  }

  //printing user parameter values.

  if(!onlyGsiFTPGet) {
    //util.printMessage("\n\n======================================",logger,null);
    util.printMessage("SRM-TESTER: SRM Tester Configuration ", logger,pIntf);
    util.printMessage("\n\tServicePath=" + servicePath,logger,pIntf);
    util.printMessage("\tDoing operation=" + typeString, logger,pIntf);
    if(isPutGiven) {
      util.printMessage("\tRemote Target Location=" + remoteTarget,logger,pIntf);
    }
    if(isMvGiven) {
      util.printMessage("\tMove Target Location=" + moveTarget,logger,pIntf);
    }
    if(isRmGiven) {
      util.printMessage("\tRemove Target Location=" + removeUrl,logger,pIntf);
    }
    if(isBrowseGiven) {
      util.printMessage("\tBrowse Target Location=" + browseUrl,logger,pIntf);
    }
    if(isMkdirGiven || isRmdirGiven) {
      util.printMessage("\tDir Location=" + dirUrl,logger,pIntf);
    }
    if(isCopyGiven) { 
      util.printMessage("\tPushModeUsed=" + pushMode, logger,pIntf);
      util.printMessage("\tPushModeGiven=" + pushModeGiven, logger,pIntf);
    }
    if(!uid.equals("")) {
      util.printMessage("\tAuthorizationID="+ uid,logger,pIntf);
    }
    if(!spaceToken.equals("")) {
      util.printMessage("\tSpaceToken="+ spaceToken,logger,pIntf);
    }
    if(!requestToken.equals("")) {
      util.printMessage("\tRequestToken="+ requestToken,logger,pIntf);
    }
    if(isPutGiven || isCopyGiven) {
      util.printMessage("\tCleanUpAll=" + cleanUpAllGiven,logger,pIntf);
    }
    if(isGetGiven) {
      util.printMessage("\tReleaseFile=" + releaseFile,logger,pIntf);
    }
    if(isPutGiven || isCopyGiven) {
      util.printMessage("\tOverwrite=" + overwrite,logger,pIntf);
    }  
    if(isPutGiven) {
      util.printMessage("\tBrowseAfterPut=" + browseAfterPut,logger,pIntf);
    }
    util.printMessage("\tRetryTimeAllowed="+retryTimeAllowed,logger,pIntf);
    util.printMessage("\tStatusWaitTime="+statusWaitTime,logger,pIntf);
    //util.printMessage("\tDetailed=" + detailed,pIntf);
  }
  else {
    util.printMessage
		("\n\n======================================",logger,pIntf);
    util.printMessage("\n\tTesting Only GsiFTP",logger,pIntf);
  }

  //util.printMessage("\tSourceFile=" + sourceFile,logger,pIntf);
  if(isGetGiven) {
    util.printMessage("\tsource for Get=" + getSourceFile,logger,pIntf);
    util.printMessage("\ttarget for Get=" + localTarget,logger,pIntf);
  }
  if(isPutGiven) {
    util.printMessage("\tsource for Put=" + putSourceFile,logger,pIntf); 
    if(remoteFileLifeTime != 0) {
      util.printMessage("\tremoteFileLifeTime=" + remoteFileLifeTime,logger,pIntf); 
    }
  } 
  if(isBringOnlineGiven) {
    if(desiredFileLifeTime != 0) {
      util.printMessage("\tdesiredFileLifeTime=" + desiredFileLifeTime,logger,pIntf); 
    }
  }
  if(!gsiftpCopySourceFile.equals("")) {
    util.printMessage("\tsource for GsiFTP Copy=" + gsiftpCopySourceFile,logger,pIntf);
  }
  if(isCopyGiven) {
    util.printMessage("\tsource for Copy=" + copySourceFile,logger,pIntf);
  }
  if(!pullCopySrms.equals("")) {
    util.printMessage("\tPullCopySrms=" + pullCopySrms,logger,pIntf);
  }
  if(!pushCopySourceFile.equals("")) {
    util.printMessage("\tPushCopySource=" + pushCopySourceFile,logger,pIntf);
  }
  if(!pullCopySourceFile.equals("")) {
    util.printMessage("\tPullCopySource=" + pullCopySourceFile,logger,pIntf);
  }

  if(!gucScriptPath.equals("")) {
    useGUC = true;
  }

  util.printMessage("\tgucScriptPath="+gucScriptPath,logger,pIntf);
  
  if(!fileStorageType.equals("")) {
    util.printMessage("\tFileStorageType=" + fileStorageType,logger,pIntf);
  }
  //util.printMessage("\tOutput log file=" + logPath,logger,pIntf);
  util.printMessage("\tDailyTest="+dailyTest,logger,pIntf);
  util.printMessage("\tNumSites="+numSites,logger,pIntf);
  util.printMessage("\tRetryTimeAllowed="+retryTimeAllowed,logger,pIntf);
  util.printMessage("\tStatusWaitTime="+statusWaitTime,logger,pIntf);
  if(!localPublish) {
    if(isPublishUrlGiven) {
      util.printMessage("\tPublishUrlStr="+publishUrlStr,logger,pIntf);
    }
  }
  if(isPutGiven || isGetGiven) {
    util.printMessage("\tDCAU=" + dcau,logger,pIntf);
  }
  util.printMessage("\tDriverOn="+ driverOn, logger,pIntf);
  util.printMessage("\tremotesfn=" + remoteSFN,logger,pIntf);
  util.printMessage("\tUseDriverOn="+ useDriverOn, logger,pIntf);
  if(defaultProxy)
    util.printMessage("\tUsing default user proxy", logger,pIntf);
  else 
    util.printMessage("\tUsing proxy file " + proxyFile, logger,pIntf);
  //util.printMessage("\n======================================",logger,pIntf);

   
  if(!onlyGsiFTPGet) {
   if(type == 0) {
     callTestPing("SrmPing");
     if(detailed) {
       doDetailedTestCallForGet(proxyString);
       doDetailedTestCallForPut(proxyString);
       srm = connectToSRM(proxyString,true,"");
       doDetailedTestCallForCopyGsiFTP(proxyString);
       StringTokenizer stk = new StringTokenizer(copySourceFile,",");
       Vector vec = new Vector ();
       while(stk.hasMoreTokens()) {
         vec.addElement(stk.nextToken()); 
       }
       for(int ii = 0; ii < vec.size(); ii++) {
         String temp = (String) vec.elementAt(ii);
         int idx = temp.indexOf("@");
         if(idx != -1) {
           doDetailedTestCallForCopyPull(proxyString,temp.substring(idx+1),
				temp.substring(idx+1)+"."+temp.substring(0,idx),
			    temp.substring(0,idx));
         }
         else {
           doDetailedTestCallForCopyPull(proxyString,temp, "","");
         }
       }
       for(int ii = 0; ii < vec.size(); ii++) {
         String temp = (String) vec.elementAt(ii);
         int idx = temp.indexOf("@");
         if(idx != -1) {
           doDetailedTestCallForCopyPush(proxyString,temp.substring(idx+1),
				temp.substring(0,idx),temp.substring(0,idx));
         }
         else {
           doDetailedTestCallForCopyPush(proxyString,temp,"","");
         }
       }
     }
     else {
       doBasicTestCallForGet(proxyString,"");
       doBasicTestCallForPut(proxyString,"");
       srm = connectToSRM(proxyString,true,"");
       doBasicTestCallForCopyGsiFTP(proxyString,"");
       StringTokenizer stk = new StringTokenizer(copySourceFile,",");
       Vector vec = new Vector ();
       while(stk.hasMoreTokens()) {
         vec.addElement(stk.nextToken()); 
       }
       for(int ii = 0; ii < vec.size(); ii++) {
         String temp = (String) vec.elementAt(ii);
         int idx = temp.indexOf("@");
         if(idx != -1) {
           doBasicTestCallForCopyPull(proxyString,temp.substring(idx+1),
				temp.substring(idx+1)+"."+temp.substring(0,idx),
			    temp.substring(0,idx),"");
         }
         else { 
           doBasicTestCallForCopyPull(proxyString,temp,"","","");
         }
       }
       for(int ii = 0; ii < vec.size(); ii++) {
         String temp = (String) vec.elementAt(ii);
         int idx = temp.indexOf("@");
         if(idx != -1) {
           doBasicTestCallForCopyPush(proxyString,temp.substring(idx+1),
				temp.substring(0,idx),temp.substring(0,idx),"");
         }
         else {
           doBasicTestCallForCopyPush(proxyString,temp,"","","");
         }
       }  
     }
   }
   //do specific tests
   else {
      for(int i = 0; i < paramVec.size(); i++) {
        String ss = (String) paramVec.elementAt(i);
          
        if(useDriverOn && !isPingOverAllSuccess.isOperOkDummy()) {
           String tempOpType=ss;
           if(ss.equalsIgnoreCase("gettransferprotocols")) {
             tempOpType="srmGetTransferProtocols";
           }
           else if(ss.equalsIgnoreCase("getrequesttokens")) {
             tempOpType="srmGetRequestTokens";
           }
           else if(ss.equalsIgnoreCase("getspacetokens")) {
             tempOpType="srmGetSpaceTokens";
           }
           else if(ss.equalsIgnoreCase("get")) {
             tempOpType="srmPrepareToGet";
           }
           else if(ss.equalsIgnoreCase("put") || 
				ss.equalsIgnoreCase("reserveput")) {
             tempOpType="srmPrepareToPut";
           }
           else if(ss.equalsIgnoreCase("bringonline")) {
             tempOpType="srmBringOnline";
           }
           else if(ss.equalsIgnoreCase("putdone")) {
             tempOpType="srmPutDone";
           }
           else if(ss.equalsIgnoreCase("abortfiles")) {
             tempOpType="srmAbortFiles";
           }
           else if(ss.equalsIgnoreCase("gsiftp") ||
                   ss.equalsIgnoreCase("pull")) {
             tempOpType="srmCopy-Pull";
           }
           else if(ss.equalsIgnoreCase("push")) {
             tempOpType="srmCopy-Push";
           }
           else if(ss.equalsIgnoreCase("mv")||
			  ss.equalsIgnoreCase("srmmv")) {
             tempOpType="srmMv";
           }
           else if(ss.equalsIgnoreCase("ls") ||
                   ss.equalsIgnoreCase("browse")||
		           ss.equalsIgnoreCase("srmls")) {
             tempOpType="srmLs";
           }
           else if(ss.equalsIgnoreCase("mkdir")) {
             tempOpType="srmMkdir";
           }
           else if(ss.equalsIgnoreCase("rmdir")) {
             tempOpType="srmRmdir";
           }
           else if(ss.equalsIgnoreCase("srmrm") || ss.equalsIgnoreCase("rm")) {
             tempOpType="srmRm";
           }
           else if(ss.equalsIgnoreCase("updatespace")) {
             tempOpType="srmUpdateSpace";
           }
           else if(ss.equalsIgnoreCase("releasespace") ||
				   ss.equalsIgnoreCase("release")) {
             tempOpType="srmReleaseSpace";
           }
           else if(ss.equalsIgnoreCase("reservespace") ||
				   ss.equalsIgnoreCase("reserve")) {
             tempOpType="srmReserveSpace";
           }
           else if(ss.equalsIgnoreCase("getspacemeta")) {
             tempOpType="srmGetSpaceMeta";
           }
           Date d = new Date();
           String operatorTimeStamp = d.toString();
           util.webScript
                (publishUrlStr,displayType,
                 tempOpType+"\n", "N.T.",
			     null,
				 "Notes from tester: srmPing failed and hence, " +
			     "not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,tempOpType,
			     tempOpType,"N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
        }
        else {
        if(ss.equalsIgnoreCase("ping")) {
          callTestPing("srmPing");
        }
        if(ss.equalsIgnoreCase("gettransferprotocols")) {
          String exceptionMessage = "";
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //exceptionMessage = callTestPing("srmGetTransferProtocols");
              exceptionMessage="";
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else {
            //exceptionMessage = callTestPing("srmGetTransferProtocols");
            exceptionMessage="";
            srm = connectToSRM(proxyString,false,"");
            isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            callTestGetTransferProtocols("srmGetTransferProtocols", 
				exceptionMessage);
          }
          else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
                (publishUrlStr,displayType,
                 "srmGetTransferProtocols\n", "N.T.",
			     null,
				 "Notes from tester: srmPing failed and hence, " +
			     "not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,"srmGetTransferProtocols",
			     "srmGetTransferProtocols","N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
          }
        }
        if(ss.equalsIgnoreCase("getrequesttokens")) {
          String exceptionMessage="";
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //exceptionMessage = callTestPing("srmGetRequestTokens");
              exceptionMessage=""; 
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else { 
              //exceptionMessage = callTestPing("srmGetRequestTokens");
              exceptionMessage=""; 
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            callTestGetRequestTokens("srmGetRequestTokens",exceptionMessage);
          }
          else {  
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
                (publishUrlStr,displayType,
                 "srmGetRequestTokens\n", "N.T",
			     null,
				 "Notes from tester: srmPing failed and hence, " +
			     "not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,"srmGetRequestTokens",
			     "srmGetRequestTokens","N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
          }
        }
        if(ss.equalsIgnoreCase("getspacetokens")) {
          String exceptionMessage="";
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //exceptionMessage = callTestPing("srmGetSpaceTokens");
              exceptionMessage ="";
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else { 
            //exceptionMessage = callTestPing("srmGetSpaceTokens");
            exceptionMessage ="";
            srm = connectToSRM(proxyString,false,"");
            isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            callTestGetSpaceTokens("srmGetSpaceTokens",exceptionMessage);
          } 
          else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
                (publishUrlStr,displayType,
                 "srmGetSpaceTokens\n", "N.T",
			     null,
				 "Notes from tester: srmPing failed and hence, " +
			     "not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,"srmGetSpaceTokens",
			     "srmGetSpaceTokens","N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
          }
        }
        if(ss.equalsIgnoreCase("get")) {
         boolean goahead = false;
         if(isPutGiven && isPutSuccess.isOperOk()) {
           goahead=true;
         }
         else if(!isPutGiven) {
           goahead=true;
         }
         if(goahead) {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //callTestPing("srmPrepareToGet");
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else {
              //callTestPing("srmPrepareToGet");
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
           StringTokenizer stk = new StringTokenizer(fileStorageType,",");
           Vector vec = new Vector ();
           while (stk.hasMoreTokens()) {
             vec.addElement(stk.nextToken()); 
           }
           String tempFileStorage="";
           for(int iii = 0; iii < vec.size(); iii++) {
              String tt = (String) vec.elementAt(iii);
              int idx = tt.indexOf("@");
              if(idx != -1) {
                if(siteName.equals(tt.substring(0,idx))) {
                  tempFileStorage=tt.substring(idx+1);
                }
              }
           }
           if(detailed) {
             if(givenTestCondition == -1) {
               doDetailedTestCallForGet(proxyString);
             }
             else {
               doDetailedTestCallForGet(proxyString,givenTestCondition);
             }
           }
           else {
             doBasicTestCallForGet(proxyString,tempFileStorage);
           }
           }
           else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
                (publishUrlStr,displayType,
                 "srmPrepareToGet\n", "N.T",
			     null,
				 "Notes from tester: srmPing failed and hence, " +
			     "not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,"srmPrepareToGet",
			     "srmPrepareToGet","N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
            }
          }else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             String operationType="srmPrepareToGet";
             String opReqType = "srmPrepareToGet Request";
             if(directGsiFTP) {
               operationType = "GridFTP-Get"; 
               opReqType = "GridFTP-Get Request";
             }
             util.webScript
                 (publishUrlStr,displayType,
                  opReqType+"\n","Sending",
                  null,
				  "Notes from tester: srmPrepareToPut is not ok. " +
			      "so, srmPrepareToGet is skipped and not tried.", 
				  timeStamp,fullTimeStamp,startTimeStamp,
                  operatorTimeStamp, servicePath,noPublish,
                  siteName,numOperations,operationType,
				  operationType,"Sending",dailyTest,
			      numSites,localPublish,bs,bsa);
             util.webScript
                (publishUrlStr,displayType,
                 opReqType+" Not tried\n", "N.T",
			     null,
				 "Notes from tester: srmPrepareToPut is not ok. " +
			     "so, srmPrepareToGet is skipped and not tried.", 
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp, servicePath,noPublish,
                 siteName,numOperations,operationType,
			     operationType,"N.T.",dailyTest,numSites,
				 localPublish,bs,bsa);
             util.webScript
                (publishUrlStr,displayType,
                  opReqType+" Request Skipped\n","SKIPTHIS",
                  null, 
				  "Notes from tester: srmPrepareToPut is not ok. " +
			      "so, srmPrepareToGet is skipped and not tried.", 
				  timeStamp,fullTimeStamp,startTimeStamp,
                  operatorTimeStamp, servicePath,noPublish,
                  siteName,numOperations,operationType,
				  operationType, "N.T.",dailyTest,numSites,localPublish,bs,bsa);
             util.printMessage("\nSkipping the get operations for this site " + 
					siteName,logger,pIntf);
          }
        }
        if(ss.equalsIgnoreCase("bringonline")) {
          if(disableBringOnLine) {
           util.printMessage("\nSkipping bringline test for this site " + 
				siteName,logger,pIntf);
          }
          else {
            boolean goahead = false;
            if(isPutGiven && isPutSuccess.isOperOk()) {
              goahead=true;
            }
            else if(!isPutGiven) {
              goahead=true;
            }
            if(goahead) {
              if(useDriverOn) {
                if(isPingOverAllSuccess.isOperOk()) {
                 //callTestPing("srmBringOnline");
                 srm = connectToSRM(proxyString,false,"");
                 isPingSuccess.isOperOk(true);
                }
              }
              else {
                 //callTestPing("srmBringOnline");
                 srm = connectToSRM(proxyString,false,"");
                 isPingSuccess.isOperOk(true);
              }
              if(isPingSuccess.isOperOk()) {
                if(detailed) {
                  doDetailedTestCallForBringOnLine(proxyString);
                }
                else {
                  doBasicTestCallForBringOnLine(proxyString);
                }
              }
              else {
                Date d = new Date();
                String operatorTimeStamp = d.toString();
                util.webScript
                  (publishUrlStr,displayType,
                   "srmBringOnline\n", "N.T",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmBringOnline",
			        "srmBringOnline","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
               }
            } 
            else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
              (publishUrlStr,displayType,
               "srmBringOnline Request \n","Sending",
                null, 
				"Notes from tester: srmPrepareToPut is not ok. " +
			    "so, srmPrepareToGet is skipped and not tried.", 
			    timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
		        servicePath,noPublish,
                siteName,numOperations,"srmBringOnline",
				"srmBringOnline","Sending",dailyTest,
			    numSites,localPublish,bs,bsa);
              util.webScript
               (publishUrlStr,displayType,
                "srmBringOnline Request\n", "N.T.",
			    null,
				"Notes from tester: srmPrepareToPut is not ok. " +
			    "so, srmPrepareToGet is skipped and not tried.", 
			    timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
			    servicePath,noPublish,
                siteName,numOperations,"srmBringOnline",
			    "srmBringOnline","N.T.",dailyTest,numSites,
				localPublish,bs,bsa);
               util.webScript
                (publishUrlStr,displayType,
                  "srmBringOnline \n","SKIPTHIS",
                   null, 
				   "Notes from tester: srmPrepareToPut is not ok. " +
			       "so, srmPrepareToGet is skipped and not tried.", 
				   timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,"srmBringOnline",
				   "srmBringOnline", "N.T.",
				   dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage(
				   "\nSkipping the bringonline operations for this site " + 
				   siteName,logger,pIntf);
            }
          }
        }
        if(ss.equalsIgnoreCase("putdone")) {
           if(useDriverOn) {
             if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmPutDone");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
             }
           }
           else { 
               //callTestPing("srmPutDone");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
           }
           if(isPingSuccess.isOperOk()) {
             doBasicTestCallForPutDone(proxyString);
           }
           else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                  (publishUrlStr,displayType,
                   "srmPutDone\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmPutDone",
			        "srmPutDone","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
            }
        }
        if(ss.equalsIgnoreCase("abortfiles")) {
           if(useDriverOn) {
             if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmAbortFiles");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
             }
           }
           else {
               //callTestPing("srmAbortFiles");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
           }
           if(isPingSuccess.isOperOk()) {
             doBasicTestCallForAbortFiles(proxyString);
           }
           else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                  (publishUrlStr,displayType,
                   "srmAbortFiles\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmAbortFiles",
			        "srmAbortFiles","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
            }
        }
        if(ss.equalsIgnoreCase("put") || ss.equalsIgnoreCase("reserveput")) {
          if(useDriverOn) {
             if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmPrepareToPut");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
             }
          }
          else {
               //callTestPing("srmPrepareToPut");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            StringTokenizer stk = new StringTokenizer(fileStorageType,",");
            Vector vec = new Vector ();
            while (stk.hasMoreTokens()) {
             vec.addElement(stk.nextToken()); 
            }
            String tempFileStorage="";
            for(int iii = 0; iii < vec.size(); iii++) {
              String tt = (String) vec.elementAt(iii);
              int idx = tt.indexOf("@");
              if(idx != -1) {
                if(siteName.equals(tt.substring(0,idx))) {
                  tempFileStorage=tt.substring(idx+1);
                }
              }
            }
            if(detailed) {
             if(givenTestCondition == -1) {
               doDetailedTestCallForPut(proxyString);
             }
             else {
               doDetailedTestCallForPut(proxyString,givenTestCondition);
             } 
            }
            else {   
             doBasicTestCallForPut(proxyString,tempFileStorage);
            }
          }
          else {
            Date d = new Date();
            String operatorTimeStamp = d.toString();
            util.webScript
              (publishUrlStr,displayType,
                "srmPrepareToPut\n","N.T.",
                 null, 
                 "Notes from tester : srmPing failed, hence not tried",
				 timeStamp,fullTimeStamp,startTimeStamp,
                 operatorTimeStamp,
		         servicePath,noPublish,
                 siteName,numOperations,ss,"srmPrepareToPut",
				 "N.T.",dailyTest,numSites,localPublish,bs,bsa);
          } 
        }
        if(ss.startsWith("gsiftp") || ss.startsWith("pull")) {
          pushMode=false;
          changeToCopyServicePath=true;
          StringTokenizer sstk = new StringTokenizer(fileStorageType,",");
          Vector vvec = new Vector ();
          while (sstk.hasMoreTokens()) {
            vvec.addElement(sstk.nextToken()); 
          }
          String tempFileStorage="";
          for(int iii = 0; iii < vvec.size(); iii++) {
             String tt = (String) vvec.elementAt(iii);
             int idx = tt.indexOf("@");
             if(idx != -1) {
               if(siteName.equals(tt.substring(0,idx))) {
                 tempFileStorage=tt.substring(idx+1);
               }
             }
          }
          Date d = new Date();
          String operatorTimeStamp = d.toString();
          if(ss.equalsIgnoreCase("gsiftp")) { 
            if(disableCopyAll) {
              util.webScript
                (publishUrlStr,displayType,
                 "Copy Request \n","Sending",
                  null,"", timeStamp,fullTimeStamp,startTimeStamp,
                  operatorTimeStamp, servicePath,noPublish,
                  siteName,numOperations,ss,"","Sending",dailyTest,
				  numSites,localPublish,bs,bsa);
              util.webScript
                (publishUrlStr,displayType,
                  "Copy Request \n","N.A.",
                   null,"", timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,ss,"","N.A.",
				   dailyTest,numSites,localPublish,bs,bsa);
              util.webScript
                (publishUrlStr,displayType,
                  "Copy Request \n","SKIPTHIS",
                   null,"", timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,ss,"srmCopy-pull-gsiftp",
				   "N.A.",dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage("Skipping all copy operations for this site " + 
					siteName,logger,pIntf);
             } 
             else {
              if(useDriverOn) {
                if(isPingOverAllSuccess.isOperOk()) {
                  //callTestPing("gsiftp");
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
                }
              }
              else {
                  //callTestPing("gsiftp");
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
              }
              if(isPingSuccess.isOperOk()) {
                if(detailed) {
                  doDetailedTestCallForCopyGsiFTP(proxyString);
                }
                else {   
                  doBasicTestCallForCopyGsiFTP(proxyString, tempFileStorage);
                }
              }
              else {
                util.webScript
                  (publishUrlStr,displayType,
                   "gsiftp\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"gsiftp",
			        "srmCopy-pull-gsiftp","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
               }
             }
          }
          else {
           if(pullCopyStandAloneGiven) {
              if(useDriverOn) {
                if(isPingOverAllSuccess.isOperOk()) {
                  //callTestPing(ss);
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
                }
              }
              else {
                  //callTestPing(ss);
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
              }
              if(isPingSuccess.isOperOk()) { 
                doBasicTestCallForCopyPull(proxyString,copySourceFile,
					copyTarget,"",tempFileStorage);
              }
              else {
                util.webScript
                  (publishUrlStr,displayType,
                   "srmCopy-Pull\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmCopy-Pull",
			        "srmCopy-Pull","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
               } 
           }
           else {
               StringTokenizer stk = new StringTokenizer(pullCopySrms,",");
               Vector vec = new Vector ();
               while(stk.hasMoreTokens()) {
                 String str = stk.nextToken();
                 vec.addElement(str); 
               }
               stk = new StringTokenizer(pullCopySourceFile,",");
               Vector vec1 = new Vector ();
               while(stk.hasMoreTokens()) {
                 vec1.addElement(stk.nextToken()); 
               }
               for(int ii = 0; ii < vec.size(); ii++) {
                  boolean disable = false;
                  if(!isPutSuccess.isOperOk()) {
                    disable = true;
                  }  
                  else {
                    String ttemp = (String)vec1.elementAt(ii);
                    int idx2 = ttemp.indexOf("@");
                    if(idx2 != -1) {
                      if(ttemp.substring(idx2+1).equals("disable=true")) {
                        disable=true;
                     }
                    }
                 }
                 String temp = (String) vec.elementAt(ii);
                 int idx = temp.indexOf("@");
                 if(disable) {
                     String sName = temp;
                     d = new Date ();
                     operatorTimeStamp = d.toString();
                     if(idx != -1) {
                        sName = temp.substring(0,idx);
                     }
                     util.webScript
                        (publishUrlStr,displayType,
                         "Copy Request \n", "Sending", null, 
                         "Notes from tester: copy request is skipped",
						 timeStamp,
						 fullTimeStamp, startTimeStamp, operatorTimeStamp,
						 servicePath,noPublish,
                         siteName,numOperations,ss+"-"+sName,
						 "","Sending",dailyTest,numSites,localPublish,bs,bsa);
                     util.webScript
                        (publishUrlStr,displayType,
                         "Copy Request \n", "N.T.", null, 
                         "Notes from tester: copy request is skipped",
						 timeStamp,
					     fullTimeStamp, startTimeStamp, operatorTimeStamp,
						 servicePath,noPublish,
                         siteName,numOperations,ss+"-"+sName,
						 "","N.T.",dailyTest,numSites,localPublish,bs,bsa);
                     util.webScript
                        (publishUrlStr,displayType,
                         "Copy Request \n","SKIPTHIS",
                          null,
                          "Notes from tester: copy request is skipped",
						  timeStamp,fullTimeStamp,startTimeStamp,
				          operatorTimeStamp, servicePath,noPublish,
                          siteName,numOperations,ss+"-"+sName,
						  "srmCopy-pull-"+sName,
				          "N.T.",dailyTest,numSites,localPublish,bs,bsa);
                     util.printMessage("Skipping all copy operations for this site " 
		                  + sName,logger,pIntf);
                 }
                 else {
                    String sName = temp;
                    if(idx != -1) {
                      sName = temp.substring(0,idx); 
                    }
                    sstk = new StringTokenizer(fileStorageType,",");
                    vvec = new Vector ();
                    while (sstk.hasMoreTokens()) {
                      vvec.addElement(sstk.nextToken()); 
                    }
                    tempFileStorage="";
                    for(int iii = 0; iii < vvec.size(); iii++) {
                     String tt = (String) vvec.elementAt(iii);
                     int idx7 = tt.indexOf("@");
                     if(idx7 != -1) {
                       if(sName.equals(tt.substring(0,idx7)) ||
					      siteName.equals(tt.substring(0,idx7))) {
                         tempFileStorage=tt.substring(idx7+1);
                     }
                    }
                   }
                   if(detailed) {
                     if(idx != -1) {
                       if(useDriverOn) {
                        if(isPingOverAllSuccess.isOperOk()) {
                          //callTestPing(ss+"-"+temp.substring(0,idx));
                          isPingSuccess.isOperOk(true);
                        }
                       }
                       else {
                          //callTestPing(ss+"-"+temp.substring(0,idx));
                          isPingSuccess.isOperOk(true);
                       }
                       srm = 
						connectToSRM(proxyString,true,temp.substring(idx+1));
                       if(isPingSuccess.isOperOk()) {
                         doDetailedTestCallForCopyPull
						  (proxyString,copySourceFile,
					       temp.substring(idx+1)+"."+temp.substring(0,idx),
						   temp.substring(0,idx));
                       }
                       else {
                        util.webScript
                         (publishUrlStr,displayType,
                          "srmCopy-Pull\n", "N.T.",
			              null,
				          "Notes from tester: srmPing failed and hence, " +
			              "not tried.", 
				          timeStamp,fullTimeStamp,startTimeStamp,
                          operatorTimeStamp, servicePath,noPublish,
                          siteName,numOperations,"srmCopy-Pull",
			              "srmCopy-Pull","N.T.",dailyTest,numSites,
				           localPublish,bs,bsa);
                        }
                     }
                     else {
                       if(useDriverOn) {
                        if(isPingOverAllSuccess.isOperOk()) {
                          //callTestPing(ss);
                          isPingSuccess.isOperOk(true);
                        }
                       }
                       else {
                          //callTestPing(ss);
                          isPingSuccess.isOperOk(true);
                       }
                       srm = connectToSRM(proxyString,true,temp);
                       if(isPingSuccess.isOperOk()) {
                         doDetailedTestCallForCopyPull 
							(proxyString,copySourceFile,temp, temp);
                       } 
                       else {
                        util.webScript
                         (publishUrlStr,displayType,
                          "srmCopy-Pull\n", "N.T.",
			              null,
				          "Notes from tester: srmPing failed and hence, " +
			              "not tried.", 
				          timeStamp,fullTimeStamp,startTimeStamp,
                          operatorTimeStamp, servicePath,noPublish,
                          siteName,numOperations,"srmCopy-Pull",
			              "srmCopy-Pull","N.T.",dailyTest,numSites,
				           localPublish,bs,bsa);
                       }
                     } 
                   }
                   else {   
                     boolean skipThisLocal=false;
                     String xxx = temp.substring(0,idx);
                     if(xxx.equals(testsites)) {
                        if(calledLocalCopyOnce) { 
                          skipThisLocal=true;
                        }
                        else {
                          calledLocalCopyOnce=true;
                        } 
                     }
                     if(idx != -1) {
                       if(useDriverOn) {
                        if(isPingOverAllSuccess.isOperOk()) {
                          //callTestPing(ss+"-"+temp.substring(0,idx));
                          isPingSuccess.isOperOk(true);
                        }
                       }
                       else {
                          //callTestPing(ss+"-"+temp.substring(0,idx));
                          isPingSuccess.isOperOk(true);
                       }
                       srm = connectToSRM(proxyString,true,
							temp.substring(idx+1));
                       if(isPingSuccess.isOperOk()) {
                         if(!skipThisLocal) {
                           doBasicTestCallForCopyPull(proxyString,copySourceFile,
						  	  temp.substring(idx+1)+"."+temp.substring(0,idx),
						      temp.substring(0,idx),tempFileStorage);
                         }
                       }
                       else {
                        util.webScript
                         (publishUrlStr,displayType,
                          "srmCopy-Pull\n", "N.T.",
			              null,
				          "Notes from tester: srmPing failed and hence, " +
			              "not tried.", 
				          timeStamp,fullTimeStamp,startTimeStamp,
                          operatorTimeStamp, servicePath,noPublish,
                          siteName,numOperations,"srmCopy-Pull",
			              "srmCopy-Pull","N.T.",dailyTest,numSites,
				           localPublish,bs,bsa);
                       }
                     }
                     else {
                       if(useDriverOn) {
                         if(isPingOverAllSuccess.isOperOk()) {
                           //callTestPing(ss);
                           isPingSuccess.isOperOk(true);
                         }
                       }
                       else {
                           //callTestPing(ss);
                           isPingSuccess.isOperOk(true);
                       } 
                       srm = connectToSRM(proxyString,true,temp);
                       if(isPingSuccess.isOperOk()) {
                        if(!skipThisLocal) {
                          doBasicTestCallForCopyPull( proxyString,
							copySourceFile,temp, temp, tempFileStorage);
                        }
                       }
                       else {
                        util.webScript
                         (publishUrlStr,displayType,
                          "srmCopy-Pull\n", "N.T.",
			              null,
				          "Notes from tester: srmPing failed and hence, " +
			              "not tried.", 
				          timeStamp,fullTimeStamp,startTimeStamp,
                          operatorTimeStamp, servicePath,noPublish,
                          siteName,numOperations,"srmCopy-Pull",
			              "srmCopy-Pull","N.T.",dailyTest,numSites,
				           localPublish,bs,bsa);
                       }
                     }
                   }
                 }//end else
               }//end for
           }
         }
        }
        if(ss.equalsIgnoreCase("push")) {
          StringTokenizer stk = new StringTokenizer(fileStorageType,",");
          Vector vec = new Vector ();
          while (stk.hasMoreTokens()) {
            vec.addElement(stk.nextToken()); 
          }
          String tempFileStorage="";
          for(int iii = 0; iii < vec.size(); iii++) {
             String tt = (String) vec.elementAt(iii);
             int idx = tt.indexOf("@");
             if(idx != -1) {
               if(siteName.equals(tt.substring(0,idx))) {
                 tempFileStorage=tt.substring(idx+1);
               }
             }
          }
          pushMode=true;
          if(pushModeGiven) {
            changeToCopyServicePath=true;
          }
          else {
            changeToCopyServicePath=false;
          }
          if(pushModeGiven) {
            if(useDriverOn) {
              if(isPingOverAllSuccess.isOperOk()) {
                  //callTestPing(ss);
                  isPingSuccess.isOperOk(true);
               }
            }
            else {
                //callTestPing(ss);
                isPingSuccess.isOperOk(true);
            }
            srm = connectToSRM(proxyString,true,copySourceFile);
            if(isPingSuccess.isOperOk()) {
              doBasicTestCallForCopyPush
		       (proxyString, copySourceFile, copyTarget,"",tempFileStorage);
            }
            else {
               Date d = new Date();
               String operatorTimeStamp = d.toString();
               util.webScript
                 (publishUrlStr,displayType,
                   "srmCopy-Push\n", "N.T.", null,
				   "Notes from tester: srmPing failed and hence, " +
			       "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmCopy-Push",
			        "srmCopy-Push","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
             } 
          }
          else {
          StringTokenizer sstk = new StringTokenizer(pushCopySourceFile,",");

          Vector vec1 = new Vector ();
          while(sstk.hasMoreTokens()) {
            vec1.addElement(sstk.nextToken()); 
          }
          sstk = new StringTokenizer(pullCopySrms,",");
          vec = new Vector ();
          while(sstk.hasMoreTokens()) {
            vec.addElement(sstk.nextToken()); 
          }
          boolean disable = false;
          if(!isPutSuccess.isOperOk()) {
               //disable = true;
               //commented on march 27th, the reason wanted to try push without
               //put or anything for the diagtool.
          } 
          else { 
            for(int jj = 0; jj < vec1.size(); jj++) {
               String ttemp = (String)vec1.elementAt(jj);
               int idx3 = ttemp.indexOf("@");
               if(idx3 != -1) {
                  /*
                  System.out.println("\nTTEMP " + ttemp.substring(0,idx3));
                  System.out.println("\nSiteName " + siteName);
                  System.out.println("is true " +
						ttemp.substring(0,idx3).equals(siteName));
                   */
                 if(ttemp.substring(0,idx3).equals(siteName)) {
                  //System.out.println(ttemp.substring(idx3+1));
                   if(ttemp.substring(idx3+1).equals("disable=true")) {
                     disable = true;
                   }
                   break;
                 }
               }
             }
          }
          
          for(int ii = 0; ii < vec1.size(); ii++) { 
            String ttemp = (String)vec1.elementAt(ii);
            String temp = (String)vec.elementAt(ii);
            boolean skipThisLocal=false;
            int idx2 = ttemp.indexOf("@");
            if(idx2 != -1) {
             String xxx = ttemp.substring(0,idx2);
             if(xxx.equals(testsites)) {
                if(calledLocalCopyOnce) {
                  skipThisLocal=true;
                }
                else {
                  calledLocalCopyOnce=true;
                }
             }
            }
            if(disable || skipThisLocal) {
               //callTestPing(ss+"-"+ttemp.substring(0,idx2));
               Date d = new Date();
               String operatorTimeStamp = d.toString();
               util.webScript
                  (publishUrlStr,displayType,
                    "Copy Request \n", "Sending", null,
                    "Notes from tester: copy request is skipped",
					timeStamp,
				    fullTimeStamp, startTimeStamp, operatorTimeStamp,
					servicePath,noPublish,
                    siteName,numOperations,ss+"-"+ttemp.substring(0,idx2),
				    "","Sending",dailyTest,numSites,localPublish,bs,bsa);
               util.webScript
                  (publishUrlStr,displayType,
                    "Copy Request \n", "N.T.", null,
                     "Notes from tester: copy request is skipped",
					 timeStamp,
				     fullTimeStamp, startTimeStamp, operatorTimeStamp,
					 servicePath,noPublish,
                     siteName,numOperations,ss+"-"+ttemp.substring(0,idx2),
					 "","N.T.",dailyTest,numSites,localPublish,bs,bsa);
               util.webScript
                  (publishUrlStr,displayType,
                    "Copy Request \n", "SKIPTHIS", null,
                     "Notes from tester: copy request is skipped",
				 	 timeStamp,
				     fullTimeStamp, startTimeStamp, operatorTimeStamp,
					 servicePath,noPublish,
                     siteName,numOperations,ss+"-"+ttemp.substring(0,idx2),
					 "srmCopy-push-"+ttemp.substring(0,idx2),
					 "N.T.",dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage(
					"SRM-TESTER: Skipping all copy push operations for this site " + 
				    siteName,logger,pIntf);
            }
            else  {
               if(useDriverOn) {
                 if(isPingOverAllSuccess.isOperOk()) {
                  //callTestPing(ss+"-"+ttemp.substring(0,idx2));
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
                 }
               }
               else {
                  //callTestPing(ss+"-"+ttemp.substring(0,idx2));
                  srm = connectToSRM(proxyString,false,"");
                  isPingSuccess.isOperOk(true);
               }
               int idx3 = temp.indexOf("@"); 
               //srm = connectToSRM(proxyString,true,temp);
               String sName = ttemp; 
               if(idx3 != -1) {
                sName = ttemp.substring(0,idx2);
               }
               sstk = new StringTokenizer(fileStorageType,",");
               Vector vvec = new Vector ();
               while (sstk.hasMoreTokens()) {
                 vvec.addElement(sstk.nextToken()); 
               }
               tempFileStorage="";
               for(int iii = 0; iii < vvec.size(); iii++) {
                 String tt = (String) vvec.elementAt(iii);
                 int idx = tt.indexOf("@");
                 if(idx != -1) {
                   if(sName.equals(tt.substring(0,idx))) {
                     tempFileStorage=tt.substring(idx+1);
                   }
                 }
               }
               if(detailed) {
                 if(idx3 != -1) {
                  doDetailedTestCallForCopyPush 
						(proxyString,copySourceFile,
						  temp.substring(idx3+1)+"."+ttemp.substring(0,idx2),
						  ttemp.substring(0,idx2));
                 }
                 else {
                  doDetailedTestCallForCopyPush 
						(proxyString,copySourceFile,
						  temp+"."+ttemp.substring(0,idx2),ttemp.substring(0,idx2));
                 }
               }
               else {   
                  if(idx3 != -1) {
                    doBasicTestCallForCopyPush
			           (proxyString, copySourceFile,
							temp.substring(idx3+1)+"."+ttemp.substring(0,idx2),
						    ttemp.substring(0,idx2),tempFileStorage);
                  }
                  else {
                    doBasicTestCallForCopyPush
			           (proxyString, copySourceFile,
							temp+"."+ttemp.substring(0,idx2),
							ttemp.substring(0,idx2),tempFileStorage);
                  }
               }
              }
           }//end for 
         }
        } 
        if(ss.equalsIgnoreCase("mv")||
			ss.equalsIgnoreCase("srmmv")) {
         boolean goahead = false;
         if(isPutGiven && isPutSuccess.isOperOk()) {
           goahead=true;
         }
         else if(!isPutGiven) {
           goahead=true;
         }
         if(goahead) {
            if(useDriverOn) {
              if(isPingOverAllSuccess.isOperOk()) {
                //callTestPing("srmMv");
                srm = connectToSRM(proxyString,false,"");
                isPingSuccess.isOperOk(true);
              }
            }
            else {
                srm = connectToSRM(proxyString,false,"");
                isPingSuccess.isOperOk(true);
                //callTestPing("srmMv");
            }
            if(isPingSuccess.isOperOk()) {
              if(detailed) {
                doDetailedTestCallForMv(proxyString);
              }
              else {   
                doBasicTestCallForMv(proxyString);
              }
            }
            else {
                Date d = new Date();
                String operatorTimeStamp = d.toString();
                util.webScript
                  (publishUrlStr,displayType,
                   "srmMv\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmMv",
			        "srmMv","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
             }
          }
          else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
              (publishUrlStr,displayType,
               "srmMv Request \n","Sending",
                null, 
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmMv is skipped",
				timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
		        servicePath,noPublish,
                siteName,numOperations,"srmMv",
				"srmMv","Sending",dailyTest,
			    numSites,localPublish,bs,bsa);
              util.webScript
               (publishUrlStr,displayType,
                "srmMv Request\n", "N.T.",
			    null,
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmMv is skipped",
			    timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
			    servicePath,noPublish,
                siteName,numOperations,"srmMv",
			    "srmMv","N.T.",dailyTest,numSites,
				localPublish,bs,bsa);
               util.webScript
                (publishUrlStr,displayType,
                  "srmMv \n","SKIPTHIS",
                   null,
                   "Notes from tester: srmPrepareToPut is not ok. "+
			       "so, srmMv is skipped",
				   timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,"srmMv",
				   "srmMv", "N.T.",
				   dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage(
				   "\nSkipping the mv operations for this site " + 
					 siteName,logger,pIntf);
          }
        }
        if(ss.equalsIgnoreCase("ls") || ss.equalsIgnoreCase("browse") ||
           ss.equalsIgnoreCase("srmls")) {
         boolean goahead = false;
         if(isPutGiven && isPutSuccess.isOperOk()) {
           goahead=true;
         }
         else if(!isPutGiven) {
           goahead=true;
         }
         if(goahead) {
            if(useDriverOn) {
              if(isPingOverAllSuccess.isOperOk()) {
                //callTestPing("srmLs");
                srm = connectToSRM(proxyString,false,"");
                isPingSuccess.isOperOk(true);
              }
            }
            else {
                srm = connectToSRM(proxyString,false,"");
                isPingSuccess.isOperOk(true);
                //callTestPing("srmLs");
            }
            if(isPingSuccess.isOperOk()) {
              if(detailed) {
                 doDetailedTestCallForSrmLs(proxyString);
              }
              else {   
                 doBasicTestCallForSrmLs(proxyString);
              }
            }
            else {
                Date d = new Date();
                String operatorTimeStamp = d.toString();
                util.webScript
                  (publishUrlStr,displayType,
                   "srmLs\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmLs",
			        "srmLs","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
             }
          }
          else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
              (publishUrlStr,displayType,
               "srmLs Request \n","Sending",
                null,
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmMv is skipped",
				timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
		        servicePath,noPublish,
                siteName,numOperations,"srmLs",
				"srmLs","Sending",dailyTest,
			    numSites,localPublish,bs,bsa);
              util.webScript
               (publishUrlStr,displayType,
                "srmLs Request\n", "N.T.",
			    null,
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmMv is skipped",
				timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
			    servicePath,noPublish,
                siteName,numOperations,"srmLs",
			    "srmLs","N.T.",dailyTest,numSites,
				localPublish,bs,bsa);
               util.webScript
                (publishUrlStr,displayType,
                  "srmLs \n","SKIPTHIS",
                   null,
                   "Notes from tester: srmPrepareToPut is not ok. "+
			       "so, srmMv is skipped",
				   timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,"srmLs",
				   "srmLs", "N.T.",
				   dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage(
				   "\nSkipping the ls operations for this site " + 
					siteName,logger,pIntf);
          }
        }
        if(ss.equalsIgnoreCase("mkdir")) {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmMkdir");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
            }
          }
          else {
               //callTestPing("srmMkdir");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            if(detailed) {
              doDetailedTestCallForMkdir(proxyString);
            }
            else {   
              doBasicTestCallForMkdir(proxyString);
            }
          }
          else {
                Date d = new Date();
                String operatorTimeStamp = d.toString();
                util.webScript
                  (publishUrlStr,displayType,
                   "srmMkdir\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmMkdir",
			        "srmMkdir","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
           }
        }
        if(ss.equalsIgnoreCase("rmdir")) {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmRmdir");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
            }
          }
          else {
               //callTestPing("srmRmdir");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
           if(detailed) {
             doDetailedTestCallForRmdir(proxyString);
           }
           else {   
             doBasicTestCallForRmdir(proxyString);
           }
          }
          else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                (publishUrlStr,displayType,
                   "srmRmdir\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmRmdir",
			        "srmRmdir","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
           }
        }
        if(ss.equalsIgnoreCase("srmrm") || ss.equalsIgnoreCase("rm")) {
         boolean goahead = false;
         if(isPutGiven && isPutSuccess.isOperOk()) {
           goahead=true;
         }
         else if(!isPutGiven) {
           goahead=true;
         }
          if(goahead) {
            if(useDriverOn) {
              if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmRm");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
              }
            }
            else {
               //callTestPing("srmRm");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
            }
            if(isPingSuccess.isOperOk()) {
              if(detailed) {
                doDetailedTestCallForSrmRm(proxyString);
              }
              else {   
                doBasicTestCallForSrmRm(proxyString);
              }
            }
            else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                (publishUrlStr,displayType,
                   "srmRm\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmRm",
			        "srmRm", "N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
             }
          }
          else {
             Date d = new Date();
             String operatorTimeStamp = d.toString();
             util.webScript
              (publishUrlStr,displayType,"Sending",
               "srmRm Request \n",
                null,
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmRm is skipped",
				timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
		        servicePath,noPublish,
                siteName,numOperations,"srmRm",
				"srmRm","Sending",dailyTest,
			    numSites,localPublish,bs,bsa);
              util.webScript
               (publishUrlStr,displayType,
                "srmRm Request\n", "N.T.",
			    null,
                "Notes from tester: srmPrepareToPut is not ok. "+
			    "so, srmRm is skipped",
				timeStamp,fullTimeStamp,startTimeStamp,
                operatorTimeStamp,
			    servicePath,noPublish,
                siteName,numOperations,"srmRm",
			    "srmRm","N.T.",dailyTest,numSites,
				localPublish,bs,bsa);
               util.webScript
                (publishUrlStr,displayType,
                  "srmRm \n", "SKIPTHIS",
                   null,
                   "Notes from tester: srmPrepareToPut is not ok. "+
			       "so, srmRm is skipped",
				   timeStamp,fullTimeStamp,startTimeStamp,
                   operatorTimeStamp,
				   servicePath,noPublish,
                   siteName,numOperations,"srmRm",
				   "srmRm", "N.T.",
				   dailyTest,numSites,localPublish,bs,bsa);
               util.printMessage(
				   "\nSkipping the rm operations for this site " + 
					siteName,logger,pIntf);
          }
        }
        if(ss.equalsIgnoreCase("3rdcopy")) {
           if(useDriverOn) {
              if(isPingOverAllSuccess.isOperOk()) {
               callTestPing("3PartyCopy");
               do3PartyCopy(proxyString);
              }
           }
           else {
             callTestPing("3PartyCopy");
             do3PartyCopy(proxyString);
           }
        }
        if(ss.equalsIgnoreCase("updatespace") || 
			ss.equalsIgnoreCase("update")) {
           SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
         try {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
               //callTestPing("srmUpdateSpace");
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
            }
          }
          else {
            //callTestPing("srmUpdateSpace");
            srm = connectToSRM(proxyString,false,"");
            isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            //just a dummy sLock, just for the setCompleted call.
            //we don't call a sLock.get and halt the thread here,  
            //because it is just only single thread
            doUpdateSpace(srm,uid,spaceToken,newSpaceSize,newSpaceTime);
           }
           else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                (publishUrlStr,displayType,
                   "srmUpdateSpace\n", "N.T.", 
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmUpdateSpace",
			        "srmUpdateSpace","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
            }
         }catch(Exception e) {
          Date d = new Date ();
          String operatorTimeStamp = d.toString();
          try {
            ReportRequestStatus rrs = util.createReportRequestStatus("","","");
            rrs.setLinkName("updatespace");
            rrs.setActualResult("Exception");
            rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
            rrs.setSpaceToken(spaceToken);
            rrs.setNewSpaceSize(""+newSpaceSize);
            rrs.setNewLifeTime(""+newSpaceTime);
            util.printMessageHException("\n"+e.toString(),logger,pIntf);
            util.webScript(publishUrlStr,
             displayType,"SendingUPDATESPACE","SrmUpdateSpace", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		     servicePath,noPublish,
	         siteName,numOperations,"srmUpdateSpace","",
			 "Exception",dailyTest,numSites,localPublish,bs,bsa);
           rrs.setLinkName("updatespace");
           util.webScript(publishUrlStr,
             displayType,"UpdateSpace","Exception", rrs,
             "Notes from tester: " +e.toString(), 
		     timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp,
			 servicePath,noPublish, siteName,numOperations,
		     "srmUpdateSpace","srmUpdateSpace",
		     "Exception", dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception ee) {ee.printStackTrace();}
         }
         if(sLock != null) sLock.setIncrementCount();
        }
        if(ss.equalsIgnoreCase("releasespace") || 
			ss.equalsIgnoreCase("release")) {
           SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
         try {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //callTestPing("srmReleaseSpace");
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else {
            //callTestPing("srmReleaseSpace");
            srm = connectToSRM(proxyString,false,"");
            isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
             //just a dummy sLock, just for the setCompleted call.
             //we don't call a sLock.get and halt the thread here,  
             //because it is just only single thread
             doReleaseSpace(srm,uid,spaceToken);
           }
           else {
              Date d = new Date();
              String operatorTimeStamp = d.toString();
              util.webScript
                (publishUrlStr,displayType,
                   "srmReleaseSpace\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmReleaseSpace",
			        "srmReleaseSpace","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
             }
         }catch(Exception e) {
          Date d = new Date();
          String operatorTimeStamp = d.toString();
          try {
            ReportRequestStatus rrs = util.createReportRequestStatus("","","");
            rrs.setLinkName("releasespace");
            rrs.setActualResult("Exception");
            rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
            rrs.setSpaceToken(spaceToken);
            util.printMessageHException("\n"+e.toString(),logger,pIntf);
            util.webScript(publishUrlStr,
             displayType,"SendingRELEASESPACE","SrmReleaseSpace", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			 servicePath,noPublish,
	         siteName,numOperations,"srmReleaseSpace","",
				"Exception",dailyTest,numSites,localPublish,bs,bsa);
           rrs.setLinkName("releasespace");
           util.webScript(publishUrlStr,
             displayType,"ReleaseSpace","Exception", rrs,
             "Notes from tester" + 	e.toString(), 
		     timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
		     "srmReleaseSpace","srmReleaseSpace",
		     "Exception", dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception ee) {ee.printStackTrace();}
         }
         if(sLock != null) sLock.setIncrementCount();
        }
        if(ss.equalsIgnoreCase("getspacemeta")) {
         Date d = new Date (); 
         String operatorTimeStamp = d.toString();
         SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
         try {
          if(useDriverOn) {
            if(isPingOverAllSuccess.isOperOk()) {
              //callTestPing("srmGetSpaceMeta");
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
            }
          }
          else {
              //callTestPing("srmGetSpaceMeta");
              srm = connectToSRM(proxyString,false,"");
              isPingSuccess.isOperOk(true);
          }
          if(isPingSuccess.isOperOk()) {
            //just a dummy sLock, just for the setCompleted call.
            //we don't call a sLock.get and halt the thread here,  
            //because it is just only single thread
            doGetSpaceMetaData(srm,uid,spaceToken);
           }
           else {
              util.webScript
                (publishUrlStr,displayType,
                   "srmGetSpaceMetaData\n","N.T.", 
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmGetSpaceMetaData",
			        "srmGetSpaceMetaData","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
            }
         }catch(Exception e) {
          try {
            ReportRequestStatus rrs = util.createReportRequestStatus("","","");
            rrs.setLinkName("getspacemeta");
            rrs.setActualResult("Exception");
            rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
            rrs.setSpaceToken(spaceToken);
            util.printMessageHException("\n"+e.toString(),logger,pIntf);
            util.webScript(publishUrlStr,
             displayType,"SendingGETSPACEMETA","GetSpaceMeta", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			 servicePath,noPublish,
	         siteName,numOperations,"srmGetSpaceMeta","","Exception",
				dailyTest,numSites,localPublish,bs,bsa);
            rrs.setSpaceToken(spaceToken);
            rrs.setLinkName("getspacemeta");
            util.webScript(publishUrlStr,
             displayType,"GetSpaceMeta","Exception", rrs,
             "Notes from tester: " + e.toString(), 
			 timeStamp, fullTimeStamp, startTimeStamp, 
             operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
		     "srmGetSpaceMeta","srmGetSpaceMetaData","Exception", 
			 dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception ee) {ee.printStackTrace();}
         }
         if(sLock != null) sLock.setIncrementCount();
        }
        if(ss.equalsIgnoreCase("reservespace") || 
			ss.equalsIgnoreCase("reserve")) {
         Date d = new Date(); 
         String operatorTimeStamp = d.toString();
         SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
         String exceptionMessage = "";
         try {
           if(useDriverOn) {
             if(isPingOverAllSuccess.isOperOk()) {
               //exceptionMessage = callTestPing("srmReserveSpace");
               exceptionMessage="";
               srm = connectToSRM(proxyString,false,"");
               isPingSuccess.isOperOk(true);
             }
           }
           else {
             //exceptionMessage = callTestPing("srmReserveSpace");
             exceptionMessage="";
             srm = connectToSRM(proxyString,false,"");
             isPingSuccess.isOperOk(true);
           }
           if(isPingSuccess.isOperOk()) { 
             //just a dummy sLock, just for the setCompleted call.
             //we don't call a sLock.get and halt the thread here,  
             //because it is just only single thread
             doReserveSpace(srm,uid,exceptionMessage);
           } 
           else {
              util.webScript
                (publishUrlStr,displayType,
                   "srmReserveSpace\n", "N.T.",
			        null,
				    "Notes from tester: srmPing failed and hence, " +
			        "not tried.", 
				    timeStamp,fullTimeStamp,startTimeStamp,
                    operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmReserveSpace",
			        "srmReserveSpace","N.T.",dailyTest,numSites,
				    localPublish,bs,bsa);
            }
         }catch(Exception e) { 
          try {
            ReportRequestStatus rrs = util.createReportRequestStatus("","","");
            rrs.setActualResult("Exception");
            rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
            rrs.setLinkName("reservespace");
            rrs.setUserId(uid);
            rrs.setTotalSpace(""+1000);
            rrs.setGuarnSpace(""+1000);
            rrs.setLifeTime(""+10000);
            util.printMessageHException("\n"+e.toString(),logger,pIntf);
            util.webScript(publishUrlStr,
             displayType,"SendingRESERVESPACE","SrmReserveSpace", rrs,
             "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
			 servicePath,noPublish,
	         siteName,numOperations,"srmReserveSpace","",
				"Exception",dailyTest,numSites,localPublish,bs,bsa);
           rrs.setLinkName("reservespace");
           util.webScript(publishUrlStr,
             displayType,"ReserveSpace","Exception", rrs,
             "Notes from tester: " + e.toString(), 
			 timeStamp, fullTimeStamp, startTimeStamp,
             operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
			 "srmReserveSpace","srmReserveSpace-OverAll","Exception", 
			 dailyTest,numSites,localPublish,bs,bsa);
          }catch(Exception ee) {ee.printStackTrace();}
        }
        if(sLock != null) sLock.setIncrementCount();
       }
      }//end else
      }//end for

           
        util.printMessage("\n\n=========================================",logger,pIntf);
        util.writeSummary(localPublish,bs,bsa,"\n\n=========================================");
        util.printMessage("                Summary",logger,pIntf);
        util.writeSummary(localPublish,bs,bsa,"<B><I>Summary</I></B>");
        util.printMessage("=========================================\n",logger,pIntf);
        util.writeSummary(localPublish,bs,bsa,"=========================================\n");
        util.printMessage("ServicePath:"+servicePath,logger,pIntf);
        util.writeSummary(localPublish,bs,bsa,"<B><I>ServicePath</I></B>:"+servicePath);
        util.printMessage("Date:"+new Date()+"\n",logger,pIntf);
        util.writeSummary(localPublish,bs,bsa,"<B><I>Date:</I></B>"+new Date()+"\n");
        if(isPingGiven) {
        if(isPingOverAllSuccess.isTried()) {
          if(isPingOverAllSuccess.isOperOk()) {
             util.printMessage("\tsrmPing : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPing : Ok");
          }
          else {
             if(isPingOverAllSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmPing : Failed",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmPing : Failed");
             }
             else {
               util.printMessage("\tsrmPing : "+isPingOverAllSuccess.getExplanation(),
					logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmPing : "+isPingOverAllSuccess.getExplanation());
             }
          }
        }
        else {
             util.printMessage("\tsrmPing : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPing : N.T.");
        }
        }

        if(isGetTransferProtocolsGiven) {

        if(isGetTransferProtocolsSuccess.isTried()) {
          if(isGetTransferProtocolsSuccess.isOperOk()) {
             util.printMessage("\tsrmGetTransferProtocols : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetTransferProtocols : Ok");
          }
          else {
             if(isGetTransferProtocolsSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmGetTransferProtocols : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetTransferProtocols : Failed");
             }
             else {
               util.printMessage("\tsrmGetTransferProtocols : "+
					isGetTransferProtocolsSuccess.getExplanation(), logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetTransferProtocols : "+
					isGetTransferProtocolsSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmGetTransferProtocols : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetTransferProtocols : N.T.");
        }
        }
        if(isPutGiven) {
        if(isPutSuccess.isTried()) {
          if(isPutSuccess.isOperOk()) {
             util.printMessage("\tsrmPrepareToPut : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToPut : Ok");
          }
          else {
             if(isPutSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmPrepareToPut : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToPut : Failed");
             }
             else {
               util.printMessage("\tsrmPrepareToPut : "+isPutSuccess.getExplanation(),
				logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmPrepareToPut : "+isPutSuccess.getExplanation());
             }
          }
        }
        else {
             util.printMessage("\tsrmPrepareToPut : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToPut : N.T.");
        }
        if(isPutSuccess.getSubOperations().size() > 0) {
          HashMap map = isPutSuccess.getSubOperations();
          OperOk putStatusOk = (OperOk) map.get("putstatus");
          if(putStatusOk != null) {
            if(putStatusOk.isTried()) {
               if(putStatusOk.isOperOk()) {
                 util.printMessage("\tsrmStatusOfPutRequest : Ok",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfPutRequest : Ok");
               }
               else {
                  if(putStatusOk.getExplanation().equals("")) {
                     util.printMessage("\tsrmStatusOfPutRequest : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfPutRequest : Failed");
                  }
                  else {
                     util.printMessage("\tsrmStatusOfPutRequest : "+
							putStatusOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tsrmStatusOfPutRequest : "+putStatusOk.getExplanation());
                  }
               }
            }
            else {
               util.printMessage("\tsrmStatusOfPutRequest : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
				"\tsrmStatusOfPutRequest : N.T.");
            }
          }
          else {
             util.printMessage("\tsrmStatusOfPutRequest : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,
				"\tsrmStatusOfPutRequest : N.T.");
          }
          OperOk gsiftpPutOk = (OperOk) map.get("gsiftp-put");
          if(gsiftpPutOk != null) {
            if(gsiftpPutOk.isTried()) {
             if(gsiftpPutOk.isOperOk()) {  
               util.printMessage("\tgsiftp-put: Ok", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tgsiftp-put: Ok");
             }
             else {
                  if(gsiftpPutOk.getExplanation().equals("")) {
                     util.printMessage("\tgsiftp-put : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,"\tgsiftp-put : Failed");
                  }
                  else {
                     util.printMessage("\tgsiftp-put : "+gsiftpPutOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tgsiftp-put : "+gsiftpPutOk.getExplanation());
                  }
            }
           }
           else {
             util.printMessage("\tgsiftp-put : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tgsiftp-put : N.T.");
           }
          }
          else {
             util.printMessage("\tgsiftp-put : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tgsiftp-put : N.T.");
          }
          OperOk putDoneOk = (OperOk) map.get("putdone");
          if(putDoneOk != null) {
            if(putDoneOk.isTried()) {
               if(putDoneOk.isOperOk()) {
                 util.printMessage("\tsrmPutDone : Ok",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmPutDone : Ok");
               }
               else {
                  if(putDoneOk.getExplanation().equals("")) {
                     util.printMessage("\tsrmPutDone : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,"\tsrmPutDone : Failed");
                  }
                  else {
                     util.printMessage("\tsrmPutDone : "+putDoneOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tsrmPutDone : "+putDoneOk.getExplanation());
                  }
               }
            }
            else {
               util.printMessage("\tsrmPutDone : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmPutDone : N.T.");
            }
          }
          else {
             util.printMessage("\tsrmPutDone : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPutDone : N.T.");
          }
          OperOk lsOk = (OperOk) map.get("srmls");
          if(lsOk != null) {
            srmLsVec.addElement(lsOk);
          }
          OperOk rmOk = (OperOk) map.get("srmrm");
          if(rmOk != null) {
            srmRmVec.addElement(rmOk);
          }
        }
        else {
             util.printMessage("\tsrmPutDone : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPutDone : N.T.");
        } 
        }
        if(isGetGiven) {
        if(isGetSuccess.isTried()) {
          if(isGetSuccess.isOperOk()) {
             util.printMessage("\tsrmPrepareToGet : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToGet : Ok");
          }
          else {
             if(isGetSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmPrepareToGet : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToGet : Failed");
             }
             else {
               util.printMessage("\tsrmPrepareToGet : "+
					isGetSuccess.getExplanation(), logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToGet : "+
					isGetSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmPrepareToGet : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmPrepareToGet : N.T.");
        }
        if(isGetSuccess.getSubOperations().size() > 0) {
          HashMap map = isGetSuccess.getSubOperations();
          OperOk getStatusOk = (OperOk) map.get("getstatus");
          if(getStatusOk != null) {
            if(getStatusOk.isTried()) {
               if(getStatusOk.isOperOk()) {
                 util.printMessage("\tsrmStatusOfGetRequest : Ok",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfGetRequest : Ok");
               }
               else {
                  if(getStatusOk.getExplanation().equals("")) {
                     util.printMessage("\tsrmStatusOfGetRequest : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfGetRequest : Failed");
                  }
                  else {
                     util.printMessage("\tsrmStatusOfGetRequest : "+getStatusOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tsrmStatusOfGetRequest : "+getStatusOk.getExplanation());
                  }
               }
            }
            else {
               util.printMessage("\tsrmStatusOfGetRequest : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfGetRequest : N.T.");
            }
          }
          else {
             util.printMessage("\tsrmStatusOfGetRequest : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfGetRequest : N.T.");
          }
          OperOk gsiftpGetOk = (OperOk) map.get("gsiftp-get");
          if(gsiftpGetOk != null) {
            if(gsiftpGetOk.isTried()) {
             if(gsiftpGetOk.isOperOk()) {  
               util.printMessage("\tgsiftp-get: Ok", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tgsiftp-get: Ok");
             }
             else {
                  if(gsiftpGetOk.getExplanation().equals("")) {
                     util.printMessage("\tgsiftp-get : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,"\tgsiftp-get : Failed");
                  }
                  else {
                     util.printMessage("\tgsiftp-get : "+gsiftpGetOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tgsiftp-get : "+gsiftpGetOk.getExplanation());
                  }
            }
           }
           else {
             util.printMessage("\tgsiftp-get : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tgsiftp-get : N.T.");
           }
          }
          else {
             util.printMessage("\tgsiftp-get : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tgsiftp-get : N.T.");
          }
        }
       } 

        if(isGetSuccess.getSubOperations().size() > 0) {
          HashMap map = isGetSuccess.getSubOperations();
          OperOk srmReleaseOk = (OperOk) map.get("srmrelease");
          srmReleaseVec.addElement(srmReleaseOk);
        }
        if(srmReleaseVec.size() == 0) {
            if(isReleaseGiven || isGetGiven) {
             util.printMessage("\tsrmReleaseFiles : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseFiles : N.T.");
            }
        }
        int releaseCount = 0;
        OperOk srmReleaseOk=null; 
        for(int m = 0; m < srmReleaseVec.size(); m++) {
           srmReleaseOk = (OperOk) srmReleaseVec.elementAt(m);
           if(srmReleaseOk.isTried()) {
              if(srmReleaseOk.isOperOk()) {
                 releaseCount ++;
                 break;
              }
           }
        }
        if(isReleaseGiven || isGetGiven) {
        if(releaseCount > 0) {
           util.printMessage("\tsrmReleaseFiles : Ok",logger,pIntf);
           util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseFiles : Ok");
        }
        else {
           if(srmReleaseOk != null) {
             if(srmReleaseOk.isTried()) {
               if(srmReleaseOk.getExplanation().equals("")) {  
                 util.printMessage("\tsrmReleaseFiles : Failed",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseFiles : Failed");
               }
               else {
                 util.printMessage("\tsrmReleaseFiles : "+srmReleaseOk.getExplanation(),logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,
					"\tsrmReleaseFiles : "+srmReleaseOk.getExplanation());
               }
             }
             else {
               util.printMessage("\tsrmReleaseFiles : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseFiles : N.T.");
             }
           }
        }
        }
        if(isBringOnlineGiven) {
        if(isBringOnlineSuccess.isTried()) {
          if(isBringOnlineSuccess.isOperOk()) {
             util.printMessage("\tsrmBringOnline : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmBringOnline : Ok");
          }
          else {
             if(isBringOnlineSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmBringOnline : Failed", logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmBringOnline : Failed");
             }
             else {
               util.printMessage("\tsrmBringOnline : "+
				 isBringOnlineSuccess.getExplanation(), logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmBringOnline : "+
				 isBringOnlineSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmBringOnline : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmBringOnline : N.T.");
        }
        if(isBringOnlineSuccess.getSubOperations().size() > 0) {
          HashMap map = isBringOnlineSuccess.getSubOperations();
          OperOk bStatusOk = (OperOk) map.get("bringonlinestatus");
          if(bStatusOk != null) {
            if(bStatusOk.isTried()) {
               if(bStatusOk.isOperOk()) {
                 util.printMessage("\tsrmStatusOfBringOnlineRequest : Ok",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmStatusOfBringOnlineRequest : Ok");
               }
               else {
                  if(bStatusOk.getExplanation().equals("")) {
                     util.printMessage("\tsrmStatusOfBringOnlineRequest : Failed",logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tsrmStatusOfBringOnlineRequest : Failed");
                  }
                  else {
                     util.printMessage("\tsrmStatusOfBringOnlineRequest : "+
						bStatusOk.getExplanation(),logger,pIntf);
                     util.writeSummary(localPublish,bs,bsa,
						"\tsrmStatusOfBringOnlineRequest : "+bStatusOk.getExplanation());
                  }
               }
            }
            else {
               util.printMessage("\tsrmStatusOfBringOnlineRequest : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
				"\tsrmStatusOfBringOnlineRequest : N.T.");
            }
          }
          else {
             util.printMessage("\tsrmStatusOfBringOnlineRequest : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,
				"\tsrmStatusOfBringOnlineRequest : N.T.");
          }
          }
        } 
        if(isReserveSpaceGiven) {

        if(isReserveSpaceSuccess.isTried()) {
          if(isReserveSpaceSuccess.isOperOk()) {
             util.printMessage("\tsrmReserveSpace : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmReserveSpace : Ok");
          }
          else {
             if(isReserveSpaceSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmReserveSpace : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmReserveSpace : Failed");
             }
             else {
               util.printMessage("\tsrmReserveSpace : "+isReserveSpaceSuccess.getExplanation(),
			  	  logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmReserveSpace : "+isReserveSpaceSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmReserveSpace : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmReserveSpace : N.T.");
        }
        }
        if(isGetSpaceMetaGiven) {
        if(isGetSpaceMetadataSuccess.isTried()) {
          if(isGetSpaceMetadataSuccess.isOperOk()) {
             util.printMessage("\tsrmGetSpaceMetadata : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceMetadata : Ok");
          }
          else {
             if(isGetSpaceMetadataSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmGetMetadata : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetMetadata : Failed");
             }
             else {
               util.printMessage("\tsrmGetMetadata : "+
					isGetSpaceMetadataSuccess.getExplanation(), logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetMetadata : "+
					isGetSpaceMetadataSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmGetSpaceMetadata : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceMetadata : N.T.");
        }
        }
        if(isGetSpaceTokensGiven) {
        if(isGetSpaceTokensSuccess.isTried()) {
          if(isGetSpaceTokensSuccess.isOperOk()) {
             util.printMessage("\tsrmGetSpaceTokens : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceTokens : Ok");
          }
          else {
             if(isGetSpaceTokensSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmGetSpaceTokens : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceTokens : Failed");
             }
             else {
               util.printMessage("\tsrmGetSpaceTokens: "+
					isGetSpaceTokensSuccess.getExplanation(), logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceTokens: "+
					isGetSpaceTokensSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmGetSpaceTokens : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmGetSpaceTokens : N.T.");
        }
        }
        if(isReleaseSpaceGiven) {
        if(isReleaseSpaceSuccess.isTried()) {
          if(isReleaseSpaceSuccess.isOperOk()) {
             util.printMessage("\tsrmReleaseSpace : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseSpace : Ok");
          }
          else {
             if(isReleaseSpaceSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmReleaseSpace : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseSpace : Failed");
             }
             else {
               util.printMessage("\tsrmReleaseSpace : "+isReleaseSpaceSuccess.getExplanation(),
			  	  logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
				  "\tsrmReleaseSpace : "+isReleaseSpaceSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmReleaseSpace : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmReleaseSpace : N.T.");
        }
        }
        if(isMvGiven) {
        if(isMvSuccess.isTried()) {
          if(isMvSuccess.isOperOk()) {
             util.printMessage("\tsrmMv : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmMv : Ok");
          }
          else {
             if(isMvSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmMv : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmMv : Failed");
             }
             else {
               util.printMessage("\tsrmMv : "+isMvSuccess.getExplanation(),
			  	  logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmMv : "+isMvSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmMv : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmMv : N.T.");
        }
        }
        if(isMkdirGiven) {
        if(isMkdirSuccess.isTried()) {
          if(isMkdirSuccess.isOperOk()) {
             util.printMessage("\tsrmMkdir : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmMkdir : Ok");
          }
          else {
             if(isMkdirSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmMkdir : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmMkdir : Failed");
             }
             else {
               util.printMessage("\tsrmMkdir : "+isMkdirSuccess.getExplanation(),
			  	  logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmMkdir : "+isMkdirSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmMkdir : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmMkdir : N.T.");
        }
        }
        if(isRmdirGiven) {
        if(isRmdirSuccess.isTried()) {
          if(isRmdirSuccess.isOperOk()) {
             util.printMessage("\tsrmRmdir : Ok",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmRmdir : Ok");
          }
          else {
             if(isRmdirSuccess.getExplanation().equals("")) {
               util.printMessage("\tsrmRmdir : Failed", logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmRmdir : Failed");
             }
             else {
               util.printMessage("\tsrmRmdir : "+isRmdirSuccess.getExplanation(),
			  	  logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,
					"\tsrmRmdir : "+isRmdirSuccess.getExplanation());
             } 
          }
        }
        else {
             util.printMessage("\tsrmRmdir : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmRmdir : N.T.");
        }
        }

        if(isCopyGiven) {
        if(isCopySuccess.isTried()) {
          if(isCopySuccess.getSubOperations().size() > 0) {
             HashMap map = isCopySuccess.getSubOperations();
             Set set = map.keySet();
             Object[] keys = set.toArray();
             for(int i = 0; i < keys.length; i++) {
                  OperOk subOk = (OperOk) map.get(keys[i]);
                  HashMap subMap = subOk.getSubOperations();
                  OperOk copyLsOk = (OperOk) subMap.get("srmls");
                  if(copyLsOk != null) {
                     srmLsVec.addElement(copyLsOk);
                  }
                  OperOk copyRmOk = (OperOk) subMap.get("srmrm");
                  if(copyRmOk != null) {
                     srmRmVec.addElement(copyRmOk);
                  }
              }
           }
         }
        }


        srmRmVec.addElement(isRmSuccess);
        srmLsVec.addElement(isLsSuccess);

        if(srmLsVec.size() == 0) {
             if(isBrowseGiven || isCopyGiven || isPutGiven) {
               util.printMessage("\tsrmLs : N.T.",logger,pIntf);
               util.writeSummary(localPublish,bs,bsa,"\tsrmLs : N.T.");
             }
        }
        int lsCount = 0;
        OperOk srmLsOk=null; 
        for(int m = 0; m < srmLsVec.size(); m++) {
           srmLsOk = (OperOk) srmLsVec.elementAt(m);
           if(srmLsOk.isTried()) {
              if(srmLsOk.isOperOk()) {
                 lsCount ++;
                 break;
              }
           }
        }
        if(isBrowseGiven || isCopyGiven || isPutGiven) {
        if(lsCount > 0) {
           util.printMessage("\tsrmLs : Ok",logger,pIntf);
           util.writeSummary(localPublish,bs,bsa,"\tsrmLs : Ok");
        }
        else {
           if(srmLsOk != null) {
             if(srmLsOk.isTried()) {
               if(srmLsOk.getExplanation().equals("")) {  
                 util.printMessage("\tsrmLs : Failed",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmLs : Failed");
               }
               else {
                 util.printMessage("\tsrmLs : "+srmLsOk.getExplanation(),logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,
						"\tsrmLs : "+srmLsOk.getExplanation());
               }
             }
             else {
                 util.printMessage("\tsrmLs : N.T.",logger,pIntf);
                 util.writeSummary(localPublish,bs,bsa,"\tsrmLs : N.T.");
             }
           }
        }
        }
        if(srmRmVec.size() == 0) {
           if(isRmGiven || isPutGiven || isCopyGiven) {
             util.printMessage("\tsrmRm : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmRm : N.T.");
           }
        }
        int rmCount = 0;
        OperOk srmRmOk=null; 
        for(int m = 0; m < srmRmVec.size(); m++) {
           srmRmOk = (OperOk) srmRmVec.elementAt(m);
           if(srmRmOk.isTried()) {
              if(srmRmOk.isOperOk()) {
                 rmCount ++;
                 break;
              }
           }
        }
        if(isRmGiven || isPutGiven || isCopyGiven) {
        if(rmCount > 0) {
           util.printMessage("\tsrmRm : Ok",logger,pIntf);
           util.writeSummary(localPublish,bs,bsa,"\tsrmRm : Ok");
        }
        else {
           if(srmRmOk != null) {
             if(srmRmOk.isTried()) {
               if(srmRmOk.getExplanation().equals("")) {  
                  util.printMessage("\tsrmRm : Failed",logger,pIntf);
                  util.writeSummary(localPublish,bs,bsa,"\tsrmRm : Failed");
               }
               else {
                  util.printMessage("\tsrmRm : "+srmRmOk.getExplanation(),logger,pIntf);
                  util.writeSummary(localPublish,bs,bsa,
						"\tsrmRm : "+srmRmOk.getExplanation());
               }
             }
             else {
                util.printMessage("\tsrmRm : N.T.",logger,pIntf);
                util.writeSummary(localPublish,bs,bsa,"\tsrmRm : N.T.");
             }
           }
        }
        }

        if(isCopyGiven) {
        if(isCopySuccess.isTried()) {
          if(isCopySuccess.getSubOperations().size() > 0) {
             HashMap map = isCopySuccess.getSubOperations();
             Set set = map.keySet();
             Object[] keys = set.toArray();
             //Object[] tempKeys = new Object[keys.length]; 
             Vector tempKeys = new Vector(); 
             Vector tempKeys2 = new Vector(); 
             for(int i = 0; i < keys.length; i++) {
                if(((String)keys[i]).startsWith("srmCopy")) {
                  tempKeys.addElement(keys[i]);
                }
             } 
             for(int i = 0; i < tempKeys.size(); i++) {
                String xxx = (String) tempKeys.elementAt(i); 
                int idx = xxx.indexOf("-"); 
                if(idx != -1) {
                  String temp = xxx.substring(idx+1);
                  for(int j = 0; j < keys.length; j++) {
                    if(keys[j].equals("srmStatusOfCopyRequest-"+temp)) { 
                       tempKeys2.addElement(xxx);         
                       tempKeys2.addElement(keys[j]);         
                    }
                  }
                }
             } 
             for(int i = 0; i < tempKeys2.size(); i++) {
                  String yyy = (String)tempKeys2.elementAt(i);
                  OperOk subOk = (OperOk) map.get(yyy);
                  int idx = ((String) yyy).indexOf(
						"srmStatusOfCopyRequest-push");
                  if(subOk.isOperOk()) {
                     //if(idx == -1) {
                        util.printMessage("\t"+yyy+":Ok",logger,pIntf);
                        util.writeSummary(localPublish,bs,bsa,"\t"+yyy+":Ok");
                     //}
                  }
                  else {
                    //if(idx == -1) {
                     if(subOk.getExplanation().equals("")) {
                        util.printMessage("\t"+yyy+":Failed", logger,pIntf);
                        util.writeSummary(localPublish,bs,bsa,"\t"+yyy+":Failed");
                     }
                     else {
                        util.printMessage("\t"+yyy+":"+
					       subOk.getExplanation(), logger,pIntf);
                        util.writeSummary(localPublish,bs,bsa,"\t"+yyy+":"+
					       subOk.getExplanation());
                     } 
                    //}
                  }
             }//end for
          }//end if
        }//end if
        else {
             util.printMessage("\tsrmCopy : N.T.",logger,pIntf);
             util.writeSummary(localPublish,bs,bsa,"\tsrmCopy : N.T.");
        }
       }
       //this part is ok, for the diag tool, only one site is used in a given time
       if(pIntf != null) {
         pIntf.setCompleted(true);
       }
       else {
         //System.out.println(">>>totalsites="+totalSites);
         //System.out.println(">>>currsite="+currSite);
         if(totalSites > 0 && currSite == (totalSites-1)) {
           System.out.println("Calling system.exit");
           System.exit(1);
         }
         else if(totalSites == 0) {
           System.exit(1);
         }
       }
   }//end else
  }//end (!onlyGSIFTPGet)
  else {
    startWebScript("GsiFTP");
    String temp = gsiftpCopySourceFile;
    String ttemp = util.getTargetFileName(localTarget);

    Date d = new Date ();
    String operatorTimeStamp = d.toString();
    try {
       gsiFTPSTimeStamp = util.getTimeStamp(); 

       gsiFTPETimeStamp = util.doOnlyGsiFTP(temp,ttemp,gsiFTPSTimeStamp,
			publishUrlStr, displayType, timeStamp, fullTimeStamp, startTimeStamp, 
		    operatorTimeStamp, servicePath, 
	        credential, proxyFile, noPublish,
		    onlyGsiFTPGet, defaultProxy, dcau, bufferSize, parallelism, 
            siteName, numOperations,typeString, dailyTest, 
			numSites,localPublish,bs,bsa,logger,pIntf,
			useGUC,gucScriptPath,tempScriptPath,null);
    }catch(Exception e) {
      d = new Date ();
      operatorTimeStamp = d.toString();
      util.printMessageHException("\n"+e.toString(),logger,pIntf);
      try {
       util.webScript(publishUrlStr,
   	 	  displayType,"GsiFTP","GsiFTP FAILED",
          util.gsiftpResultCreate(gsiFTPSTimeStamp,gsiFTPETimeStamp,0),
	      e.getMessage(),timeStamp,fullTimeStamp, startTimeStamp, 
          operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
		  "GsiFTP","GsiFTP","Failed",dailyTest, numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,
	      displayType,"RequestSummary","GsiFTP FAILED",
	   	  util.createAndConvertRequest(sourceFile,ttemp,2,0),
		  e.getMessage(),timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"GsiFTP","","Failed",dailyTest, numSites,
		  localPublish,bs,bsa);
      }catch(Exception  we) { we.printStackTrace(); }
    }
  }
  }catch(Exception e) {
    Date d =new Date();
    String operatorTimeStamp = d.toString();
    util.printMessageHException("\n"+e.toString(),logger,pIntf);
    util.printMessage("SRM-TESTER: Testing (1)" + 
		testType + " failed ", logger,null);
    e.printStackTrace();
    if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
    try {
       ReportRequestStatus rrs = new ReportRequestStatus();
       rrs.setLinkName("generalexp");
       util.webScript(publishUrlStr,displayType,"Exception","",rrs,
		"Notes from tester: " + e.getMessage(),
		timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
		siteName,numOperations,typeString,"","Exception",dailyTest, 
		numSites,localPublish,bs,bsa);
    }catch(Exception we) { 
        we.printStackTrace();
	}
   }
  }
  try {
    if(bs != null) {
      bs.close();
    }
    if(ostream != null) {
      ostream.close();
    }
  }catch(Exception e) {
    e.printStackTrace();
  }
}

 public static void printVersion () {
     String name = "SRM-Tester ";
       if(SRMTester.class.getPackage() != null) {
         name = name + " " + SRMTester.class.getPackage().getImplementationVersion();
       }
     String lbnl = "SRM-Client and BeStMan Copyright(c) 2007-2008, The Regents of the University" +
         " Of California, through Lawrence Berkeley National Laboratory. " + 
         "All rights reserved. ";
     String email = "Support at SRM@LBL.GOV";
     System.out.println(name);
     System.out.println(lbnl);
     System.out.println(email);
     System.exit(1);
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// showUsage
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void showUsage (boolean b) {
  if(b) { 
    System.exit(1);
  }

  System.out.println("Usage : \n" +
  			"\t-serviceurl  \twsdl server host (Required)\n" +
  			"\texample :     \tsrm://hostname:portname/wsdlServiceName\n" +
  			"\t-proxyfile    \tproxyfile location \n" + 
  			//"\t-output       \tlocation to output log file \n" +
			//"                \t(default is /tmp/srm-xxx-userlogin.log)\n" + 
  			"\t-localtargetdir    \t<targetUrl>\n" +
            "\t-drive        (drives the tester using the param file)\n" +
            "\t-concurrency  \t(runs tests on multiple SRMs)default:1\n" +
  			"\t-spacetoken   \t<valid space token>\n" +
  			"\t-authid  \t<valid authorization id>\n" +
  			//"\t-tokensize    \t<valid token size>\n" +
  			"\t-cleanupall  \tdefault:true\n" +
  			"\t-abortonfail   (abort is called when file transfer fails)\n" +
  			"\t-releasefile  \tdefault:true\n" +
  			"\t-remotesfn     \t<remote sfn> \n" +
  			"\t-copysource   \t<copy_sourceurl> (Required for -all option)\n" +
  			"\t-filestoragetype   \t<permanent|durable|volatile>" +
            "\t-gsiftpcopysource   \t<source_url> \n" +
  			"\t-localsource    \t<sourceUrl> (used for put option)\n" +
  			"\t-remotefilelifetime    \t<integer> (used for remote file life time for put operation)\n" +
  			"\t-desiredlifetime    \t<integer> (used for SRMBringonline operation)\n" +
  			//"\t-onlygsiftpget  <true|false> \n" +
  			//"\t               (default :false) \n" +
  			//"\t-publish  \tPublish to web site (default it does not publish)\n" +
            //"\t-publishurl  \tlocation to the web server cgi script\n" +
            "\t-gucscriptpath  \tlocation to the guc script path\n" +
            //"\t-tempscriptpath  \tlocation to the temp script path\n" +
            "\t-localpublish  \tlocation to the publish the test results\n" +
            "\t-browseafterput  \tbrowses the file after putting the file into SRM\n" +
            "\t-nooverwrite \tdefault:false\n" +
            "\t-nocopyoverwrite \tdefault:false\n" +
            "\t-direct \t<do direct gsiftp transfer>\n" +
            "\t-nodcau \t\n" +
            //"\t-dailytest \t (default:false)\n" +
            //"\t-numsites  \t (number of participating sites)\n" +
            "\t-buffersize \t<integer>default:1048576 bytes\n" +
            "\t-retrytimeallowed \t<integer>default:5 minutes\n" +
            "\t-statuswaittime \t<integer>default:30 seconds\n" +
            "\t-conf       \tproperties file path\n" +
            "\t-op           \t(ping,put,get,bringonline,push,pull,gsiftp,ls,mv,srmrm,mkdir,rmdir,reserve,getspacemeta,getspacetokens,release,gettransferprotocols)\n" +
            "\t-pushmode     \t(uses push mode to perform the copy operation)\n");

   if(pIntf == null) {
     System.exit(1);
   }
   else {
     pIntf.setCompleted(false);
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// connectToWSDL
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ISRM connectToSRM (String servicePath, StringBuffer proxyString)
	throws IOException, Exception
{
   if(directGsiFTP) {
     return null;
   }

   ISRM service = null;
   try {
      util.printMessageHL("\nConnecting to ServicePath ...\n",
		logger,pIntf);
      service = getRemoteWSDLObject (servicePath,proxyString);
   }catch(java.net.ConnectException ce) {
      util.printMessage("SRM-TESTER: Possible reason(s) ",logger,pIntf);
      util.printMessage("SRM-TESTER: 1) SRM server may not be up.",logger,pIntf);
      util.printMessage
		("SRM-TESTER: 2) Check the parameters WSG host port and path",logger,pIntf);  
      util.printMessage(" in the properties/srm.properties file.",logger,pIntf);
      throw ce;
   }catch(IOException ioe) {
      throw ioe;
   }catch(Exception e) {
      throw e;
   }
   return service;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// connectToSRM
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ISRM connectToSRM (StringBuffer proxyString, boolean forCopyCase,
   String localCopySourceFile) 
	throws IOException, Exception
{
   ISRM service = null;
   if(directGsiFTP) {
     return service;
   }

   try {
      if(changeToCopyServicePath && forCopyCase) {
        if(localCopySourceFile.startsWith("gsiftp")) {
          util.printMessage("For the push  mode copy source cannot be "+
				localCopySourceFile,logger,pIntf);
          throw new Exception("For the push  mode copy source cannot be "+
				localCopySourceFile);
        }
        else {
         MyGlobusURL curl = new MyGlobusURL(localCopySourceFile);
         String cProtocol = curl.getProtocol();
         String cHost = curl.getHost();
         int cPort = curl.getPort();
         String cPath = curl.getPath();
         int idx = cPath.indexOf("?SFN");
         if(idx != -1) {
           cPath = cPath.substring(0,idx);
         }
         copyServicePath= cProtocol+"://"+cHost+":"+cPort+cPath;
         util.printMessageHL("\nConnecting with copyServicePath ...\n",
				logger, pIntf);
   	     service = getRemoteWSDLObject (copyServicePath,proxyString);
        } 
      } 
      else {
        util.printMessageHL("\nSRM-TESTER: Connecting with regular servicepath\n", logger,pIntf);
   	    service = getRemoteWSDLObject (servicePath,proxyString);
      }
   }catch(java.net.ConnectException ce) {
      util.printMessage("SRM-TESTER: Possible reason(s) ",logger, pIntf);
      util.printMessage("SRM-TESTER: 1) SRM server may not be up.",logger, pIntf);
      util.printMessage
		("SRM-TESTER: 2) Check the parameters WSG host port and path",logger, pIntf);  
      util.printMessage(" in the properties/srm.properties file.",logger, pIntf);
      throw ce;
   }catch(IOException ioe) {
      throw ioe;
   }catch(Exception e) {
      throw e;
   }
   return service;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// startWebScript
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void startWebScript(String displayName) {

  util.printMessage("++++++++++++++++++++++++++++++++++++++++++++\n", logger,pIntf);
  util.printMessage("\t\t\t R E P O R T ", logger, pIntf);
  Date d = new Date();
  String operatorTimeStamp = d.toString();
  try {

    InetAddress localHost = InetAddress.getLocalHost ();
    String hostName = localHost.getHostName ();

    util.webScript(publishUrlStr,displayType,"START",
      "SUCCESS",null,hostName,timeStamp,fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish, 
	  siteName,numOperations,displayName,
	  "","Ok",dailyTest, numSites,localPublish,bs,bsa);

  }catch(UnknownHostException uhe) {
      uhe.printStackTrace();
  }catch(Exception e) { 
      e.printStackTrace();
  }
  util.printMessage("++++++++++++++++++++++++++++++++++++++++++++\n", logger,pIntf);
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGetSpaceTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doGetSpaceTokens(ISRM srm, 
		StringBuffer proxyString, String displayName,
	    String exceptionMessage) throws Exception {

  util.printMessageHL("\nSending GetSpaceTokens request ...\n",logger, pIntf);
  util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);
  Date d = new Date();
  String operatorTimeStamp = d.toString();

  if(srm == null) {
     String displayStatus="Exception";
     if(exceptionMessage.equals("")) {
       exceptionMessage="Possibly SRM is down";
       displayStatus="Failed";
     }
     try {
       util.webScript(publishUrlStr,displayType,
	    "GetSpaceTokens",displayStatus,null,exceptionMessage,
		   timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		   servicePath,noPublish,
		   siteName,numOperations,displayName,"srmGetSpaceTokens",
	       displayStatus,dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		   servicePath,
		noPublish, siteName,numOperations,displayName,"",displayStatus,
		dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     return;
  }

  isGetSpaceTokensSuccess.setTried(true);
  SrmGetSpaceTokensRequest request = new SrmGetSpaceTokensRequest();
  if(!uid.equals("")) {
    request.setAuthorizationID(uid);
  }

  String statusMsg = "";
  String displayStatusMsg = "Ok";
  StringBuffer explanationBuffer = new StringBuffer();
  StringBuffer statusBuffer = new StringBuffer();
  StringBuffer tokenArray = new StringBuffer();
  StringBuffer availP = new StringBuffer();
  try {
    SrmGetSpaceTokensResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    response = srm.srmGetSpaceTokens(request); 

    timeOutCallBack.setObject(response);

    if(response != null) {
        TReturnStatus rStatus = response.getReturnStatus();
        if(rStatus != null) {
          TStatusCode status = rStatus.getStatusCode();
          if(status != null) {
             statusBuffer.append(status.toString());
             util.printMessage("STATUS="+status,logger,pIntf);
             if(status == TStatusCode.SRM_SUCCESS || 
					status == TStatusCode.SRM_DONE ||
					status == TStatusCode.SRM_INVALID_REQUEST ||
					status == TStatusCode.SRM_NOT_SUPPORTED) {
                 statusMsg = status.toString();
                 if(status == TStatusCode.SRM_NOT_SUPPORTED) {
                   displayStatusMsg = "N.S.";
                 }
             }
             else { 
                 statusMsg=status.toString();
                 displayStatusMsg = "Failed";
                 if(rStatus.getExplanation() != null) {
                   explanationBuffer.append(rStatus.getExplanation());
                   util.printMessage(explanationBuffer.toString(),logger,pIntf);
                 }
             }
             if(displayStatusMsg.equals("Ok")) {    
              isGetSpaceTokensSuccess.isOperOk(true);
             }
             else {
              isGetSpaceTokensSuccess.setExplanation(displayStatusMsg);
             }
             ArrayOfString protocol = response.getArrayOfSpaceTokens();
             if(protocol != null) {
               String[] availProtocols = protocol.getStringArray();
               if(availProtocols != null) {
                 util.printMessage("spacetoken.size="+availProtocols.length,
					logger,pIntf);
               }
               if(availProtocols != null) {
                  for(int i = 0; i < availProtocols.length; i++) {
                     if(i == 0) {
                        //availP.append("("+i+")space.token="+
							//availProtocols[i]);
                     }
                     else {
                        //availP.append(",("+i+")space.token="+
							//availProtocols[i]);
                     }
                     util.printMessage("("+i+")space.token="+availProtocols[i], logger,pIntf);
                  }
               }
             } 
           }
         }
         else {
            statusMsg=
			  "Null ReturnStatus from getSpaceTokens from server";
            displayStatusMsg = "Failed";
            explanationBuffer.append
		   	  ("Notes from tester : Null ReturnStatus for getSpaceTokens from server.");
            util.printMessage(explanationBuffer.toString(),logger,pIntf);
         }
    }     
    else {
      statusMsg= "Null response for getSpaceTokens from server";
      displayStatusMsg = "Failed";
      explanationBuffer.append("Tester:Explanation: Null response for getSpaceTokens from server.");
      util.printMessage(explanationBuffer.toString(),logger,pIntf);
    }
  }catch(Exception e) {
    displayStatusMsg = "Exception";
    isGetSpaceTokensSuccess.setExplanation("Exception");
    statusMsg=e.getMessage();
	e.printStackTrace();
  }

  d = new Date ();
  operatorTimeStamp = d.toString();
  try {
    //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
	PingReportStatus rrs = new PingReportStatus ();
    createPingReportStatus(rrs,uid,"","",null);
    rrs.setActualResult(statusBuffer.toString());
    rrs.setMatchingInfo(statusMsg);
    rrs.setLinkName("getspacetokens_0");
    util.webScript(publishUrlStr,
      displayType,"SendingGetSpaceTokens","GetSpaceTokens", rrs,
      "", 
	  timeStamp,fullTimeStamp,  startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceTokens","","Sending", 
	  dailyTest, numSites,localPublish,bs,bsa);
    createPingReportStatus(rrs,uid,availP.toString(),"SpaceTokens",null);
    util.webScript(publishUrlStr,displayType,"GetSpaceTokens",statusMsg,
      rrs, explanationBuffer.toString(),
      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceTokens","srmGetSpaceTokens",
	  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
   }catch(Exception e) {e.printStackTrace();}
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGetRequestTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doGetRequestTokens(ISRM srm, 
		StringBuffer proxyString, 
		String displayName, String exceptionMessage) throws Exception {

  util.printMessageHL("\nSending GetRequestTokens request ...\n",logger, pIntf);
  util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);

  isGetRequestTokensSuccess.setTried(true);
  Date d = new Date();
  String operatorTimeStamp = d.toString();
  if(srm == null) {
     String displayStatus="Exception";
     if(exceptionMessage.equals("")) {
        exceptionMessage = "Possibly SRM is down";
        displayStatus="Failed";
     }
     try {
       util.webScript(publishUrlStr,displayType,
	    "GetRequestTokens",displayStatus,null,exceptionMessage,
		   timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		   servicePath,noPublish,
		   siteName,numOperations,displayName,"srmGetRequestTokens",
		   displayStatus,dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		servicePath,
	    noPublish, siteName,numOperations,displayName,"","Failed",
		dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     return;
  }

  SrmGetRequestTokensRequest request = new SrmGetRequestTokensRequest();
  if(!uid.equals("")) {
    request.setAuthorizationID(uid);
  }

  String statusMsg = "";
  String displayStatusMsg = "Ok";
  StringBuffer explanationBuffer = new StringBuffer();
  StringBuffer statusBuffer = new StringBuffer();
  StringBuffer tokenArray = new StringBuffer();
  StringBuffer availP = new StringBuffer();
  try {
    SrmGetRequestTokensResponse response = null;

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    response = srm.srmGetRequestTokens(request); 

    timeOutCallBack.setObject(response);

    if(response != null) {
        TReturnStatus rStatus = response.getReturnStatus();
        if(rStatus != null) {
          TStatusCode status = rStatus.getStatusCode();
          if(status != null) {
             statusBuffer.append(status.toString());
             if(status == TStatusCode.SRM_SUCCESS || 
				status == TStatusCode.SRM_DONE || 
		        status == TStatusCode.SRM_NOT_SUPPORTED) {
                 statusMsg = "Yes";
                 if ( status == TStatusCode.SRM_NOT_SUPPORTED) {
                   displayStatusMsg = "N.S.";
                 }
             }
             else {
                 statusMsg=status.toString();
                 displayStatusMsg = "Failed";
                 if(rStatus.getExplanation() != null) { 
                   explanationBuffer.append (rStatus.getExplanation());
                   util.printMessage("Explanation="+rStatus.getExplanation(),
				  	   logger,pIntf);
                 }
             }
             if(displayStatusMsg.equals("Ok")) {
               isGetRequestTokensSuccess.isOperOk(true);
             }
             else {
               isGetRequestTokensSuccess.setExplanation(displayStatusMsg);
             }
             ArrayOfTRequestTokenReturn protocol = 
				response.getArrayOfRequestTokens();
             if(protocol != null) {
               TRequestTokenReturn[] availProtocols = protocol.getTokenArray();
               util.printMessage("RequestToken.size="+availProtocols.length,
					logger,pIntf);
               /*
               if(availProtocols != null) {
                  for(int i = 0; i < availProtocols.length; i++) {
                     Calendar createdTime =  availProtocols[i].getCreatedAtTime();
                     int month = createdTime.get(Calendar.MONTH);
                     int day   = createdTime.get(Calendar.DAY_OF_MONTH);
                     int year  = createdTime.get(Calendar.YEAR);
                     int hour  = createdTime.get(Calendar.HOUR_OF_DAY);
                     int minute = createdTime.get(Calendar.MINUTE);
                     int seconds = createdTime.get(Calendar.SECOND);
                     StringBuffer buf = new StringBuffer();
                     buf.append ("Month="+month+",Day="+day+",Year="+year+
							",Hour="+hour+",Minute="+minute+",Second="+seconds);
                     if(i == 0) {
                       availP.append ("("+i+")request.token="+
							availProtocols[i].getRequestToken());
                     }
                     else {
                        availP.append
			              (",("+i+")request.token="+
							availProtocols[i].getRequestToken());
                     }
                     availP.append(",created.time="+buf.toString());
                     util.printMessage("("+i+")request.token="+availProtocols[i].getRequestToken(),
							logger,pIntf);
                     util.printMessage("("+i+")created.time="+	buf.toString(), logger,pIntf);
                  }
               }
               */
             } 
           }
          }
         else {
            statusMsg="Null Returnstatus for getRequestTokens from server";
            displayStatusMsg = "Failed";
            explanationBuffer.append
		   	  ("Notes from tester : Null ReturnStatus for getRequestTokens from server.");
            util.printMessage(explanationBuffer.toString(),logger,pIntf);
         }
    }     
    else {
      statusMsg="Null response for getRequestTokens from server";
      displayStatusMsg = "Failed";
      explanationBuffer.append
		("Notes from tester : Null response for getRequestTokens from server.");
      util.printMessage(explanationBuffer.toString(),logger,pIntf);
    }
  }catch(Exception e) {
    isGetRequestTokensSuccess.setExplanation("Exception");
    displayStatusMsg = "Exception";
    statusMsg=e.getMessage();
    e.printStackTrace();
  }

  try {
    //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
	PingReportStatus rrs = new PingReportStatus ();
    createPingReportStatus(rrs,uid,"","",null);
    rrs.setActualResult(statusBuffer.toString());
    rrs.setMatchingInfo(statusMsg);
    rrs.setLinkName("getrequesttokens_0");
    util.webScript(publishUrlStr,
      displayType,"SendingGetRequestTokens","GetRequestTokens", rrs,
      "", timeStamp,fullTimeStamp, startTimeStamp,  operatorTimeStamp,servicePath,noPublish,
	  siteName,numOperations,"srmGetRequestTokens","","Sending", 
	  dailyTest, numSites,localPublish,bs,bsa);
    createPingReportStatus(rrs,uid,availP.toString(),"RequestTokens",null);
    util.webScript(publishUrlStr,displayType,"GetRequestTokens",statusMsg,
      rrs, explanationBuffer.toString(),
      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmGetRequestTokens","srmGetRequestTokens",
	  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
   }catch(Exception e) {e.printStackTrace();}
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGetTransferProtocols
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doGetTransferProtocols(ISRM srm, 
		StringBuffer proxyString, 
		String displayName, String exceptionMessage) throws Exception {

  isGetTransferProtocolsSuccess.setTried(true);
  Date d = new Date();

  util.printMessageHL("\nSending GetTransferProtocols request ...\n",logger, pIntf);
  util.printMessageHL2("StartTime="+ d.toString() +"\n",logger,pIntf);

  String operatorTimeStamp = d.toString();

  if(srm == null) {
     String displayStatus="Exception";
     if(exceptionMessage.equals("")) {
        exceptionMessage="Possibly SRM is down";
        displayStatus="Failed";
     }
     try {
       util.webScript(publishUrlStr,displayType,
	    "GetTransferProtocols",displayStatus,null,exceptionMessage,
		   timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		   servicePath,noPublish,
		   siteName,numOperations,displayName,"srmGetTransferProtocols",
		   displayStatus,dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
		servicePath,
		noPublish, siteName,numOperations,displayName,"",displayStatus,
	    dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     return;
  }

  SrmGetTransferProtocolsRequest request = new SrmGetTransferProtocolsRequest();
  if(!uid.equals("")) {
    request.setAuthorizationID(uid);
  }

  String statusMsg = "";
  String tempStatusMsg="";
  String displayStatusMsg = "Ok";
  StringBuffer explanationBuffer = new StringBuffer();
  StringBuffer statusBuffer = new StringBuffer();
  StringBuffer availP = new StringBuffer();
  TExtraInfo[] info = null;
  try {
    SrmGetTransferProtocolsResponse response = null; 

    int ssTimeStamp = util.startTimeStamp();
    timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
    timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
    timeOutCallBack.start();

    response = srm.srmGetTransferProtocols(request); 

    timeOutCallBack.setObject(response);

    if(response != null) {
        TReturnStatus rStatus = response.getReturnStatus();
        if(rStatus != null) {
          TStatusCode status = rStatus.getStatusCode();
          if(status != null) {
             statusBuffer.append(status.toString());
             if(status == TStatusCode.SRM_SUCCESS || 
				status == TStatusCode.SRM_DONE ||
				status == TStatusCode.SRM_NOT_SUPPORTED) 
	         {
                 statusMsg = "Yes";
                 tempStatusMsg=status.toString();
                 if(status == TStatusCode.SRM_NOT_SUPPORTED) {
                   displayStatusMsg = "N.S.";
                 }
                 util.printMessage("GetTransferProtocols.Status="+status,logger,pIntf);
             }
             else {
                statusMsg=status.toString();
                displayStatusMsg = "Failed";
                if(rStatus.getExplanation() != null) {
                  explanationBuffer.append (rStatus.getExplanation());
                }
                util.printMessage(explanationBuffer.toString(),logger,pIntf);
             }
             if(displayStatusMsg.equals("Ok")) {
               isGetTransferProtocolsSuccess.isOperOk(true);
             }
             else {
               isGetTransferProtocolsSuccess.setExplanation(displayStatusMsg); 
             }
             ArrayOfTSupportedTransferProtocol protocol = response.getProtocolInfo();
             if(protocol != null) {
               TSupportedTransferProtocol[] availProtocols = protocol.getProtocolArray();
               if(availProtocols != null) {
                  for(int i = 0; i < availProtocols.length; i++) {
                      if(i == 0) {
                       availP.append
					 	 ("("+i+")"+availProtocols[i].getTransferProtocol());
                      }
                      else {
                       availP.append
					 	 (",("+i+")"+availProtocols[i].getTransferProtocol());
                      } 
                      util.printMessage
				        ("("+i+")"+availProtocols[i].getTransferProtocol(),
							logger,pIntf);
                      ArrayOfTExtraInfo aExtraInfo = 
						availProtocols[i].getAttributes();
                      if(aExtraInfo != null) {
                         info = aExtraInfo.getExtraInfoArray();
                         if(info != null && info.length > 0)  {
                           util.printMessage("\nExtra information", 
								logger, pIntf);
                           for(int j = 0; j < info.length; j++) {
                            util.printMessage("\tKey="+info[j].getKey(),
						        logger, pIntf);  
                            util.printMessage("\tValue="+info[j].getValue(),
						        logger, pIntf);  
                           }
                      }
                      else {
                       util.printMessage
						 ("\nPing did not return any extra information", 
							logger, pIntf);
                      }
                    }
                  }
               }
             }
          }
          else {
            statusMsg="Null StatusCode for getTransferProtocols from server";
            displayStatusMsg = "Failed";
            tempStatusMsg="Failed";
            explanationBuffer.append
		   	  ("Notes from tester : Null StatusCode for getTransferProtocols from server.");
            util.printMessage(explanationBuffer.toString(),logger,pIntf);
          }
        }
        else {
         statusMsg="Null ReturnStatus for getTransferProtocols from server";
         displayStatusMsg = "Failed";
         tempStatusMsg="Failed";
         explanationBuffer.append
			("Notes from tester : Null ReturnStatus for getTransferProtocols from server.");
         util.printMessage(explanationBuffer.toString(),logger,pIntf);
        }
    }     
    else {
      statusMsg="Null response for getTransferProtocols from server";
      displayStatusMsg = "Failed";
      tempStatusMsg="Failed";
      explanationBuffer.append
		("Notes from tester : Null response for getTransferProrocols from server.");
      util.printMessage(explanationBuffer.toString(),logger,pIntf);
    }
  }catch(Exception e) {
    statusMsg=e.toString();
    displayStatusMsg = "Exception";
    tempStatusMsg="Exception";
    isGetTransferProtocolsSuccess.setExplanation("Exception");
	e.printStackTrace();
  }

  try {
    //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
	PingReportStatus rrs = new PingReportStatus ();
    createPingReportStatus(rrs,uid,"","",null);
    rrs.setActualResult(statusBuffer.toString());
    rrs.setMatchingInfo(statusMsg);
    rrs.setLinkName("gettransferprotocols_0");
    util.webScript(publishUrlStr,
      displayType,"SendingGetTransferProtocols","GetTransferProtocols", rrs,
      "", timeStamp,fullTimeStamp,  startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"srmGetTransferProtocols","",
	  "Sending", dailyTest, numSites,localPublish,bs,bsa);
    createPingReportStatus(rrs,uid,availP.toString(),"ProtocolInfo",info);
    util.webScript(publishUrlStr,displayType,"GetTransferProtocols",tempStatusMsg,
      rrs, explanationBuffer.toString(),
      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,servicePath,noPublish,
	  siteName,numOperations,"srmGetTransferProtocols",
	  "srmGetTransferProtocols", displayStatusMsg,dailyTest, 
	   numSites,localPublish,bs,bsa);
   }catch(Exception e) {e.printStackTrace();}
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doPing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doPing(ISRM srm, StringBuffer proxyString, String displayName) 
	throws Exception {

  isPingOverAllSuccess.setTried(true);
  Date d = new Date();

  util.printMessageHL("\nSRM-TESTER: " +
	d.toString() + " Calling Ping request ...\n",logger, pIntf);
  //util.printMessageHL2("StartTime="+d.toString() +"\n",logger,pIntf);

  String operatorTimeStamp = d.toString();

  String tempDName = "srmPing";
  if(!displayName.equalsIgnoreCase("SrmPing")) { 
    //tempDName=tempDName+"-"+displayName;
    //tempDName = displayName;
  }

  if(srm == null) {
     try {
       util.webScript(publishUrlStr,displayType,
	    "PING","FAILED",null,"Possibly SRM is down.",
		   timeStamp,fullTimeStamp, startTimeStamp, 
		   operatorTimeStamp, servicePath,noPublish,
		   siteName,numOperations,displayName,tempDName,"Failed",dailyTest, 
		   numSites,localPublish,bs,bsa);
        isPingOverAllSuccess.isOperOkDummy(false);
     }catch(Exception e) { e.printStackTrace(); }
     try {
       util.webScript(publishUrlStr,
        displayType,"BLANK","",
        null, "", timeStamp, fullTimeStamp, startTimeStamp, 
	    operatorTimeStamp, servicePath,
		noPublish, siteName,numOperations,displayName,"","Failed",
		dailyTest, numSites,localPublish,bs,bsa);
        isPingOverAllSuccess.isOperOkDummy(false);
     }catch(Exception e) { e.printStackTrace(); }
     return;
  }

  SrmPingRequest request = new SrmPingRequest();
  if(!uid.equals("")) {
    request.setAuthorizationID(uid);
  }

  SrmPingResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();
 
  response = srm.srmPing(request); 

  timeOutCallBack.setObject(response);

  util.printMessageHL("\nSRM-TESTER: ... Output from SRM... " +
		new Date(), logger, pIntf);
  util.printMessage("===================================",logger,pIntf);
  if(response != null) {
    String version = response.getVersionInfo();
    util.printMessageHL("\nPing versionInfo=" + version+"\n",
			logger, pIntf);
    //util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);
    ArrayOfTExtraInfo aExtraInfo = response.getOtherInfo();
    TExtraInfo[] info = null;
    if(aExtraInfo != null) {
      info = aExtraInfo.getExtraInfoArray();
      if(info != null && info.length > 0)  {
        util.printMessage("\nExtra information", logger, pIntf);
        for(int i = 0; i < info.length; i++) {
         util.printMessage("\tKey="+info[i].getKey(),logger, pIntf);  
         util.printMessage("\tValue="+info[i].getValue(),logger, pIntf);  
        }
      }
      else {
        util.printMessage
		("\nSRM-TESTER: Ping did not return any extra information", 
			logger, pIntf);
      }
    }
    try {
	  PingReportStatus ps = new PingReportStatus ();
      createPingReportStatus(ps,uid,"","",null);
      ps.setLinkName("ping");
      ps.setActualResult("SUCCESS");
      ps.setMatchingInfo("Yes");
      util.webScript(publishUrlStr,
	    displayType,"SendingPING","SrmPing",
		ps, "", timeStamp, fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,
	    noPublish, siteName,numOperations,displayName,"","Ok",dailyTest, 
	    numSites,localPublish,bs,bsa);
	  createPingReportStatus(ps,uid,version,"VersionInfo",info);
      util.webScript(publishUrlStr,displayType,
	    "Ping","SUCCESS",ps,"",
	    timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
	    siteName,numOperations,displayName,tempDName,
		"Ok",dailyTest, numSites,localPublish,bs,bsa);
        isPingOverAllSuccess.isOperOk(true);
    }catch(Exception e) { e.printStackTrace(); }
    return;
  }    
  else {
     util.printMessage("\nNull Response for ping from server: " + 
			response,logger, pIntf);
     isPingOverAllSuccess.isOperOkDummy(false);
     try {
	   PingReportStatus ps = new PingReportStatus ();
       createPingReportStatus(ps,uid,"","",null);
       ps.setLinkName("ping");
       ps.setActualResult("Failed");
       ps.setMatchingInfo("No. expecting SRM_SUCCESS");
       util.webScript(publishUrlStr,
	    displayType,"SendingPING","SrmPing",
		ps, "", timeStamp, fullTimeStamp, startTimeStamp, 
	    operatorTimeStamp, servicePath,
	    noPublish, siteName,numOperations,displayName, "","Failed", 
		dailyTest, numSites,localPublish,bs,bsa);
       util.webScript(publishUrlStr,displayType,
	     "Ping","Null response from SRM",null,"Null response",
		  timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
	      siteName, numOperations,displayName,tempDName, 
		  "Failed", 
		  dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { e.printStackTrace(); }
     return;
  }
}

public void processGridFTPWaitTimeOut(Hashtable cmd) {
}

public void processThreadRequest_3(String[] cmd, SharedObjectLock sLock) {
}

public void processThreadRequest_1(String[] cmd) {
}

public void processThreadRequest_2(String type, Hashtable cmd) {
 try {
   if(type.equalsIgnoreCase("ping")) {
     isPingSuccess.isOperOk(true);
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     String displayName = "";

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("displayname")) {
        displayName = (String)me.getValue();
      }
     }
   try {
     doPing(srm,proxyString,displayName);
   }catch(Exception e) {
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     String tempDName="srmPing";
     isPingSuccess.isOperOk(false);
     isPingOverAllSuccess.isOperOkDummy(false);
     isPingOverAllSuccess.setTried(true);
     isPingOverAllSuccess.isOperOk(false);
     if(!displayName.equalsIgnoreCase("srmPing")) {
        //tempDName=tempDName+"-"+displayName;
     }
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
      //ReportRequestStatus rrs = new ReportRequestStatus();
      //rrs.setLinkName("pingexception");
	  PingReportStatus ps = new PingReportStatus ();
      createPingReportStatus(ps,uid,"","",null);
      ps.setLinkName("ping");
      ps.setActualResult("Exception");
      ps.setMatchingInfo("No. expecting SRM_SUCCESS");
      try {
        util.webScript(publishUrlStr,
	    displayType,"SendingPING","SrmPing",
		ps, "", timeStamp, fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,
		noPublish, siteName,numOperations,displayName, "","Sending", 
		dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,displayType,
	    "Ping","Exception",ps,e.getMessage(),
	    timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
	    siteName, numOperations,displayName, tempDName,"Exception", 
		dailyTest, numSites,localPublish,bs,bsa);
      }catch(Exception we) { we.printStackTrace(); }
      isPingOverAllSuccess.setExplanation("Exception " + e.getMessage());
     }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("getspacetokens")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     String displayName = "";
     String exceptionMessage="";

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("displayname")) {
        displayName = (String)me.getValue();
      }
      else if(key.equalsIgnoreCase("exceptionmessage")) {
        exceptionMessage = (String)me.getValue();
      }
     }
   try {
     isGetSpaceTokensSuccess.setTried(true);
     doGetSpaceTokens(srm,proxyString,displayName,exceptionMessage);
   }catch(Exception e) {
     isGetSpaceTokensSuccess.setTried(true);
     Date d = new Date ();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
      isGetSpaceTokensSuccess.setExplanation("Exception");
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
      rrs.setActualResult("Exception");
      rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      rrs.setLinkName("getspacetokens_0");
      try { 
        util.webScript(publishUrlStr,
          displayType,"SendingGetSpaceTokens","GetSpaceTokens", rrs,
          "", timeStamp,fullTimeStamp,startTimeStamp,   
		operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmGetSpaceTokens","","Sending", 
		  dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,displayType,"GetSpaceTokens","Exception",
          rrs, "Notes from tester : " + e.getMessage(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmGetSpaceTokens","srmGetSpaceTokens",
		  "Exception",dailyTest, numSites,localPublish,bs,bsa);
      }catch(Exception we) { we.printStackTrace(); }
     }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("getrequesttokens")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     String displayName = "";
     String exceptionMessage="";

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("displayname")) {
        displayName = (String)me.getValue();
      }
      else if(key.equalsIgnoreCase("exceptionmessage")) {
        exceptionMessage = (String)me.getValue();
      }
     }
   try {
     isGetRequestTokensSuccess.setTried(true);
     doGetRequestTokens(srm,proxyString,displayName,exceptionMessage);
   }catch(Exception e) {
     isGetRequestTokensSuccess.setTried(true);
     Date d = new Date ();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
      //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
      isGetRequestTokensSuccess.setExplanation("Exception");
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
      rrs.setActualResult("Exception");
      rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      rrs.setLinkName("getrequesttokens_0");
      try { 
        util.webScript(publishUrlStr,
          displayType,"SendingGetRequestTokens","GetRequestTokens", rrs,
          "", timeStamp,fullTimeStamp,  startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmGetRequestTokens","",
		  "Sending", dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,displayType,"GetRequestTokens","Exception",
          rrs, "Notes from tester : "  + e.getMessage(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmGetRequestTokens","srmGetRequestTokens",
	      "Exception",dailyTest, numSites,localPublish,bs,bsa);
      }catch(Exception we) { we.printStackTrace(); }
     }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("gettransferprotocols")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     String displayName = "";
     String exceptionMessage="";

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("displayname")) {
        displayName = (String)me.getValue();
      }
      else if(key.equalsIgnoreCase("exceptionmessage")) {
        exceptionMessage = (String)me.getValue();
      }
     }
   try {
     doGetTransferProtocols(srm,proxyString,displayName,exceptionMessage);
     if(sLock != null) sLock.setIncrementCount();
   }catch(Exception e) {
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("SRM-TESTER: Exception : " + e.getMessage() +"\n",logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
      //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
      rrs.setActualResult("Exception");
      rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
      rrs.setLinkName("gettransferprotocols_0");
      try { 
        util.webScript(publishUrlStr,
          displayType,"SendingGetTransferProtocols","GetTransferProtocols", rrs,
          "", timeStamp,fullTimeStamp,  startTimeStamp, operatorTimeStamp,
	      servicePath,noPublish,
	      siteName,numOperations,"srmGetTransferProtocols","",
	      "Sending", dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,displayType,"GetTransferProtocols",
		  "Exception", rrs, "Notes from tester : " + e.getMessage(),
          timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		  servicePath,noPublish,
	      siteName,numOperations,"srmGetTransferProtocols",
	      "srmGetTransferProtocols", "Exception",dailyTest, numSites,
		  localPublish,bs,bsa);
      }catch(Exception we) { we.printStackTrace(); }
     }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("putdone")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     String rToken= "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("requesttoken")) {
        rToken = (String)me.getValue(); 
        if(rToken.equals("")) { 
         rToken=null;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
   }
   StringBuffer statusBuffer = new StringBuffer();
   StringBuffer explanationBuffer = new StringBuffer();
   String statusMsg="";
   try {
     SRMPutTest putTest = new SRMPutTest(publishUrlStr, displayType, 
          testType, timeStamp, fullTimeStamp, servicePath, credential, 
		  proxyFile, onlyGsiFTPGet, noPublish, detailed, false, defaultProxy, 
	      advisoryDelete, uid, dcau, bufferSize, parallelism,
		  siteName, numOperations, startTimeStamp, 
		  dailyTest, numSites, retryTimeAllowed, statusWaitTime,
		  localPublish,bs,bsa, logger,remoteFileLifeTime,pIntf,
		  useGUC,gucScriptPath,tempScriptPath, useDriverOn, isPingOverAllSuccess);

     if(rToken != null && !rToken.equals("")) {
       statusMsg = putTest.doPutDone(srm, source, rToken, uid, statusBuffer,
            explanationBuffer, detailed, 1);
     }
     else {
        statusMsg="Notes from tester: Please provide a valid request token";
        explanationBuffer.append("Notes from tester : Please  provide a valid request token.");
        statusBuffer.append("Exception");
        util.printMessage("Request Token="+rToken,logger,pIntf);
        util.printMessage(explanationBuffer.toString(),logger,pIntf);
     }
   }catch(Exception e) {
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
   }
   try {
    Date d = new Date();
    String operatorTimeStamp = d.toString();
    String displayStatusMsg = "Ok";
    if(statusBuffer.toString().equals("SRM_SUCCESS") || 
			statusBuffer.toString().equals("SRM_DONE") ||
			statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
        displayStatusMsg="Ok";
        if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) { 
           displayStatusMsg="N.S.";
        }
    } 
    else {
        displayStatusMsg="Failed";
    }
    ReportRequestStatus rrs = util.createReportRequestStatus(sourceFile,"","");
    rrs.setRequestId(rToken);
    rrs.setActualResult(statusBuffer.toString());
    rrs.setUserId(uid);
    rrs.setMatchingInfo(statusMsg);
    rrs.setLinkName("putdone_1");
    util.webScript(publishUrlStr,
      displayType,"SendingPUTDone","PutDone", rrs,
      "", timeStamp,fullTimeStamp,  startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"PutDone","","Sending", dailyTest, numSites,
	  localPublish,bs,bsa);
    util.webScript(publishUrlStr,displayType,"PutDone",statusMsg,
      rrs, explanationBuffer.toString(),
      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"PutDone","srmPutDone",displayStatusMsg,dailyTest, 
	  numSites,localPublish,bs,bsa);
   }catch(Exception e) {e.printStackTrace();}
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("abortfiles")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     String rToken= "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("requesttoken")) {
        rToken = (String)me.getValue(); 
        if(rToken.equals("")) { 
         rToken=null;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
   }
   StringBuffer statusBuffer = new StringBuffer();
   StringBuffer explanationBuffer = new StringBuffer();
   String statusMsg="";
   String displayStatusMsg="Ok";
   try {
     SRMPutTest putTest = new SRMPutTest(publishUrlStr, displayType, 
          testType, timeStamp, fullTimeStamp, servicePath, credential, 
		  proxyFile, onlyGsiFTPGet, noPublish, detailed, false, defaultProxy, 
	      advisoryDelete, uid, dcau, bufferSize, parallelism,
		  siteName, numOperations, startTimeStamp, 
		  dailyTest, numSites, retryTimeAllowed, statusWaitTime,
		  localPublish,bs,bsa, logger,remoteFileLifeTime,pIntf,useGUC,
		  gucScriptPath, tempScriptPath, useDriverOn, isPingOverAllSuccess);

     if(rToken != null && !rToken.equals("")) {
       statusMsg = putTest.doAbortFiles(srm, source, rToken, explanationBuffer,
			statusBuffer, uid, detailed, 0);
     }
     else {
        statusMsg="Notes from tester : Please provide a valid request token";
        explanationBuffer.append("Notes from tester : Please  provide a valid request token.");
        statusBuffer.append("Exception");
        util.printMessage("Request Token="+rToken,logger,pIntf);
        util.printMessage(explanationBuffer.toString(),logger,pIntf);
     }
   }catch(Exception e) {
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
   }
   try {
     Date d = new Date();
     String operatorTimeStamp = d.toString();
    if(statusBuffer.toString().equals("SRM_SUCCESS") || 
			statusBuffer.toString().equals("SRM_DONE") ||
			statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) {
        displayStatusMsg="Ok";
        if(statusBuffer.toString().equals("SRM_NOT_SUPPORTED")) { 
           displayStatusMsg="N.S.";
        }
    } 
    else {
        displayStatusMsg="Failed";
    }
    ReportRequestStatus rrs = util.createReportRequestStatus(sourceFile,"","");
    rrs.setRequestId(rToken);
    rrs.setActualResult(statusBuffer.toString());
    rrs.setUserId(uid);
    rrs.setMatchingInfo(statusMsg);
    rrs.setLinkName("abortfiles_0");
    util.webScript(publishUrlStr,
      displayType,"SendingAbortFiles","AbortFiles", rrs,
      "", timeStamp,fullTimeStamp,  startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"AbortFiles","","Sending", dailyTest, numSites,
	  localPublish,bs,bsa);
    util.webScript(publishUrlStr,displayType,"AbortFiles",statusMsg,
      rrs, explanationBuffer.toString(),
      timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"AbortFiles","srmAbortFiles",
	  displayStatusMsg,dailyTest, numSites,localPublish,bs,bsa);
   }catch(Exception e) {e.printStackTrace();}
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("put") || type.equals("reserveput")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     String remoteTarget = "";
     String sToken= "";
     boolean plainPut=false;
     boolean reservedSpace=false;
     boolean overwrite =false;
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     TFileStorageType fStorageType=null;

    Set set = cmd.entrySet();
    Iterator itr = set.iterator ();
    while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("remote")) {
        remoteTarget = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("plainput")) {
        Boolean b = (Boolean)me.getValue(); 
        plainPut = b.booleanValue();
      }
      else if(key.equalsIgnoreCase("overwrite")) {
        Boolean tt = (Boolean)me.getValue(); 
        overwrite = tt.booleanValue();
      }
      else if(key.equalsIgnoreCase("spacetoken")) {
        sToken = (String)me.getValue(); 
        if(sToken.equals("")) { 
         sToken=null;
        }
      }
      else if(key.equalsIgnoreCase("reservedspace")) {
        Boolean b = (Boolean)me.getValue(); 
        reservedSpace = b.booleanValue();
        if(reserveSpace) {
          reservedSpace=true;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
      else if(key.equalsIgnoreCase("filestoragetype")) {
        fStorageType = (TFileStorageType)me.getValue();
      }
     }
   try {
     SRMPutTest putTest = new SRMPutTest(publishUrlStr, displayType, 
          testType, timeStamp, fullTimeStamp, servicePath, 
	      credential, proxyFile,
          onlyGsiFTPGet, noPublish, detailed, false, defaultProxy, 
	      advisoryDelete, uid, dcau, bufferSize, parallelism,
	      siteName, numOperations, startTimeStamp, dailyTest, 
	      numSites, retryTimeAllowed, statusWaitTime,
		  localPublish,bs,bsa,logger,remoteFileLifeTime,pIntf,useGUC,
		  gucScriptPath, tempScriptPath,useDriverOn, isPingOverAllSuccess);
     isPutSuccess.setTried(true);     
     isPutSuccess.addSubOperations("putdone");
     isPutSuccess.addSubOperations("putstatus");
     isPutSuccess.addSubOperations("gsiftp-put");
     if(advisoryDelete) {
       isPutSuccess.addSubOperations("srmrm");
     }
     if(browseAfterPut) {
       isPutSuccess.addSubOperations("srmls");
     }
     putTest.doPutFile(srm,proxyString,source, remoteTarget,copySourceFile, 
		plainPut,reservedSpace,browseAfterPut,overwrite,
		spaceToken,sLock,testCondition,isPutSuccess,abortOnFail,
		directGsiFTP,fStorageType);
   }catch(Exception e) {
     isPutSuccess.setTried(true);
     isPutSuccess.isOperOk(false);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
         isPutSuccess.setExplanation("Exception");
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("put_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Put","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	      servicePath,noPublish,
	      siteName,numOperations,"srmPrepareToPut","srmPrepareToPut-OverAll",
	      "Exception", dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
	     servicePath, noPublish, siteName,numOperations,
	     "srmPrepareToPut", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
         util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
      e.printStackTrace();
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("bringonline")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     boolean plainGet=false;
     String sToken= "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("plainget")) {
        Boolean b = (Boolean)me.getValue(); 
        plainGet = b.booleanValue();
      }
      else if(key.equalsIgnoreCase("spacetoken")) {
        sToken = (String)me.getValue(); 
        if(sToken.equals("")) { 
         sToken=null;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
   
  try {  
     SRMBringOnLineTest bringOnLineTest = 
		new SRMBringOnLineTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, 
            credential, proxyFile, onlyGsiFTPGet, 
			noPublish, detailed, false, defaultProxy,releaseFile, 
			dcau, bufferSize, parallelism,
            siteName, numOperations,startTimeStamp,dailyTest, 
		    numSites, retryTimeAllowed, statusWaitTime,
			localPublish, desiredFileLifeTime, bs, bsa, logger,pIntf,
			useDriverOn,isPingOverAllSuccess);
     isBringOnlineSuccess.setTried(true);
     isBringOnlineSuccess.addSubOperations("bringonlinestatus");
     bringOnLineTest.doBringOnLineFile
		(srm,proxyString,uid, source, localTarget, remoteTarget,
			plainGet,sLock,spaceToken,isBringOnlineSuccess,testCondition);
   }catch(Exception e) {
     isBringOnlineSuccess.setTried(true);
     isBringOnlineSuccess.isOperOk(false);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
       isBringOnlineSuccess.setExplanation("Exception");;
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("bringonline_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "BringOnline","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	      servicePath,noPublish,
	      siteName,numOperations,"srmBringOnline","srmBringOnline-OverAll",
		  "Exception",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
	     servicePath,noPublish,
		 siteName,numOperations,"srmBringOnline", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
         util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("get")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     boolean plainGet=false;
     String sToken= "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     TFileStorageType fStorageType = null;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("plainget")) {
        Boolean b = (Boolean)me.getValue(); 
        plainGet = b.booleanValue();
      }
      else if(key.equalsIgnoreCase("spacetoken")) {
        sToken = (String)me.getValue(); 
        if(sToken.equals("")) { 
         sToken=null;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
      else if(key.equalsIgnoreCase("filestoragetype")) {
        fStorageType = (TFileStorageType)me.getValue();
      }
     }
  try {  
     SRMGetTest getTest = new SRMGetTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, 
            credential, proxyFile, onlyGsiFTPGet, 
			noPublish, detailed, false, defaultProxy,releaseFile, dcau, 
			bufferSize,parallelism,siteName, numOperations, 
	        "srmPrepareToGet", startTimeStamp, 
	        dailyTest, numSites, retryTimeAllowed, statusWaitTime,
			false, localPublish,bs,bsa,
		    logger,pIntf,useGUC,gucScriptPath,tempScriptPath,useDriverOn,isPingOverAllSuccess);
     canGetContinue = new Boolean(true);
     isGetSuccess.setTried(true);     
     isGetSuccess.addSubOperations("getstatus");
     isGetSuccess.addSubOperations("gsiftp-get");
     if(releaseFile) {
       isGetSuccess.addSubOperations("srmrelease");
     }
     getTest.doGetFile(srm,proxyString,uid, source, localTarget, remoteTarget,
			plainGet,sLock, spaceToken, testCondition,canGetContinue,isGetSuccess,
		    directGsiFTP,fStorageType);
   }catch(Exception e) {
     Date d = new Date();
     isGetSuccess.setTried(true);
     isGetSuccess.isOperOk(false);
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
        isGetSuccess.setExplanation("Exception");
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("get_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Get","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmPrepareToGet","srmPrepareToGet-OverAll",
		  "Exception",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
		 servicePath,noPublish,
	     siteName,numOperations,"srmPrepareToGet","","Exception", 
	     dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.startsWith("pull") || type.startsWith("push") ||
		type.startsWith("gsiftp")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     String target = "";
     String sourceSiteName = "";
     boolean overwrite =false;
     String sToken= "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;
     TFileStorageType fStorageType=null;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("copysource")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("copysourcesite")) {
        sourceSiteName = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("copytarget")) {
        target = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("overwrite")) {
        Boolean tt = (Boolean)me.getValue(); 
        overwrite = tt.booleanValue();
      }
      else if(key.equalsIgnoreCase("spacetoken")) {
        sToken = (String)me.getValue(); 
        if(sToken.equals("")) { 
         sToken=null;
        }
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
      else if(key.equalsIgnoreCase("filestoragetype")) {
        fStorageType = (TFileStorageType)me.getValue();
      }
     }
   String tempType = type;
   if(!sourceSiteName.equals("")) {
     if(sourceSiteName.equals("gsiftp")) {
        sourceSiteName = "pull-"+sourceSiteName;
     }
     tempType=tempType+"-"+sourceSiteName;
   }
   else {
      if(tempType.equals("gsiftp")) {
        tempType="pull-gsiftp";
      }
   }
   try { 
     SRMCopyTest copyTest = new SRMCopyTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, 
            credential, proxyFile, onlyGsiFTPGet, 
			noPublish, detailed, defaultProxy,copyAdvisoryDelete, 
			dcau,bufferSize, parallelism,
	        siteName, numOperations, tempType, startTimeStamp, dailyTest, 
		    numSites, retryTimeAllowed, statusWaitTime,
			localPublish,bs,bsa, logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isCopySuccess.setTried(true);
     isCopySuccess.addSubOperations("srmCopy-"+tempType);
     isCopySuccess.addSubOperations("srmStatusOfCopyRequest-"+tempType);
     if(isCopySuccess.getSubOperations().size() > 0) {
        HashMap map = isCopySuccess.getSubOperations();
        OperOk subOk = (OperOk)map.get("srmCopy-"+tempType);
        if(subOk != null) {
         if(browseAfterPut) {
           subOk.addSubOperations("srmls");
         }
         if(copyAdvisoryDelete) {
           subOk.addSubOperations("srmrm");
         }
        }
     }
     copyTest.doCopyFile(srm,proxyString,uid, source, target, 
		sToken,browseAfterPut,
		overwrite,sLock,pushMode, copyServicePath,changeToCopyServicePath,
		isCopySuccess, testCondition,fStorageType);
    
   }catch(Exception e) {
     isCopySuccess.setTried(true);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("copy_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Copy","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
          operatorTimeStamp,
          servicePath,noPublish,
		  siteName,numOperations,tempType,"srmCopy-"+tempType,"Exception", 
	      dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
	     servicePath,noPublish,
		 siteName,numOperations,tempType, "srmCopy-"+tempType,"Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("mv") ||
	type.equalsIgnoreCase("srmmv")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     String target = "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("mvsource")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("mvtarget")) {
        target = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
  try {  
     SRMDirTest dirTest = new SRMDirTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, credential, 
			proxyFile, noPublish, detailed, defaultProxy,
		    siteName, numOperations,"srmMv", startTimeStamp, 
			dailyTest, numSites, retryTimeAllowed, statusWaitTime,
			localPublish,bs,bsa,
		    logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isMvSuccess.setTried(true);
     dirTest.doMvTest(srm,proxyString,uid, source, target, sLock,isMvSuccess,testCondition);
     if(!isMvSuccess.isOperOk()) {
       removeUrl = origRemoveUrl; 
     }
   }catch(Exception e) {
     isMvSuccess.setTried(true);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        isMvSuccess.setExplanation("Exception");
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("mv_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "srmMv","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmMv","srmMv","Exception", 
		  dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
		 servicePath,noPublish,
		 siteName,numOperations,"srmMv", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      } 
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("ls")
	|| type.equalsIgnoreCase("srmls")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
    
    try {
     SRMDirTest dirTest = new SRMDirTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, credential, 
			proxyFile, noPublish, detailed, defaultProxy,
            siteName, numOperations,"srmLs", startTimeStamp,
			dailyTest, numSites, retryTimeAllowed, statusWaitTime, 
		    localPublish,bs,bsa,logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isLsSuccess.setTried(true);
     dirTest.doSrmLsTest(srm,proxyString,uid, source, sLock,isLsSuccess,testCondition);
    }catch(Exception e) {
     isLsSuccess.setTried(true);
     isLsSuccess.isOperOk(false);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
			!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("browse_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Browse","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmLs","srmLs","Exception", 
		  dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
		 servicePath,noPublish,
		 siteName,numOperations,"srmLs", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
    }
    if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
    if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("srmrm") ||	
      type.equalsIgnoreCase("rm")) {
	 ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
    
   try {
     SRMDirTest dirTest = new SRMDirTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, credential, 
			proxyFile, noPublish, detailed, defaultProxy,
            siteName, numOperations,"srmRm", startTimeStamp,
			dailyTest, numSites, retryTimeAllowed, statusWaitTime,
			localPublish,bs,bsa, logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isRmSuccess.setTried(true); 
     dirTest.doSrmRmTest(srm,proxyString,uid, removeUrl, sLock,isRmSuccess,testCondition);
   }catch(Exception e) {
     Date d = new Date();
     isRmSuccess.setTried(true); 
     isRmSuccess.isOperOk(false);
     String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        isRmSuccess.setExplanation("Exception");
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("srmrm_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "SrmRm","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmRm","srmRm",
	      "Exception", dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp, servicePath,noPublish,
		 siteName,numOperations,"srmRm", "","Exception", dailyTest, 
		 numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("mkdir")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
    
   try {
     SRMDirTest dirTest = new SRMDirTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, credential, 
			proxyFile, noPublish, detailed, defaultProxy,
            siteName, numOperations,"srmMkdir", startTimeStamp,
			dailyTest,numSites, retryTimeAllowed, statusWaitTime,
			localPublish,bs,bsa, logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isMkdirSuccess.setTried(true);
     dirTest.doMkdirTest(srm,proxyString,uid, source, sLock,isMkdirSuccess,testCondition);
   }catch(Exception e) {
      Date d = new Date ();
      String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("mkdir_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Mkdir","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmMkdir","srmMkdir",
	      "Exception", dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
		 servicePath,noPublish,
		 siteName,numOperations,"srmMkdir", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
     if(sLock != null) sLock.setIncrementCount();
   }
   else if(type.equalsIgnoreCase("rmdir")) {
     ISRM srm = null;
     StringBuffer proxyString = new StringBuffer();
     String source = "";
     SharedObjectLock sLock= null; 
     int testCondition = -1;

     Set set = cmd.entrySet();
     Iterator itr = set.iterator ();
     while(itr.hasNext()) {
      Map.Entry me = (Map.Entry) itr.next();
      String key = (String) me.getKey();
      if(key.equalsIgnoreCase("ISRM")) {
        srm = (ISRM)me.getValue();
      }
      else if(key.equalsIgnoreCase("proxystring")) {
        proxyString = (StringBuffer)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("source")) {
        source = (String)me.getValue(); 
      }
      else if(key.equalsIgnoreCase("slock")) {
        sLock = (SharedObjectLock)me.getValue();
      }
      else if(key.equalsIgnoreCase("testcondition")) {
        Integer x = (Integer)me.getValue();
        testCondition = x.intValue();  
      }
     }
    
   try {
     SRMDirTest dirTest = new SRMDirTest(publishUrlStr, displayType,
            testType,timeStamp, fullTimeStamp, servicePath, 
			credential, proxyFile, noPublish, detailed, defaultProxy,
	        siteName, numOperations,"srmRmdir", startTimeStamp,
			dailyTest, numSites, retryTimeAllowed, statusWaitTime,
			localPublish,bs,bsa, logger,pIntf,useDriverOn,isPingOverAllSuccess);
     isRmdirSuccess.setTried(true);
     dirTest.doRmdirTest(srm,proxyString,uid, source, sLock,isRmdirSuccess,testCondition);
    }catch(Exception e) {
      Date d = new Date ();
      String operatorTimeStamp = d.toString();
     util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
     try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("rmdir_"+testCondition);
        rrs.setActualResult("Exception");
        rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
        util.webScript(publishUrlStr,displayType,
         "Rmdir","Exception",
          rrs,e.toString(),
          timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
	      siteName,numOperations,"srmRmdir","srmRmdir",
	      "Exception",dailyTest, numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,
         displayType,"BLANK","",
         null, "", timeStamp, fullTimeStamp, startTimeStamp, 
		 operatorTimeStamp, servicePath,noPublish,
		 siteName,numOperations,"srmRmdir", "","Exception", 
		 dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
      }catch(Exception ee) {ee.printStackTrace();}

      e.printStackTrace();
      }
    }
   }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
  }catch(Exception e){
      Date d = new Date ();
      String operatorTimeStamp = d.toString();
      e.printStackTrace();
     util.printMessage("SRM-TESTER: Exception : " + e.getMessage() +"\n",logger, pIntf);
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
       try {
        ReportRequestStatus rrs = new ReportRequestStatus();
        rrs.setLinkName("generalexp2"); 
        util.webScript(publishUrlStr,displayType,"Exception","",rrs,
         e.getMessage(),timeStamp,fullTimeStamp, startTimeStamp, 
         operatorTimeStamp,
		 servicePath,noPublish,
		 siteName,numOperations,typeString,"","Exception",dailyTest,numSites,
		 localPublish,bs,bsa);
       }catch(Exception we) { we.printStackTrace(); }
     }
     if(timeOutCallBack != null) timeOutCallBack.interruptNow(true);
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReserveSpaceTest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void  doReserveSpace(ISRM srm, String uid,
	String exceptionMessage) throws  Exception {
      Date d = new Date ();
      String operatorTimeStamp = d.toString();

 isReserveSpaceSuccess.setTried(true);
 try {
   util.printMessageHL("\nSending ReserveSpace ...\n", logger, pIntf);
   util.printMessageHL2("StartTime="+new Date()+"\n",logger,pIntf);
   util.printMessage("\n===========================================",logger,pIntf);
   SrmReserveSpaceRequest spaceRequest = new SrmReserveSpaceRequest();
   ReportRequestStatus rrs = util.createReportRequestStatus("","","");
   if(!uid.equals("")) {
     spaceRequest.setAuthorizationID(uid);
     util.printMessage("\nUID " + uid,logger, pIntf);
   }
   //util.printMessage("SpaceTokenDescription=test description",logger,pIntf);
   //spaceRequest.setUserSpaceTokenDescription("test description");
   util.printMessage("TotalSpace=1000",logger,pIntf);
   util.printMessage("GuaranteedSpace=1000",logger,pIntf);
   util.printMessage("LifeTime=10000",logger,pIntf);
   util.printMessage("===========================================\n",logger,pIntf);
   spaceRequest.setDesiredSizeOfTotalSpace(new UnsignedLong(1000));
   spaceRequest.setDesiredSizeOfGuaranteedSpace(new UnsignedLong(1000));
   spaceRequest.setDesiredLifetimeOfReservedSpace(new Integer(10000));

   TRetentionPolicyInfo retentionPolicyInfo = new TRetentionPolicyInfo();
   retentionPolicyInfo.setRetentionPolicy(TRetentionPolicy.REPLICA);
   //retentionPolicyInfo.setAccessLatency(TAccessLatency.ONLINE);

   spaceRequest.setRetentionPolicyInfo(retentionPolicyInfo);

  if(srm == null) {
     String displayStatus="Exception";
     if(exceptionMessage.equals("")) {
        exceptionMessage = "Could not connect to SRM";
        displayStatus="Failed";
     }
     isReserveSpaceSuccess.setExplanation(displayStatus);
     isReserveSpaceSuccess.isOperOk(false);
     util.printMessage(exceptionMessage,logger,pIntf);
     try {
       util.webScript(publishUrlStr,displayType,
	    "ReserveSpace",displayStatus,null,exceptionMessage,
		   timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
		   servicePath,noPublish,
		   siteName,numOperations,"srmReserveSpace","srmReserveSpace",
	       displayStatus,dailyTest, numSites,localPublish,bs,bsa);
     }catch(Exception e) { }
     return;
   }

   SrmReserveSpaceResponse spaceResponse = null;

   int ssTimeStamp = util.startTimeStamp();
   timeOutCallBack = new TimeOutCallBack(spaceResponse,ssTimeStamp);
   timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
   timeOutCallBack.start();

   spaceResponse = srm.srmReserveSpace(spaceRequest);

   timeOutCallBack.setObject(spaceResponse);

   rrs.setLinkName("reservespace");
   rrs.setUserId(uid);
   //rrs.setUserDescription("test description");
   rrs.setTotalSpace(""+1000);
   rrs.setGuarnSpace(""+1000);
   rrs.setLifeTime(""+10000);
   rrs.setRetentionPolicy("Replica");
   HashMap resultMap = new HashMap();
   String displayStatus = "Ok";
   String explanation="";

   util.printMessage("===========================================",logger,pIntf);
   if(spaceResponse != null) {
      if(spaceResponse.getReturnStatus() != null) {
        TReturnStatus spaceStatus = spaceResponse.getReturnStatus();
        TStatusCode sCode = spaceStatus.getStatusCode();
        if(spaceStatus.getExplanation() != null) {
          explanation=spaceStatus.getExplanation();
        }
        if(sCode == null) {
          rrs.setActualResult("Null status");
          rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
          isReserveSpaceSuccess.isOperOk(false);
          displayStatus = "Failed";
        }
        else {
        resultMap.put(uid,sCode);
        String rToken = spaceResponse.getRequestToken();
        util.printMessage("SpaceRequest Status Code=" +
			sCode.toString(),logger, pIntf);
        util.printMessage("SpaceRequest Explanation=" +
			explanation,logger, pIntf);
        util.printMessage("Request token=" + rToken, logger, pIntf);
        
        if(sCode == TStatusCode.SRM_REQUEST_QUEUED ||
           sCode == TStatusCode.SRM_REQUEST_INPROGRESS) {
           int i = 0;
           util.webScript(publishUrlStr,
             displayType,"ReserveSpaceMethod",sCode.toString(), null, 
			 explanation, timeStamp, fullTimeStamp,startTimeStamp, 
	         operatorTimeStamp, servicePath,
	  		 noPublish, siteName,numOperations,"srmReserveSpace",
	   	     "srmReserveSpace",
             "Ok",dailyTest,numSites,localPublish,bs,bsa);
           while (resultMap.size() > 0) {
// AS 070809
// changed from 5000 to 30000
             Thread.sleep(statusWaitTime*1000);
             if(i >= resultMap.size()) {
               i = 0;
             }
             if(rToken != null) { 
               SrmStatusOfReserveSpaceRequestRequest spaceStatusRequest =
                    new SrmStatusOfReserveSpaceRequestRequest();
               spaceStatusRequest.setRequestToken(rToken); 
               if(!uid.equals("")) {
                 spaceStatusRequest.setAuthorizationID(uid);
               }
               SrmStatusOfReserveSpaceRequestResponse spaceStatusResponse=null;
               ssTimeStamp = util.startTimeStamp();
               timeOutCallBack = new TimeOutCallBack
					(spaceStatusResponse,ssTimeStamp);
               timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
               timeOutCallBack.start();
               spaceStatusResponse =
                     srm.srmStatusOfReserveSpaceRequest(spaceStatusRequest);
               timeOutCallBack.setObject(spaceStatusResponse);
               if(spaceStatusResponse.getReturnStatus() != null) {
                 spaceStatus = spaceStatusResponse.getReturnStatus();
                 if(spaceStatus.getExplanation() != null) {
                   explanation=spaceStatus.getExplanation();
                 }
                 TStatusCode sCode_1 = spaceStatus.getStatusCode();
                 util.printMessage("StatusOfReserveSpaceRequest Status Code=" +
						sCode_1.toString(),logger, pIntf);
                 util.printMessage("StatusOfReserveSpaceRequest Explanation=" +
						explanation,logger, pIntf);
                 if(sCode_1 != TStatusCode.SRM_REQUEST_INPROGRESS &&
                    sCode_1 != TStatusCode.SRM_REQUEST_QUEUED) {
                      resultMap.remove(uid);
                      rrs.setActualResult(sCode_1.getValue());
                      spaceToken = spaceStatusResponse.getSpaceToken();
                    
                    if(sCode_1 == TStatusCode.SRM_SUCCESS ||
                       sCode_1 == TStatusCode.SRM_LOWER_SPACE_GRANTED ||
                       sCode_1 == TStatusCode.SRM_NOT_SUPPORTED) {
                       displayStatus = "Ok";
                       if(sCode_1 == TStatusCode.SRM_NOT_SUPPORTED) {
                         isReserveSpaceSuccess.setExplanation("N.S.");
                         isReserveSpaceSuccess.isOperOk(false);
                         displayStatus = "N.S.";
                       }
                     }  
                     else {
                       displayStatus = "Failed";
                       isReserveSpaceSuccess.isOperOk(false);
                     }
                     try {
                       util.webScript(publishUrlStr,
                        displayType,"ReserveSpaceStatusMethod",
						sCode_1.toString(), null, explanation,
                        timeStamp, fullTimeStamp,startTimeStamp, 
				        operatorTimeStamp, servicePath,
	  				    noPublish, siteName,numOperations,"srmReserveSpace",
	   	                "srmStatusOfReserveSpaceRequest",
                        displayStatus,dailyTest,numSites,localPublish,bs,bsa);
                     }catch(Exception e) { e.printStackTrace(); }
                    }
               }
               else { 
                 util.printMessage("Null return status from " +
					"srmStatusOfReserveSpaceRequest", logger,pIntf);
                 try {
                   util.webScript(publishUrlStr,
                    displayType,"ReserveSpaceStatusMethod","Failed", null,
			        "Notes from tester : " +
				    "Null return status from srmStatusOfReserveSpaceRequest",
                    timeStamp, fullTimeStamp,startTimeStamp, 
					operatorTimeStamp, servicePath,noPublish,
                    siteName,numOperations,"srmReserveSpace",
		            "srmStatusOfReserveSpaceRequest",
                    "Failed",dailyTest,numSites,localPublish,bs,bsa);
                 }catch(Exception e) { e.printStackTrace(); }
               }
              }  
              else {
                  util.printMessage
				   ("Expecting requestToken for this status code ", 
						logger,pIntf);
                  resultMap.remove(uid); 
                  try {
                   util.webScript(publishUrlStr,
                    displayType,"ReserveSpaceStatusMethod","Failed", null, 
					"Notes from tester : " +
					"Expecting requesttoken for this status code",
                    timeStamp, fullTimeStamp,startTimeStamp, 
                    operatorTimeStamp,
					servicePath,noPublish,
                    siteName,numOperations,"srmReserveSpace",
		            "srmStatusOfReserveSpaceRequest",
                    "Failed",dailyTest,numSites,localPublish,bs,bsa);
                  }catch(Exception e) { e.printStackTrace(); }
              }
            }
        }
        else {
           rrs.setActualResult(sCode.getValue());
           spaceToken = spaceResponse.getSpaceToken();
           String displayStatusMsg="Ok";

           if(sCode == TStatusCode.SRM_SUCCESS || 
		      sCode == TStatusCode.SRM_NOT_SUPPORTED ||
		      sCode == TStatusCode.SRM_DONE) {
               rrs.setMatchingInfo("Yes");
              if(sCode == TStatusCode.SRM_NOT_SUPPORTED) {
                displayStatusMsg = "N.S.";
                isReserveSpaceSuccess.isOperOk(false);
              }
           }
           else {
             rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
             displayStatusMsg = "Failed";
             isReserveSpaceSuccess.isOperOk(false);
           }
         
           if(displayStatusMsg.equals("Ok")) {
             isReserveSpaceSuccess.isOperOk(true);
           }
           else {
             isReserveSpaceSuccess.isOperOk(false);
             isReserveSpaceSuccess.setExplanation(displayStatusMsg);
           }
           util.webScript(publishUrlStr,
             displayType,"ReserveSpace",rrs.getActualResult(), rrs,
             explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	         operatorTimeStamp, servicePath,noPublish,
	         siteName,numOperations,"srmReserveSpace","srmReserveSpace",
	         displayStatus,dailyTest,numSites,localPublish,bs,bsa);
        }


        if(spaceToken != null && !spaceToken.equals("")) {
          util.printMessage("TargetSpaceToken=" + spaceToken,logger,pIntf);
          rrs.setSpaceToken(spaceToken);
        }
        util.printMessage("===========================================\n",logger,pIntf);
        util.printMessageHL2("EndTime="+new Date()+"\n",logger,pIntf);
      }
     }
    }
    else {
       explanation="Null status";
       rrs.setActualResult("Null status");
       rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
       displayStatus = "Failed";
    }

    if(spaceToken != null && spaceToken.equals("")) {
       util.printMessage("ReserveSpaceRequest failed", logger,pIntf);
    }

    /*
    util.webScript(publishUrlStr,
      displayType,"ReserveSpace","", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReserveSpace","","Sending",
	  dailyTest,numSites,localPublish,bs,bsa);
    */
     rrs.setLinkName("reservespace");
    util.webScript(publishUrlStr,
      displayType,"ReserveSpace",rrs.getActualResult(), rrs,
      explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReserveSpace","srmReserveSpace-OverAll",
	  displayStatus,dailyTest,numSites,localPublish,bs,bsa);
  }catch(Exception e) {
    util.printMessageHException("\n"+e.toString(),logger,pIntf);
    e.printStackTrace();
    isReserveSpaceSuccess.setExplanation("Exception");
    throw e;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doGetSpaceMetaData
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doGetSpaceMetaData(ISRM srm, String uid, String sToken) 
	throws Exception {

   Date d = new Date ();
   String operatorTimeStamp = d.toString();

  if(sToken == null || sToken.equals("")) {
    util.printMessage("Cannot do space meta for null token", logger,pIntf);
    util.webScript(publishUrlStr,
      displayType,"SendingGETSPACEMETA","GetSpaceMeta", null,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","","Sending",dailyTest,
	  numSites,localPublish,bs,bsa);
    util.webScript(publishUrlStr,
      displayType,"GetSpaceMeta","Failed", null,
      "Cannot do space meta for null token", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","srmGetSpaceMetaData",
	  "N.T.",dailyTest,numSites,localPublish,bs,bsa);
    return;
  }

 isGetSpaceMetadataSuccess.setTried(true);

 try {
  util.printMessageHL("\nSending GetSpaceMetaData ...\n", logger,pIntf);
  util.printMessageHL2("StartTime="+new Date()+"\n", logger,pIntf);
  SrmGetSpaceMetaDataRequest req = new SrmGetSpaceMetaDataRequest();
  util.printMessage("\n===================================",logger,pIntf);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
    util.printMessage("UID="+uid,logger,pIntf);
  }
  ArrayOfString aSpaceTokens = new ArrayOfString();
  String[] tokenArray = new String[1];
  tokenArray[0] = sToken;
  aSpaceTokens.setStringArray(tokenArray);
  req.setArrayOfSpaceTokens(aSpaceTokens);
  util.printMessage("SpaceToken="+sToken,logger,pIntf);
  util.printMessage("===================================\n",logger,pIntf);

  SrmGetSpaceMetaDataResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  response = srm.srmGetSpaceMetaData(req);
  timeOutCallBack.setObject(response);
  ReportRequestStatus rrs = util.createReportRequestStatus("","","");
  
  rrs.setSpaceToken(sToken);
  rrs.setLinkName("getspacemeta");
   
  util.printMessage("===================================",logger,pIntf);
  String displayStatusMsg = "Ok";
  String explanation ="";
  String statusMsg="";
  if(response != null) {
     if(response.getReturnStatus() != null) {
       util.printMessage("StatusCode=" + 
			response.getReturnStatus().getStatusCode(),logger,pIntf);
       util.printMessage("Explanation=" + 
			response.getReturnStatus().getExplanation(),logger,pIntf);
       statusMsg=response.getReturnStatus().getStatusCode().toString();
       if(response.getReturnStatus().getExplanation() != null) {
         explanation=response.getReturnStatus().getExplanation(); 
       }
       rrs.setActualResult
			(response.getReturnStatus().getStatusCode().getValue());
       if(response.getReturnStatus().getStatusCode() == 
			TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE){
           rrs.setMatchingInfo("Yes");
           if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
             displayStatusMsg = "N.S.";
           }
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
           displayStatusMsg = "Failed";
        }
        if(displayStatusMsg.equals("Ok")) {
          isGetSpaceMetadataSuccess.isOperOk(true);
        }
        else {
          isGetSpaceMetadataSuccess.setExplanation(displayStatusMsg);
        }
      }
      else {
         explanation="Notes from tester: Null return status";
         rrs.setActualResult("Null return status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
         displayStatusMsg = "Failed";
         statusMsg="Failed";
      }

     util.webScript(publishUrlStr,
      displayType,"SendingGETSPACEMETA","GetSpaceMeta", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","","Sending",dailyTest,
	  numSites,localPublish,bs,bsa);
     rrs.setSpaceToken(sToken);
     rrs.setLinkName("getspacemeta");
     util.webScript(publishUrlStr,
      displayType,"GetSpaceMeta",statusMsg, rrs,
      explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","srmGetSpaceMetaData",
	  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
  }
  else {
     util.webScript(publishUrlStr,
      displayType,"SendingGETSPACEMETA","Sending GetSpaceMeta", rrs,
      "Null response from server", timeStamp, fullTimeStamp, startTimeStamp, 
      operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","","Failed",
	  dailyTest,numSites,localPublish,bs,bsa);
     rrs.setLinkName("getspacemeta");
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     util.webScript(publishUrlStr,
      displayType,"GetSpaceMeta","Failed", rrs,
      "Null response from server", timeStamp, fullTimeStamp,startTimeStamp,
      operatorTimeStamp,
	  servicePath,noPublish,
	  siteName,numOperations,"srmGetSpaceMeta","srmGetSpaceMetaData",
	  "Failed",dailyTest,numSites,localPublish,bs,bsa);
  }
  util.printMessage("===================================\n",logger,pIntf);
  util.printMessageHL2("EndTime="+new Date()+"\n", logger,pIntf);

  }catch(Exception e) {
    util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
    isGetSpaceMetadataSuccess.setExplanation("Exception");
    e.printStackTrace();
    throw e;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doUpdateSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doUpdateSpace
	(ISRM srm, String uid, String sToken, long newSize, int newTime) 
		throws Exception {
      Date d = new Date ();
      String operatorTimeStamp = d.toString();

  if(sToken == null || sToken.equals("")) {
    util.printMessage("Cannot update space for null token", logger,pIntf);
    throw new Exception("cannot update space for null token");
  }

 try {
  util.printMessageHL("\nSending UpdateSpace ...\n", logger,pIntf);
  util.printMessageHL2("StartTime="+new Date()+"\n", logger,pIntf);
  SrmUpdateSpaceRequest req = new SrmUpdateSpaceRequest();
  util.printMessage("\n===================================",logger,pIntf);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
    util.printMessage("UID="+uid,logger,pIntf);
  }
  req.setSpaceToken(sToken);
  req.setNewSizeOfTotalSpaceDesired(new UnsignedLong(newSize));
  req.setNewLifeTime(new Integer(newTime));
  util.printMessage("SpaceToken="+sToken,logger,pIntf);
  util.printMessage("NewSpaceSize="+newSize,logger,pIntf);
  util.printMessage("NewSpaceTime="+newTime,logger,pIntf);
  util.printMessage("===================================\n",logger,pIntf);

  SrmUpdateSpaceResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  response = srm.srmUpdateSpace(req);
  timeOutCallBack.setObject(response);
  ReportRequestStatus rrs = util.createReportRequestStatus("","","");
  
  rrs.setSpaceToken(sToken);
  rrs.setLinkName("updatespace");
  rrs.setNewSpaceSize(""+newSize);
  rrs.setNewLifeTime(""+newTime);
   
  util.printMessage("===================================",logger,pIntf);
  String displayStatusMsg = "Ok";
  String explanation="";
  if(response != null) {
     if(response.getSizeOfTotalSpace() != null) {
       rrs.setTotalSpace(""+response.getSizeOfTotalSpace());
       util.printMessage("\nNewTotalSpace="+
			response.getSizeOfTotalSpace(),logger,pIntf);
     }
     if(response.getSizeOfGuaranteedSpace() != null) {
       rrs.setGuarnSpace(""+response.getSizeOfGuaranteedSpace());
       util.printMessage("\nNewGuaranteedSpace="+
			response.getSizeOfGuaranteedSpace(),logger,pIntf);
     }
     if(response.getLifetimeGranted() != null) {
       rrs.setLifeTime(""+response.getLifetimeGranted());
       util.printMessage("\nLifeTimeGranted="+
			response.getLifetimeGranted(),logger,pIntf);
     }
     if(response.getReturnStatus() != null) {
       if(response.getReturnStatus().getExplanation() != null) {
         explanation=response.getReturnStatus().getExplanation();
       }
       util.printMessage("StatusCode=" + 
			response.getReturnStatus().getStatusCode(),logger,pIntf);
       util.printMessage("Explanation=" + 
			response.getReturnStatus().getExplanation(),logger,pIntf);
       rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
           rrs.setMatchingInfo("Yes");
           if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
             displayStatusMsg = "N.S.";
           }
        }
        else {
           if(response.getReturnStatus().getStatusCode() == 
				TStatusCode.SRM_INVALID_REQUEST) {
              if(newSize == 0 && newTime == 0) {
                rrs.setMatchingInfo("Yes");
              }
              else {
                rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
                displayStatusMsg = "Failed";
              }
           }
           else {
             rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
             displayStatusMsg = "Failed";
           }
        }
      }
      else {
         explanation="Null return status from server";
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
      }

     util.webScript(publishUrlStr,
      displayType,"SendingUPDATESPACE","SrmUpdateSpace", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmUpdateSpace","","Sending",dailyTest,
	  numSites,localPublish,bs,bsa);
     rrs.setLinkName("updatespace");
     util.webScript(publishUrlStr,
      displayType,"UpdateSpace",displayStatusMsg, rrs,
      explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmUpdateSpace","srmUpdateSpace",
	  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
  }
  else {
     util.webScript(publishUrlStr,
      displayType,"SendingUPDATESPACE","Sending UpdateSpace", rrs,
      "Notes from tester : Null response from server", 
	  timeStamp, fullTimeStamp, startTimeStamp, 
      operatorTimeStamp, servicePath,noPublish, siteName,numOperations,
	  "srmUpdateSpace","","Failed",dailyTest,numSites,localPublish,bs,bsa);
     rrs.setLinkName("updatespace");
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     util.webScript(publishUrlStr,
      displayType,"UpdateSpace","Failed", rrs,
      "Null response from server", timeStamp, fullTimeStamp, startTimeStamp,
      operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmUpdateSpace","srmUpdateSpace",
	  "Failed",dailyTest,numSites,localPublish,bs,bsa);
  }
  util.printMessage("===================================\n",logger,pIntf);
  util.printMessageHL2("EndTime="+new Date()+"\n", logger,pIntf);

  }catch(Exception e) {
    util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
    e.printStackTrace();
    throw e;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doReleaseSpace
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doReleaseSpace(ISRM srm, String uid, String sToken) throws Exception {
      Date d = new Date ();
      String operatorTimeStamp = d.toString();
 
  if(sToken == null || sToken.equals("")) {
    util.printMessage("Cannot release space for null token", logger,pIntf);

    util.webScript(publishUrlStr,
      displayType,"SendingRELEASESPACE","SrmReleaseSpace", null,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReleaseSpace","","Sending",
	  dailyTest,numSites,localPublish,bs,bsa);
    util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Failed", null,
      "Cannot release space for null token", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReleaseSpace","srmReleaseSpace",
	  "N.T.",dailyTest,numSites,localPublish,bs,bsa);
     return;
  }

 isReleaseSpaceSuccess.setTried(true);
 try {
  util.printMessageHL("\nSending ReleaseSpace ...\n", logger,pIntf);
  util.printMessageHL2("StartTime="+new Date()+"\n", logger,pIntf);
  SrmReleaseSpaceRequest req = new SrmReleaseSpaceRequest();
  util.printMessage("\n===================================",logger,pIntf);
  if(!uid.equals("")) {
    req.setAuthorizationID(uid);
    util.printMessage("UID="+uid,logger,pIntf);
  }
  req.setSpaceToken(sToken);
  util.printMessage("SpaceToken="+sToken,logger,pIntf);
  util.printMessage("===================================\n",logger,pIntf);

  SrmReleaseSpaceResponse response = null;

  int ssTimeStamp = util.startTimeStamp();
  timeOutCallBack = new TimeOutCallBack(response,ssTimeStamp);
  timeOutCallBack.setDriverInfo(useDriverOn,isPingOverAllSuccess);
               //timeOutCallBack.setSharedObjectLock(sLock);
  timeOutCallBack.start();

  response = srm.srmReleaseSpace(req);
  timeOutCallBack.setObject(response);
  ReportRequestStatus rrs = util.createReportRequestStatus("","","");
  
  rrs.setSpaceToken(sToken);
  rrs.setLinkName("releasespace");
   
  String displayStatusMsg = "Ok";
  String statusMsg="";
  String explanation="";
  util.printMessage("===================================",logger,pIntf);
  if(response != null) {
     if(response.getReturnStatus() != null) {
       util.printMessage("StatusCode=" + 
			response.getReturnStatus().getStatusCode(),logger,pIntf);
       util.printMessage("Explanation=" + 
			response.getReturnStatus().getExplanation(),logger,pIntf);
       statusMsg=response.getReturnStatus().getStatusCode().toString();
       if(response.getReturnStatus().getExplanation() != null) {
         explanation=response.getReturnStatus().getExplanation();
       }
       rrs.setActualResult(response.getReturnStatus().getStatusCode().getValue());
       if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_SUCCESS ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED ||
           response.getReturnStatus().getStatusCode() == TStatusCode.SRM_DONE) {
           rrs.setMatchingInfo("Yes");
           if(response.getReturnStatus().getStatusCode() == TStatusCode.SRM_NOT_SUPPORTED) {
              displayStatusMsg = "N.S.";
           }
        }
        else {
           rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
           displayStatusMsg = "Failed";
        }
        if(displayStatusMsg.equals("Ok")) {
         isReleaseSpaceSuccess.isOperOk(true);
        }
        else {
         isReleaseSpaceSuccess.setExplanation(displayStatusMsg);
        }
      }
      else {
         explanation="Null status";
         rrs.setActualResult("Null status");
         rrs.setMatchingInfo("No. expecting SRM_SUCCESS or SRM_NOT_SUPPORTED");
         displayStatusMsg = "Failed";
         statusMsg="Failed";
      }

     util.webScript(publishUrlStr,
      displayType,"SendingRELEASESPACE","SrmReleaseSpace", rrs,
      "", timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReleaseSpace","","Sending",
	  dailyTest,numSites,localPublish,bs,bsa);
     rrs.setLinkName("releasespace");
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace",statusMsg, rrs,
      explanation, timeStamp, fullTimeStamp, startTimeStamp, 
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReleaseSpace","srmReleaseSpace",
	  displayStatusMsg,dailyTest,numSites,localPublish,bs,bsa);
  }
  else {
     util.webScript(publishUrlStr,
      displayType,"SendingRELEASESPACE","Sending ReleaseSpace", rrs,
      "Notes from tester: Null response from server", 
	  timeStamp, fullTimeStamp, startTimeStamp, operatorTimeStamp,
	  servicePath,noPublish, siteName,numOperations,
	  "srmReleaseSpace","","Failed",dailyTest,numSites,localPublish,bs,bsa);
     rrs.setLinkName("releasespace");
     rrs.setActualResult("Null response");
     rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
     util.webScript(publishUrlStr,
      displayType,"ReleaseSpace","Failed", rrs,
      "Null response from server", timeStamp, fullTimeStamp, startTimeStamp,
	  operatorTimeStamp, servicePath,noPublish,
	  siteName,numOperations,"srmReleaseSpace","srmReleaseSpace",
	  "Failed",dailyTest,numSites,localPublish,bs,bsa);
  }
  util.printMessage("===================================\n",logger,pIntf);
  util.printMessageHL2("EndTime="+new Date()+"\n", logger,pIntf);

  }catch(Exception e) {
    util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
    e.printStackTrace();
    isReleaseSpaceSuccess.setExplanation("Exception");
    throw e;
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// do3PartyCopy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void do3PartyCopy (StringBuffer proxyString) throws Exception {

  int type = 0;

  String[] surl = new String[1];
  String[] turl = new String[1];
  boolean[] overwritefiles = new boolean[1];

  if((copySourceFile.startsWith("srm")) && (copyTarget.startsWith("srm"))) {
    type = 0;
  }
  else {
    if((copySourceFile.startsWith("gsiftp")) && 
		(copyTarget.startsWith("srm"))) { type = 1; }
    else {
      throw new Exception ("For the 3rd party copy transfers, " +
		"sourceURL must begin with either srm:// or gsiftp:// "  +
        "and targetURL must begin with srm:// " +
        "Given SURL="+copySourceFile+", " + "TURL="+copyTarget);
    }
  }

  surl [0] = new String(copySourceFile.trim());
  turl [0] = new String(copyTarget.trim());
  overwritefiles[0] = overwrite;

  String sourceSRM = "";
  String targetSRM = "";

  MyGlobusURL gsurl = new MyGlobusURL(copySourceFile.trim());
  String sHost = gsurl.getHost();
  int sPort = gsurl.getPort();
  String sPath = gsurl.getPath();
  String sServiceName="/srm/managerv2";
  int sindex = sPath.indexOf("?SFN");
  if(sindex != -1) {
   sServiceName=sPath.substring(0,sindex);
  }

  if(type == 0) {
   sourceSRM="srm://"+sHost+":"+sPort+sServiceName;
  }
  else {
   sourceSRM="gsiftp://"+sHost+":"+sPort;
  }

  MyGlobusURL gturl = new MyGlobusURL(copyTarget.trim());
  String tHost = gturl.getHost();
  int tPort = gturl.getPort();
  String tPath = gturl.getPath();
  String tServiceName="/srm/managerv2";
  int tindex = tPath.indexOf("?SFN");
  if(tindex != -1) {
    tServiceName=tPath.substring(0,tindex);
  }

  targetSRM="srm://"+tHost+":"+tPort+"/"+tServiceName;

  util.printMessage("\n+++++ Doing \"3rd Party Copy request +++++", logger,pIntf);
  util.printMessage("\nSourceSRM : " + sourceSRM, logger,pIntf);
  util.printMessage("TargetSRM : " + targetSRM, logger,pIntf);

  try {
    util.webScript(publishUrlStr, displayType,"Sending3START",
	  "3rd Party Copy Request now... ",
	   util.createReportRequestStatus(sourceSRM,targetSRM,""),
      "", timeStamp, fullTimeStamp, startTimeStamp, startTimeStamp,
	  servicePath,noPublish,
      siteName,numOperations,"3PartyCopy","","Sending",dailyTest,
	  numSites,localPublish,bs,bsa);
  }catch(Exception e) { 
    e.printStackTrace(); 
    util.printMessageHException("\n"+e.getMessage(),logger,pIntf);
  }

  String sourceSRMTURL = "";

  if(type == 0) {
    ISRM sSRM = connectToSRM(sourceSRM,proxyString);
    SRMGetTest getTest = new SRMGetTest(publishUrlStr, displayType,
			testType,timeStamp, fullTimeStamp, servicePath, 
            credential, proxyFile, onlyGsiFTPGet, 
			noPublish, detailed, true, defaultProxy, releaseFile, dcau, 
            bufferSize, parallelism, siteName, numOperations, 
			"srmPrepareToGet", startTimeStamp, 
		    dailyTest, numSites, retryTimeAllowed, statusWaitTime,
			false, localPublish,bs,bsa, logger,pIntf,useGUC,
		    gucScriptPath,tempScriptPath,useDriverOn,isPingOverAllSuccess);
    canGetContinue = new Boolean(true);
    isGetSuccess.setTried(true);
    if(releaseFile) {
       isGetSuccess.addSubOperations("srmrelease");
    }
    sourceSRMTURL = 
		getTest.doGetFile(sSRM, proxyString,uid, copySourceFile,localTarget, 
	        remoteTarget, false,null,spaceToken,-1, canGetContinue, isGetSuccess,
	        false,null);
  }
  else {
    sourceSRMTURL=copySourceFile.trim();
  }

  if(!sourceSRMTURL.equals("")) {
    ISRM tSRM = connectToSRM(targetSRM,proxyString);
    SRMPutTest putTest = new SRMPutTest(publishUrlStr, displayType, 
                     testType, timeStamp, fullTimeStamp, 
				     servicePath, credential, proxyFile,
                     onlyGsiFTPGet, noPublish, detailed, true, defaultProxy, 
					 advisoryDelete, uid, 
				   	 dcau, bufferSize, parallelism, siteName, numOperations, 
		             startTimeStamp, dailyTest, 
				     numSites, retryTimeAllowed, statusWaitTime,
					 localPublish,bs,bsa, logger,remoteFileLifeTime,pIntf,useGUC,
		  gucScriptPath, tempScriptPath,useDriverOn,isPingOverAllSuccess);
    isPutSuccess.addSubOperations("putdone");
    if(advisoryDelete) {
      isPutSuccess.addSubOperations("srmrm");
    }
    if(browseAfterPut) {
      isPutSuccess.addSubOperations("srmls");
    }
    putTest.doPutFile (tSRM, proxyString,sourceSRMTURL,
		copyTarget,copySourceFile, false,false,browseAfterPut,
		overwrite, spaceToken,null,-1, isPutSuccess,true,false,null);
  }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createPingReportStatus
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void  createPingReportStatus(PingReportStatus ps, 
	String uid, String versionInfo, String protocolInfo,
   TExtraInfo[] info) {
   ps.setAuthorizationId(uid);
   ps.setVersionInfo(versionInfo);
   ps.setMethodInfo(protocolInfo);
   if(info != null) {
    for(int i = 0; i < info.length; i++) {
     ps.setKey(info[i].getKey(),info[i].getValue());
    }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getValueForOp
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String getValueForOp(String op, Vector vec) throws Exception {
   StringTokenizer tk = new StringTokenizer(op,",");
   StringBuffer value = new StringBuffer();
   while (tk.hasMoreTokens()) {
     String vv =  tk.nextToken();
     if(!vec.contains(vv)) {
       vec.addElement(vv); 
     }
   }
   for(int i = 0; i < vec.size(); i++) {
     String ss = (String) vec.elementAt(i); 
     if(ss.equalsIgnoreCase("all")) return "all"; 
     if(ss.equalsIgnoreCase("ping")) { 
       value.append("ping,");
     }
     if(ss.equalsIgnoreCase("get")) { 
       //if(getSourceFile.equals("")) {
         //System.out.println("\nPlease provide -getsource for the \"get\" operation");
         //System.exit(1);
       //}
       value.append("get,");
     }
     if(ss.equalsIgnoreCase("put")) { 
       //if(putSourceFile.equals("") || remoteTarget.equals("")) {
         //System.out.println("\nPlease provide -localsource and -remotetarget for the \"put\" operation");
         //System.exit(1);
       //}
       value.append("put,");
     }
     if(ss.equalsIgnoreCase("bringonline")) { 
       //if(sourceFile.equals("")) {
         //System.out.println("\nPlease provide -source for \"bringonline\" operation");
         //System.exit(1);
       //}
       value.append("bringonline,");
     }
     if(ss.equalsIgnoreCase("gsiftp")) { 
       //if(gsiftpCopySourceFile.equals("") || copyTarget.equals("")) {
         //System.out.println
			//("\nPlease provide -gsiftpcopysource and -copytarget for \"copygsiftp\" operation");
         //System.exit(1);
       //}
       value.append("gsiftp,");
     }
     if(ss.equalsIgnoreCase("pull")) { 
       //if(copySourceFile.equals("") || copyTarget.equals("")) {
         //System.out.println
			//("\nPlease provide -copysource and -copytarget for \"copygsiftp\" operation");
         //System.exit(1);
       //}
       value.append("pull,");
     }
     if(ss.equalsIgnoreCase("push")) { 
       //if(copySourceFile.equals("") || copyTarget.equals("")) {
         //System.out.println
			//("\nPlease provide -copysource and -copytarget for \"copygsiftp\" operation");
         //System.exit(1);
       //}
       value.append("push,");
     }
     if(ss.equalsIgnoreCase("3rdcopy")) { 
       value.append("3rdcopy,");
     }
     if(ss.equalsIgnoreCase("reserve")) { 
       value.append("reserve,");
     }
     if(ss.equalsIgnoreCase("release")) { 
       value.append("release,");
     }
     if(ss.equalsIgnoreCase("getspacemeta")) { 
       value.append("getspacemeta,");
     }
     if(ss.equalsIgnoreCase("status")) { 
       value.append("status,");
     }
     if(ss.equalsIgnoreCase("mkdir")) { 
       //if(dirUrl.equals(""))  {
         //System.out.println ("\nPlease provide -dirurl");
         //System.exit(1);
       //}
       value.append("mkdir,");
     }
     if(ss.equalsIgnoreCase("srmrm")) { 
       //if(removeUrl.equals(""))  {
         //System.out.println ("\nPlease provide -removeurl");
         //System.exit(1);
       //}
       value.append("srmrm,");
     }
     if(ss.equalsIgnoreCase("rm")) { 
       //if(removeUrl.equals(""))  {
         //System.out.println ("\nPlease provide -removeurl");
         //System.exit(1);
       //}
       value.append("rm,");
     }
     if(ss.equalsIgnoreCase("rmdir")) { 
       //if(dirUrl.equals(""))  {
         //System.out.println ("\nPlease provide -dirurl");
         //System.exit(1);
       //}
       value.append("rmdir,");
     }
     if(ss.equalsIgnoreCase("mv")) { 
       //if(sourceFile.equals("") || moveTarget.equals(""))  {
         //System.out.println ("\nPlease provide -source and -mvtarget");
         //System.exit(1);
       //}
       value.append("mv,");
     }
     if(ss.equalsIgnoreCase("ls")) { 
       value.append("ls,");
     }
     if(ss.equalsIgnoreCase("reserveput")) { 
       value.append("reserveput,");
     }
     if(ss.equalsIgnoreCase("updatespace")) { 
       value.append("updatespace,");
     }
     if(ss.equalsIgnoreCase("releasespace")) { 
       value.append("releasespace,");
     }
     if(ss.equalsIgnoreCase("reservespace")) { 
       value.append("reservespace,");
     }
     if(ss.equalsIgnoreCase("browse")) { 
       //if(browseUrl.equals(""))  {
         //System.out.println ("\nPlease provide -browseurl");
         //System.exit(1);
       //}
       value.append("browse,");
     }
     if(ss.equalsIgnoreCase("putdone")) { 
       //if(sourceFile.equals(""))  {
         //System.out.println ("\nPlease provide -source");
         //System.exit(1);
       //}
       value.append("putdone,");
     }
     if(ss.equalsIgnoreCase("abortfiles")) { 
       //if(sourceFile.equals(""))  {
         //System.out.println ("\nPlease provide -source");
         //System.exit(1);
       //}
       value.append("abortfiles,");
     }
     if(ss.equalsIgnoreCase("gettransferprotocols")) { 
       value.append("gettransferprotocols,");
     }
     if(ss.equalsIgnoreCase("getrequesttokens")) { 
       value.append("getrequesttokens,");
     }
     if(ss.equalsIgnoreCase("getspacetokens")) { 
       value.append("getspacetokens,");
     }
   }
   String temp = value.toString();
   int length = temp.length();
   if(length == 0) { 
     throw new Exception ("Please provide valid operations " + vec);
   }
   return temp.substring(0,length-1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// findDisplayType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String findDisplayType(String testType) {

  String displayType = testType.substring(1);

  if(displayType.equalsIgnoreCase("srmtester")) {
     if(!onlyGsiFTPGet) {
       displayType = "SRM";
     }
     else {
       displayType = "GsiFTP";
     }
  }
  
  if(displayType.equalsIgnoreCase("srm")) {
     displayType = "SRM";
  }
  else if(displayType.equalsIgnoreCase("dcache")) {
     displayType = "SRM/dCACHE";
  }
  else if(displayType.equalsIgnoreCase("srmsrm")) {
     displayType = "SRM-SRM";
  }
  else if(displayType.equalsIgnoreCase("srmdcache")) {
     displayType = "SRM-SRM/dCache";
  }
  else if(displayType.equalsIgnoreCase("dcachesrm")) {
     displayType = "SRM/dCache-SRM";
  }
  return displayType;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//setLogFile
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void setLogFile(String log4jlocation, String logPath) throws IOException {

  String ttemp = System.getProperty("log4j.configuration");
  if(ttemp != null && !ttemp.equals("")) {
    log4jlocation = ttemp;
  }
  else {
    log4jlocation = "properties/log4j_srmtester.properties";
  }

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
        if(ref.startsWith("log4j.appender.SRMTEST.File")) {
           out.write("log4j.appender.SRMTEST.File="+detailedLogPath+"\n");
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

   PropertyConfigurator.configure(log4jlocation);

   ClassLoader cl = this.getClass().getClassLoader();

   try {
     Class c = cl.loadClass("SRMTester");
     logger = LogFactory.getLog(c.getName());
   }catch(ClassNotFoundException cnfe) {
      util.printMessage("ClassNotFoundException" + 
			cnfe.getMessage(),logger,pIntf);
   }

}   

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getRemoteWSDLObject
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private ISRM getRemoteWSDLObject(String servicePath, 
	StringBuffer proxyString)
      throws IOException, Exception
{
   ISRM srm = null;
   String host = "";
   int port = 0;
   String pathName = "";
   try {
     //ServiceContext context = new ServiceContext();
     MyGlobusURL sUrl = new MyGlobusURL(servicePath);
     host = sUrl.getHost();
     port = sUrl.getPort();
     pathName = sUrl.getPath();
     //System.out.println("host="+host+ " " + port + " " +pathName);
     servicePath = "httpg://"+host+":"+port+pathName;
     URL uu = null;
     SimpleProvider provider = new SimpleProvider();
     SimpleTargetedChain c = null;
     if(servicePath.startsWith("httpg")) {
       String protocols0 = System.getProperty("java.protocol.handler.pkgs");
       //System.out.println("protocol pkg handler: " + protocols0);
       //System.out.println(".....supporting: httpg");
       org.globus.net.protocol.httpg.Handler handler =
            new org.globus.net.protocol.httpg.Handler();
       try {
         uu = new URL("httpg", host, port, pathName, handler);
       }catch(Exception h) { System.out.print(" does not work");}
       //System.out.println("   contacting fixed host : " + uu.toString());
       c = new SimpleTargetedChain(new GSIHTTPSender());
       provider.deployTransport("httpg",c);
     } else if(servicePath.startsWith("https")) {
       //System.out.println(".....supporting: https");
       c = new SimpleTargetedChain(new HTTPSSender());
       provider.deployTransport("https",c);
       uu = new URL(servicePath);
     }else {
       //System.out.println(".....supporting: http");
       c = new SimpleTargetedChain(new HTTPSender());
       provider.deployTransport("http",c);
       uu = new URL(servicePath);
     }
     Util.registerTransport ();
     SRMServiceLocator service = new SRMServiceLocator (provider);
     util.printMessageHL("SRM-TESTER: Connecting to url " +
                  "httpg://"+host.trim()+":"+port+ pathName.trim(),
                        logger,pIntf);
     //System.out.println("service="+service);
     URL url = new URL
            ("httpg://"+host.trim()+":"+port+ pathName.trim());
     srm = service.getsrm(uu);
 
     if(srm == null) {
       throw new Exception ("SRM-TESTER: Could not connect to wsdlHandle " +
          "httpg://"+host.trim()+":"+port+pathName.trim());
     }
     X509Credential gCreds = null;
     if(!defaultProxy) {
       gCreds = new X509Credential(proxyFile);
     }
     else {
       gCreds = new X509Credential("/tmp/x509up_u"+MyConfigUtil.getUID2());
     }
  
     util.printMessage("\nSRM-TESTER: Issues=" +
        gCreds.getIssuer(), logger,pIntf);
     util.printMessage("SRM-TESTER: Subject=" +
        gCreds.getSubject(), logger,pIntf);
     util.printMessage("SRM-TESTER: Identity=" +
        gCreds.getIdentity(), logger,pIntf);
     util.printMessage("SRM-TESTER: timeleft=" +
        gCreds.getTimeLeft(), logger,pIntf);
     if(gCreds.getTimeLeft() == 0) {
       System.out.println("\nSRM-TESTER: Please renew your credentials");
          if(pIntf == null) {
            System.exit(1);
          }
          else {
            pIntf.setCompleted(true);
          }
     }
     credential = new GlobusGSSCredentialImpl
                        (gCreds, GSSCredential.INITIATE_AND_ACCEPT);
     ExtendedGSSCredential cred = (ExtendedGSSCredential) credential;
     byte[] bb = cred.export(ExtendedGSSCredential.IMPEXP_OPAQUE);
     proxyString.append(new String(bb));
 
     setSecurityProperties(url,srm);
   }catch(Exception e) {
      e.printStackTrace();
      throw new Exception ("Could not connect to wsdlHandle " +
          "httpg://"+host.trim()+":"+port+pathName.trim() + " "  +
        e.getMessage());
   }
   return srm;
}

private void setSecurityProperties(URL endpoint,ISRM srm) {
   //System.out.println("...Setting security properties");
   //System.out.println("url="+endpoint);
   //System.out.println("srm="+srm);
   if(endpoint.getProtocol().equals("httpg")) {
     org.apache.axis.client.Stub srm_stub =
             (org.apache.axis.client.Stub) srm;
     srm_stub._setProperty
            (org.globus.axis.transport.GSIHTTPTransport.GSI_AUTHORIZATION,
             org.globus.gsi.gssapi.auth.NoAuthorization.getInstance());
     srm_stub._setProperty
         (org.globus.axis.transport.GSIHTTPTransport.GSI_MODE,
          org.globus.axis.transport.GSIHTTPTransport.GSI_MODE_FULL_DELEG);
     srm_stub._setProperty
          (org.globus.axis.gsi.GSIConstants.GSI_CREDENTIALS,credential);
     srm_stub._setProperty
     (org.globus.gsi.GSIConstants.AUTHZ_REQUIRED_WITH_DELEGATION,
         Boolean.FALSE);
   }
}



//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForMvAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForMvAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("mvsource",new String(sourceFile));
    ht.put("mvtarget",new String(moveTarget));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForMkdirAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForMkdirAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    if(dirUrl == null || dirUrl.equals("")) {
      util.printMessageHL("Please provide the -dirurl",logger,pIntf);
      showUsage(false);
    }
    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(dirUrl));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForRmAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForRmAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    if(removeUrl == null || removeUrl.equals("")) {
      util.printMessage("Please provide the -removeurl",logger,pIntf);
      showUsage(false);
    }

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(removeUrl));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForLsAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForLsAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    if(browseUrl == null || browseUrl.equals("")) {
      util.printMessageHL("Please provide the browseurl",logger,pIntf);
      showUsage(false);
    }

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(browseUrl));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForRmdirAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForRmdirAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(dirUrl));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForCopyAdvancedGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForCopyAdvancedGsiFTP (SharedObjectLock sLock, 
	StringBuffer proxyString, String tempFileStorageType) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("copysource",new String(gsiftpCopySourceFile));
    ht.put("copysourcesite",new String(""));
    Random generator = new Random();
    double r = generator.nextDouble();
    ht.put("copytarget",new String(copyTarget+".gsiftp"+"."+r));
    ht.put("overwrite",new Boolean(copyoverwrite));
    ht.put("spacetoken",new String(""));
    if(!tempFileStorageType.equals("")) {
      if(tempFileStorageType.equalsIgnoreCase("PERMANENT")) {
        TFileStorageType fs = TFileStorageType.PERMANENT;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("VOLATILE")) {
        TFileStorageType fs = TFileStorageType.VOLATILE;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("DURABLE")) {
        TFileStorageType fs = TFileStorageType.DURABLE;
        ht.put("filestoragetype",fs);
      } 
    }
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForCopyAdvancedPull
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForCopyAdvancedPull (SharedObjectLock sLock, 
	StringBuffer proxyString, String localCopySourceFile,
	String localCopyTarget, String localCopySourceSiteName,
	String tempFileStorageType) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("copysource",new String(localCopySourceFile));
    ht.put("copysourcesite",new String(localCopySourceSiteName));
    //ht.put("copytarget",new String(copyTarget+"."+localCopySourceSiteName));
    Random generator = new Random();
    double r = generator.nextDouble();
    ht.put("copytarget",new String(localCopyTarget+"."+r));
    ht.put("overwrite",new Boolean(overwrite));
    ht.put("spacetoken",new String(""));
    if(!tempFileStorageType.equals("")) {
      if(tempFileStorageType.equalsIgnoreCase("PERMANENT")) {
        TFileStorageType fs = TFileStorageType.PERMANENT;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("VOLATILE")) {
        TFileStorageType fs = TFileStorageType.VOLATILE;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("DURABLE")) {
        TFileStorageType fs = TFileStorageType.DURABLE;
        ht.put("filestoragetype",fs);
      } 
    }
    ht.put("slock",sLock);
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForCopyAdvancedPush
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForCopyAdvancedPush (SharedObjectLock sLock, 
	StringBuffer proxyString, String localCopySourceFile,
	String localCopyTarget, String localCopySourceSiteName,
	String tempFileStorageType) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("copysourcesite",new String(localCopySourceSiteName));
    ht.put("copysource",new String(localCopySourceFile));
    //ht.put("copytarget",new String(copyTarget+"."+localCopySourceSiteName));
    Random generator = new Random();
    double r = generator.nextDouble();
    ht.put("copytarget",new String(localCopyTarget+"."+r));
    ht.put("overwrite",new Boolean(overwrite));
    ht.put("spacetoken",new String(""));
    if(!tempFileStorageType.equals("")) {
      if(tempFileStorageType.equalsIgnoreCase("PERMANENT")) {
        TFileStorageType fs = TFileStorageType.PERMANENT;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("VOLATILE")) {
        TFileStorageType fs = TFileStorageType.VOLATILE;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("DURABLE")) {
        TFileStorageType fs = TFileStorageType.DURABLE;
        ht.put("filestoragetype",fs);
      } 
    }
    ht.put("slock",sLock);
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForGetAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForGetAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString, String tempFileStorageType) {

    Hashtable ht = new Hashtable();
    if(!directGsiFTP) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(getSourceFile));
    ht.put("plainget",new Boolean(true));
    ht.put("spacetoken",new String(spaceToken));
    if(!tempFileStorageType.equals("")) {
      if(tempFileStorageType.equalsIgnoreCase("PERMANENT")) {
        TFileStorageType fs = TFileStorageType.PERMANENT;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("VOLATILE")) {
        TFileStorageType fs = TFileStorageType.VOLATILE;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("DURABLE")) {
        TFileStorageType fs = TFileStorageType.DURABLE;
        ht.put("filestoragetype",fs);
      } 
    }
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForGetTransferProtocols
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForGetTransferProtocols (SharedObjectLock sLock, 
	StringBuffer proxyString, String displayName, String exceptionMessage) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("slock",sLock);
    ht.put("displayname",displayName);
    ht.put("exceptionmessage",exceptionMessage);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForPing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForPing (SharedObjectLock sLock, 
	StringBuffer proxyString, String displayName) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("slock",sLock);
    ht.put("displayname",displayName);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForGetSpaceTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForGetSpaceTokens (SharedObjectLock sLock, 
	StringBuffer proxyString, String displayName, 
	String exceptionMessage) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("slock",sLock);
    ht.put("displayname",displayName);
    ht.put("exceptionmessage",exceptionMessage);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForGetRequestTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForGetRequestTokens (SharedObjectLock sLock, 
	StringBuffer proxyString, String displayName,String exceptionMessage) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("slock",sLock);
    ht.put("displayname",displayName);
    ht.put("exceptionmessage",exceptionMessage);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForBringOnLineAdvnaced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForBringOnLineAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    Hashtable ht = new Hashtable();
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",new String(sourceFile));
    ht.put("plainget",new Boolean(true));
    ht.put("spacetoken",new String(spaceToken));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForPutDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForPutDone (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    Hashtable ht = new Hashtable();
    if(requestToken == null || requestToken.equals("")) {
      util.printMessage
		("\nCannot continue with this test, since request token is empty",logger,pIntf);
      return ht;
    }
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",sourceFile);
    ht.put("requesttoken",new String(requestToken));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForAbortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForAbortFiles (SharedObjectLock sLock, 
	StringBuffer proxyString) {

    Hashtable ht = new Hashtable();
    if(requestToken == null || requestToken.equals("")) {
      util.printMessage
		("\nCannot continue with this test, since request token is empty",logger,pIntf);
      return ht;
    }
    if(srm != null) {
      ht.put("ISRM",srm);
    }
    ht.put("proxystring",proxyString);
    ht.put("source",sourceFile);
    ht.put("requesttoken",new String(requestToken));
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// prepareForPutAdvanced
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private Hashtable prepareForPutAdvanced (SharedObjectLock sLock, 
	StringBuffer proxyString, String tempFileStorageType) {

    Hashtable ht = new Hashtable();
    if(!directGsiFTP) {
      if(srm != null) {
        ht.put("ISRM",srm);
      }
    }
    ht.put("proxystring",proxyString);
    //ht.put("source",new String(""));
    ht.put("source",new String(putSourceFile));
    ht.put("remote",remoteTarget);
    ht.put("plainput",new Boolean(true));
    ht.put("spacetoken",new String(spaceToken));
    ht.put("reservedspace",new Boolean(false));
    ht.put("overwrite",new Boolean(overwrite));
    if(!tempFileStorageType.equals("")) {
      if(tempFileStorageType.equalsIgnoreCase("PERMANENT")) {
        TFileStorageType fs = TFileStorageType.PERMANENT;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("VOLATILE")) {
        TFileStorageType fs = TFileStorageType.VOLATILE;
        ht.put("filestoragetype",fs);
      } 
      else if(tempFileStorageType.equalsIgnoreCase("DURABLE")) {
        TFileStorageType fs = TFileStorageType.DURABLE;
        ht.put("filestoragetype",fs);
      } 
    }
    ht.put("slock",sLock);
    return ht;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// stopCurrentThread
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void stopCurrentThread() {
  try {

    util.printMessageHL("\nStopping current thread...\n", logger, pIntf);
    if(tcb != null) {
      tcb.interrupt();
    }
    if(pIntf != null) {
      pIntf.setCompleted(true);
    }
  }catch(Exception e) {
    util.printMessage(e.getMessage(),logger,pIntf);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForPut (StringBuffer proxyString, 
	int testCondition) {

    doTestCallForPut(proxyString, testCondition,"");
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForPut (StringBuffer proxyString) {

  for(int i = 0; i < 14; i++) {
    doTestCallForPut(proxyString, i,"");
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForAbortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForAbortFiles (StringBuffer proxyString) {
  doTestCallForAbortFiles(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForPutDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForPutDone (StringBuffer proxyString) {
  doTestCallForPutDone(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForAbortFiles
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForAbortFiles (StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"abortfiles");
    Hashtable ht = prepareForAbortFiles(sLock,proxyString);
    if(ht.size() == 0) {
      //some parameters may be missing, and this test is not valid here.
      return;
    }
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForPutDone
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForPutDone (StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"putdone");
    Hashtable ht = prepareForPutDone(sLock,proxyString);
    if(ht.size() == 0) {
      //some parameters may be missing, and this test is not valid here.
      return;
    }
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForPut (StringBuffer proxyString, String tempFileStorageType) {
  doTestCallForPut(proxyString, -1,tempFileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForPut
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForPut (StringBuffer proxyString, int cond, 
	String tempFileStorageType) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"put");
    Hashtable ht = prepareForPutAdvanced(sLock,proxyString,tempFileStorageType);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForGet(StringBuffer proxyString, 
	int testCondition) {

    doTestCallForGet(proxyString, testCondition,"");
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForGet (StringBuffer proxyString) {

  for(int i = 0; i < 10; i++) {
    doTestCallForGet(proxyString, i,"");
  }
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForGet (StringBuffer proxyString, String tempFileStorageType) {
   doTestCallForGet(proxyString, -1, tempFileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForGet
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForGet (StringBuffer proxyString, int cond, 
	String tempFileStorageType) {
  SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
  tcb = new ThreadCallBack(this,"get");
  Hashtable ht = prepareForGetAdvanced(sLock,proxyString,tempFileStorageType);
  ht.put("testcondition",new Integer(cond)); 
  tcb.setParams_2(ht);
  tcb.start();
  sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callTestPing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private String callTestPing(String displayName) {

   startWebScript(displayName);
   proxyString = new StringBuffer();
   if(directGsiFTP) {
     return "";
   }
   try {
     srm = connectToSRM(proxyString,false,"");
     doTestCallForPing(proxyString,displayName);
     return "";
   }catch(Exception e){
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("Exception : " + e.getMessage() +"\n",logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
      //ReportRequestStatus rrs = new ReportRequestStatus();
      //rrs.setLinkName("pingexception");
	  PingReportStatus ps = new PingReportStatus ();
      createPingReportStatus(ps,uid,"","",null);
      ps.setLinkName("ping");
      ps.setActualResult("Exception");
      ps.setMatchingInfo("No. expecting SRM_SUCCESS");
      try {
        util.webScript(publishUrlStr,
	    displayType,"SendingPING","SrmPing",
		ps, "", timeStamp, fullTimeStamp, startTimeStamp,
        operatorTimeStamp,
	    servicePath,noPublish,
	    siteName,numOperations,displayName,"","Sending",dailyTest, 
		numSites,localPublish,bs,bsa);
        util.webScript(publishUrlStr,displayType,
	    "Ping","Exception",ps,e.getMessage(),
	    timeStamp,fullTimeStamp, startTimeStamp, 
		operatorTimeStamp, servicePath,noPublish,
		siteName,numOperations,displayName,"srmPing","Exception",
		dailyTest, numSites,localPublish,bs,bsa);
        util.printMessageHException("\n"+e.toString(),logger,pIntf);
        return e.getMessage();
      }catch(Exception we) { we.printStackTrace(); return we.getMessage();}
     }
  }
  return "";
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callTestGetSpaceTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void callTestGetSpaceTokens(String displayName,
	String exceptionMessage) {

   Date d = new Date();
   String operatorTimeStamp = d.toString();

   proxyString = new StringBuffer();
   try {
     //srm = connectToSRM(proxyString,false,"");
     doTestCallForGetSpaceTokens(proxyString,displayName,exceptionMessage);
   }catch(Exception e){
     util.printMessageHException("Exception : " + e.getMessage() +"\n",logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
	  rrs.setActualResult("Exception");
	  rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
	  rrs.setLinkName("getspacetokens_0");
	  try { 
		util.webScript(publishUrlStr,
		  displayType,"SendingGetSpaceTokens","GetSpaceTokens", rrs,
		  "", timeStamp,fullTimeStamp, startTimeStamp,  
	      operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmGetSpaceTokens","",
		  "Sending", dailyTest, numSites,localPublish,bs,bsa);
		util.webScript(publishUrlStr,displayType,"GetSpaceTokens","Exception",
		  rrs, e.getMessage(),
		  timeStamp,fullTimeStamp, startTimeStamp, operatorTimeStamp,
	      servicePath,noPublish,
		  siteName,numOperations,"srmGetSpaceTokens","srmGetSpaceTokens",
	      "Exception" ,dailyTest, numSites,localPublish,bs,bsa);
	  }catch(Exception we) { we.printStackTrace(); }
	 }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callTestGetRequestTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void callTestGetRequestTokens(String displayName,
		String exceptionMessage) {

   proxyString = new StringBuffer();
   try {
     //srm = connectToSRM(proxyString,false,"");
     doTestCallForGetRequestTokens(proxyString,displayName,
		exceptionMessage);
   }catch(Exception e){
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("Exception : " + e.getMessage() +"\n",logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
	  rrs.setActualResult("Exception");
	  rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
	  rrs.setLinkName("getrequesttokens_0");
	  try { 
		util.webScript(publishUrlStr,
		  displayType,"SendingGetRequestTokens","GetRequestTokens", rrs,
		  "", timeStamp,fullTimeStamp, startTimeStamp,  
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmGetRequestTokens","",
		  "Sending", dailyTest, numSites,localPublish,bs,bsa);
		util.webScript(publishUrlStr,displayType,"GetRequestTokens","Exception",
		  rrs, e.getMessage(),
		  timeStamp,fullTimeStamp, startTimeStamp, 
	      operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"srmGetRequestTokens","",
		  "Exception" ,dailyTest, numSites,localPublish,bs,bsa);
	  }catch(Exception we) { we.printStackTrace(); }
	 }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// callTestGetTransferProtocols
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void callTestGetTransferProtocols(String displayName,
	String exceptionMessage) {

   proxyString = new StringBuffer();
   try {
     //srm = connectToSRM(proxyString,false,"");
     isGetTransferProtocolsSuccess.setTried(true);
     doTestCallForGetTransferProtocols(proxyString,displayName,
		exceptionMessage);
   }catch(Exception e){
     isGetTransferProtocolsSuccess.setTried(true);
     Date d = new Date();
     String operatorTimeStamp = d.toString();
     util.printMessageHException("Exception : " + e.getMessage() +"\n",logger,pIntf);
     e.printStackTrace();
     if(e.getMessage() != null &&
		!e.getMessage().equalsIgnoreCase("ignore this.")) {
      isGetTransferProtocolsSuccess.setExplanation("Exception");
	  //ReportRequestStatus rrs = util.createReportRequestStatus("","","");
	  PingReportStatus rrs = new PingReportStatus ();
      createPingReportStatus(rrs,uid,"","",null);
	  rrs.setActualResult("Exception");
	  rrs.setMatchingInfo("No. expecting SRM_SUCCESS");
	  rrs.setLinkName("gettransferprotocols_0");
	  try { 
		util.webScript(publishUrlStr,
		  displayType,"SendingGetTransferProtocols","GetTransferProtocols", 
	      rrs, "", timeStamp,fullTimeStamp,startTimeStamp, 
          operatorTimeStamp,
	      servicePath,noPublish,
		  siteName,numOperations,"GetTransferProtocols","",
	      "Sending", dailyTest, numSites,localPublish,bs,bsa);
		util.webScript(publishUrlStr,displayType,"GetTransferProtocols",
		  "Exception", rrs, e.getMessage(),
		  timeStamp,fullTimeStamp, startTimeStamp, 
		  operatorTimeStamp, servicePath,noPublish,
		  siteName,numOperations,"GetTransferProtocols",
		  "srmGetTransferProtocols",
	      "Exception" ,dailyTest, numSites,localPublish,bs,bsa);
	  }catch(Exception we) { we.printStackTrace(); }
	 }
   }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForGetSpaceTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForGetSpaceTokens(StringBuffer proxyString, 
	String displayName, String exceptionMessage) { 

  SharedObjectLock sLock = null;
  if(typeString.equalsIgnoreCase("getspacetokens")) {
    sLock = new SharedObjectLock(1,logger,pIntf);
  }
  else {
    sLock = new SharedObjectLock(1,logger,null);
  }
  tcb = new ThreadCallBack(this,"getspacetokens");
  Hashtable ht = prepareForGetSpaceTokens(sLock,proxyString,
		displayName,exceptionMessage);
  tcb.setParams_2(ht);
  tcb.start();
  sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForGetRequestTokens
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForGetRequestTokens(StringBuffer proxyString, 
	String displayName, String exceptionMessage) { 

  SharedObjectLock sLock = null;
  if(typeString.equalsIgnoreCase("getrequesttokens")) {
    sLock = new SharedObjectLock(1,logger,pIntf);
  }
  else {
    sLock = new SharedObjectLock(1,logger,null);
  }
  tcb = new ThreadCallBack(this,"getrequesttokens");
  Hashtable ht = prepareForGetRequestTokens(sLock,proxyString,
		displayName,exceptionMessage);
  tcb.setParams_2(ht);
  tcb.start();
  sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForGetTransferProtocols
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForGetTransferProtocols(StringBuffer proxyString, 
	String displayName, String exceptionMessage) { 

  SharedObjectLock sLock = null;
  if(typeString.equalsIgnoreCase("gettransferprotocols")) {
    sLock = new SharedObjectLock(1,logger,pIntf);
  }
  else {
    sLock = new SharedObjectLock(1,logger,null);
  }
  tcb = new ThreadCallBack(this,"gettransferprotocols");
  Hashtable ht = prepareForGetTransferProtocols(sLock,proxyString,
		displayName,exceptionMessage);
  tcb.setParams_2(ht);
  tcb.start();
  sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForPing
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForPing(StringBuffer proxyString, 
	String displayName) { 

  SharedObjectLock sLock = null;
  if(typeString.equalsIgnoreCase("ping")) {
    sLock = new SharedObjectLock(1,logger,pIntf);
  }
  else {
    sLock = new SharedObjectLock(1,logger,null);
  }
  tcb = new ThreadCallBack(this,"ping");
  Hashtable ht = prepareForPing(sLock,proxyString,displayName);
  tcb.setParams_2(ht);
  tcb.start();
  sLock.get();
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForBringOnLine
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForBringOnLine (StringBuffer proxyString) {
  doTestCallForBringOnline(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForBringOnLine
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForBringOnLine (StringBuffer proxyString) {
 for(int i = 0; i < 10; i++) {
  doTestCallForBringOnline(proxyString, i);
 }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForBringOnLine
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForBringOnline(StringBuffer proxyString, int cond) {

    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"bringonline");
    Hashtable ht = prepareForBringOnLineAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForSrmRm
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForSrmRm (StringBuffer proxyString) {
    doTestCallForSrmRm(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForSrmRm
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForSrmRm (StringBuffer proxyString) {
  for(int i = 0; i < 2; i++) {
    doTestCallForSrmRm(proxyString, i);
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForSrmRm
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForSrmRm(StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"srmrm");
    Hashtable ht = prepareForRmAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForRmdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForRmdir (StringBuffer proxyString) {
   doTestCallForRmdir(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForRmdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForRmdir (StringBuffer proxyString) {
 for(int i = 0; i < 2; i++) {
   doTestCallForRmdir(proxyString, i);
 } 
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForRmdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForRmdir (StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"rmdir");
    Hashtable ht = prepareForRmdirAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForMv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForMv (StringBuffer proxyString) {
  doTestCallForMv(proxyString, -1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForMv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForMv (StringBuffer proxyString) {
  doTestCallForMv(proxyString, 0);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForMv
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForMv(StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"mv");
    Hashtable ht = prepareForMvAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForSrmLs
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForSrmLs (StringBuffer proxyString) {
   doTestCallForSrmLs(proxyString, 0);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForSrmLs
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForSrmLs (StringBuffer proxyString) {
 for(int i = 0; i < 8; i++) {
   doTestCallForSrmLs(proxyString, i);
 }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForSrmLs
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForSrmLs(StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"ls");
    Hashtable ht = prepareForLsAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForMkdir (StringBuffer proxyString) {
    doTestCallForMkdir(proxyString,-1);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForMkdir (StringBuffer proxyString) {
    doTestCallForMkdir(proxyString,0);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForMkdir
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


private void doTestCallForMkdir(StringBuffer proxyString, int cond) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"mkdir");
    Hashtable ht = prepareForMkdirAdvanced(sLock,proxyString);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForCopyGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForCopyGsiFTP (StringBuffer proxyString) {

 for(int i = 0; i < 9; i++) {
  doTestCallForCopyGsiFTP(proxyString, i,"");
 }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForCopyPull
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForCopyPull (StringBuffer proxyString,
		String localCopySourceFile, String localCopyTarget,
		String localCopySourceSiteName) {

 for(int i = 0; i < 9; i++) {
  doTestCallForCopyPull(proxyString, i, localCopySourceFile,
		localCopyTarget, localCopySourceSiteName,"");
 }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doDetailedTestCallForCopyPush
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doDetailedTestCallForCopyPush (StringBuffer proxyString, 
	String localCopySourceFile, String localCopySourceSiteName, String siteName) {

 for(int i = 0; i < 9; i++) {
  doTestCallForCopyPush(proxyString, i, localCopySourceFile,
		localCopySourceSiteName, siteName,"");
 }

}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForCopyGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForCopyGsiFTP (StringBuffer proxyString,
	String tempFileStorageType) {
  doTestCallForCopyGsiFTP(proxyString, -1,tempFileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForCopyPull
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForCopyPull (StringBuffer proxyString, 
		String localCopySourceFile, String localCopySourceSiteName, 
		String siteName,String tempFileStorageType) {
  doTestCallForCopyPull(proxyString, -1,localCopySourceFile,
		   localCopySourceSiteName,siteName,tempFileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doBasicTestCallForCopyPush
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doBasicTestCallForCopyPush (StringBuffer proxyString,
		String localCopySourceFile, String localCopySourceSiteName,
		String siteName,String tempFileStorageType) {
  doTestCallForCopyPush(proxyString, -1, localCopySourceFile,
		    localCopySourceSiteName,siteName,tempFileStorageType);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForCopyGsiFTP
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForCopyGsiFTP(StringBuffer proxyString, int cond,
		String tempFileStorageType) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"gsiftp");
    Hashtable ht = prepareForCopyAdvancedGsiFTP(sLock,proxyString,tempFileStorageType);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForCopyPull
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForCopyPull(StringBuffer proxyString, int cond, 
	   String localCopySourceFile, String localCopySourceSiteName,
	   String siteName, String tempFileStorageType) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"pull");
    Hashtable ht = prepareForCopyAdvancedPull(sLock,proxyString,
		localCopySourceFile,localCopySourceSiteName,siteName,tempFileStorageType);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// doTestCallForCopyPush
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private void doTestCallForCopyPush(StringBuffer proxyString, int cond,
       String localCopySourceFile,String localCopyTarget, String siteName,
	   String tempFileStorageType) {
    SharedObjectLock sLock = new SharedObjectLock(1,logger,pIntf);
    tcb = new ThreadCallBack(this,"push");
    Hashtable ht = prepareForCopyAdvancedPush(sLock,proxyString,
		localCopySourceFile,localCopyTarget,siteName,tempFileStorageType);
    ht.put("testcondition",new Integer(cond)); 
    tcb.setParams_2(ht);
    tcb.start();
    sLock.get();
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
       throw new IOException("SRMTester.parseFile: Can't read from `" +
          ex.getMessage() + "'");
   } catch (java.util.zip.DataFormatException de) {
       throw new java.util.zip.DataFormatException
            ("SRMTester.parseFile: Invalid format " +
            de.getMessage() + "'");
   }
   return sys_config;
}

 


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// main
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void main(String[] args) {
  boolean driverOn = false;
  for(int i = 0; i < args.length; i++) {
     if(args[i].equalsIgnoreCase("-drive") || args[i].equalsIgnoreCase("-testsites")) {
        driverOn=true;
     }
  }
  SRMTester tester = new SRMTester(args,true, null,null);
  if(!driverOn) {
    tester.startTester();
  }
}

}

