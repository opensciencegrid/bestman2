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

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.transfer.*;
import org.ietf.jgss.GSSCredential;
import gov.lbl.srm.client.SRMClient;
import javax.xml.rpc.Stub;
import java.io.*;


public class TSRMUploadToRemoteSRM implements ISRMTxfHandler {
    private String _token = null;
    private TSRMContactRemoteSRM _srmContactObj = null;
    private /*ISRMLocalPath*/ISRMTxfEndPoint _localSrc = null;
    private TSURLInfo _tgt = null;
    private TDirOption _dirOp = null;
    private Integer _tgtLifetime = null;
    private TOverwriteMode _overwriteMode = null;
    private TFileStorageType _fileStorType = null;

    public TSRMUploadToRemoteSRM(TSRMContactRemoteSRM srm, 
				 /*ISRMLocalPath*/ISRMTxfEndPoint src,
				 TSURLInfo tgt,
				 String token,
				 Integer dur,
				 TDirOption dirOp,
				 TOverwriteMode mode,
				 TFileStorageType f)
    {
	_srmContactObj = srm;
	_token = token;
	_tgt = tgt;
	_localSrc = src;
	_dirOp = dirOp;
	_tgtLifetime = dur;
	_overwriteMode = mode;
	_fileStorType = f;
    }

    public void action() {       
	/*
	if (_localSrc.isDir()) {
	    throw new TSRMException("We do not support uploading a dir to a remote site.", false);
	}
	*/
	_srmContactObj.putOnePath(/*(TSRMLocalFile)*/_localSrc, _tgt, _token, _tgtLifetime, _overwriteMode, _fileStorType);
       
	//_srmContactObj.srmPutDone();	
    }

    public void abort() {
	_srmContactObj.abort();
    }
}
