#! /bin/sh

RULE=intersect
RULE=grow-diag-final
RULE=union

java -DJACANA_HOME=/home/hltcoe/xuchen/jacana/jacana -cp ~/jacana/jacana/build/lib/jacana-align.jar edu.jhu.jacana.align.evaluation.AlignEvaluator ~/jacana/jacana/alignment-data/edinburgh/gold.test.sure.json edinburgh.align.giza.$RULE.json

# intersect:
# Exact match          13.1   (40 of 306)
# Macro-averaged (reported in paper) :
#                     average (identical/non-identical):
#   Precision          89.7 (96.9/73.0)
#   Recall             81.7 (96.1/51.3)
#   F1                 85.5 (96.5/60.3)
# percentage of identical alignments in gold: 0.708(4085/5766)
# percentage of identical alignments in test: 0.791(4040/5109)
# 
# Macro-averaged (reported in paper) :
#                     (token/phrase):
#   Precision               (89.4/0.0)
#   Recall                  (92.5/0.0)
#   F1                      (90.9/NaN)
# 
# # grow-diag-final:
# Exact match          6.5   (20 of 306)
# Macro-averaged (reported in paper) :
#                     average (identical/non-identical):
#   Precision          65.7 (94.4/31.4)
#   Recall             90.2 (98.9/72.5)
#   F1                 76.0 (96.6/43.8)
# percentage of identical alignments in gold: 0.708(4085/5766)
# percentage of identical alignments in test: 0.544(4343/7985)
# 
# Macro-averaged (reported in paper) :
#                     (token/phrase):
#   Precision               (77.7/19.6)
#   Recall                  (89.6/56.6)
#   F1                      (83.2/29.1)
# 
# # union:
# Exact match          6.2   (19 of 306)
# Macro-averaged (reported in paper) :
#                     average (identical/non-identical):
#   Precision          63.4 (93.9/28.8)
#   Recall             91.3 (99.4/74.0)
#   F1                 74.8 (96.6/41.5)
# percentage of identical alignments in gold: 0.708(4085/5766)
# percentage of identical alignments in test: 0.519(4407/8497)
# 
# Macro-averaged (reported in paper) :
#                     (token/phrase):
#   Precision               (78.3/18.7)
#   Recall                  (89.0/57.9)
#   F1                      (83.3/28.3)
 
