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

import javax.swing.AbstractCellEditor;
import javax.swing.table.TableCellEditor;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.*;
import javax.swing.*;

public class FileEditor extends AbstractCellEditor
                         implements TableCellEditor,
			            ActionListener {
    String currentFile;
    JButton button;
    JFileChooser fileChooser;
    JFrame parent;
    protected static final String EDIT = "edit";

    public FileEditor(JFrame parent) {
        //Set up the editor (from the table's point of view),
        //which is a button.
        //This button brings up the color chooser dialog,
        //which is the editor from the user's point of view.
 
        button = new JButton();
        button.setActionCommand(EDIT);
        button.addActionListener(this);
        button.setBorderPainted(false);
        this.parent = parent;

        //Set up the dialog that the button brings up.
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
    }

    /**
     * Handles events from the editor button and from
     * the dialog's OK button.
     */
    public void actionPerformed(ActionEvent e) {
        if (EDIT.equals(e.getActionCommand())) {
            //The user has clicked the cell, so
            //bring up the dialog.
            button.setLabel(currentFile);
            int returnValue = fileChooser.showOpenDialog(this.parent);
            if(returnValue == JFileChooser.APPROVE_OPTION) {
              currentFile =  fileChooser.getSelectedFile().toString();
            }
            //Make the renderer reappear.
            fireEditingStopped();

        } else { //User pressed dialog's "OK" button.
            currentFile = fileChooser.getSelectedFile().toString();
        }
    }

    //Implement the one CellEditor method that AbstractCellEditor doesn't.
    public Object getCellEditorValue() {
        return currentFile;
    }

    //Implement the one method defined by TableCellEditor.
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value,
                                                 boolean isSelected,
                                                 int row,
                                                 int column) {
        currentFile = (String)value;
        return button;
    }
}

