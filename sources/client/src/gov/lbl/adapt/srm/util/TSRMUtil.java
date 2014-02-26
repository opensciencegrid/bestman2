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

package gov.lbl.adapt.srm.util;

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.InetAddress;
import java.net.NetworkInterface;

import org.apache.axis.types.*;
import org.globus.util.ConfigUtil;

public class TSRMUtil {
   public static final String _DefMsgInvalidCredential 		= "Credential is empty or invalid.";
   public static final String _DefMsgCreateAccountFailed 	= "Account is not created in SRM.";
   public static final String _DefBadInput 		       	= "Bad input format received.";
   public static final String _DefInternalError 		= "Internal Error.";
   public static final String _DefSystemBusy			= "System is currently full.";
   public static final String _DefNoSuchUser			= "No such user.";
   public static final String _DefNoSuchRequestToken		= "No such request for the user.";
   public static final String _DefNoSuchFileRequestToken	= "No such file request.";
   public static final String _DefNoSuchFileRequest		= "No such file request with the given surl";
   public static final String _DefNoSuchSpaceToken		= "No such space token for this user.";
   public static final String _DefInProgress			= "File transfer is in progress.";
   public static final String _DefInvalidPath			= "The path is not recognized by this SRM.";
   public static final String _DefInvalidLocalPath		= "The path is not local to this SRM. Please verify the service address";
   public static final String _DefInvalidProtocol		= "Protocol used is not supported for the specific SRM function";
   public static final String _DefNoSuchPath			= "No such path exist in this SRM!";
   public static final String _DefCheckDetails			= "Details shall be in the related outputs";
   public static final String _DefEmptyInputs			= "Empty input values";
   public static final String _DefNullToken			= "NULL token";
   public static final String _DefFileTypeMismatch		= "FileType is not as requested.";
   public static final String _DefNoOverwrite                   = "file already exists, and no overwrite is allowed from the request.";
   public static final String _DefConflictStorageType           = "file already exists, and the provided fileStorageType does not match with the existing one. Cannt overwrite.";

   public static final TSRMMutex _tokenCounterMutex                 = new TSRMMutex();
   protected static int _numTokensIssued                           = 0; 
   public static final Random _randomGenerator                        = new Random(System.currentTimeMillis());	

   public static final long   MAX_MSG_SIZE                  = 104857600; // 100MB

   public static String generatePathStr(String tokenID, String filename) {
       return tokenID+"-"+TSRMUtil.getNextRandomUnsignedInt()+"/"+filename;
   }

   public static int getNextRandomUnsignedInt() {
       int out = _randomGenerator.nextInt();
       if (out < 0) {
	   out = 0 - out;
       } 
       return out;
   }


    public static boolean acquireSync(TSRMMutex m) {
    	try {
	    m.acquire();
	    return true;
    	} catch (RuntimeException e) {
	    e.printStackTrace();
	    return false;
    	}
    }

    public static boolean acquireSync(java.util.concurrent.Semaphore m) {
    	try {
	    m.acquire();
	    return true;
    	} catch (InterruptedException e) {
	    e.printStackTrace();
	    return false;
    	}
    }
    
    public static boolean releaseSync(TSRMMutex m) {
    	m.release();
    	return true;
    }

    public static boolean releaseSync(java.util.concurrent.Semaphore m) {
    	m.release();
    	return true;
    }

    public static long parseTimeStr(String timeStamp) {
	timeStamp = timeStamp.trim();
	int pos = timeStamp.indexOf(":");
	try {
	    if (pos > 0) {
		Calendar curr = Calendar.getInstance();
		int currYear = curr.get(Calendar.YEAR);
		timeStamp +=" "+currYear;

		SimpleDateFormat formatter = new SimpleDateFormat("MMM d HH:mm yyyy");
		Date d = (java.util.Date)formatter.parse(timeStamp);
		return d.getTime();
	    } else {
		SimpleDateFormat formatter = new SimpleDateFormat("MMM d yyyy");
		Date d = (java.util.Date)formatter.parse(timeStamp);
		return d.getTime();
	    }	    
	} catch (Exception e) {
	    //TSRMLog.exception(TSRMUtil.class, "parseTimeStr()"+timeStamp, e);
	    //e.printStackTrace();
	    
	    return -1;
	}
    }
	

