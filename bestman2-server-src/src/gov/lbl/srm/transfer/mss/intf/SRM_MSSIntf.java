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

package gov.lbl.srm.transfer.mss.intf;

import gov.lbl.srm.transfer.mss.*;

import java.util.HashMap; // added by junmin 2007-11-29

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//NOTE: For implementing this interface.
//
//1)  All subclass needs to implement this interface.
//
//2)  If the subclass does not need any scheduling mechanism, then
//    subclass need not have to extends SRM_MSS super class
//    In order to include logging mechanism, they have to
//    do their logging setup in their own class.
//    Also, please add your class name in the logs/log4j_srmmss.properties 
//    log4j configuration file.
//    please see example SRM_MSS_LSTORE.java
//
//3)  If any subclass needs to use the schedule mechanism, should also 
//    implement the interface mssIntf and extends SRM_MSS super class
//    The super class has all those setup already defined for logging. 
//    the subclass need not have to worry about setting up logging and 
//    detailed logging.
//    please see the example SRM_MSS_HPSS.java and SRM_MSS_NCAR.java
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public interface SRM_MSSIntf {

  public void init(String confFile) throws SRM_MSS_Exception, Exception;

  public SRM_PING_STATUS srmPing() throws SRM_MSS_Exception, Exception;

  public SRM_MSSFILE_STATUS mssFileGet (String source, String target, 
		  long fileSize, SRM_OVERWRITE_MODE  overwritemode,
          boolean srmnocipher, boolean srmnonpassivelisting,
          SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception; 

  public SRM_MSSFILE_STATUS mssFilePut (String source, String target, 
		  long fileSize, SRM_OVERWRITE_MODE overwritemode,
          boolean srmnocipher, boolean srmnonpassivelisting,
          SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception;

  public SRM_STATUS  mssMakeDirectory (SRM_PATH dirs, boolean srmnocipher,
		  boolean srmnonpassivelisting, SRM_ACCESS_INFO accessInfo) 
			throws SRM_MSS_Exception, Exception;

  public boolean abortRequest (String rid) throws Exception;

  public SRM_PATH srmLs ( String path, SRM_ACCESS_INFO accessInfo, 
          boolean recursive, boolean srmnocipher, 
          boolean srmnonpassivelisting) throws SRM_MSS_Exception, Exception;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//NOTE : Please see the format that is followed for ls listing.
//       There are three examples, 1) path is just a plain file,
//       2) path is a dir and recursive is false. 
//       3) path is a dir and recursive is true.
// Please see the implementation of SRM_MSS_HPSS and SRM_MSS_NCAR for
// sample.
//
//Example 1: 
//   If recursive is not set and the given path is just a plain file.
//   example : path=/tmp/msstest/readme1
//   The return format will be 
//     path.getDir()=/tmp/msstest
//     path.isDir()=false
//     path.getFids() will contain a vector of SRM_FILE objects in it.
//        File=/tmp/msstest/readme1
//        Size=<size of the readme1 in bytes> (like unix ls output)  
//        timestamp=<timestamp of the readme1> (like unix ls output)
//
//Example 2: 
//   If recursive is not set and the given path is a directory.
//   example : path=/tmp/msstest
//   and assuming the dir /tmp/msstest has one file readme1
//   and one subdir called testdir
//   The return format will be 
//     path.getDir()=/tmp/msstest
//     path.isDir()=true
//     path.getFids() will contain a vector of SRM_FILE objects in it and
//     it lists the the file and the subdir init.
//        File=/tmp/msstest/readme1
//        Size=<size of the readme1 in bytes> (like unix ls output)  
//        timestamp=<timestamp of the readme1> (like unix ls output)
//
//        File=/tmp/msstest/testdir
//        Size=<size of the testdir in bytes> (like unix ls output)  
//        timestamp=<timestamp of the testdir> (like unix ls output)
//
//Example 3: 
//   If recursive is set and the given path is a directory.
//   example : path=/tmp/msstest
//   and assuming the dir /tmp/msstest has one file readme1
//   and one subdir called testdir and testdir has one file called readme2
//   The return format will be 
//     path.getDir()=/tmp/msstest
//     path.isDir()=true
//     path.getFids() will contain a vector of SRM_FILE objects in it and
//     it lists the the file. 
//        File=/tmp/msstest/readme1
//        Size=<size of the readme1 in bytes> (like unix ls output)  
//        timestamp=<timestamp of the readme1> (like unix ls output)
//  
//     path.getsubPath() will contain a vector of SRM_PATH objects in it
//        subPath.getDir()=/tmp/msstest/testdir
//        subPath.isDir()=true
//        subPath.getFids() will contain a vector of SRM_FILE objects in it
//          File=/tmp/msstest/testdir/readme2
//          Size=<size of the readme2 in bytes> (like unix ls output)  
//          timestamp=<timestamp of the readme2> (like unix ls output)
//
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

  public SRM_STATUS srmCopy ( String sourcePath, String targetPath, 
          SRM_OVERWRITE_MODE  overwritemode, boolean recursive,
          boolean srmnoncipher, boolean srmnonpassivelisting,
          SRM_ACCESS_INFO accessInfo) throws SRM_MSS_Exception, Exception ;
  
  public SRM_STATUS srmDelete ( String path, SRM_ACCESS_INFO accessInfo, 
          boolean isDir, boolean recursive, boolean srmnoncipher, 
		  boolean srmnonpassivelisting) throws SRM_MSS_Exception, Exception ;
        
  public SRM_FILE srmGetFileSize (String fPath, 
	SRM_ACCESS_INFO accessInfo, boolean srmnocipher, 
	boolean srmnonpassivelisting)
   throws SRM_MSS_Exception, Exception;

  public SRM_STATUS mssGetHomeDir (SRM_ACCESS_INFO accessInfo) 
	throws SRM_MSS_Exception, Exception ;

  public Object checkStatus(String requestId) throws Exception;

    /*public SRM_ACCESS_INFO generateAccessInfo (String userId, 
		String login, String passwd) throws Exception;
    */
    // changed by junmin 11/30/2007
    public SRM_ACCESS_INFO generateAccessInfo(HashMap input) throws Exception;
}
