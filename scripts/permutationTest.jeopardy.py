#! /usr/bin/env python

import sys, random
# http://axon.cs.byu.edu/Dan/478/assignments/permutation_test.php

# ./permutationTest.jeopardy.py /Users/xuchen/data2/xuchen/j-archive/jacana-aligned/precision /Users/xuchen/data2/xuchen/j-archive/not-aligned/precision
# 
# with k = 1000:
# jacana vs. baseline: p = 0.0
# jacana vs. meteor: p = 0.039
# jacana vs. TED: p = 0.006
# meteor vs. TED: p = 0.23
# giza vs. baseline: p = 0.0
# ted vs. giza: p = 0.0


def read_prec_file(fname):
    idx2correct = {}
    with open(fname) as f:
        for line in f:
            idx,correct = line.strip().split()
            idx2correct[idx] = 1 if correct=='True' else 0
    return idx2correct

idx2c1 = read_prec_file(sys.argv[1])
idx2c2 = read_prec_file(sys.argv[2])

common_keys = set(idx2c1.keys()).intersection(set(idx2c2.keys()))

diff = [idx2c1[idx] - idx2c2[idx] for idx in common_keys]
mu_diff = abs(sum(diff))
print mu_diff

k = 1000
n = 0

for i in range(k):
    new_diff = [d if random.random()>0.5 else -d for d in diff]
    mu_new_diff = abs(sum(new_diff))
    # print mu_new_diff
    if mu_new_diff >= mu_diff:
        n += 1

print "p = %f (n = %d)" % (n*1.0/k, n)
