@ECHO OFF

::: the local directory with the data, no trailing slash
SET DESTINATION=C:\Programming\Palladian\data

::: password for read access to the server
::: do not hard code the admin password here as this file is public
SET READ_PASSWORD=rittersport
SET RSYNC_PASSWORD=

IF EXIST %DESTINATION%\nul GOTO begin
ECHO %DESTINATION% not found, set correct path in script
GOTO end

:begin

:: create Cygwin specific path; e.g. "C:\Path" is converted to "/cygdrive/c/Path"
FOR /f "delims=" %%a IN ('cygpath.exe -u %DESTINATION%') DO SET CYGWIN_DESTINATION=%%a

GOTO case_%1

:case_up
	::: SET /P RSYNC_PASSWORD=Enter admin password: 
	rsync -avz --chmod=ugo=rwX %CYGWIN_DESTINATION%/datasets/* rsync://syncadmin@141.76.40.242/all_data_admin/datasets
	rsync -avz --chmod=ugo=rwX %CYGWIN_DESTINATION%/models/* rsync://syncadmin@141.76.40.242/all_data_admin/models
	GOTO end

:case_down
	SET RSYNC_PASSWORD=%READ_PASSWORD%
	rsync -rtvz rsync://syncuser@141.76.40.242/all_data_user/datasets %CYGWIN_DESTINATION%
	rsync -rtvz rsync://syncuser@141.76.40.242/all_data_user/models %CYGWIN_DESTINATION%
	GOTO end

:case_
	ECHO usage: datasync [up/down]
 
:end

SET RSYNC_PASSWORD=