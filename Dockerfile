FROM dlsrb6342/manager-base:1.0.0

COPY . /app

WORKDIR /app/recorder
RUN cp ./build/generated/sources/headers/java/main/* ./src/native/jni/
RUN ./build.sh /usr/lib/jvm/java/include

WORKDIR /app
ENTRYPOINT exec java -Djava.library.path=recorder/build/classes/java/main/io/agora/recording/ -jar api/build/libs/api-1.0.0.jar \
           --spring.data.mongodb.uri=$MONGO_URL --spring.redis.url=$REDIS_URL --recording.app.id=$APP_ID --amazon.s3.bucket.name=$BUCKET_NAME