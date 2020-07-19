# Bolognese. manager-api for ciabatta
## 주요 기능
* 음식정 사장님들을 위한 API 제공.
* 음식정 사장님의 라이브 방송을 녹화하고 메뉴마다 조리과정을 클립으로 변환.

## 기술 스택
* Web : spring boot + spring webflux + spring security
* DB & Cache : MongoDB + Redis
* 라이브 방송 : [Agora](http://agora.io/)
* 동영상 편집 : FFmpeg

## Build
* java8
* Linux에서만 빌드 가능 
  * 방송 녹화를 위해 Agora Recording SDK(jni)를 사용하는데 Linux에서만 지원.
  * ref. https://docs.agora.io/en/Recording/product_recording?platform=Linux
* docker image build
  * ```shell
    ./gradlew clean build
    docker build -t manager-api:1.0.0 .
    ```
  * 필수 환경변수
    * MONGO_URL: MongoDB url (mongodb://...)
    * REDIS_URL: Redis url (redis://...)
    * APP_ID: Agora App Id
    * BUCKET_NAME: AWS S3 Bucket name

## Api Spec
* https://www.notion.so/APIs-for-Managers-cf19b7f0d4ab486c82ceead9dc2aab31

