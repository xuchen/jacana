#! /usr/bin/env python

import sys, random
# http://axon.cs.byu.edu/Dan/478/assignments/permutation_test.php

# ./permutationTest.py 
# 
# with k = 1000:


def read_f1(fname):
    q_f1 = []
    with open(fname) as f:
        for line in f:
            q, correct, _ = line.split('\t')
            correct = float(correct)
            q_f1.append(correct)
    return q_f1

j_f1 = read_f1('../results/freebase/webquestions.test.forced_answers.scores')
s_f1 = read_f1('../results/freebase/sempre.35.7.f1')



diff = [j-s for j,s in zip(j_f1, s_f1)]
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
