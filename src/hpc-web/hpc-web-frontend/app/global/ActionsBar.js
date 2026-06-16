//ActionsBar.js
"use client";

import ActionsButton from "./ActionsButton";
import DownloadButton from "./DownloadButton";
import { faSearch, faFilter } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {useState, useContext, useEffect} from "react";
import { GridContext } from "@/app/global/GridContext";
import { useSearchParams } from "next/navigation";


const ActionsBar = ({isOpen}) => {

    const {gridApi, absolutePath, setAbsolutePath } = useContext(GridContext);
    const searchParams = useSearchParams();
    const [browseTextValue, setBrowseTextValue] = useState('');
    const [filterTextValue, setFilterTextValue] = useState('');
    const url = '/global';

    const navigateToGlobal = (params) => {
        const queryString = params?.toString();
        window.location.href = queryString ? `${url}?${queryString}` : url;
    };

    useEffect(() => {
        if(gridApi) {
            gridApi.setGridOption(
                "quickFilterText",
                filterTextValue,
            );
        }
    }, [gridApi, filterTextValue]);

    const onBrowseTextBoxChanged = () => {
        const path = browseTextValue;
        console.log("Browse to path: ", path);
        if(path === '') {
            console.log("Path is empty");
        } else {
            const currentParams = new URLSearchParams(searchParams.toString());
            currentParams.set('path', path);
            navigateToGlobal(currentParams);
        }
    };

    const handleBrowseTextKeyDown = (event) => {
        if (event.key === 'Enter') {
            onBrowseTextBoxChanged();
        }
    };

    const handleBrowseTextChange = (event) => {
        setBrowseTextValue(event.target.value);
    };

    const handleBrowseTextClear = () => {
        setBrowseTextValue('');
    };

    const handleFilterTextClear = () => {
        setFilterTextValue('');
    };

    return (
        <div className="flex">
            <div className="row mb-3">
                <div className="">
                    <div className="row action-row">
                        <div className={`${isOpen ? 'ps-0 col-sm-5 browse-text' : 'col-sm-5 browse-text'}`}>
                            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                                <FontAwesomeIcon icon={faSearch} style={{ position: 'absolute', left: '10px', color: '#ccc' }}/>
                                <input
                                    className="form-control"
                                    type="text"
                                    placeholder="Browse to Path"
                                    style={{ paddingLeft: '35px', width: '100%' }}
                                    id="browse-text-box"
                                    onKeyDown={handleBrowseTextKeyDown}
                                    value={browseTextValue}
                                    onChange={handleBrowseTextChange}
                                />
                                {browseTextValue && ( // Conditionally render the clear button
                                    <button className="clear-button" onClick={handleBrowseTextClear}>
                                        x
                                    </button>
                                )}
                            </div>
                        </div>
                        <div className="col-sm-3 filter-text">
                            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                                <FontAwesomeIcon icon={faFilter} style={{ position: 'absolute', left: '10px', color: '#ccc' }}/>
                                <input
                                    className="form-control"
                                    type="text"
                                    placeholder="Filter"
                                    style={{ paddingLeft: '35px', width: '100%' }}
                                    id="filter-text-box"
                                    value={filterTextValue}
                                    onChange={e => setFilterTextValue(e.target.value)}
                                />
                                {filterTextValue && ( // Conditionally render the clear button
                                    <button className="clear-button" onClick={handleFilterTextClear}>
                                        x
                                    </button>
                                )}
                            </div>
                        </div>
                        <ActionsButton/>
                        <DownloadButton/>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default ActionsBar;