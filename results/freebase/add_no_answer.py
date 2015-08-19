#! /usr/bin/env python

q_gold = []
with open('webquestions.test.answers.all.list') as f:
    for line in f:
        q_gold.append(line.split("\t")[:2])

out_f = open('webquestions.test.gold_retrieval.answers.with_no_answer.list', 'w')
with open('webquestions.test.gold_retrieval.answers.list') as f:
    for line in f:
        line = line.strip()
        q = line.split("\t")[0]
        while q != q_gold[0][0]:
            print >> out_f, "%s\t%s\tNO_ANSWER" % (q_gold[0][0], q_gold[0][1])
            q_gold.pop(0)
        print >> out_f, line
        q_gold.pop(0)

while len(q_gold) != 0:
    print >> out_f, "%s\t%s\tNO_ANSWER" % (q_gold[0][0], q_gold[0][1])
    q_gold.pop(0)

out_f.close()
