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

 
public class TSRMFileTxfState {
    public static final TSRMFileTxfState INITPULL = new TSRMFileTxfState(3);
    public static final TSRMFileTxfState CLIENTPUSH = new TSRMFileTxfState(2);
    public static final TSRMFileTxfState NONE = new TSRMFileTxfState(1);
    public static final TSRMFileTxfState TRANSITION = new TSRMFileTxfState(4); // e.g. 

    private static final String strNONE = "false";
    private static final String strCLIENTPUSH = "clientpush";
    private static final String strINITPULL = "initpull";
    private static final String strTRANSITION = "transition";
    
    private int _state = 1;

    private TSRMFileTxfState(int state) {
	_state = state;
    }

    public boolean isDownloading() {
	return (this == TSRMFileTxfState.INITPULL);
    }
    public boolean isUploading() {
	return (this == TSRMFileTxfState.CLIENTPUSH);
    }

    public boolean doBlocking() {
	if (this == TSRMFileTxfState.NONE) {
	    return false;
	}
	return true;
    }

    public String toString() {
	if (this == TSRMFileTxfState.NONE) {
	    return TSRMFileTxfState.strNONE;
	} else if (this == TSRMFileTxfState.CLIENTPUSH) {
	    return TSRMFileTxfState.strCLIENTPUSH;
	} else if (this == TSRMFileTxfState.TRANSITION) {
	    return TSRMFileTxfState.strTRANSITION;
	} else {
	    return TSRMFileTxfState.strINITPULL;
	}
    }
}
