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

package gov.lbl.srm.client.util;

import java.io.*; 
import java.util.*; 
import javax.swing.*; 

import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.main.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;

import org.globus.util.ConfigUtil;
import org.globus.gsi.GlobusCredential;
import org.globus.gsi.CertUtil;
import org.globus.gsi.GSIConstants;
import org.globus.gsi.OpenSSLKey;
import org.globus.gsi.gssapi.*;
import org.globus.gsi.bc.*;
import java.security.interfaces.*;
import java.security.PrivateKey;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.interfaces.*;
import java.security.cert.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xml.serialize.*;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Util
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class Util implements threadIntf {
  private ThreadCallBack cb = new ThreadCallBack(this);
  private Properties properties;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getProxyType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static int getProxyType(Properties properties) 
	throws SRMClientException {

   boolean useProxy=true;
   String proxyPath = properties.getProperty("proxy-file");
   String ukey="";
   String ucert="";

   try {
     if(!proxyPath.startsWith("Enter")) { 
       ;
	 }
     else {
       ukey = properties.getProperty("user-key");
       ucert = properties.getProperty("user-cert");

       if(!ukey.startsWith("Enter") && (!ucert.startsWith("Enter"))) {
         useProxy=false;
       }
       else {
         //proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        try {
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) { 
          System.out.println("\nSRM-CLIENT: Exception from client="+ue.getMessage());
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
       }
     }
   }catch(Exception e) {; }

   try {
    if(useProxy) {
     if( proxyPath == null ||  proxyPath.length() == 0) {
          //proxyPath  = "/tmp/x509up_u"+ConfigUtil.getUID();
        try {
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) { 
          System.out.println("\nSRM-CLIENT: Exception from client="+ue.getMessage());
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
     }
     GlobusCredential creds = new GlobusCredential(proxyPath); 
     return creds.getProxyType();
     //10 full proxy
     // 11 limited proxy
   }
  }catch(IOException ioe) {
     throw new SRMClientException(ioe.getMessage());
  }catch(Exception e) {
     throw new SRMClientException(e.getMessage());
  }
  return -1;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getProxyType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static int getProxyType(String proxyPath, boolean useProxy) 
	throws SRMClientException {
  try {
   if(useProxy) {
     if( proxyPath == null ||  proxyPath.length() == 0) {
          //proxyPath  = "/tmp/x509up_u"+ConfigUtil.getUID();
        try {
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) { 
          System.out.println("\nSRM-CLIENT: Exception from client="+ue.getMessage());
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
     }
     GlobusCredential creds = new GlobusCredential(proxyPath); 
     return creds.getProxyType();
     //10 full proxy
     // 11 limited proxy
   }
  }catch(IOException ioe) {
     throw new SRMClientException(ioe.getMessage());
  }catch(Exception e) {
     throw new SRMClientException(e.getMessage());
  }
   return -1;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getCredential
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static GSSCredential getCredential(String proxyPath, String userKey,
	boolean useProxy, String password) throws SRMClientException { 

   GSSCredential mycred = null;
   try {
     if(!useProxy) {
       GlobusCredential gCreds = null;
       String userCert = proxyPath;
       X509Certificate[] certs = Util.loadCerts(userCert);
       OpenSSLKey k = new BouncyCastleOpenSSLKey(userKey);
       if(k.isEncrypted()) {
          k.decrypt(password);
          PrivateKey key = k.getPrivateKey();
          gCreds = new GlobusCredential(key, certs);
          mycred = new GlobusGSSCredentialImpl
               (gCreds, GSSCredential.INITIATE_AND_ACCEPT);
        }
     }
     else {
       if( proxyPath == null ||  proxyPath.length() == 0) {
          //proxyPath  = "/tmp/x509up_u"+ConfigUtil.getUID();
        try {
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) { 
          System.out.println("\nSRM-CLIENT: Exception from client="+
                ue.getMessage());
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
       }
       File f = new File(proxyPath);
       byte[] data = new byte[(int)f.length()];
       FileInputStream in = new FileInputStream (f);
       //read in the credential data
       in.read(data);
       in.close();
       ExtendedGSSManager manager =
         (ExtendedGSSManager) ExtendedGSSManager.getInstance();
       mycred = manager.createCredential(data,
                    ExtendedGSSCredential.IMPEXP_OPAQUE,
                    GSSCredential.DEFAULT_LIFETIME,
                    null,
                    GSSCredential.INITIATE_AND_ACCEPT);
      }
    }catch(FileNotFoundException fne) {
      throw new SRMClientException ("Proxy file " + proxyPath +
                " does not exist. Please initiate proxy first using the menu"+
                " Tools->init.");
    }catch(Exception e) {
      throw new SRMClientException (e.getMessage());
    }
    return mycred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static GSSCredential createProxy (XMLParseConfig pConfig, Properties properties,
	String _password, java.util.logging.Logger _theLogger, boolean silent, 
    boolean useLog,
	org.apache.commons.logging.Log logger, PrintIntf pIntf, boolean _debug) 
		throws SRMClientException {

 util.printMessage("Creating proxy now ...",logger,silent);
 Vector inputVec = new Vector ();
 util.printEventLog(_theLogger,"CreateProxy",inputVec,silent,useLog);
 try {
  String userCert = pConfig.getUserCert();
  String userKey = pConfig.getUserKey();
  String proxyFile =  pConfig.getProxyFile();
  gov.lbl.srm.client.util.Util.createProxy
        (userCert, userKey, proxyFile, _password);
 }catch(Exception e) {
     inputVec = new Vector ();
     inputVec.addElement("Exception="+e.getMessage());
     util.printEventLog(_theLogger,"CreateProxy",inputVec,silent,useLog);
     util.printMessage("Exception="+e.getMessage(),logger,silent);
     //ShowException.logDebugMessage(logger,e);
     throw new SRMClientException(e.getMessage());
 }

 GSSCredential mycred = gov.lbl.srm.client.util.Util.getCredential(properties,
	 _password,_theLogger,silent,useLog,logger,pIntf,_debug);
 return mycred;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createProxy
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static void createProxy(String userCert,
	String userKey, String proxyFile, String passwd) 
	throws SRMClientException {

 try {
    BouncyCastleCertProcessingFactory bc =
     BouncyCastleCertProcessingFactory.getDefault();
    X509Certificate[] certs = new X509Certificate[1];
    certs[0] = CertUtil.loadCertificate(userCert);
    PrivateKey key = null;
    OpenSSLKey k = new BouncyCastleOpenSSLKey(userKey);

    if(k.isEncrypted()) {
      k.decrypt(passwd);
      passwd = null;
      key = k.getPrivateKey();
      GlobusCredential cred =
        bc.createCredential(certs,(RSAPrivateKey)key, 512, 12*60*60,
        //GSIConstants.DELEGATION_FULL);
        GSIConstants.GSI_3_IMPERSONATION_PROXY);
      File f = new File(proxyFile);
      if(f.exists()) {
          File oldfile = new File(proxyFile+".old");
          StringBuffer buffer = new StringBuffer();
          try {
            InputStream in = new FileInputStream(proxyFile);
            BufferedReader bin =
                new BufferedReader(new InputStreamReader(in));
            OutputStream out =
                new FileOutputStream(proxyFile+".old");
            BufferedWriter bos =
                new BufferedWriter(new OutputStreamWriter(out));
            org.globus.util.Util.setFilePermissions
		(oldfile.getAbsolutePath(),600);
            String str = "";
            while((str = bin.readLine()) != null) {
              buffer.append(str+"\n");
            }
            bos.write(buffer.toString().trim());
            bos.close();
            out.close();
            in.close();
            bin.close();
        }catch(Exception e) {
          throw e;
        }
      }
      cred.save(new FileOutputStream(proxyFile));
    } 
    else {
     new SRMClientException(
           "User supplied key is not encrypted, key should be "+
           "encrypted in the form .pem");
    }
  }catch(Exception e) { 
     if(e.getMessage().equals("pad block corrupted")) {
        new SRMClientException
         ("ERROR: Couldn't read user key: Bad passphrase");
     }
     new SRMClientException(e.getMessage());
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  isRenewProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean isRenewProxy(String _password) {
   if(_password != null && _password.trim().length() > 0)
     return true;
   return false;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkTimeLeft
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static GSSCredential checkTimeLeft (XMLParseConfig pConfig,
    Properties properties,
	String _password,
	java.util.logging.Logger _theLogger, boolean silent,
    boolean useLog,
    org.apache.commons.logging.Log logger, PrintIntf pIntf,
	boolean _debug) throws SRMClientException {

  GSSCredential mycred = gov.lbl.srm.client.util.Util.getCredential(
		properties,_password,_theLogger, silent,useLog,logger,pIntf,_debug);
  Vector inputVec = new Vector ();
  try {
    int remainingLifeTime = mycred.getRemainingLifetime();
    if(remainingLifeTime == 0) {
       inputVec.addElement("User credential expired, ");
       inputVec.addElement("please renew your credentials");
       util.printEventLog(_theLogger,"CheckTimeLeft",inputVec,silent,useLog);
        util.printMessage
         ("User Credential expired, " +
          "please renew" +
           " your credentials",logger,silent);
        SRMClientException ee =  new SRMClientException
        ("User Credential expired, please renew your credentials");
        Throwable tt =  new Throwable
        ("User Credential expired, please renew your credentials");
        ee.initCause(tt);
        util.printHException(ee,pIntf);
        throw ee;
    }

    if(remainingLifeTime <= 1800) { //if <= 30 minutes
      if(gov.lbl.srm.client.util.Util.isRenewProxy(_password)) {
        try {
          mycred = gov.lbl.srm.client.util.Util.createProxy(pConfig,properties,_password,
			_theLogger,silent,useLog,logger, pIntf,_debug);
        }catch(SRMClientException srme) {
             inputVec = new Vector ();
             inputVec.addElement("Exception="+srme.getMessage());
             util.printEventLog(_theLogger,"CheckTimeLeft",inputVec,silent,useLog);
             if(!silent) {
             System.out.println("Exception="+srme.getMessage());
             }
             //ShowException.logDebugMessage(logger,srme);
             util.printHException(srme,pIntf);
             throw srme;
        }
      }
      else {
         inputVec = new Vector ();
         inputVec.addElement("Your proxy has only " + remainingLifeTime);
         inputVec.addElement(" second left. Please renew your proxy.");
         util.printEventLog(_theLogger,"CheckTimeLeft",inputVec,silent,useLog);
         System.out.println("Your proxy has only " + remainingLifeTime);
         System.out.println(" second left. Please renew your proxy.");
         ShowException.showMessageDialog(null, "Your proxy has only " +
        remainingLifeTime + " second left.\n" +
        "Please renew your proxy.");
      }
    }
      }catch(Exception e) {
      inputVec = new Vector ();
      inputVec.addElement("Exception="+e.getMessage());
      util.printEventLog(_theLogger,"CheckTimeLeft",inputVec,silent,useLog);
      if(!silent) {
      System.out.println("Exception="+e.getMessage());
      }
      //ShowException.logDebugMessage(logger,e);
      util.printHException(e,pIntf);
      throw new SRMClientException (e.getMessage());
   }

    return mycred;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getCredential
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static GSSCredential getCredential(Properties properties, 
	String _password, java.util.logging.Logger _theLogger, 
	boolean silent, boolean useLog, org.apache.commons.logging.Log logger, 
	PrintIntf pIntf, boolean _debug) throws SRMClientException {

   boolean useProxy=true;
   String ukey="";
   String ucert="";
   String proxyPath = properties.getProperty("proxy-file");
   GSSCredential mycred = null;
   Vector inputVec = new Vector ();

   try {
     if(!proxyPath.startsWith("Enter")) {
       inputVec = new Vector ();
       inputVec.addElement("Get credential for proxypath="+proxyPath);
       util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
       if(_debug) {
          util.printMessage("\nGet Credential for proxyPath " + proxyPath,
                logger,silent);
       }
       mycred =
        gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
     }
      else {
       ukey = properties.getProperty("user-key");
       ucert = properties.getProperty("user-cert");

       if(!ukey.startsWith("Enter") && (!ucert.startsWith("Enter"))) {
         useProxy=false;
         inputVec = new Vector ();
         inputVec.addElement("Using usercert="+ucert);
         inputVec.addElement("Using userkey="+ukey);
         util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
         util.printMessage("\nUsing usercert :" + ucert,logger,silent);
         util.printMessage("\nUsing userkey  :" + ukey,logger,silent);
         if(_password.equals("")) {
          inputVec = new Vector ();
          util.printEventLog(_theLogger,"Enter GRID passphrase",inputVec,silent,useLog);
          String line = PasswordField.readPassword("Enter GRID passphrase: ");
          _password = line;
         }
         mycred =
            gov.lbl.srm.client.util.Util.getCredential(ucert,ukey,
                useProxy,_password);
       }
       else {
          inputVec = new Vector ();
          inputVec.addElement("Using default user proxy="+proxyPath);
          util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
          util.printMessage("\nUsing default user proxy " +
            proxyPath,logger,silent);
         //proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        try {
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID2();
        }catch(Exception ue) { 
          System.out.println("\nSRM-CLIENT: Exception from client="+ue.getMessage());
          proxyPath ="/tmp/x509up_u"+MyConfigUtil.getUID();
        }
         mycred =
          gov.lbl.srm.client.util.Util.getCredential(proxyPath,"",useProxy,"");
       }
     }
     }catch(Exception e) {
      inputVec = new Vector ();
      inputVec.addElement("Exception="+e.getMessage());
      util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
      util.printMessage("Exception=" + e.getMessage(),logger,silent);
      util.printStackTrace(e,logger);
   }

   if(mycred == null) {
     if(useProxy) {
       inputVec = new Vector ();
       inputVec.addElement("Could not get credential for proxy " + proxyPath);
       inputVec.addElement("Please check your configuration settings " +
            "from menu Tools->Config.");
       util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
       SRMClientException ee = new SRMClientException
         ("Could not get credential for proxy " + proxyPath + "\n" +
          "Please check your configuration settings from menu Tools->Config.");
       Throwable tt = new Throwable
         ("Could not get credential for proxy " + proxyPath + "\n" +
          "Please check your configuration settings from menu Tools->Config.");
       ee.initCause(tt);
       util.printHException(ee,pIntf);
       throw ee;
     }
     else {
       inputVec = new Vector ();
       inputVec.addElement("Could not get credential for user-key " + ukey);
       inputVec.addElement("  and user-cert " + ucert);
       inputVec.addElement(" Please check your configuration " +
         "settings from menu Tools->Config.");
       util.printEventLog(_theLogger,"GetCredential",inputVec,silent,useLog);
       SRMClientException ee = 
        new SRMClientException
        ("Could not get credential for user-key " + ukey + "\n" +
         "and user-cert " + ucert + "\n" +
         "Please check your configuration " +
         "settings from menu Tools->Config.");
       Throwable tt = 
        new Throwable 
        ("Could not get credential for user-key " + ukey + "\n" +
         "and user-cert " + ucert + "\n" +
         "Please check your configuration " +
         "settings from menu Tools->Config.");
       ee.initCause(tt); 
       util.printHException(ee,pIntf);
       throw ee;
     }
   }
   return mycred;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isConfigExists
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean isConfigExists (String configFileLocation,
	String cmd, boolean silent, boolean useLog, java.util.logging.Logger _theLogger) {
 try {
  File f = new File(configFileLocation);
  if(!f.exists()) return false;
 }catch(Exception e) {
   Vector inputVec = new Vector ();
   inputVec.addElement("Exeption="+e.getMessage());
   util.printEventLog(_theLogger,"IsConfigExists",inputVec,silent,useLog);
   if(!silent) {
   System.out.println(cmd+ ": Exception="+e.getMessage());
   }
   //ShowException.logDebugMessage(logger,e);
   ShowException.showMessageDialog(null, "Exception : " + e.getMessage());
 }
 return true;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parsefile -- util file to parse the config
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Properties parsefile(String path, String cmd, boolean silent, boolean useLog, java.util.logging.Logger _theLogger) 
		throws IOException,
    java.util.zip.DataFormatException, Exception
{

   Properties sys_config = new Properties();
   try {
       File f = new File(path);
       if(!f.exists()) {
         Vector inputVec = new Vector ();
         inputVec.addElement("Please make sure you have conf file exists " + path);
         util.printEventLog(_theLogger,"ParseFile",inputVec,silent,useLog);
         util.printMessage("\n"+cmd + ": Please make sure you have conf file exists " +
            path,null,silent);
         throw new Exception("\nPlease make sure you have conf file exists " +
            path);
       }
       String ref;

       FileInputStream file = new FileInputStream(path);
       BufferedReader in = new BufferedReader(new InputStreamReader(file));

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
       Vector inputVec = new Vector ();
       inputVec.addElement("parsefile: Can't read from " + ex.getMessage());
       util.printEventLog(_theLogger,"parsefile",inputVec,silent,useLog);
       throw new IOException(cmd+ ": parseFile: Can't read from `" +
          ex.getMessage() + "'");
   } catch (java.util.zip.DataFormatException de) {
       Vector inputVec = new Vector ();
       inputVec.addElement("parsefile: Invalid format " + de.getMessage());
       util.printEventLog(_theLogger,"parsefile",inputVec,silent,useLog);
       throw new java.util.zip.DataFormatException
            (cmd + ": parseFile: Invalid format " + de.getMessage() + "'");
   }
   return sys_config;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//parseXML
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static  Request parseXML (String fileName, String cmd, boolean silent, 
	boolean useLog, java.util.logging.Logger _theLogger)
    throws NumberFormatException, SRMClientException, Exception {

   if(!silent) {
     System.out.println("\n"+cmd+": Parsing request file " + fileName);
   }
   Vector inputVec = new Vector ();
   inputVec.addElement("Parsing request file " + fileName);
   util.printEventLog(_theLogger,"ParseXML",inputVec,silent,useLog);
   XMLParseRequest xmlParse = new XMLParseRequest(fileName,_theLogger,silent,useLog);
   Request req = xmlParse.getRequest();

   req.getFileInfo();

   Vector filesVec = req.getFiles();

   for(int i = 0; i < filesVec.size(); i++) {
     FileInfo fInfo = (FileInfo) filesVec.elementAt(i);

     String surl = fInfo.getSURL();
     String turl = fInfo.getTURL();

     if(surl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(surl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         fInfo.setSURL("gsiftp://"+host+":"+port+"/"+path);
       }
     }
     if(turl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(turl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         fInfo.setTURL("gsiftp://"+host+":"+port+"/"+path);
       }
     }
   }

   return req;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Request createRequest(Vector surl,
   String turl, String durl, boolean _debug, boolean silent,
   boolean useLog,
   String cmd, boolean recursive, java.util.logging.Logger _theLogger,
   org.apache.commons.logging.Log logger) throws Exception {

   Vector inputVec = new Vector();
   util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
   if(_debug) {
   util.printMessage(cmd +": Creating request ...",logger,silent);
   }
   Request req = new Request();
   for(int i = 0; i < surl.size(); i++) {
    
     String temp = (String) surl.elementAt(i);

     if(temp.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(temp,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         temp = "gsiftp://"+host+":"+port+"/"+path;
       }
     }

     if(turl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(turl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         turl = "gsiftp://"+host+":"+port+"/"+path;
       }
     }

     FileInfo fInfo = new FileInfo();
     fInfo.setSURL(temp);
     fInfo.setTURL(turl);
     fInfo.setOrigSURL((String)surl.elementAt(i));
     fInfo.setOrigTURL(turl);
     fInfo.overWriteTURL(false);
     fInfo.setIsRecursive(new Boolean(recursive));
     //let us not set size.
    //fInfo.setExpectedSize(fileSize);
     req.addFileInfo(fInfo);
   }

   return req;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// createRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static Request createRequest(String surl,
   String turl, String durl, boolean _debug, boolean silent,
   boolean useLog, String cmd, boolean recursive, java.util.logging.Logger _theLogger,
   org.apache.commons.logging.Log logger) throws Exception {

   Vector inputVec = new Vector();
   util.printEventLog(_theLogger,"CreatingRequest",inputVec,silent,useLog);
   if(_debug) {
   util.printMessage(cmd +": Creating request ...",logger,silent);
   }

   if(surl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(surl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         surl = "gsiftp://"+host+":"+port+"/"+path;
       }
    }

    if(turl.startsWith("gsiftp")) {
       MyGlobusURL gurl = new MyGlobusURL(turl,0);
       String path = gurl.getPath();
       String host = gurl.getHost();
       int port = gurl.getPort();
       if(!path.startsWith("//")) {
         turl = "gsiftp://"+host+":"+port+"/"+path;
       }
    }

   Request req = new Request();
   FileInfo fInfo = new FileInfo();
   fInfo.setSURL(surl);
   fInfo.setTURL(turl);
   fInfo.setOrigSURL(surl);
   fInfo.setOrigTURL(turl);
   fInfo.overWriteTURL(false);
   fInfo.setIsRecursive(new Boolean(recursive));
   //let us not set size.
   //fInfo.setExpectedSize(fileSize);
   req.addFileInfo(fInfo);

   return req;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSFN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getSFN(String surl) throws Exception {
  int idx = surl.indexOf("?SFN");
  if(idx != -1) {
    return surl.substring(idx);
  }
  else {
    MyGlobusURL gurl = new MyGlobusURL(surl,1); 
    String path = gurl.getFilePath();
    while(true) {
       if(path.charAt(0) == '/' && path.charAt(1) == '/') {
         path = path.substring(1);
       }
       else {
        break;
       }
    }
    return "?SFN="+path;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSFN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getSFN(String surl, String serviceUrl) throws Exception {
  int idx = surl.indexOf("?SFN");
  if(idx != -1) {
    return surl.substring(idx);
  }
  else {
    MyGlobusURL gurl = new MyGlobusURL(surl,1); 
    String path = gurl.getFilePath();
    while(true) {
       if(path.charAt(0) == '/' && path.charAt(1) == '/') {
         path = path.substring(1);
       }
       else {
        break;
       }
    }
    MyGlobusURL gurl1 = new MyGlobusURL(serviceUrl,0);
    idx = path.indexOf(gurl1.getPath());
    if(idx != -1) {
      int length = gurl1.getPath().length();
      return "?SFN="+path.substring(length);
    }
    else {
      return "?SFN="+path;
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//findServiceSRMFromSURL
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String findServiceSRMFromSURL(String surl) throws Exception {
  if(!surl.startsWith("srm://")) return "";

  MyGlobusURL sgurl = new MyGlobusURL(surl,0);
  String sHost = sgurl.getHost();
  int sPort = sgurl.getPort();
  String sPath = sgurl.getPath();
  int index = sPath.indexOf("?SFN");
  String sServiceName = "/srm/managerv2";
  if(!sPath.equals("")) {
   sServiceName=sPath;
  }

  while(true) {
   if(sServiceName.charAt(0) == '/' && sServiceName.charAt(1) == '/') {
     sServiceName = sServiceName.substring(1);
   }
   else {
     break;
   }
  }
  String sourceSRM = "httpg://"+sHost+":"+sPort+sServiceName;
  return sourceSRM;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getServiceUrl
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getServiceUrl (String surl, String serviceURL, 
	String serviceHandle, int servicePortNumber, int type, boolean silent,
    boolean useLog, java.util.logging.Logger _theLogger, 
	org.apache.commons.logging.Log logger) throws Exception {

  Vector inputVec = new Vector();

  MyGlobusURL gurl = null;

  if(type == 0) {
    gurl = new MyGlobusURL(surl,0);
  }
  else {
    gurl = new MyGlobusURL(surl,1);
  }

  String protocol = gurl.getProtocol();
  String host = gurl.getHost();
  int port = gurl.getPort();
  String path = gurl.getPath();

  /*
  System.out.println("Surl="+surl);
  System.out.println("Protocol="+protocol);
  System.out.println("Port="+port);
  System.out.println("Host="+host);
  System.out.println("Path="+path);
  System.out.println("ServiceURL="+serviceURL);
  System.out.println("ServiceHandle="+serviceHandle);
  System.out.println("ServicePortNumber="+servicePortNumber);
  System.out.println("Type="+type);
  */

  if(protocol == null || protocol.equals("")) {
    util.printMessage("SRM-CLIENT: Please provide the correct serviceHandle including the protocol srm", logger,silent);
    return null;
  }

  if(host == null || host.equals("")) {
    inputVec = new Vector();
    inputVec.addElement("Reason=Please provide the WSDL Host to connect");
    util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Please provide the WSDL Host to connect.",
        logger,silent);
    return null;
  }

  if(port == -1) {
     inputVec = new Vector();
     inputVec.addElement("Reason=Please provide the WSDL port to connect");
     util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Please provide the WSDL port to connect.",
        logger,silent);
     return null;
  }

  if(path == null || path.equals("") || path.equals("/")) {
     if(serviceHandle.equals("")) {
      inputVec = new Vector();
      inputVec.addElement("Reason=Please provide the WSDL serviceHandle to connect");
      util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
      util.printMessage("SRM-CLIENT: Please provide the WSDL serviceHandle to connect.",
       logger,silent);
      path="";
     }
     else {
      inputVec = new Vector();
      inputVec.addElement("Using the serviceHandle " + serviceHandle + 	
		"to connect.");
      util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
      util.printMessage("\nSRM-CLIENT: SURL does not contains ?SFN ",
                          logger,silent);
      util.printMessage("SRM-CLIENT: serviceHandle "+ serviceHandle +
               " is taken from the srmclient.conf ", logger,silent);
      MyGlobusURL tempUrl = new MyGlobusURL(surl,2);
      util.printMessage("SRM-CLIENT: SFN is assumed as " + tempUrl.getPath(),  
			logger,silent);
      path = serviceHandle;
     }
  }
  else {
     serviceHandle = path;
  }

  if(!path.startsWith("/")) {
    path = "/"+path ;
  }

  if(path.length() == 1) return null;

  while(true) {
    if(path.charAt(0) == '/' && path.charAt(1) == '/') {
      path = path.substring(1);
    }
    else {
      break;
    }
  }

  int idx = path.indexOf("?SFN");
  if(idx != -1) {
    path = path.substring(0,idx);
  }

  if(port == 2811) { //then it is the case srm://host//path
    if(!serviceURL.equals("")) {
       return getServiceUrl2(serviceURL,silent,useLog,_theLogger, logger);
    }
    else {
    if(surl.startsWith("gsiftp")) return null;
    if(servicePortNumber == 0) {
       util.printMessage(
        "Please provide the servicePortNumber in the conf file " +
        " or in the serviceUrl",logger,silent);
       return null;
    }
    if(serviceHandle.equals("")) {
       util.printMessage(
        "Please provide the serviceHandle in the conf file " +
        " or in the serviceUrl",logger,silent);
       return null;
    }
    if(!serviceHandle.startsWith("/")) {
        serviceHandle = "/"+serviceHandle;
    }

    while(true) {
      if(serviceHandle.charAt(0) == '/' && serviceHandle.charAt(1) == '/') {
        serviceHandle = serviceHandle.substring(1);
      }
      else {
        break;
      }
    }
    return  "httpg://"+host+":"+servicePortNumber+serviceHandle;
    }
  }
  else { 
    return  "httpg://"+host+":"+port+path;
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getServiceUrl2
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static String getServiceUrl2 (String surl, boolean silent, boolean useLog, java.util.logging.Logger _theLogger, org.apache.commons.logging.Log logger) 
		throws Exception {

  MyGlobusURL gurl = new MyGlobusURL(surl,0);
  String host = gurl.getHost();
  int port = gurl.getPort();
  String path = gurl.getPath();
  Vector inputVec = new Vector ();

  if(host == null || host.equals("")) {
    inputVec = new Vector();
    inputVec.addElement("Reason=Please provide the WSDL Host to connect");
    util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
    util.printMessage("SRM-CLIENT: Please provide the WSDL Host to connect.",
        logger,silent);
    return null;
  }

  if(port == -1) {
     inputVec = new Vector();
     inputVec.addElement("Reason=Please provide the WSDL port to connect");
     util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Please provide the WSDL port to connect.",
        logger,silent);
     return null;
  }

  if(path == null || path.equals("")) {
     inputVec = new Vector();
     inputVec.addElement("Reason=Please provide the WSDL path to connect");
     util.printEventLog(_theLogger,"GetServiceURL",inputVec,silent,useLog);
     util.printMessage("SRM-CLIENT: Please provide the WSDL path to connect.",
       logger,silent);
     path="";
     //showUsage(false);
  }

  if(!path.startsWith("/")) {
    path = "/"+path ;
  }

  while(true) {
    if(path.charAt(0) == '/' && path.charAt(1) == '/') {
      path = path.substring(1);
    }
    else {
      break;
    }
  }

  int idx = path.indexOf("?SFN");
  if(idx != -1) {
    path = path.substring(0,idx);
  }

  return  "httpg://"+host+":"+port+path;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// startThreadCallBack
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void setThreadCallBack(String fileName,
	Properties properties) {
  this.properties = properties;
  cb = new ThreadCallBack(this);
  cb.setFileName(fileName);
  cb.start();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


 public void processThreadRequest(String fileName) {
    try {
       DocumentBuilderFactory factory =
       DocumentBuilderFactory.newInstance();
       DocumentBuilder parser = factory.newDocumentBuilder();
       Document doc = parser.newDocument();
       Element root = doc.createElement("config");
       Enumeration e = properties.propertyNames();
       while(e.hasMoreElements()) {
          String names = (String) e.nextElement();
          Element prop = doc.createElement(names);
          String value = properties.getProperty(names);
          if(value != null) {
            prop.appendChild(doc.createTextNode(value));
          }
          root.appendChild(prop);
       }

       Element proxyRenew = doc.createElement("renew-proxy-auto");
       proxyRenew.appendChild(doc.createTextNode(""+false));
       root.appendChild(proxyRenew);

       doc.appendChild(root);
       //default encoding is UTF-8
       OutputFormat format = new OutputFormat(doc);
       format.setIndenting(true);
       XMLSerializer serializer = new XMLSerializer(format);
       PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
       serializer.setOutputCharStream (pw);
       serializer.setOutputFormat(format);
       serializer.serialize(doc);
       System.exit(0);
    }
    catch(Exception e) {
       System.out.println("Exception :" + e.getMessage());
       //e.printStackTrace();
    }
 }

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// loadCerts -- util to load all certificates
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

private static X509Certificate[] loadCerts(String userCert) {

  Vector vec = new Vector ();

  X509Certificate[] certs = new X509Certificate[1];

  try {
    certs[0] = CertUtil.loadCertificate(userCert);
  }catch(Exception e) {
     System.out.println("Exception " + e.getMessage());
  }

  return certs;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// parseSrmcpCommands
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public static boolean parseSrmCpCommands(String args, int type) {
   //type == 2 (is for directory functions)
   //type == 3 (is for permission functions)
     if(args.startsWith("-copy")) { ; }
     else if(args.startsWith("-stage")) { ; }
     else if(args.startsWith("-bringOnline")) { ; }
     else if(args.startsWith("-webservice_path")) {;}
     else if(args.startsWith("-mv")) { ; }
     else if(args.startsWith("-ls")) { ; }
     else if(args.startsWith("-rm")) { ; }
     else if(args.startsWith("-mkdir")) { ; }
     else if(args.startsWith("-rmdir")) { ; }
     else if(args.startsWith("-ping")) { ; }
     else if(args.startsWith("-getPermissions")) { ; }
     else if(args.startsWith("-checkPermissions")) { ; }
     else if(args.startsWith("-setPermissions")) { ; }
     else if(args.startsWith("-extendFileLifetime")) { ; }
     else if(args.startsWith("-advisoryDelete")) { ; }
     else if(args.startsWith("-getRequestStatus")) { ; }
     else if(args.startsWith("-getRequestSummary")) { ; }
     else if(args.startsWith("-getRequestTokens")) { ; }
     else if(args.startsWith("-getFileMetaData")) { ; }
     else if(args.startsWith("-getSpaceTokens")) { ; }
     else if(args.startsWith("-getStorageElementInfo")) { ; }
     else if(args.startsWith("-reserveSpace")) { ; }
     else if(args.startsWith("-releaseSpace")) { ; }
     else if(args.startsWith("-getSpaceMetaData")) { ; }
     else if(args.startsWith("-2")) { ; }
     else if(args.startsWith("-nogui")) { ; }
     else if(args.startsWith("-gss_expected_name")) { ; }
     else if(args.startsWith("-retry_num")) { ; }
     else if(args.startsWith("-space_token")) { ; }
     else if(args.startsWith("-access_latency")) { ; }
     else if(args.startsWith("-retention_policy")) { ; }
     else if(args.startsWith("-h") || (args.startsWith("-help"))) { ; }
     else { return false; }
   return true;
}

}
