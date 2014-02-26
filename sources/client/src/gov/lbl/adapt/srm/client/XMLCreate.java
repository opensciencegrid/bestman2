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

import javax.xml.parsers.*;
import java.io.*;

import org.apache.xml.serialize.*;
import org.w3c.dom.*;

public class XMLCreate {
  public static void main (String[] args) {
   try {
     DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
     DocumentBuilder parser = factory.newDocumentBuilder ();
     Document doc = parser.newDocument();
     Element root = doc.createElement("request"); 
     
     for(int i = 0; i < 20000; i++) {
        StringBuffer buf = new StringBuffer();  
        Element files = doc.createElement("files");
        Element surl = doc.createElement("sourceurl");
        String sourceurl = "";
        switch(i % 5) {
          case 0: sourceurl="gsiftp://dmx.lbl.gov//tmp/medium.0"; break;
          case 1: sourceurl="gsiftp://dmx.lbl.gov//tmp/medium.1"; break;
          case 2: sourceurl="gsiftp://dmx.lbl.gov//tmp/medium.2"; break;
          case 3: sourceurl="gsiftp://dmx.lbl.gov//tmp/medium.3"; break;
          case 4: sourceurl="gsiftp://dmx.lbl.gov//tmp/medium.4"; break;
        }
        surl.appendChild(doc.createTextNode(sourceurl)); 
        Element size = doc.createElement("size");
        size.appendChild (doc.createTextNode("131019086"));
        files.appendChild(surl);
        files.appendChild(size);
        root.appendChild(files);
     }
     doc.appendChild(root);
     try {
        OutputFormat format = new OutputFormat(doc); 
        format.setIndenting(true);
        XMLSerializer serializer = new XMLSerializer(format);
        PrintWriter pw = new PrintWriter(new FileOutputStream("req.xml"));
        serializer.setOutputCharStream(pw); 
        serializer.setOutputFormat(format); 
        serializer.serialize(doc);
     }catch(Exception e) {
        System.out.println("Exception " + e.getMessage());
     }
   }catch(Exception e){
        System.out.println("Exception " + e.getMessage());
   }
  }
}
