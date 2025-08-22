//Sidebar.js
"use client";

import {useContext, useEffect, useState} from "react";
import {GridContext} from "./GridContext";
import BreadCrumb from "./BreadCrumb";


const Sidebar = ({isOpen, toggleSidebar}) => {

    const {setBasePath, setAbsolutePath } = useContext(GridContext);
    const [archives, setArchives] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const handleArchiveClick = (event) => {
        setAbsolutePath(event.currentTarget.id);
        setBasePath(event.currentTarget.id);
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
                    setBasePath(data[0] || '');
                });
        } else {
            const fetchData = async () => {
                try {
                    const response = await fetch(url, {
                        credentials: 'include',
                    });
                    if (!response.ok) {
                        const errorData = await response.text();
                        throw new Error(`HTTP error! status: ${response.status}, Message: ${errorData || 'Unknown error'}`);
                    }
                    const json = await response.json();
                    setArchives(json);
                    setAbsolutePath(json[0] || ''); // Set the first archive as the default path
                    setBasePath(json[0] || '');
                } catch (e) {
                    setError(e);
                    console.error("Fetch external archives list:", e);
                } finally {
                    setLoading(false);
                }
            }
            fetchData();
        }
    }, [setAbsolutePath, setArchives, setBasePath]);

    return (
        <div className="flex">
            <div className="row mb-3">
                {/* Button to toggle sidebar */}
                <div className="col d-flex flex-row align-items-center">
                    <div>
                        <button type="button" className="btn btn-lg btn-primary"
                                onClick={() => toggleSidebar()}>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 className="bi bi-list" viewBox="0 0 16 16">
                                <path fillRule="evenodd"
                                      d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5"></path>
                            </svg>
                        </button>
                    </div>
                    <BreadCrumb />
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
                        <h3>
                        My Archives
                        </h3>
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
                </div>
            </div>
        </div>
    );
};

export default Sidebar;