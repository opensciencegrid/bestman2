dnl AC_DEFUN(my_GET_SYSTYPE, [ MACHINE_SPEC=`/bin/uname -m` SYSTYPE= if test "X$MACHINE_SPEC" = "Xsun4u" ; then SYSTYPE=SOLARIS elif test "X$MACHINE_SPEC" = "Xi686" ; then SYSTYPE=LINUX elif test "X$MACHINE_SPEC" = "Xi86pc" ; then SYSTYPE=x86
dnl fi
dnl ])

dnl AC_DEFUN(my_GATEWAYMODE, [
dnl GATEWAYMODE=yes
dnl AC_ARG_ENABLE(gateway-mode,
dnl [  --enable-gateway-mode  (default=yes)    Enable BeStMan in Gateway mode],
dnl GATEWAYMODE=$enableval,
dnl )
dnl ])

dnl AC_DEFUN(my_FULLMODE, [
dnl FULLMODE=no
dnl AC_ARG_ENABLE(full-mode,
dnl [  --enable-full-mode  (default=no)    Enable BeStMan in Full mode],
dnl FULLMODE=$enableval,
dnl )
dnl ])

AC_DEFUN(my_BACKUPMODE, [
BACKUPMODE=yes
AC_ARG_ENABLE(backup,
[  --enable-backup  (default=yes)    Enable backup before configure],
BACKUPMODE=$enableval,
)
])

AC_DEFUN(my_SYSCONFPATH, [
SYSCONFPATH=
AC_ARG_WITH(sysconf-path,
[  --with-sysconf-path=<string> (default=$SRMHOME/etc/bestman2)  Specify the full path for the bestman2 sys conf],
SYSCONFPATH=$withval)
])

AC_DEFUN(my_MYCONFPATH, [
MYCONFPATH=
AC_ARG_WITH(bestman2-conf-path,
[  --with-bestman2-conf-path=<string> (default=$SRMHOME/conf/bestman2.rc)  Specify the full path for the bestman2 conf],
MYCONFPATH=$withval)
])


AC_DEFUN(my_BACKUPTAG, [
BACKUPTAG=
AC_ARG_WITH(backup-tag,
[  --with-backup-tag=<string> (default=current_date)  Specify the tag for backups],
BACKUPTAG=$withval)
])

AC_DEFUN(my_JETTYDEBUG, [
JETTYDEBUGENABLED=no
AC_ARG_ENABLE(debug-jetty,
[  --enable-debug-jetty (default=no)    Enable debugging Jetty requests],
JETTYDEBUGENABLED=$enableval,
)
])


AC_DEFUN(my_CHECKSUMLISTING, [
CHECKSUMLISTING=no
AC_ARG_ENABLE(checksum-listing,
[  --enable-checksum-listing  (default=no)    Enable checksum returns in file browsing],
CHECKSUMLISTING=$enableval,
)
])

AC_DEFUN(my_CHECKSUMTYPE, [
CHECKSUMTYPE=adler32
AC_ARG_WITH(checksum-type,
[  --with-checksum-type=<string> (default=adler32)  Specify the checksum type (adler32, md5, crc32) ],
CHECKSUMTYPE=$withval)
])

AC_DEFUN(my_CACHEDIDLIFETIME, [
CACHEDIDLIFETIME=1800
AC_ARG_WITH(cached-id-lifetime,
[  --with-cached-id-lifetime=<INT>  (default=1800)  Specify the default
                                    lifetime of cached id mapping in seconds],
CACHEDIDLIFETIME=$withval
)
])

AC_DEFUN(my_MSSENABLE, [
MSSENABLE=no
AC_ARG_ENABLE(mss,
[  --enable-mss  (default=no)    Enable MSS support],
MSSENABLE=$enableval,
)
])

AC_DEFUN(my_PATHFORTOKEN, [
PATHFORTOKEN=yes
AC_ARG_ENABLE(pathfortoken,
[  --enable-pathfortoken  (default=yes)    Enable PathForToken mode],
PATHFORTOKEN=$enableval,
)
])

AC_DEFUN(my_SERVERONLY, [
SERVERONLY=no
AC_ARG_ENABLE(serveronly, 
[  --enable-serveronly  (default=no)    Install BeStMan server only],
SERVERONLY=$enableval,
)
])

AC_DEFUN(my_CLIENTONLY, [
CLIENTONLY=no
AC_ARG_ENABLE(clientonly, 
[  --enable-clientonly  (default=no)    Install SRM client only],
CLIENTONLY=$enableval,
)
])

AC_DEFUN(my_TESTERONLY, [
TESTERONLY=no
AC_ARG_ENABLE(testeronly, 
[  --enable-testeronly  (default=no)    Install SRM-Tester only],
TESTERONLY=$enableval,
)
])

