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

package gov.lbl.srm.util;
   
import java.util.*;
import java.io.*;

import org.glite.voms.VOMSValidator;

//  import gov.bnl.gums.command.Configuration;
//import gov.bnl.gums.admin.*;
//import org.apache.commons.cli.*;
//import org.apache.axis.client.Stub;
//import org.opensciencegrid.authz.client.GRIDIdentityMappingServiceClient;
import org.opensciencegrid.authz.xacml.client.MapCredentialsClient;
//import org.opensciencegrid.authz.common.GridId;
import org.opensciencegrid.authz.xacml.common.LocalId;
import java.net.URL;

import java.security.cert.X509Certificate;

class ValidatorControl {
    org.glite.voms.VOMSValidator _vv = null;
    TSRMMutex _vvGuard = new TSRMMutex();
    
    public ValidatorControl() {
	Class clazz = org.glite.voms.VOMSValidator.class;
	String result0 =org.theshoemakers.which4j.Which4J.which(clazz);
	System.out.println("which vomsvalidator?"+result0);
    }
    
    public List getCerts(X509Certificate[] certChain) {
	if (!TSRMUtil.acquireSync(_vvGuard)) {
	    TSRMLog.info(ValidatorControl.class, null, "event=mutexFailed", null);
	    return null;
	}
	List result = null;
	try {
	    if (_vv == null) {
		//_vv = new org.glite.voms.VOMSValidator(null, null);
		org.glite.voms.PKIStore vomsStore = null;
		String vomsDir = System.getProperty( "VOMSDIR" );
		vomsDir = (vomsDir == null ) ? org.glite.voms.PKIStore.DEFAULT_VOMSDIR : vomsDir;
		TSRMLog.info(ValidatorControl.class, null, "vomsDir="+vomsDir, null);
		
		File theDir = new File(vomsDir);
		if (theDir.exists() && theDir.isDirectory() && theDir.list().length > 0) {
		    vomsStore = new org.glite.voms.PKIStore(vomsDir, org.glite.voms.PKIStore.TYPE_VOMSDIR, true);
		    vomsStore.rescheduleRefresh(900000);
		}
		
		org.glite.voms.PKIStore caStore;
		String caDir = System.getProperty( "CADIR" );
		caDir = (caDir == null) ? org.glite.voms.PKIStore.DEFAULT_CADIR : caDir;
		TSRMLog.info(ValidatorControl.class, null, "caDir="+caDir, null);
		caStore = new org.glite.voms.PKIStore( caDir, org.glite.voms.PKIStore.TYPE_CADIR, true );
		caStore.rescheduleRefresh(900000);
		
		_vv = new org.glite.voms.VOMSValidator(null, new org.glite.voms.ac.ACValidator(new org.glite.voms.PKIVerifier(vomsStore,caStore)));
	    }
	    _vv.setClientChain(certChain);
	    
	    List vc = _vv.parse().getVOMSAttributes();
            if ((vc == null) || (vc.size() == 0)) {
		return null;
            } 
            int nVomsCerts = vc.size();
            TSRMLog.debug(ValidatorControl.class, null, "vomsCertSize="+nVomsCerts, null);
            for (int i=0; i<nVomsCerts; i++) {
		org.glite.voms.VOMSAttribute curr = (org.glite.voms.VOMSAttribute)(vc.get(i));
                 TSRMLog.debug(ValidatorControl.class, null, "listVomsCert-"+i+"th="+curr.toString(), null);
            }
            if (gov.lbl.srm.server.Config._doValidateVoms) {
                _vv=_vv.validate();
            }
	    TSRMLog.debug(ValidatorControl.class, null, "event=validated", null);
	    
            List vomsCerts = _vv.parse().getVOMSAttributes();
	    if ((vomsCerts == null) || (vomsCerts.size() < nVomsCerts)) {
		String detail = "null vomsCerts";
		if (vomsCerts != null) {
		    detail = "vomsCertSize="+vomsCerts.size() +" expected="+nVomsCerts;
		}
		TSRMLog.debug(ValidatorControl.class, null, "event=validationFailed.", detail);
		result=null;
	    } else {
		result=vomsCerts;
	    }
	    return result;
	} catch (Exception e) {
	    TSRMLog.exception(GUMSXACMLClient.class, null, e);
	    return null;
	} finally {
	    TSRMUtil.releaseSync(_vvGuard);
	}
    }
}

