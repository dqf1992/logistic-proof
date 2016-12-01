#!/usr/local/bin python
# coding=utf-8

import os
import time

caseFolder = "testcase"
num = 32
os.system('rm -rf results')
os.system('mkdir -p results')
os.system('cp src/*.java .')
os.system('javac homework.java')

for i in xrange(1,num+1):
    os.system('cp ./%s/input{0}.txt ./input.txt'.format(i) %caseFolder)
    print("-->On test case #{0}<--".format(i))
    start_time = time.time();
    os.system('java homework > /dev/null')
    print("Running time: {0}ms".format(int((time.time() - start_time) * 1000)))
    os.system('diff ./output.txt ./%s/output{0}.txt'.format(i) %caseFolder)
    os.system('mv ./output.txt ./results/output{0}.txt'.format(i))

os.system('rm *.txt > /dev/null')
os.system('rm *.java > /dev/null')
os.system('rm *.class > /dev/null');