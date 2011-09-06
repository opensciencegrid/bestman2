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

package gov.lbl.srm.transfer.mss.hpss;

import gov.lbl.srm.transfer.mss.MSS_MESSAGE;

public class MSS_MESSAGE_W_E {
   MSS_MESSAGE status = MSS_MESSAGE.SRM_MSS_REQUEST_QUEUED;
   String explanation ="";

   public MSS_MESSAGE_W_E () {
   }

   public void setStatus(MSS_MESSAGE status) {
      this.status = status;
   }

   public MSS_MESSAGE getStatus() {
      return this.status;
   }

   public void setExplanation(String explanation) {
      this.explanation = explanation;
   }

   public String getExplanation() {
      return this.explanation;
   }
}