AC_DEFUN(my_GUMSENABLED, [
GUMSENABLED=no
AC_ARG_ENABLE(gums, 
[  --enable-gums  (default=no)    Enable GUMS interface],
GUMSENABLED=$enableval,
)
])

AC_DEFUN(my_GUMSURL, [
GUMSURL=
AC_ARG_WITH(gums-url,
[  --with-gums-url=<URL>   Specify the GUMS service URL],
GUMSURL=$withval
)
])

AC_DEFUN(my_GUMSDN, [
GUMSDN=
AC_ARG_WITH(gums-dn,
[  --with-gums-dn=<DN>   Specify the service DN for GUMS interface],
GUMSDN=$withval
)
])

AC_DEFUN(my_GUMSCERT_FILE_PATH, [
GUMSCERT_FILE_PATH=
AC_ARG_WITH(gums-certfile-path,
[  --with-gums-certfile-path=<PATH> (default=SRM service cert file path)
                            Specify the GUMS client Grid Certificate file path],
if test "$withval" ; then
    if test -f "$withval" ; then
         GUMSCERT_FILE_PATH=$withval
    else
         AC_MSG_WARN([invalid GUMSCertFileName path specified])
    fi
fi
)
])

AC_DEFUN(my_GUMSKEY_FILE_PATH, [
GUMSKEY_FILE_PATH=
AC_ARG_WITH(gums-keyfile-path,
[  --with-gums-keyfile-path=<PATH>  (default=SRM service key file path)
                        Specify the GUMS client Grid Certificate Key file path],
if test "$withval" ; then
    if test -f "$withval" ; then
         GUMSKEY_FILE_PATH=$withval
    else
         AC_MSG_WARN([invalid GUMSKeyFileName path specified])
    fi
fi
)
])

AC_DEFUN(my_GUMSPROXY_FILE_PATH, [
GUMSPROXY_FILE_PATH=
AC_ARG_WITH(gums-proxyfile-path,
[  --with-gums-proxyfile-path=<PATH>   Specify the GUMS client Grid proxy file path],
if test -f "$withval" ; then
     GUMSPROXY_FILE_PATH=$withval
else
     AC_MSG_WARN([invalid GUMS ProxyFileName path specified])
fi
)
])



AC_DEFUN(my_SRM_HOME, [
SRM_HOME=
AC_ARG_WITH(srm-home, 
[  --with-srm-home=<PATH>            Specify the SRM_HOME path],
if test -d "$withval" ; then
	if test -f "$withval/lib/bestman2-stub.jar" ; then
		SRM_HOME=$withval
	else
		AC_MSG_WARN([invalid SRM_HOME path specified]) 
	fi
fi
)
if test "$SRM_HOME" = "" ; then
	AC_MSG_CHECKING([for right SRM_HOME path])
	tmp=`pwd 2>&1`
    tmp=`echo $tmp | sed 's%/setup%%'`
	if test -f "$tmp/lib/bestman2-stub.jar" ; then
		SRM_HOME=$tmp		
		AC_MSG_RESULT([SRM_HOME found])
	else
	    AC_MSG_RESULT([SRM_HOME not found])
	fi	
fi
if test "$SRM_HOME" = "" ; then
	AC_MSG_ERROR([SRM_HOME is not set.])
fi
])	


AC_DEFUN(my_JAVA_HOME, [
JAVA_HOME=
AC_ARG_WITH(java-home,
[  --with-java-home=<PATH>       Specify the JAVA_HOME directory (> 1.6.0_01)],
if test -d "$withval" -a -f "$withval/bin/java" ; then
	JAVA_HOME=$withval
else
	AC_MSG_WARN([invalid JAVA path specified]) 
fi
)
if test "$JAVA_HOME" = "" ; then
	AC_MSG_CHECKING([for correct JAVA path])
	tmp=`which java 2>&1`
	if test -f "$tmp" ; then
		JAVA_HOME=`echo $tmp | sed 's%/bin/java%%'`
		AC_MSG_RESULT([JAVA found])
	else
		AC_MSG_RESULT([java not found])
	fi
fi
if test "$JAVA_HOME" = "" ; then
	AC_MSG_ERROR([JAVA_HOME is not set])
fi
])

AC_DEFUN(my_JAVACHECK, [
JAVACHECK=yes
AC_ARG_ENABLE(java-version-check,
[  --enable-java-version-check  (default=yes)    Enable Java version check],
JAVACHECK=$enableval,
)
])


AC_DEFUN(my_GLOBUS_TCP_PORT_RANGE, [
GLOBUS_TCP_PORT_RANGE=
AC_ARG_WITH(globus-tcp-port-range,
[  --with-globus-tcp-port-range=<VALUES>   Specify the GLOBUS_TCP_PORT_RANGE],
GLOBUS_TCP_PORT_RANGE=$withval
)
])

