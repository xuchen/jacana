File and directory descriptions

data	-	directory with the training, dev, and test data as in the Jeopardy paper, in pseudo-xml format. The "less-than-40" files contain only pairs of length <= 40 tokens, as in the Jeopardy paper.
eval	-	files encoding human judgments used for evaluations
dev	-	original development data (before filtering instances longer than 40 and removing questions with all pos. or neg. answers)
testing		-	original testing data 
training	-	original training data 

Notes on running evaluations for answer selection:
To evaluate a ranking of question answer pairs from your model, 
you must produce a ranking file to be read by trec_eval and compared with the judgments in the "eval" directory.
Then, you should run, e.g., "trec_eval -q -c judgments ranking"

The format of the trec_eval ranking file you should generate should be as follows.
Each row corresponds to a QA pair.
Data in each row is space-separated. 
The first column is the query (question) number.  This starts with 1.
Any questions for which there are either no positive or no negative answers are skipped (e.g., there is no #2 in the Test-T40.judgment file).
The second column can always be Q0.
The third column is the retrieved document (answer) number for the given question.  This starts at 0.
The ordering of the 3rd column corresponds to the ordering in data/{dev,test}-less-than-40.xml.
The fourth column can always be 0.
The fifth column is the score of the given document (answer).  Higher scores are better (i.e., a true answer should be higher).
The sixth (last) column can be anything.


