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

public class StaticToken {
  String _id;
  String _desc;
  String _owner = null;
  java.io.File _localPath = null;
  TRetentionPolicyInfo _rInfo = null;
  Integer _givenTotalSizeGB = null;
  String _checkSizeCommand = null;
  String _totalSizeCommand = null;
  long _unit = 1;
  TMetaDataSpace _metadata = new TMetaDataSpace();

  public StaticToken(String id, String desc, Integer size) {
	_id = id;
	_desc = desc;
	_givenTotalSizeGB = size;
  }

  public String getID() {
	return _id;
  }

  public String getDesc() {
      return _desc;
  }

    public String getOwner() {
	return _owner;
    }

    public TRetentionPolicyInfo getRetentionPolicyInfo() {
	return _rInfo;
    }

  public long getTotalBytes() {
	if (_totalSizeCommand != null) {
	    if (_localPath != null) {
		String hoho = TPlatformUtil.execShellCmdWithOutput(_totalSizeCommand+" "+_localPath, true);
		return runCommand(hoho);
	    } else {
		String hoho = TPlatformUtil.execShellCmdWithOutput(_totalSizeCommand, true);
		return runCommand(hoho);	  
	    }
	}

        if (_givenTotalSizeGB != null) {
	   return (long)_givenTotalSizeGB.intValue()*(long)1073741824;
	}

	return 0;
  }

  public static StaticToken[] fromInput(String[] input) {
	 if (input == null) {
	     return null;
	 }

	 StaticToken[] result = new StaticToken[input.length];
	 for (int i=0; i<input.length; i++) {
	      String curr = input[i];
	      int pos = curr.lastIndexOf("]"); 
	      if (pos == -1) {
		  result[i] = new StaticToken(curr, null, null);
	      } else if (pos != curr.length()-1) {
		  result[i] = new StaticToken(curr, null, null);
	      } else {
		  int cursor = curr.lastIndexOf("[", pos);
		  if (cursor <= 0) {
		      result[i] = new StaticToken(curr, null, null);
		  } else {
		      /*
		      String val = curr.substring(cursor+1, pos);
		      Integer size = null;
		      String desc = null;
		      if (!val.startsWith("desc:")) {
			  size = Integer.valueOf(val);
		      } else {
			  desc = val.substring(5);
		      }
		      if (curr.charAt(cursor-1) == ']') {
			  int cursor0 = curr.lastIndexOf("[", cursor-1);
			  if (cursor0 > 0) {
			      val = curr.substring(cursor0+1, cursor-1);			  
			      if (!val.startsWith("desc:")) {
				  if (size == null) {
				      size = Integer.valueOf(val);
				  } 
			      } else if (desc == null) {
				  desc = val.substring(5);
			      }
			  }
			  result[i] = new StaticToken(curr.substring(0, cursor0), desc, size);
		      } else {   				  
			  if (size == null) {
				      size = Integer.valueOf(val);
				  } 
			      } else if (desc == null) {
				  desc = val.substring(5);
			      }
			  }
			  result[i] = new StaticToken(curr.substring(0, cursor0), desc, size);
		      } else {   				  
			  result[i] = new StaticToken(curr.substring(0, cursor), desc, size); 		  		      
		      }
		      */
		      result[i] = createToken(curr);
		  }
	      }

	 }
	return result;
  }

    public TMetaDataSpace getMetadata() {
	if (_metadata.getOwner() == null) {
	    _metadata.setOwner(getOwner());
	}
	if (_metadata.getSpaceToken() == null) {
	    _metadata.setSpaceToken(getID());
	}
	if (_metadata.getStatus() == null) {
	    _metadata.setStatus(TSRMUtil.createReturnStatus(TStatusCode.SRM_SUCCESS, getDesc()));
	}
	long totalBytes = getTotalBytes();
	//if (_metadata.getTotalSize() == null) {
	    _metadata.setTotalSize(new org.apache.axis.types.UnsignedLong(totalBytes));
	//}
	//if (_metadata.getGuaranteedSize() == null) {
	    //_metadata.setGuaranteedSize(new org.apache.axis.types.UnsignedLong(getTotalBytes()));
	    _metadata.setGuaranteedSize(_metadata.getTotalSize());
	//}
	if (_metadata.getRetentionPolicyInfo() == null) {
	    _metadata.setRetentionPolicyInfo(getRetentionPolicyInfo());
	}
	if (_metadata.getLifetimeAssigned() == null) {
	    _metadata.setLifetimeAssigned(new Integer(-1));
	}
	if (_metadata.getLifetimeLeft() == null) {
	    _metadata.setLifetimeLeft(new Integer(-1));
	}
	_metadata.setUnusedSize(new org.apache.axis.types.UnsignedLong(getUnusedSize(totalBytes)));
	
	return _metadata;
    }

