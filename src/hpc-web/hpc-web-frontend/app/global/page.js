"use client";
import { useSession } from "../SessionContext";
import {GridContext, GridProvider} from './GridContext';
import GridComponent from "./GridComponent";
import Sidebar from "./Sidebar";
import {useContext, useState} from 'react';

export default function Global() {
    const session = useSession();

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    return (
        <div>
            <GridProvider>
                <Sidebar isOpen={isSidebarOpen} toggleSidebar={toggleSidebar}/>
                <div
                    className={`${isSidebarOpen ? 'offset-md-3' : ''}`}
                    style={{
                        transition: 'margin-left 0.3s ease-in-out',
                        flexGrow: 1
                    }}
                >
                    <section className="bg-white">
                        <GridComponent />
                    </section>
                </div>
            </GridProvider>
        </div>
);
}
