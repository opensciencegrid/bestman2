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

//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
// JMultiLineToolTip
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import javax.swing.*;
import javax.swing.plaf.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.*;



public class JMultiLineToolTip extends JToolTip
{

private static final String uiClassID = "ToolTipUI";
protected int columns = 0;
protected int fixedwidth = 0;
         
String tipText;
JComponent component;
            
public JMultiLineToolTip(int col, int fWidth) {
   updateUI();
   columns = col;
   fixedwidth = fWidth;
}
            
public void updateUI() {
   setUI(MultiLineToolTipUI.createUI(this));
}
            
public void setColumns(int columns)
{
   this.columns = columns;
   this.fixedwidth = 0;
}
            
public int getColumns()
{
    return columns;
}
           
public void setFixedWidth(int width)
{
    this.fixedwidth = width;
    this.columns = 0;
}
            
public int getFixedWidth()
{
    return fixedwidth;
}
            

}
