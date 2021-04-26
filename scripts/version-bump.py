# written in python 3.7.0

import os
from pathlib import Path
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

    print("Bumping to %d.%d.%d" % (major, minor, patch))

    # TODO: effectively apply version number
    # Steps:
    #   - write to VERSION.txt
    #   - write to POM file, then run 'mvn install'
    #   - create git tag (annotated!) AFTER previous steps to immortalize the built JAR on its own tag
    #
    # P.S. modify existing scripts to dynamically use version in VERSION.txt
    
