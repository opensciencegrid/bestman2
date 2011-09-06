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
//Class ObjectFIFO
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public class ObjectFIFO {

private Object[] queue;
private int capacity;
private int size;
private int head;
private int tail;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// ObjectFIFO
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public ObjectFIFO(int cap) {
   capacity = ( cap > 0 ) ? cap : 1; // at least 1
   queue = new Object[capacity];
   head = 0;
   tail = 0;
   size = 0;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getCapacity
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getCapacity() {
   return capacity;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSize2
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getSize2() {
 int count = 0;
 try {
  Object[] list = getObject();
  for(int i = 0; i < list.length; i++) {
    FileObj fObj = (FileObj) list[i];
    if(fObj != null) {
      count++;
    }
  }
  }catch(Exception e) {
    System.out.println("Exception.getSize2="+e.getMessage());
  }
  return count;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getSize
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized int getSize() {
   return size;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isEmpty
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isEmpty() {
   return ( size == 0 );
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// isFull
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean isFull() {
   return ( size == capacity );
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// add
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void add(Object obj) throws InterruptedException {
   waitWhileFull();

   queue[head] = obj;
   head = ( head + 1 ) % capacity;
   size++;

   notifyAll(); // let any waiting threads know about change
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// addEach
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void addEach(Object[] list) 
    throws InterruptedException {
   //
   // You might want to code a more efficient 
   // implementation here ... (see ByteFIFO.java)
   //

   for ( int i = 0; i < list.length; i++ ) {
       add(list[i]);
   }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getObject
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object[] getObject() 
    throws InterruptedException {

    Object[] list = new Object[queue.length]; 

    for ( int i = 0; i < queue.length; i++ ) {
          list[i] = queue[i];
    }
    return list;

}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// getObjectV
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object[] getObjectV(int index) 
    throws InterruptedException {

    Object[] list = new Object[queue.length]; 

    if( index == 0) {
      for ( int i = 0; i < queue.length; i++ ) {
          list[i] = queue[i];
      }
    }
    else {
      int j = 0;
      for ( int i = index; i < queue.length; i++ ) {
          list[j] = queue[i];
          j++;
      }
      for ( int i = 0; i < index; i++ ) {
          list[j] = queue[i];
      }
    }

    return list;

}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// remove
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object remove(int idx) 
    throws InterruptedException {


    Object obj = null;
 

       if(idx == tail) {
          Object tobj = remove();
          return tobj;
       }
       else {
          waitWhileEmpty();
          obj = queue[idx];
          //Added by viji on Jan 13, 09
          queue[idx]=null;
          for(int i = idx; idx > tail; idx--) {  
             queue[idx] = queue[idx-1]; //moving upwards
             //commented on Nov 18, 09 for eric's missing queue case
             //if(tail == idx) {
               tail = (tail+1) % capacity;
             //}
          }
          /*
          for(int i = idx; idx > tail; idx--) {
            queue[idx] = queue[idx-1];
            tail = (tail+1) % capacity;
          }
          */
          if(size > 0)
            size --;
       }

    notifyAll(); // let any waiting threads know about change

    return obj;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// remove
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object remove() 
    throws InterruptedException {

    waitWhileEmpty();
            
    Object obj = queue[tail];

    // don't block GC by keeping unnecessary reference
    queue[tail] = null; 

    tail = ( tail + 1 ) % capacity;
    size--;

    notifyAll(); // let any waiting threads know about change

    return obj;
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// removeAll
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object[] removeAll() 
    throws InterruptedException {

    //
    // You might want to code a more efficient 
    // implementation here ... (see ByteFIFO.java)
    //

    Object[] list = new Object[size]; // use the current size

    for ( int i = 0; i < list.length; i++ ) {
        list[i] = remove();
    }

    // if FIFO was empty, a zero-length array is returned
    return list; 
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// removeAtLeastOne
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized Object[] removeAtLeastOne() 
    throws InterruptedException {

    waitWhileEmpty(); // wait for a least one to be in FIFO
    return removeAll();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// waitUntilEmpty
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized boolean waitUntilEmpty(long msTimeout) 
    throws InterruptedException {

    if ( msTimeout == 0L ) {
         waitUntilEmpty();  // use other method
         return true;
    }

    // wait only for the specified amount of time
    long endTime = System.currentTimeMillis() + msTimeout;
    long msRemaining = msTimeout;

    while ( !isEmpty() && ( msRemaining > 0L ) ) {
        wait(msRemaining);
        msRemaining = endTime - System.currentTimeMillis();
    }

    // May have timed out, or may have met condition, 
   // calc return value.
   return isEmpty();
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// waitUntilEmpty
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void waitUntilEmpty() 
     throws InterruptedException {

     while ( !isEmpty() ) {
       wait();
     }
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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// waitUntilFull
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void waitUntilFull() 
     throws InterruptedException {

     while ( !isFull() ) {
       wait();
     }
}

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// waitUntilFull
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

public synchronized void waitWhileFull() 
     throws InterruptedException {

     while ( isFull() ) {
       wait();
     }
}

public synchronized int getTail() {
  return tail;
}

}
