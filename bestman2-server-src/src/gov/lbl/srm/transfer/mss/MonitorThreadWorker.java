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

package gov.lbl.srm.transfer.mss;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// MonitorThreadWorker
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.io.*;
import java.util.Vector;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import gov.lbl.srm.transfer.mss.intf.*;

public class MonitorThreadWorker 
{

private String _name;
private ObjectFIFO idleWorkers;

private Thread internalThread;
private volatile boolean noStopRequested;
private SRM_MSS _parent;
private Hashtable statusMap;
private static Log logger;
private int _debugLevel;
private java.util.logging.Logger _theLogger;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// Constructor
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public MonitorThreadWorker (ObjectFIFO idleWorkers, String name, 
		SRM_MSS parent)
{
   this.idleWorkers = idleWorkers;
   _name = name;
   _parent = parent;
   logger = _parent.getLogger();
   _theLogger = _parent.getJLogger();
   _debugLevel = _parent.getDebugLevel();

   //just before returning, the thread should be created.
   noStopRequested = true;

   Runnable r = new Runnable() {
     public void run () {
        try {
          runWork ();
         }catch(Exception ex) {
           util.printMessage(ex.getMessage());
           util.printStackTrace(ex,logger);
         }
     }
   };

   internalThread = new Thread (r);
   internalThread.start();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  process
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void process (Hashtable statusMap) 
	throws InterruptedException {
   this.statusMap = statusMap;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  runWork
//  never make this as synchronized (it never gets call)
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


private void runWork () {

 while(noStopRequested) {
  try {
   idleWorkers.add(this);
    if(statusMap != null) {
       /*
       if(_debugLevel >= 4000) {
          System.out.println(">>>StatusMap.size()="+statusMap.size());
       }
       */ 
       if(statusMap.size() == 0) {
        util.printMessage("No Active files to monitor",logger,_debugLevel);
       }
       while(statusMap.size() > 0) {
         Collection c = statusMap.values();
         Object[] oobj = c.toArray();
         internalThread.sleep(1000);  //wait for one sec. before proceed
         FileObj fObj = null;
         for(int i = 0; i < oobj.length; i++) {
           fObj = (FileObj) oobj[i];
           MSS_MESSAGE pftpmssg = fObj.getPFTPStatus(); 
           /*
           if(_debugLevel >= 4000) {
             System.out.println(">>>>ThreadWorker="+pftpmssg);
             System.out.println(">>>>fObj="+fObj);
             System.out.println(">>>>fObj.getRequestToken="+
					fObj.getRequestToken());
           }
           */
           if(pftpmssg != null && 
		      pftpmssg != MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED) {
              /* 
              if(_debugLevel >= 4000) {
                System.out.println(">>>>>>> Calling decreasePending");
              }
              */
             _parent.decreaseMSSPending();
              /*
             if(_debugLevel >= 4000) {
               System.out.println(">>>fObj.getAlreadyReported="+
					fObj.getAlreadyReported());
             } 
              */
             if(!fObj.getAlreadyReported()) {
               if(pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_DONE ||
					pftpmssg == MSS_MESSAGE.SRM_MSS_REQUEST_DONE) {
                  Object[] param = new Object[2];
                  param[0] = "SOURCE="+fObj;
                  param[1] ="REQUEST-ID="+fObj.getRequestToken();
                  _theLogger.log(java.util.logging.Level.FINE,
				   "TRANSFER_DONE",(Object[])param);
                  //System.out.println(">>>Calling remove from statusMap");
                  _parent.removeFromStatusMap(fObj); 
                }
                else if (pftpmssg == MSS_MESSAGE.SRM_MSS_TRANSFER_ABORTED) {
                   ;
                }
                else {
                  Object[] param = new Object[4];
                  param[0] = "SOURCE="+fObj;
                  param[1] = "STATUS="+pftpmssg.toString();
                  param[2] = "EXPLANATION="+pftpmssg.toString();
                  param[3] ="REQUEST-ID="+fObj.getRequestToken();
                  _theLogger.log(java.util.logging.Level.FINE,
					"TRANSFER_FAILED",(Object[])param);
                  //System.out.println(">>>Calling remove from statusMap");
                  _parent.removeFromStatusMap(fObj); 
                }
                fObj.setAlreadyReported(true); 
              }
              else {
                  Object[] param = new Object[3];
                  param[0] = "SOURCE="+fObj;
                  param[1] ="REQUEST-ID="+fObj.getRequestToken();
                  param[2] ="STATUS="+pftpmssg.toString();
                  _theLogger.log(java.util.logging.Level.FINE,
				   "ALREADY-REPORTED-STATUS-FOR-THIS-REQUEST",
				     (Object[])param);
                  //System.out.println(">>>Calling remove from statusMap");
                  _parent.removeFromStatusMap(fObj); 
             } 
           } 
           else {
             if(_debugLevel >= 300) { 
               Object[] param = new Object[2];
               param[0] ="REQUEST-ID="+fObj.getRequestToken();
               param[1] ="STATUS="+pftpmssg;
               _theLogger.log(java.util.logging.Level.FINE,
			     "REQUEST-QUEUED",(Object[])param);
             }
           }
         }

         //System.out.println(">>> isNextTransferAvailable()="+_parent.isNextTransferAvailable()); 
         //System.out.println(">>> _parent.getMSSPending()="+_parent.getMSSPending()); 
         //System.out.println(">>> _parent.getMSSMaxAllowed()="+_parent.getMSSMaxAllowed()); 
         if(_parent.isNextTransferAvailable()) {
           //if(_parent.getMSSPending() <= _parent.getMSSMaxAllowed()){
             try {
              //try this part.
              if(fObj != null && !fObj.getTapeId().equals("")) {
                if(_parent.getPreviousTapeId().equals("")) {
                   _parent.setPreviousTapeId(fObj.getTapeId());
                }
              }
              boolean ok = false;
              FileObj ffobj = _parent.getFromQueue();
              //System.out.println("GOT FROM QUEUE="+ffobj);
              if(ffobj != null) {
               Object[] param = new Object [2];
               param[0] = ffobj.toString();
               param[1] = "RID="+ffobj.getRequestToken();
               _theLogger.log(java.util.logging.Level.FINE, 
				"GOT_FROM_QUEUE", (Object[]) param);
               //don't worry about retry timeout now
               if(_debugLevel >= 100) {
                 util.printMessage("Next transfer available " + 
					ffobj.getRequestToken(), logger,_debugLevel);
               }   
               if(ffobj.getNumRetry() <= _parent.getMSSMaxRetrial()) {
                 if(_debugLevel >= 1000 && !ffobj.getTapeId().equals("")) {
                   util.printMessage("\n\n>>>>>>", logger);
                   util.printMessage("MW next File " + ffobj, logger);
				   util.printMessage("TapeId "+ffobj.getTapeId(), logger);
                   util.printMessage(">>>>>>\n\n", logger);
                  }
                  if(_debugLevel >= 1000 && !ffobj.getTapeId().equals("")) {
                   util.printMessage("\n\n>>>>>>", logger);
                   util.printMessage ("Before setting Previous TapeId" + 
	  	              _parent.getPreviousTapeId(), logger);
                   _parent.setPreviousTapeId(ffobj.getTapeId());
                   util.printMessage ("After Setting Previous TapeId "+ 
					   ffobj.getTapeId(), logger);
                   util.printMessage(">>>>>>\n\n", logger);
                  }
                  //adding next available transfer to statusMap
                  ffobj.setAlreadyReported(false);
                  //System.out.println(">>>Added to StatusMap queue");
                  _parent.addToStatusMap(ffobj);
                  param = new Object [2];
                  param[0] = ffobj.toString();
                  param[1] = "RID="+ffobj.getRequestToken();
                  _theLogger.log(java.util.logging.Level.FINE, 
					"SERVING_REQUEST", (Object[]) param);
                  ThreadCallBack taskThread = ffobj.getTaskThread();
                  taskThread.start();
                 }
               }
               else {
                  Object[] param = new Object[1];
                  param[0] = "fobj is null";
                  _theLogger.log(java.util.logging.Level.FINE, 
				  "GOT_NULL_FROM_QUEUE", (Object[]) param);
                  util.printMessage("+++++++++++++++++++++",logger);
                  util.printMessage("Number of max retries attempted", logger);
                  util.printMessage("+++++++++++++++++++++",logger);
               }
            }catch(Exception e) {
              util.printMessage("Excepiton in MonitorThreadWorker " + e.getMessage());
            }
          //}
         }//next transfer available
       }//end while 

       if(!_parent.isNextTransferAvailable()) {
          if(_debugLevel >= 300){ 
            util.printMessage("\nNext transfer not available", logger,_debugLevel);
          }
          util.printMessage("\nDisabling monitor thread", logger,_debugLevel);
          _parent.setMonitorThreadDisable(true);
       }
    }
  }catch(InterruptedException ex) {
    util.printMessage(ex.getMessage());
    util.printStackTrace(ex,logger);
    Thread.currentThread().interrupt (); //re-assert
  }
 }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//  stopRequest
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public void stopRequest () {
   noStopRequested = false;
   //commented for the InterruptedIOException comes in the solaris platform
   //with the log4j logging. it does not happen in linux though.
   //internalThread.interrupt ();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isAlive
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public boolean isAlive () {
   return internalThread.isAlive ();
}

}
