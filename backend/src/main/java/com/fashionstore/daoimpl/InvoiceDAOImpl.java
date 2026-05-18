package com.fashionstore.daoimpl;

import com.fashionstore.dao.InvoiceDAO;
import com.fashionstore.model.Invoice;
import com.fashionstore.util.DBConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAOImpl implements InvoiceDAO {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceDAOImpl.class);

    @Override
    public boolean createInvoice(Invoice invoice) {
        String sql = "INSERT INTO invoices (order_id, user_id, amount, invoice_number, status) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoice.getOrderId());
            ps.setInt(2, invoice.getUserId());
            ps.setBigDecimal(3, invoice.getAmount());
            ps.setString(4, invoice.getInvoiceNumber());
            ps.setString(5, invoice.getStatus() != null ? invoice.getStatus() : "PENDING");
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error creating invoice: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public Invoice getInvoiceById(int invoiceId) {
        String sql = "SELECT invoice_id, order_id, user_id, amount, invoice_number, status, created_at, updated_at FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapInvoice(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting invoice: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public Invoice getInvoiceByOrderId(int orderId) {
        String sql = "SELECT invoice_id, order_id, user_id, amount, invoice_number, status, created_at, updated_at FROM invoices WHERE order_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapInvoice(rs);
            }
        } catch (SQLException e) {
            logger.error("Error getting invoice by order: {}", e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Invoice> getInvoicesByUserId(int userId) {
        List<Invoice> invoices = new ArrayList<>();
        String sql = "SELECT invoice_id, order_id, user_id, amount, invoice_number, status, created_at, updated_at FROM invoices WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) invoices.add(mapInvoice(rs));
            }
        } catch (SQLException e) {
            logger.error("Error getting invoices by user: {}", e.getMessage(), e);
        }
        return invoices;
    }

    @Override
    public boolean updateInvoice(Invoice invoice) {
        String sql = "UPDATE invoices SET status = ?, amount = ? WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoice.getStatus());
            ps.setBigDecimal(2, invoice.getAmount());
            ps.setInt(3, invoice.getInvoiceId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error updating invoice: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean deleteInvoice(int invoiceId) {
        String sql = "DELETE FROM invoices WHERE invoice_id = ?";
        try (Connection conn = DBConnection.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error("Error deleting invoice: {}", e.getMessage(), e);
            return false;
        }
    }

    private Invoice mapInvoice(ResultSet rs) throws SQLException {
        Invoice invoice = new Invoice();
        invoice.setInvoiceId(rs.getInt("invoice_id"));
        invoice.setOrderId(rs.getInt("order_id"));
        invoice.setUserId(rs.getInt("user_id"));
        invoice.setAmount(rs.getBigDecimal("amount"));
        invoice.setInvoiceNumber(rs.getString("invoice_number"));
        invoice.setStatus(rs.getString("status"));
        invoice.setCreatedAt(rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null);
        invoice.setUpdatedAt(rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null);
        return invoice;
    }
}
