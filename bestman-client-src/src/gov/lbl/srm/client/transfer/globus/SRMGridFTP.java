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

package gov.lbl.srm.client.transfer.globus;

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

import gov.lbl.srm.client.transfer.ISRMFileTransfer;
//import org.apache.log4j.Logger;

import java.util.Date;
import java.util.Vector;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.lang.Thread;

import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.globus.gsi.gssapi.auth.Authorization;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

import gov.lbl.srm.client.main.util;

public class SRMGridFTP extends Thread implements ISRMFileTransfer {

protected static String myname;
private GlobusURL fromurl;
private GlobusURL tourl;
private int parallelism = 1;
//private int buffersize = 2097152;
private int buffersize = 0;
private GSSCredential sourcecredential = null;
private GSSCredential destcredential = null;
private Authorization sourceauth = null;
private Authorization destauth = null;

private int localServerMode = Session.SERVER_PASSIVE;
private int transferType = Session.TYPE_IMAGE;
//            Session.TYPE_ASCII
private int transferMode = GridFTPSession.MODE_EBLOCK;
//GridFTPSession.MODE_STREAM;
private DataChannelAuthentication dcau = DataChannelAuthentication.SELF;
//private DataChannelAuthentication dcau = DataChannelAuthentication.NONE;
//DataChannelAuthentication.SELF;
private int sessionprotection = GridFTPSession.PROTECTION_SAFE;
//            GridFTPSession.PROTECTION_CLEAR 

private boolean appendmode = false;

private SRMTransferMode tmode = SRMTransferMode.GET;
//			  SRMTransferMode.PUT  SRMTransferMode.THIRDPARTY

boolean userdcau = true;

private long remoteSize = -1;
private long localSize = -1;

private int portnumber = 2811;

private String status = null;

private boolean transferdone=false;

//added by viji on March 26th
private boolean transferok=false;

private long txfTimeInMilliSeconds = -1; 

private static Log logger; 
private java.util.logging.Logger _theLogger;
private boolean silent=false;
private boolean useLog=false;
private boolean debug = false;
private Vector inputVec = new Vector ();

//timeout
private int maxWait = 3000000;
private int waitDelay = 20000;

// AS 090707
private int blocksize=0;
// AS 090707 protected buffer size = blocksize
// the block size of the memory buffer used to read and write data
// to  disks  to  size  bytes.  The block size can be increased to improve 
// file transfer performance. This
// is not related to the extended block mode  block  size  used  to
// determine  the  ratio  of data to header for data transferred on
// the data channel.
// The  protection
// buffer  is  used to encrypt data on the data channel. The length
// of the protection buffer represents the largest encoded  message
// that is allowed on the data channel.  By default, the protection
// buffer is grown to match the internal buffer used. For efficient
// transfers,  pbsz  should  be sufficiently larger than blksize so
// that the wrapped buffer fits within the protection buffer.  Oth-
// erwise,  the  blksize  buffer  is broken into multiple pieces so
// that each write is less than pbsz when wrapped.
//private int pbsize = 1048576;
private int pbsize = 16384;

public SRMGridFTP() {
}

//added by viji

public void setLogger(Log logger, java.util.logging.Logger theLogger,
	boolean silent,boolean useLog, boolean debug) {
  this.logger = logger;
  this.silent = silent; 
  this.useLog = useLog;
  this._theLogger = theLogger;
  this.debug = debug;
}

public void setLogger(Log logger, boolean silent,boolean useLog,boolean debug) {
  this.logger = logger;
  this.silent = silent; 
  this.useLog = useLog;
  this.debug = debug;
}

public SRMGridFTP(String fromu, String tou) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
        myname = fromurl.getURL();
	} catch (Exception e) {
		e.printStackTrace();
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
		status = e.toString();
	}
}

public SRMGridFTP(GlobusURL fromu, GlobusURL tou) {
    try {
        fromurl = fromu;
        tourl = tou;
        myname = fromurl.getURL();
    } catch (Exception e) {
        e.printStackTrace();
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
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
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
		status = e.toString();
    }
}


