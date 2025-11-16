# Test Prompt for AI - Hyperliquid Trading Bot Verification

## Objective
You are an AI assistant tasked with testing a Spring Boot application from scratch. Your goal is to clone the repository, follow the documentation exactly as written, run the application, and verify all functionality is working as documented.

## Instructions

### Phase 1: Setup and Clone

1. **Delete any existing project folder** (if it exists)
   ```bash
   cd /c/Users/tbiri/Documents
   rm -rf pochyperliquid
   ```

2. **Clone the repository**
   ```bash
   cd /c/Users/tbiri/Documents
   git clone https://github.com/thalesbirino/hyperliquid-.git
   cd hyperliquid-
   ```

3. **Verify project structure**
   ```bash
   ls -la
   ```

   Expected files and directories:
   - README.md (main documentation)
   - DELIVERABLES.md (complete deliverables guide)
   - Hyperliquid-Trading-Bot.postman_collection.json
   - pom.xml
   - mvnw (Maven wrapper for Linux/Mac)
   - mvnw.cmd (Maven wrapper for Windows)
   - src/ (source code directory)
   - .gitignore

### Phase 2: Read Documentation

4. **Read the README.md file completely**
   - Use the Read tool to read README.md
   - Understand the project structure, prerequisites, and how to run

5. **Read the DELIVERABLES.md file**
   - Use the Read tool to read DELIVERABLES.md
   - Note all the test scenarios and expected outcomes

### Phase 3: Build and Run

6. **Check Java version**
   ```bash
   java -version
   ```
   Expected: Java 17 or higher

7. **Build the project** (following README instructions)
   ```bash
   mvn clean install
   ```
   Expected: BUILD SUCCESS

8. **Run the application** (following README instructions)
   ```bash
   mvn spring-boot:run
   ```

   Expected output should include:
   - Spring Boot banner
   - "Started HyperliquidApplication in X.XXX seconds"
   - Application running on port 8080
   - H2 database initialized
   - No errors in startup

### Phase 4: Verify Access Points

9. **Test Swagger UI access**
   ```bash
   curl -I http://localhost:8080/swagger-ui/index.html
   ```
   Expected: HTTP 200 OK

10. **Test H2 Console access**
    ```bash
    curl -I http://localhost:8080/h2-console
    ```
    Expected: HTTP 200 or HTTP 302 (redirect)

11. **Test API Docs access**
    ```bash
    curl -I http://localhost:8080/api-docs
    ```
    Expected: HTTP 200 OK

### Phase 5: Test Authentication

12. **Test login endpoint** (as documented in README)
    ```bash
    curl -X POST http://localhost:8080/api/auth/login \
      -H "Content-Type: application/json" \
      -d '{"username":"admin","password":"password123"}' \
      -s | python -m json.tool
    ```

    Expected response:
    ```json
    {
        "success": true,
        "message": "Login successful",
        "data": {
            "token": "eyJhbGc...",
            "type": "Bearer",
            "username": "admin",
            "email": "admin@tradingbot.com",
            "role": "ADMIN"
        }
    }
    ```

    **IMPORTANT**: Save the JWT token for next tests

### Phase 6: Test Webhook Endpoint

13. **Test webhook with ETH strategy** (as documented in DELIVERABLES.md)
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"buy","strategyId":"66e858a5-ca3c-4c2c-909c-34c605b3e5c7","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected response:
    ```json
    {
        "success": true,
        "message": "Order executed successfully",
        "orderId": "MOCK-XXXXXXXX",
        "action": "BUY",
        "asset": "ETH",
        "size": "0.10000000",
        "price": "MARKET",
        "status": "EXECUTED",
        "executedAt": "2025-XX-XXTXX:XX:XX.XXXXXXX"
    }
    ```

14. **Check console logs**
    - Verify the application console shows the beautiful order execution box with all order details

15. **Test webhook with SELL action**
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"sell","strategyId":"66e858a5-ca3c-4c2c-909c-34c605b3e5c7","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected: Same format as BUY, but with "action": "SELL"

### Phase 7: Test Protected Endpoints

16. **Extract JWT token from login response** (save it as TOKEN variable)

17. **Test GET all users** (protected endpoint)
    ```bash
    curl -X GET http://localhost:8080/api/user \
      -H "Authorization: Bearer YOUR_TOKEN_HERE" \
      -s | python -m json.tool
    ```

    Expected: List of 3 users (admin, trader001, trader002)

18. **Test GET all strategies** (protected endpoint)
    ```bash
    curl -X GET http://localhost:8080/api/strategy \
      -H "Authorization: Bearer YOUR_TOKEN_HERE" \
      -s | python -m json.tool
    ```

    Expected: List of 4 strategies (ETH, BTC, SOL, AVAX)

