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

import org.globus.ftp.Session;
import org.globus.ftp.GridFTPSession;
import org.globus.ftp.DataSource;
import org.globus.ftp.FileRandomIO;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.DataChannelAuthentication;
import org.globus.ftp.RetrieveOptions;
import org.globus.ftp.DataSinkStream;
import org.globus.ftp.DataSourceStream;
import org.globus.ftp.DataSink;
import org.globus.ftp.exception.FTPException;
import org.globus.ftp.exception.ServerException;
import org.globus.ftp.vanilla.FTPServerFacade;

import org.globus.util.GlobusURL;

import gov.lbl.srm.transfer.ISRMFileTransfer;
import gov.lbl.srm.util.*;
//import org.apache.log4j.Logger;

import java.util.Date;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.lang.Thread;

import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.globus.gsi.gssapi.auth.Authorization;

public class SRMGridFTP extends Thread implements ISRMFileTransfer {

  protected static String myname;
  private GlobusURL fromurl;
  private GlobusURL tourl;
  private int parallelism = 1;
  private int buffersize = 0;//1048576;
  private GSSCredential sourcecredential = null;
  private GSSCredential destcredential = null;
  private Authorization sourceauth = null;
  private Authorization destauth = null;
  
  private int localServerMode = Session.SERVER_PASSIVE;
  private int transferType = Session.TYPE_IMAGE;
  //            Session.TYPE_ASCII
  private int transferMode = GridFTPSession.MODE_EBLOCK;
  //            Session.MODE_STREAM
  private DataChannelAuthentication dcau = DataChannelAuthentication.SELF;
  //    private DataChannelAuthentication dcau = DataChannelAuthentication.NONE;
  //            DataChannelAuthentication.NONE
  private int sessionprotection = GridFTPSession.PROTECTION_SAFE;
  //    private int sessionprotection = GridFTPSession.PROTECTION_CLEAR 

  private boolean appendmode = false;
  private SRMTransferMode tmode = SRMTransferMode.GET;
  //			  SRMTransferMode.PUT  SRMTransferMode.THIRDPARTY
  
  boolean userdcau = true;
  
  private long remoteSize = -1;
  private long localSize = -1;
  
  private String status = null;
  
  private boolean transferdone=false;
  
  private long txfTimeInMilliSeconds = -1; 
  