    public static String generateRandomString() {
    	Random r = new Random();
    	/*
    	 byte[] bytes = new byte[100];
    	 r.nextBytes(bytes);
    	 return bytes.toString();
    	 */
    	return String.valueOf(r.nextLong());
    }

    public static String getTokenID() {	
	if (TSRMUtil.acquireSync(_tokenCounterMutex)) {
	    _numTokensIssued++;
	    TSRMUtil.releaseSync(_tokenCounterMutex);
	    return String.valueOf(_numTokensIssued-1);
	}
	return generateRandomString();
    }
    
	public static void sync(Object[] tidSet) {
		int max = 0;
        for (int i=0; i<tidSet.length; i++) {
        String curr = ((String)(tidSet[i])).substring(2);
        if (!curr.startsWith("0")) {
            int temp = Integer.parseInt(curr);
            if (temp+1 > max) {
            max = temp+1;
            }
        }
        }
		sync(max);
	}

    private static void sync(int expectedCounter) {
	//TSRMLog.info(TSRMUtil.class, null, "event=sync", "_numTokens="+_numTokensIssued+" expected="+expectedCounter);
	if (_numTokensIssued < expectedCounter) {
	    _numTokensIssued = expectedCounter;
	}
    }



    public static String getAbsPath(String str) {
	return str;
    }

    public static String getAbsPath(java.io.File f) {
	if (f == null) {
	    return null;
	}
	return f.getPath(); // full surl is: TSRMTxfProtocol.FILE.generateURI(f).toString()
    }

    public static String getAbsPath(URI uri) {
	if (uri == null) {
	    return null;
	}

	int pos = uri.toString().indexOf("?SFN=");
	if (pos == -1) {
	    if (uri.getScheme().equalsIgnoreCase("file")) {
		return uri.toString().substring(5);
	    }
	    return uri.getPath();
	}
	return uri.toString().substring(pos+5);
    }


    
    public static org.apache.axis.types.UnsignedLong createTSizeInBytes(long val) {
	if (val >= 0) {
	    UnsignedLong size = new UnsignedLong(val);
	    return size;
	} else {
	    return null;
	}
    }

  // example 2006-06-27 11:33
  public static Calendar createGMTTime(String longisoDate, String longisoTime) {
        try {
	  Calendar result = Calendar.getInstance();
	  
	  SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	  Date d = (java.util.Date)formatter.parse(longisoDate+" "+longisoTime);
	  return createGMTTime(d.getTime());
	} catch (Exception e) {
	    //TSRMLog.exception(TSRMUtil.class, "parsingLongIso:"+longisoDate+" "+longisoTime, e);
	  return createGMTTime(-1);
	}
  }
    
    public static Calendar createGMTTime(long t) {
	Calendar c = Calendar.getInstance();
	c.setTimeInMillis(t);
	return c;
    }

   
    public static Integer createTLifeTimeInSeconds(long t, boolean refuseNegativeValue) {
	if (refuseNegativeValue && (t < 0)) {
	    return null;
	}

    	Integer result = new Integer((int)t);
    	return result;
    }
    

    public static URI createTSURL(String defaultProtocol, String surl) {
	try {
	    return new URI(surl);
	} catch (URI.MalformedURIException e) {
	    return createTSURL(defaultProtocol+"://"+surl);
	}
    }

    public static URI createTSURL(String surl) {
	try {
	    return new URI(surl);
	} catch (URI.MalformedURIException e) {
	    e.printStackTrace();
	    return null;
	}
    }


