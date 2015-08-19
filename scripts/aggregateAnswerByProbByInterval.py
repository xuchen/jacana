#! /usr/bin/env python

import sys,re,os.path,operator
from optparse import OptionParser

intervals = [1, 3, 5, 10, 20, 100]
int2qid2ans2count = {}
int2qid2ans2count_force = {} 
for i in intervals:
    int2qid2ans2count[i] = {}
    int2qid2ans2count_force[i] = {} 

parser = OptionParser()
choices=['sum_prob', 'max_prob', 'no_prob']
parser.add_option("-m", "--method", type="choice", action="store", dest="method", choices=choices, default='no_prob', help="which score method to use (%s)" % "|".join(choices), metavar='no_prob')
parser.add_option("-n", "--no-force", action="store_false", dest="no_force", default=False, help="don't force an answer")
parser.add_option("-p", "--pattern-file", dest="pattern_filename", help="answer pattern file", metavar="FILE")
parser.add_option("-t", "--tag-file", dest="tag_filename", help="tagged output from mergeRefWithCRFsuite.py", metavar="FILE")

(options, args) = parser.parse_args()

SUM_PROB=0
MAX_PROB=1
NO_PROB=2

#method = SUM_PROB
method = MAX_PROB
if options.method == 'no_prob':
    method = NO_PROB
elif options.method == 'max_prob':
    method = MAX_PROB
elif options.method == 'sum_prob':
    method = SUM_PROB

tag_file = options.tag_filename
force = not options.no_force
pattern_file = options.pattern_filename
if pattern_file is None:
    # pattern file for test only
    pattern_file = 'tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'
    if not os.path.exists(pattern_file):
        pattern_file = '../tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'

def meanstdv(x): 
    from math import sqrt 
    n, mean, std = len(x), 0, 0 
    for a in x: 
        mean = mean + a 
    mean = mean / float(n) 
    for a in x: 
        std = std + (a - mean)**2 
    std = sqrt(std / float(n-1)) 
    return mean, std

def median(x):
    sorted_x = sorted(x)
    if len(sorted_x) % 2 == 1:
        return sorted_x[(len(sorted_x)+1)/2-1]
    else:
        lower = sorted_x[len(sorted_x)/2-1]
        upper = sorted_x[len(sorted_x)/2]
        return (lower+upper)/2.0

def median_mad(x):
    # compute median and median absolute deviation (MAD)
    m = median(x)
    mad = median([abs(i-m) for i in x])
    return (m, mad)

def force_an_answer(prob_ans_list):
    # given a list of (prob,word), find an answer from the list
    # the threshold is the prob of answer is beyond <code>thresh</code> std of median prob
    # we use median instead of mean here since median is more robust to outliers
    #mean, std = meanstdv([x[0] for x in prob_ans_list])
    mean, std = median_mad([x[0] for x in prob_ans_list])
    if std == 0.0: std = 1.0
    thresh_list = [50,40]
    #thresh = 30
    previous_is_answer = False
    ans_list = []
    for thresh in thresh_list:
        got_one = False
        for (prob, word) in prob_ans_list:
            if abs(prob-mean)/std > thresh:
                got_one = True
                if previous_is_answer:
                    ans_list[len(ans_list)-1] += " " + word
                else:
                    ans_list.append(word)
                previous_is_answer = True
            else:
                previous_is_answer = False
        if got_one:
            break
    return ans_list


pattern_f = open(pattern_file)
idx2pattern = {}
for line in pattern_f:
    idx, pattern = line.strip().split(' ', 1)
    if idx not in idx2pattern:
        idx2pattern[idx] = set()
    idx2pattern[idx].add(re.compile(re.escape(pattern.lower())))
pattern_f.close()

