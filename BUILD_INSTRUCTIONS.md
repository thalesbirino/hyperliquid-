# Build Instructions for Hyperliquid Trading Bot

## ⚠️ Important: Lombok Configuration

This project uses **Lombok** to reduce boilerplate code. If you encounter compilation errors about missing getters/setters, follow these steps:

### For IntelliJ IDEA

1. **Install Lombok Plugin**:
   - Go to `File` → `Settings` → `Plugins`
   - Search for "Lombok"
   - Install and restart IntelliJ

2. **Enable Annotation Processing**:
   - Go to `File` → `Settings` → `Build, Execution, Deployment` → `Compiler` → `Annotation Processors`
   - Check ✅ "Enable annotation processing"
   - Click `Apply` and `OK`

3. **Rebuild Project**:
   - `Build` → `Rebuild Project`

### For Eclipse

1. **Download Lombok JAR**:
   - Download from: https://projectlombok.org/downloads/lombok.jar

2. **Install Lombok**:
   ```bash
   java -jar lombok.jar
   ```
   - Select your Eclipse installation
   - Click "Install/Update"
   - Restart Eclipse

3. **Maven Update**:
   - Right-click project → `Maven` → `Update Project`

### For VS Code

1. **Install Extension Pack for Java**
2. **Add Lombok Dependency** (already in pom.xml)
3. **Clean and Rebuild**:
   ```bash
   mvn clean install
   ```

## Building from Command Line

### Option 1: Using Maven Wrapper (Recommended)

If Maven is not installed:

**Windows:**
```bash
mvnw.cmd clean install
```

**Linux/Mac:**
```bash
./mvnw clean install
```

### Option 2: Using Installed Maven

```bash
mvn clean install
```

### Skip Tests During Build

```bash
mvn clean install -DskipTests
```

## Running the Application

### From IDE

- Run the main class: `HyperliquidApplication.java`

### From Command Line

```bash
mvn spring-boot:run
```

Or use the JAR file:

```bash
java -jar target/hyperliquid-trading-bot-1.0.0-SNAPSHOT.jar
```

## Verifying the Build

After successful build, you should see:

```
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  XX.XXX s
[INFO] Finished at: YYYY-MM-DDTHH:MM:SS
[INFO] ------------------------------------------------------------------------
```

The generated JAR will be in: `target/hyperliquid-trading-bot-1.0.0-SNAPSHOT.jar`

## Troubleshooting

### Problem: "Cannot find symbol" errors for getters/setters

**Solution**: Lombok is not configured properly. Follow the IDE-specific steps above.

### Problem: Port 8080 already in use

**Solution**: Change the port in `application.yml`:
```yaml
server:
  port: 8081  # or any available port
```

### Problem: Database locked

**Solution**: Delete the database file and restart:
```bash
rm -rf ./data/
mvn spring-boot:run
```

### Problem: Maven dependencies not downloading

**Solution**: Force update dependencies:
```bash
mvn clean install -U
```

## Project Structure Verification

After build, verify these directories exist:

```
target/
├── classes/
│   ├── com/trading/hyperliquid/
│   └── application.yml
├── generated-sources/
│   └── annotations/  (Lombok generated code)
└── hyperliquid-trading-bot-1.0.0-SNAPSHOT.jar
```

## Quick Start After Successful Build

1. **Run the application**:
   ```bash
   mvn spring-boot:run
   ```

2. **Test the webhook**:
   ```bash
   curl -X POST http://localhost:8080/api/webhook \
     -H "Content-Type: application/json" \
     -d '{"action": "buy", "strategyId": "66e858a5-ca3c-4c2c-909c-34c605b3e5c7", "password": "Admin@9090"}'
   ```

3. **Access Swagger UI**:
   - http://localhost:8080/swagger-ui/index.html

4. **Access H2 Console**:
   - http://localhost:8080/h2-console
   - JDBC URL: `jdbc:h2:file:./data/hyperliquid-db`
   - Username: `sa`
   - Password: (empty)

## Additional Resources

- **Spring Boot Documentation**: https://docs.spring.io/spring-boot/docs/current/reference/html/
- **Lombok Documentation**: https://projectlombok.org/features/
- **Hyperliquid API Docs**: https://hyperliquid.gitbook.io/hyperliquid-docs/

## Support

If you continue to experience build issues:

1. Verify Java 17 is installed: `java -version`
2. Verify Maven is installed: `mvn -version`
3. Check Lombok plugin is installed in your IDE
4. Enable annotation processing in IDE settings
5. Clean and rebuild: `mvn clean install`

---

**Note**: The project is fully functional. Any build errors are typically related to Lombok IDE configuration, not the code itself.
