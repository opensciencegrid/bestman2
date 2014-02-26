/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2010-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//Class SRMClientThreadMain
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;
import java.util.Vector;
import java.util.HashSet;

import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class SRMClientThreadMain extends Thread {

private SRMClientThreadPool _pool;
private int _freqformonitor = 1000; 
private FileTransferTest _parent;
private boolean debug;

private Vector inputVec = new Vector();

private boolean silent;



//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public SRMClientThreadMain (SRMClientThreadPool pool, 
				FileTransferTest parent, //java.util.logging.Logger theLogger,
				boolean silent)
    {
	_pool = pool;
	_parent = parent;

	this.silent = silent;
	debug = _parent.getDebug();

	/*
	try {
	    ClassLoader cl = this.getClass().getClassLoader(); 
	    Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.SRMClientThreadMain");	    
	}catch(ClassNotFoundException cnfe) {
	    System.out.println("ClassNotFoundException ");
	    //throw new SRMClientException(cnfe.getMessage());
	}
	*/
    }


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// run 
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public void run() { 
	//logger.debug
	//("+++ Monitor thread started to monitor active files transfer.  +++ ");
	
	if(debug) {
	    System.out.println("SRM-CLIENT: Monitor thread started to monitor active files transfer");
	}
	try {
	    while(true) {
		SRMClientN.logMsg("break?="+_parent.isClientThreadDisabled(),"SRMClientThreadMain", null);
		if(_parent.isClientThreadDisabled()) {
		    if(debug) {
			System.out.println("Client thread disabled");
		    }
		    interrupt();
		    return;
		}
		sleep(_freqformonitor);  
		Vector activeFiles = _parent.getActiveFiles();
		Vector activeSRMFileTransfers = _parent.getActiveMyISRMFileTransfers();
		SRMClientN.logMsg("activeFileSize="+activeFiles.size()+" "+activeSRMFileTransfers.size(), "SRMClientThreadMain", null);

		_pool.execute(activeFiles, activeSRMFileTransfers);
	    }
	}catch(InterruptedException ix) {
	    //ShowException.logDebugMessage(logger,ix);
	} finally {
	    SRMClientN.doExit("SRMClientThreadMain", 1);
	}
    }
}