public long getSourceSize() {
    if (tmode == SRMTransferMode.GET) 
		return remoteSize;
    else if (tmode == SRMTransferMode.PUT) 
		return localSize;
    else if (tmode == SRMTransferMode.THIRDPARTY) 
		return localSize;
	else
		return remoteSize;
}

public  long getTargetSize() {
    if (tmode == SRMTransferMode.GET) 
		return localSize;
    else if (tmode == SRMTransferMode.PUT) 
		return remoteSize;
    else if (tmode == SRMTransferMode.THIRDPARTY) 
		return remoteSize;
	else
		return localSize;
}

public String getTransferName() {
        return myname;
}

public boolean transferDone() {
	return transferdone;
}

public boolean transferOk() {
	return transferok;
}

public String getStatus() {
	return status;
}

public int getBlockSize() {
	return blocksize;
}

public void run() {
        transferSync();
}

public void transfer(String fromu, String tou) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
        myname = fromurl.getURL();
        this.start();
	} catch (Exception e) {
		e.printStackTrace();
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP.transfer",inputVec,silent,useLog);
		status = e.toString();
	}
}

public void transfer () {
        this.start();
}

public void cancel() {
    util.printMessage("GRIDFTP cancelled",logger,silent);
    inputVec = new Vector ();
    inputVec.addElement("GRIDFTP cancelled");
    util.printEventLog(_theLogger,"GridFTP-Cancelled",inputVec,silent,useLog); 
}

public void setTransferMode(SRMTransferMode tmodeu) {
        tmode = tmodeu;
}

public void setParallel(int parallelu) {
        parallelism = parallelu;
}

public void setDCAU(boolean dcauoption) {
    userdcau = dcauoption;
    if (dcauoption) dcau = DataChannelAuthentication.SELF;
	else dcau = DataChannelAuthentication.NONE;
}

public void setCredentials(GSSCredential credentials) {
        sourcecredential = credentials;
        destcredential = credentials;
}

public void setDestinationCredentials(GSSCredential dstCredentials) {
    destcredential = dstCredentials;
}

public void setSourceCredentials(GSSCredential srcCredentials) {
	sourcecredential = srcCredentials;
}

public void setSourceAuthorization(Authorization auth) {
    sourceauth = auth;
}

public void setDestinationAuthorization(Authorization auth) {
    destauth = auth;
}

public void setBufferSize(int size) {
        buffersize = size;
        buffersize = size;
}

// AS 090707
public void setBlockSize(int size) {
        blocksize = size;
		pbsize = size;
}

public void setAppendMode(boolean appendMode) {
    appendmode = appendMode;
}

public void setSourceUrl(String surl) throws IOException {
    GlobusURL tempurl = null;
    try {
        tempurl = new GlobusURL (surl);
        String urlPath = tempurl.getPath();
        if (urlPath == null || urlPath.length() == 0) {
			status = "The '" + tempurl.getURL() +
                      "' url does not specify the file location.";
            throw new IOException(status);
        }
        fromurl = tempurl; 
		myname = fromurl.getURL();
    } catch (Exception e) {
        e.printStackTrace();
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP.setSourceUrl",inputVec,silent,useLog);
		status = e.toString();
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
        e.printStackTrace();
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP.setDestinationUrl",inputVec,silent,useLog);
		status = e.toString();
    }
}

public void setUseThirdPartyCopy() {
    tmode = SRMTransferMode.THIRDPARTY;
}

public void setSessionType(int sessionType) {
    transferType = sessionType;
}

public void setSessionMode(int sessionMode) {
    transferMode = sessionMode;
}

public void setSessionProtection(int sessionProtection) {
    sessionprotection = sessionProtection;
}

