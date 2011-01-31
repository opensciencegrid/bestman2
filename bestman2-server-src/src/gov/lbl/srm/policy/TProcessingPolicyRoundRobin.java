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

import gov.lbl.srm.server.TSRMRequest;
import gov.lbl.srm.server.TAccount;
import gov.lbl.srm.util.*;

import java.util.Vector;
import java.util.HashMap;

import EDU.oswego.cs.dl.util.concurrent.Mutex;

interface IRoundRobinItem {
	public String getID();
}

class TGeneralRoundRobin {
	Vector _collection = new Vector();
	HashMap _hashedCollection = new HashMap();
	Mutex _entryMutex = new Mutex();
	
	int _tokenHolderIdx = 0;
	
	int getCount() {
		return _collection.size();
	}
	
	Object peekNext() {
		if (TSRMUtil.acquireSync(_entryMutex)) {
			int total = _collection.size();
			
			if (total == 0) {
				TSRMUtil.releaseSync(_entryMutex);
				return null;
			}
			
			Object curr = _collection.get(_tokenHolderIdx);
			TSRMUtil.releaseSync(_entryMutex);
			return curr;
		} else {
			return null;
		}
	}
	
        Object getNext() {
		if (TSRMUtil.acquireSync(_entryMutex)) {
			int total = _collection.size();

			if (total == 0) {
				TSRMUtil.releaseSync(_entryMutex);
				return null;
			}

			Object curr = _collection.get(_tokenHolderIdx);

			if (_tokenHolderIdx == total-1) {
				_tokenHolderIdx = 0;
			} else {
				_tokenHolderIdx++;
			}
			 

			TSRMUtil.releaseSync(_entryMutex);
			return curr;
		} else {
			return null;
		}
	}
	
	Object getItem(Object id) {
		if (TSRMUtil.acquireSync(_entryMutex)) {
			Object result = _hashedCollection.get(id);
			TSRMUtil.releaseSync(_entryMutex);
			return result;
		} else {
			return null;
		}
	}

       // caller locks mutex
       int findIdx(Object obj) {
	       	int itemIdx = -1;
		for (int i=0; i<_collection.size(); i++) {
			 Object curr = _collection.get(i);
			 if (curr == obj) {
			 	 itemIdx = i;
			 	 break;
			 }
		}
		return itemIdx;		
       }
	
	void removeItem(IRoundRobinItem item) {
	       	if (!TSRMUtil.acquireSync(_entryMutex)) {
		    return;
		}
	
		_hashedCollection.remove(item.getID());
		
		int itemIdx = findIdx(item);
		
		if (itemIdx == -1) {
		       	TSRMUtil.releaseSync(_entryMutex);
			return;
		}

		_collection.remove(itemIdx);
		
		if (itemIdx >= _tokenHolderIdx) {
		        TSRMUtil.releaseSync(_entryMutex);
			return;
		}
		_tokenHolderIdx--;
		TSRMUtil.releaseSync(_entryMutex);
	}
	
	// will not use hashmap
	void addObject(Object item) {
		if (TSRMUtil.acquireSync(_entryMutex)) {
			_collection.add(item);
			TSRMUtil.releaseSync(_entryMutex);
		}
	}

        void removeObject(Object item) {
	        if (TSRMUtil.acquireSync(_entryMutex)) {
		        int itemIdx = findIdx(item);
			if (itemIdx != -1) {
			    _collection.remove(itemIdx);
			}
			_tokenHolderIdx--;
			TSRMUtil.releaseSync(_entryMutex);
		}
	}
	
	// will use hashamp
	void addItem(IRoundRobinItem item) {
		if (TSRMUtil.acquireSync(_entryMutex)) {
			_collection.add(item);
			_hashedCollection.put(item.getID(), item);
			TSRMUtil.releaseSync(_entryMutex);
		} else {
			return;
		}
	}
}

public class TProcessingPolicyRoundRobin extends TGeneralRoundRobin implements IProcessingPolicy{
	protected final Object _registerLock = new Object(); 
	protected int _registerLockWaittingCount = 0;
	
	private void doubleCheck(Object job) throws InterruptedException { 
		if (job == null) 
			throw new IllegalArgumentException("A job can not be null");
		if (Thread.interrupted())
			throw new InterruptedException();
		if (job.getClass()==TSRMRequest.class) {
			throw new IllegalArgumentException("wrong class for a job!");
		}
	}
	
	public void put(Object job) throws InterruptedException {
		doubleCheck(job);
		register((TSRMRequest)job);
	}
	
