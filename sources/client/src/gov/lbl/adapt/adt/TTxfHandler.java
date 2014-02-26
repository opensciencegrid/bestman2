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

import gov.lbl.adapt.adt.*;
import gov.lbl.adapt.srm.util.*;
import gov.lbl.adapt.srm.server.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.globus.ftp.*;
import org.globus.ftp.exception.*;
import org.globus.ftp.vanilla.*;
import org.globus.gsi.*;
import org.globus.gsi.gssapi.*;

import org.ietf.jgss.GSSCredential;

class TLocalFileMonitor {
    String _filename = null;
    long _txfedBytes = 0; // bytes txfed at the last inquiry
    long _fullBytes = 0;
    long _previous = 0; // bytes txfed at previous inquiry
    TSRMMutex _performanceMutex = new TSRMMutex();

    boolean _writeModeOn = false;

    RandomAccessFile _localFile = null;

    /*public TLocalFileMonitor(RandomAccessFile f) {
	_localFile = f;
	}*/

    public TLocalFileMonitor(String name, boolean setWriteMode, long size) {
	setLocalFile(name, setWriteMode, size);
    }

    public TLocalFileMonitor(RandomAccessFile f, long size) {
	_localFile = f;
	if (size > 0) {
	    _fullBytes = size;
	}
    }

    public void txfSucc(long size) {
	if (!TSRMUtil.acquireSync(_performanceMutex)) {
	    return;
	}

	try {
	    if (size > 0) {
		_fullBytes = _txfedBytes;
	    }
	    _txfedBytes = _fullBytes;
	} finally {
	    TSRMUtil.releaseSync(_performanceMutex);
	}
    }

    private long getProgress() {
	long progress = (_txfedBytes - _previous);
	TManagerADT.showMsg("  offset="+_txfedBytes+" of "+_fullBytes+"  progress="+progress);
	_previous = _txfedBytes;
	return progress;
    }

    public long report() {
	if (!TSRMUtil.acquireSync(_performanceMutex)) {
	    return 0;
	}
	
	try {
	    TManagerADT.showMsg("   report:[localFile] "+_localFile);
	    if ((_txfedBytes == _fullBytes) && (_txfedBytes > 0)) {
		TManagerADT.showMsg("   txf done. full="+_fullBytes+" prev:"+_previous+" diff: "+(_fullBytes-_previous));
		return _fullBytes - _previous;
	    }
	    if ((_localFile != null) && _writeModeOn) {
		_txfedBytes = _localFile.getFilePointer();
		return getProgress();
	    } else {
		TManagerADT.showMsg("   = null "+Thread.currentThread());
		return _txfedBytes;
	    }
	} catch (Exception e) {
	    TManagerADT.showMsg(e+"   [localFile exception] => "+_txfedBytes +" vs "+_fullBytes+" "+(double)_txfedBytes/(double)_fullBytes);	   
	    File f = getFile();
	    _txfedBytes = f.length();	    
	    return getProgress();
	} finally {
	    TSRMUtil.releaseSync(_performanceMutex);
	}
    }

    private File getFile() {
	try {
	    return new File(_filename);
	} catch (Exception e) {
	    e.printStackTrace();
	    return null;
	}
    }

    public RandomAccessFile getLocalFile() 
    {
	return _localFile;
    }

