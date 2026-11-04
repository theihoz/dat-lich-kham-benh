package com.datlichkhambenh.backend.security;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
public final class AuthTokenSecrets {
 private static final SecureRandom RANDOM = new SecureRandom();
 private AuthTokenSecrets() {}
 public static String generate() { byte[] value=new byte[32]; RANDOM.nextBytes(value); return Base64.getUrlEncoder().withoutPadding().encodeToString(value); }
 public static String hash(String value) {
  if(value==null || value.isBlank() || value.length()>1024) return "";
  try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
  catch(NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
 }
}