AC_DEFUN(my_GLOBUS_TCP_SOURCE_RANGE, [
GLOBUS_TCP_SOURCE_RANGE=
AC_ARG_WITH(globus-tcp-source-range,
[  --with-globus-tcp-source-range=<VALUES>   Specify the GLOBUS_TCP_SOURCE_RANGE],
GLOBUS_TCP_SOURCE_RANGE=$withval
)
])

AC_DEFUN(my_SRM_OWNER, [
SRM_OWNER=root
AC_ARG_WITH(srm-owner, 
[  --with-srm-owner=<LOGIN> (default=root)  Specify the bestman srm server owner],
SRM_OWNER=$withval
)
])

dnl AC_DEFUN(my_SRM_NAME, [
dnl SRM_NAME=server
dnl AC_ARG_WITH(srm-name, 
dnl [  --with-srm-name=<NAME> (default=server)  Specify the bestman server name],
dnl SRM_NAME=$withval
dnl )
dnl ])

dnl AC_DEFUN(my_BERKELEYDB, [
dnl BERKELEYDB=yes
dnl AC_ARG_ENABLE(berkeleydb, 
dnl [  --enable-berkeleydb  (default=yes)    Enable Berkeley DB as an internal management component],
dnl BERKELEYDB=$enableval,
dnl )
dnl ])

dnl AC_DEFUN(my_SRMCACHEKEY, [
dnl SRMCACHEKEY=yes
dnl AC_ARG_ENABLE(srmcache-keyword, 
dnl [  --enable-srmcache-keyword  (default=no)    Enable srmcache as the keyword in cache management],
dnl SRMCACHEKEY=$enableval,
dnl )
dnl ])

dnl AC_DEFUN(my_TWOQUEUES, [
dnl TWOQUEUES=yes
dnl AC_ARG_ENABLE(twoqueues, 
dnl [  --enable-twoqueues   (default=no)    Enable two separate queue management for incoming and outgoing file transfers],
dnl TWOQUEUES=$enableval,
dnl )
dnl ])

AC_DEFUN(my_MAX_JAVA_HEAP, [
MAX_JAVA_HEAP=1024
AC_ARG_WITH(max-java-heap, 
[  --with-max-java-heap=<INT> (default=1024)  Specify the max java heap size in MB],
MAX_JAVA_HEAP=$withval
)
])

AC_DEFUN(my_JAVA_CLIENT_MAX_HEAP, [
JAVA_CLIENT_MAX_HEAP=512
AC_ARG_WITH(java-client-max-heap, 
[  --with-java-client-max-heap=<INT> (default=512M)  Specify the max java heap size in MB for SRM clients],
JAVA_CLIENT_MAX_HEAP=$withval
)
])

AC_DEFUN(my_JAVA_STACK_SIZE, [
JAVA_STACK_SIZE=
AC_ARG_WITH(java-stack-size, 
[  --with-java-stack-size=<INT>   Specify the java stack size in KB. If defined, recommend 128],
JAVA_STACK_SIZE=$withval
)
])

AC_DEFUN(my_JAVA_CLIENT_STACK_SIZE, [
JAVA_CLIENT_STACK_SIZE=
AC_ARG_WITH(java-client-stack-size, 
[  --with-java-client-stack-size=<INT>   Specify the java stack size in KB for SRM clients. If defined, recommend 128],
JAVA_CLIENT_STACK_SIZE=$withval
)
])

AC_DEFUN(my_JAVA_CLIENT_MIN_HEAP, [
JAVA_CLIENT_MIN_HEAP=32
AC_ARG_WITH(java-client-min-heap,
[  --with-java-clienyt-min-heap=<INT> (default=32)  Specify the min java heap size for SRM clients in MB],
JAVA_CLIENT_MIN_HEAP=$withval
)
])

AC_DEFUN(my_MAX_CONTAINER_THREADS, [
MAX_CONTAINER_THREADS=256
AC_ARG_WITH(max-container-threads,
[  --with-max-container-threads=<INT> (default=256)  Specify the web service container max thread pool size (defaults 256 for 1024MB max heap size)],
MAX_CONTAINER_THREADS=$withval
)
])

AC_DEFUN(my_MIN_CONTAINER_THREADS, [
MIN_CONTAINER_THREADS=10
AC_ARG_WITH(min-container-threads,
[  --with-min-container-threads=<INT> (default=10)  Specify the web service container min thread pool size (defaults 10)],
MIN_CONTAINER_THREADS=$withval
)
])


dnl AC_DEFUN(my_HTTP_PORT, [
dnl HTTP_PORT=8080
dnl AC_ARG_WITH(http-port, 
dnl [  --with-http-port=<PORT> (default=8080)  Specify the http port],
dnl HTTP_PORT=$withval
dnl )
dnl ])

