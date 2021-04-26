class InvalidArgvError(RuntimeError):
    def __init__(self, message):
        RuntimeError.__init__(self, message)

def _switch(name, value=None):
    return { 'type': 'switch', 'name': name, 'value': value }

def _flag(name):
    return { 'type': 'flag', 'name': name }

def _arg(value):
    return { 'type': 'argument', 'value': value }

def parse(argv):
    data = []
    for i in argv[1:]:
        if i.startswith('--'):
            main = i[2:]
            parts = main.split("=", 1)
            if len(parts) == 1:
                if parts[0] == '':
                    raise InvalidArgvError("Empty switch found, this is not allowed!")
                else:
                    data.append(_switch(parts[0]))
            else:
                if parts[0] == '':
                    raise InvalidArgvError("Switch with empty name found, this is not allowed!")
                else:
                    data.append(_switch(parts[0], parts[1]))
        elif i.startswith('-'):
            flagName = i[1:]
            if flagName == '':
                raise InvalidArgvError("Empty flag found, this is not allowed!")
            data.append(_flag(flagName))
        else:
            data.append(_arg(i))

    return data
