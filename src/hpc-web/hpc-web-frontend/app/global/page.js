"use client";
import Link from "next/link";
import { useSession } from "../SessionContext";
import GridComponent from "./GridComponent";

export default function Global() {
    const session = useSession();
    return (
        <>
            <p>This would be the new Global namespace browse page</p>
            <Link href={"/"}>Go back to Home for {session?.hpcUser?.firstName}</Link>
            <GridComponent />
        </>
    );
}
