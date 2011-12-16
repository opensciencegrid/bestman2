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

package gov.lbl.srm.impl;

import gov.lbl.srm.util.TSRMHelp;
import  gov.lbl.srm.server.*;

public class TSRMServiceGateway extends TSRMService {
    static String[] _supportedEntryNamesInConfigXrootdFile = 
        {
	 ConfigGateway._DefConfigCheckXrootdFS, TSRMHelp._hasDefault+ConfigGateway._doCheckSizeWithXrootdFS,
	 ConfigGateway._DefConfigCheckSizeGsiftp, TSRMHelp._hasDefault+ConfigGateway._doCheckSizeWithGsiftp,
	 ConfigGateway._DefConfigEnableRecursionInLs, TSRMHelp._hasDefault+ConfigGateway._enableRecursionInLs,
	 ConfigGateway._DefConfigCheckXrootdToken, TSRMHelp._hasDefault+ConfigGateway._doCheckToken,
	 ConfigGateway._DefConfigGatewayTokenCompName, TSRMHelp._noDefault,
	 ConfigGateway._DefConfigGatewayFSAC, TSRMHelp._hasDefault+ConfigGateway._fsConcurrency
	 //ConfigGateway._DefConfigXrootdStatusTokens, TSRMHelp._noDefault
	};
 
    public TSRMServiceGateway() {
	super(false); 
	
	try {
	    ConfigGateway.load(Config._configFileNameLoaded);
	} catch (Exception e) {
	    e.printStackTrace();
	    gov.lbl.srm.util.TSRMUtil.startUpInfo("BeStMan is exiting due to configuration error!"+e.toString());
	    System.exit(1);
	}
	_server = new SRMGateway();
    }


    public static boolean knows(String curr) {
	boolean found = false;
        for (int i=0; i<_supportedEntryNamesInConfigXrootdFile.length; i++) {
	    if (curr.equals(_supportedEntryNamesInConfigXrootdFile[i])) {
		found = true;
		break;
	    }
	}
	return found;
    }
}
