#!/bin/bash

FILE1="$1"
FILE2="$2"

if [[ ! -f "$FILE1" || ! -f "$FILE2" ]]; then
    echo "Both files must exist."
    exit 1
fi


if diff -U0 "$FILE1" "$FILE2" | grep -c '^+[^+]'; then
    echo "Error: $FILE2 has failures not in $FILE1, likely regression"
    exit 1
fi

MISSING1=$(diff -U0 "$FILE1" "$FILE2" | grep -c '^-')

MISSING2=$(diff -U0 "$FILE1" "$FILE2" | grep -c '^+')

if (( MISSING1 > MISSING2 )); then
    echo "$FILE2 has less failures than $FILE1, improvement."
fi
