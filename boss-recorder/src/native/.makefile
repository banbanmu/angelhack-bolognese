ifeq (${CXX},)
CXX=g++
endif
LINK=${CXX}
TOPDIR=`pwd`
COMMONPATH=`pwd`
LIBPATH=${COMMONPATH}/libs
#set your jvm path!!!
#JNIINCLUDE = /usr/lib/jvm/java-9-openjdk-amd64/include/
STDFLAGS = -std=c++0x
LDFLAGS= -shared -static-libgcc
FPIC = -fPIC
CXXFLAGS  = -pipe -std=c++0x -fPIC -g -fno-omit-frame-pointer \
			-DNDEBUG=1 -Wconversion -O3 -Wall -W #-fvisibility=hidden
					
LIB	   = -pthread -lpthread -L$(LIBPATH) -lrecorder -lrt
INCPATH =-I. -I${COMMONPATH}/include -I${COMMONPATH}/include/base -I${COMMONPATH}/base -I${COMMONPATH}/agorasdk -I${COMMONPATH}
#JNIPATH = -I. -I/$(JNIINCLUDE) -I/$(JNIINCLUDE)/linux/

JNIPATH=-I${JNIINCLUDEPATH} -I${JNIINCLUDEPATH}/linux
OBJ = opt_parser.o

REALTARGET = exe
TARGET=librecording.so

.PHONY: all clean
all: $(TARGET)

$(TARGET): $(OBJ)
	$(CXX) -c ./src/native $(CXXFLAGS) $(INCPATH) ${COMMONPATH}/agorasdk/AgoraSdk.cpp
	mv AgoraSdk.o ./build
	
	$(LINK) ./src/native/RecordingJni.cpp -o $@ $(LDFLAGS) $(FPIC) $(INCPATH) $(JNIPATH) $(STDFLAGS) ./build/AgoraSdk.o ./build/opt_parser.o $(LIB) -I.
	mv $@ ./build

$(OBJ):
	$(CXX) -c ./build $(CXXFLAGS) $(INCPATH) ${COMMONPATH}/base/opt_parser.cpp
	mv opt_parser.o ./build

clean:
	rm -f ./build/$(TARGET)
	rm -f ${OBJ}
	rm -f ./build/*.o
