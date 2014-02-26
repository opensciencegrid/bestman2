/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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

package gov.lbl.adapt.adt;

import gov.lbl.adapt.srm.util.*;
import java.util.*;

class THistory {
    Vector _previous = new Vector();
    Vector _current = new Vector();
    int _sampleSize = 5;
    
    int _adtMax = 0;

    //    int _step = 1;

    double _setBackRate = 0;

    public void setAdtMax(int m) {
	_adtMax = m;
    }

    public THistory(int sampleSize, int adtMax) {
	TManagerADT.showMsg("... history size:"+sampleSize);
	if (sampleSize > 0) {
	    _sampleSize = sampleSize;
	}

	_adtMax = adtMax;

	TManagerADT.showMsg("... step = "+TManagerADT._gStep);
    }

    public boolean continueToCollect() {
	TManagerADT.showMsg(".... continue to collect? "+_previous.size()+"  "+_current.size());
	if ((_previous.size() == _sampleSize) && (_current.size() == 0)) {
	    return false;
	}
	if (_current.size() == _sampleSize) {
	    return false;
	}
	return true;
    }
	
    private int getNextStream(int increment, int streamBase) {
	int result = streamBase + increment;
	if (streamBase == 1) {
	    if (increment > 1) {
		result = increment;
	    } else {
		result = 2;
	    }
	} /*else {
	    result = streamBase+increment;
	    } */
	return result;
    }

    public int choose(int max) {
	int pStream = ((TThroughput)(_previous.get(0))).getStreams();
	
	int increment = TManagerADT._gStep * TManagerADT._parallelism;

	TManagerADT.showMsg("..... choose(): currDataSize="+_current.size()+" pStream="+pStream);
	double patch = 1.1;

	int result = 1; 
	if (_current.size() == 0) {
	    if (_setBackRate == 0) {
		result = getNextStream(increment, pStream);
	    } else {
		double p = avg(_previous); 
		double pThrpt = p*pStream/increment;
		TManagerADT.showMsg("..... choose(): previous throughpt="+pThrpt+" setbackrate="+_setBackRate);
		boolean canAdjustHigher = (pThrpt*patch > _setBackRate);
		_setBackRate = 0;
		if (canAdjustHigher) {
		    result = getNextStream(increment, pStream);//pStream;
		} else {
		    if (pStream - increment > 0) {
			result = pStream - increment;
		    } else {
			result = pStream; // min
		    }
		}
	    }
	} else {
	    double p = avg(_previous); 
	    double c = avg(_current);	

	    int cStream = ((TThroughput)(_current.get(0))).getStreams();

	    double pThrpt = p*pStream/increment;
	    double cThrpt = c*cStream/increment;
	    
	    //TManagerADT.showMsg("..... choose(): p="+p+" c="+c+" pstream="+pStream+" cstream="+cStream);
	    TManagerADT.showMsg("..... choose(): previous thrpt="+p+"MB/sec @"+pStream+" streams = "+pThrpt+" vs: "+c+"MB/sec @"+cStream+" streams="+cThrpt);

	    boolean streamCanBeAdjustedHigher = (pThrpt < cThrpt*patch);
	    TManagerADT.showMsg("       choose() adjustHigher?="+streamCanBeAdjustedHigher);
	    if (streamCanBeAdjustedHigher) {
		result = getNextStream(increment, cStream);

		if (result > max) {
		    return cStream;
		}
				
		_previous.clear();
		_previous.addAll(_current);
		_current.clear();	       

		if (result == pStream) {
		    result = cStream;
		    TManagerADT.showMsg("   choose():                    setbackrate="+pThrpt);
		    _setBackRate = pThrpt;
		} else if ((result >= _adtMax) && (cStream < _adtMax)) {
		    result = _adtMax;
		    TManagerADT.showMsg("   choose():                    setbackRate="+cThrpt);
		    _setBackRate = cThrpt;
		}
	    } else if (pThrpt > cThrpt) {
		result = pStream;
		_current.clear();
		if (pStream < cStream) {
		    _setBackRate = cThrpt;
		}
	    } else  { // rate is the same, stay put
		result = pStream;
	    }
	}
	TManagerADT.showMsg("...... choose()    result= ["+result+"]");
	return result;
    }

    public void add(TThroughput t) {
	if (_previous.size() < _sampleSize) {
	    _previous.add(t);
	} else {
	    int p = ((TThroughput)(_previous.get(0))).getStreams();
	    if (p == t.getStreams()) {
		_previous.remove(0);
		_previous.add(t);
	    } else if (_current.size() < _sampleSize) {	    		
		_current.add(t);
	    }  else {
		_current.remove(0);
		_current.add(t);
	    }
	}
	report(_previous, "choose() previous");
	report(_current, "choose() current");
    }

    private void report(Vector v, String prefix) {
	TManagerADT.showMsg("===>"+prefix);
	for (int i=0; i<v.size(); i++) {
	    ((TThroughput)(v.get(i))).print(prefix+"["+i+"]");
	}
    }

