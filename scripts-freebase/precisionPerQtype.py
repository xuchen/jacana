#! /usr/bin/env python

qword2q = {}
qtype2q = {}
qrel2q = {}
qword_qtype2q = {}
qlen2q = {}

def add_to_set(qword, q, qword2q):
    if qword not in qword2q:
        qword2q[qword] = set()
    qword2q[qword].add(q)

with open('../freebase-data/webquestions/qword.test.txt') as f:
    for line in f:
        if line.find("-->") != -1:
            # what does jamaican people speak?    what    --dobj-->   speak
            q, qword, qrel, qtype = line.strip().split("\t")
            qword_qtype = qword + " " + qtype
            add_to_set(qword, q, qword2q)
            add_to_set(qtype, q, qtype2q)
            add_to_set(qrel, q, qrel2q)
            add_to_set(qword_qtype, q, qword_qtype2q)
            l = len(q.split())
            add_to_set(l, q, qlen2q)
            if l <= 5:
                add_to_set('<=5', q, qlen2q)
            elif l <= 10:
                add_to_set('<=10', q, qlen2q)
            elif l <= 15:
                add_to_set('<=15', q, qlen2q)
            elif l <= 20:
                add_to_set('<=20', q, qlen2q)
            else:
                add_to_set('>20', q, qlen2q)

q2precision = {}
#with open('../results/freebase/sempre.35.7.f1') as f:
with open('../results/freebase/webquestions.test.forced_answers.scores') as f:
    for line in f:
        q, correct, _ = line.split('\t')
        correct = float(correct)
        q2precision[q] = correct


def print_by_precision(qword2q):
    prec_qword = []
    for qword,qs in qword2q.items():
        precisions = [q2precision[q] for q in qs]
        prec = sum(precisions)/len(precisions)
        prec_qword.append((prec, qword))
    prec_qword.sort(reverse=True)
    for prec, qword in prec_qword:
        print "%.3f\t%s\t%d" % (prec, qword, len(qword2q[qword]))

print "qword2q:"
print_by_precision(qword2q)
print "\n"*2
print "qtype2q:"
print_by_precision(qtype2q)
print "\n"*2
print "qrel2q:"
print_by_precision(qrel2q)
print "\n"*2
print "qword_qtype2q:"
print_by_precision(qword_qtype2q)
print "\n"*2
print_by_precision(qlen2q)
print "\n"*2


