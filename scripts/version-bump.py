# written in python 3.7.0

import os
from pathlib import Path
import re
import subprocess
import sys

import argv_parse

if __name__=='__main__':

    args = None
    try:
        args = argv_parse.parse(sys.argv)
    except argv_parse.InvalidArgvError as iae:
        print(iae)
        sys.exit(1)
        
    bump_type = None

    try:
        for arg in args:
            argtype = arg['type']
            if argtype == 'switch':
                name = arg['name']
                if name == 'type':
                    bump_type = arg['value']
                else:
                    raise argv_parse.InvalidArgvError("Unexpected switch '%s'" % name)
            else:
                raise argv_parse.InvalidArgvError("Unexpected %s '%s'" % (argtype, arg['name']))
    except argv_parse.InvalidArgvError as iae:
        print(iae)
        sys.exit(1)
                
    
    if bump_type is None or bump_type == '':
        print("Please specify a type using the --type switch. The types are: major, minor, patch")
        sys.exit(1)
    elif not bump_type in ['major', 'minor', 'patch']:
        print("Invalid bump type. The types are: major, minor, patch")
        sys.exit(1)
    
    current = Path(os.path.dirname(os.path.realpath(__file__)))
    version_filepath = os.path.join(current.parent, "VERSION.txt")
    version_file = open(version_filepath, 'r')

    content = version_file.read()
    version_file.close()

    parts = content.split('.')

    major = int(parts[0])
    minor = int(parts[1])
    patch = int(parts[2])

    if bump_type == "major": major += 1
    elif bump_type == "minor": minor += 1
    elif bump_type == "patch": patch += 1

    new_version = "%d.%d.%d" % (major, minor, patch)

    print("Updating VERSION.txt...")

    version_file = open(version_filepath, 'w')
    version_file.write(new_version)
    version_file.close()

    # write to POM file
    print("Updating pom.xml...")
    pom_filename = os.path.join(current.parent, "pom.xml")
    pom_file = open(pom_filename, 'r')
    pom = pom_file.read()
    pom_file.close()

    new_pom = re.sub(r'\<version\>[0-9]+\.[0-9]+(\.[0-9]+)?\<\/version\>', '<version>%s</version>' % new_version, pom, count=1)

    pom_file = open(pom_filename, 'w')
    pom_file.write(new_pom)
    pom_file.close()

    # update plugin.yml
    print("Updating plugin.yml...")
    plugin_filename = os.path.join(current.parent, "src", "main", "resources", "plugin.yml")
    plugin_file = open(plugin_filename, 'r')
    plugin_yml = plugin_file.read()
    plugin_file.close()

    new_plugin_yml = re.sub(r'\nversion: [0-9]+.[0-9]+(.[0-9]+)?\n', '\nversion: %s\n' % new_version, plugin_yml)

    plugin_file = open(plugin_filename, 'w')
    plugin_file.write(new_plugin_yml)
    plugin_file.close()

    # run maven install
    print("Running 'mvn clean install'...")

    wd = os.getcwd()
    os.chdir(str(current.parent))
    installer = subprocess.Popen("mvn clean install", shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    lineStart = True
    for ch in iter(lambda: installer.stdout.read(1), b''):
        if ch == b'': continue

        asStr = str(ch, 'utf-8')
        if asStr == '\n':
            sys.stdout.write('\n')
            lineStart = True
        else:
            if lineStart:
                sys.stdout.write(u'\u001b[32mmvn | \u001b[0m')
                lineStart = False

            sys.stdout.write(asStr)
            
    os.chdir(wd)

    print()

    # git release
    print("Performing git release...")

    proc_path = os.path.join(current, "git-release.bat")
    releaser = subprocess.Popen([proc_path], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    for ch in iter(lambda: releaser.stdout.read(1), b''):
        if ch == b'': continue

        asStr = str(ch, 'utf-8')
        if asStr == '\n':
            sys.stdout.write('\n')
            lineStart = True
        else:
            if lineStart:
                sys.stdout.write(u'\u001b[33mgit | \u001b[0m')
                lineStart = False

            sys.stdout.write(asStr)

    print()

    print("Successfully bumped to version %s!" % new_version)
