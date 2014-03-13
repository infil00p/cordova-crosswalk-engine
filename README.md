

Apache Cordova Crosswalk Engine Proof-of-Concept
===

Cordova Crosswalk Engine is a part of a Proof-of-Concept for 
third party engines to work with Apache Cordova.  This currently only works with a
personal fork of Apache Cordova found on GitHub, and as of yet, does not work with any
official Apache Cordova release.  This code should be considered very experimental.


Directions (We're still trying to automate this, sorry):
1. Pull down the pluggable_webview branch of Apache Cordova found here (https://github.com/infil00p/cordova-android/tree/pluggable_webview)
2. Generate a project with ./bin/create
3. Create directories for the engine 
    mkdir -p /src/org/apache/cordova/engine/crosswalk && mkdir -p /src/org/xwalk/runtime/extension
4. Copy the files that begin with XWalk into the src directory of that project in /src/org/apache/cordova/engine/crosswalk
5. Copy the remaining file (CordovaXWalkCoreExtensionBridge.java) to /src/org/xwalk/runtime/extension
6. Add the xwalk_core_library to the project.properties or local.properties
7. Copy the xwalk.pak to /assets
8. Copy the jsapi to /assets
9. Add this line to config.xml
    <preference name="webView" value="org.apache.cordova.engine.crosswalk.XWalkCordovaWebView" />


