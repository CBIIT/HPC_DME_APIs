"use client";
import Link from "next/link";
import { useSession } from "./SessionContext";

export default function Footer() {
    const session = useSession();
    const mailtoLink = `mailto:${session?.contactEmail}`;
    return (
        <div id="footer" className="footer" role="contentinfo">
            <div className="text-center">
                <ul className="list-inline">
                    <li><Link id="contactLink" href={"/"} tabIndex="0">Home</Link></li>
                    <li>|</li>
                    <li><Link tabIndex="0" href={mailtoLink}>Contact Us
                    </Link></li>
                    <li>|</li>
                    <li><a target="_blank" href="https://www.cancer.gov/policies"
                           tabIndex="0">Policies</a></li>
                    <li>|</li>
                    <li><a target="_blank"
                           href="http://www.cancer.gov/policies/accessibility" tabIndex="0">Accessibility</a></li>
                </ul>
                <ul className="list-inline">
                    <li><a id="HSSlink" target="_blank" href="http://www.hhs.gov/"
                           tabIndex="0">U.S. Department of Health and Human Services</a></li>
                    <li>|</li>
                    <li><a target="_blank" href="http://www.nih.gov" tabIndex="0">National
                        Institutes of Health</a></li>
                    <li>|</li>
                    <li><a target="_blank" href="http://www.cancer.gov/"
                           tabIndex="0">National Cancer Institute</a></li>
                    <li>|</li>
                    <li><a target="_blank" href="http://usa.gov" tabIndex="0">USA.gov</a></li>
                </ul>
                <ul className="list-inline">
                    <li><span className="removeOutline" tabIndex="-1">NIH ...
						Turning Discovery Into Health</span><sup>®</sup></li>
                </ul>
            </div>
        </div>
    );
}