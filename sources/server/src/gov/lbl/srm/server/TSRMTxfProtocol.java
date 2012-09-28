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

import java.io.File;
import java.util.*;
import org.apache.axis.types.URI;
import gov.lbl.srm.util.*;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.StorageResourceManager.*;

public final class TSRMTxfProtocol { 
    private boolean _isEnabled = false;
    private String _name;
	private boolean _doGetNextWithHint = true;

    private TSRMTxfProtocol(String name) {
	_name = name; 
	_policy = new TBasicRoundRobin();	    
    } 

	private TSRMTxfProtocol(String[] desc) {
		_name = desc[0];
		if (desc.length == 3) {
			_toReplace = desc[1];
			_replacement = desc[2];
			String sfn=_toReplace;
			if (sfn.endsWith("/")) {
				sfn += "sample/path";
			} else {
				sfn += "/sample/path";
			}
			TSRMLog.debug(this.getClass(), null, "evt=setTxfInfo reg="+_toReplace+" val="+_replacement, "b4="+sfn+" after="+sfn.replaceFirst(_toReplace, _replacement));
			System.out.println("Effect of your sfn replacement:"+sfn+"=>"+sfn.replaceFirst(_toReplace, _replacement));
		}
		_policy = new TBasicRoundRobin();
	}

    String[] _hostPortList = null;
    //int _count = -1;
    ISRMSelectionPolicy _policy = null;

	String _toReplace = null;
	String _replacement=null;

    public String toString() {return _name; }

    public static final TSRMTxfProtocol GSIFTP = new TSRMTxfProtocol("gsiftp");
    public static final TSRMTxfProtocol FTP = new TSRMTxfProtocol("ftp");
    public static final TSRMTxfProtocol HTTP = new TSRMTxfProtocol("http");
    
    public static final TSRMTxfProtocol FILE = new TSRMTxfProtocol("file"); 

	public static TSRMTxfProtocol[] _gUserDefinedProtocols = null;

	private static String[] parseUserProtocol(String curr) {
			String name = curr;
			int pos = curr.indexOf("[");
			if (pos > 0) {
				name = curr.substring(0, pos);
				int end =curr.indexOf("]");
				if (end == -1) {
					throw new TSRMException("Invalid user protocol input. [] not matching.", false);
				}
				String vals = curr.substring(pos+1, end);
				String[] inputs = vals.split(" ");
				if ((inputs == null) || (inputs.length != 2)) {
					throw new TSRMException("Invalid user protocol input in [regEx replacement]", false);
				}
				String[] result = new String[3];
				result[0] = name;
				result[1] = inputs[0];
				result[2] = inputs[1];

				return result;
			} else if (pos == 0) {
				throw new TSRMException("Invalid user protocol input:"+curr, false);
			} else {
				String[] result = new String[1];
				result[0] = name;
				return result;
			}
	}

	public static void setUserDefinedProtocols(String input) {
		if ((input == null) || (input.length() == 0)) {
			return;
		}
		String seperator = ";";
		String[] result =input.split(seperator);
		if (result == null) {
			throw new TSRMException("Cannt parse input for user defined protocols:"+input, false);
		}
		if (result.length == 0) {
			return;
		}
		HashMap m = new HashMap();
		for (int i=0; i<result.length; i++) {
			String curr = result[i];
			String protocolDesc[] = parseUserProtocol(curr);
			if (getProtocol(protocolDesc[0]) != null) {
				continue;
			}
			if (m.get(protocolDesc[0]) == null) {
				TSRMLog.info(TSRMTxfProtocol.class, null, "event=addUserProtocol", "value="+protocolDesc[0]);
				m.put(protocolDesc[0], new TSRMTxfProtocol(protocolDesc));
			}
		}
		_gUserDefinedProtocols = new TSRMTxfProtocol[m.size()];

		Set keys = m.keySet();
    	Iterator iter = keys.iterator();
		int n=0;
    	while (iter.hasNext()) {
        	String curr = (String)(iter.next());
			_gUserDefinedProtocols[n] = (TSRMTxfProtocol)(m.get(curr));
			n++;
		}
	}
    public boolean isEnabled() {
	return _isEnabled;
    }

    public void enable() {
	_isEnabled = true;
    }

    /*
    public String[] getHostPortList() {
	return _hostPortList;
    }
    */

