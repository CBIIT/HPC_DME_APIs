"use client";
import Link from "next/link";
import Image from "next/image";
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import {faGear} from '@fortawesome/free-solid-svg-icons';
import {useSession} from "./SessionContext";

export default function Header() {
    const session = useSession();
    const versionString = session?.env === "PROD" ? `version: ${session?.version}` : `version: ${session?.version} (${session?.env})`;
    const isLowerTier = session?.env != "PROD";
    const isCurator = session?.hpcUser?.dataCurator;
    const isAdmin = session?.hpcUser?.userRole === "SYSTEM_ADMIN";
    const isGroupAdmin = session?.hpcUser?.userRole === "GROUP_ADMIN";
    const browseUrl = process.env.NEXT_PUBLIC_DME_WEB_URL === '' ?  '/global.html' : '/global';


    return (
        <header>
            <div className="col order-first" style={{background: "#fff"}}>
                <Image
                    src="img/nci_logo.png"
                    alt="National Cancer Institute"
                    width={319}
                    height={52} // Desired height
                    priority // Optional: for above-the-fold images
                />
                <nav className="nav navbar navbar-expand-md" id="appnavbar">
                    <div id="toolTitle" className="container-fluid">
                        <div className="navbar-header container-fluid">
                            <Link className="logo" href={"/"}>Data Management Environment</Link>
                            <button type="button" className="navbar-toggler" data-bs-toggle="collapse"
                                    data-bs-target="#appNav"
                                    aria-expanded="false">
                                <span className="navbar-dark navbar-toggler-icon"></span>
                            </button>
                        </div>
                        <div id="appNav" className="col collapse navbar-collapse">
                            <ul className="nav top-menu top-nav pull-right">
                                <li className="nav-item">
                                    <a data-bs-toggle="dropdown" className="dropdown-toggle" href="#">
                                        <i className="icon_profile"></i>
                                        <span
                                            className="username">{session?.hpcUser?.firstName} {session?.hpcUser?.lastName}</span></a>
                                    <ul className="dropdown-menu extended logout">
                                        <div className="log-arrow-up"></div>
                                        <li className="eborder-top"><a href="/profile"><i
                                            className="icon_profile"></i> My Profile</a></li>
                                        <li><a href="/logout"><i className="icon_arrow_triangle_right_alt"></i> Log
                                            Out</a></li>
                                    </ul>
                                </li>
                                <li className="nav-item">
                                    <a data-bs-toggle="dropdown" className="dropdown-toggle" href="#"><i
                                        className="icon_book_alt"></i><span
                                        className="topNav-item">Help</span></a>
                                    <ul className="dropdown-menu extended logout">
                                        <div className="log-arrow-up"></div>
                                        <li className="eborder-top"><a
                                            href="https://wiki.nci.nih.gov/display/DMEdoc/Frequently+Asked+Questions"
                                            target="_blank"><i className="icon_question_alt"></i>Frequently Asked
                                            Questions</a></li>
                                        <li className="eborder-top"><a
                                            href="https://wiki.nci.nih.gov/display/DMEdoc" target="_blank"><i
                                            className="icon_book_alt"></i>User Guide</a></li>
                                        <li><a href="/swagger-ui/index.html" target="_blank"><Image
                                            src="img/swagger.svg" height = "13" width="13" alt="Swagger"/>&nbsp;&nbsp;&nbsp;&nbsp;API
                                            Specification</a></li>
                                        <li><a href="https://github.com/CBIIT/HPC_DME_APIs" target="_blank"><i
                                            className="icon_key_alt"></i>DME Site on GitHub</a></li>
                                    </ul>
                                </li>
                                <li className="nav-item versionNumber"><a href="#">
                                    <span>{versionString}</span>
                                </a></li>
                            </ul>
                        </div>
                    </div>
                </nav>
            </div>

            <nav className="nav navbar navbar-expand-md pull-left" id="menunavbar">
                <div className="container-fluid">
                    <div className="navbar-header">
                        <button type="button" className="navbar-toggler" data-bs-toggle="collapse"
                                data-bs-target="#topNav"
                                aria-expanded="false">
                            <span className="navbar-dark navbar-toggler-icon"></span>
                        </button>
                    </div>
                    <div id="topNav" className="collapse navbar-collapse">
                        <ul className="nav navbar-nav">
                            <li id="sub-menu-dashboard" className="sub-menu"><a href="/dashboard"> <i
                                className="icon_house_alt"></i> <span>Dashboard</span>
                            </a></li>
                            <li id="sub-menu-register" className="sub-menu"><a href="javascript:;"
                                                                               className="dropdown-toggle"
                                                                               data-bs-toggle="dropdown" role="button"
                                                                               aria-haspopup="true"
                                                                               aria-expanded="false">
                                <i
                                    className="icon_document_alt"></i> <span>Register</span> <span
                                className="menu-arrow arrow_carrot-right"></span>
                            </a>
                                <ul className="sub dropdown-menu" aria-labelledby="register_dropdown">
                                    <li><a href="/addCollection?init">Collection</a></li>
                                    <li><a href="/addDatafile?init">Data File</a></li>
                                    <li><a href="/addbulk?init">Bulk</a></li>
                                </ul>
                            </li>
                            <li id="sub-menu-search" className="sub-menu"><a href="/criteria"> <i
                                className="icon_search"></i> <span>Search</span>
                            </a></li>
                            <li id="sub-menu-browse" className="sub-menu"><a href="javascript:;"
                                                                             className="dropdown-toggle"
                                                                             data-bs-toggle="dropdown" role="button"
                                                                             aria-haspopup="true"
                                                                             aria-expanded="false">
                                <i
                                    className="icon_folder"></i> <span>Browse</span><span
                                className="menu-arrow arrow_carrot-right"></span>
                            </a>
                                <ul className="sub dropdown-menu" aria-labelledby="browse_dropdown">
                                    <li><a href="/browse?base">NCI Data Vault</a></li>
                                    <li><a href={browseUrl}>External Archive</a></li>
                                </ul></li>
                            <li id="sub-menu-manage" className="sub-menu"><a href="javascript:;"
                                                                             className="dropdown-toggle"
                                                                             data-bs-toggle="dropdown" role="button"
                                                                             aria-haspopup="true" aria-expanded="false">
                                <FontAwesomeIcon icon={faGear}/>
                                <span className="m-1">Manage</span>
                                <span
                                    className="menu-arrow arrow_carrot-right"></span>
                            </a>
                                <ul className="sub dropdown-menu" aria-labelledby="manage_dropdown">
                                    <li><a href="/subscribe">Notifications</a></li>
                                    <li><a href="/downloadtasks">Download Tasks</a></li>
                                    <li><a href="/uploadtasks">Registration Tasks</a></li>
                                    {isLowerTier && isCurator ? <li><a href="/review">Review</a></li> : null}
                                </ul>
                            </li>
                            { isAdmin || isGroupAdmin ?
                            <li id="sub-menu-reports" className="sub-menu">
                                <a href="/reports"> <i
                                    className="icon_document_alt"></i> <span>Reports</span>
                                </a>
                            </li> : null }
                            { isAdmin || isGroupAdmin ?
                            <li id="sub-menu-admin" className="sub-menu">
                                <a href="#" className="dropdown-toggle" data-bs-toggle="dropdown" role="button"
                                   aria-haspopup="true" aria-expanded="false">
                                    <i className="icon_lock_alt m-1"></i>
                                    <span>Admin</span>
                                    <span className="menu-arrow arrow_carrot-right"></span>
                                </a>
                                <ul className="sub dropdown-menu" aria-labelledby="admin_dropdown">
                                    <li><a href="/user">User</a></li>
                                    <li><a href="/group">Group</a></li>
                                    {isAdmin ? <li><a href="/doc">DOC</a></li> : null }
                                    {isLowerTier && isAdmin ? <li><a href="/review">Review</a></li> : null}
                                </ul>
                            </li> : null }
                        </ul>
                    </div>
                </div>
            </nav>
        </header>
    )
        ;
}