public void transferSync(String fromu, String tou) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
	    myname = fromurl.getURL();
	} catch (Exception e) {
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP.transferSync",inputVec,silent,useLog);
        if(!transferok) {
		  e.printStackTrace();
		  status = e.toString();
        }
	}
     
    try {
        long st = System.currentTimeMillis();
        if (tmode == SRMTransferMode.GET) {
            //viji
            if(debug) {
             util.printMessage("GridFTP GET: ",logger,silent);
            }
            inputVec = new Vector ();
            inputVec.addElement("GridFTP GET: ");
            util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
            get();
            // if type = ASCII, file size before and after transfer may
            // differ, otherwise they shouldn't
            if (transferType != Session.TYPE_ASCII) {
                File f = new File(tourl.getPath());
				localSize = f.length();
                //viji
                inputVec = new Vector ();
                inputVec.addElement("GET: comparing size: " + tourl.getPath() + 
					"  " + remoteSize + " == " + f.length());
                util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
                if(debug) {
                util.printMessage("GET: comparing size: " + tourl.getPath() + 
					"  " + remoteSize + " == " + f.length(),logger,silent);
                }
            }
        }
        else if (tmode == SRMTransferMode.PUT) {
            
            if(debug) {
            util.printMessage("GridFTP PUT",logger,silent);
            }
            inputVec = new Vector ();
            inputVec.addElement("GridFTP Put");
            util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
            put();
            // if type = ASCII, file sizes before and after transfer may
            // differ, otherwise they shouldn't
            if (transferType != Session.TYPE_ASCII) {
                File f = new File(fromurl.getPath());
                inputVec = new Vector ();
                inputVec.addElement("PUT: comparing size: " + 
					fromurl.getPath() + "  " + 
					remoteSize + " == " + f.length());
                util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
                if(debug) {
                util.printMessage("PUT: comparing size: " + fromurl.getPath() + 
					"  " + remoteSize + " == " + f.length(),logger,silent);
                }
            }
        }
        else if (tmode == SRMTransferMode.THIRDPARTY) {
            thirdparty();
        }
        else {
        }
        long ft = System.currentTimeMillis(); 
        //viji
        transferok = true;
        inputVec = new Vector ();
        inputVec.addElement("DONE Gridftp: " + fromurl.getPath() + 
			" time took (milliseconds)=" + (ft-st));
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        if(debug) {
        util.printMessage("DONE Gridftp: " + fromurl.getPath() + 
			" time took (milliseconds)=" + (ft-st),logger,silent);
        } 
        this.txfTimeInMilliSeconds = ft-st;
    } catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"Exception",inputVec,silent,useLog);
        if(!transferok) {
          e.printStackTrace();
		  status = e.toString();
        }
		util.printMessage(e.toString(),logger,silent);
        inputVec = new Vector ();
        inputVec.addElement(e.toString());
        util.printEventLog(_theLogger,"SRMGridFTP.transferSync",inputVec,silent,useLog);
    }
}

