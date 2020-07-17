#!/bin/bash
createBuildFloder()
{
  buildDir="build"
  if [ -d "$buildDir" ];then
  echo
  else
  mkdir "$buildDir"
  fi
}
build_java()
{
  createBuildFloder

  jniLayer="./src/native/jni"
  javaClassPath="./build"

  javac src/main/java/io/agora/recording/common/*.java -d build
  javac src/main/java/io/agora/recording/*.java -d build
  javac src/main/java/gelato/riso/*.java -d build -Xlint:unchecked

  #clean previous jni file
  rm -f $jniLayer/io_agora_recording_RecordingSDK.h
  javah -d $jniLayer -classpath $javaClassPath io.agora.recording.RecordingSDK
}

build_cpp()
{
  make -f ./src/native/.makefile JNIINCLUDEPATH=$JNI_PATH
  mv ./build/librecording.so ./build/io/agora/recording/.
}
clean_java()
{
  rm -f build/*.class
  rm -rf build/io
}
clean_cpp()
{
  make clean -f ./native/.makefile
}
build()
{
  JNI_PATH=$1
  export JNI_PATH

  CLASSPATH=`pwd`/build
  export CLASSPATH

  build_java
  build_cpp
  echo "build all done!"
}
clean()
{
  clean_java
  clean_cpp
  echo "clean all done!"
}

build $1
