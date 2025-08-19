"use client";

import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {faCaretDown} from "@fortawesome/free-solid-svg-icons";
import {useContext} from "react";
import {GridContext} from "./GridContext";

export default function ActionsButton() {
    const { selectedRows } = useContext(GridContext);
    const handleCopyPath = async () => {
        if (selectedRows.length > 0) {
            const selectedRowData = selectedRows[0];
            console.log('Selected row data:', selectedRowData);
            navigator.clipboard.writeText(selectedRowData.path);
        } else {
            console.log('No row selected.');
        }
    };

    const handleCalculateTotalSize= async () => {
        if (selectedRows.length > 0) {
            const selectedRowData = selectedRows[0];
            const url = process.env.NEXT_PUBLIC_DME_WEB_URL === '' ?
                '/usage.html?path=' + selectedRowData.path :
                '/usage?path=' + selectedRowData.path;
            window.open(url, '_blank', 'noopener noreferrer');
            console.log('Selected row data:', selectedRowData);
        } else {
            console.log('No row selected.');
        }
        console.log("Calculate Total Size clicked");
    };

    return (
        <div className="col-sm-2 dropdown">
            <button className="btn btn-lg btn-primary dropdown-toggle me-2 form-control" type="button" id="ActionButton" data-bs-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                <span className="me-2">Actions</span>
                <FontAwesomeIcon icon={faCaretDown} />
            </button>
            <div className="dropdown-menu" aria-labelledby="dropdownMenuButton">
                <a onClick={handleCopyPath} className="dropdown-item" href="#">Copy Path</a>
                <a onClick={handleCalculateTotalSize} className="dropdown-item" href="#">Calculate Total Size</a>
            </div>
        </div>
    );
}