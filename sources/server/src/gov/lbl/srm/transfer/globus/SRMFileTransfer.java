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

package gov.lbl.srm.transfer.globus;

import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.globus.gsi.gssapi.auth.Authorization;
import org.globus.ftp.GridFTPSession;

import org.globus.util.GlobusURL;
//import java.net.MalformedURLException;
import java.io.IOException;
import java.lang.Thread;
import gov.lbl.srm.transfer.ISRMFileTransfer;
import gov.lbl.srm.util.*;

public class SRMFileTransfer extends Thread implements ISRMFileTransfer {
  public static int DEFAULT_NUM_PARALLELSTREAM = 1; // defined by junmin
  public static int DEFAULT_BUFFERSIZE_MB = 0; // defined by junmin
  private boolean _isTxfing = false;
  private boolean _noProtection = false;
  
  private GlobusURL fromurl;
  private GlobusURL tourl;
  private int parallelism = DEFAULT_NUM_PARALLELSTREAM;
  private int buffersize = DEFAULT_BUFFERSIZE_MB*1048576;
  
  private GSSCredential sourcecredential = null;
  private GSSCredential destcredential = null;
  private Authorization sourceauth = null;
  private Authorization destauth = null;
  private boolean listener = false;
  // AS 03/27/2007 changed from false to true
  private boolean setdecau = true;
  // AS 03/27/2007
  private boolean appendmode = false;
  
  private SRMTransferProtocol ttype = SRMTransferProtocol.GRIDFTP;
  private SRMTransferMode tmode = SRMTransferMode.GET;
  private ISRMFileTransfer ucopy = null;
  private long _sourceSize = -1;
  
  private String status=null;

  private boolean _isHostNERSCHPSS; // added by junmin
  
  public String getSetupStr() {
    return "streams="+parallelism+" bufferbytes="+buffersize+" blocks=0";
  }

  public SRMFileTransfer () {
  }

  public void setNERSCHost() {
    _isHostNERSCHPSS = true;
  }
  
  public boolean transferDone() {
    if (_isTxfing) {
      return false;
    }
    if (ucopy == null) {
      return false;
    }
    if (ucopy.transferDone()) {
      return true;
    }
    throw new TSRMException("Transfer is probably timed out by SRM", false);
  }
  
  public String getStatus() {
    if ((status == null) && (ucopy!= null)) return ucopy.getStatus();
    return status;
  }
  
  public void run() {
    transferSync();
  }
  
  public void transfer () {
    this.start();
  }
  
  private void transferSyncNoRetry () {
    TSRMUtil.printout("TYPE#:" + ttype.toString());
    if ((ttype == SRMTransferProtocol.GRIDFTP) || (ttype == SRMTransferProtocol.GSIFTP)) {
      ucopy = doGridftp();
      if (ucopy != null) {
	ucopy.transferSync();
      }
    } else {	
      ucopy = doGasscopy();
      if (ucopy != null) {
	ucopy.transferSync();
      }
    }
  }    
  