  public SRMGridFTP() {
  }
  /*
  public SRMGridFTP(String fromu, String tou) {
    try {
      fromurl = new GlobusURL (fromu);
      tourl = new GlobusURL (tou);
      myname = fromurl.getURL();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  }
  */
  public SRMGridFTP(GlobusURL fromu, GlobusURL tou) {
    try {
      fromurl = fromu;
      tourl = tou;
      myname = fromurl.getURL();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  }
  
  public SRMGridFTP(GlobusURL fromu, GlobusURL tou, SRMTransferMode tmodeu) {
    try {
      fromurl = fromu;
      tourl = tou;
      tmode = tmodeu;
        myname = fromurl.getURL();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  }
  
  public String getTransferName() {
    return myname;
  }

  public boolean transferDone() {
    return transferdone;
  }
  
  public String getStatus() {
    return status;
  }
  
  public void run() {
    transferSync();
  }
  
  public void transfer () {
    this.start();
  }
  
  public void cancel() {
    TSRMUtil.startUpInfo("GRIDFTP cancelled");
  }
  
  public void setTransferMode(SRMTransferMode tmodeu) {
    tmode = tmodeu;
  }
  
  public void setParallel(int parallelu) {
    parallelism = parallelu;
  }
  
  public void setDCAU(boolean dcauoption) {
    userdcau = dcauoption;
    if (dcauoption) {
      dcau = DataChannelAuthentication.SELF;      
    } else {
      dcau = DataChannelAuthentication.NONE;
    }
  }
  
  public void setSourceCredentials(GSSCredential cred) {
    sourcecredential = cred;
  }

  public void setDestinationCredentials(GSSCredential cred) {
    destcredential = cred;
  }

  public void setSourceAuthorization(Authorization auth) {
    sourceauth = auth;
  }
  
  public void setDestinationAuthorization(Authorization auth) {
    destauth = auth;
  }
  
  public void setBufferSize(int size) {
	TSRMLog.info(this.getClass(), null, "event=setBufferSize", "value="+size);
    buffersize = size;
  }
  
  public void setAppendMode(boolean appendMode) {
    appendmode = appendMode;
  }
  
  public void setUseThirdPartyCopy() {
    tmode = SRMTransferMode.THIRDPARTY;
  }
  
  public void setSessionType(int sessionType) {
    transferType = sessionType;
  }
  
  public void setSessionMode(int sessionMode) {
    transferMode = sessionMode;
    TSRMUtil.startUpInfo("assigning transferMode value="+transferMode+" eblock="+GridFTPSession.MODE_EBLOCK);
  }
  
  public void setSessionProtection(int sessionProtection) {
    sessionprotection = sessionProtection;
    TSRMUtil.startUpInfo("assigning session protection value="+sessionProtection+" safe="+GridFTPSession.PROTECTION_SAFE);
  }
  
  public void transferSync() {
    TSRMUtil.startUpInfo("From " + fromurl.getURL());
    TSRMUtil.startUpInfo("To   " + tourl.getURL());
    //TSRMUtil.startUpInfo("dcau?"+ dcau.toFtpCmdArgument());
    //TSRMUtil.startUpInfo(org.globus.ftp.DataChannelAuthentication.SELF);
    //TSRMUtil.startUpInfo(org.globus.ftp.DataChannelAuthentication.NONE);
    TSRMUtil.startUpInfo("dcau = "+dcau);
    TSRMUtil.startUpInfo("client specified dcau = "+userdcau);
    TSRMUtil.startUpInfo("local server mode="+localServerMode);
    try {
      long st = System.currentTimeMillis();
      if (tmode == SRMTransferMode.GET) {
	//TSRMUtil.startUpInfo("GridFTP GET: ");
	get();
	
	// if type = ASCII, file size before and after transfer may
	// differ, otherwise they shouldn't
	if (transferType != Session.TYPE_ASCII) {
	  //File f = new File(tourl.getPath());
	  //localSize = f.length();
	  //TSRMUtil.startUpInfo("GET: comparing size: @source:" + remoteSize + ", @target:" + f.length() );
	}
      } else if (tmode == SRMTransferMode.PUT) {
	TSRMUtil.startUpInfo("GridFTP PUT");
	put();
	// if type = ASCII, file sizes before and after transfer may
	// differ, otherwise they shouldn't
	if (transferType != Session.TYPE_ASCII) {
	  //File f = new File(fromurl.getPath());
	  //TSRMUtil.startUpInfo("PUT: comparing size: @target:" + remoteSize + ", @source:" + f.length());
	}
      } else if (tmode == SRMTransferMode.THIRDPARTY) {      
	thirdparty();
      } else {      
      }

      long ft = System.currentTimeMillis();
      TSRMUtil.startUpInfo("DONE Gridftp: " + fromurl.getURL()  + ", time took (milliseconds) =" + (ft-st));
      this.txfTimeInMilliSeconds = ft-st;
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    } finally {
      transferdone = true;
    }
  }
 
  private void setFlagsOnSourceClient(GridFTPClient fromClient) {
    try {
      fromClient.setProtectionBufferSize(16384);
      fromClient.setType(transferType);// GridFTPSession.TYPE_IMAGE
      fromClient.setMode(transferMode); // GridFTPSession.MODE_STREAM
      if (parallelism > 1) {
	fromClient.setOptions(new RetrieveOptions(parallelism));
      }
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  } 

  private void setFlagsOnTargetClient(GridFTPClient tgtClient) {
    try {
      tgtClient.setProtectionBufferSize(16384);
      tgtClient.setType(transferType); // GridFTPSession.TYPE_IMAGE
      tgtClient.setMode(transferMode); // GridFTPSession.MODE_STREAM
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  }

  public void thirdparty() throws IOException {
    TSRMLog.debug(this.getClass(), null, "event=3rdPartyTxf", null);
    GridFTPClient fromClient= initGridFTPClient(fromurl, sourcecredential);
    GridFTPClient toClient = initGridFTPClient(tourl, destcredential);
    try {
      setFlagsOnSourceClient(fromClient);
      setFlagsOnTargetClient(toClient);
      
      /* does not work for GT3
	 try {
	 toclient.allocate(fromclient.getSize(fromurl.getPath()));
	 } catch (Exception e) {
	 TSRMLog.exception(SRMGridFTP.class, "noHarmDone!", e);		    
	 }
      */
      
      if (transferMode == GridFTPSession.MODE_STREAM)
	fromClient.transfer(fromurl.getPath(), toClient, tourl.getPath(), appendmode, null);
      else if (transferMode == GridFTPSession.MODE_EBLOCK)
	fromClient.extendedTransfer(fromurl.getPath(), toClient, tourl.getPath(), null);
      else {
	status = "Unknown transferMode: " + transferMode;
	throw new IOException (status);
      }
      transferdone = true;
      if (transferType != Session.TYPE_ASCII) {
	//remoteSize = toclient.getSize(tourl.getPath());
	//localSize = fromclient.getSize(fromurl.getPath());
      }
      fromClient.close();
      toClient.close();
    } catch (org.globus.ftp.exception.ServerException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (org.globus.ftp.exception.ClientException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    } 
  }
  
  public void get() throws IOException {    
    if (parallelism > 1) {		
      transferMode = GridFTPSession.MODE_EBLOCK;
    } else {
      transferMode = Session.MODE_STREAM;
    }
    
    try {
      GridFTPClient client = initGridFTPClient(fromurl, sourcecredential);
      setFlagsOnSourceClient(client);
      handleDCAU(client);

      client.setClientWaitParams(0x7fffffff, 1000);
	  if (buffersize > 0) {
      client.setLocalTCPBufferSize(buffersize);
	  }
      //coment out to test. though I donnt believe it matters 0319/2008 client.setTCPBufferSize(buffersize);
      
      handleServerMode(client);

      if (parallelism == 1) { // this line has to be here. Putting it earlier would end with exception
	client.setPassiveMode(true); // using passive mode so client under NAT would able to get remote files
      }
      
      DataSink sink = null;
      if (transferMode == GridFTPSession.MODE_EBLOCK) {		
	if (localServerMode == Session.SERVER_PASSIVE) {
	  client.setOptions(new RetrieveOptions(parallelism));
	}
	
	sink = new FileRandomIO(new java.io.RandomAccessFile(tourl.getPath(), "rw"));
	
	long size = remoteSize;
	if (size == -1) {
	  remoteSize = client.getSize(fromurl.getPath());
	  size = remoteSize;
	}
	client.extendedGet(fromurl.getPath(), size, sink, null);
	//remoteSize = size;
      } else { 
	sink = new DataSinkStream(new FileOutputStream(tourl.getPath()));
	client.get(fromurl.getPath(), sink, null);
      }
      
      transferdone = true;
      
      // if type = ASCII, file size before and after transfer may
      // differ, otherwise they shouldn't
      if (transferType != Session.TYPE_ASCII) {
	//remoteSize = client.getSize(fromurl.getPath());
      }
      client.close();
    } catch (org.globus.ftp.exception.ServerException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (org.globus.ftp.exception.ClientException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }    
  }
  
  public void put() throws IOException {
    try {
      if ((parallelism > 1) && (transferMode == Session.MODE_STREAM)) {
	TSRMUtil.startUpInfo("Gridftp PUT does not work with STREAM mode");
	status = "Gridftp PUT does not work with STREAM mode";
	throw new IOException (status);
      }
      
      GridFTPClient client = initGridFTPClient(tourl, destcredential);
      setFlagsOnTargetClient(client);

      handleDCAU(client);

      client.setClientWaitParams(0x7fffffff, 1000);

      localServerMode = Session.SERVER_ACTIVE;
      handleServerMode(client);

      /*
      // Junmin Gu 2007  for EBLOCK
      DataSource source = null;
      if (transferMode == GridFTPSession.MODE_EBLOCK) {
      if (localServerMode == Session.SERVER_ACTIVE) 
      client.setOptions(new RetrieveOptions(parallelism));
      source = new FileRandomIO(new java.io.RandomAccessFile(fromurl.getPath(), "r"));
      client.extendedPut(tourl.getPath(), source, null);
      } else {
      source = new DataSourceStream(new FileInputStream(fromurl.getPath()));
      client.put(tourl.getPath(), source, null);
      }
      */
      java.io.RandomAccessFile randomAFile =
	new java.io.RandomAccessFile(fromurl.getPath(),"r");
      
      DataSource source = new FileRandomIO(randomAFile);
	  if (buffersize > 0) {
       	client.setTCPBufferSize(buffersize);
	  }
      
      /* does not work for GT3  
	 try {
	 File srcFile = new File(fromurl.getPath());
	 long srcSize = srcFile.length();
	 client.allocate(srcSize);
	 }  catch (Exception e) {
	 TSRMLog.exception(SRMGridFTP.class, "noHarmDone!!", e);		    
	 }
      */
      client.put(tourl.getPath(), source, null);
      
      transferdone = true;
      // if type = ASCII, file sizes before and after transfer may
      // differ, otherwise they shouldn't
      if (transferType != Session.TYPE_ASCII) {
	//remoteSize = client.getSize(tourl.getPath());
      }
      client.close();
    } catch (org.globus.ftp.exception.ServerException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (org.globus.ftp.exception.ClientException e) {
      e.printStackTrace();
      status = e.toString();
    } catch (Exception e) {
      e.printStackTrace();
      status = e.toString();
    }
  }
  
  //
  // added by junmin
  //
  
  public static GridFTPClient initGridFTPClient(GlobusURL url, GSSCredential cred) {
    return initGridFTPClient(url.getHost(), url.getPort(), cred);
  }
  
  public static GridFTPClient initGridFTPClient(String host, int port, GSSCredential cred) {
    try {
      GridFTPClient result = new GridFTPClient(host, port);
      
      if (cred != null) {
	result.authenticate(cred);
      }
      TSRMLog.debug(SRMGridFTP.class, "useWaitDelay="+gov.lbl.srm.server.Config._gsiftpClientWaitParamsWaitDelayMillis, null, null);
      result.setClientWaitParams(gov.lbl.srm.server.Config._gsiftpClientWaitParamsMaxWaitMillis, gov.lbl.srm.server.Config._gsiftpClientWaitParamsWaitDelayMillis);
      return result;	
    } catch (Exception e) {
      e.printStackTrace();
      TSRMLog.exception(SRMGridFTP.class, "details:", e);
      throw new RuntimeException(e.toString());
    }
  }
    
  public void doNERSCSetting() {
    TSRMUtil.startUpInfo("..........for NERSC HPSS..............");
    setDCAU(false);
    setSessionMode(org.globus.ftp.Session.MODE_STREAM);
    setServerModeActive();
  }

  private void handleDCAU(GridFTPClient client) {
    // the fermi's server has a problem with this
    // setting dcau = SELF or NONE  
    // both causes error. - Junmin March 09. 2006
    try {
      // for fermi, AS 03/27/07 below, 451 gsiftp error     
      if (client.isFeatureSupported("DCAU")) {
	TSRMUtil.startUpInfo("### gsiftp dcau on");
	//   client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
	client.setDataChannelAuthentication(dcau);
	/// client.setDataChannelAuthentication(DataChannelAuthentication.SELF);
	if (dcau == DataChannelAuthentication.SELF) {
	  client.setDataChannelProtection(sessionprotection);
	}
      } else {
	TSRMUtil.startUpInfo("### gsiftp dcau off");
	//    client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
	client.setLocalNoDataChannelAuthentication();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  private void handleServerMode(GridFTPClient client) {
    try {
      if (localServerMode == Session.SERVER_ACTIVE) {
      	org.globus.ftp.HostPort hp=client.setPassive(); 
	client.setLocalActive();
      } else {
	org.globus.ftp.HostPort hp=client.setLocalPassive(); 
	client.setActive();
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e.toString());
    }
  }

  public long getSourceFileSize() {
    GridFTPClient client = initGridFTPClient(fromurl.getHost(), fromurl.getPort(), sourcecredential);
    setFlagsOnSourceClient(client);
    
    // commented by Junmin, March 09, 2006
    //client.setDataChannelAuthentication(dcau);
    //client.setDataChannelProtection(sessionprotection);
    
    handleDCAU(client);   
    handleServerMode(client);

    try {                             
      DataSink sink = null;
      long size = client.getSize(fromurl.getPath());
      client.close();
      remoteSize = size; // added by Junmin
      return size;
      
    } catch (org.globus.ftp.exception.ServerException e) { 
      throw new RuntimeException (e.toString());
    } catch (Exception e) {
      throw new RuntimeException (e.toString());
    }
  }
  
  public long getTransferTimeInMilliSeconds() {
    return this.txfTimeInMilliSeconds;
  }
  
  public void setServerModeActive() {
    localServerMode = Session.SERVER_ACTIVE;
  }
}

