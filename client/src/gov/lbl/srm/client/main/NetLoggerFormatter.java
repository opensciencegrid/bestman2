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

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.logging.*;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// A custom formatter class to record logs in the netlogger format
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class NetLoggerFormatter extends Formatter {

   private StringBuffer buf = new StringBuffer(1000);
   private StringBuffer timeBuffer = new StringBuffer(100);

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// format  -- This method is called for every log records
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public String format(LogRecord rec) 
{
            
  buf.delete (0,buf.length());
  timeBuffer.delete(0,timeBuffer.length());

  Calendar c1 = new GregorianCalendar ();
  int year = c1.get(Calendar.YEAR);
  int month = c1.get(Calendar.MONTH);
  int date = c1.get(Calendar.DATE);
  int hour = c1.get(Calendar.HOUR_OF_DAY);
  int minute = c1.get(Calendar.MINUTE);
  int second = c1.get(Calendar.SECOND);

  timeBuffer.append(year+":");
  timeBuffer.append(month+":");
  timeBuffer.append(date+":");
  timeBuffer.append(hour+":");
  timeBuffer.append(minute+":");
  timeBuffer.append(second);

  buf.append("DATE="+timeBuffer);
  buf.append(" PROG=SRM-CLIENT");
  buf.append(" SEC="+rec.getMillis());
  buf.append(" EVENT="+formatMessage(rec));
  buf.append('\n');
  return buf.toString();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// formatMessage -- Overrides the method defined in super class 
//                  formatter
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized String formatMessage(LogRecord record) 
{
   String format = record.getMessage();
   java.util.ResourceBundle catalog = record.getResourceBundle();
   if (catalog != null) {
       try {
         format = catalog.getString(record.getMessage());
       } catch (java.util.MissingResourceException ex) {
          // Drop through.  Use record message as format
          format = record.getMessage();
       }
    }
    // Do the formatting.
    try {
       Object parameters[] = record.getParameters();
       if (parameters == null || parameters.length == 0) {
          // No parameters.  Just return format string.
           return format;
        }
        StringBuffer buf = new StringBuffer(); 
        for(int i = 0; i < parameters.length; i++) {
           String str = (String) parameters [i];
           if( i < (parameters.length-1))
               buf.append(str + " "); 
            else
               buf.append(str); 
         }
         return format + " " + buf.toString();
     } catch (Exception ex) {
         // Formatting failed: use localized format string.
         return format;
      }
} 

}//end class NetLoggerFormatter
