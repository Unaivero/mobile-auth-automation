package com.securitytests.utils.security;

import com.securitytests.utils.logging.StructuredLogger;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import io.qameta.allure.Step;
import java.util.*;
import java.time.Instant;

/**
 * Utility for analyzing and validating JWT tokens for security issues
 */
public class JWTSecurityAnalyzer {
    private static final StructuredLogger logger = new StructuredLogger(JWTSecurityAnalyzer.class);
    
    // Minimum recommended expiration time (15 minutes in milliseconds)
    private static final long MIN_EXPIRATION_TIME = 15 * 60 * 1000;
    
    // Maximum recommended expiration time (24 hours in milliseconds)
    private static final long MAX_EXPIRATION_TIME = 24 * 60 * 60 * 1000;
    
    // Set of required claims for secure tokens
    private static final Set<String> REQUIRED_CLAIMS = new HashSet<>(Arrays.asList(
            "sub", // subject
            "exp", // expiration time
            "iat", // issued at
            "jti"  // JWT ID
    ));
    
    // Set of sensitive claims that should be reviewed
    private static final Set<String> SENSITIVE_CLAIMS = new HashSet<>(Arrays.asList(
            "email",
            "phone_number",
            "address",
            "ssn",
            "role"
    ));
    
    /**
     * Analyze JWT token for security issues
     * 
     * @param token The JWT token to analyze
     * @return JWTAnalysisResult containing analysis results
     */
    @Step("Analyze JWT token")
    public JWTAnalysisResult analyzeToken(String token) {
        logger.info("Analyzing JWT token");
        
        if (token == null || token.isEmpty()) {
            logger.error("Cannot analyze null or empty token");
            return new JWTAnalysisResult(false, "Token is null or empty", Collections.singletonList("Token is null or empty"));
        }
        
        try {
            // Parse the token without verification to analyze structure and claims
            Jwt<Header, Claims> jwt = Jwts.parserBuilder()
                    .setAllowedClockSkewSeconds(60) // 1 minute clock skew
                    .build()
                    .parseClaimsJwt(getTokenWithoutSignature(token));
            
            Header header = jwt.getHeader();
            Claims claims = jwt.getBody();
            
            // Start analyzing token
            List<String> issues = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            
            // Check algorithm
            String algorithm = (String) header.get("alg");
            if ("none".equals(algorithm)) {
                issues.add("Token uses 'none' algorithm which is insecure");
            } else if ("HS256".equals(algorithm)) {
                warnings.add("Token uses HS256 algorithm, consider using RS256 for better security");
            }
            
            // Check for required claims
            for (String claim : REQUIRED_CLAIMS) {
                if (!claims.containsKey(claim)) {
                    issues.add("Token is missing required claim: " + claim);
                }
            }
            
            // Check expiration
            if (claims.containsKey("exp")) {
                Date expiration = claims.getExpiration();
                Date now = Date.from(Instant.now());
                
                // Check if token is expired
                if (expiration.before(now)) {
                    issues.add("Token is expired (exp: " + expiration + ")");
                }
                
                // Check if token has reasonable expiration time
                Date issuedAt = claims.getIssuedAt();
                if (issuedAt != null) {
                    long duration = expiration.getTime() - issuedAt.getTime();
                    if (duration < MIN_EXPIRATION_TIME) {
                        warnings.add("Token has very short expiration time: " + (duration / 1000) + " seconds");
                    } else if (duration > MAX_EXPIRATION_TIME) {
                        issues.add("Token has too long expiration time: " + (duration / (60 * 60 * 1000)) + " hours");
                    }
                }
            }
            
            // Check sensitive information in token
            for (String claim : SENSITIVE_CLAIMS) {
                if (claims.containsKey(claim)) {
                    warnings.add("Token contains potentially sensitive claim: " + claim);
                }
            }
            
            // Check token size
            if (token.length() > 1000) {
                warnings.add("Token is very large (" + token.length() + " chars), which may impact performance");
            }
            
            boolean isSecure = issues.isEmpty();
            
            // Create analysis result
            JWTAnalysisResult result = new JWTAnalysisResult(
                isSecure,
                isSecure ? "Token appears secure" : "Token has security issues",
                issues
            );
            
            result.setWarnings(warnings);
            result.setTokenClaims(claims);
            result.setTokenHeader(header);
            
            logger.info("JWT analysis completed. Secure: {}, Issues: {}, Warnings: {}", 
                    isSecure, issues.size(), warnings.size());
            
            return result;
            
        } catch (ExpiredJwtException e) {
            logger.warn("Token is expired", e);
            return new JWTAnalysisResult(false, "Token is expired", 
                    Collections.singletonList("Token is expired: " + e.getMessage()));
        } catch (MalformedJwtException e) {
            logger.error("Token is malformed", e);
            return new JWTAnalysisResult(false, "Token is malformed", 
                    Collections.singletonList("Token is malformed: " + e.getMessage()));
        } catch (SignatureException e) {
            logger.warn("Token has invalid signature", e);
            return new JWTAnalysisResult(false, "Token has invalid signature", 
                    Collections.singletonList("Token has invalid signature: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error analyzing token", e);
            return new JWTAnalysisResult(false, "Error analyzing token", 
                    Collections.singletonList("Error analyzing token: " + e.getMessage()));
        }
    }
    
    /**
     * Verify JWT token with a specific key
     * 
     * @param token The JWT token to verify
     * @param secretKey The secret key for HS* algorithms
     * @return true if token is valid, false otherwise
     */
    @Step("Verify JWT token signature")
    public boolean verifyToken(String token, String secretKey) {
        if (token == null || secretKey == null) {
            return false;
        }
        
        try {
            Jws<Claims> jws = Jwts.parserBuilder()
                    .setSigningKey(secretKey.getBytes())
                    .build()
                    .parseClaimsJws(token);
            
            // If we get here, signature is valid
            return true;
        } catch (Exception e) {
            logger.warn("Token verification failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove signature from token to allow analysis without verification
     */
    private String getTokenWithoutSignature(String token) {
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            return parts[0] + "." + parts[1] + ".";
        }
        return token;
    }
    
    /**
     * JWT Analysis Result class
     */
    public static class JWTAnalysisResult {
        private final boolean secure;
        private final String summary;
        private final List<String> issues;
        private List<String> warnings;
        private Claims tokenClaims;
        private Header tokenHeader;
        private Date timestamp;
        
        public JWTAnalysisResult(boolean secure, String summary, List<String> issues) {
            this.secure = secure;
            this.summary = summary;
            this.issues = issues;
            this.warnings = new ArrayList<>();
            this.timestamp = new Date();
        }
        
        public boolean isSecure() {
            return secure;
        }
        
        public String getSummary() {
            return summary;
        }
        
        public List<String> getIssues() {
            return issues;
        }
        
        public List<String> getWarnings() {
            return warnings;
        }
        
        public void setWarnings(List<String> warnings) {
            this.warnings = warnings;
        }
        
        public Claims getTokenClaims() {
            return tokenClaims;
        }
        
        public void setTokenClaims(Claims tokenClaims) {
            this.tokenClaims = tokenClaims;
        }
        
        public Header getTokenHeader() {
            return tokenHeader;
        }
        
        public void setTokenHeader(Header tokenHeader) {
            this.tokenHeader = tokenHeader;
        }
        
        public Date getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }
        
        /**
         * Get JWT token metadata as a map
         * @return Map of token metadata
         */
        public Map<String, Object> getTokenMetadata() {
            Map<String, Object> metadata = new HashMap<>();
            
            if (tokenHeader != null) {
                metadata.put("algorithm", tokenHeader.get("alg"));
                metadata.put("type", tokenHeader.get("typ"));
                metadata.put("keyId", tokenHeader.get("kid"));
            }
            
            if (tokenClaims != null) {
                metadata.put("subject", tokenClaims.get("sub"));
                metadata.put("issuer", tokenClaims.getIssuer());
                metadata.put("issuedAt", tokenClaims.getIssuedAt());
                metadata.put("expiration", tokenClaims.getExpiration());
                metadata.put("audience", tokenClaims.getAudience());
                metadata.put("id", tokenClaims.getId());
            }
            
            return metadata;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("JWT Analysis Result:\n");
            sb.append("Security Status: ").append(secure ? "SECURE" : "INSECURE").append("\n");
            sb.append("Summary: ").append(summary).append("\n");
            
            if (!issues.isEmpty()) {
                sb.append("\nIssues:\n");
                for (String issue : issues) {
                    sb.append("- ").append(issue).append("\n");
                }
            }
            
            if (!warnings.isEmpty()) {
                sb.append("\nWarnings:\n");
                for (String warning : warnings) {
                    sb.append("- ").append(warning).append("\n");
                }
            }
            
            if (tokenClaims != null) {
                sb.append("\nToken Claims:\n");
                for (String key : tokenClaims.keySet()) {
                    sb.append("- ").append(key).append(": ").append(tokenClaims.get(key)).append("\n");
                }
            }
            
            return sb.toString();
        }
    }
}
