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


package tester;

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.client.intf.PrintIntf;
 
import gov.lbl.srm.client.main.MySRMFileTransfer;


public class TimeOutCallBack extends Thread {
   private Object obj;
   private int startTimeStamp;
   private String requestToken;
   private ISRM _srm;
   private boolean interruptNow=false;
   private boolean useDriver;
   private OperOk isPingOverAllSuccess;
   private SharedObjectLock sLock;

 public TimeOutCallBack (Object obj, int startTimeStamp) {
    this.obj = obj;
    this.startTimeStamp = startTimeStamp;
 }

 public void setObject(Object obj) {
    this.obj = obj;
 }

 public void setSharedObjectLock(SharedObjectLock sLock) {
    this.sLock = sLock;
 }

 public void setRequestToken(String rToken, ISRM srm) {
    requestToken = rToken;
    _srm = srm;
 }

 public void setDriverInfo(boolean useDriver, OperOk isPingOverAllSuccess) {
   this.useDriver = useDriver;
   this.isPingOverAllSuccess = isPingOverAllSuccess;
 }

 public void interruptNow(boolean b) {
   interruptNow = b;
 }

 public void run () {  
   try {
     while(true) {
       sleep(1000); 
       if(obj != null || interruptNow) { 
	     interrupt(); 
		 return;
       }
       else {
         boolean b = util.isConnectionTimeOut(startTimeStamp);
         //System.out.println("StartTimeStamp="+startTimeStamp + " " + b);
         if(b) {
           if(requestToken != null) {
             System.out.println("Going to call abort request now...");
             SrmAbortRequestRequest r = new SrmAbortRequestRequest ();
             r.setRequestToken(requestToken);
             SrmAbortRequestResponse result = null;
 
             int sTimeStamp = util.startTimeStamp();
             TimeOutCallBack timeOutCallBack = new TimeOutCallBack(result,sTimeStamp);
             timeOutCallBack.start();
 
             result = _srm.srmAbortRequest(r);
             timeOutCallBack.setObject(result);
           }
           if(!useDriver) {
             System.out.println(
				"Server did not respond for this interface");
             interrupt();
             if(sLock != null) 
               sLock.setIncrementCount();
             return;
             //System.exit(95);
           }
           else {
             System.out.println("Server did not respond for this interface.");
             interrupt();
             if(sLock != null) 
               sLock.setIncrementCount();
             return;
              //isPingOverAllSuccess.isOperOk(false); //so, all the other operations is skipped 
           }
         }
       }
     }//end while
   }catch(Exception e) { 
     System.out.println("Exception="+e.getMessage());
   }
 }

}
