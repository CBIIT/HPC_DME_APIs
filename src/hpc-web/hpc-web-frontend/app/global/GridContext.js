import React, { createContext, useState } from 'react';

export const GridContext = createContext({
    gridApi: null,
    setGridApi: () => {},
    selectedRows: [],
    setSelectedRows: () => {}
});

export const GridProvider = ({ children }) => {
    const [gridApi, setGridApi] = useState(null);
    const [selectedRows, setSelectedRows] = useState([]);

    const value = {
        gridApi,
        setGridApi,
        selectedRows,
        setSelectedRows
    };

    return (
        <GridContext.Provider value={value}>
            {children}
        </GridContext.Provider>
    );
};