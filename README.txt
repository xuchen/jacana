This is jacana, an NLP package for Question Answering and Monolingual Alignment.

For detailed introductio and instruction, please visit:

http://code.google.com/p/jacana/

Change Log:

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
Initial release
