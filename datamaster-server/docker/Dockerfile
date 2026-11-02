FROM eclipse-temurin:17-jre

# 设置时区和应用目录
ENV TZ=Asia/Shanghai \
    APP_HOME=/usr/app/jar

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone \
 && mkdir -p $APP_HOME

WORKDIR $APP_HOME

# 拷贝产物
COPY ./target/*.jar ./datamaster.jar

EXPOSE 8989

ENTRYPOINT ["java", "-Xms512m", "-Xmx2g", "-Dspring.profiles.active=prod","-jar","datamaster.jar"]
CMD ["--server.port=8989"]
