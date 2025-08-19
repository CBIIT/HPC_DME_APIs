"use client";
import {useEffect, createContext, useContext, useState} from "react";

const SessionContext = createContext(null);

export default function SessionProvider({children}) {
    const [session, setSession] = useState();
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const sessionUrl = process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/sessionMap';
        const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';

        if(useExternalApi) {
            const fetchData = async () => {

                try {
                    const response = await fetch(sessionUrl, {
                        credentials: 'include',
                    });
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    const json = await response.json();
                    setSession(json);
                } catch (e) {
                    setError(e);
                    console.error("Fetch session info: ", e);
                } finally {
                    setLoading(false);
                }
            }

            fetchData();
        } else {
            setLoading(false);
        }
    }, []); // Empty dependency array ensures this runs only once after initial render

    if (loading) {
        return <p>Loading...</p>;
    }

    return (
        <SessionContext.Provider value={{session}}>
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