public void transferSync() {
    //viji
    inputVec = new Vector ();
	inputVec.addElement("From " + fromurl.getURL());
	inputVec.addElement("To   " + tourl.getURL());
    inputVec.addElement("client specified dcau="+userdcau);
    inputVec.addElement("local server mode="+localServerMode);
    inputVec.addElement("local server mode="+Thread.currentThread().getName());
    inputVec.addElement("parallelism="+parallelism);
    inputVec.addElement("buffersize="+buffersize);
    inputVec.addElement("blocksize="+blocksize);
    util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
    if(debug) {
	util.printMessage("From " + fromurl.getURL(),logger,silent);
	util.printMessage("To   " + tourl.getURL(),logger,silent);
    //util.printMessage("dcau="+dcau.toString(),logger,silent);
    util.printMessage("client specified dcau="+userdcau,logger,silent);
    util.printMessage("local server mode="+localServerMode,logger,silent);
    util.printMessage("local server mode="+Thread.currentThread().getName(),
			logger,silent);
    util.printMessage("parallelism="+parallelism, logger,silent);
    util.printMessage("buffersize="+buffersize, logger,silent);
    util.printMessage("blocksize="+blocksize, logger,silent);
    }

    try {
        long st = System.currentTimeMillis();
		if (tmode == SRMTransferMode.GET) {
		    //System.out.println("GridFTP GET: ");
		    get();

		    // if type = ASCII, file size before and after transfer may
		    // differ, otherwise they shouldn't
		    if (transferType != Session.TYPE_ASCII) {
		        File f = new File(tourl.getPath());
			    localSize = f.length();
                //viji
                inputVec = new Vector ();
                inputVec.addElement("GET: comparing size: " + 
					tourl.getPath() + " " + remoteSize + " == " +
                      f.length()); 
                util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
                if(debug) {
		        util.printMessage("GET: comparing size: " + 
					tourl.getPath() + " " + remoteSize + " == " + 
						f.length(),logger,silent);
                }
		    }
		}
		else if (tmode == SRMTransferMode.PUT) {
            inputVec = new Vector();
		    inputVec.addElement("GridFTP PUT");
            util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
            if(debug) {
		    util.printMessage("GridFTP PUT",logger,silent);
            }
		    put();
		    // if type = ASCII, file sizes before and after transfer may
		    // differ, otherwise they shouldn't
		    if (transferType != Session.TYPE_ASCII) {
		        File f = new File(fromurl.getPath());
                inputVec = new Vector ();
		        inputVec.addElement("PUT: comparing size: " + 
					fromurl.getPath() + " " + remoteSize + " == " + 
						f.length());
                util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
                if(debug) {
		        util.printMessage("PUT: comparing size: " + 
					fromurl.getPath() + " " + remoteSize + " == " + 
						f.length(),logger,silent);
                }
		    }
		}
		else if (tmode == SRMTransferMode.THIRDPARTY) {
		    thirdparty();
		}
		else {
		}
		long ft = System.currentTimeMillis();
        transferok = true;
        //viji
        inputVec = new Vector ();
		inputVec.addElement("DONE Gridftp: " + fromurl.getPath() + 
			" time took (milliseconds) =" + (ft-st));
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        if(debug) {
		util.printMessage("DONE Gridftp: " + fromurl.getPath() + 
			" time took (milliseconds) =" + (ft-st),logger,silent);
        }
		this.txfTimeInMilliSeconds = ft-st;
    } catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        if(!transferok) {
          e.printStackTrace();
	      status = e.toString();
        }
        util.printMessage(e.toString(),logger,silent);
    }
    transferdone = true;
}

public void thirdparty() throws IOException {
	try {
		GridFTPClient fromclient = new GridFTPClient(fromurl.getHost(), fromurl.getPort());
		GridFTPClient toclient = new GridFTPClient(tourl.getHost(), tourl.getPort());
        fromclient.setClientWaitParams(maxWait,waitDelay);
		fromclient.authenticate(sourcecredential);
// AS 090707
		fromclient.setProtectionBufferSize(pbsize);
		fromclient.setType(transferType); // GridFTPSession.TYPE_IMAGE
		fromclient.setMode(transferMode); // GridFTPSession.MODE_STREAM

        // AS 04/09/2009
        if (fromclient.isFeatureSupported("DCAU")) {
          inputVec = new Vector ();
          inputVec.addElement("SOURCE gsiftp dcau feature supported");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### SOURCE gsiftp dcau feature supported ",logger,silent);
              if (dcau == DataChannelAuthentication.NONE) 
				util.printMessage("### SOURCE gsiftp dcau set to false",logger,silent);
			  else 
				util.printMessage("### SOURCE gsiftp dcau set to true",logger,silent);
          }
          //fromclient.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          fromclient.setDataChannelAuthentication(dcau);
          //fromclient.setDataChannelAuthentication(DataChannelAuthentication.SELF);
          if (dcau == DataChannelAuthentication.SELF) {
            fromclient.setDataChannelProtection(sessionprotection);
          }
        }
        else {
          inputVec = new Vector ();
          inputVec.addElement("SOURCE gsiftp dcau feature not supported");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### SOURCE gsiftp dcau feature not supported",logger,silent);
          }
          //fromclient.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          fromclient.setLocalNoDataChannelAuthentication();
        }


        toclient.setClientWaitParams(maxWait,waitDelay);
		toclient.authenticate(destcredential);
