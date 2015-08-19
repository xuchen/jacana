#! /bin/bash

# set to dev or test
type=test

cd ..
JAVA_CMD="java -classpath bin:lib/*"
EXP_DIR="tree-edit-data/answerSelectionExperiments"
DATA_DIR="${EXP_DIR}/answer"

DEV_FILE=${DATA_DIR}/${type}-less-than-40.manual-edit.xml
DEV_TAG=/tmp/answerfinder.${type}.baseline.tag

##### dev #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderBaseline -i $DEV_FILE -o $DEV_TAG -a

##### Aggregate answers and compute F1 #####
scripts/aggregateAnswer.py $DEV_TAG --no-force


cd bin

