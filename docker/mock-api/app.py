#!/usr/bin/env python3
"""
Mock API Server for Mobile Authentication Testing
Simulates a real mobile authentication backend with comprehensive security features
"""

import os
import json
import time
import uuid
import hashlib
import secrets
import base64
from datetime import datetime, timedelta
from typing import Dict, List, Optional, Any
import logging

from flask import Flask, request, jsonify, session
from flask_cors import CORS
from flask_limiter import Limiter
from flask_limiter.util import get_remote_address
import psycopg2
import redis
import jwt
from werkzeug.security import generate_password_hash, check_password_hash

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

app = Flask(__name__)
app.secret_key = os.environ.get('SECRET_KEY', 'mobile-auth-secret-key-for-testing')
CORS(app)

# Rate limiting
limiter = Limiter(
    app,
    key_func=get_remote_address,
    default_limits=["200 per day", "50 per hour"]
)

# Database and Redis connections
def get_db_connection():
    return psycopg2.connect(
        host=os.environ.get('DB_HOST', 'postgres-db'),
        database=os.environ.get('DB_NAME', 'mobile_auth_testing'),
        user=os.environ.get('DB_USER', 'test_user'),
        password=os.environ.get('DB_PASS', 'test_password'),
        port=5432
    )

def get_redis_connection():
    return redis.Redis(
        host=os.environ.get('REDIS_HOST', 'redis'),
        port=int(os.environ.get('REDIS_PORT', 6379)),
        decode_responses=True
    )

redis_client = get_redis_connection()

# JWT Configuration
JWT_SECRET = os.environ.get('JWT_SECRET', 'jwt-secret-for-testing')
JWT_ALGORITHM = 'HS256'
JWT_EXPIRATION_HOURS = 24

# Security Configuration
MAX_LOGIN_ATTEMPTS = 5
ACCOUNT_LOCKOUT_DURATION = 30  # minutes
CAPTCHA_THRESHOLD = 3
SESSION_TIMEOUT = 30  # minutes

class AuthenticationService:
    """Handles authentication logic and security features"""
    
    @staticmethod
    def generate_jwt_token(user_data: Dict) -> str:
        """Generate JWT token for authenticated user"""
        payload = {
            'user_id': str(user_data['id']),
            'username': user_data['username'],
            'email': user_data['email'],
            'role': user_data.get('user_role', 'user'),
            'exp': datetime.utcnow() + timedelta(hours=JWT_EXPIRATION_HOURS),
            'iat': datetime.utcnow(),
            'jti': str(uuid.uuid4())  # JWT ID for token revocation
        }
        return jwt.encode(payload, JWT_SECRET, algorithm=JWT_ALGORITHM)
    
    @staticmethod
    def verify_jwt_token(token: str) -> Optional[Dict]:
        """Verify and decode JWT token"""
        try:
            payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM])
            
            # Check if token is blacklisted
            if redis_client.get(f"blacklist:{payload['jti']}"):
                return None
                
            return payload
        except jwt.ExpiredSignatureError:
            logger.warning("JWT token expired")
            return None
        except jwt.InvalidTokenError:
            logger.warning("Invalid JWT token")
            return None
    
    @staticmethod
    def blacklist_token(token: str):
        """Add token to blacklist"""
        try:
            payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM], options={"verify_exp": False})
            redis_client.setex(f"blacklist:{payload['jti']}", 86400, "blacklisted")  # 24 hours
        except Exception as e:
            logger.error(f"Error blacklisting token: {e}")
    
    @staticmethod
    def check_rate_limit(identifier: str, limit: int, window: int) -> bool:
        """Check if rate limit is exceeded"""
        key = f"rate_limit:{identifier}"
        current = redis_client.get(key)
        
        if current is None:
            redis_client.setex(key, window, 1)
            return True
        elif int(current) < limit:
            redis_client.incr(key)
            return True
        else:
            return False
    
    @staticmethod
    def generate_captcha() -> Dict[str, str]:
        """Generate simple captcha for testing"""
        num1 = secrets.randbelow(10)
        num2 = secrets.randbelow(10)
        captcha_id = str(uuid.uuid4())
        answer = str(num1 + num2)
        
        # Store captcha answer in Redis with 5-minute expiration
        redis_client.setex(f"captcha:{captcha_id}", 300, answer)
        
        return {
            "captcha_id": captcha_id,
            "question": f"{num1} + {num2} = ?",
            "expires_in": 300
        }
    
    @staticmethod
    def verify_captcha(captcha_id: str, user_answer: str) -> bool:
        """Verify captcha answer"""
        correct_answer = redis_client.get(f"captcha:{captcha_id}")
        if correct_answer and str(user_answer).strip() == correct_answer:
            redis_client.delete(f"captcha:{captcha_id}")  # One-time use
            return True
        return False

