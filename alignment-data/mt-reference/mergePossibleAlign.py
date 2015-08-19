#! /usr/bin/env python

import sys, json
# merges possbile aligns into sure aligns so we can directly train on possible aligns

with open(sys.argv[1]) as f:
   j_list = json.load(f) 
   for j in j_list:
       if len(j['possibleAlign']) != 0:
           j['sureAlign'] += " " + j['possibleAlign']

print json.dumps(j_list, indent=4, separators=(',', ': '))
