# jacana-align


= Introduction =

jacana-align is a token-based word-aligner for English parallel sentences described in the following paper:

[http://cs.jhu.edu/~xuchen/paper/yao-jacana-wordalign-acl2013.pdf A Lightweight and High Performance Monolingual Word Aligner].
Xuchen Yao, Benjamin Van Durme, Chris Callison-Burch and Peter Clark.
Proceedings of ACL 2013, short papers.

Speed test: on my laptop (i5 2.6GHz), the aligner aligns about 65 sentence pairs per second, where each pair contains source/target sentences averaging 18 words long.

http://www.cs.jhu.edu/~xuchen/packages/jacana-align-demo.png

= Build =

jacana-align is written in a mixture of Java and Scala. If you build from ant, you have to set up the environmental variables JAVA_HOME and SCALA_HOME. In my system, I have:

`export JAVA_HOME=/usr/lib/jvm/java-6-sun-1.6.0.26`

`export SCALA_HOME=/home/xuchen/Downloads/scala-2.10.2`

Then type:

`ant -f build.align.xml`

`build/lib/jacana-align.jar` will be built for you.

If you build from Eclipse, first install [http://scala-ide.org/ scala-ide], then import the whole jacana folder as a Scala project. Eclipse should find the `.project` file and set up the project automatically for you.

= Demo =

`scripts-align/runDemoServer.sh` shows up the web demo. Direct your browser to http://localhost:8080/ and you should be able to align some sentences.

Note 1: the current model shipped with the package uses some extra lexical resources for better alignment quality. For the original aligner described in the paper, check out the accompanying paper submission package [http://cs.jhu.edu/~xuchen/packages/jacana-align-acl2013-data-results.tar.bz2 here].

Note 2: To make jacana-align know where to look for resource files, pass the property JACANA_HOME with Java when you run it:

`java -DJACANA_HOME=/path/to/jacana -cp jacana-align.jar ......`

= Browser =

You can also browse one or two alignment files (`*`.json) with *firefox* opening `src/web/AlignmentBrowser.html`:

http://www.cs.jhu.edu/~xuchen/packages/jacana-align-browser.png


Note 1: due to strict security setting for accessing local files, Chrome/IE won't work.

Note 2: the input `*`.json files have to be in the same folder with `AlignmentBrowser.html`.

= Align =

`scripts-align/alignFile.sh` aligns tab-separated sentence files and output the output to a .json file that's accepted by the browser:

`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -m Edingburgh_RTE2.all_sure.t2s.model -a s.txt -o s.json`

If you need a whitespace tokenizer, use the "-n" option.

= Training =

`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -r train.json -d dev.json -t test.json -m /tmp/align.model`

The aligner then would train on `train.json`, and report F1 values on `dev.json` for every 10 iterations, when the stopping criterion has reached, it will test on `test.json`. 

For every 10 iterations, a model file is saved to (in this example) `/tmp/align.model.iter_XX.F1_XX.X`. Normally what I do is to select the one with the best F1 on `dev.json`, then run a final test on `test.json`:

`java -DJACANA_HOME=../ -jar ../build/lib/jacana-align.jar -t test.json -m /tmp/align.model.iter_XX.F1_XX.X`

In this case since the training data is missing, the aligner assumes it's a test job, then reads model file still from the `-m` option, and test on `test.json`.

All the json files are in a format like the following (also accepted by the browser for display):

{{{
[
        {
        "id": "1",
        "name": "1",
        "source": "ECB spokeswoman , Regina Schueller , declined to comment on a report in Italy 's La Repubblica newspaper that the ECB council will discuss Mr. Fazio 's role in the takeover fight at its Sept. 15 meeting .",
        "target": "Regina Shueller works for Italy 's La Repubblica newspaper .",
        "sureAlign": "3-0 4-1 13-4 14-5 15-6 16-7 17-8 37-9",
        "possibleAlign": ""
        }
        ,
        {
        "id": "2",
        "name": "2",
        "source": "Meanwhile , in an exclusive interview with a TIME journalist , the first one-on-one session given to a Western print publication since his election as president of Iran earlier this year , Ahmadinejad attacked the `` threat '' to bring the issue of Iran 's nuclear activity to the UN Security Council by the US , France , Britain and Germany .",
        "target": "Ahmedinejad was attacked by the US , France , Britain and Germany .",
        "sureAlign": "32-0 33-2 52-3 53-4 54-5 55-6 56-7 57-8 58-9 59-10 60-11 61-12",
        "possibleAlign": ""
        }
]
}}}

Where `possibleAlign` is by default empty and not used.

The stopping criterion is to run up to 300 iterations or when the objective difference between two iterations is less than 0.001, whichever happens first. Currently they are hard-coded. If you need to be flexible on this, send me an email!
