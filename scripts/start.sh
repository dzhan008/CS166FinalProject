#! /bin/bash

#PATH TO DATABASE FOLDER
export PGFOLDER=/tmp/$LOGNAME
#PATH TO DATA FOLDER
export PGDATA=$PGFOLDER/myDB/data
#DATABASE LISTENING PORT
export PGPORT=$1

pg_ctl -o "-c unix_socket_directories=$PGFOLDER/myDB/sockets -p $PGPORT" -D $PGDATA -l $PGFOLDER/logfile start