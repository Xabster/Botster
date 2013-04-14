#!/bin/bash

for f in api_*.txt ; do
    cat "$f" | sed -e 's/<span class="strong">//' > tmp && mv tmp $f
done