// AS 090707
		toclient.setProtectionBufferSize(pbsize);
		toclient.setType(transferType); // GridFTPSession.TYPE_IMAGE
		toclient.setMode(transferMode); // GridFTPSession.MODE_STREAM


        // AS 04/09/2009
        if (toclient.isFeatureSupported("DCAU")) {
          inputVec = new Vector ();
          inputVec.addElement("TARGET gsiftp dcau feature supported");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### TARGET gsiftp dcau feature supported",logger,silent);
              if (dcau == DataChannelAuthentication.NONE) 
				util.printMessage("### TARGET gsiftp dcau set to false",logger,silent);
			  else 
				util.printMessage("### TARGET gsiftp dcau set to true",logger,silent);
          }
          //toclient.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          toclient.setDataChannelAuthentication(dcau);
          //toclient.setDataChannelAuthentication(DataChannelAuthentication.SELF);
          if (dcau == DataChannelAuthentication.SELF) {
            toclient.setDataChannelProtection(sessionprotection);
          }
        }
        else {
          inputVec = new Vector ();
          inputVec.addElement("TARGET gsiftp dcau feature not supported");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### TARGET gsiftp dcau feature not supported",logger,silent);
          }
          //toclient.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          toclient.setLocalNoDataChannelAuthentication();
        }


		if (parallelism > 1)
			fromclient.setOptions(new RetrieveOptions(parallelism));

		if (transferMode == GridFTPSession.MODE_STREAM)
			fromclient.transfer(fromurl.getPath(), toclient, tourl.getPath(), appendmode, null);
		else if (transferMode == GridFTPSession.MODE_EBLOCK)
			fromclient.extendedTransfer(fromurl.getPath(), toclient, tourl.getPath(), null);
		else {
			status = "Unknown transferMode: " + transferMode;
			throw new IOException (status);
		}
		transferdone = true;
		transferok = true;
	    if (transferType != Session.TYPE_ASCII) {
	        remoteSize = toclient.getSize(tourl.getPath());
	        localSize = fromclient.getSize(fromurl.getPath());
		}
		fromclient.close();
		toclient.close();
	} catch (org.globus.ftp.exception.ServerException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
		 e.printStackTrace();
		 status = e.toString();
        }
	} catch (org.globus.ftp.exception.ClientException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
		 e.printStackTrace();
		 status = e.toString();
        }
    } catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
		 e.printStackTrace();
		 status = e.toString();
        }
    }
}

