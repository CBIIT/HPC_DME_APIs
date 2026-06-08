"use client";
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { GridProvider } from './GridContext';
import GridComponent from "./GridComponent";
import Sidebar from "./Sidebar";
import ActionsBar from "./ActionsBar";
import { useSessionContext } from '../SessionContext';
import ErrorAlert from '../ErrorAlert';

export default function Global() {
    const router = useRouter();
    const { session, loading, isSidebarOpen, saveSidebarSession, message, setMessage } = useSessionContext();
    const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';

    // Redirect to login if session has expired or user is not authenticated.
    useEffect(() => {
        if (!loading && useExternalApi && !session?.hpcUser) {
            router.replace('/login');
        }
    }, [loading, session, useExternalApi, router]);

    // Keep the page blank while the redirect is in progress.
    if (!loading && useExternalApi && !session?.hpcUser) {
        return null;
    }

    const toggleSidebar = () => {
        saveSidebarSession(!isSidebarOpen);
    };

    return (
        <div>
            {message && <ErrorAlert message={message} onClose={() => setMessage(null)} />}
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

