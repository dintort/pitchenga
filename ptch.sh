#!/bin/bash
echo home=$HOME
#JDK=/Library/Java/JavaVirtualMachines/adoptopenjdk-8.jdk/Contents/Home
#JDK=/c/Program\ Files/RedHat/java-1.8.0-openjdk-1.8.0.212-3
#JDK=/c/Program\ Files/AdoptOpenJDK/jdk-11.0.10.9-hotspot
#JDK=/c/Program\ Files/AdoptOpenJDK/jdk-11.0.10.9-hotspot
JDK=/c/usr/jdk-11.0.8+10
#JDK=/usr/share/idea/jbr
#JDK=/usr/lib/jvm/java-1.11.0-openjdk-amd64
#PROJECT=$HOME/dev/pitchenga
PROJECT=$(pwd)
echo project=$PROJECT

"$JDK/bin/java" -classpath \
"$PROJECT/out/production/classes":\
"$PROJECT/out/production/resources":\
"$JDK/jre/lib/charsets.jar":\
"$JDK/jre/lib/ext/cldrdata.jar":\
"$JDK/jre/lib/ext/dnsns.jar":\
"$JDK/jre/lib/ext/jaccess.jar":\
"$JDK/jre/lib/ext/localedata.jar":\
"$JDK/jre/lib/ext/nashorn.jar":\
"$JDK/jre/lib/ext/sunec.jar":\
"$JDK/jre/lib/ext/sunjce_provider.jar":\
"$JDK/jre/lib/ext/sunpkcs11.jar":\
"$JDK/jre/lib/ext/zipfs.jar":\
"$JDK/jre/lib/jce.jar":\
"$JDK/jre/lib/jsse.jar":\
"$JDK/jre/lib/management-agent.jar":\
"$JDK/jre/lib/resources.jar":\
"$JDK/jre/lib/rt.jar":\
"$JDK/lib/dt.jar":\
"$JDK/lib/jconsole.jar":\
"$JDK/lib/sa-jdi.jar":\
"$JDK/lib/tools.jar":\
"$PROJECT/lib/TarsosDSP-2.4.jar":\
"$PROJECT/lib/JTransforms-2.4.jar":\
"$PROJECT/lib/commons-math3-3.2.jar":\
"$PROJECT/lib/commons-lang3-3.1.jar":\
"$PROJECT/lib/jlayer-1.0.1.4.jar":\
"$PROJECT/lib/macify-1.6.jar":\
"$PROJECT/lib/mp3spi-1.9.5.4.jar":\
"$PROJECT/lib/tritonus-share-0.3.7.4.jar":\
"$PROJECT/lib/jogamp\gluegen-rt.jar":\
"$PROJECT/lib/jogamp\jogl-all.jar":\
"$PROJECT/lib/jogamp\gluegen-rt-natives-windows-amd64.jar":\
"$PROJECT/lib/jogamp\jogl-all-natives-windows-amd64.jar":\
  -DNOcom.pitchenga.default.input=NO_AUDIO_INPUT\
  com.pitchenga.Ptchng \
  -NSRequiresAquaSystemAppearance False