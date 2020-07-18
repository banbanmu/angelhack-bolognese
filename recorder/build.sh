#!/bin/bash

build_cpp()
{
  make -f ./src/native/.makefile JNIINCLUDEPATH=$JNI_PATH
  mv librecording.so ./build/classes/java/main/io/agora/recording/
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

  build_cpp
  echo "build all done!"
}
clean()
{
  clean_cpp
  echo "clean all done!"
}

build $1
