"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useState, useCallback, useContext } from "react";
import { useMemo } from 'react';
import { themeQuartz, AllCommunityModule, ModuleRegistry } from "ag-grid-community";
import { GridContext } from './GridContext';

ModuleRegistry.registerModules([AllCommunityModule]);



const GridComponent = () => {
  const {rowData, setRowData, gridApi, setGridApi, setSelectedRows, absolutePath, setAbsolutePath} = useContext(GridContext);
  const [relativePath, setRelativePath] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const handleSpanClick = (event) => {
    setAbsolutePath(event.currentTarget.id);
  };

  function folderNameRenderer(props) {
    return (
        <>
          {!props.data.isDirectory ?
              <span> {props.data.name}</span>
              :
              <span id={props.data.path} role="button" onClick={handleSpanClick} >
              <i className="icon_folder me-2"></i>
                {props.data.name}
            </span>
          }
        </>
    );
  };

  const archivedValueFormatter = params => {
    if (params.value === true) {
      return 'Yes';
    } else if (params.value === false) {
      return 'No';
    }
    return ''; // Handle null/undefined values
  };

  const isoDateFormatter = params => {
    if (params.value === null || params.value === undefined)
      return ''; // Handle null/undefined values
    const isoString = params.value;
    const dateObject = new Date(isoString);

    return dateObject.toLocaleString();
  };

  function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '-';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }

  const myTheme = themeQuartz.withParams({
    backgroundColor: '#ffffff',
    foregroundColor: '#797979',
    headerTextColor: '#ffffff',
    headerBackgroundColor: '#4c89cb',
  });

  const [columnDefs, setColumnDefs] = useState([
    { headerName: 'Name', field: "name", filter: true,
      cellRenderer: folderNameRenderer
    },
    { headerName: 'Size', field: "size",
      valueFormatter: (params) => formatBytes(params.value),
      cellDataType: 'number', filter: true,
    },
    { headerName: 'Date Created', field: "created", valueFormatter: isoDateFormatter },
    { headerName: 'Date Modified', field: "lastModified", valueFormatter: isoDateFormatter },
    { headerName: 'Archived', field: "archived",
      cellDataType: 'object', filter: true,
      valueFormatter: archivedValueFormatter,
    }
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
    const url = process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/global/list?path=';
    const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';

    if(!useExternalApi) {
      fetch("/folders.json") // Fetch data from server
          .then((result) => result.json()) // Convert to JSON
          .then((data) => {
            setRelativePath(data.path.split("/").pop());
            return data.contents;
          })
          .then((rowData) => setRowData(rowData));
    } else {
      const fetchData = async () => {
        try {
          const response = await fetch(url + absolutePath, {
            credentials: 'include',
          });
          if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
          }
          const json = await response.json();
          setRelativePath(json.path.split("/").pop() + '/');
          return json.contents;
        } catch (e) {
          setError(e);
        } finally {
          setLoading(false);
        }
      }
      fetchData().then((rowData) => setRowData(rowData));
    }
  }, [absolutePath,setRelativePath,setRowData]);

  return (
      <>
        <h3>{relativePath}</h3>
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
      </>
  );
};

export default GridComponent;