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

package gov.lbl.srm.impl;

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.SRMFileTransfer;


import EDU.oswego.cs.dl.util.concurrent.Mutex;

import org.apache.axis.MessageContext;
import org.apache.axis.Constants;
import org.globus.axis.transport.GSIHTTPTransport;
import org.globus.axis.util.Util;
import org.ietf.jgss.GSSCredential;
import org.globus.gsi.gssapi.GlobusGSSCredentialImpl;
import java.security.cert.X509Certificate;

public class TSRMService implements ISRM {
    TSRMServer _server = null;
    static Mutex _commonMutex = new Mutex();
	
    public TSRMService(boolean initServer) {//used by gateway
      if (initServer) {
	      _server = new TSRMServer();
      }
      initJettyConfig();
    }
  
    public TSRMService() {
      _server = new TSRMServer();
      initJettyConfig();
    }
  
    public void initJettyConfig() {
      try { 
	//String configFileName= System.getProperty("bestmanRC");
	//String[] args = new String[1];
	//args[0] = configFileName; //"/tmp/bestman.rc";
	//System.out.println("............>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> using "+args[0]+"   pointer="+this);
	Server.mainOld(null);
      } catch (Exception e) {
	e.printStackTrace();
	throw new RuntimeException("Failed to start TSRMService.");
      }
    }    


    //
    // helpers
    //
    //

  public static String gGetUID(String currMethodName) {
    return gGetUIDForJettyHttpg(currMethodName);
  }

