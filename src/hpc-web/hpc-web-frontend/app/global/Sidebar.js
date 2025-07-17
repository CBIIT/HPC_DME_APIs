//Sidebar.js
"use client";
import {faDownload, faCaretDown} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from "@fortawesome/react-fontawesome";

const Sidebar = ({isOpen, toggleSidebar}) => {

    return (
        <div className="flex">
            <div className="row mb-3">
                {/* Button to toggle sidebar */}
                <div className={`${isOpen ? '' : 'col-md-2 d-flex flex-row '}`}>
                    <div className={`${isOpen ? 'col-md-2' : ''}`}>
                        <button type="button" className="btn btn-primary"
                                onClick={() => toggleSidebar()}>
                            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="currentColor"
                                 className="bi bi-list" viewBox="0 0 16 16">
                                <path fillRule="evenodd"
                                      d="M2.5 12a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5m0-4a.5.5 0 0 1 .5-.5h10a.5.5 0 0 1 0 1H3a.5.5 0 0 1-.5-.5"></path>
                            </svg>
                        </button>
                    </div>
                    <div className={`${isOpen ? '' : 'col-md-2'}`}>
                        <a href="#"
                           className="text-blue-600
                              hover:text-blue-600 ">
                            /data/CMM
                        </a>
                    </div>
                </div>
                <div className="col">
                    <div className="pull-right">
                        <button type="button" className="btn btn-primary me-2">
                            <span className="me-2">Actions</span>
                            <FontAwesomeIcon icon={faCaretDown} />
                        </button>

                        <button type="button" className="btn btn-primary">
                            <FontAwesomeIcon icon={faDownload} />
                            <span className="ms-2">Download</span>
                        </button>
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