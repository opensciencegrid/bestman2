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

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.transfer.globus.*;
import gov.lbl.srm.transfer.*;
import org.ietf.jgss.GSSCredential;
import gov.lbl.srm.client.SRMClient;
import javax.xml.rpc.Stub;
import org.apache.axis.types.*;
import java.io.*;

public class TSRMDownloadFromRemoteSRM implements ISRMTxfHandler {
    private TSURLInfo _src = null;
    private String _token = null;
    private TSRMContactRemoteSRM _srmContactObj = null;
    private TSRMLocalFile _tgt  = null;
    private GSSCredential _cred = null;

    private ISRMTxfHandler _auxDownloadHandler = null;

    public TSRMDownloadFromRemoteSRM(TSRMContactRemoteSRM srmContact, 
				     TSURLInfo src, 
				     String token,
				     TSRMLocalFile tgt,
				     GSSCredential cred) 
    {
	_srmContactObj = srmContact;
	_src = src;
	_token = token;
	_tgt = tgt;
	_cred = cred;
    }
    
    public void action() {
	_src.printMe();

	URI turl = _srmContactObj.getOneFile(_src, _token);

	if (turl != null) {
	    //TSupportedURL.download(turl.getValue(), _tgt, _cred);  should create a download obj for this too, and abort accordingly?
	    TSRMLog.debug(this.getClass(), null, "event=receivedRemoteTURL value=\""+turl.toString()+"\"", "src="+_src.getSURL());
	    TSURLInfo from = TSRMUtil.createTSURLInfo(turl);
	    from.setStorageSystemInfo(_src.getStorageSystemInfo()); // inherite the source accessinfo

	    TSupportedURL temp = TSupportedURL.create(from);
	    temp.useCredential(_cred);

	    _srmContactObj.extendLifeTime(_src.getSURL(), 3600);
	    _auxDownloadHandler = temp.downloadTo(_tgt);
	    _auxDownloadHandler.action();

	    _srmContactObj.releaseFile(_src.getSURL());
	} else {
	    throw new TSRMException("Download failed.", false);
	}
    }

    public void abort() {
	if (_srmContactObj != null) {
	    _srmContactObj.abort();
	}

	if (_auxDownloadHandler != null) {
	    _auxDownloadHandler.abort();
	}
    }
}

