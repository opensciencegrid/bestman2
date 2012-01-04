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
 

/*
 * 
 * schedules all jobs of type ISRMFileRequest.
 * other requests like ls, mv are processed right away
 */
public class TProcessingUnit {
    protected static ThreadPoolExecutor _threadPool = null; //new PooledExecutor(new LinkedQueue());
    protected static ThreadPoolExecutor _threadPoolForLS = null;
    public static IProcessingPolicy _policy  = null;
    //public static final Semaphore _FTPSemaphore = new Semaphore(TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED);
    public TInputOutputController _ftpUnit = new TInputOutputController();
    public final static TReschedulingThread _rescheduler = new TReschedulingThread();
    
    public TProcessingUnit() {	
	setProcessingPolicy(new TProcessingPolicyRoundRobin());
	//_threadPool.setCorePoolSize(TSRMGeneralEnforcement.THREAD_POOL_SIZE);
	//_threadPoolForLS.setCorePoolSize(TSRMGeneralEnforcement.THREAD_POOL_SIZE);
	//_policy = new TProcessingPolicyRoundRobin();
	int msslimit = TSRMGeneralEnforcement.MAX_MSS_CONNECTION; //+ TSRMGeneralEnforcement.MAX_MSS_LOOKUP_QUEUE;
	if (msslimit >= TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED) {
	    msslimit = TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED/2;
	}
	_ftpUnit.init(TConnectionType.MSSConnection, msslimit);
	int otherlimit = TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED - msslimit;
	_ftpUnit.init(TConnectionType.DiskConnection, otherlimit);
	
	TSRMUtil.startUpInfoSilent("############  processing limits: ###############");
	TSRMUtil.startUpInfoSilent("############      thread pool size for requests=:"+_threadPool.getCorePoolSize());
	TSRMUtil.startUpInfoSilent("############      thread pool size for ls=:"+_threadPoolForLS.getCorePoolSize());
	TSRMUtil.startUpInfoSilent("############      msslimit        =:"+msslimit);
	TSRMUtil.startUpInfoSilent("############      disklimit       =:"+otherlimit);
	TSRMUtil.startUpInfoSilent("#################################################");
	
    }
    
    public static void setProcessingPolicy(IProcessingPolicy p) {
	if (p == null) {
	    throw new RuntimeException("Need a valid processing policy");
	}
	_policy = p;
	long keepAliveTime = 0;
	TimeUnit timeUnit = TimeUnit.SECONDS;
	_threadPool = new ThreadPoolExecutor(TSRMGeneralEnforcement.THREAD_POOL_SIZE, TSRMGeneralEnforcement.THREAD_POOL_SIZE, keepAliveTime, timeUnit,_policy);
	_threadPoolForLS = new ThreadPoolExecutor(TSRMGeneralEnforcement.THREAD_POOL_SIZE, TSRMGeneralEnforcement.THREAD_POOL_SIZE, keepAliveTime, timeUnit,_policy);
    }
    
    public static IProcessingPolicy getProcessingPolicy() {
	return _policy;
    }
    
    public boolean acquireIOToken(String protocol) {
	if (protocol.equals(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    return _ftpUnit.getSemaphore(TConnectionType.MSSConnection);
	} else {
	    return _ftpUnit.getSemaphore(TConnectionType.DiskConnection);
	}
    }
    
    public boolean releaseIOToken(String protocol) {
	if (protocol.equals(TSupportedURLOnLocalHPSS._DefProtocolStr)) {
	    return _ftpUnit.releaseSemaphore(TConnectionType.MSSConnection);
	} else {
	    return _ftpUnit.releaseSemaphore(TConnectionType.DiskConnection);
	}
    }
    
    public boolean releaseIOToken(){
	return _ftpUnit.releaseSemaphore(TConnectionType.DiskConnection);
    }
    
    public boolean acquireIOToken() {
	return _ftpUnit.getSemaphore(TConnectionType.DiskConnection);
    }
    
    public boolean acquireIOToken(boolean outgoingTxf) {
	return acquireIOToken();
    }

    public boolean releaseIOToken(boolean outgoingTxf) {
	return releaseIOToken();
    }

    public void submitJob(TSRMRequest job) {
	try {
	    long curr = System.currentTimeMillis();
	    if (job.getDelay(curr) > 0) {
		_rescheduler.add(job);
	    } else {
		_threadPool.execute(job);
	    }
	} catch (java.lang.RuntimeException e) {
	    TSRMUtil.startUpInfo("Thread pool processing is interrupted. job="+job.description());
	}
    }

    public void submitLSJob(TSRMRequest job) {
	  try {
	      _threadPoolForLS.execute(job);
	  } catch (java.lang.RuntimeException e) {
	      TSRMUtil.startUpInfo("Thread pool processing is interrupted. job="+job.description());
	  }
    }

}

