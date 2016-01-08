#!/usr/bin/env node

module.exports = function(context) {
    var path = context.requireCordovaModule('path'),
        shell = context.requireCordovaModule('shelljs');

    var platformRoot = path.join(context.opts.projectRoot, 'platforms/ios');

    this.platformRoot = platformRoot;

    this.xcodeProjectFilePath = function() {
        var files = shell.exec('find ' + platformRoot + ' -name project.pbxproj > /dev/null').output.split('\n');
        var result = files.filter(function(element) {
            return element.search(/\/ios\/[^\/]*\.xcodeproj.*/) != -1;
        });
        if (result.length != 1) {
            throw new Error('Failed to get project file, check if there are multiple projects in the platforms/ios directory.');
        }
        return result.pop();
    }

    this.pluginSuffix = 'CordovaPluginCrosswalkWebViewInjectedForRemoval';

    var xcconfigFileName = 'xwalkBuild.xcconfig';
    var xcconfigSrcPath = path.join(context.opts.plugin.dir, 'platforms/ios', xcconfigFileName);
    var xcconfigDestPath = path.join(platformRoot, 'cordova', xcconfigFileName);
    var buildConfigPath = path.join(platformRoot, 'cordova', 'build.xcconfig');
    this.xcconfigFile = {
        name: xcconfigFileName,
        src: xcconfigSrcPath,
        dest: xcconfigDestPath,
        fileToInject: buildConfigPath
    };
}
