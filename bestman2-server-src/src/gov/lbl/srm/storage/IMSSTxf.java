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

package gov.lbl.srm.storage.Permanent;

import gov.lbl.srm.util.*;
import gov.lbl.srm.server.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.SRMTransferProtocol;
import gov.lbl.srm.transfer.mss.*;
import gov.lbl.srm.transfer.mss.intf.*;
//import gov.lbl.srm.transfer.mss.hpss.*;

//import srm.common.StorageResourceManager.*;
import gov.lbl.srm.StorageResourceManager.*;

import java.io.File;
import java.util.*;

public interface IMSSTxf {
    public Vector getStatusReport(String keyPrefix);
    public String getHomeDir(TDeviceAccessInfo info);
    public void mkdir(String topDir, String[] subdirs, TDeviceAccessInfo accessInfo, boolean pathIsAFile);
    public TMetaDataPathDetail ls(TDeviceAccessObj hpssPathObj, boolean recursive, boolean nocipher, boolean nonpassivelisting);
    public long getSize(TDeviceAccessObj pathRef);
    public String remove(TDeviceAccessObj hpssPathObj, boolean isDir, boolean recursive);
    public String bringToDisk(TDeviceAccessObj from, TSRMLocalFile tgt);
    public void abort(String rid);
    public String loadFromDisk(File fileOnDisk, TSRMLocalFile hpssFile);
    public void cp(TDeviceAccessObj src, TDeviceAccessObj tgt, SRM_OVERWRITE_MODE overwriteMode, boolean recursive);
    
}
