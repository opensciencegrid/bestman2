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

package gov.lbl.adapt.srm.client.main;

import org.ietf.jgss.GSSCredential;
import org.gridforum.jgss.ExtendedGSSManager;
import org.globus.gsi.gssapi.auth.Authorization;

import org.globus.util.GlobusURL;
import java.io.IOException;
import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.client.intf.MyISRMFileTransfer;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MySRMFileTransfer extends SRMFileTransfer implements MyISRMFileTransfer 
{
    public String _sourceStr = null;
    public String _targetStr = null;

public MySRMFileTransfer () {
  super();
}

public void setSessionType(int sessionType) {
}

public void setSessionMode(int sessionType) {
}

public void setLogger (Log log) { }

public void setLogger(Log log, java.util.logging.Logger logger, boolean silent, boolean useLog, boolean debug) {
    super.setLogger(log,logger,silent,useLog,debug);
}
    
public MySRMFileTransfer (String fromu, String tou) {
    super(fromu, tou);
    _sourceStr = fromu;
    _targetStr = tou;
}

    /*
public MySRMFileTransfer (String fromu, String tou, int parallelu) {
    super(fromu, tou, parallelu);
}

public MySRMFileTransfer (String fromu, String tou, SRMTransferProtocol ttypeu) {    
    super(fromu, tou, ttypeu); 
}

public MySRMFileTransfer (String fromu, String tou, SRMTransferMode tmodeu) {
    super(fromu, tou, tmodeu);
}

public MySRMFileTransfer (String fromu, String tou, GSSCredential credentials) {    
    super(fromu, tou, credentials);
}

public MySRMFileTransfer (String fromu, String tou, int parallelu, boolean listen) {    
     super(fromu, tou);
}
    */

public void setTransferType(SRMTransferProtocol ttypeu) {
    super.setTransferType(ttypeu);
}
    
public void setTransferMode(SRMTransferMode mode) {
    super.setTransferMode(mode);
}

public void setParallel(int parallelu) {
    super.setParallel(parallelu);
}

public void setCredentials(GSSCredential credentials) {
    super.setCredentials(credentials);
}

public void setBufferSize(int size) {
    super.setBufferSize(size);
}

public void setBlockSize(int size) {
    super.setBlockSize(size);
}

public void setDCAU(boolean dcau) {
    super.setDCAU(dcau);
}

public void start() {
    //FileTransferTest._adtTxf.add(_sourceStr, _targetStr);
    super.start();
     ////super.start();
}

}
