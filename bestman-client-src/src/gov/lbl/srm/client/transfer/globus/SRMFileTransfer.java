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

import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.globus.gsi.gssapi.auth.Authorization;

import org.globus.util.GlobusURL;
//import java.net.MalformedURLException;
import java.io.IOException;
import java.lang.Thread;
import gov.lbl.srm.client.transfer.ISRMFileTransfer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.*;

import gov.lbl.srm.client.main.util;


public class SRMFileTransfer extends Thread implements ISRMFileTransfer {
    public static int DEFAULT_NUM_PARALLELSTREAM = 1; // defined by junmin
    public static int DEFAULT_BUFFERSIZE_MB = 2; // defined by junmin
    private GlobusURL fromurl;
    private GlobusURL tourl;
    //private int parallelism = 1;
	//private int buffersize = 2097152;
    private int parallelism = DEFAULT_NUM_PARALLELSTREAM;
    //private int buffersize = DEFAULT_BUFFERSIZE_MB*1048576;
    private int buffersize = 0;
// AS 090707
    private int blocksize = 0;

	private GSSCredential sourcecredential = null;
	private GSSCredential destcredential = null;
	private Authorization sourceauth = null;
	private Authorization destauth = null;
	private boolean listener = false;
	//private boolean setdecau = false;
    // AS 03/27/2007 changed from false to true
    private boolean setdecau = true;
	private boolean appendmode = false;

	private SRMTransferProtocol ttype = SRMTransferProtocol.GRIDFTP;
	private SRMTransferMode tmode = SRMTransferMode.GET;
	private ISRMFileTransfer ucopy;

    private String status=null;
    private boolean _isHostNERSCHPSS; // added by junmin


    private static Log logger;
    private java.util.logging.Logger theLogger;
    private boolean silent = false;
    private boolean useLog =false;
    private boolean debug = false;

public SRMFileTransfer () {
}

public void setSessionType(int sessionType) {
}

public void setSessionMode(int sessionMode) {
}

public void setLogger(Log logger, boolean silent, boolean useLog,
	boolean debug) {
  this.logger = logger;
  this.silent = silent;
  this.useLog = useLog;
  this.debug = debug;
}

public void setLogger(Log logger, java.util.logging.Logger theLogger, 
		boolean silent,boolean useLog, boolean debug) {
  this.logger = logger;
  this.theLogger = theLogger;
  this.silent = silent;
  this.useLog = useLog;
  this.debug = debug;
}

public SRMFileTransfer (String fromu, String tou) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
}

public SRMFileTransfer (String fromu, String tou, int parallelu) {
    try {
        fromurl = new GlobusURL (fromu);
        tourl = new GlobusURL (tou);
		parallelism = parallelu;
    } catch (Exception e) {
        e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
    }
}

public SRMFileTransfer (String fromu, String tou, SRMTransferProtocol ttypeu) {
    try {
        fromurl = new GlobusURL (fromu);
        tourl = new GlobusURL (tou);
        ttype = ttypeu;
    } catch (Exception e) {
        e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
    }
}

public SRMFileTransfer (String fromu, String tou, SRMTransferMode tmodeu) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
		tmode = tmodeu;
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
}

public SRMFileTransfer (String fromu, String tou, GSSCredential credentials) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
		sourcecredential = credentials;
		destcredential = credentials;
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
}

public SRMFileTransfer (String fromu, String tou, int parallelu, boolean listen) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
		listener = listen;
		parallelism = parallelu;
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
}

public void setNERSCHost() {
       _isHostNERSCHPSS = true;
}

public boolean transferDone() {
	if (ucopy == null) {
		return false;
	}
	return ucopy.transferDone();
}

public boolean transferOk() {
	if (ucopy == null) {
		return false;
	}
	return ucopy.transferOk();
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

public void transfer(String fromu, String tou) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
		this.start();
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
}

public void transferSync(String fromu, String tou, SRMTransferMode tmodeu) {
	try {
		fromurl = new GlobusURL (fromu);
		tourl = new GlobusURL (tou);
		tmode = tmodeu;
        if(debug ) {
		util.printMessage("TYPE:" + ttype.toString(),logger,silent);
        util.printMessage("BUFFERSIZE:="+buffersize,logger,silent);
        util.printMessage("BLOCKSIZE:="+blocksize,logger,silent);
        util.printMessage("PARALLELISM:="+parallelism,logger,silent);
        }
		if ((ttype == SRMTransferProtocol.GRIDFTP) || (ttype == SRMTransferProtocol.GSIFTP)) {
			ucopy = doGridftp();
			ucopy.transferSync();
		}
		else {
			ucopy = doGasscopy();
			ucopy.transferSync();
		}
	} catch (Exception e) {
        if(!transferOk()) {
		  e.printStackTrace();
		  status = e.toString();
        }        
 
	}
}