    public double avg(Vector v) {
	double total = 0;
	if (v.size() == 0) {
	    return 0;
	}
	for (int i=0; i<v.size();i++) {
	    total += ((TThroughput)(v.get(i))).getThroughput();
	}
	double avgRate = total/v.size();
	return avgRate;
	//int stream = v.get(0).getStream();
	//return avgRate*stream;
    }
}

public class TManagerADT implements IAdaptiveDataTxf {
    public final int _fMaxRTTMillis = 100; 
    private TSRMMutex _accessMutex = new TSRMMutex();

    long _start = 0;
    long _bytesTxfed = 0;

    THistory  _history = null; 

    public int _concurrencyNow = 5;
    public int _maxConcurrency = 1;

    public static final java.text.SimpleDateFormat _dateFormatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z' ");

    public static int _gStep = 1;		
    public static int _gStart = 1;

    public static TSRMMutex _thrptMutex = new TSRMMutex();
    public static long _thrptStartMillis = 0;    
    public static long _txfedBytes = 0;
    public static boolean _gDoPassiveADT = Boolean.parseBoolean(System.getProperty("doPassiveADT"));
    public static int _parallelism = 0;

    public TManagerADT(int maxConcurrency, int parallelism) {
	this(maxConcurrency, parallelism, 5000);
    }

    private TManagerADT(int maxConcurrency, int parallelism, int concurrency) {
	_maxConcurrency = maxConcurrency;
	_parallelism = parallelism;

	//_concurrencyNow = 1;
       
	_concurrencyNow  = _gStart * _parallelism; 
	int suggestedMax = concurrency * _parallelism;
	
	TManagerADT.showMsg(" .. input max concurrency = "+_maxConcurrency+" passive ADT?"+_gDoPassiveADT);

	if (_maxConcurrency > suggestedMax) {
	    TManagerADT.showMsg(".. w.r.t concurrency limit, adjusted max concurrency to: "+suggestedMax);
	    _maxConcurrency = suggestedMax;
	}

	//_concurrencyNow = _maxConcurrency;	RuntimeException hi = new RuntimeException("concurrent is set to max"); hi.printStackTrace();

	TManagerADT.showMsg(" .. max concurrency = "+_maxConcurrency+" curr="+_concurrencyNow);
	if (_concurrencyNow > _maxConcurrency) {
	    //_concurrencyNow = _maxConcurrency;
	    if (TTxfHandler._atm != null) {
		int updatedMax = TTxfHandler._atm.reclaim(_concurrencyNow);
		TManagerADT.showMsg("Init: Adjusted max concurrency  from "+_maxConcurrency+" to: "+_concurrencyNow+" result:"+updatedMax);
	    }
	    _maxConcurrency = _concurrencyNow;
	} else {
	    int step = _gStep * _parallelism; 
	    int mod = (_maxConcurrency - _concurrencyNow) % step;
	    if (mod > 0) {
	        int multi = _maxConcurrency - mod;
		if (TTxfHandler._atm != null) {
		    int updatedMax = TTxfHandler._atm.reclaim(multi);
		    TManagerADT.showMsg("Init: Adjusted max concurrency  from "+_maxConcurrency+" to: "+multi+" result:"+updatedMax);
		}
		_maxConcurrency = multi;
	    }
	}

	try {
	    _history = new THistory(Integer.parseInt(System.getProperty("adtHistoryLimit")), _maxConcurrency);
	} catch (Exception e) {
	    TManagerADT.showMsg("Using default history limit: 5");
	    _history = new THistory(5, _maxConcurrency);
	}
	TManagerADT.showMsg(" ..  max concurrency = "+_maxConcurrency+" starting streams="+_concurrencyNow);

	if (TTxfHandler._atm != null) {
	    TTxfHandler._atm.fyi(_concurrencyNow);
	}
    }

    public int getUsableStreams(int input) {
	int step = _gStep * _parallelism; 
	int mod = (input - _maxConcurrency) % step;
	if (mod > 0) {
	    int multi = input - mod;
	    return multi;
	}
	return 0;
    }

    public void doStreamAdjustment(int availMaxFromPolicyModule) {
	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return;
	}

