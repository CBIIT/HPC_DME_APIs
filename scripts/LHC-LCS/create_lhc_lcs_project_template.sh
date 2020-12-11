#!/bin/bash
if [ -z "$1" ]
    then
        echo "usage: create_lhc_lcs_project_template <project-name>" >&2
       exit
fi
project_name="$1"

rm -rf PI_Lab_Xin_Wang
rm -f PI_Lab_Xin_Wang.metadata.json

mkdir PI_Lab_Xin_Wang
mkdir PI_Lab_Xin_Wang/$project_name
mkdir PI_Lab_Xin_Wang/$project_name/analysis
mkdir PI_Lab_Xin_Wang/$project_name/analysis/documentation
mkdir PI_Lab_Xin_Wang/$project_name/metadata
mkdir PI_Lab_Xin_Wang/$project_name/RNASeq
#mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/Run_template
#mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/Run_template/preprocessed
#mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/Run_template/preprocessed/documentation
#mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/Run_template/raw
#mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/Run_template/raw/documentation
mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/processed
mkdir PI_Lab_Xin_Wang/$project_name/RNASeq/processed/documentation
mkdir PI_Lab_Xin_Wang/$project_name/WES
#mkdir PI_Lab_Xin_Wang/$project_name/WES/Run_template
#mkdir PI_Lab_Xin_Wang/$project_name/WES/Run_template/preprocessed
#mkdir PI_Lab_Xin_Wang/$project_name/WES/Run_template/preprocessed/documentation
#mkdir PI_Lab_Xin_Wang/$project_name/WES/Run_template/raw
#mkdir PI_Lab_Xin_Wang/$project_name/WES/Run_template/raw/documentation
mkdir PI_Lab_Xin_Wang/$project_name/WES/processed
mkdir PI_Lab_Xin_Wang/$project_name/WES/processed/documentation

cat >PI_Lab_Xin_Wang/$project_name/WES.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Platform"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/WES/processed.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Processed_Data"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/WES/processed/documentation.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Documentation"
    }
  ]
}
EOF

cat >PI_Lab_Xin_Wang/$project_name/RNASeq.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Platform"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/RNASeq/processed.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Processed_Data"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/RNASeq/processed/documentation.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Documentation"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/analysis.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Analysis"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/analysis/documentation.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Documentation"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name/metadata.metadata.json <<EOF
{
  "metadataEntries": [
    {
      "attribute": "collection_type",
      "value": "Metadata"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang.metadata.json <<EOF
{
  "metadataEntries": [{
      "attribute": "pi_name",
      "value": "Xin Wei Wang"
    },
    {
      "attribute": "affiliation",
      "value": "Laboratory of Human Carcinogenesis"
    },
    {
      "attribute": "collection_type",
      "value": "PI_Lab"
    }
  ]
}
EOF
cat >PI_Lab_Xin_Wang/$project_name.metadata.json <<EOF
{
  "metadataEntries": [{
      "attribute": "project_title",
      "value": "Placeholder for project_title"
    },
    {
      "attribute": "project_description",
      "value": "Placeholder for project_description"
    },
    {
      "attribute": "origin",
      "value": "Placeholder for origin"
    },
    {
      "attribute": "method",
      "value": "Placeholder for method"
    },
    {
      "attribute": "start_date",
      "value": "Placeholder for start_date"
    },
    {
      "attribute": "summary_of_datasets",
      "value": "Placeholder for summary_of_datasets"
    },
    {
      "attribute": "organism",
      "value": "Placeholder for organism"
    },
    {
      "attribute": "access",
      "value": "Closed Access"
    },
    {
      "attribute": "collection_type",
      "value": "Project"
    }
  ]
}
EOF

cat >./EXCLUDE_FILE_PATH.txt <<EOF
**.metadata.json
EOF

dm_register_directory -e EXCLUDE_FILE_PATH.txt PI_Lab_Xin_Wang /LHC_LCS_Archive/PI_Lab_Xin_Wang

