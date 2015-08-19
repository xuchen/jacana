#! /bin/bash

# Answer Extraction experiment
# This is the script to train a factoid QA model and test it.
# To run it, you need to have CRFSuite (http://www.chokkan.org/software/crfsuite/)
# installed, and modify the following two commands properly:

# if Eclipse compiles everything to the "bin" folder
JAVA_CMD="java -Xmx2g -classpath bin:lib/*"
# if using the pre-compiled all-in-one .jar file
#JAVA_CMD="java -Xmx2g -classpath jacana-qa.jar"

# path to CRFSuite, on Debain-based Linux and Mac,
# you should be able to install it with:
# sudo apt-get install crfsuite
# or:
# sudo port install crfsuite
CRF=crfsuite


# set to dev or test
type=dev

TEST=dev-test
#TEST=test

STANFORD_PARSED=0

EXP_DIR="tree-edit-data/answerSelectionExperiments"

if [ $STANFORD_PARSED -eq 1 ]; then
    DATA_DIR="${EXP_DIR}/answer/stanford-parsed"
    DATA_DIR="${EXP_DIR}/answer/stanford-parsed-uiuc-ner"
else
    #DATA_DIR="${EXP_DIR}/answer/jacana-aligned"
    #DATA_DIR="${EXP_DIR}/answer/itg-aligned"
    #DATA_DIR="${EXP_DIR}/answer/meteor-aligned"
    DATA_DIR="${EXP_DIR}/answer/giza-aligned"
fi


FULL_TRAIN=1

if [ $FULL_TRAIN -eq 1 ]; then
    TRAIN_FILE=${DATA_DIR}/train2393.cleanup.aligned.xml.gz
else
    TRAIN_FILE=${DATA_DIR}/train-less-than-40.manual-edit.aligned.xml
fi

TRAIN_BNF=/tmp/answerfinder.train.align.bnf
TRAIN_EDIT=/tmp/answerfinder.train.align.edit
TRAIN_REF=/tmp/answerfinder.train.align.ref
DEV_FILE=${DATA_DIR}/${type}-less-than-40.manual-edit.aligned.xml
#DEV_FILE=${DATA_DIR}/debug.xml
DEV_BNF=/tmp/answerfinder.${type}.align.bnf
DEV_REF=/tmp/answerfinder.${type}.align.ref
DEV_EDIT=/tmp/answerfinder.${type}.align.edit

if [ $FULL_TRAIN -eq 1 ]; then
    MODEL=/tmp/answerfinder.crf.full.align.model
else
    MODEL=/tmp/answerfinder.crf.align.model
fi
cd ..

##### train #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderAlign -i $TRAIN_FILE -o $TRAIN_BNF -r $TRAIN_REF -e $TRAIN_EDIT

##### dev #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderAlign -i $DEV_FILE -o $DEV_BNF -r $DEV_REF -e $DEV_EDIT


##### train a model with crfsuite #####

# higher c1/c2 means more smoothing
$CRF learn -p c1=0.0 -p c2=1.0 -e2 -m $MODEL $TRAIN_BNF $DEV_BNF  > /dev/null
    # 5-fold cross-validation on train
    # $CRF learn -g5 -x -m $MODEL $TRAIN_BNF

##### test on dev #####
#    -t, --test          Report the performance of the model on the data
#    -r, --reference     Output the reference labels in the input data
#    -p, --probability   Output the probability of the label sequences
#    -i, --marginal      Output the marginal probabilities of items

$CRF tag -m $MODEL -tpri $DEV_BNF | scripts/mergeRefWithCRFsuite.py $DEV_REF

##### test on test #####
TEST_FILE=${DATA_DIR}/$TEST-less-than-40.manual-edit.aligned.xml
TEST_BNF=/tmp/answerfinder.test.align.bnf
TEST_REF=/tmp/answerfinder.test.align.ref
TEST_EDIT=/tmp/answerfinder.test.align.edit
TEST_TAG=/tmp/answerfinder.test.align.tag

## extract features for all positive and negative examples
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderAlign -i $TEST_FILE -o $TEST_BNF -r $TEST_REF -e $TEST_EDIT -a
## tag all examples
$CRF tag -m $MODEL -tpri $TEST_BNF | scripts/mergeRefWithCRFsuite.py $TEST_REF | tee $TEST_TAG

##### Aggregate answers and compute F1 #####
scripts/aggregateAnswer.py $TEST_TAG

cd bin
