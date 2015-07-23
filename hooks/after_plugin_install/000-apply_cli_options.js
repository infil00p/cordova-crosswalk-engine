#!/usr/bin/env node

/**
 * @description
 * Hook that acts as a polyfill for plugins which should be configurable via Cordova CLI (in addition to
 * providing values within config.xml), but which should be able to use default values without user interaction.
 * This is desirable in situations XMl file manipulation is impractical, e.g CI systems or third party
 * CLIs, like the Ionic Framework. Can removed when the feature is available in cordova-lib stable.
 *
 * Implementation:
 * Parses <preference> tag within plugin.xml, following the implementation of Ticket CB-9162.
 * Then adds values to config.xml to be accessible by existing APIs.
 *
 * @see https://issues.apache.org/jira/browse/CB-9162
 *
 * @example
 *      PLUGIN.XML:
 *      <platform name="android">
 *          <config-file target="res/xml/config.xml" parent="/*">
 *           <preference name="CROSSWALK_ANDROID_VERSION" default="13"/>
 *      </platform>
 *
 *      COMMANDLINE:
 *      cordova plugin add "cordova-plugin-crosswalk-webview" --variable CROSSWALK_ANDROID_VERSION="15+"
 **/

module.exports = function(context) {

    /** @external */
    var fs = context.requireCordovaModule('fs'),
        path = context.requireCordovaModule('path'),
        deferral = context.requireCordovaModule('q').defer(),
        ConfigParser = context.requireCordovaModule("cordova-lib/src/configparser/ConfigParser"),
        PluginInfo = context.requireCordovaModule("cordova-lib/src/PluginInfo"),
        XmlHelpers = context.requireCordovaModule("cordova-lib/src/util/xml-helpers"),
        CordovaCli = path.dirname(path.dirname(process.mainModule.filename)),
        nopt = require(path.join(CordovaCli, 'node_modules', 'nopt')),
        _ = require(path.join(CordovaCli, 'node_modules', 'underscore'));

    /** @defaults */
    var argumentsString = context.cmdLine,
        pluginConfigurationFile = path.join(context.opts.plugin.dir, 'plugin.xml'),
        projectConfigurationFile = path.join(context.opts.projectRoot, 'config.xml');

    /** Init */
    var CordovaConfig = new ConfigParser(projectConfigurationFile);

    /**
     * Gets plugin.xml default Preferences - Polyfill until CB-9162 gets released
     * @returns {Object|undefined} -  Array-like object (preference:default)
     */
    var defaultPreferences = function() {
        var pluginPreferences = {};

        var pluginXmlRoot = XmlHelpers.parseElementtreeSync(pluginConfigurationFile),
            tagName = "preference",
            containerName = "config-file",
            targetPlatform = 'android',
            targetPlatformTag = pluginXmlRoot.find('./platform[@name="' + targetPlatform + '"]');


        var tagsInRoot = pluginXmlRoot.findall(tagName) || [],
            tagsInPlatform = targetPlatformTag ? targetPlatformTag.findall(tagName) : [],
            tagsInContainer = targetPlatformTag ? targetPlatformTag.findall(containerName) : [],
            tagsList = tagsInRoot.concat(tagsInContainer);

        // Parses <preference> tags within <config-file>-blocks
        tagsList.map(function(prefTag) {
            prefTag.getchildren().forEach(function(element) {
                if ((element.tag == 'preference') && (element.attrib['name']) && 
                        element.attrib['default']) {
                    pluginPreferences[element.attrib['name']] = element.attrib['default'];
                }
            });
        });


        return pluginPreferences;
    };

    /**
     * Plugin settings, parsed from Cordova CLI
     * @returns {Object|undefined} -  Array-like object (preference:value)
     */
    var cliPreferences = function() {

        var commandlineVariablesList = nopt({ 'variable': Array }, {}, argumentsString.split(' '))['variable'],
            commandlineVariables = {};

        if (commandlineVariablesList) {
            commandlineVariablesList.forEach(function(element) {
                commandlineVariables[element.split('=')[0].toUpperCase()] = element.split('=')[1];
            });
        }

        return commandlineVariables;
    };


    /** Main method */
    // Set default values for any undefined properties
    var main = function() {
        // Establish list of valid preference names. Remove superfluous ones.

        var validPrefsList = _.intersection(Object.keys(cliPreferences()), Object.keys(defaultPreferences())),
            defaultPrefsFiltered = _.pick(defaultPreferences(), validPrefsList),
            cliPrefsFiltered = _.pick(cliPreferences(), validPrefsList);


        // Establish final preferences object by providing defaults from plugin.xml
        var resolvedPrefs = _.defaults(cliPrefsFiltered, defaultPreferences()),
            resolvedPrefsList = Object.keys(resolvedPrefs);

        // Loop through all resolved preferences
        resolvedPrefsList.forEach(function(prefName) {
            // Delete same-name preferences
            var projectXmlRoot = XmlHelpers.parseElementtreeSync(projectConfigurationFile),
                child = projectXmlRoot.find('./preference[@name="' + prefName + '"]');
            XmlHelpers.pruneXML(projectConfigurationFile, child, '/*');
            // Add the new preferences
            CordovaConfig.setGlobalPreference(prefName, resolvedPrefs[prefName]);
        });

        // Write config.xml
        CordovaConfig.write();

        deferral.resolve();
    };

    main();

    return deferral.promise;

};
