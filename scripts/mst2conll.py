#! /usr/bin/python

# http://ilk.uvt.nl/conll/#dataformat
# format accepted by whatsrong (http://code.google.com/p/whatswrong/)
# in CoNLL 2006 format
# and http://www.ark.cs.cmu.edu/parseviz/
# other visualization software:
# http://mailman.uib.no/public/corpora/2012-June/015755.html

import sys

f = open(sys.argv[1],'r');

word_line = f.readline()
while word_line != '':
    word = word_line.strip().split()
    pos = f.readline().strip().split()
    dep = f.readline().strip().split()
    index = f.readline().strip().split()
    chunk = f.readline().strip().split()
    f.readline() # empty line
    for i in range(len(word)):
        print "\t".join([str(i+1), word[i], '_', pos[i], '_', '_', index[i], dep[i], '_', '_'])
    print

    word_line = f.readline()
