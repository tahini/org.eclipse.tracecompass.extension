#!/usr/bin/env python3
###############################################################################
# Copyright (c) 2017 École Polytechnique de Montréal
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
###############################################################################

import argparse
import shutil
import os

parser = argparse.ArgumentParser(description='Creates the plugins and feature for a new extension functionnality.')
parser.add_argument('name', help='The human readable name of the plugins and feature to add. The plugin names will the the dot-separated lowercase name. For example if name is "My Test Plugin", plugins will be named org.eclipse.tracecompass.extension.my.test.plugin')
parser.add_argument('--dir', help='Directory in which to add the plugins')
parser.add_argument('--no-ui', dest='noUi', action='store_const', const=True, default=False, help='Whether to add a UI plugin for this feature')

args = parser.parse_args()
idPlaceholder = "{%skeleton}"
namePlaceholder = "{%skeletonName}"

baseDir = os.path.dirname(os.path.realpath(__file__))

def copyAndUpdate(srcDir, destDir, name, id):
    shutil.copytree(srcDir, destDir)
    for dname, dirs, files in os.walk(destDir):
        for fname in files:
            fpath = os.path.join(dname, fname)
            with open(fpath, encoding = "ISO-8859-1") as f:
                s = f.read()
            s = s.replace(idPlaceholder, id)
            s = s.replace(namePlaceholder, name)
            with open(fpath, "w") as f:
                f.write(s)

def copyDirs(fullname, dir, noUi):
    if dir is None:
        dir = '.'

    id = fullname.lower().replace(' ', '.')
    moveTo = dir + '/org.eclipse.tracecompass.extension.' + id
    print('Copying skeleton directories to ' + moveTo + '[.*]')
    copyAndUpdate(baseDir + '/skeleton.feature', moveTo, fullname, id)
    copyAndUpdate(baseDir + '/skeleton.core', moveTo + '.core', fullname, id)
    # Move the Activator of the core
    os.makedirs(moveTo + '.core/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/core')
    shutil.move(moveTo + '.core/src/Activator.java', moveTo + '.core/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/core')
    shutil.move(moveTo + '.core/src/package-info.java', moveTo + '.core/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/core')

    copyAndUpdate(baseDir + '/skeleton.core.tests', moveTo + '.core.tests', fullname, id)

    if not(noUi):
        copyAndUpdate(baseDir + '/skeleton.ui', moveTo + '.ui', fullname, id)
        # Move the Activator of the ui
        os.makedirs(moveTo + '.ui/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/ui')
        shutil.move(moveTo + '.ui/src/Activator.java', moveTo + '.ui/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/ui')
        shutil.move(moveTo + '.ui/src/package-info.java', moveTo + '.ui/src/org/eclipse/tracecompass/extension/internal/' + id.replace('.', '/') + '/ui')

    print('------------------------------')
    print('Congratulations! Your new plugins are ready to be populated and add magnificent features to Trace Compass!')
    print("")
    print("For the Hudson jobs to take them in, don't forget the add them to the appropriate pom.xml files and if necessary, create a pom.xml file in the parent directory")

copyDirs(args.name, args.dir, args.noUi)


