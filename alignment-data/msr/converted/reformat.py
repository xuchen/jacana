#! /usr/bin/env python

import json


with open('RTE2_dev_M.align.json') as f:
    j_list = json.load(f)
    for j in j_list:
        j['sureAlign'] = " ".join(sorted(j['sureAlign'].split()))
    print json.dumps(j_list, indent=4, separators=(',', ': '))