  public static GSSCredential gGetCredentialJetty(Object obj) {
    if (obj == null) {      
      return null;
    }
    javax.servlet.http.HttpServletRequest sr = (javax.servlet.http.HttpServletRequest)obj;

    try {
      Object gsiContext = sr.getAttribute(org.globus.axis.gsi.GSIConstants.GSI_CONTEXT);
      Object cred = ((org.globus.gsi.gssapi.GlobusGSSContextImpl)gsiContext).getDelegCred();       
      TSRMLog.info(TSRMService.class, null, "event=gGetCredentialJetty", "cred="+cred);
      return (GlobusGSSCredentialImpl)cred;
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
    //throw new RuntimeException("how to get credential from jetty?");
  }                 

  public static String gGetUIDForJettyHttpg(String currMethodName) {
    if (!TSRMUtil.acquireSync(_commonMutex)) {
        return null;
    }
    try {
      TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());
      TSRMServer.checkMemoryUsage(currMethodName);
      
      // is Message Context thread safe?
      org.apache.axis.MessageContext msgC = org.apache.axis.MessageContext.getCurrentContext();
      Object obj=msgC.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);
      javax.servlet.http.HttpServletRequest sr = (javax.servlet.http.HttpServletRequest)obj;
      
      if (sr != null) {
	TSRMLog.debug(TSRMService.class, currMethodName, "remoteAdd="+sr.getRemoteAddr(), null);
	Object gsiContext = sr.getAttribute(org.globus.axis.gsi.GSIConstants.GSI_CONTEXT);
	if (gsiContext != null) {
		        msgC.setProperty( org.globus.axis.gsi.GSIConstants.GSI_CONTEXT, gsiContext);
	}
	
      }
      /*
      java.util.Iterator ii = msgC.getPropertyNames();
      while (ii.hasNext()) {
	String n = (String)(ii.next());
	System.out.println("properyN="+n+" value="+msgC.getProperty(n));
      }
      */
      org.ietf.jgss.GSSContext gssC = (org.ietf.jgss.GSSContext)(msgC.getProperty(org.globus.axis.gsi.GSIConstants.GSI_CONTEXT));
      if (gssC == null) {
	return null;
      }	
      TSRMLog.info(TSRMService.class, null, "event="+currMethodName,"gssCName="+gssC.getSrcName().toString()); 
      return gssC.getSrcName().toString();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      TSRMUtil.releaseSync(_commonMutex);
    }
    return null;
  }
	

  /*
  public static X509Certificate[]  gGetCertChainTomcatSSL(String currMethodName, boolean silent) {
    	if (!TSRMUtil.acquireSync(_commonMutex)) {	  
	  return null;
	}
	try {
	    if (!silent) {
		TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());
		TSRMServer.checkMemoryUsage(currMethodName);
	    }
	    TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());
	   
	    org.apache.axis.MessageContext msgC = org.apache.axis.MessageContext.getCurrentContext();
	    Object obj=msgC.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);
	    javax.servlet.ServletRequest sr = (javax.servlet.ServletRequest)obj;
 
	    //System.out.println("......remote addr="+sr.getRemoteAddr());

	    java.security.cert.X509Certificate[] certChain = (java.security.cert.X509Certificate[]) sr.getAttribute("javax.servlet.request.X509Certificate");
	    return certChain;
	} finally {
	    TSRMUtil.releaseSync(_commonMutex);
	} 
    }
  */
    public static String gGetUIDForGlobusContainer(String currMethodName) {
	/*
	GSSCredential creds = gGetCredential(currMethodName, false);

	if (creds != null) {
	    return gGetUIDFromCredential(currMethodName, creds);
	}
	*/
	if (!TSRMUtil.acquireSync(_commonMutex)) {	  
	    return null;
	} 
	try {
	    TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());
	    TSRMServer.checkMemoryUsage(currMethodName);
    		// is Message Context thread safe?
	    MessageContext ctx = MessageContext.getCurrentContext();

	//Object o = ctx.getProperty(org.globus.wsrf.impl.security.authentication.Constants.TRANSPORT_SECURITY_CONTEXT);
	//Object o = ctx.getProperty("org.globus.security.transport.context");
	//Object o = ctx.getProperty(org.globus.gsi.gssapi.GSSConstants.TRANSPORT_SECURITY_CONTEXT);
	//org.gridforum.jgss.ExtendedGSSContext.inquireByOid(org.globus.gsi.gssapi.GSSConstants.X509_CERT_CHAIN);
	      
	      
	      
	    /* another way to get caller id (if authenticated) 
	    org.globus.ogsa.impl.security.SecurityManager sm = org.globus.ogsa.impl.security.SecurityManager.getManager();
	    System.out.println(sm);
	    if (sm != null) {
		System.out.println(sm.getCaller());
	    }
	    */
	    //System.out.println("Contains property: "+Constants.MC_REMOTE_ADDR+"? "+ctx.containsProperty(Constants.MC_REMOTE_ADDR));
	    GSSCredential result = Util.getCredentials(ctx);		 
	    String DN = (String)(ctx.getProperty("org.globus.gsi.authorized.user.dn"));
	    return DN; // use DN in case some user has several mapped ids, and in that case, they can choose which one to use through "authorizedID"
	} finally {
	    TSRMUtil.releaseSync(_commonMutex);
	}
    }

    private static String gGetUIDFromCredential(String currMethodName, GSSCredential creds) {
    	String uid = null;
    	try {
	    uid = creds.getName().toString();
    		 
	    TSRMUtil.startUpInfo("## proxy: "+uid+" has remaining life time:"+creds.getRemainingLifetime());
    	} catch (org.ietf.jgss.GSSException e) {
	    TSRMLog.exception(TSRMService.class, currMethodName, e);
	    return null;
    	}
    	
    	return uid;
    }    
    
    public static GSSCredential gGetCredential(String currMethodName) {
	return gGetCredentialGlobusContainer(currMethodName, true);
    }

    public static X509Certificate[] gGetCertChain(String currMethodName) {
      if (!TSRMUtil.acquireSync(_commonMutex)) {                                                                                                      
	return null;                                                                                                                                  
      }                                                                                                                                               
      try {                                                                                                                                           
	  TSRMServer.checkMemoryUsage(currMethodName);                                                                                            
	  TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());                                                    
	  org.apache.axis.MessageContext msgC = org.apache.axis.MessageContext.getCurrentContext();                                                   
	  Object obj=msgC.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);                                           
	  javax.servlet.ServletRequest sr = (javax.servlet.ServletRequest)obj;                                                                        
	  //System.out.println("......remote addr="+sr.getRemoteAddr());                                                                              
	  java.security.cert.X509Certificate[] certChain = (java.security.cert.X509Certificate[]) sr.getAttribute("javax.servlet.request.X509Certificate");
	  //System.out.println("..........cert chain: "+certChain);
	  return certChain;                                                                                                                           
	} catch (Exception e) {
	e.printStackTrace();
	return null;
      } finally {                                                                                                                                     
	  TSRMUtil.releaseSync(_commonMutex);                                                                                                         
      }                     
      //throw new RuntimeException("how to get cert chain from jetty?");
      //return null;
      //return gGetCertChainTomcatSSL(currMethodName, true);
    }
    public static X509Certificate[] gGetCertChainGlobusContainer(String currMethodName) {
      GSSCredential cred = gGetCredentialGlobusContainer(currMethodName,true);
	if (cred == null) {	    
	    X509Certificate[] result = gGetChainFromPatchedGlobus(currMethodName);
	    if (result != null) {
		return result;
	    }
	    return null;
	}
	if (cred.getClass() == GlobusGSSCredentialImpl.class) {
	    GlobusGSSCredentialImpl globusCred = (GlobusGSSCredentialImpl)cred;
	    return globusCred.getCertificateChain();
	}	
	
	TSRMLog.warning(TSRMService.class, currMethodName, "Not globus credential", "no x509 cert chain obtained");
	return null;
    }

    
    public static X509Certificate[] gGetChainFromPatchedGlobus(String currMethodName) {
	if (!TSRMUtil.acquireSync(_commonMutex)) {
	    return null;
	}
	try {
	    TSRMUtil.startUpInfo("### gGetChainPatched ["+currMethodName+"] tid ="+Thread.currentThread().getName());
	    MessageContext ctx = MessageContext.getCurrentContext();
	    
	    //System.out.println(".....................[end]................."+ctx.getProperty("org.globus.gssapi.authorize.certChain"));
	   TSRMUtil.getCompiledDate(org.globus.gsi.gssapi.GlobusGSSContextImpl.class);
	      java.util.Vector v = (java.util.Vector)(ctx.getProperty("org.globus.gssapi.authorize.certChain"));
	      if (v != null && v.size() > 0) {
		  return org.globus.gsi.ptls.PureTLSUtil.certificateChainToArray(v);
	      }	      
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMLog.exception(TSRMService.class, "gGetCertChainFromPatchedGlobus", e);
	} finally {
	    TSRMUtil.releaseSync(_commonMutex);	   
	}
	return null;
    }
  
    public static GSSCredential gGetCredentialGlobusContainer(String currMethodName, boolean silent) {
    	if (TSRMUtil.acquireSync(_commonMutex)) {	  
	    if (!silent) {
		TSRMUtil.startUpInfo("### ["+currMethodName+"] tid ="+Thread.currentThread().getName());
		TSRMServer.checkMemoryUsage(currMethodName);
	    }
    		// is Message Context thread safe?
	    MessageContext ctx = MessageContext.getCurrentContext();
	    //GSSCredential result = Util.getCredentials(ctx);		 
	    Object obj=ctx.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);
                                                                       
            //javax.servlet.http.HttpServletRequest sr = (javax.servlet.http.HttpServletRequest)obj;
	    GSSCredential result = gGetCredentialJetty(obj);         
	    
	    //javax.servlet.http.HttpServletRequest req = (javax.servlet.http.HttpServletRequest)ctx.getProperty(org.apache.axis.transport.http.HTTPConstants.MC_HTTP_SERVLETREQUEST);
	    //System.out.println(".....req="+req); // null. so this is not good here
	    
	    /* first 2 are null, second 2 are the server url, cannt get the host where client came from
	       System.out.println("###:: "+ctx.getProperty(Constants.MC_REMOTE_ADDR));
	       System.out.println("###:: "+ctx.getProperty(Constants.ELEM_HEADER));
	       System.out.println("###:: "+ctx.getProperty(MessageContext.TRANS_URL));
	       System.out.println("###:: "+Util.getProperty(ctx, GSIHTTPTransport.URL));
	    */
	    if (result == null) {
	        if (ctx == null) {		    
		    TSRMLog.warning(TSRMService.class, currMethodName, "No cred", "MessageContext is null");
		} else {
		  TSRMLog.warning(TSRMService.class, currMethodName, "No cred, client didnt delegate", "MessageContext is:"+ctx); // client needs to delegate the credential!
		}
	    } 
	    TSRMUtil.releaseSync(_commonMutex);
	    return result;
    	} 
    	return null;
    }

    public void writeOut(String methodName) {
	TSRMLog.info(TSRMService.class, methodName, "event=outcoming", null);
    }

    public SrmPingResponse srmPing(SrmPingRequest req) throws java.rmi.RemoteException {       		
	SrmPingResponse result = _server.srmPing(req);	       
	writeOut("srmPing");
	return result;
    }    

    public SrmBringOnlineResponse srmBringOnline(SrmBringOnlineRequest req) throws java.rmi.RemoteException {       		
	SrmBringOnlineResponse result = _server.srmBringOnline(req);	       
	writeOut("srmBringOnline");
	return result;
    } 

    public SrmStatusOfLsRequestResponse srmStatusOfLsRequest(SrmStatusOfLsRequestRequest req) throws java.rmi.RemoteException {        
	SrmStatusOfLsRequestResponse result = _server.srmStatusOfLs(req);
	writeOut("srmStatusOfLs");
	return result;
    }    

    public SrmStatusOfBringOnlineRequestResponse srmStatusOfBringOnlineRequest(SrmStatusOfBringOnlineRequestRequest req) throws java.rmi.RemoteException {       			
        SrmStatusOfBringOnlineRequestResponse result = _server.srmStatusOfBringOnline(req);	       
	writeOut("srmStatusOfBringOnline");
	return result;
    }     

    public SrmGetTransferProtocolsResponse srmGetTransferProtocols(SrmGetTransferProtocolsRequest req) throws java.rmi.RemoteException  {
	SrmGetTransferProtocolsResponse result = _server.srmGetTransferProtocols(req);
	writeOut("srmGetTransferProtocols");
	return result;
    }

    
    public SrmGetPermissionResponse srmGetPermission(SrmGetPermissionRequest req) throws java.rmi.RemoteException  {
	SrmGetPermissionResponse result = _server.srmGetPermission(req);
	writeOut("srmGetPermission");
	return result;
    }

    public SrmReserveSpaceResponse srmReserveSpace(SrmReserveSpaceRequest req) throws java.rmi.RemoteException {       		
	SrmReserveSpaceResponse result = _server.srmReserveSpace(req);	       
	writeOut("srmReserveSpace");
	return result;
    }    

    public SrmReleaseSpaceResponse srmReleaseSpace(SrmReleaseSpaceRequest req) throws java.rmi.RemoteException {
		
	SrmReleaseSpaceResponse result = _server.srmReleaseSpace(req);	 
	writeOut("srmReleaseSpace");
	return result;
    }

    public SrmUpdateSpaceResponse srmUpdateSpace(SrmUpdateSpaceRequest req) throws java.rmi.RemoteException {	       	
	SrmUpdateSpaceResponse result= _server.srmUpdateSpace(req);	 
	writeOut("srmUpdateSpace");
	return result;
    }

    public SrmGetSpaceMetaDataResponse srmGetSpaceMetaData(SrmGetSpaceMetaDataRequest req) throws java.rmi.RemoteException {
		
	SrmGetSpaceMetaDataResponse result = _server.srmGetSpaceMetaData(req);	 
	writeOut("srmGetSpaceMetaData");
	return result;
    }

    public SrmGetSpaceTokensResponse srmGetSpaceTokens(SrmGetSpaceTokensRequest req) throws java.rmi.RemoteException {
		
	SrmGetSpaceTokensResponse result = _server.srmGetSpaceToken(req);	 
	writeOut("srmGetSpaceTokens");
	return result;
    }

    public SrmPurgeFromSpaceResponse srmPurgeFromSpace(SrmPurgeFromSpaceRequest req) throws java.rmi.RemoteException {
	SrmPurgeFromSpaceResponse result = _server.srmPurgeFromSpace(req);
	writeOut("srmPurgeFromSpace");
	return result;
    }
    
    public SrmChangeSpaceForFilesResponse srmChangeSpaceForFiles(SrmChangeSpaceForFilesRequest req) throws java.rmi.RemoteException {
	SrmChangeSpaceForFilesResponse result = _server.srmChangeSpaceForFiles(req);
	writeOut("srmChangeSpaceForFiles");
	return result;
    }

    public SrmStatusOfChangeSpaceForFilesRequestResponse srmStatusOfChangeSpaceForFilesRequest(SrmStatusOfChangeSpaceForFilesRequestRequest req) throws java.rmi.RemoteException {
	SrmStatusOfChangeSpaceForFilesRequestResponse result = _server.srmStatusOfChangeSpaceForFilesRequest(req);
	writeOut("srmStatusOfChangeSpaceForFiles");
	return result;
    }

    public SrmStatusOfUpdateSpaceRequestResponse srmStatusOfUpdateSpaceRequest(SrmStatusOfUpdateSpaceRequestRequest req) throws java.rmi.RemoteException {
	SrmStatusOfUpdateSpaceRequestResponse result = _server.srmStatusOfUpdateSpace(req);
	writeOut("srmStatusOfUpdateSpaceRequest");
	return result;
    }

    public SrmStatusOfReserveSpaceRequestResponse srmStatusOfReserveSpaceRequest(SrmStatusOfReserveSpaceRequestRequest req) throws java.rmi.RemoteException {	
	SrmStatusOfReserveSpaceRequestResponse result = _server.srmStatusOfReserveSpace(req);
	writeOut("srmStatusOfReserveSpaceRequest");
	return result;
    }
    public SrmExtendFileLifeTimeInSpaceResponse srmExtendFileLifeTimeInSpace(SrmExtendFileLifeTimeInSpaceRequest req) throws java.rmi.RemoteException {
	SrmExtendFileLifeTimeInSpaceResponse result =_server.srmExtendFileLifeTimeInSpace(req);
	writeOut("srmExtendFileLifeTimeInSpace");
	return result;
    }

    public SrmSetPermissionResponse srmSetPermission(SrmSetPermissionRequest req) throws java.rmi.RemoteException {
		
	 SrmSetPermissionResponse result = _server.srmSetPermission(req);	 
	 writeOut("SrmSetPermission");
	 return result;
    }

    public SrmCheckPermissionResponse srmCheckPermission(SrmCheckPermissionRequest req) throws java.rmi.RemoteException {
		
	SrmCheckPermissionResponse result = _server.srmCheckPermission(req);	 
	writeOut("SrmCheckPermission");
	return result;
    }

    public SrmMkdirResponse srmMkdir(SrmMkdirRequest req) throws java.rmi.RemoteException {
		
	SrmMkdirResponse result = _server.srmMkdir(req);	 
	writeOut("SrmMkdirResponse");
	return result;
    }

    public SrmRmdirResponse srmRmdir(SrmRmdirRequest req) throws java.rmi.RemoteException {		
	SrmRmdirResponse result = _server.srmRmdir(req);	 
	writeOut("SrmRmDir");
	return result;
    }

    public SrmRmResponse srmRm(SrmRmRequest req) throws java.rmi.RemoteException {		
	SrmRmResponse result = _server.srmRm(req);	 
	writeOut("SrmRm");
	return result;
    }

    public SrmLsResponse srmLs(SrmLsRequest req) throws java.rmi.RemoteException { 		
	SrmLsResponse result = _server.srmLs(req);	 
	writeOut("SrmLs");
	return result;
    }

    public SrmMvResponse srmMv(SrmMvRequest req) throws java.rmi.RemoteException { 		
	SrmMvResponse result = _server.srmMv(req);	 
	writeOut("SrmMv");
	return result;
    }

    public SrmPrepareToGetResponse srmPrepareToGet(SrmPrepareToGetRequest req) throws java.rmi.RemoteException { 		
	SrmPrepareToGetResponse result = _server.srmPrepareToGet(req);	 
	writeOut("SrmPrepareToGet");
	return result;
    }

    public SrmPrepareToPutResponse srmPrepareToPut(SrmPrepareToPutRequest req) throws java.rmi.RemoteException {	
	SrmPrepareToPutResponse result = _server.srmPrepareToPut(req);	 
	writeOut("SrmPrepareToPut");
	return result;
    }

    public SrmCopyResponse srmCopy(SrmCopyRequest req) throws java.rmi.RemoteException { 		
	SrmCopyResponse result = _server.srmCopy(req);	 
	writeOut("SrmCopy");
	return result;
    }

    
    public SrmReleaseFilesResponse srmReleaseFiles(SrmReleaseFilesRequest req) throws java.rmi.RemoteException { 		
	SrmReleaseFilesResponse result = _server.srmReleaseFiles(req);	 
	writeOut("SrmReleaseFiles");
	return result;
    }

    public SrmPutDoneResponse srmPutDone(SrmPutDoneRequest req) throws java.rmi.RemoteException { 		
	SrmPutDoneResponse result = _server.srmPutDone(req);	 
	writeOut("SrmPutDone");
	return result;
    }

    public SrmAbortRequestResponse srmAbortRequest(SrmAbortRequestRequest req) throws java.rmi.RemoteException {		
	SrmAbortRequestResponse result = _server.srmAbortRequest(req);	 
	writeOut("SrmAbortRequest");
	return result;
    }

    public SrmAbortFilesResponse srmAbortFiles(SrmAbortFilesRequest req) throws java.rmi.RemoteException {		
	SrmAbortFilesResponse result = _server.srmAbortFiles(req);	 
	writeOut("SrmAbortFiles");
	return result;
    }

    public SrmSuspendRequestResponse srmSuspendRequest(SrmSuspendRequestRequest req) throws java.rmi.RemoteException { 		
	SrmSuspendRequestResponse result = _server.srmSuspendRequest(req);	 
	writeOut("SrmSuspendRequest");
	return result;
    }

    public SrmResumeRequestResponse srmResumeRequest(SrmResumeRequestRequest req) throws java.rmi.RemoteException {    		
	SrmResumeRequestResponse result = _server.srmResumeRequest(req);	 
	writeOut("SrmResumeRequest");
	return result;
    }

    public SrmStatusOfGetRequestResponse srmStatusOfGetRequest(SrmStatusOfGetRequestRequest req) throws java.rmi.RemoteException { 		
	SrmStatusOfGetRequestResponse result = _server.srmStatusOfGetRequest(req);	 
	writeOut("SrmStatusOfGetRequest");
	return result;
    }

    public SrmStatusOfPutRequestResponse srmStatusOfPutRequest(SrmStatusOfPutRequestRequest req) throws java.rmi.RemoteException {		
	SrmStatusOfPutRequestResponse result = _server.srmStatusOfPutRequest(req);	 
	writeOut("SrmStatusOfPutRequest");
	return result;
    }

    public SrmStatusOfCopyRequestResponse srmStatusOfCopyRequest(SrmStatusOfCopyRequestRequest req) throws java.rmi.RemoteException {		
	SrmStatusOfCopyRequestResponse result = _server.srmStatusOfCopyRequest(req);	 
	writeOut("SrmStatusOfCopyRequest"+result.getReturnStatus().getStatusCode());
	return result;
    }


    public SrmGetRequestSummaryResponse srmGetRequestSummary(SrmGetRequestSummaryRequest req) throws java.rmi.RemoteException {		
	SrmGetRequestSummaryResponse result = _server.srmGetRequestSummary(req);	 
	writeOut("SrmGetRequestSummary");
	return result;
    }

    public SrmExtendFileLifeTimeResponse srmExtendFileLifeTime(SrmExtendFileLifeTimeRequest req) throws java.rmi.RemoteException {		
	SrmExtendFileLifeTimeResponse result = _server.srmExtendFileLifeTime(req);	 
	writeOut("SrmExtendFileLifeTimeResponse");
	return result;
    }

    public SrmGetRequestTokensResponse srmGetRequestTokens(SrmGetRequestTokensRequest req) throws java.rmi.RemoteException {		
	SrmGetRequestTokensResponse result = _server.srmGetRequestTokens(req);	 
	writeOut("SrmGetRequestTokens");
	return result;
    }
}
