/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2010-2014, The Regents of the University of California, 
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

import javax.swing.JFrame;
import javax.swing.JPanel;

import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Vector;
import java.util.Map;
import java.util.List;
import java.util.Vector;
import java.util.Set;
import java.io.*;
import java.beans.XMLEncoder;
import java.beans.XMLDecoder;
import javax.imageio.ImageIO;

import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xml.serialize.*;

import java.net.MalformedURLException;

import gov.lbl.adapt.srm.client.transfer.*;
import gov.lbl.adapt.srm.client.transfer.globus.*;
import gov.lbl.adapt.srm.util.*;
import gov.lbl.adapt.srm.client.intf.*;
import gov.lbl.adapt.srm.client.util.*;
import gov.lbl.adapt.srm.client.exception.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.gridforum.jgss.ExtendedGSSManager;
import org.gridforum.jgss.ExtendedGSSCredential;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Class SRMClientFileTransferN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class SRMClientFileTransferN
	implements threadIntf, FileEventListener, FTPIntf
{

private SRMClientIntf _parent;
private TransferThread tThread;
private String targetDir="";
private int conc;
private Request requestMode;
private Vector fileInfo;

private Vector pList = new Vector ();
private boolean isCancel;
private SharedObjectLock sLock;

private int requestedFiles;
private int completedFiles;
private int errorFiles;
private int existsFiles;
private String resultStatus="";
private String resultExplanation="";

private static Log logger;
private int _retryAllowed;
private boolean _debug;
private boolean silent=false;
private boolean useLog=false;
private boolean reportSaved;
private boolean textReport=false;
private boolean processSaveActionStarted=false;
private java.util.logging.Logger _theLogger;
private Vector inputVec = new Vector();
private PrintIntf pIntf;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// SRMClientFileTransferN
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public SRMClientFileTransferN(SRMClientIntf parent, TransferThread tThread,
			      String targetDir, int concurrency, 
			      Request requestMode, Vector fileInfo, int retryAllowed,
			      boolean _debug, boolean textReport, java.util.logging.Logger theLogger, 
			      boolean silent, boolean useLog, PrintIntf pIntf) {
    
    ClassLoader cl = this.getClass().getClassLoader();
    try {
	Class c = cl.loadClass("gov.lbl.adapt.srm.client.main.SRMClientFileTransferN");
	logger = LogFactory.getLog(c.getName());
    }catch(ClassNotFoundException cnfe) {
	System.out.println("ClassNotFoundException ");
    }
    
    _theLogger = theLogger;
    _parent = parent;
    this.tThread = tThread; 
    this.targetDir = targetDir;
    this.requestMode = requestMode;
    this.fileInfo = fileInfo;
    this._debug = _debug;
    this.pIntf = pIntf;
    conc = concurrency;
    this.silent = silent;
    this.textReport = textReport;
    this.useLog=useLog;
    _retryAllowed = retryAllowed;
    
    pList.add("gsiftp://");
    pList.add("ftp://");
    pList.add("srm://");
    pList.add("http://");
    pList.add("https://");
    pList.add("file:////");
    
    this.tThread.setFTPIntf(this);
    this.tThread.setFileEventListener(this);
    this.tThread.setLogger(logger,_theLogger);
    this.tThread.setTargetDir(targetDir);
    this.tThread.setProtocolList(pList);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  @@@@@   Start methods implemented for FTPIntf (window)
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    public synchronized boolean checkDiskSpaceFull(SharedObjectLock sLock,
						   File targetDir, long size) throws DiskSpaceFullException
    {
	this.sLock = sLock;
	final File tDir = targetDir;
	final long tsSize = size;
	
	try {
	    long ll = TPlatformUtil.getFreeSpace(targetDir);
	    //logger.debug("Available Disk Size " + ll);
	    //logger.debug("Current File Size " + size);
	    //inputVec = new Vector ();
	    //inputVec.addElement("Available Disk Size=" + ll);
	    //inputVec.addElement("Current File Size=" + size);
	    //util.printEventLog(_theLogger,"SRMFileTransferN.checkDiskSpaceFull",inputVec);
	    if(_debug) {
		System.out.println("\nSRM-CLIENT: Available Disk Size="+ll);
		System.out.println("SRM-CLIENT: Current File Size=" + size);
	    }
	    if((size+100000) >= ll) { 
		
		if(!silent) {
		    System.out.println("SRM-CLIENT: Disk space is full now. Please remove some files " +
				       " and type yes to continue further, else the request will " + 
				       " be cancelled");
		}
		/*
		  inputVec = new Vector ();
		  inputVec.addElement("Disk space is full now. Please remove some files " +
		  " and type yes to continue further, else the request will " + 
		  " be cancelled");
		  util.printEventLog(_theLogger,"SRMFileTransferN.checkDiskSpaceFull",inputVec);
		*/
		
		InputStreamReader inputStreamReader = new InputStreamReader ( System.in );
		BufferedReader stdin = new BufferedReader ( inputStreamReader );
		String line  = stdin.readLine();
		if(line.equalsIgnoreCase("yes")) {
		    if(checkDiskSpaceFull
		       (SRMClientFileTransferN.this.sLock, tDir, tsSize)) {
			SRMClientFileTransferN.this.sLock.setIncrementCount(false);
		    }
		}
		else {
		    if(!silent) {
			System.out.println
			    ("SRM-CLIENT: Request cancelled, because of disk space full ++++");
		    }
		    /*
		      inputVec = new Vector ();
		      inputVec.addElement("Request cancelled, because of disk space full");
		      util.printEventLog(_theLogger,"SRMFileTransferN.checkDiskSpaceFull",inputVec);
		    */
		    isCancel = true;
		    SRMClientFileTransferN.this.sLock.setIncrementCount(true);
		}
		return false;	    
	    } else {
		SRMClientFileTransferN.this.sLock.setIncrementCount(false);
		return true;
	    }
	}catch(IOException ioe) {
	    util.printEventLogException(_theLogger,"",ioe);
	    /*
	      inputVec = new Vector ();
	      inputVec.addElement("Exception="+ioe.getMessage());
	      util.printEventLog(_theLogger,"SRMFileTransferN.checkDiskSpaceFull",inputVec);
	    */
	    if(!silent) {
		System.out.println("SRM-CLIENT: IOException " + ioe.getMessage());
	    }
	}
	return true;
    }

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// enableTransferButton
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void enableTransferButton (boolean b, boolean ok) {
 _parent.enableTransferButton(b, ok);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isGui
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isGui () {
  return false;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getParentWindow
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public JFrame getParentWindow () {
  return null;
}

public synchronized void refreshView() {
}

public synchronized void updateView(FileStatusGUI fgui) {
}

public synchronized void prepareView() {
}

public void scrollView() {
}

public void setCurrentTargetIndex(int idx) {
}

public void resetValues (String targetDir, boolean b) {
  completedFiles = 0;
  errorFiles = 0; 

  if(requestMode == null) return; 

  Object[] objArray = fileInfo.toArray();
  if(requestMode.getModeType().equalsIgnoreCase("Get")) {
    for(int i = 0; i < objArray.length; i++) {
      FileIntf fIntf = (FileIntf) objArray[i];
      fIntf.setTargetDir(targetDir);
      if(b) {
       fIntf.setStatusLabel("Pending");
      }
      else {
        if(!fIntf.getStatusLabel().equalsIgnoreCase("exists")) {
          fIntf.setStatusLabel("Pending");
        }  
      }
    }
  }
  if(requestMode.getModeType().equalsIgnoreCase("Put")) {
    for(int i = 0; i < objArray.length; i++) {
      FileIntf fIntf = (FileIntf) objArray[i];
      fIntf.setTargetDir(targetDir);
      if(b) {
       fIntf.setStatusLabel("Pending");
      }
      else {
        if(!fIntf.getStatusLabel().equalsIgnoreCase("exists")) {
          fIntf.setStatusLabel("Pending");
        }  
      }
    }
  }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// initProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential initProxy() throws Exception {
  return _parent.createProxy(_parent.getPassword());
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isRequestCancel
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isRequestCancel() {
  return isCancel;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// checkProxy
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public GSSCredential checkProxy() throws ProxyNotFoundException
{
  GSSCredential mycred = null;
  try {
    mycred = _parent.getCredential();
    mycred = _parent.checkTimeLeft();
     
  }catch(Exception e) {
                    util.printEventLogException(_theLogger,"",e);
     /* 
     inputVec = new Vector ();
     inputVec.addElement("Exception="+e.getMessage());
     inputVec.addElement("Please Resume transfer after your renew your credentials.");
     util.printEventLog(_theLogger,"SRMFileTransferN.checkProxy",inputVec);
     */ 
     System.out.println("SRM-CLIENT: Exception " + e.getMessage());
     //e.printStackTrace();
     throw new ProxyNotFoundException (e.getMessage()+"\n"+
        "Please Resume transfer after your renew your credentials.");

  }
  return mycred;
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  @@@@@   end methods implemented for FTPIntf (window)
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getLogger
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public Log getLogger() {
  return logger;
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getModeType
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String getModeType () {
  return requestMode.getModeType();
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// setFileInfoForSavingReport
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
public void setFileInfoForSavingReport (Vector vec) {
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processSaveAction
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processSaveAction (String fileName, String resultStatus, 
			       String resultExplanation) {
    if(!processSaveActionStarted) {
	processSaveActionStarted = true;
	this.resultStatus = resultStatus;
	this.resultExplanation = resultExplanation;
	//ThreadCallBack tb = new ThreadCallBack (this);
	//tb.setFileName(fileName);
	//tb.start();
	processThreadRequest(fileName);
    }
}

    private void record(String tag, int numSucc, int numErr, int total, String status, String comment) {
	if(pIntf != null) {
	    pIntf.setCompleted(true);
	}

	if (comment != null) {
	    String[] msg = {"numSucc="+numSucc, "numError="+numErr, "total="+total, "status="+status, comment};	   
	    SRMClientN.logMsg(msg, tag, null);
	} else {
	    String[] msg = {"numSucc="+numSucc, "numError="+numErr, "total="+total, "status="+status};	   
	    SRMClientN.logMsg(msg, tag, null);
	}
    }
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// printTextReport
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void printTextReport() {
 try {
     String tag="PrintReport";
    Object[] objArray = fileInfo.toArray();

    inputVec.clear();
    util.printEventLog(_theLogger,"PrintingReport",inputVec,silent,useLog);
    int cFiles=0;
    int eFiles=0;
    for(int i = 0; i < objArray.length; i++) {
         FileIntf fIntf = (FileIntf) objArray[i];
         if(fIntf.getCompleted ()) {
           cFiles++;         
         } else if(fIntf.getFailed ()) {
           eFiles++;
         }
    }
    completedFiles = cFiles;
    errorFiles = eFiles;
    if(errorFiles == 0) {
	if(completedFiles != 0) {
	    if(_parent.getRequestTimedOut()) {
		util.printMessage("\nSRM-CLIENT: Request completed with failure", logger,silent);
				  
		resultStatus="SRM_RETURNED_NO_STATUS"; 
		resultExplanation="Request TimedOut. Reason, may be Server is busy";

		record(tag, completedFiles, errorFiles, objArray.length, resultStatus, "Timed out. maybe server is busy");
	    } else if(completedFiles < objArray.length) {
		util.printMessage("\nSRM-CLIENT: Request completed with failure", logger,silent);
				  
		resultStatus="SRM_PARTIAL_SUCCESS"; 
		resultExplanation="Request TimedOut.";

		record(tag, completedFiles, errorFiles, objArray.length, resultStatus, null);
	    } else {
		util.printMessage("\nSRM-CLIENT: Request completed with success", logger,silent);				  
		resultStatus="SRM_SUCCESS"; 
		record(tag, completedFiles, errorFiles, objArray.length, resultStatus, null);
	    }	
	} else {
	    //probably interrupted because of timeout
	    if(_parent.getRequestTimedOut()) {
		util.printMessage("\nSRM-CLIENT: Request completed with failure", logger,silent);
				  
		if(pIntf != null) {
		    pIntf.setCompleted(true);
		}
		resultStatus="SRM_RETURNED_NO_STATUS"; 
		resultExplanation="Request TimedOut. Reason, may be Server is busy";

		record(tag, completedFiles, errorFiles, objArray.length, resultStatus, "Timed out. maybe server is busy");
	    } 
	}    
    } else {
	util.printMessage("\nSRM-CLIENT: Request completed with failure", logger,silent);
	
	if(completedFiles == 0) {
	    if(!resultStatus.equals("SRM_RETURNED_NO_STATUS")) { 
		resultStatus="SRM_FAILURE";
		record(tag, completedFiles, errorFiles, objArray.length, resultStatus, null);
	    }      
	} else {
	    resultStatus="SRM_PARTIAL_SUCCESS";
	    record(tag, completedFiles, errorFiles, objArray.length, resultStatus, null);
	}
    }

    if(resultStatus.equals("SRM_RETURNED_NO_STATUS"))  {
	errorFiles=objArray.length;

	record(tag, completedFiles, errorFiles, objArray.length, resultStatus, 
	       "Setting errorFiles=totalFiles due to SRM_RETUREND_NO_STATUS");
    }

    SRMClientN.logMsg("Printing text report now:", null, "\nSRM-CLIENT:");
    SRMClientN.logMsg("SRM-CLIENT*REQUESTTYPE="+requestMode.getModeType(), null, "\nSRM-CLIENT:");
    SRMClientN.logMsg("SRM-CLIENT*TOTALFILES="+objArray.length, null, "\nSRM-CLIENT:");
    SRMClientN.logMsg("SRM-CLIENT*TOTAL_SUCCESS="+completedFiles, null, "\nSRM-CLIENT:");
    SRMClientN.logMsg("SRM-CLIENT*TOTAL_FAILED="+errorFiles, null, "\nSRM-CLIENT:");

    Vector ridList = _parent.getRequestToken();
    StringBuffer rids = new StringBuffer(); 
    for(int i = 0; i < ridList.size(); i++) {
	if( i == ridList.size()-1)
	    rids.append(ridList.elementAt(i));
	else 
	    rids.append(ridList.elementAt(i)+",");
    }
    SRMClientN.logMsg("SRM-CLIENT*REQUEST_TOKEN="+ rids.toString(), null, "\nSRM-CLIENT:");
    SRMClientN.logMsg("SRM-CLIENT*REQUEST_STATUS="+ resultStatus, null, "\nSRM-CLIENT:");

    if(resultExplanation != null && !resultExplanation.equals("")) {
	SRMClientN.logMsg("SRM-CLIENT*REQUEST_EXPLANATION="+ resultExplanation, null, "\nSRM-CLIENT:");
    }

    for(int i = 0; i < objArray.length; i++) {
	FileIntf fIntf = (FileIntf) objArray[i];
	if(!fIntf.getOrigSURL().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*SOURCEURL["+i+"]="+ fIntf.getOrigSURL(), null, "\nSRM-CLIENT:");
	}
	if(!fIntf.getOrigTURL().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*TARGETURL["+i+"]="+ fIntf.getOrigTURL(), null, "SRM-CLIENT:");
	}
	if(!fIntf.getTransferURL().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*TRANSFERURL["+i+"]="+ fIntf.getTransferURL(), null, "SRM-CLIENT:");
	}
	if(!fIntf.getActualSize().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*ACTUALSIZE["+i+"]="+ fIntf.getActualSize(), null, "SRM-CLIENT:");
	}
	if(!fIntf.getFileStatus().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*FILE_STATUS["+i+"]="+ fIntf.getFileStatus(), null, "SRM-CLIENT:");
	}
	if(!fIntf.getFileExplanation().equals("")) {
	    SRMClientN.logMsg("SRM-CLIENT*EXPLANATION["+i+"]="+ fIntf.getFileExplanation(),null, "SRM-CLIENT:");	      
	}
	if(!fIntf.getErrorMessage().equals("") && 
	   !fIntf.getFileExplanation().equals(fIntf.getErrorMessage())) {
	    SRMClientN.logMsg("SRM-CLIENT*MESSAGE["+i+"]="+ fIntf.getErrorMessage(), null, "SRM-CLIENT:");
	}
    }//end for
 }catch(Exception e) {
     util.printEventLogException(_theLogger,"",e);
     //ShowException.logDebugMessage(logger,e);
     e.printStackTrace();
     util.printMessage("Exception="+e.getMessage(),logger,silent);
     util.printMessageHException("Exception="+e.getMessage(),pIntf);
     ShowException.showMessageDialog(_parent.getFrame(), "Exception : " + e.getMessage());				     
 }
}


//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processThreadRequest
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processThreadRequest(String fileName) {
    String tag = "processThreadRequest";
    try {
	if(textReport) {
	    printTextReport();
	}
	
	SRMClientN.logMsg("SavingReport  filename="+fileName, tag, null);
	
	Object[] objArray = fileInfo.toArray();
	
	DocumentBuilderFactory factory =  DocumentBuilderFactory.newInstance();
	    
	DocumentBuilder parser = factory.newDocumentBuilder ();
	org.w3c.dom.Document doc = parser.newDocument();
	org.w3c.dom.Element root = doc.createElement("report");
	Attr reqName = doc.createAttribute("filename");
	if(!fileName.equals("")) {
	    reqName.setValue(fileName);
	}
	root.setAttributeNode(reqName);
	Attr type = doc.createAttribute("requesttype");
	type.setValue(requestMode.getModeType());
	root.setAttributeNode(type);
	Attr totalFiles = doc.createAttribute("totalfiles");
	totalFiles.setValue(""+objArray.length);
	root.setAttributeNode(totalFiles);
	int cFiles=0;
	int eFiles=0;
	for(int i = 0; i < objArray.length; i++) {
	    FileIntf fIntf = (FileIntf) objArray[i];
	    if(fIntf.getCompleted ()) {
		cFiles++;	    
	    }else if(fIntf.getFailed ()) {
		eFiles++;
	    }
	}
	completedFiles = cFiles;
	errorFiles = eFiles;
	Attr successFiles = doc.createAttribute("totalfiles-success");
	successFiles.setValue(""+completedFiles);
	root.setAttributeNode(successFiles);
	Attr failedFiles = doc.createAttribute("totalfiles-failed");
	failedFiles.setValue(""+errorFiles);
	root.setAttributeNode(failedFiles);
	Attr totalFilesPerRequest = doc.createAttribute("totalfiles-per-request");
	totalFilesPerRequest.setValue(""+_parent.getTotalFilesPerRequest());
	root.setAttributeNode(totalFilesPerRequest);
	Attr totalSubRequest = doc.createAttribute("totalfiles-subrequests");
	totalSubRequest.setValue(""+_parent.getTotalSubRequest());
	root.setAttributeNode(totalSubRequest);
	Vector ridList = _parent.getRequestToken();
	StringBuffer rids = new StringBuffer(); 
	for(int i = 0; i < ridList.size(); i++) {
	    if( i == ridList.size()-1)
		rids.append(ridList.elementAt(i));
	    else 
		rids.append(ridList.elementAt(i)+",");
	}
	Attr requestTokens = doc.createAttribute("rids");
	requestTokens.setValue(""+rids.toString());
	root.setAttributeNode(requestTokens);
	Attr requestStatus = doc.createAttribute("request-status");
	requestStatus.setValue(resultStatus);
	root.setAttributeNode(requestStatus);
	Attr requestExplanation = doc.createAttribute("request-explanation");
	requestExplanation.setValue(resultExplanation);
	root.setAttributeNode(requestExplanation);
	for(int i = 0; i < objArray.length; i++) {
	    FileIntf fIntf = (FileIntf) objArray[i];
	    org.w3c.dom.Element file = doc.createElement("file");
	    org.w3c.dom.Element surl = doc.createElement("sourceurl");
	    surl.appendChild(doc.createTextNode(fIntf.getOrigSURL()));
	    file.appendChild(surl);
	    org.w3c.dom.Element turl = doc.createElement("targeturl");
	    turl.appendChild(doc.createTextNode(fIntf.getOrigTURL()));
	    file.appendChild(turl);
	    org.w3c.dom.Element transferurl = doc.createElement("transferurl");
	    transferurl.appendChild(doc.createTextNode(fIntf.getTransferURL()));
	    file.appendChild(transferurl);
	    if(!requestMode.getModeType().equals("get") &&
	       !requestMode.getModeType().equals("put")) {
		org.w3c.dom.Element esize = doc.createElement("expectedsize");
		esize.appendChild(doc.createTextNode (fIntf.getExpectedSize()));
		file.appendChild(esize);
	    }
	    org.w3c.dom.Element asize = doc.createElement("actualsize");
	    asize.appendChild(doc.createTextNode (fIntf.getActualSize()));
	    file.appendChild(asize);
	    /*
	      org.w3c.dom.Element status = doc.createElement("status");
	      status.appendChild(doc.createTextNode (fIntf.getStatusLabel()));
	      file.appendChild(status);
	    */
	    org.w3c.dom.Element fileStatus = doc.createElement("file-status");
	    fileStatus.appendChild(doc.createTextNode (fIntf.getFileStatus()));
	    file.appendChild(fileStatus);
	    org.w3c.dom.Element fileExplanation = doc.createElement("file-explanation");
	    fileExplanation.appendChild(doc.createTextNode (fIntf.getFileExplanation()));
	    file.appendChild(fileExplanation);
	    org.w3c.dom.Element timetaken = doc.createElement("timetaken");
	    timetaken.appendChild(doc.createTextNode (fIntf.getTimeTaken()));
	    file.appendChild(timetaken);
	    if(!fIntf.getErrorMessage().equals("")) {
		org.w3c.dom.Element errormessage = doc.createElement("message");
		errormessage.appendChild(doc.createTextNode (fIntf.getErrorMessage()));
		file.appendChild(errormessage);
	    }
	    root.appendChild(file);
	}
	doc.appendChild(root);
	//default encoding is UTF-8
	OutputFormat format = new OutputFormat(doc);
	format.setIndenting(true);
	XMLSerializer serializer = new XMLSerializer(format);
	if(!fileName.equals("")) {
	    PrintWriter pw = new PrintWriter(new FileOutputStream(fileName.trim()));
	    serializer.setOutputCharStream(pw);
	    serializer.setOutputFormat(format);
	    serializer.serialize(doc);
	    pw.close();
	    StringWriter sw = new StringWriter();
	    serializer.setOutputCharStream(sw);
	    serializer.setOutputFormat(format);
	    serializer.serialize(doc);
	    if(_debug) {
		util.printMessage("\n========================================================",logger,silent); 
		util.printMessage(sw.toString(),logger,this.silent);
		util.printMessage("==========================================================",logger,silent); 
	    }
	    //ShowException.showMessageDialog(_parent.getFrame(),"Report saved.");
	    //logger.debug("Report saved.");
	    //util.printMessage("Please see the report saved in file "+fileName,logger,this.silent);	
	} else {
	    StringWriter sw = new StringWriter();
	    serializer.setOutputCharStream(sw);
	    serializer.setOutputFormat(format);
	    serializer.serialize(doc);
	    if(_debug) {
		util.printMessage("\n========================================================",logger,silent); 
		util.printMessage(sw.toString(),logger,silent);
		util.printMessage("==========================================================",logger,silent); 
	    }
	}
	reportSaved=true;

	SRMClientN.logMsg("ReportSaved pIntf="+pIntf, tag, null);

	if(pIntf == null) {
	    int code = util.mapStatusCode(resultStatus);
	    
	    //SRMClientN.doExit(tag, code);
	}
	/* dont use this save config part, we are using the srm-wsg.rc config file
	   instead.
	   Util util = new Util();
	   util.setThreadCallBack(_parent.getConfigFileLocation(), _parent.getProperties());
	*/
    }catch(Exception e) {
	util.printEventLogException(_theLogger,"",e);
	//ShowException.logDebugMessage(logger,e);
	ShowException.showMessageDialog(_parent.getFrame(), "Exception : " + e.getMessage());					
	SRMClientN.logMsg("Exception:"+e.getMessage(), tag, null);

	util.printHException(e,pIntf);	
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// processTransferAction
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void processTransferAction () {
    String tag = "ProcessTransferAction";
    SRMClientN.logMsg("start isCancel="+isCancel, tag, null);
    if(isCancel) {
	ShowException.showMessageDialog(_parent.getFrame(),"Cannot perform transfer files, request is cancelled");	
	SRMClientN.logMsg("Cannot perform transfer files, request is cancelled", tag, null);    
    } else {
	try { 
	    Object[] objArray = fileInfo.toArray();
	    SRMClientN.logMsg(" arraySize="+objArray.length, tag, null);
	    SRMClientN.logMsg(" tThread="+tThread, tag, null);
	    for(int i = 0; i < objArray.length; i++) {
		FileIntf fIntf = (FileIntf) objArray[i];
		fIntf.addListeners(this);
		if(requestMode.getModeType().equalsIgnoreCase("Get")) {
		    fIntf.setTargetDir(targetDir);
		}
	    }
	    
	    tThread.start();
	    _parent.validateFrame();
	}catch(Exception e) {
	    util.printEventLogException(_theLogger,"",e);
	    //ShowException.logDebugMessage(logger,e);
	    ShowException.showMessageDialog(_parent.getFrame(), "Exception : " + e.getMessage());
					    
	    SRMClientN.logMsg("Exception:"+e.getMessage(), tag, "\nSRM-CLIENT");
	}
    }
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isReportSaved
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isReportSaved () {
    return reportSaved;
}

public void srmFileFailure(int idx, String message) {
    fileFailed(idx,message);
}

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// FileEventListener method
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void fileActive (int idx, String size) {
  inputVec = new Vector();
  inputVec.addElement("index="+idx+"size="+size);
  util.printEventLog(_theLogger,"FileActive",inputVec,silent,useLog);
  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
  fIntf.setStatusLabel("Active");
  String source = fIntf.getSURL();
  if(useLog) {
   try {
     MyGlobusURL fromUrl = new MyGlobusURL(source,0);
     inputVec = new Vector();
     inputVec.addElement("source="+source);
     inputVec.addElement("FromURL="+fromUrl.getPath());
     util.printEventLog(_theLogger,"FileActive",inputVec,silent,useLog);
   }catch(Exception e) { }
  }
  
  if(fIntf.getIsDirectGsiFTP()) {
    if(!size.equals("")) {
       fIntf.setExpectedSize(size);
       fIntf.setActualSize(size);
    }
  }
  else {
    //added on 11/19/10 for tanya's hadoop actualfilesize "0" case.
    if(!size.equals("")) {
       fIntf.setActualSize(size);
    }
    inputVec = new Vector();
    inputVec.addElement("index="+idx+"size="+size);
    inputVec.addElement("actualsize="+fIntf.getActualSize());
    util.printEventLog(_theLogger,"FileActive",inputVec,silent,useLog);
  }
  fIntf.setStartTime(System.currentTimeMillis());
}

public void fileCompleted (int idx, String size) {

  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 

  inputVec = new Vector();
  inputVec.addElement("index="+idx+"size="+size);
  inputVec.addElement(" isDirectGsiFTP="+fIntf.getIsDirectGsiFTP());
  inputVec.addElement(" actualsizeknown="+fIntf.getActualFileSizeKnown());
  inputVec.addElement(" actualfilesizesetalready="+
        fIntf.getActualFileSizeSetAlready());
  inputVec.addElement(" expectedSize="+fIntf.getExpectedSize());
  util.printEventLog(_theLogger,"FileCompleted",inputVec,silent,useLog);


  fIntf.setStatusLabel("Done");

  if(!fIntf.getCompleted()) {
    util.printMessage
		("\nSRM-CLIENT: " + new Date() + 
        " end file transfer for "+fIntf.getOrigSURL(),logger,silent);
    util.printMessage
		("\nSRM-CLIENT: " + new Date() + " end file transfer.",pIntf);
    if((requestMode.getModeType().equalsIgnoreCase("Put")) ||
		(requestMode.getModeType().equalsIgnoreCase("3partycopy"))) {
      if(!fIntf.getIsDirectGsiFTP()) {
       _parent.putDone(fIntf.getOrigTURL(),fIntf.getRID(),fIntf.getLabel());
        if(requestMode.getModeType().equalsIgnoreCase("3partycopy")) {
          if(!fIntf.getDoNotReleaseFile()) {
            _parent.releaseFile(fIntf.getOrigSURL(),fIntf.getGetRID(),idx);
          }
        }
        else {
          _parent.releaseFile(fIntf.getOrigTURL(),fIntf.getRID(),idx);
        }
      }
    }
    else {
      if(!fIntf.getDoNotReleaseFile()) {
        _parent.releaseFile(fIntf.getOrigSURL(), fIntf.getRID(),idx);
      } 
    }
    if(!fIntf.getIsDirectGsiFTP()) {
      if(size.equals("") || size.equals("0")) {
        if(fIntf.getActualFileSizeKnown()) {
          if(!fIntf.getActualFileSizeSetAlready()) 
             fIntf.setActualSize(size);
        }
        else {
          fIntf.setActualSize(fIntf.getExpectedSize());
        }
      }
      else {
        fIntf.setActualSize(size);
      }
    }
    else {
      //added recently to see whether direct file transfer gets the file size
      //jan 16,08
      if(size.equals("") || size.equals("0")) {
        if(fIntf.getActualFileSizeKnown()) {
          if(fIntf.getActualSize().equals("")) {
            fIntf.setActualSize(size);
          }
        }  
        else {
          fIntf.setActualSize(fIntf.getExpectedSize());
        }
      }
      else {
        fIntf.setActualSize(size);
      }
    }

    if(!fIntf.getPutDoneFailed()) { 
	fIntf.setCompleted(true);
	completedFiles++;
	_parent.incrementCompletedProcess();
    }
    else {
      //completedFiles++;
      _parent.incrementCompletedProcess();
      fIntf.setFailed(true);
    }
    fIntf.setTimeStamp(new Date());
    fIntf.setEndTime(System.currentTimeMillis());
  }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::
// used for the -nodownload option
//:::::::::::::::::::::::::::::::::::::::::::::::::::::

public void incrementCompletedFiles() {
   completedFiles++;
}

//this method is called during srmFileFailure
//this method is different from the below one, 
//we don't increment file completed process in this method

public void fileFailed (int idx, String message) {
  inputVec = new Vector();
  inputVec.addElement("index="+idx+"Message="+message);
  util.printEventLog(_theLogger,"FileFailed",inputVec,silent,useLog);
  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
  if(fIntf.getDuplicationError()) {
    completedFiles++;
    fIntf.setCompleted(true);
  }
  else if(!fIntf.getFailed()) {
    fIntf.setStatusLabel("Failed");
    fIntf.setFailed(true);
    fIntf.setErrorMessage(message);
    errorFiles++;
  }
}
 
public void fileFailed (int idx) {
  //logger.debug("Event fileFailed " + idx);
  inputVec = new Vector();
  inputVec.addElement("index="+idx);
  util.printEventLog(_theLogger,"FileFailed",inputVec,silent,useLog);
  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
  fIntf.setStatusLabel("Failed");
  if(!fIntf.getFailed()) {
    fIntf.setFailed(true);
    _parent.incrementCompletedProcess();
  }
  errorFiles++;
  if((requestMode.getModeType().equalsIgnoreCase("Put")) ||
		(requestMode.getModeType().equalsIgnoreCase("3partycopy"))) {
     if(fIntf.getFileFailedAfterAllowedRetries()) {
       if(!fIntf.isAbortFilesCalled()) {
         fIntf.setAbortFilesCalled(true);
         _parent.abortFiles(fIntf.getOrigTURL(),fIntf.getRID(),fIntf.getLabel());
         if(requestMode.getModeType().equalsIgnoreCase("3partycopy")) {
           _parent.releaseFile(fIntf.getOrigSURL(),fIntf.getGetRID(),idx);
         }//end 3partycopy
       }
     }
     //}
  } 
}

public void fileSkipped (int idx) {
  //logger.debug("Event fileSkipped" + idx);
  inputVec = new Vector();
  inputVec.addElement("index="+idx);
  util.printEventLog(_theLogger,"FileSkipped",inputVec,silent,useLog);
}

public void filePending(int idx) {
  //logger.debug("Event filePending " + idx);
  inputVec = new Vector();
  inputVec.addElement("index="+idx);
  util.printEventLog(_theLogger,"FilePending",inputVec,silent,useLog);
}

public void fileExists(int idx) {
  //logger.debug("Event fileExists " + idx);
  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
  fIntf.setStatusLabel("Exists");
  inputVec = new Vector();
  inputVec.addElement("index="+idx);
  util.printEventLog(_theLogger,"FileExists",inputVec,silent,useLog);
  existsFiles++;
}

public void requestCancelled(int idx) {
  //logger.debug("Event requestCancelled " + idx);
  inputVec = new Vector();
  inputVec.addElement("index="+idx);
  util.printEventLog(_theLogger,"RequestCancelled",inputVec,silent,useLog);
}

public void  setTimeTaken(int idx, String value) {
    //logger.debug("Event setTimeTaken " + idx);
    FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
    fIntf.setTimeTaken(value);
    inputVec = new Vector();
    inputVec.addElement("index="+idx+"value="+value);
    util.printEventLog(_theLogger,"SetTimeTaken",inputVec,silent,useLog);
}

public void  setErrorMessage(int idx, String value) {
  //logger.debug("Event setErrorMessage " + idx);
  FileIntf fIntf = (FileIntf) fileInfo.elementAt(idx); 
  fIntf.setErrorMessage(value);
  inputVec = new Vector();
  inputVec.addElement("index="+idx+"value="+value);
  util.printEventLog(_theLogger,"SetErrorMessage",inputVec,silent,useLog);
}
}
