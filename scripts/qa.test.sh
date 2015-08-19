#! /bin/bash

# set to dev or test
type=test
# capitalize the first char
Type=`echo "${type:0:1}" | tr a-z A-Z`${type:1}

JAVA_CMD="java -Xms1g -Xmx2000M -classpath bin:lib/*"
EXP_DIR="tree-edit-data/answerSelectionExperiments"

STANFORD_PARSED=0

if [ $STANFORD_PARSED -eq 1 ]; then
    DATA_DIR="${EXP_DIR}/data/stanford-parsed"
else
    DATA_DIR="${EXP_DIR}/data"
fi

TRAIN_ARFF=/tmp/qa.train.arff
DEV_ARFF=/tmp/qa.${type}.arff
MODEL=/tmp/qa.logistic.model
cd ..

DEBUG=0
##### train #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.AnswerSelection ${DATA_DIR}/train-less-than-40.xml $TRAIN_ARFF /tmp/qa.train.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.AnswerSelection ${DATA_DIR}/train-less-than-40.xml $TRAIN_ARFF
fi

##### dev #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.AnswerSelection ${DATA_DIR}/${type}-less-than-40.xml $DEV_ARFF /tmp/qa.${type}.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.AnswerSelection ${DATA_DIR}/${type}-less-than-40.xml $DEV_ARFF
fi


##### train a logistic regression model with weka #####
WEKA_OUTPUT="/tmp/weka-qa-${type}.txt"

#java -cp lib/weka.jar weka.classifiers.functions.VotedPerceptron -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
#java -cp lib/weka.jar weka.classifiers.functions.SMO -K weka.classifiers.functions.supportVector.RBFKernel -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
# output more detailed info (yes, we are retraining)
java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -d $MODEL -i #-threshold-file /tmp/qa.dev.threash.csv

##### convert the output to TREC format #####
TREC_DATA="/tmp/${type}.trec_eval"
perl ${EXP_DIR}/convertWekaOutputToTRECFormat.pl $WEKA_OUTPUT ${DATA_DIR}/${type}-less-than-40.xml > $TREC_DATA 2>/dev/null

##### evaluate #####
echo "TREC Scores:"
TREC_OUT="/tmp/${type}.trec_out"
${EXP_DIR}/trec_eval-8.0/trec_eval -q -c ${EXP_DIR}/eval/${Type}-T40.judgment $TREC_DATA > $TREC_OUT
grep -E -w 'map.*all|recip_rank.*all' $TREC_OUT

cd bin

#### significance test #####
# must prepare two $TREC_OUT files
# perl -w ${EXP_DIR}/significance_test/paired-randomization-test-v2.pl -t g -d ${EXP_DIR}/significance_test/desiredMetricsFile /tmp/dev.trec_out.baseline /tmp/dev.trec_out


