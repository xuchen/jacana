#! /bin/bash

# set to dev or test
type=dev

JAVA_CMD="java -classpath bin:lib/*"
EXP_DIR="tree-edit-data/entailment"

STANFORD_PARSED=0

if [ $STANFORD_PARSED -eq 1 ]; then
    DATA_DIR="${EXP_DIR}/data/stanford-parsed"
else
    DATA_DIR="${EXP_DIR}/data"
fi

TRAIN_ARFF=/tmp/rte.train.arff
DEV_ARFF=/tmp/rte.${type}.arff

MODEL=/tmp/rte.logistic.model
cd ..

DEBUG=1
##### train #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.TextualEntailment ${DATA_DIR}/trainData.dat $TRAIN_ARFF /tmp/rte.train.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.TextualEntailment ${DATA_DIR}/trainData.dat $TRAIN_ARFF
fi

##### dev #####
if [ $DEBUG -eq 1 ]; then
    $JAVA_CMD edu.jhu.jacana.reader.TextualEntailment ${DATA_DIR}/${type}Data.dat $DEV_ARFF /tmp/rte.${type}.edit
else
    $JAVA_CMD edu.jhu.jacana.reader.TextualEntailment ${DATA_DIR}/${type}Data.dat $DEV_ARFF
fi


##### train a logistic regression model with weka #####
WEKA_OUTPUT="/tmp/weka-rte-${type}.txt"

#java -cp lib/weka.jar weka.classifiers.functions.VotedPerceptron -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
#java -cp lib/weka.jar weka.classifiers.functions.SMO -K weka.classifiers.functions.supportVector.RBFKernel -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -p 0 > $WEKA_OUTPUT
java -cp lib/weka.jar weka.classifiers.functions.Logistic -t $TRAIN_ARFF -T $DEV_ARFF -d $MODEL -i #-threshold-file /tmp/rte.${type}.threash.csv

cd bin
