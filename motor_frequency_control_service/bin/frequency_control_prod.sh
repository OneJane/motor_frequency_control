#!/bin/bash

AppName=motor_frequency_control_service-1.0.0.jar
AppMain=org.springframework.boot.loader.JarLauncher
AppHome="$(dirname $(readlink -f $(dirname "$0")))"

#JVM参数
JVM_OPTS="-Dname=$AppName  -Duser.timezone=Asia/Shanghai -Xms4096M -Xmx4096M -XX:PermSize=2048M -XX:MaxPermSize=4096M -XX:+HeapDumpOnOutOfMemoryError -XX:+PrintGCDateStamps  -XX:+PrintGCDetails -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC -XX:+UseParallelOldGC"
APP_HOME=`pwd`

if [ "$1" = "" ];
then
    echo -e "\033[0;31m 未输入操作名 \033[0m  \033[0;34m {start|stop|restart|status} \033[0m"
    exit 1
fi

if [ "$AppName" = "" ];
then
    echo -e "\033[0;31m 未输入应用名 \033[0m"
    exit 1
fi

function start()
{
    PID=`ps -ef |grep report|grep $AppName|grep -v grep|awk '{print $2}'`

        if [ x"$PID" != x"" ]; then
            echo "$AppName is running..."
        else
                nohup /usr/local/jdk/bin/java -cp $AppHome/$AppName:$AppHome/config/ $AppMain > /dev/null 2>&1 &
                echo "Start $AppName success..."
        fi
}

function stop()
{
    echo "Stop $AppName"

        PID=""
        query(){
                PID=`ps -ef |grep report|grep $AppName|grep -v grep|awk '{print $2}'`
        }

        query
        if [ x"$PID" != x"" ]; then
                kill -TERM $PID
                echo "$AppName (pid:$PID) exiting..."
                while [ x"$PID" != x"" ]
                do
                        sleep 1
                        query
                done
                echo "$AppName exited."
        else
                echo "$AppName already stopped."
        fi
}

function restart()
{
    stop
    start
}

function status()
{
    PID=`ps -ef |grep report|grep $AppName|grep -v grep|wc -l`
    if [ $PID != 0 ];then
        echo "$AppName is running..."
    else
        echo "$AppName is not running..."
    fi
}

case $1 in
    start)
    start;;
    stop)
    stop;;
    restart)
    restart;;
    status)
    status;;
    *)

esac

#gradle clean build deploy -x test -Denv=test
#ps -ef|grep firefox|grep -v grep|awk '{print $2}'|xargs kill -9