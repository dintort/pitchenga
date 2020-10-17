#!/bin/bash
echo home=$HOME
JDK=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
#JDK=/c/Program\ Files/RedHat/java-1.8.0-openjdk-1.8.0.212-3
PROJECT=$HOME/dev/pitchenga
echo project=$PROJECT
"$JDK/bin/java" -classpath "$JDK/jre/lib/charsets.jar":"$JDK/jre/lib/ext/cldrdata.jar":"$JDK/jre/lib/ext/dnsns.jar":"$JDK/jre/lib/ext/jaccess.jar":"$JDK/jre/lib/ext/localedata.jar":"$JDK/jre/lib/ext/nashorn.jar":"$JDK/jre/lib/ext/sunec.jar":"$JDK/jre/lib/ext/sunjce_provider.jar":"$JDK/jre/lib/ext/sunpkcs11.jar":"$JDK/jre/lib/ext/zipfs.jar":"$JDK/jre/lib/jce.jar":"$JDK/jre/lib/jsse.jar":"$JDK/jre/lib/management-agent.jar":"$JDK/jre/lib/resources.jar":"$JDK/jre/lib/rt.jar":"$JDK/lib/dt.jar":"$JDK/lib/jconsole.jar":"$JDK/lib/sa-jdi.jar":"$JDK/lib/tools.jar":"$PROJECT/out/production/classes":"$PROJECT/out/production/resources":"$PROJECT/lib/TarsosDSP-2.4.jar" com.pitchenga.Multi -NSRequiresAquaSystemAppearance False