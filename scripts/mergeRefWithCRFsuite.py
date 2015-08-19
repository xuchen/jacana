#! /usr/bin/env python

# this script append the original reference (first argument) words to CRFsuite_tag output (stdin)
# so that it's easier to understand what's going on in the tagging.
# the words in the reference file has to be lined up with CRFsuite output

import sys,gzip
from sys import stdin

ref_file = sys.argv[1]
if ref_file.endswith('.gz'):
    ref_f = gzip.open(ref_file)
else:
    ref_f = open(ref_file)
words = [w.strip() for w in ref_f]
ref_f.close()

counter = 0
summary = False

qword2right = {}
qword2all = {}
correct = True
words_counter = 0
for line in sys.stdin:
    line = line.strip()
    if summary:
        print line
    else:
        if line.startswith('Performance'):
            summary = True
            print line
            continue
        elif line.startswith('@probability'):
            qword = words[words_counter]
            words_counter+=1
            qsentence = words[words_counter]
            words_counter+=1
            counter += 1
            print "PAIR%d\t%s\t%s" % (counter, qword, line)
            print "Q:\t"+qsentence
            if qword not in qword2right:
                qword2right[qword] = 0
                qword2all[qword] = 0
            qword2all[qword] += 1
        elif len(line) == 0:
            if correct:
                qword2right[qword] += 1
            words[words_counter]
            words_counter+=1
            if correct:
                print "RIGHT"
            else:
                print "WRONG"
            print
            correct = True
        else:
            print "%s\t%s" % (line, words[words_counter])
            words_counter+=1
            ref, answer = line.split()
            ref = ref.split(":")[0]
            answer = answer.split(":")[0]
            if ref != answer:
                correct = False

print "Precision by question types:"
for qword in sorted(qword2right.keys()):
    correct = qword2right[qword]
    total = qword2all[qword]
    print "%s\t%.2f(%d/%d)" % (qword, correct*1.0/total, correct, total)

