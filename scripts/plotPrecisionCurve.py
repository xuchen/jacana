#! /usr/bin/env python

import re, sys, gzip, os
import numpy as np
import matplotlib.pyplot as plt
import pylab

pattern_file = '../jeopardy-data/jeopardy.id2answer.tabbed'
# top_dir = '/Users/xuchen/data2/xuchen/j-archive/test' + '/'
# answer_files = [top_dir + 'answerfinder.test.align.save.sum_prob.gz', top_dir + 'answerfinder.test.align.save.no_prob.gz']
# legends = ['sum_prob', 'max_prob']

aligners = ['jacana', 'meteor', 'ted', 'giza', 'not']

# first one for the gold ceiling precision
legends = ['gold', 'jacana-align', 'Meteor', 'TED', 'GIZA++', 'baseline']
# marker [ + | , | . | 1 | 2 | 3 | 4 ]
# linestyle [ - | -. | : | steps | ...]
styles = ['-', '.-', '--', '-.', '+', ',-'] 
# http://matplotlib.org/api/colors_api.html
colors = ['green', 'blue', 'cyan', 'red', 'magenta', 'black']

oracle_voting = False

jacana_idx = -1
for (i,aligner) in enumerate(aligners):
    if aligner == 'jacana':
        jacana_idx = i
        break
assert jacana_idx != -1

file_path = '/Users/xuchen/Data2/xuchen/j-archive/%s-aligned/answerfinder.test.align.save.l1.sum_prob'

# all questions with an answer in the retrieved snippets
q_with_answer = set()
with open('/Users/xuchen/Data2/xuchen/j-archive/q_with_answer') as f:
    for line in f:
        q_with_answer.add(line.strip())

answer_files = []
for aligner in aligners:
    answer_files.append(file_path % aligner)

assert len(answer_files)+1 == len(legends), "length doesn't match"

def load_answer_patterns(pattern_file):
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
    return idx2pattern

def load_system_answers(answer_file):
    idx2ans_list = {}
    if answer_file.endswith('.gz'):
        f = gzip.open(answer_file)
    else:
        f = open(answer_file)
    for line in f:
        # sorted answer list for each
        # 2059.clue_DJ_3_1        [('harley superglide', 0.39543459837749995), ('bags', 0.30501130035600005)]
        idx, ans_list = line.strip().split("\t")
        ans_list = eval(ans_list)
        idx2ans_list[idx] = ans_list
    f.close()
    return idx2ans_list

def judge_correct(ans_str_list, patterns):
    for ans_str in ans_str_list:
        for pattern in patterns:
            if pattern.search(ans_str):
                return True
    return False


def sort_ans_list_by_confidence(idx2ans_list, idx2pattern):
    # return a reversely sorted (bigger to smaller) list of tuples: (confidence_in_double, correct_or_not, answer_str, id)
    score_ans_idx_list = []
    for idx, ans_list in idx2ans_list.items():
        # TODO: deal with cases with multiple matches
        ans_str = ans_list[0][0]
        ans_score = ans_list[0][1]
        if oracle_voting:
            correct = judge_correct([ans[0] for ans in ans_list], idx2pattern[idx])
        else:
            correct = judge_correct([ans_str], idx2pattern[idx])
        score_ans_idx_list.append((ans_score, correct, ans_str, idx))
    return sorted(score_ans_idx_list, reverse=True)

def average_precision1(score_correct_ans_idx_list):
    total = 0.0
    for (_,prec,_,_) in score_correct_ans_idx_list:
        total += prec
    return total/len(score_correct_ans_idx_list)

def average_precision(precisions):
    return sum(precisions)/len(precisions)/100

cumulative = True
fit_curve = False
if not cumulative:
    # whether to fit the curve with linear regression when the dots are too zig-zag
    fit_curve = True
divide = 100
def get_precisions(score_correct_ans_idx_list):
    global divide
    total = len(score_correct_ans_idx_list)
    step = total/divide

    correct_so_far = 0.0
    all_so_far = 0.0
    previous_chunk = 0
    precisions = []
    for chunk in range(step, total, step):
        if chunk > total-step: chunk = total
        if not cumulative:
            correct_so_far = 0.0
            all_so_far = 0.0
        for i in range(previous_chunk, chunk):
            candidiate_tuple = score_correct_ans_idx_list[i]
            if candidiate_tuple[1]: correct_so_far += 1
            all_so_far += 1
        previous_chunk = chunk
        precision = correct_so_far/all_so_far*100
        precisions.append(precision)
        # print "%d%%\t%.1f%%" % (chunk/step, precision)
    print all_so_far, len(precisions)
    return precisions

