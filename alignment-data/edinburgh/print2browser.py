#!/usr/bin/env python

import codecs, os, sys, json
from collections import defaultdict


# convert source index to target index list mappings to an
# int to sorted int list dict, assuming a defaultdict(list)
def add_mappings(mappings, alignment_dict):
    for mapping in mappings:
        src_index = mapping[0]
        for targ_index in mapping[1]:
            alignment_dict[src_index].append(targ_index)
            alignment_dict[src_index].sort()

# get atomic phrase pairs as a list of int tuples
def get_atomic_phrase_pairs(alignment_dict):
    # make inverse target tuple to source list dict
    inv_dict = defaultdict(list)
    for (isrc,targlist) in alignment_dict.items():
        targtup = tuple(targlist)
        inv_dict[targtup].append(isrc)
        inv_dict[targtup].sort()
    # return list in src, target order
    retval = [(tuple(srclist),targtup) for (targtup,srclist) in inv_dict.items()]
    retval.sort()
    return retval

# get tokens, with '..' for discontinuities
def get_tokens(tup, tokens):
    retlist = [tokens[tup[0]]]
    for (i,index) in enumerate(tup[1:]):
        if tup[i] != index - 1:
            retlist.append('..')
        retlist.append(tokens[index])
    return ' '.join(retlist)

    
# print the gold alignments for the given paraphrases and partition
# according to the sure_only option
def print_gold_option(paras, partition, sure_only=True):

    option = 'sure' if sure_only else 'sure+poss'
    outfn = 'gold.' + partition + '.' + option + '.json'
    outf = open(outfn, 'w')
    streamWriter = codecs.lookup("utf-8")[-1]
    outw = streamWriter(outf)

    print 'Writing gold', option, 'alignments to', outfn, '...'

    jlist = []

    for pair in paras:

        align = {}
        # print paraphrase pair
        #outw.write('##### ' + pair['id'] + ' #####\n\n')
        align['id'] = pair['id']
        align['name'] = pair['id']
        src, targ = pair['S']['string'], pair['T']['string']
        src_toks, targ_toks = src.split(), targ.split()
        #outw.write('S: ' + src + '\n')
        #outw.write('T: ' + targ + '\n\n')
        align['source'] = src
        align['target'] = targ

        # get chosen alignments as atomic phrase pairs
        anns = pair['annotations']
        gold = anns[pair['train']]
        adict = defaultdict(list)
        gold_sure = gold['S']
        add_mappings(gold_sure, adict)
        if not sure_only:
            gold_poss = gold['P']
            add_mappings(gold_poss, adict)
        atom_phr_pairs = get_atomic_phrase_pairs(adict)

        # print atomic phrase pairs
        aligns = []
        for (srctup, targtup) in atom_phr_pairs:
            #outw.write(get_tokens(srctup, src_toks) + ' <-> ' +
            #           get_tokens(targtup, targ_toks) + '\n')
            #outw.write(str(srctup) + ' <-> ' + str(targtup) + '\n')
            for i in srctup:
                for j in targtup:
                    aligns.append(str(i)+"-"+str(j))
        #outw.write('\n\n')
        align['sureAlign'] = " ".join(aligns)
        align['possibleAlign'] = ""
        jlist.append(align)

    outf.flush()
    outw.write(json.dumps(jlist, indent=4, separators=(',', ': ')))
    print 'Done.'
    print
    

# print the gold alignments for the given partition
def print_gold(partition):

    fn = partition + '.json'
    print
    print 'Reading data from', fn, '...'
    json_file = open(fn, 'r')
    json_data = json.load(json_file)
    json_file.close()
    print 'Done.'

    paras = json_data['paraphrases']
    print 'Read', len(paras), 'paraphrase pairs.'
    print

    print_gold_option(paras, partition, sure_only=True)
    print_gold_option(paras, partition, sure_only=False)


# print the gold alignments for both partitions
def main(argv):
    
    if len(argv[1:]) > 0:
        print 'Ignoring extra args:', argv[1:]

    print_gold('train')
    print_gold('test')


if __name__ == '__main__' : main(sys.argv)
