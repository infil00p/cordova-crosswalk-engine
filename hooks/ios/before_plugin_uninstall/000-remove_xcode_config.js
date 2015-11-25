#!/usr/bin/env node

module.exports = function(context) {
    var deferral = context.requireCordovaModule('q').defer(),
        shell = context.requireCordovaModule('shelljs');

    var Common = require('./../common.js'),
        common = new Common(context);

    shell.rm(common.xcconfigFile.dest);

    var textToRemove = '#include "' + common.xcconfigFile.name + '"';
    if (shell.grep(textToRemove, common.xcconfigFile.fileToInject)) {
        shell.sed('-i', textToRemove, '', common.xcconfigFile.fileToInject);
        console.log('Removed inclusion of \'' + common.xcconfigFile.name + '\' from build config');
    } else {
        console.log('There is no \'' + textToRemove + '\' defined in ' + common.xcconfigFile.fileToInject + ', ignore');
    }

    var regexpToRemove = new RegExp('.*' + common.pluginSuffix + '.*\\n', 'g');
    shell.sed('-i', regexpToRemove, '', common.xcodeProjectFilePath());
    if (shell.grep(common.pluginSuffix, common.xcodeProjectFilePath())) {
        var error = new Error('Failed to remove plugin tags from: ' + common.xcodeProjectFilePath());
        throw error;
        deferral.reject(error);
    } else {
        console.log('Removed injected project configurations');
        deferral.resolve();
    }

    return deferral.promise;
};
