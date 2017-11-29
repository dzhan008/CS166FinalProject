#! /bin/bash

#PATH TO DATABASE FOLDER
export PGFOLDER=/tmp/$LOGNAME
#PATH TO DATA FOLDER
export PGDATA=$PGFOLDER/myDB/data

echo $PGFOLDER

rm -rf $PGFOLDER
rm -rf $PGFOLDER/myDB
rm -rf $PGFOLDER/myDB/data
rm -rf $PGFOLDER/myDB/sockets

mkdir $PGFOLDER
mkdir $PGFOLDER/myDB
mkdir $PGFOLDER/myDB/data
mkdir $PGFOLDER/myDB/sockets
sleep 1

initdb
sleep 1

cp ../data/airline.csv /tmp/$LOGNAME/myDB/data/
cp ../data/bookings.csv /tmp/$LOGNAME/myDB/data/
cp ../data/flights.csv /tmp/$LOGNAME/myDB/data/
cp ../data/passenger.csv /tmp/$LOGNAME/myDB/data/
cp ../data/ratings.csv /tmp/$LOGNAME/myDB/data/