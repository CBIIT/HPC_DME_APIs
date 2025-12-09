'use client';

import { useState } from 'react';

const ErrorAlert = ({ message, onClose }) => {
    const [isVisible, setIsVisible] = useState(true);

    const handleClose = () => {
        setIsVisible(false);
        if (onClose) {
            onClose();
        }
    };

    if (!isVisible || !message) {
        return null;
    }

    return (

        <div className="alert alert-danger" role="alert">
            <strong className="font-bold">{message}</strong>
            <a className="close" onClick={handleClose}>×</a>
        </div>
    )
        ;
};

export default ErrorAlert;