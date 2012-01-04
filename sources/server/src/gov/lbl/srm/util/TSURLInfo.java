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

package gov.lbl.srm.util;

public class TSURLInfo {
    private org.apache.axis.types.URI SURL;
    private gov.lbl.srm.StorageResourceManager.ArrayOfTExtraInfo storageSystemInfo;
   
    public TSURLInfo() {
    }

    public TSURLInfo(
           org.apache.axis.types.URI SURL,
           gov.lbl.srm.StorageResourceManager.ArrayOfTExtraInfo storageSystemInfo) {
	
	if (SURL == null) {
	    throw new TSRMException("No URI!", false);
	}
	
	if (TSRMExceptionPathTooLong.exceedsLimit(SURL)) {
	    throw new TSRMExceptionPathTooLong(SURL.toString());
	}

	if (storageSystemInfo != null) {
	    gov.lbl.srm.StorageResourceManager.TExtraInfo[] infoList = storageSystemInfo.getExtraInfoArray();
	    if (infoList != null) {
		for (int i=0; i<infoList.length; i++) {
		    gov.lbl.srm.StorageResourceManager.TExtraInfo curr = infoList[i];
		    String key=curr.getKey();
		    if (key.equalsIgnoreCase("noAPI")) {
			boolean skip = Boolean.valueOf(curr.getValue());
			if (skip && (gov.lbl.srm.server.Config._gucLocation == null)) {
			    throw new TSRMException("Can not do _noAPI_!. guc location is not defined to server.", false);
			}
		    }
		}
	    }
	}
	this.SURL = SURL;
	this.storageSystemInfo = storageSystemInfo;

	printMe();
    }

    /**
     * Gets the SURL value for this TSURLInfo.
     * 
     * @return SURL
     */
    public org.apache.axis.types.URI getSURL() {
        return SURL;
    }


    /**
     * Sets the SURL value for this TSURLInfo.
     * 
     * @param SURL
     */
    public void setSURL(org.apache.axis.types.URI SURL) {
        this.SURL = SURL;
    }


    /**
     * Gets the storageSystemInfo value for this TSURLInfo.
     * 
     * @return storageSystemInfo
     */
    public gov.lbl.srm.StorageResourceManager.ArrayOfTExtraInfo getStorageSystemInfo() {
        return storageSystemInfo;
    }


    public void printMe() {
	gov.lbl.srm.StorageResourceManager.ArrayOfTExtraInfo extra = getStorageSystemInfo();
	if (extra == null) {
	    gov.lbl.srm.util.TSRMLog.debug(TSURLInfo.class, null, "url="+getSURL(), "extrainfo=null");
	    return;
	}
	gov.lbl.srm.StorageResourceManager.TExtraInfo[] arrayInfo = extra.getExtraInfoArray();
	if (arrayInfo == null) {
	    gov.lbl.srm.util.TSRMLog.debug(TSURLInfo.class, null, "url="+getSURL(), "extrainfo=nullArray");
	    return;
	}
	gov.lbl.srm.util.TSRMLog.debug(TSURLInfo.class, null, "url="+getSURL(), "extrainfoSize="+arrayInfo.length);
	for (int i=0; i<arrayInfo.length; i++) {
	    gov.lbl.srm.StorageResourceManager.TExtraInfo curr = arrayInfo[i];
	    gov.lbl.srm.util.TSRMLog.debug(TSURLInfo.class, null, "i="+i, "key=\""+curr.getKey()+"\" value=\""+curr.getValue()+"\"");
	}
	    
    }
    /**
     * Sets the storageSystemInfo value for this TSURLInfo.
     * 
     * @param storageSystemInfo
     */
    public void setStorageSystemInfo(gov.lbl.srm.StorageResourceManager.ArrayOfTExtraInfo storageSystemInfo) {
        this.storageSystemInfo = storageSystemInfo;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof TSURLInfo)) return false;
        TSURLInfo other = (TSURLInfo) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.SURL==null && other.getSURL()==null) || 
             (this.SURL!=null &&
              this.SURL.equals(other.getSURL()))) &&
            ((this.storageSystemInfo==null && other.getStorageSystemInfo()==null) || 
             (this.storageSystemInfo!=null &&
              this.storageSystemInfo.equals(other.getStorageSystemInfo())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getSURL() != null) {
            _hashCode += getSURL().hashCode();
        }
        if (getStorageSystemInfo() != null) {
            _hashCode += getStorageSystemInfo().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

 

}
