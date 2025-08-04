"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useState, useCallback, useContext } from "react";
import { useMemo } from 'react';
import { themeQuartz, AllCommunityModule, ModuleRegistry } from "ag-grid-community";
import { GridContext } from './GridContext';

ModuleRegistry.registerModules([AllCommunityModule]);


function folderNameRenderer(props) {
  return (
      <span>
      <i className="icon_folder me-2"></i>
      {props.data.name}
      </span>
  );
}

const GridComponent = () => {
  const [rowData, setRowData] = useState([]);
  const {gridApi, setGridApi, setSelectedRows} = useContext(GridContext);

  const myTheme = themeQuartz.withParams({
    backgroundColor: '#ffffff',
    foregroundColor: '#797979',
    headerTextColor: '#ffffff',
    headerBackgroundColor: '#4c89cb',
  });

  const [columnDefs, setColumnDefs] = useState([
    { headerName: 'Name', field: "name",
      cellRenderer: folderNameRenderer
    },
    { headerName: 'Size', field: "size" },
    { headerName: 'Date Created', field: "created" },
    { headerName: 'Date Modified', field: "lastModified" },
    { headerName: 'Archived', field: "archived" },
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

  const onGridReady = useCallback((params) => {
    setGridApi(params.api); // Store the AG Grid API in global context
  }, [setGridApi]);

  const onSelectionChanged = useCallback(() => {
    if (gridApi) {
      const selectedNodes = gridApi.getSelectedNodes();
      const selectedData = selectedNodes.map(node => node.data);
      setSelectedRows(selectedData);
    }
  }, [gridApi, setSelectedRows]);


  useEffect(() => {
    fetch("/folders.json") // Fetch data from server
      .then((result) => result.json()) // Convert to JSON
      .then((rowData) => setRowData(rowData)); // Update state of `rowData`
  }, []);

  return (
    <div style={{ width: "98%", height: "520px" }}>
      <AgGridReact rowData={rowData}
                   columnDefs={columnDefs}
                   rowSelection={rowSelection}
                   pagination={pagination}
                   paginationPageSize={paginationPageSize}
                   paginationPageSizeSelector={paginationPageSizeSelector}
                   theme={myTheme}
                   onGridReady={onGridReady}
                   onSelectionChanged={onSelectionChanged} />
    </div>
  );
};

export default GridComponent;