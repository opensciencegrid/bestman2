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
// MultiLineToolTipUI
//:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

import javax.swing.*;
import javax.swing.plaf.*;

import java.awt.*;
import java.awt.font.*;
import java.awt.event.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.BasicToolTipUI;
import javax.swing.text.*;



public class MultiLineToolTipUI extends BasicToolTipUI {

static MultiLineToolTipUI sharedInstance = new MultiLineToolTipUI();
Font smallFont;                              
static JToolTip tip;
protected CellRendererPane rendererPane;
            
private static JTextArea textArea ;
            
public static ComponentUI createUI(JComponent c) {
     return sharedInstance;
}
            
public MultiLineToolTipUI() {
     super();
     System.out.println("MultiLineToolTipUI");
}
            
public void installUI(JComponent c) {
     super.installUI(c);
     tip = (JToolTip)c;
     rendererPane = new CellRendererPane();
     c.add(rendererPane);
}
            
public void uninstallUI(JComponent c) {
     super.uninstallUI(c);
                   
     c.remove(rendererPane);
     rendererPane = null;
}
            
public void paint(Graphics g, JComponent c) {
     Dimension size = c.getSize();
     textArea.setBackground(c.getBackground());
     rendererPane.paintComponent(g, textArea, c, 1, 1, 
	size.width - 1, size.height - 1, true);
}
            
public Dimension getPreferredSize(JComponent c) {
     String tipText = ((JToolTip)c).getTipText();
     if (tipText == null)
         return new Dimension(0,0);
     textArea = new JTextArea(tipText );
     rendererPane.removeAll();
     rendererPane.add(textArea );
     textArea.setWrapStyleWord(true);
     int width = ((JMultiLineToolTip)c).getFixedWidth();
     int columns = ((JMultiLineToolTip)c).getColumns();

     //System.out.println("Cols " + columns);
     //System.out.println("fWidth " + width);
                    
     if( columns > 0 ) {
         textArea.setColumns(columns);
         textArea.setSize(0,0);
         textArea.setLineWrap(true);
         textArea.setSize( textArea.getPreferredSize() );
     }
     else if( width > 0 ) {
         textArea.setLineWrap(true);
         Dimension d = textArea.getPreferredSize();
         d.width = width;
         d.height++;
         textArea.setSize(d);
     }
     else
         textArea.setLineWrap(false);


     Dimension dim = textArea.getPreferredSize();
                   
     dim.height += 1;
     dim.width += 1;
     return dim;
}
            
public Dimension getMinimumSize(JComponent c) {
     return getPreferredSize(c);
}
            
public Dimension getMaximumSize(JComponent c) {
     return getPreferredSize(c);
}

}
