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

package gov.lbl.srm.util.sleepycat;

import gov.lbl.srm.server.*;
import gov.lbl.srm.util.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.StorageResourceManager.*;

import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

public class TFileBinding extends TSRMTupleBinding {
    public Object entryToObject(TupleInput ti) {
	String tid = ti.readString();
	String canonicalPath = ti.readString();
	String sourceUriStr = ti.readString();
	long bytes = ti.readLong();
	boolean isUserAppt = ti.readBoolean();
	boolean isSurl = ti.readBoolean();			
	
	TFileStorageType fileType = TFileStorageType.fromValue(ti.readString());       		

	String physicalPath = ti.readString();
	long expirationTime = ti.readLong();

	System.out.println(".............."+canonicalPath+"  isSurl="+isSurl+" fileType="+fileType);
	TSRMLocalFile result = getFile(canonicalPath, isSurl, fileType);
	System.out.println("..........."+result);
	if (result == null) {
	    return null;
	}

	try {
	    TSRMStorage.iSRMSpaceToken token = (TSRMStorage.iSRMSpaceToken)(TSRMSleepyCatDbReader._tokensRead.get(tid));
	    if (token == null) {
		TSRMUtil.startUpInfo("No such token: "+tid+" for file:"+canonicalPath);
		removeTrace(result, physicalPath);
		return null;
	    }

	    if ((expirationTime > 0) && (System.currentTimeMillis() > expirationTime)) {
		TSRMUtil.startUpInfo("File is expired!"+canonicalPath+"  "+(System.currentTimeMillis()-expirationTime)+" ago");
		removeTrace(result, physicalPath);
		return null;
	    }
	    
	    if ((result.getToken() != null) && (!result.getToken().getID().equals(tid))) {
		TSRMUtil.startUpInfo("Mismatched token: "+tid+" vs: "+result.getToken().getID()+" for file:"+canonicalPath);
		removeTrace(result, physicalPath);
		return null;
	    }


	    TSRMCacheLog.readSourceURI(result, sourceUriStr);
	    TSRMUtil.startUpInfo(".....got suri="+result.getSourceURI());

	    token.findSpaceForFile(result, bytes);

	    result.setPhysicalLocation(physicalPath, null);
	    	   
	    long actualBytes = result.getCurrentSize();

	    if (actualBytes <= 0) {
		removeTrace(result, null);
		TSRMUtil.startUpInfo("Actual file is not found. "+" for file:"+canonicalPath);
		return null;
	    }
	    
	    if (expirationTime > 0) {
		result.setInheritedLifeTime(expirationTime);
	    }

	    if (actualBytes != bytes) { 
		TSRMLog.debug(getClass(), null, "error=conflictedSizeFound action=removingFileFromDisk", "path="+result.getCanonicalPath());
		removeTrace(result, null);
		return null;
	    }

	    result.updateToken();

	    if (!result.isSurl()) {
		TSRMUtil.startUpInfo("==> loaded turl"+result.getCanonicalPath());
		TSRMNameSpace.addEntry(result.getSourceURI().toString());
		TSRMNameSpace.addEntry(result.getSourceURI().toString(), result, null);
	    } else {
		TSRMUtil.startUpInfo("==> loaded SURL"+result.getCanonicalPath());
		TSRMNameSpace.addEntry(result.getSiteURL().getSURL().toString());
		TSRMNameSpace.addEntry(result.getSiteURL().getSURL().toString(), result, null);
	    }

	    return result;
	} catch (Exception e) {	    
	    TSRMLog.exception(TFileBinding.class, "details", e);
	    removeTrace(result, null);
	    return null;
	}
    }

    private void removeTrace(TSRMLocalFile f, String physicalPath) {
	if (physicalPath != null) {
	    //java.io.File ff = new java.io.File(physicalPath);
	    //ff.delete();
	    TSRMUtil.cleanFilePath(physicalPath);
	}
	//f.deleteMe(false);

	f.unsetTokenIndependant(false);
	TSRMUtil.wipe(f);

    }

    private TSRMLocalFile getFile(String path, boolean isSurl, TFileStorageType fileType) {
	String slash = path.substring(0,1);
	String[] pathTokens = path.substring(1).split(slash);

	String uid = pathTokens[0];
	TAccount user = TAccountManager.getAccount(uid, true);
	if (user == null) {
	    TSRMLog.debug(this.getClass(), null, "event=recreatingFailed", "uid="+uid);
	    return null;
	}

	TSRMLocalDir dir = user.getTopDir();

	for (int i=1; i<pathTokens.length-1; i++) {
	    String curr = pathTokens[i];
	    dir = dir.createDir(curr, isSurl);
	}

	return dir.createSubFile(pathTokens[pathTokens.length-1], fileType, isSurl);
    }

    public void objectToEntry(Object object, TupleOutput to) {
	TSRMLocalFile f = (TSRMLocalFile)object;
	TSRMStorage.iSRMSpaceToken token = f.getToken();
	long reservedSize = f.getReservedBytes();

	to.writeString(token.getID());
	to.writeString(f.getCanonicalPath());
	//to.writeString(f.getSourceURI().toString());
	to.writeString(TSRMCacheLog.writeSourceURI(f));
	to.writeLong(reservedSize);

	to.writeBoolean(f.isSFNUserAppointed());
	to.writeBoolean(f.isSurl());
	to.writeString(f.getFileStorageType().toString());

	if (f.getPhysicalPath() != null) {
	    to.writeString(f.getPhysicalPath().toString());
	    to.writeLong(f.getExpirationTimeInMilliSeconds());
	} else {
	    to.writeString("");
	    to.writeLong(0);
	}
    }

    public static void writeMe(TSRMLocalFile f, Database db) 
    {
	TupleBinding fileBinding = new TFileBinding();
	setKey(f.getCanonicalPath());
	fileBinding.objectToEntry(f, _data);

	writeData(db);	
    }
}