AC_DEFUN(my_HTTPS_PORT, [
HTTPS_PORT=8443
AC_ARG_WITH(https-port, 
[  --with-https-port=<PORT> (default=8443)  Specify the https port],
HTTPS_PORT=$withval
)
])


AC_DEFUN(my_VERSION_COMPARE, [
if test "$1" -lt "$4" ; then
	ver=yes
elif test "$1" -eq "$4" -a "$2" -lt "$5" ; then
	ver=yes
elif test "$1" -eq "$4" -a "$2" -eq "$5" -a "$3" -le "$6" ; then
	ver=yes
else
	ver=no 
fi
])


AC_DEFUN(my_CHECK_VER_JAVA, [
if test -f "$JAVA_HOME/bin/java" -a -d "$JAVA_HOME"; then
	j_ver=`$JAVA_HOME/bin/java -version 2>&1`
	changequote(<<,>>)
	j_ver=`echo $j_ver | sed 's/[^0-9. ]//g'`
	changequote([,])
	Ver_getter $j_ver
	Ver_separator $varget
	ver=
	echo "current java version is $var1.$var2.$var3"
	my_VERSION_COMPARE(1, 6, 001, $var1, $var2, $var3)
	AC_MSG_CHECKING([for JAVA version ( > 1.6.0_01)])
	if test "$ver" = "yes" ; then
		AC_MSG_RESULT([ok])
	else
		AC_MSG_WARN([invalid JAVA specified])
		AC_MSG_RESULT([higher version needed])
	    AC_MSG_ERROR([usable java version not found])
	fi
else
	AC_MSG_ERROR([java not found])
fi
])

AC_DEFUN(my_CHECK_VER_JAVA_5, [
if test -f "$JAVA_HOME/bin/java" -a -d "$JAVA_HOME"; then
    j_ver=`$JAVA_HOME/bin/java -version 2>&1`
    changequote(<<,>>)
    j_ver=`echo $j_ver | sed 's/[^0-9. ]//g'`
    changequote([,])
    Ver_getter $j_ver
    Ver_separator $varget
    ver=
    echo "current java version is $var1.$var2.$var3"
    my_VERSION_COMPARE(1, 5, 001, $var1, $var2, $var3)
    AC_MSG_CHECKING([for JAVA version ( > 1.5.0_01)])
    if test "$ver" = "yes" ; then
        AC_MSG_RESULT([ok])
    else
        AC_MSG_WARN([invalid JAVA specified])
        AC_MSG_RESULT([higher version needed])
        AC_MSG_ERROR([usable java version not found])
    fi
else
    AC_MSG_ERROR([java not found])
fi
])

AC_DEFUN(my_CONNECTOR_QUEUE_SIZE, [
CONNECTOR_QUEUE_SIZE=
AC_ARG_WITH(connector-queue-size,
[  --with-connector-queue-size=<INT>   Specify the size of the http connector queue size],
CONNECTOR_QUEUE_SIZE=$withval
)
])