19. **Test GET all configs** (protected endpoint)
    ```bash
    curl -X GET http://localhost:8080/api/config \
      -H "Authorization: Bearer YOUR_TOKEN_HERE" \
      -s | python -m json.tool
    ```

    Expected: List of 4 configs

### Phase 8: Test Error Cases

20. **Test webhook with invalid password**
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"buy","strategyId":"66e858a5-ca3c-4c2c-909c-34c605b3e5c7","password":"WrongPassword"}' \
      -s | python -m json.tool
    ```

    Expected: Error response with "success": false

21. **Test webhook with invalid strategy ID**
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"buy","strategyId":"invalid-id","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected: Error response with "success": false

22. **Test protected endpoint without JWT token**
    ```bash
    curl -X GET http://localhost:8080/api/user -s | python -m json.tool
    ```

    Expected: Unauthorized error response

### Phase 9: Database Verification

23. **Access H2 Console**
    - Open browser or use curl to access http://localhost:8080/h2-console
    - Connection details (from DELIVERABLES.md):
      - JDBC URL: `jdbc:h2:mem:hyperliquid-db`
      - Username: `sa`
      - Password: (leave empty)

24. **Verify seed data exists**
    - Check users table has 3 records
    - Check configs table has 4 records
    - Check strategies table has 4 records

### Phase 10: Test Additional Strategies

25. **Test BTC strategy** (as documented in DELIVERABLES.md)
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"buy","strategyId":"f7a3b2c1-d4e5-6f78-9g01-h2i3j4k5l6m7","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected: Success with asset "BTC", size "0.01"

26. **Test SOL strategy**
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"buy","strategyId":"a1b2c3d4-e5f6-7g89-0h12-i3j4k5l6m7n8","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected: Success with asset "SOL", size "5.0"

27. **Test AVAX strategy**
    ```bash
    curl -X POST http://localhost:8080/api/webhook \
      -H "Content-Type: application/json" \
      -d '{"action":"sell","strategyId":"b2c3d4e5-f6g7-8h90-1i23-j4k5l6m7n8o9","password":"Admin@9090"}' \
      -s | python -m json.tool
    ```

    Expected: Success with asset "AVAX", size "2.5"

### Phase 11: Verification Report

28. **Create a comprehensive test report** that includes:
    - ✅ or ❌ for each test
    - Any errors encountered
    - Screenshots or output of key successful tests
    - Console logs showing the order execution boxes
    - Overall assessment: Does the application work exactly as documented?

## Success Criteria

The test is considered **SUCCESSFUL** if:
1. ✅ Project clones without errors
2. ✅ Build completes with BUILD SUCCESS
3. ✅ Application starts without errors
4. ✅ All 3 access points (Swagger, H2 Console, API Docs) are accessible
5. ✅ Login works and returns valid JWT token
6. ✅ Webhook endpoint accepts and processes orders for all 4 strategies
7. ✅ Protected endpoints require JWT authentication
8. ✅ Error cases return appropriate error messages
9. ✅ Database contains all seed data
10. ✅ Console logs show beautiful order execution boxes
11. ✅ All functionality matches the documentation exactly

## Expected Outcome

You should be able to complete all 27 test steps successfully, demonstrating that:
- The documentation is accurate and complete
- The application runs without any manual configuration
- All features work as advertised
- The project is production-ready in mock mode

## Report Format

Please provide your report in this format:

```
# Hyperliquid Trading Bot - Test Report

## Test Summary
- Total Tests: 27
- Passed: X
- Failed: X
- Success Rate: XX%

## Detailed Results

### Phase 1: Setup and Clone
- [✅/❌] Step 1: Delete existing folder
- [✅/❌] Step 2: Clone repository
- [✅/❌] Step 3: Verify project structure

### Phase 2: Read Documentation
- [✅/❌] Step 4: Read README.md
- [✅/❌] Step 5: Read DELIVERABLES.md

### Phase 3: Build and Run
- [✅/❌] Step 6: Check Java version
- [✅/❌] Step 7: Build project
- [✅/❌] Step 8: Run application

... continue for all phases ...

## Issues Found
1. [If any] Description of issue
2. [If any] Description of issue

## Console Output Samples
[Paste key console outputs here]

## Conclusion
[Overall assessment of whether the project works as documented]

## Recommendations
[Any suggestions for improvement]
```

## Additional Notes

- Follow the documentation EXACTLY as written
- Do not make assumptions or try to "fix" things
- Report any discrepancies between documentation and actual behavior
- If something doesn't work, note it and continue with other tests
- Take note of the Maven wrapper usage (mvnw.cmd for Windows, ./mvnw for Linux/Mac)
- The application uses H2 in-memory database, so data resets on each restart
- All passwords are BCrypt hashed in the database but plaintext in API requests
- Mock mode is enabled, so no real Hyperliquid API calls are made

---

**Start the test now and provide a detailed report of your findings.**
