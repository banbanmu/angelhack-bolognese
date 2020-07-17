FROM dlsrb6342/recorder-base:1.0.0
ARG APP_ID
ENV APP_ID=$APP_ID

COPY . /app

WORKDIR /app
RUN ./build.sh /usr/lib/jvm/java/include

WORKDIR /app/build
ENTRYPOINT java -Djava.library.path=io/agora/recording/ gelato/riso/RecordingSampleM \
                --appId $APP_ID --appliteDir ../bin/ --channel qweqwfedsczx --uid 0
