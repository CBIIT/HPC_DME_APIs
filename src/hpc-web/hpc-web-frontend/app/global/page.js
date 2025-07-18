"use client";
import Link from "next/link";
import { useSession } from "../SessionContext";
import { SelectedRowsProvider } from './SelectedRowsContext';
import GridComponent from "./GridComponent";
import Sidebar from "./Sidebar";
import { useState } from 'react';

export default function Global() {
    const session = useSession();

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    const toggleSidebar = () => {
        setIsSidebarOpen(!isSidebarOpen);
    };

    return (
        <div>
            <SelectedRowsProvider>
                <Sidebar isOpen={isSidebarOpen} toggleSidebar={toggleSidebar}/>
                <div
                    className={`${isSidebarOpen ? 'offset-md-2' : ''}`}
                    style={{
                        transition: 'margin-left 0.3s ease-in-out',
                        flexGrow: 1
                    }}
                >
                    <section className="bg-white">
                        <h3>CMM/</h3>
                        <GridComponent/>
                    </section>
                </div>
            </SelectedRowsProvider>
        </div>
);
}
