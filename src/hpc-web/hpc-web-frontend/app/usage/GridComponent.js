"use client";

import { AgGridReact } from 'ag-grid-react';
import { useEffect, useMemo, useState } from "react";
import { themeQuartz, AllCommunityModule, ModuleRegistry } from "ag-grid-community";
import { useSearchParams } from 'next/navigation';
import {useSessionContext} from "../SessionContext";

ModuleRegistry.registerModules([AllCommunityModule]);


const GridComponent = () => {
  const [rowData, setRowData] = useState([]);
  const [loading, setLoading] = useState(true);
  const {setMessage} = useSessionContext();
  const searchParams = useSearchParams();
  const usageApiUrl = useMemo(() => process.env.NEXT_PUBLIC_DME_WEB_URL + '/api/usage/calcTotalSize?path=', []);
  const useExternalApi = useMemo(() => process.env.NEXT_PUBLIC_DME_USE_EXTERNAL_API === 'true', []);

  function formatBytes(bytes, decimals = 2) {
    if (bytes === 0) return '-';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
  }
  
  const normalizePath = (path) => {
    if (!path) return path;
    return path.replace(/\\/g, '/');
  };

  const myTheme = themeQuartz.withParams({
    backgroundColor: '#ffffff',
    foregroundColor: '#000000',
    headerTextColor: '#ffffff',
    headerBackgroundColor: '#ffffff',
  });

  const [columnDefs, setColumnDefs] = useState([
    { headerName: 'Path', field: "path", width: 400},
    { headerName: 'Source Size', field: "size",
      valueFormatter: (params) => formatBytes(params.value),
      cellDataType: 'number'
    },
    { headerName: 'Source Count', field: "objectCount" },
    { headerName: 'Archived Size', field: "archiveSize",
      valueFormatter: (params) => formatBytes(params.value),
      cellDataType: 'number'
    },
    { headerName: 'Archived Count', field: "archiveCount" }
  ]);

  useEffect(() => {
    const param = normalizePath(searchParams.get('path'));

    if(!useExternalApi) {
      fetch("/usage.json") // Fetch data from server
        .then((result) => result.json()) // Convert to JSON
        .then((rowData) => setRowData(rowData)); // Update state of `rowData`
      setLoading(false);
    } else {
      const fetchData = async () => {
        try {
          const response = await fetch(usageApiUrl + param, {
            credentials: 'include',
          });
          if (!response.ok) {
            const errorData = await response.text();
            throw new Error(`HTTP error! status: ${response.status}, Message: ${errorData || 'Unknown error'}`);
          }
          const json = await response.json();
          return json.calculateTotalSizeResponse;
        } catch (e) {
          setMessage("Error fetching total size of " + param);
          console.error("Fetch total size:", e);
        } finally {
          setLoading(false);
        }
      }
      fetchData().then((rowData) => setRowData(rowData));
    }
  }, [searchParams, setMessage, usageApiUrl, useExternalApi]);

  return (
    <div className="p-4" style={{ width: "100%", height: "123px" }}>
      <AgGridReact rowData={rowData}
                   columnDefs={columnDefs}
                   loading={loading} />
    </div>
  );
};

export default GridComponent;