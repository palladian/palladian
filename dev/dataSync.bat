@ECHO OFF

::: the local directory with the data, no training slash
SET DESTINATION='/cygdrive/c/Programming/Palladian/data'

::: password for read access to the server
::: do not hard code the admin password here as this file is public
SET READ_PASSWORD='rittersport'

IF EXIST "%DESTINATION%" GOTO begin

ECHO %DESTINATION% not found, set correct path in script
GOTO end

:begin

GOTO case_%1

:case_up
	rsync -avz "$DESTINATION/datasets" rsync://syncadmin@141.76.40.242/all_data_admin/datasets
	rsync -avz "$DESTINATION/models" rsync://syncadmin@141.76.40.242/all_data_admin/models
	GOTO end

:case_down
	SET RSYNC_PASSWORD="$READ_PASSWORD"
	rsync -avz rsync://syncuser@141.76.40.242/all_data_user/datasets $DESTINATION
	rsync -avz rsync://syncuser@141.76.40.242/all_data_user/models $DESTINATION
	GOTO end

:case_
	ECHO usage: datasync [up/down]

:end
