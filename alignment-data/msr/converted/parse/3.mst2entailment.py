#! /usr/bin/env python

import sys

f = open(sys.argv[1])

lines = [line.strip() for line in f]

# every sentence takes 6 lines, premise/hypothesis
# together take 12 lines

unit = 12

if len(lines) % unit != 0:
    print >> sys.stderr, "Error: line number should be a multiple of 12:", len(lines)
    sys.exit(-1)

total = len(lines)/unit
# p for premise, h for hypothesis
for i in range(total):
    fields = []
    p_len = str(len(lines[i*unit].split()))
    h_len = str(len(lines[i*unit+6].split()))
    fields.append(p_len)
    fields += lines[i*unit:i*unit+5]
    fields.append("false")
    fields.append(h_len)
    fields += lines[i*unit+6:i*unit+11]
    fields.append("false")
    fields.append("1")
    print "\t".join(fields)

f.close()
