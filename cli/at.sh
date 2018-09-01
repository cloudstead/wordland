#!/bin/bash

ARR=()

ARR+=("\"symbol\": \"A\", \"index\": 0")
ARR+=("\"symbol\": \"B\", \"index\": 1")
ARR+=("\"symbol\": \"C\", \"index\": 2")

echo "initial---"
echo "ARR[@]=${ARR[@]}"
echo "#ARR[*]=${#ARR[*]}"
echo "ARR[0]=${ARR[0]}"
echo "ARR[1]=${ARR[1]}"
echo "ARR[2]=${ARR[2]}"
echo

unset 'ARR[1]'

echo "after unset---"
echo "ARR[@]=${ARR[@]}"
echo "#ARR[*]=${#ARR[*]}"
echo "ARR[0]=${ARR[0]}"
echo "ARR[1]=${ARR[1]}"
echo "ARR[2]=${ARR[2]}"
echo


ARR=( "${ARR[@]:0:1}" "${ARR[@]:2}" )

echo "after recreate ARR---"
echo "ARR[@]=${ARR[@]}"
echo "#ARR[*]=${#ARR[*]}"
echo "ARR[0]=${ARR[0]}"
echo "ARR[1]=${ARR[1]}"
echo "ARR[2]=${ARR[2]}"
echo
