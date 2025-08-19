//Sidebar.js
"use client";

import ActionsButton from "./ActionsButton";
import DownloadButton from "./DownloadButton";
import {faSearch, faFilter} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";
import {useCallback, useContext, useEffect, useState} from "react";
import {GridContext} from "@/app/global/GridContext";


const Sidebar = ({isOpen, toggleSidebar}) => {

    const {gridApi, absolutePath, setAbsolutePath } = useContext(GridContext);
    const [archives, setArchives] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const handleArchiveClick = (event) => {
        setAbsolutePath(event.currentTarget.id);
    };

    const onFilterTextBoxChanged = useCallback(() => {
        gridApi.setGridOption(
            "quickFilterText",
            (document.getElementById("filter-text-box")).value,
        );
    }, [gridApi]);


    const onBrowseTextBoxChanged= async () => {
        const path = document.getElementById("browse-text-box").value;
        console.log("Browse to path: ", path);
        if(path === '') {
            console.log("Path is empty");
        } else {
            // Fetch data from server with the new path
            setAbsolutePath(path);
        }
    };

    const handleEnterPress = (event) => {
        if (event.key === 'Enter') {
            onBrowseTextBoxChanged();
        }
    };


    useEffect(() => {
        const url = process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/global/externalArchives';
        const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';

        if(!useExternalApi) {
            fetch("/archives.json") // Fetch data from server
                .then((result) => result.json()) // Convert to JSON
                .then((data) => {
                    setArchives(data);
                    setAbsolutePath(data[0] || '');
                });
        } else {
            const fetchData = async () => {
                try {
                    const response = await fetch(url, {
                        credentials: 'include',
                    });
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    const json = await response.json();
                    setArchives(json);
                    setAbsolutePath(json[0] || ''); // Set the first archive as the default path
                } catch (e) {
                    setError(e);
                } finally {
                    setLoading(false);
                }
            }
            fetchData();
        }
    }, [setAbsolutePath, setArchives]);

    return (
        <div className="flex">
            <div className="row mb-3">
                {/* Button to toggle sidebar */}
                <div className="col-md-5 d-flex flex-row align-items-center">
                    <div className="m-2">
                        <button type="button" className="btn btn-lg btn-primary"
                                onClick={() => toggleSidebar()}>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 className="bi bi-list" viewBox="0 0 16 16">
                                <path fillRule="evenodd"
                                      d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5"></path>
                            </svg>
                        </button>
                    </div>
                    <div className="col">
                        Path: <a href="#"
                           className="text-blue-600
                              hover:text-blue-600 text-break">
                            {absolutePath}
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
                                    onKeyDown={handleEnterPress}
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
                className={`${isOpen ? 'col-sm-3 col-md-3 h-100 sidebar' : 'd-none'
                }`}>
                {/* Sidebar content */}
                <div className="flex flex-col items-center">
                    <div className="mt-4">
                        <p>
                        My Archives
                        </p>
                    </div>
                    <div className="mt-4">
                        <ul className="p-0">
                            {archives.map((archive, i) => (
                                <li key={i} className="list-unstyled">
                                <i className="icon_folder me-2"></i>
                                <a id={archive} onClick={handleArchiveClick} href="#"
                                   className="text-blue-600 hover:text-blue-600 ">
                                    {archive}
                                </a>
                                </li>
                            ))}
                        </ul>
                    </div>
                    {/* Add more sidebar items here */}
                </div>
            </div>
        </div>
    );
};

export default Sidebar;