    public static String createPath(String curr, String subpath) {
	// we assume subpath is one level below curr, so only need the endfilename

	int endPos = subpath.length();
	if (endPos >=2) {
	    endPos = endPos -2;
	}
	int lastSlashPos =subpath.lastIndexOf("/", endPos);
	
	if (lastSlashPos > 0) {
	    subpath = subpath.substring(lastSlashPos+1);
	} //else if (lastSlashPos == -1) {
	    //subpath = "/"+subpath;
	//}

	int pos = curr.indexOf("?");
	if (pos == -1) {
	    //subpath = curr+subpath;
	    return attachPath(curr, subpath);
	} else {
	    int sfnPos = curr.indexOf("SFN=");
	    if (sfnPos == -1) {
		//subpath = curr.substring(0, pos)+subpath+curr.substring(pos);
		return attachPath(curr.substring(0, pos), subpath+curr.substring(pos));
	    } else {
		if ((curr.charAt(sfnPos-1) != '?') && (curr.charAt(sfnPos-1) != '&')) {
		    //subpath=curr.substring(0, pos)+subpath+curr.substring(pos);
		    return attachPath(curr.substring(0, pos), subpath+curr.substring(pos));
		} else {
		    int sfnEnds = curr.indexOf("&", sfnPos+1);
		    if (sfnEnds == -1) {
			//subpath = curr+subpath;
			return attachPath(curr, subpath);
		    } else {
			//subpath = curr.substring(0, sfnEnds)+subpath+curr.substring(sfnEnds);
			return attachPath(curr.substring(0, sfnEnds), subpath+curr.substring(sfnEnds));
		    }
		}
	    }
	}

	//return subpath;
    }

    public static String attachPath(String orig, String subpath) {
	if (orig.endsWith("/")) {
	    return orig+subpath;
	} else {
	    return orig+"/"+subpath;
	}
    }

    public static String getEndFileName(String str) {	
	if (str.endsWith("/")) {
	    str = str.substring(0, str.length()-1);
	} 
	int idx = str.lastIndexOf("/");	
	
	// idx != -1, otherwise, malformed URL exception would have been thrown long ago from class URI
	return str.substring(idx+1);
    }
    /*
    public static boolean isRefToSRM(String surl, URI srmURI, URI incomingUrl) {
	boolean noServicePathCheck = true;
	int posSFNField = incomingUrl.toString().indexOf("?SFN=");	
	if (posSFNField > 0) {
	    noServicePathCheck = false;
	}
	//TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" srmURI="+srmURI.toString(), "incoming="+incomingUrl+" noServicePathCheckFlag="+noServicePathCheck);
	//TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" srmURI="+srmURI.toString(), "incoming="+incomingUrl);
	try {
	    URI input = new URI(surl);
	    if (!input.getHost().equalsIgnoreCase(srmURI.getHost())) {
		String srmServerIP = java.net.InetAddress.getByName(srmURI.getHost()).getHostAddress();
		String inputIP = java.net.InetAddress.getByName(input.getHost()).getHostAddress();
		if (!srmServerIP.equalsIgnoreCase(inputIP)) {
		    //TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" ip="+inputIP, "serverIP="+srmServerIP);
		    return false;
		}
	    }
	    if (input.getPort() != srmURI.getPort()) {
		//TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" inputPort="+input.getPort(), "serverPort="+srmURI.getPort());
		return false;
	    }
	    if (noServicePathCheck) {
		return true;
	    }
	    if (input.getPath().equalsIgnoreCase(srmURI.getPath())) {
		return true;
	    }
	    TSRMLog.debug(TSRMUtil.class, null, "event=isRefToSRM surl=\""+surl+"\" path="+input.getPath(), "serverPath="+srmURI.getPath());
	} catch (Exception e) {
	    e.printStackTrace();
	    return false;
	}

	return false;
    }
    */