tag_f = open(tag_file)
eof = False
answer_list = []
prob_list = []
qid2ans2count = {}
qid2ans2count_force = {}
prob_ans_list = []
found_one = False
sent_prob = 0.0
rank = 0
old_qid = ''
for line in tag_f:
    line = line.strip()
    if line.startswith('Performance'):
        eof = True
    if eof:
        break
    if line.startswith("PAIR"):
        # PAIR1516  how_long    @probability    0.489432
        sent_prob = float(line.split()[3])
        found_one = False
        rank += 1
        continue
    elif len(line) == 0:
        if not found_one:
            if force:
                force_ans_list = force_an_answer(prob_ans_list)
            else:
                force_ans_list = []
            if len(force_ans_list) > 0:
                if qid not in qid2ans2count_force:
                    qid2ans2count_force[qid] = {}
                    for i in intervals:
                        int2qid2ans2count_force[i][qid] = {}
                for answer in force_ans_list:
                    if answer not in qid2ans2count_force[qid]:
                        qid2ans2count_force[qid][answer] = 0
                        for i in intervals:
                            if rank <= i:
                                int2qid2ans2count_force[i][qid][answer] = 0
                    # set to count to only 0.1 since it's of low credibility
                    qid2ans2count_force[qid][answer] += 0.1
                    for i in intervals:
                        if rank <= i:
                            int2qid2ans2count_force[i][qid][answer] += 0.1

        prob_ans_list = []
        continue
    elif line.startswith("Q:"):
        #Q:	32.1	what do practitioners of wicca worship ?
        qid = line.split('\t')[1]
        if old_qid != '' and qid != old_qid:
            rank = 0
        old_qid = qid
        if qid not in qid2ans2count:
            qid2ans2count[qid] = {}
            for i in intervals:
                int2qid2ans2count[i][qid] = {}
    elif line.startswith("O") or line.startswith("ANSWER"):
        #O	ANSWER-B:0.647345	may
        #O	ANSWER-I:0.746675	12
        #O	ANSWER-I:0.709436	,
        #ANSWER-B	ANSWER-I:0.719313	1820
        ref,model,word = line.split('\t')
        ans_prob = float(model.split(":")[1])
        if model.startswith("ANSWER"):
            answer_list.append(word)
            prob_list.append(ans_prob)
            found_one = True
        elif model.startswith("O") and len(answer_list) > 0:
            answer = " ".join(answer_list)
            averaged_ans_prob = sent_prob*sum(prob_list)/len(prob_list)
            answer_list = []
            if answer not in qid2ans2count[qid]:
                qid2ans2count[qid][answer] = 0
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] = 0
            if method == SUM_PROB:
                qid2ans2count[qid][answer] += averaged_ans_prob
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] += averaged_ans_prob
            elif method == MAX_PROB:
                qid2ans2count[qid][answer] = max(averaged_ans_prob, qid2ans2count[qid][answer])
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] = max(averaged_ans_prob, qid2ans2count[qid][answer])
            elif method == NO_PROB:
                qid2ans2count[qid][answer] += 1
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] += 1
        prob_ans_list.append((ans_prob, word))
    elif line.startswith("RIGHT") or line.startswith("WRONG"):
        if len(answer_list) > 0:
            answer = " ".join(answer_list)
            answer_list = []
            if answer not in qid2ans2count[qid]:
                qid2ans2count[qid][answer] = 0
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] = 0
            if method == SUM_PROB:
                qid2ans2count[qid][answer] += averaged_ans_prob
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] += averaged_ans_prob
            elif method == MAX_PROB:
                qid2ans2count[qid][answer] = max(averaged_ans_prob, qid2ans2count[qid][answer])
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] = max(averaged_ans_prob, qid2ans2count[qid][answer])
            elif method == NO_PROB:
                qid2ans2count[qid][answer] += 1
                for i in intervals:
                    if rank <= i:
                        int2qid2ans2count[i][qid][answer] += 1

tag_f.close()

# merge qid2ans2count to qid2ans2count_force
# if set to True, then only when CRF failed to find an answer in
# all sentences, force an answer
# otherwise, even if CRF succeeded to find an answer, still 
# force an answer in other sentences where CRF failed
only_force_if_crf_did_not_found = False
if not only_force_if_crf_did_not_found:
    for qid, ans2count in qid2ans2count.items():
        if qid not in qid2ans2count_force:
            qid2ans2count_force[qid] = {}
        for ans,count in ans2count.items():
            if ans not in qid2ans2count_force[qid]:
                qid2ans2count_force[qid][ans] = count
            else:
                qid2ans2count_force[qid][ans] += count