output = '''
qword2q:
0.376	what	929
0.360	how many	5
0.274	who	261
0.259	where	357
0.203	which	35
0.097	when	100
0.000	how large	1
0.000	how i	1
0.000	how deep	1



qtype2q:
1.000	vote	1
1.000	voices	1
1.000	types	1
1.000	trade	1
1.000	superbowl	1
1.000	sign	1
1.000	running	1
1.000	religions	1
1.000	practiced	1
1.000	own	1
1.000	music	1
1.000	label	1
1.000	inventions	2
1.000	graduate	1
1.000	formed	1
1.000	drink	1
1.000	degree	1
1.000	conrad	1
1.000	club	1
1.000	bible	1
0.875	religion	4
0.833	graduated	2
0.812	system	4
0.769	english	1
0.754	currency	21
0.750	sports	2
0.720	died	12
0.700	sport	1
0.700	obama	1
0.688	teams	2
0.671	airport	7
0.667	nationality	3
0.667	engaged	3
0.645	visit	4
0.644	language	57
0.641	films	2
0.638	been	2
0.629	disease	5
0.625	province	2
0.625	invent	4
0.625	continent	4
0.614	die	22
0.600	vii	1
0.600	dating	2
0.578	instruments	3
0.572	movies	22
0.572	party	9
0.571	colleges	3
0.571	distributed	1
0.562	university	4
0.545	books	5
0.542	highschool	3
0.517	county	17
0.500	worship	1
0.500	use	2
0.500	style	5
0.500	schools	2
0.500	practice	1
0.500	playing	2
0.500	occur	4
0.500	movement	2
0.500	mohammed	1
0.500	kill	1
0.500	jobs	2
0.500	group	1
0.500	filmed	1
0.500	educated	1
0.500	drafted	2
0.500	declared	1
0.500	date	1
0.500	company	1
0.500	cancer	2
0.500	brad	1
0.500	art	1
0.500	all	2
0.497	songs	3
0.495	see	9
0.490	written	2
0.484	spoken	13
0.480	form	9
0.474	wrote	7
0.473	inspired	5
0.472	money	6
0.456	languages	3
0.451	kind	45
0.446	influenced	7
0.444	got	3
0.444	attend	3
0.427	plays	43
0.418	do	62
0.403	voiced	4
0.400	lived	1
0.400	fly	1
0.392	state	10
0.389	win	6
0.389	import	2
0.385	timezone	8
0.380	type	50
0.374	located	26
0.372	school	16
0.360	many	5
0.352	people	3
0.350	fight	2
0.350	buried	5
0.346	college	13
0.342	speak	17
0.337	position	5
0.333	time	7
0.333	rule	1
0.333	materials	1
0.333	makes	3
0.333	instrument	2
0.333	guitar	3
0.333	called	9
0.333	arrested	1
0.325	government	5
0.317	countries	41
0.315	killed	9
0.312	does	4
0.310	played	35
0.300	document	2
0.291	play	55
0.289	is	83
0.289	born	14
0.278	city	13
0.272	go	45
0.271	happened	7
0.269	team	35
0.267	owns	5
0.267	founded	5
0.267	battles	1
0.262	used	8
0.255	live	25
0.251	country	32
0.250	works	1
0.250	went	1
0.250	war	2
0.250	stiller	1
0.250	sort	1
0.250	run	4
0.250	export	2
0.244	draft	4
0.238	border	5
0.237	zone	9
0.233	are	5
0.228	grow	6
0.224	won	16
0.223	married	21
0.221	study	6
0.211	states	5
0.200	call	5
0.196	was	29
0.189	have	5
0.183	famous	13
0.178	marry	13
0.174	known	11
0.173	come	22
0.167	raised	2
0.167	made	6
0.167	continents	2
0.156	acted	2
0.144	based	4
0.143	flow	1
0.140	year	25
0.125	ocean	2
0.125	manufactured	2
0.125	grew	2
0.125	accomplish	2
0.111	region	3
0.104	originate	8
0.100	found	2
0.083	work	4
0.083	did	4
0.083	's	2
0.071	end	7
0.067	stay	3
0.067	discover	5
0.067	built	3
0.051	start	15
0.050	sing	2
0.042	book	6
0.028	invented	4
0.009	get	12
0.000	years	2
0.000	write	1
0.000	wars	1
0.000	voice	3
0.000	victory	1
0.000	vegetables	1
0.000	unified	1
0.000	tupac	1
0.000	train	1
0.000	town	2
0.000	think	1
0.000	things	1
0.000	take	5
0.000	surrender	1
0.000	stop	1
0.000	stones	1
0.000	station	1
0.000	started	2
0.000	stands	1
0.000	stand	2
0.000	stabbed	1
0.000	sri	1
0.000	spain	1
0.000	situated	1
0.000	sit	1
0.000	signed	1
0.000	shows	2
0.000	shot	1
0.000	ship	1
0.000	series	1
0.000	season	1
0.000	say	1
0.000	rules	1
0.000	ruler	1
0.000	ruled	1
0.000	role	3
0.000	return	1
0.000	retired	1
0.000	resigns	1
0.000	represent	2
0.000	ran	2
0.000	rainforest	1
0.000	race	1
0.000	prompted	1
0.000	programmed	1
0.000	products	2
0.000	produce	1
0.000	president	4
0.000	predict	1
0.000	pray	1
0.000	player	1
0.000	planet	1
0.000	places	3
0.000	pieces	1
0.000	period	1
0.000	percentage	1
0.000	percent	2
0.000	peat	1
0.000	part	5
0.000	originated	1
0.000	organizations	1
0.000	organisms	1
0.000	opening	1
0.000	open	1
0.000	offices	1
0.000	objects	1
0.000	named	1
0.000	name	2
0.000	moving	1
0.000	montesquie	1
0.000	met	1
0.000	mean	4
0.000	make	2
0.000	lohan	1
0.000	like	1
0.000	led	5
0.000	learn	1
0.000	league	1
0.000	large	1
0.000	lands	1
0.000	landed	1
0.000	join	2
0.000	johansson	1
0.000	jackson	1
0.000	islands	1
0.000	island	1
0.000	invested	1
0.000	invest	1
0.000	industry	1
0.000	indicted	1
0.000	include	1
0.000	honor	1
0.000	honduras	1
0.000	hit	1
0.000	hemisphere	1
0.000	helped	1
0.000	held	1
0.000	happen	4
0.000	governor	1
0.000	good	1
0.000	given	1
0.000	genre	1
0.000	games	1
0.000	fought	4
0.000	follow	1
0.000	flower	1
0.000	find	3
0.000	financed	1
0.000	fighting	1
0.000	explore	1
0.000	explain	1
0.000	executed	2
0.000	excuse	1
0.000	exchange	1
0.000	evolved	1
0.000	events	1
0.000	ends	1
0.000	electorate	1
0.000	elected	1
0.000	eat	1
0.000	drug	1
0.000	doing	1
0.000	district	1
0.000	discovery	1
0.000	discovered	1
0.000	developed	1
0.000	dessen	1
0.000	designed	1
0.000	design	1
0.000	degrees	3
0.000	deferred	1
0.000	defeated	1
0.000	deep	1
0.000	dated	1
0.000	created	2
0.000	counties	1
0.000	contribute	1
0.000	conduct	1
0.000	color	1
0.000	code	1
0.000	coaches	1
0.000	coached	1
0.000	coach	2
0.000	classified	1
0.000	characters	1
0.000	character	1
0.000	channel	2
0.000	change	1
0.000	century	1
0.000	celebrities	1
0.000	celebrated	1
0.000	caused	2
0.000	capital	1
0.000	camp	1
0.000	cain	1
0.000	business	1
0.000	brought	1
0.000	broncos	1
0.000	bowl	1
0.000	body	1
0.000	bisect	1
0.000	believe	3
0.000	begin	1
0.000	began	2
0.000	banjo	1
0.000	band	1
0.000	awards	2
0.000	attack	1
0.000	athlete	1
0.000	assassinated	1
0.000	area	2
0.000	approve	1
0.000	appointed	1
0.000	animal	1
0.000	age	1
0.000	address	1
0.000	activities	1
0.000	accomplished	1
0.000	about	2



qrel2q:
0.404	--dep-->	157
0.387	--det-->	659
0.300	--nsubjpass-->	2
0.275	--dobj-->	212
0.273	--nsubj-->	183
0.224	--advmod-->	465
0.075	--pobj-->	12
0.000	--prep_in-->	1



qword_qtype2q:
1.000	who trade	1
1.000	who sign	1
1.000	who running	1
1.000	who makes	1
1.000	who formed	1
1.000	who drafted	1
1.000	which university	1
1.000	where vote	1
1.000	where playing	1
1.000	where graduate	1
1.000	when occur	1
1.000	what voices	1
1.000	what types	1
1.000	what superbowl	1
1.000	what religions	1
1.000	what practiced	1
1.000	what own	1
1.000	what music	1
1.000	what label	1
1.000	what inventions	2
1.000	what founded	1
1.000	what drink	1
1.000	what die	7
1.000	what degree	1
1.000	what conrad	1
1.000	what club	1
1.000	what bible	1
0.955	what played	2
0.875	what religion	4
0.833	what died	9
0.833	where graduated	2
0.818	what have	1
0.812	what system	4
0.769	where english	1
0.754	what currency	21
0.750	what sports	2
0.733	which airport	3
0.700	where obama	1
0.700	what sport	1
0.688	what teams	2
0.673	what visit	3
0.667	who import	1
0.667	who engaged	3
0.667	what nationality	3
0.667	what continent	3
0.644	what language	57
0.641	what films	2
0.641	what spoken	9
0.638	what been	2
0.629	what disease	5
0.625	what province	2
0.625	what invent	4
0.625	what airport	4
0.606	what wrote	3
0.600	who vii	1
0.600	who dating	2
0.583	what influenced	4
0.581	what party	8
0.578	what instruments	3
0.572	what movies	22
0.571	what colleges	3
0.571	where distributed	1
0.562	where visit	1
0.556	which states	1
0.556	what is	9
0.545	what books	5
0.542	what highschool	3
0.517	what county	17
0.500	who wrote	3
0.500	who worship	1
0.500	who inspired	1
0.500	who fight	1
0.500	who brad	1
0.500	which party	1
0.500	which continent	1
0.500	where run	2
0.500	where practice	1
0.500	where mohammed	1
0.500	where married	1
0.500	where kill	1
0.500	where filmed	1
0.500	where export	1
0.500	where end	1
0.500	where educated	1
0.500	when got	2
0.500	when died	2
0.500	when declared	1
0.500	what use	2
0.500	what style	5
0.500	what schools	2
0.500	what movement	2
0.500	what killed	2
0.500	what jobs	2
0.500	what group	1
0.500	what date	1
0.500	what company	1
0.500	what cancer	2
0.500	what art	1
0.500	what all	2
0.497	what songs	3
0.495	what see	9
0.490	what written	2
0.490	what state	8
0.484	what speak	12
0.480	what form	9
0.472	what money	6
0.467	when win	5
0.466	what inspired	4
0.456	what languages	3
0.451	what kind	45
0.444	where attend	3
0.433	where die	15
0.427	who plays	43
0.420	where was	9
0.418	what do	62
0.417	who does	3
0.417	what university	3
0.411	who is	4
0.403	who voiced	4
0.402	what play	4
0.400	where lived	1
0.400	where fly	1
0.385	what timezone	8
0.380	what type	50
0.374	where located	26
0.372	what school	16
0.367	where born	11
0.360	how many many	5
0.352	what people	3
0.350	where buried	5
0.346	what college	13
0.337	what position	5
0.333	where occur	3
0.333	where killed	4
0.333	where got	1
0.333	where arrested	1
0.333	what time	7
0.333	what rule	1
0.333	what materials	1
0.333	what made	3
0.333	what instrument	2
0.333	what guitar	3
0.333	what continents	1
0.333	what called	9
0.328	what countries	34
0.325	what government	5
0.324	what country	24
0.301	what city	12
0.300	what document	2
0.298	where go	41
0.296	what used	7
0.283	where play	10
0.283	who play	41
0.280	who won	11
0.271	what happened	7
0.271	who played	33
0.269	what team	35
0.267	who owns	5
0.267	what battles	1
0.265	where is	61
0.263	who influenced	3
0.262	which countries	7
0.255	where live	25
0.250	who stiller	1
0.250	which ocean	1
0.250	where went	1
0.250	what works	1
0.250	what war	2
0.250	what study	4
0.250	what sort	1
0.244	who draft	4
0.238	what border	5
0.237	what zone	9
0.233	where are	5
0.228	where grow	6
0.209	who married	20
0.200	what fight	1
0.200	what call	5
0.183	what famous	13
0.181	where come	21
0.178	who marry	13
0.174	what known	11
0.167	who killed	3
0.167	where work	2
0.167	where raised	2
0.167	when won	3
0.163	where study	2
0.156	what acted	2
0.146	when is	8
0.144	where based	4
0.143	where flow	1
0.143	where died	1
0.140	what year	25
0.132	where spoken	4
0.132	where start	5
0.125	where manufactured	2
0.125	where grew	2
0.125	what states	4
0.125	what accomplish	2
0.119	where originate	7
0.111	who did	3
0.111	where import	1
0.111	what region	3
0.111	what invented	1
0.100	where found	2
0.100	what sing	1
0.100	when was	19
0.083	who founded	4
0.083	when 's	2
0.067	where stay	3
0.067	where built	3
0.067	what discover	5
0.062	what book	4
0.031	who have	4
0.031	which country	8
0.023	where used	1
0.011	where get	10
0.011	when start	10
0.000	who write	1
0.000	who was	1
0.000	who voice	3
0.000	who surrender	1
0.000	who started	1
0.000	who speak	1
0.000	who sing	1
0.000	who signed	1
0.000	who run	2
0.000	who rules	1
0.000	who ruled	1
0.000	who ran	2
0.000	who pray	1
0.000	who playing	1
0.000	who made	1
0.000	who led	2
0.000	who johansson	1
0.000	who invented	1
0.000	who helped	1
0.000	who fought	3
0.000	who follow	1
0.000	who financed	1
0.000	who fighting	1
0.000	who explore	1
0.000	who executed	1
0.000	who developed	1
0.000	who designed	1
0.000	who defeated	1
0.000	who dated	1
0.000	who created	1
0.000	who coaches	1
0.000	who coached	1
0.000	who coach	2
0.000	who appointed	1
0.000	which state	2
0.000	which ruler	1
0.000	which made	1
0.000	which island	1
0.000	which is	1
0.000	which export	1
0.000	which continents	1
0.000	which city	1
0.000	which book	2
0.000	which approve	1
0.000	where wrote	1
0.000	where tupac	1
0.000	where take	4
0.000	where started	1
0.000	where speak	4
0.000	where situated	1
0.000	where originated	1
0.000	where moving	1
0.000	where made	1
0.000	where jackson	1
0.000	where honduras	1
0.000	where happen	3
0.000	where fought	1
0.000	where find	2
0.000	where executed	1
0.000	where excuse	1
0.000	where exchange	1
0.000	where evolved	1
0.000	where ends	1
0.000	where eat	1
0.000	where conduct	1
0.000	where cain	1
0.000	where begin	1
0.000	where began	2
0.000	where attack	1
0.000	where address	1
0.000	when unified	1
0.000	when take	1
0.000	when stop	1
0.000	when stabbed	1
0.000	when sit	1
0.000	when shot	1
0.000	when return	1
0.000	when retired	1
0.000	when resigns	1
0.000	when president	4
0.000	when peat	1
0.000	when originate	1
0.000	when opening	1
0.000	when open	1
0.000	when landed	1
0.000	when join	2
0.000	when invented	2
0.000	when hit	1
0.000	when held	1
0.000	when happen	1
0.000	when governor	1
0.000	when go	3
0.000	when given	1
0.000	when get	1
0.000	when end	6
0.000	when elected	1
0.000	when drafted	1
0.000	when created	1
0.000	when come	1
0.000	when change	1
0.000	when celebrated	1
0.000	when born	3
0.000	when assassinated	1
0.000	what years	2
0.000	what work	2
0.000	what won	2
0.000	what win	1
0.000	what wars	1
0.000	what victory	1
0.000	what vegetables	1
0.000	what train	1
0.000	what town	2
0.000	what think	1
0.000	what things	1
0.000	what stones	1
0.000	what station	1
0.000	what stands	1
0.000	what stand	2
0.000	what sri	1
0.000	what spain	1
0.000	what shows	2
0.000	what ship	1
0.000	what series	1
0.000	what season	1
0.000	what say	1
0.000	what role	3
0.000	what represent	2
0.000	what rainforest	1
0.000	what race	1
0.000	what prompted	1
0.000	what programmed	1
0.000	what products	2
0.000	what produce	1
0.000	what predict	1
0.000	what player	1
0.000	what planet	1
0.000	what places	3
0.000	what pieces	1
0.000	what period	1
0.000	what percentage	1
0.000	what percent	2
0.000	what part	5
0.000	what organizations	1
0.000	what organisms	1
0.000	what offices	1
0.000	what ocean	1
0.000	what objects	1
0.000	what named	1
0.000	what name	2
0.000	what montesquie	1
0.000	what mean	4
0.000	what makes	2
0.000	what make	2
0.000	what lohan	1
0.000	what like	1
0.000	what led	3
0.000	what learn	1
0.000	what league	1
0.000	what lands	1
0.000	what islands	1
0.000	what invested	1
0.000	what invest	1
0.000	what industry	1
0.000	what indicted	1
0.000	what include	1
0.000	what honor	1
0.000	what hemisphere	1
0.000	what good	1
0.000	what go	1
0.000	what get	1
0.000	what genre	1
0.000	what games	1
0.000	what flower	1
0.000	what find	1
0.000	what explain	1
0.000	what events	1
0.000	what electorate	1
0.000	what drug	1
0.000	what doing	1
0.000	what does	1
0.000	what district	1
0.000	what discovery	1
0.000	what discovered	1
0.000	what did	1
0.000	what dessen	1
0.000	what design	1
0.000	what degrees	3
0.000	what deferred	1
0.000	what counties	1
0.000	what contribute	1
0.000	what color	1
0.000	what code	1
0.000	what classified	1
0.000	what characters	1
0.000	what character	1
0.000	what channel	2
0.000	what century	1
0.000	what celebrities	1
0.000	what caused	2
0.000	what capital	1
0.000	what camp	1
0.000	what business	1
0.000	what brought	1
0.000	what broncos	1
0.000	what bowl	1
0.000	what body	1
0.000	what bisect	1
0.000	what believe	3
0.000	what banjo	1
0.000	what band	1
0.000	what awards	2
0.000	what athlete	1
0.000	what area	2
0.000	what animal	1
0.000	what age	1
0.000	what activities	1
0.000	what accomplished	1
0.000	what about	2
0.000	how large large	1
0.000	how i met	1
0.000	how deep deep	1


'''
