#! /usr/bin/env python

import sys,gzip
# in the original data numbers are replaced with <num>
# this script recovers it.

# ./1.recoverNum.py  train|dev|test|train-full

# line up!
if sys.argv[1] == 'train':
    xml_f = open('../data/train-less-than-40.xml')
    out_f = open('train-less-than-40.num-recovered.xml', 'w')
elif sys.argv[1] == 'dev':
    xml_f = open('../data/dev-less-than-40.xml')
    out_f = open('dev-less-than-40.num-recovered.xml', 'w')
elif sys.argv[1] == 'test':
    xml_f = open('../data/test-less-than-40.xml')
    out_f = open('test-less-than-40.num-recovered.xml', 'w')
elif sys.argv[1] == 'train-full':
    xml_f = gzip.open('../data/train2393.xml.gz')
    out_f = gzip.open('train2393.num-recovered.xml.gz', 'wb')

def readPOSInput(fname):
    if fname.endswith('.gz'):
        f = gzip.open(fname)
    else:
        f = open(fname)
    lines = [line.lower().strip() for line in f if not (line.startswith('<') and line.strip().endswith('>'))]
    f.close()
    #print len(lines)
    return lines

if sys.argv[1] == 'train':
    pos_lines = readPOSInput('../training/1-100/Train1-100.Positive-J.POSInput')
    neg_lines = readPOSInput('../training/1-100/Train1-100.Negative-T.POSInput')
    q_lines = readPOSInput('../training/1-100/Train1-100.Question.POSInput')
elif sys.argv[1] == 'dev':
    pos_lines = readPOSInput('../dev/Dev.Positive-J.POSInput')
    neg_lines = readPOSInput('../dev//Dev.Negative-T.POSInput')
    q_lines = readPOSInput('../dev/Dev.Question.POSInput')
elif sys.argv[1] == 'test':
    pos_lines = readPOSInput('../testing/Test.Positive-J.POSInput')
    neg_lines = readPOSInput('../testing/Test.Negative-T.POSInput')
    q_lines = readPOSInput('../testing/Test.Question.POSInput')
elif sys.argv[1] == 'train-full':
    pos_lines = readPOSInput('../training/1-2393/Train1-2393.Positive-M.POSInput.gz')
    neg_lines = readPOSInput('../training/1-2393/Train1-2393.Negative-M.POSInput.gz')
    q_lines = readPOSInput('../training/1-2393/Train1-2393.Question.POSInput.gz')

def assignNum(line, num_line):
    splits = line.split()
    num_splits = num_line.split()
    for i in range(len(splits)):
        if splits[i].lower() == num_splits[i].lower():
            continue
        elif splits[i] == '<num>':
            splits[i] = num_splits[i]
        else:
            print 'wrong match: %s\t%s' % (line, num_line)
    line = '\t'.join(splits)
    return line

Q=0
P=1
N=2
case=None
xml_lines = xml_f.readlines()
counter = 0
while counter < len(xml_lines):
    line = xml_lines[counter].strip()
    if line.startswith('<positive>'):
        case = P
    elif line.startswith('<negative>'):
        case = N
    elif line.startswith('<question>'):
        case = Q
    elif line.startswith('<') and line.endswith('>'):
        None# do nothing
    else:
        if case != None:
            splits = line.split()
            if case == Q:
                lines = q_lines
            elif case == P:
                lines = pos_lines
            elif case == N:
                lines = neg_lines
            while len(lines[0].split()) != len(splits):
                lines.pop(0)
            line = assignNum(line, lines.pop(0))
            case = None
    print >> out_f, line
    counter += 1

xml_f.close()
out_f.close()
