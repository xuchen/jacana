#! /bin/bash

JAVA_CMD="java -Xmx2g -classpath bin:lib/*"

cd ..
$JAVA_CMD edu.jhu.jacana.test.TestMstXml -i $@
