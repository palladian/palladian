#!/bin/bash

# the local directory with the data, no training slash
DESTINATION='/home/pk/PalladianData'

# password for read access to the server
# do not hard code the admin password here as this file is public
export READ_PASSWORD='rittersport'

if [ ! -d "$DESTINATION" ]; then
	echo "$DESTINATION not found, set correct path in script"
	exit
fi

case "$1" in

'up')
	rsync -avz "$DESTINATION/datasets/" rsync://syncadmin@141.76.40.242/all_data_admin/datasets
	rsync -avz "$DESTINATION/models/" rsync://syncadmin@141.76.40.242/all_data_admin/models
;;

'down')
	export RSYNC_PASSWORD="$READ_PASSWORD"
	rsync -avz rsync://syncuser@141.76.40.242/all_data_user/datasets $DESTINATION
	rsync -avz rsync://syncuser@141.76.40.242/all_data_user/models $DESTINATION
;;

*)
	echo 'usage: datasync [up|down]'
;;
esac

