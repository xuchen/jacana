#! /bin/bash

CMD="java -mx4g -DJACANA_HOME=../ -cp $SCALA_HOME/lib/*:../lib/*:../bin edu.jhu.jacana.freebase.featureextraction.FeatureWriter"

echo $CMD

OUT_DIR=/Volumes/Data1/xuchen/freebase-exp/manual-align

TRAIN_IN="freebase-data/webquestions/webquestions.examples.train.80.json"
TRAIN_OUT=$OUT_DIR/train.bnf

$CMD $TRAIN_IN $TRAIN_OUT

DEV_IN="freebase-data/webquestions/webquestions.examples.dev.20.json"
DEV_OUT=$OUT_DIR/dev.bnf

$CMD $DEV_IN $DEV_OUT

./classias.sh $TRAIN_OUT $DEV_OUT

# P: 0.406, R: 0.244, F1: 0.305
