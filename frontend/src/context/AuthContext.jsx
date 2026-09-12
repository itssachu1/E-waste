import React, { createContext, useState, useEffect } from 'react';

// Create the context object
export const AuthContext = createContext();

/**
 * Provider component wrapping the React app to supply User login states globally.
 */
export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Check localStorage on bootstrap to keep user logged in if they refresh
    useEffect(() => {
        const savedUser = localStorage.getItem('janvoice_user');
        if (savedUser) {
            try {
                setUser(JSON.parse(savedUser));
            } catch (e) {
                localStorage.removeItem('janvoice_user');
            }
        }
        setLoading(false);
    }, []);

    // Login transaction method
    const login = (userData) => {
        localStorage.setItem('janvoice_user', JSON.stringify(userData));
        setUser(userData);
    };

    // Logout transaction method
    const logout = () => {
        localStorage.removeItem('janvoice_user');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {!loading && children}
        </AuthContext.Provider>
    );
};
