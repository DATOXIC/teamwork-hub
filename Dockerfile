# ==========================================
# GIAI ĐOẠN 1: Biên dịch mã nguồn bằng Maven & JDK 21
# ==========================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy cấu hình và mã nguồn
COPY pom.xml .
COPY src ./src

# Đóng gói thành file teamwork-hub.war
RUN mvn clean package -DskipTests

# ==========================================
# GIAI ĐOẠN 2: Khởi chạy trên Apache Tomcat 10.1 (Jakarta EE 10)
# ==========================================
FROM tomcat:10.1-jdk21-temurin

# Dọn dẹp ứng dụng mặc định
RUN rm -rf /usr/local/tomcat/webapps/*

# Đổi tên thành ROOT.war để chạy ngay tại root URL https://your-domain.onrender.com/
COPY --from=build /app/target/teamwork-hub.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]
