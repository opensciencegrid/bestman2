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

import java.util.*;

import gov.lbl.srm.StorageResourceManager.*;

public class TSRMRequestStatusReturn {
    public static final int gFINISHED = 0;
    public static final int gFAILED   = 1;
    public static final int gQUEUED   = 2;
    public static final int gPROGRESSED   = 3;

    private int[] _stats = new int[4];

    public TSRMRequestStatusReturn() {
	for (int i=0; i<_stats.length; i++) {
	    _stats[i] = 0;
	}
    }

    private void add(int code) {
	_stats[code]++;
    }

    public void add(TStatusCode code) {
	if (code == TStatusCode.SRM_REQUEST_QUEUED) {
	    add(gQUEUED);
	} else if (code == TStatusCode.SRM_REQUEST_INPROGRESS) {
	    add(gPROGRESSED);
	} else if ((code == TStatusCode.SRM_SUCCESS) || (code == TStatusCode.SRM_FILE_PINNED) || (code == TStatusCode.SRM_DONE) ||
		   (code == TStatusCode.SRM_RELEASED) || (code == TStatusCode.SRM_ABORTED))
	{
	    add(gFINISHED);
	} else if (code == TStatusCode.SRM_SPACE_AVAILABLE) {
	    //add(gPROGRESSED);
	    add(gFINISHED);
	} else if (code == TStatusCode.SRM_FILE_IN_CACHE) { // intermediate step for put(to permanent space) and copy-remote
	    add(gPROGRESSED);
	} else {
	    add(gFAILED);
	}
    }

    public TStatusCode getStatusCode() {
	if (_stats[gQUEUED] + _stats[gPROGRESSED] > 0) {
	    if ((_stats[gFINISHED] == 0) && (_stats[gFAILED] == 0)) {
		if (_stats[gPROGRESSED] == 0) {
		    return TStatusCode.SRM_REQUEST_QUEUED;
		} 
	    }
	    return TStatusCode.SRM_REQUEST_INPROGRESS;	    
	} 

	if (_stats[gFAILED] == 0) {
	    return TStatusCode.SRM_SUCCESS;	    
	} else if (_stats[gFINISHED] == 0) {		
	    return TStatusCode.SRM_FAILURE;
	} else {
	    return TStatusCode.SRM_PARTIAL_SUCCESS;
	}
    }

    public void collect(TPutRequestFileStatus[] statusArray) {
	if (statusArray == null) {
	    return;
	}
	for (int i=0; i<statusArray.length; i++) {
	     TPutRequestFileStatus status = statusArray[i];
	     if (status != null) {
		 TStatusCode code = status.getStatus().getStatusCode();
		 this.add(code);
	     }
	}
    }

    public void collect(TMetaDataPathDetail[] statusArray) {
	if (statusArray == null) {
	    return;
	}
	for (int i=0; i<statusArray.length; i++) {
	     TMetaDataPathDetail status = statusArray[i];
	     add(status);
	}
    }

    public void add(TMetaDataPathDetail status) {
	if (status != null) {
	    TStatusCode code = status.getStatus().getStatusCode();
	    if (code != TStatusCode.SRM_FILE_BUSY) {
		this.add(code);
	    } else {
		this.add(TStatusCode.SRM_SUCCESS);
	    }
	}
    }

    public void add(TMetaDataSpace status) {
	if (status != null) {
	    TStatusCode code = status.getStatus().getStatusCode();
	    if (code != TStatusCode.SRM_SPACE_LIFETIME_EXPIRED) {
		this.add(code);
	    } else {
		this.add(TStatusCode.SRM_SUCCESS);
	    }
	}
    }
}
