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

public class TRateStatistics {
    static long _gStartTimeMillis = System.currentTimeMillis();    
    static long _gElapseTimeMillis = 5000;


    //_rateMBSecond = 0;
    long _totalBytesTxfed = 0;
    //int _concurrency = _lastThroughput;
    int _phase = 0;
    int _lastThroughput = 0;
    
    public void print(String tag) {
	System.out.println(tag+"  phase="+_phase+"  MB="+_totalBytesTxfed/1048576);
    }

    public TRateStatistics() {}
     
    public void accumulateFrom(TRateStatistics currSegment) {
	_totalBytesTxfed += currSegment.getTotalBytes();
	_phase = currSegment.getPhase();
	_lastThroughput = currSegment.getLastThroughput();
    }

    public void add(long recentTxfedBytes) {
	_totalBytesTxfed += recentTxfedBytes;
    }

    public long getTotalBytes() {
	return _totalBytesTxfed;
    }

    public static int getPhase(long currTime) {
	return (int)((currTime-_gStartTimeMillis)/_gElapseTimeMillis);
	
    }

    public int getPhase() {
	return _phase;
    }

    public int getLastThroughput() {
	return _lastThroughput;
    }

    public void shift() {
	_lastThroughput = (int)(_totalBytesTxfed/1048576)/5;
	System.out.println("======================> shift phase: "+_phase+", throughput:"+_lastThroughput);
	_totalBytesTxfed = 0;
	_phase ++;
    }

    /*TRateStatistic(int r, int c) {
	_rateMBSecond = r;
	_concurrency = c;
    }
    

    int getConcurrency() {
	return _concurrency;
    }
    
    boolean update(int r, int c) {
	if ((c > _concurrency) && (_rateMBSecond < r)) {
	    return false;
	}

	if (r > _rateMBSecond) {
	    _rateMBSecond = r;
	}
	return true;
    }
    */
}
