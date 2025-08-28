//ActionsBar.js
"use client";

import ActionsButton from "./ActionsButton";
import DownloadButton from "./DownloadButton";
import { faSearch, faFilter } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import {useState, useCallback, useContext, useEffect} from "react";
import { GridContext } from "@/app/global/GridContext";
import { useRouter, useSearchParams } from "next/navigation";


const ActionsBar = ({isOpen}) => {

    const {gridApi, absolutePath, setAbsolutePath } = useContext(GridContext);
    const router = useRouter();
    const searchParams = useSearchParams();
    const [browseTextValue, setBrowseTextValue] = useState('');
    const [filterTextValue, setFilterTextValue] = useState('');
    const url = process.env.NEXT_PUBLIC_DME_WEB_URL === '' ?  '/global.html' : '/global';

    useEffect(() => {
        if(gridApi) {
            gridApi.setGridOption(
                "quickFilterText",
                filterTextValue,
            );
        }
    }, [gridApi, filterTextValue]);

    const onBrowseTextBoxChanged= async () => {
        const path = browseTextValue;
        console.log("Browse to path: ", path);
        if(path === '') {
            console.log("Path is empty");
        } else {
            const currentParams = new URLSearchParams(searchParams.toString());
            currentParams.set('path', path);
            router.push(url + `?${currentParams.toString()}`);
        }
    };

    const handleEnterPress = (event) => {
        setBrowseTextValue(document.getElementById("browse-text-box").value);
        if (event.key === 'Enter') {
            onBrowseTextBoxChanged();
        }
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
                                    onKeyDown={handleEnterPress}
                                    value={browseTextValue}
                                    onChange={handleEnterPress}
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