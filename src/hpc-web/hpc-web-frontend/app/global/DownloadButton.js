"use client";

import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faDownload } from "@fortawesome/free-solid-svg-icons";
import React, { useContext } from 'react';
import { GridContext } from './GridContext';

export default function DownloadButton() {
    const { selectedRows } = useContext(GridContext);

	const normalizePath = (path) => {
	  if (!path) return path;
	  return path.replace(/\\/g, '/');
	};

    const handleDownload = async () => {
        if (selectedRows.length > 1 ) {
			console.log('More than 1 row selected.');
		} else if (selectedRows.length === 0) {
			console.log('No row selected.');
		} else if (!selectedRows[0].isDirectory) {
            const selectedRowData = selectedRows[0];
            const url = selectedRowData.archived ?
                '/download?type=datafile&downloadFilePath=' + normalizePath(selectedRowData.archivePath) :
                '/download?ext=true&type=datafile&downloadFilePath=' + normalizePath(selectedRowData.path);
            window.open(url, '_blank', 'noopener noreferrer');
            console.log('Selected row data:', selectedRowData);

		} else if (selectedRows[0].isDirectory){
			alert('Downloading directories is not supported. Please select a file to download.');
		}
		
        console.log("Download clicked");
    };

    return (
    <div className="col-sm-2">
        {/* Download button */}
        <button onClick={handleDownload} type="button" className="btn btn-lg btn-primary form-control">
            <FontAwesomeIcon icon={faDownload} />
            <span className="ms-2">Download</span>
        </button>
    </div>);
}