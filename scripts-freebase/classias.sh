#! /bin/bash

# grep   -n "^[^-#+]" /tmp/train.bnf 

CLASSIAS=classias

# ALGORITHM=averaged_perceptron
ALGORITHM="lbfgs.logistic"


# TRAIN_BNF=/tmp/dev.bnf
# MODEL=/tmp/f.model.$ALGORITHM
# TEST_BNF=/tmp/dev.bnf
# TEST_OUT=/tmp/f.tagged.$ALGORITHM

TRAIN_BNF=$1
MODEL=$1.model.$ALGORITHM
TEST_BNF=$2
TEST_OUT=$2.tagged.$ALGORITHM

##   -a, --algorithm=NAME  specify a training algorithm (DEFAULT='lbfgs.logistic')
##       lbfgs.logistic        L1/L2-regularized logistic regression (LR) by L-BFGS
##       averaged_perceptron   averaged perceptron
##       pegasos.logistic      L2-regularized LR by Pegasos
##       pegasos.hinge         L2-regularized linear L1-loss SVM by Pegasos
##       truncated_gradient.logistic
##                             L1-regularized LR by Truncated Gradient
##       truncated_gradient.hinge
##                             L1-regularized L1-loss SVM by Truncated Gradient
##
## for L1 LR: use "-p c1=1 -p c2=0" with lbfgs.logistic
$CLASSIAS-train -t b -a $ALGORITHM -m $MODEL $TRAIN_BNF

$CLASSIAS-tag -m $MODEL -t -a -w -p -r -k < $TEST_BNF  > $TEST_OUT

./computeF1.py $TEST_OUT

# lbfgs.logistic (L2)
# Accuracy: 0.9965 (774993/777737)
# Micro P, R, F1: 0.5142 (724/1408), 0.2601 (724/2784), 0.3454
# compute F1:
# P: 0.556, R: 0.262, F1: 0.356

# averaged_perceptron:
# Micro P, R, F1: 0.2582 (1027/3978), 0.3689 (1027/2784), 0.3038
# compute F1:
# P: 0.350, R: 0.376, F1: 0.362

# pegasos.hinge
# Micro P, R, F1: 0.0036 (2784/777737), 1.0000 (2784/2784), 0.0071
# P: 0.007, R: 0.984, F1: 0.013
