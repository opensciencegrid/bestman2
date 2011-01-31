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

package gov.lbl.srm.client.util;


import java.io.*;

public class PasswordField {

     /**
      *@param prompt The prompt to display to the user
      *@return The password as entered by the user
      */
     public static String readPassword (String prompt) {
        MaskingThread et = new MaskingThread(prompt);
        Thread mask = new Thread(et);
        mask.start();

        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String password = "";

        try {
           password = in.readLine();
        } catch (IOException ioe) {
          ioe.printStackTrace();
        }
        // stop masking
        et.stopMasking();
        // return the password entered by the user
        return password;
    }
}

  /**
   * This class attempts to erase characters echoed to the console.
   */

class MaskingThread extends Thread {
     private volatile boolean stop;
     private char echochar = '*';

    /**
     *@param prompt The prompt displayed to the user
     */
     public MaskingThread(String prompt) {
        System.out.print(prompt);
     }

    /**
     * Begin masking until asked to stop.
     */
     public void run() {

        int priority = Thread.currentThread().getPriority();
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

        try {
           stop = true;
           while(stop) {
             System.out.print("\010" + echochar);
             try {
                // attempt masking at this rate
                Thread.currentThread().sleep(1);
             }catch (InterruptedException iex) {
                Thread.currentThread().interrupt();
                return;
             }
           }
        } finally { // restore the original priority
           Thread.currentThread().setPriority(priority);
        }
     }

    /**
     * Instruct the thread to stop masking.
     */
     public void stopMasking() {
        this.stop = false;
     }
  }