public void get() throws IOException {
    //most lines are commented by VIJI while testing to make it work with 
    //dcache
	try {
		if (fromurl.getPort() > 0) portnumber = fromurl.getPort();
        //System.out.println("Connecting to " + fromurl.getHost());
        //System.out.println("on port " + fromurl.getPort());
	    GridFTPClient client = new GridFTPClient(fromurl.getHost(), fromurl.getPort());
	
		if (sourcecredential == null) 
			sourcecredential = (new SRMGSICredential()).getCredential();

        client.setClientWaitParams(maxWait,waitDelay);
	    client.authenticate(sourcecredential); 

// AS 090707
	    client.setProtectionBufferSize(pbsize);

	    client.setType(transferType);
		if (parallelism == 1) setSessionMode(GridFTPSession.MODE_STREAM);
	    client.setMode(transferMode);

         /*
          // the fermi's server has a problem with this
          // setting dcau = SELF or NONE
          // both causes error. - Junmin March 09. 2006
          //
        client.setDataChannelAuthentication(dcau);
        if (dcau == DataChannelAuthentication.SELF)
        client.setDataChannelProtection(sessionprotection);
        */


        //viji code commented for this new additions March 29, 07
        /*
        if(client.isFeatureSupported("DCAU"))
	      client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
        else
	      client.setLocalNoDataChannelAuthentication();

        client.setClientWaitParams(0x7fffffff, 1000);
        */

        // AS 03/27/07 below
        // 451 gridftp error
        if (client.isFeatureSupported("DCAU")) {
          inputVec = new Vector ();
          inputVec.addElement("gsiftp dcau on");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau on",logger,silent);
          } 
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          client.setDataChannelAuthentication(dcau);
          //client.setDataChannelAuthentication(DataChannelAuthentication.SELF);
          if (dcau == DataChannelAuthentication.SELF) {
            client.setDataChannelProtection(sessionprotection);
          }
        }
        else {
          inputVec = new Vector ();
          inputVec.addElement("gsiftp dcau off");
          util.printEventLog(_theLogger,"DCAU",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau off",logger,silent);
          } 
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          client.setLocalNoDataChannelAuthentication();
        }

        client.setClientWaitParams(0x7fffffff, 1000);

        //System.out.println(">>>>>>buffersize="+buffersize);
        if(buffersize != 0) {
          client.setLocalTCPBufferSize(buffersize);
        }

	    //client.setDataChannelProtection(sessionprotection);

	    if (localServerMode == Session.SERVER_ACTIVE) {
	        client.setPassive();
	        client.setLocalActive();
	    } else {
	        client.setLocalPassive();
	        client.setActive();
	    }

	    DataSink sink = null;
        if(buffersize != 0) {
          client.setTCPBufferSize(buffersize);
        }
// added 5/5/08 AS
		if (parallelism == 1) {
          if(debug) {
          	util.printMessage("### gsiftp passive mode",logger,silent);
          } 
        	client.setPassiveMode(true);
		}
        if(buffersize != 0) {
          client.setTCPBufferSize(buffersize);
        }

	    if (transferMode == GridFTPSession.MODE_EBLOCK) {
	        if (localServerMode == Session.SERVER_PASSIVE) 
				client.setOptions(new RetrieveOptions(parallelism));
// AS 090707
			if (blocksize > 0) {
		        sink = new FileRandomIO
					(new java.io.RandomAccessFile(tourl.getPath(), "rw"), blocksize);
			}
			else {
		        sink = new FileRandomIO
					(new java.io.RandomAccessFile(tourl.getPath(), "rw"));
			}
	         
	        long size = client.getSize(fromurl.getPath());
	        client.extendedGet(fromurl.getPath(), size, sink, null);
	        remoteSize = size;
	    } else { 
	        sink = new DataSinkStream(new FileOutputStream(tourl.getPath()));
	        client.get(fromurl.getPath(), sink, null);
	    }
		transferdone = true;
		transferok = true;
	
	    // if type = ASCII, file size before and after transfer may
	    // differ, otherwise they shouldn't

	    if (transferType != Session.TYPE_ASCII) {
	        remoteSize = client.getSize(fromurl.getPath());
	    }
	    client.close();
    } catch (org.globus.ftp.exception.ServerException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        } 
    } catch (org.globus.ftp.exception.ClientException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        } 
    } catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        boolean exceptionDuringClose;
        ByteArrayOutputStream bss = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(bss);
        e.printStackTrace(ps);
        String str = bss.toString();
        int idx = str.indexOf("org.globus.ftp.FTPClient.close");
        if(idx != -1) {
          inputVec.addElement("Exception occured during close");          
          exceptionDuringClose=true;
        }

        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);

        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        } 
    }

}

public void put() throws IOException {
	try {
		if ((parallelism > 1) && (transferMode == Session.MODE_STREAM)) {
            if(debug) {
	        util.printMessage("Gridftp PUT does not work with STREAM mode",logger,silent);
            }
			status = "Gridftp PUT does not work with STREAM mode";
			throw new IOException (status);
		}
		localServerMode = Session.SERVER_ACTIVE;
		if (tourl.getPort() > 0) portnumber = tourl.getPort();
	    GridFTPClient client = new GridFTPClient(tourl.getHost(), tourl.getPort());

		if (destcredential == null) destcredential = (new SRMGSICredential()).getCredential();

        client.setClientWaitParams(maxWait,waitDelay);
	    client.authenticate(destcredential); 
// AS 090707
	    client.setProtectionBufferSize(pbsize);

	    client.setType(transferType);
//		if (parallelism == 1) setSessionMode(GridFTPSession.MODE_STREAM);
	    client.setMode(transferMode);

        //added on march 29 for the fix with dcau

        // AS 03/27/07 below
        // 451 gridftp error
        if (client.isFeatureSupported("DCAU")) {
          inputVec = new Vector ();
          inputVec.addElement("### gsiftp dcau on");
          util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau on",logger,silent);
          }
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          client.setDataChannelAuthentication(dcau);
          //client.setDataChannelAuthentication(DataChannelAuthentication.SELF);
          if (dcau == DataChannelAuthentication.SELF) {
            client.setDataChannelProtection(sessionprotection);
          }
        }
        else {
          inputVec = new Vector ();
          inputVec.addElement("### gsiftp dcau off");
          util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau off",logger,silent);
          } 
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          client.setLocalNoDataChannelAuthentication();
        }

        client.setClientWaitParams(0x7fffffff, 1000);

        //if (localServerMode == Session.SERVER_ACTIVE) {
            client.setPassive();
            client.setLocalActive();
        //} else {
          //client.setLocalPassive();
          //client.setActive();
        //}


        //viji's code commented for gsiftp fix on March 29, 07
        /*
        if(client.isFeatureSupported("DCAU"))
	      client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
        else
	      client.setLocalNoDataChannelAuthentication();

        client.setClientWaitParams(0x7fffffff, 1000);
        */

        RandomAccessFile randomAFile =
             new RandomAccessFile(fromurl.getPath(),"r");

        DataSource source;
