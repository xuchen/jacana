#jacana-qa.

= Introduction =

jacana-qa is a TREC-style QA engine that is able to answer basic factoid questions (what, which, when, where, who). It casts answer extraction as a sequence tagging problem and captures a lot of traditional QA intuitions succinctly. For detailed description, please check out the following paper:

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-qa-naacl2013.pdf Answer Extraction as Sequence Tagging with Tree Edit Distance].
Xuchen Yao, Benjamin Van Durme, Peter Clark and Chris Callison-Burch.
Proceedings of NAACL 2013.

jacana-qa is research code. It doesn't provide any pre-trained model for you to use in any real world applications. However, scripts and data are provided to easily train/tune/test your model. That is, jacana-qa is written in a manner that best suits the research purpose of implementing/comparing QA algorithms.

If you need to deploy it as an almost real-time QA engine (jacana-qa is able to answer tens of questions per second including parsing time, or hundreds per second excluding parsing time), please contact the author for guidance of the piece of the code that does the job for you (so you don't re-do what's done).


= Build =

jacana-qa is written in Java. After downloading it, go to the containing folder and type:

`ant -f build.qa.xml`

Then `jacana-qa.jar` is built for you. To use it, you also need to install [http://www.chokkan.org/software/crfsuite/  CRFsuite]. If you are using Linux, it should be in your repository; if you are using Mac, it's in `MacPorts`. The author also provides Windows-based build on its homepage.

= Develop with Eclipse =

Quite some Scala codes were added to the whole jacana package as the author continued to develop jacana-align. With a plain-text editor and ant, you are able to develop with jacana-qa. If you need to set it up within Eclipse, you can either:

  * set up the Eclipse environment for Scala, following JacanaAlign.
  * exclude the scala-related code (in `src/scala`) by either removing it, or excluding it from the source folder in Eclipse. Then treat jacana as a pure java package.

= Running jacana-qa =

The file `scripts/answerfinder.crf.sh` provides the basic script for training and testing your QA model.

There are also three scripts (`rte.test.sh`, `pp.test.sh` and `qa.test.sh` in `scripts`). They correspond to the three tasks described in [http://www.cs.cmu.edu/~mheilman/ Michael Heilman]'s paper:

M. Heilman and N. A. Smith. 2010. Tree Edit Models for Recognizing Textual Entailments, Paraphrases, and Answers to Questions. In Proc. of NAACL/HLT.

= Enhancing Your Retrieval with jacana-qa =

If you decide to use jacana-qa on a fixed-size corpus (i.e., not web) and are bothered by the low hit rates of your information retrieval engine (e.g., Indri):

With the pre-trained model of jacana-qa, you are able to do structured information retrieval for question answering without any manual templates and retrieval overhead (you still need to index your corpus with desired linguistic annotations though, such as POS tags and named entities). For a quick look at how this idea works, check out:

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-ir-acl2013.pdf Automatic Coupling of Answer Extraction and Information Retrieval].
Xuchen Yao, Benjamin Van Durme and Peter Clark.
Proceedings of ACL 2013, short papers. 
