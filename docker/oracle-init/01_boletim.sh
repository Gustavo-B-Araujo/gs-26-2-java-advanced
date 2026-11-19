#!/bin/bash
set -e
export NLS_LANG=AMERICAN_AMERICA.AL32UTF8
for arquivo in /opt/boletim-sql/0*.sql; do
  echo "Boletim Plus: executando $arquivo"
  sqlplus -s "${APP_USER}/${APP_USER_PASSWORD}@//localhost/FREEPDB1" <<SQL
WHENEVER SQLERROR EXIT FAILURE
@${arquivo}
EXIT
SQL
done
