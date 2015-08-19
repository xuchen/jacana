#! /usr/bin/env python

# ./computeF1.py test.retrieved.sampled.0.2.bnf.tagged.lbfgs.logistic.c1.1 

# our ACL'14 paper (on macro F1, note that we only output answers to 1605 questions):
# Precision: 0.388 (623.48/1605)
# Recall: 0.458 (930.43/2032)
# F1: 0.420

# ./computeF1.py test.retrieved.sampled.0.2.bnf.tagged.lbfgs.logistic.c1.1  true
# to align with Berant et al. (2013), whose system answered almost every question,
# we force an answer in those without an answer and average them 
# P: 0.334, R: 0.480, Accuracy: 0.354
# total: 2032

import sys,gzip
import operator

precision = 0.0
recall = 0.0
f1 = 0.0
correct = 0.0
retrieved = 0
relevant = 0
total = 0

accuracy = False

# difference among Micro,  Macro and Accuracy:
# macro: compute precision/recall per instance, thus #relevant = #gold answers per instance
#        F1 =2PR(P+R), in this case, if the system didn't output an answer per question, then the precision
#        of macro F1 should be larger than average precision (in accuracy)
# micro: compute precision/recall overall, thus #relevant = #all questions
# accuracy: compute precision/recall just as macro, but average over *all* questions, regardless of
#        whether the system outputs an answer or not


# Berant et al. (2013) used Macro. From edu.stanford.nlp.sempre.ListValue.getCompatibility()
macro = True

# force an answer if no one found (select the one with the highest prob)
force = False

if len(sys.argv) > 2:
    accuracy = True if sys.argv[2].lower() == 'true' else False

if accuracy:
    # when we are forced to compare with Berant et al. (2013), we force to output an answer
    # for every question to increase accuracy a bit
    force = True

# during test, only consider candiates from the top n retrieved topic nodes
retrieval_rank_thresh = 1
include_answer = True

use_first_node_with_answer = True

if len(sys.argv) > 3:
    retrieval_rank_thresh = int(sys.argv[3])

def addF1Micro(gold, predicted):
    global correct
    global retrieved
    global relevant
    if len(gold) == 0 and len(predicted) == 0:
        correct += 1
        relevant += 1
        retrieved += 1
    else:
        if len(gold) > 0:
            relevant += 1
        if len(predicted) > 0:
            retrieved +=1 
            correct += 1.0*len(gold.intersection(predicted)) / len(predicted)

def addAccuracy(gold, predicted):
    # Berant et al. (2013) used Macro. From edu.stanford.nlp.sempre.ListValue.getCompatibility():
    #   // Compute F1 score between two lists (partial match).
    #   // this is target (gold), that is predicted.
    #   ...
    #   precision /= that.values.size();
    #   ...
    #   recall /= this.values.size();
    #   double f1 = 2 * precision * recall / (precision + recall);
    global f1
    global precision
    global recall
    num_correct = 1.0 * len(gold.intersection(predicted))
    p = r = 0.0
    if len(predicted) != 0:
        p = num_correct / len(predicted)
        precision += p
    if len(gold) != 0:
        r = num_correct / len(gold)
        recall += r
    f1 += (2*p*r/(p+r) if p+r != 0.0 else 0)

def addF1Macro(gold, predicted):
    global retrieved
    global relevant
    global precision
    global recall
    num_correct = 1.0 * len(gold.intersection(predicted))
    if len(predicted) != 0:
        precision += num_correct / len(predicted)
        retrieved += 1
    if len(gold) != 0:
        recall += num_correct / len(gold)
        relevant += 1


predicted = set()
predicted2score = {}
force_predicted2score = {}
tagfile = sys.argv[1]
nodes_with_answer_so_far = 0
node_has_answer = False
include_answer = True
if tagfile.endswith('.gz'):
    f = gzip.open(tagfile)
else:
    f = open(tagfile)
if True:
    for line in f:
        line = line.strip()
        if line.startswith("##\t"):
            # start of a question
            _, q, gold_answer_str = line.split("\t")
            gold_answers = set(gold_answer_str.split(" || "))
            predicted.clear()
            predicted2score.clear()
            force_predicted2score.clear()
            nodes_with_answer_so_far = 0
            include_answer = True
            node_has_answer = False
        elif line.startswith("#\t"):
            splits = line.split("\t")
            if len(splits) > 1: text = splits[1]
            else: text = ""
        elif line.startswith("#_#\t"):
            # #_#     5       jamaican_english        16.039
            _, rank, _, _ = line.split("\t")
            if int(rank) <= retrieval_rank_thresh:
                include_answer = True
            else:
                include_answer = False
            if node_has_answer:
                nodes_with_answer_so_far += 1
            node_has_answer = False
            if use_first_node_with_answer and nodes_with_answer_so_far >= 1:
                include_answer = False
        elif line.startswith("+1 +1:") or line.startswith("-1 +1:"):
            score = float(line.split(":")[1])
            if include_answer: 
                predicted.add(text)
                if text not in predicted2score:
                    predicted2score[text] = 0.0
                predicted2score[text] += score
            node_has_answer = True
        elif line.startswith("-1 -1:") or line.startswith("+1 -1:"):
            # -1 -1:0.000116521
            if text not in force_predicted2score:
                if include_answer: force_predicted2score[text] = 0.0
            if include_answer: force_predicted2score[text] += float(line.split(":")[1])
        elif line.startswith("###"):
            total += 1
            if force and len(predicted) == 0 and len(force_predicted2score) != 0:
                max_score = max(force_predicted2score.values())
                sorted_x = sorted(force_predicted2score.iteritems(), key=operator.itemgetter(1), reverse=True)
                predicted.add(sorted_x[0][0])
                predicted2score[sorted_x[0][0]] = sorted_x[0][1]
            # end of a question
            print "%s\t%s\t%s" % (q, str(gold_answers), str(predicted2score))
            if accuracy:
                addAccuracy(gold_answers, predicted)
            else:
                if macro:
                    addF1Macro(gold_answers, predicted)
                else:
                    addF1Micro(gold_answers, predicted)
f.close()

if accuracy:
    precision /= total
    recall /= total
    f1 /= total
    print "# Average P: %.3f, Average R: %.3f, Average Accuracy: %.3f" % (precision, recall, f1)
    print "# total:", total
else:
    if macro:
        print "# Precision: %.3f (%.2f/%d)" % (precision/retrieved, precision, retrieved)
        precision /= retrieved
        print "# Recall: %.3f (%.2f/%d)" % (recall/relevant, recall, relevant)
        recall /= relevant
        f1 = 2*precision*recall/(precision+recall) if precision+recall!=0.0 else 0.0
        print "# F1: %.3f" % (f1)
    else:
        precision = correct*1.0/retrieved
        recall = correct*1.0/relevant
        f1 = 2*precision*recall/(precision+recall) if precision+recall!=0.0 else 0.0
        print "# Precision: %.3f (%.2f/%d)" % (precision, correct, retrieved)
        print "# Recall: %.3f (%.2f/%d)" % (recall, correct, relevant)
        print "# F1: %.3f" % (f1)
