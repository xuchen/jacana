**Note**: Original repo was at Google Code (https://code.google.com/p/jacana/). It was too big and after conversion, commit history was lost. Also,
the data files had to be separated. Please download the [data files](http://www.cs.jhu.edu/~xuchen/packages/jacana-data.tar.bz2)
and untar it to the same folder as the project files.

Jacana (/dʒəˈkɑːnə/) consists of three parts:
  * jacana-qa, a Question Answering engine for TREC-style questions, written in Java.
  * jacana-align, a monolingual word aligner for English, written in Java and Scala.
  * jacana-freebase, a Question Answering engine for web-style questions on Freebase, written in Java and Scala.

**download**: 

[http://www.cs.jhu.edu/~xuchen/packages/jacana-align.bin.20141029.tar.bz2 jacana-align.bin.20141029.tar.bz2] (103M, just the binary version of the aligner)

[http://www.cs.jhu.edu/~xuchen/packages/jacana.20141029.tar.bz2 jacana.20141029.tar.bz2] (660MB, everything (qa+align+freebase, source code only, no compiled jars), mostly data files and lexical resources)

jacana-qa
=========

contains:
  * a ranker for ranking whether a sentence contains an answer for a given question,
  * an answer extractor for extracting exact answer segments from sentences.

It provides software implementation and dataset for the following two papers:

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-qa-naacl2013.pdf Answer Extraction as Sequence Tagging with Tree Edit Distance].
Xuchen Yao, Benjamin Van Durme, Peter Clark and Chris Callison-Burch.
Proceedings of NAACL 2013.

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-ir-acl2013.pdf Automatic Coupling of Answer Extraction and Information Retrieval].
Xuchen Yao, Benjamin Van Durme and Peter Clark.
Proceedings of ACL 2013, short papers.

HOWTO: [JacanaQA](JacanaQA.md)

jacana-align
============

is a token-based aligner described in the following paper:

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-wordalign-acl2013.pdf A Lightweight and High Performance Monolingual Word Aligner].
Xuchen Yao, Benjamin Van Durme, Chris Callison-Burch and Peter Clark.
Proceedings of ACL 2013, short papers.

[http://jacana.clsp.jhu.edu/ online demo]

HOWTO: [JacanaAlign](JacanaAlign.md)

jacana-freebase
===============

is a question answering engine that extracts answers from Freebase:

Information Extraction over Structured Data: Question Answering with Freebase. Xuchen Yao and Benjamin Van Durme. Proceedings of ACL 2014.

HOWTO: [JacanaFreebase](JacanaFreebase.md)

Change Log
==========

2015-08-18:
* imported from Google Code. Original repo exceeded GitHub limit (less 1G repo, single file less than 100MB). Had to separate data from main repo and lost commit history.

2014-10-29:
* jacana-align: is now thread-safe, model is also updated/re-trained

2014-10-2
* jacana-align: removed word2vec since it's too slow
* jacana-align: added parallel support (by default), use the "--single" parameter to disable it

2014-3-19
* released jacana-freebase

2013-11-3
* added more lexical resources: word2vec, wiktionary, better PPDB support
* feature tuning for token alignment
* full UTF-8 support in AJAX demo

2013-8-5
* retrained model on larger alignment corpus
* now compiles with Scala-2.10.2
* fixed Windows compatability issue of several lexical models
* demo pre-loads model first

2013-5-28
* Initial release
