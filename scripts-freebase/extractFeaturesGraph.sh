#! /bin/bash

# if Eclipse has compiled the classes for you:
CMD="java -mx6g -DJACANA_HOME=../ -DFREEBASE_DATA=/Users/xuchen/Halo2/freebase/ -cp $SCALA_HOME/lib/*:../lib/*:../bin edu.jhu.jacana.freebase.featureextraction.GraphFeatureWriter"

# or alternatively with ant:
# cd ..; ant -f build.freebase.xml; cd -
# CMD="java -mx6g -DJACANA_HOME=../ -DFREEBASE_DATA=/Users/xuchen/Halo2/freebase/ -cp ../build/lib/jacana-freebase.jar edu.jhu.jacana.freebase.featureextraction.GraphFeatureWriter"

echo $CMD
OUT_DIR=/Volumes/Data1/xuchen/freebase-exp/graph

# expect huge .bnf output (around 50GB), or you can directly output gzipped file
TRAIN_IN="freebase-data/webquestions/webquestions.examples.train.80.json"
TRAIN_OUT=$OUT_DIR/train.bnf

$CMD $TRAIN_IN $TRAIN_OUT

DEV_IN="freebase-data/webquestions/webquestions.examples.dev.20.json"
DEV_OUT=$OUT_DIR/dev.bnf

$CMD $DEV_IN $DEV_OUT

./classias.sh $TRAIN_OUT $DEV_OUT

TEST_IN="freebase-data/webquestions/webquestions.examples.test.retrieved.json"
TEST_OUT=$OUT_DIR/test.retrieved.full.bnf
# true for gold retrieval (in training), false for test
$CMD $TEST_IN $TEST_OUT false

## in paper: 
## 1. extract .bnf on ALL train.80+dev.20 data
## 2. down-sample
## 3. test
