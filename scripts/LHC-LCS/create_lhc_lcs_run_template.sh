#!/bin/bash
if [ -z "$1" ] || [ -z "$2" ]
    then
        echo "usage: create_lhc_lcs_run_template <parent-path> <run-folder-name>" >&2
       exit
fi
parent_path="$1"
run_name="$2"

rm -rf PI_Lab_Xin_Wang

mkdir PI_Lab_Xin_Wang
mkdir PI_Lab_Xin_Wang/Platform
mkdir PI_Lab_Xin_Wang/Platform/$run_name
mkdir PI_Lab_Xin_Wang/Platform/$run_name/preprocessed
mkdir PI_Lab_Xin_Wang/Platform/$run_name/preprocessed/documentation
mkdir PI_Lab_Xin_Wang/Platform/$run_name/raw
mkdir PI_Lab_Xin_Wang/Platform/$run_name/raw/documentation

cat >PI_Lab_Xin_Wang/Platform/$run_name.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Run"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/Platform/$run_name/preprocessed.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Preprocessed_Data"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/Platform/$run_name/preprocessed/documentation.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Documentation"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/Platform/$run_name/raw.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Preprocessed_Data"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/Platform/$run_name/raw/documentation.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Documentation"
    }
  ]
}
EOF

cat >./EXCLUDE_FILE_PATH.txt <<EOF
**.metadata.json
EOF

dm_register_directory -e EXCLUDE_FILE_PATH.txt PI_Lab_Xin_Wang/Platform $parent_path

