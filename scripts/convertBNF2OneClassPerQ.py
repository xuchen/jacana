#! /usr/bin/env python

import sys
from sys import stdin

for line in stdin:
    line = line.strip()
    if len(line) == 0:
        print
    else:
        splits = line.split("\t")
        qword = ''
        for split in splits[1:]:
            if split.startswith('qword'):
                qword = split.split("|")[0]
                break
        if qword == '':
            print >> sys.stderr, "didn't find qword from:", line
        else:
            # append #qword=... to every field
            print '\t'.join([qword+"##"+w for w in splits])
