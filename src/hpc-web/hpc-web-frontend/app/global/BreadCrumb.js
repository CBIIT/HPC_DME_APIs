//BreadCrumb.js
"use client";


import { useContext, useEffect, useState } from "react";
import { GridContext } from "./GridContext";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faAngleRight } from "@fortawesome/free-solid-svg-icons";
import { useRouter, useSearchParams } from "next/navigation";


const BreadCrumb = () => {

    const {absolutePath, basePath } = useContext(GridContext);
    const [folder, setFolder] = useState(null);
    const [tokens, setTokens] = useState([]);
    const [fullPaths, setFullPaths] = useState([]);
    const router = useRouter();
    const searchParams = useSearchParams();
    const url = process.env.NEXT_PUBLIC_DME_WEB_URL === '' ?  '/global.html' : '/global';

    useEffect(() => {
        if(!absolutePath || !basePath) {
            return;
        }
        let pathAfterBasePath = absolutePath.slice(basePath.length);
        let segments = pathAfterBasePath.split('/').filter(segment => segment !== '');
        let linkPaths = [];
        setFolder(segments.pop());
        for (let i = 0; i < segments.length; i++) {
            linkPaths.push(basePath + '/' + segments.slice(0, i + 1).join('/'));
        }
        setTokens([]);
        setFullPaths([]);
        setTokens(segments);
        setFullPaths(linkPaths);
    }, [absolutePath]);

    const handleBreadCrumbClick = (event) => {
        const currentParams = new URLSearchParams(searchParams.toString());
        currentParams.set('path', event.currentTarget.id);
        router.push(url + `?${currentParams.toString()}`);
    };

    return (
        <div className="col pt-3">
            <ol className="dme-breadcrumb">
                <a id={basePath} href="#" onClick={handleBreadCrumbClick}
                   className="text-blue-600 hover:text-blue-600 text-break">
                    {basePath}
                </a>
                <span className="ms-3 me-3"><FontAwesomeIcon icon={faAngleRight} /></span>
                {tokens.map((token, i) => (
                    <li key={i} className="list-unstyled">
                        <a id={fullPaths[i]} href="#" onClick={handleBreadCrumbClick}
                           className="text-blue-600 hover:text-blue-600 text-break">
                            {token}
                        </a>
                        <span className="m-3" ><FontAwesomeIcon icon={faAngleRight} /></span>
                    </li>
                ))}
                <span id={folder}>
                    {folder}
                </span>
            </ol>
        </div>
    );
};

export default BreadCrumb;