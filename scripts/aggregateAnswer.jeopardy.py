#! /usr/bin/env python

# a memory-efficient version since the jeopardy dataset is too large to be held in memory
# this assumes that each question and its answers are clustered together

# ./aggregateAnswer.jeopardy.py -p ../jeopardy-data/jeopardy.id2answer.tabbed -t /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.tag.gz

# ./aggregateAnswer.jeopardy.py -p ../jeopardy-data/jeopardy.id2answer.tabbed -t /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.tag.gz -s /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.save.sum_prob.gz -n -m sum_prob | tee /Users/xuchen/data2/xuchen/j-archive/test/sum_prob.f1
# ./aggregateAnswer.jeopardy.py -p ../jeopardy-data/jeopardy.id2answer.tabbed -t /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.tag.gz -s /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.save.no_prob.gz -n | tee /Users/xuchen/data2/xuchen/j-archive/test/no_prob.f1
# ./aggregateAnswer.jeopardy.py -p ../jeopardy-data/jeopardy.id2answer.tabbed -t /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.tag.gz -s /Users/xuchen/data2/xuchen/j-archive/test/answerfinder.test.align.save.max_prob.gz -n -m max_prob | tee /Users/xuchen/data2/xuchen/j-archive/test/max_prob.f1

import sys,re,os.path,operator,gzip,gc,math
from optparse import OptionParser
from math import sqrt 


def gziplines(fname):
  from subprocess import Popen, PIPE
  f = Popen(['zcat', fname], stdout=PIPE)
  for line in f.stdout:
      yield line

parser = OptionParser()
choices=['sum_prob', 'max_prob', 'no_prob']
parser.add_option("-m", "--method", type="choice", action="store", dest="method", choices=choices, default='no_prob', help="which score method to use (%s)" % "|".join(choices), metavar='no_prob')
parser.add_option("-n", "--no-force", action="store_true", dest="no_force", default=False, help="don't force an answer")
parser.add_option("-p", "--pattern-file", dest="pattern_filename", help="answer pattern file", metavar="FILE")
parser.add_option("-t", "--tag-file", dest="tag_filename", help="tagged output from mergeRefWithCRFsuite.py", metavar="FILE")
parser.add_option("-s", "--save-file", dest="save_filename", help="save all candidate answers for each question to this file", metavar="FILE")

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
print options.no_force
force = not options.no_force
force = False
pattern_file = options.pattern_filename
save_file = options.save_filename
save_f = None
if save_file is not None:
    if save_file.endswith('.gz'):
        save_f = gzip.open(save_file, 'wb')
    else:
        save_f = open(save_file, 'w')

if pattern_file is None:
    # pattern file for test only
    pattern_file = 'tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'
    if not os.path.exists(pattern_file):
        pattern_file = '../tree-edit-data/answerSelectionExperiments/answer/trec13factpats.txt'

def meanstdv(x): 
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

def partial_vote(ans2count):
    # partial vote among all answers
    # the naive algorithm works like this:
    # say, two answers "april 1984" and "1984" with count 1
    # we add each answer #overlap/#total words = 1/3
    if len(ans2count) <= 1: return
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


correct = 0
retrieved = 0
relevant = 0
correct_force = 0
retrieved_force = 0
relevant_force = 0
def count_for_f1(ans2count, qid, force_it):
    global correct, correct_force
    global retrieved, retrieved_force
    global relevant, relevant_force
    global idx2pattern

    if force_it:
        relevant_force += 1
    else:
        relevant += 1
    # print len(ans2count)
    if len(ans2count) == 0: return
    if force_it:
        retrieved_force += 1
    else:
        retrieved += 1
    sorted_ans = sorted(ans2count.iteritems(), key=operator.itemgetter(1), reverse=True)
    if save_f and not force_it:
        save_f.write("%s\t%s\n" % (qid, str(sorted_ans)))
    answer = sorted_ans[0][0]
    #print qid, sorted_ans
    is_correct = False
    if qid in idx2pattern:
        for pattern in idx2pattern[qid]:
            if pattern.search(answer):
                if force_it:
                    correct_force += 1
                else:
                    correct += 1
                is_correct = True
                break
    #if is_correct:
    #    print "CORRECT"
    #else:
    #    print "WRONG"
    #print

def compute_f1(force_it):
    global correct, correct_force
    global retrieved, retrieved_force
    global relevant, relevant_force
    if force_it:
        prec = correct_force*1.0/retrieved_force
        recall = correct_force*1.0/relevant_force
        print "Precision: %.3f (%d/%d)" % (prec, correct_force, retrieved_force)
        print "Recall: %.3f (%d/%d)" % (recall, correct_force, relevant_force)
        print "F1: %.3f" % (2*prec*recall/(prec+recall) if prec+recall!=0.0 else 0.0)
    else:
        prec = correct*1.0/retrieved
        recall = correct*1.0/relevant
        print "Precision: %.3f (%d/%d)" % (prec, correct, retrieved)
        print "Recall: %.3f (%d/%d)" % (recall, correct, relevant)
        print "F1: %.3f" % (2*prec*recall/(prec+recall) if prec+recall!=0.0 else 0.0)

