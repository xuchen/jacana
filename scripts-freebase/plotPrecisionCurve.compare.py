#! /usr/bin/env python

import numpy as np
import matplotlib.pyplot as plt

def read_f1(fname):
    score_correct_list = []
    with open(fname) as f:
        for line in f:
            q, correct, score = line.split('\t')
            score_correct_list.append((float(score), float(correct)))
    score_correct_list.sort(reverse=True)
    return score_correct_list

j_score_correct_list = read_f1('../results/freebase/webquestions.test.forced_answers.scores')
# s_score_correct_list = read_f1('../results/freebase/sempre.35.7.f1')
# s_score_correct_list = read_f1('../results/freebase/sempre.best.test.tabbed.tabbed')
s_score_correct_list = read_f1('../results/freebase/sempre.marginalized.test.tabbed.tabbed')

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
        # correct_so_far = 0.0
        # all_so_far = 0.0
        for i in range(previous_chunk, chunk):
            candidiate_tuple = score_correct_ans_idx_list[i]
            correct_so_far += candidiate_tuple[1]
            all_so_far += 1
        previous_chunk = chunk
        precision = correct_so_far/all_so_far*100
        precisions.append(precision)
        # print "%d%%\t%.1f%%" % (chunk/step, precision)
    print all_so_far
    return precisions

j_precisions = get_precisions(j_score_correct_list)
s_precisions = get_precisions(s_score_correct_list)
total = len(s_score_correct_list)

x_ticks = range(0,101,10)
y_ticks = range(0,101,10)
fig = plt.figure()
ind = np.arange(0, 101, 100/divide)  # the x locations for the groups
ax = fig.add_subplot(111)
ax.set_ylabel('Accuracy (%)') # , rotation=0)
ax.set_xlabel('% Answered')
ax.set_title('Accuracy w.r.t. %% of questions answered (total: %d)'%total)
ax.set_xticks(x_ticks)
ax.set_yticks(y_ticks)
ax.set_xlim([0,100])
ax.set_ylim([20,70])
ax.grid(True)
#ax.set_xticklabels( structured_x , rotation=45)
j_plot = ax.plot(ind, j_precisions, '.-')[0]
s_plot = ax.plot(ind, s_precisions, '.-')[0]
ax.legend([j_plot, s_plot], ['jacana-freebase', 'SEMPRE'])

plt.show()