    /*
    public String displayAllServers() {
	if ((_hostPortList == null) || (_hostPortList.length == 0)) {
	    return this.toString()+"://"+Config._host;
	} 
	String result="";
	for (int i=0; i<_hostPortList.length; i++) {
	    if (result.length() > 0) {
		result +=";";
	    }
	    result += this.toString()+"://"+_hostPortList[i];
	}	
	return  result;
    }
    */
    public String[] displayAllServers() {
	if (_policy != null) {
	    String[] result = _policy.displayContents();
	    if (result != null) {
		return result;
	    }
	}

	TSRMLog.debug(this.getClass(), null, "event=displayFromServer", "_hostPortList="+_hostPortList);
	if ((_hostPortList == null) || (_hostPortList.length == 0)) {
	    String[] result = new String[1];
	    result[0] = this.toString()+"://"+Config._host;
	    return result;
	} 

	String[] result = new String[_hostPortList.length]; 
	for (int i=0; i<_hostPortList.length; i++) {
	    result[i]= this.toString()+"://"+_hostPortList[i];
	}	
	return  result;
    }

/*
    public static TSRMTxfProtocol getDefaultUploadProtocol() {
	if (TSRMTxfProtocol.GSIFTP.isEnabled()) {
	    return TSRMTxfProtocol.GSIFTP;
	} else if (TSRMTxfProtocol.FTP.isEnabled()) {
	    return TSRMTxfProtocol.FTP;
	} else if (TSRMTxfProtocol.FILE.isEnabled()) {
	    return TSRMTxfProtocol.FILE;
	} else if (TSRMTxfProtocol.HTTP.isEnabled()) {
	    return TSRMTxfProtocol.HTTP;
	}

	// if user didn't define anything, assum GSIFTP is available:
	return TSRMTxfProtocol.GSIFTP;
    }
*/
    public static void applyUserPolicyList(String userInput) {
	if (userInput == null) {
	    return;
	}

	String[] inputList=userInput.split(";");
	if (inputList == null) {
	    throw new RuntimeException("Cann't parse this protocol policy:"+userInput);
	}

	for (int i=0; i<inputList.length; i++) {
	    String curr = inputList[i];
	    applyUserPolicy(curr, inputList.length);
	}
    }