def q_routine():
    # merge ans2count to ans2count_force
    # if set to True, then only when CRF failed to find an answer in
    # all sentences, force an answer
    # otherwise, even if CRF succeeded to find an answer, still 
    # force an answer in other sentences where CRF failed
    global old_qid, qid, ans2count, ans2count_force, force
    if old_qid == '': old_qid = qid
    if force:
        only_force_if_crf_did_not_found = True
        if not only_force_if_crf_did_not_found:
            for ans,count in ans2count.items():
                if ans not in ans2count_force:
                    ans2count_force[ans] = count
                else:
                    ans2count_force[ans] += count
        else:
            if len(ans2count_force) == 0:
                ans2count_force = ans2count.copy()
    # partial_vote(ans2count)
    # partial_vote(ans2count_force)
    count_for_f1(ans2count, old_qid, False)
    if force:
        count_for_f1(ans2count_force, old_qid, True)



pattern_f = open(pattern_file)
idx2pattern = {}
for line in pattern_f:
    if line.find('\t') != -1:
        # the jeopardy answer file is tabbed: 2599.clue_DJ_2_1        vibes   vibraphone)     vibes (vibraphone)      vibraphone
        splits = line.strip().split('\t')
        idx = splits[0]
        if idx not in idx2pattern:
            idx2pattern[idx] = set()
        for ans in splits[1:]:
            idx2pattern[idx].add(re.compile(r'\b%s\b' % re.escape(ans.lower())))
    else:
        # the trec QA answer file is spaced: 1.4 black
        idx, pattern = line.strip().split(' ', 1)
        if idx not in idx2pattern:
            idx2pattern[idx] = set()
        idx2pattern[idx].add(re.compile(re.escape(pattern.lower())))
pattern_f.close()

if tag_file.endswith('.gz'):
    tag_f = gziplines(tag_file)
else:
    tag_f = open(tag_file)

eof = False
answer_list = []
prob_list = []
prob_ans_list = []
found_one = False
sent_prob = 0.0
ans2count = {}
ans2count_force = {}
old_qid = ''
qid = ''
c = 0
for line in tag_f:
    c += 1
    if c % 10000 == 0: 
        print c, len(ans2count), len(ans2count_force)
        # gc.collect()
    line = line.strip()
    if line.startswith('Performance'):
        eof = True
    if eof:
        break
    if line.startswith("PAIR"):
        # PAIR1516  how_long    @probability    0.489432
        sent_prob = float(line.split()[-1])
        if math.isnan(sent_prob):
            sent_prob = 0.5
        found_one = False
        continue
    elif len(line) == 0:
        if not found_one:
            if force:
                force_ans_list = force_an_answer(prob_ans_list)
            else:
                force_ans_list = []
            if len(force_ans_list) > 0:
                for answer in force_ans_list:
                    if answer not in ans2count_force:
                        ans2count_force[answer] = 0
                    # set to count to only 0.1 since it's of low credibility
                    ans2count_force[answer] += 0.1

        prob_ans_list = []
        continue
    elif line.startswith("Q:"):
        #Q:	32.1	what do practitioners of wicca worship ?
        qid = line.split('\t')[1]
        if qid != old_qid:
            q_routine()
            old_qid = qid
            ans2count.clear()
            ans2count_force.clear()
    elif line.startswith("O") or line.startswith("ANSWER"):
        #O	ANSWER-B:0.647345	may
        #O	ANSWER-I:0.746675	12
        #O	ANSWER-I:0.709436	,
        #ANSWER-B	ANSWER-I:0.719313	1820
        ref,model,word = line.split('\t')
        ans_prob = float(model.split(":")[1])
        if math.isnan(ans_prob):
            ans_prob = 0.5
        if model.startswith("ANSWER"):
            answer_list.append(word)
            prob_list.append(ans_prob)
            found_one = True
        elif model.startswith("O") and len(answer_list) > 0:
            answer = " ".join(answer_list)
            averaged_ans_prob = sent_prob*sum(prob_list)/len(prob_list)
            answer_list = []
            if answer not in ans2count:
                ans2count[answer] = 0
            if method == SUM_PROB:
                ans2count[answer] += averaged_ans_prob
            elif method == MAX_PROB:
                ans2count[answer] = max(averaged_ans_prob, ans2count[answer])
            elif method == NO_PROB:
                ans2count[answer] += 1
        prob_ans_list.append((ans_prob, word))
    elif line.startswith("RIGHT") or line.startswith("WRONG"):
        if len(answer_list) > 0:
            answer = " ".join(answer_list)
            answer_list = []
            if answer not in ans2count:
                ans2count[answer] = 0
            if method == SUM_PROB:
                ans2count[answer] += averaged_ans_prob
            elif method == MAX_PROB:
                ans2count[answer] = max(averaged_ans_prob, ans2count[answer])
            elif method == NO_PROB:
                ans2count[answer] += 1
if isinstance(tag_f, file):
    tag_f.close()
q_routine()

if save_f:
    save_f.close()

print "####### CRF ########"
compute_f1(False)
if force:
    print "####### CRF forced ########"
    compute_f1(True)
