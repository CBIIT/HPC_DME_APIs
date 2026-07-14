"use client";
import {useEffect, createContext, useContext, useState} from "react";

const SessionContext = createContext(null);

export default function SessionProvider({children}) {
    const [session, setSession] = useState();
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState(null);
    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    useEffect(() => {
        const sessionUrl = process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/sessionMap';
        const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';

        if(useExternalApi) {
            const fetchData = async () => {
                let redirectToLogin = false;
                try {
                    const response = await fetch(sessionUrl, {
                        credentials: 'include',
                    });

                    // Interceptor issues a 302 to /login when session is expired or not authenticated.
                    // fetch follows the redirect automatically; detect it by checking response.redirected + final URL.
                    if (response.redirected && response.url.includes('/login')) {
                        redirectToLogin = true;
                        return;
                    }

                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }

                    const json = await response.json();

                    // No authenticated user in session map → treat as expired/unauthenticated.
                    if (!json.hpcUserId) {
                        redirectToLogin = true;
                        return;
                    }

                    setSession(json);
                } catch (e) {
                    setMessage(e.message);
                    console.error("Fetch session info: ", e);
                } finally {
                    if (redirectToLogin) {
                        // Keep loading=true so children never flash before the redirect completes.
                        window.location.href = '/login';
                    } else {
                        setLoading(false);
                    }
                }
            }

            fetchData();
        } else {
            setLoading(false);
        }
        const storedSidebarSession = localStorage.getItem('sidebarSession');
        if (storedSidebarSession) {
            setIsSidebarOpen(storedSidebarSession === 'true');
        }
    }, []); // Empty dependency array ensures this runs only once after initial render

    if (loading) {
        return <p>Loading...</p>;
    }

    const saveSidebarSession = (newSidebarSession) => {
        setIsSidebarOpen(newSidebarSession);
        localStorage.setItem('sidebarSession', newSidebarSession ? 'true' : 'false');
    };

    return (
        <SessionContext.Provider value={{session, loading, message, setMessage, isSidebarOpen, saveSidebarSession}}>
            {children}
        </SessionContext.Provider>
    );
}

export function useSession() {
    const context = useContext(SessionContext);
    if (!context) {
        throw new Error("useSession must be used within a SessionProvider");
    }
    return context.session;
}

export function useSessionContext() {
    const context = useContext(SessionContext);
    if (!context) {
        throw new Error("SessionContext must be used within a SessionProvider");
    }
    return context;
}

