#! /bin/bash

# Answer Extraction experiment
# This is the script to train a factoid QA model and test it.
# To run it, you need to have CRFSuite (http://www.chokkan.org/software/crfsuite/)
# installed, and modify the following two commands properly:

# if Eclipse compiles everything to the "bin" folder
JAVA_CMD="java -Xmx10g -classpath bin:lib/*"
# if using the pre-compiled all-in-one .jar file
#JAVA_CMD="java -Xmx2g -classpath jacana-qa.jar"

# path to CRFSuite, on Debain-based Linux and Mac,
# you should be able to install it with:
# sudo apt-get install crfsuite
# or:
# sudo port install crfsuite
CRF=crfsuite


# set to dev or test

EXP_DIR="/Users/xuchen/data2/xuchen/j-archive/jacana-aligned"

DATA_DIR="${EXP_DIR}"

## Modith this:
USE_TED=0
BASELINE=0

OPTION=""

if [ $BASELINE -eq 1 ]; then
    OPTION="-b"
fi

if [ $USE_TED -eq 1 ]; then
    OPTION="-d"
fi

TRAIN_FILE=${DATA_DIR}/jeopardy_sentences.aligned.jacana.train.xml

TRAIN_BNF=$EXP_DIR/answerfinder.train.align.bnf.gz
TRAIN_EDIT=$EXP_DIR/answerfinder.train.align.edit.gz
TRAIN_REF=$EXP_DIR/answerfinder.train.align.ref.gz

MODEL=$EXP_DIR/answerfinder.crf.align.model
cd ..

##### train #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderJeopardy $OPTION -i $TRAIN_FILE -o $TRAIN_BNF -r $TRAIN_REF -e $TRAIN_EDIT


##### train a model with crfsuite #####

# higher c1/c2 means more smoothing
# $CRF learn -p c1=1.0 -p c2=0.0 -m $MODEL $TRAIN_BNF  > /dev/null
    # 5-fold cross-validation on train
    # $CRF learn -g5 -x -m $MODEL $TRAIN_BNF

##### test on dev #####
#    -t, --test          Report the performance of the model on the data
#    -r, --reference     Output the reference labels in the input data
#    -p, --probability   Output the probability of the label sequences
#    -i, --marginal      Output the marginal probabilities of items

# $CRF tag -m $MODEL -tpri $DEV_BNF | scripts/mergeRefWithCRFsuite.py $DEV_REF

##### test on test #####
TEST_FILE=${DATA_DIR}/jeopardy_sentences.aligned.jacana.test.xml
TEST_BNF=$EXP_DIR/answerfinder.test.align.bnf.gz
TEST_REF=$EXP_DIR/answerfinder.test.align.ref.gz
TEST_EDIT=$EXP_DIR/answerfinder.test.align.edit.gz
TEST_TAG=$EXP_DIR/answerfinder.test.align.tag

## extract features for all positive and negative examples
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderJeopardy $OPTION -i $TEST_FILE -o $TEST_BNF -r $TEST_REF -e $TEST_EDIT -a
## tag all examples
# $CRF tag -m $MODEL -tpri $TEST_BNF | scripts/mergeRefWithCRFsuite.py $TEST_REF | tee $TEST_TAG

##### Aggregate answers and compute F1 #####
# scripts/aggregateAnswer.py $TEST_TAG

cd bin
