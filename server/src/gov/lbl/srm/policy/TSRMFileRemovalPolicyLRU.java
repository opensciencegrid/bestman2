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

package gov.lbl.srm.policy;

import java.util.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.*;
//import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMFileRemovalPolicyLRU extends TSRMFileRemovalPolicy {
     //TreeSet _candidates           = new TreeSet(TSRMFileRemovalPolicy._ComparatorLRU);
     Vector _candidates           = new Vector();
     TSRMMutex   _accessMutex          = new TSRMMutex();
     TSRMMutex   _busyMutex            = new TSRMMutex();
     Date    _lastBusyTime         = null; 
     
    //TSRMStorage.iSRMSpaceToken _host = null;
     
     public TSRMFileRemovalPolicyLRU() 
     {
	 super();
     }    
     
     public void addCandidate(TSRMLocalFile f) {
	 TSRMUtil.acquireSync(_accessMutex);	
	 _candidates.add(f);
	 TSRMLog.debug(this.getClass(), null, "event=addCandidate+ "+f.stampPath(), null);
	 increaseInventory(f.getCurrentSize());
	 TSRMUtil.releaseSync(_accessMutex);
    }
     
     
     public void fileRemoved(TSRMLocalFile f, long fileSize) {
	 //decreaseInventory(fileSize);
	//RuntimeException e = new RuntimeException("fileRemoved:"+f);
	//e.printStackTrace();
	 if (_candidates.remove(f)) {	
	     decreaseInventory(fileSize);
	     TSRMLog.debug(this.getClass(), null, "event=removeCandidate- "+f.stampPath(), " sizeRemoved="+fileSize);
	}
     }
     
     public long removeCandidate(TSRMLocalFile f) {
//RuntimeException e = new RuntimeException("fileRemoved:"+f);
 //       e.printStackTrace();

	 long compactedSize = f.compactMe();

	 if (compactedSize > 0) {
	    if (_candidates.remove(f)) {
	     decreaseInventory(compactedSize);
	TSRMLog.debug(this.getClass(), null, "event=removeCandidate- "+f.stampPath(), " sizeRemoved="+compactedSize);
		}
	     //_candidates.remove(f);
	 }

	 return compactedSize;
     }
     
     public void cleanLimitedTo(long targetedBytes) {		 
	 if (!mayAccomodate(targetedBytes)) {
	     return;
	 }

	 if (isCurrentlyBusy()) {
	    return;
	 }
	 
	 long totalSizeGained = 0;
	 boolean noActionTaken = true;
	 
	 TSRMUtil.acquireSync(_accessMutex);
	 TSRMLog.debug(this.getClass(), null, "event=cleanLimitedTo tgtBytes="+targetedBytes, "numCandidates="+_candidates.size());
	 while (true) {
	     noActionTaken = true;
	     for (int i=0; i<_candidates.size(); i++) {
		 TSRMLocalFile curr = (TSRMLocalFile)(_candidates.elementAt(i));

		 long sizeGained = removeCandidate(curr);
		 if (sizeGained > 0) {
		     totalSizeGained += sizeGained;
		     noActionTaken = false;
		     break;
		 }		      
	     }
	     if (noActionTaken || (_candidates.size() == 0)) {
		 break;
	     }
	     if (totalSizeGained > targetedBytes) {	   
		 break;
	     }
	 }		
	 	 
	 if (noActionTaken) {
	     setCurrentlyBusy(true);
	 }
	 
	 TSRMUtil.releaseSync(_accessMutex);	
     }
     
     public long cleanAll() {
	 long totalSizeGained = 0;
	 boolean noActionTaken = true;
	 
	 TSRMUtil.acquireSync(_accessMutex);
	 TSRMLog.debug(this.getClass(), null, "event=cleanAll","numCandidates="+_candidates.size());
	 while (true) {
	     noActionTaken = true;
	     for (int i=0; i<_candidates.size(); i++) {
		 TSRMLocalFile curr = (TSRMLocalFile)(_candidates.elementAt(i));

		 long sizeGained = removeCandidate(curr);
		 if (sizeGained > 0) {
		     totalSizeGained += sizeGained;
		     noActionTaken = false;
		     break;
		 }	
	     }

	     if (noActionTaken) {
		 break;
	     } 
	     if (_candidates.size() == 0) {
		 break;
	     }
	 }
	 
	 if (noActionTaken) {
	     setCurrentlyBusy(true);
	 }	 
	 
	 TSRMUtil.releaseSync(_accessMutex);
	 
	 return totalSizeGained;
     }
     
     public void setCurrentlyBusy(boolean state) {
	 TSRMUtil.acquireSync(_busyMutex);
	 if (state) {
	     _lastBusyTime = new Date();
	 } else {
	     _lastBusyTime = null;
	 }
	TSRMLog.debug(this.getClass(), "setCurrentlyBusy", "lastBusyTime="+_lastBusyTime, "state="+state);
	 TSRMUtil.releaseSync(_busyMutex);
     }

     public boolean isCurrentlyBusy() {
	 boolean result = true;
	 
	 TSRMUtil.acquireSync(_busyMutex);
	 
	 if (_lastBusyTime == null) {
	     result = false;
	 } else {
	     Date now = new Date();
	     long elaspMilliSeconds = 60*1000;
	     if (_lastBusyTime.getTime() + elaspMilliSeconds < now.getTime()) {
		 result = false;
	     }
	 }
	 
	 TSRMUtil.releaseSync(_busyMutex); 
	 
	 return result;
     }

}

