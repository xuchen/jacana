#! /usr/bin/env python

import re,sys

# ./plaintext.py > RTE2_dev_M.sents
# ./plaintext.py RTE2_test_M.align.txt > RTE2_test_M.sents
if len(sys.argv) > 1:
    f = open(sys.argv[1])
else:
    f = open('RTE2_dev_M.align.txt')

for line in f:
    if line.startswith("#"):
        continue
    elif line.startswith("NULL"):
        print " ".join(re.sub("\(.*?\)", "", line).split()[1:])
    else:
        print line.strip()

f.close()
