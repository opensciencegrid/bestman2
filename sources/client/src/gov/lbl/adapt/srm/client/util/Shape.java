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

package gov.lbl.adapt.srm.client.util;

import javax.swing.*; // For JPanel, etc.
import java.awt.*;           // For Graphics, etc.
import java.awt.geom.*;      // For Ellipse2D, etc.

import gov.lbl.adapt.srm.client.intf.colorIntf;

public class Shape extends JPanel implements colorIntf {
  Rectangle2D.Double square;
  Color color;
  int x;
  int y;
  String label;
  
  public Shape (int x, int y, Color color, String label) {
    
     setBackground(bgColor);
     square = new Rectangle2D.Double(x, y, 10, 10);
     this.color = color;
     this.x = x;
     this.y = y;
     this.label = label;
  }

  public void paintComponent(Graphics g) {
    clear(g);
    Graphics2D g2d = (Graphics2D)g;
    g2d.setPaint(color);
    g2d.draw(square);
    g2d.fill(square);
    g2d.setPaint(Color.black);
    g2d.drawString(label,x+20,y+10);
  }

  // super.paintComponent clears offscreen pixmap,
  // since we're using double buffering by default.

  protected void clear(Graphics g) {
    super.paintComponent(g);
  }
}