public class GUMSXACMLClient implements IntGUMS {
    String _serviceUrl = null;
    MapCredentialsClient _client = null;
    static ValidatorControl _vvControl = new ValidatorControl();

    public GUMSXACMLClient(String serviceUrl, String _hostname) {
	initOpenSaml();
	_serviceUrl = serviceUrl;
	_client = new MapCredentialsClient();
	_client.setResourceX509ID(_hostname);
    }
    
    
    public static String getFQAN(X509Certificate[] certChain) {
	TSRMLog.debug(GUMSXACMLClient.class, null, "event=getFQAN input="+certChain, null);
	if (certChain == null) {
	    return null;
	}
	
	TSRMLog.debug(GUMSXACMLClient.class, null, "event=getFQAN", null);
	List all = VOMSInfo.retrieve(certChain);
        if (all == null) {
	    return null;
	}
	String allFqan="";

	for (int i=0; i<all.size(); i++) {
	    allFqan+=i+":"+all.get(i).toString();
	}
	TSRMLog.info(GUMSXACMLClient.class, "getFQAN", "fqanSize="+all.size(), "content="+allFqan);
	if (all.size() == 0) {
	   return null;
	}
	return all.get(0).toString();
    }

    public static List getVomsCert(X509Certificate[] certChain) {
	TSRMLog.debug(GUMSXACMLClient.class, null, "certChain="+certChain, null);
        if (certChain == null) {
            return null;
        }
	
	return _vvControl.getCerts(certChain);
    }
    
    public String getMappedID(String dn, String hostDN, X509Certificate[] certChain) throws Exception {
	String fqan = getFQAN(certChain);

	String stored = gov.lbl.srm.server.Config.getStored(dn, fqan);
	if (stored != null) {
	    TSRMLog.info(this.getClass(), null, "event=foundInCache", "result="+stored);
	    return stored;
	}

	return getMappedID(dn, fqan);

    }

