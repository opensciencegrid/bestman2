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

package gov.lbl.srm.transfer.mss;

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
//Class VectorFIFO
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import java.util.Vector;

public class VectorFIFO {

private Vector queue;
private int size;
private int head;
private int tail;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// VectorFIFO
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public VectorFIFO(int cap) {
   int capacity = ( cap > 0 ) ? cap : 1; // at least 1
   queue = new Vector (capacity); 
   head = 0;
   tail = 0;
   size = 0;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isEmpty
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isEmpty() {
  if(head == size) return true;
  return false;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// add
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void add(Object obj) throws InterruptedException {

   queue.addElement(obj);
   tail ++;
   size ++;

   notifyAll(); // let any waiting threads know about change
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// remove
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object remove() 
    throws InterruptedException {

    waitWhileEmpty();
            
    Object obj = queue.get(head);
    head ++;

    if(isEmpty())
      clearQueue();

    notifyAll(); // let any waiting threads know about change

    return obj;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getSize ()
{
  //always size > head
  if(size > head) {
    return (size-head);
  }
  return 0;
}


//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// clearQueue
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void clearQueue () 
{
   queue.clear();
   size = 0;  
   head = 0;
   tail = 0;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// waitUntilEmpty
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void waitWhileEmpty() 
     throws InterruptedException {

     while ( isEmpty() ) {
       wait();
     }
}

public Vector getQueue ()
{
  return queue;
}

}
