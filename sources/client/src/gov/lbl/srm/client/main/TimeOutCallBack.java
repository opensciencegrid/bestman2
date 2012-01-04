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

import java.io.*;
import java.util.*;
import java.text.*;

import gov.lbl.srm.StorageResourceManager.*;

public class TimeOutCallBack extends Thread {
   private Object obj;
   private long startTimeStamp;
   private int connectionTimeOutAllowed;
   private String requestToken;
   private String callingMethodName;
   private ISRM _srm;
   private boolean timedOut;
   private java.util.logging.Logger _theLogger;
   private boolean silent;
   private boolean useLog;
   private Vector inputVec = new Vector ();

 public TimeOutCallBack (Object obj, long startTimeStamp, 
		int connectionTimeOutAllowed, String callingMethodName) {
    this.obj = obj;
    this.startTimeStamp = startTimeStamp;
    this.connectionTimeOutAllowed = connectionTimeOutAllowed;
    this.callingMethodName = callingMethodName;
 }

 public boolean isTimedOut() {
    return timedOut;
 }

 public void setObject(Object obj) {
    this.obj = obj;
 }

 public void setRequestToken(String rToken, ISRM srm) {
    requestToken = rToken;
    _srm = srm;
 }

 public void setLogger(java.util.logging.Logger theLogger, 
                boolean silent, boolean useLog) {
   this._theLogger = theLogger;
   this.silent=silent;
   this.useLog = useLog;
 }

 public void run () {  
   try {
     while(true) {
       sleep(1000); 
       if(obj != null) { 
         interrupt();
         return;
       }
       else {
        boolean b = 
             util.isConnectionTimeOut(startTimeStamp,connectionTimeOutAllowed);
        //System.out.println(">>>startTimeStamp="+startTimeStamp+ " " +
                //connectionTimeOutAllowed + " " + b);
        if(b) {
          if(requestToken != null) {
            //if(!noAbortFile) {
              System.out.println("Going to call abort request now...");
              SrmAbortRequestRequest r = new SrmAbortRequestRequest ();
              r.setRequestToken(requestToken);
              SrmAbortRequestResponse result = null;
 
              long sTimeStamp = util.startTimeStamp();
              TimeOutCallBack timeOutCallBack = 
				new TimeOutCallBack(result,sTimeStamp,
				connectionTimeOutAllowed,"SrmAbortRequest");
              timeOutCallBack.start();
 
              result = _srm.srmAbortRequest(r);
              timeOutCallBack.setObject(result);
            //}
          }
          if(obj == null) {
           timedOut = true;
           inputVec.clear();
           inputVec.addElement(" Date="+new Date()); 
           inputVec.addElement(" TimedOut="+timedOut); 
           util.printEventLog(_theLogger,"TimeOutCallBack",
                inputVec,silent,useLog);
          }
          interrupt();
          return;
        }
       }
     }
   }catch(Exception e) { 
     System.out.println("Exception="+e.getMessage());
     util.printEventLogException(_theLogger,"TimeOutCallBack.Exception",e);
   }
 }
}
