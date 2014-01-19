@ECHO OFF
mvn -DskipTests package jboss-as:deploy
pause
