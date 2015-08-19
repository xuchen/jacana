#! /usr/bin/env python

import json, codecs

ftype = 'test'

out_f = open('edinburgh.%s.sents' % ftype, 'w')


with codecs.open('../gold.%s.sure.json' % ftype) as f:
    aligns = json.load(f)
    for ss in aligns:
        print >> out_f, ss['source'].encode('utf-8')
        print >> out_f, ss['target'].encode('utf-8')

out_f.close()
