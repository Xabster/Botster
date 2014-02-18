#!/bin/bash

for i in $(seq 1 27) ; do
    echo "downloading $i ..."
    wget -q http://download.java.net/jdk8/docs/api/index-files/index-$i.html -O api_$i.txt
done