    private long runCommand(String hoho) {
	//String hoho = TPlatformUtil.execShellCmdWithOutput(_checkSizeCommand+" "+_localPath, true);
	String output = null;
	int pos = hoho.indexOf("OUTPUT:");
	if (pos < 0) { 
	    if (hoho.startsWith("ERROR")) {
		return 0;	
	    }
	    output = hoho;
	} else {
	    output = hoho.substring(pos+7);
	}
	
	int pos2 = output.indexOf("\t");
	if (pos2 == -1) {
	    pos2 = output.indexOf(" ");
	}
	long result = -1;
	if (pos2 == -1) {
	    result = Long.valueOf(output);
	} else {
	    result =Long.valueOf(output.substring(0, pos2));
	}
	if (result < 0) {
	    TSRMLog.debug(this.getClass(), null, "event=runCommand", "errorResult="+result);
	    return 0;
	}
	return result;
    }

    private long calculateUnusedBytes(String hoho, long totalBytes) {
	long result = runCommand(hoho);
	if (result > totalBytes) {
	    TSRMLog.debug(this.getClass(), null, "event=getUnusedBytes", "errorResultSmall="+result);
	    return 0;
	} else {
	    return totalBytes - result*_unit;
	}
    }

    private long getUnusedSize(long totalBytes) {
	if (totalBytes <= 0) {
	    TSRMLog.debug(this.getClass(), null, "event=getUnusedBytes", "error=noTotalBytes");
	    return 0;
	}

	if (_localPath == null) {
	    TSRMLog.debug(this.getClass(), null, "event=getUnusedBytes", "checkSizeCommand="+_checkSizeCommand);
	    if (_checkSizeCommand != null) {
		String hoho = TPlatformUtil.execShellCmdWithOutput(_checkSizeCommand, true);
		return calculateUnusedBytes(hoho, totalBytes);
	    }
	    return 0;
	} 
	try {
	    if (!_localPath.exists()) {
		TSRMLog.debug(this.getClass(), null, "event=getUnusedBytes", "error=localPathNonExist");
		return 0;
	    }
	    
	    TSRMLog.debug(this.getClass(), null, "event=getUnusedBytes", "command="+_checkSizeCommand);
	    if (_checkSizeCommand != null) {
		String hoho = TPlatformUtil.execShellCmdWithOutput(_checkSizeCommand+" "+_localPath, true);
		return calculateUnusedBytes(hoho, totalBytes);
	    }

	    long result = _localPath.getUsableSpace();

	    if (result > getTotalBytes()) {
		return getTotalBytes();
	    }
	   
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return 0;
	}
    }

    private static StaticToken createToken(String tokenDef) {
	int pos = tokenDef.indexOf("[");
	if (pos == -1) {
	    return new StaticToken(tokenDef, null, null);
	}
	String tid = tokenDef.substring(0, pos);
	StaticToken result = new StaticToken(tid, null, null);
	
	int next = tokenDef.indexOf("]", pos+1);
	while (next != -1) {
	    String value = tokenDef.substring(pos+1, next);
	    result.setAttrValue(value);
	    pos = tokenDef.indexOf("[", next+1);
	    if (pos == -1) {
		break;
	    }
	    next = tokenDef.indexOf("]", pos+1);
	}
	return result;
    }
    
