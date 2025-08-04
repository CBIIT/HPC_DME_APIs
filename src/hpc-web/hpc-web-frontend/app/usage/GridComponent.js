"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useState } from "react";
import { themeQuartz, AllCommunityModule, ModuleRegistry } from "ag-grid-community";

ModuleRegistry.registerModules([AllCommunityModule]);


const GridComponent = () => {
  const [rowData, setRowData] = useState([]);

  const myTheme = themeQuartz.withParams({
    backgroundColor: '#ffffff',
    foregroundColor: '#000000',
    headerTextColor: '#ffffff',
    headerBackgroundColor: '#ffffff',
  });

  const [columnDefs, setColumnDefs] = useState([
    { headerName: 'Path', field: "path", width: 400},
    { headerName: 'DME Size', field: "archiveSize" },
    { headerName: 'Source Size', field: "size" },
    { headerName: 'Total number of objects', field: "objectCount" },
    { headerName: 'Error', field: "error" },
  ]);


  useEffect(() => {
    fetch("/usage.json") // Fetch data from server
      .then((result) => result.json()) // Convert to JSON
      .then((rowData) => setRowData(rowData)); // Update state of `rowData`
  }, []);

  return (
    <div className="p-4" style={{ width: "100%", height: "123px" }}>
      <AgGridReact rowData={rowData}
                   columnDefs={columnDefs} />
    </div>
  );
};

export default GridComponent;