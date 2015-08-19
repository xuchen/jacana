#!/usr/bin/python

import sys
import json

if len(sys.argv) != 2:
  sys.exit("Usage: %s <result_file>" % sys.argv[0])

"""return a tuple with recall, precision, and f1 for one example"""
def computeF1(goldList,predictedList):

  """Assume all questions have at least one answer"""
  if len(goldList)==0:
    raise Exception("gold list may not be empty")
  """If we return an empty list recall is zero and precision is one"""
  if len(predictedList)==0:
    return (0,0,0)
  """It is guaranteed now that both lists are not empty"""

  precision = 0
  for entity in predictedList:
    if entity in goldList:
      precision+=1
  precision = float(precision) / len(predictedList)

  recall=0
  for entity in goldList:
    if entity in predictedList:
      recall+=1
  recall = float(recall) / len(goldList)

  f1 = 0
  if precision+recall>0:
    f1 = 2*recall*precision / (precision + recall)
  return (recall,precision,f1)

averageRecall=0
averagePrecision=0
averageF1=0
count=0
retrieved = 0
relevant = 0

"""Go over all lines and compute recall, precision and F1"""
with open(sys.argv[1]) as f:
  for line in f:
    tokens = line.strip().split("\t")
    gold = json.loads(tokens[1])
    count += 1
    relevant += 1 # len(gold) is always > 0
    if not tokens[2] == "NO_ANSWER":
        predicted = json.loads(tokens[2])
        recall, precision, f1 = computeF1(gold,predicted)
        averageRecall += recall
        averagePrecision += precision
        averageF1 += f1
        retrieved += 1 # If you answer it - it is retrieved, even if it is empty

"""Print final results"""
averageRecall = float(averageRecall) / relevant
averagePrecision = float(averagePrecision) / retrieved
averageF1 = float(averageF1) / count
print "Number of questions: " + str(count)
print "Average recall over questions: " + str(averageRecall)
print "Average precision over questions: " + str(averagePrecision)
print "Average f1 over questions: " + str(averageF1)
averageNewF1 = 2 * averageRecall * averagePrecision / (averagePrecision + averageRecall)
print "F1 of average recall and average precision: " + str(averageNewF1)

