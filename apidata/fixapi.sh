#!/bin/bash

for f in api_*.txt ; do
    cat "$f" | sed -e 's/<span class="memberNameLink">//' > tmp && mv tmp $f
done