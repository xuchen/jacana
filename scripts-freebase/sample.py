#! /usr/bin/env python

import random

# keep 20% of original
thresh = 0.2

out_f = open('train-all.sampled.%.1f.bnf' % thresh, 'w')

with open('train-all.bnf') as f:
    for line in f:
        if line.startswith('#'):
            out_f.write(line)
        elif line.startswith('+1'):
            out_f.write(line)
        elif line.startswith('-1'):
            if random.random() < thresh:
                out_f.write(line)
        else:
            print "unknown line:", line

out_f.close()
