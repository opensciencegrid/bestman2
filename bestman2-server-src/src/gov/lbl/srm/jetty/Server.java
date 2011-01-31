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

package gov.lbl.srm.server;

import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.TSRMStorage;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.impl.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.Properties;

import javax.xml.rpc.Stub;

import org.globus.gsi.GlobusCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import org.globus.security.gridmap.GridMap;
import org.ietf.jgss.GSSCredential;

import org.apache.axis.types.URI;

import java.security.PrivateKey;
import java.security.cert.*;
import org.globus.gsi.bc.*;
import org.globus.gsi.CertUtil;
import org.globus.gsi.OpenSSLKey;
import java.util.Vector;

import java.io.File;

import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.server.nio.*;
import org.eclipse.jetty.util.thread.*;
import org.eclipse.jetty.deploy.*;
import org.eclipse.jetty.webapp.*;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlet.ServletHandler;
import org.apache.axis.transport.http.AdminServlet;
import org.apache.axis.transport.http.AxisServlet;
                                                             
public class Server {    
    //static String _serviceHandle = "/ogsa/services"; //"/ogsa/services/srm/v2/SRMFactoryService";

    public static void printVersion() {
	String name = "BeStMan "+TSRMServer.class.getPackage().getImplementationVersion();
	String lbnl = "BeStMan Copyright(c) 2007-2008, The Regents of the University Of California, through Lawrence Berkeley National Laboratory. All rights reserved. ";
	String email = "Email questions to SRM@LBL.GOV";

	System.out.println(name);
	System.out.println(lbnl);
	System.out.println(email);
    }

    public static void main(String[] args) {
      mainJetty(args);
    }

    private static void mainJetty(String[] args) {      
      loadConfig(args);
      System.out.println(".... using gsi connection.");

      String descFile = System.getProperty("JettyDescriptor");      
      if (descFile == null) {
	  throw new RuntimeException("No descriptor file defined for Jetty server.");
      }
      String resourceBase = System.getProperty("JettyResource");
      if (resourceBase == null) {
	  throw new RuntimeException("No resource dir defined for Jetty server");
      }
      try { 
	org.eclipse.jetty.server.Server jettyWebServer = new org.eclipse.jetty.server.Server();	
	String serverConfigFileName = System.getProperty("JettyConfiguration");
	if (serverConfigFileName == null) {
	    serverConfigFileName = "jetty.xml"; 
	}
	File jettyXml = new File(serverConfigFileName);
	if (jettyXml.exists()) {
	  System.out.println("...appling "+serverConfigFileName);
	  org.eclipse.jetty.xml.XmlConfiguration jettyConfig = new org.eclipse.jetty.xml.XmlConfiguration(jettyXml.toURL());
	  jettyConfig.configure(jettyWebServer);
	} else {
	  System.out.println(".. no jetty configuration file will be used for jetty server");
	}

	JettyGSIConnector7 connector = new JettyGSIConnector7();
	//connector.setHandshakeTimeout(3000000);
	//System.out.println(".................................................double check:"+connector.getHandshakeTimeout());
	//System.out.println("........connector queue size:"+connector.getAcceptQueueSize());

	String maxThreadStr = System.getProperty("ConnectorPoolMaxThreads");

	if (maxThreadStr != null) {
	    System.out.println(".........appling to connector: maxThread="+maxThreadStr);
	    QueuedThreadPool p = new QueuedThreadPool(Integer.parseInt(maxThreadStr));
	    String minThreadStr = System.getProperty("ConnectorPoolMinThreads");
	    if (minThreadStr != null) {
	        System.out.println(".........appling to connector: minThread="+minThreadStr);
		p.setMinThreads(Integer.parseInt(minThreadStr));
	    }
	    connector.setThreadPool(p);
	}
							 

	System.out.println("........pool:"+connector.getThreadPool()+" "+jettyWebServer.getThreadPool());
	String AQSStr=System.getProperty("ConnectorAcceptQueueSize");
	if (AQSStr != null) {
	   connector.setAcceptQueueSize(Integer.parseInt(AQSStr));
	}
	System.out.println("..........acceptQueueSize:"+connector.getAcceptQueueSize());
	String accStr = System.getProperty("AcceptorSize");
	if (accStr != null) {
	    connector.setAcceptors(Integer.parseInt(accStr));
	}
	System.out.println("..................acceptor:"+connector.getAcceptors());

	connector.setPort(Config._securePort);
	connector.setHostCertificatePath(Config._userCertFileName);
	connector.setHostKeyPath(Config._userKeyFileName);
	//connector.setCaCertificatePath("/etc/grid-security/certificates");
	String cadir = System.getProperty("JettyConnectorCACertPath");
	if (cadir != null) {
	  System.out.println("using certificate: "+cadir);
	  connector.setCaCertificatePath(cadir);
	}
	connector.setAutoFlush(true);                                                                                     
	connector.setEncrypt(true);                                                                                       
	connector.setRequireClientAuth(true);
	//connector.setRequireClientAuth(false);
	connector.setAcceptNoClientCerts(false);
                                                                                               
	connector.setHandshakeTimeout(10000);
	connector.setGssMode("gsi");
	jettyWebServer.addConnector(connector);
	
	WebAppContext webapp = new WebAppContext();
	
	webapp.setDescriptor(descFile);
                                                         
	webapp.setContextPath("/"); 
        
	// this is needed, not skippable
	webapp.setResourceBase(resourceBase);

	webapp.setContextPath("/");
	
	webapp.setParentLoaderPriority(true);
	jettyWebServer.setHandler(webapp);
	
	jettyWebServer.start();
	/*
Handler h = jettyWebServer.getHandler();
System.out.println("....handler..."+h);
if (h instanceof WebAppContext) {
	WebAppContext wc = (WebAppContext)h;
	System.out.println(".... checking inactive time;"+wc.getSessionHandler().getSessionManager().getMaxInactiveInterval());
	wc.getSessionHandler().getSessionManager().setMaxInactiveInterval(-1);
	System.out.println(".... checking inactive time;"+wc.getSessionHandler().getSessionManager().getMaxInactiveInterval());

}
*/
                                                                                                                             
	System.out.println("BeStMan-Jetty is ready.");
	jettyWebServer.join();
    } catch (Exception e) {   
      e.printStackTrace();                                                                              
    }                                                                                                   
  }               
    public static void loadConfig(String[] args) {    	 	
	String configFileName = Config._DefDefaultConfigFileName;
	if (args == null) {
	  // use default configfilename
	} else if ((args.length ==1) && (!(args[0].endsWith("help"))) && (!(args[0].endsWith("version")))) {
	    configFileName = args[0];
	} else if (args.length == 0) {
	    TSRMUtil.startUpInfo( "## assuming default configuration file:"+configFileName);
	} else if (args[0].endsWith("version")) {
	    printVersion();
		System.exit(0);
	    //return;
	} else {
	    TSRMHelp.usage();
		System.exit(0);
	    //return;
	}

	Runtime.getRuntime().addShutdownHook(new Thread() {
		public void run() {
		    if (TSRMLog.getCacheLog() != null) {
			TSRMLog.getCacheLog().clean();
		    }
			TSRMUtil.startUpInfo("=========== Good bye! ========");
		    //System.out.println("========= Good bye! =========");
		}
	});

	//obtainOgsiServiceHandle();
	
	 try {	     
	     if (!Config.load(configFileName)) {
		 TSRMUtil.startUpInfo("## error with entries of config file, abort.");
		 return;
	     } 
	 } catch (java.io.IOException e) {
	      System.exit(0);
	 }
	  
	Config._configFileNameLoaded = configFileName;
	Config.setWSDLEndpoint(Config._host+":"+Config._securePort+Config._serviceHandle+"/"+Config._id);
    }

