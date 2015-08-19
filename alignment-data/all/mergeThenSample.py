#! /usr/bin/env python

import json, random

# this script merges all aligned MSR/Edinburgh data and splits them into
# train (80%) and test (80%).
#
# the purpose is to train an aligner model with the most data available
# and use it in real life applications

phrasal = True

split = 0.8

if not phrasal:
  files = ['../msr/converted/RTE2_dev_M.align.json', '../msr/converted/RTE2_test_M.align.json', '../edinburgh/gold.train.sure.json', '../edinburgh/gold.test.sure.json']
else:
  files = ['../msr/converted/synthetic-phrases/RTE2_dev_M.synthetic-phrases.json', '../msr/converted/synthetic-phrases/RTE2_dev_M.synthetic-phrases.json', '../edinburgh/synthetic-phrases/gold.synthetic-phrases.train.sure.json', '../edinburgh/synthetic-phrases/gold.synthetic-phrases.test.sure.json']

aligns = []
for fname in files:
  f = open(fname)
  a = json.load(f)
  aligns += a
  f.close()


random.shuffle(aligns)

if not phrasal:
  out_train = 'msr.edinburgh.train.json'
  out_test = 'msr.edinburgh.test.json'
else:
  out_train = 'msr.edinburgh.synthetic-phrases.train.json'
  out_test = 'msr.edinburgh.synthetic-phrases.test.json'

cut = int(len(aligns)*0.8)

f_train = open(out_train, 'w')
json.dump(aligns[0:cut], f_train, indent = 4, separators=(',', ': '))
f_train.close()

f_test = open(out_test, 'w')
json.dump(aligns[cut:], f_test, indent = 4, separators=(',', ': '))
f_test.close()