    private String getMappedID(String userDN, String fqan) {
	if (_client == null) {
	    return null;
	}
	_client.setFqan(fqan);
	_client.setX509Subject(userDN);

	try {
	    LocalId id = _client.mapCredentials(_serviceUrl);
	    if (id == null) {
		TSRMLog.debug(this.getClass(), null, "event=noMappingRetrieved", "userDN="+userDN+" fqan="+fqan);
		return null;
	    }
	    TSRMLog.debug(this.getClass(), null, "event=retrievedMapping", "result="+id+" userDN="+userDN+" fqan="+fqan);
	    TSRMLog.debug(this.getClass(), null, "event=retrievedMapping", "resultName="+id.getUserName()+" userDN="+userDN+" fqan="+fqan);
	    TSRMLog.debug(this.getClass(), null, "event=retrievedMapping", "resultid="+id.getUID()+" userDN="+userDN+" fqan="+fqan);
	    String mappedName = id.getUserName();
	    if (mappedName == null) {
		mappedName = id.getUID();
	    }
	    gov.lbl.srm.server.Config.storeID(userDN, fqan, mappedName);		   
	    return mappedName;
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "Failed to get mapping from:"+_serviceUrl+"userDN="+userDN+" fqan="+fqan, e);
	    return null;
	}
		
    }

	public void initOpenSaml() {
	         try {
              org.apache.xml.security.Init.init();
              org.opensaml.DefaultBootstrap.bootstrap();
          } catch (Exception e) {
              e.printStackTrace();
              System.exit(1);
          }
	}

    public void initOpenSaml0() {
	try {
		String gumsUrlStr = "/data/junmin/gPlazma/jay/test/lib/";
		java.io.File f = new java.io.File(gumsUrlStr);
	System.out.println("..........looking in: "+f.toURL()+"  "+f.exists());
System.out.println("....."+java.io.File.class.getName());
        java.net.URL[] urlArray= {new java.io.File(gumsUrlStr+"opensaml-2.2.1.jar").toURL(), 
								  new java.io.File(gumsUrlStr+"xml-security-1.4.1.jar").toURL(),
								  new java.io.File(gumsUrlStr+"commons-logging-1.0.4.jar").toURL(),
								  new java.io.File(gumsUrlStr+"privilege-xacml-2.2.4.jar").toURL(),
								  new java.io.File(gumsUrlStr+"opensaml-2.2.1.jar").toURL(),
								  new java.io.File(gumsUrlStr+"xmltooling-1.0.1.jar").toURL(),
								  new java.io.File(gumsUrlStr+"slf4j-api-1.5.0.jar").toURL(),
								  new java.io.File(gumsUrlStr+"slf4j-simple-1.5.0.jar").toURL(),
								  new java.io.File(gumsUrlStr+"velocity-1.5.jar").toURL(),
								  new java.io.File(gumsUrlStr+"commons-collections-3.2.jar").toURL(),
								  new java.io.File(gumsUrlStr+"commons-lang-2.1.jar").toURL(),
								  new java.io.File(gumsUrlStr+"*.jar").toURL(),
									};
        java.net.URLClassLoader cl = new java.net.URLClassLoader(urlArray, null);
		//cl.loadClass("org/opensaml/DefaultBootstrap");
		Class bootClass = cl.loadClass("org.opensaml.DefaultBootstrap");
		Class initClass = cl.loadClass("org.apache.xml.security.Init");
		 cl.loadClass("org.apache.commons.logging.LogFactory");
		 cl.loadClass("org.opensciencegrid.authz.xacml.client.MapCredentialsClient");
		 cl.loadClass("org.opensaml.xml.ConfigurationException");
		 cl.loadClass("org.slf4j.LoggerFactory");
		 cl.loadClass("org.slf4j.impl.StaticLoggerBinder");
		 cl.loadClass("org.apache.velocity.app.Velocity");
		 cl.loadClass("org.apache.commons.collections.ExtendedProperties");
		 cl.loadClass("org.apache.commons.lang.StringUtils");
	    java.lang.reflect.Method initMethod = initClass.getMethod("init", null);
		java.lang.reflect.Method bootMethod = bootClass.getMethod("bootstrap", null);

    initMethod.invoke(null, null);
	bootMethod.invoke(null, null);
	/////////
	    //String result =org.theshoemakers.which4j.Which4J.which(org.apache.xml.security.Init.class);
	    //System.out.println(result);
	   // String result        =org.theshoemakers.which4j.Which4J.which(org.opensaml.DefaultBootstrap.class);
	    //System.out.println(result);
	    /*
	    org.apache.xml.security.Init.init();
	    org.opensaml.DefaultBootstrap.bootstrap();
		*/
	} catch (Exception e) {
	    TSRMLog.exception(this.getClass(), "init", e);
		e.printStackTrace();
	    throw new RuntimeException("Unabled to initialize opensaml."+e.getMessage());
	}
    }
}   


class VOMSInfo {
    public static List retrieve(X509Certificate[] certChain) {
	try {
	    //VOMSValidator vv = new VOMSValidator(certChain);
	    //List vomsCerts = vv.parse().getVOMSAttributes();
	    List vomsCerts = GUMSXACMLClient.getVomsCert(certChain);
	    if (vomsCerts == null) {
		return null;
	    }
	    Vector result = new Vector();
	    
	    int total = vomsCerts.size();
	    
	    for (int i=0; i<total; i++) {
	    org.glite.voms.VOMSAttribute curr = (org.glite.voms.VOMSAttribute)(vomsCerts.get(i));
	    //AttributeCertificate ac = curr.getAC();
	    //TSRMLog.info(GUMSClient.class, "ac issuer="+ac.getIssuer().toString(), null, null);
	    result.addAll(curr.getFullyQualifiedAttributes());
	    }
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();

	    TSRMLog.exception(VOMSInfo.class, "retrieve fqan", e);	    

	    return null;
	}
    }
}

