#! /usr/bin/env python

import sys,re,os.path,operator

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


tag_file = sys.argv[1]
force = True
if len(sys.argv) > 2 and sys.argv[2] == '--no-force':
    force = False
    if len(sys.argv) > 3:
        pattern_file = sys.argv[3]
if len(sys.argv) > 2 and sys.argv[2] != '--no-force':
    pattern_file = sys.argv[2]
else:
    # pattern file for test only
    pattern_file = 'tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'
    if not os.path.exists(pattern_file):
        pattern_file = '../tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'

pattern_f = open(pattern_file)
idx2pattern = {}
for line in pattern_f:
    idx, pattern = line.strip().split(' ', 1)
    if idx not in idx2pattern:
        idx2pattern[idx] = set()
    # idx2pattern[idx].add(re.compile(re.escape(pattern.lower())))
    idx2pattern[idx].add(re.compile(pattern.lower()))
pattern_f.close()

tag_f = open(tag_file)
eof = False
answer_list = []
qid2ans2count = {}
qid2ans2count_force = {}
prob_ans_list = []
found_one = False
for line in tag_f:
    line = line.strip()
    if line.startswith('Performance'):
        eof = True
    if eof:
        break
    if line.startswith("PAIR"):
        found_one = False
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
                for answer in force_ans_list:
                    if answer not in qid2ans2count_force[qid]:
                        qid2ans2count_force[qid][answer] = 0
                    # set to count to only 0.1 since it's of low credibility
                    qid2ans2count_force[qid][answer] += 0.1

        prob_ans_list = []
        continue
    elif line.startswith("Q:"):
        #Q:	32.1	what do practitioners of wicca worship ?
        qid = line.split('\t')[1]
        if qid not in qid2ans2count:
            qid2ans2count[qid] = {}
    elif line.startswith("O") or line.startswith("ANSWER"):
        #O	ANSWER-B:0.647345	may
        #O	ANSWER-I:0.746675	12
        #O	ANSWER-I:0.709436	,
        #ANSWER-B	ANSWER-I:0.719313	1820
        ref,model,word = line.split('\t')
        if model.startswith("ANSWER"):
            answer_list.append(word)
            found_one = True
        elif model.startswith("O") and len(answer_list) > 0:
            answer = " ".join(answer_list)
            answer_list = []
            if answer not in qid2ans2count[qid]:
                qid2ans2count[qid][answer] = 0
            qid2ans2count[qid][answer] += 1
        prob_ans_list.append((float(model.split(":")[1]), word))
    elif line.startswith("RIGHT") or line.startswith("WRONG"):
        if len(answer_list) > 0:
            answer = " ".join(answer_list)
            answer_list = []
            if answer not in qid2ans2count[qid]:
                qid2ans2count[qid][answer] = 0
            qid2ans2count[qid][answer] += 1

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
                    ans2count[ans_i] += add
                    ans2count[ans_j] += add


partial_vote(qid2ans2count)
partial_vote(qid2ans2count_force)

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
print "####### CRF forced ########"
compute_f1(qid2ans2count_force)