    public static void mainOld(String[] args) {    	 	
	if (!Config.loadSRM(Config._configFileNameLoaded)) {
	    TSRMUtil.startUpInfo("## error with entries of config file, abort.");
	    return;
	} 
    
	if (Config._id == null) {
	    TSRMUtil.startUpInfo("## error: need to define entry: FactoryID");
	    TSRMUtil.startUpInfo("## Aborting. ");
	    return;
	}

	//boolean secure = true;
	boolean secure = (Config._securePort != Config._publicPort);
	if (!secure) {
	    TSRMUtil.startUpInfo( ".................remember to set httpgPort in server-config.wsdd");
		Config._DefContactProtocolStr = "http"; 
        Config.setWSDLEndpoint(Config._host+":"+Config._securePort+Config._serviceHandle+"/"+Config._id);
	}
	boolean lazy   = false;

	GridMap gridMap = null;

	try {
	    gridMap = new GridMap();
	    gridMap.load(Config._gridMapFileName);	   	    

	    if (secure) {
		GSSCredential gssCred = null;
		GlobusCredential creds = null;
		if (Config._proxyFileName != null) {
		    creds = new GlobusCredential (Config._proxyFileName);
		    //gssCred = new GlobusGSSCredentialImpl(creds, GSSCredential.ACCEPT_ONLY);    
		} else if ((Config._userKeyFileName != null) && (Config._userCertFileName != null)) {
		    X509Certificate[] certs = Server.loadCerts(Config._userCertFileName);
		    OpenSSLKey key = new BouncyCastleOpenSSLKey(Config._userKeyFileName);
		    if(key.isEncrypted()) {
			String passwd = 
			    TSRMUtil.getPassword("Enter GRID passpharse : ");
			key.decrypt(passwd);
			PrivateKey pkey = key.getPrivateKey();
			creds = new GlobusCredential(pkey,certs);
		    } else {			    
			creds = new GlobusCredential(Config._userCertFileName, Config._userKeyFileName);
		    }			  
		} else {
		    throw new RuntimeException ("SRM server exits. No proxy or server certificates provided.");
		}
		
/*
		TSRMUtil.startUpInfo("## Subject " + creds.getSubject());
		TSRMUtil.startUpInfo("## Issuer " + creds.getIssuer());
		TSRMUtil.startUpInfo("## Id " + creds.getIdentity());
		TSRMUtil.startUpInfo("## Timeleft " + creds.getTimeLeft());
		
		if (creds.getTimeLeft() <= 0) {
			TSRMUtil.startUpInfo("Credential is expired. Exiting. ");
			System.exit(1);
		}
		
		if (creds.getTimeLeft() < 3600) {
		    System.out.print("Credential lifetime is less than an hour. Continue? [y/n]");
		    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
		    
		    String answer = null;
		    
		    long start = System.currentTimeMillis();
		    while ((!br.ready() && (System.currentTimeMillis()- start) < 10000)) {
			Thread.yield();
		    }
		    if (!br.ready()) {
			TSRMUtil.startUpInfo("\nTimed out");
			System.exit(1);
		    }
		    try {
			answer = br.readLine();
		    } catch (java.io.IOException ioe) {
			TSRMUtil.startUpInfo("IO error trying to read your answer! Exiting.");
			System.exit(1);
		    }
		    
		    if (!answer.equalsIgnoreCase("y")) {
			TSRMUtil.startUpInfo("Did not receive confirmation to continue. Exiting. Your answer was:"+answer);
			System.exit(1);
		    }			
		}
		gssCred = new GlobusGSSCredentialImpl(creds, GSSCredential.ACCEPT_ONLY);   
*/		
/*
		ContainerThread containerThread = 
		    new ContainerThread (Config._publicPort,Config._securePort, secure, gridMap, gssCred, lazy);

		containerThread.start();
		TProcessingUnit._rescheduler.start();
		TProcessingUnit._rescheduler.join();

		containerThread.join ();
	    } else {	    
		ContainerThread containerThread = 
		    new ContainerThread (Config._publicPort, 0, false, null, null, lazy);              
		containerThread.start ();
		containerThread.join();
*/
	    }
/*
	} catch (org.globus.gsi.GlobusCredentialException e) {
	    // for new GlobusGredential
	    e.printStackTrace();
	} catch (org.ietf.jgss.GSSException e) {
	    // for new GlobusGSSCredential
	    e.printStackTrace();
*/
	} catch (java.io.IOException e) {
	    // for gridMap.load()
	    e.printStackTrace();
	} catch (java.security.GeneralSecurityException e) {
	    e.printStackTrace();
	} catch (java.lang.OutOfMemoryError e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	} finally {
	}
    }