	public boolean offer(java.lang.Object item, long msecs) throws InterruptedException {
		doubleCheck(item);
		register((TSRMRequest)item);
		return true;
	}
	
	public Object peek() {
		return super.peekNext();
	}
	
	public Object poll(long msecs) throws InterruptedException {
		//return getNextJob();
		if (Thread.interrupted()) 
			throw new InterruptedException();
		Object job;
		synchronized(_registerLock) {
		    job = getNextJob();
		}
		
		if (job != null) {
			return job;
		}
		
		synchronized(_registerLock) {
			try {
				long waitTime = msecs;
				long start = (msecs <= 0)? 0 : System.currentTimeMillis();
				++_registerLockWaittingCount;
				for (;;) {
					job = getNextJob();
					 
					if (job != null || waitTime <= 0) {
						--_registerLockWaittingCount;
						return job;
					} else {
						_registerLock.wait(waitTime); 
						waitTime = msecs - (System.currentTimeMillis() - start);
					}
				}
			} catch(InterruptedException ex) {  
				--_registerLockWaittingCount; 
				_registerLock.notify();
				throw ex; 
			}
		}
	}
	
	
	public Object take() throws InterruptedException {
		if (Thread.interrupted()) 
			throw new InterruptedException();
		// try to extract. If fail, then enter wait-based retry loop

		Object job;
		synchronized(_registerLock) {
		    job = getNextJob();
		}
		
		if (job != null) {
			return job;
		}
	
		synchronized(_registerLock) {
			try {
				++_registerLockWaittingCount;
				for (;;) {
					job = getNextJob();
					if (job != null) {
						--_registerLockWaittingCount;
						return job;
					} else {
						_registerLock.wait(); 
					}
				}
			} catch(InterruptedException ex) { 		 
				--_registerLockWaittingCount;
				_registerLock.notify();
				throw ex; 
			}
		}
	}
	
	//
	//  functions for this class
	//
	public void register(TSRMRequest job) {
	    //Throwable ex = new Throwable();
	    //ex.printStackTrace();
		synchronized(_registerLock) {

			Object existing = super.getItem(job.getRequester().getOwner().getID());
			 
			if (existing == null) {
				TJobOwner owner = new TJobOwner(job.getRequester().getOwner());
				owner.add(job);
				super.addItem(owner);
			} else {
				TJobOwner owner = (TJobOwner)existing;
				owner.add(job);
			}
			
			if (_registerLockWaittingCount > 0) {
				_registerLock.notify();
			}
		}
	}
	
	public TSRMRequest getNextJob() {		 
		Object next = super.getNext();		

		TJobOwner user = (TJobOwner)next;
		
		if (user == null) {
			return null;
		}
		
		TJobGroup group = (TJobGroup)(user.getNext());

		if (group == null) {
			return null;
		}

		TSRMRequest result = (TSRMRequest)(group.getNext());
		
		if (result != null) {

		    group.removeObject(result);

		    if (group.getCount() == 0) {
			user.removeItem(group);
		    }
		    
		    if (user.getCount() == 0){
			super.removeItem(user);
		    }
		}

		return result;
		//return (TSRMRequest)(group.getNext());
	}
	
	 
}

class TJobOwner extends TGeneralRoundRobin implements IRoundRobinItem  {
	//Vector _groupCollection = new Vector();
	//HashMap _hashedGroupCollection = new HashMap();
	
	String _uid = null;
	
    //int _tokenHolderIdxForGroup = 0; // pick a job from _groupCollection[toKenHolderIdxForGroup ] 
	TJobOwner(TAccount user) {
		_uid = user.getID();
	}
	
	public String getID() {
		return _uid;
	}
	
	void add(TSRMRequest job) {
		Object existing = super.getItem(job.getRequester().getID());
		
		if (existing == null) {
			TJobGroup group = new TJobGroup(job.getRequester().getID());
			group.add(job);
			super.addItem(group);
		} else {
			TJobGroup group = (TJobGroup)existing;
			group.add(job);
		}
	}
	 
}

class TJobGroup extends TGeneralRoundRobin implements IRoundRobinItem {
	//Vector _entries = new Vector();
	String _rid = null;
	
	//int _tokenIdx = 0;
	
	TJobGroup(String rid) {
		_rid = rid;
	}
	
	// 
	// not expecting it to be called multiple times
	// 
	void add(TSRMRequest job) {
		/*
		Object existing = super.getItem(job.getRequester().getID());
		
		if (existing == null) {
			super.addItem(job);
		}
		*/
		super.addObject(job);
	}
	
	public String getID() {
		return _rid;
	}
}
