#! /usr/bin/env python

import json, codecs

ftype = 'dev'

out_f = open('mt-reference.%s.sents' % ftype, 'w')


with codecs.open('../mt-reference.%s.json' % ftype) as f:
    aligns = json.load(f)
    for ss in aligns:
        print >> out_f, ss['source'].encode('utf-8')
        print >> out_f, ss['target'].encode('utf-8')

out_f.close()
