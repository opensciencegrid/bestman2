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

import javax.swing.JTable;
import javax.swing.table.*;
import java.awt.*;
import java.util.Vector;

import gov.lbl.srm.client.util.*;

public class ProgressBarTableModel extends DefaultTableModel{
  Vector data = new Vector ();
  Vector colNames ;
  int row = 0;
  Vector fileStatusGUIVec = new Vector (); 
  boolean _used = false;

  public ProgressBarTableModel (Object[] colNames, int row) {
    super(colNames, row);
    row = 0;
  }

  public ProgressBarTableModel (Vector colNames, Vector rows) {
    super(rows, colNames);
    this.colNames = colNames;
    this.data = rows;
    row = data.size();
  }

  public synchronized FileStatusGUI getFileStatusGUIVec(int row) {
     return (FileStatusGUI) fileStatusGUIVec.elementAt(row); 
  }

  public synchronized void insertRowValues(int size, Vector row, Integer x, 
		FileStatusGUI fgui) {
     this.insertRow(size,row);
     fileStatusGUIVec.addElement(fgui);
  }

  public synchronized void setLock(boolean b) {
    _used = b;
    if(!b) notifyAll(); 
  }

  public synchronized boolean getLock() {
    return _used;
  }

  public synchronized void removeRowValues(int size) {
   if(data.size() == 0) return;
   if(data.size() < size) return;

   while(_used) {
    try {
      System.out.println("Waiting Display Thread ...");
      if(data.size() != 0) { 
        wait();
      }
    }catch(InterruptedException ie) { 
      System.out.println("InterruptedException " + ie.getMessage());
    }
   }
   _used = true;
   this.removeRow(size);
   fileStatusGUIVec.remove(size);
   _used = false;
  }

  public boolean isCellEditable(int row, int col) {
    return false;
  }

  public String getColumnName(int col) {
     return (String) colNames.elementAt(col);
  }

  public synchronized Object getValueAt(int row, int col) {
   Object obj = null;
   try {
    if(data.size() < row) {
      return obj;
    }
    Vector rowV = (Vector) data.elementAt(row);
    obj = rowV.elementAt(col);
    }catch(ArrayIndexOutOfBoundsException aie) {
      System.out.println("ArrayIndexOutofBoundsException " + aie.getMessage());
    }
    return obj;
  }

  public synchronized void setValueAt(Object value, int row, int col) {
   Object obj = null;
     Vector rowV = (Vector) data.elementAt(row);
     rowV.setElementAt(value, col);
     fireTableCellUpdated(row, col);
  }
}
