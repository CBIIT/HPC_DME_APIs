//Sidebar.js
"use client";

import ActionsButton from "./ActionsButton";
import DownloadButton from "./DownloadButton";
import {faSearch, faFilter} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {useCallback, useContext} from "react";
import {GridContext} from "@/app/global/GridContext";


const Sidebar = ({isOpen, toggleSidebar}) => {

    const { gridApi } = useContext(GridContext);

    const onFilterTextBoxChanged = useCallback(() => {
        gridApi.setGridOption(
            "quickFilterText",
            (document.getElementById("filter-text-box")).value,
        );
    }, [gridApi]);

    const onBrowseTextBoxChanged= async () => {
        const path = document.getElementById("browse-text-box").value;
        console.log("Browse to path: ", path);
    };

    return (
        <div className="flex">
            <div className="row mb-3">
                {/* Button to toggle sidebar */}
                <div className="col-md-5 d-flex flex-row align-items-center">
                    <div className={`${isOpen ? 'col-md-5' : ''}`}>
                        <button type="button" className="btn btn-lg btn-primary"
                                onClick={() => toggleSidebar()}>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 className="bi bi-list" viewBox="0 0 16 16">
                                <path fillRule="evenodd"
                                      d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5"></path>
                            </svg>
                        </button>
                    </div>
                    <div className={`${isOpen ? 'col' : 'col-md-5'}`}>
                        Path: <a href="#"
                           className="text-blue-600
                              hover:text-blue-600 ">
                            /data/CMM
                        </a>
                    </div>
                </div>
                <div className="col-md-7 d-flex">
                    <div className="row action-row">
                        <div className="col-sm-5 browse-text">
                            <div style={{ position: 'relative', display: 'flex', alignItems: 'center' }}>
                                <FontAwesomeIcon icon={faSearch} style={{ position: 'absolute', left: '10px', color: '#ccc' }}/>
                                <input
                                    className="form-control"
                                    type="text"
                                    placeholder="Browse to Path"
                                    style={{ paddingLeft: '35px', width: '100%' }}
                                    id="browse-text-box"
                                    onInput={onBrowseTextBoxChanged}
                                />
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
                                    onInput={onFilterTextBoxChanged}
                                />
                            </div>
                        </div>
                        <ActionsButton/>
                        <DownloadButton/>
                    </div>
                </div>
            </div>
            {/* Sidebar */}
            <div
                // Conditional class based on isOpen
                // state to control width and visibility
                className={`${isOpen ? 'col-sm-2 col-md-2 h-100 sidebar' : 'd-none'
                }`}>
                {/* Sidebar content */}
                <div className="flex flex-col items-center">
                    <div className="mt-4">
                        <p>
                        My Archives
                        </p>
                    </div>
                    <div className="mt-4">
                        <i className="icon_folder me-2"></i>
                        <a href="#"
                           className="text-blue-600
                          hover:text-blue-600 ">
                            /data/CMM
                        </a>
                    </div>
                    {/* Add more sidebar items here */}
                </div>
            </div>
        </div>
    );
};

export default Sidebar;