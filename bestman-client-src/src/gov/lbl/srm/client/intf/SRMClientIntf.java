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

package gov.lbl.srm.client.intf;

import gov.lbl.srm.client.*;
import javax.swing.JFrame;
import java.util.*;

import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

import gov.lbl.srm.client.exception.*;
import gov.lbl.srm.client.main.*;

public interface SRMClientIntf {

   public void enableTransferButton(boolean b, boolean ok);
   public boolean isConfigExists();
   public void showUsage (boolean b);
   public void setRequestTimedOut(boolean b);
   public boolean getRequestTimedOut();
   public void setRequestDone(boolean b, boolean allFilesFailed);
   public void setRequestInformation(String resultStatus, String resultExplanation);
   public String getRequestStatus();
   public String getRequestExplanation();
   public String getConfigFileLocation();
   public Properties getProperties(); 
   public void setConfig(XMLParseConfig config);
   public void setPassword(String str);
   public String getPassword();
   public GSSCredential getCredential() throws SRMClientException;
   public GSSCredential checkTimeLeft() throws SRMClientException;
   public GSSCredential createProxy(String passwd) throws SRMClientException;
   public boolean isRenewProxy();
   public void validateFrame();
   public void setRequest(Request req);
   public JFrame getFrame(); 
   public boolean getLock();
   public void initiatePullingFile(FileInfo fInfo);
   public void incrementCompletedProcess();
   public boolean isUrlExists();
   public Vector getSrmFirstUrl(boolean b);
   public void numberOfSpaceAvailableFiles(int x);
   public void setTotalFiles (int tFiles);
   public void putDone(String siteUrl, String rid, int label);
   public void abortFiles(String siteUrl, String rid, int label);
   public void releaseFile(String siteUrl, String rid, int label);
   public void srmFileFailure(int idx, String message);
   public int getMaximumFilesPerRequest();
   public void setTotalFilesPerRequest(int maxSize);
   public int getTotalFilesPerRequest();
   public void setTotalSubRequest(int maxSize);
   public void setGateWayModeEnabled(boolean b);
   public int getTotalSubRequest();
   public void addRequestToken(String rid);
   public Vector getRequestToken();
}
