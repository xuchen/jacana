#! /bin/bash

# set to dev or test
type=dev

EXP_DIR="bio-data/"
DATA_DIR="${EXP_DIR}"

# if use wordnet then '-w', else ''
USE_WORDNET=''

JAVA_CMD="java -Xmx2g -classpath bin:lib/*"
TRAIN_FILE=${DATA_DIR}/oct17-for-xuchen.manual-edit.xml
TRAIN_BNF=/tmp/answerfinder.train.bnf
TRAIN_EDIT=/tmp/answerfinder.train.edit
TRAIN_REF=/tmp/answerfinder.train.ref
DEV_FILE=${DATA_DIR}/oct17-for-xuchen.manual-edit.xml
#DEV_FILE=${DATA_DIR}/debug.xml
DEV_BNF=/tmp/answerfinder.${type}.bnf
DEV_REF=/tmp/answerfinder.${type}.ref
DEV_EDIT=/tmp/answerfinder.${type}.edit

MODEL=/tmp/answerfinder.crf.model
cd ..

##### train #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinder -i $TRAIN_FILE -o $TRAIN_BNF -r $TRAIN_REF -e $TRAIN_EDIT $USE_WORDNET

##### dev #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinder -i $DEV_FILE -o $DEV_BNF -r $DEV_REF -e $DEV_EDIT $USE_WORDNET


##### train a model with crfsuite #####
CRF=crfsuite

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
TEST_FILE=${DATA_DIR}/oct17-for-xuchen.manual-edit.test.xml
#TEST_FILE=${DATA_DIR}/nov3-one-train-10sentences.manual-edit.xml
TEST_BNF=/tmp/answerfinder.test.bnf
TEST_REF=/tmp/answerfinder.test.ref
TEST_EDIT=/tmp/answerfinder.test.edit
TEST_TAG=/tmp/answerfinder.test.tag

## extract features for all positive and negative examples
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinder -i $TEST_FILE -o $TEST_BNF -r $TEST_REF -e $TEST_EDIT -a $USE_WORDNET
## tag all examples
$CRF tag -m $MODEL -tpri $TEST_BNF | scripts/mergeRefWithCRFsuite.py $TEST_REF | tee $TEST_TAG

##### Aggregate answers and compute F1 #####
scripts/aggregateAnswer.py $TEST_TAG $DATA_DIR/oct17-for-xuchen.gold.pattern

cd bin
