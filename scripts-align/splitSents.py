#! /usr/bin/env python

import sys
import nltk.data
from nltk import word_tokenize

sent_detector = nltk.data.load('tokenizers/punkt/english.pickle')


for line in sys.stdin:
    sents1, sents2 = line.strip().split("\t")
    for sent1 in sent_detector.tokenize(sents1):
        for sent2 in sent_detector.tokenize(sents2):
            print "%s\t%s" % (sent1, sent2)
    print "#\t#"
