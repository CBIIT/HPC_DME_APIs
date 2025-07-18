import React, { createContext, useState } from 'react';

export const SelectedRowsContext = createContext({
    selectedRows: [],
    setSelectedRows: () => {}
});

export const SelectedRowsProvider = ({ children }) => {
    const [selectedRows, setSelectedRows] = useState([]);

    const value = {
        selectedRows,
        setSelectedRows,
    };

    return (
        <SelectedRowsContext.Provider value={value}>
            {children}
        </SelectedRowsContext.Provider>
    );
};