    public void setLocalFile(String name, boolean useWriteMode, long size) 
    {
	if (!TSRMUtil.acquireSync(_performanceMutex)) {
	    return;
	}

	_writeModeOn = useWriteMode;

	try {
	    _txfedBytes = 0;
	    _previous = 0;
	    
	    if (_writeModeOn) {
		_localFile = new RandomAccessFile(name, "rw");
	    } else {
		_localFile = new RandomAccessFile(name, "r");
	    }
		
	    _filename = name;
	    if (size > 0) {
		_fullBytes = size;	
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RuntimeException(e.toString());
	} finally {
	    TSRMUtil.releaseSync(_performanceMutex);
	}
    }

    /*
    public void setLocalFile(RandomAccessFile f, long size) 
    {
	if (!TSRMUtil.acquireSync(_performanceMutex)) {
	    return;
	}

	try {
	    _txfedBytes = 0;
	    _previous = 0;
	    
	    _localFile = f;
	    _fullBytes = size;	
	} finally {
	    TSRMUtil.releaseSync(_performanceMutex);
	}
    }
    */

    public void cleanUp() {
	try {	    
	    if (_localFile !=null) {
		TManagerADT.showMsg("closing file: "+_filename);
		_localFile.close();
		TManagerADT.showMsg("closed file: "+_filename);
		_localFile = null;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    _localFile = null;
	}
    }
}

class TConnectionGeneral implements IConnection {
    int _streamCount = 0;

    public TConnectionGeneral(String site) {
	
    }

    public void setStreamCount(int n) {
	_streamCount = n;
    }

    public int getStreamCount() {
	return _streamCount;
    }
}

class TConnectionGSIFTP implements IConnection {
    String _remoteHost = null;
    int    _streamCount = 1; // can be more if parallel streams are used

    GridFTPClient _client = null;

    int _localServerMode = Session.SERVER_PASSIVE;
    int _transferMode = GridFTPSession.MODE_EBLOCK; // default;
    int _transferType = Session.TYPE_IMAGE;
    int _bufferSize = 0;
    int _sessionProtection = GridFTPSession.PROTECTION_SAFE;

    long _lastTxfRateBytesSecond = 0;
    long _lastTxfFinished = 0;

    String _err = null;

    TLocalFileMonitor _perf = null;

    DataChannelAuthentication _dcau = DataChannelAuthentication.SELF;

    public TConnectionGSIFTP(String remoteHost) 
    {
	_remoteHost = remoteHost;
	_client = initGridFTPClient(remoteHost);

	_transferMode = GridFTPSession.MODE_EBLOCK;
    }

    public void setSessionProtection(int sp) 
    {
	_sessionProtection = sp;
    }

    public long report() {
	if (_perf != null) {
	    return _perf.report();
	}
	return 0;
    }

    public GridFTPClient initGridFTPClient(String host) 
    {
	TManagerADT.showMsg("initiating the client "+host+" "+Thread.currentThread());

	int port = 2811;

	int retryCounter = 0;

	while (true) {
	    try {
		//
		// each time the thread is up, the credential is checked
		// this way the extended proxy can have chance to be noticed
		//
		//GlobusCredential c = GlobusCredential.getDefaultCredential();
		X509Credential c = X509Credential.getDefaultCredential();
		GSSCredential cred = new GlobusGSSCredentialImpl(c, GSSCredential.INITIATE_AND_ACCEPT);
	    
		GridFTPClient result = new GridFTPClient(host, port);
		
		if (cred != null) {
		    result.authenticate(cred);
		}
		
		int maxWaitMillis   = 3000000;
		int waitDelayMillis = 20000;
		result.setClientWaitParams(maxWaitMillis, waitDelayMillis);
		return result;
	    } catch (Exception e) {
		retryCounter ++;
		if (retryCounter >=2) {
		    e.printStackTrace();		
		    throw new RuntimeException(e.toString());
		}
	    } finally {
		TManagerADT.showMsg("initiated the client "+Thread.currentThread()+" retryCounter="+retryCounter);
	    }
	}
    }

    public void resetStreamCount(int n) {
	setServerMode(_localServerMode);
	setStreamCount(n);
	//clone.setTransferMode(_transferMode);
	setBufferSize(_bufferSize);
	setDCAU(_dcau);
    }


    public IConnection clone() 
    {
	TConnectionGSIFTP clone =  new TConnectionGSIFTP(_remoteHost);
	clone.setServerMode(_localServerMode);
	clone.setStreamCount(_streamCount);
	//clone.setTransferMode(_transferMode);
	clone.setBufferSize(_bufferSize);
	clone.setDCAU(_dcau);

	return clone;
    }

    public void setStreamCount(int n) 
    {
	TManagerADT.showMsg("Stream count is set to be => "+n);
	if (n == 1) {
	    _transferMode = GridFTPSession.MODE_STREAM;	    
	}
	_streamCount = n;
	if ( n > 1) {
	    _transferMode = GridFTPSession.MODE_EBLOCK;
	} 

	try {
	    _client.setMode(_transferMode);
	    _client.setProtectionBufferSize(16384);
		    
	    // not sure if the following affects put
	    /*
	    TManagerADT.showMsg(".......parallem stream adjust is disabled as it affects put");
	    if (_streamCount > 1) {
		_client.setOptions(new RetrieveOptions(_streamCount));
	    } else if (_streamCount == 1) {
		_client.setPassiveMode(true);
	    }
	    */

	} catch (Exception e) {
	    e.printStackTrace();
	}	
    }


    public void setDCAU (DataChannelAuthentication dcau) {
	_dcau = dcau;
	try {
	    if (_client.isFeatureSupported("DCAU")) {
		// dcau is on
		_client.setDataChannelAuthentication(dcau);
		if (_dcau == DataChannelAuthentication.SELF) {
		    _client.setDataChannelProtection(_sessionProtection);
		}
	    } else {
		// no dcau
		_client.setLocalNoDataChannelAuthentication();
	    }
	} catch (Exception e) {
	    e.printStackTrace();	    
	    throw new RuntimeException(e.toString());
	}     
    }


    public void setServerMode(int localServerMode) {
	_localServerMode = localServerMode;

	try {
	    if (localServerMode == Session.SERVER_ACTIVE) {
		org.globus.ftp.HostPort hp = _client.setPassive();
		_client.setLocalActive();
	    } else {
		org.globus.ftp.HostPort hp = _client.setLocalPassive();
		_client.setActive();
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RuntimeException(e.toString());
	}
    }

    public void setBufferSize(int bufferSize) {
	try {
	    _bufferSize = bufferSize;
	    if (bufferSize > 0) {
		_client.setLocalTCPBufferSize(bufferSize);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public int getStreamCount() {
	return _streamCount;
    }

    public void startMe() {
	    
    }
    public void stopMe() {
	TManagerADT.showMsg("==> to do: stop the connection all together");
    }

    private void setRate(long startMillis, long endMillis, long sizeBytes) {
	int dur = (int)((endMillis-startMillis)/(long)1000);	
	_lastTxfRateBytesSecond = (int)(sizeBytes/(long)(dur));
	_lastTxfFinished = endMillis;

	double rate =_lastTxfRateBytesSecond/(double)1048576;
	String sizeStr = sizeBytes+" bytes("+sizeBytes/1048576+"MB)";
	TManagerADT.showMsg(Thread.currentThread()+"\tTxfRate: "+sizeStr+"/"+dur+"(secs) \t="+ rate +"MB/s");

	double throughput = ((double)sizeBytes)/(1048576*dur);
	TTxfHandler._adtServer.progressUpdate(throughput, -1);

	long txfTimeMillis = endMillis - startMillis;

	//String ptmMsg = "PTM "+sizeBytes+" "+txfTimeMillis+" "+ _streamCount;
	//gov.lbl.adapt.srm.client.main.SRMClientN.logMsg(ptmMsg, "size/dur/stream", "setRate()");

	TTxfHandler.updatePTM(sizeBytes, endMillis-startMillis, _streamCount); 	
    }
	
    public long getWetprint() {
        if (_lastTxfRateBytesSecond == 0) {
	    return 0;
	}
	long now = System.currentTimeMillis();
	if (now == _lastTxfFinished) {
	    return 0;
	}
	long bytesEstimated = _lastTxfRateBytesSecond*1000/(now - _lastTxfFinished);
	return bytesEstimated;
    }

    /*
    public void release() {
	if (_pool != null) {
	    _pool.returnConnection(this);
	}
    }
    */

    public boolean exists(String path) {
	try {
	    if (!_client.exists(path)) {
		TManagerADT.showMsg("No such path on remote machine: "+path);
		return false;
	    }
	} catch (Exception e) {
	    TManagerADT.showMsg("Unable to list path:"+path);
	    e.printStackTrace();
	    return false;
	}
	return true;
    }       
        
    public boolean list(String path, Vector result) {	
	try {
	    _client.changeDir(path);
	    Vector contents = _client.list();

	    for (int i=0; i< contents.size(); i++) {
		org.globus.ftp.FileInfo ff = (org.globus.ftp.FileInfo)(contents.get(i));
		if (ff.isDirectory()) {
		    RuntimeException r = new RuntimeException("Skipping subdir");
		    r.printStackTrace();
		} else if (ff.isFile() && ff.allCanRead()) {
		    //TManagerADT.showMsg("  adding:"+path+"/"+ff.getName());
		    result.add(ff.getName());
		} else {
		    // maybe a link, skip
		}
	    }
	    return true;   
	} catch (Exception e) {
	    //e.printStackTrace();
	    TManagerADT.showMsg(path +" is a file");
	    return false;
	}
    }

    private void adjust() {
	/*
	int p = TTxfHandler.getADTParallelStreamSuggestion();
	TManagerADT.showMsg("...suggesting: "+p+" current="+_streamCount);
	try {
	    if ((p > 0) && (p != _streamCount)) {
		resetStreamCount(p);
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    TManagerADT.showMsg(".... adjusting failed with some exception, will continue");
	}
	*/
	
	TManagerADT.showMsg(" no adjust at file level");
	if (System.currentTimeMillis() - _lastTxfFinished < 1000) {
	    TSRMUtil.sleep(1001);
	}
	
        //reset();
    }   
   	
    private long doGet(String remoteSrc, String localTgt) throws Exception {
	long size = 0;
	    // set source flag
	    
	    _client.setProtectionBufferSize(16384);
	    _client.setType(_transferType);
	    _client.setMode(_transferMode);
	    if (_streamCount > 1) {
		_client.setOptions(new RetrieveOptions(_streamCount));
	    }

	    // handle dcau
	    setDCAU(_dcau);

	    _client.setClientWaitParams(0x7fffffff, 1000);
	    // buffersize is set here
	    
	    setServerMode(_localServerMode);
	    if (_streamCount == 1) {
		_client.setPassiveMode(true);
	    }

	    size = _client.getSize(remoteSrc);
	    //setLocalFile(new java.io.RandomAccessFile(localTgt, "rw"), size);
	    //setLocalFile(localTgt, "rw", size);
	    setLocalFile(localTgt, true, size);
	    
	    DataSink sink = new FileRandomIO(_perf.getLocalFile());
	    //size = _client.getSize(remoteSrc);
	    if (_transferMode == GridFTPSession.MODE_EBLOCK) {
		if (_localServerMode == Session.SERVER_PASSIVE) {
		    _client.setOptions(new RetrieveOptions(_streamCount));
		}
		
		//sink = new FileRandomIO(new java.io.RandomAccessFile(localTgt, "rw"));
		

		//size = _client.getSize(remoteSrc);
		_client.extendedGet(remoteSrc, size, sink, null);
	    } else {
		//sink = new DataSinkStream(new FileOutputStream(localTgt));
		//_client.get(remoteSrc, sink, null);
		//sink = new FileRandomIO(new java.io.RandomAccessFile(localTgt, "rw"));

		//size = _client.getSize(remoteSrc);
		_client.get(remoteSrc, sink, null);
	    }
	    TManagerADT.showMsg("   .... getFromRemote() succ "+this+"  "+_perf.getLocalFile());
	    sink.close();
	    return size;
    }
   
    public boolean getFromRemote(String remoteSrc, String localTgt) {
	_err = null;
	adjust();
	TManagerADT.showMsg("... starting:   "+localTgt+"   "+Thread.currentThread()+" "+this+"  streams="+_streamCount+" mode="+_transferMode);
	long start = System.currentTimeMillis();
	long size = 0;

	try {
	    TTxfHandler._adtServer.txfStart(this);
	    size = doGet(remoteSrc, localTgt);	    
	    return false;
	} catch (org.globus.ftp.exception.ServerException e) {
	    TManagerADT.showMsg("............... getFromRemote from "+remoteSrc+" Server err, retry "+Thread.currentThread());	  
	    _err = e.getMessage();
	    return true;
	} catch (Exception e) {
	    TManagerADT.showMsg("............... getFromRemote has err: "+remoteSrc+" "+e);
	    e.printStackTrace();
	    _err = e.getMessage();
	    return false;
	} finally {
	    TManagerADT.showMsg("............... getFromRemote from "+remoteSrc+" msg="+_err+" "+Thread.currentThread());	  
	    if (_err == null) {
		long end = System.currentTimeMillis();
		setRate(start, end, size);
		_perf.txfSucc(size);	    
		TTxfHandler._adtServer.txfDone(this);
	    }
	    cleanUp();
	    TManagerADT.showMsg("==> done: "+localTgt+"  "+Thread.currentThread());
	} 
    }

	
    public void setOptions0() throws Exception {
	if (_client.isFeatureSupported("DCAU")) {
	    _client.setDataChannelAuthentication(DataChannelAuthentication.NONE);
	} 
	_client.setLocalNoDataChannelAuthentication();

	if (_streamCount > 1) {
	    _client.setMode(GridFTPSession.MODE_EBLOCK);
	    _client.setOptions(new RetrieveOptions(_streamCount));
	} else {
	    _client.setMode(GridFTPSession.MODE_STREAM);
	    //if (passiveServerMode) {
	    _client.setPassive();
	    _client.setLocalActive();
	}
    }

    private void doPut(String localPath, String remoteTgt) throws Exception {	
	//setOptions();      
	_client.setProtectionBufferSize(16384);
	_client.setType(_transferType);
	_client.setMode(_transferMode);
	
	if (_streamCount > 1) {
	    _client.setOptions(new RetrieveOptions(_streamCount));
	}
	
	// handle dcau
	setDCAU(_dcau);
	
	_client.setClientWaitParams(0x7fffffff, 1000);
	
	_localServerMode = Session.SERVER_ACTIVE;
	setServerMode(_localServerMode);
	
	//setLocalFile(new java.io.RandomAccessFile(localPath, "r"), -1);
	//setLocalFile(localPath, "r", -1);
	setLocalFile(localPath, false, -1);
	DataSource source = new FileRandomIO(_perf.getLocalFile());
	
	// _client.setTCPBufferSize(bufferSize);
	_client.put(remoteTgt, source, null);
	TManagerADT.showMsg("............... putToRemote is successful :"+remoteTgt+" "+Thread.currentThread());
	//_client.close();
    }
    
    public void reset() {
	TManagerADT.showMsg("closing the client "+Thread.currentThread());
	try {
	    _client.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	TManagerADT.showMsg("closed the client "+Thread.currentThread());
	_client = initGridFTPClient(_remoteHost);
    }

    public boolean putToRemote(String localPath, String remoteTgt) {
	_err = null;
	if ((_streamCount > 1) && (_transferMode == Session.MODE_STREAM)) {
	    throw new RuntimeException("Gridftp PUT does not work with STREAM mode");
	}
	adjust();

	TManagerADT.showMsg("... starting:   putToRemote "+remoteTgt+"   "+Thread.currentThread()+" "+this+"  streams ="+_streamCount+" mode="+_transferMode);	
	
	File f = new File(localPath);
	long size = f.length();
	long start = System.currentTimeMillis();

	try {
	    doPut(localPath, remoteTgt);
	    return false;
	} catch (org.globus.ftp.exception.ServerException e) {
	    TManagerADT.showMsg("............... putToRemote to "+remoteTgt+" Server err, retry."+e.getMessage());	  
	    _err = e.getMessage();
	    return true;
	} catch (Exception e) {
	    TManagerADT.showMsg("............... putToRemote has err: "+remoteTgt+" "+e.getMessage());
	    e.printStackTrace();
	    _err = e.getMessage();
	    return false;
	} finally {
	    cleanUp();
	    if (_err == null) {
		long end = System.currentTimeMillis();
		setRate(start, end, size);
		_perf.txfSucc(size);
	    }

	    TManagerADT.showMsg("................ putToRemote done:"+Thread.currentThread()+" err="+_err);	   
	}
    }

    public void setLocalFile(String name, boolean useWriteMode, long size) {
	if (_perf == null) {
	    _perf = new TLocalFileMonitor(name, useWriteMode, size);
	} else {
	    _perf.setLocalFile(name, useWriteMode, size);
	}
    }
    /*
    public void setLocalFile(RandomAccessFile f, long size) {
	if (_perf == null) {
	    _perf = new TLocalFileMonitor(f, size);
	} else {
	    _perf.setLocalFile(f, size);
	}
    }
    */

    public void cleanUp() {
	TManagerADT.showMsg("cleanUp starts "+Thread.currentThread());
	if (_perf != null) {
	    _perf.cleanUp();
	}
	TManagerADT.showMsg("cleanUp ends "+Thread.currentThread());
    }

    public String getError() {
	return _err;
    }
    //public  performance()
}

interface IConnection {
    //public IConnection(String srcHost, String tgtHost);
    
    //public void startMe();
    //public void stopMe();

    //public IConnection clone();

    public void setStreamCount(int n);
    //public void resetStreamCount(int n);
    //public void reset();

    public int getStreamCount();

    //public boolean getFromRemote(String remoteSrc, String localTgt);
    //public boolean putToRemote(String localSrc, String remoteTgt);
    //public void transfer(gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer txf);

    ///public boolean exists(String path);
    //public boolean isFile(String path);
    ///public boolean list(String path, Vector result);

    //public long report();

    //public String getError();
}

/*
class TJobGet extends TJob //implements gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer
{
    String _localTgt = null;
    String _remoteSrc = null;

    public TJobGet(String remoteSrc, String localTgt) {
	_remoteSrc = remoteSrc;
	_localTgt = localTgt;	
    }

    public void run(IConnection c) {       	
	boolean retry = c.getFromRemote(_remoteSrc, _localTgt);
	TManagerADT.showMsg(_remoteSrc+" after one run(), msg="+c.getError());
	TManagerADT.showMsg(_remoteSrc+" after one run(), retry="+retry);
	int counter = 0;

	while (retry) {
	    TSRMUtil.sleep(20000);
	    TManagerADT.showMsg("retrying: "+counter);
	    c.reset();
	    retry = c.getFromRemote(_remoteSrc, _localTgt);
	    counter ++;
	    if (counter >5) {
		_err.append("AdaptError: Gsiftp failure!");
		break;
	    }
	}
	_isDone = true;
    }
}
*/
class TJobGet2 extends TJob
{
    String _localTgt = null;
    String _remoteSrc = null;

    //long _size = -1;
    //MySRMFileTransfer _txf = null;
    ITxf _txf = null;

    File _localDest = null;

    public TJobGet2(TSupportedURL src, TSupportedURL tgt, ITxf txf) 
    {
	super(txf);
	_localTgt = tgt.getURLString();
	_remoteSrc = src.getURLString();
	TManagerADT.showMsg("remoteSrc="+_remoteSrc);
        TManagerADT.showMsg("localTgt="+_localTgt);
	_txf = txf;

	_localDest = new File(tgt.getEffectivePath());
        _localDest.getParentFile().mkdirs();
    }


    public void checkSizeOnDisk() {
	TManagerADT.showMsg("checkSizeOnDisk"+_size+" _localDest="+_localDest.length());
	if (_size <= 0) {
	    _size = _localDest.length();
	}
	TManagerADT.showMsg("checkSizeOnDisk"+_size);
    }
}


class TJobPut2 extends TJob
{
    String _localSrc = null;
    String _remoteTgt = null;

    long _size = -1;

    public TJobPut2(TSupportedURL src, TSupportedURL tgt, ITxf txf) {
	super(txf);

	_localSrc = src.getURLString();
	_remoteTgt = tgt.getURLString();

	File f = new File(src.getEffectivePath());
	_size = f.length();
    }

    public void checkSizeOnDisk() {} // size is known       


    public void run(IConnection c) {	
	boolean failed = false;
	long startMillis = System.currentTimeMillis();
	TManagerADT.showMsg(Thread.currentThread()+"\t start:"+_remoteTgt);
	
	int streamCount = _txf.getParallel();

	try {
	    _txf.setParallel(streamCount);
	    _txf.transferSync();
	} catch (Exception e) {
	    failed =true;
	    e.printStackTrace();
	} finally {	    
	    _isDone = true;
	    if (failed) {
		return;
	    }
	    long endMillis = System.currentTimeMillis();
	    int dur = (int)((endMillis-startMillis)/(long)1000);	

	    double avgThrpt = TManagerADT.thrptAccumulater(startMillis, endMillis, _size);
	    //System.out.println("............... "+_size+"bytes in sec:"+dur);
	    TManagerADT.showMsg(Thread.currentThread()+"\t end:"+_remoteTgt+" status="+_txf.getStatus());
	    
	    int lastTxfRateBytesSecond = (int)(_size/(long)(dur));
	    //_lastTxfFinished = endMillis;
	
	    double rate =lastTxfRateBytesSecond/(double)1048576;
	    String sizeStr = _size+" bytes("+_size/1048576+"MB)";
	    
	    TManagerADT.showMsg(Thread.currentThread()+"\tTxfRate: "+sizeStr+"/"+dur+"(secs) \t="+ rate +"MB/s avgThrptInSystem (MB/sec):"+avgThrpt);

	    double throughput = ((double)_size)/(1048576*dur);
	    TTxfHandler._adtServer.progressUpdate(throughput, -1);
	    
	    long txfTimeMillis = endMillis - startMillis;
	    String ptmMsg = "PTM "+_size+" "+txfTimeMillis+" "+ streamCount;
	    _txf.logPTMMsg(ptmMsg, "size/dur/stream");

	    TTxfHandler._adtServer.progressUpdate(throughput, -1);	    	    
	}
    }    
}


/*
class TJobWrapper extends TJob {
    private MySRMFileTransfer _txf = null;
    long _size = -1;

    public TJobWrapper(MySRMFileTransfer txf) {
	_txf = txf;
    }

    public void run(IConnection c) {
	boolean failed = false;
	long startMillis = System.currentTimeMillis();
	TManagerADT.showMsg(Thread.currentThread()+"\t start: getting from "+_txf.getSource());
	try {
	    //_txf.setParallel(c.getStreamCount());
	    //_txf.setParallel(gov.lbl.adapt.srm.client.main.SRMClientN._staticClient.parallelism);
	    _txf.transferSync();
	} catch (Exception e) {
	    failed =true;
	    e.printStackTrace();
	} finally {	    
	    _isDone = true;
	    if (failed) {
		return;
	    }
	    long endMillis = System.currentTimeMillis();
	    int dur = (int)((endMillis-startMillis)/(long)1000);	

	    _size = _txf.getSourceFileSize();

	    double avgThrpt = TManagerADT.thrptAccumulater(startMillis, endMillis, _size);
	    //System.out.println("............... "+_size+"bytes in sec:"+dur);
	    TManagerADT.showMsg(Thread.currentThread()+"\t end:"+_txf.getSource()+" status="+_txf.getStatus());
	    
	    int lastTxfRateBytesSecond = (int)(_size/(long)(dur));
	    //_lastTxfFinished = endMillis;
	
	    double rate =lastTxfRateBytesSecond/(double)1048576;
	    String sizeStr = _size+" bytes("+_size/1048576+"MB)";
	    
	    TManagerADT.showMsg(Thread.currentThread()+"\tTxfRate: "+sizeStr+"/"+dur+"(secs) \t="+ rate +"MB/s avgThrptInSystem (MB/sec):"+avgThrpt);

	    double throughput = ((double)_size)/(1048576*dur);
	    TTxfHandler._adtServer.progressUpdate(throughput, -1);
	    
	    long txfTimeMillis = endMillis - startMillis;
	    String ptmMsg = "PTM "+_size+" "+txfTimeMillis+" "+ c.getStreamCount();
	    //gov.lbl.adapt.srm.client.main.SRMClientN.logMsg(ptmMsg, "size/dur/stream", "setRate()");

	    TTxfHandler._adtServer.progressUpdate(throughput, -1);	    	    
	}
    }	
}
*/
/*
class TJobSrmCompatible extends TJob {
    private gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer _srmTxf = null;

    public TJobSrmCompatible(gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer txf) {
	_srmTxf = txf;
    }

    public void run(IConnection c) {
	c.transfer(_srmTxf);
    }
}
*/

abstract class TJob implements Runnable //,gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer
{
    StringBuffer _err = null;
    boolean _isDone = false;

    ITxf _txf = null;

    long _size = -1;

    public TJob(ITxf txf) {
	_txf = txf;
    }

    abstract void checkSizeOnDisk();

    void run(IConnection c) {
	boolean failed = false;
	long startMillis = System.currentTimeMillis();
	TManagerADT.showMsg(Thread.currentThread()+"\t start: getting from "+_txf.getSrc());
	try {
	    _txf.transferSync();
	} catch (Exception e) {
	    failed =true;
	    e.printStackTrace();
	} finally {	    
	    _isDone = true;
	    if (failed) {
		return;
	    }
	    long endMillis = System.currentTimeMillis();
	    int dur = (int)((endMillis-startMillis)/(long)1000);	

	    checkSizeOnDisk();
	    //_size = _localDest.length();

	    double avgThrpt = TManagerADT.thrptAccumulater(startMillis, endMillis, _size);
	    TManagerADT.showMsg(Thread.currentThread()+"\t end:"+_txf.getSrc()+" status="+_txf.getStatus());
	    
	    int lastTxfRateBytesSecond = (int)(_size/(long)(dur));
	    //_lastTxfFinished = endMillis;
	
	    double rate =lastTxfRateBytesSecond/(double)1048576;
	    String sizeStr = _size+" bytes("+_size/1048576+"MB)";
	    
	    TManagerADT.showMsg(Thread.currentThread()+"\tTxfRate: "+sizeStr+"/"+dur+"(secs) \t="+ rate +"MB/s avgThrptInSystem (MB/sec):"+avgThrpt);

	    double throughput = ((double)_size)/(1048576*dur);
	    TTxfHandler._adtServer.progressUpdate(throughput, -1);
	    
	    long txfTimeMillis = endMillis - startMillis;
	    String ptmMsg = "PTM "+_size+" "+txfTimeMillis+" "+ c.getStreamCount();
	    _txf.logPTMMsg(ptmMsg, "size/dur/stream");

	    TTxfHandler._adtServer.progressUpdate(throughput, -1);	    	    
	}
    }

    public void run() {
	TThreadWithConnection currThread = (TThreadWithConnection)(Thread.currentThread());
	run(currThread.getConnection());
    }


    // from ISRMFileTransfer
    //
    public boolean transferDone() {
	return _isDone;
    }

    public String getErrorMsg() { //
	if (_err != null) {
	    return _err.toString();
	}
	return null;
    }   

    public String getStatus() {
	return getErrorMsg();
    }

}

class TJobPool {
    Vector _pending = new Vector();
    TSRMMutex _accessMutex = new TSRMMutex();

    public TJobPool () 
    {}

    public void add(ITxf srmTxf) {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return ;
	}
	try {
	    TSupportedURL srcUrl = TSupportedURL.create(TSRMUtil.createTSURL(srmTxf.getSrc()));
	    TSupportedURL tgtUrl = TSupportedURL.create(TSRMUtil.createTSURL(srmTxf.getTgt()));

	    if (srcUrl.isProtocolFILE()) {
		_pending.add(new TJobPut2(srcUrl, tgtUrl, srmTxf));		
	    } else if (tgtUrl.isProtocolFILE()) {	    
		_pending.add(new TJobGet2(srcUrl, tgtUrl, srmTxf));
	    }
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public void assertDir(String localTgt) {
	File f = new File(localTgt);
	if (f.isFile()) {
	    TManagerADT.showMsg("Unable to txf from dir to a file");
	    throw new RuntimeException("Unable to txf from dir to file");
	}
    }

    public TJob pop() {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return null;
	}
	try {
	    if (_pending.size() > 0) {
		TJob curr = (TJob)(_pending.get(0));
		_pending.remove(0);
		return curr;
	    }
	    return null;
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public boolean isFinished() {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return false;
	}
	try {
	    if (_pending.size() == 0) {
		return true;
	    }
	    return false;
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }

    public int getSize() {
	if (!TSRMUtil.acquireSync(_accessMutex)) {
	    return -1;
	}
	try {
	    return _pending.size();
	} finally {
	    TSRMUtil.releaseSync(_accessMutex);
	}
    }
}
	
class TThreadWithConnection extends Thread {
    IConnection _connection = null;
    public TThreadWithConnection(Runnable r, IConnection conn) {
	super(r);
	_connection = conn;
    }

    public IConnection getConnection() {
	return _connection;
    }
}

class TConnectionThreadFactory implements ThreadFactory {
    IConnection _connectionBase = null;
    public Vector _tempUseCurrentConn = new Vector();
    public Vector _test = new Vector();

    public Thread newThread(Runnable r) {	
	//TThreadWithConnection c = new TThreadWithConnection(r, _connectionBase.clone());
	TManagerADT.showMsg("......................... NO NEED FOR CONNECTION SHARING ..............................");
	TThreadWithConnection c = new TThreadWithConnection(r, null);

	_tempUseCurrentConn.add(c.getConnection());
	_test.add(c);
	return c;
    }

    public void setConnectionBase(IConnection base) {
	_connectionBase = base;
    }

    public IConnection getConnectionBase() {
	return _connectionBase;
    }
}



class TADTThreadPoolExecutor extends ThreadPoolExecutor {
    boolean _wrapUp = false;

    public TADTThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
				  TimeUnit unit, BlockingQueue<Runnable> workQueue) 
    {
	super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	TManagerADT.showMsg("... ADT pool init: core="+corePoolSize+" max="+maximumPoolSize);
    }


    public void execute(Runnable command) {
	try {
	    super.execute(command);
	} catch (Exception e) {
	    TManagerADT.showMsg(Thread.currentThread()+"Error when executing. pool/queue: "+getPoolSize()+"  "+getQueue().size()+" isDone? should retry?? "+command);
	    e.printStackTrace();
	}
    }

    public void wrapUp() {
	TManagerADT.showMsg("... wrapping up jobs..");
	_wrapUp =true;
    }

    protected void afterExecute(Runnable r, Throwable t) {	
	super.afterExecute(r, t);
	
	if (_wrapUp) {
	    return;
	}
	
	int p = TTxfHandler._adtServer.getConcurrencySuggestion(/*0, 0, 0, 1*/);
	if (TTxfHandler._atm != null) {
	    TTxfHandler._atm.fyi(p);
	}
	
	TThreadWithConnection tt = (TThreadWithConnection)(Thread.currentThread());
	int parallelStreamPerThread = 	TManagerADT._parallelism;

	int numThreadNeeded = p/parallelStreamPerThread;
	
	TManagerADT.showMsg(getPoolSize()+"...afterExecute: activeCount="+getActiveCount()+" p="+p+" div "+parallelStreamPerThread+" => coresize will be: "+numThreadNeeded);
	//if ((getActiveCount() != numThreadNeeded) && (numThreadNeeded > 0)) 
	if ((numThreadNeeded > 0) && (getPoolSize() != numThreadNeeded))
	{
	    try {
		setCorePoolSize(numThreadNeeded);
		setMaximumPoolSize(numThreadNeeded);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }
}

public class TTxfHandler implements Runnable {
    public static final long _DefNapMillis = 5000; 
    
    //
    // all connections
    //
    public static TADTThreadPoolExecutor _connectionPool = null;
    
    //
    //
    //public static IAdaptiveDataTxf _adtServer = null;
    public static TManagerADT _adtServer = null;
    
    //
    // 
    private TJobPool _txfJobs = new TJobPool();
    
    private static int _poolSize = 1;
    private static int _queueSize = 1;//1024;

    private boolean _isFinished = false;
    private static gov.lbl.adapt.srm.client.main.IPassiveTransferMonitor _ptm; 
    public static gov.lbl.adapt.atm.TATMImpl  _atm; 

    public TTxfHandler () {	
    }
        
    public static void updatePTM(long sizeBytes, long durMillis, int streamCount) {	
	if (_ptm != null) {
	    _ptm.update(sizeBytes, durMillis, streamCount); 
	}
    }


    public void init(String source, String target, long totalSize, int maxConcurrencyFromClient, int parallelism, int numFiles, int initConcurrency, int incrementalConcurrency) {
	init(source, target);

	if (parallelism < 1) {
	    parallelism = 1; // 
	}

	int maxStreams = maxConcurrencyFromClient; // default
	if (_atm != null) {
	    int maxStreamsFromATM = _atm.getConcurrencySuggestion(source,target, totalSize, numFiles);
	    TManagerADT.showMsg("[from ATM] .... max streams returned = "+maxStreamsFromATM);
	    if ((maxStreams <= 0) || (maxStreamsFromATM < maxStreams)) {
		maxStreams = maxStreamsFromATM;
	    }
	} else {
	    TManagerADT.showMsg("[NO ATM] .... using default streams = "+maxStreams);
	}

	if (initConcurrency > 0){
	    gov.lbl.adapt.adt.TManagerADT._gStart = initConcurrency; // init _gStart;
	}
	if (incrementalConcurrency > 0) {
	    gov.lbl.adapt.adt.TManagerADT._gStep  = incrementalConcurrency; // init _gStep;
	}

	int initStreams = gov.lbl.adapt.adt.TManagerADT._gStart * parallelism;    

	TManagerADT.showMsg("[from user input] .... _gStart/_gStep = "+gov.lbl.adapt.adt.TManagerADT._gStart+" "+gov.lbl.adapt.adt.TManagerADT._gStep);

	//while ((maxStreams <= 0) || (maxStreams < initStreams)) {                                                                                                            
	while (true) {
	    if (_atm != null) {
		_atm.getCurrSnapshot();
	    }
	    if (maxStreams >= initStreams) {
		break;
	    } else if (maxStreams <= 0) {
		maxStreams = _atm.getConcurrencySuggestion(source,target, totalSize, numFiles);
	    } else {
		int correspondingConcurrency = maxStreams/parallelism;
		if (correspondingConcurrency > 0) {
		    gov.lbl.adapt.adt.TManagerADT._gStart = correspondingConcurrency;
		    TManagerADT.showMsg("modified starting concurrency "+gov.lbl.adapt.adt.TManagerADT._gStart);
		    break;
		}
	    }
	    TManagerADT.showMsg("hibernating as suggested "+maxStreams+" "+initStreams);
	    gov.lbl.adapt.srm.util.TSRMUtil.sleep(5000);
	}

	setAD(maxStreams, parallelism);
    }

    private void init(String src, String tgt) {
	long keepAliveTime = 0;
        TimeUnit timeUnit = TimeUnit.SECONDS;
	//_queueSize = queueSize;
	_queueSize = _adtServer._gStart;
	_poolSize = _adtServer._gStart;

	_connectionPool = new TADTThreadPoolExecutor(_poolSize, _poolSize, 
						     keepAliveTime, timeUnit, new ArrayBlockingQueue(_queueSize));

	TSupportedURL srcUrl = TSupportedURL.create(TSRMUtil.createTSURL(src));
	TSupportedURL tgtUrl = TSupportedURL.create(TSRMUtil.createTSURL(tgt));

	if (srcUrl.isProtocolFILE() && tgtUrl.isProtocolFILE()) {
	    throw new RuntimeException("Invalid input. Both sites are local");
	}

	String remoteSite = null;
	if (srcUrl.isProtocolFILE()) {
	    remoteSite = tgtUrl.getHost();
	} else if (tgtUrl.isProtocolFILE()) {	    
	    remoteSite = srcUrl.getHost();	    
	} 

	TConnectionThreadFactory ctf = new TConnectionThreadFactory();
	
	//ctf.setConnectionBase(new TConnectionGSIFTP(remoteSite));
	ctf.setConnectionBase(new TConnectionGeneral(remoteSite));
	ctf.getConnectionBase().setStreamCount(TManagerADT._parallelism); // must do stream adjustment after list() is done.  

	_connectionPool.setThreadFactory(ctf);	

    }

    /*public void add(gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer srmTxf) {
	//_txfJobs._pending.add(srmTxf);
	_txfJobs.add(srmTxf);
    }
    

    public gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer createJob(MySRMFileTransfer existingTxf) 
    {
	return new TJobWrapper(existingTxf);
    }

    public gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer createJob(String src, String tgt, ITxf tu) {
	TManagerADT.showMsg("-------- create in ttxfhandler: "+src+"    "+tgt);

	TSupportedURL srcUrl = TSupportedURL.create(TSRMUtil.createTSURL(src));
	TSupportedURL tgtUrl = TSupportedURL.create(TSRMUtil.createTSURL(tgt));

	if (srcUrl.isProtocolFILE() && tgtUrl.isProtocolFILE()) {
	    throw new RuntimeException("Invalid input. Both sites are local");
	}
	
	String remoteSite = null;
	if (srcUrl.isProtocolFILE()) {
	    //return _txfJobs.createPut(srcUrl.getEffectivePath(), tgtUrl.getEffectivePath());
	    //RuntimeException hi = new RuntimeException("Not using TJob!!");
	    //hi.printStackTrace();

	    return new TJobPut2(srcUrl, tgtUrl);

	} else if (tgtUrl.isProtocolFILE()) {	    
	    return new TJobGet2(srcUrl, tgtUrl, tu);
	} 
	throw new RuntimeException("Unable to create jobs. Expecing file:// to be in one of src/tgt"+src+" "+tgt);
    }
    */
    public void add(ITxf txf)
    {
	TManagerADT.showMsg("-------- adding in ttxfhandler: "+txf.getSrc()+"    "+txf.getTgt());
	_txfJobs.add(txf);
    }

    /*
    public void add(String src, String tgt) {
	TManagerADT.showMsg("-------- add in ttxfhandler: "+src+"    "+tgt);
	TSupportedURL srcUrl = TSupportedURL.create(TSRMUtil.createTSURL(src));
	TSupportedURL tgtUrl = TSupportedURL.create(TSRMUtil.createTSURL(tgt));

	if (srcUrl.isProtocolFILE() && tgtUrl.isProtocolFILE()) {
	    throw new RuntimeException("Invalid input. Both sites are local");
	}
	
	String remoteSite = null;
	if (srcUrl.isProtocolFILE()) {
	    _txfJobs.initializePut(srcUrl.getEffectivePath(), tgtUrl.getEffectivePath());
	} else if (tgtUrl.isProtocolFILE()) {	    
	    _txfJobs.initializeGet(srcUrl.getEffectivePath(), tgtUrl.getEffectivePath(), 
				   ((TConnectionThreadFactory)(_connectionPool.getThreadFactory())).getConnectionBase());
	} 
    }
    */

    /*
    public void initDirectoryTxf(String src, String tgt, int parallelStreams, int poolSize, int queueSize)
    {
	_poolSize = poolSize;
	_queueSize = queueSize;

	TSupportedURL srcUrl = TSupportedURL.create(TSRMUtil.createTSURL(src));
	TSupportedURL tgtUrl = TSupportedURL.create(TSRMUtil.createTSURL(tgt));

	if (srcUrl.isProtocolFILE() && tgtUrl.isProtocolFILE()) {
	    throw new RuntimeException("Invalid input. Both sites are local");
	}

	long keepAliveTime = 0;
        TimeUnit timeUnit = TimeUnit.SECONDS;

        _connectionPool = new TADTThreadPoolExecutor(poolSize, poolSize, 
						     keepAliveTime, timeUnit, new ArrayBlockingQueue(queueSize));
						 
	TConnectionThreadFactory ctf = new TConnectionThreadFactory();

	if (srcUrl.isProtocolFILE()) {
	    ctf.setConnectionBase(new TConnectionGSIFTP(tgtUrl.getHost()));
	    _txfJobs.initializePut(srcUrl.getEffectivePath(), tgtUrl.getEffectivePath());
	}
	if (tgtUrl.isProtocolFILE()) {
	    ctf.setConnectionBase(new TConnectionGSIFTP(srcUrl.getHost()));
	    _txfJobs.initializeGet(srcUrl.getEffectivePath(), tgtUrl.getEffectivePath(), ctf.getConnectionBase());
	}
	ctf.getConnectionBase().setStreamCount(parallelStreams); // must do stream adjustment after list() is done.  

	_connectionPool.setThreadFactory(ctf);	
    }
    */
    //public void useProtocol(int p);
    
    //public static void setAD(IAdaptiveDataTxf adtServer) {
    public static void setAD(int maxStreams, int parallelism) {
	_adtServer = new TManagerADT(maxStreams, parallelism);
    }

    public static void setAD(TManagerADT adtServer) {
	_adtServer = adtServer;
    }

    public static void setPTM(gov.lbl.adapt.srm.client.main.IPassiveTransferMonitor ptm) {
	_ptm = ptm;
    }

    public static void setATM(gov.lbl.adapt.atm.TATMImpl atm) {
	_atm = atm;
    }

    public boolean hasJobs() {
	if (_txfJobs.isFinished()) {
	    return false;
	}
	return true;
    }
    /*
    public static int getADTParallelStreamSuggestion() {
	int activeCount = _connectionPool.getActiveCount();
	TManagerADT.showMsg("... active count in conenction pool ="+activeCount);

	if (activeCount != _poolSize) {
	    return -1; // no suggestion when pool is not in full gear
	}

	int adtSuggestion = _adtServer.getConcurrencySuggestion(0, 0, 0, activeCount);

	return adtSuggestion/activeCount;
	//TConnectionThreadFactory ctf = (TConnectionThreadFactory)(_connectionPool.getThreadFactory());	    
	//ctf.getConnectionBase().resetStreamCount(adtSuggestion/activeCount);	
    }
    */
    private void checkProgress() {
	/*
	TConnectionThreadFactory ctf = (TConnectionThreadFactory)(_connectionPool.getThreadFactory());

	long bytesTxfed = 0;
	int accumulatedConnectionStreams = 0;
	Vector cc = ctf._tempUseCurrentConn;
	for (int i=0; i<cc.size(); i++) {
	    IConnection c = (IConnection)(cc.get(i));
	    TManagerADT.showMsg("   checkProgress "+i+"th connection "+c);
	    bytesTxfed += c.report();
	    accumulatedConnectionStreams += c.getStreamCount();
	    TThreadWithConnection t = (TThreadWithConnection)(ctf._test.get(i));
	    TManagerADT.showMsg("       checkProgress  "+i+" "+t);
	    if (t != null) {
		TManagerADT.showMsg("  has state="+t.getState());
	    } else {
		TManagerADT.showMsg("  the thread is gone!");
	    }
	}
	
	double throughput = bytesTxfed*1000/(1048576*_DefNapMillis);
	TManagerADT.showMsg("==> [bytes txfed since last query]:"+bytesTxfed+" throughput="+throughput);

	//TTxfHandler._adtServer.progressUpdate(throughput, accumulatedConnectionStreams);
	TTxfHandler._adtServer.progressUpdate(throughput, 0);
	*/
    }

    public void setFinished() {
	_isFinished = true;
    }

    public boolean isFinished() {
	return _isFinished;
    }

    public void run() {
	_adtServer.startCollecting();
	long timeStation = System.currentTimeMillis();
	//while (hasJobs()) {	    

	boolean isDormant=false; // 
	while (!isFinished()) {
	    TManagerADT.showMsg("... job size="+_txfJobs.getSize()+" curr queue size:"+_connectionPool.getQueue().size()+" pool size:"+_connectionPool.getPoolSize()+" maxQueue:"+_queueSize);	    	    

	    if (_connectionPool.getPoolSize() > _txfJobs.getSize()) {
		_connectionPool.wrapUp();
	    }
	    if (!hasJobs()) {
		TManagerADT.showMsg("....no job!");
		nap();
		continue;
	    }

	    int currQueueSize = _connectionPool.getQueue().size();

	    if (currQueueSize < _queueSize) {
		for (int i=0; i<_queueSize-currQueueSize; i++) {
		    TJob curr = _txfJobs.pop();
		    if (curr == null) {
			break;
		    }
		    if (isDormant) {
			isDormant = false;
			if (!_adtServer._gDoPassiveADT) {
			    if (_atm != null) {
				int streamsAvail = _atm.getConcurrencySuggestion(null, null, -1, -1);  
				if (streamsAvail > 0) {
				    _adtServer.doStreamAdjustment(streamsAvail);
				}
			    }
			}
		    }
		    TManagerADT.showMsg("  job submitting:"+curr);
		    _connectionPool.submit(curr);
		    TManagerADT.showMsg("  job submitted "+curr);
		}
	    } else {
		isDormant = true;
		//_adtServer.doStreamAdjustment(streamsAvail);
		nap();
	    }
	    
	    /*
	    long curr = System.currentTimeMillis();
	    if (curr - timeStation >= _DefNapMillis) {
		//checkProgress();
		timeStation = curr;
	    }
	    */
	   timeStation = System.currentTimeMillis();
	}

	_connectionPool.shutdown();

	TManagerADT.showMsg("....shut down ordered. "+System.currentTimeMillis());
	while (!_connectionPool.isTerminated()) {
	    nap();
	    //checkProgress();
	}
	TManagerADT.showMsg("... shutting down "+System.currentTimeMillis());
    }


    private void nap() {
	TSRMUtil.sleep(_DefNapMillis);
    }

}
