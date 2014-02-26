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

package gov.lbl.adapt.srm.client.main;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.adapt.srm.client.exception.*;


public class XMLParseConfig {

  private String _userCert="Enter a value";
  private String _userKey="Enter a value";
  private String _proxyFile="Enter a value";
  private String _renewProxy="false";
  private String _reqLogFile="";
  private String _lastTargetLocation="Enter a value";

  private Hashtable tokens = new Hashtable();
  private final static int _config = 0;
  private final static int _usercert = 1;
  private final static int _userkey = 2;
  private final static int _proxyfile = 3;
  private final static int _renew = 4;
  private final static int _reqlogfile = 5;
  private final static int _lasttargetloc = 6;

  private static Log logger;

  public XMLParseConfig()  {
  }
  
  public XMLParseConfig (String fileName) throws NumberFormatException, 
  		Exception {

    try {
     ClassLoader cl = this.getClass().getClassLoader();
     Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.XMLParseConfig");
     logger = LogFactory.getLog(c.getName());
    }catch(ClassNotFoundException cnfe) {
      System.out.println("ClassNotFoundException ");
      throw new SRMClientException(cnfe.getMessage());
    }

    tokens.put("config", new Integer(_config)); 
    tokens.put("user-cert", new Integer(_usercert)); 
    tokens.put("user-key", new Integer(_userkey));
    tokens.put("proxy-file", new Integer(_proxyfile));
    tokens.put("renew-proxy-auto", new Integer(_renew));
    tokens.put("request-log-location", new Integer(_reqlogfile));
    tokens.put("last-target-location", new Integer(_lasttargetloc));

    DocumentBuilderFactory factory = 
    DocumentBuilderFactory.newInstance();
    DocumentBuilder parser = factory.newDocumentBuilder();
    Document document = parser.parse(fileName); 
    document.getDocumentElement ().normalize ();

    Element rootNode = document.getDocumentElement();

    int value = getToken(rootNode.getNodeName());
    switch(value) {
         case _config : parseRootNode(rootNode); break;
         case -1 : logger.debug("Bad Element"); 
		   System.out.println("Bad Element"); 
    }
  }

  public String getUserCert() {
    return _userCert.trim();
  }

  public String getUserKey() {
    return _userKey.trim();
  }

  public String getReqLogFileLocation() {
    return _reqLogFile;
  }

  public void setReqLogFileLocation(String reqLogFile) {
    _reqLogFile = reqLogFile;
  }

  public String getProxyFile() {
    return _proxyFile.trim();
  }

  public void setProxyFile(String proxyFile) {
     _proxyFile = proxyFile;
  }

  public void setUserCert (String userCert) {
     _userCert = userCert;
  }

  public void setUserKey (String userKey) {
     _userKey = userKey;
  }

  public boolean getRenewProxy() {
    return false;
  }

  public void setLastTargetLocation(String loc) {
    _lastTargetLocation = loc;
  }

  public String getLastTargetLocation() {
    return _lastTargetLocation;
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

  private void parseRootNode (Node n)  throws Exception {

     NodeList nl = ((Node)n).getChildNodes ();
     int numChild = nl.getLength();

     for(int i = 0; i < numChild; i++) {
       Node node = nl.item(i);
       int nodeType = node.getNodeType();
       if(nodeType == 3)  {
          ; //skip
       }  
       else {
         int value = getToken(node.getNodeName());
         switch (value) {
            case _usercert :  _userCert =  node.getFirstChild().getNodeValue();
 		 	       break;
            case _userkey :   _userKey =  node.getFirstChild().getNodeValue();
 		 	       break;
            case _proxyfile : _proxyFile =  node.getFirstChild().getNodeValue();
 		 	       break;
            case _renew :     _renewProxy =  
				  node.getFirstChild().getNodeValue(); break;
            case _reqlogfile: _reqLogFile =  
				  node.getFirstChild().getNodeValue(); break;
            case _lasttargetloc: _lastTargetLocation =  
				  node.getFirstChild().getNodeValue(); break;
            default :
              logger.debug("Bad Attribute " + node.getNodeName() +
                 " is not defined.");
              throw new Exception("Bad Attribute " + node.getNodeName() +
                 " is not defined.");
         }
       }
     }
  }

  public static void main(String[] args) {

  try {
    XMLParseConfig xmlTest = new XMLParseConfig ("./config.xml");
  }catch(Exception e) {
    System.out.println("Exception " + e.getMessage());
    e.printStackTrace();
  }
    
  }

}
