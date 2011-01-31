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

//import gov.lbl.srm.policy.TSRMGeneralEnforcement;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.util.*;

import gov.lbl.srm.transfer.globus.SRMTransferProtocol;

import org.apache.axis.types.URI;
//import java.io.File;
import java.util.Vector;
//import java.util.Iterator;
import EDU.oswego.cs.dl.util.concurrent.Mutex;

public class TSRMLocalFileOnUserDisk extends TSRMLocalFile {
	public TSRMLocalFileOnUserDisk(TSRMLocalDir parent, String name, TFileStorageType fileType) {
		super(parent, name, null, true);
	}
	
	public String getCanonicalPath() {
		return null;
	}
	public TSRMLocalDir getParent() {
		return null;
	}
	public TSRMPhysicalLocation getPhysicalLocation() {
		return null;
	}
	public URI getTxfURI(SRMTransferProtocol protocol) {
		return null;
	}
	 
	
	public TSRMPin getPin(TSRMRequest r) {
		return null;
	}
	public TSRMLocalDir getTopDir() {
		return null;
	}
	public void changeParentTo(TSRMLocalDir p){
		throw new TSRMException("TSRMLocalFileOnUserDisk::changeParentTo() is not supported", false);
	}
 
	public TReturnStatus mv(TSURLInfo tgtPath) {
		return TSRMUtil.createReturnStatus(TStatusCode.SRM_FAILURE, "TSRMLocalFileOnUserDisk::mv() is not supported");
	}
	
 
	public Vector ls(Boolean isAllLevelRecursive,
			 Integer numOfLevels,
			 TFileStorageType fileStorageType,		 
			 Boolean doFullDetailedList,
			 Integer outputCount,
			 Integer offset) 
	{
		return null;
	}
}
