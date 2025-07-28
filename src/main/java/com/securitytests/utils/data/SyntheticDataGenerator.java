package com.securitytests.utils.data;

import com.securitytests.utils.logging.StructuredLogger;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Generates synthetic test data for various test scenarios
 */
public class SyntheticDataGenerator {
    private static final StructuredLogger logger = new StructuredLogger(SyntheticDataGenerator.class);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, List<String>> dataCache = new HashMap<>();
    
    // List of first names for generating random user data
    private static final String[] FIRST_NAMES = {
            "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles",
            "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen",
            "Alex", "Jamie", "Taylor", "Jordan", "Casey", "Riley", "Quinn", "Skyler", "Avery", "Morgan"
    };
    
    // List of last names for generating random user data
    private static final String[] LAST_NAMES = {
            "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
            "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
            "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young", "Hernandez", "King"
    };
    
    // List of street names for generating address data
    private static final String[] STREET_NAMES = {
            "Main", "Park", "Oak", "Pine", "Maple", "Cedar", "Elm", "View", "Washington", "Lake",
            "Hill", "Forest", "River", "Valley", "Mountain", "Spring", "Sunset", "Willow", "Meadow", "Ridge"
    };
    
    // List of street types for generating address data
    private static final String[] STREET_TYPES = {
            "St", "Ave", "Blvd", "Rd", "Ln", "Dr", "Way", "Pl", "Ct", "Ter"
    };
    
    // List of cities for generating address data
    private static final String[] CITIES = {
            "New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego",
            "Dallas", "San Jose", "Austin", "Jacksonville", "San Francisco", "Columbus", "Indianapolis", "Seattle",
            "Denver", "Boston", "Nashville", "Portland"
    };
    
    // List of states and abbreviations for generating address data
    private static final String[][] STATES = {
            {"Alabama", "AL"}, {"Alaska", "AK"}, {"Arizona", "AZ"}, {"Arkansas", "AR"}, {"California", "CA"},
            {"Colorado", "CO"}, {"Connecticut", "CT"}, {"Delaware", "DE"}, {"Florida", "FL"}, {"Georgia", "GA"},
            {"Hawaii", "HI"}, {"Idaho", "ID"}, {"Illinois", "IL"}, {"Indiana", "IN"}, {"Iowa", "IA"},
            {"Kansas", "KS"}, {"Kentucky", "KY"}, {"Louisiana", "LA"}, {"Maine", "ME"}, {"Maryland", "MD"},
            {"Massachusetts", "MA"}, {"Michigan", "MI"}, {"Minnesota", "MN"}, {"Mississippi", "MS"}, {"Missouri", "MO"},
            {"Montana", "MT"}, {"Nebraska", "NE"}, {"Nevada", "NV"}, {"New Hampshire", "NH"}, {"New Jersey", "NJ"},
            {"New Mexico", "NM"}, {"New York", "NY"}, {"North Carolina", "NC"}, {"North Dakota", "ND"}, {"Ohio", "OH"},
            {"Oklahoma", "OK"}, {"Oregon", "OR"}, {"Pennsylvania", "PA"}, {"Rhode Island", "RI"}, {"South Carolina", "SC"},
            {"South Dakota", "SD"}, {"Tennessee", "TN"}, {"Texas", "TX"}, {"Utah", "UT"}, {"Vermont", "VT"},
            {"Virginia", "VA"}, {"Washington", "WA"}, {"West Virginia", "WV"}, {"Wisconsin", "WI"}, {"Wyoming", "WY"}
    };
    
    /**
     * Generate a random string of specified length from the given character set
     * 
     * @param length Length of the string to generate
     * @param charset Character set to use for generation
     * @return Random string
     */
    public String generateRandomString(int length, String charset) {
        return random.ints(length, 0, charset.length())
                .mapToObj(i -> String.valueOf(charset.charAt(i)))
                .collect(Collectors.joining());
    }
    
    /**
     * Generate a random alphanumeric string of specified length
     * 
     * @param length Length of the string to generate
     * @return Random alphanumeric string
     */
    public String generateRandomAlphanumeric(int length) {
        return generateRandomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
    }
    
    /**
     * Generate a random numeric string of specified length
     * 
     * @param length Length of the string to generate
     * @return Random numeric string
     */
    public String generateRandomNumeric(int length) {
        return generateRandomString(length, "0123456789");
    }
    
