#!/bin/bash

for i in $(seq 1 27) ; do
    echo "downloading $i ..."
    wget -q http://docs.oracle.com/javase/7/docs/api/index-files/index-$i.html -O api_$i.txt
done