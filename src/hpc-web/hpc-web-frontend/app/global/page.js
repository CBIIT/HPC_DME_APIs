"use client";
import { GridProvider } from './GridContext';
import GridComponent from "./GridComponent";
import Sidebar from "./Sidebar";
import ActionsBar from "./ActionsBar";
import { useSessionContext } from '../SessionContext';
import ErrorAlert from '../ErrorAlert';

export default function Global() {

    const {isSidebarOpen, saveSidebarSession} = useSessionContext();
    const {
        message, setMessage
    } = useSessionContext();

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
