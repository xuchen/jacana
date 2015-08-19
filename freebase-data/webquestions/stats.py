#! /usr/bin/env python

import json

f = open('webquestions.examples.train.json')
js = json.load(f)
f.close()

aux = set(["is", "are", "was", "were", "does", "do", "did"])

total = len(js)

q2i = {}
for j in js:
    qs = j['utterance'].split()
    q = qs[0]
    if q == "what" or q == "which" or q == "how":
        q += " " + qs[1]
        #if qa[1] not in aux:
    if q not in q2i:
        q2i[q] = 0
    q2i[q] += 1

for w in sorted(q2i, key=q2i.get, reverse=True):
    print "%s\t\t%d\t%.3f" % (w, q2i[w], q2i[w]*1.0/total)

##
## who     744 0.197
## where       717 0.190
## what is     305 0.081
## what did        155 0.041
## when        148 0.039
## what are        104 0.028
## what was        80  0.021
## what language       70  0.019
## what kind       68  0.018
## what type       68  0.018
## what does       68  0.018
## what year       61  0.016
## what country        54  0.014
## what movies     52  0.014
## what countries      48  0.013
## what to     45  0.012
## what do     39  0.010
## what currency       36  0.010
## what time       36  0.010
## what team       36  0.010
## what college        29  0.008
## what state      26  0.007
## what happened       24  0.006
## what timezone       23  0.006
## what city       23  0.006
## what county     22  0.006
## what airport        18  0.005
## what money      18  0.005
## what years      17  0.004
## in      17  0.004
## what school     15  0.004
## what form       15  0.004
## what teams      14  0.004
## what continent      12  0.003
## which countries     12  0.003
## what films      11  0.003
## what other      11  0.003
## what religion       11  0.003
## which country       10  0.003
## what movie      9   0.002
## what political      9   0.002
## what books      9   0.002
## what influenced     8   0.002
## what style      7   0.002
## what inspired       7   0.002
## what were       7   0.002
## what position       7   0.002
## what instrument     7   0.002
## what party      7   0.002
## what has        7   0.002
## what character      7   0.002
## what songs      7   0.002
## what date       6   0.002
## what book       5   0.001
## what sports     5   0.001
## what disease        5   0.001
## which airport       5   0.001
## what languages      5   0.001
## what sport      5   0.001
## what's      5   0.001
## what club       5   0.001
## what job        4   0.001
## what university     4   0.001
## what government     4   0.001
## what 4      4   0.001
## what war        4   0.001
## what should     4   0.001
## what religions      4   0.001
## which states        4   0.001
## what art        4   0.001
## how much        4   0.001
## how many        4   0.001
## what two        4   0.001
## what made       4   0.001
## what region     4   0.001
## what cities     4   0.001
## what season     4   0.001
## what all        4   0.001
## what characters     4   0.001
## what channel        4   0.001
## what part       4   0.001
## what office     3   0.001
## what famous     3   0.001
## what events     3   0.001
## how old     3   0.001
## what company        3   0.001
## who's       3   0.001
## what music      3   0.001
## what jobs       3   0.001
## what inventions     3   0.001
## what nationality        3   0.001
## what system     3   0.001
## what episode        3   0.001
## what wars       3   0.001
## what highschool     3   0.001
## on      3   0.001
## what show       3   0.001
## what guitar     3   0.001
## what shows      3   0.001
## what color      3   0.001
## what instruments        3   0.001
## which province      2   0.001
## what bible      2   0.001
## what organization       2   0.001
## what the        2   0.001
## what caused     2   0.001
## what illnesses      2   0.001
## what province       2   0.001
## what major      2   0.001
## what illness        2   0.001
## what 5      2   0.001
## which team      2   0.001
## what car        2   0.001
## what role       2   0.001
## what division       2   0.001
## what clubs      2   0.001
## which college       2   0.001
## what else       2   0.001
## what can        2   0.001
## what race       2   0.001
## what planet     2   0.001
## what high       2   0.001
## what games      2   0.001
## what kourtney       2   0.001
## what tv     2   0.001
## which party     2   0.001
## what contribution       2   0.001
## what airlines       2   0.001
## what god        2   0.001
## what states     2   0.001
## what would      2   0.001
## with        2   0.001
## what four       2   0.001
## what band       2   0.001
## what age        2   0.001
## what 3      2   0.001
## what sea        2   0.001
## what awards     2   0.001
## what battles        2   0.001
## what types      2   0.001
## what land       2   0.001
## what places     2   0.001
## what three      2   0.001
## what sort       2   0.001
## where's     2   0.001
## what drugs      2   0.001
## what killed     2   0.001
## which three     1   0.000
## what hotel      1   0.000
## which kardashians       1   0.000
## what children's     1   0.000
## what theme      1   0.000
## what brand      1   0.000
## what ball       1   0.000
## what bass       1   0.000
## what good       1   0.000
## what freeview       1   0.000
## what unicef     1   0.000
## what things     1   0.000
## what techniques     1   0.000
## what makes      1   0.000
## which city      1   0.000
## what empire     1   0.000
## what schools        1   0.000
## what dialects       1   0.000
## what landforms      1   0.000
## what degree     1   0.000
## during      1   0.000
## what wild       1   0.000
## what district       1   0.000
## what education      1   0.000
## what radio      1   0.000
## what going      1   0.000
## what a      1   0.000
## what middle     1   0.000
## what sights     1   0.000
## what condition      1   0.000
## what planes     1   0.000
## what challenges     1   0.000
## what market     1   0.000
## what round      1   0.000
## what equipment      1   0.000
## what magazine       1   0.000
## what secondary      1   0.000
## what culture        1   0.000
## what main       1   0.000
## what atom       1   0.000
## what nestle     1   0.000
## what town       1   0.000
## which books     1   0.000
## what present        1   0.000
## what super      1   0.000
## what organism       1   0.000
## which continent     1   0.000
## what victoria       1   0.000
## what group      1   0.000
## what musical        1   0.000
## what battle     1   0.000
## what football       1   0.000
## what song       1   0.000
## which jane      1   0.000
## which legend        1   0.000
## what technique      1   0.000
## which hmv       1   0.000
## which part      1   0.000
## which wife      1   0.000
## what represents     1   0.000
## what exactly        1   0.000
## which kennedy       1   0.000
## which asian     1   0.000
## what features       1   0.000
## what island     1   0.000
## what percent        1   0.000
## what record     1   0.000
## what jamaican       1   0.000
## what business       1   0.000
## what medium     1   0.000
## what cases      1   0.000
## into        1   0.000
## what colleges       1   0.000
## what boudicca       1   0.000
## how rich        1   0.000
## which roman     1   0.000
## what mark       1   0.000
## what important      1   0.000
## what utc        1   0.000
## what bella      1   0.000
## from        1   0.000
## what products       1   0.000
## what primary        1   0.000
## what american       1   0.000
## what hemisphere     1   0.000
## what river      1   0.000
## which is        1   0.000
## which four      1   0.000
## what george     1   0.000
## what subatomic      1   0.000
## what ethnicity      1   0.000
## how long        1   0.000
## what lead       1   0.000
## what blood      1   0.000
## what dialect        1   0.000
## which english       1   0.000
## what areas      1   0.000
## which island        1   0.000
## what zip        1   0.000
## what kinda      1   0.000
## what undergraduate      1   0.000
## what writers        1   0.000
## what ship       1   0.000
## which ocean     1   0.000
## what cancer     1   0.000
## what beach      1   0.000
## what albums     1   0.000
## what stadium        1   0.000
## what basketball     1   0.000
## what drug       1   0.000
## what website        1   0.000
## what jersey     1   0.000
## what groups     1   0.000
## what military       1   0.000
## what strings        1   0.000
## what label      1   0.000
## what legal      1   0.000
## what each       1   0.000
## what 6      1   0.000
## what experiments        1   0.000
## what gunfight       1   0.000
## what colony     1   0.000
## what hardships      1   0.000
## what tourist        1   0.000
## what professional       1   0.000
## what place      1   0.000
## what invention      1   0.000
## what voice      1   0.000
## what oprah      1   0.000
## what prep       1   0.000
## what branch     1   0.000
## what nation     1   0.000
## what discovery      1   0.000
## what albert     1   0.000
## what gauge      1   0.000
## what food       1   0.000
## what will       1   0.000
## what times      1   0.000
## what animal     1   0.000
## which paris     1   0.000
