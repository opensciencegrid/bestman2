/**
 *
 * *** Copyright Notice ***
 *
 * BeStMan Copyright (c) 2013-2014, The Regents of the University of California, 
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
 * Email questions to SDMSUPPORT@LBL.GOV
 * Scientific Data Management Research Group
 * Lawrence Berkeley National Laboratory
 * http://sdm.lbl.gov/bestman
 *
*/

package gov.lbl.adapt.srm.client.main;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.util.*;

import gov.lbl.adapt.srm.client.intf.*;

//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// MyTableModel
//::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::


public class MyTableModel extends DefaultTableModel 
	implements SortTableModel {

        Vector columnNames;
        Vector data ;
    	Vector fileInfo; 
        Vector fileSize;

       public MyTableModel () {}

       public MyTableModel (int rows, int cols) {
          super(rows,cols);
       }

       public MyTableModel (Object[][] data, Object[] columnNames) {
           super(data, columnNames);
       }

       public MyTableModel (Object[] columnNames, int rows) {
          super(columnNames, rows);
       }

       public MyTableModel (Vector values, Vector names, 
		Vector fileInfo, Vector fileSize) {  

	     this.data = values;
	     this.columnNames = names;
             this.fileInfo = fileInfo;
             this.fileSize = fileSize;
       }

        public int getColumnCount() {
            return columnNames.size();
        }

        public int getRowCount() {
	    if(data == null) {
	       return 0;
	    }
            return data.size();
        }

        public String getColumnName(int col) {
            return (String) columnNames.elementAt(col);
        }

        public Object getValueAt(int row, int col) {
            Vector rowV = (Vector) data.elementAt(row);
            return rowV.elementAt(col);
        }

        public FileInfo getValueAt(int row) {
	   return (FileInfo)fileInfo.elementAt(row);
	}

        public FSize getFileSizeAt(int row) {
	   return (FSize)fileSize.elementAt(row);
	}

        public void setFileInfo(String value, int row, int col) {
	      FileInfo fInfo = (FileInfo)fileInfo.elementAt(row);
	      switch (col ) {
	        case 1 : fInfo.setTURL(value); break;
	        case 2 : fInfo.setStatusLabel(value); break;
	        case 9 : fInfo.setTargetDir(value); break;
          }	   
	}

        public Vector getFileInfo() { 
           return fileInfo;
        }

        /*
         * JTable uses this method to determine the default renderer/
         * editor for each cell.  If we didn't implement this method,
         * then the last column would contain text ("true"/"false"),
         * rather than a check box.
         */
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
            //Note that the data/cell address is constant,
            //no matter where the cell appears onscreen.
            if (col == 1) {
                return true;
            } else {
                return false;
            }
        }

          public void setValueAt(Object value, int row, int col) {

            Vector rowV = (Vector) data.elementAt(row);
            if(col == 1) {
                FileIntf fIntf = (FileIntf) getValueAt(row,3);
	        fIntf.setTURL((String)value);
            }
            rowV.setElementAt(value,col);
            fireTableCellUpdated(row, col);
        }

        public boolean isSortable(int col) {
            return true;
        }

       /*
        public Vector getDataValues() {
          return super.dataVector;
        }
        */

        public void sortColumn(int col, boolean ascending) {
           Collections.sort(data, new ColumnComparator(col,ascending));
        }

        public void specialSortColumn(int col, boolean ascending) {
           Collections.sort(data, new SpecialColumnComparator(col,ascending));
        }

}
