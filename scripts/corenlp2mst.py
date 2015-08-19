#! /usr/bin/env python

import re,sys
import xml.sax.saxutils as saxutils

def unescape(s):
    return saxutils.unescape(s, {"&apos;": "'", "&quot;": '"'})


id = -1
get_dep = False
no_parse = False
#     <word ind="1" pos="PRP">We</word>
m = re.compile('\s+<word.*pos="(.+)">(.+)<.*')
m_word = re.compile('.*<word>(\S+)</word>.*')
m_pos = re.compile('.*<POS>(\S+)</POS>.*')
m_ner = re.compile('.*<NER>(\S+)</NER>.*')
m_l = re.compile('.*<lemma>(\S+)</lemma>.*')
m_p = re.compile('.*<governor idx="(\d+)".*>.*')
m_c = re.compile('.*<dependent idx="(\d+)".*>.*')
m_d = re.compile('.*<dep type="(.+)">.*')
#with open(sys.argv[1]) as f:
if True:
    for line in sys.stdin:
        line = line.strip()
        if line.startswith('<sentence id='):
            id += 1
            words = ['ROOT']
            pos = ['ROOT']
            ner = ['ROOT']
            parent2child = {}
            child2parent = {}
            root = None
            no_parse = False
            # if line.strip().endswith('skipped="true"/>'):
            #     print "===SKIPPED===\n"*5
        elif line.startswith('<word>'):
            matcher = m_word.match(line)
            words.append(matcher.group(1))
        elif line.startswith('<POS>'):
            matcher = m_pos.match(line)
            pos.append(matcher.group(1))
        elif line.startswith('<NER>'):
            matcher = m_ner.match(line)
            ner.append(matcher.group(1))
        elif line.startswith('<dependencies type="basic-dependencies"'):
            if line.endswith('basic-dependencies">'):
                get_dep = True
            elif line.endswith('basic-dependencies"/>'):
                ## no parse
                get_dep = False
                no_parse = True
        elif line.startswith('</dependencies'):
            get_dep = False
        elif line.startswith('</tokens>'):
            rels = [None]*len(words)
            parent_idx = [None]*len(words)
        elif get_dep and line.startswith('<dep type='):
            #   <dep type="dep">
            matcher = m_d.match(line)
            rel = matcher.group(1)
        elif get_dep and line.startswith('<governor idx'):
            #   <governor idx="0">ROOT</governor>
            matcher = m_p.match(line)
            parent = matcher.group(1)
        elif get_dep and line.startswith('<dependent idx'):
            #   <dependent idx="2">symbolize</dependent>
            matcher = m_c.match(line)
            child = int(matcher.group(1))
            parent_idx[child] = parent
            if parent == '0': root = str(child)
            rels[child] = rel
        elif line.startswith('</sentence>'):
            for i in range(len(rels)):
                # Stanford parser omits dependencies for punctuations
                # we add them back
                if rels[i] == None:
                    rels[i] = "p"
                    parent_idx[i] = root
                    if root is None:
                        if i > 0:
                            # use the previous word as parent
                            parent_idx[i] = str(i)
                        else: # if this is the first word in a sent, then use itself
                            parent_idx[i] = "1"
                        # print >> sys.stderr, "Error: root is none for: ", id
                    else: 
                        parent_idx[i] = root
            if not no_parse:
                print unescape("\t".join(words[1:]))
                print unescape("\t".join(pos[1:]))
                print unescape("\t".join(rels[1:]))
                print unescape("\t".join(parent_idx[1:]))
                print unescape("\t".join(ner[1:]))
                print


