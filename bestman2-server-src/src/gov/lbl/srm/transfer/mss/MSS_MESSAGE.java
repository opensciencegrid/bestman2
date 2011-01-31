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

public class MSS_MESSAGE
{
   private static MSS_MESSAGE [] values_ = new MSS_MESSAGE[29];
   private int value_;

   public final static int  _SRM_MSS_UNKNOWN_ERROR = 0;
   public final static MSS_MESSAGE  SRM_MSS_UNKNOWN_ERROR = 
	new MSS_MESSAGE(_SRM_MSS_UNKNOWN_ERROR);

   public final static int  _SRM_MSS_CONNECTION_OK = 1;
   public final static MSS_MESSAGE  SRM_MSS_CONNECTION_OK = 
	new MSS_MESSAGE(_SRM_MSS_CONNECTION_OK);

   public final static int  _SRM_MSS_TRANSFER_DONE = 2;
   public final static MSS_MESSAGE  SRM_MSS_TRANSFER_DONE = 
	new MSS_MESSAGE(_SRM_MSS_TRANSFER_DONE);

   public final static int  _SRM_MSS_REQUEST_DONE = 3;
   public final static MSS_MESSAGE  SRM_MSS_REQUEST_DONE = 
	new MSS_MESSAGE(_SRM_MSS_REQUEST_DONE);

   public final static int  _SRM_MSS_QUEUE_BUSY = 4;
   public final static MSS_MESSAGE  SRM_MSS_QUEUE_BUSY = 
	new MSS_MESSAGE(_SRM_MSS_QUEUE_BUSY);

   public final static int  _SRM_MSS_FILE_EXISTS = 5;
   public final static MSS_MESSAGE  SRM_MSS_FILE_EXISTS = 
	new MSS_MESSAGE(_SRM_MSS_FILE_EXISTS);

   public final static int  _SRM_MSS_NO_SUCH_PATH = 6;
   public final static MSS_MESSAGE  SRM_MSS_NO_SUCH_PATH = 
	new MSS_MESSAGE(_SRM_MSS_NO_SUCH_PATH);

   public final static int  _SRM_MSS_NO_SUCH_LOCAL_PATH = 7;
   public final static MSS_MESSAGE  SRM_MSS_NO_SUCH_LOCAL_PATH = 
	new MSS_MESSAGE(_SRM_MSS_NO_SUCH_LOCAL_PATH);

   public final static int  _SRM_MSS_AUTHORIZATION_FAILED = 8;
   public final static MSS_MESSAGE  SRM_MSS_AUTHORIZATION_FAILED = 
	new MSS_MESSAGE(_SRM_MSS_AUTHORIZATION_FAILED);

   public final static int  _SRM_MSS_MSS_LIMIT_REACHED = 9;
   public final static MSS_MESSAGE  SRM_MSS_MSS_LIMIT_REACHED = 
	new MSS_MESSAGE(_SRM_MSS_MSS_LIMIT_REACHED);

   public final static int  _SRM_MSS_MSS_ERROR = 10;
   public final static MSS_MESSAGE  SRM_MSS_MSS_ERROR = 
	new MSS_MESSAGE(_SRM_MSS_MSS_ERROR);

   public final static int  _SRM_MSS_MSS_NOT_AVAILABLE = 11;
   public final static MSS_MESSAGE  SRM_MSS_MSS_NOT_AVAILABLE = 
	new MSS_MESSAGE(_SRM_MSS_MSS_NOT_AVAILABLE);

   public final static int  _SRM_MSS_AUTHENTICATION_FAILED = 12;
   public final static MSS_MESSAGE  SRM_MSS_AUTHENTICATION_FAILED = 
	new MSS_MESSAGE(_SRM_MSS_AUTHENTICATION_FAILED);

   public final static int  _SRM_MSS_FAILED = 13;
   public final static MSS_MESSAGE  SRM_MSS_FAILED = 
	new MSS_MESSAGE(_SRM_MSS_FAILED);

   public final static int  _SRM_MSS_NOT_INITIALIZED = 14;
   public final static MSS_MESSAGE  SRM_MSS_NOT_INITIALIZED = 
	new MSS_MESSAGE(_SRM_MSS_NOT_INITIALIZED);

   public final static int  _SRM_MSS_REQUEST_QUEUED = 15;
   public final static MSS_MESSAGE  SRM_MSS_REQUEST_QUEUED = 
	new MSS_MESSAGE(_SRM_MSS_REQUEST_QUEUED);

   public final static int  _SRM_MSS_REQUEST_FAILED = 16;
   public final static MSS_MESSAGE  SRM_MSS_REQUEST_FAILED = 
	new MSS_MESSAGE(_SRM_MSS_REQUEST_FAILED);

   public final static int  _SRM_MSS_IS_A_DIRECTORY = 17;
   public final static MSS_MESSAGE  SRM_MSS_IS_A_DIRECTORY = 
	new MSS_MESSAGE(_SRM_MSS_IS_A_DIRECTORY);

   public final static int  _SRM_MSS_IS_NOT_A_DIRECTORY = 18;
   public final static MSS_MESSAGE  SRM_MSS_IS_NOT_A_DIRECTORY = 
	new MSS_MESSAGE(_SRM_MSS_IS_NOT_A_DIRECTORY);