public void transferSync () {
    if(debug) {
	util.printMessage("TYPE:" + ttype.toString(),logger,silent);
    }
	if ((ttype == SRMTransferProtocol.GRIDFTP) || (ttype == SRMTransferProtocol.GSIFTP)) {
	    ucopy = doGridftp();
	    ucopy.transferSync();
	} else {	
	    ucopy = doGasscopy();
	    ucopy.transferSync();
	}
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
}

public void setBlockSize(int size) {
	blocksize = size;
}

public void setAppendMode(boolean appendMode) {
    appendmode = appendMode;
}

public void setSourceUrl(GlobusURL surl) throws IOException {
    try {
        String urlPath = surl.getPath();
        if (urlPath == null || urlPath.length() == 0) {
			status = "The '" + surl.getURL() +
                              "' url does not specify the file location.";
            throw new IOException(status);
        }
        fromurl = surl;
    } catch (Exception e) {
        e.printStackTrace();
		status = e.toString();
    }
}

public void setDestinationUrl(GlobusURL surl) throws IOException {
    try {
        String urlPath = surl.getPath();
        if (urlPath == null || urlPath.length() == 0) {
			status = "The '" + surl.getURL() +
                              "' url does not specify the file location.";
            throw new IOException(status);
        }
        tourl = surl;
    } catch (java.net.MalformedURLException e) {
        e.printStackTrace();
		status = e.toString();
    }
}

public void setSourceUrl(String surl) throws IOException {
	GlobusURL tempurl = null;
	try {
		tempurl = new GlobusURL (surl);
		String urlPath = tempurl.getPath();
        /*
	    if (urlPath == null || urlPath.length() == 0) {
			status = "The '" + tempurl.getURL() +
                              "' url does not specify the file location.";
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
	System.out.println("Gridftp...");
	System.out.println("from: " + fromurl.getURL());
	System.out.println("to: " + tourl.getURL());
    */
	try {
		SRMGridFTP ucopy = new SRMGridFTP(fromurl, tourl, tmode);
        ucopy.setLogger(logger,theLogger,silent,useLog,debug);
		if (buffersize > 0) ucopy.setBufferSize(buffersize);
// AS 090707
		if (blocksize > 0) ucopy.setBlockSize(blocksize);
        ucopy.setParallel(parallelism);
        ucopy.setDCAU(setdecau);
		if (sourcecredential != null) ucopy.setSourceCredentials(sourcecredential); // added  by junmin
if (destcredential != null) ucopy.setDestinationCredentials(destcredential); // added  by junmin
        if ( _isHostNERSCHPSS) {
           System.out.println("..........for NERSC..............");
           ucopy.setDCAU(false);
           ucopy.setSessionMode(org.globus.ftp.Session.MODE_STREAM);
           ucopy.setServerModeActive();
       }

		return ucopy;
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
	return null;
}

private SRMUrlCopy doGasscopy() {
    if(debug) {
	util.printMessage("gasscopy...",logger,silent);
	util.printMessage("from: " + fromurl.getURL(),logger,silent);
	util.printMessage("to: " + tourl.getURL(),logger,silent);
    }
	try {
		SRMUrlCopy ucopy = new SRMUrlCopy(fromurl, tourl);
		if (parallelism > 1) ucopy.setParallel(parallelism);
		ucopy.setDCAU(setdecau);
		if (sourcecredential != null) ucopy.setSourceCredentials(sourcecredential);
		if (destcredential != null) ucopy.setDestinationCredentials(destcredential);
		if (sourceauth != null) ucopy.setSourceAuthorization(sourceauth);
		if (destauth != null) ucopy.setDestinationAuthorization(destauth);
		if (buffersize > 0) ucopy.setBufferSize(buffersize);
// AS 090707
		if (blocksize > 0) ucopy.setBlockSize(blocksize);
		if (appendmode) ucopy.setAppendMode(appendmode);
		if (tmode == SRMTransferMode.THIRDPARTY) ucopy.setUseThirdPartyCopy();
		if (listener) ucopy.setListener(listener);
		
		return ucopy;
	} catch (Exception e) {
		e.printStackTrace();
		System.out.println(e.toString());
		status = e.toString();
	}
	return null;
}

//
// added by junmin
//
public long getSourceFileSize() {
	if (ucopy != null) {
		return ucopy.getSourceFileSize();
	} else {
		if ((ttype == SRMTransferProtocol.GRIDFTP) || (ttype == SRMTransferProtocol.GSIFTP)) {
			// cannt use this on FTP. Getting "Server refused GSSAPI authentication" error.
			SRMGridFTP gftp = new SRMGridFTP(fromurl, null);
            gftp.setLogger(logger,theLogger,silent,useLog,debug);
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
			 
			// this involves target url and java hangs at System.out.println for null pointer;
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
}

