FROM openjdk:8
ARG NAME
ARG VERSION
ARG DATE
WORKDIR /usr/src/app
COPY build/libs/${NAME}-${VERSION}.jar .
RUN ln -s ./${NAME}-${VERSION}.jar app.jar
LABEL version=${VERSION}
LABEL org.cedar.metadata.${NAME}-version=${VERSION}
LABEL org.cedar.metadata.${NAME}-date=${DATE}
LABEL org.cedar.metadata.${NAME}-name=${NAME}Module
LABEL org.cedar.metadata.${NAME}-vendor=CEDAR
EXPOSE 8080
CMD java -jar /usr/src/app/app.jar