   public final static int  _SRM_MSS_BAD_PROXY = 19;
   public final static MSS_MESSAGE  SRM_MSS_BAD_PROXY = 
	new MSS_MESSAGE(_SRM_MSS_BAD_PROXY);

   public final static int  _SRM_MSS_TRANSFER_RETRY = 20;
   public final static MSS_MESSAGE  SRM_MSS_TRANSFER_RETRY = 
	new MSS_MESSAGE(_SRM_MSS_TRANSFER_RETRY);

   public final static int  _SRM_MSS_TRANSFER_ABORTED = 21;
   public final static MSS_MESSAGE  SRM_MSS_TRANSFER_ABORTED = 
	new MSS_MESSAGE(_SRM_MSS_TRANSFER_ABORTED);

   public final static int  _SRM_MSS_BAD_NCAR_PROJECT_NUMBER = 22;
   public final static MSS_MESSAGE  SRM_MSS_BAD_NCAR_PROJECT_NUMBER = 
	new MSS_MESSAGE(_SRM_MSS_BAD_NCAR_PROJECT_NUMBER);

   public final static int  _SRM_MSS_BAD_NCAR_SCI = 23;
   public final static MSS_MESSAGE  SRM_MSS_BAD_NCAR_SCI = 
	new MSS_MESSAGE(_SRM_MSS_BAD_NCAR_SCI);

   public final static int  _SRM_MSS_STATUS_UP = 24;
   public final static MSS_MESSAGE  SRM_MSS_STATUS_UP = 
	new MSS_MESSAGE(_SRM_MSS_STATUS_UP);

   public final static int  _SRM_MSS_STATUS_DOWN = 25;
   public final static MSS_MESSAGE  SRM_MSS_STATUS_DOWN = 
	new MSS_MESSAGE(_SRM_MSS_STATUS_DOWN);

   public final static int  _SRM_MSS_STATUS_UNKNOWN = 26;
   public final static MSS_MESSAGE  SRM_MSS_STATUS_UNKNOWN = 
	new MSS_MESSAGE(_SRM_MSS_STATUS_UNKNOWN);

   public final static int  _SRM_MSS_DIRECTORY_NOT_EMPTY = 27;
   public final static MSS_MESSAGE  SRM_MSS_DIRECTORY_NOT_EMPTY = 
	new MSS_MESSAGE(_SRM_MSS_DIRECTORY_NOT_EMPTY);

   public final static int  _SRM_MSS_PROCESS_KILLED = 28;
   public final static MSS_MESSAGE  SRM_MSS_PROCESS_KILLED = 
	new MSS_MESSAGE(_SRM_MSS_PROCESS_KILLED);

   protected 
    MSS_MESSAGE(int value) 
  { 
      values_[value] = this;
      value_ = value;
  }

   public int value() {
     return value_;
   }

   public static MSS_MESSAGE from_int(int value) 
        throws Exception {
        if(value < values_.length) {
           return values_[value];
        }
        else {  
           throw new Exception("BAD PARAM out of range exception");
        }
   }

   public String toString () {
      String result = "";
      switch(value_) {
          case 0 : result= "SRM_MSS_UNKNOWN_ERROR";break; 
          case 1 : result="SRM_MSS_CONNECTION_OK"; break;
          case 2 : result="SRM_MSS_TRANSFER_DONE"; break;
          case 3 : result="SRM_MSS_REQUEST_DONE"; break;
          case 4 : result="SRM_MSS_QUEUE_BUSY"; break;
          case 5 : result="SRM_MSS_FILE_EXISTS"; break;
          case 6 : result="SRM_MSS_NO_SUCH_PATH"; break;
          case 7 : result="SRM_MSS_NO_SUCH_LOCAL_PATH"; break;
          case 8 : result="SRM_MSS_AUTHORIZATION_FAILED"; break;
          case 9 : result="SRM_MSS_MSS_LIMIT_REACHED";break;
          case 10 : result="SRM_MSS_MSS_ERROR";break;
          case 11 : result="SRM_MSS_MSS_NOT_AVAILABLE";break;
          case 12 : result="SRM_MSS_AUTHENTICATION_FAILED";break;
          case 13 : result="SRM_MSS_FAILED";break;
          case 14 : result="SRM_MSS_NOT_INITIALIZED";break;
          case 15 : result="SRM_MSS_REQUEST_QUEUED";break;
          case 16 : result="SRM_MSS_REQUEST_FAILED";break;
          case 17 : result="SRM_MSS_IS_A_DIRECTORY";break;
          case 18 : result="SRM_MSS_IS_NOT_A_DIRECTORY";break;
          case 19 : result="SRM_MSS_BAD_PROXY";break;
          case 20 : result="SRM_MSS_TRANSFER_RETRY";break;
          case 21 : result="SRM_MSS_TRANSFER_ABORTED";break;
          case 22 : result="SRM_MSS_BAD_NCAR_PROJECT_NUMBER";break;
          case 23 : result="SRM_MSS_BAD_NCAR_SCI";break;
          case 24 : result="SRM_MSS_STATUS_UP";break;
          case 25 : result="SRM_MSS_STATUS_DOWN";break;
          case 26 : result="SRM_MSS_STATUS_UNKNOWN";break;
          case 27 : result="SRM_MSS_DIRECTORY_NOT_EMPTY";break;
          case 28 : result="SRM_MSS_PROCESS_KILLED";break;
      }
      return result;
   }
}
