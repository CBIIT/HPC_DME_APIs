import "bootstrap/dist/css/bootstrap.min.css";
import "./bootstrap-theme.css";
import "./style.css"
import "./elegant-icons-style.css"
import {config} from '@fortawesome/fontawesome-svg-core';
import '@fortawesome/fontawesome-svg-core/styles.css';
config.autoAddCss = false;
import {library} from '@fortawesome/fontawesome-svg-core';
import {faGear} from '@fortawesome/free-solid-svg-icons';
library.add(faGear);


import BootstrapClient from "./Bootstrap";
import SessionProvider from "./SessionContext";
import Header from "./Header";
import Footer from "./Footer";


export const metadata = {
    title: "NCI Data Management Environment",
    description: "NCI Data Management Environment",
};

export default async function RootLayout({children}) {
    return (
        <html lang="en">
        <body>
        <BootstrapClient/>
        <SessionProvider>
            <Header/>
            {children}
            <Footer/>
        </SessionProvider>
        </body>
        </html>
    );
}
