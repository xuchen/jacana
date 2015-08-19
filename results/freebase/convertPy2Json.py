#! /usr/bin/env python

# convert the fomat of:
# webquestions.test.answers and webquestions.test.forced_answers 
# from reading using python's eval() method to json.loads()
# during the conversion confidence scores will be lost

import sys, json

with open(sys.argv[1]) as f:
    for line in f:
        if not line.startswith("#"):
            s,gold,predicted = line.strip().split("\t")
            gold = json.dumps(list(eval(gold)))
            predicted = json.dumps(list(eval(predicted).keys()))
            #if not predicted == "[]":
            print '%s\t%s\t%s' % (s, gold, predicted)