   public static GlobusCredential getValidCred(String certFile,  String keyFile) {
    TSRMUtil.startUpInfo("Finding credential from cert:"+certFile+" key:"+keyFile);
    try {
        GlobusCredential creds = null;
        X509Certificate[] certs = Server.loadCerts(certFile);
        OpenSSLKey key = new BouncyCastleOpenSSLKey(keyFile);
        if(key.isEncrypted()) {
        String passwd =
            TSRMUtil.getPassword("Enter GRID passpharse : ");
        key.decrypt(passwd);
        java.security.PrivateKey pkey = key.getPrivateKey();
        creds = new org.globus.gsi.GlobusCredential(pkey,certs);
        } else {
        creds = new org.globus.gsi.GlobusCredential(certFile, keyFile);
        }
        if (creds.getTimeLeft() <= 0) {
        TSRMUtil.startUpInfo("The credential from:"+certFile+" is expired. Exiting. ");
        return null;
        }
        return creds;
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
    }

public static X509Certificate[] loadCerts(String userCert) {

  //File f = new File(certlocation);
  //listFiles(f, vec);

  //X509Certificate[] certs = new X509Certificate[vec.size()+1];
  X509Certificate[] certs = new X509Certificate[1];

  try {
    certs[0] = CertUtil.loadCertificate(userCert);
  }catch(Exception e) {
     TSRMUtil.startUpInfo("Exception " + e.getMessage());
  }

  /*
  Vector vec = new Vector ();
  int size = vec.size();
  for(int i = 0; i < size; i++) {
    try {
    String str = (String) vec.elementAt(i);
    System.out.println("Str is " + str);
    certs[i+1] = CertUtil.loadCertificate(str); 
    }catch(Exception e) {
        System.out.println("Exception " + e.getMessage());
    }    
  }
  */
  return certs;
}  
}


class MyGridMap extends GridMap {
    public String getUserID(String gid) {
	if (Config._gumsClient != null) {
	    return "xxx";
	}
	return super.getUserID(gid);
    }

    public String[] getUserIDs(String gid) {
	if (Config._gumsClient != null) {
	    String[] result = new String[1];
	    result[0] = "xxx";
	    return result;
	}
	return super.getUserIDs(gid);
    }    
}


