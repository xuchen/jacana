#! /usr/bin/env python

import json

ftype = 'test'
def get_list(anno, ftype=ftype):
    with open('RTE2_%s_%s.align.json' % (ftype, anno)) as f:
        return json.load(f)

a_list = get_list('A')
b_list = get_list('B')
c_list = get_list('C')

sure_list = []
sure_poss_list = []
for a,b,c in zip(a_list, b_list, c_list):
    a_set = set(a['sureAlign'].split())
    b_set = set(b['sureAlign'].split())
    c_set = set(c['sureAlign'].split())
    sure_set = a_set.intersection(b_set).intersection(c_set)
    sure_poss_set = a_set.union(b_set).union(c_set)
    poss_set = sure_poss_set.difference(sure_set)

    # reuse
    a['sureAlign'] = " ".join(sorted(list(sure_set)))
    a['possibleAlign'] = " ".join(sorted(list(poss_set)))
    sure_list.append(a)

    b['sureAlign'] = " ".join(sorted(list(sure_poss_set)))
    b['possibleAlign'] = " ".join(sorted(list(poss_set)))
    sure_poss_list.append(b)

# this will generate my own oversion of "sure" alignment, but 
# it is different than MacCartney's, so let's not generate it
# with open('RTE2_%s_M.align.sure.json' % ftype, 'w') as f:
#     json.dump(sure_list, f, indent=4, separators=(',', ': '))

with open('RTE2_%s_M.align.sure+poss.json' % ftype, 'w') as f:
    json.dump(sure_poss_list, f, indent=4, separators=(',', ': '))
