#! /usr/bin/env python

import sys, re
#
# ./AggregatedAnswerAligner4TREC.py trec13factpats.txt AggregatedAnswerAligner.output gold test
#
# Given TREC pattern file, convert the output of AggregatedAnswerAligner.java
# to a format that trec_eval takes (test), and also output accompanying gold file (gold)
# 
# to evaluate:
# ../trec_eval-8.0/trec_eval -a gold test 

pattern_file = sys.argv[1]
aligner_file = sys.argv[2]

gold_file = sys.argv[3]
test_file = sys.argv[4]

pattern_f = open(pattern_file)
docid2pattern = {}
for line in pattern_f:
    docid, pattern = line.strip().split(' ', 1)
    if docid not in docid2pattern:
        docid2pattern[docid] = set()
    docid2pattern[docid].add(re.compile(pattern.lower()))
pattern_f.close()

aligner_f = open(aligner_file)
id2ans_vote = {}
for line in aligner_f:
    docid, ans, vote = line.strip().split('\t')
    if docid not in id2ans_vote:
        id2ans_vote[docid] = []
    id2ans_vote[docid].append((ans, vote))
aligner_f.close()

gold_f = open(gold_file, 'w')
test_f = open(test_file, 'w')

for docid, ans_vote in id2ans_vote.items():
    match = False
    for (ans, vote) in ans_vote:
        ans_no_space = ans.replace(' ', '_')
        for pattern in docid2pattern[docid]:
            if pattern.match(ans):
                match = True
                gold_f.write("%s 0 %s 1\n" % (docid, ans_no_space))
        test_f.write("%s Q0 %s 0 %s null\n" % (docid, ans_no_space, vote))
    if not match:
        # still have to output one genuine answer
        # so trec_eval knows there IS answer to this question
        gold_f.write("%s 0 WHATEVER 1\n" % (docid))

gold_f.close()
test_f.close()
