#! /usr/bin/env python

import numpy as np
import matplotlib.pyplot as plt

def computeMicroF1(gold, predicted):
    num_correct = 1.0 * len(gold.intersection(predicted))
    p = r = 0.0
    if len(predicted) != 0:
        p = num_correct / len(predicted)
    if len(gold) != 0:
        r = num_correct / len(gold)
    f1 = (2*p*r/(p+r) if p+r != 0.0 else 0)
    return f1

def computePrecision(gold, predicted):
    num_correct = 1.0 * len(gold.intersection(predicted))
    correct = num_correct/len(predicted)
    return correct

score_correct_list = []
out_f = open('../results/freebase/webquestions.test.answers.scores', 'w')
with open('../results/freebase/webquestions.test.answers') as f:
    for line in f:
        if line.startswith("#"):
            continue
        q, gold, predicted2score = line.split('\t')
        gold = eval(gold)
        predicted2score = eval(predicted2score)
        score = 0.0
        correct = 0.0
        if len(predicted2score) != 0:
            score = min(predicted2score.values())
            # score = sum(predicted2score.values())/l
            # correct = computePrecision(gold, predicted2score.keys())
            correct = computeMicroF1(gold, predicted2score.keys())
            score_correct_list.append((score, correct))
        else:
            score_correct_list.append((0.0, 0.0))
        print >> out_f, "%s\t%.3f\t%.3f" % (q, correct, score)

out_f.close()
score_correct_list.sort(reverse=True)

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
        for i in range(previous_chunk, chunk):
            candidiate_tuple = score_correct_ans_idx_list[i]
            correct_so_far += candidiate_tuple[1]
            all_so_far += 1
        previous_chunk = chunk
        precision = correct_so_far/all_so_far*100
        precisions.append(precision)
        print "%d%%\t%.1f%%" % (chunk/step, precision)
    print all_so_far
    return precisions

precisions = get_precisions(score_correct_list)
total = len(score_correct_list)

x_ticks = range(0,101,10)
y_ticks = range(0,101,10)
# fig = plt.figure()
fig = plt.figure(figsize=(9.66, 6.46))
ind = np.arange(0, 101, 100/divide)  # the x locations for the groups
ax = fig.add_subplot(111)
ax.set_ylabel('Precision (%)') # , rotation=0)
ax.set_xlabel('% Answered')
# ax.set_title('Precision w.r.t. % of questions answered (total: %d)'%total)
ax.set_title('Precision w.r.t. % of questions answered')
ax.set_xticks(x_ticks)
ax.set_yticks(y_ticks)
ax.set_xlim([0,100])
ax.set_ylim([0,100])
ax.grid(True)
#ax.set_xticklabels( structured_x , rotation=45)
pic, = ax.plot(ind, precisions, color="red")

leg = ax.legend([pic], ['jacana-freebase'], loc='lower right', fancybox=True)
# transparent legend
leg.get_frame().set_alpha(0.3)

fig.savefig('/tmp/jacana-freebase.pdf', transparent=True)
# pics = []
# for (i,precisions) in enumerate(precision_list):
#     pics.append(ax.plot(ind, precisions, styles[i])[0])
# ax.legend(pics, legends)

plt.show()


