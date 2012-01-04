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

import gov.lbl.srm.util.*;
import java.util.*;

public class TSRMUserSpaceTokenSupport {
    static HashMap _userTokenList = null;
    static String _DefSeperator = "\\("; // we know dirnames cannt contain this.
    static String _DefClosure = ")";
    static String _DefEqual = "=";
    
    // input=(key1=path1)...(keyN=pathN)
    public static boolean parse(String input) {
	String[] entries = input.split(_DefSeperator);
	if (entries == null) {
	    TSRMUtil.startUpInfo("Expected format is: (key1=path1)...(keyN=pathN). Your input is:"+input);
	    return false;
	}
	for (int i=0; i<entries.length; i++) {
	    String curr = entries[i];
	    if (curr.length() == 0) {
		continue;
	    } 
	    if (!curr.endsWith(_DefClosure)) {
		TSRMUtil.startUpInfo("Expected format is: (keyN=pathN). Missing the ) at the end. User gave:"+curr);
		return false;
	    }
	    int pos = curr.indexOf(_DefEqual);
	    if (pos == -1) {
		TSRMUtil.startUpInfo("Expected format is: (keyN=pathN). Missing the = in the middle. User gave:"+curr);
		return false;
	    }
	    String key = curr.substring(0, pos);
	    String path = curr.substring(pos+1, curr.length()-1);
	    add(key, path);
	}
	return true;
    }

    public static void add(String key, String path) {
	TSRMUtil.startUpInfo(".....user defines ["+key+"] to path:"+path);
	TSRMLog.debug(TSRMUserSpaceTokenSupport.class, null, "userDefinedkey="+key, "value="+path);
	//java.io.File input = new java.io.File(path);
	java.io.File input = TSRMUtil.initFile(path);
	if (!input.exists()) {
	    TSRMUtil.startUpInfo("                      [WARNING] path:"+path+" does _not_ exist.");
	}
	if (_userTokenList == null) {
	    _userTokenList = new HashMap();
	}

	_userTokenList.put(key, path);
    }

    public static TSupportedURLWithFILE expandURL(TSURLInfo info0, String path) {
	if (_userTokenList == null) {
	    //return null;
	    return new TSupportedURLWithFILE(info0, path);
	}
	while (path.startsWith("/")) {
	    path = path.substring(1);
	}
	Iterator iter = _userTokenList.keySet().iterator();
	while (iter.hasNext()) {
	    String key = (String)(iter.next());
	 
	    if (path.startsWith(key)) {
		path = _userTokenList.get(key)+path.substring(key.length());
		return new TSupportedURLWithFILE(info0, path);
	    } 
	}
	
	if (_userTokenList.get("/") != null) {
		path=_userTokenList.get("/")+"/"+path;
	}
	return new TSupportedURLWithFILE(info0, path);
    }

    public static TSupportedURLWithFILE addPrefix(String key, String diskPath) {
	String tgtPath = (String)(_userTokenList.get(key));
	TSRMLog.debug(TSRMUserSpaceTokenSupport.class, null, "event=addPrefix key="+key, "diskpath="+diskPath);
	if (!diskPath.startsWith("/")) {
	    diskPath = "/"+diskPath;
	}
	
	org.apache.axis.types.URI uri = TSRMTxfProtocol.FILE.generateURI(tgtPath+diskPath);
	String sitePath = null;
	TSURLInfo siteInfo = TSRMUtil.createTSURLInfo(uri);
	return new TSupportedURLWithFILE(siteInfo, sitePath);
    }

    public static boolean matchWithURL(String key, String path) {
	TSRMLog.debug(TSRMUserSpaceTokenSupport.class, null, "event=matchWithURL key="+ key, "path="+path);
	String tgtPath = (String)(_userTokenList.get(key));
	if (path.startsWith(tgtPath+"/")) {
	    return true;
	} 
	if (path.startsWith(key+"/")) {
	    return true;
	}
	return false;
    }

    public static String displayAll() {
	if (_userTokenList == null) {
	    return "None.";
	}
	String result="";
	Iterator iter = _userTokenList.keySet().iterator();
	while (iter.hasNext()) {
	    String key = (String)(iter.next());
	    String desc= (String)(_userTokenList.get(key));
	    result+=key+"("+desc+") ";
	}
	return result;
	    
    }

    public static String[] match(String desc) {
	TSRMLog.debug(TSRMUserSpaceTokenSupport.class, null, "action=getTokensMatching desc=\""+desc+"\"", null);
	if (_userTokenList == null) {
	    return null;
	}

	if (desc == null) {
	    String[] result = new String[_userTokenList.size()];
	    Iterator iter = _userTokenList.keySet().iterator();
	    int i=0;
	    while (iter.hasNext()) {
		String key = (String)(iter.next());
		result[i]=key;
		i++;
	    }
	    return result;
	}

	 Iterator iter = _userTokenList.keySet().iterator();
	 while (iter.hasNext()) {
	     String key = (String)(iter.next());
	     if (key.equalsIgnoreCase(desc)) {
		 String[] result = new String[1];
		 result[0] = key;
		 return result;
	     }
	 }
	 return null;
    }
}
