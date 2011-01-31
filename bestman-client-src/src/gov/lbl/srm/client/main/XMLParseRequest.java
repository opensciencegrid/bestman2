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

package gov.lbl.srm.client.main;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.srm.client.exception.*;


public class XMLParseRequest {

  private Hashtable tokens = new Hashtable();
  private final static int _request = 0;
  private final static int _file = 1;
  private final static int _sourceurl = 2;
  private final static int _targeturl = 3;
  private final static int _lahfs = 4;  
  private final static int _recursive=5;
  private final static int _extLifeTime=6;
  private final static int _size = 7;
  private final static int _overwritefiles=8;

  private static Log logger;
  private java.util.logging.Logger _theLogger;
  private boolean silent;
  private boolean useLog;

  private Request request = new Request();
  private Vector inputVec = new Vector ();

  public XMLParseRequest (String fileName,
		java.util.logging.Logger theLogger, boolean silent,boolean useLog) 
		throws NumberFormatException, Exception {
    
    _theLogger = theLogger;
    ClassLoader cl = this.getClass().getClassLoader();
    this.silent = silent;
    this.useLog = useLog;
    try {
       Class c = cl.loadClass("gov.lbl.srm.client.main.XMLParseRequest");
       logger = LogFactory.getLog(c.getName());
     }catch(ClassNotFoundException cnfe) {
      inputVec = new Vector ();
      inputVec.addElement("ClassNotFoundException="+cnfe.getMessage());
      util.printEventLog(_theLogger,"XMLParseRequest",inputVec,silent,useLog);
      throw new SRMClientException(cnfe.getMessage());
    }

    tokens.put("request", new Integer(_request)); 
    tokens.put("file", new Integer(_file)); 
    tokens.put("sourceurl", new Integer(_sourceurl)); 
    tokens.put("targeturl", new Integer(_targeturl)); 
    tokens.put("recursive", new Integer(_recursive)); 
    tokens.put("extlifetime", new Integer(_extLifeTime)); 
    tokens.put("overwritefiles",new Integer(_overwritefiles));
    tokens.put("lahfs", new Integer(_lahfs)); 
    tokens.put("size", new Integer(_size)); 

    DocumentBuilderFactory factory = 
    DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = factory.newDocumentBuilder();
    Document document = parser.parse(fileName); 
    document.getDocumentElement ().normalize ();

    Element rootNode = document.getDocumentElement();

    int value = getToken(rootNode.getNodeName());
    switch(value) {
         case _request : parseRootNode(rootNode); break;
         case -1 : 
                inputVec = new Vector ();
				inputVec.addElement("Bad Element"); 
                util.printEventLog(_theLogger,"XMLParseRequest",inputVec,silent,useLog);
	            //System.out.println("Bad Element"); 
    }
  }

  private int getToken(String name) {
      Object obj = tokens.get(name);
      if(obj == null) { 
         Object o1 = tokens.get(name.substring(1));
         if(o1 == null) return -1;
         return ((Integer)o1).intValue(); 
      }
      return ((Integer)obj).intValue(); 
  }

  public Request getRequest () {
     return request;
  }


  private void parseFileNode (Node n)  throws Exception {

     NodeList nl = ((Node)n).getChildNodes ();
     int numChild = nl.getLength();
     FileInfo fInfo = new FileInfo();

     for(int i = 0; i < numChild; i++) {
       Node node = nl.item(i);
       int nodeType = node.getNodeType();
       if(nodeType == 3)  {
          ; //skip
       }  
       else {
         int value = getToken(node.getNodeName());
         switch (value) {
            case _recursive : 
			        String recursive = node.getFirstChild().getNodeValue();
                    Boolean b = new Boolean(recursive);
                    fInfo.setIsRecursive(b);
                    break; 
            case _overwritefiles : 
			        String overwritefiles = node.getFirstChild().getNodeValue();
                    Boolean b1 = new Boolean(overwritefiles);
                    fInfo.setOverwriteFiles(b1.booleanValue());
                    break; 
            case _extLifeTime : 
			        String lifeTime = node.getFirstChild().getNodeValue();
                    try {
                      Long ll = new Long(lifeTime);
                      fInfo.setExtLifeTime(ll.longValue());
                    }catch(NumberFormatException nfe) {}
                    break; 
            case _sourceurl : 
			        String surl = node.getFirstChild().getNodeValue();
                    /*
                    if(!request.isModeSet()) {
                      if(surl.trim().startsWith("file:")) {
                        request.setModeType("put");
                      }
                    }
                    */
			        fInfo.setSURL(surl);
			        fInfo.setOrigSURL(surl);
 		 	        break;
            case _lahfs :  
				    fInfo.setLahfs(true); 
	                break;
            case _targeturl : 
					String turl = node.getFirstChild().getNodeValue();
		    	    fInfo.setTURL(turl);
			        fInfo.setOrigTURL(turl);
                    /* 
                    if(!request.isModeSet()) {
                      if(turl.trim().startsWith("file:")) {
                        request.setModeType("get");
                      }
                      if(turl.trim().startsWith("srm:") && 
						!request.getModeType().equalsIgnoreCase("put")) {
                          request.setModeType("copy");
                      }
                    }
                    */
 		 	        break;
            case _size : 
			       fInfo.setShowSize(true);
			       fInfo.setExpectedSize
				     (node.getFirstChild().getNodeValue());
 		 	       break;
            default :
              inputVec = new Vector ();
              inputVec.addElement("Bad Attribute " + node.getNodeName() +
                 " is not defined.");
              util.printEventLog(_theLogger,"XMLParseRequest.parseFileNode",inputVec,silent,useLog);
              throw new Exception("Bad Attribute " + node.getNodeName() +
                 " is not defined.");
         }
       }
     }
     request.addFileInfo(fInfo);
  }

  private void parseRootNode (Node n)  throws NumberFormatException, Exception {

     NodeList nl = ((Node)n).getChildNodes ();
     int numChild = nl.getLength();

     for(int i = 0; i < numChild; i++) {
       Node node = nl.item(i);
       int type = node.getNodeType();
       if(type == 3) {
         ; //skip
       }
       else {
         int value = getToken(node.getNodeName());
         switch (value) {
           case _file : parseFileNode(node); break;
           default :
               inputVec = new Vector ();
               inputVec.addElement("Bad Attribute " + node.getNodeName() +
                  " is not defined.");
               util.printEventLog(_theLogger,"XMLParseRequest.parseRootNode",inputVec,silent,useLog);
               throw new Exception("Bad Attribute " + node.getNodeName() +
                  " is not defined.");
       }
      }
    }
  }

  public static void main(String[] args) {

  try {
    XMLParseRequest xmlTest = new XMLParseRequest ("./putrequest.xml",null,false,false);
    Request request = xmlTest.getRequest();
    Vector fInfo = request.getFileInfo();
    for(int i = 0; i < fInfo.size(); i++) {
      FileInfo file = (FileInfo) fInfo.elementAt(i);
      System.out.println("SURL " + file.getSURL());
      System.out.println("TURL " + file.getTURL());
      System.out.println("ExpectedSize " + file.getExpectedSize());
      System.out.println("LAHFS " + file.getLahfs());
    }
  }catch(SRMClientException srme) {
    System.out.println("SRMClientException " + srme.getMessage());
    srme.printStackTrace();
  }catch(Exception e) {
    System.out.println("Exception " + e.getMessage());
    e.printStackTrace();
  }
  }

}
