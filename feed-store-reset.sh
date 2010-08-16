#!/bin/sh
datum="`date +%F-%R:%S`"
dcb="/home/muthmann/workspace/de.tud.inf.rn.iir.toolkit.feeds/target/classes"
echo sichere feedPosts nach feedPosts.$datum 
mv $dcb/data/datasets/feedPosts $dcb/data/datasets/feedPosts.$datum
echo sichere logfile nach reader.log.$datum
mv $dcb/data/logs/reader.log $dcb/data/logs/reader.log.$datum
echo setze Tabelle feeds zurück
mysql tudiirdb -utoolkit -ptoolkit -e 'UPDATE feeds SET checks = 0, lastHeadlines = "", minCheckInterval = 10, maxCheckInterval = 10, lastFeedEntry = NULL;'



