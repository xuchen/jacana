#! /usr/bin/env python

import re,sys
import xml.sax.saxutils as saxutils

def unescape(s):
    return saxutils.unescape(s, {"&apos;": "'", "&quot;": '"'})


f = open(sys.argv[1])
id = -1
#     <word ind="1" pos="PRP">We</word>
m = re.compile('\s+<word.*pos="(.+)">(.+)<.*')
m_p = re.compile('.*<governor idx="(\d+)".*>.*')
m_c = re.compile('.*<dependent idx="(\d+)".*>.*')
m_d = re.compile('.*<dep type="(.+)">.*')
for line in f:
    if line.startswith('<s id='):
        id += 1
        words = ['ROOT']
        pos = ['ROOT']
        parent2child = {}
        child2parent = {}
    elif line.startswith('    <word ind'):
        matcher = m.match(line)
        #     <word ind="1" pos="PRP">We</word>
        pos.append(matcher.group(1))
        words.append(matcher.group(2))
    elif line.startswith('  </words>'):
        rels = [None]*len(words)
        parent_idx = [None]*len(words)
        named_entities = ['-']*len(words)
    elif line.startswith('  <dep type='):
        #   <dep type="dep">
        matcher = m_d.match(line)
        rel = matcher.group(1)
    elif line.startswith('    <governor idx'):
        #   <governor idx="0">ROOT</governor>
        matcher = m_p.match(line)
        parent = matcher.group(1)
    elif line.startswith('    <dependent idx'):
        #   <dependent idx="2">symbolize</dependent>
        matcher = m_c.match(line)
        child = int(matcher.group(1))
        parent_idx[child] = parent
        if parent == '0': root = str(child)
        rels[child] = rel
    elif line.startswith('</s>'):
        for i in range(len(rels)):
            # Stanford parser omits dependencies for punctuations
            # we add them back
            if rels[i] == None:
                rels[i] = "p"
                parent_idx[i] = root
        print unescape("\t".join(words[1:]))
        print unescape("\t".join(pos[1:]))
        print unescape("\t".join(rels[1:]))
        print unescape("\t".join(parent_idx[1:]))
        print unescape("\t".join(named_entities[1:]))
        print

f.close()


