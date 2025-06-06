"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useState } from "react";
import { useMemo } from 'react';
import { AllCommunityModule, ModuleRegistry } from "ag-grid-community";

ModuleRegistry.registerModules([AllCommunityModule]);



const GridComponent = () => {
  const [rowData, setRowData] = useState([]);

  const [columnDefs, setColumnDefs] = useState([
    { field: "name" },
    { field: "size" },
    { field: "dateCreated" },
    { field: "dateModified" },
    { field: "storage" },
  ]);

  const rowSelection = useMemo(() => {
    return {
      mode: 'singleRow',
    };
  }, []);

  // enables pagination in the grid
  const pagination = true;

  // sets 10 rows per page (default is 100)
  const paginationPageSize = 10;

  // allows the user to select the page size from a predefined list of page sizes
  const paginationPageSizeSelector = [10, 20, 50, 100];


  useEffect(() => {
    fetch("/folders.json") // Fetch data from server
      .then((result) => result.json()) // Convert to JSON
      .then((rowData) => setRowData(rowData)); // Update state of `rowData`
  }, []);

  return (
    <div style={{ width: "90%", height: "500px" }}>
      <AgGridReact rowData={rowData}
                   columnDefs={columnDefs}
                   rowSelection={rowSelection}
                   pagination={pagination}
                   paginationPageSize={paginationPageSize}
                   paginationPageSizeSelector={paginationPageSizeSelector}/>
    </div>
  );
};

export default GridComponent;