auth_service = AuthenticationService()

# Helper functions
def get_client_info() -> Dict[str, Any]:
    """Extract client information from request"""
    return {
        'ip_address': request.environ.get('HTTP_X_FORWARDED_FOR', request.remote_addr),
        'user_agent': request.headers.get('User-Agent', ''),
        'timestamp': datetime.utcnow().isoformat(),
        'device_id': request.headers.get('X-Device-ID'),
        'app_version': request.headers.get('X-App-Version'),
        'platform': request.headers.get('X-Platform'),
        'device_fingerprint': request.headers.get('X-Device-Fingerprint')
    }

def log_auth_event(user_id: str, event_type: str, success: bool, additional_data: Dict = None):
    """Log authentication event to database"""
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        client_info = get_client_info()
        
        cursor.execute("""
            INSERT INTO auth_events_log (user_id, event_type, ip_address, user_agent, 
                                       device_fingerprint, additional_data, risk_score)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
        """, (
            user_id, event_type, client_info['ip_address'], client_info['user_agent'],
            client_info.get('device_fingerprint'), 
            json.dumps(additional_data or {}), 
            calculate_risk_score(client_info, success)
        ))
        
        conn.commit()
        cursor.close()
        conn.close()
    except Exception as e:
        logger.error(f"Error logging auth event: {e}")

def calculate_risk_score(client_info: Dict, success: bool) -> int:
    """Calculate risk score based on various factors"""
    risk_score = 0
    
    # IP-based risk
    ip = client_info.get('ip_address', '')
    if ip.startswith('127.') or ip.startswith('192.168.'):
        risk_score += 10  # Local IP
    
    # Device fingerprinting
    if not client_info.get('device_fingerprint'):
        risk_score += 20  # No device fingerprint
    
    # Failed authentication
    if not success:
        risk_score += 30
    
    # Time-based risk (unusual hours)
    hour = datetime.now().hour
    if hour < 6 or hour > 22:
        risk_score += 15
    
    return min(risk_score, 100)

