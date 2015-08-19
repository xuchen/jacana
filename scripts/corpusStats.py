#! /usr/bin/env python

import sys, re, gzip

q2all = {}
q2positive = {}

# <QApairs id='1.4'>
id_p = re.compile(r"<QApairs id='(.*)'>")
fname = sys.argv[1]
if fname.endswith('.gz'):
    f = gzip.open(fname)
else:
    f = open(fname)
for line in f:
    if line.startswith('<QApairs'):
        m = id_p.match(line.strip())
        idx = m.group(1)
        q2all[idx] = 0
    elif line.startswith('<positive>'):
        q2all[idx] += 1
        if idx not in q2positive:
            q2positive[idx] = 0
        q2positive[idx] += 1
    elif line.startswith('<negative>'):
        q2all[idx] += 1
f.close()

print "all questions:", len(q2all)
print "all questions with an answer:", len(q2positive)
num_all = sum(q2all.values())
print "all instances:", num_all
num_positives = sum(q2positive.values())
print "all Positive instances: %.2f (%d/%d)" % (num_positives*1.0/num_all, num_positives, num_all)
