#!/bin/bash

#sudo apt-get install p7zip-rar !!!!!!!

IFS=$'\n'; set -f
path=$1
for file in $(find $path -name '*.zip' -or -name '*.7z' -or -name '*.rar'); do
    echo "Unzipping $file"
    dir="$(dirname "${file}")"
    7z e -aoa -bso0 -bsp0 -o$dir -r $file *.*
    rm "$file"
done

for file in $(find $path -name '*.p7m'); do
    echo "Converting $file"

    if [[ $file =~ ".pdf.p7m" || $file =~ ".PDF.p7m" ]]; then
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".pdf.p7m"/".pdf"}"
        rm "$file"
    elif [[ $file =~ ".doc.p7m" ]]; then
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".doc.p7m"/".doc"}"
        rm "$file"
    elif [[ $file =~ ".docx.p7m" ]]; then
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".docx.p7m"/".docx"}"
        rm "$file"
    elif [[ $file =~ ".xlsx.p7m" ]]; then
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".xlsx.p7m"/".xlsx"}"
        rm "$file"
    elif [[ $file =~ ".xls.p7m" ]]; then
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".xls.p7m"/".xls"}"
        rm "$file"
    else
        openssl smime -verify -noverify -in "$file" -inform DER -out "${file/".p7m"/".p7m"}"
    fi

done
unset IFS; set +f
