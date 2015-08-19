#! /usr/bin/env python

import json

with open('webquestions.examples.train.json') as f:
    j = json.load(f)
    for q in j:
        print q['utterance']
