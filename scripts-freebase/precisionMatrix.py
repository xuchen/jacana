#! /usr/bin/env python

# this script computs the precision matrix between jacana and sempre

# threhold =  0.2
# 		                jacana
# 		            right	wrong
# sempre right	634 (0.31)	257 (0.13)
# sempre wrong	417 (0.21)	724 (0.36)
# 
# 
# 
# threhold =  0.5
# 		                jacana
# 		            right	wrong
# sempre right	429 (0.21)	321 (0.16)
# sempre wrong	366 (0.18)	916 (0.45)
# 
# 
# 
# threhold =  0.8
# 		                jacana
# 		            right	wrong
# sempre right	192 (0.09)	375 (0.18)
# sempre wrong	201 (0.10)	1264 (0.62)
# 
# 
# 
# threhold =  1.0
# 		                jacana
# 		            right	wrong
# sempre right	153 (0.08)	383 (0.19)
# sempre wrong	136 (0.07)	1360 (0.67)

def read_f1(fname):
    q_f1 = []
    with open(fname) as f:
        for line in f:
            q, correct, _ = line.split('\t')
            correct = float(correct)
            q_f1.append((q, correct))
    return q_f1

j_q_f1 = read_f1('../results/freebase/webquestions.test.forced_answers.scores')
s_q_f1 = read_f1('../results/freebase/sempre.35.7.f1')

# any score >= this threshold will be considered correct
thresholds = [0.2, 0.5, 0.8, 1.0]

total = len(j_q_f1)*1.0
for threshold in thresholds:
    j_right_s_right = 0
    j_right_s_wrong = 0
    j_wrong_s_right = 0
    j_wrong_s_wrong = 0
    for j, s in zip(j_q_f1, s_q_f1):
        j_f1 = j[1]
        s_f1 = s[1]
        if j_f1 >= threshold and s_f1 >= threshold:
            j_right_s_right += 1
        elif j_f1 >= threshold and s_f1 < threshold:
            j_right_s_wrong += 1
        elif j_f1 < threshold and s_f1 < threshold:
            j_wrong_s_wrong += 1
        elif j_f1 < threshold and s_f1 >= threshold:
            j_wrong_s_right += 1
    assert j_right_s_right + j_right_s_wrong + j_wrong_s_right + j_wrong_s_wrong == total 
    print "threhold = ", threshold
    print "\t\t   jacana"
    print "\t\tright\twrong"
    print "sempre right\t%d (%.2f)\t%d (%.2f)" % (j_right_s_right, j_right_s_right/total, j_wrong_s_right, j_wrong_s_right/total)
    print "sempre wrong\t%d (%.2f)\t%d (%.2f)" % (j_right_s_wrong, j_right_s_wrong/total, j_wrong_s_wrong, j_wrong_s_wrong/total)
    print "\n"*2
