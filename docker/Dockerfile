# Multi-stage build for FashionStore backend
# Stage 1: Build stage with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy pom.xml first for better Docker layer caching
COPY backend/pom.xml .

# Copy source code
COPY backend/src ./src

# Build WAR file (downloads dependencies during build)
RUN mvn clean package -DskipTests -B

# Stage 2: Runtime stage with Tomcat
FROM tomcat:10.1-jdk21

# Set working directory
WORKDIR /usr/local/tomcat

# Set environment variables
ENV CATALINA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"

# Remove default Tomcat applications to reduce attack surface
RUN rm -rf /usr/local/tomcat/webapps/ROOT \
           /usr/local/tomcat/webapps/docs \
           /usr/local/tomcat/webapps/examples \
           /usr/local/tomcat/webapps/host-manager \
           /usr/local/tomcat/webapps/manager

# Copy WAR file from builder stage
COPY --from=builder /build/target/FashionStore-0.0.1-SNAPSHOT.war /usr/local/tomcat/webapps/ROOT.war

# Create non-root user for security
RUN groupadd -r tomcat && useradd -r -g tomcat tomcat

# Ensure work directory is writable by tomcat user for JSP compilation
RUN mkdir -p /usr/local/tomcat/work/Catalina/localhost/ROOT && \
    chown -R tomcat:tomcat /usr/local/tomcat && \
    chmod -R 755 /usr/local/tomcat/work

USER tomcat

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/home || exit 1

# Start Tomcat
CMD ["catalina.sh", "run"]
