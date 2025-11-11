"use client";
import Link from "next/link";
import { useSession } from "./SessionContext";

export default function Home() {
    const session = useSession();
    return (
        <>
            <p>This is the landing page of DME Web frontend</p>
            <Link href={"/global"}>Go to new browse page for {session?.hpcUser?.firstName}</Link>
        </>
    );
}