    //
    // the expected scheme to discover whether an surl refers to local is
    // through the discovery methods that's described in SRM.v2 spec
    // i.e. go through srm/srm.endpoint etc.
    //
    public static boolean isRefToSRM2(String surl, URI srmURI, String incomingUrl) {    	
	boolean noServicePathCheck = true;
	int posSFNField = incomingUrl.indexOf("?SFN=");	
	if (posSFNField > 0) {
	    noServicePathCheck = false;
	}
	
	try {
	    String hostPort = java.net.InetAddress.getByName(srmURI.getHost()).getHostAddress()+":"+srmURI.getPort();	    
	    int where = surl.indexOf(hostPort);
	    if (where > 0) {
		// returns true so lcg tests with srm://address/filepath can pass
		if (noServicePathCheck) {
		    return true;
		} else {
		
		    if (surl.equals(srmURI.toString())) {
			return true;
		    } else {
			return false; 
		    }
		}
	    } 
	    
	    hostPort = java.net.InetAddress.getByName(srmURI.getHost()).getHostName()+":"+srmURI.getPort();
	    where = surl.indexOf(hostPort);
	    if (where > 0) {
		// returns true so lcg tests with srm://address/filepath can pass
		if (noServicePathCheck) {
		    return true;
		} else {
		
		    String curr = srmURI.getScheme()+"://"+hostPort+srmURI.getPath();
		    if (surl.equals(curr)) {
			return true;
		    } else {
			return false;
		    }
		}
	    }
	} catch (java.net.UnknownHostException e) {
	    return false;
	}

	// check to see if hostname is a masked name
	//	String serverIp = java.net.InetAddress.getByName(srmURI.getHost());
	//String inputIp = java.net.InetAddress.getByName(surl);

    	return false; 
    }

    public static String getPassword(String prompt) {
        MaskingThread et = new MaskingThread(prompt);
        Thread mask = new Thread(et);
        mask.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String password = "";

        try {
           password = in.readLine();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }

        et.stopMasking();
        // return the password entered by the user
        return password;
    }

    public static void sleepAlert(long napInMilliseconds) {
	try {
	    Thread.sleep(napInMilliseconds);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    public static void sleep(long napInMilliseconds) {
	try {
	    Thread.sleep(napInMilliseconds);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

     public static boolean getBooleanValueOf(String spec, String tokenName, char seperator) {
	String strValue = getValueOf(spec, tokenName, seperator, true);
	if (strValue == null) {
	    return false;
	}
	
	if (strValue.equalsIgnoreCase("true")) {
	    return true;
	}

	return false;
    }

    public static String getValueOf(String spec, String tokenName, char seperator, boolean allowsNullValue) {
	int posStarts = spec.indexOf(tokenName);
	if (posStarts >= 0) {
	    posStarts = posStarts + tokenName.length();
	    int posEnds = spec.indexOf(seperator, posStarts);
	    if (posEnds > 0) {
		return spec.substring(posStarts, posEnds).trim();
	    } else {
		return spec.substring(posStarts).trim();
	    }
	} else {
	    if (allowsNullValue) {
		return null;
	    }  
	    throw new RuntimeException("null value detected for "+tokenName+" in "+spec); 
	}
    }

}

  /**
   * This class attempts to erase characters echoed to the console.
   */

class MaskingThread extends Thread {
     private volatile boolean stop;
     private char echochar = '*';

    /**
     *@param prompt The prompt displayed to the user
     */
     public MaskingThread(String prompt) {
        System.out.print(prompt);
     }

    /**
     * Begin masking until asked to stop.
     */
     public void run() {

        int priority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try {
           stop = true;
           while(stop) {
             System.out.print("\010" + echochar);
             try {
                // attempt masking at this rate
                Thread.currentThread().sleep(1);
             }catch (InterruptedException iex) {
                Thread.currentThread().interrupt();
                return;
             }
           }
        } finally { // restore the original priority
           Thread.currentThread().setPriority(priority);
        }
     }

    /**
     * Instruct the thread to stop masking.
     */
     public void stopMasking() {
        this.stop = false;
     }
    
     public static void assertNull(Object obj, String msg) {
	if (obj == null) {
	    //throw new TSRMException("Unexpected error, "+msg, false);
	}
     }
    
  }