// AS 090707
	    if ((transferMode == GridFTPSession.MODE_EBLOCK) && (blocksize > 0)) {
		    source = new FileRandomIO(randomAFile, blocksize);
		}
		else {
		    source = new FileRandomIO(randomAFile);
		}

        if(buffersize != 0) {
          client.setTCPBufferSize(buffersize);
        }

        client.put(tourl.getPath(), source, null);

		transferdone = true;
		transferok = true;
	    // if type = ASCII, file sizes before and after transfer may
	    // differ, otherwise they shouldn't
	    if (transferType != Session.TYPE_ASCII) {
	        remoteSize = client.getSize(tourl.getPath());
	    }
	    client.close();
    } catch (org.globus.ftp.exception.ServerException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        }
    } catch (org.globus.ftp.exception.ClientException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        }
    } catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
        util.printMessage(e.toString(),logger,silent);
        if(!transferok) {
         e.printStackTrace();
         status = e.toString();
        }
    }

}



//
// added by junmin
//

    public void doNERSCSetting() {
    if(debug) {
    util.printMessage("..........for NERSC HPSS..............",logger,silent);
    }
    setDCAU(false);
    setSessionMode(org.globus.ftp.Session.MODE_STREAM);
    setServerModeActive();
    }

public long getSourceFileSize() {
	try {
		if (fromurl.getPort() > 0) portnumber = fromurl.getPort();
		GridFTPClient client = 
			new GridFTPClient(fromurl.getHost(), fromurl.getPort());
		
		if (sourcecredential == null) 
			sourcecredential = (new SRMGSICredential()).getCredential();

        client.setClientWaitParams(maxWait,waitDelay);
		client.authenticate(sourcecredential); 
		client.setProtectionBufferSize(16384);
		client.setType(transferType);
		client.setMode(transferMode);
		//client.setDataChannelAuthentication(dcau);
		//client.setDataChannelProtection(sessionprotection);

        // AS 03/27/07 below
        // 451 gridftp error
        if (client.isFeatureSupported("DCAU")) {
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          inputVec = new Vector ();
          inputVec.addElement("dcau off");
          util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau off",logger,silent);
          }
          client.setDataChannelAuthentication(dcau);
          //client.setDataChannelAuthentication(DataChannelAuthentication.SELF);
          if (dcau == DataChannelAuthentication.SELF) {
            client.setDataChannelProtection(sessionprotection);
          }
        }
        else {
          inputVec = new Vector ();
          inputVec.addElement("dcau on");
          util.printEventLog(_theLogger,"SRMGridFTP",inputVec,silent,useLog);
          if(debug) {
          util.printMessage("### gsiftp dcau on",logger,silent);
          }
          //client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
          client.setLocalNoDataChannelAuthentication();
        }

		if (localServerMode == Session.SERVER_ACTIVE) {
			client.setPassive();
			client.setLocalActive();
		} else {
			client.setLocalPassive();
			client.setActive();
		}

// AS 090707
//		DataSink sink = null;
		long size = client.getSize(fromurl.getPath());
		client.close();
 
		return size;
		
	} catch (org.globus.ftp.exception.ServerException e) { 
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
		throw new RuntimeException (e.toString());
	} catch (org.globus.ftp.exception.ClientException e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
		throw new RuntimeException (e.toString());
	} catch (Exception e) {
        inputVec = new Vector ();
        inputVec.addElement(e.getMessage());
        util.printEventLog(_theLogger,"TransferSync",inputVec,silent,useLog);
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