  public void transferSync() { // with retry
    long timeout = TSRMTimedTask._GSIFTP_TIME_OUT_SECONDS;
    if (_sourceSize > 0) {
      long estimate = _sourceSize/(long)524288;
      if (estimate > timeout) {
	timeout = estimate;
	TSRMLog.info(this.getClass(), null, "timeoutSec="+estimate, null);
      }
    }
    
    TSRMTimedTask tt = new TSRMTimedTask(timeout);
    _isTxfing = true;
    int counter = 0;
    
    try {
      /*while (_isTxfing) {
	int i=0;
	System.out.println("hello");
	}*/
      if (gov.lbl.srm.server.Config._retryCountMax == 0) {
	transferSyncNoRetry();
	return;
      }
      while (counter < gov.lbl.srm.server.Config._retryCountMax) {
	transferSyncNoRetry();
	String status = getStatus();
	if (status == null) {
	  return;
	}
	if (isFatalError(status)) {
	  return;
	}
	counter ++;
	long napTimeMillis = ((long)gov.lbl.srm.server.Config._retryIntervalSeconds)*(long)1000;
	gov.lbl.srm.util.TSRMLog.debug(this.getClass(), null, "event=retry_txf.", "reason=\""+status+"\"");
	gov.lbl.srm.util.TSRMUtil.sleep(napTimeMillis);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      _isTxfing = false;
      tt.setFinished();
      tt.alarm();
    }
  }

  public boolean isFatalError(String status) {
    int pos = status.indexOf("Authentication Failed");
    if (pos > 0) {
      return true;
    }
    return false;  
  }
  
  public void setTransferType(SRMTransferProtocol ttypeu) {
    ttype = ttypeu;
  }
  
  public void setParallel(int parallelu) {
    parallelism = parallelu;
  }
  
  public void setDCAU(boolean dcau) {
    setdecau = dcau;
  }
  
  public void setCredentials(GSSCredential credentials) {
    sourcecredential = credentials;
    destcredential = credentials;
  }
  
  public void setSourceAuthorization(Authorization auth) {
    sourceauth = auth;
  }
  
  public void setDestinationAuthorization(Authorization auth) {
    destauth = auth;
  }
  
  public void setBufferSize(int size) {
    buffersize = size;
  }
  
  public void setDoSessionProtection(boolean clear) {
    _noProtection = clear;
  }
  
  //public void setAppendMode(boolean appendMode) {
  //  appendmode = appendMode;
  //}
  
  public String getSourceStr() { // added by junmin
    if (fromurl != null) {
      return fromurl.getURL();
    }
    return null;
  }
  
  public String getDestinationStr() { // added by junmin
    if (tourl != null) {
      return tourl.getURL();
    }
    return null;
  }
  
  public String getInputStr() { // added by junmin
    return "src="+getSourceStr()+" tgt="+getDestinationStr();
  }
  
  public void setSourceUrl(String surl) throws IOException {
    GlobusURL tempurl = null;
    try {
      tempurl = new GlobusURL (surl);
      String urlPath = tempurl.getPath();
      
      /*
	if (urlPath == null || urlPath.length() == 0) {
	status = "The '" + tempurl.getURL() + "' url does not specify the file location.";		
	throw new IOException(status);
	}
      */
      fromurl = tempurl; 
    } catch (Exception e) {
      throw new RuntimeException(e.toString());
    }
  }
  
  public void setDestinationUrl(String surl) throws IOException {
    GlobusURL tempurl = null;
    try {
      tempurl = new GlobusURL (surl);
      String urlPath = tempurl.getPath();
      if (urlPath == null || urlPath.length() == 0) {
	status = "The '" + tempurl.getURL() +
	  "' url does not specify the file location.";
	throw new IOException(status);
      }
      tourl = tempurl; 
    } catch (java.net.MalformedURLException e) {
      throw new RuntimeException(e.toString());
    }
  }
  
  public void setTransferMode(SRMTransferMode tmodeu) {
    tmode = tmodeu; 
  }
  
  public void setUseThirdPartyCopy() {
    tmode = SRMTransferMode.THIRDPARTY; 
  }
  
  public void cancel() {
    ucopy.cancel();
  }
  
  public void setListener(boolean listen) {
	listener = listen;
  }

  private SRMGridFTP doGridftp() {
    /*
      TSRMUtil.printout("Gridftp...");
	TSRMUtil.printout("from: " + fromurl.getURL());
	TSRMUtil.printout("to: " + tourl.getURL());
    */
    try {
      SRMGridFTP ucopy = new SRMGridFTP(fromurl, tourl, tmode);
if (buffersize > 0) {
      ucopy.setBufferSize(buffersize);
}
if (parallelism > 1) {
      ucopy.setParallel(parallelism);
}
      ucopy.setDCAU(setdecau);
      if (_noProtection) {
	ucopy.setSessionProtection(GridFTPSession.PROTECTION_CLEAR);
      } else {
	ucopy.setSessionProtection(GridFTPSession.PROTECTION_SAFE);
      }
      //TSRMUtil.printout(sourcecredential.export(org.gridforum.jgss.ExtendedGSSCredential.IMPEXP_OPAQUE).length);
      //TSRMUtil.printout(sourcecredential.export(org.gridforum.jgss.ExtendedGSSCredential.IMPEXP_MECH_SPECIFIC).length);
      //the following is added by junmin
      if (sourcecredential != null) 
	ucopy.setSourceCredentials(sourcecredential); 
      if (destcredential != null) 
	ucopy.setDestinationCredentials(destcredential); 
      if ( _isHostNERSCHPSS) {
	TSRMUtil.printout("..........for NERSC..............");
	ucopy.setDCAU(false);
	ucopy.setSessionMode(org.globus.ftp.Session.MODE_STREAM);
	ucopy.setServerModeActive();
      }
      return ucopy;
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
    return null;
  }
  
  private SRMUrlCopy doGasscopy() {
    TSRMUtil.printout("gasscopy...");
    TSRMUtil.printout("from: " + fromurl.getURL());
    TSRMUtil.printout("to: " + tourl.getURL());
    try {
      SRMUrlCopy ucopy = new SRMUrlCopy(fromurl, tourl);
      if (parallelism > 1) ucopy.setParallel(parallelism);
      ucopy.setDCAU(setdecau);
      if (sourcecredential != null) ucopy.setSourceCredentials(sourcecredential);
      if (destcredential != null) ucopy.setDestinationCredentials(destcredential);
      if (sourceauth != null) ucopy.setSourceAuthorization(sourceauth);
      if (destauth != null) ucopy.setDestinationAuthorization(destauth);
if (buffersize > 0) {
      ucopy.setBufferSize(buffersize);
}
      if (appendmode) ucopy.setAppendMode(appendmode);
      if (tmode == SRMTransferMode.THIRDPARTY) ucopy.setUseThirdPartyCopy();
      if (listener) ucopy.setListener(listener);
      
      return ucopy;
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
    return null;
  }
  
  //
  // added by junmin
  //

  public long getSourceFileSize() {
    TSRMTimedTask tt = new TSRMTimedTask(TSRMTimedTask._GSIFTP_TIME_OUT_SECONDS);
    try {
      _sourceSize = getSourceFileSizeAction();
      return _sourceSize;
    } finally {	    
      tt.setFinished();
      tt.alarm();
    }
  }
  
  private long getSourceFileSizeAction() {           
    if (ucopy != null) {
      return ucopy.getSourceFileSize();
    } else {
      if ((ttype == SRMTransferProtocol.GRIDFTP) || (ttype == SRMTransferProtocol.GSIFTP)) {
	// cannt use this on FTP. Getting "Server refused GSSAPI authentication" error.
	SRMGridFTP gftp = new SRMGridFTP(fromurl, null);
	if (sourcecredential != null)
	  gftp.setSourceCredentials(sourcecredential);
	if (destcredential != null)
	  gftp.setDestinationCredentials(destcredential);
	
	if (_isHostNERSCHPSS) {
	  gftp.doNERSCSetting();
	}
	
	try {
	  return gftp.getSourceFileSize();
	} catch (RuntimeException e) {
	  return -1;
	}
	
	// this involves target url and java hangs at TSRMUtil.printout for null pointer;
	//return doGridftp().getSourceFileSize();
      } else {
	try {
	  SRMUrlCopy ucopy = new SRMUrlCopy(fromurl, null);
	  return ucopy.getSourceFileSize();
	} catch (Exception e) {
	  return -1;
	}
	//return doGasscopy().getSourceFileSize();
      }
    }
  }
  
  public long getTransferTimeInMilliSeconds() {
    if (ucopy != null) {
      return ucopy.getTransferTimeInMilliSeconds();
    } else {
      return -1;
    }
  }
  
  public boolean doubleCheck() {
    String guc = gov.lbl.srm.server.Config._gucLocation;
    if (guc == null) {
      gov.lbl.srm.util.TSRMLog.debug(this.getClass(), null, "event=use_guc", "error=no_guc_path_defined");
	return false;
    }
    
    String src = fromurl.getURL();
    String tgt = tourl.getURL();
    String cmd = guc +" "+src+" "+tgt;
    
    TSRMUtil.startUpInfo("....running binary:"+cmd);
    return gov.lbl.srm.util.TPlatformUtil.execWithOutput(cmd);
  }
}

