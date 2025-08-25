//ActionsBar.js
"use client";

import ActionsButton from "./ActionsButton";
import DownloadButton from "./DownloadButton";
import { faSearch, faFilter } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useCallback, useContext } from "react";
import { GridContext } from "@/app/global/GridContext";
import { useRouter, useSearchParams } from "next/navigation";


const ActionsBar = ({isOpen}) => {

    const {gridApi, absolutePath, setAbsolutePath } = useContext(GridContext);
    const router = useRouter();
    const searchParams = useSearchParams();

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
            const currentParams = new URLSearchParams(searchParams.toString());
            currentParams.set('path', path);
            router.push(`/global?${currentParams.toString()}`);
        }
    };

    const handleEnterPress = (event) => {
        if (event.key === 'Enter') {
            onBrowseTextBoxChanged();
        }
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
        </div>
    );
};

export default ActionsBar;