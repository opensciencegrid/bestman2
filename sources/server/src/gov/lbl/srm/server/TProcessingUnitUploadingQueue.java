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

//import EDU.oswego.cs.dl.util.concurrent.*;

import gov.lbl.srm.policy.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import java.util.*;
import java.util.concurrent.*;
 

public class TProcessingUnitUploadingQueue extends TProcessingUnit {
    protected static ThreadPoolExecutor _outgoingTxfPool = null;
    public TInputOutputController _outgoingFtpUnit = new TInputOutputController();

    public TProcessingUnitUploadingQueue(String parameter) {	
	super();

	int poolSize = TSRMGeneralEnforcement.THREAD_POOL_SIZE;
	int assignedFTPLimit = 2;

	String input[] = parameter.split(":");
	try {
	    int inputPoolSize = Integer.parseInt(input[0]);	
	    if (inputPoolSize > 0) {
		poolSize = inputPoolSize;
	    }
	    if (input.length > 1) {
		int inputAssignedFTPLimit = Integer.parseInt(input[1]);
		if (inputAssignedFTPLimit > 0) {
		    assignedFTPLimit = inputAssignedFTPLimit;
		}
	    }
	} catch (Exception e) {
	    //
	}


	long keepAliveTime = 0;
	TimeUnit timeUnit = TimeUnit.SECONDS;

	_outgoingTxfPool = new ThreadPoolExecutor(poolSize, poolSize, keepAliveTime, timeUnit, getProcessingPolicy());
	//_outgoingTxfPool.setCorePoolSize(poolSize);
	
	TSRMUtil.startUpInfo("#######################################################################################");
	TSRMUtil.startUpInfo("############  outgoing transfer thread pool created. ");
	TSRMUtil.startUpInfo("############  pool size            = "+poolSize       );
	TSRMUtil.startUpInfo("############  with ftp limit       = "+assignedFTPLimit);
	TSRMUtil.startUpInfo("#######################################################################################");

	_outgoingFtpUnit.init(TConnectionType.DiskConnectionUpload, assignedFTPLimit);
    }

    public void submitJob(TSRMRequest job) {
	if (job.getClass() != TSRMRequestCopyToRemote.class) {
	    super.submitJob(job);
	    return;
	}

	try {
	    long curr = System.currentTimeMillis();
	    if (job.getDelay(curr) > 0) {
		_rescheduler.add(job);
	    } else {
		_outgoingTxfPool.execute(job);
	    }
	} catch (java.lang.RuntimeException e) {
	    TSRMUtil.startUpInfo("Outgoing Txf thread pool processing is interrupted. job="+job.description());
	}
    }

    public boolean acquireIOToken(boolean outgoingTxf) {
	if (!outgoingTxf) {
	    return super.acquireIOToken();	   
	} else {
	    return _outgoingFtpUnit.getSemaphore(TConnectionType.DiskConnectionUpload);
	}	
    }

    public boolean releaseIOToken(boolean outgoingTxf) {
	if (!outgoingTxf) {
	    return super.releaseIOToken();
	} else {
	    return _outgoingFtpUnit.releaseSemaphore(TConnectionType.DiskConnectionUpload);       
	}        
    }
}