    public static void applyUserPolicy(String policyStr, int total) {
	String classN = TSRMUtil.getValueOf(policyStr, "class=", '&', false);
	String jarFileN = TSRMUtil.getValueOf(policyStr, "jarFile=", '&', false);
	String protocolN = TSRMUtil.getValueOf(policyStr, "name=", '&', true);
	String parameter=TSRMUtil.getValueOf(policyStr, "param=", '&', true);

	TSRMUtil.startUpInfo("........paramter="+parameter);
	Object[] input = null;
	java.lang.Class[] constructorInput = null;
	if (parameter != null) {
	    if (protocolN == null) {
		throw new RuntimeException("Cann't apply class "+classN+" to all protocols.");
	    }
	    input = new String[1];
	    input[0] = parameter;
	    constructorInput = new java.lang.Class[1];
	    constructorInput[0] = String.class;
	}

	if ((classN == null) || (jarFileN == null)) {
	    // cannt use this user input policy
	    throw new RuntimeException("Cann't apply this policy: "+policyStr);
	}

	ISRMSelectionPolicy userPolicy = null;
	try {
	    Class userPolicyClass = TSRMUtil.loadClass(Config._pluginDir+"/"+jarFileN, classN);
	    //java.lang.reflect.Constructor constructor = userPolicyClass.getConstructor((java.lang.Class [])null);
	    java.lang.reflect.Constructor constructor = userPolicyClass.getConstructor(constructorInput);
	    //userPolicy = (ISRMSelectionPolicy) (constructor.newInstance((java.lang.Object [])null));
	    userPolicy = (ISRMSelectionPolicy) (constructor.newInstance(input));

	    if (protocolN == null) {
		if (total != 1) {
		    throw new RuntimeException("Cann't apply policy:"+policyStr+" with others");
		}

		TSRMTxfProtocol.GSIFTP.applyPolicy(userPolicy);
		//TSRMTxfProtocol.FILE.applyPolicy((ISRMSelectionPolicy) (constructor.newInstance((java.lang.Object [])null)));
		TSRMTxfProtocol.HTTP.applyPolicy((ISRMSelectionPolicy) (constructor.newInstance(input)));
		TSRMTxfProtocol.FTP.applyPolicy ((ISRMSelectionPolicy) (constructor.newInstance(input)));
		return;
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMUtil.startUpInfo("Error loading user policy."+e.toString());
	    System.exit(1);
	}
	// now apply this policy on protcol, if it is enabled
	
	TSRMTxfProtocol p = getProtocol(protocolN);
	if (p == null) {
	    throw new RuntimeException("unrecognized protocol ["+protocolN+"]");
	}
	if (p == TSRMTxfProtocol.FILE) {
	    throw new RuntimeException("Cannt apply policy on file:/ protocol");
	}

	if (p.isEnabled()) {	    
	    p.applyPolicy(userPolicy);
	}
    }

    public void applyPolicy(ISRMSelectionPolicy p) {	
	if (p != null) {
	    TSRMUtil.startUpInfo("....................protocol name="+_name+" will apply policy: "+p);
	    _policy = p;	
	 
	    if (_hostPortList == null) {
		_hostPortList = new String[1];
		_hostPortList[0] = Config._host;
	    }
	    _policy.setItems(_hostPortList);

		try {
		Class[] parameterTypes = new Class[1];
        parameterTypes[0] = java.lang.Object.class;
        _policy.getClass().getDeclaredMethod("getNext", parameterTypes);
		System.out.println(".................... getNext(path) will be called to get hostport.");
		} catch (Exception e) {
			_doGetNextWithHint =false;
			System.out.println(".................... getNext() will be called to get hostport.");
		}
	}
    }  

    public static void checkProtocolWithCollection(String curr, HashMap sorted) {
	String token="://";
	int pos = curr.indexOf(token);
	if (pos <= 0) {
	    throw new RuntimeException("Cann't recognize this value: "+curr+", need machine address");
	} else {
	    String protocol = curr.substring(0, pos);
	    String hostPort = curr.substring(pos+3);		
	    while (hostPort.endsWith("/")) {
		hostPort = hostPort.substring(0, hostPort.length()-1);
	    }
	    
	    TSRMTxfProtocol p = TSRMTxfProtocol.getProtocol(protocol);
	    
		if (p != null) {
	    	p.enable();
		}
	    if ((p != null) && (p != TSRMTxfProtocol.FILE)) {
		if (!sorted.containsKey(p)) {
		    Vector v = new Vector();
		    v.add(hostPort);
		    sorted.put(p, v);
		} else {
		    Vector v = (Vector)(sorted.get(p));
		    if (!v.contains(hostPort)) {
			v.add(hostPort);
		    }
		}
	    } 	
	}
	
    }	        

   
    public static void parse(String userInput) {	
      TSRMLog.info(TSRMTxfProtocol.class, null, "event=parse", "value="+userInput);
	if (userInput == null) {
	    TSRMTxfProtocol.GSIFTP.enable(); // by default, assume gsiftp is enabled.
	    return;
	}
	String[] inputList = userInput.split(";");
	if (inputList == null) {
	    throw new RuntimeException("Cann't parse this protocol input:"+userInput);
	}

	HashMap sorted = new HashMap();
	for (int i=0; i<inputList.length; i++) {
	    String curr = inputList[i];
	    checkProtocolWithCollection(curr, sorted);	    	   
	}

	TSRMTxfProtocol.GSIFTP.setHostPortList((Vector)(sorted.get(TSRMTxfProtocol.GSIFTP)));
	TSRMTxfProtocol.FTP.setHostPortList((Vector)(sorted.get(TSRMTxfProtocol.FTP)));
	TSRMTxfProtocol.HTTP.setHostPortList((Vector)(sorted.get(TSRMTxfProtocol.HTTP)));

	if (_gUserDefinedProtocols != null) {
        for (int i=0; i<_gUserDefinedProtocols.length; i++) {
            TSRMTxfProtocol p = _gUserDefinedProtocols[i];
			p.setHostPortList((Vector)(sorted.get(p)));
	}
    }
	TSRMLog.info(TSRMTxfProtocol.class, null, "event=defaultProtocol", "value="+getDefaultTransferProtocol());
	}
    
	public static TSRMTxfProtocol getDefaultTransferProtocol() {
		if (TSRMTxfProtocol.GSIFTP.isEnabled()) {
			return TSRMTxfProtocol.GSIFTP;
		}
		if (TSRMTxfProtocol.FTP.isEnabled()) {
			return TSRMTxfProtocol.FTP;
		}
		if (TSRMTxfProtocol.HTTP.isEnabled()) {
			return TSRMTxfProtocol.HTTP;
		}
		if (TSRMTxfProtocol.FILE.isEnabled()) {
			return TSRMTxfProtocol.FILE;
		}
		if (_gUserDefinedProtocols != null) {
			for (int i=0; i<_gUserDefinedProtocols.length; i++) {
           		TSRMTxfProtocol p = _gUserDefinedProtocols[i];
				if (p.isEnabled()) {
            		return p;
				}
			}
 		}           
		// if nothing is define, assuming gsiftp
		return TSRMTxfProtocol.GSIFTP;
	}

    public void setHostPortList(Vector p) {
	if (p == null) {
	    return;
	}

	Object[] objs = p.toArray();
	_hostPortList = new String[objs.length];
		
	for (int i=0; i<objs.length; i++) {
	    _hostPortList[i] = (String)(objs[i]);
	    TSRMUtil.startUpInfo("==> input: "+toString()+" "+i+"th: "+_hostPortList[i]);
	}

	_policy.setItems(_hostPortList);
    }

    protected static TSRMTxfProtocol getProtocol(String name) {
	if (name.equalsIgnoreCase(TSRMTxfProtocol.GSIFTP.toString())) {
	    return TSRMTxfProtocol.GSIFTP;
	} else if (name.equalsIgnoreCase(TSRMTxfProtocol.FTP.toString())) {
	    return TSRMTxfProtocol.FTP;
	} else if (name.equalsIgnoreCase(TSRMTxfProtocol.HTTP.toString())) {
	    return TSRMTxfProtocol.HTTP;
	} else if (name.equalsIgnoreCase(TSRMTxfProtocol.FILE.toString())) {
	    return TSRMTxfProtocol.FILE;
	}
	if (_gUserDefinedProtocols != null) {
	    for (int i=0; i<_gUserDefinedProtocols.length; i++) {
		TSRMTxfProtocol p = _gUserDefinedProtocols[i];
		if (name.equalsIgnoreCase(p.toString())) {
		    return p;
		}
	    }
	}
	return null;
    }

    public static TSupportedTransferProtocol[] getSupportedProtocols() {
	Vector p = new Vector();
	
	if (TSRMTxfProtocol.GSIFTP.isEnabled()) {
	    p.add(TSRMTxfProtocol.GSIFTP);
	} else if (TSRMTxfProtocol.FTP.isEnabled()) {
	    p.add(TSRMTxfProtocol.FTP);
	} else if (TSRMTxfProtocol.HTTP.isEnabled()) {
	    p.add(TSRMTxfProtocol.HTTP);
	} else if (TSRMTxfProtocol.FILE.isEnabled()) {
	    p.add(TSRMTxfProtocol.FILE);
	}

	if (_gUserDefinedProtocols != null) {
		for (int i=0; i<_gUserDefinedProtocols.length; i++) {
			TSRMTxfProtocol tmp = _gUserDefinedProtocols[i];
			p.add(tmp);
		}
	}

	TSupportedTransferProtocol[] supportedArray = new TSupportedTransferProtocol[p.size()];
	for (int i=0; i<supportedArray.length; i++) {
	    TSupportedTransferProtocol temp = new TSupportedTransferProtocol();
	    temp.setTransferProtocol(((TSRMTxfProtocol)(p.get(i))).toString());
	    supportedArray[i] = temp;
	}	    
	return supportedArray;
    }

    /*
    private String getHostPort() {
	if (_hostPortList == null) {
	    return Config._host;
	}
	if (_hostPortList.length == 1) {
	    return _hostPortList[0];
	}
	_count = _count +1;
	_count = _count % (_hostPortList.length);

	return _hostPortList[_count];	
    }
    */
    private String getHostPort(String path) {
	if (_hostPortList == null) {	 
	    return Config._host;
	}

	String v = null;
	if (_doGetNextWithHint) {
	 	v = (String)(_policy.getNext(path));
	} else {
		v = (String)(_policy.getNext());
	}
	if (v == null) {
	    throw new TSRMException("Null values from user policy.", false);
	}
	return v;
    }
    

    public URI generateURI(File f) {
	return generateURI(f.getPath());
    }

    public URI generateURI(String path) {
		if ((_toReplace != null) && (_replacement != null)) {
			path = path.replaceFirst(_toReplace, _replacement);
		}
	try {
	    String uriStr = this.toString()+"://";
	    if (this != TSRMTxfProtocol.FILE) {
		uriStr += getHostPort(path);
		
	    }
	    
	    if (!path.startsWith("/")) {
		uriStr += "/";
	    }

	    uriStr += "/"+path;	    
	    
	    URI uri = new URI(uriStr);
	    
	    return uri;
	} catch (URI.MalformedURIException e) {	    
	    e.printStackTrace();
	    TSRMLog.exception(TSRMTxfProtocol.class, "details!", e);	    
	    return null;
	} catch (Exception e) {
	    e.printStackTrace();
	    TSRMLog.exception(TSRMTxfProtocol.class, "details", e);
	    return null;
	}
    }
}

class TBasicRoundRobin implements ISRMSelectionPolicy {
    int _count = -1;
    Object[] _itemArray = null;

	public Object getNext(Object hint) {
			return	getNext();
	}

    public Object getNext() {
	Object result = null;
	if ((_count >= 0) && (_itemArray != null)) {
	    result = _itemArray[_count];
	    _count  = _count +1;
	    _count = _count % (_itemArray.length);
	}
	return result;
    }

    public void setItems(Object[] col) {
	_itemArray = col;
	_count = 0;
    }

    public String[] displayContents() {
	return null;
    }
}