    private void setAttrValue(String value) {
	if (value.startsWith("desc:")) {
	    _desc = value.substring(5);
	} else if (value.startsWith("owner:")) {
	    _owner = value.substring(6);
	} else if (value.startsWith("retention:")) {
	    if (_rInfo == null) {
		_rInfo = new TRetentionPolicyInfo();
	    }
	    _rInfo.setRetentionPolicy(TRetentionPolicy.fromValue(value.substring(10)));
	} else if (value.startsWith("latency:")){
	    if (_rInfo == null){
		_rInfo = new TRetentionPolicyInfo();
	    }
	    _rInfo.setAccessLatency(TAccessLatency.fromValue(value.substring(8)));
	} else if (value.startsWith("size:")) {
	    _givenTotalSizeGB = Integer.valueOf(value.substring(5));
	    if (_givenTotalSizeGB.intValue() <= 0) {
		TSRMUtil.startUpInfo("Invalid total size:"+_givenTotalSizeGB.intValue()+", Ignored.");
		_givenTotalSizeGB = null;
	    }
	} else if (value.startsWith("path:")) {
	    //_localPath = new java.io.File(value.substring(5));
	    _localPath = TSRMUtil.initFile(value.substring(5));
	    if (!_localPath.exists()) {
		TSRMUtil.startUpInfo("This path:"+_localPath.getPath()+" is invalid.");
		System.exit(1);
	    }
	    //testCommandAndPath(null);
	} else if (value.startsWith("usedBytesCommand:")) {
	    _checkSizeCommand = value.substring(17);	    
	    testCommandAndPath(_checkSizeCommand);
	} else if (value.startsWith("totalBytesCommand:")) {
	    _totalSizeCommand = value.substring(18);
	    testCommandAndPath(_totalSizeCommand);
	} else if (value.startsWith("unit:")) {
	    String unitValue = value.substring(5);
	    if (unitValue.equalsIgnoreCase("kb")) {
	      _unit = 1024;
	    } else if (unitValue.equalsIgnoreCase("mb")) {
	      _unit = 1048576;
	    } else if (unitValue.equalsIgnoreCase("gb")) {
	      _unit = 1073741824;
	    }
	} else {
	    try {
	    	_givenTotalSizeGB = Integer.valueOf(value);
	    } catch (Exception e) {
		String err = "Invalid entry:["+value+"]. Either should be integer or starts with one of these keywords: desc:/owner:/retetion:/latency:/size:/path:/usedBytesCommand:";
		throw new TSRMException(err, false);
	    }
	}
    }
    
    private void checkOutput(String hoho) {
	if (hoho == null) {
	TSRMUtil.startUpInfo(hoho+" is invalid. Not able to read outputs.");
	System.exit(1);
	}
	int pos = hoho.indexOf("OUTPUT:");
	if ((pos <0) && (hoho.startsWith("ERROR>"))) {		    
	    TSRMUtil.startUpInfo("Command given ["+_checkSizeCommand+"] is invalid.");
		TSRMUtil.startUpInfo("reference:"+hoho);
	    System.exit(1);
	}
    }

    private void testCommandAndPath(String  commandStr) {
        TSRMLog.info(StaticToken.class, null, "command="+commandStr, "localpath="+_localPath+" unit="+_unit);
	if (commandStr == null) {
	    return;
	}

	if (_localPath == null) {
	    String hoho = TPlatformUtil.execShellCmdWithOutput(commandStr, true);
	    checkOutput(hoho);
	    return;
	}

	if (!_localPath.exists()) {
	    TSRMUtil.startUpInfo("This path:"+_localPath.getPath()+" is invalid.");
	    System.exit(1);
	}

	try {
	    String hoho = TPlatformUtil.execShellCmdWithOutput(commandStr+" "+_localPath, true);
	    checkOutput(hoho);
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("Command given["+commandStr+"] is not valid.");
	    System.exit(1);
	}
    }

  public static String[] getNames(StaticToken[] list) {
	if (list == null) {
	    return null;
	}
	String[] result = new String[list.length];
	for (int i=0; i<list.length; i++) {
	    result[i] = list[i].getID();
	}
	return result;
  }

  public static StaticToken find(StaticToken[] list, String inputName) {
	if (list == null) {
	    return null;
	}
	for (int i=0; i<list.length; i++) {
	    if (list[i].getID().equals(inputName)) {
		return list[i];
	    }
	}
	return null;
  }

    public static String[] findByDesc(StaticToken[] list, String desc) {
	if (list == null) {
	    return null;
	}
	if (desc == null) {
	    return null;
	}

	java.util.Vector result = new java.util.Vector(list.length);
	for (int i=0; i<list.length; i++) {
	    if (desc.equals(list[i].getDesc())) {
		result.add(list[i].getID());
	    }
	}
	if (result.size() == 0) {
	    return null;
	}
	String[] names = new String[result.size()];
	for (int i=0; i<result.size(); i++) {
	    names[i] = (String)(result.get(i));
	}
	return names;
    }
}