	try {
	    if (availMaxFromPolicyModule > _maxConcurrency) {
		int usable = getUsableStreams(availMaxFromPolicyModule);
		if (TTxfHandler._atm != null) {
		    int updatedMax = TTxfHandler._atm.reclaim(usable);
		    if (updatedMax > _maxConcurrency) {
			TManagerADT.showMsg("max concurrency is updated in ATM from "+_maxConcurrency+" to "+updatedMax);
			_maxConcurrency = updatedMax;
			_history.setAdtMax(_maxConcurrency);
		    } else if (updatedMax <= 0) {
			TManagerADT.showMsg("max concurrency is updated from "+_maxConcurrency+" to "+availMaxFromPolicyModule);
			_maxConcurrency = availMaxFromPolicyModule;
			_history.setAdtMax(_maxConcurrency);
		    }
		    TManagerADT.showMsg("... <<<<< WHAT IF THERE IS LESS ACCAIMED FROM policy module???? ... current"+_maxConcurrency+" got from pm:"+updatedMax);
		}
	    } 	    
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}	    
    }

    /*
    public int getConcurrencySuggestion(int achievableThroughputPercentage,
					int RTT, // round trip time
					int maxConcurrency,
					int increment)	  
    { 
	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return _concurrencyNow; // use default
	}
	
	try {
	    if (_history.continueToCollect()) {
		return _concurrencyNow;
	    } 
		
	    _concurrencyNow = _history.choose(_maxConcurrency);    
	    if (_concurrencyNow <= _maxConcurrency) {		    	
		return _concurrencyNow;
	    } else {
		TManagerADT.showMsg("... choose: stay within the max stream: "+_maxConcurrency);
		_concurrencyNow = _maxConcurrency;
		return _maxConcurrency;
	    }	    
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}		
    }
    */

    public int getConcurrencySuggestion(/*int achievableThroughputPercentage,
					int RTT, // round trip time
					int maxConcurrency,
					int increment*/)	  
    { 
	if (_history.continueToCollect()) {
	    return _concurrencyNow;
	} 

	if (_gDoPassiveADT) {
	    if (TTxfHandler._atm != null) {
		int streamsAvail = TTxfHandler._atm.getConcurrencySuggestion(null, null, -1, -1);  
		doStreamAdjustment(streamsAvail);
		TManagerADT.showMsg("... choose: to follow policy!: "+_maxConcurrency);
	    }
	    return _maxConcurrency;
	}

	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return _concurrencyNow; // use default
	}
	
	try {		
	    _concurrencyNow = _history.choose(_maxConcurrency);    
	    if (_concurrencyNow <= _maxConcurrency) {		    	
		return _concurrencyNow;
	    } else {
		TManagerADT.showMsg("... choose: stay within the max stream: "+_maxConcurrency);
		_concurrencyNow = _maxConcurrency;
		return _maxConcurrency;
	    }	    
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}		
    }

    public int getMaxRTT() {
	return 10000;
    }


    public void startCollecting() {
	_start = System.currentTimeMillis();
    }

    public void setSites(String src, String tgt)
    {}

    public void txfDone(Object conn) {}
    public void txfStart(Object conn) {}

    public void progressUpdate(double throughput, int accumulatedConnectionStreams) {
	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return;
	}
	
	try {
	    if (throughput <= 0) { // skip the null throughput
		return;
	    }
	    if (accumulatedConnectionStreams > 0) {
		_history.add(new TThroughput(throughput, accumulatedConnectionStreams));
		TManagerADT.showMsg("................ [added]:  "+throughput+","+accumulatedConnectionStreams);
	    } else {
		_history.add(new TThroughput(throughput, _concurrencyNow));
		TManagerADT.showMsg("................ [Added]: "+throughput+" MB/s, concurrent stream="+_concurrencyNow);
	    }
	} finally {
	    _accessMutex.release();
	}	
    }

    /*
    public void NoNeedprogressUpdateOld(long bytes) {       
	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return;
	}
				
	long curr = System.currentTimeMillis();	
	int p = TRateStatistics.getPhase(curr);
	
	try {
	    while (_recent.getPhase() < p) {
		// update
		_history.accumulateFrom(_recent);		
		_recent.shift();
	    }
	    _recent.add(bytes);
	    
	    _history.print("[HISTORY]");
	    _recent.print ("[ RECENT]");
	} finally {
	    _accessMutex.release();
	}	
    }
    */

    /*
    public void progressUpdate(long bytes)
    {
	if (!TSRMUtil.acquireSync(_accessMutex)) {       
	    return;
	}
		
	try {
	    _bytesTxfed += bytes/(long)(1048576);
	    
	    int durSec = (int)((System.currentTimeMillis() - _start)/(long)1000);
	    int rate = (int)(_bytesTxfed/durSec);
	    System.out.println("  progress update: "+_bytesTxfed+"MB in "+ durSec+"seconds, rate="+rate);

	    if (_previousBest == null) {
		_previousBest = new TRateStatistic(rate, _concurrencyNow);
		_increaseConcurrency = true;
	    } else {
		if (!_previousBest.update(rate, _concurrencyNow)) {
		    _concurrencyNow = _previousBest.getConcurrency();
		    _increaseConcurrency = false;
		} else {
		    _increaseConcurrency = true;
		}
		System.out.println("             continue to update currency["+_concurrencyNow+"]? "+_increaseConcurrency);
	    }

	} finally {
	    _accessMutex.release();
	}
    }
    */

    public static void showMsg(String msg) {
	long currTime = System.currentTimeMillis();
	String timeStr = TManagerADT._dateFormatter.format(currTime);
	System.out.println("ADT: "+currTime+" "+timeStr+" "+msg);
    }

    public static double thrptAccumulater(long startMillis, long endMillis, long size) {
	if (!TSRMUtil.acquireSync(_thrptMutex)) {       
	    return 0;
	}
	try {
	    if (_thrptStartMillis == 0) {
		_thrptStartMillis = startMillis;
	    }
	    _txfedBytes += size;
	    int sec = (int)((endMillis-_thrptStartMillis)/(long)1000);
	    return _txfedBytes/((double)1048576*sec);
	} finally {
	    _thrptMutex.release();
	}
    }
}
