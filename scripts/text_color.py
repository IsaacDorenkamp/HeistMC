import platform
import sys

write = None

# first index is ANSI, second is Windows console
# only including the colors I initially needed for now :)
colors = {
    'green': [32, 2],
    'yellow': [33, 6]
}

if platform.system() == 'Windows':
    import ctypes
    STDOUT = -11
    STDOUT_HANDLE = ctypes.windll.kernel32.GetStdHandle(STDOUT)
    def _write(text, color):
        c = colors[color][1]
        ctypes.windll.kernel32.SetConsoleTextAttribute(STDOUT_HANDLE, c)
        sys.stdout.write(text)
        ctypes.windll.kernel32.SetConsoleTextAttribute(STDOUT_HANDLE, 0b1111)
        
    write = _write
else:
    def _write(text, color):
        c = colors[color][0]
        sys.stdout.write('\033[%dm%s\033[0m' % (c, text))
    write = _write