else:
    for qid, ans2count in qid2ans2count.items():
        if qid not in qid2ans2count_force:
            qid2ans2count_force[qid] = ans2count
        elif len(ans2count) > 0:
            qid2ans2count_force[qid] = ans2count

if not only_force_if_crf_did_not_found:
    for i,qid2ans2count_i in int2qid2ans2count.items():
        for qid, ans2count in qid2ans2count_i.items():
            if qid not in int2qid2ans2count_force[i]:
                int2qid2ans2count_force[i][qid] = {}
            for ans,count in ans2count.items():
                if ans not in int2qid2ans2count_force[i][qid]:
                    int2qid2ans2count_force[i][qid][ans] = count
                else:
                    int2qid2ans2count_force[i][qid][ans] += count
else:
    for i,qid2ans2count_i in int2qid2ans2count.items():
        for qid, ans2count in qid2ans2count_i.items():
            if qid not in int2qid2ans2count_force[i]:
                int2qid2ans2count_force[i][qid] = ans2count
            elif len(ans2count) > 0:
                int2qid2ans2count_force[i][qid] = ans2count


def partial_vote(qid2ans2count):
    # partial vote among all answers
    # the naive algorithm works like this:
    # say, two answers "april 1984" and "1984" with count 1
    # we add each answer #overlap/#total words = 1/3
    for qid, ans2count in qid2ans2count.items():
        if len(ans2count) <= 1: continue
        # convert to a list
        ans_count = ans2count.items()
        for i in range(len(ans2count)):
            for j in range(i+1, len(ans2count)):
                ans_i, ans_j = ans_count[i][0], ans_count[j][0]
                list_i = ans_i.split()
                set_i = set(list_i)
                list_j = ans_j.split()
                set_j = set(list_j)
                overlap = set_i.intersection(set_j)
                if len(overlap) != 0:
                    total = len(list_i) + len(list_j)
                    add = len(overlap)*1.0/total
                    ans2count[ans_i] += add*ans2count[ans_j]
                    ans2count[ans_j] += add*ans2count[ans_i]


#partial_vote(qid2ans2count)
#partial_vote(qid2ans2count_force)

def compute_f1(qid2ans2count):
    correct = 0
    retrieved = 0
    for qid in sorted(qid2ans2count.keys()):
        ans2count = qid2ans2count[qid]
        if len(ans2count) == 0: continue
        retrieved += 1
        sorted_ans = sorted(ans2count.iteritems(), key=operator.itemgetter(1), reverse=True)
        answer = sorted_ans[0][0]
        #print qid, sorted_ans
        is_correct = False
        if qid in idx2pattern:
            for pattern in idx2pattern[qid]:
                if pattern.search(answer):
                    correct += 1
                    is_correct = True
                    break
        #if is_correct:
        #    print "CORRECT"
        #else:
        #    print "WRONG"
        #print

    relevant = len(qid2ans2count)
    prec = correct*1.0/retrieved
    recall = correct*1.0/relevant
    print "Precision: %.3f (%d/%d)" % (prec, correct, retrieved)
    print "Recall: %.3f (%d/%d)" % (recall, correct, relevant)
    print "F1: %.3f" % (2*prec*recall/(prec+recall) if prec+recall!=0.0 else 0.0)


print "####### CRF ########"
compute_f1(qid2ans2count)
for i,qid2ans2count_i in sorted(int2qid2ans2count.items()):
    print "====== %d ======" % i
    compute_f1(qid2ans2count_i)

print
print
print "####### CRF forced ########"
compute_f1(qid2ans2count_force)
for i,qid2ans2count_force_i in sorted(int2qid2ans2count_force.items()):
    print "====== %d ======" % i
    compute_f1(qid2ans2count_force_i)
