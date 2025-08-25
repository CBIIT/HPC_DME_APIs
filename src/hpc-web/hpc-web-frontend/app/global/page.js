"use client";
import { GridProvider } from './GridContext';
import GridComponent from "./GridComponent";
import Sidebar from "./Sidebar";
import ActionsBar from "./ActionsBar";
import { useState } from 'react';


export default function Global() {

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    return (
        <div>
            <GridProvider>
                <Sidebar isOpen={isSidebarOpen} toggleSidebar={toggleSidebar}/>
                <ActionsBar isOpen={isSidebarOpen} />
                <div
                    className={`${isSidebarOpen ? 'offset-md-3' : ''}`}
                    style={{
                        transition: 'margin-left 0.3s ease-in-out',
                        flexGrow: 1
                    }}
                >
                    <section className="bg-white rounded-3">
                        <GridComponent />
                    </section>
                </div>
            </GridProvider>
        </div>
);
}