def write_precision_file(answer_file, score_correct_ans_idx_list):
    with open(os.path.dirname(answer_file)+"/precision", 'w') as f:
        for _,correct,_,idx in score_correct_ans_idx_list:
            print >> f, idx, correct

idx2pattern = load_answer_patterns(pattern_file)

precision_list = []
ave_precision_list = []
total = 0
score_correct_ans_idx_lists = []
for answer_file in answer_files:
    idx2ans_list = load_system_answers(answer_file)
    score_correct_ans_idx_list = sort_ans_list_by_confidence(idx2ans_list, idx2pattern)
    # ave_precision_list.append(average_precision(score_correct_ans_idx_list))
    score_correct_ans_idx_lists.append(score_correct_ans_idx_list)
    prec = get_precisions(score_correct_ans_idx_list)
    write_precision_file(answer_file, score_correct_ans_idx_list)
    ave_precision_list.append(average_precision(prec))
    precision_list.append(prec)
    total = max(total, len(score_correct_ans_idx_list))

ceiling_score = []
jacana_score = score_correct_ans_idx_lists[jacana_idx]
# get the ceiling precision w.r.t. jacana, the best performing aligner
# (ans_score, correct, ans_str, idx)
for (_,_,_,idx) in jacana_score:
    ceiling_score.append((0, 1 if idx in q_with_answer else 0, '', idx))
ceiling_precision = get_precisions(ceiling_score)
# ave_ceiling_precision = average_precision(ceiling_score)
ave_ceiling_precision = average_precision(ceiling_precision)
precision_list = [ceiling_precision] +  precision_list 
ave_precision_list = [ave_ceiling_precision] +  ave_precision_list 

x_ticks = range(0,101,10)
y_ticks = range(0,101,10)
fig = plt.figure(figsize=(9.66, 6.46))
ind = np.arange(100.0/divide, 100+100.0/divide, 100.0/divide)  # the x locations for the groups
ax = fig.add_subplot(111)
ax.set_ylabel('Precision (%)') # , rotation=0)
ax.set_xlabel('% Answered')
#ax.set_title('Precision w.r.t. % of questions answered (total: %d)'%total)
if not cumulative:
    ax.set_title('Precision w.r.t. every percent of questions answered (sorted by confidence)')
else:
    ax.set_title('Precision w.r.t. % of questions answered')
ax.set_xticks(x_ticks)
ax.set_yticks(y_ticks)
ax.set_xlim([0,100])
ax.set_ylim([0,100])
ax.grid(True)
#ax.set_xticklabels( structured_x , rotation=45)

pics = []
for (i,precisions) in enumerate(precision_list):
    y = precisions[0:len(ind)]
    if fit_curve:
        fit = pylab.polyfit(ind, y, 2)
        fit_y = pylab.poly1d(fit)
        y = fit_y(ind)
    pics.append(ax.plot(ind, y, styles[i], color=colors[i])[0])
for i, prec in enumerate(ave_precision_list):
    legends[i] += ", %.4f" % prec
leg = ax.legend(pics, legends, loc='lower left', fancybox=True)
# transparent legend
leg.get_frame().set_alpha(0.3)


if not cumulative:
    if fit_curve:
        fig.savefig('/tmp/jacana-jeopardy-at-percent.curve_fitted.pdf', transparent=True)
    else:
        fig.savefig('/tmp/jacana-jeopardy-at-percent.pdf', transparent=True)
else:
    fig.savefig('/tmp/jacana-jeopardy.pdf', transparent=True)

plt.show()



# divide = 100
# step = -(max_score-min_score)/divide
# 
# correct_so_far = 0.0
# all_so_far = 0.0
# total = len(score_correct_ans_idx_list)
# current = 0
# for threshold in np.arange(max_score, min_score, step):
#     while current < total and score_correct_ans_idx_list[current][0] >= threshold:
#         candidiate_tuple = score_correct_ans_idx_list[current]
#         if candidiate_tuple [1]: correct_so_far += 1
#         all_so_far += 1
#         current += 1
#     print "%.3f\t%.1f%%" % (threshold, correct_so_far/all_so_far*100)
