#! /usr/bin/env python

import json, gzip
# this script computs the precision matrix of jacana-qa with jacana-align and Meteor
# this script runs after plotPrecisionCurve.py outputs the "precision" files

# aligners = ['jacana', 'meteor', 'ted', 'giza', 'not']
(aligner1, aligner2) = ('jacana' , 'meteor')
# (aligner1, aligner2) = ('meteor', 'ted')
# (aligner1, aligner2) = ('jacana', 'not')
# (aligner1, aligner2) = ('jacana', 'ted')
# (aligner1, aligner2) = ('giza', 'not')

def read_f1(fname):
    # it's named "f1" but really True and False
    q2f1 = {}
    with open(fname) as f:
        for line in f:
            q, correct = line.split()
            correct = eval(correct)
            q2f1[q] =  correct
    return q2f1

j_q2f1 = read_f1('/Users/xuchen/Data2/xuchen/j-archive/%s-aligned/precision'%aligner1)
s_q2f1 = read_f1('/Users/xuchen/Data2/xuchen/j-archive/%s-aligned/precision'%aligner2)

with gzip.open('/Users/xuchen/Data2/xuchen/j-archive/jeopardy.id2meta.json.gz', 'rb') as f:
    q2meta = json.load(f)

mutual_q = set(j_q2f1.keys()).intersection(set(s_q2f1.keys()))
total = len(mutual_q)*1.0
j_right_s_right = 0
j_right_s_wrong = 0
j_right_s_wrong_set = set()
j_wrong_s_right = 0
j_wrong_s_right_set = set()
j_wrong_s_wrong = 0
for q in mutual_q:
    j_f1 = j_q2f1[q]
    s_f1 = s_q2f1[q]
    if j_f1  and s_f1:
        j_right_s_right += 1
    elif j_f1  and not s_f1:
        j_right_s_wrong += 1
        j_right_s_wrong_set.add(q)
    elif not j_f1 and not s_f1:
        j_wrong_s_wrong += 1
    elif not j_f1 and s_f1:
        j_wrong_s_right += 1
        j_wrong_s_right_set.add(q)

assert j_right_s_right + j_right_s_wrong + j_wrong_s_right + j_wrong_s_wrong == total 

print "%s correct %s wrong:" % (aligner1, aligner2)
print "\n".join([json.dumps(q2meta[q], indent=4, separators=(',', ': ')) for q in j_right_s_wrong_set])
print "\n"*2

print "%s correct %s wrong:" % (aligner2, aligner1)
print "\n".join([json.dumps(q2meta[q], indent=4, separators=(',', ': ')) for q in j_wrong_s_right_set])
print "\n"*2

print "\t\t   %s" % aligner1
print "\t\tright\t\twrong"
print "%s right\t%d (%.3f)\t%d (%.3f)" % (aligner2, j_right_s_right, j_right_s_right/total, j_wrong_s_right, j_wrong_s_right/total)
print "%s wrong\t%d (%.3f)\t%d (%.3f)" % (aligner2, j_right_s_wrong, j_right_s_wrong/total, j_wrong_s_wrong, j_wrong_s_wrong/total)
