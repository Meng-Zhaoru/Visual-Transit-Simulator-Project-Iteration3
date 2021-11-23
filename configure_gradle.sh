#!/bin/bash

MACHINE_NAME=`hostname`
if [[ ${MACHINE_NAME} != *"csel-kh1250"* ]];then
    echo "This script only works on KH 1-250 lab machines."
    exit
fi


USUAL_LOCATION="/home/"`whoami`"/.gradle"
NEW_LOCATION="/export/scratch/"`whoami`"/.gradle"
if [ -d "$USUAL_LOCATION" ]; then
    rm -r $USUAL_LOCATION
fi
if [ -d "$NEW_LOCATION" ]; then
    rm -r $NEW_LOCATION
fi
mkdir -p $NEW_LOCATION
ln -s $NEW_LOCATION $USUAL_LOCATION
echo "Done."
