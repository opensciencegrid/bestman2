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

package gov.lbl.srm.server;

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.storage.*;
import org.apache.axis.types.*;
import gov.lbl.srm.util.*;

public class TSRMFileInput {
    //TSURLInfo         _identification          = null;
    URI                _original  = null;
    TSupportedURL      _identification = null;
    TFileStorageType  _fileStorageType = null;
    Integer           _txflifetime     = null;
    Integer           _lifetime        = null;
    String            _spaceToken      = null;
    TSRMSpaceType     _spaceType       = null;    
    TDirOption        _dirOption       = null; // get request
    UnsignedLong      _expectedSize    = null; // put request
    int               _pos             = -1; // position in the incoming request

    public TSRMFileInput(SrmBringOnlineRequest reqLevel, TGetFileRequest fileLevel, int pos) {
	if (fileLevel.getSourceSURL() != null) {
	    TSURLInfo info = new TSURLInfo(fileLevel.getSourceSURL(), reqLevel.getStorageSystemInfo());
	    _original = fileLevel.getSourceSURL();
	    _identification = TSupportedURL.create(info); 
	    
	}

	_dirOption       = fileLevel.getDirOption();
	_fileStorageType = reqLevel.getDesiredFileStorageType();
	_txflifetime     = reqLevel.getDesiredLifeTime();
	_spaceToken    = reqLevel.getTargetSpaceToken();

	setSpaceToken(reqLevel.getTargetFileRetentionPolicyInfo());
	
	_pos = pos;
    }

    public TSRMFileInput(SrmPrepareToGetRequest reqLevel, TGetFileRequest fileLevel, int pos) {
	if (fileLevel.getSourceSURL() != null) {
	    TSURLInfo info = new TSURLInfo(fileLevel.getSourceSURL(), reqLevel.getStorageSystemInfo());
	    _original = fileLevel.getSourceSURL();
	    _identification = TSupportedURL.create(info);
	}

	_dirOption       = fileLevel.getDirOption();
	_fileStorageType = reqLevel.getDesiredFileStorageType();
	_txflifetime     = reqLevel.getDesiredPinLifeTime();
	_spaceToken    = reqLevel.getTargetSpaceToken();
	
	setSpaceToken(reqLevel.getTargetFileRetentionPolicyInfo());

	_pos = pos;
    }

    public TSRMFileInput(SrmPrepareToPutRequest reqLevel, TPutFileRequest fileLevel, int pos) {	
	if (fileLevel.getTargetSURL() != null) {
	    TSURLInfo info = new TSURLInfo(fileLevel.getTargetSURL(), reqLevel.getStorageSystemInfo());
	    _original = fileLevel.getTargetSURL();
	    _identification = TSupportedURL.create(info);
	}

	_expectedSize    = fileLevel.getExpectedFileSize();
	_fileStorageType = reqLevel.getDesiredFileStorageType();
	_txflifetime     = reqLevel.getDesiredPinLifeTime();
	_lifetime        = reqLevel.getDesiredFileLifeTime();
	_spaceToken      = reqLevel.getTargetSpaceToken();
	
	setSpaceToken(reqLevel.getTargetFileRetentionPolicyInfo());

	_pos = pos;
    }

    public TSRMFileInput(SrmCopyRequest reqLevel, TCopyFileRequest fileLevel, int pos) {
	if (fileLevel.getTargetSURL() != null) {
	    TSURLInfo info = new TSURLInfo(fileLevel.getTargetSURL(), reqLevel.getTargetStorageSystemInfo());
	    _original = fileLevel.getSourceSURL();
	    _identification = TSupportedURL.create(info);
	}

	_dirOption       = fileLevel.getDirOption();
	_fileStorageType = reqLevel.getTargetFileStorageType();
	_lifetime        = reqLevel.getDesiredTargetSURLLifeTime();
	_spaceToken      = reqLevel.getTargetSpaceToken();
	
	setSpaceToken(reqLevel.getTargetFileRetentionPolicyInfo());

	_pos = pos;
    }

    public int getPos() {
	return _pos;
    }

    public URI getOriginalURI() {
	if (_original == null) {
	    return _identification.getURI();
	}
	return _original;
    }

    public TSupportedURL getSURLInfo() {
	return _identification;
    }

    private void setSpaceToken(TRetentionPolicyInfo info) {
	if (_spaceToken != null) {
	    _spaceType       = TSRMSpaceType.getSpaceType(_spaceToken);
	    if (info != null) {
		if (TSRMSpaceType.createSRMSpaceType(info) != _spaceType) {
		    //throw new TSRMException("Asked retention policy does not agree with given space token:"+_spaceToken, false);
		    throw new TSRMExceptionStorageTypeConflict(info, _spaceToken);
		}
	    }
	} else {
	    _spaceType       = TSRMSpaceType.createSRMSpaceType(info);
	}
    }

    public String getSpaceToken() {
	return _spaceToken;
    }

    public TSRMSpaceType getSpaceType() {
	return _spaceType;
    }

    public TFileStorageType getFileStorageType() {
	return _fileStorageType;
    }

    public TDirOption getDirOption() {
	return _dirOption;
    }

    public UnsignedLong getKnownSizeOfThisFile() {
	return _expectedSize;
    }

    public Integer getTxfLifetime() {
	return _txflifetime;
    }

    public Integer getFileLifetime() {
	return _lifetime;
    }
}
