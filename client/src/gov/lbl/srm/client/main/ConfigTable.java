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

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.swing.table.AbstractTableModel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gov.lbl.srm.client.intf.*;
import gov.lbl.srm.client.util.*;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.apache.xml.serialize.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.globus.util.Base64;

public class ConfigTable extends JFrame implements actionIntf, colorIntf,
	itemIntf, threadIntf {
  private SRMClientIntf _parent;
  private ActionCallBack actionCallBack;
  private ItemCallBack itemCallBack;
  private JCheckBox renewAuto;
  private MyTableModel model;
  private static PassPhraseWindow passPhraseWindow;
  private static Log logger; 
  private Properties properties;

    public ConfigTable(SRMClientIntf parent, String[] columnNames, 
		Properties props, boolean renewProxy)
		{
        JPanel panel = (JPanel) getContentPane();
        panel.setLayout(new SpringLayout());
        panel.setBackground(bgColor);
        JPanel p1 = new JPanel(new SpringLayout());
        p1.setBackground(bgColor);
        JPanel p2 = new JPanel(new SpringLayout());
        p2.setBackground(bgColor);
        JPanel p3 = new JPanel(new SpringLayout());
        p3.setBackground(bgColor);
        actionCallBack = new ActionCallBack(this);
        itemCallBack = new ItemCallBack(this);
        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(actionCallBack);
        saveButton.setActionCommand("save");
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(actionCallBack);
        cancelButton.setActionCommand("cancel");

        _parent = parent;
        properties = props;
        model = new MyTableModel(columnNames, props);
        JTable table = new JTable(model);
	table.setBackground(bgColor);

        try {
           ClassLoader cl = this.getClass().getClassLoader();
           Class c = cl.loadClass("gov.lbl.srm.client.main.ConfigTable");
           logger = LogFactory.getLog(c.getName());
        }catch(ClassNotFoundException cnfe) {
           System.out.println("ClassNotFoundException ");
           //throw new SRMClientException(cnfe.getMessage());
        }

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getViewport().setBackground(bgColor);

        table.setDefaultEditor(String.class,
                               new FileEditor(this));

        if(renewProxy) {
         renewAuto = new JCheckBox("Renew proxy auto", true);
	}
	else {
         renewAuto = new JCheckBox("Renew proxy auto", false);
	}
        renewAuto.setBackground(bgColor);
        renewAuto.addItemListener(itemCallBack);

        p1.add(scrollPane);
        p2.add(saveButton);
        p2.add(cancelButton);
        p3.add(renewAuto);
	panel.add(p1);
	panel.add(p3);
	panel.add(p2);
        SpringUtilities.makeCompactGrid(p1,1,1,1,1,1,1);
        SpringUtilities.makeCompactGrid(p3,1,1,1,1,1,1);
        SpringUtilities.makeCompactGrid(p2,1,2,1,1,1,1);

        SpringUtilities.makeCompactGrid(panel,3,1,1,1,1,1);
        //Add the scroll pane to this panel.
        setContentPane(panel);
    }


public void processItemEvent(ItemEvent evt) {
         
  if(renewAuto.isSelected()) {
    SwingUtilities.invokeLater(new Runnable() {
       public void run() {
         passPhraseWindow = 
	    PassPhraseWindow.getPassPhraseWindow(_parent,null,1);
         passPhraseWindow.pack(); 
         Point p = renewAuto.getLocationOnScreen();
         passPhraseWindow.setLocation((int)p.getX()+200, (int) p.getY());
         passPhraseWindow.setSize(500,85);
         passPhraseWindow.show();
       }
    });
  }
  else { 
    _parent.setPassword("");
  }
}

    public void processActionEvent(ActionEvent evt) {

        String command = evt.getActionCommand();

        if(command.equals("cancel")) {
          if(passPhraseWindow != null) {
              passPhraseWindow.hide();
          }
          this.hide();
        }
	else if(command.equals("save")) {
           if(renewAuto.isSelected()) {  
             if(passPhraseWindow != null) {
                String pwd = new String(passPhraseWindow.getPassword());
                if(pwd == null || pwd.trim().length() == 0) { 
                  logger.debug(
			"Password is not given.");
                  ShowException.showMessageDialog
			(_parent.getFrame(), "Password is not given.");
                }
                else {
                  _parent.setPassword(pwd);
                }
                passPhraseWindow.hide();
             }
           }
           else {
             if(passPhraseWindow != null) {
               passPhraseWindow.hide();
             }
           }
	   ThreadCallBack threadCallBack = new ThreadCallBack(this);
	   threadCallBack.setFileName(_parent.getConfigFileLocation());
	   threadCallBack.start();
	   this.hide();
	}
    }

    public void processThreadRequest(String fileName) {
	try {
	DocumentBuilderFactory factory = 
		DocumentBuilderFactory.newInstance();
        DocumentBuilder parser = factory.newDocumentBuilder();
	Document doc = parser.newDocument();
	Element root = doc.createElement("config");
        int rCount = model.getRowCount();
        int cCount = model.getColumnCount();
	for(int i = 0; i < rCount; i++) {
	  Element prop = null; 
	  for(int j = 0; j < cCount; j++) {
	    String value = ((String) model.getValueAt(i,j)).trim();
	    if(j == 0) {
	      prop = doc.createElement(value);
	    }
	    else if(j == 1) {
	      prop.appendChild(doc.createTextNode(value));
	      root.appendChild(prop);
	    }
	  } 
	}
	Element lastTargetLocation = doc.createElement("last-target-location"); 
    String value = properties.getProperty("last-target-location");
    if(value != null) {
	  lastTargetLocation.appendChild(doc.createTextNode(value));
    }
	root.appendChild(lastTargetLocation);

	Element proxyRenew = doc.createElement("renew-proxy-auto"); 
	proxyRenew.appendChild(doc.createTextNode(""+false));
	root.appendChild(proxyRenew);

	doc.appendChild(root);
	//default encoding is UTF-8
	  OutputFormat format = new OutputFormat(doc);
	  format.setIndenting(true);
	  XMLSerializer serializer = new XMLSerializer(format);
	  PrintWriter pw = new PrintWriter(new FileOutputStream(fileName));
	  serializer.setOutputCharStream (pw);
	  serializer.setOutputFormat(format);
  	  serializer.serialize(doc);
          XMLParseConfig pConfig = new
	 	XMLParseConfig(_parent.getConfigFileLocation());
          _parent.setConfig(pConfig);
	}catch(IOException ioe) {
	  //ShowException.logDebugMessage(logger,ioe);
	  ShowException.showMessageDialog(this,
		"Exception : " + ioe.getMessage());
	}catch(Exception e) {
	  //ShowException.logDebugMessage(logger,e);
	  ShowException.showMessageDialog(this,"Exception : " + e.getMessage());
	}
        
    }

    class MyTableModel extends AbstractTableModel {
       private String[] columnNames;
       private Properties props;
       private Vector data = new Vector();

        public MyTableModel(String[] columnNames, Properties props) {
            this.columnNames = columnNames;
            this.props = props;
            Enumeration e = props.propertyNames(); 
            while(e.hasMoreElements()) {
               String col = (String)e.nextElement();
               if(!col.equals("last-target-location")) {
                  Vector row = new Vector();
                  row.add(col);
                  row.add(props.getProperty(col));
                  data.add(row);
               }
            }
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

        public Object getValueAt(int row, int col) {
            Vector rowV = (Vector) data.elementAt(row); 
            return rowV.elementAt(col);
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
            if (col < 1) {
                return false;
            } else {
                return true;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            Vector rowV = (Vector)data.elementAt(row);
            rowV.setElementAt(value,col);
            fireTableCellUpdated(row, col);
        }
   }


}
