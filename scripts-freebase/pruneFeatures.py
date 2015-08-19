#! /usr/bin/env python

import sys, operator, gzip

# NOTE: only pruning training is enough

# features with count > threshold will be kept
threshold = 10
f2c = {}

thresholds = [1, 2, 3, 5, 10]

in_fname = sys.argv[1]
if in_fname.endswith(".gz"):
    out_fname = in_fname[0:-3] + ".%d.pruned.gz" % threshold
else:
    out_fname = in_fname + ".%d.pruned" % threshold


def open_file(fname, mode='r'):
    if fname.endswith(".gz"):
        return gzip.open(fname, mode)
    else:
        return open(fname, mode)

in_f = open_file(in_fname)
for line in in_f:
    if line.startswith("#"): continue
    for feat in line.split():
        if feat not in f2c:
            f2c[feat]  = 0
        f2c[feat] += 1
in_f.close()

print "total features:", len(f2c)

total = len(f2c)
pruned = 0
prune2count = {}
for p in thresholds:
    prune2count[p] = 0

for c in f2c.values():
    if c <= threshold:
        pruned += 1
    for p in thresholds:
        if c <= p:
            prune2count[p] += 1
print "to be pruned: %d (%.3f)" % (pruned, pruned*1.0/total)

sorted_x = sorted(f2c.iteritems(), key=operator.itemgetter(1), reverse=True)

total_count = sum([x[1] for x in sorted_x[0:10]])
print "top 10 features:"
for (f,c) in sorted_x[0:10]:
    print "%s %d (%.3f)" % (f, c, c*1.0/total_count)

for (p,c) in sorted(prune2count.items()):
    print "counts <= %d: %d (%.3f)" % (p, c, c*1.0/total)

sys.exit(0)
out_f = open_file(out_fname, 'w')

in_f = open_file(in_fname)

for line in in_f:
    if not line.startswith("#"):
        l = []
        for f in line.split():
            if f2c[f] > threshold:
                l.append(f)
        l.append("\n")
        line = " ".join(l)
    out_f.write(line)
in_f.close()
out_f.close()

print "pruned file written to", out_fname
