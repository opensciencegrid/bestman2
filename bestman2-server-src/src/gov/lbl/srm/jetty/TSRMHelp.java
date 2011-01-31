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

import gov.lbl.srm.server.*;
import gov.lbl.srm.policy.*;
import gov.lbl.srm.storage.*;
import gov.lbl.srm.transfer.globus.*;

public class TSRMHelp {
	public static String _hasDefault="\t default:";
    public static String _noDefault = "\t no default.";

    public static String[] _supportedEntryNamesInConfigFile = 
        {Config._DefConfigEntryFactoryID, _noDefault,
	 Config._DefConfigEntryPlugin, _noDefault,
	 Config._DefConfigEntryEventLogLevel, _hasDefault+TSRMEventLog._logLevel,
	 Config._DefConfigEntrySecurePort, _hasDefault+Config._securePort,
	 Config._DefConfigEntryProtocol, _hasDefault+Config._DefContactProtocolStr,
	 //Config._DefConfigEntryPublicPort, _hasDefault+Config._publicPort,
	 Config._DefConfigEntryGridMapFileName, _hasDefault+Config._gridMapFileName,
	 Config._DefConfigEntryEventLogFileName, _hasDefault+Config._defaultEventFileName,
	 Config._DefConfigEntryCacheLogFileName, _hasDefault+"/var/log/cache.srm.log",
	 Config._DefConfigEntryMSSTimeOut, _hasDefault+Config._mssTimeOutMilliSeconds/1000,
	 //Config._DefConfigEntryProxyFileName, _noDefault,
	 Config._DefConfigEntryKeyFileName, _noDefault,
	 Config._DefConfigEntryCertFileName, _noDefault,
	 Config._DefConfigEntryGlobusParallelStream, _hasDefault+SRMFileTransfer.DEFAULT_NUM_PARALLELSTREAM,
	 Config._DefConfigEntryGlobusDoDcau, _hasDefault+Config._doDcau,
	 Config._DefConfigEntryGlobusBufferSizeMB, _hasDefault+SRMFileTransfer.DEFAULT_BUFFERSIZE_MB,
	 Config._DefConfigEntryGlobusBufferSize, _hasDefault+SRMFileTransfer.DEFAULT_BUFFERSIZE_MB*1048576,
	 Config._DefConfigEntrySupportedProtocols, _noDefault+" format: protocol://host:port/ use ; to seperate multiple entries.",
	 Config._DefConfigEntrySupportedProtocolPolicy, _hasDefault+" roundrobin.",
	 Config._DefConfigEntryUseBerkeleyDB, _hasDefault+Config._useBerkeleyDB,
	 Config._DefConfigEntryFailedFTPRetryPolicy, _hasDefault+Config._retryIntervalSeconds+"/"+Config._retryCountMax,
	 Config._DefConfigEntrySilent, _hasDefault+Config._silent,
	 Config._DefConfigEntryMarkupPingMsg, _hasDefault+Config._doMarkupPingMsg,
	 Config._DefConfigEntryUsingCacheKeyword, _hasDefault+Config._usingCacheKeyword, 
	 Config._DefConfigEntryLocalPathListBlock, _hasDefault+Config._blockedLocalPath,
	 Config._DefConfigEntryLocalPathListAllow, _noDefault,
	 Config._DefConfigEntryDisableSpaceMgt, _hasDefault+Config._disableSpaceMgt,
	 Config._DefConfigEntryDisableDirMgt, _hasDefault+Config._disableDirMgt,
	 Config._DefConfigEntryDisableLocalAuthorization, _hasDefault+Config._disableLocalAuthorization,
	 Config._DefConfigEntryDisableRemoteCopy, _hasDefault+Config._disableRemoteCopy,
	 Config._DefConfigEntryGUMSService, _hasDefault+Config._gumsClient,
	 Config._DefConfigEntryGUMSCurrHostDN, _hasDefault+"/DC=org/.../CN=hostname",
	 Config._DefConfigEntryGUMSProtocol, _hasDefault+"AUTO",
	 Config._DefConfigEntryAccessFileSysUsingSUDO, _hasDefault+Config._accessFileSysUsingSUDO,
	 Config._DefConfigEntryAccessFileSysUsingGSIFTP, _hasDefault+Config._accessFileSysUsingGSIFTP,
	 Config._DefConfigEntryNoSudoOnLs, _hasDefault+Config._noSudoOnLs,
	 Config._DefConfigEntryNoEventLog, _hasDefault+"false",
	 Config._DefConfigEntryNoCacheLog, _hasDefault+"false",
	 Config._DefConfigEntryGUCPATH, _noDefault,
	 Config._DefConfigEntryUploadQueueParameter, _noDefault,
	 Config._DefConfigEntryUserTokens, _noDefault,
	 Config._DefConfigEntryMappedIDRefreshInterval, _hasDefault+Config._defaultMappedIDLifetime,
	 Config._DefConfigEntryMappedIDStorageLimit, _hasDefault+Config._defaultMaxMappedID,
	 Config._DefConfigEntryLocalFileSystemAccess, _hasDefault+" java.io.File",
	 Config._DefConfigEntryComputeChecksumOnFileLs, _hasDefault+Config._doComputeFileChecksum,
	 Config._DefConfigEntryClientWaitParamsMaxWaitMillis, _hasDefault+Config._gsiftpClientWaitParamsMaxWaitMillis,
	 Config._DefConfigEntryClientWaitParamsWaitDelayMillis, _hasDefault+Config._gsiftpClientWaitParamsWaitDelayMillis,
	 Config._DefConfigEntryValidateVoms, _hasDefault+Config._doValidateVoms,
	 Config._DefConfigStaticTokens, _noDefault,

	 // in file TSRMGeneralEnforcement
	 TSRMGeneralEnforcement._DefConfigMaxActiveAccount, _hasDefault+TSRMGeneralEnforcement.MAX_ACTIVE_ACCOUNTS,
	 TSRMGeneralEnforcement._DefConfigVolatileFileLifeTime, _hasDefault+TSRMGeneralEnforcement._DEFAULT_FILE_LIFETIME_SECS,
	 TSRMGeneralEnforcement._DefConfigMaxFileRequests, _hasDefault+TSRMGeneralEnforcement.MAX_FILE_REQUESTS,
	 TSRMGeneralEnforcement._DefConfigMaxConcurrentTxf,_hasDefault+TSRMGeneralEnforcement.MAX_CONCURRENT_TRANSFER_ALLOWED,
	 TSRMGeneralEnforcement._DefConfigThreadPoolSize,_hasDefault+TSRMGeneralEnforcement.THREAD_POOL_SIZE,
	 TSRMGeneralEnforcement._DefConfigInactiveTxfTimeOut, _hasDefault+TSRMGeneralEnforcement._INACTIVE_TXF_TIME_IN_MILLISECONDS/1000,
	 TSRMGeneralEnforcement._DefConfigMaxMSSConnection, _hasDefault+TSRMGeneralEnforcement.MAX_MSS_CONNECTION,
	 
	 TSRMStorage._DefVolatileSpaceLocations, _noDefault,
	 TSRMStorage._DefDurableSpaceLocations, _noDefault,
	 TSRMStorage._DefPermanentSpaceLocations, _noDefault,
	 TSRMStorage._DefVolatileSpaceUserQuotaMB, _hasDefault+" no limit imposed.",
	 
	 TSRMPublicTokenConfiguration.DefMAX_MB_PER_USER, _hasDefault+" up to:"+TSRMPublicTokenConfiguration.DEFAULT_MAX_SIZE_MB +"MB",
	 TSRMPublicTokenConfiguration.DefMAX_NUM_FILES, _hasDefault+TSRMPublicTokenConfiguration.DEFAULT_MAX_NUM_FILES,
	 TSRMPublicTokenConfiguration.DefMAX_FILE_LIFETIME_SECONDS, _hasDefault+TSRMPublicTokenConfiguration.DEFAULT_MAX_FILE_LIFETIME_SECS,

	 TSRMStoragePolicyDefault._DefDefaultMBPerToken, _hasDefault+"up to:"+TSRMStoragePolicyDefault.SYSTEM_DEFAULT_SIZE_PER_TOKEN_MB,	 
	 TSRMStoragePolicyDefault._DefPublicTokenPercentage, _hasDefault+TSRMStoragePolicyDefault.SYSTEM_PUBLIC_TOKEN_PERCENTAGE+"%",	 
	 TSRMStoragePolicyDefault._DefPublicTokenMB, _hasDefault+TSRMStoragePolicyDefault.SYSTEM_PUBLIC_TOKEN_PERCENTAGE+"% of replica space",
	 TSRMStoragePolicyDefault._DefDefaultFileSizeMB, _hasDefault+"1/10 of: disk size",
	 
	 TAccessControl._DefDefaultWorldPermission, _hasDefault+TAccessControl._WorldPermission+" choices are:R W None"
	};
 
    public static void usage() {
	System.out.println("Usage: Server <configuaration file>");
	System.out.println("entries supported in the configuration file:");

	for (int i=0; i<_supportedEntryNamesInConfigFile.length; i++) {
	    System.out.println(_supportedEntryNamesInConfigFile[i]);
	}
    }

    public static void listUnknownEntries(java.util.Properties prop) {
    java.util.Enumeration e = prop.propertyNames();
        while (e.hasMoreElements()) {
        String curr = (String)(e.nextElement());
        boolean found = false;
        for (int i=0; i<_supportedEntryNamesInConfigFile.length; i++) {
        if (curr.equals(_supportedEntryNamesInConfigFile[i])) {
            found = true;
            break;
        }
        }
        if (!found) {
        if (gov.lbl.srm.impl.TSRMServiceGateway.knows(curr)) {
            TSRMUtil.startUpInfo(">>>>>>>> Gateway mode specific: "+curr);
        } else {
            TSRMUtil.startUpInfo(">>>>>>>> unrecognized configuration entry: "+curr);
        }
        }
    }
    }

}
