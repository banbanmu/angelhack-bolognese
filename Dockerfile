FROM dlsrb6342/recorder-base:1.0.0
ARG APP_ID
ENV APP_ID=$APP_ID

COPY . /app

WORKDIR /app
RUN ./gradlew build

WORKDIR /app/recorder
RUN cp ./build/generated/sources/headers/java/main/* ./src/native/jni/
RUN ./build.sh /usr/lib/jvm/java/include

WORKDIR /app
ENTRYPOINT exec java -Djava.library.path=recorder/build/classes/java/main/io/agora/recording/ -jar api/build/libs/api-1.0.0.jar