#! /usr/bin/env python
import re,sys,gzip

# this script appends the original exact answer to the pseudo xml file in ../data
# it works in two steps:
# 1. try to match up with the exact answer in the file trec.answer
#   if no match, go to step 2; if multiple match, leave TODO mark for manual edit
# 2. try to match up with answer patterns in trec.pattern
#   if no match or if multiple match, leave TODO mark for manual edit
# for each match, insert two extra lines: one for answer offset and one for lexicon

# there usually is a mismatch between the pattern file and sentence splitter:
# 0?\.\s*10(?:%|(?:pct)|(?:per))? doesn't match 0.10 % 'bc the the space
# in '0.10 %', so I manually added \s* in the pattern in front of %, $, 's, etc

# ./2.appendAnswer.py train|dev|test|train-full


idx2answer = {}
if sys.argv[1] == 'train':
    answer_f = open('trec8.answer')
    for line in answer_f:
        idx, answer = line.strip().split(' ', 1)
        if idx not in idx2answer:
            idx2answer[idx] = set()
        # remove all empty spaces, we compare char by char
        idx2answer[idx].add(answer.lower().replace(' ', ''))
    answer_f.close()

if sys.argv[1] == 'train':
    pattern_f = open('trec8.pattern')
elif sys.argv[1] == 'dev' or sys.argv[1] == 'test':
    pattern_f = open('trec13factpats.txt')
elif sys.argv[1] == 'train-full':
    pattern_f = open('trec8-12.pattern')

idx2pattern = {}
for line in pattern_f:
    idx, pattern = line.strip().split(' ', 1)
    if idx not in idx2pattern:
        idx2pattern[idx] = set()
    idx2pattern[idx].add(re.compile(pattern.lower()))
pattern_f.close()

if sys.argv[1] != 'train-full':
    xml_f = open('%s-less-than-40.num-recovered.xml'%sys.argv[1])
    out_f = open('%s-less-than-40.answer-appended.xml'%sys.argv[1], 'w')
else:
    xml_f = gzip.open('train2393.num-recovered.xml.gz')
    out_f = gzip.open('train2393.answer-appended.xml.gz', 'wb')

Q=0
P=1
N=2
case=None
idx=0
write_answer = False
matcher = re.compile("<QApairs id='(.*)'>")
for line in xml_f:
    line = line.strip()
    if line.startswith('<positive>'):
        case = P
    elif line.startswith('<negative>'):
        case = N
    elif line.startswith('<question>'):
        case = Q
    elif line.startswith('<QApair'):
        m = matcher.match(line)
        idx = m.group(1)
    elif line.startswith('<') and line.endswith('>'):
        None# do nothing
    else:
        if case == P:
            write_answer = True
            ans_str='TODO'
            idx_str='TODO'
            todo_multi = ''
            if len(idx2answer) != 0:
                splits = line.lower().split()
                word_offset = [0] + [len(w) for w in splits]
                for i in range(1, len(word_offset)):
                    word_offset[i] += word_offset[i-1]
                char_line = line.lower().replace('\t', '')
                for answer in idx2answer[idx]:
                    off_start = char_line.find(answer)
                    off_end = off_start + len(answer)
                    if char_line.find(answer, off_end) != -1:
                        # multiple answers in one sentence, we don't know which one is right
                        todo_multi = '\tTODO_MULTI_CHAR'
                    if off_start != -1:
                        try:
                            idx_start = word_offset.index(off_start)
                            idx_end = word_offset.index(off_end)
                            ans_str = '\t'.join(line.split()[idx_start:idx_end]) + todo_multi
                            idx_str = '\t'.join([str(i+1) for i in range(idx_start, idx_end)]) + todo_multi
                            break
                        except:
                            None
            todo_multi = ''
            if ans_str == 'TODO':
                count = 0
                for pattern in idx2pattern[idx]:
                    word_line = line.lower().replace('\t', ' ')
                    ite = pattern.finditer(word_line)
                    if ite != None:
                        for m in ite:
                            count += 1
                            off_start = m.start()
                            off_end = m.end()
                            if pattern.search(word_line, off_end) != None:
                                # multiple answers
                                todo_multi = '\tTODO_MULTI_PATTERN'
                            idx_start = len(word_line[0:off_start].split())
                            idx_end = idx_start + len(word_line[off_start:off_end].split())
                            if ans_str == 'TODO':
                                ans_str = ''
                                idx_str = ''
                            if count != 1:
                                ans_str += '#\t'
                                idx_str += '#\t'
                            ans_str += '\t'.join(line.split()[idx_start:idx_end]) + '\t'
                            idx_str += '\t'.join([str(i+1) for i in range(idx_start, idx_end)]) + '\t'
                if count != 1:
                    todo_multi = 'TODO_MULTI_PATTERN'
                    ans_str += todo_multi
                    idx_str += todo_multi
                    
            case = None
    if line.startswith('<') and write_answer:
        print >> out_f, ans_str
        print >> out_f, idx_str
        write_answer = False
    print >> out_f, line

xml_f.close()
out_f.close()
