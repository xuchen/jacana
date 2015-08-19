# jacana-freebase

= Introduction =

jacana-freebase is a question answering engine that extracts answers from [http://www.freebase.com Freebase], given a short factoid web-style question (just as the one you type into a search engine), such as _who did natalie portman play in star wars?_

jacana-freebase is very "researchy" code with a GPLv3 license. It runs in batch mode with a bunch of scripts. Currently there is no plan to engineer it into a real-time QA engine. Underlying algorithm and evaluation result can be found at:

Information Extraction over Structured Data: Question Answering with Freebase.
Xuchen Yao and Benjamin Van Durme. Proceedings of ACL 2014.


= Build =

jacana-freebase is written in a mixture of Java and Scala. If you build from ant, you have to set up the environmental variables JAVA_HOME and SCALA_HOME. In my system, I have:

`export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.26`

`export SCALA_HOME=/home/xuchen/Downloads/scala-2.10.3`

Then type:

`ant -f build.freebase.xml`

An all-in-one jar file, `build/lib/jacana-freebase.jar`, will be built for you.

If you build from Eclipse, first install [http://scala-ide.org/ scala-ide], then import the whole jacana folder as a Scala project. Eclipse should find the `.project` file and set up the project automatically for you.


= Run =

The training/testing pipeline:
 # Given training/test files, run `edu.jhu.jacana.freebase.featureextraction.GraphFeatureWriter` to extract features, and save in `.bnf` files
 # run a high performance trainer/classifier, [http://www.chokkan.org/software/classias/ Classias], to train the model, and tag the test file.
 # the script `scripts-freebase/computeF1.py' calculates the final F1 score.

The script `scripts-freebase/extractFeaturesGraph.sh` reproduces the above pipeline. To run the code, you need to:
 * pass the JACANA_HOME variable to java through `-DJACANA_HOME=/folder/to/jacana` where it points to the parent folder of `scripts-freebase`.
 * pass the FREEBASE_DATA variable to java through `-DFREEBASE_DATA=/folder/to/freebase-data` where you should download additional data from here: [http://cs.jhu.edu/~xuchen/packages/freebase-data.tar freebase-data.tar (3.3GB)]
 * have [http://www.chokkan.org/software/classias/ Classias] installed.
