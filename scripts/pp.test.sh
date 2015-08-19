#! /bin/bash

# set to train or combined
train=train
# set to dev or test
type=test

JAVA_CMD="java -classpath bin:lib/*"
EXP_DIR="tree-edit-data/paraphraseIdentification"

STANFORD_PARSED=0

if [ $STANFORD_PARSED -eq 1 ]; then
    DATA_DIR="${EXP_DIR}/data/stanford-parsed"
else
    DATA_DIR="${EXP_DIR}/data"
fi

TRAIN_ARFF=/tmp/pp.${train}.arff
DEV_ARFF=/tmp/pp.${type}.arff

MODEL=/tmp/pp.logistic.model
cd ..

DEBUG=0
##### train #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.ParaphraseIdentification ${DATA_DIR}/par_${train}.dat $TRAIN_ARFF /tmp/pp.${train}.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.ParaphraseIdentification ${DATA_DIR}/par_${train}.dat $TRAIN_ARFF
fi

##### dev #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.ParaphraseIdentification ${DATA_DIR}/par_${type}.dat $DEV_ARFF /tmp/pp.${type}.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.ParaphraseIdentification ${DATA_DIR}/par_${type}.dat $DEV_ARFF
fi


##### train a logistic regression model with weka #####
WEKA_OUTPUT="/tmp/weka-pp-${type}.txt"

#java -cp lib/weka.jar weka.classifiers.functions.VotedPerceptron -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
#java -cp lib/weka.jar weka.classifiers.functions.SMO -K weka.classifiers.functions.supportVector.RBFKernel -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
#java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -d $MODEL -i #-threshold-file /tmp/pp.${type}.threash.csv

cd bin
