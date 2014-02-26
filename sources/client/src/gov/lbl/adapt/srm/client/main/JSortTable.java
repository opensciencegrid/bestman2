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

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

public class JSortTable extends JTable
  implements MouseListener
{
  protected int sortedColumnIndex = -1;
  protected boolean sortedColumnAscending = true;
  //private Hashtable _completedFiles = new Hashtable ();
  //private Hashtable _newFiles = new Hashtable ();
  private int specialIndex = -1;
  
  public JSortTable()
  {
    this(new MyTableModel());
  }
  
  public JSortTable(int rows, int cols)
  {
    this(new MyTableModel(rows, cols));
  }
  
  public JSortTable(Vector data, Vector names, Vector fileInfo, Vector fileSize)
  {
    this(new MyTableModel(data, names, fileInfo, fileSize));
  }

  public void setSpecialIndex (int sIndex) 
  {
    specialIndex = sIndex;
  }

  /*
  public void setCompletedFiles(Hashtable completedFiles)
  {
     _completedFiles = completedFiles;
  }


  public Hashtable getCompletedFiles()
  {
     return _completedFiles;
  }

  public void setNewFiles(Hashtable newFiles)
  {
     _newFiles = newFiles;
  }

  public Hashtable getNewFiles()
  {
     return _newFiles;
  }
  */

  public JSortTable(SortTableModel model)
  {
    super(model);
    initSortHeader();
  }

  public JSortTable(SortTableModel model,
    TableColumnModel colModel)
  {
    super(model, colModel);
    initSortHeader();
  }

  public JSortTable(SortTableModel model,
    TableColumnModel colModel,
    ListSelectionModel selModel)
  {
    super(model, colModel, selModel);
    initSortHeader();
  }

  protected void initSortHeader()
  {
    JTableHeader header = getTableHeader();
    header.setDefaultRenderer(new SortHeaderRenderer());
    header.addMouseListener(this);
  }

  public int getSortedColumnIndex()
  {
    return sortedColumnIndex;
  }
  
  public boolean isSortedColumnAscending()
  {
    return sortedColumnAscending;
  }

  public void mouseReleased(MouseEvent event)
  {
    TableColumnModel colModel = getColumnModel();
    int index = colModel.getColumnIndexAtX(event.getX());
    int modelIndex = colModel.getColumn(index).getModelIndex();
    
    SortTableModel model = (SortTableModel)getModel();
    if (model.isSortable(modelIndex))
    {
      // toggle ascension, if already sorted
      if (sortedColumnIndex == index)
      {
        sortedColumnAscending = !sortedColumnAscending;
      }
      sortedColumnIndex = index;

    
      if(modelIndex == specialIndex) {
        model.specialSortColumn(modelIndex, sortedColumnAscending);
      }
      else {
        model.sortColumn(modelIndex, sortedColumnAscending);
      }
    }
  }
  
  public void mousePressed(MouseEvent event) {}
  public void mouseClicked(MouseEvent event) {}
  public void mouseEntered(MouseEvent event) {}
  public void mouseExited(MouseEvent event) {}

  /*
 public Component prepareRenderer(TableCellRenderer renderer,
              int rowIndex, int vColIndex) 
 {
    Component c = super.prepareRenderer(renderer, rowIndex, vColIndex);
    if (c instanceof JComponent) {
           JComponent jc = (JComponent)c;
           StringBuffer buffer = new StringBuffer();
           String fid = (String) getValueAt(rowIndex, 0);
           Object o =  _completedFiles.get(fid);
           if(o != null) {
             filePercentSize file = (filePercentSize) o;
             buffer.append("SourceURL : " + file.getSource() + " | ");
             buffer.append("TargetURL : " + file.getTarget() + " | ");
             buffer.append("FinalAvgRate : " + file.getFinalAvgRate());
             if(file.getWriteToMSS()) {
               buffer.append(" | ");
               buffer.append("Final target will be on MSS");
             }
           }
           jc.setToolTipText(buffer.toString());
     }
     return c;

  }
  */
}
