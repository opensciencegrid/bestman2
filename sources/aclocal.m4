AC_DEFUN(my_ANT_HOME, [
ANT_HOME=
AC_ARG_WITH(ant-home,
[  --with-ant-home=<PATH>            Specify the ANT_HOME path],
if test -d "$withval" ; then
    if test -f "$withval/bin/ant" ; then
        ANT_HOME=$withval
        ANT_BINPATH=$withval/bin/ant
    else
        AC_MSG_ERROR([invalid ANT_HOME path specified])
    fi
fi
)
if test "$ANT_HOME" = "" ; then
    AC_MSG_CHECKING([for right ANT_HOME path])
	tmp=`which ant 2>&1`
    if test -f "$tmp" ; then
        ANT_HOME=`echo $tmp | sed 's%/bin/ant%%'`
        ANT_BINPATH=$tmp
        AC_MSG_RESULT([ANT_HOME found])
    else
        AC_MSG_RESULT([ANT_HOME not found])
    fi
fi
if test "$ANT_BINPATH" = "" ; then
    AC_MSG_ERROR([ANT_HOME is not set.])
fi
])


AC_DEFUN(my_JAVA_HOME, [
JAVA_HOME=
AC_ARG_WITH(java-home,
[  --with-java-home=<PATH>       Specify the JAVA_HOME directory (> 1.7.0)],
if test -d "$withval" -a -f "$withval/bin/javac" ; then
	JAVA_HOME=$withval
else
	AC_MSG_WARN([invalid JAVA path specified]) 
fi
)
if test "$JAVA_HOME" = "" ; then
	AC_MSG_CHECKING([for correct JAVA path])
	tmp=`which javac 2>&1`
	if test -f "$tmp" ; then
		JAVA_HOME=`echo $tmp | sed 's%/bin/javac%%'`
		AC_MSG_RESULT([JAVA found])
	else
		AC_MSG_RESULT([javac not found])
	fi
fi
if test "$JAVA_HOME" = "" ; then
	AC_MSG_ERROR([JAVA_HOME is not set])
fi
])


AC_DEFUN(my_VERSION_COMPARE, [
if test $1 -lt $4 ; then
	ver=yes
elif test $1 -eq $4 -a $2 -lt $5 ; then
	ver=yes
elif test $1 -eq $4 -a $2 -le $5 -a $3 -le $6 ; then
	ver=yes
else
	ver=no 
fi
])

AC_DEFUN(my_CHECK_VER_JAVA, [
if test -f $JAVA_HOME/bin/javac -a -d "$JAVA_HOME"; then
	j_ver=`$JAVA_HOME/bin/javac -version 2>&1`
	changequote(<<,>>)
	j_ver=`echo $j_ver | sed 's/[^0-9. ]//g'`
	changequote([,])
	Ver_getter $j_ver
	Ver_separator $varget
	ver=
	my_VERSION_COMPARE(1, 7, 0, $var1, $var2, $var3)
	AC_MSG_CHECKING([for JAVAC version ( >= 1.7.0)])
	if test "$ver" = "yes" ; then
		AC_MSG_RESULT([ok])
	else
		AC_MSG_RESULT([higher version needed])
		AC_MSG_WARN([invalid JAVAC specified])
	fi
else
	AC_MSG_WARN([javac not found])
fi
])

AC_DEFUN(my_JAR_FINDER, [
dnl $3=`echo $1 | tr ':' '\n' | grep $2`
$3=`echo $1 | tr ':' '\n' | grep $2`
])

AC_DEFUN(my_JARNAME_FINDER, [
dnl $2=`echo $1 | tr '/' '\n' | grep ".jar"`
$3=`echo $1 | tr ':' '\n' | sed -e 's/^.*\///'  | grep "^$2.*.jar"`
])

AC_DEFUN(my_JARDIR_FINDER, [
$3=`echo $1 | sed -e 's/\/'"$2"'//'`
])

dnl $1=MYCLASSPATH
dnl $2=KEYWORD
dnl $3=version
dnl $4=JARNAME to return
dnl $5=FULLPATH to return
dnl $6=DIRPATH to return
AC_DEFUN(my_JAR_VALIDATOR, [
mJARNAME=`echo $1 | tr ':' '\n' | sed -e 's/^.*\///'  | grep "^$2.*.jar"`
$4=$mJARNAME
if test "$mJARNAME" ; then
   mJARPATH=`echo $1 | tr ':' '\n' | grep $mJARNAME`
   $5=$mJARPATH
   if test -f "$mJARPATH" ; then
      mJARDIR=`echo $mJARPATH | sed -e 's/\/'"$mJARNAME"'//'`
      $6=$mJARDIR
      AC_MSG_RESULT([VALIDATED: $2 $mJARNAME for version $3 is found in $mJARDIR.]) 
   else 
      AC_MSG_ERROR([FAILED: $2 $mJARNAME for version $3 is not found in $mJARPATH.])
   fi
else
   AC_MSG_ERROR([FAILED: $2 $3 is not found in CLASSPATH.])
fi

])


