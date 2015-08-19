#! /usr/bin/env python

import gzip, sys, re

precision_p = re.compile('.* partCorrect=(\S+) .*')
prob_p = re.compile('.* prob=(\S+),.*')


# ./parseSempreResult.py /tmp/2332_exec_test.log.gz | tee ../results/freebase/sempre.35.7.f1
# parse the result (log file) from SEMPRE, output the question, the 'partialCorrect' score, and the prob (as confidence)
prev_q = ''
q = ''
precision = 0.0
prob = 0.0
with gzip.open(sys.argv[1]) as f:
    for line in f:
        line = line.strip()
        if line.startswith('iter=3.test: example '):
            if prev_q != '':
                print "%s\t%.3f\t%.3f" % (q, precision, prob)
            prev_q = q
        elif line.startswith('Example: '):
            q = " ".join(line.split()[1:-1])
        elif line.startswith('Current: '):
            m = precision_p.match(line)
            precision = float(m.group(1))
        elif line[4:9] == '@0000':
            m = prob_p.match(line)
            prob = float(m.group(1))
        elif line.startswith('Processing iter=4.train: 3778 examples'):
            print "%s\t%.3f\t%.3f" % (q, precision, prob)

#           iter=3.test: example 0/2032: lib/data/webquestions/dataset_11/webquestions.examples.test.json:0 {
#             Example: what does jamaican people speak? {
#            Current: parsed=1 numTokens=6 maxCellSize=3216 fallOffBeam=1 coarseParseTime=4 parseTime=1114 valueCorrect=0 formulaCorrect=0 correct=0 oracle=0 partCorrect=0 partOracle=0 meanBeamJump=5.972 meanRootBeamJump=0 numCandidates=200 parsedNumCandidates=200 exec-cached=1
#               Pred@0000: (derivation (formula (fb:people.ethnicity.languages_spoken fb:en.jamaican_creole)) (value (list (name fb:en.chinese _jamaican "Chinese Jamaicans") (name fb:en.jamaicans_of_african_ancestry "Jamaicans of African ancestry") (name fb:en.jamaican_american "Jam aican American") (name fb:en.indo-caribbean Indo-Caribbean) (name fb:en.jamaican_british "British Jamaican") (name fb:en.jamaican_australian "Jamaican Australian") (name fb:en.jamaican_canadian "Jamaican Canadian") (name fb:m.0hnb50_ "Lebanese immigration to Jamaica") (name fb:m.  0dgnbjt "Igbo people in Jamaica") (name fb:en.chinese_caribbean "Chinese Caribbean"))) (type fb:people.ethnicity)) [score=8.227, prob=0.589, comp=0]
