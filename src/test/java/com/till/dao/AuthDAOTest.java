package com.till.dao;

import com.till.model.UserAccount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthDAOTest {

    @Test
    void defaultUsersExistAndCredentialsValidate() {
        AuthDAO authDAO = new AuthDAO();
        authDAO.ensureDefaultUsers();

        UserAccount manager = authDAO.findByUsername("manager");
        UserAccount cashier = authDAO.findByUsername("cashier");

        assertNotNull(manager);
        assertNotNull(cashier);
        assertEquals("MANAGER", manager.getRole());
        assertEquals("CASHIER", cashier.getRole());
        assertTrue(authDAO.verifyCredentials("manager", "admin123", "MANAGER"));
        assertTrue(authDAO.verifyCredentials("cashier", "cashier123", "CASHIER"));
        assertFalse(authDAO.verifyCredentials("cashier", "wrong", "CASHIER"));
    }
}