# API Routes

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint"""
    try:
        # Check database connection
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT 1")
        cursor.close()
        conn.close()
        
        # Check Redis connection
        redis_client.ping()
        
        return jsonify({
            'status': 'healthy',
            'timestamp': datetime.utcnow().isoformat(),
            'version': '1.0.0'
        }), 200
    except Exception as e:
        logger.error(f"Health check failed: {e}")
        return jsonify({
            'status': 'unhealthy',
            'error': str(e),
            'timestamp': datetime.utcnow().isoformat()
        }), 503

@app.route('/api/auth/login', methods=['POST'])
@limiter.limit("10 per minute")
def login():
    """Authenticate user with security features"""
    try:
        data = request.get_json()
        if not data or not data.get('username') or not data.get('password'):
            return jsonify({
                'success': False,
                'message': 'Username and password are required',
                'error_code': 'MISSING_CREDENTIALS'
            }), 400
        
        username = data['username'].strip().lower()
        password = data['password']
        captcha_id = data.get('captcha_id')
        captcha_answer = data.get('captcha_answer')
        
        client_info = get_client_info()
        client_id = f"{client_info['ip_address']}:{username}"
        
        # Check rate limiting
        if not auth_service.check_rate_limit(client_id, 10, 300):  # 10 attempts per 5 minutes
            log_auth_event(username, 'login_rate_limited', False, client_info)
            return jsonify({
                'success': False,
                'message': 'Too many login attempts. Please try again later.',
                'error_code': 'RATE_LIMITED'
            }), 429
        
        # Get user from database
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("""
            SELECT id, username, email, password_hash, salt, is_active, is_locked, 
                   failed_login_attempts, biometric_enabled, two_factor_enabled, user_role
            FROM test_users WHERE username = %s OR email = %s
        """, (username, username))
        
        user = cursor.fetchone()
        
        if not user:
            # Simulate timing attack protection
            time.sleep(0.1)
            log_auth_event(username, 'login_failed_user_not_found', False, client_info)
            return jsonify({
                'success': False,
                'message': 'Invalid credentials',
                'error_code': 'INVALID_CREDENTIALS'
            }), 401
        
        user_data = {
            'id': user[0], 'username': user[1], 'email': user[2], 
            'password_hash': user[3], 'salt': user[4], 'is_active': user[5],
            'is_locked': user[6], 'failed_login_attempts': user[7],
            'biometric_enabled': user[8], 'two_factor_enabled': user[9], 'user_role': user[10]
        }
        
        # Check if user is active
        if not user_data['is_active']:
            log_auth_event(str(user_data['id']), 'login_failed_inactive', False, client_info)
            return jsonify({
                'success': False,
                'message': 'Account is inactive',
                'error_code': 'ACCOUNT_INACTIVE'
            }), 401
        
        # Check if user is locked
        if user_data['is_locked']:
            log_auth_event(str(user_data['id']), 'login_failed_locked', False, client_info)
            return jsonify({
                'success': False,
                'message': 'Account is temporarily locked due to too many failed attempts',
                'error_code': 'ACCOUNT_LOCKED',
                'unlock_time': (datetime.utcnow() + timedelta(minutes=ACCOUNT_LOCKOUT_DURATION)).isoformat()
            }), 401
        
        # Check if CAPTCHA is required
        failed_attempts = user_data['failed_login_attempts']
        if failed_attempts >= CAPTCHA_THRESHOLD:
            if not captcha_id or not captcha_answer:
                captcha = auth_service.generate_captcha()
                return jsonify({
                    'success': False,
                    'message': 'CAPTCHA verification required',
                    'error_code': 'CAPTCHA_REQUIRED',
                    'captcha': captcha
                }), 400
            
            if not auth_service.verify_captcha(captcha_id, captcha_answer):
                log_auth_event(str(user_data['id']), 'login_failed_captcha', False, client_info)
                captcha = auth_service.generate_captcha()
                return jsonify({
                    'success': False,
                    'message': 'Invalid CAPTCHA',
                    'error_code': 'INVALID_CAPTCHA',
                    'captcha': captcha
                }), 400
        
        # Verify password
        if not check_password_hash(user_data['password_hash'], password):
            # Update failed login attempts
            cursor.execute("""
                UPDATE test_users SET failed_login_attempts = failed_login_attempts + 1,
                       last_login_attempt = CURRENT_TIMESTAMP,
                       is_locked = CASE WHEN failed_login_attempts + 1 >= %s THEN true ELSE false END
                WHERE id = %s
            """, (MAX_LOGIN_ATTEMPTS, user_data['id']))
            conn.commit()
            
            log_auth_event(str(user_data['id']), 'login_failed_password', False, client_info)
            
            # Check if account should be locked
            if failed_attempts + 1 >= MAX_LOGIN_ATTEMPTS:
                return jsonify({
                    'success': False,
                    'message': 'Account locked due to too many failed attempts',
                    'error_code': 'ACCOUNT_LOCKED'
                }), 401
            
            return jsonify({
                'success': False,
                'message': 'Invalid credentials',
                'error_code': 'INVALID_CREDENTIALS',
                'remaining_attempts': MAX_LOGIN_ATTEMPTS - (failed_attempts + 1)
            }), 401
        
        # Successful authentication - reset failed attempts
        cursor.execute("""
            UPDATE test_users SET failed_login_attempts = 0, last_login_attempt = CURRENT_TIMESTAMP,
                   is_locked = false WHERE id = %s
        """, (user_data['id'],))
        conn.commit()
        cursor.close()
        conn.close()
        
        # Generate JWT token
        token = auth_service.generate_jwt_token(user_data)
        
        # Store session info in Redis
        session_id = str(uuid.uuid4())
        session_data = {
            'user_id': str(user_data['id']),
            'username': user_data['username'],
            'login_time': datetime.utcnow().isoformat(),
            'client_info': client_info
        }
        redis_client.setex(f"session:{session_id}", SESSION_TIMEOUT * 60, json.dumps(session_data))
        
        log_auth_event(str(user_data['id']), 'login_success', True, client_info)
        
        response_data = {
            'success': True,
            'message': 'Login successful',
            'token': token,
            'session_id': session_id,
            'user': {
                'id': str(user_data['id']),
                'username': user_data['username'],
                'email': user_data['email'],
                'role': user_data['user_role'],
                'biometric_enabled': user_data['biometric_enabled'],
                'two_factor_enabled': user_data['two_factor_enabled']
            },
            'expires_in': JWT_EXPIRATION_HOURS * 3600
        }
        
        # Check if 2FA is enabled
        if user_data['two_factor_enabled']:
            response_data['requires_2fa'] = True
            response_data['message'] = 'Two-factor authentication required'
        
        return jsonify(response_data), 200
        
    except Exception as e:
        logger.error(f"Login error: {e}")
        return jsonify({
            'success': False,
            'message': 'Internal server error',
            'error_code': 'INTERNAL_ERROR'
        }), 500

@app.route('/api/auth/biometric', methods=['POST'])
@limiter.limit("5 per minute")
def biometric_auth():
    """Biometric authentication endpoint"""
    try:
        data = request.get_json()
        username = data.get('username')
        biometric_type = data.get('biometric_type')  # fingerprint, face_id, voice
        biometric_data = data.get('biometric_data')  # Base64 encoded biometric template
        
        if not all([username, biometric_type, biometric_data]):
            return jsonify({
                'success': False,
                'message': 'Username, biometric type, and biometric data are required',
                'error_code': 'MISSING_BIOMETRIC_DATA'
            }), 400
        
        client_info = get_client_info()
        
        # Get user and biometric data from database
        conn = get_db_connection()
        cursor = conn.cursor()
        
        cursor.execute("""
            SELECT u.id, u.username, u.email, u.is_active, u.is_locked, u.user_role,
                   b.template_data, b.is_enrolled
            FROM test_users u
            LEFT JOIN biometric_test_data b ON u.id = b.user_id 
                AND b.biometric_type = %s AND b.is_active = true
            WHERE u.username = %s OR u.email = %s
        """, (biometric_type, username, username))
        
        result = cursor.fetchone()
        
        if not result or not result[0]:
            log_auth_event(username, 'biometric_failed_user_not_found', False, client_info)
            return jsonify({
                'success': False,
                'message': 'User not found or biometric not enrolled',
                'error_code': 'BIOMETRIC_NOT_ENROLLED'
            }), 401
        
        user_data = {
            'id': result[0], 'username': result[1], 'email': result[2],
            'is_active': result[3], 'is_locked': result[4], 'user_role': result[5],
            'template_data': result[6], 'is_enrolled': result[7]
        }
        
        # Check user status
        if not user_data['is_active']:
            return jsonify({
                'success': False,
                'message': 'Account is inactive',
                'error_code': 'ACCOUNT_INACTIVE'
            }), 401
        
        if user_data['is_locked']:
            return jsonify({
                'success': False,
                'message': 'Account is locked',
                'error_code': 'ACCOUNT_LOCKED'
            }), 401
        
        if not user_data['is_enrolled'] or not user_data['template_data']:
            return jsonify({
                'success': False,
                'message': 'Biometric authentication not enrolled for this user',
                'error_code': 'BIOMETRIC_NOT_ENROLLED'
            }), 401
        
        # Simulate biometric matching (in real implementation, this would be complex biometric comparison)
        # For testing purposes, we'll simulate successful match if provided data matches stored template
        stored_template = base64.b64encode(user_data['template_data']).decode('utf-8')
        
        # Simple comparison for testing - in production, use proper biometric matching algorithms
        if biometric_data == stored_template:
            # Update last used timestamp
            cursor.execute("""
                UPDATE biometric_test_data SET last_used = CURRENT_TIMESTAMP 
                WHERE user_id = %s AND biometric_type = %s
            """, (user_data['id'], biometric_type))
            conn.commit()
            
            # Generate JWT token
            token = auth_service.generate_jwt_token(user_data)
            
            # Create session
            session_id = str(uuid.uuid4())
            session_data = {
                'user_id': str(user_data['id']),
                'username': user_data['username'],
                'auth_method': 'biometric',
                'biometric_type': biometric_type,
                'login_time': datetime.utcnow().isoformat(),
                'client_info': client_info
            }
            redis_client.setex(f"session:{session_id}", SESSION_TIMEOUT * 60, json.dumps(session_data))
            
            log_auth_event(str(user_data['id']), f'biometric_success_{biometric_type}', True, client_info)
            
            cursor.close()
            conn.close()
            
            return jsonify({
                'success': True,
                'message': 'Biometric authentication successful',
                'token': token,
                'session_id': session_id,
                'user': {
                    'id': str(user_data['id']),
                    'username': user_data['username'],
                    'email': user_data['email'],
                    'role': user_data['user_role']
                },
                'auth_method': 'biometric',
                'biometric_type': biometric_type,
                'expires_in': JWT_EXPIRATION_HOURS * 3600
            }), 200
        else:
            log_auth_event(str(user_data['id']), f'biometric_failed_{biometric_type}', False, client_info)
            cursor.close()
            conn.close()
            
            return jsonify({
                'success': False,
                'message': 'Biometric verification failed',
                'error_code': 'BIOMETRIC_MISMATCH'
            }), 401
        
    except Exception as e:
        logger.error(f"Biometric auth error: {e}")
        return jsonify({
            'success': False,
            'message': 'Internal server error',
            'error_code': 'INTERNAL_ERROR'
        }), 500

@app.route('/api/auth/logout', methods=['POST'])
def logout():
    """Logout user and invalidate session"""
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({
                'success': False,
                'message': 'Authorization token required',
                'error_code': 'MISSING_TOKEN'
            }), 401
        
        token = auth_header.split(' ')[1]
        payload = auth_service.verify_jwt_token(token)
        
        if not payload:
            return jsonify({
                'success': False,
                'message': 'Invalid or expired token',
                'error_code': 'INVALID_TOKEN'
            }), 401
        
        # Blacklist the token
        auth_service.blacklist_token(token)
        
        # Remove session data
        session_id = request.json.get('session_id') if request.json else None
        if session_id:
            redis_client.delete(f"session:{session_id}")
        
        # Log logout event
        log_auth_event(payload['user_id'], 'logout', True, get_client_info())
        
        return jsonify({
            'success': True,
            'message': 'Logout successful'
        }), 200
        
    except Exception as e:
        logger.error(f"Logout error: {e}")
        return jsonify({
            'success': False,
            'message': 'Internal server error',
            'error_code': 'INTERNAL_ERROR'
        }), 500

@app.route('/api/auth/password-reset', methods=['POST'])
@limiter.limit("3 per minute")
def password_reset():
    """Password reset request"""
    try:
        data = request.get_json()
        email = data.get('email', '').strip().lower()
        
        if not email:
            return jsonify({
                'success': False,
                'message': 'Email address is required',
                'error_code': 'MISSING_EMAIL'
            }), 400
        
        client_info = get_client_info()
        
        # Check if user exists (don't reveal if user exists or not for security)
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT id, username, email FROM test_users WHERE email = %s", (email,))
        user = cursor.fetchone()
        
        # Always return success to prevent email enumeration
        # In real implementation, only send email if user exists
        if user:
            user_id, username, user_email = user
            
            # Generate reset token
            reset_token = secrets.token_urlsafe(32)
            reset_expires = datetime.utcnow() + timedelta(hours=1)  # 1 hour expiration
            
            # Store reset token in Redis
            redis_client.setex(
                f"password_reset:{reset_token}", 
                3600,  # 1 hour
                json.dumps({
                    'user_id': str(user_id),
                    'email': user_email,
                    'expires': reset_expires.isoformat()
                })
            )
            
            log_auth_event(str(user_id), 'password_reset_requested', True, client_info)
            
            # In real implementation, send email here
            logger.info(f"Password reset requested for user {username}. Reset token: {reset_token}")
        
        cursor.close()
        conn.close()
        
        return jsonify({
            'success': True,
            'message': 'If the email address exists in our system, a password reset link has been sent.',
            'reset_token': reset_token if user else None  # Only for testing purposes
        }), 200
        
    except Exception as e:
        logger.error(f"Password reset error: {e}")
        return jsonify({
            'success': False,
            'message': 'Internal server error',
            'error_code': 'INTERNAL_ERROR'
        }), 500

@app.route('/api/auth/verify-token', methods=['POST'])
def verify_token():
    """Verify JWT token validity"""
    try:
        auth_header = request.headers.get('Authorization')
        if not auth_header or not auth_header.startswith('Bearer '):
            return jsonify({
                'valid': False,
                'message': 'Authorization token required'
            }), 401
        
        token = auth_header.split(' ')[1]
        payload = auth_service.verify_jwt_token(token)
        
        if payload:
            return jsonify({
                'valid': True,
                'user': {
                    'id': payload['user_id'],
                    'username': payload['username'],
                    'email': payload['email'],
                    'role': payload['role']
                },
                'expires_at': payload['exp']
            }), 200
        else:
            return jsonify({
                'valid': False,
                'message': 'Invalid or expired token'
            }), 401
            
    except Exception as e:
        logger.error(f"Token verification error: {e}")
        return jsonify({
            'valid': False,
            'message': 'Token verification failed'
        }), 500

@app.route('/api/captcha', methods=['GET'])
def get_captcha():
    """Generate a new CAPTCHA challenge"""
    try:
        captcha = auth_service.generate_captcha()
        return jsonify({
            'success': True,
            'captcha': captcha
        }), 200
    except Exception as e:
        logger.error(f"CAPTCHA generation error: {e}")
        return jsonify({
            'success': False,
            'message': 'Failed to generate CAPTCHA'
        }), 500

if __name__ == '__main__':
    logger.info("Starting Mobile Auth Mock API Server")
    app.run(host='0.0.0.0', port=8081, debug=False)