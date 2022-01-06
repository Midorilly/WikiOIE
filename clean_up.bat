@echo off
SETLOCAL ENABLEDELAYEDEXPANSION
SET /P p=Please enter the folder path: 
CD %p%
FOR /R %%G IN (*.zip) DO (
    ECHO Extracting %%~nxG
    7z e -aoa -bso0 -bsp0 -o%%~pG -r %%G *.*
    DEL %%G)
FOR /R %%G IN (*.7z) DO (
    ECHO Extracting %%~nxG
    7z e -aoa -bso0 -bsp0 -o%%~pG -r %%G *.*
    DEL %%G)
FOR /R %%G IN (*.rar) DO (
    ECHO Extracting %%~nxG
    7z e -aoa -bso0 -bsp0 -o%%~pG -r %%G *.*
    DEL %%G)

FOR /R %%G IN (*.pdf.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnG
    openssl smime -verify -noverify -in %%G -inform DER -out !out! 
    DEL %%G)
FOR /R %%G IN (*.doc.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnG
    openssl smime -verify -noverify -in %%G -inform DER -out !out!
    DEL %%G)
FOR /R %%G IN (*.docx.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnG
    openssl smime -verify -noverify -in %%G -inform DER -out !out!
    DEL %%G)
FOR /R %%G IN (*.xlsx.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnG
    openssl smime -verify -noverify -in %%G -inform DER -out !out!
    DEL %%G)
FOR /R %%G IN (*.xls.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnG
    openssl smime -verify -noverify -in %%G -inform DER -out !out!
    DEL %%G)
FOR /R %%G IN (*.p7m) DO (
    ECHO Converting %%~nxG
    SET out=%%~pnxG
    openssl smime -verify -noverify -in %%G -inform DER -out !out!
) 

PAUSE
 