REM Modify below path and values as necessary

set SRM_HOME=C:\documents\bestman
set JAVA_HOME=C:\jdk1.5.0_11
set X509_USER_PROXY=C:\documents\bestman\x509up_u1234
set GLOBUS_TCP_PORT_RANGE=6001,6500
set MAX_JAVA_HEAP=512
set MIN_JAVA_HEAP=512

set CLASSPATH=%SRM_HOME%\lib\bestman-diag.jar;%SRM_HOME%\lib\bestman2-client.jar;%SRM_HOME%\lib\bestman2-stub.jar;%SRM_HOME%\lib\bestman2-transfer.jar;%SRM_HOME%\lib\bestman2-tester-main.jar;%SRM_HOME%\lib\bestman2-tester-driver.jar;%SRM_HOME%\lib\axis\axis.jar;%SRM_HOME%\lib\axis\commons-discovery-0.2.jar;%SRM_HOME%\lib\axis\commons-logging-1.0.4.jar;%SRM_HOME%\lib\axis\jaxrpc.jar;%SRM_HOME%\lib\axis\wsdl4j-1.6.2.jar;%SRM_HOME%\lib\axis\xercesImpl-2.9.1.jar;%SRM_HOME%\lib\jglobus\cog-axis-1.8.0.jar;%SRM_HOME%\lib\jglobus\cog-jglobus-1.8.0.jar;%SRM_HOME%\lib\jglobus\cog-url-1.8.0.jar;%SRM_HOME%\lib\jglobus\cryptix-asn1.jar;%SRM_HOME%\lib\jglobus\cryptix32.jar;%SRM_HOME%\lib\jglobus\log4j-1.2.15.jar;%SRM_HOME%\lib\jglobus\puretls.jar

"%JAVA_HOME%\bin\java" -client -DGLOBUS_TCP_PORT_RANGE="%GLOBUS_TCP_PORT_RANGE%" -Xms%MIN_JAVA_HEAP%M -Xmx%MIN_JAVA_HEAP%M -Dlog4j.configuration="%SRM_HOME%\properties\log4j.properties" -DSRM.HOME="%SRM_HOME%" -Djava.security.auth.login.config=%SRM_HOME%/properties/authmod-win.properties BeStManDiagClient %*

