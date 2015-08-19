#! /bin/bash

# set to dev or test
type=test

cd ..
JAVA_CMD="java -classpath bin:lib/*"
EXP_DIR="bio-data/"
DATA_DIR="${EXP_DIR}"

DEV_FILE=${DATA_DIR}/oct17-for-xuchen.manual-edit.test.xml
DEV_TAG=/tmp/answerfinder.baseline.tag

##### dev #####
$JAVA_CMD edu.jhu.jacana.qa.AnswerFinderBaseline -i $DEV_FILE -o $DEV_TAG -a

##### Aggregate answers and compute F1 #####
scripts/aggregateAnswer.py $DEV_TAG --no-force $DATA_DIR/oct17-for-xuchen.gold.pattern


cd bin

