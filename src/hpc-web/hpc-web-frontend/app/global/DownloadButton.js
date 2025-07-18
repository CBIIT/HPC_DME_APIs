"use client";

import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faDownload} from "@fortawesome/free-solid-svg-icons";
import React, { useContext } from 'react';
import { SelectedRowsContext } from './SelectedRowsContext';

export default function DownloadButton() {
    const { selectedRows } = useContext(SelectedRowsContext);

    const handleDownload = async () => {
        if (selectedRows.length > 0) {
            const selectedRowData = selectedRows[0];
            console.log('Selected row data:', selectedRowData);
        } else {
            console.log('No row selected.');
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