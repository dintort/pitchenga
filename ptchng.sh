#!/bin/bash

#jar2app Pitchenga.jar --name=Pitchenga --display-name=Pitchenga -b com.pitchenga.Pitchenga -v 0.0.1 -s 0.0.1 -m com.pitchenga.Ptchng -o --icon=../resources/main/pitchenga.png


echo home=$HOME
JRE=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
#JRE=/Library/Internet\ Plug-Ins/JavaAppletPlugin.plugin/Contents/Home
PROJECT=$HOME/Documents/pitchenga
echo project=$PROJECT
#"$JRE/bin/java" -classpath "$JRE/lib/charsets.jar":"$JRE/lib/ext/cldrdata.jar":"$JRE/lib/ext/dnsns.jar":"$JRE/lib/ext/jaccess.jar":"$JRE/lib/ext/localedata.jar":"$JRE/lib/ext/nashorn.jar":"$JRE/lib/ext/sunec.jar":"$JRE/lib/ext/sunjce_provider.jar":"$JRE/lib/ext/sunpkcs11.jar":"$JRE/lib/ext/zipfs.jar":"$JRE/lib/jce.jar":"$JRE/lib/jsse.jar":"$JRE/lib/management-agent.jar":"$JRE/lib/resources.jar":"$JRE/lib/rt.jar":"$JDK/lib/dt.jar":"$JDK/lib/jconsole.jar":"$JDK/lib/sa-jdi.jar":"$JDK/lib/tools.jar":"$PROJECT/out/production/classes":"$PROJECT/out/production/resources":"$PROJECT/lib/TarsosDSP-2.4.jar" com.pitchenga.Ptchng -NSRequiresAquaSystemAppearance False

"$JRE/bin/java" -classpath \
"$PROJECT/out/production/classes":\
"$PROJECT/out/production/resources":\
"$PROJECT/lib/TarsosDSP-2.4.jar":\
"$PROJECT/lib/JTransforms-2.4.jar":\
"$PROJECT/lib/commons-math3-3.2.jar":\
"$PROJECT/lib/commons-lang3-3.1.jar":\
"$PROJECT/lib/jlayer-1.0.1.4.jar":\
"$PROJECT/lib/macify-1.6.jar":\
"$PROJECT/lib/mp3spi-1.9.5.4.jar":\
"$PROJECT/lib/tritonus-share-0.3.7.4.jar":\
"$PROJECT/lib/jogamp/gluegen-rt-2.5.0.jar":\
"$PROJECT/lib/jogamp/gluegen-rt-2.5.0-natives-macosx-universal.jar":\
"$PROJECT/lib/jogamp/jogl-all-2.5.0.jar":\
"$PROJECT/lib/jogamp/jogl-all-2.5.0-natives-macosx-universal.jar":\
  -Xmx1g \
  -Xms128m \
  -Dcom.pitchenga.debug=true \
  --add-exports=java.desktop/com.apple.eawt=ALL-UNNAMED \
  --add-exports=java.desktop/sun.awt=ALL-UNNAMED \
  com.pitchenga.Ptchng \
  -NSRequiresAquaSystemAppearance False

