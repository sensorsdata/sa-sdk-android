#
# Created by dengshiwei on 2022/04/20.
# Copyright 2015－2022 Sensors Data Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Maven 发布全量包脚本
echo "start ====== publish AAR"
pwd

# 定义打包函数
assembleAAR() {
  ./gradlew :${1}:assembleRelease
  mv "$PWD/${1}/build/outputs/aar/${1}-release.aar" "$PWD/$aar/${2}-${3}.aar"
}

while read LINE
do
  if [[ $LINE =~ ^isEmbedCoreAAR* ]];then
    isEmbedCore=${LINE#*: }
    isEmbedCore=${isEmbedCore//,/}
    echo "isEmbedCore = $isEmbedCore"
  elif [[ $LINE =~ ^module_version* ]]; then
    module_version=${LINE#*: }
    module_version=${module_version//\"/}
    echo "module_version = $module_version"
  elif [[ $LINE =~ ^isEmbedSensorsSDKAAR* ]];then
    isEmbedSDK=${LINE#*: }
    isEmbedSDK=${isEmbedSDK//,/}
    echo "isEmbedSDK = $isEmbedSDK"
  elif [[ $LINE =~ ^sdk_version* ]]; then
    sdk_version=${LINE#*: }
    sdk_version=${sdk_version//\"/}
    sdk_version=${sdk_version//,/}
    echo "sdk_version = $sdk_version"
  fi
done < "$PWD/script_config/config.gradle"

aar="repo_aar"
if [ ! -e ${aar} ];then
  mkdir $aar
fi

./gradlew clean
if [ $isEmbedCore == true ]; then
  # 打包 core 核心模块
  assembleAAR "module_core" "sa_core" $module_version
elif [ $isEmbedSDK == true ]; then
    # 打包 SDK 全量包
  assembleAAR "SensorsAnalyticsSDK" "SensorsAnalyticsSDK" $sdk_version
else
  # 打包其它模块
  assembleAAR "module_advert" "sa_advert" $module_version
  assembleAAR "module_autoTrack" "sa_autoTrack" $module_version
  assembleAAR "module_encrypt" "sa_encrypt" $module_version
  assembleAAR "module_push" "sa_push" $module_version
  assembleAAR "module_visual" "sa_visual" $module_version
  assembleAAR "module_exposure" "sa_exposure" $module_version
fi

echo "end ====== publish AAR"