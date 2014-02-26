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

package gov.lbl.adapt.atm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import java.util.*;
import java.text.SimpleDateFormat;
 
//import com.google.gson.*;
//import com.google.gson.reflect.*;
import org.json.simple.*;
import org.json.simple.parser.*;

public class TATMImpl implements IATM{
   // String _baseUrl = "http://localhost:8080/policy-adaptws-0.0.3";
    static String _baseUrl = "http://localhost:8181";
    //static String _baseUrl="http://calculon.isi.edu:8080";
    public static final SimpleDateFormat _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z' ");

    String _id = null;
    PolicyTxf _initTxf = null;

    public TATMImpl(String baseUrl) {
	_baseUrl = baseUrl;
    }

    private String assertHostName(String url) {
	if (url.startsWith("file:/")) {
    	    try {
		java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
		String host = addr.getCanonicalHostName();
		
		return "gsiftp://"+host+url.substring(6);
	    } catch (java.net.UnknownHostException e) {
		e.printStackTrace();
		return url;
	    }
	}
	return url;
    }
    
    private String assertDomainName(String url) {
	if (url.startsWith("file:/")) {
	    try {
		java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
		String host = addr.getCanonicalHostName();
		
		int dot = host.indexOf(".");
		if (dot > 0) {
		    return "gsiftp://"+host.substring(dot+1)+url.substring(6);
		} else {
		    return "gsiftp://"+host+url.substring(6);
		}
	    } catch (java.net.UnknownHostException e) {
		e.printStackTrace();
		return url;
	    }
	}
	return url;
    }
    
    
    public void fyi(int streamsCurrentlyUsing) {
	if (_baseUrl == null) {
	    return;
	}

	clientPrints(" fyi: "+streamsCurrentlyUsing);

	if (_initTxf == null) {
	    return;
	}

	if (!_initTxf.setRealtimeStreams(streamsCurrentlyUsing)) {
	   return;
	}

        // notify policy of these chosen values
        try {
            PolicyTxf result = doPut(_baseUrl+"/transfer/"+_id, _initTxf);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static boolean testJsonSimple() {
	String src="http://i.am.source/path";
	String tgt="http://it.is.target/path";

	try {
	    PolicyTxf curr = new PolicyTxf(src, tgt, 111,222);
	    curr.setId("abc");

	    System.out.println("obj="+curr);

	    String out = curr.toJson();
	    System.out.println("out    ="+out);

	    PolicyTxf backTo = PolicyTxf.objFromJson(out);	    
	    System.out.println("back to="+backTo);

	    PolicyTxf next = new PolicyTxf(src, tgt, 333, 444);
	    next.setId("efg");
	    String outNext = next.toJson();


	    System.out.println("\n ==== now for lists ");
	    System.out.println(out);
	    System.out.println(outNext);
	    HashMap m = new HashMap();
	    m.put(curr.getId(), out);
	    m.put(next.getId(), outNext);
	    String twoObjStr = JSONValue.toJSONString(m);
	    System.out.println("!two objs:\n"+twoObjStr);
	    System.out.println();
	    HashMap<String, PolicyTxf> twoObjs = PolicyTxf.collectionFromJson(twoObjStr);
	    System.out.println(twoObjs);
	} catch (Exception hi) {
	    hi.printStackTrace();
	}
	    
	    return true;
    }

    public static void main(String[] args) {
	if (testJsonSimple()) {
	    return;
	}

	//testSet01(args);
	if (args.length == 0) {
	    testSet01(args);	    
	} else {
	    try {
		String op = args[0];
		if (op.equals("create")) {	       
		    testSet02(Integer.parseInt(args[1]));		
		} else if (op.equals("delete")) {
		    for (int i=1; i<args.length; i++) {	
			doDelete(_baseUrl+"/transfer/"+args[i]);
		    }		
		} else if (op.equalsIgnoreCase("deleteAll")) {
		    deleteAll(_baseUrl+"/transfer/");
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    public static void clientPrints(String outStr) {
	long currTime = System.currentTimeMillis();
	String timeStr = _dateFormatter.format(currTime);
	System.out.println(currTime+" "+timeStr+"[client debug] "+outStr);
    }
    //
    // create N txf, and then no delete
    //
    public static void testSet02(int n) {	
	clientPrints(" .. creating: "+n+" txfs and exit");
	try {	    
	    //String pmUrl="http://localhost:8080/policy-adaptws-0.0.3";
	    String pmUrl = _baseUrl;
	    TATMImpl c = new TATMImpl(pmUrl);
	    c.getCurrSnapshot();

	    for (int i=0; i<n; i++) {

		String src="gsiftp://data1.lbl.gov/tmp/abc_"+i;
		String tgt="gsiftp://red-gridftp.unl.edu:///tmp/ok";
		
		// create
		c.getConcurrencySuggestion(src, tgt);
		//clientPrints(" _initTxf="+c._initTxf);		
		c._initTxf = null; // no new one can init
	    }
		
	    c.getCurrSnapshot();
		
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }


    //
    // create one txf, and then delete
    //
    public static void testSet01(String[] args) {	
	try {
	    //String pmUrl="http://localhost:8080/policy-adaptws-0.0.3";
		String pmUrl=_baseUrl;
	    TATMImpl c = new TATMImpl(pmUrl);
	    c.getCurrSnapshot();

	    
	    String src="gsiftp://data1.lbl.gov/tmp/abc";
	    String tgt="gsiftp://red-gridftp.unl.edu:///tmp/ok";
	
	    // create
	    c.getConcurrencySuggestion(src, tgt);
	    clientPrints("_initTxf="+c._initTxf);	    
	    
	    // update
	    c.update(15, 222);
	    clientPrints(" _initTxf:"+c._initTxf);

	    c.getCurrSnapshot();

	    // complete, no need to complete, delete finished ones
	    //c.completed();	    
	    //c.getCurrSnapshot();

	    /*
	    // delete
	    c.delete(c._initTxf.getId());	    	   
	    c.getCurrSnapshot();
	    */
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public static void main2(String[] args) {	
	try {
	    String pmUrl="http://localhost:8080/policy-controller-0.0.3";
	    
	    clientPrints("url="+pmUrl);
	    
	    if (args.length == 0) {
		clientPrints("no input, do list");
		doList(pmUrl+"/transfer");
		return;
	    }
	    
	    String op = args[0];
	    if (op.equalsIgnoreCase("add")) {
		String src="gsiftp://dm.lbl.gov/tmp/abc";
		String tgt="file:///tmp/ok";
		doPost(pmUrl+"/transfer", src, tgt, -1, -1);		      
		
	    } else if (op.equalsIgnoreCase("get")) {
		String id = args[1];
		doGet(pmUrl+"/transfer", id);
	    } else if (op.equalsIgnoreCase("update")) {
		String id = args[1];
		PolicyTxf txf  = new PolicyTxf("gsiftp://dm.lbl.gov//tmp/test", "file:////tmp/tgt", -1, -1);
		doPut(pmUrl+"/transfer/"+id, txf);
	    } else if (op.equalsIgnoreCase("delete")) {
		
	    } else {
		clientPrints("not supported yet: "+op);		  
	    }
	    
	} catch (MalformedURLException e) {
	    e.printStackTrace();
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
    
    public void completed() {
	clientPrints("completed: "+_id);
	if (_id != null) {
	    delete(_id);
	    _id = null;
	}
    }

    public int reclaim(int streams) {
	if (streams <= 0) {
	    return -1;
	}

	if (_baseUrl == null) {
	    return -1;
	}

	clientPrints("reclaiming to: "+streams);
	//_initTxf.getProperties().clear();
	//_initTxf.setProperty(edu.isi.policy.adapt.Constants.ADJUSTED_STREAMS_PROPERTY, Integer.toString(streams));
	//_initTxf.setProperty(Constants.MAX_STREAMS_PROPERTY, Integer.toString(streams));
	_initTxf.setStreams(streams);

	try {
	    //_initTxf = doPut(_baseUrl+"/transfer/"+_id, _initTxf);			    
	    // do not overwrite _initTxf so threshhold stays true after being trimed down by reclaim()
	    /*_initTxf = */doPut(_baseUrl+"/transfer/"+_id, _initTxf);			    
	    int result = getStream(_initTxf);
	    clientPrints ("reclaimed txf to: "+result);
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	}
	return -1;
    }

    public boolean update(int streams, int rate) {
	if (_baseUrl == null) {
	    return true;
	}

	clientPrints("\n attempting to update to server with streams="+streams+" rate="+rate);
	//_initTxf.getProperties().clear();

        //_initTxf.setProperty(edu.isi.policy.adapt.Constants.ADJUSTED_STREAMS_PROPERTY, Integer.toString(streams));
	_initTxf.setStreams(streams);

        // notify policy of these chosen values
        try {
            PolicyTxf result = doPut(_baseUrl+"/transfer/"+_id, _initTxf);
	    if (result != null) {
		_initTxf = result;
		clientPrints("updated to: "+streams);
		return true;
	    } else {
		clientPrints("Unable to update. Server returned null.");
		return false;
	    }
        } catch (Exception e) {
            e.printStackTrace();
	    return false;
        } 
    }

    public void getCurrSnapshot() {
	if (_baseUrl == null) {
	    return;
	}

	try {
	    //doList(_baseUrl+"/transfer/list");
		doList(_baseUrl+"/transfer/"); // the last slash is a must for python server
		doDump(_baseUrl+"/dump");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void delete(String id) {
	if (_baseUrl == null) {
	    return ;
	}

	//RuntimeException hi = new RuntimeException("deleting:"+id);
	//hi.printStackTrace();
	clientPrints("=> deleting txf: streams="+_initTxf.getStreams());		  
	try {
	    doDelete(_baseUrl+"/transfer/"+id);
	    getCurrSnapshot();
	    _initTxf = null;
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    private static HttpURLConnection getConn(String pmUrl) {
	try {
	    URL url = new URL(pmUrl);
	    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	    clientPrints("\n........ connection to: "+pmUrl);
	    return conn;
	} catch (Exception e) {
	    e.printStackTrace();
	    throw new RuntimeException("No able to connect to:"+pmUrl+" err:"+e.getMessage());
	}
    }

    private static PolicyTxf doPut(String pmUrl, PolicyTxf txf)  throws IOException, java.net.URISyntaxException {
	if (txf == null) {
	    return null;
	}

	//URL url = new URL(pmUrl);
	//HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	HttpURLConnection conn = getConn(pmUrl);
	
	try {
	    clientPrints("=> start updating txf id: "+txf.getId());		  
	    conn.setRequestMethod("PUT");
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setUseCaches(false);
	    conn.setAllowUserInteraction(false);
	    
	    conn.setRequestProperty("Content-Type", "application/json");

	    String gsStr=txf.toJson();

	    java.io.OutputStream os = conn.getOutputStream();
	    os.write(gsStr.getBytes());
	    os.flush();
	    
	    if (conn.getResponseCode() != 200) {
		throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+" msg="+conn.getResponseMessage());
	    }
	    
	    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	    
	    String output;
	    clientPrints(" output=> ");
	    while ((output = br.readLine()) != null) {
		clientPrints(output); 
		
		//PolicyTxf result = gs.fromJson(output, PolicyTxf.class);
		PolicyTxf result = PolicyTxf.objFromJson(output);
		clientPrints("     put result:"+result.toString());
		return result;
		
	    }
	    return null;
	} finally {
	    conn.disconnect();
	}
    }

    private int getConcurrencySuggestion(String src, String tgt) {	
	if (_baseUrl == null) {
	    return 12;
	}

	return getConcurrencySuggestion(src, tgt, -1, -1);
    }

    public int getConcurrencySuggestion(String src, String tgt, long totalBytes, int numOfFiles) {	
	clientPrints(" getConcurrencySuggestion: src="+src+" tgt="+tgt+" totalBytes="+totalBytes+" numOfFile="+numOfFiles);
	if (_baseUrl == null) {
	    return 12;
	}

	try {
	    clientPrints("Getting concurrency: "+_initTxf);
	    if (_initTxf == null) {
		if ((src == null) || (tgt == null)) {
		    clientPrints("  atm is not active. return -1");
		    return -1;
		}
		_initTxf = doPost(_baseUrl+"/transfer", assertDomainName(src), assertHostName(tgt), totalBytes/1048576, numOfFiles);
	    } else {
		if (_initTxf.threshold) {
		    return _initTxf.getStreams();
		} else {
		    _initTxf.setStreams(-1);
		    _initTxf = doPut(_baseUrl+"/transfer/"+_id, _initTxf);						    
		}
	    }
	    
	    if (_initTxf == null) {
		return 0; // something is wrong talking to the PM 
	    } 
	    _id = _initTxf.getId();
	    int result =  getStream(_initTxf);
	    clientPrints("   concurrent stream returned: "+result);
	    return result;
	} catch (IOException e) {
	    e.printStackTrace();
	} catch (java.net.URISyntaxException e) {
	    e.printStackTrace();
	}
	return -1;
    }

    private static int getStream(PolicyTxf txf) {
	//clientPrints(" ==> getStream of: "+txf);
	int result = 0;
	try {
	    //result = Integer.parseInt(txf.getProperty(edu.isi.policy.adapt.Constants.MAX_STREAMS_PROPERTY));
	    result = txf.getStreams();
	    if (result <= 0) {
		result =0;
	    }
	} catch (NumberFormatException e) {
	    e.printStackTrace();
	    clientPrints("!!! Unable to find streams assigned in the properties!");
	    result =0;
	}
	return result;
    }

    private static PolicyTxf doPost(String pmUrl, String src, String tgt, long sizeMB, int numFiles) throws IOException, java.net.URISyntaxException {
	//URL url = new URL(pmUrl);
	//HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	HttpURLConnection conn = getConn(pmUrl);

	try {
 	    clientPrints("=> create txf: "+src+" "+tgt);		  
	    conn.setRequestMethod("POST");
	    //conn.setRequestProperty("Accept", "MediaType.TEXT_PLAIN");
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setUseCaches(false);
	    conn.setAllowUserInteraction(false);
	    
	    //conn.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
	    conn.setRequestProperty("Content-Type", "application/json");
	    
	    PolicyTxf txf  = new PolicyTxf(src, tgt, sizeMB, numFiles);
	    String gsStr = txf.toJson();
	    //Gson gs = new Gson();
	    //String gsStr = gs.toJson(txf);
	    clientPrints("[gson]: post: "+gsStr);
	    
	    java.io.OutputStream os = conn.getOutputStream();
	    //os.write(input.getBytes());
	    os.write(gsStr.getBytes());
	    os.flush();
	    
	    if (conn.getResponseCode() != 200) {
		throw new RuntimeException("ATM Failed : HTTP error code : "+ conn.getResponseCode()+" msg="+conn.getResponseMessage());
	    }
	    
	    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	    
	    String output;
	    while ((output = br.readLine()) != null) {
		clientPrints("[gson]: out: "+output); 		
		//PolicyTxf result = gs.fromJson(output, PolicyTxf.class);
		PolicyTxf result = PolicyTxf.objFromJson(output);
		clientPrints("result: "+result);
		//clientPrints("     max str :"+result.getProperty(edu.isi.policy.adapt.Constants.MAX_STREAMS_PROPERTY));
		return result;	       
	    }		
	    return null;
	} finally {
	    conn.disconnect();
	}
    }

    private static PolicyTxf doGet(String pmUrl, String id) throws IOException {
	//URL url = new URL(pmUrl+"/"+id);
	//HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	HttpURLConnection conn = getConn(pmUrl+"/"+id);

	try {
	    clientPrints("=> get status of id: "+id+" from url:"+pmUrl);		  
	    conn.setRequestMethod("GET");
	    conn.setDoOutput(true);
	    conn.setDoInput(true);
	    conn.setUseCaches(false);
	    conn.setAllowUserInteraction(false);
	    
	    conn.setRequestProperty("Content-Type", "application/json");
	    
	    //conn.setRequestProperty("Accept", "MediaType.TEXT_PLAIN");
	    
	    if (conn.getResponseCode() != 200) {
		if (conn.getResponseCode() == 500) {
		    return null; // no such txf
		} else {
		    throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+"  msg="+conn.getResponseMessage());
		}
	    }
	    
	    BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	    
	    String output;

	    while ((output = br.readLine()) != null) {
		clientPrints(output);
		PolicyTxf result = PolicyTxf.objFromJson(output);
		//Gson gs = new Gson();
		//PolicyTxf result = gs.fromJson(output, PolicyTxf.class);
		//clientPrints(" assigned ==> "+result.getId());
		return result;
		
	    }	
	    return null;
	} finally {
	    conn.disconnect();
	}
    }


    private static void doList(String pmUrl) throws IOException {
	//URL url = new URL(pmUrl);
	//HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	HttpURLConnection conn = getConn(pmUrl);

	clientPrints("=> listing out current txfs in the server: "+pmUrl);		  
	//conn.setRequestMethod("GET");
	
	if (conn.getResponseCode() != 200) {
	    throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+" \n msg="+conn.getResponseMessage());
	}
		  
	BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	
	String output;

	int streamCount = 0;

	while ((output = br.readLine()) != null) {	    
	   clientPrints("[gson to parse]: output===>"+output);
	   //HashMap<String, PolicyTxf> result = gs.fromJson(output, new TypeToken<Map<String, PolicyTxf>>(){}.getType());
	   HashMap<String, PolicyTxf> result = PolicyTxf.collectionFromJson(output);
	    int total = result.size();
	    clientPrints("total="+result.size());
	
	    if (result.size() == 0) {
		continue;
	    }
	    Set keys = result.keySet();	
	    Iterator i = keys.iterator();
	    while (i.hasNext()) {
		Object curr = result.get(i.next());
		clientPrints("  ==> "+curr);
		streamCount += getStream((PolicyTxf)curr);
		//streamCount += getStream(gs.fromJson(curr.toString(), PolicyTxf.class));
	    }
	    //clientPrints("  ["+i+"] txf:"+result.get(i));
	}
	clientPrints("   Total streams used in policy module = "+streamCount);
    }


    private static void deleteAll(String pmUrl) throws IOException {
	//URL url = new URL(pmUrl);
	//HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	HttpURLConnection conn = getConn(pmUrl);

	clientPrints("=> first listing out current txfs in the server: "+pmUrl);		  
	
	if (conn.getResponseCode() != 200) {
	    throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+" \n msg="+conn.getResponseMessage());
	}
		  
	BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	
	String output;

	int streamCount = 0;

	ArrayList<String> allTxf = new ArrayList<String>();
	while ((output = br.readLine()) != null) {	    
	    //System.out.println("output===>"+output);
	    //java.util.HashMap result = gs.fromJson(output, java.util.HashMap.class );
	    //HashMap<String, PolicyTxf> result = gs.fromJson(output, new TypeToken<Map<String, PolicyTxf>>(){}.getType());
	    HashMap<String, PolicyTxf> result = PolicyTxf.collectionFromJson(output);

	    int total = result.size();
	    clientPrints("total="+result.size());
	
	    if (result.size() == 0) {
		continue;
	    }
	    Set keys = result.keySet();	
	    Iterator i = keys.iterator();
	    while (i.hasNext()) {
		PolicyTxf curr = (PolicyTxf)(result.get(i.next()));
		clientPrints("  ==> "+curr);
		allTxf.add(curr.getId());
	    }
	}
	//System.out.println("  deleting all:"+allTxf.size()+" txfs");
	for (int i=0; i<allTxf.size(); i++) {
	    doDelete(pmUrl+allTxf.get(i));
	}
    }


    private static void doDump(String pmUrl) throws IOException {
	HttpURLConnection conn = getConn(pmUrl);

	clientPrints("=> dumping out current txfs in the server: "+pmUrl);		  
	//conn.setRequestMethod("GET");
	
	if (conn.getResponseCode() != 200) {
	    throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+" \n msg="+conn.getResponseMessage());
	}
		  
	BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	
	String output;

	int streamCount = 0;

	while ((output = br.readLine()) != null) {	    
	   clientPrints("output===>"+output);
	}
    }

    private static void doDelete(String pmUrl) throws IOException {
	if (_baseUrl == null) {
	    return;
	}

	HttpURLConnection conn = getConn(pmUrl);
	conn.setRequestMethod("DELETE");
	
	if (conn.getResponseCode() != 200) {
	    throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode()+" \n msg="+conn.getResponseMessage());
	}
		  
	BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));									   
	
	String output;
	clientPrints("transfer list from Server .... ");
	while ((output = br.readLine()) != null) {	    
	    clientPrints(output);
	}
    }
}

class PolicyTxf {
    private URI source;
    private URI destination;
    private String id;
    private int streams;
    public boolean threshold = false;
    private long totalMB;
    private int numFiles;
    private int realtimeStreams=-1;

    public PolicyTxf() {}
    public PolicyTxf(String src, String tgt, long sizeMB, int numFilesToTxf)  throws URISyntaxException {
	source = new URI(src);
	destination = new URI(tgt);
	totalMB = sizeMB;
	numFiles = numFilesToTxf;
    }

    public String toJson() {
	Class<?> clazz = this.getClass();  
	java.lang.reflect.Field[] allFields = clazz.getDeclaredFields();

	//System.out.println("   all fields.length="+allFields.length);
	HashMap attrVal = new HashMap();
	for (int i=0; i<allFields.length; i++) {
	    try {
		//Field field = clazz.getField("fieldName"); //Note, this can throw an exception if the field doesn't exist.  
		java.lang.reflect.Field field = allFields[i];	    
		Object fieldValue = field.get(this);  
		if (fieldValue instanceof URI) {
		    attrVal.put(field.getName(), fieldValue.toString());
		} else {
		    attrVal.put(field.getName(), fieldValue);
		}
		//System.out.println("    field:"+field.getName()+"  "+fieldValue);
	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
	return org.json.simple.JSONObject.toJSONString(attrVal);
    }

    private static Map readback(String jsonOutput) {
	JSONParser parser = new JSONParser();
	ContainerFactory containerFactory = new ContainerFactory() {
		public List creatArrayContainer() { return new LinkedList(); }

		public Map createObjectContainer() { return new LinkedHashMap(); }		
	    };
	
	try{
	    Map m = (Map)parser.parse(jsonOutput, containerFactory);
	    return m;
	} catch(ParseException pe){
	    pe.printStackTrace();
	    return null;
	}                        
    }

    public static PolicyTxf objFromJson(String jsonOutput) {
	Map m = readback(jsonOutput);
	if (m == null) {
	    return null;
	}

	return objFromJson(m);
    }

    private static PolicyTxf objFromJson(Map m) {
	PolicyTxf result = new PolicyTxf();

	Class<?> clazz = PolicyTxf.class;
	java.lang.reflect.Field[] allFields = clazz.getDeclaredFields();

	for (int i=0; i<allFields.length; i++) {
	    try {
		//Field field = clazz.getField("fieldName"); //Note, this can throw an exception if the field doesn't exist.  
		java.lang.reflect.Field field = allFields[i];	    
		//System.out.println(field.getName()+" has type:"+field.getType());
		if (field.getType().equals(URI.class)) {
		    field.set(result, new URI(m.get(field.getName()).toString()));
		} else if (field.getType().equals(String.class)) {
		    field.set(result, m.get(field.getName()).toString());
		} else if (field.getType().equals(int.class)) {
		    field.set(result, Integer.parseInt(m.get(field.getName()).toString()));
		} else if (field.getType().equals(long.class)) {
		    field.set(result, Long.parseLong(m.get(field.getName()).toString()));
		}
	    } catch (Exception e) {
		e.printStackTrace();
	    }	    
	}
	return result;
    }


    //
    // the entry after reading from parser is like:
    // {totalMB=128, realtimeStreams=-1, destination=gsiftp://red-gridftp.unl.edu:2811//mnt/hadoop/dropfiles/nersc/junk/medium_230}
    // which is not accepted by jsonsimple, it needs something like:
    // {"totalMB": 128, "realtimeStreams": -1, "destination": "gsiftp://red-gridftp.unl.edu:2811//mnt/hadoop/dropfiles/nersc/junk/med};
    //
	
    private static HashMap getValuePairs(String val) {
	val = val.substring(1, val.length()-1);

	HashMap result = new HashMap();
	for (String retval: val.split(", ")) {
		String[] curr = retval.split("=");
		result.put(curr[0], curr[1]);
	}

	return result;
    }

    public static HashMap<String, PolicyTxf> collectionFromJson(String jsonOutput) {

	Map m = readback(jsonOutput);
	if (m == null) {
	    return null;
	}


	HashMap<String, PolicyTxf> result = new HashMap<String, PolicyTxf>();

	Iterator iter = m.entrySet().iterator();

	while(iter.hasNext()){
	    Map.Entry entry = (Map.Entry)iter.next();

	    String entryVal = entry.getValue().toString();
	    HashMap valPairs = getValuePairs(entryVal);
	    result.put(entry.getKey().toString(), objFromJson(valPairs));
	}
	return result;
    }

    public String getId() {
	return id;
    }

    public void setId(String v) {
	id = v;
    }

	// return a flag to indicate whether value is changed
    public boolean setRealtimeStreams(int rts) {
	if (rts == realtimeStreams) {
		return false;
	}
	realtimeStreams = rts;
	return true;
    }

    public void setStreams(int val) {
	streams = val;
    }

    public int getStreams() {
	return streams;
    }

    public String toString() {
	String common = " src="+source+" tgt="+destination+" threshhold="+threshold+" totalMB="+totalMB+" numFiles="+numFiles+" rts="+realtimeStreams;
	if (getStreams() > 0) {
	    return "[txf id="+getId()+"] streams="+getStreams()+common;
	} else {
	    return "[txf id="+getId()+"] "+common;
	}
    }
}