    /**
     * Generate a random alphabetic string of specified length
     * 
     * @param length Length of the string to generate
     * @return Random alphabetic string
     */
    public String generateRandomAlphabetic(int length) {
        return generateRandomString(length, "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
    }
    
    /**
     * Generate a random email address
     * 
     * @param prefix Optional prefix for the email, or null for a random prefix
     * @return Random email address
     */
    public String generateEmail(String prefix) {
        String emailPrefix = prefix != null ? prefix : generateRandomAlphanumeric(8);
        String[] domains = {"example.com", "test.com", "mailtest.net", "test.org", "mailinator.com"};
        return emailPrefix + random.nextInt(1000) + "@" + domains[random.nextInt(domains.length)];
    }
    
    /**
     * Generate a random strong password meeting common security requirements
     * 
     * @return Strong random password
     */
    public String generateStrongPassword() {
        // Generate components
        String upperChars = generateRandomString(2, "ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        String lowerChars = generateRandomString(6, "abcdefghijklmnopqrstuvwxyz");
        String numbers = generateRandomString(2, "0123456789");
        String specialChars = generateRandomString(2, "!@#$%^&*_-+=");
        
        // Combine all components
        String password = upperChars + lowerChars + numbers + specialChars;
        
        // Shuffle characters
        List<Character> passwordChars = new ArrayList<>();
        for (char c : password.toCharArray()) {
            passwordChars.add(c);
        }
        Collections.shuffle(passwordChars, random);
        
        // Convert back to string
        StringBuilder shuffledPassword = new StringBuilder();
        for (char c : passwordChars) {
            shuffledPassword.append(c);
        }
        
        return shuffledPassword.toString();
    }
    
    /**
     * Generate a random first name
     * 
     * @return Random first name
     */
    public String generateFirstName() {
        return FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
    }
    
    /**
     * Generate a random last name
     * 
     * @return Random last name
     */
    public String generateLastName() {
        return LAST_NAMES[random.nextInt(LAST_NAMES.length)];
    }
    
    /**
     * Generate a random full name
     * 
     * @return Random full name
     */
    public String generateFullName() {
        return generateFirstName() + " " + generateLastName();
    }
    
    /**
     * Generate a random phone number in the format (XXX) XXX-XXXX
     * 
     * @return Random phone number
     */
    public String generatePhoneNumber() {
        return String.format("(%s) %s-%s",
                generateRandomNumeric(3),
                generateRandomNumeric(3),
                generateRandomNumeric(4));
    }
    
    /**
     * Generate a random US address
     * 
     * @return Random address
     */
    public Map<String, String> generateAddress() {
        int streetNum = random.nextInt(1000) + 1;
        String streetName = STREET_NAMES[random.nextInt(STREET_NAMES.length)];
        String streetType = STREET_TYPES[random.nextInt(STREET_TYPES.length)];
        String city = CITIES[random.nextInt(CITIES.length)];
        String[] state = STATES[random.nextInt(STATES.length)];
        String zipCode = String.format("%05d", random.nextInt(100000));
        
        Map<String, String> address = new HashMap<>();
        address.put("streetAddress", streetNum + " " + streetName + " " + streetType);
        address.put("city", city);
        address.put("state", state[0]);
        address.put("stateCode", state[1]);
        address.put("zipCode", zipCode);
        address.put("formatted", streetNum + " " + streetName + " " + streetType + ", " +
                city + ", " + state[1] + " " + zipCode);
        
        return address;
    }
    
    /**
     * Generate a random future date within the specified range
     * 
     * @param minDaysInFuture Minimum days in the future
     * @param maxDaysInFuture Maximum days in the future
     * @return Random future date in ISO format
     */
    public String generateFutureDate(int minDaysInFuture, int maxDaysInFuture) {
        int daysInFuture = random.nextInt(maxDaysInFuture - minDaysInFuture + 1) + minDaysInFuture;
        LocalDate date = LocalDate.now().plusDays(daysInFuture);
        return date.format(DateTimeFormatter.ISO_DATE);
    }
    
    /**
     * Generate a random past date within the specified range
     * 
     * @param minDaysInPast Minimum days in the past
     * @param maxDaysInPast Maximum days in the past
     * @return Random past date in ISO format
     */
    public String generatePastDate(int minDaysInPast, int maxDaysInPast) {
        int daysInPast = random.nextInt(maxDaysInPast - minDaysInPast + 1) + minDaysInPast;
        LocalDate date = LocalDate.now().minusDays(daysInPast);
        return date.format(DateTimeFormatter.ISO_DATE);
    }
    
    /**
     * Generate a random date of birth for an adult (18-80 years old)
     * 
     * @return Random date of birth in ISO format
     */
    public String generateAdultDateOfBirth() {
        int minAge = 18;
        int maxAge = 80;
        int age = random.nextInt(maxAge - minAge + 1) + minAge;
        LocalDate dob = LocalDate.now().minusYears(age).minusDays(random.nextInt(365));
        return dob.format(DateTimeFormatter.ISO_DATE);
    }
    
    /**
     * Generate a random credit card number that passes Luhn validation
     * 
     * @param type Card type: "visa", "mastercard", "amex", or "discover"
     * @return Random valid credit card number
     */
    public String generateCreditCardNumber(String type) {
        String prefix;
        int length;
        
        switch (type.toLowerCase()) {
            case "visa":
                prefix = "4";
                length = 16;
                break;
            case "mastercard":
                prefix = String.valueOf(51 + random.nextInt(5)); // 51-55
                length = 16;
                break;
            case "amex":
                prefix = random.nextBoolean() ? "34" : "37";
                length = 15;
                break;
            case "discover":
                prefix = "6011";
                length = 16;
                break;
            default:
                prefix = "4";
                length = 16;
        }
        
        // Generate all but the last digit
        StringBuilder number = new StringBuilder(prefix);
        int remainingLength = length - prefix.length() - 1; // -1 for the check digit
        for (int i = 0; i < remainingLength; i++) {
            number.append(random.nextInt(10));
        }
        
        // Calculate the check digit using Luhn algorithm
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        int checkDigit = (10 - (sum % 10)) % 10;
        
        // Add the check digit
        number.append(checkDigit);
        
        return number.toString();
    }
    
    /**
     * Generate a random device ID
     * 
     * @param platform Platform: "android" or "ios"
     * @return Random device ID
     */
    public String generateDeviceId(String platform) {
        switch (platform.toLowerCase()) {
            case "android":
                // Android UUIDs
                return UUID.randomUUID().toString();
            case "ios":
                // iOS device ID format (UUID with specific format)
                return UUID.randomUUID().toString().toUpperCase();
            default:
                return UUID.randomUUID().toString();
        }
    }
    
    /**
     * Generate a random user agent string
     * 
     * @param platform Platform: "android", "ios", "web", etc.
     * @return Random user agent string
     */
    public String generateUserAgent(String platform) {
        switch (platform.toLowerCase()) {
            case "android":
                // Android user agents
                String[] androidVersions = {"10", "11", "12", "13"};
                String[] androidDevices = {"SM-G991B", "SM-G990B", "Pixel 5", "Pixel 6", "OnePlus 9"};
                
                return "Mozilla/5.0 (Linux; Android " + 
                       androidVersions[random.nextInt(androidVersions.length)] + 
                       "; " + androidDevices[random.nextInt(androidDevices.length)] + 
                       ") AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + 
                       (90 + random.nextInt(10)) + ".0." + (4000 + random.nextInt(1000)) + 
                       "." + random.nextInt(100) + " Mobile Safari/537.36";
                       
            case "ios":
                // iOS user agents
                String[] iosVersions = {"14_8", "15_0", "15_4", "16_0"};
                String[] iosDevices = {"iPhone12,1", "iPhone13,2", "iPhone14,2"};
                
                return "Mozilla/5.0 (iPhone; CPU iPhone OS " + 
                       iosVersions[random.nextInt(iosVersions.length)] + 
                       " like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/" + 
                       (14 + random.nextInt(3)) + ".0 Mobile/15E148 Safari/604.1";
                       
            case "web":
                // Desktop user agents
                String[] browsers = {
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36",
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.0 Safari/605.1.15",
                    "Mozilla/5.0 (X11; Linux x86_64; rv:93.0) Gecko/20100101 Firefox/93.0"
                };
                
                return browsers[random.nextInt(browsers.length)];
                
            default:
                // Generic user agent
                return "Mozilla/5.0 TestApp/" + (1 + random.nextInt(10)) + "." + random.nextInt(10);
        }
    }
    
    /**
     * Generate a random IP address
     * 
     * @param type Type: "ipv4" or "ipv6"
     * @return Random IP address
     */
    public String generateIpAddress(String type) {
        if ("ipv6".equalsIgnoreCase(type)) {
            // Generate IPv6 address
            StringBuilder ipv6 = new StringBuilder();
            for (int i = 0; i < 8; i++) {
                ipv6.append(String.format("%04x", random.nextInt(65536)));
                if (i < 7) {
                    ipv6.append(":");
                }
            }
            return ipv6.toString();
        } else {
            // Generate IPv4 address
            return random.nextInt(256) + "." + random.nextInt(256) + "." + 
                   random.nextInt(256) + "." + random.nextInt(256);
        }
    }
    
    /**
     * Generate a randomized HTTP request headers for simulation
     * 
     * @param platform Platform type
     * @return Map of HTTP header name to value
     */
    public Map<String, String> generateHttpHeaders(String platform) {
        Map<String, String> headers = new HashMap<>();
        
        // Standard headers
        headers.put("User-Agent", generateUserAgent(platform));
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        
        // Add some randomization
        if (random.nextBoolean()) {
            headers.put("DNT", "1");
        }
        
        if (random.nextBoolean()) {
            headers.put("Cache-Control", "no-cache");
        }
        
        return headers;
    }
    
    /**
     * Generate a random malformed input for security testing
     * 
     * @param type Type of malformed input: "sql", "xss", "cmd", "path"
     * @return Random malformed input string
     */
    public String generateMalformedInput(String type) {
        switch (type.toLowerCase()) {
            case "sql":
                String[] sqlInjections = {
                    "' OR '1'='1", 
                    "'; DROP TABLE users; --", 
                    "' UNION SELECT * FROM users; --",
                    "1' OR '1' = '1",
                    "' OR 1=1 --",
                    "admin'--"
                };
                return sqlInjections[random.nextInt(sqlInjections.length)];
                
            case "xss":
                String[] xssInjections = {
                    "<script>alert('XSS')</script>",
                    "<img src='x' onerror='alert(\"XSS\")'>",
                    "\"><script>alert('XSS')</script>",
                    "javascript:alert('XSS')",
                    "<svg onload='alert(\"XSS\")'>",
                    "'-alert(1)-'"
                };
                return xssInjections[random.nextInt(xssInjections.length)];
                
            case "cmd":
                String[] cmdInjections = {
                    "; ls -la", 
                    "| cat /etc/passwd",
                    "; rm -rf /",
                    "$(cat /etc/passwd)",
                    "&& echo vulnerable",
                    "`cat /etc/passwd`"
                };
                return cmdInjections[random.nextInt(cmdInjections.length)];
                
            case "path":
                String[] pathInjections = {
                    "../../../etc/passwd",
                    "..\\..\\..\\windows\\system32\\config",
                    "%2e%2e%2f%2e%2e%2f%2e%2e%2fetc%2fpasswd",
                    "file:///etc/passwd",
                    "///etc/passwd",
                    "....//....//....//etc/passwd"
                };
                return pathInjections[random.nextInt(pathInjections.length)];
                
            default:
                return "' OR '1'='1";
        }
    }
    
    /**
     * Generate data based on a pattern
     * 
     * @param pattern Pattern string with placeholders
     * @return Generated string
     */
    public String generateFromPattern(String pattern) {
        // Replace placeholders with generated data
        String result = pattern;
        
        // Replace {{RANDOM_ALPHA:length}}
        result = result.replaceAll(
                "\\{\\{RANDOM_ALPHA:(\\d+)\\}\\}", 
                match -> generateRandomAlphabetic(Integer.parseInt(match.group(1))));
        
        // Replace {{RANDOM_NUMERIC:length}}
        result = result.replaceAll(
                "\\{\\{RANDOM_NUMERIC:(\\d+)\\}\\}", 
                match -> generateRandomNumeric(Integer.parseInt(match.group(1))));
        
        // Replace {{RANDOM_ALPHANUMERIC:length}}
        result = result.replaceAll(
                "\\{\\{RANDOM_ALPHANUMERIC:(\\d+)\\}\\}", 
                match -> generateRandomAlphanumeric(Integer.parseInt(match.group(1))));
        
        // Replace {{FIRST_NAME}}
        result = result.replaceAll("\\{\\{FIRST_NAME\\}\\}", match -> generateFirstName());
        
        // Replace {{LAST_NAME}}
        result = result.replaceAll("\\{\\{LAST_NAME\\}\\}", match -> generateLastName());
        
        // Replace {{EMAIL}}
        result = result.replaceAll("\\{\\{EMAIL\\}\\}", match -> generateEmail(null));
        
        // Replace {{PHONE}}
        result = result.replaceAll("\\{\\{PHONE\\}\\}", match -> generatePhoneNumber());
        
        // Replace {{DATE:format}}
        result = result.replaceAll(
                "\\{\\{DATE:(\\w+)\\}\\}", 
                match -> {
                    String format = match.group(1);
                    LocalDate date = LocalDate.now();
                    switch (format) {
                        case "FUTURE":
                            date = date.plusDays(random.nextInt(30) + 1);
                            break;
                        case "PAST":
                            date = date.minusDays(random.nextInt(30) + 1);
                            break;
                    }
                    return date.format(DateTimeFormatter.ISO_DATE);
                });
        
        return result;
    }
    
    /**
     * Generate a collection of related test data for a specific scenario
     * 
     * @param scenario Type of scenario
     * @param count Number of data items to generate
     * @return List of maps containing the generated data
     */
    public List<Map<String, Object>> generateScenarioData(String scenario, int count) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        
        switch (scenario) {
            case "user_registration":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> userData = new HashMap<>();
                    String firstName = generateFirstName();
                    String lastName = generateLastName();
                    
                    userData.put("firstName", firstName);
                    userData.put("lastName", lastName);
                    userData.put("email", generateEmail(firstName.toLowerCase() + "." + lastName.toLowerCase()));
                    userData.put("password", generateStrongPassword());
                    userData.put("phone", generatePhoneNumber());
                    userData.put("dateOfBirth", generateAdultDateOfBirth());
                    userData.put("address", generateAddress());
                    
                    dataList.add(userData);
                }
                break;
                
            case "payment_info":
                String[] cardTypes = {"visa", "mastercard", "amex", "discover"};
                for (int i = 0; i < count; i++) {
                    Map<String, Object> paymentData = new HashMap<>();
                    String cardType = cardTypes[random.nextInt(cardTypes.length)];
                    
                    paymentData.put("cardType", cardType);
                    paymentData.put("cardNumber", generateCreditCardNumber(cardType));
                    paymentData.put("expiryDate", (random.nextInt(12) + 1) + "/" + (LocalDate.now().getYear() + random.nextInt(5) + 1));
                    paymentData.put("cvv", generateRandomNumeric("amex".equals(cardType) ? 4 : 3));
                    paymentData.put("cardholderName", generateFullName());
                    paymentData.put("billingAddress", generateAddress());
                    
                    dataList.add(paymentData);
                }
                break;
                
            case "device_info":
                String[] platforms = {"android", "ios"};
                String[] manufacturers = {"Samsung", "Apple", "Google", "OnePlus", "Xiaomi", "Huawei"};
                for (int i = 0; i < count; i++) {
                    Map<String, Object> deviceData = new HashMap<>();
                    String platform = platforms[random.nextInt(platforms.length)];
                    
                    deviceData.put("platform", platform);
                    deviceData.put("manufacturer", manufacturers[random.nextInt(manufacturers.length)]);
                    deviceData.put("model", "Model " + generateRandomAlphanumeric(4));
                    deviceData.put("osVersion", random.nextInt(5) + 8 + "." + random.nextInt(10));
                    deviceData.put("deviceId", generateDeviceId(platform));
                    deviceData.put("screenResolution", (720 + random.nextInt(1080)) + "x" + (1280 + random.nextInt(920)));
                    
                    dataList.add(deviceData);
                }
                break;
                
            case "login_attempts":
                for (int i = 0; i < count; i++) {
                    Map<String, Object> loginData = new HashMap<>();
                    boolean success = random.nextBoolean();
                    
                    loginData.put("username", generateEmail(null));
                    loginData.put("timestamp", new Date().toString());
                    loginData.put("success", success);
                    loginData.put("ipAddress", generateIpAddress("ipv4"));
                    loginData.put("userAgent", generateUserAgent(random.nextBoolean() ? "android" : "ios"));
                    
                    if (!success) {
                        String[] failureReasons = {"invalid_credentials", "account_locked", "expired_password", "suspicious_location"};
                        loginData.put("failureReason", failureReasons[random.nextInt(failureReasons.length)]);
                    }
                    
                    dataList.add(loginData);
                }
                break;
                
            default:
                logger.warn("Unknown scenario type: {}", scenario);
        }
        
        return dataList;
    }
    
    /**
     * Get or create a list of data values
     * 
     * @param key Cache key
     * @param generator Supplier to generate data if not in cache
     * @return List of data values
     */
    public List<String> getOrCreateDataList(String key, Supplier<List<String>> generator) {
        if (!dataCache.containsKey(key)) {
            logger.info("Generating new data list for key: {}", key);
            dataCache.put(key, generator.get());
        }
        return dataCache.get(key);
    }
    
    /**
     * Get a random item from a list of data values
     * 
     * @param key Cache key
     * @param generator Supplier to generate data if not in cache
     * @return Random data value
     */
    public String getRandomDataItem(String key, Supplier<List<String>> generator) {
        List<String> dataList = getOrCreateDataList(key, generator);
        return dataList.get(random.nextInt(dataList.size()));
    }
}
