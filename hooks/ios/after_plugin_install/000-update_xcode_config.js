#!/usr/bin/env node

module.exports = function(context) {
    var deferral = context.requireCordovaModule('q').defer(),
        fs = context.requireCordovaModule('fs'),
        shell = context.requireCordovaModule('shelljs');

    var Common = require('./../common.js'),
        common = new Common(context);

    shell.cp(common.xcconfigFile.src, common.xcconfigFile.dest);

    var textToInsert = '#include "' + common.xcconfigFile.name + '"';
    if (shell.grep(textToInsert, common.xcconfigFile.fileToInject)) {
        console.log('common.xcconfigFile.fileToInject: ' + common.xcconfigFile.fileToInject);
    } else {
        fs.appendFile(common.xcconfigFile.fileToInject, textToInsert, function(error){
            if (error) {
                console.log('Failed to append deployment target version onto ', common.xcconfigFile.fileToInject);
                throw error;
                deferral.reject(error);
            }
        });
    }

    var FrameworkEntry = function(name, id, index, segs) {
        var segments = segs;
        segments[0] = id;
        segments.splice(4, 0, 'Embed');
        segments.splice(segments.length - 1, 0, 'settings = {ATTRIBUTES = (CodeSignOnCopy, RemoveHeadersOnCopy, ); };');
        return {
            name : name,
            id : id,
            originalIndex : index,
            entryStringToInject : function() {
                return '\t\t' + segments.join(' ');
            }
        }
    }

    var projectFileArray = fs.readFileSync(common.xcodeProjectFilePath(), 'utf8').split('\n');

    var advanceEntryID = function(id) {
        var original = id.trim();
        if (original.length != 24) {
            var error = new Error('Wrong ID: ' + id.trim() + '. Length sould be 24 byte.');
            throw error;
            deferral.reject(error);
        }

        var head = original.substring(0, 8);
        var tail = original.substring(8, 24);

        var headNumber = new Number('0x' + head);
        headNumber++;
        return headNumber.toString(16).toUpperCase() + tail;
    }

    var frameworkEntryByName = function(frameworkName) {
        var index = projectFileArray.findIndex(function(element, index, array) {
            return element.match(frameworkName + '.framework in Frameworks.*isa');
        });
        if (index == -1) {
            var error = new Error('Failed to find ' + frameworkName + ' within project file.');
            throw error;
            deferral.reject(error);
        }

        var segs = projectFileArray[index].split(' ');
        return new FrameworkEntry(frameworkName, advanceEntryID(segs[0]), index, segs);
    }

    var fileEntries = [];
    fileEntries.push(frameworkEntryByName('GCDWebServer'));
    fileEntries.push(frameworkEntryByName('XWalkView'));
    fileEntries.sort(function(a, b) {
        return a.originalIndex - b.originalIndex;
    });

    var embedFrameworkSectionID = 'AB7767801BFEE3EF00927EAE';

    var embedFrameworkBuildSectionToInject = function(frameworks) {
        var entries = [];
        frameworks.forEach(function(element, index, array){
            entries.push('\t\t\t\t' + element.id + ' /* ' + element.name + '.framework in Embed Frameworks */,');
        });
        var segs = [
            '/* Begin PBXCopyFilesBuildPhase section */',
            '\t\t' + embedFrameworkSectionID + ' /* Embed Frameworks */ = {',
            '\t\t\tisa = PBXCopyFilesBuildPhase;',
            '\t\t\tbuildActionMask = 2147483647;',
            '\t\t\tdstPath = "";',
            '\t\t\tdstSubfolderSpec = 10;',
            '\t\t\tfiles = (',
            '\t\t\t);',
            '\t\t\tname = "Embed Frameworks";',
            '\t\t\trunOnlyForDeploymentPostprocessing = 0;',
            '\t\t};',
            '/* End PBXCopyFilesBuildPhase section */'
        ];

        entries.forEach(function(element, index, array) {
            segs.splice(7, 0, element);
        });
        return segs;
    }

    var arrayToWrite = [];
    var lastIndex = 0;
    var pluginSuffix = '\t\t/* '+ common.pluginSuffix + ' */';

    Array.prototype.appendPluginSuffix = function() {
        this.forEach(function(element, index, array) {
            array[index] = element + pluginSuffix;
        });
        return this;
    };

    for (var i = 0; i < fileEntries.length; i++) {
        var element = fileEntries[i];
        arrayToWrite = arrayToWrite.concat(projectFileArray.slice(lastIndex, element.originalIndex));
        arrayToWrite.push(projectFileArray[element.originalIndex]);
        arrayToWrite.push(element.entryStringToInject() + pluginSuffix);
        lastIndex = element.originalIndex + 1;
    }

    var proxyIndex = projectFileArray.findIndex(function(element, index, array) {
        return element.match('End PBXContainerItemProxy section');
    });
    if (proxyIndex == -1) {
        var error = new Error('Failed to find PBXContainerItemProxy section to insert Embed Framework section.');
        throw error;
        deferral.reject(error);
    }

    arrayToWrite = arrayToWrite.concat(projectFileArray.slice(lastIndex, proxyIndex + 1), '', embedFrameworkBuildSectionToInject(fileEntries).appendPluginSuffix());
    lastIndex = proxyIndex + 1;

    var buildPhasesIndex = projectFileArray.findIndex(function(element, index, array) {
        return element.match('buildPhases = .');
    });
    if (buildPhasesIndex == -1) {
        var error = new Error('Failed to find buildPhases section to insert Embed Framework.');
        throw error;
        deferral.reject(error);
    }

    arrayToWrite = arrayToWrite.concat(projectFileArray.slice(lastIndex, buildPhasesIndex + 1), '\t\t\t\t' + embedFrameworkSectionID + ' /* Embed Frameworks */,' + pluginSuffix);
    lastIndex = buildPhasesIndex + 1;
    arrayToWrite = arrayToWrite.concat(projectFileArray.slice(lastIndex));

    fs.writeFile(common.xcodeProjectFilePath(), arrayToWrite.join('\n'), function(error) {
        if (error) {
            throw error;
            deferral.reject(error);
        } else {
            deferral.resolve();
        }
    });

    return deferral.promise;
};
