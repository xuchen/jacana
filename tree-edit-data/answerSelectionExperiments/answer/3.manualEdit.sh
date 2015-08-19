#! /bin/sh
for f in train dev test
do
    mv -i ${f}-less-than-40.answer-appended.xml ${f}-less-than-40.manual-edit.xml
done

# now let the pain of manual edit begin...
# multiple answers are separated by #
#
# for train2393, I just did simple cleanup