AC_DEFUN(my_CONNECTION_ACCEPTOR_SIZE, [
CONNECTION_ACCEPTOR_SIZE=
AC_ARG_WITH(connection-acceptor-thread-size,
[  --with-connection-acceptor-thread-size=<INT>   Specify number of acceptor threads 
                                 available for the server's channel connector],
CONNECTION_ACCEPTOR_SIZE=$withval
)
])

AC_DEFUN(my_EVENTLOG, [
EVENTLOG=yes
AC_ARG_ENABLE(eventlog,
[  --enable-eventlog  (default=yes)    Enable Event logging],
EVENTLOG=$enableval,
)
])

AC_DEFUN(my_EVENTLOG_NUM, [
EVENTLOG_NUM=
AC_ARG_WITH(eventlog-num,
[  --with-eventlog-num=<INT>   Specify the number of event log files],
EVENTLOG_NUM=$withval
)
])

AC_DEFUN(my_EVENTLOG_SIZE, [
EVENTLOG_SIZE=
AC_ARG_WITH(eventlog-size,
[  --with-eventlog-size=<INT>   Specify the size of each event log file],
EVENTLOG_SIZE=$withval
)
])


AC_DEFUN(my_EVENTLOG_PATH, [
EVENTLOG_PATH=/var/log
AC_ARG_WITH(eventlog-path, 
[  --with-eventlog-path=<PATH> (default=/var/log)  Specify the EventLogFile 
                                                   directory path],
if test -d "$withval" ; then
     EVENTLOG_PATH=$withval
else
     AC_MSG_WARN([invalid EventLogFile directory path specified])
fi
)
])

AC_DEFUN(my_OUTPUTLOG_PATH, [
OUTPUTLOG_PATH=/var/log
AC_ARG_WITH(outputlog-path, 
[  --with-outputlog-path=<PATH> (default=/var/log)  Specify the output log
                                                   directory path],
if test -d "$withval" ; then
     OUTPUTLOG_PATH=$withval
else
     AC_MSG_WARN([invalid output log directory path specified])
fi
)
])

AC_DEFUN(my_PID_PATH, [
PID_PATH=/var/run
AC_ARG_WITH(pid-path, 
[  --with-pid-path=<PATH> (default=/var/run)  Specify the PID directory path],
if test -d "$withval" ; then
     PID_PATH=$withval
else
     AC_MSG_WARN([invalid PID path specified])
fi
)
])



AC_DEFUN(my_EVENTLOG_LEVEL, [
EVENTLOG_LEVEL=INFO
AC_ARG_WITH(eventlog-level,
[  --with-eventlog-level=<STRING> (default=INFO) Specify the eventLogLevel],
EVENTLOG_LEVEL=$withval
)
])

AC_DEFUN(my_CACHELOG_PATH, [
CACHELOG_PATH=
AC_ARG_WITH(cachelog-path, 
[  --with-cachelog-path=<PATH> (default=/var/log)  Specify the CacheLogFile 
                                                   directory path],
if test -d "$withval" ; then
     CACHELOG_PATH=$withval
else
     AC_MSG_WARN([invalid CacheLogFile directory path specified])
fi
)
])

AC_DEFUN(my_REPLICA_STORAGE_PATH, [
REPLICA_STORAGE_PATH=
AC_ARG_WITH(replica-storage-path, 
[  --with-replica-storage-path=<PATH>   Specify the ReplicaQualityStorage 
                                        directory path],
if test -d "$withval" ; then
     REPLICA_STORAGE_PATH=$withval
else
     AC_MSG_WARN([invalid ReplicaQualityStorage directory path specified])
fi
)
])

AC_DEFUN(my_REPLICA_STORAGE_SIZE, [
REPLICA_STORAGE_SIZE=
AC_ARG_WITH(replica-storage-size, 
[  --with-replica-storage-size=<INT>   Specify the ReplicaQualityStorage 
                                       Size in MB],
REPLICA_STORAGE_SIZE=$withval
)
])

AC_DEFUN(my_OUTPUT_STORAGE_PATH, [
OUTPUT_STORAGE_PATH=
AC_ARG_WITH(output-storage-path, 
[  --with-output-storage-path=<PATH>   Specify the OutputQualityStorage
                                       directory path],
if test -d "$withval" ; then
     OUTPUT_STORAGE_PATH=$withval
else
     AC_MSG_WARN([invalid OutputQualityStorage directory path specified])
fi
)
])

AC_DEFUN(my_OUTPUT_STORAGE_SIZE, [
OUTPUT_STORAGE_SIZE=
AC_ARG_WITH(output-storage-size, 
[  --with-output-storage-size=<INT>   Specify the OutputQualityStorage 
                                      Size in MB],
OUTPUT_STORAGE_SIZE=$withval
)
])

AC_DEFUN(my_CUSTODIAL_STORAGE_PATH, [
CUSTODIAL_STORAGE_PATH=
AC_ARG_WITH(custodial-storage-path, 
[  --with-custodial-storage-path=<PATH>   Specify the CustodialQualityStorage 
                                          directory path],
if test -d "$withval" ; then
     CUSTODIAL_STORAGE_PATH=$withval
else
     AC_MSG_WARN([invalid CustodialQualityStorage directory path specified])
fi
)
])

AC_DEFUN(my_CUSTODIAL_STORAGE_SIZE, [
CUSTODIAL_STORAGE_SIZE=
AC_ARG_WITH(custodial-storage-size, 
[  --with-custodial-storage-size=<INT>   Specify the CustodialQualityStorage 
                                         Size in MB],
CUSTODIAL_STORAGE_SIZE=$withval
)
])

AC_DEFUN(my_MAX_USERS, [
MAX_USERS=100
AC_ARG_WITH(max-users, 
[  --with-max-users=<INT>   (default=100)   Specify the Maximum Number 
                                            Of Active Users],
MAX_USERS=$withval
)
])

AC_DEFUN(my_MAX_FILES, [
MAX_FILES=1000000
AC_ARG_WITH(max-filerequests, 
[  --with-max-filerequests=<INT>   (default=1000000)   Specify the Maximum 
                                            Number of files request],
MAX_FILES=$withval
)
])

AC_DEFUN(my_CONCURRENCY, [
CONCURRENCY=40
AC_ARG_WITH(concurrency, 
[  --with-concurrency=<INT>   (default=40)   Specify the number of 
                                             concurrent requests],
CONCURRENCY=$withval
)
])

AC_DEFUN(my_CONCURRENT_FILE_TRANSFER, [
CONCURRENT_FILE_TRANSFER=10
AC_ARG_WITH(concurrent-filetransfer, 
[  --with-concurrent-filetransfer=<INT>  (default=10)  Specify the number
                                          of concurrent file transfers],
CONCURRENT_FILE_TRANSFER=$withval
)
])

AC_DEFUN(my_GRIDFTP_STREAMS, [
GRIDFTP_STREAMS=1
AC_ARG_WITH(gridftp-parallel-streams, 
[  --with-gridftp-parallel-streams=<INT>  (default=1)  Specify the number
                                          of gridftp parallel streams],
GRIDFTP_STREAMS=$withval
)
])

AC_DEFUN(my_GRIDFTP_BUFFERSIZE, [
GRIDFTP_BUFFERSIZE=1048576
AC_ARG_WITH(gridftp-buffersize, 
[  --with-gridftp-buffersize=<INT>  (default=1048576)  Specify the gridftp 
                                    buffer size in bytes],
GRIDFTP_BUFFERSIZE=$withval
)
])

AC_DEFUN(my_DEFAULT_FILESIZE, [
DEFAULT_FILESIZE=500
AC_ARG_WITH(default-filesize, 
[  --with-default-filesize=<INT>  (default=500)  Specify the default file 
                                    size in MB],
DEFAULT_FILESIZE=$withval
)
])


AC_DEFUN(my_FILELIFETIME, [
FILELIFETIME=1800
AC_ARG_WITH(volatile-file-lifetime, 
[  --with-volatile-file-lifetime=<INT>  (default=1800)  Specify the default
                                    lifetime of volatile files in seconds],
FILELIFETIME=$withval
)
])

AC_DEFUN(my_SPACEFILELIFETIME, [
SPACEFILELIFETIME=1800
AC_ARG_WITH(space-file-lifetime, 
[  --with-space-file-lifetime=<INT>  (default=1800)  Specify the default
                                  lifetime of files in public space in seconds],
SPACEFILELIFETIME=$withval
)
])

AC_DEFUN(my_INACTIVE_TIMEOUT, [
INACTIVE_TIMEOUT=300
AC_ARG_WITH(inactive-transfer-timeout, 
[  --with-inactive-transfer-timeout=<INT>  (default=300)  Specify the default
                   time out value for inactive user file transfer in seconds],
INACTIVE_TIMEOUT=$withval
)
])

AC_DEFUN(my_PUBLIC_SPACE, [
PUBLIC_SPACE=
AC_ARG_WITH(public-space-size, 
[  --with-public-space-size=<INT>     Specify the default size for SRM 
                                      owned volatile space in MB],
PUBLIC_SPACE=$withval
)
])

AC_DEFUN(my_RESERVED_SIZE, [
RESERVED_SIZE=1000
AC_ARG_WITH(default-space-size, 
[  --with-default-space-size=<INT>  (default=1000)  Specify the default 
                                    size for space reservation in MB],
RESERVED_SIZE=$withval
)
])

AC_DEFUN(my_CA_DIR_PATH, [
CA_DIR_PATH=/etc/grid-security/certificates
AC_ARG_WITH(cacert-path, 
[  --with-cacert-path=<PATH> (default=/etc/grid-security/certificates) 
                             Specify the Grid CA Certificate directory path],
if test -d "$withval" ; then
     CA_DIR_PATH=$withval
else
     AC_MSG_WARN([invalid CA_DIR_PATH specified])
fi
)
])

AC_DEFUN(my_VOMS_DIR_PATH, [
VOMS_DIR_PATH=
AC_ARG_WITH(vomsdir-path,
[  --with-vomsdir-path=<PATH> Specify the VOMS directory path],
VOMS_DIR_PATH=$withval
)
])

AC_DEFUN(my_CERT_FILE_PATH, [
CERT_FILE_PATH=/etc/grid-security/hostcert.pem
AC_ARG_WITH(certfile-path, 
[  --with-certfile-path=<PATH> (default=/etc/grid-security/hostcert.pem) 
                                Specify the Grid Certificate file path],
if test -f "$withval" ; then
     CERT_FILE_PATH=$withval
else
     AC_MSG_ERROR([invalid CertFileName path specified])
fi
)
])

AC_DEFUN(my_KEY_FILE_PATH, [
KEY_FILE_PATH=/etc/grid-security/hostkey.pem
AC_ARG_WITH(keyfile-path, 
[  --with-keyfile-path=<PATH>  (default=/etc/grid-security/hostkey.pem) 
                                Specify the Grid Certificate Key file path],
if test -f "$withval" ; then
     KEY_FILE_PATH=$withval
else
     AC_MSG_ERROR([invalid KeyFileName path specified])
fi
)
])

AC_DEFUN(my_PROXY_FILE_PATH, [
PROXY_FILE_PATH=
AC_ARG_WITH(proxyfile-path, 
[  --with-proxyfile-path=<PATH>   Specify the Grid proxy file path],
if test -f "$withval" ; then
     PROXY_FILE_PATH=$withval
else
     AC_MSG_WARN([invalid ProxyFileName path specified])
fi
)
])

AC_DEFUN(my_GRIDMAP_PATH, [
GRIDMAP_PATH=
AC_ARG_WITH(gridmap-path, 
[  --with-gridmap-path=<PATH>  (default=/etc/grid-security/grid-mapfile) 
                               Specify the grid-mapfile path],
if test -f "$withval" ; then
     GRIDMAP_PATH=$withval
else
     AC_MSG_WARN([invalid GridMapFile path specified])
fi
)
])

AC_DEFUN(my_MAX_MSS_CONNECTION, [
MAX_MSS_CONNECTION=5
AC_ARG_WITH(max-mss-connection, 
[  --with-max-mss-connection=<INT>  (default=5)  Specify the maximum 
                                     MSS file transfers when supported],
MAX_MSS_CONNECTION=$withval
)
])

AC_DEFUN(my_MSS_TIMEOUT, [
MSS_TIMEOUT=600000
AC_ARG_WITH(mss-timtout, 
[  --with-mss-timeout=<INT>  (default=600)  Specify the MSS connection 
                              timeout in seconds when supported],
MSS_TIMEOUT=$withval
)
])

AC_DEFUN(my_PROTOCOL_POLICY, [
PROTOCOL_POLICY=
AC_ARG_WITH(protocol-selection-policy,
[  --with-protocol-selection-policy=<string>   Specify definition of transfer protocol selection policy ],
PROTOCOL_POLICY=$withval)
])


AC_DEFUN(my_PLUGIN_PATH, [
PLUGIN_PATH=
AC_ARG_WITH(plugin-path, 
[  --with-plugin-path=<PATH>   Specify the plugin library directory 
                               path when supported],
if test -d "$withval" ; then
     PLUGIN_PATH=$withval
else
     AC_MSG_WARN([invalid plugin lib directory path specified])
fi
)
])

AC_DEFUN(my_PUBLIC_SPACE_PROPORTION, [
PUBLIC_SPACE_PROPORTION=80
AC_ARG_WITH(, 
[  --with-public-space-proportion=<INT> (default=80)  Specify default
                          size for SRM owned volatile space in percentage],
PUBLIC_SPACE_PROPORTION=$withval
)
])

AC_DEFUN(my_GUC_GLOBUS_PATH, [
GUC_GLOBUS_PATH=
AC_ARG_WITH(globus-location,
[  --with-globus-location=<PATH>            Specify the GLOBUS_LOCATION path],
if test -d "$withval" ; then
    if test -f "$withval/bin/globus-url-copy" ; then
        GUC_GLOBUS_PATH=$withval
    else
        AC_MSG_WARN([invalid GLOBUS_LOCATION specified])
        AC_MSG_ERROR([invalid GLOBUS_LOCATION provided])
    fi
fi
)
if test "$GUC_GLOBUS_PATH" = "" ; then
    AC_MSG_CHECKING([for right GLOBUS_LOCATION path])
    tmp=`echo $GLOBUS_LOCATION`
    if test -f "$tmp/bin/globus-url-copy" ; then
        GUC_GLOBUS_PATH=$tmp
        AC_MSG_RESULT([GLOBUS_LOCATION found])
    fi
fi
dnl if test "$GUC_GLOBUS_PATH" = "" ; then
dnl     AC_MSG_WARN([GLOBUS_LOCATION is not set.])
dnl fi
])

AC_DEFUN(my_GSIFTPFSM, [
GSIFTPFSM=no
AC_ARG_ENABLE(gsiftpfsmng, 
[  --enable-gsiftpfsmng  (default=no)   Use GridFTP to manage file system],
GSIFTPFSM=$enableval,
)
])

AC_DEFUN(my_SUDOFSM, [
SUDOFSM=no
AC_ARG_ENABLE(sudofsmng, 
[  --enable-sudofsmng  (default=no)   Use Sudo to manage file system],
SUDOFSM=$enableval,
)
])

AC_DEFUN(my_SUDOLS, [
SUDOLS=no
AC_ARG_ENABLE(sudols,  
[  --enable-sudols  (default=no)   Use Sudo to manage file system browsing (ls)],
SUDOLS=$enableval,
)
])

dnl AC_DEFUN(my_APPENDTURL, [
dnl APPENDTURL=oss.cgroup
dnl AC_ARG_WITH(append-turl, 
dnl [  --with-append-turl=<string>   Specify the appending name to TURL 
dnl                                  for space token when supported],
dnl APPENDTURL=$withval
dnl )
dnl ])

AC_DEFUN(my_TOKENSLIST, [
TOKENSLIST=
AC_ARG_WITH(tokens-list,
[  --with-tokens-list=<string>   Specify the static tokens list with 
                                 its size info when supported (refer to manual)],
TOKENSLIST=$withval
)
])

AC_DEFUN(my_CHECKFILEFS, [
CHECKFILEFS=yes
AC_ARG_ENABLE(checkfile-fs,
[  --enable-checkfile-fs  (default=yes)   Use file system to check file size in gateway mode],
CHECKFILEFS=$enableval,
)
])

AC_DEFUN(my_CHECKFILEGSIFTP, [
CHECKFILEGSIFTP=no
AC_ARG_ENABLE(checkfile-gsiftp,
[  --enable-checkfile-gsiftp  (default=no)   Use gsiftp to check file size in gateway mode],
CHECKFILEGSIFTP=$enableval,
)
])

AC_DEFUN(my_FS_CONCURRENCY, [
FS_CONCURRENCY=
AC_ARG_WITH(concurrent-fs,
[  --with-concurrent-fs=<INT>  (default=the value from with-concurrency)   Specify the number of concurrent file system involved-operations processiong],
FS_CONCURRENCY=$withval
)
])

AC_DEFUN(my_TSERVERS, [
TSERVERS=
AC_ARG_WITH(transfer-servers,
[  --with-transfer-servers=<string>   Specify supported file transfer servers ],
TSERVERS=$withval)
])

AC_DEFUN(my_BLOCKEDPATHS, [
BLOCKEDPATHS=
AC_ARG_WITH(blocked-paths,
[  --with-blocked-paths=<string>   Specify Non-accessible paths (in addition to /;/etc;/var) ],
BLOCKEDPATHS=$withval)
])

AC_DEFUN(my_ALLOWEDPATHS, [
ALLOWEDPATHS=
AC_ARG_WITH(allowed-paths,
[  --with-allowed-paths=<string>   Specify accessible paths only (separated by semi-colon) ],
ALLOWEDPATHS=$withval)
])

AC_DEFUN(my_EXTRALIBS, [
EXTRALIBS=
AC_ARG_WITH(extra-libs,
[  --with-extra-libs=<string>   Specify extra libraries definitions to the bestman ],
EXTRALIBS=$withval)
])

AC_DEFUN(my_USER_SPACE_KEY, [
USER_SPACE_KEY=
AC_ARG_WITH(user-space-key,
[  --with-user-space-key=<string>   Specify user space keys. format: (key1=/path1)(key2=/path2) ],
USER_SPACE_KEY=$withval)
])

AC_DEFUN(my_VOMS_VALIDATION, [
VOMS_VALIDATION=no
AC_ARG_ENABLE(voms_validation,
[  --enable-voms-validation  (default=no)  Enable VOMS validation],
VOMS_VALIDATION=$enableval,
)
])

AC_DEFUN(my_CHECKSUM_CALLOUT, [
CHECKSUM_CALLOUT=
AC_ARG_WITH(checksum-callout,
[  --with-checksum-callout=<PATH>   Specify the path for the checksum callout command ],
CHECKSUM_CALLOUT=$withval)
])

AC_DEFUN(my_JAR_VALIDATOR, [
if test "X$1" = "XNOBUILD"; then
   mJARNAME=
   mJARPATH=
else
   mJARNAME=`grep $3 $1 | tr '=' '\n' | sed -e 's/^.*\///'  | grep "^$3.*.jar"`
   mJARPATH=`grep $mJARNAME $1 | tr '=' '\n' | grep "/$mJARNAME"`
fi
if test "X$mJARPATH" = "X" ; then
   mJARNAME=`echo $2 | tr ':' '\n' | sed -e 's/^.*\///'  | grep "^$3.*.jar"`
   $5=$mJARNAME
   if test "$mJARNAME" ; then
      mJARPATH=`echo $2 | tr ':' '\n' | grep "/$mJARNAME"`
      $6=$mJARPATH
      if test -f "$mJARPATH" ; then
         AC_MSG_RESULT([VALIDATED: $3 $mJARNAME for version $4 is found in $mJARPATH.]) 
      else 
         AC_MSG_ERROR([FAILED: $3 $mJARNAME for version $4 is not found in $mJARPATH.])
      fi
   else
      AC_MSG_ERROR([FAILED: $3 $4 is not found in $1 nor in CLASSPATH $2.])
   fi
else
   $5=$mJARNAME
   $6=$mJARPATH
   AC_MSG_RESULT([VALIDATED: $3 $mJARNAME for version $4 is found in $mJARPATH.]) 
fi
])

