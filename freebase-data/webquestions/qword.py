#! /usr/bin/env python

import re, operator

q2dep = {}
# r = re.compile("(\w+) --(\w+)--> (\w+)")
with open('qword.txt') as f:
    for line in f:
        if line.find("-->") != -1:
            # m = r.match(line.strip())
            # (qword, dep, parent) = m.groups()
            qdep, parent = line.strip().rsplit(" ", 1)
            if qdep not in q2dep:
                q2dep[qdep] = {}
            if parent not in q2dep[qdep]:
                q2dep[qdep][parent] = 0
            q2dep[qdep][parent] += 1

for qdep in sorted(q2dep):
    print qdep
    v = q2dep[qdep]
    print sorted(v.iteritems(), key=operator.itemgetter(1), reverse=True)
    # print v


"""
how long --advmod-->
[('long', 1)]
how many --advmod-->
[('many', 4)]
how much --advmod-->
[('much', 4)]
how old --advmod-->
[('old', 3)]
how rich --advmod-->
[('rich', 1)]
what --conj_and-->
[('who', 2), ('make', 1)]
what --cop-->
[('is', 224), ('are', 96), ('was', 50), ('were', 7), ("'s", 5)]
what --dep-->
[('do', 30), ('die', 23), ('is', 22), ('known', 19), ('see', 15), ('died', 10), ('call', 8), ('border', 5), ('in', 4), ('go', 4), ('has', 4), ('look', 4), ('been', 3), ('stand', 3), ('have', 2), ('played', 2), ('win', 2), ('stands', 2), ('make', 2), ('used', 2), ('get', 2), ('name', 2), ('consist', 2), ('searching', 1), ('named', 1), ('own', 1), ('shot', 1), ('beckham', 1), ('share', 1), ('corwin', 1), ('currency', 1), ('export', 1), ('trained', 1), ('speak', 1), ('city', 1), ('lead', 1), ('won', 1), ('ringwald', 1), ('fought', 1), ('want', 1), ('star', 1), ('like', 1), ('trying', 1), ('directed', 1), ('made', 1), ('did', 1), ('flow', 1), ('influenced', 1), ('conducted', 1), ('starting', 1), ('wrote', 1)]
what --det-->
[('language', 69), ('type', 69), ('kind', 66), ('year', 61), ('countries', 56), ('country', 55), ('movies', 46), ('team', 40), ('currency', 37), ('college', 29), ('state', 26), ('city', 24), ('school', 23), ('timezone', 23), ('county', 22), ('airport', 19), ('years', 18), ('time', 18), ('zone', 17), ('teams', 16), ('money', 15), ('form', 15), ('continent', 14), ('party', 13), ('religion', 11), ('films', 11), ('books', 10), ('system', 9), ('movie', 8), ('style', 7), ('character', 7), ('instrument', 7), ('book', 7), ('position', 7), ('songs', 7), ('languages', 6), ('date', 6), ('sport', 5), ('sports', 5), ('war', 5), ('channel', 5), ('disease', 5), ('all', 4), ('office', 4), ('show', 4), ('religions', 4), ('university', 4), ('jobs', 4), ('job', 4), ('cities', 4), ('region', 4), ('instruments', 4), ('music', 4), ('season', 4), ('part', 4), ('government', 4), ('characters', 4), ('inventions', 3), ('people', 3), ('guitar', 3), ('nationality', 3), ('movement', 3), ('color', 3), ('club', 3), ('company', 3), ('wars', 3), ('events', 3), ('highschool', 3), ('episode', 3), ('group', 2), ('division', 2), ('band', 2), ('race', 2), ('clubs', 2), ('sea', 2), ('label', 2), ('shows', 2), ('cars', 2), ('awards', 2), ('bible', 2), ('god', 2), ('contribution', 2), ('sort', 2), ('town', 2), ('planet', 2), ('illnesses', 2), ('illness', 2), ('states', 2), ('things', 2), ('battles', 2), ('role', 2), ('province', 2), ('games', 2), ('organization', 2), ('types', 2), ('land', 2), ('age', 2), ('code', 1), ('queen', 1), ('colleges', 1), ('experiments', 1), ('battle', 1), ('family', 1), ('tv', 1), ('technique', 1), ('theme', 1), ('writers', 1), ('branch', 1), ('song', 1), ('food', 1), ('landforms', 1), ('nation', 1), ('schools', 1), ('condition', 1), ('magic', 1), ('river', 1), ('round', 1), ('bowls', 1), ('zones', 1), ('culture', 1), ('empire', 1), ('techniques', 1), ('degree', 1), ('oprah', 1), ('particles', 1), ('columbus', 1), ('bass', 1), ('business', 1), ('planes', 1), ('percent', 1), ('experience', 1), ('times', 1), ('products', 1), ('jersey', 1), ('features', 1), ('cancer', 1), ('each', 1), ('colony', 1), ('period', 1), ('market', 1), ('gunfight', 1), ('district', 1), ('station', 1), ('stadium', 1), ('medium', 1), ('continents', 1), ('hotel', 1), ('dialects', 1), ('atom', 1), ('albums', 1), ('obama', 1), ('challenges', 1), ('places', 1), ('car', 1), ('invention', 1), ('attractions', 1), ('voice', 1), ('discoveries', 1), ('penny', 1), ('discovery', 1), ('drug', 1), ('ship', 1), ('education', 1), ('movements', 1), ('drugs', 1), ('brand', 1), ('magazine', 1), ('website', 1), ('equipment', 1), ('animal', 1), ('beach', 1), ('dialect', 1), ('ball', 1), ('models', 1), ('island', 1), ('hardships', 1), ('place', 1), ('organism', 1), ('strings', 1)]
what --dobj-->
[('do', 62), ('speak', 25), ('called', 11), ('spoken', 8), ('play', 8), ('believe', 8), ('say', 8), ('discover', 6), ('sing', 6), ('used', 5), ('mean', 5), ('represent', 4), ('own', 4), ('contribute', 4), ('accomplish', 4), ('write', 4), ('like', 4), ('invent', 3), ('make', 3), ('study', 3), ('invented', 3), ('doing', 2), ('discovered', 2), ('see', 2), ('have', 2), ('border', 2), ('visit', 2), ('live', 2), ('star', 2), ('die', 2), ('export', 2), ('control', 1), ('played', 1), ('carver', 1), ('done', 1), ('create', 1), ('sell', 1), ('develop', 1), ('hate', 1), ('writer', 1), ('written', 1), ('been', 1), ('eat', 1), ('does', 1), ('rookie', 1), ('announce', 1), ('hub', 1), ('propose', 1), ('act', 1), ('direct', 1), ('famous', 1), ('explore', 1), ('produce', 1), ('import', 1), ('known', 1), ('grow', 1), ('fly', 1), ('compose', 1), ('owns', 1), ('practiced', 1), ('name', 1), ('created', 1), ('did', 1), ('od', 1), ('rule', 1), ('champlain', 1), ('considered', 1), ('bad', 1), ('compete', 1), ('stand', 1), ('involved', 1), ('identify', 1), ('wrote', 1)]
what --nsubj-->
[('name', 48), ('happened', 25), ('currency', 20), ('code', 18), ('language', 18), ('names', 14), ('city', 12), ('mascot', 12), ('system', 11), ('countries', 10), ('languages', 9), ('do', 9), ('zone', 8), ('influenced', 8), ('religion', 7), ('inspired', 7), ('religions', 6), ('capital', 6), ('places', 6), ('things', 5), ('book', 5), ('cause', 4), ('made', 4), ('flower', 4), ('time', 4), ('exports', 3), ('government', 3), ('job', 3), ('nationality', 3), ('cities', 3), ('sports', 3), ('inventions', 3), ('beliefs', 3), ('imports', 3), ('gods', 2), ('sights', 2), ('colors', 2), ('postcode', 2), ('style', 2), ('caused', 2), ('killed', 2), ('capitals', 2), ('neighborhood', 2), ('some', 2), ('see', 2), ('sport', 2), ('movie', 2), ('state', 2), ('bird', 2), ('dialects', 2), ('kids', 2), ('nations', 2), ('movies', 2), ('is', 2), ('timezone', 2), ('education', 2), ('party', 2), ('holidays', 2), ('wrong', 2), ('purpose', 2), ('president', 2), ('songs', 2), ('jabbar', 1), ('all', 1), ('senna', 1), ('traditions', 1), ('jurisdiction', 1), ('symbols', 1), ('helens', 1), ('gauge', 1), ('battle', 1), ('contributions', 1), ('causes', 1), ('occupation', 1), ('northeast', 1), ('title', 1), ('terriers', 1), ('character', 1), ('going', 1), ('titans', 1), ('resources', 1), ('giants', 1), ('queen', 1), ('wagner', 1), ('landforms', 1), ('represents', 1), ('airports', 1), ('james', 1), ('leader', 1), ('shows', 1), ('people', 1), ('miller', 1), ('church', 1), ('rivers', 1), ('legacy', 1), ('anthem', 1), ('happen', 1), ('edison', 1), ('est', 1), ('cooper', 1), ('lead', 1), ('theories', 1), ('does', 1), ('discovery', 1), ('achievements', 1), ('houses', 1), ('bannatyne', 1), ('motto', 1), ('isthmus', 1), ('island', 1), ('frankel', 1), ('airport', 1), ('medicare', 1), ('islands', 1), ('makes', 1), ('holydays', 1), ('origin', 1), ('attractions', 1), ('reagan', 1), ('one', 1), ('accomplishments', 1), ('spanish', 1), ('mark', 1), ('hotels', 1), ('palace', 1), ('union', 1), ('there', 1), ('flag', 1), ('circumstances', 1), ('saint', 1), ('heritage', 1), ('today', 1), ('thing', 1), ('form', 1), ('pie', 1), ('hemisphere', 1), ('part', 1), ('jordan', 1), ('goals', 1), ('albums', 1), ('kennedys', 1), ('present', 1), ('king', 1), ('player', 1), ('work', 1), ('mendler', 1), ('country', 1), ('newspaper', 1), ('powers', 1), ('conferences', 1), ('al', 1), ('disease', 1), ('jitsu', 1), ('spade', 1), ('italy', 1), ('laden', 1), ('song', 1), ('range', 1), ('speech', 1), ('animal', 1), ('events', 1), ('economy', 1), ('literature', 1), ('columbia', 1), ('roster', 1), ('hometown', 1), ('structure', 1), ('a', 1), ('happening', 1), ('dog', 1), ('person', 1), ('spaniels', 1)]
what --nsubjpass-->
[('discovered', 1), ('considered', 1), ('fun', 1)]
what --pobj-->
[('famous', 19), ('responsible', 1)]
what --prep_in-->
[('is', 1), ('montana', 1), ('argentina', 1)]
when --advmod-->
[('win', 23), ('was', 18), ('start', 13), ('is', 11), ('died', 6), ('elected', 6), ('president', 5), ('are', 3), ('occur', 3), ('run', 3), ('come', 3), ('born', 2), ('die', 2), ('appear', 2), ('won', 2), ('take', 2), ('drafted', 2), ('started', 2), ('change', 2), ('made', 2), ('join', 2), ('leave', 2), ('shot', 1), ('played', 1), ('predict', 1), ('allowed', 1), ('discovered', 1), ('invaded', 1), ('held', 1), ('have', 1), ('go', 1), ('close', 1), ('open', 1), ('established', 1), ('erupt', 1), ('end', 1), ('famous', 1), ('inaugurated', 1), ('live', 1), ('created', 1), ('written', 1), ('factor', 1), ('got', 1), ('entered', 1), ('assassinated', 1), ('final', 1), ('happened', 1), ('play', 1), ('hit', 1), ('get', 1), ('burn', 1), ('stop', 1), ('free', 1), ('released', 1), ('designed', 1), ('killed', 1), ('opened', 1), ('founded', 1), ('sworn', 1), ('originate', 1), ('joined', 1), ('admit', 1), ('race', 1), ('compete', 1), ('does', 1), ('defaults', 1), ('team', 1), ('makes', 1), ('came', 1), ('traveling', 1)]
when --dep-->
[('and', 1)]
where --advmod-->
[('is', 123), ('live', 93), ('go', 70), ('located', 69), ('born', 35), ('play', 28), ('come', 26), ('die', 18), ('was', 17), ('are', 13), ('made', 12), ('grow', 12), ('get', 9), ('originate', 9), ('start', 8), ('buried', 8), ('stay', 7), ('take', 6), ('died', 6), ('held', 5), ('went', 5), ('raised', 5), ('fly', 5), ('grew', 5), ('attend', 5), ('study', 4), ('end', 4), ('begin', 4), ('married', 3), ('work', 3), ('happen', 3), ('visit', 3), ('travel', 3), ('based', 2), ('practice', 2), ('watch', 2), ('found', 2), ('got', 2), ('run', 2), ('manufactured', 2), ('teach', 2), ('spoken', 2), ('speak', 2), ('fight', 2), ('surrender', 2), ('used', 2), ("'s", 2), ('flow', 2), ('coach', 1), ('move', 1), ('founded', 1), ('colonise', 1), ('irene', 1), ('filmed', 1), ('indian', 1), ('ski', 1), ('happened', 1), ('lived', 1), ('do', 1), ('hit', 1), ('import', 1), ('fled', 1), ('leave', 1), ('wrote', 1), ('fired', 1), ('does', 1), ('invade', 1), ('be', 1), ('exchange', 1), ('originated', 1), ('explore', 1), ('herbstreit', 1), ('put', 1), ('traded', 1), ('reside', 1), ('marlins', 1), ('spending', 1), ('vacation', 1), ('going', 1), ('educated', 1), ('started', 1), ('train', 1), ('lives', 1), ('eat', 1), ('he', 1), ('elected', 1), ('derived', 1), ('played', 1), ('centered', 1), ('buy', 1), ('occur', 1), ('built', 1), ('perform', 1), ('graduate', 1), ('map', 1), ('hang', 1), ('kurdish', 1), ('land', 1), ('register', 1), ('talk', 1), ('english', 1), ('operate', 1), ('came', 1)]
where --dep-->
[('when', 2), ('revolution', 1)]
which --dep-->
[('is', 1)]
which --det-->
[('countries', 14), ('country', 13), ('airport', 6), ('states', 5), ('province', 4), ('continent', 3), ('team', 3), ('college', 2), ('city', 2), ('party', 2), ('stores', 1), ('books', 1), ('austen', 1), ('islamabad', 1), ('god', 1), ('state', 1), ('island', 1), ('part', 1), ('legend', 1), ('kardashians', 1), ('wife', 1), ('university', 1), ('kennedy', 1), ('ocean', 1), ('river', 1)]
who --advmod-->
[('now', 2)]
who --cop-->
[('is', 129), ('was', 79), ('are', 27), ('were', 7), ("'s", 3)]
who --dep-->
[('play', 23), ('married', 17), ('played', 6), ('have', 4), ('sign', 2), ('influenced', 2), ('sail', 2), ('after', 2), ('voice', 2), ('coach', 1), ('give', 1), ('cheat', 1), ('retire', 1), ('engaged', 1), ('klum', 1), ('discovered', 1), ('vote', 1), ('export', 1), ('go', 1), ('cheated', 1), ('from', 1), ('travel', 1), ('ponce', 1), ('fight', 1), ('import', 1), ('racing', 1), ('revolted', 1), ('descend', 1), ('get', 1), ('gain', 1), ('trade', 1), ('during', 1), ('surrender', 1), ('with', 1), ('belong', 1), ('made', 1), ('look', 1), ('considered', 1), ('inspired', 1), ('drive', 1), ('signed', 1), ('segel', 1), ('shakur', 1), ('race', 1), ('starting', 1), ('wrote', 1)]
who --det-->
[('voice', 1)]
who --dobj-->
[('play', 43), ('married', 19), ('marry', 13), ('dating', 6), ('playing', 3), ('draft', 2), ('drive', 2), ('traded', 2), ('think', 2), ('represent', 1), ('love', 1), ('engaged', 1), ('sign', 1), ('hair', 1), ('worship', 1), ('believe', 1), ('end', 1), ('driving', 1), ('marrying', 1), ('fight', 1), ('beat', 1), ('trade', 1), ('died', 1), ('boyfriend', 1), ('called', 1), ('work', 1), ('signed', 1), ('dated', 1), ('liars', 1), ('inspired', 1)]
who --nsubj-->
[('plays', 68), ('played', 64), ('president', 23), ('won', 21), ('coach', 14), ('leader', 13), ('governor', 13), ('owns', 13), ('parents', 9), ('wife', 9), ('senators', 8), ('started', 8), ('minister', 8), ('voice', 8), ('created', 7), ('senator', 6), ('founded', 5), ('killed', 5), ('does', 5), ('invented', 5), ('queen', 4), ('wrote', 4), ('husband', 4), ('founder', 3), ('father', 3), ('did', 3), ('elizabeth', 3), ('voices', 3), ('girlfriend', 3), ('influenced', 3), ('actor', 3), ('speaks', 3), ('play', 3), ('coached', 3), ('mother', 3), ('shot', 2), ('nominated', 2), ('partners', 2), ('judges', 2), ('winner', 2), ('runs', 2), ('sang', 2), ('players', 2), ('owner', 2), ('dictator', 2), ('king', 2), ('made', 2), ('helped', 2), ('voiced', 2), ('brothers', 2), ('husbands', 2), ('sisters', 2), ('bilbo', 1), ('vader', 1), ('manager', 1), ('daniel', 1), ('children', 1), ('cody', 1), ('justice', 1), ('minnesota', 1), ('controls', 1), ('tunisia', 1), ('pays', 1), ('siblings', 1), ('bernstein', 1), ('2012', 1), ('dad', 1), ('bio', 1), ('seized', 1), ('succeeded', 1), ('mom', 1), ('maslow', 1), ('son', 1), ('bryant', 1), ('name', 1), ('james', 1), ('married', 1), ('brother', 1), ('ups', 1), ('blackwell', 1), ('revolution', 1), ('dean', 1), ('crowe', 1), ('kane', 1), ('taught', 1), ('sponsor', 1), ('randolph', 1), ('accomplice', 1), ('riel', 1), ('garcia', 1), ('fox', 1), ('portrayed', 1), ('xvi', 1), ('baggins', 1), ('shakespeare', 1), ('leia', 1), ('drafted', 1), ('korea', 1), ('iran', 1), ('lee', 1), ('underwood', 1), ('jr', 1), ('ackles', 1), ('china', 1), ('answers', 1), ('members', 1), ('teacher', 1), ('galilei', 1), ('publisher', 1), ('sister', 1), ('sumpter', 1), ('daughter', 1), ('hicks', 1), ('inspired', 1), ('carries', 1), ('descended', 1), ('dated', 1), ('davis', 1), ('hussein', 1), ('emperor', 1), ('makes', 1), ('kicker', 1), ('raised', 1), ('diocletian', 1), ('win', 1), ('number', 1), ('lois', 1), ('sandler', 1), ('right', 1), ('master', 1), ('quinn', 1), ('has', 1), ('groban', 1), ('people', 1), ('cyrus', 1), ('beneke', 1), ('stars', 1), ('eagles', 1), ('white', 1), ('friend', 1), ('hewitt', 1), ('gibb', 1), ('packers', 1), ('obama', 1), ('wore', 1), ('seuss', 1), ('virchow', 1), ('inventor', 1), ('ran', 1), ('is', 1), ('general', 1), ('jackson', 1), ('brockovich', 1), ('kossuth', 1), ('grant', 1), ('juan', 1), ('illinois', 1), ('member', 1), ('winners', 1), ('speaker', 1), ('tia', 1), ('quarterback', 1), ('rimes', 1), ('snl', 1), ('hawkeye', 1), ('uses', 1), ('thoreau', 1), ('grimshaw', 1), ('grandson', 1), ('doc', 1), ('dow', 1), ('oracle', 1), ('steffens', 1), ('athletes', 1), ('playing', 1)]
who --nsubjpass-->
[('married', 5), ('elected', 1), ('engaged', 1), ('killed', 1)]
who --prep_of-->
[('griffin', 1), ('skellington', 1)]
who --rcmod-->
[('doctor', 1)]
why --advmod-->
[('important', 2), ('famous', 1)]
"""
