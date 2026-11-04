package com.datlichkhambenh.backend.ops;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.sql.*;
import java.util.Locale;

/** Explicit offline operator command; never exposed over HTTP or run during normal application startup. */
public final class BootstrapAdminCommand {
 public static void main(String[] args) throws Exception {
  String email=required("BOOTSTRAP_ADMIN_EMAIL").trim().toLowerCase(Locale.ROOT);
  String password=required("BOOTSTRAP_ADMIN_PASSWORD");
  if(!email.contains("@") || password.length()<16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Valid email and password of 16+ characters / at most72 UTF8 bytes required");
  if(!"yes".equals(required("BOOTSTRAP_EMAIL_VERIFIED"))) throw new IllegalArgumentException("Operator must verify email ownership before bootstrap");
  Class.forName("com.mysql.cj.jdbc.Driver");
  try(Connection connection=DriverManager.getConnection(required("DB_URL"),required("DB_USERNAME"),required("DB_PASSWORD"))) {
   // One connection holds the MySQL advisory lock until the creation transaction commits.
   try(Statement statement=connection.createStatement();ResultSet lock=statement.executeQuery("SELECT GET_LOCK('clinic_bootstrap_admin',10)")) {
    if(!lock.next() || lock.getInt(1)!=1) throw new IllegalStateException("Cannot acquire bootstrap lock");
   }
   try {
    connection.setAutoCommit(false);
    try(Statement statement=connection.createStatement();ResultSet admins=statement.executeQuery("SELECT COUNT(*) FROM users WHERE role='ADMIN'")) {
     admins.next();if(admins.getLong(1)!=0) throw new IllegalStateException("Admin already exists; use normal administration");
    }
    try(PreparedStatement insert=connection.prepareStatement("INSERT INTO users(email,password_hash,role,is_active,email_verified,credentials_version,created_at,updated_at) VALUES (?,?,'ADMIN',TRUE,TRUE,0,NOW(6),NOW(6))")) {
     insert.setString(1,email);insert.setString(2,new BCryptPasswordEncoder(12).encode(password));insert.executeUpdate();
    }
    connection.commit(); System.out.println("Initial admin created. Remove bootstrap credentials from the operator environment.");
   } catch(Exception failure) {connection.rollback();throw failure;}
   finally {try(Statement statement=connection.createStatement()){statement.execute("SELECT RELEASE_LOCK('clinic_bootstrap_admin')");}}
  }
 }
 private static String required(String key) {String value=System.getenv(key);if(value==null || value.isBlank())throw new IllegalArgumentException("Missing "+key);return value;}
}
