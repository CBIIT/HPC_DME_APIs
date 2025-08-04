"use client";
import {useSession} from "../SessionContext";
import GridComponent from "./GridComponent";
import React from "react";

export default function Usage() {
    const session = useSession();
    return (
        <>
            <div className="container mb-4">
                <div className="row justify-content-end">
                    <div className="col-lg-1">
                        {/* Close button */}
                        <button type="button" className="btn btn-primary form-control">
                            <span>Close</span>
                        </button>
                    </div>
                </div>
            </div>
            <section className="p-4 bg-white">
                <h3 className="ms-4">Calculate Total Size</h3>
                <div className="m-4 border border-2 rounded-3">
                    <h3 className="ms-4 mt-4">Selected Path</h3>
                    <GridComponent />
                </div>
            </section>`
        </>
    );
}
