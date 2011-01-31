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

import java.io.*;
import java.util.zip.*;
import java.security.MessageDigest;

import gov.lbl.srm.StorageResourceManager.*;
import gov.lbl.srm.server.*;

public class Checksum {
    public static int Adler32 = 1;
    public static int Md5 = 2;
    public static int CRC32 = 3;

	public static String StrADLER32="ADLER32";
	public static String StrMD5="MD5";
	public static String StrCRC32="CRC32";

    public static long getAdler32(String file) {
	try {
	    FileInputStream f = new FileInputStream(file);
	    Adler32 ad32 = new Adler32();
	    CheckedInputStream cis = new CheckedInputStream(f, ad32);
	    BufferedInputStream input = new BufferedInputStream(cis);
	    while (input.read() != -1) {
		//read.. till the end of file
	    }
	    long result = cis.getChecksum().getValue();
	    input.close();
	    cis.close();
	    f.close();
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return 0;
	}
    }

    public static int getType(String input) {       
	if (input.equalsIgnoreCase("adler32")) {
	    return Checksum.Adler32;
	} else if (input.equalsIgnoreCase("md5")) {
	    return Checksum.Md5;
	} else if (input.equalsIgnoreCase("CRC32")) {
	    return Checksum.CRC32;
	}
	System.err.println("Error: The specified checksum type "+input+" is not supported.");
	return -1;
    }
	
    public static long getCRC32(String file) {
	try {
	    FileInputStream f = new FileInputStream(file);
	    CRC32 crc32 = new CRC32();
	    CheckedInputStream cis = new CheckedInputStream(f, crc32);
	    BufferedInputStream input = new BufferedInputStream(cis);
	    while (input.read() != -1) {
		//read.. till the end of file
	    }
	    long result = cis.getChecksum().getValue();
	    input.close();
	    cis.close();
	    f.close();
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return 0;
	}
    }

    public static String getMd5(String filename) {
	try {
	    InputStream fis = new FileInputStream(filename);
	    byte[] buffer = new byte[1024];
	    MessageDigest complete = MessageDigest.getInstance("MD5");
	    int numRead = fis.read(buffer);

	    while (numRead != -1) {
		if (numRead > 0) {
		    complete.update(buffer, 0, numRead);
		}
		numRead = fis.read(buffer);
	    }
	    
	    fis.close();
	    byte[] b = complete.digest();
	    
	    String result="";
	    for (int i=0; i<b.length; i++) {
		result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	    }
	    return result;
	} catch (Exception e) {
	    e.printStackTrace();
	    return "";
	}
    }

	public static String display(int type) {
		if (type == Checksum.Adler32) {
			return StrADLER32;
		}
		if (type == Checksum.Md5) {
			return StrMD5;
		}
		if (type == Checksum.CRC32) {
			return StrCRC32;
		}
		return null;
	}
	private static void compute(String command, String filepath, TMetaDataPathDetail result) {
		int type = Config._defaultChecksumType;

		String typeStr = Checksum.display(type);
		if (typeStr != null) {
		    result.setCheckSumType(typeStr);
		} else {
		   return;
		}
		String value = TPlatformUtil.execCmdWithOutput(command+" "+filepath, true);
		int pos = value.indexOf(" ");
		if (pos == -1) {
			result.setCheckSumValue(value);
		} else {
			result.setCheckSumValue(value.substring(0,pos));
		}
	}

    public static void handle(TMetaDataPathDetail result, File f, TSRMFileListingOption lsOption) {
	if (!lsOption.isDetailNeeded()) {
	    return;
	}

	try {
	    if (gov.lbl.srm.server.Config._doComputeFileChecksum) {
		String filepath = f.getCanonicalPath();
		if (gov.lbl.srm.server.Config._checksumCommand != null) {
            compute(gov.lbl.srm.server.Config._checksumCommand, filepath,result);
		} else {
			useAssignedType(result, filepath, Config._defaultChecksumType);
		}
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }

    private static void useAssignedType(TMetaDataPathDetail result, String filepath, int type) {
	if (type == Checksum.Adler32) {
	    useAdler(result, filepath);
	} else if (type == Checksum.Md5) {
	    useMd5(result, filepath);
	} else if (type == Checksum.CRC32) {
	    useCRC32(result, filepath);
	}
    }
	
    private static void useAdler(TMetaDataPathDetail result, String filepath) {
	result.setCheckSumType("ADLER32");
	//result.setCheckSumValue(String.valueOf(Checksum.getAdler32(filepath)));
	result.setCheckSumValue(Long.toHexString(Checksum.getAdler32(filepath)));
    }

    private static void useMd5(TMetaDataPathDetail result, String filepath) {
	result.setCheckSumType("MD5");
	result.setCheckSumValue(Checksum.getMd5(filepath));
    }

    private static void useCRC32(TMetaDataPathDetail result, String filepath) {
	result.setCheckSumType("CRC32");
	//result.setCheckSumValue(String.valueOf(Checksum.getCRC32(filepath)));
	result.setCheckSumValue(Long.toHexString(Checksum.getCRC32(filepath)));
    }
}
