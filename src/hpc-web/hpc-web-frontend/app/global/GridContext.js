import React, { createContext, useState } from 'react';

export const GridContext = createContext({
    rowData: [],
    setRowData: () => {},
    gridApi: null,
    setGridApi: () => {},
    absolutePath: null,
    setAbsolutePath: () => {},
    basePath: null,
    setBasePath: () => {},
    selectedRows: [],
    setSelectedRows: () => {}
});

export const GridProvider = ({ children }) => {
    const [rowData, setRowData] = useState([]);
    const [gridApi, setGridApi] = useState(null);
    const [absolutePath, setAbsolutePath] = useState(null);
    const [basePath, setBasePath] = useState(null);
    const [selectedRows, setSelectedRows] = useState([]);

    const value = {
        rowData,
        setRowData,
        gridApi,
        setGridApi,
        absolutePath,
        setAbsolutePath,
        basePath,
        setBasePath,
        selectedRows,
        setSelectedRows
    };

    return (
        <GridContext.Provider value={value}>
            {children}
        </GridContext.Provider>
    );
};