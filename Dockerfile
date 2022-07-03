FROM bellsoft/liberica-openjdk-alpine:17

MAINTAINER suranyi suranyi.sysu@gmail.com

LABEL create_time=2022.06.15

RUN wget http://pmglab.top/gbc/download/gbc.jar -O /gbc.jar

ENTRYPOINT ["java", "-XshowSettings:vm", "-XX:InitialRAMPercentage=100", "-XX:MaxRAMPercentage=100", "-jar", "/gbc.jar"]