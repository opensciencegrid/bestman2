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

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;

public class ConfigTableModel extends AbstractTableModel {

  final String[] columnNames = {"Property Name", "Property Value "};
                  
  final Class[] columnClasses = {String.class, String.class};
                  
  // holds our data
  final Vector data = new Vector();
                   
  // adds a new property
  public void addProperty(ConfigProperty p) {
     data.addElement(p);
     fireTableRowsInserted(data.size()-1, data.size()-1);
  }
                  
  public int getColumnCount() {
    return columnNames.length;
  }
                          
  public int getRowCount() {
    return data.size();
  }
                  
  public String getColumnName(int col) {
   return columnNames[col];
  }
                  
  public Class getColumnClass(int c) {
     return columnClasses[c];
  }
                  
  public Object getValueAt(int row, int col) {
     ConfigProperty cProperty = (ConfigProperty) data.elementAt(row);
        if (col == 0)      return cProperty.getName();
        else if (col == 1) return cProperty.getValue();
        else return null;
  }
                  
  public boolean isCellEditable(int row, int col) {
     return true;
  }
}

