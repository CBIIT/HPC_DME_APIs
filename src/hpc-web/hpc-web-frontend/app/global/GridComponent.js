"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useState, useCallback, useContext } from "react";
import { useMemo } from 'react';
import { themeQuartz, AllCommunityModule, ModuleRegistry } from "ag-grid-community";
import { GridContext } from './GridContext';
import { faAngleLeft } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { useRouter, useSearchParams } from "next/navigation";

ModuleRegistry.registerModules([AllCommunityModule]);

const GridComponent = () => {
  const {
    rowData,
    setRowData,
    gridApi,
    setGridApi,
    setSelectedRows,
    basePath,
    setAbsolutePath
  } = useContext(GridContext);
  const [relativePath, setRelativePath] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [parentPath, setParentPath] = useState(null);
  const router = useRouter();
  const searchParams = useSearchParams();
  const url = process.env.NEXT_PUBLIC_DME_WEB_URL === '' ?  '/global.html' : '/global';


  const handleSpanClick = (event) => {
    const currentParams = new URLSearchParams(searchParams.toString());
    currentParams.set('path', event.currentTarget.id);
    router.push(url + `?${currentParams.toString()}`);
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

  function archivedFilterValueGetter(params) {
    if (params.data.archived === true) {
      return 'Yes';
    } else if (params.data.archived === false) {
      return 'No';
    }
    return 'No'; // Handle null/undefined values
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
    { headerName: 'Name', field: "name",
      cellRenderer: folderNameRenderer,
      filter: 'agTextColumnFilter',
      suppressMovable: true
    },
    { headerName: 'Size', field: "size",
      valueFormatter: (params) => formatBytes(params.value),
      cellDataType: 'number',
      suppressMovable: true
    },
    { headerName: 'Date Created', field: "created", valueFormatter: isoDateFormatter, suppressMovable: true },
    { headerName: 'Date Modified', field: "lastModified", valueFormatter: isoDateFormatter, suppressMovable: true },
    { headerName: 'Archived',
      filter: 'agTextColumnFilter',
      cellDataType: 'text',
      filterValueGetter: archivedFilterValueGetter,
      valueGetter: function(params) {
        if (params.data.archived && typeof params.data.archived === 'boolean') {
          return params.data.archived ? 'Yes' : 'No';
        }
        return 'No'; // Handle cases where data or field is not present/boolean
      },
      suppressMovable: true
    }
  ]);

  const gridOptions = {
    suppressMoveWhenColumnDragging: true,
    suppressDragLeaveHidesColumns: true,
    maintainColumnOrder: true,
  };

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

  const handleBackButtonClick = (event) => {
    const currentParams = new URLSearchParams(searchParams.toString());
    const path = event.currentTarget.id.length < basePath.length ? basePath : event.currentTarget.id;
    currentParams.set('path', path);
    router.push(url + `?${currentParams.toString()}`);
  };

  useEffect(() => {
    const url = process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/global/list?path=';
    const useExternalApi = process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true';
    const param = searchParams.get('path');
    setAbsolutePath(param);

    if(!useExternalApi) {
      fetch("/folders.json") // Fetch data from server
          .then((result) => result.json()) // Convert to JSON
          .then((data) => {
            setRelativePath(data.path.split("/").pop() + '/');
            setParentPath(data.path.substring(0, data.path.lastIndexOf("/")));
            return data.contents;
          })
          .then((rowData) => setRowData(rowData));
      setLoading(false);
    } else if(param !== null) {
      const fetchData = async () => {
        try {
          setLoading(true);
          if (gridApi) {
            gridApi.setGridOption("loading", true)
          }
          const response = await fetch(url + param, {
            credentials: 'include',
          });
          if (!response.ok) {
            const errorData = await response.text();
            throw new Error(`HTTP error! status: ${response.status}, Message: ${errorData || 'Unknown error'}`);
          }
          const json = await response.json();
          setRelativePath(json.path.split("/").pop() + '/');
          setParentPath(json.path.substring(0, json.path.lastIndexOf("/")));
          return json.contents;
        } catch (e) {
          setError(e);
          console.error("Fetch object list:", e);
        } finally {
          setLoading(false);
          if(gridApi) {
            gridApi.setGridOption("loading", false)
          }
        }
      }
      fetchData().then((rowData) => setRowData(rowData));
    }
  }, [searchParams]);

  return (
      <>

        <h3><a id={parentPath} href="#" onClick={handleBackButtonClick}><span className="m-3" ><FontAwesomeIcon icon={faAngleLeft} /></span></a>
          {relativePath}</h3>
        <div className="ps-3" style={{ width: "98%", height: "520px" }}>
          <AgGridReact gridOptions={gridOptions}
                       rowData={rowData}
                       columnDefs={columnDefs}
                       rowSelection={rowSelection}
                       pagination={pagination}
                       paginationPageSize={paginationPageSize}
                       paginationPageSizeSelector={paginationPageSizeSelector}
                       theme={myTheme}
                       onGridReady={onGridReady}
                       onSelectionChanged={onSelectionChanged}
                       loading={loading} />
        </div>
      </>
  );
};

export default GridComponent;